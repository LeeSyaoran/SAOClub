<template>
  <div class="donut-chart-container d-flex align-items-center gap-4 flex-wrap justify-content-center justify-content-sm-start">
    <div class="position-relative flex-shrink-0" :style="{ width: size + 'px', height: size + 'px' }">
      <svg :width="size" :height="size" :viewBox="`0 0 ${size} ${size}`" style="display:block;">
        <!-- Vòng nền xám mờ -->
        <circle
          :cx="size/2" :cy="size/2" :r="radius"
          fill="none" stroke="var(--border-color-soft, rgba(255,255,255,0.06))" :stroke-width="thickness"
        />

        <!-- Vòng tiến độ các phân đoạn -->
        <circle
          v-for="(seg, i) in segments"
          :key="i"
          :cx="size/2" :cy="size/2" :r="radius"
          fill="none"
          :stroke="seg.color"
          :stroke-width="hoveredIndex === i ? thickness + 3 : thickness"
          :stroke-dasharray="`${seg.dash} ${circumference - seg.dash}`"
          :stroke-dashoffset="-seg.offset"
          stroke-linecap="butt"
          style="transform:rotate(-90deg); transform-origin:center; transition: all 0.3s ease; cursor:pointer;"
          :style="{ opacity: hoveredIndex !== null && hoveredIndex !== i ? 0.45 : 1 }"
          @mouseenter="hoveredIndex = i"
          @mouseleave="hoveredIndex = null"
        />

        <!-- Số liệu ở tâm -->
        <text
          v-if="centerValue !== ''"
          :x="size/2"
          :y="size/2 - 2"
          text-anchor="middle"
          fill="var(--text-heading, #ffffff)"
          style="font-size:22px; font-weight:800; letter-spacing:-0.03em;"
        >
          {{ hoveredIndex !== null ? segments[hoveredIndex]?.value : centerValue }}
        </text>
        <text
          v-if="centerLabel"
          :x="size/2"
          :y="size/2 + 18"
          text-anchor="middle"
          fill="var(--text-secondary, #94a3b8)"
          style="font-size:10.5px; font-weight:500;"
        >
          {{ hoveredIndex !== null ? segments[hoveredIndex]?.label : centerLabel }}
        </text>
      </svg>
    </div>

    <!-- Danh sách chú giải (Legend) -->
    <div class="donut-legend d-flex flex-column gap-2 flex-grow-1" style="min-width:160px; max-width:280px;">
      <div
        v-for="(seg, i) in segments"
        :key="i"
        class="donut-legend-item d-flex align-items-center gap-2 p-1 rounded-2"
        :class="{ 'legend-active': hoveredIndex === i }"
        style="cursor:pointer; transition: background 0.15s ease;"
        @mouseenter="hoveredIndex = i"
        @mouseleave="hoveredIndex = null"
      >
        <span
          class="rounded-circle flex-shrink-0"
          :style="{ background: seg.color, width: '10px', height: '10px', boxShadow: `0 0 6px ${seg.color}66` }"
        ></span>
        <span class="flex-grow-1 text-truncate" style="font-size:12px; color:var(--text-primary);">
          {{ seg.label }}
        </span>
        <span class="fw-bold" style="font-size:12px; color:var(--text-heading);">
          {{ seg.value }}
        </span>
        <span
          class="badge rounded-pill fw-semibold"
          style="font-size:10px; background:var(--bg-card-inset, rgba(255,255,255,0.08)); color:var(--text-secondary);"
        >
          {{ total > 0 ? Math.round((seg.value / total) * 100) : 0 }}%
        </span>
      </div>

      <div v-if="segments.length === 0" class="small py-2" style="color:var(--text-muted);">
        {{ emptyText }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';

const props = defineProps({
  data:        { type: Array,  required: true }, // [{ label, value, color }]
  size:        { type: Number, default: 150 },
  thickness:   { type: Number, default: 20 },
  centerLabel: { type: String, default: '' },
  centerValue: { type: String, default: '' },
  emptyText:   { type: String, default: '' },
});

const hoveredIndex = ref(null);

const radius = computed(() => props.size / 2 - props.thickness / 2 - 3);
const circumference = computed(() => 2 * Math.PI * radius.value);
const total = computed(() => props.data.reduce((s, d) => s + (Number(d.value) || 0), 0));

const segments = computed(() => {
  if (total.value === 0) return [];
  let running = 0;
  return props.data
    .filter(d => (Number(d.value) || 0) > 0)
    .map(d => {
      const val = Number(d.value) || 0;
      const dash = (val / total.value) * circumference.value;
      const seg = { ...d, value: val, dash, offset: running };
      running += dash;
      return seg;
    });
});
</script>

<style scoped>
.donut-legend-item:hover,
.donut-legend-item.legend-active {
  background: var(--bg-hover, rgba(255,255,255,0.06));
}
</style>
