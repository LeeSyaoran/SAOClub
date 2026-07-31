# Ghi nhận phương thức thanh toán khi bán hàng tại quầy (POS) — Design Spec

## Bối cảnh

Khi tạo đơn tại quầy (`PosPanel.vue` → `posPlaceOrder`), đơn được tạo thẳng với
`trangThaiDonHang: 'confirmed'`, `trangThaiThanhToan: 'paid'` — không có bước hỏi nhân
viên khách trả bằng gì, và **không có gì được ghi lại** để sau này biết khách đã thanh
toán bằng phương thức nào.

Bên online (`CheckoutModal.vue`), có 1 bước chọn phương thức (Tiền mặt/QR/Chuyển khoản)
nhưng — phát hiện quan trọng khi rà code — lựa chọn này (`selectedPayment`) **thuần UI**:
chỉ quyết định hiện thông báo hướng dẫn nào cho khách, không hề được gửi lên backend hay
lưu vào DB.

Backend đã có sẵn hạ tầng đầy đủ cho việc này nhưng chưa ai dùng: entity `ThanhToan`
(bảng `thanh_toan`), `ThanhToanController`/`Service`/`Repository`/`Request`/`Response`
đầy đủ CRUD, khóa `@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")` — comment
trong `ThanhToanController.java` ghi rõ: "chưa có service/component frontend nào gọi tới
controller này". Vì endpoint này **staff-only**, POS (màn hình staff-only) là nơi hợp lý
nhất để bắt đầu dùng nó — trang khách hàng (customer role) không gọi được endpoint này
nếu không mở rộng quyền riêng, nên việc "sửa luôn cả online" nằm ngoài phạm vi lần này.

## Kiến trúc

Chạm cả backend lẫn frontend, quy mô vừa (1 sửa nhỏ backend + 2 vùng frontend).

### A. UI chọn phương thức trong POS (inline, không phải modal)

Trong panel giỏ hàng bên phải (`PosPanel.vue`), thêm 1 khối "Phương thức thanh toán"
ngay cạnh khối "Mã khuyến mãi" đã có — cùng phong cách inline, không phải modal riêng
(giữ đúng mật độ 1-màn-hình hiện tại của POS, không thêm bước click mở/đóng modal).

4 nút dạng segmented-button (giống style filter "Tất cả trạng thái" đã dùng chỗ khác):
- 💵 Tiền mặt (`tien_mat`)
- 📱 QR (map vào `vnpay` — xem lý do chọn giá trị này ở phần B bên dưới)
- 🏦 Chuyển khoản (`chuyen_khoan`)
- 💳 Thẻ (`the_tin_dung`)

State mới: `posPaymentMethod = ref(null)`. Bắt buộc chọn 1 trước khi bấm "Tạo đơn hàng"
— nút này disable thêm điều kiện `!posPaymentMethod` (cạnh điều kiện `!posCart.length`/
`posPlacing` đã có), y hệt cách nút hiện đang bị khóa khi giỏ trống.

### B. Ghi vào bảng `thanh_toan` khi tạo đơn thành công

Trong `posPlaceOrder()`, sau khi vòng lặp thêm `chi_tiet_don_hang` cho tất cả dòng
giỏ hàng thành công (đúng vị trí hiện tại, trước khi reset form) — gọi thêm:

```
POST /api/thanh-toan
{
  donHangId,                       // id đơn vừa tạo
  ngayThanhToan: nowLocalIso(),
  phuongThucThanhToan: <map từ posPaymentMethod>,
  soTien: posGrandTotal.value,
  maGiaoDich: null,                // tiền mặt/thẻ tại quầy không có mã giao dịch
  trangThai: 'success',
  ghiChu: null,
}
```

