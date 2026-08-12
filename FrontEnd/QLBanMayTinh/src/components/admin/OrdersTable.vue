<script setup>
import { ref, reactive, computed, onMounted } from "vue";
import { t } from "../../i18n/index.js";
import { orderStatusLabel, orderStatusColor, orderStatusIcon, paymentStatusLabel, paymentStatusColor, paymentStatusIcon, paymentMethodLabel, paymentMethodIcon } from "../../utils/orderStatus.js";
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
import { CheckCircle2, Package, Truck, Bike, Inbox, Laptop, User } from '@lucide/vue';

onMounted(() => { ensureOrders(); ensureCustomers(); ensureProducts(); });

// ── Helpers ───────────────────────────────────────────────────────────────────
const customerName = (id) =>
  (CustomersStore.items ?? []).find((c) => c.khachHangId === id)?.hoTen ?? `KH#${id}`;

// Ngày dạng YYYY-MM-DD cho input[type=date] / so sánh — bản sao cục bộ của cùng hàm
// ở AdminPage.vue (dùng chung ở Dashboard/Reports, không đáng promote lên module chung
// chỉ vì 1 hàm thuần 3 dòng).
const toDateInputValue = (d) => {
  const y = d.getFullYear(), m = String(d.getMonth() + 1).padStart(2, '0'), day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
};

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
    if (orderStatusFilter.value && o.trangThaiDonHang !== orderStatusFilter.value) return false;
    if (orderPaymentFilter.value && o.trangThaiThanhToan !== orderPaymentFilter.value) return false;
    if (!q) return true;
    const name = customerName(o.khachHangId).toLowerCase();
    return String(o.donHangId).includes(q) || (o.maDonHang ?? '').toLowerCase().includes(q) || name.includes(q) || (o.nguoiNhan ?? '').toLowerCase().includes(q) || (o.sdtNguoiNhan ?? '').includes(q);
  });
});
const { currentPage, totalPages, pagedItems: pagedOrders, pageSize } = usePagination(filteredOrders);

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

// Helper: fetch serial của nhiều bienTheId song song → { bienTheId: serial[] } — dùng bởi
// openXacNhanSerialModal() (xác nhận đơn online). ProductDetailModal.vue có bản sao riêng
// của hàm này cho luồng "xem chi tiết sản phẩm" — 2 luồng độc lập, không đáng gộp.
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

