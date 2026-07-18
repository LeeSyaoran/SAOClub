<script setup>
import { ref, computed, onMounted, onUnmounted, reactive, watch } from "vue";
import { AuthStore, clearSession, setSession } from "../stores/index.js";
import { t, I18nStore, LOCALES, setLocale } from "../i18n/index.js";
import { orderStatusLabel, orderStatusColor, orderStatusIcon } from "../utils/orderStatus.js";
import { nowLocalIso } from "../utils/datetime.js";
import * as NhanVienService  from "../Service/NhanVienService.js";
import * as DonHangService   from "../Service/DonHangService.js";
import * as KhuyenMaiService from "../Service/KhuyenMaiService.js";
import * as TonKhoService          from "../Service/TonKhoService.js";
import * as DmService              from "../Service/DmService.js";
import * as ChiTietSanPhamService  from "../Service/ChiTietSanPhamService.js";
import * as PhieuNhapKhoService    from "../Service/PhieuNhapKhoService.js";
import * as ChiTietPhieuNhapService from "../Service/ChiTietPhieuNhapService.js";
import * as DashboardService       from "../Service/DashboardService.js";
import * as XLSX from "xlsx";
import DonutChart from "../components/common/DonutChart.vue";
import RevenueBarChart from "../components/common/RevenueBarChart.vue";
import * as CaiDatService from "../Service/CaiDatService.js";
import { SettingsStore } from "../stores/settings.js";
import BarChart   from "../components/common/BarChart.vue";
import GaugeChart from "../components/common/GaugeChart.vue";
import TrendChart from "../components/common/TrendChart.vue";
import ConfirmDialog from "../components/common/ConfirmDialog.vue";
import SearchSelect from "../components/common/SearchSelect.vue";
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
import { ProductsStore, ensureProducts, refreshProducts } from "../stores/products.js";
import { OrdersStore, ensureOrders, refreshOrders, connectOrderEvents, disconnectOrderEvents } from "../stores/orders.js";
import { CustomersStore, ensureCustomers, refreshCustomers } from "../stores/customers.js";
import { InventoryStore, ensureInventory, refreshInventory } from "../stores/inventory.js";
import { SuppliersStore, ensureSuppliers } from "../stores/suppliers.js";
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

// ── Menu hồ sơ (dropdown ở chân sidebar) ─────────────────────────────────────
const showUserMenu = ref(false);
const userMenuTriggerRef = ref(null);
const closeUserMenu = () => {
  showUserMenu.value = false;
  userMenuTriggerRef.value?.focus();
};
const onUserMenuFocusOut = (e) => {
  if (!e.currentTarget.contains(e.relatedTarget)) showUserMenu.value = false;
};

// ── Modal: Chỉnh sửa hồ sơ ────────────────────────────────────────────────────
const showEditProfileModal = ref(false);
const profileForm = ref({ hoTen: '', soDienThoai: '', email: '' });
const profileSaving = ref(false);
const profileError = ref('');
const profileSaved = ref(false);

const openEditProfileModal = () => {
  showUserMenu.value = false;
  profileForm.value = {
    hoTen: AuthStore.user?.hoTen ?? '',
    soDienThoai: AuthStore.user?.soDienThoai ?? '',
    email: AuthStore.user?.email ?? '',
  };
  profileError.value = '';
  profileSaved.value = false;
  showEditProfileModal.value = true;
};

const saveProfile = async () => {
  profileSaving.value = true;
  profileError.value = '';
  profileSaved.value = false;
  try {
    const res = await CaiDatService.capNhatHoSo(profileForm.value);
    setSession({ ...AuthStore.user, hoTen: res.hoTen, soDienThoai: res.soDienThoai, email: res.email });
    profileSaved.value = true;
  } catch (e) {
    profileError.value = e.message || String(e);
  } finally {
    profileSaving.value = false;
  }
};

