# Phiếu bảo hành — Design Spec

**Ngày:** 2026-07-20

## Bối cảnh

Người dùng nhận xét tab "Bảo hành" ở trang quản lý kho (`AdminPage.vue`) "hơi trống vắng". Khảo sát cho thấy tab này hiện chỉ là 1 bảng đọc danh sách serial còn hạn bảo hành (`ChiTietSanPhamService.getUnderWarranty()` → `GET /api/chi-tiet-san-pham/con-bao-hanh`) — không có form tiếp nhận/xử lý gì cả, không liên kết tới thực thể `PhieuBaoHanh`. Trong khi đó backend đã có sẵn `PhieuBaoHanhController` full CRUD (`GET`/`POST`/`PUT`/`DELETE` tại `/api/phieu-bao-hanh`) nhưng **chưa từng có frontend service/UI nào gọi tới** — đúng tình trạng `PhieuTraHangController` trước khi được xây dựng ở spec [2026-07-19-tra-hang-vi-khach-hang-design.md](2026-07-19-tra-hang-vi-khach-hang-design.md).

`WarehouseManagementPage.vue` hiện **không có** tab bảo hành nào cả (bị loại khỏi phạm vi lúc tách trang staff/kho trước đây — xem [2026-07-18-staff-warehouse-pages-design.md](2026-07-18-staff-warehouse-pages-design.md): "tab bao-hanh hiện có trong khoTab... KHÔNG nằm trong phạm vi task này").

### Bug backend phát hiện trong lúc khảo sát (phải vá trước khi xây UI)

`PhieuBaoHanhRequest`/`PhieuBaoHanhResponse`/`PhieuBaoHanhService` có 2 lỗi tự mâu thuẫn, chưa từng lộ ra vì chưa ai gọi API này:

1. **`sanPhamId` mang 2 nghĩa khác nhau giữa đọc và ghi.** `PhieuBaoHanhService.create()`/`update()` dùng `request.getSanPhamId()` như thể nó là `bienTheId` (biến thể) — code tự comment thừa nhận: *"Request dùng sanPhamId nhưng entity cần bienThe (bienTheId) — sanPhamId trong request thực tế là bienTheId"*. Nhưng `PhieuBaoHanhRepository.hienThiPhieuBaoHanh()`'s JPQL lại lấy `p.bienThe.sanPham.sanPhamId` — tức là **san_pham_id thật** (ID sản phẩm cha, khác hẳn ID biến thể). Đọc 1 phiếu rồi gửi lại y nguyên để sửa sẽ vô tình lưu nhầm biến thể khác (2 ID gian trùng nhau tình cờ).
2. **`serialNumber` (String) trên Request hoàn toàn chết.** `create()`/`update()` đều bỏ qua field này — comment code: *"serialNumber (không có trong entity)"*. `PhieuBaoHanh.chiTietSanPham` (cột `chi_tiet_id`, FK tới serial cụ thể) **không bao giờ được set** dù entity đã map quan hệ này đầy đủ và cột DB cho phép NULL. Mọi phiếu tạo qua API hiện tại sẽ luôn có `chi_tiet_id = NULL`.

## Mục tiêu

1. Vá 2 lỗi backend trên — đổi tên field cho đúng nghĩa (khớp cả đọc/ghi), set lại `chiTietSanPham` khi có `chiTietId`.
2. Xây UI "Phiếu bảo hành" (tiếp nhận, xử lý, trả khách) dùng 1 component chung, gắn vào `WarehouseManagementPage.vue` (mới) và `AdminPage.vue` (thay tab đọc-only hiện có).
3. Khoá quyền backend cho `PhieuBaoHanhController` (hiện đang mở hoàn toàn).

## Phần 1 — Vá backend

### 1.1 Đổi tên field DTO cho đúng nghĩa

`PhieuBaoHanhRequest.java` / `PhieuBaoHanhResponse.java`:
- `sanPhamId` (Integer) → **`bienTheId`** (Integer, `@NotNull`) — khớp đúng cột `bien_the_id` cả đọc lẫn ghi, không còn 2 nghĩa khác nhau.
- `serialNumber` (String) → **`chiTietId`** (Integer, **không bắt buộc** — serial cụ thể là tuỳ chọn, đúng như cột DB `chi_tiet_id NULL`).

