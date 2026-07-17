<script setup>
// ── Import các thư viện Vue 3 cần thiết ──────────────────────────────────────
import {
  ref,
  computed,
  reactive,
  watch,
  onMounted,
  onBeforeUnmount,
} from "vue";

// Import store xác thực
import { AuthStore, setSession, clearSession } from "./stores/index.js";
import { loadSettings } from "./stores/settings.js";
import { formatPrice as formatPriceRaw } from "./utils/formatPrice.js";
import { t } from "./i18n/index.js";

// Import services
import * as SanPhamService from "./Service/SanPhamService.js";
import * as DanhMucService from "./Service/DanhMucService.js";
import * as AuthService from "./Service/AuthService.js";

// Import các component trang
import AdminPage from "./pages/AdminPage.vue";
import AccountPage from "./pages/AccountPage.vue";
import LoginForm from "./components/auth/LoginForm.vue";
import RegisterForm from "./components/auth/RegisterForm.vue";
import NavBar from "./components/layout/NavBar.vue";
import AppFooter from "./components/layout/Footer.vue";
import ProductFilter from "./components/product/ProductFilter.vue";
import ProductDetail from "./components/product/ProductDetail.vue";
import ProductCard from "./components/product/ProductCard.vue";
import CartItem from "./components/cart/CartItem.vue";
import CartSummary from "./components/cart/CartSummary.vue";
import CheckoutModal from "./components/checkout/CheckoutModal.vue";
import Modal from "./components/common/Modal.vue";

// ── State & Store ─────────────────────────────────────────────────────────────

// Lấy object auth từ store (reactive)
const auth = AuthStore;

// Theo dõi fragment URL hiện tại (ví dụ: "#admin")
const currentHash = ref(window.location.hash);

// Computed: kiểm tra có đang ở route admin không
const isAdminHash = computed(() => currentHash.value === "#admin");

// Computed: kiểm tra có đang ở route tài khoản khách hàng không
const isAccountHash = computed(() => currentHash.value === "#account");

// ── Dữ liệu sản phẩm ─────────────────────────────────────────────────────────

const products = ref([]); // Danh sách toàn bộ sản phẩm từ API
const loading = ref(false); // Đang tải dữ liệu hay không
const error = ref(null); // Thông báo lỗi nếu có
const searchQuery = ref("");
const selectedSort = ref("default");

// ── Lọc nâng cao (ProductFilter) ──────────────────────────────────────────────
const showAdvFilter = ref(false);
const advFilter = reactive({
  brands: [],
  priceMin: null,
  priceMax: null,
  category: null,
});

// Danh mục thực từ API — dùng để map chip → danhMucId chính xác
const apiCats = ref([]);
const fetchApiCats = async () => {
  apiCats.value = await DanhMucService.getAll().catch(() => []);
};
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
const authTab = ref("login"); // 'login' | 'register' — tab đang hiện trong modal

const openLogin = () => {
  loginModalErr.value = "";
  authTab.value = "login";
  showLoginModal.value = true;
};

// Đăng ký thành công → quay về tab đăng nhập để dùng tài khoản vừa tạo
const onRegisterSuccess = () => {
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
  loadCart(); // Chuyển về giỏ hàng của khách vãng lai (giỏ hàng của tài khoản vẫn được lưu lại)
  showCart.value = false;
  showToast(t("toast.loggedOut"), "info");
  window.location.hash = "";
};

// Danh sách thương hiệu duy nhất từ data sản phẩm đã load
const allBrands = computed(() => [
  ...new Set(products.value.map((p) => p.tenThuongHieu).filter(Boolean)),
]);

// Danh sách danh mục duy nhất từ data sản phẩm đã load
const allCategories = computed(() => {
  const seen = new Set();
  return products.value
    .filter(
      (p) =>
        p.danhMucId &&
        p.tenDanhMuc &&
        !seen.has(p.danhMucId) &&
        seen.add(p.danhMucId),
    )
    .map((p) => ({ id: p.danhMucId, tenDanhMuc: p.tenDanhMuc }));
});

const onAdvFilterChange = (f) => {
  advFilter.brands = f.brands;
  advFilter.priceMin = f.priceMin;
  advFilter.priceMax = f.priceMax;
  advFilter.category = f.category;
};

// ── Deal section state ────────────────────────────────────────────────────────

const activeTab = ref("deal");
const activeFilter = ref("all"); // id của chip đang active
const activeCatId = ref(null); // danhMucId từ sidebar
const activeSidebarCat = ref(null); // full object sidebar cat (có keywords cho fallback)

// Tabs: DEAL SỐC | HOT TREND | MÁY MỚI (nhãn dịch theo ngôn ngữ hiện tại)
const dealTabs = computed(() => [
  { id: "deal", label: t("home.dealTabDeal") },
  { id: "hot", label: t("home.dealTabHot") },
  { id: "new", label: t("home.dealTabNew") },
]);

