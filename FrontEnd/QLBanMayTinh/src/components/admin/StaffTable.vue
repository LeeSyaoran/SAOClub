<script setup>
import { ref, computed, reactive, onMounted, watch } from "vue";
import { Search, Phone, Mail, Filter, RotateCcw, X, Plus, ChevronDown } from "@lucide/vue";
import { t } from "../../i18n/index.js";
import { statusLabel, boDauTiengViet } from "../../utils/adminFormat.js";
import { StaffStore, ensureStaff, refreshStaff } from "../../stores/staff.js";
import * as NhanVienService from "../../services/NhanVienService.js";
import * as DmService from "../../services/DmService.js";
import Pagination from "../common/Pagination.vue";
import { usePagination } from "../../composables/usePagination.js";

onMounted(() => {
  ensureStaff();
  fetchChucVu();
});

const chucVuList = ref([]);
const fetchChucVu = async () => {
  try {
    chucVuList.value = await DmService.getChucVu();
  } catch (e) {
    chucVuList.value = [];
  }
};

const chucVuName = (id) => {
  return chucVuList.value.find((c) => c.id === id)?.tenChucVu ?? "—";
};

// ── Tìm kiếm & Bộ lọc nâng cao: Nhân viên ─────────────────────────────────────
const staffSearch = ref("");
const isFilterOpen = ref(false);
const filters = reactive({
  chucVuId: "",
  trangThai: "",
  luongMin: "",
  luongMax: "",
});

const activeFilterCount = computed(() => {
  let count = 0;
  if (filters.chucVuId !== "") count++;
  if (filters.trangThai) count++;
  if (filters.luongMin !== "") count++;
  if (filters.luongMax !== "") count++;
  return count;
});

const resetFilters = () => {
  filters.chucVuId = "";
  filters.trangThai = "";
  filters.luongMin = "";
  filters.luongMax = "";
  staffSearch.value = "";
};

const filteredStaff = computed(() => {
  const rawQ = staffSearch.value.trim();
  const q = boDauTiengViet(rawQ);
  const all = StaffStore.items ?? [];

  return all.filter((s) => {
    // 1. Text search
    if (q) {
      const match =
        boDauTiengViet(s.hoTen ?? "").includes(q) ||
        (s.soDienThoai ?? "").includes(q) ||
        boDauTiengViet(s.email ?? "").includes(q) ||
        boDauTiengViet(s.username ?? "").includes(q);
      if (!match) return false;
    }
    // 2. Chức vụ
    if (filters.chucVuId !== "" && s.chucVuId !== Number(filters.chucVuId)) {
      return false;
    }
    // 3. Trạng thái
    if (filters.trangThai && s.trangThai !== filters.trangThai) {
      return false;
    }
    // 4. Mức lương cơ bản
    const luong = Number(s.luongCoBan ?? 0);
    if (filters.luongMin !== "" && luong < Number(filters.luongMin)) return false;
    if (filters.luongMax !== "" && luong > Number(filters.luongMax)) return false;

    return true;
  });
});

const { currentPage, totalPages, pagedItems: pagedStaff, pageSize } = usePagination(filteredStaff);

// Reset trang về 0 khi bộ lọc thay đổi
watch(
  [
    staffSearch,
    () => filters.chucVuId,
    () => filters.trangThai,
    () => filters.luongMin,
    () => filters.luongMax,
  ],
  () => {
    currentPage.value = 0;
  }
);

// ── Avatar helpers ──
const getInitials = (s) => {
  const name = s?.hoTen || 'S';
  const parts = name.trim().split(' ');
  if (parts.length >= 2) return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  return name.substring(0, 2).toUpperCase();
};
const getAvatarBgColor = (s) => {
  const colors = ['#f472b6', '#38bdf8', '#fb923c', '#a78bfa', '#34d399', '#fbbf24', '#f87171'];
  const name = s?.username || s?.hoTen || '';
  let hash = 0;
  for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash);
  return colors[Math.abs(hash) % colors.length];
};
const getStatusClass = (s) => s.trangThai === 'active' ? 'staff-status active' : 'staff-status inactive';
const formatPrice = (p) => p ? new Intl.NumberFormat('vi-VN').format(p) + 'đ' : '0đ';

