# StaffPage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Xây trang riêng cho vai trò `nhan_vien` (nhân viên bán hàng), thay thế hoàn toàn AdminPage cho role này — theo đúng spec `docs/superpowers/specs/2026-07-18-staff-warehouse-pages-design.md` và các component/store dùng chung đã xây ở Plan 2.

**Architecture:** Plan 3/4 trong chuỗi 4 plan. `StaffPage.vue` là 1 page shell mới (sidebar+topbar, theo đúng layout AdminPage.vue) chỉ với 4 mục: Bán hàng, Đơn hàng, Khách hàng, Sản phẩm — ghép từ các component đã có sẵn ở `components/admin/` (không viết lại logic). 2 trong 4 component (`ProductsTable`, `OrdersTable`) cần thêm prop giới hạn quyền (chưa có ở Plan 2, phát hiện khi viết plan này) để đúng ý spec: Sản phẩm chỉ xem, Đơn hàng ẩn nút Xoá.

**Tech Stack:** Vue 3 `<script setup>`, tái sử dụng store/component từ Plan 1-2.

## Global Constraints

- Component dùng chung (`ProductsTable`, `OrdersTable`, `CustomersTable`, `PosPanel`, `UserProfileMenu`) — mọi prop mới thêm PHẢI có giá trị mặc định giữ nguyên hành vi hiện tại của AdminPage.vue (AdminPage.vue không cần sửa gì khi các prop này được thêm — default `readonly=false`/`canDelete=true`/`showSettingsLink=true`).
- `StaffPage.vue` theo đúng layout shell của AdminPage.vue (sidebar 240px + topbar + main), tái dùng nguyên `.adm-nav`/`.adm-icon`/`.adm-nav-label` CSS (copy vào `<style scoped>` riêng — CSS scoped không kế thừa qua biên component, đã rút kinh nghiệm từ Plan 2).
- Routing: `nhan_vien` → `#staff`. `admin`/`quan_kho` giữ nguyên hành vi hiện tại (`quan_kho` vẫn tạm vào `#admin` — WarehouseManagementPage chưa xây, đổi ở Plan 4).
- Không tạo i18n key mới — toàn bộ nhãn StaffPage tái dùng key `admin.sidebar.*`/`admin.pageMeta.*`/`admin.brand.*` đã có sẵn.

---

### Task 1: Thêm prop giới hạn quyền vào 4 component dùng chung

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/ProductDetailModal.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/ProductsTable.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/OrdersTable.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/UserProfileMenu.vue`

**Interfaces:**
- Produces: `ProductDetailModal` prop `readonly` (Boolean, default `false`) — ẩn nút Sửa/Thêm biến thể/Xóa biến thể khi `true`. `ProductsTable` prop `readonly` (Boolean, default `false`) — ẩn nút Thêm/Xoá, truyền `readonly` xuống `ProductDetailModal`. `OrdersTable` prop `canDelete` (Boolean, default `true`) — ẩn nút Xoá đơn khi `false`. `UserProfileMenu` prop `showSettingsLink` (Boolean, default `true`) — ẩn mục "Cài đặt" trong dropdown khi `false`. Task 2 (StaffPage.vue) dùng cả 4 prop này.

- [ ] **Step 1: `ProductDetailModal.vue` — thêm prop `readonly`**

Sửa `defineProps` (đầu file, sau các import):
```js
const props = defineProps({
  modelValue: { type: Boolean, default: false },
  sanPhamId: { type: [Number, String], default: null },
  sanPhamName: { type: String, default: "" },
  readonly: { type: Boolean, default: false },
});
```

Trong template, bọc nút "Thêm biến thể" (đoạn `<button ... @click="requestAddVariant">{{ t('admin.variantModal.addVariant') }}</button>`) bằng `v-if="!readonly"`:
```html
<button v-if="!readonly" class="btn btn-sm btn-warning text-dark fw-bold" style="font-size:0.78rem;" @click="requestAddVariant">{{ t('admin.variantModal.addVariant') }}</button>
```

Bọc cụm 2 nút "Sửa"/"Xóa" (đoạn `<div class="d-flex gap-2 flex-shrink-0">...</div>` chứa `requestEdit`/`requestDeleteVariant`) bằng `v-if="!readonly"`:
```html
<div v-if="!readonly" class="d-flex gap-2 flex-shrink-0">
  <button class="btn btn-sm btn-outline-warning" style="font-size:0.75rem;padding:3px 12px;" @click="requestEdit(v)">{{ t('admin.detailModal.edit') }}</button>
  <button class="btn btn-sm btn-outline-danger" style="font-size:0.75rem;padding:3px 12px;" @click="requestDeleteVariant(v.bienTheId)">{{ t('admin.products.delete') }}</button>
