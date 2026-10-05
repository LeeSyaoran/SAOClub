<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from "vue";
import {
  Search, Filter, X, ChevronDown, ChevronUp,
  Hash, Layers, Laptop, Barcode, Activity, Calendar,
  SlidersHorizontal, CheckCircle2, Clock, Package, AlertTriangle,
  RotateCcw, Eye, Pencil, Plus, Cpu, MemoryStick, HardDrive, Monitor, Lock, User, FileText,
} from "@lucide/vue";
import { t } from "../../i18n/index.js";
import * as ChiTietSanPhamService from "../../services/ChiTietSanPhamService.js";
import * as DmService from "../../services/DmService.js";
import { formatDate } from "../../utils/adminFormat.js";
import { nowLocalIso } from "../../utils/datetime.js";
import { showToast } from "../../stores/toast.js";
import { askConfirm } from "../../stores/confirm.js";
import { ProductsStore, ensureProducts } from "../../stores/products.js";
import { serialEvents } from "../../stores/serialEvents.js";
import { posCartChiTietIds, getPosCartItem } from "../../stores/posCart.js";
import { AuthStore } from "../../stores/index.js";
import SearchSelect from "../common/SearchSelect.vue";
import ProductDetailModal from "./ProductDetailModal.vue";
import Pagination from "../common/Pagination.vue";
import { usePagination } from "../../composables/usePagination.js";

const items = ref([]);
const heldOrderSerials = ref([]);
const loading = ref(false);
const search = ref("");

