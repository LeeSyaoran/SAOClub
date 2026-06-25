<script setup>
import LoginForm from "./components/auth/LoginForm.vue";
// ── Import các thư viện Vue 3 cần thiết ──────────────────────────────────────
import { ref, computed, reactive, onMounted, onBeforeUnmount } from "vue";

// Import store xác thực (isAdmin: true/false)
import { AuthStore } from "./stores/index.js";

// Import services
import * as SanPhamService  from "./Service/SanPhamService.js";
import * as DanhMucService  from "./Service/DanhMucService.js";
import * as KhachHangService from "./Service/KhachHangService.js";
import * as KhuyenMaiService from "./Service/KhuyenMaiService.js";
import * as DonHangService  from "./Service/DonHangService.js";

// Import các component trang
import AdminPage     from "./pages/AdminPage.vue";
import NavBar        from "./components/layout/NavBar.vue";
import AppFooter     from "./components/layout/Footer.vue";
import ProductFilter from "./components/product/ProductFilter.vue";
import ProductDetail from "./components/product/ProductDetail.vue";

// ── State & Store ─────────────────────────────────────────────────────────────

// Lấy object auth từ store (reactive)
const auth = AuthStore;

// Theo dõi fragment URL hiện tại (ví dụ: "#admin")
const currentHash = ref(window.location.hash);

// Computed: kiểm tra có đang ở route admin không
const isAdminHash = computed(() => currentHash.value === "#admin");

// ── Dữ liệu sản phẩm ─────────────────────────────────────────────────────────

const products      = ref([]);   // Danh sách toàn bộ sản phẩm từ API
const loading       = ref(false);// Đang tải dữ liệu hay không
const error         = ref(null); // Thông báo lỗi nếu có
const searchQuery   = ref("");
const selectedSort  = ref("default");

// ── Lọc nâng cao (ProductFilter) ──────────────────────────────────────────────
const showAdvFilter = ref(false);
const advFilter     = reactive({ brands: [], priceMin: null, priceMax: null, category: null });

// Danh mục thực từ API — dùng để map chip → danhMucId chính xác
const apiCats = ref([]);
const fetchApiCats = async () => {
  apiCats.value = await DanhMucService.getAll().catch(() => []);
};
const showLogin = ref(false);

const openLogin = () => {
  showLogin.value = true;
};

const closeLogin = () => {
  showLogin.value = false;
};

// Danh sách thương hiệu duy nhất từ data sản phẩm đã load
const allBrands = computed(() =>
  [...new Set(products.value.map(p => p.tenThuongHieu).filter(Boolean))]
);

// Danh sách danh mục duy nhất từ data sản phẩm đã load
const allCategories = computed(() => {
  const seen = new Set();
  return products.value
    .filter(p => p.danhMucId && p.tenDanhMuc && !seen.has(p.danhMucId) && seen.add(p.danhMucId))
    .map(p => ({ id: p.danhMucId, tenDanhMuc: p.tenDanhMuc }));
});

const onAdvFilterChange = (f) => {
  advFilter.brands   = f.brands;
  advFilter.priceMin = f.priceMin;
  advFilter.priceMax = f.priceMax;
  advFilter.category = f.category;
};

// ── Deal section state ────────────────────────────────────────────────────────

const activeTab        = ref("deal");
const activeFilter     = ref("Tất cả"); // chip đang active
const activeCatId      = ref(null);     // danhMucId từ sidebar
const activeSidebarCat = ref(null);     // full object sidebar cat (có keywords cho fallback)

const dealFilters = [
  "Tất cả",
  "Laptop Gaming",
  "Văn phòng - Học tập",
  "MacBook - Cao cấp",
  "Đồ họa kỹ thuật - AI"
];

// Sidebar: keywords dùng cả cho apiCats lookup VÀ phanLoaiTags fallback
const sidebarCatsBase = [
  { icon: "💻", name: "Laptop Văn Phòng / Sinh Viên",  keywords: ["van_phong", "sinh_vien", "văn phòng", "sinh viên"] },
  { icon: "🎮", name: "Laptop Gaming Cấu Hình Cao",    keywords: ["gaming"] },
  { icon: "⚡", name: "Laptop Đồ Họa - Kỹ Thuật",     keywords: ["do_hoa", "ky_thuat", "đồ họa", "kỹ thuật"] },
  { icon: "🍎", name: "MacBook / Apple Silicon",        keywords: ["macbook", "apple"] },
  { icon: "⭐", name: "Laptop Cũ Giá Rẻ Chính Hãng",  keywords: ["cu", "gia_re", "cũ", "rẻ"] },
  { icon: "🔧", name: "Linh Kiện & Nâng Cấp RAM/SSD", keywords: ["linh_kien", "ram", "ssd", "linh kiện"] },
];

// Map mỗi sidebar item → catId thực từ apiCats (null nếu chưa có trong DB)
const sidebarCats = computed(() =>
  sidebarCatsBase.map(sc => {
    const matched = apiCats.value.find(c =>
      sc.keywords.some(kw => c.tenDanhMuc?.toLowerCase().includes(kw))
    );
    return { ...sc, catId: matched?.id ?? null };
  })
);

// Click sidebar → lưu cả object (cần keywords cho fallback), xóa chip filter, scroll
const selectSidebarCat = (cat) => {
  activeSidebarCat.value = cat;
  activeCatId.value      = cat.catId;
  activeFilter.value     = "Tất cả";
  advFilter.brands   = [];
  advFilter.priceMin = null;
  advFilter.priceMax = null;
  advFilter.category = null;
  const el = document.getElementById("deal-section");
  if (el) el.scrollIntoView({ behavior: "smooth" });
};

// Click chip → lọc theo chip, xóa sidebar filter
const selectChip = (f) => {
  activeFilter.value     = f;
  activeCatId.value      = null;
  activeSidebarCat.value = null;
};

