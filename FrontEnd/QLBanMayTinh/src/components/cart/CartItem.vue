<template>
  <div
    class="d-flex gap-2 p-3 rounded-3 position-relative"
    style="background:var(--bg-page); border:1px solid var(--border-color-soft);"
  >
    <!-- Nút xóa -->
    <button
      class="position-absolute d-flex align-items-center justify-content-center"
      style="top:6px;right:6px;width:22px;height:22px;padding:0;background:transparent;color:var(--text-muted);border:none;border-radius:50%;cursor:pointer;font-size:14px;line-height:1;"
      :aria-label="t('cart.remove')"
      @click="$emit('remove', item)"
    >
      ×
    </button>

    <!-- Checkbox -->
    <div class="d-flex align-items-center" style="flex-shrink:0;">
      <input
        type="checkbox"
        :checked="selected"
        @change="$emit('toggle', item)"
        style="width:16px;height:16px;cursor:pointer;"
      />
    </div>

    <div class="flex-shrink-0" style="width:64px;height:64px;">
      <img
        v-if="item.hinhAnhChinh" :src="item.hinhAnhChinh" :alt="item.tenSanPham"
        style="width:64px;height:64px;object-fit:contain;border-radius:10px;background:var(--bg-card-inset);"
      />
      <div
        v-else class="d-flex align-items-center justify-content-center rounded-3"
        style="width:64px;height:64px;background:var(--bg-card-alt);"
      >
        <Laptop :size="26" color="var(--text-muted)" />
      </div>
    </div>

    <div class="flex-grow-1 min-width-0">
      <div class="fw-semibold pe-4" style="font-size:12px; line-height:1.4; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; line-clamp:2; -webkit-box-orient:vertical; color:var(--text-primary);">{{ item.tenSanPham }}</div>
      <div class="mt-1" style="font-size:10px; color:var(--text-secondary);">
        <span v-if="item.mauSac">{{ item.mauSac }}</span>
        <span v-if="item.mauSac && item.cpu"> · </span>
        <span v-if="item.cpu">{{ item.cpu }}</span>
      </div>
      <div class="d-flex align-items-center justify-content-between mt-2">
        <div class="d-flex align-items-center gap-1">
          <button
            class="d-flex align-items-center justify-content-center"
            style="width:26px;height:26px;padding:0;background:var(--bg-card-alt);color:var(--text-primary);border:none;border-radius:7px;font-size:15px;cursor:pointer;line-height:1;"
            :aria-label="t('cart.decrease')"
            @click="$emit('decrease', item)"
          >
            −
          </button>
          <span class="fw-bold" style="font-size:13px;min-width:22px;text-align:center; color:var(--text-heading);">{{ item.quantity }}</span>
          <button
            class="d-flex align-items-center justify-content-center"
            style="width:26px;height:26px;padding:0;background:var(--bg-card-alt);color:var(--text-primary);border:none;border-radius:7px;font-size:15px;cursor:pointer;line-height:1;"
            :aria-label="t('cart.increase')"
            @click="$emit('increase', item)"
          >
            +
          </button>
        </div>
        <span class="text-warning fw-bold" style="font-size:13px;">{{ formatPrice(item.giaBan * item.quantity) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
// Props: item, selected, emits: increase, decrease, remove, toggle
import { Laptop } from '@lucide/vue';
import { t } from '../../i18n/index.js';
import { formatPrice } from '../../utils/formatPrice.js';

const props = defineProps({
  item: { type: Object, required: true },
  selected: { type: Boolean, default: false },
});
defineEmits(['increase', 'decrease', 'remove', 'toggle']);
</script>