</div>
```

- [ ] **Step 2: `ProductsTable.vue` — thêm prop `readonly`**

Sửa đầu `<script setup>`, thêm ngay sau các import (trước `onMounted(() => { ensureProducts(); });`):
```js
const props = defineProps({ readonly: { type: Boolean, default: false } });
```

Bọc nút "Thêm sản phẩm" (dòng có `@click="openAdd"`) bằng `v-if="!readonly"`:
```html
<button v-if="!readonly" class="btn btn-sm btn-warning text-dark fw-bold" @click="openAdd">{{ t('admin.products.add') }}</button>
```

Bọc nút "Xóa" trong bảng (dòng có `@click="deleteProduct(p.sanPhamId)"`) bằng `v-if="!readonly"`:
```html
<button v-if="!readonly" class="btn btn-sm btn-outline-danger"  style="font-size:0.78rem; padding:2px 8px;" @click="deleteProduct(p.sanPhamId)">{{ t('admin.products.delete') }}</button>
```

Truyền `readonly` xuống `ProductDetailModal` (đoạn gọi component này ngay dưới bảng):
```html
<ProductDetailModal
  v-model="showDetailModal"
  :san-pham-id="detailModalSanPhamId"
  :san-pham-name="detailModalSanPhamName"
  :readonly="readonly"
  @edit-requested="onDetailEditRequested"
/>
```

- [ ] **Step 3: `OrdersTable.vue` — thêm prop `canDelete`**

Sửa đầu `<script setup>`, thêm ngay sau các import (trước `onMounted(() => { ensureOrders(); ensureCustomers(); ensureProducts(); });`):
```js
const props = defineProps({ canDelete: { type: Boolean, default: true } });
```

Bọc nút "Xóa" đơn hàng (dòng có `@click="deleteOrder(o.donHangId)"`) bằng `v-if="canDelete"`:
```html
<button v-if="canDelete" class="btn btn-sm btn-outline-danger"  style="font-size:0.78rem;padding:2px 8px;" @click="deleteOrder(o.donHangId)">{{ t('admin.orders.delete') }}</button>
```

- [ ] **Step 4: `UserProfileMenu.vue` — thêm prop `showSettingsLink`**

Sửa đầu `<script setup>` (sau `const emit = defineEmits(["navigate-settings"]);`):
```js
const props = defineProps({ showSettingsLink: { type: Boolean, default: true } });
```

Bọc nút "Cài đặt" trong dropdown (đoạn `<button ... @click="goToSettingsFromMenu">{{ t('admin.sidebar.settings') }}</button>`) bằng `v-if="showSettingsLink"`:
```html
<button v-if="showSettingsLink" class="btn btn-sm w-100 text-start rounded-0 border-0" style="color:var(--text-primary);" @click="goToSettingsFromMenu">
  {{ t('admin.sidebar.settings') }}
</button>
```

- [ ] **Step 5: Build kiểm tra — xác nhận AdminPage.vue không đổi hành vi**

Run:
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run build
```
Expected: `✓ built in ...`, không lỗi. AdminPage.vue hiện gọi `<ProductsTable />`/`<OrdersTable />`/`<UserProfileMenu @navigate-settings="navigate('settings')" />` KHÔNG truyền prop mới nào — 4 prop mới đều nhận giá trị mặc định (`readonly=false`, `canDelete=true`, `showSettingsLink=true`), giữ nguyên hành vi admin đầy đủ.