// Mapping chip → keywords (bao gồm cả phan_loai_tags slug VÀ text tiếng Việt)
const CHIP_KEYWORDS = {
  "Laptop Gaming":         ["gaming"],
  "Văn phòng - Học tập":  ["van_phong", "sinh_vien", "văn phòng", "sinh viên"],
  "MacBook - Cao cấp":    ["macbook", "apple"],
  "Đồ họa kỹ thuật - AI": ["do_hoa", "ky_thuat", "đồ họa", "kỹ thuật"],
};

// Map sanPhamId → số lượng biến thể (để card biết hiển thị "Từ X.XXXđ" hay không)
const variantCountMap = computed(() => {
  const map = new Map();
  products.value.forEach(p => map.set(p.sanPhamId, (map.get(p.sanPhamId) || 0) + 1));
  return map;
});

// ── Computed: Lọc + deduplicate (1 card/sản phẩm) + sắp xếp ─────────────────
const filteredProducts = computed(() => {
  const query = searchQuery.value.trim().toLowerCase();

  const filtered = products.value.filter((product) => {
    const name  = product.tenSanPham?.toLowerCase()    || "";
    const brand = product.tenThuongHieu?.toLowerCase() || "";
    const cat   = product.tenDanhMuc?.toLowerCase()    || "";
    const desc  = product.moTa?.toLowerCase()          || "";
    const tags  = (product.phanLoaiTags || "").split(",").map(t => t.trim()).filter(Boolean);

    const matchesKw = (keywords) =>
      keywords.some(kw => tags.includes(kw) || cat.includes(kw) || brand.includes(kw) || name.includes(kw));

    if (query && !name.includes(query) && !brand.includes(query) && !desc.includes(query)) return false;

    if (activeSidebarCat.value !== null) {
      if (!matchesKw(activeSidebarCat.value.keywords)) return false;
    } else if (activeFilter.value !== "Tất cả") {
      const keywords = CHIP_KEYWORDS[activeFilter.value] || [];
      if (!matchesKw(keywords)) return false;
    }

    if (advFilter.brands.length > 0 && !advFilter.brands.includes(product.tenThuongHieu)) return false;

    const price = Number(product.giaBan) || 0;
    if (advFilter.priceMin !== null && price < advFilter.priceMin) return false;
    if (advFilter.priceMax !== null && advFilter.priceMax !== Infinity && price > advFilter.priceMax) return false;

    if (advFilter.category !== null && product.danhMucId !== advFilter.category) return false;

    return true;
  });

  // Deduplicate: 1 card / sanPhamId — lấy biến thể giá thấp nhất làm đại diện
  const deduped = [...filtered.reduce((map, p) => {
    const ex = map.get(p.sanPhamId);
    if (!ex || Number(p.giaBan) < Number(ex.giaBan)) map.set(p.sanPhamId, p);
    return map;
  }, new Map()).values()];

  return deduped.sort((a, b) => {
    if (selectedSort.value === "price-asc")  return (Number(a.giaBan) || 0) - (Number(b.giaBan) || 0);
    if (selectedSort.value === "price-desc") return (Number(b.giaBan) || 0) - (Number(a.giaBan) || 0);
    return 0;
  });
});

// ── Chi tiết sản phẩm ────────────────────────────────────────────────────────
const selectedProduct = ref(null);
const openProduct  = (p)  => { selectedProduct.value = p; };
const closeProduct = ()   => { selectedProduct.value = null; };

// ── Giỏ hàng ─────────────────────────────────────────────────────────────────

const cart     = ref([]);          // Mảng sản phẩm trong giỏ
const showCart = ref(false);       // Hiển thị/ẩn panel giỏ hàng

// Tổng số lượng sản phẩm trong giỏ
const cartCount = computed(() =>
  cart.value.reduce((total, item) => total + item.quantity, 0)
);

// Tổng tiền tạm tính
const cartTotal = computed(() =>
  cart.value.reduce((total, item) => total + (item.quantity || 0) * (item.giaBan || 0), 0)
);

// Bật/tắt hiển thị giỏ hàng
const toggleCart = () => { showCart.value = !showCart.value; };

// Nhận từ khoá tìm kiếm từ NavBar emit lên
const handleSearch = (q) => { searchQuery.value = q; };

// Xoá 1 sản phẩm khỏi giỏ theo bienTheId
const removeFromCart = (bienTheId) => {
  cart.value = cart.value.filter((item) => item.bienTheId !== bienTheId);
};

// Định dạng tiền tệ VND
const formatPrice = (value) => {
  if (value == null) return "Liên hệ";
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(value);
};

// ── API: Lấy danh sách sản phẩm ──────────────────────────────────────────────
const fetchProducts = async () => {
  loading.value = true;
  error.value   = null;
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
  const existing = cart.value.find((item) => item.bienTheId === product.bienTheId);
  if (existing) {
    existing.quantity += 1;
  } else {
    cart.value.push({ ...product, quantity: 1 });
  }
};

// ── Checkout (Thanh toán) ─────────────────────────────────────────────────────

const showCheckout    = ref(false); // Hiển thị modal thanh toán
const checkoutSuccess = ref(false); // Đặt hàng thành công chưa
const checkoutLoading = ref(false); // Đang xử lý API
const checkoutError   = ref('');    // Thông báo lỗi khi checkout
const checkoutOrderId = ref(null);  // ID đơn hàng sau khi đặt xong
const allCustomers    = ref([]);    // Cache danh sách khách hàng
const allPromos       = ref([]);    // Cache danh sách khuyến mãi
const foundCustomer   = ref(null);  // Khách hàng tìm thấy qua SĐT
const appliedPromo    = ref(null);  // Khuyến mãi đã áp dụng
const promoMsg        = ref('');    // Thông báo kết quả áp dụng mã

