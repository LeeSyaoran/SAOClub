<template>
  <!-- Overlay nền mờ — click ra ngoài để đóng -->
  <div v-if="modelValue"
       class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
       style="background:rgba(0,0,0,0.75); z-index:1050; backdrop-filter:blur(4px);"
       @click.self="$emit('update:modelValue', false)">

    <!-- Hộp modal -->
    <div class="rounded-4 p-4 position-relative" ref="dialogEl" tabindex="-1"
         role="dialog" aria-modal="true" @keydown="trapFocus"
         :style="`background:var(--bg-card); border:1px solid var(--border-color); width:${width}; max-width:94vw; box-shadow:0 24px 80px rgba(0,0,0,0.4);`">

      <!-- Nút đóng — nổi ở góc trên phải, không có thanh tiêu đề riêng -->
      <button class="btn-close position-absolute"
              style="top:16px; right:16px; font-size:0.75rem;"
              :aria-label="t('common.close')"
              @click="$emit('update:modelValue', false)"></button>

      <!-- Nội dung (slot) -->
      <slot />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue';
import { t } from '../../i18n/index.js';

// Props: modelValue dùng v-model để đóng/mở, width tùy chỉnh độ rộng hộp
const props = defineProps({
  modelValue: { type: Boolean, default: false },
  width:      { type: String,  default: '460px' },
});
const emit = defineEmits(['update:modelValue']);

// Đóng bằng phím Escape — chỉ lắng nghe trong lúc modal đang mở
const onKeydown = (e) => { if (e.key === 'Escape') emit('update:modelValue', false); };
watch(() => props.modelValue, (open) => {
  if (open) window.addEventListener('keydown', onKeydown);
  else window.removeEventListener('keydown', onKeydown);
});

// Bẫy focus — trước đây Tab thoát được ra ngoài modal (vào sidebar/nội dung phía sau đang
// bị che), người dùng bàn phím/màn hình đọc dễ lạc mất vị trí. Focus phần tử focusable đầu
// tiên khi mở, giữ Tab/Shift+Tab quẩn trong modal, trả focus lại đúng chỗ cũ khi đóng.
const dialogEl = ref(null);
const FOCUSABLE_SELECTOR = 'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])';
let elTruocKhiMo = null;

const trapFocus = (e) => {
  if (e.key !== 'Tab') return;
  const focusables = dialogEl.value?.querySelectorAll(FOCUSABLE_SELECTOR);
  if (!focusables || focusables.length === 0) return;
  const first = focusables[0];
  const last = focusables[focusables.length - 1];
  if (e.shiftKey && document.activeElement === first) {
    e.preventDefault();
    last.focus();
  } else if (!e.shiftKey && document.activeElement === last) {
    e.preventDefault();
    first.focus();
  }
};

watch(() => props.modelValue, (open) => {
  if (open) {
    elTruocKhiMo = document.activeElement;
    nextTick(() => {
      const first = dialogEl.value?.querySelector(FOCUSABLE_SELECTOR);
      (first ?? dialogEl.value)?.focus();
    });
  } else {
    elTruocKhiMo?.focus?.();
    elTruocKhiMo = null;
  }
});
</script>
