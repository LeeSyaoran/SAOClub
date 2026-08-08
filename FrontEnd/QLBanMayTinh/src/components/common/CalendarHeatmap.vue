<template>
  <!-- Lưới tháng, đậm nhạt theo count/ngày — dùng CSS grid, không cần SVG. -->
  <div class="d-grid gap-1" style="grid-template-columns: repeat(7, 1fr);">
    <div v-for="dow in DOW_LABELS" :key="dow" class="text-center" style="font-size:9px; color:var(--text-muted);">{{ dow }}</div>
    <div v-for="n in leadingBlanks" :key="'blank' + n"></div>
    <div v-for="cell in cells" :key="cell.day"
         class="rounded-2 d-flex align-items-center justify-content-center"
         :class="{ 'ring-today': cell.isToday }"
         :style="{ aspectRatio: '1', background: cellColor(cell.count), fontSize: '9px', color: cell.count > 0 ? '#fff' : 'var(--text-muted)' }">
      {{ cell.day }}
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

const countByDay = computed(() => new Map(props.data.map(d => [d.day, d.count])));
const maxCount = computed(() => Math.max(1, ...props.data.map(d => d.count)));
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
  if (count === 0) return 'var(--bg-hover)';
  const pct = Math.round((0.35 + (count / maxCount.value) * 0.65) * 100);
  return `color-mix(in srgb, var(--accent-2) ${pct}%, transparent)`;
};
</script>

<style scoped>
.ring-today { outline: 2px solid var(--accent); outline-offset: 1px; }
</style>
