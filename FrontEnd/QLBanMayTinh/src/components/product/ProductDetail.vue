<template>
  <div
    class="position-fixed top-0 start-0 w-100 h-100"
    style="background:var(--bg-card-inset); z-index:900; overflow-y:auto;"
  >
    <!-- ── Header sticky ── -->
    <div
      class="d-flex align-items-center gap-3 px-3 py-2 position-sticky top-0"
      style="background:var(--bg-card-inset); backdrop-filter:blur(8px); border-bottom:1px solid var(--border-color); z-index:10; opacity:0.98;"
    >
      <button
        class="btn btn-sm btn-outline-secondary rounded-circle d-flex align-items-center justify-content-center flex-shrink-0"
        style="width:36px; height:36px; padding:0;"
        @click="$emit('close')"
      >‹</button>
      <span class="fw-semibold small text-truncate" style="color:var(--text-primary);">{{ activeVariant.tenSanPham }}</span>
      <span
        class="badge ms-auto flex-shrink-0"
        :class="activeVariant.trangThai === 'active' ? 'bg-success' : 'bg-secondary'"
        style="font-size:10px;"
      >{{ activeVariant.trangThai === 'active' ? t('productDetail.inStock') : t('productDetail.outOfStock') }}</span>
    </div>

    <div class="container-xl py-4">
      <div class="row g-4">

        <!-- ── Cột trái: ảnh ── -->
        <div class="col-12 col-lg-5">
          <div
            class="rounded-3 d-flex align-items-center justify-content-center"
            style="background:var(--bg-card); border:1px solid var(--border-color); min-height:320px; padding:24px;"
          >
            <img
              v-if="activeVariant.hinhAnhChinh"
              :src="activeVariant.hinhAnhChinh"
              :alt="activeVariant.tenSanPham"
              style="max-width:100%; max-height:360px; object-fit:contain;"
            />
            <span v-else style="font-size:6rem;">💻</span>
          </div>

          <!-- Tags phân loại -->
          <div v-if="activeVariant.phanLoaiTen" class="d-flex flex-wrap gap-2 mt-3">
            <span
              v-for="tag in activeVariant.phanLoaiTen.split(',')"
              :key="tag"
              class="badge bg-warning text-dark"
              style="font-size:11px;"
            >{{ tag.trim() }}</span>
          </div>
        </div>

        <!-- ── Cột phải: thông tin ── -->
        <div class="col-12 col-lg-7 d-flex flex-column gap-3">

          <!-- Tên + brand -->
          <div>
            <div class="small mb-1" style="color:var(--text-secondary);">
              {{ activeVariant.tenThuongHieu }} · {{ activeVariant.tenDanhMuc }}
            </div>
            <h1 class="fw-black mb-2" style="font-size:1.3rem; line-height:1.3; color:var(--text-heading);">
              {{ activeVariant.tenSanPham }}
            </h1>
            <div class="d-flex align-items-baseline gap-3 flex-wrap">
              <span class="fw-black" style="font-size:1.8rem; color:var(--accent-fg);">
                {{ formatPrice(activeVariant.giaBan) }}
              </span>
              <span class="small" style="color:var(--text-secondary);">{{ t('productDetail.freeShipping') }}</span>
            </div>
          </div>

          <!-- ── Phiên bản (cấu hình) ── -->
          <div v-if="configs.length > 1">
            <div class="fw-semibold mb-2" style="font-size:0.72rem; text-transform:uppercase; letter-spacing:0.06em; color:var(--text-secondary);">
              {{ t('productDetail.versions', { count: configs.length }) }}
            </div>
            <div class="d-flex flex-wrap gap-2">
              <button
                v-for="v in configs"
                :key="configKey(v)"
                class="btn btn-sm d-flex flex-column align-items-start text-start px-3 py-2"
                style="border-radius:10px; min-width:140px; transition:all 0.15s;"
                :style="activeConfigKey === configKey(v)
                  ? 'background:rgba(244,63,94,0.12); border:1.5px solid var(--accent); color:var(--accent-fg);'
                  : 'background:var(--bg-input); border:1.5px solid var(--border-color-strong); color:var(--text-secondary);'"
                @click="selectConfig(v)"
              >
                <span class="fw-semibold" style="font-size:11px; line-height:1.5;">{{ configLabel(v).line1 }}</span>
                <span v-if="configLabel(v).line2" style="font-size:10px; opacity:0.75;">{{ configLabel(v).line2 }}</span>
              </button>
            </div>
          </div>

          <!-- ── Màu sắc ── -->
          <div v-if="colorsForConfig.some(v => v.mauSac)">
            <div class="fw-semibold mb-2" style="font-size:0.72rem; text-transform:uppercase; letter-spacing:0.06em; color:var(--text-secondary);">
              {{ t('productDetail.colorHeading') }}
            </div>
            <div class="d-flex flex-wrap gap-2">
              <button
                v-for="v in colorsForConfig"
                :key="v.bienTheId"
                class="btn btn-sm d-flex align-items-center gap-2 px-3 py-2"
                style="border-radius:10px; transition:all 0.15s;"
                :style="activeColor === v.mauSac
                  ? 'background:rgba(244,63,94,0.12); border:1.5px solid var(--accent); color:var(--accent-fg);'
                  : 'background:var(--bg-input); border:1.5px solid var(--border-color-strong); color:var(--text-secondary);'"
                @click="selectColor(v)"
              >
                <span
                  class="rounded-circle flex-shrink-0"
                  :style="`width:13px; height:13px; background:${colorDot(v.mauSac)}; border:1.5px solid #666; display:inline-block;`"
                ></span>
                <div class="d-flex flex-column align-items-start text-start">
                  <span class="fw-semibold" style="font-size:11px; line-height:1.3;">{{ v.mauSac }}</span>
                  <span style="font-size:10px; color:var(--accent-fg);">{{ formatPrice(v.giaBan) }}</span>
                </div>
              </button>
            </div>
          </div>

          <!-- Meta: màu sắc, SKU, bảo hành -->
          <div class="d-flex flex-wrap gap-3 small" style="color:var(--text-secondary);">
            <span v-if="activeVariant.mauSac">🎨 {{ t('productDetail.color') }} <strong style="color:var(--text-primary);">{{ activeVariant.mauSac }}</strong></span>
            <span v-if="activeVariant.baoHanhThang">🛡️ {{ t('productDetail.warranty') }} <strong style="color:var(--text-primary);">{{ activeVariant.baoHanhThang }} {{ t('productDetail.months') }}</strong></span>
          </div>

          <!-- ── Thông số kỹ thuật (nhóm) ── -->
          <div v-if="specGroups.phancung.length || specGroups.manha.length || specGroups.hethong.length || specGroups.sanpham.length">
            <div class="fw-semibold mb-2" style="font-size:0.72rem; text-transform:uppercase; letter-spacing:0.06em; color:var(--text-secondary);">
              {{ t('productDetail.specsHeading') }}
            </div>
            <div class="d-flex flex-column gap-3">

              <!-- Phần cứng -->
              <div v-if="specGroups.phancung.length" class="rounded-3 overflow-hidden" style="border:1px solid var(--border-color);">
                <div class="px-3 py-1" style="background:var(--bg-input); font-size:0.68rem; font-weight:700; letter-spacing:0.08em; color:var(--accent-fg); text-transform:uppercase;">
                  {{ t('productDetail.hardwareGroup') }}
                </div>
                <table class="w-100 mb-0" style="border-collapse:collapse;">
                  <tbody>
                    <tr v-for="s in specGroups.phancung" :key="s.label" style="border-top:1px solid var(--border-color-soft);">
                      <td class="px-3 py-2" style="width:42%; background:var(--bg-card-alt); font-size:0.8rem; font-weight:600; white-space:nowrap; color:var(--text-secondary);">{{ s.label }}</td>
                      <td class="px-3 py-2" style="background:var(--bg-card); font-size:0.82rem; color:var(--text-primary);">{{ s.value }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <!-- Màn hình & Thiết kế -->
              <div v-if="specGroups.manha.length" class="rounded-3 overflow-hidden" style="border:1px solid var(--border-color);">
                <div class="px-3 py-1" style="background:var(--bg-input); font-size:0.68rem; font-weight:700; letter-spacing:0.08em; color:#60a5fa; text-transform:uppercase;">
                  {{ t('productDetail.displayGroup') }}
                </div>
                <table class="w-100 mb-0" style="border-collapse:collapse;">
                  <tbody>
                    <tr v-for="s in specGroups.manha" :key="s.label" style="border-top:1px solid var(--border-color-soft);">
                      <td class="px-3 py-2" style="width:42%; background:var(--bg-card-alt); font-size:0.8rem; font-weight:600; white-space:nowrap; color:var(--text-secondary);">{{ s.label }}</td>
                      <td class="px-3 py-2" style="background:var(--bg-card); font-size:0.82rem; color:var(--text-primary);">{{ s.value }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <!-- Hệ thống -->
              <div v-if="specGroups.hethong.length" class="rounded-3 overflow-hidden" style="border:1px solid var(--border-color);">
                <div class="px-3 py-1" style="background:var(--bg-input); font-size:0.68rem; font-weight:700; letter-spacing:0.08em; color:#34d399; text-transform:uppercase;">
                  {{ t('productDetail.systemGroup') }}
                </div>
                <table class="w-100 mb-0" style="border-collapse:collapse;">
                  <tbody>
                    <tr v-for="s in specGroups.hethong" :key="s.label" style="border-top:1px solid var(--border-color-soft);">
                      <td class="px-3 py-2" style="width:42%; background:var(--bg-card-alt); font-size:0.8rem; font-weight:600; white-space:nowrap; color:var(--text-secondary);">{{ s.label }}</td>
                      <td class="px-3 py-2" style="background:var(--bg-card); font-size:0.82rem; color:var(--text-primary);">{{ s.value }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <!-- Thông tin sản phẩm -->
              <div v-if="specGroups.sanpham.length" class="rounded-3 overflow-hidden" style="border:1px solid var(--border-color);">
                <div class="px-3 py-1" style="background:var(--bg-input); font-size:0.68rem; font-weight:700; letter-spacing:0.08em; color:#a78bfa; text-transform:uppercase;">
                  {{ t('productDetail.productGroup') }}
                </div>
                <table class="w-100 mb-0" style="border-collapse:collapse;">
                  <tbody>
                    <tr v-for="s in specGroups.sanpham" :key="s.label" style="border-top:1px solid var(--border-color-soft);">
                      <td class="px-3 py-2" style="width:42%; background:var(--bg-card-alt); font-size:0.8rem; font-weight:600; white-space:nowrap; color:var(--text-secondary);">{{ s.label }}</td>
                      <td class="px-3 py-2" style="background:var(--bg-card); font-size:0.82rem; color:var(--text-primary);">{{ s.value }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>

            </div>
          </div>

          <!-- Nút thêm vào giỏ -->
          <div class="d-flex gap-2 mt-auto pt-2">
            <button
              class="btn fw-black flex-grow-1 py-2"
              style="background:var(--accent); color:var(--accent-text); border-radius:12px; font-size:0.95rem;"
              :disabled="activeVariant.trangThai !== 'active'"
              @click="$emit('add-to-cart', activeVariant)"
            >
              {{ t('productDetail.addToCart') }}
            </button>
          </div>

        </div>
      </div><!-- /row -->

      <!-- ── Sản phẩm gợi ý ── -->
      <div v-if="related.length > 0" class="mt-5">
        <h2 class="fw-bold mb-3" style="font-size:1rem; border-bottom:1px solid var(--border-color); padding-bottom:8px; color:var(--text-heading);">
          {{ t('productDetail.relatedProducts') }}
        </h2>
        <div class="d-flex gap-3 pb-2" style="overflow-x:auto; scrollbar-width:thin; scrollbar-color:#333 transparent;">
          <div
            v-for="p in related"
            :key="p.sanPhamId"
            class="flex-shrink-0 rounded-3 d-flex flex-column"
            style="width:160px; background:var(--bg-card); border:1px solid var(--border-color); cursor:pointer; transition:transform 0.15s;"
            @mouseenter="e => e.currentTarget.style.transform='translateY(-3px)'"
            @mouseleave="e => e.currentTarget.style.transform=''"
            @click="$emit('open-product', p)"
          >
            <!-- Ảnh -->
            <div class="d-flex align-items-center justify-content-center rounded-top"
                 style="height:110px; background:var(--bg-card-inset); padding:8px;">
              <img v-if="p.hinhAnhChinh"
                   :src="p.hinhAnhChinh" :alt="p.tenSanPham"
                   style="max-width:100%; max-height:90px; object-fit:contain;" />
              <span v-else style="font-size:2.5rem;">💻</span>
            </div>
            <!-- Info -->
            <div class="p-2 d-flex flex-column gap-1 flex-grow-1">
              <p class="fw-semibold mb-0"
                 style="font-size:10px; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; line-clamp:2; color:var(--text-primary);">
                {{ p.tenSanPham }}
              </p>
              <p class="mb-0" style="font-size:9px; color:var(--text-secondary);">{{ p.tenThuongHieu }}</p>
              <p class="fw-black mb-0 mt-auto" style="font-size:11px; color:var(--accent-fg);">
                {{ formatPrice(p.giaBan) }}
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- ── Mô tả sản phẩm ── -->
      <div v-if="activeVariant.moTa" class="mt-5">
        <h2 class="fw-bold mb-3" style="font-size:1rem; border-bottom:1px solid var(--border-color); padding-bottom:8px; color:var(--text-heading);">
          {{ t('productDetail.description') }}
        </h2>
        <div class="small" style="line-height:1.8; white-space:pre-wrap; color:var(--text-secondary);">{{ activeVariant.moTa }}</div>
      </div>

    </div><!-- /container -->
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import { t } from '../../i18n/index.js';
import { formatPrice as formatPriceRaw } from '../../utils/formatPrice.js';
import { configKey, configLabel, colorDot } from '../../utils/productGrouping.js';

const props = defineProps({
  product:  { type: Object,  required: true },
  products: { type: Array,   default: () => [] },
});

defineEmits(['close', 'add-to-cart', 'open-product']);

// Khóa scroll trang nền khi overlay mở để tránh 2 scrollbar
onMounted(() => { document.body.style.overflow = 'hidden'; });
onUnmounted(() => { document.body.style.overflow = ''; });

// Tất cả biến thể cùng sanPhamId
const variants = computed(() =>
  props.products.filter(p => p.sanPhamId === props.product.sanPhamId)
);

// Trạng thái lựa chọn
const activeConfigKey = ref(configKey(props.product));
const activeColor     = ref(props.product.mauSac ?? '');

// Reset khi mở sản phẩm khác
watch(() => props.product, (p) => {
  activeConfigKey.value = configKey(p);
  activeColor.value     = p.mauSac ?? '';
});

// Cấu hình duy nhất (deduplicate theo cpu+ram+oCung)
const configs = computed(() => {
  const seen = new Set();
  return variants.value.filter(v => {
    const k = configKey(v);
    if (seen.has(k)) return false;
    seen.add(k); return true;
  });
});

// Màu sắc của cấu hình đang chọn (deduplicate theo mauSac)
const colorsForConfig = computed(() => {
  const seen = new Set();
  return variants.value
    .filter(v => configKey(v) === activeConfigKey.value)
    .filter(v => {
      const c = v.mauSac ?? '';
      if (seen.has(c)) return false;
      seen.add(c); return true;
    });
});

// Variant hiện tại = giao của cấu hình + màu đã chọn
const activeVariant = computed(() =>
  variants.value.find(v =>
    configKey(v) === activeConfigKey.value &&
    (v.mauSac ?? '') === activeColor.value
  ) ?? props.product
);

const selectConfig = (v) => {
  activeConfigKey.value = configKey(v);
  const available = variants.value.filter(vv => configKey(vv) === activeConfigKey.value);
  if (!available.find(vv => (vv.mauSac ?? '') === activeColor.value))
    activeColor.value = available[0]?.mauSac ?? '';
};
const selectColor = (v) => { activeColor.value = v.mauSac ?? ''; };

// Sản phẩm gợi ý: cùng danh mục hoặc thương hiệu, khác sanPhamId, lấy 1 variant/sp
const related = computed(() => {
  const seen = new Set();
  return props.products
    .filter(p =>
      p.sanPhamId !== props.product.sanPhamId &&
      (p.tenDanhMuc === props.product.tenDanhMuc || p.tenThuongHieu === props.product.tenThuongHieu)
    )
    .filter(p => {
      if (seen.has(p.sanPhamId)) return false;
      seen.add(p.sanPhamId);
      return true;
    })
    .slice(0, 8);
});

const formatPrice = (v) => (v == null ? t('productDetail.contact') : formatPriceRaw(v));

const row = (label, value) => (value ? { label, value } : null);
const specGroups = computed(() => {
  const v = activeVariant.value ?? {};
  const f = (arr) => arr.filter(Boolean);
  const s = t('productDetail.specs');
  return {
    phancung: f([
      row(s.cpu,  v.cpu),
      row(s.ram,  v.ram),
      row(s.storage, v.oCung),
      row(s.gpu, v.gpu),
    ]),
    manha: f([
      row(s.screenSize, v.kichThuocManHinh),
      row(s.color,       v.mauSac),
      row(s.weight,       v.trongLuongKg ? `${v.trongLuongKg} kg` : null),
    ]),
    hethong: f([
      row(s.os,      v.heDieuHanh),
      row(s.battery, v.pin),
      row(s.warranty, v.baoHanhThang ? `${v.baoHanhThang} ${t('productDetail.months')}` : null),
    ]),
    sanpham: f([
      row(s.brand,       v.tenThuongHieu),
      row(s.category,    v.tenDanhMuc),
      row(s.supplier,    v.tenNhaCungCap),
      row(s.productType, v.loaiSanPham),
    ]),
  };
});
</script>
