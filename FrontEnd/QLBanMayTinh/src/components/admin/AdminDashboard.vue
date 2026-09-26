<script setup>
import { ref, computed } from "vue";
import { t } from "../../i18n/index.js";
import {
  Laptop, Users, Wallet, Calendar, AlertTriangle, PieChart, Flame,
  Turtle, TrendingUp, Archive, Monitor, Tag, FolderOpen, Banknote, Bookmark,
  ShoppingBag, ArrowUpRight, ArrowDownRight, ChevronRight, Boxes,
  CheckCircle2
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

const props = defineProps({
  totalProducts: { type: Number, default: 0 },
  totalOrders: { type: Number, default: 0 },
  totalCustomers: { type: Number, default: 0 },
  revenueThisMonth: { type: Number, default: 0 },
  revenueThisMonthDelta: { type: Number, default: null },
  revenueThisYear: { type: Number, default: 0 },
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
  orderCompletionRate: { type: Number, default: 0 },
  paymentRate: { type: Number, default: 0 },
  stockHealthRate: { type: Number, default: 0 },
  revenueTrendChart: { type: Array, default: () => [] },
  products: { type: Array, default: () => [] },
  activeProductRatio: { type: Number, default: 0 },
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
  "navigate",
]);

// Tab chuyển đổi chế độ xem trạng thái đơn hàng (Ngày / Tuần)
const orderStatusMode = ref('day'); // 'day' | 'week'

// Tab chuyển đổi hiệu suất sản phẩm (Bán chạy / Bán chậm)
const productSalesTab = ref('top'); // 'top' | 'slow'

const toDateInputValue = (d) => {
  const y = d.getFullYear(), m = String(d.getMonth() + 1).padStart(2, "0"), day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
};

const anyStoreLoading = computed(() =>
  ProductsStore.loading || OrdersStore.loading || CustomersStore.loading || InventoryStore.loading || !StaffStore.loaded
);

const weeklyRevenueBarData = computed(() =>
  props.weeklyRevenueChart.map((d) => ({ ...d, color: 'var(--accent-2, #7c3aed)' }))
);

const weeklyTotalRevenue = computed(() =>
  props.weeklyRevenueChart.reduce((sum, item) => sum + (Number(item.value) || 0), 0)
);

const now = new Date();
const formattedCurrentDate = computed(() => {
  const d = String(now.getDate()).padStart(2, '0');
  const m = String(now.getMonth() + 1).padStart(2, '0');
  const y = now.getFullYear();
  return `${d}/${m}/${y}`;
});
</script>

<template>
  <section class="dashboard-compact">
    <!-- Trạng thái tải dữ liệu -->
    <div v-if="anyStoreLoading" class="d-flex align-items-center justify-content-center p-5">
      <div class="spinner-border text-primary me-3" role="status">
        <span class="visually-hidden">Loading...</span>
      </div>
      <span class="fw-semibold text-secondary">{{ t('admin.dashboard.loading') }}</span>
    </div>

    <template v-else>
      <!-- ══════════════════════════════════════════════════════════════════════════
           1. COMPACT HEADER BAR
           ══════════════════════════════════════════════════════════════════════════ -->
      <div class="d-flex align-items-center justify-content-between flex-wrap gap-2 mb-3">
        <div>
          <h1 class="fw-bold fs-5 mb-0 text-heading">Tổng quan kinh doanh</h1>
          <span class="text-secondary small" style="font-size:0.8rem;">
            Dữ liệu tài chính, hiệu suất đơn hàng và tình trạng tồn kho thời gian thực
          </span>
        </div>

        <div class="d-flex align-items-center gap-2">
          <div class="status-chip d-flex align-items-center gap-1.5 px-2.5 py-1 rounded-pill small">
            <span class="pulse-dot"></span>
            <span class="fw-medium" style="font-size:0.75rem;">Trực tuyến</span>
          </div>
          <div class="date-chip d-flex align-items-center gap-1 px-2.5 py-1 rounded-pill text-secondary small" style="font-size:0.75rem;">
            <Calendar :size="12" />
            <span>{{ formattedCurrentDate }}</span>
          </div>
        </div>
      </div>

      <!-- ══════════════════════════════════════════════════════════════════════════
           2. 4 EXECUTIVE KPI CARDS (GỌN GÀNG, TƯƠNG PHẢN CAO)
           ══════════════════════════════════════════════════════════════════════════ -->
      <div class="row g-2.5 mb-3">
        <!-- KPI 1: Doanh thu tháng -->
        <div class="col-12 col-sm-6 col-xl-3">
          <div class="kpi-card card border-0 h-100">
            <div class="card-body p-3 d-flex flex-column justify-content-between">
              <div>
                <div class="d-flex align-items-center justify-content-between mb-1.5">
                  <span class="text-secondary small fw-medium">{{ t('admin.dashboard.revenueThisMonth') }}</span>
                  <div class="kpi-icon icon-emerald rounded-2 d-flex align-items-center justify-content-center">
                    <Wallet :size="16" />
                  </div>
                </div>

                <div class="kpi-val fw-black text-heading mb-1">
                  {{ formatPrice(revenueThisMonth) }}
                </div>

                <div class="d-flex align-items-center gap-1.5 mb-2">
                  <span
                    v-if="revenueThisMonthDelta !== null"
                    class="badge rounded-pill fw-bold"
                    :class="revenueThisMonthDelta >= 0 ? 'bg-success-subtle text-success' : 'bg-danger-subtle text-danger'"
                    style="font-size:0.72rem; padding: 2px 6px;"
                  >
                    <component :is="revenueThisMonthDelta >= 0 ? ArrowUpRight : ArrowDownRight" :size="11" />
                    {{ revenueThisMonthDelta >= 0 ? '+' : '' }}{{ revenueThisMonthDelta }}%
                  </span>
                  <span class="text-secondary" style="font-size:0.72rem;">so với tháng trước</span>
                </div>
              </div>

              <div class="pt-2 border-top d-flex justify-content-between small text-secondary" style="font-size:0.75rem;">
                <span>{{ t('admin.dashboard.revenueThisYear') }}</span>
                <span class="fw-semibold text-heading">{{ formatPrice(revenueThisYear) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- KPI 2: Đơn hàng -->
        <div class="col-12 col-sm-6 col-xl-3">
          <div
            class="kpi-card card border-0 h-100 kpi-clickable"
            @click="$emit('navigate', 'orders')"
          >
            <div class="card-body p-3 d-flex flex-column justify-content-between">
              <div>
                <div class="d-flex align-items-center justify-content-between mb-1.5">
                  <span class="text-secondary small fw-medium">{{ t('admin.dashboard.totalOrders') }}</span>
                  <div class="kpi-icon icon-indigo rounded-2 d-flex align-items-center justify-content-center">
                    <ShoppingBag :size="16" />
                  </div>
                </div>

                <div class="kpi-val fw-black text-heading mb-1">
                  {{ totalOrders }} <span class="fs-6 fw-normal text-secondary">đơn</span>
                </div>

                <div class="mb-2">
                  <div class="d-flex justify-content-between text-secondary mb-1" style="font-size:0.72rem;">
                    <span>Hoàn tất giao</span>
                    <span class="fw-bold text-indigo">{{ Math.round(orderCompletionRate) }}%</span>
                  </div>
                  <div class="progress" style="height:4px; background:var(--bg-card-inset);">
                    <div
                      class="progress-bar rounded-pill"
                      style="background:linear-gradient(90deg, #6366f1, #06b6d4);"
                      :style="{ width: orderCompletionRate + '%' }"
                    ></div>
                  </div>
                </div>
              </div>

              <div class="pt-2 border-top d-flex justify-content-between small text-secondary" style="font-size:0.75rem;">
                <span>Chi tiết đơn hàng</span>
                <span class="text-indigo fw-semibold d-inline-flex align-items-center gap-0.5">
                  Xem ngay <ChevronRight :size="12" />
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- KPI 3: Khách hàng -->
        <div class="col-12 col-sm-6 col-xl-3">
          <div
            class="kpi-card card border-0 h-100 kpi-clickable"
            @click="$emit('navigate', 'customers')"
          >
            <div class="card-body p-3 d-flex flex-column justify-content-between">
              <div>
                <div class="d-flex align-items-center justify-content-between mb-1.5">
                  <span class="text-secondary small fw-medium">{{ t('admin.dashboard.totalCustomers') }}</span>
                  <div class="kpi-icon icon-purple rounded-2 d-flex align-items-center justify-content-center">
                    <Users :size="16" />
                  </div>
                </div>

                <div class="kpi-val fw-black text-heading mb-1">
                  {{ totalCustomers }} <span class="fs-6 fw-normal text-secondary">thành viên</span>
                </div>

                <div class="mb-2">
                  <div class="d-flex justify-content-between text-secondary mb-1" style="font-size:0.72rem;">
                    <span>Đã thanh toán</span>
                    <span class="fw-bold text-purple">{{ Math.round(paymentRate) }}%</span>
                  </div>
                  <div class="progress" style="height:4px; background:var(--bg-card-inset);">
                    <div
                      class="progress-bar rounded-pill"
                      style="background:linear-gradient(90deg, #a855f7, #ec4899);"
                      :style="{ width: paymentRate + '%' }"
                    ></div>
                  </div>
                </div>
              </div>

              <div class="pt-2 border-top d-flex justify-content-between small text-secondary" style="font-size:0.75rem;">
                <span>Quản lý khách</span>
                <span class="text-purple fw-semibold d-inline-flex align-items-center gap-0.5">
                  Xem ngay <ChevronRight :size="12" />
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- KPI 4: Sản phẩm & Tồn kho -->
        <div class="col-12 col-sm-6 col-xl-3">
          <div
            class="kpi-card card border-0 h-100 kpi-clickable"
            @click="$emit('navigate', 'inventory')"
          >
            <div class="card-body p-3 d-flex flex-column justify-content-between">
              <div>
                <div class="d-flex align-items-center justify-content-between mb-1.5">
                  <span class="text-secondary small fw-medium">{{ t('admin.dashboard.totalProducts') }}</span>
                  <div class="kpi-icon icon-rose rounded-2 d-flex align-items-center justify-content-center">
                    <Boxes :size="16" />
                  </div>
                </div>

                <div class="kpi-val fw-black text-heading mb-1">
                  {{ totalProducts }} <span class="fs-6 fw-normal text-secondary">mẫu</span>
                </div>

                <div class="mb-2 d-flex align-items-center">
                  <span
                    v-if="lowStockItems.length > 0"
                    class="badge rounded-pill bg-danger-subtle text-danger fw-bold d-inline-flex align-items-center gap-1"
                    style="font-size:0.72rem; padding:2px 8px;"
                  >
                    <AlertTriangle :size="11" /> {{ lowStockItems.length }} sắp hết hàng
                  </span>
                  <span
                    v-else
                    class="badge rounded-pill bg-success-subtle text-success fw-bold d-inline-flex align-items-center gap-1"
                    style="font-size:0.72rem; padding:2px 8px;"
                  >
                    <CheckCircle2 :size="11" /> Kho an toàn ({{ Math.round(stockHealthRate) }}%)
                  </span>
                </div>
              </div>

              <div class="pt-2 border-top d-flex justify-content-between small text-secondary" style="font-size:0.75rem;">
                <span>Kinh doanh: {{ Math.round(activeProductRatio) }}%</span>
                <span class="text-rose fw-semibold d-inline-flex align-items-center gap-0.5">
                  Kho hàng <ChevronRight :size="12" />
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Cảnh báo tồn kho gọn nhẹ (chỉ hiện khi có mặt hàng chạm ngưỡng) -->
      <div
        v-if="lowStockItems.length"
        class="compact-alert alert border-0 py-2 px-3 mb-3 rounded-3 d-flex align-items-center justify-content-between flex-wrap gap-2"
      >
        <div class="d-flex align-items-center gap-2">
          <AlertTriangle :size="15" class="text-danger flex-shrink-0" />
          <span class="small">
            Có <strong>{{ lowStockItems.length }} mặt hàng</strong> sắp hết hoặc hết hàng trong kho.
          </span>
        </div>
        <button
          type="button"
          class="btn btn-sm btn-danger py-0.5 px-2.5 rounded-pill fw-semibold"
          style="font-size:0.75rem;"
          @click="$emit('navigate', 'inventory')"
        >
          Xử lý ngay
        </button>
      </div>

      <!-- ══════════════════════════════════════════════════════════════════════════
           3. KHỐI TÀI CHÍNH: XU HƯỚNG THÁNG (8 CỘT) + DOANH THU TUẦN (4 CỘT)
           ══════════════════════════════════════════════════════════════════════════ -->
      <div class="row g-2.5 mb-3">
        <!-- Xu hướng doanh thu theo tháng -->
        <div class="col-12 col-xl-8">
          <div class="dash-card card border-0 h-100">
            <div class="card-body p-3">
              <div class="d-flex align-items-center justify-content-between mb-2 flex-wrap gap-2">
                <div class="d-flex align-items-center gap-1.5">
                  <TrendingUp :size="15" class="text-rose" />
                  <span class="fw-bold small text-heading">Xu hướng doanh thu theo tháng</span>
                </div>
                <span class="badge rounded-pill bg-body-tertiary text-body border" style="font-size:0.72rem;">
                  Năm nay: {{ formatPrice(revenueThisYear) }}
                </span>
              </div>

              <TrendChart
                :data="revenueTrendChart"
                :height="170"
                color="#f43f5e"
                :empty-text="t('admin.dashboard.chartEmptyOrders')"
              />
            </div>
          </div>
        </div>

        <!-- Doanh thu 7 ngày trong tuần -->
        <div class="col-12 col-xl-4">
          <div class="dash-card card border-0 h-100">
            <div class="card-body p-3">
              <div class="d-flex align-items-center justify-content-between mb-2 flex-wrap gap-2">
                <div class="d-flex align-items-center gap-1.5">
                  <Calendar :size="15" class="text-purple" />
                  <span class="fw-bold small text-heading">Doanh thu trong tuần</span>
                </div>
                <span class="badge rounded-pill bg-body-tertiary text-body border" style="font-size:0.72rem;">
                  Tổng: {{ formatPrice(weeklyTotalRevenue) }}
                </span>
              </div>

              <BarChart
                :data="weeklyRevenueBarData"
                vertical
                :empty-text="t('admin.dashboard.chartEmptyOrders')"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- ══════════════════════════════════════════════════════════════════════════
           4. KHỐI VẬN HÀNH & SẢN PHẨM: TRẠNG THÁI ĐƠN (TAB) + HIỆU SUẤT SP (TAB)
           ══════════════════════════════════════════════════════════════════════════ -->
      <div class="row g-2.5 mb-3">
        <!-- Widget 1: Trạng thái đơn hàng (Tích hợp Tab Ngày / Tuần gọn gàng) -->
        <div class="col-12 col-xl-5">
          <div class="dash-card card border-0 h-100">
            <div class="card-body p-3">
              <!-- Header với Tab Ngày / Tuần -->
              <div class="d-flex align-items-center justify-content-between mb-2.5 flex-wrap gap-2">
                <div class="d-flex align-items-center gap-1.5">
                  <PieChart :size="15" class="text-indigo" />
                  <span class="fw-bold small text-heading">Trạng thái đơn hàng</span>
                </div>

                <!-- Segmented Control Button Group -->
                <div class="btn-group btn-group-sm p-0.5 rounded-pill tab-segmented">
                  <button
                    type="button"
                    class="btn rounded-pill py-0.5 px-2"
                    :class="orderStatusMode === 'day' ? 'btn-primary' : 'btn-ghost'"
                    style="font-size:0.72rem;"
                    @click="orderStatusMode = 'day'"
                  >
                    Hôm nay
                  </button>
                  <button
                    type="button"
                    class="btn rounded-pill py-0.5 px-2"
                    :class="orderStatusMode === 'week' ? 'btn-primary' : 'btn-ghost'"
                    style="font-size:0.72rem;"
                    @click="orderStatusMode = 'week'"
                  >
                    Tuần này
                  </button>
                </div>
              </div>

              <!-- Bộ chọn ngày nếu ở chế độ Day -->
              <div v-if="orderStatusMode === 'day'" class="mb-2 d-flex align-items-center justify-content-between gap-2 flex-wrap">
                <span class="badge rounded-pill bg-body-tertiary text-body border" style="font-size:0.72rem;">
                  {{ isStatusChartToday
                    ? `Hôm nay: ${ordersOnStatusChartDate.length} đơn`
                    : `Ngày chọn: ${ordersOnStatusChartDate.length} đơn` }}
                </span>
                <div class="d-flex align-items-center gap-1">
                  <input
                    type="date"
                    :value="statusChartDate"
                    :max="toDateInputValue(new Date())"
                    class="form-control form-control-sm dash-input-sm"
                    @input="$emit('update:statusChartDate', $event.target.value)"
                  />
                  <button
                    v-if="!isStatusChartToday"
                    type="button"
                    class="btn btn-sm btn-outline-secondary py-0.5 px-2 rounded-pill"
                    style="font-size:0.7rem;"
                    @click="$emit('backToToday')"
                  >
                    Về hôm nay
                  </button>
                </div>
              </div>

              <!-- Bộ chọn tuần nếu ở chế độ Week -->
              <div v-else class="mb-2 d-flex align-items-center justify-content-between gap-2 flex-wrap">
                <span class="badge rounded-pill bg-body-tertiary text-body border" style="font-size:0.72rem;">
                  {{ weekChartRangeLabel }} ({{ ordersInWeekRange.length }} đơn)
                </span>
                <div class="d-flex align-items-center gap-1">
                  <input
                    type="date"
                    :value="weekChartAnchor"
                    :max="toDateInputValue(new Date())"
                    class="form-control form-control-sm dash-input-sm"
                    @input="$emit('update:weekChartAnchor', $event.target.value)"
                  />
                  <button
                    v-if="!isWeekChartCurrentWeek"
                    type="button"
                    class="btn btn-sm btn-outline-secondary py-0.5 px-2 rounded-pill"
                    style="font-size:0.7rem;"
                    @click="$emit('resetToCurrentWeek')"
                  >
                    Về tuần này
                  </button>
                </div>
              </div>

              <!-- Donut Chart -->
              <div class="d-flex justify-content-center py-1">
                <DonutChart
                  v-if="orderStatusMode === 'day'"
                  :data="orderStatusChartData"
                  :size="135"
                  :thickness="18"
                  :center-value="String(ordersOnStatusChartDate.length)"
                  :center-label="t('admin.dashboard.totalOrders')"
                  :empty-text="t('admin.dashboard.chartEmptyOrders')"
                />
                <DonutChart
                  v-else
                  :data="weekOrderStatusChartData"
                  :size="135"
                  :thickness="18"
                  :center-value="String(ordersInWeekRange.length)"
                  :center-label="t('admin.dashboard.totalOrders')"
                  :empty-text="t('admin.dashboard.chartEmptyOrders')"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- Widget 2: Hiệu suất sản phẩm (Tích hợp Tab Bán chạy / Bán chậm) -->
        <div class="col-12 col-xl-7">
          <div class="dash-card card border-0 h-100">
            <div class="card-body p-3">
              <!-- Header với Tab Bán chạy / Bán chậm -->
              <div class="d-flex align-items-center justify-content-between mb-2.5 flex-wrap gap-2">
                <div class="d-flex align-items-center gap-1.5">
                  <component :is="productSalesTab === 'top' ? Flame : Turtle" :size="15" :class="productSalesTab === 'top' ? 'text-warning' : 'text-danger'" />
                  <span class="fw-bold small text-heading">
                    {{ productSalesTab === 'top' ? 'Top 5 sản phẩm bán chạy' : '5 sản phẩm cần đẩy mạnh bán' }}
                  </span>
                </div>

                <!-- Segmented Control Button Group -->
                <div class="btn-group btn-group-sm p-0.5 rounded-pill tab-segmented">
                  <button
                    type="button"
                    class="btn rounded-pill py-0.5 px-2"
                    :class="productSalesTab === 'top' ? 'btn-primary' : 'btn-ghost'"
                    style="font-size:0.72rem;"
                    @click="productSalesTab = 'top'"
                  >
                    Bán chạy
                  </button>
                  <button
                    type="button"
                    class="btn rounded-pill py-0.5 px-2"
                    :class="productSalesTab === 'slow' ? 'btn-primary' : 'btn-ghost'"
                    style="font-size:0.72rem;"
                    @click="productSalesTab = 'slow'"
                  >
                    Bán chậm
                  </button>
                </div>
              </div>

              <!-- BarChart ngang -->
              <BarChart
                v-if="productSalesTab === 'top'"
                :data="topSellingChart"
                :show-rank="true"
                :empty-text="t('admin.dashboard.chartEmptyOrders')"
              />
              <BarChart
                v-else
                :data="slowSellingChart"
                :show-rank="true"
                :empty-text="t('admin.dashboard.chartEmptyProducts')"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- ══════════════════════════════════════════════════════════════════════════
           5. SẢN PHẨM MỚI CẬP NHẬT GẦN ĐÂY (BẢNG GỌN)
           ══════════════════════════════════════════════════════════════════════════ -->
      <div class="dash-card card border-0">
        <div class="card-body p-3">
          <div class="d-flex align-items-center justify-content-between mb-2.5 flex-wrap gap-2">
            <div class="d-flex align-items-center gap-1.5">
              <Archive :size="15" class="text-indigo" />
              <span class="fw-bold small text-heading">Sản phẩm mới cập nhật gần đây</span>
            </div>

            <button
              type="button"
              class="btn btn-sm btn-outline-secondary py-0.5 px-2.5 rounded-pill d-flex align-items-center gap-1"
              style="font-size:0.75rem;"
              @click="$emit('navigate', 'products')"
            >
              <span>Xem danh mục</span>
              <ChevronRight :size="12" />
            </button>
          </div>

          <div class="table-responsive rounded-2 border">
            <table class="table table-hover table-sm align-middle mb-0 custom-table-compact">
              <thead>
                <tr>
                  <th style="width:44px;"></th>
                  <th><Monitor :size="12" class="me-1" style="vertical-align:-1px;" /> {{ t('admin.dashboard.colName') }}</th>
                  <th><Tag :size="12" class="me-1" style="vertical-align:-1px;" /> {{ t('admin.dashboard.colBrand') }}</th>
                  <th><FolderOpen :size="12" class="me-1" style="vertical-align:-1px;" /> {{ t('admin.dashboard.colCategory') }}</th>
                  <th><Banknote :size="12" class="me-1" style="vertical-align:-1px;" /> {{ t('admin.dashboard.colPrice') }}</th>
                  <th><Bookmark :size="12" class="me-1" style="vertical-align:-1px;" /> {{ t('admin.dashboard.colStatus') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="p in products.slice(0, 5)" :key="p.sanPhamId">
                  <td>
                    <div
                      class="prod-thumb rounded-1 d-flex align-items-center justify-content-center overflow-hidden"
                    >
                      <img
                        v-if="p.hinhAnhChinh"
                        :src="p.hinhAnhChinh"
                        :alt="p.tenSanPham"
                        style="width:100%; height:100%; object-fit:contain; padding:1px;"
                      />
                      <span v-else><Laptop :size="14" color="var(--text-muted)" /></span>
                    </div>
                  </td>
                  <td class="fw-semibold text-heading">{{ p.tenSanPham }}</td>
                  <td>
                    <span class="badge rounded-pill bg-body-secondary text-body fw-normal px-2 py-0.5" style="font-size:0.75rem;">
                      {{ p.tenThuongHieu || '—' }}
                    </span>
                  </td>
                  <td>
                    <span class="badge rounded-pill bg-body-secondary text-body fw-normal px-2 py-0.5" style="font-size:0.75rem;">
                      {{ p.tenDanhMuc || '—' }}
                    </span>
                  </td>
                  <td class="fw-bold text-heading">{{ formatPrice(p.giaBan) }}</td>
                  <td>
                    <span
                      class="status-dot-badge d-inline-flex align-items-center gap-1 px-2 py-0.5 rounded-pill fw-semibold"
                      :class="p.trangThai === 'active' ? 'st-active' : 'st-inactive'"
                      style="font-size:0.72rem;"
                    >
                      <span class="dot"></span>
                      {{ statusLabel(p.trangThai) }}
                    </span>
                  </td>
                </tr>
                <tr v-if="products.length === 0">
                  <td colspan="6" class="text-center py-3 text-secondary small">
                    {{ t('admin.dashboard.emptyProducts') }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.dashboard-compact {
  animation: fadeIn 0.25s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ─── Top Header Chips ─── */
.status-chip {
  background: rgba(34, 197, 94, 0.1);
  color: #22c55e;
  border: 1px solid rgba(34, 197, 94, 0.2);
}
.pulse-dot {
  width: 6px;
  height: 6px;
  background: #22c55e;
  border-radius: 50%;
  animation: pulseDot 2s infinite;
}
@keyframes pulseDot {
  0% { box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.7); }
  70% { box-shadow: 0 0 0 6px rgba(34, 197, 94, 0); }
  100% { box-shadow: 0 0 0 0 rgba(34, 197, 94, 0); }
}
.date-chip {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
}

/* ─── Cards ─── */
.dash-card,
.kpi-card {
  background: var(--bg-card) !important;
  border: 1px solid var(--border-color) !important;
  border-radius: 12px !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.2s ease;
}
.kpi-clickable {
  cursor: pointer;
}
.kpi-clickable:hover {
  transform: translateY(-2px);
  border-color: var(--accent-2, #7c3aed) !important;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
}

.kpi-val {
  font-size: 1.45rem;
  letter-spacing: -0.02em;
  line-height: 1.15;
}
.kpi-icon {
  width: 32px;
  height: 32px;
}
.icon-emerald { background: rgba(16, 185, 129, 0.12); color: #10b981; }
.icon-indigo  { background: rgba(99, 102, 241, 0.12); color: #6366f1; }
.icon-purple  { background: rgba(168, 85, 247, 0.12); color: #a855f7; }
.icon-rose    { background: rgba(244, 63, 94, 0.12); color: #f43f5e; }

.text-indigo { color: #6366f1 !important; }
.text-purple { color: #a855f7 !important; }
.text-rose   { color: #f43f5e !important; }

/* ─── Segmented Tabs ─── */
.tab-segmented {
  background: var(--bg-card-inset);
  border: 1px solid var(--border-color);
}
.btn-ghost {
  background: transparent;
  color: var(--text-secondary);
  border: none;
}
.btn-ghost:hover {
  color: var(--text-heading);
}

/* ─── Compact Alert ─── */
.compact-alert {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.08), rgba(245, 158, 11, 0.06));
  border: 1px solid rgba(239, 68, 68, 0.2) !important;
}

/* ─── Inputs ─── */
.dash-input-sm {
  background: var(--bg-card-inset) !important;
  color: var(--text-primary) !important;
  border-color: var(--border-color) !important;
  font-size: 0.72rem;
  padding: 2px 6px;
  border-radius: 6px;
  width: auto;
}

/* ─── Compact Table ─── */
.custom-table-compact {
  --bs-table-bg: var(--bg-card);
  --bs-table-color: var(--text-primary);
  --bs-table-hover-bg: var(--bg-hover);
  --bs-table-hover-color: var(--text-primary);
  --bs-table-border-color: var(--border-color-soft);
}
.custom-table-compact th {
  background: var(--bg-card-inset);
  color: var(--text-secondary);
  font-size: 0.72rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  padding: 6px 10px;
}
.custom-table-compact td {
  padding: 6px 10px;
  font-size: 0.8rem;
}
.prod-thumb {
  width: 32px;
  height: 28px;
  background: var(--bg-card-inset);
  border: 1px solid var(--border-color-soft);
}
.status-dot-badge {
  font-size: 0.72rem;
}
.st-active {
  background: rgba(34, 197, 94, 0.12);
  color: #22c55e;
}
.st-inactive {
  background: rgba(100, 116, 139, 0.12);
  color: #64748b;
}
.status-dot-badge .dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
}
</style>
