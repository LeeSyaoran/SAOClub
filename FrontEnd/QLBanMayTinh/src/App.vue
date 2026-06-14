<script setup>
import { ref, computed, onMounted } from "vue";

const products = ref([]);
const loading = ref(false);
const error = ref(null);
const searchQuery = ref("");
const selectedSort = ref("default");
const cart = ref([]);

const laptopProducts = computed(() => {
  return products.value.filter((product) => {
    const category = product.tenDanhMuc?.toLowerCase() || "";
    const type = product.loaiSanPham?.toLowerCase() || "";
    return category.includes("laptop") || type.includes("laptop");
  });
});

const filteredProducts = computed(() => {
  const query = searchQuery.value.trim().toLowerCase();
  return laptopProducts.value
    .filter((product) => {
      if (!query) return true;
      const term = query;
      const title = product.tenSanPham?.toLowerCase() || "";
      const brand = product.tenThuongHieu?.toLowerCase() || "";
      const desc = product.moTa?.toLowerCase() || "";
      return (
        title.includes(term) || brand.includes(term) || desc.includes(term)
      );
    })
    .sort((a, b) => {
      if (selectedSort.value === "price-asc") {
        return (a.giaBan || 0) - (b.giaBan || 0);
      }
      if (selectedSort.value === "price-desc") {
        return (b.giaBan || 0) - (a.giaBan || 0);
      }
      return 0;
    });
});

const showCart = ref(false);

const cartCount = computed(() => {
  return cart.value.reduce((total, item) => total + item.quantity, 0);
});

const cartTotal = computed(() => {
  return cart.value.reduce(
    (total, item) => total + (item.quantity || 0) * (item.giaBan || 0),
    0,
  );
});

const toggleCart = () => {
  showCart.value = !showCart.value;
};

const removeFromCart = (productId) => {
  cart.value = cart.value.filter((item) => item.sanPhamId !== productId);
};

const formatPrice = (value) => {
  if (value == null) return "Liên hệ";
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(value);
};

const fetchProducts = async () => {
  loading.value = true;
  error.value = null;

  try {
    const res = await fetch("/api/san-pham/hien-thi");
    if (!res.ok) {
      const body = await res.text();
      throw new Error(`Lỗi API: ${res.status} - ${body}`);
    }
    products.value = await res.json();
    console.log("Loaded products", products.value);
  } catch (err) {
    error.value = err?.message || "Không thể tải dữ liệu sản phẩm";
    console.error("Fetch product error:", err);
  } finally {
    loading.value = false;
  }
};

const addToCart = (product) => {
  const existing = cart.value.find(
    (item) => item.sanPhamId === product.sanPhamId,
  );
  if (existing) {
    existing.quantity += 1;
  } else {
    cart.value.push({ ...product, quantity: 1 });
  }
};

onMounted(fetchProducts);
</script>

