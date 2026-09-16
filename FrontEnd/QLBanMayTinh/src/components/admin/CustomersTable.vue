<script setup>
import { ref, computed, watch, onMounted, reactive } from "vue";
import { Search, Phone } from "@lucide/vue";
import { t } from "../../i18n/index.js";
import { statusLabel } from "../../utils/adminFormat.js";
import { CustomersStore, ensureCustomers } from "../../stores/customers.js";
import CustomerFormModal from "./CustomerFormModal.vue";
import CustomerDetailModal from "./CustomerDetailModal.vue";
import Pagination from "../common/Pagination.vue";
import { usePagination } from "../../composables/usePagination.js";
import { Filter, X, ChevronDown, ChevronUp } from "@lucide/vue";

const emit = defineEmits(["view-detail", "view-order"]);

onMounted(() => { ensureCustomers(); });

// ── Bộ lọc nâng cao: Khách hàng ──────────────────────────────────────────────
const customerSearch = ref("");
const isFilterOpen = ref(false);
const filters = reactive({
  trangThai: "",   // '' | 'active' | 'inactive'
  loaiKhach: "",   // '' | 'ca_nhan' | 'cong_ty'
  diemMin: "",     // số điểm tích lũy tối thiểu
  diemMax: "",     // số điểm tích lũy tối đa
});

const activeFilterCount = computed(() => {
  let count = 0;
  if (filters.trangThai) count++;
  if (filters.loaiKhach) count++;
  if (filters.diemMin !== "") count++;
  if (filters.diemMax !== "") count++;
  return count;
});

const resetFilters = () => {
  filters.trangThai = "";
  filters.loaiKhach = "";
  filters.diemMin = "";
  filters.diemMax = "";
  customerSearch.value = "";
};

const filteredCustomers = computed(() => {
  const q = customerSearch.value.trim().toLowerCase();
  const all = CustomersStore.items ?? [];
  return all.filter((c) => {
    // text search
    if (q) {
      const match =
        (c.hoTen ?? '').toLowerCase().includes(q) ||
        (c.soDienThoai ?? '').includes(q) ||
        (c.email ?? '').toLowerCase().includes(q);
      if (!match) return false;
    }
    // dropdown filters
    if (filters.trangThai && c.trangThai !== filters.trangThai) return false;
    if (filters.loaiKhach && (c.loaiKhach ?? 'ca_nhan') !== filters.loaiKhach) return false;
    // điểm tích lũy range
    const diem = Number(c.diemTichLuy ?? 0);
    if (filters.diemMin !== "" && diem < Number(filters.diemMin)) return false;
    if (filters.diemMax !== "" && diem > Number(filters.diemMax)) return false;
    return true;
  });
});
const { currentPage, totalPages, pagedItems: pagedCustomers } = usePagination(filteredCustomers);
// Reset về trang 1 mỗi khi bộ lọc thay đổi
watch([customerSearch, () => filters.trangThai, () => filters.loaiKhach, () => filters.diemMin, () => filters.diemMax], () => {
  currentPage.value = 0;
});

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
          <Search class="alt-search__icon" :size="14" />
          <input v-model="customerSearch" :placeholder="t('admin.customers.searchPlaceholder')" />
        </div>
        <!-- Nút bộ lọc nâng cao -->
        <button
          class="alt-btn alt-btn--filter"
          :class="{ 'alt-btn--filter-active': activeFilterCount > 0 || isFilterOpen }"
          @click="isFilterOpen = !isFilterOpen"
        >
          <Filter :size="14" />
          Bộ lọc
          <span v-if="activeFilterCount > 0" class="filter-badge">{{ activeFilterCount }}</span>
          <ChevronDown v-if="!isFilterOpen" :size="13" style="margin-left:2px" />
          <ChevronUp v-else :size="13" style="margin-left:2px" />
        </button>
        <button v-if="activeFilterCount > 0" class="alt-btn alt-btn--ghost-sm" @click="resetFilters" title="Xóa bộ lọc">
          <X :size="13" /> Xóa lọc
        </button>
        <button class="alt-btn alt-btn--primary" @click="showCustomerForm = true; customerFormModalRef?.openForCreate()">{{ t('admin.customers.add') }}</button>
      </div>
    </div>

    <!-- Panel lọc nâng cao (ẩn/hiện) -->
    <div v-if="isFilterOpen" class="adv-filter-panel">
      <div class="adv-filter-row">
        <div class="adv-filter-group">
          <label class="adv-filter-label">Trạng thái</label>
          <select v-model="filters.trangThai" class="adv-filter-select">
            <option value="">Tất cả</option>
            <option value="active">Đang hoạt động</option>
            <option value="inactive">Ngừng hoạt động</option>
          </select>
        </div>
        <div class="adv-filter-group">
          <label class="adv-filter-label">Loại khách</label>
          <select v-model="filters.loaiKhach" class="adv-filter-select">
            <option value="">Tất cả</option>
            <option value="ca_nhan">Cá nhân</option>
            <option value="cong_ty">Công ty</option>
          </select>
        </div>
        <div class="adv-filter-group adv-filter-group--range">
          <label class="adv-filter-label">Điểm tích lũy</label>
          <div class="adv-filter-range">
            <input v-model="filters.diemMin" type="number" min="0" placeholder="Từ" class="adv-filter-input" />
            <span class="adv-filter-sep">–</span>
            <input v-model="filters.diemMax" type="number" min="0" placeholder="Đến" class="adv-filter-input" />
          </div>
        </div>
        <button v-if="activeFilterCount > 0" class="adv-filter-reset" @click="resetFilters">
          <X :size="13" /> Xóa bộ lọc
        </button>
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
              <Phone :size="14" />
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

