<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { t } from '../../i18n/index.js';
import { formatPrice } from '../../utils/formatPrice.js';
import * as VongQuayService from '../../services/VongQuayService.js';
import Modal from '../common/Modal.vue';
import {
  Triangle,
  Trophy,
  Clover,
  Copy,
  Check,
  ShoppingBag,
  ArrowRight,
  RotateCw,
  Flame,
  Sparkles
} from '@lucide/vue';

// Điểm tích lũy hiện tại của khách hàng
const props = defineProps({
  points: { type: Number, default: 0 },
});
// spun: báo cho AccountPage.vue biết vừa quay xong (kèm điểm còn lại) để cập nhật lại badge.
const emit = defineEmits(['spun']);
const router = useRouter();

const loading = ref(true);
const loadError = ref('');
const diemMoiLuot = ref(0);
// Cố định thứ tự ô vòng quay
const khuyenMaiKhaDung = ref([]);
const spinning = ref(false);
const rotation = ref(0);
const showResultModal = ref(false);
const lastResult = ref(null);
const spinError = ref('');
const copied = ref(false);

const confettiCanvas = ref(null);
let animationFrameId = null;

const sliceCount = computed(() => khuyenMaiKhaDung.value.length + 1); // +1 ô "Chúc may mắn lần sau"
const anglePerSlice = computed(() => 360 / sliceCount.value);

const SLICE_COLORS = ['#f43f5e', '#f59e0b', '#22c55e', '#3b82f6', '#a855f7', '#ec4899'];

const sliceLabel = (index) => {
  if (index === khuyenMaiKhaDung.value.length) {
    // Hiển thị khi không có khuyến mãi
    return khuyenMaiKhaDung.value.length === 0 ? t('wheel.noPrizesSlice') : t('wheel.missSlice');
  }
  const km = khuyenMaiKhaDung.value[index];
  return km.loai === 'percent' ? `-${km.giaTri}%` : `-${formatPrice(km.giaTri)}`;
};

// Góc giữa ô i, chuẩn hoá về [0, 360) — dùng để định vị VÀ để quyết định có cần lật chữ.
const sliceCenterAngle = (i) => (i * anglePerSlice.value + anglePerSlice.value / 2) % 360;

// Tính góc xoay nhãn ô vòng quay
const sliceLabelTransform = (i) => {
  // Căn giữa chữ khi chỉ có 1 ô
  if (sliceCount.value === 1) return '';
  const angle = sliceCenterAngle(i);
  const flip = angle > 90 && angle < 270 ? 180 : 0;
  return `rotate(${angle}deg) translateY(-100px) rotate(${flip}deg)`;
};

const wheelBackground = computed(() => {
  const n = sliceCount.value;
  const stops = [];
  for (let i = 0; i < n; i++) {
    const color = SLICE_COLORS[i % SLICE_COLORS.length];
    stops.push(`${color} ${i * anglePerSlice.value}deg ${(i + 1) * anglePerSlice.value}deg`);
  }
  return `conic-gradient(${stops.join(', ')})`;
});

const canSpin = computed(() => !loading.value && !spinning.value
  && khuyenMaiKhaDung.value.length > 0 && props.points >= diemMoiLuot.value);

const loadConfig = async () => {
  loading.value = true;
  loadError.value = '';
  try {
    const res = await VongQuayService.getCauHinh();
    diemMoiLuot.value = res.diemMoiLuot;
    khuyenMaiKhaDung.value = res.khuyenMaiKhaDung;
  } catch (e) {
    loadError.value = e.message || t('wheel.loadError');
  } finally {
    loading.value = false;
  }
};

onMounted(loadConfig);

