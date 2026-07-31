<script setup>
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
      class="btn btn-sm fw-bold rounded-pill"
      :disabled="currentPage <= 0"
      style="color: var(--text-secondary)"
      @click="emit('page-change', currentPage - 1)"
    >
      ←
    </button>
    <span class="small fw-bold px-2" style="color: var(--text-primary)">
      {{ currentPage + 1 }} / {{ totalPages }}
    </span>
    <button
      class="btn btn-sm fw-bold rounded-pill"
      :disabled="currentPage >= totalPages - 1"
      style="color: var(--text-secondary)"
      @click="emit('page-change', currentPage + 1)"
    >
      →
    </button>
  </div>
</template>