- [ ] **Step 6: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/admin/ProductDetailModal.vue \
  FrontEnd/QLBanMayTinh/src/components/admin/ProductsTable.vue \
  FrontEnd/QLBanMayTinh/src/components/admin/OrdersTable.vue \
  FrontEnd/QLBanMayTinh/src/components/admin/UserProfileMenu.vue
git commit -m "feat: add role-scoping props (readonly/canDelete/showSettingsLink) to shared admin components for StaffPage"
```

---

### Task 2: Tạo `StaffPage.vue`

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/pages/StaffPage.vue`

**Interfaces:**
- Consumes: `ProductsTable` (`:readonly="true"`), `OrdersTable` (`:can-delete="false"`), `CustomersTable` (đầy đủ quyền, không giới hạn), `PosPanel` (đầy đủ), `UserProfileMenu` (`:show-settings-link="false"`), `ConfirmDialog`, `ToastHost` — tất cả từ Plan 1-2, Task 1 (Plan 3) vừa thêm 4 prop.
- Produces: page component `StaffPage.vue` — Task 3 (App.vue) import và render khi `nhan_vien` đăng nhập.

- [ ] **Step 1: Viết `StaffPage.vue`**

`FrontEnd/QLBanMayTinh/src/pages/StaffPage.vue`:
```vue
<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { AuthStore } from "../stores/index.js";
import { t } from "../i18n/index.js";
import { ThemeStore, toggleTheme } from "../stores/theme.js";
import ConfirmDialog from "../components/common/ConfirmDialog.vue";
import ToastHost from "../components/common/ToastHost.vue";
import UserProfileMenu from "../components/admin/UserProfileMenu.vue";
import PosPanel from "../components/admin/PosPanel.vue";
import OrdersTable from "../components/admin/OrdersTable.vue";
import CustomersTable from "../components/admin/CustomersTable.vue";
import ProductsTable from "../components/admin/ProductsTable.vue";
import { OrdersStore, connectOrderEvents, disconnectOrderEvents } from "../stores/orders.js";
import { CustomersStore, ensureCustomers } from "../stores/customers.js";

// ── Navigation — mac dinh vao thang Ban hang (viec chinh hang ngay cua nhan vien) ──
const currentPage = ref("ban-hang");
const navigate = (page) => { currentPage.value = page; };

const PAGE_META = {
  "ban-hang": { titleKey: "admin.pageMeta.banHang.title", subKey: "admin.pageMeta.banHang.sub", icon: "🛒" },
  orders: { titleKey: "admin.pageMeta.orders.title", subKey: "admin.pageMeta.orders.sub", icon: "🧾" },
  customers: { titleKey: "admin.pageMeta.customers.title", subKey: "admin.pageMeta.customers.sub", icon: "👥" },
  products: { titleKey: "admin.pageMeta.products.title", subKey: "admin.pageMeta.products.sub", icon: "💻" },
};
const topbarTitle = computed(() => t(PAGE_META[currentPage.value]?.titleKey ?? "admin.pageMeta.banHang.title"));
const topbarSub = computed(() => t(PAGE_META[currentPage.value]?.subKey ?? ""));
const topbarIcon = computed(() => PAGE_META[currentPage.value]?.icon ?? "🛒");

// ── Badge sidebar: don hang hom nay, tong khach hang — doc thang tu store, khong
// can qua AdminPage.vue vi trang nay doc lap hoan toan ──
const toDateInputValue = (d) => {
  const y = d.getFullYear(), m = String(d.getMonth() + 1).padStart(2, '0'), day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
};
const todayOrdersCount = computed(
  () => OrdersStore.items.filter((o) => o.ngayDat?.slice(0, 10) === toDateInputValue(new Date())).length,
);
const totalCustomers = computed(() => CustomersStore.items.length);

onMounted(() => {
  ensureCustomers();
  connectOrderEvents(AuthStore.user?.token);
});
onUnmounted(() => {
  disconnectOrderEvents();
});
</script>

<template>
  <!-- Layout chinh: sidebar ben trai + main content ben phai — dong bo AdminPage.vue -->
  <div class="d-flex overflow-hidden" style="height:100vh; background:var(--bg-page-alt); color:var(--text-primary); font-family:'Nunito Sans',sans-serif;">

    <!-- ══════════ SIDEBAR ══════════ -->
    <aside class="d-flex flex-column border-end flex-shrink-0"
           style="width:240px; background:var(--bg-card-inset); border-color:var(--border-color)!important; overflow-y:auto;">

      <!-- Logo -->
      <div class="d-flex align-items-center gap-2 p-3 border-bottom"
           style="border-color:var(--border-color-soft)!important;">
        <div class="rounded-circle d-flex align-items-center justify-content-center fw-black flex-shrink-0"
             style="width:38px;height:38px;background:var(--accent);color:var(--accent-text);font-size:0.8rem;">SAO</div>
        <div>
          <div class="fw-bold" style="font-size:0.95rem;">{{ t('admin.brand.name') }}</div>
          <div style="font-size:0.7rem;color:var(--text-muted);">{{ t('admin.brand.tagline') }}</div>
        </div>
      </div>

      <!-- Nav staff -->
      <nav class="flex-grow-1 d-flex flex-column px-2 pb-2">
        <div class="adm-nav" :class="{active: currentPage==='ban-hang'}" @click="navigate('ban-hang')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M3 1a1 1 0 000 2h1.22l.305 1.222a.997.997 0 00.01.042l1.358 5.43-.893.892C4.328 11.142 4 11.574 4 12a2 2 0 002 2h10a1 1 0 000-2H6.414l1-1H14a1 1 0 00.894-.553l3-6A1 1 0 0017 4H6.28l-.31-1.243A1 1 0 005 2H3z"/><path d="M16 16.5a1.5 1.5 0 11-3 0 1.5 1.5 0 013 0zM6.5 18a1.5 1.5 0 100-3 1.5 1.5 0 000 3z"/></svg>
          {{ t('admin.sidebar.banHang') }}
        </div>
        <div class="adm-nav" :class="{active: currentPage==='orders'}" @click="navigate('orders')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M9 2a1 1 0 000 2h2a1 1 0 100-2H9z"/><path fill-rule="evenodd" d="M4 5a2 2 0 012-2 3 3 0 003 3h2a3 3 0 003-3 2 2 0 012 2v11a2 2 0 01-2 2H6a2 2 0 01-2-2V5zm3 4a1 1 0 000 2h.01a1 1 0 100-2H7zm3 0a1 1 0 000 2h3a1 1 0 100-2h-3zm-3 4a1 1 0 100 2h.01a1 1 0 100-2H7zm3 0a1 1 0 100 2h3a1 1 0 100-2h-3z" clip-rule="evenodd"/></svg>
          {{ t('admin.sidebar.orders') }}
          <span class="badge bg-warning text-dark ms-auto" style="font-size:0.68rem;">{{ todayOrdersCount }}</span>
        </div>
        <div class="adm-nav" :class="{active: currentPage==='customers'}" @click="navigate('customers')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3zM6 8a2 2 0 11-4 0 2 2 0 014 0zM16 18v-3a5.972 5.972 0 00-.75-2.906A3.005 3.005 0 0119 15v3h-3zM4.75 12.094A5.973 5.973 0 004 15v3H1v-3a3 3 0 013.75-2.906z"/></svg>
          {{ t('admin.sidebar.customers') }}
          <span class="badge bg-warning text-dark ms-auto" style="font-size:0.68rem;">{{ totalCustomers }}</span>
        </div>
        <div class="adm-nav" :class="{active: currentPage==='products'}" @click="navigate('products')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M5 3a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2V5a2 2 0 00-2-2H5zm0 8a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2v-2a2 2 0 00-2-2H5zm6-6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V5zm0 8a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"/></svg>
          {{ t('admin.sidebar.products') }}
        </div>
      </nav>

      <UserProfileMenu :show-settings-link="false" />
    </aside><!-- /sidebar -->

    <!-- ══════════ MAIN CONTENT ══════════ -->
    <main class="flex-grow-1 d-flex flex-column overflow-hidden">

      <!-- Topbar -->
      <div class="d-flex align-items-center justify-content-between p-3 border-bottom"
           style="background:var(--bg-card-inset); border-color:var(--border-color)!important;">
        <div>
          <div class="fw-bold" style="font-size:1.05rem;">{{ topbarIcon }} {{ topbarTitle }}</div>
          <div style="font-size:0.78rem;color:var(--text-muted);">{{ topbarSub }}</div>
        </div>
        <button type="button" class="d-flex align-items-center justify-content-center rounded-2 border-0"
                style="width:34px;height:34px;background:var(--bg-hover);color:var(--text-primary);cursor:pointer;font-size:1rem;"
                :title="ThemeStore.mode === 'dark' ? t('theme.toggleToLight') : t('theme.toggleToDark')"
                :aria-label="ThemeStore.mode === 'dark' ? t('theme.toggleToLight') : t('theme.toggleToDark')"
                @click="toggleTheme">
          {{ ThemeStore.mode === 'dark' ? '🌙' : '☀️' }}
        </button>
      </div>

      <!-- Noi dung -->
      <div class="flex-grow-1 overflow-y-auto p-3">
        <section v-show="currentPage === 'ban-hang'"><PosPanel /></section>
        <section v-show="currentPage === 'orders'"><OrdersTable :can-delete="false" /></section>
        <section v-show="currentPage === 'customers'"><CustomersTable /></section>
        <section v-show="currentPage === 'products'"><ProductsTable :readonly="true" /></section>
      </div>
    </main>
  </div>

  <ConfirmDialog />
  <ToastHost />
</template>

<style scoped>
/* Nav item: dong bo AdminPage.vue (.adm-nav/.adm-icon/.adm-nav-label) — CSS scoped
   khong ke thua qua bien gioi component nen phai copy lai o day. */
.adm-nav {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 8px 10px;
  border-radius: 7px;
  cursor: pointer;
  font-size: 0.87rem;
  color: var(--text-primary);
  transition: background 0.12s, color 0.12s;
  user-select: none;
}
.adm-nav:hover { background: var(--bg-hover); color: var(--text-heading); }
.adm-nav.active { background: rgba(244,63,94,0.12); color: var(--accent-fg); }
.adm-nav.active .adm-icon { opacity: 1; }
.adm-icon { width: 17px; height: 17px; flex-shrink: 0; opacity: 0.75; }
</style>
```

