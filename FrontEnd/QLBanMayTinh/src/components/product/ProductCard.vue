<template>
  <!-- Thẻ sản phẩm — hiển thị trên lưới trang chủ -->
  <article
    class="card h-100 border-secondary"
    style="background:var(--bg-card); border-radius:14px; overflow:hidden; transition:transform 0.15s, box-shadow 0.15s; cursor:pointer;"
    @mouseenter="e => { e.currentTarget.style.transform='translateY(-3px)'; e.currentTarget.style.boxShadow='0 8px 24px rgba(0,0,0,0.4)'; }"
    @mouseleave="e => { e.currentTarget.style.transform=''; e.currentTarget.style.boxShadow=''; }"
    @click="$emit('click', product)"
  >
    <!-- Ảnh sản phẩm -->
    <div class="position-relative" style="background:var(--bg-card-inset); height:160px;">
      <img
        v-if="product.hinhAnhChinh"
        :src="product.hinhAnhChinh"
        :alt="product.tenSanPham"
        class="w-100 h-100"
        style="object-fit:contain; padding:8px;"
      />
      <!-- Placeholder nếu không có ảnh -->
      <div
        v-else
        class="w-100 h-100 d-flex align-items-center justify-content-center"
      >
        <Laptop :size="40" color="var(--text-muted)" />
      </div>
      <span
        class="badge position-absolute top-0 start-0 m-2"
        style="font-size:10px;"
        :class="stockBadgeClass"
      >
        {{ stockBadgeText }}
      </span>

      <!-- Nút yêu thích — nổi góc trên phải, đối xứng với badge tồn kho góc trái -->
      <button
        type="button"
        class="btn position-absolute top-0 end-0 m-2 d-flex align-items-center justify-content-center p-0"
        style="width:26px; height:26px; border-radius:50%; background:rgba(0,0,0,0.5); border:none; font-size:13px;"
        :aria-label="isWishlisted ? t('wishlist.remove') : t('wishlist.add')"
        :title="isWishlisted ? t('wishlist.remove') : t('wishlist.add')"
        @click.stop="$emit('toggle-wishlist', product)"
      >
        <Heart :size="16" :fill="isWishlisted ? 'currentColor' : 'none'" />
      </button>
    </div>

    <!-- Thông tin sản phẩm -->
    <div class="card-body p-2 d-flex flex-column gap-1">
      <h3
        class="fw-bold mb-0"
        style="font-size:11px; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; line-clamp:2; -webkit-box-orient:vertical; color:var(--text-primary);"
      >
        {{ product.tenSanPham }}
      </h3>
      <p class="mb-0" style="font-size:10px; color:var(--text-secondary);">
        {{ product.tenThuongHieu || product.tenDanhMuc }}
      </p>
      <p class="mb-0 text-warning fw-black" style="font-size:13px;">
        <span v-if="variantCount > 1" class="fw-normal" style="font-size:9px; color:var(--text-secondary);">{{ t('home.fromPrice') }} </span>{{ formatPrice(product.giaBan) }}
      </p>
      <p v-if="rating" class="mb-0" style="font-size:10px; color:var(--text-secondary);">
        <Star :size="12" fill="currentColor" style="vertical-align:-2px;" /> {{ rating.diemTrungBinh.toFixed(1) }} ({{ rating.tongSoDanhGia }})
      </p>
      <p class="mb-0" style="font-size:10px; color:var(--text-secondary);"><Truck :size="11" style="vertical-align:-2px;" /> {{ t('home.fastDelivery') }}</p>
      <!-- Tags phân loại — hiển thị tên tiếng Việt từ phanLoaiTen -->
      <div v-if="product.phanLoaiTen" class="d-flex flex-wrap gap-1 mt-1">
        <span
          v-for="tag in product.phanLoaiTen.split(',')"
          :key="tag"
          class="badge"
          style="font-size:9px; background:rgba(244,63,94,0.12); color:var(--accent-fg); border:1px solid rgba(244,63,94,0.35);"
        >
          {{ tag.trim() }}
        </span>
      </div>
      <!-- Nút thêm vào giỏ — disabled nếu hết hàng. Cha quyết định mở trang chi tiết
           thay vì thêm thẳng nếu sản phẩm có nhiều biến thể (tránh thêm nhầm biến
           thể giá thấp nhất đang hiển thị đại diện). -->
      <button
        class="btn btn-sm w-100 fw-bold mt-1"
        style="font-size:11px; border-radius:8px;"
        :class="stockBadgeClass === 'bg-secondary' ? 'btn-secondary' : 'btn-warning text-dark'"
        :disabled="stockBadgeClass === 'bg-secondary'"
        @click.stop="$emit('add-to-cart', product)"
      >
        <ShoppingCart :size="12" style="vertical-align:-2px;" /> {{ t('home.addToCart') }}
      </button>

      <!-- Checkbox "So sánh" — disabled khi đã chọn đủ số lượng tối đa (trừ chính nó, vẫn
           bấm được để bỏ chọn). Cha (CustomerPage.vue) giữ danh sách so sánh dùng chung. -->
      <label
        class="d-flex align-items-center gap-1 mt-1"
        style="font-size:10px; color:var(--text-secondary); cursor:pointer; user-select:none;"
        @click.stop
      >
        <input
          type="checkbox"
          class="form-check-input m-0"
          style="width:12px; height:12px;"
          :checked="isComparing"
          :disabled="compareDisabled"
          @change="$emit('toggle-compare', product)"
        />
        <CheckCircle2 v-if="isComparing" :size="11" style="vertical-align:-2px;" /> {{ isComparing ? t('productCompare.added') : t('productCompare.add') }}
      </label>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue';
