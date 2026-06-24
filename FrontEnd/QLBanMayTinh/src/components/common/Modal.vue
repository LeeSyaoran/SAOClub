<template>
  <!-- Overlay nền mờ — click ra ngoài để đóng -->
  <Teleport to="body">
    <div v-if="modelValue"
         class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
         style="background:rgba(0,0,0,0.6); z-index:1050; backdrop-filter:blur(2px);"
         @click.self="$emit('update:modelValue', false)">

      <!-- Hộp modal -->
      <div class="rounded-4 d-flex flex-column"
           :style="`background:#181818; border:1px solid rgba(255,255,255,0.1); width:${width}; max-width:95vw; max-height:90vh;`">

        <!-- Tiêu đề + nút X -->
        <div class="d-flex align-items-center justify-content-between p-3 border-bottom border-secondary">
          <span class="fw-bold">{{ title }}</span>
          <button class="btn-close btn-close-white btn-sm"
                  @click="$emit('update:modelValue', false)"></button>
        </div>

        <!-- Nội dung (slot) -->
        <div class="overflow-y-auto p-4">
          <slot />
        </div>

        <!-- Footer (slot tùy chọn) -->
        <div v-if="$slots.footer" class="p-3 border-top border-secondary">
          <slot name="footer" />
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
// Props: modelValue dùng v-model để đóng/mở, title hiện ở header, width tùy chỉnh độ rộng
defineProps({
  modelValue: { type: Boolean, default: false },
  title:      { type: String,  default: '' },
  width:      { type: String,  default: '520px' },
});
defineEmits(['update:modelValue']);
</script>
