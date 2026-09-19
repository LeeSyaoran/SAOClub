<script setup>
import { ref, computed, onMounted } from "vue";
import { Search, Filter, ChevronDown, Plus } from "@lucide/vue";
import { t } from "../../i18n/index.js";
import * as NhaCungCapService from "../../services/NhaCungCapService.js";
import { statusLabel } from "../../utils/adminFormat.js";
import { showToast } from "../../stores/toast.js";
import { SuppliersStore, ensureSuppliers, refreshSuppliers } from "../../stores/suppliers.js";
import Pagination from "../common/Pagination.vue";
import { usePagination } from "../../composables/usePagination.js";
import { Building2, Truck, X, Hash, Image, Phone, Mail, MapPin, FileText, User, Activity, SlidersHorizontal } from "@lucide/vue";

onMounted(() => { ensureSuppliers(); });

// ── Bo loc co ban ────────────────────────────────────────────────────────────
const supplierSearch = ref("");

// ── Bo loc nang cao ────────────────────────────────────────────────────────
const showAdvanced = ref(false);
const filterStatus = ref("");
const filterDateFrom = ref("");
const filterDateTo = ref("");

const filteredSuppliers = computed(() => {
  const q = supplierSearch.value.trim().toLowerCase();
  const all = SuppliersStore.items ?? [];
  return all.filter((s) => {
    // Tim kiem text
    if (q && !(s.tenNhaCungCap ?? "").toLowerCase().includes(q) &&
        !(s.soDienThoai ?? "").includes(q) &&
        !(s.email ?? "").toLowerCase().includes(q) &&
        !(s.diaChi ?? "").toLowerCase().includes(q)) return false;
    // Filter trang thai
    if (filterStatus.value && s.trangThai !== filterStatus.value) return false;
    // Filter ngay tao
    if (filterDateFrom.value && s.ngayTao && s.ngayTao.substring(0, 10) < filterDateFrom.value) return false;
    if (filterDateTo.value && s.ngayTao && s.ngayTao.substring(0, 10) > filterDateTo.value) return false;
    return true;
  });
});

const activeFilterCount = computed(() =>
  [filterStatus.value, filterDateFrom.value, filterDateTo.value].filter(Boolean).length
);

const clearFilters = () => {
  filterStatus.value = "";
  filterDateFrom.value = "";
  filterDateTo.value = "";
};

const { currentPage, totalPages, pagedItems: pagedSuppliers, pageSize } = usePagination(filteredSuppliers);

// ── Modal them/sua ────────────────────────────────────────────────────────────
const showModal = ref(false);
const editingId = ref(null);
const formError = ref("");
const saving = ref(false);
const emptyForm = () => ({
  tenNhaCungCap: "",
  soDienThoai: "",
  email: "",
  diaChi: "",
  hinhAnh: "",
  maSoThue: "",
  nguoiLienHe: "",
  trangThai: "active",
});
const form = ref(emptyForm());

const openAdd = () => {
  editingId.value = null;
  form.value = emptyForm();
  formError.value = "";
  showModal.value = true;
};
const openEdit = (s) => {
  editingId.value = s.nhaCungCapId;
  form.value = {
    tenNhaCungCap: s.tenNhaCungCap ?? "",
    soDienThoai: s.soDienThoai ?? "",
    email: s.email ?? "",
    diaChi: s.diaChi ?? "",
    hinhAnh: s.hinhAnh ?? "",
    maSoThue: s.maSoThue ?? "",
    nguoiLienHe: s.nguoiLienHe ?? "",
    trangThai: s.trangThai ?? "active",
  };
  formError.value = "";
  showModal.value = true;
};

const saveSupplier = async () => {
  formError.value = "";
  if (!form.value.tenNhaCungCap.trim()) {
    formError.value = t("admin.supplierModal.nameRequired");
    return;
  }
  if (!form.value.soDienThoai.trim()) {
    formError.value = t("admin.supplierModal.phoneRequired");
    return;
  }
  if (!form.value.diaChi.trim()) {
    formError.value = t("admin.supplierModal.addressRequired");
    return;
  }
  if (saving.value) return;
  saving.value = true;
  try {
    const res = await NhaCungCapService.save(editingId.value, form.value);
    if (!res.ok) {
      formError.value = t("admin.errors.saveFailed", { status: res.status, text: await res.text() });
      return;
    }
    showModal.value = false;
    await refreshSuppliers();
  } catch (e) {
    formError.value = e.message;
  } finally {
    saving.value = false;
  }
};
</script>

