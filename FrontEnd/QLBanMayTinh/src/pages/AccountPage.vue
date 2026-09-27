<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import { AuthStore, setSession } from "../stores/index.js";
import { I18nStore, t } from "../i18n/index.js";
import { orderStatusLabel, orderStatusColor, orderStatusIcon, isQrPayment, getQrEffectiveStatus, getCodEffectiveStatus } from "../utils/orderStatus.js";
import { formatPrice as formatPriceRaw } from "../utils/formatPrice.js";
import * as DonHangService from "../services/DonHangService.js";
import * as ChiTietDonHangService from "../services/ChiTietDonHangService.js";
import * as SanPhamService from "../services/SanPhamService.js";
import * as KhachHangService from "../services/KhachHangService.js";
import * as LichSuDonHangService from "../services/LichSuDonHangService.js";
import * as PhieuTraHangService from "../services/PhieuTraHangService.js";
import * as DmDoiThuongService from "../services/DmDoiThuongService.js";
import * as PhieuGiamGiaCaNhanService from "../services/PhieuGiamGiaCaNhanService.js";
import * as YeuThichService from "../services/YeuThichService.js";
import * as PhieuBaoHanhService from "../services/PhieuBaoHanhService.js";
import OrderStatusTimeline from "../components/order/OrderStatusTimeline.vue";
import OrderTrackingLog from "../components/order/OrderTrackingLog.vue";
import ReturnRequestModal from "../components/order/ReturnRequestModal.vue";
import CustomerOrderDetailModal from "../components/order/CustomerOrderDetailModal.vue";
import ProductDetail from "../components/product/ProductDetail.vue";
import Skeleton from "../components/common/Skeleton.vue";
import LuckyWheelPanel from "../components/account/LuckyWheelPanel.vue";
import WarrantyTab from "../components/account/WarrantyTab.vue";
import WarrantyPlanTab from "../components/account/WarrantyPlanTab.vue";
import ChatWidget from "../components/account/ChatWidget.vue";
import {Clock,
  Truck,
  CheckCircle2,
  XCircle,
  Heart,
  Sparkles,
  Settings,
  ArrowLeft,
  User,
  Gift,
  Wallet,
  Package,
  ShoppingBag,
  Receipt,
  Laptop,
  History,
  RefreshCw,
  Undo2,
  AlertTriangle,
  Loader2,
  Smartphone,
  Mail,
  MapPin,
  Save,
  ShoppingCart,
  Home,
  Award,
  Tag,
  MapPin as Location,
  BookOpen,
  HelpCircle,
  FileText,
  LogOut,
  ChevronDown,
  ChevronRight,
  Star,
  CreditCard,
  Shield,
  X,
  Menu,
  ShieldPlus,
  Ticket,
  BadgeCheck,
  ShieldCheck,
  Phone,
  MessageCircle,
  Pencil,
  Copy,
  Check,
  Trophy,
  Zap,
  ExternalLink,
  QrCode,
  Banknote} from '@lucide/vue';

defineOptions({
  inheritAttrs: false,
});
const emit = defineEmits(["go-home", "logout", "add-to-cart", "buy-again-unavailable", "toast"]);
const router = useRouter();

const goHome = () => {
  emit("go-home");
  router.push("/");
};

const handleLogout = () => {
  emit("logout");
};

const auth = AuthStore;
const activeTab = ref('overview'); // Default to overview (no separate 'account' tab)

const warrantySubTab = ref('claims');
const warrantySubTabs = computed(() => [
  { id: 'claims', icon: Shield, label: 'Phiếu bảo hành', badge: warrantyClaimsCount.value },
  { id: 'plans',  icon: ShieldPlus, label: 'Gói bảo hành mở rộng' },
]);
const warrantyClaimsCount = ref(0);
const activeHistoryTab = ref("pending");
const sidebarOpen = ref(false);

const TAB_STATUS_GROUPS = {
  pending:   ["pending", "confirmed", "processing"],
  shipping:  ["shipping", "out_for_delivery", "awaiting_confirmation"],
  completed: ["delivered"],
  cancelled: ["cancelled", "returned"],
};

const hasActiveReturn = (donHangId) =>
  (returnsByOrder.value[donHangId] || []).some(r => r.trangThai === 'cho_xu_ly' || r.trangThai === 'da_xu_ly');

const orderTabId = (o) => {
  if (hasActiveReturn(o.donHangId)) return 'cancelled';
  for (const [tabId, statuses] of Object.entries(TAB_STATUS_GROUPS)) {
    if (statuses.includes(o.trangThaiDonHang)) return tabId;
  }
  return null;
};

const canRequestReturn = (o) => {
  if (o.trangThaiDonHang !== 'delivered' || hasActiveReturn(o.donHangId) || !o.ngayGiaoThucTe) return false;
  return Date.now() <= new Date(o.ngayGiaoThucTe).getTime() + 7 * 24 * 60 * 60 * 1000;
};

const RETURN_STATUS_LABEL = {
  cho_xu_ly: 'account.returnStatusPending',
  da_xu_ly: 'account.returnStatusDone',
  tu_choi: 'account.returnStatusRejected'
};
const RETURN_STATUS_COLOR = {
  cho_xu_ly: { bg: 'rgba(250,204,21,0.15)', text: '#facc15' },
  da_xu_ly:  { bg: 'rgba(34,197,94,0.15)',  text: '#22c55e' },
  tu_choi:   { bg: 'rgba(239,68,68,0.15)',  text: '#f87171' },
};

const SIDEBAR_MENU = computed(() => [
  { id: 'overview',  icon: Home,        label: 'Tổng quan',            desc: 'Đơn hàng gần đây & ưu đãi' },
  { id: 'history',   icon: History,     label: 'Lịch sử mua hàng',     desc: 'Đơn hàng đang xử lý và đã hoàn tất' },
  { id: 'warranty',  icon: Shield,      label: 'Bảo hành & Phiếu BH',  desc: 'Gửi yêu cầu và tra cứu', subTabs: 2 },
  { id: 'rewards',   icon: Gift,        label: 'Điểm thưởng & Voucher', desc: 'Đổi quà và theo dõi ưu đãi' },
  { id: 'settings',  icon: Settings,    label: 'Tài khoản & Hỗ trợ',   desc: 'Thông tin cá nhân & CSKH' },
]);

const HISTORY_TABS = computed(() => [
  { id: "pending", icon: Clock, label: t("account.tabPending") },
  { id: "shipping", icon: Truck, label: t("account.tabShipping") },
  { id: "completed", icon: CheckCircle2, label: t("account.tabCompleted") },
  { id: "cancelled", icon: XCircle, label: t("account.tabCancelled") },
]);

const orders      = ref([]);
const products    = ref([]);
const itemsByOrder = ref({});
const historyByOrder = ref({});
const returnsByOrder = ref({});
const returnModalOrder = ref(null);
const loading      = ref(false);
const ordersLoading = ref(false);

const tabOrderCounts = computed(() => {
  const counts = { history: 0, pending: 0, shipping: 0, completed: 0, cancelled: 0 };
  orders.value.forEach(o => {
    const tabId = orderTabId(o);
    if (tabId && tabId in counts) counts[tabId]++;
  });
  counts.history = orders.value.length;
  return counts;
});

const totalSpent = computed(() =>
  orders.value.reduce((total, order) => total + (Number(order.thanhTien ?? order.tongTien) || 0), 0),
);

const currentOrders = computed(() => {
  return activeTab.value === 'history'
    ? orders.value.filter(o => orderTabId(o) === activeHistoryTab.value && ['pending', 'shipping'].includes(activeHistoryTab.value))
    : [];
});

const historyOrders = computed(() => {
  return activeTab.value === 'history'
    ? orders.value.filter(o => orderTabId(o) === activeHistoryTab.value && ['completed', 'cancelled'].includes(activeHistoryTab.value))
    : [];
});

const productByBienThe = (bienTheId) =>
  products.value.find(p => p.bienTheId === bienTheId);

const expandedHistoryOrders = ref(new Set());
const toggleHistoryOrder = (donHangId) => {
  const s = expandedHistoryOrders.value;
  if (s.has(donHangId)) s.delete(donHangId); else s.add(donHangId);
  expandedHistoryOrders.value = new Set(s);
};

const selectedProductDetail = ref(null);
const viewProductDetail = (item) => {
  const product = productByBienThe(item.bienTheId);
  if (product) selectedProductDetail.value = product;
}

const selectedOrderDetail = ref(null);
const viewOrderDetail = async (order) => {
  selectedOrderDetail.value = order;
  const oId = order?.donHangId || order?.id;
  if (oId) {
    if (!historyByOrder.value[oId]) {
      try {
        const logs = await LichSuDonHangService.getByDonHang(oId);
        historyByOrder.value[oId] = logs || [];
      } catch {
        historyByOrder.value[oId] = [];
      }
    }
    if (!itemsByOrder.value[oId]) {
      try {
        const its = await ChiTietDonHangService.getByDonHang(oId);
        itemsByOrder.value[oId] = its || [];
      } catch {
        itemsByOrder.value[oId] = [];
      }
    }
  }
};

const handleOrderUpdated = (updatedOrder) => {
  if (!updatedOrder) return;
  const oId = updatedOrder.donHangId || updatedOrder.id;
  if (selectedOrderDetail.value && (selectedOrderDetail.value.donHangId === oId || selectedOrderDetail.value.id === oId)) {
    selectedOrderDetail.value = { ...selectedOrderDetail.value, ...updatedOrder };
  }
  const idx = orders.value.findIndex(o => (o.donHangId === oId || o.id === oId));
  if (idx !== -1) {
    orders.value[idx] = { ...orders.value[idx], ...updatedOrder };
  }
  fetchData();
};

const buyAgainOrder = (o) => {
  const khongMuaLaiDuoc = [];
  for (const item of itemsByOrder.value[o.donHangId] || []) {
    const product = productByBienThe(item.bienTheId);
    if (product && product.trangThai !== 'inactive') {
      emit("add-to-cart", product, item.soLuong);
    } else {
      khongMuaLaiDuoc.push(product?.tenSanPham || item.maSku);
    }
  }
  if (khongMuaLaiDuoc.length > 0) emit("buy-again-unavailable", khongMuaLaiDuoc);
};

const wishlistItems = ref([]);
const wishlistLoading = ref(false);

const loadWishlistItems = async () => {
  wishlistLoading.value = true;
  try {
    wishlistItems.value = await YeuThichService.getAll();
  } catch {
    wishlistItems.value = [];
  } finally {
    wishlistLoading.value = false;
  }
};

const removeWishlistItem = async (item) => {
  await YeuThichService.remove(item.bienTheId);
  wishlistItems.value = wishlistItems.value.filter((i) => i.bienTheId !== item.bienTheId);
};

const addWishlistItemToCart = (item) => {
  if (item.trangThai !== 'active' || (item.soLuongTon ?? 0) <= 0) return;
  emit("add-to-cart", item, 1);
};

const wishlistIdSet = computed(() => new Set(wishlistItems.value.map((i) => i.bienTheId)));

const toggleWishlistInDetail = async (variant) => {
  const daThich = wishlistIdSet.value.has(variant.bienTheId);
  try {
    if (daThich) {
      await YeuThichService.remove(variant.bienTheId);
      wishlistItems.value = wishlistItems.value.filter((i) => i.bienTheId !== variant.bienTheId);
    } else {
      await YeuThichService.add(variant.bienTheId);
      await loadWishlistItems();
    }
  } catch (e) {
    emit("toast", e.message, "error");
  }
};

const confirmingOrderId = ref(null);
const confirmReceived = async (o) => {
  if (confirmingOrderId.value) return;
  confirmingOrderId.value = o.donHangId;
  try {
    const res = await DonHangService.xacNhanDaNhanHang(o.donHangId);
    if (!res.ok) {
      emit("toast", await res.text().catch(() => t("account.confirmReceivedFailed")), "error");
      return;
    }
    emit("toast", t("account.confirmReceivedSuccess"), "success");
    await fetchData();
    if (selectedOrderDetail.value?.donHangId === o.donHangId) {
      const updated = orders.value.find(ord => ord.donHangId === o.donHangId);
      if (updated) selectedOrderDetail.value = updated;
    }
  } finally {
    confirmingOrderId.value = null;
  }
};

const fetchData = async () => {
  loading.value = true;
  try {
    const [myOrders, allProducts] = await Promise.all([
      DonHangService.getByKhachHang(auth.user?.id).catch(() => []),
      SanPhamService.getAll().catch(() => []),
    ]);
    products.value = allProducts;
    orders.value = myOrders.sort((a, b) => new Date(b.ngayDat) - new Date(a.ngayDat));

    const [entries, historyEntries] = await Promise.all([
      Promise.all(
        orders.value.map(async (o) => [
          o.donHangId,
          await ChiTietDonHangService.getByDonHang(o.donHangId).catch(() => []),
        ])
      ),
      Promise.all(
        orders.value.map(async (o) => [
          o.donHangId,
          ["shipping", "out_for_delivery", "awaiting_confirmation", "delivered"].includes(o.trangThaiDonHang)
            ? await LichSuDonHangService.getByDonHang(o.donHangId).catch(() => [])
            : [],
        ])
      ),
    ]);
    itemsByOrder.value = Object.fromEntries(entries);
    historyByOrder.value = Object.fromEntries(historyEntries);

    const returnEntries = await Promise.all(
      orders.value.map(async (o) => [
        o.donHangId,
        await PhieuTraHangService.getByDonHang(o.donHangId).catch(() => []),
      ])
    );
    returnsByOrder.value = Object.fromEntries(returnEntries);
  } finally {
    loading.value = false;
  }
};

