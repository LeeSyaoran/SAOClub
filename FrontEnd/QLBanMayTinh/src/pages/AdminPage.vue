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
import { ChiTietCpuService, ChiTietRamService, ChiTietGpuService, ChiTietOCungService } from "../services/ChiTietLinhKienService.js";
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
import ProductsTable from "../components/admin/ProductsTable.vue";
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
import BienTheTable from "../components/admin/BienTheTable.vue";
import SerialManager from "../components/admin/SerialManager.vue";
import DmCategoryTable from "../components/admin/DmCategoryTable.vue";
import UserProfileMenu from "../components/admin/UserProfileMenu.vue";
import AdminDashboard from "../components/admin/AdminDashboard.vue";
import AdminReports from "../components/admin/AdminReports.vue";
import AdminSettings from "../components/admin/AdminSettings.vue";
import { ProductsStore, ensureProducts, refreshProducts } from "../stores/products.js";
import { OrdersStore, ensureOrders, refreshOrders, connectOrderEvents, disconnectOrderEvents } from "../stores/orders.js";
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
  Shield, Hash, Truck, ScrollText, Cpu, MemoryStick, Gamepad2, HardDrive, Layers,
} from '@lucide/vue';

defineEmits(['addToCart', 'buyAgainUnavailable', 'goHome']);

// ── Navigation ───────────────────────────────────────────────────────────────
const route = useRoute();
const currentPage = ref(route.params.id ? "san-pham-detail" : "dashboard");
// Sidebar bật/tắt được ở MỌI kích thước màn hình bằng nút hamburger ở topbar — mặc định
// mở trên desktop (>=768px), đóng trên mobile. Trên mobile sidebar hiện dạng overlay đè lên
// nội dung; trên desktop nó ẩn/hiện ngay trong layout (xem CSS .adm-sidebar cuối file).
// ponytail: không đồng bộ lại khi resize cửa sổ giữa chừng qua mốc 768px, F5 lại nếu cần.
const sidebarOpen = ref(window.matchMedia("(min-width: 768px)").matches);
const selectedCustomerId = ref(null);
const selectedSanPhamId = ref(route.params.id ? Number(route.params.id) : null);
const openCustomerDetail = (id) => {
  selectedCustomerId.value = id;
  currentPage.value = "customer-detail";
};
const navigate = (page) => {
  currentPage.value = page;
  if (window.matchMedia("(max-width: 767.98px)").matches) sidebarOpen.value = false; // chọn xong tự đóng lại trên mobile
  if (page === "staff") { ensureChucVuList(); ensureStaff(); }
  // ReturnsPanel.vue chỉ tải dữ liệu 1 lần lúc mount (v-show giữ nguyên component, không
  // tự huỷ/tạo lại theo tab) — khách gửi yêu cầu trả hàng mới sau khi admin đã mở trang sẽ
  // không tự hiện nếu không làm mới lại mỗi lần vào tab này.
  if (page === "tra-hang") refreshReturns();
};
// icon khớp đúng ý nghĩa icon SVG tương ứng ở sidebar (adm-icon) — hiện lại 1 lần nữa
// cạnh tiêu đề trang cho dễ nhận biết đang ở đâu, không cần đổi cả 2 nơi khi thêm trang mới.
const PAGE_META = {
  dashboard: { titleKey: "admin.pageMeta.dashboard.title", subKey: "admin.pageMeta.dashboard.sub", icon: BarChart3 },
  products: { titleKey: "admin.pageMeta.products.title", subKey: "admin.pageMeta.products.sub", icon: Laptop },
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
};
const topbarTitle = computed(
  () => t(PAGE_META[currentPage.value]?.titleKey ?? "admin.pageMeta.dashboard.title"),
);
const topbarSub = computed(() => t(PAGE_META[currentPage.value]?.subKey ?? ""));
const topbarIcon = computed(() => PAGE_META[currentPage.value]?.icon ?? BarChart3);

