<script setup>
import { ref, computed, reactive, onMounted } from "vue";
import { Search, Phone, Mail } from "@lucide/vue";
import { t } from "../../i18n/index.js";
import { statusLabel } from "../../utils/adminFormat.js";
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

const staffSearch = ref("");
const filteredStaff = computed(() => {
  const q = staffSearch.value.trim().toLowerCase();
  const all = StaffStore.items ?? [];
  if (!q) return all;
  return all.filter((s) => {
    return (s.hoTen ?? '').toLowerCase().includes(q) ||
           (s.soDienThoai ?? '').includes(q) ||
           (s.email ?? '').toLowerCase().includes(q) ||
           (s.username ?? '').toLowerCase().includes(q);
  });
});

const { currentPage, totalPages, pagedItems: pagedStaff } = usePagination(filteredStaff);

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
        <div class="alt-search">
          <Search class="alt-search__icon" :size="14" />
          <input v-model="staffSearch" placeholder="Tìm kiếm nhân sự..." />
        </div>
        <button class="alt-btn alt-btn--primary" @click="openAddStaff">{{ t('admin.staff.add') }}</button>
      </div>
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
</style>
