<script setup>
import { t } from "../../i18n/index.js";
import { formatPrice } from "../../utils/adminFormat.js";
import RevenueBarChart from "../common/RevenueBarChart.vue";
import { TrendingUp, PieChart, Flame, Trophy, Package, Tag, DollarSign } from '@lucide/vue';

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
    <!-- ═══════ KPI Cards ═══════ -->
    <div class="row g-3 mb-4">
      <div class="col-6 col-xl-3">
        <div class="alt-card h-100" style="border-color:var(--accent-2);">
          <div class="card-body">
            <div class="d-flex align-items-center gap-2 mb-1">
              <span class="rounded-2 d-flex align-items-center justify-content-center" style="width:32px;height:32px;background:rgba(240,107,129,0.15);">
                <DollarSign :size="16" style="color:#f06b81;" />
              </span>
              <span class="small text-secondary">{{ t('admin.reports.totalRevenue') }}</span>
            </div>
            <div class="fw-black" style="font-size:1.35rem;color:var(--text-heading);">{{ formatPrice(totalRevenue) }}</div>
          </div>
        </div>
      </div>
      <div class="col-6 col-xl-3">
        <div class="alt-card h-100" style="border-color:var(--accent);">
          <div class="card-body">
            <div class="d-flex align-items-center gap-2 mb-1">
              <span class="rounded-2 d-flex align-items-center justify-content-center" style="width:32px;height:32px;background:rgba(var(--accent-rgb),0.15);">
                <Package :size="16" style="color:var(--accent);" />
              </span>
              <span class="small text-secondary">{{ t('admin.reports.activeProducts') }}</span>
            </div>
            <div class="fw-black" style="font-size:1.55rem;color:var(--text-heading);">{{ activeProducts }}</div>
          </div>
        </div>
      </div>
      <div class="col-6 col-xl-3">
        <div class="alt-card h-100" style="border-color:#22c55e;">
          <div class="card-body">
            <div class="d-flex align-items-center gap-2 mb-1">
              <span class="rounded-2 d-flex align-items-center justify-content-center" style="width:32px;height:32px;background:rgba(34,197,94,0.15);">
                <Tag :size="16" style="color:#22c55e;" />
              </span>
              <span class="small text-secondary">{{ t('admin.reports.activePromotions') }}</span>
            </div>
            <div class="fw-black" style="font-size:1.55rem;color:var(--text-heading);">{{ activePromos }}</div>
          </div>
        </div>
      </div>
      <div class="col-6 col-xl-3">
        <div class="alt-card h-100" :style="{ borderColor: lowStockItems.length ? '#f87171' : 'var(--border-color)' }">
          <div class="card-body">
            <div class="d-flex align-items-center gap-2 mb-1">
              <span class="rounded-2 d-flex align-items-center justify-content-center" :style="{ width:'32px',height:'32px',background: lowStockItems.length ? 'rgba(248,113,113,0.15)' : 'rgba(var(--text-muted-rgb,128,128,128),0.15)' }">
                <Flame :size="16" :style="{ color: lowStockItems.length ? '#f87171' : 'var(--text-muted,var(--text-secondary))' }" />
              </span>
              <span class="small text-secondary">{{ t('admin.reports.lowStockVariants') }}</span>
            </div>
            <div class="fw-black" :style="{ fontSize:'1.55rem', color: lowStockItems.length ? '#f87171' : 'var(--text-heading)' }">{{ lowStockItems.length }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- ═══════ Filters ═══════ -->
    <div class="d-flex flex-wrap align-items-center gap-3 mb-4 px-3 py-2 rounded-3 alt-card">
      <div class="d-flex align-items-center gap-2">
        <span class="text-secondary small fw-semibold">{{ t('admin.reports.groupByLabel') }}</span>
        <div class="alt-toolbar__tabs">
          <button
            v-for="opt in ['day','month','year']" :key="opt" type="button"
            class="alt-btn"
            :class="{ 'alt-btn--primary': reportsGroupBy === opt }"
            @click="$emit('update:reportsGroupBy', opt)"
          >
            {{ t(`admin.reports.groupBy${opt.charAt(0).toUpperCase()}${opt.slice(1)}`) }}
          </button>
        </div>
      </div>
      <div
        v-if="reportsGroupBy==='day'" class="d-flex flex-wrap align-items-center gap-2"
        style="border-left:1px solid var(--border-color-soft);padding-left:0.9rem;"
      >
        <div class="alt-toolbar__tabs">
          <button
            v-for="opt in ['today','week','month','custom']" :key="opt" type="button"
            class="alt-btn"
            :class="{ 'alt-btn--primary': reportsDateRange === opt }"
            @click="$emit('update:reportsDateRange', opt)"
          >
            {{ t(`admin.reports.dateRange${opt.charAt(0).toUpperCase()}${opt.slice(1)}`) }}
          </button>
        </div>
        <template v-if="reportsDateRange==='custom'">
          <input
            type="date" :value="reportsCustomFrom" class="alt-input"
            style="width:auto;"
            @input="$emit('update:reportsCustomFrom', $event.target.value)"
          />
          <span class="text-secondary">→</span>
          <input
            type="date" :value="reportsCustomTo" class="alt-input"
            style="width:auto;"
            @input="$emit('update:reportsCustomTo', $event.target.value)"
          />
        </template>
      </div>
    </div>

    <!-- ═══════ Revenue Chart ═══════ -->
    <div class="alt-card mb-4">
      <div class="card-body">
        <div class="small fw-semibold text-secondary mb-3 d-flex align-items-center gap-1">
          <TrendingUp :size="14" /> {{ t('admin.reports.revenueChartTitle') }}
        </div>
        <RevenueBarChart :data="reportsRevenueChartData" :granularity="reportsGroupBy" :empty-text="t('admin.reports.revenueChartEmpty')" />
      </div>
    </div>

    <!-- ═══════ Orders by Status ═══════ -->
    <div class="small fw-semibold text-secondary mb-2 d-flex align-items-center gap-1">
      <PieChart :size="14" /> {{ t('admin.reports.ordersByStatus') }}
    </div>
    <div class="table-responsive mb-4">
      <table class="alt-table">
        <thead>
          <tr>
            <th style="width:40%">{{ t('admin.reports.colStatus') }}</th>
            <th style="width:60%">{{ t('admin.reports.colQuantity') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in reportsOrdersByStatus" :key="row.status">
            <td><span class="alt-badge" :style="{ background: row.color?.bg || 'var(--bg-card-alt)', color: row.color?.text || 'var(--text-primary)' }">{{ row.label }}</span></td>
            <td><strong>{{ row.count }}</strong></td>
          </tr>
          <tr v-if="reportsOrdersByStatus.length===0">
            <td colspan="2" class="text-center text-secondary py-4">{{ t('admin.reports.emptyOrders') }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- ═══════ Top Selling Products ═══════ -->
    <div class="small fw-semibold text-secondary mb-2 d-flex align-items-center gap-1">
      <Flame :size="14" /> {{ t('admin.reports.topProducts') }}
    </div>
    <div class="table-responsive mb-4">
      <table class="alt-table">
        <thead>
          <tr>
            <th style="width:10%">#</th>
            <th style="width:55%">{{ t('admin.reports.colName') }}</th>
            <th style="width:35%">{{ t('admin.reports.colQuantitySold') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(p,i) in reportsTopSelling" :key="p.tenSanPham">
            <td>
              <span class="rounded-circle d-inline-flex align-items-center justify-content-center fw-bold" style="width:24px;height:24px;font-size:0.75rem;" :style="{ background: i === 0 ? 'rgba(240,107,129,0.2)' : i === 1 ? 'rgba(34,197,94,0.15)' : i === 2 ? 'rgba(251,191,36,0.15)' : 'var(--bg-card-alt)', color: i === 0 ? '#f06b81' : i === 1 ? '#22c55e' : i === 2 ? '#fbbf24' : 'var(--text-secondary)' }">{{ i+1 }}</span>
            </td>
            <td>{{ p.tenSanPham }}</td>
            <td><span class="fw-semibold" style="color:var(--accent);">{{ p.soLuongDaBan }}</span></td>
          </tr>
          <tr v-if="reportsTopSelling.length===0">
            <td colspan="3" class="text-center text-secondary py-4">{{ t('admin.reports.emptyOrders') }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- ═══════ Customer Report ═══════ -->
    <div class="small fw-semibold text-secondary mb-2 d-flex align-items-center gap-1">
      <Trophy :size="14" /> {{ t('admin.reports.customersTitle') }}
    </div>
    <div class="text-secondary small mb-3">{{ reportsRepeatRateText }}</div>
    <div class="table-responsive">
      <table class="alt-table">
        <thead>
          <tr>
            <th style="width:10%">#</th>
            <th style="width:35%">{{ t('admin.reports.colCustomerName') }}</th>
            <th style="width:25%">{{ t('admin.reports.colOrderCount') }}</th>
            <th style="width:30%">{{ t('admin.reports.colTotalSpent') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(c,i) in reportsCustomerReport.topKhach" :key="c.khachHangId">
            <td>
              <span class="rounded-circle d-inline-flex align-items-center justify-content-center fw-bold" style="width:24px;height:24px;font-size:0.75rem;" :style="{ background: i === 0 ? 'rgba(240,107,129,0.2)' : i === 1 ? 'rgba(34,197,94,0.15)' : i === 2 ? 'rgba(251,191,36,0.15)' : 'var(--bg-card-alt)', color: i === 0 ? '#f06b81' : i === 1 ? '#22c55e' : i === 2 ? '#fbbf24' : 'var(--text-secondary)' }">{{ i+1 }}</span>
            </td>
            <td>{{ c.hoTen }}</td>
            <td><span class="fw-semibold">{{ c.soDonHang }}</span></td>
            <td><span class="fw-semibold" style="color:#22c55e;">{{ formatPrice(c.tongChiTieu) }}</span></td>
          </tr>
          <tr v-if="reportsCustomerReport.topKhach.length===0">
            <td colspan="4" class="text-center text-secondary py-4">{{ t('admin.reports.customersEmpty') }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
