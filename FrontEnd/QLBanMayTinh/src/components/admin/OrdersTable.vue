<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from "vue";
import { Search } from "@lucide/vue";
import { Filter, X, ChevronDown, ChevronUp } from "@lucide/vue";
import { t } from "../../i18n/index.js";
import {
  orderStatusLabel,
  orderStatusColor,
  orderStatusIcon,
  paymentStatusLabel,
  paymentStatusColor,
  paymentStatusIcon,
  paymentMethodLabel,
  paymentMethodIcon,
  channelLabel,
  channelColor,
  isQrPayment,
  QR_TIMELINE_STEPS,
  isQrStepReached,
  isQrStepDone,
  isQrStepCurrent,
  isQrStepNext,
  getQrEffectiveStatus,
  COD_TIMELINE_STEPS,
  isCodStepReached,
  isCodStepDone,
  isCodStepCurrent,
  isCodStepNext,
  getCodEffectiveStatus,
  getCodLinearStatusIndex,
  COD_LINEAR_STATUS_ORDER,
} from "../../utils/orderStatus.js";
import { nowLocalIso } from "../../utils/datetime.js";
import { formatPrice, formatDate, formatDateTime } from "../../utils/adminFormat.js";
import { authHeaders } from "../../services/api.js";
import { showToast } from "../../stores/toast.js";
import { askConfirm } from "../../stores/confirm.js";
import * as DonHangService from "../../services/DonHangService.js";
import * as ChiTietDonHangService from "../../services/ChiTietDonHangService.js";
import * as ChiTietDonHangSerialService from "../../services/ChiTietDonHangSerialService.js";
import * as ChiTietSanPhamService from "../../services/ChiTietSanPhamService.js";
import * as ThanhToanService from "../../services/ThanhToanService.js";
import { OrdersStore, ensureOrders, refreshOrders } from "../../stores/orders.js";
import { CustomersStore, ensureCustomers } from "../../stores/customers.js";
import { ProductsStore, ensureProducts } from "../../stores/products.js";
import { AuthStore } from "../../stores/index.js";
import ProductDetailModal from "./ProductDetailModal.vue";
import Pagination from "../common/Pagination.vue";
import { usePagination } from "../../composables/usePagination.js";
import {
  CheckCircle2,
  Check,
  Package,
  Truck,
  Bike,
  Inbox,
  Laptop,
  User,
  Printer,
  Phone,
  Mail,
  MapPin,
  QrCode,
  ShieldCheck,
  Clock,
  FileText,
  Info,
  AlertCircle,
} from "@lucide/vue";
import InvoiceModal from "./InvoiceModal.vue";

// Nhận order ID để navigate từ ngoài (CustomerDetailModal tab đơn hàng → "Xem chi tiết")
const props = defineProps({
  navigateToOrderId: { type: Number, default: null },
});
const emit = defineEmits(["order-detail-opened"]);

onMounted(() => {
  ensureOrders();
  ensureCustomers();
  ensureProducts();
});

// SSE realtime: khi có đơn cập nhật và modal đang mở → refresh modal ngay (không cần reload)
let orderDetailEventSource = null;
const setupOrderDetailSse = () => {
  if (orderDetailEventSource) return;
  orderDetailEventSource = new EventSource("/api/don-hang/events");
  orderDetailEventSource.addEventListener("order-updated", async () => {
    await refreshOrders();
    const openedId = orderDetailData.value?.donHangId;
    if (!openedId) return;
    // refreshOrderDetail() chỉ chạy nếu modal đang hiển thị (data đã set)
    const fresh = (OrdersStore.items ?? []).find((o) => o.donHangId === openedId);
    if (fresh) {
      orderDetailData.value = fresh;
      orderDetailItems.value = await loadOrderDetailItems(fresh);
      orderDetailPayments.value = await ThanhToanService.getByDonHang(openedId).catch(() => []);
    }
  });
};
const teardownOrderDetailSse = () => {
  if (orderDetailEventSource) {
    orderDetailEventSource.close();
    orderDetailEventSource = null;
  }
};
onMounted(setupOrderDetailSse);
onUnmounted(teardownOrderDetailSse);

// Khi navigateToOrderId được set từ bên ngoài → mở modal chi tiết đơn đó
watch(
  () => props.navigateToOrderId,
  (id) => {
    if (!id) return;
    const order = (OrdersStore.items ?? []).find((o) => o.donHangId === id);
    if (order) openOrderDetail(order);
  },
  { immediate: true },
);

// ── Helpers ───────────────────────────────────────────────────────────────────
const customerObj = (id) => (CustomersStore.items ?? []).find((c) => c.khachHangId === id);

const customerName = (id) => customerObj(id)?.hoTen ?? `KH#${id}`;

const customerPhone = (order) =>
  order?.sdtNguoiNhan || order?.khachHangSdt || customerObj(order?.khachHangId)?.soDienThoai || "";

const customerEmail = (order) => customerObj(order?.khachHangId)?.email || "";

const deliveryAddressText = (order) => {
  if (order?.diaChiGiaoHangText) return order.diaChiGiaoHangText;
  if (order?.khachHangDiaChi) return order.khachHangDiaChi;
  const c = customerObj(order?.khachHangId);
  if (c?.diaChi) return c.diaChi;
  if (order?.kenhBan === "in_store") return "Khách nhận tại quầy";
  return "Chưa có địa chỉ giao hàng";
};

// Định dạng ngày YYYY-MM-DD cho input date
const toDateInputValue = (d) => {
  const y = d.getFullYear(),
    m = String(d.getMonth() + 1).padStart(2, "0"),
    day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
};

// ── Bộ lọc nâng cao: Đơn hàng ────────────────────────────────────────────────────
const orderSearch = ref("");
const isOrderFilterOpen = ref(false);
const orderFilters = reactive({
  trangThaiDonHang: "",
  trangThaiThanhToan: "",
  kenhBan: "",
  ngayFrom: "",
  ngayTo: "",
  tongTienMin: "",
  tongTienMax: "",
});

const activeOrderFilterCount = computed(
  () =>
    [
      orderFilters.trangThaiDonHang,
      orderFilters.trangThaiThanhToan,
      orderFilters.kenhBan,
      orderFilters.ngayFrom,
      orderFilters.ngayTo,
      orderFilters.tongTienMin !== "" ? orderFilters.tongTienMin : "",
      orderFilters.tongTienMax !== "" ? orderFilters.tongTienMax : "",
    ].filter((v) => v !== "").length,
);

const resetOrderFilters = () => {
  orderFilters.trangThaiDonHang = "";
  orderFilters.trangThaiThanhToan = "";
  orderFilters.kenhBan = "";
  orderFilters.ngayFrom = "";
  orderFilters.ngayTo = "";
  orderFilters.tongTienMin = "";
  orderFilters.tongTienMax = "";
  orderSearch.value = "";
};

// Chế độ xem danh sách đơn hàng
const orderViewMode = ref("today");
const historySelectedDate = ref(null); // 'YYYY-MM-DD'

// Danh sách đơn hàng làm nền cho bộ lọc bên dưới, tùy theo chế độ xem đang chọn.
const ordersBaseList = computed(() => {
  const all = OrdersStore.items ?? [];
  if (orderViewMode.value === "history-day" && historySelectedDate.value) {
    return all.filter((o) => o.ngayDat?.slice(0, 10) === historySelectedDate.value);
  }
  if (orderViewMode.value === "today") {
    return all.filter((o) => o.ngayDat?.slice(0, 10) === toDateInputValue(new Date()));
  }
  return all;
});

const filteredOrders = computed(() => {
  const q = orderSearch.value.trim().toLowerCase();
  return ordersBaseList.value.filter((o) => {
    if (orderFilters.trangThaiDonHang && o.trangThaiDonHang !== orderFilters.trangThaiDonHang)
      return false;
    if (orderFilters.trangThaiThanhToan && o.trangThaiThanhToan !== orderFilters.trangThaiThanhToan)
      return false;
    if (orderFilters.kenhBan && o.kenhBan !== orderFilters.kenhBan) return false;
    // ngày đặt từ-đến
    if (orderFilters.ngayFrom && (o.ngayDat ?? "").slice(0, 10) < orderFilters.ngayFrom)
      return false;
    if (orderFilters.ngayTo && (o.ngayDat ?? "").slice(0, 10) > orderFilters.ngayTo) return false;
    // khoảng tổng tiền
    const tong = Number(o.tongTien ?? 0);
    if (orderFilters.tongTienMin !== "" && tong < Number(orderFilters.tongTienMin)) return false;
    if (orderFilters.tongTienMax !== "" && tong > Number(orderFilters.tongTienMax)) return false;
    if (!q) return true;
    const name = customerName(o.khachHangId).toLowerCase();
    return (
      String(o.donHangId).includes(q) ||
      (o.maDonHang ?? "").toLowerCase().includes(q) ||
      name.includes(q) ||
      (o.nguoiNhan ?? "").toLowerCase().includes(q) ||
      (o.sdtNguoiNhan ?? "").includes(q)
    );
  });
});
const {
  currentPage,
  totalPages,
  pagedItems: pagedOrders,
  pageSize,
} = usePagination(filteredOrders);
watch(
  [
    orderSearch,
    () => orderFilters.trangThaiDonHang,
    () => orderFilters.trangThaiThanhToan,
    () => orderFilters.kenhBan,
    () => orderFilters.ngayFrom,
    () => orderFilters.ngayTo,
    () => orderFilters.tongTienMin,
    () => orderFilters.tongTienMax,
  ],
  () => {
    currentPage.value = 0;
  },
);

// Danh sách ngày có đơn hàng (mới nhất trước), dùng cho màn "Lịch sử đơn hàng"
const VN_WEEKDAYS = ["Chủ nhật", "Thứ hai", "Thứ ba", "Thứ tư", "Thứ năm", "Thứ sáu", "Thứ bảy"];
const formatDateHeading = (dateKey) => {
  const [y, m, d] = dateKey.split("-").map(Number);
  const dt = new Date(y, m - 1, d);
  return `${VN_WEEKDAYS[dt.getDay()]}, ${String(d).padStart(2, "0")}/${String(m).padStart(2, "0")}/${y}`;
};
const orderDatesGrouped = computed(() => {
  const map = {};
  (OrdersStore.items ?? []).forEach((o) => {
    const key = o.ngayDat?.slice(0, 10);
    if (key) map[key] = (map[key] || 0) + 1;
  });
  return Object.keys(map)
    .sort((a, b) => b.localeCompare(a))
    .map((key) => ({ dateKey: key, label: formatDateHeading(key), count: map[key] }));
});

const openOrderHistory = () => {
  orderViewMode.value = "history-dates";
};
const openHistoryDay = (dateKey) => {
  historySelectedDate.value = dateKey;
  orderViewMode.value = "history-day";
};
const backToToday = () => {
  orderViewMode.value = "today";
  historySelectedDate.value = null;
};
const backToDateList = () => {
  orderViewMode.value = "history-dates";
  historySelectedDate.value = null;
};

// Tải danh sách serial theo biến thể
const fetchSerialMap = async (bienTheIds) => {
  const results = await Promise.all(
    bienTheIds.map((id) => ChiTietSanPhamService.getByBienThe(id).catch(() => [])),
  );
  const map = {};
  bienTheIds.forEach((id, i) => {
    map[id] = results[i];
  });
  return map;
};

// ── Order detail modal (xem san pham trong don) ───────────────────────────────
const showOrderDetailModal = ref(false);
const orderDetailData = ref(null); // don hang dang xem
const orderDetailItems = ref([]); // ChiTietDonHangResponse[]
const orderDetailPayments = ref([]); // ThanhToanResponse[] — co the rong (don cu/don online)
const orderDetailLoading = ref(false);

// Invoice modal
const showInvoice = ref(false);
const invoiceOrder = ref(null);