// Chip filter — id ổn định để logic lọc không phụ thuộc ngôn ngữ hiển thị
const dealFilters = computed(() => [
  { id: "all", label: t("home.chipAll") },
  { id: "gaming", label: t("home.chipGaming") },
  { id: "office", label: t("home.chipOffice") },
  { id: "macbook", label: t("home.chipMacbook") },
  { id: "graphics", label: t("home.chipGraphics") },
]);

// Sidebar: keywords dùng cả cho apiCats lookup VÀ phanLoaiTags fallback
const sidebarCatsBase = computed(() => [
  {
    id: "office",
    icon: "💻",
    name: t("home.sidebar.office"),
    keywords: ["van_phong", "sinh_vien", "văn phòng", "sinh viên"],
  },
  {
    id: "gaming",
    icon: "🎮",
    name: t("home.sidebar.gaming"),
    keywords: ["gaming"],
  },
  {
    id: "graphics",
    icon: "⚡",
    name: t("home.sidebar.graphics"),
    keywords: ["do_hoa", "ky_thuat", "đồ họa", "kỹ thuật"],
  },
  {
    id: "macbook",
    icon: "🍎",
    name: t("home.sidebar.macbook"),
    keywords: ["macbook", "apple"],
  },
  {
    id: "used",
    icon: "⭐",
    name: t("home.sidebar.used"),
    keywords: ["cu", "gia_re", "cũ", "rẻ"],
  },
  {
    id: "parts",
    icon: "🔧",
    name: t("home.sidebar.parts"),
    keywords: ["linh_kien", "ram", "ssd", "linh kiện"],
  },
]);

// Map mỗi sidebar item → catId thực từ apiCats (null nếu chưa có trong DB)
const sidebarCats = computed(() =>
  sidebarCatsBase.value.map((sc) => {
    const matched = apiCats.value.find((c) =>
      sc.keywords.some((kw) => c.tenDanhMuc?.toLowerCase().includes(kw)),
    );
    return { ...sc, catId: matched?.id ?? null };
  }),
);

// Click sidebar → lưu cả object (cần keywords cho fallback), xóa chip filter, scroll
const selectSidebarCat = (cat) => {
  activeSidebarCat.value = cat;
  activeCatId.value = cat.catId;
  activeFilter.value = "all";
  advFilter.brands = [];
  advFilter.priceMin = null;
  advFilter.priceMax = null;
  advFilter.category = null;
  const el = document.getElementById("deal-section");
  if (el) el.scrollIntoView({ behavior: "smooth" });
};

// Click chip → lọc theo chip, xóa sidebar filter
const selectChip = (id) => {
  activeFilter.value = id;
  activeCatId.value = null;
  activeSidebarCat.value = null;
};

// Mapping chip id → keywords (bao gồm cả phan_loai_tags slug VÀ text tiếng Việt)
const CHIP_KEYWORDS = {
  gaming: ["gaming"],
  office: ["van_phong", "sinh_vien", "văn phòng", "sinh viên"],
  macbook: ["macbook", "apple"],
  graphics: ["do_hoa", "ky_thuat", "đồ họa", "kỹ thuật"],
};

// Map sanPhamId → số lượng biến thể (để card biết hiển thị "Từ X.XXXđ" hay không)
const variantCountMap = computed(() => {
  const map = new Map();
  products.value.forEach((p) =>
    map.set(p.sanPhamId, (map.get(p.sanPhamId) || 0) + 1),
  );
  return map;
});

// ── Computed: Lọc + deduplicate (1 card/sản phẩm) + sắp xếp ─────────────────
const filteredProducts = computed(() => {
  const query = searchQuery.value.trim().toLowerCase();

  const filtered = products.value.filter((product) => {
    const name = product.tenSanPham?.toLowerCase() || "";
    const brand = product.tenThuongHieu?.toLowerCase() || "";
    const cat = product.tenDanhMuc?.toLowerCase() || "";
    const desc = product.moTa?.toLowerCase() || "";
    const tags = (product.phanLoaiTags || "")
      .split(",")
      .map((t) => t.trim())
      .filter(Boolean);

    const matchesKw = (keywords) =>
      keywords.some(
        (kw) =>
          tags.includes(kw) ||
          cat.includes(kw) ||
          brand.includes(kw) ||
          name.includes(kw),
      );

    if (
      query &&
      !name.includes(query) &&
      !brand.includes(query) &&
      !desc.includes(query)
    )
      return false;

    if (activeSidebarCat.value !== null) {
      if (!matchesKw(activeSidebarCat.value.keywords)) return false;
    } else if (activeFilter.value !== "all") {
      const keywords = CHIP_KEYWORDS[activeFilter.value] || [];
      if (!matchesKw(keywords)) return false;
    }

    if (
      advFilter.brands.length > 0 &&
      !advFilter.brands.includes(product.tenThuongHieu)
    )
      return false;

    const price = Number(product.giaBan) || 0;
    if (advFilter.priceMin !== null && price < advFilter.priceMin) return false;
    if (
      advFilter.priceMax !== null &&
      advFilter.priceMax !== Infinity &&
      price > advFilter.priceMax
    )
      return false;

    if (advFilter.category !== null && product.danhMucId !== advFilter.category)
      return false;

    return true;
  });

  // Deduplicate: 1 card / sanPhamId — lấy biến thể giá thấp nhất làm đại diện
  const deduped = [
    ...filtered
      .reduce((map, p) => {
        const ex = map.get(p.sanPhamId);
        if (!ex || Number(p.giaBan) < Number(ex.giaBan))
          map.set(p.sanPhamId, p);
        return map;
      }, new Map())
      .values(),
  ];

  return deduped.sort((a, b) => {
    if (selectedSort.value === "price-asc")
      return (Number(a.giaBan) || 0) - (Number(b.giaBan) || 0);
    if (selectedSort.value === "price-desc")
      return (Number(b.giaBan) || 0) - (Number(a.giaBan) || 0);
    return 0;
  });
});

