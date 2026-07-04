<template>
  <!-- Khối nhấp nháy chờ dữ liệu — thay thế cho text "Đang tải..." -->
  <div class="skeleton-pulse rounded-2"
       :style="`width:${width}; height:${height}; border-radius:${radius};`"></div>
</template>

<script setup>
// width/height: kích thước khớp với nội dung đang chờ (vd: '100%', '120px')
defineProps({
  width:  { type: String, default: '100%' },
  height: { type: String, default: '16px' },
  radius: { type: String, default: '6px' },
});
</script>

<style scoped>
.skeleton-pulse {
  background: linear-gradient(90deg, var(--bg-card-alt) 25%, var(--border-color-strong) 50%, var(--bg-card-alt) 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.4s infinite;
}
@keyframes skeleton-shimmer {
  0%   { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