// ── Modal: Đổi mật khẩu (link nhanh từ menu hồ sơ — cùng API đã có ở trang Cài đặt) ──
const showQuickPasswordModal = ref(false);
const qpMatKhauCu = ref('');
const qpMatKhauMoi = ref('');
const qpMatKhauXacNhan = ref('');
const qpError = ref('');
const qpSuccess = ref('');
const qpLoading = ref(false);

const openQuickPasswordModal = () => {
  showUserMenu.value = false;
  qpMatKhauCu.value = '';
  qpMatKhauMoi.value = '';
  qpMatKhauXacNhan.value = '';
  qpError.value = '';
  qpSuccess.value = '';
  showQuickPasswordModal.value = true;
};

const quickChangePassword = async () => {
  qpError.value = '';
  qpSuccess.value = '';
  if (qpMatKhauMoi.value !== qpMatKhauXacNhan.value) {
    qpError.value = t('admin.settings.passwordMismatch');
    return;
  }
  qpLoading.value = true;
  try {
    await CaiDatService.doiMatKhau(qpMatKhauCu.value, qpMatKhauMoi.value);
    qpSuccess.value = t('admin.settings.passwordChanged');
    qpMatKhauCu.value = '';
    qpMatKhauMoi.value = '';
    qpMatKhauXacNhan.value = '';
  } catch (e) {
    qpError.value = e.message || String(e);
  } finally {
    qpLoading.value = false;
  }
};

