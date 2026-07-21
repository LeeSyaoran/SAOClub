# Trả hàng tự yêu cầu (khách hàng) — Design Spec

**Ngày:** 2026-07-21

## Bối cảnh

Backend `PhieuTraHangController`/`ChiTietTraHangController` + frontend `ReturnsPanel.vue` đã có sẵn từ trước (spec `2026-07-19-tra-hang-vi-khach-hang-design.md`) — nhưng chỉ dành cho nhân viên/admin tạo phiếu **hộ** khách. Khách hàng chưa có cách nào tự gửi yêu cầu trả hàng khi đã nhận đơn — đây là khoảng trống được yêu cầu bổ sung.

Hạ tầng liên quan đã có sẵn, không cần đổi: bảng `phieu_tra_hang`/`chi_tiet_tra_hang`, cơ chế cộng ví tự động khi phiếu chuyển `da_xu_ly` + `hinh_thuc_hoan='vi'` (`PhieuTraHangService.congViNeuVuaHoanTat()`), badge số dư ví đã hiện sẵn ở `AccountPage.vue`. Việc kiểm tra "chủ đơn hay không" cũng đã có tiền lệ mới thêm (`LichSuDonHangService.isStaffOrOwner`, spec mã vận đơn).

## Mục tiêu

1. Khách hàng tự gửi yêu cầu trả hàng cho đơn "Hoàn tất" (delivered) trong vòng 7 ngày kể từ ngày nhận hàng, chọn từng sản phẩm + số lượng muốn trả, kèm lý do.
2. Đơn có yêu cầu trả hàng đang active chuyển từ tab "Hoàn tất" sang tab "Đã hủy/Trả hàng", hiện trạng thái xử lý.
3. Không đụng/nới lỏng quyền của `PhieuTraHangController`/`ChiTietTraHangController` hiện có (vẫn khóa cứng staff-only) — thêm endpoint riêng, hẹp, tự suy khách hàng từ token.

## Phần 1 — Backend

### Endpoint mới (tách biệt hoàn toàn CRUD staff hiện có)

`PhieuTraHangController` thêm 2 method mới (không đổi class-level `@PreAuthorize` hiện có — 2 method mới này không có `@PreAuthorize` riêng, chỉ cần đăng nhập, giống `LichSuDonHangController`):

- `POST /api/phieu-tra-hang/tu-yeu-cau` — body `YeuCauTraHangRequest { donHangId, lyDo, dongTra: [{ chiTietDonHangId, soLuong }] }`.
- `GET /api/phieu-tra-hang/don-hang/{donHangId}` — trả `List<PhieuTraHangResponse>` (kèm dòng chi tiết) cho 1 đơn.

### `YeuCauTraHangRequest` (mới)

```java
public class YeuCauTraHangRequest {
    @NotNull private Integer donHangId;
    @NotBlank private String lyDo;
    @NotEmpty private List<DongTraRequest> dongTra; // { chiTietDonHangId, soLuong }
}
```

### `PhieuTraHangService.taoYeuCauTuKhachHang(Integer donHangId, YeuCauTraHangRequest request)` (mới)

1. Resolve khách hàng hiện tại qua `SecurityContextHolder` (đúng pattern `isStaffOrOwner` đã có) — **không tin `donHangId` gắn với khách nào từ client**, tự tra `donHang.getKhachHang()`.
2. Chặn nếu: đơn không thuộc khách này (`AccessDeniedException`); đơn không tồn tại (`IllegalArgumentException`); `trangThaiDonHang != "delivered"`; `ngayGiaoThucTe` null hoặc đã quá 7 ngày (`LocalDateTime.now().isAfter(ngayGiaoThucTe.plusDays(7))`); đơn đã có phiếu trả hàng nào ở trạng thái `cho_xu_ly` hoặc `da_xu_ly` (chặn gửi trùng — phiếu `tu_choi` thì cho gửi lại).
3. Với mỗi dòng `dongTra`: load `ChiTietDonHang` theo `chiTietDonHangId`, xác nhận thuộc đúng đơn, `soLuong` yêu cầu ≤ `soLuong` đã mua trừ đi số đã có trong các phiếu trả hàng active khác của cùng dòng đó (chống trả vượt số đã mua qua nhiều lần gửi).
4. Tạo `PhieuTraHang`: `trangThai="cho_xu_ly"`, `hinhThucHoan="vi"`, `nhanVien=null`, `ngayTra=now()`, `soTienHoan` = tổng `donGia * soLuong` các dòng chọn, `ghiChu=null`, `lyDo` từ request.
5. Tạo từng `ChiTietTraHang` tương ứng: `bienTheId`/`chiTietId` lấy từ `ChiTietDonHang`, `soLuong` theo request, `donGiaHoan` = `donGia` gốc, `tinhTrang=null` (nhân viên tự đánh giá khi nhận hàng trả về).
6. Không có bước cộng ví ở đây — phiếu luôn khởi tạo `cho_xu_ly`, cộng ví chỉ xảy ra khi nhân viên duyệt sang `da_xu_ly` qua `ReturnsPanel.vue` (luồng cũ, không đổi).

