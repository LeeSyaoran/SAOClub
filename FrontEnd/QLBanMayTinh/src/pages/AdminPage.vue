<script setup>
import { ref, computed, onMounted, onUnmounted, reactive, watch } from "vue";
import { AuthStore, clearSession } from "../stores/index.js";
import { t } from "../i18n/index.js";
import { orderStatusLabel, orderStatusColor, orderStatusIcon, paymentStatusLabel, paymentStatusColor, paymentStatusIcon } from "../utils/orderStatus.js";
import { nowLocalIso } from "../utils/datetime.js";
import * as SanPhamService   from "../Service/SanPhamService.js";
import * as BienTheSanPhamService from "../Service/BienTheSanPhamService.js";
import * as KhachHangService from "../Service/KhachHangService.js";
import * as NhanVienService  from "../Service/NhanVienService.js";
import * as DonHangService   from "../Service/DonHangService.js";
import * as KhuyenMaiService from "../Service/KhuyenMaiService.js";
import * as TonKhoService          from "../Service/TonKhoService.js";
import * as DanhMucService         from "../Service/DanhMucService.js";
import * as DmService              from "../Service/DmService.js";
import * as ChiTietSanPhamService  from "../Service/ChiTietSanPhamService.js";
import * as ChiTietDonHangService  from "../Service/ChiTietDonHangService.js";
import * as ChiTietDonHangSerialService from "../Service/ChiTietDonHangSerialService.js";
import * as PhieuNhapKhoService    from "../Service/PhieuNhapKhoService.js";
import * as ChiTietPhieuNhapService from "../Service/ChiTietPhieuNhapService.js";
import * as DashboardService       from "../Service/DashboardService.js";
import * as XLSX from "xlsx";
import DonutChart from "../components/common/DonutChart.vue";
import RevenueBarChart from "../components/common/RevenueBarChart.vue";
import BarChart   from "../components/common/BarChart.vue";
import GaugeChart from "../components/common/GaugeChart.vue";
import TrendChart from "../components/common/TrendChart.vue";
import ConfirmDialog from "../components/common/ConfirmDialog.vue";
import SearchSelect from "../components/common/SearchSelect.vue";
import { askConfirm } from "../stores/confirm.js";
import { ThemeStore, toggleTheme } from "../stores/theme.js";
import { authHeaders } from "../Service/api.js";

// ── Toast thông báo (thay window.alert()) ──────────────────────────────────
const toast = reactive({ show: false, msg: '', type: 'success' });
let toastTimer = null;
const showToast = (msg, type = 'error') => {
  clearTimeout(toastTimer);
  toast.msg  = msg;
  toast.type = type;
  toast.show = true;
  // Lỗi (đặc biệt lý do chặn xóa) thường dài hơn — cho thêm thời gian đọc so với thông báo
  // thành công ngắn gọn.
  toastTimer = setTimeout(() => { toast.show = false; }, type === 'error' ? 6000 : 3500);
};

// ── Navigation ───────────────────────────────────────────────────────────────
const currentRole = ref("admin");
const currentPage = ref("dashboard");
const navigate = (page) => {
  currentPage.value = page;
  if (page === "staff") { ensureChucVuList(); ensureStaffData(); }
};
const switchRole = (role) => {
  currentRole.value = role;
  currentPage.value = role === "admin" ? "dashboard" : "user-home";
};
const PAGE_META = {
  dashboard: { titleKey: "admin.pageMeta.dashboard.title", subKey: "admin.pageMeta.dashboard.sub" },
  products: { titleKey: "admin.pageMeta.products.title", subKey: "admin.pageMeta.products.sub" },
  orders: { titleKey: "admin.pageMeta.orders.title", subKey: "admin.pageMeta.orders.sub" },
  customers: { titleKey: "admin.pageMeta.customers.title", subKey: "admin.pageMeta.customers.sub" },
  inventory: { titleKey: "admin.pageMeta.inventory.title", subKey: "admin.pageMeta.inventory.sub" },
  promotions: { titleKey: "admin.pageMeta.promotions.title", subKey: "admin.pageMeta.promotions.sub" },
  staff: { titleKey: "admin.pageMeta.staff.title", subKey: "admin.pageMeta.staff.sub" },
  "ban-hang": { titleKey: "admin.pageMeta.banHang.title", subKey: "admin.pageMeta.banHang.sub" },
  reports: { titleKey: "admin.pageMeta.reports.title", subKey: "admin.pageMeta.reports.sub" },
  settings: { titleKey: "admin.pageMeta.settings.title", subKey: "admin.pageMeta.settings.sub" },
  "user-home": { titleKey: "admin.pageMeta.userHome.title", subKey: "admin.pageMeta.userHome.sub" },
  "user-orders": { titleKey: "admin.pageMeta.userOrders.title", subKey: "admin.pageMeta.userOrders.sub" },
  "user-browse": { titleKey: "admin.pageMeta.userBrowse.title", subKey: "admin.pageMeta.userBrowse.sub" },
  "user-warranty": { titleKey: "admin.pageMeta.userWarranty.title", subKey: "admin.pageMeta.userWarranty.sub" },
  "user-profile": { titleKey: "admin.pageMeta.userProfile.title", subKey: "admin.pageMeta.userProfile.sub" },
};
const topbarTitle = computed(
  () => t(PAGE_META[currentPage.value]?.titleKey ?? "admin.pageMeta.dashboard.title"),
);
const topbarSub = computed(() => t(PAGE_META[currentPage.value]?.subKey ?? ""));

// ── User ─────────────────────────────────────────────────────────────────────
const userDisplayName = computed(() => AuthStore.user?.hoTen ?? AuthStore.user?.username ?? "Admin");
const userAvatar = computed(() => userDisplayName.value.charAt(0).toUpperCase());
const userDisplayRole = computed(() => {
  const role = AuthStore.user?.role;
  if (role === "admin")     return t("admin.userRole.admin");
  if (role === "nhan_vien") return t("admin.userRole.nhanVien");
  if (role === "quan_kho")  return t("admin.userRole.quanKho");
  return t("admin.userRole.guest");
});

const logout = () => {
  clearSession();
  window.location.hash = '';
};

// ── Data refs ─────────────────────────────────────────────────────────────────
const products = ref([]);
const orders = ref([]);
const customers = ref([]);
const staff = ref([]);
const promotions = ref([]);
const inventory = ref([]);
const phieuNhapList = ref([]);
const chiTietPhieuNhapList = ref([]);
const categories = ref([]);
const brands = ref([]);
const suppliers = ref([]);
const chucVuList = ref([]);
const cpuList = ref([]);
const ramList = ref([]);
const oCungList = ref([]);
const gpuList = ref([]);
const loading = ref(false);

// ── Helpers ───────────────────────────────────────────────────────────────────
const statusLabel = (s) => t(`admin.statusLabel.${s}`);

const formatPrice = (v) =>
  v == null
    ? "—"
    : new Intl.NumberFormat("vi-VN", {
        style: "currency",
        currency: "VND",
      }).format(v);

const formatDate = (d) => {
  if (!d) return "—";
  try {
    return new Date(d).toLocaleDateString("vi-VN");
  } catch {
    return d;
  }
};

// Ngày + giờ (khác formatDate — chỉ có ngày) — dùng cho ngày giao dự kiến/thực tế,
// vì admin cần biết cả mốc giờ, không chỉ ngày.
const formatDateTime = (d) => {
  if (!d) return "—";
  try {
    return new Date(d).toLocaleString("vi-VN");
  } catch {
    return d;
  }
};

const toLocalDT = (s) =>
  s ? (s.length === 16 ? s + ":00" : s.slice(0, 19)) : null;

const customerName = (id) =>
  customers.value.find((c) => c.khachHangId === id)?.hoTen ?? `KH#${id}`;
const chucVuName = (id) =>
  chucVuList.value.find((c) => c.id === id)?.tenChucVu ?? "—";

// ── Bo loc man hinh Don hang ──────────────────────────────────────────────────
const orderSearch = ref("");
const orderStatusFilter = ref("");
const orderPaymentFilter = ref("");

// Chế độ xem đơn hàng: 'today' = mặc định chỉ đơn hôm nay, 'history-dates' = danh
// sách các ngày có đơn (để xem lịch sử), 'history-day' = đơn của 1 ngày cụ thể đã chọn.
const orderViewMode = ref('today');
const historySelectedDate = ref(null); // 'YYYY-MM-DD'

// Danh sách đơn hàng làm nền cho bộ lọc bên dưới, tùy theo chế độ xem đang chọn.
const ordersBaseList = computed(() => {
  if (orderViewMode.value === 'history-day' && historySelectedDate.value) {
    return orders.value.filter((o) => o.ngayDat?.slice(0, 10) === historySelectedDate.value);
  }
  if (orderViewMode.value === 'today') {
    return orders.value.filter((o) => o.ngayDat?.slice(0, 10) === toDateInputValue(new Date()));
  }
  return orders.value;
});

const filteredOrders = computed(() => {
  const q = orderSearch.value.trim().toLowerCase();
  return ordersBaseList.value.filter((o) => {
    if (orderStatusFilter.value && o.trangThaiDonHang !== orderStatusFilter.value) return false;
    if (orderPaymentFilter.value && o.trangThaiThanhToan !== orderPaymentFilter.value) return false;
    if (!q) return true;
    const name = customerName(o.khachHangId).toLowerCase();
    return String(o.donHangId).includes(q) || name.includes(q) || (o.nguoiNhan ?? '').toLowerCase().includes(q) || (o.sdtNguoiNhan ?? '').includes(q);
  });
});

// Danh sách ngày có đơn hàng (mới nhất trước), dùng cho màn "Lịch sử đơn hàng"
const VN_WEEKDAYS = ['Chủ nhật', 'Thứ hai', 'Thứ ba', 'Thứ tư', 'Thứ năm', 'Thứ sáu', 'Thứ bảy'];
const formatDateHeading = (dateKey) => {
  const [y, m, d] = dateKey.split('-').map(Number);
  const dt = new Date(y, m - 1, d);
  return `${VN_WEEKDAYS[dt.getDay()]}, ${String(d).padStart(2, '0')}/${String(m).padStart(2, '0')}/${y}`;
};
const orderDatesGrouped = computed(() => {
  const map = {};
  orders.value.forEach((o) => {
    const key = o.ngayDat?.slice(0, 10);
    if (key) map[key] = (map[key] || 0) + 1;
  });
  return Object.keys(map)
    .sort((a, b) => b.localeCompare(a))
    .map((key) => ({ dateKey: key, label: formatDateHeading(key), count: map[key] }));
});

const openOrderHistory = () => { orderViewMode.value = 'history-dates'; };
const openHistoryDay = (dateKey) => { historySelectedDate.value = dateKey; orderViewMode.value = 'history-day'; };
const backToToday = () => { orderViewMode.value = 'today'; historySelectedDate.value = null; };
const backToDateList = () => { orderViewMode.value = 'history-dates'; historySelectedDate.value = null; };

// ── Bo loc man hinh Khach hang ────────────────────────────────────────────────
const customerSearch = ref("");
const filteredCustomers = computed(() => {
  const q = customerSearch.value.trim().toLowerCase();
  if (!q) return customers.value;
  return customers.value.filter((c) =>
    (c.hoTen ?? '').toLowerCase().includes(q) ||
    (c.soDienThoai ?? '').includes(q) ||
    (c.email ?? '').toLowerCase().includes(q)
  );
});

// ── Bo loc man hinh San pham ──────────────────────────────────────────────────
const productSearch = ref("");
const filteredGroupedProducts = computed(() => {
  const q = productSearch.value.trim().toLowerCase();
  if (!q) return groupedProducts.value;
  return groupedProducts.value.filter((p) =>
    (p.tenSanPham ?? '').toLowerCase().includes(q) ||
    (p.tenThuongHieu ?? '').toLowerCase().includes(q)
  );
});

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

