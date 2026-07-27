<script setup>
// ========================================================
// AccountPage.vue — Trang tài khoản khách hàng
// Gồm 3 tab: Đơn hàng hiện tại | Lịch sử mua hàng | Cài đặt tài khoản
// Emit ra: go-home, add-to-cart (App.vue sở hữu giỏ hàng, xem xem sản phẩm rồi
// mua lại sẽ báo lên App.vue thêm hộ)
// ========================================================
import { ref, computed, onMounted, onUnmounted } from "vue";
import { AuthStore, setSession } from "../stores/index.js";
import { I18nStore, t } from "../i18n/index.js";
import { orderStatusLabel, orderStatusColor, orderStatusIcon } from "../utils/orderStatus.js";
import { formatPrice as formatPriceRaw } from "../utils/formatPrice.js";
import * as DonHangService         from "../Service/DonHangService.js";
import * as ChiTietDonHangService  from "../Service/ChiTietDonHangService.js";
import * as SanPhamService         from "../Service/SanPhamService.js";
import * as KhachHangService       from "../Service/KhachHangService.js";
import * as LichSuDonHangService   from "../Service/LichSuDonHangService.js";
import * as PhieuTraHangService    from "../Service/PhieuTraHangService.js";
import * as DmDoiThuongService         from "../Service/DmDoiThuongService.js";
import * as PhieuGiamGiaCaNhanService  from "../Service/PhieuGiamGiaCaNhanService.js";
import OrderStatusTimeline from "../components/order/OrderStatusTimeline.vue";
import OrderTrackingLog from "../components/order/OrderTrackingLog.vue";
import ReturnRequestModal from "../components/order/ReturnRequestModal.vue";
import ProductDetail from "../components/product/ProductDetail.vue";
import Skeleton from "../components/common/Skeleton.vue";
import LuckyWheelPanel from "../components/account/LuckyWheelPanel.vue";

const emit = defineEmits(["go-home", "add-to-cart", "buy-again-unavailable"]);

const auth = AuthStore;
// Tách theo trạng thái kiểu Shopee thay vì gộp chung "hiện tại/lịch sử"
const activeTab = ref("pending"); // 'pending' | 'shipping' | 'completed' | 'cancelled' | 'wheel' | 'settings'

// Nhóm trạng thái đơn hàng (trangThaiDonHang) ứng với từng tab — "processing" (đang đóng
// gói) vẫn nằm ở tab "Chờ xác nhận" (chưa giao cho shipper), chỉ chuyển sang tab "Đang
// giao" khi admin bấm "Giao hàng" (trangThaiDonHang -> shipping). Xem AdminPage.vue
// advanceOrderStatus() cho luồng cập nhật phía admin.
const TAB_STATUS_GROUPS = {
  pending:   ["pending", "confirmed", "processing"],
  shipping:  ["shipping", "out_for_delivery"],
  completed: ["delivered"],
  cancelled: ["cancelled", "returned"],
};

// Đơn có phiếu trả hàng đang active (chờ xử lý/đã xử lý) — luôn hiện ở tab "Đã hủy/Trả
// hàng" bất kể trangThaiDonHang thực tế (thường vẫn là "delivered", vì phiếu trả hàng
// không tự đổi trạng thái đơn — nhân viên tự bấm đổi riêng nếu muốn).
const hasActiveReturn = (donHangId) =>
  (returnsByOrder.value[donHangId] || []).some(r => r.trangThai === 'cho_xu_ly' || r.trangThai === 'da_xu_ly');

const orderTabId = (o) => {
  if (hasActiveReturn(o.donHangId)) return 'cancelled';
  for (const [tabId, statuses] of Object.entries(TAB_STATUS_GROUPS)) {
    if (statuses.includes(o.trangThaiDonHang)) return tabId;
  }
  return null;
};

// Còn trong hạn 7 ngày kể từ khi nhận hàng, và đơn chưa có phiếu trả hàng active.
const canRequestReturn = (o) => {
  if (o.trangThaiDonHang !== 'delivered' || hasActiveReturn(o.donHangId) || !o.ngayGiaoThucTe) return false;
  return Date.now() <= new Date(o.ngayGiaoThucTe).getTime() + 7 * 24 * 60 * 60 * 1000;
};

