<template>
  <div v-if="modelValue" class="profile-modal-overlay" @click.self="emit('update:modelValue', false)">
    <div class="profile-modal-box">
      <div class="profile-modal-header">
        <h5 class="mb-0">Hoàn tất hồ sơ</h5>
      </div>

      <p class="text-muted small mb-3">
        Vui lòng cung cấp thông tin để hoàn tất đăng nhập
      </p>

      <form @submit.prevent="handleSubmit">
        <div class="mb-3">
          <label class="form-label small fw-semibold">Họ tên <span class="text-danger">*</span></label>
          <input
            v-model="form.hoTen"
            type="text"
            class="form-control"
            placeholder="Nhập họ tên của bạn"
            required
            autofocus
          />
        </div>

        <div class="mb-3">
          <label class="form-label small fw-semibold">Số điện thoại <span class="text-danger">*</span></label>
          <input
            v-model="form.soDienThoai"
            type="tel"
            class="form-control"
            placeholder="Nhập số điện thoại"
            pattern="[0-9]{9,11}"
            required
          />
        </div>

        <div v-if="error" class="alert alert-danger py-2 small mb-3">{{ error }}</div>

        <button type="submit" class="btn btn-warning w-100 fw-bold" :disabled="loading">
          <span v-if="loading" class="spinner-border spinner-border-sm me-1"></span>
          {{ loading ? 'Đang xử lý...' : 'Xác nhận' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch } from 'vue';
import { updateProfile, getCurrentUser } from '../../services/AuthService.js';

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  defaultName: { type: String, default: '' },
  defaultEmail: { type: String, default: '' },
  avatarUrl: { type: String, default: '' },
});

const emit = defineEmits(['update:modelValue', 'success']);

const form = reactive({ hoTen: '', soDienThoai: '' });
const loading = ref(false);
const error = ref('');

watch(() => props.modelValue, (open) => {
  if (open) {
    form.hoTen = props.defaultName || '';
    form.soDienThoai = '';
    error.value = '';
  }
});

const handleSubmit = async () => {
  error.value = '';
  loading.value = true;
  try {
    await updateProfile({
      hoTen: form.hoTen,
      soDienThoai: form.soDienThoai,
      email: props.defaultEmail || undefined,
      avatarUrl: props.avatarUrl || undefined,
    });
    // Lấy session mới với JWT mới sau khi update
    const res = await getCurrentUser();
    if (res.ok) {
      const user = await res.json();
      emit('success', user);
    } else {
      error.value = 'Cập nhật thành công nhưng không lấy được phiên mới. Vui lòng đăng nhập lại.';
    }
  } catch (e) {
    error.value = e.message || 'Cập nhật thất bại. Vui lòng thử lại.';
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.profile-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}
.profile-modal-box {
  background: var(--bg-card, #fff);
  border-radius: 16px;
  padding: 28px;
  width: 380px;
  max-width: 95vw;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
}
.profile-modal-header {
  margin-bottom: 16px;
}
</style>
