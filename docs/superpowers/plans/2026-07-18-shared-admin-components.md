# Shared Admin Components Extraction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tách các phần dùng chung của `AdminPage.vue` (~5300 dòng, monolithic) thành component + store độc lập, để `StaffPage.vue`/`WarehouseManagementPage.vue` (Plan 3, 4) tái sử dụng được mà không phải copy-paste hay phụ thuộc ngầm vào AdminPage.

**Architecture:** Plan 2/4 trong chuỗi 4 plan (xem spec `docs/superpowers/specs/2026-07-18-staff-warehouse-pages-design.md`). Đây là refactor thuần túy — **không thêm tính năng mới, không đổi hành vi UI nào của AdminPage.vue**. Khảo sát sâu (Explore agent) phát hiện các phần dự kiến tách trong spec **ràng buộc chặt hơn** giả định ban đầu — đã chốt lại hướng xử lý với người dùng qua AskUserQuestion trước khi viết plan này:
1. Dữ liệu dùng chung (products/orders/customers/inventory/suppliers/staff/promotions) → **shared store** (`reactive()` + fetch-once-cache, theo đúng pattern `SettingsStore`/`loadSettings()` đã có), không để mỗi component tự fetch riêng (tránh fetch trùng + mất đồng bộ SSE).
2. Modal "Chi tiết sản phẩm" (Products+Orders dùng chung) và luồng "thêm khách hàng nhanh từ POS" (Customers+POS dùng chung) → tách thành **component riêng độc lập** (`ProductDetailModal.vue`, `CustomerFormModal.vue`), không giữ trong 1 trong 2 component gốc.
3. `TonKhoPanel`/`PhieuNhapKhoPanel` (2 mục trong spec ban đầu) → **gộp thành 1 `InventoryPanel.vue`** với tab nội bộ, đúng với UI thực tế hiện có (2 tab của cùng 1 khối "Kho hàng"), không tách theo spec ban đầu.
4. `showToast`/`formatPrice`/`formatDate`/`formatDateTime`/`statusLabel`/`toLocalDT` hiện là hàm cục bộ trong AdminPage.vue (không phải import global thật) → promote lên module dùng chung trước khi tách bất kỳ component nào phụ thuộc chúng.

**Tech Stack:** Vue 3 `<script setup>`, không Pinia — `reactive()` store thủ công theo pattern `stores/settings.js`/`stores/confirm.js` đã có sẵn.

## Global Constraints

- **KHÔNG đổi hành vi UI nào của AdminPage.vue.** Đây là refactor thuần túy — mọi task phải giữ AdminPage.vue hoạt động y hệt trước/sau (trừ 1 ngoại lệ đã xác nhận: xóa dead code `showVariantModal`/`openVariants` ở Task 3 — xem ghi chú trong task đó).
- Style store mới: theo đúng `stores/settings.js` — `reactive()` object + hàm `ensureXxx()`/`loadXxx()` cached-promise (đã có tiền lệ `ensureStaffData`/`ensureProductRefData` ngay trong AdminPage.vue hiện tại, dùng closure `let xxxPromise = null` để cache).
- Style modal component mới: theo đúng pattern overlay tự viết của AdminPage.vue — `<div v-if="show" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:...;" @click.self="...">`. KHÔNG dùng `Modal.vue` (đúng convention đã ghi trong spec `trang-cai-dat`/`menu-ho-so-admin` trước đó).
- Mọi "move verbatim" trong các task dưới đây trích dẫn số dòng chính xác **theo commit `51999e5`** (HEAD tại thời điểm viết plan này) — nếu implementer thấy số dòng lệch do thay đổi trung gian, dùng nội dung/tên biến làm mốc xác định thay vì số dòng cứng.
- Component mới đặt tại `src/components/admin/`. Store mới đặt tại `src/stores/`.
- Sau MỖI task, chạy `npm run build` — phải thành công, không lỗi — trước khi coi task hoàn tất (đây là cách kiểm tra chính cho 1 refactor thuần túy không có test tự động).

---

### Task 1: Promote helper functions dùng chung + toast store

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/utils/adminFormat.js`
- Create: `FrontEnd/QLBanMayTinh/src/stores/toast.js`
- Create: `FrontEnd/QLBanMayTinh/src/components/common/ToastHost.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue`

**Interfaces:**
- Produces: `formatPrice(v)`, `formatDate(d)`, `formatDateTime(d)`, `statusLabel(s)`, `toLocalDT(s)` (từ `utils/adminFormat.js`); `showToast(msg, type)` + `ToastState` (từ `stores/toast.js`); `<ToastHost />` component — tất cả các task sau (3-8) dùng lại thay vì định nghĩa cục bộ.

- [ ] **Step 1: Tạo `utils/adminFormat.js`**

`FrontEnd/QLBanMayTinh/src/utils/adminFormat.js`:
```js
import { t } from "../i18n/index.js";
import { formatPrice as formatPriceRaw } from "./formatPrice.js";

// ── Helper định dạng dùng chung cho các trang staff (Admin/Staff/WarehouseManagement) ──
// Trước đây là hàm cục bộ trong AdminPage.vue — promote lên đây để các trang khác
// (StaffPage, WarehouseManagementPage) dùng lại được, không phải copy-paste.

export const statusLabel = (s) => t(`admin.statusLabel.${s}`);

export const formatPrice = (v) => (v == null ? "—" : formatPriceRaw(v));

export const formatDate = (d) => {
  if (!d) return "—";
  try {
    return new Date(d).toLocaleDateString("vi-VN");
  } catch {
    return d;
  }
};

// Ngày + giờ (khác formatDate — chỉ có ngày) — dùng cho ngày giao dự kiến/thực tế,
// vì admin cần biết cả mốc giờ, không chỉ ngày.
export const formatDateTime = (d) => {
  if (!d) return "—";
  try {
    return new Date(d).toLocaleString("vi-VN");
  } catch {
    return d;
  }
};

export const toLocalDT = (s) =>
  s ? (s.length === 16 ? s + ":00" : s.slice(0, 19)) : null;
```

- [ ] **Step 2: Tạo `stores/toast.js`**

`FrontEnd/QLBanMayTinh/src/stores/toast.js`:
```js
import { reactive } from "vue";

// ── Toast Store — thay window.alert() trên các trang staff (Admin/Staff/WarehouseManagement) ──
// Trước đây là state+hàm cục bộ trong AdminPage.vue — promote lên đây theo đúng pattern
// stores/confirm.js đã có, để dùng lại được ở StaffPage/WarehouseManagementPage.
export const ToastState = reactive({ show: false, msg: "", type: "success" });

let toastTimer = null;

export const showToast = (msg, type = "error") => {
  clearTimeout(toastTimer);
  ToastState.msg = msg;
  ToastState.type = type;
  ToastState.show = true;
  // Lỗi (đặc biệt lý do chặn xóa) thường dài hơn — cho thêm thời gian đọc so với thông báo
  // thành công ngắn gọn.
  toastTimer = setTimeout(() => { ToastState.show = false; }, type === "error" ? 6000 : 3500);
};
```

- [ ] **Step 3: Tạo `components/common/ToastHost.vue`**

`FrontEnd/QLBanMayTinh/src/components/common/ToastHost.vue`:
```vue
<template>
  <!-- Toast thông báo lỗi/thành công (thay window.alert()) — dùng chung Admin/Staff/WarehouseManagement -->
  <Transition name="adm-toast-slide">
    <div v-if="ToastState.show"
         class="position-fixed d-flex align-items-start gap-2 px-4 py-3 rounded-3 fw-semibold small shadow-lg"
         style="top:24px; right:24px; z-index:9999; min-width:260px; max-width:440px; pointer-events:none; line-height:1.4;"
         :style="ToastState.type === 'success'
           ? 'background:var(--state-success,#16a34a); color:#fff;'
           : 'background:var(--state-danger,#dc2626); color:#fff;'"
         role="status" aria-live="polite">
      <span style="font-size:1.1rem; flex-shrink:0;">{{ ToastState.type === 'success' ? '✓' : '✕' }}</span>
      <span>{{ ToastState.msg }}</span>
    </div>
  </Transition>
</template>

<script setup>
import { ToastState } from '../../stores/toast.js';
</script>

<style scoped>
.adm-toast-slide-enter-active, .adm-toast-slide-leave-active { transition: transform 0.3s ease, opacity 0.25s ease; }
.adm-toast-slide-enter-from, .adm-toast-slide-leave-to       { transform: translateX(110%); opacity: 0; }
</style>
```

- [ ] **Step 4: Sửa `AdminPage.vue` — dùng lại thay vì định nghĩa cục bộ**

Xóa định nghĩa cục bộ (dòng 37-48, khối "Toast thông báo"):
```js
// ── Toast thông báo (thay window.alert()) ──────────────────────────────────
const toast = reactive({ show: false, msg: '', type: 'success' });
let toastTimer = null;
const showToast = (msg, type = 'error') => {
  clearTimeout(toastTimer);
  toast.msg  = msg;
  toast.type = type;
  toast.show = true;
  // Lỗi (đặc biệt lý do chặn xóa) thường dài hơn — cho thêm thời gian đọc so với thông báo
  // thành công ngắn gọn.
  toastTimer = setTimeout(() => { toast.show = false; }, type === 'error' ? 6000 : 3500);
};
```

Xóa định nghĩa cục bộ (dòng 202, 204, 206-227 — `statusLabel`, `formatPrice`, `formatDate`, `formatDateTime`, `toLocalDT`; **giữ nguyên `customerName`/`chucVuName`**, 2 hàm này KHÔNG thuộc phạm vi Task 1):
```js
// ── Helpers ───────────────────────────────────────────────────────────────────
const statusLabel = (s) => t(`admin.statusLabel.${s}`);