const formatPrice = (v) => (v == null ? "—" : formatPriceRaw(v));
const formatPriceCompact = (v) => {
  if (v == null) return "—";
  const n = Number(v) || 0;
  if (n >= 1_000_000_000) return (n / 1_000_000_000).toFixed(1) + ' tỷ';
  if (n >= 1_000_000) return (n / 1_000_000).toFixed(1) + ' tr';
  return formatPriceRaw(n);
};
const formatDate = (d) => {
  if (!d) return "—";
  try { return new Date(d).toLocaleString(I18nStore.locale); } catch { return d; }
};

const profile        = ref(null);
const profileLoading = ref(false);
const profileSaving  = ref(false);
const profileError   = ref("");
const profileSuccess = ref("");
const profileForm = ref({ hoTen: "", soDienThoai: "", email: "", diaChi: "" });

// Rewards & Vouchers State
const rewards         = ref([]);
const myVouchers       = ref([]);
const redeemingId      = ref(null);
const redeemError      = ref("");
const voucherFilter    = ref("all"); // 'all' | 'active' | 'used' | 'expired'
const copiedCode       = ref(null);

const isVoucherExpired = (v) => {
  if (!v || !v.ngayHetHan) return false;
  return new Date(v.ngayHetHan) < new Date();
};

const isVoucherActive = (v) => {
  if (!v) return false;
  return !v.daSuDung && !isVoucherExpired(v);
};

const activeVouchersCount = computed(() => myVouchers.value.filter(isVoucherActive).length);
const usedVouchersCount   = computed(() => myVouchers.value.filter(v => v.daSuDung).length);
const expiredVouchersCount = computed(() => myVouchers.value.filter(v => !v.daSuDung && isVoucherExpired(v)).length);

const filteredVouchers = computed(() => {
  if (voucherFilter.value === 'active') return myVouchers.value.filter(isVoucherActive);
  if (voucherFilter.value === 'used') return myVouchers.value.filter(v => v.daSuDung);
  if (voucherFilter.value === 'expired') return myVouchers.value.filter(v => !v.daSuDung && isVoucherExpired(v));
  return myVouchers.value;
});

const activeRewardsCount = computed(() => rewards.value.filter(r => r.trangThai === 'active').length);

const currentTier = computed(() => {
  const pts = profile.value?.diemTichLuy || 0;
  if (pts >= 5000) {
    return { name: 'Kim Cương', color: '#06b6d4', badgeClass: 'tier-diamond', next: null, target: 5000, progress: 100, needed: 0 };
  }
  if (pts >= 2000) {
    return { name: 'Vàng', color: '#f59e0b', badgeClass: 'tier-gold', next: 'Kim Cương', target: 5000, progress: Math.min(100, Math.round(((pts - 2000) / 3000) * 100)), needed: 5000 - pts };
  }
  if (pts >= 500) {
    return { name: 'Bạc', color: '#94a3b8', badgeClass: 'tier-silver', next: 'Vàng', target: 2000, progress: Math.min(100, Math.round(((pts - 500) / 1500) * 100)), needed: 2000 - pts };
  }
  return { name: 'Đồng', color: '#cd7f32', badgeClass: 'tier-bronze', next: 'Bạc', target: 500, progress: Math.min(100, Math.round((pts / 500) * 100)), needed: 500 - pts };
});

const rewardsSubTab = ref('my-vouchers');
const rewardsSubTabs = computed(() => [
  { id: 'my-vouchers', icon: Ticket, label: 'Voucher của tôi', badge: activeVouchersCount.value || null },
  { id: 'redeem',      icon: Gift,   label: 'Đổi điểm thưởng', badge: activeRewardsCount.value ? `${activeRewardsCount.value} quà` : null },
  { id: 'wheel',       icon: Sparkles, label: 'Vòng quay may mắn' },
]);

const copyVoucherCode = async (code) => {
  if (!code) return;
  try {
    await navigator.clipboard.writeText(code);
    copiedCode.value = code;
    emit('toast', `Đã sao chép mã voucher ${code}`, 'success');
    setTimeout(() => {
      if (copiedCode.value === code) copiedCode.value = null;
    }, 2000);
  } catch {
    emit('toast', 'Không thể sao chép mã voucher', 'error');
  }
};

const useVoucherNow = async (v) => {
  if (!v || !v.maPhieu) return;
  await copyVoucherCode(v.maPhieu);
  emit('toast', `Mã ${v.maPhieu} đã được sao chép! Hãy áp dụng khi thanh toán nhé.`, 'info');
  setTimeout(() => {
    goHome();
  }, 350);
};

const formatVoucherDate = (d) => {
  if (!d) return 'Vô thời hạn';
  try {
    const dt = new Date(d);
    return `${String(dt.getDate()).padStart(2, '0')}/${String(dt.getMonth() + 1).padStart(2, '0')}/${dt.getFullYear()}`;
  } catch {
    return d;
  }
};

const formatVoucherTitle = (v) => {
  if (!v) return '';
  if (v.loai === 'percent' || v.loaiGiam === 'phan_tram') {
    let text = `Giảm ${v.giaTri}%`;
    if (v.giaTriToiDa && Number(v.giaTriToiDa) > 0) {
      text += ` (Tối đa ${formatPriceRaw(v.giaTriToiDa)})`;
    }
    return text;
  }
  return `Giảm ${formatPriceRaw(v.giaTri)}`;
};

const formatVoucherValue = (v) => {
  if (!v) return '';
  if (v.loai === 'percent' || v.loaiGiam === 'phan_tram') {
    return `${v.giaTri}%`;
  }
  return formatPriceRaw(v.giaTri);
};

// Warranty claim count for sub-tab badge
const fetchWarrantyClaims = async () => {
  try {
    const list = await PhieuBaoHanhService.getByKhachHang(auth.user?.id || 0);
    warrantyClaimsCount.value = (list || []).filter((c) => c.trangThai === 'cho_xu_ly' || c.trangThai === 'dang_xu_ly').length;
  } catch (e) {
    warrantyClaimsCount.value = 0;
  }
};

// Vouchers (backward compatibility with overview)
const vouchers = myVouchers;
const voucherCount = computed(() => activeVouchersCount.value);
const formatVoucher = (v) => formatVoucherTitle(v);
const loadVouchers = async () => {
  try {
    myVouchers.value = await PhieuGiamGiaCaNhanService.getCuaToi();
  } catch (err) {
    myVouchers.value = [];
  }
};

// BuyBack items (mock - can wire later)
const buyBackItems = ref([]);

const getOrderProductsName = (o) => {
  if (o.tenSanPham) return o.tenSanPham;
  if (o.sanPham && o.sanPham.length > 0) return o.sanPham[0].tenSanPham || '';
  const orderItems = itemsByOrder.value[o.donHangId];
  if (orderItems && orderItems.length > 0) {
    const p = productByBienThe(orderItems[0].bienTheId);
    const name = p?.tenSanPham || orderItems[0].tenSanPham || orderItems[0].maSku;
    if (name) {
      return orderItems.length > 1 ? `${name} (+${orderItems.length - 1} sản phẩm khác)` : name;
    }
  }
  return 'Đơn hàng';
};

const fetchProfile = async () => {
  if (!auth.user?.id) return;
  profileLoading.value = true;
  profileError.value = "";
  try {
    profile.value = await KhachHangService.getById(auth.user.id);
    profileForm.value = {
      hoTen:        profile.value.hoTen ?? "",
      soDienThoai:  profile.value.soDienThoai ?? "",
      email:        profile.value.email ?? "",
      diaChi:       profile.value.diaChi ?? "",
    };
    rewards.value = await DmDoiThuongService.getAll().catch(() => []);
    myVouchers.value = await PhieuGiamGiaCaNhanService.getCuaToi().catch(() => []);
  } catch (e) {
    profileError.value = e.message || t("account.settings.loadError");
  } finally {
    profileLoading.value = false;
  }
};

const redeemReward = async (r) => {
  redeemError.value = "";
  redeemingId.value = r.doiThuongId;
  try {
    const res = await PhieuGiamGiaCaNhanService.doiThuong(r.doiThuongId);
    if (!res.ok) {
      const errText = await res.text().catch(() => res.statusText);
      try {
        const j = JSON.parse(errText);
        redeemError.value = j.message || errText;
      } catch {
        redeemError.value = errText;
      }
      return;
    }
    myVouchers.value = await PhieuGiamGiaCaNhanService.getCuaToi().catch(() => []);
    await fetchProfile();
    emit('toast', `Đổi thành công mã giảm giá ${r.ten}!`, 'success');
    rewardsSubTab.value = 'my-vouchers';
    voucherFilter.value = 'active';
  } catch (e) {
    redeemError.value = e.message || t("account.rewards.redeemError");
  } finally {
    redeemingId.value = null;
  }
};

const onSpunWheel = async () => {
  await fetchProfile();
  myVouchers.value = await PhieuGiamGiaCaNhanService.getCuaToi().catch(() => []);
};

const isEditingProfile = ref(false);

const startEditProfile = () => {
  profileError.value = "";
  profileSuccess.value = "";
  if (profile.value) {
    profileForm.value = {
      hoTen:       profile.value.hoTen ?? "",
      soDienThoai: profile.value.soDienThoai ?? "",
      email:       profile.value.email ?? "",
      diaChi:      profile.value.diaChi ?? "",
    };
  }
  isEditingProfile.value = true;
};

const cancelEditProfile = () => {
  if (profile.value) {
    profileForm.value = {
      hoTen:       profile.value.hoTen ?? "",
      soDienThoai: profile.value.soDienThoai ?? "",
      email:       profile.value.email ?? "",
      diaChi:      profile.value.diaChi ?? "",
    };
  }
  profileError.value = "";
  isEditingProfile.value = false;
};

const saveProfile = async () => {
  if (!profile.value) return;
  profileSaving.value = true;
  profileError.value = "";
  profileSuccess.value = "";
  try {
    const body = {
      ...profile.value,
      hoTen:       profileForm.value.hoTen,
      soDienThoai: profileForm.value.soDienThoai,
      email:       profileForm.value.email,
      diaChi:      profileForm.value.diaChi,
    };
    const res = await KhachHangService.save(auth.user.id, body);
    if (!res.ok) throw new Error(`${t("account.settings.saveErrorPrefix")} ${res.status} ${await res.text()}`);
    profile.value = body;
    profileSuccess.value = t("account.settings.saveSuccess");
    setSession({ ...auth.user, hoTen: body.hoTen, email: body.email, soDienThoai: body.soDienThoai, diaChi: body.diaChi });
    isEditingProfile.value = false;
  } catch (e) {
    profileError.value = e.message || t("account.settings.saveErrorPrefix");
  } finally {
    profileSaving.value = false;
  }
};

watch(activeTab, (tab) => {
  if (tab === 'settings') {
    fetchProfile();
  }
});

watch(
  () => auth.user,
  (newUser) => {
    if (newUser) {
      if (profileForm.value) {
        if (newUser.hoTen) profileForm.value.hoTen = newUser.hoTen;
        if (newUser.soDienThoai) profileForm.value.soDienThoai = newUser.soDienThoai;
        if (newUser.email) profileForm.value.email = newUser.email;
        if (newUser.diaChi) profileForm.value.diaChi = newUser.diaChi;
      }
      fetchProfile();
    }
  },
  { deep: true }
);

const handleSidebarMenu = (item) => {
  activeTab.value = item.id;
  sidebarOpen.value = false;
};

let orderSse = null;
onMounted(() => {

  loadVouchers();
  fetchWarrantyClaims();
  fetchData();
  fetchProfile();
  loadWishlistItems();
  document.cookie = `sse_token=${encodeURIComponent(auth.user?.token ?? '')}; path=/api/don-hang; SameSite=Strict`;
  orderSse = new EventSource('/api/don-hang/events');
  orderSse.addEventListener('order-updated', () => { fetchData(); });
  document.addEventListener('click', handleOutsideClick);
});

onUnmounted(() => {
  if (orderSse) orderSse.close();
  document.cookie = 'sse_token=; path=/api/don-hang; expires=Thu, 01 Jan 1970 00:00:00 GMT; SameSite=Strict';
  document.removeEventListener('click', handleOutsideClick);
});

const handleOutsideClick = (e) => {
  if (sidebarOpen.value && !e.target.closest('.account-sidebar') && !e.target.closest('.mobile-menu-btn')) {
    sidebarOpen.value = false;
  }
};
</script>