- [ ] **Step 2: Build kiểm tra**

Run:
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run build
```
Expected: `✓ built in ...`, không lỗi.

- [ ] **Step 3: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/pages/StaffPage.vue
git commit -m "feat: add StaffPage shell (Ban hang/Don hang/Khach hang/San pham) composed from shared admin components"
```

---

### Task 3: Điều hướng theo role trong `App.vue`

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/App.vue`

**Interfaces:**
- Consumes: `StaffPage.vue` (Task 2).
- Sau task này: `admin` → `#admin` (AdminPage, siết đúng role `admin`, không còn dùng chung `auth.isAdmin` cho riêng route này), `nhan_vien` → `#staff` (StaffPage), `quan_kho` → tạm thời vẫn `#admin` (không đổi — WarehouseManagementPage chưa xây, đổi ở Plan 4).

**⚠️ Phát hiện khi viết plan:** `<AdminPage v-if="isAdminHash && auth.isAdmin" />` hiện dùng `auth.isAdmin` — biến này đúng cho CẢ 3 role staff (xem `stores/index.js` `STAFF_ROLES`), không phải riêng `admin`. Nghĩa là hiện tại nhân viên gõ tay `#admin` trên URL vẫn vào được AdminPage đầy đủ, phá vỡ đúng mục đích tách trang theo role. Task này sửa luôn điều kiện route `#admin` thành đúng `auth.user?.role === 'admin'` — đây là hệ quả bắt buộc của yêu cầu "StaffPage thay thế hoàn toàn AdminPage cho nhan_vien" đã chốt khi brainstorming, không phải thay đổi phạm vi mới. Các chỗ khác dùng `auth.isAdmin` (vd NavBar ẩn giỏ hàng cho mọi role staff) KHÔNG đổi.

