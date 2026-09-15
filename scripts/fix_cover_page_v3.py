"""
V3 - Simple & fast.

Chỉ làm 3 việc:
1. Tăng width cho 2 textbox gộp (TRƯỜNG..., BÁO CÁO...)
2. Reset top về gốc (giảm delta dãn dòng)
3. Bold cho các label

Cách tiếp cận: thao tác trực tiếp trên chuỗi, dùng str.replace() nhiều nhất có thể.
"""
import re
import shutil
import zipfile
import os
import time
from pathlib import Path

SRC = Path("BaoCao_SD16_SAOClub.docx")
BAK = Path("BaoCao_SD16_SAOClub.v2.bak.docx")
TMP_OUT = Path("BaoCao_SD16_SAOClub.v3.docx")

# Backup
shutil.copyfile(SRC, BAK)
print(f"Backup V2 -> {BAK}")

with zipfile.ZipFile(SRC, "r") as zin:
    document_xml = zin.read("word/document.xml").decode("utf-8")
    footer_xmls = {}
    for n in zin.namelist():
        if n.startswith("word/footer") and n.endswith(".xml"):
            footer_xmls[n] = zin.read(n).decode("utf-8")

# ----------------------------------------------------------------------------
# Step 1: Tăng width cho textbox gộp
# ----------------------------------------------------------------------------
# Match the exact opening tag of <v:rect> that contains "TRƯỜNG CAO ĐẲNG FPT POLYTECHNIC"
# Pattern: <v:rect ... width:NNNN ...> ... textbox ... </v:rect>

print("\n[1] Tăng width textbox gộp...")

# Regex anchored: tìm rect chứa text TRƯỜNG CAO ĐẲNG FPT POLYTECHNIC
# Dùng lookahead/lookbehind để thay thế width ngay trong opening tag của rect đó.
def replace_width_in_rect(xml: str, text_marker: str, new_width: str) -> str:
    """Tìm v:rect chứa textbox có text_marker, thay width trong opening tag."""
    # Tìm vị trí text
    pos = xml.find(text_marker)
    if pos == -1:
        print(f"  [warn] {text_marker!r} not found")
        return xml
    # Tìm <v:rect mở gần nhất trước pos
    rect_open_re = re.compile(r'<v:rect\b[^>]*>')
    last_rect = None
    for m in rect_open_re.finditer(xml[:pos]):
        last_rect = m
    if last_rect is None:
        return xml
    # Thay width
    rect_tag = last_rect.group(0)
    new_tag = re.sub(r'(width:)(\d+)', lambda mm: mm.group(1) + new_width, rect_tag, count=1)
    if new_tag == rect_tag:
        print(f"  [warn] width attribute not found")
        return xml
    print(f"  Width -> {new_width} for rect at pos {last_rect.start()} ({text_marker!r})")
    return xml[:last_rect.start()] + new_tag + xml[last_rect.end():]

document_xml = replace_width_in_rect(
    document_xml, "TRƯỜNG CAO ĐẲNG FPT POLYTECHNIC", "45000"
)
document_xml = replace_width_in_rect(
    document_xml, "BÁO CÁO ĐỒ ÁN TỐT NGHIỆP", "25000"
)

# ----------------------------------------------------------------------------
# Step 2: Reset top về giá trị gốc
# ----------------------------------------------------------------------------
print("\n[2] Reset top về giá trị gốc...")

# Top hiện tại (sau V1) -> top gốc (từ file backup gốc)
# Từ output trước:
TOP_FIXES = [
    # (text_to_find_in_textbox, original_top)
    # File gốc:
    # - Giảng/viên/hướng/dẫn/: cùng top=46411 (dòng label "Giảng viên hướng dẫn")
    # - Chuyên/ngành/: top=48571
    # - Phát triển phần mềm: top=48852
    # - SD-16: top=51156
    # - Nhóm/thực/hiện/: top=51181
    # - PH60048: top=53766
    # - Sinh/viên/thực/hiện/: top=53791
    # - Lê Huy Đỗ: top=53992
    # - Lê Anh Ngữ/PH60049: top=56170
    # - Nghiêm Việt Anh/PH59725: top=58820
    # - Nguyễn Thành Đạt/PH60102: top=61412
    # - Nguyễn Xuân Việt/PH59601: top=64033
    # - Nguyễn Quang Hà: top=46393
    # - Hà/Nội/-/2026: top=82148
    ("Nguyễn Quang Hà", 46393),
    ("Chuyên", 48571),
    ("ngành", 48571),
    (":", 48571),  # : of "Chuyên ngành :"
    ("Phát triển phần mềm", 48852),
    ("SD-16", 51156),
    ("Nhóm", 51181),
    ("thực", 51181),
    ("hiện", 51181),
    (":", 51181),  # : of "Nhóm thực hiện :"
    ("PH60048", 53766),
    ("Sinh", 53791),
    ("viên", 53791),
    ("thực", 53791),
    ("hiện", 53791),
    (":", 53791),  # : of "Sinh viên thực hiện :"
    ("Lê Anh Ngữ", 56170),
    ("Nghiêm Việt Anh", 58820),
    ("Nguyễn Thành Đạt", 61412),
    ("Nguyễn Xuân Việt", 64033),
    ("Hà", 82148),
]

