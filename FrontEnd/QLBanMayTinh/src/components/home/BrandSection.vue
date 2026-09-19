<template>
  <section class="mt-4 p-3 rounded-4" style="background: #fff; border: 1px solid #f0f0f0;">
    <!-- Header với màu của brand -->
    <div class="d-flex align-items-center justify-content-between mb-3">
      <div class="d-flex align-items-center gap-2">
        <span
          class="d-flex align-items-center justify-content-center rounded-circle"
          :style="`width: 40px; height: 40px; background: ${brand.color}; color: #fff; font-weight: 900; font-size: 16px;`"
        >
          {{ brand.name.charAt(0) }}
        </span>
        <h3 class="mb-0 fw-bold" style="font-size: 1.1rem; color: #333;">
          {{ brand.name }}
        </h3>
        <span class="badge" :style="`background: ${brand.color}20; color: ${brand.color}; font-size: 11px;`">
          {{ products.length }} sản phẩm
        </span>
      </div>
      <a
        href="#"
        class="btn btn-sm text-decoration-none fw-semibold"
        :style="`color: ${brand.color};`"
      >
        Xem tất cả <ChevronRight :size="14" style="vertical-align: -2px;" />
      </a>
    </div>

    <!-- Products Grid - 2 hàng, mỗi hàng 4 sản phẩm -->
    <div class="row g-3">
      <!-- Hàng 1 -->
      <div class="row g-3 mb-3">
        <div
          v-for="product in products.slice(0, 4)"
          :key="product.bienTheId"
          class="col-6 col-lg-3"
        >
          <div
            class="card h-100 cursor-pointer"
            style="border-radius: 12px; border: 2px solid transparent; box-shadow: 0 4px 0 rgba(190, 24, 93, 0.3), 0 6px 16px rgba(0, 0, 0, 0.15);"
            @click="$emit('open-product', product)"
            @mouseenter="e => { e.currentTarget.style.transform = 'translateY(-4px)'; e.currentTarget.style.borderColor = `${brand.color}80`; e.currentTarget.style.boxShadow = '0 8px 0 rgba(190, 24, 93, 0.4), 0 12px 24px rgba(244, 63, 94, 0.35)'; }"
            @mouseleave="e => { e.currentTarget.style.transform = ''; e.currentTarget.style.borderColor = 'transparent'; e.currentTarget.style.boxShadow = '0 4px 0 rgba(190, 24, 93, 0.3), 0 6px 16px rgba(0, 0, 0, 0.15)'; }"
          >
            <div class="position-relative" style="height: 100px; background: #f9fafb;">
              <img
                v-if="product.hinhAnhChinh"
                :src="product.hinhAnhChinh"
                :alt="product.tenSanPham"
                style="width: 100%; height: 100%; object-fit: contain;"
              />
              <span
                v-if="product.giaGoc && product.giaGoc > product.giaBan"
                class="badge position-absolute top-0 start-0 m-1"
                style="background: #ef4444; font-size: 9px;"
              >
                -{{ Math.round(((product.giaGoc - product.giaBan) / product.giaGoc) * 100) }}%
              </span>
            </div>
            <div class="card-body p-2">
              <p class="mb-1 text-truncate fw-bold" style="font-size: 11px; color: #333; line-height: 1.3;">
                {{ product.tenSanPham }}
              </p>
              <p class="mb-0 fw-black" style="font-size: 13px; color: #d40f28;">
                {{ formatPrice(product.giaBan) }}
              </p>
              <p v-if="product.giaGoc && product.giaGoc > product.giaBan" class="mb-0 text-decoration-line-through" style="font-size: 10px; color: #999;">
                {{ formatPrice(product.giaGoc) }}
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- Hàng 2 -->
      <div v-if="products.length > 4" class="row g-3">
        <div
          v-for="product in products.slice(4, 8)"
          :key="product.bienTheId"
          class="col-6 col-lg-3"
        >
          <div
            class="card h-100 cursor-pointer"
            style="border-radius: 12px; border: 2px solid transparent; box-shadow: 0 4px 0 rgba(190, 24, 93, 0.3), 0 6px 16px rgba(0, 0, 0, 0.15);"
            @click="$emit('open-product', product)"
            @mouseenter="e => { e.currentTarget.style.transform = 'translateY(-4px)'; e.currentTarget.style.borderColor = `${brand.color}80`; e.currentTarget.style.boxShadow = '0 8px 0 rgba(190, 24, 93, 0.4), 0 12px 24px rgba(244, 63, 94, 0.35)'; }"
            @mouseleave="e => { e.currentTarget.style.transform = ''; e.currentTarget.style.borderColor = 'transparent'; e.currentTarget.style.boxShadow = '0 4px 0 rgba(190, 24, 93, 0.3), 0 6px 16px rgba(0, 0, 0, 0.15)'; }"
          >
            <div class="position-relative" style="height: 100px; background: #f9fafb;">
              <img
                v-if="product.hinhAnhChinh"
                :src="product.hinhAnhChinh"
                :alt="product.tenSanPham"
                style="width: 100%; height: 100%; object-fit: contain;"
              />
              <span
                v-if="product.giaGoc && product.giaGoc > product.giaBan"
                class="badge position-absolute top-0 start-0 m-1"
                style="background: #ef4444; font-size: 9px;"
              >
                -{{ Math.round(((product.giaGoc - product.giaBan) / product.giaGoc) * 100) }}%
              </span>
            </div>
            <div class="card-body p-2">
              <p class="mb-1 text-truncate fw-bold" style="font-size: 11px; color: #333; line-height: 1.3;">
                {{ product.tenSanPham }}
              </p>
              <p class="mb-0 fw-black" style="font-size: 13px; color: #d40f28;">
                {{ formatPrice(product.giaBan) }}
              </p>
              <p v-if="product.giaGoc && product.giaGoc > product.giaBan" class="mb-0 text-decoration-line-through" style="font-size: 10px; color: #999;">
                {{ formatPrice(product.giaGoc) }}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ChevronRight } from '@lucide/vue';
import { formatPrice as formatPriceRaw } from '../../utils/formatPrice.js';

const props = defineProps({
  brand: { type: Object, required: true },
  products: { type: Array, default: () => [] },
});

defineEmits(['open-product', 'add-to-cart']);

const formatPrice = (v) => (v == null ? 'Liên hệ' : formatPriceRaw(v));
</script>
