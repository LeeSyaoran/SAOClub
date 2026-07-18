<script setup>
import { ref, computed } from "vue";
import { AuthStore, clearSession, setSession } from "../../stores/index.js";
import { t } from "../../i18n/index.js";
import * as CaiDatService from "../../Service/CaiDatService.js";

const emit = defineEmits(["navigate-settings"]);

// ── User ─────────────────────────────────────────────────────────────────────
const userDisplayName = computed(() => AuthStore.user?.hoTen ?? AuthStore.user?.username ?? "Admin");
const userAvatar = computed(() => userDisplayName.value.charAt(0).toUpperCase());
const userDisplayRole = computed(() => {
  const role = AuthStore.user?.role;
  if (role === "admin")     return t("admin.userRole.admin");
  if (role === "nhan_vien") return t("admin.userRole.nhanVien");
  if (role === "quan_kho")  return t("admin.userRole.quanKho");
  return t("admin.userRole.guest");
});

const logout = () => {
  clearSession();
  window.location.hash = '';
};

// ── Menu hồ sơ (dropdown ở chân sidebar) ─────────────────────────────────────
const showUserMenu = ref(false);
const userMenuTriggerRef = ref(null);
const closeUserMenu = () => {
  showUserMenu.value = false;
  userMenuTriggerRef.value?.focus();
};
const onUserMenuFocusOut = (e) => {
  if (!e.currentTarget.contains(e.relatedTarget)) showUserMenu.value = false;
};

// ── Modal: Chỉnh sửa hồ sơ ────────────────────────────────────────────────────
const showEditProfileModal = ref(false);
const profileForm = ref({ hoTen: '', soDienThoai: '', email: '' });
const profileSaving = ref(false);
const profileError = ref('');
const profileSaved = ref(false);

const openEditProfileModal = () => {
  showUserMenu.value = false;
  profileForm.value = {
    hoTen: AuthStore.user?.hoTen ?? '',
    soDienThoai: AuthStore.user?.soDienThoai ?? '',
    email: AuthStore.user?.email ?? '',
  };
  profileError.value = '';
  profileSaved.value = false;
  showEditProfileModal.value = true;
};

const saveProfile = async () => {
  profileSaving.value = true;
  profileError.value = '';
  profileSaved.value = false;
  try {
    const res = await CaiDatService.capNhatHoSo(profileForm.value);
    setSession({ ...AuthStore.user, hoTen: res.hoTen, soDienThoai: res.soDienThoai, email: res.email });
    profileSaved.value = true;
  } catch (e) {
    profileError.value = e.message || String(e);
  } finally {
    profileSaving.value = false;
  }
};

// ── Modal: Đổi mật khẩu (link nhanh từ menu hồ sơ — cùng API đã có ở trang Cài đặt) ──
const showQuickPasswordModal = ref(false);
const qpMatKhauCu = ref('');
const qpMatKhauMoi = ref('');
const qpMatKhauXacNhan = ref('');
const qpError = ref('');
const qpSuccess = ref('');
const qpLoading = ref(false);

const openQuickPasswordModal = () => {
  showUserMenu.value = false;
  qpMatKhauCu.value = '';
  qpMatKhauMoi.value = '';
  qpMatKhauXacNhan.value = '';
  qpError.value = '';
  qpSuccess.value = '';
  showQuickPasswordModal.value = true;
};

const quickChangePassword = async () => {
  qpError.value = '';
  qpSuccess.value = '';
  if (qpMatKhauMoi.value !== qpMatKhauXacNhan.value) {
    qpError.value = t('admin.settings.passwordMismatch');
    return;
  }
  qpLoading.value = true;
  try {
    await CaiDatService.doiMatKhau(qpMatKhauCu.value, qpMatKhauMoi.value);
    qpSuccess.value = t('admin.settings.passwordChanged');
    qpMatKhauCu.value = '';
    qpMatKhauMoi.value = '';
    qpMatKhauXacNhan.value = '';
  } catch (e) {
    qpError.value = e.message || String(e);
  } finally {
    qpLoading.value = false;
  }
};

const goToSettingsFromMenu = () => {
  showUserMenu.value = false;
  emit('navigate-settings');
};
</script>