// ── Chi tiết sản phẩm ────────────────────────────────────────────────────────
const selectedProduct = ref(null);
// Mở chi tiết sản phẩm — đẩy thêm 1 bước lịch sử trình duyệt riêng cho việc này
// (kèm bienTheId), để nút lùi/tiến quay đúng về sản phẩm đã xem trước đó (hoặc
// đóng hẳn overlay nếu lùi ra khỏi toàn bộ chuỗi sản phẩm đã xem), thay vì nhảy
// xuyên qua các mục lịch sử URL cũ (ví dụ #account) không liên quan.
const openProduct = (p) => {
  selectedProduct.value = p;
  history.pushState({ view: "product", bienTheId: p.bienTheId }, "");
};
const closeProduct = () => {
  selectedProduct.value = null;
};

// Xử lý khi người dùng bấm nút lùi/tiến của trình duyệt (hoặc chuột).
const onPopState = (event) => {
  currentHash.value = window.location.hash;
  const state = event.state;
  if (state?.view === "product") {
    // Đang lùi/tiến giữa các sản phẩm đã xem trong cùng phiên — khôi phục đúng sản phẩm đó
    selectedProduct.value =
      products.value.find((p) => p.bienTheId === state.bienTheId) || null;
  } else {
    // Lùi ra khỏi toàn bộ chuỗi xem sản phẩm — đóng hết overlay như đổi trang bình thường
    selectedProduct.value = null;
    showCart.value = false;
    showCheckout.value = false;
    showLoginModal.value = false;
  }
};

// ── Giỏ hàng ─────────────────────────────────────────────────────────────────

const cart = ref([]); // Mảng sản phẩm trong giỏ
const showCart = ref(false); // Hiển thị/ẩn panel giỏ hàng

// ── Lưu giỏ hàng theo từng tài khoản (localStorage) ──────────────────────────
// Khách vãng lai dùng key riêng "guest"; đăng nhập tài khoản nào thì giỏ hàng
// của tài khoản đó được khôi phục khi đăng nhập lại.
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

// Tự động lưu lại mỗi khi giỏ hàng thay đổi (thêm/xóa/đổi số lượng)
watch(cart, saveCart, { deep: true });

// Tổng số lượng sản phẩm trong giỏ
const cartCount = computed(() =>
  cart.value.reduce((total, item) => total + item.quantity, 0),
);

// Tổng tiền tạm tính
const cartTotal = computed(() =>
  cart.value.reduce(
    (total, item) => total + (item.quantity || 0) * (item.giaBan || 0),
    0,
  ),
);

// Bật/tắt hiển thị giỏ hàng
const toggleCart = () => {
  showCart.value = !showCart.value;
};

// Nhận từ khoá tìm kiếm từ NavBar emit lên
const handleSearch = (q) => {
  searchQuery.value = q;
};

// Xoá 1 sản phẩm khỏi giỏ theo bienTheId
const removeFromCart = (bienTheId) => {
  cart.value = cart.value.filter((item) => item.bienTheId !== bienTheId);
};

// Định dạng tiền tệ VND
const formatPrice = (value) => (value == null ? t("productDetail.contact") : formatPriceRaw(value));

// ── API: Lấy danh sách sản phẩm ──────────────────────────────────────────────
const fetchProducts = async () => {
  loading.value = true;
  error.value = null;
  try {
    products.value = await SanPhamService.getAll();
  } catch (err) {
    error.value = err?.message || "Không thể tải dữ liệu sản phẩm";
  } finally {
    loading.value = false;
  }
};

