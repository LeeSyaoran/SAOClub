<template>
  <!-- Vòng tròn tiến độ 1 giá trị (0-100), gradient thương hiệu — khác DonutChart.vue
       (nhiều segment + legend): chỉ 1 giá trị, không legend, không thư viện ngoài. -->
  <div class="d-flex flex-column align-items-center">
    <svg :width="size" :height="size" :viewBox="`0 0 ${size} ${size}`">
      <defs>
        <linearGradient :id="gradientId" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" :stop-color="color1" />
          <stop offset="100%" :stop-color="color2" />
        </linearGradient>
      </defs>
      <circle :cx="size / 2" :cy="size / 2" :r="radius" fill="none" stroke="var(--bg-hover)" :stroke-width="thickness" />
      <circle :cx="size / 2" :cy="size / 2" :r="radius" fill="none" :stroke="`url(#${gradientId})`" :stroke-width="thickness"
              stroke-linecap="round"
              :stroke-dasharray="`${dash} ${circumference - dash}`"
              style="transform:rotate(-90deg); transform-origin:center; transition:stroke-dasharray .6s ease;" />
      <text :x="size / 2" :y="size / 2 + 7" text-anchor="middle" fill="var(--text-heading)" style="font-size:20px; font-weight:800;">{{ Math.round(clamped) }}%</text>
    </svg>
    <div v-if="label" class="small text-center mt-1" style="color:var(--text-secondary); max-width:100%;">{{ label }}</div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  value:     { type: Number, required: true }, // 0-100
  label:     { type: String, default: '' },
  size:      { type: Number, default: 110 },
  thickness: { type: Number, default: 10 },
  color1:    { type: String, default: 'var(--accent-2)' },
  color2:    { type: String, default: 'var(--accent)' },
});

// id ngẫu nhiên/instance — nhiều RingProgress trên cùng trang không được trùng id
// <linearGradient>, nếu không SVG sau sẽ vẽ đè gradient của SVG trước.
const gradientId = `ring-grad-${Math.random().toString(36).slice(2, 9)}`;

const radius = computed(() => props.size / 2 - props.thickness / 2 - 2);
const circumference = computed(() => 2 * Math.PI * radius.value);
const clamped = computed(() => Math.min(100, Math.max(0, props.value)));
const dash = computed(() => (clamped.value / 100) * circumference.value);
</script>
