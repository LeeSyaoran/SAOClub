<template>
  <!-- Timeline dọc chi tiết đơn hàng (5 bước cho COD, 7 bước cho đơn thanh toán QR) -->
  <div class="d-flex flex-column gap-0" style="position:relative;">
    <div
      v-for="(step, index) in steps" :key="step.id || index"
      class="d-flex align-items-start gap-3 position-relative"
    >
      <!-- Đường nối dọc giữa các step -->
      <div
        v-if="index < steps.length - 1"
        style="position:absolute; left:15px; top:28px; bottom:-2px; width:2px; z-index:0;"
        :style="isStepCompleted(steps[index+1], index+1) || isStepActive(steps[index+1], index+1) ? 'background:var(--accent);' : 'background:var(--border-color-strong); opacity:0.4;'"
      ></div>

      <!-- Cột icon tròn -->
      <div class="d-flex flex-column align-items-center" style="width:32px; flex-shrink:0; position:relative; z-index:1;">
        <div
          class="rounded-circle d-flex align-items-center justify-content-center position-relative"
          style="width:30px; height:30px; z-index:1;"
          :style="isStepCompleted(step, index)
            ? 'background:var(--accent); border:2px solid var(--accent);'
            : isStepActive(step, index)
              ? 'background:var(--bg-hover); border:2px solid var(--accent); box-shadow:0 0 0 3px rgba(244,63,94,0.18);'
              : 'background:var(--bg-card-alt); border:2px solid var(--border-color-strong);'"
        >
          <Check v-if="isStepCompleted(step, index)" :size="14" color="white" />
          <component v-else :is="step.icon" :size="13" :style="{ opacity: isStepActive(step, index) ? 1 : 0.4, color: isStepActive(step, index) ? 'var(--accent-fg)' : 'inherit' }" />
        </div>
      </div>

      <!-- Label + mô tả -->
      <div class="flex-grow-1 pb-2.5" style="padding-top:3px; position:relative; z-index:1;">
        <div
          class="fw-semibold" style="font-size:0.83rem; line-height:1.3;"
          :style="isStepActive(step, index)
            ? 'color:var(--accent-fg); font-weight:700;'
            : isStepCompleted(step, index) ? 'color:var(--text-primary);' : 'color:var(--text-secondary);'"
        >
          {{ step.title }}
        </div>
        <div style="font-size:0.71rem; color:var(--text-muted); line-height:1.35; margin-top:2px;">
          {{ step.desc }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { t } from '../../i18n/index.js';
import { Check, PartyPopper } from '@lucide/vue';
import {
  isQrPayment, QR_TIMELINE_STEPS, isQrStepDone, isQrStepCurrent, isQrStepNext, isQrStepReached,
  COD_TIMELINE_STEPS, isCodStepDone, isCodStepCurrent, isCodStepNext, isCodStepReached
} from '../../utils/orderStatus.js';

// Danh sách bước xử lý đơn hàng theo trạng thái
const props = defineProps({
  status:    { type: String, default: 'pending' },
  kenhBan:   { type: String, default: 'online' },
  order:     { type: Object, default: null },
});

const isInStore = computed(() => props.kenhBan === 'in_store');
const isQr = computed(() => props.order && isQrPayment(props.order));

const steps = computed(() => {
  // Đơn tại quầy
  if (isInStore.value) {
    return [
      { title: t('orderStatus.timeline.deliveredTitle'), desc: t('orderStatus.timeline.inStoreDeliveredDesc') || t('orderStatus.timeline.deliveredDesc'), icon: PartyPopper },
    ];
  }
  // Đơn thanh toán qua mã QR (7 bước)
  if (isQr.value) {
    return QR_TIMELINE_STEPS;
  }
  // Đơn thanh toán sau (COD) (5 bước)
  return COD_TIMELINE_STEPS;
});

const isStepCompleted = (step, index) => {
  if (isInStore.value) return props.status === 'delivered';
  if (isQr.value) return isQrStepDone(props.order, step.id);
  return isCodStepDone(props.order, step.id);
};

const isStepActive = (step, index) => {
  if (isInStore.value) return false;
  if (isQr.value) return isQrStepNext(props.order, step.id);
  return isCodStepNext(props.order, step.id);
};

const isStepReachedIdx = (step, index) => {
  if (isInStore.value) return true;
  if (isQr.value) return isQrStepReached(props.order, step.id);
  return isCodStepReached(props.order, step.id);
};
</script>
