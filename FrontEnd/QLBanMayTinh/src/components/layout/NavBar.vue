<template>
  <!-- ========================================================
    NavBar.vue — Thanh điều hướng chính của trang khách hàng
    Props nhận: cartCount (số lượng sản phẩm trong giỏ)
    Emit ra:   toggle-cart | open-admin | open-account | search
  ======================================================== -->

  <!-- Topbar: dải thông báo nhỏ chạy ở trên cùng -->

  <div style="background:var(--bg-page-alt); border-bottom:1px solid var(--border-color);">
    <div class="container-xl d-flex justify-content-between align-items-center py-1 overflow-hidden">
      <!-- Tên thương hiệu bên trái -->
      <span class="small fw-medium d-none d-md-inline" style="color:var(--text-secondary);">
        {{ t('nav.tagline') }}
      </span>
      <!-- Các thông tin tiện ích bên phải -->
      <div class="d-flex align-items-center gap-3 small fw-semibold" style="color:var(--text-secondary);">
        <span class="d-none d-lg-inline">✓ {{ t('nav.genuine') }}</span>
        <span class="d-none d-xl-inline">🚚 {{ t('nav.freeShip') }}</span>
        <span class="d-none d-xl-inline">🔄 {{ t('nav.tradeIn') }}</span>
        <!-- Số hotline nổi bật màu vàng -->
        <span class="text-warning fw-black">{{ t('nav.hotline') }}</span>

        <!-- Đổi giao diện sáng/tối — dùng được kể cả khi chưa đăng nhập -->
        <button type="button" class="btn btn-sm p-0 border-0 lh-1"
                style="background:transparent; color:var(--text-secondary); font-size:14px;"
                :title="ThemeStore.mode === 'dark' ? t('theme.toggleToLight') : t('theme.toggleToDark')"
                :aria-label="ThemeStore.mode === 'dark' ? t('theme.toggleToLight') : t('theme.toggleToDark')"
                @click="toggleTheme">
          {{ ThemeStore.mode === 'dark' ? '🌙' : '☀️' }}
        </button>

        <!-- Đổi ngôn ngữ — dùng được kể cả khi chưa đăng nhập -->
        <select class="form-select form-select-sm fw-semibold py-0"
                style="width:auto; background:transparent; border:none; color:var(--text-secondary); font-size:11px; cursor:pointer;"
                :value="I18nStore.locale"
                :title="t('language.label')"
                :aria-label="t('language.label')"
                @change="setLocale($event.target.value)">
          <option v-for="loc in LOCALES" :key="loc.code" :value="loc.code" style="background:var(--bg-card); color:var(--text-primary);">
            {{ loc.flag }} {{ loc.code.toUpperCase() }}
          </option>
        </select>
      </div>
    </div>
  </div>

  <!-- Header chính: sticky ở trên khi scroll -->
  <header class="sticky-top" style="background:var(--bg-header); border-bottom:1px solid var(--border-color); z-index:100;">
    <div class="container-xl d-flex align-items-center gap-2 gap-md-3 py-2">

      <!-- Logo SAOPHONE -->
      <a href="/" class="text-decoration-none fw-black fs-5 me-1 flex-shrink-0"
         style="letter-spacing:-0.04em; color:var(--text-heading);">
        SAO<span class="text-warning">PHONE</span>
      </a>

      <!-- ── Nút Danh mục + Mega dropdown ── -->
      <div class="position-relative flex-shrink-0"
           @mouseenter="isMenuOpen = true"
           @mouseleave="isMenuOpen = false"
           @keydown.esc="closeMenu"
           @focusout="onMenuFocusOut">

        <!-- Nút trigger dropdown — click hoặc hover đều mở, bàn phím dùng được -->
        <button
          ref="menuTriggerRef"
          type="button"
          class="btn btn-sm fw-bold small"
          :class="isMenuOpen ? 'text-warning border-warning' : 'border-secondary'"
          style="border:1px solid; border-radius:12px; background:var(--bg-input); color:var(--text-primary);"
          aria-haspopup="true"
          :aria-expanded="isMenuOpen"
          @click="isMenuOpen = !isMenuOpen">
          ☰ <span class="d-none d-sm-inline">{{ t('nav.categories') }}</span>
        </button>

        <!-- Mega menu dropdown panel -->
        <div v-if="isMenuOpen"
             class="position-absolute top-100 start-0 mt-1 shadow-lg rounded-3 overflow-hidden"
             style="width:820px; background:var(--bg-page-alt); border:1px solid var(--border-color); z-index:200; height:340px; display:flex;">

          <!-- Cột trái: danh sách các loại laptop -->
          <div class="d-flex flex-column gap-1 p-2 flex-shrink-0 overflow-y-auto"
               style="width:280px; background:var(--bg-hover); border-right:1px solid var(--border-color);">
            <div
              v-for="cat in categories"
              :key="cat.id"
              tabindex="0"
              role="button"
              class="d-flex justify-content-between align-items-center px-3 py-2 rounded-2 small fw-bold"
              :class="activeCategory === cat.id ? 'text-warning' : ''"
              :style="(activeCategory === cat.id
                ? 'background:var(--bg-card); padding-left:1.25rem!important; cursor:pointer;'
                : 'cursor:pointer;') + (activeCategory === cat.id ? '' : ' color:var(--text-secondary);')"
              @mouseenter="activeCategory = cat.id"
              @focus="activeCategory = cat.id"
              @click="onCategoryClick(cat)">
              <span>{{ cat.title }}</span>
              <span style="font-size:14px; opacity:0.5;">›</span>
            </div>
          </div>

          <!-- Cột phải: hãng và phân khúc tương ứng -->
          <div class="p-4 flex-grow-1 overflow-y-auto" style="background:var(--bg-page-alt);">
            <template v-for="cat in categories" :key="cat.id">
              <div v-if="cat.id === activeCategory" class="row g-4">
                <!-- Cột hãng sản xuất -->
                <div class="col-6">
                  <div class="text-warning fw-black text-uppercase small pb-2 mb-2"
                       style="font-size:10px; letter-spacing:0.08em; border-bottom:1px solid var(--border-color);">
                    {{ t('nav.brandsHeading') }}
                  </div>
                  <button v-for="brand in cat.brands" :key="brand"
                     type="button"
                     class="d-flex align-items-center gap-1 text-decoration-none py-1 small fw-bold border-0 bg-transparent text-start"
                     style="font-size:12px; color:var(--text-secondary);"
                     @mouseenter="e => e.target.style.color='var(--text-heading)'"
                     @mouseleave="e => e.target.style.color=''"
                     @click="onBrandClick(brand)">
                    <span style="color:var(--border-color-strong);">·</span> {{ brand }}
                  </button>
                </div>
                <!-- Cột phân khúc nổi bật -->
                <div class="col-6">
                  <div class="text-warning fw-black text-uppercase small pb-2 mb-2"
                       style="font-size:10px; letter-spacing:0.08em; border-bottom:1px solid var(--border-color);">
                    {{ t('nav.tagsHeading') }}
                  </div>
                  <!-- Text mô tả marketing, không map được vào bộ lọc thật (không có
                       field dữ liệu tương ứng) — hiện dạng span thuần, không giả vờ
                       là link để tránh gây hiểu lầm có thể bấm được. -->
                  <span v-for="tag in cat.tags" :key="tag"
                     class="d-block py-1 small fw-bold"
                     style="font-size:12px; color:var(--text-secondary);">
                    {{ tag }}
                  </span>
                </div>
              </div>
            </template>
          </div>
        </div>
      </div><!-- /category dropdown -->

      <!-- Nút chọn thành phố (chỉ hiện trên màn to) -->
      <button class="btn btn-sm fw-bold d-none d-xl-flex align-items-center gap-2 flex-shrink-0"
              style="background:var(--bg-input); border:1px solid var(--border-color-strong); border-radius:12px; color:var(--text-primary);">
        📍
        <div class="text-start lh-1">
          <div style="font-size:10px; color:var(--text-muted);">{{ t('nav.changeCityLabel') }}</div>
          <div style="font-size:12px; font-weight:800;">{{ t('nav.city') }}</div>
        </div>
      </button>

      <!-- Thanh tìm kiếm - chiếm phần lớn không gian còn lại -->
      <div class="input-group flex-grow-1" style="min-width:0;">
        <input
          v-model="searchValue"
          type="text"
          class="form-control form-control-sm"
          style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-primary); border-radius:12px 0 0 12px; font-size:12px; font-weight:600; min-width:0;"
          :placeholder="t('nav.searchPlaceholder')"
          @keyup.enter="emit('search', searchValue)"
        />
        <!-- Nút kính lúp trigger tìm kiếm -->
        <button
          class="btn btn-sm"
          style="background:var(--bg-input); border-color:var(--border-color-strong); border-left:none; color:var(--text-secondary); border-radius:0 12px 12px 0;"
          @click="emit('search', searchValue)">
          🔎
        </button>
      </div>

      <!-- Nhóm nút bên phải: Giỏ hàng | Đăng nhập -->
      <div class="d-flex align-items-center gap-2 flex-shrink-0 ms-1">

        <!-- Nút giỏ hàng: phát ra sự kiện toggle-cart khi click -->
        <button class="btn btn-sm d-flex align-items-center gap-1 fw-bold"
                style="background:var(--bg-input); border:1px solid var(--border-color-strong); border-radius:12px; color:var(--text-primary); font-size:12px; white-space:nowrap;"
                @click="emit('toggle-cart')">
          🛒 <span class="d-none d-sm-inline">{{ t('nav.cart') }}</span>
          <!-- Badge hiển thị số lượng sản phẩm trong giỏ -->
          <span class="badge fw-black"
                style="background:var(--accent); color:var(--accent-text); border-radius:999px; font-size:11px;">
            {{ cartCount }}
          </span>
        </button>

        <!-- Đã đăng nhập: hiển thị tên (nhân viên → mở trang quản trị, khách → mở trang tài khoản) + nút đăng xuất -->
        <template v-if="user">
          <button
              class="d-flex align-items-center gap-1 px-3 py-1 rounded-pill fw-semibold border-0"
              style="background:var(--bg-input); border:1px solid var(--border-color-strong); color:var(--accent-fg); font-size:12px; white-space:nowrap; max-width:160px; overflow:hidden; text-overflow:ellipsis; cursor:pointer;"
              @click="emit(isStaff ? 'open-admin' : 'open-account')">
            <span style="font-size:10px;">●</span>
            {{ user.hoTen || user.username }}
          </button>
          <button class="btn btn-sm fw-bold"
                  style="background:var(--bg-input); border:1px solid #7f1d1d; border-radius:12px; color:#f87171; font-size:12px; white-space:nowrap;"
                  @click="emit('logout')">
            {{ t('nav.logout') }}
          </button>
        </template>

        <!-- Chưa đăng nhập: nút đăng nhập -->
        <button v-else class="btn btn-warning fw-bold" style="font-size:13px;" @click="emit('open-login')">
          {{ t('nav.login') }}
        </button>
      </div>

    </div><!-- /container-xl -->
  </header>
