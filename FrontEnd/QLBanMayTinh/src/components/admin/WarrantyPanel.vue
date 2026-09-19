<script setup>
import { ref, reactive, computed, watch, onMounted, onUnmounted } from "vue";
import { Search } from "@lucide/vue";
import { Filter, ChevronDown, ChevronUp } from '@lucide/vue';
import { t } from "../../i18n/index.js";
import * as ChiTietSanPhamService from "../../services/ChiTietSanPhamService.js";
import * as PhieuBaoHanhService from "../../services/PhieuBaoHanhService.js";
import { formatPrice, formatDate } from "../../utils/adminFormat.js";
import { nowLocalIso } from "../../utils/datetime.js";
import { showToast } from "../../stores/toast.js";
import { askConfirm } from "../../stores/confirm.js";
import { CustomersStore, ensureCustomers } from "../../stores/customers.js";
import { BaoHanhStore, ensureBaoHanh, refreshBaoHanh } from "../../stores/baoHanh.js";
import Pagination from "../common/Pagination.vue";
import { usePagination } from "../../composables/usePagination.js";
import {
  Calendar, Shield, ScanLine, Cpu, MemoryStick, HardDrive, Monitor,
  Tag, User, Phone, Package, ImageOff, X, Camera, AlertTriangle,
  Hash, Barcode, Laptop, CalendarCheck, Clock, ShieldCheck,
  SlidersHorizontal, Wrench, CheckCircle2, DollarSign, Edit3, Eye,
  FileText, Activity, Pencil,
} from '@lucide/vue';

onMounted(() => {
  ensureWarrantyData();
  ensureBaoHanh();
  ensureCustomers();
});

onUnmounted(() => {
  closeUsbScanner();
});

// ── Bảng "Còn hạn bảo hành" — chuyển nguyên xi từ AdminPage.vue ────────────────
const warrantyList = ref([]);
const warrantyLoading = ref(false);
const warrantySearch = ref('');
const isWarrantyFilterOpen = ref(false);
const warrantyFilters = reactive({
  expireDays: '',   // '' | '30' | '60' | '90' — còn hạn trong X ngày
  ngayFrom: '',
  ngayTo: '',
});
const activeWarrantyFilterCount = computed(() =>
  [warrantyFilters.expireDays, warrantyFilters.ngayFrom, warrantyFilters.ngayTo].filter((v) => v !== '').length
);
const resetWarrantyFilters = () => {
  warrantySearch.value = '';
  warrantyFilters.expireDays = '';
  warrantyFilters.ngayFrom = '';
  warrantyFilters.ngayTo = '';
};
let warrantyPromise = null;
const ensureWarrantyData = (force = false) => {
  if (warrantyPromise && !force) return warrantyPromise;
  warrantyLoading.value = true;
  warrantyPromise = ChiTietSanPhamService.getUnderWarranty().catch(() => []).then((list) => {
    warrantyList.value = list;
    warrantyLoading.value = false;
  });
  return warrantyPromise;
};
const filteredWarranty = computed(() => {
  const q = warrantySearch.value.trim().toLowerCase();
  return warrantyList.value.filter((w) => {
    if (q && ![w.soSerial, w.maSku, w.tenSanPham, w.maDonHang, w.tenKhachHang, w.soDienThoaiKhachHang]
      .some((v) => (v || '').toLowerCase().includes(q))) return false;
    const days = daysUntilExpiry(w.ngayHetBaoHanh);
    if (warrantyFilters.expireDays !== '' && days > Number(warrantyFilters.expireDays)) return false;
    const ngayStr = (w.ngayHetBaoHanh || '').slice(0, 10);
    if (warrantyFilters.ngayFrom && ngayStr < warrantyFilters.ngayFrom) return false;
    if (warrantyFilters.ngayTo   && ngayStr > warrantyFilters.ngayTo)   return false;
    return true;
  });
});
const { currentPage: wCurrentPage, totalPages: wTotalPages, pagedItems: pagedWarranty, pageSize: wPageSize } = usePagination(filteredWarranty);
watch([warrantySearch, () => warrantyFilters.expireDays, () => warrantyFilters.ngayFrom, () => warrantyFilters.ngayTo], () => { wCurrentPage.value = 0; });
const daysUntilExpiry = (isoDate) => Math.ceil((new Date(isoDate) - new Date()) / 86400000);

const warrantyStats = computed(() => {
  const list = warrantyList.value ?? [];
  const totalUnderWarranty = list.length;
  const expiringSoon30 = list.filter((w) => daysUntilExpiry(w.ngayHetBaoHanh) <= 30 && daysUntilExpiry(w.ngayHetBaoHanh) >= 0).length;
  const expiringSoon90 = list.filter((w) => daysUntilExpiry(w.ngayHetBaoHanh) <= 90 && daysUntilExpiry(w.ngayHetBaoHanh) > 30).length;
  const totalClaims = (BaoHanhStore.items ?? []).length;
  return { totalUnderWarranty, expiringSoon30, expiringSoon90, totalClaims };
});

// ── Helpers ───────────────────────────────────────────────────────────────────
const customerName = (id) => (CustomersStore.items ?? []).find(c => c.khachHangId === id)?.hoTen ?? (id > 0 ? `Khách #${id}` : 'Khách vãng lai');
const statusLabel = (s) => {
  if (!s) return "—";
  const normalized = String(s).toLowerCase();
  const direct = t(`admin.warrantyClaimStatus.${normalized}`);
  if (direct && direct !== `admin.warrantyClaimStatus.${normalized}`) return direct;
  return s;
};
const STATUS_COLOR = {
  con_bao_hanh: { bg: '#bfdbfe', text: '#1e3a8a' },
  dang_xu_ly:   { bg: '#fde68a', text: '#92400e' },
  da_xu_ly:     { bg: '#bbf7d0', text: '#166534' },
  het_bao_hanh: { bg: '#fecaca', text: '#991b1b' },
  tu_choi:      { bg: '#e5e7eb', text: '#374151' },
};
const statusColor = (s) => STATUS_COLOR[s] ?? { bg: '#e5e7eb', text: '#374151' };

// ── Bảng "Phiếu bảo hành" (CRUD) ────────────────────────────────────────────────
const claimSearch = ref("");
const claimStatusFilter = ref('');  // '' | trạng thái cụ thể
const filteredClaims = computed(() => {
  const q = claimSearch.value.trim().toLowerCase();
  return (BaoHanhStore.items ?? []).filter((p) => {
    if (claimStatusFilter.value && p.trangThai !== claimStatusFilter.value) return false;
    if (!q) return true;
    const name = customerName(p.khachHangId).toLowerCase();
    return String(p.baoHanhId).includes(q) || name.includes(q) || (p.soSerial ?? '').toLowerCase().includes(q);
  });
});
const { currentPage: cCurrentPage, totalPages: cTotalPages, pagedItems: pagedClaims, pageSize: cPageSize } = usePagination(filteredClaims);
watch([claimSearch, claimStatusFilter], () => { cCurrentPage.value = 0; });

const showModal = ref(false);
const editingId = ref(null);
const formError = ref("");
const saving = ref(false);
const lockedInfo = ref(null); // { tenSanPham, maSku, soSerial, tenKhachHang, maDonHang } — hien thi tinh, khong sua

const emptyForm = () => ({
  donHangId: null, bienTheId: null, chiTietId: null, khachHangId: null,
  ngayMua: '', ngayHetBh: '',
  ngayTiepNhan: '', ngayTraKhach: '',
  moTaLoi: '', ketQuaXuLy: '', trangThai: 'con_bao_hanh',
  chiPhiPhatSinh: 0, ghiChu: '',
});
const form = ref(emptyForm());

