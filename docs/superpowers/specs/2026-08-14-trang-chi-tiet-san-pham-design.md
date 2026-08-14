# Trang chi tiết sản phẩm (Admin) — thiết kế

**Ngày:** 2026-08-14
**Nguồn:** `Note những thứ cần cải thiện.docx`, mục I.3 "Sản phẩm":
> "khi nhấn vào chi tiết sản phẩm: mở 1 tab khác load luôn lên trang sản phẩm, trong đó sẽ có 3 tab con gồm: thông tin sản phẩm, biến thể, lịch sử thay đổi (sẽ hiển thị khi update sản phẩm hoặc biến thể)"

Sub-project 3/6 trong kế hoạch cải thiện tổng thể (POS → Đơn hàng → **Sản phẩm** → Biến thể → Khách hàng → Kho hàng).

## Mục tiêu

Khi Admin nhấn "Chi tiết" trên một sản phẩm ở `ProductsTable.vue`, thay vì mở modal so sánh biến thể như hiện tại, mở một **tab trình duyệt mới** dẫn tới một trang chi tiết sản phẩm độc lập với 3 tab con: Thông tin sản phẩm, Biến thể sản phẩm, Lịch sử thay đổi. Lịch sử thay đổi ghi nhận **từng trường** đã đổi (giá trị cũ → mới) mỗi khi sản phẩm hoặc một biến thể của nó được cập nhật.

## Phạm vi

**Trong phạm vi:**
- Route mới `/admin/san-pham/:id`, mở qua tab mới từ nút "Chi tiết" trong `ProductsTable.vue` (chỉ áp dụng cho instance Admin, không phải readonly).
- Trang `SanPhamDetailPage.vue` với 3 tab: Thông tin sản phẩm / Biến thể sản phẩm (N) / Lịch sử thay đổi.
- Tách form thêm/sửa sản phẩm hiện có trong `ProductsTable.vue` thành component dùng chung `ProductFormModal.vue`, để nút "Chỉnh sửa" ở trang chi tiết dùng lại được.
- Thêm prop `filter-san-pham-id` cho `BienTheTable.vue` để lọc theo 1 sản phẩm.
- Backend: bảng `lich_su_thay_doi_san_pham`, ghi log chênh lệch từng trường khi `SanPhamService.updateSanPham()` hoặc `BienTheSanPhamService.update()` chạy, endpoint `GET /api/san-pham/{id}/lich-su`.

**Ngoài phạm vi (không làm trong sub-project này):**
- "Xóa danh mục sản phẩm" — bullet thứ 2 trong cùng mục 3 của note. Thiết kế đã duyệt ("rồi") chỉ bao gồm 3 phần nêu trên; bullet này cần làm rõ thêm ý định (xóa field danh mục khỏi form, hay xóa chức năng quản lý danh mục, hay cho phép xóa 1 dòng danh mục) — để lại cho vòng brainstorm riêng.
- Nút "Chi tiết" ở `StaffPage.vue` (instance `ProductsTable :readonly="true"`, role `nhan_vien`) — giữ nguyên hành vi mở modal cũ, vì route `/admin/san-pham/:id` chỉ cấp quyền `admin` và note ghi rõ yêu cầu này nằm trong mục "I. Admin".
- Ghi log cho hành động **tạo mới** sản phẩm/biến thể — note chỉ yêu cầu log khi "update"; tạo mới không có "giá trị cũ" để so sánh.
- Phân trang cho danh sách lịch sử thay đổi — v1 trả về toàn bộ, sắp xếp mới nhất trước.
- Xử lý ghi log cho việc gán/gỡ nhà cung cấp qua các luồng nhập kho khác (không đi qua `SanPhamService`/`BienTheSanPhamService`).

## Phần 1 — Route và điều hướng

- `router/index.js`: thêm route
  ```js
  { path: "/admin/san-pham/:id", name: "admin-san-pham-detail", component: AdminPage,
    meta: { requiresAuth: true, roles: ["admin"] } }
  ```
  Dùng lại `AdminPage.vue` (đã lazy-import sẵn), không tạo page riêng — giữ chung sidebar/topbar/theme/i18n store đã khởi tạo trong `AdminPage.vue`, giống cách `CustomerDetailPage.vue` được nhúng vào hiện tại.
