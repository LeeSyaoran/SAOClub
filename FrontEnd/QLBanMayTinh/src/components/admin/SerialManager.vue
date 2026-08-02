<script setup>
import { ref, reactive, computed, onMounted } from "vue";
import { t } from "../../i18n/index.js";
import * as ChiTietSanPhamService from "../../services/ChiTietSanPhamService.js";
import { ChiTietCpuService, ChiTietRamService, ChiTietGpuService, ChiTietOCungService } from "../../services/ChiTietLinhKienService.js";
import * as DmService from "../../services/DmService.js";
import { formatDate } from "../../utils/adminFormat.js";
import { nowLocalIso } from "../../utils/datetime.js";
import { showToast } from "../../stores/toast.js";
import { askConfirm } from "../../stores/confirm.js";
import { ProductsStore, ensureProducts } from "../../stores/products.js";
import SearchSelect from "../common/SearchSelect.vue";
import ProductDetailModal from "./ProductDetailModal.vue";
import Pagination from "../common/Pagination.vue";
import { usePagination } from "../../composables/usePagination.js";

const specLists = reactive({ cpu: [], ram: [], gpu: [], oCung: [] });
const LINH_KIEN_META = {
  cpu:   { idField: 'cpuId',   nameField: 'tenCpu',    itemIdField: 'chiTietCpuId',   service: ChiTietCpuService },
  ram:   { idField: 'ramId',   nameField: 'dungLuong', itemIdField: 'chiTietRamId',   service: ChiTietRamService },
  gpu:   { idField: 'gpuId',   nameField: 'tenGpu',    itemIdField: 'chiTietGpuId',   service: ChiTietGpuService },
  oCung: { idField: 'oCungId', nameField: 'loaiOcung', itemIdField: 'chiTietOCungId', service: ChiTietOCungService },
};

const items = ref([]);
const loading = ref(false);
const search = ref("");

const load = async () => {
  loading.value = true;
  try {
    const [sp, cpu, ram, gpu, oCung] = await Promise.all([
      ChiTietSanPhamService.getAll().catch(() => []),
      ChiTietCpuService.getAll().catch(() => []),
      ChiTietRamService.getAll().catch(() => []),
      ChiTietGpuService.getAll().catch(() => []),
      ChiTietOCungService.getAll().catch(() => []),
    ]);
    items.value = [
      ...(sp ?? []).map((i) => ({ ...i, loai: 'sanPham', rowId: i.chiTietId })),
      ...(cpu ?? []).map((i) => ({ ...i, loai: 'cpu', rowId: i.chiTietCpuId })),
      ...(ram ?? []).map((i) => ({ ...i, loai: 'ram', rowId: i.chiTietRamId })),
      ...(gpu ?? []).map((i) => ({ ...i, loai: 'gpu', rowId: i.chiTietGpuId })),
      ...(oCung ?? []).map((i) => ({ ...i, loai: 'oCung', rowId: i.chiTietOCungId })),
    ];
  } finally {
    loading.value = false;
  }
};
onMounted(() => {
  load();
  ensureProducts();
  DmService.getCpu().then((l) => { specLists.cpu = l; }).catch(() => {});
  DmService.getRam().then((l) => { specLists.ram = l; }).catch(() => {});
  DmService.getGpu().then((l) => { specLists.gpu = l; }).catch(() => {});
  DmService.getOCung().then((l) => { specLists.oCung = l; }).catch(() => {});
});

const variantOptions = computed(() =>
  (ProductsStore.items ?? []).map((p) => ({ value: p.bienTheId, label: `${p.tenSanPham} — ${p.maSku}` }))
);
const variantLabel = (bienTheId) => variantOptions.value.find((o) => o.value === bienTheId)?.label ?? '';
const findVariant = (bienTheId) => (ProductsStore.items ?? []).find((p) => p.bienTheId === bienTheId);

// Modal "Chi tiet san pham" xem-thuan cho dong loai "sanPham" — chi hien DUNG bien the cua
// serial dang xem (onlyBienTheIds), khong phai ca ho bien the cua san pham do. Dong loai
// linh kien (cpu/ram/gpu/oCung) khong co nut nay — khong co "san pham"/bien the de xem.
const showDetailModal = ref(false);
const detailSanPhamId = ref(null);
const detailSanPhamName = ref('');
const detailOnlyBienTheIds = ref(null);
const openDetail = (item) => {
  const variant = findVariant(item.bienTheId);
  if (!variant) return;
  detailSanPhamId.value = variant.sanPhamId;
  detailSanPhamName.value = variant.tenSanPham;
  detailOnlyBienTheIds.value = [item.bienTheId];
  showDetailModal.value = true;
};

