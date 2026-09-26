<script setup>
import { ref, reactive, computed, onMounted, watch } from "vue";
import { Search } from "@lucide/vue";
import { Filter, X, ChevronDown, ChevronUp } from '@lucide/vue';
import { t } from "../../i18n/index.js";
import {
  orderStatusLabel, orderStatusColor, orderStatusIcon, paymentStatusLabel, paymentStatusColor, paymentStatusIcon,
  paymentMethodLabel, paymentMethodIcon, channelLabel, channelColor,
  isQrPayment, QR_TIMELINE_STEPS, isQrStepReached, isQrStepDone, isQrStepCurrent, getQrEffectiveStatus
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
import ProductDetailModal from "./ProductDetailModal.vue";
import Pagination from "../common/Pagination.vue";
import { usePagination } from "../../composables/usePagination.js";
import { CheckCircle2, Check, Package, Truck, Bike, Inbox, Laptop, User, Printer, Phone, Mail, MapPin, QrCode, ShieldCheck, Clock, FileText } from '@lucide/vue';
import InvoiceModal from "./InvoiceModal.vue";

// Nhận order ID để navigate từ ngoài (CustomerDetailModal tab đơn hàng → "Xem chi tiết")
const props = defineProps({
  navigateToOrderId: { type: Number, default: null },
});
const emit = defineEmits(["order-detail-opened"]);

onMounted(() => { ensureOrders(); ensureCustomers(); ensureProducts(); });

// Khi navigateToOrderId được set từ bên ngoài → mở modal chi tiết đơn đó
watch(() => props.navigateToOrderId, (id) => {
  if (!id) return;
  const order = (OrdersStore.items ?? []).find(o => o.donHangId === id);
  if (order) openOrderDetail(order);
}, { immediate: true });

// ── Helpers ───────────────────────────────────────────────────────────────────
const customerObj = (id) =>
  (CustomersStore.items ?? []).find((c) => c.khachHangId === id);

const customerName = (id) =>
  customerObj(id)?.hoTen ?? `KH#${id}`;

const customerPhone = (order) =>
  order?.sdtNguoiNhan || order?.khachHangSdt || customerObj(order?.khachHangId)?.soDienThoai || '';

const customerEmail = (order) =>
  customerObj(order?.khachHangId)?.email || '';

const deliveryAddressText = (order) => {
  if (order?.diaChiGiaoHangText) return order.diaChiGiaoHangText;
  if (order?.khachHangDiaChi) return order.khachHangDiaChi;
  const c = customerObj(order?.khachHangId);
  if (c?.diaChi) return c.diaChi;
  if (order?.kenhBan === 'in_store') return 'Khách nhận tại quầy';
  return 'Chưa có địa chỉ giao hàng';
};

// Định dạng ngày YYYY-MM-DD cho input date
const toDateInputValue = (d) => {
  const y = d.getFullYear(), m = String(d.getMonth() + 1).padStart(2, '0'), day = String(d.getDate()).padStart(2, '0');
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

const activeOrderFilterCount = computed(() =>
  [orderFilters.trangThaiDonHang, orderFilters.trangThaiThanhToan, orderFilters.kenhBan,
   orderFilters.ngayFrom, orderFilters.ngayTo,
   orderFilters.tongTienMin !== "" ? orderFilters.tongTienMin : "",
   orderFilters.tongTienMax !== "" ? orderFilters.tongTienMax : "",
  ].filter((v) => v !== "").length
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
const orderViewMode = ref('today');
const historySelectedDate = ref(null); // 'YYYY-MM-DD'

// Danh sách đơn hàng làm nền cho bộ lọc bên dưới, tùy theo chế độ xem đang chọn.
const ordersBaseList = computed(() => {
  const all = OrdersStore.items ?? [];
  if (orderViewMode.value === 'history-day' && historySelectedDate.value) {
    return all.filter((o) => o.ngayDat?.slice(0, 10) === historySelectedDate.value);
  }
  if (orderViewMode.value === 'today') {
    return all.filter((o) => o.ngayDat?.slice(0, 10) === toDateInputValue(new Date()));
  }
  return all;
});

const filteredOrders = computed(() => {
  const q = orderSearch.value.trim().toLowerCase();
  return ordersBaseList.value.filter((o) => {
    if (orderFilters.trangThaiDonHang   && o.trangThaiDonHang   !== orderFilters.trangThaiDonHang)   return false;
    if (orderFilters.trangThaiThanhToan && o.trangThaiThanhToan !== orderFilters.trangThaiThanhToan) return false;
    if (orderFilters.kenhBan            && o.kenhBan            !== orderFilters.kenhBan)            return false;
    // ngày đặt từ-đến
    if (orderFilters.ngayFrom && (o.ngayDat ?? '').slice(0, 10) < orderFilters.ngayFrom) return false;
    if (orderFilters.ngayTo   && (o.ngayDat ?? '').slice(0, 10) > orderFilters.ngayTo)   return false;
    // khoảng tổng tiền
    const tong = Number(o.tongTien ?? 0);
    if (orderFilters.tongTienMin !== "" && tong < Number(orderFilters.tongTienMin)) return false;
    if (orderFilters.tongTienMax !== "" && tong > Number(orderFilters.tongTienMax)) return false;
    if (!q) return true;
    const name = customerName(o.khachHangId).toLowerCase();
    return String(o.donHangId).includes(q) || (o.maDonHang ?? '').toLowerCase().includes(q) || name.includes(q) || (o.nguoiNhan ?? '').toLowerCase().includes(q) || (o.sdtNguoiNhan ?? '').includes(q);
  });
});
const { currentPage, totalPages, pagedItems: pagedOrders, pageSize } = usePagination(filteredOrders);
watch([orderSearch, () => orderFilters.trangThaiDonHang, () => orderFilters.trangThaiThanhToan, () => orderFilters.kenhBan, () => orderFilters.ngayFrom, () => orderFilters.ngayTo, () => orderFilters.tongTienMin, () => orderFilters.tongTienMax], () => {
  currentPage.value = 0;
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
  (OrdersStore.items ?? []).forEach((o) => {
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

// Tải danh sách serial theo biến thể
const fetchSerialMap = async (bienTheIds) => {
  const results = await Promise.all(
    bienTheIds.map(id => ChiTietSanPhamService.getByBienThe(id).catch(() => []))
  );
  const map = {};
  bienTheIds.forEach((id, i) => { map[id] = results[i]; });
  return map;
};

// ── Order detail modal (xem san pham trong don) ───────────────────────────────
const showOrderDetailModal = ref(false);
const orderDetailData      = ref(null);   // don hang dang xem
const orderDetailItems     = ref([]);     // ChiTietDonHangResponse[]
const orderDetailPayments  = ref([]);     // ThanhToanResponse[] — co the rong (don cu/don online)
const orderDetailLoading   = ref(false);

// Invoice modal
const showInvoice = ref(false);
const invoiceOrder = ref(null);

// Gom các khoản thanh toán theo phương thức
const orderDetailPaymentsSummary = computed(() => {
  const map = new Map();
  for (const p of orderDetailPayments.value) {
    const cur = map.get(p.phuongThucThanhToan) ?? { method: p.phuongThucThanhToan, count: 0, total: 0 };
    cur.count += 1;
    cur.total += p.soTien ?? 0;
    map.set(p.phuongThucThanhToan, cur);
  }
  return [...map.values()];
});

const openOrderDetail = async (o) => {
  orderDetailData.value  = o;
  orderDetailItems.value = [];
  orderDetailPayments.value = [];
  showOrderDetailModal.value = true;
  orderDetailLoading.value = true;
  emit("order-detail-opened", o.donHangId); // thông báo cho AdminPage reset selectedOrderId
  // Chuyển đơn tại quầy sang delivered
  if (o.kenhBan === 'in_store' && o.trangThaiDonHang === 'pending') {
    jumpToStatus(o, 'delivered').catch(() => {});
  }
  try {
    orderDetailItems.value = await ChiTietDonHangService.getByDonHang(o.donHangId).catch(err => { console.error('chi tiet don hang error', err); return []; });
    orderDetailPayments.value = await ThanhToanService.getByDonHang(o.donHangId).catch(err => { console.error('thanh toan error', err); return []; });
  } finally {
    orderDetailLoading.value = false;
  }
};

// Tim ten san pham tu bienTheId trong danh sach products da load
const productByBienThe = (bienTheId) => (ProductsStore.items ?? []).find(p => p.bienTheId === bienTheId);

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
  for (const p of (ProductsStore.items ?? [])) {
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
  await refreshOrders();
  const updated = (OrdersStore.items ?? []).find(o => o.donHangId === orderDetailData.value?.donHangId);
  if (updated) orderDetailData.value = updated;
  orderDetailItems.value = await ChiTietDonHangService.getByDonHang(orderDetailData.value.donHangId).catch(() => []);
  orderDetailPayments.value = await ThanhToanService.getByDonHang(orderDetailData.value.donHangId).catch(() => []);
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



// Modal xem chi tiết biến thể sản phẩm
const showDetailModal = ref(false);
const detailModalSanPhamId = ref(null);
const detailModalSanPhamName = ref('');
const detailModalBienTheIds = ref([]);

const openVariantDetail = (bienTheId) => {
  const v = productByBienThe(bienTheId);
  if (!v) return;
  detailModalSanPhamId.value = v.sanPhamId;
  detailModalSanPhamName.value = v.tenSanPham;
  // Hiển thị biến thể trong chi tiết đơn hàng
  detailModalBienTheIds.value = [...new Set(
    orderDetailItems.value
      .map((item) => item.bienTheId)
      .filter((id) => productByBienThe(id)?.sanPhamId === v.sanPhamId)
  )];
  showDetailModal.value = true;
};

// ── Order status helpers (dùng chung — xem src/utils/orderStatus.js) ──────────

// Cập nhật trạng thái đơn hàng
const showOrderModal = ref(false);
const editingOrder = ref(null);
const orderStatusError = ref("");
const orderStatusSaving = ref(false);
const orderStatusForm = reactive({
  trangThaiDonHang: "",
  trangThaiThanhToan: "",
  ngayGiaoDuKien: "", // Ngày dự kiến giao hàng
  ngayGiaoThucTe: "", // Ngày khách nhận hàng thực tế
  maVanDon: "",        // Mã vận đơn — nhân viên/admin nhập tay khi chuyển sang "Đang giao"
});

const openOrderStatus = (o) => {
  editingOrder.value = o;
  orderStatusForm.trangThaiDonHang = o.trangThaiDonHang ?? "";
  orderStatusForm.trangThaiThanhToan = o.trangThaiThanhToan ?? "";
  orderStatusForm.ngayGiaoDuKien = o.ngayGiaoDuKien?.slice(0, 16) ?? "";
  orderStatusForm.ngayGiaoThucTe = o.ngayGiaoThucTe?.slice(0, 16) ?? "";
  orderStatusForm.maVanDon = o.maVanDon ?? "";
  orderStatusError.value = "";
  showOrderModal.value = true;
};
// Tạo dữ liệu cập nhật trạng thái đơn hàng
const buildOrderUpdateBody = (o, { trangThaiDonHang, trangThaiThanhToan, ngayGiaoDuKien, ngayGiaoThucTe, maVanDon }) => ({
  khachHangId: o.khachHangId,
  nhanVienId: o.nhanVienId ?? null,
  khuyenMaiId: o.khuyenMaiId ?? null,
  diaChiGiaoHangId: o.diaChiGiaoHangId ?? null,
  diaChiGiaoHangText: o.diaChiGiaoHangText ?? null,
  nguoiNhan: o.nguoiNhan || customerName(o.khachHangId),
  sdtNguoiNhan:
    o.sdtNguoiNhan ||
    ((CustomersStore.items ?? []).find((c) => c.khachHangId === o.khachHangId)
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
  maVanDon: maVanDon || null,
});

const saveOrderStatus = async () => {
  orderStatusError.value = "";
  if (orderStatusSaving.value) return;
  orderStatusSaving.value = true;
  try {
    const o = editingOrder.value;
    const body = buildOrderUpdateBody(o, {
      trangThaiDonHang: orderStatusForm.trangThaiDonHang,
      trangThaiThanhToan: orderStatusForm.trangThaiThanhToan,
      ngayGiaoDuKien: orderStatusForm.ngayGiaoDuKien,
      ngayGiaoThucTe: orderStatusForm.ngayGiaoThucTe,
      maVanDon: orderStatusForm.maVanDon,
    });
    const res = await DonHangService.update(o.donHangId, body);
    if (!res.ok) {
      orderStatusError.value = t('admin.errors.saveFailedWithText', { status: res.status, text: await res.text() });
      return;
    }
    showOrderModal.value = false;
    await refreshOrders();
  } catch (e) {
    orderStatusError.value = e.message;
  } finally {
    orderStatusSaving.value = false;
  }
};

// Trạng thái đơn hàng kế tiếp
const NEXT_ORDER_STATUS = {
  pending: 'confirmed', confirmed: 'processing', processing: 'out_for_delivery',
  shipping: 'out_for_delivery', out_for_delivery: 'awaiting_confirmation',
};
const NEXT_ORDER_STATUS_LABEL = {
  pending:          { icon: CheckCircle2, key: 'admin.orders.nextConfirm' },
  confirmed:        { icon: Package, key: 'admin.orders.nextPack' },
  processing:       { icon: Bike, key: 'admin.orders.nextOutForDelivery' },
  shipping:         { icon: Bike, key: 'admin.orders.nextOutForDelivery' },
  out_for_delivery: { icon: Inbox, key: 'admin.orders.nextDelivered' },
};

// Danh sách trạng thái theo quy trình đơn hàng
const LINEAR_STATUS_ORDER = [
  'pending', 'confirmed', 'processing',
  'out_for_delivery', 'awaiting_confirmation', 'delivered',
];

const getLinearStatusIndex = (status) => {
  if (status === 'shipping') return LINEAR_STATUS_ORDER.indexOf('processing');
  return LINEAR_STATUS_ORDER.indexOf(status);
};

// Timeline đơn hàng: Hỗ trợ 8 bước dành riêng cho đơn thanh toán qua mã QR
const orderTimelineSteps = computed(() => {
  if (orderDetailData.value?.kenhBan === 'in_store') {
    return [
      {
        id: 'delivered',
        title: t('orderStatus.timeline.deliveredTitle'),
        desc: t('orderStatus.timeline.inStoreDeliveredDesc') || t('orderStatus.timeline.deliveredDesc'),
        icon: CheckCircle2,
      },
    ];
  }
  if (isQrPayment(orderDetailData.value)) {
    return QR_TIMELINE_STEPS;
  }
  return [
    { id: 'pending',                title: orderStatusLabel('pending'),                desc: t('orderStatus.timeline.placedDesc'),    icon: CheckCircle2 },
    { id: 'confirmed',              title: orderStatusLabel('confirmed'),              desc: t('orderStatus.timeline.confirmedDesc'), icon: CheckCircle2 },
    { id: 'processing',             title: orderStatusLabel('processing'),             desc: t('orderStatus.timeline.packingDesc'),   icon: Package },
    { id: 'out_for_delivery',       title: orderStatusLabel('out_for_delivery'),       desc: t('orderStatus.timeline.outForDeliveryDesc'), icon: Bike },
    { id: 'awaiting_confirmation',  title: orderStatusLabel('awaiting_confirmation'),  desc: t('orderStatus.timeline.deliveredDesc'),  icon: Inbox },
    { id: 'delivered',              title: orderStatusLabel('delivered'),              desc: t('orderStatus.timeline.deliveredDesc'),  icon: CheckCircle2 },
  ];
});

// Kiểm tra trạng thái đã qua trên timeline
const isStepReached = (order, stepId) => {
  if (order?.kenhBan === 'in_store') {
    return !['cancelled', 'returned'].includes(order.trangThaiDonHang);
  }
  if (isQrPayment(order)) {
    return isQrStepReached(order, stepId);
  }
  const cur = getLinearStatusIndex(order.trangThaiDonHang);
  const idx = LINEAR_STATUS_ORDER.indexOf(stepId);
  return cur !== -1 && idx !== -1 && idx <= cur;
};

// Kiểm tra bước đã hoàn tất trên timeline
const isStepDoneById = (order, stepId) => {
  if (order?.kenhBan === 'in_store') {
    return !['cancelled', 'returned'].includes(order.trangThaiDonHang);
  }
  if (isQrPayment(order)) {
    return isQrStepDone(order, stepId);
  }
  const cur = getLinearStatusIndex(order.trangThaiDonHang);
  const idx = LINEAR_STATUS_ORDER.indexOf(stepId);
  return cur !== -1 && idx !== -1 && idx <= cur;
};

// Kiểm tra bước hiện tại
const isStepCurrentById = (order, stepId) => {
  if (order?.kenhBan === 'in_store') return stepId === 'delivered';
  if (isQrPayment(order)) {
    return isQrStepCurrent(order, stepId);
  }
  const curStatus = order?.trangThaiDonHang === 'shipping' ? 'processing' : order?.trangThaiDonHang;
  return curStatus === stepId;
};

// Bấm được khi step đó nằm sau trạng thái hiện tại (chuyển tiến), HOẶC chính là bước hiện tại
const canJumpToStep = (order, stepId) => {
  if (['cancelled', 'returned'].includes(order.trangThaiDonHang)) return false;
  if (order?.kenhBan === 'in_store') return false;
  if (isQrPayment(order)) {
    if (stepId === 'thanh_toan' && order.trangThaiThanhToan !== 'paid') return true;
    if (stepId === 'cho_xu_ly') return true;
    if (stepId === 'da_len_don') return true;
    if (stepId === 'dang_dong_goi') return true;
    if (stepId === 'dang_giao_hang') return true;
    if (stepId === 'da_giao_cho_xac_nhan') return true;
    if (stepId === 'da_giao') return true;
    return false;
  }
  const cur = getLinearStatusIndex(order.trangThaiDonHang);
  const idx = LINEAR_STATUS_ORDER.indexOf(stepId);
  return idx >= cur;
};

// Admin duyệt thanh toán qua mã QR cho đơn hàng
const confirmingPayment = ref(false);
const adminConfirmQrPayment = async (order) => {
  if (!order) return;
  confirmingPayment.value = true;
  try {
    await ThanhToanService.confirmPayment(order.donHangId, {
      soTien: order.thanhTien ?? order.tongTien,
      phuongThuc: 'chuyen_khoan',
      maGiaoDich: `ADMIN_QR_${Date.now()}`
    });
    showToast('Đã duyệt thanh toán qua mã QR thành công!', 'success');
    await refreshOrders();
    const updated = OrdersStore.items.find(o => o.donHangId === order.donHangId);
    if (updated) {
      orderDetailData.value = updated;
    }
  } catch (e) {
    showToast('Lỗi khi duyệt thanh toán: ' + (e.message || e), 'error');
  } finally {
    confirmingPayment.value = false;
  }
};

// Chuyển trạng thái đơn hàng theo bước đã chọn (Hỗ trợ cả 8 bước QR)
const jumpToStatus = async (order, stepId) => {
  let targetStatus = stepId;

  if (isQrPayment(order)) {
    if (stepId === 'thanh_toan') {
      if (order.trangThaiThanhToan !== 'paid') {
        await adminConfirmQrPayment(order);
      }
      return;
    }
    if (stepId === 'cho_xu_ly') {
      if (order.trangThaiThanhToan !== 'paid') {
        await adminConfirmQrPayment(order);
      }
      return;
    }
    if (stepId === 'da_len_don') {
      if (order.trangThaiThanhToan !== 'paid') {
        await adminConfirmQrPayment(order);
      }
      if (order.kenhBan === 'online' && order.trangThaiDonHang === 'pending') {
        await openXacNhanSerialModal(order);
        return;
      }
      targetStatus = 'confirmed';
    } else if (stepId === 'dang_dong_goi') {
      targetStatus = 'processing';
    } else if (stepId === 'dang_giao_hang') {
      targetStatus = 'out_for_delivery';
    } else if (stepId === 'da_giao_cho_xac_nhan') {
      targetStatus = 'awaiting_confirmation';
    } else if (stepId === 'da_giao') {
      targetStatus = 'delivered';
    }
  }

  if (order.trangThaiDonHang === targetStatus) return;
  if (!canJumpToStep(order, stepId)) return;
  // Bước cần nhập thêm mã vận đơn: mở modal nếu chưa có mã
  if (targetStatus === 'out_for_delivery' && !order.maVanDon) {
    openOrderStatus(order);
    orderStatusForm.trangThaiDonHang = 'out_for_delivery';
    return;
  }
  // Đơn online pending -> confirmed: mở modal chọn serial
  if (targetStatus === 'confirmed' && order.kenhBan === 'online') {
    await openXacNhanSerialModal(order);
    return;
  }
  // Cập nhật thanh toán và ngày giao hàng thực tế
  const body = buildOrderUpdateBody(order, {
    trangThaiDonHang: targetStatus,
    trangThaiThanhToan: (targetStatus === 'awaiting_confirmation' || targetStatus === 'delivered') && order.trangThaiThanhToan === 'unpaid'
      ? 'paid'
      : order.trangThaiThanhToan,
    ngayGiaoDuKien: order.ngayGiaoDuKien,
    ngayGiaoThucTe: (targetStatus === 'awaiting_confirmation' || targetStatus === 'delivered') && !order.ngayGiaoThucTe
      ? nowLocalIso()
      : order.ngayGiaoThucTe,
    maVanDon: order.maVanDon,
  });
  const res = await DonHangService.update(order.donHangId, body);
  if (!res.ok) { showToast(await res.text().catch(() => t('admin.errors.updateFailed', { status: res.status }))); return; }
  await refreshOrders();
  // Cập nhật lại orderDetailData để sidebar hiển thị đúng trạng thái mới ngay lập tức
  const updated = OrdersStore.items.find(o => o.donHangId === order.donHangId);
  if (updated) orderDetailData.value = updated;
};
const advanceOrderStatus = async (o) => {
  const next = NEXT_ORDER_STATUS[o.trangThaiDonHang];
  if (!next) return;
  // Đơn online chuyển sang "confirmed" (xác nhận) phải chọn serial trước
  if (next === 'confirmed' && o.kenhBan === 'online') {
    await openXacNhanSerialModal(o);
    return;
  }
  // Chuyển sang "Đang giao hàng": mở modal nhập mã vận đơn nếu chưa có
  if (next === 'out_for_delivery' && !o.maVanDon) {
    openOrderStatus(o);
    orderStatusForm.trangThaiDonHang = 'out_for_delivery';
    return;
  }
  const body = buildOrderUpdateBody(o, {
    trangThaiDonHang: next,
    trangThaiThanhToan: next === 'awaiting_confirmation' && o.trangThaiThanhToan === 'unpaid'
      ? 'paid'
      : o.trangThaiThanhToan,
    ngayGiaoDuKien: o.ngayGiaoDuKien,
    ngayGiaoThucTe: (next === 'awaiting_confirmation' || next === 'delivered') && !o.ngayGiaoThucTe
      ? nowLocalIso()
      : o.ngayGiaoThucTe,
    maVanDon: o.maVanDon,
  });
  const res = await DonHangService.update(o.donHangId, body);
  if (!res.ok) { showToast(await res.text().catch(() => t('admin.errors.updateFailed', { status: res.status }))); return; }
  // Tải lại danh sách đơn hàng từ máy chủ
  await refreshOrders();
  // Cập nhật lại orderDetailData để sidebar hiển thị đúng trạng thái mới ngay lập tức
  const updated = OrdersStore.items.find(o2 => o2.donHangId === o.donHangId);
  if (updated) orderDetailData.value = updated;
};

// Modal chọn serial xác nhận đơn online
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

// Lọc danh sách serial khả dụng
const xacNhanAvailableSerials = (line) => {
  const all = xacNhanSerialMap.value[line.bienTheId] ?? [];
  return all.filter((s) => s.trangThai === 'trong_kho' || line.chosenSerialIds.has(s.chiTietId));
};

const xacNhanToggleSerial = (line, serialId) => {
  if (line.chosenSerialIds.has(serialId)) { line.chosenSerialIds.delete(serialId); return; }
  // Chọn serial cho chi tiết đơn hàng
  if (line.soLuong === 1) { line.chosenSerialIds.clear(); line.chosenSerialIds.add(serialId); return; }
  if (line.chosenSerialIds.size < line.soLuong) line.chosenSerialIds.add(serialId);
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
    await refreshOrders();
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
        <span class="fw-bold" style="color:var(--text-heading);">{{ t('admin.orders.history') }}</span>
        <div class="alt-toolbar__actions">
          <button class="alt-btn alt-btn--ghost" @click="backToToday">{{ t('admin.orders.backToToday') }}</button>
        </div>
      </div>
      <div v-if="OrdersStore.loading" class="alt-empty">{{ t('admin.orders.loading') }}</div>
      <div v-else class="d-flex flex-column" style="padding:8px;">
        <div
          v-for="d in orderDatesGrouped" :key="d.dateKey"
          class="d-flex justify-content-between align-items-center px-3 py-3 rounded-3"
          style="cursor:pointer;"
          @click="openHistoryDay(d.dateKey)"
        >
          <span class="fw-semibold" style="color:var(--text-primary);">{{ d.label }}</span>
          <span class="text-secondary small d-flex align-items-center gap-2">{{ d.count }} {{ t('admin.orders.countSuffix') }} <span style="font-size:1.1rem;">›</span></span>
        </div>
        <div v-if="orderDatesGrouped.length===0" class="alt-empty">{{ t('admin.orders.empty') }}</div>
      </div>
    </div>
  </template>

  <!-- Chế độ: đơn hôm nay (mặc định) hoặc đơn của 1 ngày lịch sử đã chọn -->
  <template v-else>
    <div class="alt-card">
      <div class="alt-toolbar">
        <div class="alt-toolbar__left">
          <button v-if="orderViewMode==='history-day'" class="alt-btn alt-btn--ghost" @click="backToDateList">{{ t('admin.orders.backToDateList') }}</button>
          <span class="alt-toolbar__count">
            <span v-if="orderViewMode==='history-day'" class="fw-semibold" style="color:var(--text-primary);">{{ formatDateHeading(historySelectedDate) }} · </span>
            {{ filteredOrders.length }}/{{ ordersBaseList.length }} {{ t('admin.orders.countSuffix') }}
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
            <span v-if="activeOrderFilterCount > 0" class="filter-badge">{{ activeOrderFilterCount }}</span>
            <ChevronDown v-if="!isOrderFilterOpen" :size="13" />
            <ChevronUp v-else :size="13" />
          </button>
          <button v-if="activeOrderFilterCount > 0" class="alt-btn alt-btn--ghost-sm" @click="resetOrderFilters">
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
              <option value="pending">{{ orderStatusLabel('pending') }}</option>
              <option value="confirmed">{{ orderStatusLabel('confirmed') }}</option>
              <option value="processing">{{ orderStatusLabel('processing') }}</option>
              <option value="out_for_delivery">{{ orderStatusLabel('out_for_delivery') }}</option>
              <option value="awaiting_confirmation">{{ orderStatusLabel('awaiting_confirmation') }}</option>
              <option value="delivered">{{ orderStatusLabel('delivered') }}</option>
              <option value="cancelled">{{ orderStatusLabel('cancelled') }}</option>
              <option value="returned">{{ orderStatusLabel('returned') }}</option>
            </select>
          </div>
          <div class="adv-filter-group">
            <label class="adv-filter-label">Thanh toán</label>
            <select v-model="orderFilters.trangThaiThanhToan" class="adv-filter-select">
              <option value="">Tất cả</option>
              <option value="paid">{{ t('admin.orders.paid') }}</option>
              <option value="unpaid">{{ t('admin.orders.unpaid') }}</option>
            </select>
          </div>
          <div class="adv-filter-group">
            <label class="adv-filter-label">Kênh bán</label>
            <select v-model="orderFilters.kenhBan" class="adv-filter-select">
              <option value="">Tất cả</option>
              <option value="in_store">{{ channelLabel('in_store') }}</option>
              <option value="online">{{ channelLabel('online') }}</option>
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
              <input v-model="orderFilters.tongTienMin" type="number" min="0" placeholder="Từ" class="adv-filter-input" />
              <span class="adv-filter-sep">–</span>
              <input v-model="orderFilters.tongTienMax" type="number" min="0" placeholder="Đến" class="adv-filter-input" />
            </div>
          </div>
          <button v-if="activeOrderFilterCount > 0" class="adv-filter-reset" @click="resetOrderFilters">
            <X :size="13" /> Xóa bộ lọc
          </button>
        </div>
      </div>

      <!-- Nút xem lịch sử đơn hàng -->
      <div v-if="orderViewMode==='today'" class="alt-history-row">
        <button class="alt-btn alt-btn--ghost" @click="openOrderHistory">{{ t('admin.orders.history') }}</button>
      </div>
      <div v-if="OrdersStore.loading" class="alt-empty">{{ t('admin.orders.loading') }}</div>
      <div v-else class="alt-table-wrap">
        <table class="alt-table">
          <thead><tr><th style="width:40px;">{{ t('admin.common.stt') }}</th><th>{{ t('admin.orders.colOrderCode') }}</th><th>{{ t('admin.orders.colCustomer') }}</th><th>{{ t('admin.orders.colTotal') }}</th><th>{{ t('admin.orders.colOrderStatus') }}</th><th>{{ t('admin.orders.colPaymentStatus') }}</th><th>{{ t('admin.orders.colOrderDate') }}</th><th style="width:80px;">{{ t('admin.orders.colChannel') }}</th><th>{{ t('admin.orders.colAction') }}</th></tr></thead>
          <tbody>
            <tr v-for="(o, idx) in pagedOrders" :key="o.donHangId">
              <td class="text-secondary">{{ currentPage * pageSize + idx + 1 }}</td>
              <td class="text-secondary">{{ o.maDonHang || ('#' + o.donHangId) }}</td>
              <td>{{ customerName(o.khachHangId) }}</td>
              <td>{{ formatPrice(o.thanhTien) }}</td>
              <td>
                <span v-if="isQrPayment(o)" class="alt-tag" :style="{ background: getQrEffectiveStatus(o).color.bg, color: getQrEffectiveStatus(o).color.text, border: o.trangThaiThanhToan === 'unpaid' ? '1px solid #fed7aa' : 'none', fontWeight: '600' }">
                  <component :is="orderStatusIcon(o.trangThaiDonHang)" :size="13" /> {{ getQrEffectiveStatus(o).label }}
                </span>
                <span v-else class="alt-tag" :style="{ background: orderStatusColor(o.trangThaiDonHang).bg, color: orderStatusColor(o.trangThaiDonHang).text }">
                  <component :is="orderStatusIcon(o.trangThaiDonHang)" :size="13" /> {{ orderStatusLabel(o.trangThaiDonHang) }}
                </span>
              </td>
              <td>
                <span v-if="o.trangThaiThanhToan" class="alt-tag" :style="{ background: paymentStatusColor(o.trangThaiThanhToan).bg, color: paymentStatusColor(o.trangThaiThanhToan).text }">
                  <component :is="paymentStatusIcon(o.trangThaiThanhToan)" :size="13" /> {{ paymentStatusLabel(o.trangThaiThanhToan) }}
                </span>
                <span v-else class="text-secondary">—</span>
              </td>
              <td>
                {{ formatDate(o.ngayDat) }}
                <div v-if="o.ngayGiaoThucTe" class="text-success" style="font-size:0.72rem;">
                  <CheckCircle2 :size="13" style="vertical-align:-2px;" /> {{ t('admin.orderStatusModal.actualDeliveryLabel') }}: {{ formatDateTime(o.ngayGiaoThucTe) }}
                </div>
              </td>
              <td>
                <span v-if="o.kenhBan" class="alt-tag" :style="{ background: channelColor(o.kenhBan).bg, color: channelColor(o.kenhBan).text }">
                  {{ channelLabel(o.kenhBan) }}
                </span>
                <span v-else class="text-secondary">—</span>
              </td>
              <td>
                <div class="d-flex align-items-center gap-1.5">
                  <button
                    v-if="isQrPayment(o) && o.trangThaiThanhToan !== 'paid'"
                    class="alt-btn"
                    style="background:#16a34a; color:#fff; border:none; padding:4px 9px; font-size:0.75rem; border-radius:6px; font-weight:600; display:inline-flex; align-items:center; gap:3px; box-shadow:0 1px 3px rgba(22,163,74,0.3); white-space:nowrap;"
                    title="Duyệt thanh toán QR cho đơn này"
                    :disabled="confirmingPayment"
                    @click.stop="adminConfirmQrPayment(o)"
                  >
                    <CheckCircle2 :size="12" /> Duyệt QR
                  </button>
                  <button class="alt-btn alt-btn--ghost" style="padding:4px 12px;" @click="openOrderDetail(o)">{{ t('admin.orders.detail') }}</button>
                </div>
              </td>
            </tr>
            <tr v-if="filteredOrders.length===0"><td colspan="9" class="alt-empty">{{ t('admin.orders.empty') }}</td></tr>
          </tbody>
        </table>
        <div v-if="totalPages > 1" class="alt-pager"><Pagination :current-page="currentPage" :total-pages="totalPages" @page-change="currentPage = $event" /></div>
      </div>
    </div>
  </template>

  <!-- ══ MODAL THEM SAN PHAM CHI TIET ══ -->
  <div
    v-if="showAddItemDetailModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
    style="background:var(--bg-overlay);z-index:1070;" @click.self="showAddItemDetailModal=false"
  >
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card-inset);border:1px solid var(--border-color-strong);width:960px;max-width:97vw;max-height:93vh;">
      <!-- Header -->
      <div class="d-flex justify-content-between align-items-center px-4 py-3" style="border-bottom:1px solid var(--bg-input);">
        <span class="text-secondary" style="font-size:0.8rem;">{{ t('admin.addItemDetailModal.chooseProduct') }}</span>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="showAddItemDetailModal=false"></button>
      </div>

      <!-- Body -->
      <div v-if="addItemDetailGroup" class="overflow-y-auto flex-grow-1 p-0">
        <div class="d-flex" style="min-height:400px;">
          <!-- Left: Anh san pham -->
          <div
            class="d-flex flex-column align-items-center justify-content-center p-4"
            style="width:42%;background:var(--bg-page-alt);border-right:1px solid var(--bg-input);flex-shrink:0;"
          >
            <div style="width:100%;max-width:320px;aspect-ratio:4/3;display:flex;align-items:center;justify-content:center;background:var(--bg-card-alt);border-radius:12px;overflow:hidden;padding:16px;">
              <img
                v-if="(addItemCurrentVariant || addItemDetailGroup.variants[0])?.hinhAnhChinh"
                :src="(addItemCurrentVariant || addItemDetailGroup.variants[0]).hinhAnhChinh"
                style="max-width:100%;max-height:100%;object-fit:contain;"
              />
              <span v-else><Laptop :size="64" color="var(--text-muted)" /></span>
            </div>
            <div class="mt-3 d-flex gap-1 flex-wrap justify-content-center">
              <span
                v-for="tag in (addItemDetailGroup.variants[0]?.phanLoaiTen||'').split(',').filter(Boolean)"
                :key="tag" class="badge" style="background:rgba(244,63,94,0.12);color:var(--accent-fg);font-size:0.7rem;"
              >{{ tag.trim() }}</span>
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
                <button
                  v-for="cfg in addItemConfigs" :key="cfg.key"
                  class="btn btn-sm text-start"
                  style="padding:8px 12px;border-radius:8px;min-width:140px;"
                  :style="addItemSelectedConfig === cfg.key
                    ? 'background:rgba(244,63,94,0.12);border:2px solid var(--accent);color:var(--accent-fg);'
                    : 'background:var(--bg-card);border:1px solid var(--border-color-strong);color:var(--text-secondary);'"
                  @click="selectConfig(cfg.key)"
                >
                  <div style="font-size:0.78rem;font-weight:600;">{{ cfg.cpu || t('admin.addItemDetailModal.standard') }}</div>
                  <div style="font-size:0.68rem;">{{ [cfg.ram, cfg.oCung].filter(Boolean).join(' · ') }}</div>
                </button>
              </div>
            </div>

            <!-- Chon mau sac -->
            <div v-if="addItemColorsForConfig.length > 0" class="mb-3">
              <div class="text-secondary mb-2" style="font-size:0.72rem;font-weight:700;letter-spacing:.05em;">{{ t('admin.addItemDetailModal.color') }}</div>
              <div class="d-flex flex-wrap gap-2">
                <button
                  v-for="v in addItemColorsForConfig" :key="v.bienTheId"
                  class="btn btn-sm"
                  style="padding:6px 14px;border-radius:8px;"
                  :style="addItemSelectedColor === v.mauSac
                    ? 'background:rgba(244,63,94,0.12);border:2px solid var(--accent);color:var(--accent-fg);'
                    : 'background:var(--bg-card);border:1px solid var(--border-color-strong);color:var(--text-primary);'"
                  @click="addItemSelectedColor = v.mauSac"
                >
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
                <tr
                  v-for="([label, val]) in [
                    [t('admin.addItemDetailModal.specCpu'), addItemCurrentVariant.cpu],
                    [t('admin.addItemDetailModal.specRam'), addItemCurrentVariant.ram],
                    [t('admin.addItemDetailModal.specStorage'), addItemCurrentVariant.oCung],
                    [t('admin.addItemDetailModal.specGpu'), addItemCurrentVariant.gpu],
                    [t('admin.addItemDetailModal.specScreen'), addItemCurrentVariant.kichThuocManHinh],
                    [t('admin.addItemDetailModal.specOs'), addItemCurrentVariant.heDieuHanh],
                    [t('admin.addItemDetailModal.specBattery'), addItemCurrentVariant.pin],
                    [t('admin.addItemDetailModal.specWeight'), addItemCurrentVariant.trongLuongKg ? addItemCurrentVariant.trongLuongKg + ' kg' : null],
                  ].filter(([,v]) => v)" :key="label" style="border-top:1px solid var(--bg-input);"
                >
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
        <input
          v-model.number="addItemQty" type="number" min="1" max="99"
          class="form-control form-control-sm"
          style="width:80px;background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);"
        />
        <button
          class="btn btn-warning flex-grow-1 fw-bold" style="font-size:0.9rem;"
          :disabled="!addItemCurrentVariant || addItemQty < 1 || addItemLoading"
          @click="confirmAddFromDetail"
        >
          {{ addItemLoading ? t('admin.addItemDetailModal.adding') : t('admin.addItemDetailModal.addToOrder') }}
        </button>
      </div>
    </div>
  </div>

  <!-- ══ MODAL CHI TIET DON HANG ══ -->
  <div v-if="showOrderDetailModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1050;" @click.self="showOrderDetailModal=false">
    <div class="alt-card d-flex flex-column" style="width:840px;max-width:96vw;max-height:92vh;border-radius:14px;">
      <!-- Header gọn: chỉ tên khách + mã đơn + nút đóng. Tất cả action nằm bên sidebar phải. -->
      <div class="alt-toolbar">
        <div>
          <div class="fw-bold d-flex align-items-center gap-2" style="font-size:1.05rem;color:var(--text-heading);">
            <User :size="15" style="vertical-align:-2px;" />
            <span>{{ customerName(orderDetailData?.khachHangId) }}</span>
          </div>
          <div class="d-flex align-items-center gap-2 mt-0.5" style="font-size:0.78rem;flex-wrap:wrap;color:var(--text-muted);">
            <span>{{ t('admin.orderDetailModal.titlePrefix') }}{{ orderDetailData?.donHangId }}</span>
            <span v-if="orderDetailData?.maDonHang" style="font-family:monospace;">{{ orderDetailData.maDonHang }}</span>
            <span v-if="orderDetailData?.kenhBan" class="alt-tag" style="font-size:0.7rem;" :style="{ background: channelColor(orderDetailData.kenhBan).bg, color: channelColor(orderDetailData.kenhBan).text }">
              {{ channelLabel(orderDetailData.kenhBan) }}
            </span>
            <span>· {{ formatDate(orderDetailData?.ngayDat) }}</span>
          </div>
        </div>
        <div class="alt-toolbar__actions">
          <button class="alt-btn alt-btn--primary d-flex align-items-center gap-1" @click="invoiceOrder = orderDetailData; showInvoice = true">
            <Printer :size="13" /> In hóa đơn
          </button>
          <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="showOrderDetailModal=false"></button>
        </div>
      </div>

      <!-- Body 2 cột: trái = sản phẩm + tổng tiền, phải = sidebar trạng thái -->
      <div class="d-flex flex-grow-1 overflow-hidden">
        <!-- Cột trái: scroll độc lập -->
        <div class="overflow-y-auto flex-grow-1" style="border-right:1px solid var(--border-color-soft);">
          <!-- Danh sach san pham trong don -->
          <div class="p-3">
            <div class="text-secondary fw-bold mb-2 text-uppercase" style="font-size:0.72rem; letter-spacing:0.05em;">
              Sản phẩm trong đơn ({{ orderDetailItems.length }})
            </div>
            <div v-if="orderDetailLoading" class="text-secondary small text-center py-4">{{ t('admin.orderDetailModal.loading') }}</div>
            <div v-else-if="orderDetailItems.length === 0" class="text-secondary small text-center py-4">{{ t('admin.orderDetailModal.empty') }}</div>
            <div v-else class="d-flex flex-column gap-2">
              <div
                v-for="item in orderDetailItems" :key="item.id"
                class="d-flex align-items-start gap-3 p-2.5 rounded-3"
                style="background:var(--bg-input); border:1px solid var(--border-color-soft);"
              >
                <!-- Ảnh sản phẩm -->
                <div style="width:56px; height:50px; flex-shrink:0; background:var(--bg-card); border-radius:8px; display:flex; align-items:center; justify-content:center; overflow:hidden; border:1px solid var(--border-color-soft);">
                  <img
                    v-if="productByBienThe(item.bienTheId)?.hinhAnhChinh"
                    :src="productByBienThe(item.bienTheId).hinhAnhChinh"
                    style="max-width:50px; max-height:44px; object-fit:contain;"
                  />
                  <Laptop v-else :size="22" style="color:var(--text-muted);" />
                </div>

                <!-- Thông tin SP -->
                <div class="flex-grow-1 min-w-0">
                  <div class="fw-semibold text-truncate" style="font-size:0.88rem; color:var(--text-heading);">
                    {{ productByBienThe(item.bienTheId)?.tenSanPham || item.tenSanPham || 'Sản phẩm' }}
                  </div>
                  <!-- Phân loại cấu hình -->
                  <div
                    v-if="[productByBienThe(item.bienTheId)?.cpu, productByBienThe(item.bienTheId)?.ram, productByBienThe(item.bienTheId)?.oCung, productByBienThe(item.bienTheId)?.mauSac].filter(Boolean).length"
                    class="mt-1" style="font-size:0.74rem; color:var(--text-secondary);"
                  >
                    {{ [productByBienThe(item.bienTheId)?.cpu, productByBienThe(item.bienTheId)?.ram, productByBienThe(item.bienTheId)?.oCung, productByBienThe(item.bienTheId)?.mauSac].filter(Boolean).join(' · ') }}
                  </div>
                  <div class="d-flex align-items-center gap-2 mt-1" style="font-size:0.72rem; color:var(--text-muted); flex-wrap:wrap;">
                    <span v-if="item.maSku">SKU: <code style="color:var(--text-secondary);">{{ item.maSku }}</code></span>
                    <span v-if="item.soSerial">· Serial: <strong style="color:var(--accent-fg); font-family:monospace;">{{ item.soSerial }}</strong></span>
                  </div>
                </div>

                <!-- Giá + Số lượng -->
                <div class="text-end flex-shrink-0 d-flex flex-column align-items-end" style="min-width:110px;">
                  <div class="fw-bold" style="font-size:0.9rem; color:var(--accent-fg);">
                    {{ formatPrice(item.thanhTien ?? (item.donGia * (item.soLuong || 1))) }}
                  </div>
                  <div class="text-secondary" style="font-size:0.74rem;">
                    {{ formatPrice(item.donGia) }} × {{ item.soLuong || 1 }}
                  </div>
                  <button
                    v-if="productByBienThe(item.bienTheId)"
                    class="btn btn-sm btn-outline-secondary mt-1 py-0 px-2"
                    style="font-size:0.68rem; border-radius:4px;"
                    @click="openVariantDetail(item.bienTheId)"
                  >
                    {{ t('admin.orderDetailModal.detail') }}
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Footer: tong ket -->
          <div v-if="orderDetailData" class="px-4 py-3 d-flex flex-column gap-1" style="border-top:1px solid var(--border-color-soft);background:var(--bg-card-alt);">
            <div class="d-flex justify-content-between small text-secondary">
              <span>{{ t('admin.orderDetailModal.subtotal') }}</span><span>{{ formatPrice(orderDetailData.tongTien) }}</span>
            </div>
            <div v-if="orderDetailData.giamGia > 0" class="d-flex justify-content-between small text-success">
              <span>{{ t('admin.orderDetailModal.discount') }}</span><span>− {{ formatPrice(orderDetailData.giamGia) }}</span>
            </div>
            <div v-if="orderDetailData.kenhBan !== 'in_store'" class="d-flex justify-content-between small text-secondary">
              <span>{{ t('admin.orderDetailModal.shippingFee') }}</span>
              <span :class="orderDetailData.phiVanChuyen === 0 ? 'text-success' : ''">
                {{ orderDetailData.phiVanChuyen === 0 ? t('admin.orderDetailModal.free') : formatPrice(orderDetailData.phiVanChuyen) }}
              </span>
            </div>
            <div class="d-flex justify-content-between fw-bold pt-2 mt-1" style="border-top:1px solid var(--border-color);">
              <span style="color:var(--text-heading);">{{ t('admin.orderDetailModal.total') }}</span>
              <span class="text-warning" style="font-size:1rem;">{{ formatPrice(orderDetailData.thanhTien) }}</span>
            </div>


          </div>
        </div>

        <!-- Sidebar chi tiết trạng thái đơn hàng -->
        <div v-if="orderDetailData" class="d-flex flex-column" style="width:280px; min-width:280px; flex-shrink:0; background:var(--bg-card-alt);">
          <div class="overflow-y-auto p-3 d-flex flex-column gap-3">
            <!-- Nhóm trạng thái: badge trạng thái hiện tại -->
            <div>
              <div class="text-secondary fw-bold mb-2" style="font-size:0.7rem; text-transform:uppercase; letter-spacing:0.06em;">
                {{ t('admin.orderDetailModal.orderStatus') }}
              </div>
              <span v-if="isQrPayment(orderDetailData)" class="alt-tag d-inline-flex align-items-center gap-1" :style="{ background: getQrEffectiveStatus(orderDetailData).color.bg, color: getQrEffectiveStatus(orderDetailData).color.text, border: orderDetailData.trangThaiThanhToan === 'unpaid' ? '1px solid #fed7aa' : 'none', fontWeight: '600' }">
                <component :is="orderStatusIcon(orderDetailData.trangThaiDonHang)" :size="13" />
                {{ getQrEffectiveStatus(orderDetailData).label }}
              </span>
              <span v-else class="alt-tag d-inline-flex align-items-center gap-1" :style="{ background: orderStatusColor(orderDetailData.kenhBan === 'in_store' && !['cancelled','returned'].includes(orderDetailData.trangThaiDonHang) ? 'delivered' : orderDetailData.trangThaiDonHang).bg, color: orderStatusColor(orderDetailData.kenhBan === 'in_store' && !['cancelled','returned'].includes(orderDetailData.trangThaiDonHang) ? 'delivered' : orderDetailData.trangThaiDonHang).text }">
                <component :is="orderStatusIcon(orderDetailData.kenhBan === 'in_store' && !['cancelled','returned'].includes(orderDetailData.trangThaiDonHang) ? 'delivered' : orderDetailData.trangThaiDonHang)" :size="13" />
                {{ orderStatusLabel(orderDetailData.kenhBan === 'in_store' && !['cancelled','returned'].includes(orderDetailData.trangThaiDonHang) ? 'delivered' : orderDetailData.trangThaiDonHang) }}
              </span>

              <!-- Nút Duyệt thanh toán qua mã QR dành riêng cho Admin -->
              <div
                v-if="isQrPayment(orderDetailData) && orderDetailData.trangThaiThanhToan !== 'paid'"
                class="p-2.5 rounded-3 mt-2"
                style="background:#fff7ed; border:1px solid #fed7aa;"
              >
                <div class="fw-bold d-flex align-items-center gap-1.5 mb-1" style="font-size:0.78rem; color:#ea580c;">
                  <QrCode :size="15" /> Duyệt thanh toán VietQR
                </div>
                <div class="text-secondary small mb-2" style="font-size:0.72rem; line-height:1.4;">
                  Khách chọn chuyển khoản QR (Timo: 0338861232). Khi thấy tiền vào tài khoản, nhấn duyệt:
                </div>
                <button
                  type="button"
                  class="btn btn-sm btn-success fw-bold w-100 d-flex align-items-center justify-content-center gap-1.5 shadow-sm"
                  style="font-size:0.8rem; padding:6px 12px; background:#16a34a; border:none;"
                  :disabled="confirmingPayment"
                  @click="adminConfirmQrPayment(orderDetailData)"
                >
                  <span v-if="confirmingPayment" class="spinner-border spinner-border-sm me-1"></span>
                  <CheckCircle2 v-else :size="14" />
                  <span>{{ confirmingPayment ? 'Đang duyệt...' : 'Duyệt thanh toán QR' }}</span>
                </button>
              </div>
            </div>

            <!-- Timeline các bước xử lý đơn hàng (8 bước dành cho QR) -->
            <div>
              <div class="d-flex align-items-center justify-content-between mb-2">
                <div class="text-secondary fw-bold text-uppercase" style="font-size:0.7rem; letter-spacing:0.06em;">
                  {{ isQrPayment(orderDetailData) ? 'TIẾN TRÌNH THANH TOÁN QR' : t('orderStatus.timeline.title') }}
                </div>
              </div>
              <div class="d-flex flex-column gap-0" style="position:relative;">
                <div
                  v-for="(step, index) in orderTimelineSteps" :key="step.id"
                  class="d-flex align-items-start gap-3" style="position:relative;"
                >
                  <div class="d-flex flex-column align-items-center" style="width:32px; flex-shrink:0; position:relative;">
                    <button
                      type="button"
                      class="rounded-circle d-flex align-items-center justify-content-center position-relative p-0"
                      style="width:32px; height:32px; border:none;"
                      :disabled="!canJumpToStep(orderDetailData, step.id)"
                      :title="canJumpToStep(orderDetailData, step.id) ? `Chuyển sang &quot;${step.title}&quot;` : ''"
                      :style="isStepReached(orderDetailData, step.id)
                        ? isStepDoneById(orderDetailData, step.id)
                          ? 'background:var(--accent); border:2px solid var(--accent); cursor:default;'
                          : 'background:var(--bg-hover); border:2px solid var(--accent); box-shadow:0 0 0 4px rgba(244,63,94,0.18); cursor:pointer;'
                        : 'background:var(--bg-card-alt); border:2px solid var(--border-color-strong); cursor:pointer;'"
                      @click="jumpToStatus(orderDetailData, step.id)"
                    >
                      <Check v-if="isStepDoneById(orderDetailData, step.id)" :size="14" color="white" />
                      <component v-else :is="step.icon" :size="14" :style="{ opacity: canJumpToStep(orderDetailData, step.id) ? 1 : 0.35 }" />
                    </button>
                    <div
                      v-if="index < orderTimelineSteps.length - 1" style="width:2px; flex-grow:1; min-height:18px; margin-top:4px;"
                      :style="isStepReached(orderDetailData, orderTimelineSteps[index+1].id) ? 'background:var(--accent);' : 'background:var(--border-color-strong); opacity:0.4;'"
                    ></div>
                  </div>
                  <div class="flex-grow-1 pb-3" style="padding-top:4px;">
                    <div
                      class="fw-semibold" style="font-size:0.85rem; line-height:1.3;"
                      :style="isStepCurrentById(orderDetailData, step.id)
                        ? 'color:var(--accent-fg); font-weight:700;'
                        : isStepReached(orderDetailData, step.id) ? 'color:var(--text-primary);' : 'color:var(--text-secondary);'"
                    >
                      {{ step.title }}
                    </div>
                    <div style="font-size:0.72rem; color:var(--text-muted); line-height:1.35; margin-top:2px;">
                      {{ step.desc }}
                    </div>
                  </div>
                </div>
              </div>

              <!-- Nút chuyển bước tiếp theo nhanh dành riêng cho đơn hàng QR (8 bước) -->
              <div v-if="isQrPayment(orderDetailData) && !['cancelled','returned','delivered'].includes(orderDetailData.trangThaiDonHang)" class="mt-3">
                <button
                  v-if="orderDetailData.trangThaiThanhToan !== 'paid'"
                  type="button"
                  class="btn btn-sm btn-success fw-bold w-100 d-flex align-items-center justify-content-center gap-1.5 shadow-sm"
                  style="font-size:0.8rem; padding:7px 12px; background:#16a34a; border:none;"
                  :disabled="confirmingPayment"
                  @click="adminConfirmQrPayment(orderDetailData)"
                >
                  <CheckCircle2 :size="14" />
                  <span>{{ confirmingPayment ? 'Đang duyệt...' : 'Duyệt thanh toán QR (➔ Chờ xử lý)' }}</span>
                </button>
                <button
                  v-else-if="orderDetailData.trangThaiDonHang === 'pending'"
                  type="button"
                  class="btn btn-sm btn-primary fw-bold w-100 d-flex align-items-center justify-content-center gap-1.5 shadow-sm"
                  style="font-size:0.8rem; padding:7px 12px; background:#2563eb; border:none;"
                  @click="jumpToStatus(orderDetailData, 'da_len_don')"
                >
                  <FileText :size="14" />
                  <span>Xác nhận & Lên đơn (➔ Đã lên đơn)</span>
                </button>
                <button
                  v-else-if="orderDetailData.trangThaiDonHang === 'confirmed'"
                  type="button"
                  class="btn btn-sm text-white fw-bold w-100 d-flex align-items-center justify-content-center gap-1.5 shadow-sm"
                  style="font-size:0.8rem; padding:7px 12px; background:#9333ea; border:none;"
                  @click="jumpToStatus(orderDetailData, 'dang_dong_goi')"
                >
                  <Package :size="14" />
                  <span>Chuyển sang Đang đóng gói</span>
                </button>
                <button
                  v-else-if="orderDetailData.trangThaiDonHang === 'processing' || orderDetailData.trangThaiDonHang === 'shipping'"
                  type="button"
                  class="btn btn-sm btn-warning text-dark fw-bold w-100 d-flex align-items-center justify-content-center gap-1.5 shadow-sm"
                  style="font-size:0.8rem; padding:7px 12px; background:#f59e0b; border:none;"
                  @click="jumpToStatus(orderDetailData, 'dang_giao_hang')"
                >
                  <Bike :size="14" />
                  <span>Giao cho bên vận chuyển (➔ Đang giao hàng)</span>
                </button>
                <button
                  v-else-if="orderDetailData.trangThaiDonHang === 'out_for_delivery'"
                  type="button"
                  class="btn btn-sm text-white fw-bold w-100 d-flex align-items-center justify-content-center gap-1.5 shadow-sm"
                  style="font-size:0.8rem; padding:7px 12px; background:#0d9488; border:none;"
                  @click="jumpToStatus(orderDetailData, 'da_giao_cho_xac_nhan')"
                >
                  <Inbox :size="14" />
                  <span>Cập nhật Đã giao - Chờ xác nhận</span>
                </button>
                <button
                  v-else-if="orderDetailData.trangThaiDonHang === 'awaiting_confirmation'"
                  type="button"
                  class="btn btn-sm btn-success fw-bold w-100 d-flex align-items-center justify-content-center gap-1.5 shadow-sm"
                  style="font-size:0.8rem; padding:7px 12px; background:#16a34a; border:none;"
                  @click="jumpToStatus(orderDetailData, 'da_giao')"
                >
                  <CheckCircle2 :size="14" />
                  <span>Hoàn tất đơn hàng (➔ Đã giao)</span>
                </button>
              </div>

              <!-- Trường hợp đơn không phải QR ở trạng thái cuối (cancelled/returned/delivered) -->
              <div
                v-else-if="!isQrPayment(orderDetailData) && !NEXT_ORDER_STATUS[orderDetailData.trangThaiDonHang] && !['cancelled','returned','delivered'].includes(orderDetailData.trangThaiDonHang)"
                class="small text-secondary mt-3 text-center py-2 rounded-2" style="background:var(--bg-input);"
              >
                {{ t('admin.orderDetailModal.noNextStep') }}
              </div>
            </div>

            <!-- Thanh toán -->
            <div style="border-top:1px solid var(--border-color-soft); padding-top:12px;">
              <div class="text-secondary fw-bold mb-2" style="font-size:0.7rem; text-transform:uppercase; letter-spacing:0.06em;">
                {{ t('admin.orderDetailModal.paymentStatus') }}
              </div>
              <div class="alt-tag d-inline-flex align-items-center gap-1" :style="{ background: paymentStatusColor(orderDetailData.trangThaiThanhToan).bg, color: paymentStatusColor(orderDetailData.trangThaiThanhToan).text }">
                <component :is="paymentStatusIcon(orderDetailData.trangThaiThanhToan)" :size="13" />
                {{ orderDetailData.trangThaiThanhToan ? paymentStatusLabel(orderDetailData.trangThaiThanhToan) : '—' }}
              </div>
              <div v-if="orderDetailPayments.length" class="mt-2 small text-secondary">
                <span class="d-block mb-1">{{ t('admin.orderDetailModal.paymentMethod') }}:</span>
                <span style="color:var(--text-primary);">
                  <template v-for="(g, idx) in orderDetailPaymentsSummary" :key="g.method">
                    <component :is="paymentMethodIcon(g.method)" :size="13" style="vertical-align:-2px;" /> {{ paymentMethodLabel(g.method) }}<template v-if="g.count > 1"> ×{{ g.count }} ({{ formatPrice(g.total) }})</template><span v-if="idx < orderDetailPaymentsSummary.length - 1">, </span>
                  </template>
                </span>
              </div>
            </div>

            <!-- Ngày giao -->
            <div v-if="orderDetailData.ngayGiaoDuKien || orderDetailData.ngayGiaoThucTe" style="border-top:1px solid var(--border-color-soft); padding-top:12px;">
              <div class="text-secondary fw-bold mb-2" style="font-size:0.7rem; text-transform:uppercase; letter-spacing:0.06em;">
                {{ t('orderStatus.deliveryTitle') }}
              </div>
              <div v-if="orderDetailData.ngayGiaoDuKien" class="d-flex justify-content-between small">
                <span class="text-secondary">{{ t('admin.orderStatusModal.expectedDeliveryLabel') }}</span>
                <span style="color:var(--text-primary);">{{ formatDateTime(orderDetailData.ngayGiaoDuKien) }}</span>
              </div>
              <div v-if="orderDetailData.ngayGiaoThucTe" class="d-flex justify-content-between small mt-1">
                <span class="text-secondary">{{ t('admin.orderStatusModal.actualDeliveryLabel') }}</span>
                <span class="text-success fw-semibold">{{ formatDateTime(orderDetailData.ngayGiaoThucTe) }}</span>
              </div>
            </div>

            <!-- Mã vận đơn -->
            <div v-if="orderDetailData.maVanDon" style="border-top:1px solid var(--border-color-soft); padding-top:12px;">
              <div class="text-secondary fw-bold mb-2" style="font-size:0.7rem; text-transform:uppercase; letter-spacing:0.06em;">
                {{ t('admin.orderStatusModal.trackingCodeLabel') }}
              </div>
              <div class="fw-semibold" style="font-family:monospace; color:var(--text-primary);">{{ orderDetailData.maVanDon }}</div>
            </div>

            <!-- Thông tin khách hàng & Giao hàng -->
            <div style="border-top:1px solid var(--border-color-soft); padding-top:12px;">
              <div class="text-secondary fw-bold mb-2" style="font-size:0.7rem; text-transform:uppercase; letter-spacing:0.06em;">
                Thông tin khách hàng & Giao hàng
              </div>
              <div class="d-flex flex-column gap-2 small">
                <!-- Khách hàng / liên hệ -->
                <div class="p-2 rounded-2" style="background:var(--bg-input); border:1px solid var(--border-color-soft);">
                  <div class="d-flex align-items-center gap-1.5 fw-semibold" style="color:var(--text-primary); font-size:0.82rem;">
                    <User :size="13" class="text-primary flex-shrink-0" />
                    <span>{{ customerName(orderDetailData.khachHangId) }}</span>
                  </div>
                  <div v-if="customerPhone(orderDetailData)" class="d-flex align-items-center gap-1.5 text-secondary mt-1" style="font-size:0.76rem;">
                    <Phone :size="11" class="flex-shrink-0" />
                    <span>{{ customerPhone(orderDetailData) }}</span>
                  </div>
                  <div v-if="customerEmail(orderDetailData)" class="d-flex align-items-center gap-1.5 text-secondary mt-0.5" style="font-size:0.76rem;">
                    <Mail :size="11" class="flex-shrink-0" />
                    <span class="text-truncate">{{ customerEmail(orderDetailData) }}</span>
                  </div>
                </div>

                <!-- Địa chỉ giao hàng & Người nhận -->
                <div class="p-2 rounded-2" style="background:var(--bg-input); border:1px solid var(--border-color-soft);">
                  <div class="text-secondary fw-semibold mb-1 d-flex align-items-center gap-1" style="font-size:0.68rem; text-transform:uppercase; letter-spacing:0.04em;">
                    <MapPin :size="11" class="text-danger" /> Địa chỉ giao hàng:
                  </div>
                  <div v-if="orderDetailData.nguoiNhan && orderDetailData.nguoiNhan !== customerName(orderDetailData.khachHangId)" class="small mb-1" style="font-size:0.76rem; color:var(--text-secondary);">
                    Người nhận: <strong style="color:var(--text-primary);">{{ orderDetailData.nguoiNhan }}</strong>
                  </div>
                  <div v-if="orderDetailData.sdtNguoiNhan && orderDetailData.sdtNguoiNhan !== customerPhone(orderDetailData)" class="small text-secondary mb-1" style="font-size:0.76rem;">
                    SĐT nhận: <strong>{{ orderDetailData.sdtNguoiNhan }}</strong>
                  </div>
                  <div style="font-size:0.78rem; color:var(--text-primary); line-height:1.4;">
                    {{ deliveryAddressText(orderDetailData) }}
                  </div>
                  <div v-if="orderDetailData.ghiChu" class="mt-1 pt-1 border-top border-secondary small text-secondary" style="font-size:0.73rem;">
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

  <!-- ══ MODAL NHẬP MÃ VẬN ĐƠN (chỉ dùng khi chuyển sang 'shipping' từ timeline-click) ══ -->
  <div v-if="showOrderModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="showOrderModal=false">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:460px;max-width:95vw;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>{{ t('admin.orderStatusModal.trackingCodeTitle') }}</span>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="showOrderModal=false"></button>
      </div>
      <div class="p-4">
        <div v-if="orderStatusError" class="alert alert-danger small py-2 mb-3">{{ orderStatusError }}</div>
        <div v-if="editingOrder" class="small p-2 rounded-2 mb-3 text-secondary" style="background:var(--bg-hover);">
          {{ t('admin.orderStatusModal.orderPrefix') }}{{ editingOrder.donHangId }} — {{ t('admin.orderStatusModal.customerLabel') }} <strong>{{ customerName(editingOrder.khachHangId) }}</strong>
        </div>
        <label class="form-label small text-secondary">{{ t('admin.orderStatusModal.trackingCodeLabel') }}</label>
        <input v-model="orderStatusForm.maVanDon" type="text" class="form-control form-control-sm" :placeholder="t('admin.orderStatusModal.trackingCodePlaceholder')" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" />
      </div>
      <div class="d-flex justify-content-end gap-2 p-3 border-top border-secondary">
        <button class="btn btn-sm btn-outline-secondary" @click="showOrderModal=false">{{ t('admin.orderStatusModal.cancel') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" :disabled="orderStatusSaving" @click="saveOrderStatus">{{ t('admin.orderStatusModal.save') }}</button>
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
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="showXacNhanSerialModal=false"></button>
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
            <button
              v-for="s in xacNhanAvailableSerials(line)" :key="s.chiTietId"
              class="btn btn-sm"
              :class="line.chosenSerialIds.has(s.chiTietId) ? 'btn-warning text-dark' : 'btn-outline-secondary'"
              style="font-family:monospace;font-size:0.75rem;"
              @click="xacNhanToggleSerial(line, s.chiTietId)"
            >
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
  display: inline-flex; align-items: center; gap: 5px;
  padding: 6px 12px; border: 1px solid var(--border-color, #e2e8f0);
  border-radius: 8px; background: var(--bg-card, #fff);
  color: var(--text-primary, #1e293b); font-size: 13px; font-weight: 500; cursor: pointer; transition: all 0.15s ease;
  flex-shrink: 0;
}
.alt-btn--filter:hover, .alt-btn--filter-active {
  border-color: var(--pink-400, #f472b6); background: var(--pink-50, #fdf2f8); color: var(--pink-700, #be185d);
}
.filter-badge {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 18px; height: 18px; padding: 0 5px; border-radius: 9px;
  background: var(--pink-600, #db2777); color: #fff; font-size: 11px; font-weight: 700;
}
.alt-btn--ghost-sm {
  display: inline-flex; align-items: center; gap: 4px; padding: 5px 10px;
  border: 1px solid var(--border-color, #e2e8f0); border-radius: 8px;
  background: transparent; color: var(--text-secondary, #64748b); font-size: 12px; cursor: pointer; transition: all 0.15s; flex-shrink: 0;
}
.alt-btn--ghost-sm:hover { background: #fee2e2; color: #dc2626; border-color: #dc2626; }
.adv-filter-panel {
  border-top: 1px solid var(--border-color, #e2e8f0); background: var(--bg-card-alt, #f8fafc);
  padding: 12px 16px; animation: slideDown 0.15s ease;
}
@keyframes slideDown { from { opacity:0; transform:translateY(-6px); } to { opacity:1; transform:translateY(0); } }
.adv-filter-row { display: flex; flex-wrap: wrap; align-items: flex-end; gap: 12px; }
.adv-filter-group { display: flex; flex-direction: column; gap: 4px; min-width: 140px; }
.adv-filter-group--range { min-width: 240px; }
.adv-filter-label { font-size: 11px; font-weight: 600; color: var(--text-secondary, #64748b); text-transform: uppercase; letter-spacing: 0.04em; }
.adv-filter-select, .adv-filter-input {
  padding: 6px 10px; border: 1px solid var(--border-color, #e2e8f0); border-radius: 7px;
  background: var(--bg-input, #fff); color: var(--text-primary, #1e293b); font-size: 13px; outline: none; transition: border-color 0.15s; width: 100%;
}
.adv-filter-select:focus, .adv-filter-input:focus { border-color: var(--pink-500, #ec4899); }
.adv-filter-range { display: flex; align-items: center; gap: 6px; }
.adv-filter-range .adv-filter-input { width: 100px; }
.adv-filter-sep { color: var(--text-secondary, #94a3b8); font-size: 13px; font-weight: 600; }
.adv-filter-reset {
  display: inline-flex; align-items: center; gap: 5px; padding: 6px 12px;
  border: 1px solid #dc2626; border-radius: 7px; background: transparent; color: #dc2626;
  font-size: 12px; font-weight: 500; cursor: pointer; align-self: flex-end; transition: all 0.15s;
}
.adv-filter-reset:hover { background: #dc2626; color: #fff; }
</style>

