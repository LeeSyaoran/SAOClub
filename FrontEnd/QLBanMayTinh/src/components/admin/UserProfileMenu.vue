<script setup>
import { ref, computed } from "vue";
import { User, Key, Settings, LogOut, Camera } from "@lucide/vue";
import { AuthStore, clearSession, setSession } from "../../stores/index.js";
import { resetAllStores } from "../../stores/resetAll.js";
import { t } from "../../i18n/index.js";
import * as CaiDatService from "../../services/CaiDatService.js";

const emit = defineEmits(["navigate-settings"]);
const props = defineProps({ showSettingsLink: { type: Boolean, default: true } });

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
  resetAllStores();
  window.location.hash = '#/';
};

// ── Menu hồ sơ (dropdown ở topbar) ─────────────────────────────────────
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
const profileForm = ref({ hoTen: '', soDienThoai: '', email: '', avatarBase64: '' });
const profileSaving = ref(false);
const profileError = ref('');
const profileSaved = ref(false);

const avatarInput = ref(null);
const avatarPreview = ref(null);

const onAvatarChange = (e) => {
  const file = e.target.files[0];
  if (file) {
    if (file.size > 20 * 1024 * 1024) {
      profileError.value = "Kích thước ảnh vượt quá giới hạn 20MB.";
      e.target.value = ''; // Reset input
      return;
    }
    profileError.value = ''; // Xóa lỗi nếu file hợp lệ
    
    const reader = new FileReader();
    reader.onload = (evt) => {
      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement('canvas');
        const MAX_SIZE = 500;
        let width = img.width;
        let height = img.height;
        if (width > height && width > MAX_SIZE) {
          height *= MAX_SIZE / width;
          width = MAX_SIZE;
        } else if (height > MAX_SIZE) {
          width *= MAX_SIZE / height;
          height = MAX_SIZE;
        }
        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0, width, height);
        const dataUrl = canvas.toDataURL('image/webp', 0.85);
        avatarPreview.value = dataUrl;
        profileForm.value.avatarBase64 = dataUrl;
      };
      img.src = evt.target.result;
    };
    reader.readAsDataURL(file);
  }
};

const openEditProfileModal = () => {
  showUserMenu.value = false;
  profileForm.value = {
    hoTen: AuthStore.user?.hoTen ?? '',
    soDienThoai: AuthStore.user?.soDienThoai ?? '',
    email: AuthStore.user?.email ?? '',
    avatarBase64: ''
  };
  avatarPreview.value = AuthStore.user?.avatarUrl || null;
  profileError.value = '';
  profileSaved.value = false;
  showEditProfileModal.value = true;
};

function dataURLtoFile(dataurl, filename) {
  var arr = dataurl.split(','), mime = arr[0].match(/:(.*?);/)[1],
      bstr = atob(arr[1]), n = bstr.length, u8arr = new Uint8Array(n);
  while(n--){
      u8arr[n] = bstr.charCodeAt(n);
  }
  return new File([u8arr], filename, {type:mime});
}