<template>
  <div class="account-shell">
    <!-- HEADER: compact 1 row, ~ 1/3 chiều cao cũ, theme hồng admin -->
    <header class="account-header">
      <div class="container-xl">
        <div class="header-top">
          <div class="header-left">
            <button class="mobile-menu-btn d-lg-none" @click.stop="sidebarOpen = !sidebarOpen">
              <Menu :size="18" />
            </button>
            <button class="btn-back" @click="goHome" title="Về trang chủ">
              <ArrowLeft :size="16" />
            </button>
            <div class="brand-text" style="cursor:pointer;" @click="goHome" title="Về trang chủ">SAOClub</div>

            <div class="header-stats-compact d-none d-md-flex">
              <div class="stat-chip">
                <span class="stat-chip-icon"><Receipt :size="12" /></span>
                <span>{{ tabOrderCounts.history }} đơn</span>
              </div>
              <div class="stat-chip-divider"></div>
              <div class="stat-chip">
                <span class="stat-chip-icon"><Wallet :size="12" /></span>
                <span>{{ formatPriceCompact(totalSpent) }}</span>
              </div>
              <div class="stat-chip-divider"></div>
              <div class="stat-chip">
                <span class="stat-chip-icon"><Gift :size="12" /></span>
                <span>{{ myVouchers.length }} voucher</span>
              </div>
            </div>
          </div>

          <div class="header-right">
            <div class="user-compact d-none d-sm-flex">
              <div class="user-compact-avatar">
                {{ (auth.user?.hoTen || auth.user?.username || '?').charAt(0).toUpperCase() }}
              </div>
              <div class="user-compact-info">
                <div class="user-compact-name">{{ auth.user?.hoTen || auth.user?.username }}</div>
                <div class="user-compact-role"><Star :size="9" style="display:inline; vertical-align:-1px;" /> {{ profile?.diemTichLuy ?? 0 }} điểm</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </header>

    <div class="container-xl account-body">
      <div class="row g-4">
        <aside class="col-lg-3">
          <nav class="account-sidebar" :class="{ 'sidebar-open': sidebarOpen }">
            <div class="sidebar-header">
              <div class="sidebar-title"></div>
            </div>
            <div class="sidebar-menu">
              <button
                v-for="item in SIDEBAR_MENU" :key="item.id"
                class="sidebar-item"
                :class="{ 'is-active': item.id === activeTab }"
                @click="handleSidebarMenu(item)"
              >
                <div class="sidebar-item-icon"><component :is="item.icon" :size="18" /></div>
                <span class="sidebar-item-label">{{ item.label }}</span>
                <span v-if="item.badge" class="sidebar-item-badge">{{ item.badge }}</span>
              </button>
            </div>
            <div class="sidebar-footer">
              <button class="sidebar-item sidebar-item--logout" @click="handleLogout">
                <div class="sidebar-item-icon"><LogOut :size="18" /></div>
                <span class="sidebar-item-label">Đăng xuất</span>
              </button>
            </div>
          </nav>
        </aside>

        <main class="col-lg-9">
          <Transition name="panel-fade" mode="out-in">
            <div :key="activeTab" class="account-panel">

                            <!-- ══════════════ TAB: TỔNG QUAN ══════════════ -->
              <div v-if="activeTab === 'overview'" class="panel-section">
                <!-- Recent orders -->
                <div class="overview-card">
                  <div class="overview-card-header">
                    <Receipt :size="18" class="overview-card-icon" />
                    <h3 class="overview-card-title">Đơn hàng gần đây</h3>
                    <button class="overview-card-link" @click="activeTab = 'history'">Xem tất cả →</button>
                  </div>
                  <div v-if="ordersLoading" class="overview-loading">
                    <div v-for="i in 2" :key="i" class="skel-row"></div>
                  </div>
                  <div v-else-if="orders.length === 0" class="overview-empty">
                    <Package :size="32" class="overview-empty-icon" />
                    <span>Bạn chưa có đơn hàng nào</span>
                  </div>
                  <div v-else class="overview-order-list">
                    <div
                      v-for="o in orders.slice(0, 3)" :key="o.donHangId"
                      class="overview-order-row"
                      @click="viewOrderDetail(o)"
                    >
                      <div class="overview-order-info">
                        <div class="overview-order-line1">
                          <span class="overview-order-label">Đơn hàng:</span>
                          <strong class="overview-order-code">#{{ o.maDon || o.donHangId }}</strong>
                          <span class="overview-order-dot">•</span>
                          <span class="overview-order-date">Ngày đặt hàng: {{ formatDate(o.ngayDat || o.createdAt) }}</span>
                        </div>
                        <div class="overview-order-line2">
                          <span class="overview-order-product">{{ getOrderProductsName(o) }}</span>
                        </div>
                      </div>
                      <div class="overview-order-right">
                        <span
                          class="overview-order-status"
                          :style="isQrPayment(o)
                            ? { backgroundColor: getQrEffectiveStatus(o).color.bg, color: getQrEffectiveStatus(o).color.text, border: o.trangThaiThanhToan === 'unpaid' ? '1px solid #fed7aa' : 'none', fontWeight: '700' }
                            : { backgroundColor: getCodEffectiveStatus(o).color.bg, color: getCodEffectiveStatus(o).color.text, fontWeight: '700' }"
                        >
                          {{ isQrPayment(o) ? getQrEffectiveStatus(o).label : getCodEffectiveStatus(o).label }}
                        </span>
                        <div class="overview-order-total">Tổng thanh toán: <strong>{{ formatPriceRaw(o.tongThanhToan || o.tongTien) }}</strong></div>
                        <button class="overview-order-detail-btn" @click.stop="viewOrderDetail(o)">
                          Xem chi tiết
                          <ChevronRight :size="12" />
                        </button>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Vouchers + Loyalty -->
                <div class="overview-grid">
                  <!-- Vouchers -->
                  <div class="overview-card">
                    <div class="overview-card-header">
                      <Ticket :size="18" class="overview-card-icon" />
                      <h3 class="overview-card-title">Ưu đãi của bạn</h3>
                    </div>
                    <div v-if="voucherCount > 0" class="overview-voucher-summary">
                      <div class="overview-voucher-count">
                        <Sparkles :size="22" />
                        <span><strong>{{ voucherCount }}</strong> voucher đang có</span>
                      </div>
                      <button class="overview-voucher-btn" @click="activeTab = 'rewards'">Dùng ngay</button>
                    </div>
                    <div v-else class="overview-empty overview-empty--inline">
                      <Gift :size="28" class="overview-empty-icon" />
                      <span>Bạn chưa có ưu đãi nào.</span>
                      <button class="overview-empty-link" @click="activeTab = 'rewards'">Xem sản phẩm →</button>
                    </div>
                  </div>

                  <!-- Price Guarantee -->
                  <div class="overview-card">
                    <div class="overview-card-header">
                      <ShieldCheck :size="18" class="overview-card-icon" />
                      <h3 class="overview-card-title">Gói cam kết giá thu</h3>
                      <span class="overview-badge-pill"><BadgeCheck :size="11" /> S-BuyBack</span>
                    </div>
                    <div v-if="buyBackItems.length === 0" class="overview-empty overview-empty--inline">
                      <ShieldCheck :size="28" class="overview-empty-icon" />
                      <span>Bạn chưa có gói cam kết giá thu nào</span>
                      <button class="overview-empty-link">Tìm hiểu thêm →</button>
                    </div>
                    <div v-else class="overview-buyback">
                      <div v-for="b in buyBackItems.slice(0, 2)" :key="b.id" class="overview-buyback-row">
                        <strong>{{ b.tenSp }}</strong>
                        <span>Giá thu: <strong>{{ formatPriceRaw(b.giaThu) }}</strong></span>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Wishlist -->
                <div class="overview-card">
                  <div class="overview-card-header">
                    <Heart :size="18" class="overview-card-icon" />
                    <h3 class="overview-card-title">Sản phẩm yêu thích</h3>
                    <button class="overview-card-link" @click="activeTab = 'wishlist'">Xem tất cả →</button>
                  </div>
                  <div v-if="wishlistLoading" class="overview-loading">
                    <div v-for="i in 3" :key="i" class="skel-row"></div>
                  </div>
                  <div v-else-if="wishlistItems.length === 0" class="overview-empty">
                    <Heart :size="32" class="overview-empty-icon" />
                    <span>Bạn chưa thích sản phẩm nào</span>
                    <button class="overview-empty-link" @click="goHome">Khám phá ngay →</button>
                  </div>
                  <div v-else class="overview-wishlist-grid">
                    <div
                      v-for="w in wishlistItems.slice(0, 4)" :key="w.bienTheId"
                      class="overview-wishlist-card"
                      @click="goHome"
                    >
                      <div class="overview-wishlist-thumb">
                        <img v-if="w.hinhAnh" :src="w.hinhAnh" />
                        <Package v-else :size="32" />
                      </div>
                      <div class="overview-wishlist-name">{{ w.tenSanPham || w.tenBienThe }}</div>
                      <div class="overview-wishlist-price">{{ formatPriceRaw(w.giaBan) }}</div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- ══════════════ TAB: LỊCH SỬ MUA HÀNG ══════════════ -->
              <div v-else-if="activeTab === 'history'" class="panel-section">
                <div class="panel-header">
                  <div class="panel-header-content">
                    <History :size="22" class="panel-header-icon" />
                    <div>
                      <div class="panel-title">Lịch sử mua hàng</div>
                      <div class="panel-subtitle">{{ orders.length }} đơn hàng</div>
                    </div>
                  </div>
                </div>

                <div class="sub-tabs">
                  <button
                    v-for="tab in HISTORY_TABS" :key="tab.id"
                    class="sub-tab"
                    :class="{ 'is-active': activeHistoryTab === tab.id }"
                    @click="activeHistoryTab = tab.id"
                  >
                    <component :is="tab.icon" :size="14" />
                    <span>{{ tab.label }}</span>
                    <span v-if="tabOrderCounts[tab.id]" class="sub-tab-count">{{ tabOrderCounts[tab.id] }}</span>
                  </button>
                </div>

                <div class="panel-content">
                  <div v-if="loading" class="loading-state">
                    <div v-for="i in 3" :key="i"><Skeleton width="100%" height="80px" radius="16px" /></div>
                  </div>

                  <template v-else-if="['pending', 'shipping'].includes(activeHistoryTab)">
                    <div v-if="currentOrders.length === 0" class="empty-state">
                      <div class="empty-icon-wrap"><Package :size="48" /></div>
                      <div class="empty-title">Chưa có đơn hàng nào</div>
                      <div class="empty-text">Hãy tiếp tục mua sắm để tích lũy điểm thưởng nhé!</div>
                      <button class="btn-primary mt-4" @click="goHome">
                        <ShoppingBag :size="14" /> Bắt đầu mua sắm
                      </button>
                    </div>
                    <div v-else class="order-list">
                      <div v-for="o in currentOrders" :key="o.donHangId" class="order-card">
                        <div class="order-card-header">
                          <div class="order-info">
                            <div class="order-code">
                              <span class="order-code-label">Mã đơn</span>
                              <span class="order-code-value">{{ o.maDon || o.donHangId }}</span>
                              <span
                                v-if="isQrPayment(o)"
                                class="badge d-inline-flex align-items-center gap-1 ms-1.5"
                                style="background:#fff7ed; color:#ea580c; border:1px solid #fed7aa; font-size:0.68rem; font-weight:600; padding:2px 6px; border-radius:6px;"
                              >
                                <QrCode :size="10" /> VietQR
                              </span>
                              <span
                                v-else
                                class="badge d-inline-flex align-items-center gap-1 ms-1.5"
                                style="background:#f1f5f9; color:#475569; border:1px solid #cbd5e1; font-size:0.68rem; font-weight:600; padding:2px 6px; border-radius:6px;"
                              >
                                <Banknote :size="10" /> COD
                              </span>
                            </div>
                            <div class="order-date">{{ formatDate(o.ngayDat) }}</div>
                          </div>
                          <span
                            class="status-pill"
                            :style="isQrPayment(o)
                              ? { background: getQrEffectiveStatus(o).color.bg, color: getQrEffectiveStatus(o).color.text, border: o.trangThaiThanhToan === 'unpaid' ? '1px solid #fed7aa' : 'none' }
                              : { background: getCodEffectiveStatus(o).color.bg, color: getCodEffectiveStatus(o).color.text }"
                          >
                            <component :is="orderStatusIcon(o.trangThaiDonHang)" :size="12" />
                            {{ isQrPayment(o) ? getQrEffectiveStatus(o).label : getCodEffectiveStatus(o).label }}
                          </span>
                        </div>
                        <div class="order-timeline"><OrderStatusTimeline :status="o.trangThaiDonHang" :kenh-ban="o.kenhBan" :order="o" /></div>
                        <div class="order-products">
                          <div v-for="item in (itemsByOrder[o.donHangId] || []).slice(0, 2)" :key="item.id" class="order-product-mini" @click="viewProductDetail(item)">
                            <div class="product-thumb">
                              <img v-if="productByBienThe(item.bienTheId)?.hinhAnhChinh" :src="productByBienThe(item.bienTheId).hinhAnhChinh" />
                              <Laptop v-else :size="20" />
                            </div>
                            <div class="product-mini-info">
                              <div class="product-mini-name">{{ productByBienThe(item.bienTheId)?.tenSanPham || item.maSku }}</div>
                              <div class="product-mini-qty">x{{ item.soLuong }}</div>
                            </div>
                            <div class="product-mini-price">{{ formatPrice(item.thanhTien) }}</div>
                          </div>
                          <div v-if="(itemsByOrder[o.donHangId] || []).length > 2" class="products-more">+{{ (itemsByOrder[o.donHangId] || []).length - 2 }} sản phẩm khác</div>
                        </div>
                        <div class="order-card-footer">
                          <div class="order-total">
                            <span class="total-label">Tổng cộng</span>
                            <span class="total-value">{{ formatPrice(o.thanhTien ?? o.tongTien) }}</span>
                          </div>
                          <div class="order-actions">
                            <button
                              v-if="isQrPayment(o) && o.trangThaiThanhToan !== 'paid'"
                              class="btn btn-warning btn-sm text-white fw-bold d-inline-flex align-items-center gap-1"
                              style="background:linear-gradient(135deg, #ea580c 0%, #f97316 100%); border:none; padding:5px 12px; border-radius:20px; font-size:0.78rem;"
                              @click="viewOrderDetail(o)"
                            >
                              <CreditCard :size="12" /> Thanh toán QR
                            </button>
                            <button class="btn-outline-secondary btn-sm" @click="viewOrderDetail(o)">
                              <Receipt :size="13" /> Chi tiết đơn
                            </button>
                            <button v-if="['shipping', 'out_for_delivery', 'awaiting_confirmation'].includes(o.trangThaiDonHang)" class="btn-outline-danger btn-sm" :disabled="o.trangThaiDonHang !== 'awaiting_confirmation' || confirmingOrderId === o.donHangId" @click="confirmReceived(o)">
                              <CheckCircle2 :size="14" /> Đã nhận hàng
                            </button>
                          </div>
                        </div>
                        <OrderTrackingLog v-if="['shipping', 'out_for_delivery', 'awaiting_confirmation'].includes(o.trangThaiDonHang)" :ma-van-don="o.maVanDon || ''" :history="historyByOrder[o.donHangId] || []" />
                      </div>
                    </div>
                  </template>

                  <template v-else>
                    <div v-if="historyOrders.length === 0" class="empty-state">
                      <div class="empty-icon-wrap"><History :size="48" /></div>
                      <div class="empty-title">Không có đơn hàng nào</div>
                    </div>
                    <div v-else class="order-list">
                      <div v-for="o in historyOrders" :key="o.donHangId" class="order-card order-card--compact" :style="{ '--card-accent': orderStatusColor(o.trangThaiDonHang).text }">
                        <div class="compact-header" @click="toggleHistoryOrder(o.donHangId)">
                          <div class="compact-info">
                            <div class="order-code">
                              <span class="order-code-label">Mã đơn</span>
                              <span class="order-code-value">{{ o.maDon || o.donHangId }}</span>
                              <span
                                v-if="isQrPayment(o)"
                                class="badge d-inline-flex align-items-center gap-1 ms-1.5"
                                style="background:#fff7ed; color:#ea580c; border:1px solid #fed7aa; font-size:0.65rem; font-weight:600; padding:1px 5px; border-radius:4px;"
                              >
                                VietQR
                              </span>
                              <span
                                v-else
                                class="badge d-inline-flex align-items-center gap-1 ms-1.5"
                                style="background:#f1f5f9; color:#475569; border:1px solid #cbd5e1; font-size:0.65rem; font-weight:600; padding:1px 5px; border-radius:4px;"
                              >
                                COD
                              </span>
                            </div>
                            <div class="order-date">{{ formatDate(o.ngayDat) }} · {{ (itemsByOrder[o.donHangId] || []).length }} sp</div>
                          </div>
                          <div class="compact-right">
                            <span
                              class="status-pill"
                              :style="isQrPayment(o)
                                ? { background: getQrEffectiveStatus(o).color.bg, color: getQrEffectiveStatus(o).color.text, border: o.trangThaiThanhToan === 'unpaid' ? '1px solid #fed7aa' : 'none' }
                                : { background: getCodEffectiveStatus(o).color.bg, color: getCodEffectiveStatus(o).color.text }"
                            >
                              <component :is="orderStatusIcon(o.trangThaiDonHang)" :size="12" />
                              {{ isQrPayment(o) ? getQrEffectiveStatus(o).label : getCodEffectiveStatus(o).label }}
                            </span>
                            <span class="compact-total">{{ formatPrice(o.thanhTien ?? o.tongTien) }}</span>
                          </div>
                          <ChevronDown :size="18" class="expand-chevron" :class="{ 'is-open': expandedHistoryOrders.has(o.donHangId) }" />
                        </div>
                        <Transition name="slide-down">
                          <div v-if="expandedHistoryOrders.has(o.donHangId)" class="compact-detail">
                            <div class="detail-products">
                              <div v-for="item in itemsByOrder[o.donHangId] || []" :key="item.id" class="order-product-mini" @click="viewProductDetail(item)">
                                <div class="product-thumb">
                                  <img v-if="productByBienThe(item.bienTheId)?.hinhAnhChinh" :src="productByBienThe(item.bienTheId).hinhAnhChinh" />
                                  <Laptop v-else :size="20" />
                                </div>
                                <div class="product-mini-info">
                                  <div class="product-mini-name">{{ productByBienThe(item.bienTheId)?.tenSanPham || item.maSku }}</div>
                                  <div class="product-mini-qty">x{{ item.soLuong }}</div>
                                </div>
                                <div class="product-mini-price">{{ formatPrice(item.thanhTien) }}</div>
                              </div>
                            </div>
                            <div class="detail-actions">
                              <button class="btn-outline-secondary btn-sm" @click.stop="viewOrderDetail(o)"><Receipt :size="13" /> Chi tiết đơn</button>
                              <button v-if="o.trangThaiDonHang === 'delivered'" class="btn-outline-secondary btn-sm" @click.stop="buyAgainOrder(o)"><RefreshCw :size="13" /> Mua lại</button>
                              <button v-if="canRequestReturn(o)" class="btn-outline-danger btn-sm" @click.stop="returnModalOrder = o"><Undo2 :size="13" /> Đổi trả</button>
                            </div>
                            <OrderTrackingLog v-if="o.trangThaiDonHang === 'delivered'" :ma-van-don="o.maVanDon || ''" :history="historyByOrder[o.donHangId] || []" />
                            <div v-if="(returnsByOrder[o.donHangId] || []).length" class="returns-list">
                              <div v-for="r in returnsByOrder[o.donHangId]" :key="r.phieuTraId" class="return-chip">
                                <span class="status-pill status-pill--sm" :style="{ background: RETURN_STATUS_COLOR[r.trangThai]?.bg, color: RETURN_STATUS_COLOR[r.trangThai]?.text }">{{ t(RETURN_STATUS_LABEL[r.trangThai]) }}</span>
                                <span class="return-amount">{{ formatPrice(r.soTienHoan) }}</span>
                                <span class="return-reason">{{ r.lyDo }}</span>
                              </div>
                            </div>
                          </div>
                        </Transition>
                      </div>
                    </div>
                  </template>
                </div>
              </div>

              <!-- ══════════════ TAB: BẢO HÀNH (Phiếu + Gói mở rộng) ══════════════ -->
              <div v-else-if="activeTab === 'warranty'" class="panel-section">
                <div class="panel-header">
                  <div class="panel-header-content">
                    <Shield :size="22" class="panel-header-icon" />
                    <div>
                      <div class="panel-title">Bảo hành & Gói mở rộng</div>
                      <div class="panel-subtitle">Theo dõi phiếu BH hoặc đăng ký gói gia hạn</div>
                    </div>
                  </div>
                </div>

                <!-- Sub-tabs -->
                <div class="sub-tabs">
                  <button
                    v-for="t in warrantySubTabs" :key="t.id"
                    class="sub-tab"
                    :class="{ 'is-active': warrantySubTab === t.id }"
                    @click="warrantySubTab = t.id"
                  >
                    <component :is="t.icon" :size="14" />
                    {{ t.label }}
                    <span v-if="t.badge" class="sub-tab-badge">{{ t.badge }}</span>
                  </button>
                </div>

                <div class="panel-content">
                  <!-- Sub-tab 1: Phiếu bảo hành (WarrantyTab gốc) -->
                  <WarrantyTab v-show="warrantySubTab === 'claims'" @toast="(m, ty) => emit('toast', m, ty)" @view-product="viewProductDetail" />

                  <!-- Sub-tab 2: Gói bảo hành mở rộng (mới) -->
                  <WarrantyPlanTab v-show="warrantySubTab === 'plans'" @toast="(m, ty) => emit('toast', m, ty)" />
                </div>
              </div>

              <!-- ══════════════ TAB: ĐIỂM THƯỞNG & VOUCHER ══════════════ -->
              <div v-else-if="activeTab === 'rewards'" class="panel-section">
                <!-- Panel Header -->
                <div class="panel-header">
                  <div class="panel-header-content">
                    <Gift :size="22" class="panel-header-icon" />
                    <div>
                      <div class="panel-title">Điểm thưởng & Voucher</div>
                      <div class="panel-subtitle">Tích lũy điểm khi mua sắm, quản lý voucher cá nhân và đổi quà ưu đãi</div>
                    </div>
                  </div>
                </div>

                <!-- Loyalty Hero Card (Thẻ Điểm Thưởng & Hạng Thành Viên) -->
                <div class="loyalty-hero-card">
                  <div class="loyalty-hero-pattern"></div>
                  <div class="loyalty-hero-body">
                    <div class="loyalty-hero-main">
                      <div class="loyalty-tier-badge" :class="currentTier.badgeClass">
                        <Trophy :size="14" />
                        <span>Hạng {{ currentTier.name }}</span>
                      </div>
                      <div class="loyalty-pts-row">
                        <span class="loyalty-pts-val">{{ (profile?.diemTichLuy ?? 0).toLocaleString('vi-VN') }}</span>
                        <span class="loyalty-pts-unit">điểm tích lũy</span>
                      </div>

                      <div v-if="currentTier.next" class="loyalty-progress-box">
                        <div class="d-flex justify-content-between align-items-center mb-1 text-xs">
                          <span>Tiến độ lên hạng <strong>{{ currentTier.next }}</strong></span>
                          <span>Cần thêm <strong>{{ currentTier.needed.toLocaleString('vi-VN') }}</strong> điểm</span>
                        </div>
                        <div class="loyalty-progress-track">
                          <div class="loyalty-progress-fill" :style="{ width: currentTier.progress + '%' }"></div>
                        </div>
                      </div>
                      <div v-else class="loyalty-max-tier-note">
                        <Award :size="14" />
                        <span>Bạn đã đạt hạng thành viên cao nhất với đặc quyền VIP!</span>
                      </div>

                      <div class="loyalty-tip">
                        <Zap :size="13" class="text-amber" />
                        <span>Nhận 1 điểm với mỗi 1.000đ khi thanh toán đơn hàng thành công tại SAOClub.</span>
                      </div>
                    </div>

                    <div class="loyalty-hero-stats">
                      <div class="loyalty-stat-card" role="button" @click="rewardsSubTab = 'my-vouchers'">
                        <div class="loyalty-stat-icon loyalty-stat-icon--voucher">
                          <Ticket :size="20" />
                        </div>
                        <div class="loyalty-stat-info">
                          <div class="loyalty-stat-num">{{ activeVouchersCount }}</div>
                          <div class="loyalty-stat-lbl">Voucher khả dụng</div>
                        </div>
                      </div>

                      <div class="loyalty-stat-card" role="button" @click="rewardsSubTab = 'wheel'">
                        <div class="loyalty-stat-icon loyalty-stat-icon--wheel">
                          <Sparkles :size="20" />
                        </div>
                        <div class="loyalty-stat-info">
                          <div class="loyalty-stat-num">{{ Math.floor((profile?.diemTichLuy ?? 0) / 50) }}</div>
                          <div class="loyalty-stat-lbl">Lượt quay có thể đổi</div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Sub-tabs Navigation -->
                <div class="sub-tabs">
                  <button
                    v-for="t in rewardsSubTabs" :key="t.id"
                    class="sub-tab"
                    :class="{ 'is-active': rewardsSubTab === t.id }"
                    @click="rewardsSubTab = t.id"
                  >
                    <component :is="t.icon" :size="14" />
                    {{ t.label }}
                    <span v-if="t.badge" class="sub-tab-badge">{{ t.badge }}</span>
                  </button>
                </div>

                <div class="panel-content">
                  <!-- ════════════ Sub-tab 1: Voucher của tôi ════════════ -->
                  <div v-if="rewardsSubTab === 'my-vouchers'" class="tab-pane-rewards">
                    <!-- Filter Pills -->
                    <div class="voucher-filter-bar">
                      <button
                        type="button"
                        class="voucher-filter-btn"
                        :class="{ 'is-active': voucherFilter === 'all' }"
                        @click="voucherFilter = 'all'"
                      >
                        Tất cả ({{ myVouchers.length }})
                      </button>
                      <button
                        type="button"
                        class="voucher-filter-btn"
                        :class="{ 'is-active': voucherFilter === 'active' }"
                        @click="voucherFilter = 'active'"
                      >
                        Còn hiệu lực ({{ activeVouchersCount }})
                      </button>
                      <button
                        type="button"
                        class="voucher-filter-btn"
                        :class="{ 'is-active': voucherFilter === 'used' }"
                        @click="voucherFilter = 'used'"
                      >
                        Đã sử dụng ({{ usedVouchersCount }})
                      </button>
                      <button
                        type="button"
                        class="voucher-filter-btn"
                        :class="{ 'is-active': voucherFilter === 'expired' }"
                        @click="voucherFilter = 'expired'"
                      >
                        Hết hạn ({{ expiredVouchersCount }})
                      </button>
                    </div>

                    <!-- Empty State -->
                    <div v-if="filteredVouchers.length === 0" class="rewards-empty-box">
                      <div class="rewards-empty-icon-wrap">
                        <Ticket :size="40" />
                      </div>
                      <h4 class="rewards-empty-title">Chưa có voucher nào trong mục này</h4>
                      <p class="rewards-empty-desc">
                        Hãy dùng điểm tích lũy để đổi các phiếu giảm giá giá trị cao hoặc tham gia vòng quay may mắn!
                      </p>
                      <div class="d-flex gap-2 justify-content-center flex-wrap mt-3">
                        <button class="btn-reward-cta btn-reward-cta--solid" @click="rewardsSubTab = 'redeem'">
                          <Gift :size="14" />
                          <span>Đổi điểm lấy quà</span>
                        </button>
                        <button class="btn-reward-cta btn-reward-cta--outline" @click="rewardsSubTab = 'wheel'">
                          <Sparkles :size="14" />
                          <span>Quay thưởng</span>
                        </button>
                      </div>
                    </div>

                    <!-- Voucher Cards Grid -->
                    <div v-else class="my-voucher-grid">
                      <div
                        v-for="v in filteredVouchers"
                        :key="v.phieuId || v.maPhieu"
                        class="my-voucher-ticket"
                        :class="{ 'ticket--disabled': !isVoucherActive(v) }"
                      >
                        <!-- Ticket Left: Discount Value & Cutout -->
                        <div class="ticket-left">
                          <div class="ticket-discount-val">{{ formatVoucherValue(v) }}</div>
                          <div class="ticket-discount-tag">GIẢM GIÁ</div>
                          <div class="ticket-notch ticket-notch--top"></div>
                          <div class="ticket-notch ticket-notch--bottom"></div>
                        </div>

                        <!-- Ticket Center: Details -->
                        <div class="ticket-center">
                          <div class="ticket-badge-row">
                            <span v-if="v.nguon" class="ticket-source-badge">{{ v.nguon }}</span>
                            <span v-if="isVoucherActive(v)" class="ticket-status-pill ticket-status-pill--active">
                              Còn hiệu lực
                            </span>
                            <span v-else-if="v.daSuDung" class="ticket-status-pill ticket-status-pill--used">
                              Đã sử dụng
                            </span>
                            <span v-else class="ticket-status-pill ticket-status-pill--expired">
                              Hết hạn
                            </span>
                          </div>

                          <div class="ticket-title">{{ formatVoucherTitle(v) }}</div>

                          <div class="ticket-rules">
                            <div class="ticket-rule-item">
                              <span>Đơn tối thiểu:</span>
                              <strong>{{ v.donHangToiThieu && Number(v.donHangToiThieu) > 0 ? formatPriceRaw(v.donHangToiThieu) : 'Không giới hạn' }}</strong>
                            </div>
                            <div class="ticket-rule-item">
                              <span>Hạn sử dụng:</span>
                              <strong>{{ formatVoucherDate(v.ngayHetHan) }}</strong>
                            </div>
                          </div>
                        </div>

                        <!-- Ticket Right: Code & Action -->
                        <div class="ticket-right">
                          <div class="ticket-code-tag" :title="'Mã: ' + v.maPhieu">
                            {{ v.maPhieu }}
                          </div>

                          <div v-if="isVoucherActive(v)" class="ticket-actions">
                            <button
                              type="button"
                              class="btn-ticket-copy"
                              :class="{ 'is-copied': copiedCode === v.maPhieu }"
                              @click="copyVoucherCode(v.maPhieu)"
                              title="Sao chép mã"
                            >
                              <Check v-if="copiedCode === v.maPhieu" :size="13" />
                              <Copy v-else :size="13" />
                              <span>{{ copiedCode === v.maPhieu ? 'Đã chép' : 'Sao chép' }}</span>
                            </button>

                            <button
                              type="button"
                              class="btn-ticket-use"
                              @click="useVoucherNow(v)"
                              title="Sao chép và đi tới cửa hàng"
                            >
                              <span>Dùng ngay</span>
                              <ChevronRight :size="13" />
                            </button>
                          </div>

                          <div v-else class="ticket-inactive-msg">
                            {{ v.daSuDung ? 'Đã dùng' : 'Hết hạn' }}
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- ════════════ Sub-tab 2: Đổi điểm thưởng ════════════ -->
                  <div v-else-if="rewardsSubTab === 'redeem'" class="tab-pane-rewards">
                    <div class="redeem-intro-bar">
                      <div class="d-flex align-items-center gap-2">
                        <Gift :size="18" class="text-pink-500" />
                        <div>
                          <div class="fw-bold text-sm">Kho quà tặng đổi điểm</div>
                          <div class="text-xs text-secondary">
                            Đổi điểm thưởng lấy phiếu giảm giá áp dụng trực tiếp vào đơn hàng.
                          </div>
                        </div>
                      </div>
                      <div class="redeem-pts-indicator">
                        <span class="text-xs text-secondary">Điểm của bạn:</span>
                        <strong class="text-pink-600">{{ (profile?.diemTichLuy ?? 0).toLocaleString('vi-VN') }} điểm</strong>
                      </div>
                    </div>

                    <!-- Error Alert -->
                    <div v-if="redeemError" class="alert alert-danger d-flex align-items-center justify-content-between p-2.5 rounded-3 mb-3 text-sm">
                      <div class="d-flex align-items-center gap-2">
                        <AlertTriangle :size="16" />
                        <span>{{ redeemError }}</span>
                      </div>
                      <button type="button" class="btn-close btn-close-sm" @click="redeemError = ''"></button>
                    </div>

                    <!-- Redeem Grid -->
                    <div v-if="rewards.length === 0" class="rewards-empty-box">
                      <Gift :size="40" class="text-muted mb-2" />
                      <h4 class="rewards-empty-title">Hiện chưa có phần thưởng nào để đổi</h4>
                      <p class="rewards-empty-desc">Các quà tặng và voucher giảm giá sẽ sớm được cập nhật!</p>
                    </div>

                    <div v-else class="redeem-grid">
                      <div
                        v-for="r in rewards.filter(item => item.trangThai === 'active')"
                        :key="r.doiThuongId"
                        class="redeem-card"
                        :class="{ 'redeem-card--affordable': (profile?.diemTichLuy ?? 0) >= r.diemCan }"
                      >
                        <div class="redeem-card-badge">
                          <span class="redeem-val">{{ r.loai === 'percent' ? r.giaTri + '%' : formatPriceRaw(r.giaTri) }}</span>
                          <span class="redeem-sub">GIẢM</span>
                        </div>

                        <div class="redeem-card-body">
                          <div class="redeem-card-title">{{ r.ten }}</div>
                          <div class="redeem-card-desc">{{ r.moTa || 'Áp dụng cho mọi đơn hàng tại SAOClub' }}</div>
                          <div v-if="r.giaTriToiDa && Number(r.giaTriToiDa) > 0" class="redeem-card-max">
                            Giảm tối đa: <strong>{{ formatPriceRaw(r.giaTriToiDa) }}</strong>
                          </div>
                        </div>

                        <div class="redeem-card-footer">
                          <div class="redeem-cost">
                            <Star :size="14" class="text-amber fill-amber" />
                            <strong>{{ r.diemCan.toLocaleString('vi-VN') }}</strong>
                            <span>điểm</span>
                          </div>

                          <button
                            type="button"
                            class="btn-redeem"
                            :class="{ 'btn-redeem--active': (profile?.diemTichLuy ?? 0) >= r.diemCan }"
                            :disabled="(profile?.diemTichLuy ?? 0) < r.diemCan || redeemingId === r.doiThuongId"
                            @click="redeemReward(r)"
                          >
                            <Loader2 v-if="redeemingId === r.doiThuongId" :size="14" class="spin" />
                            <template v-else-if="(profile?.diemTichLuy ?? 0) >= r.diemCan">
                              <span>Đổi ngay</span>
                            </template>
                            <template v-else>
                              <span>Thiếu {{ (r.diemCan - (profile?.diemTichLuy ?? 0)).toLocaleString('vi-VN') }} điểm</span>
                            </template>
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- ════════════ Sub-tab 3: Vòng quay may mắn ════════════ -->
                  <div v-else-if="rewardsSubTab === 'wheel'" class="tab-pane-rewards">
                    <div class="wheel-panel-container">
                      <div class="wheel-panel-intro">
                        <Sparkles :size="20" class="text-pink-500" />
                        <div>
                          <div class="fw-bold text-sm">Vòng quay may mắn SAOClub</div>
                          <div class="text-xs text-secondary">
                            Mỗi lượt quay tốn 50 điểm tích lũy. Cơ hội nhận được các phiếu giảm giá siêu hấp dẫn!
                          </div>
                        </div>
                      </div>

                      <LuckyWheelPanel :points="profile?.diemTichLuy ?? 0" @spun="onSpunWheel" />
                    </div>
                  </div>
                </div>
              </div>

              <!-- ══════════════ TAB: TÀI KHOẢN & HỖ TRỢ ══════════════ -->
              <div v-else-if="activeTab === 'settings'" class="panel-section">
                <div class="panel-header">
                  <div class="panel-header-content">
                    <Settings :size="22" class="panel-header-icon" />
                    <div>
                      <div class="panel-title">Tài khoản & Hỗ trợ</div>
                      <div class="panel-subtitle">Thông tin cá nhân & Chăm sóc khách hàng</div>
                    </div>
                  </div>
                </div>

                <div class="panel-content">
                  <!-- Settings: profile -->
                  <div class="settings-section">
                    <div class="settings-section-header">
                      <div class="d-flex align-items-center gap-2">
                        <User :size="16" />
                        <span>Thông tin cá nhân</span>
                      </div>
                      <button
                        v-if="!isEditingProfile && !profileLoading"
                        type="button"
                        class="btn-edit-profile d-inline-flex align-items-center gap-1.5 px-3 py-1 rounded-pill"
                        @click="startEditProfile"
                      >
                        <Pencil :size="13" />
                        <span>Chỉnh sửa</span>
                      </button>
                    </div>

                    <div v-if="profileLoading" class="loading-state py-3">
                      <div v-for="i in 4" :key="i" class="mb-2"><Skeleton width="100%" height="42px" radius="10px" /></div>
                    </div>

                    <!-- Chế độ chỉ xem (View-only) -->
                    <div v-else-if="!isEditingProfile" class="profile-view-wrap py-1">
                      <div class="profile-view-grid">
                        <div class="profile-view-item">
                          <div class="profile-view-label">
                            <User :size="12" />
                            <span>Họ và tên</span>
                          </div>
                          <div class="profile-view-value fw-semibold">
                            {{ profileForm.hoTen || auth.user?.hoTen || 'Chưa cập nhật' }}
                          </div>
                        </div>

                        <div class="profile-view-item">
                          <div class="profile-view-label">
                            <Smartphone :size="12" />
                            <span>Số điện thoại</span>
                          </div>
                          <div class="profile-view-value fw-semibold">
                            {{ profileForm.soDienThoai || auth.user?.soDienThoai || 'Chưa cập nhật' }}
                          </div>
                        </div>

                        <div class="profile-view-item">
                          <div class="profile-view-label">
                            <Mail :size="12" />
                            <span>Email</span>
                          </div>
                          <div class="profile-view-value fw-semibold">
                            {{ profileForm.email || auth.user?.email || 'Chưa cập nhật' }}
                          </div>
                        </div>

                        <div class="profile-view-item">
                          <div class="profile-view-label">
                            <MapPin :size="12" />
                            <span>Địa chỉ</span>
                          </div>
                          <div class="profile-view-value fw-semibold">
                            {{ profileForm.diaChi || auth.user?.diaChi || 'Chưa cập nhật' }}
                          </div>
                        </div>
                      </div>
                    </div>

                    <!-- Chế độ chỉnh sửa (Edit mode) -->
                    <form v-else class="profile-form px-0 py-1" @submit.prevent="saveProfile">
                      <div class="form-row">
                        <div class="form-group">
                          <label class="form-label">Họ và tên</label>
                          <div class="input-with-icon">
                            <User :size="14" class="input-icon" />
                            <input v-model="profileForm.hoTen" type="text" required class="form-input" placeholder="Họ và tên..." />
                          </div>
                        </div>
                        <div class="form-group">
                          <label class="form-label">Số điện thoại</label>
                          <div class="input-with-icon">
                            <Smartphone :size="14" class="input-icon" />
                            <input v-model="profileForm.soDienThoai" type="tel" required class="form-input" placeholder="Số điện thoại..." />
                          </div>
                        </div>
                      </div>
                      <div class="form-row">
                        <div class="form-group">
                          <label class="form-label">Email</label>
                          <div class="input-with-icon">
                            <Mail :size="14" class="input-icon" />
                            <input v-model="profileForm.email" type="email" required class="form-input" placeholder="Email..." />
                          </div>
                        </div>
                        <div class="form-group">
                          <label class="form-label">Địa chỉ</label>
                          <div class="input-with-icon">
                            <MapPin :size="14" class="input-icon" />
                            <input v-model="profileForm.diaChi" type="text" required class="form-input" placeholder="Địa chỉ giao hàng..." />
                          </div>
                        </div>
                      </div>
                      <div v-if="profileError" class="alert-banner alert-banner--danger mb-3">
                        <AlertTriangle :size="14" /> {{ profileError }}
                      </div>
                      <div v-if="profileSuccess" class="alert-banner alert-banner--success mb-3">
                        <CheckCircle2 :size="14" /> {{ profileSuccess }}
                      </div>
                      <div class="form-actions d-flex align-items-center justify-content-end gap-2">
                        <button type="button" class="btn-outline-secondary" @click="cancelEditProfile">
                          Hủy
                        </button>
                        <button type="submit" class="btn-primary" :disabled="profileSaving">
                          <Loader2 v-if="profileSaving" :size="14" class="spin" />
                          <Save v-else :size="14" />
                          {{ profileSaving ? 'Đang lưu...' : 'Lưu thay đổi' }}
                        </button>
                      </div>
                    </form>
                  </div>


                  <!-- Support -->
                  <div class="settings-section">
                    <div class="settings-section-header">
                      <HelpCircle :size="16" />
                      Hỗ trợ khách hàng
                    </div>
                    <div class="settings-support-grid">
                      <a href="tel:19001234" class="support-item">
                        <Phone :size="20" />
                        <div>
                          <strong>Hotline</strong>
                          <span>1900 1234</span>
                        </div>
                      </a>
                      <a href="mailto:cskh@saoclub.vn" class="support-item">
                        <Mail :size="20" />
                        <div>
                          <strong>Email</strong>
                          <span>cskh@saoclub.vn</span>
                        </div>
                      </a>
                      <button type="button" class="support-item">
                        <MessageCircle :size="20" />
                        <div>
                          <strong>Live chat</strong>
                          <span>Phản hồi trong 5 phút</span>
                        </div>
                      </button>
                      <button type="button" class="support-item">
                        <HelpCircle :size="20" />
                        <div>
                          <strong>FAQ</strong>
                          <span>Câu hỏi thường gặp</span>
                        </div>
                      </button>
                    </div>
                  </div>
                </div>
              </div>

            </div>
          </Transition>
        </main>
      </div>
    </div>

    <CustomerOrderDetailModal
      v-if="selectedOrderDetail"
      :order="selectedOrderDetail"
      :items="itemsByOrder[selectedOrderDetail.donHangId || selectedOrderDetail.id] || []"
      :products="products"
      :history="historyByOrder[selectedOrderDetail.donHangId || selectedOrderDetail.id] || []"
      @close="selectedOrderDetail = null"
      @confirm-received="confirmReceived"
      @buy-again="buyAgainOrder"
      @request-return="o => { selectedOrderDetail = null; returnModalOrder = o; }"
      @order-updated="handleOrderUpdated"
    />

    <ProductDetail v-if="selectedProductDetail" :key="selectedProductDetail.bienTheId" :product="selectedProductDetail" :products="products" :wishlist-ids="wishlistIdSet" :auth-user="auth.user"
      @close="selectedProductDetail = null"
      @add-to-cart="p => { emit('add-to-cart', p); selectedProductDetail = null; }"
      @open-product="p => selectedProductDetail = p"
      @toggle-wishlist="toggleWishlistInDetail"
    />

    <ReturnRequestModal v-if="returnModalOrder" :order="returnModalOrder" :items="itemsByOrder[returnModalOrder.donHangId] || []"
      @close="returnModalOrder = null"
      @submitted="returnModalOrder = null; fetchData();"
    />
  </div>

  <!-- Floating chat widget — góc dưới bên phải, dùng chung cho cả khách đã đăng nhập -->
  <ChatWidget />