Map UI → giá trị enum DB (`CK_tt_phuongthuc`): `tien_mat`→`tien_mat`,
`chuyen_khoan`→`chuyen_khoan`, `the_tin_dung`→`the_tin_dung`, QR→`vnpay` (QR quét mã ở
VN quy về ví/ngân hàng qua cổng, `vnpay` là giá trị enum sát nghĩa nhất trong 8 giá trị
cho phép — không dùng `chuyen_khoan` vì giá trị đó đã mang nghĩa riêng "chuyển khoản thủ
công" cho phương thức thứ 3).

**Rollback nếu lỗi**: nếu request tạo `ThanhToan` thất bại, xóa luôn đơn vừa tạo
(`DonHangService.remove`) rồi `refreshOrders()` — **y hệt pattern rollback đã có sẵn**
khi 1 dòng `chi_tiet_don_hang` lỗi giữa chừng (xem khối `try/catch` hiện tại trong
`posPlaceOrder`). Không để lại đơn "đã tạo" nhưng thiếu payment record.

### C. Sửa validate backend quá chặt

`ThanhToanRequest.java` hiện đánh dấu `maGiaoDich` và `ghiChu` là `@NotBlank` (bắt buộc
có nội dung), nhưng cột DB tương ứng (`ma_giao_dich`, `ghi_chu`) đều `NULL`-able. Vì
endpoint này trước giờ chưa từng được gọi thật, validate này chưa bao giờ bị test bằng
use-case thật — thanh toán tiền mặt/thẻ tại quầy hoàn toàn hợp lệ mà không có "mã giao
dịch". Đổi `@NotBlank` → bỏ hẳn (cho phép null) cho cả 2 field, khớp đúng với schema.

### D. Hiển thị lại cho admin xem

Modal "Chi tiết đơn hàng" (`OrdersTable.vue`) hiện chỉ hiện badge trạng thái thanh toán
(`trangThaiThanhToan`: unpaid/paid/...), không hiện đã thanh toán bằng phương thức gì.
Thêm:

- Backend: `ThanhToanRepository` thêm 1 query method lọc theo đơn (mirror
  `hienThiThanhToan()` đã có, thêm `WHERE t.donHang.id = :donHangId`); `ThanhToanController`
  thêm `GET /api/thanh-toan/don-hang/{donHangId}` trả `List<ThanhToanResponse>`.
- Frontend: file mới `Service/ThanhToanService.js` (theo đúng pattern
  `ChiTietDonHangService.js` — 1 hàm `getByDonHang(donHangId)` gọi `get()`).
- `OrdersTable.vue`: khi mở modal chi tiết đơn (`openOrderDetail`), gọi thêm
  `ThanhToanService.getByDonHang(donHangId)`, hiện phương thức đã dùng ngay cạnh badge
  trạng thái thanh toán hiện có (label + icon tương ứng — dùng lại icon 💵📱🏦💳 đã
  chọn ở phần A cho nhất quán). Nếu đơn không có record nào (đơn cũ tạo trước tính năng
  này, hoặc đơn online chưa được wire) — không hiện gì thêm, không báo lỗi.

## Phạm vi KHÔNG đổi

- Luồng online (`CheckoutModal.vue`) — `selectedPayment` vẫn thuần UI như hiện tại,
  không đổi gì (mở rộng backend cho phép khách hàng tự ghi `ThanhToan` là việc khác,
  ngoài phạm vi lần này).
- `trangThaiDonHang`/`trangThaiThanhToan` của đơn POS vẫn `confirmed`/`paid` ngay khi
  tạo — không đổi logic trạng thái đơn, chỉ thêm record `ThanhToan` đi kèm.
- Cổng xác định khách hàng, modal chọn cấu hình/màu, modal chọn serial, giữ đơn, mã
  khuyến mãi — không đụng tới.

## i18n

Cần thêm nhãn cho 4 phương thức + tiêu đề khối chọn, ở namespace `admin.pos.*` (tái
dùng nếu `checkout.*` đã có nhãn tương đương phù hợp ngữ cảnh — quyết định cụ thể khi
viết plan).

## Testing

Không có test frontend tự động (đã xác nhận từ trước). Backend: `ThanhToanRequest`
đổi validate — nên thêm 1 test đơn giản trong `ThanhToanServiceTest`/tương đương xác
nhận tạo `ThanhToan` với `maGiaoDich`/`ghiChu` = null vẫn thành công (test hiện có,
nếu có, cho endpoint này gần như chắc chắn chưa tồn tại vì endpoint chưa từng được
dùng — kiểm tra khi viết plan). Verify cuối cùng bằng Playwright: tạo 1 đơn POS chọn
"Tiền mặt", mở lại đơn đó ở "Chi tiết đơn hàng", xác nhận hiện đúng phương thức.
