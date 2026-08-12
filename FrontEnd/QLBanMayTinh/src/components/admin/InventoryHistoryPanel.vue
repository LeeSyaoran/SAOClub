<script setup>
import { ref, computed, onMounted } from "vue";
import { t } from "../../i18n/index.js";
import * as LichSuTonKhoService from "../../services/LichSuTonKhoService.js";
import { formatDateTime } from "../../utils/adminFormat.js";
import Pagination from "../common/Pagination.vue";
import { usePagination } from "../../composables/usePagination.js";

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
const { currentPage, totalPages, pagedItems: pagedHistory, pageSize } = usePagination(filteredItems);
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
      <thead>
        <tr>
          <th style="width:40px;">{{ t('admin.common.stt') }}</th>
          <th>{{ t('admin.inventoryHistory.colDate') }}</th><th>{{ t('admin.inventoryHistory.colSku') }}</th>
          <th>{{ t('admin.inventoryHistory.colType') }}</th><th>{{ t('admin.inventoryHistory.colQty') }}</th>
          <th>{{ t('admin.inventoryHistory.colNote') }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(h, idx) in pagedHistory" :key="h.lichSuId">
          <td class="text-secondary">{{ currentPage * pageSize + idx + 1 }}</td>
          <td class="text-secondary">{{ formatDateTime(h.ngayTao) }}</td>
          <td style="font-family:monospace;">{{ h.maSku }}</td>
          <td><span class="badge" :style="{ background: typeColor(h.loaiBienDong) }">{{ typeLabel(h.loaiBienDong) }}</span></td>
          <td :class="h.soLuongThayDoi >= 0 ? 'text-success' : 'text-danger'" class="fw-bold">{{ h.soLuongThayDoi >= 0 ? '+' : '' }}{{ h.soLuongThayDoi }}</td>
          <td class="text-secondary">{{ h.ghiChu || '—' }}</td>
        </tr>
        <tr v-if="filteredItems.length===0"><td colspan="6" class="text-center text-secondary">{{ t('admin.inventoryHistory.empty') }}</td></tr>
      </tbody>
    </table>
    <Pagination :current-page="currentPage" :total-pages="totalPages" @page-change="currentPage = $event" />
  </div>
</template>