// Thêm sản phẩm vào giỏ — dùng bienTheId để phân biệt đúng biến thể
const addToCart = (product) => {
  const existing = cart.value.find(
    (item) => item.bienTheId === product.bienTheId,
  );
  if (existing) {
    existing.quantity += 1;
  } else {
    cart.value.push({ ...product, quantity: 1 });
  }
};

// Bấm "Thêm vào giỏ" trên thẻ sản phẩm ở lưới: nếu sản phẩm có nhiều biến thể
// thì mở trang chi tiết để khách chọn cấu hình/màu trước, không thêm nhầm
// biến thể giá thấp nhất đang hiển thị đại diện trên thẻ.
const handleQuickAdd = (product) => {
  if ((variantCountMap.value.get(product.sanPhamId) || 0) > 1) {
    openProduct(product);
  } else {
    addToCart(product);
  }
};

// ── Giỏ hàng: tăng/giảm số lượng ────────────────────────────────────────────
const updateQty = (bienTheId, delta) => {
  const item = cart.value.find((i) => i.bienTheId === bienTheId);
  if (!item) return;
  const newQty = item.quantity + delta;
  if (newQty <= 0)
    cart.value = cart.value.filter((i) => i.bienTheId !== bienTheId);
  else item.quantity = newQty;
};

// ── Checkout (Thanh toán) — toàn bộ logic 2 bước sống trong CheckoutModal.vue ──
const showCheckout = ref(false); // Hiển thị modal thanh toán

// Mở modal thanh toán (bỏ qua nếu giỏ trống)
const openCheckout = () => {
  if (cart.value.length === 0) return;
  showCart.value = false;
  showCheckout.value = true;
};

// CheckoutModal báo đơn đã tạo xong — xóa giỏ hàng (modal tự hiện màn hình thành công)
const handleOrderPlaced = () => {
  cart.value = [];
};

// ── Routing bằng hash URL ─────────────────────────────────────────────────────

// Cập nhật currentHash khi URL fragment thay đổi
function onHashChange() {
  currentHash.value = window.location.hash;
  // Các overlay (chi tiết sản phẩm, giỏ hàng, thanh toán, đăng nhập) chỉ thuộc về
  // trang khách hàng — đóng lại khi chuyển trang (kể cả bấm nút lùi/tiến của trình
  // duyệt) để tránh chồng lên nội dung trang Admin/Tài khoản vừa điều hướng tới.
  selectedProduct.value = null;
  showCart.value = false;
  showCheckout.value = false;
  showLoginModal.value = false;
}

// Quay về trang chủ
function goHome() {
  window.location.hash = "";
}

// Chuyển sang trang admin
function goAdmin() {
  window.location.hash = "#admin";
}

// Chuyển sang trang tài khoản khách hàng
function goAccount() {
  window.location.hash = "#account";
}

// Xử lý sau khi đăng nhập thành công — phân quyền theo role
function onLoginSuccess(user) {
  setSession(user);
  loadCart(); // Khôi phục giỏ hàng đã lưu của tài khoản này (nếu có)
  const staffRoles = ["admin", "nhan_vien", "quan_kho"];
  if (staffRoles.includes(user.role)) {
    window.location.hash = "#admin";
  } else {
    window.location.hash = "";
  }
}

// ── Lifecycle hooks ───────────────────────────────────────────────────────────
onMounted(() => {
  window.addEventListener("hashchange", onHashChange);
  window.addEventListener("popstate", onPopState);
  loadCart(); // Khôi phục giỏ hàng đã lưu (theo tài khoản đang đăng nhập, hoặc khách vãng lai)
  fetchProducts();
  fetchApiCats();
  loadSettings();
});
onBeforeUnmount(() => {
  window.removeEventListener("hashchange", onHashChange); // Dọn dẹp listener
  window.removeEventListener("popstate", onPopState);
});
</script>

