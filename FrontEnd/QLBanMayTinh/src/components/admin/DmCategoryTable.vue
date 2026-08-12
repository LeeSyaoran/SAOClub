<script setup>
import { ref, computed, onMounted } from "vue";
import { Hash, FolderOpen, X } from '@lucide/vue';
import { t } from "../../i18n/index.js";
import { showToast } from "../../stores/toast.js";
import { nowLocalIso } from "../../utils/datetime.js";
import { formatDate } from "../../utils/adminFormat.js";
import * as XLSX from "xlsx";
import Pagination from "../common/Pagination.vue";
import { usePagination } from "../../composables/usePagination.js";

const props = defineProps({
  service: { type: Object, required: true },
  idField: { type: String, required: true },
  nameField: { type: String, required: true },
  label: { type: String, required: true },
  nameLabel: { type: String, required: true },
  serialService: { type: Object, required: true },
  serialFieldName: { type: String, required: true },
});

const items = ref([]);
const loading = ref(false);
const search = ref("");
// Toan bo serial cua loai linh kien nay (khong loc theo item) — tai 1 lan cung items,
// dung de dem "con bao nhieu" (trang_thai='trong_kho') tren tung dong + hien danh sach
// khi bam nut xem.
const serials = ref([]);

const load = async () => {
  loading.value = true;
  try {
    [items.value, serials.value] = await Promise.all([
      props.service.getAll().catch(() => []),
      props.serialService.getAll().catch(() => []),
    ]);
  } finally {
    loading.value = false;
  }
};
onMounted(load);

const serialsOf = (item) => serials.value.filter((s) => s[props.serialFieldName] === item[props.idField]);
const stockCountOf = (item) => serialsOf(item).filter((s) => s.trangThai === 'trong_kho').length;

const showSerialsModal = ref(false);
const serialsModalItem = ref(null);
const openSerials = (item) => {
  serialsModalItem.value = item;
  showSerialsModal.value = true;
};

const filteredItems = computed(() => {
  const q = search.value.trim().toLowerCase();
  if (!q) return items.value;
  return items.value.filter((i) => (i[props.nameField] || '').toLowerCase().includes(q));
});
const { currentPage, totalPages, pagedItems: pagedRows, pageSize } = usePagination(filteredItems);

const showModal = ref(false);
const editingId = ref(null);
const formError = ref("");
const formValue = ref("");
const saving = ref(false);

const newSerials = ref(['']);
const addSerialRow = () => newSerials.value.push('');
const removeSerialRow = (idx) => {
  if (newSerials.value.length > 1) newSerials.value.splice(idx, 1);
  else newSerials.value[idx] = '';
};
// Nhập hàng loạt từ file — .xlsx/.xls đọc qua thư viện xlsx, .csv/.txt đọc thẳng dạng
// text (mỗi serial 1 dòng hoặc cách nhau bằng dấu phẩy) — tái dùng đúng cơ chế của
// InventoryPanel.vue's importSerialsFromFile.
const importSerialsFromFile = async (e) => {
  const file = e.target.files?.[0];
  if (!file) return;
  const ext = file.name.split('.').pop()?.toLowerCase();
  let parsed;
  if (ext === 'xlsx' || ext === 'xls') {
    const buf = await file.arrayBuffer();
    const wb = XLSX.read(buf, { type: 'array' });
    const rows = XLSX.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]], { header: 1 });
    parsed = rows.flat().map((v) => String(v ?? '').trim()).filter(Boolean);
  } else {
    const text = await file.text();
    parsed = text.split(/[\n,]+/).map((s) => s.trim()).filter(Boolean);
  }
  const existing = newSerials.value.filter(Boolean);
  newSerials.value = [...existing, ...parsed].length ? [...existing, ...parsed] : [''];
  e.target.value = '';
};

const openAdd = () => {
  editingId.value = null;
  formValue.value = "";
  formError.value = "";
  newSerials.value = [''];
  showModal.value = true;
};
const openEdit = (item) => {
  editingId.value = item[props.idField];
  formValue.value = item[props.nameField];
  formError.value = "";
  showModal.value = true;
};

const saveItem = async () => {
  formError.value = "";
  if (!formValue.value.trim()) {
    formError.value = t('admin.dmCategory.nameRequired', { label: props.nameLabel });
    return;
  }
  const serials = newSerials.value.map((s) => s.trim()).filter(Boolean);
  if (!editingId.value && serials.length === 0) {
    formError.value = t('admin.dmCategory.serialRequired', { label: props.nameLabel });
    return;
  }
  if (saving.value) return;
  saving.value = true;
  try {
    const body = { [props.nameField]: formValue.value.trim() };
    const res = await props.service.save(editingId.value, body);
    if (!res.ok) {
      formError.value = t('admin.errors.saveFailed', { status: res.status, text: await res.text() });
      return;
    }
    if (!editingId.value) {
      const created = await res.json();
      const newId = created[props.idField];
      for (const soSerial of serials) {
        const sres = await props.serialService.create({
          [props.serialFieldName]: newId,
          soSerial,
          trangThai: 'trong_kho',
          ngayNhapKho: nowLocalIso(),
        });
        if (!sres.ok) {
          showToast(await sres.text().catch(() => t('admin.errors.addSerialError')));
          return;
        }
      }
    }
    showModal.value = false;
    await load();
  } catch (e) {
    formError.value = e.message;
  } finally {
    saving.value = false;
  }
};

</script>