// Hiệu ứng pháo hoa confetti ăn mừng
const fireConfetti = () => {
  const canvas = confettiCanvas.value;
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  if (!ctx) return;

  const dpr = window.devicePixelRatio || 1;
  const rect = canvas.getBoundingClientRect();
  const width = rect.width || 460;
  const height = rect.height || 420;
  canvas.width = width * dpr;
  canvas.height = height * dpr;
  ctx.scale(dpr, dpr);

  const colors = ['#f43f5e', '#ec4899', '#f59e0b', '#10b981', '#3b82f6', '#8b5cf6', '#fbbf24', '#ff007f'];
  const particles = Array.from({ length: 65 }, () => ({
    x: width / 2,
    y: 110,
    vx: (Math.random() - 0.5) * 16,
    vy: -Math.random() * 10 - 2,
    size: Math.random() * 7 + 4,
    color: colors[Math.floor(Math.random() * colors.length)],
    rotation: Math.random() * 360,
    rotationSpeed: (Math.random() - 0.5) * 14,
    opacity: 1,
    gravity: 0.32,
    decay: Math.random() * 0.012 + 0.007,
  }));

  const animate = () => {
    ctx.clearRect(0, 0, width, height);
    let alive = false;
    particles.forEach(p => {
      p.x += p.vx;
      p.y += p.vy;
      p.vy += p.gravity;
      p.vx *= 0.98;
      p.rotation += p.rotationSpeed;
      p.opacity -= p.decay;

      if (p.opacity > 0) {
        alive = true;
        ctx.save();
        ctx.translate(p.x, p.y);
        ctx.rotate((p.rotation * Math.PI) / 180);
        ctx.globalAlpha = Math.max(0, p.opacity);
        ctx.fillStyle = p.color;
        ctx.fillRect(-p.size / 2, -p.size / 2, p.size, p.size * 0.6);
        ctx.restore();
      }
    });

    if (alive && showResultModal.value) {
      animationFrameId = requestAnimationFrame(animate);
    }
  };

  if (animationFrameId) cancelAnimationFrame(animationFrameId);
  animate();
};

watch(showResultModal, (val) => {
  if (val && lastResult.value?.ketQua === 'trung') {
    nextTick(() => fireConfetti());
  } else {
    if (animationFrameId) cancelAnimationFrame(animationFrameId);
  }
});

const onSpin = async () => {
  if (!canSpin.value) return;
  spinning.value = true;
  spinError.value = '';
  try {
    const res = await VongQuayService.quay();
    if (!res.ok) throw new Error(await res.text());
    const data = await res.json();
    const targetIndex = data.ketQua === 'truot'
      ? khuyenMaiKhaDung.value.length
      : khuyenMaiKhaDung.value.findIndex(k => k.khuyenMaiId === data.khuyenMai.khuyenMaiId);
    const slice = anglePerSlice.value;
    const targetAngleInCircle = 360 - (targetIndex * slice + slice / 2);
    // Tính góc quay dừng trúng ô kết quả
    rotation.value += 5 * 360 + targetAngleInCircle - (rotation.value % 360);
    lastResult.value = data;
    setTimeout(() => {
      spinning.value = false;
      showResultModal.value = true;
      emit('spun', data.diemConLai);
      if (data.ketQua === 'trung') {
        nextTick(() => fireConfetti());
      }
    }, 4000); // khớp đúng transition 4s ở CSS bên dưới
  } catch (e) {
    spinning.value = false;
    spinError.value = e.message || t('wheel.spinError');
  }
};

const copyCode = async (code) => {
  if (!code) return;
  try {
    await navigator.clipboard.writeText(code);
    copied.value = true;
    setTimeout(() => { copied.value = false; }, 2000);
  } catch {
    // fallback
  }
};

const useNow = async (code) => {
  if (code) await copyCode(code);
  showResultModal.value = false;
  router.push('/');
};

const spinAgain = () => {
  showResultModal.value = false;
  setTimeout(() => {
    if (canSpin.value) {
      onSpin();
    }
  }, 350);
};
</script>

