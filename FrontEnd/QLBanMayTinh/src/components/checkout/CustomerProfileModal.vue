<template>
  <div
    v-if="modelValue"
    class="profile-modal-backdrop position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
    style="background:rgba(0,0,0,0.72); z-index:1070; backdrop-filter:blur(8px);"
    @click.self="handleDismiss"
  >
    <div
      class="profile-modal-container rounded-4 d-flex flex-column shadow-lg position-relative overflow-hidden"
      role="dialog"
      aria-modal="true"
      style="background:var(--bg-card); border:1px solid var(--border-color); width:580px; max-width:94vw; max-height:92vh; box-shadow:0 24px 64px rgba(0,0,0,0.6);"
    >
      <!-- Header -->
      <div
        class="px-4 py-3.5 border-bottom d-flex align-items-center justify-content-between"
        style="border-color:var(--border-color-soft) !important; background:var(--bg-card-alt);"
      >
        <div class="d-flex align-items-center gap-3">
          <div
            class="d-flex align-items-center justify-content-center rounded-3 flex-shrink-0"
            style="width:42px; height:42px; background:linear-gradient(135deg, rgba(245,158,11,0.2), rgba(244,63,94,0.15)); color:#f59e0b;"
          >
            <UserCheck :size="22" />
          </div>
          <div>
            <div class="d-flex align-items-center gap-2">
              <h5 class="fw-bold mb-0" style="font-size:1.05rem; color:var(--text-heading);">
                Hoàn thiện thông tin tài khoản
              </h5>
              <span class="badge rounded-pill px-2.5 py-0.5 fw-semibold" style="background:rgba(239,68,68,0.12); color:#ef4444; font-size:11px;">
                Bắt buộc
              </span>
            </div>
            <div class="text-secondary small mt-0.5" style="font-size:12.5px;">
              Bổ sung Số điện thoại & Địa chỉ để tiếp tục lên đơn
            </div>
          </div>
        </div>

        <button
          type="button"
          class="btn btn-sm btn-outline-secondary p-1 rounded-circle d-flex align-items-center justify-content-center"
          style="width:30px; height:30px;"
          aria-label="Đóng"
          :disabled="loading"
          @click="handleDismiss"
        >
          <X :size="16" />
        </button>
      </div>

      <!-- Body Form -->
      <div class="p-4 overflow-y-auto custom-scrollbar flex-grow-1 d-flex flex-column gap-3.5">
        <!-- Callout Banner -->
        <div
          class="p-3 rounded-3 small d-flex align-items-start gap-2.5"
          style="background:rgba(59,130,246,0.08); border:1px solid rgba(59,130,246,0.2); color:var(--text-primary); font-size:12.5px; line-height:1.55;"
        >
          <Sparkles :size="16" class="text-primary flex-shrink-0 mt-0.5" />
          <div>
            Để đảm bảo đơn hàng được xác nhận chính xác, bảo hành thuận tiện và giao hàng tận nơi, vui lòng bổ sung thông tin dưới đây.
            <div class="text-secondary mt-1" style="font-size:12px;">
              ✓ Thông tin sẽ được tự động lưu vào tài khoản để bạn không phải nhập lại.<br />
              ✓ <strong>Tiến trình tạo đơn và giỏ hàng của bạn sẽ được giữ nguyên hoàn toàn.</strong>
            </div>
          </div>
        </div>

        <!-- Alert Error Message -->
        <div
          v-if="errorMessage"
          class="alert alert-danger py-2.5 px-3 small rounded-3 mb-0 d-flex align-items-center gap-2"
          role="alert"
        >
          <AlertCircle :size="16" class="flex-shrink-0 text-danger" />
          <span>{{ errorMessage }}</span>
        </div>

        <!-- Họ và tên -->
        <div>
          <label class="form-label small fw-semibold mb-1" style="color:var(--text-secondary); font-size:12.5px;">
            Họ và tên khách hàng <span class="text-danger">*</span>
          </label>
          <div class="input-group" style="height:42px;">
            <span class="input-group-text border-end-0 px-3" style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-secondary); width:44px;">
              <User :size="16" />
            </span>
            <input
              v-model="form.hoTen"
              type="text"
              class="form-control border-start-0"
              style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-primary); height:42px; font-size:13.5px;"
              placeholder="Ví dụ: Nguyễn Văn A"
              required
            />
          </div>
        </div>

        <!-- Số điện thoại -->
        <div>
          <div class="d-flex align-items-center justify-content-between mb-1">
            <label class="form-label small fw-semibold mb-0" style="color:var(--text-secondary); font-size:12.5px;">
              Số điện thoại liên hệ <span class="text-danger">*</span>
            </label>
            <span v-if="isPhoneValid" class="small text-success fw-semibold d-inline-flex align-items-center gap-1" style="font-size:11.5px;">
              <CheckCircle2 :size="13" /> Hợp lệ
            </span>
            <span v-else-if="phoneTouched && !isPhoneValid" class="small text-danger fw-semibold" style="font-size:11.5px;">
              10 chữ số bắt đầu bằng 0
            </span>
          </div>
          <div class="input-group" style="height:42px;">
            <span class="input-group-text border-end-0 px-3" style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-secondary); width:44px;">
              <Phone :size="16" />
            </span>
            <input
              v-model="form.soDienThoai"
              type="tel"
              class="form-control border-start-0"
              :class="{ 'is-invalid': phoneTouched && !isPhoneValid, 'is-valid': isPhoneValid }"
              style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-primary); height:42px; font-size:13.5px;"
              placeholder="0912345678"
              required
              @blur="phoneTouched = true"
            />
          </div>
          <div class="small text-muted mt-1" style="font-size:11.5px;">
            Dùng để gọi xác nhận giao hàng và tra cứu bảo hành điện tử.
          </div>
        </div>

        <!-- Địa chỉ giao hàng (sử dụng AddressPicker) -->
        <div>
          <label class="form-label small fw-semibold mb-1" style="color:var(--text-secondary); font-size:12.5px;">
            Địa chỉ nhận hàng mặc định <span class="text-danger">*</span>
          </label>
          <AddressPicker
            v-model="form.diaChi"
            placeholder="Nhập số nhà, tên đường, phường/xã, quận/huyện, tỉnh/thành..."
            @select-coords="onSelectCoords"
          />
          <div class="small text-muted mt-1" style="font-size:11.5px;">
            Địa chỉ này sẽ dùng để giao hàng và tự động tính cước vận chuyển theo cự ly kho.
          </div>
        </div>

        <!-- Email (tùy chọn hoặc hiển thị sẵn) -->
        <div>
          <label class="form-label small fw-semibold mb-1" style="color:var(--text-secondary); font-size:12.5px;">
            Email nhận thông báo <span class="text-muted fw-normal">(hóa đơn điện tử & tra cứu đơn)</span>
          </label>
          <div class="input-group" style="height:42px;">
            <span class="input-group-text border-end-0 px-3" style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-secondary); width:44px;">
              <Mail :size="16" />
            </span>
            <input
              v-model="form.email"
              type="email"
              class="form-control border-start-0"
              style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-primary); height:42px; font-size:13.5px;"
              placeholder="tenban@gmail.com"
            />
          </div>
        </div>
      </div>

      <!-- Footer Buttons -->
      <div
        class="px-4 py-3 border-top d-flex align-items-center justify-content-between gap-3"
        style="border-color:var(--border-color-soft) !important; background:var(--bg-card-alt);"
      >
        <button
          type="button"
          class="btn btn-outline-secondary px-3.5 py-2 rounded-3 small fw-semibold"
          style="font-size:13px;"
          :disabled="loading"
          @click="handleDismiss"
        >
          Để sau
        </button>

        <button
          type="button"
          class="btn btn-warning fw-bold px-4 py-2 rounded-3 d-inline-flex align-items-center gap-2 shadow-sm"
          style="font-size:13.5px;"
          :disabled="loading || !isFormValid"
          @click="saveProfile"
        >
          <span v-if="loading" class="spinner-border spinner-border-sm" role="status"></span>
          <Check v-else :size="16" />
          <span>{{ loading ? 'Đang lưu...' : 'Lưu & Tiếp tục lên đơn' }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue';
import { UserCheck, User, Phone, Mail, MapPin, X, Check, CheckCircle2, AlertCircle, Sparkles } from '@lucide/vue';
import AddressPicker from './AddressPicker.vue';
import * as KhachHangService from '../../services/KhachHangService.js';
import { AuthStore, setSession } from '../../stores/index.js';
import { isValidPhoneNumber } from '../../utils/validators.js';

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  customer:   { type: Object, default: () => ({}) },
});

