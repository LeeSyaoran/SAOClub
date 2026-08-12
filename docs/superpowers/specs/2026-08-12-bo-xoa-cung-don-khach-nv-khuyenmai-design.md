# Bỏ xóa cứng cho Đơn hàng, Khách hàng, Nhân viên, Khuyến mãi

## Bối cảnh

Bấm "Xóa" trên các bảng Biến thể, Đơn hàng, Khách hàng ở admin thất bại khi bản ghi đã dính hóa đơn/giao dịch — do `KhachHangService.delete()` và `DonHangService.delete()` xóa cứng (`deleteById`) không hề kiểm tra ràng buộc khóa ngoại trước, nên vỡ với `DataIntegrityViolationException` ngay khi khách hàng đã có đơn hàng, hoặc đơn hàng đã có phiếu bảo hành/trả hàng/lịch sử liên quan còn sót lại.

Soi lại toàn bộ, **Biến thể sản phẩm** (`BienTheSanPhamService.delete()`) đã có sẵn pattern đúng: gọi `hasTransactionHistory()` trước, chặn xóa kèm thông báo "hãy chuyển trạng thái sang ngừng kinh doanh" nếu đã có giao dịch. **Chi tiết sản phẩm/serial** cũng đã guard qua điều kiện `trangThai == "trong_kho"`. Chỉ 4 bảng sau là xóa cứng vô điều kiện, không có guard nào: `DonHang`, `KhachHang`, `NhanVien`, `KhuyenMai`.

Quyết định: thay vì thêm guard theo pattern Biến thể (chặn + gợi ý), bỏ hẳn tính năng xóa cho 4 bảng này — vòng đời từ nay chỉ quản lý qua đổi trạng thái. Lý do chọn hướng này thay vì thêm guard: cả 4 form Sửa hiện có đều **đã sẵn** dropdown trạng thái hoạt động (xem Phạm vi), nên "xóa" chưa từng là cách hợp lệ duy nhất để thao tác vòng đời — chỉ là đường xóa cứng bị bỏ sót không rào.

## Phạm vi

**Bỏ cả backend endpoint lẫn nút UI** (2 vị trí xóa cứng, không có nơi gọi nào khác dùng service `delete()`/`remove()` này — đã grep xác nhận đúng 1 call site mỗi service):

| Entity | Backend xóa | Frontend xóa | Thay thế đã có sẵn |
|---|---|---|---|
| `KhachHang` | `KhachHangController.delete()` (`DELETE /api/khach-hang/delete/{id}`) + `KhachHangService.delete()` | `CustomersTable.vue`: hàm `deleteCustomer`, nút Xóa (dòng ~31-34, ~67) | `CustomerFormModal.vue` đã có `<select v-model="customerForm.trangThai">` (`active`/`inactive`) |
| `NhanVien` | `NhanVienController.delete()` (`DELETE /api/nhan-vien/delete/{id}`) + `NhanVienService.delete()` | `AdminPage.vue`: hàm `deleteStaff`, nút Xóa (dòng ~731-734, ~1437) | Form sửa nhân viên trong `AdminPage.vue` đã có select trạng thái (`active`/`inactive`) |
| `KhuyenMai` | `KhuyenMaiController.delete()` (`DELETE /api/khuyen-mai/delete/{id}`) + `KhuyenMaiService.delete()` | `AdminPage.vue`: hàm `deletePromo`, nút Xóa (dòng ~812-815, ~1373) | Form sửa khuyến mãi trong `AdminPage.vue` đã có select trạng thái (`active`/`inactive`) |

**CHỈ bỏ frontend, GIỮ NGUYÊN backend** (ngoại lệ quan trọng):

| Entity | Giữ lại | Chỉ bỏ |
|---|---|---|
| `DonHang` | `DonHangController.delete()` + `DonHangService.delete()` + `DonHangService.js` hàm `remove()` — **vẫn cần** cho rollback nội bộ khi checkout/POS lỗi giữa chừng (`CheckoutModal.vue:719`, `PosPanel.vue:549` gọi `DonHangService.remove(donHangId).catch(() => {})` để hủy đơn "pending" vừa tạo, chưa hề có hóa đơn) | `OrdersTable.vue`: hàm `deleteOrder`, nút Xóa (dòng ~657), prop `canDelete` (chỉ tồn tại để gate nút này — bỏ luôn cả khai báo `defineProps` lẫn mọi nơi truyền `:can-delete`) |