// ── Staff Modal ──
const showStaffModal = ref(false);
const editingStaffId = ref(null);
const staffFormError = ref("");
const emptyStaffForm = () => ({
  hoTen: "", soDienThoai: "", email: "", chucVuId: null,
  username: "", matKhauHash: "", luongCoBan: 0, trangThai: "active",
});
const staffForm = reactive(emptyStaffForm());

const openAddStaff = () => {
  Object.assign(staffForm, emptyStaffForm());
  editingStaffId.value = null;
  staffFormError.value = "";
  showStaffModal.value = true;
};
const openEditStaff = (s) => {
  Object.assign(staffForm, {
    hoTen: s.hoTen, soDienThoai: s.soDienThoai, email: s.email ?? "",
    chucVuId: s.chucVuId, username: s.username ?? "", matKhauHash: "",
    luongCoBan: s.luongCoBan ?? 0, trangThai: s.trangThai ?? "active",
  });
  editingStaffId.value = s.nhanVienId;
  staffFormError.value = "";
  showStaffModal.value = true;
};
const saveStaff = async () => {
  staffFormError.value = "";
  const body = {
    ...staffForm,
    chucVuId: Number(staffForm.chucVuId),
    luongCoBan: Number(staffForm.luongCoBan),
  };
  try {
    const res = await NhanVienService.save(editingStaffId.value, body);
    if (!res.ok) {
      staffFormError.value = t('admin.errors.saveFailedWithText', { status: res.status, text: await res.text() });
      return;
    }
    showStaffModal.value = false;
    await refreshStaff();
  } catch (e) {
    staffFormError.value = e.message;
  }
};
</script>

