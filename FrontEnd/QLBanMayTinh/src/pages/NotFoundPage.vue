<script setup>
defineEmits(['addToCart', 'buyAgainUnavailable', 'goHome']);
import { usePageMeta } from "@/composables/usePageMeta.js";
usePageMeta("404 - Không tìm thấy trang", "Trang bạn tìm kiếm không tồn tại.");
</script>

<template>
  <div class="d-flex align-items-center justify-content-center"
       style="min-height:100vh; background:var(--bg-page); color:var(--text-primary); font-family:'Nunito Sans',sans-serif;">
    <div class="text-center d-flex flex-column align-items-center gap-3">
      <div style="font-size:4rem">🔍</div>
      <h2 class="fw-black mb-0" style="font-size:1.5rem">404</h2>
      <p class="mb-0" style="color:var(--text-secondary)">Trang bạn tìm không tồn tại</p>
      <router-link to="/" class="btn btn-warning fw-bold rounded-pill px-4 py-2">← Về trang chủ</router-link>
    </div>
  </div>
</template>
