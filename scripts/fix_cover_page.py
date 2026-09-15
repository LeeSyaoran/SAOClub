"""
Chỉnh sửa trang bìa + footer BaoCao_SD16_SAOClub.docx:
1. Trang bìa: Gộp các textbox từ rời rạc thành 1 dòng có space.
2. Trang bìa: Xóa textbox dòng 'Xây dựng Website bán máy tính – laptop SAOClub'.
3. Trang bìa: Bỏ in đậm tất cả text.
4. Trang bìa: Dãn dòng (tăng khoảng cách giữa các dòng thông tin giảng viên + sinh viên).
5. Footer: Đổi 'Xây dựng Website bán máy tính – laptop SAOClub' ->
           'Xây dựng website bán máy tính SAOClub' (toàn bộ footer).
"""
import re
import shutil
import zipfile
from pathlib import Path

SRC = Path("BaoCao_SD16_SAOClub.docx")
BAK = Path("BaoCao_SD16_SAOClub.bak.docx")
TMP_OUT = Path("BaoCao_SD16_SAOClub.fixed.docx")

# ----------------------------------------------------------------------------
# 1. Backup & load
# ----------------------------------------------------------------------------
shutil.copyfile(SRC, BAK)
print(f"Backup -> {BAK}")

with zipfile.ZipFile(SRC, "r") as zin:
    document_xml = zin.read("word/document.xml").decode("utf-8")
    footer_files = [n for n in zin.namelist()
                    if n.startswith("word/footer") and n.endswith(".xml")]
    footer_xmls = {fn: zin.read(fn).decode("utf-8") for fn in footer_files}

# ----------------------------------------------------------------------------
# 2. Helpers
# ----------------------------------------------------------------------------
def find_rect_bounds(xml: str, tb_start: int):
    """Tìm <v:rect ...> mở gần nhất phía trước tb_start (chưa đóng) và </v:rect> đóng."""
    rect_open_re = re.compile(r'<v:rect\b[^>]*>')
    rect_close_re = re.compile(r'</v:rect>')
    open_pos = -1
    open_match = None
    i = tb_start
    while i > 0:
        ms = list(rect_open_re.finditer(xml, max(0, i - 5000), i))
        if not ms:
            break
        last = ms[-1]
        closes = rect_close_re.findall(xml, last.start(), tb_start)
        if len(closes) == 0:
            open_pos, open_match = last.start(), last
            break
        i = last.start()
    if open_pos == -1:
        return None, None
    close_m = rect_close_re.search(xml, tb_start)
    if not close_m:
        return None, None
    return open_pos, close_m.end()


def remove_rect_for_text(xml: str, text: str) -> str:
    """Xóa <v:rect>...</v:rect> đầu tiên có textbox chứa text chính xác."""
    pattern_text = re.escape(text)
    txbx_re = re.compile(
        r'<w:txbxContent>(?:(?!</w:txbxContent>).)*?<w:t[^>]*>'
        + pattern_text + r'</w:t>(?:(?!</w:txbxContent>).)*?</w:txbxContent>',
        re.DOTALL
    )
    m = txbx_re.search(xml)
    if not m:
        print(f"  [warn] txbxContent với text {text!r} không tìm thấy")
        return xml
    rect_open, rect_close = find_rect_bounds(xml, m.start())
    if rect_open is None:
        print(f"  [warn] Không tìm thấy <v:rect> cho {text!r}")
        return xml
    print(f"  Remove rect {text!r}: pos {rect_open}..{rect_close}")
    return xml[:rect_open] + xml[rect_close:]


def shift_rect_top(xml: str, text: str, delta: int) -> str:
    """Tăng/giảm top: của <v:rect> chứa textbox với text."""
    pattern_text = re.escape(text)
    txbx_re = re.compile(
        r'<w:txbxContent>(?:(?!</w:txbxContent>).)*?<w:t[^>]*>'
        + pattern_text + r'</w:t>(?:(?!</w:txbxContent>).)*?</w:txbxContent>',
        re.DOTALL
    )
    m = txbx_re.search(xml)
    if not m:
        return xml
    rect_open, _ = find_rect_bounds(xml, m.start())
    if rect_open is None:
        return xml
    rect_open_end = xml.find(">", rect_open)
    rect_tag = xml[rect_open:rect_open_end]
    new_tag, n = re.subn(
        r'(top:)(\d+)',
        lambda mm: mm.group(1) + str(int(mm.group(2)) + delta),
        rect_tag, count=1
    )
    if n == 0:
        return xml
    return xml[:rect_open] + new_tag + xml[rect_open_end:]


