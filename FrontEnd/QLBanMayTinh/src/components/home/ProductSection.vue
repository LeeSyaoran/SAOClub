<template>
  <section class="mt-4 p-3 rounded-4" style="background: #fff; border: 1px solid #f0f0f0;">
    <!-- Header -->
    <div class="d-flex align-items-center justify-content-between mb-3">
      <div class="d-flex align-items-center gap-2">
        <span style="font-size: 20px;">{{ sectionIcon }}</span>
        <h3 class="mb-0 fw-bold" style="font-size: 1.1rem; color: #333;">
          {{ title }}
        </h3>
      </div>
      <a href="#" class="btn btn-sm text-decoration-none" style="color: #ec4899; font-weight: 600;">
        Xem tất cả <ChevronRight :size="14" style="vertical-align: -2px;" />
      </a>
    </div>

    <div class="row g-3">
      <!-- 2 Banner bên trái -->
      <div class="col-12 col-md-3">
        <div class="d-flex flex-column gap-3 h-100">
          <div
            v-for="(banner, idx) in banners"
            :key="idx"
            class="rounded-3 p-3 flex-grow-1 d-flex flex-column justify-content-between cursor-pointer"
            :style="`background: ${banner.bg}; min-height: 140px; transition: all 0.2s; cursor: pointer;`"
            style="border: 1px solid transparent;"
            @mouseenter="e => { e.currentTarget.style.transform = 'translateY(-3px)'; e.currentTarget.style.boxShadow = '0 8px 20px rgba(0,0,0,0.12)'; }"
            @mouseleave="e => { e.currentTarget.style.transform = ''; e.currentTarget.style.boxShadow = ''; }"
          >
            <div>
              <h4 class="mb-1 fw-bold" :style="`color: ${banner.color}; font-size: 1rem;`">
                {{ banner.title }}
              </h4>
              <p class="mb-0" style="font-size: 12px; color: #666;">
                {{ banner.desc }}
              </p>
            </div>
            <div class="d-flex align-items-center gap-2 mt-2">
              <span class="badge" :style="`background: ${banner.color}; color: #fff; font-size: 10px;`">
                Xem ngay
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- Hàng sản phẩm - 2 hàng, mỗi hàng 4 sản phẩm -->
      <div class="col-12 col-md-9">
        <div class="d-flex flex-column gap-3">
          <!-- Hàng 1 -->
          <div class="row g-3">
            <div
              v-for="product in products.slice(0, 4)"
              :key="product.bienTheId"
              class="col-6 col-lg-3"
            >
              <div
                class="card h-100 cursor-pointer"
                style="border-radius: 12px; border: 2px solid transparent; box-shadow: 0 4px 0 rgba(190, 24, 93, 0.3), 0 6px 16px rgba(0, 0, 0, 0.15);"
                @click="$emit('open-product', product)"
                @mouseenter="e => { e.currentTarget.style.transform = 'translateY(-4px)'; e.currentTarget.style.borderColor = 'rgba(190, 24, 93, 0.5)'; e.currentTarget.style.boxShadow = '0 8px 0 rgba(190, 24, 93, 0.4), 0 12px 24px rgba(244, 63, 94, 0.35)'; }"
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
                @mouseenter="e => { e.currentTarget.style.transform = 'translateY(-4px)'; e.currentTarget.style.borderColor = 'rgba(190, 24, 93, 0.5)'; e.currentTarget.style.boxShadow = '0 8px 0 rgba(190, 24, 93, 0.4), 0 12px 24px rgba(244, 63, 94, 0.35)'; }"
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
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue';
import { ChevronRight } from '@lucide/vue';
import { formatPrice as formatPriceRaw } from '../../utils/formatPrice.js';

const props = defineProps({
  title: { type: String, required: true },
  icon: { type: String, default: 'laptop' },
  products: { type: Array, default: () => [] },
  banners: { type: Array, default: () => [] },
});

defineEmits(['open-product', 'add-to-cart']);

const formatPrice = (v) => (v == null ? 'Liên hệ' : formatPriceRaw(v));

const iconMap = {
  graduation: '🎓',
  crown: '👑',
  feather: '🪶',
  palette: '🎨',
  gamepad: '🎮',
  laptop: '💻',
};

const sectionIcon = computed(() => iconMap[props.icon] || '💻');
</script>