const emit = defineEmits(['update:modelValue', 'saved', 'dismiss']);

const loading = ref(false);
const errorMessage = ref('');
const phoneTouched = ref(false);
const currentFullProfile = ref(null);

const form = reactive({
  hoTen: '',
  soDienThoai: '',
  diaChi: '',
  email: '',
});

const isPhoneValid = computed(() => isValidPhoneNumber(form.soDienThoai));
const isAddressValid = computed(() => typeof form.diaChi === 'string' && form.diaChi.trim().length >= 5);
const isFormValid = computed(() => {
  return form.hoTen.trim().length >= 2 && isPhoneValid.value && isAddressValid.value;
});

const onSelectCoords = (coords) => {
  if (coords?.address) {
    form.diaChi = coords.address;
  }
};

const initForm = async () => {
  errorMessage.value = '';
  phoneTouched.value = false;
  const user = AuthStore.user || props.customer || {};

  form.hoTen = user.hoTen || user.username || '';
  form.email = user.email || '';

  // Prefill phone if valid, otherwise empty
  if (isValidPhoneNumber(user.soDienThoai)) {
    form.soDienThoai = user.soDienThoai;
  } else {
    form.soDienThoai = '';
  }

  // Prefill address if valid
  if (user.diaChi && user.diaChi.trim().length >= 3) {
    form.diaChi = user.diaChi.trim();
  } else {
    form.diaChi = '';
  }

  // Fetch full details from database if available
  const customerId = user.id || user.khachHangId;
  if (customerId) {
    try {
      const full = await KhachHangService.getById(customerId);
      if (full) {
        currentFullProfile.value = full;
        if (!form.hoTen && full.hoTen) form.hoTen = full.hoTen;
        if (!form.email && full.email) form.email = full.email;
        if (!form.soDienThoai && isValidPhoneNumber(full.soDienThoai)) form.soDienThoai = full.soDienThoai;
        if (!form.diaChi && full.diaChi) form.diaChi = full.diaChi;
      }
    } catch {}
  }
};

