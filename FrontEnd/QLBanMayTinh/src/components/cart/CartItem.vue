<template>
  <!-- Một dòng sản phẩm trong giỏ hàng -->
  <div class="d-flex align-items-center gap-3 p-2 rounded-3"
       style="background:rgba(255,255,255,0.04);">

    <!-- Ảnh sản phẩm -->
    <div class="rounded-2 flex-shrink-0 overflow-hidden"
         style="width:56px; height:56px; background:#1a1a1a;">
      <img v-if="item.hinhAnhChinh"
           :src="item.hinhAnhChinh"
           :alt="item.tenSanPham"
           style="width:100%; height:100%; object-fit:cover;" />
      <div v-else class="w-100 h-100 d-flex align-items-center justify-content-center text-secondary"
           style="font-size:1.2rem;">📦</div>
    </div>

    <!-- Tên + giá -->
    <div class="flex-grow-1" style="min-width:0;">
      <div class="fw-semibold text-light text-truncate" style="font-size:0.85rem;">
        {{ item.tenSanPham }}
      </div>
      <div class="text-secondary" style="font-size:0.73rem;">{{ item.maSku }}</div>
      <div class="text-warning fw-bold" style="font-size:0.88rem;">
        {{ formatPrice(item.giaBan) }}
      </div>
    </div>

    <!-- Điều chỉnh số lượng -->
    <div class="d-flex align-items-center gap-1 flex-shrink-0">
      <button class="btn btn-sm btn-outline-secondary"
              style="width:26px; height:26px; padding:0; font-size:1rem; line-height:1;"
              @click="$emit('decrease', item)">−</button>
      <span style="min-width:24px; text-align:center; font-size:0.9rem;">{{ item.soLuong }}</span>
      <button class="btn btn-sm btn-outline-secondary"
              style="width:26px; height:26px; padding:0; font-size:1rem; line-height:1;"
              @click="$emit('increase', item)">+</button>
    </div>

    <!-- Thành tiền + nút xóa -->
    <div class="text-end flex-shrink-0">
      <div class="text-warning fw-bold" style="font-size:0.82rem; min-width:80px;">
        {{ formatPrice(item.giaBan * item.soLuong) }}
      </div>
      <button class="btn btn-sm btn-link text-danger p-0 text-decoration-none"
              style="font-size:0.75rem;"
              @click="$emit('remove', item)">Xóa</button>
    </div>
  </div>
</template>

<script setup>
// Props: item = { tenSanPham, maSku, giaBan, soLuong, hinhAnhChinh }
// Emits: increase(item), decrease(item), remove(item)
defineProps({ item: { type: Object, required: true } });
defineEmits(['increase', 'decrease', 'remove']);

const formatPrice = (v) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v ?? 0);
</script>