// Form thông tin đặt hàng (reactive để Vue theo dõi thay đổi)
const checkoutForm = reactive({
  soDienThoai:          '', // SĐT tìm kiếm khách hàng
  hoTen:                '', // Họ tên khách hàng
  email:                '', // Email
  nguoiNhan:            '', // Tên người nhận hàng
  sdtNguoiNhan:         '', // SĐT người nhận
  diaChiGiaoHangText:   '', // Địa chỉ giao hàng
  maKhuyenMai:          '', // Mã khuyến mãi nhập vào
});

// Phí vận chuyển: miễn phí nếu đơn từ 300k
const phiVanChuyen = computed(() => cartTotal.value >= 300000 ? 0 : 30000);

// Tính giảm giá từ mã khuyến mãi
const checkoutGiamGia = computed(() => {
  const p = appliedPromo.value;
  if (!p) return 0;
  if (p.loai === 'percent') {
    // Giảm theo % nhưng không vượt quá giaTriToiDa
    let d = cartTotal.value * Number(p.giaTri) / 100;
    if (p.giaTriToiDa) d = Math.min(d, Number(p.giaTriToiDa));
    return d;
  }
  return Number(p.giaTri) || 0; // Giảm theo số tiền cố định
});

// Tổng tiền thanh toán cuối cùng
const checkoutTotal = computed(() =>
  Math.max(0, cartTotal.value + phiVanChuyen.value - checkoutGiamGia.value)
);

// Mở modal thanh toán và load dữ liệu cần thiết
const openCheckout = async () => {
  if (cart.value.length === 0) return; // Không mở nếu giỏ trống
  checkoutSuccess.value = false;
  checkoutError.value   = '';
  promoMsg.value        = '';
  appliedPromo.value    = null;
  foundCustomer.value   = null;
  // Reset toàn bộ form
  Object.keys(checkoutForm).forEach(k => { checkoutForm[k] = ''; });
  // Nếu chưa có cache khách hàng và khuyến mãi thì fetch
  if (!allCustomers.value.length) {
    [allCustomers.value, allPromos.value] = await Promise.all([
      KhachHangService.getAll().catch(() => []),
      KhuyenMaiService.getAll().catch(() => []),
    ]);
  }
  showCart.value     = false; // Đóng giỏ hàng trước khi mở modal
  showCheckout.value = true;
};

// Tìm khách hàng theo số điện thoại
const lookupCustomer = () => {
  const c = allCustomers.value.find(
    (x) => x.soDienThoai === checkoutForm.soDienThoai.trim()
  );
  foundCustomer.value = c || null;
  if (c) {
    // Tự điền thông tin nếu tìm thấy khách hàng
    checkoutForm.hoTen              = c.hoTen;
    checkoutForm.email              = c.email || '';
    checkoutForm.nguoiNhan          = c.hoTen;
    checkoutForm.sdtNguoiNhan       = c.soDienThoai;
    checkoutForm.diaChiGiaoHangText = c.diaChi || '';
  }
};

// Kiểm tra và áp dụng mã khuyến mãi
const applyPromo = () => {
  const code = checkoutForm.maKhuyenMai.trim().toUpperCase();
  if (!code) { appliedPromo.value = null; promoMsg.value = ''; return; }
  const p = allPromos.value.find(
    (x) => x.maKhuyenMai?.toUpperCase() === code && x.trangThai === 'active'
  );
  if (p) {
    appliedPromo.value = p;
    promoMsg.value     = `Áp dụng thành công: ${p.tenKhuyenMai}`;
  } else {
    appliedPromo.value = null;
    promoMsg.value     = 'Mã khuyến mãi không hợp lệ hoặc hết hạn';
  }
};

// Gửi đơn hàng lên API
const placeOrder = async () => {
  checkoutError.value   = '';
  checkoutLoading.value = true;
  try {
    let khachHangId = foundCustomer.value?.khachHangId;

    // Nếu không tìm thấy khách hàng → tạo mới
    if (!khachHangId) {
      const custBody = {
        hoTen:        checkoutForm.hoTen,
        soDienThoai:  checkoutForm.soDienThoai,
        email:        checkoutForm.email,
        diaChi:       checkoutForm.diaChiGiaoHangText || 'Chua cap nhat',
        loaiKhach:    'ca_nhan',
        diemTichLuy:  0,
        trangThai:    'active',
      };
      const r = await KhachHangService.save(null, custBody);
      if (!r.ok) throw new Error(`Lỗi tạo khách hàng: ${r.status} ${await r.text()}`);
      const newC  = await r.json();
      khachHangId = newC.khachHangId;
      allCustomers.value = await KhachHangService.getAll().catch(() => []);
    }

    // Tạo đơn hàng chính
    const orderBody = {
      khachHangId,
      nguoiNhan:          checkoutForm.nguoiNhan,
      sdtNguoiNhan:       checkoutForm.sdtNguoiNhan,
      diaChiGiaoHangText: checkoutForm.diaChiGiaoHangText,
      khuyenMaiId:        appliedPromo.value?.khuyenMaiId ?? null,
      tongTien:           cartTotal.value,
      giamGia:            checkoutGiamGia.value,
      phiVanChuyen:       phiVanChuyen.value,
      thanhTien:          checkoutTotal.value,
      ngayDat:            new Date().toISOString().slice(0, 19),
      trangThaiDonHang:   'pending',
      trangThaiThanhToan: 'unpaid',
      kenhBan:            'online',
    };
    const orderRes = await DonHangService.create(orderBody);
    if (!orderRes.ok)
      throw new Error(`Lỗi đặt hàng: ${orderRes.status} ${await orderRes.text()}`);
    const createdOrder = await orderRes.json();
    const donHangId    = createdOrder.id;

    // Thêm từng sản phẩm vào chi tiết đơn hàng
    for (const item of cart.value) {
      const itemRes = await DonHangService.addChiTiet({
        donHangId,
        bienTheId:   item.bienTheId,
        soLuong:     item.quantity,
        donGia:      item.giaBan,
        giamGiaDong: 0,
      });
      if (!itemRes.ok)
        throw new Error(`Lỗi chi tiết đơn hàng: ${itemRes.status}`);
    }

    // Đặt hàng thành công: lưu mã đơn, xoá giỏ
    checkoutOrderId.value = donHangId;
    checkoutSuccess.value = true;
    cart.value = [];
  } catch (e) {
    checkoutError.value = e.message;
  } finally {
    checkoutLoading.value = false;
  }
};

