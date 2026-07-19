# Trả hàng & Ví khách hàng — Design Spec

**Ngày:** 2026-07-19

## Bối cảnh

Nhân viên (`StaffPage.vue`) và quản kho (`WarehouseManagementPage.vue`) hiện thiếu chức năng xử lý trả hàng. Backend đã có đầy đủ `PhieuTraHangController`/`ChiTietTraHangController` (CRUD phiếu trả hàng + dòng chi tiết) từ trước nhưng **chưa từng có frontend service/UI nào gọi tới** — cơ hội bổ sung tính năng lớn nhất vì chỉ cần xây lớp frontend, tận dụng backend có sẵn.

Trong lúc chốt thiết kế, phát sinh thêm yêu cầu: khi hoàn tiền trả hàng, tiền có thể vào **ví điện tử** của khách hàng (thay vì chỉ ghi 1 con số `so_tien_hoan` không có nơi chốn thật). Ví chỉ nhận tiền hoàn (không tiêu được ở checkout — ngoài phạm vi task này).

## Mục tiêu

1. Xây UI "Trả hàng" cho 3 vai trò, dùng đúng 1 component dùng chung.
2. Thêm ví (`so_du_vi`) cho khách hàng, tự động cộng tiền khi phiếu trả hàng hoàn tất với hình thức hoàn = "ví".
3. Khoá quyền backend cho 2 controller trả hàng hiện đang mở (chưa ai gọi tới nên khoá an toàn tuyệt đối).

## Phần 1 — UI Trả hàng

### Component dùng chung: `ReturnsPanel.vue`

Đặt tại `src/components/admin/ReturnsPanel.vue`, theo đúng khuôn `SupplierManager.vue` (CRUD đơn giản) kết hợp pattern header+dòng con của `InventoryPanel.vue` (tab phiếu nhập kho — xem `savePhieuNhap()` dùng diff `originalIds` để update/insert/xoá dòng con khi sửa).

Props: `{ readonly: { type: Boolean, default: false }, canPickStaff: { type: Boolean, default: false } }`.
- `readonly=true` (WarehouseManagementPage): ẩn nút Thêm/Sửa/Xoá, chỉ xem danh sách + xem chi tiết dòng.
- `canPickStaff=true` (AdminPage): hiện dropdown chọn nhân viên xử lý (tái dùng `StaffStore`/`ensureStaff()` + pattern `staffOptions` y hệt `InventoryPanel.vue:312`). Khi `false` (StaffPage): khoá cứng `nhanVienId = AuthStore.user.id`, hiện tên mình dạng text tĩnh, không cho đổi.

Component tự quản lý store/service riêng (theo đúng cách `OrdersTable.vue`/`InventoryPanel.vue` đang làm — import `OrdersStore`, `ensureOrders()`, `ProductsStore`, `ensureProducts()`, `StaffStore`, `ensureStaff()`, `AuthStore` trực tiếp, không nhận qua props từ trang cha).

### Service mới (frontend)

- `Service/PhieuTraHangService.js` — `getAll`, `getById`, `save(id, body)`, `remove(id)` — map `/api/phieu-tra-hang`, theo đúng khuôn `NhaCungCapService.js`.
- `Service/ChiTietTraHangService.js` — `getAll`, `create`, `update`, `remove` — map `/api/chi-tiet-tra-hang`, theo đúng khuôn `ChiTietPhieuNhapService.js`.

### Luồng tạo phiếu