<template>
  <div class="sup-card">
    <!-- Header icon + title -->
    <div class="sup-header">
      <div class="sup-header__icon">
        <Building2 :size="32" />
      </div>
      <div class="sup-header__text">
        <h2 class="sup-header__title">{{ t("admin.suppliers.title") }}</h2>
        <p class="sup-header__sub">{{ t("admin.suppliers.subtitle") }}</p>
      </div>
    </div>

    <!-- Toolbar -->
    <div class="alt-toolbar">
      <span class="alt-toolbar__count">
        {{ filteredSuppliers.length }}/{{ (SuppliersStore.items ?? []).length }}
        {{ t("admin.suppliers.countSuffix") }}
      </span>
      <div class="alt-toolbar__actions">
        <div class="alt-search">
          <Search class="alt-search__icon" :size="14" />
          <input v-model="supplierSearch" :placeholder="t('admin.suppliers.searchPlaceholder')" />
          <button v-if="supplierSearch" class="sup-search__clear" @click="supplierSearch = ''">
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
          <span v-if="activeFilterCount > 0" class="sup-filter-chip">{{ activeFilterCount }}</span>
          <ChevronDown style="font-size:0.7rem;transition:transform 0.2s;" :size="14" />
        </button>

        <button class="alt-btn alt-btn--primary" @click="openAdd">
          <Plus :size="14" /> {{ t("admin.suppliers.add") }}
        </button>
      </div>
    </div>

    <!-- Bo loc nang cao -->
    <div v-if="showAdvanced" class="sup-advanced-filter">
      <div class="sup-filter-row">
        <div class="sup-filter-group">
          <label class="sup-filter-label">{{ t("admin.suppliers.filterStatus") || "Trạng thái" }}</label>
          <select v-model="filterStatus" class="sup-filter-select">
            <option value="">Tất cả</option>
            <option value="active">{{ t("admin.productModal.statusActive") || "Active" }}</option>
            <option value="inactive">{{ t("admin.productModal.statusInactive") || "Inactive" }}</option>
          </select>
        </div>

        <div class="sup-filter-group">
          <label class="sup-filter-label">Từ ngày</label>
          <input type="date" v-model="filterDateFrom" class="sup-filter-select" />
        </div>
        
        <div class="sup-filter-group">
          <label class="sup-filter-label">Đến ngày</label>
          <input type="date" v-model="filterDateTo" class="sup-filter-select" />
        </div>

        <button v-if="activeFilterCount > 0" class="sup-filter-clear" @click="clearFilters">
          <X :size="13" /> Xóa lọc
        </button>
      </div>
    </div>

    <!-- Bang -->
    <div v-if="SuppliersStore.loading" class="alt-empty">{{ t("admin.suppliers.loading") }}</div>
    <div v-else class="alt-table-wrap">
      <table class="alt-table">
        <thead>
          <tr>
            <th style="width:4%; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><Hash :size="12" /> {{ t("admin.common.stt") }}</span></th>
            <th style="width:5%; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><Image :size="12" /> Hình ảnh</span></th>
            <th style="width:18%;"><span class="d-inline-flex align-items-center gap-1.5"><Building2 :size="12" /> {{ t("admin.suppliers.colName") }}</span></th>
            <th style="width:10%; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><Phone :size="12" /> {{ t("admin.suppliers.colPhone") }}</span></th>
            <th style="width:12%;"><span class="d-inline-flex align-items-center gap-1.5"><Mail :size="12" /> {{ t("admin.suppliers.colEmail") }}</span></th>
            <th style="width:18%;"><span class="d-inline-flex align-items-center gap-1.5"><MapPin :size="12" /> {{ t("admin.suppliers.colAddress") || "Địa chỉ" }}</span></th>
            <th style="width:9%; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><FileText :size="12" /> {{ t("admin.suppliers.colTaxCode") || "Mã số thuế" }}</span></th>
            <th style="width:10%;"><span class="d-inline-flex align-items-center gap-1.5"><User :size="12" /> {{ t("admin.suppliers.colContact") }}</span></th>
            <th style="width:7%; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><Activity :size="12" /> {{ t("admin.suppliers.colStatus") }}</span></th>
            <th style="width:7%; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><SlidersHorizontal :size="12" /> {{ t("admin.suppliers.colAction") }}</span></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(s, idx) in pagedSuppliers" :key="s.nhaCungCapId">
            <td class="sup-stt text-center">{{ currentPage * pageSize + idx + 1 }}</td>
            <td class="text-center">
              <img v-if="s.hinhAnh" :src="s.hinhAnh" style="width:40px;height:40px;object-fit:contain;border-radius:4px;border:1px solid var(--border-color);" class="mx-auto" />
              <div v-else class="sup-name__icon mx-auto" style="margin:0;"><Truck :size="16" /></div>
            </td>
            <td class="sup-name">
              <span>{{ s.tenNhaCungCap }}</span>
            </td>
            <td class="sup-phone text-center">{{ s.soDienThoai }}</td>
            <td class="sup-email">{{ s.email || "—" }}</td>
            <td class="sup-address" :title="s.diaChi">{{ s.diaChi || "—" }}</td>
            <td class="sup-taxcode text-center">{{ s.maSoThue || "—" }}</td>
            <td class="sup-contact">{{ s.nguoiLienHe || "—" }}</td>
            <td class="text-center">
              <span
                class="alt-tag"
                :style="s.trangThai === 'active'
                  ? 'background:rgba(22,163,74,0.14);color:var(--state-success);'
                  : 'background:var(--bg-card-alt);color:var(--text-secondary);'"
              >
                {{ statusLabel(s.trangThai) }}
              </span>
            </td>
            <td class="text-center">
              <button class="alt-btn alt-btn--ghost" style="padding:4px 12px;" @click="openEdit(s)">
                {{ t("admin.suppliers.edit") }}
              </button>
            </td>
          </tr>
          <tr v-if="filteredSuppliers.length === 0">
            <td colspan="9" class="alt-empty">
              {{ supplierSearch || filterStatus || filterDateFrom || filterDateTo
                ? (t("admin.suppliers.noResult") || "Không có kết quả phù hợp")
                : (t("admin.suppliers.empty") || "Chưa có nhà cung cấp nào") }}
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

  <!-- Modal them/sua -->
  <div
    v-if="showModal"
    class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
    style="background:rgba(0,0,0,0.6);z-index:1000;backdrop-filter:blur(2px);"
    @click.self="showModal = false"
  >
    <div class="rounded-3 p-4" style="background:var(--bg-card);width:480px;max-width:94vw;box-shadow:0 24px 64px rgba(0,0,0,0.5);">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <div class="fw-bold" style="color:var(--text-heading);font-size:1rem;">
          <Truck :size="18" class="me-1" style="vertical-align:-3px;" />
          {{ editingId ? t("admin.supplierModal.titleEdit") : t("admin.supplierModal.titleAdd") }}
        </div>
        <button
          class="sup-modal-close"
          :aria-label="t('common.close')"
          @click="showModal = false"
        >
          <X :size="16" />
        </button>
      </div>

      <div v-if="formError" class="alert alert-danger small py-2 mb-3">{{ formError }}</div>

      <div class="mb-2">
        <label class="form-label small text-secondary mb-1">{{ t("admin.supplierModal.nameLabel") }} *</label>
        <input v-model="form.tenNhaCungCap" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>

      <div class="row g-2 mb-2">
        <div class="col-6">
          <label class="form-label small text-secondary mb-1">{{ t("admin.supplierModal.phoneLabel") }} *</label>
          <input v-model="form.soDienThoai" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
        </div>
        <div class="col-6">
          <label class="form-label small text-secondary mb-1">{{ t("admin.supplierModal.emailLabel") }}</label>
          <input v-model="form.email" type="email" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
        </div>
      </div>

      <div class="mb-2">
        <label class="form-label small text-secondary mb-1">{{ t("admin.supplierModal.addressLabel") }} *</label>
        <input v-model="form.diaChi" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>

      <div class="row g-2 mb-2">
        <div class="col-6">
          <label class="form-label small text-secondary mb-1">Hình ảnh (URL)</label>
          <input v-model="form.hinhAnh" class="form-control form-control-sm" placeholder="https://..." style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
        </div>
        <div class="col-6">
          <label class="form-label small text-secondary mb-1">{{ t("admin.supplierModal.taxCodeLabel") }}</label>
          <input v-model="form.maSoThue" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);font-family:monospace;" />
        </div>
      </div>

      <div class="mb-2">
        <label class="form-label small text-secondary mb-1">{{ t("admin.supplierModal.contactLabel") }}</label>
        <input v-model="form.nguoiLienHe" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>

      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ t("admin.supplierModal.statusLabel") }}</label>
        <select v-model="form.trangThai" class="form-select form-select-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);">
          <option value="active">{{ t("admin.productModal.statusActive") }}</option>
          <option value="inactive">{{ t("admin.productModal.statusInactive") }}</option>
        </select>
      </div>

      <div class="d-flex justify-content-end gap-2">
        <button class="btn btn-sm btn-outline-secondary" @click="showModal = false">
          {{ t("admin.productModal.cancel") }}
        </button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" :disabled="saving" @click="saveSupplier">
          <span v-if="saving">{{ t("admin.saving", "Đang lưu...") }}</span>
          <span v-else>{{ editingId ? t("admin.productModal.update") : t("admin.productModal.addNew") }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ── Card wrapper ─────────────────────────────────────────────────── */
.sup-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  overflow: hidden;
}

