<template>
  <!-- Form đăng ký — dùng trong modal chung với LoginForm (không có overlay riêng) -->
  <div class="mx-auto" style="max-width:420px;">

    <!-- Tiêu đề -->
    <div class="text-center mb-4">
      <div class="fw-black fs-5 mb-1" style="color:var(--text-heading);">{{ t('register.title') }}</div>
      <div class="small" style="color:var(--text-secondary);">{{ t('register.subtitle') }}</div>
    </div>

    <form @submit.prevent="onSubmit" class="d-flex flex-column gap-3" novalidate>

      <FormField :label="t('register.fullNameLabel')" :errors="errors.hoTen">
        <template #default="{ errors: fieldErr }">
          <input v-model="hoTen" type="text"
                 class="form-control form-control-sm"
                 :class="{ 'is-invalid': fieldErr }"
                 :placeholder="t('register.fullNamePlaceholder')" />
        </template>
      </FormField>

      <div class="row g-2">
        <div class="col-6">
          <FormField :label="t('register.phoneLabel')" :errors="errors.soDienThoai">
            <template #default="{ errors: fieldErr }">
              <input v-model="soDienThoai" type="text"
                     class="form-control form-control-sm"
                     :class="{ 'is-invalid': fieldErr }"
                     :placeholder="t('register.phonePlaceholder')" />
            </template>
          </FormField>
        </div>
        <div class="col-6">
          <FormField :label="t('register.emailLabel')" :errors="errors.email">
            <template #default="{ errors: fieldErr }">
              <input v-model="email" type="email"
                     class="form-control form-control-sm"
                     :class="{ 'is-invalid': fieldErr }"
                     :placeholder="t('register.emailPlaceholder')" />
            </template>
          </FormField>
        </div>
      </div>

      <FormField :label="t('register.usernameLabel')" :errors="errors.username">
        <template #default="{ errors: fieldErr }">
          <input v-model="username" type="text"
                 class="form-control form-control-sm"
                 :class="{ 'is-invalid': fieldErr }"
                 :placeholder="t('register.usernamePlaceholder')" />
        </template>
      </FormField>

      <div class="row g-2">
        <div class="col-6">
          <FormField :label="t('register.passwordLabel')" :errors="errors.password">
            <template #default="{ errors: fieldErr }">
              <div class="input-group input-group-sm">
                <input v-model="password" :type="showPassword ? 'text' : 'password'"
                       class="form-control form-control-sm"
                       :class="{ 'is-invalid': fieldErr }"
                       :placeholder="t('register.passwordPlaceholder')" />
                <button type="button" class="btn btn-sm"
                        style="background:var(--bg-input); border:1px solid var(--border-color-strong); border-left:none; color:var(--text-secondary);"
                        :title="showPassword ? t('register.hidePassword') : t('register.showPassword')"
                        @click="showPassword = !showPassword">
                  {{ showPassword ? '🙈' : '👁' }}
                </button>
              </div>
            </template>
          </FormField>
        </div>
        <div class="col-6">
          <FormField :label="t('register.confirmPasswordLabel')" :errors="errors.confirmPassword">
            <template #default="{ errors: fieldErr }">
              <div class="input-group input-group-sm">
                <input v-model="confirmPassword" :type="showConfirm ? 'text' : 'password'"
                       class="form-control form-control-sm"
                       :class="{ 'is-invalid': fieldErr }"
                       :placeholder="t('register.passwordPlaceholder')" />
                <button type="button" class="btn btn-sm"
                        style="background:var(--bg-input); border:1px solid var(--border-color-strong); border-left:none; color:var(--text-secondary);"
                        :title="showConfirm ? t('register.hidePassword') : t('register.showPassword')"
                        @click="showConfirm = !showConfirm">
                  {{ showConfirm ? '🙈' : '👁' }}
                </button>
              </div>
            </template>
          </FormField>
        </div>
      </div>

      <FormField :errors="errors.agree">
        <template #default="{ errors: fieldErr }">
          <label class="d-flex align-items-start gap-2 small" style="color:var(--text-secondary); cursor:pointer;">
            <input type="checkbox" v-model="agree" class="form-check-input mt-1" style="flex-shrink:0;" />
            <span>{{ t('register.agreeText') }}</span>
          </label>
          <div v-if="fieldErr" class="small text-danger mt-1">{{ fieldErr }}</div>
        </template>
      </FormField>

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
import { ref } from "vue";
import { useForm, useField } from "vee-validate";
import { toTypedSchema } from "@vee-validate/zod";
import { registerSchema } from "../../utils/validators.js";
import { t } from "../../i18n/index.js";
import * as KhachHangService from "../../services/KhachHangService.js";
import FormField from "../common/FormField.vue";

const emit = defineEmits(["register-success", "open-login"]);

const loading = ref(false);
const error   = ref("");
const success = ref("");

const showPassword = ref(false);
const showConfirm  = ref(false);

const { handleSubmit, errors, resetForm } = useForm({
  validationSchema: toTypedSchema(registerSchema),
});

const { value: hoTen } = useField('hoTen');
const { value: soDienThoai } = useField('soDienThoai');
const { value: email } = useField('email');
const { value: username } = useField('username');
const { value: password } = useField('password');
const { value: confirmPassword } = useField('confirmPassword');
const { value: agree } = useField('agree');

const onSubmit = handleSubmit(async (values) => {
  if (!values.agree) {
    error.value = t('register.errors.mustAgree');
    return;
  }
  error.value = "";
  success.value = "";

  loading.value = true;
  try {
    const { confirmPassword: _, agree: __, ...body } = values;
    const res = await KhachHangService.register(body);
    if (!res.ok) {
      error.value = await res.text() || t('register.errors.registerFailed');
      return;
    }
    const newAccount = await res.json();
    success.value = t('register.success');
    resetForm();
    agree.value = false;
    setTimeout(() => emit('register-success', newAccount), 1200);
  } catch {
    error.value = t('register.errors.cannotConnect');
  } finally {
    loading.value = false;
  }
});
</script>
