<script setup>
import { ref, computed, onMounted, watch } from "vue";
import { t } from "../../i18n/index.js";
import { AuthStore } from "../../stores/index.js";
import { WarrantyStore, ensureMyClaims, refreshMyClaims } from "../../services/warrantyStore.js";
import {
  WARRANTY_STATUS,
  STATUS_COLOR,
  STATUS_I18N_KEY,
  canKhachHangCancel,
  isActive,
} from "../../services/warrantyConstants.js";
import * as PhieuBaoHanhService from "../../services/PhieuBaoHanhService.js";
import Skeleton from "../common/Skeleton.vue";
import WarrantyClaimDetailModal from "./WarrantyClaimDetailModal.vue";
import {
  FileText, Shield, Clock, ShieldAlert, ShieldCheck, ShieldX,
  Loader2, RefreshCw, ChevronRight, AlertCircle, Send, XCircle, Package,
} from '@lucide/vue';

const emit = defineEmits(["toast", "view-claim"]);

const auth = AuthStore;
const statusFilter = ref('all');
const detailClaimId = ref(null);
const cancellingId = ref(null);
const refreshing = ref(false);

const refresh = async () => {
  if (!auth.user?.id) return;
  refreshing.value = true;
  try {
    await refreshMyClaims(auth.user.id);
    emit("toast", "Đã làm mới", "success");
  } finally {
    refreshing.value = false;
  }
};

// ── Filter ─────────────────────────────────────────────────────────────────
const filteredClaims = computed(() => {
  const list = WarrantyStore.myClaims || [];
  if (statusFilter.value === 'all')         return list;
  if (statusFilter.value === 'active')      return list.filter((c) => isActive(c.trangThai));
  if (statusFilter.value === 'done')        return list.filter((c) => c.trangThai === WARRANTY_STATUS.DA_XU_LY);
  if (statusFilter.value === 'rejected')    return list.filter((c) => c.trangThai === WARRANTY_STATUS.TU_CHOI);
  if (statusFilter.value === 'cancelled')   return list.filter((c) => c.trangThai === WARRANTY_STATUS.DA_HUY);
  return list.filter((c) => c.trangThai === statusFilter.value);
});

const statusCounts = computed(() => {
  const list = WarrantyStore.myClaims || [];
  return {
    all: list.length,
    active: list.filter((c) => isActive(c.trangThai)).length,
    done: list.filter((c) => c.trangThai === WARRANTY_STATUS.DA_XU_LY).length,
    rejected: list.filter((c) => c.trangThai === WARRANTY_STATUS.TU_CHOI).length,
    cancelled: list.filter((c) => c.trangThai === WARRANTY_STATUS.DA_HUY).length,
  };
});

const statusTabs = [
  { id: 'all', label: 'Tất cả', icon: FileText, color: 'all' },
  { id: 'active', label: 'Đang xử lý', icon: Clock, color: 'blue' },
  { id: 'done', label: 'Hoàn thành', icon: ShieldCheck, color: 'green' },
  { id: 'rejected', label: 'Từ chối', icon: ShieldX, color: 'red' },
  { id: 'cancelled', label: 'Đã hủy', icon: XCircle, color: 'gray' },
];

// ── Cancel phiếu ────────────────────────────────────────────────────────────
const cancelClaim = async (claim) => {
  if (!canKhachHangCancel(claim.trangThai)) return;
  if (cancellingId.value) return;
  if (!confirm(`Bạn chắc chắn muốn hủy phiếu #${claim.baoHanhId}?`)) return;

  cancellingId.value = claim.baoHanhId;
  try {
    const res = await PhieuBaoHanhService.huyPhieu(claim.baoHanhId, 'Khách hàng tự hủy');
    if (res?.ok === false) {
      emit("toast", "Không thể hủy phiếu. Vui lòng liên hệ CSKH.", "error");
      return;
    }
    emit("toast", "Đã hủy phiếu bảo hành", "success");
    await refreshMyClaims(auth.user.id);
  } catch (e) {
    emit("toast", e.message || "Có lỗi xảy ra", "error");
  } finally {
    cancellingId.value = null;
  }
};