// So sánh doanh thu tháng gần nhất với tháng trước đó (mũi tên xu hướng trên the KPI)
const revenueTrendDelta = computed(() => {
  const pts = revenueTrendChart.value;
  if (pts.length < 2) return null;
  const prev = pts[pts.length - 2].value;
  const curr = pts[pts.length - 1].value;
  if (prev === 0) return null;
  return Math.round(((curr - prev) / prev) * 100);
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
// "Sắp hết" (khác Hết hàng): còn hàng nhưng <= tối thiểu
const lowStockOnlyItems = computed(() =>
  inventory.value.filter(t => (t.soLuongTon ?? 0) > 0 && t.tonKhoToiThieu != null && t.soLuongTon <= t.tonKhoToiThieu),
);
const totalStockQty = computed(() => inventory.value.reduce((s, i) => s + (i.soLuongTon || 0), 0));

// ── Inventory grouped by product ──────────────────────────────────────────────
const khoTab = ref('ton-kho'); // 'ton-kho' | 'phieu-nhap' | 'bao-hanh'

// ── Bảo hành: serial đã bán còn trong hạn (server tự lọc, hết hạn tự rớt khỏi danh sách) ──
const warrantyList = ref([]);
const warrantyLoading = ref(false);
const warrantySearch = ref('');
let warrantyPromise = null;
const ensureWarrantyData = (force = false) => {
  if (warrantyPromise && !force) return warrantyPromise;
  warrantyLoading.value = true;
  warrantyPromise = ChiTietSanPhamService.getUnderWarranty().catch(() => []).then((list) => {
    warrantyList.value = list;
    warrantyLoading.value = false;
  });
  return warrantyPromise;
};
const filteredWarranty = computed(() => {
  const q = warrantySearch.value.trim().toLowerCase();
  if (!q) return warrantyList.value;
  return warrantyList.value.filter((w) =>
    [w.soSerial, w.maSku, w.tenSanPham, w.maDonHang, w.tenKhachHang, w.soDienThoaiKhachHang]
      .some((v) => (v || '').toLowerCase().includes(q)));
});
const daysUntilExpiry = (isoDate) => Math.ceil((new Date(isoDate) - new Date()) / 86400000);
const inventorySearch = ref('');
const inventoryStatusFilter = ref('all'); // all | out | low | ok
const expandedGroups = ref({});
const toggleGroup = (name) => { expandedGroups.value[name] = !expandedGroups.value[name]; };
const allGroupsExpanded = computed(() =>
  inventoryGrouped.value.length > 0 && inventoryGrouped.value.every(g => expandedGroups.value[g.name]),
);
const toggleAllGroups = () => {
  const next = !allGroupsExpanded.value;
  inventoryGrouped.value.forEach(g => { expandedGroups.value[g.name] = next; });
};

const inventoryGrouped = computed(() => {
  const groups = {};
  for (const item of inventory.value) {
    const name = item.bienThe?.sanPham?.tenSanPham || '—';
    if (!groups[name]) groups[name] = [];
    groups[name].push(item);
  }
  return Object.entries(groups)
    .filter(([name]) => !inventorySearch.value || name.toLowerCase().includes(inventorySearch.value.toLowerCase()))
    .map(([name, items]) => {
      const p = products.value.find(p => p.tenSanPham === name);
      const totalTon = items.reduce((s, i) => s + (i.soLuongTon || 0), 0);
      const outCount = items.filter(i => (i.soLuongTon ?? 0) === 0).length;
      const lowCount = items.filter(i => i.soLuongTon != null && i.tonKhoToiThieu != null && i.soLuongTon > 0 && i.soLuongTon <= i.tonKhoToiThieu).length;
      return { name, items, hinhAnh: p?.hinhAnhChinh, thuongHieu: p?.thuongHieu, totalTon, outCount, lowCount };
    })
    .filter(g => {
      if (inventoryStatusFilter.value === 'out') return g.outCount > 0;
      if (inventoryStatusFilter.value === 'low') return g.lowCount > 0;
      if (inventoryStatusFilter.value === 'ok') return g.outCount === 0 && g.lowCount === 0;
      return true;
    });
});

const getVariantInfo = (item) => products.value.find(p => p.bienTheId === item.bienThe?.bienTheId);
const stockClass = (item) => {
  if ((item.soLuongTon ?? 0) === 0) return 'text-danger';
  if (item.soLuongTon != null && item.tonKhoToiThieu != null && item.soLuongTon <= item.tonKhoToiThieu) return 'text-warning';
  return 'text-success';
};

// ── Phieu nhap kho ───────────────────────────────────────────────────────────
let phieuNhapDataPromise = null;
const ensurePhieuNhapData = () => {
  if (phieuNhapDataPromise) return phieuNhapDataPromise;
  phieuNhapDataPromise = Promise.all([
    PhieuNhapKhoService.getAll().catch(() => []),
    ChiTietPhieuNhapService.getAll().catch(() => []),
    ensureProductRefData(),
    ensureStaffData(),
  ]).then(([pn, ct]) => {
    phieuNhapList.value = pn;
    chiTietPhieuNhapList.value = ct;
  });
  return phieuNhapDataPromise;
};

const supplierName = (id) => suppliers.value.find(s => s.nhaCungCapId === id)?.tenNhaCungCap ?? '—';
const staffName = (id) => staff.value.find(s => s.nhanVienId === id)?.hoTen ?? '—';

// Cùng tông màu với orderStatusColor() (utils/orderStatus.js) — dùng lại đúng hex
// cho vàng/xanh lá/đỏ để nhất quán trạng thái trên toàn app.
const phieuNhapStatusColor = (s) => {
  if (s === 'hoan_thanh') return { bg: 'rgba(34,197,94,0.15)',  text: '#22c55e' };
  if (s === 'huy')        return { bg: 'rgba(239,68,68,0.15)',  text: '#f87171' };
  return                         { bg: 'rgba(250,204,21,0.15)', text: '#facc15' }; // cho_duyet
};
const phieuNhapStatusIcon = (s) => (s === 'hoan_thanh' ? '✅' : s === 'huy' ? '❌' : '⏳');

const phieuNhapCounts = computed(() => ({
  total: phieuNhapList.value.length,
  choDuyet: phieuNhapList.value.filter(p => p.trangThai === 'cho_duyet').length,
  hoanThanh: phieuNhapList.value.filter(p => p.trangThai === 'hoan_thanh').length,
  huy: phieuNhapList.value.filter(p => p.trangThai === 'huy').length,
}));

const phieuNhapSearch = ref('');
const phieuNhapStatusFilter = ref('');
const filteredPhieuNhap = computed(() =>
  phieuNhapList.value
    .filter(p => !phieuNhapSearch.value || (p.maPhieuNhap ?? '').toLowerCase().includes(phieuNhapSearch.value.toLowerCase()))
    .filter(p => !phieuNhapStatusFilter.value || p.trangThai === phieuNhapStatusFilter.value)
    .sort((a, b) => new Date(b.ngayNhap) - new Date(a.ngayNhap)),
);

// San pham + bien the de chon khi tao dong phieu nhap — lay tu ton kho (da co san, khoi tai them)
// Options dang {value,label} de dung truc tiep voi SearchSelect.
const productOptionsForPhieuNhap = computed(() => {
  const map = new Map();
  for (const item of inventory.value) {
    const sp = item.bienThe?.sanPham;
    if (sp?.sanPhamId != null && !map.has(sp.sanPhamId)) {
      map.set(sp.sanPhamId, { value: sp.sanPhamId, label: sp.tenSanPham ?? '' });
    }
  }
  return [...map.values()];
});
const variantOptionsByProduct = computed(() => {
  const map = new Map();
  for (const item of inventory.value) {
    const bt = item.bienThe;
    const sanPhamId = bt?.sanPham?.sanPhamId;
    if (sanPhamId == null || bt?.bienTheId == null) continue;
    if (!map.has(sanPhamId)) map.set(sanPhamId, new Map());
    const variants = map.get(sanPhamId);
    if (!variants.has(bt.bienTheId)) {
      const specs = [bt.mauSac, bt.cpu?.tenCpu, bt.ram?.dungLuong].filter(Boolean).join(' · ');
      variants.set(bt.bienTheId, { value: bt.bienTheId, label: specs ? `${bt.maSku} — ${specs}` : bt.maSku });
    }
  }
  return map;
});
const variantsForProduct = (sanPhamId) => [...(variantOptionsByProduct.value.get(Number(sanPhamId)) ?? new Map()).values()];
const supplierOptions = computed(() => suppliers.value.map(s => ({ value: s.nhaCungCapId, label: s.tenNhaCungCap })));
const staffOptions = computed(() => staff.value.map(s => ({ value: s.nhanVienId, label: s.hoTen })));

const showPhieuNhapModal = ref(false);
const phieuNhapFormError = ref('');
const emptyPhieuNhapForm = () => {
  const now = new Date();
  const local = nowLocalIso(now).slice(0, 16);
  return {
    nhaCungCapId: '',
    nhanVienId: '',
    ngayNhap: local,
    ghiChu: '',
    items: [{ sanPhamId: '', bienTheId: '', soLuong: 1, donGia: 0 }],
  };
};
const phieuNhapForm = reactive(emptyPhieuNhapForm());
const phieuNhapItemsTotal = computed(() =>
  phieuNhapForm.items.reduce((s, i) => s + (Number(i.soLuong) || 0) * (Number(i.donGia) || 0), 0),
);
const addPhieuNhapItemRow = () => phieuNhapForm.items.push({ sanPhamId: '', bienTheId: '', soLuong: 1, donGia: 0 });
const removePhieuNhapItemRow = (idx) => {
  if (phieuNhapForm.items.length > 1) {
    phieuNhapForm.items.splice(idx, 1);
  } else {
    // Chỉ còn 1 dòng — không xóa hẳn (form sẽ trống hoàn toàn), reset về giá trị rỗng.
    phieuNhapForm.items[idx] = { sanPhamId: '', bienTheId: '', soLuong: 1, donGia: 0 };
  }
};
const editingPhieuNhapId = ref(null);
const openAddPhieuNhap = () => {
  editingPhieuNhapId.value = null;
  Object.assign(phieuNhapForm, emptyPhieuNhapForm());
  phieuNhapFormError.value = '';
  showPhieuNhapModal.value = true;
};
// Chỉ sửa được khi còn "cho_duyet" — đã duyệt/hủy thì coi như chốt sổ, sửa lại sẽ sai đối
// chiếu với NCC. Nạp lại đúng dữ liệu đang có: header + từng dòng chi tiết (kèm id để
// savePhieuNhap() biết dòng nào update, dòng nào tạo mới/xóa khi lưu).
const openEditPhieuNhap = (p) => {
  editingPhieuNhapId.value = p.phieuNhapId;
  const bienTheToSanPham = new Map(products.value.map(pp => [pp.bienTheId, pp.sanPhamId]));
  const items = chiTietPhieuNhapList.value
    .filter(c => c.phieuNhapId === p.phieuNhapId)
    .map(c => ({
      id: c.id,
      sanPhamId: bienTheToSanPham.get(c.bienTheId) ?? '',
      bienTheId: c.bienTheId,
      soLuong: c.soLuong,
      donGia: c.donGiaNhap,
    }));
  Object.assign(phieuNhapForm, {
    nhaCungCapId: p.nhaCungCapId,
    nhanVienId: p.nhanVienId,
    ngayNhap: (p.ngayNhap || '').slice(0, 16),
    ghiChu: p.ghiChu === '—' ? '' : (p.ghiChu || ''),
    items: items.length ? items : [{ sanPhamId: '', bienTheId: '', soLuong: 1, donGia: 0 }],
  });
  phieuNhapFormError.value = '';
  showPhieuNhapModal.value = true;
};
const savePhieuNhap = async () => {
  phieuNhapFormError.value = '';
  if (!phieuNhapForm.nhaCungCapId || !phieuNhapForm.nhanVienId) {
    phieuNhapFormError.value = t('admin.phieuNhapModal.missingRequired');
    return;
  }
  const items = phieuNhapForm.items.filter(i => i.bienTheId);
  if (items.length === 0) {
    phieuNhapFormError.value = t('admin.phieuNhapModal.missingItems');
    return;
  }
  try {
    const headerBody = {
      nhaCungCapId: Number(phieuNhapForm.nhaCungCapId),
      nhanVienId: Number(phieuNhapForm.nhanVienId),
      ngayNhap: toLocalDT(phieuNhapForm.ngayNhap),
      tongTien: phieuNhapItemsTotal.value,
      trangThai: 'cho_duyet',
      ghiChu: phieuNhapForm.ghiChu || '—',
    };
    const res = await PhieuNhapKhoService.save(editingPhieuNhapId.value, headerBody);
    if (!res.ok) {
      phieuNhapFormError.value = t('admin.errors.saveFailedWithText', { status: res.status, text: await res.text() });
      return;
    }
    if (editingPhieuNhapId.value) {
      // Đối chiếu dòng cũ/mới: id có sẵn -> update, không có id -> tạo mới,
      // dòng cũ không còn trong form -> xóa.
      const phieuNhapId = editingPhieuNhapId.value;
      const originalIds = chiTietPhieuNhapList.value
        .filter(c => c.phieuNhapId === phieuNhapId).map(c => c.id);
      const keptIds = items.filter(i => i.id).map(i => i.id);
      for (const oldId of originalIds.filter(id => !keptIds.includes(id))) {
        await ChiTietPhieuNhapService.remove(oldId);
      }
      for (const i of items) {
        const body = {
          phieuNhapId, bienTheId: Number(i.bienTheId),
          soLuong: Number(i.soLuong) || 0, donGiaNhap: Number(i.donGia) || 0,
        };
        if (i.id) await ChiTietPhieuNhapService.update(i.id, body);
        else await ChiTietPhieuNhapService.create(body);
      }
    } else {
      const created = await res.json();
      for (const i of items) {
        await ChiTietPhieuNhapService.create({
          phieuNhapId: created.phieuNhapId,
          bienTheId: Number(i.bienTheId),
          soLuong: Number(i.soLuong) || 0,
          donGiaNhap: Number(i.donGia) || 0,
        });
      }
    }
    // API tạo trả về entity lồng nhau (nhaCungCap/nhanVien object) khác format phẳng của
    // getAll() (PhieuNhapKhoResponse) — tải lại danh sách thay vì tự ráp để tránh lệch dữ liệu.
    [phieuNhapList.value, chiTietPhieuNhapList.value] = await Promise.all([
      PhieuNhapKhoService.getAll().catch(() => phieuNhapList.value),
      ChiTietPhieuNhapService.getAll().catch(() => chiTietPhieuNhapList.value),
    ]);
    showPhieuNhapModal.value = false;
  } catch (e) {
    phieuNhapFormError.value = e.message;
  }
};
const deletePhieuNhap = async (id) => {
  if (!(await askConfirm(t('admin.confirm.deletePhieuNhap')))) return;
  const res = await PhieuNhapKhoService.remove(id);
  if (!res.ok) { showToast(await res.text().catch(() => t('admin.errors.deleteFailed', { status: res.status }))); return; }
  phieuNhapList.value = phieuNhapList.value.filter(p => p.phieuNhapId !== id);
  chiTietPhieuNhapList.value = chiTietPhieuNhapList.value.filter(c => c.phieuNhapId !== id);
};

const updatePhieuNhapStatus = async (p, trangThai) => {
  const res = await PhieuNhapKhoService.save(p.phieuNhapId, {
    nhaCungCapId: p.nhaCungCapId,
    nhanVienId: p.nhanVienId,
    ngayNhap: p.ngayNhap,
    tongTien: p.tongTien,
    trangThai,
    ghiChu: p.ghiChu,
  });
  if (!res.ok) { showToast(t('admin.errors.updateFailed', { status: res.status })); return; }
  const idx = phieuNhapList.value.findIndex(x => x.phieuNhapId === p.phieuNhapId);
  if (idx !== -1) phieuNhapList.value[idx] = { ...phieuNhapList.value[idx], trangThai };
};

const showPhieuNhapDetailModal = ref(false);
const phieuNhapDetailData = ref(null);
const phieuNhapDetailItems = computed(() =>
  chiTietPhieuNhapList.value.filter(c => c.phieuNhapId === phieuNhapDetailData.value?.phieuNhapId),
);
const openPhieuNhapDetail = (p) => {
  phieuNhapDetailData.value = p;
  showPhieuNhapDetailModal.value = true;
};

const printEsc = (v) => String(v ?? '').replace(/[&<>]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' }[c]));

// In HTML qua iframe ẩn thay vì window.open('_blank') — tránh mở tab/cửa sổ mới lộ ra
// phía sau hộp thoại in, và tránh window.print() in nguyên trang admin (sidebar, topbar...).
const printHtmlInIframe = (html) => {
  const iframe = document.createElement('iframe');
  iframe.style.cssText = 'position:fixed;width:0;height:0;border:0;visibility:hidden;';
  document.body.appendChild(iframe);
  iframe.contentDocument.write(html);
  iframe.contentDocument.close();
  iframe.onload = () => {
    iframe.contentWindow.print();
    setTimeout(() => document.body.removeChild(iframe), 500);
  };
};

// In DANH SÁCH nhiều phiếu — chỉ bảng tóm tắt (báo cáo/xem nhanh cho quản lý), không
// kèm chi tiết từng dòng hàng (kèm hết sẽ ra rất nhiều trang nếu danh sách dài).
// Muốn xem chi tiết 1 phiếu cụ thể để đối chiếu/ký nhận thì dùng "In phiếu" trong
// modal Chi tiết (xem printPhieuNhapDetail bên dưới).
const printPhieuNhapList = () => {
  const headers = ['Mã', 'Ngày nhập', 'Nhà cung cấp', 'Nhân viên', 'Tổng tiền', 'Trạng thái'];
  const rows = filteredPhieuNhap.value.map(p => [
    p.maPhieuNhap, formatDate(p.ngayNhap), supplierName(p.nhaCungCapId),
    staffName(p.nhanVienId), formatPrice(p.tongTien ?? 0), statusLabel(p.trangThai),
  ]);
  const html = `<!doctype html><html><head><meta charset="utf-8"><title>Danh sách phiếu nhập</title>
    <style>
      body{font-family:Arial,sans-serif;padding:24px;color:#111;}
      h1{font-size:18px;margin-bottom:16px;}
      table{width:100%;border-collapse:collapse;font-size:13px;}
      th,td{border:1px solid #999;padding:6px 10px;text-align:left;}
      th{background:#eee;}
    </style></head><body>
    <h1>Danh sách phiếu nhập kho</h1>
    <table><thead><tr>${headers.map(h => `<th>${printEsc(h)}</th>`).join('')}</tr></thead>
    <tbody>${rows.map(r => `<tr>${r.map(c => `<td>${printEsc(c)}</td>`).join('')}</tr>`).join('')}</tbody></table>
    </body></html>`;
  printHtmlInIframe(html);
};

// In 1 PHIẾU — chứng từ đầy đủ để đối chiếu/lưu kho: thông tin phiếu, bảng chi tiết hàng,
// tổng tiền, và 3 dòng ký tên (người lập phiếu / thủ kho / người giao hàng).
const printPhieuNhapDetail = (p) => {
  if (!p) return;
  const items = chiTietPhieuNhapList.value.filter(c => c.phieuNhapId === p.phieuNhapId);
  const itemRows = items.map((c, i) => `<tr>
      <td class="center">${i + 1}</td>
      <td>${printEsc(c.maSku)}</td>
      <td class="center">${printEsc(c.soLuong)}</td>
      <td class="right">${printEsc(formatPrice(c.donGiaNhap))}</td>
      <td class="right">${printEsc(formatPrice(c.thanhTien))}</td>
    </tr>`).join('') || `<tr><td colspan="5" class="center muted">Không có hàng</td></tr>`;
  const html = `<!doctype html><html><head><meta charset="utf-8"><title>Phiếu nhập ${printEsc(p.maPhieuNhap)}</title>
    <style>
      body{font-family:Arial,sans-serif;padding:28px;color:#111;}
      h1{font-size:18px;margin:0 0 4px;}
      .sub{font-size:12px;color:#555;margin-bottom:18px;}
      .info{display:flex;justify-content:space-between;font-size:13px;margin-bottom:16px;}
      table{width:100%;border-collapse:collapse;font-size:13px;margin-bottom:6px;}
      th,td{border:1px solid #999;padding:6px 8px;text-align:left;}
      th{background:#eee;}
      .center{text-align:center;} .right{text-align:right;} .muted{color:#888;}
      .total{text-align:right;font-weight:bold;font-size:14px;margin-bottom:40px;}
      .signs{display:flex;justify-content:space-between;text-align:center;font-size:13px;}
      .signs div{width:30%;}
      .sign-line{margin-top:60px;border-top:1px solid #333;padding-top:4px;}
    </style></head><body>
    <h1>PHIẾU NHẬP KHO</h1>
    <div class="sub">Số phiếu: ${printEsc(p.maPhieuNhap)}</div>
    <div class="info">
      <div>Ngày nhập: ${printEsc(formatDate(p.ngayNhap))}</div>
      <div>Nhà cung cấp: ${printEsc(supplierName(p.nhaCungCapId))}</div>
      <div>Nhân viên: ${printEsc(staffName(p.nhanVienId))}</div>
    </div>
    <table>
      <thead><tr><th style="width:36px;">#</th><th>Mã SKU</th><th class="center" style="width:80px;">Số lượng</th><th class="right" style="width:120px;">Đơn giá</th><th class="right" style="width:130px;">Thành tiền</th></tr></thead>
      <tbody>${itemRows}</tbody>
    </table>
    <div class="total">Tổng tiền: ${printEsc(formatPrice(p.tongTien ?? 0))}</div>
    <div class="signs">
      <div>Người lập phiếu<div class="sign-line"></div></div>
      <div>Thủ kho<div class="sign-line"></div></div>
      <div>Người giao hàng<div class="sign-line"></div></div>
    </div>
    </body></html>`;
  printHtmlInIframe(html);
};

// Xuất CSV (mở được bằng Excel) — khỏi cần thêm thư viện xlsx cho một bảng đơn giản
const exportPhieuNhapExcel = () => {
  const rows = [
    ['Mã', 'Ngày nhập', 'Nhà cung cấp', 'Nhân viên', 'Tổng tiền', 'Trạng thái'],
    ...filteredPhieuNhap.value.map(p => [
      p.maPhieuNhap, formatDate(p.ngayNhap), supplierName(p.nhaCungCapId),
      staffName(p.nhanVienId), p.tongTien ?? 0, statusLabel(p.trangThai),
    ]),
  ];
  const csv = rows.map(r => r.map(v => `"${String(v ?? '').replaceAll('"', '""')}"`).join(',')).join('\n');
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `phieu-nhap-kho-${Date.now()}.csv`;
  a.click();
  URL.revokeObjectURL(url);
};

// ── Fetch ─────────────────────────────────────────────────────────────────────
// Chỉ tải 6 bảng chính lúc vào trang (dashboard + các bảng danh sách cần ngay).
// 7 danh sách tham chiếu (danh mục/hãng/NCC/CPU/RAM/ổ cứng/GPU) + chức vụ
// KHÔNG tải ở đây nữa — chỉ tải khi thực sự cần (mở form thêm/sửa sản phẩm,
// vào trang Nhân viên), xem ensureProductRefData()/ensureChucVuList() bên dưới.
// Với dữ liệu lớn, bớt 7-8 lệnh gọi song song này giúp trang vào nhanh hơn hẳn.
// Nhân viên KHÔNG tải ở đây nữa — không có KPI/dashboard/POS nào cần đến staff.value,
// chỉ tab Nhân viên và tab Phiếu nhập (staffName/staffOptions) cần, cả 2 đều lazy-load
// qua ensureStaffData() bên dưới. products/orders/customers/promotions/inventory VẪN
// tải eager vì dashboard KPI + POS (tìm SP, áp mã khuyến mãi, tra cứu KH) cần ngay.
const fetchAll = async () => {
  loading.value = true;
  const safe = (p) => p.catch(() => []);
  [
    products.value,
    orders.value,
    customers.value,
    promotions.value,
    inventory.value,
  ] = await Promise.all([
    safe(SanPhamService.getAll()),
    safe(DonHangService.getAll()),
    safe(KhachHangService.getAll()),
    safe(KhuyenMaiService.getAll()),
    safe(TonKhoService.getAll()),
  ]);
  loading.value = false;
  await autoMergeAllDuplicates();
};

let staffDataPromise = null;
const ensureStaffData = () => {
  if (staffDataPromise) return staffDataPromise;
  staffDataPromise = NhanVienService.getAll().catch(() => []).then((list) => {
    staff.value = list;
  });
  return staffDataPromise;
};

// Danh mục/hãng/NCC/CPU/RAM/ổ cứng/GPU — chỉ cần khi mở form thêm/sửa sản phẩm.
// Tải 1 lần, cache lại (promise dùng chung để 2 lần gọi gần nhau không tải trùng).
let productRefDataPromise = null;
const ensureProductRefData = () => {
  if (productRefDataPromise) return productRefDataPromise;
  productRefDataPromise = Promise.all([
    DanhMucService.getAll().catch(() => []),
    DmService.getThuongHieu().catch(() => []),
    DmService.getNhaCungCap().catch(() => []),
    DmService.getCpu().catch(() => []),
    DmService.getRam().catch(() => []),
    DmService.getOCung().catch(() => []),
    DmService.getGpu().catch(() => []),
  ]).then(([cat, br, sup, cpu, ram, oc, gpu]) => {
    categories.value = cat;
    brands.value = br;
    suppliers.value = sup;
    cpuList.value = cpu;
    ramList.value = ram;
    oCungList.value = oc;
    gpuList.value = gpu;
  });
  return productRefDataPromise;
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
    orders.value = await DonHangService.getAll().catch(() => []);
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

// ── Variant detail modal ───────────────────────────────────────────────────────
const showVariantModal   = ref(false);
const variantModalName   = ref('');
const variantModalList   = ref([]);
const variantSerialMap   = ref({});   // bienTheId → serial[]
const variantSerialLoad  = ref(false);
const serialInputs       = ref({});   // bienTheId → { soSerial, saving }

const showDetailModal  = ref(false);
const detailModalName  = ref('');
const detailModalList  = ref([]);
const detailSerialMap  = ref({});  // bienTheId → serial[]

// Helper: fetch serial của nhiều bienTheId song song → { bienTheId: serial[] }
const fetchSerialMap = async (bienTheIds) => {
  const results = await Promise.all(
    bienTheIds.map(id => ChiTietSanPhamService.getByBienThe(id).catch(() => []))
  );
  const map = {};
  bienTheIds.forEach((id, i) => { map[id] = results[i]; });
  return map;
};

const openDetail = async (sanPhamId, name) => {
  detailModalName.value = name;
  const list = products.value.filter(p => p.sanPhamId === sanPhamId);
  detailModalList.value = list;
  detailSerialMap.value = {};
  showDetailModal.value = true;
  detailSerialMap.value = await fetchSerialMap(list.map(v => v.bienTheId));
};

const openVariants = async (sanPhamId, name) => {
  variantModalName.value = name;
  const list = products.value.filter(p => p.sanPhamId === sanPhamId);
  variantModalList.value = list;
  variantSerialMap.value = {};
  const inputs = {};
  list.forEach(v => { inputs[v.bienTheId] = { soSerial: '', saving: false }; });
  serialInputs.value = inputs;
  showVariantModal.value = true;

  variantSerialLoad.value = true;
  variantSerialMap.value = await fetchSerialMap(list.map(v => v.bienTheId));
  variantSerialLoad.value = false;
};

const addSerial = async (bienTheId) => {
  const inp = serialInputs.value[bienTheId];
  if (!inp?.soSerial?.trim()) return;
  inp.saving = true;
  try {
    const res = await ChiTietSanPhamService.create({
      bienTheId,
      soSerial: inp.soSerial.trim(),
      trangThai: 'trong_kho',
      ngayNhapKho: nowLocalIso(),
    });
    if (!res.ok) throw new Error(t('admin.errors.addSerialError'));
    // Chỉ refresh serial của biến thể vừa thêm
    const updated = await ChiTietSanPhamService.getByBienThe(bienTheId).catch(() => []);
    variantSerialMap.value = { ...variantSerialMap.value, [bienTheId]: updated };
    inp.soSerial = '';
  } finally {
    inp.saving = false;
  }
};

// ── Products CRUD ─────────────────────────────────────────────────────────────
const showProductModal = ref(false);
const editingId = ref(null);
const formError = ref("");

// Serial number cho lần tạo mới
const soSerialMoi = ref('');
// Preview ảnh + file thực tế chờ upload
const imagePreview  = ref('');
const imageFilePending = ref(null);

// Tag phân loại (để lọc trang khách, xem App.vue CHIP_KEYWORDS/sidebarCatsBase) — danh
// sách cố định, chọn qua chip thay vì gõ tay để khỏi gõ sai slug làm hỏng bộ lọc.
const PHAN_LOAI_TAG_OPTIONS = [
  { value: 'gaming', label: 'Gaming' },
  { value: 'van_phong', label: 'Văn phòng' },
  { value: 'sinh_vien', label: 'Sinh viên' },
  { value: 'do_hoa', label: 'Đồ họa' },
  { value: 'ky_thuat', label: 'Kỹ thuật' },
  { value: 'macbook', label: 'MacBook' },
  { value: 'cu', label: 'Cũ' },
  { value: 'gia_re', label: 'Giá rẻ' },
  { value: 'linh_kien', label: 'Linh kiện' },
];
const toggleTag = (value) => {
  const tags = form.phanLoaiTags.split(',').map(s => s.trim()).filter(Boolean);
  const idx = tags.indexOf(value);
  if (idx === -1) tags.push(value); else tags.splice(idx, 1);
  form.phanLoaiTags = tags.join(',');
  // Tự ghép "Phân loại Tên" (tên hiển thị) từ nhãn của các tag đang chọn — khỏi gõ lại tay.
  form.phanLoaiTen = tags
    .map(v => PHAN_LOAI_TAG_OPTIONS.find(o => o.value === v)?.label)
    .filter(Boolean)
    .join(', ');
};
const isTagSelected = (value) => form.phanLoaiTags.split(',').map(s => s.trim()).includes(value);

const emptyForm = () => ({
  bienTheId: null,
  tenSanPham: "",
  thuongHieuId: null,
  danhMucId: null,
  nhaCungCapId: null,
  loaiSanPham: "",
  maSku: "",
  cpuId: null,
  ramId: null,
  oCungId: null,
  gpuId: null,
  kichThuocManHinh: "",
  heDieuHanh: "",
  pin: "",
  trongLuongKg: "",
  mauSac: "",
  giaBan: "",
  giaNhap: "",
  baoHanhThang: "",
  moTa: "",
  hinhAnhChinh: "",
  trangThai: "active",
  phanLoaiTags: "",
  phanLoaiTen: "",
});
const form = reactive(emptyForm());

const resetImageState = () => {
  imagePreview.value = '';
  imageFilePending.value = null;
};

const openAdd = async () => {
  await ensureProductRefData();
  Object.assign(form, emptyForm());
  editingId.value = null;
  addVariantMode.value = false;
  addVariantSanPhamId.value = null;
  formError.value = "";
  soSerialMoi.value = '';
  resetImageState();
  showProductModal.value = true;
};

// Thêm biến thể mới (màu/cấu hình khác) cho một sản phẩm ĐÃ TỒN TẠI —
// dùng lại modal Sản phẩm nhưng ẩn phần thông tin định danh sản phẩm (tên/hãng/danh mục...)
// vì những field đó dùng chung cho mọi biến thể, không tạo lại. POST thẳng tới
// /api/bien-the-san-pham (bienTheSanPhamId mới) thay vì /api/san-pham (tránh tạo sanPhamId trùng lặp).
const addVariantMode      = ref(false);
const addVariantSanPhamId = ref(null);
const addVariantSanPhamName = ref('');

const openAddVariant = async () => {
  await ensureProductRefData();
  Object.assign(form, emptyForm());
  const first = detailModalList.value[0];
  form.tenSanPham  = detailModalName.value;
  form.thuongHieuId = first?.thuongHieuId ?? null;
  form.danhMucId    = first?.danhMucId ?? null;
  form.loaiSanPham  = first?.loaiSanPham ?? '';
  editingId.value = null;
  addVariantMode.value = true;
  addVariantSanPhamId.value = first?.sanPhamId ?? null;
  addVariantSanPhamName.value = detailModalName.value;
  formError.value = "";
  soSerialMoi.value = '';
  resetImageState();
  showDetailModal.value = false;
  showProductModal.value = true;
};
const openEdit = async (p) => {
  await ensureProductRefData();
  Object.assign(form, {
    bienTheId: p.bienTheId,
    tenSanPham: p.tenSanPham,
    thuongHieuId: p.thuongHieuId,
    danhMucId: p.danhMucId,
    nhaCungCapId: p.nhaCungCapId,
    loaiSanPham: p.loaiSanPham,
    maSku: p.maSku,
    cpuId: cpuList.value.find((c) => c.tenCpu === p.cpu)?.cpuId ?? null,
    ramId: ramList.value.find((r) => r.dungLuong === p.ram)?.ramId ?? null,
    oCungId:
      oCungList.value.find((o) => o.loaiOcung === p.oCung)?.oCungId ?? null,
    gpuId: gpuList.value.find((g) => g.tenGpu === p.gpu)?.gpuId ?? null,
    kichThuocManHinh: p.kichThuocManHinh,
    heDieuHanh: p.heDieuHanh,
    pin: p.pin,
    trongLuongKg: p.trongLuongKg,
    mauSac: p.mauSac,
    giaBan: p.giaBan,
    giaNhap: p.giaNhap,
    baoHanhThang: p.baoHanhThang,
    moTa: p.moTa,
    hinhAnhChinh: p.hinhAnhChinh,
    trangThai: p.trangThai,
    phanLoaiTags: p.phanLoaiTags ?? "",
    phanLoaiTen: p.phanLoaiTen ?? "",
  });
  editingId.value = p.sanPhamId;
  addVariantMode.value = false;
  addVariantSanPhamId.value = null;
  formError.value = "";
  imagePreview.value = p.hinhAnhChinh || '';
  imageFilePending.value = null;
  showProductModal.value = true;
};

const handleImageFile = (e) => {
  const file = e.target.files[0];
  if (!file) return;
  imageFilePending.value = file;
  imagePreview.value = URL.createObjectURL(file);
};

const saveProduct = async () => {
  formError.value = "";

  // Upload ảnh trước nếu có file mới chọn
  if (imageFilePending.value) {
    const fd = new FormData();
    fd.append('file', imageFilePending.value);
    try {
      const upRes = await fetch('/api/upload/image', { method: 'POST', headers: authHeaders(), body: fd });
      if (upRes.ok) {
        const upData = await upRes.json();
        form.hinhAnhChinh = upData.url;
      } else {
        formError.value = t('admin.errors.uploadFailed', { status: upRes.status });
        return;
      }
    } catch (e) {
      formError.value = t('admin.errors.uploadError', { message: e.message });
      return;
    }
  }

  if (addVariantMode.value) {
    if (!form.maSku.trim()) { formError.value = t('admin.errors.skuRequired'); return; }
    const variantBody = {
      sanPhamId: addVariantSanPhamId.value,
      maSku: form.maSku,
      giaNhap: Number(form.giaNhap),
      giaBan: Number(form.giaBan),
      baoHanhThang: Number(form.baoHanhThang) || 0,
      hinhAnhBienThe: form.hinhAnhChinh,
      trangThai: form.trangThai,
      mauSac: form.mauSac,
      cpuId: form.cpuId ? Number(form.cpuId) : null,
      ramId: form.ramId ? Number(form.ramId) : null,
      oCungId: form.oCungId ? Number(form.oCungId) : null,
      gpuId: form.gpuId ? Number(form.gpuId) : null,
      kichThuocManHinh: form.kichThuocManHinh,
      heDieuHanh: form.heDieuHanh,
      pin: form.pin,
      trongLuongKg: form.trongLuongKg ? Number(form.trongLuongKg) : null,
    };
    try {
      const res = await BienTheSanPhamService.create(variantBody);
      if (!res.ok) {
        formError.value = t('admin.errors.saveFailed', { status: res.status, text: await res.text() });
        return;
      }
      const created = await res.json();
      if (soSerialMoi.value.trim()) {
        await ChiTietSanPhamService.create({
          bienTheId: created.bienTheId,
          soSerial: soSerialMoi.value.trim(),
          trangThai: 'trong_kho',
          ngayNhapKho: nowLocalIso(),
        }).catch(() => {});
      }
      showProductModal.value = false;
      resetImageState();
      products.value = await SanPhamService.getAll().catch(() => []);
      await openDetail(addVariantSanPhamId.value, addVariantSanPhamName.value);
    } catch (e) {
      formError.value = e.message;
    }
    return;
  }

  const body = {
    ...form,
    thuongHieuId: Number(form.thuongHieuId),
    danhMucId: Number(form.danhMucId),
    nhaCungCapId: form.nhaCungCapId ? Number(form.nhaCungCapId) : null,
    cpuId: form.cpuId ? Number(form.cpuId) : null,
    ramId: form.ramId ? Number(form.ramId) : null,
    oCungId: form.oCungId ? Number(form.oCungId) : null,
    gpuId: form.gpuId ? Number(form.gpuId) : null,
    giaBan: Number(form.giaBan),
    giaNhap: Number(form.giaNhap),
    trongLuongKg: form.trongLuongKg ? Number(form.trongLuongKg) : null,
    baoHanhThang: Number(form.baoHanhThang),
    ...(editingId.value ? {} : { ngayTao: nowLocalIso() }),
  };
  try {
    const res = await SanPhamService.save(editingId.value, body);
    if (!res.ok) {
      formError.value = t('admin.errors.saveFailed', { status: res.status, text: await res.text() });
      return;
    }

    // Nếu tạo mới + có serial → tìm bienTheId vừa tạo rồi POST chi_tiet
    if (!editingId.value && soSerialMoi.value.trim()) {
      const newList = await SanPhamService.getAll().catch(() => []);
      const newVariant = [...newList].reverse().find(p => p.maSku === form.maSku);
      if (newVariant) {
        await ChiTietSanPhamService.create({
          bienTheId: newVariant.bienTheId,
          soSerial: soSerialMoi.value.trim(),
          trangThai: 'trong_kho',
          ngayNhapKho: nowLocalIso(),
        }).catch(() => {});
      }
    }

    showProductModal.value = false;
    resetImageState();
    products.value = await SanPhamService.getAll().catch(() => []);
  } catch (e) {
    formError.value = e.message;
  }
};
// Xoá xong không cần tải lại cả bảng — API trả 204 rỗng nên chỉ cần biết ID
// vừa xoá là đủ để lọc khỏi mảng cục bộ (products = 1 dòng/biến thể, nên xoá
// sản phẩm = xoá hết các dòng cùng sanPhamId).
// Hỏi trước khi bấm xóa: sản phẩm chưa từng bán -> chỉ hỏi xác nhận đơn giản; đã có giao
// dịch -> báo thẳng lý do không xóa được, khỏi cần hỏi "có chắc không" cho việc chắc chắn
// sẽ thất bại.
const deleteProduct = async (id) => {
  const name = products.value.find(p => p.sanPhamId === id)?.tenSanPham ?? '';
  const daGiaoDich = await SanPhamService.hasTransactionHistory(id).catch(() => false);
  if (daGiaoDich) {
    showToast(t('admin.errors.cannotDeleteProduct', { name }));
    return;
  }
  if (!(await askConfirm(t('admin.confirm.deleteProductSimple', { name })))) return;
  const res = await SanPhamService.remove(id);
  if (!res.ok) { showToast(await res.text().catch(() => t('admin.errors.deleteFailed', { status: res.status }))); return; }
  products.value = products.value.filter(p => p.sanPhamId !== id);
};

// Xoá 1 biến thể (bienTheId) — dùng trong modal "Biến thể" và "Chi tiết", không xoá cả sản phẩm
const deleteVariant = async (bienTheId) => {
  const sku = products.value.find(p => p.bienTheId === bienTheId)?.maSku ?? '';
  const daGiaoDich = await BienTheSanPhamService.hasTransactionHistory(bienTheId).catch(() => false);
  if (daGiaoDich) {
    showToast(t('admin.errors.cannotDeleteVariant', { sku }));
    return;
  }
  if (!(await askConfirm(t('admin.confirm.deleteVariantSimple', { sku })))) return;
  const res = await BienTheSanPhamService.remove(bienTheId);
  if (!res.ok) { showToast(await res.text().catch(() => t('admin.errors.deleteFailed', { status: res.status }))); return; }
  products.value = products.value.filter(p => p.bienTheId !== bienTheId);
  variantModalList.value = variantModalList.value.filter(v => v.bienTheId !== bienTheId);
  detailModalList.value = detailModalList.value.filter(v => v.bienTheId !== bienTheId);
};

// ── Customers CRUD ────────────────────────────────────────────────────────────
const showCustomerModal = ref(false);
const editingCustomerId = ref(null);
const customerFormError = ref("");
const emptyCustomerForm = () => ({
  hoTen: "",
  soDienThoai: "",
  email: "",
  diaChi: "",
  loaiKhach: "ca_nhan",
  tenCongTy: "",
  maSoThue: "",
  diemTichLuy: 0,
  trangThai: "active",
});
const customerForm = reactive(emptyCustomerForm());

const openAddCustomer = () => {
  Object.assign(customerForm, emptyCustomerForm());
  editingCustomerId.value = null;
  customerFormError.value = "";
  showCustomerModal.value = true;
};
const openEditCustomer = (c) => {
  Object.assign(customerForm, {
    hoTen: c.hoTen,
    soDienThoai: c.soDienThoai,
    email: c.email ?? "",
    diaChi: c.diaChi ?? "",
    loaiKhach: c.loaiKhach ?? "ca_nhan",
    tenCongTy: c.tenCongTy ?? "",
    maSoThue: c.maSoThue ?? "",
    diemTichLuy: c.diemTichLuy ?? 0,
    trangThai: c.trangThai ?? "active",
  });
  editingCustomerId.value = c.khachHangId;
  customerFormError.value = "";
  posOpeningCustomerFromPos.value = false;
  showCustomerModal.value = true;
};
const closeCustomerModal = () => {
  showCustomerModal.value = false;
  posOpeningCustomerFromPos.value = false;
};
const saveCustomer = async () => {
  customerFormError.value = "";
  const body = {
    ...customerForm,
    diemTichLuy: Number(customerForm.diemTichLuy),
  };
  try {
    const res = await KhachHangService.save(editingCustomerId.value, body);
    if (!res.ok) {
      customerFormError.value = t('admin.errors.saveFailedWithText', { status: res.status, text: await res.text() });
      return;
    }
    showCustomerModal.value = false;
    // Khách hàng là entity phẳng (không join tên qua ID) nên vá cục bộ an toàn,
    // khỏi phải tải lại cả bảng khách hàng.
    if (editingCustomerId.value) {
      const idx = customers.value.findIndex((c) => c.khachHangId === editingCustomerId.value);
      if (idx !== -1) customers.value[idx] = { ...customers.value[idx], ...body };
    } else {
      const created = await res.json();
      customers.value = [...customers.value, created];
    }
    // Neu modal nay duoc mo tu luong tao hoa don POS (khach chua co trong he thong) —
    // tu dong gan khach vua tao lam khach hang cho hoa don dang tao, roi cho phep them SP.
    if (posOpeningCustomerFromPos.value) {
      posOpeningCustomerFromPos.value = false;
      const newCust = customers.value.find((c) => c.soDienThoai === body.soDienThoai);
      if (newCust) {
        posFoundCust.value = newCust;
        posPhoneNotFound.value = false;
        posError.value = '';
        posStage.value = 'selling';
      }
    }
  } catch (e) {
    customerFormError.value = e.message;
  }
};
const deleteCustomer = async (id) => {
  if (!(await askConfirm(t('admin.confirm.deleteCustomer')))) return;
  const res = await KhachHangService.remove(id);
  if (!res.ok) { showToast(t('admin.errors.deleteFailed', { status: res.status })); return; }
  customers.value = customers.value.filter((c) => c.khachHangId !== id);
};

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
      const created = await res.json();
      staff.value = [...staff.value, created];
    }
  } catch (e) {
    staffFormError.value = e.message;
  }
};
const deleteStaff = async (id) => {
  if (!(await askConfirm(t('admin.confirm.deleteStaff')))) return;
  const res = await NhanVienService.remove(id);
  if (!res.ok) { showToast(t('admin.errors.deleteFailed', { status: res.status })); return; }
  staff.value = staff.value.filter((s) => s.nhanVienId !== id);
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
      const created = await res.json();
      promotions.value = [...promotions.value, created];
    }
  } catch (e) {
    promoFormError.value = e.message;
  }
};
const deletePromo = async (id) => {
  if (!(await askConfirm(t('admin.confirm.deletePromo')))) return;
  const res = await KhuyenMaiService.remove(id);
  if (!res.ok) { showToast(t('admin.errors.deleteFailed', { status: res.status })); return; }
  promotions.value = promotions.value.filter((p) => p.khuyenMaiId !== id);
};

