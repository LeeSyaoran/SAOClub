<script setup>
import { ref, computed, onMounted, onUnmounted } from "vue";
import { t } from "../../i18n/index.js";
import * as ChiTietSanPhamService from "../../services/ChiTietSanPhamService.js";
import * as PhieuBaoHanhService from "../../services/PhieuBaoHanhService.js";
import { formatPrice, formatDate } from "../../utils/adminFormat.js";
import { nowLocalIso } from "../../utils/datetime.js";
import { showToast } from "../../stores/toast.js";
import { askConfirm } from "../../stores/confirm.js";
import { BaoHanhStore, ensureBaoHanh, refreshBaoHanh } from "../../stores/baoHanh.js";
import {
  WARRANTY_STATUS,
  STATUS_COLOR,
  canAdminReceive,
} from "../../services/warrantyConstants.js";
import { canEditWarranty } from "../../stores/warranty.js";
import Pagination from "../common/Pagination.vue";
import { usePagination } from "../../composables/usePagination.js";
<<<<<<< HEAD
import {Shield, ShieldCheck, ShieldX, ShieldAlert, Calendar, Clock, Edit, Save,
  X, Search, Plus, FileText, Users, Package,
  SendIcon, Loader2, CheckCircle2, XCircle, AlertCircle, MessageCircle,
  Image as ImageIcon, Eye, Lock,
  ShieldPlus} from '@lucide/vue';

import WarrantyClaimManagementPanel from "./WarrantyClaimManagementPanel.vue";
import * as HinhAnhBaoHanhService from "../../services/HinhAnhBaoHanhService.js";
import * as BinhLuanBaoHanhService from "../../services/BinhLuanBaoHanhService.js";

// ── Tab state ─────────────────────────────────────────────────────────────
const activeTab = ref('claims'); // 'claims' | 'list'
const showExtensionPanel = ref(false);
const selectedExtensionRequest = ref(null);

const openExtensionPanel = async (force = false) => {
  await ensureWarrantyData(force);
  selectedExtensionRequest.value = extensionRequests.value[0] ?? null;
  showExtensionPanel.value = true;
};

const closeExtensionPanel = () => {
  showExtensionPanel.value = false;
  selectedExtensionRequest.value = null;
};

const selectExtensionRequest = (req) => {
  selectedExtensionRequest.value = req;
};
=======
import {
  Calendar, Shield, ScanLine, Cpu, MemoryStick, HardDrive, Monitor,
  Tag, User, Phone, Package, ImageOff, X, Camera, AlertTriangle,
} from '@lucide/vue';
>>>>>>> 263fbf4733d7677b5a1c903b79b60a2fc9633142

onMounted(() => {
  ensureWarrantyData();
  ensureBaoHanh();
});

<<<<<<< HEAD
// ════════════════════════════════════════════════════════════════════════
// SUB-TAB 1: PHIẾU BẢO HÀNH (dùng lại WarrantyClaimManagementPanel)
// ════════════════════════════════════════════════════════════════════════

// ════════════════════════════════════════════════════════════════════════
// SUB-TAB 2: DANH SÁCH BẢO HÀNH (serial/SKU + chỉnh sửa trực tiếp + nhận yêu cầu)
// ════════════════════════════════════════════════════════════════════════

=======
onUnmounted(() => {
  closeUsbScanner();
});

// ── Bảng "Còn hạn bảo hành" — chuyển nguyên xi từ AdminPage.vue ────────────────
>>>>>>> 263fbf4733d7677b5a1c903b79b60a2fc9633142
const warrantyList = ref([]);
const warrantyLoading = ref(false);
const warrantySearch = ref('');
const statusFilter = ref('all'); // all | active | expired
const extensionRequests = ref([]); // Yêu cầu gia hạn BH từ KH

let warrantyPromise = null;
const ensureWarrantyData = (force = false) => {
  if (warrantyPromise && !force) return warrantyPromise;
  warrantyLoading.value = true;
  warrantyPromise = ChiTietSanPhamService.getUnderWarranty()
    .catch(() => [])
    .then(async (list) => {
      warrantyList.value = list;
      warrantyLoading.value = false;
      // Fetch extension requests
      try {
        extensionRequests.value = await PhieuBaoHanhService.getExtensionRequests();
      } catch (e) {
        extensionRequests.value = [];
      }
    });
  return warrantyPromise;
};

const filteredWarranty = computed(() => {
  const q = warrantySearch.value.trim().toLowerCase();
  let list = warrantyList.value;

  if (statusFilter.value === 'active') {
    list = list.filter((w) => daysUntilExpiry(w.ngayHetBh) > 0);
  } else if (statusFilter.value === 'expiring30') {
    list = list.filter((w) => {
      const d = daysUntilExpiry(w.ngayHetBh);
      return d > 0 && d <= 30;
    });
  } else if (statusFilter.value === 'expired') {
    list = list.filter((w) => daysUntilExpiry(w.ngayHetBh) <= 0);
  }

  if (!q) return list;
  return list.filter((w) =>
    [w.soSerial, w.maSku, w.tenSanPham, w.maDonHang, w.tenKhachHang, w.soDienThoaiKhachHang]
      .some((v) => (v || '').toLowerCase().includes(q))
  );
});

const setWarrantyFilter = (next) => {
  statusFilter.value = statusFilter.value === next ? 'all' : next;
};

<<<<<<< HEAD
const {
  currentPage: wCurrentPage,
  totalPages: wTotalPages,
  pagedItems: pagedWarranty,
  pageSize: wPageSize,
} = usePagination(filteredWarranty);