<template>
  <div class="d-flex flex-column align-items-center gap-4 py-4">
    <div v-if="loadError" class="alert alert-danger small">{{ loadError }}</div>
    <template v-else>
      <div class="position-relative" style="width:280px; height:280px;">
        <div class="position-absolute top-0 start-50 translate-middle-x" style="z-index:2; margin-top:-14px;">
          <Triangle :size="22" style="transform:rotate(180deg);" fill="currentColor" class="wheel-pointer" />
        </div>
        <div
          class="rounded-circle position-relative wheel-disk shadow-lg"
          style="width:100%; height:100%; transition:transform 4s cubic-bezier(0.17,0.67,0.12,0.99);"
          :style="{ background: wheelBackground, transform: `rotate(${rotation}deg)` }"
        >
          <div
            v-for="(_, i) in sliceCount" :key="i"
            class="position-absolute top-50 start-50 fw-bold text-white text-center"
            style="width:120px; margin-left:-60px; margin-top:-10px; font-size:12px; text-shadow:0 1px 3px rgba(0,0,0,0.5);"
            :style="{ transform: sliceLabelTransform(i) }"
          >
            {{ sliceLabel(i) }}
          </div>
        </div>
      </div>

      <div class="text-center">
        <div class="small fw-semibold text-secondary">
          {{ t('wheel.costLabel', { points: diemMoiLuot }) }}
        </div>
        <button
          class="btn btn-spin-wheel fw-bold rounded-pill px-5 py-2 mt-2 shadow-sm"
          :disabled="!canSpin"
          @click="onSpin"
        >
          <Sparkles :size="16" class="me-1.5" />
          {{ spinning ? t('wheel.spinning') : t('wheel.spinButton') }}
        </button>
        <div v-if="spinError" class="alert alert-danger small mt-2 mb-0">{{ spinError }}</div>
        <div v-if="!loading && khuyenMaiKhaDung.length === 0" class="small mt-2 text-muted">
          {{ t('wheel.noPrizesAvailable') }}
        </div>
        <div v-else-if="!loading && points < diemMoiLuot" class="small mt-2 text-pink-600 fw-semibold">
          {{ t('wheel.notEnoughPoints') }}
        </div>
      </div>
    </template>

    <!-- ════════════ MODAL THÔNG BÁO KẾT QUẢ ĐẲNG CẤP ════════════ -->
    <Modal v-model="showResultModal" width="460px">
      <!-- Confetti canvas overlay -->
      <canvas
        v-if="lastResult?.ketQua === 'trung'"
        ref="confettiCanvas"
        class="celebrate-confetti-canvas"
      ></canvas>

      <div v-if="lastResult" class="celebrate-modal-container">
        <!-- ════════ THẮNG THƯỞNG (TRÚNG VOUCHER) ════════ -->
        <template v-if="lastResult.ketQua === 'trung'">
          <!-- Radial glowing sunburst -->
          <div class="celebrate-sunburst"></div>

          <!-- Trophy Circle with Stars -->
          <div class="celebrate-icon-wrap">
            <div class="celebrate-trophy-circle">
              <Trophy :size="38" class="celebrate-trophy-icon" />
            </div>
            <span class="celebrate-star star-1">✨</span>
            <span class="celebrate-star star-2">⭐</span>
            <span class="celebrate-star star-3">🎉</span>
          </div>

          <!-- Badge & Heading -->
          <div class="celebrate-badge-pill">
            <Flame :size="13" class="text-amber fill-amber" />
            <span>XIN CHÚC MỪNG BẠN!</span>
          </div>

          <h3 class="celebrate-title">Bạn Đã Trúng Thưởng!</h3>
          <p class="celebrate-desc">
            Phần quà ưu đãi đặc biệt từ vòng quay may mắn đã được gửi vào ví voucher của bạn.
          </p>

          <!-- Golden Ticket Card -->
          <div class="celebrate-ticket">
            <div class="celebrate-ticket-left">
              <div class="celebrate-ticket-val">
                {{ lastResult.khuyenMai.loai === 'percent'
                  ? `${lastResult.khuyenMai.giaTri}%`
                  : formatPrice(lastResult.khuyenMai.giaTri) }}
              </div>
              <div class="celebrate-ticket-tag">GIẢM GIÁ</div>
              <div class="celebrate-notch notch-top"></div>
              <div class="celebrate-notch notch-bottom"></div>
            </div>

            <div class="celebrate-ticket-body">
              <div class="celebrate-ticket-name">
                {{ lastResult.khuyenMai.loai === 'percent'
                  ? `Giảm ${lastResult.khuyenMai.giaTri}% cho đơn hàng tiếp theo`
                  : `Giảm ${formatPrice(lastResult.khuyenMai.giaTri)} cho đơn hàng tiếp theo` }}
              </div>

              <div class="celebrate-ticket-condition">
                <span v-if="lastResult.khuyenMai.donHangToiThieu">
                  Đơn tối thiểu: <strong>{{ formatPrice(lastResult.khuyenMai.donHangToiThieu) }}</strong>
                </span>
                <span v-else>Áp dụng cho mọi đơn hàng</span>
              </div>

              <!-- Monospace Code Box with Copy Button -->
              <div class="celebrate-code-box">
                <div class="d-flex align-items-center gap-1.5 min-w-0">
                  <span class="celebrate-code-lbl">MÃ:</span>
                  <span class="celebrate-code-str">{{ lastResult.phieuGiamGia.maPhieu }}</span>
                </div>
                <button
                  type="button"
                  class="btn-copy-code"
                  :class="{ 'is-copied': copied }"
                  @click="copyCode(lastResult.phieuGiamGia.maPhieu)"
                  title="Sao chép mã"
                >
                  <Check v-if="copied" :size="12" />
                  <Copy v-else :size="12" />
                  <span>{{ copied ? 'Đã chép' : 'Chép' }}</span>
                </button>
              </div>
            </div>
          </div>

          <!-- Action Buttons -->
          <div class="celebrate-actions">
            <button
              type="button"
              class="btn-celebrate-use"
              @click="useNow(lastResult.phieuGiamGia.maPhieu)"
            >
              <ShoppingBag :size="16" />
              <span>Dùng ngay đơn hàng</span>
              <ArrowRight :size="15" />
            </button>

            <div class="celebrate-btn-row">
              <button
                v-if="points >= diemMoiLuot"
                type="button"
                class="btn-celebrate-again"
                @click="spinAgain"
              >
                <RotateCw :size="13" />
                <span>Quay tiếp ({{ diemMoiLuot }} điểm)</span>
              </button>

              <button
                type="button"
                class="btn-celebrate-close"
                @click="showResultModal = false"
              >
                <span>Đóng lại</span>
              </button>
            </div>
          </div>
        </template>

        <!-- ════════ TRƯỢT THƯỞNG (CHÚC MAY MẮN) ════════ -->
        <template v-else>
          <div class="miss-icon-wrap">
            <div class="miss-clover-circle">
              <Clover :size="40" class="miss-clover-icon" />
            </div>
          </div>

          <div class="miss-badge-pill">
            <span>CHÚC BẠN MAY MẮN LẦN SAU</span>
          </div>

          <h3 class="miss-title">Tiếc quá, suýt trúng rồi!</h3>
          <p class="miss-desc">
            Đừng nản lòng nhé! Vận may lớn đang chờ đợi bạn ở lượt quay tiếp theo.
          </p>

          <div class="miss-actions">
            <button
              v-if="points >= diemMoiLuot"
              type="button"
              class="btn-miss-spin"
              @click="spinAgain"
            >
              <RotateCw :size="15" />
              <span>Thử lại ngay ({{ diemMoiLuot }} điểm)</span>
            </button>

            <button
              type="button"
              class="btn-celebrate-close w-100"
              @click="showResultModal = false"
            >
              <span>Để lần sau</span>
            </button>
          </div>
        </template>
      </div>
    </Modal>
  </div>
