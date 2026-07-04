<template>
  <!-- Một dòng sản phẩm trong giỏ hàng -->
  <div class="d-flex gap-3 p-3 rounded-3"
       style="background:var(--bg-page); border:1px solid var(--border-color-soft);">

    <!-- Ảnh sản phẩm -->
    <div class="flex-shrink-0" style="width:64px;height:64px;">
      <img v-if="item.hinhAnhChinh" :src="item.hinhAnhChinh" :alt="item.tenSanPham"
           style="width:64px;height:64px;object-fit:contain;border-radius:10px;background:var(--bg-card-inset);" />
      <div v-else class="d-flex align-items-center justify-content-center rounded-3"
           style="width:64px;height:64px;background:var(--bg-card-alt);font-size:1.6rem;">💻</div>
    </div>

    <!-- Thông tin -->
    <div class="flex-grow-1 min-width-0">
      <div class="fw-semibold" style="font-size:12px; line-height:1.4; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; line-clamp:2; -webkit-box-orient:vertical; color:var(--text-primary);">{{ item.tenSanPham }}</div>
      <div class="mt-1" style="font-size:10px; color:var(--text-secondary);">
        <span v-if="item.mauSac">{{ item.mauSac }}</span>
        <span v-if="item.mauSac && item.cpu"> · </span>
        <span v-if="item.cpu">{{ item.cpu }}</span>
      </div>
      <!-- Số lượng + giá -->
      <div class="d-flex align-items-center justify-content-between mt-2">
        <div class="d-flex align-items-center gap-1">
          <button class="d-flex align-items-center justify-content-center"
                  style="width:26px;height:26px;padding:0;background:var(--bg-card-alt);color:var(--text-primary);border:none;border-radius:7px;font-size:15px;cursor:pointer;line-height:1;"
                  @click="$emit('decrease', item)">−</button>
          <span class="fw-bold" style="font-size:13px;min-width:22px;text-align:center; color:var(--text-heading);">{{ item.quantity }}</span>
          <button class="d-flex align-items-center justify-content-center"
                  style="width:26px;height:26px;padding:0;background:var(--bg-card-alt);color:var(--text-primary);border:none;border-radius:7px;font-size:15px;cursor:pointer;line-height:1;"
                  @click="$emit('increase', item)">+</button>
        </div>
        <span class="text-warning fw-bold" style="font-size:13px;">{{ formatPrice(item.giaBan * item.quantity) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
// Props: item = { tenSanPham, hinhAnhChinh, giaBan, quantity, mauSac?, cpu? }
// Emits: increase(item), decrease(item) — cha (App.vue) đã có sẵn updateQty(bienTheId, delta)
defineProps({ item: { type: Object, required: true } });
defineEmits(['increase', 'decrease']);

const formatPrice = (v) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v ?? 0);
</script>