// Gom các khoản thanh toán theo phương thức
const orderDetailPaymentsSummary = computed(() => {
  const map = new Map();
  for (const p of orderDetailPayments.value) {
    const cur = map.get(p.phuongThucThanhToan) ?? {
      method: p.phuongThucThanhToan,
      count: 0,
      total: 0,
    };
    cur.count += 1;
    cur.total += p.soTien ?? 0;
    map.set(p.phuongThucThanhToan, cur);
  }
  return [...map.values()];
});

// Tải chi tiết sản phẩm trong đơn, ẩn serial nếu trước bước Đã lên đơn hoặc khi admin/nhân viên chưa chọn serial (trạng thái serial vẫn là trong_kho)
const loadOrderDetailItems = async (o) => {
  if (!o) return [];
  const items = await ChiTietDonHangService.getByDonHang(o.donHangId).catch((err) => {
    console.error("chi tiet don hang error", err);
    return [];
  });
  if (o.kenhBan === "online") {
    if (o.trangThaiDonHang === "pending") {
      return items.map((item) => ({ ...item, soSerial: null, serialChosen: false }));
    }
    const bienTheIds = [...new Set(items.map((i) => i.bienTheId).filter(Boolean))];
    const serialMap = await fetchSerialMap(bienTheIds);
    return items.map((item) => {
      if (!item.soSerial) {
        return { ...item, soSerial: null, serialChosen: false };
      }
      const list = serialMap[item.bienTheId] ?? [];
      const found = list.find(
        (s) => s.chiTietId === item.chiTietId || s.soSerial === item.soSerial,
      );
      if (found && found.trangThai === "trong_kho") {
        return { ...item, soSerial: null, serialChosen: false };
      }
      return { ...item, serialChosen: true };
    });
  }
  return items.map((item) => ({ ...item, serialChosen: Boolean(item.soSerial) }));
};

const hasChosenAllSerials = computed(() => {
  if (!orderDetailItems.value.length) return false;
  return orderDetailItems.value.every((item) => Boolean(item.serialChosen && item.soSerial));
});

const openOrderDetail = async (o) => {
  orderDetailData.value = o;
  orderDetailItems.value = [];
  orderDetailPayments.value = [];
  showOrderDetailModal.value = true;
  orderDetailLoading.value = true;
  emit("order-detail-opened", o.donHangId); // thông báo cho AdminPage reset selectedOrderId
  // Chuyển đơn tại quầy sang delivered
  if (o.kenhBan === "in_store" && o.trangThaiDonHang === "pending") {
    jumpToStatus(o, "delivered").catch(() => {});
  }
  try {
    orderDetailItems.value = await loadOrderDetailItems(o);
    orderDetailPayments.value = await ThanhToanService.getByDonHang(o.donHangId).catch((err) => {
      console.error("thanh toan error", err);
      return [];
    });
  } finally {
    orderDetailLoading.value = false;
  }
};

// Tim ten san pham tu bienTheId trong danh sach products da load
const productByBienThe = (bienTheId) =>
  (ProductsStore.items ?? []).find((p) => p.bienTheId === bienTheId);

// ── Them san pham vao don ─────────────────────────────────────────────────────
const addItemMode = ref(false);
const addItemBienTheId = ref("");
const addItemQty = ref(1);
const addItemLoading = ref(false);
const addItemSearch = ref("");
const addItemSelectedSpId = ref(null); // sanPhamId dang mo xem bien the

// Modal chi tiet san pham (khi click vao card)
const showAddItemDetailModal = ref(false);
const addItemDetailGroup = ref(null); // group dang xem { sanPhamId, tenSanPham, ... variants[] }
const addItemSelectedConfig = ref(null); // cpu+ram+oCung key
const addItemSelectedColor = ref(null); // mauSac

// Cac phien ban doc nhat (cpu + ram + oCung) cho san pham dang xem
const addItemConfigs = computed(() => {
  if (!addItemDetailGroup.value) return [];
  const seen = new Set();
  const result = [];
  for (const v of addItemDetailGroup.value.variants) {
    const key = [v.cpu, v.ram, v.oCung].filter(Boolean).join("|");
    if (!seen.has(key)) {
      seen.add(key);
      result.push({ key, cpu: v.cpu, ram: v.ram, oCung: v.oCung });
    }
  }
  return result;
});

// Cac mau sac trong phien ban dang chon
const addItemColorsForConfig = computed(() => {
  if (!addItemDetailGroup.value || !addItemSelectedConfig.value) return [];
  const [cpu, ram, oCung] = addItemSelectedConfig.value.split("|");
  return addItemDetailGroup.value.variants.filter(
    (v) =>
      (v.cpu || "") === (cpu || "") &&
      (v.ram || "") === (ram || "") &&
      (v.oCung || "") === (oCung || ""),
  );
});

// Bien the hien tai dua vao config + mau sac dang chon
const addItemCurrentVariant = computed(
  () =>
    addItemColorsForConfig.value.find((v) => v.mauSac === addItemSelectedColor.value) ||
    addItemColorsForConfig.value[0] ||
    null,
);

const openAddItemDetail = (group) => {
  addItemDetailGroup.value = group;
  addItemSelectedConfig.value = addItemConfigs.value[0]?.key ?? null;
  addItemSelectedColor.value = addItemColorsForConfig.value[0]?.mauSac ?? null;
  showAddItemDetailModal.value = true;
};

// Khi chon config moi → reset color ve first of that config
const selectConfig = (key) => {
  addItemSelectedConfig.value = key;
  const [cpu, ram, oCung] = key.split("|");
  const first = addItemDetailGroup.value?.variants.find(
    (v) =>
      (v.cpu || "") === (cpu || "") &&
      (v.ram || "") === (ram || "") &&
      (v.oCung || "") === (oCung || ""),
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
      donHangId: orderDetailData.value.donHangId,
      bienTheId: v.bienTheId,
      soLuong: addItemQty.value,
      donGia: v.giaBan,
      giamGiaDong: 0,
    });
    if (!res.ok) {
      showToast(t("admin.errors.addItemFailed", { status: res.status }));
      return;
    }
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
  for (const p of ProductsStore.items ?? []) {
    if (!map[p.sanPhamId]) {
      map[p.sanPhamId] = {
        sanPhamId: p.sanPhamId,
        tenSanPham: p.tenSanPham,
        tenThuongHieu: p.tenThuongHieu,
        hinhAnhChinh: p.hinhAnhChinh,
        phanLoaiTen: p.phanLoaiTen,
        variants: [],
      };
    }
    map[p.sanPhamId].variants.push(p);
  }
  let groups = Object.values(map);
  if (q)
    groups = groups.filter(
      (g) => g.tenSanPham.toLowerCase().includes(q) || g.tenThuongHieu?.toLowerCase().includes(q),
    );
  groups.forEach((g) => {
    g.minPrice = Math.min(...g.variants.map((v) => Number(v.giaBan) || 0));
  });
  return groups.sort((a, b) => a.tenSanPham.localeCompare(b.tenSanPham));
});

const refreshOrderDetail = async () => {
  await refreshOrders();
  const updated = (OrdersStore.items ?? []).find(
    (o) => o.donHangId === orderDetailData.value?.donHangId,
  );
  if (updated) orderDetailData.value = updated;
  orderDetailItems.value = await loadOrderDetailItems(orderDetailData.value);
  orderDetailPayments.value = await ThanhToanService.getByDonHang(
    orderDetailData.value.donHangId,
  ).catch(() => []);
};

const addItemToOrder = async () => {
  if (!addItemBienTheId.value || addItemQty.value < 1) return;
  const v = productByBienThe(Number(addItemBienTheId.value));
  if (!v) return;
  addItemLoading.value = true;
  try {
    const res = await DonHangService.addChiTiet({
      donHangId: orderDetailData.value.donHangId,
      bienTheId: v.bienTheId,
      soLuong: addItemQty.value,
      donGia: v.giaBan,
      giamGiaDong: 0,
    });
    if (!res.ok) {
      showToast(t("admin.errors.addItemFailed", { status: res.status }));
      return;
    }
    await DonHangService.recalculate(orderDetailData.value.donHangId);
    await refreshOrderDetail();
    addItemBienTheId.value = "";
    addItemQty.value = 1;
    addItemMode.value = false;
  } finally {
    addItemLoading.value = false;
  }
};

const removeItemFromOrder = async (chiTietId) => {
  if (!(await askConfirm(t("admin.confirm.removeItemFromOrder")))) return;
  const res = await fetch(`/api/chi-tiet-don-hang/delete/${chiTietId}`, {
    method: "DELETE",
    headers: authHeaders(),
  });
  if (!res.ok) {
    showToast(t("admin.errors.deleteFailed", { status: res.status }));
    return;
  }
  await DonHangService.recalculate(orderDetailData.value.donHangId);
  await refreshOrderDetail();
};

// Modal xem chi tiết biến thể sản phẩm
const showDetailModal = ref(false);
const detailModalSanPhamId = ref(null);
const detailModalSanPhamName = ref("");
const detailModalBienTheIds = ref([]);

const openVariantDetail = (bienTheId) => {
  const v = productByBienThe(bienTheId);
  if (!v) return;
  detailModalSanPhamId.value = v.sanPhamId;
  detailModalSanPhamName.value = v.tenSanPham;
  // Hiển thị biến thể trong chi tiết đơn hàng
  detailModalBienTheIds.value = [
    ...new Set(
      orderDetailItems.value
        .map((item) => item.bienTheId)
        .filter((id) => productByBienThe(id)?.sanPhamId === v.sanPhamId),
    ),
  ];
  showDetailModal.value = true;
};

// ── Order status helpers (dùng chung — xem src/utils/orderStatus.js) ──────────

// Tạo dữ liệu cập nhật trạng thái đơn hàng
const buildOrderUpdateBody = (
  o,
  { trangThaiDonHang, trangThaiThanhToan, ngayGiaoDuKien, ngayGiaoThucTe, maVanDon },
) => ({
  khachHangId: o.khachHangId,
  nhanVienId: o.nhanVienId ?? null,
  khuyenMaiId: o.khuyenMaiId ?? null,
  diaChiGiaoHangId: o.diaChiGiaoHangId ?? null,
  diaChiGiaoHangText: o.diaChiGiaoHangText ?? null,
  nguoiNhan: o.nguoiNhan || customerName(o.khachHangId),
  sdtNguoiNhan:
    o.sdtNguoiNhan ||
    ((CustomersStore.items ?? []).find((c) => c.khachHangId === o.khachHangId)?.soDienThoai ?? ""),
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
  maVanDon: maVanDon || null,
  phuongThucThanhToan: o.phuongThucThanhToan || o.phuongThuc || null,
  idempotencyKey: o.idempotencyKey ?? null,
});

// Trạng thái đơn hàng kế tiếp (từng bước 1)
const NEXT_ORDER_STATUS = {
  pending: "confirmed",
  confirmed: "processing",
  processing: "out_for_delivery",
  shipping: "out_for_delivery",
  out_for_delivery: "awaiting_confirmation",
  awaiting_confirmation: "delivered",
};
const NEXT_ORDER_STATUS_LABEL = {
  pending: { icon: CheckCircle2, key: "admin.orders.nextConfirm" },
  confirmed: { icon: Package, key: "admin.orders.nextPack" },
  processing: { icon: Bike, key: "admin.orders.nextOutForDelivery" },
  shipping: { icon: Bike, key: "admin.orders.nextOutForDelivery" },
  out_for_delivery: { icon: CheckCircle2, key: "admin.orders.nextDelivered" },
  awaiting_confirmation: { icon: CheckCircle2, key: "admin.orders.nextDelivered" },
};

// Timeline đơn hàng: Phân biệt rõ đơn Thanh toán sau (5 bước) và Thanh toán QR (7 bước)
const orderTimelineSteps = computed(() => {
  if (orderDetailData.value?.kenhBan === "in_store") {
    return [
      {
        id: "delivered",
        title: t("orderStatus.timeline.deliveredTitle"),
        desc:
          t("orderStatus.timeline.inStoreDeliveredDesc") || t("orderStatus.timeline.deliveredDesc"),
        icon: CheckCircle2,
      },
    ];
  }
  if (isQrPayment(orderDetailData.value)) {
    return QR_TIMELINE_STEPS;
  }
  return COD_TIMELINE_STEPS;
});