watch(() => props.modelValue, (isOpen) => {
  if (isOpen) {
    initForm();
  }
});

const handleDismiss = () => {
  emit('update:modelValue', false);
  emit('dismiss');
};

const saveProfile = async () => {
  errorMessage.value = '';
  phoneTouched.value = true;

  if (!form.hoTen.trim()) {
    errorMessage.value = 'Vui lòng nhập họ và tên khách hàng.';
    return;
  }
  if (!isPhoneValid.value) {
    errorMessage.value = 'Số điện thoại không hợp lệ (cần 10 chữ số bắt đầu bằng 0).';
    return;
  }
  if (!isAddressValid.value) {
    errorMessage.value = 'Vui lòng nhập địa chỉ nhận hàng chi tiết (ít nhất 5 ký tự).';
    return;
  }

  const user = AuthStore.user || props.customer;
  const customerId = user?.id || user?.khachHangId;
  if (!customerId) {
    errorMessage.value = 'Không tìm thấy thông tin tài khoản người dùng.';
    return;
  }

  loading.value = true;
  try {
    // Merge with full profile from DB to preserve loaiKhach, diemTichLuy, trangThai, etc.
    const base = currentFullProfile.value || {};
    const body = {
      ...base,
      hoTen: form.hoTen.trim(),
      soDienThoai: form.soDienThoai.trim(),
      diaChi: form.diaChi.trim(),
      email: form.email?.trim() || base.email || null,
      loaiKhach: base.loaiKhach || 'ca_nhan',
      diemTichLuy: base.diemTichLuy ?? 0,
      trangThai: base.trangThai || 'active',
    };

    const res = await KhachHangService.save(customerId, body);
    if (!res.ok) {
      const errText = await res.text().catch(() => '');
      try {
        const errObj = JSON.parse(errText);
        throw new Error(errObj.message || errObj.error || errText);
      } catch (parseErr) {
        throw new Error(errText || 'Không thể lưu thông tin vào tài khoản');
      }
    }

    // Update AuthStore session with updated profile
    const updatedUser = {
      ...AuthStore.user,
      hoTen: body.hoTen,
      soDienThoai: body.soDienThoai,
      diaChi: body.diaChi,
      email: body.email,
    };
    setSession(updatedUser);

    // Emit event with updated info
    emit('saved', {
      hoTen: body.hoTen,
      soDienThoai: body.soDienThoai,
      diaChi: body.diaChi,
      email: body.email,
    });

    emit('update:modelValue', false);
  } catch (err) {
    errorMessage.value = err.message || 'Lỗi khi cập nhật thông tin tài khoản.';
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.profile-modal-backdrop {
  animation: fadeIn 0.2s ease-out;
}
.profile-modal-container {
  animation: scaleUp 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
@keyframes scaleUp {
  from {
    opacity: 0;
    transform: scale(0.94) translateY(8px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}
</style>