</template>

<style scoped>
/* Quản lý tài khoản */
.account-shell {
  /* Admin pink palette - đồng bộ với admin-theme.css */
  --pink-50:  #fff5f9;
  --pink-100: #ffe6f0;
  --pink-200: #ffcfe1;
  --pink-300: #f7a8c8;
  --pink-400: #ec4899;
  --pink-500: #db2777;
  --pink-600: #db2777;
  --pink-700: #a81b5d;

  --primary:    var(--pink-500);
  --primary-dark: var(--pink-700);
  --primary-light: var(--pink-100);
  --primary-50: var(--pink-50);

  --success:    #059669;
  --success-light: #ecfdf5;
  --info:       #2563eb;
  --info-light: #dbeafe;
  --purple:     #7c3aed;
  --purple-light: #ede9fe;

  --gray-50:    #f9fafb;
  --gray-100:   #f3f4f6;
  --gray-200:   #e5e7eb;
  --gray-300:   #d1d5db;
  --gray-400:   #9ca3af;
  --gray-500:   #6b7280;
  --gray-600:   #4b5563;
  --gray-700:   #374151;
  --gray-800:   #1f2937;
  --gray-900:   #111827;

  --text-primary: var(--gray-900);
  --text-secondary: var(--gray-600);
  --text-muted:    var(--gray-400);
  --bg-page:    var(--pink-50);
  --bg-card:    #ffffff;
  --border:     var(--pink-200);
  --border-strong: var(--pink-300);

  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --radius-xl: 20px;
  --radius-full: 9999px;
  --sh-1: 0 1px 2px rgba(168, 27, 93, .08), 0 1px 3px rgba(168, 27, 93, .05);
  --sh-2: 0 4px 6px rgba(168, 27, 93, .1), 0 2px 4px rgba(168, 27, 93, .06);
  --sh-3: 0 10px 15px rgba(168, 27, 93, .12), 0 4px 6px rgba(168, 27, 93, .08);

  min-height: 100vh;
  background: var(--bg-page);
  color: var(--text-primary);
  font-family: 'Nunito Sans', 'Segoe UI', -apple-system, BlinkMacSystemFont, sans-serif;
  line-height: 1.5;
}

