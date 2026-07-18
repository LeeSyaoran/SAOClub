<script setup>
import { ref, computed, onMounted } from "vue";
import { t } from "../../i18n/index.js";
import * as KhachHangService from "../../Service/KhachHangService.js";
import { statusLabel } from "../../utils/adminFormat.js";
import { showToast } from "../../stores/toast.js";
import { askConfirm } from "../../stores/confirm.js";
import { CustomersStore, ensureCustomers, refreshCustomers } from "../../stores/customers.js";
import CustomerFormModal from "./CustomerFormModal.vue";

onMounted(() => { ensureCustomers(); });

// ── Bo loc man hinh Khach hang ────────────────────────────────────────────────
const customerSearch = ref("");
const filteredCustomers = computed(() => {
  const q = customerSearch.value.trim().toLowerCase();
  if (!q) return CustomersStore.items;
  return CustomersStore.items.filter((c) =>
    (c.hoTen ?? '').toLowerCase().includes(q) ||
    (c.soDienThoai ?? '').includes(q) ||
    (c.email ?? '').toLowerCase().includes(q)
  );
});

const deleteCustomer = async (id) => {
  if (!(await askConfirm(t('admin.confirm.deleteCustomer')))) return;
  const res = await KhachHangService.remove(id);
  if (!res.ok) { showToast(t('admin.errors.deleteFailed', { status: res.status })); return; }
  await refreshCustomers();
};

const showCustomerModal = ref(false);
const customerModalRef = ref(null);
</script>

<template>
  <div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
    <span class="text-secondary small">{{ filteredCustomers.length }}/{{ CustomersStore.items.length }} {{ t('admin.customers.countSuffix') }}</span>
    <div class="d-flex gap-2 flex-wrap">
      <input v-model="customerSearch" class="form-control form-control-sm" style="width:240px;background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);" :placeholder="t('admin.customers.searchPlaceholder')" />
      <button class="btn btn-sm btn-warning text-dark fw-bold" @click="customerModalRef.openForCreate()">{{ t('admin.customers.add') }}</button>
    </div>
  </div>
  <div v-if="CustomersStore.loading" class="text-secondary small">{{ t('admin.customers.loading') }}</div>
  <div v-else class="table-responsive">
    <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
      <thead><tr><th style="width:40px;">{{ t('admin.common.stt') }}</th><th>{{ t('admin.customers.colFullName') }}</th><th>{{ t('admin.customers.colPhone') }}</th><th>{{ t('admin.customers.colEmail') }}</th><th>{{ t('admin.customers.colCustomerType') }}</th><th>{{ t('admin.customers.colPoints') }}</th><th>{{ t('admin.customers.colStatus') }}</th><th>{{ t('admin.customers.colAction') }}</th></tr></thead>
      <tbody>
        <tr v-for="(c, idx) in filteredCustomers" :key="c.khachHangId">
          <td class="text-secondary">{{ idx + 1 }}</td>
          <td>{{ c.hoTen }}</td>
          <td class="text-secondary">{{ c.soDienThoai }}</td>
          <td class="text-secondary">{{ c.email }}</td>
          <td>{{ c.loaiKhach||'—' }}</td>
          <td>{{ c.diemTichLuy??0 }}</td>
          <td><span class="badge" :class="c.trangThai==='active'?'bg-success':'bg-secondary'">{{ statusLabel(c.trangThai) }}</span></td>
          <td>
            <div class="d-flex gap-1">
              <button class="btn btn-sm btn-outline-warning" style="font-size:0.78rem; padding:2px 8px;" @click="customerModalRef.openForEdit(c)">{{ t('admin.customers.edit') }}</button>
              <button class="btn btn-sm btn-outline-danger"  style="font-size:0.78rem; padding:2px 8px;" @click="deleteCustomer(c.khachHangId)">{{ t('admin.customers.delete') }}</button>
            </div>
          </td>
        </tr>
        <tr v-if="filteredCustomers.length===0"><td colspan="8" class="text-center text-secondary">{{ t('admin.customers.empty') }}</td></tr>
      </tbody>
    </table>
  </div>

  <CustomerFormModal ref="customerModalRef" v-model="showCustomerModal" />
</template>
