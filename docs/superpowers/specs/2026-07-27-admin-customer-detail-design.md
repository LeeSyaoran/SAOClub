# Trang chi tiết khách hàng (Admin) — thiết kế

Ngày: 2026-07-27
Trạng thái: Đã duyệt design, chờ viết plan triển khai

## Bối cảnh

Admin hiện chỉ sửa được thông tin cơ bản của khách hàng (`CustomerFormModal.vue`, gồm cả sửa
tay điểm `diemTichLuy`) và xóa khách hàng từ `CustomersTable.vue`. Không có nơi nào xem lịch sử
mua hàng, lịch sử đổi thưởng/trúng vòng quay, hay tặng điểm/tặng voucher riêng cho 1 khách hàng.

Yêu cầu: 1 trang riêng bên Admin, gộp đủ các việc trên cho 1 khách hàng cụ thể.

## 1. Điều hướng

Không dùng vue-router — theo đúng pattern `currentPage` (state-switch) đã có trong
`AdminPage.vue` (giống `productsMainTab`, `inventoryMainTab`).

- Thêm nút "Chi tiết" ở mỗi dòng `CustomersTable.vue`, cạnh Sửa/Xóa.
- Bấm vào → `AdminPage.vue` set `currentPage = 'customer-detail'` + `selectedCustomerId = c.khachHangId`.
- Trang mới có nút "← Quay lại danh sách khách hàng" set `currentPage = 'customers'`.
- Trang này chỉ sống trong `AdminPage.vue` → chỉ role `admin` vào được (đã đúng theo routing
  hiện có: `admin → #admin`, `nhan_vien → #staff`, `quan_kho → #kho`), không cần gate thêm ở
  frontend.

## 2. Bố cục trang `CustomerDetailPage.vue` (component mới)

1. **Header**: họ tên, SĐT, email, loại khách, badge trạng thái (active/khóa), nút "Sửa thông
   tin" → mở lại `CustomerFormModal.vue` sẵn có (không viết form mới).
2. **KPI row**: Tổng chi tiêu, Số đơn, Điểm hiện có, Đơn gần nhất — tính client-side từ dữ liệu
   đã có sẵn trong store (không gọi API mới).
3. **Đơn hàng**: bảng tóm tắt (mã đơn, ngày đặt, tổng tiền, trạng thái), lọc client-side từ
   `OrdersStore.items` theo `khachHangId`.
   - **Phạm vi v1**: chỉ đọc, KHÔNG bấm vào để xem chi tiết đơn (modal chi tiết đơn trong
     `OrdersTable.vue` đang gắn khá chặt với state của bảng đó, tách ra tốn công không cân xứng
     với việc này). Nếu cần xem đầy đủ, admin qua tab Đơn hàng tìm theo SĐT/tên.
4. **Phiếu giảm giá / điểm thưởng**: bảng `phieu_giam_gia_ca_nhan` của khách này, thêm cột
   "Nguồn" suy ra được (không thêm cột DB):
   - Có `doiThuongId` HOẶC khớp với 1 dòng trong `lich_su_quay` → "Khách tự đổi / trúng thưởng"
   - Ngược lại → "Admin tặng"
   Cột trạng thái: Còn hạn / Đã dùng / Hết hạn (so `daSuDung` + `ngayHetHan` với hiện tại).
5. **Lịch sử tặng điểm**: bảng ledger mới (mới nhất trước), cột: số điểm, lý do, người tặng,
   ngày.
6. **Hành động** (chỉ admin gọi được, nút vẫn hiện nhưng gọi API sẽ 403 nếu lỡ vào bằng role
   khác — về lý thuyết không xảy ra vì trang chỉ sống trong AdminPage.vue):
   - "🎁 Tặng điểm" → modal nhỏ: số điểm (bắt buộc > 0), lý do (tùy chọn).
   - "🎟️ Tặng voucher" → modal nhỏ: loại (percent/fixed), giá trị, giảm tối đa (tùy chọn), hạn
     dùng, đơn tối thiểu (tùy chọn). Validate: percent ≤ 100 (giống CHECK constraint hiện có ở
     `khuyen_mai`/`dm_doi_thuong`), hạn dùng phải sau hiện tại.
   - Cả 2 đều patch cục bộ danh sách tương ứng sau khi gọi API thành công + toast, không load
     lại cả trang.

## 3. Backend

### Bảng mới: `lich_su_tang_diem`

