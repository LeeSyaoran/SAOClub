<template>
  <!-- Biểu đồ đường + vùng tô (trend theo thời gian) — vẽ bằng SVG thuần -->
  <div>
    <svg :width="width" :height="height" :viewBox="`0 0 ${width} ${height}`" style="display:block; width:100%; height:auto;">
      <path v-if="points.length > 1" :d="areaPath" :fill="color" fill-opacity="0.15" stroke="none" />
      <polyline v-if="points.length > 1" :points="linePoints" fill="none" :stroke="color" stroke-width="2.5"
                stroke-linejoin="round" stroke-linecap="round" />
      <circle v-for="(p, i) in points" :key="i" :cx="p.x" :cy="p.y" r="3.5" :fill="color" />
    </svg>
    <div class="d-flex justify-content-between mt-1">
      <span v-for="(d, i) in data" :key="i" style="font-size:10px; color:var(--text-muted);">{{ d.label }}</span>
    </div>
    <div v-if="data.length === 0" class="small text-center py-3" style="color:var(--text-muted);">{{ emptyText }}</div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  data:      { type: Array, required: true }, // [{ label, value }]
  width:     { type: Number, default: 400 },
  height:    { type: Number, default: 120 },
  color:     { type: String, default: '#facc15' },
  emptyText: { type: String, default: '' },
});

const PADDING = 10;

const maxVal = computed(() => Math.max(...props.data.map(d => d.value), 0));
const minVal = computed(() => Math.min(...props.data.map(d => d.value), 0));

const points = computed(() => {
  if (props.data.length === 0) return [];
  const range = (maxVal.value - minVal.value) || 1;
  const step = props.data.length > 1 ? (props.width - PADDING * 2) / (props.data.length - 1) : 0;
  return props.data.map((d, i) => ({
    x: PADDING + i * step,
    y: props.height - PADDING - ((d.value - minVal.value) / range) * (props.height - PADDING * 2),
  }));
});

const linePoints = computed(() => points.value.map(p => `${p.x},${p.y}`).join(' '));

const areaPath = computed(() => {
  if (points.value.length < 2) return '';
  const first = points.value[0];
  const last = points.value[points.value.length - 1];
  const mid = points.value.map(p => `L ${p.x},${p.y}`).join(' ');
  return `M ${first.x},${props.height - PADDING} ${mid} L ${last.x},${props.height - PADDING} Z`;
});
</script>
