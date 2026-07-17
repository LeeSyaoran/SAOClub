<template>
  <!-- Biểu đồ cột doanh thu theo ngày — vẽ bằng SVG thuần, không cần thư viện ngoài -->
  <div>
    <svg :width="width" :height="height" :viewBox="`0 0 ${width} ${height}`" style="width:100%;height:auto;">
      <g v-for="(bar, i) in bars" :key="i">
        <rect :x="bar.x" :y="bar.y" :width="barWidth" :height="bar.barHeight"
              :fill="hoverIndex === i ? 'var(--accent-fg)' : 'var(--accent)'"
              rx="2"
              style="cursor:pointer;transition:fill .15s ease;"
              @mouseenter="hoverIndex = i" @mouseleave="hoverIndex = null" />
        <text v-if="i % labelStep === 0"
              :x="bar.x + barWidth / 2" :y="height - 4"
              text-anchor="middle" style="font-size:9px;fill:var(--text-secondary);">{{ bar.label }}</text>
      </g>
    </svg>
    <div v-if="hoverIndex !== null" class="small mt-1" style="color:var(--text-secondary);">
      {{ bars[hoverIndex].label }}: <strong style="color:var(--text-heading);">{{ formatPrice(data[hoverIndex].doanhThu) }}</strong>
    </div>
    <div v-if="data.length === 0" class="text-secondary small text-center py-4">{{ emptyText }}</div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';

const props = defineProps({
  data:      { type: Array, required: true }, // [{ ngay: 'YYYY-MM-DD', doanhThu: number }]
  width:     { type: Number, default: 600 },
  height:    { type: Number, default: 160 },
  emptyText: { type: String, default: '' },
});

const hoverIndex = ref(null);
const barGap = 2;
const barWidth = computed(() => props.data.length ? Math.max(4, props.width / props.data.length - barGap) : 0);
// Nhãn thưa dần khi nhiều cột — tối đa ~12 nhãn hiện trên trục X để khỏi chồng chữ.
const labelStep = computed(() => Math.max(1, Math.ceil(props.data.length / 12)));

const maxValue = computed(() => Math.max(1, ...props.data.map(d => Number(d.doanhThu) || 0)));
const chartHeight = computed(() => props.height - 16); // chừa chỗ nhãn trục X

const bars = computed(() => props.data.map((d, i) => {
  const value = Number(d.doanhThu) || 0;
  const barHeight = (value / maxValue.value) * (chartHeight.value - 4);
  const [, m, day] = d.ngay.split('-');
  return {
    x: i * (props.width / props.data.length),
    y: chartHeight.value - barHeight,
    barHeight,
    label: `${day}/${m}`,
  };
}));

const formatPrice = (v) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v || 0);
</script>
