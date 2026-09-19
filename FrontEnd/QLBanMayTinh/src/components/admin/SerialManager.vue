<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from "vue";
import {
  Search, Filter, X, ChevronDown, ChevronUp,
  Hash, Layers, Laptop, Barcode, Activity, Calendar,
  SlidersHorizontal, CheckCircle2, Clock, Package, AlertTriangle,
  RotateCcw, Eye, Pencil, Trash2, Plus, Cpu, MemoryStick, HardDrive, Monitor, Lock, User,
} from "@lucide/vue";
import { t } from "../../i18n/index.js";
import * as ChiTietSanPhamService from "../../services/ChiTietSanPhamService.js";
import { ChiTietCpuService, ChiTietRamService, ChiTietGpuService, ChiTietOCungService } from "../../services/ChiTietLinhKienService.js";
import * as DmService from "../../services/DmService.js";
import { formatDate } from "../../utils/adminFormat.js";
import { nowLocalIso } from "../../utils/datetime.js";
import { showToast } from "../../stores/toast.js";
import { askConfirm } from "../../stores/confirm.js";
import { ProductsStore, ensureProducts } from "../../stores/products.js";
import { serialEvents } from "../../stores/serialEvents.js";
import { posCartChiTietIds } from "../../stores/posCart.js";
import SearchSelect from "../common/SearchSelect.vue";
import ProductDetailModal from "./ProductDetailModal.vue";
import Pagination from "../common/Pagination.vue";
import { usePagination } from "../../composables/usePagination.js";

const specLists = reactive({ cpu: [], ram: [], gpu: [], oCung: [] });
const LINH_KIEN_META = {
  cpu:   { idField: 'cpuId',   nameField: 'tenCpu',    itemIdField: 'chiTietCpuId',   service: ChiTietCpuService },
  ram:   { idField: 'ramId',   nameField: 'dungLuong', itemIdField: 'chiTietRamId',   service: ChiTietRamService },
  gpu:   { idField: 'gpuId',   nameField: 'tenGpu',    itemIdField: 'chiTietGpuId',   service: ChiTietGpuService },
  oCung: { idField: 'oCungId', nameField: 'loaiOcung', itemIdField: 'chiTietOCungId', service: ChiTietOCungService },
};

const items = ref([]);
const loading = ref(false);
const search = ref("");

const load = async () => {
  loading.value = true;
  try {
    const [sp, cpu, ram, gpu, oCung] = await Promise.all([
      ChiTietSanPhamService.getAll().catch(() => []),
      ChiTietCpuService.getAll().catch(() => []),
      ChiTietRamService.getAll().catch(() => []),
      ChiTietGpuService.getAll().catch(() => []),
      ChiTietOCungService.getAll().catch(() => []),
    ]);
    items.value = [
      ...(sp ?? []).map((i) => ({ ...i, loai: 'sanPham', rowId: i.chiTietId })),
      ...(cpu ?? []).map((i) => ({ ...i, loai: 'cpu', rowId: i.chiTietCpuId })),
      ...(ram ?? []).map((i) => ({ ...i, loai: 'ram', rowId: i.chiTietRamId })),
      ...(gpu ?? []).map((i) => ({ ...i, loai: 'gpu', rowId: i.chiTietGpuId })),
      ...(oCung ?? []).map((i) => ({ ...i, loai: 'oCung', rowId: i.chiTietOCungId })),
    ];
  } finally {
    loading.value = false;
  }
};
let refreshTimer = null;
onMounted(() => {
  load();
  ensureProducts();
  DmService.getCpu().then((l) => { specLists.cpu = l; }).catch(() => {});
  DmService.getRam().then((l) => { specLists.ram = l; }).catch(() => {});
  DmService.getGpu().then((l) => { specLists.gpu = l; }).catch(() => {});
  DmService.getOCung().then((l) => { specLists.oCung = l; }).catch(() => {});
  // Auto-refresh every 30s to update lock status
  refreshTimer = setInterval(load, 30000);
});
onBeforeUnmount(() => clearInterval(refreshTimer));

// Tu dong reload khi PosPanel da thay doi trang thai serial
watch(() => serialEvents.count, () => { load(); });

const variantOptions = computed(() =>
  (ProductsStore.items ?? []).map((p) => ({ value: p.bienTheId, label: `${p.tenSanPham} — ${p.maSku}` }))
);
const variantLabel = (bienTheId) => variantOptions.value.find((o) => o.value === bienTheId)?.label ?? '';
const findVariant = (bienTheId) => (ProductsStore.items ?? []).find((p) => p.bienTheId === bienTheId);

