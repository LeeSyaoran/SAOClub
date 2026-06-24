<template>
  <!-- ========================================================
    NavBar.vue — Thanh điều hướng chính của trang khách hàng
    Props nhận: cartCount (số lượng sản phẩm trong giỏ)
    Emit ra:   toggle-cart | open-admin | search
  ======================================================== -->

  <!-- Topbar: dải thông báo nhỏ chạy ở trên cùng -->
  <div style="background:#111; border-bottom:1px solid #2a2a2a;">
    <div class="container-xl d-flex justify-content-between align-items-center py-1 overflow-hidden">
      <!-- Tên thương hiệu bên trái -->
      <span class="text-secondary small fw-medium d-none d-md-inline">
        SAOPHONE — Hệ thống bán lẻ Laptop chính hãng hàng đầu
      </span>
      <!-- Các thông tin tiện ích bên phải -->
      <div class="d-flex gap-3 small fw-semibold text-secondary">
        <span class="d-none d-lg-inline">✓ Chính hãng - Xuất VAT đầy đủ</span>
        <span class="d-none d-xl-inline">🚚 Giao nhanh Miễn phí từ 300k</span>
        <span class="d-none d-xl-inline">🔄 Thu cũ đổi mới trợ giá cao</span>
        <!-- Số hotline nổi bật màu vàng -->
        <span class="text-warning fw-black">1800.9999</span>
      </div>
    </div>
  </div>

  <!-- Header chính: sticky ở trên khi scroll -->
  <header class="sticky-top" style="background:#171717; border-bottom:1px solid #2a2a2a; z-index:100;">
    <div class="container-xl d-flex align-items-center gap-2 gap-md-3 py-2">

      <!-- Logo SAOPHONE -->
      <a href="/" class="text-decoration-none fw-black fs-5 text-white me-1 flex-shrink-0"
         style="letter-spacing:-0.04em;">
        SAO<span class="text-warning">PHONE</span>
      </a>

      <!-- ── Nút Danh mục + Mega dropdown ── -->
      <div class="position-relative flex-shrink-0"
           @mouseenter="isMenuOpen = true"
           @mouseleave="isMenuOpen = false">

        <!-- Nút trigger dropdown -->
        <button
          class="btn btn-sm fw-bold small"
          :class="isMenuOpen ? 'text-warning border-warning' : 'text-light border-secondary'"
          style="border:1px solid; border-radius:12px; background:#1f1f1f;">
          ☰ Danh mục Laptop
        </button>

        <!-- Mega menu dropdown panel -->
        <div v-if="isMenuOpen"
             class="position-absolute top-100 start-0 mt-1 shadow-lg rounded-3 overflow-hidden"
             style="width:820px; background:#0a0a0a; border:1px solid #2a2a2a; z-index:200; height:340px; display:flex;">

          <!-- Cột trái: danh sách các loại laptop -->
          <div class="d-flex flex-column gap-1 p-2 flex-shrink-0 overflow-y-auto"
               style="width:280px; background:rgba(255,255,255,0.03); border-right:1px solid #2a2a2a;">
            <div
              v-for="cat in categories"
              :key="cat.id"
              class="d-flex justify-content-between align-items-center px-3 py-2 rounded-2 small fw-bold"
              :class="activeCategory === cat.id ? 'text-warning' : 'text-secondary'"
              :style="activeCategory === cat.id
                ? 'background:#1a1a1a; padding-left:1.25rem!important; cursor:pointer;'
                : 'cursor:pointer;'"
              @mouseenter="activeCategory = cat.id">
              <span>{{ cat.title }}</span>
              <span style="font-size:14px; opacity:0.5;">›</span>
            </div>
          </div>

          <!-- Cột phải: hãng và phân khúc tương ứng -->
          <div class="p-4 flex-grow-1 overflow-y-auto" style="background:#0a0a0a;">
            <template v-for="cat in categories" :key="cat.id">
              <div v-if="cat.id === activeCategory" class="row g-4">
                <!-- Cột hãng sản xuất -->
                <div class="col-6">
                  <div class="text-warning fw-black text-uppercase small pb-2 mb-2"
                       style="font-size:10px; letter-spacing:0.08em; border-bottom:1px solid #2a2a2a;">
                    Hãng sản xuất chính
                  </div>
                  <a v-for="brand in cat.brands" :key="brand"
                     href="#"
                     class="d-flex align-items-center gap-1 text-decoration-none py-1 small fw-bold text-secondary"
                     style="font-size:12px;"
                     @mouseenter="e => e.target.style.color='#fff'"
                     @mouseleave="e => e.target.style.color=''">
                    <span style="color:#3f3f3f;">·</span> {{ brand }}
                  </a>
                </div>
                <!-- Cột phân khúc nổi bật -->
                <div class="col-6">
                  <div class="text-warning fw-black text-uppercase small pb-2 mb-2"
                       style="font-size:10px; letter-spacing:0.08em; border-bottom:1px solid #2a2a2a;">
                    Phân khúc nổi bật
                  </div>
                  <a v-for="tag in cat.tags" :key="tag"
                     href="#"
                     class="d-block text-decoration-none py-1 small fw-bold text-secondary"
                     style="font-size:12px;"
                     @mouseenter="e => e.target.style.color='#facc15'"
                     @mouseleave="e => e.target.style.color=''">
                    {{ tag }}
                  </a>
                </div>
              </div>
            </template>
          </div>
        </div>
      </div><!-- /category dropdown -->

      <!-- Nút chọn thành phố (chỉ hiện trên màn to) -->
      <button class="btn btn-sm fw-bold d-none d-xl-flex align-items-center gap-2 flex-shrink-0"
              style="background:#1f1f1f; border:1px solid #3f3f3f; border-radius:12px; color:#e5e7eb;">
        📍
        <div class="text-start lh-1">
          <div style="font-size:10px; color:#6b7280;">Xem giá tại</div>
          <div style="font-size:12px; font-weight:800;">Hà Nội</div>
        </div>
      </button>

      <!-- Thanh tìm kiếm - chiếm phần lớn không gian còn lại -->
      <div class="input-group flex-grow-1" style="min-width:0;">
        <input
          v-model="searchValue"
          type="text"
          class="form-control form-control-sm"
          style="background:#1f1f1f; border-color:#3f3f3f; color:#e5e7eb; border-radius:12px 0 0 12px; font-size:12px; font-weight:600;"
          placeholder="Nhập tên laptop, hãng hoặc nhu cầu cần tìm...?"
          @keyup.enter="$emit('search', searchValue)"
        />
        <!-- Nút kính lúp trigger tìm kiếm -->
        <button
          class="btn btn-sm"
          style="background:#1f1f1f; border-color:#3f3f3f; border-left:none; color:#9ca3af; border-radius:0 12px 12px 0;"
          @click="$emit('search', searchValue)">
          🔎
        </button>
      </div>

      <!-- Nhóm nút bên phải: Tra cứu | Giỏ hàng | Đăng nhập -->
      <div class="d-flex align-items-center gap-2 flex-shrink-0 ms-1">

        <!-- Nút tra cứu đơn hàng (ẩn trên mobile) -->
        <button class="btn btn-sm d-none d-lg-flex align-items-center gap-1 fw-bold"
                style="background:#1f1f1f; border:1px solid #3f3f3f; border-radius:12px; color:#e5e7eb; font-size:12px; white-space:nowrap;">
          📋 Tra cứu đơn hàng
        </button>

        <!-- Nút giỏ hàng: phát ra sự kiện toggle-cart khi click -->
        <button class="btn btn-sm d-flex align-items-center gap-1 fw-bold"
                style="background:#1f1f1f; border:1px solid #3f3f3f; border-radius:12px; color:#e5e7eb; font-size:12px; white-space:nowrap;"
                @click="$emit('toggle-cart')">
          🛒 Giỏ hàng
          <!-- Badge hiển thị số lượng sản phẩm trong giỏ -->
          <span class="badge text-dark fw-black"
                style="background:#facc15; border-radius:999px; font-size:11px;">
            {{ cartCount }}
          </span>
        </button>

        <!-- Nút đăng nhập: phát ra sự kiện open-admin -->
        <button class="btn btn-sm fw-black"
                style="background:#facc15; color:#111; border:none; border-radius:12px; font-size:12px; white-space:nowrap;"
                @click="$emit('open-admin')">
          👤 Đăng nhập
        </button>
      </div>

    </div><!-- /container-xl -->
  </header>
