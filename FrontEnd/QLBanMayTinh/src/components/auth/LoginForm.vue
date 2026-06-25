<template>
  <!-- Form đăng nhập — dùng trong modal hoặc trang riêng -->
  <div class="mx-auto" style="max-width:420px;">

    <!-- Tiêu đề -->
    <div class="text-center mb-4">
      <div class="fw-black fs-5 mb-1">Chào mừng trở lại!</div>
      <div class="text-secondary small">Đăng nhập tài khoản hệ thống SAOPhone</div>
    </div>

    <!-- Form -->
    <form @submit.prevent="handleSubmit" class="d-flex flex-column gap-3">

      <!-- Username / Email -->
      <div>
        <label class="form-label small text-secondary fw-semibold">Tên tài khoản hoặc Email</label>
        <input v-model="form.username" type="text"
               class="form-control form-control-sm"
               style="background:#1a1a1a; border-color:rgba(255,255,255,0.12); color:#eee;"
               placeholder="Gõ tên đăng nhập hoặc email..." required />
      </div>

      <!-- Mật khẩu -->
      <div>
        <label class="form-label small text-secondary fw-semibold">Mật khẩu</label>
        <input v-model="form.password" type="password"
               class="form-control form-control-sm"
               style="background:#1a1a1a; border-color:rgba(255,255,255,0.12); color:#eee;"
               placeholder="••••••••" required />
        <div class="text-end mt-1">
          <a href="#" class="text-warning small fw-semibold text-decoration-none">Quên mật khẩu?</a>
        </div>
      </div>

      <!-- Thông báo lỗi -->
      <div v-if="error" class="alert alert-danger small py-2 mb-0">{{ error }}</div>

      <!-- Nút đăng nhập -->
      <button type="submit" class="btn btn-warning text-dark fw-black w-100">
        ĐĂNG NHẬP HỆ THỐNG
      </button>
    </form>

    <!-- Chuyển sang đăng ký -->
    <div class="text-center mt-4 pt-3 border-top border-secondary small text-secondary">
      Chưa có tài khoản?
      <button
          type="button"
          class="btn btn-link btn-sm text-warning fw-bold p-0 text-decoration-none"
          @click="emit('open-register')"
      >
        Đăng ký ngay
      </button>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';

// Emit: submit (trả về { username, password }), register-click (chuyển tab đăng ký)
const emit = defineEmits([
  "submit",
  "login-success",
  "close",
  "open-register"
]);

const form = reactive({ username: '', password: '' });
const error = ref('');
const handleSubmit = () => {
  error.value = '';
  if (!form.username || !form.password) {
    error.value = 'Vui lòng điền đầy đủ thông tin.';
    return;
  }
  emit('submit', { username: form.username, password: form.password });
};
</script>