// ── Orders CRUD ───────────────────────────────────────────────────────────────
const deleteOrder = async (id) => {
  if (!(await askConfirm(t('admin.confirm.deleteOrder')))) return;
  const res = await DonHangService.remove(id);
  if (!res.ok) { showToast(t('admin.errors.deleteFailed', { status: res.status })); return; }
  orders.value = orders.value.filter((o) => o.donHangId !== id);
};

// ── Order detail modal (xem san pham trong don) ───────────────────────────────
const showOrderDetailModal = ref(false);
const orderDetailData      = ref(null);   // don hang dang xem
const orderDetailItems     = ref([]);     // ChiTietDonHangResponse[]
const orderDetailLoading   = ref(false);

const openOrderDetail = async (o) => {
  orderDetailData.value  = o;
  orderDetailItems.value = [];
  showOrderDetailModal.value = true;
  orderDetailLoading.value = true;
  try {
    orderDetailItems.value = await ChiTietDonHangService.getByDonHang(o.donHangId).catch(() => []);
  } finally {
    orderDetailLoading.value = false;
  }
};

// Tim ten san pham tu bienTheId trong danh sach products da load
const productByBienThe = (bienTheId) => products.value.find(p => p.bienTheId === bienTheId);

// ── Them san pham vao don ─────────────────────────────────────────────────────
const addItemMode           = ref(false);
const addItemBienTheId      = ref('');
const addItemQty            = ref(1);
const addItemLoading        = ref(false);
const addItemSearch         = ref('');
const addItemSelectedSpId   = ref(null);  // sanPhamId dang mo xem bien the

// Modal chi tiet san pham (khi click vao card)
const showAddItemDetailModal  = ref(false);
const addItemDetailGroup      = ref(null);   // group dang xem { sanPhamId, tenSanPham, ... variants[] }
const addItemSelectedConfig   = ref(null);   // cpu+ram+oCung key
const addItemSelectedColor    = ref(null);   // mauSac

// Cac phien ban doc nhat (cpu + ram + oCung) cho san pham dang xem
const addItemConfigs = computed(() => {
  if (!addItemDetailGroup.value) return [];
  const seen = new Set();
  const result = [];
  for (const v of addItemDetailGroup.value.variants) {
    const key = [v.cpu, v.ram, v.oCung].filter(Boolean).join('|');
    if (!seen.has(key)) { seen.add(key); result.push({ key, cpu: v.cpu, ram: v.ram, oCung: v.oCung }); }
  }
  return result;
});

// Cac mau sac trong phien ban dang chon
const addItemColorsForConfig = computed(() => {
  if (!addItemDetailGroup.value || !addItemSelectedConfig.value) return [];
  const [cpu, ram, oCung] = addItemSelectedConfig.value.split('|');
  return addItemDetailGroup.value.variants.filter(v =>
    (v.cpu || '') === (cpu || '') &&
    (v.ram || '') === (ram || '') &&
    (v.oCung || '') === (oCung || '')
  );
});

// Bien the hien tai dua vao config + mau sac dang chon
const addItemCurrentVariant = computed(() =>
  addItemColorsForConfig.value.find(v => v.mauSac === addItemSelectedColor.value) ||
  addItemColorsForConfig.value[0] || null
);

const openAddItemDetail = (group) => {
  addItemDetailGroup.value = group;
  addItemSelectedConfig.value = addItemConfigs.value[0]?.key ?? null;
  addItemSelectedColor.value  = addItemColorsForConfig.value[0]?.mauSac ?? null;
  showAddItemDetailModal.value = true;
};

// Khi chon config moi → reset color ve first of that config
const selectConfig = (key) => {
  addItemSelectedConfig.value = key;
  const [cpu, ram, oCung] = key.split('|');
  const first = addItemDetailGroup.value?.variants.find(v =>
    (v.cpu || '') === (cpu || '') && (v.ram || '') === (ram || '') && (v.oCung || '') === (oCung || '')
  );
  addItemSelectedColor.value = first?.mauSac ?? null;
};

const confirmAddFromDetail = async () => {
  const v = addItemCurrentVariant.value;
  if (!v) return;
  addItemLoading.value = true;
  showAddItemDetailModal.value = false;
  try {
    const res = await DonHangService.addChiTiet({
      donHangId:   orderDetailData.value.donHangId,
      bienTheId:   v.bienTheId,
      soLuong:     addItemQty.value,
      donGia:      v.giaBan,
      giamGiaDong: 0,
    });
    if (!res.ok) { showToast(t('admin.errors.addItemFailed', { status: res.status })); return; }
    await DonHangService.recalculate(orderDetailData.value.donHangId);
    await refreshOrderDetail();
    addItemQty.value = 1;
  } finally {
    addItemLoading.value = false;
  }
};

// Nhom products theo sanPhamId, lay gia thap nhat, loc theo search
const addItemProductGroups = computed(() => {
  const q = addItemSearch.value.toLowerCase().trim();
  const map = {};
  for (const p of products.value) {
    if (!map[p.sanPhamId]) {
      map[p.sanPhamId] = { sanPhamId: p.sanPhamId, tenSanPham: p.tenSanPham,
        tenThuongHieu: p.tenThuongHieu, hinhAnhChinh: p.hinhAnhChinh,
        phanLoaiTen: p.phanLoaiTen, variants: [] };
    }
    map[p.sanPhamId].variants.push(p);
  }
  let groups = Object.values(map);
  if (q) groups = groups.filter(g =>
    g.tenSanPham.toLowerCase().includes(q) || g.tenThuongHieu?.toLowerCase().includes(q)
  );
  groups.forEach(g => {
    g.minPrice = Math.min(...g.variants.map(v => Number(v.giaBan) || 0));
  });
  return groups.sort((a, b) => a.tenSanPham.localeCompare(b.tenSanPham));
});

const refreshOrderDetail = async () => {
  orders.value = await DonHangService.getAll().catch(() => []);
  const updated = orders.value.find(o => o.donHangId === orderDetailData.value?.donHangId);
  if (updated) orderDetailData.value = updated;
  orderDetailItems.value = await ChiTietDonHangService.getByDonHang(orderDetailData.value.donHangId).catch(() => []);
};

const addItemToOrder = async () => {
  if (!addItemBienTheId.value || addItemQty.value < 1) return;
  const v = productByBienThe(Number(addItemBienTheId.value));
  if (!v) return;
  addItemLoading.value = true;
  try {
    const res = await DonHangService.addChiTiet({
      donHangId:    orderDetailData.value.donHangId,
      bienTheId:    v.bienTheId,
      soLuong:      addItemQty.value,
      donGia:       v.giaBan,
      giamGiaDong:  0,
    });
    if (!res.ok) { showToast(t('admin.errors.addItemFailed', { status: res.status })); return; }
    await DonHangService.recalculate(orderDetailData.value.donHangId);
    await refreshOrderDetail();
    addItemBienTheId.value = '';
    addItemQty.value = 1;
    addItemMode.value = false;
  } finally {
    addItemLoading.value = false;
  }
};

const removeItemFromOrder = async (chiTietId) => {
  if (!(await askConfirm(t('admin.confirm.removeItemFromOrder')))) return;
  const res = await fetch(`/api/chi-tiet-don-hang/delete/${chiTietId}`, { method: 'DELETE', headers: authHeaders() });
  if (!res.ok) { showToast(t('admin.errors.deleteFailed', { status: res.status })); return; }
  await DonHangService.recalculate(orderDetailData.value.donHangId);
  await refreshOrderDetail();
};

// ── Gop don hang ─────────────────────────────────────────────────────────────
const mergeLoading = ref(false);

// Don hang cung khach, cung ngay dat (khong tinh gio), khac don hien tai
const mergeCandidates = computed(() => {
  if (!orderDetailData.value) return [];
  const curDate = orderDetailData.value.ngayDat?.slice(0, 10);
  return orders.value.filter(o =>
    o.khachHangId === orderDetailData.value.khachHangId &&
    o.donHangId   !== orderDetailData.value.donHangId &&
    o.ngayDat?.slice(0, 10) === curDate
  );
});

// Gop tat ca don cung ngay cung khach vao don hien tai (khong can chon thu cong)
const autoMergeOrders = async () => {
  if (mergeCandidates.value.length === 0) return;
  if (!(await askConfirm(t('admin.confirm.mergeOrders', { count: mergeCandidates.value.length, id: orderDetailData.value.donHangId })))) return;
  mergeLoading.value = true;
  try {
    const res = await DonHangService.merge(
      orderDetailData.value.donHangId,
      mergeCandidates.value.map(o => o.donHangId)
    );
    if (!res.ok) { showToast(t('admin.errors.mergeFailed', { status: res.status })); return; }
    await refreshOrderDetail();
  } finally {
    mergeLoading.value = false;
  }
};

// Mo chi tiet 1 bien the cu the (khong load tat ca bien the cung san pham)
const openVariantDetail = (bienTheId) => {
  const v = productByBienThe(bienTheId);
  if (!v) return;
  detailModalName.value = v.tenSanPham;
  detailModalList.value = [v];
  detailSerialMap.value = {};
  showDetailModal.value = true;
  ChiTietSanPhamService.getByBienThe(bienTheId)
    .catch(() => [])
    .then(serials => { detailSerialMap.value = { [bienTheId]: serials }; });
};

// ── Order status helpers (dùng chung — xem src/utils/orderStatus.js) ──────────

// ── Orders status update ──────────────────────────────────────────────────────
const showOrderModal = ref(false);
const editingOrder = ref(null);
const orderStatusError = ref("");
const orderStatusForm = reactive({
  trangThaiDonHang: "",
  trangThaiThanhToan: "",
  ngayGiaoDuKien: "", // Ngày dự kiến giao hàng
  ngayGiaoThucTe: "", // Ngày khách nhận hàng thực tế
});

const openOrderStatus = (o) => {
  editingOrder.value = o;
  orderStatusForm.trangThaiDonHang = o.trangThaiDonHang ?? "";
  orderStatusForm.trangThaiThanhToan = o.trangThaiThanhToan ?? "";
  orderStatusForm.ngayGiaoDuKien = o.ngayGiaoDuKien?.slice(0, 16) ?? "";
  orderStatusForm.ngayGiaoThucTe = o.ngayGiaoThucTe?.slice(0, 16) ?? "";
  orderStatusError.value = "";
  showOrderModal.value = true;
};
// Dựng body PUT /don-hang/update — dùng chung cho modal "Cập nhật trạng thái" (sửa tay,
// nhiều trường) và nút "next step" nhanh trên bảng (chỉ đổi trangThaiDonHang).
const buildOrderUpdateBody = (o, { trangThaiDonHang, trangThaiThanhToan, ngayGiaoDuKien, ngayGiaoThucTe }) => ({
  khachHangId: o.khachHangId,
  nhanVienId: o.nhanVienId ?? null,
  khuyenMaiId: o.khuyenMaiId ?? null,
  diaChiGiaoHangId: o.diaChiGiaoHangId ?? null,
  diaChiGiaoHangText: o.diaChiGiaoHangText ?? null,
  nguoiNhan: o.nguoiNhan || customerName(o.khachHangId),
  sdtNguoiNhan:
    o.sdtNguoiNhan ||
    (customers.value.find((c) => c.khachHangId === o.khachHangId)
      ?.soDienThoai ?? ""),
  tongTien: o.tongTien ?? 0,
  giamGia: o.giamGia ?? 0,
  phiVanChuyen: o.phiVanChuyen ?? 0,
  thanhTien: o.thanhTien ?? 0,
  ngayDat: o.ngayDat?.slice(0, 19),
  ngayGiaoDuKien: ngayGiaoDuKien || null,
  ngayGiaoThucTe: ngayGiaoThucTe || null,
  trangThaiDonHang,
  trangThaiThanhToan,
  kenhBan: o.kenhBan ?? null,
  ghiChu: o.ghiChu ?? null,
});

const saveOrderStatus = async () => {
  orderStatusError.value = "";
  const o = editingOrder.value;
  if (orderStatusForm.trangThaiDonHang === 'confirmed' && o.trangThaiDonHang !== 'confirmed' && o.kenhBan === 'online') {
    showOrderModal.value = false;
    await openXacNhanSerialModal(o);
    return;
  }
  const body = buildOrderUpdateBody(o, {
    trangThaiDonHang: orderStatusForm.trangThaiDonHang,
    trangThaiThanhToan: orderStatusForm.trangThaiThanhToan,
    ngayGiaoDuKien: orderStatusForm.ngayGiaoDuKien,
    ngayGiaoThucTe: orderStatusForm.ngayGiaoThucTe,
  });
  try {
    const res = await DonHangService.update(o.donHangId, body);
    if (!res.ok) {
      orderStatusError.value = t('admin.errors.saveFailedWithText', { status: res.status, text: await res.text() });
      return;
    }
    showOrderModal.value = false;
    orders.value = await DonHangService.getAll().catch(() => []);
  } catch (e) {
    orderStatusError.value = e.message;
  }
};

// Quy trình xử lý đơn thực tế: chờ xác nhận -> đã xác nhận -> đang đóng gói -> đang vận
// chuyển -> đã giao. Nút "bước tiếp theo" trên bảng đơn hàng đi đúng theo thứ tự này,
// khỏi phải mở modal chọn tay mỗi lần chỉ để nhích 1 bước — mở modal vẫn dùng được cho
// các trường hợp khác (hủy đơn, sửa ngày giao...).
const NEXT_ORDER_STATUS = { pending: 'confirmed', confirmed: 'processing', processing: 'shipping', shipping: 'delivered' };
const NEXT_ORDER_STATUS_LABEL = {
  pending:    { icon: '✅', key: 'admin.orders.nextConfirm' },
  confirmed:  { icon: '📦', key: 'admin.orders.nextPack' },
  processing: { icon: '🚚', key: 'admin.orders.nextShip' },
  shipping:   { icon: '🎉', key: 'admin.orders.nextComplete' },
};
const advanceOrderStatus = async (o) => {
  const next = NEXT_ORDER_STATUS[o.trangThaiDonHang];
  if (!next) return;
  // Đơn online chuyển sang "confirmed" (xác nhận) phải chọn serial trước — mở modal thay
  // vì đổi trạng thái thẳng. Sau khi xác nhận xong, bước "đóng gói" (confirmed -> processing)
  // chỉ còn đổi trạng thái đơn thuần, không qua modal nữa. Đơn tại quầy đã chốt serial từ
  // lúc tạo, không qua đây.
  if (next === 'confirmed' && o.kenhBan === 'online') {
    await openXacNhanSerialModal(o);
    return;
  }
  const body = buildOrderUpdateBody(o, {
    trangThaiDonHang: next,
    trangThaiThanhToan: o.trangThaiThanhToan,
    ngayGiaoDuKien: o.ngayGiaoDuKien,
    // Chuyển sang "delivered" mà chưa có ngày khách nhận hàng -> tự đóng dấu thời điểm này.
    ngayGiaoThucTe: next === 'delivered' && !o.ngayGiaoThucTe
      ? nowLocalIso()
      : o.ngayGiaoThucTe,
  });
  const res = await DonHangService.update(o.donHangId, body);
  if (!res.ok) { showToast(await res.text().catch(() => t('admin.errors.updateFailed', { status: res.status }))); return; }
  // Tải lại ngay thay vì tự ráp state cục bộ — chắc chắn đúng dữ liệu server, không phụ
  // thuộc việc SSE (chỉ để đồng bộ các tab/khách hàng khác) có tới kịp hay không.
  orders.value = await DonHangService.getAll().catch(() => orders.value);
};

// ── Modal "Chọn serial trước khi xác nhận" (chỉ đơn online) ──────────────────────
// Đơn online chỉ giữ chỗ serial ("giu_hang") lúc đặt hàng — admin phải xem lại/đổi rồi
// xác nhận ở đây trước khi đơn chuyển "confirmed". Sau đó bước "Đóng gói" (confirmed ->
// processing) chỉ còn là đổi trạng thái đơn thuần. Serial đã giữ chỗ sẵn từ lúc đặt hàng
// được tick trước, admin chỉ cần xác nhận hoặc đổi sang serial khác.
const showXacNhanSerialModal = ref(false);
const xacNhanOrder     = ref(null);
const xacNhanLines     = ref([]);   // [{ ...ChiTietDonHangResponse, chosenSerialIds: Set<number> }]
const xacNhanSerialMap = ref({});   // bienTheId -> ChiTietSanPhamResponse[]
const xacNhanLoading   = ref(false);
const xacNhanError     = ref('');

const openXacNhanSerialModal = async (o) => {
  xacNhanOrder.value = o;
  xacNhanLines.value = [];
  xacNhanError.value = '';
  showXacNhanSerialModal.value = true;
  xacNhanLoading.value = true;
  try {
    const [items, reserved] = await Promise.all([
      ChiTietDonHangService.getByDonHang(o.donHangId),
      ChiTietDonHangSerialService.getByDonHang(o.donHangId),
    ]);
    const reservedByLine = {};
    reserved.forEach((r) => {
      (reservedByLine[r.chiTietDonHangId] ??= []).push(r.chiTietId);
    });
    xacNhanSerialMap.value = await fetchSerialMap(items.map((i) => i.bienTheId));
    xacNhanLines.value = items.map((item) => ({
      ...item,
      chosenSerialIds: new Set(reservedByLine[item.id] ?? []),
    }));
  } catch (e) {
    xacNhanError.value = e.message;
  } finally {
    xacNhanLoading.value = false;
  }
};

// Serial khả dụng để chọn cho 1 dòng: đang "trong_kho", hoặc đang "giu_hang" nhưng đã
// giữ sẵn cho chính dòng này (FIFO lúc đặt hàng) — không hiện serial đang giữ cho đơn khác.
const xacNhanAvailableSerials = (line) => {
  const all = xacNhanSerialMap.value[line.bienTheId] ?? [];
  return all.filter((s) => s.trangThai === 'trong_kho' || line.chosenSerialIds.has(s.chiTietId));
};

const xacNhanToggleSerial = (line, serialId) => {
  if (line.chosenSerialIds.has(serialId)) line.chosenSerialIds.delete(serialId);
  else if (line.chosenSerialIds.size < line.soLuong) line.chosenSerialIds.add(serialId);
};

const xacNhanAllLinesComplete = computed(() =>
  xacNhanLines.value.length > 0 && xacNhanLines.value.every((l) => l.chosenSerialIds.size === l.soLuong)
);

const confirmXacNhanSerial = async () => {
  if (!xacNhanAllLinesComplete.value) return;
  xacNhanError.value = '';
  xacNhanLoading.value = true;
  try {
    const res = await DonHangService.xacNhan(xacNhanOrder.value.donHangId, {
      lines: xacNhanLines.value.map((l) => ({
        chiTietDonHangId: l.id,
        serialIds: [...l.chosenSerialIds],
      })),
    });
    if (!res.ok) {
      xacNhanError.value = await res.text().catch(() => t('admin.errors.updateFailed', { status: res.status }));
      return;
    }
    showXacNhanSerialModal.value = false;
    orders.value = await DonHangService.getAll().catch(() => orders.value);
  } catch (e) {
    xacNhanError.value = e.message;
  } finally {
    xacNhanLoading.value = false;
  }
};

// ── Inventory stock edit ──────────────────────────────────────────────────────
const showStockModal = ref(false);
const editingStock = ref(null);
// soLuongTon KHÔNG sửa tay được nữa — chỉ tăng khi nhập serial mới (xem newSerials),
// khớp đúng thực tế: mỗi máy nhập kho phải có 1 serial, số lượng = số serial đang "trong_kho".
const stockForm = reactive({ soLuongGiu: 0, tonKhoToiThieu: 0, newSerials: [''] });

const openEditStock = (item) => {
  editingStock.value = item;
  stockForm.soLuongGiu = item.soLuongGiu ?? 0;
  stockForm.tonKhoToiThieu = item.tonKhoToiThieu ?? 0;
  stockForm.newSerials = [''];
  showStockModal.value = true;
};
const addStockSerialRow = () => stockForm.newSerials.push('');
const removeStockSerialRow = (idx) => {
  if (stockForm.newSerials.length > 1) stockForm.newSerials.splice(idx, 1);
  else stockForm.newSerials[idx] = '';
};
// Nhập hàng loạt từ file — .xlsx/.xls đọc qua thư viện xlsx (mọi ô có dữ liệu, không
// phân biệt hàng/cột), .csv/.txt đọc thẳng dạng text (mỗi serial 1 dòng hoặc cách nhau
// bằng dấu phẩy) — khỏi phải gõ/dán tay từng dòng.
const importSerialsFromFile = async (e) => {
  const file = e.target.files?.[0];
  if (!file) return;
  const ext = file.name.split('.').pop()?.toLowerCase();
  let parsed;
  if (ext === 'xlsx' || ext === 'xls') {
    const buf = await file.arrayBuffer();
    const wb = XLSX.read(buf, { type: 'array' });
    const rows = XLSX.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]], { header: 1 });
    parsed = rows.flat().map((v) => String(v ?? '').trim()).filter(Boolean);
  } else {
    const text = await file.text();
    parsed = text.split(/[\n,]+/).map((s) => s.trim()).filter(Boolean);
  }
  const existing = stockForm.newSerials.filter(Boolean);
  stockForm.newSerials = [...existing, ...parsed].length ? [...existing, ...parsed] : [''];
  e.target.value = '';
};
const saveStock = async () => {
  const item = editingStock.value;
  const bienTheId = item.bienThe?.bienTheId;
  try {
    // 1) Thêm từng serial mới — mỗi cái tự tăng soLuongTon ở server (trigger DB tính lại
    // từ số serial "trong_kho", không phải giá trị FE gửi lên).
    const serials = stockForm.newSerials.map((s) => s.trim()).filter(Boolean);
    for (const soSerial of serials) {
      const res = await ChiTietSanPhamService.create({
        bienTheId, soSerial, trangThai: 'trong_kho',
        ngayNhapKho: nowLocalIso(),
      });
      if (!res.ok) { showToast(t('admin.errors.addSerialError')); return; }
    }
    // 2) Đang giữ / tồn kho tối thiểu — 2 field còn lại được sửa tay bình thường.
    const res = await TonKhoService.update(item.tonKhoId, {
      soLuongGiu: Number(stockForm.soLuongGiu),
      tonKhoToiThieu: Number(stockForm.tonKhoToiThieu),
    });
    if (!res.ok) { showToast(t('admin.errors.updateFailed', { status: res.status })); return; }
    showStockModal.value = false;
    // Lấy lại đúng dòng vừa đổi để có soLuongTon mới nhất do server tính.
    const updated = await TonKhoService.getByBienThe(bienTheId).catch(() => null);
    const idx = inventory.value.findIndex((i) => i.tonKhoId === item.tonKhoId);
    if (idx !== -1 && updated) inventory.value[idx] = updated;
  } catch (e) {
    showToast(e.message);
  }
};

// ── Stock Detail Modal (serial numbers) ──────────────────────────────────────
const showStockDetailModal = ref(false);
const stockDetailItem      = ref(null);   // tonKho item
const stockDetailSerials   = ref([]);
const stockDetailLoading   = ref(false);
const stockDetailNewSerial = ref('');
const stockDetailSaving    = ref(false);

const openStockDetail = async (item) => {
  stockDetailItem.value    = item;
  stockDetailSerials.value = [];
  stockDetailNewSerial.value = '';
  showStockDetailModal.value = true;
  stockDetailLoading.value   = true;
  const bienTheId = item.bienThe?.bienTheId;
  if (bienTheId) {
    stockDetailSerials.value = await ChiTietSanPhamService.getByBienThe(bienTheId).catch(() => []);
  }
  stockDetailLoading.value = false;
};

const addStockSerial = async () => {
  if (!stockDetailNewSerial.value.trim()) return;
  const bienTheId = stockDetailItem.value?.bienThe?.bienTheId;
  if (!bienTheId) return;
  stockDetailSaving.value = true;
  try {
    const res = await ChiTietSanPhamService.create({
      bienTheId,
      soSerial: stockDetailNewSerial.value.trim(),
      trangThai: 'trong_kho',
      ngayNhapKho: nowLocalIso(),
    });
    if (!res.ok) throw new Error(t('admin.errors.addSerialError'));
    stockDetailSerials.value = await ChiTietSanPhamService.getByBienThe(bienTheId).catch(() => []);
    // Chỉ lấy lại đúng 1 dòng tồn kho vừa đổi (server tự tính lại soLuongTon
    // từ số serial), khỏi phải tải cả bảng tồn kho.
    const updatedStock = await TonKhoService.getByBienThe(bienTheId).catch(() => null);
    if (updatedStock) {
      const idx = inventory.value.findIndex((i) => i.tonKhoId === updatedStock.tonKhoId);
      if (idx !== -1) inventory.value[idx] = updatedStock;
    }
    stockDetailNewSerial.value = '';
  } catch(e) { showToast(e.message); }
  finally { stockDetailSaving.value = false; }
};

// Xóa serial thêm nhầm — chỉ cho phép khi đang "trong_kho" (server chặn nếu đã bán/đã dùng).
const removeStockSerial = async (chiTietId) => {
  if (!(await askConfirm(t('admin.confirm.deleteSerial')))) return;
  const bienTheId = stockDetailItem.value?.bienThe?.bienTheId;
  try {
    const res = await ChiTietSanPhamService.remove(chiTietId);
    if (!res.ok) { showToast(await res.text().catch(() => t('admin.errors.deleteSerialError'))); return; }
    stockDetailSerials.value = stockDetailSerials.value.filter((s) => s.chiTietId !== chiTietId);
    const updatedStock = await TonKhoService.getByBienThe(bienTheId).catch(() => null);
    if (updatedStock) {
      const idx = inventory.value.findIndex((i) => i.tonKhoId === updatedStock.tonKhoId);
      if (idx !== -1) inventory.value[idx] = updatedStock;
    }
  } catch (e) { showToast(e.message); }
};

