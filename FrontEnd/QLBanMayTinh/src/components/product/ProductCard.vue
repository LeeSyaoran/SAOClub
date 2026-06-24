<template>
  <!-- Thẻ sản phẩm — hiển thị trên trang chủ và trang danh mục -->
  <div class="card h-100 border-secondary"
       style="background:#141414; cursor:pointer; transition:border-color 0.15s, transform 0.15s;"
       @mouseenter="e => { e.currentTarget.style.borderColor='rgba(244,194,0,0.5)'; e.currentTarget.style.transform='translateY(-2px)'; }"
       @mouseleave="e => { e.currentTarget.style.borderColor=''; e.currentTarget.style.transform=''; }"
       @click="$emit('click', product)">

    <!-- Ảnh sản phẩm -->
    <div class="position-relative overflow-hidden"
         style="height:150px; background:#1a1a1a; border-radius:0.375rem 0.375rem 0 0;">
      <img v-if="product.hinhAnhChinh"
           :src="product.hinhAnhChinh"
           :alt="product.tenSanPham"
           style="width:100%; height:100%; object-fit:contain; padding:8px;" />
      <div v-else class="w-100 h-100 d-flex align-items-center justify-content-center text-secondary"
           style="font-size:2rem;">💻</div>

      <!-- Badge trạng thái hàng -->
      <div class="position-absolute top-0 start-0 m-2">
        <Badge :text="product.trangThai === 'active' ? 'Còn hàng' : 'Hết hàng'"
               :variant="product.trangThai === 'active' ? 'success' : 'danger'" />
      </div>
    </div>

    <!-- Thông tin sản phẩm -->
    <div class="card-body p-2 d-flex flex-column gap-1">
      <!-- Tên sản phẩm (tối đa 2 dòng) -->
      <div class="fw-semibold text-light lh-sm"
           style="font-size:0.82rem; display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; overflow:hidden;">
        {{ product.tenSanPham }}
      </div>

      <!-- Thương hiệu + danh mục -->
      <div class="text-secondary" style="font-size:0.72rem;">
        {{ product.tenThuongHieu }} · {{ product.tenDanhMuc }}
      </div>

      <!-- Giá -->
      <div class="fw-black text-warning mt-auto" style="font-size:0.95rem;">
        {{ formatPrice(product.giaBan) }}
      </div>

      <!-- Nút thêm vào giỏ -->
      <button class="btn btn-sm btn-warning text-dark fw-bold w-100 mt-1"
              style="font-size:0.78rem;"
              :disabled="product.trangThai !== 'active'"
              @click.stop="$emit('add-to-cart', product)">
        🛒 Thêm vào giỏ
      </button>
    </div>
  </div>
</template>

<script setup>
import Badge from '../common/Badge.vue';

// Props: object sản phẩm từ API /api/san-pham/hien-thi
defineProps({ product: { type: Object, required: true } });

// Emits: click (xem chi tiết), add-to-cart (thêm vào giỏ)
defineEmits(['click', 'add-to-cart']);

const formatPrice = (v) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v ?? 0);
</script>
