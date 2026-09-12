# Plan — Khung quét barcode trên Tab Bảo hành

## Context

Tab **Bảo hành** hiện tại ([`WarrantyPanel.vue`](FrontEnd/QLBanMayTinh/src/components/admin/WarrantyPanel.vue))
gồm 2 bảng: "Còn hạn bảo hành" (danh sách serial còn bảo hành — lấy từ
`GET /api/chi-tiet-san-pham/con-bao-hanh`) và "Phiếu bảo hành" (CRUD phiếu — lấy
từ `GET /api/phieu-bao-hanh`). Workflow hiện tại của nhân viên:

1. Khách đem máy đến → nhân viên tìm serial trong bảng "Còn hạn bảo hành"
   bằng cách cuộn trang hoặc gõ tay vào ô search.
2. Bấm "Tạo phiếu bảo hành" → modal mở sẵn form điền sẵn.

**Vấn đề khi dùng máy quét barcode:** Tab không có ô input chuyên cho scanner
phía trên. Khi quét, ký tự chỉ rơi vào ô search chung (gõ text vào
`filteredWarranty`) — không hiện rõ "đúng máy này không", không hiện đầy đủ
biến thể (CPU/RAM/GPU/ổ cứng), không có nút tạo phiếu ngay sau khi quét, và nếu
serial đã hết hạn bảo hành thì không có cách nào tra được (vì serial đó không
còn trong danh sách "Còn hạn").

**Mục tiêu:** Chèn một **khung tra cứu nhanh** phía trên cùng tab Bảo hành.
Khi nhân viên quét barcode (hoặc gõ tay và nhấn Enter), panel sẽ hiện **đầy đủ
thông tin máy + biến thể + khách hàng + đơn hàng**, kèm nút **"Tạo phiếu bảo
hành"** (mở modal đã điền sẵn, y như nút hiện tại) — xử lý được cả 3 trường
hợp: còn bảo hành / đã hết bảo hành / chưa từng bán (serial còn trong kho).

## Thiết kế tổng thể

Khung mới gồm 4 phần chính:

```
┌─ Khung tra cứu serial (alt-card) ─────────────────────────────────────┐
│ ┌─ Ô nhập ────────────────────────────────────────┐ ┌─ Nút ───────┐ │
│ │ [🔍 Quét hoặc gõ số serial...    ] [Enter]      │ │ 🔍 Tra cứu  │ │
│ └──────────────────────────────────────────────────┘ └──────────────┘ │
│                                                                       │
│ ── Empty state (khi chưa tra) ───────────────────────────────────────│
│   💡 Quét barcode bằng máy quét USB, hoặc gõ số serial rồi nhấn    │
│      Enter / bấm "Tra cứu". Mỗi lần quét sẽ tự động tra.            │
│                                                                       │
│ ── Loading ──────────────────────────────────────────────────────────│
│   ⏳ Đang tra cứu serial...                                          │
│                                                                       │
│ ── Không tìm thấy (màu đỏ nhạt) ────────────────────────────────────│
│   ❌ Không tìm thấy serial "XXX". Kiểm tra lại hoặc dùng nút          │
│      "Tạo phiếu thủ công" bên dưới.                                  │
│                                                                       │
│ ── Tìm thấy: dạng 2 cột ────────────────────────────────────────────│
│  ┌─ Cột trái: thông tin máy ──────┐ ┌─ Cột phải: biến thể ────────┐ │
│  │ Sản phẩm: HP Pavilion 15       │ │ SKU: HP-PAV15-I5-8G-SLV     │ │
│  │ Serial:  N24H300010            │ │ CPU:  Intel Core i5-1235U   │ │
│  │ Trạng thái: [Còn bảo hành]     │ │ RAM:  8GB DDR4              │ │
│  │ Ngày mua:  22/08/2026          │ │ Ổ cứng: 512GB SSD          │ │
│  │ Hết hạn:   22/08/2027 (348 ngày│ │ GPU:  Intel Iris Xe         │ │
│  │             còn / ĐÃ HẾT)      │ │ Màn hình: 15.6" FHD        │ │
│  │ BH:        12 tháng            │ │ Màu: Bạc                   │ │
│  │ Khách:     Nguyễn Minh Đức     │ │ Pin:  41Wh                 │ │
│  │ SĐT:       0956789012          │ │ Trọng lượng: 1.74 kg       │ │
│  │ Đơn hàng:  290512034ADA        │ │                             │ │
│  │ Ngày giao: 22/08/2026          │ │ (Các trường = "—" nếu null) │ │
│  └───────────────────────────────┘ └─────────────────────────────┘ │
│                                                                       │
│  ┌─ Footer actions ──────────────────────────────────────────────┐    │
│  │ [🛡 Tạo phiếu bảo hành]    [✕ Xóa tra cứu]                  │    │
│  └───────────────────────────────────────────────────────────────┘    │
│                                                                       │
│  ┌─ Lịch sử phiếu bảo hành (chỉ hiện khi có) ─────────────────────┐  │
│  │ #5  HP-PAV15-I5-8G-SLV  22/08/2026  Màn hình bị sọc   Đã xử lý │  │
│  │ #2  HP-PAV15-I5-8G-SLV  15/01/2026  Pin chai           Từ chối   │  │
│  └────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────────────┘
```

