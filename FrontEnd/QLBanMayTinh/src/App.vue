<script setup>
import {
  ref,
  computed,
  reactive,
  watch,
  onMounted,
  onBeforeUnmount,
  provide,
} from "vue";
import { useRouter } from "vue-router";
import { useHead } from "@unhead/vue";
import { CheckCircle2, XCircle, Info } from '@lucide/vue';

useHead({
  title: "SAOPhone",
  meta: [
    { name: "description", content: "Cửa hàng laptop & thiết bị công nghệ" },
    { name: "viewport", content: "width=device-width, initial-scale=1" },
  ],
});

import { AuthStore, setSession, clearSession } from "./stores/index.js";
import { resetAllStores } from "./stores/resetAll.js";
import { loadSettings, SettingsStore } from "./stores/settings.js";
import { formatPrice as formatPriceRaw } from "./utils/formatPrice.js";
import { t, applySystemDefaultLocale } from "./i18n/index.js";

import * as SanPhamService from "./services/SanPhamService.js";
import * as AuthService from "./services/AuthService.js";
import * as YeuThichService from "./services/YeuThichService.js";
import * as DanhGiaService from "./services/DanhGiaService.js";

import LoginForm from "./components/auth/LoginForm.vue";
import RegisterForm from "./components/auth/RegisterForm.vue";
import CheckoutModal from "./components/checkout/CheckoutModal.vue";
import ProductDetail from "./components/product/ProductDetail.vue";
import Modal from "./components/common/Modal.vue";

const router = useRouter();

const auth = AuthStore;

// ── Toast notification ────────────────────────────────────────────────────────
const toast = reactive({ show: false, msg: "", type: "success" });
let toastTimer = null;
const showToast = (msg, type = "success") => {
  clearTimeout(toastTimer);
  toast.msg = msg;
  toast.type = type;
  toast.show = true;
  toastTimer = setTimeout(() => {
    toast.show = false;
  }, 3500);
};

// ── Login modal ───────────────────────────────────────────────────────────────
const showLoginModal = ref(false);
const loginModalErr = ref("");
const authTab = ref("login");

const openLogin = () => {
  loginModalErr.value = "";
  authTab.value = "login";
  showLoginModal.value = true;
};

const onRegisterSuccess = (newAccount) => {
  if (newAccount?.khachHangId != null) {
    localStorage.removeItem(`saophone_cart_${newAccount.khachHangId}`);
  }
  authTab.value = "login";
  showToast(t("register.success"), "success");
};

const handleModalLogin = async ({ username, password }) => {
  loginModalErr.value = "";
  try {
    const res = await AuthService.login(username, password);
    if (!res.ok) {
      const msg = await res.text();
      loginModalErr.value = msg;
      showToast(msg || t("toast.loginFailed"), "error");
      return;
    }
    const user = await res.json();
    showLoginModal.value = false;
    showToast(t("toast.welcomeUser", { name: user.hoTen }), "success");
    onLoginSuccess(user);
  } catch {
    loginModalErr.value = t("toast.cannotConnect");
    showToast(t("toast.cannotConnect"), "error");
  }
};

// ── Logout ────────────────────────────────────────────────────────────────────
const onLogout = () => {
  clearSession();
  resetAllStores();
  loadCart();
  loadWishlist();
  showCart.value = false;
  showToast(t("toast.loggedOut"), "info");
  router.push("/");
};

// ── Cart ──────────────────────────────────────────────────────────────────────

const cart = ref([]);
const showCart = ref(false);

const cartStorageKey = () => `saophone_cart_${auth.user?.id ?? "guest"}`;

const loadCart = () => {
  try {
    const raw = localStorage.getItem(cartStorageKey());
    cart.value = raw ? JSON.parse(raw) : [];
  } catch {
    cart.value = [];
  }
};

const saveCart = () => {
  localStorage.setItem(cartStorageKey(), JSON.stringify(cart.value));
};

watch(cart, saveCart, { deep: true });

const cartCount = computed(() =>
  cart.value.reduce((total, item) => total + item.quantity, 0),
);

const cartTotal = computed(() =>
  cart.value.reduce(
    (total, item) => total + (item.quantity || 0) * (item.giaBan || 0),
    0,
  ),
);

const toggleCart = () => {
  showCart.value = !showCart.value;
};

const removeFromCart = (bienTheId) => {
  cart.value = cart.value.filter((item) => item.bienTheId !== bienTheId);
};

const formatPrice = (value) => (value == null ? t("productDetail.contact") : formatPriceRaw(value));

const updateQty = (bienTheId, delta) => {
  const item = cart.value.find((i) => i.bienTheId === bienTheId);
  if (!item) return;
  const ton = item.soLuongTon ?? Infinity;
  const newQty = Math.min(item.quantity + delta, ton);
  if (newQty <= 0)
    cart.value = cart.value.filter((i) => i.bienTheId !== bienTheId);
  else item.quantity = newQty;
};