/* ============== HEADER (gọn, 1 dòng, ~1/3 chiều cao cũ) ============== */
.account-header {
  background: linear-gradient(135deg, var(--pink-500) 0%, var(--pink-600) 100%);
  border-bottom: 1px solid var(--pink-700);
  padding: 8px 0;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: var(--sh-2);
}

.header-top { display: flex; align-items: center; justify-content: space-between; gap: 12px; min-height: 48px; }
.header-left { display: flex; align-items: center; gap: 10px; min-width: 0; flex-wrap: wrap; }
.header-right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }

.btn-back {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 8px;
  color: white;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}
.btn-back:hover { background: rgba(255, 255, 255, 0.28); }

.brand-text {
  font-weight: 800;
  font-size: 1.05rem;
  color: white;
  letter-spacing: 0.3px;
  white-space: nowrap;
}

.header-stats-compact {
  display: flex;
  align-items: center;
  gap: 0;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 999px;
  padding: 3px 5px;
  margin-left: 8px;
}

.stat-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  font-size: 12px;
  font-weight: 700;
  color: white;
  white-space: nowrap;
  line-height: 1;
}

.stat-chip-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  background: rgba(255, 255, 255, 0.18);
  border-radius: 50%;
  flex-shrink: 0;
}

.stat-chip-divider { width: 1px; height: 16px; background: rgba(255, 255, 255, 0.2); }