## Các quyết định thiết kế

### 1. Luồng xử lý khi quét

Máy quét barcode USB hoạt động như một bàn phím — gõ xong ký tự sẽ gửi
`Enter`. Do đó:

- Dùng `<input>` thường, bắt sự kiện `@keyup.enter` để tự động tra.
- Nút "Tra cứu" vẫn giữ cho case nhân viên gõ tay rồi bấm chuột.
- Sau khi tra xong, **KHÔNG** reset ô input — nhân viên có thể thấy lại serial
  vừa quét, hoặc sửa rồi tra lại. Chỉ reset khi bấm "Xóa tra cứu".

### 2. Xử lý 3 trường hợp dữ liệu

Cần 1 endpoint backend tra cứu serial theo `soSerial` (chính xác). Hiện tại
chỉ có `getUnderWarranty` (lọc theo ngày) và `getByBienThe` (theo biến thể) —
không có cách nào tra 1 serial bất kỳ.

**Endpoint mới (theo lựa chọn của user):**
`GET /api/phieu-bao-hanh/tra-cuu-serial?soSerial=XXX` — đặt trong
`PhieuBaoHanhController` vì phục vụ workflow bảo hành.

Trả về `WarrantyLookupResponse` (xem mục Backend bên dưới) — gồm đầy đủ:

- Thông tin serial: `chiTietId`, `soSerial`, `trangThai`, `ngayNhapKho`
- Thông tin biến thể: `bienTheId`, `maSku`, `barcode`, `giaBan`, `baoHanhThang`,
  CPU/RAM/GPU/Ổ cứng tên + thông số, màn hình, HĐH, pin, trọng lượng, màu,
  ảnh (`hinhAnhBienThe`)
- Thông tin sản phẩm: `sanPhamId`, `tenSanPham`, `maSanPham`
- Thông tin đơn hàng bán (nếu có): `donHangId`, `maDonHang`, `ngayGiaoThucTe`,
  `ngayHetBaoHanh` (tính sẵn)
- Thông tin khách hàng (nếu có): `khachHangId`, `tenKhachHang`, `soDienThoai`
- **Lịch sử phiếu bảo hành cũ của serial này** (`List<PhieuBaoHanhResponse>
  lichSuPhieuBaoHanh`) — số phiếu đã mở trước đó + trạng thái, giúp nhân
  viên biết máy đã sửa bao giờ chưa.

3 trường hợp:
- **Serial đã bán, còn hạn** → hiện badge xanh, nút "Tạo phiếu bảo hành"
  hoạt động bình thường (điền sẵn form).
- **Serial đã bán, hết hạn** (theo lựa chọn của user) → hiện badge đỏ
  "Hết bảo hành X ngày", nút "Tạo phiếu" **vẫn hoạt động** để ghi nhận sửa
  ngoài hạn — cùng hành vi như nút "Tạo phiếu thủ công" hiện tại.
- **Serial trong kho / chưa bán** (theo lựa chọn của user) → hiện badge
  xám "Chưa bán", **vẫn hiện đầy đủ thông tin máy** (sản phẩm, biến thể,
  CPU/RAM/...) để nhân viên xác nhận đúng serial, nhưng nút "Tạo phiếu"
  bị **disable** kèm tooltip "Serial chưa được bán — không có thông tin đơn
  hàng/khách hàng để tạo phiếu bảo hành".

### 3. Tận dụng sẵn

- `WarrantyPanel.vue` đã có sẵn `openCreateFromWarranty(w)` và `openCreateManual()` —
  tái sử dụng. Khi tra cứu ra serial đã bán, build một object có cùng shape
  với `WarrantyStatusResponse` rồi gọi `openCreateFromWarranty()`.