</template>

<style scoped>
.wheel-pointer {
  color: #db2777;
  filter: drop-shadow(0 2px 4px rgba(219, 39, 119, 0.4));
}
.wheel-disk {
  border: 4px solid #ffffff;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
}
.btn-spin-wheel {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: white;
  border: none;
  font-size: 14px;
  letter-spacing: 0.3px;
  transition: all 0.2s ease;
}
.btn-spin-wheel:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(245, 158, 11, 0.45);
  background: linear-gradient(135deg, #fbbf24 0%, #d97706 100%);
}
.btn-spin-wheel:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ════════════ CELEBRATION MODAL STYLES ════════════ */
.celebrate-confetti-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 10;
  border-radius: 16px;
}

.celebrate-modal-container {
  position: relative;
  text-align: center;
  padding: 8px 4px;
}

/* Sunburst aura */
.celebrate-sunburst {
  position: absolute;
  top: -80px;
  left: 50%;
  transform: translateX(-50%);
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(245, 158, 11, 0.22) 0%, rgba(236, 72, 153, 0.12) 50%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
  animation: pulse-glow 3s infinite ease-in-out;
}
@keyframes pulse-glow {
  0%, 100% { transform: translateX(-50%) scale(0.95); opacity: 0.8; }
  50% { transform: translateX(-50%) scale(1.1); opacity: 1; }
}

