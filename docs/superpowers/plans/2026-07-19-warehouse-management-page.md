# WarehouseManagementPage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Xây trang riêng cho vai trò `quan_kho` (quản lý kho), thay thế việc tạm thời dùng chung AdminPage — theo đúng spec `docs/superpowers/specs/2026-07-18-staff-warehouse-pages-design.md` và các component/store dùng chung đã xây ở Plan 2/3.

**Architecture:** Plan 4/4 (plan cuối) trong chuỗi 4 plan. `WarehouseManagementPage.vue` là page shell mới, theo đúng pattern `StaffPage.vue` (Plan 3), với 3 mục sidebar: Kho hàng (tái dùng nguyên `InventoryPanel.vue` từ Plan 2 — đã gộp sẵn 2 tab Tồn kho + Phiếu nhập), Nhà cung cấp (component mới, CRUD đầy đủ), Lịch sử tồn kho (component mới, chỉ đọc). Backend cho 2 phần mới đã có sẵn từ Plan 1 (`NhaCungCapController`/`LichSuTonKhoController`, đã khoá `hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')`) — plan này thuần frontend, không cần sửa backend.

**Tech Stack:** Vue 3 `<script setup>`, tái dùng store/component từ Plan 1-3.

## Global Constraints

- Backend không cần sửa gì — `NhaCungCapController`/`LichSuTonKhoController` đã tồn tại đầy đủ CRUD (Plan 1), đã khoá đúng role.
- Component mới đặt tại `src/components/admin/`, Service mới đặt tại `src/Service/`, theo đúng convention `get`/`post`/`put`/`del` từ `Service/api.js` (xem `Service/KhachHangService.js` làm mẫu — `get()` trả JSON đã parse, `post`/`put`/`del()` trả `Response` chưa parse, caller tự `.then(parseOrThrow)` hoặc check `res.ok`).
- `WarehouseManagementPage.vue` theo đúng layout shell của `StaffPage.vue` (Plan 3) — sidebar 240px + topbar + main, copy `.adm-nav`/`.adm-icon` CSS vào `<style scoped>` riêng (CSS scoped không kế thừa qua biên component).
- i18n: thêm key mới cho phần thật sự mới (`admin.suppliers.*`, `admin.supplierModal.*`, `admin.inventoryHistory.*`, `admin.sidebar.suppliers`/`inventoryHistory`, `admin.pageMeta.suppliers`/`inventoryHistory`) — đủ cả 5 file `vi.js`/`en.js`/`zh.js`/`ko.js`/`ja.js`. Tái dùng `admin.sidebar.inventory`/`admin.pageMeta.inventory` đã có cho mục "Kho hàng".
- Routing: `quan_kho` → `#kho` (đổi từ tạm thời `#admin` ở Plan 3). Điều kiện `#admin` quay lại đúng `role === 'admin'` (bỏ `quan_kho` ra khỏi danh sách được phép, vì giờ đã có trang riêng).

---

### Task 1: Frontend Service cho Nhà cung cấp + Lịch sử tồn kho

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/Service/NhaCungCapService.js`
- Create: `FrontEnd/QLBanMayTinh/src/Service/LichSuTonKhoService.js`

**Interfaces:**
- Produces: `NhaCungCapService.getAll()/save(id, body)/remove(id)` — map đúng `NhaCungCapController` (`GET /api/nha-cung-cap`, `POST /api/nha-cung-cap`, `PUT /api/nha-cung-cap/update/{id}`, `DELETE /api/nha-cung-cap/delete/{id}`). `LichSuTonKhoService.getAll()` — map `GET /api/lich-su-ton-kho`. Task 2 (SupplierManager + store) và Task 3 (InventoryHistoryPanel) dùng lại.

- [ ] **Step 1: Tạo `NhaCungCapService.js`**

`FrontEnd/QLBanMayTinh/src/Service/NhaCungCapService.js`:
```js
import { get, post, put, del } from './api.js';

export const getAll = () => get('/api/nha-cung-cap');

export const getById = (id) => get(`/api/nha-cung-cap/${id}`);

export const save = (id, body) =>
  id ? put(`/api/nha-cung-cap/update/${id}`, body) : post('/api/nha-cung-cap', body);

export const remove = (id) => del(`/api/nha-cung-cap/delete/${id}`);
```

- [ ] **Step 2: Tạo `LichSuTonKhoService.js`**

`FrontEnd/QLBanMayTinh/src/Service/LichSuTonKhoService.js`:
```js
import { get } from './api.js';

// Lịch sử tồn kho — chỉ đọc, backend cố ý không có endpoint cập nhật (audit trail,
// chỉ ghi thêm/xóa ở phía backend cho các luồng nghiệp vụ khác, UI không tự tạo/xóa).
export const getAll = () => get('/api/lich-su-ton-kho');
```

- [ ] **Step 3: Build kiểm tra**

Run:
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run build
```
Expected: `✓ built in ...`, không lỗi (2 file mới chưa được import ở đâu, chỉ cần không có lỗi cú pháp).

- [ ] **Step 4: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/Service/NhaCungCapService.js \
  FrontEnd/QLBanMayTinh/src/Service/LichSuTonKhoService.js
git commit -m "feat: add NhaCungCapService and LichSuTonKhoService frontend services"
```

---

### Task 2: `SupplierManager.vue` (CRUD Nhà cung cấp)

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/stores/suppliers.js`
- Create: `FrontEnd/QLBanMayTinh/src/components/admin/SupplierManager.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/en.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js`

**Interfaces:**
- Produces: `SuppliersStore`/`ensureSuppliers()`/`refreshSuppliers()` (mới thêm `refreshSuppliers`) từ `stores/suppliers.js` — `SupplierManager.vue` gọi sau khi thêm/sửa/xóa để `ProductsTable.vue`/`InventoryPanel.vue` (đang cùng đọc `SuppliersStore` trên cùng trang WarehouseManagementPage) thấy dữ liệu mới ngay, không cần tải lại trang.
- Không props/emit — tự chứa toàn bộ CRUD.

