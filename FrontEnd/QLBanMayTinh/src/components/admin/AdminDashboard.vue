<script setup>
import { computed } from "vue";
import { t } from "../../i18n/index.js";
import {
  Laptop, Users, Wallet, Calendar, AlertTriangle, PieChart, Flame,
  Turtle, Activity, TrendingUp, Archive, Monitor, Tag, FolderOpen, Banknote, Bookmark,
} from '@lucide/vue';
import { ProductsStore } from "../../stores/products.js";
import { OrdersStore } from "../../stores/orders.js";
import { CustomersStore } from "../../stores/customers.js";
import { InventoryStore } from "../../stores/inventory.js";
import { StaffStore } from "../../stores/staff.js";
import { formatPrice, statusLabel } from "../../utils/adminFormat.js";
import DonutChart from "../common/DonutChart.vue";
import BarChart from "../common/BarChart.vue";
import TrendChart from "../common/TrendChart.vue";
import RingProgress from "../common/RingProgress.vue";
import DotMatrix from "../common/DotMatrix.vue";
import RadarChart from "../common/RadarChart.vue";
import CalendarHeatmap from "../common/CalendarHeatmap.vue";

const props = defineProps({
  totalProducts: Number,
  totalOrders: Number,
  totalCustomers: Number,
  revenueThisMonth: Number,
  revenueThisMonthDelta: Number,
  revenueThisYear: Number,
  lowStockItems: { type: Array, default: () => [] },
  statusChartDate: String,
  isStatusChartToday: Boolean,
  ordersOnStatusChartDate: { type: Array, default: () => [] },
  orderStatusChartData: { type: Array, default: () => [] },
  weekChartAnchor: String,
  isWeekChartCurrentWeek: Boolean,
  weekChartRangeLabel: String,
  ordersInWeekRange: { type: Array, default: () => [] },
  weekOrderStatusChartData: { type: Array, default: () => [] },
  topSellingChart: { type: Array, default: () => [] },
  slowSellingChart: { type: Array, default: () => [] },
  orderCompletionRate: Number,
  paymentRate: Number,
  stockHealthRate: Number,
  revenueTrendChart: { type: Array, default: () => [] },
  products: { type: Array, default: () => [] },
  activeProductRatio: Number,
  weeklyRevenueChart: { type: Array, default: () => [] },
  monthlyOrderHeat: { type: Array, default: () => [] },
  kpiRadarData: { type: Array, default: () => [] },
  staffByRole: { type: Array, default: () => [] },
});

const emit = defineEmits([
  "update:statusChartDate",
  "update:weekChartAnchor",
  "resetToCurrentWeek",
  "backToToday",
]);

const toDateInputValue = (d) => {
  const y = d.getFullYear(), m = String(d.getMonth() + 1).padStart(2, "0"), day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
};

const anyStoreLoading = computed(() =>
  ProductsStore.loading || OrdersStore.loading || CustomersStore.loading || InventoryStore.loading || !StaffStore.loaded
);

// ensureStaff() không set StaffStore.loading (chỉ refreshStaff() có) — dùng .loaded làm
// cờ chờ thay vì .loading cho đúng với cách store này báo trạng thái.
const weeklyRevenueBarData = computed(() =>
  props.weeklyRevenueChart.map((d) => ({ ...d, color: 'var(--accent-2)', displayValue: formatPrice(d.value) }))
);

// Tính 1 lần lúc mount — component giữ nguyên qua v-show nên không tự cập nhật qua nửa
// đêm, chấp nhận được cho dashboard admin (F5 lại nếu cần đúng tháng mới).
const now = new Date();
const heatmapMonth = now.getMonth();
const heatmapYear = now.getFullYear();
</script>

