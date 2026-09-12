<script setup>
import { ref, computed, onMounted, watch } from "vue";
import { t } from "../../i18n/index.js";
import { AuthStore } from "../../stores/index.js";
import * as PhieuBaoHanhService from "../../services/PhieuBaoHanhService.js";
import * as HinhAnhBaoHanhService from "../../services/HinhAnhBaoHanhService.js";
import * as BinhLuanBaoHanhService from "../../services/BinhLuanBaoHanhService.js";
import {
  WARRANTY_STATUS,
  STATUS_COLOR,
  canKhachHangCancel,
  daysUntilWarrantyExpiry,
} from "../../services/warrantyConstants.js";
import Skeleton from "../common/Skeleton.vue";
import {
  X, Send, Loader2, Image as ImageIcon, MessageCircle,
  Clock, ShieldCheck, ShieldX, AlertCircle, RefreshCw,
  Calendar, Package, User as UserIcon, Phone, MapPin,
  Wrench, Truck, CheckCircle2, XCircle, ChevronDown, Trash2,
} from '@lucide/vue';

const props = defineProps({
  claimId: { type: [Number, String], required: true },
  isKhachHangView: { type: Boolean, default: true },
});

const emit = defineEmits(["close", "cancelled", "toast", "updated"]);

const auth = AuthStore;

// ── Data ─────────────────────────────────────────────────────────────────
const claim = ref(null);
const images = ref([]);
const comments = ref([]);
const loading = ref(false);
const imagesLoading = ref(false);
const commentsLoading = ref(false);

const fetchDetail = async () => {
  loading.value = true;
  try {
    claim.value = await PhieuBaoHanhService.getById(props.claimId);
  } catch (e) {
    emit("toast", e.message || "Lỗi tải phiếu", "error");
  } finally {
    loading.value = false;
  }
};

const fetchImages = async () => {
  imagesLoading.value = true;
  try {
    images.value = await HinhAnhBaoHanhService.getByBaoHanh(props.claimId);
  } catch {
    images.value = [];
  } finally {
    imagesLoading.value = false;
  }
};

const fetchComments = async () => {
  commentsLoading.value = true;
  try {
    comments.value = await BinhLuanBaoHanhService.getByBaoHanh(props.claimId);
  } catch {
    comments.value = [];
  } finally {
    commentsLoading.value = false;
  }
};

onMounted(() => {
  fetchDetail();
  fetchImages();
  fetchComments();
});

// ── Comment ──────────────────────────────────────────────────────────────
const newComment = ref('');
const sendingComment = ref(false);

const sendComment = async () => {
  const text = newComment.value.trim();
  if (!text) return;
  if (sendingComment.value) return;
  sendingComment.value = true;
  try {
    const res = await BinhLuanBaoHanhService.send(props.claimId, text);
    if (!res.ok) {
      emit("toast", `Gửi thất bại: ${res.status}`, "error");
      return;
    }
    newComment.value = '';
    await fetchComments();
    emit("toast", "Đã gửi bình luận", "success");
  } catch (e) {
    emit("toast", e.message, "error");
  } finally {
    sendingComment.value = false;
  }
};

// ── Cancel ──────────────────────────────────────────────────────────────
const cancelling = ref(false);
const cancelClaim = async () => {
  if (!confirm(`Hủy phiếu #${props.claimId}?`)) return;
  cancelling.value = true;
  try {
    const res = await PhieuBaoHanhService.huyPhieu(props.claimId, 'Khách hàng tự hủy');
    if (res?.ok === false) {
      emit("toast", "Không thể hủy phiếu", "error");
      return;
    }
    emit("toast", "Đã hủy phiếu", "success");
    emit("cancelled");
    emit("close");
  } catch (e) {
    emit("toast", e.message, "error");
  } finally {
    cancelling.value = false;
  }
};

// ── Image viewer ────────────────────────────────────────────────────────
const lightboxImage = ref(null);
const openLightbox = (img) => {
  lightboxImage.value = img;
};
const closeLightbox = () => { lightboxImage.value = null; };