// Nhãn hiển thị cột "Sản phẩm/SKU" cho MỌI loại dòng (sản phẩm lẫn linh kiện) —
// linh kiện đã có sẵn tên spec (tenCpu/dungLuong/...) ngay trong response, không cần
// tra cứu thêm.
const rowSpecLabel = (item) => {
  if (item.loai === 'sanPham') return variantLabel(item.bienTheId) || item.maSku;
  const meta = LINH_KIEN_META[item.loai];
  return meta ? item[meta.nameField] : '';
};

const filteredItems = computed(() => {
  const q = search.value.trim().toLowerCase();
  if (!q) return items.value;
  return items.value.filter((i) =>
    [i.soSerial, rowSpecLabel(i)].some((v) => (v || '').toLowerCase().includes(q))
  );
});
const { currentPage, totalPages, pagedItems, pageSize } = usePagination(filteredItems);

const STATUS_COLOR = {
  trong_kho: '#22c55e',
  giu_hang: '#facc15',
  da_ban: '#94a3b8',
  loi_bao_hanh: '#fb923c',
  da_tra_hang: '#38bdf8',
  da_su_dung: '#a78bfa',
};
const statusColor = (s) => STATUS_COLOR[s] ?? '#6b7280';
const statusLabel = (s) => t(`admin.statusLabel.${s}`);

const showModal = ref(false);
const editingId = ref(null);
const formError = ref("");
const saving = ref(false);
const emptyForm = () => ({
  loai: 'sanPham',
  specId: '',
  soSerial: '',
  trangThai: 'trong_kho',
  ngayNhapKho: nowLocalIso().slice(0, 16),
  ghiChu: '',
});
const form = ref(emptyForm());
const STATUS_OPTIONS_SAN_PHAM = ['trong_kho', 'giu_hang', 'da_ban', 'loi_bao_hanh', 'da_tra_hang'];
const STATUS_OPTIONS_LINH_KIEN = ['trong_kho', 'da_su_dung', 'loi_bao_hanh'];
const statusOptions = computed(() =>
  form.value.loai === 'sanPham' ? STATUS_OPTIONS_SAN_PHAM : STATUS_OPTIONS_LINH_KIEN
);
// Đổi Loại (người dùng bấm chọn trong modal) → trạng thái/spec cũ có thể không hợp lệ
// với loại mới, reset về mặc định. Gắn vào @change của <select> (xem Step 8), KHÔNG
// dùng watch(() => form.value.loai) — watch sẽ fire cả lúc openEdit() gán nguyên object
// form mới (loai đổi từ giá trị cũ sang item.loai), xoá mất specId/trangThai vừa set.
const onLoaiChange = () => {
  form.value.trangThai = 'trong_kho';
  form.value.specId = '';
};

const specOptions = computed(() => {
  if (form.value.loai === 'sanPham') return variantOptions.value;
  const meta = LINH_KIEN_META[form.value.loai];
  if (!meta) return [];
  return specLists[form.value.loai].map((s) => ({ value: s[meta.idField], label: s[meta.nameField] }));
});

