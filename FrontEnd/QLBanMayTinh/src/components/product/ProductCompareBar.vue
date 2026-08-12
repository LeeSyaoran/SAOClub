<template>
  <div
    v-if="items.length > 0"
    class="position-fixed bottom-0 start-0 w-100 d-flex align-items-center gap-3 px-3 py-2 flex-wrap"
    style="background:var(--bg-card); border-top:1px solid var(--border-color); z-index:1030; box-shadow:0 -4px 16px rgba(0,0,0,0.3);"
  >
    <div class="d-flex gap-2 flex-grow-1" style="overflow-x:auto;">
      <div
        v-for="item in items" :key="item.bienTheId"
        class="d-flex align-items-center gap-1 flex-shrink-0 rounded-2 px-2 py-1"
        style="background:var(--bg-card-inset); font-size:11px; color:var(--text-primary);"
      >
        <span class="text-truncate" style="max-width:140px;">{{ item.tenSanPham }}</span>
        <button
          class="btn-close btn-close-white" style="font-size:8px;"
          :aria-label="t('common.remove')"
          @click="$emit('remove', item)"
        ></button>
      </div>
    </div>

    <span class="small flex-shrink-0" style="color:var(--text-secondary); font-size:11px;">
      {{ t('productCompare.barLabel', { count: items.length, max }) }}
    </span>

    <button
      class="btn btn-sm btn-outline-secondary flex-shrink-0" style="font-size:11px;"
      @click="$emit('clear')"
    >
      {{ t('productCompare.barClear') }}
    </button>
    <button
      class="btn btn-sm btn-warning text-dark fw-bold flex-shrink-0" style="font-size:11px;"
      :disabled="items.length < 2"
      @click="$emit('open')"
    >
      {{ t('productCompare.barOpen') }}
    </button>
  </div>
</template>

<script setup>
import { t } from '../../i18n/index.js';

defineProps({
  items: { type: Array,  default: () => [] },
  max:   { type: Number, default: 4 },
});

defineEmits(['open', 'clear', 'remove']);
</script>