.user-compact {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 3px 12px 3px 3px;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 999px;
  color: white;
}

.user-compact-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: white;
  color: var(--pink-600);
  font-weight: 800;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.user-compact-info { display: flex; flex-direction: column; line-height: 1.1; }
.user-compact-name { font-weight: 700; font-size: 12.5px; white-space: nowrap; max-width: 140px; overflow: hidden; text-overflow: ellipsis; }
.user-compact-role { font-size: 10px; opacity: 0.85; display: inline-flex; align-items: center; gap: 3px; }

.mobile-menu-btn {
  display: none;
  width: 32px;
  height: 32px;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 8px;
  color: white;
  cursor: pointer;
  transition: all 0.2s ease;
  align-items: center;
  justify-content: center;
}
.mobile-menu-btn:hover { background: rgba(255, 255, 255, 0.28); }

/* ============== BODY WRAPPER ============== */
.account-body { padding: 20px 0; }

/* ============== SIDEBAR (đồng bộ admin theme) ============== */
.account-sidebar {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: var(--sh-1);
  position: sticky;
  top: 80px;
}

.sidebar-header {
  padding: 14px 18px;
  background: linear-gradient(135deg, var(--pink-500) 0%, var(--pink-600) 100%);
}
.sidebar-title { font-weight: 700; color: white; font-size: 13.5px; }

.sidebar-menu { padding: 6px; }

.sidebar-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
  background: transparent;
  border: none;
  border-radius: var(--radius-md);
  color: var(--gray-600);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
  position: relative;
}
.sidebar-item:hover { background: var(--pink-50); color: var(--pink-600); }
.sidebar-item.is-active { background: var(--pink-100); color: var(--pink-600); font-weight: 700; }
.sidebar-item.is-active::before {
  content: ''; position: absolute; left: 0; top: 6px; bottom: 6px;
  width: 3px; background: var(--pink-500); border-radius: 0 3px 3px 0;
}

.sidebar-item-icon {
  width: 32px; height: 32px;
  border-radius: 8px;
  background: var(--gray-100);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s ease;
}
.sidebar-item:hover .sidebar-item-icon { background: var(--pink-200); color: var(--pink-600); }
.sidebar-item.is-active .sidebar-item-icon { background: var(--pink-500); color: white; }

.sidebar-item-label { flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.sidebar-item-badge {
  min-width: 22px; height: 22px; padding: 0 7px;
  background: var(--pink-500);
  color: white;
  border-radius: 9999px;
  font-size: 10.5px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
}

.sidebar-footer { padding: 6px; border-top: 1px solid var(--border); }
.sidebar-item--logout { color: var(--pink-600); }
.sidebar-item--logout .sidebar-item-icon { color: var(--pink-600); }
.sidebar-item--logout:hover { background: var(--pink-100); }

/* ============== MAIN PANEL ============== */
.account-panel {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: var(--sh-1);
}
.panel-section { min-height: 500px; }
/* ══════════════ OVERVIEW (đơn giản, giống ảnh mẫu) ══════════════ */
.overview-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px 16px;
  margin-bottom: 14px;
}
.overview-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.overview-card-icon { color: var(--pink-500); }
.overview-card-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  flex: 1;
}
.overview-card-link {
  background: transparent;
  border: none;
  color: var(--pink-500);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}
.overview-card-link:hover { text-decoration: underline; }
.overview-badge-pill {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  background: var(--success-light, #d1fae5);
  color: var(--success, #059669);
  padding: 3px 10px;
  border-radius: 9999px;
  font-size: 11px;
  font-weight: 700;
}
.overview-loading { display: flex; flex-direction: column; gap: 8px; }
.skel-row { background: var(--gray-100); height: 60px; border-radius: 8px; }

.overview-empty {
  text-align: center;
  padding: 24px 16px;
  color: var(--text-secondary);
  font-size: 12.5px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}
.overview-empty--inline {
  padding: 20px 12px;
}
.overview-empty-icon { color: var(--text-muted); }
.overview-empty-link {
  background: transparent;
  border: none;
  color: var(--pink-500);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.overview-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 14px;
}

.overview-order-list { display: flex; flex-direction: column; gap: 8px; }
.overview-order-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  background: var(--gray-50);
  border: 1px solid var(--border);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.overview-order-row:hover { background: var(--pink-50); border-color: var(--pink-200); }
.overview-order-info { flex: 1; min-width: 0; }
.overview-order-line1 {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  font-size: 12px;
}
.overview-order-label { color: var(--text-muted); font-size: 11.5px; }
.overview-order-code { color: var(--text-primary); font-weight: 700; }
.overview-order-dot { color: var(--text-muted); }
.overview-order-date { color: var(--text-secondary); font-size: 11.5px; }
.overview-order-line2 {
  font-size: 12.5px;
  color: var(--text-primary);
  font-weight: 600;
  margin-top: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.overview-order-right {
  text-align: right;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}
.overview-order-status {
  background: var(--pink-100);
  color: var(--pink-600);
  padding: 3px 10px;
  border-radius: 9999px;
  font-size: 10.5px;
  font-weight: 700;
}
.overview-order-total {
  font-size: 11.5px;
  color: var(--text-secondary);
}
.overview-order-total strong { color: var(--text-primary); font-size: 13px; }
.overview-order-detail-btn {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  background: transparent;
  border: none;
  color: var(--pink-500);
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
}

.overview-voucher-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, var(--pink-50) 0%, var(--pink-100) 100%);
  border-radius: 10px;
  padding: 14px 16px;
}
.overview-voucher-count {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--pink-700);
}
.overview-voucher-count strong { font-size: 18px; color: var(--pink-600); }
.overview-voucher-btn {
  background: var(--pink-500);
  color: white;
  border: none;
  border-radius: 9999px;
  padding: 7px 16px;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 3px 0 var(--pink-700);
}

.overview-buyback { display: flex; flex-direction: column; gap: 6px; }
.overview-buyback-row {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  padding: 8px 10px;
  background: var(--gray-50);
  border-radius: 8px;
  color: var(--text-secondary);
}
.overview-buyback-row strong { color: var(--text-primary); }

.overview-wishlist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 10px;
}
.overview-wishlist-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.overview-wishlist-card:hover { border-color: var(--pink-300); transform: translateY(-2px); }
.overview-wishlist-thumb {
  aspect-ratio: 1;
  background: var(--gray-50);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
  overflow: hidden;
}
.overview-wishlist-thumb img { width: 100%; height: 100%; object-fit: contain; padding: 6px; }
.overview-wishlist-thumb svg { color: var(--text-muted); }
.overview-wishlist-name {
  font-size: 12px;
  color: var(--text-primary);
  font-weight: 600;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.3;
}
.overview-wishlist-price {
  font-size: 12px;
  color: var(--pink-600);
  font-weight: 700;
  margin-top: 4px;
}

/* Sub-tabs (Bảo hành) */
.sub-tabs {
  display: flex;
  gap: 4px;
  padding: 4px;
  background: var(--gray-100);
  border-radius: 10px;
  margin-bottom: 12px;
  width: fit-content;
}
.sub-tab {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 7px 14px;
  background: transparent;
  border: none;
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}
.sub-tab:hover { color: var(--text-primary); }
.sub-tab.is-active {
  background: var(--bg-card);
  color: var(--pink-600);
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}
.sub-tab-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: var(--pink-500);
  color: white;
  border-radius: 9999px;
  font-size: 10px;
  font-weight: 700;
}

/* ══════════════════════════════════════════════════════════
   REWARDS & VOUCHERS MODERN STYLING
══════════════════════════════════════════════════════════ */

