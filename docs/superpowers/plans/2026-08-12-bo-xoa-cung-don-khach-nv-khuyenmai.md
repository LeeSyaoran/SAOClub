# Bỏ xóa cứng cho Đơn hàng, Khách hàng, Nhân viên, Khuyến mãi — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Gỡ bỏ tính năng xóa cứng (backend endpoint + nút UI) cho Khách hàng, Nhân viên, Khuyến mãi; riêng Đơn hàng chỉ gỡ nút UI admin, giữ nguyên backend vì còn dùng cho rollback nội bộ khi checkout/POS lỗi.

**Architecture:** Xóa code chết theo cặp Controller+Service (backend) và hàm-xử-lý+nút+service-call (frontend), dọn theo i18n key liên quan. Không thêm logic mới — vòng đời 4 entity này từ nay chỉ qua các form Sửa đã có sẵn dropdown trạng thái.

**Tech Stack:** Spring Boot (Java), Vue 3 `<script setup>`, i18n thuần object literal (`vi.js`/`en.js`).

## Global Constraints

- Spec gốc: `docs/superpowers/specs/2026-08-12-bo-xoa-cung-don-khach-nv-khuyenmai-design.md` — mọi task dưới đây phải khớp đúng phạm vi đó.
- **Đơn hàng (`DonHang`) KHÔNG được đụng backend** — `DonHangController.delete()`/`DonHangService.delete()`/`DonHangService.js remove()` phải giữ nguyên 100%, vì `CheckoutModal.vue:719` và `PosPanel.vue:549` còn gọi để rollback đơn "pending" tạo lỗi giữa chừng.
- Không xóa i18n key `admin.errors.deleteFailed` — vẫn dùng chung bởi các nút xóa khác còn lại (Biến thể, Nhà cung cấp, Phiếu trả hàng...).
- Không đụng `BienTheSanPham`, `ChiTietSanPham`, `NhaCungCap`, `DmDoiThuong` — ngoài phạm vi đã chốt.

---

### Task 1: Backend — bỏ hard-delete cho Khách hàng, Nhân viên, Khuyến mãi

**Files:**
- Modify: `BackEnd/src/main/java/com/example/backend/controller/KhachHangController.java:55,68-73`
- Modify: `BackEnd/src/main/java/com/example/backend/service/KhachHangService.java:138-143`
- Modify: `BackEnd/src/main/java/com/example/backend/controller/NhanVienController.java:52-56`
- Modify: `BackEnd/src/main/java/com/example/backend/service/NhanVienService.java:92-97`
- Modify: `BackEnd/src/main/java/com/example/backend/controller/KhuyenMaiController.java` (method `delete`)
- Modify: `BackEnd/src/main/java/com/example/backend/service/KhuyenMaiService.java:43-47`
- Test: chạy lại `BackEnd/src/test/java/com/example/backend/service/KhachHangServiceTest.java` (không có test nào gọi `.delete()` — xác nhận không vỡ)

**Interfaces:**
- Consumes: không có, đây là xóa code không ai khác gọi tới (đã grep xác nhận đúng 1 call site mỗi service trước khi xóa, xem spec).
- Produces: `KhachHangService`/`NhanVienService`/`KhuyenMaiService` không còn method `delete(Integer id)`. Task 3 (frontend) dựa vào việc backend 404 khi gọi các endpoint này để xác nhận đã gỡ đúng.

- [ ] **Step 1: Xóa method `delete` khỏi `KhachHangController.java`**

Xóa đúng khối này (dòng 55, 68-73):
```java
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        khachHangService.delete(id);
        return ResponseEntity.noContent().build();
    }

```
(giữ nguyên toàn bộ các method khác trong file)

- [ ] **Step 2: Xóa method `delete` khỏi `KhachHangService.java`**

Xóa đúng khối này (dòng 138-143):
```java
    @Transactional
    public void delete(Integer id) {
        if (!khachHangRepository.existsById(id))
            throw new IllegalArgumentException("Khách hàng không tồn tại với id: " + id);
        khachHangRepository.deleteById(id);
    }

```

