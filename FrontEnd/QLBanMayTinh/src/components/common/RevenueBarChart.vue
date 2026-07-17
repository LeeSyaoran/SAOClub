<template>
  <!-- Biểu đồ cột doanh thu theo ngày — vẽ bằng SVG thuần, không cần thư viện ngoài -->
  <div>
    <svg v-if="!allZero" :width="width" :height="height" :viewBox="`0 0 ${width} ${height}`" style="width:100%;height:auto;">
      <!-- gridline đáy (0) và đỉnh (giá trị lớn nhất) — hairline, màu recessive -->
      <line :x1="0" :x2="width" :y1="chartTop" :y2="chartTop" stroke="var(--border-color-soft)" stroke-width="1" />
      <line :x1="0" :x2="width" :y1="chartBottom" :y2="chartBottom" stroke="var(--border-color-soft)" stroke-width="1" />
      <text :x="2" :y="chartTop - 5" style="font-size:9px;fill:var(--text-secondary);">{{ formatCompact(maxValue) }}</text>

      <g v-for="(bar, i) in bars" :key="i">
        <!-- vùng hover phủ cả ô (kể cả khi doanh thu ngày đó = 0) để luôn xem được giá trị -->
        <rect :x="bar.slotX" :y="chartTop" :width="slotWidth" :height="chartBottom - chartTop"
              fill="transparent" style="cursor:pointer;"
              @mouseenter="hoverIndex = i" @mouseleave="hoverIndex = null" />
        <template v-if="bar.barHeight > 0">
          <rect :x="bar.x" :y="bar.y" :width="barWidth" :height="bar.barHeight" rx="4"
                fill="var(--accent)" :style="hoverIndex === i ? 'filter:brightness(1.2);' : ''" />
          <!-- vuông lại đáy cột (chỉ bo góc trên) bằng cách đè 1 dải chữ nhật vuông lên đáy -->
          <rect v-if="bar.barHeight > 4" :x="bar.x" :y="bar.y + bar.barHeight - 4" :width="barWidth" height="4"
                fill="var(--accent)" :style="hoverIndex === i ? 'filter:brightness(1.2);' : ''" />
        </template>
        <text v-if="i % labelStep === 0" :x="bar.slotX + slotWidth / 2" :y="height - 4"
              text-anchor="middle" style="font-size:9px;fill:var(--text-secondary);">{{ bar.label }}</text>
      </g>
    </svg>
    <div class="small mt-1" style="color:var(--text-secondary);min-height:1.2em;">
      <template v-if="hoverIndex !== null">
        {{ bars[hoverIndex].label }}: <strong style="color:var(--text-heading);">{{ formatPrice(data[hoverIndex].doanhThu) }}</strong>
      </template>
    </div>
    <div v-if="allZero" class="text-secondary small text-center py-4">{{ emptyText }}</div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';

const props = defineProps({
  data:        { type: Array, required: true }, // [{ ngay: 'YYYY-MM-DD', doanhThu: number }], liên tục theo đúng đơn vị granularity (đã zero-fill/gộp ở nơi gọi)
  width:       { type: Number, default: 600 },
  height:      { type: Number, default: 160 },
  emptyText:   { type: String, default: '' },
  granularity: { type: String, default: 'day' }, // 'day' | 'month' | 'year' — chỉ ảnh hưởng cách hiển thị nhãn trục X/tooltip
});

const hoverIndex = ref(null);

const allZero = computed(() => props.data.length === 0 || props.data.every(d => !(Number(d.doanhThu) > 0)));

const chartTop = 18; // chừa chỗ cho nhãn giá trị lớn nhất phía trên cột cao nhất
const chartBottom = computed(() => props.height - 16); // chừa chỗ nhãn trục X

const slotWidth = computed(() => props.data.length ? props.width / props.data.length : 0);
// Cột dày tối đa 24px, không kéo giãn hết ô — để trống 2 bên cho thoáng, chỉ co lại khi nhiều cột.
const barWidth = computed(() => Math.min(24, Math.max(4, slotWidth.value - 4)));
// Nhãn thưa dần khi nhiều cột — tối đa ~12 nhãn hiện trên trục X để khỏi chồng chữ.
const labelStep = computed(() => Math.max(1, Math.ceil(props.data.length / 12)));

const maxValue = computed(() => Math.max(1, ...props.data.map(d => Number(d.doanhThu) || 0)));

const bars = computed(() => props.data.map((d, i) => {
  const value = Number(d.doanhThu) || 0;
  const barHeight = (value / maxValue.value) * (chartBottom.value - chartTop);
  const slotX = i * slotWidth.value;
  const [yr, m, day] = d.ngay.split('-');
  const label = props.granularity === 'year' ? yr : props.granularity === 'month' ? `${m}/${yr}` : `${day}/${m}`;
  return {
    slotX,
    x: slotX + (slotWidth.value - barWidth.value) / 2,
    y: chartBottom.value - barHeight,
    barHeight,
    label,
  };
}));

const formatPrice = (v) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v || 0);
// Rút gọn số cho nhãn trục (1.2tr, 850k) — chỉ cần đọc nhanh mức doanh thu, số đủ đã có ở tooltip
const formatCompact = (v) => {
  if (v >= 1_000_000) return `${(v / 1_000_000).toFixed(1).replace(/\.0$/, '')}tr`;
  if (v >= 1_000) return `${Math.round(v / 1000)}k`;
  return `${v}`;
};
</script>