// ── Data refs ─────────────────────────────────────────────────────────────────
// products/orders/customers/staff/promotions/inventory/suppliers giờ sống trong stores/*.js
// dùng chung nhiều trang — computed alias bên dưới giữ nguyên tên biến cũ để phần còn lại
// của file (200+ chỗ đọc products.value/orders.value/...) không cần sửa. computed = read-only,
// mọi chỗ CRUD phải gọi refreshXxx()/ensureXxx() của store thay vì gán tay vào các biến này.
const products = computed(() => ProductsStore?.items ?? []);
const orders = computed(() => OrdersStore?.items ?? []);
const customers = computed(() => CustomersStore?.items ?? []);
const staff = computed(() => StaffStore?.items ?? []);
const promotions = computed(() => PromotionsStore?.items ?? []);
// Cấu hình vòng quay may mắn — không dùng store riêng vì chỉ 1 dòng dữ liệu phẳng, chỉ
// dùng ở đúng section này (khác các store khác dùng chung nhiều nơi).
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
    // capNhatCauHinh() dùng put() (Service/api.js) — trả về Response THÔ, không tự parse
    // JSON và không tự throw khi !ok (khác get()). Phải tự kiểm tra res.ok, nếu không lỗi
    // lưu (vd validate 400 do nhập điểm/lượt <=0) sẽ bị nuốt im lặng, admin tưởng đã lưu.
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
// ── Dữ liệu mới cho bố cục Dashboard kiểu "Joint Payroll" ─────────────────────
// Tỉ lệ sản phẩm đang bán/tổng sản phẩm — dùng cho ring "Sản phẩm đang bán" và 1 trục
// radar KPI. groupedProducts (không phải products) vì products là 1 dòng/biến thể.
const activeProductRatio = computed(() => {
  const total = groupedProducts.value.length;
  if (!total) return 0;
  const active = new Set(
    products.value.filter(p => p.trangThai === 'active').map(p => p.sanPhamId)
  ).size;
  return (active / total) * 100;
});

// Doanh thu theo từng ngày trong tuần hiện tại — dùng lại đúng ordersInWeekRange đã có
// (tính theo weekChartAnchor, mặc định tuần hiện tại) cho biểu đồ cột "Weekly Payroll
// Budget". getDay() trả 0=Chủ nhật — quy về mảng bắt đầu Thứ 2 (index 0) cho khớp UI.
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

// Số đơn hàng theo từng ngày trong THÁNG HIỆN TẠI — cho "Heat Map" lịch. Chỉ tính 1
// lần lúc load trang (không tự cập nhật qua nửa đêm — chấp nhận được cho 1 dashboard
// admin, F5 lại nếu cần xem đúng tháng mới).
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

// Gộp 4 tỉ lệ vận hành đã có (đơn hoàn tất/thanh toán/tồn kho/sản phẩm đang bán) thành
// 1 mảng cho RadarChart — tái dùng đúng key i18n các gauge cũ, không tạo nhãn mới.
const kpiRadarData = computed(() => [
  { axis: t('admin.dashboard.gaugeCompletion'), value: orderCompletionRate.value },
  { axis: t('admin.dashboard.gaugePayment'), value: paymentRate.value },
  { axis: t('admin.dashboard.gaugeStock'), value: stockHealthRate.value },
  { axis: t('admin.dashboard.activeProducts'), value: activeProductRatio.value },
]);

// Nhân viên theo chức vụ — cho DotMatrix "Positions". Màu cố định nhỏ (không sinh màu
// động) vì số chức vụ thực tế của shop chỉ vài nhóm, lặp lại bảng nếu nhiều hơn.
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
// lowStockOnlyItems/totalStockQty (chỉ dùng trong tab Tồn kho) đã chuyển vào
// components/admin/InventoryPanel.vue (Task 7), cùng khoTab.

// ── Trang Kho hàng: tab-switcher cấp cao (Kho hàng vs Bảo hành) ──────────────────
// Thay cho khoTab cũ (giờ đã chuyển hẳn vào InventoryPanel.vue) — trang này chỉ còn
// đúng 2 lựa chọn: "kho" (InventoryPanel — gồm Tồn kho + Phiếu nhập) và "bao-hanh".
const inventoryMainTab = ref('kho');
// Nhóm "Kho hàng" trong sidebar hiển thị dạng nhãn tĩnh (adm-nav-label) + danh sách mục
// con luôn hiện sẵn bên dưới (không phải nút xổ/thu gọn) — bấm mục con để chuyển tab.
const selectInventoryTab = (tab) => {
  inventoryMainTab.value = tab;
  navigate('inventory');
};
const productsMainTab = ref('sanPham');
const selectProductsTab = (tab) => {
  productsMainTab.value = tab;
  navigate('products');
};

