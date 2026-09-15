# Plan: Sinh ERD tổng bằng draw.io + thay thế vào BaoCao_SD16_SAOClub.docx

## Context

User muốn thay thế **Hình 49** (ERD tổng thể cũ, 42 bảng) bằng **ERD mới** vẽ bằng draw.io, và **xóa luôn Hình 50/51/52** (3 sơ đồ ERD phân vùng).

**Yêu cầu đã xác nhận:**
- Sinh XML `.drawio` bằng Python (từ schema SQL `QLBanMayTinh.sql`)
- Thay Hình 49 hiện tại
- Xóa 3 sơ đồ ERD phân vùng (Hình 50, 51, 52) + caption tương ứng

## Các bước

### Step 1: Phân tích SQL schema → cấu trúc ERD
- Đọc `Database/QLBanMayTinh.sql`
- Regex lấy: tên bảng, cột (tên, kiểu, nullable, PK/FK, DEFAULT, CHECK), khóa ngoại (`REFERENCES`)
- Phát hiện các bảng nhiều-nhiều (N-N) bằng cách tìm bảng trung gian có 2 FK trỏ vào 2 bảng khác
- Từ đó suy ra: **các thực thể**, **thuộc tính**, **mối quan hệ** (1-N, N-N)

**Output:** Danh sách `Entity(name, cols, pk, fks)` và `Relation(from_table, from_col, to_table, to_col, type)`

### Step 2: Sinh file `ERD_total.drawio` (mxGraph XML)
- Mỗi bảng → 1 `<mxCell>` hình `swimlane` hoặc `table` (entity notation kiểu draw.io)
- Mỗi FK → 1 `<mxCell>` edge nối 2 cell
- Dùng kiểu ERD notation: đường thẳng gạch chân (1) hoặc ghi rõ cardinality (N)
- Tô màu phân vùng: Users (xanh), Products (cam), Orders (tím), Inventory (xanh lá), Rewards (đỏ)
- Kích thước canvas: đủ lớn cho ~30-35 bảng, dùng scroll trong draw.io

### Step 3: Xuất PNG từ draw.io
- Cách 1 (khuyến nghị): dùng draw.io portable command-line:
  ```
  draw.io.bat --export --format=png --output=ERD_total.png ERD_total.drawio
  ```
  (file `draw.io` hoặc `draw.io.bat` cần có sẵn)
- Cách 2: nếu không có draw.io CLI, sinh trực tiếp PNG bằng **matplotlib** với layout thủ công (mỗi bảng là 1 khối rect + text)

### Step 4: Cập nhật `.docx`
Đọc `BaoCao_SD16_SAOClub.docx` như zip:
1. **Thêm ảnh mới** vào `word/media/` — đặt tên `imageERD.png`
2. **Thêm relationship** trong `word/_rels/document.xml.rels` cho ảnh mới (rId mới)
3. **Thay thế drawing rId70** bằng reference mới tới `imageERD.png` (giữ nguyên kích thước `<a:ext>` 5715000×4749165)
4. **Xóa drawings rId71, rId72, rId73** khỏi `document.xml`
5. **Xóa các paragraph caption** "Hình 50: ...", "Hình 51: ...", "Hình 52: ..." trong vùng nội dung ERD (sau đoạn text mô tả)
6. **Dọn khoảng trắng thừa** sau khi xóa 3 ảnh

### Step 5: Ghi zip → `.docx` mới

## Files cần tạo/sửa

| File | Action |
|---|---|
| `scripts/generate_erd.py` | Script chính: parse SQL → sinh `.drawio` |
| `scripts/insert_erd.py` | Thay thế ảnh ERD trong `.docx` |
| `scripts/ERD_total.drawio` | Output: file draw.io để chỉnh sửa tay sau nếu cần |
| `scripts/ERD_total.png` | Output: ảnh ERD tổng |
| `BaoCao_SD16_SAOClub.docx` | In-place update |

## Verification

1. Mở `scripts/ERD_total.drawio` bằng diagrams.net (trình duyệt) — kiểm tra bố cục, màu sắc, font
2. Mở `BaoCao_SD16_SAOClub.docx` — xem trang ERD có đúng 1 ảnh (Hình 49) không
3. Kiểm tra phần mềm: so sánh số bảng trong ERD mới vs schema thực