// ── Helpers ─────────────────────────────────────────────────────────────
const statusColor = (s) => STATUS_COLOR[s] || STATUS_COLOR.cho_xu_ly;

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
    da_xu_ly: CheckCircle2,
    tu_choi: XCircle,
    da_huy: XCircle,
  };
  return map[s] || Clock;
};

const formatDateTime = (d) => {
  if (!d) return "—";
  try {
    return new Date(d).toLocaleString('vi-VN', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  } catch { return d; }
};

const formatDate = (d) => {
  if (!d) return "—";
  try {
    return new Date(d).toLocaleDateString('vi-VN');
  } catch { return d; }
};

const formatPrice = (v) => {
  if (v == null) return "—";
  return new Intl.NumberFormat('vi-VN').format(v) + ' đ';
};

// Timeline flow
const TIMELINE_STEPS = [
  { key: 'cho_xu_ly',  label: 'Gửi yêu cầu',  icon: Send },
  { key: 'dang_xu_ly', label: 'Đang xử lý',    icon: Wrench },
  { key: 'da_xu_ly',   label: 'Hoàn thành',    icon: ShieldCheck },
];

const isStepDone = (stepKey) => {
  const s = claim.value?.trangThai;
  if (s === 'tu_choi' || s === 'da_huy') return false;
  if (s === 'cho_xu_ly')  return stepKey === 'cho_xu_ly';
  if (s === 'dang_xu_ly') return stepKey === 'cho_xu_ly' || stepKey === 'dang_xu_ly';
  if (s === 'da_xu_ly')   return true;
  return false;
};

const isStepCurrent = (stepKey) => {
  const s = claim.value?.trangThai;
  if (s === 'tu_choi' || s === 'da_huy') return false;
  if (s === 'cho_xu_ly' && stepKey === 'cho_xu_ly')   return true;
  if (s === 'dang_xu_ly' && stepKey === 'dang_xu_ly') return true;
  if (s === 'da_xu_ly' && stepKey === 'da_xu_ly')     return true;
  return false;
};

const stepDate = (stepKey) => {
  if (!claim.value) return null;
  if (stepKey === 'cho_xu_ly')  return claim.value.ngayTiepNhan || claim.value.ngayMua || claim.value.createdAt;
  if (stepKey === 'dang_xu_ly') return claim.value.ngayBatDauXuLy || claim.value.ngayTiepNhan;
  if (stepKey === 'da_xu_ly')   return claim.value.ngayTraKhach;
  return null;
};

const handleBackdropClick = (e) => {
  if (e.target === e.currentTarget) emit("close");
};
</script>

<template>
  <div class="detail-modal-backdrop" @click="handleBackdropClick">
    <div class="detail-modal">
      <!-- Header -->
      <div class="detail-modal-header">
        <div class="detail-modal-title">
          <Wrench :size="20" />
          <div>
            <div class="modal-title-main">Phiếu bảo hành #{{ claimId }}</div>
            <div v-if="claim" class="modal-title-sub">{{ claim.tenSanPham }}</div>
          </div>
        </div>
        <button class="modal-close" @click="emit('close')"><X :size="18" /></button>
      </div>

      <!-- Body -->
      <div class="detail-modal-body">
        <div v-if="loading" class="loading-state">
          <Skeleton v-for="i in 4" :key="i" width="100%" height="60px" radius="10px" />
        </div>

        <template v-else-if="claim">
          <!-- Status banner -->
          <div
            class="status-banner"
            :style="{ background: statusColor(claim.trangThai).bg, borderColor: statusColor(claim.trangThai).dot }"
          >
            <component :is="statusIcon(claim.trangThai)" :size="22" :style="{ color: statusColor(claim.trangThai).text }" />
            <div class="status-banner-content">
              <div class="status-banner-label" :style="{ color: statusColor(claim.trangThai).text }">
                {{ statusLabel(claim.trangThai) }}
              </div>
              <div class="status-banner-date">
                {{ claim.ngayTraKhach ? `Hoàn thành: ${formatDateTime(claim.ngayTraKhach)}` :
                   claim.ngayBatDauXuLy ? `Bắt đầu: ${formatDateTime(claim.ngayBatDauXuLy)}` :
                   `Gửi yêu cầu: ${formatDateTime(claim.ngayMua)}` }}
              </div>
              <div v-if="claim.lyDoTuChoi" class="status-banner-reason">
                <strong>Lý do từ chối:</strong> {{ claim.lyDoTuChoi }}
              </div>
            </div>
          </div>

          <!-- Timeline -->
          <div v-if="!['tu_choi', 'da_huy'].includes(claim.trangThai)" class="timeline-section">
            <div class="timeline-flow">
              <div
                v-for="(step, idx) in TIMELINE_STEPS"
                :key="step.key"
                class="flow-step"
                :class="{
                  'is-done': isStepDone(step.key),
                  'is-current': isStepCurrent(step.key),
                }"
              >
                <div class="flow-icon">
                  <component :is="step.icon" :size="16" />
                </div>
                <div class="flow-label">{{ step.label }}</div>
                <div v-if="stepDate(step.key)" class="flow-date">{{ formatDate(stepDate(step.key)) }}</div>
                <div v-else-if="isStepCurrent(step.key)" class="flow-date flow-date--current">Đang thực hiện</div>
                <div v-else class="flow-date flow-date--pending">—</div>
                <div v-if="idx < TIMELINE_STEPS.length - 1" class="flow-line" :class="{ 'is-done': isStepDone(TIMELINE_STEPS[idx + 1].key) }"></div>
              </div>
            </div>
          </div>

          <!-- Product info -->
          <div class="info-section">
            <h4 class="info-title"><Package :size="16" /> Thông tin sản phẩm</h4>
            <div class="info-grid">
              <div class="info-item">
                <span class="info-label">Sản phẩm</span>
                <span class="info-value">{{ claim.tenSanPham || '—' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">Mã SKU</span>
                <span class="info-value info-mono">{{ claim.maSku || '—' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">Serial</span>
                <span class="info-value info-mono">{{ claim.soSerial || '—' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">Mã đơn hàng</span>
                <span class="info-value info-mono">#{{ claim.donHangId || '—' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">Ngày mua</span>
                <span class="info-value">{{ formatDate(claim.ngayMua) }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">Hết hạn BH</span>
                <span class="info-value">{{ formatDate(claim.ngayHetBh) }}</span>
              </div>
            </div>
          </div>

          <!-- Issue + result -->
          <div class="info-section">
            <h4 class="info-title"><AlertCircle :size="16" /> Mô tả lỗi & Kết quả</h4>
            <div class="issue-block">
              <div class="issue-label">Bạn đã mô tả:</div>
              <div class="issue-text">{{ claim.moTaLoi || '—' }}</div>
            </div>
            <div v-if="claim.ketQuaXuLy" class="issue-block issue-block--success">
              <div class="issue-label">Kết quả xử lý từ SAOPhone:</div>
              <div class="issue-text">{{ claim.ketQuaXuLy }}</div>
            </div>
            <div v-if="claim.chiPhiPhatSinh > 0" class="cost-row">
              <span>Chi phí phát sinh:</span>
              <strong class="cost-value">{{ formatPrice(claim.chiPhiPhatSinh) }}</strong>
            </div>
            <div v-if="claim.ghiChu" class="note-row">
              <span class="note-label">Ghi chú:</span>
              <span>{{ claim.ghiChu }}</span>
            </div>
          </div>

          <!-- Images -->
          <div class="info-section">
            <h4 class="info-title">
              <ImageIcon :size="16" />
              Ảnh / Video minh chứng
              <span v-if="images.length" class="count-pill">{{ images.length }}</span>
            </h4>

            <div v-if="imagesLoading" class="loading-state">
              <Skeleton width="100%" height="80px" radius="10px" />
            </div>
            <div v-else-if="images.length === 0" class="no-media">
              Không có ảnh/video đính kèm
            </div>
            <div v-else class="image-grid">
              <div
                v-for="img in images"
                :key="img.hinhAnhId"
                class="image-thumb"
                @click="openLightbox(img)"
              >
                <img v-if="img.loai === 'image'" :src="img.url" :alt="img.tenFile" />
                <video v-else :src="img.url" muted></video>
                <span class="image-type">
                  <template v-if="img.loai === 'image'"><ImageIcon :size="11" /> Ảnh</template>
                  <template v-else>🎬 Video</template>
                </span>
              </div>
            </div>
          </div>

          <!-- Comments -->
          <div class="info-section">
            <h4 class="info-title">
              <MessageCircle :size="16" />
              Trao đổi với nhân viên
              <span v-if="comments.length" class="count-pill">{{ comments.length }}</span>
            </h4>

            <div v-if="commentsLoading" class="loading-state">
              <Skeleton width="100%" height="40px" radius="8px" />
            </div>
            <div v-else-if="comments.length === 0" class="no-media">
              Chưa có trao đổi nào. Bạn có thể gửi thêm thông tin cho nhân viên ở dưới.
            </div>
            <div v-else class="comments-list">
              <div
                v-for="c in comments"
                :key="c.binhLuanId"
                class="comment"
                :class="{ 'comment--staff': c.vaiTro === 'nhan_vien' || c.vaiTro === 'admin' }"
              >
                <div class="comment-avatar">
                  {{ (c.tenNguoiGui || 'U').charAt(0).toUpperCase() }}
                </div>
                <div class="comment-body">
                  <div class="comment-meta">
                    <strong>{{ c.tenNguoiGui || 'Người dùng' }}</strong>
                    <span v-if="c.vaiTro" class="role-pill" :class="`role-pill--${c.vaiTro}`">
                      {{ c.vaiTro === 'khach_hang' ? 'Khách hàng' :
                         c.vaiTro === 'nhan_vien' ? 'Nhân viên' :
                         c.vaiTro === 'admin' ? 'Quản trị viên' : c.vaiTro }}
                    </span>
                    <span class="comment-time">{{ formatDateTime(c.ngayGui) }}</span>
                  </div>
                  <div class="comment-text">{{ c.noiDung }}</div>
                </div>
              </div>
            </div>

            <!-- New comment -->
            <form
              v-if="!['da_xu_ly', 'tu_choi', 'da_huy'].includes(claim.trangThai)"
              class="comment-form"
              @submit.prevent="sendComment"
            >
              <textarea
                v-model="newComment"
                rows="2"
                placeholder="Nhập nội dung trao đổi..."
                class="comment-textarea"
                maxlength="500"
              />
              <button type="submit" class="btn-send-comment" :disabled="!newComment.trim() || sendingComment">
                <Loader2 v-if="sendingComment" :size="14" class="spin" />
                <Send v-else :size="14" />
                Gửi
              </button>
            </form>
          </div>
        </template>

        <div v-else class="empty-state">
          <div class="empty-icon-wrap"><AlertCircle :size="40" /></div>
          <div class="empty-title">Không tìm thấy phiếu</div>
        </div>
      </div>

      <!-- Footer -->
      <div v-if="claim" class="detail-modal-footer">
        <button
          v-if="canKhachHangCancel(claim.trangThai)"
          class="btn-modal btn-modal--danger"
          @click="cancelClaim"
          :disabled="cancelling"
        >
          <Loader2 v-if="cancelling" :size="14" class="spin" />
          <Trash2 v-else :size="14" />
          Hủy phiếu
        </button>
        <button class="btn-modal btn-modal--ghost" @click="emit('close')">Đóng</button>
      </div>
    </div>

    <!-- Lightbox -->
    <Transition name="lightbox-fade">
      <div v-if="lightboxImage" class="lightbox" @click="closeLightbox">
        <button class="lightbox-close"><X :size="20" /></button>
        <img v-if="lightboxImage.loai === 'image'" :src="lightboxImage.url" />
        <video v-else :src="lightboxImage.url" controls autoplay></video>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.detail-modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(31, 41, 55, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1100;
  padding: 20px;
  backdrop-filter: blur(4px);
}

.detail-modal {
  background: var(--bg-card);
  border-radius: 16px;
  width: 100%;
  max-width: 680px;
  max-height: 92vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
}

.detail-modal-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(180deg, var(--pink-50) 0%, var(--bg-card) 100%);
}

.detail-modal-title {
  display: flex;
  gap: 10px;
  align-items: center;
  color: var(--primary);
}

.modal-title-main {
  font-weight: 800;
  font-size: 1rem;
  color: var(--gray-900);
}

.modal-title-sub {
  font-size: 12.5px;
  color: var(--text-secondary);
  margin-top: 2px;
  font-weight: 500;
}

.modal-close {
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

.modal-close:hover {
  background: var(--gray-100);
  color: var(--gray-900);
}

.detail-modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-modal-footer {
  padding: 14px 20px;
  border-top: 1px solid var(--border);
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  background: var(--gray-50);
}

/* Status banner */
.status-banner {
  display: flex;
  gap: 14px;
  padding: 16px;
  border-radius: 12px;
  border-left: 4px solid;
  align-items: flex-start;
}

.status-banner-content { flex: 1; }

.status-banner-label {
  font-weight: 800;
  font-size: 1.05rem;
  line-height: 1.2;
}

.status-banner-date {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.status-banner-reason {
  font-size: 12.5px;
  color: var(--gray-700);
  margin-top: 8px;
  padding: 8px 10px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 8px;
}

/* Timeline */
.timeline-section {
  padding: 16px 0;
}

.timeline-flow {
  display: flex;
  align-items: flex-start;
  position: relative;
}

.flow-step {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  text-align: center;
}

.flow-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--gray-100);
  color: var(--text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--border);
  z-index: 1;
  transition: all 0.3s ease;
}

.flow-step.is-done .flow-icon {
  background: var(--success);
  color: white;
  border-color: var(--success);
}

.flow-step.is-current .flow-icon {
  background: var(--primary);
  color: white;
  border-color: var(--primary);
  box-shadow: 0 0 0 4px var(--primary-light);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 4px var(--primary-light); }
  50%      { box-shadow: 0 0 0 8px transparent; }
}

.flow-label {
  margin-top: 8px;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-muted);
}

.flow-step.is-done .flow-label,
.flow-step.is-current .flow-label {
  color: var(--gray-900);
}

.flow-step.is-current .flow-label {
  color: var(--primary);
}

.flow-date {
  font-size: 10.5px;
  color: var(--text-muted);
  margin-top: 2px;
}

.flow-date--current {
  color: var(--primary);
  font-weight: 700;
}

.flow-date--pending { opacity: 0.5; }

.flow-line {
  position: absolute;
  top: 17px;
  left: 50%;
  right: -50%;
  height: 2px;
  background: var(--border);
  z-index: 0;
}

.flow-line.is-done {
  background: var(--success);
}

/* Info sections */
.info-section {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px 16px;
}

.info-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13.5px;
  font-weight: 700;
  color: var(--gray-900);
  margin-bottom: 12px;
}

.info-title svg { color: var(--primary); }

.count-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  background: var(--primary-light);
  color: var(--primary);
  border-radius: 9999px;
  font-size: 10.5px;
  font-weight: 700;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.info-label {
  font-size: 11px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  font-weight: 600;
}

.info-value {
  font-size: 13px;
  color: var(--gray-900);
  font-weight: 600;
}

.info-mono {
  font-family: 'SF Mono', monospace;
  font-size: 12px;
}

.issue-block {
  background: var(--gray-50);
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 10px;
}

.issue-block--success {
  background: var(--success-light);
  border-left: 3px solid var(--success);
}

.issue-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.issue-text {
  font-size: 13px;
  color: var(--gray-800);
  line-height: 1.55;
  white-space: pre-wrap;
}

.cost-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 12px;
  background: var(--primary-light);
  border-radius: 8px;
  font-size: 13px;
}

.cost-value {
  color: var(--primary);
  font-size: 14px;
}

.note-row {
  display: flex;
  gap: 8px;
  padding: 10px 12px;
  background: var(--gray-50);
  border-radius: 8px;
  font-size: 12.5px;
  color: var(--text-secondary);
  margin-top: 8px;
}

.note-label {
  font-weight: 600;
  color: var(--gray-700);
  flex-shrink: 0;
}

.no-media {
  text-align: center;
  padding: 16px;
  font-size: 12.5px;
  color: var(--text-muted);
  background: var(--gray-50);
  border-radius: 8px;
}

/* Images */
.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(90px, 1fr));
  gap: 8px;
}

