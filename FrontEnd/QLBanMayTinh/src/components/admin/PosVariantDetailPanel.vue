<script setup>
import { computed, onMounted, onBeforeUnmount } from "vue";
import { t } from "../../i18n/index.js";
import { ProductsStore } from "../../stores/products.js";
import { variantsForDetail } from "../../utils/productGrouping.js";
import { formatPrice, statusLabel } from "../../utils/adminFormat.js";
import { colorDot } from "../../utils/productGrouping.js";
import {
  Laptop, X, Cpu, MemoryStick, HardDrive, Palette, Barcode, Package,
  Monitor, Tv, Terminal, Battery, ShieldCheck, Scale,
} from '@lucide/vue';

const props = defineProps({
  modelValue:   { type: Boolean, default: false },
  sanPhamId:    { type: [Number, String], default: null },
  sanPhamName:  { type: String, default: "" },
  posCartCount: { type: Object, default: () => ({}) },
});
const emit = defineEmits(["update:modelValue"]);

const variantList = computed(() => {
  if (props.sanPhamId == null) return [];
  return variantsForDetail(ProductsStore.items ?? [], props.sanPhamId, null);
});

const inCartCount = (bienTheId) => Number(props.posCartCount?.[bienTheId] ?? 0);

const close = () => emit("update:modelValue", false);

const onKey = (e) => { if (e.key === 'Escape') close(); };
onMounted(() => document.addEventListener('keydown', onKey));
onBeforeUnmount(() => document.removeEventListener('keydown', onKey));
</script>

