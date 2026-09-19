<script setup>
defineEmits(['addToCart', 'buyAgainUnavailable', 'goHome', 'toast']);
import {
  ref,
  computed,
  reactive,
  onMounted,
  onBeforeUnmount,
  inject,
} from "vue";
import { useRouter } from "vue-router";
import { Laptop, Gamepad2, Zap, Apple, Star, Wrench, Flame, ShoppingCart, X, ShoppingBag, GraduationCap, RefreshCw, SlidersHorizontal, ChevronLeft, ChevronRight, Sparkles, Cpu, Headphones, ShieldCheck, Tag, Heart, Truck, HardDrive } from '@lucide/vue';
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
  products, productsLoading, cart, showCart, cartCount, cartTotal,
  cartSelected, cartSelectedTotal, cartSelectedCount,
  auth, ratingSummaries,
} = inject("appState");
const {
  addToCart, removeFromCart, updateQty, toggleCart,
  toggleCartItem, selectAllCartItems, deselectAllCartItems,
  openCheckout, openProduct, showToast, openLogin, openRegister, onLogout,
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

// Bảng ảnh fallback dựa theo tên thương hiệu/dòng máy — đảm bảo không bao giờ hiển thị placeholder
const FALLBACK_IMAGES = {
  apple:   'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800&q=80',
  macbook: 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800&q=80',
  lenovo:  'https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=800&q=80',
  thinkpad:'https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=800&q=80',
  asus:    'https://images.unsplash.com/photo-1593642702821-c8da6771f0c6?w=800&q=80',
  zenbook: 'https://images.unsplash.com/photo-1593642702821-c8da6771f0c6?w=800&q=80',
  rog:     'https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=800&q=80',
  tuf:     'https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=800&q=80',
  hp:      'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=800&q=80',
  dell:    'https://images.unsplash.com/photo-1525547719571-a2d4ac8945e2?w=800&q=80',
  vostro:  'https://images.unsplash.com/photo-1525547719571-a2d4ac8945e2?w=800&q=80',
  xps:     'https://images.unsplash.com/photo-1525547719571-a2d4ac8945e2?w=800&q=80',
  inspiron:'https://images.unsplash.com/photo-1593642632559-0c6d3fc62b89?w=800&q=80',
  msi:     'https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=800&q=80',
  acer:    'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800&q=80',
  lg:      'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=800&q=80',
  gram:    'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=800&q=80',
  default: 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=800&q=80',
};

// Tìm ảnh fallback dựa trên tên sản phẩm
const fallbackImage = (product) => {
  const name = (product?.tenSanPham || '').toLowerCase();
  for (const key of Object.keys(FALLBACK_IMAGES)) {
    if (key !== 'default' && name.includes(key)) return FALLBACK_IMAGES[key];
  }
  return FALLBACK_IMAGES.default;
};

// Ảnh an toàn — ưu tiên ảnh thật từ DB, nếu rỗng thì dùng fallback theo tên
const safeImage = (product) => {
  return product?.hinhAnhChinh || product?.hinhAnh || fallbackImage(product);
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
  { id: "deal", label: "DEAL SỐC MỚI NGAY", icon: Flame, filter: "all", catId: null },
  { id: "hot", label: "SẢN PHẨM HOT TREND", icon: null, filter: "gaming", catId: null },
  { id: "new", label: "HÀNG MỚI VỀ", icon: null, filter: "macbook", catId: null },
]);

// Giới hạn chỉ hiện 5 sản phẩm trong phần deal sốc (1 dòng)
const dealProducts = computed(() => filteredProducts.value.slice(0, 5));

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
  // Luôn thêm trực tiếp vào giỏ hàng, không kiểm tra số biến thể
  addToCart(product);
};

// ── So sánh sản phẩm — thuần frontend, mọi thông số cần đều đã có sẵn trong products
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
  startSlider();
  startCountdown();
});

onBeforeUnmount(() => {
  stopSlider();
  if (countdownTimer) clearInterval(countdownTimer);
});

// ─── Hero slider: 5 slides chủ đề laptop/ưu đãi, tự chuyển mỗi 5s ────────────────────
const heroSlides = [
  {
    image: "https://images.unsplash.com/photo-1603302576837-37561b2e2302?q=80&w=1200",
    overlay: "linear-gradient(45deg, rgba(236,72,153,0.35) 0%, transparent 50%)",
    badge: "AI Next-Gen 2026",
    badgeBg: "#fde047",
    badgeFg: "#831843",
    icon: Sparkles,
    title: "LAPTOP AI NEXT-GEN 2026",
    subtitle: "Ưu đãi mùa tựu trường | Trả góp 0% + Tặng Balo Gaming cao cấp",
    subColor: "#fde047",
  },
  {
    image: "https://images.unsplash.com/photo-1593642632559-0c6d3fc62b89?q=80&w=1200",
    overlay: "linear-gradient(45deg, rgba(59,130,246,0.35) 0%, transparent 50%)",
    badge: "Gaming Series",
    badgeBg: "#f43f5e",
    badgeFg: "#ffffff",
    icon: Gamepad2,
    title: "GAMING ĐỈNH CAO — RTX 50 SERIES",
    subtitle: "Voucher giảm đến 5 triệu · Trả góp 0% · Tặng bàn phím cơ",
    subColor: "#fbbf24",
  },
  {
    image: "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?q=80&w=1200",
    overlay: "linear-gradient(45deg, rgba(16,185,129,0.35) 0%, transparent 50%)",
    badge: "MacBook M-Series",
    badgeBg: "#ffffff",
    badgeFg: "#0f172a",
    icon: Apple,
    title: "MACBOOK PRO M4 — SỨC MẠNH TỐI THƯỢNG",
    subtitle: "Giảm ngay 4 triệu · Thu cũ đổi mới trợ giá 2 triệu",
    subColor: "#86efac",
  },
  {
    image: "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?q=80&w=1200",
    overlay: "linear-gradient(45deg, rgba(245,158,11,0.35) 0%, transparent 50%)",
    badge: "Sinh viên Deal",
    badgeBg: "#10b981",
    badgeFg: "#ffffff",
    icon: GraduationCap,
    title: "MÙA TỰU TRƯỜNG — LAPTOP CHO SINH VIÊN",
    subtitle: "Giảm đến 30% · Balo + Chuột không dây miễn phí",
    subColor: "#fef3c7",
  },
  {
    image: "https://images.unsplash.com/photo-1525547719571-a2d4ac8945e2?q=80&w=1200",
    overlay: "linear-gradient(45deg, rgba(168,85,247,0.35) 0%, transparent 50%)",
    badge: "Đồ họa chuyên nghiệp",
    badgeBg: "#a855f7",
    badgeFg: "#ffffff",
    icon: Cpu,
    title: "WORKSTATION ĐỒ HỌA — DÀNH CHO CREATOR",
    subtitle: "RAM 32GB · Màn hình chuẩn màu DCI-P3 · Giảm đến 7 triệu",
    subColor: "#ddd6fe",
  },
];

// ─── 3 banner nhỏ dạng ô chữ (giữ nguyên phong cách gốc, không ảnh) ──────────────────
const miniBanners = [
  {
    bg: "linear-gradient(135deg, #fef3c7 0%, #fde68a 100%)",
    fg: "#78350f",
    border: "#fcd34d",
    iconBg: "#f59e0b",
    iconFg: "#ffffff",
    icon: Tag,
    tag: "Hot deal",
    title: "Giảm đến 50%",
    desc: "Laptop tồn kho cuối mùa",
    href: "#",
  },
  {
    bg: "linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%)",
    fg: "#1e3a8a",
    border: "#93c5fd",
    iconBg: "#3b82f6",
    iconFg: "#ffffff",
    icon: ShieldCheck,
    tag: "Bảo hành",
    title: "Bảo hành 24 tháng",
    desc: "Đổi trả 7 ngày miễn phí",
    href: "#",
  },
  {
    bg: "linear-gradient(135deg, #fce7f3 0%, #fbcfe8 100%)",
    fg: "#831843",
    border: "#f9a8d4",
    iconBg: "#ec4899",
    iconFg: "#ffffff",
    icon: Headphones,
    tag: "Hỗ trợ 24/7",
    title: "Tư vấn miễn phí",
    desc: "Hotline 1800.9999",
    href: "#",
  },
];

const currentSlide = ref(0);
const SLIDE_INTERVAL = 5000;
let slideTimer = null;

const startSlider = () => {
  slideTimer = setInterval(() => {
    currentSlide.value = (currentSlide.value + 1) % heroSlides.length;
  }, SLIDE_INTERVAL);
};

const stopSlider = () => {
  if (slideTimer) clearInterval(slideTimer);
  slideTimer = null;
};

const resumeSlider = () => startSlider();
const pauseSlider = () => stopSlider();

const nextSlide = () => {
  stopSlider();
  currentSlide.value = (currentSlide.value + 1) % heroSlides.length;
  startSlider();
};

const prevSlide = () => {
  stopSlider();
  currentSlide.value = (currentSlide.value - 1 + heroSlides.length) % heroSlides.length;
  startSlider();
};

const goToSlide = (idx) => {
  stopSlider();
  currentSlide.value = idx;
  startSlider();
};

// ─── Flashsale ─────────────────────────────────────────────────────────────────────
// Lấy TOP sản phẩm mới nhất (group theo sanPhamId như filteredProducts) làm deal flashsale.
// Không phải sản phẩm nào cũng có giaGoc, nên giả định: sản phẩm flashsale = sản phẩm có
// giaGoc hợp lệ VÀ giaGoc > giaBan; fallback nếu không có thì lấy 10 sp đầu tiên.
const flashsaleProducts = computed(() => {
  const list = groupBySanPham(products.value);
  const withDiscount = list.filter(
    (p) => Number(p.giaGoc) > Number(p.giaBan) && Number(p.giaGoc) > 0,
  );
  const source = withDiscount.length >= 6 ? withDiscount : list;
  return source.slice(0, 12);
});

// Countdown đến 22:00 hôm nay (giờ VN) — đơn giản là 2 tiếng từ lúc vào trang.
const countdown = reactive({ h: 0, m: 0, s: 0 });
let countdownTimer = null;
const startCountdown = () => {
  const target = new Date();
  target.setHours(22, 0, 0, 0);
  if (target.getTime() <= Date.now()) target.setDate(target.getDate() + 1);
  countdownTimer = setInterval(() => {
    const diff = Math.max(0, target.getTime() - Date.now());
    countdown.h = Math.floor(diff / 3_600_000);
    countdown.m = Math.floor((diff % 3_600_000) / 60_000);
    countdown.s = Math.floor((diff % 60_000) / 1000);
  }, 1000);
};

// Flashsale carousel — 5 card/hiển thị
const flashsaleIndex = ref(0);
const flashsalePrev = () => {
  if (flashsaleIndex.value > 0) flashsaleIndex.value--;
};
const flashsaleNext = () => {
  if (flashsaleIndex.value < flashsaleProducts.value.length - 1)
    flashsaleIndex.value++;
};

const discountPct = (p) => {
  const now = Number(p.giaBan) || 0;
  const orig = Number(p.giaGoc) || 0;
  if (orig <= now) return 0;
  return Math.round(((orig - now) / orig) * 100);
};

// ─── Helper lọc sản phẩm theo từ khóa ─────────────────────────────────
const locTheoTuKhoa = (keywords) => {
  const kws = keywords.map((k) => String(k || "").toLowerCase());
  return groupBySanPham(products.value).filter((p) => {
    const tags = (p.phanLoaiTags || "").toLowerCase();
    const name = (p.tenSanPham || "").toLowerCase();
    const brand = (p.tenThuongHieu || "").toLowerCase();
    const cat = (p.tenDanhMuc || "").toLowerCase();
    return kws.some(
      (kw) => tags.includes(kw) || cat.includes(kw) || brand.includes(kw) || name.includes(kw)
    );
  });
};

// Sản phẩm dành cho sinh viên
const studentProducts = computed(() => locTheoTuKhoa([
  "sinh_vien", "sinhviên", "van_phong", "văn phòng", "student", "office",
]).slice(0, 8));

// Gaming
const gamingProducts = computed(() => locTheoTuKhoa([
  "gaming", "rog", "tuf", "legion", "predator", "nitro", "alienware",
]).slice(0, 8));

// Đồ họa
const graphicsProducts = computed(() => locTheoTuKhoa([
  "do_hoa", "ky_thuat", "đồ họa", "kỹ thuật", "creator", "proart", "precision", "workstation",
]).slice(0, 8));

// MacBook
const macbookProducts = computed(() => locTheoTuKhoa([
  "macbook", "apple", "imac",
]).slice(0, 8));

// Cao cấp - Mỏng nhẹ
const premiumProducts = computed(() => locTheoTuKhoa([
  "zenbook", "yoga", "slim", "xps", "envy", "spectre", "gram", "premium",
]).slice(0, 8));