const formatPrice = (v) => (v == null ? "—" : formatPriceRaw(v));

const formatDate = (d) => {
  if (!d) return "—";
  try {
    return new Date(d).toLocaleDateString("vi-VN");
  } catch {
    return d;
  }
};

// Ngày + giờ (khác formatDate — chỉ có ngày) — dùng cho ngày giao dự kiến/thực tế,
// vì admin cần biết cả mốc giờ, không chỉ ngày.
const formatDateTime = (d) => {
  if (!d) return "—";
  try {
    return new Date(d).toLocaleString("vi-VN");
  } catch {
    return d;
  }
};

const toLocalDT = (s) =>
  s ? (s.length === 16 ? s + ":00" : s.slice(0, 19)) : null;
```

Thêm import mới sau dòng `import { authHeaders } from "../Service/api.js";` (dòng 35):
```js
import { formatPrice, formatDate, formatDateTime, statusLabel, toLocalDT } from "../utils/adminFormat.js";
import { showToast } from "../stores/toast.js";
import ToastHost from "../components/common/ToastHost.vue";
```

Lưu ý: import `formatPriceRaw` (dòng 7, `import { formatPrice as formatPriceRaw } from "../utils/formatPrice.js";`) không còn được dùng trực tiếp trong AdminPage.vue nữa sau khi xóa `formatPrice` cục bộ — xóa luôn dòng import này (kiểm tra grep `formatPriceRaw` trong file trước khi xóa để chắc chắn không còn chỗ nào khác dùng).

Thay khối `<ConfirmDialog />` + toast markup (dòng 5263-5281) thành:
```html
  <!-- Dialog xác nhận + toast dùng chung toàn trang — PHẢI nằm ngoài mọi v-if của modal cụ
       thể, nếu không component sẽ không tồn tại trong DOM khi modal đó đang đóng, khiến
       askConfirm()/showToast() gọi ra nhưng không có gì hiển thị (Promise của askConfirm
       treo mãi, code gọi nó bị kẹt không chạy tiếp). -->
  <ConfirmDialog />
  <ToastHost />
```
(Xóa toàn bộ khối `<Transition name="adm-toast-slide">...</Transition>` cũ ngay dưới, đã chuyển vào `ToastHost.vue`.)

Xóa 2 dòng CSS đã chuyển đi khỏi `<style scoped>` cuối file (dòng 5285-5286):
```css
.adm-toast-slide-enter-active, .adm-toast-slide-leave-active { transition: transform 0.3s ease, opacity 0.25s ease; }
.adm-toast-slide-enter-from, .adm-toast-slide-leave-to       { transform: translateX(110%); opacity: 0; }
```

- [ ] **Step 5: Build kiểm tra**

Run:
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run build
```
Expected: `✓ built in ...`, không lỗi. Đặc biệt kiểm tra không còn tham chiếu nào tới `toast`/`toastTimer` cục bộ cũ trong AdminPage.vue (build sẽ báo lỗi "not defined" nếu sót).

- [ ] **Step 6: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/utils/adminFormat.js \
  FrontEnd/QLBanMayTinh/src/stores/toast.js \
  FrontEnd/QLBanMayTinh/src/components/common/ToastHost.vue \
  FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue
git commit -m "refactor: promote toast + format helpers from AdminPage into shared modules"
```

---

### Task 2: Shared data store (products/orders/customers/inventory/suppliers/staff/promotions)

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/stores/products.js`
- Create: `FrontEnd/QLBanMayTinh/src/stores/orders.js`
- Create: `FrontEnd/QLBanMayTinh/src/stores/customers.js`
- Create: `FrontEnd/QLBanMayTinh/src/stores/inventory.js`
- Create: `FrontEnd/QLBanMayTinh/src/stores/suppliers.js`
- Create: `FrontEnd/QLBanMayTinh/src/stores/staff.js`
- Create: `FrontEnd/QLBanMayTinh/src/stores/promotions.js`
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue`

**Interfaces:**
- Produces: `ProductsStore`/`ensureProducts()`, `OrdersStore`/`ensureOrders()`/`connectOrderEvents()`/`disconnectOrderEvents()`, `CustomersStore`/`ensureCustomers()`, `InventoryStore`/`ensureInventory()`, `SuppliersStore`/`ensureSuppliers()`, `StaffStore`/`ensureStaff()`, `PromotionsStore`/`ensurePromotions()` — Task 3-8 import trực tiếp từ các store này thay vì nhận props từ AdminPage.
- **⚠️ Rủi ro cao nhất trong plan này** — đây là task duy nhất đổi cách AdminPage.vue tải dữ liệu (từ 1 `fetchAll()` cục bộ sang gọi các `ensureXxx()` của store). Đọc kỹ Step 8 trước khi sửa.

- [ ] **Step 1: Tạo `stores/products.js`**

```js
import { reactive } from "vue";
import * as SanPhamService from "../Service/SanPhamService.js";

// ── Products Store — dữ liệu sản phẩm dùng chung nhiều trang (ProductsTable, PosPanel,
// OrdersTable). fetch-once-cache: gọi ensureProducts() nhiều lần chỉ tải 1 lần, refresh()
// dùng khi cần tải lại có chủ đích (sau khi thêm/sửa/xóa sản phẩm). ──
export const ProductsStore = reactive({ items: [], loading: false, loaded: false });

let productsPromise = null;
export const ensureProducts = () => {
  if (productsPromise) return productsPromise;
  productsPromise = refreshProducts();
  return productsPromise;
};

export const refreshProducts = async () => {
  ProductsStore.loading = true;
  try {
    ProductsStore.items = await SanPhamService.getAll().catch(() => []);
    ProductsStore.loaded = true;
  } finally {
    ProductsStore.loading = false;
  }
  return ProductsStore.items;
};
```

- [ ] **Step 2: Tạo `stores/orders.js`**

```js
import { reactive } from "vue";
import * as DonHangService from "../Service/DonHangService.js";

// ── Orders Store — dữ liệu đơn hàng dùng chung (OrdersTable, PosPanel). Có SSE realtime:
// connectOrderEvents() mở 1 kết nối EventSource, tự patch OrdersStore.items khi có đơn mới/
// đơn cập nhật — mọi trang gọi connectOrderEvents() đều thấy cùng dữ liệu, không lệch nhau
// như khi mỗi trang tự fetch + tự mở SSE riêng. ──
export const OrdersStore = reactive({ items: [], loading: false, loaded: false });

let ordersPromise = null;
export const ensureOrders = () => {
  if (ordersPromise) return ordersPromise;
  ordersPromise = refreshOrders();
  return ordersPromise;
};

export const refreshOrders = async () => {
  OrdersStore.loading = true;
  try {
    OrdersStore.items = await DonHangService.getAll().catch(() => []);
    OrdersStore.loaded = true;
  } finally {
    OrdersStore.loading = false;
  }
  return OrdersStore.items;
};

let eventSource = null;
let subscriberCount = 0;

// Gọi trong onMounted của mỗi trang dùng đơn hàng realtime (Admin/Staff). Đếm số trang đang
// mở (subscriberCount) — chỉ mở/đóng EventSource thật khi trang cuối cùng unmount, để 2 trang
// mở cùng lúc (hiếm nhưng có thể, vd 2 tab) không tranh nhau mở 2 kết nối SSE trùng lặp.
export const connectOrderEvents = (token) => {
  subscriberCount += 1;
  if (eventSource) return;
  eventSource = new EventSource(`/api/don-hang/events?token=${encodeURIComponent(token ?? '')}`);
  eventSource.onerror = (e) => console.error('Kết nối SSE (đơn hàng real-time) lỗi:', e);
  eventSource.addEventListener('new-order', () => { refreshOrders(); });
  eventSource.addEventListener('order-updated', () => { refreshOrders(); });
};

export const disconnectOrderEvents = () => {
  subscriberCount = Math.max(0, subscriberCount - 1);
  if (subscriberCount === 0 && eventSource) {
    eventSource.close();
    eventSource = null;
  }
};
```

- [ ] **Step 3: Tạo `stores/customers.js`**

```js
import { reactive } from "vue";
import * as KhachHangService from "../Service/KhachHangService.js";

export const CustomersStore = reactive({ items: [], loading: false, loaded: false });

let customersPromise = null;
export const ensureCustomers = () => {
  if (customersPromise) return customersPromise;
  customersPromise = refreshCustomers();
  return customersPromise;
};

export const refreshCustomers = async () => {
  CustomersStore.loading = true;
  try {
    CustomersStore.items = await KhachHangService.getAll().catch(() => []);
    CustomersStore.loaded = true;
  } finally {
    CustomersStore.loading = false;
  }
  return CustomersStore.items;
};
```

- [ ] **Step 4: Tạo `stores/inventory.js`**

```js
import { reactive } from "vue";
import * as TonKhoService from "../Service/TonKhoService.js";

export const InventoryStore = reactive({ items: [], loading: false, loaded: false });

let inventoryPromise = null;
export const ensureInventory = () => {
  if (inventoryPromise) return inventoryPromise;
  inventoryPromise = refreshInventory();
  return inventoryPromise;
};

