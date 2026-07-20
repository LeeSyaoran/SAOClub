# Danh mục linh kiện (CPU/RAM/GPU/Ổ cứng) + Quản lý Serial — Design Spec

**Ngày:** 2026-07-20

## Bối cảnh

Người dùng muốn thêm tab quản lý danh mục linh kiện (CPU/RAM/GPU/Ổ cứng) vào trang Sản phẩm, và tách riêng 1 tab "Quản lý Serial" (thay vì chỉ nhập được 1 serial lúc tạo sản phẩm như hiện tại). Khảo sát cho thấy:

- Backend `DmCpuController`/`DmRamController`/`DmGpuController`/`DmOcungController` (`/api/dm-cpu`, `/api/dm-ram`, `/api/dm-gpu`, `/api/dm-o-cung`) đã có đủ CRUD (GET/POST/PUT/DELETE thao tác thẳng qua Repository, không dùng DTO) nhưng **chưa từng có frontend nào gọi POST/PUT/DELETE** — `Service/DmService.js` chỉ có 4 hàm GET dùng đổ dropdown trong `ProductsTable.vue`.
- Backend `ChiTietSanPhamController` (`/api/chi-tiet-san-pham`) cũng đã có đủ CRUD, và **frontend `ChiTietSanPhamService.js` đã có sẵn `create`/`update`/`remove`** (dùng bởi `ProductsTable.vue` để tạo 1 serial lúc thêm sản phẩm mới) — chỉ chưa có UI xem/sửa/xoá toàn bộ serial.
- **Cả 5 controller trên đều KHÔNG có `@PreAuthorize`** — bất kỳ ai có JWT hợp lệ (kể cả khách hàng, vì các endpoint này không nằm trong danh sách `permitAll()` của `SecurityConfig` nên rơi vào `anyRequest().authenticated()`, không có role check) đều gọi được POST/PUT/DELETE. Grep toàn frontend xác nhận chỉ 2 file (`ChiTietSanPhamService.js`, `DmService.js`) từng gọi tới các endpoint này, cả 2 đều chỉ dùng ở phía admin (`ProductsTable.vue`) — khoá quyền an toàn tuyệt đối.
- `ChiTietSanPhamRequest`/`Response` thiếu field `ghiChu` dù cột `chi_tiet_san_pham.ghi_chu` đã tồn tại trong DB — cần bổ sung để form Serial có ô ghi chú hữu ích (vd "trầy nhẹ nắp máy").

## Mục tiêu

1. Khoá quyền backend cho 5 controller hiện đang mở hoàn toàn.
2. Bổ sung field `ghiChu` còn thiếu trong `ChiTietSanPhamRequest`/`Response`.
3. Xây UI quản lý danh mục CPU/RAM/GPU/Ổ cứng (Admin) và Serial (Admin + Kho).

## Phần 1 — Vá backend

### 1.1 Khoá quyền

Thêm class-level `@PreAuthorize`:
- `DmCpuController`, `DmRamController`, `DmGpuController`, `DmOcungController`: `@PreAuthorize("hasRole('ADMIN')")` — chỉ admin, đúng khuôn `CaiDatController` (đã dùng `hasRole('ADMIN')` cho các thao tác admin-only trong dự án).
- `ChiTietSanPhamController`: `@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")` — đúng khuôn `PhieuTraHangController`/`PhieuBaoHanhController`. Nhân viên không có UI vào tab Serial nhưng API vẫn mở theo đúng convention "cả nhóm staff" đã dùng nhất quán trong toàn dự án.

**Lưu ý quan trọng:** `GET /api/chi-tiet-san-pham/con-bao-hanh` hiện được `WarrantyPanel.vue` gọi (tính năng Phiếu bảo hành đã merge trước đó) — khoá class-level không ảnh hưởng vì `WarrantyPanel.vue` chỉ mount ở trang Kho/Admin, cả 2 đều đã có JWT với role hợp lệ.

### 1.2 Bổ sung `ghiChu`

`ChiTietSanPhamRequest.java`: thêm field `private String ghiChu;` (không bắt buộc, cột DB cho phép NULL).
`ChiTietSanPhamResponse.java`: thêm field `private String ghiChu;`.
`ChiTietSanPhamService.java`: `create()`/`update()` đã dùng `BeanUtils.copyProperties` nên tự động copy field mới, không cần sửa logic — chỉ cần đảm bảo JPQL trong repository (nếu `hienThiChiTietSanPham()` dùng constructor expression) có thêm `c.ghiChu`.

## Phần 2 — Frontend

### 2.1 `DmService.js` — thêm CRUD

Thêm 1 factory function dùng chung:
```js
const crud = (path) => ({
  getAll: () => get(`/api/${path}`),
  save: (id, body) => id ? put(`/api/${path}/update/${id}`, body) : post(`/api/${path}`, body),
  remove: (id) => del(`/api/${path}/delete/${id}`),
});

export const DmCpuService = crud('dm-cpu');
export const DmRamService = crud('dm-ram');
export const DmGpuService = crud('dm-gpu');
export const DmOCungService = crud('dm-o-cung');
```
4 hàm GET hiện có (`getCpu`/`getRam`/`getGpu`/`getOCung`, dùng đổ dropdown ở `ProductsTable.vue`) giữ nguyên, không đụng.

