<script setup>
import { ref, computed, onMounted } from 'vue';
import { t } from '../../i18n/index.js';
import { formatPrice } from '../../utils/formatPrice.js';
import * as VongQuayService from '../../Service/VongQuayService.js';
import Modal from '../common/Modal.vue';

// points: điểm tích lũy hiện tại của khách — nhận từ AccountPage.vue (đã load sẵn cho
// badge điểm ở header), không tự fetch profile riêng trong component này.
const props = defineProps({
  points: { type: Number, default: 0 },
});
// spun: báo cho AccountPage.vue biết vừa quay xong (kèm điểm còn lại) để cập nhật lại badge.
const emit = defineEmits(['spun']);

const loading = ref(true);
const loadError = ref('');
const diemMoiLuot = ref(0);
// Cố định thứ tự ô sau khi load — không refetch giữa các lượt quay, tránh lệch chỉ số ô
// so với animate lúc component đã render.
const khuyenMaiKhaDung = ref([]);
const spinning = ref(false);
const rotation = ref(0);
const showResultModal = ref(false);
const lastResult = ref(null);
const spinError = ref('');

const sliceCount = computed(() => khuyenMaiKhaDung.value.length + 1); // +1 ô "Chúc may mắn lần sau"
const anglePerSlice = computed(() => 360 / sliceCount.value);

const SLICE_COLORS = ['#f43f5e', '#f59e0b', '#22c55e', '#3b82f6', '#a855f7', '#ec4899'];

const sliceLabel = (index) => {
  if (index === khuyenMaiKhaDung.value.length) {
    // Ô duy nhất khi không có khuyến mãi nào (không phải "trượt" giữa nhiều lựa chọn thật)
    // — chữ khác với ô "trượt" thông thường để không gây hiểu lầm là còn cơ hội trúng.
    return khuyenMaiKhaDung.value.length === 0 ? t('wheel.noPrizesSlice') : t('wheel.missSlice');
  }
  const km = khuyenMaiKhaDung.value[index];
  return km.loai === 'percent' ? `-${km.giaTri}%` : `-${formatPrice(km.giaTri)}`;
};

// Góc giữa ô i, chuẩn hoá về [0, 360) — dùng để định vị VÀ để quyết định có cần lật chữ.
const sliceCenterAngle = (i) => (i * anglePerSlice.value + anglePerSlice.value / 2) % 360;

// Ô nằm ở nửa dưới bánh xe (góc 90°-270°) sẽ khiến chữ bị xoay lộn ngược nếu chỉ xoay
// theo đúng góc định vị — xoay thêm 180° tại chỗ (không đổi vị trí) để chữ luôn đọc được.
const sliceLabelTransform = (i) => {
  // Chỉ 1 ô (không có khuyến mãi nào) = cả vòng tròn — chữ nằm đúng giữa tâm, không dịch
  // ra rìa như khi chia nhiều ô thật.
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
    // Quay thêm 5 vòng trọn rồi dừng đúng giữa ô targetIndex — trừ phần dư hiện tại để luôn
    // quay THEO CHIỀU THUẬN, không giật ngược khi rotation hiện tại lệch pha.
    rotation.value += 5 * 360 + targetAngleInCircle - (rotation.value % 360);
    lastResult.value = data;
    setTimeout(() => {
      spinning.value = false;
      showResultModal.value = true;
      emit('spun', data.diemConLai);
    }, 4000); // khớp đúng transition 4s ở CSS bên dưới
  } catch (e) {
    spinning.value = false;
    spinError.value = e.message || t('wheel.spinError');
  }
};
</script>

<template>
  <div class="d-flex flex-column align-items-center gap-4 py-4">
    <div v-if="loadError" class="alert alert-danger small">{{ loadError }}</div>
    <template v-else>
      <div class="position-relative" style="width:280px; height:280px;">
        <div class="position-absolute top-0 start-50 translate-middle-x" style="z-index:2; font-size:28px; margin-top:-14px;">🔻</div>
        <div class="rounded-circle position-relative"
             style="width:100%; height:100%; transition:transform 4s cubic-bezier(0.17,0.67,0.12,0.99);"
             :style="{ background: wheelBackground, transform: `rotate(${rotation}deg)` }">
          <div v-for="(_, i) in sliceCount" :key="i"
               class="position-absolute top-50 start-50 fw-bold text-white text-center"
               style="width:120px; margin-left:-60px; margin-top:-10px; font-size:12px; text-shadow:0 1px 3px rgba(0,0,0,0.5);"
               :style="{ transform: sliceLabelTransform(i) }">
            {{ sliceLabel(i) }}
          </div>
        </div>
      </div>

      <div class="text-center">
        <div class="small" style="color:var(--text-secondary);">{{ t('wheel.costLabel', { points: diemMoiLuot }) }}</div>
        <button class="btn btn-warning fw-bold rounded-pill px-4 mt-2"
                :disabled="!canSpin"
                @click="onSpin">
          {{ spinning ? t('wheel.spinning') : t('wheel.spinButton') }}
        </button>
        <div v-if="spinError" class="alert alert-danger small mt-2 mb-0">{{ spinError }}</div>
        <div v-if="!loading && khuyenMaiKhaDung.length === 0" class="small mt-2" style="color:var(--text-secondary);">
          {{ t('wheel.noPrizesAvailable') }}
        </div>
        <div v-else-if="!loading && points < diemMoiLuot" class="small mt-2" style="color:var(--text-secondary);">
          {{ t('wheel.notEnoughPoints') }}
        </div>
      </div>
    </template>

    <Modal v-model="showResultModal" width="380px">
      <div v-if="lastResult" class="text-center">
        <template v-if="lastResult.ketQua === 'trung'">
          <div style="font-size:2.4rem;">🎉</div>
          <h5 class="fw-black mt-2" style="color:var(--text-heading);">{{ t('wheel.winTitle') }}</h5>
          <p class="mb-1" style="color:var(--text-primary);">
            {{ lastResult.khuyenMai.loai === 'percent'
                ? t('wheel.winPercent', { value: lastResult.khuyenMai.giaTri })
                : t('wheel.winFixed', { value: formatPrice(lastResult.khuyenMai.giaTri) }) }}
          </p>
          <div class="small" style="color:var(--text-secondary);">{{ t('wheel.winCode', { code: lastResult.phieuGiamGia.maPhieu }) }}</div>
        </template>
        <template v-else>
          <div style="font-size:2.4rem;">🍀</div>
          <h5 class="fw-black mt-2" style="color:var(--text-heading);">{{ t('wheel.missTitle') }}</h5>
          <p class="small" style="color:var(--text-secondary);">{{ t('wheel.missDesc') }}</p>
        </template>
        <button class="btn btn-sm btn-outline-secondary rounded-pill px-4 mt-3" @click="showResultModal = false">
          {{ t('common.close') }}
        </button>
      </div>
    </Modal>
  </div>
</template>
