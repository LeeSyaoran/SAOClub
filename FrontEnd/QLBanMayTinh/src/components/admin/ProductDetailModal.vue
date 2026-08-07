<script setup>
import { ref, watch } from "vue";
import { t } from "../../i18n/index.js";
import { ProductsStore } from "../../stores/products.js";
import * as ChiTietSanPhamService from "../../services/ChiTietSanPhamService.js";
import { formatPrice, statusLabel } from "../../utils/adminFormat.js";
import { variantsForDetail } from "../../utils/productGrouping.js";
import { Laptop } from '@lucide/vue';

// ── Modal "Chi tiết sản phẩm" — dùng chung bởi ProductsTable.vue (xem/so sánh toàn bộ
// biến thể của 1 sản phẩm) và OrdersTable.vue (chỉ xem (các) biến thể khách đã mua trong
// 1 đơn cụ thể, qua prop onlyBienTheIds — nhận 1 id hoặc mảng nhiều id). Thuần XEM —
// sửa/thêm/xóa biến thể giờ ở tab "Biến thể" riêng (BienTheTable.vue).
const props = defineProps({
  modelValue: { type: Boolean, default: false },
  sanPhamId: { type: [Number, String], default: null },
  sanPhamName: { type: String, default: "" },
  onlyBienTheIds: { type: [Number, String, Array], default: null },
});
const emit = defineEmits(["update:modelValue"]);

const detailModalList = ref([]);
const detailSerialMap = ref({}); // bienTheId → serial[] (không hiển thị trong template, giữ nguyên fetch như bản gốc)

// Helper: fetch serial của nhiều bienTheId song song → { bienTheId: serial[] }
const fetchSerialMap = async (bienTheIds) => {
  const results = await Promise.all(
    bienTheIds.map((id) => ChiTietSanPhamService.getByBienThe(id).catch(() => []))
  );
  const map = {};
  bienTheIds.forEach((id, i) => { map[id] = results[i]; });
  return map;
};

watch(
  () => [props.modelValue, props.sanPhamId, props.onlyBienTheIds],
  async ([open, sanPhamId, onlyBienTheIds]) => {
    if (!open || sanPhamId == null) return;
    const list = variantsForDetail(ProductsStore.items ?? [], sanPhamId, onlyBienTheIds);
    detailModalList.value = list;
    detailSerialMap.value = {};
    detailSerialMap.value = await fetchSerialMap(list.map((v) => v.bienTheId));
  },
);

const close = () => emit("update:modelValue", false);
</script>

<template>
  <div v-if="modelValue" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1060;" @click.self="close">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:1100px;max-width:97vw;max-height:92vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>{{ t('admin.detailModal.titlePrefix') }} {{ sanPhamName }}</span>
        <div class="d-flex align-items-center gap-2">
          <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="close"></button>
        </div>
      </div>
      <div class="overflow-y-auto p-3">
        <div v-for="v in detailModalList" :key="v.bienTheId" class="mb-4 rounded-3 overflow-hidden" style="border:1px solid var(--border-color);">
          <!-- Header bien the -->
          <div class="d-flex align-items-center justify-content-between gap-3 p-3" style="background:var(--bg-input);">
            <div class="d-flex align-items-center gap-3">
              <img v-if="v.hinhAnhChinh" :src="v.hinhAnhChinh" style="width:72px;height:54px;object-fit:contain;background:var(--bg-card-inset);border-radius:6px;padding:4px;" />
              <span v-else style="width:72px;text-align:center;"><Laptop :size="32" color="var(--text-muted)" /></span>
              <div>
                <div class="fw-bold text-light" style="font-size:0.95rem;">{{ v.tenSanPham }}</div>
                <div class="text-secondary" style="font-size:0.75rem;font-family:monospace;">{{ v.maSku }}</div>
              </div>
            </div>
          </div>
          <!-- Bang thong tin 4 cot (label | value | label | value) -->
          <table class="w-100 mb-0" style="border-collapse:collapse;font-size:0.8rem;">
            <tbody>
              <tr style="border-top:1px solid var(--border-color-soft);">
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.brand') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ v.tenThuongHieu }}</td>
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.category') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ v.tenDanhMuc }}</td>
              </tr>
              <tr style="border-top:1px solid var(--border-color-soft);">
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.supplier') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ v.tenNhaCungCap }}</td>
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.productType') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ v.loaiSanPham }}</td>
              </tr>
              <tr style="border-top:1px solid var(--border-color-soft);">
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.priceSell') }}</td>
                <td class="px-3 py-1 fw-bold" style="background:var(--bg-card);color:var(--accent-fg);">{{ formatPrice(v.giaBan) }}</td>
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.priceBuy') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ formatPrice(v.giaNhap) }}</td>
              </tr>
              <tr style="border-top:1px solid var(--border-color-soft);">
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.warranty') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ v.baoHanhThang ? v.baoHanhThang + ' ' + t('admin.detailModal.months') : '—' }}</td>
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.color') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ v.mauSac }}</td>
              </tr>
              <tr style="border-top:1px solid var(--border-color-soft);">
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.status') }}</td>
                <td class="px-3 py-1" style="background:var(--bg-card);">
                  <span class="badge" :class="v.trangThai==='active'?'bg-success':'bg-secondary'" style="font-size:0.7rem;">{{ statusLabel(v.trangThai) }}</span>
                </td>
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.cpu') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ v.cpu || '—' }}</td>
              </tr>
              <tr style="border-top:1px solid var(--border-color-soft);">
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.ram') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ v.ram || '—' }}</td>
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.storage') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ v.oCung || '—' }}</td>
              </tr>
              <tr style="border-top:1px solid var(--border-color-soft);">
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.gpu') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ v.gpu || '—' }}</td>
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.screen') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ v.kichThuocManHinh || '—' }}</td>
              </tr>
              <tr style="border-top:1px solid var(--border-color-soft);">
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.os') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ v.heDieuHanh || '—' }}</td>
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.battery') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ v.pin || '—' }}</td>
              </tr>
              <tr style="border-top:1px solid var(--border-color-soft);">
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.weight') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ v.trongLuongKg ? v.trongLuongKg + ' ' + t('admin.detailModal.kg') : '—' }}</td>
                <td class="px-3 py-1 text-secondary" style="background:var(--bg-card-alt);font-weight:600;">{{ t('admin.detailModal.classification') }}</td>
                <td class="px-3 py-1 text-light" style="background:var(--bg-card);">{{ v.phanLoaiTen || '—' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Bootstrap .text-light hardcode mau trang co dinh — ghi de theo theme hien tai (dung
   trong bang thong so variant, tren nen the/card, khong phai nen mau thuong hieu co
   dinh, nen an toan khi ghi de theo bien theme). Scoped rieng cho component nay vi CSS
   scoped khong ke thua qua bien gioi component. */
.text-light {
  color: var(--text-primary) !important;
}
</style>