// Kiểm tra trạng thái đã qua trên timeline
const isStepReached = (order, stepId) => {
  if (order?.kenhBan === "in_store") {
    return !["cancelled", "returned"].includes(order.trangThaiDonHang);
  }
  if (isQrPayment(order)) {
    return isQrStepReached(order, stepId);
  }
  return isCodStepReached(order, stepId);
};

// Kiểm tra bước đã hoàn tất trên timeline
const isStepDoneById = (order, stepId) => {
  if (order?.kenhBan === "in_store") {
    return !["cancelled", "returned"].includes(order.trangThaiDonHang);
  }
  if (isQrPayment(order)) {
    return isQrStepDone(order, stepId);
  }
  return isCodStepDone(order, stepId);
};

// Kiểm tra bước hiện tại
const isStepCurrentById = (order, stepId) => {
  if (order?.kenhBan === "in_store") return stepId === "delivered";
  if (isQrPayment(order)) {
    return isQrStepCurrent(order, stepId);
  }
  return isCodStepCurrent(order, stepId);
};

// Kiểm tra bước kế tiếp đang chuẩn bị thực hiện (để sáng lên)
const isStepNextById = (order, stepId) => {
  if (order?.kenhBan === "in_store") return false;
  if (isQrPayment(order)) {
    return isQrStepNext(order, stepId);
  }
  return isCodStepNext(order, stepId);
};

// Kiểm tra bước đã được đến (reached) — dùng để hiển thị màu cam "đang chờ" khi chưa done
const isStepReachedById = (order, stepId) => {
  if (!order) return false;
  if (order?.kenhBan === "in_store")
    return !["cancelled", "returned"].includes(order.trangThaiDonHang);
  if (isQrPayment(order)) return isQrStepReached(order, stepId);
  return isCodStepReached(order, stepId);
};

// Lấy ID của bước đang active cần thực hiện tiếp theo (nhảy đúng từng bước 1)
const getNextStepId = (order) => {
  if (!order) return null;
  if (["cancelled", "returned", "delivered"].includes(order.trangThaiDonHang)) return null;
  if (order?.kenhBan === "in_store") return null;

  if (isQrPayment(order)) {
    // Ở bước Chờ thanh toán phải đợi khách thanh toán mới được nhấn các bước dưới
    if (order.trangThaiThanhToan !== "paid") {
      return null;
    }
    const cur = order.trangThaiDonHang;
    if (cur === "pending") return "cho_xu_ly";
    if (cur === "confirmed") return "da_len_don";
    if (cur === "processing" || cur === "shipping") return "dang_dong_goi";
    if (cur === "out_for_delivery") return "dang_giao_hang";
    if (cur === "awaiting_confirmation") return "da_giao";
    return null;
  }

  const cur = order.trangThaiDonHang;
  if (cur === "pending") return "pending";
  if (cur === "confirmed") return "confirmed";
  if (cur === "processing" || cur === "shipping") return "processing";
  if (cur === "out_for_delivery") return "out_for_delivery";
  if (cur === "awaiting_confirmation") return "delivered";
  return null;
};

// Bấm được đúng vào bước đang active trên thanh tiến trình (step by step)
const canJumpToStep = (order, stepId) => {
  if (!order) return false;
  if (["cancelled", "returned", "delivered"].includes(order.trangThaiDonHang)) return false;
  if (order?.kenhBan === "in_store") return false;

  const nextStep = getNextStepId(order);
  if (!nextStep) return false;
  if (stepId === nextStep) return true;

  // Cho phép bấm vào bước kế tiếp liền kề để chuyển trạng thái trực tiếp
  const cur = order.trangThaiDonHang;
  if (isQrPayment(order)) {
    if (cur === "pending" && stepId === "da_len_don") return true;
    if (cur === "confirmed" && stepId === "dang_dong_goi") return true;
    if ((cur === "processing" || cur === "shipping") && stepId === "dang_giao_hang") return true;
    if (cur === "out_for_delivery" && stepId === "da_giao") return true;
  } else {
    if (cur === "pending" && stepId === "confirmed") return true;
    if (cur === "confirmed" && stepId === "processing") return true;
    if ((cur === "processing" || cur === "shipping") && stepId === "out_for_delivery") return true;
    if (cur === "out_for_delivery" && stepId === "delivered") return true;
  }

  return false;
};

const getStepActionTitle = (order, stepId) => {
  if (!order) return "";
  if (isQrPayment(order) && order.trangThaiThanhToan !== "paid") {
    if (stepId === "cho_thanh_toan") return "Đang chờ khách hàng thanh toán qua mã QR";
    return "Phải đợi khách thanh toán mới được nhấn các bước dưới";
  }
  if (!canJumpToStep(order, stepId)) return "";
  if (isQrPayment(order)) {
    if (stepId === "cho_xu_ly" || (order.trangThaiDonHang === "pending" && stepId === "da_len_don")) {
      return 'Xác nhận kiểm tra → chuyển sang "Đã lên đơn"';
    }
    if (stepId === "da_len_don" || (order.trangThaiDonHang === "confirmed" && stepId === "dang_dong_goi")) {
      return 'Xác nhận lên đơn → chuyển sang "Đang đóng gói"';
    }
    if (stepId === "dang_dong_goi" || ((order.trangThaiDonHang === "processing" || order.trangThaiDonHang === "shipping") && stepId === "dang_giao_hang")) {
      return 'Hoàn tất đóng gói → chuyển sang "Đang giao hàng"';
    }
    if (stepId === "dang_giao_hang" || (order.trangThaiDonHang === "out_for_delivery" && stepId === "da_giao")) {
      return 'Xác nhận giao hàng → chuyển sang bước "Đã giao"';
    }
    if (stepId === "da_giao") return 'Xác nhận hoàn tất "Đã giao"';
  } else {
    if (stepId === "pending" || (order.trangThaiDonHang === "pending" && stepId === "confirmed")) {
      return 'Xác nhận đơn hàng → chuyển sang "Đã lên đơn"';
    }
    if (stepId === "confirmed" || (order.trangThaiDonHang === "confirmed" && stepId === "processing")) {
      return 'Xác nhận lên đơn → chuyển sang "Đang đóng gói"';
    }
    if (stepId === "processing" || ((order.trangThaiDonHang === "processing" || order.trangThaiDonHang === "shipping") && stepId === "out_for_delivery")) {
      return 'Hoàn tất đóng gói → chuyển sang "Đang giao hàng"';
    }
    if (stepId === "out_for_delivery" || (order.trangThaiDonHang === "out_for_delivery" && stepId === "delivered")) {
      return 'Xác nhận giao hàng → chuyển sang bước "Đã giao"';
    }
    if (stepId === "delivered") return 'Xác nhận hoàn tất "Đã giao"';
  }
  return "Chuyển trạng thái";
};

// Admin duyệt thanh toán qua mã QR cho đơn hàng
const confirmingPayment = ref(false);
const adminConfirmQrPayment = async (order) => {
  if (!order) return;
  confirmingPayment.value = true;
  try {
    await ThanhToanService.confirmPayment(order.donHangId, {
      soTien: order.thanhTien ?? order.tongTien,
      phuongThuc: "chuyen_khoan",
      maGiaoDich: `ADMIN_QR_${Date.now()}`,
    });
    showToast("Đã duyệt thanh toán qua mã QR thành công!", "success");
    await refreshOrderDetail();
  } catch (e) {
    showToast("Lỗi khi duyệt thanh toán: " + (e.message || e), "error");
  } finally {
    confirmingPayment.value = false;
  }
};

// Chuyển trạng thái đơn hàng theo từng bước một (step by step cho cả 7 bước QR và 5 bước COD)
const jumpToStatus = async (order, stepId) => {
  if (!order) return;
  if (order.kenhBan !== "in_store" && !canJumpToStep(order, stepId)) return;

  let targetStatus = stepId;

  if (order.kenhBan === "in_store") {
    targetStatus = "delivered";
  } else if (isQrPayment(order)) {
    // Bước 2: Chờ thanh toán -> phải đợi khách thanh toán mới được nhấn các bước dưới
    if (order.trangThaiThanhToan !== "paid") {
      showToast(
        "Vui lòng đợi khách hàng hoàn tất thanh toán QR trước khi xử lý các bước tiếp theo.",
        "warning",
      );
      return;
    }
    // Bước 3: Chờ xử lý (pending) -> bấm để sang bước 4: Đã lên đơn (confirmed)
    if (stepId === "cho_xu_ly" || (order.trangThaiDonHang === "pending" && stepId === "da_len_don")) {
      targetStatus = "confirmed";
    }
    // Bước 4: Đã lên đơn (confirmed) -> bấm để sang bước 5: Đang đóng gói (processing)
    // → Phải chọn serial trước khi chuyển sang bước này
    else if (stepId === "da_len_don" || (order.trangThaiDonHang === "confirmed" && stepId === "dang_dong_goi")) {
      await openXacNhanSerialModal(order);
      return; // Không advance ngay — confirmXacNhanSerial sẽ lo phần advance
    }
    // Bước 5: Đang đóng gói (processing/shipping) -> bấm để sang bước 6: Đang giao hàng (out_for_delivery)
    else if (stepId === "dang_dong_goi" || ((order.trangThaiDonHang === "processing" || order.trangThaiDonHang === "shipping") && stepId === "dang_giao_hang")) {
      targetStatus = "out_for_delivery";
    }
    // Bước 6: Đang giao hàng (out_for_delivery) -> bấm để sang bước 7: Đã giao sáng đèn (awaiting_confirmation)
    else if (stepId === "dang_giao_hang" || (order.trangThaiDonHang === "out_for_delivery" && stepId === "da_giao")) {
      targetStatus = "awaiting_confirmation";
    }
    // Bước 7: Đã giao (awaiting_confirmation) -> bấm để hoàn tất (delivered)
    else if (stepId === "da_giao") {
      targetStatus = "delivered";
    } else {
      return;
    }
  } else {
    // Đơn COD (thanh toán sau) - nhảy đúng từng bước 1:
    // Bước 1: Chờ xác nhận (pending) -> sang bước 2: Đã lên đơn (confirmed)
    if (stepId === "pending" || (order.trangThaiDonHang === "pending" && stepId === "confirmed")) {
      targetStatus = "confirmed";
    }
    // Bước 2: Đã lên đơn (confirmed) -> sang bước 3: Đang đóng gói (processing)
    // → Phải chọn serial trước khi chuyển sang bước này
    else if (stepId === "confirmed" || (order.trangThaiDonHang === "confirmed" && stepId === "processing")) {
      await openXacNhanSerialModal(order);
      return; // Không advance ngay — confirmXacNhanSerial sẽ lo phần advance
    }
    // Bước 3: Đang đóng gói (processing/shipping) -> sang bước 4: Đang giao hàng (out_for_delivery)
    else if (stepId === "processing" || ((order.trangThaiDonHang === "processing" || order.trangThaiDonHang === "shipping") && stepId === "out_for_delivery")) {
      targetStatus = "out_for_delivery";
    }
    // Bước 4: Đang giao hàng (out_for_delivery) -> sang bước 5: Đã giao sáng đèn (awaiting_confirmation)
    else if (stepId === "out_for_delivery" || (order.trangThaiDonHang === "out_for_delivery" && stepId === "delivered")) {
      targetStatus = "awaiting_confirmation";
    }
    // Bước 5: Đã giao (awaiting_confirmation) -> hoàn tất toàn bộ (delivered)
    else if (stepId === "delivered") {
      targetStatus = "delivered";
    } else {
      return;
    }
  }

  if (order.trangThaiDonHang === targetStatus) return;

  // Cập nhật thanh toán và ngày giao hàng thực tế khi hoàn tất bước cuối (delivered)
  const body = buildOrderUpdateBody(order, {
    trangThaiDonHang: targetStatus,
    trangThaiThanhToan:
      targetStatus === "delivered" && order.trangThaiThanhToan === "unpaid"
        ? "paid"
        : order.trangThaiThanhToan,
    ngayGiaoDuKien: order.ngayGiaoDuKien,
    ngayGiaoThucTe:
      targetStatus === "delivered" && !order.ngayGiaoThucTe ? nowLocalIso() : order.ngayGiaoThucTe,
    maVanDon: order.maVanDon,
  });
  const res = await DonHangService.update(order.donHangId, body);
  if (!res.ok) {
    showToast(await res.text().catch(() => t("admin.errors.updateFailed", { status: res.status })), "error");
    return;
  }
  await refreshOrderDetail();
};
const advanceOrderStatus = async (o) => {
  const next = NEXT_ORDER_STATUS[o.trangThaiDonHang];
  if (!next) return;
  const body = buildOrderUpdateBody(o, {
    trangThaiDonHang: next,
    trangThaiThanhToan:
      next === "delivered" && o.trangThaiThanhToan === "unpaid" ? "paid" : o.trangThaiThanhToan,
    ngayGiaoDuKien: o.ngayGiaoDuKien,
    ngayGiaoThucTe: next === "delivered" && !o.ngayGiaoThucTe ? nowLocalIso() : o.ngayGiaoThucTe,
    maVanDon: o.maVanDon,
  });
  const res = await DonHangService.update(o.donHangId, body);
  if (!res.ok) {
    showToast(await res.text().catch(() => t("admin.errors.updateFailed", { status: res.status })), "error");
    return;
  }
  await refreshOrderDetail();
};

