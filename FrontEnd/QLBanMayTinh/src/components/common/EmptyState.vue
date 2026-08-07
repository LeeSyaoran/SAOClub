<script setup>
import { Inbox } from '@lucide/vue';

defineProps({
  message: { type: String, default: "" },
  icon: { type: Object, default: () => Inbox },
});

const emit = defineEmits(["action"]);
</script>

<template>
  <div
    class="d-flex flex-column align-items-center justify-content-center py-5 gap-3"
    style="min-height: 150px; color: var(--text-secondary)"
  >
    <div><component :is="icon" :size="40" /></div>
    <p class="small mb-0">
      {{ message || ($t?.("common.noData") || "Không có dữ liệu") }}
    </p>
    <button
      v-if="emit"
      class="btn btn-outline-warning rounded-pill px-4 py-1 btn-sm"
      @click="emit('action')"
    >
      {{ $t?.("common.retry") || "Thử lại" }}
    </button>
  </div>
</template>
