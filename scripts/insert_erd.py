#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Xóa tất cả sơ đồ quan hệ riêng lẻ (Hình 40-48), thay Hình 49 bằng ERD tổng mới.
Giữ lại các hình khác (Mockup, Activity Diagram, Use Case, ...).
"""
import re, shutil, os, zipfile, io, sys
from pathlib import Path

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

SRC     = Path(__file__).parent.parent / "BaoCao_SD16_SAOClub.docx"
BAK     = Path(__file__).parent.parent / "BaoCao_SD16_SAOClub.erd_bak.docx"
ERD_PNG = Path(__file__).parent / "ERD_total.png"
TMP     = Path(__file__).parent.parent / "BaoCao_SD16_SAOClub.tmp.docx"

# Các rId cần xóa (Hình 40-48)
REMOVE_RIDS = ["rId61", "rId62", "rId63", "rId64", "rId65",
               "rId66", "rId67", "rId68", "rId69"]

# rId cho Hình 49 (sẽ được thay bằng ERD mới)
REPLACE_RID = "rId70"

# Backup
shutil.copyfile(SRC, BAK)
print(f"[+] Backup: {BAK}")

with zipfile.ZipFile(SRC, "r") as zin:
    names = zin.namelist()
    doc_xml = zin.read("word/document.xml").decode("utf-8")
    rels_xml = zin.read("word/_rels/document.xml.rels").decode("utf-8")
    content_types_xml = zin.read("[Content_Types].xml").decode("utf-8")
    all_files = {n: zin.read(n) for n in names}

# ─────────────────────────────────────────────────────────
# 1. Đọc ERD PNG
# ─────────────────────────────────────────────────────────
erd_data = ERD_PNG.read_bytes()
print(f"[+] ERD PNG: {len(erd_data):,} bytes")

# ─────────────────────────────────────────────────────────
# 2. Tạo rId mới
# ─────────────────────────────────────────────────────────
existing_rids = [int(re.search(r'rId(\d+)', m.group(1)).group(1))
                 for m in re.finditer(r'Id="(rId\d+)"', rels_xml)]
max_rid = max(existing_rids) if existing_rids else 0
new_rid = f"rId{max_rid + 1}"
print(f"[+] New image rId: {new_rid}")

# ─────────────────────────────────────────────────────────
# 3. Thêm relationship
# ─────────────────────────────────────────────────────────
new_rel = f'<Relationship Id="{new_rid}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/erd_new.png"/>'
rels_xml = rels_xml.replace('</Relationships>', new_rel + '</Relationships>')

# ─────────────────────────────────────────────────────────
# 4. Content types (đảm bảo có PNG)
# ─────────────────────────────────────────────────────────
if 'Extension="png"' not in content_types_xml:
    ct_add = '<Default Extension="png" ContentType="image/png"/>'
    content_types_xml = content_types_xml.replace(
        '<Types xmlns', ct_add + '<Types xmlns', 1
    )

# ─────────────────────────────────────────────────────────
# 5. New drawing XML cho ERD mới
# ─────────────────────────────────────────────────────────
# PNG 5860x4936 → scale về ~5.5 inch wide trên A4
# 5.5 inch = 5029200 EMU, height theo tỉ lệ
w_emu = 5500000  # ~6 inch
# Lấy aspect ratio từ file PNG
try:
    from PIL import Image
    with Image.open(ERD_PNG) as img:
        pw, ph = img.size
except Exception:
    pw, ph = 5860, 4936
aspect = pw / ph
h_emu = int(w_emu / aspect)
print(f"[+] New image size: {w_emu} x {h_emu} EMU "
      f"({w_emu/914400:.1f} x {h_emu/914400:.1f} inches)")

new_drawing_xml = (
    f'<w:drawing xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" '
    f'xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" '
    f'xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" '
    f'xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture" '
    f'xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">'
    f'<wp:inline distT="0" distB="0" distL="0" distR="0">'
    f'<wp:extent cx="{w_emu}" cy="{h_emu}"/>'
    f'<wp:docPr id="1" name="ERD_Tong_The"/>'
    f'<a:graphic>'
    f'<a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">'
    f'<pic:pic>'
    f'<pic:nvPicPr>'
    f'<pic:cNvPr id="1" name="ERD_Tong_The"/>'
    f'<pic:cNvPicPr/>'
    f'</pic:nvPicPr>'
    f'<pic:blipFill>'
    f'<a:blip r:embed="{new_rid}"/>'
    f'<a:stretch><a:fillRect/></a:stretch>'
    f'</pic:blipFill>'
    f'<pic:spPr>'
    f'<a:xfrm><a:off x="0" y="0"/><a:ext cx="{w_emu}" cy="{h_emu}"/></a:xfrm>'
    f'<a:prstGeom prst="rect"><a:avLst/></a:prstGeom>'
    f'</pic:spPr>'
    f'</pic:pic>'
    f'</a:graphicData>'
    f'</a:graphic>'
    f'</wp:inline>'
    f'</w:drawing>'
)

# ─────────────────────────────────────────────────────────
# 6. Xử lý drawings
# ─────────────────────────────────────────────────────────
def find_drawing_by_rid(xml, rid):
    pattern = re.compile(
        r'<w:drawing\b(?:(?!<w:drawing).)*?r:embed="' + re.escape(rid) + r'"(?:(?!</w:drawing>).)*?</w:drawing>',
        re.DOTALL
    )
    return pattern.search(xml)

# Thay rId70 bằng ảnh mới
dr_replace = find_drawing_by_rid(doc_xml, REPLACE_RID)
if dr_replace:
    doc_xml = doc_xml[:dr_replace.start()] + new_drawing_xml + doc_xml[dr_replace.end():]
    print(f"[+] Replaced {REPLACE_RID} (Hình 49) with new ERD")

# Xóa các rId61-69
for rid in REMOVE_RIDS:
    m = find_drawing_by_rid(doc_xml, rid)
    if m:
        doc_xml = doc_xml[:m.start()] + doc_xml[m.end():]
        print(f"[+] Removed {rid}")

# ─────────────────────────────────────────────────────────
# 7. Xóa caption paragraphs cho Hình 40-48
# ─────────────────────────────────────────────────────────
print(f"\n[+] Removing caption paragraphs for Hình 40-48...")
for n in range(40, 49):
    fig = f"Hình {n}"
    p_re = re.compile(r'<w:p\b[^>]*>(?:(?!</w:p>).)*?<w:t[^>]*>([^<]*'
                      + re.escape(fig) + r'[^<]*)</w:t>(?:(?!</w:p>).)*?</w:p>', re.DOTALL)
    matches = list(p_re.finditer(doc_xml))
    print(f"  '{fig}': {len(matches)} paragraph(s)")
    for m in matches:
        doc_xml = doc_xml[:m.start()] + doc_xml[m.end():]

# ─────────────────────────────────────────────────────────
# 8. Thêm ảnh vào ZIP
# ─────────────────────────────────────────────────────────
all_files["word/media/erd_new.png"] = erd_data

# ─────────────────────────────────────────────────────────
# 9. Ghi file mới
# ─────────────────────────────────────────────────────────
all_files["word/document.xml"] = doc_xml.encode("utf-8")
all_files["word/_rels/document.xml.rels"] = rels_xml.encode("utf-8")
all_files["[Content_Types].xml"] = content_types_xml.encode("utf-8")

with zipfile.ZipFile(TMP, "w", zipfile.ZIP_DEFLATED) as zout:
    for name, data in all_files.items():
        zout.writestr(name, data)

os.replace(str(TMP), str(SRC))
print(f"\n[+] Done! Updated: {SRC}")
print(f"    Backup: {BAK}")
