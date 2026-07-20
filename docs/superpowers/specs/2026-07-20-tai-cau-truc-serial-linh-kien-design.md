# Tái cấu trúc Quản lý Serial + Serial linh kiện CPU/RAM/GPU/Ổ cứng — Design Spec

**Ngày:** 2026-07-20

## Bối cảnh

Sau khi triển khai tính năng "Danh mục linh kiện + Quản lý Serial" (spec `2026-07-20-dm-linh-kien-serial-design.md`), người dùng đặt lại câu hỏi về vị trí và sự cần thiết của tab "Serial" mới. Khảo sát cho thấy:

- `InventoryPanel.vue` (trang Kho hàng) **đã có sẵn** chức năng serial theo từng biến thể: nút "Chi tiết" xem/thêm/xóa serial (`ChiTietSanPhamService.getByBienThe`), nút "Cập nhật" nhập serial hàng loạt + import file (`.csv/.txt/.xlsx/.xls`, hàm `importSerialsFromFile`). `SerialManager.vue` (vừa thêm) có phần trùng chức năng, khác biệt duy nhất là xem/tìm/sửa serial **trên toàn hệ thống** thay vì phải bấm vào từng biến thể.
- `bien_the_san_pham` tham chiếu `cpu_id`/`ram_id`/`gpu_id`/`o_cung_id` chỉ như **thông số kỹ thuật** (CPU/RAM của laptop này là gì), không phải hàng tồn kho có serial riêng. 4 bảng `dm_cpu`/`dm_ram`/`dm_gpu`/`dm_o_cung` hiện là dữ liệu tham chiếu thuần túy.
- Người dùng muốn theo dõi serial cho CPU/RAM/GPU/Ổ cứng ở mức **nội bộ** (nhập kho/truy vết bảo hành), KHÔNG bán rời linh kiện như 1 SKU độc lập.

## Mục tiêu

1. Giải quyết trùng lặp: giữ `SerialManager.vue` làm trang tổng hợp toàn hệ thống, `InventoryPanel.vue` giữ nguyên chức năng thêm/xóa nhanh theo biến thể — 2 bên dùng chung nguồn dữ liệu `chi_tiet_san_pham`, không đổi backend phần sản phẩm.
2. Di chuyển vị trí: bỏ sub-tab "Serial" khỏi trang Sản phẩm, thêm vào trang Kho hàng cạnh "Bảo hành".
3. Thêm khả năng theo dõi serial nội bộ cho CPU/RAM/GPU/Ổ cứng: bắt buộc nhập serial (đơn/nhiều/Excel) khi tạo mới 1 spec linh kiện, nhưng không hiện serial trên bảng danh sách.
4. Mở rộng `SerialManager.vue` thành nơi duy nhất để thêm serial cho CẢ sản phẩm lẫn linh kiện, qua 1 bộ chọn "Loại".

## Phần 1 — Backend: 4 bảng serial linh kiện mới

**Đã áp dụng vào `Database/QLBanMayTinh.sql`** (chèn ngay sau `chi_tiet_san_pham`, mục 5 "KHO HÀNG"), theo khuôn idempotent `IF NOT EXISTS` như các bảng khác:

```sql
CREATE TABLE chi_tiet_cpu (
    chi_tiet_cpu_id INT           IDENTITY(1,1) PRIMARY KEY,
    cpu_id          INT           NOT NULL,
    so_serial       VARCHAR(100)  NOT NULL,
    trang_thai      NVARCHAR(30)  NOT NULL DEFAULT N'trong_kho'
        CONSTRAINT CK_ctcpu_trangthai CHECK (trang_thai IN (N'trong_kho', N'da_su_dung', N'loi_bao_hanh')),
    ngay_nhap_kho   DATETIME      NOT NULL DEFAULT GETDATE(),
    ghi_chu         NVARCHAR(255) NULL,
    CONSTRAINT FK_ctcpu_cpu FOREIGN KEY (cpu_id) REFERENCES dm_cpu(cpu_id) ON DELETE CASCADE
);
-- UNIQUE INDEX UX_ctcpu_serial ON chi_tiet_cpu(so_serial)
```
(Lặp lại y hệt cho `chi_tiet_ram`/`ram_id`/`dm_ram`, `chi_tiet_gpu`/`gpu_id`/`dm_gpu`, `chi_tiet_o_cung`/`o_cung_id`/`dm_o_cung`.)