const openCreateFromWarranty = (w) => {
  editingId.value = null;
  form.value = {
    ...emptyForm(),
    donHangId: w.donHangId,
    bienTheId: w.bienTheId,
    chiTietId: w.chiTietId,
    khachHangId: w.khachHangId,
    ngayMua: (w.ngayGiaoThucTe || '').slice(0, 16),
    ngayHetBh: (w.ngayHetBaoHanh || '').slice(0, 16),
  };
  lockedInfo.value = {
    tenSanPham: w.tenSanPham, maSku: w.maSku, soSerial: w.soSerial,
    tenKhachHang: w.tenKhachHang, maDonHang: w.maDonHang,
  };
  formError.value = "";
  showModal.value = true;
};

// Tạo phiếu thủ công (không qua bảng "Còn hạn bảo hành") — khi serial đã hết hạn hoặc cần
// mở phiếu khi không tìm được serial trong danh sách. Người dùng phải tự nhập các ID.
const openCreateManual = () => {
  editingId.value = null;
  form.value = emptyForm();
  lockedInfo.value = null;
  formError.value = "";
  showModal.value = true;
};

const openEdit = (p) => {
  editingId.value = p.baoHanhId;
  form.value = {
    donHangId: p.donHangId, bienTheId: p.bienTheId, chiTietId: p.chiTietId, khachHangId: p.khachHangId,
    ngayMua: (p.ngayMua || '').slice(0, 16),
    ngayHetBh: (p.ngayHetBh || '').slice(0, 16),
    ngayTiepNhan: (p.ngayTiepNhan || '').slice(0, 16),
    ngayTraKhach: (p.ngayTraKhach || '').slice(0, 16),
    moTaLoi: p.moTaLoi || '',
    ketQuaXuLy: p.ketQuaXuLy || '',
    trangThai: p.trangThai,
    chiPhiPhatSinh: p.chiPhiPhatSinh ?? 0,
    ghiChu: p.ghiChu || '',
  };
  lockedInfo.value = {
    tenSanPham: null, maSku: p.maSku, soSerial: p.soSerial,
    tenKhachHang: customerName(p.khachHangId), maDonHang: `#${p.donHangId}`,
  };
  formError.value = "";
  showModal.value = true;
};

const saveClaim = async () => {
  formError.value = "";
  if (!form.value.donHangId || !form.value.bienTheId || !form.value.khachHangId) {
    formError.value = t('admin.warrantyClaimModal.missingInfo');
    return;
  }
  if (!form.value.moTaLoi.trim()) {
    formError.value = t('admin.warrantyClaimModal.faultRequired');
    return;
  }
  const ngayMuaValid = form.value.ngayMua && !isNaN(new Date(form.value.ngayMua).getTime());
  const ngayHetBhValid = form.value.ngayHetBh && !isNaN(new Date(form.value.ngayHetBh).getTime());
  if (!ngayMuaValid || !ngayHetBhValid) {
    formError.value = t('admin.warrantyClaimModal.datesRequired');
    return;
  }
  if (saving.value) return;
  saving.value = true;
  try {
    const body = {
      donHangId: form.value.donHangId,
      bienTheId: form.value.bienTheId,
      chiTietId: form.value.chiTietId,
      khachHangId: form.value.khachHangId,
      ngayMua: nowLocalIso(new Date(form.value.ngayMua)),
      ngayHetBh: nowLocalIso(new Date(form.value.ngayHetBh)),
      ngayTiepNhan: form.value.ngayTiepNhan ? nowLocalIso(new Date(form.value.ngayTiepNhan)) : null,
      ngayTraKhach: form.value.ngayTraKhach ? nowLocalIso(new Date(form.value.ngayTraKhach)) : null,
      moTaLoi: form.value.moTaLoi,
      ketQuaXuLy: form.value.ketQuaXuLy || null,
      trangThai: form.value.trangThai,
      chiPhiPhatSinh: form.value.chiPhiPhatSinh ?? 0,
      ghiChu: form.value.ghiChu || null,
    };
    const res = await PhieuBaoHanhService.save(editingId.value, body);
    if (!res.ok) {
      formError.value = t('admin.errors.saveFailed', { status: res.status, text: await res.text() });
      return;
    }
    showModal.value = false;
    await refreshBaoHanh();
  } catch (e) {
    formError.value = e.message;
  } finally {
    saving.value = false;
  }
};

// ── Barcode scan lookup ───────────────────────────────────────────────────────
const serialInput = ref('');
const lookupResult = ref(null);
const lookupLoading = ref(false);
const lookupError = ref('');
const lookupErrorCode = ref(''); // 'NOT_FOUND' | 'DELETED' | 'ERROR' | ''
const showUsbScanner = ref(false);
const usbScanStatus = ref('connecting'); // 'connecting' | 'ready' | 'error'
const usbLastScan = ref('');
const usbHistory = ref([]);

let usbEventSource = null;

// Connect to USB proxy via SSE
const openUsbScanner = async () => {
  showUsbScanner.value = true;
  usbScanStatus.value = 'connecting';
  usbLastScan.value = '';
  usbHistory.value = [];

  // Dong ket noi cu
  closeUsbScanner();

  try {
    usbEventSource = new EventSource('http://localhost:8484/events');

    usbEventSource.onopen = () => {
      usbScanStatus.value = 'ready';
    };

    usbEventSource.onmessage = (e) => {
      try {
        const data = JSON.parse(e.data);
        if (data.type === 'connected') {
          usbScanStatus.value = 'ready';
        }
        if (data.text) {
          const now = Date.now();
          usbLastScan.value = data.text;
          usbHistory.value.unshift({ text: data.text, time: data.time || now });
          if (usbHistory.value.length > 10) usbHistory.value.pop();
          // Auto fill + lookup
          serialInput.value = data.text.trim();
          lookupSerial();
        }
      } catch { /* ignore */ }
    };

    usbEventSource.onerror = () => {
      usbScanStatus.value = 'error';
      // Thu ket noi lai sau 3s
      setTimeout(() => {
        if (showUsbScanner.value && usbEventSource?.readyState === EventSource.CLOSED) {
          usbEventSource = null;
          openUsbScanner();
        }
      }, 3000);
    };
  } catch {
    usbScanStatus.value = 'error';
  }
};

const closeUsbScanner = () => {
  if (usbEventSource) {
    usbEventSource.close();
    usbEventSource = null;
  }
  showUsbScanner.value = false;
};

const lookupSerial = async () => {
  const serial = serialInput.value.trim();
  if (!serial) return;
  lookupLoading.value = true;
  lookupError.value = '';
  lookupErrorCode.value = '';
  try {
    lookupResult.value = await PhieuBaoHanhService.lookupBySerial(serial);
  } catch (e) {
    if (e.status === 404) {
      lookupErrorCode.value = e.code === 'DELETED' ? 'DELETED' : 'NOT_FOUND';
      lookupError.value = t(`admin.warrantyScan.${lookupErrorCode.value === 'DELETED' ? 'deleted' : 'notFound'}`, { serial });
    } else {
      lookupErrorCode.value = 'ERROR';
      lookupError.value = t('admin.warrantyScan.error');
    }
    lookupResult.value = null;
  } finally {
    lookupLoading.value = false;
  }
};

const clearLookup = () => {
  serialInput.value = '';
  lookupResult.value = null;
  lookupError.value = '';
  lookupErrorCode.value = '';
};