const saveProfile = async () => {
  profileSaving.value = true;
  profileError.value = '';
  profileSaved.value = false;
  try {
    let finalAvatarUrl = profileForm.value.avatarBase64;
    
    // Nếu có ảnh mới (base64 data URL), thì upload ảnh trước
    if (finalAvatarUrl && finalAvatarUrl.startsWith('data:')) {
      const file = dataURLtoFile(finalAvatarUrl, 'avatar.webp');
      const uploadData = await CaiDatService.uploadImage(file);
      finalAvatarUrl = uploadData.url;
    }

    const payload = { ...profileForm.value };
    payload.avatarUrl = finalAvatarUrl;
    delete payload.avatarBase64; // Xóa base64 khổng lồ để không làm nặng request

    const res = await CaiDatService.capNhatHoSo(payload);
    setSession({ ...AuthStore.user, hoTen: res.hoTen, soDienThoai: res.soDienThoai, email: res.email, avatarUrl: res.avatarUrl });
    profileSaved.value = true;
    setTimeout(() => { if(showEditProfileModal.value) showEditProfileModal.value = false; }, 1000);
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
  <div
    class="position-relative"
    @keydown.esc="closeUserMenu" @focusout="onUserMenuFocusOut"
  >
    <button
      ref="userMenuTriggerRef" type="button"
      class="user-menu-trigger shadow-sm px-1 py-1"
      :class="{ 'is-open': showUserMenu }"
      aria-haspopup="true" :aria-expanded="showUserMenu"
      @click="showUserMenu = !showUserMenu"
    >
      <span class="user-name-text text-truncate">{{ userDisplayName }}</span>
      <div class="avatar-circle">
        <img v-if="AuthStore.user?.avatarUrl" :src="AuthStore.user.avatarUrl" class="w-100 h-100" style="object-fit:cover;" />
        <User v-else :size="18" />
      </div>
    </button>

    <div
      v-if="showUserMenu" class="position-absolute shadow-lg overflow-hidden d-flex flex-column"
      style="width:260px; right:0; top:100%; margin-top:12px; background:var(--bg-card); border:1px solid var(--border-color); z-index:1050; border-radius: 12px;"
    >
      <div class="px-4 py-3" style="background:var(--bg-card-alt); border-bottom:1px solid var(--border-color);">
        <div class="fw-bold text-truncate" style="font-size:1.05rem;color:var(--text-heading);">{{ userDisplayName }}</div>
        <div style="font-size:0.75rem;color:var(--text-muted); text-transform: uppercase; font-weight: 600; margin-top: 4px; letter-spacing: 0.05em;">{{ userDisplayRole }}</div>
      </div>
      <div class="py-1">
        <button class="btn btn-sm w-100 text-start rounded-0 px-4 py-2 d-flex align-items-center" style="color:var(--text-primary); font-size:0.95rem; border:none; border-bottom: 1px solid var(--border-color); background:transparent;" @click="openEditProfileModal" onmouseover="this.style.background='var(--bg-hover)'" onmouseout="this.style.background='transparent'">
          <User class="me-3" style="color:var(--text-secondary);" :size="18" /> {{ t('admin.profileMenu.editProfile') }}
        </button>
        <button class="btn btn-sm w-100 text-start rounded-0 px-4 py-2 d-flex align-items-center" style="color:var(--text-primary); font-size:0.95rem; border:none; border-bottom: 1px solid var(--border-color); background:transparent;" @click="openQuickPasswordModal" onmouseover="this.style.background='var(--bg-hover)'" onmouseout="this.style.background='transparent'">
          <Key class="me-3" style="color:var(--text-secondary);" :size="18" /> {{ t('admin.settings.changePasswordTitle') }}
        </button>
        <button v-if="showSettingsLink" class="btn btn-sm w-100 text-start rounded-0 px-4 py-2 d-flex align-items-center" style="color:var(--text-primary); font-size:0.95rem; border:none; border-bottom: 1px solid var(--border-color); background:transparent;" @click="goToSettingsFromMenu" onmouseover="this.style.background='var(--bg-hover)'" onmouseout="this.style.background='transparent'">
          <Settings class="me-3" style="color:var(--text-secondary);" :size="18" /> {{ t('admin.sidebar.settings') }}
        </button>
      </div>
      <div class="p-3 pt-2">
        <button class="btn btn-sm w-100 fw-medium d-flex align-items-center justify-content-center" style="color:var(--danger); border: 1px solid var(--danger); background:transparent; padding: 8px 0; font-size: 0.95rem; border-radius: 8px;" @click="logout" onmouseover="this.style.background='var(--danger-light, rgba(220,38,38,0.1))'" onmouseout="this.style.background='transparent'">
          <LogOut class="me-2" style="color:var(--danger);" :size="18" /> {{ t('admin.sidebar.logout') }}
        </button>
      </div>
    </div>
  </div>

  <div v-if="showEditProfileModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:rgba(15,23,42,0.6); backdrop-filter: blur(4px); z-index:1070;" @click.self="showEditProfileModal=false">
    <div class="rounded-4 p-4 shadow-lg" style="background:var(--bg-card);width:420px;max-width:94vw; border: 1px solid var(--border-color);">
      <div class="d-flex justify-content-between align-items-center mb-4">
        <div class="fw-bold fs-5" style="color:var(--text-heading);">{{ t('admin.profileMenu.editProfile') }}</div>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="showEditProfileModal=false" style="filter: var(--btn-close-filter, none);"></button>
      </div>

      <div class="mb-4 d-flex flex-column align-items-center">
        <div class="position-relative" style="width:84px;height:84px;cursor:pointer;" @click="avatarInput.click()">
          <div class="rounded-circle d-flex align-items-center justify-content-center shadow-sm" style="width:100%;height:100%;background:var(--bg-input);border:2px dashed var(--border-color-strong);overflow:hidden; transition: all 0.2s;" onmouseover="this.style.borderColor='var(--accent)'" onmouseout="this.style.borderColor='var(--border-color-strong)'">
            <img v-if="avatarPreview" :src="avatarPreview" class="w-100 h-100" style="object-fit:cover;" />
            <User v-else :size="36" style="color:var(--text-muted);" />
          </div>
          <div class="position-absolute bottom-0 end-0 rounded-circle d-flex align-items-center justify-content-center shadow-sm" style="width:28px;height:28px; background:var(--accent); color:white; right: -4px !important; bottom: -4px !important;">
            <Camera :size="14" />
          </div>
        </div>
        <input ref="avatarInput" type="file" class="d-none" accept="image/*" @change="onAvatarChange" />
        <div class="mt-2 text-muted" style="font-size: 0.7rem;">(Tối đa 20MB)</div>
      </div>

      <div class="mb-3">
        <label class="form-label small fw-semibold text-secondary mb-1">{{ t('admin.profileMenu.fullName') }}</label>
        <input v-model="profileForm.hoTen" class="form-control" style="background:var(--bg-input);color:var(--text-primary);border:1px solid var(--border-color-soft);border-radius:8px;padding:10px 14px;" />
      </div>
      <div class="mb-3">
        <label class="form-label small fw-semibold text-secondary mb-1">{{ t('admin.profileMenu.phone') }}</label>
        <input v-model="profileForm.soDienThoai" class="form-control" style="background:var(--bg-input);color:var(--text-primary);border:1px solid var(--border-color-soft);border-radius:8px;padding:10px 14px;" />
      </div>
      <div class="mb-4">
        <label class="form-label small fw-semibold text-secondary mb-1">{{ t('admin.profileMenu.email') }}</label>
        <input v-model="profileForm.email" class="form-control" style="background:var(--bg-input);color:var(--text-primary);border:1px solid var(--border-color-soft);border-radius:8px;padding:10px 14px;" />
      </div>
      
      <div v-if="profileError" class="alert alert-danger py-2 small mb-3 border-0" style="background:rgba(220,38,38,0.1); color:#ef4444;">{{ profileError }}</div>
      <div v-if="profileSaved" class="alert alert-success py-2 small mb-3 border-0" style="background:rgba(34,197,94,0.1); color:#22c55e;">{{ t('admin.profileMenu.profileSaved') }}</div>
      
      <div class="d-flex justify-content-end gap-2 mt-2">
        <button class="btn btn-light px-4" style="border-radius: 8px; font-weight: 500; background: var(--bg-hover); color: var(--text-primary); border: none;" @click="showEditProfileModal=false">{{ t('admin.productModal.cancel') }}</button>
        <button class="btn text-white px-4 shadow-sm" style="border-radius: 8px; font-weight: 500; background: var(--gradient-brand); border: none;" :disabled="profileSaving" @click="saveProfile">{{ profileSaving ? 'Đang lưu...' : t('admin.settings.saveButton') }}</button>
      </div>
    </div>
  </div>

  <div v-if="showQuickPasswordModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:rgba(15,23,42,0.6); backdrop-filter: blur(4px); z-index:1070;" @click.self="showQuickPasswordModal=false">
    <div class="rounded-4 p-4 shadow-lg" style="background:var(--bg-card);width:420px;max-width:94vw; border: 1px solid var(--border-color);">
      <div class="d-flex justify-content-between align-items-center mb-4">
        <div class="fw-bold fs-5" style="color:var(--text-heading);">{{ t('admin.settings.changePasswordTitle') }}</div>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="showQuickPasswordModal=false" style="filter: var(--btn-close-filter, none);"></button>
      </div>
      
      <div class="mb-3">
        <label class="form-label small fw-semibold text-secondary mb-1">{{ t('admin.settings.currentPassword') }}</label>
        <input v-model="qpMatKhauCu" type="password" class="form-control" style="background:var(--bg-input);color:var(--text-primary);border:1px solid var(--border-color-soft);border-radius:8px;padding:10px 14px;" />
      </div>
      <div class="mb-3">
        <label class="form-label small fw-semibold text-secondary mb-1">{{ t('admin.settings.newPassword') }}</label>
        <input v-model="qpMatKhauMoi" type="password" class="form-control" style="background:var(--bg-input);color:var(--text-primary);border:1px solid var(--border-color-soft);border-radius:8px;padding:10px 14px;" />
      </div>
      <div class="mb-4">
        <label class="form-label small fw-semibold text-secondary mb-1">{{ t('admin.settings.confirmNewPassword') }}</label>
        <input v-model="qpMatKhauXacNhan" type="password" class="form-control" style="background:var(--bg-input);color:var(--text-primary);border:1px solid var(--border-color-soft);border-radius:8px;padding:10px 14px;" />
      </div>
      
      <div v-if="qpError" class="alert alert-danger py-2 small mb-3 border-0" style="background:rgba(220,38,38,0.1); color:#ef4444;">{{ qpError }}</div>
      <div v-if="qpSuccess" class="alert alert-success py-2 small mb-3 border-0" style="background:rgba(34,197,94,0.1); color:#22c55e;">{{ qpSuccess }}</div>
      
      <div class="d-flex justify-content-end gap-2 mt-2">
        <button class="btn btn-light px-4" style="border-radius: 8px; font-weight: 500; background: var(--bg-hover); color: var(--text-primary); border: none;" @click="showQuickPasswordModal=false">{{ t('admin.productModal.cancel') }}</button>
        <button class="btn text-white px-4 shadow-sm" style="border-radius: 8px; font-weight: 500; background: var(--gradient-brand); border: none;" :disabled="qpLoading" @click="quickChangePassword">{{ qpLoading ? 'Đang lưu...' : t('admin.settings.saveButton') }}</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.user-menu-trigger {
  background: var(--bg-input);
  border: 1px solid var(--border-color);
  border-radius: 50px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  width: 46px; /* 36px avatar + padding */
  overflow: hidden;
}

.user-menu-trigger:hover,
.user-menu-trigger.is-open {
  width: 180px; /* Expand to show text */
  background: var(--bg-card);
  transform: scale(1.02);
}

.user-name-text {
  font-size: 0.95rem;
  color: var(--text-primary);
  font-weight: 500;
  white-space: nowrap;
  opacity: 0;
  max-width: 0;
  transition: opacity 0.2s ease, max-width 0.3s ease, margin 0.3s ease;
  margin-right: 0;
  margin-left: 0;
}

.user-menu-trigger:hover .user-name-text,
.user-menu-trigger.is-open .user-name-text {
  opacity: 1;
  max-width: 140px;
  margin-right: 12px;
  margin-left: 12px;
}

.avatar-circle {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #64748b, #475569);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  overflow: hidden;
  flex-shrink: 0;
}
</style>