.image-thumb {
  position: relative;
  aspect-ratio: 1;
  background: var(--gray-100);
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--border);
  transition: all 0.2s ease;
}

.image-thumb:hover {
  border-color: var(--primary);
  transform: scale(1.05);
}

.image-thumb img,
.image-thumb video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-type {
  position: absolute;
  bottom: 4px;
  left: 4px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  padding: 2px 6px;
  border-radius: 9999px;
  font-size: 9.5px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

/* Comments */
.comments-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 12px;
}

.comment {
  display: flex;
  gap: 10px;
}

.comment-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--gray-200);
  color: var(--gray-700);
  font-weight: 700;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.comment--staff .comment-avatar {
  background: var(--primary);
  color: white;
}

.comment-body {
  flex: 1;
  background: var(--gray-50);
  border-radius: 12px;
  padding: 10px 12px;
  min-width: 0;
}

.comment--staff .comment-body {
  background: var(--primary-light);
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}

.comment-meta strong {
  font-size: 12.5px;
  color: var(--gray-900);
}

.role-pill {
  padding: 1px 7px;
  border-radius: 9999px;
  font-size: 9.5px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.role-pill--khach_hang {
  background: var(--gray-200);
  color: var(--gray-700);
}

.role-pill--nhan_vien,
.role-pill--admin {
  background: var(--primary);
  color: white;
}

.comment-time {
  font-size: 10.5px;
  color: var(--text-muted);
  margin-left: auto;
}

.comment-text {
  font-size: 13px;
  color: var(--gray-700);
  line-height: 1.5;
  white-space: pre-wrap;
}

.comment-form {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}

.comment-textarea {
  flex: 1;
  padding: 8px 12px;
  background: var(--gray-50);
  border: 1px solid var(--border);
  border-radius: 10px;
  color: var(--text-primary);
  font-size: 13px;
  font-family: inherit;
  resize: vertical;
  min-height: 40px;
  transition: all 0.2s ease;
}

.comment-textarea:focus {
  outline: none;
  background: var(--bg-card);
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
}

.btn-send-comment {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 8px 14px;
  background: var(--primary);
  color: white;
  border: none;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-send-comment:hover:not(:disabled) {
  background: var(--pink-700);
}

.btn-send-comment:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Buttons */
.btn-modal {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 9px 18px;
  border: none;
  border-radius: 9999px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-modal--ghost {
  background: var(--bg-card);
  color: var(--text-secondary);
  border: 1px solid var(--border);
}

.btn-modal--ghost:hover { background: var(--gray-100); }

.btn-modal--danger {
  background: var(--bg-card);
  color: var(--primary);
  border: 1px solid var(--primary);
}

.btn-modal--danger:hover:not(:disabled) { background: var(--primary-light); }

.btn-modal:disabled { opacity: 0.5; cursor: not-allowed; }

.spin { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

.empty-state { text-align: center; padding: 40px 24px; }
.empty-icon-wrap {
  width: 70px;
  height: 70px;
  margin: 0 auto 12px;
  background: var(--pink-50);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary);
}

.loading-state { display: flex; flex-direction: column; gap: 10px; }

/* Lightbox */
.lightbox {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1200;
  padding: 30px;
}

.lightbox img, .lightbox video {
  max-width: 90vw;
  max-height: 90vh;
  object-fit: contain;
}

.lightbox-close {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 50%;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.lightbox-close:hover { background: rgba(255, 255, 255, 0.25); }

.lightbox-fade-enter-active, .lightbox-fade-leave-active {
  transition: opacity 0.2s ease;
}
.lightbox-fade-enter-from, .lightbox-fade-leave-to { opacity: 0; }

@media (max-width: 575.98px) {
  .info-grid { grid-template-columns: 1fr; }
  .comment-meta { gap: 4px; }
  .comment-time { margin-left: 0; flex-basis: 100%; }
}
</style>
