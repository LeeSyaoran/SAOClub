#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Sinh ERD với:
- Layout FORCE-DIRECTED (đơn giản) cho gọn
- PK có icon khóa vàng
- FK có mũi tên xanh
- Bảng cùng nhóm màu
"""
import re, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
from pathlib import Path
from collections import defaultdict
from lxml import etree
import random
import math

# ─────────────────────────────────────────────────────────
# 1. PARSE SQL
# ─────────────────────────────────────────────────────────
SQL_FILE = Path(__file__).parent.parent / "Database" / "QLBanMayTinh.sql"
sql_text = SQL_FILE.read_text(encoding="utf-8")
sql_text = re.sub(r'--[^\n]*', '', sql_text)
sql_text = re.sub(r'/\*.*?\*/', '', sql_text, flags=re.DOTALL)


def parse_table_body(body):
    tokens, current, depth = [], "", 0
    for ch in body:
        if ch == '(': depth += 1; current += ch
        elif ch == ')': depth -= 1; current += ch
        elif ch == ',' and depth == 0:
            if current.strip(): tokens.append(current.strip()); current = ""
        else: current += ch
    if current.strip(): tokens.append(current.strip())
    return tokens


CREATE_TABLE_RE = re.compile(
    r'CREATE\s+TABLE\s+(\w+)\s*\((.*?)\)\s*;', re.IGNORECASE | re.DOTALL
)
tables = {}
for m in CREATE_TABLE_RE.finditer(sql_text):
    tbl_name = m.group(1)
    cols, pks, fks = [], [], []
    for token in parse_table_body(m.group(2)):
        t = token.strip()
        tu = t.upper()
        if 'PRIMARY KEY' in tu and not tu.startswith('CONSTRAINT'):
            if '(' not in tu[tu.index('PRIMARY KEY'):]:
                mm = re.match(r'(\w+)', t.strip())
                if mm and mm.group(1) not in pks: pks.append(mm.group(1))
            else:
                pk_m = re.search(r'PRIMARY\s+KEY\s*\(([^)]+)\)', t, re.IGNORECASE)
                if pk_m:
                    for c in re.split(r'[,]', pk_m.group(1)):
                        cn = c.strip().strip('[]"')
                        if cn and cn not in pks: pks.append(cn)
        elif 'FOREIGN KEY' in tu:
            fk_m = re.search(
                r'FOREIGN\s+KEY\s*\(([^)]+)\)\s*REFERENCES\s+(\w+)\s*\(([^)]+)\)',
                t, re.IGNORECASE
            )
            if fk_m:
                fks.append({
                    'from': fk_m.group(1).strip().strip('[]"'),
                    'to_table': fk_m.group(2).strip().strip('[]"'),
                    'to_col': fk_m.group(3).strip().strip('[]"'),
                })
        elif re.match(r'\w+', t):
            cm = re.match(r'(\w+)\s+(.*)', t.strip(), re.IGNORECASE)
            if cm:
                cn = cm.group(1).strip()
                if cn.upper() not in {
                    'CONSTRAINT','PRIMARY','FOREIGN','UNIQUE','CHECK',
                    'DEFAULT','NOT','NULL','IDENTITY','INDEX'
                }:
                    cols.append({'name': cn})
    tables[tbl_name] = {'cols': cols, 'pks': pks, 'fks': fks}

for tbl, info in tables.items():
    existing = {c['name'] for c in info['cols']}
    for pk in info['pks']:
        if pk not in existing:
            info['cols'].insert(0, {'name': pk})

print(f"[+] Parsed: {len(tables)} tables, "
      f"{sum(len(t['fks']) for t in tables.values())} edges")

# ─────────────────────────────────────────────────────────
# 2. GROUPS & COLORS
# ─────────────────────────────────────────────────────────
GROUPS = {
    'users':     {'bg': '#dae8fc', 'hdr': '#6e8efb', 'tables': ['tai_khoan','nhan_vien','chuc_vu','khach_hang']},
    'products':  {'bg': '#fff2cc', 'hdr': '#f5a623', 'tables': ['san_pham','san_pham_hinh_anh','san_pham_phan_loai',
                'phan_loai','danh_muc','thuong_hieu','nha_cung_cap','dm_cpu','dm_ram','dm_gpu','dm_o_cung']},
    'variants':  {'bg': '#e1d5e7', 'hdr': '#9673a6', 'tables': ['bien_the_san_pham','chi_tiet_san_pham',
                'chi_tiet_cpu','chi_tiet_ram','chi_tiet_gpu','chi_tiet_o_cung']},
    'inventory': {'bg': '#d9e8d9', 'hdr': '#6e8e5e', 'tables': ['ton_kho','lich_su_ton_kho',
                'lich_su_thay_doi_san_pham','phieu_nhap_kho','chi_tiet_phieu_nhap']},
    'orders':    {'bg': '#f8cecc', 'hdr': '#b85450', 'tables': ['don_hang','chi_tiet_don_hang',
                'chi_tiet_don_hang_serial','lich_su_don_hang','thanh_toan','khuyen_mai',
                'dia_chi_giao_hang','san_pham_yeu_thich','danh_gia']},
    'returns':   {'bg': '#ffe6cc', 'hdr': '#d79b00', 'tables': ['phieu_tra_hang','chi_tiet_tra_hang','phieu_bao_hanh']},
    'loyalty':   {'bg': '#e6e6e6', 'hdr': '#666666', 'tables': ['dm_doi_thuong','phieu_giam_gia_ca_nhan',
                'lich_su_tang_diem','cau_hinh_vong_quay','lich_su_quay']},
    'system':   {'bg': '#f2f2f2', 'hdr': '#999999', 'tables': ['cai_dat_he_thong']},
}

def get_group(tbl):
    for g, i in GROUPS.items():
        if tbl in i['tables']: return g, i
    return 'system', GROUPS['system']

GROUP_ORDER = ['users','products','variants','inventory','orders','returns','loyalty','system']

# ─────────────────────────────────────────────────────────
# 3. FORCE-DIRECTED LAYOUT
# ─────────────────────────────────────────────────────────
W_BOX  = 180
H_HDR  = 22
H_ROW  = 15
MARGIN = 30

# Tính kích thước mỗi table
tbl_size = {}
for tbl, info in tables.items():
    h = H_HDR + len(info['cols']) * H_ROW
    tbl_size[tbl] = (W_BOX, h)

# Khởi tạo vị trí: gán nhóm nghiệp vụ vào cụm vị trí
group_centers = {
    'users':     (400,  500),
    'products':  (1000, 300),
    'variants':  (1400, 700),
    'inventory': (500,  1100),
    'orders':    (1300, 1400),
    'returns':   (2000, 1300),
    'loyalty':   (2300, 500),
    'system':   (2400, 100),
}

pos = {}
for tbl in tables:
    g, _ = get_group(tbl)
    cx, cy = group_centers.get(g, (1500, 800))
    # Random offset trong vùng nhóm
    pos[tbl] = [cx + random.uniform(-100, 100), cy + random.uniform(-100, 100)]

# Force-directed: spring (attract) cho edges, repulsion cho tất cả cặp
edges = []
for tbl, info in tables.items():
    for fk in info['fks']:
        if fk['to_table'] in tables and fk['to_table'] != tbl:
            edges.append((fk['to_table'], tbl))  # parent, child

ITERATIONS = 1000
REPULSION = 150000  # Tăng để bảng không đè
SPRING_K = 0.025
SPRING_LEN = 200
DAMPING = 0.88

velocity = {t: [0.0, 0.0] for t in tables}

for it in range(ITERATIONS):
    # Repulsion (mọi cặp nodes)
    force = {t: [0.0, 0.0] for t in tables}
    tbls = list(tables.keys())
    for i in range(len(tbls)):
        for j in range(i+1, len(tbls)):
            a, b = tbls[i], tbls[j]
            dx = pos[a][0] - pos[b][0]
            dy = pos[a][1] - pos[b][1]
            d2 = dx*dx + dy*dy + 1
            d = math.sqrt(d2)
            f = REPULSION / d2
            fx = f * dx / d
            fy = f * dy / d
            force[a][0] += fx; force[a][1] += fy
            force[b][0] -= fx; force[b][1] -= fy

    # Spring (edges)
    for a, b in edges:
        dx = pos[b][0] - pos[a][0]
        dy = pos[b][1] - pos[a][1]
        d = math.sqrt(dx*dx + dy*dy) + 0.01
        f = SPRING_K * (d - SPRING_LEN)
        fx = f * dx / d
        fy = f * dy / d
        force[a][0] += fx; force[a][1] += fy
        force[b][0] -= fx; force[b][1] -= fy

    # Pull về group center
    for tbl in tables:
        g, _ = get_group(tbl)
        cx, cy = group_centers.get(g, (1500, 800))
        force[tbl][0] += (cx - pos[tbl][0]) * 0.002
        force[tbl][1] += (cy - pos[tbl][1]) * 0.002

    # Cập nhật vị trí
    for tbl in tables:
        velocity[tbl][0] = (velocity[tbl][0] + force[tbl][0]) * DAMPING
        velocity[tbl][1] = (velocity[tbl][1] + force[tbl][1]) * DAMPING
        pos[tbl][0] += velocity[tbl][0]
        pos[tbl][1] += velocity[tbl][1]

# ── Tính bounding box ────────────────────────────────────
xs = [pos[t][0] for t in tables]
ys = [pos[t][1] for t in tables]
min_x, max_x = min(xs), max(xs)
min_y, max_y = min(ys), max(ys)
canvas_w = int(max_x - min_x + 2 * MARGIN + W_BOX)
canvas_h = int(max_y - min_y + 2 * MARGIN + 200)
shift_x = MARGIN - min_x
shift_y = MARGIN - min_y

# ─────────────────────────────────────────────────────────
# 4. BUILD SVG
# ─────────────────────────────────────────────────────────
NS = "http://www.w3.org/2000/svg"


def el(tag, attribs=None, text=None, parent=None):
    e = etree.Element(f"{{{NS}}}{tag}", attribs or {})
    if text: e.text = text
    if parent is not None: parent.append(e)
    return e


root = etree.Element(f"{{{NS}}}svg", nsmap={None: NS})
root.set('width', str(canvas_w))
root.set('height', str(canvas_h))
root.set('viewBox', f"0 0 {canvas_w} {canvas_h}")
root.set('font-family', 'Times New Roman')

# Background
el('rect', {
    'width': str(canvas_w), 'height': str(canvas_h),
    'fill': '#ffffff'
}, parent=root)

# Defs: arrow marker + arrow for FK
defs = el('defs', parent=root)
# Mũi tên cho FK (tam giác)
m1 = el('marker', {
    'id': 'arr', 'markerWidth': '10', 'markerHeight': '8',
    'refX': '10', 'refY': '4', 'orient': 'auto'
}, parent=defs)
el('polygon', {'points': '0 0, 10 4, 0 8', 'fill': '#222'}, parent=m1)

FK_COLS = {}
for tbl, info in tables.items():
    for fk in info['fks']:
        FK_COLS[(tbl, fk['from'])] = True

# ── Edges ────────────────────────────────────────────────
edge_group = el('g', {'stroke': '#333', 'stroke-width': '0.9',
                       'fill': 'none', 'marker-end': 'url(#arr)'}, parent=root)

# Final positions with shift
fp = {}  # final pos: (x_top_left, y_top_left, w, h)
for tbl in tables:
    w, h = tbl_size[tbl]
    fp[tbl] = (pos[tbl][0] + shift_x, pos[tbl][1] + shift_y, w, h)

for tbl, info in tables.items():
    for fk in info['fks']:
        dst = fk['to_table']
        if dst not in fp: continue
        x1, y1, w1, h1 = fp[tbl]
        x2, y2, w2, h2 = fp[dst]

        src_col_idx = next((i for i, ci in enumerate(info['cols'])
                            if ci['name'] == fk['from']), 0)
        dst_col_idx = next((i for i, ci in enumerate(tables[dst]['cols'])
                            if ci['name'] == fk['to_col']), 0)

        # Tính điểm nối: từ cạnh gần nhất
        cx1 = x1 + w1/2; cy1 = y1 + H_HDR + src_col_idx * H_ROW + H_ROW/2
        cx2 = x2 + w2/2; cy2 = y2 + H_HDR + dst_col_idx * H_ROW + H_ROW/2

        # Tìm cạnh gần nhất (top/bottom/left/right) giữa 2 điểm
        def edge_point(cx, cy, tx, ty, x, y, w, h):
            # Tìm điểm trên cạnh box (x,y,w,h) gần (tx,ty) nhất
            dx = tx - cx
            dy = ty - cy
            if dx == 0 and dy == 0: return (x + w/2, y + h/2)
            # Tham số t cho đường từ (cx,cy) đi đến (tx,ty) cắt box
            if dx != 0:
                t1 = (x - cx) / dx
                t2 = (x + w - cx) / dx
                if t1 > 0:
                    iy = cy + t1 * dy
                    if y <= iy <= y + h:
                        return (x if dx > 0 else x + w, iy)
                if t2 > 0:
                    iy = cy + t2 * dy
                    if y <= iy <= y + h:
                        return (x + w if dx > 0 else x, iy)
            if dy != 0:
                t1 = (y - cy) / dy
                t2 = (y + h - cy) / dy
                if t1 > 0:
                    ix = cx + t1 * dx
                    if x <= ix <= x + w:
                        return (ix, y if dy > 0 else y + h)
                if t2 > 0:
                    ix = cx + t2 * dx
                    if x <= ix <= x + w:
                        return (ix, y + h if dy > 0 else y)
            return (cx, cy)

        sx, sy = edge_point(cx2, cy2, cx1, cy1, x1, y1, w1, h1)
        tx, ty = edge_point(cx1, cy1, cx2, cy2, x2, y2, w2, h2)

        # Ortho routing với 2 đoạn ngang/dọc
        if abs(sx - tx) < 2 or abs(sy - ty) < 2:
            d = f"M {sx},{sy} L {tx},{ty}"
        else:
            d = f"M {sx},{sy} L {sx},{ty} L {tx},{ty}"
        el('path', {'d': d}, parent=edge_group)

# ── Tables ───────────────────────────────────────────────
for tbl in tables:
    if tbl not in fp: continue
    x, y, w, h = fp[tbl]
    grp, ginfo = get_group(tbl)
    info = tables[tbl]

    # Shadow
    el('rect', {
        'x': str(x+1.5), 'y': str(y+1.5),
        'width': str(w), 'height': str(h),
        'fill': '#888', 'opacity': '0.2'
    }, parent=root)

    # Header
    el('rect', {
        'x': str(x), 'y': str(y),
        'width': str(w), 'height': str(H_HDR),
        'fill': ginfo['hdr']
    }, parent=root)

    el('text', {
        'x': str(x + 5), 'y': str(y + H_HDR - 5),
        'font-size': '10', 'font-weight': 'bold', 'fill': '#000'
    }, text=tbl, parent=root)

    # Body
    el('rect', {
        'x': str(x), 'y': str(y + H_HDR),
        'width': str(w), 'height': str(h - H_HDR),
        'fill': ginfo['bg'], 'stroke': ginfo['hdr'], 'stroke-width': '0.8'
    }, parent=root)

    # Rows
    for i, ci in enumerate(info['cols']):
        rowy = y + H_HDR + i * H_ROW
        cn = ci['name']
        is_pk = cn in info['pks']
        is_fk = (tbl, cn) in FK_COLS
        if is_pk: bg = '#b8dafa'; fs = 'bold'
        elif is_fk: bg = '#dadada'; fs = 'italic'
        else: bg = '#fff'; fs = 'normal'

        el('rect', {
            'x': str(x+1), 'y': str(rowy),
            'width': str(w-2), 'height': str(H_ROW-1),
            'fill': bg, 'stroke': '#bbb', 'stroke-width': '0.3'
        }, parent=root)

        # PK icon: khóa vàng (to hơn cho dễ nhìn)
        if is_pk:
            # Lock shackle (curved)
            el('path', {
                'd': f'M {x+4},{rowy+H_ROW/2-1} L {x+4},{rowy+H_ROW/2-4.5} '
                     f'Q {x+7},{rowy+H_ROW/2-6.5} {x+10},{rowy+H_ROW/2-4.5} '
                     f'L {x+10},{rowy+H_ROW/2-1}',
                'stroke': '#b8730d', 'stroke-width': '1.2', 'fill': 'none'
            }, parent=root)
            # Lock body
            el('rect', {
                'x': str(x+3), 'y': str(rowy+H_ROW/2-1.5),
                'width': '8', 'height': '5',
                'fill': '#f39c12', 'stroke': '#b8730d', 'stroke-width': '0.6'
            }, parent=root)
        # FK icon: mũi tên xanh (to hơn)
        elif is_fk:
            el('path', {
                'd': f'M {x+3},{rowy+H_ROW/2-3} L {x+9},{rowy+H_ROW/2} '
                     f'L {x+3},{rowy+H_ROW/2+3} Z',
                'fill': '#2980b9', 'stroke': '#1f5f8b', 'stroke-width': '0.5'
            }, parent=root)

        # Column name
        text_x = x + 13 if (is_pk or is_fk) else x + 4
        el('text', {
            'x': str(text_x), 'y': str(rowy + H_ROW - 3),
            'font-size': '8', 'fill': '#000', 'font-weight': fs
        }, text=cn, parent=root)

# ── Legend ──────────────────────────────────────────────
leg_x = 30
leg_y = 30
leg_g = el('g', parent=root)

el('rect', {
    'x': str(leg_x-10), 'y': str(leg_y-20),
    'width': '180', 'height': '75',
    'fill': '#fafafa', 'stroke': '#888', 'rx': '4'
}, parent=leg_g)

el('text', {
    'x': str(leg_x), 'y': str(leg_y - 5),
    'font-size': '10', 'font-weight': 'bold'
}, text='CHÚ THÍCH:', parent=leg_g)

# PK legend
el('rect', {
    'x': str(leg_x+5), 'y': str(leg_y+5),
    'width': '14', 'height': '10', 'fill': '#b8dafa', 'stroke': '#888'
}, parent=leg_g)
# Lock icon in legend
el('rect', {
    'x': str(leg_x+6), 'y': str(leg_y+8),
    'width': '3.5', 'height': '4', 'fill': '#f39c12'
}, parent=leg_g)
el('text', {
    'x': str(leg_x+25), 'y': str(leg_y+13),
    'font-size': '9'
}, text='PK (Primary Key)', parent=leg_g)

# FK legend
el('rect', {
    'x': str(leg_x+5), 'y': str(leg_y+22),
    'width': '14', 'height': '10', 'fill': '#dadada', 'stroke': '#888'
}, parent=leg_g)
el('path', {
    'd': f'M {leg_x+6},{leg_y+24} L {leg_x+12},{leg_y+27} L {leg_x+6},{leg_y+30} Z',
    'fill': '#2980b9'
}, parent=leg_g)
el('text', {
    'x': str(leg_x+25), 'y': str(leg_y+30),
    'font-size': '9'
}, text='FK (Foreign Key)', parent=leg_g)

# Edge legend
el('line', {
    'x1': str(leg_x+5), 'y1': str(leg_y+43),
    'x2': str(leg_x+19), 'y2': str(leg_y+43),
    'stroke': '#333', 'stroke-width': '1'
}, parent=leg_g)
el('polygon', {
    'points': f'{leg_x+19},{leg_y+41} {leg_x+23},{leg_y+43} {leg_x+19},{leg_y+45}',
    'fill': '#333'
}, parent=leg_g)
el('text', {
    'x': str(leg_x+28), 'y': str(leg_y+46),
    'font-size': '9'
}, text='FK relationship', parent=leg_g)

# ─────────────────────────────────────────────────────────
# 5. SAVE SVG + HQ PNG
# ─────────────────────────────────────────────────────────
svg_path = Path(__file__).parent / 'ERD_total.svg'
svg_bytes = etree.tostring(root, xml_declaration=True, encoding='UTF-8', pretty_print=True)
svg_path.write_bytes(svg_bytes)
print(f"\n[+] SVG: {svg_path} ({len(svg_bytes):,} bytes)")
print(f"[+] Canvas: {canvas_w} x {canvas_h}")

import subprocess
sharp_script = f'''
const sharp = require('sharp');
const fs = require('fs');
const svg = fs.readFileSync({str(svg_path)!r}, 'utf-8');
const w = parseInt(svg.match(/width="(\\d+)"/)[1]);
const h = parseInt(svg.match(/height="(\\d+)"/)[1]);
console.log('Size:', w, 'x', h);
sharp(Buffer.from(svg), {{ density: 300 }})
  .resize(w*3, h*3).png().toFile({str(svg_path.with_name('ERD_total_hq.png'))!r})
  .then(() => console.log('HQ PNG:', fs.statSync({str(svg_path.with_name('ERD_total_hq.png'))!r}).size, 'bytes'));
sharp(Buffer.from(svg), {{ density: 144 }})
  .resize(w*2, h*2).png().toFile({str(svg_path.with_suffix('.png'))!r})
  .then(() => console.log('PNG:', fs.statSync({str(svg_path.with_suffix('.png'))!r}).size, 'bytes'));
'''
try:
    r = subprocess.run(['node', '-e', sharp_script], cwd=str(svg_path.parent),
                       check=True, capture_output=True, text=True, timeout=180)
    print(r.stdout)
except Exception as e:
    print(f"[!] PNG conversion: {e}")