const createClaimFromLookup = () => {
  if (!lookupResult.value) return;
  const r = lookupResult.value;
  // Only allow creation for sold (da_ban) serials
  if (r.trangThaiSerial !== 'da_ban') return;
  editingId.value = null;
  form.value = {
    ...emptyForm(),
    donHangId: r.donHangId ?? null,
    bienTheId: r.bienTheId ?? null,
    chiTietId: r.chiTietId ?? null,
    khachHangId: r.khachHangId ?? null,
    ngayMua: r.ngayGiaoThucTe ? r.ngayGiaoThucTe.slice(0, 16) : '',
    ngayHetBh: r.ngayHetBaoHanh ? r.ngayHetBaoHanh.slice(0, 16) : '',
  };
  lockedInfo.value = {
    tenSanPham: r.tenSanPham, maSku: r.maSku, soSerial: r.soSerial,
    tenKhachHang: r.tenKhachHang || '', maDonHang: r.maDonHang || `#${r.donHangId}`,
  };
  formError.value = '';
  showModal.value = true;
};

const canCreateClaim = computed(() =>
  lookupResult.value && lookupResult.value.trangThaiSerial === 'da_ban'
);

const isSerialSold = computed(() =>
  lookupResult.value && lookupResult.value.trangThaiSerial === 'da_ban'
);

const isSerialNotSold = computed(() =>
  lookupResult.value && lookupResult.value.trangThaiSerial !== 'da_ban'
);

const warrantyBadge = computed(() => {
  if (!lookupResult.value) return null;
  const r = lookupResult.value;
  if (r.trangThaiSerial !== 'da_ban') return { key: 'warrantyScan.notSold', cls: 'bg-secondary' };
  if (!r.ngayHetBaoHanh) return { key: 'warrantyScan.noWarranty', cls: 'bg-secondary' };
  const daysLeft = Math.ceil((new Date(r.ngayHetBaoHanh) - new Date()) / 86400000);
  if (daysLeft < 0) return { key: 'warrantyScan.expired', cls: 'bg-danger', days: Math.abs(daysLeft) };
  return { key: 'warrantyScan.active', cls: 'bg-success', days: daysLeft };
});

// Banner trang thai thay the badge cu
const lookupBanner = computed(() => {
  if (!lookupResult.value) return null;
  const r = lookupResult.value;
  if (r.trangThaiSerial !== 'da_ban') {
    return {
      icon: '📦',
      bg: 'rgba(107,114,128,0.15)',
      color: '#9ca3af',
      border: 'rgba(107,114,128,0.3)',
      label: t('admin.warrantyScan.banner.inStock'),
    };
  }
  if (!r.ngayHetBaoHanh) {
    return {
      icon: '⚠️',
      bg: 'rgba(251,191,36,0.15)',
      color: '#fbbf24',
      border: 'rgba(251,191,36,0.3)',
      label: t('admin.warrantyScan.noWarranty'),
    };
  }
  const daysLeft = Math.ceil((new Date(r.ngayHetBaoHanh) - new Date()) / 86400000);
  if (daysLeft < 0) {
    return {
      icon: '❌',
      bg: 'rgba(239,68,68,0.15)',
      color: '#f87171',
      border: 'rgba(239,68,68,0.3)',
      label: t('admin.warrantyScan.banner.expired', { count: Math.abs(daysLeft) }),
    };
  }
  return {
    icon: '✅',
    bg: 'rgba(34,197,94,0.15)',
    color: '#22c55e',
    border: 'rgba(34,197,94,0.3)',
    label: t('admin.warrantyScan.banner.active', { count: daysLeft }),
  };
});

</script>