- [ ] **Step 3: Xóa method `delete` khỏi `NhanVienController.java`**

Xóa đúng khối này (dòng 52-56):
```java
    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        nhanVienService.delete(id);
        return ResponseEntity.noContent().build();
    }
```

- [ ] **Step 4: Xóa method `delete` khỏi `NhanVienService.java`**

Xóa đúng khối này (dòng 92-97):
```java
    @Transactional
    public void delete(Integer id) {
        if (!nhanVienRepository.existsById(id))
            throw new IllegalArgumentException("Nhân viên không tồn tại với id: " + id);
        nhanVienRepository.deleteById(id);
    }

```

- [ ] **Step 5: Xóa method `delete` khỏi `KhuyenMaiController.java`**

Đọc file trước để lấy đúng đoạn `@DeleteMapping("delete/{id}") public ResponseEntity<Void> delete(...)` (theo cùng mẫu 3 controller trên) rồi xóa nguyên khối method đó — kể cả `@PreAuthorize` phía trên nếu có.

- [ ] **Step 6: Xóa method `delete` khỏi `KhuyenMaiService.java`**

Xóa đúng khối này:
```java
    public void delete(Integer id) {
        if (!khuyenMaiRepository.existsById(id))
            throw new IllegalArgumentException("Khuyến mãi không tồn tại với id: " + id);
        khuyenMaiRepository.deleteById(id);
    }
```

- [ ] **Step 7: Build backend, xác nhận compile sạch**

Run: `cd BackEnd && ./mvnw compile -q` (hoặc `mvn compile -q` tùy máy)
Expected: BUILD SUCCESS, không còn tham chiếu nào tới 3 method vừa xóa (nếu còn nơi gọi sót, compile sẽ báo lỗi rõ ràng — quay lại tìm và xóa nốt).

- [ ] **Step 8: Chạy full test suite backend, xác nhận không vỡ test nào**

Run: `cd BackEnd && ./mvnw test -q`
Expected: tất cả test hiện có PASS — đặc biệt `KhachHangServiceTest`, `DonHangServiceTest` (không đụng `DonHang` nên phải xanh y nguyên).

- [ ] **Step 9: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/controller/KhachHangController.java \
        BackEnd/src/main/java/com/example/backend/service/KhachHangService.java \
        BackEnd/src/main/java/com/example/backend/controller/NhanVienController.java \
        BackEnd/src/main/java/com/example/backend/service/NhanVienService.java \
        BackEnd/src/main/java/com/example/backend/controller/KhuyenMaiController.java \
        BackEnd/src/main/java/com/example/backend/service/KhuyenMaiService.java
git commit -m "refactor(backend): bỏ xóa cứng khách hàng/nhân viên/khuyến mãi

Cả 3 xóa cứng không kiểm tra ràng buộc, vỡ khi khách/nhân viên/khuyến mãi
đã dính đơn hàng. Vòng đời từ nay chỉ qua đổi trạng thái (form Sửa đã có
sẵn dropdown active/inactive)."
```

---

### Task 2: Frontend — bỏ nút Xóa + service call cho Khách hàng, Nhân viên, Khuyến mãi

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/CustomersTable.vue:31-36,67`
- Modify: `FrontEnd/QLBanMayTinh/src/services/KhachHangService.js:12`
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue:731-736,812-817,1373,1437`
- Modify: `FrontEnd/QLBanMayTinh/src/services/NhanVienService.js:10`
- Modify: `FrontEnd/QLBanMayTinh/src/services/KhuyenMaiService.js:8`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js` (key `customers.delete`, `staff.delete`, `promotions.delete`, `confirm.deleteCustomer`, `confirm.deleteStaff`, `confirm.deletePromo`)
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/en.js` (same keys)

**Interfaces:**
- Consumes: Task 1 xong (backend endpoint đã 404) — không bắt buộc thứ tự nhưng test cuối cùng (Task 5) cần cả hai.
- Produces: không còn UI nào gọi `KhachHangService.remove`/`NhanVienService.remove`/`KhuyenMaiService.remove`.

- [ ] **Step 1: `CustomersTable.vue` — xóa hàm `deleteCustomer` và nút Xóa**

Xóa khối hàm (dòng 31-36):
```javascript
const deleteCustomer = async (id) => {
  if (!(await askConfirm(t('admin.confirm.deleteCustomer')))) return;
  const res = await KhachHangService.remove(id);
  if (!res.ok) { showToast(t('admin.errors.deleteFailed', { status: res.status })); return; }
  await refreshCustomers();
};