/* ── Header icon ───────────────────────────────────────────────── */
.sup-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px 16px;
  border-bottom: 1px solid var(--border-color);
}
.sup-header__icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  background: rgba(72, 199, 142, 0.12);
  color: #48c78e;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.sup-header__title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--text-heading);
}
.sup-header__sub {
  margin: 2px 0 0;
  font-size: 0.78rem;
  color: var(--text-muted);
}

/* ── Toolbar ─────────────────────────────────────────────────────── */
.sup-search__clear {
  background: none;
  border: none;
  padding: 0 6px;
  cursor: pointer;
  color: var(--text-muted);
  display: flex;
  align-items: center;
}
.sup-search__clear:hover { color: var(--text-primary); }

.sup-filter-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #48c78e;
  color: #fff;
  font-size: 0.65rem;
  font-weight: 700;
}

/* ── Advanced filter ─────────────────────────────────────────────── */
.sup-advanced-filter {
  padding: 12px 16px;
  background: var(--bg-card-inset);
  border-bottom: 1px solid var(--border-color);
}
.sup-filter-row {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}
.sup-filter-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 140px;
}
.sup-filter-label {
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.sup-filter-select {
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid var(--border-color-strong);
  background: var(--bg-input);
  color: var(--text-primary);
  font-size: 0.82rem;
}
.sup-filter-clear {
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
.sup-filter-clear:hover {
  background: var(--bg-card-alt);
  color: var(--text-primary);
}

/* ── Table cells ────────────────────────────────────────────────── */
.sup-stt {
  color: var(--text-muted);
  font-size: 0.78rem;
  text-align: center;
}
.sup-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: var(--text-primary);
}
.sup-name__icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: rgba(72, 199, 142, 0.1);
  color: #48c78e;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.sup-phone {
  font-family: monospace;
  font-size: 0.82rem;
}
.sup-email {
  font-size: 0.8rem;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sup-address {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 0.8rem;
  color: var(--text-secondary);
}
.sup-taxcode {
  font-family: monospace;
  font-size: 0.8rem;
  color: var(--text-secondary);
}
.sup-contact {
  font-size: 0.82rem;
}

/* ── Modal close button ─────────────────────────────────────────── */
.sup-modal-close {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: 1px solid var(--border-color);
  background: var(--bg-card-alt);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.sup-modal-close:hover {
  background: var(--bg-card-alt);
  color: var(--text-primary);
}
</style>
