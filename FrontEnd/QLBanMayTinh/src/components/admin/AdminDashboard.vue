<script setup>
import { computed } from "vue";
import { t } from "../../i18n/index.js";
import { ProductsStore } from "../../stores/products.js";
import { OrdersStore } from "../../stores/orders.js";
import { CustomersStore } from "../../stores/customers.js";
import { InventoryStore } from "../../stores/inventory.js";
import { formatPrice, statusLabel } from "../../utils/adminFormat.js";
import DonutChart from "../common/DonutChart.vue";
import BarChart from "../common/BarChart.vue";
import GaugeChart from "../common/GaugeChart.vue";
import TrendChart from "../common/TrendChart.vue";

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

const gaugeColor = (pct) => (pct >= 70 ? "#22c55e" : pct >= 40 ? "#facc15" : "#f87171");

const anyStoreLoading = computed(() => ProductsStore.loading || OrdersStore.loading || CustomersStore.loading || InventoryStore.loading);
</script>

<template>
  <section>
    <div v-if="anyStoreLoading" class="text-secondary small">{{ t('admin.dashboard.loading') }}</div>
    <template v-else>
      <div class="row g-3 mb-4">
        <div class="col-6 col-xl-2">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body d-flex align-items-center gap-3">
              <div class="rounded-3 d-flex align-items-center justify-content-center flex-shrink-0"
                   style="width:44px;height:44px;background:rgba(96,165,250,0.15);font-size:1.3rem;">💻</div>
              <div>
                <div class="text-secondary small mb-1">{{ t('admin.dashboard.totalProducts') }}</div>
                <div class="fw-bold" style="font-size:1.55rem;">{{ totalProducts }}</div>
              </div>
            </div>
          </div>
        </div>
        <div class="col-6 col-xl-2">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body d-flex align-items-center gap-3">
              <div class="rounded-3 d-flex align-items-center justify-content-center flex-shrink-0"
                   style="width:44px;height:44px;background:rgba(167,139,250,0.15);font-size:1.3rem;">🧾</div>
              <div>
                <div class="text-secondary small mb-1">{{ t('admin.dashboard.totalOrders') }}</div>
                <div class="fw-bold" style="font-size:1.55rem;">{{ totalOrders }}</div>
              </div>
            </div>
          </div>
        </div>
        <div class="col-6 col-xl-2">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body d-flex align-items-center gap-3">
              <div class="rounded-3 d-flex align-items-center justify-content-center flex-shrink-0"
                   style="width:44px;height:44px;background:rgba(52,211,153,0.15);font-size:1.3rem;">👥</div>
              <div>
                <div class="text-secondary small mb-1">{{ t('admin.dashboard.totalCustomers') }}</div>
                <div class="fw-bold" style="font-size:1.55rem;">{{ totalCustomers }}</div>
              </div>
            </div>
          </div>
        </div>
        <div class="col-6 col-xl-3">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body d-flex align-items-center gap-3">
              <div class="rounded-3 d-flex align-items-center justify-content-center flex-shrink-0"
                   style="width:44px;height:44px;background:rgba(244,63,94,0.15);font-size:1.3rem;">💰</div>
              <div>
                <div class="text-secondary small mb-1">{{ t('admin.dashboard.revenueThisMonth') }}</div>
                <div class="d-flex align-items-center gap-2">
                  <span class="fw-bold" style="font-size:1.1rem;">{{ formatPrice(revenueThisMonth) }}</span>
                  <span v-if="revenueThisMonthDelta !== null"
                        class="fw-bold" style="font-size:0.7rem;white-space:nowrap;"
                        :style="{ color: revenueThisMonthDelta >= 0 ? '#22c55e' : '#f87171' }">
                    {{ revenueThisMonthDelta >= 0 ? '▲' : '▼' }} {{ Math.abs(revenueThisMonthDelta) }}%
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="col-6 col-xl-3">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body d-flex align-items-center gap-3">
              <div class="rounded-3 d-flex align-items-center justify-content-center flex-shrink-0"
                   style="width:44px;height:44px;background:rgba(250,204,21,0.15);font-size:1.3rem;">📅</div>
              <div>
                <div class="text-secondary small mb-1">{{ t('admin.dashboard.revenueThisYear') }}</div>
                <div class="fw-bold" style="font-size:1.1rem;">{{ formatPrice(revenueThisYear) }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="lowStockItems.length" class="alert alert-danger small py-2 mb-3 d-flex align-items-center gap-2">
        <span class="rounded-circle d-flex align-items-center justify-content-center flex-shrink-0"
              style="width:22px;height:22px;background:rgba(248,113,113,0.25);font-size:0.85rem;">⚠️</span>
        {{ t('admin.dashboard.lowStockAlert', { count: lowStockItems.length }) }}
      </div>

      <div class="row g-3 mb-4">
        <div class="col-12 col-xl-5">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body">
              <div class="d-flex align-items-center justify-content-between flex-wrap gap-2 mb-2">
                <div class="fw-semibold small text-secondary">🍩 {{ t('admin.dashboard.ordersByStatusChart') }}</div>
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
                <div class="fw-semibold small text-secondary">📅 {{ t('admin.dashboard.ordersByWeekChart') }}</div>
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
              <div class="fw-semibold small text-secondary mb-3">🔥 {{ t('admin.dashboard.topSellingChart') }}</div>
              <BarChart :data="topSellingChart" :empty-text="t('admin.dashboard.chartEmptyOrders')" />
            </div>
          </div>
        </div>
        <div class="col-12 col-xl-6">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body">
              <div class="fw-semibold small text-secondary mb-3">🐌 {{ t('admin.dashboard.slowSellingChart') }}</div>
              <BarChart :data="slowSellingChart" :empty-text="t('admin.dashboard.chartEmptyProducts')" />
            </div>
          </div>
        </div>
      </div>

      <div class="card border-secondary mb-4" style="background:var(--bg-hover);">
        <div class="card-body">
          <div class="fw-semibold small text-secondary mb-3">🩺 {{ t('admin.dashboard.kpiHealth') }}</div>
          <div class="row g-3 text-center">
            <div class="col-12 col-md-4 d-flex justify-content-center">
              <GaugeChart :value="orderCompletionRate" :color="gaugeColor(orderCompletionRate)"
                          :label="'✅ ' + t('admin.dashboard.gaugeCompletion')" />
            </div>
            <div class="col-12 col-md-4 d-flex justify-content-center">
              <GaugeChart :value="paymentRate" :color="gaugeColor(paymentRate)"
                          :label="'💳 ' + t('admin.dashboard.gaugePayment')" />
            </div>
            <div class="col-12 col-md-4 d-flex justify-content-center">
              <GaugeChart :value="stockHealthRate" :color="gaugeColor(stockHealthRate)"
                          :label="'📦 ' + t('admin.dashboard.gaugeStock')" />
            </div>
          </div>
        </div>
      </div>

      <div class="card border-secondary mb-4" style="background:var(--bg-hover);">
        <div class="card-body">
          <div class="fw-semibold small text-secondary mb-3">📈 {{ t('admin.dashboard.revenueTrendChart') }}</div>
          <TrendChart :data="revenueTrendChart" :height="140" color="#f06b81" :empty-text="t('admin.dashboard.chartEmptyOrders')" />
        </div>
      </div>

      <div class="small fw-semibold text-secondary mb-2">🗃️ {{ t('admin.dashboard.recentProducts') }}</div>
      <div class="table-responsive">
        <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
          <thead><tr>
            <th></th><th>🖥️ {{ t('admin.dashboard.colName') }}</th><th>🏷️ {{ t('admin.dashboard.colBrand') }}</th><th>🗂️ {{ t('admin.dashboard.colCategory') }}</th><th>💵 {{ t('admin.dashboard.colPrice') }}</th><th>🔖 {{ t('admin.dashboard.colStatus') }}</th>
          </tr></thead>
          <tbody>
            <tr v-for="p in products.slice(0,5)" :key="p.sanPhamId">
              <td style="width:48px;">
                <div class="rounded-2 d-flex align-items-center justify-content-center overflow-hidden"
                     style="width:38px;height:32px;background:var(--bg-card-inset);">
                  <img v-if="p.hinhAnhChinh" :src="p.hinhAnhChinh" :alt="p.tenSanPham"
                       style="width:100%;height:100%;object-fit:contain;padding:2px;" />
                  <span v-else style="font-size:1rem;">💻</span>
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
