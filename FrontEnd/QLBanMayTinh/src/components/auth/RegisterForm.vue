<template>
  <!-- Form đăng ký — dùng trong modal chung với LoginForm (không có overlay riêng) -->
  <div class="mx-auto" style="max-width:420px;">

    <!-- Tiêu đề -->
    <div class="text-center mb-4">
      <div class="fw-black fs-5 mb-1" style="color:var(--text-heading);">{{ t('register.title') }}</div>
      <div class="small" style="color:var(--text-secondary);">{{ t('register.subtitle') }}</div>
    </div>

    <form @submit.prevent="handleSubmit" class="d-flex flex-column gap-3" novalidate>

      <div>
        <label class="form-label small fw-semibold" style="color:var(--text-secondary);">{{ t('register.fullNameLabel') }}</label>
        <input v-model="form.hoTen" type="text"
               class="form-control form-control-sm"
               :style="fieldStyle(hoTenValid, touched.hoTen)"
               :placeholder="t('register.fullNamePlaceholder')" required
               @blur="touched.hoTen = true" />
        <div v-if="touched.hoTen && !hoTenValid" class="small text-danger mt-1">{{ t('register.errors.required') }}</div>
      </div>

      <div class="row g-2">
        <div class="col-6">
          <label class="form-label small fw-semibold" style="color:var(--text-secondary);">{{ t('register.phoneLabel') }}</label>
          <input v-model="form.soDienThoai" type="text"
                 class="form-control form-control-sm"
                 :style="fieldStyle(phoneValid, touched.soDienThoai)"
                 :placeholder="t('register.phonePlaceholder')" required
                 @blur="touched.soDienThoai = true" />
          <div v-if="touched.soDienThoai && !phoneValid" class="small text-danger mt-1">{{ t('register.errors.invalidPhone') }}</div>
        </div>
        <div class="col-6">
          <label class="form-label small fw-semibold" style="color:var(--text-secondary);">{{ t('register.emailLabel') }}</label>
          <input v-model="form.email" type="email"
                 class="form-control form-control-sm"
                 :style="fieldStyle(emailValid, touched.email)"
                 :placeholder="t('register.emailPlaceholder')" required
                 @blur="touched.email = true" />
          <div v-if="touched.email && !emailValid" class="small text-danger mt-1">{{ t('register.errors.invalidEmail') }}</div>
        </div>
      </div>

      <div>
        <label class="form-label small fw-semibold" style="color:var(--text-secondary);">{{ t('register.usernameLabel') }}</label>
        <input v-model="form.username" type="text"
               class="form-control form-control-sm"
               :style="fieldStyle(usernameValid, touched.username)"
               :placeholder="t('register.usernamePlaceholder')" required
               @blur="touched.username = true" />
        <div v-if="touched.username && !usernameValid" class="small text-danger mt-1">{{ t('register.errors.usernameTooShort') }}</div>
      </div>

      <div class="row g-2">
        <div class="col-6">
          <label class="form-label small fw-semibold" style="color:var(--text-secondary);">{{ t('register.passwordLabel') }}</label>
          <div class="input-group input-group-sm">
            <input v-model="form.password" :type="showPassword ? 'text' : 'password'"
                   class="form-control form-control-sm"
                   :style="fieldStyle(passwordValid, touched.password)"
                   :placeholder="t('register.passwordPlaceholder')" required
                   @blur="touched.password = true" />
            <button type="button" class="btn btn-sm"
                    style="background:var(--bg-input); border:1px solid var(--border-color-strong); border-left:none; color:var(--text-secondary);"
                    :title="showPassword ? t('register.hidePassword') : t('register.showPassword')"
                    @click="showPassword = !showPassword">
              {{ showPassword ? '🙈' : '👁' }}
            </button>
          </div>
          <div v-if="touched.password && !passwordValid" class="small text-danger mt-1">{{ t('register.errors.passwordTooShort') }}</div>
        </div>
        <div class="col-6">
          <label class="form-label small fw-semibold" style="color:var(--text-secondary);">{{ t('register.confirmPasswordLabel') }}</label>
          <div class="input-group input-group-sm">
            <input v-model="confirmPassword" :type="showConfirm ? 'text' : 'password'"
                   class="form-control form-control-sm"
                   :style="fieldStyle(confirmValid, touched.confirmPassword)"
                   :placeholder="t('register.passwordPlaceholder')" required
                   @blur="touched.confirmPassword = true" />
            <button type="button" class="btn btn-sm"
                    style="background:var(--bg-input); border:1px solid var(--border-color-strong); border-left:none; color:var(--text-secondary);"
                    :title="showConfirm ? t('register.hidePassword') : t('register.showPassword')"
                    @click="showConfirm = !showConfirm">
              {{ showConfirm ? '🙈' : '👁' }}
            </button>
          </div>
          <div v-if="touched.confirmPassword && !confirmValid" class="small text-danger mt-1">{{ t('register.errors.passwordMismatch') }}</div>
        </div>
      </div>

      <label class="d-flex align-items-start gap-2 small" style="color:var(--text-secondary); cursor:pointer;">
        <input type="checkbox" v-model="agree" class="form-check-input mt-1" style="flex-shrink:0;" />
        <span>{{ t('register.agreeText') }}</span>
      </label>

      <div v-if="error" class="alert alert-danger small py-2 mb-0">{{ error }}</div>
      <div v-if="success" class="alert alert-success small py-2 mb-0">{{ success }}</div>

      <button type="submit" class="btn btn-warning text-dark fw-black w-100" :disabled="loading">
        {{ loading ? t('register.submitting') : t('register.submit') }}
      </button>
    </form>

    <div class="text-center mt-4 pt-3 border-top small" style="border-color:var(--border-color)!important; color:var(--text-secondary);">
      {{ t('register.haveAccount') }}
      <button type="button" class="btn btn-link btn-sm text-warning fw-bold p-0 text-decoration-none" @click="emit('open-login')">
        {{ t('register.loginNow') }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed } from "vue";