const load = async () => {
  loading.value = true;
  try {
    const [sp, heldOrders] = await Promise.all([
      ChiTietSanPhamService.getAll().catch(() => []),
      ChiTietSanPhamService.getHeldWithOrder().catch(() => []),
    ]);
    heldOrderSerials.value = heldOrders ?? [];
    items.value = (sp ?? []).map((i) => ({ ...i, loai: 'sanPham', rowId: i.chiTietId }));
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

// Xem chi tiết biến thể của serial
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

// Nhãn hiển thị sản phẩm hoặc linh kiện
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

// Map serials trong đơn hàng đang tiến hành
const activeOrderSerialMap = computed(() => {
  const map = new Map();
  (heldOrderSerials.value || []).forEach((item) => {
    if (item.trangThai === 'loi_bao_hanh') return;
    if (item.chiTietId) map.set(item.chiTietId, item);
    if (item.soSerial) map.set(item.soSerial, item);
  });
  return map;
});

// Kiểm tra serial có đang trong giỏ POS hiện tại không (không tính sản phẩm lỗi/bảo hành)
const isInPosCart = (item) => item.loai === 'sanPham' && item.trangThai !== 'loi_bao_hanh' && posCartChiTietIds.value.has(item.chiTietId);

// Lấy thông tin đơn hàng đang tiến hành của serial (nếu có, không tính bảo hành)
const getActiveOrderItem = (item) => {
  if (item.loai !== 'sanPham' || item.trangThai === 'loi_bao_hanh') return null;
  return activeOrderSerialMap.value.get(item.chiTietId) || activeOrderSerialMap.value.get(item.soSerial) || null;
};

// Kiểm tra serial có thuộc đơn hàng ở giai đoạn chờ xác nhận / đã lên đơn (chưa chuyển sang đóng gói / xuất bán)
const isOrderPendingPacking = (item) => {
  if (item.trangThai === 'loi_bao_hanh' || item.trangThai === 'da_tra_hang') return false;
  const ord = getActiveOrderItem(item);
  if (!ord) return false;
  return ['pending', 'confirmed'].includes(ord.trangThaiDonHang) && item.trangThai !== 'da_ban';
};
const isOrderInProgress = isOrderPendingPacking;

// Kiểm tra serial đã bán (đã có trạng thái da_ban hoặc đơn hàng đã sang bước đóng gói / giao hàng / hoàn tất)
const isOrderSold = (item) => {
  if (item.trangThai === 'loi_bao_hanh') return false;
  if (item.trangThai === 'da_ban') return true;
  const ord = getActiveOrderItem(item);
  if (!ord) return false;
  return ['processing', 'shipping', 'out_for_delivery', 'awaiting_confirmation', 'delivered'].includes(ord.trangThaiDonHang);
};

// Xác định người thực hiện: Admin hay Nhân viên tùy vào tài khoản đang login và thực hiện thanh toán hiện tại
const getSerialPerformer = (item) => {
  if (item.trangThai === 'loi_bao_hanh') return null;
  // 1. Nếu đang trong giỏ POS (thực hiện thanh toán hiện tại):
  if (isInPosCart(item)) {
    const cartItem = getPosCartItem(item.chiTietId);
    const roleStr = (cartItem?.performerRole || (AuthStore.user?.role === 'admin' ? 'Admin' : 'Nhân viên')).toLowerCase();
    const isAdmin = roleStr.includes('admin');
    return {
      role: isAdmin ? 'Admin' : 'Nhân viên',
      name: cartItem?.performerName || AuthStore.user?.hoTen || AuthStore.user?.username || '',
      isAdmin,
    };
  }

  // 2. Nếu đang trong đơn hàng tiến trình hoặc đã lên đơn:
  const ord = getActiveOrderItem(item);
  if (ord) {
    if (ord.nhanVienRole || ord.nhanVienTen) {
      const isAdmin = (ord.nhanVienRole || '').toLowerCase().includes('admin');
      return {
        role: isAdmin ? 'Admin' : 'Nhân viên',
        name: ord.nhanVienTen || '',
        isAdmin,
      };
    }
    const isAdmin = AuthStore.user?.role === 'admin';
    return {
      role: isAdmin ? 'Admin' : 'Nhân viên',
      name: AuthStore.user?.hoTen || '',
      isAdmin,
    };
  }

  // 3. Nếu có lock cũ từ POS session
  if (item.lockedBy && item.lockedByTen) {
    return {
      role: 'Nhân viên',
      name: item.lockedByTen,
      isAdmin: false,
    };
  }

  return null;
};

const filteredItems = computed(() => {
  const q = search.value.trim().toLowerCase();
  return items.value.filter((i) => {
    if (q && ![i.soSerial, rowSpecLabel(i)].some((v) => (v || '').toLowerCase().includes(q))) return false;
    if (filterLoai.value && i.loai !== filterLoai.value) return false;
    if (filterTrangThai.value === 'dang_len_don_pos' && !isInPosCart(i)) return false;
    if (filterTrangThai.value === 'da_len_don' && !isOrderPendingPacking(i)) return false;
    if (filterTrangThai.value === 'da_ban' && !isOrderSold(i)) return false;
    if (filterTrangThai.value === 'trong_kho' && (i.trangThai !== 'trong_kho' || isInPosCart(i) || isOrderPendingPacking(i) || isOrderSold(i))) return false;
    if (filterTrangThai.value && !['dang_len_don_pos', 'da_len_don', 'da_ban', 'trong_kho'].includes(filterTrangThai.value) && i.trangThai !== filterTrangThai.value) return false;
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
  dang_len_don_pos: '#f59e0b',
  da_len_don: '#0284c7',
  giu_hang: '#f59e0b',
  da_ban: '#94a3b8',
  loi_bao_hanh: '#fb923c',
  da_tra_hang: '#38bdf8',
  da_su_dung: '#a78bfa',
};
const statusColor = (s) => STATUS_COLOR[s] ?? '#6b7280';
const statusLabel = (s) => {
  if (s === 'dang_len_don_pos') return 'Đang lên đơn POS';
  if (s === 'da_len_don') return 'Đã lên đơn';
  return t(`admin.statusLabel.${s}`);
};

const serialStats = computed(() => {
  const all = items.value ?? [];
  const trongKho = all.filter((i) => i.trangThai === 'trong_kho' && !isInPosCart(i) && !isOrderPendingPacking(i) && !isOrderSold(i)).length;
  const daBan = all.filter((i) => isOrderSold(i)).length;
  const loiBaoHanh = all.filter((i) => i.trangThai === 'loi_bao_hanh').length;
  return { total: all.length, trongKho, daBan, loiBaoHanh };
});

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
const STATUS_OPTIONS_SAN_PHAM = ['trong_kho', 'da_ban', 'loi_bao_hanh', 'da_tra_hang'];
const STATUS_OPTIONS_LINH_KIEN = ['trong_kho', 'da_su_dung', 'loi_bao_hanh'];
const statusOptions = computed(() =>
  form.value.loai === 'sanPham' ? STATUS_OPTIONS_SAN_PHAM : STATUS_OPTIONS_LINH_KIEN
);
// Đặt lại dữ liệu khi thay đổi loại serial
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

// Tải danh sách serial
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
            <div class="text-secondary small fw-semibold" style="font-size:11.5px;">Lỗi bảo hành</div>
            <div class="fs-4 fw-bold mt-1 text-danger">{{ serialStats.loiBaoHanh }}</div>
          </div>
          <div class="rounded-circle d-flex align-items-center justify-content-center flex-shrink-0" style="width:42px;height:42px;background:rgba(239,68,68,0.12);color:#ef4444;">
            <AlertTriangle :size="20" />
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
            <option value="dang_len_don_pos">Đang lên đơn POS</option>
            <option value="da_len_don">Đã lên đơn (tiến trình)</option>
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
            <th style="width:13%; min-width:160px; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><SlidersHorizontal :size="12" /> {{ t('admin.serialManager.colAction') }}</span></th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(item, idx) in pagedItems"
            :key="`${item.loai}-${item.rowId}`"
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
              <!-- Sản phẩm đang bảo hành / lỗi: không tính đang lên đơn và luôn hiển thị Lỗi / Bảo hành -->
              <span v-if="item.trangThai === 'loi_bao_hanh'" class="badge rounded-pill bg-danger-subtle text-danger border border-danger-subtle px-2.5 py-1 d-inline-flex align-items-center gap-1 text-nowrap" style="font-size:11.5px;">
                <AlertTriangle :size="12" /> {{ statusLabel(item.trangThai) }}
              </span>
              <!-- Đang lên đơn trong giỏ POS -->
              <span
                v-else-if="isInPosCart(item)"
                class="badge rounded-pill bg-warning-subtle text-warning border border-warning-subtle px-2.5 py-1 d-inline-flex align-items-center gap-1 text-nowrap"
                style="font-size:11.5px; font-weight:600;"
                title="Sản phẩm đang được lên đơn tại quầy POS"
              >
                <Clock :size="12" /> Đang lên đơn POS
              </span>
              <!-- Đã bán (bao gồm đơn hàng đã sang bước đóng gói / giao hàng / hoàn tất) -->
              <span
                v-else-if="item.trangThai === 'da_ban' || isOrderSold(item)"
                class="badge rounded-pill bg-secondary-subtle text-secondary border px-2.5 py-1 d-inline-flex align-items-center gap-1 text-nowrap"
                style="font-size:11.5px;"
                :title="getActiveOrderItem(item) ? `Đơn hàng #${getActiveOrderItem(item)?.maDonHang || getActiveOrderItem(item)?.donHangId} — Đã bán` : 'Đã bán'"
              >
                <Package :size="12" /> Đã bán
              </span>
              <!-- Đang ở trạng thái đã lên đơn ở thanh tiến trình -->
              <span
                v-else-if="isOrderPendingPacking(item)"
                class="badge rounded-pill bg-info-subtle text-info border border-info-subtle px-2.5 py-1 d-inline-flex align-items-center gap-1 text-nowrap"
                style="font-size:11.5px; font-weight:600;"
                :title="`Đơn hàng #${getActiveOrderItem(item)?.maDonHang || getActiveOrderItem(item)?.donHangId} — Đã lên đơn ở thanh tiến trình`"
              >
                <FileText :size="12" /> Đã lên đơn
              </span>
              <!-- Các trạng thái thông thường -->
              <span v-else-if="item.trangThai === 'trong_kho'" class="badge rounded-pill bg-success-subtle text-success border border-success-subtle px-2.5 py-1 d-inline-flex align-items-center gap-1 text-nowrap" style="font-size:11.5px;">
                <CheckCircle2 :size="12" /> {{ statusLabel(item.trangThai) }}
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
              <template v-if="getSerialPerformer(item)">
                <span
                  v-if="getSerialPerformer(item).isAdmin"
                  class="badge px-2.5 py-1 d-inline-flex align-items-center gap-1 text-nowrap"
                  style="background: #f3e8ff; color: #7e22ce; border: 1px solid #d8b4fe; font-size: 11px; font-weight: 600; border-radius: 9999px;"
                  :title="getSerialPerformer(item).name ? `Người thực hiện: ${getSerialPerformer(item).name} (Admin)` : 'Người thực hiện: Admin'"
                >
                  <User :size="11" /> Admin
                </span>
                <span
                  v-else
                  class="badge px-2.5 py-1 d-inline-flex align-items-center gap-1 text-nowrap"
                  style="background: #e0f2fe; color: #0284c7; border: 1px solid #bae6fd; font-size: 11px; font-weight: 600; border-radius: 9999px;"
                  :title="getSerialPerformer(item).name ? `Người thực hiện: ${getSerialPerformer(item).name} (Nhân viên)` : 'Người thực hiện: Nhân viên'"
                >
                  <User :size="11" /> Nhân viên
                </span>
              </template>
              <span v-else class="text-secondary opacity-75">—</span>
            </td>
            <td class="text-center text-secondary small text-nowrap">{{ formatDate(item.ngayNhapKho) }}</td>
            <td class="text-center">
              <div class="d-inline-flex align-items-center justify-content-center gap-1.5 text-nowrap">
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

