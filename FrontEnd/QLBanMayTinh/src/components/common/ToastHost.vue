<template>
  <!-- Toast thông báo lỗi/thành công (thay window.alert()) — dùng chung Admin/Staff/WarehouseManagement -->
  <Transition name="adm-toast-slide">
    <div v-if="ToastState.show"
         class="position-fixed d-flex align-items-start gap-2 px-4 py-3 rounded-3 fw-semibold small shadow-lg"
         style="top:24px; right:24px; z-index:9999; min-width:260px; max-width:440px; pointer-events:none; line-height:1.4;"
         :style="ToastState.type === 'success'
           ? 'background:var(--state-success,#16a34a); color:#fff;'
           : 'background:var(--state-danger,#dc2626); color:#fff;'"
         role="status" aria-live="polite">
      <span style="font-size:1.1rem; flex-shrink:0;">{{ ToastState.type === 'success' ? '✓' : '✕' }}</span>
      <span>{{ ToastState.msg }}</span>
    </div>
  </Transition>
</template>

<script setup>
import { ToastState } from '../../stores/toast.js';
</script>

<style scoped>
.adm-toast-slide-enter-active, .adm-toast-slide-leave-active { transition: transform 0.3s ease, opacity 0.25s ease; }
.adm-toast-slide-enter-from, .adm-toast-slide-leave-to       { transform: translateX(110%); opacity: 0; }
</style>
