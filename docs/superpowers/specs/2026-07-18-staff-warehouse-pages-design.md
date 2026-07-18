# StaffPage & WarehouseManagementPage — Design Spec

**Ngày:** 2026-07-18

## Bối cảnh

Hiện tại `AuthStore.isAdmin` gộp cả 3 vai trò staff (`admin`, `nhan_vien`, `quan_kho`) làm một — `App.vue.onLoginSuccess()` đẩy cả 3 vào `#admin`, và `App.vue` render `<AdminPage v-if="isAdminHash && auth.isAdmin">` không phân biệt vai trò. Nghĩa là nhân viên bán hàng và quản lý kho hiện đang thấy **y hệt** dashboard admin đầy đủ — bao gồm cả các mục mà backend đã chặn quyền (Cài đặt, Nhân viên), dẫn tới trải nghiệm có nút bấm vào là lỗi 403.

`AdminPage.vue` (~5300 dòng) là 1 file monolithic chứa toàn bộ: Dashboard, Sản phẩm, Đơn hàng, Khách hàng, Kho hàng (Tồn kho + Phiếu nhập kho), Khuyến mãi, Bán hàng (POS), Nhân viên, Báo cáo, Cài đặt.

## Mục tiêu

Tách 2 trang riêng theo đúng vai trò:
- **StaffPage** (`nhan_vien`): Bán hàng (POS), Đơn hàng, Khách hàng, Sản phẩm (chỉ xem).
- **WarehouseManagementPage** (`quan_kho`): Kho hàng/Tồn kho, Phiếu nhập kho, Nhà cung cấp, Lịch sử tồn kho.

`admin` role tiếp tục dùng AdminPage đầy đủ như hiện tại, không đổi.

## Kiến trúc

### Routing (App.vue)

`onLoginSuccess()` hiện tại:
```js
const staffRoles = ["admin", "nhan_vien", "quan_kho"];
if (staffRoles.includes(user.role)) {
  window.location.hash = "#admin";
}
```
Đổi thành route theo đúng role:
```js
const ROLE_HASH = { admin: "#admin", nhan_vien: "#staff", quan_kho: "#kho" };
if (ROLE_HASH[user.role]) {
  window.location.hash = ROLE_HASH[user.role];
}
```

`App.vue` cần 2 computed hash mới (`isStaffHash`, `isKhoHash`, theo đúng pattern `isAdminHash` hiện có) và 2 nhánh render mới trong template, mỗi nhánh check đúng cả hash lẫn role (không chỉ check "là staff nói chung" — phải đúng role tương ứng, để nhân viên không gõ tay `#kho` vào URL mà vào được trang kho):
```html
<StaffPage v-if="isStaffHash && auth.user?.role === 'nhan_vien'" />
<WarehouseManagementPage v-else-if="isKhoHash && auth.user?.role === 'quan_kho'" />
```
Nhánh "từ chối quyền truy cập" hiện có (khi vào `#admin` mà không phải admin) nhân bản tương tự cho 2 hash mới — tái dùng cùng 1 section markup, đổi hash cần check.

`AuthStore.isAdmin` (đang là "isStaff" trá hình) giữ nguyên ý nghĩa "là 1 trong 3 role staff" — dùng cho NavBar ẩn nút giỏ hàng/tài khoản khách hàng khi user là staff bất kỳ. Không đổi tên biến này (tránh diff không cần thiết ở NavBar.vue và nơi khác đang dùng).

### 2 file trang mới

- `src/pages/StaffPage.vue`
- `src/pages/WarehouseManagementPage.vue`

Cả 2 theo đúng shell layout của AdminPage.vue: `<aside>` sidebar 240px (logo + nav + footer user-menu) + `<main>` (topbar + nội dung theo `currentPage`). Mỗi trang có `<script setup>` riêng, độc lập với AdminPage — không import hay kế thừa script của AdminPage. Dùng lại các phần hạ tầng chung sẵn có (không đổi): `AuthStore`, `t`/`I18nStore`, `ThemeStore`/`toggleTheme`, `SettingsStore`, `formatPrice`, `askConfirm`, `authHeaders`.

