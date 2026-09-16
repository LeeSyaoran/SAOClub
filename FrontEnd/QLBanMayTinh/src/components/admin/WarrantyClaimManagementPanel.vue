<script setup>
import { ref, computed, onMounted, watch, reactive } from "vue";
import { Filter, X, ChevronDown, ChevronUp } from '@lucide/vue';
import * as PhieuBaoHanhService from "../../services/PhieuBaoHanhService.js";
import * as HinhAnhBaoHanhService from "../../services/HinhAnhBaoHanhService.js";
import * as BinhLuanBaoHanhService from "../../services/BinhLuanBaoHanhService.js";
import {
  WARRANTY_STATUS,
  STATUS_COLOR,
  canAdminReceive,
} from "../../services/warrantyConstants.js";
import { BaoHanhStore, ensureBaoHanh, refreshBaoHanh } from "../../stores/baoHanh.js";
import { formatPrice, formatDate } from "../../utils/adminFormat.js";
import { nowLocalIso } from "../../utils/datetime.js";
import { showToast } from "../../stores/toast.js";
import { askConfirm } from "../../stores/confirm.js";
import Skeleton from "../common/Skeleton.vue";
import {
  Shield, ShieldCheck, ShieldX, ShieldAlert, Clock, Send, XCircle,
  CheckCircle2, Loader2, AlertCircle, RefreshCw, Filter, ChevronRight,
  Wrench, Image as ImageIcon, MessageCircle, Calendar, Package, User,
  Phone, AlertTriangle, Trash2, Send as SendIcon, Save, Ban,
  Truck, MapPin, Search,
} from '@lucide/vue';

// ── Filters ───────────────────────────────────────────────────────────
const statusFilter = ref('all'); // all | pending | processing | done | rejected
const searchQuery = ref('');
const isAdvFilterOpen = ref(false);
const advFilters = reactive({
  ngayFrom: '',
  ngayTo: '',
  onlyOverdue: false,
});

const advFilterCount = computed(() => [
  advFilters.ngayFrom, advFilters.ngayTo, advFilters.onlyOverdue ? '1' : ''
].filter((v) => v !== '').length);

const resetAdvFilters = () => {
  advFilters.ngayFrom = '';
  advFilters.ngayTo = '';
  advFilters.onlyOverdue = false;
  searchQuery.value = '';
};

const filteredClaims = computed(() => {
  const list = BaoHanhStore.items || [];
  let filtered = list;

  if (statusFilter.value === 'pending')
    filtered = filtered.filter((c) => c.trangThai === WARRANTY_STATUS.CHO_XU_LY);
  else if (statusFilter.value === 'processing')
    filtered = filtered.filter((c) => c.trangThai === WARRANTY_STATUS.DANG_XU_LY);
  else if (statusFilter.value === 'done')
    filtered = filtered.filter((c) => c.trangThai === WARRANTY_STATUS.DA_XU_LY);
  else if (statusFilter.value === 'rejected')
    filtered = filtered.filter((c) => c.trangThai === WARRANTY_STATUS.TU_CHOI || c.trangThai === 'da_huy');

  const q = searchQuery.value.trim().toLowerCase();
  if (q) {
    filtered = filtered.filter((c) => {
      const name = (c.tenKhachHang || '').toLowerCase();
      return String(c.baoHanhId || '').includes(q)
        || name.includes(q)
        || (c.soSerial || '').toLowerCase().includes(q)
        || (c.maSku || '').toLowerCase().includes(q)
        || String(c.bienTheId || '').includes(q);
    });
  }
  // ngày tiếp nhận từ-đến
  if (advFilters.ngayFrom) filtered = filtered.filter((c) => (c.ngayTiepNhan || '').slice(0, 10) >= advFilters.ngayFrom);
  if (advFilters.ngayTo)   filtered = filtered.filter((c) => (c.ngayTiepNhan || '').slice(0, 10) <= advFilters.ngayTo);
  // quick-filter quá hạn
  if (advFilters.onlyOverdue) filtered = filtered.filter((c) => isOverdue(c));
  return filtered;
});

const stats = computed(() => {
  const list = BaoHanhStore.items || [];
  return {
    total: list.length,
    pending: list.filter((c) => c.trangThai === WARRANTY_STATUS.CHO_XU_LY).length,
    processing: list.filter((c) => c.trangThai === WARRANTY_STATUS.DANG_XU_LY).length,
    done: list.filter((c) => c.trangThai === WARRANTY_STATUS.DA_XU_LY).length,
    rejected: list.filter((c) => c.trangThai === WARRANTY_STATUS.TU_CHOI || c.trangThai === 'da_huy').length,
  };
});

// ── Helpers ─────────────────────────────────────────────────────────────────
const statusLabel = (s) => {
  const map = {
    cho_xu_ly: 'Chờ tiếp nhận',
    dang_xu_ly: 'Đang xử lý',
    da_xu_ly: 'Hoàn thành',
    tu_choi: 'Từ chối',
    da_huy: 'Đã hủy (KH)',
  };
  return map[s] || s;
};

const statusColor = (s) => STATUS_COLOR[s] || STATUS_COLOR.cho_xu_ly;

const statusIcon = (s) => {
  const map = {
    cho_xu_ly: Clock,
    dang_xu_ly: Loader2,
    da_xu_ly: ShieldCheck,
    tu_choi: ShieldX,
    da_huy: XCircle,
  };
  return map[s] || Clock;
};