/* Trophy Circle & Floating Stars */
.celebrate-icon-wrap {
  position: relative;
  width: 82px;
  height: 82px;
  margin: 0 auto 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.celebrate-trophy-circle {
  width: 78px;
  height: 78px;
  background: linear-gradient(135deg, #f59e0b 0%, #ea580c 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 10px 25px rgba(245, 158, 11, 0.45), 0 0 0 6px rgba(245, 158, 11, 0.2);
  animation: bounce-in 0.6s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}
@keyframes bounce-in {
  0% { transform: scale(0.3); opacity: 0; }
  60% { transform: scale(1.15); opacity: 1; }
  100% { transform: scale(1); }
}
.celebrate-star {
  position: absolute;
  font-size: 18px;
  animation: star-float 2.2s infinite ease-in-out;
}
.star-1 { top: -2px; left: -4px; animation-delay: 0.1s; }
.star-2 { bottom: 2px; right: -4px; animation-delay: 0.6s; }
.star-3 { top: 6px; right: -6px; animation-delay: 1.1s; font-size: 15px; }
@keyframes star-float {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-5px) scale(1.2); }
}

/* Badge Pill */
.celebrate-badge-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  color: #b45309;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.5px;
  padding: 4px 12px;
  border-radius: 9999px;
  margin-bottom: 8px;
}
.celebrate-badge-pill .text-amber { color: #f59e0b; }
.celebrate-badge-pill .fill-amber { fill: #f59e0b; }

.celebrate-title {
  font-size: 22px;
  font-weight: 900;
  color: #111827;
  margin-bottom: 4px;
  letter-spacing: -0.3px;
}
.celebrate-desc {
  font-size: 12.5px;
  color: #6b7280;
  line-height: 1.4;
  margin-bottom: 16px;
  max-width: 380px;
  margin-left: auto;
  margin-right: auto;
}

/* ════════════ VIP GOLDEN TICKET ════════════ */
.celebrate-ticket {
  display: flex;
  background: linear-gradient(135deg, #fffdfa 0%, #fff1f2 100%);
  border: 1.5px dashed #f59e0b;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 18px;
  box-shadow: 0 6px 18px rgba(245, 158, 11, 0.16);
  position: relative;
  text-align: left;
}
.celebrate-ticket-left {
  width: 115px;
  min-width: 115px;
  background: linear-gradient(135deg, #f59e0b 0%, #ea580c 100%);
  color: white;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 14px 6px;
  position: relative;
  text-align: center;
}
.celebrate-ticket-val {
  font-size: 18px;
  font-weight: 900;
  line-height: 1.1;
  word-break: break-word;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.25);
}
.celebrate-ticket-tag {
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.5px;
  opacity: 0.95;
  margin-top: 4px;
}
.celebrate-notch {
  position: absolute;
  width: 14px;
  height: 14px;
  background: #ffffff;
  border-radius: 50%;
  right: -7px;
  z-index: 2;
}
.notch-top { top: -7px; }
.notch-bottom { bottom: -7px; }

.celebrate-ticket-body {
  flex: 1;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
}
.celebrate-ticket-name {
  font-size: 13px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1.3;
}
.celebrate-ticket-condition {
  font-size: 11.5px;
  color: #6b7280;
}
.celebrate-ticket-condition strong {
  color: #111827;
}

.celebrate-code-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: white;
  border: 1px solid #fde68a;
  padding: 5px 8px;
  border-radius: 6px;
  margin-top: 2px;
}
.celebrate-code-lbl {
  font-size: 10px;
  font-weight: 800;
  color: #92400e;
}
.celebrate-code-str {
  font-family: 'SF Mono', 'Fira Code', monospace;
  font-size: 12.5px;
  font-weight: 800;
  color: #d97706;
  letter-spacing: 0.5px;
}
.btn-copy-code {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  background: #fef3c7;
  color: #b45309;
  border: 1px solid #fde68a;
  border-radius: 4px;
  font-size: 10.5px;
  font-weight: 700;
  padding: 2px 7px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn-copy-code:hover {
  background: #fde68a;
}
.btn-copy-code.is-copied {
  background: #ecfdf5;
  color: #059669;
  border-color: #a7f3d0;
}

/* ════════════ CELEBRATE ACTION BUTTONS ════════════ */
.celebrate-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.btn-celebrate-use {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 11px 18px;
  background: linear-gradient(135deg, #ec4899 0%, #db2777 50%, #be185d 100%);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(219, 39, 119, 0.4);
  transition: all 0.2s ease;
}
.btn-celebrate-use:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(219, 39, 119, 0.5);
  background: linear-gradient(135deg, #f472b6 0%, #db2777 50%, #be185d 100%);
}

.celebrate-btn-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.btn-celebrate-again {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 7px 14px;
  border-radius: 9999px;
  background: #fffbeb;
  border: 1px solid #fcd34d;
  color: #b45309;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn-celebrate-again:hover {
  background: #fef3c7;
}
.btn-celebrate-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 7px 16px;
  border-radius: 9999px;
  background: transparent;
  border: 1px solid #e5e7eb;
  color: #6b7280;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn-celebrate-close:hover {
  background: #f3f4f6;
  color: #374151;
}

/* ════════════ MISS STATE ════════════ */
.miss-icon-wrap {
  width: 76px;
  height: 76px;
  margin: 0 auto 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.miss-clover-circle {
  width: 72px;
  height: 72px;
  background: #f3f4f6;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  border: 2px dashed #d1d5db;
  animation: gentle-shake 2.5s infinite ease-in-out;
}
@keyframes gentle-shake {
  0%, 100% { transform: rotate(0deg); }
  25% { transform: rotate(-5deg); }
  75% { transform: rotate(5deg); }
}
.miss-badge-pill {
  display: inline-flex;
  padding: 4px 12px;
  border-radius: 9999px;
  background: #f3f4f6;
  color: #6b7280;
  font-size: 11px;
  font-weight: 700;
  margin-bottom: 8px;
}
.miss-title {
  font-size: 20px;
  font-weight: 800;
  color: #1f2937;
  margin-bottom: 4px;
}
.miss-desc {
  font-size: 12.5px;
  color: #6b7280;
  line-height: 1.4;
  margin-bottom: 16px;
}
.miss-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.btn-miss-spin {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 9px 16px;
  background: var(--pink-500);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn-miss-spin:hover {
  background: var(--pink-600);
}
</style>