`userDisplayName`/`userAvatar`/`userDisplayRole`/`logout` (AdminPage.vue:77-90) đã role-agnostic sẵn (đọc `AuthStore.user.role` để suy ra nhãn hiển thị) — copy y nguyên sang cả 2 trang mới, không cần sửa.

`PAGE_META`/`topbarTitle`/`topbarSub`/`topbarIcon` (AdminPage.vue:58-74) mỗi trang tự định nghĩa bảng `PAGE_META` riêng, chỉ chứa các key của trang đó (StaffPage: `ban-hang`, `orders`, `customers`, `products`; Warehouse: `inventory`, `phieu-nhap`, `nha-cung-cap`, `lich-su-ton-kho`), tái dùng đúng các i18n key đã có trong `admin.pageMeta.*` cho phần trùng tên, thêm key mới cho phần chưa có (`phieu-nhap` đang là tab con của `inventory` chứ chưa có `pageMeta` riêng; `nha-cung-cap`, `lich-su-ton-kho` hoàn toàn mới).

## Component dùng chung (rút từ AdminPage.vue)

Chuyển vào `src/components/admin/`, mỗi component nhận state qua props + emit event thay vì đọc trực tiếp biến ambient của AdminPage — để AdminPage, StaffPage, WarehouseManagementPage đều gọi được độc lập.

| Component | Rút từ AdminPage.vue | Nội dung | Dùng ở |
|---|---|---|---|
| `UserProfileMenu.vue` | dòng 2870-2904 (+ state dòng ~86-160 vừa xây cho plan menu-ho-so-admin) | Dropdown hồ sơ chân sidebar (Chỉnh sửa hồ sơ/Đổi mật khẩu/Cài đặt) + 2 modal | AdminPage + StaffPage + WarehouseManagementPage |
| `ProductsTable.vue` | 3162-3200 | Bảng sản phẩm (nhóm theo sanPhamId, số biến thể, giá thấp nhất) | AdminPage (đủ quyền) + StaffPage (`:readonly="true"` — ẩn nút Thêm/Xoá) |
| `OrdersTable.vue` | 3203-3297 | Bảng đơn hàng + lọc trạng thái/thanh toán + xem lịch sử theo ngày | AdminPage (đủ quyền) + StaffPage (`:can-delete="false"` — ẩn nút Xoá, giữ Xem chi tiết/Cập nhật trạng thái) |
| `CustomersTable.vue` | 3300-3334 | Bảng khách hàng | AdminPage + StaffPage |
| `PosPanel.vue` | 3968-4088 + modal chọn serial + modal đơn đang giữ (quanh 4209+) + state liên quan (~20 ref: `posCart`, `posStage`, `posPhone`, `heldOrders`, `showHeldOrders`...) | Toàn bộ luồng bán hàng tại quầy | AdminPage (tab Bán hàng) + StaffPage |
| `TonKhoPanel.vue` | phần tab `khoTab==='ton-kho'` trong 3335-3661 | Xem/điều chỉnh tồn kho theo biến thể | AdminPage (tab Kho hàng) + WarehouseManagementPage |
| `PhieuNhapKhoPanel.vue` | phần tab `khoTab==='phieu-nhap'` trong 3335-3661 | Tạo/xem phiếu nhập kho | AdminPage (tab Kho hàng) + WarehouseManagementPage |

**Lưu ý phạm vi:** tab `bao-hanh` hiện có trong `khoTab` (AdminPage.vue:663) KHÔNG nằm trong phạm vi task này — giữ nguyên trong AdminPage, không rút ra, không thêm vào WarehouseManagementPage (người dùng không chọn mục này khi chốt scope).

## Component xây mới hoàn toàn

Backend đã có đủ API (`NhaCungCapController`, `LichSuTonKhoController`) nhưng chưa có bất kỳ service/UI frontend nào:

- **`SupplierManager.vue`** + **`Service/NhaCungCapService.js`** — CRUD nhà cung cấp (list/create/update/delete), map đúng `NhaCungCapController`: `GET /api/nha-cung-cap`, `POST /api/nha-cung-cap`, `PUT /api/nha-cung-cap/update/{id}`, `DELETE /api/nha-cung-cap/delete/{id}`.
- **`InventoryHistoryPanel.vue`** + **`Service/LichSuTonKhoService.js`** — xem lịch sử biến động tồn kho, chỉ đọc (`GET /api/lich-su-ton-kho`) — backend cố ý không có endpoint update (audit trail chỉ ghi thêm/xoá).