const openOrderDetail = async (o) => {
  orderDetailData.value  = o;
  orderDetailItems.value = [];
  orderDetailPayments.value = [];
  showOrderDetailModal.value = true;
  orderDetailLoading.value = true;
  try {
    orderDetailItems.value = await ChiTietDonHangService.getByDonHang(o.donHangId).catch(() => []);
    orderDetailPayments.value = await ThanhToanService.getByDonHang(o.donHangId).catch(() => []);
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

// ── Gop don hang ─────────────────────────────────────────────────────────────
const mergeLoading = ref(false);

// Don hang cung khach, cung ngay dat (khong tinh gio), khac don hien tai — loai don
// "pending" vi backend tu choi gop don chua xac nhan (xem mergeOrders() service),
// hien nut gop voi ung vien chac chan fail se lam nguoi dung bam lai vo ich.
const mergeCandidates = computed(() => {
  if (!orderDetailData.value) return [];
  const curDate = orderDetailData.value.ngayDat?.slice(0, 10);
  return (OrdersStore.items ?? []).filter(o =>
    o.khachHangId === orderDetailData.value.khachHangId &&
    o.donHangId   !== orderDetailData.value.donHangId &&
    o.ngayDat?.slice(0, 10) === curDate &&
    o.trangThaiDonHang !== 'pending'
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
    if (!res.ok) { showToast(t('admin.errors.mergeFailed', { status: res.status, text: await res.text() })); return; }
    await refreshOrderDetail();
  } finally {
    mergeLoading.value = false;
  }
};

// ── Modal "Chi tiet san pham" (xem cac bien the da mua trong don nay) — dung lai ProductDetailModal.vue
const showDetailModal = ref(false);
const detailModalSanPhamId = ref(null);
const detailModalSanPhamName = ref('');
const detailModalBienTheIds = ref([]);

const openVariantDetail = (bienTheId) => {
  const v = productByBienThe(bienTheId);
  if (!v) return;
  detailModalSanPhamId.value = v.sanPhamId;
  detailModalSanPhamName.value = v.tenSanPham;
  // Khach co the mua cung 1 san pham nhung nhieu bien the khac nhau trong CUNG don nay
  // (vd may A bien the 1 va 2) — bam "Chi tiet" o dong nao cung phai hien du cac bien the
  // da mua trong don, khong chi dung dong vua bam.
  detailModalBienTheIds.value = [...new Set(
    orderDetailItems.value
      .map((item) => item.bienTheId)
      .filter((id) => productByBienThe(id)?.sanPhamId === v.sanPhamId)
  )];
  showDetailModal.value = true;
};

// ── Order status helpers (dùng chung — xem src/utils/orderStatus.js) ──────────

// ── Orders status update ──────────────────────────────────────────────────────
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
// Dựng body PUT /don-hang/update — dùng chung cho modal "Cập nhật trạng thái" (sửa tay,
// nhiều trường) và nút "next step" nhanh trên bảng (chỉ đổi trangThaiDonHang).
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

// Quy trình xử lý đơn thực tế: chờ xác nhận -> đã xác nhận -> đang đóng gói -> đã gửi
// hàng -> đang giao -> đã giao (chờ khách xác nhận) -> hoàn tất. Nút "bước tiếp theo" trên
// bảng đơn hàng đi đúng theo thứ tự này, khỏi phải mở modal chọn tay mỗi lần chỉ để nhích 1
// bước — mở modal vẫn dùng được cho các trường hợp khác (hủy đơn, sửa ngày giao...). Bước
// cuối "awaiting_confirmation -> delivered" KHÔNG có nút ở đây — chỉ khách hàng (hoặc staff
// qua modal "Cập nhật trạng thái") mới xác nhận được, xem AccountPage.vue confirmReceived().
const NEXT_ORDER_STATUS = {
  pending: 'confirmed', confirmed: 'processing', processing: 'shipping',
  shipping: 'out_for_delivery', out_for_delivery: 'awaiting_confirmation',
};
const NEXT_ORDER_STATUS_LABEL = {
  pending:          { icon: CheckCircle2, key: 'admin.orders.nextConfirm' },
  confirmed:        { icon: Package, key: 'admin.orders.nextPack' },
  processing:       { icon: Truck, key: 'admin.orders.nextShip' },
  shipping:         { icon: Bike, key: 'admin.orders.nextOutForDelivery' },
  out_for_delivery: { icon: Inbox, key: 'admin.orders.nextDelivered' },
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
  // Chuyển sang "Đã gửi hàng" bắt buộc dừng lại nhập mã vận đơn — mở modal thay vì 1-click.
  if (next === 'shipping') {
    openOrderStatus(o);
    orderStatusForm.trangThaiDonHang = 'shipping';
    return;
  }
  const body = buildOrderUpdateBody(o, {
    trangThaiDonHang: next,
    // Admin đánh dấu "đã giao" (awaiting_confirmation) mà thanh toán vẫn "chưa thanh toán" ->
    // tự chuyển "đã thanh toán" (đơn ở đây mặc định thu tiền khi giao — COD, tiền đã thu
    // xong tại thời điểm giao chứ không đợi khách bấm xác nhận). "partial"/"paid"/"refunded"
    // giữ nguyên, chỉ tự động hoá đúng 1 chiều unpaid -> paid, không đụng trạng thái staff
    // đã set tay.
    trangThaiThanhToan: next === 'awaiting_confirmation' && o.trangThaiThanhToan === 'unpaid'
      ? 'paid'
      : o.trangThaiThanhToan,
    ngayGiaoDuKien: o.ngayGiaoDuKien,
    // Chuyển sang "awaiting_confirmation" (shipper đã giao) mà chưa có ngày khách nhận hàng
    // -> tự đóng dấu thời điểm này ngay lúc đó, không đợi khách bấm "Xác nhận đã nhận hàng"
    // (có thể vài ngày sau) — hạn trả hàng 7 ngày phải tính từ lúc giao thật, không phải lúc
    // khách rảnh bấm xác nhận.
    ngayGiaoThucTe: next === 'awaiting_confirmation' && !o.ngayGiaoThucTe
      ? nowLocalIso()
      : o.ngayGiaoThucTe,
    maVanDon: o.maVanDon,
  });
  const res = await DonHangService.update(o.donHangId, body);
  if (!res.ok) { showToast(await res.text().catch(() => t('admin.errors.updateFailed', { status: res.status }))); return; }
  // Tải lại ngay thay vì tự ráp state cục bộ — chắc chắn đúng dữ liệu server, không phụ
  // thuộc việc SSE (chỉ để đồng bộ các tab/khách hàng khác) có tới kịp hay không.
  await refreshOrders();
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
  if (line.chosenSerialIds.has(serialId)) { line.chosenSerialIds.delete(serialId); return; }
  // Dòng chỉ cần 1 serial: bấm serial khác thay luôn cái đang chọn (kiểu radio) — nếu
  // không, đã chọn đủ 1/1 thì bấm serial khác không có tác dụng, phải bỏ tích cái cũ
  // trước mới chọn được cái mới.
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
    <div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
      <span class="fw-bold" style="color:var(--text-heading);">{{ t('admin.orders.history') }}</span>
      <button class="btn btn-sm btn-outline-secondary" @click="backToToday">{{ t('admin.orders.backToToday') }}</button>
    </div>
    <div v-if="OrdersStore.loading" class="text-secondary small">{{ t('admin.orders.loading') }}</div>
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
          <option value="out_for_delivery">{{ orderStatusLabel('out_for_delivery') }}</option>
          <option value="awaiting_confirmation">{{ orderStatusLabel('awaiting_confirmation') }}</option>
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
    <div v-if="OrdersStore.loading" class="text-secondary small">{{ t('admin.orders.loading') }}</div>
    <div v-else class="table-responsive">
      <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
        <thead><tr><th style="width:40px;">{{ t('admin.common.stt') }}</th><th>{{ t('admin.orders.colOrderCode') }}</th><th>{{ t('admin.orders.colCustomer') }}</th><th>{{ t('admin.orders.colTotal') }}</th><th>{{ t('admin.orders.colOrderStatus') }}</th><th>{{ t('admin.orders.colPaymentStatus') }}</th><th>{{ t('admin.orders.colOrderDate') }}</th><th>{{ t('admin.orders.colAction') }}</th></tr></thead>
        <tbody>
          <tr v-for="(o, idx) in pagedOrders" :key="o.donHangId">
            <td class="text-secondary">{{ currentPage * pageSize + idx + 1 }}</td>
            <td class="text-secondary">{{ o.maDonHang || ('#' + o.donHangId) }}</td>
            <td>{{ customerName(o.khachHangId) }}</td>
            <td>{{ formatPrice(o.thanhTien) }}</td>
            <td>
              <span class="badge d-inline-flex align-items-center gap-1" :style="{ background: orderStatusColor(o.trangThaiDonHang).bg, color: orderStatusColor(o.trangThaiDonHang).text }">
                <component :is="orderStatusIcon(o.trangThaiDonHang)" :size="13" /> {{ orderStatusLabel(o.trangThaiDonHang) }}
              </span>
            </td>
            <td>
              <span v-if="o.trangThaiThanhToan" class="badge d-inline-flex align-items-center gap-1" :style="{ background: paymentStatusColor(o.trangThaiThanhToan).bg, color: paymentStatusColor(o.trangThaiThanhToan).text }">
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
              <div class="d-flex gap-1">
                <button class="btn btn-sm btn-outline-info"    style="font-size:0.78rem;padding:2px 8px;" @click="openOrderDetail(o)">{{ t('admin.orders.detail') }}</button>
                <button v-if="NEXT_ORDER_STATUS[o.trangThaiDonHang]" class="btn btn-sm btn-outline-success" style="font-size:0.78rem;padding:2px 8px;" @click="advanceOrderStatus(o)">
                  <component :is="NEXT_ORDER_STATUS_LABEL[o.trangThaiDonHang].icon" :size="14" /> {{ t(NEXT_ORDER_STATUS_LABEL[o.trangThaiDonHang].key) }}
                </button>
                <button v-if="!['delivered','cancelled','returned'].includes(o.trangThaiDonHang)" class="btn btn-sm btn-outline-warning" style="font-size:0.78rem;padding:2px 8px;" @click="openOrderStatus(o)">{{ t('admin.orders.update') }}</button>
              </div>
            </td>
          </tr>
          <tr v-if="filteredOrders.length===0"><td colspan="8" class="text-center text-secondary">{{ t('admin.orders.empty') }}</td></tr>
        </tbody>
      </table>
      <Pagination :current-page="currentPage" :total-pages="totalPages" @page-change="currentPage = $event" />
    </div>
  </template>

  <!-- ══ MODAL THEM SAN PHAM CHI TIET ══ -->
  <div v-if="showAddItemDetailModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
       style="background:var(--bg-overlay);z-index:1070;" @click.self="showAddItemDetailModal=false">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card-inset);border:1px solid var(--border-color-strong);width:960px;max-width:97vw;max-height:93vh;">

      <!-- Header -->
      <div class="d-flex justify-content-between align-items-center px-4 py-3" style="border-bottom:1px solid var(--bg-input);">
        <span class="text-secondary" style="font-size:0.8rem;">{{ t('admin.addItemDetailModal.chooseProduct') }}</span>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="showAddItemDetailModal=false"></button>
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
              <span v-else><Laptop :size="64" color="var(--text-muted)" /></span>
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
          <div class="fw-bold" style="font-size:1.05rem;color:var(--text-heading);">
            <User :size="14" style="vertical-align:-2px;" /> {{ customerName(orderDetailData?.khachHangId) }}
          </div>
          <div class="text-secondary" style="font-size:0.78rem;">
            {{ t('admin.orderDetailModal.titlePrefix') }}{{ orderDetailData?.donHangId }}
            <span v-if="orderDetailData?.maDonHang" class="ms-1" style="font-family:monospace;">{{ orderDetailData.maDonHang }}</span>
            · {{ formatDate(orderDetailData?.ngayDat) }}
          </div>
        </div>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="showOrderDetailModal=false"></button>
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
                  <span v-else style="flex-shrink:0;"><Laptop :size="19" color="var(--text-muted)" /></span>
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
          <span class="badge d-inline-flex align-items-center gap-1" :style="{ background: orderStatusColor(orderDetailData.trangThaiDonHang).bg, color: orderStatusColor(orderDetailData.trangThaiDonHang).text }">
            <component :is="orderStatusIcon(orderDetailData.trangThaiDonHang)" :size="13" /> {{ orderStatusLabel(orderDetailData.trangThaiDonHang) }}
          </span>
        </div>
        <div class="d-flex justify-content-between small">
          <span class="text-secondary">{{ t('admin.orderDetailModal.paymentStatus') }}</span>
          <span class="badge d-inline-flex align-items-center gap-1" :style="{ background: paymentStatusColor(orderDetailData.trangThaiThanhToan).bg, color: paymentStatusColor(orderDetailData.trangThaiThanhToan).text }">
            <component :is="paymentStatusIcon(orderDetailData.trangThaiThanhToan)" :size="13" /> {{ orderDetailData.trangThaiThanhToan ? paymentStatusLabel(orderDetailData.trangThaiThanhToan) : '—' }}
          </span>
        </div>
        <div v-if="orderDetailPayments.length" class="d-flex justify-content-between small">
          <span class="text-secondary">{{ t('admin.orderDetailModal.paymentMethod') }}</span>
          <span style="color:var(--text-primary);">
            <template v-for="(p, idx) in orderDetailPayments" :key="p.thanhToanId">
              <component :is="paymentMethodIcon(p.phuongThucThanhToan)" :size="14" style="vertical-align:-2px;" /> {{ paymentMethodLabel(p.phuongThucThanhToan) }}<span v-if="idx < orderDetailPayments.length - 1">, </span>
            </template>
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
                  <span v-else><Laptop :size="29" color="var(--text-muted)" /></span>
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
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="showOrderModal=false"></button>
      </div>
      <div class="overflow-y-auto p-4">
        <div v-if="orderStatusError" class="alert alert-danger small py-2 mb-3">{{ orderStatusError }}</div>
        <div v-if="editingOrder" class="small p-2 rounded-2 mb-3 text-secondary" style="background:var(--bg-hover);">
          {{ t('admin.orderStatusModal.orderPrefix') }}{{ editingOrder.donHangId }} — {{ t('admin.orderStatusModal.customerLabel') }} <strong>{{ customerName(editingOrder.khachHangId) }}</strong>
        </div>
        <div class="d-flex flex-column gap-3">
          <div><label class="form-label small text-secondary">{{ t('admin.orderStatusModal.statusLabel') }}</label><select v-model="orderStatusForm.trangThaiDonHang" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)"><option value="pending">{{ t('admin.orderStatusModal.status.pending') }}</option><option value="confirmed">{{ t('admin.orderStatusModal.status.confirmed') }}</option><option value="processing">{{ t('admin.orderStatusModal.status.processing') }}</option><option value="shipping">{{ t('admin.orderStatusModal.status.shipping') }}</option><option value="out_for_delivery">{{ t('admin.orderStatusModal.status.out_for_delivery') }}</option><option value="awaiting_confirmation">{{ t('admin.orderStatusModal.status.awaiting_confirmation') }}</option><option value="delivered">{{ t('admin.orderStatusModal.status.delivered') }}</option><option value="cancelled">{{ t('admin.orderStatusModal.status.cancelled') }}</option><option value="returned">{{ t('admin.orderStatusModal.status.returned') }}</option></select></div>
          <div><label class="form-label small text-secondary">{{ t('admin.orderStatusModal.trackingCodeLabel') }}</label><input v-model="orderStatusForm.maVanDon" type="text" class="form-control form-control-sm" :placeholder="t('admin.orderStatusModal.trackingCodePlaceholder')" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
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

  <ProductDetailModal
    v-model="showDetailModal"
    :san-pham-id="detailModalSanPhamId"
    :san-pham-name="detailModalSanPhamName"
    :only-bien-the-ids="detailModalBienTheIds"
  />
</template>

<style scoped>
/* Bootstrap .text-light hardcode mau trang co dinh — ghi de theo theme hien tai (dung
   tren nen the/card, khong phai nen mau thuong hieu co dinh, nen an toan khi ghi de
   theo bien theme). Scoped rieng cho component nay vi CSS scoped khong ke thua qua bien
   gioi component. */
.text-light {
  color: var(--text-primary) !important;
}
</style>