import { t } from "../../i18n/index.js";
import * as KhachHangService from "../../Service/KhachHangService.js";

const emit = defineEmits(["register-success", "open-login"]);

const form = reactive({
  hoTen: "",
  soDienThoai: "",
  email: "",
  username: "",
  password: "",
});
const confirmPassword = ref("");
const agree   = ref(false);
const loading = ref(false);
const error   = ref("");
const success = ref("");

// Hiện/ẩn mật khẩu để người dùng tự kiểm tra lại đã gõ đúng chưa
const showPassword = ref(false);
const showConfirm  = ref(false);

// Đánh dấu field đã bị rời khỏi (blur) lần nào chưa — chỉ hiện lỗi sau khi người dùng đã tương tác
const touched = reactive({
  hoTen: false, soDienThoai: false, email: false,
  username: false, password: false, confirmPassword: false,
});

const EMAIL_RE = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
const PHONE_RE = /^[0-9]{10}$/;

// ── Kiểm tra hợp lệ theo từng ô, cập nhật ngay khi gõ ──────────────────────────
const hoTenValid    = computed(() => form.hoTen.trim().length > 0);
const phoneValid    = computed(() => PHONE_RE.test(form.soDienThoai));
const emailValid    = computed(() => EMAIL_RE.test(form.email));
const usernameValid = computed(() => form.username.trim().length >= 3);
const passwordValid = computed(() => form.password.length >= 6);
const confirmValid  = computed(() => confirmPassword.value.length > 0 && confirmPassword.value === form.password);

// Viền ô input đổi màu theo trạng thái hợp lệ, chỉ sau khi đã touched
const fieldStyle = (isValid, isTouched) => {
  const base = 'background:var(--bg-input); color:var(--text-primary);';
  if (!isTouched) return `${base} border-color:var(--border-color-strong);`;
  return isValid ? `${base} border-color:#22c55e;` : `${base} border-color:#f87171;`;
};

const handleSubmit = async () => {
  error.value = "";
  success.value = "";

  // Đánh dấu tất cả field đã touched để hiện đủ lỗi khi bấm submit sớm
  Object.keys(touched).forEach(k => { touched[k] = true; });

  if (!form.hoTen || !form.soDienThoai || !form.email || !form.username || !form.password) {
    error.value = t('register.errors.fillAllFields');
    return;
  }
  if (!emailValid.value) {
    error.value = t('register.errors.invalidEmail');
    return;
  }
  if (!phoneValid.value) {
    error.value = t('register.errors.invalidPhone');
    return;
  }
  if (!usernameValid.value) {
    error.value = t('register.errors.usernameTooShort');
    return;
  }
  if (!passwordValid.value) {
    error.value = t('register.errors.passwordTooShort');
    return;
  }
  if (!confirmValid.value) {
    error.value = t('register.errors.passwordMismatch');
    return;
  }
  if (!agree.value) {
    error.value = t('register.errors.mustAgree');
    return;
  }

  loading.value = true;
  try {
    const res = await KhachHangService.register({ ...form });
    if (!res.ok) {
      error.value = await res.text() || t('register.errors.registerFailed');
      return;
    }
    success.value = t('register.success');
    Object.assign(form, { hoTen: "", soDienThoai: "", email: "", username: "", password: "" });
    confirmPassword.value = "";
    agree.value = false;
    Object.keys(touched).forEach(k => { touched[k] = false; });
    setTimeout(() => emit('register-success'), 1200);
  } catch {
    error.value = t('register.errors.cannotConnect');
  } finally {
    loading.value = false;
  }
};
</script>
