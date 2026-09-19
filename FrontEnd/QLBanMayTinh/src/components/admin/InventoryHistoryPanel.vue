<script setup>
import { ref, computed, onMounted } from "vue";
import { Search, Filter, ChevronDown } from "@lucide/vue";
import { t } from "../../i18n/index.js";
import * as LichSuTonKhoService from "../../services/LichSuTonKhoService.js";
import * as NhanVienService from "../../services/NhanVienService.js";
import { formatDateTime } from "../../utils/adminFormat.js";
import Pagination from "../common/Pagination.vue";
import { usePagination } from "../../composables/usePagination.js";
import { History, Package, User, X } from "@lucide/vue";

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

// ── Nhan vien (cho filter) ─────────────────────────────────────────────────
const nhanVienList = ref([]);
const fetchNhanVien = async () => {
  nhanVienList.value = await NhanVienService.getAll().catch(() => []);
};
onMounted(fetchNhanVien);

// ── Loai bien dong metadata ─────────────────────────────────────────────────
const LOAI_BIEN_DONG_META = {
  nhap:       { label: () => t("admin.inventoryHistory.typeNhap"),      color: "#48c78e" },
  xuat_ban:   { label: () => t("admin.inventoryHistory.typeXuatBan"),  color: "#e05252" },
  tra_hang:   { label: () => t("admin.inventoryHistory.typeTraHang"),  color: "#3e8ed0" },
  dieu_chinh: { label: () => t("admin.inventoryHistory.typeDieuChinh"), color: "#ffb703" },
  huy:        { label: () => t("admin.inventoryHistory.typeHuy"),      color: "#6c757d" },
  giu_hang:   { label: () => t("admin.inventoryHistory.typeGiuHang"),  color: "#8a63d2" },
};
const typeLabel = (loai) => LOAI_BIEN_DONG_META[loai]?.label() ?? loai;
const typeColor = (loai) => LOAI_BIEN_DONG_META[loai]?.color ?? "#6c757d";

// ── Bo loc co ban ──────────────────────────────────────────────────────────
const search = ref("");
const typeFilter = ref("");

// ── Bo loc nang cao ───────────────────────────────────────────────────────
const showAdvanced = ref(false);
const filterDateFrom = ref("");
const filterDateTo = ref("");
const filterNhanVien = ref("");

const activeFilterCount = computed(() =>
  [typeFilter.value, filterDateFrom.value, filterDateTo.value, filterNhanVien.value].filter(Boolean).length
);

const clearFilters = () => {
  typeFilter.value = "";
  filterDateFrom.value = "";
  filterDateTo.value = "";
  filterNhanVien.value = "";
};

// ── Loc ────────────────────────────────────────────────────────────────────
const filteredItems = computed(() => {
  const q = search.value.trim().toLowerCase();
  return items.value
    .filter((h) => {
      // Text search
      if (q && !(h.maSku ?? "").toLowerCase().includes(q) &&
          !(h.ghiChu ?? "").toLowerCase().includes(q) &&
          !(h.tenSanPham ?? "").toLowerCase().includes(q)) return false;

      // Type filter
      if (typeFilter.value && h.loaiBienDong !== typeFilter.value) return false;

      // Date range
      if (filterDateFrom.value) {
        const itemDate = new Date(h.ngayTao);
        const fromDate = new Date(filterDateFrom.value);
        fromDate.setHours(0, 0, 0, 0);
        if (itemDate < fromDate) return false;
      }
      if (filterDateTo.value) {
        const itemDate = new Date(h.ngayTao);
        const toDate = new Date(filterDateTo.value);
        toDate.setHours(23, 59, 59, 999);
        if (itemDate > toDate) return false;
      }

      // Nhan vien
      if (filterNhanVien.value && h.nguoiTao !== parseInt(filterNhanVien.value)) return false;

      return true;
    })
    .sort((a, b) => new Date(b.ngayTao) - new Date(a.ngayTao));
});

const { currentPage, totalPages, pagedItems: pagedHistory, pageSize } = usePagination(filteredItems);
</script>

