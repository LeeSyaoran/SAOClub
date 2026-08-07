<script setup>
defineEmits(['addToCart', 'buyAgainUnavailable', 'goHome']);
import {
  ref,
  computed,
  reactive,
  onMounted,
  inject,
} from "vue";
import { useRouter } from "vue-router";
import { Laptop, Gamepad2, Zap, Apple, Star, Wrench, Flame, ShoppingCart, X, ShoppingBag, GraduationCap, RefreshCw, SlidersHorizontal } from '@lucide/vue';
import * as SanPhamService from "../services/SanPhamService.js";
import * as DanhMucService from "../services/DanhMucService.js";
import { t } from "../i18n/index.js";
import { formatPrice as formatPriceRaw } from "../utils/formatPrice.js";
import { groupBySanPham, variantCountBySanPham } from "../utils/productGrouping.js";
import NavBar from "../components/layout/NavBar.vue";
import AppFooter from "../components/layout/Footer.vue";
import ProductFilter from "../components/product/ProductFilter.vue";
import ProductCard from "../components/product/ProductCard.vue";
import ProductCompareBar from "../components/product/ProductCompareBar.vue";
import ProductCompareModal from "../components/product/ProductCompareModal.vue";
import CartItem from "../components/cart/CartItem.vue";
import CartSummary from "../components/cart/CartSummary.vue";

const router = useRouter();

const {
  products, cart, showCart, cartCount, cartTotal, auth, ratingSummaries,
} = inject("appState");
const {
  addToCart, removeFromCart, updateQty, toggleCart,
  openCheckout, openProduct, showToast, openLogin, onLogout,
  fetchProducts, formatPrice, isWishlisted, toggleWishlist,
} = inject("appActions");

const searchQuery = ref("");
const selectedSort = ref("default");
const showAdvFilter = ref(false);
const advFilter = reactive({
  brands: [],
  priceMin: null,
  priceMax: null,
  category: null,
  cpu: [],
  ram: [],
  gpu: [],
  storage: [],
});

const apiCats = ref([]);
const fetchApiCats = async () => {
  apiCats.value = await DanhMucService.getAll().catch(() => []);
};

const allBrands = computed(() => [
  ...new Set(products.value.map((p) => p.tenThuongHieu).filter(Boolean)),
]);

// Chỉ liệt kê giá trị cấu hình THỰC SỰ có trong tập sản phẩm hiện tại — tránh chip lọc ra
// danh sách rỗng. sort() cho RAM/Ổ cứng chỉ là sắp chữ (không phải theo dung lượng tăng dần)
// nhưng với số lượng giá trị nhỏ (xem ProductFilter.vue) không đáng để viết parser riêng.
const allCpus = computed(() => [...new Set(products.value.map((p) => p.cpu).filter(Boolean))].sort());
const allRams = computed(() => [...new Set(products.value.map((p) => p.ram).filter(Boolean))].sort());
const allGpus = computed(() => [...new Set(products.value.map((p) => p.gpu).filter(Boolean))].sort());
const allStorages = computed(() => [...new Set(products.value.map((p) => p.oCung).filter(Boolean))].sort());

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
  advFilter.cpu = f.cpu;
  advFilter.ram = f.ram;
  advFilter.gpu = f.gpu;
  advFilter.storage = f.storage;
};

const activeTab = ref("deal");
const activeFilter = ref("all");
const activeCatId = ref(null);
const activeSidebarCat = ref(null);

const dealTabs = computed(() => [
  { id: "deal", label: t("home.dealTabDeal"), icon: Flame },
  { id: "hot", label: t("home.dealTabHot") },
  { id: "new", label: t("home.dealTabNew") },
]);

const dealFilters = computed(() => [
  { id: "all", label: t("home.chipAll") },
  { id: "gaming", label: t("home.chipGaming") },
  { id: "office", label: t("home.chipOffice") },
  { id: "macbook", label: t("home.chipMacbook") },
  { id: "graphics", label: t("home.chipGraphics") },
]);

const sidebarCatsBase = computed(() => [
  {
    id: "office",
    icon: Laptop,
    name: t("home.sidebar.office"),
    keywords: ["van_phong", "sinh_vien", "văn phòng", "sinh viên"],
  },
  {
    id: "gaming",
    icon: Gamepad2,
    name: t("home.sidebar.gaming"),
    keywords: ["gaming"],
  },
  {
    id: "graphics",
    icon: Zap,
    name: t("home.sidebar.graphics"),
    keywords: ["do_hoa", "ky_thuat", "đồ họa", "kỹ thuật"],
  },
  {
    id: "macbook",
    icon: Apple,
    name: t("home.sidebar.macbook"),
    keywords: ["macbook", "apple"],
  },
  {
    id: "used",
    icon: Star,
    name: t("home.sidebar.used"),
    keywords: ["cu", "gia_re", "cũ", "rẻ"],
  },
  {
    id: "parts",
    icon: Wrench,
    name: t("home.sidebar.parts"),
    keywords: ["linh_kien", "ram", "ssd", "linh kiện"],
  },
]);