// Deadline helpers: phiếu quá 24h chưa tiếp nhận → cảnh báo
const daysSinceCreated = (ngayTiepNhan) => {
  if (!ngayTiepNhan) return -1;
  return Math.ceil((new Date() - new Date(ngayTiepNhan)) / 86400000);
};
const isOverdue = (claim) => {
  if (claim.trangThai !== 'cho_xu_ly') return false;
  return daysSinceCreated(claim.ngayTao || claim.ngayMua) >= 1;
};
const overdueDays = (claim) => {
  if (!isOverdue(claim)) return 0;
  return daysSinceCreated(claim.ngayTao || claim.ngayMua);
};

// ── Detail modal ───────────────────────────────────────────────────────────
const showDetailModal = ref(false);
const detailClaim = ref(null);
const detailImages = ref([]);
const detailComments = ref([]);
const detailLoading = ref(false);

const openDetail = async (claim) => {
  showDetailModal.value = true;
  detailClaim.value = claim;
  detailImages.value = [];
  detailComments.value = [];
  detailLoading.value = true;
  try {
    const [images, comments] = await Promise.all([
      HinhAnhBaoHanhService.getByBaoHanh(claim.baoHanhId),
      BinhLuanBaoHanhService.getByBaoHanh(claim.baoHanhId),
    ]);
    detailImages.value = images || [];
    detailComments.value = comments || [];
  } finally {
    detailLoading.value = false;
  }
};

const closeDetail = () => {
  showDetailModal.value = false;
  detailClaim.value = null;
};

// ── Process actions ────────────────────────────────────────────────────────
const receiptForm = ref({
  trangThai: '',
  ketQuaXuLy: '',
  chiPhiPhatSinh: 0,
  ngayTraKhach: '',
  lyDoTuChoi: '',
});
const processing = ref(false);
const rejectMode = ref(false);
const newComment = ref('');
const sendingComment = ref(false);

const receiveClaim = async () => {
  if (!detailClaim.value) return;
  processing.value = true;
  try {
    const res = await PhieuBaoHanhService.updateStatus(detailClaim.value.baoHanhId, {
      trangThai: WARRANTY_STATUS.DANG_XU_LY,
      ngayBatDauXuLy: nowLocalIso(new Date()),
    });
    if (!res.ok) {
      showToast(`Lỗi: ${res.status}`, 'error');
      return;
    }
    showToast('Đã tiếp nhận phiếu bảo hành', 'success');
    await refreshBaoHanh();
    closeDetail();
  } catch (e) {
    showToast(e.message, 'error');
  } finally {
    processing.value = false;
  }
};

const submitProcessing = async () => {
  if (!detailClaim.value) return;
  processing.value = true;
  try {
    const payload = {
      trangThai: WARRANTY_STATUS.DANG_XU_LY,
      ketQuaXuLy: receiptForm.value.ketQuaXuLy,
      chiPhiPhatSinh: receiptForm.value.chiPhiPhatSinh || 0,
    };
    const res = await PhieuBaoHanhService.updateStatus(detailClaim.value.baoHanhId, payload);
    if (!res.ok) { showToast(`Lỗi: ${res.status}`, 'error'); return; }
    showToast('Đã cập nhật tiến độ', 'success');
    await refreshBaoHanh();
  } catch (e) {
    showToast(e.message, 'error');
  } finally {
    processing.value = false;
  }
};

const completeClaim = async () => {
  if (!detailClaim.value) return;
  if (!confirm(`Hoàn thành phiếu #${detailClaim.value.baoHanhId} và trả khách?`)) return;

  processing.value = true;
  try {
    const payload = {
      trangThai: WARRANTY_STATUS.DA_XU_LY,
      ketQuaXuLy: receiptForm.value.ketQuaXuLy,
      chiPhiPhatSinh: receiptForm.value.chiPhiPhatSinh || 0,
      ngayTraKhach: receiptForm.value.ngayTraKhach
        ? nowLocalIso(new Date(receiptForm.value.ngayTraKhach))
        : nowLocalIso(new Date()),
    };
    const res = await PhieuBaoHanhService.updateStatus(detailClaim.value.baoHanhId, payload);
    if (!res.ok) { showToast(`Lỗi: ${res.status}`, 'error'); return; }
    showToast('Đã hoàn thành phiếu bảo hành', 'success');
    await refreshBaoHanh();
    closeDetail();
  } catch (e) {
    showToast(e.message, 'error');
  } finally {
    processing.value = false;
  }
};

const rejectClaim = async () => {
  if (!detailClaim.value) return;
  if (!receiptForm.value.lyDoTuChoi.trim()) {
    showToast('Vui lòng nhập lý do từ chối', 'warning');
    return;
  }
  if (!confirm(`Từ chối phiếu #${detailClaim.value.baoHanhId}?`)) return;

  processing.value = true;
  try {
    const res = await PhieuBaoHanhService.updateStatus(detailClaim.value.baoHanhId, {
      trangThai: WARRANTY_STATUS.TU_CHOI,
      lyDoTuChoi: receiptForm.value.lyDoTuChoi,
    });
    if (!res.ok) { showToast(`Lỗi: ${res.status}`, 'error'); return; }
    showToast('Đã từ chối phiếu', 'success');
    await refreshBaoHanh();
    closeDetail();
  } catch (e) {
    showToast(e.message, 'error');
  } finally {
    processing.value = false;
  }
};