/* Loyalty Hero Card */
.loyalty-hero-card {
  position: relative;
  background: linear-gradient(135deg, #1e1b4b 0%, #2e1065 45%, #701a75 100%);
  border-radius: 16px;
  color: white;
  padding: 22px 24px;
  margin-bottom: 18px;
  box-shadow: 0 10px 25px -5px rgba(112, 26, 117, 0.35), 0 8px 10px -6px rgba(30, 27, 75, 0.3);
  overflow: hidden;
}
.loyalty-hero-pattern {
  position: absolute;
  top: -40px;
  right: -40px;
  width: 220px;
  height: 220px;
  background: radial-gradient(circle, rgba(236, 72, 153, 0.22) 0%, rgba(255, 255, 255, 0) 70%);
  border-radius: 50%;
  pointer-events: none;
}
.loyalty-hero-body {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 18px;
}
@media (min-width: 768px) {
  .loyalty-hero-body {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }
}
.loyalty-hero-main {
  flex: 1;
  max-width: 540px;
}
.loyalty-tier-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11.5px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 4px 10px;
  border-radius: 9999px;
  margin-bottom: 10px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(8px);
}
.loyalty-tier-badge.tier-bronze  { color: #fed7aa; background: rgba(217, 119, 6, 0.25); border: 1px solid rgba(217, 119, 6, 0.4); }
.loyalty-tier-badge.tier-silver  { color: #e2e8f0; background: rgba(148, 163, 184, 0.25); border: 1px solid rgba(148, 163, 184, 0.4); }
.loyalty-tier-badge.tier-gold    { color: #fde047; background: rgba(245, 158, 11, 0.28); border: 1px solid rgba(245, 158, 11, 0.5); }
.loyalty-tier-badge.tier-diamond { color: #67e8f9; background: rgba(6, 182, 212, 0.28); border: 1px solid rgba(6, 182, 212, 0.5); }

.loyalty-pts-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
}
.loyalty-pts-val {
  font-size: 34px;
  font-weight: 900;
  line-height: 1;
  letter-spacing: -0.5px;
  background: linear-gradient(180deg, #ffffff 30%, #fbcfe8 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.loyalty-pts-unit {
  font-size: 14px;
  font-weight: 600;
  color: #fbcfe8;
}

.loyalty-progress-box {
  margin: 10px 0 12px;
}
.loyalty-progress-box .text-xs {
  font-size: 11.5px;
  color: rgba(255, 255, 255, 0.85);
}
.loyalty-progress-track {
  width: 100%;
  height: 7px;
  background: rgba(255, 255, 255, 0.16);
  border-radius: 9999px;
  overflow: hidden;
}
.loyalty-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #ec4899, #f59e0b);
  border-radius: 9999px;
  transition: width 0.4s ease;
}
.loyalty-max-tier-note {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #fde047;
  margin: 8px 0;
}
.loyalty-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11.5px;
  color: rgba(255, 255, 255, 0.72);
  margin-top: 4px;
}
.loyalty-tip .text-amber { color: #f59e0b; }

.loyalty-hero-stats {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}
@media (max-width: 576px) {
  .loyalty-hero-stats {
    flex-direction: column;
    width: 100%;
  }
}
.loyalty-stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.loyalty-stat-card:hover {
  background: rgba(255, 255, 255, 0.14);
  transform: translateY(-2px);
  border-color: rgba(255, 255, 255, 0.3);
}
.loyalty-stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.loyalty-stat-icon--voucher {
  background: rgba(236, 72, 153, 0.25);
  color: #f472b6;
}
.loyalty-stat-icon--wheel {
  background: rgba(245, 158, 11, 0.25);
  color: #fbbf24;
}
.loyalty-stat-num {
  font-size: 20px;
  font-weight: 800;
  line-height: 1.1;
  color: white;
}
.loyalty-stat-lbl {
  font-size: 11.5px;
  color: rgba(255, 255, 255, 0.75);
}

/* Voucher Filter Bar */
.voucher-filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  overflow-x: auto;
  padding-bottom: 4px;
}
.voucher-filter-btn {
  border: 1px solid var(--border);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 12.5px;
  font-weight: 600;
  padding: 6px 14px;
  border-radius: 9999px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s ease;
}
.voucher-filter-btn:hover {
  border-color: var(--pink-300);
  color: var(--pink-600);
}
.voucher-filter-btn.is-active {
  background: var(--pink-500);
  border-color: var(--pink-500);
  color: white;
  box-shadow: 0 2px 6px rgba(219, 39, 119, 0.3);
}

/* Empty State Box */
.rewards-empty-box {
  background: var(--bg-card);
  border: 1px dashed var(--border-strong);
  border-radius: 14px;
  padding: 40px 20px;
  text-align: center;
}
.rewards-empty-icon-wrap {
  width: 68px;
  height: 68px;
  border-radius: 50%;
  background: var(--pink-50);
  color: var(--pink-500);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}
.rewards-empty-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 6px;
}
.rewards-empty-desc {
  font-size: 12.5px;
  color: var(--text-secondary);
  max-width: 420px;
  margin: 0 auto;
}
.btn-reward-cta {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 9999px;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn-reward-cta--solid {
  background: var(--pink-500);
  color: white;
  border: 1px solid var(--pink-500);
}
.btn-reward-cta--solid:hover {
  background: var(--pink-600);
}
.btn-reward-cta--outline {
  background: transparent;
  color: var(--pink-600);
  border: 1px solid var(--pink-300);
}
.btn-reward-cta--outline:hover {
  background: var(--pink-50);
}

/* Voucher Ticket Grid & Cards */
.my-voucher-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 14px;
}
@media (max-width: 480px) {
  .my-voucher-grid {
    grid-template-columns: 1fr;
  }
}
.my-voucher-ticket {
  display: flex;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow: hidden;
  position: relative;
  box-shadow: var(--sh-1);
  transition: all 0.2s ease;
}
.my-voucher-ticket:hover:not(.ticket--disabled) {
  transform: translateY(-2px);
  border-color: var(--pink-300);
  box-shadow: var(--sh-2);
}
.my-voucher-ticket.ticket--disabled {
  opacity: 0.65;
  filter: grayscale(0.5);
  background: var(--gray-50);
}

/* Ticket Left */
.ticket-left {
  width: 95px;
  min-width: 95px;
  background: linear-gradient(135deg, var(--pink-500) 0%, var(--pink-600) 100%);
  color: white;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 14px 6px;
  position: relative;
  text-align: center;
  border-right: 1px dashed rgba(255, 255, 255, 0.4);
}
.ticket--disabled .ticket-left {
  background: linear-gradient(135deg, #6b7280 0%, #4b5563 100%);
}
.ticket-discount-val {
  font-size: 17px;
  font-weight: 900;
  line-height: 1.1;
  word-break: break-word;
}
.ticket-discount-tag {
  font-size: 9.5px;
  font-weight: 800;
  letter-spacing: 0.5px;
  opacity: 0.9;
  margin-top: 4px;
}
.ticket-notch {
  position: absolute;
  width: 14px;
  height: 14px;
  background: var(--bg-page);
  border-radius: 50%;
  right: -7px;
  z-index: 2;
}
.ticket-notch--top { top: -7px; }
.ticket-notch--bottom { bottom: -7px; }

/* Ticket Center */
.ticket-center {
  flex: 1;
  padding: 12px 14px;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.ticket-badge-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}
.ticket-source-badge {
  font-size: 10px;
  font-weight: 600;
  color: var(--pink-600);
  background: var(--pink-50);
  padding: 2px 7px;
  border-radius: 4px;
}
.ticket-status-pill {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 9999px;
}
.ticket-status-pill--active {
  background: #ecfdf5;
  color: #059669;
}
.ticket-status-pill--used {
  background: var(--gray-100);
  color: var(--gray-500);
}
.ticket-status-pill--expired {
  background: #fef2f2;
  color: #ef4444;
}

.ticket-title {
  font-size: 13.5px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.3;
  margin-bottom: 6px;
}
.ticket-rules {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 11px;
  color: var(--text-secondary);
}
.ticket-rule-item {
  display: flex;
  align-items: center;
  gap: 4px;
}
.ticket-rule-item strong {
  color: var(--text-primary);
}

/* Ticket Right */
.ticket-right {
  width: 125px;
  min-width: 125px;
  border-left: 1px dashed var(--border);
  background: var(--pink-50);
  padding: 12px 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.ticket--disabled .ticket-right {
  background: var(--gray-100);
}
.ticket-code-tag {
  font-family: 'SF Mono', 'Fira Code', Menlo, monospace;
  font-size: 11px;
  font-weight: 700;
  color: var(--pink-700);
  background: white;
  border: 1px dashed var(--pink-300);
  padding: 3px 6px;
  border-radius: 6px;
  max-width: 105px;
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
}
.ticket--disabled .ticket-code-tag {
  color: var(--gray-500);
  border-color: var(--gray-300);
}
.ticket-actions {
  display: flex;
  flex-direction: column;
  gap: 5px;
  width: 100%;
}
.btn-ticket-copy, .btn-ticket-use {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 100%;
  padding: 5px 6px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn-ticket-copy {
  background: white;
  color: var(--pink-600);
  border: 1px solid var(--pink-200);
}
.btn-ticket-copy:hover {
  background: var(--pink-100);
  border-color: var(--pink-400);
}
.btn-ticket-copy.is-copied {
  background: #ecfdf5;
  color: #059669;
  border-color: #a7f3d0;
}
.btn-ticket-use {
  background: var(--pink-500);
  color: white;
  border: none;
}
.btn-ticket-use:hover {
  background: var(--pink-600);
}
.ticket-inactive-msg {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 600;
  text-align: center;
}

/* ════════════ Sub-tab 2: Đổi điểm thưởng ════════════ */
.redeem-intro-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: var(--pink-50);
  border: 1px solid var(--pink-200);
  border-radius: 12px;
  margin-bottom: 16px;
}
@media (max-width: 576px) {
  .redeem-intro-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
.redeem-pts-indicator {
  display: flex;
  align-items: baseline;
  gap: 6px;
  background: white;
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px solid var(--pink-200);
}

.redeem-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
}
.redeem-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  transition: all 0.2s ease;
  box-shadow: var(--sh-1);
  position: relative;
}
.redeem-card:hover {
  transform: translateY(-2px);
  border-color: var(--pink-300);
  box-shadow: var(--sh-2);
}
.redeem-card--affordable {
  border-color: var(--pink-300);
}
.redeem-card-badge {
  display: inline-flex;
  align-items: baseline;
  gap: 4px;
  background: var(--pink-500);
  color: white;
  padding: 4px 10px;
  border-radius: 8px;
  width: fit-content;
  margin-bottom: 10px;
}
.redeem-card-badge .redeem-val {
  font-size: 15px;
  font-weight: 800;
}
.redeem-card-badge .redeem-sub {
  font-size: 10px;
  font-weight: 700;
  opacity: 0.9;
}
.redeem-card-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 4px;
  line-height: 1.3;
}
.redeem-card-desc {
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.4;
  margin-bottom: 6px;
}
.redeem-card-max {
  font-size: 11px;
  color: var(--text-muted);
  margin-bottom: 12px;
}
.redeem-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px dashed var(--border);
  margin-top: 8px;
}
.redeem-cost {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-primary);
}
.redeem-cost .fill-amber { fill: #f59e0b; color: #f59e0b; }
.btn-redeem {
  padding: 6px 14px;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 700;
  border: none;
  cursor: pointer;
  background: var(--gray-200);
  color: var(--gray-600);
  transition: all 0.15s ease;
}
.btn-redeem--active {
  background: var(--pink-500);
  color: white;
}
.btn-redeem--active:hover:not(:disabled) {
  background: var(--pink-600);
}
.btn-redeem:disabled {
  cursor: not-allowed;
  opacity: 0.8;
}

/* ════════════ Sub-tab 3: Vòng quay may mắn ════════════ */
.wheel-panel-container {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 16px;
}
.wheel-panel-intro {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: var(--pink-50);
  border: 1px solid var(--pink-200);
  border-radius: 10px;
  margin-bottom: 16px;
}

/* Settings sections */
.settings-section {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px 16px;
  margin-bottom: 14px;
}
.settings-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 13.5px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px dashed var(--border);
}
.settings-section-header svg { color: var(--pink-500); }

.btn-edit-profile {
  background: var(--pink-50);
  color: var(--pink-600);
  border: 1px solid var(--pink-200);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn-edit-profile:hover {
  background: var(--pink-100);
  border-color: var(--pink-400);
  transform: translateY(-1px);
}

.profile-view-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  padding: 4px 0;
}
.profile-view-item {
  background: var(--gray-50);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 10px 14px;
}
.profile-view-label {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}
.profile-view-label svg { color: var(--text-secondary); }
.profile-view-value {
  font-size: 13.5px;
  color: var(--text-primary);
  word-break: break-word;
}
@media (max-width: 575.98px) {
  .profile-view-grid { grid-template-columns: 1fr; }
}
.settings-section-empty {
  text-align: center;
  padding: 20px 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);
  font-size: 12.5px;
}
.settings-support-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.support-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: var(--gray-50);
  border: 1px solid var(--border);
  border-radius: 10px;
  cursor: pointer;
  text-align: left;
  text-decoration: none;
  color: inherit;
  transition: all 0.15s ease;
}
.support-item:hover { background: var(--pink-50); border-color: var(--pink-300); transform: translateY(-1px); }
.support-item :first-child {
  color: var(--pink-500);
  flex-shrink: 0;
}
.support-item strong { display: block; font-size: 13px; color: var(--text-primary); }
.support-item span { font-size: 11.5px; color: var(--text-secondary); }

@media (max-width: 767.98px) {
  .overview-grid { grid-template-columns: 1fr; }
  .settings-support-grid { grid-template-columns: 1fr; }
  .overview-order-row { flex-direction: column; align-items: stretch; }
  .overview-order-right { align-items: flex-start; text-align: left; }
}


