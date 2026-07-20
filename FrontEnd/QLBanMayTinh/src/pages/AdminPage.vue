<script setup>
import { ref, computed, onMounted, onUnmounted, reactive, watch } from "vue";
import { AuthStore } from "../stores/index.js";
import { t, I18nStore, LOCALES, setLocale } from "../i18n/index.js";
import { orderStatusLabel, orderStatusColor, orderStatusIcon } from "../utils/orderStatus.js";
import * as NhanVienService  from "../Service/NhanVienService.js";
import * as DonHangService   from "../Service/DonHangService.js";
import * as KhuyenMaiService from "../Service/KhuyenMaiService.js";
import * as DmService              from "../Service/DmService.js";
import { ChiTietCpuService, ChiTietRamService, ChiTietGpuService, ChiTietOCungService } from "../Service/ChiTietLinhKienService.js";
import * as DashboardService       from "../Service/DashboardService.js";
import DonutChart from "../components/common/DonutChart.vue";
import RevenueBarChart from "../components/common/RevenueBarChart.vue";
import * as CaiDatService from "../Service/CaiDatService.js";
import { SettingsStore } from "../stores/settings.js";
import BarChart   from "../components/common/BarChart.vue";
import GaugeChart from "../components/common/GaugeChart.vue";
import TrendChart from "../components/common/TrendChart.vue";
import ConfirmDialog from "../components/common/ConfirmDialog.vue";
import { askConfirm } from "../stores/confirm.js";
import { ThemeStore, toggleTheme } from "../stores/theme.js";
import { authHeaders } from "../Service/api.js";
import { formatPrice, formatDate, formatDateTime, statusLabel, toLocalDT } from "../utils/adminFormat.js";
import { showToast } from "../stores/toast.js";
import ToastHost from "../components/common/ToastHost.vue";
import ProductsTable from "../components/admin/ProductsTable.vue";
import CustomersTable from "../components/admin/CustomersTable.vue";
import OrdersTable from "../components/admin/OrdersTable.vue";
import PosPanel from "../components/admin/PosPanel.vue";
import InventoryPanel from "../components/admin/InventoryPanel.vue";
import ReturnsPanel from "../components/admin/ReturnsPanel.vue";
import WarrantyPanel from "../components/admin/WarrantyPanel.vue";
import DmCategoryTable from "../components/admin/DmCategoryTable.vue";
import SerialManager from "../components/admin/SerialManager.vue";
import UserProfileMenu from "../components/admin/UserProfileMenu.vue";
import { ProductsStore, ensureProducts, refreshProducts } from "../stores/products.js";
import { OrdersStore, ensureOrders, refreshOrders, connectOrderEvents, disconnectOrderEvents } from "../stores/orders.js";
import { CustomersStore, ensureCustomers, refreshCustomers } from "../stores/customers.js";
import { InventoryStore, ensureInventory, refreshInventory } from "../stores/inventory.js";
import { StaffStore, ensureStaff, refreshStaff } from "../stores/staff.js";
import { PromotionsStore, ensurePromotions, refreshPromotions } from "../stores/promotions.js";

// ── Navigation ───────────────────────────────────────────────────────────────
const currentPage = ref("dashboard");
const navigate = (page) => {
  currentPage.value = page;
  if (page === "staff") { ensureChucVuList(); ensureStaff(); }
};
// icon khớp đúng ý nghĩa icon SVG tương ứng ở sidebar (adm-icon) — hiện lại 1 lần nữa
// cạnh tiêu đề trang cho dễ nhận biết đang ở đâu, không cần đổi cả 2 nơi khi thêm trang mới.
const PAGE_META = {
  dashboard: { titleKey: "admin.pageMeta.dashboard.title", subKey: "admin.pageMeta.dashboard.sub", icon: "📊" },
  products: { titleKey: "admin.pageMeta.products.title", subKey: "admin.pageMeta.products.sub", icon: "💻" },
  orders: { titleKey: "admin.pageMeta.orders.title", subKey: "admin.pageMeta.orders.sub", icon: "🧾" },
  customers: { titleKey: "admin.pageMeta.customers.title", subKey: "admin.pageMeta.customers.sub", icon: "👥" },
  inventory: { titleKey: "admin.pageMeta.inventory.title", subKey: "admin.pageMeta.inventory.sub", icon: "📦" },
  "tra-hang": { titleKey: "admin.pageMeta.traHang.title", subKey: "admin.pageMeta.traHang.sub", icon: "↩️" },
  promotions: { titleKey: "admin.pageMeta.promotions.title", subKey: "admin.pageMeta.promotions.sub", icon: "🏷️" },
  staff: { titleKey: "admin.pageMeta.staff.title", subKey: "admin.pageMeta.staff.sub", icon: "🧑‍💼" },
  "ban-hang": { titleKey: "admin.pageMeta.banHang.title", subKey: "admin.pageMeta.banHang.sub", icon: "🛒" },
  reports: { titleKey: "admin.pageMeta.reports.title", subKey: "admin.pageMeta.reports.sub", icon: "📈" },
  settings: { titleKey: "admin.pageMeta.settings.title", subKey: "admin.pageMeta.settings.sub", icon: "⚙️" },
};
const topbarTitle = computed(
  () => t(PAGE_META[currentPage.value]?.titleKey ?? "admin.pageMeta.dashboard.title"),
);
const topbarSub = computed(() => t(PAGE_META[currentPage.value]?.subKey ?? ""));
const topbarIcon = computed(() => PAGE_META[currentPage.value]?.icon ?? "📊");

// ── Data refs ─────────────────────────────────────────────────────────────────
// products/orders/customers/staff/promotions/inventory/suppliers giờ sống trong stores/*.js
// dùng chung nhiều trang — computed alias bên dưới giữ nguyên tên biến cũ để phần còn lại
// của file (200+ chỗ đọc products.value/orders.value/...) không cần sửa. computed = read-only,
// mọi chỗ CRUD phải gọi refreshXxx()/ensureXxx() của store thay vì gán tay vào các biến này.
const products = computed(() => ProductsStore.items);
const orders = computed(() => OrdersStore.items);
const customers = computed(() => CustomersStore.items);
const staff = computed(() => StaffStore.items);
const promotions = computed(() => PromotionsStore.items);
const inventory = computed(() => InventoryStore.items);
const chucVuList = ref([]);

// ── Helpers ───────────────────────────────────────────────────────────────────
// customerName()/orderSearch/filteredOrders/... (bo loc + lich su Don hang) da chuyen
// vao components/admin/OrdersTable.vue (Task 5).
const chucVuName = (id) =>
  chucVuList.value.find((c) => c.id === id)?.tenChucVu ?? "—";

// ── Dashboard stats ───────────────────────────────────────────────────────────
// Dem so san pham PHAN BIET (theo sanPhamId), khong phai so dong bien the tho —
// products.value co 1 dong/bien the nen dem thang se ra con so sai (vd 42 thay vi 12 san pham that)
const totalProducts = computed(() => groupedProducts.value.length);
const totalOrders = computed(() => orders.value.length);
// Badge sidebar "Đơn hàng" chỉ đếm đơn hôm nay (khác totalOrders — vẫn dùng cho KPI Dashboard).
const todayOrdersCount = computed(
  () => orders.value.filter((o) => o.ngayDat?.slice(0, 10) === toDateInputValue(new Date())).length,
);
const totalCustomers = computed(() => customers.value.length);
const totalRevenue = computed(() =>
  orders.value.reduce((s, o) => s + (Number(o.thanhTien) || 0), 0),
);
// Doanh thu tháng này / cả năm — cho thẻ KPI ở tab Dashboard (khác thẻ "Tổng doanh thu"
// lũy kế toàn thời gian ở tab Báo cáo, không đổi).
const revenueThisMonth = computed(() => {
  const now = new Date();
  return orders.value.reduce((s, o) => {
    if (!o.ngayDat) return s;
    const d = new Date(o.ngayDat);
    const sameMonth = d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth();
    return sameMonth ? s + (Number(o.thanhTien) || 0) : s;
  }, 0);
});
const revenueThisYear = computed(() => {
  const now = new Date();
  return orders.value.reduce((s, o) => {
    if (!o.ngayDat) return s;
    const sameYear = new Date(o.ngayDat).getFullYear() === now.getFullYear();
    return sameYear ? s + (Number(o.thanhTien) || 0) : s;
  }, 0);
});
// So sánh doanh thu tháng này với tháng trước — lấy cùng số ngày đã trôi qua ở cả 2 tháng
// (month-to-date vs month-to-date), KHÔNG so cả tháng trước (đủ ngày) với tháng này (mới
// trôi qua một phần), vì như vậy tháng nào cũng bị báo "giảm" một cách giả tạo dù doanh thu
// theo ngày không đổi.
const revenueThisMonthDelta = computed(() => {
  const now = new Date();
  const cutoffDay = now.getDate();
  const prevMonth = now.getMonth() - 1, prevYear = now.getMonth() === 0 ? now.getFullYear() - 1 : now.getFullYear();
  const prevMonthDayCount = new Date(prevYear, (prevMonth + 12) % 12 + 1, 0).getDate();
  const prevCutoffDay = Math.min(cutoffDay, prevMonthDayCount);

  const prevMtd = orders.value.reduce((s, o) => {
    if (!o.ngayDat) return s;
    const d = new Date(o.ngayDat);
    const sameMonth = d.getFullYear() === prevYear && d.getMonth() === (prevMonth + 12) % 12;
    return sameMonth && d.getDate() <= prevCutoffDay ? s + (Number(o.thanhTien) || 0) : s;
  }, 0);

  if (prevMtd === 0) return null;
  return Math.round(((revenueThisMonth.value - prevMtd) / prevMtd) * 100);
});

// ── Reports stats ─────────────────────────────────────────────────────────────
const ordersByStatus = computed(() => {
  const map = {};
  orders.value.forEach((o) => {
    map[o.trangThaiDonHang] = (map[o.trangThaiDonHang] || 0) + 1;
  });
  return Object.entries(map).map(([k, v]) => ({ status: k, count: v }));
});