// khoTab + toàn bộ state/hàm của tab Tồn kho (inventorySearch, inventoryGrouped,
// getVariantInfo, stockClass...) và tab Phiếu nhập kho (ensurePhieuNhapData,
// phieuNhapList, savePhieuNhap, printPhieuNhapDetail...) đã chuyển vào
// components/admin/InventoryPanel.vue (Task 7).

// ── Fetch ─────────────────────────────────────────────────────────────────────
// Danh mục/hãng/CPU/RAM/ổ cứng/GPU (ensureProductRefData) đã chuyển vào
// ProductsTable.vue (Task 3) — chỉ tab Sản phẩm cần, vẫn lazy-load riêng.
// Nhân viên + chức vụ NAY tải eager (ensureStaff/ensureChucVuList, có cache promise nên
// gọi lại ở navigate('staff') không tốn thêm lần fetch) — khối "Positions" ở Dashboard
// cần staffByRole ngay khi vào trang, đổi lại quyết định lazy-load cũ (staff từng không
// cần cho dashboard/KPI/POS, nay dashboard cần).
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
// ── Rewards (Đổi thưởng) CRUD ─────────────────────────────────────────────────
const showRewardModal = ref(false);
const editingRewardId = ref(null);
const rewardFormError = ref("");
const emptyRewardForm = () => ({
  ten: "",
  moTa: "",
  diemCan: "",
  loai: "percent",
  giaTri: "",
  giaTriToiDa: "",
  trangThai: "active",
});
const rewardForm = reactive(emptyRewardForm());