- [ ] **Step 1: Sửa `stores/suppliers.js` — thêm `refreshSuppliers`, chuyển sang dùng `NhaCungCapService`**

Thay toàn bộ nội dung `FrontEnd/QLBanMayTinh/src/stores/suppliers.js` bằng:
```js
import { reactive } from "vue";
import * as NhaCungCapService from "../Service/NhaCungCapService.js";

export const SuppliersStore = reactive({ items: [], loading: false, loaded: false });

let suppliersPromise = null;
export const ensureSuppliers = () => {
  if (suppliersPromise) return suppliersPromise;
  suppliersPromise = refreshSuppliers();
  return suppliersPromise;
};

export const refreshSuppliers = async () => {
  SuppliersStore.loading = true;
  try {
    SuppliersStore.items = await NhaCungCapService.getAll().catch(() => []);
    SuppliersStore.loaded = true;
  } finally {
    SuppliersStore.loading = false;
  }
  return SuppliersStore.items;
};
```
(Trước đây dùng `DmService.getNhaCungCap()` — cùng endpoint `GET /api/nha-cung-cap`, giờ chuyển hẳn sang `NhaCungCapService.getAll()` vừa tạo ở Task 1 để có 1 nơi duy nhất định nghĩa API nhà cung cấp, tránh 2 service cùng gọi 1 endpoint theo 2 cách khác nhau.)

**Lưu ý cho người triển khai:** kiểm tra `DmService.js` sau khi sửa xong — nếu `getNhaCungCap` không còn được gọi ở bất kỳ đâu khác trong dự án (grep `DmService.getNhaCungCap` toàn bộ `src/`), có thể để nguyên hàm đó trong `DmService.js` (không xóa — ngoài phạm vi task này, không phải dead code thật sự nếu còn nơi khác dùng, và xóa hàm export khỏi 1 service file có sẵn không thuộc phạm vi "thêm tính năng mới" của task này).

- [ ] **Step 2: Viết `SupplierManager.vue`**