```

Trong `<template>`, sửa dòng chứa nút Xóa (dòng 67), từ:
```html
              <button class="btn btn-sm btn-outline-danger" style="font-size:0.78rem; padding:2px 8px;" @click="deleteCustomer(c.khachHangId)">{{ t('admin.customers.delete') }}</button>
```
thành: xóa hẳn dòng này (giữ nguyên 2 nút "Chi tiết"/"Sửa" phía trên nó).

- [ ] **Step 2: `KhachHangService.js` — xóa `remove()`**

Xóa dòng 12:
```javascript
export const remove = (id) => del(`/api/khach-hang/delete/${id}`);
```
Kiểm tra import `del` ở dòng 1 còn dùng ở đâu khác trong file không (hiện không) — nếu không còn dùng, xóa `del` khỏi import list.

- [ ] **Step 3: `AdminPage.vue` — xóa hàm `deleteStaff` và nút Xóa nhân viên**

Xóa khối hàm (dòng 731-736):
```javascript
const deleteStaff = async (id) => {
  if (!(await askConfirm(t('admin.confirm.deleteStaff')))) return;
  const res = await NhanVienService.remove(id);
  if (!res.ok) { showToast(t('admin.errors.deleteFailed', { status: res.status })); return; }
  await refreshStaff();
};

```
Trong template, xóa dòng nút Xóa nhân viên (dòng 1437):
```html
                      <button class="btn btn-sm btn-outline-danger" style="font-size:0.78rem; padding:2px 8px;" @click="deleteStaff(s.nhanVienId)">{{ t('admin.staff.delete') }}</button>
```

- [ ] **Step 4: `AdminPage.vue` — xóa hàm `deletePromo` và nút Xóa khuyến mãi**

Xóa khối hàm (dòng 812-817):
```javascript
const deletePromo = async (id) => {
  if (!(await askConfirm(t('admin.confirm.deletePromo')))) return;
  const res = await KhuyenMaiService.remove(id);
  if (!res.ok) { showToast(t('admin.errors.deleteFailed', { status: res.status })); return; }
  await refreshPromotions();
};

```
Trong template, xóa dòng nút Xóa khuyến mãi (dòng 1373):
```html
                      <button class="btn btn-sm btn-outline-danger" style="font-size:0.78rem; padding:2px 8px;" @click="deletePromo(p.khuyenMaiId)">{{ t('admin.promotions.delete') }}</button>