- Dùng theme `admin-list-theme.css` (`.alt-card`, `.alt-toolbar`, `.alt-search`,
  `.alt-btn--primary`, `.alt-tag`) — đã có sẵn, không thêm CSS mới ngoài vài
  class phụ trợ.
- Icon: dùng `@lucide/vue` (đã import ở component), thêm `ScanLine` cho ô
  nhập và `Cpu`/`MemoryStick`/`HardDrive`/`Monitor` cho cột biến thể.

### 4. Xử lý lỗi

- 404 (không tìm thấy) → hiện alert nhỏ trong khung, không popup toast.
- 500 / lỗi mạng → toast lỗi + giữ nguyên state input.
- Debounce 300ms khi gõ phím (chỉ auto-search khi đã ngừng gõ) để giảm số
  request, NHƯNG **Enter vẫn tra ngay** không debounce.

## Phạm vi thay đổi

### Backend

1. **Mới:** file
   [`response/WarrantyLookupResponse.java`](BackEnd/src/main/java/com/example/backend/response/WarrantyLookupResponse.java)
   — DTO tổng hợp các trường liệt kê ở mục 2 (gồm `lichSuPhieuBaoHanh`).
2. **Mới:** method `findBySoSerial(String)` trong
   [`ChiTietSanPhamRepository`](BackEnd/src/main/java/com/example/backend/repository/ChiTietSanPhamRepository.java)
   — query JOIN FETCH BienThe + SanPham + CPU/RAM/GPU/OCung, LEFT JOIN đơn
   hàng (nếu serial đã bán) để lấy thông tin KH + ngày giao. Bỏ filter
   `da_ban` để tra được serial bất kỳ trạng thái nào.
3. **Mở rộng:** [`PhieuBaoHanhRepository`](BackEnd/src/main/java/com/example/backend/repository/PhieuBaoHanhRepository.java)
   — method `findByChiTietIdOrderByNgayTiepNhanDesc(Integer chiTietId)` để
   load lịch sử phiếu bảo hành cũ (chỉ cần nếu repo chưa có).
4. **Mới:** method `traCuuSerial(String soSerial)` trong
   [`PhieuBaoHanhService`](BackEnd/src/main/java/com/example/backend/service/PhieuBaoHanhService.java)
   — gọi repo `ChiTietSanPhamRepository.findBySoSerial()`, nếu tìm thấy
   tính `ngayHetBaoHanh` (giống `getStillUnderWarranty`), load
   `lichSuPhieuBaoHanh` từ `PhieuBaoHanhRepository.findByChiTietIdOrderByNgayTiepNhanDesc()`,
   trả về DTO. Nếu không thấy, throw `EntityNotFoundException`.
5. **Mới:** endpoint
   `GET /api/phieu-bao-hanh/tra-cuu-serial?soSerial=XXX` trong
   [`PhieuBaoHanhController`](BackEnd/src/main/java/com/example/backend/controller/PhieuBaoHanhController.java)
   — trả về `ResponseEntity.ok(lookup)` hoặc
   `ResponseEntity.notFound().build()`.
6. **Mở rộng:** [`GlobalExceptionHandler`](BackEnd/src/main/java/com/example/backend/exception/GlobalExceptionHandler.java)
   — đảm bảo `EntityNotFoundException` trả về 404 (đã có sẵn pattern với
   các entity khác, kiểm tra & thêm nếu thiếu).

### Frontend

1. **Mở rộng:** [`PhieuBaoHanhService.js`](FrontEnd/QLBanMayTinh/src/services/PhieuBaoHanhService.js)
   — thêm `lookupBySerial(serial) => get('/api/phieu-bao-hanh/tra-cuu-serial?soSerial=' + encodeURIComponent(serial))`.
2. **Mở rộng:** [`WarrantyPanel.vue`](FrontEnd/QLBanMayTinh/src/components/admin/WarrantyPanel.vue):
   - Thêm state: `serialInput`, `lookupResult`, `lookupLoading`, `lookupError`.
   - Thêm method `lookupSerial()` — gọi service, set state.
   - Thêm method `clearLookup()`.
   - Thêm method `createClaimFromLookup()` — build object shape giống
     `WarrantyStatusResponse` rồi gọi `openCreateFromWarranty()`.
   - Thêm template **khung tra cứu** phía trên bảng "Còn hạn bảo hành",
     gồm:
     - Ô input + nút "Tra cứu" (hỗ trợ Enter).
     - Empty state hướng dẫn quét/gõ.
     - Loading / Error state.
     - Panel kết quả 2 cột (thông tin máy + biến thể + giá bán + ảnh) +
       danh sách "Lịch sử phiếu bảo hành cũ" (nếu có) + footer nút.
   - Nút "Tạo phiếu bảo hành" trong footer:
     - `lookupResult.trangThai === 'da_ban'` → enable.
     - Còn lại (trong kho, giu_hang) → disable với tooltip.
   - Import thêm icon `ScanLine`, `Cpu`, `MemoryStick`, `HardDrive`, `Monitor`,
     `Tag`, `User`, `Phone`, `Package`, `ImageOff` từ `@lucide/vue`.