# ----------------------------------------------------------------------------
# 3. Gộp textbox tiêu đề trường: TRƯỜNG CAO ĐẲNG FPT POLYTECHNIC
# ----------------------------------------------------------------------------
print("\n[Step 1] Gộp textbox TRƯỜNG CAO ĐẲNG FPT POLYTECHNIC...")
for word in ["POLYTECHNIC", "FPT", "ĐẲNG", "CAO"]:
    document_xml = remove_rect_for_text(document_xml, word)
document_xml = document_xml.replace(
    '<w:t>TRƯỜNG</w:t>',
    '<w:t xml:space="preserve">TRƯỜNG CAO ĐẲNG FPT POLYTECHNIC</w:t>',
    1
)

# ----------------------------------------------------------------------------
# 4. Gộp textbox tiêu đề báo cáo: BÁO CÁO ĐỒ ÁN TỐT NGHIỆP
# ----------------------------------------------------------------------------
print("\n[Step 2] Gộp textbox BÁO CÁO ĐỒ ÁN TỐT NGHIỆP...")
for word in ["NGHIỆP", "TỐT", "ÁN", "DỰ", "CÁO"]:
    document_xml = remove_rect_for_text(document_xml, word)
document_xml = document_xml.replace(
    '<w:t>BÁO</w:t>',
    '<w:t xml:space="preserve">BÁO CÁO ĐỒ ÁN TỐT NGHIỆP</w:t>',
    1
)

# ----------------------------------------------------------------------------
# 5. Xóa textbox dòng 'Xây dựng Website bán máy tính – laptop SAOClub'
# ----------------------------------------------------------------------------
print("\n[Step 3] Xóa textbox 'Xây dựng Website bán máy tính – laptop SAOClub'...")
document_xml = remove_rect_for_text(document_xml, "Xây dựng Website bán máy tính – laptop SAOClub")

# ----------------------------------------------------------------------------
# 6. Dãn dòng: xác định thứ tự "dòng thông tin" dựa trên thứ tự xuất hiện
#    của textbox đầu tiên của mỗi dòng.
#
#    Phương pháp: lấy tất cả <v:rect> trong cover (top > 30000, < 85000),
#    sort theo top tăng dần. Gom các rect có top rất gần nhau (delta < 200)
#    thành 1 "dòng". Mỗi dòng cộng dồn STEP vào top của tất cả rect trong dòng.
# ----------------------------------------------------------------------------
print("\n[Step 4] Dãn dòng (gom theo top, mỗi rect trong dòng cộng STEP*N)...")

# Lấy tất cả <v:rect> trong vùng cover (sau khi đã sửa ở bước 1-3)
COVER_TOP_MIN = 30000   # bỏ qua tiêu đề
COVER_TOP_MAX = 100000  # bỏ qua footer cover (Hà Nội - 2026)

rects = []
for m in re.finditer(r'<v:rect\b[^>]*>', document_xml):
    top_m = re.search(r'top:(\d+)', m.group(0))
    if not top_m:
        continue
    top = int(top_m.group(1))
    if not (COVER_TOP_MIN <= top <= COVER_TOP_MAX):
        continue
    rect_end = document_xml.find('</v:rect>', m.end())
    if rect_end == -1:
        continue
    block = document_xml[m.end():rect_end]
    texts = re.findall(r'<w:t[^>]*>([^<]+)</w:t>', block)
    full = "".join(texts).strip()
    if full:
        rects.append((top, m.start(), m.end(), full))

# Sort theo top
rects.sort(key=lambda x: (x[0], x[1]))

# Gom thành các dòng: rects có top cách nhau < 200 twips thuộc cùng dòng
# (đây là sai số của file gốc — `Nguyễn Quang Hà` top=46393 vs `Giảng` top=46411)
LINE_THRESHOLD = 200  # twips
lines = []  # mỗi phần tử: [(rect_open, rect_end), ...]
current = []
last_top = None
for top, ro, re_, txt in rects:
    if last_top is None or (top - last_top) <= LINE_THRESHOLD:
        current.append((ro, re_, top))
    else:
        lines.append(current)
        current = [(ro, re_, top)]
    last_top = top
if current:
    lines.append(current)

print(f"  Tìm được {len(lines)} dòng trong cover (top {COVER_TOP_MIN}..{COVER_TOP_MAX})")
for i, line in enumerate(lines):
    tops = [t for _, _, t in line]
    txts = []
    for ro, re_, _ in line:
        # Tìm text ngay trong rect này
        tx = re.findall(r'<w:t[^>]*>([^<]+)</w:t>', document_xml[ro:re_ + 500])
        txts.append("".join(tx)[:30])
    print(f"    line {i}: top={min(tops)}..{max(tops)} | {len(line)} rects | {txts}")