const sidebarCats = computed(() =>
  sidebarCatsBase.value.map((sc) => {
    const matched = apiCats.value.find((c) =>
      sc.keywords.some((kw) => c.tenDanhMuc?.toLowerCase().includes(kw)),
    );
    return { ...sc, catId: matched?.id ?? null };
  }),
);

const selectSidebarCat = (cat) => {
  activeSidebarCat.value = cat;
  activeCatId.value = cat.catId;
  activeFilter.value = "all";
  advFilter.brands = [];
  advFilter.priceMin = null;
  advFilter.priceMax = null;
  advFilter.category = null;
  advFilter.cpu = [];
  advFilter.ram = [];
  advFilter.gpu = [];
  advFilter.storage = [];
  const el = document.getElementById("deal-section");
  if (el) el.scrollIntoView({ behavior: "smooth" });
};

const selectChip = (id) => {
  activeFilter.value = id;
  activeCatId.value = null;
  activeSidebarCat.value = null;
};

const CHIP_KEYWORDS = {
  gaming: ["gaming"],
  office: ["van_phong", "sinh_vien", "văn phòng", "sinh viên"],
  macbook: ["macbook", "apple"],
  graphics: ["do_hoa", "ky_thuat", "đồ họa", "kỹ thuật"],
};

const variantCountMap = computed(() => variantCountBySanPham(products.value));

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

    if (advFilter.cpu.length > 0 && !advFilter.cpu.includes(product.cpu))
      return false;
    if (advFilter.ram.length > 0 && !advFilter.ram.includes(product.ram))
      return false;
    if (advFilter.gpu.length > 0 && !advFilter.gpu.includes(product.gpu))
      return false;
    if (advFilter.storage.length > 0 && !advFilter.storage.includes(product.oCung))
      return false;

    return true;
  });

  const deduped = groupBySanPham(filtered);

  return deduped.sort((a, b) => {
    if (selectedSort.value === "price-asc")
      return (Number(a.giaBan) || 0) - (Number(b.giaBan) || 0);
    if (selectedSort.value === "price-desc")
      return (Number(b.giaBan) || 0) - (Number(a.giaBan) || 0);
    return 0;
  });
});

const handleSearch = (q) => {
  searchQuery.value = q;
};

const goAdmin = () => router.push("/admin");
const goAccount = () => { router.push("/account"); };

const handleQuickAdd = (product) => {
  if ((variantCountMap.value.get(product.sanPhamId) || 0) > 1) {
    openProduct(product);
  } else {
    addToCart(product);
  }
};

// ── So sánh sản phẩm — thuần frontend, mọi thông số cần đều đã có sẵn trong products.value
// (SanPhamResponse.java), không cần gọi thêm API nào. ─────────────────────────────────────
const MAX_COMPARE = 4;
const compareList = ref([]);
const showCompareModal = ref(false);

const isComparing = (product) => compareList.value.some((p) => p.bienTheId === product.bienTheId);

const toggleCompare = (product) => {
  const idx = compareList.value.findIndex((p) => p.bienTheId === product.bienTheId);
  if (idx !== -1) {
    compareList.value.splice(idx, 1);
    return;
  }
  if (compareList.value.length >= MAX_COMPARE) {
    showToast(t("productCompare.maxReached", { max: MAX_COMPARE }));
    return;
  }
  compareList.value.push(product);
};

const removeFromCompare = (product) => {
  compareList.value = compareList.value.filter((p) => p.bienTheId !== product.bienTheId);
};

const clearCompare = () => { compareList.value = []; };

const addToCartFromCompare = (product) => {
  addToCart(product);
  showCompareModal.value = false;
};

onMounted(() => {
  fetchProducts();
  fetchApiCats();
});
</script>