1. Chọn đơn hàng (tìm theo mã đơn/SĐT khách trong `OrdersStore.items`).
2. Gọi `ChiTietDonHangService.getByDonHang(donHangId)` → liệt kê đúng các dòng đã bán của đơn đó (đã có sẵn `bienTheId`, `chiTietId`/serial, `donGia`, `maSku`, `soSerial`).
3. Nhân viên tick dòng cần trả, nhập số lượng, tình trạng (chuỗi tự do, ví dụ "tốt"/"lỗi" — backend `ChiTietTraHangService.laHangLoi()` đã tự nhận diện từ khoá "lỗi"/"hỏng" để quyết định có cộng lại tồn kho serial hay không, không cần đổi backend).
4. Nhập lý do, số tiền hoàn (mặc định = tổng `donGia * soLuong` các dòng đã chọn, cho sửa tay), hình thức hoàn (xem Phần 2), ghi chú.
5. Lưu: `PhieuTraHangService.save()` tạo/cập nhật header, sau đó diff `ChiTietTraHangService` cho các dòng con (giống `savePhieuNhap()`).

### Trạng thái phiếu (`trang_thai`)

Dùng đúng enum đã có sẵn trong CHECK constraint của bảng `phieu_tra_hang`: **`cho_xu_ly` / `da_xu_ly` / `tu_choi`** (khác enum của phiếu nhập kho — không nhầm lẫn 2 bảng).

### Vị trí gắn vào 3 trang

| Trang | Props | Vai trò |
|---|---|---|
| `StaffPage.vue` | (mặc định) | Tab "Trả hàng" mới, full CRUD, tự nhận `nhanVienId` = chính mình |
| `WarehouseManagementPage.vue` | `:readonly="true"` | Tab "Trả hàng" mới, chỉ xem — biết hàng nào vừa nhập lại kho |
| `AdminPage.vue` | `:can-pick-staff="true"` | Page mới `tra-hang` trong `PAGE_META`, full CRUD, chọn được nhân viên xử lý thay |

Mỗi trang thêm đúng 1 nav item + 1 `<section v-show>` theo khuôn đã có (xem `orders`/`products` trong `StaffPage.vue`, `suppliers` trong `WarehouseManagementPage.vue`).

### Khoá quyền backend

Thêm class-level `@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")` cho `PhieuTraHangController` và `ChiTietTraHangController` — đã xác nhận (grep toàn frontend) chưa có bất kỳ nơi nào trong luồng khách hàng gọi tới 2 controller này, khoá an toàn tuyệt đối, đúng tiền lệ `NhaCungCapController`/`TonKhoController` đã khoá trước đó.

## Phần 2 — Ví khách hàng

### Không xây bảng ledger riêng

Nhận ra `phieu_tra_hang` tự nó đã đủ làm sổ giao dịch cho ví: mỗi lần cộng ví gắn với đúng 1 phiếu trả hàng (`hinh_thuc_hoan='vi'` + `trang_thai='da_xu_ly'`). Không thêm bảng `giao_dich_vi` — dữ liệu sẽ trùng lặp, không ai truy vấn tới ledger riêng khi `phieu_tra_hang` đã trả lời được câu "tại sao số dư là X".

### Thay đổi DB (thêm vào cuối `Database/QLBanMayTinh.sql`, mỗi lệnh idempotent — vì user luôn chạy lại toàn bộ file)

```sql
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('khach_hang') AND name = 'so_du_vi')
BEGIN
    ALTER TABLE khach_hang ADD so_du_vi DECIMAL(18,0) NOT NULL DEFAULT 0
        CONSTRAINT CK_kh_sodu_vi CHECK (so_du_vi >= 0);
END

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('phieu_tra_hang') AND name = 'hinh_thuc_hoan')
BEGIN
    ALTER TABLE phieu_tra_hang ADD hinh_thuc_hoan NVARCHAR(20) NOT NULL DEFAULT N'vi'
        CONSTRAINT CK_pth_hinhthuchoan CHECK (hinh_thuc_hoan IN (N'tien_mat', N'vi'));
END
```

(Constraint 2 chỗ đặt tên `CK_kh_sodu_vi`/`CK_pth_hinhthuchoan` — kiểm tra không trùng tên constraint đã tồn tại trong file trước khi chạy thật.)

### Logic cộng ví (backend)