- `AdminPage.vue`: đọc `route.params.id` lúc mount. Nếu có, set `currentPage.value = 'san-pham-detail'` và `selectedSanPhamId.value = Number(route.params.id)` thay vì mặc định vào `dashboard`. Sidebar ẩn mục này (không phải 1 tab điều hướng như "Sản phẩm"/"Đơn hàng" — chỉ vào được qua link trực tiếp), nút "← Quay lại" trong `SanPhamDetailPage.vue` điều hướng `router.push('/admin')` (cùng tab).
- `ProductsTable.vue`, hàm `openDetail(sanPhamId, name)` (dòng 100): nếu `props.readonly` giữ nguyên hành vi hiện tại (mở `ProductDetailModal`); nếu không readonly, đổi thành:
  ```js
  window.open(`${location.origin}${location.pathname}#/admin/san-pham/${sanPhamId}`, '_blank');
  ```
  `ProductDetailModal.vue` không bị xóa hay sửa — vẫn được dùng nguyên trạng ở `PosPanel.vue`, `OrdersTable.vue`, `SerialManager.vue`, và ở `ProductsTable.vue` khi `readonly`.
  `sessionStorage` (phiên đăng nhập) được chia sẻ tự động sang tab mới vì cùng origin + mở bằng `window.open`, nên không cần đăng nhập lại.

## Phần 2 — `SanPhamDetailPage.vue` (3 tab)

Component mới, nhận `sanPhamId` (Number). Header: breadcrumb "Sản phẩm > {tên sản phẩm}" + nút "← Quay lại" + nút "Chỉnh sửa" (mở `ProductFormModal.vue` ở chế độ edit).

**Tab "Thông tin sản phẩm"** — 2 card theo đúng layout ảnh mẫu đã duyệt:
- Card "Thông tin cơ bản": tên sản phẩm, thương hiệu, danh mục, nhà cung cấp, loại sản phẩm, trạng thái (badge), ngày ra mắt (map từ `ngayTao` kế thừa `BaseEntity`), mô tả, hình ảnh chính.
- Card "Thống kê nhanh": số biến thể, khoảng giá bán (min–max trong các biến thể), tổng tồn kho các biến thể, ngày cập nhật gần nhất (`ngayCapNhat`).
- Dữ liệu lấy từ `SanPhamService.getById()` (đã có, `GET /api/san-pham/{id}`) + lọc `ProductsStore.items` theo `sanPhamId` để tính thống kê (không gọi API riêng).

**Tab "Biến thể sản phẩm (N)"** — nhúng `<BienTheTable :filter-san-pham-id="sanPhamId" />`.
- `BienTheTable.vue`: thêm prop `filterSanPhamId: { type: Number, default: null }`; trong `filteredVariants` (dòng 64), thêm điều kiện lọc `p.sanPhamId === props.filterSanPhamId` khi prop được truyền (giữ nguyên hành vi cũ khi không truyền — dùng chung cho cả tab "Biến thể" độc lập và tab con này). `ProductsStore.items` đã là danh sách phẳng mỗi dòng = 1 biến thể kèm `sanPhamId`, nên lọc trực tiếp, không cần API mới.
- `N` = `filteredVariants.length` sau khi lọc, hiển thị ngay trên tên tab.

**Tab "Lịch sử thay đổi"** — bảng liệt kê từ `GET /api/san-pham/{id}/lich-su`: thời gian, người sửa, đối tượng (Sản phẩm / SKU biến thể), tên trường (map sang nhãn tiếng Việt qua i18n), giá trị cũ → giá trị mới. Sắp xếp mới nhất trước, không phân trang (v1).

**Tách `ProductFormModal.vue`:** hiện form thêm/sửa sản phẩm nằm inline trong `ProductsTable.vue`. Tách phần JSX/logic form (fields + validate + gọi `SanPhamService.createSanPham`/`updateSanPham`) thành component riêng nhận props `mode` ('create'|'edit') và `sanPhamId` (khi edit), emit `saved`. `ProductsTable.vue` dùng lại component này (hành vi không đổi), `SanPhamDetailPage.vue` dùng cho nút "Chỉnh sửa" — tránh trùng lặp ~100 dòng form.

## Phần 3 — Backend: lịch sử thay đổi theo từng trường

**Bảng mới `lich_su_thay_doi_san_pham`** (thêm vào `Database/QLBanMayTinh.sql`, idempotent theo pattern `IF NOT EXISTS (SELECT 1 FROM sys.tables ...)` đã dùng cho `lich_su_ton_kho`):

```sql
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'lich_su_thay_doi_san_pham')
BEGIN
    CREATE TABLE lich_su_thay_doi_san_pham (
        lich_su_id   INT           IDENTITY(1,1) PRIMARY KEY,
        san_pham_id  INT           NOT NULL,
        bien_the_id  INT           NULL,
        doi_tuong    NVARCHAR(20)  NOT NULL
            CONSTRAINT CK_lstsp_doi_tuong CHECK (doi_tuong IN (N'san_pham', N'bien_the')),
        ten_truong   NVARCHAR(50)  NOT NULL,
        gia_tri_cu   NVARCHAR(500) NULL,
        gia_tri_moi  NVARCHAR(500) NULL,
        nhan_vien_id INT           NULL,
        thoi_gian    DATETIME      NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_lstsp_san_pham  FOREIGN KEY (san_pham_id) REFERENCES san_pham(san_pham_id) ON DELETE CASCADE,
        CONSTRAINT FK_lstsp_bien_the  FOREIGN KEY (bien_the_id) REFERENCES bien_the_san_pham(bien_the_id),
        CONSTRAINT FK_lstsp_nhan_vien FOREIGN KEY (nhan_vien_id) REFERENCES nhan_vien(nhan_vien_id)
    );
