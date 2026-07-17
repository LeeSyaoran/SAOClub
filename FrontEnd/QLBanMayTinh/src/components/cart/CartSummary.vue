<template>
  <!-- Footer giỏ hàng: tổng tiền + nút đặt hàng -->
  <div class="px-4 py-3 d-flex flex-column gap-2" style="border-top:1px solid var(--border-color-soft); background:var(--bg-page-alt);">
    <div class="d-flex justify-content-between align-items-center">
      <span class="small" style="color:var(--text-secondary);">{{ t('cart.subtotal', { count: cartCount }) }}</span>
      <strong style="color:var(--text-heading);">{{ formatPrice(cartTotal) }}</strong>
    </div>
    <div style="font-size:10px; color:var(--text-secondary);">{{ t('cart.freeShipNote') }}</div>
    <button class="btn btn-warning fw-bold w-100 py-2 mt-1"
            style="border-radius:12px; font-size:0.9rem; letter-spacing:0.01em;"
            @click="$emit('checkout')">
      {{ t('cart.checkout') }} &nbsp;·&nbsp; {{ formatPrice(cartTotal) }}
    </button>
  </div>
</template>

<script setup>
import { t } from '../../i18n/index.js';
import { formatPrice } from '../../utils/formatPrice.js';

defineProps({
  cartCount: { type: Number, default: 0 },
  cartTotal: { type: Number, default: 0 },
});
defineEmits(['checkout']);
</script>