**Trạng thái riêng cho linh kiện** (khác 5 trạng thái của `chi_tiet_san_pham` vì không bán rời):
- `trong_kho` — còn hàng, chưa dùng
- `da_su_dung` — đã lắp vào máy (KHÔNG theo dõi lắp vào máy/serial sản phẩm nào cụ thể — ngoài phạm vi)
- `loi_bao_hanh` — lỗi, cần đổi trả nhà cung cấp

**Backend Java cần thêm** (theo khuôn `ChiTietSanPham`/`ChiTietSanPhamController` nhưng tối giản hơn, không cần validate phức tạp):
- 4 entity: `ChiTietCpu`, `ChiTietRam`, `ChiTietGpu`, `ChiTietOCung` — field tương ứng cột DB ở trên, `@ManyToOne` tới `DmCpu`/`DmRam`/`DmGpu`/`DmOcung`.
- 4 repository JPA (`JpaRepository<ChiTietCpu, Integer>`, ...).
- 4 controller REST (`/api/chi-tiet-cpu`, `/api/chi-tiet-ram`, `/api/chi-tiet-gpu`, `/api/chi-tiet-o-cung`), CRUD cơ bản (GET all, GET by spec id, POST tạo 1, DELETE — không cần PUT sửa vì serial linh kiện chỉ thêm/xóa, không sửa nội dung sau khi tạo, giống `ChiTietSanPham` KHÔNG cho sửa `soSerial` sau khi tạo trong thực tế dùng — chỉ sửa `trangThai`/`ghiChu` nên PUT vẫn cần để `SerialManager` sửa trạng thái/ghi chú).
- **Quyền:** `@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")` — giống `ChiTietSanPhamController`, KHÔNG phải Admin-only như `Dm*Controller`, vì Kho cần thêm serial qua `SerialManager` cho spec linh kiện đã có sẵn.

Sửa lại: 4 controller cần đủ CRUD (GET/POST/PUT update/DELETE) để khớp với `SerialManager.vue`'s sửa/xóa, đúng khuôn `ChiTietSanPhamController`.

## Phần 2 — Frontend: di chuyển vị trí UI

- **`AdminPage.vue` — trang Sản phẩm:** bỏ sub-tab "Serial" (thứ 6), quay về 5 sub-tab (Sản phẩm/CPU/RAM/GPU/Ổ cứng).
- **`AdminPage.vue` — trang Kho hàng:** thêm sub-tab thứ 3 "Serial" cạnh "Bảo hành" — biến `inventoryMainTab: 'kho' | 'bao-hanh' | 'serial'`, mount `<SerialManager />`.
- **`WarehouseManagementPage.vue`:** không đổi — Serial vẫn là nav top-level như hiện tại (đã đúng vị trí từ trước, vì trang này không có trang "Sản phẩm" để di dời).
- `InventoryPanel.vue`: không đổi.

## Phần 3 — Frontend: bắt buộc serial khi tạo mới linh kiện + SerialManager chọn loại

**`DmCategoryTable.vue` — modal "Thêm" (chỉ lúc TẠO MỚI, không áp dụng lúc Sửa):**
- Sau ô nhập tên spec, thêm phần "Serial" bắt buộc ≥1 dòng — tái dùng đúng UI đã có ở `InventoryPanel.vue` (`stockForm.newSerials`: nhập từng dòng, nút "+ thêm dòng", nút import file qua `importSerialsFromFile`, hỗ trợ `.csv/.txt/.xlsx/.xls`).
- Lưu: gọi tạo spec (`props.service.save(null, body)`) trước, lấy id vừa tạo từ response, rồi POST từng serial vào endpoint linh kiện tương ứng (`/api/chi-tiet-cpu` v.v., xác định qua 1 prop mới `serialApiPath` truyền vào component, vd `'chi-tiet-cpu'`).
- Modal "Sửa": giữ nguyên hiện tại — chỉ 1 ô tên, không đụng serial.
- Bảng danh sách: không đổi, không hiện tồn kho/serial.