// ── Routing bằng hash URL ─────────────────────────────────────────────────────

// Cập nhật currentHash khi URL fragment thay đổi
function onHashChange() { currentHash.value = window.location.hash; }

// Quay về trang chủ
function goHome() { window.location.hash = ""; }

// Chuyển sang trang admin
function goAdmin() { window.location.hash = "#admin"; }

// ── Lifecycle hooks ───────────────────────────────────────────────────────────
onMounted(() => {
  window.addEventListener("hashchange", onHashChange);
  fetchProducts();
  fetchApiCats();
});
onBeforeUnmount(() => {
  window.removeEventListener("hashchange", onHashChange); // Dọn dẹp listener
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
    <section v-else-if="isAdminHash && !auth.isAdmin"
             class="d-flex align-items-center justify-content-center"
             style="min-height:100vh; background:#171717;">
      <div class="text-center text-light d-flex flex-column align-items-center gap-3">
        <div style="font-size:3rem;">🔒</div>
        <h2 class="fw-black mb-0" style="font-size:1.5rem;">Quyền truy cập bị từ chối</h2>
        <p class="text-secondary mb-0">Bạn cần đăng nhập với tài khoản admin để xem trang quản trị.</p>
        <!-- Nút quay về trang chủ -->
        <button class="btn btn-warning fw-bold rounded-pill px-4 py-2" @click="goHome">
          Quay về trang chủ
        </button>
      </div>
    </section>

    <!-- ══════════════════════════════════════════════════════
        TRANG KHÁCH HÀNG — hiển thị khi không có #admin
    ══════════════════════════════════════════════════════ -->
    <div v-else style="min-height:100vh; background:#171717; color:#e5e7eb; font-family:'Inter','Segoe UI',sans-serif;">

      <!-- Header / NavBar — nhận cartCount và xử lý các sự kiện -->
      <NavBar
          :cart-count="cartCount"
          @toggle-cart="toggleCart"
          @search="handleSearch"
          @open-admin="goAdmin"
          @open-login="openLogin"
      />

      <!-- Dải ticker chạy ngang (thông báo khuyến mãi) -->
      <div class="overflow-x-auto" style="background:#0f0f0f; border-bottom:1px solid #1f1f1f; padding:8px 16px;">
        <div class="d-flex gap-4 small fw-bold text-secondary" style="white-space:nowrap; width:max-content;">
          <span class="text-warning text-uppercase" style="letter-spacing:0.05em;">
            🔥 TUẦN LỄ LAPTOP GAMING &amp; ĐỒ HỌA
          </span>
          <span>MACBOOK PRO M4 - Thiết lập chuẩn mực AI mới</span>
          <span>SẮM LAPTOP LENOVO AI - Nhận bộ quà tặng Cực High</span>
          <span>ASUS ROG STRIX SCAR - Giảm giá sốc cho sinh viên</span>
        </div>
      </div>

      <!-- ── Nội dung trang chính ── -->
      <div class="container-xl py-3">

        <!-- ── Hero Grid: Sidebar | Banner | Info Panel ── -->
        <div class="row g-3 mb-3">

          <!-- Sidebar danh mục (chỉ hiện trên màn lớn) -->
          <div class="col-xl-2 d-none d-xl-block">
            <div class="rounded-3 p-1 d-flex flex-column gap-1 h-100"
                 style="background:#1a1a1a; border:1px solid #2a2a2a;">
              <a v-for="cat in sidebarCats"
                 :key="cat.name"
                 href="#"
                 class="d-flex align-items-center justify-content-between px-3 py-2 rounded-2 text-decoration-none small fw-bold"
                 style="font-size:12px; transition:all 0.15s;"
                 :style="activeSidebarCat && activeSidebarCat.name === cat.name
                   ? 'background:#252525; color:#facc15;'
                   : 'color:#6b7280;'"
                 @mouseenter="e => { e.currentTarget.style.background='#252525'; e.currentTarget.style.color='#facc15'; }"
                 @mouseleave="e => {
                   const isActive = activeSidebarCat && activeSidebarCat.name === cat.name;
                   e.currentTarget.style.background = isActive ? '#252525' : '';
                   e.currentTarget.style.color      = isActive ? '#facc15' : '';
                 }"
                 @click.prevent="selectSidebarCat(cat)">
                <span class="d-flex align-items-center gap-2">
                  <span style="font-size:13px;">{{ cat.icon }}</span>
                  {{ cat.name }}
                </span>
                <span style="color:#4b5563;">›</span>
              </a>
            </div>
          </div>

          <!-- Banner trung tâm + 3 sản phẩm nổi bật -->
          <div class="col-12 col-xl-7">
            <!-- Banner ảnh chính -->
            <div class="position-relative rounded-3 overflow-hidden mb-2" style="height:280px;">
              <img
                src="https://images.unsplash.com/photo-1603302576837-37561b2e2302?q=80&w=800"
                alt="Laptop Premium Promotion"
                class="w-100 h-100"
                style="object-fit:cover;"
              />
              <!-- Overlay chữ nổi trên banner -->
              <div class="position-absolute bottom-0 start-0 p-4 w-100"
                   style="background:linear-gradient(to top, rgba(0,0,0,0.85) 0%, transparent 100%);">
                <h2 class="text-white fw-black mb-1" style="font-size:1.3rem; text-shadow:0 2px 8px rgba(0,0,0,0.8);">
                  LAPTOP AI NEXT-GEN 2026
                </h2>
                <p class="text-warning small fw-bold mb-0">
                  Ưu đãi mùa tựu trường | Trả góp 0% + Tặng Balo Gaming cao cấp
                </p>
              </div>
            </div>

            <!-- 3 thẻ sản phẩm nổi bật nhỏ bên dưới banner -->
            <div class="row g-2">
              <template v-if="loading">
                <div class="col-4">
                  <div class="p-3 rounded-2 text-secondary small text-center"
                       style="background:#1a1a1a; border:1px solid #2a2a2a;">
                    Đang tải...
                  </div>
                </div>
              </template>
              <template v-else-if="products.length">
                <div v-for="p in products.slice(0, 3)" :key="p.sanPhamId" class="col-4">
                  <div class="p-2 rounded-2 small"
                       style="background:#1a1a1a; border:1px solid #2a2a2a;">
                    <p class="fw-bold mb-1 text-light"
                       style="font-size:11px; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; line-clamp:2; -webkit-box-orient:vertical;">
                      {{ p.tenSanPham }}
                    </p>
                    <p class="text-warning fw-black mb-0" style="font-size:12px;">
                      {{ formatPrice(p.giaBan) }}
                    </p>
                  </div>
                </div>
              </template>
              <template v-else>
                <div class="col-12">
                  <div class="p-2 rounded-2 text-secondary small text-center"
                       style="background:#1a1a1a; border:1px solid #2a2a2a;">
                    Chưa có sản phẩm để hiển thị
                  </div>
                </div>
              </template>
            </div>
          </div>

          <!-- Info panel bên phải -->
          <div class="col-12 col-xl-3">
            <div class="rounded-3 p-3 h-100 d-flex flex-column gap-3"
                 style="background:#1a1a1a; border:1px solid #2a2a2a;">
              <!-- Logo thương hiệu -->
              <div class="d-flex align-items-center gap-2">
                <div class="rounded-circle d-flex align-items-center justify-content-center fw-black flex-shrink-0"
                     style="width:44px; height:44px; background:#facc15; color:#111; font-size:0.8rem;">
                  SAO
                </div>
                <div>
                  <h4 class="fw-black mb-0 text-white" style="font-size:0.82rem; line-height:1.3;">
                    Hệ thống phân phối LAPTOP SAOPHONE
                  </h4>
                  <p class="mb-0 text-secondary" style="font-size:10px;">Hệ thống công nghệ Đen &amp; Vàng</p>
                </div>
              </div>
              <!-- Các liên kết ưu đãi -->
              <div class="d-flex flex-column gap-1">
                <a v-for="link in [
                  '🎓 Ưu đãi độc quyền học sinh sinh viên',
                  '🔥 Deal sốc linh kiện máy tính đồ họa',
                  '💻 Laptop Gaming cấu hình cực mạnh',
                  '🔄 Thu cũ đổi mới trợ giá lên tới 3 triệu',
                ]" :key="link" href="#"
                   class="d-block text-decoration-none fw-semibold text-secondary p-2 rounded-2 small"
                   style="font-size:11px; transition:background 0.15s;"
                   @mouseenter="e => { e.target.style.background='#252525'; e.target.style.color='#facc15'; }"
                   @mouseleave="e => { e.target.style.background=''; e.target.style.color=''; }">
                  {{ link }}
                </a>
              </div>
              <!-- Banner CTA -->
              <div class="mt-auto text-center fw-black py-2 rounded-2 small"
                   style="background:linear-gradient(135deg,#facc15,#f59e0b); color:#111; font-size:11px; letter-spacing:0.06em;">
                MÙA THI CỬ - GIẢM GIÁ ĐẾN 50%
              </div>
            </div>
          </div>

        </div><!-- /hero-grid row -->

        <!-- ── Deal Section: tabs + filter + danh sách sản phẩm ── -->
        <section id="deal-section" class="mt-3">

          <!-- Tabs: DEAL SỐC | HOT TREND | MÁY MỚI -->
          <div class="d-flex gap-2 mb-3 border-bottom pb-0"
               style="border-color:#2a2a2a!important;">
            <button
              v-for="tab in [
                { id:'deal', label:'🔥 DEAL SỐC LAPTOP' },
                { id:'hot',  label:'LAPTOP HOT TREND 2026' },
                { id:'new',  label:'MÁY MỚI CẬP BẾN' },
              ]" :key="tab.id"
              class="btn btn-sm fw-black px-3 pb-2 rounded-0 border-0"
              style="font-size:12px; letter-spacing:0.04em; border-bottom:3px solid transparent!important;"
              :class="activeTab === tab.id ? 'text-warning' : 'text-secondary'"
              :style="activeTab === tab.id
                ? 'border-bottom:3px solid #facc15!important;'
                : ''"
              @click="activeTab = tab.id">
              {{ tab.label }}
            </button>
          </div>

          <!-- Thanh filter + sắp xếp -->
          <div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-2">
            <!-- Chip filter theo loại laptop -->
            <div class="d-flex flex-wrap gap-2">
              <button
                v-for="f in dealFilters" :key="f"
                class="btn btn-sm fw-bold"
                style="font-size:11px; border-radius:999px;"
                :class="activeFilter === f && activeCatId === null
                  ? 'btn-warning text-dark'
                  : 'btn-outline-secondary text-secondary'"
                @click="selectChip(f)">
                {{ f }}
              </button>
            </div>
            <!-- Nút lọc nâng cao + select sắp xếp -->
            <div class="d-flex align-items-center gap-2">
              <button
                class="btn btn-sm fw-bold"
                style="font-size:11px; border-radius:999px;"
                :class="showAdvFilter ? 'btn-warning text-dark' : 'btn-outline-secondary text-secondary'"
                @click="showAdvFilter = !showAdvFilter">
                🔍 Lọc nâng cao
                <span v-if="advFilter.brands.length || advFilter.priceMin || advFilter.category"
                      class="badge bg-danger ms-1"
                      style="font-size:9px;">
                  {{ advFilter.brands.length + (advFilter.priceMin ? 1 : 0) + (advFilter.category ? 1 : 0) }}
                </span>
              </button>
              <select v-model="selectedSort"
                      class="form-select form-select-sm"
                      style="width:auto; background:#1f1f1f; border-color:#3f3f3f; color:#e5e7eb; font-size:12px;"
                      aria-label="Sắp xếp">
                <option value="default">Mặc định</option>
                <option value="price-asc">Giá thấp → cao</option>
                <option value="price-desc">Giá cao → thấp</option>
              </select>
            </div>
          </div>

          <!-- Panel lọc nâng cao (thương hiệu, khoảng giá, danh mục) -->
          <div v-show="showAdvFilter"
               class="p-3 mb-3 rounded-3"
               style="background:#1a1a1a; border:1px solid #2a2a2a;">
            <ProductFilter
              :brands="allBrands"
              :categories="allCategories"
              @change="onAdvFilterChange"
            />
          </div>

          <!-- Trạng thái loading / lỗi / trống -->
          <div v-if="loading" class="text-secondary text-center py-4 small">
            Đang tải dữ liệu sản phẩm...
          </div>
          <div v-else-if="error" class="alert alert-danger small py-2">{{ error }}</div>
          <div v-else-if="filteredProducts.length === 0" class="text-secondary text-center py-4 small">
            Không có sản phẩm nào phù hợp.
          </div>

          <!-- Lưới sản phẩm -->
          <div v-else class="row g-3">
            <article
              v-for="product in filteredProducts"
              :key="product.sanPhamId"
              class="col-6 col-md-4 col-lg-3 col-xl-2">
              <!-- Thẻ sản phẩm -->
              <div class="card h-100 border-secondary"
                   style="background:#1a1a1a; border-radius:14px; overflow:hidden; transition:transform 0.15s, box-shadow 0.15s; cursor:pointer;"
                   @mouseenter="e => { e.currentTarget.style.transform='translateY(-3px)'; e.currentTarget.style.boxShadow='0 8px 24px rgba(0,0,0,0.4)'; }"
                   @mouseleave="e => { e.currentTarget.style.transform=''; e.currentTarget.style.boxShadow=''; }"
                   @click="openProduct(product)">

                <!-- Ảnh sản phẩm -->
                <div class="position-relative" style="background:#111; height:160px;">
                  <img
                    v-if="product.hinhAnhChinh"
                    :src="product.hinhAnhChinh"
                    :alt="product.tenSanPham"
                    class="w-100 h-100"
                    style="object-fit:contain; padding:8px;"
                  />
                  <!-- Placeholder nếu không có ảnh -->
                  <div v-else
                       class="w-100 h-100 d-flex align-items-center justify-content-center"
                       style="font-size:2.5rem;">
                    💻
                  </div>
                  <!-- Badge trạng thái: Còn hàng / Hết hàng -->
                  <span
                    class="badge position-absolute top-0 start-0 m-2"
                    style="font-size:10px;"
                    :class="product.trangThai === 'active' ? 'bg-success' : 'bg-secondary'">
                    {{ product.trangThai === 'active' ? 'Còn hàng' : 'Hết hàng' }}
                  </span>
                </div>

                <!-- Thông tin sản phẩm -->
                <div class="card-body p-2 d-flex flex-column gap-1">
                  <h3 class="fw-bold text-light mb-0"
                      style="font-size:11px; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; line-clamp:2; -webkit-box-orient:vertical;">
                    {{ product.tenSanPham }}
                  </h3>
                  <p class="mb-0 text-secondary" style="font-size:10px;">
                    {{ product.tenThuongHieu || product.tenDanhMuc }}
                  </p>
                  <p class="mb-0 text-warning fw-black" style="font-size:13px;">
                    <span v-if="variantCountMap.get(product.sanPhamId) > 1" class="text-secondary fw-normal" style="font-size:9px;">Từ </span>{{ formatPrice(product.giaBan) }}
                  </p>
                  <p class="mb-0 text-secondary" style="font-size:10px;">🚚 Giao nhanh 2H</p>
                  <!-- Tags phân loại — hiển thị tên tiếng Việt từ phanLoaiTen -->
                  <div v-if="product.phanLoaiTen" class="d-flex flex-wrap gap-1 mt-1">
                    <span
                      v-for="tag in product.phanLoaiTen.split(',')"
                      :key="tag"
                      class="badge"
                      style="font-size:9px; background:#2a2200; color:#facc15; border:1px solid #3d3000;">
                      {{ tag.trim() }}
                    </span>
                  </div>
                  <!-- Nút thêm vào giỏ — disabled nếu hết hàng -->
                  <button
                    class="btn btn-sm w-100 fw-bold mt-1"
                    style="font-size:11px; border-radius:8px;"
                    :class="product.trangThai === 'active' ? 'btn-warning text-dark' : 'btn-secondary'"
                    :disabled="product.trangThai !== 'active'"
                    @click.stop="addToCart(product)">
                    🛒 Thêm vào giỏ
                  </button>
                </div>

              </div><!-- /card -->
            </article>
          </div><!-- /product grid -->

        </section><!-- /deal section -->

        <!-- ── Panel giỏ hàng (slide-in từ phải) ── -->
        <div v-if="showCart"
             class="position-fixed top-0 end-0 h-100 d-flex flex-column"
             style="width:360px; background:#111; border-left:1px solid #2a2a2a; z-index:500; box-shadow:-8px 0 32px rgba(0,0,0,0.5);">

          <!-- Header giỏ hàng -->
          <div class="d-flex justify-content-between align-items-center p-3"
               style="border-bottom:1px solid #2a2a2a;">
            <h3 class="mb-0 fw-bold text-white" style="font-size:1rem;">Giỏ hàng</h3>
            <!-- Nút đóng giỏ hàng -->
            <button class="btn btn-sm btn-outline-secondary rounded-circle"
                    style="width:32px; height:32px; padding:0;"
                    @click="toggleCart">✕</button>
          </div>

          <!-- Nội dung giỏ hàng -->
          <div v-if="cartCount === 0"
               class="flex-grow-1 d-flex align-items-center justify-content-center text-secondary small">
            Chưa có sản phẩm nào trong giỏ.
          </div>
          <div v-else class="flex-grow-1 d-flex flex-column p-3 overflow-y-auto gap-2">

            <!-- Từng sản phẩm trong giỏ -->
            <div v-for="item in cart" :key="item.sanPhamId"
                 class="d-flex justify-content-between align-items-start gap-2 p-2 rounded-2"
                 style="background:#1a1a1a; border:1px solid #2a2a2a;">
              <div class="flex-grow-1 min-width-0">
                <div class="fw-bold text-light small">{{ item.tenSanPham }}</div>
                <div class="text-secondary" style="font-size:11px;">
                  {{ item.quantity }} × {{ formatPrice(item.giaBan) }}
                </div>
                <div class="text-warning fw-bold" style="font-size:12px;">
                  {{ formatPrice(item.quantity * item.giaBan) }}
                </div>
              </div>
              <!-- Nút xoá khỏi giỏ -->
              <button class="btn btn-sm btn-outline-danger flex-shrink-0"
                      style="font-size:10px; padding:2px 8px;"
                      @click="removeFromCart(item.bienTheId)">Xóa</button>
            </div>

            <!-- Tổng tạm tính -->
            <div class="d-flex justify-content-between align-items-center pt-2 mt-1"
                 style="border-top:1px solid #2a2a2a;">
              <span class="text-secondary small">Tổng tạm tính</span>
              <strong class="text-warning">{{ formatPrice(cartTotal) }}</strong>
            </div>

            <!-- Nút thanh toán -->
            <button class="btn btn-warning btn-sm fw-black w-100 mt-1"
                    style="border-radius:10px;"
                    @click="openCheckout">
              Thanh toán →
            </button>
          </div>

        </div><!-- /cart panel -->

      </div><!-- /container-xl -->

      <!-- Footer -->
      <AppFooter />

    </div><!-- /trang khách hàng -->

  </div><!-- /root -->

  <!-- ══════════════════════════════════════════════════════
      CHECKOUT MODAL — Hiển thị form đặt hàng
  ══════════════════════════════════════════════════════ -->
  <div v-if="showCheckout"
       class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
       style="background:rgba(0,0,0,0.7); z-index:1050;"
       @click.self="showCheckout = false">

    <div class="rounded-4 d-flex flex-column"
         style="background:#181818; border:1px solid rgba(255,255,255,0.1); width:620px; max-width:95vw; max-height:90vh;">

      <!-- ── Màn hình thành công ── -->
      <template v-if="checkoutSuccess">
        <div class="d-flex flex-column align-items-center justify-content-center gap-3 p-5 text-center">
          <!-- Icon tick xanh -->
          <div class="d-flex align-items-center justify-content-center rounded-circle"
               style="width:64px; height:64px; background:rgba(72,199,142,0.15); color:#48c78e; font-size:1.8rem;">
            ✓
          </div>
          <h2 class="fw-black text-white mb-0">Đặt hàng thành công!</h2>
          <p class="text-secondary mb-0">
            Mã đơn hàng của bạn: <strong class="text-warning">#{{ checkoutOrderId }}</strong>
          </p>
          <p class="text-secondary small mb-0">Chúng tôi sẽ liên hệ xác nhận trong thời gian sớm nhất.</p>
          <button class="btn btn-warning fw-bold px-4 rounded-pill" @click="showCheckout = false">Đóng</button>
        </div>
      </template>

      <!-- ── Form đặt hàng ── -->
      <template v-else>
        <!-- Header modal -->
        <div class="d-flex justify-content-between align-items-center p-3 fw-bold text-white"
             style="border-bottom:1px solid rgba(255,255,255,0.07); font-size:0.95rem;">
          <span>Xác nhận đặt hàng</span>
          <button class="btn-close btn-close-white btn-sm" @click="showCheckout = false"></button>
        </div>

        <!-- Body modal (scroll được) -->
        <div class="p-4 overflow-y-auto flex-grow-1 d-flex flex-column gap-3">

          <!-- Thông báo lỗi -->
          <div v-if="checkoutError" class="alert alert-danger small py-2 mb-0">{{ checkoutError }}</div>

          <!-- Danh sách sản phẩm trong đơn -->
          <div>
            <div class="fw-bold text-secondary small mb-2 text-uppercase" style="letter-spacing:0.04em;">
              Giỏ hàng ({{ cart.length }} sản phẩm)
            </div>
            <div class="d-flex flex-column gap-1">
              <div v-for="item in cart" :key="item.bienTheId"
                   class="d-flex justify-content-between align-items-center small p-2 rounded-2"
                   style="background:rgba(255,255,255,0.04);">
                <span class="text-light fw-medium">{{ item.tenSanPham }}</span>
                <span class="text-secondary mx-2">×{{ item.quantity }}</span>
                <span class="text-warning fw-bold">{{ formatPrice(item.giaBan * item.quantity) }}</span>
              </div>
            </div>
          </div>

          <!-- Thông tin khách hàng -->
          <div>
            <div class="fw-bold text-secondary small mb-2 text-uppercase" style="letter-spacing:0.04em;">
              Thông tin khách hàng
            </div>
            <!-- Tìm theo SĐT -->
            <div class="d-flex gap-2 mb-2">
              <input v-model="checkoutForm.soDienThoai"
                     class="form-control form-control-sm"
                     style="background:#1f1f1f; border-color:#3f3f3f; color:#f0f0f0;"
                     placeholder="Số điện thoại *" />
              <button class="btn btn-sm btn-outline-warning flex-shrink-0" @click="lookupCustomer">Tìm</button>
            </div>
            <!-- Kết quả tìm kiếm khách hàng -->
            <div v-if="foundCustomer"
                 class="small p-2 rounded-2 mb-2"
                 style="background:rgba(72,199,142,0.1); color:#48c78e;">
              ✓ Khách hàng: <strong>{{ foundCustomer.hoTen }}</strong>
            </div>
            <div v-else-if="checkoutForm.soDienThoai"
                 class="small p-2 rounded-2 mb-2 text-secondary"
                 style="background:rgba(255,255,255,0.04);">
              Số điện thoại chưa có trong hệ thống — sẽ tạo khách hàng mới.
            </div>
            <!-- Họ tên + Email -->
            <div class="row g-2">
              <div class="col-6">
                <input v-model="checkoutForm.hoTen"
                       class="form-control form-control-sm"
                       style="background:#1f1f1f; border-color:#3f3f3f; color:#f0f0f0;"
                       placeholder="Họ tên *" />
              </div>
              <div class="col-6">
                <input v-model="checkoutForm.email"
                       class="form-control form-control-sm"
                       style="background:#1f1f1f; border-color:#3f3f3f; color:#f0f0f0;"
                       placeholder="Email *" />
              </div>
            </div>
          </div>

          <!-- Thông tin giao hàng -->
          <div>
            <div class="fw-bold text-secondary small mb-2 text-uppercase" style="letter-spacing:0.04em;">
              Thông tin giao hàng
            </div>
            <div class="row g-2 mb-2">
              <div class="col-6">
                <input v-model="checkoutForm.nguoiNhan"
                       class="form-control form-control-sm"
                       style="background:#1f1f1f; border-color:#3f3f3f; color:#f0f0f0;"
                       placeholder="Người nhận *" />
              </div>
              <div class="col-6">
                <input v-model="checkoutForm.sdtNguoiNhan"
                       class="form-control form-control-sm"
                       style="background:#1f1f1f; border-color:#3f3f3f; color:#f0f0f0;"
                       placeholder="SĐT người nhận *" />
              </div>
            </div>
            <input v-model="checkoutForm.diaChiGiaoHangText"
                   class="form-control form-control-sm"
                   style="background:#1f1f1f; border-color:#3f3f3f; color:#f0f0f0;"
                   placeholder="Địa chỉ giao hàng *" />
          </div>

          <!-- Mã khuyến mãi -->
          <div>
            <div class="fw-bold text-secondary small mb-2 text-uppercase" style="letter-spacing:0.04em;">
              Mã khuyến mãi
            </div>
            <div class="d-flex gap-2 mb-1">
              <input v-model="checkoutForm.maKhuyenMai"
                     class="form-control form-control-sm"
                     style="background:#1f1f1f; border-color:#3f3f3f; color:#f0f0f0;"
                     placeholder="Nhập mã (nếu có)" />
              <button class="btn btn-sm btn-outline-warning flex-shrink-0" @click="applyPromo">Áp dụng</button>
            </div>
            <!-- Kết quả áp dụng mã -->
            <div v-if="promoMsg" class="small"
                 :class="appliedPromo ? 'text-success' : 'text-danger'">
              {{ promoMsg }}
            </div>
          </div>

          <!-- Bảng tổng kết tiền -->
          <div class="p-3 rounded-2 d-flex flex-column gap-1"
               style="background:rgba(255,255,255,0.04); border:1px solid rgba(255,255,255,0.07);">
            <div class="d-flex justify-content-between small text-secondary">
              <span>Tạm tính</span><span>{{ formatPrice(cartTotal) }}</span>
            </div>
            <div class="d-flex justify-content-between small text-secondary">
              <span>Phí vận chuyển</span>
              <span>{{ phiVanChuyen === 0 ? 'Miễn phí' : formatPrice(phiVanChuyen) }}</span>
            </div>
            <div v-if="checkoutGiamGia > 0"
                 class="d-flex justify-content-between small text-success">
              <span>Giảm giá</span><span>- {{ formatPrice(checkoutGiamGia) }}</span>
            </div>
            <!-- Tổng thanh toán -->
            <div class="d-flex justify-content-between fw-bold pt-2 mt-1"
                 style="border-top:1px solid rgba(255,255,255,0.07);">
              <span class="text-white">Thành tiền</span>
              <strong class="text-warning" style="font-size:1rem;">{{ formatPrice(checkoutTotal) }}</strong>
            </div>
          </div>

        </div><!-- /modal body -->

        <!-- Footer modal: nút Hủy và Đặt hàng -->
        <div class="d-flex justify-content-end gap-2 p-3"
             style="border-top:1px solid rgba(255,255,255,0.07);">
          <button class="btn btn-sm btn-outline-secondary" @click="showCheckout = false">Hủy</button>
          <button class="btn btn-sm btn-warning fw-bold px-4"
                  :disabled="checkoutLoading"
                  @click="placeOrder">
            {{ checkoutLoading ? 'Đang xử lý...' : 'Đặt hàng' }}
          </button>
        </div>

      </template>
    </div><!-- /modal box -->
  </div><!-- /checkout overlay -->

  <!-- ── Trang chi tiết sản phẩm (full-screen overlay) ── -->
  <Transition name="slide-up">
    <ProductDetail
      v-if="selectedProduct"
      :key="selectedProduct.bienTheId"
      :product="selectedProduct"
      :products="products"
      @close="closeProduct"
      @add-to-cart="p => { addToCart(p); closeProduct(); }"
      @open-product="openProduct"
    />
  </Transition>
  <div
      v-if="showLogin"
      class="position-fixed top-0 start-0 w-100 h-100 d-flex justify-content-center align-items-center"
      style="background:rgba(0,0,0,.7);z-index:9999"
      @click.self="closeLogin"
  >

    <div
        class="bg-white rounded-4 p-4"
        style="width:450px;max-width:95%"
    >

      <LoginForm />

    </div>

  </div>
</template>


<style>
.slide-up-enter-active, .slide-up-leave-active { transition: transform 0.28s ease, opacity 0.2s ease; }
.slide-up-enter-from, .slide-up-leave-to       { transform: translateY(30px); opacity: 0; }
</style>

<!-- Không còn CSS scoped — toàn bộ dùng Bootstrap utility classes + inline style tối thiểu -->