`FrontEnd/QLBanMayTinh/src/components/admin/SupplierManager.vue`:
```vue
<script setup>
import { ref, computed, onMounted } from "vue";
import { t } from "../../i18n/index.js";
import * as NhaCungCapService from "../../Service/NhaCungCapService.js";
import { statusLabel } from "../../utils/adminFormat.js";
import { showToast } from "../../stores/toast.js";
import { askConfirm } from "../../stores/confirm.js";
import { SuppliersStore, ensureSuppliers, refreshSuppliers } from "../../stores/suppliers.js";

onMounted(() => { ensureSuppliers(); });

// ── Bo loc ────────────────────────────────────────────────────────────────────
const supplierSearch = ref("");
const filteredSuppliers = computed(() => {
  const q = supplierSearch.value.trim().toLowerCase();
  if (!q) return SuppliersStore.items;
  return SuppliersStore.items.filter((s) =>
    (s.tenNhaCungCap ?? '').toLowerCase().includes(q) ||
    (s.soDienThoai ?? '').includes(q) ||
    (s.email ?? '').toLowerCase().includes(q)
  );
});

// ── Modal them/sua ────────────────────────────────────────────────────────────
const showModal = ref(false);
const editingId = ref(null);
const formError = ref("");
const emptyForm = () => ({
  tenNhaCungCap: "",
  soDienThoai: "",
  email: "",
  diaChi: "",
  maSoThue: "",
  nguoiLienHe: "",
  trangThai: "active",
});
const form = ref(emptyForm());

const openAdd = () => {
  editingId.value = null;
  form.value = emptyForm();
  formError.value = "";
  showModal.value = true;
};
const openEdit = (s) => {
  editingId.value = s.nhaCungCapId;
  form.value = {
    tenNhaCungCap: s.tenNhaCungCap ?? "",
    soDienThoai: s.soDienThoai ?? "",
    email: s.email ?? "",
    diaChi: s.diaChi ?? "",
    maSoThue: s.maSoThue ?? "",
    nguoiLienHe: s.nguoiLienHe ?? "",
    trangThai: s.trangThai ?? "active",
  };
  formError.value = "";
  showModal.value = true;
};

const saveSupplier = async () => {
  formError.value = "";
  if (!form.value.tenNhaCungCap.trim()) {
    formError.value = t('admin.supplierModal.nameRequired');
    return;
  }
  try {
    const res = await NhaCungCapService.save(editingId.value, form.value);
    if (!res.ok) {
      formError.value = t('admin.errors.saveFailed', { status: res.status, text: await res.text() });
      return;
    }
    showModal.value = false;
    await refreshSuppliers();
  } catch (e) {
    formError.value = e.message;
  }
};

const deleteSupplier = async (id) => {
  if (!(await askConfirm(t('admin.confirm.deleteSupplier')))) return;
  const res = await NhaCungCapService.remove(id);
  if (!res.ok) { showToast(await res.text().catch(() => t('admin.errors.deleteFailed', { status: res.status }))); return; }
  await refreshSuppliers();
};
</script>

<template>
  <div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
    <span class="text-secondary small">{{ filteredSuppliers.length }}/{{ SuppliersStore.items.length }} {{ t('admin.suppliers.countSuffix') }}</span>
    <div class="d-flex gap-2 flex-wrap">
      <input v-model="supplierSearch" class="form-control form-control-sm" style="width:240px;background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);" :placeholder="t('admin.suppliers.searchPlaceholder')" />
      <button class="btn btn-sm btn-warning text-dark fw-bold" @click="openAdd">{{ t('admin.suppliers.add') }}</button>
    </div>
  </div>
  <div v-if="SuppliersStore.loading" class="text-secondary small">{{ t('admin.suppliers.loading') }}</div>
  <div v-else class="table-responsive">
    <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
      <thead><tr>
        <th style="width:40px;">{{ t('admin.common.stt') }}</th>
        <th>{{ t('admin.suppliers.colName') }}</th><th>{{ t('admin.suppliers.colPhone') }}</th><th>{{ t('admin.suppliers.colEmail') }}</th>
        <th>{{ t('admin.suppliers.colContact') }}</th><th>{{ t('admin.suppliers.colStatus') }}</th><th>{{ t('admin.suppliers.colAction') }}</th>
      </tr></thead>
      <tbody>
        <tr v-for="(s, idx) in filteredSuppliers" :key="s.nhaCungCapId">
          <td class="text-secondary">{{ idx + 1 }}</td>
          <td>{{ s.tenNhaCungCap }}</td>
          <td class="text-secondary">{{ s.soDienThoai }}</td>
          <td class="text-secondary">{{ s.email }}</td>
          <td class="text-secondary">{{ s.nguoiLienHe || '—' }}</td>
          <td><span class="badge" :class="s.trangThai==='active'?'bg-success':'bg-secondary'">{{ statusLabel(s.trangThai) }}</span></td>
          <td>
            <div class="d-flex gap-1">
              <button class="btn btn-sm btn-outline-warning" style="font-size:0.78rem; padding:2px 8px;" @click="openEdit(s)">{{ t('admin.suppliers.edit') }}</button>
              <button class="btn btn-sm btn-outline-danger"  style="font-size:0.78rem; padding:2px 8px;" @click="deleteSupplier(s.nhaCungCapId)">{{ t('admin.suppliers.delete') }}</button>
            </div>
          </td>
        </tr>
        <tr v-if="filteredSuppliers.length===0"><td colspan="7" class="text-center text-secondary">{{ t('admin.suppliers.empty') }}</td></tr>
      </tbody>
    </table>
  </div>

  <!-- ══ MODAL NHA CUNG CAP ══ -->
  <div v-if="showModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="showModal=false">
    <div class="rounded-3 p-3" style="background:var(--bg-card);width:460px;max-width:94vw;">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <div class="fw-bold" style="color:var(--text-heading);">{{ editingId ? t('admin.supplierModal.titleEdit') : t('admin.supplierModal.titleAdd') }}</div>
        <button class="btn-close btn-close-white btn-sm" @click="showModal=false"></button>
      </div>
      <div v-if="formError" class="alert alert-danger small py-2 mb-2">{{ formError }}</div>
      <div class="mb-2">
        <label class="form-label small text-secondary mb-1">{{ t('admin.supplierModal.nameLabel') }}</label>
        <input v-model="form.tenNhaCungCap" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>
      <div class="row g-2 mb-2">
        <div class="col-6">
          <label class="form-label small text-secondary mb-1">{{ t('admin.supplierModal.phoneLabel') }}</label>
          <input v-model="form.soDienThoai" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
        </div>
        <div class="col-6">
          <label class="form-label small text-secondary mb-1">{{ t('admin.supplierModal.emailLabel') }}</label>
          <input v-model="form.email" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
        </div>
      </div>
      <div class="mb-2">
        <label class="form-label small text-secondary mb-1">{{ t('admin.supplierModal.addressLabel') }}</label>
        <input v-model="form.diaChi" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>
      <div class="row g-2 mb-2">
        <div class="col-6">
          <label class="form-label small text-secondary mb-1">{{ t('admin.supplierModal.taxCodeLabel') }}</label>
          <input v-model="form.maSoThue" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
        </div>
        <div class="col-6">
          <label class="form-label small text-secondary mb-1">{{ t('admin.supplierModal.contactLabel') }}</label>
          <input v-model="form.nguoiLienHe" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
        </div>
      </div>
      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ t('admin.supplierModal.statusLabel') }}</label>
        <select v-model="form.trangThai" class="form-select form-select-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);">
          <option value="active">{{ t('admin.productModal.statusActive') }}</option>
          <option value="inactive">{{ t('admin.productModal.statusInactive') }}</option>
        </select>
      </div>
      <div class="d-flex justify-content-end gap-2">
        <button class="btn btn-sm btn-outline-secondary" @click="showModal=false">{{ t('admin.productModal.cancel') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" @click="saveSupplier">{{ editingId ? t('admin.productModal.update') : t('admin.productModal.addNew') }}</button>
      </div>
    </div>
  </div>
</template>
```
(Tái dùng `admin.productModal.statusActive`/`statusInactive`/`cancel`/`update`/`addNew` đã có sẵn — không tạo key trùng lặp cho các nhãn nút chung.)

- [ ] **Step 3: Thêm i18n block `admin.suppliers` + `admin.supplierModal` — đủ cả 5 file**

Trong mỗi file locale, tìm khối `customers: { ... }` (kết thúc bằng `},` ngay trước dòng trống + `customerModal: {`), chèn 2 khối MỚI `suppliers`/`supplierModal` ngay sau khối `customerModal` đóng (tìm dòng `},` đóng `customerModal`, trước dòng trống tiếp theo):

`vi.js`:
```js

    suppliers: {
      countSuffix: "nhà cung cấp",
      add: "+ Thêm nhà cung cấp",
      loading: "Đang tải...",
      colName: "Tên nhà cung cấp",
      colPhone: "Điện thoại",
      colEmail: "Email",
      colContact: "Người liên hệ",
      colStatus: "Trạng thái",
      colAction: "Thao tác",
      edit: "Sửa",
      delete: "Xóa",
      empty: "Chưa có nhà cung cấp",
      searchPlaceholder: "Tìm tên, SĐT, email...",
    },

    supplierModal: {
      titleEdit: "Cập nhật nhà cung cấp",
      titleAdd: "Thêm nhà cung cấp mới",
      nameLabel: "Tên nhà cung cấp *",
      phoneLabel: "Điện thoại",
      emailLabel: "Email",
      addressLabel: "Địa chỉ",
      taxCodeLabel: "Mã số thuế",
      contactLabel: "Người liên hệ",
      statusLabel: "Trạng thái",
      nameRequired: "Vui lòng nhập tên nhà cung cấp",
    },
```

