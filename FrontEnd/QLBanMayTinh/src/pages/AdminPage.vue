<script setup>
import { ref, computed, onMounted, onUnmounted, reactive, watch } from "vue";
import { AuthStore } from "../stores/index.js";
import { useRoute } from "vue-router";
import { t, I18nStore, LOCALES, setLocale } from "../i18n/index.js";
import { orderStatusLabel, orderStatusColor, orderStatusIcon } from "../utils/orderStatus.js";
import * as NhanVienService  from "../services/NhanVienService.js";
import * as DonHangService   from "../services/DonHangService.js";
import * as KhuyenMaiService from "../services/KhuyenMaiService.js";
import * as VongQuayService from "../services/VongQuayService.js";
import * as DmService              from "../services/DmService.js";
import * as DashboardService       from "../services/DashboardService.js";
import DonutChart from "../components/common/DonutChart.vue";
import RevenueBarChart from "../components/common/RevenueBarChart.vue";
import * as CaiDatService from "../services/CaiDatService.js";
import { SettingsStore } from "../stores/settings.js";
import BarChart   from "../components/common/BarChart.vue";
import TrendChart from "../components/common/TrendChart.vue";
import ConfirmDialog from "../components/common/ConfirmDialog.vue";
import { askConfirm } from "../stores/confirm.js";
import { ThemeStore, toggleTheme } from "../stores/theme.js";
import { authHeaders } from "../services/api.js";
import { formatPrice, formatDate, formatDateTime, statusLabel, toLocalDT } from "../utils/adminFormat.js";
import { showToast } from "../stores/toast.js";
import ToastHost from "../components/common/ToastHost.vue";
import CustomersTable from "../components/admin/CustomersTable.vue";
import CustomerDetailPage from "../components/admin/CustomerDetailPage.vue";
import SanPhamDetailPage from "../components/admin/SanPhamDetailPage.vue";
import OrdersTable from "../components/admin/OrdersTable.vue";
import PosPanel from "../components/admin/PosPanel.vue";
import InventoryPanel from "../components/admin/InventoryPanel.vue";
import SupplierManager from "../components/admin/SupplierManager.vue";
import InventoryHistoryPanel from "../components/admin/InventoryHistoryPanel.vue";
import ReturnsPanel from "../components/admin/ReturnsPanel.vue";
import WarrantyPanel from "../components/admin/WarrantyPanel.vue";
import DanhGiaPanel from "../components/admin/DanhGiaPanel.vue";
import SerialManager from "../components/admin/SerialManager.vue";
import DmCategoryTable from "../components/admin/DmCategoryTable.vue";
import UserProfileMenu from "../components/admin/UserProfileMenu.vue";
import StaffTable from "../components/admin/StaffTable.vue";
import PromotionsPanel from "../components/admin/PromotionsPanel.vue";
import RewardsPanel from "../components/admin/RewardsPanel.vue";
import AdminDashboard from "../components/admin/AdminDashboard.vue";
import AdminReports from "../components/admin/AdminReports.vue";
import AdminSettings from "../components/admin/AdminSettings.vue";
import AdminChatPanel from "../components/admin/AdminChatPanel.vue";
import AiKienThucPanel from "../components/admin/AiKienThucPanel.vue";
import HangHoa from "../components/HangHoa.vue";
import BienTheTable from "../components/admin/BienTheTable.vue";
import ThuocTinhPanel from "../components/admin/ThuocTinhPanel.vue";
import { ProductsStore, ensureProducts, refreshProducts } from "../stores/products.js";
import { OrdersStore, ensureOrders, refreshOrders, connectOrderEvents, disconnectOrderEvents } from "../stores/orders.js";
import { connectSerialEvents, disconnectSerialEvents } from "../stores/serialEvents.js";
import { CustomersStore, ensureCustomers, refreshCustomers } from "../stores/customers.js";
import { InventoryStore, ensureInventory, refreshInventory } from "../stores/inventory.js";
import { StaffStore, ensureStaff, refreshStaff } from "../stores/staff.js";
import { PromotionsStore, ensurePromotions, refreshPromotions } from "../stores/promotions.js";
import { DoiThuongStore, ensureDoiThuong, refreshDoiThuong } from "../stores/doiThuong.js";
import { refreshReturns } from "../stores/returns.js";
import * as DmDoiThuongService from "../services/DmDoiThuongService.js";
import {
  BarChart3, Laptop, Receipt, Users, User, Package, Undo2, Star, Tag, Gift,
  Briefcase, ShoppingCart, TrendingUp, Settings, X, Menu, Moon, Sun, Bell,
  Shield, Hash, Truck, ScrollText, Cpu, MemoryStick, Monitor, HardDrive, Layers,
  MessageCircle, Brain, SlidersHorizontal,
} from '@lucide/vue';

defineEmits(['addToCart', 'buyAgainUnavailable', 'goHome', 'toast']);

// ── Chat ref cho notification badge ─────────────────────────────────────────
const chatPanelRef = ref(null);