<template>
  <div class="page-shell">
    <div class="topbar">
      <div class="brand-block">
        <div class="brand-mark">SAO</div>
        <div>
          <div class="brand-name">SAOPhone</div>
          <div class="brand-sub">Hệ thống công nghệ Đen & Vàng</div>
        </div>
      </div>

      <div class="top-actions">
        <button class="menu-button">Danh mục</button>
        <a href="#" class="top-link">Xem giá tại Hà Nội</a>
        <div class="search-bar">
          <input
            v-model="searchQuery"
            type="text"
            placeholder="Bạn muốn mua gì hôm nay...?"
          />
          <button type="button">🔎</button>
        </div>
        <select v-model="selectedSort" class="sort-select" aria-label="Sắp xếp">
          <option value="default">Mặc định</option>
          <option value="price-asc">Giá thấp đến cao</option>
          <option value="price-desc">Giá cao đến thấp</option>
        </select>
        <button
          class="btn btn-primary cart-button"
          @click="toggleCart"
          type="button"
        >
          Giỏ hàng <span class="cart-badge">{{ cartCount }}</span>
        </button>
        <button class="btn btn-secondary">Đăng nhập</button>
      </div>
    </div>

    <section class="hero-topline">
      <span>Chính hãng - Xuất VAT đầy đủ</span>
      <span>Giao nhanh Miễn phí từ 300k</span>
      <span>Thu cũ giá cao</span>
      <span>1800.9999</span>
    </section>

    <div class="hero-grid">
      <aside class="category-panel">
        <div class="panel-header">Danh mục sản phẩm</div>
        <nav class="category-list">
          <a href="#">Laptop</a>
        </nav>
      </aside>

      <section class="hero-card">
        <div class="hero-image">
          <div class="hero-image-fake"></div>
          <div class="hero-badge">HUAWEI MATEPAD SE 11"</div>
          <div class="hero-copy">
            <h1>HUAWEI MATEPAD SE 11"</h1>
            <p>Mở bán từ 01.06 - 01.07 | Giảm ngay 1.000K + Tặng bút cảm ứng</p>
          </div>
        </div>

        <div class="hero-highlights">
          <template v-if="loading">
            <div class="highlight-card highlight-loading">
              Đang tải sản phẩm...
            </div>
          </template>
          <template v-else-if="error">
            <div class="highlight-card highlight-error">{{ error }}</div>
          </template>
          <template v-else-if="products.length">
            <div
              v-for="product in products.slice(0, 3)"
              :key="product.sanPhamId"
              class="highlight-card"
            >
              <strong>{{ product.tenSanPham }}</strong>
              <span>{{ formatPrice(product.giaBan) }}</span>
            </div>
          </template>
          <template v-else>
            <div class="highlight-card">Chưa có sản phẩm để hiển thị</div>
          </template>
        </div>
      </section>

      <aside class="info-panel">
        <div class="info-top">Chào mừng bạn đến với SAOPhone</div>
        <div class="info-sub">Hệ thống công nghệ Đen & Vàng</div>
        <ul class="info-list">
          <li>Ưu đãi độc quyền học sinh sinh viên</li>
          <li>Deal sốc linh kiện máy tính đồ họa</li>
          <li>Laptop Gaming cấu hình cực mạnh</li>
          <li>Thu cũ đổi mới trợ giá lên tới 3 triệu</li>
        </ul>
        <button class="btn btn-primary btn-block">
          Mùa thi cử - Giảm giá đến 50%
        </button>
        <a href="#" class="support-link">Liên hệ hỗ trợ</a>
      </aside>
    </div>

    <section class="product-list-section">
      <div class="section-header">
        <h2>Sản phẩm Laptop</h2>
        <span>{{ filteredProducts.length }} sản phẩm</span>
      </div>

      <div v-if="loading" class="product-message">Đang tải dữ liệu...</div>
      <div v-else-if="error" class="product-message error">{{ error }}</div>
      <div v-else-if="laptopProducts.length === 0" class="product-message">
        Không tìm thấy sản phẩm laptop.
      </div>

      <div v-else class="product-list">
        <article
          v-for="product in filteredProducts"
          :key="product.sanPhamId"
          class="product-card"
        >
          <div class="product-image">
            <img
              v-if="product.hinhAnhChinh"
              :src="product.hinhAnhChinh"
              :alt="product.tenSanPham"
            />
            <div v-else class="product-image-fallback">No image</div>
          </div>
          <div class="product-info">
            <h3>{{ product.tenSanPham }}</h3>
            <p class="product-brand">
              {{ product.tenThuongHieu || product.tenDanhMuc }}
            </p>
            <p class="product-price">{{ formatPrice(product.giaBan) }}</p>
            <p class="product-desc">{{ product.moTa || "Không có mô tả." }}</p>
            <button
              class="btn btn-primary add-to-cart"
              @click="addToCart(product)"
            >
              Thêm vào giỏ
            </button>
          </div>
        </article>
      </div>

      <aside v-if="showCart" class="cart-panel">
        <div class="cart-header">
          <h3>Giỏ hàng</h3>
          <button class="btn btn-secondary" type="button" @click="toggleCart">
            Đóng
          </button>
        </div>
        <div v-if="cartCount === 0" class="cart-empty">
          Chưa có sản phẩm nào trong giỏ.
        </div>
        <div v-else class="cart-items">
          <div v-for="item in cart" :key="item.sanPhamId" class="cart-item">
            <div>
              <strong>{{ item.tenSanPham }}</strong>
              <div class="cart-item-meta">
                <span
                  >{{ item.quantity }} x {{ formatPrice(item.giaBan) }}</span
                >
                <span class="cart-line-total">{{
                  formatPrice(item.quantity * item.giaBan)
                }}</span>
              </div>
            </div>
            <button
              class="btn btn-secondary cart-remove"
              type="button"
              @click="removeFromCart(item.sanPhamId)"
            >
              Xóa
            </button>
          </div>
          <div class="cart-total">
            <span>Tổng</span>
            <strong>{{ formatPrice(cartTotal) }}</strong>
          </div>
        </div>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.page-shell {
  min-height: 100vh;
  background: radial-gradient(circle at top left, #222, #090909 38%);
  color: #f5f5f5;
  padding: 24px;
  font-family: "Inter", sans-serif;
}

.topbar {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.brand-block {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-mark {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #f4c200;
  color: #111;
  display: grid;
  place-items: center;
  font-weight: 800;
}

.brand-name {
  font-size: 1.15rem;
  font-weight: 700;
}

.brand-sub {
  color: #bfbfbd;
  font-size: 0.9rem;
}

.top-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  justify-content: flex-end;
}

.top-link {
  color: #f5f5f5;
  text-decoration: none;
  font-weight: 500;
  white-space: nowrap;
}

.menu-button,
.btn {
  border: none;
  border-radius: 999px;
  padding: 10px 18px;
  cursor: pointer;
  font-weight: 600;
}

.menu-button {
  background: rgba(255, 255, 255, 0.06);
  color: #f5f5f5;
}

.btn-primary {
  background: #ffb400;
  color: #111;
}

.btn-secondary {
  background: rgba(255, 255, 255, 0.1);
  color: #f5f5f5;
}

.search-bar {
  display: flex;
  align-items: center;
  min-width: 320px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 999px;
  overflow: hidden;
}

.search-bar input {
  flex: 1;
  border: none;
  background: transparent;
  color: #f5f5f5;
  padding: 10px 14px;
}

.search-bar button {
  border: none;
  background: rgba(255, 255, 255, 0.08);
  color: #f5f5f5;
  width: 48px;
  cursor: pointer;
}

.sort-select {
  border: none;
  border-radius: 999px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.08);
  color: #f5f5f5;
  cursor: pointer;
}

.cart-badge {
  display: inline-block;
  min-width: 22px;
  text-align: center;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 999px;
  padding: 2px 8px;
  margin-left: 8px;
  color: #111;
  font-weight: 700;
}

.add-to-cart {
  width: fit-content;
  padding: 10px 14px;
}

.cart-panel {
  margin-top: 24px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 24px;
  padding: 18px;
}

.cart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.cart-items {
  display: grid;
  gap: 14px;
}

.cart-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 14px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 18px;
}

