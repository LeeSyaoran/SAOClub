<template>
  <!-- Overlay full màn hình -->
  <Teleport to="body">
    <div v-if="modelValue"
         class="position-fixed top-0 start-0 w-100 h-100"
         style="background:rgba(0,0,0,0.7); z-index:1060;"
         @click.self="$emit('update:modelValue', false)">

      <!-- Panel trượt từ phải -->
      <div class="position-fixed top-0 end-0 h-100 d-flex flex-column overflow-hidden"
           style="width:100%; max-width:460px; background:#121212; border-left:1px solid rgba(255,255,255,0.08);">

        <!-- Header -->
        <div class="d-flex align-items-center justify-content-between p-4 border-bottom border-secondary">
          <button class="btn btn-sm btn-outline-secondary" @click="step > 1 ? step-- : $emit('update:modelValue', false)">
            ‹ Quay lại
          </button>
          <span class="fw-black text-uppercase" style="font-size:0.88rem; letter-spacing:0.05em;">
            {{ step === 1 ? 'Thông tin đặt hàng' : 'Phương thức thanh toán' }}
          </span>
          <button class="btn-close btn-close-white btn-sm"
                  @click="$emit('update:modelValue', false)"></button>
        </div>

        <!-- Thanh tiến trình 2 bước -->
        <div class="d-flex gap-2 p-3 border-bottom border-secondary">
          <div class="flex-grow-1 text-center py-2 rounded-3 small fw-black text-uppercase"
               :style="step === 1 ? 'background:#f4c200; color:#111;' : 'background:rgba(255,255,255,0.06); color:#666;'">
            1. Thông tin
          </div>
          <div class="flex-grow-1 text-center py-2 rounded-3 small fw-black text-uppercase"
               :style="step === 2 ? 'background:#f4c200; color:#111;' : 'background:rgba(255,255,255,0.06); color:#666;'">
            2. Thanh toán
          </div>
        </div>

        <!-- Nội dung cuộn được -->
        <div class="flex-grow-1 overflow-y-auto p-4">

          <!-- Bước 1: Thông tin người nhận -->
          <div v-if="step === 1" class="d-flex flex-column gap-3">
            <div>
              <label class="form-label small text-secondary fw-semibold">Họ và tên *</label>
              <input v-model="form.name"
                     class="form-control form-control-sm"
                     style="background:#1a1a1a; border-color:rgba(255,255,255,0.12); color:#eee;"
                     placeholder="Nguyễn Văn A" />
            </div>
            <div>
              <label class="form-label small text-secondary fw-semibold">Số điện thoại *</label>
              <input v-model="form.phone"
                     class="form-control form-control-sm"
                     style="background:#1a1a1a; border-color:rgba(255,255,255,0.12); color:#eee;"
                     placeholder="0909xxxxxx" />
            </div>
            <div>
              <label class="form-label small text-secondary fw-semibold">Địa chỉ giao hàng</label>
              <textarea v-model="form.address" rows="2"
                        class="form-control form-control-sm"
                        style="background:#1a1a1a; border-color:rgba(255,255,255,0.12); color:#eee;"
                        placeholder="Số nhà, đường, phường, quận, thành phố..."></textarea>
            </div>
            <div>
              <label class="form-label small text-secondary fw-semibold">Ghi chú</label>
              <input v-model="form.note"
                     class="form-control form-control-sm"
                     style="background:#1a1a1a; border-color:rgba(255,255,255,0.12); color:#eee;"
                     placeholder="Giao giờ hành chính..." />
            </div>
            <div v-if="formError" class="alert alert-danger small py-2 mb-0">{{ formError }}</div>
            <button class="btn btn-warning text-dark fw-black" @click="nextStep">
              TIẾP TỤC →
            </button>
          </div>

          <!-- Bước 2: Phương thức thanh toán -->
          <div v-else class="d-flex flex-column gap-3">
            <!-- COD -->
            <div class="p-3 rounded-3 d-flex align-items-center gap-3"
                 style="cursor:pointer;"
                 :style="payMethod === 'cod'
                   ? 'border:1.5px solid #f4c200; background:rgba(244,194,0,0.08);'
                   : 'border:1px solid rgba(255,255,255,0.1); background:rgba(255,255,255,0.03);'"
                 @click="payMethod = 'cod'">
              <span style="font-size:1.3rem;">🚚</span>
              <div>
                <div class="fw-bold" :class="payMethod === 'cod' ? 'text-warning' : 'text-light'">
                  Thanh toán khi nhận hàng (COD)
                </div>
                <div class="text-secondary small">Miễn phí vận chuyển đơn từ 300.000đ</div>
              </div>
            </div>
            <!-- Chuyển khoản -->
            <div class="p-3 rounded-3 d-flex align-items-center gap-3"
                 style="cursor:pointer;"
                 :style="payMethod === 'bank'
                   ? 'border:1.5px solid #f4c200; background:rgba(244,194,0,0.08);'
                   : 'border:1px solid rgba(255,255,255,0.1); background:rgba(255,255,255,0.03);'"
                 @click="payMethod = 'bank'">
              <span style="font-size:1.3rem;">💳</span>
              <div>
                <div class="fw-bold" :class="payMethod === 'bank' ? 'text-warning' : 'text-secondary'">
                  Chuyển khoản ngân hàng
                </div>
                <div class="text-secondary small">Xác nhận qua ảnh bill chuyển khoản</div>
              </div>
            </div>

            <!-- Tổng tiền -->
            <div class="border-top border-secondary pt-3 d-flex justify-content-between align-items-center">
              <span class="text-secondary small">Tổng thanh toán</span>
              <span class="fw-black text-warning" style="font-size:1.2rem;">{{ formatPrice(total) }}</span>
            </div>

            <div v-if="submitError" class="alert alert-danger small py-2 mb-0">{{ submitError }}</div>

            <button class="btn btn-warning text-dark fw-black"
                    :disabled="loading"
                    @click="handleSubmit">
              {{ loading ? 'Đang đặt hàng...' : 'XÁC NHẬN THANH TOÁN' }}
            </button>
          </div>

        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, reactive } from 'vue';

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  total:      { type: Number,  default: 0 },
});

// Emit: update:modelValue (đóng modal), confirm (gửi form lên App.vue xử lý)
const emit = defineEmits(['update:modelValue', 'confirm']);

const step       = ref(1);
const payMethod  = ref('cod');
const loading    = ref(false);
const formError  = ref('');
const submitError = ref('');

const form = reactive({ name: '', phone: '', address: '', note: '' });

const formatPrice = (v) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v ?? 0);

// Kiểm tra bước 1 rồi chuyển sang bước 2
const nextStep = () => {
  formError.value = '';
  if (!form.name.trim())  { formError.value = 'Vui lòng nhập họ tên.';         return; }
  if (!form.phone.trim()) { formError.value = 'Vui lòng nhập số điện thoại.';  return; }
  step.value = 2;
};

// Gửi thông tin đặt hàng lên component cha
const handleSubmit = async () => {
  submitError.value = '';
  loading.value = true;
  try {
    await emit('confirm', {
      name:      form.name,
      phone:     form.phone,
      address:   form.address,
      note:      form.note,
      payMethod: payMethod.value,
    });
  } catch (e) {
    submitError.value = e.message ?? 'Đặt hàng thất bại, thử lại.';
  } finally {
    loading.value = false;
  }
};
</script>