</template>

<script setup>
// ── Import ──────────────────────────────────────────────────
import { ref } from 'vue';

// Nhận prop cartCount từ App.vue
defineProps({
  cartCount: { type: Number, default: 0 }
});

// Khai báo các sự kiện có thể emit lên component cha
defineEmits(['toggle-cart', 'open-admin', 'search']);

// Trạng thái mở/đóng menu danh mục
const isMenuOpen = ref(false);

// Danh mục đang được hover trong mega menu
const activeCategory = ref('all-laptop');

// Giá trị người dùng nhập vào ô tìm kiếm
const searchValue = ref('');

// Danh sách các loại laptop trong mega dropdown
const categories = [
  {
    id: 'all-laptop',
    title: 'Tất cả thương hiệu Laptop',
    brands: ['ASUS', 'Lenovo', 'MacBook (Apple)', 'MSI', 'Acer', 'HP', 'Dell', 'Gigabyte', 'LG'],
    tags: ['Laptop bán chạy nhất', 'Máy mới về 2026', 'Xả kho máy trưng bày', 'Hỗ trợ trả góp 0%']
  },
  {
    id: 'gaming',
    title: 'Laptop Gaming / Đồ họa nặng',
    brands: ['ASUS ROG / TUF', 'Lenovo Legion / LOQ', 'MSI Gaming', 'Acer Predator / Nitro', 'Gigabyte Gaming', 'Dell Alienware'],
    tags: ['Card RTX 40 Series', 'Màn hình 144Hz - 240Hz', 'Tản nhiệt chuyên dụng', 'Cấu hình khủng']
  },
  {
    id: 'office',
    title: 'Laptop Văn phòng - Học tập',
    brands: ['ASUS Vivobook', 'Lenovo IdeaPad', 'HP Pavilion / ProBook', 'Dell Inspiron', 'Acer Aspire'],
    tags: ['Giá rẻ dưới 15 triệu', 'Bàn phím gõ êm', 'Pin trâu trên 8 tiếng', 'Màn hình chống chói']
  },
  {
    id: 'premium',
    title: 'Cao cấp - Mỏng nhẹ - Sang trọng',
    brands: ['MacBook Air / Pro', 'ASUS Zenbook', 'Lenovo Yoga / Slim', 'HP Envy / Spectre', 'Dell XPS', 'LG Gram'],
    tags: ['Vỏ nhôm nguyên khối', 'Trọng lượng dưới 1.2kg', 'Màn hình OLED / Retina', 'Nhận diện khuôn mặt']
  },
  {
    id: 'creator',
    title: 'Đồ họa chuyên nghiệp - Kỹ thuật AI',
    brands: ['MacBook Pro M-Series', 'ASUS ProArt', 'Lenovo ThinkPad P-Series', 'MSI Creator', 'Dell Precision'],
    tags: ['Màn hình chuẩn màu DCI-P3', 'RAM khủng từ 32GB', 'Tối ưu phần mềm Adobe/CAD', 'Xử lý dữ liệu AI chuyên sâu']
  }
];
</script>

<!-- Không còn CSS scoped — toàn bộ giao diện dùng Bootstrap utility classes -->
