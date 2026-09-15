#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tao ERD draw.io trung binh (15 bang) de test"""
import re, sys, io, html, math, random
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
from pathlib import Path

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

# Lay 15 bang dau
tbl_list = list(tables.keys())[:15]
small_tables = {k: tables[k] for k in tbl_list}
print('Tables:', len(small_tables))

W, H_HDR, H_ROW = 160, 20, 14
def get_pos(i):
    row, col = divmod(i, 5)
    return (col * 220 + 30, row * 200 + 30)

def esc(s):
    return html.escape(str(s))

cell_id = 1
edge_endpoints = {}
all_cells = ['<mxCell id="0"/>', '<mxCell id="1" parent="0"/>']

def nid():
    global cell_id
    cell_id += 1
    return str(cell_id)

for ti, tbl in enumerate(small_tables):
    info = small_tables[tbl]
    gname, ginfo = get_group(tbl)
    x, y = get_pos(ti)
    body_h = H_HDR + len(info['cols']) * H_ROW
    cont_id = nid()

    all_cells.append(
        f'<mxCell id="{cont_id}" value="{esc(tbl)}" '
        f'style="group;rounded=0;shadow=0;fillColor=none;strokeColor=none;'
        f'fontStyle=1;fontSize=10;verticalAlign=top;align=left;spacingLeft=4;spacingTop=2;" '
        f'vertex="1" parent="1">'
        f'<mxGeometry x="{x}" y="{y}" width="{W}" height="{body_h}" as="geometry"/>'
        f'</mxCell>'
    )

    all_cells.append(
        f'<mxCell id="{nid()}" value="" '
        f'style="shape=rectangle;fillColor={ginfo["hdr"]};strokeColor=none;shadow=0;" '
        f'vertex="1" parent="{cont_id}">'
        f'<mxGeometry x="0" y="0" width="{W}" height="{H_HDR}" as="geometry"/>'
        f'</mxCell>'
    )
    all_cells.append(
        f'<mxCell id="{nid()}" value="{esc(tbl)}" '
        f'style="fontColor=#000000;fontSize=10;fontStyle=1;align=left;verticalAlign=middle;'
        f'fillColor=none;strokeColor=none;spacingLeft=4;" '
        f'vertex="1" parent="{cont_id}">'
        f'<mxGeometry x="0" y="0" width="{W}" height="{H_HDR}" as="geometry"/>'
        f'</mxCell>'
    )
    all_cells.append(
        f'<mxCell id="{nid()}" value="" '
        f'style="shape=rectangle;fillColor={ginfo["bg"]};strokeColor={ginfo["hdr"]};'
        f'strokeWidth=0.8;shadow=0;" '
        f'vertex="1" parent="{cont_id}">'
        f'<mxGeometry x="0" y="{H_HDR}" width="{W}" height="{body_h - H_HDR}" as="geometry"/>'
        f'</mxCell>'
    )

    for i, ci in enumerate(info['cols']):
        rowy = H_HDR + i * H_ROW
        cn = ci['name']
        is_pk = cn in info['pks']
        is_fk = any(fk['from'] == cn for fk in info['fks'])
        bg = '#b8dafa' if is_pk else ('#dadada' if is_fk else '#ffffff')
        fs = 'fontStyle=1' if is_pk else ('fontStyle=2' if is_fk else '')
        text_x = 13 if (is_pk or is_fk) else 4

        row_id = nid()
        all_cells.append(
            f'<mxCell id="{row_id}" value="" '
            f'style="shape=rectangle;fillColor={bg};strokeColor=#bbbbbb;'
            f'strokeWidth=0.3;shadow=0;" '
            f'vertex="1" parent="{cont_id}">'
            f'<mxGeometry x="1" y="{rowy}" width="{W-2}" height="{H_ROW-1}" as="geometry"/>'
            f'</mxCell>'
        )
        all_cells.append(
            f'<mxCell id="{nid()}" value="{esc(cn)}" '
            f'style="fontColor=#000000;fontSize=8;{fs};align=left;verticalAlign=middle;'
            f'fillColor=none;strokeColor=none;" '
            f'vertex="1" parent="{cont_id}">'
            f'<mxGeometry x="{text_x}" y="{rowy+2}" width="{W-text_x-4}" height="{H_ROW-4}" as="geometry"/>'
            f'</mxCell>'
        )
        if is_pk: edge_endpoints[(tbl, cn, 'pk')] = row_id
        if is_fk: edge_endpoints[(tbl, cn, 'fk')] = row_id

n_edges = 0
for tbl, info in small_tables.items():
    for fk in info['fks']:
        dst = fk['to_table']
        if dst not in small_tables: continue
        src = edge_endpoints.get((tbl, fk['from'], 'fk'))
        tgt = edge_endpoints.get((dst, fk['to_col'], 'pk'))
        if not src or not tgt: continue
        n_edges += 1
        all_cells.append(
            f'<mxCell id="{nid()}" value="" '
            f'style="endArrow=ERmany;startArrow=ERone;html=1;rounded=0;'
            f'strokeColor=#333333;strokeWidth=0.9;edgeStyle=entityRelationEdgeStyle;" '
            f'edge="1" parent="1" source="{src}" target="{tgt}">'
            f'<mxGeometry relative="1" as="geometry"/>'
            f'</mxCell>'
        )

cells_xml = '\n'.join(all_cells)
CANVAS_W, CANVAS_H = 1200, 700

mxfile = (
    '<?xml version="1.0" encoding="UTF-8"?>\n'
    '<mxfile host="app.diagrams.net">\n'
    '<diagram name="ERD Medium" id="erd-med">\n'
    f'<mxGraphModel dx="{CANVAS_W*2}" dy="{CANVAS_H*2}" grid="1" gridSize="10" '
    'guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" '
    f'pageWidth="{CANVAS_W}" pageHeight="{CANVAS_H}" math="0" shadow="0">\n'
    '<root>\n' + cells_xml + '\n</root>\n'
    '</mxGraphModel>\n'
    '</diagram>\n'
    '</mxfile>\n'
)

with open('ERD_medium_test.drawio', 'w', encoding='utf-8') as f:
    f.write(mxfile)

import os
print(f'Size: {os.path.getsize("ERD_medium_test.drawio"):,} bytes')
print(f'Cells: {len(all_cells)}, Edges: {n_edges}')