const RETURN_STATUS_LABEL = { cho_xu_ly: 'account.returnStatusPending', da_xu_ly: 'account.returnStatusDone', tu_choi: 'account.returnStatusRejected' };
const RETURN_STATUS_COLOR = {
  cho_xu_ly: { bg: 'rgba(250,204,21,0.15)', text: '#facc15' },
  da_xu_ly:  { bg: 'rgba(34,197,94,0.15)',  text: '#22c55e' },
  tu_choi:   { bg: 'rgba(239,68,68,0.15)',  text: '#f87171' },
};

const TABS = computed(() => [
  { id: "pending",   icon: "🕐", label: t("account.tabPending") },
  { id: "shipping",  icon: "🚚", label: t("account.tabShipping") },
  { id: "completed", icon: "✅", label: t("account.tabCompleted") },
  { id: "cancelled", icon: "❌", label: t("account.tabCancelled") },
  { id: "wheel",     icon: "🎡", label: t("account.tabWheel") },
  { id: "settings",  icon: "⚙️", label: t("account.tabSettings") },
]);

// ── Dữ liệu ─────────────────────────────────────────────────────────────────
const orders      = ref([]);   // toàn bộ đơn hàng của khách hàng này
const products    = ref([]);   // danh sách bienThe/sanPham (để map ảnh + tên)
const itemsByOrder = ref({});  // { [donHangId]: ChiTietDonHangResponse[] }
const historyByOrder = ref({});  // { [donHangId]: LichSuDonHangResponse[] }
const returnsByOrder = ref({});  // { [donHangId]: PhieuTraHangResponse[] }
const returnModalOrder = ref(null);  // đơn đang mở modal trả hàng (null = đóng)
const loading      = ref(false);

// Số đơn theo từng tab — hiện badge trên tab dù đang xem tab khác
const tabOrderCounts = computed(() => {
  const counts = { pending: 0, shipping: 0, completed: 0, cancelled: 0 };
  orders.value.forEach(o => {
    const tabId = orderTabId(o);
    if (tabId && tabId in counts) counts[tabId]++;
  });
  return counts;
});

// Đơn thuộc tab "Chờ xác nhận" / "Đang giao" — hiện dạng thẻ đầy đủ + timeline
const currentOrders = computed(() => {
  return (activeTab.value === 'pending' || activeTab.value === 'shipping')
    ? orders.value.filter(o => orderTabId(o) === activeTab.value)
    : [];
});

// Đơn thuộc tab "Hoàn tất" / "Đã hủy/Trả hàng" — hiện dạng dòng gọn, bấm mở xem sản phẩm
const historyOrders = computed(() => {
  return (activeTab.value === 'completed' || activeTab.value === 'cancelled')
    ? orders.value.filter(o => orderTabId(o) === activeTab.value)
    : [];
});

const productByBienThe = (bienTheId) =>
  products.value.find(p => p.bienTheId === bienTheId);

// Bấm vào 1 đơn trong "Lịch sử mua hàng" để xem đã mua sản phẩm gì (đóng/mở)
const expandedHistoryOrders = ref(new Set());
const toggleHistoryOrder = (donHangId) => {
  const s = expandedHistoryOrders.value;
  if (s.has(donHangId)) s.delete(donHangId); else s.add(donHangId);
  expandedHistoryOrders.value = new Set(s);
};

// Bấm vào 1 sản phẩm đã mua — mở trang chi tiết (đủ thông số + nút thêm vào giỏ
// để mua lại), dùng lại đúng ProductDetail của trang chủ.
const selectedProductDetail = ref(null);
const viewProductDetail = (item) => {
  const product = productByBienThe(item.bienTheId);
  if (product) selectedProductDetail.value = product;
};

// "Mua lại" cả đơn — thêm từng dòng vào giỏ đúng số lượng đã mua trước đó. Trước đây sản
// phẩm đã ngừng kinh doanh (trangThai="inactive") hoặc đã bị xóa hẳn (productByBienThe trả
// undefined) bị bỏ qua âm thầm, khách không biết vì sao giỏ hàng thiếu mất 1 món.
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
          ["shipping", "out_for_delivery", "delivered"].includes(o.trangThaiDonHang)
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

// ── Trạng thái đơn hàng: nhãn + màu (dùng chung — xem src/utils/orderStatus.js) ──
// OrderStatusTimeline.vue giờ nhận thẳng trangThaiDonHang, tự chọn bộ bước phù hợp.

const formatPrice = (v) => (v == null ? "—" : formatPriceRaw(v));

const formatDate = (d) => {
  if (!d) return "—";
  try { return new Date(d).toLocaleString(I18nStore.locale); } catch { return d; }
};