const daysUntilExpiry = (isoDate) => {
  if (!isoDate) return -99999;
  return Math.ceil((new Date(isoDate) - new Date()) / 86400000);
=======
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
>>>>>>> 263fbf4733d7677b5a1c903b79b60a2fc9633142
};

// ── Edit modal ───────────────────────────────────────────────────────────
const editingItem = ref(null);
const editForm = ref({
  ngayMua: '',
  ngayHetBh: '',
  ghiChu: '',
  loaiBaoHanh: '',
});
const editSaving = ref(false);

const openEdit = (item) => {
  if (!canEditWarranty(item)) {
    showToast('Không thể sửa BH khi đã có phiếu đang xử lý', 'warning');
    return;
  }
  editingItem.value = item;
  editForm.value = {
    ngayMua: item.ngayMua ? item.ngayMua.slice(0, 16) : '',
    ngayHetBh: item.ngayHetBh ? item.ngayHetBh.slice(0, 16) : '',
    ghiChu: item.ghiChu || '',
    loaiBaoHanh: item.loaiBaoHanh || 'tieu_chuan',
  };
};

const closeEdit = () => {
  editingItem.value = null;
};

const saveEdit = async () => {
  if (!editingItem.value) return;
  editSaving.value = true;
  try {
<<<<<<< HEAD
    const res = await ChiTietSanPhamService.updateWarranty(editingItem.value.chiTietId, {
      ngayMua: editForm.value.ngayMua ? nowLocalIso(new Date(editForm.value.ngayMua)) : null,
      ngayHetBh: editForm.value.ngayHetBh ? nowLocalIso(new Date(editForm.value.ngayHetBh)) : null,
      ghiChu: editForm.value.ghiChu,
      loaiBaoHanh: editForm.value.loaiBaoHanh,
    });
    if (!res.ok) throw new Error(`${res.status}`);
    showToast('Cập nhật bảo hành thành công', 'success');
    await ensureWarrantyData(true);
    closeEdit();
=======
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
>>>>>>> 263fbf4733d7677b5a1c903b79b60a2fc9633142
  } catch (e) {
    showToast(`Lỗi: ${e.message}`, 'error');
  } finally {
    editSaving.value = false;
  }
};

<<<<<<< HEAD
// ── Approve extension request ────────────────────────────────────────────
const approvingRequest = ref(null);

const approveExtension = async (req) => {
  if (!confirm(`Duyệt yêu cầu gia hạn BH #${req.baoHanhId} cho sản phẩm ${req.tenSanPham}?`)) return;
  approvingRequest.value = req.baoHanhId;
  try {
    const res = await PhieuBaoHanhService.approveExtensionRequest(req.baoHanhId, {
      approvedAt: nowLocalIso(new Date()),
    });
    if (!res.ok) throw new Error(`${res.status}`);
    showToast('Đã duyệt yêu cầu gia hạn BH', 'success');
    await ensureWarrantyData(true);
  } catch (e) {
    showToast(`Lỗi: ${e.message}`, 'error');
  } finally {
    approvingRequest.value = null;
  }
};

const rejectExtension = async (req) => {
  const reason = await askConfirm(
    'Từ chối gia hạn',
    `Từ chối yêu cầu gia hạn BH cho "${req.tenSanPham}"?`,
    { confirmText: 'Từ chối', confirmDanger: true }
  );
  if (!reason) return;
  try {
    const res = await PhieuBaoHanhService.rejectExtensionRequest(req.baoHanhId, {
      lyDoTuChoi: reason,
      rejectedAt: nowLocalIso(new Date()),
    });
    if (!res.ok) throw new Error(`${res.status}`);
    showToast('Đã từ chối', 'info');
    await ensureWarrantyData(true);
  } catch (e) {
    showToast(`Lỗi: ${e.message}`, 'error');
  }
};

const stats = computed(() => {
  const list = warrantyList.value;
  return {
    total: list.length,
    active: list.filter((w) => daysUntilExpiry(w.ngayHetBh) > 0).length,
    expiring30: list.filter((w) => {
      const d = daysUntilExpiry(w.ngayHetBh);
      return d > 0 && d <= 30;
    }).length,
    expired: list.filter((w) => daysUntilExpiry(w.ngayHetBh) <= 0).length,
    extensionPending: extensionRequests.value.filter((r) => r.trangThai === 'cho_duyet').length,
  };
});
</script>

<template>
  <div class="warranty-panel">
    <!-- Sub-tabs -->
    <div class="wp-sub-tabs">
      <div class="wp-sub-tabs-left">
        <button
          class="wp-sub-tab"
          :class="{ 'is-active': activeTab === 'claims' }"
          @click="activeTab = 'claims'"
        >
          <FileText :size="14" />
          Phiếu bảo hành
        </button>
        <button
          class="wp-sub-tab"
          :class="{ 'is-active': activeTab === 'list' }"
          @click="activeTab = 'list'"
        >
          <Package :size="14" />
          Danh sách bảo hành
        </button>
=======
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
                <span class="badge" :style="{ background: statusColor(p.trangThai).bg, color: statusColor(p.trangThai).text }">{{ t(`admin.warrantyClaimStatus.${p.trangThai}`) }}</span>
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

  <div class="alt-card mb-4">
    <div class="alt-toolbar">
      <span class="alt-toolbar__count">{{ filteredWarranty.length }} {{ t('admin.warranty.countSuffix') }}</span>
      <span class="alt-tag" style="background:var(--bg-card-alt);color:var(--text-secondary);"><Calendar :size="11" /> {{ t('admin.warranty.today') }}: {{ formatDate(new Date()) }}</span>
      <div class="alt-toolbar__actions">
        <div class="alt-search">
          <i class="fa fa-search alt-search__icon"></i>
          <input v-model="warrantySearch" :placeholder="t('admin.warranty.searchPlaceholder')" />
        </div>