<template>
  <div class="alt-card staff-card-board">
    <div class="alt-toolbar">
      <span class="alt-toolbar__count">{{ filteredStaff.length }}/{{ (StaffStore.items ?? []).length }} {{ t('admin.staff.countSuffix') }}</span>
      <div class="alt-toolbar__actions">
        <!-- Thanh tìm kiếm -->
        <div class="alt-search">
          <Search class="alt-search__icon" :size="14" />
          <input v-model="staffSearch" placeholder="Tìm kiếm theo tên, sđt, email, user..." />
          <button
            v-if="staffSearch"
            type="button"
            class="alt-search__clear"
            @click="staffSearch = ''"
            title="Xóa tìm kiếm"
          >
            <X :size="12" />
          </button>
        </div>

        <!-- Nút Lọc nâng cao -->
        <button
          type="button"
          class="alt-btn"
          :class="isFilterOpen || activeFilterCount > 0 ? 'alt-btn--filter-active' : 'alt-btn--ghost'"
          @click="isFilterOpen = !isFilterOpen"
        >
          <Filter :size="13" />
          <span>Lọc nâng cao</span>
          <span v-if="activeFilterCount > 0" class="alt-badge-count">{{ activeFilterCount }}</span>
          <ChevronDown :size="13" class="filter-caret" :class="{ 'is-open': isFilterOpen }" />
        </button>

        <button class="alt-btn alt-btn--primary" @click="openAddStaff">
          <Plus :size="13" /> {{ t('admin.staff.add') }}
        </button>
      </div>
    </div>

    <!-- ══ DRAWER BỘ LỌC NÂNG CAO ══ -->
    <div class="sm-filter-collapse" :class="{ 'is-open': isFilterOpen }">
      <div class="sm-filter-panel">
        <div class="sm-filter-grid">
          <!-- Chức vụ -->
          <div class="sm-filter-field">
            <label>{{ t('admin.staff.colPosition') || 'Chức vụ' }}</label>
            <select v-model="filters.chucVuId" class="alt-select">
              <option value="">-- Tất cả chức vụ --</option>
              <option v-for="cv in chucVuList" :key="cv.id" :value="cv.id">
                {{ cv.tenChucVu }}
              </option>
            </select>
          </div>

          <!-- Trạng thái -->
          <div class="sm-filter-field">
            <label>{{ t('admin.staff.colStatus') || 'Trạng thái' }}</label>
            <select v-model="filters.trangThai" class="alt-select">
              <option value="">-- Tất cả trạng thái --</option>
              <option value="active">{{ t('admin.staffModal.statusActive') || 'Đang làm việc' }}</option>
              <option value="inactive">{{ t('admin.staffModal.statusResigned') || 'Đã nghỉ việc' }}</option>
            </select>
          </div>

          <!-- Khoảng lương cơ bản -->
          <div class="sm-filter-field">
            <label>{{ t('admin.staff.colBaseSalary') || 'Lương cơ bản (VNĐ)' }}</label>
            <div class="range-inputs">
              <input
                v-model="filters.luongMin"
                type="number"
                min="0"
                placeholder="Từ mức lương"
                class="alt-input"
              />
              <span class="range-sep">–</span>
              <input
                v-model="filters.luongMax"
                type="number"
                min="0"
                placeholder="Đến mức lương"
                class="alt-input"
              />
            </div>
          </div>
        </div>

        <div class="sm-filter-foot">
          <button
            type="button"
            class="alt-btn alt-btn--ghost alt-btn--sm"
            :disabled="activeFilterCount === 0"
            @click="resetFilters"
          >
            <RotateCcw :size="12" /> Xóa bộ lọc
          </button>
          <button
            type="button"
            class="alt-btn alt-btn--primary alt-btn--sm"
            @click="isFilterOpen = false"
          >
            Hoàn tất
          </button>
        </div>
      </div>
    </div>

    <!-- ══ CHIPS BỘ LỌC ĐANG ÁP DỤNG ══ -->
    <div v-if="activeFilterCount > 0" class="sm-active-chips">
      <span class="sm-chips-label">Đang lọc:</span>

      <span v-if="filters.chucVuId !== ''" class="sm-chip">
        Chức vụ: <b>{{ chucVuName(Number(filters.chucVuId)) }}</b>
        <button type="button" @click="filters.chucVuId = ''"><X :size="10" /></button>
      </span>

      <span v-if="filters.trangThai" class="sm-chip">
        Trạng thái: <b>{{ filters.trangThai === 'active' ? 'Đang làm việc' : 'Đã nghỉ việc' }}</b>
        <button type="button" @click="filters.trangThai = ''"><X :size="10" /></button>
      </span>

      <span v-if="filters.luongMin !== '' || filters.luongMax !== ''" class="sm-chip">
        Lương: <b>{{ filters.luongMin ? Number(filters.luongMin).toLocaleString() + 'đ' : '0đ' }} – {{ filters.luongMax ? Number(filters.luongMax).toLocaleString() + 'đ' : '∞' }}</b>
        <button type="button" @click="filters.luongMin = ''; filters.luongMax = ''"><X :size="10" /></button>
      </span>

      <button type="button" class="sm-chip-clear" @click="resetFilters">Xóa tất cả</button>
    </div>

    <div v-if="StaffStore.loading" class="alt-empty">{{ t('admin.staff.loading') }}</div>
    <div v-else class="staff-grid-wrap">
      <div v-if="pagedStaff.length === 0" class="alt-empty">{{ t('admin.staff.empty') }}</div>
      <div v-else class="staff-card-grid">
        <div
          v-for="s in pagedStaff"
          :key="s.nhanVienId"
          class="staff-card"
          @click="openEditStaff(s)"
        >
          <div class="staff-card__avatar-wrap">
            <div class="staff-card__avatar staff-card__avatar--initials" :style="{ background: getAvatarBgColor(s) }">
              {{ getInitials(s) }}
            </div>
            <span class="staff-card__status" :class="getStatusClass(s)">{{ statusLabel(s.trangThai) }}</span>
          </div>
          <div class="staff-card__body">
            <h4 class="staff-card__name">{{ s.hoTen || 'Nhân sự' }}</h4>
            <div class="staff-card__role">{{ chucVuName(s.chucVuId) }}</div>
            <div class="staff-card__info mt-2">
              <div><Phone :size="14" /> {{ s.soDienThoai || '—' }}</div>
              <div v-if="s.email" class="text-truncate" style="max-width: 150px;"><Mail :size="14" /> {{ s.email }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="totalPages > 1" class="staff-pagination">
      <Pagination :current-page="currentPage" :total-pages="totalPages" />
    </div>

    <!-- ══ MODAL NHAN VIEN ══ -->
    <div v-if="showStaffModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="showStaffModal=false">
      <div class="alt-card d-flex flex-column" style="width:560px;max-width:95vw;max-height:90vh;border-radius:14px;">
        <div class="alt-toolbar">
          <span>{{ editingStaffId?t('admin.staffModal.titleEdit'):t('admin.staffModal.titleAdd') }}</span>
          <button class="btn-close btn-sm ms-auto" :aria-label="t('common.close')" @click="showStaffModal=false"></button>
        </div>
        <div class="overflow-y-auto p-4">
          <div v-if="staffFormError" class="alert alert-danger small py-2 mb-3">{{ staffFormError }}</div>
          <div class="row g-3">
            <div class="col-6"><label class="form-label small">{{ t('admin.staffModal.fullNameLabel') }}</label><input v-model="staffForm.hoTen" class="form-control form-control-sm admin-input" /></div>
            <div class="col-6"><label class="form-label small">{{ t('admin.staffModal.phoneLabel') }}</label><input v-model="staffForm.soDienThoai" class="form-control form-control-sm admin-input" /></div>
            <div class="col-6"><label class="form-label small">{{ t('admin.staffModal.emailLabel') }}</label><input v-model="staffForm.email" class="form-control form-control-sm admin-input" /></div>
            <div class="col-6"><label class="form-label small">{{ t('admin.staffModal.positionLabel') }}</label><select v-model="staffForm.chucVuId" class="form-select form-select-sm admin-input"><option :value="null" disabled>{{ t('admin.staffModal.positionSelectPlaceholder') }}</option><option v-for="cv in chucVuList" :key="cv.id" :value="cv.id">{{ cv.tenChucVu }}</option></select></div>
            <div class="col-6"><label class="form-label small">{{ t('admin.staffModal.usernameLabel') }}</label><input v-model="staffForm.username" class="form-control form-control-sm admin-input" /></div>
            <div class="col-6"><label class="form-label small">{{ t('admin.staffModal.passwordLabel') }} {{ editingStaffId?t('admin.staffModal.passwordKeepHint'):t('admin.staffModal.passwordRequired') }}</label><input v-model="staffForm.matKhauHash" type="password" class="form-control form-control-sm admin-input" /></div>
            <div class="col-6"><label class="form-label small">{{ t('admin.staffModal.baseSalaryLabel') }}</label><input v-model="staffForm.luongCoBan" type="number" min="0" class="form-control form-control-sm admin-input" /></div>
            <div class="col-6"><label class="form-label small">{{ t('admin.staffModal.statusLabel') }}</label><select v-model="staffForm.trangThai" class="form-select form-select-sm admin-input"><option value="active">{{ t('admin.staffModal.statusActive') }}</option><option value="inactive">{{ t('admin.staffModal.statusResigned') }}</option></select></div>
          </div>
        </div>
        <div class="alt-toolbar" style="border-top:1px solid var(--border-color);border-bottom:none;justify-content:flex-end;gap:8px;">
          <button class="alt-btn alt-btn--ghost" @click="showStaffModal=false">{{ t('admin.staffModal.cancel') }}</button>
          <button class="alt-btn alt-btn--primary" @click="saveStaff">{{ editingStaffId?t('admin.staffModal.update'):t('admin.staffModal.addNew') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.staff-grid-wrap { padding: 16px; }
.staff-card-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;
}

.staff-card {
  position: relative;
  background: var(--bg-card-alt) !important;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: var(--shadow-sm);
  transition: all 0.15s ease;
}
.staff-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: var(--pink-300);
}

.staff-card__avatar-wrap {
  position: relative;
  padding: 16px 16px 0;
  display: flex;
  justify-content: center;
}
.staff-card__avatar {
  width: 75px;
  height: 75px;
  border-radius: 50%;
  border: 3px solid var(--bg-card);
  box-shadow: 0 3px 8px rgba(0,0,0,0.1);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  text-shadow: 0 1px 2px rgba(0,0,0,0.2);
}
.staff-card__status {
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
.staff-card__status.active {
  background: var(--success);
  color: #fff;
}
.staff-card__status.inactive {
  background: var(--danger);
  color: #fff;
}

.staff-card__body {
  padding: 16px 12px 16px;
  text-align: center;
}
.staff-card__name {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.staff-card__role {
  font-size: 11px;
  color: var(--pink-600);
  font-weight: 600;
  text-transform: uppercase;
}
.staff-card__info {
  font-size: 12px;
  color: var(--text-secondary);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.staff-card__info i { font-size: 11px; width: 12px; text-align: center; color: var(--pink-500); }

.staff-pagination {
  display: flex;
  justify-content: center;
  padding: 16px;
  border-top: 1px solid var(--border-color);
}

@media (max-width: 1400px) { .staff-card-grid { grid-template-columns: repeat(4, 1fr); } }
@media (max-width: 1100px) { .staff-card-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 800px)  { .staff-card-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 500px)  { .staff-card-grid { grid-template-columns: 1fr; } }

/* ══ Toolbar & Tìm kiếm ══ */
.alt-search {
  position: relative;
  display: flex;
  align-items: center;
}
.alt-search__clear {
  position: absolute;
  right: 8px;
  background: none;
  border: none;
  padding: 0;
  color: var(--text-muted, #94a3b8);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.15s;
}
.alt-search__clear:hover {
  color: var(--text-primary, #0f172a);
}

.filter-caret {
  transition: transform 0.2s ease;
}
.filter-caret.is-open {
  transform: rotate(180deg);
}

.alt-btn--filter-active {
  background: rgba(236, 72, 153, 0.15) !important;
  color: var(--pink-600, #db2777) !important;
  border-color: var(--pink-400, #f472b6) !important;
  font-weight: 700 !important;
}
.alt-badge-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--pink-600, #db2777);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  margin-left: 4px;
}

/* ══ Drawer Bộ lọc ══ */
.sm-filter-collapse {
  display: grid;
  grid-template-rows: 0fr;
  transition: grid-template-rows 0.24s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  background: var(--bg-card-alt, #fff5f9);
  border-bottom: 1px solid var(--border-color, #f1dbe6);
}
.sm-filter-collapse.is-open {
  grid-template-rows: 1fr;
}
.sm-filter-panel {
  min-height: 0;
  padding: 0 16px;
  transition: padding 0.24s ease;
}
.sm-filter-collapse.is-open .sm-filter-panel {
  padding: 16px;
}

.sm-filter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 12px 16px;
  align-items: flex-end;
}
.sm-filter-field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.sm-filter-field label {
  font-size: 12px;
  font-weight: 700;
  color: var(--pink-600, #db2777);
}
.sm-filter-field select,
.sm-filter-field input {
  width: 100%;
  padding: 7px 10px;
  font-size: 13px;
  border-radius: 8px;
  border: 1px solid var(--border-color-strong, #dba9c7);
  background: var(--bg-card, #fff);
  color: var(--text-primary, #1f2937);
  outline: none;
}
.sm-filter-field select:focus,
.sm-filter-field input:focus {
  border-color: var(--pink-500, #ec4899);
  box-shadow: 0 0 0 3px rgba(236, 72, 153, 0.15);
}

.range-inputs {
  display: flex;
  align-items: center;
  gap: 6px;
}
.range-inputs input {
  flex: 1;
}
.range-sep {
  color: var(--text-muted, #94a3b8);
  font-weight: 600;
}

.sm-filter-foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed var(--border-color, #f1dbe6);
}
.alt-btn--sm {
  padding: 5px 12px;
  font-size: 12px;
  border-radius: 999px;
}

/* ══ Thanh Chips ══ */
.sm-active-chips {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 8px 16px;
  background: var(--bg-card, #fff);
  border-bottom: 1px solid var(--border-color, #f1dbe6);
  font-size: 12.5px;
}
.sm-chips-label {
  color: var(--text-muted, #6b7280);
  font-weight: 600;
  font-size: 12px;
}
.sm-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 10px;
  border-radius: 999px;
  background: var(--bg-card-alt, #fff5f9);
  border: 1px solid var(--pink-200, #ffcfe1);
  color: var(--pink-600, #db2777);
  font-size: 12px;
}
.sm-chip button {
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  color: var(--pink-600, #db2777);
  display: inline-flex;
  align-items: center;
  opacity: 0.7;
}
.sm-chip button:hover {
  opacity: 1;
}
.sm-chip-clear {
  background: none;
  border: none;
  color: var(--pink-600, #db2777);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  text-decoration: underline;
  margin-left: 4px;
}
</style>