const stockDetailStatusLabel = (s) => t(`admin.statusLabel.${s}`);
const stockDetailStatusColor = (s) => {
  if (s === 'trong_kho')    return '#22c55e';
  if (s === 'giu_hang')     return '#facc15';
  if (s === 'da_ban')       return '#94a3b8';
  if (s === 'loi_bao_hanh') return '#fb923c';
  if (s === 'da_tra_hang')  return '#38bdf8';
  return '#6b7280';
};

// ── POS / Ban hang ───────────────────────────────────────────────────────────
// Luong bat buoc: phai xac dinh khach hang (co san hoac tao moi) TRUOC khi duoc
// them san pham vao gio — tranh tao hoa don "vo danh" roi moi lo tim/tao khach sau.
// posStage: 'start' (chua bat dau) -> 'phone' (dang nhap SDT tim/tao khach) -> 'selling' (da co khach, duoc them SP)
const posStage = ref('start');
const posPhoneNotFound = ref(false); // da tim nhung khong thay khach ung voi SDT vua nhap
const posOpeningCustomerFromPos = ref(false); // dang mo modal "Them khach hang" tu luong POS (de biet gan lai posFoundCust sau khi luu)
const posSearch = ref("");
const posCart = ref([]);
const posPhone = ref("");
const posFoundCust = ref(null);
const posError = ref("");
const posSuccess = ref(false);
const posPromoCode = ref("");
const posAppliedPromo = ref(null);
const posPromoMsg = ref("");

const posProducts = computed(() => {
  const q = posSearch.value.toLowerCase();
  return products.value.filter(
    (p) =>
      p.trangThai === "active" &&
      (!q ||
        p.tenSanPham.toLowerCase().includes(q) ||
        (p.maSku ?? "").toLowerCase().includes(q)),
  );
});
const posCartTotal = computed(() =>
  posCart.value.reduce((s, i) => s + i.giaBan * i.soLuong, 0),
);
const posFee = computed(() => (posCartTotal.value >= 300000 ? 0 : 30000));
const posGiamGia = computed(() => {
  const p = posAppliedPromo.value;
  if (!p) return 0;
  if (p.loai === 'percent') {
    let d = posCartTotal.value * Number(p.giaTri) / 100;
    if (p.giaTriToiDa) d = Math.min(d, Number(p.giaTriToiDa));
    return d;
  }
  return Number(p.giaTri) || 0;
});
const posGrandTotal = computed(() => Math.max(0, posCartTotal.value + posFee.value - posGiamGia.value));

const posApplyPromo = () => {
  const code = posPromoCode.value.trim().toUpperCase();
  if (!code) { posAppliedPromo.value = null; posPromoMsg.value = ''; return; }
  const p = promotions.value.find(
    (x) => x.maKhuyenMai?.toUpperCase() === code && x.trangThai === 'active'
  );
  if (p) {
    posAppliedPromo.value = p;
    posPromoMsg.value = t('checkout.promoSuccess', { name: p.tenKhuyenMai });
  } else {
    posAppliedPromo.value = null;
    posPromoMsg.value = t('checkout.promoInvalid');
  }
};

// ── Hoa don cho (giu don POS) ─────────────────────────────────────────────────
// Luu tam gio hang dang ban do khach chua thanh toan xong / nhan vien can phuc vu
// khach khac — luu o localStorage (tinh nang tien loi cho nhan vien tai quay,
// khong can bang rieng trong DB vi don chua thuc su ton tai cho toi khi thanh toan).
const HELD_ORDERS_KEY = 'saophone_pos_held_orders';
const heldOrders = ref([]);
const loadHeldOrders = () => {
  try { heldOrders.value = JSON.parse(localStorage.getItem(HELD_ORDERS_KEY)) ?? []; }
  catch { heldOrders.value = []; }
};
const saveHeldOrders = () => {
  localStorage.setItem(HELD_ORDERS_KEY, JSON.stringify(heldOrders.value));
};
loadHeldOrders();

const showHeldOrders = ref(false);

const posHoldOrder = () => {
  if (!posCart.value.length) return;
  heldOrders.value.unshift({
    id: Date.now(),
    heldAt: new Date().toISOString(),
    cart: posCart.value,
    phone: posPhone.value,
    foundCust: posFoundCust.value,
    promoCode: posPromoCode.value,
    appliedPromo: posAppliedPromo.value,
  });
  saveHeldOrders();
  // Chi don sach form tai cho — KHONG goi posReset() vi no se tra serial ve trong_kho.
  // Cac serial trong gio nay van phai o trang thai "giu_hang" cho toi khi tiep tuc
  // ban (Tiep tuc) hoac huy han (Xoa o danh sach don dang giu).
  posCart.value = [];
  posPhone.value = "";
  posFoundCust.value = null;
  posError.value = "";
  posSuccess.value = false;
  posPromoCode.value = "";
  posAppliedPromo.value = null;
  posPromoMsg.value = "";
  posStage.value = 'start';
  posPhoneNotFound.value = false;
};

const posResumeHeld = (id) => {
  const held = heldOrders.value.find((h) => h.id === id);
  if (!held) return;
  posCart.value = held.cart;
  posPhone.value = held.phone;
  posFoundCust.value = held.foundCust;
  posPromoCode.value = held.promoCode;
  posAppliedPromo.value = held.appliedPromo;
  heldOrders.value = heldOrders.value.filter((h) => h.id !== id);
  saveHeldOrders();
  showHeldOrders.value = false;
  // Khach hang cua don nay da duoc xac dinh tu truoc (luc giu don) — vao thang man hinh ban
  posStage.value = 'selling';
};

const posDeleteHeld = async (id) => {
  const held = heldOrders.value.find((h) => h.id === id);
  heldOrders.value = heldOrders.value.filter((h) => h.id !== id);
  saveHeldOrders();
  // Huy han don giu -> tra lai tat ca serial trong don do ve trong_kho de ban duoc tiep
  if (held) await Promise.all(held.cart.map((item) => setSerialTrangThai(item, 'trong_kho')));
};

// Ban tai quay bat buoc chon serial cu the truoc khi cho vao gio — moi dong trong
// gio la 1 don vi vat ly rieng (chiTietId rieng), khong dung soLuong gop nhieu may
// lai vi moi may co IMEI khac nhau. Serial da o trong gio se khong hien lai de chon.
const showSerialPicker = ref(false);
const serialPickerProduct = ref(null);
const serialPickerList = ref([]);
const serialPickerLoading = ref(false);

const posOpenSerialPicker = async (p) => {
  // Chan them vao gio neu chua xac dinh khach hang — nhan vien duyet san pham
  // thoai mai, nhung phai qua cong "Tao hoa don" (o khu vuc gio hang) truoc.
  if (posStage.value !== 'selling') {
    if (posStage.value === 'start') posStartInvoice();
    posError.value = t('admin.pos.needCustomerFirst');
    return;
  }
  serialPickerProduct.value = p;
  serialPickerList.value = [];
  showSerialPicker.value = true;
  serialPickerLoading.value = true;
  const all = await ChiTietSanPhamService.getByBienThe(p.bienTheId).catch(() => []);
  // "trong_kho" la nguon duy nhat cho serial con ban duoc — 1 serial da bi danh dau
  // "giu_hang" (do dang nam trong gio cua BAT KY phien POS nao, ke ca giu don) se
  // tu dong bi loai o day, khong can biet gio do thuoc phien nao.
  serialPickerList.value = all.filter((s) => s.trangThai === 'trong_kho');
  serialPickerLoading.value = false;
};

// Doi trang thai 1 serial — dung khi chon vao gio (giu_hang) hoac tra lai kho (trong_kho).
// Phai truyen du bienTheId/soSerial/ngayNhapKho vi backend dung BeanUtils copy toan bo
// request len entity, thieu ngayNhapKho se lam mat ngay nhap kho goc.
const setSerialTrangThai = async (item, trangThai) => {
  await ChiTietSanPhamService.update(item.chiTietId, {
    bienTheId: item.bienTheId,
    soSerial: item.soSerial,
    trangThai,
    ngayNhapKho: item.ngayNhapKho,
  }).catch(() => {});
};

const posSelectSerial = async (serial) => {
  const p = serialPickerProduct.value;
  const item = {
    bienTheId: p.bienTheId,
    tenSanPham: p.tenSanPham,
    maSku: p.maSku,
    giaBan: p.giaBan,
    hinhAnhChinh: p.hinhAnhChinh,
    chiTietId: serial.chiTietId,
    soSerial: serial.soSerial,
    ngayNhapKho: serial.ngayNhapKho,
    soLuong: 1,
  };
  posCart.value.push(item);
  showSerialPicker.value = false;
  // Danh dau giu ngay khi chon — de phien POS khac (hoac don khac) khong the chon trung
  // serial nay, ke ca khi don nay chua duoc "giu don" chinh thuc.
  await setSerialTrangThai(item, 'giu_hang');
};

const posRemove = async (chiTietId) => {
  const item = posCart.value.find((i) => i.chiTietId === chiTietId);
  posCart.value = posCart.value.filter((i) => i.chiTietId !== chiTietId);
  if (item) await setSerialTrangThai(item, 'trong_kho');
};
const posReset = async () => {
  await Promise.all(posCart.value.map((item) => setSerialTrangThai(item, 'trong_kho')));
  posCart.value = [];
  posPhone.value = "";
  posFoundCust.value = null;
  posError.value = "";
  posSuccess.value = false;
  posPromoCode.value = "";
  posAppliedPromo.value = null;
  posPromoMsg.value = "";
  posStage.value = 'start';
  posPhoneNotFound.value = false;
};

const posStartInvoice = () => {
  posStage.value = 'phone';
  posPhoneNotFound.value = false;
  posError.value = '';
};

const posLookup = () => {
  const phone = posPhone.value.trim();
  if (!phone) return;
  const found = customers.value.find((c) => c.soDienThoai === phone) ?? null;
  posFoundCust.value = found;
  if (found) {
    posPhoneNotFound.value = false;
    posError.value = '';
    posStage.value = 'selling';
  } else {
    posPhoneNotFound.value = true;
  }
};

const posCancelCreateCustomer = () => {
  posPhoneNotFound.value = false;
  posPhone.value = '';
};

// Mo lai chinh modal "Them khach hang" cua muc Quan ly khach hang (khong tao UI moi) —
// chi khac la sau khi luu thanh cong se tu gan lam khach hang cho hoa don POS dang tao.
const posConfirmCreateCustomer = () => {
  posOpeningCustomerFromPos.value = true;
  openAddCustomer();
  customerForm.soDienThoai = posPhone.value.trim();
};

// Chuyen loi validate dang JSON {"field":"message"} tu backend thanh 1 dong text de doc
const parsePosApiError = async (res) => {
  const raw = await res.text();
  try {
    const obj = JSON.parse(raw);
    const messages = Object.values(obj).filter((v) => typeof v === 'string');
    if (messages.length) return messages.join(' · ');
  } catch { /* khong phai JSON, dung raw text */ }
  return raw;
};

const posPlaceOrder = async () => {
  if (!posCart.value.length) { posError.value = t('admin.pos.cartEmpty'); return; }
  // Khach hang bat buoc phai duoc xac dinh (co san hoac tao moi) TRUOC khi co san pham
  // trong gio (theo luong posStage) nen o day luon phai co san posFoundCust.
  if (!posFoundCust.value) { posError.value = t('admin.pos.phoneRequired'); return; }
  posError.value = "";
  posSuccess.value = false;
  try {
    const khachHangId = posFoundCust.value.khachHangId;
    const nguoiNhan = posFoundCust.value.hoTen;
    const orderRes = await DonHangService.create({
      khachHangId, nguoiNhan, sdtNguoiNhan: posFoundCust.value.soDienThoai,
      diaChiGiaoHangText: posFoundCust.value.diaChi ?? "Tai cua hang",
      khuyenMaiId: posAppliedPromo.value?.khuyenMaiId ?? null,
      tongTien: posCartTotal.value, giamGia: posGiamGia.value,
      phiVanChuyen: posFee.value, thanhTien: posGrandTotal.value,
      ngayDat: nowLocalIso(),
      trangThaiDonHang: "confirmed", trangThaiThanhToan: "paid", kenhBan: "in_store",
    });
    if (!orderRes.ok) throw new Error(t('admin.errors.createOrderError', { message: await parsePosApiError(orderRes) }));
    const created = await orderRes.json();
    const donHangId = created.id ?? created.donHangId;
    for (const item of posCart.value) {
      const ctRes = await DonHangService.addChiTiet({
        donHangId, bienTheId: item.bienTheId, chiTietId: item.chiTietId, soLuong: item.soLuong, donGia: item.giaBan, giamGiaDong: 0,
      });
      if (!ctRes.ok) throw new Error(t('admin.errors.addProductError', { message: await ctRes.text() }));
    }
    posSuccess.value = true;
    posCart.value = []; posPhone.value = ""; posFoundCust.value = null;
    posPromoCode.value = ""; posAppliedPromo.value = null; posPromoMsg.value = "";
    posStage.value = 'start';
    orders.value = await DonHangService.getAll().catch(() => []);
  } catch (e) {
    posError.value = e.message;
  }
};

let orderSse = null;

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

  // EventSource không gửi được header Authorization → truyền JWT qua query string
  orderSse = new EventSource(`/api/don-hang/events?token=${encodeURIComponent(AuthStore.user?.token ?? '')}`);
  orderSse.onerror = (e) => console.error('Kết nối SSE (đơn hàng real-time) lỗi:', e);
  orderSse.addEventListener('new-order', async () => {
    orders.value = await DonHangService.getAll().catch(() => []);
    await autoMergeAllDuplicates();
    await fetchProductSales();
  });
  // Đơn hàng đổi trạng thái — từ tab admin khác, hoặc từ chính tab này (advanceOrderStatus/
  // saveOrderStatus không tự patch state cục bộ, dựa hẳn vào đây để khỏi trùng 2 nguồn dữ
  // liệu) — tải lại danh sách, khỏi cần F5.
  orderSse.addEventListener('order-updated', async () => {
    orders.value = await DonHangService.getAll().catch(() => []);
  });
});