## Khoá quyền backend

Đã xác nhận từng method có bị gọi bởi luồng khách hàng (checkout, AccountPage) hay không trước khi quyết định — tránh làm hỏng mua hàng đang chạy.

**Class-level `@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")`** (an toàn tuyệt đối — xác nhận không nơi nào trong code khách hàng gọi tới):
- `TonKhoController`
- `PhieuNhapKhoController`
- `ChiTietPhieuNhapController`
- `NhaCungCapController`
- `LichSuTonKhoController`
- `ThanhToanController` (hiện không có bất kỳ service/component frontend nào gọi tới controller này)

**Method-level trên `DonHangController`** (class giữ mở, chỉ khoá 4 method xác nhận chỉ AdminPage gọi):
- `update` (PUT `/update/{id}`), `merge` (POST `/merge`), `recalculate` (PATCH `/{id}/recalculate`), `xacNhan` (PATCH `/{id}/xac-nhan`) → `@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")`
- `create`, `delete`, `getAll`/`getPage`, `getById`, `subscribe` (SSE `/events`) → **giữ nguyên mở**, do `create`/`delete` được `CheckoutModal.vue` gọi (đặt hàng + rollback khi lỗi), `getAll`/`getPage` được `AccountPage.vue` gọi qua tham số `khachHangId` (đơn hàng của tôi), `subscribe` được cả `AccountPage.vue` lẫn `AdminPage.vue` dùng cho realtime.

**Method-level trên `ChiTietDonHangController`** (class giữ mở, chỉ khoá 4 method hiện không có nơi nào trong frontend gọi tới — khoá an toàn tuyệt đối):
- `getAll`, `getById`, `update`, `delete` → `@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")`
- `create`, `getByDonHang` → giữ nguyên mở (checkout tạo chi tiết đơn, AccountPage xem chi tiết đơn của mình).

**Ngoài phạm vi:** `PhieuTraHangController`, `ChiTietTraHangController`, `PhieuBaoHanhController`, `DiaChiGiaoHangController` — không đụng tới, vì "Phiếu trả hàng"/"Bảo hành" không nằm trong scope 2 trang này.

## Ngoài phạm vi (Non-goals)

- Không refactor toàn bộ AdminPage.vue thành nhiều file nhỏ — chỉ rút đúng 7 component ở bảng trên, phần còn lại (Dashboard, Khuyến mãi, Nhân viên, Báo cáo, Cài đặt) giữ nguyên trong AdminPage.vue.
- Không xây quyền ownership-level (vd nhân viên chỉ thấy đơn hàng do chính mình xử lý) — "Báo cáo cá nhân" đã bị loại khỏi scope khi chốt yêu cầu.
- Không đụng `PhieuTraHangController`/`PhieuBaoHanhController`/`DiaChiGiaoHangController`.
- Không thêm ownership-check cho `DonHangController.getAll` (khách hàng đổi `khachHangId` trên URL để xem đơn người khác) — đây là lỗ hổng khác, không phát sinh từ việc thiếu `@PreAuthorize`, để dành cho yêu cầu riêng.

## Tự rà soát (self-review)

**1. Phủ đủ yêu cầu đã chốt:**
- StaffPage = Bán hàng + Đơn hàng + Khách hàng + Sản phẩm (chỉ xem) ✅
- WarehouseManagementPage = Kho hàng/Tồn kho + Phiếu nhập kho + Nhà cung cấp + Lịch sử tồn kho ✅
- Routing theo role, admin không đổi ✅
- Tách component dùng chung (lựa chọn được xác nhận qua AskUserQuestion) ✅
- Khoá quyền backend, đã rà từng method để không phá checkout ✅

**2. Không còn placeholder** — mọi component/controller/method đều đã được xác định chính xác qua đọc code thực tế, không suy đoán.

**3. Nhất quán:** tên component trong bảng "component dùng chung" và "component xây mới" khớp với tên dùng ở phần routing/kiến trúc.