3. **Mở rộng:** i18n `vi.js` + `en.js` — thêm các key mới
   (`admin.warranty.scan.*`).
4. **Không cần đụng:** SQL (`Database/QLBanMayTinh.sql`) — query mới đọc từ
   các bảng đã có, không đổi schema.

### Files tham chiếu (giữ nguyên, chỉ đọc)

- [`WarrantyStatusResponse.java`](BackEnd/src/main/java/com/example/backend/response/WarrantyStatusResponse.java)
  — pattern để dựng `WarrantyLookupResponse`.
- [`WarrantyPanel.vue`](FrontEnd/QLBanMayTinh/src/components/admin/WarrantyPanel.vue)
  — `openCreateFromWarranty()` được tái sử dụng.
- [`admin-list-theme.css`](FrontEnd/QLBanMayTinh/src/assets/admin-list-theme.css)
  — class `.alt-card`, `.alt-search`, `.alt-btn--primary`, `.alt-tag`.

## Verification

1. **Backend compile & run:**
   - `./mvnw spring-boot:run` từ `BackEnd/` → app start, không lỗi.
2. **Test endpoint mới qua Postman/curl:**
   - Serial còn hạn (lấy từ bảng "Còn hạn bảo hành"): trả 200 + đầy đủ
     trường, `lichSuPhieuBaoHanh` (có thể rỗng).
   - Serial không tồn tại: trả 404.
   - Serial đã bán hết hạn (lấy từ `chi_tiet_don_hang` cũ): trả 200 +
     `ngayHetBaoHanh < now`, badge "Hết bảo hành", nút tạo vẫn hoạt động.
   - Serial trong kho (nhập kho chưa bán): trả 200 + không có
     `donHangId`/`khachHangId`, badge "Chưa bán", nút tạo disable.
   - Serial đã có 1-2 phiếu bảo hành cũ: trả `lichSuPhieuBaoHanh` không
     rỗng.
3. **Frontend:** vào tab Bảo hành với role Nhân viên (đã có trong memory
   [`feedback_multirole_testing`](../../../../../../Users/huydo/.claude/projects/d--project-code-SAOClub/memory/feedback_multirole_testing.md)).
   - Quét thử serial (hoặc gõ + Enter) → khung hiện đúng thông tin +
     ảnh biến thể (nếu có) + giá bán.
   - Tra serial đã có phiếu cũ → bảng lịch sử phiếu hiện bên dưới.
   - Bấm "Tạo phiếu bảo hành" → modal mở với form đã điền sẵn.
   - Tra serial không tồn tại → alert trong khung.
   - Tra serial chưa bán → nút "Tạo phiếu" disable, vẫn thấy biến thể.
   - **KHÔNG** bấm nút "In PDF" / `window.print()` trong modal (memory
     [`feedback_no_print_dialog_testing`](../../../../../../Users/huydo/.claude/projects/d--project-code-SAOClub/memory/feedback_no_print_dialog_testing.md)
     — verify qua đọc code thay vì click trực tiếp).
4. **Test 4 role song song 4 tab** theo memory đã lưu.

## Những gì BỊ BỎ (YAGNI)

- **Không làm:** lưu lịch sử các serial đã tra trong session. Tra xong đóng
  tab là mất — nhân viên tra lại nếu cần. Thêm tính năng này khi có nhu cầu
  thật.
- **Không làm:** tự động tạo phiếu sau khi quét (bỏ qua modal xác nhận).
  Modal cho phép nhân viên kiểm tra lại + sửa mô tả lỗi trước khi lưu — quan
  trọng hơn tốc độ.
- **Không làm:** webhook nhận diện máy quét barcode qua `WebUSB` /
  `HID API`. Đa số máy quét USB hoạt động như bàn phím, input + Enter là
  đủ.
- **Không thêm dependency mới** — chỉ dùng `<input>` thường, `@lucide/vue`
  đã có sẵn.
