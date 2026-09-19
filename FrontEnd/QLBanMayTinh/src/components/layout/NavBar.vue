<template>
  <div class="sticky-top" style="z-index:100;">
    <!-- TopBanner: nền hồng giống menu, chữ trắng -->
    <div
      style="
        background: linear-gradient(135deg, #ec4899 0%, #db2777 50%, #be185d 100%);
        overflow: hidden;
        position: relative;
        height: 38px;
        display: flex;
        align-items: center;
      "
    >
      <div class="container-xl d-flex align-items-center w-100">
        <!-- Dòng chạy ưu đãi ở giữa -->
        <div class="overflow-hidden flex-grow-1" style="mask-image: linear-gradient(to right, transparent 0%, black 5%, black 90%, transparent 100%);">
          <div
            class="d-flex align-items-center marquee-track"
            style="white-space: nowrap;"
          >
            <span
              v-for="(item, idx) in [...promoItems, ...promoItems]"
              :key="idx"
              class="d-inline-flex align-items-center gap-2"
              style="font-size: 12px; font-weight: 600; color: #ffffff; padding: 0 24px; flex-shrink:0;"
            >
              <component :is="item.icon" :size="13" style="color:#fde047;" />
              {{ item.text }}
              <span style="color:rgba(255,255,255,0.6);">•</span>
            </span>
          </div>
        </div>

        <!-- Theme & Lang: cố định bên phải -->
        <div class="d-flex align-items-center gap-2 flex-shrink-0">
          <button
            type="button" class="btn btn-sm p-0 border-0 lh-1"
            style="background:rgba(255,255,255,0.2); color:#ffffff; font-size:14px; border-radius:4px; padding:2px 6px;"
            :title="ThemeStore.mode === 'dark' ? t('theme.toggleToLight') : t('theme.toggleToDark')"
            :aria-label="ThemeStore.mode === 'dark' ? t('theme.toggleToLight') : t('theme.toggleToDark')"
            @click="toggleTheme"
          >
            <component :is="ThemeStore.mode === 'dark' ? Moon : Sun" :size="16" />
          </button>

          <select
            class="form-select form-select-sm fw-semibold py-0"
            style="width:auto; background:rgba(255,255,255,0.9); border:1px solid rgba(255,255,255,0.5); color:#831843; font-size:11px; cursor:pointer; border-radius:6px;"
            :value="I18nStore.locale"
            :title="t('language.label')"
            :aria-label="t('language.label')"
            @change="setLocale($event.target.value)"
          >
            <option v-for="loc in LOCALES" :key="loc.code" :value="loc.code" style="background:var(--bg-card); color:var(--text-primary);">
              {{ loc.flag }} {{ loc.code.toUpperCase() }}
            </option>
          </select>
        </div>
      </div>
    </div>

    <!-- NavBar chính: nền hồng -->
    <header
      style="background:linear-gradient(135deg, #ec4899 0%, #db2777 50%, #be185d 100%); box-shadow:0 4px 14px rgba(190,24,93,0.4);"
    >
      <div class="container-xl d-flex align-items-center gap-2 gap-md-3 py-2">
        <!-- Logo SVG: chuyên nghiệp với hiệu ứng 3D -->
        <a
          href="/"
          class="flex-shrink-0 d-flex align-items-center logo-3d"
          style="text-decoration: none;"
        >
          <img
            src="/images/logo.svg"
            alt="SAOCLUB"
            style="height: 42px; width: auto; transition: transform 0.3s ease, filter 0.3s ease;"
          />
        </a>

        <div
          class="position-relative flex-shrink-0"
          @mouseenter="isMenuOpen = true"
          @mouseleave="isMenuOpen = false"
          @keydown.esc="closeMenu"
          @focusout="onMenuFocusOut"
        >
          <!-- Nút danh mục: trắng đục trên nền hồng -->
          <button
            ref="menuTriggerRef"
            type="button"
            class="btn btn-sm fw-bold small"
            :class="isMenuOpen ? 'border-white' : 'border-white'"
            style="border:1px solid rgba(255,255,255,0.6); border-radius:12px; background:rgba(255,255,255,0.95); color:#be185d; font-size:1em; font-weight:800;"
            aria-haspopup="true"
            :aria-expanded="isMenuOpen"
            @click="isMenuOpen = !isMenuOpen"
          >
            <Menu :size="22" style="vertical-align:-3px;" /> <span class="d-none d-sm-inline">Danh mục</span>
          </button>

          <!-- Mega dropdown — nền trắng, viền hồng -->
          <div
            v-if="isMenuOpen"
            class="position-absolute top-100 start-0 mt-1 shadow-lg rounded-3 overflow-hidden navbar-mega"
            style="width:900px; background:#ffffff; border:1px solid #f9a8d4; z-index:200; height:380px; display:flex;"
          >
            <!-- Cột trái: danh sách danh mục -->
            <div
              class="d-flex flex-column gap-1 p-3 flex-shrink-0 overflow-y-auto"
              style="width:300px; background:#fff0f6; border-right:1px solid #fce7f3;"
            >
              <div
                v-for="cat in categories"
                :key="cat.id"
                tabindex="0"
                role="button"
                class="d-flex justify-content-between align-items-center px-4 py-3 rounded-2 mega-category"
                :style="activeCategory === cat.id
                  ? 'background:#ffffff; padding-left:1.5rem; cursor:pointer; box-shadow:2px 0 8px rgba(236,72,153,0.15); color:#dc2626; font-weight:800;'
                  : 'cursor:pointer; color:#1e293b; font-weight:700;'"
                style="font-size:1em;"
                @mouseenter="activeCategory = cat.id"
                @focus="activeCategory = cat.id"
                @click="onCategoryClick(cat)"
              >
                <span>{{ cat.title }}</span>
                <span style="font-size:16px; color:#ec4899;">›</span>
              </div>
            </div>

            <!-- Cột phải: brands + tags -->
            <div class="p-4 flex-grow-1 overflow-y-auto" style="background:#ffffff;">
              <template v-for="cat in categories" :key="cat.id">
                <div v-if="cat.id === activeCategory" class="row g-4">
                  <div class="col-6">
                    <div
                      class="fw-black text-uppercase small pb-2 mb-3 mega-heading"
                      style="font-size:1em; letter-spacing:0.08em; border-bottom:1px solid #fce7f3; color:#be185d; font-weight:800;"
                    >
                      {{ t('nav.brandsHeading') }}
                    </div>
                    <button
                      v-for="brand in cat.brands" :key="brand"
                      type="button"
                      class="d-flex align-items-center gap-1 text-decoration-none py-2 small fw-bold border-0 bg-transparent text-start mega-item"
                      style="font-size:1em; color:#1e293b; font-weight:700;"
                      @mouseenter="e => e.target.style.color='#be185d'"
                      @mouseleave="e => e.target.style.color='#1e293b'"
                      @click="onBrandClick(brand)"
                    >
                      <span style="color:#f9a8d4;">·</span> {{ brand }}
                    </button>
                  </div>
                  <div class="col-6">
                    <div
                      class="fw-black text-uppercase small pb-2 mb-3 mega-heading"
                      style="font-size:1em; letter-spacing:0.08em; border-bottom:1px solid #fce7f3; color:#be185d; font-weight:800;"
                    >
                      {{ t('nav.tagsHeading') }}
                    </div>
                    <span
                      v-for="tag in cat.tags" :key="tag"
                      class="d-block py-2 mega-tag"
                      style="font-size:1em; color:#1e293b; font-weight:700;"
                    >
                      {{ tag }}
                    </span>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </div><!-- /category dropdown -->

        <!-- Thanh tìm kiếm: trắng đục trên nền hồng -->
        <div class="input-group flex-grow-1" style="min-width:0;">
          <input
            v-model="searchValue"
            type="text"
            class="form-control form-control-sm"
            style="background:#ffffff; border-color:rgba(255,255,255,0.8); color:#831843; border-radius:12px 0 0 12px; font-size:1em; font-weight:700; min-width:0;"
            :placeholder="t('nav.searchPlaceholder')"
            @keyup.enter="emit('search', searchValue)"
          />
          <!-- Nút kính lúp: hồng đậm nổi bật -->
          <button
            class="btn btn-sm"
            style="background:#be185d; border-color:#be185d; border-left:none; color:#ffffff; border-radius:0 12px 12px 0;"
            @click="emit('search', searchValue)"
          >
            <Search :size="20" />
          </button>
        </div>

        <!-- Nhóm nút bên phải: Giỏ hàng | Đăng nhập -->
        <div class="d-flex align-items-center gap-2 flex-shrink-0 ms-1">
          <!-- Nút giỏ hàng: trắng đục, badge hồng đậm -->
          <button
            class="btn btn-sm d-flex align-items-center gap-1 fw-bold"
            style="background:rgba(255,255,255,0.95); border:1px solid rgba(255,255,255,0.7); border-radius:12px; color:#831843; font-size:1em; white-space:nowrap;"
            @click="emit('toggle-cart')"
          >
            <ShoppingCart :size="22" style="vertical-align:-3px;" /> <span class="d-none d-sm-inline">{{ t('nav.cart') }}</span>
            <span
              class="badge fw-black"
              style="background:#be185d; color:#ffffff; border-radius:999px; font-size:1em;"
            >
              {{ cartCount }}
            </span>
          </button>

          <!-- Đã đăng nhập: avatar khách hàng - click đến trang tài khoản -->
          <template v-if="user">
            <a
              :href="isStaff ? '#' : '/account'"
              class="d-flex align-items-center justify-content-center rounded-pill border-0 overflow-hidden"
              style="width:40px; height:40px; background:rgba(255,255,255,0.95); cursor:pointer; border:2px solid rgba(255,255,255,0.8); text-decoration:none;"
              :title="user.hoTen || user.username"
              @click.prevent="emit(isStaff ? 'open-admin' : 'open-account')"
            >
              <img
                v-if="user.hinhAnh"
                :src="user.hinhAnh"
                :alt="user.hoTen || user.username"
                style="width:100%; height:100%; object-fit:cover;"
              />
              <span v-else class="fw-bold" style="font-size:16px; color:#be185d;">
                {{ (user.hoTen || user.username || 'U').charAt(0).toUpperCase() }}
              </span>
            </a>
          </template>

          <!-- Chưa đăng nhập: nút vàng 3D nổi bật -->
          <button v-else class="btn-nav-3d" style="font-size:1em;" @click="emit('open-login')">
            {{ t('nav.login') }}
          </button>
        </div>
      </div><!-- /container-xl -->
    </header>
  </div>
</template>

<style scoped>
/* ── Logo 3D hover effect ── */
.logo-3d:hover img {
  transform: translateY(-3px) scale(1.05);
  filter: drop-shadow(0 8px 16px rgba(233, 30, 99, 0.5));
}

/* ── Hiệu ứng nút 3D trong NavBar ── */
.btn-nav-3d {
  border: none;
  border-radius: 12px;
  padding: 10px 18px;
  font-size: 1em;
  font-weight: 800;
  cursor: pointer;
  background: linear-gradient(180deg, #fde047 0%, #facc15 50%, #f59e0b 100%);
  color: #7c2d12;
  box-shadow: 0 3px 0 #b45309, 0 4px 10px rgba(245, 158, 11, 0.4);
  border-bottom: 3px solid #b45309;
  transition: all 0.15s ease;
  font-family: inherit;
}
.btn-nav-3d:hover:not(:disabled) {
  background: linear-gradient(180deg, #fef08a 0%, #fde047 50%, #facc15 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 0 #b45309, 0 6px 14px rgba(245, 158, 11, 0.5);
}
.btn-nav-3d:active:not(:disabled) {
  transform: translateY(2px);
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.15);
  border-bottom-width: 0;
  padding-bottom: 13px;
}
.btn-nav-3d:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ── Mega dropdown text overrides ── */
.navbar-mega .mega-item { color: #1e293b !important; font-weight:700; }
.navbar-mega .mega-item:hover { color: #be185d !important; }
.navbar-mega .mega-heading { color: #be185d !important; font-weight:800; }
.navbar-mega .mega-tag { color: #1e293b !important; font-weight:700; }
.navbar-mega .mega-category { color: #1e293b !important; font-weight:700; }
.navbar-mega .mega-category.active { color: #dc2626 !important; font-weight:800; }

/* ── Animation chạy ngang từ phải sang trái liên tục ── */
.marquee-track {
  display: flex;
  width: max-content;
  animation: marquee 30s linear infinite;
}

@keyframes marquee {
  0% { transform: translateX(0); }
  100% { transform: translateX(-50%); }
}
</style>

<script setup>
import { ref, computed } from 'vue';
import { Moon, Sun, Menu, Search, ShoppingCart, ShieldCheck, Truck, RefreshCw, Tag, Sparkles } from '@lucide/vue';
import { t, I18nStore, LOCALES, setLocale } from '../../i18n/index.js';
import { ThemeStore, toggleTheme } from '../../stores/theme.js';

const emit = defineEmits(["toggle-cart", "search", "open-admin", "open-account", "open-login", "logout", "select-category"]);

const props = defineProps({
  cartCount: { type: Number, default: 0 },
  user:      { type: Object, default: null },
});

// Ưu đãi chạy ngang từ phải sang trái
const promoItems = [
  { icon: ShieldCheck, text: 'Sản phẩm Chính hãng - Xuất VAT đầy đủ' },
  { icon: Truck, text: 'Giao nhanh - Miễn phí cho đơn 300k' },
  { icon: RefreshCw, text: 'Thu cũ giá ngon - Lên đời tiết kiệm' },
  { icon: Tag, text: 'Trả góp 0% lãi suất - Duyệt nhanh 15 phút' },
  { icon: Sparkles, text: 'Giảm thêm 5% cho sinh viên - Mã SV2024' },
  { icon: ShoppingCart, text: 'Miễn phí cài đặt phần mềm trọn đời' },
];

const STAFF_ROLES = ["admin", "nhan_vien", "quan_kho"];
const isStaff = computed(() => STAFF_ROLES.includes(props.user?.role));

// Trạng thái mở/đóng menu danh mục
const isMenuOpen = ref(false);
const menuTriggerRef = ref(null);

// Đóng menu + trả focus về nút trigger
const closeMenu = () => {
  isMenuOpen.value = false;
  menuTriggerRef.value?.focus();
};
// Focus rời khỏi cụm trigger+panel → đóng
const onMenuFocusOut = (e) => {
  if (!e.currentTarget.contains(e.relatedTarget)) isMenuOpen.value = false;
};

// Danh mục đang được hover trong mega menu
const activeCategory = ref('all-laptop');

// Giá trị người dùng nhập vào ô tìm kiếm
const searchValue = ref('');

// Danh sách các loại laptop trong mega dropdown
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

// Chuẩn hoá tên hãng/dòng máy thành từ khoá lọc
const toKeywords = (label) => label.toLowerCase().split('/').map(s => s.trim());

// Click cả danh mục (cột trái)
const onCategoryClick = (cat) => {
  emit('select-category', {
    id: `mega-${cat.id}`,
    catId: null,
    keywords: [...new Set(cat.brands.flatMap(toKeywords))],
  });
  isMenuOpen.value = false;
};

// Click 1 hãng/dòng máy cụ thể (cột phải)
const onBrandClick = (brand) => {
  emit('select-category', { id: `mega-brand-${brand}`, catId: null, keywords: toKeywords(brand) });
  isMenuOpen.value = false;
};
</script>