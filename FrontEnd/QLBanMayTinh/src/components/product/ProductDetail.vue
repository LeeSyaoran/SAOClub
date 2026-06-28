<template>
  <div
    class="position-fixed top-0 start-0 w-100 h-100"
    style="background:#111; z-index:900; overflow-y:auto;"
  >
    <!-- ── Header sticky ── -->
    <div
      class="d-flex align-items-center gap-3 px-3 py-2 position-sticky top-0"
      style="background:rgba(17,17,17,0.95); backdrop-filter:blur(8px); border-bottom:1px solid #2a2a2a; z-index:10;"
    >
      <button
        class="btn btn-sm btn-outline-secondary rounded-circle d-flex align-items-center justify-content-center flex-shrink-0"
        style="width:36px; height:36px; padding:0;"
        @click="$emit('close')"
      >‹</button>
      <span class="text-light fw-semibold small text-truncate">{{ activeVariant.tenSanPham }}</span>
      <span
        class="badge ms-auto flex-shrink-0"
        :class="activeVariant.trangThai === 'active' ? 'bg-success' : 'bg-secondary'"
        style="font-size:10px;"
      >{{ activeVariant.trangThai === 'active' ? 'Còn hàng' : 'Hết hàng' }}</span>
    </div>

    <div class="container-xl py-4">
      <div class="row g-4">

        <!-- ── Cột trái: ảnh ── -->
        <div class="col-12 col-lg-5">
          <div
            class="rounded-3 d-flex align-items-center justify-content-center"
            style="background:#1a1a1a; border:1px solid #2a2a2a; min-height:320px; padding:24px;"
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
            <div class="text-secondary small mb-1">
              {{ activeVariant.tenThuongHieu }} · {{ activeVariant.tenDanhMuc }}
            </div>
            <h1 class="text-white fw-black mb-2" style="font-size:1.3rem; line-height:1.3;">
              {{ activeVariant.tenSanPham }}
            </h1>
            <div class="d-flex align-items-baseline gap-3 flex-wrap">
              <span class="fw-black" style="font-size:1.8rem; color:#facc15;">
                {{ formatPrice(activeVariant.giaBan) }}
              </span>
              <span class="text-secondary small">🚚 Giao nhanh 2H · Miễn phí từ 300K</span>
            </div>
          </div>

          <!-- ── Phiên bản (cấu hình) ── -->
          <div v-if="configs.length > 1">
            <div class="text-secondary fw-semibold mb-2" style="font-size:0.72rem; text-transform:uppercase; letter-spacing:0.06em;">
              Phiên bản ({{ configs.length }} cấu hình)
            </div>
            <div class="d-flex flex-wrap gap-2">
              <button
                v-for="v in configs"
                :key="configKey(v)"
                class="btn btn-sm d-flex flex-column align-items-start text-start px-3 py-2"
                style="border-radius:10px; min-width:140px; transition:all 0.15s;"
                :style="activeConfigKey === configKey(v)
                  ? 'background:#2a2200; border:1.5px solid #facc15; color:#facc15;'
                  : 'background:#1f1f1f; border:1.5px solid #333; color:#aaa;'"
                @click="selectConfig(v)"
              >
                <span class="fw-semibold" style="font-size:11px; line-height:1.5;">{{ configLabel(v).line1 }}</span>
                <span v-if="configLabel(v).line2" style="font-size:10px; opacity:0.75;">{{ configLabel(v).line2 }}</span>
              </button>
            </div>
          </div>

          <!-- ── Màu sắc ── -->
          <div v-if="colorsForConfig.some(v => v.mauSac)">
            <div class="text-secondary fw-semibold mb-2" style="font-size:0.72rem; text-transform:uppercase; letter-spacing:0.06em;">
              Màu sắc
            </div>
            <div class="d-flex flex-wrap gap-2">
              <button
                v-for="v in colorsForConfig"
                :key="v.bienTheId"
                class="btn btn-sm d-flex align-items-center gap-2 px-3 py-2"
                style="border-radius:10px; transition:all 0.15s;"
                :style="activeColor === v.mauSac
                  ? 'background:#2a2200; border:1.5px solid #facc15; color:#facc15;'
                  : 'background:#1f1f1f; border:1.5px solid #333; color:#aaa;'"
                @click="selectColor(v)"
              >
                <span
                  class="rounded-circle flex-shrink-0"
                  :style="`width:13px; height:13px; background:${colorDot(v.mauSac)}; border:1.5px solid #666; display:inline-block;`"
                ></span>
                <div class="d-flex flex-column align-items-start text-start">
                  <span class="fw-semibold" style="font-size:11px; line-height:1.3;">{{ v.mauSac }}</span>
                  <span style="font-size:10px; color:#facc15;">{{ formatPrice(v.giaBan) }}</span>
                </div>
              </button>
            </div>
          </div>

          <!-- Meta: màu sắc, SKU, bảo hành -->
          <div class="d-flex flex-wrap gap-3 small text-secondary">
            <span v-if="activeVariant.mauSac">🎨 Màu: <strong class="text-light">{{ activeVariant.mauSac }}</strong></span>
            <span v-if="activeVariant.baoHanhThang">🛡️ Bảo hành: <strong class="text-light">{{ activeVariant.baoHanhThang }} tháng</strong></span>
          </div>

          <!-- ── Thông số kỹ thuật (nhóm) ── -->
          <div v-if="specGroups.phancung.length || specGroups.manha.length || specGroups.hethong.length || specGroups.sanpham.length">
            <div class="text-secondary fw-semibold mb-2" style="font-size:0.72rem; text-transform:uppercase; letter-spacing:0.06em;">
              Thông số kỹ thuật
            </div>
            <div class="d-flex flex-column gap-3">

              <!-- Phần cứng -->
              <div v-if="specGroups.phancung.length" class="rounded-3 overflow-hidden" style="border:1px solid #2a2a2a;">
                <div class="px-3 py-1" style="background:#1f1f1f; font-size:0.68rem; font-weight:700; letter-spacing:0.08em; color:#facc15; text-transform:uppercase;">
                  Phần cứng
                </div>
                <table class="w-100 mb-0" style="border-collapse:collapse;">
                  <tbody>
                    <tr v-for="s in specGroups.phancung" :key="s.label" style="border-top:1px solid #1f1f1f;">
                      <td class="px-3 py-2 text-secondary" style="width:42%; background:#161616; font-size:0.8rem; font-weight:600; white-space:nowrap;">{{ s.label }}</td>
                      <td class="px-3 py-2 text-light" style="background:#1a1a1a; font-size:0.82rem;">{{ s.value }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <!-- Màn hình & Thiết kế -->
              <div v-if="specGroups.manha.length" class="rounded-3 overflow-hidden" style="border:1px solid #2a2a2a;">
                <div class="px-3 py-1" style="background:#1f1f1f; font-size:0.68rem; font-weight:700; letter-spacing:0.08em; color:#60a5fa; text-transform:uppercase;">
                  Màn hình &amp; Thiết kế
                </div>
                <table class="w-100 mb-0" style="border-collapse:collapse;">
                  <tbody>
                    <tr v-for="s in specGroups.manha" :key="s.label" style="border-top:1px solid #1f1f1f;">
                      <td class="px-3 py-2 text-secondary" style="width:42%; background:#161616; font-size:0.8rem; font-weight:600; white-space:nowrap;">{{ s.label }}</td>
                      <td class="px-3 py-2 text-light" style="background:#1a1a1a; font-size:0.82rem;">{{ s.value }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <!-- Hệ thống -->
              <div v-if="specGroups.hethong.length" class="rounded-3 overflow-hidden" style="border:1px solid #2a2a2a;">
                <div class="px-3 py-1" style="background:#1f1f1f; font-size:0.68rem; font-weight:700; letter-spacing:0.08em; color:#34d399; text-transform:uppercase;">
                  Hệ thống
                </div>
                <table class="w-100 mb-0" style="border-collapse:collapse;">
                  <tbody>
                    <tr v-for="s in specGroups.hethong" :key="s.label" style="border-top:1px solid #1f1f1f;">
                      <td class="px-3 py-2 text-secondary" style="width:42%; background:#161616; font-size:0.8rem; font-weight:600; white-space:nowrap;">{{ s.label }}</td>
                      <td class="px-3 py-2 text-light" style="background:#1a1a1a; font-size:0.82rem;">{{ s.value }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <!-- Thông tin sản phẩm -->
              <div v-if="specGroups.sanpham.length" class="rounded-3 overflow-hidden" style="border:1px solid #2a2a2a;">
                <div class="px-3 py-1" style="background:#1f1f1f; font-size:0.68rem; font-weight:700; letter-spacing:0.08em; color:#a78bfa; text-transform:uppercase;">
                  Thông tin sản phẩm
                </div>
                <table class="w-100 mb-0" style="border-collapse:collapse;">
                  <tbody>
                    <tr v-for="s in specGroups.sanpham" :key="s.label" style="border-top:1px solid #1f1f1f;">
                      <td class="px-3 py-2 text-secondary" style="width:42%; background:#161616; font-size:0.8rem; font-weight:600; white-space:nowrap;">{{ s.label }}</td>
                      <td class="px-3 py-2 text-light" style="background:#1a1a1a; font-size:0.82rem;">{{ s.value }}</td>
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
              style="background:#facc15; color:#111; border-radius:12px; font-size:0.95rem;"
              :disabled="activeVariant.trangThai !== 'active'"
              @click="$emit('add-to-cart', activeVariant)"
            >
              🛒 Thêm vào giỏ hàng
            </button>
          </div>

        </div>
      </div><!-- /row -->

      <!-- ── Sản phẩm gợi ý ── -->
      <div v-if="related.length > 0" class="mt-5">
        <h2 class="fw-bold text-white mb-3" style="font-size:1rem; border-bottom:1px solid #2a2a2a; padding-bottom:8px;">
          Có thể bạn cũng thích
        </h2>
        <div class="d-flex gap-3 pb-2" style="overflow-x:auto; scrollbar-width:thin; scrollbar-color:#333 transparent;">
          <div
            v-for="p in related"
            :key="p.sanPhamId"
            class="flex-shrink-0 rounded-3 d-flex flex-column"
            style="width:160px; background:#1a1a1a; border:1px solid #2a2a2a; cursor:pointer; transition:transform 0.15s;"
            @mouseenter="e => e.currentTarget.style.transform='translateY(-3px)'"
            @mouseleave="e => e.currentTarget.style.transform=''"
            @click="$emit('open-product', p)"
          >
            <!-- Ảnh -->
            <div class="d-flex align-items-center justify-content-center rounded-top"
                 style="height:110px; background:#111; padding:8px;">
              <img v-if="p.hinhAnhChinh"
                   :src="p.hinhAnhChinh" :alt="p.tenSanPham"
                   style="max-width:100%; max-height:90px; object-fit:contain;" />
              <span v-else style="font-size:2.5rem;">💻</span>
            </div>
            <!-- Info -->
            <div class="p-2 d-flex flex-column gap-1 flex-grow-1">
              <p class="text-light fw-semibold mb-0"
                 style="font-size:10px; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; line-clamp:2;">
                {{ p.tenSanPham }}
              </p>
              <p class="text-secondary mb-0" style="font-size:9px;">{{ p.tenThuongHieu }}</p>
              <p class="fw-black mb-0 mt-auto" style="font-size:11px; color:#facc15;">
                {{ formatPrice(p.giaBan) }}
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- ── Mô tả sản phẩm ── -->
      <div v-if="activeVariant.moTa" class="mt-5">
        <h2 class="fw-bold text-white mb-3" style="font-size:1rem; border-bottom:1px solid #2a2a2a; padding-bottom:8px;">
          Mô tả sản phẩm
        </h2>
        <div class="text-secondary small" style="line-height:1.8; white-space:pre-wrap;">{{ activeVariant.moTa }}</div>
      </div>

    </div><!-- /container -->
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';

const props = defineProps({
  product:  { type: Object,  required: true },
  products: { type: Array,   default: () => [] },
});

defineEmits(['close', 'add-to-cart', 'open-product']);

// Khóa scroll trang nền khi overlay mở để tránh 2 scrollbar
onMounted(() => { document.body.style.overflow = 'hidden'; });
onUnmounted(() => { document.body.style.overflow = ''; });

const configKey = (v) => `${v.cpu ?? ''}|${v.ram ?? ''}|${v.oCung ?? ''}`;

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

// Nhãn 2 dòng cho nút cấu hình
const configLabel = (v) => ({
  line1: v.cpu || v.ram || 'Cấu hình',
  line2: [v.ram, v.oCung].filter(Boolean).join(' · '),
});

// Màu dot cho color swatch
const colorDot = (mauSac) => {
  if (!mauSac) return '#555';
  const s = mauSac.toLowerCase();
  const map = [
    ['đen','#18181b'], ['den','#18181b'],
    ['trắng','#e4e4e7'], ['trang','#e4e4e7'],
    ['bạc','#94a3b8'], ['bac','#94a3b8'],
    ['xám','#6b7280'], ['xam','#6b7280'],
    ['đỏ','#dc2626'], ['do','#dc2626'],
    ['xanh lá','#16a34a'], ['xanh la','#16a34a'],
    ['xanh dương','#2563eb'], ['xanh duong','#2563eb'],
    ['xanh','#2563eb'],
    ['vàng','#ca8a04'], ['vang','#ca8a04'],
    ['hồng','#ec4899'], ['hong','#ec4899'],
    ['tím','#9333ea'], ['tim','#9333ea'],
    ['cam','#ea580c'],
    ['nâu','#92400e'], ['nau','#92400e'],
  ];
  const found = map.find(([k]) => s.includes(k));
  return found ? found[1] : '#555';
};

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

const formatPrice = (v) =>
  v == null ? 'Liên hệ'
  : new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v);

const row = (label, value) => (value ? { label, value } : null);
const specGroups = computed(() => {
  const v = activeVariant.value ?? {};
  const f = (arr) => arr.filter(Boolean);
  return {
    phancung: f([
      row('Bộ xử lý (CPU)',  v.cpu),
      row('RAM',             v.ram),
      row('Ổ cứng',         v.oCung),
      row('Card đồ họa',    v.gpu),
    ]),
    manha: f([
      row('Kích thước màn hình', v.kichThuocManHinh),
      row('Màu sắc',             v.mauSac),
      row('Trọng lượng',         v.trongLuongKg ? `${v.trongLuongKg} kg` : null),
    ]),
    hethong: f([
      row('Hệ điều hành', v.heDieuHanh),
      row('Pin',          v.pin),
      row('Bảo hành',     v.baoHanhThang ? `${v.baoHanhThang} tháng` : null),
    ]),
    sanpham: f([
      row('Thương hiệu',   v.tenThuongHieu),
      row('Danh mục',      v.tenDanhMuc),
      row('Nhà cung cấp',  v.tenNhaCungCap),
      row('Loại sản phẩm', v.loaiSanPham),
    ]),
  };
});
</script>