const sendAdminComment = async () => {
  if (!detailClaim.value || !newComment.value.trim()) return;
  sendingComment.value = true;
  try {
    const res = await BinhLuanBaoHanhService.send(detailClaim.value.baoHanhId, newComment.value.trim());
    if (!res.ok) { showToast(`Lỗi: ${res.status}`, 'error'); return; }
    newComment.value = '';
    const comments = await BinhLuanBaoHanhService.getByBaoHanh(detailClaim.value.baoHanhId);
    detailComments.value = comments || [];
    showToast('Đã gửi phản hồi cho khách', 'success');
  } catch (e) {
    showToast(e.message, 'error');
  } finally {
    sendingComment.value = false;
  }
};

// ── Image lightbox ──────────────────────────────────────────────────────────
const lightboxImage = ref(null);

// ── Refresh ─────────────────────────────────────────────────────────────────
const refresh = async () => {
  await refreshBaoHanh();
};

// ── Lifecycle ───────────────────────────────────────────────────────────────
onMounted(() => {
  ensureBaoHanh();
});

// ── Filter cards (dùng làm nút lọc) ────────────────────────────────────────
const filterCards = [
  { id: 'all', label: 'Tổng phiếu', icon: Shield, color: 'blue' },
  { id: 'pending', label: 'Chờ tiếp nhận', icon: Clock, color: 'cyan' },
  { id: 'processing', label: 'Đang xử lý', icon: Loader2, color: 'green' },
  { id: 'done', label: 'Hoàn thành', icon: ShieldCheck, color: 'yellow' },
  { id: 'rejected', label: 'Từ chối/Hủy', icon: ShieldX, color: 'red' },
];
</script>

