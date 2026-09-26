<template>
  <div class="trend-chart-wrapper position-relative">
    <!-- Tooltip hiển thị giá trị khi rê chuột -->
    <div
      v-if="hoveredPoint !== null && data[hoveredPoint]"
      class="trend-tooltip position-absolute shadow-sm"
      :style="{ left: tooltipX + 'px', top: tooltipY + 'px' }"
    >
      <div class="fw-semibold text-truncate" style="font-size:11px;">{{ data[hoveredPoint].label }}</div>
      <div class="fw-bold" style="font-size:12px; color:var(--text-heading);">
        {{ formatValue(data[hoveredPoint].value) }}
      </div>
    </div>

    <svg
      :width="width"
      :height="height"
      :viewBox="`0 0 ${width} ${height}`"
      style="display:block; width:100%; height:auto; overflow:visible;"
      @mouseleave="hoveredPoint = null"
    >
      <defs>
        <linearGradient :id="gradId" x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" :stop-color="color" stop-opacity="0.32" />
          <stop offset="60%" :stop-color="color" stop-opacity="0.08" />
          <stop offset="100%" :stop-color="color" stop-opacity="0.00" />
        </linearGradient>
        <filter :id="glowId" x="-20%" y="-20%" width="140%" height="140%">
          <feDropShadow dx="0" dy="2" stdDeviation="3" :flood-color="color" flood-opacity="0.45" />
        </filter>
      </defs>

      <!-- Đường gióng ngang (Gridlines) -->
      <g class="grid-lines" opacity="0.4">
        <line
          v-for="yRatio in [0.25, 0.5, 0.75]"
          :key="yRatio"
          :x1="PADDING"
          :y1="PADDING + (height - PADDING * 2) * yRatio"
          :x2="width - PADDING"
          :y2="PADDING + (height - PADDING * 2) * yRatio"
          stroke="var(--border-color-soft, #4c1d95)"
          stroke-dasharray="3 4"
          stroke-width="1"
        />
      </g>

      <!-- Vùng diện tích Gradient -->
      <path v-if="points.length > 1" :d="areaPath" :fill="`url(#${gradId})`" stroke="none" />

      <!-- Đường biểu đồ chính -->
      <polyline
        v-if="points.length > 1"
        :points="linePoints"
        fill="none"
        :stroke="color"
        stroke-width="2.5"
        stroke-linejoin="round"
        stroke-linecap="round"
        :filter="`url(#${glowId})`"
      />

      <!-- Vùng bắt sự kiện hover dọc từng cột mốc -->
      <rect
        v-for="(p, i) in points"
        :key="'hit-' + i"
        :x="p.x - hitWidth / 2"
        :y="0"
        :width="hitWidth"
        :height="height"
        fill="transparent"
        style="cursor:pointer;"
        @mouseenter="hoveredPoint = i"
      />

      <!-- Các điểm nút tròn -->
      <g v-for="(p, i) in points" :key="'pt-' + i" style="pointer-events:none;">
        <circle
          :cx="p.x"
          :cy="p.y"
          :r="hoveredPoint === i ? 6 : 3.5"
          :fill="hoveredPoint === i ? '#ffffff' : color"
          :stroke="color"
          :stroke-width="hoveredPoint === i ? 2.5 : 1"
          style="transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);"
        />
        <circle
          v-if="hoveredPoint === i"
          :cx="p.x"
          :cy="p.y"
          r="10"
          :fill="color"
          fill-opacity="0.2"
        />
      </g>
    </svg>

    <div class="d-flex justify-content-between mt-2 px-1">
      <span
        v-for="(d, i) in data"
        :key="i"
        class="chart-label-item"
        :class="{ 'chart-label-active': hoveredPoint === i }"
      >
        {{ d.label }}
      </span>
    </div>

    <div v-if="data.length === 0" class="small text-center py-4" style="color:var(--text-muted);">
      {{ emptyText }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';

const props = defineProps({
  data:      { type: Array, required: true }, // [{ label, value }]
  width:     { type: Number, default: 450 },
  height:    { type: Number, default: 180 },
  color:     { type: String, default: '#f06b81' },
  emptyText: { type: String, default: '' },
});

const PADDING = 14;
const hoveredPoint = ref(null);

const uniqueSuffix = Math.random().toString(36).slice(2, 8);
const gradId = `trend-grad-${uniqueSuffix}`;
const glowId = `trend-glow-${uniqueSuffix}`;

const maxVal = computed(() => Math.max(...props.data.map(d => Number(d.value) || 0), 0));
const minVal = computed(() => Math.min(...props.data.map(d => Number(d.value) || 0), 0));

const hitWidth = computed(() => {
  if (props.data.length <= 1) return props.width;
  return (props.width - PADDING * 2) / (props.data.length - 1);
});

const points = computed(() => {
  if (props.data.length === 0) return [];
  const range = (maxVal.value - minVal.value) || 1;
  const step = props.data.length > 1 ? (props.width - PADDING * 2) / (props.data.length - 1) : 0;
  return props.data.map((d, i) => ({
    x: PADDING + i * step,
    y: props.height - PADDING - (((Number(d.value) || 0) - minVal.value) / range) * (props.height - PADDING * 2),
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

const tooltipX = computed(() => {
  if (hoveredPoint.value === null || !points.value[hoveredPoint.value]) return 0;
  const rawX = (points.value[hoveredPoint.value].x / props.width) * 100;
  return Math.min(88, Math.max(12, rawX));
});

const tooltipY = computed(() => {
  if (hoveredPoint.value === null || !points.value[hoveredPoint.value]) return 0;
  return Math.max(0, points.value[hoveredPoint.value].y - 38);
});

const formatValue = (val) => {
  const num = Number(val) || 0;
  return new Intl.NumberFormat('vi-VN').format(num) + ' ₫';
};
</script>

<style scoped>
.trend-chart-wrapper {
  width: 100%;
}
.trend-tooltip {
  pointer-events: none;
  background: var(--bg-card-inset, #1a1a2e);
  color: var(--text-primary, #ffffff);
  border: 1px solid var(--border-color, rgba(255,255,255,0.12));
  border-radius: 8px;
  padding: 4px 8px;
  transform: translate(-50%, -100%);
  z-index: 10;
  white-space: nowrap;
  backdrop-filter: blur(6px);
  transition: all 0.15s ease-out;
}
.chart-label-item {
  font-size: 10.5px;
  color: var(--text-muted);
  transition: color 0.15s ease;
}
.chart-label-active {
  color: var(--text-heading);
  font-weight: 600;
}
</style>