```sql
CREATE TABLE lich_su_tang_diem (
    id             INT           IDENTITY(1,1) PRIMARY KEY,
    khach_hang_id  INT           NOT NULL,
    nhan_vien_id   INT           NOT NULL,   -- admin đã tặng
    so_diem        INT           NOT NULL CONSTRAINT CK_lstd_sodiem CHECK (so_diem > 0),
    ly_do          NVARCHAR(255) NULL,
    ngay_tao       DATETIME      NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_lstd_khach_hang FOREIGN KEY (khach_hang_id) REFERENCES khach_hang(khach_hang_id),
    CONSTRAINT FK_lstd_nhan_vien  FOREIGN KEY (nhan_vien_id)  REFERENCES nhan_vien(nhan_vien_id)
);
```

Thêm vào `Database/QLBanMayTinh.sql` theo đúng vị trí các bảng liên quan tới khách hàng/điểm
thưởng hiện có (gần `phieu_giam_gia_ca_nhan`).

Entity `LichSuTangDiem` + `LichSuTangDiemRepository` (`findByKhachHang_KhachHangIdOrderByNgayTaoDesc`).

### Service

- `KhachHangService.tangDiem(Integer khachHangId, Integer soDiem, String lyDo)`:
  - Validate `soDiem > 0`.
  - Khóa ghi khách hàng (`findWithLockByKhachHangId`, đã có sẵn — dùng lại đúng pattern
    `PhieuGiamGiaCaNhanService.doiThuong()`), cộng `diemTichLuy`.
  - Lấy admin hiện tại qua `SecurityContextHolder` (đúng pattern `currentAccount()` trong
    `PhieuGiamGiaCaNhanService`), lưu 1 dòng `lich_su_tang_diem`.
- `PhieuGiamGiaCaNhanService.taoVoucherAdmin(Integer khachHangId, TangVoucherRequest req)`:
  - Tạo `PhieuGiamGiaCaNhan` mới, `doiThuong = null`, `donHang = null`, `daSuDung = false`,
    `ngayDoi = now()`, các field còn lại lấy từ request.
- `PhieuGiamGiaCaNhanService.getByKhachHangIdForAdmin(Integer khachHangId)`:
  - Dùng lại `findByKhachHang_KhachHangId` có sẵn, map sang response có thêm field `nguon`
    (tính theo mục 2.4 — cần thêm 1 query nhỏ lấy set `phieuId` từ `lich_su_quay` theo
    `khachHangId` để check trúng vòng quay).

### Response DTO

- Thêm field `nguon` (String) vào `PhieuGiamGiaCaNhanResponse` — endpoint tự phục vụ
  (`getCuaToi()`) truyền `null` cho field này (không tính nguồn ở đó, ngoài phạm vi yêu cầu lần
  này).
- `LichSuTangDiemResponse` (mới): id, soDiem, lyDo, tenNhanVien, ngayTao.

### Controller — tất cả `@PreAuthorize("hasRole('ADMIN')")`

- `POST /api/khach-hang/{id}/tang-diem` — body `{ soDiem, lyDo }` → `KhachHangController`.
- `GET  /api/khach-hang/{id}/lich-su-diem` → `KhachHangController`.
- `POST /api/phieu-giam-gia-ca-nhan/tang/{khachHangId}` — body
  `{ loai, giaTri, giaTriToiDa, ngayHetHan, donHangToiThieu }` → `PhieuGiamGiaCaNhanController`.
- `GET  /api/phieu-giam-gia-ca-nhan/khach-hang/{id}` → `PhieuGiamGiaCaNhanController` (endpoint
  admin riêng, KHÔNG đụng vào `GET cua-toi` tự phục vụ hiện có).

## 4. Xử lý lỗi

- `soDiem <= 0` → 400, thông báo rõ ràng.
- Voucher `loai = percent` mà `giaTri > 100` → 400.
- `ngayHetHan` <= hiện tại → 400.
- Cả 2 action: lỗi hiện toast đỏ, không đóng modal (giữ nguyên input để sửa lại).

## 5. Phạm vi bỏ qua (ghi rõ để khỏi hiểu nhầm là quên)

- Không cho phép trừ điểm / thu hồi voucher đã tặng trong lần này — chỉ tặng thêm. Cần thì làm
  sau, đây là tính năng riêng.
- Không click-through xem chi tiết đơn hàng trong bảng "Đơn hàng" của trang này (xem mục 2.3).
- Không tính "nguồn" (đổi điểm/trúng thưởng) cho danh sách voucher tự phục vụ của khách hàng
  (`GET cua-toi`) — chỉ áp dụng cho view admin.
- Không giới hạn nhân viên bán hàng/quản kho dùng 2 hành động tặng — chỉ admin theo đúng lựa
  chọn đã chốt.