const addToCart = (product, qty = 1) => {
  if (!auth.user) {
    showToast(t("toast.loginRequiredForCart"), "error");
    openLogin();
    return;
  }
  const ton = product.soLuongTon ?? Infinity;
  if (ton <= 0) {
    showToast(t("toast.outOfStock", { name: product.tenSanPham }), "error");
    return;
  }
  const existing = cart.value.find(
    (item) => item.bienTheId === product.bienTheId,
  );
  const soLuongMuon = existing ? existing.quantity + qty : qty;
  const daDatToiHan = soLuongMuon > ton;
  const soLuongCuoi = Math.min(soLuongMuon, ton);
  if (existing) existing.quantity = soLuongCuoi;
  else cart.value.push({ ...product, quantity: soLuongCuoi });

  if (daDatToiHan) {
    showToast(t("toast.maxStockReached", { name: product.tenSanPham, ton }), "error");
  } else {
    showToast(t("toast.addedToCart", { name: product.tenSanPham }), "success");
  }
};

const onBuyAgainUnavailable = (names) => {
  showToast(t("toast.buyAgainUnavailable", { names: names.join(", ") }), "error");
};

// ── Wishlist (yêu thích) — lưu ở backend theo khách hàng (không phải localStorage như giỏ
// hàng), nên cần load lại mỗi khi đăng nhập/đăng xuất. Set để tra cứu isWishlisted() O(1)
// trên lưới sản phẩm thay vì .find() O(n) trên mảng mỗi lần render 1 thẻ. ────────────────
const wishlistIds = ref(new Set());

const loadWishlist = async () => {
  if (!auth.user) { wishlistIds.value = new Set(); return; }
  try {
    const list = await YeuThichService.getAll();
    wishlistIds.value = new Set(list.map((i) => i.bienTheId));
  } catch {
    wishlistIds.value = new Set();
  }
};

const isWishlisted = (bienTheId) => wishlistIds.value.has(bienTheId);

const toggleWishlist = async (product) => {
  if (!auth.user) {
    showToast(t("toast.loginRequiredForWishlist"), "error");
    openLogin();
    return;
  }
  const daThich = wishlistIds.value.has(product.bienTheId);
  try {
    if (daThich) {
      await YeuThichService.remove(product.bienTheId);
      wishlistIds.value.delete(product.bienTheId);
      showToast(t("toast.removedFromWishlist", { name: product.tenSanPham }), "info");
    } else {
      await YeuThichService.add(product.bienTheId);
      wishlistIds.value.add(product.bienTheId);
      showToast(t("toast.addedToWishlist", { name: product.tenSanPham }), "success");
    }
  } catch (e) {
    showToast(e.message, "error");
  }
};

// ── Đánh giá (rating summary) — công khai, không phụ thuộc đăng nhập nên chỉ load 1 lần lúc
// mount (khác wishlist phải reload theo phiên). Map để ProductCard tra cứu O(1) theo sanPhamId. ──
const ratingSummaries = ref(new Map());

const loadRatingSummaries = async () => {
  try {
    const list = await DanhGiaService.getTongHop();
    ratingSummaries.value = new Map(list.map((s) => [s.sanPhamId, s]));
  } catch {
    // giữ nguyên map cũ nếu lỗi mạng
  }
};

// ── Checkout ──────────────────────────────────────────────────────────────────
const showCheckout = ref(false);

const openCheckout = () => {
  if (cart.value.length === 0) return;
  if (!auth.user) {
    showToast(t("toast.loginRequiredForCart"), "error");
    openLogin();
    return;
  }
  showCart.value = false;
  showCheckout.value = true;
};

const handleOrderPlaced = () => {
  cart.value = [];
};

// ── Products (shared state for ProductDetail overlay) ─────────────────────────
const products = ref([]);

const fetchProducts = async () => {
  try {
    products.value = await SanPhamService.getAll();
  } catch {
    // handled silently
  }
};

const selectedProduct = ref(null);

const openProduct = (p) => {
  selectedProduct.value = p;
  history.pushState({ view: "product", bienTheId: p.bienTheId }, "");
};

const closeProduct = () => {
  selectedProduct.value = null;
};

const onPopState = (event) => {
  const state = event.state;
  if (state?.view === "product") {
    selectedProduct.value =
      products.value.find((p) => p.bienTheId === state.bienTheId) || null;
  } else {
    selectedProduct.value = null;
    showCart.value = false;
    showCheckout.value = false;
    showLoginModal.value = false;
  }
};

// ── Login success ─────────────────────────────────────────────────────────────
function onLoginSuccess(user) {
  setSession(user);
  loadCart();
  loadWishlist();
  const ROLE_PATH = { admin: "/admin", nhan_vien: "/staff", quan_kho: "/kho" };
  router.push(ROLE_PATH[user.role] ?? "/");
}