const openAdd = () => {
  editingId.value = null;
  form.value = emptyForm();
  formError.value = "";
  showModal.value = true;
};
const openEdit = (item) => {
  editingId.value = item.rowId;
  const specId = item.loai === 'sanPham' ? item.bienTheId : item[LINH_KIEN_META[item.loai].idField];
  form.value = {
    loai: item.loai,
    specId,
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
  if (!form.value.specId) {
    formError.value = t(form.value.loai === 'sanPham' ? 'admin.serialManager.variantRequired' : 'admin.serialManager.specRequired');
    return;
  }
  if (!form.value.soSerial.trim()) { formError.value = t('admin.serialManager.serialRequired'); return; }
  if (saving.value) return;
  saving.value = true;
  try {
    const common = {
      soSerial: form.value.soSerial.trim(),
      trangThai: form.value.trangThai,
      ngayNhapKho: nowLocalIso(new Date(form.value.ngayNhapKho)),
      ghiChu: form.value.ghiChu || null,
    };
    let res;
    if (form.value.loai === 'sanPham') {
      const body = { bienTheId: Number(form.value.specId), ...common };
      res = editingId.value
        ? await ChiTietSanPhamService.update(editingId.value, body)
        : await ChiTietSanPhamService.create(body);
    } else {
      const meta = LINH_KIEN_META[form.value.loai];
      const body = { [meta.idField]: Number(form.value.specId), ...common };
      res = editingId.value
        ? await meta.service.update(editingId.value, body)
        : await meta.service.create(body);
    }
    if (!res.ok) {
      formError.value = t('admin.errors.saveFailed', { status: res.status, text: await res.text() });
      return;
    }
    showModal.value = false;
    await load();
  } catch (e) {
    formError.value = e.message;
  } finally {
    saving.value = false;
  }
};

const deleteSerial = async (item) => {
  if (!(await askConfirm(t('admin.confirm.deleteSerial')))) return;
  const service = item.loai === 'sanPham' ? ChiTietSanPhamService : LINH_KIEN_META[item.loai].service;
  const res = await service.remove(item.rowId);
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
          <th>{{ t('admin.serialManager.colLoai') }}</th>
          <th>{{ t('admin.serialManager.colVariant') }}</th>
          <th>{{ t('admin.serialManager.colSerial') }}</th>
          <th>{{ t('admin.serialManager.colStatus') }}</th>
          <th>{{ t('admin.serialManager.colDate') }}</th>
          <th>{{ t('admin.serialManager.colNote') }}</th>
          <th style="width:140px;">{{ t('admin.serialManager.colAction') }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(item, idx) in pagedItems" :key="`${item.loai}-${item.rowId}`">
          <td class="text-secondary">{{ currentPage * pageSize + idx + 1 }}</td>
          <td>{{ t(`admin.productsTabs.${item.loai}`) }}</td>
          <td>{{ rowSpecLabel(item) }}</td>
          <td>{{ item.soSerial }}</td>
          <td>
            <span style="display:inline-block;width:9px;height:9px;border-radius:50%;margin-right:6px;" :style="{ background: statusColor(item.trangThai) }"></span>
            {{ statusLabel(item.trangThai) }}
          </td>
          <td class="text-secondary">{{ formatDate(item.ngayNhapKho) }}</td>
          <td class="text-secondary">{{ item.ghiChu }}</td>
          <td>
            <div class="d-flex gap-1">
              <button v-if="item.loai === 'sanPham'" class="btn btn-sm btn-outline-secondary" style="font-size:0.78rem;padding:2px 8px;" @click="openDetail(item)">{{ t('admin.products.detail') }}</button>
              <button class="btn btn-sm btn-outline-warning" style="font-size:0.78rem;padding:2px 8px;" @click="openEdit(item)">{{ t('admin.serialManager.edit') }}</button>
              <button v-if="item.trangThai === 'trong_kho'" class="btn btn-sm btn-outline-danger" style="font-size:0.78rem;padding:2px 8px;" @click="deleteSerial(item)">{{ t('admin.serialManager.delete') }}</button>
            </div>
          </td>
        </tr>
        <tr v-if="filteredItems.length===0"><td colspan="8" class="text-center text-secondary">{{ t('admin.serialManager.empty') }}</td></tr>
      </tbody>
    </table>
    <Pagination :current-page="currentPage" :total-pages="totalPages" @page-change="currentPage = $event" />
  </div>

  <div v-if="showModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="showModal=false">
    <div class="rounded-3 p-3" style="background:var(--bg-card);width:420px;max-width:94vw;">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <div class="fw-bold" style="color:var(--text-heading);">{{ editingId ? t('admin.serialManager.titleEdit') : t('admin.serialManager.titleAdd') }}</div>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="showModal=false"></button>
      </div>
      <div v-if="formError" class="alert alert-danger small py-2 mb-2">{{ formError }}</div>

      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ t('admin.serialManager.colLoai') }}</label>
        <select v-model="form.loai" class="form-select form-select-sm" :disabled="!!editingId" @change="onLoaiChange" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);">
          <option value="sanPham">{{ t('admin.productsTabs.sanPham') }}</option>
          <option value="cpu">{{ t('admin.productsTabs.cpu') }}</option>
          <option value="ram">{{ t('admin.productsTabs.ram') }}</option>
          <option value="gpu">{{ t('admin.productsTabs.gpu') }}</option>
          <option value="oCung">{{ t('admin.productsTabs.oCung') }}</option>
        </select>
      </div>
      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ form.loai === 'sanPham' ? t('admin.serialManager.variantLabel') : t(`admin.productsTabs.${form.loai}`) }}</label>
        <SearchSelect v-model="form.specId" :options="specOptions" :placeholder="form.loai === 'sanPham' ? t('admin.serialManager.variantPlaceholder') : t('admin.serialManager.specPlaceholder')" />
      </div>
      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ t('admin.serialManager.serialLabel') }}</label>
        <input v-model="form.soSerial" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>
      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ t('admin.serialManager.statusLabel') }}</label>
        <select v-model="form.trangThai" class="form-select form-select-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);">
          <option v-for="s in statusOptions" :key="s" :value="s">{{ t(`admin.statusLabel.${s}`) }}</option>
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
        <button class="btn btn-sm btn-warning text-dark fw-bold" :disabled="saving" @click="saveSerial">{{ t('admin.serialManager.save') }}</button>
      </div>
    </div>
  </div>

  <ProductDetailModal
    v-model="showDetailModal"
    :san-pham-id="detailSanPhamId"
    :san-pham-name="detailSanPhamName"
    :only-bien-the-ids="detailOnlyBienTheIds"
  />
</template>
