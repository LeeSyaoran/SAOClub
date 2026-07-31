<template>
  <div>
    <label v-if="label" class="form-label small fw-semibold" :style="labelStyle">{{ label }}</label>
    <slot :errors="fieldErrors" :meta="meta" />
    <div v-if="fieldErrors && meta?.touched" class="small text-danger mt-1">
      {{ fieldErrors }}
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  label: { type: String, default: '' },
  errors: { type: [String, Array], default: '' },
  meta: { type: Object, default: null },
});

const labelStyle = { color: 'var(--text-secondary)' };

const fieldErrors = computed(() => {
  if (!props.errors) return '';
  if (typeof props.errors === 'string') return props.errors;
  if (Array.isArray(props.errors)) return props.errors[0] || '';
  return '';
});
</script>