>>>>>>> 263fbf4733d7677b5a1c903b79b60a2fc9633142
      </div>

    </div>

    <!-- ════ TAB: PHIẾU BẢO HÀNH ════ -->
    <div v-if="activeTab === 'claims'">
      <WarrantyClaimManagementPanel />
    </div>

    <!-- ════ TAB: DANH SÁCH BẢO HÀNH ════ -->
    <div v-else-if="activeTab === 'list'">
      <div class="wp-list-status-cards">
        <div class="wp-card-classification wp-card-classification--total" :class="{ 'is-on': statusFilter === 'all' }" @click="setWarrantyFilter('all')">
          <div class="wp-card-classification__icon"><Package :size="16" /></div>
          <div class="wp-card-classification__body">
            <div class="wp-card-classification__label">Tổng SKU</div>
            <div class="wp-card-classification__value">{{ stats.total }}</div>
          </div>
        </div>
        <div class="wp-card-classification wp-card-classification--active" :class="{ 'is-on': statusFilter === 'active' }" @click="setWarrantyFilter('active')">
          <div class="wp-card-classification__icon"><ShieldCheck :size="16" /></div>
          <div class="wp-card-classification__body">
            <div class="wp-card-classification__label">Còn bảo hành</div>
            <div class="wp-card-classification__value">{{ stats.active }}</div>
          </div>
        </div>
        <div class="wp-card-classification wp-card-classification--warning" :class="{ 'is-on': statusFilter === 'expiring30' }" @click="setWarrantyFilter('expiring30')">
          <div class="wp-card-classification__icon"><Clock :size="16" /></div>
          <div class="wp-card-classification__body">
            <div class="wp-card-classification__label">Sắp hết hạn</div>
            <div class="wp-card-classification__value">{{ stats.expiring30 }}</div>
          </div>
        </div>
        <div class="wp-card-classification wp-card-classification--danger" :class="{ 'is-on': statusFilter === 'expired' }" @click="setWarrantyFilter('expired')">
          <div class="wp-card-classification__icon"><ShieldX :size="16" /></div>
          <div class="wp-card-classification__body">
            <div class="wp-card-classification__label">Hết hạn</div>
            <div class="wp-card-classification__value">{{ stats.expired }}</div>
          </div>
        </div>
        <button class="alt-btn alt-btn--ghost" style="padding:4px 12px;" @click="openCreateManual">
          <Shield :size="12" style="vertical-align:-2px;" /> {{ t('admin.warrantyClaims.createManual') }}
        </button>
      </div>


      <!-- Extension requests modal -->
      <Transition name="modal-fade">
        <div v-if="showExtensionPanel" class="wp-modal-backdrop" @click.self="closeExtensionPanel">
          <div class="wp-modal wp-extension-modal">
            <div class="wp-modal-header">
              <div class="wp-modal-title">
                <Calendar :size="20" />
                <div>
                  <div class="wp-modal-title-main">Gia hạn bảo hành</div>
                  <div class="wp-modal-title-sub">Yêu cầu của khách hàng</div>
                </div>
              </div>
              <button class="wp-modal-close" @click="closeExtensionPanel">
                <X :size="18" />
              </button>
            </div>

            <div class="wp-modal-body wp-extension-modal-body">
              <div class="wp-extension-modal-grid">
                <aside class="wp-extension-request-list">
                  <div class="wp-extension-request-head">
                    <span class="wp-section-title">Danh sách yêu cầu</span>
                    <span class="wp-count-pill">{{ extensionRequests.length }}</span>
                  </div>

                  <div v-if="extensionRequests.length === 0" class="wp-empty-small">
                    <Package :size="30" />
                    <span>Chưa có yêu cầu</span>
                  </div>

                  <div v-for="req in extensionRequests" :key="req.baoHanhId" class="wp-extension-request-row"
                    :class="{ 'is-active': selectedExtensionRequest && selectedExtensionRequest.baoHanhId === req.baoHanhId, 'is-pending': req.trangThai === 'cho_duyet' }"
                    @click="selectExtensionRequest(req)">
                    <div class="wp-extension-request-row-top">
                      <span class="wp-extension-request-name">{{ req.tenSanPham }}</span>
                      <span class="wp-extension-request-status">{{ req.trangThai === 'cho_duyet' ? 'Chờ duyệt' : req.trangThai }}</span>
                    </div>
                    <div class="wp-extension-request-meta">
                      <span>{{ req.tenKhachHang }}</span>
                      <span>Đơn #{{ req.donHangId }}</span>
                    </div>
                    <div class="wp-extension-request-meta">
                      <span>{{ req.tenGoi }}</span>
                      <span>{{ formatPrice(req.donGia) }}</span>
                    </div>
                  </div>
                </aside>

                <section v-if="selectedExtensionRequest" class="wp-extension-detail-panel">
                  <div class="wp-extension-detail-head">
                    <div>
                      <span class="wp-label-mini">Chi tiết yêu cầu</span>
                      <h4>{{ selectedExtensionRequest.tenSanPham }}</h4>
                    </div>
                    <span class="wp-detail-status" :class="`wp-detail-status--${selectedExtensionRequest.trangThai}`">
                      {{ selectedExtensionRequest.trangThai === 'cho_duyet' ? 'Chờ duyệt' : selectedExtensionRequest.trangThai === 'da_duyet' ? 'Đã duyệt' : 'Đã từ chối' }}
                    </span>
                  </div>

                  <div class="wp-extension-detail-body">
                    <div class="wp-detail-grid">
                      <div>
                        <span class="wp-detail-label">Khách hàng</span>
                        <span class="wp-detail-value">{{ selectedExtensionRequest.tenKhachHang }}</span>
                      </div>
                      <div>
                        <span class="wp-detail-label">Đơn hàng</span>
                        <span class="wp-detail-value">#{{ selectedExtensionRequest.donHangId }}</span>
                      </div>
                      <div>
                        <span class="wp-detail-label">SKU</span>
                        <span class="wp-detail-value"><code>{{ selectedExtensionRequest.maSku }}</code></span>
                      </div>
                      <div>
                        <span class="wp-detail-label">Serial</span>
                        <span class="wp-detail-value"><code>{{ selectedExtensionRequest.soSerial }}</code></span>
                      </div>
                      <div>
                        <span class="wp-detail-label">Gói</span>
                        <span class="wp-detail-value">{{ selectedExtensionRequest.tenGoi }}</span>
                      </div>
                      <div>
                        <span class="wp-detail-label">Thời hạn</span>
                        <span class="wp-detail-value">{{ selectedExtensionRequest.duration }} tháng</span>
                      </div>
                      <div>
                        <span class="wp-detail-label">Ngày gửi</span>
                        <span class="wp-detail-value">{{ formatDate(selectedExtensionRequest.ngayGui) }}</span>
                      </div>
                      <div>
                        <span class="wp-detail-label">Đơn giá</span>
                        <span class="wp-detail-value">{{ formatPrice(selectedExtensionRequest.donGia) }}</span>
                      </div>
                    </div>
                  </div>

                  <div class="wp-extension-detail-actions">
                    <button class="wp-btn-approve" @click="approveExtension(selectedExtensionRequest)" :disabled="approvingRequest === selectedExtensionRequest.baoHanhId">
                      <Loader2 v-if="approvingRequest === selectedExtensionRequest.baoHanhId" :size="13" class="spin" />
                      <CheckCircle2 v-else :size="13" />
                      Duyệt
                    </button>
                    <button class="wp-btn-reject" @click="rejectExtension(selectedExtensionRequest)">
                      <XCircle :size="13" />
                      Từ chối
                    </button>
                  </div>
                </section>
              </div>
            </div>
          </div>
        </div>
      </Transition>

      <!-- Filter toolbar / list container -->
      <section class="wp-toolbar">
        <div class="wp-toolbar__left">
          <div class="wp-search-wrap">
            <Search :size="13" class="wp-search-icon" />
            <input
              v-model="warrantySearch"
              type="search"
              class="wp-search-input"
              placeholder="Tìm SKU, serial, tên KH..."
            />
          </div>
        </div>

        <div v-if="warrantyLoading && warrantyList.length === 0" class="wp-loading">
          <div v-for="i in 5" :key="i" class="wp-skel-row"></div>
        </div>

        <div v-else-if="filteredWarranty.length === 0" class="wp-empty">
          <Package :size="42" class="wp-empty-icon" />
          <div class="wp-empty-title">Không có sản phẩm bảo hành nào</div>
          <div class="wp-empty-text">
            {{ statusFilter === 'all' ? 'Chưa có dữ liệu bảo hành.' : 'Không có sản phẩm nào ở trạng thái này.' }}
          </div>
        </div>

        <div v-else class="wp-table-wrap">
        <table class="wp-table">
          <thead>
            <tr>
              <th>Mã SKU</th>
              <th>Mã biến thể</th>
              <th>Sản phẩm</th>
              <th>Serial</th>
              <th>Khách hàng</th>
              <th>Ngày mua</th>
              <th>Hết hạn</th>
              <th>Còn lại</th>
              <th>Trạng thái</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="w in pagedWarranty" :key="w.chiTietId"
              class="wp-row"
              :class="{ 'wp-row--expired': daysUntilExpiry(w.ngayHetBh) <= 0 }"
              @click="openEdit(w)"
            >
              <td><code class="wp-code">{{ w.maSku || '—' }}</code></td>
              <td><code class="wp-code wp-code--variant">#{{ w.bienTheId }}</code></td>
              <td class="wp-product-cell">{{ w.tenSanPham || '—' }}</td>
              <td><code class="wp-code wp-code--serial">{{ w.soSerial || '—' }}</code></td>
              <td>{{ w.tenKhachHang || `KH#${w.khachHangId}` }}</td>
              <td class="wp-muted">{{ formatDate(w.ngayMua) }}</td>
              <td class="wp-muted">{{ formatDate(w.ngayHetBh) }}</td>
              <td>
                <span
                  class="wp-days-pill"
                  :class="{
                    'wp-days-pill--ok': daysUntilExpiry(w.ngayHetBh) > 90,
                    'wp-days-pill--warn': daysUntilExpiry(w.ngayHetBh) > 0 && daysUntilExpiry(w.ngayHetBh) <= 90,
                    'wp-days-pill--danger': daysUntilExpiry(w.ngayHetBh) <= 0,
                  }"
                >
                  <Clock :size="11" />
                  {{ daysUntilExpiry(w.ngayHetBh) > 0 ? daysUntilExpiry(w.ngayHetBh) + ' ngày' : 'Đã hết' }}
                </span>
              </td>
              <td>
                <span
                  v-if="daysUntilExpiry(w.ngayHetBh) > 0"
                  class="wp-status wp-status--active"
                >
                  <ShieldCheck :size="11" /> Còn bảo hành
                </span>
                <span v-else class="wp-status wp-status--expired">
                  <ShieldX :size="11" /> Hết bảo hành
                </span>
              </td>
            </tr>
          </tbody>
        </table>

        <Pagination
          v-if="filteredWarranty.length > wPageSize"
          :current-page="wCurrentPage"
          :total-pages="wTotalPages"
          :total="filteredWarranty.length"
          :page-size="wPageSize"
          @page-change="wCurrentPage = $event"
        />
      </div>
    </section>
    </div>

    <!-- ════ EDIT MODAL ════ -->
    <Transition name="modal-fade">
      <div v-if="editingItem" class="wp-modal-backdrop" @click.self="closeEdit">
        <div class="wp-modal">
          <div class="wp-modal-header">
            <div class="wp-modal-title">
              <Edit :size="20" />
              <div>
                <div class="wp-modal-title-main">Chỉnh sửa bảo hành</div>
                <div class="wp-modal-title-sub">{{ editingItem.tenSanPham }} · {{ editingItem.soSerial }}</div>
              </div>
            </div>
            <button class="wp-modal-close" @click="closeEdit">
              <X :size="18" />
            </button>
          </div>

