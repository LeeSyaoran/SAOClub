<template>
  <!-- Bảng tóm tắt tổng tiền giỏ hàng -->
  <div class="card border-secondary" style="background:rgba(255,255,255,0.04);">
    <div class="card-body d-flex flex-column gap-3">

      <div class="fw-bold text-uppercase" style="font-size:0.82rem; letter-spacing:0.05em;">
        Tóm tắt đơn hàng
      </div>

      <!-- Ô nhập mã giảm giá -->
      <div class="d-flex gap-2">
        <input v-model="promoCode"
               class="form-control form-control-sm"
               style="background:#1a1a1a; border-color:rgba(255,255,255,0.12); color:#eee;"
               placeholder="Mã giảm giá (Voucher)"
               @keyup.enter="$emit('apply-promo', promoCode)" />
        <button class="btn btn-sm btn-outline-warning flex-shrink-0"
                @click="$emit('apply-promo', promoCode)">
          Áp dụng
        </button>
      </div>

      <!-- Thông báo kết quả voucher -->
      <div v-if="promoMessage" class="small px-2 py-1 rounded-2"
           :style="promoOk ? 'background:rgba(72,199,142,0.1);color:#48c78e;'
                           : 'background:rgba(220,53,69,0.1);color:#e05252;'">
        {{ promoMessage }}
      </div>

      <!-- Chi tiết tính tiền -->
      <div class="d-flex flex-column gap-2 small">
        <div class="d-flex justify-content-between text-secondary">
          <span>Tạm tính:</span>
          <span>{{ formatPrice(subTotal) }}</span>
        </div>
        <div v-if="discount > 0" class="d-flex justify-content-between text-success">
          <span>Giảm giá:</span>
          <span>− {{ formatPrice(discount) }}</span>
        </div>
        <div class="d-flex justify-content-between text-secondary">
          <span>Phí vận chuyển:</span>
          <span>{{ shippingFee === 0 ? 'Miễn phí' : formatPrice(shippingFee) }}</span>
        </div>
        <div class="d-flex justify-content-between fw-bold border-top border-secondary pt-2">
          <span>Tổng thanh toán:</span>
          <span class="text-warning">{{ formatPrice(grandTotal) }}</span>
        </div>
      </div>

      <!-- Nút tiến hành đặt hàng -->
      <button class="btn btn-warning text-dark fw-black w-100"
              :disabled="!canCheckout"
              @click="$emit('checkout')">
        TIẾN HÀNH ĐẶT HÀNG
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';

const props = defineProps({
  subTotal:     { type: Number, default: 0 },
  discount:     { type: Number, default: 0 },
  shippingFee:  { type: Number, default: 30000 },
  promoMessage: { type: String, default: '' },
  promoOk:      { type: Boolean, default: false },
  canCheckout:  { type: Boolean, default: true },
});

defineEmits(['checkout', 'apply-promo']);

// Tổng = tạm tính - giảm + ship
const grandTotal = computed(() => props.subTotal - props.discount + props.shippingFee);

const promoCode = ref('');

const formatPrice = (v) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v ?? 0);
</script>