.cart-item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  color: #d1d1d1;
  font-size: 0.92rem;
}

.cart-line-total {
  font-weight: 700;
  color: #ffb400;
}

.cart-remove {
  min-width: 72px;
  padding: 10px 12px;
}

.cart-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.08);
  margin-top: 12px;
}

.hero-topline {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin: 24px 0 20px;
  color: #d5d5d5;
  font-size: 0.95rem;
}

.hero-topline span {
  background: rgba(255, 255, 255, 0.04);
  padding: 10px 14px;
  border-radius: 999px;
}

.hero-grid {
  display: grid;
  gap: 20px;
  grid-template-columns: minmax(240px, 280px) minmax(420px, 1fr) minmax(
      240px,
      300px
    );
}

.category-panel,
.info-panel,
.hero-card {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 24px;
  padding: 24px;
}

.panel-header {
  font-size: 1rem;
  font-weight: 700;
  margin-bottom: 18px;
  letter-spacing: 0.02em;
}

.category-list {
  display: grid;
  gap: 12px;
}

.category-list a {
  display: block;
  padding: 12px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.03);
  color: #f5f5f5;
  text-decoration: none;
}

.category-list a:hover {
  background: rgba(255, 255, 255, 0.1);
}

.hero-image {
  position: relative;
  min-height: 420px;
  overflow: hidden;
  border-radius: 28px;
  background: linear-gradient(135deg, #171717 0%, #111 45%, #252525 100%);
  box-shadow: 0 30px 70px rgba(0, 0, 0, 0.45);
}

.hero-image-fake {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 45% 30%, rgba(250, 185, 0, 0.2), transparent 32%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0));
}

