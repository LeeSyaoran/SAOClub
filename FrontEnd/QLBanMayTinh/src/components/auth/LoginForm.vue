<template>
  <!-- Form đăng nhập — dùng trong modal hoặc trang riêng -->
  <div class="mx-auto" style="max-width:420px;">

    <!-- Tiêu đề -->
    <div class="text-center mb-4">
      <div class="fw-black fs-5 mb-1" style="color:var(--text-heading);">{{ t('login.welcome') }}</div>
      <div class="small" style="color:var(--text-secondary);">{{ t('login.subtitle') }}</div>
    </div>

    <!-- Form -->
    <form @submit.prevent="handleSubmit" class="d-flex flex-column gap-3">

      <!-- Username / Email -->
      <div>
        <label class="form-label small fw-semibold" style="color:var(--text-secondary);">{{ t('login.usernameLabel') }}</label>
        <input v-model="form.username" type="text"
               class="form-control form-control-sm"
               style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-primary);"
               :placeholder="t('login.usernamePlaceholder')" required />
      </div>

      <!-- Mật khẩu -->
      <div>
        <label class="form-label small fw-semibold" style="color:var(--text-secondary);">{{ t('login.passwordLabel') }}</label>
        <div class="input-group input-group-sm">
          <input v-model="form.password" :type="showPassword ? 'text' : 'password'"
                 class="form-control form-control-sm"
                 style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-primary);"
                 :placeholder="t('login.passwordPlaceholder')" required />
          <button type="button" class="btn btn-sm"
                  style="background:var(--bg-input); border:1px solid var(--border-color-strong); border-left:none; color:var(--text-secondary);"
                  :title="showPassword ? t('register.hidePassword') : t('register.showPassword')"
                  @click="showPassword = !showPassword">
            {{ showPassword ? '🙈' : '👁' }}
          </button>
        </div>
        <div class="text-end mt-1">
          <a href="#" class="text-warning small fw-semibold text-decoration-none">{{ t('login.forgotPassword') }}</a>
        </div>
      </div>

      <!-- Thông báo lỗi -->
      <div v-if="error" class="alert alert-danger small py-2 mb-0">{{ error }}</div>

      <!-- Nút đăng nhập -->
      <button type="submit" class="btn btn-warning text-dark fw-black w-100">
        {{ t('login.submit') }}
      </button>
    </form>

    <!-- Chuyển sang đăng ký -->
    <div class="text-center mt-4 pt-3 border-top small" style="border-color:var(--border-color)!important; color:var(--text-secondary);">
      {{ t('login.noAccount') }}
      <button
          type="button"
          class="btn btn-link btn-sm text-warning fw-bold p-0 text-decoration-none"
          @click="emit('open-register')"
      >
        {{ t('login.registerNow') }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { t } from '../../i18n/index.js';

// Emit: submit (trả về { username, password }), register-click (chuyển tab đăng ký)
const emit = defineEmits([
  "submit",
  "login-success",
  "close",
  "open-register"
]);

const form = reactive({ username: '', password: '' });
const error = ref('');
const showPassword = ref(false);
const handleSubmit = () => {
  error.value = '';
  if (!form.username || !form.password) {
    error.value = t('login.fillAllFields');
    return;
  }
  emit('submit', { username: form.username, password: form.password });
};
</script>