// ── Format helpers ─────────────────────────────────────────────────────────
const formatDate = (d) => {
  if (!d) return "—";
  try { return new Date(d).toLocaleString('vi-VN'); } catch { return d; }
};

const formatDateShort = (d) => {
  if (!d) return "—";
  try {
    return new Date(d).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
  } catch { return d; }
};

// ── Status helpers ─────────────────────────────────────────────────────────
const statusInfo = (s) => ({
  color: STATUS_COLOR[s] || STATUS_COLOR.cho_xu_ly,
  i18nKey: STATUS_I18N_KEY[s],
});

const statusLabel = (s) => {
  const map = {
    cho_xu_ly: 'Chờ tiếp nhận',
    dang_xu_ly: 'Đang xử lý',
    da_xu_ly: 'Hoàn thành',
    tu_choi: 'Từ chối',
    da_huy: 'Đã hủy',
  };
  return map[s] || s;
};

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

// ── Lifecycle ──────────────────────────────────────────────────────────────
onMounted(() => {
  if (auth.user?.id) {
    ensureMyClaims(auth.user.id);
  }
});

watch(() => auth.user?.id, (newId) => {
  if (newId) {
    ensureMyClaims(newId);
  }
});
</script>

<template>
  <div class="claim-list-panel">
    <!-- Filter tabs -->
    <div class="filter-tabs">
      <button
        v-for="tab in statusTabs"
        :key="tab.id"
        class="filter-tab"
        :class="{ 'is-active': statusFilter === tab.id }"
        @click="statusFilter = tab.id"
      >
        <component :is="tab.icon" :size="14" />
        <span>{{ tab.label }}</span>
        <span v-if="statusCounts[tab.id]" class="filter-tab-count">{{ statusCounts[tab.id] }}</span>
      </button>
      <button class="refresh-btn" @click="refresh" :disabled="refreshing" title="Làm mới">
        <RefreshCw :size="14" :class="{ 'spin': refreshing }" />
      </button>
    </div>

    <!-- Loading -->
    <div v-if="WarrantyStore.myClaimsLoading" class="loading-state">
      <div v-for="i in 3" :key="i"><Skeleton width="100%" height="100px" radius="12px" /></div>
    </div>

    <!-- Empty -->
    <div v-else-if="filteredClaims.length === 0" class="empty-state">
      <div class="empty-icon-wrap">
        <FileText :size="48" />
      </div>
      <div class="empty-title">Chưa có phiếu bảo hành</div>
      <div class="empty-text">
        {{ statusFilter === 'all'
          ? 'Bạn chưa gửi yêu cầu bảo hành nào. Vào tab "Sản phẩm của tôi" để tạo yêu cầu.'
          : 'Không có phiếu nào ở trạng thái này' }}
      </div>
    </div>

    <!-- Claim list -->
    <div v-else class="claim-list">
      <div
        v-for="claim in filteredClaims"
        :key="claim.baoHanhId"
        class="claim-card"
        @click="detailClaimId = claim.baoHanhId"
      >
        <!-- Header -->
        <div class="claim-card-header">
          <div class="claim-card-id">
            <span class="claim-id-label">Phiếu</span>
            <span class="claim-id-value">#{{ claim.baoHanhId }}</span>
          </div>
          <span
            class="status-pill"
            :style="{ background: statusInfo(claim.trangThai).color.bg, color: statusInfo(claim.trangThai).color.text }"
          >
            <component
              :is="statusIcon(claim.trangThai)"
              :size="11"
              :class="{ 'spin': claim.trangThai === 'dang_xu_ly' }"
            />
            {{ statusLabel(claim.trangThai) }}
          </span>
        </div>

        <!-- Body -->
        <div class="claim-card-body">
          <div class="claim-product">
            <div class="claim-product-icon">
              <Package :size="20" />
            </div>
            <div class="claim-product-info">
              <div class="claim-product-name">{{ claim.tenSanPham || 'Sản phẩm' }}</div>
              <div class="claim-product-meta">
                {{ claim.maSku || '' }}
                <template v-if="claim.soSerial">
                  · Serial: <code>{{ claim.soSerial }}</code>
                </template>
              </div>
            </div>
          </div>

          <div class="claim-issue">
            <div class="claim-issue-label">Mô tả lỗi:</div>
            <div class="claim-issue-text">{{ claim.moTaLoi || '—' }}</div>
          </div>
        </div>

        <!-- Footer -->
        <div class="claim-card-footer">
          <div class="claim-meta-row">
            <div class="claim-meta-item">
              <Clock :size="11" />
              <span>Gửi: {{ formatDateShort(claim.ngayTiepNhan || claim.ngayMua) }}</span>
            </div>
            <div v-if="claim.ngayTraKhach" class="claim-meta-item claim-meta-item--done">
              <ShieldCheck :size="11" />
              <span>Trả: {{ formatDateShort(claim.ngayTraKhach) }}</span>
            </div>
          </div>

          <div class="claim-actions" @click.stop>
            <button class="btn-action-mini btn-action-mini--ghost" @click="detailClaimId = claim.baoHanhId">
              Chi tiết <ChevronRight :size="12" />
            </button>
            <button
              v-if="canKhachHangCancel(claim.trangThai)"
              class="btn-action-mini btn-action-mini--danger"
              @click="cancelClaim(claim)"
              :disabled="cancellingId === claim.baoHanhId"
            >
              <XCircle :size="12" />
              {{ cancellingId === claim.baoHanhId ? 'Đang hủy...' : 'Hủy phiếu' }}
            </button>
          </div>
        </div>

        <!-- Timeline mini -->
        <div class="claim-timeline-mini">
          <div
            v-for="(step, idx) in ['cho_xu_ly', 'dang_xu_ly', 'da_xu_ly']"
            :key="step"
            class="timeline-step"
            :class="{
              'is-done': ['da_xu_ly', 'tu_choi', 'da_huy'].includes(claim.trangThai)
                ? idx <= 2
                : claim.trangThai === 'dang_xu_ly'
                  ? idx <= 1
                  : claim.trangThai === 'cho_xu_ly'
                    ? idx === 0
                    : false,
              'is-current': (claim.trangThai === 'cho_xu_ly' && idx === 0)
                          || (claim.trangThai === 'dang_xu_ly' && idx === 1)
                          || (claim.trangThai === 'da_xu_ly' && idx === 2),
            }"
          >
            <span class="timeline-dot"></span>
            <span class="timeline-label">{{ statusLabel(step) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Detail modal -->
    <WarrantyClaimDetailModal
      v-if="detailClaimId"
      :claim-id="detailClaimId"
      :is-khach-hang-view="true"
      @close="detailClaimId = null"
      @cancelled="refresh"
    />
  </div>
</template>

<style scoped>
.claim-list-panel { display: flex; flex-direction: column; gap: 16px; }

.filter-tabs {
  display: flex;
  gap: 6px;
  padding: 8px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow-x: auto;
  scrollbar-width: none;
}

.filter-tabs::-webkit-scrollbar { display: none; }

.filter-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 12px;
  background: transparent;
  border: none;
  border-radius: 9999px;
  color: var(--text-secondary);
  font-size: 12.5px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-tab:hover { background: var(--primary-light); color: var(--primary); }
.filter-tab.is-active {
  background: var(--primary);
  color: white;
}

.filter-tab-count {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 9999px;
  font-size: 10px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.filter-tab:not(.is-active) .filter-tab-count {
  background: var(--gray-100);
  color: var(--text-secondary);
}

.refresh-btn {
  margin-left: auto;
  width: 32px;
  height: 32px;
  background: transparent;
  border: 1px solid var(--border);
  border-radius: 8px;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.refresh-btn:hover:not(:disabled) {
  background: var(--primary-light);
  color: var(--primary);
  border-color: var(--primary);
}

.refresh-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.spin { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

.claim-list { display: flex; flex-direction: column; gap: 12px; }

.claim-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s ease;
}

.claim-card:hover {
  border-color: var(--primary);
  box-shadow: 0 4px 16px rgba(219, 39, 119, 0.1);
  transform: translateY(-1px);
}

.claim-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
  background: var(--gray-50);
}

.claim-card-id { display: flex; align-items: center; gap: 8px; }
.claim-id-label {
  font-size: 10.5px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.claim-id-value {
  font-weight: 800;
  font-size: 13.5px;
  color: var(--gray-900);
  font-family: 'SF Mono', monospace;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 9999px;
  font-size: 11px;
  font-weight: 600;
}

.claim-card-body {
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.claim-product {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.claim-product-icon {
  width: 40px;
  height: 40px;
  background: var(--pink-50);
  color: var(--primary);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.claim-product-info { flex: 1; min-width: 0; }
.claim-product-name {
  font-weight: 700;
  font-size: 13.5px;
  color: var(--gray-900);
  line-height: 1.3;
}
.claim-product-meta {
  font-size: 11.5px;
  color: var(--text-secondary);
  margin-top: 2px;
}
.claim-product-meta code {
  background: var(--gray-100);
  padding: 1px 4px;
  border-radius: 3px;
  font-family: 'SF Mono', monospace;
  font-size: 10.5px;
}

.claim-issue {
  padding: 10px 12px;
  background: var(--gray-50);
  border-radius: 10px;
}

.claim-issue-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.claim-issue-text {
  font-size: 12.5px;
  color: var(--gray-700);
  margin-top: 4px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.claim-card-footer {
  padding: 10px 16px;
  border-top: 1px solid var(--border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  background: var(--bg-card);
}

.claim-meta-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.claim-meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--text-secondary);
}

.claim-meta-item--done {
  color: var(--success);
  font-weight: 600;
}

.claim-actions { display: flex; gap: 6px; }

.btn-action-mini {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  background: transparent;
  border: 1px solid var(--border);
  border-radius: 9999px;
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-action-mini--ghost:hover:not(:disabled) {
  background: var(--primary-light);
  color: var(--primary);
  border-color: var(--primary);
}

.btn-action-mini--danger {
  color: var(--primary);
  border-color: var(--primary);
}

.btn-action-mini--danger:hover:not(:disabled) {
  background: var(--primary-light);
}

.btn-action-mini:disabled { opacity: 0.5; cursor: not-allowed; }

.claim-timeline-mini {
  display: flex;
  justify-content: space-between;
  padding: 12px 16px;
  background: var(--gray-50);
  position: relative;
}

.claim-timeline-mini::before {
  content: '';
  position: absolute;
  left: 24px;
  right: 24px;
  top: 50%;
  height: 2px;
  background: var(--border);
  transform: translateY(-50%);
  z-index: 0;
}

.timeline-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  position: relative;
  z-index: 1;
  flex: 1;
}

.timeline-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--bg-card);
  border: 2px solid var(--border);
  transition: all 0.2s ease;
}

.timeline-step.is-done .timeline-dot {
  background: var(--success);
  border-color: var(--success);
}

.timeline-step.is-current .timeline-dot {
  background: var(--primary);
  border-color: var(--primary);
  box-shadow: 0 0 0 4px var(--primary-light);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 4px var(--primary-light); }
  50%      { box-shadow: 0 0 0 8px transparent; }
}

.timeline-label {
  font-size: 10.5px;
  font-weight: 600;
  color: var(--text-muted);
  text-align: center;
}

.timeline-step.is-done .timeline-label,
.timeline-step.is-current .timeline-label {
  color: var(--gray-700);
}

.timeline-step.is-current .timeline-label {
  color: var(--primary);
  font-weight: 700;
}

.empty-state { text-align: center; padding: 48px 24px; }
.empty-icon-wrap {
  width: 80px;
  height: 80px;
  margin: 0 auto 14px;
  background: var(--pink-50);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary);
}
.empty-title { font-weight: 700; font-size: 0.95rem; color: var(--gray-900); }
.empty-text { font-size: 12.5px; color: var(--text-secondary); margin-top: 6px; max-width: 320px; margin: 6px auto 0; }

.loading-state { display: flex; flex-direction: column; gap: 12px; }

@media (max-width: 575.98px) {
  .filter-tab { padding: 6px 10px; font-size: 11.5px; }
  .claim-card-header { padding: 10px 12px; }
  .claim-card-body { padding: 12px; }
  .claim-card-footer { padding: 10px 12px; }
}
</style>
