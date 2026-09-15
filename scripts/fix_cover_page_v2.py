"""
Sửa lại trang bìa BaoCao_SD16_SAOClub.docx — V2.

V2 khắc phục 3 lỗi của V1:
1. Textbox gộp (TRƯỜNG..., BÁO CÁO...) quá hẹp -> tăng width.
2. Dãn dòng quá xa (đè chữ sinh viên) -> giảm STEP.
3. Bỏ in đậm cả label -> chỉ bỏ in đậm value, giữ in đậm label.
"""
import re
import shutil
import zipfile
import os
import time
from pathlib import Path

SRC = Path("BaoCao_SD16_SAOClub.docx")
BAK = Path("BaoCao_SD16_SAOClub.v1.bak.docx")  # backup của V1
TMP_OUT = Path("BaoCao_SD16_SAOClub.v2.docx")

# ----------------------------------------------------------------------------
# 1. Backup
# ----------------------------------------------------------------------------
shutil.copyfile(SRC, BAK)
print(f"Backup V1 -> {BAK}")

with zipfile.ZipFile(SRC, "r") as zin:
    document_xml = zin.read("word/document.xml").decode("utf-8")
    footer_files = [n for n in zin.namelist()
                    if n.startswith("word/footer") and n.endswith(".xml")]
    footer_xmls = {fn: zin.read(fn).decode("utf-8") for fn in footer_files}

# ----------------------------------------------------------------------------
# 2. Helpers
# ----------------------------------------------------------------------------
def find_rect_bounds(xml: str, tb_start: int):
    rect_open_re = re.compile(r'<v:rect\b[^>]*>')
    rect_close_re = re.compile(r'</v:rect>')
    open_pos = -1
    i = tb_start
    while i > 0:
        ms = list(rect_open_re.finditer(xml, max(0, i - 5000), i))
        if not ms:
            break
        last = ms[-1]
        closes = rect_close_re.findall(xml, last.start(), tb_start)
        if len(closes) == 0:
            return last.start(), rect_close_re.search(xml, tb_start).end()
        i = last.start()
    return None, None


def set_rect_attr(xml: str, text: str, attr: str, new_value: str) -> str:
    """Đổi giá trị attribute của <v:rect> chứa textbox có text.
    attr: 'width', 'top', 'left', 'height'.
    """
    pattern_text = re.escape(text)
    txbx_re = re.compile(
        r'<w:txbxContent>(?:(?!</w:txbxContent>).)*?<w:t[^>]*>'
        + pattern_text + r'</w:t>(?:(?!</w:txbxContent>).)*?</w:txbxContent>',
        re.DOTALL
    )
    m = txbx_re.search(xml)
    if not m:
        print(f"  [warn] text {text!r} not found")
        return xml
    rect_open, _ = find_rect_bounds(xml, m.start())
    if rect_open is None:
        return xml
    rect_open_end = xml.find(">", rect_open)
    rect_tag = xml[rect_open:rect_open_end]
    # Thay attribute
    pattern = re.compile(rf'({attr}:)(\d+)')
    new_tag, n = pattern.subn(
        lambda mm: mm.group(1) + new_value,
        rect_tag, count=1
    )
    if n == 0:
        print(f"  [warn] attribute {attr} not found in rect of {text!r}")
        return xml
    return xml[:rect_open] + new_tag + xml[rect_open_end:]


def set_run_bold_in_textbox(xml: str, text: str, bold: bool) -> str:
    """Set <w:b/> trong rPr của run chứa text trong textbox."""
    pattern_text = re.escape(text)
    txbx_re = re.compile(
        r'(<w:txbxContent>(?:(?!</w:txbxContent>).)*?<w:r>\s*<w:rPr>)(.*?)(</w:rPr>\s*<w:t[^>]*>'
        + pattern_text + r'</w:t>(?:(?!</w:txbxContent>).)*?</w:txbxContent>)',
        re.DOTALL
    )
    m = txbx_re.search(xml)
    if not m:
        print(f"  [warn] run for {text!r} not found")
        return xml
    rpr_content = m.group(2)
    # Remove existing <w:b/> hoặc <w:b w:val="..."/>
    new_rpr = re.sub(r'<w:b\s*/>', '', rpr_content)
    new_rpr = re.sub(r'<w:b\s+w:val="(?:true|1|on)"\s*/>', '', new_rpr)
    # Add <w:b/> nếu cần
    if bold:
        new_rpr = '<w:b/>' + new_rpr
    return xml[:m.start()] + m.group(1) + new_rpr + m.group(3) + xml[m.end():]


# ----------------------------------------------------------------------------
# 3. Tăng width của các textbox gộp
# ----------------------------------------------------------------------------
print("\n[Step 1] Tăng width cho các textbox đã gộp...")
# TRƯỜNG CAO ĐẲNG FPT POLYTECHNIC - cần đủ rộng cho toàn bộ text (~50 ký tự)
# Font sz=32, mỗi ký tự ~ 16pt = 320 twips. 50 ký tự * 320 = 16000 twips + padding
# Width mới: 50000 twips (≈ 8.7 inch)
document_xml = set_rect_attr(document_xml, "TRƯỜNG CAO ĐẲNG FPT POLYTECHNIC", "width", "50000")

