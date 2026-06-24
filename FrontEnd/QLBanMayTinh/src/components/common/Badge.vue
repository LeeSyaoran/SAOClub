<template>
  <!-- Badge trạng thái: success / danger / warning / secondary -->
  <span class="badge fw-semibold" :class="variantClass" style="font-size:0.72rem;">
    {{ text }}
  </span>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  text:    { type: String, default: '' },
  // variant: 'success' | 'danger' | 'warning' | 'info' | 'secondary'
  variant: { type: String, default: 'secondary' },
});

// Map variant sang Bootstrap badge class
const variantClass = computed(() => ({
  success:   'bg-success bg-opacity-25 text-success',
  danger:    'bg-danger  bg-opacity-25 text-danger',
  warning:   'bg-warning bg-opacity-25 text-warning',
  info:      'bg-info    bg-opacity-25 text-info',
  secondary: 'bg-secondary text-secondary',
}[props.variant] ?? 'bg-secondary text-secondary'));
</script>
