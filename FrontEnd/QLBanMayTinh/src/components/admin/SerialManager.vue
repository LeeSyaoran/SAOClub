<script setup>
import { ref, computed, onMounted } from "vue";
import { t } from "../../i18n/index.js";
import * as ChiTietSanPhamService from "../../Service/ChiTietSanPhamService.js";
import { formatDate } from "../../utils/adminFormat.js";
import { nowLocalIso } from "../../utils/datetime.js";
import { showToast } from "../../stores/toast.js";
import { askConfirm } from "../../stores/confirm.js";
import { ProductsStore, ensureProducts } from "../../stores/products.js";
import SearchSelect from "../common/SearchSelect.vue";

const items = ref([]);
const loading = ref(false);
const search = ref("");

const load = async () => {
  loading.value = true;
  try {
    items.value = await ChiTietSanPhamService.getAll().catch(() => []);
  } finally {
    loading.value = false;
  }
};
onMounted(() => { load(); ensureProducts(); });

const variantOptions = computed(() =>
  ProductsStore.items.map((p) => ({ value: p.bienTheId, label: `${p.tenSanPham} — ${p.maSku}` }))
);
const variantLabel = (bienTheId) => variantOptions.value.find((o) => o.value === bienTheId)?.label ?? '';

const filteredItems = computed(() => {
  const q = search.value.trim().toLowerCase();
  if (!q) return items.value;
  return items.value.filter((i) =>
    [i.soSerial, i.maSku, variantLabel(i.bienTheId)].some((v) => (v || '').toLowerCase().includes(q))
  );
});

const STATUS_COLOR = {
  trong_kho: '#22c55e',
  giu_hang: '#facc15',
  da_ban: '#94a3b8',
  loi_bao_hanh: '#fb923c',
  da_tra_hang: '#38bdf8',
};
const statusColor = (s) => STATUS_COLOR[s] ?? '#6b7280';
const statusLabel = (s) => t(`admin.statusLabel.${s}`);

const showModal = ref(false);
const editingId = ref(null);
const formError = ref("");
const emptyForm = () => ({
  bienTheId: '',
  soSerial: '',
  trangThai: 'trong_kho',
  ngayNhapKho: nowLocalIso().slice(0, 16),
  ghiChu: '',
});
const form = ref(emptyForm());

const openAdd = () => {
  editingId.value = null;
  form.value = emptyForm();
  formError.value = "";
  showModal.value = true;
};
const openEdit = (item) => {
  editingId.value = item.chiTietId;
  form.value = {
    bienTheId: item.bienTheId,
    soSerial: item.soSerial,
    trangThai: item.trangThai,
    ngayNhapKho: (item.ngayNhapKho || '').slice(0, 16),
    ghiChu: item.ghiChu || '',
  };
  formError.value = "";
  showModal.value = true;
};

const saveSerial = async () => {
  formError.value = "";
  if (!form.value.bienTheId) { formError.value = t('admin.serialManager.variantRequired'); return; }
  if (!form.value.soSerial.trim()) { formError.value = t('admin.serialManager.serialRequired'); return; }
  try {
    const body = {
      bienTheId: Number(form.value.bienTheId),
      soSerial: form.value.soSerial.trim(),
      trangThai: form.value.trangThai,
      ngayNhapKho: nowLocalIso(new Date(form.value.ngayNhapKho)),
      ghiChu: form.value.ghiChu || null,
    };
    const res = editingId.value
      ? await ChiTietSanPhamService.update(editingId.value, body)
      : await ChiTietSanPhamService.create(body);
    if (!res.ok) {
      formError.value = t('admin.errors.saveFailed', { status: res.status, text: await res.text() });
      return;
    }
    showModal.value = false;
    await load();
  } catch (e) {
    formError.value = e.message;
  }
};

const deleteSerial = async (id) => {
  if (!(await askConfirm(t('admin.confirm.deleteSerial')))) return;
  const res = await ChiTietSanPhamService.remove(id);
  if (!res.ok) {
    showToast(await res.text().catch(() => t('admin.errors.deleteFailed', { status: res.status })));
    return;
  }
  await load();
};
</script>