`en.js`:
```js

    suppliers: {
      countSuffix: "suppliers",
      add: "+ Add supplier",
      loading: "Loading...",
      colName: "Supplier name",
      colPhone: "Phone",
      colEmail: "Email",
      colContact: "Contact person",
      colStatus: "Status",
      colAction: "Action",
      edit: "Edit",
      delete: "Delete",
      empty: "No suppliers yet",
      searchPlaceholder: "Search name, phone, email...",
    },

    supplierModal: {
      titleEdit: "Update supplier",
      titleAdd: "Add new supplier",
      nameLabel: "Supplier name *",
      phoneLabel: "Phone",
      emailLabel: "Email",
      addressLabel: "Address",
      taxCodeLabel: "Tax code",
      contactLabel: "Contact person",
      statusLabel: "Status",
      nameRequired: "Please enter the supplier name",
    },
```

`zh.js`:
```js

    suppliers: {
      countSuffix: "个供应商",
      add: "+ 添加供应商",
      loading: "加载中...",
      colName: "供应商名称",
      colPhone: "电话",
      colEmail: "邮箱",
      colContact: "联系人",
      colStatus: "状态",
      colAction: "操作",
      edit: "编辑",
      delete: "删除",
      empty: "暂无供应商",
      searchPlaceholder: "搜索名称、电话、邮箱...",
    },

    supplierModal: {
      titleEdit: "更新供应商",
      titleAdd: "添加新供应商",
      nameLabel: "供应商名称 *",
      phoneLabel: "电话",
      emailLabel: "邮箱",
      addressLabel: "地址",
      taxCodeLabel: "税号",
      contactLabel: "联系人",
      statusLabel: "状态",
      nameRequired: "请输入供应商名称",
    },
```

`ko.js`:
```js

    suppliers: {
      countSuffix: "개 공급업체",
      add: "+ 공급업체 추가",
      loading: "로딩 중...",
      colName: "공급업체명",
      colPhone: "전화번호",
      colEmail: "이메일",
      colContact: "담당자",
      colStatus: "상태",
      colAction: "작업",
      edit: "수정",
      delete: "삭제",
      empty: "등록된 공급업체가 없습니다",
      searchPlaceholder: "이름, 전화번호, 이메일 검색...",
    },

    supplierModal: {
      titleEdit: "공급업체 수정",
      titleAdd: "새 공급업체 추가",
      nameLabel: "공급업체명 *",
      phoneLabel: "전화번호",
      emailLabel: "이메일",
      addressLabel: "주소",
      taxCodeLabel: "사업자번호",
      contactLabel: "담당자",
      statusLabel: "상태",
      nameRequired: "공급업체명을 입력해 주세요",
    },
```

`ja.js`:
```js

    suppliers: {
      countSuffix: "件のサプライヤー",
      add: "+ サプライヤーを追加",
      loading: "読み込み中...",
      colName: "サプライヤー名",
      colPhone: "電話番号",
      colEmail: "メール",
      colContact: "担当者",
      colStatus: "ステータス",
      colAction: "操作",
      edit: "編集",
      delete: "削除",
      empty: "サプライヤーがまだありません",
      searchPlaceholder: "名前・電話番号・メールで検索...",
    },

    supplierModal: {
      titleEdit: "サプライヤーを更新",
      titleAdd: "新しいサプライヤーを追加",
      nameLabel: "サプライヤー名 *",
      phoneLabel: "電話番号",
      emailLabel: "メール",
      addressLabel: "住所",
      taxCodeLabel: "税番号",
      contactLabel: "担当者",
      statusLabel: "ステータス",
      nameRequired: "サプライヤー名を入力してください",
    },
```

- [ ] **Step 4: Thêm key `admin.confirm.deleteSupplier` — đủ cả 5 file**

Trong khối `confirm: { ... }` đã có (tìm dòng `deletePhieuNhap: "..."`, ngay trước dấu `},` đóng khối), thêm 1 dòng mới ngay sau:

`vi.js`: `deleteSupplier: "Xóa nhà cung cấp này?",`
`en.js`: `deleteSupplier: "Delete this supplier?",`
`zh.js`: `deleteSupplier: "删除此供应商？",`
`ko.js`: `deleteSupplier: "이 공급업체를 삭제하시겠습니까?",`
`ja.js`: `deleteSupplier: "このサプライヤーを削除しますか？",`

- [ ] **Step 5: Build kiểm tra**

Run:
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run build
```
Expected: `✓ built in ...`, không lỗi.

- [ ] **Step 6: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/stores/suppliers.js \
  FrontEnd/QLBanMayTinh/src/components/admin/SupplierManager.vue \
  FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js \
  FrontEnd/QLBanMayTinh/src/i18n/locales/en.js \
  FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js \
  FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js \
  FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js
git commit -m "feat: add SupplierManager (full CRUD) + refreshSuppliers store function"
```

---

### Task 3: `InventoryHistoryPanel.vue` (chỉ đọc)

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/components/admin/InventoryHistoryPanel.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/en.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js`

**Interfaces:**
- Không props/emit. Tự fetch qua `LichSuTonKhoService.getAll()` (Task 1) — không qua store dùng chung (dữ liệu chỉ dùng ở đúng 1 nơi, không cần cache/share như products/orders).

- [ ] **Step 1: Viết `InventoryHistoryPanel.vue`**

`FrontEnd/QLBanMayTinh/src/components/admin/InventoryHistoryPanel.vue`:
```vue
<script setup>
import { ref, computed, onMounted } from "vue";
import { t } from "../../i18n/index.js";
import * as LichSuTonKhoService from "../../Service/LichSuTonKhoService.js";
import { formatDateTime } from "../../utils/adminFormat.js";

const items = ref([]);
const loading = ref(false);

const fetchHistory = async () => {
  loading.value = true;
  try {
    items.value = await LichSuTonKhoService.getAll().catch(() => []);
  } finally {
    loading.value = false;
  }
};
onMounted(fetchHistory);

