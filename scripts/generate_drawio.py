#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Sinh ERD SAOClub dạng draw.io — dùng shape=table (1 cell/bảng):
  - Mỗi bảng = 1 <mxCell> shape=table duy nhất
  - Cell value = header|row1|row2...  (mỗi dòng = 1 cột)
  - Rất nhỏ gọn, collapse/expand được trong draw.io
  - Font: Times New Roman 9pt, màu theo nhóm
"""
import re, sys, io, html, math, random
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
from pathlib import Path

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
    for pk in info['pks']:
        if pk not in {c['name'] for c in info['cols']}:
            info['cols'].insert(0, {'name': pk})

print(f"[+] Parsed: {len(tables)} tables, "
      f"{sum(len(t['fks']) for t in tables.values())} FK edges")

# ─────────────────────────────────────────────────────────
# 2. GROUPS & COLORS
# ─────────────────────────────────────────────────────────
GROUPS = {
    'users':     {'bg': '#dae8fc', 'hdr': '#6e8efb', 'tables': ['tai_khoan','nhan_vien','chuc_vu','khach_hang']},
    'products':  {'bg': '#fff2cc', 'hdr': '#f5a623', 'tables': ['san_pham','san_pham_hinh_anh','san_pham_phan_loai','phan_loai','danh_muc','thuong_hieu','nha_cung_cap','dm_cpu','dm_ram','dm_gpu','dm_o_cung']},
    'variants':  {'bg': '#e1d5e7', 'hdr': '#9673a6', 'tables': ['bien_the_san_pham','chi_tiet_san_pham','chi_tiet_cpu','chi_tiet_ram','chi_tiet_gpu','chi_tiet_o_cung']},
    'inventory': {'bg': '#d9e8d9', 'hdr': '#6e8e5e', 'tables': ['ton_kho','lich_su_ton_kho','lich_su_thay_doi_san_pham','phieu_nhap_kho','chi_tiet_phieu_nhap']},
    'orders':    {'bg': '#f8cecc', 'hdr': '#b85450', 'tables': ['don_hang','chi_tiet_don_hang','chi_tiet_don_hang_serial','lich_su_don_hang','thanh_toan','khuyen_mai','dia_chi_giao_hang','san_pham_yeu_thich','danh_gia']},
    'returns':   {'bg': '#ffe6cc', 'hdr': '#d79b00', 'tables': ['phieu_tra_hang','chi_tiet_tra_hang','phieu_bao_hanh']},
    'loyalty':   {'bg': '#e6e6e6', 'hdr': '#666666', 'tables': ['dm_doi_thuong','phieu_giam_gia_ca_nhan','lich_su_tang_diem','cau_hinh_vong_quay','lich_su_quay']},
    'system':   {'bg': '#f2f2f2', 'hdr': '#999999', 'tables': ['cai_dat_he_thong']},
}

def get_group(tbl):
    for g, i in GROUPS.items():
        if tbl in i['tables']: return g, i
    return 'system', GROUPS['system']

# ─────────────────────────────────────────────────────────
# 3. GRID LAYOUT THEO NHÓM
# ─────────────────────────────────────────────────────────
W_BOX = 190
H_HDR = 20
H_ROW = 14
COL_GAP = 30
ROW_GAP = 30
GROUP_GAP = 60  # khoảng cách giữa các nhóm
MARGIN = 30

tbl_size = {}
for tbl, info in tables.items():
    h = H_HDR + len(info['cols']) * H_ROW
    tbl_size[tbl] = (W_BOX, h)

# ── Grid layout THEO NHÓM (mỗi nhóm = 1 vùng có label riêng)
GROUP_ORDER = ['users', 'products', 'variants', 'inventory',
               'orders', 'returns', 'loyalty', 'system']

# Gom bảng theo nhóm, sort alpha
tables_by_group = {g: [] for g in GROUP_ORDER}
for tbl in tables:
    g, _ = get_group(tbl)
    tables_by_group[g].append(tbl)
for g in GROUP_ORDER:
    tables_by_group[g].sort()

# Tính layout: mỗi nhóm 1 vùng riêng, bảng xếp dọc trong nhóm
GROUP_LABEL_H = 28  # vùng label phía trên nhóm
GROUP_PAD = 20      # padding trong nhóm

pos = {}
group_boxes = {}  # g -> (x, y, w, h) của vùng nhóm

x_cursor = MARGIN
for g in GROUP_ORDER:
    tbls = tables_by_group[g]
    if not tbls:
        continue
    # Bắt đầu xếp từ y = group_top + label_h
    y_local = GROUP_LABEL_H + 10  # thêm 10 cho label nhãn
    for t in tbls:
        w, h = tbl_size[t]
        pos[t] = (x_cursor + GROUP_PAD, y_local + GROUP_PAD)
        y_local += h + 15

    group_h = y_local + GROUP_PAD
    group_w = W_BOX + GROUP_PAD * 2
    group_boxes[g] = (x_cursor, 0, group_w, group_h)
    x_cursor += group_w + 30  # gap giữa các nhóm

# Tính max_y của các group để set canvas, sau đó shift pos.y
max_group_h = max(bb[3] for bb in group_boxes.values()) if group_boxes else 0
shift_y = MARGIN
for tbl in pos:
    pos[tbl] = (pos[tbl][0], pos[tbl][1] + shift_y)

# Canvas
max_x = max((bb[0] + bb[2] for bb in group_boxes.values()), default=0)
CANVAS_W = max_x + MARGIN
CANVAS_H = max_group_h + shift_y + MARGIN

# Shift group_boxes too
group_boxes = {g: (bb[0], bb[1] + shift_y, bb[2], bb[3])
               for g, bb in group_boxes.items()}

fp = {}
for tbl in tables:
    w, h = tbl_size[tbl]
    x, y = pos[tbl]
    fp[tbl] = (x, y, w, h)

# ─────────────────────────────────────────────────────────
# 4. TRACK FK COLS
# ─────────────────────────────────────────────────────────
FK_SET = set()
for tbl, info in tables.items():
    for fk in info['fks']:
        FK_SET.add((tbl, fk['from']))

# ─────────────────────────────────────────────────────────
# 5. SINH DRAW.IO XML — shape=table (1 cell/bang)
# ─────────────────────────────────────────────────────────
cell_id = 1  # id=0 (root), id=1 (parent) reserved
cells_xml = []  # list of XML strings

def nid():
    global cell_id
    cell_id += 1
    return str(cell_id)

def esc(s):
    return html.escape(str(s))

def add_cell(parent, style, value, geo_xml):
    i = nid()
    cells_xml.append(
        f'<mxCell id="{i}" value="{esc(value)}" style="{style}" vertex="1" parent="{parent}">'
        f'{geo_xml}'
        f'</mxCell>'
    )
    return i

def geo(x, y, w, h, relative=0):
    if relative:
        return f'<mxGeometry relative="{relative}" as="geometry"/>'
    return f'<mxGeometry x="{x:.1f}" y="{y:.1f}" width="{w}" height="{h}" as="geometry"/>'

# Root
cells_xml.append('<mxCell id="0"/>')
cells_xml.append('<mxCell id="1" parent="0"/>')

# Legend
LEG_X, LEG_Y, LEG_W, LEG_H = 15, 15, 195, 100
add_cell("1",
    'shape=rectangle;fillColor=#fafafa;strokeColor=#888888;strokeWidth=1;rounded=0;',
    '', geo(LEG_X, LEG_Y, LEG_W, LEG_H))
add_cell("1",
    'fontColor=#000000;fontSize=10;fontStyle=1;align=left;verticalAlign=middle;fillColor=none;strokeColor=none;',
    'CHÚ THÍCH:', geo(LEG_X+8, LEG_Y+5, LEG_W-16, 18))
add_cell("1",
    'fontColor=#000000;fontSize=9;align=left;verticalAlign=middle;fillColor=none;strokeColor=none;',
    'PK - Khóa chính (nền xanh)', geo(LEG_X+8, LEG_Y+28, LEG_W-16, 16))
add_cell("1",
    'fontColor=#000000;fontSize=9;align=left;verticalAlign=middle;fillColor=none;strokeColor=none;',
    'FK - Khóa ngoại (nền xám)', geo(LEG_X+8, LEG_Y+50, LEG_W-16, 16))
add_cell("1",
    'fontColor=#000000;fontSize=9;align=left;verticalAlign=middle;fillColor=none;strokeColor=none;',
    'Quan hệ FK (đường nối)', geo(LEG_X+8, LEG_Y+72, LEG_W-16, 16))

# Bang: shape=table, 1 cell / bang
# style: shape=table;container=1;collapsible=1;childLayout=tableLayout;
#   fixedRows=1;rowLines=0;fontSize=9;align=left;verticalAlign=top;
#   spacingLeft=4;spacingTop=4;fontColor=#000000;
#   fillColor=...;strokeColor=...;strokeWidth=1;
# value: tenbang|PK col1 col2|FK col3|col4...
# Row 0 = header, Row 1+ = data rows

TABLE_ROW_IDS = {}  # tbl -> {col_idx: cell_id}

# ─── VẼ GROUP CONTAINERS (viền nét đứt + label) ───
# Vẽ TRƯỚC bảng để nằm dưới
GROUP_LABELS = {
    'users': 'NHÓM NGƯỜI DÙNG',
    'products': 'NHÓM SẢN PHẨM',
    'variants': 'NHÓM BIẾN THỂ',
    'inventory': 'NHÓM TỒN KHO',
    'orders': 'NHÓM ĐƠN HÀNG',
    'returns': 'NHÓM TRẢ HÀNG',
    'loyalty': 'NHÓM KHÁCH HÀNG THÂN THIẾT',
    'system': 'NHÓM HỆ THỐNG',
}

for g in GROUP_ORDER:
    if g not in group_boxes:
        continue
    gx, gy, gw, gh = group_boxes[g]
    ginfo = GROUPS[g]
    label = GROUP_LABELS.get(g, g.upper())

    # Vùng nền nhóm (rất nhạt)
    bg_style = (
        f'shape=rectangle;fillColor={ginfo["hdr"]};fillOpacity=8;'
        f'strokeColor={ginfo["hdr"]};strokeWidth=1.5;'
        f'dashed=1;rounded=1;arcSize=4;shadow=0;'
    )
    add_cell("1", bg_style, '', geo(gx, gy, gw, gh))

    # Label nhóm ở góc trên-trái
    label_style = (
        f'fontColor={ginfo["hdr"]};fontSize=11;fontStyle=1;'
        f'align=left;verticalAlign=middle;'
        f'fillColor=#ffffff;strokeColor={ginfo["hdr"]};strokeWidth=1;'
        f'rounded=1;arcSize=8;shadow=0;'
    )
    add_cell("1", label_style, label, geo(gx + 10, gy + 5, 200, 22))

for tbl in tables:
    if tbl not in fp:
        continue
    x, y, w, h = fp[tbl]
    grp, ginfo = get_group(tbl)
    info = tables[tbl]

    # Build table value: header|row1|row2...
    # Header
    rows_content = [esc(tbl)]

    # Columns
    for i, ci in enumerate(info['cols']):
        cn = ci['name']
        is_pk = cn in info['pks']
        is_fk = (tbl, cn) in FK_SET
        label = cn
        if is_pk: label = 'PK ' + label
        elif is_fk: label = 'FK ' + label
        rows_content.append(label)

    table_value = '\n'.join(rows_content)

    # Table style
    tbl_style = (
        f'shape=table;container=1;collapsible=1;childLayout=tableLayout;'
        f'recursiveResize=1;'
        f'fixedRows=1;rowLines=0;'
        f'fontSize=9;align=left;verticalAlign=middle;'
        f'spacingLeft=4;spacingTop=2;'
        f'fontColor=#000000;'
        f'fillColor={ginfo["bg"]};strokeColor={ginfo["hdr"]};strokeWidth=1;'
        f'columnResizable=1;'
    )

    tbl_id = nid()
    cells_xml.append(
        f'<mxCell id="{tbl_id}" value="{table_value}" style="{tbl_style}" vertex="1" parent="1">'
        f'{geo(x, y, w, h)}'
        f'</mxCell>'
    )

    # Row styles: header row + per-row styling
    # Row 0 (header) - header bg color
    hdr_row_style = (
        f'shape=tableRow;horizontal=0;'
        f'fillColor={ginfo["hdr"]};strokeColor={ginfo["hdr"]};strokeWidth=0.5;'
        f'fontColor=#000000;fontStyle=1;fontSize=9;align=left;verticalAlign=middle;'
        f'spacingLeft=4;spacingTop=2;'
    )
    hdr_row_id = nid()
    cells_xml.append(
        f'<mxCell id="{hdr_row_id}" value="" style="{hdr_row_style}" vertex="1" parent="{tbl_id}">'
        f'{geo(0, 0, w, H_HDR)}'
        f'</mxCell>'
    )

    TABLE_ROW_IDS.setdefault(tbl, {})[0] = hdr_row_id

    # Data rows
    for i, ci in enumerate(info['cols']):
        cn = ci['name']
        is_pk = cn in info['pks']
        is_fk = (tbl, cn) in FK_SET
        if is_pk: bg = '#b8dafa'; fs = '1'  # bold
        elif is_fk: bg = '#dadada'; fs = '2'  # italic
        else: bg = '#ffffff'; fs = '0'  # normal

        row_style = (
            f'shape=tableRow;horizontal=0;'
            f'fillColor={bg};strokeColor=#bbbbbb;strokeWidth=0.3;'
            f'fontColor=#000000;fontStyle={fs};fontSize=8;'
            f'align=left;verticalAlign=middle;'
            f'spacingLeft=4;spacingTop=2;'
        )
        row_id = nid()
        rowy = H_HDR + i * H_ROW
        cells_xml.append(
            f'<mxCell id="{row_id}" value="" style="{row_style}" vertex="1" parent="{tbl_id}">'
            f'{geo(0, rowy, w, H_ROW)}'
            f'</mxCell>'
        )
        TABLE_ROW_IDS[tbl][i + 1] = row_id

# ─── EDGES ───
n_edges = 0
for tbl_name, info in tables.items():
    if tbl_name not in TABLE_ROW_IDS:
        continue
    for fk in info['fks']:
        to_tbl = fk['to_table']
        if to_tbl not in tables or to_tbl not in TABLE_ROW_IDS:
            continue

        # Row index của FK col trong bảng nguồn (idx+1 vì row 0 = header)
        src_idx = next(
            (idx + 1 for idx, ci in enumerate(info['cols']) if ci['name'] == fk['from']),
            None
        )
        if src_idx is None:
            continue

        # Row index của PK trong bảng đích (luôn nối đến PK)
        dst_info = tables[to_tbl]
        # Tìm row index của PK đầu tiên (row sau header = pk_index + 1)
        # Nếu tên FK column trùng tên PK thì dùng, không thì dùng PK đầu tiên
        pk_idx = next(
            (idx for idx, ci in enumerate(dst_info['cols']) if ci['name'] in dst_info['pks']),
            0
        )
        dst_idx = pk_idx + 1  # +1 vì row 0 = header

        src_id = TABLE_ROW_IDS.get(tbl_name, {}).get(src_idx)
        dst_id = TABLE_ROW_IDS.get(to_tbl, {}).get(dst_idx)
        if not src_id or not dst_id:
            continue

        n_edges += 1
        edge_id = nid()
        cells_xml.append(
            f'<mxCell id="{edge_id}" value="" '
            f'style="endArrow=ERmany;startArrow=ERone;html=1;rounded=0;'
            f'strokeColor=#555555;strokeWidth=0.8;'
            f'edgeStyle=orthogonalEdgeStyle;'
            f'jettySize=auto;orthogonalLoop=1;'
            f'exitX=0.5;exitY=1;exitDx=0;exitDy=0;'
            f'entryX=0.5;entryY=0;entryDx=0;entryDy=0;" '
            f'edge="1" parent="1" source="{src_id}" target="{dst_id}">'
            f'{geo(0, 0, 0, 0, relative=1)}'
            f'</mxCell>'
        )

# ─────────────────────────────────────────────────────────
# 6. GHI FILE
# ─────────────────────────────────────────────────────────
final_xml = '\n'.join(cells_xml)

mxfile = (
    '<?xml version="1.0" encoding="UTF-8"?>\n'
    '<mxfile host="app.diagrams.net" modified="2026-09-01T00:00:00.000Z" '
    'agent="SAOClub-ERD" version="24.0.0">\n'
    '<diagram name="ERD - SAOClub" id="erd-sao-club">\n'
    f'<mxGraphModel dx="{CANVAS_W*2}" dy="{CANVAS_H*2}" grid="1" gridSize="10" '
    'guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" '
    f'pageWidth="{CANVAS_W}" pageHeight="{CANVAS_H}" math="0" shadow="0">\n'
    '<root>\n'
    + final_xml + '\n'
    '</root>\n'
    '</mxGraphModel>\n'
    '</diagram>\n'
    '</mxfile>\n'
)

out = Path(__file__).parent / "ERD_total.drawio"
out.write_text(mxfile, encoding='utf-8')

import os
size = os.path.getsize(out)
print(f"\n[+] Draw.io: {out}")
print(f"[+] Canvas: {CANVAS_W} x {CANVAS_H}")
print(f"[+] File size: {size:,} bytes")
print(f"[+] Tables: {len(tables)}, Edges: {n_edges}")
print(f"[+] Total cells: {len(cells_xml)}")
