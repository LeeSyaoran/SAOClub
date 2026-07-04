<template>
  <!-- Thẻ sản phẩm — hiển thị trên lưới trang chủ -->
  <article class="card h-100 border-secondary"
       style="background:var(--bg-card); border-radius:14px; overflow:hidden; transition:transform 0.15s, box-shadow 0.15s; cursor:pointer;"
       @mouseenter="e => { e.currentTarget.style.transform='translateY(-3px)'; e.currentTarget.style.boxShadow='0 8px 24px rgba(0,0,0,0.4)'; }"
       @mouseleave="e => { e.currentTarget.style.transform=''; e.currentTarget.style.boxShadow=''; }"
       @click="$emit('click', product)">

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
        {{ product.trangThai === 'active' ? t('home.inStock') : t('home.outOfStock') }}
      </span>
    </div>

    <!-- Thông tin sản phẩm -->
    <div class="card-body p-2 d-flex flex-column gap-1">
      <h3 class="fw-bold mb-0"
          style="font-size:11px; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; line-clamp:2; -webkit-box-orient:vertical; color:var(--text-primary);">
        {{ product.tenSanPham }}
      </h3>
      <p class="mb-0" style="font-size:10px; color:var(--text-secondary);">
        {{ product.tenThuongHieu || product.tenDanhMuc }}
      </p>
      <p class="mb-0 text-warning fw-black" style="font-size:13px;">
        <span v-if="variantCount > 1" class="fw-normal" style="font-size:9px; color:var(--text-secondary);">{{ t('home.fromPrice') }} </span>{{ formatPrice(product.giaBan) }}
      </p>
      <p class="mb-0" style="font-size:10px; color:var(--text-secondary);">{{ t('home.fastDelivery') }}</p>
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
      <!-- Nút thêm vào giỏ — disabled nếu hết hàng. Cha quyết định mở trang chi tiết
           thay vì thêm thẳng nếu sản phẩm có nhiều biến thể (tránh thêm nhầm biến
           thể giá thấp nhất đang hiển thị đại diện). -->
      <button
        class="btn btn-sm w-100 fw-bold mt-1"
        style="font-size:11px; border-radius:8px;"
        :class="product.trangThai === 'active' ? 'btn-warning text-dark' : 'btn-secondary'"
        :disabled="product.trangThai !== 'active'"
        @click.stop="$emit('add-to-cart', product)">
        {{ t('home.addToCart') }}
      </button>
    </div>
  </article>
</template>

<script setup>
import { t } from '../../i18n/index.js';

defineProps({
  // Sản phẩm từ API /api/san-pham/hien-thi
  product:      { type: Object,  required: true },
  // Số biến thể của sản phẩm này — > 1 thì hiện nhãn "từ giá"
  variantCount: { type: Number,  default: 0 },
});

// Emits: click (xem chi tiết), add-to-cart (thêm nhanh — cha tự quyết định có mở trang chi tiết trước hay không)
defineEmits(['click', 'add-to-cart']);

const formatPrice = (v) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v ?? 0);
</script>