// Theo từng hãng
const asusProducts = computed(() => locTheoTuKhoa(["asus"]).slice(0, 8));
const lenovoProducts = computed(() => locTheoTuKhoa(["lenovo"]).slice(0, 8));
const dellProducts = computed(() => locTheoTuKhoa(["dell"]).slice(0, 8));
const hpProducts = computed(() => locTheoTuKhoa(["hp"]).slice(0, 8));
const msiProducts = computed(() => locTheoTuKhoa(["msi"]).slice(0, 8));
const acerProducts = computed(() => locTheoTuKhoa(["acer"]).slice(0, 8));
const gigabyteProducts = computed(() => locTheoTuKhoa(["gigabyte"]).slice(0, 8));
const lgProducts = computed(() => locTheoTuKhoa(["lg"]).slice(0, 8));
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

    <div class="container-xl py-3">
      <div class="row g-3 mb-3">
        <div class="col-xl-2 d-none d-xl-block">
          <div
            class="rounded-3 p-1 d-flex flex-column gap-1 h-100"
            style="
              background: #ffffff;
              border: 1px solid var(--border-color);
            "
          >
            <a
              v-for="cat in sidebarCats"
              :key="cat.id"
              href="#"
              class="d-flex align-items-center justify-content-between px-3 py-2 rounded-2 text-decoration-none fw-bold"
              :style="activeSidebarCat && activeSidebarCat.id === cat.id
                ? 'background:linear-gradient(135deg, #fce7f3, #fbcfe8); color:#be185d; font-size:13px; transition:all 0.15s;'
                : 'color:#1e293b; font-size:13px; transition:all 0.15s;'"
              @mouseenter="(e) => { e.currentTarget.style.background = 'linear-gradient(135deg, #fce7f3, #fbcfe8)'; e.currentTarget.style.color = '#be185d'; e.currentTarget.style.transform = 'translateX(4px)'; }"
              @mouseleave="(e) => { const isActive = activeSidebarCat && activeSidebarCat.id === cat.id; e.currentTarget.style.background = isActive ? 'linear-gradient(135deg, #fce7f3, #fbcfe8)' : ''; e.currentTarget.style.color = isActive ? '#be185d' : '#1e293b'; e.currentTarget.style.transform = ''; }"
              @click.prevent="selectSidebarCat(cat)"
            >
              <span class="d-flex align-items-center gap-2">
                <component :is="cat.icon" :size="16" :color="activeSidebarCat && activeSidebarCat.id === cat.id ? '#be185d' : '#64748b'" />
                {{ cat.name }}
              </span>
              <span :style="`color: ${activeSidebarCat && activeSidebarCat.id === cat.id ? '#be185d' : '#cbd5e1'}; font-weight:900;`">›</span>
            </a>
          </div>
        </div>

        <div class="col-12 col-xl-7">
          <!-- ─── Hero slider: 5 slides tự động chuyển mỗi 5s, có nút prev/next + dots ─── -->
          <div
            class="position-relative rounded-3 overflow-hidden mb-2"
            style="height: 280px"
            @mouseenter="pauseSlider"
            @mouseleave="resumeSlider"
          >
            <template v-for="(slide, idx) in heroSlides" :key="idx">
              <Transition name="hero-fade">
                <div
                  v-if="currentSlide === idx"
                  class="position-absolute top-0 start-0 w-100 h-100"
                  style="z-index: 1;"
                >
                  <img
                    :src="slide.image"
                    :alt="slide.title"
                    class="w-100 h-100"
                    style="object-fit: cover"
                  />
                  <div
                    class="position-absolute top-0 start-0 w-100 h-100"
                    :style="`background:${slide.overlay}`"
                  ></div>
                  <div
                    class="position-absolute bottom-0 start-0 p-4 w-100"
                    style="
                      background: linear-gradient(
                        to top,
                        rgba(0, 0, 0, 0.85) 0%,
                        transparent 60%
                      );
                    "
                  >
                    <span
                      class="badge fw-black text-uppercase mb-2"
                      :style="`background:${slide.badgeBg}; color:${slide.badgeFg}; letter-spacing:0.08em; font-size:10px;`"
                    >
                      <component :is="slide.icon" :size="11" style="vertical-align:-2px;" /> {{ slide.badge }}
                    </span>
                    <h2
                      class="text-white fw-black mb-1"
                      style="
                        font-size: 1.3rem;
                        text-shadow: 0 2px 8px rgba(0, 0, 0, 0.8);
                      "
                    >
                      {{ slide.title }}
                    </h2>
                    <p
                      class="fw-bold mb-0"
                      :style="`color:${slide.subColor}; font-size:0.85rem;`"
                    >
                      {{ slide.subtitle }}
                    </p>
                  </div>
                </div>
              </Transition>
            </template>

            <button
              class="position-absolute top-50 start-0 translate-middle-y btn btn-sm rounded-circle d-flex align-items-center justify-content-center"
              style="z-index: 5; width:32px; height:32px; padding:0; background:rgba(0,0,0,0.45); color:#fff; border:none; backdrop-filter:blur(4px);"
              aria-label="Previous"
              @click="prevSlide"
            >
              <ChevronLeft :size="18" />
            </button>
            <button
              class="position-absolute top-50 end-0 translate-middle-y btn btn-sm rounded-circle d-flex align-items-center justify-content-center"
              style="z-index: 5; width:32px; height:32px; padding:0; background:rgba(0,0,0,0.45); color:#fff; border:none; backdrop-filter:blur(4px);"
              aria-label="Next"
              @click="nextSlide"
            >
              <ChevronRight :size="18" />
            </button>

            <div
              class="position-absolute bottom-0 end-0 m-3 d-flex gap-1"
              style="z-index: 5;"
            >
              <button
                v-for="(_, idx) in heroSlides"
                :key="idx"
                type="button"
                class="border-0 p-0 rounded-pill"
                :style="`width:${currentSlide === idx ? '22px' : '8px'}; height:8px; background:${currentSlide === idx ? '#fff' : 'rgba(255,255,255,0.5)'}; transition:all 0.3s; cursor:pointer;`"
                :aria-label="`Slide ${idx + 1}`"
                @click="goToSlide(idx)"
              ></button>
            </div>
          </div>

          <!-- ─── 3 banner phụ: dạng ô chữ có icon (giữ phong cách gốc) ─── -->
          <div class="row g-2">
            <div v-for="(banner, idx) in miniBanners" :key="idx" class="col-4">
              <a
                :href="banner.href"
                class="d-block p-2 rounded-2 text-decoration-none h-100"
                :style="`background:${banner.bg}; color:${banner.fg}; border:1px solid ${banner.border}; transition:all 0.2s;`"
                @mouseenter="(e) => { e.currentTarget.style.transform = 'translateY(-2px)'; e.currentTarget.style.boxShadow = '0 6px 16px rgba(0,0,0,0.15)'; }"
                @mouseleave="(e) => { e.currentTarget.style.transform = ''; e.currentTarget.style.boxShadow = ''; }"
              >
                <div class="d-flex align-items-center gap-2 mb-1">
                  <span
                    class="d-flex align-items-center justify-content-center rounded-circle flex-shrink-0"
                    :style="`width:28px; height:28px; background:${banner.iconBg}; color:${banner.iconFg};`"
                  >
                    <component :is="banner.icon" :size="14" />
                  </span>
                  <span
                    class="fw-black text-uppercase"
                    style="font-size:9px; letter-spacing:0.08em;"
                  >
                    {{ banner.tag }}
                  </span>
                </div>
                <p
                  class="fw-black mb-0"
                  style="font-size:12px; line-height:1.3;"
                >
                  {{ banner.title }}
                </p>
                <p
                  class="mb-0 fw-semibold"
                  style="font-size:10px; opacity:0.85;"
                >
                  {{ banner.desc }}
                </p>
              </a>
            </div>
          </div>
        </div>

        <div class="col-12 col-xl-3">
          <div
            class="rounded-3 p-3 h-100 d-flex flex-column gap-3"
            style="
              background: #ffffff;
              border: 1px solid var(--border-color);
            "
          >
            <div class="d-flex align-items-center gap-2">
              <div
                class="rounded-circle d-flex align-items-center justify-content-center fw-black flex-shrink-0"
                style="
                  width: 44px;
                  height: 44px;
                  background: linear-gradient(135deg, #ec4899, #be185d);
                  color: #ffffff;
                  font-size: 0.8rem;
                  box-shadow: 0 4px 10px rgba(236,72,153,0.35);
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
                    color: #1e293b;
                  "
                >
                  {{ t("home.brandName") }}
                </h4>
                <p
                  class="mb-0"
                  style="font-size: 10px; color: #64748b;"
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
                  transition: all 0.15s;
                  color: #1e293b;
                  background: #ffffff;
                  border: 1px solid #fce7f3;
                "
                @mouseenter="
                  (e) => {
                    e.currentTarget.style.background = 'linear-gradient(135deg, #fce7f3, #fbcfe8)';
                    e.currentTarget.style.borderColor = '#f9a8d4';
                    e.currentTarget.style.transform = 'translateX(4px)';
                  }
                "
                @mouseleave="
                  (e) => {
                    e.currentTarget.style.background = '#ffffff';
                    e.currentTarget.style.borderColor = '#fce7f3';
                    e.currentTarget.style.transform = '';
                  }
                "
              >
                <component :is="link.icon" :size="13" style="vertical-align:-2px; color:#ec4899;" /> {{ link.text }}
              </a>
            </div>
            <div
              class="mt-auto text-center fw-black py-2 rounded-2 small"
              style="
                background: linear-gradient(135deg, #ec4899, #be185d);
                color: #ffffff;
                font-size: 11px;
                font-weight: 800;
                letter-spacing: 0.06em;
                box-shadow: 0 4px 12px rgba(236,72,153,0.35);
              "
            >
              {{ t("home.examBanner") }}
            </div>
          </div>
        </div>
      </div>

      <section id="deal-section" class="mt-3">
        <!-- ─── Banner khuyến mãi full-width (nằm trên phần deal sốc) ─── -->
        <a
          href="#"
          class="d-block rounded-3 overflow-hidden mb-3 position-relative"
          style="height:90px; transition:transform 0.2s;"
          @mouseenter="(e) => e.currentTarget.style.transform = 'translateY(-2px)'"
          @mouseleave="(e) => e.currentTarget.style.transform = ''"
        >
          <img
            src="https://images.unsplash.com/photo-1556742111-a301076d9d18?q=80&w=1600"
            alt="Ưu đãi trả góp sinh viên"
            class="w-100 h-100"
            style="object-fit: cover; display: block;"
          />
          <div
            class="position-absolute top-0 start-0 w-100 h-100"
            style="background:linear-gradient(90deg, rgba(15,23,42,0.85) 0%, rgba(15,23,42,0.45) 40%, rgba(15,23,42,0.15) 100%);"
          ></div>
          <div class="position-absolute top-0 start-0 w-100 h-100 d-flex align-items-center px-4">
            <div class="d-flex align-items-center gap-3">
              <span
                class="badge fw-black text-uppercase"
                style="background:linear-gradient(135deg, #f43f5e, #ec4899); color:#ffffff; font-size:11px; letter-spacing:0.08em; padding:6px 12px;"
              >
                <Sparkles :size="11" style="vertical-align:-2px;" /> Hot
              </span>
              <div>
                <p class="fw-black text-white mb-0" style="font-size:18px; line-height:1.2; text-shadow:0 2px 6px rgba(0,0,0,0.6);">
                  TUẦN LỄ LAPTOP — DEAL KHỦNG GIẢM ĐẾN <span style="color:#fde047;">5 TRIỆU</span>
                </p>
                <p class="mb-0 fw-semibold" style="font-size:12px; color:rgba(255,255,255,0.9); text-shadow:0 1px 3px rgba(0,0,0,0.6);">
                  Trả góp 0% lên đến 12 tháng · Tặng Balo Gaming · Áp dụng đến hết 31/10
                </p>
              </div>
            </div>
          </div>
          <div
            class="position-absolute top-50 end-0 translate-middle-y d-flex align-items-center gap-2 pe-4"
          >
            <span class="text-white fw-black" style="font-size:13px; text-shadow:0 2px 4px rgba(0,0,0,0.7);">
              Săn deal ngay
            </span>
            <span
              class="d-flex align-items-center justify-content-center rounded-circle fw-black"
              style="width:34px; height:34px; background:#fde047; color:#831843; font-size:18px;"
            >
              →
            </span>
          </div>
        </a>

        <!-- ─── Flashsale carousel — dùng dữ liệu thật từ API ─── -->
        <div
          v-if="flashsaleProducts.length"
          class="position-relative mb-3"
          style="
            background: linear-gradient(160deg, #ec4899 0%, #db2777 30%, #be185d 70%, #9d174d 100%);
            border-radius: 20px;
            padding: 16px 16px 20px;
            box-shadow:
              0 0 0 1px rgba(255,255,255,0.08) inset,
              0 4px 0 0 rgba(0,0,0,0.12) inset,
              0 8px 24px rgba(219,39,119,0.35),
              0 2px 8px rgba(0,0,0,0.15);
            border: 2px solid rgba(255,255,255,0.12);
          "
        >
          <!-- Ánh sáng 3D góc trái trên -->
          <div
            style="
              position: absolute; top: -1px; left: -1px; right: -1px; height: 80px;
              background: linear-gradient(180deg, rgba(255,255,255,0.15) 0%, transparent 100%);
              border-radius: 20px 20px 0 0;
              pointer-events: none;
            "
          ></div>

          <div class="d-flex align-items-center justify-content-between mb-3" style="padding: 0 4px;">
            <div class="d-flex align-items-center gap-2">
              <span
                class="d-inline-flex align-items-center justify-content-center rounded-pill fw-black"
                style="
                  background: linear-gradient(135deg, #fbbf24, #f59e0b);
                  color: #7c2d12;
                  padding: 7px 18px;
                  font-size: 14px;
                  letter-spacing: 0.05em;
                  border: 1px solid rgba(255,255,255,0.4);
                  box-shadow: 0 4px 12px rgba(245,158,11,0.4), 0 2px 4px rgba(0,0,0,0.15) inset;
                "
              >
                <Flame :size="15" style="vertical-align:-2px;" /> FLASHSALE TỰU TRƯỜNG
              </span>
            </div>
            <div class="d-flex align-items-center gap-2">
              <span class="fw-bold text-white" style="font-size:12px; opacity:0.9;">KẾT THÚC SAU</span>
              <span
                class="fw-black px-2 py-1 rounded-2 text-white"
                style="
                  background: linear-gradient(135deg, #1f2937, #111827);
                  font-size:14px; min-width:34px; text-align:center;
                  box-shadow: 0 2px 4px rgba(0,0,0,0.3) inset, 0 1px 0 rgba(255,255,255,0.1);
                  border: 1px solid rgba(255,255,255,0.1);
                "
              >{{ String(countdown.h).padStart(2,'0') }}</span>
              <span class="fw-black text-white" style="font-size:14px; opacity:0.7;">:</span>
              <span
                class="fw-black px-2 py-1 rounded-2 text-white"
                style="
                  background: linear-gradient(135deg, #1f2937, #111827);
                  font-size:14px; min-width:34px; text-align:center;
                  box-shadow: 0 2px 4px rgba(0,0,0,0.3) inset, 0 1px 0 rgba(255,255,255,0.1);
                  border: 1px solid rgba(255,255,255,0.1);
                "
              >{{ String(countdown.m).padStart(2,'0') }}</span>
              <span class="fw-black text-white" style="font-size:14px; opacity:0.7;">:</span>
              <span
                class="fw-black px-2 py-1 rounded-2 text-white"
                style="
                  background: linear-gradient(135deg, #1f2937, #111827);
                  font-size:14px; min-width:34px; text-align:center;
                  box-shadow: 0 2px 4px rgba(0,0,0,0.3) inset, 0 1px 0 rgba(255,255,255,0.1);
                  border: 1px solid rgba(255,255,255,0.1);
                "
              >{{ String(countdown.s).padStart(2,'0') }}</span>
            </div>
          </div>

          <div class="position-relative px-1">
            <button
              v-show="flashsaleIndex > 0"
              class="position-absolute top-50 start-0 translate-middle-y btn btn-sm rounded-circle d-flex align-items-center justify-content-center"
              style="
                z-index:5; width:36px; height:36px; padding:0;
                background: linear-gradient(135deg, #ffffff, #fce7f3);
                color:#be185d;
                border: 2px solid rgba(255,255,255,0.6);
                box-shadow: 0 4px 12px rgba(0,0,0,0.2), 0 2px 4px rgba(219,39,119,0.2);
              "
              aria-label="Prev"
              @click="flashsalePrev"
            >
              <ChevronLeft :size="18" />
            </button>
            <button
              v-show="flashsaleIndex < flashsaleProducts.length - 1"
              class="position-absolute top-50 end-0 translate-middle-y btn btn-sm rounded-circle d-flex align-items-center justify-content-center"
              style="
                z-index:5; width:36px; height:36px; padding:0;
                background: linear-gradient(135deg, #ffffff, #fce7f3);
                color:#be185d;
                border: 2px solid rgba(255,255,255,0.6);
                box-shadow: 0 4px 12px rgba(0,0,0,0.2), 0 2px 4px rgba(219,39,119,0.2);
              "
              aria-label="Next"
              @click="flashsaleNext"
            >
              <ChevronRight :size="18" />
            </button>

            <div class="overflow-hidden">
              <div
                class="d-flex gap-3"
                style="transition:transform 0.4s ease;"
                :style="`transform:translateX(calc(-${flashsaleIndex} * (100% / 5 + 0.75rem / 5)));`"
              >
                <div
                  v-for="p in flashsaleProducts"
                  :key="p.bienTheId"
                  style="flex: 0 0 calc((100% - 4 * 0.75rem) / 5);"
                >
                  <div
                    class="bg-white h-100 position-relative d-flex flex-column h-100"
                    style="
                      cursor:pointer;
                      border-radius: 16px;
                      overflow: hidden;
                      box-shadow:
                        0 6px 0 0 rgba(219,39,119,0.3),
                        0 8px 20px rgba(0,0,0,0.12),
                        0 2px 6px rgba(0,0,0,0.08);
                      border: 1px solid rgba(219,39,119,0.08);
                      transition: transform 0.2s, box-shadow 0.2s;
                    "
                    @click="openProduct(p)"
                    @mouseenter="(e) => { e.currentTarget.style.transform = 'translateY(-4px)'; e.currentTarget.style.boxShadow = '0 10px 0 0 rgba(219,39,119,0.3), 0 14px 28px rgba(0,0,0,0.18), 0 4px 10px rgba(0,0,0,0.12)'; }"
                    @mouseleave="(e) => { e.currentTarget.style.transform = ''; e.currentTarget.style.boxShadow = '0 6px 0 0 rgba(219,39,119,0.3), 0 8px 20px rgba(0,0,0,0.12), 0 2px 6px rgba(0,0,0,0.08)'; }"
                  >
                    <span
                      v-if="discountPct(p) > 0"
                      class="position-absolute top-0 end-0 m-2 fw-black text-white"
                      style="
                        background: linear-gradient(135deg, #ec4899, #be185d);
                        font-size:11px;
                        padding:3px 8px;
                        border-radius: 10px;
                        z-index:3;
                        box-shadow: 0 2px 8px rgba(219,39,119,0.4);
                        border: 1px solid rgba(255,255,255,0.3);
                      "
                    >
                      -{{ discountPct(p) }}%
                    </span>

                    <div
                      class="d-flex align-items-center justify-content-center"
                      style="height:115px; background:linear-gradient(180deg, #fff1f2, #fce7f3);"
                    >
                      <img
                        :src="safeImage(p)"
                        :alt="p.tenSanPham"
                        style="max-height:100%; max-width:100%; object-fit:contain;"
                      />
                    </div>

                    <div class="p-2 d-flex flex-column flex-grow-1">
                      <p
                        class="fw-bold mb-1"
                        style="font-size:11px; line-height:1.3; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; line-clamp:2; -webkit-box-orient:vertical; color:#1f2937;"
                      >
                        {{ p.tenSanPham }}
                      </p>
                      <div class="mt-auto">
                        <p
                          v-if="Number(p.giaGoc) > Number(p.giaBan)"
                          class="text-muted text-decoration-line-through mb-0"
                          style="font-size:10px;"
                        >
                          {{ formatPrice(p.giaGoc) }}
                        </p>
                        <p
                          class="fw-black mb-1"
                          style="font-size:13px; color:#be185d;"
                        >
                          {{ formatPrice(p.giaBan) }}
                        </p>
                        <div
                          class="rounded-pill overflow-hidden"
                          style="height:16px; background:#fce7f3;"
                        >
                          <div
                            class="d-flex align-items-center justify-content-center h-100"
                            style="background:linear-gradient(90deg, #ec4899, #f43f5e); width:30%; transition:width 0.5s;"
                          >
                            <span
                              class="fw-black text-white"
                              style="font-size:9px; white-space:nowrap;"
                            >
                              🔥 {{ Math.floor(Math.random() * 50) + 1 }}/{{ Math.floor(Math.random() * 50) + 50 }}
                            </span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <p
            class="text-center fw-semibold mb-0"
            style="font-size:11px; color:rgba(255,255,255,0.8); margin-top:14px; padding: 0 4px;"
          >
            ⚡ Áp dụng cho hàng thành viên S-TEACHER, S-STUDENT — Chỉ thanh toán online thành công — Mỗi SĐT chỉ được mua 1 sản phẩm cùng loại.
          </p>
        </div>

        <!-- ─── Tabs lớn (DEAL SỐC / SẢN PHẨM HOT TREND / HÀNG MỚI VỀ) ─── -->
        <div
          class="d-flex gap-2"
          style="flex-wrap:nowrap; margin-bottom:-3px; position:relative; z-index:2;"
        >
          <button
            v-for="tab in dealTabs"
            :key="tab.id"
            class="flex-grow-1 d-flex align-items-center justify-content-center gap-2"
            :style="activeTab === tab.id
              ? 'background:linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%); color:#ffffff; border:3px solid #3b82f6; border-bottom:3px solid #ffffff; border-radius:14px 14px 0 0; padding:16px 8px; font-size:24px; font-weight:900; text-transform:uppercase; letter-spacing:0.04em; -webkit-text-stroke:0.6px #ffffff; box-shadow:0 -4px 12px rgba(37,99,235,0.25);'
              : 'background:#ffffff; color:#1d4ed8; border:3px solid transparent; border-bottom:3px solid #3b82f6; border-radius:14px 14px 0 0; padding:16px 8px; font-size:24px; font-weight:900; text-transform:uppercase; letter-spacing:0.04em; -webkit-text-stroke:0.5px #1d4ed8;'"
            @click="activeTab = tab.id; activeFilter = tab.filter; activeCatId = tab.catId; activeSidebarCat = null;"
          >
            <Flame
              v-if="activeTab === tab.id"
              :size="24"
              fill="#fbbf24"
              color="#f97316"
            />
            <span>{{ tab.label }}</span>
            <ChevronRight v-if="activeTab === tab.id" :size="16" style="color:#ffffff;" />
          </button>
        </div>

        <!-- ─── Khung xanh bao quanh (chỉ bo 2 góc dưới, nền xanh nhạt) ─── -->
        <div
          class="position-relative"
          style="
            background: #eff6ff;
            border: 3px solid #3b82f6;
            border-radius: 0 0 14px 14px;
            border-top: none;
            padding: 16px;
            box-shadow: 0 6px 20px rgba(59,130,246,0.1);
          "
        >
          <!-- ─── Chip danh mục ─── -->
          <div
            class="d-flex flex-wrap gap-2 mb-3 align-items-center justify-content-center"
            style="padding: 4px 0;"
          >
            <button
              v-for="cat in apiCats.slice(0, 8)"
              :key="cat.id"
              class="fw-bold"
              :style="activeCatId === cat.id
                ? 'background:#eff6ff; color:#2563eb; border:1.5px solid #3b82f6; border-radius:999px; padding:6px 16px; font-size:12px;'
                : 'background:#ffffff; color:#475569; border:1.5px solid #e2e8f0; border-radius:999px; padding:6px 16px; font-size:12px;'"
              @click="activeCatId = cat.id; activeFilter = 'all'"
            >
              {{ cat.tenDanhMuc }}
            </button>
          </div>

          <!-- ─── Grid sản phẩm 1 hàng căn đều 5 ô ─── -->
          <div
            v-if="dealProducts.length === 0"
            class="text-center py-3 small"
            style="color: #64748b;"
          >
            {{ t("home.noMatch") }}
          </div>

          <div v-else class="d-flex gap-3" style="width:100%; flex-wrap:nowrap;">
            <div
              v-for="product in dealProducts"
              :key="product.sanPhamId"
              :style="`flex:1; min-width:0; max-width:${100/dealProducts.length}%;`"
            >
              <!-- ─── Card sản phẩm (vừa khít, cân đối) ─── -->
              <div
                class="h-100 d-flex flex-column"
                style="
                  background: #ffffff;
                  border: 1px solid #bfdbfe;
                  border-radius: 8px;
                  overflow: hidden;
                  transition: all 0.2s;
                "
                @click="openProduct(product)"
                @mouseenter="(e) => { e.currentTarget.style.borderColor='#3b82f6'; e.currentTarget.style.boxShadow='0 4px 14px rgba(59,130,246,0.12)'; }"
                @mouseleave="(e) => { e.currentTarget.style.borderColor='#bfdbfe'; e.currentTarget.style.boxShadow='none'; }"
              >
                <!-- Ảnh sản phẩm (lấp đầy) -->
                <div
                  class="position-relative"
                  style="
                    background: linear-gradient(180deg, #fce7f3 0%, #fdf2f8 100%);
                    height: 160px;
                    overflow: hidden;
                  "
                >
                  <span
                    class="position-absolute"
                    style="top:8px; left:8px; background:#6b7280; color:#fff; font-size:10px; font-weight:700; padding:3px 8px; border-radius:4px; z-index:2;"
                  >
                    Hết hàng
                  </span>
                  <button
                    class="position-absolute d-flex align-items-center justify-content-center"
                    style="top:8px; right:8px; width:28px; height:28px; background:#fff; border:1px solid #e2e8f0; border-radius:50%; color:#ef4444; padding:0; z-index:2;"
                    @click.stop="toggleWishlist(product)"
                  >
                    <Heart :size="13" :fill="isWishlisted(product.bienTheId) ? '#ef4444' : 'none'" />
                  </button>
                  <img
                    :src="safeImage(product)"
                    :alt="product.tenSanPham"
                    style="width:100%; height:100%; object-fit:cover;"
                  />
                </div>

                <!-- Body -->
                <div class="p-2 d-flex flex-column flex-grow-1">
                  <!-- Tên sản phẩm -->
                  <p
                    class="fw-black mb-1"
                    style="
                      font-size:15px;
                      line-height:1.35;
                      overflow:hidden;
                      display:-webkit-box;
                      -webkit-line-clamp:2;
                      line-clamp:2;
                      -webkit-box-orient:vertical;
                      color:#0f172a;
                      min-height:40px;
                    "
                  >
                    {{ product.tenSanPham }}
                  </p>

                  <!-- Thương hiệu -->
                  <p class="mb-auto" style="font-size:12px; color:#64748b; font-weight:600;">
                    {{ product.tenThuongHieu || product.tenDanhMuc }}
                  </p>

                  <!-- Giá -->
                  <p class="fw-black mb-1" style="font-size:20px; color:#dc2626; line-height:1;">
                    {{ formatPrice(product.giaBan) }}
                  </p>

                  <!-- Giao nhanh -->
                  <p class="mb-2 d-flex align-items-center gap-1" style="font-size:12px; color:#475569; font-weight:600;">
                    <Truck :size="13" /> Giao nhanh 2H
                  </p>

                  <!-- Tags -->
                  <div class="d-flex flex-wrap gap-1 mb-2">
                    <span
                      v-if="(product.phanLoaiTags||'').includes('gaming') || (product.tenDanhMuc||'').toLowerCase().includes('gaming')"
                      style="background:#fce7f3; color:#be185d; font-size:11px; font-weight:800; padding:4px 10px; border-radius:4px; text-transform:uppercase; letter-spacing:0.04em;"
                    >Gaming</span>
                    <span
                      v-if="(product.phanLoaiTags||'').includes('do_hoa') || (product.phanLoaiTags||'').includes('ky_thuat')"
                      style="background:#fce7f3; color:#be185d; font-size:11px; font-weight:800; padding:4px 10px; border-radius:4px; text-transform:uppercase; letter-spacing:0.04em;"
                    >Đồ họa</span>
                  </div>

                  <!-- Nút Thêm vào giỏ -->
                  <button
                    class="btn-3d-blue w-100 mb-2"
                    style="font-size:13px; padding:9px 12px;"
                    @click.stop="handleQuickAdd(product)"
                  >
                    <ShoppingCart :size="14" style="vertical-align:-2px;" /> Thêm vào giỏ
                  </button>

                  <!-- So sánh -->
                  <label class="d-flex align-items-center gap-2" style="font-size:12px; color:#1e293b; cursor:pointer; font-weight:600;">
                    <input
                      type="checkbox"
                      :checked="isComparing(product)"
                      :disabled="!isComparing(product) && compareList.length >= MAX_COMPARE"
                      @click.stop="toggleCompare(product)"
                    />
                    <span>So sánh</span>
                  </label>
                </div>
              </div>
            </div>
          </div>
        </div><!-- /khung xanh -->
      </section>

      <!-- ─── Section: Dành cho sinh viên (2 banners trái + 4 products/hàng) ─── -->
      <section class="mt-4" style="border-radius:16px; padding:20px;">
        <!-- Tiêu đề -->
        <div class="d-flex align-items-center gap-3 mb-3">
          <div class="d-flex align-items-center gap-2">
            <GraduationCap :size="28" style="color:#ec4899;" />
            <h2
              class="fw-black mb-0"
              style="font-size:1.4rem; color:#be185d; text-transform:uppercase; letter-spacing:0.04em;"
            >
              Dành cho sinh viên
            </h2>
          </div>
          <a
            href="#"
            class="fw-bold text-decoration-none d-flex align-items-center gap-1 ms-auto"
            style="font-size:13px; color:#ec4899;"
          >
            Xem tất cả <ChevronRight :size="16" />
          </a>
        </div>

        <div class="row g-3">
          <!-- 2 Banner dọc bên trái - sát mép -->
          <div class="col-12 col-md-3 col-lg-2" style="padding-left:0;">
            <div class="d-flex flex-row d-md-none gap-2">
              <!-- Banner 1: Ưu đãi sinh viên (mobile) -->
              <a
                href="#"
                class="flex-grow-1 d-block rounded-3 overflow-hidden position-relative"
                style="min-height:100px; text-decoration:none;"
              >
                <img
                  src="https://images.unsplash.com/photo-1522202176988-66273c2fd55f?q=80&w=400"
                  alt="Ưu đãi sinh viên"
                  class="w-100 h-100"
                  style="object-fit:cover; position:absolute; top:0; left:0;"
                />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(190,24,93,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-2">
                  <div>
                    <h3 class="fw-black text-white mb-0" style="font-size:11px; line-height:1.2;">LAPTOP SINH VIÊN</h3>
                    <p class="mb-0 fw-bold" style="font-size:9px; color:#fce7f3;">Giảm đến 30%</p>
                  </div>
                </div>
              </a>

              <!-- Banner 2: Trả góp (mobile) -->
              <a
                href="#"
                class="flex-grow-1 d-block rounded-3 overflow-hidden position-relative"
                style="min-height:100px; text-decoration:none;"
              >
                <img
                  src="https://images.unsplash.com/photo-1553484771-371a605b060b?q=80&w=400"
                  alt="Hỗ trợ trả góp"
                  class="w-100 h-100"
                  style="object-fit:cover; position:absolute; top:0; left:0;"
                />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(37,99,235,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-2">
                  <div>
                    <h3 class="fw-black text-white mb-0" style="font-size:11px; line-height:1.2;">TRẢ GÓP 0%</h3>
                    <p class="mb-0 fw-bold" style="font-size:9px; color:#dbeafe;">Qua thẻ tín dụng</p>
                  </div>
                </div>
              </a>
            </div>

            <!-- Desktop: banners dọc - cao hơn, sát mép -->
            <div class="d-none d-md-flex flex-column gap-2 h-100">
              <!-- Banner 1: Ưu đãi sinh viên (hồng) - dài hơn -->
              <a
                href="#"
                class="d-block rounded-3 overflow-hidden position-relative flex-grow-1"
                style="min-height:220px; text-decoration:none;"
                @mouseenter="(e) => e.currentTarget.style.transform='translateY(-3px)'"
                @mouseleave="(e) => e.currentTarget.style.transform=''"
              >
                <img
                  src="https://images.unsplash.com/photo-1522202176988-66273c2fd55f?q=80&w=500"
                  alt="Ưu đãi sinh viên"
                  class="w-100 h-100"
                  style="object-fit:cover; object-position:center top; position:absolute; top:0; left:0;"
                />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(190,24,93,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-3">
                  <div>
                    <h3 class="fw-black text-white mb-1" style="font-size:14px; line-height:1.2; text-shadow:0 2px 6px rgba(0,0,0,0.4);">
                      LAPTOP<br />SINH VIÊN
                    </h3>
                    <p class="mb-0 fw-bold" style="font-size:11px; color:#fce7f3;">
                      Giảm đến 30%
                    </p>
                  </div>
                </div>
              </a>

              <!-- Banner 2: Hỗ trợ trả góp (xanh dương) - dài hơn -->
              <a
                href="#"
                class="d-block rounded-3 overflow-hidden position-relative flex-grow-1"
                style="min-height:220px; text-decoration:none;"
                @mouseenter="(e) => e.currentTarget.style.transform='translateY(-3px)'"
                @mouseleave="(e) => e.currentTarget.style.transform=''"
              >
                <img
                  src="https://images.unsplash.com/photo-1553484771-371a605b060b?q=80&w=500"
                  alt="Hỗ trợ trả góp"
                  class="w-100 h-100"
                  style="object-fit:cover; object-position:center top; position:absolute; top:0; left:0;"
                />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(37,99,235,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-3">
                  <div>
                    <h3 class="fw-black text-white mb-1" style="font-size:14px; line-height:1.2; text-shadow:0 2px 6px rgba(0,0,0,0.4);">
                      TRẢ GÓP<br />0% LÃI SUẤT
                    </h3>
                    <p class="mb-0 fw-bold" style="font-size:11px; color:#dbeafe;">
                      Qua thẻ tín dụng
                    </p>
                  </div>
                </div>
              </a>
            </div>
          </div>

          <!-- Sản phẩm bên phải: 4 sản phẩm/hàng -->
          <div class="col-12 col-md-9 col-lg-10">
            <div class="row g-3">
              <div
                v-for="product in studentProducts.slice(0, 8)"
                :key="product.sanPhamId"
                class="col-6 col-md-3"
              >
                <div
                  class="h-100 d-flex flex-column bg-white rounded-3 overflow-hidden"
                  style="border: 1px solid #e2e8f0; transition: all 0.2s; cursor:pointer;"
                  @click="openProduct(product)"
                  @mouseenter="(e) => { e.currentTarget.style.borderColor='#ec4899'; e.currentTarget.style.boxShadow='0 4px 14px rgba(236,72,153,0.2)'; e.currentTarget.style.transform='translateY(-3px)'; }"
                  @mouseleave="(e) => { e.currentTarget.style.borderColor='#e2e8f0'; e.currentTarget.style.boxShadow='none'; e.currentTarget.style.transform=''; }"
                >
                  <!-- Ảnh sản phẩm -->
                  <div
                    class="position-relative"
                    style="height:180px; background:linear-gradient(180deg, #f8fafc, #f1f5f9);"
                  >
                    <button
                      class="position-absolute d-flex align-items-center justify-content-center"
                      style="top:8px; right:8px; width:28px; height:28px; background:#fff; border:1px solid #e2e8f0; border-radius:50%; color:#ef4444; padding:0; z-index:2;"
                      @click.stop="toggleWishlist(product)"
                    >
                      <Heart :size="13" :fill="isWishlisted(product.bienTheId) ? '#ef4444' : 'none'" />
                    </button>
                    <img
                      :src="safeImage(product)"
                      :alt="product.tenSanPham"
                      style="width:100%; height:100%; object-fit:contain; padding:8px;"
                    />
                  </div>

                  <!-- Body -->
                  <div class="p-2 d-flex flex-column flex-grow-1">
                    <!-- Tên sản phẩm -->
                    <p
                      class="fw-black mb-1"
                      style="font-size:13px; line-height:1.3; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; line-clamp:2; -webkit-box-orient:vertical; color:#0f172a; min-height:34px;"
                    >
                      {{ product.tenSanPham }}
                    </p>

                    <!-- Cấu hình (RAM, CPU, GPU) - to và rõ ràng -->
                    <div
                      class="mb-2 p-2 rounded-2"
                      style="background:#f1f5f9; font-size:11px; color:#475569; font-weight:600; line-height:1.5;"
                    >
                      <div class="d-flex align-items-center gap-1 mb-1">
                        <Cpu :size="11" style="color:#2563eb;" />
                        <span>Intel Core i5-1235U</span>
                      </div>
                      <div class="d-flex align-items-center gap-1">
                        <HardDrive :size="11" style="color:#0891b2;" />
                        <span>8GB RAM · 256GB SSD</span>
                      </div>
                    </div>

                    <!-- Giá (màu xanh dương) -->
                    <p class="fw-black mb-1" style="font-size:18px; color:#2563eb; line-height:1;">
                      {{ formatPrice(product.giaBan) }}
                    </p>

                    <!-- Nút Thêm vào giỏ (hồng 3D) -->
                    <button
                      class="btn w-100 d-flex align-items-center justify-content-center gap-1 fw-bold"
                      style="font-size:12px; padding:9px 12px; background:linear-gradient(180deg, #f472b6 0%, #ec4899 50%, #db2777 100%); color:#ffffff; border:none; border-radius:8px; box-shadow:0 3px 0 #9d174d; transition:all 0.15s;"
                      @click.stop="handleQuickAdd(product)"
                      @mouseenter="(e) => { e.currentTarget.style.transform='translateY(-2px)'; e.currentTarget.style.boxShadow='0 5px 0 #9d174d'; }"
                      @mouseleave="(e) => { e.currentTarget.style.transform=''; e.currentTarget.style.boxShadow='0 3px 0 #9d174d'; }"
                    >
                      <ShoppingCart :size="13" /> Thêm vào giỏ
                    </button>
                  </div>
                </div>
              </div>

              <!-- Empty state -->
              <div v-if="studentProducts.length === 0" class="col-12">
                <div class="text-center py-5">
                  <GraduationCap :size="48" style="color:#f9a8d4;" />
                  <p class="mt-3 mb-0" style="color:#be185d; font-size:14px;">
                    Không có sản phẩm nào cho sinh viên
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ─── Section: Gaming ─── -->
      <section v-if="gamingProducts.length > 0" class="mt-4" style="border-radius:16px; padding:20px;">
        <div class="d-flex align-items-center gap-3 mb-3">
          <div class="d-flex align-items-center gap-2">
            <Gamepad2 :size="28" style="color:#ec4899;" />
            <h2 class="fw-black mb-0" style="font-size:1.4rem; color:#be185d; text-transform:uppercase; letter-spacing:0.04em;">Gaming</h2>
          </div>
          <a href="#" class="fw-bold text-decoration-none d-flex align-items-center gap-1 ms-auto" style="font-size:13px; color:#ec4899;">Xem tất cả <ChevronRight :size="16" /></a>
        </div>
        <div class="row g-3">
          <!-- Banner trái -->
          <div class="col-12 col-md-3 col-lg-2" style="padding-left:0;">
            <div class="d-flex flex-row d-md-none gap-2">
              <a href="#" class="flex-grow-1 d-block rounded-3 overflow-hidden position-relative" style="min-height:100px; text-decoration:none;">
                <img src="https://images.unsplash.com/photo-1593640408182-31c70c8268f5?q=80&w=400" alt="Ưu đãi Gaming" class="w-100 h-100" style="object-fit:cover; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(190,24,93,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-2">
                  <div>
                    <h3 class="fw-black text-white mb-0" style="font-size:11px; line-height:1.2;">LAPTOP GAMING</h3>
                    <p class="mb-0 fw-bold" style="font-size:9px; color:#fce7f3;">Giảm đến 35%</p>
                  </div>
                </div>
              </a>
              <a href="#" class="flex-grow-1 d-block rounded-3 overflow-hidden position-relative" style="min-height:100px; text-decoration:none;">
                <img src="https://images.unsplash.com/photo-1603302576837-37561b2e2302?q=80&w=400" alt="RTX Gaming" class="w-100 h-100" style="object-fit:cover; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(37,99,235,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-2">
                  <div>
                    <h3 class="fw-black text-white mb-0" style="font-size:11px; line-height:1.2;">RTX 40 SERIES</h3>
                    <p class="mb-0 fw-bold" style="font-size:9px; color:#dbeafe;">Card đồ họa</p>
                  </div>
                </div>
              </a>
            </div>
            <div class="d-none d-md-flex flex-column gap-2 h-100">
              <a
                href="#" class="d-block rounded-3 overflow-hidden position-relative flex-grow-1" style="min-height:220px; text-decoration:none;"
                @mouseenter="(e) => e.currentTarget.style.transform='translateY(-3px)'"
                @mouseleave="(e) => e.currentTarget.style.transform=''"
              >
                <img src="https://images.unsplash.com/photo-1593640408182-31c70c8268f5?q=80&w=500" alt="Ưu đãi Gaming" class="w-100 h-100" style="object-fit:cover; object-position:center top; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(190,24,93,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-3">
                  <div>
                    <h3 class="fw-black text-white mb-1" style="font-size:14px; line-height:1.2; text-shadow:0 2px 6px rgba(0,0,0,0.4);">LAPTOP<br />GAMING</h3>
                    <p class="mb-0 fw-bold" style="font-size:11px; color:#fce7f3;">Giảm đến 35%</p>
                  </div>
                </div>
              </a>
              <a
                href="#" class="d-block rounded-3 overflow-hidden position-relative flex-grow-1" style="min-height:220px; text-decoration:none;"
                @mouseenter="(e) => e.currentTarget.style.transform='translateY(-3px)'"
                @mouseleave="(e) => e.currentTarget.style.transform=''"
              >
                <img src="https://images.unsplash.com/photo-1603302576837-37561b2e2302?q=80&w=500" alt="RTX Gaming" class="w-100 h-100" style="object-fit:cover; object-position:center top; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(37,99,235,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-3">
                  <div>
                    <h3 class="fw-black text-white mb-1" style="font-size:14px; line-height:1.2; text-shadow:0 2px 6px rgba(0,0,0,0.4);">RTX 40<br />SERIES</h3>
                    <p class="mb-0 fw-bold" style="font-size:11px; color:#dbeafe;">Card đồ họa khủng</p>
                  </div>
                </div>
              </a>
            </div>
          </div>
          <!-- Sản phẩm: hàng 1 -->
          <div class="col-12 col-md-9 col-lg-10">
            <div class="row g-3 mb-2">
              <div v-for="product in gamingProducts.slice(0, 4)" :key="product.sanPhamId" class="col-6 col-md-3">
                <div
                  class="h-100 d-flex flex-column bg-white rounded-3 overflow-hidden" style="border: 1px solid #e2e8f0; transition: all 0.2s; cursor:pointer;"
                  @click="openProduct(product)"
                  @mouseenter="(e) => { e.currentTarget.style.borderColor='#ec4899'; e.currentTarget.style.boxShadow='0 4px 14px rgba(236,72,153,0.2)'; e.currentTarget.style.transform='translateY(-3px)'; }"
                  @mouseleave="(e) => { e.currentTarget.style.borderColor='#e2e8f0'; e.currentTarget.style.boxShadow='none'; e.currentTarget.style.transform=''; }"
                >
                  <div class="position-relative" style="height:180px; background:linear-gradient(180deg, #f8fafc, #f1f5f9);">
                    <button class="position-absolute d-flex align-items-center justify-content-center" style="top:8px; right:8px; width:28px; height:28px; background:#fff; border:1px solid #e2e8f0; border-radius:50%; color:#ef4444; padding:0; z-index:2;" @click.stop="toggleWishlist(product)">
                      <Heart :size="13" :fill="isWishlisted(product.bienTheId) ? '#ef4444' : 'none'" />
                    </button>
                    <img :src="safeImage(product)" :alt="product.tenSanPham" style="width:100%; height:100%; object-fit:contain; padding:8px;" />
                  </div>
                  <div class="p-2 d-flex flex-column flex-grow-1">
                    <p class="fw-black mb-1" style="font-size:13px; line-height:1.3; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; line-clamp:2; -webkit-box-orient:vertical; color:#0f172a; min-height:34px;">{{ product.tenSanPham }}</p>
                    <div class="mb-2 p-2 rounded-2" style="background:#f1f5f9; font-size:11px; color:#475569; font-weight:600; line-height:1.5;">
                      <div class="d-flex align-items-center gap-1 mb-1"><Cpu :size="11" style="color:#dc2626;" /><span>{{ product.cpu || 'Intel Core i5' }}</span></div>
                      <div class="d-flex align-items-center gap-1"><HardDrive :size="11" style="color:#0891b2;" /><span>{{ product.ram || '8GB RAM' }} · {{ product.oCung || '256GB SSD' }}</span></div>
                    </div>
                    <p class="fw-black mb-1" style="font-size:18px; color:#dc2626; line-height:1;">{{ formatPrice(product.giaBan) }}</p>
                    <button
                      class="btn w-100 d-flex align-items-center justify-content-center gap-1 fw-bold" style="font-size:12px; padding:9px 12px; background:linear-gradient(180deg, #ef4444 0%, #dc2626 50%, #b91c1c 100%); color:#ffffff; border:none; border-radius:8px; box-shadow:0 3px 0 #991b1b; transition:all 0.15s;"
                      @click.stop="handleQuickAdd(product)"
                      @mouseenter="(e) => { e.currentTarget.style.transform='translateY(-2px)'; e.currentTarget.style.boxShadow='0 5px 0 #991b1b'; }"
                      @mouseleave="(e) => { e.currentTarget.style.transform=''; e.currentTarget.style.boxShadow='0 3px 0 #991b1b'; }"
                    >
                      <ShoppingCart :size="13" /> Thêm vào giỏ
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <!-- Hàng 2 -->
            <div v-if="gamingProducts.length > 4" class="row g-3">
              <div v-for="product in gamingProducts.slice(4, 8)" :key="product.sanPhamId" class="col-6 col-md-3">
                <div
                  class="h-100 d-flex flex-column bg-white rounded-3 overflow-hidden" style="border: 1px solid #e2e8f0; transition: all 0.2s; cursor:pointer;"
                  @click="openProduct(product)"
                  @mouseenter="(e) => { e.currentTarget.style.borderColor='#ec4899'; e.currentTarget.style.boxShadow='0 4px 14px rgba(236,72,153,0.2)'; e.currentTarget.style.transform='translateY(-3px)'; }"
                  @mouseleave="(e) => { e.currentTarget.style.borderColor='#e2e8f0'; e.currentTarget.style.boxShadow='none'; e.currentTarget.style.transform=''; }"
                >
                  <div class="position-relative" style="height:180px; background:linear-gradient(180deg, #f8fafc, #f1f5f9);">
                    <button class="position-absolute d-flex align-items-center justify-content-center" style="top:8px; right:8px; width:28px; height:28px; background:#fff; border:1px solid #e2e8f0; border-radius:50%; color:#ef4444; padding:0; z-index:2;" @click.stop="toggleWishlist(product)">
                      <Heart :size="13" :fill="isWishlisted(product.bienTheId) ? '#ef4444' : 'none'" />
                    </button>
                    <img :src="safeImage(product)" :alt="product.tenSanPham" style="width:100%; height:100%; object-fit:contain; padding:8px;" />
                  </div>
                  <div class="p-2 d-flex flex-column flex-grow-1">
                    <p class="fw-black mb-1" style="font-size:13px; line-height:1.3; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; line-clamp:2; -webkit-box-orient:vertical; color:#0f172a; min-height:34px;">{{ product.tenSanPham }}</p>
                    <div class="mb-2 p-2 rounded-2" style="background:#f1f5f9; font-size:11px; color:#475569; font-weight:600; line-height:1.5;">
                      <div class="d-flex align-items-center gap-1 mb-1"><Cpu :size="11" style="color:#dc2626;" /><span>{{ product.cpu || 'Intel Core i5' }}</span></div>
                      <div class="d-flex align-items-center gap-1"><HardDrive :size="11" style="color:#0891b2;" /><span>{{ product.ram || '8GB RAM' }} · {{ product.oCung || '256GB SSD' }}</span></div>
                    </div>
                    <p class="fw-black mb-1" style="font-size:18px; color:#dc2626; line-height:1;">{{ formatPrice(product.giaBan) }}</p>
                    <button
                      class="btn w-100 d-flex align-items-center justify-content-center gap-1 fw-bold" style="font-size:12px; padding:9px 12px; background:linear-gradient(180deg, #ef4444 0%, #dc2626 50%, #b91c1c 100%); color:#ffffff; border:none; border-radius:8px; box-shadow:0 3px 0 #991b1b; transition:all 0.15s;"
                      @click.stop="handleQuickAdd(product)"
                      @mouseenter="(e) => { e.currentTarget.style.transform='translateY(-2px)'; e.currentTarget.style.boxShadow='0 5px 0 #991b1b'; }"
                      @mouseleave="(e) => { e.currentTarget.style.transform=''; e.currentTarget.style.boxShadow='0 3px 0 #991b1b'; }"
                    >
                      <ShoppingCart :size="13" /> Thêm vào giỏ
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ─── Section: Đồ họa ─── -->
      <section v-if="graphicsProducts.length > 0" class="mt-4" style="border-radius:16px; padding:20px;">
        <div class="d-flex align-items-center gap-3 mb-3">
          <div class="d-flex align-items-center gap-2">
            <Zap :size="28" style="color:#7c3aed;" />
            <h2 class="fw-black mb-0" style="font-size:1.4rem; color:#7c3aed; text-transform:uppercase; letter-spacing:0.04em;">Đồ họa - Kỹ thuật</h2>
          </div>
          <a href="#" class="fw-bold text-decoration-none d-flex align-items-center gap-1 ms-auto" style="font-size:13px; color:#7c3aed;">Xem tất cả <ChevronRight :size="16" /></a>
        </div>
        <div class="row g-3">
          <!-- Banner trái -->
          <div class="col-12 col-md-3 col-lg-2 d-none d-md-block" style="padding-left:0;">
            <div class="d-flex flex-column gap-2 h-100">
              <a
                href="#" class="d-block rounded-3 overflow-hidden position-relative flex-grow-1" style="min-height:220px; text-decoration:none;"
                @mouseenter="(e) => e.currentTarget.style.transform='translateY(-3px)'"
                @mouseleave="(e) => e.currentTarget.style.transform=''"
              >
                <img src="https://images.unsplash.com/photo-1626785774573-4b799315345d?q=80&w=500" alt="Đồ họa chuyên nghiệp" class="w-100 h-100" style="object-fit:cover; object-position:center top; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(190,24,93,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-3">
                  <div>
                    <h3 class="fw-black text-white mb-1" style="font-size:14px; line-height:1.2; text-shadow:0 2px 6px rgba(0,0,0,0.4);">ĐỒ HỌA<br />CHUYÊN NGHIỆP</h3>
                    <p class="mb-0 fw-bold" style="font-size:11px; color:#fce7f3;">RAM 32GB · RTX</p>
                  </div>
                </div>
              </a>
              <a
                href="#" class="d-block rounded-3 overflow-hidden position-relative flex-grow-1" style="min-height:220px; text-decoration:none;"
                @mouseenter="(e) => e.currentTarget.style.transform='translateY(-3px)'"
                @mouseleave="(e) => e.currentTarget.style.transform=''"
              >
                <img src="https://images.unsplash.com/photo-1587831990711-23ca6441447b?q=80&w=500" alt="Kỹ thuật AI" class="w-100 h-100" style="object-fit:cover; object-position:center top; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(37,99,235,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-3">
                  <div>
                    <h3 class="fw-black text-white mb-1" style="font-size:14px; line-height:1.2; text-shadow:0 2px 6px rgba(0,0,0,0.4);">KỸ THUẬT<br />AI MACHINE</h3>
                    <p class="mb-0 fw-bold" style="font-size:11px; color:#dbeafe;">NVIDIA RTX · AI</p>
                  </div>
                </div>
              </a>
            </div>
          </div>
          <!-- Hàng 1 -->
          <div class="col-12 col-md-9 col-lg-10">
            <div class="row g-3 mb-2">
              <div v-for="product in graphicsProducts.slice(0, 4)" :key="product.sanPhamId" class="col-6 col-md-3">
                <div
                  class="h-100 d-flex flex-column bg-white rounded-3 overflow-hidden" style="border: 1px solid #e2e8f0; transition: all 0.2s; cursor:pointer;"
                  @click="openProduct(product)"
                  @mouseenter="(e) => { e.currentTarget.style.borderColor='#7c3aed'; e.currentTarget.style.boxShadow='0 4px 14px rgba(124,58,237,0.2)'; e.currentTarget.style.transform='translateY(-3px)'; }"
                  @mouseleave="(e) => { e.currentTarget.style.borderColor='#e2e8f0'; e.currentTarget.style.boxShadow='none'; e.currentTarget.style.transform=''; }"
                >
                  <div class="position-relative" style="height:180px; background:linear-gradient(180deg, #f8fafc, #f1f5f9);">
                    <button class="position-absolute d-flex align-items-center justify-content-center" style="top:8px; right:8px; width:28px; height:28px; background:#fff; border:1px solid #e2e8f0; border-radius:50%; color:#ef4444; padding:0; z-index:2;" @click.stop="toggleWishlist(product)">
                      <Heart :size="13" :fill="isWishlisted(product.bienTheId) ? '#ef4444' : 'none'" />
                    </button>
                    <img :src="safeImage(product)" :alt="product.tenSanPham" style="width:100%; height:100%; object-fit:contain; padding:8px;" />
                  </div>
                  <div class="p-2 d-flex flex-column flex-grow-1">
                    <p class="fw-black mb-1" style="font-size:13px; line-height:1.3; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; line-clamp:2; -webkit-box-orient:vertical; color:#0f172a; min-height:34px;">{{ product.tenSanPham }}</p>
                    <div class="mb-2 p-2 rounded-2" style="background:#f1f5f9; font-size:11px; color:#475569; font-weight:600; line-height:1.5;">
                      <div class="d-flex align-items-center gap-1 mb-1"><Cpu :size="11" style="color:#7c3aed;" /><span>{{ product.cpu || 'Intel Core i5' }}</span></div>
                      <div class="d-flex align-items-center gap-1"><HardDrive :size="11" style="color:#0891b2;" /><span>{{ product.ram || '8GB RAM' }} · {{ product.oCung || '256GB SSD' }}</span></div>
                    </div>
                    <p class="fw-black mb-1" style="font-size:18px; color:#7c3aed; line-height:1;">{{ formatPrice(product.giaBan) }}</p>
                    <button
                      class="btn w-100 d-flex align-items-center justify-content-center gap-1 fw-bold" style="font-size:12px; padding:9px 12px; background:linear-gradient(180deg, #8b5cf6 0%, #7c3aed 50%, #6d28d9 100%); color:#ffffff; border:none; border-radius:8px; box-shadow:0 3px 0 #5b21b6; transition:all 0.15s;"
                      @click.stop="handleQuickAdd(product)"
                      @mouseenter="(e) => { e.currentTarget.style.transform='translateY(-2px)'; e.currentTarget.style.boxShadow='0 5px 0 #5b21b6'; }"
                      @mouseleave="(e) => { e.currentTarget.style.transform=''; e.currentTarget.style.boxShadow='0 3px 0 #5b21b6'; }"
                    >
                      <ShoppingCart :size="13" /> Thêm vào giỏ
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <!-- Hàng 2 -->
            <div v-if="graphicsProducts.length > 4" class="row g-3">
              <div v-for="product in graphicsProducts.slice(4, 8)" :key="product.sanPhamId" class="col-6 col-md-3">
                <div
                  class="h-100 d-flex flex-column bg-white rounded-3 overflow-hidden" style="border: 1px solid #e2e8f0; transition: all 0.2s; cursor:pointer;"
                  @click="openProduct(product)"
                  @mouseenter="(e) => { e.currentTarget.style.borderColor='#7c3aed'; e.currentTarget.style.boxShadow='0 4px 14px rgba(124,58,237,0.2)'; e.currentTarget.style.transform='translateY(-3px)'; }"
                  @mouseleave="(e) => { e.currentTarget.style.borderColor='#e2e8f0'; e.currentTarget.style.boxShadow='none'; e.currentTarget.style.transform=''; }"
                >
                  <div class="position-relative" style="height:180px; background:linear-gradient(180deg, #f8fafc, #f1f5f9);">
                    <button class="position-absolute d-flex align-items-center justify-content-center" style="top:8px; right:8px; width:28px; height:28px; background:#fff; border:1px solid #e2e8f0; border-radius:50%; color:#ef4444; padding:0; z-index:2;" @click.stop="toggleWishlist(product)">
                      <Heart :size="13" :fill="isWishlisted(product.bienTheId) ? '#ef4444' : 'none'" />
                    </button>
                    <img :src="safeImage(product)" :alt="product.tenSanPham" style="width:100%; height:100%; object-fit:contain; padding:8px;" />
                  </div>
                  <div class="p-2 d-flex flex-column flex-grow-1">
                    <p class="fw-black mb-1" style="font-size:13px; line-height:1.3; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; line-clamp:2; -webkit-box-orient:vertical; color:#0f172a; min-height:34px;">{{ product.tenSanPham }}</p>
                    <div class="mb-2 p-2 rounded-2" style="background:#f1f5f9; font-size:11px; color:#475569; font-weight:600; line-height:1.5;">
                      <div class="d-flex align-items-center gap-1 mb-1"><Cpu :size="11" style="color:#7c3aed;" /><span>{{ product.cpu || 'Intel Core i5' }}</span></div>
                      <div class="d-flex align-items-center gap-1"><HardDrive :size="11" style="color:#0891b2;" /><span>{{ product.ram || '8GB RAM' }} · {{ product.oCung || '256GB SSD' }}</span></div>
                    </div>
                    <p class="fw-black mb-1" style="font-size:18px; color:#7c3aed; line-height:1;">{{ formatPrice(product.giaBan) }}</p>
                    <button
                      class="btn w-100 d-flex align-items-center justify-content-center gap-1 fw-bold" style="font-size:12px; padding:9px 12px; background:linear-gradient(180deg, #8b5cf6 0%, #7c3aed 50%, #6d28d9 100%); color:#ffffff; border:none; border-radius:8px; box-shadow:0 3px 0 #5b21b6; transition:all 0.15s;"
                      @click.stop="handleQuickAdd(product)"
                      @mouseenter="(e) => { e.currentTarget.style.transform='translateY(-2px)'; e.currentTarget.style.boxShadow='0 5px 0 #5b21b6'; }"
                      @mouseleave="(e) => { e.currentTarget.style.transform=''; e.currentTarget.style.boxShadow='0 3px 0 #5b21b6'; }"
                    >
                      <ShoppingCart :size="13" /> Thêm vào giỏ
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ─── Section: MacBook ─── -->
      <section v-if="macbookProducts.length > 0" class="mt-4" style="border-radius:16px; padding:20px;">
        <div class="d-flex align-items-center gap-3 mb-3">
          <div class="d-flex align-items-center gap-2">
            <Apple :size="28" style="color:#ec4899;" />
            <h2 class="fw-black mb-0" style="font-size:1.4rem; color:#be185d; text-transform:uppercase; letter-spacing:0.04em;">MacBook / Apple</h2>
          </div>
          <a href="#" class="fw-bold text-decoration-none d-flex align-items-center gap-1 ms-auto" style="font-size:13px; color:#ec4899;">Xem tất cả <ChevronRight :size="16" /></a>
        </div>
        <div class="row g-3">
          <!-- Banner trái -->
          <div class="col-12 col-md-3 col-lg-2" style="padding-left:0;">
            <div class="d-flex flex-row d-md-none gap-2">
              <a href="#" class="flex-grow-1 d-block rounded-3 overflow-hidden position-relative" style="min-height:100px; text-decoration:none;">
                <img src="https://images.unsplash.com/photo-1517336714731-489689fd1ca8?q=80&w=400" alt="MacBook Pro" class="w-100 h-100" style="object-fit:cover; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(190,24,93,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-2">
                  <div>
                    <h3 class="fw-black text-white mb-0" style="font-size:11px; line-height:1.2;">MACBOOK PRO M4</h3>
                    <p class="mb-0 fw-bold" style="font-size:9px; color:#fce7f3;">Chip M4 mới</p>
                  </div>
                </div>
              </a>
              <a href="#" class="flex-grow-1 d-block rounded-3 overflow-hidden position-relative" style="min-height:100px; text-decoration:none;">
                <img src="https://images.unsplash.com/photo-1611186871348-b1ce696e52c9?q=80&w=400" alt="iMac" class="w-100 h-100" style="object-fit:cover; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(37,99,235,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-2">
                  <div>
                    <h3 class="fw-black text-white mb-0" style="font-size:11px; line-height:1.2;">IMAC M4 2024</h3>
                    <p class="mb-0 fw-bold" style="font-size:9px; color:#dbeafe;">Màn 4.5K Retina</p>
                  </div>
                </div>
              </a>
            </div>
            <div class="d-none d-md-flex flex-column gap-2 h-100">
              <a
                href="#" class="d-block rounded-3 overflow-hidden position-relative flex-grow-1" style="min-height:220px; text-decoration:none;"
                @mouseenter="(e) => e.currentTarget.style.transform='translateY(-3px)'"
                @mouseleave="(e) => e.currentTarget.style.transform=''"
              >
                <img src="https://images.unsplash.com/photo-1517336714731-489689fd1ca8?q=80&w=500" alt="MacBook Pro" class="w-100 h-100" style="object-fit:cover; object-position:center top; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(190,24,93,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-3">
                  <div>
                    <h3 class="fw-black text-white mb-1" style="font-size:14px; line-height:1.2; text-shadow:0 2px 6px rgba(0,0,0,0.4);">MACBOOK<br />PRO M4</h3>
                    <p class="mb-0 fw-bold" style="font-size:11px; color:#fce7f3;">Chip M4 thế hệ mới</p>
                  </div>
                </div>
              </a>
              <a
                href="#" class="d-block rounded-3 overflow-hidden position-relative flex-grow-1" style="min-height:220px; text-decoration:none;"
                @mouseenter="(e) => e.currentTarget.style.transform='translateY(-3px)'"
                @mouseleave="(e) => e.currentTarget.style.transform=''"
              >
                <img src="https://images.unsplash.com/photo-1611186871348-b1ce696e52c9?q=80&w=500" alt="iMac" class="w-100 h-100" style="object-fit:cover; object-position:center top; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(37,99,235,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-3">
                  <div>
                    <h3 class="fw-black text-white mb-1" style="font-size:14px; line-height:1.2; text-shadow:0 2px 6px rgba(0,0,0,0.4);">IMAC<br />M4 2024</h3>
                    <p class="mb-0 fw-bold" style="font-size:11px; color:#dbeafe;">Màn hình 4.5K Retina</p>
                  </div>
                </div>
              </a>
            </div>
          </div>
          <!-- Hàng 1 -->
          <div class="col-12 col-md-9 col-lg-10">
            <div class="row g-3 mb-2">
              <div v-for="product in macbookProducts.slice(0, 4)" :key="product.sanPhamId" class="col-6 col-md-3">
                <div
                  class="h-100 d-flex flex-column bg-white rounded-3 overflow-hidden" style="border: 1px solid #e2e8f0; transition: all 0.2s; cursor:pointer;"
                  @click="openProduct(product)"
                  @mouseenter="(e) => { e.currentTarget.style.borderColor='#64748b'; e.currentTarget.style.boxShadow='0 4px 14px rgba(100,116,139,0.2)'; e.currentTarget.style.transform='translateY(-3px)'; }"
                  @mouseleave="(e) => { e.currentTarget.style.borderColor='#e2e8f0'; e.currentTarget.style.boxShadow='none'; e.currentTarget.style.transform=''; }"
                >
                  <div class="position-relative" style="height:180px; background:linear-gradient(180deg, #f8fafc, #f1f5f9);">
                    <button class="position-absolute d-flex align-items-center justify-content-center" style="top:8px; right:8px; width:28px; height:28px; background:#fff; border:1px solid #e2e8f0; border-radius:50%; color:#ef4444; padding:0; z-index:2;" @click.stop="toggleWishlist(product)">
                      <Heart :size="13" :fill="isWishlisted(product.bienTheId) ? '#ef4444' : 'none'" />
                    </button>
                    <img :src="safeImage(product)" :alt="product.tenSanPham" style="width:100%; height:100%; object-fit:contain; padding:8px;" />
                  </div>
                  <div class="p-2 d-flex flex-column flex-grow-1">
                    <p class="fw-black mb-1" style="font-size:13px; line-height:1.3; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; line-clamp:2; -webkit-box-orient:vertical; color:#0f172a; min-height:34px;">{{ product.tenSanPham }}</p>
                    <div class="mb-2 p-2 rounded-2" style="background:#f1f5f9; font-size:11px; color:#475569; font-weight:600; line-height:1.5;">
                      <div class="d-flex align-items-center gap-1 mb-1"><Cpu :size="11" style="color:#64748b;" /><span>{{ product.cpu || 'Apple M4' }}</span></div>
                      <div class="d-flex align-items-center gap-1"><HardDrive :size="11" style="color:#0891b2;" /><span>{{ product.ram || '16GB RAM' }} · {{ product.oCung || '256GB SSD' }}</span></div>
                    </div>
                    <p class="fw-black mb-1" style="font-size:18px; color:#334155; line-height:1;">{{ formatPrice(product.giaBan) }}</p>
                    <button
                      class="btn w-100 d-flex align-items-center justify-content-center gap-1 fw-bold" style="font-size:12px; padding:9px 12px; background:linear-gradient(180deg, #94a3b8 0%, #64748b 50%, #475569 100%); color:#ffffff; border:none; border-radius:8px; box-shadow:0 3px 0 #334155; transition:all 0.15s;"
                      @click.stop="handleQuickAdd(product)"
                      @mouseenter="(e) => { e.currentTarget.style.transform='translateY(-2px)'; e.currentTarget.style.boxShadow='0 5px 0 #334155'; }"
                      @mouseleave="(e) => { e.currentTarget.style.transform=''; e.currentTarget.style.boxShadow='0 3px 0 #334155'; }"
                    >
                      <ShoppingCart :size="13" /> Thêm vào giỏ
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <!-- Hàng 2 -->
            <div v-if="macbookProducts.length > 4" class="row g-3">
              <div v-for="product in macbookProducts.slice(4, 8)" :key="product.sanPhamId" class="col-6 col-md-3">
                <div
                  class="h-100 d-flex flex-column bg-white rounded-3 overflow-hidden" style="border: 1px solid #e2e8f0; transition: all 0.2s; cursor:pointer;"
                  @click="openProduct(product)"
                  @mouseenter="(e) => { e.currentTarget.style.borderColor='#64748b'; e.currentTarget.style.boxShadow='0 4px 14px rgba(100,116,139,0.2)'; e.currentTarget.style.transform='translateY(-3px)'; }"
                  @mouseleave="(e) => { e.currentTarget.style.borderColor='#e2e8f0'; e.currentTarget.style.boxShadow='none'; e.currentTarget.style.transform=''; }"
                >
                  <div class="position-relative" style="height:180px; background:linear-gradient(180deg, #f8fafc, #f1f5f9);">
                    <button class="position-absolute d-flex align-items-center justify-content-center" style="top:8px; right:8px; width:28px; height:28px; background:#fff; border:1px solid #e2e8f0; border-radius:50%; color:#ef4444; padding:0; z-index:2;" @click.stop="toggleWishlist(product)">
                      <Heart :size="13" :fill="isWishlisted(product.bienTheId) ? '#ef4444' : 'none'" />
                    </button>
                    <img :src="safeImage(product)" :alt="product.tenSanPham" style="width:100%; height:100%; object-fit:contain; padding:8px;" />
                  </div>
                  <div class="p-2 d-flex flex-column flex-grow-1">
                    <p class="fw-black mb-1" style="font-size:13px; line-height:1.3; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; line-clamp:2; -webkit-box-orient:vertical; color:#0f172a; min-height:34px;">{{ product.tenSanPham }}</p>
                    <div class="mb-2 p-2 rounded-2" style="background:#f1f5f9; font-size:11px; color:#475569; font-weight:600; line-height:1.5;">
                      <div class="d-flex align-items-center gap-1 mb-1"><Cpu :size="11" style="color:#64748b;" /><span>{{ product.cpu || 'Apple M4' }}</span></div>
                      <div class="d-flex align-items-center gap-1"><HardDrive :size="11" style="color:#0891b2;" /><span>{{ product.ram || '16GB RAM' }} · {{ product.oCung || '256GB SSD' }}</span></div>
                    </div>
                    <p class="fw-black mb-1" style="font-size:18px; color:#334155; line-height:1;">{{ formatPrice(product.giaBan) }}</p>
                    <button
                      class="btn w-100 d-flex align-items-center justify-content-center gap-1 fw-bold" style="font-size:12px; padding:9px 12px; background:linear-gradient(180deg, #94a3b8 0%, #64748b 50%, #475569 100%); color:#ffffff; border:none; border-radius:8px; box-shadow:0 3px 0 #334155; transition:all 0.15s;"
                      @click.stop="handleQuickAdd(product)"
                      @mouseenter="(e) => { e.currentTarget.style.transform='translateY(-2px)'; e.currentTarget.style.boxShadow='0 5px 0 #334155'; }"
                      @mouseleave="(e) => { e.currentTarget.style.transform=''; e.currentTarget.style.boxShadow='0 3px 0 #334155'; }"
                    >
                      <ShoppingCart :size="13" /> Thêm vào giỏ
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ─── Section: Cao cấp - Mỏng nhẹ ─── -->
      <section v-if="premiumProducts.length > 0" class="mt-4" style="border-radius:16px; padding:20px;">
        <div class="d-flex align-items-center gap-3 mb-3">
          <div class="d-flex align-items-center gap-2">
            <Sparkles :size="28" style="color:#ec4899;" />
            <h2 class="fw-black mb-0" style="font-size:1.4rem; color:#be185d; text-transform:uppercase; letter-spacing:0.04em;">Cao cấp - Mỏng nhẹ</h2>
          </div>
          <a href="#" class="fw-bold text-decoration-none d-flex align-items-center gap-1 ms-auto" style="font-size:13px; color:#ec4899;">Xem tất cả <ChevronRight :size="16" /></a>
        </div>
        <div class="row g-3">
          <!-- Banner trái -->
          <div class="col-12 col-md-3 col-lg-2" style="padding-left:0;">
            <div class="d-flex flex-row d-md-none gap-2">
              <a href="#" class="flex-grow-1 d-block rounded-3 overflow-hidden position-relative" style="min-height:100px; text-decoration:none;">
                <img src="https://images.unsplash.com/photo-1496181133206-80ce9b88a853?q=80&w=400" alt="Cao cấp" class="w-100 h-100" style="object-fit:cover; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(190,24,93,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-2">
                  <div>
                    <h3 class="fw-black text-white mb-0" style="font-size:11px; line-height:1.2;">MỎNG NHẸ CAO CẤP</h3>
                    <p class="mb-0 fw-bold" style="font-size:9px; color:#fce7f3;">ZenBook · Gram</p>
                  </div>
                </div>
              </a>
              <a href="#" class="flex-grow-1 d-block rounded-3 overflow-hidden position-relative" style="min-height:100px; text-decoration:none;">
                <img src="https://images.unsplash.com/photo-1525547719571-a2d4ac8945e2?q=80&w=400" alt="Doanh nhân" class="w-100 h-100" style="object-fit:cover; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(37,99,235,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-2">
                  <div>
                    <h3 class="fw-black text-white mb-0" style="font-size:11px; line-height:1.2;">DOANH NHÂN</h3>
                    <p class="mb-0 fw-bold" style="font-size:9px; color:#dbeafe;">XPS · ThinkPad</p>
                  </div>
                </div>
              </a>
            </div>
            <div class="d-none d-md-flex flex-column gap-2 h-100">
              <a
                href="#" class="d-block rounded-3 overflow-hidden position-relative flex-grow-1" style="min-height:220px; text-decoration:none;"
                @mouseenter="(e) => e.currentTarget.style.transform='translateY(-3px)'"
                @mouseleave="(e) => e.currentTarget.style.transform=''"
              >
                <img src="https://images.unsplash.com/photo-1496181133206-80ce9b88a853?q=80&w=500" alt="Cao cấp" class="w-100 h-100" style="object-fit:cover; object-position:center top; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(190,24,93,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-3">
                  <div>
                    <h3 class="fw-black text-white mb-1" style="font-size:14px; line-height:1.2; text-shadow:0 2px 6px rgba(0,0,0,0.4);">MỎNG NHẸ<br />CAO CẤP</h3>
                    <p class="mb-0 fw-bold" style="font-size:11px; color:#fce7f3;">ZenBook · Gram</p>
                  </div>
                </div>
              </a>
              <a
                href="#" class="d-block rounded-3 overflow-hidden position-relative flex-grow-1" style="min-height:220px; text-decoration:none;"
                @mouseenter="(e) => e.currentTarget.style.transform='translateY(-3px)'"
                @mouseleave="(e) => e.currentTarget.style.transform=''"
              >
                <img src="https://images.unsplash.com/photo-1525547719571-a2d4ac8945e2?q=80&w=500" alt="Doanh nhân" class="w-100 h-100" style="object-fit:cover; object-position:center top; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(37,99,235,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-3">
                  <div>
                    <h3 class="fw-black text-white mb-1" style="font-size:14px; line-height:1.2; text-shadow:0 2px 6px rgba(0,0,0,0.4);">DOANH NHÂN<br />SIÊU SANG</h3>
                    <p class="mb-0 fw-bold" style="font-size:11px; color:#dbeafe;">XPS · ThinkPad</p>
                  </div>
                </div>
              </a>
            </div>
          </div>
          <!-- Hàng 1 -->
          <div class="col-12 col-md-9 col-lg-10">
            <div class="row g-3 mb-2">
              <div v-for="product in premiumProducts.slice(0, 4)" :key="product.sanPhamId" class="col-6 col-md-3">
                <div
                  class="h-100 d-flex flex-column bg-white rounded-3 overflow-hidden" style="border: 1px solid #e2e8f0; transition: all 0.2s; cursor:pointer;"
                  @click="openProduct(product)"
                  @mouseenter="(e) => { e.currentTarget.style.borderColor='#d97706'; e.currentTarget.style.boxShadow='0 4px 14px rgba(217,119,6,0.2)'; e.currentTarget.style.transform='translateY(-3px)'; }"
                  @mouseleave="(e) => { e.currentTarget.style.borderColor='#e2e8f0'; e.currentTarget.style.boxShadow='none'; e.currentTarget.style.transform=''; }"
                >
                  <div class="position-relative" style="height:180px; background:linear-gradient(180deg, #f8fafc, #f1f5f9);">
                    <button class="position-absolute d-flex align-items-center justify-content-center" style="top:8px; right:8px; width:28px; height:28px; background:#fff; border:1px solid #e2e8f0; border-radius:50%; color:#ef4444; padding:0; z-index:2;" @click.stop="toggleWishlist(product)">
                      <Heart :size="13" :fill="isWishlisted(product.bienTheId) ? '#ef4444' : 'none'" />
                    </button>
                    <img :src="safeImage(product)" :alt="product.tenSanPham" style="width:100%; height:100%; object-fit:contain; padding:8px;" />
                  </div>
                  <div class="p-2 d-flex flex-column flex-grow-1">
                    <p class="fw-black mb-1" style="font-size:13px; line-height:1.3; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; line-clamp:2; -webkit-box-orient:vertical; color:#0f172a; min-height:34px;">{{ product.tenSanPham }}</p>
                    <div class="mb-2 p-2 rounded-2" style="background:#f1f5f9; font-size:11px; color:#475569; font-weight:600; line-height:1.5;">
                      <div class="d-flex align-items-center gap-1 mb-1"><Cpu :size="11" style="color:#d97706;" /><span>{{ product.cpu || 'Intel Core i5' }}</span></div>
                      <div class="d-flex align-items-center gap-1"><HardDrive :size="11" style="color:#0891b2;" /><span>{{ product.ram || '8GB RAM' }} · {{ product.oCung || '256GB SSD' }}</span></div>
                    </div>
                    <p class="fw-black mb-1" style="font-size:18px; color:#d97706; line-height:1;">{{ formatPrice(product.giaBan) }}</p>
                    <button
                      class="btn w-100 d-flex align-items-center justify-content-center gap-1 fw-bold" style="font-size:12px; padding:9px 12px; background:linear-gradient(180deg, #fbbf24 0%, #f59e0b 50%, #d97706 100%); color:#ffffff; border:none; border-radius:8px; box-shadow:0 3px 0 #92400e; transition:all 0.15s;"
                      @click.stop="handleQuickAdd(product)"
                      @mouseenter="(e) => { e.currentTarget.style.transform='translateY(-2px)'; e.currentTarget.style.boxShadow='0 5px 0 #92400e'; }"
                      @mouseleave="(e) => { e.currentTarget.style.transform=''; e.currentTarget.style.boxShadow='0 3px 0 #92400e'; }"
                    >
                      <ShoppingCart :size="13" /> Thêm vào giỏ
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <!-- Hàng 2 -->
            <div v-if="premiumProducts.length > 4" class="row g-3">
              <div v-for="product in premiumProducts.slice(4, 8)" :key="product.sanPhamId" class="col-6 col-md-3">
                <div
                  class="h-100 d-flex flex-column bg-white rounded-3 overflow-hidden" style="border: 1px solid #e2e8f0; transition: all 0.2s; cursor:pointer;"
                  @click="openProduct(product)"
                  @mouseenter="(e) => { e.currentTarget.style.borderColor='#d97706'; e.currentTarget.style.boxShadow='0 4px 14px rgba(217,119,6,0.2)'; e.currentTarget.style.transform='translateY(-3px)'; }"
                  @mouseleave="(e) => { e.currentTarget.style.borderColor='#e2e8f0'; e.currentTarget.style.boxShadow='none'; e.currentTarget.style.transform=''; }"
                >
                  <div class="position-relative" style="height:180px; background:linear-gradient(180deg, #f8fafc, #f1f5f9);">
                    <button class="position-absolute d-flex align-items-center justify-content-center" style="top:8px; right:8px; width:28px; height:28px; background:#fff; border:1px solid #e2e8f0; border-radius:50%; color:#ef4444; padding:0; z-index:2;" @click.stop="toggleWishlist(product)">
                      <Heart :size="13" :fill="isWishlisted(product.bienTheId) ? '#ef4444' : 'none'" />
                    </button>
                    <img :src="safeImage(product)" :alt="product.tenSanPham" style="width:100%; height:100%; object-fit:contain; padding:8px;" />
                  </div>
                  <div class="p-2 d-flex flex-column flex-grow-1">
                    <p class="fw-black mb-1" style="font-size:13px; line-height:1.3; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; line-clamp:2; -webkit-box-orient:vertical; color:#0f172a; min-height:34px;">{{ product.tenSanPham }}</p>
                    <div class="mb-2 p-2 rounded-2" style="background:#f1f5f9; font-size:11px; color:#475569; font-weight:600; line-height:1.5;">
                      <div class="d-flex align-items-center gap-1 mb-1"><Cpu :size="11" style="color:#d97706;" /><span>{{ product.cpu || 'Intel Core i5' }}</span></div>
                      <div class="d-flex align-items-center gap-1"><HardDrive :size="11" style="color:#0891b2;" /><span>{{ product.ram || '8GB RAM' }} · {{ product.oCung || '256GB SSD' }}</span></div>
                    </div>
                    <p class="fw-black mb-1" style="font-size:18px; color:#d97706; line-height:1;">{{ formatPrice(product.giaBan) }}</p>
                    <button
                      class="btn w-100 d-flex align-items-center justify-content-center gap-1 fw-bold" style="font-size:12px; padding:9px 12px; background:linear-gradient(180deg, #fbbf24 0%, #f59e0b 50%, #d97706 100%); color:#ffffff; border:none; border-radius:8px; box-shadow:0 3px 0 #92400e; transition:all 0.15s;"
                      @click.stop="handleQuickAdd(product)"
                      @mouseenter="(e) => { e.currentTarget.style.transform='translateY(-2px)'; e.currentTarget.style.boxShadow='0 5px 0 #92400e'; }"
                      @mouseleave="(e) => { e.currentTarget.style.transform=''; e.currentTarget.style.boxShadow='0 3px 0 #92400e'; }"
                    >
                      <ShoppingCart :size="13" /> Thêm vào giỏ
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ─── Section: Theo từng hãng ─── -->
      <section v-if="asusProducts.length > 0" class="mt-4" style="border-radius:16px; padding:20px;">
        <div class="d-flex align-items-center gap-3 mb-3">
          <div class="d-flex align-items-center gap-2">
            <Laptop :size="28" style="color:#ec4899;" />
            <h2 class="fw-black mb-0" style="font-size:1.4rem; color:#be185d; text-transform:uppercase; letter-spacing:0.04em;">ASUS</h2>
          </div>
          <a href="#" class="fw-bold text-decoration-none d-flex align-items-center gap-1 ms-auto" style="font-size:13px; color:#ec4899;">Xem tất cả <ChevronRight :size="16" /></a>
        </div>
        <div class="row g-3">
          <!-- Banner trái -->
          <div class="col-12 col-md-3 col-lg-2" style="padding-left:0;">
            <div class="d-flex flex-row d-md-none gap-2">
              <a href="#" class="flex-grow-1 d-block rounded-3 overflow-hidden position-relative" style="min-height:100px; text-decoration:none;">
                <img src="https://images.unsplash.com/photo-1517336714731-489689fd1ca8?q=80&w=400" alt="ASUS" class="w-100 h-100" style="object-fit:cover; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(190,24,93,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-2">
                  <div>
                    <h3 class="fw-black text-white mb-0" style="font-size:11px; line-height:1.2;">LAPTOP ASUS</h3>
                    <p class="mb-0 fw-bold" style="font-size:9px; color:#fce7f3;">Giảm đến 25%</p>
                  </div>
                </div>
              </a>
              <a href="#" class="flex-grow-1 d-block rounded-3 overflow-hidden position-relative" style="min-height:100px; text-decoration:none;">
                <img src="https://images.unsplash.com/photo-1593640408182-31c70c8268f5?q=80&w=400" alt="ASUS ROG" class="w-100 h-100" style="object-fit:cover; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(37,99,235,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-2">
                  <div>
                    <h3 class="fw-black text-white mb-0" style="font-size:11px; line-height:1.2;">ROG GAMING</h3>
                    <p class="mb-0 fw-bold" style="font-size:9px; color:#dbeafe;">Chơi game cực đỉnh</p>
                  </div>
                </div>
              </a>
            </div>
            <div class="d-none d-md-flex flex-column gap-2 h-100">
              <a
                href="#" class="d-block rounded-3 overflow-hidden position-relative flex-grow-1" style="min-height:220px; text-decoration:none;"
                @mouseenter="(e) => e.currentTarget.style.transform='translateY(-3px)'"
                @mouseleave="(e) => e.currentTarget.style.transform=''"
              >
                <img src="https://images.unsplash.com/photo-1517336714731-489689fd1ca8?q=80&w=500" alt="ASUS" class="w-100 h-100" style="object-fit:cover; object-position:center top; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(190,24,93,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-3">
                  <div>
                    <h3 class="fw-black text-white mb-1" style="font-size:14px; line-height:1.2; text-shadow:0 2px 6px rgba(0,0,0,0.4);">LAPTOP<br />ASUS</h3>
                    <p class="mb-0 fw-bold" style="font-size:11px; color:#fce7f3;">Giảm đến 25%</p>
                  </div>
                </div>
              </a>
              <a
                href="#" class="d-block rounded-3 overflow-hidden position-relative flex-grow-1" style="min-height:220px; text-decoration:none;"
                @mouseenter="(e) => e.currentTarget.style.transform='translateY(-3px)'"
                @mouseleave="(e) => e.currentTarget.style.transform=''"
              >
                <img src="https://images.unsplash.com/photo-1593640408182-31c70c8268f5?q=80&w=500" alt="ASUS ROG" class="w-100 h-100" style="object-fit:cover; object-position:center top; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(37,99,235,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-3">
                  <div>
                    <h3 class="fw-black text-white mb-1" style="font-size:14px; line-height:1.2; text-shadow:0 2px 6px rgba(0,0,0,0.4);">ROG<br />GAMING</h3>
                    <p class="mb-0 fw-bold" style="font-size:11px; color:#dbeafe;">Chơi game cực đỉnh</p>
                  </div>
                </div>
              </a>
            </div>
          </div>
          <!-- Hàng 1 -->
          <div class="col-12 col-md-9 col-lg-10">
            <div class="row g-3 mb-2">
              <div v-for="product in asusProducts.slice(0, 4)" :key="product.sanPhamId" class="col-6 col-md-3">
                <div
                  class="h-100 d-flex flex-column bg-white rounded-3 overflow-hidden" style="border: 1px solid #e2e8f0; transition: all 0.2s; cursor:pointer;"
                  @click="openProduct(product)"
                  @mouseenter="(e) => { e.currentTarget.style.borderColor='#ec4899'; e.currentTarget.style.boxShadow='0 4px 14px rgba(236,72,153,0.2)'; e.currentTarget.style.transform='translateY(-3px)'; }"
                  @mouseleave="(e) => { e.currentTarget.style.borderColor='#e2e8f0'; e.currentTarget.style.boxShadow='none'; e.currentTarget.style.transform=''; }"
                >
                  <div class="position-relative" style="height:180px; background:linear-gradient(180deg, #f8fafc, #f1f5f9);">
                    <button class="position-absolute d-flex align-items-center justify-content-center" style="top:8px; right:8px; width:28px; height:28px; background:#fff; border:1px solid #e2e8f0; border-radius:50%; color:#ef4444; padding:0; z-index:2;" @click.stop="toggleWishlist(product)">
                      <Heart :size="13" :fill="isWishlisted(product.bienTheId) ? '#ef4444' : 'none'" />
                    </button>
                    <img :src="safeImage(product)" :alt="product.tenSanPham" style="width:100%; height:100%; object-fit:contain; padding:8px;" />
                  </div>
                  <div class="p-2 d-flex flex-column flex-grow-1">
                    <p class="fw-black mb-1" style="font-size:13px; line-height:1.3; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; line-clamp:2; -webkit-box-orient:vertical; color:#0f172a; min-height:34px;">{{ product.tenSanPham }}</p>
                    <div class="mb-2 p-2 rounded-2" style="background:#f1f5f9; font-size:11px; color:#475569; font-weight:600; line-height:1.5;">
                      <div class="d-flex align-items-center gap-1 mb-1"><Cpu :size="11" style="color:#ec4899;" /><span>{{ product.cpu || 'Intel Core i5' }}</span></div>
                      <div class="d-flex align-items-center gap-1"><HardDrive :size="11" style="color:#0891b2;" /><span>{{ product.ram || '8GB RAM' }} · {{ product.oCung || '256GB SSD' }}</span></div>
                    </div>
                    <p class="fw-black mb-1" style="font-size:18px; color:#be185d; line-height:1;">{{ formatPrice(product.giaBan) }}</p>
                    <button
                      class="btn w-100 d-flex align-items-center justify-content-center gap-1 fw-bold" style="font-size:12px; padding:9px 12px; background:linear-gradient(180deg, #f472b6 0%, #ec4899 50%, #db2777 100%); color:#ffffff; border:none; border-radius:8px; box-shadow:0 3px 0 #9d174d; transition:all 0.15s;"
                      @click.stop="handleQuickAdd(product)"
                      @mouseenter="(e) => { e.currentTarget.style.transform='translateY(-2px)'; e.currentTarget.style.boxShadow='0 5px 0 #9d174d'; }"
                      @mouseleave="(e) => { e.currentTarget.style.transform=''; e.currentTarget.style.boxShadow='0 3px 0 #9d174d'; }"
                    >
                      <ShoppingCart :size="13" /> Thêm vào giỏ
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <!-- Hàng 2 -->
            <div v-if="asusProducts.length > 4" class="row g-3">
              <div v-for="product in asusProducts.slice(4, 8)" :key="product.sanPhamId" class="col-6 col-md-3">
                <div
                  class="h-100 d-flex flex-column bg-white rounded-3 overflow-hidden" style="border: 1px solid #e2e8f0; transition: all 0.2s; cursor:pointer;"
                  @click="openProduct(product)"
                  @mouseenter="(e) => { e.currentTarget.style.borderColor='#ec4899'; e.currentTarget.style.boxShadow='0 4px 14px rgba(236,72,153,0.2)'; e.currentTarget.style.transform='translateY(-3px)'; }"
                  @mouseleave="(e) => { e.currentTarget.style.borderColor='#e2e8f0'; e.currentTarget.style.boxShadow='none'; e.currentTarget.style.transform=''; }"
                >
                  <div class="position-relative" style="height:180px; background:linear-gradient(180deg, #f8fafc, #f1f5f9);">
                    <button class="position-absolute d-flex align-items-center justify-content-center" style="top:8px; right:8px; width:28px; height:28px; background:#fff; border:1px solid #e2e8f0; border-radius:50%; color:#ef4444; padding:0; z-index:2;" @click.stop="toggleWishlist(product)">
                      <Heart :size="13" :fill="isWishlisted(product.bienTheId) ? '#ef4444' : 'none'" />
                    </button>
                    <img :src="safeImage(product)" :alt="product.tenSanPham" style="width:100%; height:100%; object-fit:contain; padding:8px;" />
                  </div>
                  <div class="p-2 d-flex flex-column flex-grow-1">
                    <p class="fw-black mb-1" style="font-size:13px; line-height:1.3; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; line-clamp:2; -webkit-box-orient:vertical; color:#0f172a; min-height:34px;">{{ product.tenSanPham }}</p>
                    <div class="mb-2 p-2 rounded-2" style="background:#f1f5f9; font-size:11px; color:#475569; font-weight:600; line-height:1.5;">
                      <div class="d-flex align-items-center gap-1 mb-1"><Cpu :size="11" style="color:#ec4899;" /><span>{{ product.cpu || 'Intel Core i5' }}</span></div>
                      <div class="d-flex align-items-center gap-1"><HardDrive :size="11" style="color:#0891b2;" /><span>{{ product.ram || '8GB RAM' }} · {{ product.oCung || '256GB SSD' }}</span></div>
                    </div>
                    <p class="fw-black mb-1" style="font-size:18px; color:#be185d; line-height:1;">{{ formatPrice(product.giaBan) }}</p>
                    <button
                      class="btn w-100 d-flex align-items-center justify-content-center gap-1 fw-bold" style="font-size:12px; padding:9px 12px; background:linear-gradient(180deg, #f472b6 0%, #ec4899 50%, #db2777 100%); color:#ffffff; border:none; border-radius:8px; box-shadow:0 3px 0 #9d174d; transition:all 0.15s;"
                      @click.stop="handleQuickAdd(product)"
                      @mouseenter="(e) => { e.currentTarget.style.transform='translateY(-2px)'; e.currentTarget.style.boxShadow='0 5px 0 #9d174d'; }"
                      @mouseleave="(e) => { e.currentTarget.style.transform=''; e.currentTarget.style.boxShadow='0 3px 0 #9d174d'; }"
                    >
                      <ShoppingCart :size="13" /> Thêm vào giỏ
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section v-if="lenovoProducts.length > 0" class="mt-4" style="border-radius:16px; padding:20px;">
        <div class="d-flex align-items-center gap-3 mb-3">
          <div class="d-flex align-items-center gap-2">
            <Laptop :size="28" style="color:#ec4899;" />
            <h2 class="fw-black mb-0" style="font-size:1.4rem; color:#be185d; text-transform:uppercase; letter-spacing:0.04em;">Lenovo</h2>
          </div>
          <a href="#" class="fw-bold text-decoration-none d-flex align-items-center gap-1 ms-auto" style="font-size:13px; color:#ec4899;">Xem tất cả <ChevronRight :size="16" /></a>
        </div>
        <div class="row g-3">
          <!-- Banner trái -->
          <div class="col-12 col-md-3 col-lg-2" style="padding-left:0;">
            <div class="d-flex flex-row d-md-none gap-2">
              <a href="#" class="flex-grow-1 d-block rounded-3 overflow-hidden position-relative" style="min-height:100px; text-decoration:none;">
                <img src="https://images.unsplash.com/photo-1496181133206-80ce9b88a853?q=80&w=400" alt="Lenovo" class="w-100 h-100" style="object-fit:cover; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(190,24,93,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-2">
                  <div>
                    <h3 class="fw-black text-white mb-0" style="font-size:11px; line-height:1.2;">LAPTOP LENOVO</h3>
                    <p class="mb-0 fw-bold" style="font-size:9px; color:#fce7f3;">Giảm đến 20%</p>
                  </div>
                </div>
              </a>
              <a href="#" class="flex-grow-1 d-block rounded-3 overflow-hidden position-relative" style="min-height:100px; text-decoration:none;">
                <img src="https://images.unsplash.com/photo-1525547719571-a2d4ac8945e2?q=80&w=400" alt="ThinkPad" class="w-100 h-100" style="object-fit:cover; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(37,99,235,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-2">
                  <div>
                    <h3 class="fw-black text-white mb-0" style="font-size:11px; line-height:1.2;">THINKPAD</h3>
                    <p class="mb-0 fw-bold" style="font-size:9px; color:#dbeafe;">Doanh nhân</p>
                  </div>
                </div>
              </a>
            </div>
            <div class="d-none d-md-flex flex-column gap-2 h-100">
              <a
                href="#" class="d-block rounded-3 overflow-hidden position-relative flex-grow-1" style="min-height:220px; text-decoration:none;"
                @mouseenter="(e) => e.currentTarget.style.transform='translateY(-3px)'"
                @mouseleave="(e) => e.currentTarget.style.transform=''"
              >
                <img src="https://images.unsplash.com/photo-1496181133206-80ce9b88a853?q=80&w=500" alt="Lenovo" class="w-100 h-100" style="object-fit:cover; object-position:center top; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(190,24,93,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-3">
                  <div>
                    <h3 class="fw-black text-white mb-1" style="font-size:14px; line-height:1.2; text-shadow:0 2px 6px rgba(0,0,0,0.4);">LAPTOP<br />LENOVO</h3>
                    <p class="mb-0 fw-bold" style="font-size:11px; color:#fce7f3;">Giảm đến 20%</p>
                  </div>
                </div>
              </a>
              <a
                href="#" class="d-block rounded-3 overflow-hidden position-relative flex-grow-1" style="min-height:220px; text-decoration:none;"
                @mouseenter="(e) => e.currentTarget.style.transform='translateY(-3px)'"
                @mouseleave="(e) => e.currentTarget.style.transform=''"
              >
                <img src="https://images.unsplash.com/photo-1525547719571-a2d4ac8945e2?q=80&w=500" alt="ThinkPad" class="w-100 h-100" style="object-fit:cover; object-position:center top; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(37,99,235,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-3">
                  <div>
                    <h3 class="fw-black text-white mb-1" style="font-size:14px; line-height:1.2; text-shadow:0 2px 6px rgba(0,0,0,0.4);">THINKPAD<br />DOANH NHÂN</h3>
                    <p class="mb-0 fw-bold" style="font-size:11px; color:#dbeafe;">Siêu bền</p>
                  </div>
                </div>
              </a>
            </div>
          </div>
          <!-- Hàng 1 -->
          <div class="col-12 col-md-9 col-lg-10">
            <div class="row g-3 mb-2">
              <div v-for="product in lenovoProducts.slice(0, 4)" :key="product.sanPhamId" class="col-6 col-md-3">
                <div
                  class="h-100 d-flex flex-column bg-white rounded-3 overflow-hidden" style="border: 1px solid #e2e8f0; transition: all 0.2s; cursor:pointer;"
                  @click="openProduct(product)"
                  @mouseenter="(e) => { e.currentTarget.style.borderColor='#ec4899'; e.currentTarget.style.boxShadow='0 4px 14px rgba(236,72,153,0.2)'; e.currentTarget.style.transform='translateY(-3px)'; }"
                  @mouseleave="(e) => { e.currentTarget.style.borderColor='#e2e8f0'; e.currentTarget.style.boxShadow='none'; e.currentTarget.style.transform=''; }"
                >
                  <div class="position-relative" style="height:180px; background:linear-gradient(180deg, #f8fafc, #f1f5f9);">
                    <button class="position-absolute d-flex align-items-center justify-content-center" style="top:8px; right:8px; width:28px; height:28px; background:#fff; border:1px solid #e2e8f0; border-radius:50%; color:#ef4444; padding:0; z-index:2;" @click.stop="toggleWishlist(product)">
                      <Heart :size="13" :fill="isWishlisted(product.bienTheId) ? '#ef4444' : 'none'" />
                    </button>
                    <img :src="safeImage(product)" :alt="product.tenSanPham" style="width:100%; height:100%; object-fit:contain; padding:8px;" />
                  </div>
                  <div class="p-2 d-flex flex-column flex-grow-1">
                    <p class="fw-black mb-1" style="font-size:13px; line-height:1.3; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; line-clamp:2; -webkit-box-orient:vertical; color:#0f172a; min-height:34px;">{{ product.tenSanPham }}</p>
                    <div class="mb-2 p-2 rounded-2" style="background:#f1f5f9; font-size:11px; color:#475569; font-weight:600; line-height:1.5;">
                      <div class="d-flex align-items-center gap-1 mb-1"><Cpu :size="11" style="color:#ec4899;" /><span>{{ product.cpu || 'Intel Core i5' }}</span></div>
                      <div class="d-flex align-items-center gap-1"><HardDrive :size="11" style="color:#0891b2;" /><span>{{ product.ram || '8GB RAM' }} · {{ product.oCung || '256GB SSD' }}</span></div>
                    </div>
                    <p class="fw-black mb-1" style="font-size:18px; color:#be185d; line-height:1;">{{ formatPrice(product.giaBan) }}</p>
                    <button
                      class="btn w-100 d-flex align-items-center justify-content-center gap-1 fw-bold" style="font-size:12px; padding:9px 12px; background:linear-gradient(180deg, #f472b6 0%, #ec4899 50%, #db2777 100%); color:#ffffff; border:none; border-radius:8px; box-shadow:0 3px 0 #9d174d; transition:all 0.15s;"
                      @click.stop="handleQuickAdd(product)"
                      @mouseenter="(e) => { e.currentTarget.style.transform='translateY(-2px)'; e.currentTarget.style.boxShadow='0 5px 0 #9d174d'; }"
                      @mouseleave="(e) => { e.currentTarget.style.transform=''; e.currentTarget.style.boxShadow='0 3px 0 #9d174d'; }"
                    >
                      <ShoppingCart :size="13" /> Thêm vào giỏ
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <!-- Hàng 2 -->
            <div v-if="lenovoProducts.length > 4" class="row g-3">
              <div v-for="product in lenovoProducts.slice(4, 8)" :key="product.sanPhamId" class="col-6 col-md-3">
                <div
                  class="h-100 d-flex flex-column bg-white rounded-3 overflow-hidden" style="border: 1px solid #e2e8f0; transition: all 0.2s; cursor:pointer;"
                  @click="openProduct(product)"
                  @mouseenter="(e) => { e.currentTarget.style.borderColor='#ec4899'; e.currentTarget.style.boxShadow='0 4px 14px rgba(236,72,153,0.2)'; e.currentTarget.style.transform='translateY(-3px)'; }"
                  @mouseleave="(e) => { e.currentTarget.style.borderColor='#e2e8f0'; e.currentTarget.style.boxShadow='none'; e.currentTarget.style.transform=''; }"
                >
                  <div class="position-relative" style="height:180px; background:linear-gradient(180deg, #f8fafc, #f1f5f9);">
                    <button class="position-absolute d-flex align-items-center justify-content-center" style="top:8px; right:8px; width:28px; height:28px; background:#fff; border:1px solid #e2e8f0; border-radius:50%; color:#ef4444; padding:0; z-index:2;" @click.stop="toggleWishlist(product)">
                      <Heart :size="13" :fill="isWishlisted(product.bienTheId) ? '#ef4444' : 'none'" />
                    </button>
                    <img :src="safeImage(product)" :alt="product.tenSanPham" style="width:100%; height:100%; object-fit:contain; padding:8px;" />
                  </div>
                  <div class="p-2 d-flex flex-column flex-grow-1">
                    <p class="fw-black mb-1" style="font-size:13px; line-height:1.3; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; line-clamp:2; -webkit-box-orient:vertical; color:#0f172a; min-height:34px;">{{ product.tenSanPham }}</p>
                    <div class="mb-2 p-2 rounded-2" style="background:#f1f5f9; font-size:11px; color:#475569; font-weight:600; line-height:1.5;">
                      <div class="d-flex align-items-center gap-1 mb-1"><Cpu :size="11" style="color:#ec4899;" /><span>{{ product.cpu || 'Intel Core i5' }}</span></div>
                      <div class="d-flex align-items-center gap-1"><HardDrive :size="11" style="color:#0891b2;" /><span>{{ product.ram || '8GB RAM' }} · {{ product.oCung || '256GB SSD' }}</span></div>
                    </div>
                    <p class="fw-black mb-1" style="font-size:18px; color:#be185d; line-height:1;">{{ formatPrice(product.giaBan) }}</p>
                    <button
                      class="btn w-100 d-flex align-items-center justify-content-center gap-1 fw-bold" style="font-size:12px; padding:9px 12px; background:linear-gradient(180deg, #f472b6 0%, #ec4899 50%, #db2777 100%); color:#ffffff; border:none; border-radius:8px; box-shadow:0 3px 0 #9d174d; transition:all 0.15s;"
                      @click.stop="handleQuickAdd(product)"
                      @mouseenter="(e) => { e.currentTarget.style.transform='translateY(-2px)'; e.currentTarget.style.boxShadow='0 5px 0 #9d174d'; }"
                      @mouseleave="(e) => { e.currentTarget.style.transform=''; e.currentTarget.style.boxShadow='0 3px 0 #9d174d'; }"
                    >
                      <ShoppingCart :size="13" /> Thêm vào giỏ
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section v-if="dellProducts.length > 0" class="mt-4" style="border-radius:16px; padding:20px;">
        <div class="d-flex align-items-center gap-3 mb-3">
          <div class="d-flex align-items-center gap-2">
            <Laptop :size="28" style="color:#ec4899;" />
            <h2 class="fw-black mb-0" style="font-size:1.4rem; color:#be185d; text-transform:uppercase; letter-spacing:0.04em;">Dell</h2>
          </div>
          <a href="#" class="fw-bold text-decoration-none d-flex align-items-center gap-1 ms-auto" style="font-size:13px; color:#ec4899;">Xem tất cả <ChevronRight :size="16" /></a>
        </div>
        <div class="row g-3">
          <!-- Banner trái -->
          <div class="col-12 col-md-3 col-lg-2" style="padding-left:0;">
            <div class="d-flex flex-row d-md-none gap-2">
              <a href="#" class="flex-grow-1 d-block rounded-3 overflow-hidden position-relative" style="min-height:100px; text-decoration:none;">
                <img src="https://images.unsplash.com/photo-1593642632559-0c6d3fc62b89?q=80&w=400" alt="Dell" class="w-100 h-100" style="object-fit:cover; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(190,24,93,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-2">
                  <div><h3 class="fw-black text-white mb-0" style="font-size:11px; line-height:1.2;">LAPTOP DELL</h3><p class="mb-0 fw-bold" style="font-size:9px; color:#fce7f3;">Giảm đến 25%</p></div>
                </div>
              </a>
              <a href="#" class="flex-grow-1 d-block rounded-3 overflow-hidden position-relative" style="min-height:100px; text-decoration:none;">
                <img src="https://images.unsplash.com/photo-1496181133206-80ce9b88a853?q=80&w=400" alt="Dell XPS" class="w-100 h-100" style="object-fit:cover; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(37,99,235,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-2">
                  <div><h3 class="fw-black text-white mb-0" style="font-size:11px; line-height:1.2;">DELL XPS</h3><p class="mb-0 fw-bold" style="font-size:9px; color:#dbeafe;">Cao cấp</p></div>
                </div>
              </a>
            </div>
            <div class="d-none d-md-flex flex-column gap-2 h-100">
              <a
                href="#" class="d-block rounded-3 overflow-hidden position-relative flex-grow-1" style="min-height:220px; text-decoration:none;"
                @mouseenter="(e) => e.currentTarget.style.transform='translateY(-3px)'"
                @mouseleave="(e) => e.currentTarget.style.transform=''"
              >
                <img src="https://images.unsplash.com/photo-1593642632559-0c6d3fc62b89?q=80&w=500" alt="Dell" class="w-100 h-100" style="object-fit:cover; object-position:center top; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(190,24,93,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-3">
                  <div><h3 class="fw-black text-white mb-1" style="font-size:14px; line-height:1.2; text-shadow:0 2px 6px rgba(0,0,0,0.4);">LAPTOP<br />DELL</h3><p class="mb-0 fw-bold" style="font-size:11px; color:#fce7f3;">Giảm đến 25%</p></div>
                </div>
              </a>
              <a
                href="#" class="d-block rounded-3 overflow-hidden position-relative flex-grow-1" style="min-height:220px; text-decoration:none;"
                @mouseenter="(e) => e.currentTarget.style.transform='translateY(-3px)'"
                @mouseleave="(e) => e.currentTarget.style.transform=''"
              >
                <img src="https://images.unsplash.com/photo-1496181133206-80ce9b88a853?q=80&w=500" alt="Dell XPS" class="w-100 h-100" style="object-fit:cover; object-position:center top; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(37,99,235,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-3">
                  <div><h3 class="fw-black text-white mb-1" style="font-size:14px; line-height:1.2; text-shadow:0 2px 6px rgba(0,0,0,0.4);">DELL XPS<br />CAO CẤP</h3><p class="mb-0 fw-bold" style="font-size:11px; color:#dbeafe;">Mỏng nhẹ</p></div>
                </div>
              </a>
            </div>
          </div>
          <!-- Hàng 1 -->
          <div class="col-12 col-md-9 col-lg-10">
            <div class="row g-3 mb-2">
              <div v-for="product in dellProducts.slice(0, 4)" :key="product.sanPhamId" class="col-6 col-md-3">
                <div
                  class="h-100 d-flex flex-column bg-white rounded-3 overflow-hidden" style="border: 1px solid #e2e8f0; transition: all 0.2s; cursor:pointer;"
                  @click="openProduct(product)"
                  @mouseenter="(e) => { e.currentTarget.style.borderColor='#ec4899'; e.currentTarget.style.boxShadow='0 4px 14px rgba(236,72,153,0.2)'; e.currentTarget.style.transform='translateY(-3px)'; }"
                  @mouseleave="(e) => { e.currentTarget.style.borderColor='#e2e8f0'; e.currentTarget.style.boxShadow='none'; e.currentTarget.style.transform=''; }"
                >
                  <div class="position-relative" style="height:180px; background:linear-gradient(180deg, #f8fafc, #f1f5f9);">
                    <button class="position-absolute d-flex align-items-center justify-content-center" style="top:8px; right:8px; width:28px; height:28px; background:#fff; border:1px solid #e2e8f0; border-radius:50%; color:#ef4444; padding:0; z-index:2;" @click.stop="toggleWishlist(product)">
                      <Heart :size="13" :fill="isWishlisted(product.bienTheId) ? '#ef4444' : 'none'" />
                    </button>
                    <img :src="safeImage(product)" :alt="product.tenSanPham" style="width:100%; height:100%; object-fit:contain; padding:8px;" />
                  </div>
                  <div class="p-2 d-flex flex-column flex-grow-1">
                    <p class="fw-black mb-1" style="font-size:13px; line-height:1.3; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; line-clamp:2; -webkit-box-orient:vertical; color:#0f172a; min-height:34px;">{{ product.tenSanPham }}</p>
                    <div class="mb-2 p-2 rounded-2" style="background:#f1f5f9; font-size:11px; color:#475569; font-weight:600; line-height:1.5;">
                      <div class="d-flex align-items-center gap-1 mb-1"><Cpu :size="11" style="color:#ec4899;" /><span>{{ product.cpu || 'Intel Core i5' }}</span></div>
                      <div class="d-flex align-items-center gap-1"><HardDrive :size="11" style="color:#0891b2;" /><span>{{ product.ram || '8GB RAM' }} · {{ product.oCung || '256GB SSD' }}</span></div>
                    </div>
                    <p class="fw-black mb-1" style="font-size:18px; color:#be185d; line-height:1;">{{ formatPrice(product.giaBan) }}</p>
                    <button
                      class="btn w-100 d-flex align-items-center justify-content-center gap-1 fw-bold" style="font-size:12px; padding:9px 12px; background:linear-gradient(180deg, #f472b6 0%, #ec4899 50%, #db2777 100%); color:#ffffff; border:none; border-radius:8px; box-shadow:0 3px 0 #9d174d; transition:all 0.15s;"
                      @click.stop="handleQuickAdd(product)"
                      @mouseenter="(e) => { e.currentTarget.style.transform='translateY(-2px)'; e.currentTarget.style.boxShadow='0 5px 0 #9d174d'; }"
                      @mouseleave="(e) => { e.currentTarget.style.transform=''; e.currentTarget.style.boxShadow='0 3px 0 #9d174d'; }"
                    >
                      <ShoppingCart :size="13" /> Thêm vào giỏ
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <!-- Hàng 2 -->
            <div v-if="dellProducts.length > 4" class="row g-3">
              <div v-for="product in dellProducts.slice(4, 8)" :key="product.sanPhamId" class="col-6 col-md-3">
                <div
                  class="h-100 d-flex flex-column bg-white rounded-3 overflow-hidden" style="border: 1px solid #e2e8f0; transition: all 0.2s; cursor:pointer;"
                  @click="openProduct(product)"
                  @mouseenter="(e) => { e.currentTarget.style.borderColor='#ec4899'; e.currentTarget.style.boxShadow='0 4px 14px rgba(236,72,153,0.2)'; e.currentTarget.style.transform='translateY(-3px)'; }"
                  @mouseleave="(e) => { e.currentTarget.style.borderColor='#e2e8f0'; e.currentTarget.style.boxShadow='none'; e.currentTarget.style.transform=''; }"
                >
                  <div class="position-relative" style="height:180px; background:linear-gradient(180deg, #f8fafc, #f1f5f9);">
                    <button class="position-absolute d-flex align-items-center justify-content-center" style="top:8px; right:8px; width:28px; height:28px; background:#fff; border:1px solid #e2e8f0; border-radius:50%; color:#ef4444; padding:0; z-index:2;" @click.stop="toggleWishlist(product)">
                      <Heart :size="13" :fill="isWishlisted(product.bienTheId) ? '#ef4444' : 'none'" />
                    </button>
                    <img :src="safeImage(product)" :alt="product.tenSanPham" style="width:100%; height:100%; object-fit:contain; padding:8px;" />
                  </div>
                  <div class="p-2 d-flex flex-column flex-grow-1">
                    <p class="fw-black mb-1" style="font-size:13px; line-height:1.3; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; line-clamp:2; -webkit-box-orient:vertical; color:#0f172a; min-height:34px;">{{ product.tenSanPham }}</p>
                    <div class="mb-2 p-2 rounded-2" style="background:#f1f5f9; font-size:11px; color:#475569; font-weight:600; line-height:1.5;">
                      <div class="d-flex align-items-center gap-1 mb-1"><Cpu :size="11" style="color:#ec4899;" /><span>{{ product.cpu || 'Intel Core i5' }}</span></div>
                      <div class="d-flex align-items-center gap-1"><HardDrive :size="11" style="color:#0891b2;" /><span>{{ product.ram || '8GB RAM' }} · {{ product.oCung || '256GB SSD' }}</span></div>
                    </div>
                    <p class="fw-black mb-1" style="font-size:18px; color:#be185d; line-height:1;">{{ formatPrice(product.giaBan) }}</p>
                    <button
                      class="btn w-100 d-flex align-items-center justify-content-center gap-1 fw-bold" style="font-size:12px; padding:9px 12px; background:linear-gradient(180deg, #f472b6 0%, #ec4899 50%, #db2777 100%); color:#ffffff; border:none; border-radius:8px; box-shadow:0 3px 0 #9d174d; transition:all 0.15s;"
                      @click.stop="handleQuickAdd(product)"
                      @mouseenter="(e) => { e.currentTarget.style.transform='translateY(-2px)'; e.currentTarget.style.boxShadow='0 5px 0 #9d174d'; }"
                      @mouseleave="(e) => { e.currentTarget.style.transform=''; e.currentTarget.style.boxShadow='0 3px 0 #9d174d'; }"
                    >
                      <ShoppingCart :size="13" /> Thêm vào giỏ
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section v-if="hpProducts.length > 0" class="mt-4" style="border-radius:16px; padding:20px;">
        <div class="d-flex align-items-center gap-3 mb-3">
          <div class="d-flex align-items-center gap-2">
            <Laptop :size="28" style="color:#ec4899;" />
            <h2 class="fw-black mb-0" style="font-size:1.4rem; color:#be185d; text-transform:uppercase; letter-spacing:0.04em;">HP</h2>
          </div>
          <a href="#" class="fw-bold text-decoration-none d-flex align-items-center gap-1 ms-auto" style="font-size:13px; color:#ec4899;">Xem tất cả <ChevronRight :size="16" /></a>
        </div>
        <div class="row g-3">
          <!-- Banner trái -->
          <div class="col-12 col-md-3 col-lg-2" style="padding-left:0;">
            <div class="d-flex flex-row d-md-none gap-2">
              <a href="#" class="flex-grow-1 d-block rounded-3 overflow-hidden position-relative" style="min-height:100px; text-decoration:none;">
                <img src="https://images.unsplash.com/photo-1496181133206-80ce9b88a853?q=80&w=400" alt="HP" class="w-100 h-100" style="object-fit:cover; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(190,24,93,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-2">
                  <div><h3 class="fw-black text-white mb-0" style="font-size:11px; line-height:1.2;">LAPTOP HP</h3><p class="mb-0 fw-bold" style="font-size:9px; color:#fce7f3;">Giảm đến 20%</p></div>
                </div>
              </a>
              <a href="#" class="flex-grow-1 d-block rounded-3 overflow-hidden position-relative" style="min-height:100px; text-decoration:none;">
                <img src="https://images.unsplash.com/photo-1525547719571-a2d4ac8945e2?q=80&w=400" alt="HP Envy" class="w-100 h-100" style="object-fit:cover; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(37,99,235,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-2">
                  <div><h3 class="fw-black text-white mb-0" style="font-size:11px; line-height:1.2;">HP ENVY</h3><p class="mb-0 fw-bold" style="font-size:9px; color:#dbeafe;">Cao cấp</p></div>
                </div>
              </a>
            </div>
            <div class="d-none d-md-flex flex-column gap-2 h-100">
              <a
                href="#" class="d-block rounded-3 overflow-hidden position-relative flex-grow-1" style="min-height:220px; text-decoration:none;"
                @mouseenter="(e) => e.currentTarget.style.transform='translateY(-3px)'"
                @mouseleave="(e) => e.currentTarget.style.transform=''"
              >
                <img src="https://images.unsplash.com/photo-1496181133206-80ce9b88a853?q=80&w=500" alt="HP" class="w-100 h-100" style="object-fit:cover; object-position:center top; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(190,24,93,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-3">
                  <div><h3 class="fw-black text-white mb-1" style="font-size:14px; line-height:1.2; text-shadow:0 2px 6px rgba(0,0,0,0.4);">LAPTOP<br />HP</h3><p class="mb-0 fw-bold" style="font-size:11px; color:#fce7f3;">Giảm đến 20%</p></div>
                </div>
              </a>
              <a
                href="#" class="d-block rounded-3 overflow-hidden position-relative flex-grow-1" style="min-height:220px; text-decoration:none;"
                @mouseenter="(e) => e.currentTarget.style.transform='translateY(-3px)'"
                @mouseleave="(e) => e.currentTarget.style.transform=''"
              >
                <img src="https://images.unsplash.com/photo-1525547719571-a2d4ac8945e2?q=80&w=500" alt="HP Envy" class="w-100 h-100" style="object-fit:cover; object-position:center top; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(37,99,235,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-3">
                  <div><h3 class="fw-black text-white mb-1" style="font-size:14px; line-height:1.2; text-shadow:0 2px 6px rgba(0,0,0,0.4);">HP ENVY<br />CAO CẤP</h3><p class="mb-0 fw-bold" style="font-size:11px; color:#dbeafe;">Mỏng nhẹ</p></div>
                </div>
              </a>
            </div>
          </div>
          <!-- Hàng 1 -->
          <div class="col-12 col-md-9 col-lg-10">
            <div class="row g-3 mb-2">
              <div v-for="product in hpProducts.slice(0, 4)" :key="product.sanPhamId" class="col-6 col-md-3">
                <div
                  class="h-100 d-flex flex-column bg-white rounded-3 overflow-hidden" style="border: 1px solid #e2e8f0; transition: all 0.2s; cursor:pointer;"
                  @click="openProduct(product)"
                  @mouseenter="(e) => { e.currentTarget.style.borderColor='#ec4899'; e.currentTarget.style.boxShadow='0 4px 14px rgba(236,72,153,0.2)'; e.currentTarget.style.transform='translateY(-3px)'; }"
                  @mouseleave="(e) => { e.currentTarget.style.borderColor='#e2e8f0'; e.currentTarget.style.boxShadow='none'; e.currentTarget.style.transform=''; }"
                >
                  <div class="position-relative" style="height:180px; background:linear-gradient(180deg, #f8fafc, #f1f5f9);">
                    <button class="position-absolute d-flex align-items-center justify-content-center" style="top:8px; right:8px; width:28px; height:28px; background:#fff; border:1px solid #e2e8f0; border-radius:50%; color:#ef4444; padding:0; z-index:2;" @click.stop="toggleWishlist(product)">
                      <Heart :size="13" :fill="isWishlisted(product.bienTheId) ? '#ef4444' : 'none'" />
                    </button>
                    <img :src="safeImage(product)" :alt="product.tenSanPham" style="width:100%; height:100%; object-fit:contain; padding:8px;" />
                  </div>
                  <div class="p-2 d-flex flex-column flex-grow-1">
                    <p class="fw-black mb-1" style="font-size:13px; line-height:1.3; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; line-clamp:2; -webkit-box-orient:vertical; color:#0f172a; min-height:34px;">{{ product.tenSanPham }}</p>
                    <div class="mb-2 p-2 rounded-2" style="background:#f1f5f9; font-size:11px; color:#475569; font-weight:600; line-height:1.5;">
                      <div class="d-flex align-items-center gap-1 mb-1"><Cpu :size="11" style="color:#ec4899;" /><span>{{ product.cpu || 'Intel Core i5' }}</span></div>
                      <div class="d-flex align-items-center gap-1"><HardDrive :size="11" style="color:#0891b2;" /><span>{{ product.ram || '8GB RAM' }} · {{ product.oCung || '256GB SSD' }}</span></div>
                    </div>
                    <p class="fw-black mb-1" style="font-size:18px; color:#be185d; line-height:1;">{{ formatPrice(product.giaBan) }}</p>
                    <button
                      class="btn w-100 d-flex align-items-center justify-content-center gap-1 fw-bold" style="font-size:12px; padding:9px 12px; background:linear-gradient(180deg, #f472b6 0%, #ec4899 50%, #db2777 100%); color:#ffffff; border:none; border-radius:8px; box-shadow:0 3px 0 #9d174d; transition:all 0.15s;"
                      @click.stop="handleQuickAdd(product)"
                      @mouseenter="(e) => { e.currentTarget.style.transform='translateY(-2px)'; e.currentTarget.style.boxShadow='0 5px 0 #9d174d'; }"
                      @mouseleave="(e) => { e.currentTarget.style.transform=''; e.currentTarget.style.boxShadow='0 3px 0 #9d174d'; }"
                    >
                      <ShoppingCart :size="13" /> Thêm vào giỏ
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <!-- Hàng 2 -->
            <div v-if="hpProducts.length > 4" class="row g-3">
              <div v-for="product in hpProducts.slice(4, 8)" :key="product.sanPhamId" class="col-6 col-md-3">
                <div
                  class="h-100 d-flex flex-column bg-white rounded-3 overflow-hidden" style="border: 1px solid #e2e8f0; transition: all 0.2s; cursor:pointer;"
                  @click="openProduct(product)"
                  @mouseenter="(e) => { e.currentTarget.style.borderColor='#ec4899'; e.currentTarget.style.boxShadow='0 4px 14px rgba(236,72,153,0.2)'; e.currentTarget.style.transform='translateY(-3px)'; }"
                  @mouseleave="(e) => { e.currentTarget.style.borderColor='#e2e8f0'; e.currentTarget.style.boxShadow='none'; e.currentTarget.style.transform=''; }"
                >
                  <div class="position-relative" style="height:180px; background:linear-gradient(180deg, #f8fafc, #f1f5f9);">
                    <button class="position-absolute d-flex align-items-center justify-content-center" style="top:8px; right:8px; width:28px; height:28px; background:#fff; border:1px solid #e2e8f0; border-radius:50%; color:#ef4444; padding:0; z-index:2;" @click.stop="toggleWishlist(product)">
                      <Heart :size="13" :fill="isWishlisted(product.bienTheId) ? '#ef4444' : 'none'" />
                    </button>
                    <img :src="safeImage(product)" :alt="product.tenSanPham" style="width:100%; height:100%; object-fit:contain; padding:8px;" />
                  </div>
                  <div class="p-2 d-flex flex-column flex-grow-1">
                    <p class="fw-black mb-1" style="font-size:13px; line-height:1.3; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; line-clamp:2; -webkit-box-orient:vertical; color:#0f172a; min-height:34px;">{{ product.tenSanPham }}</p>
                    <div class="mb-2 p-2 rounded-2" style="background:#f1f5f9; font-size:11px; color:#475569; font-weight:600; line-height:1.5;">
                      <div class="d-flex align-items-center gap-1 mb-1"><Cpu :size="11" style="color:#ec4899;" /><span>{{ product.cpu || 'Intel Core i5' }}</span></div>
                      <div class="d-flex align-items-center gap-1"><HardDrive :size="11" style="color:#0891b2;" /><span>{{ product.ram || '8GB RAM' }} · {{ product.oCung || '256GB SSD' }}</span></div>
                    </div>
                    <p class="fw-black mb-1" style="font-size:18px; color:#be185d; line-height:1;">{{ formatPrice(product.giaBan) }}</p>
                    <button
                      class="btn w-100 d-flex align-items-center justify-content-center gap-1 fw-bold" style="font-size:12px; padding:9px 12px; background:linear-gradient(180deg, #f472b6 0%, #ec4899 50%, #db2777 100%); color:#ffffff; border:none; border-radius:8px; box-shadow:0 3px 0 #9d174d; transition:all 0.15s;"
                      @click.stop="handleQuickAdd(product)"
                      @mouseenter="(e) => { e.currentTarget.style.transform='translateY(-2px)'; e.currentTarget.style.boxShadow='0 5px 0 #9d174d'; }"
                      @mouseleave="(e) => { e.currentTarget.style.transform=''; e.currentTarget.style.boxShadow='0 3px 0 #9d174d'; }"
                    >
                      <ShoppingCart :size="13" /> Thêm vào giỏ
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section v-if="msiProducts.length > 0" class="mt-4" style="border-radius:16px; padding:20px;">
        <div class="d-flex align-items-center gap-3 mb-3">
          <div class="d-flex align-items-center gap-2">
            <Laptop :size="28" style="color:#ec4899;" />
            <h2 class="fw-black mb-0" style="font-size:1.4rem; color:#be185d; text-transform:uppercase; letter-spacing:0.04em;">MSI</h2>
          </div>
          <a href="#" class="fw-bold text-decoration-none d-flex align-items-center gap-1 ms-auto" style="font-size:13px; color:#ec4899;">Xem tất cả <ChevronRight :size="16" /></a>
        </div>
        <div class="row g-3">
          <!-- Banner trái -->
          <div class="col-12 col-md-3 col-lg-2" style="padding-left:0;">
            <div class="d-flex flex-row d-md-none gap-2">
              <a href="#" class="flex-grow-1 d-block rounded-3 overflow-hidden position-relative" style="min-height:100px; text-decoration:none;">
                <img src="https://images.unsplash.com/photo-1593640408182-31c70c8268f5?q=80&w=400" alt="MSI" class="w-100 h-100" style="object-fit:cover; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(190,24,93,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-2">
                  <div><h3 class="fw-black text-white mb-0" style="font-size:11px; line-height:1.2;">LAPTOP MSI</h3><p class="mb-0 fw-bold" style="font-size:9px; color:#fce7f3;">Gaming</p></div>
                </div>
              </a>
              <a href="#" class="flex-grow-1 d-block rounded-3 overflow-hidden position-relative" style="min-height:100px; text-decoration:none;">
                <img src="https://images.unsplash.com/photo-1603302576837-37561b2e2302?q=80&w=400" alt="MSI Gaming" class="w-100 h-100" style="object-fit:cover; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(37,99,235,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-2">
                  <div><h3 class="fw-black text-white mb-0" style="font-size:11px; line-height:1.2;">MSI GAMING</h3><p class="mb-0 fw-bold" style="font-size:9px; color:#dbeafe;">RTX Series</p></div>
                </div>
              </a>
            </div>
            <div class="d-none d-md-flex flex-column gap-2 h-100">
              <a
                href="#" class="d-block rounded-3 overflow-hidden position-relative flex-grow-1" style="min-height:220px; text-decoration:none;"
                @mouseenter="(e) => e.currentTarget.style.transform='translateY(-3px)'"
                @mouseleave="(e) => e.currentTarget.style.transform=''"
              >
                <img src="https://images.unsplash.com/photo-1593640408182-31c70c8268f5?q=80&w=500" alt="MSI" class="w-100 h-100" style="object-fit:cover; object-position:center top; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(190,24,93,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-3">
                  <div><h3 class="fw-black text-white mb-1" style="font-size:14px; line-height:1.2; text-shadow:0 2px 6px rgba(0,0,0,0.4);">LAPTOP<br />MSI</h3><p class="mb-0 fw-bold" style="font-size:11px; color:#fce7f3;">Gaming</p></div>
                </div>
              </a>
              <a
                href="#" class="d-block rounded-3 overflow-hidden position-relative flex-grow-1" style="min-height:220px; text-decoration:none;"
                @mouseenter="(e) => e.currentTarget.style.transform='translateY(-3px)'"
                @mouseleave="(e) => e.currentTarget.style.transform=''"
              >
                <img src="https://images.unsplash.com/photo-1603302576837-37561b2e2302?q=80&w=500" alt="MSI Gaming" class="w-100 h-100" style="object-fit:cover; object-position:center top; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(37,99,235,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-3">
                  <div><h3 class="fw-black text-white mb-1" style="font-size:14px; line-height:1.2; text-shadow:0 2px 6px rgba(0,0,0,0.4);">MSI<br />GAMING</h3><p class="mb-0 fw-bold" style="font-size:11px; color:#dbeafe;">RTX Series</p></div>
                </div>
              </a>
            </div>
          </div>
          <!-- Hàng 1 -->
          <div class="col-12 col-md-9 col-lg-10">
            <div class="row g-3 mb-2">
              <div v-for="product in msiProducts.slice(0, 4)" :key="product.sanPhamId" class="col-6 col-md-3">
                <div
                  class="h-100 d-flex flex-column bg-white rounded-3 overflow-hidden" style="border: 1px solid #e2e8f0; transition: all 0.2s; cursor:pointer;"
                  @click="openProduct(product)"
                  @mouseenter="(e) => { e.currentTarget.style.borderColor='#ec4899'; e.currentTarget.style.boxShadow='0 4px 14px rgba(236,72,153,0.2)'; e.currentTarget.style.transform='translateY(-3px)'; }"
                  @mouseleave="(e) => { e.currentTarget.style.borderColor='#e2e8f0'; e.currentTarget.style.boxShadow='none'; e.currentTarget.style.transform=''; }"
                >
                  <div class="position-relative" style="height:180px; background:linear-gradient(180deg, #f8fafc, #f1f5f9);">
                    <button class="position-absolute d-flex align-items-center justify-content-center" style="top:8px; right:8px; width:28px; height:28px; background:#fff; border:1px solid #e2e8f0; border-radius:50%; color:#ef4444; padding:0; z-index:2;" @click.stop="toggleWishlist(product)">
                      <Heart :size="13" :fill="isWishlisted(product.bienTheId) ? '#ef4444' : 'none'" />
                    </button>
                    <img :src="safeImage(product)" :alt="product.tenSanPham" style="width:100%; height:100%; object-fit:contain; padding:8px;" />
                  </div>
                  <div class="p-2 d-flex flex-column flex-grow-1">
                    <p class="fw-black mb-1" style="font-size:13px; line-height:1.3; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; line-clamp:2; -webkit-box-orient:vertical; color:#0f172a; min-height:34px;">{{ product.tenSanPham }}</p>
                    <div class="mb-2 p-2 rounded-2" style="background:#f1f5f9; font-size:11px; color:#475569; font-weight:600; line-height:1.5;">
                      <div class="d-flex align-items-center gap-1 mb-1"><Cpu :size="11" style="color:#ec4899;" /><span>{{ product.cpu || 'Intel Core i5' }}</span></div>
                      <div class="d-flex align-items-center gap-1"><HardDrive :size="11" style="color:#0891b2;" /><span>{{ product.ram || '8GB RAM' }} · {{ product.oCung || '256GB SSD' }}</span></div>
                    </div>
                    <p class="fw-black mb-1" style="font-size:18px; color:#be185d; line-height:1;">{{ formatPrice(product.giaBan) }}</p>
                    <button
                      class="btn w-100 d-flex align-items-center justify-content-center gap-1 fw-bold" style="font-size:12px; padding:9px 12px; background:linear-gradient(180deg, #f472b6 0%, #ec4899 50%, #db2777 100%); color:#ffffff; border:none; border-radius:8px; box-shadow:0 3px 0 #9d174d; transition:all 0.15s;"
                      @click.stop="handleQuickAdd(product)"
                      @mouseenter="(e) => { e.currentTarget.style.transform='translateY(-2px)'; e.currentTarget.style.boxShadow='0 5px 0 #9d174d'; }"
                      @mouseleave="(e) => { e.currentTarget.style.transform=''; e.currentTarget.style.boxShadow='0 3px 0 #9d174d'; }"
                    >
                      <ShoppingCart :size="13" /> Thêm vào giỏ
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <!-- Hàng 2 -->
            <div v-if="msiProducts.length > 4" class="row g-3">
              <div v-for="product in msiProducts.slice(4, 8)" :key="product.sanPhamId" class="col-6 col-md-3">
                <div
                  class="h-100 d-flex flex-column bg-white rounded-3 overflow-hidden" style="border: 1px solid #e2e8f0; transition: all 0.2s; cursor:pointer;"
                  @click="openProduct(product)"
                  @mouseenter="(e) => { e.currentTarget.style.borderColor='#ec4899'; e.currentTarget.style.boxShadow='0 4px 14px rgba(236,72,153,0.2)'; e.currentTarget.style.transform='translateY(-3px)'; }"
                  @mouseleave="(e) => { e.currentTarget.style.borderColor='#e2e8f0'; e.currentTarget.style.boxShadow='none'; e.currentTarget.style.transform=''; }"
                >
                  <div class="position-relative" style="height:180px; background:linear-gradient(180deg, #f8fafc, #f1f5f9);">
                    <button class="position-absolute d-flex align-items-center justify-content-center" style="top:8px; right:8px; width:28px; height:28px; background:#fff; border:1px solid #e2e8f0; border-radius:50%; color:#ef4444; padding:0; z-index:2;" @click.stop="toggleWishlist(product)">
                      <Heart :size="13" :fill="isWishlisted(product.bienTheId) ? '#ef4444' : 'none'" />
                    </button>
                    <img :src="safeImage(product)" :alt="product.tenSanPham" style="width:100%; height:100%; object-fit:contain; padding:8px;" />
                  </div>
                  <div class="p-2 d-flex flex-column flex-grow-1">
                    <p class="fw-black mb-1" style="font-size:13px; line-height:1.3; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; line-clamp:2; -webkit-box-orient:vertical; color:#0f172a; min-height:34px;">{{ product.tenSanPham }}</p>
                    <div class="mb-2 p-2 rounded-2" style="background:#f1f5f9; font-size:11px; color:#475569; font-weight:600; line-height:1.5;">
                      <div class="d-flex align-items-center gap-1 mb-1"><Cpu :size="11" style="color:#ec4899;" /><span>{{ product.cpu || 'Intel Core i5' }}</span></div>
                      <div class="d-flex align-items-center gap-1"><HardDrive :size="11" style="color:#0891b2;" /><span>{{ product.ram || '8GB RAM' }} · {{ product.oCung || '256GB SSD' }}</span></div>
                    </div>
                    <p class="fw-black mb-1" style="font-size:18px; color:#be185d; line-height:1;">{{ formatPrice(product.giaBan) }}</p>
                    <button
                      class="btn w-100 d-flex align-items-center justify-content-center gap-1 fw-bold" style="font-size:12px; padding:9px 12px; background:linear-gradient(180deg, #f472b6 0%, #ec4899 50%, #db2777 100%); color:#ffffff; border:none; border-radius:8px; box-shadow:0 3px 0 #9d174d; transition:all 0.15s;"
                      @click.stop="handleQuickAdd(product)"
                      @mouseenter="(e) => { e.currentTarget.style.transform='translateY(-2px)'; e.currentTarget.style.boxShadow='0 5px 0 #9d174d'; }"
                      @mouseleave="(e) => { e.currentTarget.style.transform=''; e.currentTarget.style.boxShadow='0 3px 0 #9d174d'; }"
                    >
                      <ShoppingCart :size="13" /> Thêm vào giỏ
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section v-if="acerProducts.length > 0" class="mt-4" style="border-radius:16px; padding:20px;">
        <div class="d-flex align-items-center gap-3 mb-3">
          <div class="d-flex align-items-center gap-2">
            <Laptop :size="28" style="color:#ec4899;" />
            <h2 class="fw-black mb-0" style="font-size:1.4rem; color:#be185d; text-transform:uppercase; letter-spacing:0.04em;">Acer</h2>
          </div>
          <a href="#" class="fw-bold text-decoration-none d-flex align-items-center gap-1 ms-auto" style="font-size:13px; color:#ec4899;">Xem tất cả <ChevronRight :size="16" /></a>
        </div>
        <div class="row g-3">
          <!-- Banner trái -->
          <div class="col-12 col-md-3 col-lg-2" style="padding-left:0;">
            <div class="d-flex flex-row d-md-none gap-2">
              <a href="#" class="flex-grow-1 d-block rounded-3 overflow-hidden position-relative" style="min-height:100px; text-decoration:none;">
                <img src="https://images.unsplash.com/photo-1496181133206-80ce9b88a853?q=80&w=400" alt="Acer" class="w-100 h-100" style="object-fit:cover; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(190,24,93,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-2">
                  <div><h3 class="fw-black text-white mb-0" style="font-size:11px; line-height:1.2;">LAPTOP ACER</h3><p class="mb-0 fw-bold" style="font-size:9px; color:#fce7f3;">Giá tốt</p></div>
                </div>
              </a>
              <a href="#" class="flex-grow-1 d-block rounded-3 overflow-hidden position-relative" style="min-height:100px; text-decoration:none;">
                <img src="https://images.unsplash.com/photo-1525547719571-a2d4ac8945e2?q=80&w=400" alt="Acer Aspire" class="w-100 h-100" style="object-fit:cover; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(37,99,235,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-2">
                  <div><h3 class="fw-black text-white mb-0" style="font-size:11px; line-height:1.2;">ACER ASPIRE</h3><p class="mb-0 fw-bold" style="font-size:9px; color:#dbeafe;">Văn phòng</p></div>
                </div>
              </a>
            </div>
            <div class="d-none d-md-flex flex-column gap-2 h-100">
              <a
                href="#" class="d-block rounded-3 overflow-hidden position-relative flex-grow-1" style="min-height:220px; text-decoration:none;"
                @mouseenter="(e) => e.currentTarget.style.transform='translateY(-3px)'"
                @mouseleave="(e) => e.currentTarget.style.transform=''"
              >
                <img src="https://images.unsplash.com/photo-1496181133206-80ce9b88a853?q=80&w=500" alt="Acer" class="w-100 h-100" style="object-fit:cover; object-position:center top; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(190,24,93,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-3">
                  <div><h3 class="fw-black text-white mb-1" style="font-size:14px; line-height:1.2; text-shadow:0 2px 6px rgba(0,0,0,0.4);">LAPTOP<br />ACER</h3><p class="mb-0 fw-bold" style="font-size:11px; color:#fce7f3;">Giá tốt nhất</p></div>
                </div>
              </a>
              <a
                href="#" class="d-block rounded-3 overflow-hidden position-relative flex-grow-1" style="min-height:220px; text-decoration:none;"
                @mouseenter="(e) => e.currentTarget.style.transform='translateY(-3px)'"
                @mouseleave="(e) => e.currentTarget.style.transform=''"
              >
                <img src="https://images.unsplash.com/photo-1525547719571-a2d4ac8945e2?q=80&w=500" alt="Acer Aspire" class="w-100 h-100" style="object-fit:cover; object-position:center top; position:absolute; top:0; left:0;" />
                <div class="position-absolute top-0 start-0 w-100 h-100" style="background:linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 50%, rgba(37,99,235,0.75) 100%);"></div>
                <div class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-end p-3">
                  <div><h3 class="fw-black text-white mb-1" style="font-size:14px; line-height:1.2; text-shadow:0 2px 6px rgba(0,0,0,0.4);">ACER<br />ASPIRE</h3><p class="mb-0 fw-bold" style="font-size:11px; color:#dbeafe;">Văn phòng</p></div>
                </div>
              </a>
            </div>
          </div>
          <!-- Hàng 1 -->
          <div class="col-12 col-md-9 col-lg-10">
            <div class="row g-3 mb-2">
              <div v-for="product in acerProducts.slice(0, 4)" :key="product.sanPhamId" class="col-6 col-md-3">
                <div
                  class="h-100 d-flex flex-column bg-white rounded-3 overflow-hidden" style="border: 1px solid #e2e8f0; transition: all 0.2s; cursor:pointer;"
                  @click="openProduct(product)"
                  @mouseenter="(e) => { e.currentTarget.style.borderColor='#ec4899'; e.currentTarget.style.boxShadow='0 4px 14px rgba(236,72,153,0.2)'; e.currentTarget.style.transform='translateY(-3px)'; }"
                  @mouseleave="(e) => { e.currentTarget.style.borderColor='#e2e8f0'; e.currentTarget.style.boxShadow='none'; e.currentTarget.style.transform=''; }"
                >
                  <div class="position-relative" style="height:180px; background:linear-gradient(180deg, #f8fafc, #f1f5f9);">
                    <button class="position-absolute d-flex align-items-center justify-content-center" style="top:8px; right:8px; width:28px; height:28px; background:#fff; border:1px solid #e2e8f0; border-radius:50%; color:#ef4444; padding:0; z-index:2;" @click.stop="toggleWishlist(product)">
                      <Heart :size="13" :fill="isWishlisted(product.bienTheId) ? '#ef4444' : 'none'" />
                    </button>
                    <img :src="safeImage(product)" :alt="product.tenSanPham" style="width:100%; height:100%; object-fit:contain; padding:8px;" />
                  </div>
                  <div class="p-2 d-flex flex-column flex-grow-1">
                    <p class="fw-black mb-1" style="font-size:13px; line-height:1.3; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; line-clamp:2; -webkit-box-orient:vertical; color:#0f172a; min-height:34px;">{{ product.tenSanPham }}</p>
                    <div class="mb-2 p-2 rounded-2" style="background:#f1f5f9; font-size:11px; color:#475569; font-weight:600; line-height:1.5;">
                      <div class="d-flex align-items-center gap-1 mb-1"><Cpu :size="11" style="color:#ec4899;" /><span>{{ product.cpu || 'Intel Core i5' }}</span></div>
                      <div class="d-flex align-items-center gap-1"><HardDrive :size="11" style="color:#0891b2;" /><span>{{ product.ram || '8GB RAM' }} · {{ product.oCung || '256GB SSD' }}</span></div>
                    </div>
                    <p class="fw-black mb-1" style="font-size:18px; color:#be185d; line-height:1;">{{ formatPrice(product.giaBan) }}</p>
                    <button
                      class="btn w-100 d-flex align-items-center justify-content-center gap-1 fw-bold" style="font-size:12px; padding:9px 12px; background:linear-gradient(180deg, #f472b6 0%, #ec4899 50%, #db2777 100%); color:#ffffff; border:none; border-radius:8px; box-shadow:0 3px 0 #9d174d; transition:all 0.15s;"
                      @click.stop="handleQuickAdd(product)"
                      @mouseenter="(e) => { e.currentTarget.style.transform='translateY(-2px)'; e.currentTarget.style.boxShadow='0 5px 0 #9d174d'; }"
                      @mouseleave="(e) => { e.currentTarget.style.transform=''; e.currentTarget.style.boxShadow='0 3px 0 #9d174d'; }"
                    >
                      <ShoppingCart :size="13" /> Thêm vào giỏ
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <!-- Hàng 2 -->
            <div v-if="acerProducts.length > 4" class="row g-3">
              <div v-for="product in acerProducts.slice(4, 8)" :key="product.sanPhamId" class="col-6 col-md-3">
                <div
                  class="h-100 d-flex flex-column bg-white rounded-3 overflow-hidden" style="border: 1px solid #e2e8f0; transition: all 0.2s; cursor:pointer;"
                  @click="openProduct(product)"
                  @mouseenter="(e) => { e.currentTarget.style.borderColor='#ec4899'; e.currentTarget.style.boxShadow='0 4px 14px rgba(236,72,153,0.2)'; e.currentTarget.style.transform='translateY(-3px)'; }"
                  @mouseleave="(e) => { e.currentTarget.style.borderColor='#e2e8f0'; e.currentTarget.style.boxShadow='none'; e.currentTarget.style.transform=''; }"
                >
                  <div class="position-relative" style="height:180px; background:linear-gradient(180deg, #f8fafc, #f1f5f9);">
                    <button class="position-absolute d-flex align-items-center justify-content-center" style="top:8px; right:8px; width:28px; height:28px; background:#fff; border:1px solid #e2e8f0; border-radius:50%; color:#ef4444; padding:0; z-index:2;" @click.stop="toggleWishlist(product)">
                      <Heart :size="13" :fill="isWishlisted(product.bienTheId) ? '#ef4444' : 'none'" />
                    </button>
                    <img :src="safeImage(product)" :alt="product.tenSanPham" style="width:100%; height:100%; object-fit:contain; padding:8px;" />
                  </div>
                  <div class="p-2 d-flex flex-column flex-grow-1">
                    <p class="fw-black mb-1" style="font-size:13px; line-height:1.3; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; line-clamp:2; -webkit-box-orient:vertical; color:#0f172a; min-height:34px;">{{ product.tenSanPham }}</p>
                    <div class="mb-2 p-2 rounded-2" style="background:#f1f5f9; font-size:11px; color:#475569; font-weight:600; line-height:1.5;">
                      <div class="d-flex align-items-center gap-1 mb-1"><Cpu :size="11" style="color:#ec4899;" /><span>{{ product.cpu || 'Intel Core i5' }}</span></div>
                      <div class="d-flex align-items-center gap-1"><HardDrive :size="11" style="color:#0891b2;" /><span>{{ product.ram || '8GB RAM' }} · {{ product.oCung || '256GB SSD' }}</span></div>
                    </div>
                    <p class="fw-black mb-1" style="font-size:18px; color:#be185d; line-height:1;">{{ formatPrice(product.giaBan) }}</p>
                    <button
                      class="btn w-100 d-flex align-items-center justify-content-center gap-1 fw-bold" style="font-size:12px; padding:9px 12px; background:linear-gradient(180deg, #f472b6 0%, #ec4899 50%, #db2777 100%); color:#ffffff; border:none; border-radius:8px; box-shadow:0 3px 0 #9d174d; transition:all 0.15s;"
                      @click.stop="handleQuickAdd(product)"
                      @mouseenter="(e) => { e.currentTarget.style.transform='translateY(-2px)'; e.currentTarget.style.boxShadow='0 5px 0 #9d174d'; }"
                      @mouseleave="(e) => { e.currentTarget.style.transform=''; e.currentTarget.style.boxShadow='0 3px 0 #9d174d'; }"
                    >
                      <ShoppingCart :size="13" /> Thêm vào giỏ
                    </button>
                  </div>
                </div>
              </div>
            </div>
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
              >{{ t("cart.title") }}</span>
              <span
                v-if="cartCount > 0"
                class="badge bg-warning text-dark fw-bold rounded-pill"
                style="font-size: 10px"
              >{{ cartCount }}</span>
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
            v-if="cartCount > 0"
            class="d-flex align-items-center justify-content-between px-4 py-2"
            style="border-bottom: 1px solid var(--border-color-soft); font-size: 11px; color: var(--text-secondary);"
          >
            <label class="d-flex align-items-center gap-2" style="cursor:pointer;">
              <input
                type="checkbox"
                :checked="cartSelected.size === cart.length && cart.length > 0"
                :indeterminate="cartSelected.size > 0 && cartSelected.size < cart.length"
                @change="cartSelected.size === cart.length ? deselectAllCartItems() : selectAllCartItems()"
              />
              <span>{{ t('cart.selectAll') }}</span>
            </label>
            <span v-if="cartSelectedCount > 0" class="text-warning fw-semibold">
              {{ cartSelectedCount }} {{ t('cart.selected') }}
            </span>
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
              :selected="cartSelected.has(item.bienTheId)"
              @decrease="updateQty(item.bienTheId, -1)"
              @increase="updateQty(item.bienTheId, 1)"
              @remove="removeFromCart(item.bienTheId)"
              @toggle="toggleCartItem(item.bienTheId)"
            />
          </div>

          <CartSummary
            v-if="cartCount > 0"
            :cart-count="cartSelectedCount"
            :cart-total="cartSelectedTotal"
            @checkout="openCheckout"
          />
        </div>
      </Transition>
    </div>

    <AppFooter @open-register="openRegister" />
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

<style scoped>
/* ── Hiệu ứng nút 3D (giống trang flash sale) ── */

/* Nút hồng 3D */
.btn-3d-pink {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: none;
  border-radius: 10px;
  padding: 10px 20px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  background: linear-gradient(180deg, #ec4899 0%, #db2777 50%, #be185d 100%);
  color: #ffffff;
  box-shadow: 0 4px 0 #9d174d, 0 6px 16px rgba(168, 27, 93, 0.35);
  border-bottom: 4px solid #9d174d;
  transition: all 0.15s ease;
  font-family: inherit;
}
.btn-3d-pink:hover:not(:disabled) {
  background: linear-gradient(180deg, #f43f5e 0%, #ec4899 50%, #db2777 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 0 #9d174d, 0 8px 20px rgba(168, 27, 93, 0.4);
}
.btn-3d-pink:active:not(:disabled) {
  transform: translateY(3px);
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.2);
  border-bottom-width: 0;
  padding-bottom: 14px;
}
.btn-3d-pink:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

/* Nút xanh 3D */
.btn-3d-blue {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: none;
  border-radius: 10px;
  padding: 10px 20px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  background: linear-gradient(180deg, #3b82f6 0%, #2563eb 50%, #1d4ed8 100%);
  color: #ffffff;
  box-shadow: 0 4px 0 #1e40af, 0 6px 16px rgba(37, 99, 235, 0.35);
  border-bottom: 4px solid #1e40af;
  transition: all 0.15s ease;
  font-family: inherit;
}
.btn-3d-blue:hover:not(:disabled) {
  background: linear-gradient(180deg, #60a5fa 0%, #3b82f6 50%, #2563eb 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 0 #1e40af, 0 8px 20px rgba(37, 99, 235, 0.4);
}
.btn-3d-blue:active:not(:disabled) {
  transform: translateY(3px);
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.2);
  border-bottom-width: 0;
  padding-bottom: 14px;
}
.btn-3d-blue:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

/* Nút xám ghost 3D */
.btn-3d-ghost {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: none;
  border-radius: 10px;
  padding: 10px 20px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  background: transparent;
  color: #6b7280;
  border: 2px solid #d1d5db;
  box-shadow: none;
  transition: all 0.15s ease;
  font-family: inherit;
}
.btn-3d-ghost:hover {
  background: #fdf2f8;
  border-color: #f9a8d4;
  color: #be185d;
}

/* ── Hero fade ── */
.hero-fade-enter-active,
.hero-fade-leave-active {
  transition: opacity 0.7s ease-in-out;
}
.hero-fade-enter-from,
.hero-fade-leave-to {
  opacity: 0;
}
</style>