const openAddReward = () => {
  Object.assign(rewardForm, emptyRewardForm());
  editingRewardId.value = null;
  rewardFormError.value = "";
  showRewardModal.value = true;
};
const openEditReward = (r) => {
  Object.assign(rewardForm, {
    ten: r.ten,
    moTa: r.moTa ?? "",
    diemCan: r.diemCan ?? "",
    loai: r.loai ?? "percent",
    giaTri: r.giaTri ?? "",
    giaTriToiDa: r.giaTriToiDa ?? "",
    trangThai: r.trangThai ?? "active",
  });
  editingRewardId.value = r.doiThuongId;
  rewardFormError.value = "";
  showRewardModal.value = true;
};
const saveReward = async () => {
  rewardFormError.value = "";
  const body = {
    ...rewardForm,
    diemCan: rewardForm.diemCan ? Number(rewardForm.diemCan) : null,
    giaTri: rewardForm.giaTri ? Number(rewardForm.giaTri) : null,
    giaTriToiDa: rewardForm.giaTriToiDa ? Number(rewardForm.giaTriToiDa) : null,
  };
  try {
    const res = await DmDoiThuongService.save(editingRewardId.value, body);
    if (!res.ok) {
      rewardFormError.value = t('admin.errors.saveFailedWithText', { status: res.status, text: await res.text() });
      return;
    }
    showRewardModal.value = false;
    if (editingRewardId.value) {
      const idx = rewards.value.findIndex((r) => r.doiThuongId === editingRewardId.value);
      if (idx !== -1) rewards.value[idx] = { ...rewards.value[idx], ...body };
    } else {
      await refreshDoiThuong();
    }
  } catch (e) {
    rewardFormError.value = e.message;
  }
};
const deleteReward = async (id) => {
  if (!(await askConfirm(t('admin.confirm.deleteReward')))) return;
  const res = await DmDoiThuongService.remove(id);
  if (!res.ok) { showToast(t('admin.errors.deleteFailed', { status: res.status })); return; }
  await refreshDoiThuong();
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
    await loadWheelConfig();
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
        <div
          class="rounded-circle d-flex align-items-center justify-content-center fw-black flex-shrink-0"
          style="width:38px;height:38px;background:var(--gradient-brand);color:var(--accent-text);font-size:0.8rem;"
        >
          SAO
        </div>
        <div class="adm-brand-text">
          <div class="fw-bold" style="font-size:0.95rem;">{{ t('admin.brand.name') }}</div>
          <div style="font-size:0.7rem;color:var(--text-muted);">{{ t('admin.brand.tagline') }}</div>
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
        <div class="adm-nav adm-subnav" :class="{active: currentPage==='products' && productsMainTab==='sanPham'}" @click="selectProductsTab('sanPham')">
          <Laptop class="adm-icon" :size="15" /> {{ t('admin.productsTabs.sanPham') }}
        </div>
        <div class="adm-nav adm-subnav" :class="{active: currentPage==='products' && productsMainTab==='bienThe'}" @click="selectProductsTab('bienThe')">
          <Layers class="adm-icon" :size="15" /> {{ t('admin.productsTabs.bienThe') }}
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
        <div class="adm-nav adm-subnav" :class="{active: currentPage==='inventory' && inventoryMainTab==='cpu'}" @click="selectInventoryTab('cpu')">
          <Cpu class="adm-icon" :size="15" /> {{ t('admin.productsTabs.cpu') }}
        </div>
        <div class="adm-nav adm-subnav" :class="{active: currentPage==='inventory' && inventoryMainTab==='ram'}" @click="selectInventoryTab('ram')">
          <MemoryStick class="adm-icon" :size="15" /> {{ t('admin.productsTabs.ram') }}
        </div>
        <div class="adm-nav adm-subnav" :class="{active: currentPage==='inventory' && inventoryMainTab==='gpu'}" @click="selectInventoryTab('gpu')">
          <Gamepad2 class="adm-icon" :size="15" /> {{ t('admin.productsTabs.gpu') }}
        </div>
        <div class="adm-nav adm-subnav" :class="{active: currentPage==='inventory' && inventoryMainTab==='o-cung'}" @click="selectInventoryTab('o-cung')">
          <HardDrive class="adm-icon" :size="15" /> {{ t('admin.productsTabs.oCung') }}
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
      </nav>

      <UserProfileMenu @navigate-settings="navigate('settings')" />
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
            class="d-flex align-items-center justify-content-center rounded-2"
            style="width:34px;height:34px;background:var(--bg-hover);cursor:pointer;display:flex;align-items:center;justify-content:center;"
          >
            <Bell :size="18" />
          </div>
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

        <!-- ── San pham: dieu huong qua submenu sidebar (adm-subnav) ── -->
        <section v-show="currentPage === 'products'">
          <div v-show="productsMainTab==='sanPham'">
            <ProductsTable />
          </div>
          <div v-show="productsMainTab==='bienThe'">
            <BienTheTable />
          </div>
        </section>

        <!-- ── Don hang ── -->
        <section v-show="currentPage === 'orders'">
          <OrdersTable />
        </section>

        <!-- ── Khach hang ── -->
        <section v-show="currentPage === 'customers'">
          <CustomersTable @view-detail="openCustomerDetail" />
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

          <!-- ══ TAB: NHA CUNG CAP / LICH SU TON KHO / LINH KIEN — trước đây chỉ có ở
               WarehouseManagementPage.vue (role quan_kho), giờ nối lại cho admin ══ -->
          <div v-show="inventoryMainTab==='suppliers'">
            <SupplierManager />
          </div>
          <div v-show="inventoryMainTab==='lich-su'">
            <InventoryHistoryPanel />
          </div>
          <div v-show="inventoryMainTab==='cpu'">
            <DmCategoryTable :service="DmService.DmCpuService" id-field="cpuId" name-field="tenCpu" :label="t('admin.productsTabs.cpu')" :name-label="t('admin.productsTabs.cpu')" :serial-service="ChiTietCpuService" serial-field-name="cpuId" />
          </div>
          <div v-show="inventoryMainTab==='ram'">
            <DmCategoryTable :service="DmService.DmRamService" id-field="ramId" name-field="dungLuong" :label="t('admin.productsTabs.ram')" :name-label="t('admin.productsTabs.ram')" :serial-service="ChiTietRamService" serial-field-name="ramId" />
          </div>
          <div v-show="inventoryMainTab==='gpu'">
            <DmCategoryTable :service="DmService.DmGpuService" id-field="gpuId" name-field="tenGpu" :label="t('admin.productsTabs.gpu')" :name-label="t('admin.productsTabs.gpu')" :serial-service="ChiTietGpuService" serial-field-name="gpuId" />
          </div>
          <div v-show="inventoryMainTab==='o-cung'">
            <DmCategoryTable :service="DmService.DmOCungService" id-field="oCungId" name-field="loaiOcung" :label="t('admin.productsTabs.oCung')" :name-label="t('admin.productsTabs.oCung')" :serial-service="ChiTietOCungService" serial-field-name="oCungId" />
          </div>
        </section>

        <section v-show="currentPage === 'tra-hang'"><ReturnsPanel :can-pick-staff="true" /></section>

        <!-- ── Danh gia san pham (kiem duyet) ── -->
        <section v-show="currentPage === 'reviews'"><DanhGiaPanel /></section>

        <!-- ── Khuyen mai ── -->
        <section v-show="currentPage === 'promotions'">
          <div
            class="d-flex align-items-center flex-wrap gap-3 p-3 mb-3 rounded-3"
            style="background:var(--bg-card-inset); border:1px solid var(--border-color);"
          >
            <span class="fw-bold small">{{ t('admin.wheelConfig.title') }}</span>
            <label class="small text-secondary mb-0">{{ t('admin.wheelConfig.pointsPerSpin') }}</label>
            <input
              v-model.number="wheelConfig.diemMoiLuot" type="number" min="1"
              class="form-control form-control-sm" style="width:90px;"
            />
            <label class="small text-secondary mb-0">{{ t('admin.wheelConfig.missRate') }}</label>
            <input
              v-model.number="wheelConfig.tyLeTruot" type="number" min="0" max="100"
              class="form-control form-control-sm" style="width:70px;"
            />
            <button class="btn btn-sm btn-warning text-dark fw-bold" :disabled="wheelConfigSaving" @click="saveWheelConfig">
              {{ t('admin.wheelConfig.save') }}
            </button>
            <span v-if="wheelConfigError" class="text-danger small">{{ wheelConfigError }}</span>
          </div>
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
                    </div>
                  </td>
                </tr>
                <tr v-if="promotions.length===0"><td colspan="9" class="text-center text-secondary">{{ t('admin.promotions.empty') }}</td></tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- ── Doi thuong ── -->
        <section v-show="currentPage === 'doi-thuong'">
          <div class="d-flex justify-content-between align-items-center mb-3">
            <span class="text-secondary small">{{ rewards.length }} {{ t('admin.rewards.countSuffix') }}</span>
            <button class="btn btn-sm btn-warning text-dark fw-bold" @click="openAddReward">{{ t('admin.rewards.add') }}</button>
          </div>
          <div v-if="DoiThuongStore.loading" class="text-secondary small">{{ t('admin.rewards.loading') }}</div>
          <div v-else class="table-responsive">
            <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
              <thead><tr><th style="width:40px;">{{ t('admin.common.stt') }}</th><th>{{ t('admin.rewards.colName') }}</th><th>{{ t('admin.rewards.colPoints') }}</th><th>{{ t('admin.rewards.colType') }}</th><th>{{ t('admin.rewards.colValue') }}</th><th>{{ t('admin.rewards.colStatus') }}</th><th>{{ t('admin.rewards.colAction') }}</th></tr></thead>
              <tbody>
                <tr v-for="(r, idx) in rewards" :key="r.doiThuongId">
                  <td class="text-secondary">{{ idx + 1 }}</td>
                  <td>{{ r.ten }}</td>
                  <td>{{ r.diemCan }}</td>
                  <td>{{ r.loai==='percent'?t('admin.rewards.typePercent'):t('admin.rewards.typeFixed') }}</td>
                  <td>{{ r.loai==='percent'?`${r.giaTri}%`:formatPrice(r.giaTri) }}</td>
                  <td><span class="badge" :class="r.trangThai==='active'?'bg-success':'bg-secondary'">{{ statusLabel(r.trangThai) }}</span></td>
                  <td>
                    <div class="d-flex gap-1">
                      <button class="btn btn-sm btn-outline-warning" style="font-size:0.78rem; padding:2px 8px;" @click="openEditReward(r)">{{ t('admin.rewards.edit') }}</button>
                      <button class="btn btn-sm btn-outline-danger" style="font-size:0.78rem; padding:2px 8px;" @click="deleteReward(r.doiThuongId)">{{ t('admin.rewards.delete') }}</button>
                    </div>
                  </td>
                </tr>
                <tr v-if="rewards.length===0"><td colspan="7" class="text-center text-secondary">{{ t('admin.rewards.empty') }}</td></tr>
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
                    </div>
                  </td>
                </tr>
                <tr v-if="staff.length===0"><td colspan="8" class="text-center text-secondary">{{ t('admin.staff.empty') }}</td></tr>
              </tbody>
            </table>
          </div>
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
      </div><!-- /content -->
    </main>
  </div><!-- /dashboard-shell -->

  <!-- ══ MODAL NHAN VIEN ══ -->
  <div v-if="showStaffModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="showStaffModal=false">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:560px;max-width:95vw;max-height:90vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>{{ editingStaffId?t('admin.staffModal.titleEdit'):t('admin.staffModal.titleAdd') }}</span>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="showStaffModal=false"></button>
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
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="showPromoModal=false"></button>
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

  <!-- ══ MODAL DOI THUONG ══ -->
  <div v-if="showRewardModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="showRewardModal=false">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:620px;max-width:95vw;max-height:90vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>{{ editingRewardId?t('admin.rewardModal.titleEdit'):t('admin.rewardModal.titleAdd') }}</span>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="showRewardModal=false"></button>
      </div>
      <div class="overflow-y-auto p-4">
        <div v-if="rewardFormError" class="alert alert-danger small py-2 mb-3">{{ rewardFormError }}</div>
        <div class="row g-3">
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.rewardModal.nameLabel') }}</label><input v-model="rewardForm.ten" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.rewardModal.pointsLabel') }}</label><input v-model="rewardForm.diemCan" type="number" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-12"><label class="form-label small text-secondary">{{ t('admin.rewardModal.descLabel') }}</label><textarea v-model="rewardForm.moTa" rows="2" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)"></textarea></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.rewardModal.typeLabel') }}</label><select v-model="rewardForm.loai" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)"><option value="percent">{{ t('admin.rewardModal.typePercent') }}</option><option value="fixed">{{ t('admin.rewardModal.typeFixed') }}</option></select></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ rewardForm.loai==='percent'?t('admin.rewardModal.valueLabelPercent'):t('admin.rewardModal.valueLabelFixed') }}</label><input v-model="rewardForm.giaTri" type="number" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.rewardModal.maxDiscountLabel') }}</label><input v-model="rewardForm.giaTriToiDa" type="number" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.rewardModal.statusLabel') }}</label><select v-model="rewardForm.trangThai" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)"><option value="active">{{ t('admin.rewardModal.statusActive') }}</option><option value="inactive">{{ t('admin.rewardModal.statusStopped') }}</option></select></div>
        </div>
      </div>
      <div class="d-flex justify-content-end gap-2 p-3 border-top border-secondary">
        <button class="btn btn-sm btn-outline-secondary" @click="showRewardModal=false">{{ t('admin.rewardModal.cancel') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" @click="saveReward">{{ editingRewardId?t('admin.rewardModal.update'):t('admin.rewardModal.addNew') }}</button>
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

/* Muc con cua nhom "Quan ly san pham" / "Quan ly kho hang", thut le duoi nhan nhom */
.adm-subnav { padding-left: 30px; font-size: 0.82rem; }

/* Tieu de phan nhom trong sidebar */
.adm-nav-label {
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.06em;
  color: var(--text-heading);
  text-transform: uppercase;
  padding: 10px 8px 3px;
}

/* Sidebar an/hien theo sidebarOpen o moi kich thuoc man hinh (bam hamburger de bat/tat).
   Desktop: thu gon con dai icon 60px (van dieu huong duoc, khong mat han). Mobile (<768px):
   luon giu width day du, truot vao/ra bang transform (overlay de len noi dung). */
.adm-sidebar {
  width: 240px;
  overflow: hidden;
  transition: width 0.2s ease;
}
.adm-sidebar:not(.adm-sidebar-open) { width: 60px; }

/* Trang thai rail (desktop, dong): chi con icon, an het chu/badge/nhom/chan sidebar.
   Dung font-size:0 de an text-node tran (khong bang <span>) — icon SVG kich thuoc px co dinh
   nen khong bi anh huong. Tren mobile trang thai nay nam ngoai man hinh nen khong ai thay. */
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