<<<<<<< HEAD
          <div class="wp-modal-body">
            <div class="wp-form-grid">
              <div class="wp-form-group">
                <label class="wp-form-label">Ngày mua</label>
                <input v-model="editForm.ngayMua" type="datetime-local" class="wp-form-input" />
              </div>
              <div class="wp-form-group">
                <label class="wp-form-label">Ngày hết hạn BH <span class="wp-required">*</span></label>
                <input v-model="editForm.ngayHetBh" type="datetime-local" class="wp-form-input" />
              </div>
              <div class="wp-form-group wp-form-group--full">
                <label class="wp-form-label">Loại bảo hành</label>
                <select v-model="editForm.loaiBaoHanh" class="wp-form-input">
                  <option value="tieu_chuan">Tiêu chuẩn (12 tháng)</option>
                  <option value="mo_rong_12">Mở rộng 12 tháng (gói trả phí)</option>
                  <option value="mo_rong_24">Mở rộng 24 tháng (gói trả phí)</option>
                  <option value="vip_36">VIP 36 tháng (gói trả phí)</option>
                </select>
              </div>
              <div class="wp-form-group wp-form-group--full">
                <label class="wp-form-label">Ghi chú nội bộ</label>
                <textarea
                  v-model="editForm.ghiChu"
                  rows="3"
                  class="wp-form-input wp-form-textarea"
                  placeholder="Vd: SP thay thế từ bảo hành lần 1, BH tiếp tục tính..."
                />
              </div>
            </div>