// ── Nhan/mau theo loai bien dong (khop CK_lsdk_loai trong DB: nhap, xuat_ban,
// tra_hang, dieu_chinh, huy, giu_hang) ──
const LOAI_BIEN_DONG_META = {
  nhap:       { label: () => t('admin.inventoryHistory.typeNhap'),      color: '#48c78e' },
  xuat_ban:   { label: () => t('admin.inventoryHistory.typeXuatBan'),   color: '#e05252' },
  tra_hang:   { label: () => t('admin.inventoryHistory.typeTraHang'),   color: '#3e8ed0' },
  dieu_chinh: { label: () => t('admin.inventoryHistory.typeDieuChinh'), color: '#ffb703' },
  huy:        { label: () => t('admin.inventoryHistory.typeHuy'),       color: '#6c757d' },
  giu_hang:   { label: () => t('admin.inventoryHistory.typeGiuHang'),   color: '#8a63d2' },
};
const typeLabel = (loai) => LOAI_BIEN_DONG_META[loai]?.label() ?? loai;
const typeColor = (loai) => LOAI_BIEN_DONG_META[loai]?.color ?? '#6c757d';

// ── Bo loc ────────────────────────────────────────────────────────────────────
const search = ref("");
const typeFilter = ref("");
const filteredItems = computed(() => {
  const q = search.value.trim().toLowerCase();
  return items.value
    .filter((h) => !typeFilter.value || h.loaiBienDong === typeFilter.value)
    .filter((h) => !q || (h.maSku ?? '').toLowerCase().includes(q) || (h.ghiChu ?? '').toLowerCase().includes(q))
    .sort((a, b) => new Date(b.ngayTao) - new Date(a.ngayTao));
});
</script>

<template>
  <div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
    <span class="text-secondary small">{{ filteredItems.length }}/{{ items.length }} {{ t('admin.inventoryHistory.countSuffix') }}</span>
    <div class="d-flex gap-2 flex-wrap">
      <input v-model="search" class="form-control form-control-sm" style="width:220px;background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);" :placeholder="t('admin.inventoryHistory.searchPlaceholder')" />
      <select v-model="typeFilter" class="form-select form-select-sm" style="width:170px;background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);">
        <option value="">{{ t('admin.inventoryHistory.allTypes') }}</option>
        <option value="nhap">{{ t('admin.inventoryHistory.typeNhap') }}</option>
        <option value="xuat_ban">{{ t('admin.inventoryHistory.typeXuatBan') }}</option>
        <option value="tra_hang">{{ t('admin.inventoryHistory.typeTraHang') }}</option>
        <option value="dieu_chinh">{{ t('admin.inventoryHistory.typeDieuChinh') }}</option>
        <option value="huy">{{ t('admin.inventoryHistory.typeHuy') }}</option>
        <option value="giu_hang">{{ t('admin.inventoryHistory.typeGiuHang') }}</option>
      </select>
    </div>
  </div>
  <div v-if="loading" class="text-secondary small">{{ t('admin.inventoryHistory.loading') }}</div>
  <div v-else class="table-responsive">
    <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
      <thead><tr>
        <th style="width:40px;">{{ t('admin.common.stt') }}</th>
        <th>{{ t('admin.inventoryHistory.colDate') }}</th><th>{{ t('admin.inventoryHistory.colSku') }}</th>
        <th>{{ t('admin.inventoryHistory.colType') }}</th><th>{{ t('admin.inventoryHistory.colQty') }}</th>
        <th>{{ t('admin.inventoryHistory.colNote') }}</th>
      </tr></thead>
      <tbody>
        <tr v-for="(h, idx) in filteredItems" :key="h.lichSuId">
          <td class="text-secondary">{{ idx + 1 }}</td>
          <td class="text-secondary">{{ formatDateTime(h.ngayTao) }}</td>
          <td style="font-family:monospace;">{{ h.maSku }}</td>
          <td><span class="badge" :style="{ background: typeColor(h.loaiBienDong) }">{{ typeLabel(h.loaiBienDong) }}</span></td>
          <td :class="h.soLuongThayDoi >= 0 ? 'text-success' : 'text-danger'" class="fw-bold">{{ h.soLuongThayDoi >= 0 ? '+' : '' }}{{ h.soLuongThayDoi }}</td>
          <td class="text-secondary">{{ h.ghiChu || '—' }}</td>
        </tr>
        <tr v-if="filteredItems.length===0"><td colspan="6" class="text-center text-secondary">{{ t('admin.inventoryHistory.empty') }}</td></tr>
      </tbody>
    </table>
  </div>
</template>
```

- [ ] **Step 2: Thêm i18n block `admin.inventoryHistory` — đủ cả 5 file**

Trong mỗi file, tìm khối `inventory: { ... }` đã có (kết thúc bằng `empty: "..."` rồi `},`), chèn khối MỚI `inventoryHistory` ngay sau dấu `},` đóng khối `inventory`:

`vi.js`:
```js

    inventoryHistory: {
      countSuffix: "lượt biến động",
      loading: "Đang tải...",
      searchPlaceholder: "Tìm SKU, ghi chú...",
      allTypes: "Tất cả loại",
      typeNhap: "Nhập kho",
      typeXuatBan: "Xuất bán",
      typeTraHang: "Trả hàng",
      typeDieuChinh: "Điều chỉnh",
      typeHuy: "Hủy",
      typeGiuHang: "Giữ hàng",
      colDate: "Thời gian",
      colSku: "SKU",
      colType: "Loại biến động",
      colQty: "Số lượng",
      colNote: "Ghi chú",
      empty: "Chưa có lịch sử biến động",
    },
```

`en.js`:
```js

    inventoryHistory: {
      countSuffix: "movements",
      loading: "Loading...",
      searchPlaceholder: "Search SKU, note...",
      allTypes: "All types",
      typeNhap: "Stock in",
      typeXuatBan: "Sold",
      typeTraHang: "Returned",
      typeDieuChinh: "Adjusted",
      typeHuy: "Cancelled",
      typeGiuHang: "Held",
      colDate: "Date",
      colSku: "SKU",
      colType: "Movement type",
      colQty: "Quantity",
      colNote: "Note",
      empty: "No inventory movements yet",
    },
