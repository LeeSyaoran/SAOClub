<script setup>
// ========================================================
// AccountPage.vue — Trang tài khoản khách hàng
// Gồm 3 tab: Đơn hàng hiện tại | Lịch sử mua hàng | Cài đặt tài khoản
// Emit ra: go-home
// ========================================================
import { ref, computed, onMounted } from "vue";
import { AuthStore, setSession } from "../stores/index.js";
import { I18nStore, t } from "../i18n/index.js";
import { orderStatusLabel, orderStatusColor } from "../utils/orderStatus.js";
import * as DonHangService         from "../Service/DonHangService.js";
import * as ChiTietDonHangService  from "../Service/ChiTietDonHangService.js";
import * as SanPhamService         from "../Service/SanPhamService.js";
import * as KhachHangService       from "../Service/KhachHangService.js";
import OrderStatusTimeline from "../components/order/OrderStatusTimeline.vue";
import Skeleton from "../components/common/Skeleton.vue";

const emit = defineEmits(["go-home"]);

const auth = AuthStore;
const activeTab = ref("orders"); // 'orders' | 'history' | 'settings'

const TABS = computed(() => [
  { id: "orders",   icon: "📦", label: t("account.tabOrders") },
  { id: "history",  icon: "🕘", label: t("account.tabHistory") },
  { id: "settings", icon: "⚙️", label: t("account.tabSettings") },
]);

// ── Dữ liệu ─────────────────────────────────────────────────────────────────
const orders      = ref([]);   // toàn bộ đơn hàng của khách hàng này
const products    = ref([]);   // danh sách bienThe/sanPham (để map ảnh + tên)
const itemsByOrder = ref({});  // { [donHangId]: ChiTietDonHangResponse[] }
const loading      = ref(false);

const HOAN_TAT_STATUSES = ["delivered", "cancelled", "returned"];

// Đơn hàng đang xử lý (chưa hoàn tất)
const currentOrders = computed(() =>
  orders.value.filter(o => !HOAN_TAT_STATUSES.includes(o.trangThaiDonHang))
);

// Lịch sử mua hàng (đã giao / đã hủy / đã trả)
const historyOrders = computed(() =>
  orders.value.filter(o => HOAN_TAT_STATUSES.includes(o.trangThaiDonHang))
);

const productByBienThe = (bienTheId) =>
  products.value.find(p => p.bienTheId === bienTheId);

const fetchData = async () => {
  loading.value = true;
  try {
    const [allOrders, allProducts] = await Promise.all([
      DonHangService.getAll().catch(() => []),
      SanPhamService.getAll().catch(() => []),
    ]);
    products.value = allProducts;
    orders.value = allOrders
      .filter(o => o.khachHangId === auth.user?.id)
      .sort((a, b) => new Date(b.ngayDat) - new Date(a.ngayDat));

    const entries = await Promise.all(
      orders.value.map(async (o) => [
        o.donHangId,
        await ChiTietDonHangService.getByDonHang(o.donHangId).catch(() => []),
      ])
    );
    itemsByOrder.value = Object.fromEntries(entries);
  } finally {
    loading.value = false;
  }
};

// ── Trạng thái đơn hàng: nhãn + màu (dùng chung — xem src/utils/orderStatus.js) ──

// Map trạng thái đơn hàng → bước trên timeline (0..3)
const orderStep = (s) => ({
  pending: 0, confirmed: 1, processing: 1, shipping: 2, delivered: 3,
})[s] ?? 0;