.hero-image::before {
  content: "";
  position: absolute;
  left: 12%;
  top: 8%;
  width: 76%;
  height: 72%;
  border-radius: 32px;
  background: linear-gradient(180deg, #202020 0%, #0d0d0d 100%);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.06);
}

.hero-badge {
  position: absolute;
  left: 28px;
  top: 24px;
  display: inline-flex;
  padding: 10px 16px;
  background: rgba(255, 180, 0, 0.14);
  border-radius: 999px;
  color: #ffd766;
  font-weight: 700;
  letter-spacing: 0.03em;
}

.hero-copy {
  position: absolute;
  left: 28px;
  bottom: 36px;
  z-index: 1;
  max-width: 66%;
}

.hero-copy h1 {
  margin: 0 0 10px;
  font-size: clamp(2.1rem, 4vw, 3.2rem);
  line-height: 1.02;
}

.hero-copy p {
  margin: 0;
  color: #d4d4d4;
  font-size: 1rem;
}

.hero-highlights {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 22px;
}

.highlight-loading,
.highlight-error {
  grid-column: 1 / -1;
}

.highlight-card {
  padding: 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.05);
  color: #fff;
  min-height: 80px;
}

.highlight-card strong {
  display: block;
  margin-bottom: 8px;
  font-size: 0.98rem;
}

.highlight-card span {
  color: #bdbdbd;
  font-size: 0.92rem;
}

.info-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.info-top {
  font-size: 1.1rem;
  font-weight: 700;
}

.info-sub {
  color: #c7c7c7;
}

.info-list {
  display: grid;
  gap: 12px;
  list-style: none;
  padding: 0;
  margin: 0;
}

.info-list li {
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.04);
}

.btn-block {
  width: 100%;
}

.support-link {
  display: inline-flex;
  justify-content: center;
  padding: 12px 16px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
  text-decoration: none;
  font-weight: 600;
}

.product-list-section {
  margin-top: 28px;
  padding: 24px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
}

.section-header h2 {
  font-size: 1.25rem;
}

.section-header span {
  color: #d1d1d1;
}

.product-message {
  color: #d1d1d1;
  padding: 14px 0;
}

.product-message.error {
  color: #ffcccc;
}

.product-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.product-card {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 24px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.product-image {
  min-height: 170px;
  background: rgba(255, 255, 255, 0.04);
  display: grid;
  place-items: center;
}

.product-image img {
  max-width: 100%;
  max-height: 170px;
  object-fit: contain;
}

.product-image-fallback {
  color: #b3b3b3;
  font-size: 0.95rem;
}

.product-info {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.product-info h3 {
  margin: 0;
  font-size: 1.05rem;
}

.product-brand {
  color: #d1d1d1;
  font-size: 0.9rem;
}

.product-price {
  color: #ffb400;
  font-weight: 700;
}

.product-desc {
  margin: 0;
  color: #c8c8c8;
  font-size: 0.95rem;
}

@media (max-width: 1120px) {
  .hero-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 820px) {
  .topbar,
  .hero-grid {
    flex-direction: column;
  }

  .top-actions {
    min-width: 0;
    width: 100%;
  }

  .search-bar {
    min-width: 0;
    width: 100%;
  }
}
</style>
