<template>
  <!-- Lưới chấm tỉ lệ theo danh mục — dùng cho "Nhân viên theo chức vụ". Lưới cố định
       50 dot (5 hàng x 10 cột), mỗi category lấp 1 số dot liên tục tỉ lệ theo value. -->
  <div class="d-flex align-items-center gap-3 flex-wrap">
    <svg :width="gridWidth" :height="gridHeight" :viewBox="`0 0 ${gridWidth} ${gridHeight}`" style="flex-shrink:0;">
      <circle v-for="(dot, i) in dots" :key="i" :cx="dot.x" :cy="dot.y" :r="DOT_RADIUS" :fill="dot.color" />
    </svg>
    <div class="d-flex flex-column gap-2">
      <div v-for="(seg, i) in segments" :key="i" class="d-flex align-items-center gap-2">
        <span class="rounded-circle flex-shrink-0" :style="{ background: seg.color, width: '9px', height: '9px' }"></span>
        <span class="flex-grow-1" style="font-size:12px; color:var(--text-primary);">{{ seg.label }}</span>
        <span class="fw-bold" style="font-size:12px; color:var(--text-secondary);">{{ Math.round(seg.pct) }}%</span>
      </div>
      <div v-if="segments.length === 0" class="small" style="color:var(--text-muted);">{{ emptyText }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  data:      { type: Array, required: true }, // [{ label, value, color }]
  emptyText: { type: String, default: '' },
});

const TOTAL_DOTS = 50;
const COLS = 10;
const DOT_RADIUS = 5;
const GAP = 14;
const gridWidth = COLS * GAP;
const gridHeight = Math.ceil(TOTAL_DOTS / COLS) * GAP;

const total = computed(() => props.data.reduce((s, d) => s + (d.value || 0), 0));

const segments = computed(() => {
  if (total.value === 0) return [];
  return props.data.filter(d => d.value > 0).map(d => ({ ...d, pct: (d.value / total.value) * 100 }));
});

// Category cuối lấy hết dot còn lại (thay vì round riêng từng category) để tổng luôn
// đúng 50, không lệch do làm tròn.
const dots = computed(() => {
  if (total.value === 0) return [];
  const result = [];
  let dotIndex = 0;
  let allocated = 0;
  segments.value.forEach((seg, i) => {
    const isLast = i === segments.value.length - 1;
    const count = isLast ? TOTAL_DOTS - allocated : Math.round((seg.value / total.value) * TOTAL_DOTS);
    allocated += count;
    for (let n = 0; n < count && dotIndex < TOTAL_DOTS; n++, dotIndex++) {
      const col = dotIndex % COLS;
      const row = Math.floor(dotIndex / COLS);
      result.push({ x: col * GAP + GAP / 2, y: row * GAP + GAP / 2, color: seg.color });
    }
  });
  return result;
});
</script>