<template>
  <!-- ── Khung quet barcode / tra cuu serial ─────────────────────────────── -->
  <div class="alt-card mb-4" style="border-color:var(--accent);">
    <!-- Search bar -->
    <div class="alt-toolbar">
      <div class="alt-search" style="flex:1; max-width:420px;">
        <ScanLine :size="14" style="position:absolute;left:13px;top:50%;transform:translateY(-50%);color:var(--accent-fg);pointer-events:none;" />
        <input
          v-model="serialInput"
          :placeholder="t('admin.warrantyScan.placeholder')"
          style="padding-left:38px;"
          @keyup.enter="lookupSerial"
        />
      </div>
      <div class="alt-toolbar__actions">
        <!-- USB barcode scanner button -->
        <button
          class="alt-btn"
          style="padding:7px 12px;"
          :title="t('admin.warrantyScan.openCamera')"
          @click="openUsbScanner"
        >
          <Camera :size="13" />
        </button>
        <button class="alt-btn alt-btn--primary" style="padding:7px 20px;" :disabled="lookupLoading" @click="lookupSerial">
          <ScanLine :size="13" /> {{ t('admin.warrantyScan.searchBtn') }}
        </button>
      </div>
    </div>

    <!-- USB Scanner Modal -->
    <Teleport to="body">
      <div v-if="showUsbScanner" class="usb-scanner-overlay" @click.self="closeUsbScanner">
        <div class="usb-scanner-modal">
          <div class="usb-scanner-header">
            <span class="usb-scanner-title">📷 USB Barcode Scanner</span>
            <button class="btn btn-sm btn-link p-0" @click="closeUsbScanner">
              <X :size="18" />
            </button>
          </div>

          <!-- Connection status -->
          <div class="usb-status-bar">
            <div v-if="usbScanStatus === 'connecting'" class="d-flex align-items-center gap-2">
              <div class="spinner-border spinner-border-sm text-primary" role="status"></div>
              <span>Đang kết nối USB...</span>
            </div>
            <div v-else-if="usbScanStatus === 'ready'" class="d-flex align-items-center gap-2" style="color:#10b981;">
              <span style="font-size:1.1em;">●</span>
              <span>Đã kết nối — quét trên điện thoại</span>
            </div>
            <div v-else class="d-flex align-items-center gap-2" style="color:#f87171;">
              <span>⚠️</span>
              <span>Chưa kết nối — đảm bảo đã chạy proxy và kết nối USB</span>
            </div>
          </div>

          <!-- Instructions -->
          <div class="usb-instructions">
            <div class="usb-step">
              <span class="step-num">1</span>
              <span>Trên <strong>điện thoại</strong>, mở Chrome truy cập:</span>
            </div>
            <div class="usb-url-box">
              <code>http://localhost:8484/phone</code>
            </div>
            <div class="usb-step">
              <span class="step-num">2</span>
              <span>Cho phép truy cập camera trên điện thoại</span>
            </div>
            <div class="usb-step">
              <span class="step-num">3</span>
              <span>Đưa camera điện thoại vào <strong>mã vạch</strong> cần quét</span>
            </div>
            <div class="usb-step">
              <span class="step-num">4</span>
              <span>Serial tự động điền vào ô tra cứu bên dưới</span>
            </div>
          </div>

          <!-- Last scanned -->
          <div v-if="usbLastScan" class="usb-last-scan">
            <span class="text-secondary small">Mã vừa quét:</span>
            <span class="fw-bold" style="color:#10b981;word-break:break-all;">{{ usbLastScan }}</span>
          </div>

          <!-- History -->
          <div v-if="usbHistory.length > 0" class="usb-history">
            <div class="small text-secondary mb-1">Lịch sử:</div>
            <div class="usb-history-list">
              <div v-for="(item, i) in usbHistory.slice(0, 5)" :key="i" class="usb-history-item">
                <span>{{ item.text }}</span>
                <span class="text-secondary" style="font-size:0.75em;">
                  {{ new Date(item.time).toLocaleTimeString('vi-VN') }}
                </span>
              </div>
            </div>
          </div>

          <div class="usb-hint small text-center text-secondary mt-2">
            Máy tính nhận barcode từ điện thoại qua cáp USB
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Empty state -->
    <div v-if="!lookupResult && !lookupError && !lookupLoading" class="p-4 text-center">
      <ScanLine :size="32" style="color:var(--text-muted);margin:0 auto 10px;" />
      <div class="small text-secondary" style="max-width:380px;margin:0 auto;line-height:1.6;">
        {{ t('admin.warrantyScan.emptyHint') }}
      </div>
    </div>

    <!-- Loading -->
    <div v-else-if="lookupLoading" class="p-4 text-center">
      <div class="spinner-border text-warning" style="width:1.8rem;height:1.8rem;" role="status"></div>
      <div class="small text-secondary mt-2">{{ t('admin.warrantyScan.loading') }}</div>
    </div>

    <!-- Not found / Deleted -->
    <div v-else-if="lookupError" class="p-4">
      <div class="p-3 rounded-2 text-center" :style="lookupErrorCode === 'DELETED' ? 'background:rgba(251,191,36,0.1);border:1px solid rgba(251,191,36,0.3);' : 'background:rgba(248,113,113,0.1);border:1px solid rgba(248,113,113,0.3);'">
        <component :is="lookupErrorCode === 'DELETED' ? 'AlertTriangle' : X" :size="20" :style="'margin:0 auto 6px;color:' + (lookupErrorCode === 'DELETED' ? '#fbbf24' : '#f87171')" />
        <div class="small" :style="'color:' + (lookupErrorCode === 'DELETED' ? '#fbbf24' : '#f87171')">{{ lookupError }}</div>
      </div>
    </div>

    <!-- Result found -->
    <div v-else-if="lookupResult" class="p-3">
      <!-- Status banner -->
      <div
        v-if="lookupBanner"
        class="mb-3 p-2 rounded-2 text-center fw-semibold"
        :style="'font-size:0.82rem;background:' + lookupBanner.bg + ';color:' + lookupBanner.color + ';border:1px solid ' + lookupBanner.border"
      >
        {{ lookupBanner.icon }}
        {{ lookupBanner.label }}
      </div>

      <!-- Image + name row -->
      <div class="d-flex align-items-start gap-3 mb-3 flex-wrap">
        <!-- Product image — large -->
        <div v-if="lookupResult.hinhAnhBienThe" style="width:120px;height:120px;border-radius:10px;overflow:hidden;flex-shrink:0;border:1px solid var(--border-color-soft);">
          <img :src="lookupResult.hinhAnhBienThe" style="width:100%;height:100%;object-fit:cover;" />
        </div>
        <div v-else style="width:120px;height:120px;border-radius:10px;flex-shrink:0;background:var(--bg-input);display:flex;align-items:center;justify-content:center;border:1px solid var(--border-color-soft);">
          <ImageOff :size="28" style="color:var(--text-muted);" />
        </div>

        <!-- Info -->
        <div style="flex:1;min-width:0;">
          <div class="fw-bold" style="color:var(--text-heading);font-size:0.95rem;">{{ lookupResult.tenSanPham }}</div>
          <div class="text-secondary small font-monospace">#{{ lookupResult.soSerial }}</div>
          <div v-if="lookupResult.maSku" class="text-secondary small font-monospace">{{ lookupResult.maSku }}</div>
        </div>
      </div>

      <!-- 2-column detail -->
      <div class="row g-2 mb-3">
        <!-- Left: machine info -->
        <div class="col-12 col-md-6">
          <div class="p-2 rounded-2" style="background:var(--bg-input);">
            <div class="small text-secondary mb-2 fw-semibold" style="font-size:0.72rem;text-transform:uppercase;letter-spacing:.5px;">{{ t('admin.warrantyScan.machineInfo') }}</div>
            <div class="row g-1 small">
              <div class="col-5 text-secondary">{{ t('admin.warrantyScan.sku') }}</div>
              <div class="col-7 font-monospace">{{ lookupResult.maSku || '—' }}</div>
              <div class="col-5 text-secondary">{{ t('admin.warrantyScan.price') }}</div>
              <div class="col-7 fw-semibold" style="color:var(--accent-fg);">{{ lookupResult.giaBan ? formatPrice(lookupResult.giaBan) : '—' }}</div>
              <div class="col-5 text-secondary">{{ t('admin.warrantyScan.purchaseDate') }}</div>
              <div class="col-7">{{ lookupResult.ngayGiaoThucTe ? formatDate(lookupResult.ngayGiaoThucTe) : '—' }}</div>
              <div class="col-5 text-secondary">{{ t('admin.warrantyScan.warrantyExpiry') }}</div>
              <div class="col-7">{{ lookupResult.ngayHetBaoHanh ? formatDate(lookupResult.ngayHetBaoHanh) : '—' }}</div>
              <div class="col-5 text-secondary">{{ t('admin.warrantyScan.warrantyMonths') }}</div>
              <div class="col-7">{{ lookupResult.baoHanhThang ? lookupResult.baoHanhThang + ' tháng' : '—' }}</div>
              <div class="col-5 text-secondary"><User :size="11" style="vertical-align:-1px;" /> {{ t('admin.warrantyScan.customer') }}</div>
              <div class="col-7">{{ lookupResult.tenKhachHang || '—' }}</div>
              <div class="col-5 text-secondary"><Phone :size="11" style="vertical-align:-1px;" /> {{ t('admin.warrantyScan.phone') }}</div>
              <div class="col-7">{{ lookupResult.soDienThoai || '—' }}</div>
              <div class="col-5 text-secondary"><Package :size="11" style="vertical-align:-1px;" /> {{ t('admin.warrantyScan.order') }}</div>
              <div class="col-7 font-monospace">{{ lookupResult.maDonHang || '—' }}</div>
            </div>
          </div>
        </div>

        <!-- Right: variant specs -->
        <div class="col-12 col-md-6">
          <div class="p-2 rounded-2" style="background:var(--bg-input);">
            <div class="small text-secondary mb-2 fw-semibold" style="font-size:0.72rem;text-transform:uppercase;letter-spacing:.5px;">{{ t('admin.warrantyScan.variantSpecs') }}</div>
            <div class="row g-1 small">
              <div v-if="lookupResult.cpuTen" class="col-5 text-secondary"><Cpu :size="11" style="vertical-align:-1px;" /> CPU</div>
              <div v-if="lookupResult.cpuTen" class="col-7">{{ lookupResult.cpuTen }}</div>
              <div v-if="lookupResult.ramTen" class="col-5 text-secondary"><MemoryStick :size="11" style="vertical-align:-1px;" /> RAM</div>
              <div v-if="lookupResult.ramTen" class="col-7">{{ lookupResult.ramTen }}</div>
              <div v-if="lookupResult.oCungTen" class="col-5 text-secondary"><HardDrive :size="11" style="vertical-align:-1px;" /> Ổ cứng</div>
              <div v-if="lookupResult.oCungTen" class="col-7">{{ lookupResult.oCungTen }}</div>
              <div v-if="lookupResult.gpuTen" class="col-5 text-secondary"><Tag :size="11" style="vertical-align:-1px;" /> GPU</div>
              <div v-if="lookupResult.gpuTen" class="col-7">{{ lookupResult.gpuTen }}</div>
              <div v-if="lookupResult.kichThuocManHinh" class="col-5 text-secondary"><Monitor :size="11" style="vertical-align:-1px;" /> Màn</div>
              <div v-if="lookupResult.kichThuocManHinh" class="col-7">{{ lookupResult.kichThuocManHinh }}</div>
              <div v-if="lookupResult.heDieuHanh" class="col-5 text-secondary">HĐH</div>
              <div v-if="lookupResult.heDieuHanh" class="col-7">{{ lookupResult.heDieuHanh }}</div>
              <div v-if="lookupResult.mauSac" class="col-5 text-secondary">Màu</div>
              <div v-if="lookupResult.mauSac" class="col-7">{{ lookupResult.mauSac }}</div>
              <div v-if="lookupResult.pin" class="col-5 text-secondary">Pin</div>
              <div v-if="lookupResult.pin" class="col-7">{{ lookupResult.pin }}</div>
              <div v-if="lookupResult.trongLuongKg" class="col-5 text-secondary">Trọng lượng</div>
              <div v-if="lookupResult.trongLuongKg" class="col-7">{{ lookupResult.trongLuongKg }} kg</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Warranty history -->
      <div v-if="lookupResult.lichSuPhieuBaoHanh && lookupResult.lichSuPhieuBaoHanh.length > 0" class="mb-3">
        <div class="small text-secondary mb-1 fw-semibold" style="font-size:0.72rem;text-transform:uppercase;letter-spacing:.5px;">{{ t('admin.warrantyScan.historyTitle') }}</div>
        <div class="row g-2">
          <div v-for="p in lookupResult.lichSuPhieuBaoHanh" :key="p.baoHanhId" class="col-12 col-sm-6 col-md-4">
            <div class="p-2 rounded-2 small" style="background:var(--bg-input);">
              <div class="d-flex justify-content-between align-items-center">
                <span class="font-monospace text-secondary">#{{ p.baoHanhId }}</span>
                <span class="badge" :style="{ background: statusColor(p.trangThai).bg, color: statusColor(p.trangThai).text }">{{ statusLabel(p.trangThai) }}</span>
              </div>
              <div class="text-secondary" style="font-size:0.78rem;">{{ p.ngayTiepNhan ? formatDate(p.ngayTiepNhan) : '—' }}</div>
              <div class="text-secondary" style="font-size:0.78rem;" :title="p.moTaLoi">{{ p.moTaLoi ? (p.moTaLoi.length > 40 ? p.moTaLoi.slice(0,40)+'…' : p.moTaLoi) : '—' }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Actions -->
      <div class="d-flex gap-2 flex-wrap justify-content-end border-top pt-3" style="border-color:var(--border-color) !important;">
        <router-link
          v-if="isSerialNotSold && lookupResult.sanPhamId"
          :to="{ name: 'admin-san-pham-detail', params: { id: lookupResult.sanPhamId } }"
          class="alt-btn alt-btn--ghost"
          style="text-decoration:none;"
        >
          <Package :size="13" /> {{ t('admin.warrantyScan.viewVariant') }}
        </router-link>
        <button
          class="alt-btn alt-btn--primary"
          :disabled="!canCreateClaim"
          :title="!canCreateClaim ? t('admin.warrantyScan.notSoldTooltip') : ''"
          @click="createClaimFromLookup"
        >
          <Shield :size="13" /> {{ t('admin.warrantyScan.createClaim') }}
        </button>
        <button class="alt-btn alt-btn--ghost" @click="clearLookup">
          <X :size="13" /> {{ t('admin.warrantyScan.clear') }}
        </button>
      </div>
    </div>
  </div>

  <!-- KPI Summary Cards for Warranty -->
  <div class="row g-3 mb-3">
    <div class="col-6 col-md-3">
      <div class="card border shadow-sm rounded-3 p-3 h-100" style="background:var(--bg-card); border-color:var(--border-color-soft) !important;">
        <div class="d-flex align-items-center justify-content-between">
          <div>
            <div class="text-secondary small fw-semibold" style="font-size:11.5px;">Serial còn bảo hành</div>
            <div class="fs-4 fw-bold mt-1 text-success">{{ warrantyStats.totalUnderWarranty }}</div>
          </div>
          <div class="rounded-circle d-flex align-items-center justify-content-center flex-shrink-0" style="width:42px;height:42px;background:rgba(34,197,94,0.12);color:#22c55e;">
            <ShieldCheck :size="20" />
          </div>
        </div>
      </div>
    </div>
    <div class="col-6 col-md-3">
      <div class="card border shadow-sm rounded-3 p-3 h-100" style="background:var(--bg-card); border-color:var(--border-color-soft) !important;">
        <div class="d-flex align-items-center justify-content-between">
          <div>
            <div class="text-secondary small fw-semibold" style="font-size:11.5px;">Sắp hết hạn (≤30 ngày)</div>
            <div class="fs-4 fw-bold mt-1 text-danger">{{ warrantyStats.expiringSoon30 }}</div>
          </div>
          <div class="rounded-circle d-flex align-items-center justify-content-center flex-shrink-0" style="width:42px;height:42px;background:rgba(239,68,68,0.12);color:#ef4444;">
            <AlertTriangle :size="20" />
          </div>
        </div>
      </div>
    </div>
    <div class="col-6 col-md-3">
      <div class="card border shadow-sm rounded-3 p-3 h-100" style="background:var(--bg-card); border-color:var(--border-color-soft) !important;">
        <div class="d-flex align-items-center justify-content-between">
          <div>
            <div class="text-secondary small fw-semibold" style="font-size:11.5px;">Hết hạn trong ≤90 ngày</div>
            <div class="fs-4 fw-bold mt-1 text-warning">{{ warrantyStats.expiringSoon90 }}</div>
          </div>
          <div class="rounded-circle d-flex align-items-center justify-content-center flex-shrink-0" style="width:42px;height:42px;background:rgba(234,179,8,0.12);color:#eab308;">
            <Clock :size="20" />
          </div>
        </div>
      </div>
    </div>
    <div class="col-6 col-md-3">
      <div class="card border shadow-sm rounded-3 p-3 h-100" style="background:var(--bg-card); border-color:var(--border-color-soft) !important;">
        <div class="d-flex align-items-center justify-content-between">
          <div>
            <div class="text-secondary small fw-semibold" style="font-size:11.5px;">Phiếu BH đã tiếp nhận</div>
            <div class="fs-4 fw-bold mt-1 text-primary">{{ warrantyStats.totalClaims }}</div>
          </div>
          <div class="rounded-circle d-flex align-items-center justify-content-center flex-shrink-0" style="width:42px;height:42px;background:rgba(59,130,246,0.12);color:#3b82f6;">
            <Wrench :size="20" />
          </div>
        </div>
      </div>
    </div>
  </div>

  <div class="alt-card mb-4">
    <div class="alt-toolbar">
      <div class="d-flex align-items-center gap-2">
        <ShieldCheck :size="16" class="text-success" />
        <span class="alt-toolbar__count">{{ filteredWarranty.length }} {{ t('admin.warranty.countSuffix') }}</span>
      </div>
      <span class="alt-tag" style="background:var(--bg-card-alt);color:var(--text-secondary);"><Calendar :size="11" /> {{ t('admin.warranty.today') }}: {{ formatDate(new Date()) }}</span>
      <div class="alt-toolbar__actions">
        <div class="alt-search">
          <Search class="alt-search__icon" :size="14" />
          <input v-model="warrantySearch" :placeholder="t('admin.warranty.searchPlaceholder')" />
        </div>
        <button
          class="alt-btn alt-btn--filter"
          :class="{ 'alt-btn--filter-active': activeWarrantyFilterCount > 0 || isWarrantyFilterOpen }"
          @click="isWarrantyFilterOpen = !isWarrantyFilterOpen"
        >
          <Filter :size="13" /> Bộ lọc
          <span v-if="activeWarrantyFilterCount > 0" class="filter-badge">{{ activeWarrantyFilterCount }}</span>
          <ChevronDown v-if="!isWarrantyFilterOpen" :size="12" />
          <ChevronUp v-else :size="12" />
        </button>
        <button v-if="activeWarrantyFilterCount > 0" class="alt-btn alt-btn--ghost-sm" @click="resetWarrantyFilters">
          <X :size="12" /> Xóa lọc
        </button>
      </div>
    </div>

    <!-- Panel lọc bảo hành -->
    <div v-if="isWarrantyFilterOpen" class="adv-filter-panel">
      <div class="adv-filter-row">
        <div class="adv-filter-group">
          <label class="adv-filter-label">Sắp hết hạn</label>
          <select v-model="warrantyFilters.expireDays" class="adv-filter-select">
            <option value="">Tất cả</option>
            <option value="30">Trong 30 ngày</option>
            <option value="60">Trong 60 ngày</option>
            <option value="90">Trong 90 ngày</option>
          </select>
        </div>
        <div class="adv-filter-group adv-filter-group--range">
          <label class="adv-filter-label">Ngày hết hạn</label>
          <div class="adv-filter-range">
            <input v-model="warrantyFilters.ngayFrom" type="date" class="adv-filter-input" />
            <span class="adv-filter-sep">–</span>
            <input v-model="warrantyFilters.ngayTo" type="date" class="adv-filter-input" />
          </div>
        </div>
        <button v-if="activeWarrantyFilterCount > 0" class="adv-filter-reset" @click="resetWarrantyFilters">
          <X :size="12" /> Xóa bộ lọc
        </button>
      </div>
    </div>
    <div v-if="warrantyLoading" class="alt-empty">{{ t('admin.warranty.loading') }}</div>
    <div v-else class="alt-table-wrap">
      <table class="alt-table">
        <thead>
          <tr>
            <th style="width:4%; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><Hash :size="12" /> {{ t('admin.common.stt') }}</span></th>
            <th style="width:11%; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100">{{ t('admin.warranty.colSerial') }}</span></th>
            <th style="width:23%;"><span class="d-inline-flex align-items-center gap-1.5"><Laptop :size="12" /> {{ t('admin.warranty.colProduct') }}</span></th>
            <th style="width:13%;"><span class="d-inline-flex align-items-center gap-1.5"><User :size="12" /> {{ t('admin.warranty.colCustomer') }}</span></th>
            <th style="width:10%; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><Phone :size="12" /> {{ t('admin.warranty.colPhone') }}</span></th>
            <th style="width:10%; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100">{{ t('admin.warranty.colOrder') }}</span></th>
            <th style="width:8%; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><Calendar :size="12" /> {{ t('admin.warranty.colDelivered') }}</span></th>
            <th style="width:8%; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><CalendarCheck :size="12" /> {{ t('admin.warranty.colExpires') }}</span></th>
            <th style="width:11%; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><Clock :size="12" /> {{ t('admin.warranty.colRemaining') }}</span></th>
            <th style="width:12%; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><SlidersHorizontal :size="12" /> {{ t('admin.warranty.colAction') }}</span></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(w, idx) in pagedWarranty" :key="w.chiTietId">
            <td class="text-center text-secondary">{{ wCurrentPage * wPageSize + idx + 1 }}</td>
            <td class="text-center">
              <span class="font-monospace fw-semibold px-2 py-0.5 rounded" style="font-size: 0.83rem; background: var(--bg-card-alt); border: 1px solid var(--border-color-soft); color: var(--text-primary); letter-spacing: 0.3px;">
                {{ w.soSerial }}
              </span>
            </td>
            <td>
              <div class="d-flex flex-column py-0.5">
                <div class="d-flex align-items-center gap-2">
                  <Laptop :size="14" class="text-primary flex-shrink-0" />
                  <span class="fw-semibold text-truncate" style="color:var(--text-heading); font-size: 0.86rem;" :title="w.tenSanPham">
                    {{ w.tenSanPham }}
                  </span>
                </div>
                <span v-if="w.maSku" class="text-secondary font-monospace" style="font-size: 0.74rem; padding-left: 22px;">
                  {{ w.maSku }}
                </span>
              </div>
            </td>
            <td>
              <div class="d-flex align-items-center gap-2 text-nowrap">
                <User :size="13" class="text-secondary flex-shrink-0" />
                <span>{{ w.tenKhachHang || 'Khách vãng lai' }}</span>
              </div>
            </td>
            <td class="text-center">
              <div class="d-inline-flex align-items-center gap-1.5 font-monospace small text-nowrap">
                <Phone :size="12" class="text-muted flex-shrink-0" />
                <span>{{ w.soDienThoaiKhachHang || '—' }}</span>
              </div>
            </td>
            <td class="text-center">
              <span class="badge font-monospace bg-light-subtle text-body border px-2 py-1 text-nowrap">
                {{ w.maDonHang }}
              </span>
            </td>
            <td class="text-center small text-secondary text-nowrap">{{ formatDate(w.ngayGiaoThucTe) }}</td>
            <td class="text-center small text-secondary text-nowrap">{{ formatDate(w.ngayHetBaoHanh) }}</td>
            <td class="text-center">
              <span
                v-if="daysUntilExpiry(w.ngayHetBaoHanh) <= 30"
                class="badge rounded-pill bg-danger-subtle text-danger border border-danger-subtle px-2.5 py-1 d-inline-flex align-items-center gap-1.5 text-nowrap"
                style="font-size:11px;"
              >
                <AlertTriangle :size="12" /> {{ t('admin.warranty.daysLeft', { count: daysUntilExpiry(w.ngayHetBaoHanh) }) }} (Sắp hết)
              </span>
              <span
                v-else-if="daysUntilExpiry(w.ngayHetBaoHanh) <= 90"
                class="badge rounded-pill bg-warning-subtle text-warning border border-warning-subtle px-2.5 py-1 d-inline-flex align-items-center gap-1.5 text-nowrap"
                style="font-size:11px;"
              >
                <Clock :size="12" /> {{ t('admin.warranty.daysLeft', { count: daysUntilExpiry(w.ngayHetBaoHanh) }) }}
              </span>
              <span
                v-else
                class="badge rounded-pill bg-success-subtle text-success border border-success-subtle px-2.5 py-1 d-inline-flex align-items-center gap-1.5 text-nowrap"
                style="font-size:11px;"
              >
                <ShieldCheck :size="12" /> {{ t('admin.warranty.daysLeft', { count: daysUntilExpiry(w.ngayHetBaoHanh) }) }}
              </span>
            </td>
            <td class="text-center">
              <button
                class="btn btn-sm btn-outline-warning d-inline-flex align-items-center gap-1.5 px-2.5 py-1 rounded-2 shadow-sm text-nowrap"
                style="font-size:12px; font-weight:600;"
                :title="t('admin.warranty.createClaim')"
                @click="openCreateFromWarranty(w)"
              >
                <Wrench :size="12" /> Tạo phiếu
              </button>
            </td>
          </tr>
          <tr v-if="filteredWarranty.length===0"><td colspan="10" class="alt-empty">{{ t('admin.warranty.empty') }}</td></tr>
        </tbody>
      </table>
      <div v-if="wTotalPages > 1" class="alt-pager"><Pagination :current-page="wCurrentPage" :total-pages="wTotalPages" @page-change="wCurrentPage = $event" /></div>
    </div>
  </div>

  <div class="alt-card">
    <div class="alt-toolbar">
      <div class="d-flex align-items-center gap-2">
        <Wrench :size="16" class="text-primary" />
        <span class="alt-toolbar__count">{{ filteredClaims.length }}/{{ (BaoHanhStore.items ?? []).length }} {{ t('admin.warrantyClaims.countSuffix') }}</span>
      </div>
      <div class="alt-toolbar__actions">
        <div class="alt-search">
          <Search class="alt-search__icon" :size="14" />
          <input v-model="claimSearch" :placeholder="t('admin.warrantyClaims.searchPlaceholder')" />
        </div>
        <select v-model="claimStatusFilter" class="alt-select" style="font-size:13px;">
          <option value="">Tất cả trạng thái</option>
          <option value="con_bao_hanh">Còn bảo hành</option>
          <option value="dang_xu_ly">Đang xử lý</option>
          <option value="da_xu_ly">Đã xử lý</option>
          <option value="het_bao_hanh">Hết bảo hành</option>
          <option value="tu_choi">Từ chối</option>
        </select>
        <button v-if="claimStatusFilter" class="alt-btn alt-btn--ghost-sm" @click="claimStatusFilter = ''">
          <X :size="12" /> Xóa lọc
        </button>
        <button class="alt-btn alt-btn--ghost d-inline-flex align-items-center gap-1.5" style="padding:4px 12px;" @click="openCreateManual">
          <Shield :size="13" /> {{ t('admin.warrantyClaims.createManual') }}
        </button>
      </div>
    </div>
    <div v-if="BaoHanhStore.loading" class="alt-empty">{{ t('admin.warrantyClaims.loading') }}</div>
    <div v-else class="alt-table-wrap">
      <table class="alt-table">
        <thead>
          <tr>
            <th style="width:4%; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><Hash :size="12" /> {{ t('admin.common.stt') }}</span></th>
            <th style="width:9%; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100">{{ t('admin.warrantyClaims.colId') }}</span></th>
            <th style="width:20%;"><span class="d-inline-flex align-items-center gap-1.5"><Laptop :size="12" /> {{ t('admin.warrantyClaims.colProduct') }}</span></th>
            <th style="width:13%; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100">{{ t('admin.warrantyClaims.colSerial') }}</span></th>
            <th style="width:15%;"><span class="d-inline-flex align-items-center gap-1.5"><User :size="12" /> {{ t('admin.warrantyClaims.colCustomer') }}</span></th>
            <th style="width:9%; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100">{{ t('admin.warrantyClaims.colOrder') }}</span></th>
            <th style="width:10%; text-align:end;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-end w-100"><DollarSign :size="12" /> {{ t('admin.warrantyClaims.colCost') }}</span></th>
            <th style="width:10%; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><Activity :size="12" /> {{ t('admin.warrantyClaims.colStatus') }}</span></th>
            <th style="width:10%; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><SlidersHorizontal :size="12" /> {{ t('admin.warrantyClaims.colAction') }}</span></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(p, idx) in pagedClaims" :key="p.baoHanhId">
            <td class="text-center text-secondary">{{ cCurrentPage * cPageSize + idx + 1 }}</td>
            <td class="text-center">
              <span class="badge font-monospace bg-body-secondary text-body border px-2 py-1">
                #{{ p.baoHanhId }}
              </span>
            </td>
            <td>
              <span class="badge rounded-pill bg-light-subtle text-secondary border px-2 py-1 font-monospace" style="font-size:11px;">
                {{ p.maSku }}
              </span>
            </td>
            <td class="text-center">
              <span v-if="p.soSerial" class="font-monospace fw-semibold px-2 py-0.5 rounded" style="font-size: 0.83rem; background: var(--bg-card-alt); border: 1px solid var(--border-color-soft); color: var(--text-primary); letter-spacing: 0.3px;">
                {{ p.soSerial }}
              </span>
              <span v-else class="text-secondary">—</span>
            </td>
            <td>
              <div class="d-flex align-items-center gap-2 text-nowrap">
                <User :size="13" class="text-secondary flex-shrink-0" />
                <span>{{ customerName(p.khachHangId) }}</span>
              </div>
            </td>
            <td class="text-center">
              <span class="badge font-monospace bg-light-subtle text-body border px-2 py-1">
                #{{ p.donHangId }}
              </span>
            </td>
            <td class="text-end">
              <span class="fw-semibold font-monospace" :class="Number(p.chiPhiPhatSinh) > 0 ? 'text-danger' : 'text-muted'">
                {{ formatPrice(p.chiPhiPhatSinh) }}
              </span>
            </td>
            <td class="text-center">
              <span class="alt-tag" :style="{ background: statusColor(p.trangThai).bg, color: statusColor(p.trangThai).text }">
                {{ statusLabel(p.trangThai) }}
              </span>
            </td>
            <td class="text-center">
              <div class="d-flex justify-content-center gap-1 text-nowrap">
                <button
                  class="btn btn-sm btn-outline-secondary d-inline-flex align-items-center gap-1.5 px-2.5 py-1 rounded-2 text-nowrap"
                  style="font-size:12px; font-weight: 500;"
                  @click="openEdit(p)"
                >
                  <Pencil :size="13" />
                  <span>{{ t('admin.warrantyClaims.edit') }}</span>
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="filteredClaims.length===0"><td colspan="9" class="alt-empty">{{ t('admin.warrantyClaims.empty') }}</td></tr>
        </tbody>
      </table>
      <div v-if="cTotalPages > 1" class="alt-pager"><Pagination :current-page="cCurrentPage" :total-pages="cTotalPages" @page-change="cCurrentPage = $event" /></div>
    </div>
  </div>

  <div v-if="showModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="showModal=false">
    <div class="rounded-3 p-3" style="background:var(--bg-card);width:560px;max-width:96vw;max-height:90vh;overflow-y:auto;">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <div class="fw-bold" style="color:var(--text-heading);">{{ editingId ? t('admin.warrantyClaimModal.titleEdit') : t('admin.warrantyClaimModal.titleAdd') }}</div>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="showModal=false"></button>
      </div>
      <div v-if="formError" class="alert alert-danger small py-2 mb-2">{{ formError }}</div>

      <div v-if="lockedInfo" class="p-2 mb-3 rounded-2" style="background:var(--bg-input);">
        <div v-if="lockedInfo?.tenSanPham" class="small">{{ t('admin.warrantyClaimModal.productLabel') }}: <strong>{{ lockedInfo.tenSanPham }}</strong> ({{ lockedInfo.maSku }})</div>
        <div v-else class="small">{{ t('admin.warrantyClaimModal.productLabel') }}: <strong>{{ lockedInfo?.maSku }}</strong></div>
        <div class="small">{{ t('admin.warrantyClaimModal.serialLabel') }}: <strong>{{ lockedInfo?.soSerial || '—' }}</strong></div>
        <div class="small">{{ t('admin.warrantyClaimModal.customerLabel') }}: <strong>{{ lockedInfo?.tenKhachHang }}</strong></div>
        <div class="small">{{ t('admin.warrantyClaimModal.orderLabel') }}: <strong>{{ lockedInfo?.maDonHang }}</strong></div>
      </div>
      <div v-else class="p-2 mb-3 rounded-2" style="background:var(--bg-input);">
        <div class="text-secondary small mb-2">{{ t('admin.warrantyClaimModal.manualHint') }}</div>
        <div class="row g-2">
          <div class="col-6">
            <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.orderLabel') }}</label>
            <input v-model.number="form.donHangId" type="number" min="1" class="form-control form-control-sm" style="background:var(--bg-page);color:var(--text-primary);border-color:var(--border-color-strong);" />
          </div>
          <div class="col-6">
            <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.variantLabel') }}</label>
            <input v-model.number="form.bienTheId" type="number" min="1" class="form-control form-control-sm" style="background:var(--bg-page);color:var(--text-primary);border-color:var(--border-color-strong);" />
          </div>
          <div class="col-6">
            <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.customerLabel') }}</label>
            <input v-model.number="form.khachHangId" type="number" min="1" class="form-control form-control-sm" style="background:var(--bg-page);color:var(--text-primary);border-color:var(--border-color-strong);" />
          </div>
          <div class="col-6">
            <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.chiTietLabel') }}</label>
            <input v-model.number="form.chiTietId" type="number" min="1" class="form-control form-control-sm" style="background:var(--bg-page);color:var(--text-primary);border-color:var(--border-color-strong);" />
          </div>
        </div>
      </div>

      <div class="row g-2 mb-2">
        <div class="col-6">
          <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.purchaseDateLabel') }}</label>
          <input v-model="form.ngayMua" type="datetime-local" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
        </div>
        <div class="col-6">
          <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.expiryDateLabel') }}</label>
          <input v-model="form.ngayHetBh" type="datetime-local" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
        </div>
      </div>

      <div class="mb-2">
        <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.faultLabel') }} *</label>
        <input v-model="form.moTaLoi" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>

      <div class="mb-2">
        <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.statusLabel') }}</label>
        <select v-model="form.trangThai" class="form-select form-select-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);">
          <option value="con_bao_hanh">{{ t('admin.warrantyClaimStatus.con_bao_hanh') }}</option>
          <option value="dang_xu_ly">{{ t('admin.warrantyClaimStatus.dang_xu_ly') }}</option>
          <option value="da_xu_ly">{{ t('admin.warrantyClaimStatus.da_xu_ly') }}</option>
          <option value="het_bao_hanh">{{ t('admin.warrantyClaimStatus.het_bao_hanh') }}</option>
          <option value="tu_choi">{{ t('admin.warrantyClaimStatus.tu_choi') }}</option>
        </select>
      </div>

      <div class="row g-2 mb-2">
        <div class="col-6">
          <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.receivedDateLabel') }}</label>
          <input v-model="form.ngayTiepNhan" type="datetime-local" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
        </div>
        <div class="col-6">
          <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.returnedDateLabel') }}</label>
          <input v-model="form.ngayTraKhach" type="datetime-local" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
        </div>
      </div>

      <div class="mb-2">
        <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.resultLabel') }}</label>
        <input v-model="form.ketQuaXuLy" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>

      <div class="mb-2">
        <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.costLabel') }}</label>
        <input v-model.number="form.chiPhiPhatSinh" type="number" min="0" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>

      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.noteLabel') }}</label>
        <input v-model="form.ghiChu" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>

      <div class="d-flex justify-content-end gap-2">
        <button class="btn btn-sm btn-outline-secondary" @click="showModal=false">{{ t('admin.warrantyClaimModal.cancel') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" :disabled="saving" @click="saveClaim">{{ t('admin.warrantyClaimModal.save') }}</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.usb-scanner-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}
.usb-scanner-modal {
  background: var(--bg-card, #1a1d27);
  border: 1px solid var(--border-color, #2d3142);
  border-radius: 16px;
  padding: 20px;
  width: 100%;
  max-width: 440px;
  max-height: 90vh;
  overflow-y: auto;
}
.usb-scanner-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.usb-scanner-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--text-primary, #e5e7eb);
}
.usb-status-bar {
  background: var(--bg-card-alt, #0f1117);
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 0.82rem;
  margin-bottom: 16px;
}
.usb-instructions {
  background: var(--bg-card-alt, #0f1117);
  border-radius: 10px;
  padding: 14px;
  margin-bottom: 12px;
}
.usb-step {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 10px;
  font-size: 0.82rem;
  color: var(--text-secondary, #9ca3af);
}
.usb-step:last-child { margin-bottom: 0; }
.step-num {
  background: #374151;
  color: #fff;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.7rem;
  font-weight: 700;
  flex-shrink: 0;
}
.usb-url-box {
  background: #1f2937;
  border-radius: 8px;
  padding: 8px 12px;
  margin: 4px 0 4px 30px;
}
.usb-url-box code {
  font-family: monospace;
  font-size: 0.82rem;
  color: #10b981;
}
.usb-last-scan {
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.3);
  border-radius: 8px;
  padding: 10px 14px;
  margin-bottom: 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.usb-history {
  background: var(--bg-card-alt, #0f1117);
  border-radius: 8px;
  padding: 10px;
}
.usb-history-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 120px;
  overflow-y: auto;
}
.usb-history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 6px;
  border-radius: 6px;
  background: var(--bg-card, #1a1d27);
  font-family: monospace;
  font-size: 0.78rem;
  color: #10b981;
}

/* ─── Advanced Filter Panel ─── */
.alt-btn--filter {
  display: inline-flex; align-items: center; gap: 5px; padding: 6px 12px;
  border: 1px solid var(--border-color, #e2e8f0); border-radius: 8px;
  background: var(--bg-card, #fff); color: var(--text-primary, #1e293b);
  font-size: 13px; font-weight: 500; cursor: pointer; transition: all 0.15s; flex-shrink: 0;
}
.alt-btn--filter:hover, .alt-btn--filter-active {
  border-color: var(--pink-400, #f472b6); background: var(--pink-50, #fdf2f8); color: var(--pink-700, #be185d);
}
.filter-badge {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 16px; height: 16px; padding: 0 4px; border-radius: 8px;
  background: var(--pink-600, #db2777); color: #fff; font-size: 10px; font-weight: 700;
}
.alt-btn--ghost-sm {
  display: inline-flex; align-items: center; gap: 4px; padding: 5px 10px;
  border: 1px solid var(--border-color, #e2e8f0); border-radius: 8px;
  background: transparent; color: var(--text-secondary, #64748b); font-size: 12px; cursor: pointer; transition: all 0.15s; flex-shrink: 0;
}
.alt-btn--ghost-sm:hover { background: #fee2e2; color: #dc2626; border-color: #dc2626; }
.adv-filter-panel {
  border-top: 1px solid var(--border-color, #e2e8f0); background: var(--bg-card-alt, #f8fafc);
  padding: 12px 16px; animation: slideDown 0.15s ease;
}
@keyframes slideDown { from { opacity:0; transform:translateY(-6px); } to { opacity:1; transform:translateY(0); } }
.adv-filter-row { display: flex; flex-wrap: wrap; align-items: flex-end; gap: 12px; }
.adv-filter-group { display: flex; flex-direction: column; gap: 4px; min-width: 140px; }
.adv-filter-group--range { min-width: 240px; }
.adv-filter-label { font-size: 11px; font-weight: 600; color: var(--text-secondary, #64748b); text-transform: uppercase; letter-spacing: 0.04em; }
.adv-filter-select, .adv-filter-input {
  padding: 6px 10px; border: 1px solid var(--border-color, #e2e8f0); border-radius: 7px;
  background: var(--bg-input, #fff); color: var(--text-primary, #1e293b); font-size: 13px; outline: none; transition: border-color 0.15s; width: 100%;
}
.adv-filter-select:focus, .adv-filter-input:focus { border-color: var(--pink-500, #ec4899); }
.adv-filter-range { display: flex; align-items: center; gap: 6px; }
.adv-filter-range .adv-filter-input { width: 100px; }
.adv-filter-sep { color: var(--text-secondary, #94a3b8); font-size: 13px; font-weight: 600; }
.adv-filter-reset {
  display: inline-flex; align-items: center; gap: 5px; padding: 6px 12px;
  border: 1px solid #dc2626; border-radius: 7px; background: transparent; color: #dc2626;
  font-size: 12px; font-weight: 500; cursor: pointer; align-self: flex-end; transition: all 0.15s;
}
.adv-filter-reset:hover { background: #dc2626; color: #fff; }
</style>