// Modal "Chi tiet san pham" xem-thuan cho dong loai "sanPham" — chi hien DUNG bien the cua
// serial dang xem (onlyBienTheIds), khong phai ca ho bien the cua san pham do. Dong loai
// linh kien (cpu/ram/gpu/oCung) khong co nut nay — khong co "san pham"/bien the de xem.
const showDetailModal = ref(false);
const detailSanPhamId = ref(null);
const detailSanPhamName = ref('');
const detailOnlyBienTheIds = ref(null);
const openDetail = (item) => {
  const variant = findVariant(item.bienTheId);
  if (!variant) return;
  detailSanPhamId.value = variant.sanPhamId;
  detailSanPhamName.value = variant.tenSanPham;
  detailOnlyBienTheIds.value = [item.bienTheId];
  showDetailModal.value = true;
};

// Nhãn hiển thị cột "Sản phẩm/SKU" cho MỌI loại dòng (sản phẩm lẫn linh kiện) —
// linh kiện đã có sẵn tên spec (tenCpu/dungLuong/...) ngay trong response, không cần
// tra cứu thêm.
const rowSpecLabel = (item) => {
  if (item.loai === 'sanPham') return variantLabel(item.bienTheId) || item.maSku;
  const meta = LINH_KIEN_META[item.loai];
  return meta ? item[meta.nameField] : '';
};

const itemProductName = (item) => {
  if (item.loai === 'sanPham') {
    const v = findVariant(item.bienTheId);
    if (v?.tenSanPham) return v.tenSanPham;
    const label = variantLabel(item.bienTheId);
    if (label && label.includes(' — ')) return label.split(' — ')[0];
    return label || item.tenSanPham || item.maSku || '—';
  }
  return rowSpecLabel(item);
};

const itemProductSku = (item) => {
  if (item.loai === 'sanPham') {
    const v = findVariant(item.bienTheId);
    if (v?.maSku) return v.maSku;
    const label = variantLabel(item.bienTheId);
    if (label && label.includes(' — ')) return label.split(' — ')[1];
    return item.maSku || '';
  }
  return '';
};

// ── Bộ lọc nâng cao: Serial ────────────────────────────────────────────────────
const isFilterOpen = ref(false);
const filterLoai = ref('');       // '' | 'sanPham' | 'cpu' | 'ram' | 'gpu' | 'oCung'
const filterTrangThai = ref('');  // '' | 'trong_kho' | 'giu_hang' | 'da_ban' | ...
const filterInPosCart = ref(false);
const filterNgayFrom = ref('');
const filterNgayTo = ref('');

const activeFilterCount = computed(() => [
  filterLoai.value, filterTrangThai.value,
  filterInPosCart.value ? '1' : '',
  filterNgayFrom.value, filterNgayTo.value,
].filter((v) => v !== '').length);

const resetFilters = () => {
  search.value = '';
  filterLoai.value = '';
  filterTrangThai.value = '';
  filterInPosCart.value = false;
  filterNgayFrom.value = '';
  filterNgayTo.value = '';
};

const filteredItems = computed(() => {
  const q = search.value.trim().toLowerCase();
  return items.value.filter((i) => {
    if (q && ![i.soSerial, rowSpecLabel(i)].some((v) => (v || '').toLowerCase().includes(q))) return false;
    if (filterLoai.value && i.loai !== filterLoai.value) return false;
    if (filterTrangThai.value && i.trangThai !== filterTrangThai.value) return false;
    if (filterInPosCart.value && !isInPosCart(i)) return false;
    const ngay = (i.ngayNhapKho || '').slice(0, 10);
    if (filterNgayFrom.value && ngay < filterNgayFrom.value) return false;
    if (filterNgayTo.value   && ngay > filterNgayTo.value)   return false;
    return true;
  });
});
const { currentPage, totalPages, pagedItems, pageSize } = usePagination(filteredItems);
watch([search, filterLoai, filterTrangThai, filterInPosCart, filterNgayFrom, filterNgayTo], () => {
  currentPage.value = 0;
});

const STATUS_COLOR = {
  trong_kho: '#22c55e',
  giu_hang: '#f59e0b',
  da_ban: '#94a3b8',
  loi_bao_hanh: '#fb923c',
  da_tra_hang: '#38bdf8',
  da_su_dung: '#a78bfa',
};
const statusColor = (s) => STATUS_COLOR[s] ?? '#6b7280';
const statusLabel = (s) => t(`admin.statusLabel.${s}`);

const serialStats = computed(() => {
  const all = items.value ?? [];
  const trongKho = all.filter((i) => i.trangThai === 'trong_kho').length;
  const daBan = all.filter((i) => i.trangThai === 'da_ban').length;
  const giuHangOrLoi = all.filter((i) => i.trangThai === 'giu_hang' || i.trangThai === 'loi_bao_hanh').length;
  return { total: all.length, trongKho, daBan, giuHangOrLoi };
});