</template>

<script setup>
import { ref, computed } from 'vue';
import { t, I18nStore, LOCALES, setLocale } from '../../i18n/index.js';
import { ThemeStore, toggleTheme } from '../../stores/theme.js';

const emit = defineEmits(["toggle-cart", "search", "open-admin", "open-account", "open-login", "logout", "select-category"]);

const props = defineProps({
  cartCount: { type: Number, default: 0 },
  user:      { type: Object, default: null },
});

const STAFF_ROLES = ["admin", "nhan_vien", "quan_kho"];
const isStaff = computed(() => STAFF_ROLES.includes(props.user?.role));

// Trạng thái mở/đóng menu danh mục
const isMenuOpen = ref(false);
const menuTriggerRef = ref(null);

// Đóng menu + trả focus về nút trigger (Escape, hoặc focus rời khỏi cả cụm menu)
const closeMenu = () => {
  isMenuOpen.value = false;
  menuTriggerRef.value?.focus();
};
// Focus rời khỏi cụm trigger+panel (không phải chuyển sang phần tử con khác bên trong) → đóng
const onMenuFocusOut = (e) => {
  if (!e.currentTarget.contains(e.relatedTarget)) isMenuOpen.value = false;
};

// Danh mục đang được hover trong mega menu
const activeCategory = ref('all-laptop');