/* ─── Filter Button ─── */
.alt-btn--filter {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  border: 1px solid var(--border, #e2e8f0);
  border-radius: 8px;
  background: var(--surface, #fff);
  color: var(--ink, #1e293b);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
}
.alt-btn--filter:hover,
.alt-btn--filter-active {
  border-color: var(--pink-400, #f472b6);
  background: var(--pink-50, #fdf2f8);
  color: var(--pink-700, #be185d);
}
.filter-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: var(--pink-600, #db2777);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
}
.alt-btn--ghost-sm {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  border: 1px solid var(--border, #e2e8f0);
  border-radius: 8px;
  background: transparent;
  color: var(--muted, #64748b);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.alt-btn--ghost-sm:hover { background: var(--danger-light, #fee2e2); color: var(--danger, #dc2626); border-color: var(--danger, #dc2626); }

/* ─── Advanced Filter Panel ─── */
.adv-filter-panel {
  border-top: 1px solid var(--pink-100, #fce7f3);
  background: var(--pink-50, #fdf2f8);
  padding: 12px 16px;
  animation: slideDown 0.15s ease;
}
@keyframes slideDown {
  from { opacity: 0; transform: translateY(-6px); }
  to   { opacity: 1; transform: translateY(0); }
}
.adv-filter-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 12px;
}
.adv-filter-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 140px;
}
.adv-filter-group--range { min-width: 220px; }
.adv-filter-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--pink-700, #be185d);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.adv-filter-select,
.adv-filter-input {
  padding: 6px 10px;
  border: 1px solid var(--pink-200, #fbcfe8);
  border-radius: 7px;
  background: #fff;
  color: var(--ink, #1e293b);
  font-size: 13px;
  outline: none;
  transition: border-color 0.15s;
  width: 100%;
}
.adv-filter-select:focus,
.adv-filter-input:focus { border-color: var(--pink-500, #ec4899); }
.adv-filter-range {
  display: flex;
  align-items: center;
  gap: 6px;
}
.adv-filter-range .adv-filter-input { width: 90px; }
.adv-filter-sep { color: var(--muted, #94a3b8); font-size: 13px; font-weight: 600; }
.adv-filter-reset {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  border: 1px solid var(--danger, #dc2626);
  border-radius: 7px;
  background: transparent;
  color: var(--danger, #dc2626);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  align-self: flex-end;
  transition: all 0.15s;
}
.adv-filter-reset:hover { background: var(--danger, #dc2626); color: #fff; }
</style>