<template>
  <div class="invhis-card">
    <!-- Header icon + title -->
    <div class="invhis-header">
      <div class="invhis-header__icon">
        <History :size="32" />
      </div>
      <div class="invhis-header__text">
        <h2 class="invhis-header__title">{{ t("admin.inventoryHistory.title") }}</h2>
        <p class="invhis-header__sub">{{ t("admin.inventoryHistory.subtitle") }}</p>
      </div>
    </div>

    <!-- Toolbar -->
    <div class="alt-toolbar">
      <span class="alt-toolbar__count">
        {{ filteredItems.length }}/{{ items.length }}
        {{ t("admin.inventoryHistory.countSuffix") }}
      </span>
      <div class="alt-toolbar__actions">
        <div class="alt-search">
          <Search class="alt-search__icon" :size="14" />
          <input v-model="search" :placeholder="t('admin.inventoryHistory.searchPlaceholder')" />
          <button v-if="search" class="invhis-search__clear" @click="search = ''">
            <X :size="12" />
          </button>
        </div>

        <!-- Toggle bo loc nang cao -->
        <button
          class="alt-btn alt-btn--ghost"
          :class="{ 'is-active': showAdvanced || activeFilterCount > 0 }"
          @click="showAdvanced = !showAdvanced"
        >
          <Filter :size="14" />
          <span>Bộ lọc</span>
          <span v-if="activeFilterCount > 0" class="invhis-filter-chip">{{ activeFilterCount }}</span>
          <ChevronDown style="font-size:0.7rem;transition:transform 0.2s;" :size="14" />
        </button>
      </div>
    </div>

    <!-- Bo loc nang cao -->
    <div v-if="showAdvanced" class="invhis-advanced-filter">
      <div class="invhis-filter-row">
        <!-- Loai bien dong -->
        <div class="invhis-filter-group">
          <label class="invhis-filter-label">Loại biến động</label>
          <select v-model="typeFilter" class="invhis-filter-select">
            <option value="">Tất cả</option>
            <option value="nhap">{{ t("admin.inventoryHistory.typeNhap") }}</option>
            <option value="xuat_ban">{{ t("admin.inventoryHistory.typeXuatBan") }}</option>
            <option value="tra_hang">{{ t("admin.inventoryHistory.typeTraHang") }}</option>
            <option value="dieu_chinh">{{ t("admin.inventoryHistory.typeDieuChinh") }}</option>
            <option value="huy">{{ t("admin.inventoryHistory.typeHuy") }}</option>
            <option value="giu_hang">{{ t("admin.inventoryHistory.typeGiuHang") }}</option>
          </select>
        </div>

        <!-- Tu ngay -->
        <div class="invhis-filter-group">
          <label class="invhis-filter-label">Từ ngày</label>
          <input v-model="filterDateFrom" type="date" class="invhis-filter-input" />
        </div>

        <!-- Den ngay -->
        <div class="invhis-filter-group">
          <label class="invhis-filter-label">Đến ngày</label>
          <input v-model="filterDateTo" type="date" class="invhis-filter-input" />
        </div>

        <!-- Nhan vien -->
        <div class="invhis-filter-group">
          <label class="invhis-filter-label">Người thực hiện</label>
          <select v-model="filterNhanVien" class="invhis-filter-select">
            <option value="">Tất cả</option>
            <option v-for="nv in nhanVienList" :key="nv.nhanVienId" :value="nv.nhanVienId">
              {{ nv.hoTen }}
            </option>
          </select>
        </div>

        <button v-if="activeFilterCount > 0" class="invhis-filter-clear" @click="clearFilters">
          <X :size="13" /> Xóa lọc
        </button>
      </div>
    </div>

    <!-- Bang -->
    <div v-if="loading" class="alt-empty">{{ t("admin.inventoryHistory.loading") }}</div>
    <div v-else class="alt-table-wrap">
      <table class="alt-table">
        <thead>
          <tr>
            <th style="width:40px;">#</th>
            <th style="width:140px;">{{ t("admin.inventoryHistory.colDate") }}</th>
            <th>{{ t("admin.inventoryHistory.colProduct") }}</th>
            <th style="width:130px;">{{ t("admin.inventoryHistory.colSku") }}</th>
            <th style="width:120px;">{{ t("admin.inventoryHistory.colType") }}</th>
            <th style="width:80px;">{{ t("admin.inventoryHistory.colQty") }}</th>
            <th>{{ t("admin.inventoryHistory.colStaff") }}</th>
            <th>{{ t("admin.inventoryHistory.colNote") }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(h, idx) in pagedHistory" :key="h.lichSuId">
            <td class="invhis-stt">{{ currentPage * pageSize + idx + 1 }}</td>
            <td class="invhis-date">{{ formatDateTime(h.ngayTao) }}</td>
            <td class="invhis-product">
              <div v-if="h.tenSanPham" class="invhis-product__name">
                <Package :size="13" />
                {{ h.tenSanPham }}
              </div>
              <span v-else class="text-secondary">—</span>
            </td>
            <td class="invhis-sku">{{ h.maSku || "—" }}</td>
            <td>
              <span
                class="invhis-type-tag"
                :style="{ background: typeColor(h.loaiBienDong) + '26', color: typeColor(h.loaiBienDong) }"
              >
                {{ typeLabel(h.loaiBienDong) }}
              </span>
            </td>
            <td class="invhis-qty" :class="h.soLuongThayDoi >= 0 ? 'invhis-qty--plus' : 'invhis-qty--minus'">
              {{ h.soLuongThayDoi >= 0 ? "+" : "" }}{{ h.soLuongThayDoi }}
            </td>
            <td class="invhis-staff">
              <div v-if="h.tenNhanVien" class="invhis-staff__name">
                <User :size="12" />
                {{ h.tenNhanVien }}
              </div>
              <span v-else class="text-secondary">—</span>
            </td>
            <td class="invhis-note">{{ h.ghiChu || "—" }}</td>
          </tr>
          <tr v-if="filteredItems.length === 0">
            <td colspan="8" class="alt-empty">
              {{ search || activeFilterCount > 0
                ? (t("admin.inventoryHistory.noResult") || "Không có kết quả phù hợp")
                : (t("admin.inventoryHistory.empty")) }}
              <span v-if="search || activeFilterCount > 0" class="d-block mt-1 small">
                <button type="button" class="btn btn-link p-0 small border-0" @click="clearFilters(); search = ''">
                  Xóa bộ lọc để xem tất cả
                </button>
              </span>
            </td>
          </tr>
        </tbody>
      </table>
      <Pagination
        v-if="totalPages > 1"
        class="alt-pager"
        :current-page="currentPage"
        :total-pages="totalPages"
        @page-change="currentPage = $event"
      />
    </div>
  </div>
</template>

<style scoped>
/* ── Card ─────────────────────────────────────────────────────────── */
.invhis-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  overflow: hidden;
}