// Modal chọn serial xác nhận đơn online
const showXacNhanSerialModal = ref(false);
const xacNhanOrder = ref(null);
const xacNhanLines = ref([]); // [{ ...ChiTietDonHangResponse, chosenSerialIds: Set<number>, reservedSerialIds: Set<number> }]
const xacNhanSerialMap = ref({}); // bienTheId -> ChiTietSanPhamResponse[]
const xacNhanLoading = ref(false);
const xacNhanError = ref("");

const openXacNhanSerialModal = async (o) => {
  xacNhanOrder.value = o;
  xacNhanLines.value = [];
  xacNhanError.value = "";
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
    xacNhanLines.value = items.map((item) => {
      const lineReservedIds = reservedByLine[item.id] ?? [];
      const allForVariant = xacNhanSerialMap.value[item.bienTheId] ?? [];
      // Chỉ coi là đã được admin/nhân viên chọn nếu serial đó có trạng thái 'da_ban'
      const confirmedIds = lineReservedIds.filter((id) => {
        const sObj = allForVariant.find((s) => s.chiTietId === id);
        return sObj && sObj.trangThai === "da_ban";
      });
      return {
        ...item,
        chosenSerialIds: new Set(confirmedIds),
        reservedSerialIds: new Set(lineReservedIds),
      };
    });
  } catch (e) {
    xacNhanError.value = e.message;
  } finally {
    xacNhanLoading.value = false;
  }
};

// Lọc danh sách serial khả dụng — chỉ hiển thị serial còn trong kho
const xacNhanAvailableSerials = (line) => {
  const all = xacNhanSerialMap.value[line.bienTheId] ?? [];
  return all.filter(
    (s) =>
      s.trangThai === "trong_kho" ||
      line.chosenSerialIds.has(s.chiTietId), // giữ serial đã chọn để vẫn hiện
  );
};

const xacNhanToggleSerial = (line, serialId) => {
  if (line.chosenSerialIds.has(serialId)) {
    line.chosenSerialIds.delete(serialId);
    return;
  }
  // Chọn serial cho chi tiết đơn hàng
  if (line.soLuong === 1) {
    line.chosenSerialIds.clear();
    line.chosenSerialIds.add(serialId);
    return;
  }
  if (line.chosenSerialIds.size < line.soLuong) line.chosenSerialIds.add(serialId);
};

const xacNhanAllLinesComplete = computed(
  () =>
    xacNhanLines.value.length > 0 &&
    xacNhanLines.value.every((l) => l.chosenSerialIds.size === l.soLuong),
);

const confirmXacNhanSerial = async () => {
  if (!xacNhanAllLinesComplete.value) return;
  xacNhanError.value = "";
  xacNhanLoading.value = true;
  try {
    // Bước 1: Lưu serial đã chọn
    const res = await DonHangService.xacNhan(xacNhanOrder.value.donHangId, {
      nhanVienId: AuthStore.user?.id || AuthStore.user?.nhanVienId,
      lines: xacNhanLines.value.map((l) => ({
        chiTietDonHangId: l.id,
        serialIds: [...l.chosenSerialIds],
      })),
    });
    if (!res.ok) {
      xacNhanError.value = await res
        .text()
        .catch(() => t("admin.errors.updateFailed", { status: res.status }));
      return;
    }

    // Bước 2: Advance đơn hàng sang trạng thái Đang đóng gói (processing)
    const order = xacNhanOrder.value;
    const updateBody = buildOrderUpdateBody(order, {
      trangThaiDonHang: "processing",
      trangThaiThanhToan: order.trangThaiThanhToan,
      ngayGiaoDuKien: order.ngayGiaoDuKien,
      ngayGiaoThucTe: order.ngayGiaoThucTe,
      maVanDon: order.maVanDon,
    });
    const updateRes = await DonHangService.update(order.donHangId, updateBody);
    if (!updateRes.ok) {
      xacNhanError.value = await updateRes
        .text()
        .catch(() => t("admin.errors.updateFailed", { status: updateRes.status }));
      return;
    }

    showXacNhanSerialModal.value = false;
    showToast("Đã xác nhận serial và chuyển đơn sang Đang đóng gói!", "success");
    await refreshOrderDetail();
  } catch (e) {
    xacNhanError.value = e.message;
  } finally {
    xacNhanLoading.value = false;
  }
};
</script>

