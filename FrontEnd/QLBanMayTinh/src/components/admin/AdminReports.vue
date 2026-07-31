<script setup>
import { t } from "../../i18n/index.js";
import { formatPrice } from "../../utils/adminFormat.js";
import RevenueBarChart from "../common/RevenueBarChart.vue";

defineProps({
  totalRevenue: Number,
  activeProducts: Number,
  activePromos: Number,
  lowStockItems: { type: Array, default: () => [] },
  reportsGroupBy: String,
  reportsDateRange: String,
  reportsCustomFrom: String,
  reportsCustomTo: String,
  reportsRevenueChartData: { type: Array, default: () => [] },
  reportsOrdersByStatus: { type: Array, default: () => [] },
  reportsTopSelling: { type: Array, default: () => [] },
  reportsCustomerReport: { type: Object, default: () => ({ topKhach: [], tyLeMuaLai: 0, tongSoKhach: 0 }) },
  reportsRepeatRateText: String,
});

const emit = defineEmits([
  "update:reportsGroupBy",
  "update:reportsDateRange",
  "update:reportsCustomFrom",
  "update:reportsCustomTo",
]);
</script>

<template>
  <section>
    <div class="row g-3 mb-4">
      <div class="col-6 col-xl-3">
        <div class="card border-secondary" style="background:var(--bg-hover);"><div class="card-body">
          <div class="text-secondary small mb-1">{{ t('admin.reports.totalRevenue') }}</div>
          <div class="fw-bold" style="font-size:1.1rem;">{{ formatPrice(totalRevenue) }}</div>
        </div></div>
      </div>
      <div class="col-6 col-xl-3">
        <div class="card border-secondary" style="background:var(--bg-hover);"><div class="card-body">
          <div class="text-secondary small mb-1">{{ t('admin.reports.activeProducts') }}</div>
          <div class="fw-bold" style="font-size:1.55rem;">{{ activeProducts }}</div>
        </div></div>
      </div>
      <div class="col-6 col-xl-3">
        <div class="card border-secondary" style="background:var(--bg-hover);"><div class="card-body">
          <div class="text-secondary small mb-1">{{ t('admin.reports.activePromotions') }}</div>
          <div class="fw-bold" style="font-size:1.55rem;">{{ activePromos }}</div>
        </div></div>
      </div>
      <div class="col-6 col-xl-3">
        <div class="card border-secondary" style="background:var(--bg-hover);"><div class="card-body">
          <div class="text-secondary small mb-1">{{ t('admin.reports.lowStockVariants') }}</div>
          <div class="fw-bold" :class="lowStockItems.length?'text-danger':''" style="font-size:1.55rem;">{{ lowStockItems.length }}</div>
        </div></div>
      </div>
    </div>
    <div class="d-flex flex-wrap align-items-center gap-3 mb-3 px-3 py-2 rounded-3" style="background:var(--bg-card-alt);">
      <div class="d-flex align-items-center gap-2">
        <span class="text-secondary small">{{ t('admin.reports.groupByLabel') }}</span>
        <div class="d-flex align-items-center gap-1 rounded-pill p-1" style="background:var(--bg-input);">
          <button v-for="opt in ['day','month','year']" :key="opt" type="button"
                  class="btn btn-sm border-0 rounded-pill px-3 py-1"
                  :style="reportsGroupBy===opt
                    ? 'background:var(--accent);color:var(--accent-text);font-weight:600;'
                    : 'background:transparent;color:var(--text-secondary);'"
                  @click="$emit('update:reportsGroupBy', opt)">
            {{ t(`admin.reports.groupBy${opt.charAt(0).toUpperCase()}${opt.slice(1)}`) }}
          </button>
        </div>
      </div>
      <div v-if="reportsGroupBy==='day'" class="d-flex flex-wrap align-items-center gap-2"
           style="border-left:1px solid var(--border-color-soft); padding-left:0.9rem;">
        <div class="d-flex align-items-center gap-1 rounded-pill p-1" style="background:var(--bg-input);">
          <button v-for="opt in ['today','week','month','custom']" :key="opt" type="button"
                  class="btn btn-sm border-0 rounded-pill px-3 py-1"
                  :style="reportsDateRange===opt
                    ? 'background:var(--accent);color:var(--accent-text);font-weight:600;'
                    : 'background:transparent;color:var(--text-secondary);'"
                  @click="$emit('update:reportsDateRange', opt)">
            {{ t(`admin.reports.dateRange${opt.charAt(0).toUpperCase()}${opt.slice(1)}`) }}
          </button>
        </div>
        <template v-if="reportsDateRange==='custom'">
          <input type="date" :value="reportsCustomFrom" @input="$emit('update:reportsCustomFrom', $event.target.value)"
                 class="form-control form-control-sm"
                 :aria-label="t('admin.reports.dateFrom')"
                 style="width:auto;background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
          <span class="text-secondary small">→</span>
          <input type="date" :value="reportsCustomTo" @input="$emit('update:reportsCustomTo', $event.target.value)"
                 class="form-control form-control-sm"
                 :aria-label="t('admin.reports.dateTo')"
                 style="width:auto;background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
        </template>
      </div>
    </div>
    <div class="small fw-semibold text-secondary mb-2">📈 {{ t('admin.reports.revenueChartTitle') }}</div>
    <div class="card border-secondary mb-4" style="background:var(--bg-hover);"><div class="card-body">
      <RevenueBarChart :data="reportsRevenueChartData" :granularity="reportsGroupBy" :empty-text="t('admin.reports.revenueChartEmpty')" />
    </div></div>
    <div class="small fw-semibold text-secondary mb-2">🍩 {{ t('admin.reports.ordersByStatus') }}</div>
    <div class="table-responsive mb-4">
      <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
        <thead><tr><th>{{ t('admin.reports.colStatus') }}</th><th>{{ t('admin.reports.colQuantity') }}</th></tr></thead>
        <tbody>
          <tr v-for="row in reportsOrdersByStatus" :key="row.status">
            <td><span class="badge" :style="{ background: row.color.bg, color: row.color.text }">{{ row.label }}</span></td>
            <td><strong>{{ row.count }}</strong></td>
          </tr>
          <tr v-if="reportsOrdersByStatus.length===0"><td colspan="2" class="text-center text-secondary">{{ t('admin.reports.emptyOrders') }}</td></tr>
        </tbody>
      </table>
    </div>
    <div class="small fw-semibold text-secondary mb-2">🔥 {{ t('admin.reports.topProducts') }}</div>
    <div class="table-responsive">
      <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
        <thead><tr><th>{{ t('admin.reports.colIndex') }}</th><th>{{ t('admin.reports.colName') }}</th><th>{{ t('admin.reports.colQuantitySold') }}</th></tr></thead>
        <tbody>
          <tr v-for="(p,i) in reportsTopSelling" :key="p.tenSanPham">
            <td class="text-secondary">{{ i+1 }}</td><td>{{ p.tenSanPham }}</td><td>{{ p.soLuongDaBan }}</td>
          </tr>
          <tr v-if="reportsTopSelling.length===0"><td colspan="3" class="text-center text-secondary">{{ t('admin.reports.emptyOrders') }}</td></tr>
        </tbody>
      </table>
    </div>
    <div class="small fw-semibold text-secondary mb-2 mt-4">🏆 {{ t('admin.reports.customersTitle') }}</div>
    <div class="text-secondary small mb-2">{{ reportsRepeatRateText }}</div>
    <div class="table-responsive">
      <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
        <thead><tr><th>{{ t('admin.reports.colIndex') }}</th><th>{{ t('admin.reports.colCustomerName') }}</th><th>{{ t('admin.reports.colOrderCount') }}</th><th>{{ t('admin.reports.colTotalSpent') }}</th></tr></thead>
        <tbody>
          <tr v-for="(c,i) in reportsCustomerReport.topKhach" :key="c.khachHangId">
            <td class="text-secondary">{{ i+1 }}</td><td>{{ c.hoTen }}</td><td>{{ c.soDonHang }}</td><td>{{ formatPrice(c.tongChiTieu) }}</td>
          </tr>
          <tr v-if="reportsCustomerReport.topKhach.length===0"><td colspan="4" class="text-center text-secondary">{{ t('admin.reports.customersEmpty') }}</td></tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