export const refreshInventory = async () => {
  InventoryStore.loading = true;
  try {
    InventoryStore.items = await TonKhoService.getAll().catch(() => []);
    InventoryStore.loaded = true;
  } finally {
    InventoryStore.loading = false;
  }
  return InventoryStore.items;
};
```

- [ ] **Step 5: Tạo `stores/suppliers.js`**

```js
import { reactive } from "vue";
import * as DmService from "../Service/DmService.js";

export const SuppliersStore = reactive({ items: [], loading: false, loaded: false });

let suppliersPromise = null;
export const ensureSuppliers = () => {
  if (suppliersPromise) return suppliersPromise;
  suppliersPromise = DmService.getNhaCungCap().catch(() => []).then((list) => {
    SuppliersStore.items = list;
    SuppliersStore.loaded = true;
  });
  return suppliersPromise;
};
```

- [ ] **Step 6: Tạo `stores/staff.js`**

```js
import { reactive } from "vue";
import * as NhanVienService from "../Service/NhanVienService.js";

export const StaffStore = reactive({ items: [], loading: false, loaded: false });

let staffPromise = null;
export const ensureStaff = () => {
  if (staffPromise) return staffPromise;
  staffPromise = NhanVienService.getAll().catch(() => []).then((list) => {
    StaffStore.items = list;
    StaffStore.loaded = true;
  });
  return staffPromise;
};
```

- [ ] **Step 7: Tạo `stores/promotions.js`**

```js
import { reactive } from "vue";
import * as KhuyenMaiService from "../Service/KhuyenMaiService.js";

export const PromotionsStore = reactive({ items: [], loading: false, loaded: false });

let promotionsPromise = null;
export const ensurePromotions = () => {
  if (promotionsPromise) return promotionsPromise;
  promotionsPromise = refreshPromotions();
  return promotionsPromise;
};

export const refreshPromotions = async () => {
  PromotionsStore.loading = true;
  try {
    PromotionsStore.items = await KhuyenMaiService.getAll().catch(() => []);
    PromotionsStore.loaded = true;
  } finally {
    PromotionsStore.loading = false;
  }
  return PromotionsStore.items;
};
```

- [ ] **Step 8: Sửa `AdminPage.vue` — dùng store thay vì `ref` + `fetchAll()` cục bộ cho 5 mảng đã lên store**

**Đây là bước rủi ro nhất — AdminPage.vue hiện có ~200+ chỗ đọc `products.value`/`orders.value`/`customers.value`/`inventory.value`/`promotions.value` (template lẫn script). KHÔNG đổi tên biến ở nơi dùng — thay vào đó, đặt các `computed` "alias" đọc từ store, để toàn bộ phần còn lại của AdminPage.vue (mọi chỗ chưa tách trong Task 3-8, vd Dashboard, Reports, Khuyến mãi, Nhân viên) không cần sửa gì cả.**

Xóa khai báo `ref` cũ (dòng 183-188):
```js
const products = ref([]);
const orders = ref([]);
const customers = ref([]);
const staff = ref([]);
const promotions = ref([]);
const inventory = ref([]);
```
(**Giữ nguyên** `phieuNhapList`, `chiTietPhieuNhapList` — 2 ref này chưa lên store, thuộc phạm vi Task 7.)

Thay bằng import store + computed alias, đặt ngay sau dòng import `ToastHost` vừa thêm ở Task 1:
```js
import { ProductsStore, ensureProducts, refreshProducts } from "../stores/products.js";
import { OrdersStore, ensureOrders, refreshOrders, connectOrderEvents, disconnectOrderEvents } from "../stores/orders.js";
import { CustomersStore, ensureCustomers, refreshCustomers } from "../stores/customers.js";
import { InventoryStore, ensureInventory, refreshInventory } from "../stores/inventory.js";
import { SuppliersStore, ensureSuppliers } from "../stores/suppliers.js";
import { StaffStore, ensureStaff } from "../stores/staff.js";
import { PromotionsStore, ensurePromotions, refreshPromotions } from "../stores/promotions.js";
```
Và, tại đúng vị trí đã xóa `const products = ref([])` ở trên, thêm computed alias (giữ đúng tên biến `products`/`orders`/`customers`/`staff`/`promotions`/`inventory` để phần còn lại của file không phải sửa):
```js
const products = computed(() => ProductsStore.items);
const orders = computed(() => OrdersStore.items);
const customers = computed(() => CustomersStore.items);
const staff = computed(() => StaffStore.items);
const promotions = computed(() => PromotionsStore.items);
const inventory = computed(() => InventoryStore.items);
```

**Lưu ý cho người triển khai:** `computed` là read-only — nếu build báo lỗi "Cannot assign to read-only property" ở chỗ nào đó trong AdminPage.vue đang gán trực tiếp `products.value = ...`/`orders.value.push(...)` v.v (thay vì gọi service rồi `refreshXxx()`), đó là chỗ cần sửa thành gọi `refreshProducts()`/`refreshOrders()` v.v sau khi service call thành công, KHÔNG gán tay vào biến — vì dữ liệu giờ thuộc sở hữu của store. Dùng grep `products.value =`, `orders.value =`, `customers.value =`, `inventory.value =`, `promotions.value =`, `staff.value =` trong AdminPage.vue để tìm hết các chỗ này trước khi build lần đầu.

`suppliers` (dòng 193, `const suppliers = ref([])`) đổi thành alias tương tự:
```js
const suppliers = computed(() => SuppliersStore.items);
```
(Xóa dòng `const suppliers = ref([]);` gốc.)

Sửa `fetchAll()` (dòng 1078-1096) — thay bằng gọi `refreshXxx()` của các store thay vì tự fetch:
```js
const fetchAll = async () => {
  await Promise.all([
    refreshProducts(),
    refreshOrders(),
    refreshCustomers(),
    refreshPromotions(),
    refreshInventory(),
  ]);
  await autoMergeAllDuplicates();
};
```
(Bỏ `loading.value = true/false` thủ công ở đây — mỗi `refreshXxx()` đã tự set `loading` riêng trong store của nó. Nếu AdminPage.vue có chỗ đọc `loading.value` dùng chung cho nhiều bảng — vd `v-if="loading"` ở Products/Orders/Customers/Inventory — đổi từng chỗ đó sang đúng cờ loading của store tương ứng, vd Products section dùng `ProductsStore.loading`, Orders section dùng `OrdersStore.loading`. Grep `loading.value` và `v-if="loading"`/`:disabled="loading"` trong AdminPage.vue để liệt kê hết các chỗ cần đổi.)

Xóa `const loading = ref(false);` (dòng 199) sau khi đã thay hết các chỗ dùng nó — nếu build vẫn còn báo lỗi "loading is not defined" ở đâu đó chưa liệt kê hết, quay lại rà thêm.

Sửa `ensureStaffData()` (dòng 1099-1105) — xóa hẳn, thay mọi chỗ gọi `ensureStaffData()` trong file bằng `ensureStaff()` (import từ `stores/staff.js`).

Sửa `ensureProductRefData()` (dòng 1110-1130) — bỏ dòng `DmService.getNhaCungCap().catch(() => [])` khỏi `Promise.all` và bỏ gán `suppliers.value = sup` (vì suppliers giờ có store riêng, `ensureProductRefData` không còn cần tải nó nữa), thay vào đó gọi thêm `ensureSuppliers()` song song:
```js
let productRefDataPromise = null;
const ensureProductRefData = () => {
  if (productRefDataPromise) return productRefDataPromise;
  productRefDataPromise = Promise.all([
    DanhMucService.getAll().catch(() => []),
    DmService.getThuongHieu().catch(() => []),
    DmService.getCpu().catch(() => []),
    DmService.getRam().catch(() => []),
    DmService.getOCung().catch(() => []),
    DmService.getGpu().catch(() => []),
    ensureSuppliers(),
  ]).then(([cat, br, cpu, ram, oc, gpu]) => {
    categories.value = cat;
    brands.value = br;
    cpuList.value = cpu;
    ramList.value = ram;
    oCungList.value = oc;
    gpuList.value = gpu;
  });
  return productRefDataPromise;
};
```

Sửa `onMounted`/`onUnmounted` (quanh dòng 2767-2796, khối SSE `orderSse`) — thay khối tự mở/đóng `EventSource` cục bộ bằng gọi store:
```js
onMounted(() => {
  connectOrderEvents(AuthStore.user?.token);
});
onUnmounted(() => {
  disconnectOrderEvents();
});
```
(Xóa biến `let orderSse = null;` và toàn bộ logic mở/đóng `EventSource` cũ tại đây — đã chuyển vào `stores/orders.js`. Nếu `onMounted`/`onUnmounted` hiện có nhiều việc khác ngoài SSE (vd `fetchAll()`, load settings...), CHỈ thay đúng phần liên quan SSE, giữ nguyên phần còn lại.)

- [ ] **Step 9: Build kiểm tra**

Run:
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run build
```
Expected: `✓ built in ...`, không lỗi "not defined" hay "Cannot assign to read-only property" nào còn sót.

- [ ] **Step 10: Kiểm thử thủ công tối thiểu (không có test tự động cho refactor thuần UI)**