<template>
  <!-- Chế độ: danh sách các ngày có đơn hàng (Lịch sử đơn hàng) -->
  <template v-if="orderViewMode === 'history-dates'">
    <div class="alt-card">
      <div class="alt-toolbar">
        <span class="fw-bold" style="color: var(--text-heading)">{{
          t("admin.orders.history")
        }}</span>
        <div class="alt-toolbar__actions">
          <button class="alt-btn alt-btn--ghost" @click="backToToday">
            {{ t("admin.orders.backToToday") }}
          </button>
        </div>
      </div>
      <div v-if="OrdersStore.loading" class="alt-empty">{{ t("admin.orders.loading") }}</div>
      <div v-else class="d-flex flex-column" style="padding: 8px">
        <div
          v-for="d in orderDatesGrouped"
          :key="d.dateKey"
          class="d-flex justify-content-between align-items-center px-3 py-3 rounded-3"
          style="cursor: pointer"
          @click="openHistoryDay(d.dateKey)"
        >
          <span class="fw-semibold" style="color: var(--text-primary)">{{ d.label }}</span>
          <span class="text-secondary small d-flex align-items-center gap-2"
            >{{ d.count }} {{ t("admin.orders.countSuffix") }}
            <span style="font-size: 1.1rem">›</span></span
          >
        </div>
        <div v-if="orderDatesGrouped.length === 0" class="alt-empty">
          {{ t("admin.orders.empty") }}
        </div>
      </div>
    </div>
  </template>

  <!-- Chế độ: đơn hôm nay (mặc định) hoặc đơn của 1 ngày lịch sử đã chọn -->
  <template v-else>
    <div class="alt-card">
      <div class="alt-toolbar">
        <div class="alt-toolbar__left">
          <button
            v-if="orderViewMode === 'history-day'"
            class="alt-btn alt-btn--ghost"
            @click="backToDateList"
          >
            {{ t("admin.orders.backToDateList") }}
          </button>
          <span class="alt-toolbar__count">
            <span
              v-if="orderViewMode === 'history-day'"
              class="fw-semibold"
              style="color: var(--text-primary)"
              >{{ formatDateHeading(historySelectedDate) }} ·
            </span>
            {{ filteredOrders.length }}/{{ ordersBaseList.length }}
            {{ t("admin.orders.countSuffix") }}
          </span>
        </div>
        <div class="alt-toolbar__actions">
          <div class="alt-search">
            <Search class="alt-search__icon" :size="14" />
            <input v-model="orderSearch" :placeholder="t('admin.orders.searchPlaceholder')" />
          </div>
          <button
            class="alt-btn alt-btn--filter"
            :class="{ 'alt-btn--filter-active': activeOrderFilterCount > 0 || isOrderFilterOpen }"
            @click="isOrderFilterOpen = !isOrderFilterOpen"
          >
            <Filter :size="14" /> Bộ lọc
            <span v-if="activeOrderFilterCount > 0" class="filter-badge">{{
              activeOrderFilterCount
            }}</span>
            <ChevronDown v-if="!isOrderFilterOpen" :size="13" />
            <ChevronUp v-else :size="13" />
          </button>
          <button
            v-if="activeOrderFilterCount > 0"
            class="alt-btn alt-btn--ghost-sm"
            @click="resetOrderFilters"
          >
            <X :size="13" /> Xóa lọc
          </button>
        </div>
      </div>

      <!-- Panel lọc nâng cao -->
      <div v-if="isOrderFilterOpen" class="adv-filter-panel">
        <div class="adv-filter-row">
          <div class="adv-filter-group">
            <label class="adv-filter-label">Trạng thái đơn</label>
            <select v-model="orderFilters.trangThaiDonHang" class="adv-filter-select">
              <option value="">Tất cả</option>
              <option value="pending">{{ orderStatusLabel("pending") }}</option>
              <option value="confirmed">{{ orderStatusLabel("confirmed") }}</option>
              <option value="processing">{{ orderStatusLabel("processing") }}</option>
              <option value="out_for_delivery">{{ orderStatusLabel("out_for_delivery") }}</option>
              <option value="awaiting_confirmation">
                {{ orderStatusLabel("awaiting_confirmation") }}
              </option>
              <option value="delivered">{{ orderStatusLabel("delivered") }}</option>
              <option value="cancelled">{{ orderStatusLabel("cancelled") }}</option>
              <option value="returned">{{ orderStatusLabel("returned") }}</option>
            </select>
          </div>
          <div class="adv-filter-group">
            <label class="adv-filter-label">Thanh toán</label>
            <select v-model="orderFilters.trangThaiThanhToan" class="adv-filter-select">
              <option value="">Tất cả</option>
              <option value="paid">{{ t("admin.orders.paid") }}</option>
              <option value="unpaid">{{ t("admin.orders.unpaid") }}</option>
            </select>
          </div>
          <div class="adv-filter-group">
            <label class="adv-filter-label">Kênh bán</label>
            <select v-model="orderFilters.kenhBan" class="adv-filter-select">
              <option value="">Tất cả</option>
              <option value="in_store">{{ channelLabel("in_store") }}</option>
              <option value="online">{{ channelLabel("online") }}</option>
            </select>
          </div>
          <div class="adv-filter-group adv-filter-group--range">
            <label class="adv-filter-label">Ngày đặt</label>
            <div class="adv-filter-range">
              <input v-model="orderFilters.ngayFrom" type="date" class="adv-filter-input" />
              <span class="adv-filter-sep">–</span>
              <input v-model="orderFilters.ngayTo" type="date" class="adv-filter-input" />
            </div>
          </div>
          <div class="adv-filter-group adv-filter-group--range">
            <label class="adv-filter-label">Tổng tiền (₫)</label>
            <div class="adv-filter-range">
              <input
                v-model="orderFilters.tongTienMin"
                type="number"
                min="0"
                placeholder="Từ"
                class="adv-filter-input"
              />
              <span class="adv-filter-sep">–</span>
              <input
                v-model="orderFilters.tongTienMax"
                type="number"
                min="0"
                placeholder="Đến"
                class="adv-filter-input"
              />
            </div>
          </div>
          <button
            v-if="activeOrderFilterCount > 0"
            class="adv-filter-reset"
            @click="resetOrderFilters"
          >
            <X :size="13" /> Xóa bộ lọc
          </button>
        </div>
      </div>

      <!-- Nút xem lịch sử đơn hàng -->
      <div v-if="orderViewMode === 'today'" class="alt-history-row">
        <button class="alt-btn alt-btn--ghost" @click="openOrderHistory">
          {{ t("admin.orders.history") }}
        </button>
      </div>
      <div v-if="OrdersStore.loading" class="alt-empty">{{ t("admin.orders.loading") }}</div>
      <div v-else class="alt-table-wrap">
        <table class="alt-table">
          <thead>
            <tr>
              <th style="width: 40px">{{ t("admin.common.stt") }}</th>
              <th>{{ t("admin.orders.colOrderCode") }}</th>
              <th>{{ t("admin.orders.colCustomer") }}</th>
              <th>{{ t("admin.orders.colTotal") }}</th>
              <th>{{ t("admin.orders.colOrderStatus") }}</th>
              <th>{{ t("admin.orders.colPaymentStatus") }}</th>
              <th>{{ t("admin.orders.colOrderDate") }}</th>
              <th style="width: 80px">{{ t("admin.orders.colChannel") }}</th>
              <th>{{ t("admin.orders.colAction") }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(o, idx) in pagedOrders" :key="o.donHangId">
              <td class="text-secondary">{{ currentPage * pageSize + idx + 1 }}</td>
              <td class="text-secondary">{{ o.maDonHang || "#" + o.donHangId }}</td>
              <td>{{ customerName(o.khachHangId) }}</td>
              <td>{{ formatPrice(o.thanhTien) }}</td>
              <td>
                <span
                  v-if="isQrPayment(o)"
                  class="alt-tag"
                  :style="{
                    background: getQrEffectiveStatus(o).color.bg,
                    color: getQrEffectiveStatus(o).color.text,
                    border: o.trangThaiThanhToan === 'unpaid' ? '1px solid #fed7aa' : 'none',
                    fontWeight: '600',
                  }"
                >
                  <component :is="orderStatusIcon(o.trangThaiDonHang)" :size="13" />
                  {{ getQrEffectiveStatus(o).label }}
                </span>
                <span
                  v-else
                  class="alt-tag"
                  :style="{
                    background: getCodEffectiveStatus(o).color.bg,
                    color: getCodEffectiveStatus(o).color.text,
                    fontWeight: '600',
                  }"
                >
                  <component :is="orderStatusIcon(o.trangThaiDonHang)" :size="13" />
                  {{ getCodEffectiveStatus(o).label }}
                </span>
              </td>
              <td>
                <span
                  v-if="o.trangThaiThanhToan"
                  class="alt-tag"
                  :style="{
                    background: paymentStatusColor(o.trangThaiThanhToan).bg,
                    color: paymentStatusColor(o.trangThaiThanhToan).text,
                  }"
                >
                  <component :is="paymentStatusIcon(o.trangThaiThanhToan)" :size="13" />
                  {{ paymentStatusLabel(o.trangThaiThanhToan) }}
                </span>
                <span v-else class="text-secondary">—</span>
              </td>
              <td>
                {{ formatDate(o.ngayDat) }}
                <div v-if="o.ngayGiaoThucTe" class="text-success" style="font-size: 0.72rem">
                  <CheckCircle2 :size="13" style="vertical-align: -2px" />
                  {{ t("admin.orderStatusModal.actualDeliveryLabel") }}:
                  {{ formatDateTime(o.ngayGiaoThucTe) }}
                </div>
              </td>
              <td>
                <span
                  v-if="o.kenhBan"
                  class="alt-tag"
                  :style="{
                    background: channelColor(o.kenhBan).bg,
                    color: channelColor(o.kenhBan).text,
                  }"
                >
                  {{ channelLabel(o.kenhBan) }}
                </span>
                <span v-else class="text-secondary">—</span>
              </td>
              <td>
                <div class="d-flex align-items-center gap-1.5">
                  <button
                    class="alt-btn alt-btn--ghost"
                    style="padding: 4px 12px"
                    @click="openOrderDetail(o)"
                  >
                    {{ t("admin.orders.detail") }}
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="filteredOrders.length === 0">
              <td colspan="9" class="alt-empty">{{ t("admin.orders.empty") }}</td>
            </tr>
          </tbody>
        </table>
        <div v-if="totalPages > 1" class="alt-pager">
          <Pagination
            :current-page="currentPage"
            :total-pages="totalPages"
            @page-change="currentPage = $event"
          />
        </div>
      </div>
    </div>
  </template>

  <!-- ══ MODAL THEM SAN PHAM CHI TIET ══ -->
  <div
    v-if="showAddItemDetailModal"
    class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
    style="background: var(--bg-overlay); z-index: 1070"
    @click.self="showAddItemDetailModal = false"
  >
    <div
      class="rounded-4 d-flex flex-column"
      style="
        background: var(--bg-card-inset);
        border: 1px solid var(--border-color-strong);
        width: 960px;
        max-width: 97vw;
        max-height: 93vh;
      "
    >
      <!-- Header -->
      <div
        class="d-flex justify-content-between align-items-center px-4 py-3"
        style="border-bottom: 1px solid var(--bg-input)"
      >
        <span class="text-secondary" style="font-size: 0.8rem">{{
          t("admin.addItemDetailModal.chooseProduct")
        }}</span>
        <button
          class="btn-close btn-sm"
          :aria-label="t('common.close')"
          @click="showAddItemDetailModal = false"
        ></button>
      </div>

      <!-- Body -->
      <div v-if="addItemDetailGroup" class="overflow-y-auto flex-grow-1 p-0">
        <div class="d-flex" style="min-height: 400px">
          <!-- Left: Anh san pham -->
          <div
            class="d-flex flex-column align-items-center justify-content-center p-4"
            style="
              width: 42%;
              background: var(--bg-page-alt);
              border-right: 1px solid var(--bg-input);
              flex-shrink: 0;
            "
          >
            <div
              style="
                width: 100%;
                max-width: 320px;
                aspect-ratio: 4/3;
                display: flex;
                align-items: center;
                justify-content: center;
                background: var(--bg-card-alt);
                border-radius: 12px;
                overflow: hidden;
                padding: 16px;
              "
            >
              <img
                v-if="(addItemCurrentVariant || addItemDetailGroup.variants[0])?.hinhAnhChinh"
                :src="(addItemCurrentVariant || addItemDetailGroup.variants[0]).hinhAnhChinh"
                style="max-width: 100%; max-height: 100%; object-fit: contain"
              />
              <span v-else><Laptop :size="64" color="var(--text-muted)" /></span>
            </div>
            <div class="mt-3 d-flex gap-1 flex-wrap justify-content-center">
              <span
                v-for="tag in (addItemDetailGroup.variants[0]?.phanLoaiTen || '')
                  .split(',')
                  .filter(Boolean)"
                :key="tag"
                class="badge"
                style="
                  background: rgba(244, 63, 94, 0.12);
                  color: var(--accent-fg);
                  font-size: 0.7rem;
                "
                >{{ tag.trim() }}</span
              >
            </div>
          </div>

          <!-- Right: Thong tin + chon bien the -->
          <div class="d-flex flex-column p-4 overflow-y-auto flex-grow-1">
            <!-- Ten + thuong hieu -->
            <div class="text-secondary mb-1" style="font-size: 0.78rem">
              {{ addItemDetailGroup.variants[0]?.tenThuongHieu }} ·
              {{ addItemDetailGroup.variants[0]?.tenDanhMuc }}
            </div>
            <h5 class="fw-bold text-light mb-2">{{ addItemDetailGroup.tenSanPham }}</h5>
            <div class="mb-3" style="font-size: 1.4rem; font-weight: 700; color: var(--accent-fg)">
              {{
                addItemCurrentVariant
                  ? formatPrice(addItemCurrentVariant.giaBan)
                  : formatPrice(addItemDetailGroup.minPrice)
              }}
              <span class="text-secondary ms-2" style="font-size: 0.8rem; font-weight: 400">{{
                t("admin.addItemDetailModal.freeshipNote")
              }}</span>
            </div>

            <!-- Chon phien ban (CPU + RAM + Storage) -->
            <div v-if="addItemConfigs.length > 1" class="mb-3">
              <div
                class="text-secondary mb-2"
                style="font-size: 0.72rem; font-weight: 700; letter-spacing: 0.05em"
              >
                {{ t("admin.addItemDetailModal.configCount", { count: addItemConfigs.length }) }}
              </div>
              <div class="d-flex flex-wrap gap-2">
                <button
                  v-for="cfg in addItemConfigs"
                  :key="cfg.key"
                  class="btn btn-sm text-start"
                  style="padding: 8px 12px; border-radius: 8px; min-width: 140px"
                  :style="
                    addItemSelectedConfig === cfg.key
                      ? 'background:rgba(244,63,94,0.12);border:2px solid var(--accent);color:var(--accent-fg);'
                      : 'background:var(--bg-card);border:1px solid var(--border-color-strong);color:var(--text-secondary);'
                  "
                  @click="selectConfig(cfg.key)"
                >
                  <div style="font-size: 0.78rem; font-weight: 600">
                    {{ cfg.cpu || t("admin.addItemDetailModal.standard") }}
                  </div>
                  <div style="font-size: 0.68rem">
                    {{ [cfg.ram, cfg.oCung].filter(Boolean).join(" · ") }}
                  </div>
                </button>
              </div>
            </div>

            <!-- Chon mau sac -->
            <div v-if="addItemColorsForConfig.length > 0" class="mb-3">
              <div
                class="text-secondary mb-2"
                style="font-size: 0.72rem; font-weight: 700; letter-spacing: 0.05em"
              >
                {{ t("admin.addItemDetailModal.color") }}
              </div>
              <div class="d-flex flex-wrap gap-2">
                <button
                  v-for="v in addItemColorsForConfig"
                  :key="v.bienTheId"
                  class="btn btn-sm"
                  style="padding: 6px 14px; border-radius: 8px"
                  :style="
                    addItemSelectedColor === v.mauSac
                      ? 'background:rgba(244,63,94,0.12);border:2px solid var(--accent);color:var(--accent-fg);'
                      : 'background:var(--bg-card);border:1px solid var(--border-color-strong);color:var(--text-primary);'
                  "
                  @click="addItemSelectedColor = v.mauSac"
                >
                  <div style="font-size: 0.78rem; font-weight: 600">{{ v.mauSac }}</div>
                  <div style="font-size: 0.7rem; color: var(--accent-fg)">
                    {{ formatPrice(v.giaBan) }}
                  </div>
                </button>
              </div>
            </div>

            <!-- Thong tin chon -->
            <div
              v-if="addItemCurrentVariant"
              class="mb-3 py-2 px-3 rounded-3"
              style="background: var(--bg-card); font-size: 0.8rem"
            >
              <span class="text-secondary">{{ t("admin.addItemDetailModal.colorLabel") }} </span>
              <strong class="text-light">{{ addItemCurrentVariant.mauSac }}</strong>
              <span class="mx-2 text-secondary">·</span>
              <span class="text-secondary">{{ t("admin.addItemDetailModal.warrantyLabel") }} </span>
              <strong class="text-light">{{
                addItemCurrentVariant.baoHanhThang
                  ? addItemCurrentVariant.baoHanhThang + " " + t("admin.addItemDetailModal.months")
                  : "—"
              }}</strong>
              <span class="mx-2 text-secondary">·</span>
              <span class="text-secondary">{{ t("admin.addItemDetailModal.skuLabel") }} </span>
              <span class="text-light" style="font-family: monospace; font-size: 0.75rem">{{
                addItemCurrentVariant.maSku
              }}</span>
            </div>

            <!-- Thong so ky thuat -->
            <div v-if="addItemCurrentVariant" class="mb-3">
              <div
                class="text-secondary mb-2"
                style="font-size: 0.72rem; font-weight: 700; letter-spacing: 0.05em"
              >
                {{ t("admin.addItemDetailModal.specsHeading") }}
              </div>
              <table style="width: 100%; font-size: 0.78rem; border-collapse: collapse">
                <tr
                  v-for="[label, val] in [
                    [t('admin.addItemDetailModal.specCpu'), addItemCurrentVariant.cpu],
                    [t('admin.addItemDetailModal.specRam'), addItemCurrentVariant.ram],
                    [t('admin.addItemDetailModal.specStorage'), addItemCurrentVariant.oCung],
                    [t('admin.addItemDetailModal.specGpu'), addItemCurrentVariant.gpu],
                    [
                      t('admin.addItemDetailModal.specScreen'),
                      addItemCurrentVariant.kichThuocManHinh,
                    ],
                    [t('admin.addItemDetailModal.specOs'), addItemCurrentVariant.heDieuHanh],
                    [t('admin.addItemDetailModal.specBattery'), addItemCurrentVariant.pin],
                    [
                      t('admin.addItemDetailModal.specWeight'),
                      addItemCurrentVariant.trongLuongKg
                        ? addItemCurrentVariant.trongLuongKg + ' kg'
                        : null,
                    ],
                  ].filter(([, v]) => v)"
                  :key="label"
                  style="border-top: 1px solid var(--bg-input)"
                >
                  <td class="py-1 text-secondary" style="padding-left: 0; width: 44%">
                    {{ label }}
                  </td>
                  <td class="py-1 text-light fw-semibold">{{ val }}</td>
                </tr>
              </table>
            </div>
          </div>
        </div>
      </div>

      <!-- Footer: so luong + them -->
      <div
        class="px-4 py-3 d-flex align-items-center gap-3"
        style="border-top: 1px solid var(--bg-input); background: var(--bg-page-alt)"
      >
        <span class="text-secondary" style="font-size: 0.85rem">{{
          t("admin.addItemDetailModal.qtyLabel")
        }}</span>
        <input
          v-model.number="addItemQty"
          type="number"
          min="1"
          max="99"
          class="form-control form-control-sm"
          style="
            width: 80px;
            background: var(--bg-input);
            color: var(--text-primary);
            border-color: var(--border-color-strong);
          "
        />
        <button
          class="btn btn-warning flex-grow-1 fw-bold"
          style="font-size: 0.9rem"
          :disabled="!addItemCurrentVariant || addItemQty < 1 || addItemLoading"
          @click="confirmAddFromDetail"
        >
          {{
            addItemLoading
              ? t("admin.addItemDetailModal.adding")
              : t("admin.addItemDetailModal.addToOrder")
          }}
        </button>
      </div>
    </div>
  </div>

  <!-- ══ MODAL CHI TIET DON HANG ══ -->
  <div
    v-if="showOrderDetailModal"
    class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
    style="background: var(--bg-overlay); z-index: 1050"
    @click.self="showOrderDetailModal = false"
  >
    <div
      class="alt-card d-flex flex-column"
      style="width: 840px; max-width: 96vw; max-height: 92vh; border-radius: 14px"
    >
      <!-- Header gọn: chỉ tên khách + mã đơn + nút đóng. Tất cả action nằm bên sidebar phải. -->
      <div class="alt-toolbar">
        <div>
          <div
            class="fw-bold d-flex align-items-center gap-2"
            style="font-size: 1.05rem; color: var(--text-heading)"
          >
            <User :size="15" style="vertical-align: -2px" />
            <span>{{ customerName(orderDetailData?.khachHangId) }}</span>
          </div>
          <div
            class="d-flex align-items-center gap-2 mt-0.5"
            style="font-size: 0.78rem; flex-wrap: wrap; color: var(--text-muted)"
          >
            <span
              >{{ t("admin.orderDetailModal.titlePrefix") }}{{ orderDetailData?.donHangId }}</span
            >
            <span v-if="orderDetailData?.maDonHang" style="font-family: monospace">{{
              orderDetailData.maDonHang
            }}</span>
            <span
              v-if="orderDetailData?.kenhBan"
              class="alt-tag"
              style="font-size: 0.7rem"
              :style="{
                background: channelColor(orderDetailData.kenhBan).bg,
                color: channelColor(orderDetailData.kenhBan).text,
              }"
            >
              {{ channelLabel(orderDetailData.kenhBan) }}
            </span>
            <span>· {{ formatDate(orderDetailData?.ngayDat) }}</span>
          </div>
        </div>
        <div class="alt-toolbar__actions">
          <button
            class="alt-btn alt-btn--primary d-flex align-items-center gap-1"
            @click="
              invoiceOrder = orderDetailData;
              showInvoice = true;
            "
          >
            <Printer :size="13" /> In hóa đơn
          </button>
          <button
            class="btn-close btn-sm"
            :aria-label="t('common.close')"
            @click="showOrderDetailModal = false"
          ></button>
        </div>
      </div>

      <!-- Body 2 cột: trái = sản phẩm + tổng tiền, phải = sidebar trạng thái -->
      <div class="d-flex flex-grow-1 overflow-hidden">
        <!-- Cột trái: scroll độc lập -->
        <div
          class="overflow-y-auto flex-grow-1"
          style="border-right: 1px solid var(--border-color-soft)"
        >
          <!-- Danh sach san pham trong don -->
          <div class="p-3">
            <div
              class="text-secondary fw-bold mb-2 text-uppercase"
              style="font-size: 0.72rem; letter-spacing: 0.05em"
            >
              Sản phẩm trong đơn ({{ orderDetailItems.length }})
            </div>
            <div v-if="orderDetailLoading" class="text-secondary small text-center py-4">
              {{ t("admin.orderDetailModal.loading") }}
            </div>
            <div
              v-else-if="orderDetailItems.length === 0"
              class="text-secondary small text-center py-4"
            >
              {{ t("admin.orderDetailModal.empty") }}
            </div>
            <div v-else class="d-flex flex-column gap-2">
              <div
                v-for="item in orderDetailItems"
                :key="item.id"
                class="d-flex align-items-start gap-3 p-2.5 rounded-3"
                style="background: var(--bg-input); border: 1px solid var(--border-color-soft)"
              >
                <!-- Ảnh sản phẩm -->
                <div
                  style="
                    width: 56px;
                    height: 50px;
                    flex-shrink: 0;
                    background: var(--bg-card);
                    border-radius: 8px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    overflow: hidden;
                    border: 1px solid var(--border-color-soft);
                  "
                >
                  <img
                    v-if="productByBienThe(item.bienTheId)?.hinhAnhChinh"
                    :src="productByBienThe(item.bienTheId).hinhAnhChinh"
                    style="max-width: 50px; max-height: 44px; object-fit: contain"
                  />
                  <Laptop v-else :size="22" style="color: var(--text-muted)" />
                </div>

                <!-- Thông tin SP -->
                <div class="flex-grow-1 min-w-0">
                  <div
                    class="fw-semibold text-truncate"
                    style="font-size: 0.88rem; color: var(--text-heading)"
                  >
                    {{
                      productByBienThe(item.bienTheId)?.tenSanPham || item.tenSanPham || "Sản phẩm"
                    }}
                  </div>
                  <!-- Phân loại cấu hình -->
                  <div
                    v-if="
                      [
                        productByBienThe(item.bienTheId)?.cpu,
                        productByBienThe(item.bienTheId)?.ram,
                        productByBienThe(item.bienTheId)?.oCung,
                        productByBienThe(item.bienTheId)?.mauSac,
                      ].filter(Boolean).length
                    "
                    class="mt-1"
                    style="font-size: 0.74rem; color: var(--text-secondary)"
                  >
                    {{
                      [
                        productByBienThe(item.bienTheId)?.cpu,
                        productByBienThe(item.bienTheId)?.ram,
                        productByBienThe(item.bienTheId)?.oCung,
                        productByBienThe(item.bienTheId)?.mauSac,
                      ]
                        .filter(Boolean)
                        .join(" · ")
                    }}
                  </div>
                  <div
                    class="d-flex align-items-center gap-2 mt-1"
                    style="font-size: 0.72rem; color: var(--text-muted); flex-wrap: wrap"
                  >
                    <span v-if="item.maSku"
                      >SKU: <code style="color: var(--text-secondary)">{{ item.maSku }}</code></span
                    >
                    <span
                      v-if="
                        orderDetailData?.trangThaiDonHang !== 'pending' &&
                        item.soSerial
                      "
                      >· Serial:
                      <strong style="color: var(--accent-fg); font-family: monospace">{{
                        item.soSerial
                      }}</strong></span
                    >
                  </div>
                </div>

                <!-- Giá + Số lượng -->
                <div
                  class="text-end flex-shrink-0 d-flex flex-column align-items-end"
                  style="min-width: 110px"
                >
                  <div class="fw-bold" style="font-size: 0.9rem; color: var(--accent-fg)">
                    {{ formatPrice(item.thanhTien ?? item.donGia * (item.soLuong || 1)) }}
                  </div>
                  <div class="text-secondary" style="font-size: 0.74rem">
                    {{ formatPrice(item.donGia) }} × {{ item.soLuong || 1 }}
                  </div>
                  <div class="d-flex gap-1 mt-1">
                    <button
                      v-if="productByBienThe(item.bienTheId)"
                      class="btn btn-sm btn-outline-secondary py-0 px-2"
                      style="font-size: 0.68rem; border-radius: 4px"
                      @click="openVariantDetail(item.bienTheId)"
                    >
                      {{ t("admin.orderDetailModal.detail") }}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Footer: tong ket -->
          <div
            v-if="orderDetailData"
            class="px-4 py-3 d-flex flex-column gap-1"
            style="border-top: 1px solid var(--border-color-soft); background: var(--bg-card-alt)"
          >
            <div class="d-flex justify-content-between small text-secondary">
              <span>{{ t("admin.orderDetailModal.subtotal") }}</span
              ><span>{{ formatPrice(orderDetailData.tongTien) }}</span>
            </div>
            <div
              v-if="orderDetailData.giamGia > 0"
              class="d-flex justify-content-between small text-success"
            >
              <span>{{ t("admin.orderDetailModal.discount") }}</span
              ><span>− {{ formatPrice(orderDetailData.giamGia) }}</span>
            </div>
            <div
              v-if="orderDetailData.kenhBan !== 'in_store'"
              class="d-flex justify-content-between small text-secondary"
            >
              <span>{{ t("admin.orderDetailModal.shippingFee") }}</span>
              <span :class="orderDetailData.phiVanChuyen === 0 ? 'text-success' : ''">
                {{
                  orderDetailData.phiVanChuyen === 0
                    ? t("admin.orderDetailModal.free")
                    : formatPrice(orderDetailData.phiVanChuyen)
                }}
              </span>
            </div>
            <div
              class="d-flex justify-content-between fw-bold pt-2 mt-1"
              style="border-top: 1px solid var(--border-color)"
            >
              <span style="color: var(--text-heading)">{{
                t("admin.orderDetailModal.total")
              }}</span>
              <span class="text-warning" style="font-size: 1rem">{{
                formatPrice(orderDetailData.thanhTien)
              }}</span>
            </div>
          </div>
        </div>

        <!-- Sidebar chi tiết trạng thái đơn hàng -->
        <div
          v-if="orderDetailData"
          class="d-flex flex-column"
          style="width: 280px; min-width: 280px; flex-shrink: 0; background: var(--bg-card-alt)"
        >
          <div class="overflow-y-auto p-3 d-flex flex-column gap-3">
            <!-- Nhóm trạng thái: badge trạng thái hiện tại -->
            <div>
              <div
                class="text-secondary fw-bold mb-2"
                style="font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.06em"
              >
                {{ t("admin.orderDetailModal.orderStatus") }}
              </div>
              <span
                v-if="isQrPayment(orderDetailData)"
                class="alt-tag d-inline-flex align-items-center gap-1"
                :style="{
                  background: getQrEffectiveStatus(orderDetailData).color.bg,
                  color: getQrEffectiveStatus(orderDetailData).color.text,
                  border:
                    orderDetailData.trangThaiThanhToan === 'unpaid' ? '1px solid #fed7aa' : 'none',
                  fontWeight: '600',
                }"
              >
                <component :is="orderStatusIcon(orderDetailData.trangThaiDonHang)" :size="13" />
                {{ getQrEffectiveStatus(orderDetailData).label }}
              </span>
              <span
                v-else
                class="alt-tag d-inline-flex align-items-center gap-1"
                :style="{
                  background: getCodEffectiveStatus(orderDetailData).color.bg,
                  color: getCodEffectiveStatus(orderDetailData).color.text,
                  fontWeight: '600',
                }"
              >
                <component :is="orderStatusIcon(orderDetailData.trangThaiDonHang)" :size="13" />
                {{ getCodEffectiveStatus(orderDetailData).label }}
              </span>
            </div>

            <!-- Timeline các bước xử lý đơn hàng (5 bước COD / 7 bước QR) -->
            <div>
              <div class="d-flex align-items-center justify-content-between mb-2">
                <div
                  class="text-secondary fw-bold text-uppercase"
                  style="font-size: 0.7rem; letter-spacing: 0.06em"
                >
                  {{
                    isQrPayment(orderDetailData)
                      ? "TIẾN TRÌNH THANH TOÁN QR"
                      : "TIẾN TRÌNH THANH TOÁN SAU"
                  }}
                </div>
              </div>
              <div class="d-flex flex-column gap-0" style="position: relative">
                <div
                  v-for="(step, index) in orderTimelineSteps"
                  :key="step.id"
                  class="d-flex align-items-start gap-3 position-relative"
                  :style="canJumpToStep(orderDetailData, step.id) ? 'cursor:pointer;' : ''"
                  @click="
                    canJumpToStep(orderDetailData, step.id) &&
                    !confirmingPayment &&
                    jumpToStatus(orderDetailData, step.id)
                  "
                >
                  <!-- Đường kẻ dọc liền mạch tuyệt đối giữa các bước -->
                  <div
                    v-if="index < orderTimelineSteps.length - 1"
                    style="
                      position: absolute;
                      left: 15px;
                      top: 30px;
                      bottom: -2px;
                      width: 2px;
                      z-index: 0;
                    "
                    :style="
                      isStepDoneById(orderDetailData, orderTimelineSteps[index + 1].id) ||
                      isStepNextById(orderDetailData, orderTimelineSteps[index + 1].id)
                        ? 'background:var(--accent);'
                        : 'background:var(--border-color-strong); opacity:0.35;'
                    "
                  ></div>

                  <div
                    class="d-flex flex-column align-items-center"
                    style="width: 32px; flex-shrink: 0; position: relative; z-index: 1"
                  >
                    <button
                      type="button"
                      class="rounded-circle d-flex align-items-center justify-content-center position-relative p-0"
                      style="width: 32px; height: 32px; border: none; z-index: 1"
                      :disabled="!canJumpToStep(orderDetailData, step.id) || confirmingPayment"
                      :title="getStepActionTitle(orderDetailData, step.id)"
                      :style="
                        isStepDoneById(orderDetailData, step.id)
                          ? canJumpToStep(orderDetailData, step.id)
                            ? 'background:var(--accent); border:2px solid var(--accent); cursor:pointer;'
                            : 'background:var(--accent); border:2px solid var(--accent); cursor:default;'
                          : isStepNextById(orderDetailData, step.id)
                            ? canJumpToStep(orderDetailData, step.id)
                              ? 'background:var(--bg-hover); border:2.5px solid var(--accent); box-shadow:0 0 0 4px rgba(244,63,94,0.18); cursor:pointer;'
                              : 'background:rgba(251,146,60,0.12); border:2.5px solid #fb923c; box-shadow:0 0 0 4px rgba(251,146,60,0.18); cursor:not-allowed;'
                            : isStepReachedById(orderDetailData, step.id) &&
                                !isStepDoneById(orderDetailData, step.id) &&
                                !isStepNextById(orderDetailData, step.id)
                              ? 'background:rgba(251,146,60,0.12); border:2px solid #fb923c; cursor:not-allowed;'
                              : canJumpToStep(orderDetailData, step.id)
                                ? 'background:var(--bg-card-alt); border:2px solid var(--border-color-strong); cursor:pointer;'
                                : 'background:var(--bg-card-alt); border:2px solid var(--border-color-strong); cursor:not-allowed; opacity:0.4;'
                      "
                      @click.stop="
                        canJumpToStep(orderDetailData, step.id) &&
                        !confirmingPayment &&
                        jumpToStatus(orderDetailData, step.id)
                      "
                    >
                      <Check
                        v-if="isStepDoneById(orderDetailData, step.id)"
                        :size="14"
                        color="white"
                      />
                      <span
                        v-else-if="
                          confirmingPayment &&
                          (step.id === 'cho_thanh_toan' || step.id === 'cho_xu_ly')
                        "
                        class="spinner-border spinner-border-sm text-danger"
                        style="width: 14px; height: 14px; border-width: 2px"
                      ></span>
                      <component
                        v-else
                        :is="step.icon"
                        :size="14"
                        :style="{
                          opacity:
                            isStepNextById(orderDetailData, step.id) ||
                            (isStepReachedById(orderDetailData, step.id) &&
                              !isStepDoneById(orderDetailData, step.id) &&
                              !isStepNextById(orderDetailData, step.id))
                              ? 1
                              : 0.35,
                          color: isStepNextById(orderDetailData, step.id)
                            ? 'var(--accent-fg)'
                            : isStepReachedById(orderDetailData, step.id) &&
                                !isStepDoneById(orderDetailData, step.id)
                              ? '#fb923c'
                              : 'inherit',
                        }"
                      />
                    </button>
                  </div>
                  <div
                    class="flex-grow-1 pb-3"
                    style="padding-top: 4px; position: relative; z-index: 1"
                  >
                    <div
                      class="fw-semibold"
                      style="font-size: 0.85rem; line-height: 1.3"
                      :style="
                        isStepNextById(orderDetailData, step.id)
                          ? 'color:var(--accent-fg); font-weight:700;'
                          : isStepDoneById(orderDetailData, step.id)
                            ? 'color:var(--text-primary);'
                            : isStepReachedById(orderDetailData, step.id) &&
                                !isStepDoneById(orderDetailData, step.id) &&
                                !isStepNextById(orderDetailData, step.id)
                              ? 'color:#fb923c;'
                              : 'color:var(--text-secondary);'
                      "
                    >
                      {{ step.title }}
                    </div>
                    <div
                      style="
                        font-size: 0.72rem;
                        color: var(--text-muted);
                        line-height: 1.35;
                        margin-top: 2px;
                      "
                    >
                      {{ step.desc }}
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Thanh toán -->
            <div style="border-top: 1px solid var(--border-color-soft); padding-top: 12px">
              <div
                class="text-secondary fw-bold mb-2"
                style="font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.06em"
              >
                {{ t("admin.orderDetailModal.paymentStatus") }}
              </div>
              <div
                class="alt-tag d-inline-flex align-items-center gap-1"
                :style="{
                  background: paymentStatusColor(orderDetailData.trangThaiThanhToan).bg,
                  color: paymentStatusColor(orderDetailData.trangThaiThanhToan).text,
                }"
              >
                <component :is="paymentStatusIcon(orderDetailData.trangThaiThanhToan)" :size="13" />
                {{
                  orderDetailData.trangThaiThanhToan
                    ? paymentStatusLabel(orderDetailData.trangThaiThanhToan)
                    : "—"
                }}
              </div>
              <div v-if="orderDetailPayments.length" class="mt-2 small text-secondary">
                <span class="d-block mb-1">{{ t("admin.orderDetailModal.paymentMethod") }}:</span>
                <span style="color: var(--text-primary)">
                  <template v-for="(g, idx) in orderDetailPaymentsSummary" :key="g.method">
                    <component
                      :is="paymentMethodIcon(g.method)"
                      :size="13"
                      style="vertical-align: -2px"
                    />
                    {{ paymentMethodLabel(g.method)
                    }}<template v-if="g.count > 1">
                      ×{{ g.count }} ({{ formatPrice(g.total) }})</template
                    ><span v-if="idx < orderDetailPaymentsSummary.length - 1">, </span>
                  </template>
                </span>
              </div>
            </div>

            <!-- Ngày giao -->
            <div
              v-if="orderDetailData.ngayGiaoDuKien || orderDetailData.ngayGiaoThucTe"
              style="border-top: 1px solid var(--border-color-soft); padding-top: 12px"
            >
              <div
                class="text-secondary fw-bold mb-2"
                style="font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.06em"
              >
                {{ t("orderStatus.deliveryTitle") }}
              </div>
              <div
                v-if="orderDetailData.ngayGiaoDuKien"
                class="d-flex justify-content-between small"
              >
                <span class="text-secondary">{{
                  t("admin.orderStatusModal.expectedDeliveryLabel")
                }}</span>
                <span style="color: var(--text-primary)">{{
                  formatDateTime(orderDetailData.ngayGiaoDuKien)
                }}</span>
              </div>
              <div
                v-if="orderDetailData.ngayGiaoThucTe"
                class="d-flex justify-content-between small mt-1"
              >
                <span class="text-secondary">{{
                  t("admin.orderStatusModal.actualDeliveryLabel")
                }}</span>
                <span class="text-success fw-semibold">{{
                  formatDateTime(orderDetailData.ngayGiaoThucTe)
                }}</span>
              </div>
            </div>

            <!-- Mã vận đơn -->
            <div
              v-if="orderDetailData.maVanDon"
              style="border-top: 1px solid var(--border-color-soft); padding-top: 12px"
            >
              <div
                class="text-secondary fw-bold mb-2"
                style="font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.06em"
              >
                {{ t("admin.orderStatusModal.trackingCodeLabel") }}
              </div>
              <div class="fw-semibold" style="font-family: monospace; color: var(--text-primary)">
                {{ orderDetailData.maVanDon }}
              </div>
            </div>

            <!-- Thông tin khách hàng & Giao hàng -->
            <div style="border-top: 1px solid var(--border-color-soft); padding-top: 12px">
              <div
                class="text-secondary fw-bold mb-2"
                style="font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.06em"
              >
                Thông tin khách hàng & Giao hàng
              </div>
              <div class="d-flex flex-column gap-2 small">
                <!-- Khách hàng / liên hệ -->
                <div
                  class="p-2 rounded-2"
                  style="background: var(--bg-input); border: 1px solid var(--border-color-soft)"
                >
                  <div
                    class="d-flex align-items-center gap-1.5 fw-semibold"
                    style="color: var(--text-primary); font-size: 0.82rem"
                  >
                    <User :size="13" class="text-primary flex-shrink-0" />
                    <span>{{ customerName(orderDetailData.khachHangId) }}</span>
                  </div>
                  <div
                    v-if="customerPhone(orderDetailData)"
                    class="d-flex align-items-center gap-1.5 text-secondary mt-1"
                    style="font-size: 0.76rem"
                  >
                    <Phone :size="11" class="flex-shrink-0" />
                    <span>{{ customerPhone(orderDetailData) }}</span>
                  </div>
                  <div
                    v-if="customerEmail(orderDetailData)"
                    class="d-flex align-items-center gap-1.5 text-secondary mt-0.5"
                    style="font-size: 0.76rem"
                  >
                    <Mail :size="11" class="flex-shrink-0" />
                    <span class="text-truncate">{{ customerEmail(orderDetailData) }}</span>
                  </div>
                </div>

                <!-- Địa chỉ giao hàng & Người nhận -->
                <div
                  class="p-2 rounded-2"
                  style="background: var(--bg-input); border: 1px solid var(--border-color-soft)"
                >
                  <div
                    class="text-secondary fw-semibold mb-1 d-flex align-items-center gap-1"
                    style="font-size: 0.68rem; text-transform: uppercase; letter-spacing: 0.04em"
                  >
                    <MapPin :size="11" class="text-danger" /> Địa chỉ giao hàng:
                  </div>
                  <div
                    v-if="
                      orderDetailData.nguoiNhan &&
                      orderDetailData.nguoiNhan !== customerName(orderDetailData.khachHangId)
                    "
                    class="small mb-1"
                    style="font-size: 0.76rem; color: var(--text-secondary)"
                  >
                    Người nhận:
                    <strong style="color: var(--text-primary)">{{
                      orderDetailData.nguoiNhan
                    }}</strong>
                  </div>
                  <div
                    v-if="
                      orderDetailData.sdtNguoiNhan &&
                      orderDetailData.sdtNguoiNhan !== customerPhone(orderDetailData)
                    "
                    class="small text-secondary mb-1"
                    style="font-size: 0.76rem"
                  >
                    SĐT nhận: <strong>{{ orderDetailData.sdtNguoiNhan }}</strong>
                  </div>
                  <div style="font-size: 0.78rem; color: var(--text-primary); line-height: 1.4">
                    {{ deliveryAddressText(orderDetailData) }}
                  </div>
                  <div
                    v-if="orderDetailData.ghiChu"
                    class="mt-1 pt-1 border-top border-secondary small text-secondary"
                    style="font-size: 0.73rem"
                  >
                    <span class="fw-semibold">Ghi chú:</span> {{ orderDetailData.ghiChu }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- ══ MODAL CHỌN SERIAL TRƯỚC KHI CHUYỂN SANG ĐANG ĐÓNG GÓI ══ -->
  <div
    v-if="showXacNhanSerialModal"
    class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
    style="background: rgba(15,23,42,0.6); z-index: 1070; backdrop-filter: blur(4px);"
    @click.self="showXacNhanSerialModal = false"
  >
    <div
      class="rounded-4 shadow-lg overflow-hidden"
      style="background: var(--bg-card); width: 560px; max-height: 88vh; display: flex; flex-direction: column; border: 1px solid var(--border-color-soft);"
    >
      <!-- Header -->
      <div class="d-flex align-items-center justify-content-between px-4 py-3"
        style="border-bottom: 1px solid var(--border-color-soft); background: var(--bg-card-alt);"
      >
        <div class="d-flex align-items-center gap-2">
          <div class="rounded-3 d-flex align-items-center justify-content-center"
            style="width:36px; height:36px; background:linear-gradient(135deg,#f97316,#ea580c); color:#fff; flex-shrink:0;"
          >
            <Package :size="18" />
          </div>
          <div>
            <div class="fw-bold" style="color:var(--text-heading); font-size:0.95rem;">
              Xác nhận Serial &amp; Chuyển sang Đóng gói
            </div>
            <div class="text-secondary" style="font-size:0.72rem; font-family:monospace;">
              Đơn hàng #{{ xacNhanOrder?.maDonHang }}
            </div>
          </div>
        </div>
        <button
          class="btn-close btn-sm"
          :aria-label="t('common.close')"
          @click="showXacNhanSerialModal = false"
        ></button>
      </div>

      <!-- Hướng dẫn -->
      <div class="px-4 py-2" style="background: #fff7ed; border-bottom: 1px solid #fed7aa;">
        <div class="d-flex align-items-center gap-2 small" style="color:#9a3412;">
          <Info :size="14" style="flex-shrink:0;" />
          <span>Chọn đúng số lượng serial cho mỗi sản phẩm. Sau khi xác nhận, đơn sẽ chuyển sang <strong>Đang đóng gói</strong>.</span>
        </div>
      </div>

      <!-- Body: danh sách sản phẩm + serial -->
      <div class="overflow-y-auto px-4 py-3 d-flex flex-column gap-3" style="flex:1;">
        <div v-if="xacNhanLoading" class="d-flex align-items-center justify-content-center gap-2 py-5 text-secondary">
          <span class="spinner-border spinner-border-sm"></span>
          <span style="font-size:0.85rem;">Đang tải danh sách serial...</span>
        </div>

        <div v-else>
          <div v-if="xacNhanError" class="alert alert-danger py-2 small mb-3">{{ xacNhanError }}</div>

          <div
            v-for="line in xacNhanLines"
            :key="line.id"
            class="rounded-3 border overflow-hidden"
            style="border-color: var(--border-color-soft);"
          >
            <!-- Product header -->
            <div class="d-flex align-items-center justify-content-between px-3 py-2"
              style="background:var(--bg-card-alt); border-bottom:1px solid var(--border-color-soft);"
            >
              <div class="d-flex align-items-center gap-2">
                <div class="rounded-2 d-flex align-items-center justify-content-center"
                  style="width:30px;height:30px;background:var(--bg-input);border:1px solid var(--border-color-soft);flex-shrink:0;"
                >
                  <Laptop :size="14" style="color:var(--text-secondary);" />
                </div>
                <div>
                  <div class="fw-semibold" style="font-size:0.85rem;color:var(--text-primary); line-height:1.2;">
                    {{ productByBienThe(line.bienTheId)?.tenSanPham || line.maSku }}
                  </div>
                  <div class="text-secondary" style="font-size:0.72rem;">SKU: {{ line.maSku }}</div>
                </div>
              </div>
              <!-- Progress badge -->
              <div class="d-flex align-items-center gap-1.5">
                <div
                  class="rounded-pill px-2 py-0 fw-bold d-flex align-items-center gap-1"
                  style="font-size:0.75rem; font-family:monospace;"
                  :style="line.chosenSerialIds.size === line.soLuong
                    ? 'background:#dcfce7;color:#166534;'
                    : 'background:#fef9c3;color:#854d0e;'"
                >
                  <Check v-if="line.chosenSerialIds.size === line.soLuong" :size="11" />
                  <span>{{ line.chosenSerialIds.size }}/{{ line.soLuong }}</span>
                </div>
              </div>
            </div>

            <!-- Serial list -->
            <div class="px-3 py-2.5">
              <div v-if="xacNhanAvailableSerials(line).length === 0"
                class="d-flex align-items-center gap-2 py-2 text-danger small"
              >
                <AlertCircle :size="14" />
                <span>Không có serial trong kho cho sản phẩm này. Vui lòng nhập hàng trước.</span>
              </div>
              <div v-else class="d-flex flex-wrap gap-2">
                <button
                  v-for="s in xacNhanAvailableSerials(line)"
                  :key="s.chiTietId"
                  class="serial-chip"
                  :class="{ 'serial-chip--chosen': line.chosenSerialIds.has(s.chiTietId) }"
                  :title="s.trangThai"
                  @click="xacNhanToggleSerial(line, s.chiTietId)"
                >
                  <Check v-if="line.chosenSerialIds.has(s.chiTietId)" :size="11" style="flex-shrink:0;" />
                  <span>{{ s.soSerial }}</span>
                </button>
              </div>
              <div v-if="xacNhanAvailableSerials(line).length > 0"
                class="mt-2 text-muted" style="font-size:0.7rem;"
              >
                Nhấn vào serial để chọn/bỏ chọn. Cần chọn đúng <strong>{{ line.soLuong }}</strong> serial.
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div class="d-flex align-items-center justify-content-between gap-2 px-4 py-3"
        style="border-top:1px solid var(--border-color-soft); background:var(--bg-card-alt);"
      >
        <div class="small text-secondary">
          <template v-if="xacNhanAllLinesComplete">
            <span style="color:#166534; font-weight:600;">✓ Đã chọn đủ serial — sẵn sàng xác nhận</span>
          </template>
          <template v-else>
            Còn {{ xacNhanLines.filter(l => l.chosenSerialIds.size < l.soLuong).length }} sản phẩm chưa đủ serial
          </template>
        </div>
        <div class="d-flex gap-2">
          <button class="btn btn-sm btn-outline-secondary rounded-pill px-3" @click="showXacNhanSerialModal = false">
            Huỷ
          </button>
          <button
            class="btn btn-sm rounded-pill px-3 fw-semibold d-flex align-items-center gap-1.5"
            :disabled="!xacNhanAllLinesComplete || xacNhanLoading"
            style="background:linear-gradient(135deg,#f97316,#ea580c); color:#fff; border:none;"
            @click="confirmXacNhanSerial"
          >
            <span v-if="xacNhanLoading" class="spinner-border spinner-border-sm" style="width:14px;height:14px;border-width:2px;"></span>
            <Package v-else :size="14" />
            <span>Xác nhận &amp; Đóng gói</span>
          </button>
        </div>
      </div>
    </div>
  </div>

  <ProductDetailModal
    v-model="showDetailModal"
    :san-pham-id="detailModalSanPhamId"
    :san-pham-name="detailModalSanPhamName"
    :only-bien-the-ids="detailModalBienTheIds"
  />

  <!-- Modal in hoa don -->
  <InvoiceModal
    :show="showInvoice"
    :don-hang-id="invoiceOrder?.donHangId"
    :order="invoiceOrder"
    @close="showInvoice = false"
  />
</template>

<style scoped>
/* Màu chữ sáng tùy chỉnh */
.text-light {
  color: var(--text-primary) !important;
}

/* ─── Serial chip trong modal chọn serial ─── */
.serial-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 6px;
  border: 1.5px solid var(--border-color-strong, #cbd5e1);
  background: var(--bg-input, #f8fafc);
  color: var(--text-primary, #1e293b);
  font-family: monospace;
  font-size: 0.75rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
  line-height: 1.4;
}
.serial-chip:hover {
  border-color: #f97316;
  background: #fff7ed;
  color: #c2410c;
}
.serial-chip--chosen {
  border-color: #16a34a !important;
  background: #dcfce7 !important;
  color: #166534 !important;
  font-weight: 700;
}
.serial-chip--chosen:hover {
  border-color: #dc2626 !important;
  background: #fee2e2 !important;
  color: #dc2626 !important;
}
.serial-chip--reserved {
  /* removed: không còn màu vàng cho serial đã đặt trước */
}

/* Hàng nút lịch sử đơn hàng */
.alt-history-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 16px 12px;
  background: var(--bg-card-alt);
  border-bottom: 1px solid var(--border-color);
}

/* Bố cục thanh thao tác toolbar */
.alt-toolbar__actions {
  flex-wrap: nowrap !important;
}
.alt-toolbar__actions .alt-search {
  flex-shrink: 1;
  min-width: 180px;
}

/* ─── Advanced Filter Panel ─── */
.alt-btn--filter {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  border: 1px solid var(--border-color, #e2e8f0);
  border-radius: 8px;
  background: var(--bg-card, #fff);
  color: var(--text-primary, #1e293b);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
  flex-shrink: 0;
}
.alt-btn--filter:hover,
.alt-btn--filter-active {
  border-color: var(--pink-400, #f472b6);
  background: var(--pink-50, #fdf2f8);
  color: var(--pink-700, #be185d);
}
.filter-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: var(--pink-600, #db2777);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
}
.alt-btn--ghost-sm {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  border: 1px solid var(--border-color, #e2e8f0);
  border-radius: 8px;
  background: transparent;
  color: var(--text-secondary, #64748b);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
  flex-shrink: 0;
}
.alt-btn--ghost-sm:hover {
  background: #fee2e2;
  color: #dc2626;
  border-color: #dc2626;
}
.adv-filter-panel {
  border-top: 1px solid var(--border-color, #e2e8f0);
  background: var(--bg-card-alt, #f8fafc);
  padding: 12px 16px;
  animation: slideDown 0.15s ease;
}
@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
.adv-filter-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 12px;
}
.adv-filter-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 140px;
}
.adv-filter-group--range {
  min-width: 240px;
}
.adv-filter-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-secondary, #64748b);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.adv-filter-select,
.adv-filter-input {
  padding: 6px 10px;
  border: 1px solid var(--border-color, #e2e8f0);
  border-radius: 7px;
  background: var(--bg-input, #fff);
  color: var(--text-primary, #1e293b);
  font-size: 13px;
  outline: none;
  transition: border-color 0.15s;
  width: 100%;
}
.adv-filter-select:focus,
.adv-filter-input:focus {
  border-color: var(--pink-500, #ec4899);
}
.adv-filter-range {
  display: flex;
  align-items: center;
  gap: 6px;
}
.adv-filter-range .adv-filter-input {
  width: 100px;
}
.adv-filter-sep {
  color: var(--text-secondary, #94a3b8);
  font-size: 13px;
  font-weight: 600;
}
.adv-filter-reset {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  border: 1px solid #dc2626;
  border-radius: 7px;
  background: transparent;
  color: #dc2626;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  align-self: flex-end;
  transition: all 0.15s;
}
.adv-filter-reset:hover {
  background: #dc2626;
  color: #fff;
}
</style>