=======
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
>>>>>>> 263fbf4733d7677b5a1c903b79b60a2fc9633142

            <div v-if="editingItem.daysExtensionRemaining" class="wp-extension-info">
              <ShieldPlus :size="14" />
              Sản phẩm đang có gia hạn BH +{{ editingItem.daysExtensionRemaining }} ngày
            </div>
          </div>

          <div class="wp-modal-footer">
            <button class="wp-btn-ghost" @click="closeEdit" :disabled="editSaving">Hủy</button>
            <button class="wp-btn-primary" @click="saveEdit" :disabled="editSaving">
              <Loader2 v-if="editSaving" :size="14" class="spin" />
              <Save v-else :size="14" />
              {{ editSaving ? 'Đang lưu...' : 'Lưu thay đổi' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
<<<<<<< HEAD
.warranty-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
  background: transparent;
  border: none;
  border-radius: 0;
  padding: 0;
  color: var(--text-primary);
  box-shadow: none;
  overflow: visible;
}

.wp-page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 6px 2px 12px;
  border-bottom: 1px solid var(--border-color-soft, var(--border));
}

.wp-page-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.wp-page-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  background: var(--accent-soft, var(--pink-50));
  color: var(--accent-fg, var(--pink-600));
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border-color-soft, var(--border));
}

.wp-page-title-main {
  font-size: 1rem;
  font-weight: 800;
  color: var(--text-heading, var(--text-primary));
}

.wp-page-title-sub {
  font-size: 11.5px;
  color: var(--text-muted);
  margin-top: 2px;
}

.wp-page-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.wp-refresh-btn {
  width: auto;
  height: auto;
  padding: 7px 14px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11.5px;
  font-weight: 700;
}