/* ── Header ─────────────────────────────────────────────────────── */
.invhis-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px 16px;
  border-bottom: 1px solid var(--border-color);
}
.invhis-header__icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  background: rgba(139, 99, 210, 0.12);
  color: #8a63d2;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.invhis-header__title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--text-heading);
}
.invhis-header__sub {
  margin: 2px 0 0;
  font-size: 0.78rem;
  color: var(--text-muted);
}

/* ── Search clear ──────────────────────────────────────────────────── */
.invhis-search__clear {
  background: none;
  border: none;
  padding: 0 6px;
  cursor: pointer;
  color: var(--text-muted);
  display: flex;
  align-items: center;
}
.invhis-search__clear:hover { color: var(--text-primary); }

.invhis-filter-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #8a63d2;
  color: #fff;
  font-size: 0.65rem;
  font-weight: 700;
}

/* ── Advanced filter ─────────────────────────────────────────────── */
.invhis-advanced-filter {
  padding: 12px 16px;
  background: var(--bg-card-inset);
  border-bottom: 1px solid var(--border-color);
}
.invhis-filter-row {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}
.invhis-filter-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 140px;
}
.invhis-filter-label {
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.invhis-filter-select,
.invhis-filter-input {
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid var(--border-color-strong);
  background: var(--bg-input);
  color: var(--text-primary);
  font-size: 0.82rem;
}
.invhis-filter-input[type="date"] {
  color-scheme: dark;
}
.invhis-filter-clear {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 6px;
  border: 1px solid var(--border-color-strong);
  background: transparent;
  color: var(--text-secondary);
  font-size: 0.8rem;
  cursor: pointer;
  white-space: nowrap;
  align-self: flex-end;
}
.invhis-filter-clear:hover {
  background: var(--bg-card-alt);
  color: var(--text-primary);
}

/* ── Table cells ─────────────────────────────────────────────────── */
.invhis-stt {
  color: var(--text-muted);
  font-size: 0.78rem;
  text-align: center;
}
.invhis-date {
  font-size: 0.78rem;
  color: var(--text-secondary);
  white-space: nowrap;
}
.invhis-product {
  max-width: 200px;
}
.invhis-product__name {
  display: flex;
  align-items: center;
  gap: 5px;
  font-weight: 600;
  font-size: 0.82rem;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.invhis-sku {
  font-family: monospace;
  font-size: 0.8rem;
}
.invhis-type-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.72rem;
  font-weight: 600;
  white-space: nowrap;
}
.invhis-qty {
  font-weight: 700;
  text-align: right;
}
.invhis-qty--plus { color: #48c78e; }
.invhis-qty--minus { color: #e05252; }
.invhis-staff {
  max-width: 130px;
}
.invhis-staff__name {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 0.8rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.invhis-note {
  font-size: 0.78rem;
  color: var(--text-secondary);
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
