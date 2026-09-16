<script setup>
import { ChevronLeft, ChevronRight } from '@lucide/vue';
defineProps({
  currentPage: { type: Number, required: true },
  totalPages: { type: Number, required: true },
  totalElements: { type: Number, default: 0 },
});
const emit = defineEmits(["page-change"]);
</script>

<template>
  <div
    v-if="totalPages > 1"
    class="d-flex justify-content-center align-items-center gap-2 mt-3"
  >
    <button
      class="btn btn-sm rounded-pill d-inline-flex align-items-center justify-content-center"
      :disabled="currentPage <= 0"
      style="color: var(--text-secondary); width:32px; height:32px; padding:0;"
      @click="emit('page-change', currentPage - 1)"
    >
      <ChevronLeft :size="16" />
    </button>
    <span class="small fw-bold px-2" style="color: var(--text-primary)">
      {{ currentPage + 1 }} / {{ totalPages }}
    </span>
    <button
      class="btn btn-sm rounded-pill d-inline-flex align-items-center justify-content-center"
      :disabled="currentPage >= totalPages - 1"
      style="color: var(--text-secondary); width:32px; height:32px; padding:0;"
      @click="emit('page-change', currentPage + 1)"
    >
      <ChevronRight :size="16" />
    </button>
  </div>
</template>