`PhieuTraHangService.create()` và `.update()`: sau khi lưu entity, nếu **`trang_thai` mới = `da_xu_ly`** và **`trang_thai` cũ ≠ `da_xu_ly`** (tránh cộng ví 2 lần khi sửa phiếu đã xử lý) và **`hinh_thuc_hoan='vi'`** và `so_tien_hoan > 0`:
- Lấy `KhachHang` qua `donHang.getKhachHang()`, cộng `soDuVi += soTienHoan`, save.
- Toàn bộ trong `@Transactional` (thêm annotation vào `PhieuTraHangService` — hiện chưa có).

`hinh_thuc_hoan='tien_mat'` → không đụng ví (nhân viên tự đưa tiền mặt ngoài hệ thống).

### Gate "tiền mặt chỉ dùng khi khách có mặt"

Trên form `ReturnsPanel.vue`: checkbox "Khách có mặt tại cửa hàng" — **chỉ là state UI, không lưu DB**. Mặc định bỏ tick → dropdown "Hình thức hoàn" chỉ có option "Ví". Tick vào mới hiện thêm option "Tiền mặt". Bản thân giá trị `hinh_thuc_hoan='tien_mat'` đã lưu tự nói lên khách từng có mặt — không cần cột thứ 2 để xác nhận lại chuyện đã xảy ra.

### Hiển thị cho khách hàng

- Thêm `soDuVi` (Integer/BigDecimal) vào `KhachHangResponse.java` và `KhachHangService` (map từ entity, không đổi logic khác).
- `AccountPage.vue`: thêm badge "💰 Số dư ví: {{ formatPrice(profile.soDuVi) }}" ngay cạnh badge "🎁 Điểm tích lũy" đã có sẵn (dòng ~225-228) — copy đúng pattern, đổi icon/label/field.

## Ngoài phạm vi (Non-goals)

- Không xây "tiêu ví lúc checkout" — ví hiện tại chỉ nhận tiền, không tiêu được. Để dành cho yêu cầu riêng khi cần.
- Không xây trang lịch sử giao dịch ví riêng cho khách hàng — mỗi phiếu trả hàng qua ví đã tự là 1 bản ghi tra cứu được (qua nhân viên/kho/admin), không cần UI riêng phía khách hàng cho việc này ở giai đoạn này.
- Không đụng `trangThaiDonHang` của đơn hàng — nhân viên tự bấm "Cập nhật" trên `OrdersTable` nếu muốn đổi đơn sang trạng thái "Đã trả hàng" (`returned`), tách biệt hoàn toàn khỏi phiếu trả hàng.
- Không xây phiếu bảo hành (`PhieuBaoHanhController`) — vẫn ngoài phạm vi như spec trước.
- Không nạp tiền vào ví qua cổng thanh toán ngoài — chỉ có 1 nguồn duy nhất cộng ví: hoàn tiền trả hàng.

## Tự rà soát (self-review)

**1. Phủ đủ yêu cầu đã chốt:**
- 3 trang đều có UI trả hàng theo đúng quyền hạn ✅
- Ví khách hàng tự động cộng khi hoàn tiền qua ví ✅
- Gate tiền mặt chỉ dùng khi khách có mặt ✅
- Khoá quyền backend 2 controller trả hàng ✅

**2. Không còn placeholder** — mọi bảng/cột/method/component đã xác định chính xác qua đọc code + schema thực tế.

**3. Nhất quán:** enum `trang_thai` của `phieu_tra_hang` (`cho_xu_ly`/`da_xu_ly`/`tu_choi`) đã xác nhận đúng từ CHECK constraint trong `QLBanMayTinh.sql`, không lẫn với enum phiếu nhập kho (`cho_duyet`/`hoan_thanh`/`huy`).

**4. Idempotency:** cả 2 câu lệnh `ALTER TABLE` đều bọc `IF NOT EXISTS` theo đúng convention file `.sql` hiện tại (chạy lại toàn bộ file không lỗi).