<template>
  <div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
    <span class="text-secondary small">{{ filteredItems.length }}/{{ items.length }} {{ t('admin.dmCategory.countSuffix', { label }) }}</span>
    <div class="d-flex gap-2 flex-wrap">
      <input v-model="search" class="form-control form-control-sm" style="width:220px;background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);" :placeholder="t('admin.dmCategory.searchPlaceholder', { label })" />
      <button class="btn btn-sm btn-warning text-dark fw-bold" @click="openAdd">{{ t('admin.dmCategory.add', { label }) }}</button>
    </div>
  </div>

  <div v-if="loading" class="text-secondary small">{{ t('admin.dmCategory.loading') }}</div>
  <div v-else class="table-responsive">
    <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
      <thead>
        <tr>
          <th style="width:40px;">{{ t('admin.common.stt') }}</th>
          <th>{{ nameLabel }}</th>
          <th style="width:140px;">{{ t('admin.dmCategory.colAction') }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(item, idx) in pagedRows" :key="item[idField]">
          <td class="text-secondary">{{ currentPage * pageSize + idx + 1 }}</td>
          <td>{{ item[nameField] }}</td>
          <td>
            <div class="d-flex gap-1">
              <button class="btn btn-sm btn-outline-info" style="font-size:0.78rem;padding:2px 8px;" @click="openSerials(item)"><Hash :size="12" style="vertical-align:-2px;" /> {{ t('admin.dmCategory.viewSerials', { count: stockCountOf(item) }) }}</button>
              <button class="btn btn-sm btn-outline-warning" style="font-size:0.78rem;padding:2px 8px;" @click="openEdit(item)">{{ t('admin.dmCategory.edit') }}</button>
            </div>
          </td>
        </tr>
        <tr v-if="filteredItems.length===0"><td colspan="3" class="text-center text-secondary">{{ t('admin.dmCategory.empty', { label }) }}</td></tr>
      </tbody>
    </table>
    <Pagination :current-page="currentPage" :total-pages="totalPages" @page-change="currentPage = $event" />
  </div>

  <div v-if="showModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="showModal=false">
    <div class="rounded-3 p-3" style="background:var(--bg-card);width:400px;max-width:94vw;">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <div class="fw-bold" style="color:var(--text-heading);">{{ editingId ? t('admin.dmCategory.titleEdit', { label }) : t('admin.dmCategory.titleAdd', { label }) }}</div>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="showModal=false"></button>
      </div>
      <div v-if="formError" class="alert alert-danger small py-2 mb-2">{{ formError }}</div>
      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ nameLabel }}</label>
        <input v-model="formValue" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" @keyup.enter="saveItem" />
      </div>
      <div v-if="!editingId" class="mb-3">
        <div class="d-flex justify-content-between align-items-center mb-1">
          <label class="form-label small text-secondary mb-0">{{ t('admin.stockModal.newSerialsLabel') }}</label>
          <label class="btn btn-sm btn-outline-info" style="padding:2px 10px;font-size:0.72rem;cursor:pointer;">
            <FolderOpen :size="14" style="vertical-align:-2px;" /> {{ t('admin.stockModal.importFromFile') }}
            <input type="file" accept=".csv,.txt,.xlsx,.xls" class="d-none" @change="importSerialsFromFile" />
          </label>
        </div>
        <div class="d-flex flex-column gap-2">
          <div v-for="(s, idx) in newSerials" :key="idx" class="d-flex gap-2 align-items-center">
            <input v-model="newSerials[idx]" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" :placeholder="t('admin.stockModal.serialPlaceholder')" />
            <button class="btn btn-sm btn-outline-danger" style="padding:2px 8px;" :aria-label="t('common.remove')" @click="removeSerialRow(idx)"><X :size="14" /></button>
          </div>
        </div>
        <button class="btn btn-sm btn-outline-warning mt-2" @click="addSerialRow">{{ t('admin.stockModal.addSerialRow') }}</button>
        <div class="text-secondary mt-1" style="font-size:0.72rem;">{{ t('admin.stockModal.importHint') }}</div>
      </div>
      <div class="d-flex justify-content-end gap-2">
        <button class="btn btn-sm btn-outline-secondary" @click="showModal=false">{{ t('admin.dmCategory.cancel') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" :disabled="saving" @click="saveItem">{{ t('admin.dmCategory.save') }}</button>
      </div>
    </div>
  </div>

  <!-- Modal xem danh sach serial cua 1 muc danh muc (CPU/RAM/GPU/O cung) — thuan xem,
       sua/xoa tung serial van lam o tab Serial chung (SerialManager.vue) -->
  <div v-if="showSerialsModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="showSerialsModal=false">
    <div class="rounded-3 p-3 d-flex flex-column" style="background:var(--bg-card);width:420px;max-width:94vw;max-height:80vh;">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <div class="fw-bold" style="color:var(--text-heading);">{{ t('admin.dmCategory.serialsModalTitle', { name: serialsModalItem?.[nameField] }) }}</div>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="showSerialsModal=false"></button>
      </div>
      <div class="overflow-y-auto d-flex flex-column gap-1">
        <div v-if="serialsOf(serialsModalItem).length===0" class="text-secondary small text-center py-3">{{ t('admin.serialManager.empty') }}</div>
        <div v-for="s in serialsOf(serialsModalItem)" :key="s.soSerial"
             class="d-flex justify-content-between align-items-center p-2 rounded-2" style="background:var(--bg-hover);font-size:0.82rem;">
          <span class="font-monospace">{{ s.soSerial }}</span>
          <span class="text-secondary">{{ t(`admin.statusLabel.${s.trangThai}`) }}</span>
          <span class="text-secondary" style="font-size:0.72rem;">{{ formatDate(s.ngayNhapKho) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>