// ── Navigation ───────────────────────────────────────────────────────────────
const route = useRoute();
// Khôi phục tab đang mở từ sessionStorage
const ADMIN_PAGE_STORAGE_KEY = "admin.lastPage";
const NON_RESTORABLE_PAGES = new Set(["customer-detail", "san-pham-detail"]);
const savedPage = sessionStorage.getItem(ADMIN_PAGE_STORAGE_KEY);
const currentPage = ref(
  route.params.id
    ? "san-pham-detail"
    : (savedPage && !NON_RESTORABLE_PAGES.has(savedPage) ? savedPage : "dashboard"),
);
watch(currentPage, (page) => {
  if (page && !NON_RESTORABLE_PAGES.has(page)) sessionStorage.setItem(ADMIN_PAGE_STORAGE_KEY, page);
});
// Trạng thái mở hoặc đóng sidebar
const sidebarOpen = ref(window.matchMedia("(min-width: 768px)").matches);
const selectedCustomerId = ref(null);
const selectedSanPhamId = ref(route.params.id ? Number(route.params.id) : null);
const selectedOrderId = ref(null); // dùng để navigate từ CustomerDetailModal → OrdersTable
// Đồng bộ tab hiện tại theo route params
watch(
  () => route.params.id,
  (id) => {
    _fromRouteWatch = true;
    currentPage.value = id ? "san-pham-detail" : "dashboard";
    selectedSanPhamId.value = id ? Number(id) : null;
  },
);
const openCustomerDetail = (id) => {
  selectedCustomerId.value = id;
  currentPage.value = "customer-detail";
};
// Chuyển hướng tới chi tiết đơn hàng từ khách hàng
const handleViewOrder = (order) => {
  selectedOrderId.value = order.donHangId;
  currentPage.value = "orders";
};
const navigate = (page) => {
  currentPage.value = page;
  if (window.matchMedia("(max-width: 767.98px)").matches) sidebarOpen.value = false; // chọn xong tự đóng lại trên mobile
  if (page === "staff") { ensureChucVuList(); ensureStaff(); }
  // Làm mới dữ liệu trả hàng khi chuyển tab
  if (page === "tra-hang") refreshReturns();
  if (page === "inventory") {
    refreshInventory().catch(() => {});
    refreshProducts().catch(() => {});
  }
};
// Biểu tượng tương ứng với từng trang quản trị
const PAGE_META = {
  dashboard: { titleKey: "admin.pageMeta.dashboard.title", subKey: "admin.pageMeta.dashboard.sub", icon: BarChart3 },
  products: { titleKey: "admin.pageMeta.products.title", subKey: "admin.pageMeta.products.sub", icon: Laptop },
  "bien-the": { titleKey: "admin.pageMeta.bienThe.title", subKey: "admin.pageMeta.bienThe.sub", icon: Layers },
  "thuoc-tinh": { titleKey: "admin.pageMeta.thuocTinh.title", subKey: "admin.pageMeta.thuocTinh.sub", icon: SlidersHorizontal },
  orders: { titleKey: "admin.pageMeta.orders.title", subKey: "admin.pageMeta.orders.sub", icon: Receipt },
  customers: { titleKey: "admin.pageMeta.customers.title", subKey: "admin.pageMeta.customers.sub", icon: Users },
  "customer-detail": { titleKey: "admin.pageMeta.customerDetail.title", subKey: "admin.pageMeta.customerDetail.sub", icon: User },
  inventory: { titleKey: "admin.pageMeta.inventory.title", subKey: "admin.pageMeta.inventory.sub", icon: Package },
  "tra-hang": { titleKey: "admin.pageMeta.traHang.title", subKey: "admin.pageMeta.traHang.sub", icon: Undo2 },
  reviews: { titleKey: "admin.pageMeta.reviews.title", subKey: "admin.pageMeta.reviews.sub", icon: Star },
  promotions: { titleKey: "admin.pageMeta.promotions.title", subKey: "admin.pageMeta.promotions.sub", icon: Tag },
  "doi-thuong": { titleKey: "admin.pageMeta.doiThuong.title", subKey: "admin.pageMeta.doiThuong.sub", icon: Gift },
  staff: { titleKey: "admin.pageMeta.staff.title", subKey: "admin.pageMeta.staff.sub", icon: Briefcase },
  "ban-hang": { titleKey: "admin.pageMeta.banHang.title", subKey: "admin.pageMeta.banHang.sub", icon: ShoppingCart },
  reports: { titleKey: "admin.pageMeta.reports.title", subKey: "admin.pageMeta.reports.sub", icon: TrendingUp },
  settings: { titleKey: "admin.pageMeta.settings.title", subKey: "admin.pageMeta.settings.sub", icon: Settings },
  chat: { titleKey: "admin.pageMeta.chat.title", subKey: "admin.pageMeta.chat.sub", icon: MessageCircle },
  "ai-kien-thuc": { titleKey: "admin.pageMeta.aiKienThuc.title", subKey: "admin.pageMeta.aiKienThuc.sub", icon: Brain },
};
const topbarTitle = computed(
  () => t(PAGE_META[currentPage.value]?.titleKey ?? "admin.pageMeta.dashboard.title"),
);
const topbarSub = computed(() => t(PAGE_META[currentPage.value]?.subKey ?? ""));
const topbarIcon = computed(() => PAGE_META[currentPage.value]?.icon ?? BarChart3);