```

`zh.js`:
```js

    inventoryHistory: {
      countSuffix: "条变动记录",
      loading: "加载中...",
      searchPlaceholder: "搜索 SKU、备注...",
      allTypes: "全部类型",
      typeNhap: "入库",
      typeXuatBan: "售出",
      typeTraHang: "退货",
      typeDieuChinh: "调整",
      typeHuy: "取消",
      typeGiuHang: "锁定",
      colDate: "时间",
      colSku: "SKU",
      colType: "变动类型",
      colQty: "数量",
      colNote: "备注",
      empty: "暂无库存变动记录",
    },
```

`ko.js`:
```js

    inventoryHistory: {
      countSuffix: "건의 변동 내역",
      loading: "로딩 중...",
      searchPlaceholder: "SKU, 메모 검색...",
      allTypes: "전체 유형",
      typeNhap: "입고",
      typeXuatBan: "판매",
      typeTraHang: "반품",
      typeDieuChinh: "조정",
      typeHuy: "취소",
      typeGiuHang: "홀드",
      colDate: "일시",
      colSku: "SKU",
      colType: "변동 유형",
      colQty: "수량",
      colNote: "메모",
      empty: "재고 변동 내역이 없습니다",
    },
```

`ja.js`:
```js

    inventoryHistory: {
      countSuffix: "件の変動",
      loading: "読み込み中...",
      searchPlaceholder: "SKU・メモで検索...",
      allTypes: "すべての種類",
      typeNhap: "入庫",
      typeXuatBan: "販売",
      typeTraHang: "返品",
      typeDieuChinh: "調整",
      typeHuy: "キャンセル",
      typeGiuHang: "保留",
      colDate: "日時",
      colSku: "SKU",
      colType: "変動種類",
      colQty: "数量",
      colNote: "メモ",
      empty: "在庫変動履歴はまだありません",
    },
```

- [ ] **Step 3: Build kiểm tra**

Run:
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run build
```
Expected: `✓ built in ...`, không lỗi.

- [ ] **Step 4: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/admin/InventoryHistoryPanel.vue \
  FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js \
  FrontEnd/QLBanMayTinh/src/i18n/locales/en.js \
  FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js \
  FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js \
  FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js
git commit -m "feat: add InventoryHistoryPanel (read-only stock movement log)"
```

---

### Task 4: `WarehouseManagementPage.vue`

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/pages/WarehouseManagementPage.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/en.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js`

**Interfaces:**
- Consumes: `InventoryPanel` (Plan 2, đầy đủ quyền — không cần prop giới hạn, đúng công việc của quản lý kho), `SupplierManager` (Task 2), `InventoryHistoryPanel` (Task 3), `UserProfileMenu` (`:show-settings-link="false"`, giống `StaffPage.vue`).
- Produces: page component `WarehouseManagementPage.vue` — Task 5 (App.vue) import và render khi `quan_kho` đăng nhập.

- [ ] **Step 1: Thêm i18n `admin.sidebar.suppliers`/`inventoryHistory` + `admin.pageMeta.suppliers`/`inventoryHistory` — đủ cả 5 file**

Trong mỗi file, tìm khối `sidebar: { ... }` (khối THỨ 2 trong file, dùng cho admin — không phải khối đầu dùng cho trang khách hàng), thêm 2 dòng mới ngay trước dấu `},` đóng khối:

`vi.js` (thêm vào `sidebar`):
```js
      suppliers: "Nhà cung cấp",
      inventoryHistory: "Lịch sử tồn kho",
```
`en.js`: `suppliers: "Suppliers",` / `inventoryHistory: "Inventory history",`
`zh.js`: `suppliers: "供应商",` / `inventoryHistory: "库存历史",`
`ko.js`: `suppliers: "공급업체",` / `inventoryHistory: "재고 이력",`
`ja.js`: `suppliers: "サプライヤー",` / `inventoryHistory: "在庫履歴",`

Trong khối `pageMeta: { ... }`, thêm 2 dòng mới ngay trước dấu `},` đóng khối:

`vi.js`:
```js
      suppliers:        { title: "Nhà cung cấp",   sub: "Quản lý nhà cung cấp" },
      inventoryHistory:  { title: "Lịch sử tồn kho", sub: "Lịch sử biến động tồn kho" },
```
`en.js`:
```js
      suppliers:        { title: "Suppliers",   sub: "Manage suppliers" },
      inventoryHistory:  { title: "Inventory history", sub: "Stock movement history" },
```
`zh.js`:
```js
      suppliers:        { title: "供应商",   sub: "管理供应商" },
      inventoryHistory:  { title: "库存历史", sub: "库存变动历史" },
```
`ko.js`:
```js
      suppliers:        { title: "공급업체",   sub: "공급업체 관리" },
      inventoryHistory:  { title: "재고 이력", sub: "재고 변동 이력" },
```
`ja.js`:
```js
      suppliers:        { title: "サプライヤー",   sub: "サプライヤー管理" },
      inventoryHistory:  { title: "在庫履歴", sub: "在庫変動履歴" },
```

- [ ] **Step 2: Viết `WarehouseManagementPage.vue`**