// ── Dữ liệu cho biểu đồ Dashboard ──────────────────────────────────────────────
// Đơn hàng theo ngày được chọn — donut "Đơn hàng theo trạng thái" chỉ tính đơn của 1 ngày,
// mặc định là hôm nay nhưng admin có thể chọn lại ngày khác (kể cả hôm trước) để xem.
const toDateInputValue = (d) => {
  const y = d.getFullYear(), m = String(d.getMonth() + 1).padStart(2, '0'), day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
};
const statusChartDate = ref(toDateInputValue(new Date()));
const isStatusChartToday = computed(() => statusChartDate.value === toDateInputValue(new Date()));
const ordersOnStatusChartDate = computed(() => {
  return orders.value.filter((o) => {
    if (!o.ngayDat) return false;
    return toDateInputValue(new Date(o.ngayDat)) === statusChartDate.value;
  });
});
const ordersByStatusOnDate = computed(() => {
  const map = {};
  ordersOnStatusChartDate.value.forEach((o) => {
    map[o.trangThaiDonHang] = (map[o.trangThaiDonHang] || 0) + 1;
  });
  return Object.entries(map).map(([k, v]) => ({ status: k, count: v }));
});
// Đơn hàng theo trạng thái (donut) — dùng lại đúng bảng màu trạng thái đã chốt
const orderStatusChartData = computed(() =>
  ordersByStatusOnDate.value.map((row) => ({
    label: orderStatusLabel(row.status),
    value: row.count,
    color: orderStatusColor(row.status).text,
  }))
);

// ── Đơn hàng theo tuần (donut) — chọn khoảng ngày, mặc định tuần hiện tại (Thứ
// hai → Chủ nhật). Đơn có thể giao trễ tối đa khoảng 1 tuần mới tới tay khách,
// nên xem gộp theo tuần dễ theo dõi tiến độ hơn xem từng ngày riêng lẻ.
const startOfWeek = (d) => {
  const date = new Date(d);
  const day = date.getDay(); // 0 = Chủ nhật
  date.setDate(date.getDate() + (day === 0 ? -6 : 1 - day)); // lùi về Thứ hai
  return date;
};
const endOfWeek = (d) => {
  const e = startOfWeek(d);
  e.setDate(e.getDate() + 6);
  return e;
};
// Chỉ chọn 1 ngày mốc — khoảng tuần (Thứ hai → Chủ nhật) luôn tự tính từ ngày đó,
// không cho chọn khoảng tuỳ ý (tránh lọc lẫn nhiều tuần hoặc khoảng lệch tuần).
const weekChartAnchor = ref(toDateInputValue(new Date()));
const weekChartFrom = computed(() => toDateInputValue(startOfWeek(new Date(weekChartAnchor.value))));
const weekChartTo   = computed(() => toDateInputValue(endOfWeek(new Date(weekChartAnchor.value))));
const isWeekChartCurrentWeek = computed(() => weekChartAnchor.value === toDateInputValue(new Date()));
const resetToCurrentWeek = () => { weekChartAnchor.value = toDateInputValue(new Date()); };
// Hiển thị khoảng tuần đã khoanh, vd "29/06 → 05/07" — vì input chỉ còn 1 ngày mốc
const weekChartRangeLabel = computed(() => {
  const fmt = (s) => { const [, m, d] = s.split('-'); return `${d}/${m}`; };
  return `${fmt(weekChartFrom.value)} → ${fmt(weekChartTo.value)}`;
});
const ordersInWeekRange = computed(() => {
  if (!weekChartFrom.value || !weekChartTo.value) return [];
  return orders.value.filter((o) => {
    if (!o.ngayDat) return false;
    const d = toDateInputValue(new Date(o.ngayDat));
    return d >= weekChartFrom.value && d <= weekChartTo.value;
  });
});
const ordersByStatusInWeek = computed(() => {
  const map = {};
  ordersInWeekRange.value.forEach((o) => {
    map[o.trangThaiDonHang] = (map[o.trangThaiDonHang] || 0) + 1;
  });
  return Object.entries(map).map(([k, v]) => ({ status: k, count: v }));
});
const weekOrderStatusChartData = computed(() =>
  ordersByStatusInWeek.value.map((row) => ({
    label: orderStatusLabel(row.status),
    value: row.count,
    color: orderStatusColor(row.status).text,
  }))
);

// ── Báo cáo: bộ lọc khoảng thời gian (Hôm nay/Tuần này/Tháng này/Tùy chọn) ────────────
// Dùng lại đúng quy ước ngày-tháng đã có ở tab Dashboard (toDateInputValue, so sánh
// string 'YYYY-MM-DD') — xem weekChartFrom/weekChartTo cùng file để đối chiếu.
const reportsDateRange = ref('week'); // 'today' | 'week' | 'month' | 'custom'
const reportsCustomFrom = ref(toDateInputValue(new Date()));
const reportsCustomTo   = ref(toDateInputValue(new Date()));

const reportsDateFrom = computed(() => {
  const now = new Date();
  if (reportsDateRange.value === 'today') return toDateInputValue(now);
  if (reportsDateRange.value === 'week') return toDateInputValue(startOfWeek(now));
  if (reportsDateRange.value === 'month') return toDateInputValue(new Date(now.getFullYear(), now.getMonth(), 1));
  return reportsCustomFrom.value;
});
const reportsDateTo = computed(() => {
  const now = new Date();
  if (reportsDateRange.value === 'today') return toDateInputValue(now);
  if (reportsDateRange.value === 'week') return toDateInputValue(endOfWeek(now));
  if (reportsDateRange.value === 'month') return toDateInputValue(new Date(now.getFullYear(), now.getMonth() + 1, 0));
  return reportsCustomTo.value;
});

// Top sản phẩm bán chạy trong khoảng đã chọn — tải lại mỗi khi khoảng đổi.
const reportsTopSelling = ref([]); // [{ tenSanPham, soLuongDaBan }]
const loadReportsTopSelling = async () => {
  reportsTopSelling.value = await DashboardService
    .getTopSelling(5, reportsDateFrom.value, reportsDateTo.value)
    .catch(() => []);
};
watch([reportsDateFrom, reportsDateTo], loadReportsTopSelling, { immediate: true });

// Doanh thu theo ngày trong khoảng đã chọn — cho biểu đồ cột. API chỉ trả về những
// ngày có đơn (SUM/GROUP BY ở SQL), nên cần điền thêm các ngày không có đơn = 0đ —
// nếu không, khoảng nhiều ngày mà chỉ 1-2 ngày có doanh thu sẽ vẽ ra 1 cột chiếm hết
// cả biểu đồ thay vì dải cột theo từng ngày.
const reportsRevenueByDay = ref([]); // [{ ngay, doanhThu }], liên tục từng ngày trong khoảng
const loadReportsRevenueByDay = async () => {
  const raw = await DashboardService
    .getRevenueByDay(reportsDateFrom.value, reportsDateTo.value)
    .catch(() => []);
  const byDay = Object.fromEntries(raw.map((r) => [r.ngay, Number(r.doanhThu) || 0]));
  const days = [];
  const cur = new Date(reportsDateFrom.value);
  const end = new Date(reportsDateTo.value);
  while (cur <= end) {
    const key = toDateInputValue(cur);
    days.push({ ngay: key, doanhThu: byDay[key] || 0 });
    cur.setDate(cur.getDate() + 1);
  }
  reportsRevenueByDay.value = days;
};
watch([reportsDateFrom, reportsDateTo], loadReportsRevenueByDay, { immediate: true });