Chạy `npm run dev` + backend, đăng nhập admin, vào từng tab đã đụng tới ở task này (Dashboard, Sản phẩm, Đơn hàng, Khách hàng, Kho hàng, Bán hàng, Khuyến mãi) — xác nhận dữ liệu hiển thị đúng như trước (không trắng trang, không lỗi console). Tạo 1 đơn hàng thử qua tab Bán hàng, xác nhận nó xuất hiện ngay trong tab Đơn hàng (xác nhận `refreshOrders()`/computed alias hoạt động đúng, không chỉ build pass).

- [ ] **Step 11: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/stores/products.js \
  FrontEnd/QLBanMayTinh/src/stores/orders.js \
  FrontEnd/QLBanMayTinh/src/stores/customers.js \
  FrontEnd/QLBanMayTinh/src/stores/inventory.js \
  FrontEnd/QLBanMayTinh/src/stores/suppliers.js \
  FrontEnd/QLBanMayTinh/src/stores/staff.js \
  FrontEnd/QLBanMayTinh/src/stores/promotions.js \
  FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue
git commit -m "refactor: move products/orders/customers/inventory/suppliers/staff/promotions data into shared stores"
```

---

### Task 3: `ProductDetailModal.vue` + `ProductsTable.vue`

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/components/admin/ProductDetailModal.vue`
- Create: `FrontEnd/QLBanMayTinh/src/components/admin/ProductsTable.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue`