<template>
  <div class="claim-management-panel">
    <!-- Filter cards -->
    <div class="cm-filter-bar">
      <button
        v-for="card in filterCards" :key="card.id"
        class="cm-filter-card"
        :class="[`cm-filter-card--${card.color}`, { 'is-active': statusFilter === card.id }]"
        @click="statusFilter = statusFilter === card.id ? 'all' : card.id"
      >
        <div class="cm-filter-card__icon">
          <component :is="card.icon" :size="16" />
        </div>
        <div class="cm-filter-card__body">
          <div class="cm-filter-card__value">{{ card.id === 'all' ? stats.total : (stats[card.id] ?? 0) }}</div>
          <div class="cm-filter-card__label">{{ card.label }}</div>
        </div>
      </button>
    </div>

    <!-- Khung viền bọc search + table -->
    <div class="cm-content-box">
      <!-- Search + Refresh bên dưới -->
      <div class="cm-search-bar">
        <div class="cm-search-wrap">
          <Search :size="13" class="cm-search-icon" />
          <input
            v-model="searchQuery"
            type="search"
            class="cm-search-input"
            placeholder="Tìm mã phiếu, serial, SKU..."
          />
        </div>
        <button
          class="cm-filter-btn"
          :class="{ active: advFilterCount > 0 || isAdvFilterOpen }"
          @click="isAdvFilterOpen = !isAdvFilterOpen"
          title="Bộ lọc nâng cao"
        >
          <Filter :size="13" /> Bộ lọc
          <span v-if="advFilterCount > 0" class="cm-filter-badge">{{ advFilterCount }}</span>
          <ChevronDown v-if="!isAdvFilterOpen" :size="12" />
          <ChevronUp v-else :size="12" />
        </button>
        <button v-if="advFilterCount > 0" class="cm-reset-btn" @click="resetAdvFilters" title="Xóa bộ lọc">
          <X :size="13" />
        </button>
        <button class="cm-refresh-btn" @click="refresh" title="Làm mới">
          <RefreshCw :size="14" :class="{ 'spin': BaoHanhStore.loading }" />
        </button>
      </div>

      <!-- Panel lọc phụ -->
      <div v-if="isAdvFilterOpen" class="cm-adv-panel">
        <div class="cm-adv-row">
          <div class="cm-adv-group cm-adv-group--range">
            <label class="cm-adv-label">Ngày tiếp nhận</label>
            <div class="cm-adv-range">
              <input v-model="advFilters.ngayFrom" type="date" class="cm-adv-input" />
              <span class="cm-adv-sep">–</span>
              <input v-model="advFilters.ngayTo" type="date" class="cm-adv-input" />
            </div>
          </div>
          <div class="cm-adv-group">
            <label class="cm-adv-label">&nbsp;</label>
            <label class="cm-adv-checkbox">
              <input type="checkbox" v-model="advFilters.onlyOverdue" />
              <span>⚠️ Chỉ quá hạn</span>
            </label>
          </div>
          <button v-if="advFilterCount > 0" class="cm-adv-reset" @click="resetAdvFilters">
            <X :size="12" /> Xóa bộ lọc
          </button>
        </div>
      </div>

      <!-- Table -->
      <div v-if="BaoHanhStore.loading && !BaoHanhStore.loaded" class="loading-state">
        <div v-for="i in 5" :key="i"><Skeleton width="100%" height="60px" radius="10px" /></div>
      </div>

      <div v-else-if="filteredClaims.length === 0" class="empty-state">
        <Shield :size="42" class="empty-icon" />
        <div class="empty-title">Không có phiếu bảo hành nào</div>
        <div class="empty-text">{{ statusFilter === 'all' ? 'Chưa có phiếu bảo hành nào trong hệ thống.' : 'Không có phiếu nào ở trạng thái này.' }}</div>
      </div>

      <div v-else class="cm-table-wrap">
        <table class="cm-table">
          <thead>
            <tr>
              <th style="width:40px;">#</th>
              <th>Mã phiếu</th>
              <th>Mã biến thể</th>
              <th>Sản phẩm / Serial</th>
              <th>Khách hàng</th>
              <th>Ngày gửi</th>
              <th>Phương thức</th>
              <th>Trạng thái</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(c, idx) in filteredClaims" :key="c.baoHanhId"
              class="cm-row"
              :class="[`cm-row--${c.trangThai}`, { 'cm-row--alert': c.trangThai === 'cho_xu_ly' }]"
              @click="openDetail(c)"
            >
              <td class="text-muted small">{{ idx + 1 }}</td>
              <td>
                <code class="code-id">#{{ c.baoHanhId }}</code>
                <div class="text-muted small">Đơn #{{ c.donHangId }}</div>
              </td>
              <td>
                <code class="code-serial">{{ c.maSku || '—' }}</code>
              </td>
              <td>
                <div class="cm-product-name">{{ c.tenSanPham || c.maSku || '—' }}</div>
                <code v-if="c.soSerial" class="code-serial">{{ c.soSerial }}</code>
              </td>
              <td>
                <div class="fw-semibold">{{ c.tenKhachHang || `KH#${c.khachHangId}` }}</div>
              </td>
              <td class="text-muted small">{{ formatDate(c.ngayMua || c.ngayTiepNhan) }}</td>
              <td>
                <span v-if="c.phuongThuc === 'giao_tan_noi'" class="cm-pickup-badge"><Truck :size="11" /> Tận nơi</span>
                <span v-else class="cm-pickup-badge cm-pickup-badge--ghost"><MapPin :size="11" /> Tại shop</span>
              </td>
              <td @click.stop>
                <!-- Overdue badge -->
                <div v-if="isOverdue(c)" class="overdue-badge" :title="`Quá ${overdueDays(c)} ngày chưa xử lý`">
                  <AlertTriangle :size="11" />
                  Quá {{ overdueDays(c) }} ngày
                </div>
                <span
                  class="status-pill"
                  :style="{ background: statusColor(c.trangThai).bg, color: statusColor(c.trangThai).text }"
                >
                  <component :is="statusIcon(c.trangThai)" :size="11" :class="{ 'spin': c.trangThai === 'dang_xu_ly' }" />
                  {{ statusLabel(c.trangThai) }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Inline Detail Form -->
    <Transition name="slide-down">
      <div v-if="detailClaim" class="cm-detail-panel">
        <div class="cm-detail-header">
          <div class="cm-detail-title">
            <Wrench :size="18" />
            <span>Phiếu bảo hành #{{ detailClaim.baoHanhId }}</span>
            <span class="cm-detail-product">{{ detailClaim.tenSanPham }} {{ detailClaim.soSerial ? `· ${detailClaim.soSerial}` : '' }}</span>
          </div>
          <button class="cm-detail-close" @click="closeDetail">
            <XCircle :size="18" />
          </button>
        </div>

        <div class="cm-detail-body">
          <!-- Left: Info + Actions -->
          <div class="cm-detail-col">
            <!-- Status -->
            <div
              class="cm-detail-status"
              :style="{ background: statusColor(detailClaim.trangThai).bg, color: statusColor(detailClaim.trangThai).text }"
            >
              <component :is="statusIcon(detailClaim.trangThai)" :size="16" />
              <strong>{{ statusLabel(detailClaim.trangThai) }}</strong>
              <span v-if="detailClaim.ngayTraKhach"> · Hoàn thành: {{ formatDate(detailClaim.ngayTraKhach) }}</span>
              <span v-else-if="detailClaim.ngayBatDauXuLy"> · Đang xử lý từ: {{ formatDate(detailClaim.ngayBatDauXuLy) }}</span>
              <span v-else> · Gửi: {{ formatDate(detailClaim.ngayMua) }}</span>
            </div>

            <!-- Info cards row -->
            <div class="cm-detail-info-row">
              <div class="cm-info-card">
                <div class="cm-info-card-title"><User :size="13" /> Khách hàng</div>
                <div class="cm-info-card-content">
                  <div><strong>{{ detailClaim.tenKhachHang || `KH#${detailClaim.khachHangId}` }}</strong></div>
                  <div class="text-muted small">Mã KH #{{ detailClaim.khachHangId }}</div>
                </div>
              </div>
              <div class="cm-info-card">
                <div class="cm-info-card-title"><Package :size="13" /> Sản phẩm</div>
                <div class="cm-info-card-content">
                  <div><strong>{{ detailClaim.tenSanPham || '—' }}</strong></div>
                  <div class="text-muted small">SKU: {{ detailClaim.maSku || '—' }} · Serial: {{ detailClaim.soSerial || '—' }}</div>
                </div>
              </div>
            </div>

            <!-- Issue -->
            <div class="cm-issue-block">
              <div class="cm-issue-label"><AlertCircle :size="13" /> Mô tả lỗi</div>
              <div class="cm-issue-text">{{ detailClaim.moTaLoi || '—' }}</div>
            </div>

            <!-- Actions -->
            <div class="cm-detail-actions">
              <button v-if="canAdminReceive(detailClaim.trangThai)" class="cm-btn cm-btn--primary" @click="receiveClaim" :disabled="processing">
                <Loader2 v-if="processing" :size="14" class="spin" />
                <SendIcon v-else :size="14" />
                Tiếp nhận
              </button>

              <div v-if="detailClaim.trangThai === WARRANTY_STATUS.DANG_XU_LY" class="cm-action-form">
                <div class="cm-action-form-row">
                  <div class="cm-form-group">
                    <label>Kết quả xử lý</label>
                    <textarea v-model="receiptForm.ketQuaXuLy" rows="2" placeholder="Nhập kết quả..."></textarea>
                  </div>
                  <div class="cm-form-group">
                    <label>Chi phí (VNĐ)</label>
                    <input v-model.number="receiptForm.chiPhiPhatSinh" type="number" min="0" placeholder="0" />
                  </div>
                  <div class="cm-form-group">
                    <label>Ngày trả</label>
                    <input v-model="receiptForm.ngayTraKhach" type="datetime-local" />
                  </div>
                </div>
                <div class="cm-action-btns">
                  <button class="cm-btn cm-btn--secondary" @click="submitProcessing" :disabled="processing">
                    <Save :size="14" /> Lưu
                  </button>
                  <button class="cm-btn cm-btn--success" @click="completeClaim" :disabled="processing">
                    <CheckCircle2 :size="14" /> Hoàn thành
                  </button>
                </div>
              </div>

              <button v-if="!['da_xu_ly', 'da_huy'].includes(detailClaim.trangThai)" class="cm-btn cm-btn--danger" @click="rejectClaim" :disabled="processing">
                <ShieldX :size="14" />
                Từ chối
              </button>
            </div>
          </div>

          <!-- Right: Images + Comments -->
          <div class="cm-detail-col">
            <!-- Images -->
            <div class="cm-media-section">
              <div class="cm-media-title"><ImageIcon :size="13" /> Ảnh / Video <span v-if="detailImages.length" class="count-pill">{{ detailImages.length }}</span></div>
              <div v-if="detailLoading" class="cm-media-loading">Đang tải...</div>
              <div v-else-if="detailImages.length === 0" class="cm-media-empty">Không có ảnh/video</div>
              <div v-else class="cm-media-grid">
                <div v-for="img in detailImages" :key="img.hinhAnhId" class="cm-media-thumb" @click="lightboxImage = img">
                  <img v-if="img.loai === 'image'" :src="img.url" />
                  <video v-else :src="img.url" muted></video>
                </div>
              </div>
            </div>

            <!-- Comments -->
            <div class="cm-comment-section">
              <div class="cm-media-title"><MessageCircle :size="13" /> Trao đổi <span v-if="detailComments.length" class="count-pill">{{ detailComments.length }}</span></div>
              <div v-if="detailLoading" class="cm-media-loading">Đang tải...</div>
              <div v-else-if="detailComments.length === 0" class="cm-media-empty">Chưa có trao đổi</div>
              <div v-else class="cm-comment-list">
                <div v-for="c in detailComments" :key="c.binhLuanId" class="cm-comment" :class="{ 'cm-comment--staff': c.vaiTro !== 'khach_hang' }">
                  <div class="cm-comment-avatar">{{ (c.tenNguoiGui || 'U').charAt(0).toUpperCase() }}</div>
                  <div class="cm-comment-body">
                    <div class="cm-comment-meta">
                      <strong>{{ c.tenNguoiGui }}</strong>
                      <span class="cm-role-pill">{{ c.vaiTro === 'khach_hang' ? 'Khách' : 'NV' }}</span>
                      <span class="text-muted small ms-auto">{{ formatDate(c.ngayGui) }}</span>
                    </div>
                    <div class="cm-comment-text">{{ c.noiDung }}</div>
                  </div>
                </div>
              </div>
              <form v-if="!['da_xu_ly', 'da_huy', 'tu_choi'].includes(detailClaim.trangThai)" class="cm-comment-form" @submit.prevent="sendAdminComment">
                <textarea v-model="newComment" rows="2" placeholder="Phản hồi..." maxlength="500"></textarea>
                <button type="submit" class="cm-btn-send" :disabled="!newComment.trim() || sendingComment">
                  <Loader2 v-if="sendingComment" :size="14" class="spin" />
                  <SendIcon v-else :size="14" />
                </button>
              </form>
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Lightbox -->
    <Transition name="lightbox-fade">
      <div v-if="lightboxImage" class="lightbox-overlay" @click="lightboxImage = null">
        <img v-if="lightboxImage.loai === 'image'" :src="lightboxImage.url" />
        <video v-else :src="lightboxImage.url" controls autoplay></video>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.claim-management-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: transparent;
  border: none;
  border-radius: 0;
  padding: 0;
  color: var(--text-primary);
  box-shadow: none;
}