`FrontEnd/QLBanMayTinh/src/pages/WarehouseManagementPage.vue`:
```vue
<script setup>
import { computed, ref } from "vue";
import { t } from "../i18n/index.js";
import { ThemeStore, toggleTheme } from "../stores/theme.js";
import ConfirmDialog from "../components/common/ConfirmDialog.vue";
import ToastHost from "../components/common/ToastHost.vue";
import UserProfileMenu from "../components/admin/UserProfileMenu.vue";
import InventoryPanel from "../components/admin/InventoryPanel.vue";
import SupplierManager from "../components/admin/SupplierManager.vue";
import InventoryHistoryPanel from "../components/admin/InventoryHistoryPanel.vue";

// ── Navigation — mac dinh vao thang Kho hang (viec chinh hang ngay cua quan ly kho) ──
const currentPage = ref("inventory");
const navigate = (page) => { currentPage.value = page; };

const PAGE_META = {
  inventory: { titleKey: "admin.pageMeta.inventory.title", subKey: "admin.pageMeta.inventory.sub", icon: "📦" },
  suppliers: { titleKey: "admin.pageMeta.suppliers.title", subKey: "admin.pageMeta.suppliers.sub", icon: "🚚" },
  inventoryHistory: { titleKey: "admin.pageMeta.inventoryHistory.title", subKey: "admin.pageMeta.inventoryHistory.sub", icon: "📜" },
};
const topbarTitle = computed(() => t(PAGE_META[currentPage.value]?.titleKey ?? "admin.pageMeta.inventory.title"));
const topbarSub = computed(() => t(PAGE_META[currentPage.value]?.subKey ?? ""));
const topbarIcon = computed(() => PAGE_META[currentPage.value]?.icon ?? "📦");
</script>

<template>
  <!-- Layout chinh: sidebar ben trai + main content ben phai — dong bo AdminPage.vue/StaffPage.vue -->
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

      <!-- Nav kho -->
      <nav class="flex-grow-1 d-flex flex-column px-2 pb-2">
        <div class="adm-nav" :class="{active: currentPage==='inventory'}" @click="navigate('inventory')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M4 3a2 2 0 100 4h12a2 2 0 100-4H4z"/><path fill-rule="evenodd" d="M3 8h14v7a2 2 0 01-2 2H5a2 2 0 01-2-2V8zm5 3a1 1 0 011-1h2a1 1 0 110 2H9a1 1 0 01-1-1z" clip-rule="evenodd"/></svg>
          {{ t('admin.sidebar.inventory') }}
        </div>
        <div class="adm-nav" :class="{active: currentPage==='suppliers'}" @click="navigate('suppliers')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M8 16.5a1.5 1.5 0 11-3 0 1.5 1.5 0 013 0zM15 16.5a1.5 1.5 0 11-3 0 1.5 1.5 0 013 0z"/><path d="M3 4a1 1 0 00-1 1v9a2 2 0 002 2h.05a2.5 2.5 0 014.9 0h4.1a2.5 2.5 0 014.9 0H18a1 1 0 001-1v-4a1 1 0 00-.293-.707l-3-3A1 1 0 0015 7h-1V5a1 1 0 00-1-1H3z"/></svg>
          {{ t('admin.sidebar.suppliers') }}
        </div>
        <div class="adm-nav" :class="{active: currentPage==='inventoryHistory'}" @click="navigate('inventoryHistory')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4zm2 6a1 1 0 011-1h6a1 1 0 110 2H7a1 1 0 01-1-1zm1 3a1 1 0 100 2h6a1 1 0 100-2H7z" clip-rule="evenodd"/></svg>
          {{ t('admin.sidebar.inventoryHistory') }}
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
        <section v-show="currentPage === 'inventory'"><InventoryPanel /></section>
        <section v-show="currentPage === 'suppliers'"><SupplierManager /></section>
        <section v-show="currentPage === 'inventoryHistory'"><InventoryHistoryPanel /></section>
      </div>
    </main>
  </div>

  <ConfirmDialog />
  <ToastHost />
</template>

<style scoped>
/* Nav item: dong bo AdminPage.vue/StaffPage.vue (.adm-nav/.adm-icon) — CSS scoped
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

- [ ] **Step 3: Build kiểm tra**

Run:
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run build
```
Expected: `✓ built in ...`, không lỗi.

- [ ] **Step 4: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/pages/WarehouseManagementPage.vue \
  FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js \
  FrontEnd/QLBanMayTinh/src/i18n/locales/en.js \
  FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js \
  FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js \
  FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js
git commit -m "feat: add WarehouseManagementPage shell (Kho hang/Nha cung cap/Lich su ton kho)"
```

---

### Task 5: Điều hướng `quan_kho` sang `#kho` trong `App.vue`

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/App.vue`

**Interfaces:**
- Consumes: `WarehouseManagementPage.vue` (Task 4).
- Sau task này: `admin` → `#admin`, `nhan_vien` → `#staff` (không đổi), `quan_kho` → `#kho` (đổi từ tạm thời `#admin` ở Plan 3). Điều kiện `#admin` bỏ `quan_kho` ra khỏi danh sách được phép — chỉ còn đúng `admin`.

**Trạng thái hiện tại (sau Plan 3, đã xác nhận qua đọc trực tiếp `App.vue`):**
```js
const ROLE_HASH = { admin: "#admin", nhan_vien: "#staff", quan_kho: "#admin" };
```
```html
<AdminPage v-if="isAdminHash && ['admin', 'quan_kho'].includes(auth.user?.role)" />
...
v-else-if="isAdminHash && !['admin', 'quan_kho'].includes(auth.user?.role)"
```
(`quan_kho` được thêm tạm thời vào 2 điều kiện này ở Plan 3 vì WarehouseManagementPage chưa tồn tại — giờ đã có, gỡ bỏ.)

- [ ] **Step 1: Thêm computed `isKhoHash`**

Tìm dòng:
```js
const isStaffHash = computed(() => currentHash.value === "#staff");
```
Thêm ngay sau:
```js
const isKhoHash = computed(() => currentHash.value === "#kho");
```

- [ ] **Step 2: Sửa `onLoginSuccess` — route `quan_kho` sang `#kho`**

Thay:
```js
  const ROLE_HASH = { admin: "#admin", nhan_vien: "#staff", quan_kho: "#admin" };
```
bằng:
```js
  const ROLE_HASH = { admin: "#admin", nhan_vien: "#staff", quan_kho: "#kho" };
```
(Xóa comment "quan_kho tạm thời vẫn về #admin" ở dòng ngay trên nếu có — không còn đúng nữa.)

