<template>
  <div class="login-wrapper">
    <!-- Brand Header -->
    <div class="login-brand text-center mb-4">
      <div class="brand-icon mb-3">
        <svg width="56" height="56" viewBox="0 0 56 56" fill="none" xmlns="http://www.w3.org/2000/svg">
          <defs>
            <linearGradient id="brandGrad" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#7c3aed"/>
              <stop offset="100%" stop-color="#f43f5e"/>
            </linearGradient>
          </defs>
          <circle cx="28" cy="28" r="26" stroke="url(#brandGrad)" stroke-width="3" fill="none"/>
          <path d="M20 28L24 32L36 20" stroke="url(#brandGrad)" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </div>
      <h1 class="brand-name mb-1">{{ t('login.welcome') }}</h1>
      <p class="brand-subtitle mb-0">{{ t('login.subtitle') }}</p>
    </div>

    <!-- Login Form -->
    <form class="login-form" @submit.prevent="onSubmit">
      <!-- Username -->
      <div class="form-group-custom">
        <label class="form-label-custom">
          <User :size="16" class="me-1"/>
          {{ t('login.usernameLabel') }}
        </label>
        <div class="input-wrapper">
          <input
            v-model="username"
            type="text"
            class="form-input"
            :class="{ 'is-invalid': errors.username }"
            :placeholder="t('login.usernamePlaceholder')"
            @keydown.enter.prevent="passwordRef?.focus()"
          />
          <div v-if="errors.username" class="error-message">{{ errors.username }}</div>
        </div>
      </div>

      <!-- Password -->
      <div class="form-group-custom">
        <label class="form-label-custom">
          <Lock :size="16" class="me-1"/>
          {{ t('login.passwordLabel') }}
        </label>
        <div class="input-wrapper">
          <input
            ref="passwordRef"
            v-model="password"
            :type="showPassword ? 'text' : 'password'"
            class="form-input"
            :class="{ 'is-invalid': errors.password }"
            :placeholder="t('login.passwordPlaceholder')"
            @keydown.enter.prevent="onSubmit"
          />
          <button
            type="button"
            class="btn-toggle-password"
            :title="showPassword ? t('register.hidePassword') : t('register.showPassword')"
            @click="showPassword = !showPassword"
          >
            <component :is="showPassword ? EyeOff : Eye" :size="18" />
          </button>
          <div v-if="errors.password" class="error-message">{{ errors.password }}</div>
        </div>
      </div>

      <!-- Forgot Password -->
      <div class="form-options">
        <button type="button" class="link-forgot" @click="handleForgotPassword">{{ t('login.forgotPassword') }}</button>
      </div>

      <!-- Error Alert -->
      <Transition name="fade">
        <div v-if="error" class="alert-error">
          <AlertCircle :size="16" class="me-2"/>
          {{ error }}
        </div>
      </Transition>

      <!-- Submit Button -->
      <button type="submit" class="btn-submit" :class="{ loading: submitting }">
        <span v-if="!submitting">{{ t('login.submit') }}</span>
        <span v-else class="d-flex align-items-center justify-content-center gap-2">
          <span class="spinner-border spinner-border-sm"></span>
          {{ t('login.submitting', 'Đang đăng nhập...') }}
        </span>
      </button>
    </form>

    <!-- Divider -->
    <div class="divider">
      <span class="divider-text">{{ t('login.orLoginWith', 'Hoặc đăng nhập với') }}</span>
    </div>

    <!-- Social Login -->
    <div class="social-login">
      <button
        type="button"
        class="btn-social btn-google"
        :disabled="loadingSocial"
        @click="handleGoogleLogin"
      >
        <span v-if="socialLoading === 'google'" class="spinner-border spinner-border-sm"></span>
        <svg v-else width="20" height="20" viewBox="0 0 24 24">
          <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
          <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
          <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/>
          <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
        </svg>
        <span>Google</span>
      </button>

      <button
        type="button"
        class="btn-social btn-facebook"
        :disabled="loadingSocial"
        @click="handleFacebookLogin"
      >
        <span v-if="socialLoading === 'facebook'" class="spinner-border spinner-border-sm"></span>
        <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="#1877F2">
          <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/>
        </svg>
        <span>Facebook</span>
      </button>
    </div>

    <!-- Register Link -->
    <div class="register-prompt">
      <span>{{ t('login.noAccount') }}</span>
      <button type="button" class="link-register" @click="emit('open-register')">
        {{ t('login.registerNow') }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useForm, useField } from 'vee-validate';
import { toTypedSchema } from '@vee-validate/zod';
import { loginSchema } from '../../utils/validators.js';
import { t } from '../../i18n/index.js';
import { Eye, EyeOff, User, Lock, AlertCircle } from '@lucide/vue';
import { signInWithGoogle, signInWithFacebook } from '../../firebase.js';
import { showToast } from '../../stores/toast.js';

const emit = defineEmits(["submit", "login-success", "close", "open-register", "social-success"]);

const error = ref('');
const showPassword = ref(false);

const handleForgotPassword = () => {
  showToast("Tính năng đang phát triển. Vui lòng liên hệ hotline để hỗ trợ.", "info");
};
const loadingSocial = ref(false);
const socialLoading = ref('');
const submitting = ref(false);

const { handleSubmit, errors } = useForm({
  validationSchema: toTypedSchema(loginSchema),
});

const { value: username } = useField('username');
const { value: password } = useField('password');
const passwordRef = ref(null);