// Gộp theo Ngày/Tháng/Năm cho biểu đồ doanh thu — Tháng/Năm bỏ qua bộ lọc khoảng ngày ở
// trên (xem xu hướng nhiều tháng/năm không hợp với khoảng "Hôm nay"/"Tuần này") và gộp
// trực tiếp từ `orders` đã tải sẵn, giống cách revenueTrendChart ở tab Dashboard đang làm.
const reportsGroupBy = ref('day'); // 'day' | 'month' | 'year'
const reportsRevenueByMonth = computed(() => {
  const map = {};
  orders.value.forEach((o) => {
    if (!o.ngayDat) return;
    const d = new Date(o.ngayDat);
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`;
    map[key] = (map[key] || 0) + (Number(o.thanhTien) || 0);
  });
  return Object.entries(map)
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([ngay, doanhThu]) => ({ ngay, doanhThu }));
});
const reportsRevenueByYear = computed(() => {
  const map = {};
  orders.value.forEach((o) => {
    if (!o.ngayDat) return;
    const key = `${new Date(o.ngayDat).getFullYear()}-01-01`;
    map[key] = (map[key] || 0) + (Number(o.thanhTien) || 0);
  });
  return Object.entries(map)
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([ngay, doanhThu]) => ({ ngay, doanhThu }));
});
const reportsRevenueChartData = computed(() => {
  if (reportsGroupBy.value === 'month') return reportsRevenueByMonth.value;
  if (reportsGroupBy.value === 'year') return reportsRevenueByYear.value;
  return reportsRevenueByDay.value;
});

// Khách hàng nổi bật (top chi tiêu + tỷ lệ mua lại) trong khoảng đã chọn.
const reportsCustomerReport = ref({ topKhach: [], tyLeMuaLai: 0, tongSoKhach: 0 });
const loadReportsCustomerReport = async () => {
  reportsCustomerReport.value = await DashboardService
    .getCustomerReport(reportsDateFrom.value, reportsDateTo.value, 5)
    .catch(() => ({ topKhach: [], tyLeMuaLai: 0, tongSoKhach: 0 }));
};
watch([reportsDateFrom, reportsDateTo], loadReportsCustomerReport, { immediate: true });

const reportsRepeatRateText = computed(() => {
  const c = reportsCustomerReport.value;
  const repeat = Math.round(c.tyLeMuaLai * c.tongSoKhach);
  const pct = Math.round(c.tyLeMuaLai * 100);
  return t('admin.reports.repeatRateLabel', { repeat, total: c.tongSoKhach, pct });
});

// Đơn hàng theo trạng thái trong khoảng đã chọn — vẫn tính từ orders đã tải sẵn (đủ
// nhanh, không cần thêm endpoint riêng vì đây chỉ là group-by theo status, không phải
// SUM/COUNT nặng), nhưng nay có lọc theo ngày + dùng đúng nhãn/màu trạng thái đã chốt.
const reportsOrdersByStatus = computed(() => {
  const map = {};
  orders.value.forEach((o) => {
    if (!o.ngayDat) return;
    const d = toDateInputValue(new Date(o.ngayDat));
    if (d < reportsDateFrom.value || d > reportsDateTo.value) return;
    map[o.trangThaiDonHang] = (map[o.trangThaiDonHang] || 0) + 1;
  });
  return Object.entries(map).map(([status, count]) => ({
    status, count,
    label: orderStatusLabel(status),
    color: orderStatusColor(status),
  }));
});

// Top 5 bán chạy/bán chậm cho Dashboard — tính bằng SUM/GROUP BY ở backend (xem
// DashboardController), thay vì tải toàn bộ chi tiết đơn hàng (hàng nghìn dòng) về
// JS để cộng dồn. Chỉ trả về 5+5 dòng mỗi lần thay vì toàn bộ chi_tiet_don_hang.
const topSellingRaw = ref([]); // [{ tenSanPham, soLuongDaBan }]
const slowSellingRaw = ref([]);
const fetchProductSales = async () => {
  [topSellingRaw.value, slowSellingRaw.value] = await Promise.all([
    DashboardService.getTopSelling(5).catch(() => []),
    DashboardService.getSlowSelling(5).catch(() => []),
  ]);
};

const imageByProductName = computed(() => new Map(products.value.map((p) => [p.tenSanPham, p.hinhAnhChinh])));

const topSellingChart = computed(() =>
  topSellingRaw.value.map((r) => ({
    label: r.tenSanPham, value: r.soLuongDaBan, image: imageByProductName.value.get(r.tenSanPham) || '',
    displayValue: t('admin.dashboard.unitsSold', { count: r.soLuongDaBan }), color: '#22c55e',
  }))
);

const slowSellingChart = computed(() =>
  slowSellingRaw.value.map((r) => ({
    label: r.tenSanPham, value: r.soLuongDaBan, image: imageByProductName.value.get(r.tenSanPham) || '',
    displayValue: t('admin.dashboard.unitsSold', { count: r.soLuongDaBan }), color: '#f87171',
  }))
);

// ── Gauge KPI: 3 chỉ số sức khỏe vận hành dạng % ──────────────────────────────
const orderCompletionRate = computed(() => {
  if (!orders.value.length) return 0;
  return (orders.value.filter((o) => o.trangThaiDonHang === 'delivered').length / orders.value.length) * 100;
});
const paymentRate = computed(() => {
  if (!orders.value.length) return 0;
  return (orders.value.filter((o) => o.trangThaiThanhToan === 'paid').length / orders.value.length) * 100;
});
const stockHealthRate = computed(() => {
  if (!inventory.value.length) return 0;
  const unhealthyIds = new Set([
    ...lowStockItems.value.map(t => t.tonKhoId),
    ...outOfStockItems.value.map(t => t.tonKhoId),
  ]);
  return Math.max(0, ((inventory.value.length - unhealthyIds.size) / inventory.value.length) * 100);
});
const gaugeColor = (pct) => (pct >= 70 ? '#22c55e' : pct >= 40 ? '#facc15' : '#f87171');

// ── Doanh thu theo tháng (trend) ───────────────────────────────────────────────
const revenueTrendChart = computed(() => {
  const map = {};
  orders.value.forEach((o) => {
    if (!o.ngayDat) return;
    const d = new Date(o.ngayDat);
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
    map[key] = (map[key] || 0) + (Number(o.thanhTien) || 0);
  });
  return Object.entries(map)
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([key, value]) => {
      const [y, m] = key.split('-');
      return { label: `${m}/${y}`, value };
    });
});

const activeProducts = computed(
  () => products.value.filter((p) => p.trangThai === "active").length,
);
const activePromos = computed(() => {
  const now = new Date();
  return promotions.value.filter(
    (p) => p.trangThai === "active" && new Date(p.ngayKetThuc) > now,
  ).length;
});
const lowStockItems = computed(() =>
  inventory.value.filter(
    (t) => t.soLuongTon != null && t.tonKhoToiThieu != null && t.soLuongTon <= t.tonKhoToiThieu,
  ),
);
const outOfStockItems = computed(() =>
  inventory.value.filter(t => (t.soLuongTon ?? 0) === 0),
);
// lowStockOnlyItems/totalStockQty (chỉ dùng trong tab Tồn kho) đã chuyển vào
// components/admin/InventoryPanel.vue (Task 7), cùng khoTab.

// ── Trang Kho hàng: tab-switcher cấp cao (Kho hàng vs Bảo hành) ──────────────────
// Thay cho khoTab cũ (giờ đã chuyển hẳn vào InventoryPanel.vue) — trang này chỉ còn
// đúng 2 lựa chọn: "kho" (InventoryPanel — gồm Tồn kho + Phiếu nhập) và "bao-hanh".
const inventoryMainTab = ref('kho');
const productsMainTab = ref('sanPham');

// khoTab + toàn bộ state/hàm của tab Tồn kho (inventorySearch, inventoryGrouped,
// getVariantInfo, stockClass...) và tab Phiếu nhập kho (ensurePhieuNhapData,
// phieuNhapList, savePhieuNhap, printPhieuNhapDetail...) đã chuyển vào
// components/admin/InventoryPanel.vue (Task 7).

// ── Fetch ─────────────────────────────────────────────────────────────────────
// Chỉ tải 6 bảng chính lúc vào trang (dashboard + các bảng danh sách cần ngay).
// Danh mục/hãng/CPU/RAM/ổ cứng/GPU (ensureProductRefData) đã chuyển vào
// ProductsTable.vue (Task 3) — chỉ tab Sản phẩm cần. Chức vụ (ensureChucVuList) vẫn
// KHÔNG tải ở đây — chỉ tải khi vào trang Nhân viên, xem ensureChucVuList() bên dưới.
// Với dữ liệu lớn, bớt 7-8 lệnh gọi song song này giúp trang vào nhanh hơn hẳn.
// Nhân viên KHÔNG tải ở đây nữa — không có KPI/dashboard/POS nào cần đến staff.value,
// chỉ tab Nhân viên và tab Phiếu nhập (staffName/staffOptions) cần, cả 2 đều lazy-load
// qua ensureStaff() (stores/staff.js). products/orders/customers/promotions/inventory VẪN
// tải eager vì dashboard KPI + POS (tìm SP, áp mã khuyến mãi, tra cứu KH) cần ngay.
const fetchAll = async () => {
  await Promise.all([
    refreshProducts(),
    refreshOrders(),
    refreshCustomers(),
    refreshPromotions(),
    refreshInventory(),
  ]);
  await autoMergeAllDuplicates();
};

// Chức vụ — cần cho cả bảng Nhân viên (chucVuName) lẫn form thêm/sửa nhân viên.
let chucVuListPromise = null;
const ensureChucVuList = () => {
  if (chucVuListPromise) return chucVuListPromise;
  chucVuListPromise = DmService.getChucVu().catch(() => []).then((list) => {
    chucVuList.value = list;
  });
  return chucVuListPromise;
};

// Tự động gộp tất cả đơn cùng khách + cùng ngày khi tải trang
// Khóa chống chạy chồng — nhiều sự kiện SSE "đơn mới" dồn dập cùng gọi hàm này cùng lúc
// sẽ tranh nhau gộp CÙNG 1 cặp đơn: lượt thắng xóa đơn nguồn, các lượt sau gọi merge cho
// đơn đã bị xóa → lỗi 400 "không tồn tại" lặp lại nhiều lần (đã thấy trong console).
let isMerging = false;
const autoMergeAllDuplicates = async () => {
  if (isMerging) return;
  isMerging = true;
  try {
    const groups = {};
    for (const o of orders.value) {
      const key = `${o.khachHangId}_${o.ngayDat?.slice(0, 10)}`;
      if (!groups[key]) groups[key] = [];
      groups[key].push(o);
    }
    const toMerge = Object.values(groups).filter(g => g.length > 1);
    if (toMerge.length === 0) return;
    for (const group of toMerge) {
      group.sort((a, b) => a.donHangId - b.donHangId);
      const targetId = group[0].donHangId;
      const sourceIds = group.slice(1).map(o => o.donHangId);
      await DonHangService.merge(targetId, sourceIds).catch((e) => console.error('Gộp đơn trùng lỗi:', e));
    }
    await refreshOrders();
  } finally {
    isMerging = false;
  }
};

// ── Products: gộp theo sanPhamId cho bảng ─────────────────────────────────────
const groupedProducts = computed(() => {
  const map = new Map();
  products.value.forEach(p => {
    if (!map.has(p.sanPhamId)) {
      map.set(p.sanPhamId, { ...p, variantCount: 1, minPrice: Number(p.giaBan), maxPrice: Number(p.giaBan) });
    } else {
      const ex = map.get(p.sanPhamId);
      ex.variantCount++;
      if (Number(p.giaBan) < ex.minPrice) ex.minPrice = Number(p.giaBan);
      if (Number(p.giaBan) > ex.maxPrice) ex.maxPrice = Number(p.giaBan);
    }
  });
  return [...map.values()];
});

// fetchSerialMap() giờ chỉ dùng bởi openXacNhanSerialModal() (Orders) — đã chuyển cùng
// vào components/admin/OrdersTable.vue (Task 5). ProductDetailModal.vue có bản sao riêng.

// ── Customers CRUD — đã chuyển vào components/admin/CustomersTable.vue +
// CustomerFormModal.vue (Task 4). Luồng "thêm khách hàng nhanh" từ POS (từng tạm
// thời bị hỏng sau Task 4) đã được khắc phục trong PosPanel.vue (Task 6) bằng 1
// instance CustomerFormModal riêng của PosPanel.

// ── Staff CRUD ────────────────────────────────────────────────────────────────
const showStaffModal = ref(false);
const editingStaffId = ref(null);
const staffFormError = ref("");
const emptyStaffForm = () => ({
  hoTen: "",
  soDienThoai: "",
  email: "",
  chucVuId: null,
  username: "",
  matKhauHash: "",
  luongCoBan: 0,
  trangThai: "active",
});
const staffForm = reactive(emptyStaffForm());

const openAddStaff = () => {
  Object.assign(staffForm, emptyStaffForm());
  editingStaffId.value = null;
  staffFormError.value = "";
  showStaffModal.value = true;
};
const openEditStaff = (s) => {
  Object.assign(staffForm, {
    hoTen: s.hoTen,
    soDienThoai: s.soDienThoai,
    email: s.email ?? "",
    chucVuId: s.chucVuId,
    username: s.username ?? "",
    matKhauHash: "",
    luongCoBan: s.luongCoBan ?? 0,
    trangThai: s.trangThai ?? "active",
  });
  editingStaffId.value = s.nhanVienId;
  staffFormError.value = "";
  showStaffModal.value = true;
};
const saveStaff = async () => {
  staffFormError.value = "";
  const body = {
    ...staffForm,
    chucVuId: Number(staffForm.chucVuId),
    luongCoBan: Number(staffForm.luongCoBan),
  };
  try {
    const res = await NhanVienService.save(editingStaffId.value, body);
    if (!res.ok) {
      staffFormError.value = t('admin.errors.saveFailedWithText', { status: res.status, text: await res.text() });
      return;
    }
    showStaffModal.value = false;
    if (editingStaffId.value) {
      const idx = staff.value.findIndex((s) => s.nhanVienId === editingStaffId.value);
      if (idx !== -1) staff.value[idx] = { ...staff.value[idx], ...body };
    } else {
      await refreshStaff();
    }
  } catch (e) {
    staffFormError.value = e.message;
  }
};
const deleteStaff = async (id) => {
  if (!(await askConfirm(t('admin.confirm.deleteStaff')))) return;
  const res = await NhanVienService.remove(id);
  if (!res.ok) { showToast(t('admin.errors.deleteFailed', { status: res.status })); return; }
  await refreshStaff();
};

// ── Promotions CRUD ───────────────────────────────────────────────────────────
const showPromoModal = ref(false);
const editingPromoId = ref(null);
const promoFormError = ref("");
const emptyPromoForm = () => ({
  maKhuyenMai: "",
  tenKhuyenMai: "",
  loai: "percent",
  giaTri: "",
  giaTriToiDa: "",
  donHangToiThieu: "",
  ngayBatDau: "",
  ngayKetThuc: "",
  soLuongToiDa: "",
  trangThai: "active",
});
const promoForm = reactive(emptyPromoForm());

const openAddPromo = () => {
  Object.assign(promoForm, emptyPromoForm());
  editingPromoId.value = null;
  promoFormError.value = "";
  showPromoModal.value = true;
};
const openEditPromo = (p) => {
  const dt = (d) => (d ? d.slice(0, 16) : "");
  Object.assign(promoForm, {
    maKhuyenMai: p.maKhuyenMai,
    tenKhuyenMai: p.tenKhuyenMai,
    loai: p.loai ?? "percent",
    giaTri: p.giaTri ?? "",
    giaTriToiDa: p.giaTriToiDa ?? "",
    donHangToiThieu: p.donHangToiThieu ?? "",
    ngayBatDau: dt(p.ngayBatDau),
    ngayKetThuc: dt(p.ngayKetThuc),
    soLuongToiDa: p.soLuongToiDa ?? "",
    trangThai: p.trangThai ?? "active",
  });
  editingPromoId.value = p.khuyenMaiId;
  promoFormError.value = "";
  showPromoModal.value = true;
};
const savePromo = async () => {
  promoFormError.value = "";
  const body = {
    ...promoForm,
    giaTri: promoForm.giaTri ? Number(promoForm.giaTri) : null,
    giaTriToiDa: promoForm.giaTriToiDa ? Number(promoForm.giaTriToiDa) : null,
    donHangToiThieu: promoForm.donHangToiThieu
      ? Number(promoForm.donHangToiThieu)
      : null,
    soLuongToiDa: promoForm.soLuongToiDa
      ? Number(promoForm.soLuongToiDa)
      : null,
    ngayBatDau: toLocalDT(promoForm.ngayBatDau),
    ngayKetThuc: toLocalDT(promoForm.ngayKetThuc),
  };
  try {
    const res = await KhuyenMaiService.save(editingPromoId.value, body);
    if (!res.ok) {
      promoFormError.value = t('admin.errors.saveFailedWithText', { status: res.status, text: await res.text() });
      return;
    }
    showPromoModal.value = false;
    if (editingPromoId.value) {
      const idx = promotions.value.findIndex((p) => p.khuyenMaiId === editingPromoId.value);
      if (idx !== -1) promotions.value[idx] = { ...promotions.value[idx], ...body };
    } else {
      await refreshPromotions();
    }
  } catch (e) {
    promoFormError.value = e.message;
  }
};
const deletePromo = async (id) => {
  if (!(await askConfirm(t('admin.confirm.deletePromo')))) return;
  const res = await KhuyenMaiService.remove(id);
  if (!res.ok) { showToast(t('admin.errors.deleteFailed', { status: res.status })); return; }
  await refreshPromotions();
};

// ── Orders CRUD/detail/status/serial-confirm — đã chuyển vào
// components/admin/OrdersTable.vue (Task 5), gồm openVariantDetail() (đã sửa xong lỗi
// ReferenceError từ Task 3 tại đó, xem ghi chú trong OrdersTable.vue).
// ── Inventory stock edit + Stock Detail Modal — đã chuyển vào
// components/admin/InventoryPanel.vue (Task 7).
// ── POS / Ban hang — da chuyen vao components/admin/PosPanel.vue (Task 6), gom ca
// sua loi posConfirmCreateCustomer() de lai tu Task 4 (xem CustomerFormModal.vue).

// ── Cài đặt: đổi mật khẩu ──────────────────────────────────────────────────────
const cdMatKhauCu = ref('');
const cdMatKhauMoi = ref('');
const cdMatKhauXacNhan = ref('');
const cdMatKhauError = ref('');
const cdMatKhauSuccess = ref('');
const cdMatKhauLoading = ref(false);

const doiMatKhauSubmit = async () => {
  cdMatKhauError.value = '';
  cdMatKhauSuccess.value = '';
  if (cdMatKhauMoi.value !== cdMatKhauXacNhan.value) {
    cdMatKhauError.value = t('admin.settings.passwordMismatch');
    return;
  }
  cdMatKhauLoading.value = true;
  try {
    await CaiDatService.doiMatKhau(cdMatKhauCu.value, cdMatKhauMoi.value);
    cdMatKhauSuccess.value = t('admin.settings.passwordChanged');
    cdMatKhauCu.value = '';
    cdMatKhauMoi.value = '';
    cdMatKhauXacNhan.value = '';
  } catch (e) {
    cdMatKhauError.value = e.message || String(e);
  } finally {
    cdMatKhauLoading.value = false;
  }
};

// ── Cài đặt: thông tin cửa hàng ─────────────────────────────────────────────────
const cdForm = reactive({
  tenCuaHang: '', diaChi: '', soDienThoai: '', email: '', maSoThue: '', logoUrl: '',
});
const cdLogoPreview = ref('');
const cdLogoFilePending = ref(null);
const cdStoreSaving = ref(false);
const cdStoreSaved = ref(false);
const cdStoreError = ref('');

watch(() => SettingsStore.loaded, (loaded) => {
  if (!loaded) return;
  cdForm.tenCuaHang = SettingsStore.tenCuaHang;
  cdForm.diaChi = SettingsStore.diaChi;
  cdForm.soDienThoai = SettingsStore.soDienThoai;
  cdForm.email = SettingsStore.email;
  cdForm.maSoThue = SettingsStore.maSoThue;
  cdForm.logoUrl = SettingsStore.logoUrl;
  cdLogoPreview.value = SettingsStore.logoUrl || '';
}, { immediate: true });

const handleLogoFile = (e) => {
  const file = e.target.files[0];
  if (!file) return;
  cdLogoFilePending.value = file;
  cdLogoPreview.value = URL.createObjectURL(file);
};

const saveStoreInfo = async () => {
  cdStoreError.value = '';
  cdStoreSaving.value = true;
  cdStoreSaved.value = false;
  try {
    if (cdLogoFilePending.value) {
      const fd = new FormData();
      fd.append('file', cdLogoFilePending.value);
      const upRes = await fetch('/api/upload/image', { method: 'POST', headers: authHeaders(), body: fd });
      if (upRes.ok) {
        const upData = await upRes.json();
        cdForm.logoUrl = upData.url;
      } else {
        throw new Error('Upload ảnh thất bại: ' + upRes.status);
      }
    }
    const updated = await CaiDatService.updateCaiDat({
      tenCuaHang: cdForm.tenCuaHang, diaChi: cdForm.diaChi, soDienThoai: cdForm.soDienThoai,
      email: cdForm.email, maSoThue: cdForm.maSoThue, logoUrl: cdForm.logoUrl,
      ngonNguMacDinh: SettingsStore.ngonNguMacDinh, dinhDangSo: SettingsStore.dinhDangSo,
    });
    Object.assign(SettingsStore, updated);
    cdLogoFilePending.value = null;
    cdStoreSaved.value = true;
  } catch (e) {
    cdStoreError.value = e.message || String(e);
  } finally {
    cdStoreSaving.value = false;
  }
};

// ── Cài đặt: ngưỡng cảnh báo tồn kho ─────────────────────────────────────────────
const cdNguongTonKho = ref(5);
watch(() => SettingsStore.loaded, (loaded) => {
  if (loaded) cdNguongTonKho.value = SettingsStore.nguongTonKhoMacDinh;
}, { immediate: true });
const cdApplyingThreshold = ref(false);

const apDungNguongTonKhoSubmit = async () => {
  const count = inventory.value.length;
  const ok = await askConfirm(t('admin.settings.applyToAllConfirm', { nguong: cdNguongTonKho.value, count }));
  if (!ok) return;
  cdApplyingThreshold.value = true;
  try {
    const res = await CaiDatService.apDungNguongTonKho(cdNguongTonKho.value);
    SettingsStore.nguongTonKhoMacDinh = cdNguongTonKho.value;
    // Không có loader tồn-kho-riêng-lẻ trong file này — inventory chỉ được tải lại cùng
    // 1 lượt với products/orders/customers/promotions qua fetchAll() (AdminPage.vue:1001-1019),
    // dùng lại đúng hàm đó để bảng/cảnh báo hết hàng cập nhật ngay.
    await fetchAll();
    // showToast(msg, type) đã có sẵn (AdminPage.vue:35-45), dùng lại thay vì alert() —
    // toàn bộ thông báo thành công/lỗi khác trong trang admin đều qua đường này.
    showToast(t('admin.settings.applyToAllDone', { count: res.soBienTheDaCapNhat }), 'success');
  } catch (e) {
    showToast(e.message || String(e), 'error');
  } finally {
    cdApplyingThreshold.value = false;
  }
};

// Lưu ngôn ngữ mặc định / định dạng số ngay khi đổi dropdown — đọc field từ SettingsStore
// (không phải cdForm) vì 2 lý do: (1) cdForm chỉ được điền sau khi SettingsStore.loaded,
// đổi dropdown trước lúc đó sẽ gửi chuỗi rỗng đè lên dữ liệu thật; (2) đổi ngôn ngữ không
// nên vô tình lưu luôn các trường thông tin cửa hàng đang gõ dở nhưng chưa bấm Lưu.
const saveAppearancePrefs = async () => {
  try {
    const updated = await CaiDatService.updateCaiDat({
      tenCuaHang: SettingsStore.tenCuaHang, diaChi: SettingsStore.diaChi,
      soDienThoai: SettingsStore.soDienThoai, email: SettingsStore.email,
      maSoThue: SettingsStore.maSoThue, logoUrl: SettingsStore.logoUrl,
      ngonNguMacDinh: SettingsStore.ngonNguMacDinh, dinhDangSo: SettingsStore.dinhDangSo,
    });
    Object.assign(SettingsStore, updated);
  } catch (e) {
    showToast(e.message || String(e), 'error');
  }
};

// SSE đơn hàng real-time giờ dùng chung qua stores/orders.js (connectOrderEvents/
// disconnectOrderEvents) — không còn EventSource cục bộ ở đây. connectOrderEvents() vẫn phân
// biệt được 'new-order' vs 'order-updated' qua callback onNewOrder/onOrderUpdated, nên chỉ
// gộp đơn trùng + làm mới biểu đồ SP bán chạy khi thực sự có đơn MỚI (không chạy lại trên mọi
// thay đổi của OrdersStore.items như xoá đơn/đổi trạng thái).
onMounted(async () => {
  // try/catch riêng — nếu fetchAll()/fetchProductSales() lỗi (throw) mà không có try/catch
  // ở đây, phần mở SSE bên dưới sẽ KHÔNG BAO GIỜ chạy tới (await bị chặn ngang), khiến admin
  // mất real-time vĩnh viễn dù F5 lại bao nhiêu lần cũng vậy.
  try {
    await fetchAll();
    await fetchProductSales();
  } catch (e) {
    console.error('fetchAll/fetchProductSales lỗi khi vào trang:', e);
  }

  connectOrderEvents(AuthStore.user?.token, {
    onNewOrder: () => { autoMergeAllDuplicates(); fetchProductSales(); },
  });
});

onUnmounted(() => {
  disconnectOrderEvents();
});
</script>

<template>
  <!-- Layout chính: sidebar bên trái + main content bên phải -->
  <div class="d-flex overflow-hidden" style="height:100vh; background:var(--bg-page-alt); color:var(--text-primary); font-family:'Nunito Sans',sans-serif;">

    <!-- ══════════ SIDEBAR ══════════ -->
    <aside class="d-flex flex-column border-end flex-shrink-0"
           style="width:240px; background:var(--bg-card-inset); border-color:var(--border-color)!important; overflow-y:auto;">

      <!-- Logo -->
      <div class="d-flex align-items-center gap-2 p-3 border-bottom"
           style="border-color:var(--border-color-soft)!important;">
        <div class="rounded-circle d-flex align-items-center justify-content-center fw-black flex-shrink-0"
             style="width:38px;height:38px;background:var(--accent);color:var(--accent-text);font-size:0.8rem;">SAO</div>
        <div>
          <div class="fw-bold" style="font-size:0.95rem;">{{ t('admin.brand.name') }}</div>
          <div style="font-size:0.7rem;color:var(--text-muted);">{{ t('admin.brand.tagline') }}</div>
        </div>
      </div>

      <!-- Nav admin -->
      <nav class="flex-grow-1 d-flex flex-column px-2 pb-2">
        <div class="adm-nav-label">{{ t('admin.sidebar.groupOverview') }}</div>
        <div class="adm-nav" :class="{active: currentPage==='dashboard'}" @click="navigate('dashboard')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M3 4a1 1 0 011-1h3a1 1 0 011 1v3a1 1 0 01-1 1H4a1 1 0 01-1-1V4zm7 0a1 1 0 011-1h3a1 1 0 011 1v3a1 1 0 01-1 1h-3a1 1 0 01-1-1V4zM3 11a1 1 0 011-1h3a1 1 0 011 1v3a1 1 0 01-1 1H4a1 1 0 01-1-1v-3zm7 0a1 1 0 011-1h3a1 1 0 011 1v3a1 1 0 01-1 1h-3a1 1 0 01-1-1v-3z"/></svg>
          {{ t('admin.sidebar.dashboard') }}
        </div>

        <div class="adm-nav-label">{{ t('admin.sidebar.groupManagement') }}</div>
        <div class="adm-nav" :class="{active: currentPage==='products'}" @click="navigate('products')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M5 3a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2V5a2 2 0 00-2-2H5zm0 8a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2v-2a2 2 0 00-2-2H5zm6-6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V5zm0 8a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"/></svg>
          {{ t('admin.sidebar.products') }}
        </div>
        <div class="adm-nav" :class="{active: currentPage==='orders'}" @click="navigate('orders')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M9 2a1 1 0 000 2h2a1 1 0 100-2H9z"/><path fill-rule="evenodd" d="M4 5a2 2 0 012-2 3 3 0 003 3h2a3 3 0 003-3 2 2 0 012 2v11a2 2 0 01-2 2H6a2 2 0 01-2-2V5zm3 4a1 1 0 000 2h.01a1 1 0 100-2H7zm3 0a1 1 0 000 2h3a1 1 0 100-2h-3zm-3 4a1 1 0 100 2h.01a1 1 0 100-2H7zm3 0a1 1 0 100 2h3a1 1 0 100-2h-3z" clip-rule="evenodd"/></svg>
          {{ t('admin.sidebar.orders') }}
          <span class="badge bg-warning text-dark ms-auto" style="font-size:0.68rem;">{{ todayOrdersCount }}</span>
        </div>
        <div class="adm-nav" :class="{active: currentPage==='customers'}" @click="navigate('customers')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3zM6 8a2 2 0 11-4 0 2 2 0 014 0zM16 18v-3a5.972 5.972 0 00-.75-2.906A3.005 3.005 0 0119 15v3h-3zM4.75 12.094A5.973 5.973 0 004 15v3H1v-3a3 3 0 013.75-2.906z"/></svg>
          {{ t('admin.sidebar.customers') }}
          <span class="badge bg-warning text-dark ms-auto" style="font-size:0.68rem;">{{ totalCustomers }}</span>
        </div>
        <div class="adm-nav" :class="{active: currentPage==='inventory'}" @click="navigate('inventory')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M4 3a2 2 0 100 4h12a2 2 0 100-4H4z"/><path fill-rule="evenodd" d="M3 8h14v7a2 2 0 01-2 2H5a2 2 0 01-2-2V8zm5 3a1 1 0 011-1h2a1 1 0 110 2H9a1 1 0 01-1-1z" clip-rule="evenodd"/></svg>
          {{ t('admin.sidebar.inventory') }}
          <span v-if="lowStockItems.length" class="badge bg-danger ms-auto" style="font-size:0.68rem;">{{ lowStockItems.length }}</span>
        </div>
        <div class="adm-nav" :class="{active: currentPage==='tra-hang'}" @click="navigate('tra-hang')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M9.707 3.293a1 1 0 010 1.414L7.414 7H15a1 1 0 110 2H7.414l2.293 2.293a1 1 0 11-1.414 1.414l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 0z" clip-rule="evenodd"/></svg>
          {{ t('admin.sidebar.traHang') }}
        </div>
        <div class="adm-nav" :class="{active: currentPage==='promotions'}" @click="navigate('promotions')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M5 5a3 3 0 015-2.236A3 3 0 0114.83 6H16a2 2 0 110 4h-5V9a1 1 0 10-2 0v1H4a2 2 0 110-4h1.17C5.06 5.687 5 5.35 5 5zm4 1V5a1 1 0 10-1 1h1zm3 0a1 1 0 10-1-1v1h1z" clip-rule="evenodd"/><path d="M9 11H3v5a2 2 0 002 2h4v-7zm2 7h4a2 2 0 002-2v-5h-6v7z"/></svg>
          {{ t('admin.sidebar.promotions') }}
        </div>
        <div class="adm-nav" :class="{active: currentPage==='ban-hang'}" @click="navigate('ban-hang')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M3 1a1 1 0 000 2h1.22l.305 1.222a.997.997 0 00.01.042l1.358 5.43-.893.892C4.328 11.142 4 11.574 4 12a2 2 0 002 2h10a1 1 0 000-2H6.414l1-1H14a1 1 0 00.894-.553l3-6A1 1 0 0017 4H6.28l-.31-1.243A1 1 0 005 2H3z"/><path d="M16 16.5a1.5 1.5 0 11-3 0 1.5 1.5 0 013 0zM6.5 18a1.5 1.5 0 100-3 1.5 1.5 0 000 3z"/></svg>
          {{ t('admin.sidebar.banHang') }}
        </div>
        <div class="adm-nav" :class="{active: currentPage==='staff'}" @click="navigate('staff')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clip-rule="evenodd"/></svg>
          {{ t('admin.sidebar.staff') }}
        </div>

        <div class="adm-nav-label">{{ t('admin.sidebar.groupAnalytics') }}</div>
        <div class="adm-nav" :class="{active: currentPage==='reports'}" @click="navigate('reports')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M2 11a1 1 0 011-1h2a1 1 0 011 1v5a1 1 0 01-1 1H3a1 1 0 01-1-1v-5zm6-4a1 1 0 011-1h2a1 1 0 011 1v9a1 1 0 01-1 1H9a1 1 0 01-1-1V7zm6-3a1 1 0 011-1h2a1 1 0 011 1v12a1 1 0 01-1 1h-2a1 1 0 01-1-1V4z"/></svg>
          {{ t('admin.sidebar.reports') }}
        </div>
        <div class="adm-nav" :class="{active: currentPage==='settings'}" @click="navigate('settings')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M11.49 3.17c-.38-1.56-2.6-1.56-2.98 0a1.532 1.532 0 01-2.286.948c-1.372-.836-2.942.734-2.106 2.106.54.886.061 2.042-.947 2.287-1.561.379-1.561 2.6 0 2.978a1.532 1.532 0 01.947 2.287c-.836 1.372.734 2.942 2.106 2.106a1.532 1.532 0 012.287.947c.379 1.561 2.6 1.561 2.978 0a1.533 1.533 0 012.287-.947c1.372.836 2.942-.734 2.106-2.106a1.533 1.533 0 01.947-2.287c1.561-.379 1.561-2.6 0-2.978a1.532 1.532 0 01-.947-2.287c.836-1.372-.734-2.942-2.106-2.106a1.532 1.532 0 01-2.287-.947zM10 13a3 3 0 100-6 3 3 0 000 6z" clip-rule="evenodd"/></svg>
          {{ t('admin.sidebar.settings') }}
        </div>
      </nav>

      <UserProfileMenu @navigate-settings="navigate('settings')" />
    </aside><!-- /sidebar -->

    <!-- ══════════ MAIN CONTENT ══════════ -->
    <main class="flex-grow-1 d-flex flex-column overflow-hidden">

      <!-- Topbar: tieu de trang hien tai -->
      <div class="d-flex align-items-center justify-content-between p-3 border-bottom"
           style="background:var(--bg-card-inset); border-color:var(--border-color)!important;">
        <div>
          <div class="fw-bold" style="font-size:1.05rem;">{{ topbarIcon }} {{ topbarTitle }}</div>
          <div style="font-size:0.78rem;color:var(--text-muted);">{{ topbarSub }}</div>
        </div>
        <div class="d-flex align-items-center gap-2">
          <button type="button" class="d-flex align-items-center justify-content-center rounded-2 border-0"
                  style="width:34px;height:34px;background:var(--bg-hover);color:var(--text-primary);cursor:pointer;font-size:1rem;"
                  :title="ThemeStore.mode === 'dark' ? t('theme.toggleToLight') : t('theme.toggleToDark')"
                  :aria-label="ThemeStore.mode === 'dark' ? t('theme.toggleToLight') : t('theme.toggleToDark')"
                  @click="toggleTheme">
            {{ ThemeStore.mode === 'dark' ? '🌙' : '☀️' }}
          </button>
          <div class="d-flex align-items-center justify-content-center rounded-2"
               style="width:34px;height:34px;background:var(--bg-hover);cursor:pointer;">&#128276;</div>
        </div>
      </div>

      <!-- Noi dung trang (scroll duoc) -->
      <div class="flex-grow-1 overflow-y-auto p-4">

        <!-- ── Dashboard ── -->
        <section v-show="currentPage === 'dashboard'">
          <div v-if="ProductsStore.loading || OrdersStore.loading || CustomersStore.loading || InventoryStore.loading" class="text-secondary small">{{ t('admin.dashboard.loading') }}</div>
          <template v-else>
            <!-- Stat cards -->
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

            <!-- Canh bao het hang -->
            <div v-if="lowStockItems.length" class="alert alert-danger small py-2 mb-3 d-flex align-items-center gap-2">
              <span class="rounded-circle d-flex align-items-center justify-content-center flex-shrink-0"
                    style="width:22px;height:22px;background:rgba(248,113,113,0.25);font-size:0.85rem;">⚠️</span>
              {{ t('admin.dashboard.lowStockAlert', { count: lowStockItems.length }) }}
            </div>

            <!-- Bieu do thong ke -->
            <div class="row g-3 mb-4">
              <div class="col-12 col-xl-5">
                <div class="card border-secondary h-100" style="background:var(--bg-hover);">
                  <div class="card-body">
                    <div class="d-flex align-items-center justify-content-between flex-wrap gap-2 mb-2">
                      <div class="fw-semibold small text-secondary">🍩 {{ t('admin.dashboard.ordersByStatusChart') }}</div>
                      <div class="d-flex align-items-center gap-2">
                        <input type="date" v-model="statusChartDate" :max="toDateInputValue(new Date())"
                               class="form-control form-control-sm"
                               style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong); width:auto; font-size:0.78rem; padding:2px 8px;" />
                        <button v-if="!isStatusChartToday" type="button" class="btn btn-sm py-0 px-2"
                                style="font-size:0.72rem; color:var(--accent-fg); border:1px solid var(--border-color-strong);"
                                @click="statusChartDate = toDateInputValue(new Date())">
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
                        <input type="date" v-model="weekChartAnchor" :max="toDateInputValue(new Date())"
                               class="form-control form-control-sm"
                               style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong); width:auto; font-size:0.78rem; padding:2px 8px;" />
                        <button v-if="!isWeekChartCurrentWeek" type="button" class="btn btn-sm py-0 px-2"
                                style="font-size:0.72rem; color:var(--accent-fg); border:1px solid var(--border-color-strong);"
                                @click="resetToCurrentWeek">
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

            <!-- San pham ban chay / ban cham -->
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

            <!-- Gauge KPI: suc khoe van hanh -->
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

            <!-- Xu huong doanh thu theo thang -->
            <div class="card border-secondary mb-4" style="background:var(--bg-hover);">
              <div class="card-body">
                <div class="fw-semibold small text-secondary mb-3">📈 {{ t('admin.dashboard.revenueTrendChart') }}</div>
                <TrendChart :data="revenueTrendChart" :height="140" color="#f06b81" :empty-text="t('admin.dashboard.chartEmptyOrders')" />
              </div>
            </div>

            <!-- Bang san pham gan day -->
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

        <!-- ── San pham ── -->
        <section v-show="currentPage === 'products'">
          <ul class="nav nav-tabs mb-3">
            <li class="nav-item"><button class="nav-link" :class="{active: productsMainTab==='sanPham'}" @click="productsMainTab='sanPham'">{{ t('admin.productsTabs.sanPham') }}</button></li>
            <li class="nav-item"><button class="nav-link" :class="{active: productsMainTab==='cpu'}" @click="productsMainTab='cpu'">{{ t('admin.productsTabs.cpu') }}</button></li>
            <li class="nav-item"><button class="nav-link" :class="{active: productsMainTab==='ram'}" @click="productsMainTab='ram'">{{ t('admin.productsTabs.ram') }}</button></li>
            <li class="nav-item"><button class="nav-link" :class="{active: productsMainTab==='gpu'}" @click="productsMainTab='gpu'">{{ t('admin.productsTabs.gpu') }}</button></li>
            <li class="nav-item"><button class="nav-link" :class="{active: productsMainTab==='oCung'}" @click="productsMainTab='oCung'">{{ t('admin.productsTabs.oCung') }}</button></li>
          </ul>

          <div v-show="productsMainTab==='sanPham'">
            <ProductsTable />
          </div>
          <div v-show="productsMainTab==='cpu'">
            <DmCategoryTable :service="DmService.DmCpuService" id-field="cpuId" name-field="tenCpu" :label="t('admin.productsTabs.cpu')" :name-label="t('admin.productsTabs.cpu')" :serial-service="ChiTietCpuService" serial-field-name="cpuId" />
          </div>
          <div v-show="productsMainTab==='ram'">
            <DmCategoryTable :service="DmService.DmRamService" id-field="ramId" name-field="dungLuong" :label="t('admin.productsTabs.ram')" :name-label="t('admin.productsTabs.ram')" :serial-service="ChiTietRamService" serial-field-name="ramId" />
          </div>
          <div v-show="productsMainTab==='gpu'">
            <DmCategoryTable :service="DmService.DmGpuService" id-field="gpuId" name-field="tenGpu" :label="t('admin.productsTabs.gpu')" :name-label="t('admin.productsTabs.gpu')" :serial-service="ChiTietGpuService" serial-field-name="gpuId" />
          </div>
          <div v-show="productsMainTab==='oCung'">
            <DmCategoryTable :service="DmService.DmOCungService" id-field="oCungId" name-field="loaiOcung" :label="t('admin.productsTabs.oCung')" :name-label="t('admin.productsTabs.oCung')" :serial-service="ChiTietOCungService" serial-field-name="oCungId" />
          </div>
        </section>

        <!-- ── Don hang ── -->
        <section v-show="currentPage === 'orders'">
          <OrdersTable />
        </section>

        <!-- ── Khach hang ── -->
        <section v-show="currentPage === 'customers'">
          <CustomersTable />
        </section>

        <!-- ── Kho hang ── -->
        <section v-show="currentPage === 'inventory'">
          <ul class="nav nav-tabs mb-3">
            <li class="nav-item">
              <button class="nav-link" :class="{active: inventoryMainTab==='kho'}" @click="inventoryMainTab='kho'">📦 {{ t('admin.inventory.tabStock') }} / {{ t('admin.inventory.tabReceipts') }}</button>
            </li>
            <li class="nav-item">
              <button class="nav-link" :class="{active: inventoryMainTab==='bao-hanh'}" @click="inventoryMainTab='bao-hanh'">🛡️ {{ t('admin.inventory.tabWarranty') }}</button>
            </li>
            <li class="nav-item">
              <button class="nav-link" :class="{active: inventoryMainTab==='serial'}" @click="inventoryMainTab='serial'">🔢 {{ t('admin.inventory.tabSerial') }}</button>
            </li>
          </ul>

          <div v-show="inventoryMainTab==='kho'">
            <InventoryPanel />
          </div>

          <!-- ══ TAB: BAO HANH ══ -->
          <div v-show="inventoryMainTab==='bao-hanh'">
            <WarrantyPanel />
          </div>

          <!-- ══ TAB: SERIAL ══ -->
          <div v-show="inventoryMainTab==='serial'">
            <SerialManager />
          </div>
        </section>

        <section v-show="currentPage === 'tra-hang'"><ReturnsPanel :can-pick-staff="true" /></section>

        <!-- ── Khuyen mai ── -->
        <section v-show="currentPage === 'promotions'">
          <div class="d-flex justify-content-between align-items-center mb-3">
            <span class="text-secondary small">{{ promotions.length }} {{ t('admin.promotions.countSuffix') }}</span>
            <button class="btn btn-sm btn-warning text-dark fw-bold" @click="openAddPromo">{{ t('admin.promotions.add') }}</button>
          </div>
          <div v-if="PromotionsStore.loading" class="text-secondary small">{{ t('admin.promotions.loading') }}</div>
          <div v-else class="table-responsive">
            <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
              <thead><tr><th style="width:40px;">{{ t('admin.common.stt') }}</th><th>{{ t('admin.promotions.colCode') }}</th><th>{{ t('admin.promotions.colName') }}</th><th>{{ t('admin.promotions.colType') }}</th><th>{{ t('admin.promotions.colValue') }}</th><th>{{ t('admin.promotions.colStart') }}</th><th>{{ t('admin.promotions.colEnd') }}</th><th>{{ t('admin.promotions.colUsed') }}</th><th>{{ t('admin.promotions.colStatus') }}</th><th>{{ t('admin.promotions.colAction') }}</th></tr></thead>
              <tbody>
                <tr v-for="(p, idx) in promotions" :key="p.khuyenMaiId">
                  <td class="text-secondary">{{ idx + 1 }}</td>
                  <td class="text-secondary">{{ p.maKhuyenMai }}</td>
                  <td>{{ p.tenKhuyenMai }}</td>
                  <td>{{ p.loai==='percent'?t('admin.promotions.typePercent'):t('admin.promotions.typeFixed') }}</td>
                  <td>{{ p.loai==='percent'?`${p.giaTri}%`:formatPrice(p.giaTri) }}</td>
                  <td>{{ formatDate(p.ngayBatDau) }}</td>
                  <td>{{ formatDate(p.ngayKetThuc) }}</td>
                  <td>{{ p.soLanDaDung??0 }}/{{ p.soLuongToiDa??'∞' }}</td>
                  <td><span class="badge" :class="p.trangThai==='active'?'bg-success':'bg-secondary'">{{ statusLabel(p.trangThai) }}</span></td>
                  <td>
                    <div class="d-flex gap-1">
                      <button class="btn btn-sm btn-outline-warning" style="font-size:0.78rem; padding:2px 8px;" @click="openEditPromo(p)">{{ t('admin.promotions.edit') }}</button>
                      <button class="btn btn-sm btn-outline-danger"  style="font-size:0.78rem; padding:2px 8px;" @click="deletePromo(p.khuyenMaiId)">{{ t('admin.promotions.delete') }}</button>
                    </div>
                  </td>
                </tr>
                <tr v-if="promotions.length===0"><td colspan="9" class="text-center text-secondary">{{ t('admin.promotions.empty') }}</td></tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- ── Nhan vien ── -->
        <section v-show="currentPage === 'staff'">
          <div class="d-flex justify-content-between align-items-center mb-3">
            <span class="text-secondary small">{{ staff.length }} {{ t('admin.staff.countSuffix') }}</span>
            <button class="btn btn-sm btn-warning text-dark fw-bold" @click="openAddStaff">{{ t('admin.staff.add') }}</button>
          </div>
          <div v-if="StaffStore.loading" class="text-secondary small">{{ t('admin.staff.loading') }}</div>
          <div v-else class="table-responsive">
            <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
              <thead><tr><th style="width:40px;">{{ t('admin.common.stt') }}</th><th>{{ t('admin.staff.colFullName') }}</th><th>{{ t('admin.staff.colPhone') }}</th><th>{{ t('admin.staff.colEmail') }}</th><th>{{ t('admin.staff.colPosition') }}</th><th>{{ t('admin.staff.colUsername') }}</th><th>{{ t('admin.staff.colBaseSalary') }}</th><th>{{ t('admin.staff.colStatus') }}</th><th>{{ t('admin.staff.colAction') }}</th></tr></thead>
              <tbody>
                <tr v-for="(s, idx) in staff" :key="s.nhanVienId">
                  <td class="text-secondary">{{ idx + 1 }}</td>
                  <td>{{ s.hoTen }}</td>
                  <td class="text-secondary">{{ s.soDienThoai }}</td>
                  <td class="text-secondary">{{ s.email }}</td>
                  <td>{{ chucVuName(s.chucVuId) }}</td>
                  <td class="text-secondary">{{ s.username }}</td>
                  <td>{{ formatPrice(s.luongCoBan) }}</td>
                  <td><span class="badge" :class="s.trangThai==='active'?'bg-success':'bg-secondary'">{{ statusLabel(s.trangThai) }}</span></td>
                  <td>
                    <div class="d-flex gap-1">
                      <button class="btn btn-sm btn-outline-warning" style="font-size:0.78rem; padding:2px 8px;" @click="openEditStaff(s)">{{ t('admin.staff.edit') }}</button>
                      <button class="btn btn-sm btn-outline-danger"  style="font-size:0.78rem; padding:2px 8px;" @click="deleteStaff(s.nhanVienId)">{{ t('admin.staff.delete') }}</button>
                    </div>
                  </td>
                </tr>
                <tr v-if="staff.length===0"><td colspan="8" class="text-center text-secondary">{{ t('admin.staff.empty') }}</td></tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- ── Bao cao ── -->
        <section v-show="currentPage === 'reports'">
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
                        @click="reportsGroupBy=opt">
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
                        @click="reportsDateRange=opt">
                  {{ t(`admin.reports.dateRange${opt.charAt(0).toUpperCase()}${opt.slice(1)}`) }}
                </button>
              </div>
              <template v-if="reportsDateRange==='custom'">
                <input type="date" v-model="reportsCustomFrom" class="form-control form-control-sm"
                       :aria-label="t('admin.reports.dateFrom')"
                       style="width:auto;background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
                <span class="text-secondary small">→</span>
                <input type="date" v-model="reportsCustomTo" class="form-control form-control-sm"
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

        <!-- ── Cai dat ── -->
        <section v-show="currentPage === 'settings'">
          <div class="row g-3">
            <!-- Đổi mật khẩu -->
            <div class="col-12 col-xl-6">
              <div class="card border-secondary h-100" style="background:var(--bg-hover);">
                <div class="card-body">
                  <div class="fw-bold mb-3">🔑 {{ t('admin.settings.changePasswordTitle') }}</div>
                  <div class="mb-2">
                    <label class="form-label small text-secondary mb-1">{{ t('admin.settings.currentPassword') }}</label>
                    <input type="password" v-model="cdMatKhauCu" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
                  </div>
                  <div class="mb-2">
                    <label class="form-label small text-secondary mb-1">{{ t('admin.settings.newPassword') }}</label>
                    <input type="password" v-model="cdMatKhauMoi" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
                  </div>
                  <div class="mb-3">
                    <label class="form-label small text-secondary mb-1">{{ t('admin.settings.confirmNewPassword') }}</label>
                    <input type="password" v-model="cdMatKhauXacNhan" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
                  </div>
                  <div v-if="cdMatKhauError" class="text-danger small mb-2">{{ cdMatKhauError }}</div>
                  <div v-if="cdMatKhauSuccess" class="text-success small mb-2">{{ cdMatKhauSuccess }}</div>
                  <button class="btn btn-warning btn-sm" :disabled="cdMatKhauLoading || !cdMatKhauCu || !cdMatKhauMoi" @click="doiMatKhauSubmit">
                    {{ t('admin.settings.changePasswordButton') }}
                  </button>
                </div>
              </div>
            </div>

            <!-- Thông tin cửa hàng -->
            <div class="col-12 col-xl-6">
              <div class="card border-secondary h-100" style="background:var(--bg-hover);">
                <div class="card-body">
                  <div class="fw-bold mb-3">🏪 {{ t('admin.settings.storeInfoTitle') }}</div>
                  <div class="d-flex align-items-center gap-3 mb-3">
                    <label class="d-flex flex-column align-items-center justify-content-center rounded-3 border border-secondary text-secondary" style="width:88px;height:70px;cursor:pointer;flex-shrink:0;overflow:hidden;background:var(--bg-card-inset);">
                      <img v-if="cdLogoPreview" :src="cdLogoPreview" style="width:88px;height:70px;object-fit:contain;" />
                      <span v-else style="font-size:1.3rem;">🖼️</span>
                      <input type="file" accept="image/*" class="d-none" @change="handleLogoFile" />
                    </label>
                    <span class="text-secondary small">{{ t('admin.settings.storeLogo') }}</span>
                  </div>
                  <div class="row g-2 mb-3">
                    <div class="col-12">
                      <label class="form-label small text-secondary mb-1">{{ t('admin.settings.storeName') }}</label>
                      <input v-model="cdForm.tenCuaHang" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
                    </div>
                    <div class="col-12">
                      <label class="form-label small text-secondary mb-1">{{ t('admin.settings.storeAddress') }}</label>
                      <input v-model="cdForm.diaChi" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
                    </div>
                    <div class="col-6">
                      <label class="form-label small text-secondary mb-1">{{ t('admin.settings.storePhone') }}</label>
                      <input v-model="cdForm.soDienThoai" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
                    </div>
                    <div class="col-6">
                      <label class="form-label small text-secondary mb-1">{{ t('admin.settings.storeEmail') }}</label>
                      <input v-model="cdForm.email" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
                    </div>
                    <div class="col-12">
                      <label class="form-label small text-secondary mb-1">{{ t('admin.settings.storeTaxCode') }}</label>
                      <input v-model="cdForm.maSoThue" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
                    </div>
                  </div>
                  <div v-if="cdStoreError" class="text-danger small mb-2">{{ cdStoreError }}</div>
                  <div v-if="cdStoreSaved" class="text-success small mb-2">{{ t('admin.settings.saved') }}</div>
                  <button class="btn btn-warning btn-sm" :disabled="cdStoreSaving" @click="saveStoreInfo">
                    {{ t('admin.settings.saveButton') }}
                  </button>
                </div>
              </div>
            </div>

            <!-- Ngưỡng cảnh báo tồn kho -->
            <div class="col-12 col-xl-6">
              <div class="card border-secondary h-100" style="background:var(--bg-hover);">
                <div class="card-body">
                  <div class="fw-bold mb-3">📦 {{ t('admin.settings.lowStockThresholdTitle') }}</div>
                  <div class="mb-3">
                    <label class="form-label small text-secondary mb-1">{{ t('admin.settings.lowStockThresholdLabel') }}</label>
                    <input type="number" min="0" v-model.number="cdNguongTonKho" class="form-control form-control-sm" style="width:120px;background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
                  </div>
                  <button class="btn btn-outline-warning btn-sm" :disabled="cdApplyingThreshold" @click="apDungNguongTonKhoSubmit">
                    {{ t('admin.settings.applyToAllButton') }}
                  </button>
                </div>
              </div>
            </div>

            <!-- Giao diện & ngôn ngữ -->
            <div class="col-12 col-xl-6">
              <div class="card border-secondary h-100" style="background:var(--bg-hover);">
                <div class="card-body">
                  <div class="fw-bold mb-3">🎨 {{ t('admin.settings.appearanceTitle') }}</div>
                  <div class="d-flex justify-content-between align-items-center py-2 border-bottom border-secondary small">
                    <span class="text-secondary">{{ t('admin.settings.themeLabel') }}</span>
                    <button type="button" class="btn btn-sm btn-outline-secondary" @click="toggleTheme">
                      {{ ThemeStore.mode === 'dark' ? '🌙' : '☀️' }}
                    </button>
                  </div>
                  <div class="d-flex justify-content-between align-items-center py-2 border-bottom border-secondary small">
                    <span class="text-secondary">{{ t('admin.settings.languageLabel') }}</span>
                    <select class="form-select form-select-sm" style="width:auto;background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);"
                            :value="I18nStore.locale" @change="setLocale($event.target.value)">
                      <option v-for="loc in LOCALES" :key="loc.code" :value="loc.code">{{ loc.flag }} {{ loc.label }}</option>
                    </select>
                  </div>
                  <div class="d-flex justify-content-between align-items-center py-2 border-bottom border-secondary small">
                    <span class="text-secondary">{{ t('admin.settings.defaultLanguageLabel') }}</span>
                    <select class="form-select form-select-sm" style="width:auto;background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);"
                            v-model="SettingsStore.ngonNguMacDinh"
                            @change="saveAppearancePrefs">
                      <option v-for="loc in LOCALES" :key="loc.code" :value="loc.code">{{ loc.flag }} {{ loc.label }}</option>
                    </select>
                  </div>
                  <div class="d-flex justify-content-between align-items-center py-2 small">
                    <span class="text-secondary">{{ t('admin.settings.numberFormatLabel') }}</span>
                    <select class="form-select form-select-sm" style="width:auto;background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);"
                            v-model="SettingsStore.dinhDangSo"
                            @change="saveAppearancePrefs">
                      <option value="vi">{{ t('admin.settings.numberFormatVi') }}</option>
                      <option value="en">{{ t('admin.settings.numberFormatEn') }}</option>
                    </select>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- ── Ban hang (POS) ── -->
        <section v-show="currentPage === 'ban-hang'">
          <PosPanel />
        </section>

      </div><!-- /content -->
    </main>
  </div><!-- /dashboard-shell -->

  <!-- ══ MODAL NHAN VIEN ══ -->
  <div v-if="showStaffModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="showStaffModal=false">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:560px;max-width:95vw;max-height:90vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>{{ editingStaffId?t('admin.staffModal.titleEdit'):t('admin.staffModal.titleAdd') }}</span>
        <button class="btn-close btn-close-white btn-sm" @click="showStaffModal=false"></button>
      </div>
      <div class="overflow-y-auto p-4">
        <div v-if="staffFormError" class="alert alert-danger small py-2 mb-3">{{ staffFormError }}</div>
        <div class="row g-3">
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.staffModal.fullNameLabel') }}</label><input v-model="staffForm.hoTen" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.staffModal.phoneLabel') }}</label><input v-model="staffForm.soDienThoai" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.staffModal.emailLabel') }}</label><input v-model="staffForm.email" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.staffModal.positionLabel') }}</label><select v-model="staffForm.chucVuId" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)"><option :value="null" disabled>{{ t('admin.staffModal.positionSelectPlaceholder') }}</option><option v-for="cv in chucVuList" :key="cv.id" :value="cv.id">{{ cv.tenChucVu }}</option></select></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.staffModal.usernameLabel') }}</label><input v-model="staffForm.username" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.staffModal.passwordLabel') }} {{ editingStaffId?t('admin.staffModal.passwordKeepHint'):t('admin.staffModal.passwordRequired') }}</label><input v-model="staffForm.matKhauHash" type="password" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.staffModal.baseSalaryLabel') }}</label><input v-model="staffForm.luongCoBan" type="number" min="0" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.staffModal.statusLabel') }}</label><select v-model="staffForm.trangThai" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)"><option value="active">{{ t('admin.staffModal.statusActive') }}</option><option value="inactive">{{ t('admin.staffModal.statusResigned') }}</option></select></div>
        </div>
      </div>
      <div class="d-flex justify-content-end gap-2 p-3 border-top border-secondary">
        <button class="btn btn-sm btn-outline-secondary" @click="showStaffModal=false">{{ t('admin.staffModal.cancel') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" @click="saveStaff">{{ editingStaffId?t('admin.staffModal.update'):t('admin.staffModal.addNew') }}</button>
      </div>
    </div>
  </div>

  <!-- ══ MODAL KHUYEN MAI ══ -->
  <div v-if="showPromoModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="showPromoModal=false">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:620px;max-width:95vw;max-height:90vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>{{ editingPromoId?t('admin.promoModal.titleEdit'):t('admin.promoModal.titleAdd') }}</span>
        <button class="btn-close btn-close-white btn-sm" @click="showPromoModal=false"></button>
      </div>
      <div class="overflow-y-auto p-4">
        <div v-if="promoFormError" class="alert alert-danger small py-2 mb-3">{{ promoFormError }}</div>
        <div class="row g-3">
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.promoModal.codeLabel') }}</label><input v-model="promoForm.maKhuyenMai" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.promoModal.nameLabel') }}</label><input v-model="promoForm.tenKhuyenMai" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.promoModal.typeLabel') }}</label><select v-model="promoForm.loai" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)"><option value="percent">{{ t('admin.promoModal.typePercent') }}</option><option value="fixed">{{ t('admin.promoModal.typeFixed') }}</option></select></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ promoForm.loai==='percent'?t('admin.promoModal.valueLabelPercent'):t('admin.promoModal.valueLabelFixed') }}</label><input v-model="promoForm.giaTri" type="number" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.promoModal.maxDiscountLabel') }}</label><input v-model="promoForm.giaTriToiDa" type="number" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.promoModal.minOrderLabel') }}</label><input v-model="promoForm.donHangToiThieu" type="number" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.promoModal.startDateLabel') }}</label><input v-model="promoForm.ngayBatDau" type="datetime-local" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.promoModal.endDateLabel') }}</label><input v-model="promoForm.ngayKetThuc" type="datetime-local" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.promoModal.maxUsageLabel') }}</label><input v-model="promoForm.soLuongToiDa" type="number" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.promoModal.statusLabel') }}</label><select v-model="promoForm.trangThai" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)"><option value="active">{{ t('admin.promoModal.statusActive') }}</option><option value="inactive">{{ t('admin.promoModal.statusStopped') }}</option></select></div>
        </div>
      </div>
      <div class="d-flex justify-content-end gap-2 p-3 border-top border-secondary">
        <button class="btn btn-sm btn-outline-secondary" @click="showPromoModal=false">{{ t('admin.promoModal.cancel') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" @click="savePromo">{{ editingPromoId?t('admin.promoModal.update'):t('admin.promoModal.addNew') }}</button>
      </div>
    </div>
  </div>

  <!-- Dialog xác nhận + toast dùng chung toàn trang — PHẢI nằm ngoài mọi v-if của modal cụ
       thể, nếu không component sẽ không tồn tại trong DOM khi modal đó đang đóng, khiến
       askConfirm()/showToast() gọi ra nhưng không có gì hiển thị (Promise của askConfirm
       treo mãi, code gọi nó bị kẹt không chạy tiếp). -->
  <ConfirmDialog />
  <ToastHost />
</template>

<style scoped>

/* CSS toi thieu cho nhung gi Bootstrap khong the thay the */

/* .text-light override (Bootstrap hardcode mau trang co dinh) da chuyen vao
   components/admin/InventoryPanel.vue (Task 7) — chi con dung trong modal
   Chi tiet phieu nhap, da chuyen het sang component do, khong con cho nao
   trong file nay dung class .text-light nua. */

/* Nav item: hover va active state voi mau vang dac trung */
.adm-nav {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 8px 10px;
  border-radius: 7px;
  cursor: pointer;
  font-size: 0.87rem;
  color: var(--text-primary);
  transition: background 0.12s, color 0.12s;
  user-select: none;
}
.adm-nav:hover { background: var(--bg-hover); color: var(--text-heading); }
.adm-nav.active { background: rgba(244,63,94,0.12); color: var(--accent-fg); }
.adm-nav.active .adm-icon { opacity: 1; }

/* Icon trong nav */
.adm-icon { width: 17px; height: 17px; flex-shrink: 0; opacity: 0.75; }

/* Tieu de phan nhom trong sidebar */
.adm-nav-label {
  font-size: 0.68rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: #555;
  text-transform: uppercase;
  padding: 10px 8px 3px;
}

</style>