<template>
  <div
    v-if="modelValue"
    style="position: fixed; inset: 0; z-index: 1080; background: var(--bg-overlay); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; padding: 16px;"
    @click.self="close"
  >
    <div
      class="alt-card d-flex flex-column"
      style="width: 960px; max-width: 97vw; max-height: 92vh;"
    >
      <!-- Header -->
      <div class="alt-toolbar fw-bold">
        <div class="d-flex align-items-center gap-2">
          <Package :size="16" color="var(--accent-fg)" />
          <span style="color: var(--text-primary);">{{ sanPhamName }}</span>
          <span class="alt-tag" style="background: var(--bg-card-alt); color: var(--text-secondary);">
            {{ variantList.length }} biến thể
          </span>
        </div>
        <div class="d-flex align-items-center gap-2 ms-auto">
          <button
            type="button"
            aria-label="Đóng"
            style="width: 32px; height: 32px; background: var(--bg-card-alt); border: 1px solid var(--border-color); color: var(--text-secondary); border-radius: 50%; padding: 0; display: inline-flex; align-items: center; justify-content: center; cursor: pointer;"
            @click="close"
          >
            <X :size="16" />
          </button>
        </div>
      </div>

      <!-- Body -->
      <div class="overflow-y-auto p-3">
        <div
          v-if="variantList.length === 0"
          class="text-center py-5 text-secondary"
          style="font-size: 0.85rem;"
        >
          <Laptop :size="40" color="var(--text-muted)" />
          <p class="mt-2 mb-0">Sản phẩm này chưa có biến thể nào.</p>
        </div>

        <div v-else class="row g-3">
          <div
            v-for="v in variantList"
            :key="v.bienTheId"
            class="col-12 col-lg-6"
          >
            <div class="variant-card h-100 d-flex flex-column">
              <!-- Top: ảnh + thông tin định danh (SKU, barcode, trạng thái, CPU tiêu đề) -->
              <div class="d-flex gap-3 p-3 align-items-center" style="background: var(--bg-card-alt); border-bottom: 1px solid var(--border-color);">
                <div class="variant-thumb">
                  <img
                    v-if="v.hinhAnhChinh"
                    :src="v.hinhAnhChinh"
                    :alt="v.tenSanPham"
                  />
                  <Laptop v-else :size="28" class="text-secondary" />
                </div>
                <div class="flex-grow-1" style="min-width: 0;">
                  <div class="d-flex align-items-center justify-content-between gap-2 mb-1">
                    <div class="d-flex align-items-center gap-1.5 min-w-0">
                      <span class="badge font-monospace bg-light-subtle text-body border px-2 py-0.5 text-truncate" style="font-size: 0.75rem;">
                        {{ v.maSku || '—' }}
                      </span>
                      <span v-if="v.barcodeBienThe" class="badge font-monospace bg-body-secondary text-secondary border px-1.5 py-0.5 d-none d-sm-inline-flex align-items-center gap-1" style="font-size: 0.68rem;">
                        <Barcode :size="10" /> {{ v.barcodeBienThe }}
                      </span>
                    </div>
                    <span
                      class="badge rounded-pill flex-shrink-0 d-inline-flex align-items-center gap-1 px-2 py-0.5"
                      :class="v.trangThai === 'active'
                        ? 'bg-success-subtle text-success border border-success-subtle'
                        : 'bg-secondary-subtle text-secondary border'"
                      style="font-size: 0.72rem;"
                    >
                      <span class="status-dot"></span>
                      {{ statusLabel(v.trangThai) }}
                    </span>
                  </div>

                  <!-- Tên CPU nổi bật làm định danh chính của biến thể -->
                  <div class="fw-bold d-flex align-items-center gap-1.5 text-truncate mt-1" style="font-size: 0.92rem; color: var(--text-heading);">
                    <Cpu v-if="v.cpu" :size="15" class="text-primary flex-shrink-0" />
                    <span class="text-truncate">{{ v.cpu || v.tenSanPham }}</span>
                  </div>

                  <!-- Thương hiệu · Danh mục · Nhà cung cấp -->
                  <div class="text-secondary small mt-0.5 text-truncate" style="font-size: 0.72rem;">
                    <span>{{ [v.tenThuongHieu, v.tenDanhMuc, v.tenNhaCungCap].filter(Boolean).join(' · ') || '—' }}</span>
                  </div>
                </div>
              </div>

              <!-- Mid: CẤU HÌNH (Lưới 2 cột cân đối, chip icon đẹp mắt) -->
              <div class="p-3 flex-grow-1 d-flex flex-column justify-content-center">
                <div class="text-secondary small mb-2 fw-bold text-uppercase d-flex align-items-center justify-content-between" style="font-size: 0.68rem; letter-spacing: 0.5px;">
                  <span>Cấu hình chi tiết</span>
                </div>
                <div class="spec-grid">
                  <!-- RAM -->
                  <div v-if="v.ram" class="spec-chip" :title="'RAM: ' + v.ram">
                    <MemoryStick :size="13" class="text-primary flex-shrink-0" />
                    <span class="spec-chip__text">{{ v.ram }}</span>
                  </div>

                  <!-- GPU -->
                  <div v-if="v.gpu" class="spec-chip" :title="'Card đồ họa: ' + v.gpu">
                    <Monitor :size="13" class="flex-shrink-0" style="color: #8b5cf6;" />
                    <span class="spec-chip__text">{{ v.gpu }}</span>
                  </div>

                  <!-- Ổ cứng -->
                  <div v-if="v.oCung" class="spec-chip" :title="'Ổ cứng: ' + v.oCung">
                    <HardDrive :size="13" class="flex-shrink-0" style="color: #06b6d4;" />
                    <span class="spec-chip__text">{{ v.oCung }}</span>
                  </div>

                  <!-- Màn hình -->
                  <div v-if="v.kichThuocManHinh" class="spec-chip" :title="'Màn hình: ' + v.kichThuocManHinh">
                    <Tv :size="13" class="flex-shrink-0" style="color: #0ea5e9;" />
                    <span class="spec-chip__text">{{ v.kichThuocManHinh }}</span>
                  </div>

                  <!-- Màu sắc -->
                  <div v-if="v.mauSac" class="spec-chip" :title="'Màu sắc: ' + v.mauSac">
                    <span class="color-dot" :style="{ background: colorDot(v.mauSac) }"></span>
                    <Palette :size="13" class="flex-shrink-0" style="color: #ec4899;" />
                    <span class="spec-chip__text">{{ v.mauSac }}</span>
                  </div>

                  <!-- Pin -->
                  <div v-if="v.pin" class="spec-chip" :title="'Pin: ' + v.pin">
                    <Battery :size="13" class="flex-shrink-0" style="color: #16a34a;" />
                    <span class="spec-chip__text">{{ v.pin }}</span>
                  </div>

                  <!-- Hệ điều hành -->
                  <div v-if="v.heDieuHanh" class="spec-chip" :title="'Hệ điều hành: ' + v.heDieuHanh">
                    <Terminal :size="13" class="text-secondary flex-shrink-0" />
                    <span class="spec-chip__text">{{ v.heDieuHanh }}</span>
                  </div>

                  <!-- Bảo hành -->
                  <div v-if="v.baoHanhThang" class="spec-chip" :title="'Bảo hành: ' + v.baoHanhThang + ' tháng'">
                    <ShieldCheck :size="13" class="flex-shrink-0" style="color: #d97706;" />
                    <span class="spec-chip__text">{{ v.baoHanhThang }} tháng</span>
                  </div>

                  <!-- Trọng lượng -->
                  <div v-if="v.trongLuongKg" class="spec-chip" :title="'Trọng lượng: ' + v.trongLuongKg + ' kg'">
                    <Scale :size="13" class="text-secondary flex-shrink-0" />
                    <span class="spec-chip__text">{{ v.trongLuongKg }} kg</span>
                  </div>
                </div>
              </div>

              <!-- Bottom: giá + tồn kho (read-only) -->
              <div
                class="mt-auto px-3 py-2.5 d-flex align-items-center justify-content-between"
                style="background: var(--bg-card-alt); border-top: 1px solid var(--border-color);"
              >
                <div class="d-flex flex-column">
                  <span v-if="Number(v.giaGoc) > Number(v.giaBan)" class="text-decoration-line-through text-secondary" style="font-size: 0.7rem;">
                    {{ formatPrice(v.giaGoc) }}
                  </span>
                  <span class="fw-bold font-monospace" style="font-size: 1.05rem; color: var(--accent-fg);">
                    {{ formatPrice(v.giaBan) }}
                  </span>
                </div>
                <div class="d-flex align-items-center gap-2">
                  <span
                    v-if="inCartCount(v.bienTheId) > 0"
                    class="badge rounded-pill bg-danger-subtle text-danger border border-danger-subtle"
                    style="font-size: 0.7rem;"
                  >
                    Trong giỏ: {{ inCartCount(v.bienTheId) }}
                  </span>
                  <span
                    class="badge rounded-pill px-2.5 py-1 font-monospace"
                    :class="(v.soLuongTon ?? 0) > 0
                      ? 'bg-success-subtle text-success border border-success-subtle fw-semibold'
                      : 'bg-body-secondary text-secondary border'"
                    style="font-size: 0.75rem;"
                  >
                    Tồn: {{ v.soLuongTon ?? 0 }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.variant-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
  transition: transform 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}
.variant-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
  border-color: var(--accent);
}

.variant-thumb {
  width: 76px;
  height: 60px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.variant-thumb img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  margin-right: 4px;
  vertical-align: 1px;
}

.spec-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 7px 10px;
}

.spec-chip {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 5px 11px;
  border-radius: 999px;
  font-size: 0.76rem;
  font-weight: 600;
  line-height: 1.3;
  min-width: 0;
  transition: all 0.15s ease;
  background: var(--pink-100, #ffe6f0);
  color: var(--pink-700, #a81b5d);
  border: 1px solid rgba(168, 27, 93, 0.12);
}
.spec-chip:hover {
  background: var(--pink-200, #ffcfe1);
  transform: translateY(-1px);
}

.spec-chip__text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.color-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.2);
  display: inline-block;
  flex-shrink: 0;
}

:root[data-theme="dark"] .spec-chip,
[data-theme="dark"] .spec-chip {
  background: rgba(244, 63, 94, 0.14);
  color: #fda4af;
  border-color: rgba(244, 63, 94, 0.25);
}
:root[data-theme="dark"] .spec-chip:hover,
[data-theme="dark"] .spec-chip:hover {
  background: rgba(244, 63, 94, 0.22);
}
</style>