const onSubmit = handleSubmit((values) => {
  error.value = '';
  submitting.value = true;
  emit('submit', values);
  // Reset submitting state after a delay (parent will handle actual navigation)
  setTimeout(() => { submitting.value = false; }, 1500);
});

// Social Login: Google — popup flow
const handleGoogleLogin = async () => {
  loadingSocial.value = true;
  socialLoading.value = 'google';
  error.value = '';
  try {
    const result = await signInWithGoogle();
    emit('social-success', result);
  } catch (err) {
    error.value = 'Đăng nhập Google thất bại. Vui lòng thử lại.';
    loadingSocial.value = false;
    socialLoading.value = '';
  }
};

// Social Login: Facebook — popup flow
const handleFacebookLogin = async () => {
  loadingSocial.value = true;
  socialLoading.value = 'facebook';
  error.value = '';
  try {
    const result = await signInWithFacebook();
    emit('social-success', result);
  } catch (err) {
    error.value = 'Đăng nhập Facebook thất bại. Vui lòng thử lại.';
    loadingSocial.value = false;
    socialLoading.value = '';
  }
};
</script>

<style scoped>
/* ── Login Wrapper ─────────────────────────────────────────── */
.login-wrapper {
  padding: 8px;
}

/* ── Brand Header ─────────────────────────────────────────── */
.login-brand {
  margin-bottom: 24px;
}

.brand-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.18), rgba(244, 63, 94, 0.18));
  border-radius: 20px;
  border: 1.5px solid #f3b8d0;
  box-shadow: 0 4px 12px rgba(225, 29, 72, 0.12);
}

.brand-name {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-heading);
  margin: 0 0 4px 0;
  letter-spacing: -0.02em;
}

.brand-subtitle {
  font-size: 0.9rem;
  color: var(--text-secondary);
}

/* ── Form ─────────────────────────────────────────────────── */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-group-custom {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label-custom {
  display: flex;
  align-items: center;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--text-primary);
}

.input-wrapper {
  position: relative;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  padding-right: 44px;
  background: #ffffff;
  border: 1.5px solid #dba9c7;
  border-radius: 10px;
  color: var(--text-primary);
  font-size: 0.95rem;
  transition: all 0.2s ease;
  box-shadow: var(--shadow-inset);
}

.form-input::placeholder {
  color: var(--text-muted);
}

.form-input:focus {
  outline: none;
  border-color: var(--accent);
  box-shadow: var(--shadow-inset), 0 0 0 3px rgba(225, 29, 72, 0.2);
}

.form-input.is-invalid {
  border-color: var(--state-danger);
  box-shadow: var(--shadow-inset), 0 0 0 3px rgba(220, 38, 38, 0.15);
}

.btn-toggle-password {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: #e11d48;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s;
}

.btn-toggle-password:hover {
  color: var(--text-primary);
}

.error-message {
  font-size: 0.8rem;
  color: var(--state-danger);
  margin-top: 4px;
  padding-left: 4px;
}

/* ── Options ──────────────────────────────────────────────── */
.form-options {
  display: flex;
  justify-content: flex-end;
  margin-top: -4px;
}

.link-forgot {
  font-size: 0.85rem;
  color: var(--accent);
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s;
}

.link-forgot:hover {
  color: var(--accent-2);
}

/* ── Error Alert ──────────────────────────────────────────── */
.alert-error {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: rgba(220, 38, 38, 0.1);
  border: 1px solid rgba(220, 38, 38, 0.3);
  border-radius: 10px;
  color: var(--state-danger);
  font-size: 0.9rem;
  font-weight: 500;
}

/* ── Submit Button ────────────────────────────────────────── */
.btn-submit {
  width: 100%;
  padding: 14px 24px;
  background: linear-gradient(135deg, #7c3aed, #f43f5e);
  border: none;
  border-radius: 10px;
  color: white;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: 0 4px 15px rgba(225, 29, 72, 0.35);
  letter-spacing: 0.02em;
}

.btn-submit:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(225, 29, 72, 0.45);
}

.btn-submit:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(225, 29, 72, 0.3);
}

.btn-submit:disabled,
.btn-submit.loading {
  opacity: 0.85;
  cursor: not-allowed;
}

/* ── Divider ──────────────────────────────────────────────── */
.divider {
  display: flex;
  align-items: center;
  gap: 16px;
  margin: 24px 0;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--border-color), transparent);
}

.divider-text {
  font-size: 0.85rem;
  color: var(--text-muted);
  white-space: nowrap;
}

/* ── Social Login ─────────────────────────────────────────── */
.social-login {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.btn-social {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 12px 16px;
  background: #ffffff;
  border: 1.5px solid #dba9c7;
  border-radius: 10px;
  color: var(--text-primary);
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: var(--shadow-sm);
}

.btn-social:hover:not(:disabled) {
  background: #fff5fa;
  border-color: var(--accent);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.btn-social:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-social span {
  display: flex;
  align-items: center;
}

/* ── Register Prompt ──────────────────────────────────────── */
.register-prompt {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--border-color);
  font-size: 0.9rem;
  color: var(--text-secondary);
}

.link-register {
  background: none;
  border: none;
  color: var(--accent);
  font-weight: 600;
  cursor: pointer;
  padding: 0;
  font-size: 0.9rem;
  transition: color 0.2s;
}

.link-register:hover {
  color: var(--accent-2);
  text-decoration: underline;
}

/* ── Transitions ─────────────────────────────────────────── */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
