# Cải thiện Bán hàng tại quầy (POS) — sub-project 1/6 từ note cải thiện Admin

## Bối cảnh

File `Note những thứ cần cải thiện.docx` liệt kê ~20 việc cần làm trên 6 khu vực Admin độc lập (POS, Đơn hàng, Sản phẩm, Biến thể, Khách hàng, Kho hàng). Quá lớn cho 1 spec — chẻ thành 6 sub-project, làm tuần tự theo đúng thứ tự trong note. Đây là sub-project đầu tiên: **Bán hàng tại quầy**.

Đã đọc toàn bộ `PosPanel.vue` (908 dòng) và đối chiếu `CheckoutModal.vue` (checkout online) để hiểu đúng hiện trạng trước khi thiết kế. Một việc trong note (mục 7 — ưu tiên giữ hàng tại quầy) sau khi kiểm tra code hoá ra **đã hoạt động đúng sẵn** (`SanPhamRepository.java:51` tính "còn X máy" bằng subquery COUNT real-time `trang_thai = 'trong_kho'`, tự động loại serial đang `giu_hang`; gán serial dùng pessimistic lock chống bán trùng) — người dùng xác nhận đây chỉ là giả định chưa kiểm chứng, đã loại khỏi phạm vi.

## Phạm vi — 6 việc

### 1. Bỏ hiển thị SKU + danh mục trên card sản phẩm

`PosPanel.vue:585-591` (card trong lưới sản phẩm bên trái) đang hiện:
```html
<div class="fw-semibold small text-light">{{ p.tenSanPham }}</div>
<div class="text-secondary" style="font-size:0.76rem;">{{ p.maSku }}</div>
<div class="text-secondary" style="font-size:0.75rem;">{{ p.tenThuongHieu }} · {{ p.tenDanhMuc }}</div>
```
Xóa 2 dòng `maSku` và `tenThuongHieu · tenDanhMuc`, chỉ giữ tên sản phẩm + giá.

### 2. Tìm không phân biệt dấu tiếng Việt

`PosPanel.vue:40-49` (`posProducts` computed) hiện so khớp `p.tenSanPham.toLowerCase().includes(q)` — gõ không dấu ("laptop dell") sẽ không khớp "Laptop Dell" có dấu.

Thêm hàm `boDauTiengViet(str)` (dùng `String.normalize('NFD')` + xóa combining diacritical marks + xóa riêng `đ/Đ`) trong `utils/adminFormat.js` (đã có sẵn các hàm format dùng chung ở đây). Áp hàm này cho cả `q` (từ khóa) và `p.tenSanPham`/`p.maSku` trước khi so khớp trong `posProducts`.

### 3. Chọn nhiều serial cùng lúc

`PosPanel.vue:834-858` (modal "Chọn serial") hiện là chọn 1 cái → `posSelectSerial(s)` thêm ngay vào giỏ + đóng modal. Muốn mua N máy cùng biến thể phải mở lại modal N lần.

Đổi thành chọn nhiều (checkbox), theo đúng pattern đã có ở modal "Xác nhận đóng gói" trong `OrdersTable.vue` (`xacNhanToggleSerial`/`chosenSerialIds`):
- Thêm `serialPickerChosenIds` (Set) — bấm vào 1 serial trong danh sách toggle chọn/bỏ chọn thay vì thêm ngay.
- Thêm nút "Thêm N máy vào giỏ" ở cuối modal — khi bấm, lặp qua các serial đã chọn, mỗi cái tạo 1 dòng trong `posCart` (giữ đúng cấu trúc `item` hiện có) + gọi `setSerialTrangThai(item, 'giu_hang')`, rồi đóng modal.
- Luồng "đổi serial" (nút 🔄, `serialPickerSwapChiTietId`) vẫn giữ hành vi chọn ĐÚNG 1 cái như cũ (đổi 1-đổi-1, không đổi thành multi) — chỉ luồng "thêm mới từ đầu" mới cho chọn nhiều.

### 4. Giao tận nơi tại quầy (thay cho phí vận chuyển vô nghĩa)

`PosPanel.vue:90` (`posFee`) và dòng hiển thị `admin.pos.shippingFeeLabel` (`PosPanel.vue:732`) hiện luôn tính/hiện phí kiểu online (miễn phí ≥300k, else 30k) dù đơn tại quầy luôn `kenhBan: "in_store"` và chuyển thẳng `"delivered"` ngay sau khi tạo (`PosPanel.vue:532-546`) — khách tự mang máy về, phí này vô nghĩa.