Theo đúng khuôn `ChiTietTraHangRequest`/`ChiTietTraHangResponse` đã dùng cho Trả hàng (cùng 1 pattern: FK biến thể bắt buộc, FK serial cụ thể tuỳ chọn).

### 1.2 Sửa `PhieuBaoHanhService`

`create()`/`update()`: dùng `bienTheSanPhamRepository.getReferenceById(request.getBienTheId())` (thay vì `getSanPhamId()`). Thêm logic set `chiTietSanPham` khi `request.getChiTietId() != null`:

```java
entity.setChiTietSanPham(request.getChiTietId() != null
        ? chiTietSanPhamRepository.getReferenceById(request.getChiTietId()) : null);
```

(Cần autowire thêm `ChiTietSanPhamRepository`.)

### 1.3 Sửa `PhieuBaoHanhRepository` JPQL

Đổi `p.bienThe.sanPham.sanPhamId` → `p.bienThe.bienTheId`. Thêm `p.bienThe.maSku` và `ctsp.chiTietId` vào constructor expression (giữ `ctsp.soSerial` để hiển thị) — Response cần đủ cả `chiTietId` (để sửa) lẫn `soSerial`/`maSku` (để hiển thị), giống `ChiTietTraHangResponse` có cả `chiTietId` lẫn `maSku`.

### 1.4 Nới lỏng validation

`ngayTiepNhan`, `ngayTraKhach`, `ketQuaXuLy` trên `PhieuBaoHanhRequest`: bỏ `@NotNull`/`@NotBlank` — lúc mới tiếp nhận (`trang_thai='con_bao_hanh'`) chưa thể có ngày trả khách/kết quả xử lý. Không thêm validation có điều kiện theo trạng_thái (giữ đơn giản — nếu sau này cần chặt hơn thì làm riêng).

### 1.5 Khoá quyền

Thêm class-level `@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")` cho `PhieuBaoHanhController` — đã xác nhận (grep toàn frontend) chưa có luồng khách hàng nào gọi tới, khoá an toàn tuyệt đối, đúng tiền lệ `PhieuTraHangController`/`NhaCungCapController`.

## Phần 2 — UI Phiếu bảo hành

### Component dùng chung: `WarrantyPanel.vue`

Đặt tại `src/components/admin/WarrantyPanel.vue`. Gồm 2 phần trong cùng 1 component:

1. **Bảng "Còn hạn bảo hành"** — chuyển nguyên xi code hiện đang nằm thẳng trong `AdminPage.vue` (dòng ~1230-1276: search, đếm, bảng serial/SP/khách/đơn/ngày giao/ngày hết hạn/badge ngày còn lại) thành component riêng, dùng `ChiTietSanPhamService.getUnderWarranty()` y nguyên. Thêm 1 cột thao tác mới: nút "Tạo phiếu bảo hành" trên từng dòng.
2. **Bảng "Phiếu bảo hành"** — danh sách CRUD `PhieuBaoHanhService`, theo đúng khuôn `ReturnsPanel.vue` (list + modal tạo/sửa). Không có prop `readonly`/`canPickStaff` — cả 2 trang mount đều full CRUD như nhau, và bảng `phieu_bao_hanh` không có cột nhân viên xử lý nên không cần dropdown chọn người xử lý.

### Luồng tạo phiếu

Từ bảng "Còn hạn bảo hành", bấm "Tạo phiếu bảo hành" trên 1 dòng → mở modal tạo mới, tự điền sẵn (không cho sửa các FK này trong modal — đã chọn đúng từ dòng bấm vào):
- `donHangId` ← suy ra từ `maDonHang` của dòng (cần `OrdersStore` để tra `donHangId` theo `maDonHang`, hoặc thêm `donHangId` vào `WarrantyStatusResponse` — **quyết định: thêm `donHangId` vào `WarrantyStatusResponse`**, đơn giản hơn tra cứu chéo qua mã đơn ở frontend).
- `bienTheId` ← cần thêm vào `WarrantyStatusResponse` tương tự (hiện tại response chỉ có `maSku`/`tenSanPham`, không có ID biến thể).
- `chiTietId` ← chính là `chiTietId` của dòng.
- `khachHangId` ← cần thêm vào `WarrantyStatusResponse` (hiện chỉ có tên/SĐT hiển thị, không có ID).
- `ngayMua` ← `ngayGiaoThucTe` của dòng (ngày giao thực tế — không có "ngày mua" riêng trong dữ liệu nguồn, dùng ngày giao làm ngày mua, hợp lý về nghiệp vụ).
- `ngayHetBh` ← `ngayHetBaoHanh` của dòng (đã tính sẵn ở server).

