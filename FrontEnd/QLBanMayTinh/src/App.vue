<script setup>
// ── Import các thư viện Vue 3 cần thiết ──────────────────────────────────────
import { ref, computed, reactive, onMounted, onBeforeUnmount } from "vue";

// Import store xác thực
import { AuthStore, setSession, clearSession } from "./stores/index.js";

// Import services
import * as SanPhamService  from "./Service/SanPhamService.js";
import * as DanhMucService  from "./Service/DanhMucService.js";
import * as KhachHangService from "./Service/KhachHangService.js";
import * as KhuyenMaiService from "./Service/KhuyenMaiService.js";
import * as DonHangService  from "./Service/DonHangService.js";

// Import các component trang
import AdminPage     from "./pages/AdminPage.vue";
import LoginForm     from "./components/auth/LoginForm.vue";
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
// ── Toast notification ────────────────────────────────────────────────────────
const toast = reactive({ show: false, msg: '', type: 'success' });
let toastTimer = null;
const showToast = (msg, type = 'success') => {
  clearTimeout(toastTimer);
  toast.msg  = msg;
  toast.type = type;
  toast.show = true;
  toastTimer = setTimeout(() => { toast.show = false; }, 3500);
};

// ── Login modal ───────────────────────────────────────────────────────────────
const showLoginModal = ref(false);
const loginModalErr  = ref('');

const openLogin = () => {
  loginModalErr.value  = '';
  showLoginModal.value = true;
};

const handleModalLogin = async ({ username, password }) => {
  loginModalErr.value = '';
  try {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    });
    if (!res.ok) {
      const msg = await res.text();
      loginModalErr.value = msg;
      showToast(msg || 'Đăng nhập thất bại.', 'error');
      return;
    }
    const user = await res.json();
    showLoginModal.value = false;
    showToast(`Xin chào, ${user.hoTen}!`, 'success');
    onLoginSuccess(user);
  } catch {
    loginModalErr.value = 'Không thể kết nối đến máy chủ.';
    showToast('Không thể kết nối đến máy chủ.', 'error');
  }
};

