<template>
  <div class="calendar-heatmap-wrapper">
    <!-- Lưới lịch 7 ngày trong tuần -->
    <div class="d-grid gap-1.5" style="grid-template-columns: repeat(7, 1fr);">
      <div
        v-for="dow in DOW_LABELS"
        :key="dow"
        class="text-center fw-semibold py-0.5"
        style="font-size:10px; color:var(--text-muted);"
      >
        {{ dow }}
      </div>

      <div v-for="n in leadingBlanks" :key="'blank' + n" class="heatmap-blank"></div>

      <div
        v-for="cell in cells"
        :key="cell.day"
        class="heatmap-cell rounded-2 d-flex flex-column align-items-center justify-content-center position-relative"
        :class="{ 'ring-today': cell.isToday, 'has-orders': cell.count > 0 }"
        :style="{
          aspectRatio: '1',
          background: cellColor(cell.count),
          color: cell.count > 0 ? '#ffffff' : 'var(--text-muted)',
        }"
        :title="`Ngày ${cell.day}/${month + 1}: ${cell.count} đơn hàng`"
      >
        <span class="day-number fw-semibold">{{ cell.day }}</span>
        <span v-if="cell.count > 0" class="order-dot"></span>
      </div>
    </div>

    <!-- Thanh chú thích thang đo màu sắc -->
    <div class="d-flex align-items-center justify-content-between mt-3 pt-2" style="border-top:1px solid var(--border-color-soft, rgba(255,255,255,0.06)); font-size:10.5px; color:var(--text-muted);">
      <div class="d-flex align-items-center gap-1.5">
        <span>Ít</span>
        <span class="scale-box" style="background:var(--bg-card-inset, rgba(255,255,255,0.06));"></span>
        <span class="scale-box" style="background:color-mix(in srgb, var(--accent-2, #7c3aed) 35%, transparent);"></span>
        <span class="scale-box" style="background:color-mix(in srgb, var(--accent-2, #7c3aed) 65%, transparent);"></span>
        <span class="scale-box" style="background:var(--accent-2, #7c3aed);"></span>
        <span>Nhiều</span>
      </div>
      <div class="fw-semibold" style="color:var(--text-secondary);">
        {{ totalOrders }} đơn trong tháng
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  data:  { type: Array, required: true }, // [{ day, count }]
  month: { type: Number, required: true }, // 0-11
  year:  { type: Number, required: true },
});

const DOW_LABELS = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];

const countByDay = computed(() => new Map(props.data.map(d => [d.day, Number(d.count) || 0])));
const totalOrders = computed(() => props.data.reduce((sum, d) => sum + (Number(d.count) || 0), 0));
const maxCount = computed(() => Math.max(1, ...props.data.map(d => Number(d.count) || 0)));
const daysInMonth = computed(() => new Date(props.year, props.month + 1, 0).getDate());

// getDay() trả 0=Chủ nhật — quy về cột Thứ 2 đầu tuần (0=T2...6=CN) cho khớp DOW_LABELS.
const leadingBlanks = computed(() => (new Date(props.year, props.month, 1).getDay() + 6) % 7);

const TODAY = new Date();
const cells = computed(() =>
  Array.from({ length: daysInMonth.value }, (_, i) => {
    const day = i + 1;
    return {
      day,
      count: countByDay.value.get(day) || 0,
      isToday: props.year === TODAY.getFullYear() && props.month === TODAY.getMonth() && day === TODAY.getDate(),
    };
  })
);

const cellColor = (count) => {
  if (count === 0) return 'var(--bg-card-inset, rgba(255,255,255,0.04))';
  const pct = Math.round((0.35 + (count / maxCount.value) * 0.65) * 100);
  return `color-mix(in srgb, var(--accent-2, #7c3aed) ${pct}%, transparent)`;
};
</script>

<style scoped>
.heatmap-cell {
  font-size: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}
.heatmap-cell:hover {
  transform: scale(1.15);
  z-index: 2;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.25);
  border-color: var(--accent-2, #7c3aed);
}
.day-number {
  line-height: 1;
}
.order-dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: #ffffff;
  margin-top: 2px;
}
.ring-today {
  outline: 2px solid var(--accent, #f43f5e) !important;
  outline-offset: 1px;
  font-weight: 800;
}
.scale-box {
  width: 11px;
  height: 11px;
  border-radius: 3px;
  display: inline-block;
}
</style>