<template>
  <!-- Footer sidebar: thong tin user (bam mo dropdown ho so) + logout -->
  <div class="p-3 border-top position-relative" style="border-color:var(--border-color-soft)!important;"
       @keydown.esc="closeUserMenu" @focusout="onUserMenuFocusOut">
    <!-- Dropdown menu ho so — mo LEN tren (bottom:100%) vi dang o cuoi sidebar -->
    <div v-if="showUserMenu" class="position-absolute rounded-3 shadow-lg overflow-hidden"
         style="left:12px; right:12px; bottom:100%; margin-bottom:8px; background:var(--bg-card); border:1px solid var(--border-color); z-index:50;">
      <button class="btn btn-sm w-100 text-start rounded-0 border-0" style="color:var(--text-primary);" @click="openEditProfileModal">
        {{ t('admin.profileMenu.editProfile') }}
      </button>
      <button class="btn btn-sm w-100 text-start rounded-0 border-0" style="color:var(--text-primary);" @click="openQuickPasswordModal">
        {{ t('admin.settings.changePasswordTitle') }}
      </button>
      <button class="btn btn-sm w-100 text-start rounded-0 border-0" style="color:var(--text-primary);" @click="goToSettingsFromMenu">
        {{ t('admin.sidebar.settings') }}
      </button>
    </div>

    <button ref="userMenuTriggerRef" type="button"
            class="btn d-flex align-items-center gap-2 mb-2 w-100 text-start p-0 border-0"
            style="background:transparent;"
            aria-haspopup="true" :aria-expanded="showUserMenu"
            @click="showUserMenu = !showUserMenu">
      <div class="rounded-circle d-flex align-items-center justify-content-center fw-bold flex-shrink-0"
           style="width:34px;height:34px;background:var(--accent);color:var(--accent-text);font-size:0.9rem;">{{ userAvatar }}</div>
      <div class="flex-grow-1" style="min-width:0;">
        <div class="fw-semibold text-truncate" style="font-size:0.85rem;">{{ userDisplayName }}</div>
        <div style="font-size:0.72rem;color:var(--text-muted);">{{ userDisplayRole }}</div>
      </div>
    </button>
    <button class="btn btn-sm w-100 fw-semibold"
            style="background:var(--bg-card); border:1px solid #7f1d1d; border-radius:8px; color:#f87171; font-size:0.78rem;"
            @click="logout">
      {{ t('admin.sidebar.logout') }}
    </button>
  </div>

  <!-- ══ MODAL CHINH SUA HO SO ══ -->
  <div v-if="showEditProfileModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1070;" @click.self="showEditProfileModal=false">
    <div class="rounded-3 p-3" style="background:var(--bg-card);width:420px;max-width:94vw;">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <div class="fw-bold" style="color:var(--text-heading);">{{ t('admin.profileMenu.editProfile') }}</div>
        <button class="btn-close btn-close-white btn-sm" @click="showEditProfileModal=false"></button>
      </div>
      <div class="mb-2">
        <label class="form-label small text-secondary mb-1">{{ t('admin.profileMenu.fullName') }}</label>
        <input v-model="profileForm.hoTen" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>
      <div class="mb-2">
        <label class="form-label small text-secondary mb-1">{{ t('admin.profileMenu.phone') }}</label>
        <input v-model="profileForm.soDienThoai" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>
      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ t('admin.profileMenu.email') }}</label>
        <input v-model="profileForm.email" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>
      <div v-if="profileError" class="text-danger small mb-2">{{ profileError }}</div>
      <div v-if="profileSaved" class="text-success small mb-2">{{ t('admin.profileMenu.profileSaved') }}</div>
      <div class="d-flex justify-content-end gap-2">
        <button class="btn btn-sm btn-outline-secondary" @click="showEditProfileModal=false">{{ t('admin.productModal.cancel') }}</button>
        <button class="btn btn-sm btn-warning" :disabled="profileSaving" @click="saveProfile">{{ t('admin.settings.saveButton') }}</button>
      </div>
    </div>
  </div>

  <!-- ══ MODAL DOI MAT KHAU NHANH (tu menu ho so) ══ -->
  <div v-if="showQuickPasswordModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1070;" @click.self="showQuickPasswordModal=false">
    <div class="rounded-3 p-3" style="background:var(--bg-card);width:420px;max-width:94vw;">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <div class="fw-bold" style="color:var(--text-heading);">{{ t('admin.settings.changePasswordTitle') }}</div>
        <button class="btn-close btn-close-white btn-sm" @click="showQuickPasswordModal=false"></button>
      </div>
      <div class="mb-2">
        <label class="form-label small text-secondary mb-1">{{ t('admin.settings.currentPassword') }}</label>
        <input type="password" v-model="qpMatKhauCu" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>
      <div class="mb-2">
        <label class="form-label small text-secondary mb-1">{{ t('admin.settings.newPassword') }}</label>
        <input type="password" v-model="qpMatKhauMoi" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>
      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ t('admin.settings.confirmNewPassword') }}</label>
        <input type="password" v-model="qpMatKhauXacNhan" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>
      <div v-if="qpError" class="text-danger small mb-2">{{ qpError }}</div>
      <div v-if="qpSuccess" class="text-success small mb-2">{{ qpSuccess }}</div>
      <div class="d-flex justify-content-end gap-2">
        <button class="btn btn-sm btn-outline-secondary" @click="showQuickPasswordModal=false">{{ t('admin.productModal.cancel') }}</button>
        <button class="btn btn-sm btn-warning" :disabled="qpLoading || !qpMatKhauCu || !qpMatKhauMoi" @click="quickChangePassword">{{ t('admin.settings.changePasswordButton') }}</button>
      </div>
    </div>
  </div>
</template>
