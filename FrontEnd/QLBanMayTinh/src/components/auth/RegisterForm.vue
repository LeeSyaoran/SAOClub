<template>
  <div class="mx-auto" style="max-width:420px;">
    <!-- Tiêu đề -->
    <div class="text-center mb-4">
      <div class="fw-black fs-5 mb-1" style="color:var(--text-heading);">{{ t('register.title') }}</div>
      <div class="small" style="color:var(--text-secondary);">{{ t('register.subtitle') }}</div>
    </div>

    <!-- Form -->
    <form class="d-flex flex-column gap-3" @submit.prevent="onSubmit">
      <!-- Họ và tên -->
      <FormField :label="t('register.fullNameLabel')" :errors="errors.hoTen" :meta="meta">
        <template #default="{ errors: fieldErr }">
          <input
            v-model="hoTen" type="text"
            class="form-control form-control-sm"
            :class="{ 'is-invalid': fieldErr }"
            style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-primary);"
            :placeholder="t('register.fullNamePlaceholder')"
          />
        </template>
      </FormField>

      <!-- Số điện thoại + Email -->
      <div class="row g-2">
        <div class="col-6">
          <FormField :label="t('register.phoneLabel')" :errors="errors.soDienThoai" :meta="meta">
            <template #default="{ errors: fieldErr }">
              <input
                v-model="soDienThoai" type="text"
                class="form-control form-control-sm"
                :class="{ 'is-invalid': fieldErr }"
                style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-primary);"
                :placeholder="t('register.phonePlaceholder')"
              />
            </template>
          </FormField>
        </div>
        <div class="col-6">
          <FormField :label="t('register.emailLabel')" :errors="errors.email" :meta="meta">
            <template #default="{ errors: fieldErr }">
              <input
                v-model="email" type="email"
                class="form-control form-control-sm"
                :class="{ 'is-invalid': fieldErr }"
                style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-primary);"
                :placeholder="t('register.emailPlaceholder')"
              />
            </template>
          </FormField>
        </div>
      </div>

      <!-- Tên đăng nhập -->
      <FormField :label="t('register.usernameLabel')" :errors="errors.username" :meta="meta">
        <template #default="{ errors: fieldErr }">
          <input
            v-model="username" type="text"
            class="form-control form-control-sm"
            :class="{ 'is-invalid': fieldErr }"
            style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-primary);"
            :placeholder="t('register.usernamePlaceholder')"
          />
        </template>
      </FormField>

      <!-- Mật khẩu + Xác nhận -->
      <div class="row g-2">
        <div class="col-6">
          <FormField :label="t('register.passwordLabel')" :errors="errors.password" :meta="meta">
            <template #default="{ errors: fieldErr }">
              <div class="input-group input-group-sm">
                <input
                  v-model="password" :type="showPassword ? 'text' : 'password'"
                  class="form-control form-control-sm"
                  :class="{ 'is-invalid': fieldErr }"
                  style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-primary);"
                  :placeholder="t('register.passwordPlaceholder')"
                />
                <button
                  type="button" class="btn btn-sm"
                  style="background:var(--bg-input); border:1px solid var(--border-color-strong); border-left:none; color:var(--text-secondary);"
                  @click="showPassword = !showPassword"
                >
                  <component :is="showPassword ? EyeOff : Eye" :size="14" />
                </button>
              </div>
            </template>
          </FormField>
        </div>
        <div class="col-6">
          <FormField :label="t('register.confirmPasswordLabel')" :errors="errors.confirmPassword" :meta="meta">
            <template #default="{ errors: fieldErr }">
              <div class="input-group input-group-sm">
                <input
                  v-model="confirmPassword" :type="showPassword ? 'text' : 'password'"
                  class="form-control form-control-sm"
                  :class="{ 'is-invalid': fieldErr }"
                  style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-primary);"
                  :placeholder="t('register.confirmPasswordPlaceholder')"
                />
                <button
                  type="button" class="btn btn-sm"
                  style="background:var(--bg-input); border:1px solid var(--border-color-strong); border-left:none; color:var(--text-secondary);"
                  @click="showPassword = !showPassword"
                >
                  <component :is="showPassword ? EyeOff : Eye" :size="14" />
                </button>
              </div>
            </template>
          </FormField>
        </div>
      </div>

      <!-- Đồng ý điều khoản -->
      <FormField :errors="errors.terms" :meta="meta">
        <template #default="{ errors: fieldErr }">
          <div class="form-check small" :class="{ 'is-invalid': fieldErr }">
            <input
              v-model="terms" type="checkbox" class="form-check-input" id="register-terms"
            />
            <label class="form-check-label" for="register-terms" style="color:var(--text-secondary);">
              {{ t('register.termsPrefix') }}
              <a href="#" class="text-warning fw-bold text-decoration-none">{{ t('register.termsLink') }}</a>
              {{ t('register.termsAnd') }}
              <a href="#" class="text-warning fw-bold text-decoration-none">{{ t('register.privacyLink') }}</a>
            </label>
          </div>
        </template>
      </FormField>

      <!-- Thông báo lỗi -->
      <div v-if="error" class="alert alert-danger small py-2 mb-0">{{ error }}</div>

      <!-- Nút đăng ký -->
      <button
        type="submit"
        class="btn w-100 fw-black text-white d-flex align-items-center justify-content-center gap-2"
        style="background:linear-gradient(135deg, #ec4899, #be185d); border:none;"
        :disabled="submitting"
      >
        <span v-if="submitting" class="spinner-border spinner-border-sm"></span>
        {{ submitting ? t('register.submitting', 'Đang đăng ký...') : t('register.submit') }}
      </button>
    </form>

    <!-- Chuyển sang đăng nhập -->
    <div class="text-center mt-4 pt-3 border-top small" style="border-color:var(--border-color)!important; color:var(--text-secondary);">
      {{ t('register.hasAccount', t('register.haveAccount', 'Đã có tài khoản?')) }}
      <button
        type="button"
        class="btn btn-link btn-sm text-warning fw-bold p-0 text-decoration-none"
        @click="emit('open-login')"
      >
        {{ t('register.loginNow') }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useForm, useField } from 'vee-validate';
import { toTypedSchema } from '@vee-validate/zod';
import { registerSchema } from '../../utils/validators.js';
import { t } from '../../i18n/index.js';
import FormField from '../common/FormField.vue';
import { Eye, EyeOff } from '@lucide/vue';

const emit = defineEmits(["submit", "register-success", "close", "open-login"]);

const error = ref('');
const showPassword = ref(false);
const submitting = ref(false);

const { handleSubmit, errors, meta } = useForm({
  validationSchema: toTypedSchema(registerSchema),
});

const { value: hoTen } = useField('hoTen');
const { value: soDienThoai } = useField('soDienThoai');
const { value: email } = useField('email');
const { value: username } = useField('username');
const { value: password } = useField('password');
const { value: confirmPassword } = useField('confirmPassword');
const { value: terms } = useField('terms');

const onSubmit = handleSubmit((values) => {
  error.value = '';
  submitting.value = true;
  emit('submit', values);
  // Reset after 1.5s if parent doesn't close modal (means error)
  setTimeout(() => { submitting.value = false; }, 1500);
});
</script>