// Giá trị người dùng nhập vào ô tìm kiếm
const searchValue = ref('');

// Danh sách các loại laptop trong mega dropdown — tiêu đề VÀ tag (marketing copy) đều dịch
// theo t() (trước đây tag hardcode tiếng Việt, đổi ngôn ngữ vẫn hiện tiếng Việt giữa các nhãn
// đã dịch xung quanh). brands là tên hãng/thương hiệu — giữ nguyên, không dịch.
const categories = computed(() => [
  {
    id: 'all-laptop',
    title: t('nav.catAll'),
    brands: ['ASUS', 'Lenovo', 'MacBook (Apple)', 'MSI', 'Acer', 'HP', 'Dell', 'Gigabyte', 'LG'],
    tags: t('nav.catAllTags')
  },
  {
    id: 'gaming',
    title: t('nav.catGaming'),
    brands: ['ASUS ROG / TUF', 'Lenovo Legion / LOQ', 'MSI Gaming', 'Acer Predator / Nitro', 'Gigabyte Gaming', 'Dell Alienware'],
    tags: t('nav.catGamingTags')
  },
  {
    id: 'office',
    title: t('nav.catOffice'),
    brands: ['ASUS Vivobook', 'Lenovo IdeaPad', 'HP Pavilion / ProBook', 'Dell Inspiron', 'Acer Aspire'],
    tags: t('nav.catOfficeTags')
  },
  {
    id: 'premium',
    title: t('nav.catPremium'),
    brands: ['MacBook Air / Pro', 'ASUS Zenbook', 'Lenovo Yoga / Slim', 'HP Envy / Spectre', 'Dell XPS', 'LG Gram'],
    tags: t('nav.catPremiumTags')
  },
  {
    id: 'creator',
    title: t('nav.catCreator'),
    brands: ['MacBook Pro M-Series', 'ASUS ProArt', 'Lenovo ThinkPad P-Series', 'MSI Creator', 'Dell Precision'],
    tags: t('nav.catCreatorTags')
  }
]);

// Chuẩn hoá tên hãng/dòng máy thành từ khoá lọc — tách theo "/" vì mega-menu gộp
// chung các dòng máy anh em vào 1 chuỗi (vd "ASUS ROG / TUF"). Dùng chung logic
// substring-match đã có sẵn ở App.vue (matchesKw: name/brand/cat/tags.includes(kw)).
const toKeywords = (label) => label.toLowerCase().split('/').map(s => s.trim());

// Click cả danh mục (cột trái) → lọc theo TẤT CẢ hãng/dòng máy thuộc danh mục đó
const onCategoryClick = (cat) => {
  emit('select-category', {
    id: `mega-${cat.id}`,
    catId: null,
    keywords: [...new Set(cat.brands.flatMap(toKeywords))],
  });
  isMenuOpen.value = false;
};

// Click 1 hãng/dòng máy cụ thể (cột phải) → lọc chính xác hãng/dòng đó
const onBrandClick = (brand) => {
  emit('select-category', { id: `mega-brand-${brand}`, catId: null, keywords: toKeywords(brand) });
  isMenuOpen.value = false;
};
</script>

<!-- Không còn CSS scoped — toàn bộ giao diện dùng Bootstrap utility classes -->