// Dữ liệu từ các store dùng chung
const products = computed(() => ProductsStore?.items ?? []);
const orders = computed(() => OrdersStore?.items ?? []);
const customers = computed(() => CustomersStore?.items ?? []);
const staff = computed(() => StaffStore?.items ?? []);
const promotions = computed(() => PromotionsStore?.items ?? []);
// Cấu hình vòng quay may mắn
const wheelConfig = ref({ diemMoiLuot: 0, tyLeTruot: 0 });
const wheelConfigSaving = ref(false);
const wheelConfigError = ref("");
const loadWheelConfig = async () => {
  try {
    const res = await VongQuayService.getCauHinh();
    wheelConfig.value = { diemMoiLuot: res.diemMoiLuot, tyLeTruot: res.tyLeTruot };
  } catch (e) {
    wheelConfigError.value = e.message || t("admin.wheelConfig.loadError");
  }
};
const saveWheelConfig = async () => {
  wheelConfigSaving.value = true;
  wheelConfigError.value = "";
  try {
    const res = await VongQuayService.capNhatCauHinh(wheelConfig.value);
    if (!res.ok) throw new Error(await res.text().catch(() => res.statusText));
    showToast(t("admin.wheelConfig.saveSuccess"), "success");
  } catch (e) {
    wheelConfigError.value = e.message || t("admin.wheelConfig.saveError");
  } finally {
    wheelConfigSaving.value = false;
  }
};
const rewards = computed(() => DoiThuongStore?.items ?? []);
const inventory = computed(() => InventoryStore?.items ?? []);
const chucVuList = ref([]);

const chucVuName = (id) =>
  chucVuList.value.find((c) => c.id === id)?.tenChucVu ?? "—";

// Đếm số sản phẩm phân biệt
const totalProducts = computed(() => groupedProducts.value.length);
const totalOrders = computed(() => orders.value.length);
// Đếm số đơn hàng phát sinh hôm nay
const todayOrdersCount = computed(
  () => orders.value.filter((o) => o.ngayDat?.slice(0, 10) === toDateInputValue(new Date())).length,
);
const totalCustomers = computed(() => customers.value.length);
const totalRevenue = computed(() =>
  orders.value.reduce((s, o) => s + (Number(o.thanhTien) || 0), 0),
);
// Doanh thu tháng này và cả năm cho thẻ KPI
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
// So sánh doanh thu tháng này với tháng trước (month-to-date)
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

// Thống kê đơn hàng theo ngày đã chọn
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
// Thống kê trạng thái đơn hàng dạng donut
const orderStatusChartData = computed(() =>
  ordersByStatusOnDate.value.map((row) => ({
    label: orderStatusLabel(row.status),
    value: row.count,
    color: orderStatusColor(row.status).text,
  }))
);

// Thống kê đơn hàng theo tuần (donut)
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
// Khoảng ngày trong tuần đã chọn
const weekChartAnchor = ref(toDateInputValue(new Date()));
const weekChartFrom = computed(() => toDateInputValue(startOfWeek(new Date(weekChartAnchor.value))));
const weekChartTo   = computed(() => toDateInputValue(endOfWeek(new Date(weekChartAnchor.value))));
const isWeekChartCurrentWeek = computed(() => weekChartAnchor.value === toDateInputValue(new Date()));
const resetToCurrentWeek = () => { weekChartAnchor.value = toDateInputValue(new Date()); };
// Nhãn hiển thị khoảng ngày trong tuần
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

// Bộ lọc thời gian cho báo cáo
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
    .then((r) => r ?? [])
    .catch(() => []);
};
watch([reportsDateFrom, reportsDateTo], loadReportsTopSelling, { immediate: true });

// Thống kê doanh thu theo ngày đã chọn
const reportsRevenueByDay = ref([]); // [{ ngay, doanhThu }], liên tục từng ngày trong khoảng
const loadReportsRevenueByDay = async () => {
  const raw = await DashboardService
    .getRevenueByDay(reportsDateFrom.value, reportsDateTo.value)
    .then((r) => r ?? [])
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

// Gom nhóm doanh thu theo ngày, tháng hoặc năm
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
    .then((r) => r ?? { topKhach: [], tyLeMuaLai: 0, tongSoKhach: 0 })
    .catch(() => ({ topKhach: [], tyLeMuaLai: 0, tongSoKhach: 0 }));
};
watch([reportsDateFrom, reportsDateTo], loadReportsCustomerReport, { immediate: true });

const reportsRepeatRateText = computed(() => {
  const c = reportsCustomerReport.value;
  const repeat = Math.round(c.tyLeMuaLai * c.tongSoKhach);
  const pct = Math.round(c.tyLeMuaLai * 100);
  return t('admin.reports.repeatRateLabel', { repeat, total: c.tongSoKhach, pct });
});

// Thống kê trạng thái đơn hàng trong kỳ báo cáo
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