// ── Tab cài đặt: form thông tin cá nhân ──────────────────────────────────────
const profile        = ref(null);  // KhachHang đầy đủ từ API (để giữ lại field không sửa)
const profileLoading = ref(false);
const profileSaving  = ref(false);
const profileError   = ref("");
const profileSuccess = ref("");
const profileForm = ref({ hoTen: "", soDienThoai: "", email: "", diaChi: "" });

// ── Điểm & Voucher: danh mục phần thưởng đổi được + voucher cá nhân đã đổi ──
const rewards       = ref([]);   // danh mục phần thưởng đổi được
const myVouchers     = ref([]);  // voucher cá nhân đã đổi
const redeemingId    = ref(null); // đang gọi API đổi thưởng cho id nào (disable nút, tránh double-click)
const redeemError    = ref("");

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
    if (!res.ok) { redeemError.value = await res.text().catch(() => res.statusText); return; }
    myVouchers.value = await PhieuGiamGiaCaNhanService.getCuaToi().catch(() => []);
    await fetchProfile(); // cập nhật lại số điểm hiện tại trên badge header
  } catch (e) {
    redeemError.value = e.message || t("account.rewards.redeemError");
  } finally {
    redeemingId.value = null;
  }
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
    // Đồng bộ lại tên hiển thị trên NavBar / session
    setSession({ ...auth.user, hoTen: body.hoTen, email: body.email, soDienThoai: body.soDienThoai });
  } catch (e) {
    profileError.value = e.message || t("account.settings.saveErrorPrefix");
  } finally {
    profileSaving.value = false;
  }
};

// Admin đổi trạng thái đơn ở tab khác -> trang này tự tải lại, khỏi cần F5.
let orderSse = null;
onMounted(() => {
  fetchData();
  fetchProfile();
  orderSse = new EventSource(`/api/don-hang/events?token=${encodeURIComponent(auth.user?.token ?? '')}`);
  orderSse.addEventListener('order-updated', () => { fetchData(); });
});
onUnmounted(() => { if (orderSse) orderSse.close(); });
</script>