# BÁO CÁO ĐỒ ÁN TỐT NGHIỆP - ~22 ký tự, font sz=26 -> ~416 twips/char
# Width cũ: 5519. Mới: 30000
document_xml = set_rect_attr(document_xml, "BÁO CÁO ĐỒ ÁN TỐT NGHIỆP", "width", "30000")

# ----------------------------------------------------------------------------
# 4. Dãn dòng lại với STEP nhỏ hơn (giảm đè chữ)
# ----------------------------------------------------------------------------
print("\n[Step 2] Điều chỉnh dãn dòng...")
# V1 đã cộng STEP=800 cho mỗi dòng (cộng dồn).
# Bây giờ reset: tìm vị trí gốc của từng rect, tính delta mới so với hiện tại.
# Gốc: Nguyễn Quang Hà = 46393. Hiện tại = 46393 (line_idx=0 -> delta=0).
#        Chuyên ngành = 48571. Hiện tại = 49371 (delta=+800). Gốc cách dòng 1 = 48571-46393 = 2178.
#        Phát triển = 48852. Hiện tại = 50452. Gốc cách = 48852-46393 = 2459.
#        SD-16 = 51156. Hiện tại = 53556. Gốc cách = 51156-46393 = 4763.
#        ...
#
# Mục tiêu: các dòng cách nhau ~600-800 twips thôi (≈ 11-14pt).
# Chiến lược: lấy top gốc của dòng đầu (Nguyễn Quang Hà), các dòng sau cách dòng trước 700 twips.

# Hàm: trả về top GỐC của từng dòng (từ backup V1)
with zipfile.ZipFile(BAK, "r") as z:
    bak_xml = z.read("word/document.xml").decode("utf-8")

# Lấy top gốc
def get_top(xml: str, text: str) -> int:
    pattern_text = re.escape(text)
    txbx_re = re.compile(
        r'<w:txbxContent>(?:(?!</w:txbxContent>).)*?<w:t[^>]*>'
        + pattern_text + r'</w:t>(?:(?!</w:txbxContent>).)*?</w:txbxContent>',
        re.DOTALL
    )
    m = txbx_re.search(xml)
    if not m:
        return None
    rect_open, _ = find_rect_bounds(xml, m.start())
    if rect_open is None:
        return None
    rect_tag = xml[rect_open:xml.find(">", rect_open)]
    top_m = re.search(r'top:(\d+)', rect_tag)
    return int(top_m.group(1)) if top_m else None

# Lấy top GỐC (từ file V1 đã sửa lần 1, top đã đúng từ file gốc)
# Từ output trước: dòng 0 = 46393 (gốc cũng 46393)
# Các dòng gốc (từ V1 backup):
# line 0: 46393 (Nguyễn Quang Hà)
# line 1: 48571 (Chuyên ngành)
# line 2: 48852 (Phát triển phần mềm)
# line 3: 51156 / 51181 (SD-16 + Nhóm thực hiện)
# line 4: 53766 / 53791 (Sinh viên thực hiện)
# line 5: 53992 (Lê Huy Đỗ)
# line 6: 56170 (Lê Anh Ngữ)
# line 7: 58820 (Nghiêm Việt Anh)
# line 8: 61412 (Nguyễn Thành Đạt)
# line 9: 64033 (Nguyễn Xuân Việt)
# line 10: 82148 (Hà Nội - 2026)
#
# Khoảng cách gốc giữa các dòng: ~2000-3000 twips (~35-52pt) — đã OK.
# V1 đã cộng thêm 800*i, gây cộng dồn. V2: trừ bớt delta đã cộng dồn.

# Tính delta cần trừ: line_idx * STEP * 1 (vì V1 đã cộng STEP * line_idx)
# Nhưng V1 chỉ cộng cho line_idx >= 1, và STEP = 800.
# => line 1: +800 -> cần -800
#    line 2: +1600 -> cần -1600
#    ...
#
# Tốt hơn: đặt lại top về giá trị gốc, rồi áp dụng STEP mới (chỉ +300 twips giữa các dòng).
# Lấy top gốc từ backup V1.

# Danh sách textbox tiêu biểu cho mỗi dòng (dùng textbox đầu tiên hoặc duy nhất)
line_representatives = [
    ("Nguyễn Quang Hà", 46393),     # gốc
    ("Chuyên", 48571),                # gốc
    ("Phát triển phần mềm", 48852),  # gốc
    ("SD-16", 51156),                  # gốc
    ("PH60048", 53766),                # gốc (Lê Huy Đỗ label ngang hàng)
    ("Lê Huy Đỗ", 53992),             # gốc
    ("Lê Anh Ngữ", 56170),            # gốc
    ("Nghiêm Việt Anh", 58820),       # gốc
    ("Nguyễn Thành Đạt", 61412),      # gốc
    ("Nguyễn Xuân Việt", 64033),      # gốc
    ("Hà", 82148),                    # gốc
]

