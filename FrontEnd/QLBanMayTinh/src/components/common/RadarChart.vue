<template>
  <!-- Radar nhiều trục, 1 series — không so sánh 2 kỳ (xem Global Constraints trong
       plan). Toạ độ mỗi trục chia đều 360°, bắt đầu từ đỉnh (12h). -->
  <svg v-if="data.length" :width="size" :height="size" :viewBox="`0 0 ${size} ${size}`">
    <polygon v-for="ring in GRID_RINGS" :key="ring" :points="ringPoints(ring)"
             fill="none" stroke="var(--border-color-soft)" stroke-width="1" />
    <line v-for="(p, i) in axisPoints" :key="'axis' + i" :x1="center" :y1="center" :x2="p.x" :y2="p.y"
          stroke="var(--border-color-soft)" stroke-width="1" />
    <polygon :points="valuePolygon" fill="var(--accent-2)" fill-opacity="0.25" :stroke="color" stroke-width="2"
              style="transition: points .6s ease;" />
    <circle v-for="(p, i) in valuePoints" :key="'dot' + i" :cx="p.x" :cy="p.y" r="3" :fill="color" />
    <text v-for="(p, i) in labelPoints" :key="'label' + i" :x="p.x" :y="p.y"
          :text-anchor="p.anchor" dominant-baseline="middle"
          fill="var(--text-secondary)" style="font-size:10px;">{{ data[i].axis }}</text>
  </svg>
  <div v-else class="small text-center py-3" style="color:var(--text-muted);">{{ emptyText }}</div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  data:      { type: Array, required: true }, // [{ axis, value }] value 0-100
  size:      { type: Number, default: 220 },
  color:     { type: String, default: 'var(--accent)' },
  emptyText: { type: String, default: '' },
});

const GRID_RINGS = [0.25, 0.5, 0.75, 1];

const center = computed(() => props.size / 2);
const maxRadius = computed(() => props.size / 2 - 28);
const n = computed(() => props.data.length);

const angleFor = (i) => (2 * Math.PI * i) / n.value - Math.PI / 2;

const pointAt = (i, radiusFraction) => {
  const a = angleFor(i);
  return {
    x: center.value + Math.cos(a) * maxRadius.value * radiusFraction,
    y: center.value + Math.sin(a) * maxRadius.value * radiusFraction,
  };
};

const ringPoints = (fraction) =>
  Array.from({ length: n.value }, (_, i) => {
    const p = pointAt(i, fraction);
    return `${p.x},${p.y}`;
  }).join(' ');

const axisPoints = computed(() => Array.from({ length: n.value }, (_, i) => pointAt(i, 1)));

const valuePoints = computed(() =>
  props.data.map((d, i) => pointAt(i, Math.min(100, Math.max(0, d.value)) / 100))
);
const valuePolygon = computed(() => valuePoints.value.map(p => `${p.x},${p.y}`).join(' '));

const labelPoints = computed(() =>
  Array.from({ length: n.value }, (_, i) => {
    const p = pointAt(i, 1.18);
    const cos = Math.cos(angleFor(i));
    const anchor = cos > 0.3 ? 'start' : cos < -0.3 ? 'end' : 'middle';
    return { ...p, anchor };
  })
);
</script>