### 2.2 `DmCategoryTable.vue` — component dùng chung cho 4 bảng

Nhận props: `apiPath` (service object từ 2.1), `idField` (`cpuId`/`ramId`/`gpuId`/`oCungId`), `nameField` (`tenCpu`/`dungLuong`/`tenGpu`/`loaiOCung`), `label` (nhãn hiển thị, vd "CPU"). List + modal thêm/sửa 1 ô text duy nhất (tên/cấu hình), nút xoá — theo đúng khuôn `SupplierManager.vue` nhưng tối giản hơn (1 field thay vì nhiều).

Gắn 4 lần trong sub-tab mới của trang Sản phẩm, mỗi lần truyền props khác nhau cho CPU/RAM/GPU/Ổ cứng.

### 2.3 `SerialManager.vue` — component CRUD serial

Bảng liệt kê toàn bộ `chi_tiet_san_pham` (dùng `ChiTietSanPhamService.getAll()` đã có), cột: SKU/biến thể, số serial, trạng thái (badge màu theo `trong_kho`/`giu_hang`/`da_ban`/`loi_bao_hanh`/`da_tra_hang` — tái dùng bảng màu đã thống nhất trước đó cho serial status), ngày nhập kho, ghi chú, thao tác. Modal thêm/sửa: chọn biến thể (dropdown từ `ProductsStore`, tái dùng dữ liệu đã có), nhập số serial, trạng thái, ngày nhập kho, ghi chú — gọi thẳng `ChiTietSanPhamService.create/update/remove` đã có sẵn từ trước.

Không props phân quyền — cả 2 nơi gắn đều full CRUD giống nhau (entity không có field theo role như phiếu trả hàng).

### 2.4 Gắn vào trang

**`AdminPage.vue`** — trang "Sản phẩm" (`currentPage === 'products'`) thêm sub-tab switcher mới (biến `productsMainTab`, giống `inventoryMainTab` của trang Kho hàng):

| Sub-tab | Component |
|---|---|
| Sản phẩm (mặc định) | `<ProductsTable />` (giữ nguyên, không đổi) |
| CPU | `<DmCategoryTable :service="DmCpuService" id-field="cpuId" name-field="tenCpu" label="CPU" />` |
| RAM | tương tự, `DmRamService`/`ramId`/`dungLuong` |
| GPU | tương tự, `DmGpuService`/`gpuId`/`tenGpu` |
| Ổ cứng | tương tự, `DmOCungService`/`oCungId`/`loaiOCung` |
| Serial | `<SerialManager />` |

**`WarehouseManagementPage.vue`** — thêm 1 mục nav cấp cao mới "Serial" (trang này chưa có trang Sản phẩm để lồng sub-tab), gắn `<SerialManager />`, không gắn 4 tab Dm* (Dm* chỉ Admin theo quyết định đã chốt).

Không đụng `StaffPage.vue`.

## Ngoài phạm vi (Non-goals)

- Không đổi luồng "nhập 1 serial lúc tạo sản phẩm mới" đã có sẵn trong `ProductsTable.vue` — giữ nguyên, tab Serial mới là bổ sung, không thay thế.
- Không thêm `mo_ta`/`trang_thai`/`ngay_tao` vào 4 bảng `dm_cpu`/`dm_ram`/`dm_gpu`/`dm_o_cung` — schema hiện tại tối giản (2 cột), không đổi DB.
- Không dùng lại `DmXxxRequest`/`DmXxxResponse` DTO đang tồn tại nhưng chưa được controller dùng — giữ nguyên cách controller thao tác thẳng entity, không phải phạm vi task này để dọn.
- Không thêm validate nhập nhiều serial cùng lúc (bulk import) — tab Serial mới vẫn thêm từng serial 1, chỉ khác là có bảng xem/sửa/xoá toàn bộ thay vì chỉ tạo được lúc thêm sản phẩm.

## Tự rà soát (self-review)

**1. Phủ đủ yêu cầu đã chốt:**
- Khoá quyền 5 controller ✅
- Bổ sung ghiChu ✅
- Component dùng chung cho 4 bảng Dm* ✅
- Component Serial riêng, dùng lại service có sẵn ✅
- Gắn đúng trang theo quyết định phân quyền (Dm* chỉ Admin, Serial cả Admin+Kho) ✅

**2. Không còn placeholder** — mọi field/endpoint đã xác định chính xác qua đọc code thực tế.

**3. Nhất quán:** enum `trang_thai` của `chi_tiet_san_pham` (`trong_kho`/`giu_hang`/`da_ban`/`loi_bao_hanh`/`da_tra_hang`) xác nhận đúng từ CHECK constraint trong `QLBanMayTinh.sql`, khớp với bảng màu đã dùng ở `InventoryPanel.vue`/`InventoryHistoryPanel.vue` trước đó.

**4. Idempotency:** không có thay đổi DB schema trong spec này (chỉ sửa Java/Vue) — không cần lệnh SQL mới.
