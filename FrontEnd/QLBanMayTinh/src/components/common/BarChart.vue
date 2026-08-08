<template>
  <!-- Biểu đồ cột — ngang (mặc định) hoặc đứng (vertical) — không cần thư viện ngoài -->
  <div v-if="!vertical" class="d-flex flex-column gap-3">
    <div v-for="(row, i) in rows" :key="i" class="d-flex align-items-center gap-2">
      <div v-if="row.image !== undefined" class="rounded-2 d-flex align-items-center justify-content-center overflow-hidden flex-shrink-0"
           style="width:30px; height:26px; background:var(--bg-card-inset);">
        <img v-if="row.image" :src="row.image" :alt="row.label" style="width:100%; height:100%; object-fit:contain; padding:2px;" />
        <span v-else><Laptop :size="14" color="var(--text-muted)" /></span>
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

  <div v-else class="d-flex align-items-end gap-2" style="height:150px;">
    <div v-for="(row, i) in rows" :key="i" class="d-flex flex-column align-items-center flex-grow-1 h-100">
      <div class="flex-grow-1 d-flex align-items-end w-100">
        <div class="w-100 rounded-top-2"
             :style="{ height: Math.max(row.pct, row.value > 0 ? 4 : 0) + '%', background: row.color || 'var(--accent)', transition: 'height .6s ease' }"></div>
      </div>
      <div class="text-truncate mt-1" style="font-size:10px; color:var(--text-muted); max-width:100%;">{{ row.label }}</div>
      <div class="fw-bold text-truncate" style="font-size:10px; color:var(--text-primary); max-width:100%;">{{ row.displayValue ?? row.value }}</div>
    </div>
    <div v-if="rows.length === 0" class="small" style="color:var(--text-muted);">{{ emptyText }}</div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { Laptop } from '@lucide/vue';

const props = defineProps({
  data:      { type: Array, required: true }, // [{ label, value, color?, displayValue? }]
  emptyText: { type: String, default: '' },
  vertical:  { type: Boolean, default: false },
});

const max = computed(() => Math.max(...props.data.map(d => d.value || 0), 1));
const rows = computed(() =>
  props.data.map(d => ({ ...d, pct: d.value > 0 ? Math.max(4, (d.value / max.value) * 100) : 0 }))
);
</script>