// Kiểm tra serial có đang trong giỏ POS hiện tại không (cùng tab, tức thì)
const isInPosCart = (item) => item.loai === 'sanPham' && posCartChiTietIds.value.has(item.chiTietId);

const showModal = ref(false);
const editingId = ref(null);
const formError = ref("");
const saving = ref(false);
const emptyForm = () => ({
  loai: 'sanPham',
  specId: '',
  soSerial: '',
  trangThai: 'trong_kho',
  ngayNhapKho: nowLocalIso().slice(0, 16),
  ghiChu: '',
});
const form = ref(emptyForm());
const STATUS_OPTIONS_SAN_PHAM = ['trong_kho', 'giu_hang', 'da_ban', 'loi_bao_hanh', 'da_tra_hang'];
const STATUS_OPTIONS_LINH_KIEN = ['trong_kho', 'da_su_dung', 'loi_bao_hanh'];
const statusOptions = computed(() =>
  form.value.loai === 'sanPham' ? STATUS_OPTIONS_SAN_PHAM : STATUS_OPTIONS_LINH_KIEN
);
// Đổi Loại (người dùng bấm chọn trong modal) → trạng thái/spec cũ có thể không hợp lệ
// với loại mới, reset về mặc định. Gắn vào @change của <select> (xem Step 8), KHÔNG
// dùng watch(() => form.value.loai) — watch sẽ fire cả lúc openEdit() gán nguyên object
// form mới (loai đổi từ giá trị cũ sang item.loai), xoá mất specId/trangThai vừa set.
const onLoaiChange = () => {
  form.value.trangThai = 'trong_kho';
  form.value.specId = '';
};

const specOptions = computed(() => {
  if (form.value.loai === 'sanPham') return variantOptions.value;
  const meta = LINH_KIEN_META[form.value.loai];
  if (!meta) return [];
  return specLists[form.value.loai].map((s) => ({ value: s[meta.idField], label: s[meta.nameField] }));
});

const openAdd = () => {
  editingId.value = null;
  form.value = emptyForm();
  formError.value = "";
  showModal.value = true;
};
const openEdit = (item) => {
  editingId.value = item.rowId;
  const specId = item.loai === 'sanPham' ? item.bienTheId : item[LINH_KIEN_META[item.loai].idField];
  form.value = {
    loai: item.loai,
    specId,
    soSerial: item.soSerial,
    trangThai: item.trangThai,
    ngayNhapKho: (item.ngayNhapKho || '').slice(0, 16),
    ghiChu: item.ghiChu || '',
  };
  formError.value = "";
  showModal.value = true;
};

const saveSerial = async () => {
  formError.value = "";
  if (!form.value.specId) {
    formError.value = t(form.value.loai === 'sanPham' ? 'admin.serialManager.variantRequired' : 'admin.serialManager.specRequired');
    return;
  }
  if (!form.value.soSerial.trim()) { formError.value = t('admin.serialManager.serialRequired'); return; }
  if (saving.value) return;
  saving.value = true;
  try {
    const common = {
      soSerial: form.value.soSerial.trim(),
      trangThai: form.value.trangThai,
      ngayNhapKho: nowLocalIso(new Date(form.value.ngayNhapKho)),
      ghiChu: form.value.ghiChu || null,
    };
    let res;
    if (form.value.loai === 'sanPham') {
      const body = { bienTheId: Number(form.value.specId), ...common };
      res = editingId.value
        ? await ChiTietSanPhamService.update(editingId.value, body)
        : await ChiTietSanPhamService.create(body);
    } else {
      const meta = LINH_KIEN_META[form.value.loai];
      const body = { [meta.idField]: Number(form.value.specId), ...common };
      res = editingId.value
        ? await meta.service.update(editingId.value, body)
        : await meta.service.create(body);
    }
    if (!res.ok) {
      formError.value = t('admin.errors.saveFailed', { status: res.status, text: await res.text() });
      return;
    }
    showModal.value = false;
    await load();
  } catch (e) {
    formError.value = e.message;
  } finally {
    saving.value = false;
  }
};

const deleteSerial = async (item) => {
  if (!(await askConfirm(t('admin.confirm.deleteSerial')))) return;
  const service = item.loai === 'sanPham' ? ChiTietSanPhamService : LINH_KIEN_META[item.loai].service;
  const res = await service.remove(item.rowId);
  if (!res.ok) {
    showToast(await res.text().catch(() => t('admin.errors.deleteFailed', { status: res.status })), 'error');
    return;
  }
  showToast(t('admin.toast.serialDeleted', { serial: item.soSerial }), 'success');
  await load();
};

