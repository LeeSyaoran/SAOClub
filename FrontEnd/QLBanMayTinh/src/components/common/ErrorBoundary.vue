<script setup>
defineProps({
  message: { type: String, default: "" },
});

const emit = defineEmits(["retry"]);
</script>

<template>
  <div
    class="d-flex flex-column align-items-center justify-content-center py-5 gap-3"
    style="min-height: 200px; color: var(--text-secondary)"
  >
    <div style="font-size: 2.5rem">⚠️</div>
    <p class="fw-bold mb-0" style="color: var(--text-primary)">
      {{ message || $t?.("common.error") || "Đã xảy ra lỗi" }}
    </p>
    <button
      v-if="emit"
      class="btn btn-warning fw-bold rounded-pill px-4 py-1 btn-sm"
      @click="emit('retry')"
    >
      {{ $t?.("common.retry") || "Thử lại" }}
    </button>
  </div>
</template>