- [ ] **Step 1: Thêm computed `isStaffHash`**

Tìm dòng (gần đầu `<script setup>`, ngay sau khai báo `isAdminHash`):
```js
const isAdminHash = computed(() => currentHash.value === "#admin");
```
Thêm ngay sau:
```js
const isStaffHash = computed(() => currentHash.value === "#staff");
```

- [ ] **Step 2: Sửa `onLoginSuccess` — route `nhan_vien` sang `#staff`**

Thay:
```js
function onLoginSuccess(user) {
  setSession(user);
  loadCart(); // Khôi phục giỏ hàng đã lưu của tài khoản này (nếu có)
  const staffRoles = ["admin", "nhan_vien", "quan_kho"];
  if (staffRoles.includes(user.role)) {
    window.location.hash = "#admin";
  } else {
    window.location.hash = "";
  }
}
```
bằng:
```js
function onLoginSuccess(user) {
  setSession(user);
  loadCart(); // Khôi phục giỏ hàng đã lưu của tài khoản này (nếu có)
  // quan_kho tạm thời vẫn về #admin — WarehouseManagementPage chưa xây (Plan 4).
  const ROLE_HASH = { admin: "#admin", nhan_vien: "#staff", quan_kho: "#admin" };
  window.location.hash = ROLE_HASH[user.role] ?? "";
}
```