Nhân viên nhập tiếp: `moTaLoi` (bắt buộc), `trangThai` (mặc định `con_bao_hanh`), `ghiChu`. Các trường `ngayTiepNhan`/`ngayTraKhach`/`ketQuaXuLy`/`chiPhiPhatSinh` để trống/0, sửa sau khi xử lý.

**Bổ sung `WarrantyStatusResponse`** (`chi_tiet_san_pham` liên quan): thêm 3 field mới — `donHangId`, `bienTheId`, `khachHangId` — vào constructor JPQL của `ChiTietSanPhamService.getStillUnderWarranty()`, chỉ thêm ID thô để frontend dùng khi tạo phiếu, không đổi field hiển thị đã có.

### Trạng thái phiếu (`trang_thai`)

Enum CHECK constraint có sẵn trong DB: `con_bao_hanh` (mới tiếp nhận/còn hạn) / `dang_xu_ly` (đang sửa chữa) / `da_xu_ly` (đã trả khách) / `het_bao_hanh` (từ chối — hết hạn BH) / `tu_choi` (từ chối — lý do khác, vd hư hỏng do người dùng).

### Vị trí gắn vào 2 trang

| Trang | Nội dung |
|---|---|
| `WarehouseManagementPage.vue` | Tab mới "Bảo hành" — mount `<WarrantyPanel />`, full CRUD. |
| `AdminPage.vue` | Thay nội dung `<div v-show="inventoryMainTab==='bao-hanh'">` hiện có (bảng đọc-only code thẳng) bằng `<WarrantyPanel />`, full CRUD. Nav tab "🛡️ Bảo hành" giữ nguyên vị trí/tên. |

Không gắn vào `StaffPage.vue`.

## Ngoài phạm vi (Non-goals)

- Không thêm cột/nút "Tạo phiếu bảo hành" ngay trong luồng bán hàng (POS) hay trang tài khoản khách hàng — chỉ làm phía kho/admin theo đúng yêu cầu.
- Không thêm validation có điều kiện theo `trang_thai` (ví dụ bắt buộc `ketQuaXuLy` khi `trang_thai='da_xu_ly'`) — chỉ nới lỏng thành optional đơn giản.
- Không xây thông báo/email cho khách khi phiếu bảo hành đổi trạng thái.
- Không đụng tới `existsByBienThe_BienTheId` (method có sẵn trong repository, không dùng tới, không phải phạm vi task này).

## Tự rà soát (self-review)

**1. Phủ đủ yêu cầu đã chốt:**
- Vá 2 lỗi backend (sanPhamId/bienTheId lẫn nghĩa, serialNumber/chiTietId chết) ✅
- Nới lỏng validation 3 trường theo quyết định người dùng ✅
- Khoá quyền backend ✅
- UI Phiếu bảo hành tích hợp vào bảng còn hạn BH sẵn có ✅
- Gắn vào Kho + Admin, không đụng Staff ✅

**2. Không còn placeholder** — mọi field/method/component đã xác định chính xác qua đọc code thực tế.

**3. Nhất quán:** enum `trang_thai` của `phieu_bao_hanh` (`con_bao_hanh`/`dang_xu_ly`/`da_xu_ly`/`het_bao_hanh`/`tu_choi`) xác nhận đúng từ CHECK constraint trong `QLBanMayTinh.sql`, không lẫn với enum khác. Field rename (`bienTheId`/`chiTietId`) khớp đúng convention đã dùng ở `ChiTietTraHangRequest`/`Response`.

**4. Idempotency:** không có thay đổi DB schema trong spec này (chỉ sửa Java) — không cần lệnh SQL mới.
