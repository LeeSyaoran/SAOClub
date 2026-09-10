# Plan: Dọn rác serial "đang đặt hàng" (giu_hang) bị kẹt

## Context

Trên trang Kho hàng (`/admin` tab "Quản lý tồn kho" → `SerialManager.vue`) hiển thị một số serial có trạng thái **"Đang đặt hàng"** (`giu_hang`) trong khi đơn hàng tương ứng đã không còn tồn tại (bị hủy, bị xóa, hoặc đơn online `pending` đã bị xóa khi rollback). Kết quả là serial bị **kẹt vĩnh viễn** ở `giu_hang`, không thể bán lại và không xuất hiện trong dropdown chọn serial POS.

### Root cause

Flow POS ([PosPanel.vue:478](FrontEnd/QLBanMayTinh/src/components/admin/PosPanel.vue#L478)) set `giu_hang` **ngay khi nhân viên chọn serial vào giỏ**, trước cả khi đơn hàng được tạo ở backend. Nếu:
- Đóng tab / refresh giữa chừng,
- `posPlaceOrder()` throw exception **trước khi** `addChiTiet` được gọi,

thì serial đã bị set `giu_hang` ở DB nhưng **không có** `don_hang_id` nào liên kết.

`DonHangService.releaseSerialsToStock()` ([DonHangService.java:202-214](BackEnd/src/main/java/com/example/backend/service/DonHangService.java#L202-L214)) chỉ release serial thuộc `chi_tiet_don_hang` đã được lưu — không có orphan cleanup.

Entity `ChiTietSanPham` không có `ngay_cap_nhat` ([ChiTietSanPham.java](BackEnd/src/main/java/com/example/backend/entity/ChiTietSanPham.java)) nên không có scheduler tự động theo thời gian.

### Hướng xử lý đã chốt với user

Chỉ dọn rác thủ công: thêm 1 nút "Giải phóng serial kẹt" trên trang Kho hàng, gọi 1 endpoint `POST /api/chi-tiet-san-pham/release-orphans` ở backend.

Không đổi flow POS (giữ `giu_hang` ngay khi chọn — tránh race condition giữa 2 nhân viên), không thêm scheduler tự động (giữ scope nhỏ).

---

## Critical files để sửa

### Backend

1. **`BackEnd/src/main/java/com/example/backend/repository/ChiTietSanPhamRepository.java`** — thêm query tìm orphan:
   ```java
   @Query("""
       SELECT c FROM ChiTietSanPham c
       WHERE c.trangThai = 'giu_hang'
         AND NOT EXISTS (
             SELECT 1 FROM ChiTietDonHangSerial s
             WHERE s.chiTietSanPham.chiTietId = c.chiTietId
         )
       """)
   List<ChiTietSanPham> findOrphanGiuHangSerials();
   ```

2. **`BackEnd/src/main/java/com/example/backend/service/ChiTietSanPhamService.java`** — thêm method mới:
   - `@Autowired LichSuTonKhoRepository lichSuTonKhoRepository;`
   - `@Transactional` method `releaseOrphanSerials()` trả về `int releasedCount`:
     - Gọi `chiTietSanPhamRepository.findOrphanGiuHangSerials()`
     - Với mỗi serial: set `trangThai = "trong_kho"`, `save`, ghi `LichSuTonKho` với `loaiBienDong = "giu_hang"` (tái dùng — không thêm enum mới), `soLuongThayDoi = +1` (vì trả về kho), `donHang = null` (orphan), `ghiChu = "Giải phóng serial giu_hang mồ côi (đơn đã hủy/xóa) — serial #X"`

3. **`BackEnd/src/main/java/com/example/backend/controller/ChiTietSanPhamController.java`** — thêm endpoint:
   - `@PostMapping("release-orphans")`
   - `@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")` — giữ pattern như các endpoint hiện có ở controller này
   - Trả về `Map.of("releasedCount", releasedCount)` để UI đọc được số lượng

### Frontend

4. **`FrontEnd/QLBanMayTinh/src/services/ChiTietSanPhamService.js`** — thêm wrapper:
   ```js
   export const releaseOrphans = () => post('/api/chi-tiet-san-pham/release-orphans', {});
   ```

5. **`FrontEnd/QLBanMayTinh/src/components/admin/SerialManager.vue`** — thêm nút trong toolbar:
   - Computed `orphanCount` đếm serial `giu_hang` không có trong đơn (lọc client-side từ `items.value` đang load — proxy đủ tốt vì user cũng đang xem trang này)
   - Nút "Giải phóng X serial kẹt" bên cạnh nút "+ Thêm serial", `disabled` khi `orphanCount === 0`
   - `await askConfirm(...)` trước khi gọi; `showToast` báo kết quả; gọi lại `load()`

6. **`FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js` + `en.js`** — thêm key trong `admin.serialManager`:
   - `releaseOrphans: "Giải phóng {count} serial kẹt"`
   - `releaseOrphansConfirm: "Giải phóng {count} serial đang ở trạng thái 'Đang đặt hàng' nhưng không còn liên kết đơn nào? Hành động này không thể hoàn tác."`
   - `releaseOrphansDone: "Đã giải phóng {count} serial về kho"`
   - `releaseOrphansNone: "Không có serial kẹt"`

### Không đụng

- `Database/QLBanMayTinh.sql` — không thêm cột `ngay_cap_nhat`, không cần thiết cho giải pháp thủ công
- `PosPanel.vue` — giữ flow hiện tại
- Backend `DonHangService.releaseSerialsToStock` — không đổi

---

## Verification

1. **Reproduce bug trước khi fix**:
   - Mở POS, chọn 1 sản phẩm → chọn 1 serial vào giỏ
   - Reload trang (F5) ngay sau khi chọn serial, **trước** khi thanh toán
   - Vào trang Kho hàng → serial đó sẽ hiển thị trạng thái "Đang đặt hàng"

2. **Sau khi fix**:
   - Vẫn ở trang Kho hàng, thấy nút "Giải phóng 1 serial kẹt"
   - Bấm nút → confirm → toast "Đã giải phóng 1 serial về kho"
   - Serial chuyển sang "Trong kho", có thể chọn lại trong POS

3. **Regression test**:
   - Tạo đơn POS bình thường: serial `giu_hang` → sau thanh toán → `da_ban` (không bị ảnh hưởng)
   - Hủy đơn online: serial đã chọn → `trong_kho` (BE `xacNhanSerialsOnline` xử lý, không đụng)
   - Xóa đơn POS: serial `da_ban` → `trong_kho` (BE `releaseSerialsToStock`, không đụng)

4. **Test backend endpoint bằng curl** (optional):
   ```
   curl -X POST http://localhost:8080/api/chi-tiet-san-pham/release-orphans \
     -H "Authorization: Bearer <token>"
   ```