<template>
  <section>
    <div v-if="anyStoreLoading" class="text-secondary small">{{ t('admin.dashboard.loading') }}</div>
    <template v-else>

      <!-- ══════════ HÀNG 1: Doanh thu/Đơn + 3 ring vận hành + Positions ══════════ -->
      <div class="row g-3 mb-4">
        <div class="col-12 col-xl-3">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body">
              <div class="fw-semibold small text-secondary mb-2 d-flex align-items-center gap-1"><Wallet :size="14" /> {{ t('admin.dashboard.revenueThisMonth') }}</div>
              <div class="d-flex align-items-center gap-2 mb-1 flex-wrap">
                <span class="fw-black" style="font-size:1.5rem; color:var(--text-heading);">{{ formatPrice(revenueThisMonth) }}</span>
                <span v-if="revenueThisMonthDelta !== null" class="fw-bold" style="font-size:0.72rem;"
                      :style="{ color: revenueThisMonthDelta >= 0 ? '#22c55e' : '#f87171' }">
                  {{ revenueThisMonthDelta >= 0 ? '▲' : '▼' }} {{ Math.abs(revenueThisMonthDelta) }}%
                </span>
              </div>
              <div class="fw-bold mb-3" style="font-size:1.1rem; color:var(--text-primary);">
                {{ totalOrders }} <span class="small fw-normal" style="color:var(--text-secondary);">{{ t('admin.dashboard.totalOrders') }}</span>
              </div>
              <div class="d-flex flex-column gap-1 pt-2" style="border-top:1px solid var(--border-color-soft);">
                <div class="d-flex justify-content-between small">
                  <span style="color:var(--text-secondary);">{{ t('admin.dashboard.totalProducts') }}</span>
                  <span class="fw-semibold">{{ totalProducts }}</span>
                </div>
                <div class="d-flex justify-content-between small">
                  <span style="color:var(--text-secondary);">{{ t('admin.dashboard.totalCustomers') }}</span>
                  <span class="fw-semibold">{{ totalCustomers }}</span>
                </div>
                <div class="d-flex justify-content-between small">
                  <span style="color:var(--text-secondary);">{{ t('admin.dashboard.revenueThisYear') }}</span>
                  <span class="fw-semibold">{{ formatPrice(revenueThisYear) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="col-6 col-xl-2">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body d-flex flex-column align-items-center justify-content-center">
              <RingProgress :value="orderCompletionRate" :label="t('admin.dashboard.gaugeCompletion')" />
            </div>
          </div>
        </div>
        <div class="col-6 col-xl-2">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body d-flex flex-column align-items-center justify-content-center">
              <RingProgress :value="paymentRate" :label="t('admin.dashboard.gaugePayment')" />
            </div>
          </div>
        </div>
        <div class="col-6 col-xl-2">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body d-flex flex-column align-items-center justify-content-center">
              <RingProgress :value="stockHealthRate" :label="t('admin.dashboard.gaugeStock')" />
            </div>
          </div>
        </div>

        <div class="col-12 col-xl-3">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body">
              <div class="fw-semibold small text-secondary mb-3 d-flex align-items-center gap-1"><Users :size="14" /> {{ t('admin.dashboard.positions') }}</div>
              <DotMatrix :data="staffByRole" :empty-text="t('admin.dashboard.emptyProducts')" />
            </div>
          </div>
        </div>
      </div>

      <!-- ══════════ HÀNG 2: Doanh thu theo tháng (line lớn) + KPI radar ══════════ -->
      <div class="row g-3 mb-4">
        <div class="col-12 col-xl-7">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body">
              <div class="fw-semibold small text-secondary mb-3 d-flex align-items-center gap-1"><TrendingUp :size="14" /> {{ t('admin.dashboard.revenueTrendChart') }}</div>
              <TrendChart :data="revenueTrendChart" :height="180" color="#f06b81" :empty-text="t('admin.dashboard.chartEmptyOrders')" />
            </div>
          </div>
        </div>
        <div class="col-12 col-xl-5">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body d-flex flex-column align-items-center">
              <div class="fw-semibold small text-secondary mb-2 align-self-start d-flex align-items-center gap-1"><Activity :size="14" /> {{ t('admin.dashboard.kpiRadarChart') }}</div>
              <RadarChart :data="kpiRadarData" :empty-text="t('admin.dashboard.chartEmptyOrders')" />
            </div>
          </div>
        </div>
      </div>

      <!-- ══════════ HÀNG 3: Sản phẩm đang bán + Doanh thu theo tuần + Lịch đơn hàng ══════════ -->
      <div class="row g-3 mb-4">
        <div class="col-12 col-md-4 col-xl-3">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body d-flex flex-column align-items-center justify-content-center h-100">
              <RingProgress :value="activeProductRatio" :label="t('admin.dashboard.activeProducts')" />
            </div>
          </div>
        </div>
        <div class="col-12 col-md-8 col-xl-5">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body">
              <div class="fw-semibold small text-secondary mb-3 d-flex align-items-center gap-1"><Calendar :size="14" /> {{ t('admin.dashboard.weeklyRevenueChart') }}</div>
              <BarChart :data="weeklyRevenueBarData" vertical :empty-text="t('admin.dashboard.chartEmptyOrders')" />
            </div>
          </div>
        </div>
        <div class="col-12 col-xl-4">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body">
              <div class="fw-semibold small text-secondary mb-3 d-flex align-items-center gap-1"><Flame :size="14" /> {{ t('admin.dashboard.orderHeatmap') }}</div>
              <CalendarHeatmap :data="monthlyOrderHeat" :month="heatmapMonth" :year="heatmapYear" />
            </div>
          </div>
        </div>
      </div>

      <div v-if="lowStockItems.length" class="alert alert-danger small py-2 mb-3 d-flex align-items-center gap-2">
        <span class="rounded-circle d-flex align-items-center justify-content-center flex-shrink-0"
              style="width:22px;height:22px;background:rgba(248,113,113,0.25);"><AlertTriangle :size="13" color="#f87171" /></span>
        {{ t('admin.dashboard.lowStockAlert', { count: lowStockItems.length }) }}
      </div>

      <div class="row g-3 mb-4">
        <div class="col-12 col-xl-5">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body">
              <div class="d-flex align-items-center justify-content-between flex-wrap gap-2 mb-2">
                <div class="fw-semibold small text-secondary d-flex align-items-center gap-1"><PieChart :size="14" /> {{ t('admin.dashboard.ordersByStatusChart') }}</div>
                <div class="d-flex align-items-center gap-2">
                  <input type="date" :value="statusChartDate" :max="toDateInputValue(new Date())"
                         @input="$emit('update:statusChartDate', $event.target.value)"
                         class="form-control form-control-sm"
                         style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong); width:auto; font-size:0.78rem; padding:2px 8px;" />
                  <button v-if="!isStatusChartToday" type="button" class="btn btn-sm py-0 px-2"
                          style="font-size:0.72rem; color:var(--accent-fg); border:1px solid var(--border-color-strong);"
                          @click="$emit('backToToday')">
                    {{ t('admin.dashboard.backToToday') }}
                  </button>
                </div>
              </div>
              <div class="mb-3">
                <span class="badge rounded-pill" style="background:var(--bg-card-inset); color:var(--text-secondary); font-weight:600;">
                  {{ isStatusChartToday
                    ? t('admin.dashboard.todayOrders', { count: ordersOnStatusChartDate.length })
                    : t('admin.dashboard.ordersOnDate', { count: ordersOnStatusChartDate.length }) }}
                </span>
              </div>
              <DonutChart
                :data="orderStatusChartData"
                :center-value="String(ordersOnStatusChartDate.length)"
                :center-label="t('admin.dashboard.totalOrders')"
                :empty-text="t('admin.dashboard.chartEmptyOrders')" />
            </div>
          </div>
        </div>
        <div class="col-12 col-xl-7">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body">
              <div class="d-flex align-items-center justify-content-between flex-wrap gap-2 mb-2">
                <div class="fw-semibold small text-secondary d-flex align-items-center gap-1"><Calendar :size="14" /> {{ t('admin.dashboard.ordersByWeekChart') }}</div>
                <div class="d-flex align-items-center gap-2 flex-wrap">
                  <input type="date" :value="weekChartAnchor" :max="toDateInputValue(new Date())"
                         @input="$emit('update:weekChartAnchor', $event.target.value)"
                         class="form-control form-control-sm"
                         style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong); width:auto; font-size:0.78rem; padding:2px 8px;" />
                  <button v-if="!isWeekChartCurrentWeek" type="button" class="btn btn-sm py-0 px-2"
                          style="font-size:0.72rem; color:var(--accent-fg); border:1px solid var(--border-color-strong);"
                          @click="$emit('resetToCurrentWeek')">
                    {{ t('admin.dashboard.backToThisWeek') }}
                  </button>
                </div>
              </div>
              <div class="mb-3 d-flex align-items-center gap-2 flex-wrap">
                <span class="badge rounded-pill" style="background:var(--bg-card-inset); color:var(--text-secondary); font-weight:600;">
                  {{ weekChartRangeLabel }}
                </span>
                <span class="badge rounded-pill" style="background:var(--bg-card-inset); color:var(--text-secondary); font-weight:600;">
                  {{ t('admin.dashboard.ordersInRange', { count: ordersInWeekRange.length }) }}
                </span>
              </div>
              <DonutChart
                :data="weekOrderStatusChartData"
                :center-value="String(ordersInWeekRange.length)"
                :center-label="t('admin.dashboard.totalOrders')"
                :empty-text="t('admin.dashboard.chartEmptyOrders')" />
            </div>
          </div>
        </div>
      </div>

      <div class="row g-3 mb-4">
        <div class="col-12 col-xl-6">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body">
              <div class="fw-semibold small text-secondary mb-3 d-flex align-items-center gap-1"><Flame :size="14" /> {{ t('admin.dashboard.topSellingChart') }}</div>
              <BarChart :data="topSellingChart" :empty-text="t('admin.dashboard.chartEmptyOrders')" />
            </div>
          </div>
        </div>
        <div class="col-12 col-xl-6">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body">
              <div class="fw-semibold small text-secondary mb-3 d-flex align-items-center gap-1"><Turtle :size="14" /> {{ t('admin.dashboard.slowSellingChart') }}</div>
              <BarChart :data="slowSellingChart" :empty-text="t('admin.dashboard.chartEmptyProducts')" />
            </div>
          </div>
        </div>
      </div>

      <div class="small fw-semibold text-secondary mb-2 d-flex align-items-center gap-1"><Archive :size="14" /> {{ t('admin.dashboard.recentProducts') }}</div>
      <div class="table-responsive">
        <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
          <thead><tr>
            <th></th><th><Monitor :size="13" style="vertical-align:-2px;" /> {{ t('admin.dashboard.colName') }}</th><th><Tag :size="13" style="vertical-align:-2px;" /> {{ t('admin.dashboard.colBrand') }}</th><th><FolderOpen :size="13" style="vertical-align:-2px;" /> {{ t('admin.dashboard.colCategory') }}</th><th><Banknote :size="13" style="vertical-align:-2px;" /> {{ t('admin.dashboard.colPrice') }}</th><th><Bookmark :size="13" style="vertical-align:-2px;" /> {{ t('admin.dashboard.colStatus') }}</th>
          </tr></thead>
          <tbody>
            <tr v-for="p in products.slice(0,5)" :key="p.sanPhamId">
              <td style="width:48px;">
                <div class="rounded-2 d-flex align-items-center justify-content-center overflow-hidden"
                     style="width:38px;height:32px;background:var(--bg-card-inset);">
                  <img v-if="p.hinhAnhChinh" :src="p.hinhAnhChinh" :alt="p.tenSanPham"
                       style="width:100%;height:100%;object-fit:contain;padding:2px;" />
                  <span v-else><Laptop :size="16" color="var(--text-muted)" /></span>
                </div>
              </td>
              <td>{{ p.tenSanPham }}</td>
              <td>{{ p.tenThuongHieu }}</td>
              <td>{{ p.tenDanhMuc }}</td>
              <td>{{ formatPrice(p.giaBan) }}</td>
              <td><span class="badge" :class="p.trangThai==='active'?'bg-success':'bg-secondary'">{{ statusLabel(p.trangThai) }}</span></td>
            </tr>
            <tr v-if="products.length===0"><td colspan="6" class="text-center text-secondary">{{ t('admin.dashboard.emptyProducts') }}</td></tr>
          </tbody>
        </table>
      </div>
    </template>
  </section>
</template>
