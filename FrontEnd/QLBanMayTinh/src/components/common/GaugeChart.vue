<template>
  <!-- Đồng hồ đo nửa hình tròn có kim chỉ — kiểu CSAT/CES/NPS -->
  <div class="d-flex flex-column align-items-center">
    <div class="small fw-semibold text-center mb-1 d-inline-flex align-items-center gap-1" style="color:var(--text-secondary); max-width:100%;"><component v-if="icon" :is="icon" :size="12" /> {{ label }}</div>
    <svg :width="size + pad * 2" :height="size / 2 + 22" :viewBox="`0 0 ${size + pad * 2} ${size / 2 + 22}`">
      <path :d="bgPath" fill="none" stroke="var(--bg-hover)" :stroke-width="thickness" stroke-linecap="round" />
      <path :d="valuePath" fill="none" :stroke="color" :stroke-width="thickness" stroke-linecap="round"
            style="transition: d .6s ease;" />
      <text :x="tick0.x" :y="tick0.y" text-anchor="middle" fill="var(--text-muted)" style="font-size:10px;">0</text>
      <text :x="tick100.x" :y="tick100.y" text-anchor="middle" fill="var(--text-muted)" style="font-size:10px;">100</text>
      <line :x1="cx" :y1="cy" :x2="needleTip.x" :y2="needleTip.y"
            :stroke="color" stroke-width="3" stroke-linecap="round" style="transition: all .6s ease;" />
      <circle :cx="cx" :cy="cy" r="6" :fill="color" />
    </svg>
    <div class="fw-bold text-center" style="font-size:22px; color:var(--text-heading);">{{ displayValue }}</div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  value:        { type: Number, required: true }, // 0-100
  label:        { type: String, default: '' },
  displayValue: { type: String, default: '' },     // ví dụ "82%" — mặc định tự lấy value + "%"
  size:         { type: Number, default: 140 },
  thickness:    { type: Number, default: 14 },
  color:        { type: String, default: '#f43f5e' },
  icon:         { type: Object, default: null },
});

const pad = 16;
const r  = computed(() => props.size / 2 - props.thickness / 2 - 2);
const cx = computed(() => props.size / 2 + pad);
const cy = computed(() => props.size / 2);

const point = (pct, radius) => {
  const theta = (180 - (pct / 100) * 180) * (Math.PI / 180);
  return { x: cx.value + radius * Math.cos(theta), y: cy.value - radius * Math.sin(theta) };
};

const bgPath = computed(() => {
  const s = point(0, r.value), e = point(100, r.value);
  return `M ${s.x},${s.y} A ${r.value},${r.value} 0 0 1 ${e.x},${e.y}`;
});
const valuePath = computed(() => {
  const clamped = Math.min(100, Math.max(0, props.value));
  const s = point(0, r.value), e = point(clamped, r.value);
  // Cung nửa hình tròn (0-180°) không bao giờ vượt 180° nên large-arc-flag luôn là 0
  return `M ${s.x},${s.y} A ${r.value},${r.value} 0 0 1 ${e.x},${e.y}`;
});

const tick0   = computed(() => point(-2, r.value + 12));
const tick100 = computed(() => point(102, r.value + 12));

const needleTip = computed(() => {
  const clamped = Math.min(100, Math.max(0, props.value));
  return point(clamped, r.value - props.thickness / 2 - 6);
});

const displayValue = computed(() => props.displayValue || `${Math.round(props.value)}%`);
</script>