<template>
  <!-- Root div — toàn bộ ứng dụng -->
  <div>
    <!-- ══════════════════════════════════════════════════════
        TRANG ADMIN — chỉ hiển thị khi URL có #admin VÀ là admin
    ══════════════════════════════════════════════════════ -->
    <AdminPage v-if="isAdminHash && auth.isAdmin" />

    <!-- Thông báo từ chối quyền truy cập -->
    <section
      v-else-if="isAdminHash && !auth.isAdmin"
      class="d-flex align-items-center justify-content-center"
      style="min-height: 100vh; background: var(--bg-page)"
    >
      <div
        class="text-center d-flex flex-column align-items-center gap-3"
        style="color: var(--text-primary)"
      >
        <div style="font-size: 3rem">🔒</div>
        <h2 class="fw-black mb-0" style="font-size: 1.5rem">
          {{ t("adminAccess.title") }}
        </h2>
        <p class="mb-0" style="color: var(--text-secondary)">
          {{ t("adminAccess.desc") }}
        </p>
        <!-- Nút quay về trang chủ -->
        <button
          class="btn btn-warning fw-bold rounded-pill px-4 py-2"
          @click="goHome"
        >
          {{ t("common.goHome") }}
        </button>
      </div>
    </section>

    <!-- ══════════════════════════════════════════════════════
        TRANG TÀI KHOẢN KHÁCH HÀNG — chỉ hiển thị khi URL có #account VÀ đã đăng nhập khách hàng
    ══════════════════════════════════════════════════════ -->
    <AccountPage
      v-else-if="isAccountHash && auth.user && !auth.isAdmin"
      @go-home="goHome"
      @add-to-cart="addToCart"
    />

    <!-- Chưa đăng nhập (hoặc là tài khoản staff) mà vào #account -->
    <section
      v-else-if="isAccountHash"
      class="d-flex align-items-center justify-content-center"
      style="min-height: 100vh; background: var(--bg-page)"
    >
      <div
        class="text-center d-flex flex-column align-items-center gap-3"
        style="color: var(--text-primary)"
      >
        <div style="font-size: 3rem">🔒</div>
        <h2 class="fw-black mb-0" style="font-size: 1.5rem">
          {{ t("account.needLoginTitle") }}
        </h2>
        <p class="mb-0" style="color: var(--text-secondary)">
          {{ t("account.needLoginDesc") }}
        </p>
        <button
          class="btn btn-warning fw-bold rounded-pill px-4 py-2"
          @click="goHome"
        >
          {{ t("common.goHome") }}
        </button>
      </div>
    </section>

    <!-- ══════════════════════════════════════════════════════
        TRANG KHÁCH HÀNG — hiển thị khi không có #admin
    ══════════════════════════════════════════════════════ -->
    <div
      v-else
      style="
        min-height: 100vh;
        background: var(--bg-page);
        color: var(--text-primary);
        font-family: &quot;Nunito Sans&quot;, &quot;Segoe UI&quot;, sans-serif;
      "
    >
      <!-- Header / NavBar — nhận cartCount và xử lý các sự kiện -->
      <NavBar
        :cart-count="cartCount"
        :user="auth.isAdmin ? null : auth.user"
        @toggle-cart="toggleCart"
        @search="handleSearch"
        @open-admin="goAdmin"
        @open-account="goAccount"
        @open-login="openLogin"
        @logout="onLogout"
      />

      <!-- Dải ticker chạy ngang (thông báo khuyến mãi) -->
      <div
        class="overflow-x-auto"
        style="
          background: var(--bg-page-alt);
          border-bottom: 1px solid var(--border-color-soft);
          padding: 8px 16px;
        "
      >
        <div
          class="d-flex gap-4 small fw-bold"
          style="
            white-space: nowrap;
            width: max-content;
            color: var(--text-secondary);
          "
        >
          <span
            class="text-warning text-uppercase"
            style="letter-spacing: 0.05em"
          >
            🔥 {{ t("home.tickerBadge") }}
          </span>
          <span>{{ t("home.ticker1") }}</span>
          <span>{{ t("home.ticker2") }}</span>
          <span>{{ t("home.ticker3") }}</span>
        </div>
      </div>

      <!-- ── Nội dung trang chính ── -->
      <div class="container-xl py-3">
        <!-- ── Hero Grid: Sidebar | Banner | Info Panel ── -->
        <div class="row g-3 mb-3">
          <!-- Sidebar danh mục (chỉ hiện trên màn lớn) -->
          <div class="col-xl-2 d-none d-xl-block">
            <div
              class="rounded-3 p-1 d-flex flex-column gap-1 h-100"
              style="
                background: var(--bg-card);
                border: 1px solid var(--border-color);
              "
            >
              <a
                v-for="cat in sidebarCats"
                :key="cat.id"
                href="#"
                class="d-flex align-items-center justify-content-between px-3 py-2 rounded-2 text-decoration-none small fw-bold"
                style="font-size: 12px; transition: all 0.15s"
                :style="
                  activeSidebarCat && activeSidebarCat.id === cat.id
                    ? 'background:var(--bg-hover); color:var(--accent-fg);'
                    : 'color:var(--text-muted);'
                "
                @mouseenter="
                  (e) => {
                    e.currentTarget.style.background = 'var(--bg-hover)';
                    e.currentTarget.style.color = 'var(--accent)';
                  }
                "
                @mouseleave="
                  (e) => {
                    const isActive =
                      activeSidebarCat && activeSidebarCat.id === cat.id;
                    e.currentTarget.style.background = isActive
                      ? 'var(--bg-hover)'
                      : '';
                    e.currentTarget.style.color = isActive
                      ? 'var(--accent)'
                      : '';
                  }
                "
                @click.prevent="selectSidebarCat(cat)"
              >
                <span class="d-flex align-items-center gap-2">
                  <span style="font-size: 13px">{{ cat.icon }}</span>
                  {{ cat.name }}
                </span>
                <span style="color: var(--text-muted)">›</span>
              </a>
            </div>
          </div>

          <!-- Banner trung tâm + 3 sản phẩm nổi bật -->
          <div class="col-12 col-xl-7">
            <!-- Banner ảnh chính -->
            <div
              class="position-relative rounded-3 overflow-hidden mb-2"
              style="height: 280px"
            >
              <img
                src="https://images.unsplash.com/photo-1603302576837-37561b2e2302?q=80&w=800"
                alt="Laptop Premium Promotion"
                class="w-100 h-100"
                style="object-fit: cover"
              />
              <!-- Overlay chữ nổi trên banner -->
              <div
                class="position-absolute bottom-0 start-0 p-4 w-100"
                style="
                  background: linear-gradient(
                    to top,
                    rgba(0, 0, 0, 0.85) 0%,
                    transparent 100%
                  );
                "
              >
                <h2
                  class="text-white fw-black mb-1"
                  style="
                    font-size: 1.3rem;
                    text-shadow: 0 2px 8px rgba(0, 0, 0, 0.8);
                  "
                >
                  {{ t("home.heroTitle") }}
                </h2>
                <p class="text-warning small fw-bold mb-0">
                  {{ t("home.heroSubtitle") }}
                </p>
              </div>
            </div>

            <!-- 3 thẻ sản phẩm nổi bật nhỏ bên dưới banner -->
            <div class="row g-2">
              <template v-if="loading">
                <div class="col-4">
                  <div
                    class="p-3 rounded-2 small text-center"
                    style="
                      background: var(--bg-card);
                      border: 1px solid var(--border-color);
                      color: var(--text-secondary);
                    "
                  >
                    {{ t("home.loadingShort") }}
                  </div>
                </div>
              </template>
              <template v-else-if="products.length">
                <div
                  v-for="p in products.slice(0, 3)"
                  :key="p.sanPhamId"
                  class="col-4"
                >
                  <div
                    class="p-2 rounded-2 small"
                    style="
                      background: var(--bg-card);
                      border: 1px solid var(--border-color);
                    "
                  >
                    <p
                      class="fw-bold mb-1"
                      style="
                        font-size: 11px;
                        overflow: hidden;
                        display: -webkit-box;
                        -webkit-line-clamp: 2;
                        line-clamp: 2;
                        -webkit-box-orient: vertical;
                        color: var(--text-primary);
                      "
                    >
                      {{ p.tenSanPham }}
                    </p>
                    <p
                      class="text-warning fw-black mb-0"
                      style="font-size: 12px"
                    >
                      {{ formatPrice(p.giaBan) }}
                    </p>
                  </div>
                </div>
              </template>
              <template v-else>
                <div class="col-12">
                  <div
                    class="p-2 rounded-2 small text-center"
                    style="
                      background: var(--bg-card);
                      border: 1px solid var(--border-color);
                      color: var(--text-secondary);
                    "
                  >
                    {{ t("home.noProducts") }}
                  </div>
                </div>
              </template>
            </div>
          </div>

          <!-- Info panel bên phải -->
          <div class="col-12 col-xl-3">
            <div
              class="rounded-3 p-3 h-100 d-flex flex-column gap-3"
              style="
                background: var(--bg-card);
                border: 1px solid var(--border-color);
              "
            >
              <!-- Logo thương hiệu -->
              <div class="d-flex align-items-center gap-2">
                <div
                  class="rounded-circle d-flex align-items-center justify-content-center fw-black flex-shrink-0"
                  style="
                    width: 44px;
                    height: 44px;
                    background: var(--accent);
                    color: var(--accent-text);
                    font-size: 0.8rem;
                  "
                >
                  SAO
                </div>
                <div>
                  <h4
                    class="fw-black mb-0"
                    style="
                      font-size: 0.82rem;
                      line-height: 1.3;
                      color: var(--text-heading);
                    "
                  >
                    {{ t("home.brandName") }}
                  </h4>
                  <p
                    class="mb-0"
                    style="font-size: 10px; color: var(--text-secondary)"
                  >
                    {{ t("home.brandTagline") }}
                  </p>
                </div>
              </div>
              <!-- Các liên kết ưu đãi -->
              <div class="d-flex flex-column gap-1">
                <a
                  v-for="link in [
                    t('home.promoLink1'),
                    t('home.promoLink2'),
                    t('home.promoLink3'),
                    t('home.promoLink4'),
                  ]"
                  :key="link"
                  href="#"
                  class="d-block text-decoration-none fw-semibold p-2 rounded-2 small"
                  style="
                    font-size: 11px;
                    transition: background 0.15s;
                    color: var(--text-secondary);
                  "
                  @mouseenter="
                    (e) => {
                      e.target.style.background = 'var(--bg-hover)';
                      e.target.style.color = 'var(--accent)';
                    }
                  "
                  @mouseleave="
                    (e) => {
                      e.target.style.background = '';
                      e.target.style.color = '';
                    }
                  "
                >
                  {{ link }}
                </a>
              </div>
              <!-- Banner CTA -->
              <div
                class="mt-auto text-center fw-black py-2 rounded-2 small"
                style="
                  background: linear-gradient(
                    135deg,
                    var(--accent),
                    var(--accent-2)
                  );
                  color: var(--accent-text);
                  font-size: 11px;
                  letter-spacing: 0.06em;
                "
              >
                {{ t("home.examBanner") }}
              </div>
            </div>
          </div>
        </div>
        <!-- /hero-grid row -->

        <!-- ── Deal Section: tabs + filter + danh sách sản phẩm ── -->
        <section id="deal-section" class="mt-3">
          <!-- Tabs: DEAL SỐC | HOT TREND | MÁY MỚI -->
          <div
            class="d-flex gap-2 mb-3 border-bottom pb-0"
            style="border-color: var(--border-color) !important"
          >
            <button
              v-for="tab in dealTabs"
              :key="tab.id"
              class="btn btn-sm fw-black px-3 pb-2 rounded-0 border-0"
              style="
                font-size: 12px;
                letter-spacing: 0.04em;
                border-bottom: 3px solid transparent !important;
              "
              :style="
                activeTab === tab.id
                  ? 'border-bottom:3px solid var(--accent)!important; color:var(--accent-fg);'
                  : 'color:var(--text-secondary);'
              "
              @click="activeTab = tab.id"
            >
              {{ tab.label }}
            </button>
          </div>

          <!-- Thanh filter + sắp xếp -->
          <div
            class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-2"
          >
            <!-- Chip filter theo loại laptop -->
            <div class="d-flex flex-wrap gap-2">
              <button
                v-for="f in dealFilters"
                :key="f.id"
                class="btn btn-sm fw-bold"
                style="font-size: 11px; border-radius: 999px"
                :class="
                  activeFilter === f.id && activeCatId === null
                    ? 'btn-warning text-dark'
                    : 'btn-outline-secondary'
                "
                :style="
                  !(activeFilter === f.id && activeCatId === null)
                    ? 'color:var(--text-secondary);'
                    : ''
                "
                @click="selectChip(f.id)"
              >
                {{ f.label }}
              </button>
            </div>
            <!-- Nút lọc nâng cao + select sắp xếp -->
            <div class="d-flex align-items-center gap-2">
              <button
                class="btn btn-sm fw-bold"
                style="font-size: 11px; border-radius: 999px"
                :class="
                  showAdvFilter
                    ? 'btn-warning text-dark'
                    : 'btn-outline-secondary'
                "
                :style="!showAdvFilter ? 'color:var(--text-secondary);' : ''"
                @click="showAdvFilter = !showAdvFilter"
              >
                {{ t("home.advFilter") }}
                <span
                  v-if="
                    advFilter.brands.length ||
                    advFilter.priceMin ||
                    advFilter.category
                  "
                  class="badge bg-danger ms-1"
                  style="font-size: 9px"
                >
                  {{
                    advFilter.brands.length +
                    (advFilter.priceMin ? 1 : 0) +
                    (advFilter.category ? 1 : 0)
                  }}
                </span>
              </button>
              <select
                v-model="selectedSort"
                class="form-select form-select-sm"
                style="
                  width: auto;
                  background: var(--bg-input);
                  border-color: var(--border-color-strong);
                  color: var(--text-primary);
                  font-size: 12px;
                "
                :aria-label="t('home.sortLabel')"
              >
                <option value="default">{{ t("home.sortDefault") }}</option>
                <option value="price-asc">{{ t("home.sortPriceAsc") }}</option>
                <option value="price-desc">
                  {{ t("home.sortPriceDesc") }}
                </option>
              </select>
            </div>
          </div>

          <!-- Panel lọc nâng cao (thương hiệu, khoảng giá, danh mục) -->
          <div
            v-show="showAdvFilter"
            class="p-3 mb-3 rounded-3"
            style="
              background: var(--bg-card);
              border: 1px solid var(--border-color);
            "
          >
            <ProductFilter
              :brands="allBrands"
              :categories="allCategories"
              @change="onAdvFilterChange"
            />
          </div>

          <!-- Trạng thái loading / lỗi / trống -->
          <div
            v-if="loading"
            class="text-center py-4 small"
            style="color: var(--text-secondary)"
          >
            {{ t("home.loadingProducts") }}
          </div>
          <div v-else-if="error" class="alert alert-danger small py-2">
            {{ error }}
          </div>
          <div
            v-else-if="filteredProducts.length === 0"
            class="text-center py-4 small"
            style="color: var(--text-secondary)"
          >
            {{ t("home.noMatch") }}
          </div>

          <!-- Lưới sản phẩm -->
          <div v-else class="row g-3">
            <div
              v-for="product in filteredProducts"
              :key="product.sanPhamId"
              class="col-6 col-md-4 col-lg-3 col-xl-2"
            >
              <ProductCard
                :product="product"
                :variant-count="variantCountMap.get(product.sanPhamId) || 0"
                @click="openProduct(product)"
                @add-to-cart="handleQuickAdd(product)"
              />
            </div>
          </div>
          <!-- /product grid -->
        </section>
        <!-- /deal section -->

        <!-- ── Panel giỏ hàng (slide-in từ phải) ── -->
        <Transition name="cart-slide">
          <div
            v-if="showCart"
            class="position-fixed top-0 end-0 h-100 d-flex flex-column"
            style="
              width: 390px;
              background: var(--bg-page-alt);
              border-left: 1px solid var(--border-color-soft);
              z-index: 500;
              box-shadow: -12px 0 48px rgba(0, 0, 0, 0.4);
            "
          >
            <!-- Header giỏ hàng -->
            <div
              class="d-flex justify-content-between align-items-center px-4 py-3"
              style="border-bottom: 1px solid var(--border-color-soft)"
            >
              <div class="d-flex align-items-center gap-2">
                <span style="font-size: 1.1rem">🛒</span>
                <span
                  class="fw-bold"
                  style="font-size: 0.95rem; color: var(--text-heading)"
                  >{{ t("cart.title") }}</span
                >
                <span
                  v-if="cartCount > 0"
                  class="badge bg-warning text-dark fw-bold rounded-pill"
                  style="font-size: 10px"
                  >{{ cartCount }}</span
                >
              </div>
              <button
                class="btn btn-sm d-flex align-items-center justify-content-center rounded-circle"
                style="
                  width: 30px;
                  height: 30px;
                  padding: 0;
                  background: var(--bg-input);
                  color: var(--text-secondary);
                  border: none;
                  font-size: 14px;
                "
                :aria-label="t('common.close')"
                @click="toggleCart"
              >
                ✕
              </button>
            </div>

            <!-- Empty state -->
            <div
              v-if="cartCount === 0"
              class="flex-grow-1 d-flex flex-column align-items-center justify-content-center gap-3 text-center px-4"
            >
              <div style="font-size: 3rem; opacity: 0.2">🛍️</div>
              <p class="small mb-0" style="color: var(--text-secondary)">
                {{ t("cart.empty") }}
              </p>
              <button
                class="btn btn-sm btn-outline-warning rounded-pill px-4"
                @click="toggleCart"
              >
                {{ t("cart.continueShopping") }}
              </button>
            </div>

            <!-- Danh sách sản phẩm -->
            <div
              v-else
              class="flex-grow-1 overflow-y-auto px-3 py-2 d-flex flex-column gap-2"
            >
              <CartItem
                v-for="item in cart"
                :key="item.bienTheId"
                :item="item"
                @decrease="updateQty(item.bienTheId, -1)"
                @increase="updateQty(item.bienTheId, 1)"
              />
            </div>

            <!-- Footer: tổng + checkout -->
            <CartSummary
              v-if="cartCount > 0"
              :cart-count="cartCount"
              :cart-total="cartTotal"
              @checkout="openCheckout"
            />
          </div> </Transition
        ><!-- /cart panel -->
      </div>
      <!-- /container-xl -->

      <!-- Footer -->
      <AppFooter />
    </div>
    <!-- /trang khách hàng -->
  </div>
  <!-- /root -->

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
      <span style="font-size: 1.1rem; flex-shrink: 0">
        {{
          toast.type === "success" ? "✓" : toast.type === "error" ? "✕" : "ℹ"
        }}
      </span>
      {{ toast.msg }}
    </div>
  </Transition>

  <!-- ══════════════════════════════════════════════════════
      LOGIN MODAL — overlay trên trang khách hàng
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
      CHECKOUT MODAL — 2 bước: Thông tin → Thanh toán
  ══════════════════════════════════════════════════════ -->
  <CheckoutModal
    v-model="showCheckout"
    :cart="cart"
    :cart-total="cartTotal"
    @order-placed="handleOrderPlaced"
  />

  <!-- ── Trang chi tiết sản phẩm (full-screen overlay) ── -->
  <Transition name="slide-up">
    <ProductDetail
      v-if="selectedProduct"
      :key="selectedProduct.bienTheId"
      :product="selectedProduct"
      :products="products"
      @close="closeProduct"
      @add-to-cart="
        (p) => {
          addToCart(p);
          closeProduct();
        }
      "
      @open-product="openProduct"
    />
  </Transition>
</template>

<style>
/* ProductDetail overlay */
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

/* Cart panel slide-in từ phải */
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

/* QR / bank info fade */
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

/* Toast slide-in từ phải */
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

<!-- Không còn CSS scoped — toàn bộ dùng Bootstrap utility classes + inline style tối thiểu -->