// ── Logout ────────────────────────────────────────────────────────────────────
const onLogout = () => {
  clearSession();
  showToast('Đã đăng xuất thành công.', 'info');
  window.location.hash = '';
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

// ── Giỏ hàng: tăng/giảm số lượng ────────────────────────────────────────────
const updateQty = (bienTheId, delta) => {
  const item = cart.value.find(i => i.bienTheId === bienTheId);
  if (!item) return;
  const newQty = item.quantity + delta;
  if (newQty <= 0) cart.value = cart.value.filter(i => i.bienTheId !== bienTheId);
  else item.quantity = newQty;
};

// ── Checkout (Thanh toán) ─────────────────────────────────────────────────────

const showCheckout    = ref(false); // Hiển thị modal thanh toán
const checkoutStep    = ref(1);     // 1 = thông tin giao hàng, 2 = phương thức thanh toán
const checkoutSuccess = ref(false); // Đặt hàng thành công chưa
const checkoutLoading = ref(false); // Đang xử lý API
const checkoutError   = ref('');    // Thông báo lỗi khi checkout
const checkoutOrderId    = ref(null);  // ID đơn hàng sau khi đặt xong
const checkoutFinalTotal = ref(0);     // Tổng tiền lúc đặt hàng (lưu trước khi xóa giỏ)
const allCustomers    = ref([]);    // Cache danh sách khách hàng
const allPromos       = ref([]);    // Cache danh sách khuyến mãi
const foundCustomer   = ref(null);  // Khách hàng tìm thấy qua SĐT
const appliedPromo    = ref(null);  // Khuyến mãi đã áp dụng
const promoMsg        = ref('');    // Thông báo kết quả áp dụng mã
const selectedPayment = ref('tien_mat'); // 'tien_mat' | 'qr' | 'chuyen_khoan'

// VietQR — tạo QR code thật từ API vietqr.io
const qrImageUrl = computed(() => {
  const bank    = 'VCB';
  const account = '9876543210';
  const info    = encodeURIComponent('Thanh toan SAO LAPTOP');
  const name    = encodeURIComponent('SAO LAPTOP');
  return `https://img.vietqr.io/image/${bank}-${account}-compact2.png?amount=${checkoutTotal.value}&addInfo=${info}&accountName=${name}`;
});

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
  if (cart.value.length === 0) return;
  checkoutStep.value    = 1;
  checkoutSuccess.value = false;
  checkoutError.value   = '';
  promoMsg.value        = '';
  appliedPromo.value    = null;
  foundCustomer.value   = null;
  selectedPayment.value = 'tien_mat';
  Object.keys(checkoutForm).forEach(k => { checkoutForm[k] = ''; });
  if (!allCustomers.value.length) {
    [allCustomers.value, allPromos.value] = await Promise.all([
      KhachHangService.getAll().catch(() => []),
      KhuyenMaiService.getAll().catch(() => []),
    ]);
  }
  showCart.value     = false;
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
      // thanhTien bỏ qua — computed column trong DB (tong_tien - giam_gia + phi_van_chuyen)
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

    // Lưu tổng tiền trước khi xóa giỏ (checkoutTotal sẽ về 0 sau khi cart rỗng)
    checkoutFinalTotal.value = checkoutTotal.value;
    checkoutOrderId.value    = donHangId;
    checkoutSuccess.value    = true;
    cart.value               = [];
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

// Xử lý sau khi đăng nhập thành công — phân quyền theo role
function onLoginSuccess(user) {
  setSession(user);
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
          :user="auth.user"
          @toggle-cart="toggleCart"
          @search="handleSearch"
          @open-admin="goAdmin"
          @open-login="openLogin"
          @logout="onLogout"
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
        <Transition name="cart-slide">
        <div v-if="showCart"
             class="position-fixed top-0 end-0 h-100 d-flex flex-column"
             style="width:390px; background:#0f0f0f; border-left:1px solid #1e1e1e; z-index:500; box-shadow:-12px 0 48px rgba(0,0,0,0.7);">

          <!-- Header giỏ hàng -->
          <div class="d-flex justify-content-between align-items-center px-4 py-3" style="border-bottom:1px solid #1e1e1e;">
            <div class="d-flex align-items-center gap-2">
              <span style="font-size:1.1rem;">🛒</span>
              <span class="fw-bold text-white" style="font-size:0.95rem;">Giỏ hàng</span>
              <span v-if="cartCount > 0" class="badge bg-warning text-dark fw-bold rounded-pill" style="font-size:10px;">{{ cartCount }}</span>
            </div>
            <button class="btn btn-sm d-flex align-items-center justify-content-center rounded-circle"
                    style="width:30px;height:30px;padding:0;background:#222;color:#999;border:none;font-size:14px;"
                    @click="toggleCart">✕</button>
          </div>

          <!-- Empty state -->
          <div v-if="cartCount === 0" class="flex-grow-1 d-flex flex-column align-items-center justify-content-center gap-3 text-center px-4">
            <div style="font-size:3rem; opacity:0.2;">🛍️</div>
            <p class="text-secondary small mb-0">Giỏ hàng của bạn đang trống</p>
            <button class="btn btn-sm btn-outline-warning rounded-pill px-4" @click="toggleCart">Tiếp tục mua sắm</button>
          </div>

          <!-- Danh sách sản phẩm -->
          <div v-else class="flex-grow-1 overflow-y-auto px-3 py-2 d-flex flex-column gap-2">
            <div v-for="item in cart" :key="item.bienTheId"
                 class="d-flex gap-3 p-3 rounded-3"
                 style="background:#171717; border:1px solid #242424;">

              <!-- Ảnh sản phẩm -->
              <div class="flex-shrink-0" style="width:64px;height:64px;">
                <img v-if="item.hinhAnhChinh" :src="item.hinhAnhChinh" :alt="item.tenSanPham"
                     style="width:64px;height:64px;object-fit:contain;border-radius:10px;background:#111;" />
                <div v-else class="d-flex align-items-center justify-content-center rounded-3"
                     style="width:64px;height:64px;background:#1e1e1e;font-size:1.6rem;">💻</div>
              </div>

              <!-- Thông tin -->
              <div class="flex-grow-1 min-width-0">
                <div class="fw-semibold text-light" style="font-size:12px; line-height:1.4; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; line-clamp:2; -webkit-box-orient:vertical;">{{ item.tenSanPham }}</div>
                <div class="text-secondary mt-1" style="font-size:10px;">
                  <span v-if="item.mauSac">{{ item.mauSac }}</span>
                  <span v-if="item.mauSac && item.cpu"> · </span>
                  <span v-if="item.cpu">{{ item.cpu }}</span>
                </div>
                <!-- Số lượng + giá -->
                <div class="d-flex align-items-center justify-content-between mt-2">
                  <div class="d-flex align-items-center gap-1">
                    <button class="d-flex align-items-center justify-content-center"
                            style="width:26px;height:26px;padding:0;background:#252525;color:#ccc;border:none;border-radius:7px;font-size:15px;cursor:pointer;line-height:1;"
                            @click="updateQty(item.bienTheId, -1)">−</button>
                    <span class="text-white fw-bold" style="font-size:13px;min-width:22px;text-align:center;">{{ item.quantity }}</span>
                    <button class="d-flex align-items-center justify-content-center"
                            style="width:26px;height:26px;padding:0;background:#252525;color:#ccc;border:none;border-radius:7px;font-size:15px;cursor:pointer;line-height:1;"
                            @click="updateQty(item.bienTheId, 1)">+</button>
                  </div>
                  <span class="text-warning fw-bold" style="font-size:13px;">{{ formatPrice(item.giaBan * item.quantity) }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Footer: tổng + checkout -->
          <div v-if="cartCount > 0" class="px-4 py-3 d-flex flex-column gap-2" style="border-top:1px solid #1e1e1e; background:#0f0f0f;">
            <div class="d-flex justify-content-between align-items-center">
              <span class="text-secondary small">Tạm tính ({{ cartCount }} sản phẩm)</span>
              <strong class="text-white">{{ formatPrice(cartTotal) }}</strong>
            </div>
            <div class="text-secondary" style="font-size:10px;">🚚 Miễn phí vận chuyển cho đơn từ 300.000đ</div>
            <button class="btn btn-warning fw-bold w-100 py-2 mt-1"
                    style="border-radius:12px; font-size:0.9rem; letter-spacing:0.01em;"
                    @click="openCheckout">
              Thanh toán &nbsp;·&nbsp; {{ formatPrice(cartTotal) }}
            </button>
          </div>

        </div>
        </Transition><!-- /cart panel -->

      </div><!-- /container-xl -->

      <!-- Footer -->
      <AppFooter />

    </div><!-- /trang khách hàng -->

  </div><!-- /root -->

  <!-- ══════════════════════════════════════════════════════
      TOAST NOTIFICATION
  ══════════════════════════════════════════════════════ -->
  <Transition name="toast-slide">
    <div v-if="toast.show"
         class="position-fixed d-flex align-items-center gap-2 px-4 py-3 rounded-3 fw-semibold small shadow-lg"
         style="top:80px; right:24px; z-index:9999; min-width:260px; max-width:380px; pointer-events:none;"
         :style="toast.type === 'success'
           ? 'background:#16a34a; color:#fff;'
           : toast.type === 'error'
           ? 'background:#dc2626; color:#fff;'
           : 'background:#2563eb; color:#fff;'">
      <span style="font-size:1.1rem; flex-shrink:0;">
        {{ toast.type === 'success' ? '✓' : toast.type === 'error' ? '✕' : 'ℹ' }}
      </span>
      {{ toast.msg }}
    </div>
  </Transition>

  <!-- ══════════════════════════════════════════════════════
      LOGIN MODAL — overlay trên trang khách hàng
  ══════════════════════════════════════════════════════ -->
  <div v-if="showLoginModal"
       class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
       style="background:rgba(0,0,0,0.75); z-index:1050; backdrop-filter:blur(4px);"
       @click.self="showLoginModal = false">
    <div class="rounded-4 p-4 position-relative"
         style="background:#141414; border:1px solid #252525; width:460px; max-width:94vw; box-shadow:0 24px 80px rgba(0,0,0,0.7);">
      <button class="btn-close btn-close-white position-absolute"
              style="top:16px; right:16px; font-size:0.75rem;"
              @click="showLoginModal = false"></button>
      <LoginForm
          @submit="handleModalLogin"
          @open-register="showLoginModal = false; window.location.hash = '#login'" />
      <div v-if="loginModalErr"
           class="alert alert-danger small py-2 mt-2 mb-0 rounded-3">
        {{ loginModalErr }}
      </div>
    </div>
  </div>

  <!-- ══════════════════════════════════════════════════════
      CHECKOUT MODAL — 2 bước: Thông tin → Thanh toán
  ══════════════════════════════════════════════════════ -->
  <div v-if="showCheckout"
       class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
       style="background:rgba(0,0,0,0.8); z-index:1050; backdrop-filter:blur(4px);"
       @click.self="showCheckout = false">

    <div class="rounded-4 d-flex flex-column"
         style="background:#141414; border:1px solid #252525; width:660px; max-width:96vw; max-height:92vh; box-shadow:0 24px 80px rgba(0,0,0,0.7);">

      <!-- ══ Màn hình thành công ══ -->
      <template v-if="checkoutSuccess">
        <div class="d-flex flex-column align-items-center justify-content-center gap-4 p-5 text-center">
          <div class="d-flex align-items-center justify-content-center rounded-circle"
               style="width:72px;height:72px;background:rgba(72,199,142,0.15);color:#48c78e;font-size:2rem;">✓</div>
          <div>
            <h2 class="fw-black text-white mb-1" style="font-size:1.4rem;">Đặt hàng thành công!</h2>
            <p class="text-secondary mb-0" style="font-size:0.9rem;">
              Mã đơn hàng: <strong class="text-warning">#{{ checkoutOrderId }}</strong>
            </p>
          </div>
          <!-- Hướng dẫn thanh toán theo phương thức đã chọn -->
          <div v-if="selectedPayment === 'tien_mat'"
               class="p-3 rounded-3 text-center small"
               style="background:#1e2a1e; color:#6ee7b7; border:1px solid #2a3d2a; max-width:360px;">
            💵 Vui lòng chuẩn bị tiền mặt <strong>{{ formatPrice(checkoutFinalTotal) }}</strong> khi nhân viên giao hàng đến.
          </div>
          <div v-else-if="selectedPayment === 'qr'"
               class="p-3 rounded-3 text-center small"
               style="background:#1a1e2a; color:#93c5fd; border:1px solid #252e3a; max-width:360px;">
            ✅ Cảm ơn! Chúng tôi sẽ xác nhận thanh toán sau khi nhận được chuyển khoản.
          </div>
          <div v-else
               class="p-3 rounded-3 text-center small"
               style="background:#1a1e2a; color:#93c5fd; border:1px solid #252e3a; max-width:360px;">
            🏦 Vui lòng chuyển khoản <strong>{{ formatPrice(checkoutFinalTotal) }}</strong> theo thông tin đã cung cấp.
          </div>
          <button class="btn btn-warning fw-bold px-5 rounded-pill" style="font-size:0.9rem;" @click="showCheckout = false">Đóng</button>
        </div>
      </template>

      <!-- ══ Form đặt hàng ══ -->
      <template v-else>

        <!-- Header + step indicator -->
        <div class="px-4 pt-4 pb-3" style="border-bottom:1px solid #222;">
          <div class="d-flex justify-content-between align-items-start">
            <div>
              <h5 class="fw-black text-white mb-1" style="font-size:1rem;">
                {{ checkoutStep === 1 ? 'Thông tin đặt hàng' : 'Phương thức thanh toán' }}
              </h5>
              <div class="d-flex align-items-center gap-2">
                <div class="d-flex align-items-center gap-1">
                  <div class="rounded-circle d-flex align-items-center justify-content-center fw-bold"
                       style="width:20px;height:20px;font-size:10px;"
                       :style="checkoutStep >= 1 ? 'background:#facc15;color:#000;' : 'background:#333;color:#666;'">1</div>
                  <span class="small" :class="checkoutStep >= 1 ? 'text-warning' : 'text-secondary'" style="font-size:11px;">Thông tin</span>
                </div>
                <div class="text-secondary" style="font-size:10px;">───</div>
                <div class="d-flex align-items-center gap-1">
                  <div class="rounded-circle d-flex align-items-center justify-content-center fw-bold"
                       style="width:20px;height:20px;font-size:10px;"
                       :style="checkoutStep >= 2 ? 'background:#facc15;color:#000;' : 'background:#333;color:#666;'">2</div>
                  <span class="small" :class="checkoutStep >= 2 ? 'text-warning' : 'text-secondary'" style="font-size:11px;">Thanh toán</span>
                </div>
              </div>
            </div>
            <button class="btn-close btn-close-white mt-1" style="font-size:0.7rem;" @click="showCheckout = false"></button>
          </div>
        </div>

        <!-- Thông báo lỗi -->
        <div v-if="checkoutError" class="mx-4 mt-3">
          <div class="alert alert-danger small py-2 mb-0 rounded-3">{{ checkoutError }}</div>
        </div>

        <!-- ── BƯỚC 1: Thông tin giao hàng ── -->
        <div v-if="checkoutStep === 1" class="overflow-y-auto flex-grow-1 px-4 py-3 d-flex flex-column gap-4">

          <!-- Giỏ hàng tóm tắt -->
          <div>
            <div class="text-secondary fw-semibold mb-2" style="font-size:11px; letter-spacing:0.06em; text-transform:uppercase;">Đơn hàng · {{ cart.length }} sản phẩm</div>
            <div class="d-flex flex-column gap-1 rounded-3 p-2" style="background:#1a1a1a;">
              <div v-for="item in cart" :key="item.bienTheId"
                   class="d-flex align-items-center gap-3 px-2 py-1">
                <div style="width:36px;height:36px;flex-shrink:0;">
                  <img v-if="item.hinhAnhChinh" :src="item.hinhAnhChinh" :alt="item.tenSanPham" style="width:36px;height:36px;object-fit:contain;border-radius:6px;" />
                  <div v-else class="d-flex align-items-center justify-content-center rounded-2" style="width:36px;height:36px;background:#222;font-size:1rem;">💻</div>
                </div>
                <span class="text-light flex-grow-1 small text-truncate">{{ item.tenSanPham }}</span>
                <span class="text-secondary small flex-shrink-0">×{{ item.quantity }}</span>
                <span class="text-warning fw-semibold small flex-shrink-0">{{ formatPrice(item.giaBan * item.quantity) }}</span>
              </div>
            </div>
          </div>

          <!-- Thông tin khách hàng -->
          <div>
            <div class="text-secondary fw-semibold mb-2" style="font-size:11px; letter-spacing:0.06em; text-transform:uppercase;">Khách hàng</div>
            <div class="d-flex gap-2 mb-2">
              <input v-model="checkoutForm.soDienThoai"
                     class="form-control form-control-sm"
                     style="background:#1e1e1e;border-color:#333;color:#f0f0f0;border-radius:10px;"
                     placeholder="Số điện thoại *" @keyup.enter="lookupCustomer" />
              <button class="btn btn-sm btn-outline-warning flex-shrink-0 px-3" style="border-radius:10px;" @click="lookupCustomer">Tìm</button>
            </div>
            <div v-if="foundCustomer" class="small p-2 rounded-3 mb-2" style="background:rgba(72,199,142,0.1);color:#48c78e;">
              ✓ Đã tìm thấy: <strong>{{ foundCustomer.hoTen }}</strong>
            </div>
            <div v-else-if="checkoutForm.soDienThoai" class="small p-2 rounded-3 mb-2 text-secondary" style="background:#1a1a1a;">
              Số mới — sẽ tạo tài khoản khách hàng.
            </div>
            <div class="row g-2">
              <div class="col-6">
                <input v-model="checkoutForm.hoTen" class="form-control form-control-sm"
                       style="background:#1e1e1e;border-color:#333;color:#f0f0f0;border-radius:10px;"
                       placeholder="Họ tên *" />
              </div>
              <div class="col-6">
                <input v-model="checkoutForm.email" class="form-control form-control-sm"
                       style="background:#1e1e1e;border-color:#333;color:#f0f0f0;border-radius:10px;"
                       placeholder="Email" />
              </div>
            </div>
          </div>

          <!-- Thông tin giao hàng -->
          <div>
            <div class="text-secondary fw-semibold mb-2" style="font-size:11px; letter-spacing:0.06em; text-transform:uppercase;">Giao hàng</div>
            <div class="row g-2 mb-2">
              <div class="col-6">
                <input v-model="checkoutForm.nguoiNhan" class="form-control form-control-sm"
                       style="background:#1e1e1e;border-color:#333;color:#f0f0f0;border-radius:10px;"
                       placeholder="Người nhận *" />
              </div>
              <div class="col-6">
                <input v-model="checkoutForm.sdtNguoiNhan" class="form-control form-control-sm"
                       style="background:#1e1e1e;border-color:#333;color:#f0f0f0;border-radius:10px;"
                       placeholder="SĐT người nhận *" />
              </div>
            </div>
            <input v-model="checkoutForm.diaChiGiaoHangText" class="form-control form-control-sm"
                   style="background:#1e1e1e;border-color:#333;color:#f0f0f0;border-radius:10px;"
                   placeholder="Địa chỉ giao hàng *" />
          </div>

          <!-- Mã khuyến mãi -->
          <div>
            <div class="text-secondary fw-semibold mb-2" style="font-size:11px; letter-spacing:0.06em; text-transform:uppercase;">Mã khuyến mãi</div>
            <div class="d-flex gap-2">
              <input v-model="checkoutForm.maKhuyenMai" class="form-control form-control-sm"
                     style="background:#1e1e1e;border-color:#333;color:#f0f0f0;border-radius:10px;"
                     placeholder="Nhập mã giảm giá (nếu có)" @keyup.enter="applyPromo" />
              <button class="btn btn-sm btn-outline-warning flex-shrink-0 px-3" style="border-radius:10px;" @click="applyPromo">Áp dụng</button>
            </div>
            <div v-if="promoMsg" class="small mt-2 px-1" :class="appliedPromo ? 'text-success' : 'text-danger'">{{ promoMsg }}</div>
          </div>

          <!-- Tổng tiền -->
          <div class="p-3 rounded-3 d-flex flex-column gap-2" style="background:#1a1a1a;border:1px solid #252525;">
            <div class="d-flex justify-content-between small text-secondary">
              <span>Tạm tính</span><span>{{ formatPrice(cartTotal) }}</span>
            </div>
            <div class="d-flex justify-content-between small text-secondary">
              <span>Phí vận chuyển</span>
              <span :class="phiVanChuyen === 0 ? 'text-success' : ''">{{ phiVanChuyen === 0 ? 'Miễn phí' : formatPrice(phiVanChuyen) }}</span>
            </div>
            <div v-if="checkoutGiamGia > 0" class="d-flex justify-content-between small text-success">
              <span>Giảm giá</span><span>− {{ formatPrice(checkoutGiamGia) }}</span>
            </div>
            <div class="d-flex justify-content-between fw-bold pt-2" style="border-top:1px solid #2a2a2a;">
              <span class="text-white">Thành tiền</span>
              <strong class="text-warning" style="font-size:1.05rem;">{{ formatPrice(checkoutTotal) }}</strong>
            </div>
          </div>

        </div><!-- /bước 1 -->

        <!-- ── BƯỚC 2: Phương thức thanh toán ── -->
        <div v-else class="overflow-y-auto flex-grow-1 px-4 py-3 d-flex flex-column gap-4">

          <!-- Số tiền cần thanh toán -->
          <div class="text-center py-2">
            <div class="text-secondary small mb-1">Tổng cần thanh toán</div>
            <div class="fw-black text-warning" style="font-size:2rem;">{{ formatPrice(checkoutTotal) }}</div>
          </div>

          <!-- Chọn phương thức -->
          <div>
            <div class="text-secondary fw-semibold mb-3" style="font-size:11px; letter-spacing:0.06em; text-transform:uppercase;">Chọn phương thức</div>
            <div class="d-flex flex-column gap-2">

              <!-- Tiền mặt -->
              <label class="d-flex align-items-center gap-3 p-3 rounded-3 cursor-pointer"
                     style="border:2px solid; cursor:pointer;"
                     :style="selectedPayment==='tien_mat' ? 'border-color:#facc15;background:rgba(250,204,21,0.06);' : 'border-color:#252525;background:#1a1a1a;'"
                     @click="selectedPayment='tien_mat'">
                <div class="d-flex align-items-center justify-content-center rounded-circle flex-shrink-0"
                     style="width:42px;height:42px;background:#2a2000;font-size:1.3rem;">💵</div>
                <div class="flex-grow-1">
                  <div class="fw-bold text-white" style="font-size:0.9rem;">Tiền mặt khi nhận hàng</div>
                  <div class="text-secondary" style="font-size:11px;">Thanh toán cho nhân viên giao hàng</div>
                </div>
                <div class="rounded-circle border d-flex align-items-center justify-content-center flex-shrink-0"
                     style="width:20px;height:20px;"
                     :style="selectedPayment==='tien_mat' ? 'border-color:#facc15;background:#facc15;' : 'border-color:#444;background:transparent;'">
                  <div v-if="selectedPayment==='tien_mat'" style="width:8px;height:8px;border-radius:50%;background:#000;"></div>
                </div>
              </label>

              <!-- QR Code -->
              <label class="d-flex align-items-center gap-3 p-3 rounded-3"
                     style="border:2px solid; cursor:pointer;"
                     :style="selectedPayment==='qr' ? 'border-color:#facc15;background:rgba(250,204,21,0.06);' : 'border-color:#252525;background:#1a1a1a;'"
                     @click="selectedPayment='qr'">
                <div class="d-flex align-items-center justify-content-center rounded-circle flex-shrink-0"
                     style="width:42px;height:42px;background:#0a1a2a;font-size:1.3rem;">📱</div>
                <div class="flex-grow-1">
                  <div class="fw-bold text-white" style="font-size:0.9rem;">Quét mã QR</div>
                  <div class="text-secondary" style="font-size:11px;">VietQR · Mọi ngân hàng hỗ trợ</div>
                </div>
                <div class="rounded-circle border d-flex align-items-center justify-content-center flex-shrink-0"
                     style="width:20px;height:20px;"
                     :style="selectedPayment==='qr' ? 'border-color:#facc15;background:#facc15;' : 'border-color:#444;background:transparent;'">
                  <div v-if="selectedPayment==='qr'" style="width:8px;height:8px;border-radius:50%;background:#000;"></div>
                </div>
              </label>

              <!-- Chuyển khoản -->
              <label class="d-flex align-items-center gap-3 p-3 rounded-3"
                     style="border:2px solid; cursor:pointer;"
                     :style="selectedPayment==='chuyen_khoan' ? 'border-color:#facc15;background:rgba(250,204,21,0.06);' : 'border-color:#252525;background:#1a1a1a;'"
                     @click="selectedPayment='chuyen_khoan'">
                <div class="d-flex align-items-center justify-content-center rounded-circle flex-shrink-0"
                     style="width:42px;height:42px;background:#0a1a0a;font-size:1.3rem;">🏦</div>
                <div class="flex-grow-1">
                  <div class="fw-bold text-white" style="font-size:0.9rem;">Chuyển khoản ngân hàng</div>
                  <div class="text-secondary" style="font-size:11px;">Nhập số tài khoản thủ công</div>
                </div>
                <div class="rounded-circle border d-flex align-items-center justify-content-center flex-shrink-0"
                     style="width:20px;height:20px;"
                     :style="selectedPayment==='chuyen_khoan' ? 'border-color:#facc15;background:#facc15;' : 'border-color:#444;background:transparent;'">
                  <div v-if="selectedPayment==='chuyen_khoan'" style="width:8px;height:8px;border-radius:50%;background:#000;"></div>
                </div>
              </label>

            </div>
          </div>

          <!-- QR Code hiển thị khi chọn QR -->
          <Transition name="fade">
          <div v-if="selectedPayment === 'qr'" class="d-flex flex-column align-items-center gap-3 p-4 rounded-3" style="background:#111;border:1px solid #252525;">
            <div class="text-white fw-bold small">Quét QR bằng ứng dụng ngân hàng</div>
            <img :src="qrImageUrl" alt="VietQR" style="width:220px;height:220px;border-radius:12px;background:#fff;padding:6px;" />
            <div class="text-center small text-secondary" style="line-height:1.8;">
              Ngân hàng: <strong class="text-white">Vietcombank (VCB)</strong><br />
              Số tài khoản: <strong class="text-warning">9876543210</strong><br />
              Chủ TK: <strong class="text-white">CÔNG TY SAO LAPTOP</strong><br />
              Số tiền: <strong class="text-warning">{{ formatPrice(checkoutTotal) }}</strong><br />
              Nội dung: <strong class="text-white">Thanh toan SAO LAPTOP</strong>
            </div>
          </div>
          </Transition>

          <!-- Thông tin chuyển khoản thủ công -->
          <Transition name="fade">
          <div v-if="selectedPayment === 'chuyen_khoan'" class="p-4 rounded-3 small" style="background:#111;border:1px solid #252525;line-height:2;">
            <div class="fw-bold text-white mb-2">Thông tin chuyển khoản</div>
            <div class="text-secondary">
              Ngân hàng: <strong class="text-white">Vietcombank (VCB)</strong><br />
              Số tài khoản: <strong class="text-warning">9876543210</strong><br />
              Chủ tài khoản: <strong class="text-white">CÔNG TY SAO LAPTOP</strong><br />
              Số tiền: <strong class="text-warning">{{ formatPrice(checkoutTotal) }}</strong><br />
              Nội dung CK: <strong class="text-white">Thanh toan SAO LAPTOP</strong>
            </div>
          </div>
          </Transition>

        </div><!-- /bước 2 -->

        <!-- Footer: nút điều hướng bước -->
        <div class="d-flex justify-content-between align-items-center px-4 py-3" style="border-top:1px solid #222;">
          <button v-if="checkoutStep === 1"
                  class="btn btn-sm btn-outline-secondary px-4" style="border-radius:10px;"
                  @click="showCheckout = false">Hủy</button>
          <button v-else
                  class="btn btn-sm btn-outline-secondary px-4" style="border-radius:10px;"
                  @click="checkoutStep = 1">← Quay lại</button>

          <button v-if="checkoutStep === 1"
                  class="btn btn-warning fw-bold px-5" style="border-radius:10px;"
                  @click="checkoutStep = 2">
            Tiếp tục →
          </button>
          <button v-else
                  class="btn btn-warning fw-bold px-5" style="border-radius:10px;"
                  :disabled="checkoutLoading"
                  @click="placeOrder">
            {{ checkoutLoading ? 'Đang xử lý...' : 'Xác nhận đặt hàng' }}
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
</template>


<style>
/* ProductDetail overlay */
.slide-up-enter-active, .slide-up-leave-active { transition: transform 0.28s ease, opacity 0.2s ease; }
.slide-up-enter-from, .slide-up-leave-to       { transform: translateY(30px); opacity: 0; }

/* Cart panel slide-in từ phải */
.cart-slide-enter-active, .cart-slide-leave-active { transition: transform 0.25s ease, opacity 0.2s ease; }
.cart-slide-enter-from, .cart-slide-leave-to       { transform: translateX(100%); opacity: 0; }

/* QR / bank info fade */
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.fade-enter-from, .fade-leave-to       { opacity: 0; transform: translateY(8px); }

/* Toast slide-in từ phải */
.toast-slide-enter-active, .toast-slide-leave-active { transition: transform 0.3s ease, opacity 0.25s ease; }
.toast-slide-enter-from, .toast-slide-leave-to       { transform: translateX(110%); opacity: 0; }
</style>

<!-- Không còn CSS scoped — toàn bộ dùng Bootstrap utility classes + inline style tối thiểu -->