const goToSettingsFromMenu = () => {
  showUserMenu.value = false;
  navigate('settings');
};

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
const suppliers = computed(() => SuppliersStore.items);
const phieuNhapList = ref([]);
const chiTietPhieuNhapList = ref([]);
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
    ensureSuppliers(),
    ensureStaff(),
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

      <!-- Footer sidebar: thong tin user (bam mo dropdown ho so) + logout -->
      <div class="p-3 border-top position-relative" style="border-color:var(--border-color-soft)!important;"
           @keydown.esc="closeUserMenu" @focusout="onUserMenuFocusOut">
        <!-- Dropdown menu ho so — mo LEN tren (bottom:100%) vi dang o cuoi sidebar -->
        <div v-if="showUserMenu" class="position-absolute rounded-3 shadow-lg overflow-hidden"
             style="left:12px; right:12px; bottom:100%; margin-bottom:8px; background:var(--bg-card); border:1px solid var(--border-color); z-index:50;">
          <button class="btn btn-sm w-100 text-start rounded-0 border-0" style="color:var(--text-primary);" @click="openEditProfileModal">
            {{ t('admin.profileMenu.editProfile') }}
          </button>
          <button class="btn btn-sm w-100 text-start rounded-0 border-0" style="color:var(--text-primary);" @click="openQuickPasswordModal">
            {{ t('admin.settings.changePasswordTitle') }}
          </button>
          <button class="btn btn-sm w-100 text-start rounded-0 border-0" style="color:var(--text-primary);" @click="goToSettingsFromMenu">
            {{ t('admin.sidebar.settings') }}
          </button>
        </div>

        <button ref="userMenuTriggerRef" type="button"
                class="btn d-flex align-items-center gap-2 mb-2 w-100 text-start p-0 border-0"
                style="background:transparent;"
                aria-haspopup="true" :aria-expanded="showUserMenu"
                @click="showUserMenu = !showUserMenu">
          <div class="rounded-circle d-flex align-items-center justify-content-center fw-bold flex-shrink-0"
               style="width:34px;height:34px;background:var(--accent);color:var(--accent-text);font-size:0.9rem;">{{ userAvatar }}</div>
          <div class="flex-grow-1" style="min-width:0;">
            <div class="fw-semibold text-truncate" style="font-size:0.85rem;">{{ userDisplayName }}</div>
            <div style="font-size:0.72rem;color:var(--text-muted);">{{ userDisplayRole }}</div>
          </div>
        </button>
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
          <ProductsTable />
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

          <div v-if="InventoryStore.loading" class="text-secondary small py-4 text-center">{{ t('admin.inventory.loading') }}</div>
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

        <!-- ══ MODAL CHINH SUA HO SO ══ -->
        <div v-if="showEditProfileModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1070;" @click.self="showEditProfileModal=false">
          <div class="rounded-3 p-3" style="background:var(--bg-card);width:420px;max-width:94vw;">
            <div class="d-flex justify-content-between align-items-center mb-3">
              <div class="fw-bold" style="color:var(--text-heading);">{{ t('admin.profileMenu.editProfile') }}</div>
              <button class="btn-close btn-close-white btn-sm" @click="showEditProfileModal=false"></button>
            </div>
            <div class="mb-2">
              <label class="form-label small text-secondary mb-1">{{ t('admin.profileMenu.fullName') }}</label>
              <input v-model="profileForm.hoTen" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
            </div>
            <div class="mb-2">
              <label class="form-label small text-secondary mb-1">{{ t('admin.profileMenu.phone') }}</label>
              <input v-model="profileForm.soDienThoai" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
            </div>
            <div class="mb-3">
              <label class="form-label small text-secondary mb-1">{{ t('admin.profileMenu.email') }}</label>
              <input v-model="profileForm.email" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
            </div>
            <div v-if="profileError" class="text-danger small mb-2">{{ profileError }}</div>
            <div v-if="profileSaved" class="text-success small mb-2">{{ t('admin.profileMenu.profileSaved') }}</div>
            <div class="d-flex justify-content-end gap-2">
              <button class="btn btn-sm btn-outline-secondary" @click="showEditProfileModal=false">{{ t('admin.productModal.cancel') }}</button>
              <button class="btn btn-sm btn-warning" :disabled="profileSaving" @click="saveProfile">{{ t('admin.settings.saveButton') }}</button>
            </div>
          </div>
        </div>

        <!-- ══ MODAL DOI MAT KHAU NHANH (tu menu ho so) ══ -->
        <div v-if="showQuickPasswordModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1070;" @click.self="showQuickPasswordModal=false">
          <div class="rounded-3 p-3" style="background:var(--bg-card);width:420px;max-width:94vw;">
            <div class="d-flex justify-content-between align-items-center mb-3">
              <div class="fw-bold" style="color:var(--text-heading);">{{ t('admin.settings.changePasswordTitle') }}</div>
              <button class="btn-close btn-close-white btn-sm" @click="showQuickPasswordModal=false"></button>
            </div>
            <div class="mb-2">
              <label class="form-label small text-secondary mb-1">{{ t('admin.settings.currentPassword') }}</label>
              <input type="password" v-model="qpMatKhauCu" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
            </div>
            <div class="mb-2">
              <label class="form-label small text-secondary mb-1">{{ t('admin.settings.newPassword') }}</label>
              <input type="password" v-model="qpMatKhauMoi" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
            </div>
            <div class="mb-3">
              <label class="form-label small text-secondary mb-1">{{ t('admin.settings.confirmNewPassword') }}</label>
              <input type="password" v-model="qpMatKhauXacNhan" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
            </div>
            <div v-if="qpError" class="text-danger small mb-2">{{ qpError }}</div>
            <div v-if="qpSuccess" class="text-success small mb-2">{{ qpSuccess }}</div>
            <div class="d-flex justify-content-end gap-2">
              <button class="btn btn-sm btn-outline-secondary" @click="showQuickPasswordModal=false">{{ t('admin.productModal.cancel') }}</button>
              <button class="btn btn-sm btn-warning" :disabled="qpLoading || !qpMatKhauCu || !qpMatKhauMoi" @click="quickChangePassword">{{ t('admin.settings.changePasswordButton') }}</button>
            </div>
          </div>
        </div>

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

        <div class="fw-semibold small text-secondary mb-2">📦 {{ t('admin.phieuNhapModal.itemsLabel') }}</div>
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
  <ToastHost />
</template>

<style scoped>

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
</style>
