<script setup>
import { ref, computed, onMounted } from "vue";
import { t } from "../../i18n/index.js";
import * as KhachHangService from "../../services/KhachHangService.js";
import { statusLabel } from "../../utils/adminFormat.js";
import { showToast } from "../../stores/toast.js";
import { askConfirm } from "../../stores/confirm.js";
import { CustomersStore, ensureCustomers, refreshCustomers } from "../../stores/customers.js";
import CustomerFormModal from "./CustomerFormModal.vue";
import Pagination from "../common/Pagination.vue";
import { usePagination } from "../../composables/usePagination.js";

const emit = defineEmits(["view-detail"]);

onMounted(() => { ensureCustomers(); });

// ── Bo loc man hinh Khach hang ────────────────────────────────────────────────
const customerSearch = ref("");
const filteredCustomers = computed(() => {
  const q = customerSearch.value.trim().toLowerCase();
  const all = CustomersStore.items ?? [];
  if (!q) return all;
  return all.filter((c) =>
    (c.hoTen ?? '').toLowerCase().includes(q) ||
    (c.soDienThoai ?? '').includes(q) ||
    (c.email ?? '').toLowerCase().includes(q)
  );
});
const { currentPage, totalPages, pagedItems: pagedCustomers, pageSize } = usePagination(filteredCustomers);

const showCustomerModal = ref(false);
const customerModalRef = ref(null);
</script>

<template>
  <div class="alt-card">
    <div class="alt-toolbar">
      <span class="alt-toolbar__count">{{ filteredCustomers.length }}/{{ (CustomersStore.items ?? []).length }} {{ t('admin.customers.countSuffix') }}</span>
      <div class="alt-toolbar__actions">
        <div class="alt-search">
          <i class="fa fa-search alt-search__icon"></i>
          <input v-model="customerSearch" :placeholder="t('admin.customers.searchPlaceholder')" />
        </div>
        <button class="alt-btn alt-btn--primary" @click="customerModalRef.openForCreate()">{{ t('admin.customers.add') }}</button>
      </div>
    </div>
    <div v-if="CustomersStore.loading" class="alt-empty">{{ t('admin.customers.loading') }}</div>
    <div v-else class="alt-table-wrap">
      <table class="alt-table">
        <thead><tr><th style="width:40px;">{{ t('admin.common.stt') }}</th><th>{{ t('admin.customers.colFullName') }}</th><th>{{ t('admin.customers.colPhone') }}</th><th>{{ t('admin.customers.colEmail') }}</th><th>{{ t('admin.customers.colCustomerType') }}</th><th>{{ t('admin.customers.colPoints') }}</th><th>{{ t('admin.customers.colStatus') }}</th><th>{{ t('admin.customers.colAction') }}</th></tr></thead>
        <tbody>
          <tr v-for="(c, idx) in pagedCustomers" :key="c.khachHangId">
            <td class="text-secondary">{{ currentPage * pageSize + idx + 1 }}</td>
            <td>{{ c.hoTen }}</td>
            <td class="text-secondary">{{ c.soDienThoai }}</td>
            <td class="text-secondary">{{ c.email }}</td>
            <td>{{ c.loaiKhach||'—' }}</td>
            <td>{{ c.diemTichLuy??0 }}</td>
            <td>
              <span class="alt-tag" :style="c.trangThai==='active' ? 'background:rgba(22,163,74,0.14);color:var(--state-success);' : 'background:var(--bg-card-alt);color:var(--text-secondary);'">{{ statusLabel(c.trangThai) }}</span>
            </td>
            <td>
              <div class="d-flex gap-1">
                <button class="alt-btn alt-btn--ghost" style="padding:4px 12px;" @click="emit('view-detail', c.khachHangId)">{{ t('admin.customers.viewDetail') }}</button>
                <button class="alt-btn alt-btn--ghost" style="padding:4px 12px;" @click="customerModalRef.openForEdit(c)">{{ t('admin.customers.edit') }}</button>
              </div>
            </td>
          </tr>
          <tr v-if="filteredCustomers.length===0"><td colspan="8" class="alt-empty">{{ t('admin.customers.empty') }}</td></tr>
        </tbody>
      </table>
      <div v-if="totalPages > 1" class="alt-pager"><Pagination :current-page="currentPage" :total-pages="totalPages" @page-change="currentPage = $event" /></div>
    </div>
  </div>

  <CustomerFormModal ref="customerModalRef" v-model="showCustomerModal" />
</template>