Thay thế cho Đơn hàng: modal "Cập nhật trạng thái" (`openOrderStatus`) đã cho chọn tự do mọi `trangThaiDonHang` kể cả `cancelled`, không bị giới hạn theo bước workflow tuần tự — dùng ngay, không cần sửa gì.

**Ngoài phạm vi, không đụng:** `BienTheSanPham` (đã đúng pattern), `ChiTietSanPham` (đã guard qua trạng thái), `NhaCungCap`, `DmDoiThuong` (phần thưởng đổi điểm), và toàn bộ ~24 controller khác có `@DeleteMapping` — không nằm trong 4 bảng người dùng đã chốt phạm vi.

**i18n dọn theo (`vi.js`/`en.js`, chỉ 2 file hiện có):** xóa các key chỉ phục vụ nút/hàm bị gỡ — `admin.customers.delete`, `admin.staff.delete`, `admin.promotions.delete`, `admin.orders.delete`, `admin.confirm.deleteCustomer/deleteStaff/deletePromo/deleteOrder` — **sau khi** xác nhận lại từng key không còn nơi nào khác dùng (đã grep sơ bộ, cần grep lại đúng lúc sửa để chắc không lệch dòng do file đổi).

## Cách làm

1. **Backend** — với `KhachHang`, `NhanVien`, `KhuyenMai`: xóa method `delete()` trong Controller (cả `@DeleteMapping`) và trong Service. `DonHang`: không đụng backend.
2. **Frontend service layer** — xóa hàm `remove()` trong `KhachHangService.js`, `NhanVienService.js`, `KhuyenMaiService.js`. `DonHangService.js`: giữ nguyên `remove()`.
3. **Frontend UI** — xóa hàm `deleteCustomer`/`deleteStaff`/`deletePromo`/`deleteOrder` và nút "Xóa" tương ứng trong `CustomersTable.vue`, `AdminPage.vue` (2 chỗ), `OrdersTable.vue`. Xóa prop `canDelete` khỏi `OrdersTable.vue` và mọi nơi component cha truyền `:can-delete`.
4. **i18n** — xóa các key liệt kê ở Phạm vi khỏi `vi.js` và `en.js`.

## Kiểm tra

Không có logic nghiệp vụ mới, chỉ gỡ code chết + xác nhận đường thay thế đã hoạt động:
- Backend: build lại (`mvn compile` hoặc tương đương) đảm bảo không còn tham chiếu tới các method đã xóa.
- Docker: restart container `backend`, xác nhận start sạch (không lỗi bean).
- Trình duyệt (app đang chạy qua Docker, `localhost:5173`): với mỗi bảng trong 4 bảng — xác nhận nút Xóa đã biến mất, và đổi trạng thái qua form Sửa vẫn lưu đúng. Với Đơn hàng: xác nhận nút Xóa biến mất khỏi bảng, nhưng luồng checkout lỗi giữa chừng (khó dựng lại thủ công) không cần test trực tiếp — chỉ cần xác nhận code `CheckoutModal.vue`/`PosPanel.vue` không bị đụng tới.
- Gọi thử `DELETE /api/khach-hang/delete/{id}` v.v. trực tiếp (Postman/curl) xác nhận trả 404/405 thay vì 200.

## Việc không làm trong spec này

- Không thêm guard kiểu "chặn + gợi ý đổi trạng thái" (như Biến thể) cho 4 bảng này — đã chọn hướng bỏ hẳn nút xóa thay vì thêm guard, vì thay thế qua trạng thái đã có sẵn.
- Không đụng đến `DonHang` ở tầng backend/service — chỉ gỡ đường vào từ admin UI.
- Không dọn dữ liệu rác hiện có trong DB (khách/đơn test còn sót) — nằm ngoài phạm vi, xử lý tay qua DB nếu cần.
- Không mở rộng sang các bảng khác ngoài 4 bảng đã chốt (Nhà cung cấp, Phần thưởng đổi điểm...).
