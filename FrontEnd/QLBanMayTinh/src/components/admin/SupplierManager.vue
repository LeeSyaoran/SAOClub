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
const saving = ref(false);
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
  if (!form.value.soDienThoai.trim()) {
    formError.value = t('admin.supplierModal.phoneRequired');
    return;
  }
  if (!form.value.diaChi.trim()) {
    formError.value = t('admin.supplierModal.addressRequired');
    return;
  }
  if (saving.value) return;
  saving.value = true;
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
  } finally {
    saving.value = false;
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
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="showModal=false"></button>
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
        <button class="btn btn-sm btn-warning text-dark fw-bold" :disabled="saving" @click="saveSupplier">{{ editingId ? t('admin.productModal.update') : t('admin.productModal.addNew') }}</button>
      </div>
    </div>
  </div>
</template>
