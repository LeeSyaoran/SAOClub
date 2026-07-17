<template>
  <!-- Biểu đồ cột doanh thu — vẽ bằng SVG thuần, không cần thư viện ngoài. Cột tô gradient
       hồng→tím (đúng cặp màu --accent/--accent-2 dùng cho gradient avatar/nút CTA nơi khác
       trong app), cột cao nhất được gắn nhãn "★ đỉnh" làm điểm nhấn thay vì các cột đều màu. -->
  <div>
    <svg v-if="!allZero" :width="width" :height="height" :viewBox="`0 0 ${width} ${height}`" style="width:100%;height:auto;">
      <defs>
        <linearGradient :id="gradientId" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="var(--accent)" />
          <stop offset="100%" stop-color="var(--accent-2)" />
        </linearGradient>
      </defs>
      <!-- gridline đáy (0) và đỉnh khung vẽ — hairline, màu recessive -->
      <line :x1="0" :x2="width" :y1="chartTop" :y2="chartTop" stroke="var(--border-color-soft)" stroke-width="1" />
      <line :x1="0" :x2="width" :y1="chartBottom" :y2="chartBottom" stroke="var(--border-color-soft)" stroke-width="1" />

      <g v-for="(bar, i) in bars" :key="i">
        <!-- vùng hover phủ cả ô (kể cả khi doanh thu ngày đó = 0) để luôn xem được giá trị -->
        <rect :x="bar.slotX" :y="chartTop" :width="slotWidth" :height="chartBottom - chartTop"
              fill="transparent" style="cursor:pointer;"
              @mouseenter="hoverIndex = i" @mouseleave="hoverIndex = null" />
        <template v-if="bar.barHeight > 0">
          <rect :x="bar.x" :y="bar.y" :width="barWidth" :height="bar.barHeight" rx="4"
                :fill="`url(#${gradientId})`" :style="hoverIndex === i ? 'filter:brightness(1.2);' : ''" />
          <!-- vuông lại đáy cột (chỉ bo góc trên) bằng cách đè 1 dải chữ nhật vuông lên đáy,
               màu trùng điểm cuối gradient (--accent-2) nên nối liền mượt -->
          <rect v-if="bar.barHeight > 4" :x="bar.x" :y="bar.y + bar.barHeight - 4" :width="barWidth" height="4"
                fill="var(--accent-2)" :style="hoverIndex === i ? 'filter:brightness(1.2);' : ''" />
        </template>
        <!-- điểm nhấn: chỉ cột doanh thu cao nhất được gắn nhãn "★ đỉnh" phía trên -->
        <text v-if="i === peakIndex" :x="bar.slotX + slotWidth / 2" :y="bar.y - 8"
              text-anchor="middle" style="font-size:10px;font-weight:700;fill:var(--text-heading);">★ {{ formatCompact(bar.value) }}</text>
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
import { ref, computed, useId } from 'vue';

const props = defineProps({
  data:        { type: Array, required: true }, // [{ ngay: 'YYYY-MM-DD', doanhThu: number }], liên tục theo đúng đơn vị granularity (đã zero-fill/gộp ở nơi gọi)
  width:       { type: Number, default: 600 },
  height:      { type: Number, default: 160 },
  emptyText:   { type: String, default: '' },
  granularity: { type: String, default: 'day' }, // 'day' | 'month' | 'year' — chỉ ảnh hưởng cách hiển thị nhãn trục X/tooltip
});

// id gradient riêng theo instance — phòng khi có 2 biểu đồ cùng lúc trên 1 trang, tránh trùng #id SVG
const gradientId = `revenue-bar-gradient-${useId()}`;

const hoverIndex = ref(null);

const allZero = computed(() => props.data.length === 0 || props.data.every(d => !(Number(d.doanhThu) > 0)));

const chartTop = 28; // chừa chỗ cho nhãn "★ đỉnh" phía trên cột cao nhất
const chartBottom = computed(() => props.height - 16); // chừa chỗ nhãn trục X

const slotWidth = computed(() => props.data.length ? props.width / props.data.length : 0);
// Cột dày tối đa 24px, không kéo giãn hết ô — để trống 2 bên cho thoáng, chỉ co lại khi nhiều cột.
const barWidth = computed(() => Math.min(24, Math.max(4, slotWidth.value - 4)));
// Nhãn thưa dần khi nhiều cột — tối đa ~12 nhãn hiện trên trục X để khỏi chồng chữ.
const labelStep = computed(() => Math.max(1, Math.ceil(props.data.length / 12)));

const maxValue = computed(() => Math.max(1, ...props.data.map(d => Number(d.doanhThu) || 0)));
// Cột doanh thu cao nhất — được gắn nhãn "★ đỉnh" làm điểm nhấn của biểu đồ.
const peakIndex = computed(() => {
  let idx = -1, best = 0;
  props.data.forEach((d, i) => {
    const v = Number(d.doanhThu) || 0;
    if (v > best) { best = v; idx = i; }
  });
  return idx;
});

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
    value,
    label,
  };
}));

const formatPrice = (v) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v || 0);
// Rút gọn số cho nhãn trên biểu đồ (1.2tr, 850k) — số đủ đã có ở tooltip khi hover
const formatCompact = (v) => {
  if (v >= 1_000_000) return `${(v / 1_000_000).toFixed(1).replace(/\.0$/, '')}tr`;
  if (v >= 1_000) return `${Math.round(v / 1000)}k`;
  return `${v}`;
};
</script>