<template>
  <div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
    <span class="text-secondary small">{{ filteredItems.length }}/{{ items.length }} serial</span>
    <div class="d-flex gap-2 flex-wrap">
      <input v-model="search" class="form-control form-control-sm" style="width:260px;background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);" :placeholder="t('admin.serialManager.searchPlaceholder')" />
      <button class="btn btn-sm btn-warning text-dark fw-bold" @click="openAdd">{{ t('admin.serialManager.add') }}</button>
    </div>
  </div>

  <div v-if="loading" class="text-secondary small">{{ t('admin.serialManager.loading') }}</div>
  <div v-else class="table-responsive">
    <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
      <thead>
        <tr>
          <th style="width:40px;">{{ t('admin.common.stt') }}</th>
          <th>{{ t('admin.serialManager.colVariant') }}</th>
          <th>{{ t('admin.serialManager.colSerial') }}</th>
          <th>{{ t('admin.serialManager.colStatus') }}</th>
          <th>{{ t('admin.serialManager.colDate') }}</th>
          <th>{{ t('admin.serialManager.colNote') }}</th>
          <th style="width:140px;">{{ t('admin.serialManager.colAction') }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(item, idx) in filteredItems" :key="item.chiTietId">
          <td class="text-secondary">{{ idx + 1 }}</td>
          <td>{{ variantLabel(item.bienTheId) || item.maSku }}</td>
          <td>{{ item.soSerial }}</td>
          <td>
            <span style="display:inline-block;width:9px;height:9px;border-radius:50%;margin-right:6px;" :style="{ background: statusColor(item.trangThai) }"></span>
            {{ statusLabel(item.trangThai) }}
          </td>
          <td class="text-secondary">{{ formatDate(item.ngayNhapKho) }}</td>
          <td class="text-secondary">{{ item.ghiChu }}</td>
          <td>
            <div class="d-flex gap-1">
              <button class="btn btn-sm btn-outline-warning" style="font-size:0.78rem;padding:2px 8px;" @click="openEdit(item)">{{ t('admin.serialManager.edit') }}</button>
              <button v-if="item.trangThai === 'trong_kho'" class="btn btn-sm btn-outline-danger" style="font-size:0.78rem;padding:2px 8px;" @click="deleteSerial(item.chiTietId)">{{ t('admin.serialManager.delete') }}</button>
            </div>
          </td>
        </tr>
        <tr v-if="filteredItems.length===0"><td colspan="7" class="text-center text-secondary">{{ t('admin.serialManager.empty') }}</td></tr>
      </tbody>
    </table>
  </div>

  <div v-if="showModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="showModal=false">
    <div class="rounded-3 p-3" style="background:var(--bg-card);width:420px;max-width:94vw;">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <div class="fw-bold" style="color:var(--text-heading);">{{ editingId ? t('admin.serialManager.titleEdit') : t('admin.serialManager.titleAdd') }}</div>
        <button class="btn-close btn-close-white btn-sm" @click="showModal=false"></button>
      </div>
      <div v-if="formError" class="alert alert-danger small py-2 mb-2">{{ formError }}</div>

      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ t('admin.serialManager.variantLabel') }}</label>
        <SearchSelect v-model="form.bienTheId" :options="variantOptions" :placeholder="t('admin.serialManager.variantPlaceholder')" />
      </div>
      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ t('admin.serialManager.serialLabel') }}</label>
        <input v-model="form.soSerial" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>
      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ t('admin.serialManager.statusLabel') }}</label>
        <select v-model="form.trangThai" class="form-select form-select-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);">
          <option value="trong_kho">{{ t('admin.statusLabel.trong_kho') }}</option>
          <option value="giu_hang">{{ t('admin.statusLabel.giu_hang') }}</option>
          <option value="da_ban">{{ t('admin.statusLabel.da_ban') }}</option>
          <option value="loi_bao_hanh">{{ t('admin.statusLabel.loi_bao_hanh') }}</option>
          <option value="da_tra_hang">{{ t('admin.statusLabel.da_tra_hang') }}</option>
        </select>
      </div>
      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ t('admin.serialManager.dateLabel') }}</label>
        <input v-model="form.ngayNhapKho" type="datetime-local" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>
      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ t('admin.serialManager.noteLabel') }}</label>
        <input v-model="form.ghiChu" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>

      <div class="d-flex justify-content-end gap-2">
        <button class="btn btn-sm btn-outline-secondary" @click="showModal=false">{{ t('admin.serialManager.cancel') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" @click="saveSerial">{{ t('admin.serialManager.save') }}</button>
      </div>
    </div>
  </div>
</template>
