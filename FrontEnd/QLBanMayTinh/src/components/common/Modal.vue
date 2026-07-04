<template>
  <!-- Overlay nền mờ — click ra ngoài để đóng -->
  <div v-if="modelValue"
       class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
       style="background:rgba(0,0,0,0.75); z-index:1050; backdrop-filter:blur(4px);"
       @click.self="$emit('update:modelValue', false)">

    <!-- Hộp modal -->
    <div class="rounded-4 p-4 position-relative"
         :style="`background:var(--bg-card); border:1px solid var(--border-color); width:${width}; max-width:94vw; box-shadow:0 24px 80px rgba(0,0,0,0.4);`">

      <!-- Nút đóng — nổi ở góc trên phải, không có thanh tiêu đề riêng -->
      <button class="btn-close btn-close-white position-absolute"
              style="top:16px; right:16px; font-size:0.75rem;"
              @click="$emit('update:modelValue', false)"></button>

      <!-- Nội dung (slot) -->
      <slot />
    </div>
  </div>
</template>

<script setup>
// Props: modelValue dùng v-model để đóng/mở, width tùy chỉnh độ rộng hộp
defineProps({
  modelValue: { type: Boolean, default: false },
  width:      { type: String,  default: '460px' },
});
defineEmits(['update:modelValue']);
</script>