// ── Provide shared state & actions to child route components ──────────────────
provide("appState", {
  products,
  cart,
  showCart,
  cartCount,
  cartTotal,
  showCheckout,
  selectedProduct,
  auth,
  wishlistIds,
  ratingSummaries,
});

provide("appActions", {
  addToCart,
  removeFromCart,
  updateQty,
  toggleCart,
  openCheckout,
  handleOrderPlaced,
  openProduct,
  closeProduct,
  showToast,
  openLogin,
  onLogout,
  onBuyAgainUnavailable,
  fetchProducts,
  formatPrice,
  isWishlisted,
  toggleWishlist,
  loadWishlist,
});

// ── Lifecycle hooks ───────────────────────────────────────────────────────────
onMounted(async () => {
  window.addEventListener("popstate", onPopState);
  loadCart();
  loadWishlist();
  loadRatingSummaries();
  await loadSettings();
  applySystemDefaultLocale(SettingsStore.ngonNguMacDinh);
});

onBeforeUnmount(() => {
  window.removeEventListener("popstate", onPopState);
});
</script>

<template>
  <div>
    <router-view v-slot="{ Component }">
      <component
        :is="Component"
        @add-to-cart="addToCart"
        @buy-again-unavailable="onBuyAgainUnavailable"
        @toast="(msg, type) => showToast(msg, type)"
        @go-home="() => router.push('/')"
      />
    </router-view>

    <!-- ══════════════════════════════════════════════════════
        TOAST NOTIFICATION
    ══════════════════════════════════════════════════════ -->
    <Transition name="toast-slide">
      <div
        v-if="toast.show"
        class="position-fixed d-flex align-items-center gap-2 px-4 py-3 rounded-3 fw-semibold small shadow-lg"
        style="
          top: 80px;
          right: 24px;
          z-index: 9999;
          min-width: 260px;
          max-width: 380px;
          pointer-events: none;
        "
        :style="
          toast.type === 'success'
            ? 'background:#16a34a; color:#fff;'
            : toast.type === 'error'
              ? 'background:#dc2626; color:#fff;'
              : 'background:#2563eb; color:#fff;'
        "
      >
        <span style="flex-shrink: 0">
          <component
            :is="toast.type === 'success' ? CheckCircle2 : toast.type === 'error' ? XCircle : Info"
            :size="18"
          />
        </span>
        {{ toast.msg }}
      </div>
    </Transition>

    <!-- ══════════════════════════════════════════════════════
        LOGIN MODAL
    ══════════════════════════════════════════════════════ -->
    <Modal v-model="showLoginModal">
      <LoginForm
        v-if="authTab === 'login'"
        @submit="handleModalLogin"
        @open-register="authTab = 'register'"
      />
      <RegisterForm
        v-else
        @open-login="authTab = 'login'"
        @register-success="onRegisterSuccess"
      />
      <div
        v-if="authTab === 'login' && loginModalErr"
        class="alert alert-danger small py-2 mt-2 mb-0 rounded-3"
      >
        {{ loginModalErr }}
      </div>
    </Modal>

    <!-- ══════════════════════════════════════════════════════
        CHECKOUT MODAL
    ══════════════════════════════════════════════════════ -->
    <CheckoutModal
      v-model="showCheckout"
      :cart="cart"
      :cart-total="cartTotal"
      @order-placed="handleOrderPlaced"
    />

    <!-- ══════════════════════════════════════════════════════
        PRODUCT DETAIL OVERLAY
    ══════════════════════════════════════════════════════ -->
    <Transition name="slide-up">
      <ProductDetail
        v-if="selectedProduct"
        :key="selectedProduct.bienTheId"
        :product="selectedProduct"
        :products="products"
        :wishlist-ids="wishlistIds"
        :auth-user="auth.user"
        @close="closeProduct"
        @add-to-cart="
          (p) => {
            addToCart(p);
            closeProduct();
          }
        "
        @open-product="openProduct"
        @toggle-wishlist="toggleWishlist"
      />
    </Transition>
  </div>
</template>

<style>
.slide-up-enter-active,
.slide-up-leave-active {
  transition:
    transform 0.28s ease,
    opacity 0.2s ease;
}
.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(30px);
  opacity: 0;
}

.cart-slide-enter-active,
.cart-slide-leave-active {
  transition:
    transform 0.25s ease,
    opacity 0.2s ease;
}
.cart-slide-enter-from,
.cart-slide-leave-to {
  transform: translateX(100%);
  opacity: 0;
}

.fade-enter-active,
.fade-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

.toast-slide-enter-active,
.toast-slide-leave-active {
  transition:
    transform 0.3s ease,
    opacity 0.25s ease;
}
.toast-slide-enter-from,
.toast-slide-leave-to {
  transform: translateX(110%);
  opacity: 0;
}
</style>