// Danh sách sản phẩm bán chạy và bán chậm nhất
const topSellingRaw = ref([]); // [{ tenSanPham, soLuongDaBan }]
const slowSellingRaw = ref([]);
const fetchProductSales = async () => {
  [topSellingRaw.value, slowSellingRaw.value] = await Promise.all([
    DashboardService.getTopSelling(5).then((r) => r ?? []).catch(() => []),
    DashboardService.getSlowSelling(5).then((r) => r ?? []).catch(() => []),
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

// Các chỉ số hiệu suất KPI vận hành
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
// Tỷ lệ sản phẩm đang kinh doanh
const activeProductRatio = computed(() => {
  const total = groupedProducts.value.length;
  if (!total) return 0;
  const active = new Set(
    products.value.filter(p => p.trangThai === 'active').map(p => p.sanPhamId)
  ).size;
  return (active / total) * 100;
});

// Doanh thu từng ngày trong tuần hiện tại
const weeklyRevenueChart = computed(() => {
  const dayLabels = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];
  const sums = new Array(7).fill(0);
  ordersInWeekRange.value.forEach((o) => {
    if (!o.ngayDat) return;
    const idx = (new Date(o.ngayDat).getDay() + 6) % 7;
    sums[idx] += Number(o.thanhTien) || 0;
  });
  return dayLabels.map((label, i) => ({ label, value: sums[i] }));
});

// Bản đồ nhiệt số lượng đơn hàng theo ngày trong tháng
const monthlyOrderHeat = computed(() => {
  const now = new Date();
  const map = {};
  orders.value.forEach((o) => {
    if (!o.ngayDat) return;
    const d = new Date(o.ngayDat);
    if (d.getFullYear() !== now.getFullYear() || d.getMonth() !== now.getMonth()) return;
    map[d.getDate()] = (map[d.getDate()] || 0) + 1;
  });
  return Object.entries(map).map(([day, count]) => ({ day: Number(day), count }));
});

// Tỷ lệ vận hành cho biểu đồ Radar
const kpiRadarData = computed(() => [
  { axis: t('admin.dashboard.gaugeCompletion'), value: orderCompletionRate.value },
  { axis: t('admin.dashboard.gaugePayment'), value: paymentRate.value },
  { axis: t('admin.dashboard.gaugeStock'), value: stockHealthRate.value },
  { axis: t('admin.dashboard.activeProducts'), value: activeProductRatio.value },
]);

// Thống kê nhân viên theo chức vụ
const ROLE_COLORS = ['#7c3aed', '#f43f5e', '#22c55e', '#facc15', '#0e7490'];
const staffByRole = computed(() => {
  const map = {};
  staff.value.forEach((s) => {
    const name = chucVuName(s.chucVuId);
    map[name] = (map[name] || 0) + 1;
  });
  return Object.entries(map).map(([label, value], i) => ({
    label, value, color: ROLE_COLORS[i % ROLE_COLORS.length],
  }));
});

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
// Pending warranty claims count for sidebar badge


// Tab quản lý kho hàng và bảo hành
const INVENTORY_TAB_STORAGE_KEY = "admin.lastInventoryTab";
const inventoryMainTab = ref(sessionStorage.getItem(INVENTORY_TAB_STORAGE_KEY) || 'kho');
watch(inventoryMainTab, (tab) => sessionStorage.setItem(INVENTORY_TAB_STORAGE_KEY, tab));

const selectInventoryTab = (tab) => {
  inventoryMainTab.value = tab;
  navigate('inventory');
};


// components/admin/InventoryPanel.vue (Task 7).

// ── Fetch ─────────────────────────────────────────────────────────────────────

// Tải thông tin nhân viên và chức vụ
const fetchAll = async () => {
  await Promise.all([
    refreshProducts(),
    refreshOrders(),
    refreshCustomers(),
    refreshPromotions(),
    refreshInventory(),
    refreshDoiThuong(),
    ensureStaff(),
    ensureChucVuList(),
  ]);
  await autoMergeAllDuplicates();
};

// Danh sách chức vụ nhân viên
let chucVuListPromise = null;
const ensureChucVuList = () => {
  if (chucVuListPromise) return chucVuListPromise;
  chucVuListPromise = DmService.getChucVu().catch(() => []).then((list) => {
    chucVuList.value = list;
  });
  return chucVuListPromise;
};

// Tự động gộp các đơn hàng trùng
let isMerging = false;
const autoMergeAllDuplicates = async () => {
  if (isMerging) return;
  isMerging = true;
  try {
    const groups = {};
    for (const o of orders.value) {
      if (o.trangThaiDonHang === 'pending') continue;
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




// instance CustomerFormModal riêng của PosPanel.

// ── Staff CRUD ────────────────────────────────────────────────────────────────

// ── Promotions CRUD ───────────────────────────────────────────────────────────


// ── Rewards (Đổi thưởng) CRUD ─────────────────────────────────────────────────






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
    // Cập nhật lại dữ liệu tồn kho
    await fetchAll();
    
    showToast(t('admin.settings.applyToAllDone', { count: res.soBienTheDaCapNhat }), 'success');
  } catch (e) {
    showToast(e.message || String(e), 'error');
  } finally {
    cdApplyingThreshold.value = false;
  }
};

// Lưu cài đặt ngôn ngữ và định dạng số
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

// Kết nối sự kiện đơn hàng realtime
onMounted(async () => {
  // Tải toàn bộ dữ liệu trang quản trị
  try {
    await fetchAll();
    await fetchProductSales();
    await loadWheelConfig();
  } catch (e) {
    console.error('fetchAll/fetchProductSales lỗi khi vào trang:', e);
  }

  connectOrderEvents(AuthStore.user?.token, {
    onNewOrder: () => { autoMergeAllDuplicates(); fetchProductSales(); },
  });
  connectSerialEvents(AuthStore.user?.token);
});

onUnmounted(() => {
  disconnectOrderEvents();
  disconnectSerialEvents();
});
</script>

<template>
  <!-- Layout chính: sidebar bên trái + main content bên phải -->
  <div class="d-flex overflow-hidden" style="height:100vh; background:var(--bg-page-alt); color:var(--text-primary); font-family:'Nunito Sans',sans-serif;">
    <!-- Lớp phủ mờ phía sau sidebar khi mở trên mobile — bấm ra ngoài để đóng -->
    <div
      v-if="sidebarOpen" class="d-md-none position-fixed top-0 start-0 w-100 h-100"
      style="background:rgba(0,0,0,0.5); z-index:1039;"
      @click="sidebarOpen = false"
    ></div>

    <!-- ══════════ SIDEBAR ══════════ -->
    <aside
      class="d-flex flex-column border-end flex-shrink-0 adm-sidebar"
      :class="{ 'adm-sidebar-open': sidebarOpen }"
      style="background:var(--bg-card-inset); border-color:var(--border-color)!important; overflow-y:auto;"
    >
      <!-- Logo -->
      <div
        class="d-flex align-items-center gap-2 p-3 border-bottom adm-brand-row"
        style="border-color:var(--border-color-soft)!important;"
      >
        <!-- Logo: ưu tiên ảnh từ store, fallback circle SAO -->
        <img
          v-if="SettingsStore.logoUrl"
          :src="SettingsStore.logoUrl"
          class="rounded-circle flex-shrink-0"
          style="width:38px;height:38px;object-fit:contain;"
        />
        <div
          v-else
          class="rounded-circle d-flex align-items-center justify-content-center fw-black flex-shrink-0"
          style="width:38px;height:38px;background:var(--gradient-brand);color:var(--accent-text);font-size:0.8rem;"
        >
          SAO
        </div>
        <div class="adm-brand-text">
          <div class="fw-bold" style="font-size:0.95rem;">{{ SettingsStore.tenCuaHang || t('admin.brand.name') }}</div>
          <div style="font-size:0.7rem;color:var(--text-muted);">{{ SettingsStore.diaChi || t('admin.brand.tagline') }}</div>
        </div>
      </div>

      <!-- Nav admin -->
      <nav class="flex-grow-1 d-flex flex-column px-2 pb-2">
        <div class="adm-nav" :class="{active: currentPage==='dashboard'}" @click="navigate('dashboard')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M3 4a1 1 0 011-1h3a1 1 0 011 1v3a1 1 0 01-1 1H4a1 1 0 01-1-1V4zm7 0a1 1 0 011-1h3a1 1 0 011 1v3a1 1 0 01-1 1h-3a1 1 0 01-1-1V4zM3 11a1 1 0 011-1h3a1 1 0 011 1v3a1 1 0 01-1 1H4a1 1 0 01-1-1v-3zm7 0a1 1 0 011-1h3a1 1 0 011 1v3a1 1 0 01-1 1h-3a1 1 0 01-1-1v-3z" /></svg>
          {{ t('admin.sidebar.dashboard') }}
        </div>

        <div class="adm-nav-label">{{ t('admin.sidebar.groupOrders') }}</div>
        <div class="adm-nav adm-subnav" :class="{active: currentPage==='orders'}" @click="navigate('orders')">
          <Receipt class="adm-icon" :size="15" /> {{ t('admin.sidebar.orders') }}
          <span class="badge bg-warning text-dark ms-auto" style="font-size:0.68rem;">{{ todayOrdersCount }}</span>
        </div>
        <div class="adm-nav adm-subnav" :class="{active: currentPage==='ban-hang'}" @click="navigate('ban-hang')">
          <ShoppingCart class="adm-icon" :size="15" /> {{ t('admin.sidebar.banHang') }}
        </div>

        <div class="adm-nav-label">{{ t('admin.sidebar.products') }}</div>
        <div class="adm-nav adm-subnav" :class="{active: currentPage==='products'}" @click="navigate('products')">
          <Laptop class="adm-icon" :size="15" /> {{ t('admin.productsTabs.sanPham') }}
        </div>
        <div class="adm-nav adm-subnav" :class="{active: currentPage==='bien-the'}" @click="navigate('bien-the')">
          <Layers class="adm-icon" :size="15" /> {{ t('admin.productsTabs.bienThe') }}
        </div>
        <div class="adm-nav adm-subnav" :class="{active: currentPage==='thuoc-tinh'}" @click="navigate('thuoc-tinh')">
          <SlidersHorizontal class="adm-icon" :size="15" /> {{ t('admin.productsTabs.thuocTinh') }}
        </div>

        <div class="adm-nav-label">{{ t('admin.sidebar.groupCustomers') }}</div>
        <div class="adm-nav adm-subnav" :class="{active: currentPage==='customers' || currentPage==='customer-detail'}" @click="navigate('customers')">
          <Users class="adm-icon" :size="15" /> {{ t('admin.sidebar.customers') }}
          <span class="badge bg-warning text-dark ms-auto" style="font-size:0.68rem;">{{ totalCustomers }}</span>
        </div>
        <div class="adm-nav adm-subnav" :class="{active: currentPage==='tra-hang'}" @click="navigate('tra-hang')">
          <Undo2 class="adm-icon" :size="15" /> {{ t('admin.sidebar.traHang') }}
        </div>
        <div class="adm-nav adm-subnav" :class="{active: currentPage==='reviews'}" @click="navigate('reviews')">
          <Star class="adm-icon" :size="15" /> {{ t('admin.sidebar.reviews') }}
        </div>

        <div class="adm-nav-label">{{ t('admin.sidebar.inventory') }}</div>
        <div class="adm-nav adm-subnav" :class="{active: currentPage==='inventory' && inventoryMainTab==='kho'}" @click="selectInventoryTab('kho')">
          <Package class="adm-icon" :size="15" /> {{ t('admin.inventory.tabStock') }} / {{ t('admin.inventory.tabReceipts') }}
          <span v-if="lowStockItems.length" class="badge bg-danger ms-auto" style="font-size:0.68rem;">{{ lowStockItems.length }}</span>
        </div>
        <div class="adm-nav adm-subnav" :class="{active: currentPage==='inventory' && inventoryMainTab==='bao-hanh'}" @click="selectInventoryTab('bao-hanh')">
          <Shield class="adm-icon" :size="15" /> {{ t('admin.inventory.tabWarranty') }}
        </div>

        <div class="adm-nav adm-subnav" :class="{active: currentPage==='inventory' && inventoryMainTab==='serial'}" @click="selectInventoryTab('serial')">
          <Hash class="adm-icon" :size="15" /> {{ t('admin.inventory.tabSerial') }}
        </div>
        <div class="adm-nav adm-subnav" :class="{active: currentPage==='inventory' && inventoryMainTab==='suppliers'}" @click="selectInventoryTab('suppliers')">
          <Truck class="adm-icon" :size="15" /> {{ t('admin.sidebar.suppliers') }}
        </div>
        <div class="adm-nav adm-subnav" :class="{active: currentPage==='inventory' && inventoryMainTab==='lich-su'}" @click="selectInventoryTab('lich-su')">
          <ScrollText class="adm-icon" :size="15" /> {{ t('admin.sidebar.inventoryHistory') }}
        </div>

        <div class="adm-nav-label">{{ t('admin.sidebar.groupPromotions') }}</div>
        <div class="adm-nav adm-subnav" :class="{active: currentPage==='promotions'}" @click="navigate('promotions')">
          <Tag class="adm-icon" :size="15" /> {{ t('admin.sidebar.promotions') }}
        </div>
        <div class="adm-nav adm-subnav" :class="{active: currentPage==='doi-thuong'}" @click="navigate('doi-thuong')">
          <Gift class="adm-icon" :size="15" /> {{ t('admin.sidebar.rewards') }}
        </div>

        <div class="adm-nav-label">{{ t('admin.sidebar.groupStaff') }}</div>
        <div class="adm-nav adm-subnav" :class="{active: currentPage==='staff'}" @click="navigate('staff')">
          <Briefcase class="adm-icon" :size="15" /> {{ t('admin.sidebar.staff') }}
        </div>

        <div class="adm-nav-label">{{ t('admin.sidebar.groupAnalytics') }}</div>
        <div class="adm-nav" :class="{active: currentPage==='reports'}" @click="navigate('reports')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M2 11a1 1 0 011-1h2a1 1 0 011 1v5a1 1 0 01-1 1H3a1 1 0 01-1-1v-5zm6-4a1 1 0 011-1h2a1 1 0 011 1v9a1 1 0 01-1 1H9a1 1 0 01-1-1V7zm6-3a1 1 0 011-1h2a1 1 0 011 1v12a1 1 0 01-1 1h-2a1 1 0 01-1-1V4z" /></svg>
          {{ t('admin.sidebar.reports') }}
        </div>
        <div class="adm-nav" :class="{active: currentPage==='settings'}" @click="navigate('settings')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M11.49 3.17c-.38-1.56-2.6-1.56-2.98 0a1.532 1.532 0 01-2.286.948c-1.372-.836-2.942.734-2.106 2.106.54.886.061 2.042-.947 2.287-1.561.379-1.561 2.6 0 2.978a1.532 1.532 0 01.947 2.287c-.836 1.372.734 2.942 2.106 2.106a1.532 1.532 0 012.287.947c.379 1.561 2.6 1.561 2.978 0a1.533 1.533 0 012.287-.947c1.372.836 2.942-.734 2.106-2.106a1.533 1.533 0 01.947-2.287c1.561-.379 1.561-2.6 0-2.978a1.532 1.532 0 01-.947-2.287c.836-1.372-.734-2.942-2.106-2.106a1.532 1.532 0 01-2.287-.947zM10 13a3 3 0 100-6 3 3 0 000 6z" clip-rule="evenodd" /></svg>
          {{ t('admin.sidebar.settings') }}
        </div>

        <div class="adm-nav" :class="{active: currentPage==='chat'}" @click="navigate('chat')">
          <MessageCircle class="adm-icon" :size="15" />
          Hỗ trợ khách
          <span
            v-if="chatPanelRef?.unreadCount > 0"
            class="adm-badge"
          >{{ chatPanelRef.unreadCount > 9 ? '9+' : chatPanelRef.unreadCount }}</span>
        </div>

        <div class="adm-nav" :class="{active: currentPage==='ai-kien-thuc'}" @click="navigate('ai-kien-thuc')">
          <Brain class="adm-icon" :size="15" />
          Kiến thức AI
        </div>
      </nav>

    </aside><!-- /sidebar -->

    <!-- ══════════ MAIN CONTENT ══════════ -->
    <main class="flex-grow-1 d-flex flex-column overflow-hidden">
      <!-- Topbar: tieu de trang hien tai -->
      <div
        class="d-flex align-items-center justify-content-between p-3 border-bottom"
        style="background:var(--bg-card-inset); border-color:var(--border-color)!important;"
      >
        <div class="d-flex align-items-center gap-2">
          <button
            type="button" class="d-flex align-items-center justify-content-center rounded-2 border-0"
            style="width:34px;height:34px;background:var(--bg-hover);color:var(--text-primary);cursor:pointer;font-size:1.1rem;"
            :aria-label="t('admin.sidebar.toggleMenu')" :title="t('admin.sidebar.toggleMenu')"
            @click="sidebarOpen = !sidebarOpen"
          >
            <component :is="sidebarOpen ? X : Menu" :size="20" />
          </button>
          <div>
            <div class="fw-bold d-flex align-items-center gap-1" style="font-size:1.05rem;"><component :is="topbarIcon" :size="18" /> {{ topbarTitle }}</div>
            <div style="font-size:0.78rem;color:var(--text-muted);">{{ topbarSub }}</div>
          </div>
        </div>
        <div class="d-flex align-items-center gap-2">
          <button
            type="button" class="d-flex align-items-center justify-content-center rounded-2 border-0"
            style="width:34px;height:34px;background:var(--bg-hover);color:var(--text-primary);cursor:pointer;font-size:1rem;"
            :title="ThemeStore.mode === 'dark' ? t('theme.toggleToLight') : t('theme.toggleToDark')"
            :aria-label="ThemeStore.mode === 'dark' ? t('theme.toggleToLight') : t('theme.toggleToDark')"
            @click="toggleTheme"
          >
            <component :is="ThemeStore.mode === 'dark' ? Moon : Sun" :size="18" />
          </button>
          <div
            class="d-flex align-items-center justify-content-center rounded-2 position-relative"
            style="width:34px;height:34px;background:var(--bg-hover);cursor:pointer;display:flex;align-items:center;justify-content:center;"
            title="Hỗ trợ khách hàng"
            @click="navigate('chat')"
          >
            <MessageCircle :size="18" />
            <span
              v-if="chatPanelRef?.unreadCount > 0"
              class="position-absolute rounded-circle d-flex align-items-center justify-content-center"
              style="top:-4px;right:-4px;width:16px;height:16px;background:#ef4444;color:#fff;font-size:9px;font-weight:700;"
            >{{ chatPanelRef.unreadCount > 9 ? '9+' : chatPanelRef.unreadCount }}</span>
          </div>
          <UserProfileMenu @navigate-settings="navigate('settings')" />
        </div>
      </div>

      <!-- Noi dung trang (scroll duoc) -->
      <div class="flex-grow-1 overflow-y-auto p-4">
        <!-- ── Dashboard ── -->
        <AdminDashboard
          v-show="currentPage === 'dashboard'"
          :total-products="totalProducts"
          :total-orders="totalOrders"
          :total-customers="totalCustomers"
          :revenue-this-month="revenueThisMonth"
          :revenue-this-month-delta="revenueThisMonthDelta"
          :revenue-this-year="revenueThisYear"
          :low-stock-items="lowStockItems"
          :status-chart-date="statusChartDate"
          :is-status-chart-today="isStatusChartToday"
          :orders-on-status-chart-date="ordersOnStatusChartDate"
          :order-status-chart-data="orderStatusChartData"
          :week-chart-anchor="weekChartAnchor"
          :is-week-chart-current-week="isWeekChartCurrentWeek"
          :week-chart-range-label="weekChartRangeLabel"
          :orders-in-week-range="ordersInWeekRange"
          :week-order-status-chart-data="weekOrderStatusChartData"
          :top-selling-chart="topSellingChart"
          :slow-selling-chart="slowSellingChart"
          :order-completion-rate="orderCompletionRate"
          :payment-rate="paymentRate"
          :stock-health-rate="stockHealthRate"
          :revenue-trend-chart="revenueTrendChart"
          :active-product-ratio="activeProductRatio"
          :weekly-revenue-chart="weeklyRevenueChart"
          :monthly-order-heat="monthlyOrderHeat"
          :kpi-radar-data="kpiRadarData"
          :staff-by-role="staffByRole"
          :products="products"
          @update:status-chart-date="statusChartDate = $event"
          @update:week-chart-anchor="weekChartAnchor = $event"
          @reset-to-current-week="resetToCurrentWeek"
          @back-to-today="statusChartDate = toDateInputValue(new Date())"
        />

        <!-- ── San pham ── -->
        <section v-show="currentPage === 'products'">
          <HangHoa />
        </section>

        <!-- ── Bien the: tat ca bien the cua moi san pham (khong loc theo 1 san pham) ── -->
        <section v-show="currentPage === 'bien-the'">
          <BienTheTable />
        </section>

        <!-- ── Thuoc tinh san pham (mau sac, man hinh, pin,...) ── -->
        <section v-show="currentPage === 'thuoc-tinh'">
          <ThuocTinhPanel />
        </section>

        <!-- ── Don hang ── -->
        <section v-show="currentPage === 'orders'">
          <OrdersTable
            :navigate-to-order-id="selectedOrderId"
            @order-detail-opened="selectedOrderId = null"
          />
        </section>

        <!-- ── Khach hang ── -->
        <section v-show="currentPage === 'customers'">
          <CustomersTable
            @view-detail="openCustomerDetail"
            @view-order="handleViewOrder"
          />
        </section>

        <!-- ── Chi tiet khach hang ── -->
        <section v-show="currentPage === 'customer-detail'">
          <CustomerDetailPage v-if="selectedCustomerId" :key="selectedCustomerId" :customer-id="selectedCustomerId" @back="() => { currentPage = 'customers'; selectedCustomerId = null; }" />
        </section>

        <!-- ── Chi tiet san pham (mo qua tab moi, xem ProductsTable.vue openDetail) ── -->
        <section v-show="currentPage === 'san-pham-detail'">
          <SanPhamDetailPage v-if="selectedSanPhamId" :key="selectedSanPhamId" :san-pham-id="selectedSanPhamId" />
        </section>

        <!-- ── Kho hang: dieu huong qua submenu sidebar (adm-subnav), khong con thanh tab ngang ── -->
        <section v-show="currentPage === 'inventory'">
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

          <!-- Quản lý nhà cung cấp, lịch sử tồn kho, linh kiện -->
          <div v-show="inventoryMainTab==='suppliers'">
            <SupplierManager />
          </div>
          <div v-show="inventoryMainTab==='lich-su'">
            <InventoryHistoryPanel />
          </div>
        </section>

        <section v-show="currentPage === 'tra-hang'"><ReturnsPanel :can-pick-staff="true" /></section>

        <!-- ── Danh gia san pham (kiem duyet) ── -->
        <section v-show="currentPage === 'reviews'"><DanhGiaPanel /></section>

        <!-- ── Khuyen mai ── -->
        <section v-show="currentPage === 'promotions'">
          <PromotionsPanel />
        </section>

        <!-- ── Doi thuong ── -->
        <section v-show="currentPage === 'doi-thuong'">
          <RewardsPanel />
        </section>

        <!-- ── Nhan vien ── -->
        <section v-show="currentPage === 'staff'">
          <StaffTable />
        </section>

        <!-- ── Bao cao ── -->
        <AdminReports
          v-show="currentPage === 'reports'"
          :total-revenue="totalRevenue"
          :active-products="activeProducts"
          :active-promos="activePromos"
          :low-stock-items="lowStockItems"
          :reports-group-by="reportsGroupBy"
          :reports-date-range="reportsDateRange"
          :reports-custom-from="reportsCustomFrom"
          :reports-custom-to="reportsCustomTo"
          :reports-revenue-chart-data="reportsRevenueChartData"
          :reports-orders-by-status="reportsOrdersByStatus"
          :reports-top-selling="reportsTopSelling"
          :reports-customer-report="reportsCustomerReport"
          :reports-repeat-rate-text="reportsRepeatRateText"
          @update:reports-group-by="reportsGroupBy = $event"
          @update:reports-date-range="reportsDateRange = $event"
          @update:reports-custom-from="reportsCustomFrom = $event"
          @update:reports-custom-to="reportsCustomTo = $event"
        />

        <!-- ── Cai dat ── -->
        <AdminSettings
          v-show="currentPage === 'settings'"
          :cd-mat-khau-cu="cdMatKhauCu"
          :cd-mat-khau-moi="cdMatKhauMoi"
          :cd-mat-khau-xac-nhan="cdMatKhauXacNhan"
          :cd-mat-khau-error="cdMatKhauError"
          :cd-mat-khau-success="cdMatKhauSuccess"
          :cd-mat-khau-loading="cdMatKhauLoading"
          :cd-form="cdForm"
          :cd-logo-preview="cdLogoPreview"
          :cd-store-error="cdStoreError"
          :cd-store-saved="cdStoreSaved"
          :cd-store-saving="cdStoreSaving"
          :cd-nguong-ton-kho="cdNguongTonKho"
          :cd-applying-threshold="cdApplyingThreshold"
          @update:cd-mat-khau-cu="cdMatKhauCu = $event"
          @update:cd-mat-khau-moi="cdMatKhauMoi = $event"
          @update:cd-mat-khau-xac-nhan="cdMatKhauXacNhan = $event"
          @update:cd-nguong-ton-kho="cdNguongTonKho = $event"
          @change-password="doiMatKhauSubmit"
          @handle-logo-file="handleLogoFile"
          @save-store="saveStoreInfo"
          @apply-threshold="apDungNguongTonKhoSubmit"
          @save-appearance="saveAppearancePrefs"
        />

        <!-- ── Ban hang (POS) ── -->
        <section v-show="currentPage === 'ban-hang'">
          <PosPanel />
        </section>

        <!-- ── Chat ho tro ── -->
        <AdminChatPanel
          v-show="currentPage === 'chat'"
          ref="chatPanelRef"
        />

        <!-- ── Kiến thức AI ── -->
        <AiKienThucPanel
          v-show="currentPage === 'ai-kien-thuc'"
        />
      </div><!-- /content -->
    </main>
  </div><!-- /dashboard-shell -->

  <!-- Nhan Vien Modal has been moved to StaffTable.vue -->

  <!-- Khuyen Mai Modal has been moved to PromotionsPanel.vue -->
  <!-- Doi Thuong Modal has been moved to RewardsPanel.vue -->

  <!-- Dialog xác nhận và thông báo -->
  <ConfirmDialog />
  <ToastHost />
</template>

<style scoped>
/* Trạng thái nav item */
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

/* Mục con trong sidebar */
.adm-subnav { padding-left: 30px; font-size: 0.82rem; }

/* Tieu de phan nhom trong sidebar */
.adm-nav-label {
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--text-primary);
  opacity: 0.85;
  text-transform: uppercase;
  padding: 12px 8px 4px;
}

/* Điều khiển ẩn hiện sidebar */
.adm-sidebar {
  width: 240px;
  overflow: hidden;
  transition: width 0.2s ease;
}
.adm-sidebar:not(.adm-sidebar-open) { width: 60px; }

/* Trạng thái rail thu gọn thanh bên trên desktop */
.adm-sidebar:not(.adm-sidebar-open) .adm-nav {
  font-size: 0;
  justify-content: center;
  padding-left: 6px;
  padding-right: 6px;
}
.adm-sidebar:not(.adm-sidebar-open) .adm-nav .badge,
.adm-sidebar:not(.adm-sidebar-open) .adm-nav-label {
  display: none;
}
.adm-sidebar:not(.adm-sidebar-open) .adm-brand-row {
  justify-content: center;
  padding-left: 6px;
  padding-right: 6px;
}
.adm-sidebar:not(.adm-sidebar-open) .adm-brand-text,
.adm-sidebar:not(.adm-sidebar-open) :deep(.adm-sidebar-footer) {
  display: none;
}

@media (max-width: 767.98px) {
  .adm-sidebar {
    position: fixed;
    top: 0;
    left: 0;
    height: 100vh;
    z-index: 1040;
    width: 240px !important;
    transform: translateX(-100%);
    transition: transform 0.2s ease;
  }
  .adm-sidebar.adm-sidebar-open { transform: translateX(0); }
}

</style>