.cm-filter-bar {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
  margin-bottom: 12px;
}

.cm-filter-card {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;
  height: 96px;
  color: #fff;
  border: none;
  border-radius: 14px;
  padding: 0 14px;
  box-shadow: 0 4px 14px rgba(0,0,0,.12);
  cursor: pointer;
  user-select: none;
  transition: transform .12s, box-shadow .12s, border-color .12s;
  border: 2px solid transparent;
  overflow: hidden;
}
.cm-filter-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(0,0,0,.18);
}
.cm-filter-card.is-active {
  border-color: rgba(255,255,255,.85);
  box-shadow: 0 6px 18px rgba(0,0,0,.25);
  transform: translateY(-2px);
}
.cm-filter-card__icon {
  width: 40px; height: 40px;
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  background: rgba(255,255,255,.22);
  flex-shrink: 0;
}
.cm-filter-card__body {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
}
.cm-filter-card__label {
  font-size: 11px;
  font-weight: 600;
  color: rgba(255,255,255,.85);
  margin-bottom: 2px;
  white-space: nowrap;
}
.cm-filter-card__value {
  font-size: 1.4rem;
  font-weight: 800;
  color: #fff;
  line-height: 1;
}
.cm-filter-card--blue     { background: linear-gradient(135deg, #60a5fa, #2563eb); }
.cm-filter-card--cyan     { background: linear-gradient(135deg, #38bdf8, #0284c7); }
.cm-filter-card--yellow   { background: linear-gradient(135deg, #fbbf24, #d97706); }
.cm-filter-card--green    { background: linear-gradient(135deg, #34d399, #059669); }
.cm-filter-card--red      { background: linear-gradient(135deg, #f87171, #dc2626); }

.cm-filter-card--blue:hover    { box-shadow: 0 6px 18px rgba(37,99,235,.24); }
.cm-filter-card--cyan:hover    { box-shadow: 0 6px 18px rgba(14,165,233,.24); }
.cm-filter-card--yellow:hover  { box-shadow: 0 6px 18px rgba(217,119,6,.24); }
.cm-filter-card--green:hover   { box-shadow: 0 6px 18px rgba(5,150,105,.24); }
.cm-filter-card--red:hover     { box-shadow: 0 6px 18px rgba(220,38,38,.24); }

.cm-content-box {
  background: var(--bg-card, #fff);
  border: 1px solid var(--border);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: var(--sh-2, 0 8px 18px rgba(0,0,0,.06));
}

.cm-search-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 12px 16px;
}
.cm-search-wrap {
  position: relative;
  flex: 1 1 240px;
  min-width: 200px;
  max-width: 340px;
}
.cm-search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--pink-500, #ec4899);
  pointer-events: none;
  z-index: 1;
}
.cm-search-input {
  width: 100%;
  padding: 8px 14px 8px 34px;
  border: 1px solid var(--pink-200, #fbcfe8);
  border-radius: 999px;
  font-size: 13px;
  background: var(--pink-50, #fff1f7);
  font-family: inherit;
  color: var(--text-primary, var(--ink));
  outline: none;
  transition: all .2s ease;
}
.cm-search-input:focus {
  outline: none;
  border-color: var(--pink-500, #ec4899);
  background: #fff;
  box-shadow: 0 0 0 3px var(--pink-100, #fce7f3);
}

.cm-refresh-btn {
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
  transition: all .2s ease;
}
.cm-refresh-btn:hover {
  background: var(--pink-100);
  border-color: var(--pink-400);
  color: var(--pink-600);
}

.cm-table-wrap {
  background: var(--bg-card, #fff);
  border: 0;
  border-radius: 0;
  overflow: visible;
  box-shadow: none;
}
.cm-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  font-size: 12.5px;
}
.cm-table thead {
  background: var(--pink-50, #fdf2f8);
  border-bottom: 2px solid var(--pink-200, #fbcfe8);
}
.cm-table th {
  text-align: left;
  padding: 14px 16px;
  font-size: 11px;
  font-weight: 800;
  color: var(--pink-700, #be185d);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  border-bottom: 2px solid var(--pink-200, #fbcfe8);
  white-space: nowrap;
  background: transparent;
}
.cm-row {
  border-bottom: 1px solid transparent;
  transition: background 0.2s ease, color 0.2s ease, transform 0.2s ease;
  cursor: pointer;
}
.cm-row:hover { background: var(--bg-hover, #fdf2f8); color: var(--text-primary, #1f2937); }
.cm-row--alert {
  background: rgba(245, 158, 11, 0.04);
  border-left: 3px solid #f59e0b;
}
.cm-row td {
  padding: 12px 14px;
  vertical-align: middle;
  color: var(--text-primary, #1f2937);
  font-size: 12.5px;
  border-bottom: 1px solid transparent;
  white-space: nowrap;
}
.cm-row td.text-muted { color: var(--text-muted); }
.cm-product-name {
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
}
.code-id, .code-serial, .code-mono {
  background: var(--bg-input);
  padding: 1px 6px;
  border-radius: 4px;
  font-family: 'SF Mono', monospace;
  font-size: 11.5px;
  color: var(--accent-fg);
  font-weight: 600;
}
.cm-pickup-badge {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 3px 8px;
  border-radius: 9999px;
  font-size: 10.5px;
  font-weight: 600;
  background: var(--pink-50, #fff1f7);
  color: var(--pink-700, #be185d);
}
.cm-pickup-badge--ghost {
  background: var(--bg-input);
  color: var(--text-secondary);
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 9999px;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}
.text-muted { color: var(--text-muted); }
.fw-semibold { font-weight: 600; }
.ms-auto { margin-left: auto; }

.loading-state { display: flex; flex-direction: column; gap: 8px; }
.empty-state { text-align: center; padding: 40px 20px; }
.empty-icon { color: var(--text-muted); margin-bottom: 10px; }
.empty-title { font-weight: 700; font-size: 0.95rem; color: var(--text-primary); }
.empty-text { font-size: 12px; color: var(--text-secondary); margin-top: 6px; }

/* Detail Panel */
.cm-detail-panel {
  background: var(--bg-card, #fff);
  border: 1px solid var(--border);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: var(--sh-2, 0 8px 18px rgba(0,0,0,.06));
  margin-top: 12px;
}

.cm-detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: var(--gray-50, #f8fafc);
  border-bottom: 1px solid var(--border);
}

.cm-detail-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  color: var(--text-heading);
}
.cm-detail-title svg { color: var(--pink-500); }
.cm-detail-product {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 500;
}

.cm-detail-close {
  width: 32px; height: 32px;
  background: transparent;
  border: none;
  border-radius: 8px;
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.cm-detail-close:hover { background: var(--gray-100); color: var(--text-primary); }

.cm-detail-body {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  padding: 16px;
}

.cm-detail-col {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cm-detail-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  width: fit-content;
}

.cm-detail-info-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.cm-info-card {
  background: var(--gray-50);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 12px;
}

.cm-info-card-title {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  font-weight: 700;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-bottom: 6px;
}
.cm-info-card-title svg { color: var(--pink-500); }
.cm-info-card-content { font-size: 12.5px; color: var(--text-primary); }
.cm-info-card-content small { display: block; margin-top: 2px; }

.cm-issue-block {
  background: var(--gray-50);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 12px;
}

.cm-issue-label {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  font-weight: 700;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-bottom: 8px;
}
.cm-issue-label svg { color: var(--warning, #f59e0b); }
.cm-issue-text {
  font-size: 12.5px;
  color: var(--text-primary);
  line-height: 1.5;
  white-space: pre-wrap;
}

.cm-detail-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.cm-action-form {
  background: var(--gray-50);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 12px;
}

.cm-action-form-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 8px;
  margin-bottom: 10px;
}

.cm-form-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.cm-form-group label {
  font-size: 11px;
  font-weight: 700;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.cm-form-group input,
.cm-form-group textarea {
  padding: 7px 10px;
  border: 1px solid var(--border);
  border-radius: 6px;
  font-size: 12.5px;
  background: #fff;
  color: var(--text-primary);
  font-family: inherit;
  outline: none;
  transition: border-color 0.2s ease;
}
.cm-form-group input:focus,
.cm-form-group textarea:focus {
  border-color: var(--pink-500);
}
.cm-form-group textarea { resize: vertical; }

.cm-action-btns {
  display: flex;
  gap: 8px;
}

.cm-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 8px 16px;
  border: none;
  border-radius: 8px;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}
.cm-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.cm-btn--primary { background: var(--pink-500); color: #fff; }
.cm-btn--primary:hover:not(:disabled) { background: var(--pink-600); }
.cm-btn--secondary { background: var(--gray-100); color: var(--text-primary); border: 1px solid var(--border); }
.cm-btn--secondary:hover:not(:disabled) { background: var(--gray-200); }
.cm-btn--success { background: var(--success, #16a34a); color: #fff; }
.cm-btn--success:hover:not(:disabled) { filter: brightness(1.1); }
.cm-btn--danger { background: var(--danger, #dc2626); color: #fff; }
.cm-btn--danger:hover:not(:disabled) { filter: brightness(1.1); }

.cm-media-section,
.cm-comment-section {
  background: var(--gray-50);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 12px;
}

.cm-media-title {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  font-weight: 700;
  color: var(--text-heading);
  margin-bottom: 10px;
}
.cm-media-title svg { color: var(--pink-500); }

.cm-media-empty,
.cm-media-loading {
  text-align: center;
  padding: 20px;
  font-size: 12px;
  color: var(--text-muted);
  background: #fff;
  border-radius: 8px;
}

.cm-media-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
}

.cm-media-thumb {
  aspect-ratio: 1;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--border);
  transition: border-color 0.15s ease;
}
.cm-media-thumb:hover { border-color: var(--pink-500); }
.cm-media-thumb img,
.cm-media-thumb video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cm-comment-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 180px;
  overflow-y: auto;
  margin-bottom: 10px;
}

.cm-comment {
  display: flex;
  gap: 8px;
}

.cm-comment-avatar {
  width: 28px; height: 28px;
  border-radius: 50%;
  background: var(--gray-100);
  color: var(--text-secondary);
  font-weight: 700;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.cm-comment--staff .cm-comment-avatar { background: var(--pink-500); color: #fff; }

.cm-comment-body {
  flex: 1;
  background: #fff;
  border-radius: 10px;
  padding: 8px 10px;
}
.cm-comment--staff .cm-comment-body { background: var(--pink-50); }

.cm-comment-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}
.cm-comment-meta strong { font-size: 12px; }

.cm-role-pill {
  padding: 1px 6px;
  border-radius: 9999px;
  font-size: 9px;
  font-weight: 700;
  text-transform: uppercase;
  background: var(--pink-500);
  color: #fff;
}
.cm-comment-text {
  font-size: 12.5px;
  color: var(--text-primary);
  line-height: 1.4;
  white-space: pre-wrap;
}

.cm-comment-form {
  display: flex;
  gap: 6px;
  align-items: flex-end;
}
.cm-comment-form textarea {
  flex: 1;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: 8px;
  font-size: 12.5px;
  background: #fff;
  color: var(--text-primary);
  font-family: inherit;
  resize: none;
  outline: none;
}
.cm-comment-form textarea:focus { border-color: var(--pink-500); }

.cm-btn-send {
  width: 36px; height: 36px;
  background: var(--pink-500);
  color: #fff;
  border: none;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
}
.cm-btn-send:hover:not(:disabled) { background: var(--pink-600); }
.cm-btn-send:disabled { opacity: 0.5; cursor: not-allowed; }

.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.25s ease;
  overflow: hidden;
}
.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  transform: translateY(-10px);
  max-height: 0;
}
.slide-down-enter-to,
.slide-down-leave-from {
  opacity: 1;
  transform: translateY(0);
  max-height: 800px;
}

.lightbox-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.92);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1200;
  padding: 20px;
}
.lightbox-overlay img, .lightbox-overlay video {
  max-width: 90vw;
  max-height: 90vh;
  object-fit: contain;
}
.lightbox-fade-enter-active, .lightbox-fade-leave-active { transition: opacity 0.2s ease; }
.lightbox-fade-enter-from, .lightbox-fade-leave-to { opacity: 0; }

.overdue-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  background: rgba(239, 68, 68, 0.12);
  color: #dc2626;
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 9999px;
  font-size: 10.5px;
  font-weight: 700;
  margin-bottom: 4px;
  animation: overdue-pulse 2s ease-in-out infinite;
}
@keyframes overdue-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.spin { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

@media (max-width: 1200px) {
  .cm-filter-bar { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 991.98px) {
  .cm-filter-bar { grid-template-columns: repeat(2, 1fr); }
  .cm-detail-body { grid-template-columns: 1fr; }
  .cm-detail-info-row { grid-template-columns: 1fr; }
  .cm-action-form-row { grid-template-columns: 1fr 1fr; }
}
@media (max-width: 575.98px) {
  .cm-filter-bar { grid-template-columns: repeat(2, 1fr); gap: 8px; }
  .cm-filter-card { min-height: 80px; padding: 12px 14px; }
  .cm-filter-card__icon { width: 36px; height: 36px; }
  .cm-filter-card__value { font-size: 1.25rem; }
  .cm-filter-card__label { font-size: 11px; }
  .cm-action-form-row { grid-template-columns: 1fr; }
  .cm-media-grid { grid-template-columns: repeat(3, 1fr); }
}

/* ─── Advanced Filter Panel (mới thêm) ─── */
.cm-filter-btn {
  display: inline-flex; align-items: center; gap: 5px; padding: 6px 12px;
  border: 1px solid var(--border-color, #e2e8f0); border-radius: 8px;
  background: var(--bg-card, #fff); color: var(--text-primary, #1e293b);
  font-size: 13px; font-weight: 500; cursor: pointer; transition: all 0.15s; flex-shrink: 0;
}
.cm-filter-btn:hover, .cm-filter-btn.active {
  border-color: var(--pink-400, #f472b6); background: var(--pink-50, #fdf2f8); color: var(--pink-700, #be185d);
}
.cm-filter-badge {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 16px; height: 16px; padding: 0 4px; border-radius: 8px;
  background: var(--pink-600, #db2777); color: #fff; font-size: 10px; font-weight: 700;
}
.cm-reset-btn {
  display: inline-flex; align-items: center; justify-content: center;
  width: 30px; height: 30px; border: 1px solid #dc2626; border-radius: 7px;
  background: transparent; color: #dc2626; cursor: pointer; transition: all 0.15s; flex-shrink: 0;
}
.cm-reset-btn:hover { background: #dc2626; color: #fff; }
.cm-adv-panel {
  border-top: 1px solid var(--border-color, #e2e8f0); background: var(--bg-card-alt, #f8fafc);
  padding: 12px 16px; animation: fadeIn 0.15s ease;
}
@keyframes fadeIn { from { opacity:0; transform:translateY(-4px); } to { opacity:1; transform:translateY(0); } }
.cm-adv-row { display: flex; flex-wrap: wrap; align-items: flex-end; gap: 12px; }
.cm-adv-group { display: flex; flex-direction: column; gap: 4px; min-width: 140px; }
.cm-adv-group--range { min-width: 260px; }
.cm-adv-label { font-size: 11px; font-weight: 600; color: var(--text-secondary, #64748b); text-transform: uppercase; letter-spacing: 0.04em; }
.cm-adv-input {
  padding: 6px 10px; border: 1px solid var(--border-color, #e2e8f0); border-radius: 7px;
  background: #fff; color: var(--text-primary, #1e293b); font-size: 13px; outline: none; transition: border-color 0.15s;
}
.cm-adv-input:focus { border-color: var(--pink-500, #ec4899); }
.cm-adv-range { display: flex; align-items: center; gap: 6px; }
.cm-adv-sep { color: var(--text-secondary, #94a3b8); font-size: 13px; font-weight: 600; }
.cm-adv-checkbox {
  display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--text-primary, #1e293b);
  cursor: pointer; padding: 7px 12px; border: 1px solid var(--border-color, #e2e8f0);
  border-radius: 7px; background: #fff; white-space: nowrap;
}
.cm-adv-checkbox input { cursor: pointer; accent-color: var(--pink-500, #ec4899); width: 14px; height: 14px; }
.cm-adv-reset {
  display: inline-flex; align-items: center; gap: 5px; padding: 6px 12px;
  border: 1px solid #dc2626; border-radius: 7px; background: transparent; color: #dc2626;
  font-size: 12px; font-weight: 500; cursor: pointer; align-self: flex-end; transition: all 0.15s;
}
.cm-adv-reset:hover { background: #dc2626; color: #fff; }
</style>

