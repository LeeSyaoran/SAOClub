<template>
  <Modal :model-value="modelValue" width="min(96vw, 1000px)" @update:model-value="$emit('update:modelValue', $event)">
    <div class="fw-bold mb-3" style="font-size:1.05rem; color:var(--text-heading);">
      {{ t('productCompare.title') }}
    </div>

    <div style="overflow-x:auto;">
      <table class="table table-borderless align-middle mb-0" style="min-width:560px;">
        <thead>
          <tr>
            <th style="width:120px;"></th>
            <th v-for="item in items" :key="item.bienTheId" class="text-center" style="min-width:150px;">
              <div class="d-flex flex-column align-items-center gap-1">
                <div style="width:80px; height:80px; background:var(--bg-card-inset); border-radius:8px;" class="d-flex align-items-center justify-content-center overflow-hidden">
                  <img v-if="item.hinhAnhChinh" :src="item.hinhAnhChinh" :alt="item.tenSanPham" style="width:100%; height:100%; object-fit:contain; padding:6px;" />
                  <span v-else style="font-size:1.5rem;">💻</span>
                </div>
                <span class="fw-bold small" style="color:var(--text-primary); font-size:0.78rem;">{{ item.tenSanPham }}</span>
                <button class="btn-close" style="font-size:9px;" :aria-label="t('common.remove')" @click="$emit('remove', item)"></button>
              </div>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.label" style="border-top:1px solid var(--border-color-soft);">
            <td class="small fw-semibold" style="color:var(--text-secondary); font-size:0.78rem;">{{ row.label }}</td>
            <td v-for="item in items" :key="item.bienTheId" class="text-center small" style="color:var(--text-primary); font-size:0.78rem;">
              {{ row.value(item) }}
            </td>
          </tr>
          <tr style="border-top:1px solid var(--border-color-soft);">
            <td></td>
            <td v-for="item in items" :key="item.bienTheId" class="text-center py-2">
              <button class="btn btn-sm btn-warning text-dark fw-bold" style="font-size:0.75rem;"
                      @click="$emit('add-to-cart', item)">
                {{ t('productCompare.addToCart') }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </Modal>
</template>

<script setup>
import { computed } from 'vue';
import Modal from '../common/Modal.vue';
import { t } from '../../i18n/index.js';
import { formatPrice } from '../../utils/formatPrice.js';

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  items:      { type: Array,   default: () => [] },
});
defineEmits(['update:modelValue', 'remove', 'add-to-cart']);

// Danh sách hàng thông số — value(item) đọc trực tiếp field phẳng từ SanPhamResponse
// (giống ProductDetail.vue), tái dùng nhãn productDetail.specs.* có sẵn thay vì tạo nhãn mới.
const rows = computed(() => [
  { label: t('productCompare.rowPrice'),      value: (i) => formatPrice(i.giaBan) },
  { label: t('productDetail.specs.brand'),    value: (i) => i.tenThuongHieu || '—' },
  { label: t('productDetail.specs.cpu'),      value: (i) => i.cpu || '—' },
  { label: t('productDetail.specs.ram'),      value: (i) => i.ram || '—' },
  { label: t('productDetail.specs.storage'),  value: (i) => i.oCung || '—' },
  { label: t('productDetail.specs.gpu'),      value: (i) => i.gpu || '—' },
  { label: t('productDetail.specs.screenSize'), value: (i) => i.kichThuocManHinh || '—' },
  { label: t('productDetail.specs.os'),       value: (i) => i.heDieuHanh || '—' },
  { label: t('productDetail.specs.battery'),  value: (i) => i.pin || '—' },
  { label: t('productDetail.specs.weight'),   value: (i) => (i.trongLuongKg != null ? `${i.trongLuongKg} kg` : '—') },
  { label: t('productDetail.specs.warranty'), value: (i) => (i.baoHanhThang != null ? `${i.baoHanhThang} ${t('productDetail.months')}` : '—') },
]);
</script>