.panel-header {
  padding: 16px 22px;
  border-bottom: 1px solid var(--border);
  background: linear-gradient(180deg, var(--pink-50) 0%, #fff 100%);
}
.panel-header-content { display: flex; align-items: center; gap: 12px; }
.panel-header-icon { color: var(--pink-500); }
.panel-title { font-weight: 800; font-size: 1.05rem; color: var(--gray-900); line-height: 1.3; }
.panel-subtitle { font-size: 12px; color: var(--text-secondary); margin-top: 2px; }

.sub-tabs {
  display: flex; gap: 4px; padding: 10px 14px;
  border-bottom: 1px solid var(--border);
  overflow-x: auto; scrollbar-width: none;
}
.sub-tabs::-webkit-scrollbar { display: none; }

.sub-tab {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 8px 14px;
  background: transparent;
  border: 1px solid var(--border);
  border-radius: 9999px;
  color: var(--text-secondary);
  font-size: 12.5px; font-weight: 600;
  white-space: nowrap; cursor: pointer;
  transition: all 0.2s ease;
}
.sub-tab:hover { background: var(--pink-50); color: var(--pink-600); border-color: var(--pink-200); }
.sub-tab.is-active {
  background: var(--pink-500); color: white; border-color: var(--pink-500);
}

.sub-tab-count {
  min-width: 18px; height: 18px; padding: 0 5px;
  background: rgba(255,255,255,0.25);
  border-radius: 9999px;
  font-size: 10px; font-weight: 700;
}
.sub-tab:not(.is-active) .sub-tab-count { background: var(--pink-100); color: var(--pink-600); }

.panel-content { padding: 18px 22px; }

/* ============== ORDER CARDS ============== */
.order-list { display: flex; flex-direction: column; gap: 14px; }

.order-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: all 0.2s ease;
}
.order-card:hover { border-color: var(--pink-300); box-shadow: var(--sh-2); }

.order-card-header {
  display: flex; justify-content: space-between; align-items: flex-start;
  padding: 14px 18px; gap: 12px; flex-wrap: wrap;
}
.order-info { display: flex; flex-direction: column; gap: 3px; }
.order-code { display: flex; align-items: center; gap: 6px; }
.order-code-label { font-size: 10.5px; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.05em; }
.order-code-value { font-weight: 700; color: var(--gray-900); font-size: 13.5px; }
.order-date { font-size: 11.5px; color: var(--text-secondary); }

.status-pill {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 5px 11px;
  border-radius: 9999px;
  font-size: 11px; font-weight: 600;
  white-space: nowrap;
}
.status-pill--sm { padding: 3px 8px; font-size: 10px; }

.order-timeline { padding: 0 18px 14px; }
.order-products { padding: 0 18px; display: flex; flex-direction: column; gap: 8px; }

.order-product-mini {
  display: flex; align-items: center; gap: 12px;
  padding: 10px;
  background: var(--gray-50);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s ease;
}
.order-product-mini:hover { background: var(--pink-50); }

.product-thumb {
  width: 50px; height: 42px;
  border-radius: 8px;
  background: var(--bg-card);
  display: flex; align-items: center; justify-content: center;
  overflow: hidden;
  border: 1px solid var(--border);
  flex-shrink: 0;
}
.product-thumb img { width: 100%; height: 100%; object-fit: contain; padding: 4px; }

.product-mini-info { flex: 1; min-width: 0; }
.product-mini-name {
  font-weight: 600; font-size: 13px; color: var(--text-primary);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.product-mini-qty { font-size: 11px; color: var(--text-secondary); margin-top: 1px; }
.product-mini-price { font-weight: 700; color: var(--pink-500); font-size: 13px; min-width: 85px; text-align: right; }
.products-more { font-size: 11.5px; color: var(--text-muted); text-align: center; padding: 6px; }

.order-card-footer {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 18px; margin-top: 10px;
  border-top: 1px solid var(--border);
  flex-wrap: wrap; gap: 10px;
}
.order-total { display: flex; align-items: center; gap: 6px; }
.total-label { font-size: 11.5px; color: var(--text-secondary); }
.total-value { font-weight: 800; font-size: 1rem; color: var(--pink-500); }
.order-actions { display: flex; gap: 6px; }

.order-card--compact { border-left: 4px solid var(--card-accent, var(--pink-500)); }

.compact-header {
  display: flex; align-items: center; gap: 14px;
  padding: 12px 18px;
  cursor: pointer;
  transition: background 0.2s ease;
  flex-wrap: wrap;
}
.compact-header:hover { background: var(--pink-50); }
.compact-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.compact-right { display: flex; align-items: center; gap: 10px; }
.compact-total { font-weight: 800; font-size: 14px; color: var(--pink-500); }

.expand-chevron { color: var(--text-muted); transition: transform 0.2s ease; flex-shrink: 0; }
.expand-chevron.is-open { transform: rotate(180deg); }

.compact-detail { padding: 0 18px 18px; border-top: 1px solid var(--border); }
.detail-products { display: flex; flex-direction: column; gap: 8px; margin-top: 14px; }
.detail-actions { display: flex; gap: 6px; margin-top: 14px; flex-wrap: wrap; }
.returns-list { display: flex; flex-direction: column; gap: 8px; margin-top: 14px; }

.return-chip {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 14px;
  background: var(--gray-50);
  border-radius: var(--radius-md);
  flex-wrap: wrap;
}
.return-amount { font-weight: 700; color: var(--pink-500); }
.return-reason { font-size: 11.5px; color: var(--text-secondary); }

/* ============== WISHLIST ============== */
.wishlist-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 14px; }

.wishlist-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s ease;
}
.wishlist-card:hover { border-color: var(--pink-300); box-shadow: var(--sh-2); transform: translateY(-2px); }

.wishlist-thumb {
  height: 150px; background: var(--gray-50);
  display: flex; align-items: center; justify-content: center; overflow: hidden;
}
.wishlist-thumb img { width: 100%; height: 100%; object-fit: contain; padding: 14px; }
.wishlist-info { padding: 14px; }

.wishlist-name {
  font-weight: 700; font-size: 13.5px; color: var(--text-primary);
  line-height: 1.4;
  display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.wishlist-brand { font-size: 11px; color: var(--text-muted); margin-top: 3px; }
.wishlist-price { font-weight: 800; font-size: 1rem; color: var(--pink-500); margin-top: 6px; }

.wishlist-actions {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 14px; border-top: 1px solid var(--border); background: var(--gray-50);
}
.out-of-stock-badge { font-size: 10.5px; font-weight: 600; color: var(--text-muted); padding: 3px 9px; background: var(--gray-200); border-radius: 9999px; }

/* ============== SETTINGS ============== */
.settings-card { background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius-lg); overflow: hidden; margin-bottom: 14px; }
.settings-card:last-child { margin-bottom: 0; }

.settings-card-header {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 18px; border-bottom: 1px solid var(--border); background: var(--pink-50);
}
.settings-card-icon {
  width: 40px; height: 40px;
  border-radius: 10px;
  background: var(--pink-100); color: var(--pink-600);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.settings-card-icon--purple { background: var(--purple-light); color: var(--purple); }
.settings-card-icon--blue { background: var(--info-light); color: var(--info); }

.settings-card-name { font-weight: 700; font-size: 13.5px; color: var(--text-primary); }
.settings-card-desc { font-size: 11.5px; color: var(--text-secondary); margin-top: 2px; }

.rewards-list, .vouchers-list { padding: 10px; display: flex; flex-direction: column; gap: 8px; }
.reward-item, .voucher-item {
  display: flex; align-items: center; justify-content: space-between;
  gap: 10px; padding: 10px;
  background: var(--gray-50);
  border-radius: var(--radius-md);
  flex-wrap: wrap;
}
.reward-name, .voucher-code { font-weight: 600; font-size: 12.5px; color: var(--text-primary); }
.voucher-code { font-family: 'SF Mono', 'Consolas', monospace; color: var(--pink-500); }
.reward-cost, .voucher-meta { font-size: 11px; color: var(--text-secondary); margin-top: 1px; }
.voucher-meta { display: flex; flex-direction: column; gap: 2px; }
.no-items { text-align: center; padding: 20px; color: var(--text-muted); font-size: 12.5px; }

.profile-form { padding: 18px; }
.form-row { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; margin-bottom: 14px; }
.form-group { display: flex; flex-direction: column; gap: 5px; }
.form-label { font-size: 11.5px; font-weight: 600; color: var(--text-secondary); }

.input-with-icon { position: relative; }
.input-icon {
  position: absolute; left: 11px; top: 50%; transform: translateY(-50%);
  color: var(--text-muted); pointer-events: none;
}

.form-input {
  width: 100%; padding: 10px 12px 10px 36px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 10px;
  color: var(--text-primary);
  font-size: 13px; font-family: inherit;
  transition: all 0.2s ease;
}
.form-input:focus { outline: none; border-color: var(--pink-500); box-shadow: 0 0 0 3px var(--pink-100); }
.form-actions { display: flex; justify-content: flex-end; padding-top: 6px; }

/* ============== BUTTONS ============== */
.btn-primary {
  display: inline-flex; align-items: center; justify-content: center; gap: 6px;
  padding: 10px 20px;
  background: var(--pink-500); color: white;
  border: none;
  border-radius: 9999px;
  font-size: 13px; font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 3px 0 var(--pink-700), 0 4px 8px rgba(168, 27, 93, 0.3);
}
.btn-primary:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 0 var(--pink-700), 0 6px 12px rgba(168, 27, 93, 0.35); }
.btn-primary:active:not(:disabled) { transform: translateY(1px); box-shadow: inset 0 2px 4px rgba(0,0,0,0.2); }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; box-shadow: none; }

.btn-outline-secondary {
  display: inline-flex; align-items: center; justify-content: center; gap: 5px;
  padding: 7px 14px;
  background: var(--bg-card); color: var(--text-primary);
  border: 1px solid var(--border);
  border-radius: 9999px;
  font-size: 11.5px; font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}
.btn-outline-secondary:hover:not(:disabled) { background: var(--pink-50); border-color: var(--pink-300); color: var(--pink-600); }
.btn-outline-secondary:disabled { opacity: 0.5; cursor: not-allowed; }

.btn-outline-danger {
  display: inline-flex; align-items: center; justify-content: center; gap: 5px;
  padding: 7px 14px;
  background: var(--bg-card); color: var(--pink-600);
  border: 1px solid var(--pink-500);
  border-radius: 9999px;
  font-size: 11.5px; font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}
.btn-outline-danger:hover:not(:disabled) { background: var(--pink-100); }
.btn-outline-danger:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-sm { padding: 6px 12px; font-size: 11px; }

.btn-icon {
  width: 32px; height: 32px; padding: 0;
  background: transparent;
  border: 1px solid var(--border);
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}
.btn-icon--primary { background: var(--pink-500); color: white; border-color: var(--pink-500); }
.btn-icon--primary:hover { background: var(--pink-700); }
.btn-icon--danger { color: var(--pink-600); }
.btn-icon--danger:hover { background: var(--pink-100); border-color: var(--pink-500); }

/* ============== ALERTS ============== */
.alert-banner {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 12.5px;
  margin: 14px 0;
}
.alert-banner--danger { background: #fef2f2; color: #b91c1c; border: 1px solid rgba(220, 38, 38, 0.2); }
.alert-banner--success { background: var(--success-light); color: var(--success); border: 1px solid rgba(5, 150, 105, 0.2); }

/* ============== EMPTY & LOADING ============== */
.empty-state { text-align: center; padding: 40px 20px; }
.empty-icon-wrap {
  width: 80px; height: 80px; margin: 0 auto 16px;
  background: var(--pink-50); border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: var(--pink-400);
}
.empty-title { font-weight: 700; font-size: 0.95rem; color: var(--text-primary); }
.empty-text { font-size: 12.5px; color: var(--text-secondary); margin-top: 6px; max-width: 280px; margin-left: auto; margin-right: auto; }

.loading-state { display: flex; flex-direction: column; gap: 10px; }

/* ============== ANIMATIONS ============== */
.spin { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

.panel-fade-enter-active, .panel-fade-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.panel-fade-enter-from { opacity: 0; transform: translateY(6px); }
.panel-fade-leave-to { opacity: 0; transform: translateY(-3px); }

.slide-down-enter-active, .slide-down-leave-active { transition: all 0.25s ease; overflow: hidden; }
.slide-down-enter-from, .slide-down-leave-to { opacity: 0; max-height: 0; padding-top: 0; padding-bottom: 0; }
.slide-down-enter-to, .slide-down-leave-from { opacity: 1; max-height: 2000px; }

/* ============== RESPONSIVE ============== */
@media (max-width: 991.98px) {
  .account-sidebar {
    position: fixed; left: 0; top: 0; bottom: 0;
    width: 300px; z-index: 1000;
    border-radius: 0;
    transform: translateX(-100%);
    transition: transform 0.3s ease;
    box-shadow: var(--sh-3);
    max-height: 100vh; overflow-y: auto;
  }
  .account-sidebar.sidebar-open { transform: translateX(0); }
  .mobile-menu-btn { display: flex; }
}

@media (max-width: 767.98px) {
  .user-compact-info { display: none; }
  .panel-content { padding: 14px; }
  .form-row { grid-template-columns: 1fr; }
  .wishlist-grid { grid-template-columns: 1fr; }
  .header-stats-compact { display: none !important; }
}

@media (max-width: 575.98px) {
  .brand-text { font-size: 0.95rem; }
  .user-compact { padding: 2px 8px 2px 2px; }
  .panel-header { padding: 14px 16px; }
  .sub-tab { padding: 7px 12px; font-size: 12px; }
}
</style>