- [ ] **Step 3: Thêm import `WarehouseManagementPage`**

Tìm dòng:
```js
import StaffPage from "./pages/StaffPage.vue";
```
Thêm ngay sau:
```js
import WarehouseManagementPage from "./pages/WarehouseManagementPage.vue";
```

- [ ] **Step 4: Gỡ `quan_kho` khỏi điều kiện `#admin`**

Thay:
```html
<AdminPage v-if="isAdminHash && ['admin', 'quan_kho'].includes(auth.user?.role)" />
```
bằng:
```html
<AdminPage v-if="isAdminHash && auth.user?.role === 'admin'" />
```

Thay:
```html
      v-else-if="isAdminHash && !['admin', 'quan_kho'].includes(auth.user?.role)"
```
bằng:
```html
      v-else-if="isAdminHash && auth.user?.role !== 'admin'"
```

- [ ] **Step 5: Thêm nhánh route `#kho`**

Ngay sau khối "Thông báo từ chối quyền truy cập (staff)" của `#staff` (kết thúc bằng `</section>` trước comment `TRANG TÀI KHOẢN KHÁCH HÀNG`), chèn thêm:
```html
    <!-- ══════════════════════════════════════════════════════
        TRANG QUẢN LÝ KHO — chỉ hiển thị khi URL có #kho VÀ đúng role quan_kho
    ══════════════════════════════════════════════════════ -->
    <WarehouseManagementPage v-else-if="isKhoHash && auth.user?.role === 'quan_kho'" />

    <!-- Thông báo từ chối quyền truy cập (kho) -->
    <section
      v-else-if="isKhoHash && auth.user?.role !== 'quan_kho'"
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
(Tái dùng nguyên `t("adminAccess.title")`/`t("adminAccess.desc")` — giống hệt cách `#staff` đã làm ở Plan 3, không cần key riêng.)

- [ ] **Step 6: Build kiểm tra**

Run:
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run build
```
Expected: `✓ built in ...`, không lỗi.

- [ ] **Step 7: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/App.vue
git commit -m "feat: route quan_kho role to WarehouseManagementPage, remove quan_kho from #admin allowlist"
```

---

### Task 6: Kiểm thử thủ công end-to-end

**Files:** không có file thay đổi — chỉ chạy và quan sát.

- [ ] **Step 1: Chạy backend + frontend**

```powershell
cd "d:\project code\SAOClub\BackEnd"; $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'; .\mvnw.cmd spring-boot:run
```
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run dev
```

- [ ] **Step 2: Kịch bản chính**

1. Đăng nhập bằng tài khoản `quan_kho` — tự động chuyển sang `#kho`, thấy đúng 3 mục sidebar (Kho hàng/Nhà cung cấp/Lịch sử tồn kho), mặc định vào tab Kho hàng.
2. Tab Kho hàng: xác nhận `InventoryPanel` hoạt động y hệt bên AdminPage (2 tab con Tồn kho/Phiếu nhập, đầy đủ quyền sửa tồn kho, tạo phiếu nhập).
3. Tab Nhà cung cấp: thêm 1 nhà cung cấp thử, sửa, xóa — xác nhận hoạt động đầy đủ. Sau khi thêm, chuyển ngay sang tab Kho hàng → mở form "Tạo phiếu nhập" → xác nhận nhà cung cấp vừa thêm xuất hiện ngay trong dropdown (không cần F5) — xác nhận `refreshSuppliers()` đồng bộ đúng.
4. Tab Lịch sử tồn kho: xác nhận danh sách hiển thị đúng (nếu có dữ liệu), lọc theo loại biến động, tìm theo SKU/ghi chú hoạt động.
5. Gõ tay `#admin` trên URL trong khi đang đăng nhập `quan_kho` — xác nhận bị chặn (màn "không có quyền"), KHÔNG vào được AdminPage (khác Plan 3 lúc trước, giờ `quan_kho` đã có trang riêng nên không còn được vào `#admin` nữa).
6. Đăng xuất, đăng nhập lại bằng `admin` — xác nhận AdminPage vẫn hoạt động y hệt trước (không bị ảnh hưởng bởi việc gỡ `quan_kho` khỏi điều kiện `#admin`).
7. Đăng nhập lại bằng `nhan_vien` — xác nhận vẫn vào đúng `#staff` như Plan 3, không bị ảnh hưởng.

- [ ] **Step 3: Dừng server**

`Ctrl+C` ở cả 2 terminal.

---

## Tự rà soát (self-review)

**1. Phủ đủ spec:**
- WarehouseManagementPage = Kho hàng/Tồn kho + Phiếu nhập kho (qua `InventoryPanel` đã gộp sẵn) + Nhà cung cấp + Lịch sử tồn kho → Task 2, 3, 4. ✅
- Routing theo role, không ảnh hưởng admin/nhan_vien → Task 5. ✅
- Backend không cần sửa (đã xong ở Plan 1) → xác nhận trong Global Constraints. ✅

**2. Không còn placeholder** — mọi file mới đều có code đầy đủ, không có TODO.

**3. Nhất quán:** `SuppliersStore`/`refreshSuppliers` (Task 2) được `ProductsTable.vue`/`InventoryPanel.vue` (Plan 2, không sửa gì ở đây) đọc đúng qua cùng 1 store — thêm `refreshSuppliers()` không phá vỡ interface cũ (`ensureSuppliers()` vẫn giữ nguyên chữ ký).

## Ngoài phạm vi

- Không đụng `PhieuTraHangController`/`ChiTietTraHangController`/tab Bảo hành — đúng theo spec ban đầu.
- Không thêm ownership-check hay phân quyền chi tiết hơn trong `NhaCungCapController`/`LichSuTonKhoController` — đã khoá đúng ở Plan 1 (`hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')`), không cần sửa thêm.
- `DmService.getNhaCungCap()` không bị xóa (dù có thể không còn ai gọi sau Task 2) — để nguyên, ngoài phạm vi dọn dẹp của plan này.