- [ ] **Step 3: Thêm import `StaffPage`**

Tìm dòng:
```js
import AdminPage from "./pages/AdminPage.vue";
```
Thêm ngay sau:
```js
import StaffPage from "./pages/StaffPage.vue";
```

- [ ] **Step 4: Sửa điều kiện route `#admin` — siết đúng role `admin`**

Thay:
```html
<AdminPage v-if="isAdminHash && auth.isAdmin" />
```
bằng:
```html
<AdminPage v-if="isAdminHash && auth.user?.role === 'admin'" />
```

- [ ] **Step 5: Thêm nhánh route `#staff`**

Ngay sau khối "Thông báo từ chối quyền truy cập" của `#admin` (kết thúc bằng `</section>` trước comment `TRANG TÀI KHOẢN KHÁCH HÀNG`), chèn thêm:
```html
    <!-- ══════════════════════════════════════════════════════
        TRANG NHÂN VIÊN — chỉ hiển thị khi URL có #staff VÀ đúng role nhan_vien
    ══════════════════════════════════════════════════════ -->
    <StaffPage v-else-if="isStaffHash && auth.user?.role === 'nhan_vien'" />

    <!-- Thông báo từ chối quyền truy cập (staff) -->
    <section
      v-else-if="isStaffHash && auth.user?.role !== 'nhan_vien'"
      class="d-flex align-items-center justify-content-center"
      style="min-height: 100vh; background: var(--bg-page)"
    >
      <div
        class="text-center d-flex flex-column align-items-center gap-3"
        style="color: var(--text-primary)"
      >
        <div style="font-size: 3rem">🔒</div>
        <h2 class="fw-black mb-0" style="font-size: 1.5rem">
          {{ t("adminAccess.title") }}
        </h2>
        <p class="mb-0" style="color: var(--text-secondary)">
          {{ t("adminAccess.desc") }}
        </p>
        <button
          class="btn btn-warning fw-bold rounded-pill px-4 py-2"
          @click="goHome"
        >
          {{ t("common.goHome") }}
        </button>
      </div>
    </section>
```
(Tái dùng nguyên `t("adminAccess.title")`/`t("adminAccess.desc")` đã có — thông báo "không có quyền" chung, không cần key riêng cho staff.)

- [ ] **Step 6: Build kiểm tra**