### `PhieuTraHangService.getByDonHang(Integer donHangId)` (mới)

Kiểm tra `isStaffOrOwner` (copy pattern từ `LichSuDonHangService`) rồi trả `phieuTraHangRepository.findByDonHang_Id(donHangId)` (map response kèm danh sách `ChiTietTraHangResponse` của từng phiếu).

## Phần 2 — Frontend

### Service mới

- `Service/PhieuTraHangService.js` (khách dùng, tách khỏi service admin nếu có, hoặc thêm 2 hàm `taoYeuCau`/`getByDonHang` vào cùng file service hiện tại nếu đã có — kiểm tra lúc viết plan).

### `ReturnRequestModal.vue` (mới)

Props: `{ order, items }` (items = `itemsByOrder[donHangId]` đã có sẵn ở `AccountPage.vue`). Liệt kê từng dòng, checkbox chọn + input số lượng (max = `soLuong` đã mua), 1 textarea lý do dùng chung cho cả phiếu. Submit gọi `PhieuTraHangService.taoYeuCau()`, đóng modal, `fetchData()` lại.

### `AccountPage.vue`

- Nút "Trả hàng" trên thẻ đơn "Hoàn tất": hiện khi `trangThaiDonHang === 'delivered'`, còn hạn 7 ngày (`ngayGiaoThucTe`), và đơn chưa có phiếu trả hàng active (`cho_xu_ly`/`da_xu_ly`) trong `returnsByOrder`.
- `fetchData()` thêm 1 lượt fetch song song `returnsByOrder` (giống `historyByOrder`) — `PhieuTraHangService.getByDonHang(donHangId)`.
- `TAB_STATUS_GROUPS`/`historyOrders`: đơn có phiếu trả hàng active (bất kể `trangThaiDonHang`) hiện ở tab "Đã hủy/Trả hàng" thay vì "Hoàn tất" — đổi điều kiện lọc tab từ thuần `trangThaiDonHang` sang kết hợp thêm `returnsByOrder[donHangId]`.
- Dòng đơn ở tab "Đã hủy/Trả hàng" hiện thêm badge trạng thái phiếu (Chờ xử lý/Đã xử lý/Từ chối — dùng lại 3 giá trị enum `cho_xu_ly`/`da_xu_ly`/`tu_choi` đã có) + khi mở rộng, hiện các dòng đã chọn trả + lý do.

## Ngoài phạm vi (Non-goals)

- Không cho khách chọn "tình trạng" (tốt/lỗi) — nhân viên tự đánh giá khi nhận hàng trả về, giữ nguyên luồng `ReturnsPanel.vue` xử lý tiếp sau khi khách gửi yêu cầu.
- Không cho khách chọn hình thức hoàn tiền mặt — luôn ép `vi`, đúng lý do đã chốt ở spec ví trước đó (không có nhân viên tại chỗ xác nhận).
- Không cho khách sửa/hủy yêu cầu đã gửi — nếu gửi nhầm, khách liên hệ nhân viên xử lý qua kênh khác (kênh này ngoài phạm vi).
- Không đổi `PhieuTraHangController`/`ChiTietTraHangController` (CRUD staff) hiện có — chỉ thêm method mới.

## Tự rà soát (self-review)

**1. Phủ đủ yêu cầu đã chốt:** chọn từng sản phẩm + số lượng ✅; hạn 7 ngày kể từ nhận hàng ✅; chuyển hẳn sang tab Đã hủy/Trả hàng, tách khỏi Hoàn tất ✅.

**2. Không còn placeholder** — mọi bảng/cột/method đã xác định chính xác qua đọc code thực tế (`PhieuTraHangService`, `PhieuTraHangRequest`, `ChiTietTraHangRequest`, `ChiTietDonHangRepository`).

**3. Nhất quán:** không đụng cơ chế cộng ví hiện có (`congViNeuVuaHoanTat` chỉ trigger khi `da_xu_ly`, phiếu khách tạo luôn khởi tạo `cho_xu_ly` nên an toàn tuyệt đối, không có đường nào khách tự cộng ví được).

**4. Bảo mật:** endpoint mới tự suy khách hàng từ `SecurityContextHolder`, không tin `donHangId`/số tiền hoàn từ client — số tiền hoàn luôn server tự tính từ đơn giá gốc, không nhận input số tiền từ khách.