**`SerialManager.vue` — thêm bộ chọn "Loại":**
- Modal "Thêm serial" thêm dropdown "Loại": Sản phẩm (mặc định) / CPU / RAM / GPU / Ổ cứng.
- Chọn "Sản phẩm": giữ nguyên form hiện tại (biến thể qua `SearchSelect` + `ChiTietSanPhamService`, 5 trạng thái).
- Chọn CPU/RAM/GPU/Ổ cứng: đổi dropdown chọn spec (dùng lại `DmService.getCpu()`/`getRam()`/`getGpu()`/`getOCung()` đã có), gọi endpoint linh kiện tương ứng, đổi trạng thái sang 3 lựa chọn (`trong_kho`/`da_su_dung`/`loi_bao_hanh`).
- Bảng danh sách: thêm cột "Loại" (Sản phẩm/CPU/RAM/GPU/Ổ cứng) để phân biệt nguồn dòng dữ liệu — dữ liệu bảng giờ là gộp (merge) từ `ChiTietSanPhamService.getAll()` + 4 service linh kiện mới, mỗi dòng gắn thêm field `loai` xác định khi merge ở frontend (không đổi backend để hợp nhất).

## Ngoài phạm vi (Non-goals)

- Không liên kết serial linh kiện với 1 serial sản phẩm cụ thể (không truy vết "thanh RAM này lắp vào máy nào") — trạng thái `da_su_dung` chỉ là cờ đánh dấu, không có FK.
- Không bán linh kiện rời như 1 SKU độc lập — không giá bán, không gắn `don_hang`/`chi_tiet_don_hang`.
- Không đổi cấu trúc nav của `WarehouseManagementPage.vue` (giữ 3 mục top-level riêng: Kho hàng/Bảo hành/Serial).
- Không đổi `ChiTietSanPhamController`/`ChiTietSanPhamService` (backend) — `SerialManager` gộp dữ liệu sản phẩm+linh kiện ở tầng frontend, không có endpoint backend hợp nhất.
- Không thêm sửa `soSerial` sau khi tạo (chỉ sửa trạng thái/ghi chú), đúng quy ước hiện có của `chi_tiet_san_pham`.

## Tự rà soát (self-review)

**1. Phủ đủ yêu cầu đã chốt qua các câu hỏi:**
- Vai trò SerialManager (trang tổng hợp, không gộp vào InventoryPanel) ✅
- Mục đích serial linh kiện (nội bộ, không bán rời) ✅
- Schema 4 bảng riêng theo khuôn Dm* ✅
- Serial chỉ bắt buộc lúc tạo mới, không bắt buộc lúc sửa ✅
- Không hiện tồn kho trên bảng danh sách linh kiện ✅
- WarehouseManagementPage giữ nguyên cấu trúc nav ✅
- Trạng thái riêng cho linh kiện (3 mức, không dùng chung 5 mức sản phẩm) ✅
- Di chuyển Serial từ trang Sản phẩm sang trang Kho hàng (cạnh Bảo hành) ✅

**2. Không còn placeholder** — schema đã áp dụng thật vào SQL và xác nhận chạy không lỗi; các quyết định API/permission đều dựa trên khuôn có sẵn (`ChiTietSanPhamController`).

**3. Nhất quán:** đã sửa mâu thuẫn nội bộ giữa Phần 1 (bản nháp đầu ghi "không cần PUT") và yêu cầu thực tế của `SerialManager` (cần sửa trạng thái/ghi chú) — chốt lại cần đủ CRUD 4 controller.

**4. Idempotency:** phần SQL đã viết và áp dụng dùng `IF NOT EXISTS`/`IF NOT EXISTS (SELECT 1 FROM sys.indexes ...)` nhất quán với toàn file, an toàn khi chạy lại nhiều lần.