# Dãn dòng: mỗi dòng sau dòng đầu cộng thêm STEP vào top
# Nhưng phải cộng vào TẤT CẢ rect trong dòng để giữ alignment
STEP = 800  # twips (≈ 14 pt)

# Thực hiện dãn: với mỗi dòng, cộng STEP * line_index vào top của tất cả rect
# Xử lý theo thứ tự từ dưới lên để tránh ảnh hưởng index
for line_idx in range(len(lines) - 1, 0, -1):  # từ dòng cuối lên dòng 1
    line = lines[line_idx]
    delta = STEP * line_idx
    if delta == 0:
        continue
    for ro, re_, top in line:
        # Cộng delta vào top trong rect tag
        rect_open_end = document_xml.find(">", ro)
        rect_tag = document_xml[ro:rect_open_end]
        new_tag = re.sub(
            r'(top:)(\d+)',
            lambda mm: mm.group(1) + str(int(mm.group(2)) + delta),
            rect_tag, count=1
        )
        document_xml = document_xml[:ro] + new_tag + document_xml[rect_open_end:]

print(f"  Đã dãn {len(lines) - 1} dòng (mỗi dòng +{STEP} twips so với dòng trước)")

# ----------------------------------------------------------------------------
# 7. Bỏ in đậm toàn bộ vùng trang bìa
# ----------------------------------------------------------------------------
print("\n[Step 5] Bỏ in đậm vùng trang bìa...")
muc_luc_pos = document_xml.find("MỤC LỤC")
if muc_luc_pos == -1:
    cover_end = 270000
else:
    p_start = document_xml.rfind("<w:p ", 0, muc_luc_pos)
    if p_start == -1:
        p_start = document_xml.rfind("<w:p>", 0, muc_luc_pos)
    cover_end = p_start if p_start != -1 else muc_luc_pos

cover_xml = document_xml[:cover_end]
cover_xml = re.sub(r'<w:b\s*/>', '', cover_xml)
cover_xml = re.sub(r'<w:bCs\s*/>', '', cover_xml)
cover_xml = re.sub(r'<w:b\s+w:val="(?:true|1|on)"\s*/>', '', cover_xml)
cover_xml = re.sub(r'<w:bCs\s+w:val="(?:true|1|on)"\s*/>', '', cover_xml)
document_xml = cover_xml + document_xml[cover_end:]
print(f"  Removed bold in [0, {cover_end})")

# ----------------------------------------------------------------------------
# 8. Footer
# ----------------------------------------------------------------------------
print("\n[Step 6] Sửa footer...")
OLD_FOOTER = "Xây dựng Website bán máy tính – laptop SAOClub"
NEW_FOOTER = "Xây dựng website bán máy tính  SAOClub"
footer_modified = False
for fn in footer_xmls:
    if OLD_FOOTER in footer_xmls[fn]:
        footer_xmls[fn] = footer_xmls[fn].replace(OLD_FOOTER, NEW_FOOTER)
        footer_modified = True
        print(f"  Updated {fn}")
if not footer_modified:
    print(f"  [warn] Không tìm thấy footer text cần sửa")

# ----------------------------------------------------------------------------
# 9. Ghi file zip mới
# ----------------------------------------------------------------------------
print(f"\n[Step Z] Ghi file mới -> {TMP_OUT}")
with zipfile.ZipFile(SRC, "r") as zin, zipfile.ZipFile(
    TMP_OUT, "w", zipfile.ZIP_DEFLATED
) as zout:
    for item in zin.infolist():
        if item.filename == "word/document.xml":
            zout.writestr(item, document_xml)
        elif item.filename in footer_xmls and footer_modified:
            zout.writestr(item, footer_xmls[item.filename])
        else:
            zout.writestr(item, zin.read(item.filename))

# Move file mới vào SRC. Trên Windows, nếu file SRC đang bị lock,
# dùng os.replace hoặc copy + remove.
import os
import time

def safe_replace(src: str, dst: str, retries: int = 3, delay: float = 0.5):
    for i in range(retries):
        try:
            os.replace(src, dst)
            return
        except PermissionError:
            if i < retries - 1:
                time.sleep(delay)
            else:
                # Fallback: copy + remove
                shutil.copyfile(src, dst)
                os.unlink(src)
                return

safe_replace(str(TMP_OUT), str(SRC))
print(f"\nDone. Backup at: {BAK}")