<template>
  <div style="min-height:100vh; background:var(--bg-page); color:var(--text-primary); font-family:'Nunito Sans','Segoe UI',sans-serif;">

    <!-- Header -->
    <header class="sticky-top" style="background:var(--bg-header); backdrop-filter:blur(8px); border-bottom:1px solid var(--border-color);">
      <div class="container-xl d-flex flex-wrap align-items-center gap-3 py-3">
        <button class="btn btn-sm fw-bold d-flex align-items-center gap-1"
                style="background:var(--bg-input); border:1px solid var(--border-color-strong); border-radius:12px; color:var(--text-primary);"
                @click="emit('go-home')">
          ← {{ t('common.backHome') }}
        </button>

        <div class="vr d-none d-sm-block" style="opacity:0.15; height:28px;"></div>

        <div class="d-flex align-items-center gap-3 flex-grow-1">
          <div class="rounded-circle d-flex align-items-center justify-content-center fw-black flex-shrink-0"
               style="width:44px; height:44px; background:linear-gradient(135deg,var(--accent),var(--accent-2)); color:var(--accent-text); font-size:1.05rem; box-shadow:0 4px 14px rgba(244,63,94,0.25);">
            {{ (auth.user?.hoTen || auth.user?.username || '?').charAt(0).toUpperCase() }}
          </div>
          <div>
            <div class="fw-black" style="color:var(--text-heading); font-size:1.05rem; line-height:1.25;">{{ auth.user?.hoTen || auth.user?.username }}</div>
            <div style="color:var(--text-secondary); font-size:11px;">
              <span>👤 {{ t('account.myAccount') }}</span>
            </div>
          </div>
        </div>

        <!-- Điểm tích lũy -->
        <div v-if="profile" class="d-flex align-items-center gap-1 px-3 py-1 rounded-pill fw-bold"
             style="background:rgba(244,63,94,0.1); border:1px solid rgba(244,63,94,0.25); color:var(--accent-fg); font-size:12px; white-space:nowrap;">
          🎁 {{ t('account.points', { points: profile.diemTichLuy ?? 0 }) }}
        </div>

        <!-- So du vi -->
        <div v-if="profile" class="d-flex align-items-center gap-1 px-3 py-1 rounded-pill fw-bold"
             style="background:rgba(34,197,94,0.1); border:1px solid rgba(34,197,94,0.25); color:#22c55e; font-size:12px; white-space:nowrap;">
          💰 {{ t('account.walletBalance', { amount: formatPrice(profile.soDuVi ?? 0) }) }}
        </div>
      </div>
    </header>

    <div class="container-xl py-4">

      <!-- Tabs -->
      <div class="d-flex flex-wrap gap-1 mb-4 border-bottom pb-0" style="border-color:var(--border-color)!important;">
        <button v-for="tab in TABS" :key="tab.id"
                class="btn btn-sm fw-black px-3 pb-2 pt-2 rounded-top-3 border-0 d-flex align-items-center gap-2"
                style="font-size:12.5px; letter-spacing:0.02em; border-bottom:3px solid transparent!important; transition:background 0.15s;"
                :style="activeTab === tab.id
                  ? 'border-bottom:3px solid var(--accent)!important; background:rgba(244,63,94,0.06); color:var(--accent-fg);'
                  : 'color:var(--text-secondary);'"
                @mouseenter="e => { if (activeTab !== tab.id) e.currentTarget.style.background = 'var(--bg-hover)'; }"
                @mouseleave="e => { if (activeTab !== tab.id) e.currentTarget.style.background = ''; }"
                @click="activeTab = tab.id">
          <span>{{ tab.icon }}</span>
          {{ tab.label }}
          <span v-if="tabOrderCounts[tab.id]"
                class="badge rounded-pill" style="background:var(--accent); color:var(--accent-text); font-size:10px;">
            {{ tabOrderCounts[tab.id] }}
          </span>
        </button>
      </div>

      <!-- ══ Tab: Chờ xác nhận / Đang giao — thẻ đầy đủ + timeline ══ -->
      <div v-if="activeTab === 'pending' || activeTab === 'shipping'">
        <div v-if="loading" class="d-flex flex-column gap-3">
          <div v-for="i in 2" :key="i" class="rounded-4 p-3" style="background:var(--bg-card); border:1px solid var(--border-color);">
            <Skeleton width="220px" height="18px" class="mb-3" />
            <Skeleton width="100%" height="70px" radius="12px" />
          </div>
        </div>

        <div v-else-if="currentOrders.length === 0"
             class="d-flex flex-column align-items-center justify-content-center text-center rounded-4 py-5"
             style="background:var(--bg-card); border:1px dashed var(--border-color);">
          <div style="font-size:2.6rem; opacity:0.35;">📦</div>
          <div class="fw-bold mt-2" style="color:var(--text-primary); font-size:0.95rem;">{{ t('account.ordersEmptyTitle') }}</div>
          <div class="mt-1" style="color:var(--text-secondary); font-size:12px;">{{ t('account.ordersEmptyDesc') }}</div>
          <button class="btn btn-warning btn-sm fw-bold rounded-pill px-4 mt-3" @click="emit('go-home')">
            🛍️ {{ t('common.continueShopping') }}
          </button>
        </div>

        <div v-else class="d-flex flex-column gap-3">
          <div v-for="o in currentOrders" :key="o.donHangId"
               class="rounded-4 p-3 p-md-4"
               style="background:var(--bg-card); border:1px solid var(--border-color); box-shadow:0 4px 18px var(--shadow-color); transition:border-color 0.15s;"
               @mouseenter="e => e.currentTarget.style.borderColor = 'rgba(244,63,94,0.35)'"
               @mouseleave="e => e.currentTarget.style.borderColor = 'var(--border-color)'">

            <!-- Header đơn -->
            <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3 pb-3"
                 style="border-bottom:1px solid var(--border-color-soft);">
              <div class="d-flex align-items-center gap-2">
                <span style="font-size:1.1rem;">🧾</span>
                <div>
                  <div class="fw-bold" style="color:var(--text-heading); font-size:0.9rem;">{{ t('account.orderCode', { code: o.maDonHang || o.donHangId }) }}</div>
                  <div style="color:var(--text-secondary); font-size:11px;">{{ formatDate(o.ngayDat) }}</div>
                </div>
              </div>
              <span class="badge d-flex align-items-center gap-1 px-3 py-2 rounded-pill fw-semibold"
                    :style="{ background: orderStatusColor(o.trangThaiDonHang).bg, color: orderStatusColor(o.trangThaiDonHang).text }">
                <span style="font-size:11px;">{{ orderStatusIcon(o.trangThaiDonHang) }}</span>
                {{ orderStatusLabel(o.trangThaiDonHang) }}
              </span>
            </div>

            <!-- Timeline -->
            <div class="rounded-3 p-2 p-md-3 mb-3" style="background:var(--bg-card-alt);">
              <OrderStatusTimeline :status="o.trangThaiDonHang" />
            </div>

            <!-- Ngày giao dự kiến / ngày nhận hàng thực tế -->
            <div v-if="o.ngayGiaoDuKien || o.ngayGiaoThucTe"
                 class="d-flex flex-wrap gap-3 mb-3 small" style="color:var(--text-secondary);">
              <span v-if="o.ngayGiaoDuKien">📦 {{ t('account.expectedDelivery') }}: {{ formatDate(o.ngayGiaoDuKien) }}</span>
              <span v-if="o.ngayGiaoThucTe">✅ {{ t('account.actualDelivery') }}: {{ formatDate(o.ngayGiaoThucTe) }}</span>
            </div>

            <!-- Mã vận đơn + lịch sử trạng thái -->
            <OrderTrackingLog v-if="['shipping', 'out_for_delivery'].includes(o.trangThaiDonHang)"
                               :ma-van-don="o.maVanDon || ''"
                               :history="historyByOrder[o.donHangId] || []"
                               class="mb-3" />

            <!-- Danh sách sản phẩm trong đơn -->
            <div class="d-flex flex-column gap-2">
              <div v-for="item in itemsByOrder[o.donHangId] || []" :key="item.id"
                   class="d-flex align-items-center gap-3 p-2 rounded-3"
                   style="background:var(--bg-card-alt); border:1px solid var(--border-color-soft); transition:background 0.15s; cursor:pointer;"
                   @click="viewProductDetail(item)"
                   @mouseenter="e => e.currentTarget.style.background = 'var(--bg-hover)'"
                   @mouseleave="e => e.currentTarget.style.background = 'var(--bg-card-alt)'">
                <div class="rounded-3 d-flex align-items-center justify-content-center flex-shrink-0"
                     style="width:48px; height:40px; background:var(--bg-card-inset); overflow:hidden;">
                  <img v-if="productByBienThe(item.bienTheId)?.hinhAnhChinh"
                       :src="productByBienThe(item.bienTheId).hinhAnhChinh"
                       style="width:100%; height:100%; object-fit:contain; padding:4px;" />
                  <span v-else style="font-size:1.2rem;">💻</span>
                </div>
                <span class="flex-grow-1" style="color:var(--text-primary); font-size:12.5px;">{{ productByBienThe(item.bienTheId)?.tenSanPham || item.maSku }}</span>
                <span class="fw-semibold" style="color:var(--text-secondary); font-size:12px;">x{{ item.soLuong }}</span>
                <span class="fw-bold" style="color:var(--accent-fg); font-size:13px; min-width:100px; text-align:right;">{{ formatPrice(item.thanhTien) }}</span>
              </div>
            </div>

            <!-- Tổng tiền -->
            <div class="d-flex justify-content-end align-items-center gap-2 mt-3 pt-3" style="border-top:1px solid var(--border-color-soft);">
              <span style="color:var(--text-secondary); font-size:12px;">{{ t('account.total') }}</span>
              <span class="fw-black" style="color:var(--accent-fg); font-size:1.05rem;">{{ formatPrice(o.thanhTien ?? o.tongTien) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- ══ Tab: Hoàn tất / Đã hủy-Trả hàng — dòng gọn, bấm mở xem sản phẩm ══ -->
      <div v-else-if="activeTab === 'completed' || activeTab === 'cancelled'">
        <div v-if="loading" class="d-flex flex-column gap-2">
          <Skeleton v-for="i in 3" :key="i" width="100%" height="64px" radius="14px" />
        </div>

        <div v-else-if="historyOrders.length === 0"
             class="d-flex flex-column align-items-center justify-content-center text-center rounded-4 py-5"
             style="background:var(--bg-card); border:1px dashed var(--border-color);">
          <div style="font-size:2.6rem; opacity:0.35;">🕘</div>
          <div class="fw-bold mt-2" style="color:var(--text-primary); font-size:0.95rem;">{{ t('account.historyEmptyTitle') }}</div>
          <div class="mt-1" style="color:var(--text-secondary); font-size:12px;">{{ t('account.historyEmptyDesc') }}</div>
        </div>

        <div v-else class="d-flex flex-column gap-2">
          <div v-for="o in historyOrders" :key="o.donHangId"
               class="rounded-4 p-3"
               style="background:var(--bg-card); border:1px solid var(--border-color); border-left:4px solid; transition:transform 0.15s, box-shadow 0.15s; cursor:pointer;"
               :style="{ borderLeftColor: orderStatusColor(o.trangThaiDonHang).text }"
               @click="toggleHistoryOrder(o.donHangId)"
               @mouseenter="e => { e.currentTarget.style.transform='translateY(-2px)'; e.currentTarget.style.boxShadow='0 6px 20px var(--shadow-color)'; }"
               @mouseleave="e => { e.currentTarget.style.transform=''; e.currentTarget.style.boxShadow=''; }">
            <div class="d-flex flex-wrap justify-content-between align-items-center gap-2">
              <div class="d-flex align-items-center gap-3">
                <span style="font-size:1.3rem; opacity:0.6;">🧾</span>
                <div>
                  <div class="fw-bold" style="color:var(--text-heading); font-size:0.88rem;">{{ t('account.orderCode', { code: o.maDonHang || o.donHangId }) }}</div>
                  <div style="color:var(--text-secondary); font-size:11px;">
                    {{ formatDate(o.ngayDat) }} · {{ (itemsByOrder[o.donHangId] || []).length }} {{ t('account.products') }}
                  </div>
                  <div v-if="o.ngayGiaoThucTe" style="color:var(--text-secondary); font-size:11px;">
                    ✅ {{ t('account.actualDelivery') }}: {{ formatDate(o.ngayGiaoThucTe) }}
                  </div>
                </div>
              </div>
              <div class="d-flex align-items-center gap-3">
                <span class="badge d-flex align-items-center gap-1 px-3 py-2 rounded-pill fw-semibold"
                      :style="{ background: orderStatusColor(o.trangThaiDonHang).bg, color: orderStatusColor(o.trangThaiDonHang).text }">
                  <span style="font-size:11px;">{{ orderStatusIcon(o.trangThaiDonHang) }}</span>
                  {{ orderStatusLabel(o.trangThaiDonHang) }}
                </span>
                <span class="fw-black" style="color:var(--accent-fg); font-size:0.95rem;">{{ formatPrice(o.thanhTien ?? o.tongTien) }}</span>
                <button v-if="o.trangThaiDonHang === 'delivered'"
                        class="btn btn-sm fw-bold rounded-pill px-3"
                        style="background:var(--bg-input); border:1px solid var(--border-color-strong); color:var(--text-primary); font-size:11.5px;"
                        @click.stop="buyAgainOrder(o)">
                  🔁 {{ t('account.buyAgain') }}
                </button>
                <button v-if="canRequestReturn(o)"
                        class="btn btn-sm fw-bold rounded-pill px-3"
                        style="background:var(--bg-input); border:1px solid var(--border-color-strong); color:var(--text-primary); font-size:11.5px;"
                        @click.stop="returnModalOrder = o">
                  ↩️ {{ t('account.requestReturn') }}
                </button>
                <span style="color:var(--text-secondary); font-size:0.8rem;">{{ expandedHistoryOrders.has(o.donHangId) ? '▲' : '▼' }}</span>
              </div>
            </div>

            <!-- Danh sách sản phẩm đã mua — chỉ hiện khi bấm mở đơn -->
            <div v-if="expandedHistoryOrders.has(o.donHangId)"
                 class="rounded-3 mt-3 p-2" style="background:var(--bg-card-inset);"
                 @click.stop>
              <div v-for="(item, idx) in (itemsByOrder[o.donHangId] || [])" :key="item.id"
                   class="d-flex align-items-center gap-3 py-2"
                   style="cursor:pointer;"
                   :style="idx > 0 ? 'border-top:1px solid var(--border-color-soft);' : ''"
                   @click="viewProductDetail(item)">
                <div class="rounded-2 d-flex align-items-center justify-content-center flex-shrink-0"
                     style="width:36px; height:30px; background:var(--bg-card-alt); overflow:hidden;">
                  <img v-if="productByBienThe(item.bienTheId)?.hinhAnhChinh"
                       :src="productByBienThe(item.bienTheId).hinhAnhChinh"
                       style="width:100%; height:100%; object-fit:contain; padding:3px;" />
                  <span v-else style="font-size:1rem;">💻</span>
                </div>
                <span class="flex-grow-1" style="color:var(--text-primary); font-size:12.5px;">{{ productByBienThe(item.bienTheId)?.tenSanPham || item.maSku }}</span>
                <span class="text-secondary" style="font-size:12px;">x{{ item.soLuong }}</span>
                <span class="fw-semibold" style="color:var(--accent-fg); font-size:12.5px; min-width:100px; text-align:right;">{{ formatPrice(item.thanhTien) }}</span>
              </div>
              <OrderTrackingLog v-if="o.trangThaiDonHang === 'delivered'"
                                 :ma-van-don="o.maVanDon || ''"
                                 :history="historyByOrder[o.donHangId] || []"
                                 class="mt-2" />
              <div v-if="(returnsByOrder[o.donHangId] || []).length" class="rounded-3 mt-2 p-2 d-flex flex-column gap-2" style="background:var(--bg-card-alt);">
                <div v-for="r in returnsByOrder[o.donHangId]" :key="r.phieuTraId" class="d-flex flex-column gap-1">
                  <div class="d-flex align-items-center gap-2">
                    <span class="badge px-2 py-1 rounded-pill fw-semibold"
                          :style="{ background: RETURN_STATUS_COLOR[r.trangThai]?.bg, color: RETURN_STATUS_COLOR[r.trangThai]?.text }">
                      {{ t(RETURN_STATUS_LABEL[r.trangThai]) }}
                    </span>
                    <span style="font-size:11px; color:var(--text-secondary);">{{ formatPrice(r.soTienHoan) }}</span>
                  </div>
                  <div style="font-size:12px; color:var(--text-primary);">{{ r.lyDo }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ══ Tab: Vòng quay may mắn ══ -->
      <div v-else-if="activeTab === 'wheel'" class="d-flex flex-column mx-auto" style="max-width:640px;">
        <LuckyWheelPanel :points="profile?.diemTichLuy ?? 0" @spun="fetchProfile" />
      </div>

      <!-- ══ Tab: Cài đặt tài khoản ══ -->
      <div v-else class="d-flex flex-column gap-3 mx-auto" style="max-width:640px;">

        <!-- Điểm & Voucher -->
        <div class="rounded-4 p-4" style="background:var(--bg-card); border:1px solid var(--border-color); box-shadow:0 4px 18px var(--shadow-color);">
          <div class="d-flex align-items-center gap-2 mb-4">
            <span style="font-size:1.3rem;">🎁</span>
            <div>
              <h5 class="fw-black mb-0" style="color:var(--text-heading);">{{ t('account.rewards.heading') }}</h5>
              <div style="color:var(--text-secondary); font-size:11.5px;">{{ t('account.rewards.subtitle', { points: profile?.diemTichLuy ?? 0 }) }}</div>
            </div>
          </div>

          <div v-if="redeemError" class="alert alert-danger small py-2 mb-3">{{ redeemError }}</div>

          <div class="mb-2 small fw-semibold" style="color:var(--text-secondary);">{{ t('account.rewards.catalogHeading') }}</div>
          <div class="d-flex flex-column gap-2 mb-4">
            <div v-for="r in rewards.filter(x => x.trangThai === 'active')" :key="r.doiThuongId"
                 class="d-flex align-items-center justify-content-between p-2 rounded-3" style="background:var(--bg-card-inset);">
              <div>
                <div class="fw-semibold" style="font-size:13px; color:var(--text-primary);">{{ r.ten }}</div>
                <div style="font-size:11px; color:var(--text-secondary);">{{ t('account.rewards.pointsCost', { points: r.diemCan }) }}</div>
              </div>
              <button class="btn btn-sm fw-bold rounded-pill px-3"
                      :disabled="(profile?.diemTichLuy ?? 0) < r.diemCan || redeemingId === r.doiThuongId"
                      style="background:var(--bg-input); border:1px solid var(--border-color-strong); color:var(--text-primary); font-size:11.5px;"
                      @click="redeemReward(r)">
                {{ t('account.rewards.redeemButton') }}
              </button>
            </div>
            <div v-if="rewards.filter(x => x.trangThai === 'active').length === 0" class="small" style="color:var(--text-secondary);">{{ t('account.rewards.catalogEmpty') }}</div>
          </div>

          <div class="mb-2 small fw-semibold" style="color:var(--text-secondary);">{{ t('account.rewards.myVouchersHeading') }}</div>
          <div class="d-flex flex-column gap-2">
            <div v-for="v in myVouchers" :key="v.phieuId"
                 class="d-flex align-items-center justify-content-between p-2 rounded-3" style="background:var(--bg-card-inset);">
              <div>
                <div class="fw-bold" style="font-size:12.5px; color:var(--text-primary);">{{ v.maPhieu }}</div>
                <div style="font-size:11px; color:var(--text-secondary);">{{ t('account.rewards.expiresOn', { date: formatDate(v.ngayHetHan) }) }}</div>
                <div v-if="v.donHangToiThieu" style="font-size:11px; color:var(--text-secondary);">{{ t('account.rewards.minOrder', { amount: formatPrice(v.donHangToiThieu) }) }}</div>
              </div>
              <span class="badge px-2 py-1 rounded-pill fw-semibold"
                    :style="v.daSuDung ? 'background:rgba(107,114,128,0.15);color:#9ca3af;' : 'background:rgba(34,197,94,0.15);color:#22c55e;'">
                {{ v.daSuDung ? t('account.rewards.used') : t('account.rewards.available') }}
              </span>
            </div>
            <div v-if="myVouchers.length === 0" class="small" style="color:var(--text-secondary);">{{ t('account.rewards.myVouchersEmpty') }}</div>
          </div>
        </div>

        <!-- Thông tin cá nhân -->
        <div class="rounded-4 p-4" style="background:var(--bg-card); border:1px solid var(--border-color); box-shadow:0 4px 18px var(--shadow-color);">
          <div class="d-flex align-items-center gap-2 mb-4">
            <span style="font-size:1.3rem;">👤</span>
            <div>
              <h5 class="fw-black mb-0" style="color:var(--text-heading);">{{ t('account.settings.heading') }}</h5>
              <div style="color:var(--text-secondary); font-size:11.5px;">{{ t('account.settings.subtitle') }}</div>
            </div>
          </div>

          <div v-if="profileLoading" class="d-flex flex-column gap-3">
            <Skeleton v-for="i in 4" :key="i" width="100%" height="38px" radius="10px" />
          </div>

          <form v-else @submit.prevent="saveProfile" class="d-flex flex-column gap-3">
            <div>
              <label class="form-label small fw-semibold" style="color:var(--text-secondary);">🧑 {{ t('account.settings.fullName') }}</label>
              <input v-model="profileForm.hoTen" type="text" required
                     class="form-control"
                     style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-primary); border-radius:10px; padding:0.55rem 0.85rem;" />
            </div>

            <div class="row g-3">
              <div class="col-md-6">
                <label class="form-label small fw-semibold" style="color:var(--text-secondary);">📱 {{ t('account.settings.phone') }}</label>
                <input v-model="profileForm.soDienThoai" type="text" required
                       class="form-control"
                       style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-primary); border-radius:10px; padding:0.55rem 0.85rem;" />
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-semibold" style="color:var(--text-secondary);">✉️ {{ t('account.settings.email') }}</label>
                <input v-model="profileForm.email" type="email" required
                       class="form-control"
                       style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-primary); border-radius:10px; padding:0.55rem 0.85rem;" />
              </div>
            </div>

            <div>
              <label class="form-label small fw-semibold" style="color:var(--text-secondary);">📍 {{ t('account.settings.address') }}</label>
              <input v-model="profileForm.diaChi" type="text" required
                     class="form-control"
                     style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-primary); border-radius:10px; padding:0.55rem 0.85rem;" />
            </div>

            <div v-if="profileError" class="alert alert-danger small py-2 mb-0 rounded-3">⚠️ {{ profileError }}</div>
            <div v-if="profileSuccess" class="alert alert-success small py-2 mb-0 rounded-3">✅ {{ profileSuccess }}</div>

            <div class="d-flex justify-content-end pt-2" style="border-top:1px solid var(--border-color-soft);">
              <button type="submit" class="btn btn-warning fw-bold rounded-pill px-4 py-2 mt-3" :disabled="profileSaving"
                      style="font-size:0.85rem;">
                {{ profileSaving ? '⏳ ' + t('common.saving') : '💾 ' + t('common.save') }}
              </button>
            </div>
          </form>
        </div>

      </div>

    </div>
  </div>

  <!-- Chi tiết sản phẩm đã mua — bấm vào 1 sản phẩm trong đơn để xem lại + mua lại -->
  <ProductDetail
    v-if="selectedProductDetail"
    :key="selectedProductDetail.bienTheId"
    :product="selectedProductDetail"
    :products="products"
    @close="selectedProductDetail = null"
    @add-to-cart="p => { emit('add-to-cart', p); selectedProductDetail = null; }"
    @open-product="p => selectedProductDetail = p"
  />

  <!-- Yêu cầu trả hàng — chọn sản phẩm + số lượng muốn trả -->
  <ReturnRequestModal v-if="returnModalOrder"
                       :order="returnModalOrder"
                       :items="itemsByOrder[returnModalOrder.donHangId] || []"
                       @close="returnModalOrder = null"
                       @submitted="returnModalOrder = null; fetchData();" />
</template>