<template>
  <div
    style="
      min-height: 100vh;
      background: var(--bg-page);
      color: var(--text-primary);
      font-family: &quot;Nunito Sans&quot;, &quot;Segoe UI&quot;, sans-serif;
    "
  >
    <NavBar
      :cart-count="cartCount"
      :user="auth.isAdmin ? null : auth.user"
      @toggle-cart="toggleCart"
      @search="handleSearch"
      @open-admin="goAdmin"
      @open-account="goAccount"
      @open-login="openLogin"
      @logout="onLogout"
      @select-category="selectSidebarCat"
    />

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
          <Flame :size="13" style="vertical-align:-2px;" /> {{ t("home.tickerBadge") }}
        </span>
        <span>{{ t("home.ticker1") }}</span>
        <span>{{ t("home.ticker2") }}</span>
        <span>{{ t("home.ticker3") }}</span>
      </div>
    </div>

    <div class="container-xl py-3">
      <div class="row g-3 mb-3">
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
                <component :is="cat.icon" :size="16" />
                {{ cat.name }}
              </span>
              <span style="color: var(--text-muted)">›</span>
            </a>
          </div>
        </div>

        <div class="col-12 col-xl-7">
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

          <div class="row g-2">
            <template v-if="false">
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

        <div class="col-12 col-xl-3">
          <div
            class="rounded-3 p-3 h-100 d-flex flex-column gap-3"
            style="
              background: var(--bg-card);
              border: 1px solid var(--border-color);
            "
          >
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
            <div class="d-flex flex-column gap-1">
              <a
                v-for="link in [
                  { icon: GraduationCap, text: t('home.promoLink1') },
                  { icon: Flame, text: t('home.promoLink2') },
                  { icon: Laptop, text: t('home.promoLink3') },
                  { icon: RefreshCw, text: t('home.promoLink4') },
                ]"
                :key="link.text"
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
                <component :is="link.icon" :size="13" style="vertical-align:-2px;" /> {{ link.text }}
              </a>
            </div>
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

      <section id="deal-section" class="mt-3">
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
            <component v-if="tab.icon" :is="tab.icon" :size="12" style="vertical-align:-2px;" /> {{ tab.label }}
          </button>
        </div>

        <div
          class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-2"
        >
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
              <SlidersHorizontal :size="12" style="vertical-align:-2px;" /> {{ t("home.advFilter") }}
              <span
                v-if="
                  advFilter.brands.length ||
                  advFilter.priceMin ||
                  advFilter.category ||
                  advFilter.cpu.length ||
                  advFilter.ram.length ||
                  advFilter.gpu.length ||
                  advFilter.storage.length
                "
                class="badge bg-danger ms-1"
                style="font-size: 9px"
              >
                {{
                  advFilter.brands.length +
                  (advFilter.priceMin ? 1 : 0) +
                  (advFilter.category ? 1 : 0) +
                  advFilter.cpu.length +
                  advFilter.ram.length +
                  advFilter.gpu.length +
                  advFilter.storage.length
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
            :cpus="allCpus"
            :rams="allRams"
            :gpus="allGpus"
            :storages="allStorages"
            @change="onAdvFilterChange"
          />
        </div>

        <div
          v-if="false"
          class="text-center py-4 small"
          style="color: var(--text-secondary)"
        >
          {{ t("home.loadingProducts") }}
        </div>
        <div v-else-if="false" class="alert alert-danger small py-2">
          {{ "error" }}
        </div>
        <div
          v-else-if="filteredProducts.length === 0"
          class="text-center py-4 small"
          style="color: var(--text-secondary)"
        >
          {{ t("home.noMatch") }}
        </div>

        <div v-else class="row g-3">
          <div
            v-for="product in filteredProducts"
            :key="product.sanPhamId"
            class="col-6 col-md-4 col-lg-3 col-xl-2"
          >
            <ProductCard
              :product="product"
              :variant-count="variantCountMap.get(product.sanPhamId) || 0"
              :is-comparing="isComparing(product)"
              :compare-disabled="!isComparing(product) && compareList.length >= MAX_COMPARE"
              :is-wishlisted="isWishlisted(product.bienTheId)"
              :rating="ratingSummaries.get(product.sanPhamId)"
              @click="openProduct(product)"
              @add-to-cart="handleQuickAdd(product)"
              @toggle-compare="toggleCompare(product)"
              @toggle-wishlist="toggleWishlist(product)"
            />
          </div>
        </div>
      </section>

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
          <div
            class="d-flex justify-content-between align-items-center px-4 py-3"
            style="border-bottom: 1px solid var(--border-color-soft)"
          >
            <div class="d-flex align-items-center gap-2">
              <span><ShoppingCart :size="18" /></span>
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
              <X :size="16" />
            </button>
          </div>

          <div
            v-if="cartCount === 0"
            class="flex-grow-1 d-flex flex-column align-items-center justify-content-center gap-3 text-center px-4"
          >
            <div style="opacity: 0.2"><ShoppingBag :size="48" /></div>
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

          <CartSummary
            v-if="cartCount > 0"
            :cart-count="cartCount"
            :cart-total="cartTotal"
            @checkout="openCheckout"
          />
        </div>
      </Transition>
    </div>

    <AppFooter />
  </div>

  <ProductCompareBar
    :items="compareList"
    :max="MAX_COMPARE"
    @open="showCompareModal = true"
    @clear="clearCompare"
    @remove="removeFromCompare"
  />
  <ProductCompareModal
    v-model="showCompareModal"
    :items="compareList"
    @remove="removeFromCompare"
    @add-to-cart="addToCartFromCompare"
  />
</template>