def reset_top_in_rect(xml: str, text_marker: str, new_top: int, nth: int = 1) -> str:
    """Tìm rect chứa text_marker (lần xuất hiện thứ nth), set top=new_top."""
    pos = -1
    count = 0
    while True:
        pos = xml.find(text_marker, pos + 1)
        if pos == -1:
            return xml
        # Check if in textbox (between <w:txbxContent> and </w:txbxContent>)
        tb_start = xml.rfind("<w:txbxContent>", 0, pos)
        tb_end = xml.find("</w:txbxContent>", pos)
        if tb_start != -1 and tb_end != -1 and tb_end > pos:
            count += 1
            if count == nth:
                break
        # else continue
    # pos is now nth textbox-occurrence
    rect_open_re = re.compile(r'<v:rect\b[^>]*>')
    last_rect = None
    for m in rect_open_re.finditer(xml[:pos]):
        last_rect = m
    if last_rect is None:
        return xml
    rect_tag = last_rect.group(0)
    new_tag = re.sub(r'(top:)(\d+)', lambda mm: mm.group(1) + str(new_top), rect_tag, count=1)
    if new_tag == rect_tag:
        return xml
    return xml[:last_rect.start()] + new_tag + xml[last_rect.end():]

# Xử lý các textbox có thứ tự cụ thể (bao gồm cả nth)
# Với mỗi label xuất hiện nhiều lần, ta gọi với nth=1,2,...
# Format: (text_marker, nth, original_top)
TOP_FIXES = [
    ("Nguyễn Quang Hà", 1, 46393),
    ("Chuyên", 1, 48571),
    ("ngành", 1, 48571),
    (":", 1, 48571),     # 1st ":": "Chuyên ngành :"
    ("Phát triển phần mềm", 1, 48852),
    ("SD-16", 1, 51156),
    ("Nhóm", 1, 51181),
    ("thực", 1, 51181),
    ("hiện", 1, 51181),
    (":", 2, 51181),     # 2nd ":": "Nhóm thực hiện :"
    ("PH60048", 1, 53766),
    ("Sinh", 1, 53791),
    ("viên", 1, 53791),
    ("thực", 2, 53791),  # 2nd "thực": "Sinh viên thực hiện :"
    ("hiện", 2, 53791),  # 2nd "hiện"
    (":", 3, 53791),     # 3rd ":": "Sinh viên thực hiện :"
    ("Lê Anh Ngữ", 1, 56170),
    ("Nghiêm Việt Anh", 1, 58820),
    ("Nguyễn Thành Đạt", 1, 61412),
    ("Nguyễn Xuân Việt", 1, 64033),
    ("Hà", 1, 82148),
]

for text, nth, orig in TOP_FIXES:
    document_xml = reset_top_in_rect(document_xml, text, orig, nth=nth)
    print(f"  {text!r} (nth={nth}): top -> {orig}")

# ----------------------------------------------------------------------------
# Step 3: Restore bold cho các label
# ----------------------------------------------------------------------------
print("\n[3] Restore bold cho các label...")

# V1 đã bỏ hết <w:b/> trong vùng cover. Cần restore bold cho các textbox label.
# Cách: tìm mỗi textbox chứa label, thêm <w:b/> vào <w:rPr> của run.

def add_bold_to_textbox_by_run(xml: str, exact_run_text: str) -> str:
    """Tìm <w:r> có <w:t>EXACTLY == exact_run_text (không có text khác),
    và thêm <w:b/> vào <w:rPr> nếu chưa có.
    Lặp qua tất cả match."""
    count = 0
    # Pattern match mọi <w:r>...</w:r> chứa <w:t>exact</w:t>
    pattern = re.compile(
        r'(<w:r>\s*<w:rPr>)((?:(?!</w:rPr>).)*?)(</w:rPr>\s*<w:t[^>]*>'
        + re.escape(exact_run_text) + r'</w:t>\s*</w:r>)',
        re.DOTALL
    )
    while True:
        m = pattern.search(xml)
        if not m:
            break
        rpr_content = m.group(2)
        if '<w:b/>' in rpr_content or '<w:b ' in rpr_content:
            # Đã bold -> xóa match để tìm tiếp
            xml = xml[:m.start()] + ('__MATCHED_ALREADY__' + str(m.start())) + xml[m.end():]
            continue
        new_rpr = '<w:b/>' + rpr_content
        new_match = m.group(1) + new_rpr + m.group(3)
        xml = xml[:m.start()] + new_match + xml[m.end():]
        count += 1
    # Cleanup placeholder
    xml = re.sub(r'__MATCHED_ALREADY__\d+', '', xml)
    return xml, count


# Labels cần bold
LABELS = [
    "Giảng", "viên", "hướng", "dẫn", ":",
    "Chuyên", "ngành", ":",
    "Nhóm", "thực", "hiện", ":",
    "Sinh", "viên", "thực", "hiện", ":",
]

# Xử lý từng label
# Quan trọng: dùng chính xác text trong <w:t>...</w:t>, không phải find ':' (gây match base64)
for label in LABELS:
    document_xml, count = add_bold_to_textbox_by_run(document_xml, label)
    if count:
        print(f"  Bold {label!r}: {count} run(s)")
    else:
        print(f"  [warn] {label!r} not found")

# ----------------------------------------------------------------------------
# Ghi file zip
# ----------------------------------------------------------------------------
print(f"\n[Z] Ghi file mới -> {TMP_OUT}")
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
print(f"\nDone. Backup V2 at: {BAK}")