onUnmounted(() => {
  if (orderSse) orderSse.close();
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

      <!-- Chuyển role Admin / Nhan vien -->
      <div class="d-flex gap-2 p-3 pb-2">
        <button class="btn btn-sm flex-grow-1 fw-medium"
                :class="currentRole==='admin' ? 'btn-warning text-dark' : 'btn-outline-secondary text-secondary'"
                style="font-size:0.82rem; border-radius:7px;"
                @click="switchRole('admin')">{{ t('admin.roleSwitch.admin') }}</button>
        <button class="btn btn-sm flex-grow-1 fw-medium"
                :class="currentRole==='user' ? 'btn-warning text-dark' : 'btn-outline-secondary text-secondary'"
                style="font-size:0.82rem; border-radius:7px;"
                @click="switchRole('user')">{{ t('admin.roleSwitch.staff') }}</button>
      </div>

      <!-- Nav admin -->
      <nav class="flex-grow-1 d-flex flex-column px-2 pb-2" v-show="currentRole === 'admin'">
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

      <!-- Nav nhan vien -->
      <nav class="flex-grow-1 d-flex flex-column px-2 pb-2" v-show="currentRole === 'user'">
        <div class="adm-nav-label">{{ t('admin.sidebar.groupMyPage') }}</div>
        <div class="adm-nav" :class="{active: currentPage==='user-home'}" @click="navigate('user-home')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M10.707 2.293a1 1 0 00-1.414 0l-7 7a1 1 0 001.414 1.414L4 10.414V17a1 1 0 001 1h2a1 1 0 001-1v-2a1 1 0 011-1h2a1 1 0 011 1v2a1 1 0 001 1h2a1 1 0 001-1v-6.586l.293.293a1 1 0 001.414-1.414l-7-7z"/></svg>
          {{ t('admin.sidebar.userHome') }}
        </div>
        <div class="adm-nav" :class="{active: currentPage==='user-orders'}" @click="navigate('user-orders')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M9 2a1 1 0 000 2h2a1 1 0 100-2H9z"/><path fill-rule="evenodd" d="M4 5a2 2 0 012-2 3 3 0 003 3h2a3 3 0 003-3 2 2 0 012 2v11a2 2 0 01-2 2H6a2 2 0 01-2-2V5z" clip-rule="evenodd"/></svg>
          {{ t('admin.sidebar.userOrders') }}
        </div>
        <div class="adm-nav" :class="{active: currentPage==='user-browse'}" @click="navigate('user-browse')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 2a4 4 0 00-4 4v1H5a1 1 0 00-.994.89l-1 9A1 1 0 004 18h12a1 1 0 00.994-1.11l-1-9A1 1 0 0015 7h-1V6a4 4 0 00-4-4zm2 5V6a2 2 0 10-4 0v1h4zm-6 3a1 1 0 112 0 1 1 0 01-2 0zm7-1a1 1 0 100 2 1 1 0 000-2z" clip-rule="evenodd"/></svg>
          {{ t('admin.sidebar.userBrowse') }}
        </div>
        <div class="adm-nav" :class="{active: currentPage==='user-profile'}" @click="navigate('user-profile')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clip-rule="evenodd"/></svg>
          {{ t('admin.sidebar.userProfile') }}
        </div>
      </nav>

      <!-- Footer sidebar: thong tin user + logout -->
      <div class="p-3 border-top" style="border-color:var(--border-color-soft)!important;">
        <div class="d-flex align-items-center gap-2 mb-2">
          <div class="rounded-circle d-flex align-items-center justify-content-center fw-bold flex-shrink-0"
               style="width:34px;height:34px;background:var(--accent);color:var(--accent-text);font-size:0.9rem;">{{ userAvatar }}</div>
          <div class="flex-grow-1" style="min-width:0;">
            <div class="fw-semibold text-truncate" style="font-size:0.85rem;">{{ userDisplayName }}</div>
            <div style="font-size:0.72rem;color:var(--text-muted);">{{ userDisplayRole }}</div>
          </div>
        </div>
        <button class="btn btn-sm w-100 fw-semibold"
                style="background:var(--bg-card); border:1px solid #7f1d1d; border-radius:8px; color:#f87171; font-size:0.78rem;"
                @click="logout">
          {{ t('admin.sidebar.logout') }}
        </button>
      </div>
    </aside><!-- /sidebar -->

    <!-- ══════════ MAIN CONTENT ══════════ -->
    <main class="flex-grow-1 d-flex flex-column overflow-hidden">

      <!-- Topbar: tieu de trang hien tai -->
      <div class="d-flex align-items-center justify-content-between p-3 border-bottom"
           style="background:var(--bg-card-inset); border-color:var(--border-color)!important;">
        <div>
          <div class="fw-bold" style="font-size:1.05rem;">{{ topbarTitle }}</div>
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
          <div v-if="loading" class="text-secondary small">{{ t('admin.dashboard.loading') }}</div>
          <template v-else>
            <!-- Stat cards -->
            <div class="row g-3 mb-4">
              <div class="col-6 col-xl-3">
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
              <div class="col-6 col-xl-3">
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
              <div class="col-6 col-xl-3">
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
                      <div class="d-flex align-items-center gap-2 mb-1">
                        <span class="text-secondary small">{{ t('admin.dashboard.totalRevenue') }}</span>
                        <span v-if="revenueTrendDelta !== null"
                              class="fw-bold" style="font-size:0.7rem;"
                              :style="{ color: revenueTrendDelta >= 0 ? '#22c55e' : '#f87171' }">
                          {{ revenueTrendDelta >= 0 ? '▲' : '▼' }} {{ Math.abs(revenueTrendDelta) }}%
                        </span>
                      </div>
                      <div class="fw-bold" style="font-size:1.1rem;">{{ formatPrice(totalRevenue) }}</div>
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
          <div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
            <span class="text-secondary small">{{ filteredGroupedProducts.length }}/{{ groupedProducts.length }} {{ t('admin.products.countSuffix') }}</span>
            <div class="d-flex gap-2 flex-wrap">
              <input v-model="productSearch" class="form-control form-control-sm" style="width:220px;background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);" :placeholder="t('admin.products.searchPlaceholder')" />
              <button class="btn btn-sm btn-warning text-dark fw-bold" @click="openAdd">{{ t('admin.products.add') }}</button>
            </div>
          </div>
          <div v-if="loading" class="text-secondary small">{{ t('admin.products.loading') }}</div>
          <div v-else class="table-responsive">
            <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
              <thead><tr>
                <th style="width:40px;">{{ t('admin.common.stt') }}</th>
                <th>{{ t('admin.products.colName') }}</th><th>{{ t('admin.products.colBrand') }}</th><th>{{ t('admin.products.colCategory') }}</th>
                <th>{{ t('admin.products.colVariant') }}</th><th>{{ t('admin.products.colPriceFrom') }}</th><th>{{ t('admin.products.colStatus') }}</th><th>{{ t('admin.products.colAction') }}</th>
              </tr></thead>
              <tbody>
                <tr v-for="(p, idx) in filteredGroupedProducts" :key="p.sanPhamId">
                  <td class="text-secondary">{{ idx + 1 }}</td>
                  <td>{{ p.tenSanPham }}</td>
                  <td>{{ p.tenThuongHieu }}</td>
                  <td>{{ p.tenDanhMuc }}</td>
                  <td class="text-center">
                    <span class="badge bg-secondary">{{ p.variantCount }}</span>
                  </td>
                  <td>{{ formatPrice(p.minPrice) }}</td>
                  <td><span class="badge" :class="p.trangThai==='active'?'bg-success':'bg-secondary'">{{ statusLabel(p.trangThai) }}</span></td>
                  <td>
                    <div class="d-flex gap-1">
                      <button class="btn btn-sm btn-outline-primary" style="font-size:0.78rem; padding:2px 8px;" @click="openDetail(p.sanPhamId, p.tenSanPham)">{{ t('admin.products.detail') }}</button>
                      <button class="btn btn-sm btn-outline-danger"  style="font-size:0.78rem; padding:2px 8px;" @click="deleteProduct(p.sanPhamId)">{{ t('admin.products.delete') }}</button>
                    </div>
                  </td>
                </tr>
                <tr v-if="filteredGroupedProducts.length===0"><td colspan="8" class="text-center text-secondary">{{ t('admin.products.empty') }}</td></tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- ── Don hang ── -->
        <section v-show="currentPage === 'orders'">

          <!-- Chế độ: danh sách các ngày có đơn hàng (Lịch sử đơn hàng) -->
          <template v-if="orderViewMode === 'history-dates'">
            <div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
              <span class="fw-bold" style="color:var(--text-heading);">{{ t('admin.orders.history') }}</span>
              <button class="btn btn-sm btn-outline-secondary" @click="backToToday">{{ t('admin.orders.backToToday') }}</button>
            </div>
            <div v-if="loading" class="text-secondary small">{{ t('admin.orders.loading') }}</div>
            <div v-else class="d-flex flex-column gap-2">
              <div v-for="d in orderDatesGrouped" :key="d.dateKey"
                   class="d-flex justify-content-between align-items-center px-3 py-3 rounded-3"
                   style="background:var(--bg-card); border:1px solid var(--border-color-soft); cursor:pointer;"
                   @click="openHistoryDay(d.dateKey)">
                <span class="fw-semibold" style="color:var(--text-primary);">{{ d.label }}</span>
                <span class="text-secondary small d-flex align-items-center gap-2">{{ d.count }} {{ t('admin.orders.countSuffix') }} <span style="font-size:1.1rem;">›</span></span>
              </div>
              <div v-if="orderDatesGrouped.length===0" class="text-center text-secondary py-3">{{ t('admin.orders.empty') }}</div>
            </div>
          </template>

          <!-- Chế độ: đơn hôm nay (mặc định) hoặc đơn của 1 ngày lịch sử đã chọn -->
          <template v-else>
            <div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
              <div class="d-flex align-items-center gap-2">
                <button v-if="orderViewMode==='history-day'" class="btn btn-sm btn-outline-secondary" @click="backToDateList">{{ t('admin.orders.backToDateList') }}</button>
                <span class="text-secondary small">
                  <span v-if="orderViewMode==='history-day'" class="fw-semibold" style="color:var(--text-primary);">{{ formatDateHeading(historySelectedDate) }} · </span>
                  {{ filteredOrders.length }}/{{ ordersBaseList.length }} {{ t('admin.orders.countSuffix') }}
                </span>
              </div>
              <div class="d-flex gap-2 flex-wrap">
                <input v-model="orderSearch" class="form-control form-control-sm" style="width:220px;background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);" :placeholder="t('admin.orders.searchPlaceholder')" />
                <select v-model="orderStatusFilter" class="form-select form-select-sm" style="width:170px;background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);">
                  <option value="">{{ t('admin.orders.allStatuses') }}</option>
                  <option value="pending">{{ orderStatusLabel('pending') }}</option>
                  <option value="confirmed">{{ orderStatusLabel('confirmed') }}</option>
                  <option value="processing">{{ orderStatusLabel('processing') }}</option>
                  <option value="shipping">{{ orderStatusLabel('shipping') }}</option>
                  <option value="delivered">{{ orderStatusLabel('delivered') }}</option>
                  <option value="cancelled">{{ orderStatusLabel('cancelled') }}</option>
                  <option value="returned">{{ orderStatusLabel('returned') }}</option>
                </select>
                <select v-model="orderPaymentFilter" class="form-select form-select-sm" style="width:150px;background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);">
                  <option value="">{{ t('admin.orders.allPayments') }}</option>
                  <option value="paid">{{ t('admin.orders.paid') }}</option>
                  <option value="unpaid">{{ t('admin.orders.unpaid') }}</option>
                </select>
                <button v-if="orderViewMode==='today'" class="btn btn-sm btn-outline-warning" @click="openOrderHistory">{{ t('admin.orders.history') }}</button>
              </div>
            </div>
            <div v-if="loading" class="text-secondary small">{{ t('admin.orders.loading') }}</div>
            <div v-else class="table-responsive">
              <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
                <thead><tr><th style="width:40px;">{{ t('admin.common.stt') }}</th><th>{{ t('admin.orders.colOrderCode') }}</th><th>{{ t('admin.orders.colCustomer') }}</th><th>{{ t('admin.orders.colTotal') }}</th><th>{{ t('admin.orders.colOrderStatus') }}</th><th>{{ t('admin.orders.colPaymentStatus') }}</th><th>{{ t('admin.orders.colOrderDate') }}</th><th>{{ t('admin.orders.colAction') }}</th></tr></thead>
                <tbody>
                  <tr v-for="(o, idx) in filteredOrders" :key="o.donHangId">
                    <td class="text-secondary">{{ idx + 1 }}</td>
                    <td class="text-secondary">#{{ o.donHangId }}</td>
                    <td>{{ customerName(o.khachHangId) }}</td>
                    <td>{{ formatPrice(o.thanhTien) }}</td>
                    <td>
                      <span class="badge" :style="{ background: orderStatusColor(o.trangThaiDonHang).bg, color: orderStatusColor(o.trangThaiDonHang).text }">
                        {{ orderStatusIcon(o.trangThaiDonHang) }} {{ orderStatusLabel(o.trangThaiDonHang) }}
                      </span>
                    </td>
                    <td>
                      <span v-if="o.trangThaiThanhToan" class="badge" :style="{ background: paymentStatusColor(o.trangThaiThanhToan).bg, color: paymentStatusColor(o.trangThaiThanhToan).text }">
                        {{ paymentStatusIcon(o.trangThaiThanhToan) }} {{ paymentStatusLabel(o.trangThaiThanhToan) }}
                      </span>
                      <span v-else class="text-secondary">—</span>
                    </td>
                    <td>
                      {{ formatDate(o.ngayDat) }}
                      <div v-if="o.ngayGiaoThucTe" class="text-success" style="font-size:0.72rem;">
                        ✅ {{ t('admin.orderStatusModal.actualDeliveryLabel') }}: {{ formatDateTime(o.ngayGiaoThucTe) }}
                      </div>
                    </td>
                    <td>
                      <div class="d-flex gap-1">
                        <button class="btn btn-sm btn-outline-info"    style="font-size:0.78rem;padding:2px 8px;" @click="openOrderDetail(o)">{{ t('admin.orders.detail') }}</button>
                        <button v-if="NEXT_ORDER_STATUS[o.trangThaiDonHang]" class="btn btn-sm btn-outline-success" style="font-size:0.78rem;padding:2px 8px;" @click="advanceOrderStatus(o)">
                          {{ NEXT_ORDER_STATUS_LABEL[o.trangThaiDonHang].icon }} {{ t(NEXT_ORDER_STATUS_LABEL[o.trangThaiDonHang].key) }}
                        </button>
                        <button v-if="!['delivered','cancelled','returned'].includes(o.trangThaiDonHang)" class="btn btn-sm btn-outline-warning" style="font-size:0.78rem;padding:2px 8px;" @click="openOrderStatus(o)">{{ t('admin.orders.update') }}</button>
                        <button class="btn btn-sm btn-outline-danger"  style="font-size:0.78rem;padding:2px 8px;" @click="deleteOrder(o.donHangId)">{{ t('admin.orders.delete') }}</button>
                      </div>
                    </td>
                  </tr>
                  <tr v-if="filteredOrders.length===0"><td colspan="8" class="text-center text-secondary">{{ t('admin.orders.empty') }}</td></tr>
                </tbody>
              </table>
            </div>
          </template>
        </section>

        <!-- ── Khach hang ── -->
        <section v-show="currentPage === 'customers'">
          <div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
            <span class="text-secondary small">{{ filteredCustomers.length }}/{{ totalCustomers }} {{ t('admin.customers.countSuffix') }}</span>
            <div class="d-flex gap-2 flex-wrap">
              <input v-model="customerSearch" class="form-control form-control-sm" style="width:240px;background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);" :placeholder="t('admin.customers.searchPlaceholder')" />
              <button class="btn btn-sm btn-warning text-dark fw-bold" @click="openAddCustomer">{{ t('admin.customers.add') }}</button>
            </div>
          </div>
          <div v-if="loading" class="text-secondary small">{{ t('admin.customers.loading') }}</div>
          <div v-else class="table-responsive">
            <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
              <thead><tr><th style="width:40px;">{{ t('admin.common.stt') }}</th><th>{{ t('admin.customers.colFullName') }}</th><th>{{ t('admin.customers.colPhone') }}</th><th>{{ t('admin.customers.colEmail') }}</th><th>{{ t('admin.customers.colCustomerType') }}</th><th>{{ t('admin.customers.colPoints') }}</th><th>{{ t('admin.customers.colStatus') }}</th><th>{{ t('admin.customers.colAction') }}</th></tr></thead>
              <tbody>
                <tr v-for="(c, idx) in filteredCustomers" :key="c.khachHangId">
                  <td class="text-secondary">{{ idx + 1 }}</td>
                  <td>{{ c.hoTen }}</td>
                  <td class="text-secondary">{{ c.soDienThoai }}</td>
                  <td class="text-secondary">{{ c.email }}</td>
                  <td>{{ c.loaiKhach||'—' }}</td>
                  <td>{{ c.diemTichLuy??0 }}</td>
                  <td><span class="badge" :class="c.trangThai==='active'?'bg-success':'bg-secondary'">{{ statusLabel(c.trangThai) }}</span></td>
                  <td>
                    <div class="d-flex gap-1">
                      <button class="btn btn-sm btn-outline-warning" style="font-size:0.78rem; padding:2px 8px;" @click="openEditCustomer(c)">{{ t('admin.customers.edit') }}</button>
                      <button class="btn btn-sm btn-outline-danger"  style="font-size:0.78rem; padding:2px 8px;" @click="deleteCustomer(c.khachHangId)">{{ t('admin.customers.delete') }}</button>
                    </div>
                  </td>
                </tr>
                <tr v-if="filteredCustomers.length===0"><td colspan="8" class="text-center text-secondary">{{ t('admin.customers.empty') }}</td></tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- ── Kho hang ── -->
        <section v-show="currentPage === 'inventory'">
          <!-- Tabs -->
          <div class="d-flex gap-2 mb-3">
            <button class="btn btn-sm fw-bold" :class="khoTab==='ton-kho' ? 'btn-warning text-dark' : 'btn-outline-secondary'"
                    @click="khoTab='ton-kho'">📦 {{ t('admin.inventory.tabStock') }}</button>
            <button class="btn btn-sm fw-bold" :class="khoTab==='phieu-nhap' ? 'btn-warning text-dark' : 'btn-outline-secondary'"
                    @click="khoTab='phieu-nhap'; ensurePhieuNhapData()">📋 {{ t('admin.inventory.tabReceipts') }}</button>
            <button class="btn btn-sm fw-bold" :class="khoTab==='bao-hanh' ? 'btn-warning text-dark' : 'btn-outline-secondary'"
                    @click="khoTab='bao-hanh'; ensureWarrantyData()">🛡️ {{ t('admin.inventory.tabWarranty') }}</button>
          </div>

          <!-- ══ TAB: TON KHO ══ -->
          <template v-if="khoTab==='ton-kho'">
          <div class="row g-3 mb-3">
            <div class="col-6 col-xl-3">
              <div class="card border-secondary h-100" style="background:var(--bg-hover);">
                <div class="card-body d-flex align-items-center gap-3">
                  <div class="rounded-3 d-flex align-items-center justify-content-center flex-shrink-0"
                       style="width:44px;height:44px;background:rgba(96,165,250,0.15);font-size:1.3rem;">📦</div>
                  <div>
                    <div class="text-secondary small mb-1">{{ t('admin.inventory.statTotalSku') }}</div>
                    <div class="fw-bold" style="font-size:1.55rem;">{{ inventory.length }}</div>
                  </div>
                </div>
              </div>
            </div>
            <div class="col-6 col-xl-3">
              <div class="card border-secondary h-100" style="background:var(--bg-hover);">
                <div class="card-body d-flex align-items-center gap-3">
                  <div class="rounded-3 d-flex align-items-center justify-content-center flex-shrink-0"
                       style="width:44px;height:44px;background:rgba(52,211,153,0.15);font-size:1.3rem;">📊</div>
                  <div>
                    <div class="text-secondary small mb-1">{{ t('admin.inventory.statTotalStock') }}</div>
                    <div class="fw-bold" style="font-size:1.55rem;">{{ totalStockQty }}</div>
                  </div>
                </div>
              </div>
            </div>
            <div class="col-6 col-xl-3">
              <div class="card border-secondary h-100" style="background:var(--bg-hover);">
                <div class="card-body d-flex align-items-center gap-3">
                  <div class="rounded-3 d-flex align-items-center justify-content-center flex-shrink-0"
                       style="width:44px;height:44px;background:rgba(250,204,21,0.15);font-size:1.3rem;">⚠️</div>
                  <div>
                    <div class="text-secondary small mb-1">{{ t('admin.inventory.statLowStock') }}</div>
                    <div class="fw-bold" :style="lowStockOnlyItems.length?{color:'#facc15'}:{}" style="font-size:1.55rem;">{{ lowStockOnlyItems.length }}</div>
                  </div>
                </div>
              </div>
            </div>
            <div class="col-6 col-xl-3">
              <div class="card border-secondary h-100" style="background:var(--bg-hover);">
                <div class="card-body d-flex align-items-center gap-3">
                  <div class="rounded-3 d-flex align-items-center justify-content-center flex-shrink-0"
                       style="width:44px;height:44px;background:rgba(244,63,94,0.15);font-size:1.3rem;">🚫</div>
                  <div>
                    <div class="text-secondary small mb-1">{{ t('admin.inventory.statOutOfStock') }}</div>
                    <div class="fw-bold" :style="outOfStockItems.length?{color:'#f87171'}:{}" style="font-size:1.55rem;">{{ outOfStockItems.length }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Summary + Search -->
          <div class="d-flex align-items-center gap-2 mb-3 flex-wrap">
            <span class="text-secondary small">{{ t('admin.inventory.summary', { groups: inventoryGrouped.length, skus: inventory.length }) }}</span>
            <span v-if="outOfStockItems.length" class="badge" style="background:rgba(244,63,94,0.15);color:#f87171;">🚫 {{ outOfStockItems.length }} {{ t('admin.inventory.outOfStock') }}</span>
            <span v-if="lowStockItems.length" class="badge" style="background:rgba(250,204,21,0.15);color:#facc15;">⚠️ {{ lowStockItems.length }} {{ t('admin.inventory.lowStock') }}</span>
            <button class="btn btn-sm btn-outline-info" style="font-size:0.78rem;padding:2px 10px;" @click="toggleAllGroups">
              {{ allGroupsExpanded ? '▲ ' + t('admin.inventory.collapseAll') : '▼ ' + t('admin.inventory.expandAll') }}
            </button>
            <select v-model="inventoryStatusFilter" class="form-select form-select-sm" style="width:auto;background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);font-size:0.8rem;">
              <option value="all">{{ t('admin.inventory.filterAll') }}</option>
              <option value="out">{{ t('admin.inventory.filterOut') }}</option>
              <option value="low">{{ t('admin.inventory.filterLow') }}</option>
              <option value="ok">{{ t('admin.inventory.filterOk') }}</option>
            </select>
            <div class="ms-auto" style="min-width:200px;">
              <input v-model="inventorySearch" class="form-control form-control-sm"
                     :placeholder="t('admin.inventory.searchPlaceholder')"
                     style="background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);font-size:0.82rem;" />
            </div>
          </div>

          <div v-if="loading" class="text-secondary small py-4 text-center">{{ t('admin.inventory.loading') }}</div>
          <div v-else class="d-flex flex-column gap-2">

            <div v-for="group in inventoryGrouped" :key="group.name"
                 class="rounded-3 overflow-hidden"
                 style="background:var(--bg-card);border:1px solid var(--border-color);">

              <!-- Product header -->
              <div class="d-flex align-items-center px-3 py-2 gap-3"
                   style="cursor:pointer;transition:background 0.15s;"
                   @mouseenter="$event.currentTarget.style.background='var(--bg-hover)'"
                   @mouseleave="$event.currentTarget.style.background=''"
                   @click="toggleGroup(group.name)">
                <img v-if="group.hinhAnh" :src="group.hinhAnh"
                     style="width:44px;height:36px;object-fit:contain;border-radius:4px;background:var(--bg-card-inset);flex-shrink:0;" />
                <div v-else style="width:44px;height:36px;background:var(--bg-input);border-radius:4px;flex-shrink:0;display:flex;align-items:center;justify-content:center;font-size:1rem;">💻</div>
                <div class="flex-grow-1 min-width-0">
                  <div class="fw-semibold" style="font-size:0.88rem;color:var(--text-heading);">{{ group.name }}</div>
                  <div class="text-secondary" style="font-size:0.72rem;">
                    {{ group.thuongHieu ? group.thuongHieu + ' · ' : '' }}{{ group.items.length }} {{ t('admin.inventory.totalStockLabel') }} <strong :class="group.totalTon===0?'text-danger':group.totalTon<5?'text-warning':'text-success'">{{ group.totalTon }}</strong>
                  </div>
                </div>
                <div class="d-flex align-items-center gap-2">
                  <span v-if="group.outCount" class="badge" style="font-size:0.7rem;background:rgba(244,63,94,0.15);color:#f87171;">🚫 {{ group.outCount }} {{ t('admin.inventory.outOfStock') }}</span>
                  <span v-else-if="group.lowCount" class="badge" style="font-size:0.7rem;background:rgba(250,204,21,0.15);color:#facc15;">⚠️ {{ group.lowCount }} {{ t('admin.inventory.lowStock') }}</span>
                  <span v-else class="badge" style="font-size:0.7rem;background:rgba(34,197,94,0.15);color:#22c55e;">✅ {{ t('admin.inventory.ok') }}</span>
                  <span class="text-secondary" style="font-size:0.75rem;width:12px;text-align:center;">{{ expandedGroups[group.name] ? '▲' : '▼' }}</span>
                </div>
              </div>

              <!-- Variant detail table -->
              <div v-if="expandedGroups[group.name]" style="border-top:1px solid var(--border-color-soft);">
                <table class="w-100" style="border-collapse:collapse;font-size:0.8rem;">
                  <thead>
                    <tr style="background:var(--bg-input);">
                      <th class="px-3 py-2 text-secondary" style="font-weight:500;width:22%;">{{ t('admin.inventory.colSku') }}</th>
                      <th class="px-3 py-2 text-secondary" style="font-weight:500;">{{ t('admin.inventory.colConfig') }}</th>
                      <th class="px-3 py-2 text-secondary text-center" style="font-weight:500;width:80px;">{{ t('admin.inventory.colStock') }}</th>
                      <th class="px-3 py-2 text-secondary text-center" style="font-weight:500;width:70px;">{{ t('admin.inventory.colHeld') }}</th>
                      <th class="px-3 py-2 text-secondary text-center" style="font-weight:500;width:80px;">{{ t('admin.inventory.colMinStock') }}</th>
                      <th class="px-3 py-2 text-secondary" style="font-weight:500;width:90px;"></th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in group.items" :key="item.tonKhoId"
                        style="border-top:1px solid var(--border-color-soft);">
                      <td class="px-3 py-2 text-secondary" style="font-family:monospace;font-size:0.73rem;">{{ item.bienThe?.maSku || '—' }}</td>
                      <td class="px-3 py-2">
                        <div class="d-flex gap-1 flex-wrap">
                          <span v-if="getVariantInfo(item)?.cpu" class="badge" style="background:#2a2a3a;color:#aab;font-size:0.68rem;">{{ getVariantInfo(item).cpu }}</span>
                          <span v-if="getVariantInfo(item)?.ram" class="badge" style="background:#2a3a2a;color:#aba;font-size:0.68rem;">{{ getVariantInfo(item).ram }}</span>
                          <span v-if="getVariantInfo(item)?.oCung" class="badge" style="background:#3a2a2a;color:#baa;font-size:0.68rem;">{{ getVariantInfo(item).oCung }}</span>
                          <span v-if="getVariantInfo(item)?.mauSac" class="badge" style="background:#2a2a2a;color:#999;font-size:0.68rem;">{{ getVariantInfo(item).mauSac }}</span>
                        </div>
                      </td>
                      <td class="px-3 py-2 text-center">
                        <span :class="stockClass(item)" class="fw-bold" style="font-size:0.88rem;">{{ item.soLuongTon ?? '—' }}</span>
                      </td>
                      <td class="px-3 py-2 text-center text-secondary">{{ item.soLuongGiu ?? 0 }}</td>
                      <td class="px-3 py-2 text-center text-secondary">{{ item.tonKhoToiThieu ?? '—' }}</td>
                      <td class="px-3 py-2">
                        <div class="d-flex gap-1">
                          <button class="btn btn-sm btn-outline-info"
                                  style="font-size:0.72rem;padding:2px 8px;"
                                  @click.stop="openStockDetail(item)">🔍 {{ t('admin.inventory.detail') }}</button>
                          <button class="btn btn-sm btn-outline-warning"
                                  style="font-size:0.72rem;padding:2px 8px;"
                                  @click.stop="openEditStock(item)">✏️ {{ t('admin.inventory.update') }}</button>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <div v-if="inventoryGrouped.length === 0" class="text-secondary small text-center py-5">{{ t('admin.inventory.empty') }}</div>
          </div>
          </template>

          <!-- ══ TAB: PHIEU NHAP ══ -->
          <template v-else-if="khoTab==='phieu-nhap'">
          <div class="row g-3 mb-3">
            <div class="col-6 col-xl-3">
              <div class="card border-secondary h-100" style="background:var(--bg-hover);">
                <div class="card-body d-flex align-items-center gap-3">
                  <div class="rounded-3 d-flex align-items-center justify-content-center flex-shrink-0"
                       style="width:44px;height:44px;background:rgba(167,139,250,0.15);font-size:1.3rem;">📋</div>
                  <div>
                    <div class="text-secondary small mb-1">{{ t('admin.phieuNhap.statTotal') }}</div>
                    <div class="fw-bold" style="font-size:1.55rem;">{{ phieuNhapCounts.total }}</div>
                  </div>
                </div>
              </div>
            </div>
            <div class="col-6 col-xl-3">
              <div class="card border-secondary h-100" style="background:var(--bg-hover);">
                <div class="card-body d-flex align-items-center gap-3">
                  <div class="rounded-3 d-flex align-items-center justify-content-center flex-shrink-0"
                       style="width:44px;height:44px;background:rgba(250,204,21,0.15);font-size:1.3rem;">⏳</div>
                  <div>
                    <div class="text-secondary small mb-1">{{ t('admin.phieuNhap.statPending') }}</div>
                    <div class="fw-bold" :style="phieuNhapCounts.choDuyet?{color:'#facc15'}:{}" style="font-size:1.55rem;">{{ phieuNhapCounts.choDuyet }}</div>
                  </div>
                </div>
              </div>
            </div>
            <div class="col-6 col-xl-3">
              <div class="card border-secondary h-100" style="background:var(--bg-hover);">
                <div class="card-body d-flex align-items-center gap-3">
                  <div class="rounded-3 d-flex align-items-center justify-content-center flex-shrink-0"
                       style="width:44px;height:44px;background:rgba(34,197,94,0.15);font-size:1.3rem;">✅</div>
                  <div>
                    <div class="text-secondary small mb-1">{{ t('admin.phieuNhap.statDone') }}</div>
                    <div class="fw-bold" style="font-size:1.55rem;color:#22c55e;">{{ phieuNhapCounts.hoanThanh }}</div>
                  </div>
                </div>
              </div>
            </div>
            <div class="col-6 col-xl-3">
              <div class="card border-secondary h-100" style="background:var(--bg-hover);">
                <div class="card-body d-flex align-items-center gap-3">
                  <div class="rounded-3 d-flex align-items-center justify-content-center flex-shrink-0"
                       style="width:44px;height:44px;background:rgba(244,63,94,0.15);font-size:1.3rem;">❌</div>
                  <div>
                    <div class="text-secondary small mb-1">{{ t('admin.phieuNhap.statCancelled') }}</div>
                    <div class="fw-bold" :style="phieuNhapCounts.huy?{color:'#f87171'}:{}" style="font-size:1.55rem;">{{ phieuNhapCounts.huy }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="d-flex align-items-center gap-2 mb-3 flex-wrap">
            <input v-model="phieuNhapSearch" class="form-control form-control-sm" style="max-width:220px;background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);font-size:0.82rem;"
                   :placeholder="t('admin.phieuNhap.searchPlaceholder')" />
            <select v-model="phieuNhapStatusFilter" class="form-select form-select-sm" style="width:auto;background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);font-size:0.8rem;">
              <option value="">{{ t('admin.inventory.filterAll') }}</option>
              <option value="cho_duyet">{{ t('admin.statusLabel.cho_duyet') }}</option>
              <option value="hoan_thanh">{{ t('admin.statusLabel.hoan_thanh') }}</option>
              <option value="huy">{{ t('admin.statusLabel.huy') }}</option>
            </select>
            <div class="ms-auto d-flex gap-2">
              <button class="btn btn-sm btn-outline-danger" @click="printPhieuNhapList">🖨️ {{ t('admin.phieuNhap.printPdf') }}</button>
              <button class="btn btn-sm btn-outline-success" @click="exportPhieuNhapExcel">📥 {{ t('admin.phieuNhap.exportExcel') }}</button>
              <button class="btn btn-sm btn-warning text-dark fw-bold" @click="openAddPhieuNhap">➕ {{ t('admin.phieuNhap.add') }}</button>
            </div>
          </div>

          <div class="table-responsive">
            <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
              <thead><tr>
                <th style="width:40px;">{{ t('admin.common.stt') }}</th>
                <th>{{ t('admin.phieuNhap.colCode') }}</th>
                <th>{{ t('admin.phieuNhap.colDate') }}</th>
                <th>{{ t('admin.phieuNhap.colSupplier') }}</th>
                <th>{{ t('admin.phieuNhap.colStaff') }}</th>
                <th>{{ t('admin.phieuNhap.colTotal') }}</th>
                <th>{{ t('admin.phieuNhap.colStatus') }}</th>
                <th>{{ t('admin.phieuNhap.colAction') }}</th>
              </tr></thead>
              <tbody>
                <tr v-for="(p, idx) in filteredPhieuNhap" :key="p.phieuNhapId">
                  <td class="text-secondary">{{ idx + 1 }}</td>
                  <td class="text-secondary" style="font-family:monospace;">{{ p.maPhieuNhap }}</td>
                  <td>{{ formatDate(p.ngayNhap) }}</td>
                  <td>{{ supplierName(p.nhaCungCapId) }}</td>
                  <td>{{ staffName(p.nhanVienId) }}</td>
                  <td>{{ formatPrice(p.tongTien) }}</td>
                  <td>
                    <span class="badge" :style="{ background: phieuNhapStatusColor(p.trangThai).bg, color: phieuNhapStatusColor(p.trangThai).text }">
                      {{ phieuNhapStatusIcon(p.trangThai) }} {{ statusLabel(p.trangThai) }}
                    </span>
                  </td>
                  <td>
                    <div class="d-flex gap-1">
                      <button class="btn btn-sm btn-outline-info" style="font-size:0.72rem;padding:2px 8px;" @click="openPhieuNhapDetail(p)">🔍 {{ t('admin.phieuNhap.viewDetail') }}</button>
                      <template v-if="p.trangThai==='cho_duyet'">
                        <button class="btn btn-sm btn-outline-success" style="font-size:0.72rem;padding:2px 8px;" @click="updatePhieuNhapStatus(p,'hoan_thanh')">✔️ {{ t('admin.phieuNhap.approve') }}</button>
                        <button class="btn btn-sm btn-outline-danger" style="font-size:0.72rem;padding:2px 8px;" @click="updatePhieuNhapStatus(p,'huy')">✖️ {{ t('admin.phieuNhap.cancel') }}</button>
                        <button class="btn btn-sm btn-outline-warning" style="font-size:0.72rem;padding:2px 8px;" @click="openEditPhieuNhap(p)">✏️ {{ t('admin.phieuNhap.editAction') }}</button>
                        <button class="btn btn-sm btn-outline-danger" style="font-size:0.72rem;padding:2px 8px;" @click="deletePhieuNhap(p.phieuNhapId)">🗑️ {{ t('admin.phieuNhap.deleteAction') }}</button>
                      </template>
                    </div>
                  </td>
                </tr>
                <tr v-if="filteredPhieuNhap.length===0"><td colspan="8" class="text-center text-secondary">{{ t('admin.phieuNhap.empty') }}</td></tr>
              </tbody>
            </table>
          </div>
          </template>

          <!-- ══ TAB: BAO HANH ══ -->
          <template v-else>
          <div class="d-flex align-items-center gap-2 mb-3 flex-wrap">
            <span class="text-secondary small">{{ filteredWarranty.length }} {{ t('admin.warranty.countSuffix') }}</span>
            <span class="badge" style="background:rgba(148,163,184,0.15);color:#94a3b8;font-size:0.72rem;">📅 {{ t('admin.warranty.today') }}: {{ formatDate(new Date()) }}</span>
            <input v-model="warrantySearch" class="form-control form-control-sm ms-auto" style="max-width:260px;background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);font-size:0.82rem;"
                   :placeholder="t('admin.warranty.searchPlaceholder')" />
          </div>
          <div v-if="warrantyLoading" class="text-secondary small text-center py-5">{{ t('admin.warranty.loading') }}</div>
          <div v-else class="table-responsive">
            <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
              <thead><tr>
                <th style="width:40px;">{{ t('admin.common.stt') }}</th>
                <th>{{ t('admin.warranty.colSerial') }}</th>
                <th>{{ t('admin.warranty.colProduct') }}</th>
                <th>{{ t('admin.warranty.colCustomer') }}</th>
                <th>{{ t('admin.warranty.colPhone') }}</th>
                <th>{{ t('admin.warranty.colOrder') }}</th>
                <th>{{ t('admin.warranty.colDelivered') }}</th>
                <th>{{ t('admin.warranty.colExpires') }}</th>
                <th>{{ t('admin.warranty.colRemaining') }}</th>
              </tr></thead>
              <tbody>
                <tr v-for="(w, idx) in filteredWarranty" :key="w.chiTietId">
                  <td class="text-secondary">{{ idx + 1 }}</td>
                  <td class="text-secondary" style="font-family:monospace;">{{ w.soSerial }}</td>
                  <td>{{ w.tenSanPham }} <span class="text-secondary" style="font-size:0.75rem;">({{ w.maSku }})</span></td>
                  <td>{{ w.tenKhachHang }}</td>
                  <td class="text-secondary">{{ w.soDienThoaiKhachHang }}</td>
                  <td class="text-secondary" style="font-family:monospace;">{{ w.maDonHang }}</td>
                  <td>{{ formatDate(w.ngayGiaoThucTe) }}</td>
                  <td>{{ formatDate(w.ngayHetBaoHanh) }}</td>
                  <td>
                    <span class="badge" :style="daysUntilExpiry(w.ngayHetBaoHanh) <= 30
                      ? { background: 'rgba(248,113,113,0.15)', color: '#f87171' }
                      : daysUntilExpiry(w.ngayHetBaoHanh) <= 90
                        ? { background: 'rgba(250,204,21,0.15)', color: '#facc15' }
                        : { background: 'rgba(34,197,94,0.15)', color: '#22c55e' }">
                      {{ t('admin.warranty.daysLeft', { count: daysUntilExpiry(w.ngayHetBaoHanh) }) }}
                    </span>
                  </td>
                </tr>
                <tr v-if="filteredWarranty.length===0"><td colspan="9" class="text-center text-secondary">{{ t('admin.warranty.empty') }}</td></tr>
              </tbody>
            </table>
          </div>
          </template>
        </section>

        <!-- ── Khuyen mai ── -->
        <section v-show="currentPage === 'promotions'">
          <div class="d-flex justify-content-between align-items-center mb-3">
            <span class="text-secondary small">{{ promotions.length }} {{ t('admin.promotions.countSuffix') }}</span>
            <button class="btn btn-sm btn-warning text-dark fw-bold" @click="openAddPromo">{{ t('admin.promotions.add') }}</button>
          </div>
          <div v-if="loading" class="text-secondary small">{{ t('admin.promotions.loading') }}</div>
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
          <div v-if="loading" class="text-secondary small">{{ t('admin.staff.loading') }}</div>
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
          <div class="d-flex flex-wrap align-items-center gap-2 mb-3">
            <button v-for="opt in ['today','week','month','custom']" :key="opt"
                    class="btn btn-sm"
                    :class="reportsDateRange===opt ? 'btn-warning text-dark' : 'btn-outline-secondary'"
                    @click="reportsDateRange=opt">
              {{ t(`admin.reports.dateRange${opt.charAt(0).toUpperCase()}${opt.slice(1)}`) }}
            </button>
            <template v-if="reportsDateRange==='custom'">
              <span class="text-secondary small">{{ t('admin.reports.dateFrom') }}</span>
              <input type="date" v-model="reportsCustomFrom" class="form-control form-control-sm" style="width:auto;background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
              <span class="text-secondary small">{{ t('admin.reports.dateTo') }}</span>
              <input type="date" v-model="reportsCustomTo" class="form-control form-control-sm" style="width:auto;background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
            </template>
          </div>
          <div class="small fw-semibold text-secondary mb-2">{{ t('admin.reports.revenueChartTitle') }}</div>
          <div class="card border-secondary mb-4" style="background:var(--bg-hover);"><div class="card-body">
            <RevenueBarChart :data="reportsRevenueByDay" :empty-text="t('admin.reports.revenueChartEmpty')" />
          </div></div>
          <div class="small fw-semibold text-secondary mb-2">{{ t('admin.reports.ordersByStatus') }}</div>
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
          <div class="small fw-semibold text-secondary mb-2">{{ t('admin.reports.topProducts') }}</div>
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
          <div class="small fw-semibold text-secondary mb-2 mt-4">{{ t('admin.reports.customersTitle') }}</div>
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
          <div class="card border-secondary" style="background:var(--bg-hover); max-width:520px;">
            <div class="card-body">
              <div class="fw-bold mb-3">{{ t('admin.settings.systemInfo') }}</div>
              <div v-for="row in [
                {label:t('admin.settings.systemName'), value:'SAOPhone Admin'},
                {label:t('admin.settings.version'), value:'1.0.0'},
                {label:t('admin.settings.backendApi'), value:'http://localhost:8080'},
                {label:t('admin.settings.database'), value:'SQL Server — QLBanMayTinh'},
              ]" :key="row.label"
                   class="d-flex justify-content-between align-items-center py-2 border-bottom border-secondary small">
                <span class="text-secondary">{{ row.label }}</span>
                <span>{{ row.value }}</span>
              </div>
              <div class="d-flex justify-content-between align-items-center py-2 small">
                <span class="text-secondary">{{ t('admin.settings.status') }}</span>
                <span class="badge bg-success">{{ t('admin.settings.active') }}</span>
              </div>
            </div>
          </div>
        </section>

        <!-- ── Ban hang (POS) ── -->
        <section v-show="currentPage === 'ban-hang'">
          <div class="pos-grid-layout">
            <!-- LEFT: tim kiem + san pham — luon hien, nhan vien duyet duoc binh thuong;
                 chi viec THEM VAO GIO moi bi chan neu chua xac dinh khach hang (xem posOpenSerialPicker) -->
            <div class="d-flex flex-column gap-3 overflow-hidden">
              <input v-model="posSearch" class="form-control form-control-sm"
                     style="background:var(--bg-hover); border-color:var(--border-color-strong); color:var(--text-primary);"
                     :placeholder="t('admin.pos.searchPlaceholder')" />
              <div v-if="loading" class="text-secondary small">{{ t('admin.pos.loading') }}</div>
              <div v-else class="row g-2 overflow-y-auto">
                <div v-for="p in posProducts" :key="p.bienTheId" class="col-6 col-xl-4">
                  <div class="card h-100 border-secondary" style="background:var(--bg-hover);">
                    <div class="d-flex align-items-center justify-content-center" style="height:88px;background:var(--bg-card-inset);">
                      <img v-if="p.hinhAnhChinh" :src="p.hinhAnhChinh" :alt="p.tenSanPham" style="width:100%;height:100%;object-fit:contain;padding:6px;" />
                      <span v-else style="font-size:1.8rem;">💻</span>
                    </div>
                    <div class="card-body p-2 d-flex flex-column gap-1">
                      <div class="fw-semibold small text-light">{{ p.tenSanPham }}</div>
                      <div class="text-secondary" style="font-size:0.76rem;">{{ p.maSku }}</div>
                      <div class="text-secondary" style="font-size:0.75rem;">{{ p.tenThuongHieu }} · {{ p.tenDanhMuc }}</div>
                      <div class="fw-bold text-warning" style="font-size:0.95rem;">{{ formatPrice(p.giaBan) }}</div>
                      <button class="btn btn-sm btn-warning text-dark fw-bold mt-auto" @click="posOpenSerialPicker(p)">{{ t('admin.pos.addToCart') }}</button>
                    </div>
                  </div>
                </div>
                <div v-if="posProducts.length===0" class="col-12 text-center text-secondary small py-4">{{ t('admin.pos.noProductsFound') }}</div>
              </div>
            </div>

            <!-- RIGHT: gio hang POS — cong xac dinh khach hang nam o day -->
            <div class="card border-secondary d-flex flex-column overflow-hidden" style="background:var(--bg-hover);">
              <div class="card-header border-secondary d-flex justify-content-between align-items-center fw-bold">
                <span>{{ t('admin.pos.cart') }} <span class="text-secondary fw-normal small">{{ posCart.length }} {{ t('admin.pos.cartCountSuffix') }}</span></span>
                <button class="btn btn-sm btn-outline-info position-relative" style="font-size:0.72rem;padding:2px 8px;" @click="showHeldOrders = true">
                  {{ t('admin.pos.heldOrders') }}
                  <span v-if="heldOrders.length" class="badge rounded-pill bg-warning text-dark" style="font-size:0.62rem;">{{ heldOrders.length }}</span>
                </button>
              </div>

              <!-- Cong xac dinh khach hang: thay the phan gio hang cho toi khi co khach -->
              <div v-if="posStage !== 'selling'" class="d-flex flex-column align-items-center justify-content-center gap-3 flex-grow-1 text-center p-3">
                <div v-if="posError" class="small p-2 rounded-2 w-100" style="background:rgba(220,53,69,0.1);color:#e05252;">{{ posError }}</div>
                <template v-if="posStage === 'start'">
                  <div style="font-size:2.4rem;">🧾</div>
                  <div class="text-secondary small">{{ t('admin.pos.startHint') }}</div>
                  <button class="btn btn-warning text-dark fw-bold px-4" @click="posStartInvoice">{{ t('admin.pos.startInvoice') }}</button>
                </template>
                <template v-else-if="posStage === 'phone'">
                  <div class="fw-bold text-light">{{ t('admin.pos.enterPhoneTitle') }}</div>
                  <div class="d-flex gap-2 w-100">
                    <input v-model="posPhone" class="form-control form-control-sm" style="background:var(--bg-hover);border-color:var(--border-color-strong);color:var(--text-primary);" :placeholder="t('admin.pos.phonePlaceholder')" @keyup.enter="posLookup" />
                    <button class="btn btn-sm btn-warning text-dark fw-bold flex-shrink-0" @click="posLookup">{{ t('admin.pos.find') }}</button>
                  </div>
                  <div v-if="posPhoneNotFound" class="d-flex flex-column align-items-center gap-2 w-100">
                    <div class="small" style="color:#e05252;">{{ t('admin.pos.customerNotFound') }}</div>
                    <div class="small text-secondary">{{ t('admin.pos.askCreateCustomer') }}</div>
                    <div class="d-flex gap-2">
                      <button class="btn btn-sm btn-outline-secondary" @click="posCancelCreateCustomer">{{ t('admin.pos.no') }}</button>
                      <button class="btn btn-sm btn-warning text-dark fw-bold" @click="posConfirmCreateCustomer">{{ t('admin.pos.yesCreateCustomer') }}</button>
                    </div>
                  </div>
                </template>
              </div>

              <!-- Danh sach san pham trong gio: chi hien khi da xac dinh khach hang -->
              <template v-if="posStage === 'selling'">
              <div class="flex-grow-1 overflow-y-auto p-2 d-flex flex-column gap-1">
                <div v-if="posCart.length===0" class="text-secondary small text-center py-4">{{ t('admin.pos.cartEmptyList') }}</div>
                <div v-for="item in posCart" :key="item.chiTietId"
                     class="d-flex align-items-center gap-2 p-2 rounded-2" style="background:var(--bg-hover);">
                  <div class="d-flex align-items-center justify-content-center flex-shrink-0 rounded-2" style="width:36px;height:36px;background:var(--bg-card-inset);">
                    <img v-if="item.hinhAnhChinh" :src="item.hinhAnhChinh" :alt="item.tenSanPham" style="width:100%;height:100%;object-fit:contain;padding:2px;" />
                    <span v-else style="font-size:1rem;">💻</span>
                  </div>
                  <div class="flex-grow-1" style="min-width:0;">
                    <div class="fw-semibold small text-light text-truncate">{{ item.tenSanPham }}</div>
                    <div class="text-secondary" style="font-size:0.73rem;">{{ item.maSku }}</div>
                    <div class="text-info" style="font-size:0.7rem;">S/N: {{ item.soSerial }}</div>
                  </div>
                  <div class="d-flex align-items-center gap-1 flex-shrink-0">
                    <button class="btn btn-sm btn-outline-danger" style="width:20px;height:20px;padding:0;font-size:0.72rem;"
                            @click="posRemove(item.chiTietId)">✕</button>
                  </div>
                  <div class="text-warning fw-bold flex-shrink-0 text-end" style="font-size:0.8rem;min-width:72px;">{{ formatPrice(item.giaBan*item.soLuong) }}</div>
                </div>
              </div>
              <!-- Ma khuyen mai -->
              <div class="p-2 border-top border-secondary d-flex flex-column gap-1">
                <div class="d-flex gap-2">
                  <input v-model="posPromoCode" class="form-control form-control-sm" style="background:var(--bg-hover);border-color:var(--border-color-strong);color:var(--text-primary);" :placeholder="t('checkout.promoPlaceholder')" @keyup.enter="posApplyPromo" />
                  <button class="btn btn-sm btn-outline-warning flex-shrink-0" @click="posApplyPromo">{{ t('checkout.apply') }}</button>
                </div>
                <div v-if="posPromoMsg" class="small" :class="posAppliedPromo ? 'text-success' : 'text-danger'">{{ posPromoMsg }}</div>
              </div>
              <!-- Tong tien -->
              <div class="p-2 border-top border-secondary d-flex flex-column gap-1">
                <div class="d-flex justify-content-between text-secondary small"><span>{{ t('admin.pos.subtotalLabel') }}</span><span>{{ formatPrice(posCartTotal) }}</span></div>
                <div v-if="posGiamGia > 0" class="d-flex justify-content-between text-success small"><span>{{ t('checkout.discount') }}</span><span>-{{ formatPrice(posGiamGia) }}</span></div>
                <div class="d-flex justify-content-between text-secondary small"><span>{{ t('admin.pos.shippingFeeLabel') }}</span><span>{{ posFee===0?t('admin.pos.free'):formatPrice(posFee) }}</span></div>
                <div class="d-flex justify-content-between fw-bold"><span>{{ t('admin.pos.totalLabel') }}</span><span>{{ formatPrice(posGrandTotal) }}</span></div>
              </div>
              <!-- Khach hang -->
              <div class="p-2 border-top border-secondary d-flex flex-column gap-2">
                <div class="text-uppercase text-secondary fw-bold" style="font-size:0.78rem;letter-spacing:0.04em;">{{ t('admin.pos.customerInfo') }}</div>
                <div v-if="posFoundCust" class="d-flex justify-content-between align-items-center gap-2 small p-2 rounded-2" style="background:rgba(72,199,142,0.1);color:#48c78e;">
                  <span>✓ {{ posFoundCust.hoTen }} · {{ posFoundCust.soDienThoai }}</span>
                  <button class="btn btn-sm btn-link text-secondary p-0" style="font-size:0.7rem;text-decoration:underline;" @click="posReset">{{ t('admin.pos.changeCustomer') }}</button>
                </div>
                <div v-else class="small text-secondary">{{ t('admin.pos.noCustomerYet') }}</div>
                <div v-if="posError" class="small p-2 rounded-2" style="background:rgba(220,53,69,0.1);color:#e05252;">{{ posError }}</div>
                <div v-if="posSuccess" class="small p-2 rounded-2" style="background:rgba(72,199,142,0.1);color:#48c78e;">{{ t('admin.pos.orderCreated') }}</div>
                <div class="d-flex gap-2">
                  <button class="btn btn-sm btn-outline-secondary" @click="posReset">{{ t('admin.pos.reset') }}</button>
                  <button class="btn btn-sm btn-outline-info" :disabled="!posCart.length" @click="posHoldOrder">{{ t('admin.pos.holdOrder') }}</button>
                  <button class="btn btn-sm btn-warning text-dark fw-bold" style="flex:2;" :disabled="posStage !== 'selling' || !posCart.length" @click="posPlaceOrder">{{ t('admin.pos.createOrder') }}</button>
                </div>
              </div>
              </template>
            </div>
          </div>
        </section>

        <!-- ══ MODAL CHON SERIAL (POS) ══ -->
        <div v-if="showSerialPicker" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1070;" @click.self="showSerialPicker=false">
          <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:480px;max-width:95vw;max-height:75vh;">
            <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
              <div>
                <div>{{ t('admin.pos.chooseSerial') }}</div>
                <div class="text-secondary fw-normal" style="font-size:0.75rem;">{{ serialPickerProduct?.tenSanPham }} — {{ serialPickerProduct?.maSku }}</div>
              </div>
              <button class="btn-close btn-close-white btn-sm" @click="showSerialPicker=false"></button>
            </div>
            <div class="overflow-y-auto p-3 d-flex flex-column gap-2">
              <div v-if="serialPickerLoading" class="text-secondary small text-center py-4">{{ t('admin.pos.loading') }}</div>
              <div v-else-if="serialPickerList.length===0" class="text-secondary small text-center py-4">{{ t('admin.pos.noSerialAvailable') }}</div>
              <button v-else v-for="s in serialPickerList" :key="s.chiTietId"
                      class="btn btn-outline-warning d-flex justify-content-between align-items-center"
                      style="font-family:monospace;font-size:0.85rem;"
                      @click="posSelectSerial(s)">
                <span>{{ s.soSerial }}</span>
                <span class="text-secondary" style="font-size:0.7rem;">{{ formatDate(s.ngayNhapKho) }}</span>
              </button>
            </div>
          </div>
        </div>

        <!-- ══ MODAL CHỌN SERIAL TRƯỚC KHI XÁC NHẬN ══ -->
        <div v-if="showXacNhanSerialModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1070;" @click.self="showXacNhanSerialModal=false">
          <div class="rounded-3 p-3" style="background:var(--bg-card);width:520px;max-height:85vh;overflow-y:auto;">
            <div class="d-flex justify-content-between align-items-center mb-3">
              <div>
                <div class="fw-bold" style="color:var(--text-heading);">{{ t('admin.packModal.title') }}</div>
                <div class="text-secondary" style="font-size:0.75rem;">{{ xacNhanOrder?.maDonHang }}</div>
              </div>
              <button class="btn-close btn-sm" @click="showXacNhanSerialModal=false"></button>
            </div>

            <div v-if="xacNhanLoading" class="text-secondary small text-center py-4">{{ t('admin.packModal.loading') }}</div>
            <div v-else>
              <div v-if="xacNhanError" class="alert alert-danger py-2 small">{{ xacNhanError }}</div>
              <div v-for="line in xacNhanLines" :key="line.id" class="mb-3 p-2 rounded-2" style="background:var(--bg-card-inset);">
                <div class="d-flex justify-content-between mb-1">
                  <span class="text-light">{{ productByBienThe(line.bienTheId)?.tenSanPham || line.maSku }}</span>
                  <span class="text-secondary" style="font-size:0.75rem;">{{ t('admin.packModal.selectedCount', { selected: line.chosenSerialIds.size, count: line.soLuong }) }}</span>
                </div>
                <div v-if="xacNhanAvailableSerials(line).length === 0" class="text-danger small">{{ t('admin.packModal.noSerialAvailable') }}</div>
                <div v-else class="d-flex flex-wrap gap-2">
                  <button v-for="s in xacNhanAvailableSerials(line)" :key="s.chiTietId"
                          class="btn btn-sm"
                          :class="line.chosenSerialIds.has(s.chiTietId) ? 'btn-warning text-dark' : 'btn-outline-secondary'"
                          style="font-family:monospace;font-size:0.75rem;"
                          @click="xacNhanToggleSerial(line, s.chiTietId)">
                    {{ s.soSerial }}
                  </button>
                </div>
              </div>
            </div>

            <div class="d-flex justify-content-end gap-2 mt-3">
              <button class="btn btn-sm btn-outline-secondary" @click="showXacNhanSerialModal=false">{{ t('admin.packModal.cancel') }}</button>
              <button class="btn btn-sm btn-success" :disabled="!xacNhanAllLinesComplete || xacNhanLoading" @click="confirmXacNhanSerial">{{ t('admin.packModal.confirm') }}</button>
            </div>
          </div>
        </div>

        <!-- ══ MODAL DON DANG GIU (POS) ══ -->
        <div v-if="showHeldOrders" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1070;" @click.self="showHeldOrders=false">
          <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:520px;max-width:95vw;max-height:80vh;">
            <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
              <span>{{ t('admin.pos.heldOrders') }}</span>
              <button class="btn-close btn-close-white btn-sm" @click="showHeldOrders=false"></button>
            </div>
            <div class="overflow-y-auto p-3 d-flex flex-column gap-2">
              <div v-if="heldOrders.length===0" class="text-secondary small text-center py-4">{{ t('admin.pos.noHeldOrders') }}</div>
              <div v-for="h in heldOrders" :key="h.id" class="rounded-3 p-2 d-flex justify-content-between align-items-center" style="background:var(--bg-hover);border:1px solid var(--border-color-soft);">
                <div>
                  <div class="small text-light">{{ h.foundCust?.hoTen ?? (h.newName || h.phone || t('admin.pos.walkInCustomer')) }}</div>
                  <div class="text-secondary" style="font-size:0.72rem;">{{ h.cart.length }} {{ t('admin.pos.cartCountSuffix') }} · {{ formatDate(h.heldAt) }}</div>
                </div>
                <div class="d-flex gap-1">
                  <button class="btn btn-sm btn-warning text-dark fw-bold" style="font-size:0.72rem;padding:2px 8px;" @click="posResumeHeld(h.id)">{{ t('admin.pos.resume') }}</button>
                  <button class="btn btn-sm btn-outline-danger" style="font-size:0.72rem;padding:2px 8px;" @click="posDeleteHeld(h.id)">{{ t('admin.products.delete') }}</button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- ── Trang placeholder cho nhan vien ── -->
        <section v-show="['user-home','user-orders','user-browse','user-warranty','user-profile'].includes(currentPage)"
                 class="flex-column align-items-center justify-content-center text-secondary"
                 style="display:flex;min-height:300px;gap:12px;">
          <div style="font-size:2.8rem;">&#128101;</div>
          <div class="fw-bold" style="color:var(--text-muted);font-size:1.15rem;">{{ topbarTitle }}</div>
          <div style="font-size:0.83rem;">{{ t('admin.placeholder.userPageNote') }}</div>
        </section>

      </div><!-- /content -->
    </main>
  </div><!-- /dashboard-shell -->

  <!-- ══ MODAL SAN PHAM ══ -->
  <div v-if="showProductModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="showProductModal=false">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:860px;max-width:96vw;max-height:92vh;">

      <!-- Header -->
      <div class="d-flex justify-content-between align-items-center px-4 py-3" style="border-bottom:1px solid var(--border-color);">
        <div>
          <div class="fw-bold text-light" style="font-size:1rem;">{{ addVariantMode ? t('admin.variantModal.addVariant') : (editingId ? t('admin.productModal.titleEdit') : t('admin.productModal.titleAdd')) }}</div>
          <div v-if="addVariantMode" class="text-secondary" style="font-size:0.72rem;margin-top:2px;">{{ addVariantSanPhamName }}</div>
          <div v-else-if="editingId" class="text-secondary" style="font-size:0.72rem;margin-top:2px;">{{ t('admin.productModal.idLabel') }} {{ editingId }}</div>
        </div>
        <button class="btn-close btn-close-white btn-sm" @click="showProductModal=false"></button>
      </div>

      <!-- Body -->
      <div class="overflow-y-auto px-4 py-3" style="gap:0;">
        <div v-if="formError" class="alert alert-danger small py-2 mb-3">{{ formError }}</div>

        <!-- ── Thong tin co ban ── -->
        <div class="text-uppercase fw-bold mb-2" style="font-size:0.65rem;letter-spacing:0.1em;color:#60a5fa;">{{ t('admin.productModal.sectionBasic') }}</div>
        <div class="rounded-3 p-3 mb-3" style="background:var(--bg-input);border:1px solid var(--border-color);">
          <div class="row g-3">
            <div class="col-8" v-if="!addVariantMode">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.nameLabel') }}</label>
              <input v-model="form.tenSanPham" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" :placeholder="t('admin.productModal.namePlaceholder')" />
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.skuLabel') }}</label>
              <input v-model="form.maSku" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong); font-family:monospace;" :placeholder="t('admin.productModal.skuPlaceholder')" />
            </div>
            <div class="col-3" v-if="!addVariantMode">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.typeLabel') }}</label>
              <select v-model="form.loaiSanPham" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)">
                <option value="" disabled>{{ t('admin.productModal.selectPlaceholder') }}</option>
                <option value="LAPTOP">{{ t('admin.productModal.typeLaptop') }}</option>
                <option value="PHU_KIEN">{{ t('admin.productModal.typeAccessory') }}</option>
              </select>
            </div>
            <div class="col-3">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.statusLabel') }}</label>
              <select v-model="form.trangThai" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)">
                <option value="active">{{ t('admin.productModal.statusActive') }}</option>
                <option value="inactive">{{ t('admin.productModal.statusInactive') }}</option>
                <option value="ngung_kin_doanh">{{ t('admin.productModal.statusDiscontinued') }}</option>
              </select>
            </div>
            <div class="col-3">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.colorLabel') }}</label>
              <input v-model="form.mauSac" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" :placeholder="t('admin.productModal.colorPlaceholder')" />
            </div>
            <div class="col-3">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.warrantyLabel') }}</label>
              <input v-model="form.baoHanhThang" type="number" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" />
            </div>
            <template v-if="!addVariantMode">
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.brandLabel') }}</label>
              <select v-model="form.thuongHieuId" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)">
                <option :value="null" disabled>{{ t('admin.productModal.selectPlaceholder') }}</option>
                <option v-for="b in brands" :key="b.thuongHieuId" :value="b.thuongHieuId">{{ b.tenThuongHieu }}</option>
              </select>
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.categoryLabel') }}</label>
              <select v-model="form.danhMucId" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)">
                <option :value="null" disabled>{{ t('admin.productModal.selectPlaceholder') }}</option>
                <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.tenDanhMuc }}</option>
              </select>
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.supplierLabel') }}</label>
              <select v-model="form.nhaCungCapId" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)">
                <option :value="null">{{ t('admin.productModal.noneOption') }}</option>
                <option v-for="s in suppliers" :key="s.nhaCungCapId" :value="s.nhaCungCapId">{{ s.tenNhaCungCap }}</option>
              </select>
            </div>
            </template>
          </div>
        </div>

        <!-- ── Cau hinh ky thuat ── -->
        <div class="text-uppercase fw-bold mb-2" style="font-size:0.65rem;letter-spacing:0.1em;color:#60a5fa;">{{ t('admin.productModal.sectionTech') }}</div>
        <div class="rounded-3 p-3 mb-3" style="background:var(--bg-input);border:1px solid var(--border-color);">
          <div class="row g-3">
            <div class="col-6">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.cpuLabel') }}</label>
              <select v-model="form.cpuId" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)">
                <option :value="null">{{ t('admin.productModal.noneOption') }}</option>
                <option v-for="c in cpuList" :key="c.cpuId" :value="c.cpuId">{{ c.tenCpu }}</option>
              </select>
            </div>
            <div class="col-6">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.gpuLabel') }}</label>
              <select v-model="form.gpuId" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)">
                <option :value="null">{{ t('admin.productModal.noneOption') }}</option>
                <option v-for="g in gpuList" :key="g.gpuId" :value="g.gpuId">{{ g.tenGpu }}</option>
              </select>
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.ramLabel') }}</label>
              <select v-model="form.ramId" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)">
                <option :value="null">{{ t('admin.productModal.noneOption') }}</option>
                <option v-for="r in ramList" :key="r.ramId" :value="r.ramId">{{ r.dungLuong }}</option>
              </select>
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.storageLabel') }}</label>
              <select v-model="form.oCungId" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)">
                <option :value="null">{{ t('admin.productModal.noneOption') }}</option>
                <option v-for="o in oCungList" :key="o.oCungId" :value="o.oCungId">{{ o.loaiOcung }}</option>
              </select>
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.screenLabel') }}</label>
              <input v-model="form.kichThuocManHinh" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" :placeholder="t('admin.productModal.screenPlaceholder')" />
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.osLabel') }}</label>
              <input v-model="form.heDieuHanh" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" :placeholder="t('admin.productModal.osPlaceholder')" />
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.batteryLabel') }}</label>
              <input v-model="form.pin" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" :placeholder="t('admin.productModal.batteryPlaceholder')" />
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.weightLabel') }}</label>
              <input v-model="form.trongLuongKg" type="number" step="0.1" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" />
            </div>
          </div>
        </div>

        <!-- ── Gia ca ── -->
        <div class="text-uppercase fw-bold mb-2" style="font-size:0.65rem;letter-spacing:0.1em;color:#60a5fa;">{{ t('admin.productModal.sectionPrice') }}</div>
        <div class="rounded-3 p-3 mb-3" style="background:var(--bg-input);border:1px solid var(--border-color);">
          <div class="row g-3">
            <div class="col-6">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.priceSellLabel') }}</label>
              <input v-model="form.giaBan" type="number" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" />
            </div>
            <div class="col-6">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.priceBuyLabel') }}</label>
              <input v-model="form.giaNhap" type="number" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" />
            </div>
          </div>
        </div>

        <!-- ── Hinh anh, mo ta, phan loai ── -->
        <div class="text-uppercase fw-bold mb-2" style="font-size:0.65rem;letter-spacing:0.1em;color:#60a5fa;">{{ t('admin.productModal.sectionMedia') }}</div>
        <div class="rounded-3 p-3 mb-3" style="background:var(--bg-input);border:1px solid var(--border-color);">
          <div class="row g-3">
            <div class="col-12">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.imageLabel') }}</label>
              <div class="d-flex align-items-center gap-3">
                <label class="d-flex flex-column align-items-center justify-content-center rounded-3 border border-secondary text-secondary" style="width:110px;height:88px;cursor:pointer;flex-shrink:0;overflow:hidden;background:var(--bg-card-inset);">
                  <img v-if="imagePreview" :src="imagePreview" style="width:110px;height:88px;object-fit:contain;" />
                  <template v-else>
                    <span style="font-size:1.4rem;">&#128247;</span>
                    <span style="font-size:0.68rem;margin-top:4px;">{{ t('admin.productModal.imageClickToChoose') }}</span>
                  </template>
                  <input type="file" accept="image/*" class="d-none" @change="handleImageFile" />
                </label>
                <div v-if="imageFilePending" class="text-warning" style="font-size:0.75rem;">{{ imageFilePending.name }}</div>
                <div v-else class="text-secondary" style="font-size:0.75rem;">{{ t('admin.productModal.imageFormats') }}</div>
              </div>
            </div>
            <template v-if="!addVariantMode">
            <div class="col-12">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.descLabel') }}</label>
              <textarea v-model="form.moTa" rows="3" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)"></textarea>
            </div>
            <div class="col-6">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.tagsLabel') }} <span class="text-warning small">{{ t('admin.productModal.tagsHint') }}</span></label>
              <div class="d-flex flex-wrap gap-2">
                <button v-for="opt in PHAN_LOAI_TAG_OPTIONS" :key="opt.value" type="button"
                        class="btn btn-sm" :class="isTagSelected(opt.value) ? 'btn-warning text-dark fw-bold' : 'btn-outline-secondary'"
                        style="font-size:0.75rem;padding:3px 12px;border-radius:999px;"
                        @click="toggleTag(opt.value)">{{ opt.label }}</button>
              </div>
            </div>
            <div class="col-6">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.tagNameLabel') }} <span class="text-muted small">{{ t('admin.productModal.tagNameHint') }}</span></label>
              <input v-model="form.phanLoaiTen" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" :placeholder="t('admin.productModal.tagNamePlaceholder')" />
            </div>
            </template>
          </div>
        </div>

        <!-- ── Serial (chi khi them moi) ── -->
        <div v-if="!editingId">
          <div class="text-uppercase fw-bold mb-2" style="font-size:0.65rem;letter-spacing:0.1em;color:var(--accent-fg);">{{ t('admin.productModal.sectionSerial') }}</div>
          <div class="rounded-3 p-3" style="background:var(--bg-input);border:1px solid var(--border-color);">
            <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.serialLabel') }} <span class="text-danger">*</span></label>
            <input v-model="soSerialMoi" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong); font-family:monospace;" :placeholder="t('admin.productModal.serialPlaceholder')" />
            <div class="text-secondary mt-1" style="font-size:0.72rem;">{{ t('admin.productModal.serialHint') }}</div>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div class="d-flex justify-content-end gap-2 px-4 py-3" style="border-top:1px solid var(--border-color);">
        <button class="btn btn-sm btn-outline-secondary px-3" @click="showProductModal=false">{{ t('admin.productModal.cancel') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold px-4" @click="saveProduct">{{ addVariantMode ? t('admin.variantModal.addVariant') : (editingId ? t('admin.productModal.update') : t('admin.productModal.addNew')) }}</button>
      </div>
    </div>
  </div>

  <!-- ══ MODAL BIEN THE SAN PHAM ══ -->
  <div v-if="showVariantModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1050;" @click.self="showVariantModal=false">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:960px;max-width:96vw;max-height:90vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>{{ t('admin.variantModal.titlePrefix') }} {{ variantModalName }}</span>
        <button class="btn-close btn-close-white btn-sm" @click="showVariantModal=false"></button>
      </div>
      <div class="overflow-y-auto p-3">
        <!-- moi bien the la 1 card -->
        <div v-for="v in variantModalList" :key="v.bienTheId" class="mb-3 rounded-3 overflow-hidden" style="border:1px solid var(--border-color);">
          <!-- header bien the -->
          <div class="d-flex align-items-center gap-3 px-3 py-2 justify-content-between" style="background:var(--bg-input);">
            <div class="d-flex align-items-center gap-3 flex-wrap">
              <span class="text-secondary" style="font-size:0.75rem;font-family:monospace;">{{ v.maSku }}</span>
              <span class="badge rounded-pill" style="font-size:0.72rem;background:var(--border-color-strong);color:var(--text-primary);">{{ v.mauSac }}</span>
              <span class="text-light" style="font-size:0.82rem;">{{ v.cpu }} · {{ v.ram }} · {{ v.oCung }}</span>
              <span class="text-secondary" style="font-size:0.8rem;">{{ v.kichThuocManHinh }}</span>
              <span class="fw-bold text-warning" style="font-size:0.88rem;">{{ formatPrice(v.giaBan) }}</span>
              <span class="badge" :class="v.trangThai==='active'?'bg-success':'bg-secondary'" style="font-size:0.7rem;">{{ statusLabel(v.trangThai) }}</span>
            </div>
            <div class="d-flex gap-2 flex-shrink-0">
              <button class="btn btn-sm btn-outline-warning" style="font-size:0.75rem;padding:2px 8px;" @click="showVariantModal=false; openEdit(v)">{{ t('admin.variantModal.edit') }}</button>
              <button class="btn btn-sm btn-outline-danger" style="font-size:0.75rem;padding:2px 8px;" @click="deleteVariant(v.bienTheId)">{{ t('admin.products.delete') }}</button>
            </div>
          </div>

        </div>
      </div>
    </div>
  </div>

  <!-- ══ MODAL CHI TIET SAN PHAM ══ -->
  <div v-if="showDetailModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1060;" @click.self="showDetailModal=false">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:1100px;max-width:97vw;max-height:92vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>{{ t('admin.detailModal.titlePrefix') }} {{ detailModalName }}</span>
        <div class="d-flex align-items-center gap-2">
          <button class="btn btn-sm btn-warning text-dark fw-bold" style="font-size:0.78rem;" @click="openAddVariant">{{ t('admin.variantModal.addVariant') }}</button>
          <button class="btn-close btn-close-white btn-sm" @click="showDetailModal=false"></button>
        </div>
      </div>
      <div class="overflow-y-auto p-3">
        <div v-for="v in detailModalList" :key="v.bienTheId" class="mb-4 rounded-3 overflow-hidden" style="border:1px solid var(--border-color);">
          <!-- Header bien the -->
          <div class="d-flex align-items-center justify-content-between gap-3 p-3" style="background:var(--bg-input);">
            <div class="d-flex align-items-center gap-3">
              <img v-if="v.hinhAnhChinh" :src="v.hinhAnhChinh" style="width:72px;height:54px;object-fit:contain;background:var(--bg-card-inset);border-radius:6px;padding:4px;" />
              <span v-else style="font-size:2rem;width:72px;text-align:center;">💻</span>
              <div>
                <div class="fw-bold text-light" style="font-size:0.95rem;">{{ v.tenSanPham }}</div>
                <div class="text-secondary" style="font-size:0.75rem;font-family:monospace;">{{ v.maSku }}</div>
              </div>
            </div>
            <div class="d-flex gap-2 flex-shrink-0">
              <button class="btn btn-sm btn-outline-warning" style="font-size:0.75rem;padding:3px 12px;" @click="showDetailModal=false; openEdit(v)">{{ t('admin.detailModal.edit') }}</button>
              <button class="btn btn-sm btn-outline-danger" style="font-size:0.75rem;padding:3px 12px;" @click="deleteVariant(v.bienTheId)">{{ t('admin.products.delete') }}</button>
            </div>
          </div>
          <!-- Bang thong tin 4 cot (label | value | label | value) -->
          <table class="w-100 mb-0" style="border-collapse:collapse;font-size:0.8rem;">
            <tbody>
              <tr style="border-top:1px solid var(--border-color-soft);">
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.brand') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ v.tenThuongHieu }}</td>
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.category') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ v.tenDanhMuc }}</td>
              </tr>
              <tr style="border-top:1px solid var(--border-color-soft);">
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.supplier') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ v.tenNhaCungCap }}</td>
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.productType') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ v.loaiSanPham }}</td>
              </tr>
              <tr style="border-top:1px solid var(--border-color-soft);">
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.priceSell') }}</td>
                <td class="px-3 py-1 fw-bold" style="background:var(--bg-card);color:var(--accent-fg);">{{ formatPrice(v.giaBan) }}</td>
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.priceBuy') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ formatPrice(v.giaNhap) }}</td>
              </tr>
              <tr style="border-top:1px solid var(--border-color-soft);">
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.warranty') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ v.baoHanhThang ? v.baoHanhThang + ' ' + t('admin.detailModal.months') : '—' }}</td>
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.color') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ v.mauSac }}</td>
              </tr>
              <tr style="border-top:1px solid var(--border-color-soft);">
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.status') }}</td>
                <td class="px-3 py-1" style="background:var(--bg-card);">
                  <span class="badge" :class="v.trangThai==='active'?'bg-success':'bg-secondary'" style="font-size:0.7rem;">{{ statusLabel(v.trangThai) }}</span>
                </td>
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.cpu') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ v.cpu || '—' }}</td>
              </tr>
              <tr style="border-top:1px solid var(--border-color-soft);">
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.ram') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ v.ram || '—' }}</td>
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.storage') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ v.oCung || '—' }}</td>
              </tr>
              <tr style="border-top:1px solid var(--border-color-soft);">
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.gpu') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ v.gpu || '—' }}</td>
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.screen') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ v.kichThuocManHinh || '—' }}</td>
              </tr>
              <tr style="border-top:1px solid var(--border-color-soft);">
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.os') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ v.heDieuHanh || '—' }}</td>
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.battery') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ v.pin || '—' }}</td>
              </tr>
              <tr style="border-top:1px solid var(--border-color-soft);">
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.weight') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ v.trongLuongKg ? v.trongLuongKg + ' ' + t('admin.detailModal.kg') : '—' }}</td>
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.classification') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ v.phanLoaiTen || '—' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>

  <!-- ══ MODAL KHACH HANG ══ -->
  <div v-if="showCustomerModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="closeCustomerModal()">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:560px;max-width:95vw;max-height:90vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>{{ editingCustomerId?t('admin.customerModal.titleEdit'):t('admin.customerModal.titleAdd') }}</span>
        <button class="btn-close btn-close-white btn-sm" @click="closeCustomerModal()"></button>
      </div>
      <div class="overflow-y-auto p-4">
        <div v-if="customerFormError" class="alert alert-danger small py-2 mb-3">{{ customerFormError }}</div>
        <div class="row g-3">
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.customerModal.fullNameLabel') }}</label><input v-model="customerForm.hoTen" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.customerModal.phoneLabel') }}</label><input v-model="customerForm.soDienThoai" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.customerModal.emailLabel') }}</label><input v-model="customerForm.email" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.customerModal.typeLabel') }}</label><select v-model="customerForm.loaiKhach" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)"><option value="ca_nhan">{{ t('admin.customerModal.typePersonal') }}</option><option value="doanh_nghiep">{{ t('admin.customerModal.typeBusiness') }}</option></select></div>
          <div class="col-12"><label class="form-label small text-secondary">{{ t('admin.customerModal.addressLabel') }}</label><input v-model="customerForm.diaChi" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.customerModal.companyNameLabel') }}</label><input v-model="customerForm.tenCongTy" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.customerModal.taxCodeLabel') }}</label><input v-model="customerForm.maSoThue" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.customerModal.pointsLabel') }}</label><input v-model="customerForm.diemTichLuy" type="number" min="0" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.customerModal.statusLabel') }}</label><select v-model="customerForm.trangThai" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)"><option value="active">{{ t('admin.customerModal.statusActive') }}</option><option value="inactive">{{ t('admin.customerModal.statusLocked') }}</option></select></div>
        </div>
      </div>
      <div class="d-flex justify-content-end gap-2 p-3 border-top border-secondary">
        <button class="btn btn-sm btn-outline-secondary" @click="closeCustomerModal()">{{ t('admin.customerModal.cancel') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" @click="saveCustomer">{{ editingCustomerId?t('admin.customerModal.update'):t('admin.customerModal.addNew') }}</button>
      </div>
    </div>
  </div>

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

  <!-- ══ MODAL TAO PHIEU NHAP ══ -->
  <div v-if="showPhieuNhapModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="showPhieuNhapModal=false">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:860px;max-width:96vw;max-height:90vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>{{ editingPhieuNhapId ? t('admin.phieuNhapModal.titleEdit') : t('admin.phieuNhapModal.title') }}</span>
        <button class="btn-close btn-close-white btn-sm" @click="showPhieuNhapModal=false"></button>
      </div>
      <div class="overflow-y-auto p-4">
        <div v-if="phieuNhapFormError" class="alert alert-danger small py-2 mb-3">{{ phieuNhapFormError }}</div>
        <div class="row g-3 mb-3">
          <div class="col-6">
            <label class="form-label small text-secondary">{{ t('admin.phieuNhapModal.supplierLabel') }}</label>
            <SearchSelect v-model="phieuNhapForm.nhaCungCapId" :options="supplierOptions" :placeholder="t('admin.phieuNhapModal.selectPlaceholder')" />
          </div>
          <div class="col-6">
            <label class="form-label small text-secondary">{{ t('admin.phieuNhapModal.staffLabel') }}</label>
            <SearchSelect v-model="phieuNhapForm.nhanVienId" :options="staffOptions" :placeholder="t('admin.phieuNhapModal.selectPlaceholder')" />
          </div>
          <div class="col-6">
            <label class="form-label small text-secondary">{{ t('admin.phieuNhapModal.dateLabel') }}</label>
            <input v-model="phieuNhapForm.ngayNhap" type="datetime-local" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" />
          </div>
          <div class="col-6">
            <label class="form-label small text-secondary">{{ t('admin.phieuNhapModal.noteLabel') }}</label>
            <input v-model="phieuNhapForm.ghiChu" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" />
          </div>
        </div>

        <div class="fw-semibold small text-secondary mb-2">{{ t('admin.phieuNhapModal.itemsLabel') }}</div>
        <div class="d-flex gap-2 mb-1">
          <label class="form-label small text-secondary mb-0" style="flex:2 1 0;">{{ t('admin.phieuNhapModal.colProduct') }}</label>
          <label class="form-label small text-secondary mb-0" style="flex:2 1 0;">{{ t('admin.phieuNhapModal.colVariant') }}</label>
          <label class="form-label small text-secondary mb-0" style="flex:0 0 80px;">{{ t('admin.phieuNhapModal.colQty') }}</label>
          <label class="form-label small text-secondary mb-0" style="flex:0 0 110px;">{{ t('admin.phieuNhapModal.colPrice') }}</label>
          <span style="flex:0 0 34px;"></span>
        </div>
        <div class="d-flex flex-column gap-2 mb-2">
          <div v-for="(row, idx) in phieuNhapForm.items" :key="idx" class="d-flex gap-2 align-items-center">
            <div style="flex:2 1 0;min-width:0;">
              <SearchSelect v-model="row.sanPhamId" @update:model-value="row.bienTheId=''"
                            :options="productOptionsForPhieuNhap"
                            :placeholder="t('admin.phieuNhapModal.selectProductPlaceholder')" />
            </div>
            <div style="flex:2 1 0;min-width:0;">
              <SearchSelect v-model="row.bienTheId" :disabled="!row.sanPhamId"
                            :options="variantsForProduct(row.sanPhamId)"
                            :placeholder="t('admin.phieuNhapModal.selectVariantPlaceholder')" />
            </div>
            <input v-model="row.soLuong" type="number" min="1" class="form-control form-control-sm" style="flex:0 0 80px;background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" :placeholder="t('admin.phieuNhapModal.qtyPlaceholder')" />
            <input v-model="row.donGia" type="number" min="0" class="form-control form-control-sm" style="flex:0 0 110px;background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" :placeholder="t('admin.phieuNhapModal.unitPricePlaceholder')" />
            <button class="btn btn-sm btn-outline-danger" style="padding:2px 8px;flex:0 0 34px;" @click="removePhieuNhapItemRow(idx)">✕</button>
          </div>
        </div>
        <button class="btn btn-sm btn-outline-warning mb-3" @click="addPhieuNhapItemRow">{{ t('admin.phieuNhapModal.addRow') }}</button>

        <div class="d-flex justify-content-end fw-bold" style="font-size:1.05rem;">
          {{ t('admin.phieuNhapModal.totalLabel') }} {{ formatPrice(phieuNhapItemsTotal) }}
        </div>
      </div>
      <div class="d-flex justify-content-end gap-2 p-3 border-top border-secondary">
        <button class="btn btn-sm btn-outline-secondary" @click="showPhieuNhapModal=false">{{ t('admin.phieuNhapModal.cancel') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" @click="savePhieuNhap">{{ editingPhieuNhapId ? t('admin.phieuNhapModal.saveEdit') : t('admin.phieuNhapModal.save') }}</button>
      </div>
    </div>
  </div>

  <!-- ══ MODAL CHI TIET PHIEU NHAP ══ -->
  <div v-if="showPhieuNhapDetailModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="showPhieuNhapDetailModal=false">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:680px;max-width:96vw;max-height:90vh;">

      <!-- Header -->
      <div class="d-flex justify-content-between align-items-center px-4 py-3" style="border-bottom:1px solid var(--border-color-soft);" v-if="phieuNhapDetailData">
        <div class="d-flex align-items-center gap-3">
          <div class="rounded-3 d-flex align-items-center justify-content-center flex-shrink-0"
               style="width:40px;height:40px;background:rgba(167,139,250,0.15);font-size:1.2rem;">📋</div>
          <div>
            <div class="fw-bold" style="font-size:0.95rem;color:var(--text-heading);">
              {{ t('admin.phieuNhapDetailModal.title') }}
              <span class="text-secondary ms-1" style="font-size:0.8rem;font-family:monospace;">{{ phieuNhapDetailData.maPhieuNhap }}</span>
            </div>
            <div class="text-secondary" style="font-size:0.78rem;">{{ supplierName(phieuNhapDetailData.nhaCungCapId) }} · {{ formatDate(phieuNhapDetailData.ngayNhap) }}</div>
          </div>
        </div>
        <button class="btn-close btn-close-white btn-sm" @click="showPhieuNhapDetailModal=false"></button>
      </div>

      <div class="overflow-y-auto flex-grow-1" v-if="phieuNhapDetailData">
        <!-- Info chips -->
        <div class="d-flex flex-wrap gap-2 p-3" style="border-bottom:1px solid var(--border-color-soft);">
          <span class="d-flex align-items-center gap-1 rounded-pill px-3 py-1 small" style="background:var(--bg-card-alt);">
            🏢 <span class="text-secondary">{{ t('admin.phieuNhap.colSupplier') }}:</span> <span class="text-light fw-semibold">{{ supplierName(phieuNhapDetailData.nhaCungCapId) }}</span>
          </span>
          <span class="d-flex align-items-center gap-1 rounded-pill px-3 py-1 small" style="background:var(--bg-card-alt);">
            👤 <span class="text-secondary">{{ t('admin.phieuNhap.colStaff') }}:</span> <span class="text-light fw-semibold">{{ staffName(phieuNhapDetailData.nhanVienId) }}</span>
          </span>
          <span class="d-flex align-items-center gap-1 rounded-pill px-3 py-1 small" style="background:var(--bg-card-alt);">
            📅 <span class="text-secondary">{{ t('admin.phieuNhap.colDate') }}:</span> <span class="text-light fw-semibold">{{ formatDate(phieuNhapDetailData.ngayNhap) }}</span>
          </span>
          <span class="badge d-flex align-items-center" :style="{ background: phieuNhapStatusColor(phieuNhapDetailData.trangThai).bg, color: phieuNhapStatusColor(phieuNhapDetailData.trangThai).text }">
            {{ phieuNhapStatusIcon(phieuNhapDetailData.trangThai) }} {{ statusLabel(phieuNhapDetailData.trangThai) }}
          </span>
          <div v-if="phieuNhapDetailData.ghiChu" class="w-100 text-secondary small fst-italic" style="padding-left:2px;">📝 {{ phieuNhapDetailData.ghiChu }}</div>
        </div>

        <!-- Danh sach hang -->
        <div class="p-3">
          <table class="w-100 mb-0" style="border-collapse:collapse;font-size:0.82rem;">
            <thead>
              <tr style="background:var(--bg-input);">
                <th class="px-3 py-2 text-secondary" style="font-weight:600;">{{ t('admin.inventory.colSku') }}</th>
                <th class="px-3 py-2 text-secondary text-center" style="font-weight:600;width:80px;">{{ t('admin.phieuNhapModal.qtyPlaceholder') }}</th>
                <th class="px-3 py-2 text-secondary text-end" style="font-weight:600;width:130px;">{{ t('admin.phieuNhapModal.unitPricePlaceholder') }}</th>
                <th class="px-3 py-2 text-secondary text-end" style="font-weight:600;width:140px;">{{ t('admin.phieuNhapModal.totalLabel') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="c in phieuNhapDetailItems" :key="c.id" style="border-top:1px solid var(--border-color-soft);">
                <td class="px-3 py-2 text-secondary" style="font-family:monospace;">{{ c.maSku }}</td>
                <td class="px-3 py-2 text-center fw-bold" style="color:var(--text-heading);">{{ c.soLuong }}</td>
                <td class="px-3 py-2 text-end text-secondary">{{ formatPrice(c.donGiaNhap) }}</td>
                <td class="px-3 py-2 text-end fw-semibold" style="color:var(--accent-fg);">{{ formatPrice(c.thanhTien) }}</td>
              </tr>
              <tr v-if="phieuNhapDetailItems.length===0"><td colspan="4" class="text-center text-secondary py-4">{{ t('admin.phieuNhap.empty') }}</td></tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Footer: tong ket -->
      <div v-if="phieuNhapDetailData" class="px-4 py-3 d-flex justify-content-between align-items-center" style="border-top:1px solid var(--border-color-soft);background:var(--bg-card-alt);">
        <span class="text-secondary small">{{ phieuNhapDetailItems.length }} {{ t('admin.inventory.colSku') }}</span>
        <div class="d-flex align-items-center gap-2">
          <span class="text-secondary small">{{ t('admin.phieuNhapModal.totalLabel') }}</span>
          <span class="fw-bold" style="font-size:1.15rem;color:var(--accent-fg);">{{ formatPrice(phieuNhapDetailData.tongTien) }}</span>
        </div>
      </div>
      <div class="d-flex justify-content-end gap-2 p-3 pt-0" v-if="phieuNhapDetailData">
        <button class="btn btn-sm btn-outline-danger" @click="printPhieuNhapDetail(phieuNhapDetailData)">🖨️ {{ t('admin.phieuNhap.printPdf') }}</button>
        <button class="btn btn-sm btn-outline-secondary" @click="showPhieuNhapDetailModal=false">{{ t('admin.promoModal.cancel') }}</button>
      </div>
    </div>
  </div>

  <!-- ══ MODAL THEM SAN PHAM CHI TIET ══ -->
  <div v-if="showAddItemDetailModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
       style="background:var(--bg-overlay);z-index:1070;" @click.self="showAddItemDetailModal=false">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card-inset);border:1px solid var(--border-color-strong);width:960px;max-width:97vw;max-height:93vh;">

      <!-- Header -->
      <div class="d-flex justify-content-between align-items-center px-4 py-3" style="border-bottom:1px solid var(--bg-input);">
        <span class="text-secondary" style="font-size:0.8rem;">{{ t('admin.addItemDetailModal.chooseProduct') }}</span>
        <button class="btn-close btn-close-white btn-sm" @click="showAddItemDetailModal=false"></button>
      </div>

      <!-- Body -->
      <div class="overflow-y-auto flex-grow-1 p-0" v-if="addItemDetailGroup">
        <div class="d-flex" style="min-height:400px;">

          <!-- Left: Anh san pham -->
          <div class="d-flex flex-column align-items-center justify-content-center p-4"
               style="width:42%;background:var(--bg-page-alt);border-right:1px solid var(--bg-input);flex-shrink:0;">
            <div style="width:100%;max-width:320px;aspect-ratio:4/3;display:flex;align-items:center;justify-content:center;background:var(--bg-card-alt);border-radius:12px;overflow:hidden;padding:16px;">
              <img v-if="(addItemCurrentVariant || addItemDetailGroup.variants[0])?.hinhAnhChinh"
                   :src="(addItemCurrentVariant || addItemDetailGroup.variants[0]).hinhAnhChinh"
                   style="max-width:100%;max-height:100%;object-fit:contain;" />
              <span v-else style="font-size:4rem;">💻</span>
            </div>
            <div class="mt-3 d-flex gap-1 flex-wrap justify-content-center">
              <span v-for="tag in (addItemDetailGroup.variants[0]?.phanLoaiTen||'').split(',').filter(Boolean)"
                    :key="tag" class="badge" style="background:rgba(244,63,94,0.12);color:var(--accent-fg);font-size:0.7rem;">{{ tag.trim() }}</span>
            </div>
          </div>

          <!-- Right: Thong tin + chon bien the -->
          <div class="d-flex flex-column p-4 overflow-y-auto flex-grow-1">
            <!-- Ten + thuong hieu -->
            <div class="text-secondary mb-1" style="font-size:0.78rem;">
              {{ addItemDetailGroup.variants[0]?.tenThuongHieu }} · {{ addItemDetailGroup.variants[0]?.tenDanhMuc }}
            </div>
            <h5 class="fw-bold text-light mb-2">{{ addItemDetailGroup.tenSanPham }}</h5>
            <div class="mb-3" style="font-size:1.4rem;font-weight:700;color:var(--accent-fg);">
              {{ addItemCurrentVariant ? formatPrice(addItemCurrentVariant.giaBan) : formatPrice(addItemDetailGroup.minPrice) }}
              <span class="text-secondary ms-2" style="font-size:0.8rem;font-weight:400;">{{ t('admin.addItemDetailModal.freeshipNote') }}</span>
            </div>

            <!-- Chon phien ban (CPU + RAM + Storage) -->
            <div v-if="addItemConfigs.length > 1" class="mb-3">
              <div class="text-secondary mb-2" style="font-size:0.72rem;font-weight:700;letter-spacing:.05em;">
                {{ t('admin.addItemDetailModal.configCount', { count: addItemConfigs.length }) }}
              </div>
              <div class="d-flex flex-wrap gap-2">
                <button v-for="cfg in addItemConfigs" :key="cfg.key"
                        @click="selectConfig(cfg.key)"
                        class="btn btn-sm text-start"
                        style="padding:8px 12px;border-radius:8px;min-width:140px;"
                        :style="addItemSelectedConfig === cfg.key
                          ? 'background:rgba(244,63,94,0.12);border:2px solid var(--accent);color:var(--accent-fg);'
                          : 'background:var(--bg-card);border:1px solid var(--border-color-strong);color:var(--text-secondary);'">
                  <div style="font-size:0.78rem;font-weight:600;">{{ cfg.cpu || t('admin.addItemDetailModal.standard') }}</div>
                  <div style="font-size:0.68rem;">{{ [cfg.ram, cfg.oCung].filter(Boolean).join(' · ') }}</div>
                </button>
              </div>
            </div>

            <!-- Chon mau sac -->
            <div v-if="addItemColorsForConfig.length > 0" class="mb-3">
              <div class="text-secondary mb-2" style="font-size:0.72rem;font-weight:700;letter-spacing:.05em;">{{ t('admin.addItemDetailModal.color') }}</div>
              <div class="d-flex flex-wrap gap-2">
                <button v-for="v in addItemColorsForConfig" :key="v.bienTheId"
                        @click="addItemSelectedColor = v.mauSac"
                        class="btn btn-sm"
                        style="padding:6px 14px;border-radius:8px;"
                        :style="addItemSelectedColor === v.mauSac
                          ? 'background:rgba(244,63,94,0.12);border:2px solid var(--accent);color:var(--accent-fg);'
                          : 'background:var(--bg-card);border:1px solid var(--border-color-strong);color:var(--text-primary);'">
                  <div style="font-size:0.78rem;font-weight:600;">{{ v.mauSac }}</div>
                  <div style="font-size:0.7rem;color:var(--accent-fg);">{{ formatPrice(v.giaBan) }}</div>
                </button>
              </div>
            </div>

            <!-- Thong tin chon -->
            <div v-if="addItemCurrentVariant" class="mb-3 py-2 px-3 rounded-3" style="background:var(--bg-card);font-size:0.8rem;">
              <span class="text-secondary">{{ t('admin.addItemDetailModal.colorLabel') }} </span>
              <strong class="text-light">{{ addItemCurrentVariant.mauSac }}</strong>
              <span class="mx-2 text-secondary">·</span>
              <span class="text-secondary">{{ t('admin.addItemDetailModal.warrantyLabel') }} </span>
              <strong class="text-light">{{ addItemCurrentVariant.baoHanhThang ? addItemCurrentVariant.baoHanhThang + ' ' + t('admin.addItemDetailModal.months') : '—' }}</strong>
              <span class="mx-2 text-secondary">·</span>
              <span class="text-secondary">{{ t('admin.addItemDetailModal.skuLabel') }} </span>
              <span class="text-light" style="font-family:monospace;font-size:0.75rem;">{{ addItemCurrentVariant.maSku }}</span>
            </div>

            <!-- Thong so ky thuat -->
            <div v-if="addItemCurrentVariant" class="mb-3">
              <div class="text-secondary mb-2" style="font-size:0.72rem;font-weight:700;letter-spacing:.05em;">{{ t('admin.addItemDetailModal.specsHeading') }}</div>
              <table style="width:100%;font-size:0.78rem;border-collapse:collapse;">
                <tr v-for="([label, val]) in [
                  [t('admin.addItemDetailModal.specCpu'), addItemCurrentVariant.cpu],
                  [t('admin.addItemDetailModal.specRam'), addItemCurrentVariant.ram],
                  [t('admin.addItemDetailModal.specStorage'), addItemCurrentVariant.oCung],
                  [t('admin.addItemDetailModal.specGpu'), addItemCurrentVariant.gpu],
                  [t('admin.addItemDetailModal.specScreen'), addItemCurrentVariant.kichThuocManHinh],
                  [t('admin.addItemDetailModal.specOs'), addItemCurrentVariant.heDieuHanh],
                  [t('admin.addItemDetailModal.specBattery'), addItemCurrentVariant.pin],
                  [t('admin.addItemDetailModal.specWeight'), addItemCurrentVariant.trongLuongKg ? addItemCurrentVariant.trongLuongKg + ' kg' : null],
                ].filter(([,v]) => v)" :key="label" style="border-top:1px solid var(--bg-input);">
                  <td class="py-1 text-secondary" style="padding-left:0;width:44%;">{{ label }}</td>
                  <td class="py-1 text-light fw-semibold">{{ val }}</td>
                </tr>
              </table>
            </div>
          </div>
        </div>
      </div>

      <!-- Footer: so luong + them -->
      <div class="px-4 py-3 d-flex align-items-center gap-3" style="border-top:1px solid var(--bg-input);background:var(--bg-page-alt);">
        <span class="text-secondary" style="font-size:0.85rem;">{{ t('admin.addItemDetailModal.qtyLabel') }}</span>
        <input v-model.number="addItemQty" type="number" min="1" max="99"
               class="form-control form-control-sm"
               style="width:80px;background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
        <button class="btn btn-warning flex-grow-1 fw-bold" style="font-size:0.9rem;"
                :disabled="!addItemCurrentVariant || addItemQty < 1 || addItemLoading"
                @click="confirmAddFromDetail">
          {{ addItemLoading ? t('admin.addItemDetailModal.adding') : t('admin.addItemDetailModal.addToOrder') }}
        </button>
      </div>
    </div>
  </div>

  <!-- ══ MODAL CHI TIET DON HANG ══ -->
  <div v-if="showOrderDetailModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1050;" @click.self="showOrderDetailModal=false">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:720px;max-width:96vw;max-height:90vh;">

      <!-- Header -->
      <div class="d-flex justify-content-between align-items-center px-4 py-3" style="border-bottom:1px solid var(--border-color-soft);">
        <div>
          <div class="fw-bold" style="font-size:0.95rem;color:var(--text-heading);">
            {{ t('admin.orderDetailModal.titlePrefix') }}{{ orderDetailData?.donHangId }}
            <span v-if="orderDetailData?.maDonHang" class="text-secondary ms-2" style="font-size:0.8rem;font-family:monospace;">{{ orderDetailData.maDonHang }}</span>
          </div>
          <div class="text-secondary" style="font-size:0.78rem;">
            {{ customerName(orderDetailData?.khachHangId) }} · {{ formatDate(orderDetailData?.ngayDat) }}
          </div>
        </div>
        <button class="btn-close btn-close-white btn-sm" @click="showOrderDetailModal=false"></button>
      </div>

      <!-- Toan bo body scroll cung nhau -->
      <div class="overflow-y-auto flex-grow-1">
      <!-- Danh sach san pham trong don -->
      <div class="p-3">
        <div v-if="orderDetailLoading" class="text-secondary small text-center py-4">{{ t('admin.orderDetailModal.loading') }}</div>
        <div v-else-if="orderDetailItems.length === 0" class="text-secondary small text-center py-4">{{ t('admin.orderDetailModal.empty') }}</div>
        <table v-else class="w-100 mb-0" style="border-collapse:collapse;font-size:0.82rem;">
          <thead>
            <tr style="background:var(--bg-input);">
              <th class="px-3 py-2 text-secondary" style="font-weight:600;width:38%;">{{ t('admin.orderDetailModal.colProduct') }}</th>
              <th class="px-3 py-2 text-secondary" style="font-weight:600;width:16%;font-family:monospace;">{{ t('admin.orderDetailModal.colSku') }}</th>
              <th class="px-3 py-2 text-secondary text-center" style="font-weight:600;width:8%;">{{ t('admin.orderDetailModal.colQty') }}</th>
              <th class="px-3 py-2 text-secondary text-end" style="font-weight:600;width:14%;">{{ t('admin.orderDetailModal.colUnitPrice') }}</th>
              <th class="px-3 py-2 text-secondary text-end" style="font-weight:600;width:14%;">{{ t('admin.orderDetailModal.colTotal') }}</th>
              <th class="px-3 py-2 text-secondary" style="font-weight:600;width:10%;"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in orderDetailItems" :key="item.id" style="border-top:1px solid var(--border-color-soft);">
              <td class="px-3 py-2">
                <div class="d-flex align-items-center gap-2">
                  <img v-if="productByBienThe(item.bienTheId)?.hinhAnhChinh"
                       :src="productByBienThe(item.bienTheId).hinhAnhChinh"
                       style="width:36px;height:28px;object-fit:contain;border-radius:4px;background:var(--bg-card-inset);flex-shrink:0;" />
                  <span v-else style="font-size:1.2rem;flex-shrink:0;">💻</span>
                  <span class="text-light">{{ productByBienThe(item.bienTheId)?.tenSanPham || '—' }}</span>
                </div>
              </td>
              <td class="px-3 py-2 text-secondary" style="font-family:monospace;font-size:0.75rem;">
                {{ item.maSku }}
                <div v-if="item.soSerial" class="text-info" style="font-size:0.7rem;">S/N: {{ item.soSerial }}</div>
              </td>
              <td class="px-3 py-2 text-center fw-bold" style="color:var(--text-heading);">{{ item.soLuong }}</td>
              <td class="px-3 py-2 text-end text-secondary">{{ formatPrice(item.donGia) }}</td>
              <td class="px-3 py-2 text-end text-warning fw-semibold">{{ formatPrice(item.thanhTien) }}</td>
              <td class="px-3 py-2">
                <div class="d-flex gap-1 justify-content-center">
                  <button v-if="productByBienThe(item.bienTheId)"
                          class="btn btn-sm btn-outline-secondary"
                          style="font-size:0.72rem;padding:2px 6px;"
                          @click="openVariantDetail(item.bienTheId)">
                    {{ t('admin.orderDetailModal.detail') }}
                  </button>
                  <button class="btn btn-sm btn-outline-danger"
                          style="font-size:0.72rem;padding:2px 6px;"
                          @click="removeItemFromOrder(item.id)">
                    {{ t('admin.orderDetailModal.delete') }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Footer: tong ket -->
      <div v-if="orderDetailData" class="px-4 py-3 d-flex flex-column gap-1" style="border-top:1px solid var(--border-color-soft);background:var(--bg-card-alt);">
        <div class="d-flex justify-content-between small text-secondary">
          <span>{{ t('admin.orderDetailModal.subtotal') }}</span><span>{{ formatPrice(orderDetailData.tongTien) }}</span>
        </div>
        <div v-if="orderDetailData.giamGia > 0" class="d-flex justify-content-between small text-success">
          <span>{{ t('admin.orderDetailModal.discount') }}</span><span>− {{ formatPrice(orderDetailData.giamGia) }}</span>
        </div>
        <div class="d-flex justify-content-between small text-secondary">
          <span>{{ t('admin.orderDetailModal.shippingFee') }}</span>
          <span :class="orderDetailData.phiVanChuyen === 0 ? 'text-success' : ''">
            {{ orderDetailData.phiVanChuyen === 0 ? t('admin.orderDetailModal.free') : formatPrice(orderDetailData.phiVanChuyen) }}
          </span>
        </div>
        <div class="d-flex justify-content-between fw-bold pt-2 mt-1" style="border-top:1px solid var(--border-color);">
          <span style="color:var(--text-heading);">{{ t('admin.orderDetailModal.total') }}</span>
          <span class="text-warning" style="font-size:1rem;">{{ formatPrice(orderDetailData.thanhTien) }}</span>
        </div>
        <div class="d-flex justify-content-between small mt-2 pt-2" style="border-top:1px solid var(--bg-input);">
          <span class="text-secondary">{{ t('admin.orderDetailModal.orderStatus') }}</span>
          <span class="badge" :style="{ background: orderStatusColor(orderDetailData.trangThaiDonHang).bg, color: orderStatusColor(orderDetailData.trangThaiDonHang).text }">
            {{ orderStatusIcon(orderDetailData.trangThaiDonHang) }} {{ orderStatusLabel(orderDetailData.trangThaiDonHang) }}
          </span>
        </div>
        <div class="d-flex justify-content-between small">
          <span class="text-secondary">{{ t('admin.orderDetailModal.paymentStatus') }}</span>
          <span class="badge" :style="{ background: paymentStatusColor(orderDetailData.trangThaiThanhToan).bg, color: paymentStatusColor(orderDetailData.trangThaiThanhToan).text }">
            {{ paymentStatusIcon(orderDetailData.trangThaiThanhToan) }} {{ orderDetailData.trangThaiThanhToan ? paymentStatusLabel(orderDetailData.trangThaiThanhToan) : '—' }}
          </span>
        </div>
        <div v-if="orderDetailData.ngayGiaoDuKien" class="d-flex justify-content-between small">
          <span class="text-secondary">{{ t('admin.orderStatusModal.expectedDeliveryLabel') }}</span>
          <span style="color:var(--text-primary);">{{ formatDateTime(orderDetailData.ngayGiaoDuKien) }}</span>
        </div>
        <div v-if="orderDetailData.ngayGiaoThucTe" class="d-flex justify-content-between small">
          <span class="text-secondary">{{ t('admin.orderStatusModal.actualDeliveryLabel') }}</span>
          <span style="color:var(--text-primary);">{{ formatDateTime(orderDetailData.ngayGiaoThucTe) }}</span>
        </div>

        <!-- Them san pham moi vao don -->
        <div class="mt-3 pt-2" style="border-top:1px solid var(--bg-input);">
          <div class="d-flex align-items-center justify-content-between mb-2" style="cursor:pointer;"
               @click="addItemMode = !addItemMode; addItemBienTheId = ''; addItemSelectedSpId = null; addItemSearch = ''">
            <span class="fw-semibold" style="font-size:0.85rem;color:var(--text-primary);">{{ t('admin.orderDetailModal.addNewItem') }}</span>
            <span style="color:var(--text-muted);font-size:0.75rem;">{{ addItemMode ? '▲' : '▼' }}</span>
          </div>

          <div v-if="addItemMode">
            <!-- Tim kiem -->
            <input v-model="addItemSearch" type="text" :placeholder="t('admin.orderDetailModal.searchPlaceholder')"
                   class="form-control form-control-sm mb-3"
                   style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);font-size:0.8rem;" />

            <!-- Grid san pham -->
            <div class="d-grid gap-2 mb-2" style="grid-template-columns:repeat(3,1fr);max-height:280px;overflow-y:auto;">
              <div v-for="g in addItemProductGroups" :key="g.sanPhamId"
                   @click="openAddItemDetail(g)"
                   class="rounded-3 overflow-hidden"
                   style="cursor:pointer;border:1px solid var(--border-color);background:var(--bg-card);transition:border-color .15s;"
                   @mouseenter="$event.currentTarget.style.borderColor='var(--accent)'"
                   @mouseleave="$event.currentTarget.style.borderColor='var(--border-color)'">
                <div style="background:var(--bg-card-inset);height:80px;display:flex;align-items:center;justify-content:center;overflow:hidden;">
                  <img v-if="g.hinhAnhChinh" :src="g.hinhAnhChinh"
                       style="max-height:76px;max-width:100%;object-fit:contain;" />
                  <span v-else style="font-size:1.8rem;">💻</span>
                </div>
                <div class="px-2 py-1">
                  <div class="fw-semibold text-light" style="font-size:0.72rem;line-height:1.3;
                       display:-webkit-box;-webkit-line-clamp:2;line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;">
                    {{ g.tenSanPham }}
                  </div>
                  <div class="text-secondary" style="font-size:0.65rem;">{{ g.tenThuongHieu }}</div>
                  <div style="font-size:0.72rem;color:var(--accent-fg);font-weight:600;margin-top:2px;">
                    {{ t('admin.orderDetailModal.priceFrom') }} {{ formatPrice(g.minPrice) }}
                  </div>
                  <div v-if="g.phanLoaiTen" class="mt-1">
                    <span v-for="tag in (g.phanLoaiTen||'').split(',').filter(Boolean)" :key="tag"
                          class="badge bg-secondary me-1" style="font-size:0.58rem;">{{ tag.trim() }}</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="text-secondary text-center py-1" style="font-size:0.75rem;">{{ t('admin.orderDetailModal.selectVariantHint') }}</div>
          </div>
        </div>

        <!-- Canh bao gop don: chi 1 nut, tu dong gop tat ca don cung ngay -->
        <div v-if="mergeCandidates.length > 0" class="mt-2 pt-2 d-flex align-items-center justify-content-between gap-2"
             style="border-top:1px solid var(--bg-input);background:#1a1500;border-radius:6px;padding:8px 12px;">
          <span style="font-size:0.78rem;color:#fbbf24;">
            {{ t('admin.orderDetailModal.mergeBannerText', { count: mergeCandidates.length }) }}
            <span class="text-secondary ms-1">(#{{ mergeCandidates.map(o => o.donHangId).join(', #') }})</span>
          </span>
          <button class="btn btn-sm btn-warning flex-shrink-0" style="font-size:0.78rem;padding:3px 10px;"
                  :disabled="mergeLoading"
                  @click="autoMergeOrders">
            {{ mergeLoading ? t('admin.orderDetailModal.merging') : t('admin.orderDetailModal.mergeAll') }}
          </button>
        </div>
      </div>
      </div><!-- end outer scroll wrapper -->
    </div>
  </div>

  <!-- ══ MODAL TRANG THAI DON HANG ══ -->
  <div v-if="showOrderModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="showOrderModal=false">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:460px;max-width:95vw;max-height:90vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>{{ t('admin.orderStatusModal.title') }}</span>
        <button class="btn-close btn-close-white btn-sm" @click="showOrderModal=false"></button>
      </div>
      <div class="overflow-y-auto p-4">
        <div v-if="orderStatusError" class="alert alert-danger small py-2 mb-3">{{ orderStatusError }}</div>
        <div v-if="editingOrder" class="small p-2 rounded-2 mb-3 text-secondary" style="background:var(--bg-hover);">
          {{ t('admin.orderStatusModal.orderPrefix') }}{{ editingOrder.donHangId }} — {{ t('admin.orderStatusModal.customerLabel') }} <strong>{{ customerName(editingOrder.khachHangId) }}</strong>
        </div>
        <div class="d-flex flex-column gap-3">
          <div><label class="form-label small text-secondary">{{ t('admin.orderStatusModal.statusLabel') }}</label><select v-model="orderStatusForm.trangThaiDonHang" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)"><option value="pending">{{ t('admin.orderStatusModal.status.pending') }}</option><option value="confirmed">{{ t('admin.orderStatusModal.status.confirmed') }}</option><option value="processing">{{ t('admin.orderStatusModal.status.processing') }}</option><option value="shipping">{{ t('admin.orderStatusModal.status.shipping') }}</option><option value="delivered">{{ t('admin.orderStatusModal.status.delivered') }}</option><option value="cancelled">{{ t('admin.orderStatusModal.status.cancelled') }}</option><option value="returned">{{ t('admin.orderStatusModal.status.returned') }}</option></select></div>
          <div><label class="form-label small text-secondary">{{ t('admin.orderStatusModal.paymentLabel') }}</label><select v-model="orderStatusForm.trangThaiThanhToan" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)"><option value="unpaid">{{ t('admin.paymentStatus.unpaid') }}</option><option value="partial">{{ t('admin.paymentStatus.partial') }}</option><option value="paid">{{ t('admin.paymentStatus.paid') }}</option><option value="refunded">{{ t('admin.paymentStatus.refunded') }}</option></select></div>
          <div class="row g-2">
            <div class="col-6">
              <label class="form-label small text-secondary">{{ t('admin.orderStatusModal.expectedDeliveryLabel') }}</label>
              <input type="datetime-local" v-model="orderStatusForm.ngayGiaoDuKien" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" />
            </div>
            <div class="col-6">
              <label class="form-label small text-secondary">{{ t('admin.orderStatusModal.actualDeliveryLabel') }}</label>
              <input type="datetime-local" v-model="orderStatusForm.ngayGiaoThucTe" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" />
            </div>
          </div>
        </div>
      </div>
      <div class="d-flex justify-content-end gap-2 p-3 border-top border-secondary">
        <button class="btn btn-sm btn-outline-secondary" @click="showOrderModal=false">{{ t('admin.orderStatusModal.cancel') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" @click="saveOrderStatus">{{ t('admin.orderStatusModal.save') }}</button>
      </div>
    </div>
  </div>

  <!-- ══ MODAL TON KHO ══ -->
  <div v-if="showStockModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="showStockModal=false">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:480px;max-width:95vw;max-height:90vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>{{ t('admin.stockModal.title') }}</span>
        <button class="btn-close btn-close-white btn-sm" @click="showStockModal=false"></button>
      </div>
      <div class="overflow-y-auto p-4">
        <div v-if="editingStock" class="small p-2 rounded-2 mb-3 text-secondary" style="background:var(--bg-hover);">
          {{ editingStock.bienThe?.sanPham?.tenSanPham??'—' }} — {{ t('admin.stockModal.skuLabel') }} <strong>{{ editingStock.bienThe?.maSku??'—' }}</strong>
        </div>
        <div class="row g-3">
          <div class="col-6">
            <label class="form-label small text-secondary">{{ t('admin.stockModal.stockLabel') }}</label>
            <div class="form-control form-control-sm d-flex align-items-center" style="background:var(--bg-hover); color:var(--text-secondary); border-color:var(--border-color-strong)">{{ editingStock?.soLuongTon ?? 0 }}</div>
            <div class="text-secondary" style="font-size:0.72rem;">{{ t('admin.stockModal.stockHint') }}</div>
          </div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.stockModal.heldLabel') }}</label><input v-model="stockForm.soLuongGiu" type="number" min="0" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-12"><label class="form-label small text-secondary">{{ t('admin.stockModal.minStockLabel') }}</label><input v-model="stockForm.tonKhoToiThieu" type="number" min="0" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-12">
            <div class="d-flex justify-content-between align-items-center mb-1">
              <label class="form-label small text-secondary mb-0">{{ t('admin.stockModal.newSerialsLabel') }}</label>
              <label class="btn btn-sm btn-outline-info" style="padding:2px 10px;font-size:0.72rem;cursor:pointer;">
                📂 {{ t('admin.stockModal.importFromFile') }}
                <input type="file" accept=".csv,.txt,.xlsx,.xls" class="d-none" @change="importSerialsFromFile" />
              </label>
            </div>
            <div class="d-flex flex-column gap-2">
              <div v-for="(s, idx) in stockForm.newSerials" :key="idx" class="d-flex gap-2 align-items-center">
                <input v-model="stockForm.newSerials[idx]" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" :placeholder="t('admin.stockModal.serialPlaceholder')" />
                <button class="btn btn-sm btn-outline-danger" style="padding:2px 8px;" @click="removeStockSerialRow(idx)">✕</button>
              </div>
            </div>
            <button class="btn btn-sm btn-outline-warning mt-2" @click="addStockSerialRow">{{ t('admin.stockModal.addSerialRow') }}</button>
            <div class="text-secondary mt-1" style="font-size:0.72rem;">{{ t('admin.stockModal.importHint') }}</div>
          </div>
        </div>
      </div>
      <div class="d-flex justify-content-end gap-2 p-3 border-top border-secondary">
        <button class="btn btn-sm btn-outline-secondary" @click="showStockModal=false">{{ t('admin.stockModal.cancel') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" @click="saveStock">{{ t('admin.stockModal.save') }}</button>
      </div>
    </div>
  </div>

  <!-- ══ MODAL CHI TIET SERIAL ══ -->
  <div v-if="showStockDetailModal"
       class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
       style="background:var(--bg-overlay);z-index:1060;"
       @click.self="showStockDetailModal=false">
    <div class="rounded-4 d-flex flex-column"
         style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:760px;max-width:96vw;max-height:88vh;">

      <!-- Header -->
      <div class="d-flex align-items-start justify-content-between px-4 py-3" style="border-bottom:1px solid var(--border-color-soft);">
        <div>
          <div class="fw-bold" style="font-size:0.95rem;color:var(--text-heading);">
            {{ t('admin.stockDetailModal.titlePrefix') }} {{ stockDetailItem?.bienThe?.maSku || '—' }}
          </div>
          <div class="d-flex gap-1 mt-1 flex-wrap">
            <span v-if="getVariantInfo(stockDetailItem)?.cpu" class="badge" style="background:#2a2a3a;color:#aab;font-size:0.7rem;">{{ getVariantInfo(stockDetailItem).cpu }}</span>
            <span v-if="getVariantInfo(stockDetailItem)?.ram" class="badge" style="background:#2a3a2a;color:#aba;font-size:0.7rem;">{{ getVariantInfo(stockDetailItem).ram }}</span>
            <span v-if="getVariantInfo(stockDetailItem)?.oCung" class="badge" style="background:#3a2a2a;color:#baa;font-size:0.7rem;">{{ getVariantInfo(stockDetailItem).oCung }}</span>
            <span v-if="getVariantInfo(stockDetailItem)?.mauSac" class="badge" style="background:#2a2a2a;color:#999;font-size:0.7rem;">{{ getVariantInfo(stockDetailItem).mauSac }}</span>
          </div>
        </div>
        <div class="d-flex align-items-center gap-3">
          <div class="text-center">
            <div class="d-flex align-items-center justify-content-center gap-1">
              <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:#22c55e;flex-shrink:0;"></span>
              <span class="fw-bold" style="font-size:1.2rem;color:var(--text-heading);">{{ stockDetailSerials.filter(s=>s.trangThai==='trong_kho').length }}</span>
            </div>
            <div class="text-secondary" style="font-size:0.7rem;">{{ t('admin.stockDetailModal.inStock') }}</div>
          </div>
          <div class="text-center">
            <div class="d-flex align-items-center justify-content-center gap-1">
              <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:#94a3b8;flex-shrink:0;"></span>
              <span class="fw-bold" style="font-size:1.2rem;color:var(--text-heading);">{{ stockDetailSerials.filter(s=>s.trangThai==='da_ban').length }}</span>
            </div>
            <div class="text-secondary" style="font-size:0.7rem;">{{ t('admin.stockDetailModal.sold') }}</div>
          </div>
          <div class="text-center">
            <div class="d-flex align-items-center justify-content-center gap-1">
              <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:#fb923c;flex-shrink:0;"></span>
              <span class="fw-bold" style="font-size:1.2rem;color:var(--text-heading);">{{ stockDetailSerials.filter(s=>s.trangThai==='loi_bao_hanh').length }}</span>
            </div>
            <div class="text-secondary" style="font-size:0.7rem;">{{ t('admin.stockDetailModal.warranty') }}</div>
          </div>
          <button class="btn-close btn-close-white btn-sm ms-2" @click="showStockDetailModal=false"></button>
        </div>
      </div>

      <!-- Serial list -->
      <div class="overflow-y-auto flex-grow-1">
        <div v-if="stockDetailLoading" class="text-secondary small text-center py-5">{{ t('admin.stockDetailModal.loading') }}</div>
        <div v-else-if="stockDetailSerials.length === 0" class="text-secondary small text-center py-5">{{ t('admin.stockDetailModal.empty') }}</div>
        <table v-else class="w-100" style="border-collapse:collapse;font-size:0.82rem;">
          <thead>
            <tr style="background:var(--bg-input);position:sticky;top:0;">
              <th class="px-4 py-2 text-secondary" style="font-weight:500;width:40px;">{{ t('admin.stockDetailModal.colIndex') }}</th>
              <th class="px-4 py-2 text-secondary" style="font-weight:500;">{{ t('admin.stockDetailModal.colSerial') }}</th>
              <th class="px-4 py-2 text-secondary" style="font-weight:500;">{{ t('admin.stockDetailModal.colImportDate') }}</th>
              <th class="px-4 py-2 text-secondary" style="font-weight:500;">{{ t('admin.stockDetailModal.colStatus') }}</th>
              <th class="px-4 py-2 text-secondary" style="font-weight:500;width:60px;"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(s, idx) in stockDetailSerials" :key="s.chiTietId"
                style="border-top:1px solid var(--bg-input);">
              <td class="px-4 py-2 text-secondary">{{ idx + 1 }}</td>
              <td class="px-4 py-2 fw-semibold" style="font-family:monospace;color:var(--text-heading);">{{ s.soSerial }}</td>
              <td class="px-4 py-2 text-secondary">{{ formatDate(s.ngayNhapKho) }}</td>
              <td class="px-4 py-2">
                <span
                  style="display:inline-block;width:10px;height:10px;border-radius:50%;"
                  :style="{ background: stockDetailStatusColor(s.trangThai) }"
                  :title="stockDetailStatusLabel(s.trangThai)"
                ></span>
              </td>
              <td class="px-4 py-2">
                <button v-if="s.trangThai==='trong_kho'" class="btn btn-sm btn-outline-danger" style="padding:1px 7px;font-size:0.72rem;" :title="t('admin.stockDetailModal.deleteSerial')" @click="removeStockSerial(s.chiTietId)">✕</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

    </div>
  </div>

  <!-- Dialog xác nhận + toast dùng chung toàn trang — PHẢI nằm ngoài mọi v-if của modal cụ
       thể, nếu không component sẽ không tồn tại trong DOM khi modal đó đang đóng, khiến
       askConfirm()/showToast() gọi ra nhưng không có gì hiển thị (Promise của askConfirm
       treo mãi, code gọi nó bị kẹt không chạy tiếp). -->
  <ConfirmDialog />

    <!-- Toast thông báo lỗi/thành công (thay window.alert()) -->
    <Transition name="adm-toast-slide">
      <div v-if="toast.show"
           class="position-fixed d-flex align-items-start gap-2 px-4 py-3 rounded-3 fw-semibold small shadow-lg"
           style="top:24px; right:24px; z-index:9999; min-width:260px; max-width:440px; pointer-events:none; line-height:1.4;"
           :style="toast.type === 'success'
             ? 'background:var(--state-success,#16a34a); color:#fff;'
             : 'background:var(--state-danger,#dc2626); color:#fff;'"
           role="status" aria-live="polite">
        <span style="font-size:1.1rem; flex-shrink:0;">{{ toast.type === 'success' ? '✓' : '✕' }}</span>
        <span>{{ toast.msg }}</span>
      </div>
    </Transition>
</template>

<style scoped>
.adm-toast-slide-enter-active, .adm-toast-slide-leave-active { transition: transform 0.3s ease, opacity 0.25s ease; }
.adm-toast-slide-enter-from, .adm-toast-slide-leave-to       { transform: translateX(110%); opacity: 0; }

/* CSS toi thieu cho nhung gi Bootstrap khong the thay the */

/* Bootstrap .text-light hardcode mau trang co dinh — ghi de theo theme hien tai
   (toan bo cac cho dung .text-light trong file nay deu nam tren nen the/card,
   khong phai nen mau thuong hieu co dinh, nen an toan khi ghi de theo bien theme) */
.text-light {
  color: var(--text-primary) !important;
}

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

/* Row do trong bang ton kho khi san pham sap het */
.row-warn td { background: rgba(224,82,82,0.06) !important; }

/* Layout POS: 2 cot, chiem toan bo chieu cao */
.pos-grid-layout {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 18px;
  height: calc(100vh - 120px);
}
</style>