const formatPrice = (v) =>
  v == null ? "—" : new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(v);

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
  } catch (e) {
    profileError.value = e.message || t("account.settings.loadError");
  } finally {
    profileLoading.value = false;
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

onMounted(() => {
  fetchData();
  fetchProfile();
});
</script>

<template>
  <div style="min-height:100vh; background:var(--bg-page); color:var(--text-primary); font-family:'Inter','Segoe UI',sans-serif;">

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
               style="width:44px; height:44px; background:linear-gradient(135deg,var(--accent),var(--accent-2)); color:var(--accent-text); font-size:1.05rem; box-shadow:0 4px 14px rgba(250,204,21,0.25);">
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
             style="background:rgba(250,204,21,0.1); border:1px solid rgba(250,204,21,0.25); color:var(--accent); font-size:12px; white-space:nowrap;">
          🎁 {{ t('account.points', { points: profile.diemTichLuy ?? 0 }) }}
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
                  ? 'border-bottom:3px solid var(--accent)!important; background:rgba(250,204,21,0.06); color:var(--accent);'
                  : 'color:var(--text-secondary);'"
                @mouseenter="e => { if (activeTab !== tab.id) e.currentTarget.style.background = 'var(--bg-hover)'; }"
                @mouseleave="e => { if (activeTab !== tab.id) e.currentTarget.style.background = ''; }"
                @click="activeTab = tab.id">
          <span>{{ tab.icon }}</span>
          {{ tab.label }}
          <span v-if="tab.id === 'orders' && currentOrders.length"
                class="badge rounded-pill" style="background:var(--accent); color:var(--accent-text); font-size:10px;">
            {{ currentOrders.length }}
          </span>
        </button>
      </div>

      <!-- ══ Tab: Đơn hàng hiện tại ══ -->
      <div v-if="activeTab === 'orders'">
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
               @mouseenter="e => e.currentTarget.style.borderColor = 'rgba(250,204,21,0.35)'"
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
                <span style="font-size:8px;">●</span>
                {{ orderStatusLabel(o.trangThaiDonHang) }}
              </span>
            </div>

            <!-- Timeline -->
            <div class="rounded-3 p-2 p-md-3 mb-3" style="background:var(--bg-card-alt);">
              <OrderStatusTimeline :current-step="orderStep(o.trangThaiDonHang)" />
            </div>

            <!-- Danh sách sản phẩm trong đơn -->
            <div class="d-flex flex-column gap-2">
              <div v-for="item in itemsByOrder[o.donHangId] || []" :key="item.id"
                   class="d-flex align-items-center gap-3 p-2 rounded-3"
                   style="background:var(--bg-card-alt); border:1px solid var(--border-color-soft); transition:background 0.15s;"
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
                <span class="fw-bold" style="color:var(--accent); font-size:13px; min-width:100px; text-align:right;">{{ formatPrice(item.thanhTien) }}</span>
              </div>
            </div>

            <!-- Tổng tiền -->
            <div class="d-flex justify-content-end align-items-center gap-2 mt-3 pt-3" style="border-top:1px solid var(--border-color-soft);">
              <span style="color:var(--text-secondary); font-size:12px;">{{ t('account.total') }}</span>
              <span class="fw-black" style="color:var(--accent); font-size:1.05rem;">{{ formatPrice(o.thanhTien ?? o.tongTien) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- ══ Tab: Lịch sử mua hàng ══ -->
      <div v-else-if="activeTab === 'history'">
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
               class="rounded-4 p-3 d-flex flex-wrap justify-content-between align-items-center gap-2"
               style="background:var(--bg-card); border:1px solid var(--border-color); border-left:4px solid; transition:transform 0.15s, box-shadow 0.15s;"
               :style="{ borderLeftColor: orderStatusColor(o.trangThaiDonHang).text }"
               @mouseenter="e => { e.currentTarget.style.transform='translateY(-2px)'; e.currentTarget.style.boxShadow='0 6px 20px var(--shadow-color)'; }"
               @mouseleave="e => { e.currentTarget.style.transform=''; e.currentTarget.style.boxShadow=''; }">
            <div class="d-flex align-items-center gap-3">
              <span style="font-size:1.3rem; opacity:0.6;">🧾</span>
              <div>
                <div class="fw-bold" style="color:var(--text-heading); font-size:0.88rem;">{{ t('account.orderCode', { code: o.maDonHang || o.donHangId }) }}</div>
                <div style="color:var(--text-secondary); font-size:11px;">
                  {{ formatDate(o.ngayDat) }} · {{ (itemsByOrder[o.donHangId] || []).length }} {{ t('account.products') }}
                </div>
              </div>
            </div>
            <div class="d-flex align-items-center gap-3">
              <span class="badge d-flex align-items-center gap-1 px-3 py-2 rounded-pill fw-semibold"
                    :style="{ background: orderStatusColor(o.trangThaiDonHang).bg, color: orderStatusColor(o.trangThaiDonHang).text }">
                <span style="font-size:8px;">●</span>
                {{ orderStatusLabel(o.trangThaiDonHang) }}
              </span>
              <span class="fw-black" style="color:var(--accent); font-size:0.95rem;">{{ formatPrice(o.thanhTien ?? o.tongTien) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- ══ Tab: Cài đặt tài khoản ══ -->
      <div v-else class="d-flex flex-column gap-3 mx-auto" style="max-width:640px;">

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
</template>
