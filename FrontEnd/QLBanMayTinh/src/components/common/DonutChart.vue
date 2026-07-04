<template>
  <!-- Biểu đồ tròn (donut) — vẽ bằng SVG thuần, không cần thư viện ngoài -->
  <div class="d-flex align-items-center gap-4 flex-wrap">
    <svg :width="size" :height="size" :viewBox="`0 0 ${size} ${size}`" style="flex-shrink:0;">
      <!-- Nền vòng tròn khi không có dữ liệu -->
      <circle v-if="total === 0"
              :cx="size/2" :cy="size/2" :r="radius"
              fill="none" stroke="var(--bg-hover)" :stroke-width="thickness" />
      <circle
        v-for="(seg, i) in segments" :key="i"
        :cx="size/2" :cy="size/2" :r="radius"
        fill="none" :stroke="seg.color" :stroke-width="thickness"
        :stroke-dasharray="`${seg.dash} ${circumference - seg.dash}`"
        :stroke-dashoffset="-seg.offset"
        stroke-linecap="butt"
        style="transform:rotate(-90deg); transform-origin:center; transition:stroke-dasharray .5s ease;"
      />
      <text v-if="centerValue" :x="size/2" :y="size/2 - 3" text-anchor="middle"
            fill="var(--text-heading)" style="font-size:19px; font-weight:800;">{{ centerValue }}</text>
      <text v-if="centerLabel" :x="size/2" :y="size/2 + 16" text-anchor="middle"
            fill="var(--text-secondary)" style="font-size:10px;">{{ centerLabel }}</text>
    </svg>

    <!-- Chú giải -->
    <div class="d-flex flex-column gap-2" style="min-width:140px;">
      <div v-for="(seg, i) in segments" :key="i" class="d-flex align-items-center gap-2">
        <span class="rounded-circle flex-shrink-0" :style="{ background: seg.color, width: '9px', height: '9px' }"></span>
        <span class="flex-grow-1" style="font-size:12px; color:var(--text-primary);">{{ seg.label }}</span>
        <span class="fw-bold" style="font-size:12px; color:var(--text-secondary);">{{ seg.value }}</span>
      </div>
      <div v-if="segments.length === 0" class="small" style="color:var(--text-muted);">{{ emptyText }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  data:        { type: Array,  required: true }, // [{ label, value, color }]
  size:        { type: Number, default: 150 },
  thickness:   { type: Number, default: 22 },
  centerLabel: { type: String, default: '' },
  centerValue: { type: String, default: '' },
  emptyText:   { type: String, default: '' },
});

const radius = computed(() => props.size / 2 - props.thickness / 2 - 2);
const circumference = computed(() => 2 * Math.PI * radius.value);
const total = computed(() => props.data.reduce((s, d) => s + (d.value || 0), 0));

const segments = computed(() => {
  if (total.value === 0) return [];
  let running = 0;
  return props.data
    .filter(d => d.value > 0)
    .map(d => {
      const dash = (d.value / total.value) * circumference.value;
      const seg = { ...d, dash, offset: running };
      running += dash;
      return seg;
    });
});
</script>