Thêm toggle 2 lựa chọn trong khung giỏ hàng (cạnh phần "Phương thức thanh toán"): **"Khách tự lấy"** (mặc định, ẩn hẳn dòng phí vận chuyển) / **"Giao tận nơi"** (hiện ô nhập địa chỉ + hiện dòng phí = `posFee` như cũ). Khi chọn "Giao tận nơi": gửi `diaChiGiaoHangText` là địa chỉ vừa nhập (thay vì mặc định `"Tai cua hang"`) và **không** chuyển thẳng sang `"delivered"` lúc tạo đơn — dừng ở `"confirmed"`, đi qua luồng giao hàng tuần tự bình thường như đơn online (đã có sẵn state machine trong `DonHangService.kiemTraChuyenTrangThai`, không cần sửa backend). Khi chọn "Khách tự lấy": giữ nguyên hành vi hiện tại (chuyển `"delivered"` ngay, phí = 0, không hiện dòng phí).

### 5. Nút mở tab xem khuyến mãi

`PosPanel.vue:709-727` (khung nhập mã khuyến mãi) — thêm 1 nút icon (vd `ExternalLink` từ `@lucide/vue`) cạnh nút "Áp dụng", `@click` gọi `window.open('/#/admin', '_blank')` kèm query hoặc hash để AdminPage.vue mở sẵn tab "Khuyến mãi" (dùng lại cơ chế điều hướng tab hiện có của AdminPage — cần xem đúng cách AdminPage set `currentPage` từ URL, nếu chưa hỗ trợ qua URL thì mở thẳng `/#/admin` và chấp nhận nhân viên tự bấm sang tab Khuyến mãi).

### 6. QR chuyển khoản + giả lập đã quét

`utils/orderStatus.js:61`: `POS_PAYMENT_METHODS = ['tien_mat', 'vnpay', 'chuyen_khoan', 'the_tin_dung']` — bỏ `'vnpay'` (icon `Smartphone`, trùng ý nghĩa quét/chuyển khoản với `chuyen_khoan`), còn lại `['tien_mat', 'chuyen_khoan', 'the_tin_dung']`.

`PosPanel.vue:736-752` (khối chọn phương thức thanh toán) — khi `posPaymentMethod === 'chuyen_khoan'`, hiện thêm 1 khối QR ngay dưới, tái dùng đúng cách `CheckoutModal.vue:436-442` tạo `qrImageUrl` (VietQR API, `amount` = `posGrandTotal`, `addInfo` = tên khách/mã đơn tạm) + xử lý ảnh lỗi (`qrImageFailed`) y hệt logic đã có.

Thêm state `posQrScanned = ref(false)` (reset về `false` mỗi khi đổi phương thức thanh toán hoặc reset đơn) + nút **"Giả lập đã quét"** dưới mã QR — bấm mới set `true`. Nút "Tạo đơn" (`posPlaceOrder`) hiện chỉ disable khi thiếu giỏ hàng/khách hàng/phương thức thanh toán (`PosPanel.vue:766`) — thêm điều kiện: nếu `posPaymentMethod === 'chuyen_khoan'` thì bắt buộc `posQrScanned === true` mới bấm được.

## Không làm trong spec này

- Việc #7 (ưu tiên giữ hàng tại quầy) — đã xác nhận hoạt động đúng sẵn, không đụng.
- 5 khu vực còn lại trong note (Đơn hàng, Sản phẩm, Biến thể, Khách hàng, Kho hàng) — làm ở sub-project riêng, theo đúng thứ tự đã chốt.
- Không đổi luồng thanh toán `tien_mat`/`the_tin_dung` — chỉ thêm QR cho `chuyen_khoan`.
- Không xây webhook/tích hợp cổng thanh toán thật — "giả lập đã quét" là nút xác nhận thủ công cho nhân viên, đúng tinh thần "tạo chức năng quét giả lập" trong note.

## Kiểm tra

Không có test tự động cho luồng UI này (dự án không có test frontend component-level, xem `__tests__/` chỉ có utils/services/stores) — verify bằng cách chạy app qua Docker, thao tác trực tiếp trên trình duyệt:
- Card sản phẩm: xác nhận không còn SKU/danh mục hiển thị.
- Gõ tìm không dấu, xác nhận vẫn ra đúng sản phẩm có dấu.
- Mở modal chọn serial, chọn 3-4 serial cùng lúc, xác nhận cả 3-4 dòng lên giỏ đúng.
- Bật "Giao tận nơi", xác nhận hiện ô địa chỉ + phí; tắt lại xác nhận ẩn phí, tổng tiền đúng.
- Bấm nút mở tab khuyến mãi, xác nhận mở đúng trang.
- Chọn "Chuyển khoản", xác nhận hiện mã QR thật (ảnh load được), nút "Tạo đơn" bị khoá tới khi bấm "Giả lập đã quét".
- Tạo 1 đơn tại quầy trọn vẹn (tiền mặt, giao tận nơi bật/tắt), xác nhận đơn lên đúng trong Đơn hàng.