import { t } from '../../i18n/index.js';
import { formatPrice } from '../../utils/formatPrice.js';
import { Laptop, Heart, Star, Truck, ShoppingCart, CheckCircle2 } from '@lucide/vue';

const props = defineProps({
  // Sản phẩm từ API /api/san-pham/hien-thi
  product:      { type: Object,  required: true },
  // Số biến thể của sản phẩm này — > 1 thì hiện nhãn "từ giá"
  variantCount: { type: Number,  default: 0 },
  // Sản phẩm này có đang nằm trong danh sách so sánh không (cha giữ state dùng chung)
  isComparing:     { type: Boolean, default: false },
  // Đã chọn đủ số lượng so sánh tối đa (và sản phẩm này chưa được chọn) — khoá checkbox lại
  compareDisabled: { type: Boolean, default: false },
  // Sản phẩm này có đang trong danh sách yêu thích không (App.vue giữ state dùng chung)
  isWishlisted: { type: Boolean, default: false },
  // Điểm đánh giá trung bình { diemTrungBinh, tongSoDanhGia } — null nếu chưa có đánh giá nào
  rating: { type: Object, default: null },
});

// Emits: click (xem chi tiết), add-to-cart (thêm nhanh — cha tự quyết định có mở trang chi tiết
// trước hay không), toggle-compare (bật/tắt trong danh sách so sánh), toggle-wishlist (bật/tắt yêu thích)
defineEmits(['click', 'add-to-cart', 'toggle-compare', 'toggle-wishlist']);

// Ngưỡng "sắp hết hàng" — dưới mức này tạo cảm giác khan hiếm (giống nhiều sàn TMĐT khác).
const LOW_STOCK_THRESHOLD = 5;

const stockBadgeClass = computed(() => {
  if (props.product.trangThai !== 'active' || (props.product.soLuongTon ?? 0) <= 0) return 'bg-secondary';
  if (props.product.soLuongTon <= LOW_STOCK_THRESHOLD) return 'bg-warning text-dark';
  return 'bg-success';
});

const stockBadgeText = computed(() => {
  const soLuong = props.product.soLuongTon ?? 0;
  if (props.product.trangThai !== 'active' || soLuong <= 0) return t('home.outOfStock');
  if (soLuong <= LOW_STOCK_THRESHOLD) return t('home.lowStockCount', { count: soLuong });
  return t('home.inStockCount', { count: soLuong });
});
</script>
