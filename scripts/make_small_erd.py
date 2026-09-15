#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tạo ERD draw.io nhỏ nhất để test"""
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

# Chi lay 4 bang co FK voi nhau
small_tbl_names = ['khach_hang', 'tai_khoan', 'chuc_vu', 'nhan_vien']
small_tables = {k: tables[k] for k in small_tbl_names if k in tables}
print("Tables:", list(small_tables.keys()))

GROUPS = {
    'khach_hang': {'bg': '#dae8fc', 'hdr': '#6e8efb'},
    'tai_khoan':  {'bg': '#dae8fc', 'hdr': '#6e8efb'},
    'chuc_vu':    {'bg': '#dae8fc', 'hdr': '#6e8efb'},
    'nhan_vien':   {'bg': '#dae8fc', 'hdr': '#6e8efb'},
}
def get_group(tbl):
    return GROUPS.get(tbl, {'bg': '#e6e6e6', 'hdr': '#999999'})

W, H_HDR, H_ROW = 160, 20, 14
pos = {
    'khach_hang': (50, 50),
    'tai_khoan':  (300, 50),
    'chuc_vu':    (550, 50),
    'nhan_vien':   (50, 300),
}

def esc(s):
    return html.escape(str(s))

cell_id = 1
def nid():
    global cell_id
    cell_id += 1
    return str(cell_id)

# Edge endpoint IDs
edge_endpoints = {}

all_cells = []
all_cells.append('<mxCell id="0"/>')
all_cells.append('<mxCell id="1" parent="0"/>')

for tbl in small_tables:
    x, y = pos[tbl]
    info = small_tables[tbl]
    ginfo = get_group(tbl)
    body_h = H_HDR + len(info['cols']) * H_ROW
    cont_id = nid()

    # Container
    all_cells.append(
        f'<mxCell id="{cont_id}" value="{esc(tbl)}" '
        f'style="group;rounded=0;shadow=0;fillColor=none;strokeColor=none;'
        f'fontStyle=1;fontSize=10;verticalAlign=top;align=left;spacingLeft=4;spacingTop=2;" '
        f'vertex="1" parent="1">'
        f'<mxGeometry x="{x}" y="{y}" width="{W}" height="{body_h}" as="geometry"/>'
        f'</mxCell>'
    )

    # Header
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

    # Body
    all_cells.append(
        f'<mxCell id="{nid()}" value="" '
        f'style="shape=rectangle;fillColor={ginfo["bg"]};strokeColor={ginfo["hdr"]};'
        f'strokeWidth=0.8;shadow=0;" '
        f'vertex="1" parent="{cont_id}">'
        f'<mxGeometry x="0" y="{H_HDR}" width="{W}" height="{body_h - H_HDR}" as="geometry"/>'
        f'</mxCell>'
    )

    # Rows
    for i, ci in enumerate(info['cols'][:5]):  # chi 5 cot
        rowy = H_HDR + i * H_ROW
        cn = ci['name']
        is_pk = cn in info['pks']
        is_fk = any(fk['from'] == cn for fk in info['fks'])
        bg = '#b8dafa' if is_pk else ('#dadada' if is_fk else '#ffffff')
        fs = 'fontStyle=1' if is_pk else ('fontStyle=2' if is_fk else '')
        text_x = 13 if (is_pk or is_fk) else 4

        row_cell_id = nid()
        all_cells.append(
            f'<mxCell id="{row_cell_id}" value="" '
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

        # Store edge endpoints (row_cell_id la endpoint)
        if is_pk:
            edge_endpoints[(tbl, cn, 'pk')] = row_cell_id
        if is_fk:
            edge_endpoints[(tbl, cn, 'fk')] = row_cell_id

# Edges
n_edges = 0
for tbl, info in small_tables.items():
    for fk in info['fks']:
        dst = fk['to_table']
        if dst not in small_tables:
            continue
        src_id = edge_endpoints.get((tbl, fk['from'], 'fk'))
        dst_id = edge_endpoints.get((dst, fk['to_col'], 'pk'))
        if not src_id or not dst_id:
            continue
        n_edges += 1
        all_cells.append(
            f'<mxCell id="{nid()}" value="" '
            f'style="endArrow=ERmany;startArrow=ERone;html=1;rounded=0;'
            f'strokeColor=#333333;strokeWidth=0.9;edgeStyle=entityRelationEdgeStyle;" '
            f'edge="1" parent="1" source="{src_id}" target="{dst_id}">'
            f'<mxGeometry relative="1" as="geometry"/>'
            f'</mxCell>'
        )

cells_xml = '\n'.join(all_cells)
CANVAS_W, CANVAS_H = 800, 500

mxfile = (
    '<?xml version="1.0" encoding="UTF-8"?>\n'
    '<mxfile host="app.diagrams.net">\n'
    '<diagram name="ERD Small Test" id="erd-small">\n'
    f'<mxGraphModel dx="{CANVAS_W*2}" dy="{CANVAS_H*2}" grid="1" gridSize="10" '
    'guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" '
    f'pageWidth="{CANVAS_W}" pageHeight="{CANVAS_H}" math="0" shadow="0">\n'
    '<root>\n'
    + cells_xml + '\n'
    '</root>\n'
    '</mxGraphModel>\n'
    '</diagram>\n'
    '</mxfile>\n'
)

out = Path(__file__).parent / "ERD_small_test.drawio"
out.write_text(mxfile, encoding='utf-8')

import os
print(f"Written: {out}")
print(f"Size: {os.path.getsize(out):,} bytes")
print(f"Tables: {len(small_tables)}, Edges: {n_edges}")
print(f"Cells: {len(all_cells)}")

# Quick verify
xml = out.read_text(encoding='utf-8')
ids = re.findall(r'<mxCell id="(\d+)"', xml)
print(f"IDs: {len(ids)} total, {len(set(ids))} unique, {len(ids)-len(set(ids))} dupes")