**Interfaces:**
- `ProductDetailModal.vue` props: `modelValue` (Boolean, hiển thị modal), `sanPhamId`/`sanPhamName` (String/Number). Emits: `update:modelValue`, `edit-requested` (payload: sản phẩm cha, để cha mở `ProductsTable`'s edit form — modal này KHÔNG tự sửa được, chỉ xem + điều hướng sang sửa). Dùng chung bởi `ProductsTable.vue` (Task 3, sở hữu) và `OrdersTable.vue` (Task 5, tiêu thụ qua cùng props).
- `ProductsTable.vue`: không nhận props (tự đọc `ProductsStore` qua `ensureProducts()`), emit không có (tự quản lý modal của chính nó).
- **Xóa dead code:** `showVariantModal`/`openVariants`/`variantModalName`/`variantModalList`/`variantSerialMap`/`variantSerialLoad`/`serialInputs`/`addSerial` (dòng 1188-1233 theo khảo sát) — xác nhận không nơi nào trong template gọi `openVariants()`, modal "Biến thể sản phẩm" (dòng 4442-4471) không thể mở được từ bất kỳ đâu. Xóa hẳn khi tách, không mang sang `ProductsTable.vue`.

- [ ] **Step 1: Đọc đoạn cần chuyển trong AdminPage.vue (theo commit `51999e5`)**

Trước khi viết `ProductsTable.vue`, đọc các đoạn sau trong `AdminPage.vue` (dùng Read tool, không dùng số dòng cứng nếu file đã lệch — tìm theo tên biến/comment nêu dưới):
- Template bảng sản phẩm: đoạn bắt đầu `<section v-show="currentPage === 'products'">` (~dòng 3162), kết thúc ở `</section>` tương ứng (~dòng 3200).
- Template modal "Sản phẩm" (thêm/sửa): đoạn `v-if="showProductModal"` (~dòng 4236-4440).
- Template modal "Chi tiết sản phẩm": đoạn `v-if="showDetailModal"` (~dòng 4473-4564) — đây là phần chuyển sang `ProductDetailModal.vue` riêng, KHÔNG giữ trong `ProductsTable.vue`.
- Script: `productSearch`, `filteredGroupedProducts`, `showProductModal`, `editingId`, `formError`, `soSerialMoi`, `imagePreview`, `imageFilePending`, `PHAN_LOAI_TAG_OPTIONS`, `toggleTag`, `isTagSelected`, `emptyForm`, `form`, `addVariantMode`, `addVariantSanPhamId`, `addVariantSanPhamName`, `openAddVariant`, `openEdit`, `handleImageFile`, `saveProduct`, `resetImageState`, `deleteProduct`, `deleteVariant`, `openAdd`, `openDetail` (~dòng 302-1551, không liên tục — đọc toàn bộ vùng 1150-1560 để lấy đủ).
- Script (chuyển sang `ProductDetailModal.vue`): `showDetailModal`, `detailModalName`, `detailModalList`, `detailSerialMap`, `fetchSerialMap` (~dòng 1195-1217).

- [ ] **Step 2: Viết `ProductDetailModal.vue`**

Cấu trúc: `<script setup>` nhận props `modelValue`/`sanPhamId`/`sanPhamName`, tự fetch chi tiết biến thể qua `ChiTietSanPhamService`/`fetchSerialMap`-equivalent khi `sanPhamId` đổi (dùng `watch`), emit `update:modelValue` khi đóng (nút X hoặc click overlay), emit `edit-requested` khi bấm nút "Sửa"/"Thêm biến thể"/"Xóa biến thể" bên trong (cha — `ProductsTable.vue` — lắng nghe và tự gọi `openEdit`/`openAddVariant`/`deleteVariant` của chính nó). Markup lấy nguyên từ đoạn `v-if="showDetailModal"` đã đọc ở Step 1, đổi `showDetailModal` → `modelValue`, các state `detailModalName`/`detailModalList`/`detailSerialMap` thành state cục bộ của component này (không còn là AdminPage-level).

**Lưu ý quan trọng:** modal hiện tại gọi trực tiếp `openEdit(p)`/`openAddVariant(...)`/`deleteVariant(...)` (hàm của `ProductsTable`) ngay bên trong nó — vì tách ra component riêng, đổi các lời gọi này thành `emit('edit-requested', { action: 'edit', product: p })` / `{ action: 'addVariant', ... }` / `{ action: 'deleteVariant', ... }`, và tự đóng modal (`emit('update:modelValue', false)`) trước khi emit — cha xử lý hành động sau khi modal đã đóng.

- [ ] **Step 3: Viết `ProductsTable.vue`**

`<script setup>` import `ProductsStore`/`ensureProducts` từ `stores/products.js`, `ProductDetailModal` từ cùng thư mục, `formatPrice`/`statusLabel` từ `utils/adminFormat.js`, `showToast` từ `stores/toast.js`, `askConfirm` từ `stores/confirm.js`, `t` từ `i18n/index.js`, các Service (`SanPhamService`, `BienTheSanPhamService`, `ChiTietSanPhamService`), `authHeaders` từ `Service/api.js`. Gọi `ensureProducts()` trong `onMounted`. Toàn bộ state/hàm liệt kê ở Step 1 (trừ phần đã chuyển sang `ProductDetailModal`) chuyển nguyên vào đây — đổi `products.value`/`products` (đọc) thành `ProductsStore.items`, và sau `saveProduct`/`deleteProduct`/`deleteVariant` thành công, gọi `refreshProducts()` (từ `stores/products.js`) thay vì tự gán `products.value = [...]`.

Render `<ProductDetailModal v-model="showDetailModal" :san-pham-id="detailModalSanPhamId" :san-pham-name="detailModalSanPhamName" @edit-requested="onDetailEditRequested" />` — `onDetailEditRequested(payload)` switch theo `payload.action` gọi đúng `openEdit`/`openAddVariant`/`deleteVariant` cục bộ.

`ensureProductRefData()`/`categories`/`brands`/`suppliers`/`cpuList`/`ramList`/`oCungList`/`gpuList` (dùng bởi `openAdd`/`openEdit`/`openAddVariant`) — các ref này VẪN ở AdminPage.vue sau Task 2 (không lên store trong task này, chỉ `suppliers` có store riêng). `ProductsTable.vue` cần các ref/hàm này — **KHÔNG có sẵn** vì `ProductsTable.vue` không còn ở trong AdminPage.vue nữa. Xử lý: chuyển `ensureProductRefData`/`categories`/`brands`/`cpuList`/`ramList`/`oCungList`/`gpuList` (và hàm `ensureProductRefData` sửa ở Task 2 Step 8) vào LUÔN trong `ProductsTable.vue` (chúng chỉ được dùng bởi form thêm/sửa sản phẩm — đúng thuộc về đây), import `SuppliersStore`/`ensureSuppliers` cho phần NCC. Xóa các ref/hàm này khỏi AdminPage.vue khi hoàn tất task này (không còn ai dùng ở AdminPage.vue nữa sau khi ProductsTable là nơi duy nhất cần chúng — xác nhận bằng grep trước khi xóa: `categories`, `brands`, `cpuList`, `ramList`, `oCungList`, `gpuList` không còn xuất hiện ở đâu khác trong AdminPage.vue).

- [ ] **Step 4: Xóa dead code**

Không mang `showVariantModal`, `openVariants`, `variantModalName`, `variantModalList`, `variantSerialMap`, `variantSerialLoad`, `serialInputs`, `addSerial` sang `ProductsTable.vue` — xóa hẳn. Không mang modal "Biến thể sản phẩm" (template `v-if="showVariantModal"`) sang.

- [ ] **Step 5: Sửa `AdminPage.vue` — thay section cũ bằng component**

Xóa toàn bộ: template section `products` (~3162-3200), modal `showProductModal` (~4236-4440), modal `showDetailModal` (~4473-4564), modal `showVariantModal` (~4442-4471, dead code), và toàn bộ script liệt kê ở Step 1 + Step 3 (bao gồm `ensureProductRefData`/`categories`/`brands`/`cpuList`/`ramList`/`oCungList`/`gpuList` — đã chuyển hẳn vào `ProductsTable.vue`).

Thêm import:
```js
import ProductsTable from "../components/admin/ProductsTable.vue";
```

Thay vị trí section `products` cũ bằng:
```html
<section v-show="currentPage === 'products'">
  <ProductsTable />
</section>
```

**Lưu ý:** `groupedProducts`/`totalProducts` (Dashboard KPI, không thuộc phạm vi tách) vẫn cần đọc `ProductsStore.items` — giữ 2 computed này lại trong AdminPage.vue, chỉ đổi nguồn đọc từ `products.value` (alias đã có từ Task 2) sang vẫn dùng `products` (alias vẫn hoạt động, không đổi gì thêm ở đây).

- [ ] **Step 6: Build + kiểm thử thủ công**

```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run build
```
Sau đó `npm run dev`, vào tab Sản phẩm: thêm 1 sản phẩm thử, xem chi tiết, thêm biến thể, xóa biến thể, xóa sản phẩm — xác nhận mọi thao tác hoạt động y hệt trước khi tách. Dọn sản phẩm thử sau khi test.

- [ ] **Step 7: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/admin/ProductDetailModal.vue \
  FrontEnd/QLBanMayTinh/src/components/admin/ProductsTable.vue \
  FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue
git commit -m "refactor: extract ProductsTable + ProductDetailModal from AdminPage"
```

---

### Task 4: `CustomerFormModal.vue` + `CustomersTable.vue`

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/components/admin/CustomerFormModal.vue`
- Create: `FrontEnd/QLBanMayTinh/src/components/admin/CustomersTable.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue`

**Interfaces:**
- `CustomerFormModal.vue` props: `modelValue` (Boolean). Emits: `update:modelValue`, `saved` (payload: khách hàng vừa lưu — object đầy đủ từ response, để cha — `CustomersTable.vue` hoặc `PosPanel.vue` Task 6 — biết vừa tạo/sửa ai). Có 1 method `openForCreate(prefill)`/`openForEdit(customer)` expose ra ngoài qua `defineExpose` (cha gọi qua template ref) — vì PosPanel cần mở modal này với SĐT đã điền sẵn (từ `posConfirmCreateCustomer`), khác hẳn cách CustomersTable mở nó (nút "Thêm khách hàng" trống).
- `CustomersTable.vue`: không props, tự đọc `CustomersStore`.

- [ ] **Step 1: Đọc đoạn cần chuyển (theo commit `51999e5`, dùng tên biến để định vị nếu số dòng lệch)**

- Template bảng khách hàng: `<section v-show="currentPage === 'customers'">` (~3300-3332).
- Template modal khách hàng: `v-if="showCustomerModal"` (~4566-4592).
- Script: `customerSearch`, `filteredCustomers`, `showCustomerModal`, `editingCustomerId`, `customerFormError`, `emptyCustomerForm`, `customerForm`, `openAddCustomer`, `openEditCustomer`, `closeCustomerModal`, `saveCustomer`, `deleteCustomer` (~290-1640, không liên tục).

- [ ] **Step 2: Viết `CustomerFormModal.vue`**

Markup lấy từ modal `v-if="showCustomerModal"` đã đọc. State cục bộ: `editingCustomerId`, `customerFormError`, `customerForm` (dùng `emptyCustomerForm` làm giá trị khởi tạo). 2 hàm expose:
```js
const openForCreate = (prefill = {}) => {
  editingCustomerId.value = null;
  customerForm.value = { ...emptyCustomerForm(), ...prefill };
  customerFormError.value = '';
  emit('update:modelValue', true);
};
const openForEdit = (customer) => {
  editingCustomerId.value = customer.khachHangId;
  customerForm.value = { ...emptyCustomerForm(), ...customer };
  customerFormError.value = '';
  emit('update:modelValue', true);
};
defineExpose({ openForCreate, openForEdit });
```
`saveCustomer()` (từ `KhachHangService.save`) sau khi thành công: gọi `refreshCustomers()` (từ `stores/customers.js`), `emit('saved', savedCustomer)`, `emit('update:modelValue', false)`. `deleteCustomer` KHÔNG thuộc modal này — vẫn ở `CustomersTable.vue` (xóa trực tiếp từ bảng, không qua modal sửa).

- [ ] **Step 3: Viết `CustomersTable.vue`**

Import `CustomersStore`/`ensureCustomers`/`refreshCustomers`, `CustomerFormModal`, `formatPrice` (nếu bảng có hiển thị số tiền — kiểm tra lại đoạn template đã đọc), `showToast`, `askConfirm`, `t`, `KhachHangService`. Gọi `ensureCustomers()` trong `onMounted`. `customerSearch`/`filteredCustomers`/`deleteCustomer` chuyển nguyên vào đây, đọc `CustomersStore.items` thay vì `customers.value`.

Dùng template ref tới `CustomerFormModal`:
```html
<CustomerFormModal ref="customerModalRef" v-model="showCustomerModal" />
```
Nút "Thêm khách hàng" gọi `customerModalRef.value.openForCreate()`; nút "Sửa" trên mỗi dòng gọi `customerModalRef.value.openForEdit(c)`.

- [ ] **Step 4: Sửa `AdminPage.vue`**

Xóa template section `customers` (~3300-3332), modal `showCustomerModal` (~4566-4592), toàn bộ script liệt kê ở Step 1.

Thêm import: `import CustomersTable from "../components/admin/CustomersTable.vue";`

Thay section cũ bằng:
```html
<section v-show="currentPage === 'customers'">
  <CustomersTable />
</section>
```

**Lưu ý:** `totalCustomers` (Dashboard KPI + sidebar badge, không thuộc phạm vi tách) vẫn cần đọc `CustomersStore.items` qua alias `customers` đã có từ Task 2 — không cần sửa gì thêm.

**⚠️ Chưa xử lý trong task này — để lại cho Task 6 (PosPanel):** `posConfirmCreateCustomer()` trong AdminPage.vue hiện gọi `openAddCustomer()` (hàm cũ, giờ đã xóa) rồi gán `customerForm.value.soDienThoai = posPhone.value`. Việc này sẽ tạm thời BỊ HỎNG sau task này cho tới khi Task 6 sửa `posConfirmCreateCustomer()` để gọi `customerModalRef.value.openForCreate({ soDienThoai: posPhone.value })` qua 1 template ref mới trỏ tới `<CustomerFormModal>` mà AdminPage.vue giờ cần tự dựng riêng cho luồng POS (vì `CustomersTable.vue`'s modal instance không truy cập được từ ngoài component). Ghi rõ điều này vào báo cáo Task 4, KHÔNG tự ý sửa `posConfirmCreateCustomer` ở task này — thuộc phạm vi Task 6.

- [ ] **Step 5: Build + kiểm thử thủ công**

```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run build
```
`npm run dev`, vào tab Khách hàng: thêm 1 khách hàng thử, sửa, xóa — xác nhận hoạt động y hệt trước. **Bỏ qua** kiểm thử luồng "thêm khách hàng nhanh từ POS" ở task này (biết trước sẽ lỗi, sửa ở Task 6).

- [ ] **Step 6: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/admin/CustomerFormModal.vue \
  FrontEnd/QLBanMayTinh/src/components/admin/CustomersTable.vue \
  FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue
git commit -m "refactor: extract CustomersTable + CustomerFormModal from AdminPage (POS quick-add flow temporarily broken, fixed in Task 6)"
```

---

### Task 5: `OrdersTable.vue`

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/components/admin/OrdersTable.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue`

**Interfaces:**
- Consumes: `ProductDetailModal.vue` (Task 3), `OrdersStore`/`CustomersStore`/`ProductsStore` (Task 2).
- Không props, không emit — tự chứa toàn bộ luồng đơn hàng.

- [ ] **Step 1: Đọc đoạn cần chuyển (theo commit `51999e5`, dùng tên biến để định vị nếu lệch)**

- Template chính: `<section v-show="currentPage === 'orders'">` (~3203-3297).
- Modal "Chi tiết đơn hàng": `v-if="showOrderDetailModal"` (~4921-5097).
- Modal "Thêm sản phẩm chi tiết": `v-if="showAddItemDetailModal"` (~4793-4919).
- Modal "Trạng thái đơn hàng": `v-if="showOrderModal"` (~5099-5131).
- Modal "Chọn serial trước khi xác nhận": `v-if="showXacNhanSerialModal"` (~4114-4151).
- Script: toàn bộ danh sách hàm/state ở mục "Target 3" của báo cáo khảo sát (orderSearch/orderStatusFilter/orderPaymentFilter, ordersBaseList, filteredOrders, VN_WEEKDAYS, formatDateHeading, orderDatesGrouped, openOrderHistory/openHistoryDay/backToToday/backToDateList, deleteOrder, showOrderDetailModal+liên quan, openOrderDetail, productByBienThe, addItemMode+liên quan (thêm sản phẩm vào đơn), mergeLoading/mergeCandidates/autoMergeOrders, openVariantDetail, showOrderModal+liên quan (trạng thái đơn), NEXT_ORDER_STATUS+advanceOrderStatus, showXacNhanSerialModal+liên quan) — vùng dòng ~235-2199, không liên tục, đọc trực tiếp AdminPage.vue theo tên biến.

- [ ] **Step 2: Viết `OrdersTable.vue`**

Import: `OrdersStore`/`ensureOrders`/`refreshOrders` (`stores/orders.js`), `CustomersStore`/`ensureCustomers` (`stores/customers.js`, cho `customerName`), `ProductsStore`/`ensureProducts` (`stores/orders.js`, cho `productByBienThe`), `ProductDetailModal` (Task 3), `formatPrice`/`formatDate`/`formatDateTime` (`utils/adminFormat.js`), `orderStatusLabel`/`orderStatusColor`/`orderStatusIcon`/`paymentStatusLabel`/`paymentStatusColor`/`paymentStatusIcon` (`utils/orderStatus.js`, không đổi — đã global từ trước), `showToast` (`stores/toast.js`), `askConfirm` (`stores/confirm.js`), `t`, `nowLocalIso`, `authHeaders`, các Service (`DonHangService`, `ChiTietDonHangService`, `ChiTietDonHangSerialService`, `ChiTietSanPhamService`).

`customerName(id)` chuyển vào đây làm hàm cục bộ, đọc `CustomersStore.items` (không phải store riêng — Target 3 xác nhận chỉ OrdersTable dùng hàm này, không cần promote lên module chung).

Toàn bộ state/hàm ở Step 1 chuyển nguyên vào `<script setup>`, đổi `orders.value` → `OrdersStore.items`, `customers.value` (trong `customerName`) → `CustomersStore.items`, `products.value` (trong `productByBienThe`) → `ProductsStore.items`. Sau các hàm ghi (`deleteOrder`, `saveOrderStatus`, `advanceOrderStatus`, `confirmXacNhanSerial`, `autoMergeOrders`, `addItemToOrder`, `removeItemFromOrder`) thành công, gọi `refreshOrders()` thay vì tự gán `orders.value = [...]` hoặc mutate trực tiếp.

Gọi `ensureOrders()`, `ensureCustomers()`, `ensureProducts()` trong `onMounted` (an toàn khi gọi nhiều lần — cached-promise, không fetch trùng nếu AdminPage.vue/PosPanel đã gọi trước đó).

Render `<ProductDetailModal v-model="showDetailModal" :san-pham-id="..." :san-pham-name="..." @edit-requested="..." />` — nhưng khác với `ProductsTable.vue` (nơi `edit-requested` mở form sửa sản phẩm ngay), ở đây `openVariantDetail()` chỉ dùng modal để XEM (dòng "Sửa" bên trong modal, khi bấm ở OrdersTable, không có ý nghĩa nghiệp vụ rõ ràng — kiểm tra lại: nếu template gốc modal có nút "Sửa" luôn hiển thị bất kể ai mở nó, giữ nguyên hành vi cũ bằng cách OrdersTable's `@edit-requested` handler điều hướng người dùng sang tab Sản phẩm rồi mở form sửa — thực tế đơn giản nhất: emit ra ngoài không xử lý được vì OrdersTable không sở hữu `ProductsTable`; **quyết định:** OrdersTable's `onDetailEditRequested` chỉ hiển thị `showToast(t('admin.orders.editFromProductsTab'), 'info')` gợi ý người dùng qua tab Sản phẩm để sửa — thêm key i18n mới `admin.orders.editFromProductsTab` (5 file locale) nếu key tương đương chưa tồn tại, kiểm tra trước khi thêm).

- [ ] **Step 3: Sửa `AdminPage.vue`**

Xóa template section `orders` (~3203-3297) + 4 modal liệt kê ở Step 1, xóa toàn bộ script liệt kê ở Step 1.

Thêm import: `import OrdersTable from "../components/admin/OrdersTable.vue";`

Thay section cũ bằng:
```html
<section v-show="currentPage === 'orders'">
  <OrdersTable />
</section>
```

`todayOrdersCount` (sidebar badge, không thuộc phạm vi tách) vẫn đọc `orders` alias — không cần sửa.

- [ ] **Step 4: Build + kiểm thử thủ công**

```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run build
```
`npm run dev`, tab Đơn hàng: xem chi tiết 1 đơn, thêm sản phẩm vào đơn, cập nhật trạng thái, xem lịch sử theo ngày, xác nhận serial (nếu có đơn online đang chờ) — xác nhận mọi thao tác y hệt trước.

- [ ] **Step 5: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/admin/OrdersTable.vue \
  FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue
git commit -m "refactor: extract OrdersTable from AdminPage"
```

---

### Task 6: `PosPanel.vue` (+ hoàn tất luồng thêm khách hàng nhanh còn dang dở từ Task 4)

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/components/admin/PosPanel.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue`

**Interfaces:**
- Consumes: `CustomerFormModal.vue` (Task 4, tự dựng 1 instance riêng — KHÔNG dùng chung instance với `CustomersTable.vue`, vì 2 nơi mở modal độc lập nhau, không cùng lúc hiển thị).

- [ ] **Step 1: Đọc đoạn cần chuyển (theo commit `51999e5`, dùng tên biến để định vị nếu lệch)**

- Template chính: `<section v-show="currentPage === 'ban-hang'">` (~3968-4088).
- Modal "Chọn serial (POS)": `v-if="showSerialPicker"` (~4090-4112).
- Modal "Đơn đang giữ": `v-if="showHeldOrders"` (~4209-4230).
- Script: toàn bộ mục "Target 5" của báo cáo khảo sát (~2344-2628: posStage, posPhoneNotFound, posOpeningCustomerFromPos, posSearch, posCart, posPhone, posFoundCust, posError, posSuccess, posPromoCode, posAppliedPromo, posPromoMsg, posProducts, posCartTotal, posFee, posGiamGia, posGrandTotal, posApplyPromo, HELD_ORDERS_KEY+heldOrders+loadHeldOrders+saveHeldOrders, showHeldOrders, posHoldOrder/posResumeHeld/posDeleteHeld, showSerialPicker+liên quan, posOpenSerialPicker, setSerialTrangThai, posSelectSerial/posRemove/posReset, posStartInvoice/posLookup/posCancelCreateCustomer, posConfirmCreateCustomer, parsePosApiError/posPlaceOrder).

- [ ] **Step 2: Viết `PosPanel.vue`**

Import: `ProductsStore`/`ensureProducts` (`stores/products.js`), `CustomersStore`/`ensureCustomers` (`stores/customers.js`), `PromotionsStore`/`ensurePromotions` (`stores/promotions.js`), `OrdersStore`/`ensureOrders`/`refreshOrders` (`stores/orders.js`), `CustomerFormModal` (Task 4), `formatPrice` (`utils/adminFormat.js`), `t`, `nowLocalIso`, `DonHangService`, `ChiTietSanPhamService`.

Toàn bộ state/hàm ở Step 1 chuyển nguyên vào đây, đổi `products.value` → `ProductsStore.items`, `customers.value` → `CustomersStore.items`, `promotions.value` → `PromotionsStore.items`. Sau `posPlaceOrder()` thành công, gọi `refreshOrders()` (thay vì tự gán `orders.value`).

Gọi `ensureProducts()`, `ensureCustomers()`, `ensurePromotions()` trong `onMounted`.

**Hoàn tất luồng "thêm khách hàng nhanh" (khắc phục lỗ hổng để lại từ Task 4):** render `<CustomerFormModal ref="quickCustomerModalRef" v-model="showQuickCustomerModal" @saved="onQuickCustomerSaved" />` (state `showQuickCustomerModal` mới, cục bộ trong `PosPanel.vue`). Sửa `posConfirmCreateCustomer()`:
```js
const posConfirmCreateCustomer = () => {
  posPhoneNotFound.value = false;
  quickCustomerModalRef.value.openForCreate({ soDienThoai: posPhone.value });
};
const onQuickCustomerSaved = (customer) => {
  posFoundCust.value = customer;
  posStage.value = 'selling';
};
```
(So với bản gốc: gọi thẳng `openForCreate()` của modal riêng thay vì `openAddCustomer()` cũ đã xóa; gán `posFoundCust`/`posStage` qua sự kiện `@saved` của modal thay vì làm thủ công sau khi `saveCustomer()` chạy xong trong cùng file.)

- [ ] **Step 3: Sửa `AdminPage.vue`**

Xóa template section `ban-hang` (~3968-4088) + 2 modal liệt kê ở Step 1, xóa toàn bộ script liệt kê ở Step 1.

Thêm import: `import PosPanel from "../components/admin/PosPanel.vue";`

Thay section cũ bằng:
```html
<section v-show="currentPage === 'ban-hang'">
  <PosPanel />
</section>
```

- [ ] **Step 4: Build + kiểm thử thủ công — bắt buộc test lại luồng thêm khách hàng nhanh (đã bị hỏng tạm thời từ Task 4)**

```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run build
```
`npm run dev`, tab Bán hàng: bắt đầu hóa đơn, nhập SĐT chưa tồn tại → xác nhận modal thêm khách hàng mở ra với SĐT đã điền sẵn → lưu → xác nhận quay lại màn bán hàng với khách hàng vừa tạo đã được gán, giỏ hàng hoạt động bình thường, đặt hàng thành công, đơn xuất hiện ngay ở tab Đơn hàng (xác nhận `refreshOrders()` hoạt động). Test thêm: giữ đơn, khôi phục đơn đã giữ, xóa đơn đã giữ, chọn serial khi thêm vào giỏ.

- [ ] **Step 5: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/admin/PosPanel.vue \
  FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue
git commit -m "refactor: extract PosPanel from AdminPage, fix quick-add-customer flow via dedicated CustomerFormModal instance"
```

---

### Task 7: `InventoryPanel.vue` (gộp Tồn kho + Phiếu nhập kho, 2 tab nội bộ)

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/components/admin/InventoryPanel.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue`

**Interfaces:**
- Không props, không emit. Sở hữu `khoTab` (state nội bộ, không còn ở AdminPage.vue) — quyết định đã chốt với người dùng: gộp 2 tab thành 1 component thay vì tách riêng như spec ban đầu.
- **Không bao gồm tab "Bảo hành"** (`khoTab === 'bao-hanh'`) — ngoài phạm vi theo spec, giữ nguyên trong AdminPage.vue như 1 section độc lập mới (xem Step 3).

- [ ] **Step 1: Đọc đoạn cần chuyển (theo commit `51999e5`, dùng tên biến để định vị nếu lệch)**

- Template container: `<section v-show="currentPage === 'inventory'">` (~3335-3659) — bao gồm cả tab-switcher buttons (~3338-3343) VÀ 2 khối `<template v-if="khoTab==='ton-kho'">`/`<template v-else-if="khoTab==='phieu-nhap'">`. **Không chuyển** khối `khoTab==='bao-hanh'` (xem Step 3).
- Modal "Tồn kho": `v-if="showStockModal"` (~5133-5176).
- Modal "Chi tiết serial": `v-if="showStockDetailModal"` (~5178-5261).
- Modal "Tạo phiếu nhập": `v-if="showPhieuNhapModal"` (~4650-4714).
- Modal "Chi tiết phiếu nhập": `v-if="showPhieuNhapDetailModal"` (~4716-4791).
- Script Tồn kho (mục "Target 6" báo cáo khảo sát): inventorySearch, inventoryStatusFilter, expandedGroups, toggleGroup, allGroupsExpanded, toggleAllGroups, inventoryGrouped, getVariantInfo, stockClass, lowStockOnlyItems, totalStockQty, showStockModal+liên quan, openEditStock/addStockSerialRow/removeStockSerialRow, importSerialsFromFile, saveStock, showStockDetailModal+liên quan, openStockDetail/addStockSerial/removeStockSerial, stockDetailStatusLabel/stockDetailStatusColor (~648-2341, không liên tục).
- Script Phiếu nhập kho (mục "Target 7" báo cáo khảo sát): phieuNhapDataPromise/ensurePhieuNhapData, supplierName/staffName, phieuNhapStatusColor/phieuNhapStatusIcon, phieuNhapCounts, phieuNhapSearch/phieuNhapStatusFilter/filteredPhieuNhap, productOptionsForPhieuNhap/variantOptionsByProduct/variantsForProduct, supplierOptions/staffOptions, showPhieuNhapModal+liên quan, addPhieuNhapItemRow/removePhieuNhapItemRow, editingPhieuNhapId/openAddPhieuNhap/openEditPhieuNhap/savePhieuNhap/deletePhieuNhap/updatePhieuNhapStatus, showPhieuNhapDetailModal+liên quan/openPhieuNhapDetail, printEsc/printHtmlInIframe/printPhieuNhapList/printPhieuNhapDetail/exportPhieuNhapExcel (~731-1066), cộng `phieuNhapList`/`chiTietPhieuNhapList` refs (~189-190).
- `khoTab` ref (~663).

- [ ] **Step 2: Viết `InventoryPanel.vue`**

Import: `InventoryStore`/`ensureInventory`/`refreshInventory` (`stores/inventory.js`), `SuppliersStore`/`ensureSuppliers` (`stores/suppliers.js`), `StaffStore`/`ensureStaff` (`stores/staff.js`), `formatPrice`/`formatDate`/`statusLabel`/`toLocalDT` (`utils/adminFormat.js`), `showToast` (`stores/toast.js`), `askConfirm` (`stores/confirm.js`), `t`, `nowLocalIso`, `XLSX`, `SearchSelect` (`components/common/SearchSelect.vue`), `authHeaders`, các Service (`ChiTietSanPhamService`, `TonKhoService`, `PhieuNhapKhoService`, `ChiTietPhieuNhapService`, `DanhMucService`, `DmService`, `NhanVienService` — 2 cái cuối chỉ cần nếu `ensurePhieuNhapData` gọi trực tiếp thay vì qua `ensureSuppliers`/`ensureStaff`, kiểm tra lại code gốc và ưu tiên dùng store thay vì gọi Service trực tiếp trùng lặp).

`khoTab` ref khai báo cục bộ trong component này (mặc định `'ton-kho'`). Toàn bộ state/hàm ở Step 1 chuyển nguyên vào, đổi `inventory.value` → `InventoryStore.items`, `suppliers.value` → `SuppliersStore.items`, `staff.value` → `StaffStore.items`, `phieuNhapList`/`chiTietPhieuNhapList` giữ nguyên là `ref([])` cục bộ (chưa lên store — đúng phạm vi task này, không phải Task 2).

`ensurePhieuNhapData()` sửa lại gọi `ensureSuppliers()`/`ensureStaff()` (store) thay vì tự fetch, giữ nguyên phần tự fetch `phieuNhapList`/`chiTietPhieuNhapList` (2 API riêng của phiếu nhập kho, không thuộc phạm vi Task 2's shared stores).

Gọi `ensureInventory()` trong `onMounted`. `ensurePhieuNhapData()` giữ nguyên là cached-promise gọi khi bấm vào tab "phiếu-nhap" (đúng hành vi gốc — lazy load, không tải ngay khi mount).

Template: giữ nguyên cấu trúc tab-switcher + 2 `<template v-if="khoTab===...">` đã đọc ở Step 1, cộng 4 modal.

- [ ] **Step 3: Xử lý tab "Bảo hành" còn lại trong AdminPage.vue**

Tab `khoTab === 'bao-hanh'` (ngoài phạm vi Plan này) hiện đang sống chung `<section currentPage === 'inventory'>` với 2 tab vừa tách — sau khi tách, section `inventory` gốc CHỈ CÒN nội dung tab "bảo hành" + tab-switcher đã rút gọn còn 1 nút. Kiểm tra lại: nếu tab "bảo hành" phụ thuộc `khoTab` để biết đang active hay không (dùng chung biến `khoTab` với 2 tab vừa tách) — vì `khoTab` giờ đã chuyển hẳn vào `InventoryPanel.vue`, cần 1 biến `khoTab` MỚI, riêng, cục bộ trong AdminPage.vue chỉ dùng cho tab bảo hành (luôn `'bao-hanh'`, không cần switcher nữa vì chỉ còn đúng 1 tab). Đơn giản hóa: bỏ hẳn khái niệm tab-switcher ở phần còn lại trong AdminPage.vue, hiển thị thẳng nội dung "bảo hành" không qua điều kiện `khoTab` nữa (vì giờ nó là nội dung duy nhất của section `inventory` trong AdminPage.vue). Đọc kỹ code tab bảo hành hiện tại (tìm theo `khoTab==='bao-hanh'` hoặc `ensureWarrantyData`) trước khi sửa, XÁC NHẬN không có phần nào của tab bảo hành gọi tới state/hàm vừa chuyển sang `InventoryPanel.vue` (nếu có, đó là 1 dạng coupling khác chưa được khảo sát — DỪNG, báo cáo lại thay vì tự ý sửa).

- [ ] **Step 4: Sửa `AdminPage.vue` — phần tồn kho + phiếu nhập**

Xóa khỏi template: 2 khối `<template v-if="khoTab==='ton-kho'">`/`<template v-else-if="khoTab==='phieu-nhap'">` + 2 nút tab-switcher tương ứng (giữ nút bảo hành), 4 modal liệt kê ở Step 1.
Xóa khỏi script: toàn bộ liệt kê ở Step 1 (trừ phần tab bảo hành xử lý riêng ở Step 3).

Thêm import: `import InventoryPanel from "../components/admin/InventoryPanel.vue";`

Cấu trúc mới của section `inventory` trong AdminPage.vue (gộp `InventoryPanel` + phần bảo hành còn lại):
```html
<section v-show="currentPage === 'inventory'">
  <ul class="nav nav-tabs mb-3">
    <li class="nav-item"><button class="nav-link" :class="{active: inventoryMainTab==='kho'}" @click="inventoryMainTab='kho'">{{ t('admin.inventory.tabStock') }} / {{ t('admin.inventory.tabReceipts') }}</button></li>
    <li class="nav-item"><button class="nav-link" :class="{active: inventoryMainTab==='bao-hanh'}" @click="inventoryMainTab='bao-hanh'; ensureWarrantyData()">{{ t('admin.inventory.tabWarranty') }}</button></li>
  </ul>
  <InventoryPanel v-show="inventoryMainTab==='kho'" />
  <div v-show="inventoryMainTab==='bao-hanh'"><!-- nội dung bảo hành cũ chuyển vào đây --></div>
</section>
```
(Thêm `const inventoryMainTab = ref('kho');` — biến mới, thay thế `khoTab` cũ ở cấp AdminPage.vue. Kiểm tra i18n key `admin.inventory.tabStock`/`tabReceipts`/`tabWarranty` đã tồn tại — theo khảo sát dòng 3340-3341 gốc đã có `tabReceipts`, kiểm tra 2 key còn lại, thêm nếu thiếu vào cả 5 file locale.)

- [ ] **Step 5: Build + kiểm thử thủ công**

```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run build
```
`npm run dev`, tab Kho hàng: xem/sửa tồn kho 1 biến thể, thêm serial, tạo phiếu nhập kho mới, xem chi tiết phiếu nhập, in/xuất Excel (nếu môi trường cho phép), chuyển sang tab Bảo hành xác nhận vẫn hoạt động y hệt trước.

- [ ] **Step 6: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/admin/InventoryPanel.vue \
  FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue
git commit -m "refactor: extract InventoryPanel (ton-kho + phieu-nhap tabs) from AdminPage"
```

---

### Task 8: `UserProfileMenu.vue`

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/components/admin/UserProfileMenu.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue`

**Interfaces:**
- Props: không có (tự đọc `AuthStore`). Emits: `navigate-settings` (cha lắng nghe, tự gọi `navigate('settings')` — component này không tự biết cách điều hướng, vì `navigate()`/`currentPage` là khái niệm cấp trang, sẽ khác nhau giữa AdminPage/StaffPage/WarehouseManagementPage).

- [ ] **Step 1: Đọc đoạn cần chuyển (theo commit `51999e5`, dùng tên biến để định vị nếu lệch)**

- Template: khối footer sidebar `<div class="p-3 border-top position-relative" ...>` (~2871-2904).
- Modal "Chỉnh sửa hồ sơ": `v-if="showEditProfileModal"` (~4153-4179).
- Modal "Đổi mật khẩu nhanh": `v-if="showQuickPasswordModal"` (~4181-4207).
- Script: `showUserMenu`, `userMenuTriggerRef`, `closeUserMenu`, `onUserMenuFocusOut`, `showEditProfileModal`+liên quan, `openEditProfileModal`, `saveProfile`, `showQuickPasswordModal`+liên quan, `openQuickPasswordModal`, `quickChangePassword`, `goToSettingsFromMenu`, `userDisplayName`, `userAvatar`, `userDisplayRole`, `logout` (~77-180, không liên tục).

- [ ] **Step 2: Viết `UserProfileMenu.vue`**

Import: `AuthStore`/`clearSession`/`setSession` (`stores/index.js`), `CaiDatService`, `t`. Toàn bộ state/hàm ở Step 1 chuyển nguyên vào, KHÔNG đổi tên gì (đã role-agnostic sẵn, không phụ thuộc AdminPage-specific state nào ngoài `AuthStore`).

`goToSettingsFromMenu()` đổi từ gọi `navigate('settings')` trực tiếp thành:
```js
const emit = defineEmits(['navigate-settings']);
const goToSettingsFromMenu = () => {
  showUserMenu.value = false;
  emit('navigate-settings');
};
```

- [ ] **Step 3: Sửa `AdminPage.vue`**

Xóa template footer sidebar cũ + 2 modal liệt kê ở Step 1, xóa toàn bộ script liệt kê ở Step 1.

Thêm import: `import UserProfileMenu from "../components/admin/UserProfileMenu.vue";`

Thay vị trí cũ bằng:
```html
<UserProfileMenu @navigate-settings="navigate('settings')" />
```
(Đặt đúng vị trí cũ trong `<aside>`, ngay trước `</aside>`.)

- [ ] **Step 4: Build + kiểm thử thủ công**

```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run build
```
`npm run dev`: mở dropdown hồ sơ, sửa hồ sơ, đổi mật khẩu nhanh, bấm "Cài đặt" từ menu — xác nhận điều hướng đúng sang tab Cài đặt. Xác nhận hành vi y hệt trước khi tách (đã kiểm thử kỹ ở plan `menu-ho-so-admin` trước đó, task này chỉ đổi VỊ TRÍ code, không đổi hành vi).

- [ ] **Step 5: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/admin/UserProfileMenu.vue \
  FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue
git commit -m "refactor: extract UserProfileMenu from AdminPage"
```

---

### Task 9: Kiểm thử hồi quy toàn diện AdminPage.vue

**Files:** không có file thay đổi — chỉ chạy và quan sát toàn bộ AdminPage.vue sau khi đã tách hết 7 component + 7 store.

- [ ] **Step 1: Build production**

```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run build
```
Expected: `✓ built in ...`, không warning lạ (so với build trước khi bắt đầu Plan này — nếu có warning mới xuất hiện, điều tra trước khi tiếp tục).

- [ ] **Step 2: Kịch bản hồi quy toàn diện (đăng nhập admin, chạy backend + `npm run dev`)**

Đi qua TỪNG tab, xác nhận không có tab nào trắng/lỗi console, dữ liệu hiển thị đúng:
1. Dashboard — số liệu KPI, biểu đồ doanh thu hiển thị đúng (đọc từ các store mới qua alias).
2. Sản phẩm — thêm/sửa/xem chi tiết/xóa 1 sản phẩm thử.
3. Đơn hàng — xem chi tiết, đổi trạng thái 1 đơn thử, xác nhận sidebar badge "đơn hôm nay" cập nhật đúng.
4. Khách hàng — thêm/sửa/xóa 1 khách hàng thử, xác nhận sidebar/dashboard đếm đúng.
5. Kho hàng — tab tồn kho + tab phiếu nhập + tab bảo hành, cả 3 hoạt động đúng.
6. Khuyến mãi — không đổi gì (ngoài phạm vi tách) nhưng vẫn đọc `promotions` alias — xác nhận vẫn hiển thị đúng.
7. Bán hàng (POS) — chạy trọn 1 giao dịch: tìm sản phẩm, thêm vào giỏ, xác định khách hàng (thử cả 2 nhánh: khách đã có SĐT, và tạo khách mới ngay từ POS), đặt hàng, xác nhận đơn xuất hiện ngay ở tab Đơn hàng không cần F5 (SSE qua store hoạt động đúng).
8. Nhân viên — không đổi gì nhưng đọc `staff` alias (giờ từ `StaffStore`) — xác nhận vẫn hiển thị đúng.
9. Báo cáo — không đổi gì nhưng đọc `orders`/`products`/`customers` alias — xác nhận số liệu đúng.
10. Cài đặt — không đổi gì, không phụ thuộc store mới, xác nhận vẫn hoạt động.
11. Menu hồ sơ (chân sidebar) — dropdown, sửa hồ sơ, đổi mật khẩu, điều hướng sang Cài đặt.

- [ ] **Step 3: Dừng server, dọn dữ liệu thử (nếu có sản phẩm/khách hàng/đơn hàng thử tạo ra ở Step 2)**

## Tự rà soát (self-review)

**1. Phủ đủ spec đã điều chỉnh qua AskUserQuestion:**
- Shared store cho data → Task 2. ✅
- `ProductDetailModal`/`CustomerFormModal` tách riêng → Task 3, 4. ✅
- Gộp `InventoryPanel` (không tách 2 như spec gốc) → Task 7. ✅
- Promote `showToast`/format helpers → Task 1. ✅
- Cả 6 component/panel dùng chung theo spec (UserProfileMenu, ProductsTable, OrdersTable, CustomersTable, PosPanel, InventoryPanel-gộp) → Task 3, 4, 5, 6, 7, 8. ✅

**2. Không còn placeholder** — mọi task đều có code đầy đủ cho phần MỚI (store, component scaffold, đoạn tích hợp AdminPage.vue); phần markup/script MOVE nguyên vẹn từ AdminPage.vue trích dẫn chính xác theo commit `51999e5`, có tên biến làm mốc dự phòng nếu số dòng lệch — đây là cách xử lý bắt buộc do khối lượng code cần di chuyển quá lớn để chép tay lại toàn bộ vào văn bản plan mà không có rủi ro sai lệch cao hơn so với trích dẫn trực tiếp từ nguồn.

**3. Nhất quán:** tên store/component xuyên suốt các task khớp nhau (`ProductsStore`, `OrdersStore`, `CustomersStore`, `InventoryStore`, `SuppliersStore`, `StaffStore`, `PromotionsStore`; `ProductDetailModal`, `CustomerFormModal`, `ProductsTable`, `OrdersTable`, `CustomersTable`, `PosPanel`, `InventoryPanel`, `UserProfileMenu`).

**4. Thứ tự task đã tính đến phụ thuộc:** Task 1 (helpers) → Task 2 (data store) → Task 3-8 (component, mỗi cái phụ thuộc Task 1+2, một số phụ thuộc lẫn nhau: Task 5 cần Task 3's `ProductDetailModal`; Task 6 cần Task 4's `CustomerFormModal`) → Task 9 (hồi quy cuối). Task 4 cố ý để lại 1 lỗ hổng đã ghi rõ trong commit message, Task 6 khắc phục ngay sau — đây là phụ thuộc thẳng hàng, không dispatch Task 6 trước khi Task 4 xong.

## Ngoài phạm vi

- Tab "Bảo hành" trong section Kho hàng — không tách, không đổi hành vi (chỉ đổi cách nó tồn tại độc lập trong AdminPage.vue sau khi 2 tab kia rời đi, xem Task 7 Step 3).
- Dashboard, Khuyến mãi, Nhân viên (trang quản lý), Báo cáo, Cài đặt — không tách trong plan này, tiếp tục sống trong AdminPage.vue.
- StaffPage.vue, WarehouseManagementPage.vue chưa được tạo — đó là Plan 3, Plan 4, dùng lại các component/store plan này tạo ra.
- Không thêm tính năng mới nào — mọi hành vi phải giống hệt trước khi tách (trừ việc xóa dead code `showVariantModal`/`openVariants` ở Task 3, đã xác nhận với người dùng là an toàn vì không nơi nào gọi tới).
