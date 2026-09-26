<template>
  <!-- Biểu đồ dạng thanh ngang -->
  <div v-if="!vertical" class="barchart-horizontal d-flex flex-column gap-3">
    <div
      v-for="(row, i) in rows"
      :key="i"
      class="barchart-row d-flex align-items-center gap-2.5 p-1 rounded-2"
    >
      <!-- Huy hiệu thứ hạng (Rank) -->
      <div
        v-if="showRank"
        class="rank-badge flex-shrink-0 d-flex align-items-center justify-content-center fw-bold"
        :class="`rank-${i + 1}`"
      >
        {{ i + 1 }}
      </div>

      <!-- Thumbnail ảnh sản phẩm (nếu có) -->
      <div
        v-if="row.image !== undefined"
        class="barchart-thumb rounded-2 d-flex align-items-center justify-content-center overflow-hidden flex-shrink-0"
      >
        <img
          v-if="row.image"
          :src="row.image"
          :alt="row.label"
          style="width:100%; height:100%; object-fit:contain; padding:2px;"
        />
        <span v-else><Laptop :size="15" color="var(--text-muted)" /></span>
      </div>

      <!-- Tiêu đề / Tên nhãn -->
      <div
        class="text-truncate flex-shrink-0"
        style="width:130px; font-size:12px; font-weight:500; color:var(--text-primary);"
        :title="row.label"
      >
        {{ row.label }}
      </div>

      <!-- Thanh tiến độ có track mờ -->
      <div class="flex-grow-1 barchart-track rounded-pill overflow-hidden position-relative">
        <div
          class="h-100 rounded-pill barchart-fill"
          :style="{
            width: row.pct + '%',
            background: row.color ? `linear-gradient(90deg, ${row.color}cc, ${row.color})` : 'var(--gradient-brand)',
            boxShadow: row.color ? `0 0 8px ${row.color}40` : 'none',
          }"
        ></div>
      </div>

      <!-- Giá trị hiển thị bên phải -->
      <div
        class="fw-bold text-end flex-shrink-0"
        style="min-width:70px; font-size:12px; color:var(--text-heading);"
      >
        <span class="badge rounded-pill bg-body-tertiary text-body fw-bold px-2 py-1">
          {{ row.displayValue ?? row.value }}
        </span>
      </div>
    </div>

    <div v-if="rows.length === 0" class="small py-3 text-center" style="color:var(--text-muted);">
      {{ emptyText }}
    </div>
  </div>

  <!-- Biểu đồ cột đứng (Vertical) -->
  <div v-else class="barchart-vertical-wrapper">
    <div class="d-flex align-items-end gap-2 gap-sm-3" style="height:160px; padding-top:20px;">
      <div
        v-for="(row, i) in rows"
        :key="i"
        class="barchart-col d-flex flex-column align-items-center flex-grow-1 h-100 position-relative"
        @mouseenter="hoveredCol = i"
        @mouseleave="hoveredCol = null"
      >
        <!-- Tooltip giá trị trên đầu cột khi hover -->
        <div
          v-if="hoveredCol === i"
          class="col-tooltip position-absolute"
          style="top:-24px; font-size:10.5px; z-index:5;"
        >
          {{ formatPriceTooltip(row.value) }}
        </div>

        <!-- Cột và Track -->
        <div class="flex-grow-1 d-flex align-items-end w-100 col-track rounded-top-2 position-relative">
          <div
            class="w-100 rounded-top-2 col-fill"
            :style="{
              height: Math.max(row.pct, row.value > 0 ? 5 : 0) + '%',
              background: row.color ? `linear-gradient(180deg, ${row.color}, ${row.color}aa)` : 'var(--gradient-brand)',
              boxShadow: hoveredCol === i ? `0 -2px 10px ${row.color || 'var(--accent)'}88` : 'none',
            }"
          ></div>
        </div>

        <!-- Nhãn cột -->
        <div class="text-truncate mt-1.5 fw-semibold" style="font-size:11px; color:var(--text-secondary); max-width:100%;">
          {{ row.label }}
        </div>
        <!-- Giá trị vắn tắt -->
        <div class="fw-bold text-truncate" style="font-size:10px; color:var(--text-heading); max-width:100%;">
          {{ row.displayValue ?? formatCompact(row.value) }}
        </div>
      </div>
    </div>

    <div v-if="rows.length === 0" class="small py-3 text-center" style="color:var(--text-muted);">
      {{ emptyText }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { Laptop } from '@lucide/vue';

const props = defineProps({
  data:      { type: Array, required: true }, // [{ label, value, color?, displayValue?, image? }]
  emptyText: { type: String, default: '' },
  vertical:  { type: Boolean, default: false },
  showRank:  { type: Boolean, default: true },
});

const hoveredCol = ref(null);

const max = computed(() => Math.max(...props.data.map(d => Number(d.value) || 0), 1));
const rows = computed(() =>
  props.data.map(d => {
    const val = Number(d.value) || 0;
    return {
      ...d,
      value: val,
      pct: val > 0 ? Math.max(5, (val / max.value) * 100) : 0,
    };
  })
);

const formatCompact = (val) => {
  if (val >= 1000000) return (val / 1000000).toFixed(1) + 'M';
  if (val >= 1000) return (val / 1000).toFixed(0) + 'k';
  return String(val);
};

const formatPriceTooltip = (val) => {
  return new Intl.NumberFormat('vi-VN').format(val) + ' ₫';
};
</script>

<style scoped>
.barchart-track {
  background: var(--bg-card-inset, rgba(255,255,255,0.06));
  height: 10px;
}
.barchart-fill {
  transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}
.barchart-thumb {
  width: 34px;
  height: 30px;
  background: var(--bg-card-inset, rgba(255,255,255,0.06));
  border: 1px solid var(--border-color-soft, rgba(255,255,255,0.1));
}
.rank-badge {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  font-size: 11px;
  background: var(--bg-card-inset, rgba(255,255,255,0.08));
  color: var(--text-secondary);
}
.rank-1 {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: #ffffff;
  box-shadow: 0 2px 6px rgba(245, 158, 11, 0.4);
}
.rank-2 {
  background: linear-gradient(135deg, #94a3b8, #64748b);
  color: #ffffff;
}
.rank-3 {
  background: linear-gradient(135deg, #b45309, #78350f);
  color: #ffffff;
}
.col-track {
  background: var(--bg-card-inset, rgba(255,255,255,0.06));
  height: 100%;
}
.col-fill {
  transition: height 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}
.col-tooltip {
  background: var(--bg-card-inset, #1e1e2d);
  color: var(--text-heading, #ffffff);
  border: 1px solid var(--border-color, rgba(255,255,255,0.12));
  padding: 2px 6px;
  border-radius: 6px;
  white-space: nowrap;
  pointer-events: none;
}
</style>