# Mục tiêu: reset top về gốc (đã đúng), KHÔNG cộng thêm STEP.
# V1 đã cộng STEP * line_idx vào top. Ta cần trừ đi phần đó.
# Hiện tại top của "Chuyên" = 49371. Gốc = 48571. Delta hiện tại = +800.
# Cần reset về 48571.

print("  Reset top về giá trị gốc từ file V1...")
for text, orig_top in line_representatives:
    cur_top = get_top(document_xml, text)
    if cur_top is None:
        print(f"  [warn] {text!r} not found")
        continue
    if cur_top == orig_top:
        continue  # đã đúng
    delta = orig_top - cur_top
    print(f"  {text!r}: {cur_top} -> {orig_top} (delta={delta})")
    # Cộng delta vào rect
    pattern_text = re.escape(text)
    txbx_re = re.compile(
        r'<w:txbxContent>(?:(?!</w:txbxContent>).)*?<w:t[^>]*>'
        + pattern_text + r'</w:t>(?:(?!</w:txbxContent>).)*?</w:txbxContent>',
        re.DOTALL
    )
    m = txbx_re.search(document_xml)
    if m:
        rect_open, _ = find_rect_bounds(document_xml, m.start())
        if rect_open is not None:
            rect_open_end = document_xml.find(">", rect_open)
            rect_tag = document_xml[rect_open:rect_open_end]
            new_tag = re.sub(
                r'(top:)(\d+)',
                lambda mm: mm.group(1) + str(int(mm.group(2)) + delta),
                rect_tag, count=1
            )
            document_xml = document_xml[:rect_open] + new_tag + document_xml[rect_open_end:]

# ----------------------------------------------------------------------------
# 5. Restore in đậm cho các LABEL
# ----------------------------------------------------------------------------
print("\n[Step 3] Restore in đậm cho các label...")
# Label là: "Giảng", "viên", "hướng", "dẫn", ":", "Chuyên", "ngành", ":",
#            "Nhóm", "thực", "hiện", ":", "Sinh", "viên", "thực", "hiện", ":"
# KHÔNG in đậm cho: "Nguyễn Quang Hà", "Phát triển phần mềm", "SD-16",
#                     tên SV + MSSV, "Hà Nội - 2026"
#
# Lưu ý: "Giảng", "viên", ... được tách thành nhiều textbox. Mỗi textbox có 1 run riêng.
# Restore bold cho từng textbox.

label_texts = [
    "Giảng", "viên", "hướng", "dẫn", ":",
    "Chuyên", "ngành", ":",
    "Nhóm", "thực", "hiện", ":",
    "Sinh", "viên", "thực", "hiện", ":",
]
for text in label_texts:
    document_xml = set_run_bold_in_textbox(document_xml, text, True)
    # Lưu ý: nhiều textbox có cùng text (VD ":" xuất hiện 4 lần).
    # set_run_bold_in_textbox chỉ áp dụng cho match đầu tiên -> cần replace_all
    pass

# Để xử lý TẤT CẢ textbox ":" (và các label khác trùng lặp), dùng replace_all
print("\n  Bold cho tất cả textbox ':' (4 chỗ)...")
for _ in range(10):
    before = document_xml
    document_xml = set_run_bold_in_textbox(document_xml, ":", True)
    if document_xml == before:
        break

# Tương tự cho các label có thể xuất hiện nhiều lần
print("  Bold cho 'viên' (nhiều chỗ)...")
for _ in range(10):
    before = document_xml
    document_xml = set_run_bold_in_textbox(document_xml, "viên", True)
    if document_xml == before:
        break

print("  Bold cho 'thực'...")
for _ in range(10):
    before = document_xml
    document_xml = set_run_bold_in_textbox(document_xml, "thực", True)
    if document_xml == before:
        break

print("  Bold cho 'hiện'...")
for _ in range(10):
    before = document_xml
    document_xml = set_run_bold_in_textbox(document_xml, "hiện", True)
    if document_xml == before:
        break

# ----------------------------------------------------------------------------
# 6. Ghi file zip mới
# ----------------------------------------------------------------------------
print(f"\n[Step Z] Ghi file mới -> {TMP_OUT}")
with zipfile.ZipFile(SRC, "r") as zin, zipfile.ZipFile(
    TMP_OUT, "w", zipfile.ZIP_DEFLATED
) as zout:
    for item in zin.infolist():
        if item.filename == "word/document.xml":
            zout.writestr(item, document_xml)
        elif item.filename in footer_xmls:
            zout.writestr(item, footer_xmls[item.filename])
        else:
            zout.writestr(item, zin.read(item.filename))

def safe_replace(src: str, dst: str, retries: int = 5, delay: float = 1.0):
    for i in range(retries):
        try:
            os.replace(src, dst)
            return
        except PermissionError:
            if i < retries - 1:
                time.sleep(delay)
            else:
                shutil.copyfile(src, dst)
                os.unlink(src)
                return

safe_replace(str(TMP_OUT), str(SRC))
print(f"\nDone. Backup V1 at: {BAK}")