```

- [ ] **Step 5: `NhanVienService.js` / `KhuyenMaiService.js` — xóa `remove()`**

`NhanVienService.js` dòng 10: xóa `export const remove = (id) => del(\`/api/nhan-vien/delete/${id}\`);`
`KhuyenMaiService.js` dòng 8: xóa `export const remove = (id) => del(\`/api/khuyen-mai/delete/${id}\`);`
Cả 2 file: nếu `del` không còn dùng ở đâu khác trong file, xóa khỏi import list dòng 1.

- [ ] **Step 6: i18n — xóa 3 key label + 3 key confirm khỏi `vi.js` và `en.js`**

Trong `vi.js`: xóa `delete: "Xóa",` ở đúng 3 namespace — `customers` (dòng ~897, cạnh `viewDetail`/`empty: "Chưa có khách hàng"`), `promotions` (dòng ~1376, cạnh `empty: "Chưa có khuyến mãi"`), `staff` (dòng ~1462, cạnh `empty: "Chưa có nhân viên"`). **Không đụng** các `delete: "Xóa"` khác trong file (products, variants, suppliers, returns, warranty, dm, serial, rewards, orderDetailModal...).

Trong khối `confirm: {...}`, xóa 3 dòng:
```javascript
      deleteCustomer: "Xóa khách hàng này?",
      deleteStaff: "Xóa nhân viên này?",
      deletePromo: "Xóa khuyến mãi này?",
```
(giữ nguyên `deleteVariantSimple`, `deleteReward`, `deleteOrder`, `removeItemFromOrder`).

Lặp lại tương tự trên `en.js` (label `delete: "Delete",` ở 3 namespace tương ứng, và 3 dòng trong `confirm`: `deleteCustomer`, `deleteStaff`, `deletePromo`).

- [ ] **Step 7: Build frontend, xác nhận không còn tham chiếu vỡ**

Run: `cd FrontEnd/QLBanMayTinh && npm run build`
Expected: build thành công, không lỗi "used before defined"/import thiếu (nếu `deleteCustomer`/`deleteStaff`/`deletePromo` còn sót chỗ gọi nào khác, build sẽ báo lỗi rõ).

- [ ] **Step 8: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/admin/CustomersTable.vue \
        FrontEnd/QLBanMayTinh/src/services/KhachHangService.js \
        FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue \
        FrontEnd/QLBanMayTinh/src/services/NhanVienService.js \
        FrontEnd/QLBanMayTinh/src/services/KhuyenMaiService.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/en.js
git commit -m "refactor(frontend): bỏ nút Xóa khách hàng/nhân viên/khuyến mãi

Backend đã bỏ endpoint tương ứng (commit trước). Vòng đời từ nay chỉ qua
form Sửa (đổi trạng thái active/inactive) đã có sẵn."
```

---

### Task 3: Frontend — bỏ nút Xóa Đơn hàng (chỉ UI admin, KHÔNG đụng backend)

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/OrdersTable.vue:23,109-114,657`
- Modify: `FrontEnd/QLBanMayTinh/src/pages/StaffPage.vue:155`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js` (key `orders.delete`, `confirm.deleteOrder`)
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/en.js` (same keys)

**Interfaces:**
- Consumes: không có.
- Produces: `OrdersTable.vue` không còn prop `canDelete` — cả `AdminPage.vue:1273` (`<OrdersTable />`, không truyền gì) lẫn `StaffPage.vue:155` (`<OrdersTable :can-delete="false" />`) đều phải hết truyền prop này. `DonHangService.js` **không đổi** — `remove()` vẫn còn nguyên cho `CheckoutModal.vue`/`PosPanel.vue` dùng.

- [ ] **Step 1: `OrdersTable.vue` — xóa prop `canDelete`**

Dòng 23, từ:
```javascript
const props = defineProps({ canDelete: { type: Boolean, default: true } });
```
Xóa hẳn dòng này (không còn dùng `defineProps` nào khác trong file → xóa cả dòng, không để lại `defineProps({})` rỗng).

- [ ] **Step 2: `OrdersTable.vue` — xóa hàm `deleteOrder`**

Xóa khối (dòng 109-114):
```javascript
const deleteOrder = async (id) => {
  if (!(await askConfirm(t('admin.confirm.deleteOrder')))) return;
  const res = await DonHangService.remove(id);
  if (!res.ok) { showToast(t('admin.errors.deleteFailed', { status: res.status })); return; }
  await refreshOrders();
};

```

- [ ] **Step 3: `OrdersTable.vue` — xóa nút Xóa trong template**

Dòng 657, xóa hẳn:
```html
                <button v-if="canDelete" class="btn btn-sm btn-outline-danger" style="font-size:0.78rem;padding:2px 8px;" @click="deleteOrder(o.donHangId)">{{ t('admin.orders.delete') }}</button>
```
(giữ nguyên 3 nút "Chi tiết"/next-step/"Cập nhật" phía trên nó trong cùng `<div class="d-flex gap-1">`)

- [ ] **Step 4: `StaffPage.vue` — bỏ prop `:can-delete="false"` không còn tồn tại**

Dòng 155, từ:
```html
        <section v-show="currentPage === 'orders'"><OrdersTable :can-delete="false" /></section>
```
thành:
```html
        <section v-show="currentPage === 'orders'"><OrdersTable /></section>
```

- [ ] **Step 5: i18n — xóa key `orders.delete` và `confirm.deleteOrder`**

`vi.js`: xóa `delete: "Xóa",` trong namespace `orders` (dòng ~770, cạnh `update: "Hủy/Sửa khác"`); xóa dòng `deleteOrder: "Bạn có chắc muốn xóa đơn hàng này? Hành động này không thể hoàn tác.",` trong khối `confirm`.
`en.js`: tương tự — `delete: "Delete",` trong `orders`, và dòng `deleteOrder: "Are you sure you want to delete this order? This action cannot be undone.",` trong `confirm`.

- [ ] **Step 6: Build frontend**

Run: `cd FrontEnd/QLBanMayTinh && npm run build`
Expected: BUILD SUCCESS. Xác nhận riêng: `grep -rn "DonHangService.remove" FrontEnd/QLBanMayTinh/src` vẫn phải còn đúng 2 kết quả (`CheckoutModal.vue`, `PosPanel.vue`) — nếu mất 1 trong 2, đã lỡ xóa nhầm.

- [ ] **Step 7: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/admin/OrdersTable.vue \
        FrontEnd/QLBanMayTinh/src/pages/StaffPage.vue \
        FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/en.js
git commit -m "refactor(frontend): bỏ nút Xóa đơn hàng ở admin, giữ nguyên backend

DonHangService.remove()/DonHangController vẫn giữ nguyên — còn dùng để
rollback đơn 'pending' khi checkout/POS lỗi giữa chừng (CheckoutModal.vue,
PosPanel.vue). Chỉ gỡ đường vào từ UI quản trị; thay thế bằng nút 'Cập
nhật' đổi trangThaiDonHang sang 'cancelled' đã có sẵn."
```

---

### Task 4: Xác nhận end-to-end qua Docker + trình duyệt

**Files:** không sửa file nào — chỉ verify.

- [ ] **Step 1: Restart backend container, xác nhận start sạch**

Run: `docker compose up -d --build backend`
Expected: log có dòng `Started BackEndApplication`, container ở trạng thái `Up` (không exit code 1).

- [ ] **Step 2: Gọi trực tiếp 3 endpoint đã xóa, xác nhận không còn tồn tại**

Run:
```bash
curl -s -o /dev/null -w "%{http_code}\n" -X DELETE http://localhost:8080/api/khach-hang/delete/1
curl -s -o /dev/null -w "%{http_code}\n" -X DELETE http://localhost:8080/api/nhan-vien/delete/1
curl -s -o /dev/null -w "%{http_code}\n" -X DELETE http://localhost:8080/api/khuyen-mai/delete/1
```
Expected: cả 3 trả `404` hoặc `403` (bị chặn bởi Spring Security do không có route khớp — KHÔNG được là `500`/`204`).

- [ ] **Step 3: Mở app thật (`localhost:5173`), đăng nhập admin, kiểm tra trực quan 4 bảng**

Với mỗi bảng Khách hàng / Nhân viên / Khuyến mãi / Đơn hàng trong trang Admin:
- Xác nhận nút "Xóa" không còn hiển thị.
- Mở form Sửa 1 bản ghi bất kỳ, đổi trạng thái (active ↔ inactive, hoặc với đơn hàng: đổi `trangThaiDonHang` sang `cancelled` qua nút "Cập nhật"), lưu, xác nhận badge trạng thái trên bảng cập nhật đúng.

- [ ] **Step 4: Không cần commit — task này chỉ verify, không sửa code.**

## Self-Review Note

- **Spec coverage:** Task 1+2 phủ Khách hàng/Nhân viên/Khuyến mãi (backend+frontend); Task 3 phủ đúng ngoại lệ Đơn hàng (frontend-only, backend giữ nguyên); Task 4 phủ mục "Kiểm tra" trong spec. Không có mục nào trong spec bị bỏ sót.
- **Placeholder scan:** không còn "TBD"/"tương tự Task N" — mọi step đều có code/lệnh cụ thể.
- **Type/signature consistency:** không phát sinh type mới, chỉ xóa — không có rủi ro lệch tên hàm giữa các task.
