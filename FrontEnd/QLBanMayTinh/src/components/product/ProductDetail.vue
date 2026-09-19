<template>
  <div
    class="position-fixed start-0 w-100"
    style="top: 0px; height: 100vh; background: #FFFFFF; z-index: 99999; overflow-y: auto;"
  >
    <!-- ── NavBar ── -->
    <NavBar
      :cart-count="cartCount"
      :user="authUser"
      @toggle-cart="$emit('toggle-cart')"
      @search="$emit('search', $event)"
      @open-admin="$emit('open-admin')"
      @open-account="$emit('open-account')"
      @open-login="$emit('open-login')"
      @logout="$emit('logout')"
    />

    <!-- ── Header Breadcrumb ── -->
    <div
      class="d-flex align-items-center gap-2 px-4 py-3 position-sticky"
      style="top: 0; background: #FFFFFF; border-bottom: 1px solid #E5E5E7; z-index: 10;"
    >
      <nav aria-label="breadcrumb" class="flex-grow-1" style="font-size: 13px;">
        <ol class="breadcrumb mb-0 align-items-center">
          <li class="breadcrumb-item">
            <a
              href="/"
              @click.prevent="goHome"
              class="d-inline-flex align-items-center gap-1 text-decoration-none"
              style="color: #777777; cursor: pointer; line-height: 1;"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#D40F28" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
              <span style="font-weight: 500;">Trang chủ</span>
            </a>
          </li>
          <li v-if="activeVariant.tenThuongHieu" class="breadcrumb-item">
            <a
              href="/"
              @click.prevent="goHome"
              class="text-decoration-none"
              style="color: #777777; cursor: pointer;"
            >{{ activeVariant.tenThuongHieu }}</a>
          </li>
          <li class="breadcrumb-item active" aria-current="page" style="color: #333333; font-weight: 500;">
            {{ activeVariant.tenSanPham }}
          </li>
        </ol>
      </nav>
    </div>

    <!-- ── Nội dung chính ── -->
    <div class="container-xl py-4 px-4">
      <div class="row g-4">
        <!-- ════════════ CỘT TRÁI: Ảnh sản phẩm ════════════ -->
        <div class="col-12 col-lg-6">
          <!-- Ảnh chính -->
          <div
            class="rounded-2 d-flex align-items-center justify-content-center mb-3"
            style="background: #F5F5F7; min-height: 400px; padding: 32px;"
          >
            <img
              v-if="displayedImage"
              :src="displayedImage"
              :alt="activeVariant.tenSanPham"
              style="max-width: 100%; max-height: 400px; object-fit: contain;"
            />
            <svg width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" style="opacity:0.3;"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="2" y1="20" x2="22" y2="20"/></svg>
          </div>

          <!-- Gallery carousel ngang -->
          <div v-if="galleryImages.length > 1" class="d-flex gap-2" style="overflow-x: auto; padding-bottom: 8px;">
            <button
              v-for="(url, i) in galleryImages"
              :key="i"
              type="button"
              class="flex-shrink-0 rounded"
              :style="activeImageIndex === i
                ? 'width: 70px; height: 70px; border: 2px solid #D40F28; padding: 3px;'
                : 'width: 70px; height: 70px; border: 1px solid #E5E5E7; padding: 3px;'"
              style="cursor: pointer; background: #F5F5F7; transition: all 0.15s;"
              @click="activeImageIndex = i"
            >
              <img :src="url" alt="" style="width: 100%; height: 100%; object-fit: cover; border-radius: 4px;" />
            </button>
          </div>
        </div>

        <!-- ════════════ CỘT PHẢI: Thông tin sản phẩm ════════════ -->
        <div class="col-12 col-lg-6 d-flex flex-column">

          <!-- Tên sản phẩm -->
          <div class="mb-3">
            <p class="mb-1" style="font-size: 13px; color: #777777; text-transform: uppercase; letter-spacing: 0.03em;">
              {{ activeVariant.tenThuongHieu }}
            </p>
            <h1 class="mb-0" style="font-size: 1.4rem; line-height: 1.4; color: #333333; font-weight: 600;">
              {{ activeVariant.tenSanPham }}
            </h1>
          </div>

          <!-- SKU & Bảo hành -->
          <div class="d-flex gap-3 mb-3" style="font-size: 12px; color: #777777;">
            <span v-if="activeVariant.baoHanhThang">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#D40F28" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
              Bảo hành: <strong style="color: #333333;">{{ activeVariant.baoHanhThang }} tháng</strong>
            </span>
          </div>

          <!-- Giá tiền nổi bật -->
          <div class="mb-3 p-3 rounded" style="background: #F5F5F7;">
            <div class="d-flex align-items-baseline gap-3 flex-wrap">
              <span style="font-size: 2rem; font-weight: 700; color: #D40F28;">
                {{ formatPrice(activeVariant.giaBan) }}
              </span>
              <span
                v-if="activeVariant.giaGoc && activeVariant.giaGoc > activeVariant.giaBan"
                class="text-decoration-line-through"
                style="color: #999999; font-size: 1rem;"
              >
                {{ formatPrice(activeVariant.giaGoc) }}
              </span>
            </div>
            <div style="font-size: 12px; color: #777777;">
              ✓ Đã bao gồm VAT. Miễn phí giao hàng toàn quốc.
            </div>
          </div>

          <!-- Chọn Phiên bản -->
          <div v-if="configs.length > 1" class="mb-3">
            <div class="fw-bold mb-2" style="font-size: 13px; color: #333333;">
              Phiên bản cấu hình
            </div>
            <div class="d-flex flex-wrap gap-2">
              <button
                v-for="v in configs"
                :key="configKey(v)"
                type="button"
                class="btn d-flex flex-column align-items-start text-start px-3 py-2"
                style="border-radius: 8px; min-width: 150px; font-size: 12px; transition: all 0.15s;"
                :style="activeConfigKey === configKey(v)
                  ? 'background: #FFFFFF; border: 2px solid #D40F28; color: #D40F28; box-shadow: 0 2px 8px rgba(212,15,40,0.15);'
                  : 'background: #FFFFFF; border: 1px solid #E5E5E7; color: #333333;'"
                @click="selectConfig(v)"
              >
                <span class="fw-semibold">{{ configLabel(v).line1 }}</span>
                <span v-if="configLabel(v).line2" style="color: #777777; font-size: 11px;">{{ configLabel(v).line2 }}</span>
              </button>
            </div>
          </div>

          <!-- Chọn Màu sắc -->
          <div v-if="colorsForConfig.some(v => v.mauSac)" class="mb-3">
            <div class="fw-bold mb-2" style="font-size: 13px; color: #333333;">
              Màu sắc: <span style="color: #D40F28;">{{ activeVariant.mauSac }}</span>
            </div>
            <div class="d-flex flex-wrap gap-2">
              <button
                v-for="v in colorsForConfig"
                :key="v.bienTheId"
                type="button"
                class="d-flex align-items-center gap-2 px-3 py-2 rounded"
                style="font-size: 12px; transition: all 0.15s; background: #FFFFFF;"
                :style="activeColor === v.mauSac
                  ? 'border: 2px solid #D40F28; color: #D40F28; box-shadow: 0 2px 8px rgba(212,15,40,0.15);'
                  : 'border: 1px solid #E5E5E7; color: #333333;'"
                @click="selectColor(v)"
              >
                <span
                  class="rounded-circle"
                  :style="`width: 16px; height: 16px; background: ${colorDot(v.mauSac)}; border: 1px solid #DDD;`"
                ></span>
                <span class="fw-medium">{{ v.mauSac }}</span>
              </button>
            </div>
          </div>

          <!-- Thông số kỹ thuật -->
          <div v-if="specGroups.phancung.length || specGroups.hethong.length" class="mb-3">
            <div class="fw-bold mb-2" style="font-size: 13px; color: #333333;">
              Thông số kỹ thuật
            </div>
            <div class="rounded" style="background: #FFFFFF; border: 1px solid #E5E5E7;">
              <table class="w-100 mb-0" style="border-collapse: collapse; font-size: 12px;">
                <tbody>
                  <tr
                    v-for="s in [...specGroups.phancung, ...specGroups.hethong].slice(0, 6)"
                    :key="s.label"
                    style="border-bottom: 1px solid #F0F0F0;"
                  >
                    <td class="px-3 py-2" style="width: 40%; color: #777777;">{{ s.label }}</td>
                    <td class="px-3 py-2" style="color: #333333; font-weight: 500;">{{ s.value }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- Nút Thêm vào giỏ hàng - MÀU HỒNG NHƯ MENU -->
          <div class="mt-auto">
            <button
              class="btn w-100 py-3 d-flex align-items-center justify-content-center gap-2"
              style="background: linear-gradient(135deg, #f43f5e 0%, #e11d48 100%); color: #FFFFFF; border-radius: 8px; font-size: 15px; font-weight: 700; border: none; transition: all 0.2s; box-shadow: 0 4px 12px rgba(244,63,94,0.35);"
              :disabled="isOutOfStock"
              @mouseenter="e => { e.currentTarget.style.transform = 'translateY(-2px)'; e.currentTarget.style.boxShadow = '0 6px 16px rgba(244,63,94,0.45)'; }"
              @mouseleave="e => { e.currentTarget.style.transform = 'translateY(0)'; e.currentTarget.style.boxShadow = '0 4px 12px rgba(244,63,94,0.35)'; }"
              @click="$emit('add-to-cart', activeVariant)"
            >
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>
              THÊM VÀO GIỎ HÀNG
            </button>
            <p v-if="isOutOfStock" class="text-center mt-2 mb-0" style="font-size: 12px; color: #D40F28;">
              Sản phẩm hiện hết hàng
            </p>
          </div>
        </div>
      </div>

      <!-- ── Khối Cam kết & Chính sách ── -->
      <div class="mt-4 p-4 rounded" style="background: #F5F5F7; border: 1px solid #E5E5E7;">
        <h3 class="fw-bold mb-3" style="font-size: 14px; color: #333333;">
          ✨ Cam kết mua hàng
        </h3>
        <div class="row g-3">
          <div class="col-md-4">
            <div class="d-flex align-items-start gap-2">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#D40F28" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>
              <div>
                <div class="fw-medium" style="font-size: 13px; color: #333333;">Bộ sản phẩm</div>
                <div style="font-size: 12px; color: #777777;">Hộp, Sách hướng dẫn, Cáp/Sạc, Máy chính</div>
              </div>
            </div>
          </div>
          <div class="col-md-4">
            <div class="d-flex align-items-start gap-2">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#D40F28" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
              <div>
                <div class="fw-medium" style="font-size: 13px; color: #333333;">Bảo hành chính hãng</div>
                <div style="font-size: 12px; color: #777777;">{{ activeVariant.baoHanhThang || 12 }} tháng tại trung tâm ủy quyền</div>
              </div>
            </div>
          </div>
          <div class="col-md-4">
            <div class="d-flex align-items-start gap-2">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#D40F28" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="3" width="15" height="13"/><polygon points="16 8 20 8 23 11 23 16 16 16 16 8"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg>
              <div>
                <div class="fw-medium" style="font-size: 13px; color: #333333;">Giao hàng miễn phí</div>
                <div style="font-size: 12px; color: #777777;">Toàn quốc nhanh chóng</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ── Mô tả sản phẩm ── -->
      <div v-if="activeVariant.moTa" class="mt-4 p-4 rounded" style="background: #FFFFFF; border: 1px solid #E5E5E7;">
        <h3 class="fw-bold mb-3" style="font-size: 14px; color: #333333;">
          📝 Mô tả sản phẩm
        </h3>
        <div style="font-size: 13px; line-height: 1.8; color: #555555; white-space: pre-wrap;">
          {{ activeVariant.moTa }}
        </div>
      </div>

      <!-- ── Sản phẩm gợi ý - KHÔNG VIỀN MẶC ĐỊNH ── -->
      <div v-if="related.length > 0" class="mt-4">
        <h3 class="fw-bold mb-3" style="font-size: 14px; color: #333333;">
          ⚡ Có thể bạn cũng thích
        </h3>
        <div class="d-flex gap-3 pb-2" style="overflow-x: auto;">
          <div
            v-for="p in related"
            :key="p.sanPhamId"
            class="flex-shrink-0 rounded d-flex flex-column"
            style="width: 180px; background: #FFFFFF; cursor: pointer; transition: all 0.2s; border: 1px solid transparent;"
            @mouseenter="e => { e.currentTarget.style.boxShadow = '0 4px 16px rgba(0,0,0,0.12)'; e.currentTarget.style.borderColor = '#D40F28'; }"
            @mouseleave="e => { e.currentTarget.style.boxShadow = 'none'; e.currentTarget.style.borderColor = 'transparent'; }"
            @click="$emit('open-product', p)"
          >
            <div
              class="d-flex align-items-center justify-content-center rounded-top"
              style="height: 120px; background: #F5F5F7; padding: 12px;"
            >
              <img
                v-if="p.hinhAnhChinh"
                :src="p.hinhAnhChinh"
                :alt="p.tenSanPham"
                style="max-width: 100%; max-height: 100px; object-fit: contain;"
              />
              <span v-else style="font-size: 40px; opacity: 0.3;">💻</span>
            </div>
            <div class="p-2 d-flex flex-column gap-1">
              <p
                class="mb-0 text-truncate"
                style="font-size: 12px; color: #333333; font-weight: 500;"
              >
                {{ p.tenSanPham }}
              </p>
              <p style="font-size: 14px; font-weight: 700; color: #D40F28; margin: 0;">
                {{ formatPrice(p.giaBan) }}
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- ── Đánh giá sản phẩm ── -->
      <div class="mt-4 p-4 rounded" style="background: #FFFFFF; border: 1px solid #E5E5E7;">
        <h3 class="fw-bold mb-3" style="font-size: 14px; color: #333333;">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="#D40F28" stroke="#D40F28" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg> Đánh giá sản phẩm
          <span v-if="avgRating != null" style="font-weight: 400; font-size: 13px; color: #777777;">
            · {{ avgRating.toFixed(1) }}/5 ({{ reviews.length }} đánh giá)
          </span>
        </h3>

        <!-- Form đánh giá -->
        <div v-if="authUser && !myReview" class="mb-4 p-3 rounded" style="background: #F5F5F7;">
          <div class="d-flex align-items-center gap-1 mb-2">
            <button
              v-for="n in 5"
              :key="n"
              type="button"
              class="btn p-0"
              style="background: transparent; border: none;"
              @click="newSoSao = n"
            >
              <svg width="24" height="24" viewBox="0 0 24 24" :fill="n <= newSoSao ? '#D40F28' : 'none'" :stroke="n <= newSoSao ? '#D40F28' : '#CCC'" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
            </button>
          </div>
          <textarea
            v-model="newNoiDung"
            class="form-control mb-2"
            rows="3"
            maxlength="1000"
            placeholder="Chia sẻ cảm nhận của bạn về sản phẩm..."
            style="font-size: 13px; border-radius: 6px; border: 1px solid #E5E5E7;"
          ></textarea>
          <div v-if="reviewError" class="small mb-2" style="color: #D40F28;">{{ reviewError }}</div>
          <button
            class="btn px-4 py-2"
            style="background: linear-gradient(135deg, #f43f5e 0%, #e11d48 100%); color: #FFFFFF; border: none; border-radius: 6px; font-weight: 600; font-size: 13px;"
            :disabled="submittingReview"
            @click="submitReview"
          >
            Gửi đánh giá
          </button>
        </div>

        <div v-if="!authUser" class="mb-4" style="font-size: 13px; color: #777777;">
          Vui lòng <a href="#" @click.prevent="$emit('open-login')" style="color: #0066CC;">đăng nhập</a> để đánh giá sản phẩm.
        </div>

        <!-- Đánh giá của tôi -->
        <div
          v-if="myReview"
          class="mb-4 p-3 rounded d-flex justify-content-between align-items-start gap-3"
          style="background: rgba(244,63,94,0.05); border: 1px solid rgba(244,63,94,0.2);"
        >
          <div>
            <div class="fw-medium mb-1" style="font-size: 13px; color: #333333;">
              Đánh giá của bạn
              <span class="ms-1" style="opacity: 0.7;">
                <span style="font-size: 14px;" v-for="n in myReview.soSao" :key="n"><svg width="14" height="14" viewBox="0 0 24 24" fill="#D40F28" stroke="#D40F28" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg></span>
              </span>
            </div>
            <div v-if="myReview.noiDung" style="font-size: 12px; color: #555555;">{{ myReview.noiDung }}</div>
          </div>
          <button
            class="btn btn-sm"
            style="border: 1px solid #D40F28; color: #D40F28; background: transparent; font-size: 12px;"
            @click="deleteMyReview"
          >
            Xóa
          </button>
        </div>

        <!-- Loading -->
        <div v-if="reviewsLoading" style="font-size: 13px; color: #777777;">
          Đang tải đánh giá...
        </div>

        <!-- Empty state -->
        <div v-else-if="reviews.length === 0 && !myReview" style="font-size: 13px; color: #777777;">
          Chưa có đánh giá nào cho sản phẩm này.
        </div>

        <!-- Danh sách đánh giá -->
        <div v-else class="d-flex flex-column gap-3">
          <div
            v-for="r in pagedReviews"
            :key="r.danhGiaId"
            class="pb-3"
            style="border-bottom: 1px solid #F0F0F0;"
          >
            <div class="d-flex justify-content-between align-items-center mb-1">
              <span class="fw-medium" style="font-size: 13px; color: #333333;">{{ r.tenKhachHang }}</span>
              <span style="opacity: 0.8;">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="#D40F28" stroke="#D40F28" stroke-width="2" v-for="n in r.soSao" :key="n"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
              </span>
            </div>
            <div v-if="r.noiDung" style="font-size: 12px; color: #555555;">{{ r.noiDung }}</div>
          </div>
          <Pagination
            v-if="reviewsTotalPages > 1"
            :current-page="reviewsPage"
            :total-pages="reviewsTotalPages"
            @page-change="reviewsPage = $event"
          />
        </div>
      </div>
    </div>

    <!-- ── Footer ── -->
    <AppFooter @open-register="$emit('open-register')" />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import { t } from '../../i18n/index.js';
import { formatPrice as formatPriceRaw } from '../../utils/formatPrice.js';
import { configKey, configLabel, colorDot } from '../../utils/productGrouping.js';
import * as DanhGiaService from '../../services/DanhGiaService.js';
import * as SanPhamService from '../../services/SanPhamService.js';
import Pagination from '../common/Pagination.vue';
import { usePagination } from '../../composables/usePagination.js';
import NavBar from '@/components/layout/NavBar.vue';
import AppFooter from '@/components/layout/Footer.vue';
import { useRouter } from 'vue-router';

const router = useRouter();

const goHome = () => {
  emit('close');
  router.push('/');
};

const props = defineProps({
  product: { type: Object, required: true },
  products: { type: Array, default: () => [] },
  wishlistIds: { type: Set, default: () => new Set() },
  authUser: { type: Object, default: null },
  cartCount: { type: Number, default: 0 },
});

const emit = defineEmits([
  'close', 'add-to-cart', 'open-product', 'toggle-wishlist',
  'toggle-cart', 'search', 'open-admin', 'open-account',
  'open-login', 'logout', 'open-register',
]);

// Variants
const variants = computed(() =>
  props.products.filter(p => p.sanPhamId === props.product.sanPhamId)
);

const activeConfigKey = ref(configKey(props.product));
const activeColor = ref(props.product.mauSac ?? '');

watch(() => props.product, (p) => {
  activeConfigKey.value = configKey(p);
  activeColor.value = p.mauSac ?? '';
});

const configs = computed(() => {
  const seen = new Set();
  return variants.value.filter(v => {
    const k = configKey(v);
    if (seen.has(k)) return false;
    seen.add(k);
    return true;
  });
});

const colorsForConfig = computed(() => {
  const seen = new Set();
  return variants.value
    .filter(v => configKey(v) === activeConfigKey.value)
    .filter(v => {
      const c = v.mauSac ?? '';
      if (seen.has(c)) return false;
      seen.add(c);
      return true;
    });
});

const activeVariant = computed(() =>
  variants.value.find(v =>
    configKey(v) === activeConfigKey.value &&
    (v.mauSac ?? '') === activeColor.value
  ) ?? props.product
);

const isWishlisted = computed(() =>
  props.wishlistIds.has(activeVariant.value.bienTheId)
);

// Gallery
const galleryExtra = ref([]);
const activeImageIndex = ref(0);

const loadGallery = async (sanPhamId) => {
  activeImageIndex.value = 0;
  try {
    galleryExtra.value = await SanPhamService.getHinhAnh(sanPhamId);
  } catch {
    galleryExtra.value = [];
  }
};

watch(() => props.product.sanPhamId, (id) => loadGallery(id), { immediate: true });

const galleryImages = computed(() => {
  const anh = activeVariant.value.hinhAnhChinh;
  const list = anh ? [anh, ...galleryExtra.value.filter((u) => u !== anh)] : galleryExtra.value;
  return list;
});

const displayedImage = computed(() =>
  galleryImages.value[activeImageIndex.value] ?? activeVariant.value.hinhAnhChinh
);

// Stock
const LOW_STOCK_THRESHOLD = 5;

const isOutOfStock = computed(() => {
  return activeVariant.value.trangThai !== 'active' || (activeVariant.value.soLuongTon ?? 0) <= 0;
});

const stockBadgeClass = computed(() => {
  if (isOutOfStock.value) return 'bg-secondary';
  if (activeVariant.value.soLuongTon <= LOW_STOCK_THRESHOLD) return 'bg-warning text-dark';
  return 'bg-success';
});

const stockBadgeText = computed(() => {
  const soLuong = activeVariant.value.soLuongTon ?? 0;
  if (isOutOfStock.value) return 'Hết hàng';
  if (soLuong <= LOW_STOCK_THRESHOLD) return `Chỉ còn ${soLuong} máy`;
  return `Còn ${soLuong} máy`;
});

const selectConfig = (v) => {
  activeConfigKey.value = configKey(v);
  const available = variants.value.filter(vv => configKey(vv) === activeConfigKey.value);
  if (!available.find(vv => (vv.mauSac ?? '') === activeColor.value))
    activeColor.value = available[0]?.mauSac ?? '';
};

const selectColor = (v) => {
  activeColor.value = v.mauSac ?? '';
};

// Related
const related = computed(() => {
  const seen = new Set();
  return props.products
    .filter(p =>
      p.sanPhamId !== props.product.sanPhamId &&
      (p.tenDanhMuc === props.product.tenDanhMuc || p.tenThuongHieu === props.product.tenThuongHieu)
    )
    .filter(p => {
      if (seen.has(p.sanPhamId)) return false;
      seen.add(p.sanPhamId);
      return true;
    })
    .slice(0, 8);
});

const formatPrice = (v) => (v == null ? 'Liên hệ' : formatPriceRaw(v));

// Spec groups
const row = (label, value) => (value ? { label, value } : null);
const specGroups = computed(() => {
  const v = activeVariant.value ?? {};
  const f = (arr) => arr.filter(Boolean);
  return {
    phancung: f([
      row('CPU', v.cpu),
      row('RAM', v.ram),
      row('Ổ cứng', v.oCung),
      row('Card đồ họa', v.gpu),
    ]),
    hethong: f([
      row('Hệ điều hành', v.heDieuHanh),
      row('Pin', v.pin),
      row('Màn hình', v.kichThuocManHinh),
    ]),
  };
});

// Reviews
const reviews = ref([]);
const reviewsLoading = ref(true);

const loadReviews = async () => {
  reviewsLoading.value = true;
  try {
    reviews.value = await DanhGiaService.getBySanPham(props.product.sanPhamId);
  } catch {
    reviews.value = [];
  } finally {
    reviewsLoading.value = false;
  }
};

onMounted(loadReviews);

const myReview = computed(() =>
  props.authUser ? reviews.value.find(r => r.khachHangId === props.authUser.id) ?? null : null
);

const otherReviews = computed(() =>
  reviews.value.filter(r => r.danhGiaId !== myReview.value?.danhGiaId)
);

const { currentPage: reviewsPage, totalPages: reviewsTotalPages, pagedItems: pagedReviews } = usePagination(otherReviews, 5);

const avgRating = computed(() =>
  reviews.value.length ? reviews.value.reduce((sum, r) => sum + r.soSao, 0) / reviews.value.length : null
);

const newSoSao = ref(5);
const newNoiDung = ref('');
const submittingReview = ref(false);
const reviewError = ref('');

const submitReview = async () => {
  reviewError.value = '';
  submittingReview.value = true;
  try {
    const res = await DanhGiaService.add(props.product.sanPhamId, newSoSao.value, newNoiDung.value.trim() || null);
    if (!res.ok) { reviewError.value = await res.text().catch(() => res.statusText); return; }
    newSoSao.value = 5;
    newNoiDung.value = '';
    await loadReviews();
  } catch (e) {
    reviewError.value = e.message || 'Không thể gửi đánh giá.';
  } finally {
    submittingReview.value = false;
  }
};

const deleteMyReview = async () => {
  if (!myReview.value) return;
  try {
    const res = await DanhGiaService.remove(myReview.value.danhGiaId);
    if (!res.ok) { reviewError.value = await res.text().catch(() => res.statusText); return; }
    await loadReviews();
  } catch (e) {
    reviewError.value = e.message || 'Không thể xóa đánh giá.';
  }
};

onMounted(() => { document.body.style.overflow = 'hidden'; });
onUnmounted(() => { document.body.style.overflow = ''; });
</script>