END
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_lstsp_san_pham')
    CREATE INDEX IX_lstsp_san_pham ON lich_su_thay_doi_san_pham(san_pham_id, thoi_gian DESC);
GO
```

`bien_the_id` = biến thể liên quan khi `doi_tuong = 'bien_the'` (NULL khi sửa trường của chính sản phẩm). Ràng buộc `san_pham_id` luôn trỏ về sản phẩm cha, kể cả khi thay đổi nằm ở 1 biến thể — để tab "Lịch sử thay đổi" của trang chỉ cần 1 query duy nhất theo `san_pham_id`, đúng yêu cầu note ("hiển thị khi update sản phẩm hoặc biến thể" — hợp nhất trên cùng trang).

**Entity/Repository/Response:**
- `LichSuThayDoiSanPham.java` — entity thường (không kế thừa `BaseEntity`, bảng log chỉ ghi 1 lần, đã có cột `thoi_gian` riêng).
- `LichSuThayDoiSanPhamRepository extends JpaRepository<...>` với `@Query` trả `LichSuThayDoiSanPhamResponse` (join `nhan_vien` lấy tên, join `bien_the_san_pham` lấy `maSku` khi có) theo `san_pham_id`, sắp xếp `thoi_gian DESC`.
- `LichSuThayDoiSanPhamResponse`: `lichSuId, doiTuong, bienTheId, maSku (nullable), tenTruong, giaTriCu, giaTriMoi, tenNhanVien (nullable), thoiGian`.

**Ghi log — diff theo trường:**
Trong `SanPhamService.updateSanPham()` và `BienTheSanPhamService.update()`, trước khi `BeanUtils.copyProperties` ghi đè entity, snapshot giá trị cũ của các trường theo dõi; sau khi lưu, so sánh cũ/mới và insert 1 dòng `lich_su_thay_doi_san_pham` cho mỗi trường thực sự đổi (bỏ qua nếu bằng nhau, kể cả khi cả 2 đều `null`). Giá trị lưu dạng chuỗi (`String.valueOf`), các trường khóa ngoại (thương hiệu/danh mục/CPU/RAM/...) log theo ID thô, không resolve tên hiển thị (đơn giản hóa cho v1; frontend có thể tra cứu tên nếu cần sau).

Trường theo dõi:
- **Sản phẩm** (`doi_tuong = 'san_pham'`): `tenSanPham, thuongHieuId, danhMucId, nhaCungCapId, loaiSanPham, moTa, hinhAnhChinh, trangThai`.
- **Biến thể** (`doi_tuong = 'bien_the'`): `maSku, giaNhap, giaBan, baoHanhThang, hinhAnhBienThe, trangThai, mauSac, cpuId, ramId, oCungId, gpuId, kichThuocManHinh, heDieuHanh, pin, trongLuongKg`.

**Người sửa (`nhan_vien_id`):** resolve phía server, không nhận từ request (audit log không nên tin dữ liệu client gửi). Dùng lại pattern đã có ở `DanhGiaService.currentKhachHang()` / `PhieuTraHangService.currentAccount()`:
```java
String username = SecurityContextHolder.getContext().getAuthentication().getName();
NhanVien nv = taiKhoanRepository.findByUsername(username)
        .map(TaiKhoan::getNhanVien).orElse(null);
```
`nhan_vien_id` NULL nếu không resolve được (tài khoản không gắn `NhanVien`) — không chặn việc lưu thay đổi sản phẩm chỉ vì thiếu thông tin log.

**API mới:** `GET /api/san-pham/{id}/lich-su` trong `SanPhamController`, `@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")`, trả `List<LichSuThayDoiSanPhamResponse>`.

## Kiểm thử

- Frontend: mở `ProductsTable.vue` → "Chi tiết" → xác nhận mở tab mới đúng route, cả 3 tab con render đúng dữ liệu, tab "Biến thể" chỉ hiện biến thể của đúng sản phẩm, "Chỉnh sửa" mở đúng `ProductFormModal.vue` và lưu thành công.
- Backend: sửa 1 trường của sản phẩm → xác nhận đúng 1 dòng log với `gia_tri_cu`/`gia_tri_moi` đúng; sửa nhiều trường 1 lần → nhiều dòng log cùng `thoi_gian`; sửa mà không đổi giá trị → không sinh dòng log nào; sửa biến thể → log với `doi_tuong='bien_the'` và `bien_the_id` đúng, `san_pham_id` = sản phẩm cha.
- Không có test trình duyệt tự động khả dụng trong phiên này (Playwright MCP mất kết nối) — người dùng tự xác nhận qua UI thật sau khi cài đặt.