/* Sub-tabs */
.wp-sub-tabs {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0;
  background: transparent;
  border-radius: 0;
  border: none;
  box-shadow: none;
}
.wp-sub-tabs-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.wp-sub-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: 999px;
  border: 1px solid var(--pink-200, #fbcfe8);
  background: linear-gradient(180deg, #fff 0%, var(--pink-50, #fff1f7) 100%);
  color: var(--pink-700, #be185d);
  font-size: 13px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s ease;
  box-shadow: 0 2px 0 var(--pink-300, #f9a8d4), 0 4px 10px rgba(168, 27, 93, 0.14);
}
.wp-sub-tab:hover:not(:disabled) {
  background: linear-gradient(180deg, var(--pink-50, #fff1f7) 0%, var(--white, #fff) 100%);
  border-color: var(--pink-300, #f9a8d4);
  box-shadow: 0 3px 0 var(--pink-400, #f9a8d4), 0 6px 14px rgba(168, 27, 93, 0.20);
  transform: translateY(-1px);
}
.wp-sub-tab.is-active {
  background: linear-gradient(180deg, var(--pink-200, #fbcfe8) 0%, var(--pink-100, #fce7f3) 100%);
  color: var(--pink-700, #be185d);
  border-color: var(--pink-400, #f9a8d4);
  box-shadow: 0 3px 0 var(--pink-700, #be185d), 0 8px 16px rgba(168, 27, 93, 0.30);
  transform: translateY(-1px);
}
.wp-sub-tab.is-active:hover {
  background: linear-gradient(180deg, var(--pink-300, #f9a8d4) 0%, var(--pink-100, #fce7f3) 100%);
  box-shadow: 0 4px 0 var(--pink-700, #be185d), 0 10px 18px rgba(168, 27, 93, 0.34);
}
.wp-sub-tab:active:not(:disabled) {
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.16), 0 1px 0 var(--pink-700, #be185d);
  transform: translateY(2px);
}
.wp-sub-tab--extension {
  margin-left: auto;
  background: #fff;
  color: var(--pink-700, #be185d);
  border-color: var(--pink-200, #fbcfe8);
}
.wp-sub-tab--extension:hover:not(:disabled) {
  background: var(--pink-50, #fff1f7);
  border-color: var(--pink-300, #f9a8d4);
  color: var(--pink-700, #be185d);
}
.wp-tab-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 7px;
  background: var(--warning-bg, #fef3c7);
  color: var(--warning-text, #92400e);
  border-radius: 9999px;
  font-size: 10px;
  font-weight: 700;
  margin-left: 2px;
}

/* Extension banner */
.wp-extension-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border: 1px solid #f59e0b;
  border-radius: 10px;
  color: #92400e;
}
.wp-extension-icon {
  width: 40px;
  height: 40px;
  background: rgba(245, 158, 11, 0.2);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #b45309;
}
.wp-extension-banner > div:nth-child(2) { flex: 1; }
.wp-extension-text { font-size: 11.5px; opacity: 0.85; margin-top: 2px; }
.wp-extension-btn {
  background: #92400e;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 7px 14px;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
}

/* List layout heading */
.wp-list-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 2px 2px 12px;
  border-bottom: 1px solid var(--pink-200, #fbcfe8);
}
.wp-list-title {
  font-size: 1rem;
  font-weight: 800;
  color: var(--text-heading, var(--text-primary));
}
.wp-list-subtitle {
  font-size: 11.5px;
  color: var(--text-muted);
  margin-top: 3px;
}
.wp-refresh-btn {
  border: 1px solid var(--pink-200, #fbcfe8);
  background: var(--pink-50, #fff1f7);
  color: var(--pink-700, #be185d);
  border-radius: 10px;
  padding: 8px 14px;
  font-size: 12px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}
.wp-list-status-cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(150px, 1fr));
  gap: 12px;
  margin: 14px 0 12px;
}
.wp-card-classification {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 96px;
  color: #fff;
  border-radius: 14px;
  padding: 16px 18px;
  border: 2px solid transparent;
  box-shadow: 0 4px 14px rgba(0, 0, 0, .12);
  cursor: pointer;
  user-select: none;
  transition: transform .12s, box-shadow .12s, border-color .12s, outline-color .12s;
}
.wp-card-classification:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(0, 0, 0, .18);
}
.wp-card-classification.is-on {
  border-color: rgba(255,255,255,.85);
}
.wp-card-classification__icon {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255,255,255,.22);
  color: #fff;
  flex-shrink: 0;
}
.wp-card-classification__label {
  font-size: 12.5px;
  color: rgba(255,255,255,.85);
  margin-bottom: 2px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}
.wp-card-classification__value {
  font-size: 1.6rem;
  font-weight: 800;
  color: #fff;
}
.wp-card-classification--total { background: linear-gradient(135deg, #60a5fa, #2563eb); }
.wp-card-classification--active { background: linear-gradient(135deg, #34d399, #059669); }
.wp-card-classification--warning { background: linear-gradient(135deg, #fbbf24, #d97706); }
.wp-card-classification--danger { background: linear-gradient(135deg, #f87171, #dc2626); }

.wp-card-classification--total:hover { box-shadow: 0 6px 18px rgba(37,99,235,.24); }
.wp-card-classification--active:hover { box-shadow: 0 6px 18px rgba(5,150,105,.24); }
.wp-card-classification--warning:hover { box-shadow: 0 6px 18px rgba(217,119,6,.24); }
.wp-card-classification--danger:hover { box-shadow: 0 6px 18px rgba(220,38,38,.24); }

/* Stats */
.wp-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(150px, 1fr));
  gap: 10px;
}

.wp-section-title-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.wp-section-title-wrap .wp-section-title {
  margin: 0;
}

.wp-section-title-wrap::after {
  content: '';
  height: 1px;
  flex: 1;
  background: var(--border-color-soft, var(--border));
  margin-left: 12px;
}
.wp-stat {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 12px 14px;
  display: flex;
  gap: 10px;
  align-items: center;
}
.wp-stat-icon {
  width: 36px;
  height: 36px;
  background: var(--gray-100);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
}
.wp-stat--success .wp-stat-icon { background: rgba(34, 197, 94, 0.15); color: #16a34a; }
.wp-stat--warning .wp-stat-icon { background: rgba(245, 158, 11, 0.15); color: #d97706; }
.wp-stat--danger .wp-stat-icon { background: rgba(239, 68, 68, 0.15); color: #dc2626; }
.wp-stat-value { font-weight: 800; font-size: 1.1rem; line-height: 1; }
.wp-stat-label { font-size: 11px; color: var(--text-secondary); margin-top: 3px; }

/* Extension section */
.wp-extension-section {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px 16px;
}
.wp-section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13.5px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 12px;
}
.wp-section-title :first-child { color: #d97706; }
.wp-count-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  background: var(--gray-100);
  color: var(--text-secondary);
  border-radius: 9999px;
  font-size: 10.5px;
  font-weight: 700;
  margin-left: auto;
}

.wp-extension-list { display: flex; flex-direction: column; gap: 8px; }
.wp-extension-card {
  display: grid;
  grid-template-columns: 1.5fr 1fr auto;
  gap: 14px;
  align-items: center;
  padding: 12px 14px;
  background: var(--gray-50);
  border: 1px solid var(--border);
  border-radius: 10px;
}
.wp-extension-card--pending { background: linear-gradient(135deg, #fef9c3 0%, #fef3c7 100%); border-color: #fde047; }
.wp-extension-product { display: flex; gap: 10px; align-items: center; }
.wp-extension-thumb {
  width: 50px;
  height: 50px;
  background: var(--bg-card);
  border-radius: 8px;
  padding: 8px;
  color: var(--text-muted);
  flex-shrink: 0;
}
.wp-extension-name { font-weight: 700; font-size: 13px; color: var(--text-primary); }
.wp-extension-meta, .wp-extension-customer {
  font-size: 11px;
  color: var(--text-secondary);
  margin-top: 3px;
}
.wp-extension-meta code {
  background: var(--bg-card);
  padding: 1px 4px;
  border-radius: 4px;
  font-family: 'SF Mono', monospace;
  font-size: 10.5px;
}
.wp-extension-customer { display: inline-flex; align-items: center; gap: 4px; }

.wp-extension-plan {
  background: var(--bg-card);
  border-radius: 8px;
  padding: 10px 12px;
}
.wp-extension-plan-label {
  font-size: 10.5px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  font-weight: 700;
}
.wp-extension-plan-name { font-weight: 700; font-size: 13px; color: var(--text-primary); margin-top: 4px; }
.wp-extension-plan-detail { font-size: 11.5px; color: var(--text-secondary); margin-top: 2px; }
.wp-extension-date {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 10.5px;
  color: var(--text-muted);
  margin-top: 4px;
}

.wp-extension-actions { display: flex; flex-direction: column; gap: 4px; }
.wp-btn-approve, .wp-btn-reject {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 7px 14px;
  border: none;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}
.wp-btn-approve { background: var(--success, #16a34a); color: white; }
.wp-btn-approve:hover:not(:disabled) { filter: brightness(1.1); }
.wp-btn-approve:disabled { opacity: 0.5; cursor: not-allowed; }
.wp-btn-reject { background: var(--gray-100); color: var(--text-secondary); border: 1px solid var(--border); }
.wp-btn-reject:hover { background: var(--gray-200); }

.wp-extension-decided {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}
.wp-decided-pill {
  display: inline-flex;
  padding: 3px 10px;
  border-radius: 9999px;
  font-size: 11px;
  font-weight: 700;
}
.wp-decided-pill--da_duyet { background: var(--success-light, #d1fae5); color: #059669; }
.wp-decided-pill--tu_choi { background: var(--gray-100); color: #6b7280; }

/* Toolbar */
.wp-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  background: var(--bg-card, #fff);
  border: 1px solid var(--line, var(--border));
  border-radius: 14px;
  padding: 12px 16px;
  margin-bottom: 12px;
  box-shadow: var(--sh-2, 0 8px 18px rgba(0,0,0,0.06));
}

.wp-toolbar__left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1 1 240px;
  min-width: 200px;
  max-width: 340px;
}
.wp-toolbar__right {
  display: none;
}
.wp-search-wrap {
  position: relative;
  flex: 1 1 240px;
  min-width: 200px;
  max-width: 340px;
}
.wp-search-wrap .wp-search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--pink-500, #ec4899);
  pointer-events: none;
}
.wp-search-wrap .wp-search-input {
  width: 100%;
  padding: 8px 14px 8px 34px;
  border: 1px solid var(--pink-200, #fbcfe8);
  border-radius: 999px;
  font-size: 13px;
  background: var(--pink-50, #fff1f7);
  font-family: inherit;
  color: var(--text-primary, var(--ink));
}
.wp-search-wrap .wp-search-input:focus {
  outline: none;
  border-color: var(--pink-500, #ec4899);
  background: #fff;
  box-shadow: 0 0 0 3px var(--pink-100, #fce7f3);
}
.wp-icon-btn {
  display: none;
}

/* Table */
.wp-table-wrap {
  background: var(--bg-card, #fff);
  border: 0;
  border-radius: 0;
  overflow: visible;
  box-shadow: none;
}
.wp-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  font-size: 12.5px;
}
.wp-table thead { background: var(--gray-50, #f8fafc); }
.wp-table th {
  text-align: left;
  padding: 12px 14px;
  font-size: 11px;
  font-weight: 800;
  color: var(--text-secondary, #475569);
  text-transform: uppercase;
  letter-spacing: 0.035em;
  border-bottom: 1px solid transparent;
  white-space: nowrap;
}
.wp-row {
  border-bottom: 1px solid transparent;
  transition: background 0.2s ease, color 0.2s ease, transform 0.2s ease;
  cursor: pointer;
}
.wp-row:hover { background: var(--bg-hover, #fdf2f8); color: var(--text-primary, #1f2937); }
.wp-row--expired { background: rgba(239, 68, 68, 0.04); }
.wp-row td {
  padding: 12px 14px;
  vertical-align: middle;
  color: var(--text-primary, #1f2937);
  font-size: 12.5px;
  border-bottom: 1px solid transparent;
  white-space: nowrap;
}
.wp-table tbody tr:last-child td { border-bottom: none; }
.wp-table th:nth-child(1), .wp-table td:nth-child(1) { width: 11%; }
.wp-table th:nth-child(2), .wp-table td:nth-child(2) { width: 9%; }
.wp-table th:nth-child(3), .wp-table td:nth-child(3) { width: 20%; }
.wp-table th:nth-child(4), .wp-table td:nth-child(4) { width: 13%; }
.wp-table th:nth-child(5), .wp-table td:nth-child(5) { width: 14%; }
.wp-table th:nth-child(6), .wp-table td:nth-child(6) { width: 11%; }
.wp-table th:nth-child(7), .wp-table td:nth-child(7) { width: 11%; }
.wp-table th:nth-child(8), .wp-table td:nth-child(8) { width: 9%; }
.wp-table th:nth-child(9), .wp-table td:nth-child(9) { width: 12%; }
.wp-code {
  background: var(--gray-100);
  padding: 1px 6px;
  border-radius: 4px;
  font-family: 'SF Mono', monospace;
  font-size: 11px;
  font-weight: 600;
}
.wp-code--serial { color: var(--pink-600); }
.wp-code--variant { color: var(--info, #2563eb); background: rgba(37, 99, 235, 0.08); }
.wp-product-cell {
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 600;
}
.wp-muted { color: var(--text-muted); font-size: 12px; }

.wp-days-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border-radius: 9999px;
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
}
.wp-days-pill--ok { background: rgba(34, 197, 94, 0.15); color: #16a34a; }
.wp-days-pill--warn { background: rgba(245, 158, 11, 0.15); color: #d97706; }
.wp-days-pill--danger { background: rgba(239, 68, 68, 0.15); color: #dc2626; }

.wp-status {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 9999px;
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
}
.wp-status--active { background: rgba(34, 197, 94, 0.12); color: #15803d; }
.wp-status--expired { background: rgba(239, 68, 68, 0.12); color: #b91c1c; }

.wp-edit-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 6px;
  color: var(--text-secondary);
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}
.wp-edit-btn:hover:not(:disabled) {
  background: var(--pink-50);
  border-color: var(--pink-500);
  color: var(--pink-600);
}
.wp-edit-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* Loading & Empty */
.wp-loading { display: flex; flex-direction: column; gap: 6px; }
.wp-skel-row {
  background: linear-gradient(90deg, var(--gray-100) 0%, var(--gray-200) 50%, var(--gray-100) 100%);
  height: 48px;
  border-radius: 8px;
}
.wp-empty {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 40px 20px;
  text-align: center;
}
.wp-empty-icon { color: var(--text-muted); margin-bottom: 10px; }
.wp-empty-title { font-weight: 700; font-size: 0.95rem; color: var(--text-primary); }
.wp-empty-text { font-size: 12px; color: var(--text-secondary); margin-top: 6px; }

/* Edit modal */
.wp-modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(31, 41, 55, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1100;
  padding: 20px;
  backdrop-filter: blur(4px);
}
.wp-modal {
  background: var(--bg-card);
  border-radius: 14px;
  width: 100%;
  max-width: 540px;
  max-height: 92vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.wp-modal-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--gray-50);
}
.wp-modal-title { display: flex; gap: 10px; align-items: center; color: var(--pink-500); }
.wp-modal-title-main { font-weight: 800; font-size: 1rem; color: var(--text-primary); }
.wp-modal-title-sub {
  font-size: 12.5px;
  color: var(--text-secondary);
  margin-top: 2px;
  font-weight: 500;
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.wp-modal-close {
  width: 32px;
  height: 32px;
  background: transparent;
  border: none;
  border-radius: 8px;
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.wp-modal-close:hover { background: var(--gray-100); }

.wp-modal-body { padding: 18px 20px; overflow-y: auto; }

.wp-form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.wp-form-group--full { grid-column: 1 / -1; }
.wp-form-label {
  font-size: 11px;
  font-weight: 700;
  color: var(--text-secondary);
  display: block;
  margin-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.wp-required { color: var(--danger); }
.wp-form-input {
  width: 100%;
  padding: 8px 10px;
  background: var(--bg-input);
  border: 1px solid var(--border);
  border-radius: 6px;
  color: var(--text-primary);
  font-size: 12.5px;
  font-family: inherit;
  outline: none;
  transition: all 0.2s ease;
}
.wp-form-input:focus { border-color: var(--pink-500); }
.wp-form-textarea {
  resize: vertical;
  line-height: 1.5;
}

.wp-extension-info {
  margin-top: 12px;
  background: var(--gray-50);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 12px;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
}
.wp-extension-info :first-child { color: var(--pink-500); }

.wp-modal-footer {
  padding: 14px 20px;
  border-top: 1px solid var(--border);
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  background: var(--gray-50);
}
.wp-btn-primary, .wp-btn-ghost {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 8px 18px;
  border: none;
  border-radius: 9999px;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}
.wp-btn-primary {
  background: var(--pink-500);
  color: white;
  box-shadow: 0 3px 0 var(--pink-700);
}
.wp-btn-primary:hover:not(:disabled) { background: var(--pink-600); }
.wp-btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.wp-btn-ghost {
  background: var(--bg-card);
  color: var(--text-secondary);
  border: 1px solid var(--border);
}
.wp-btn-ghost:hover:not(:disabled) { background: var(--gray-100); }

.spin { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 0.2s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }

@media (max-width: 767.98px) {
  .wp-stats { grid-template-columns: repeat(2, 1fr); }
  .wp-extension-card { grid-template-columns: 1fr; }
  .wp-form-grid { grid-template-columns: 1fr; }
=======
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
>>>>>>> 263fbf4733d7677b5a1c903b79b60a2fc9633142
}
</style>
