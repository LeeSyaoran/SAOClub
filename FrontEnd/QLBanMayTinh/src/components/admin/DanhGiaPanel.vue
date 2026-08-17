<script setup>
import { ref, computed, onMounted } from "vue";
import { t } from "../../i18n/index.js";
import * as DanhGiaService from "../../services/DanhGiaService.js";
import { formatDateTime } from "../../utils/adminFormat.js";
import { showToast } from "../../stores/toast.js";
import { askConfirm } from "../../stores/confirm.js";
import Pagination from "../common/Pagination.vue";
import { usePagination } from "../../composables/usePagination.js";
import { Star } from '@lucide/vue';

const items = ref([]);
const loading = ref(false);

const fetchReviews = async () => {
  loading.value = true;
  try {
    items.value = await DanhGiaService.getAllAdmin().catch(() => []);
  } finally {
    loading.value = false;
  }
};
onMounted(fetchReviews);

const search = ref("");
const filteredItems = computed(() => {
  const q = search.value.trim().toLowerCase();
  if (!q) return items.value;
  return items.value.filter((d) =>
    [d.tenSanPham, d.tenKhachHang, d.noiDung].some((v) => (v || "").toLowerCase().includes(q)));
});
const { currentPage, totalPages, pagedItems: pagedReviews, pageSize } = usePagination(filteredItems, 20);

const deleteReview = async (d) => {
  if (!(await askConfirm(t('admin.confirm.deleteReview')))) return;
  const res = await DanhGiaService.removeAdmin(d.danhGiaId);
  if (!res.ok) { showToast(await res.text().catch(() => t('admin.errors.deleteFailed', { status: res.status }))); return; }
  items.value = items.value.filter((x) => x.danhGiaId !== d.danhGiaId);
};
</script>

<template>
  <div class="alt-card">
    <div class="alt-toolbar">
      <span class="alt-toolbar__count">{{ filteredItems.length }}/{{ items.length }} {{ t('admin.reviews.countSuffix') }}</span>
      <div class="alt-toolbar__actions">
        <div class="alt-search">
          <i class="fa fa-search alt-search__icon"></i>
          <input v-model="search" :placeholder="t('admin.reviews.searchPlaceholder')" />
        </div>
      </div>
    </div>
    <div v-if="loading" class="alt-empty">{{ t('admin.reviews.loading') }}</div>
    <div v-else class="alt-table-wrap">
      <table class="alt-table">
        <thead>
          <tr>
            <th style="width:40px;">{{ t('admin.common.stt') }}</th>
            <th>{{ t('admin.reviews.colDate') }}</th>
            <th>{{ t('admin.reviews.colProduct') }}</th>
            <th>{{ t('admin.reviews.colCustomer') }}</th>
            <th>{{ t('admin.reviews.colStars') }}</th>
            <th>{{ t('admin.reviews.colContent') }}</th>
            <th style="width:70px;"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(d, idx) in pagedReviews" :key="d.danhGiaId">
            <td class="text-secondary">{{ currentPage * pageSize + idx + 1 }}</td>
            <td class="text-secondary">{{ formatDateTime(d.ngayDanhGia) }}</td>
            <td>{{ d.tenSanPham }}</td>
            <td>{{ d.tenKhachHang }}</td>
            <td><span class="d-inline-flex" style="gap:1px;color:var(--accent-fg);"><Star v-for="n in d.soSao" :key="n" :size="12" fill="currentColor" /></span></td>
            <td class="text-secondary">{{ d.noiDung || '—' }}</td>
            <td>
              <button class="alt-btn alt-btn--ghost" style="padding:4px 12px;color:var(--state-danger);border-color:var(--state-danger);" @click="deleteReview(d)">{{ t('admin.common.delete') }}</button>
            </td>
          </tr>
          <tr v-if="filteredItems.length===0"><td colspan="7" class="alt-empty">{{ t('admin.reviews.empty') }}</td></tr>
        </tbody>
      </table>
      <div v-if="totalPages > 1" class="alt-pager"><Pagination :current-page="currentPage" :total-pages="totalPages" @page-change="currentPage = $event" /></div>
    </div>
  </div>
</template>
