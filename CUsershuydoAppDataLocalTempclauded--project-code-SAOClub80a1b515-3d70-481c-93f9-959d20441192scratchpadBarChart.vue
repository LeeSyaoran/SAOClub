<template>
  <!-- Biểu đồ cột ngang đơn giản — không cần thư viện ngoài -->
  <div class="d-flex flex-column gap-3">
    <div v-for="(row, i) in rows" :key="i" class="d-flex align-items-center gap-2">
      <div v-if="row.image !== undefined" class="rounded-2 d-flex align-items-center justify-content-center overflow-hidden flex-shrink-0"
           style="width:30px; height:26px; background:var(--bg-card-inset);">
        <img v-if="row.image" :src="row.image" :alt="row.label" style="width:100%; height:100%; object-fit:contain; padding:2px;" />
        <span v-else style="font-size:0.85rem;">💻</span>
      </div>
      <div class="text-truncate flex-shrink-0" style="width:110px; font-size:11.5px; color:var(--text-secondary);" :title="row.label">
        {{ row.label }}
      </div>
      <div class="flex-grow-1 rounded-pill overflow-hidden" style="background:var(--bg-hover); height:14px;">
        <div class="h-100 rounded-pill" :style="{ width: row.pct + '%', background: row.color || 'var(--accent)', transition: 'width .6s ease' }"></div>
      </div>
      <div class="fw-bold text-end flex-shrink-0" style="width:64px; font-size:11.5px; color:var(--text-primary);">
        {{ row.displayValue ?? row.value }}
      </div>
    </div>
    <div v-if="rows.length === 0" class="small" style="color:var(--text-muted);">{{ emptyText }}</div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  data:      { type: Array, required: true }, // [{ label, value, color?, displayValue? }]
  emptyText: { type: String, default: '' },
});

const max = computed(() => Math.max(...props.data.map(d => d.value || 0), 1));
const rows = computed(() =>
  props.data.map(d => ({ ...d, pct: d.value > 0 ? Math.max(4, (d.value / max.value) * 100) : 0 }))
);
</script>
