<script setup>
import { ref, computed, onMounted } from "vue";
import { t } from "../../i18n/index.js";
import { statusLabel } from "../../utils/adminFormat.js";
import { CustomersStore, ensureCustomers } from "../../stores/customers.js";
import CustomerFormModal from "./CustomerFormModal.vue";
import CustomerDetailModal from "./CustomerDetailModal.vue";
import Pagination from "../common/Pagination.vue";
import { usePagination } from "../../composables/usePagination.js";

const emit = defineEmits(["view-detail", "view-order"]);

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
const { currentPage, totalPages, pagedItems: pagedCustomers } = usePagination(filteredCustomers);

const customerFormModalRef = ref(null);
const showCustomerForm = ref(false);

// ── Avatar helpers ────────────────────────────────────────────────────────────
const getAvatarUrl = (c) => c?.hinhAnh || c?.avatarUrl || null;
const getInitials = (c) => {
  const name = c?.hoTen || 'K';
  const parts = name.trim().split(' ');
  if (parts.length >= 2) return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  return name.substring(0, 2).toUpperCase();
};
const getAvatarBgColor = (c) => {
  const colors = [
    '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7',
    '#DDA0DD', '#98D8C8', '#F7DC6F', '#BB8FCE', '#85C1E9'
  ];
  const name = c?.hoTen || '';
  let hash = 0;
  for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash);
  return colors[Math.abs(hash) % colors.length];
};
const getStatusClass = (c) => c.trangThai === 'active' ? 'customer-status active' : 'customer-status inactive';

// ── Modal chi tiet khach hang (3 tabs) ────────────────────────────────────────
const showDetailModal = ref(false);
const selectedCustomer = ref(null);

const openDetailModal = (customer) => {
  selectedCustomer.value = customer;
  showDetailModal.value = true;
};
const closeDetailModal = () => {
  showDetailModal.value = false;
  selectedCustomer.value = null;
};
</script>

<template>
  <div class="alt-card customer-card-board">
    <div class="alt-toolbar">
      <span class="alt-toolbar__count">{{ filteredCustomers.length }}/{{ (CustomersStore.items ?? []).length }} {{ t('admin.customers.countSuffix') }}</span>
      <div class="alt-toolbar__actions">
        <div class="alt-search">
          <i class="fa fa-search alt-search__icon"></i>
          <input v-model="customerSearch" :placeholder="t('admin.customers.searchPlaceholder')" />
        </div>
        <button class="alt-btn alt-btn--primary" @click="showCustomerForm = true; customerFormModalRef?.openForCreate()">{{ t('admin.customers.add') }}</button>
      </div>
    </div>

    <div v-if="CustomersStore.loading" class="alt-empty">{{ t('admin.customers.loading') }}</div>
    <div v-else class="customer-grid-wrap">
      <div v-if="pagedCustomers.length === 0" class="alt-empty">{{ t('admin.customers.empty') }}</div>
      <div v-else class="customer-card-grid">
        <div
          v-for="c in pagedCustomers"
          :key="c.khachHangId"
          class="customer-card"
          @click="openDetailModal(c)"
        >
          <div class="customer-card__avatar-wrap">
            <div v-if="getAvatarUrl(c)" class="customer-card__avatar customer-card__avatar--img">
              <img :src="getAvatarUrl(c)" :alt="c.hoTen || 'Khách hàng'" />
            </div>
            <div v-else class="customer-card__avatar customer-card__avatar--initials" :style="{ background: getAvatarBgColor(c) }">
              {{ getInitials(c) }}
            </div>
            <span class="customer-card__status" :class="getStatusClass(c)">{{ statusLabel(c.trangThai) }}</span>
          </div>
          <div class="customer-card__body">
            <h4 class="customer-card__name">{{ c.hoTen || 'Khách hàng' }}</h4>
            <div class="customer-card__phone">
              <i class="fa fa-phone"></i>
              {{ c.soDienThoai || '—' }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="totalPages > 1" class="customer-pagination">
      <Pagination :current-page="currentPage" :total-pages="totalPages" />
    </div>

    <CustomerFormModal ref="customerFormModalRef" v-model="showCustomerForm" />
    <CustomerDetailModal
      v-if="showDetailModal && selectedCustomer"
      :customer="selectedCustomer"
      @close="closeDetailModal"
      @view-order="(o) => { closeDetailModal(); emit('view-order', o); }"
    />
  </div>
</template>

<style scoped>
/* ─── Card Grid Layout ─── */
.customer-grid-wrap { padding: 16px; }
.customer-card-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;
}

/* ─── Customer Card 3D Button Style ─── */
.customer-card {
  position: relative;
  background: linear-gradient(180deg, var(--pink-100) 0%, var(--pink-200) 100%) !important;
  border: none;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 4px 0 var(--pink-700), 0 6px 12px rgba(168, 27, 93, 0.2);
  border-bottom: 4px solid var(--pink-700) !important;
  transition: all 0.15s ease;
}
.customer-card:hover {
  background: linear-gradient(180deg, var(--pink-50) 0%, var(--pink-100) 100%) !important;
  transform: translateY(-2px);
  box-shadow: 0 6px 0 var(--pink-700), 0 10px 20px rgba(168, 27, 93, 0.25);
  border-bottom-color: var(--pink-600) !important;
}
.customer-card:active {
  transform: translateY(2px);
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.15);
  border-bottom-width: 0 !important;
  transition: all 0.05s ease;
}

/* ─── Avatar ─── */
.customer-card__avatar-wrap {
  position: relative;
  padding: 16px 16px 0;
  display: flex;
  justify-content: center;
}
.customer-card__avatar {
  width: 75px;
  height: 75px;
  border-radius: 50%;
  border: 3px solid #fff;
  box-shadow: 0 3px 8px rgba(168, 27, 93, 0.25);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  text-shadow: 0 1px 2px rgba(0,0,0,0.2);
}
.customer-card__avatar--img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.customer-card__status {
  position: absolute;
  bottom: -8px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 9px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 600;
  white-space: nowrap;
  z-index: 1;
}
.customer-card__status.active {
  background: var(--success);
  color: #fff;
  box-shadow: 0 2px 4px rgba(5, 150, 105, 0.3);
}
.customer-card__status.inactive {
  background: var(--danger);
  color: #fff;
  box-shadow: 0 2px 4px rgba(220, 38, 38, 0.3);
}

/* ─── Card Body ─── */
.customer-card__body {
  padding: 12px 12px 16px;
  text-align: center;
}
.customer-card__name {
  font-size: 13px;
  font-weight: 700;
  color: var(--ink);
  margin: 0 0 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.customer-card__phone {
  font-size: 12px;
  color: var(--pink-700);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-weight: 600;
}
.customer-card__phone i { font-size: 11px; }

/* ─── Pagination ─── */
.customer-pagination {
  display: flex;
  justify-content: center;
  padding: 16px;
  border-top: 1px solid var(--pink-50);
}

/* ─── Responsive ─── */
@media (max-width: 1400px) { .customer-card-grid { grid-template-columns: repeat(4, 1fr); } }
@media (max-width: 1100px) { .customer-card-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 800px)  { .customer-card-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 500px)  { .customer-card-grid { grid-template-columns: 1fr; } }
</style>
