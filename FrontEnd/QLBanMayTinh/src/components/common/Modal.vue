<template>
  <div
    v-if="modelValue"
    class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
    style="background:rgba(255, 255, 255, 0.35); z-index:1050; backdrop-filter:blur(3px);"
    @click.self="$emit('update:modelValue', false)"
  >
    <div
      ref="dialogEl" class="rounded-4 p-4 position-relative" tabindex="-1"
      role="dialog" aria-modal="true" :style="`background:linear-gradient(180deg, #ffffff 0%, #fef7fa 100%); border:1px solid #f3b8d0; width:${width}; max-width:94vw; box-shadow:0 24px 80px rgba(225,29,72,0.12);`"
      @keydown="trapFocus"
    >
      <button
        class="btn-close-x position-absolute d-flex align-items-center justify-content-center rounded-circle"
        style="top:16px; right:16px; width:32px; height:32px; background:var(--bg-card-alt); border:1px solid var(--border-color); color:var(--text-secondary);"
        :aria-label="t('common.close')"
        @click="$emit('update:modelValue', false)"
      >
        <X :size="16" />
      </button>

      <slot />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue';
import { t } from '../../i18n/index.js';
import { X } from '@lucide/vue';

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