Run:
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run build
```
Expected: `✓ built in ...`, không lỗi.

- [ ] **Step 7: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/App.vue
git commit -m "feat: route nhan_vien role to StaffPage, tighten #admin route to admin role only"
```

---

### Task 4: Kiểm thử thủ công end-to-end

**Files:** không có file thay đổi — chỉ chạy và quan sát.

- [ ] **Step 1: Chạy backend + frontend**

```powershell
cd "d:\project code\SAOClub\BackEnd"; $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'; .\mvnw.cmd spring-boot:run
```
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run dev
```

- [ ] **Step 2: Kịch bản chính**

1. Đăng nhập bằng tài khoản `nhan_vien` — tự động chuyển sang `#staff`, thấy đúng 4 mục sidebar (Bán hàng/Đơn hàng/Khách hàng/Sản phẩm), mặc định vào tab Bán hàng.
2. Tab Sản phẩm: xác nhận KHÔNG có nút "Thêm sản phẩm", bấm "Chi tiết" 1 sản phẩm → modal mở nhưng KHÔNG có nút "Sửa"/"Thêm biến thể"/"Xóa biến thể", bảng KHÔNG có nút "Xóa".
3. Tab Đơn hàng: xác nhận KHÔNG có nút "Xóa" trên mỗi dòng, các nút Xem chi tiết/Cập nhật trạng thái vẫn hoạt động bình thường.
4. Tab Khách hàng: thêm/sửa/xóa 1 khách hàng thử — đầy đủ quyền như admin.
5. Tab Bán hàng: chạy trọn 1 giao dịch (tìm sản phẩm → thêm giỏ → xác định khách hàng → đặt hàng) — xác nhận đơn xuất hiện ngay ở tab Đơn hàng.
6. Dropdown hồ sơ (chân sidebar): xác nhận CHỈ có 2 mục (Chỉnh sửa hồ sơ, Đổi mật khẩu) — KHÔNG có mục "Cài đặt".
7. Gõ tay `#admin` trên URL trong khi đang đăng nhập `nhan_vien` — xác nhận bị chặn (màn "không có quyền"), KHÔNG vào được AdminPage.
8. Đăng xuất, đăng nhập lại bằng tài khoản `admin` — xác nhận AdminPage vẫn hoạt động y hệt trước (đủ quyền Thêm/Sửa/Xóa ở Sản phẩm/Đơn hàng, có mục "Cài đặt" trong dropdown).
9. Đăng nhập bằng tài khoản `quan_kho` — xác nhận vẫn tạm vào `#admin` như hành vi hiện tại (chưa đổi, để dành Plan 4).

- [ ] **Step 3: Dừng server**

`Ctrl+C` ở cả 2 terminal.

---

## Tự rà soát (self-review)

**1. Phủ đủ spec:**
- StaffPage = Bán hàng + Đơn hàng + Khách hàng + Sản phẩm (chỉ xem) → Task 2. ✅
- Đơn hàng "không toàn quyền như admin" → Task 1 (`canDelete=false`). ✅
- Routing theo role, admin không đổi hành vi → Task 3, đồng thời phát hiện và vá lỗ hổng route-bypass qua URL. ✅
- Không tạo i18n key mới → xác nhận toàn bộ nhãn StaffPage tái dùng key có sẵn. ✅

**2. Không còn placeholder** — mọi file/đoạn code đều đầy đủ, không có TODO.

**3. Nhất quán:** tên prop mới (`readonly`, `canDelete`, `showSettingsLink`) dùng xuyên suốt Task 1 và Task 2 khớp nhau.

## Ngoài phạm vi

- WarehouseManagementPage, routing cho `quan_kho` → Plan 4.
- Không đổi bất kỳ hành vi nào của AdminPage.vue ngoài việc siết điều kiện route `#admin` (bắt buộc theo đúng yêu cầu tách trang).
- Không thêm tính năng "báo cáo cá nhân" cho nhân viên — đã loại khỏi scope khi chốt yêu cầu ở brainstorming.