// Lưu ý: việc dọn rác serial 'giu_hang' bị kẹt (đơn đã bị xóa/hủy hoặc user đóng tab POS
// giữa chừng) được backend xử lý TỰ ĐỘNG mỗi khi load() chạy — xem
// ChiTietSanPhamService.hienThiChiTietSanPham() gọi releaseOrphanSerials() ở đầu. Staff
// không cần bấm nút, chỉ cần mở tab Kho hàng là bảng sẽ hiển thị serial đã được giải phóng.
</script>

<template>
  <!-- KPI Summary Cards for Serial Manager -->
  <div class="row g-3 mb-3">
    <div class="col-6 col-md-3">
      <div class="card border shadow-sm rounded-3 p-3 h-100" style="background:var(--bg-card); border-color:var(--border-color-soft) !important;">
        <div class="d-flex align-items-center justify-content-between">
          <div>
            <div class="text-secondary small fw-semibold" style="font-size:11.5px;">Tổng serial hệ thống</div>
            <div class="fs-4 fw-bold mt-1" style="color:var(--text-heading);">{{ serialStats.total }}</div>
          </div>
          <div class="rounded-circle d-flex align-items-center justify-content-center flex-shrink-0" style="width:42px;height:42px;background:rgba(168,85,247,0.12);color:#a855f7;">
            <Barcode :size="20" />
          </div>
        </div>
      </div>
    </div>
    <div class="col-6 col-md-3">
      <div class="card border shadow-sm rounded-3 p-3 h-100" style="background:var(--bg-card); border-color:var(--border-color-soft) !important;">
        <div class="d-flex align-items-center justify-content-between">
          <div>
            <div class="text-secondary small fw-semibold" style="font-size:11.5px;">Đang trong kho (sẵn bán)</div>
            <div class="fs-4 fw-bold mt-1 text-success">{{ serialStats.trongKho }}</div>
          </div>
          <div class="rounded-circle d-flex align-items-center justify-content-center flex-shrink-0" style="width:42px;height:42px;background:rgba(34,197,94,0.12);color:#22c55e;">
            <CheckCircle2 :size="20" />
          </div>
        </div>
      </div>
    </div>
    <div class="col-6 col-md-3">
      <div class="card border shadow-sm rounded-3 p-3 h-100" style="background:var(--bg-card); border-color:var(--border-color-soft) !important;">
        <div class="d-flex align-items-center justify-content-between">
          <div>
            <div class="text-secondary small fw-semibold" style="font-size:11.5px;">Đã xuất bán</div>
            <div class="fs-4 fw-bold mt-1 text-primary">{{ serialStats.daBan }}</div>
          </div>
          <div class="rounded-circle d-flex align-items-center justify-content-center flex-shrink-0" style="width:42px;height:42px;background:rgba(59,130,246,0.12);color:#3b82f6;">
            <Package :size="20" />
          </div>
        </div>
      </div>
    </div>
    <div class="col-6 col-md-3">
      <div class="card border shadow-sm rounded-3 p-3 h-100" style="background:var(--bg-card); border-color:var(--border-color-soft) !important;">
        <div class="d-flex align-items-center justify-content-between">
          <div>
            <div class="text-secondary small fw-semibold" style="font-size:11.5px;">Giữ hàng POS / Lỗi</div>
            <div class="fs-4 fw-bold mt-1 text-warning">{{ serialStats.giuHangOrLoi }}</div>
          </div>
          <div class="rounded-circle d-flex align-items-center justify-content-center flex-shrink-0" style="width:42px;height:42px;background:rgba(234,179,8,0.12);color:#eab308;">
            <Clock :size="20" />
          </div>
        </div>
      </div>
    </div>
  </div>

  <div class="alt-card">
    <div class="alt-toolbar">
      <div class="d-flex align-items-center gap-2">
        <Barcode :size="16" class="text-secondary" />
        <span class="alt-toolbar__count">{{ filteredItems.length }}/{{ items.length }} serial</span>
      </div>
      <div class="alt-toolbar__actions">
        <div class="alt-search">
          <Search class="alt-search__icon" :size="14" />
          <input v-model="search" :placeholder="t('admin.serialManager.searchPlaceholder')" />
        </div>
        <button
          class="alt-btn alt-btn--filter"
          :class="{ 'alt-btn--filter-active': activeFilterCount > 0 || isFilterOpen }"
          @click="isFilterOpen = !isFilterOpen"
        >
          <Filter :size="14" /> Bộ lọc
          <span v-if="activeFilterCount > 0" class="filter-badge">{{ activeFilterCount }}</span>
          <ChevronDown v-if="!isFilterOpen" :size="13" />
          <ChevronUp v-else :size="13" />
        </button>
        <button v-if="activeFilterCount > 0" class="alt-btn alt-btn--ghost-sm" @click="resetFilters">
          <X :size="13" /> Xóa lọc
        </button>
        <button class="alt-btn alt-btn--primary d-inline-flex align-items-center gap-1.5" @click="openAdd">
          <Plus :size="14" /> {{ t('admin.serialManager.add') }}
        </button>
      </div>
    </div>

    <!-- Panel lọc nâng cao -->
    <div v-if="isFilterOpen" class="adv-filter-panel">
      <div class="adv-filter-row">
        <div class="adv-filter-group">
          <label class="adv-filter-label">Loại mặt hàng</label>
          <select v-model="filterLoai" class="adv-filter-select">
            <option value="">Tất cả</option>
            <option value="sanPham">Sản phẩm</option>
            <option value="cpu">CPU</option>
            <option value="ram">RAM</option>
            <option value="gpu">GPU</option>
            <option value="oCung">Ổ cứng</option>
          </select>
        </div>
        <div class="adv-filter-group">
          <label class="adv-filter-label">Trạng thái</label>
          <select v-model="filterTrangThai" class="adv-filter-select">
            <option value="">Tất cả</option>
            <option value="trong_kho">Trong kho</option>
            <option value="giu_hang">Giữ hàng (POS)</option>
            <option value="da_ban">Đã bán</option>
            <option value="loi_bao_hanh">Lỗi / Bảo hành</option>
            <option value="da_tra_hang">Đã trả hàng</option>
            <option value="da_su_dung">Đã dùng (linh kiện)</option>
          </select>
        </div>
        <div class="adv-filter-group adv-filter-group--range">
          <label class="adv-filter-label">Ngày nhập kho</label>
          <div class="adv-filter-range">
            <input v-model="filterNgayFrom" type="date" class="adv-filter-input" />
            <span class="adv-filter-sep">–</span>
            <input v-model="filterNgayTo" type="date" class="adv-filter-input" />
          </div>
        </div>
        <div class="adv-filter-group" style="justify-content: flex-end">
          <label class="adv-filter-label">&nbsp;</label>
          <label class="adv-filter-checkbox">
            <input type="checkbox" v-model="filterInPosCart" />
            <span>Trong giỏ POS</span>
          </label>
        </div>
        <button v-if="activeFilterCount > 0" class="adv-filter-reset" @click="resetFilters">
          <X :size="13" /> Xóa bộ lọc
        </button>
      </div>
    </div>

    <div v-if="loading" class="alt-empty">{{ t('admin.serialManager.loading') }}</div>
    <div v-else class="alt-table-wrap">
      <table class="alt-table" style="width: 100%; min-width: 950px;">
        <thead>
          <tr>
            <th style="width:4%; min-width:45px; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><Hash :size="12" /> {{ t('admin.common.stt') }}</span></th>
            <th style="width:8%; min-width:85px; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><Layers :size="12" /> {{ t('admin.serialManager.colLoai') }}</span></th>
            <th style="width:25%; min-width:180px;"><span class="d-inline-flex align-items-center gap-1.5"><Laptop :size="12" /> {{ t('admin.serialManager.colVariant') }}</span></th>
            <th style="width:17%; min-width:160px; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><Barcode :size="12" /> {{ t('admin.serialManager.colSerial') }}</span></th>
            <th style="width:11%; min-width:110px; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><Activity :size="12" /> {{ t('admin.serialManager.colStatus') }}</span></th>
            <th style="width:11%; min-width:120px; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><User :size="12" /> {{ t('admin.serialManager.colPerformer') }}</span></th>
            <th style="width:9%; min-width:95px; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><Calendar :size="12" /> {{ t('admin.serialManager.colDate') }}</span></th>
            <th style="width:16%; min-width:235px; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><SlidersHorizontal :size="12" /> {{ t('admin.serialManager.colAction') }}</span></th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(item, idx) in pagedItems"
            :key="`${item.loai}-${item.rowId}`"
            :class="{ 'sm-row--held': item.trangThai === 'giu_hang' }"
          >
            <td class="text-center text-secondary">{{ currentPage * pageSize + idx + 1 }}</td>
            <td class="text-center text-nowrap">
              <span v-if="item.loai === 'sanPham'" class="badge bg-primary-subtle text-primary border border-primary-subtle px-2 py-1 d-inline-flex align-items-center gap-1" style="font-size:11px;">
                <Laptop :size="11" /> Sản phẩm
              </span>
              <span v-else-if="item.loai === 'cpu'" class="badge bg-info-subtle text-info border border-info-subtle px-2 py-1 d-inline-flex align-items-center gap-1" style="font-size:11px;">
                <Cpu :size="11" /> CPU
              </span>
              <span v-else-if="item.loai === 'ram'" class="badge bg-success-subtle text-success border border-success-subtle px-2 py-1 d-inline-flex align-items-center gap-1" style="font-size:11px;">
                <MemoryStick :size="11" /> RAM
              </span>
              <span v-else-if="item.loai === 'gpu'" class="badge bg-warning-subtle text-warning border border-warning-subtle px-2 py-1 d-inline-flex align-items-center gap-1" style="font-size:11px;">
                <Monitor :size="11" /> GPU
              </span>
              <span v-else-if="item.loai === 'oCung'" class="badge bg-danger-subtle text-danger border border-danger-subtle px-2 py-1 d-inline-flex align-items-center gap-1" style="font-size:11px;">
                <HardDrive :size="11" /> Ổ cứng
              </span>
              <span v-else class="badge rounded-pill bg-secondary-subtle text-secondary border px-2 py-1" style="font-size:11px;">
                {{ t(`admin.productsTabs.${item.loai}`) }}
              </span>
            </td>
            <td>
              <div v-if="item.loai === 'sanPham'" class="d-flex flex-column py-0.5">
                <span class="fw-semibold" style="color:var(--text-heading); font-size: 0.86rem; line-height: 1.35; word-break: break-word;" :title="itemProductName(item)">
                  {{ itemProductName(item) }}
                </span>
                <span v-if="itemProductSku(item)" class="text-secondary font-monospace" style="font-size: 0.74rem;">
                  {{ itemProductSku(item) }}
                </span>
              </div>
              <div v-else>
                <span class="fw-semibold" style="color:var(--text-heading); font-size: 0.86rem; line-height: 1.35; word-break: break-word;">
                  {{ rowSpecLabel(item) }}
                </span>
              </div>
            </td>
            <td class="text-center text-nowrap">
              <span class="font-monospace fw-semibold px-2 py-0.5 rounded text-nowrap" style="font-size: 0.83rem; background: var(--bg-card-alt); border: 1px solid var(--border-color-soft); color: var(--text-primary); letter-spacing: 0.3px; white-space: nowrap; display: inline-block;">
                {{ item.soSerial }}
              </span>
            </td>
            <td class="text-center text-nowrap">
              <span v-if="item.trangThai === 'giu_hang'" class="sm-badge sm-badge--held text-nowrap" :title="isInPosCart(item) ? 'Đang trong giỏ POS hiện tại' : 'Đang giữ (có thể từ phiên POS khác)'">
                <span class="sm-dot sm-dot--held"></span>
                <Clock :size="11" class="me-1" />
                {{ statusLabel(item.trangThai) }}
              </span>
              <span v-else-if="item.trangThai === 'trong_kho'" class="badge rounded-pill bg-success-subtle text-success border border-success-subtle px-2.5 py-1 d-inline-flex align-items-center gap-1 text-nowrap" style="font-size:11.5px;">
                <CheckCircle2 :size="12" /> {{ statusLabel(item.trangThai) }}
              </span>
              <span v-else-if="item.trangThai === 'da_ban'" class="badge rounded-pill bg-secondary-subtle text-secondary border px-2.5 py-1 d-inline-flex align-items-center gap-1 text-nowrap" style="font-size:11.5px;">
                <Package :size="12" /> {{ statusLabel(item.trangThai) }}
              </span>
              <span v-else-if="item.trangThai === 'loi_bao_hanh'" class="badge rounded-pill bg-danger-subtle text-danger border border-danger-subtle px-2.5 py-1 d-inline-flex align-items-center gap-1 text-nowrap" style="font-size:11.5px;">
                <AlertTriangle :size="12" /> {{ statusLabel(item.trangThai) }}
              </span>
              <span v-else-if="item.trangThai === 'da_tra_hang'" class="badge rounded-pill bg-info-subtle text-info border border-info-subtle px-2.5 py-1 d-inline-flex align-items-center gap-1 text-nowrap" style="font-size:11.5px;">
                <RotateCcw :size="12" /> {{ statusLabel(item.trangThai) }}
              </span>
              <span v-else class="text-nowrap">
                <span style="display:inline-block;width:9px;height:9px;border-radius:50%;margin-right:6px;" :style="{ background: statusColor(item.trangThai) }"></span>
                {{ statusLabel(item.trangThai) }}
              </span>
            </td>
            <td class="text-center text-nowrap">
              <span
                v-if="item.lockedBy && item.lockedByTen"
                class="badge d-inline-flex align-items-center gap-1 text-nowrap px-2.5 py-1"
                style="background: #b45309; color: #fef9c3; font-size: 11.5px; font-weight: 600; border-radius: 6px;"
                :title="`Đang được ${item.lockedByTen} chọn trong POS / picker`"
              >
                <Lock :size="11" /> {{ item.lockedByTen }}
              </span>
              <span
                v-else-if="isInPosCart(item)"
                class="badge rounded-pill bg-warning-subtle text-warning border border-warning-subtle px-2.5 py-1 d-inline-flex align-items-center gap-1 text-nowrap"
                style="font-size: 11px; font-weight: 600;"
                title="Đang trong giỏ hàng POS"
              >
                <Clock :size="11" /> Giỏ POS
              </span>
              <span v-else class="text-secondary opacity-75">—</span>
            </td>
            <td class="text-center text-secondary small text-nowrap">{{ formatDate(item.ngayNhapKho) }}</td>
            <td class="text-center">
              <div class="d-inline-flex align-items-center justify-content-start gap-1.5 text-nowrap" style="width: 222px;">
                <button
                  v-if="item.loai === 'sanPham'"
                  class="btn btn-sm btn-outline-info d-inline-flex align-items-center justify-content-center gap-1.5 px-2 py-1 rounded-2 text-nowrap"
                  style="font-size: 11.5px; font-weight: 500; width: 82px;"
                  :title="t('admin.products.detail')"
                  @click="openDetail(item)"
                >
                  <Eye :size="13" />
                  <span>{{ t('admin.products.detail') }}</span>
                </button>
                <button
                  class="btn btn-sm btn-outline-secondary d-inline-flex align-items-center justify-content-center gap-1.5 px-2 py-1 rounded-2 text-nowrap"
                  style="font-size: 11.5px; font-weight: 500; width: 64px;"
                  :title="t('admin.serialManager.edit')"
                  @click="openEdit(item)"
                >
                  <Pencil :size="13" />
                  <span>{{ t('admin.serialManager.edit') }}</span>
                </button>
                <button
                  v-if="item.trangThai === 'trong_kho'"
                  class="btn btn-sm btn-outline-danger d-inline-flex align-items-center justify-content-center gap-1.5 px-2 py-1 rounded-2 text-nowrap"
                  style="font-size: 11.5px; font-weight: 500; width: 64px;"
                  :title="t('admin.serialManager.delete')"
                  @click="deleteSerial(item)"
                >
                  <Trash2 :size="13" />
                  <span>{{ t('admin.serialManager.delete') }}</span>
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="filteredItems.length===0"><td colspan="8" class="alt-empty">{{ t('admin.serialManager.empty') }}</td></tr>
        </tbody>
      </table>
      <div v-if="totalPages > 1" class="alt-pager"><Pagination :current-page="currentPage" :total-pages="totalPages" @page-change="currentPage = $event" /></div>
    </div>
  </div>

  <div v-if="showModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="showModal=false">
    <div class="rounded-3 p-3" style="background:var(--bg-card);width:420px;max-width:94vw;">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <div class="fw-bold" style="color:var(--text-heading);">{{ editingId ? t('admin.serialManager.titleEdit') : t('admin.serialManager.titleAdd') }}</div>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="showModal=false"></button>
      </div>
      <div v-if="formError" class="alert alert-danger small py-2 mb-2">{{ formError }}</div>

      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ t('admin.serialManager.colLoai') }}</label>
        <select v-model="form.loai" class="form-select form-select-sm" :disabled="!!editingId" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" @change="onLoaiChange">
          <option value="sanPham">{{ t('admin.productsTabs.sanPham') }}</option>
          <option value="cpu">{{ t('admin.productsTabs.cpu') }}</option>
          <option value="ram">{{ t('admin.productsTabs.ram') }}</option>
          <option value="gpu">{{ t('admin.productsTabs.gpu') }}</option>
          <option value="oCung">{{ t('admin.productsTabs.oCung') }}</option>
        </select>
      </div>
      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ form.loai === 'sanPham' ? t('admin.serialManager.variantLabel') : t(`admin.productsTabs.${form.loai}`) }}</label>
        <SearchSelect v-model="form.specId" :options="specOptions" :placeholder="form.loai === 'sanPham' ? t('admin.serialManager.variantPlaceholder') : t('admin.serialManager.specPlaceholder')" />
      </div>
      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ t('admin.serialManager.serialLabel') }}</label>
        <input v-model="form.soSerial" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>
      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ t('admin.serialManager.statusLabel') }}</label>
        <select v-model="form.trangThai" class="form-select form-select-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);">
          <option v-for="s in statusOptions" :key="s" :value="s">{{ t(`admin.statusLabel.${s}`) }}</option>
        </select>
      </div>
      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ t('admin.serialManager.dateLabel') }}</label>
        <input v-model="form.ngayNhapKho" type="datetime-local" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>
      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ t('admin.serialManager.noteLabel') }}</label>
        <input v-model="form.ghiChu" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>

      <div class="d-flex justify-content-end gap-2">
        <button class="btn btn-sm btn-outline-secondary" @click="showModal=false">{{ t('admin.serialManager.cancel') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" :disabled="saving" @click="saveSerial">{{ t('admin.serialManager.save') }}</button>
      </div>
    </div>
  </div>

  <ProductDetailModal
    v-model="showDetailModal"
    :san-pham-id="detailSanPhamId"
    :san-pham-name="detailSanPhamName"
    :only-bien-the-ids="detailOnlyBienTheIds"
  />
</template>

<style scoped>
/* Dòng serial đang lên đơn POS — nền vàng nhạt nổi bật để nhân viên nhận biết ngay */
.sm-row--held {
  background: #fffbeb;
}
.sm-row--held:hover {
  background: #fef3c7;
}

/* Badge "Đang lên đơn" — pill amber */
.sm-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-weight: 600;
}
.sm-badge--held {
  color: #92400e;
  background: #fde68a;
  border-radius: 999px;
  padding: 2px 10px 2px 6px;
  font-size: 12.5px;
}

/* Chấm tròn màu amber */
.sm-dot--held {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f59e0b;
  flex-shrink: 0;
}

/* Badge nhỏ "⬤ POS" — chỉ hiện khi serial đang trong giỏ phiên này */
.sm-pos-indicator {
  font-size: 10px; font-weight: 700; color: #b45309; background: #fbbf24;
  border-radius: 999px; padding: 1px 6px; margin-left: 2px; letter-spacing: 0.03em;
}

/* ─── Filter ─── */
.alt-btn--filter {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 6px 12px; border: 1px solid var(--border, #e2e8f0);
  border-radius: 8px; background: var(--surface, #fff);
  color: var(--ink, #1e293b); font-size: 13px; font-weight: 500; cursor: pointer; transition: all 0.15s ease;
}
.alt-btn--filter:hover, .alt-btn--filter-active {
  border-color: var(--pink-400, #f472b6); background: var(--pink-50, #fdf2f8); color: var(--pink-700, #be185d);
}
.filter-badge {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 18px; height: 18px; padding: 0 5px; border-radius: 9px;
  background: var(--pink-600, #db2777); color: #fff; font-size: 11px; font-weight: 700;
}
.alt-btn--ghost-sm {
  display: inline-flex; align-items: center; gap: 4px; padding: 5px 10px;
  border: 1px solid var(--border, #e2e8f0); border-radius: 8px;
  background: transparent; color: var(--muted, #64748b); font-size: 12px; cursor: pointer; transition: all 0.15s;
}
.alt-btn--ghost-sm:hover { background: #fee2e2; color: #dc2626; border-color: #dc2626; }
.adv-filter-panel {
  border-top: 1px solid var(--border, #e2e8f0); background: var(--surface-alt, #f8fafc);
  padding: 12px 16px; animation: slideDown 0.15s ease;
}
@keyframes slideDown { from { opacity:0; transform:translateY(-6px); } to { opacity:1; transform:translateY(0); } }
.adv-filter-row { display: flex; flex-wrap: wrap; align-items: flex-end; gap: 12px; }
.adv-filter-group { display: flex; flex-direction: column; gap: 4px; min-width: 140px; }
.adv-filter-group--range { min-width: 240px; }
.adv-filter-label { font-size: 11px; font-weight: 600; color: var(--muted, #64748b); text-transform: uppercase; letter-spacing: 0.04em; }
.adv-filter-select, .adv-filter-input {
  padding: 6px 10px; border: 1px solid var(--border, #e2e8f0); border-radius: 7px;
  background: #fff; color: var(--ink, #1e293b); font-size: 13px; outline: none; transition: border-color 0.15s; width: 100%;
}
.adv-filter-select:focus, .adv-filter-input:focus { border-color: var(--pink-500, #ec4899); }
.adv-filter-range { display: flex; align-items: center; gap: 6px; }
.adv-filter-range .adv-filter-input { width: 100px; }
.adv-filter-sep { color: var(--muted, #94a3b8); font-size: 13px; font-weight: 600; }
.adv-filter-checkbox {
  display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--ink, #1e293b);
  cursor: pointer; padding: 7px 10px; border: 1px solid var(--border, #e2e8f0);
  border-radius: 7px; background: #fff; white-space: nowrap;
}
.adv-filter-checkbox input { cursor: pointer; accent-color: var(--pink-500, #ec4899); width: 14px; height: 14px; }
.adv-filter-reset {
  display: inline-flex; align-items: center; gap: 5px; padding: 6px 12px;
  border: 1px solid #dc2626; border-radius: 7px; background: transparent; color: #dc2626;
  font-size: 12px; font-weight: 500; cursor: pointer; align-self: flex-end; transition: all 0.15s;
}
.adv-filter-reset:hover { background: #dc2626; color: #fff; }
</style>

