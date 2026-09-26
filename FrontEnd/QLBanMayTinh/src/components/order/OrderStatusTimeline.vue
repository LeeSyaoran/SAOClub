<template>
  <!-- Timeline dọc chi tiết đơn hàng (hỗ trợ cả 8 bước cho đơn thanh toán QR) -->
  <div class="d-flex flex-column gap-0" style="position:relative;">
    <div
      v-for="(step, index) in steps" :key="step.id || index"
      class="d-flex align-items-start gap-3" style="position:relative;"
    >
      <!-- Cột icon + đường nối dọc giữa các step -->
      <div class="d-flex flex-column align-items-center" style="width:32px; flex-shrink:0; position:relative;">
        <div
          class="rounded-circle d-flex align-items-center justify-content-center position-relative"
          style="width:30px; height:30px;"
          :style="isStepCompleted(step, index)
            ? 'background:var(--accent); border:2px solid var(--accent);'
            : isStepActive(step, index)
              ? 'background:var(--bg-hover); border:2px solid var(--accent); box-shadow:0 0 0 3px rgba(244,63,94,0.18);'
              : 'background:var(--bg-card-alt); border:2px solid var(--border-color-strong);'"
        >
          <Check v-if="isStepCompleted(step, index)" :size="14" color="white" />
          <component v-else :is="step.icon" :size="13" :style="{ opacity: isStepReachedIdx(step, index) ? 1 : 0.4 }" />
        </div>
        <div
          v-if="index < steps.length - 1" style="width:2px; flex-grow:1; min-height:18px; margin-top:4px;"
          :style="isStepReachedIdx(steps[index+1], index+1) ? 'background:var(--accent);' : 'background:var(--border-color-strong); opacity:0.4;'"
        ></div>
      </div>

      <!-- Label + mô tả -->
      <div class="flex-grow-1 pb-2.5" style="padding-top:3px;">
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
import { Check, Send, Bike, PartyPopper, FileText, CheckCircle2, Package, Clock, CreditCard, FileCheck, Inbox } from '@lucide/vue';
import { isQrPayment, QR_TIMELINE_STEPS, isQrStepDone, isQrStepCurrent, isQrStepReached } from '../../utils/orderStatus.js';

// Danh sách bước xử lý đơn hàng theo trạng thái
const props = defineProps({
  status:    { type: String, default: 'pending' },
  kenhBan:   { type: String, default: 'online' },
  order:     { type: Object, default: null },
});

const PRE_SHIP  = ['pending', 'confirmed', 'processing'];
const POST_SHIP = ['out_for_delivery', 'awaiting_confirmation'];

const isInStore = computed(() => props.kenhBan === 'in_store');
const isQr = computed(() => props.order && isQrPayment(props.order));
const isPostShip = computed(() => POST_SHIP.includes(props.status) || props.status === 'shipping');
const isAwaitingConfirmation = computed(() => props.status === 'awaiting_confirmation');

const steps = computed(() => {
  // Đơn tại quầy
  if (isInStore.value) {
    return [
      { title: t('orderStatus.timeline.deliveredTitle'), desc: t('orderStatus.timeline.inStoreDeliveredDesc') || t('orderStatus.timeline.deliveredDesc'), icon: PartyPopper },
    ];
  }
  // Đơn thanh toán qua mã QR (8 bước)
  if (isQr.value) {
    return QR_TIMELINE_STEPS;
  }
  // Đơn thông thường
  return isPostShip.value ? [
    { title: t('orderStatus.timeline.outForDeliveryTitle'),  desc: t('orderStatus.timeline.outForDeliveryDesc'),  icon: Bike },
    { title: t('orderStatus.timeline.deliveredTitle'),       desc: t('orderStatus.timeline.deliveredDesc'),       icon: PartyPopper },
  ] : [
    { title: t('orderStatus.timeline.placedTitle'),    desc: t('orderStatus.timeline.placedDesc'),    icon: Clock },
    { title: t('orderStatus.timeline.confirmedTitle'), desc: t('orderStatus.timeline.confirmedDesc'), icon: CheckCircle2 },
    { title: t('orderStatus.timeline.packingTitle'),   desc: t('orderStatus.timeline.packingDesc'),   icon: Package },
  ];
});

const currentStep = computed(() => {
  if (isInStore.value) return 0;
  if (isQr.value) return -1;
  const list = isPostShip.value ? POST_SHIP : PRE_SHIP;
  let status = props.status;
  if (status === 'shipping') status = 'out_for_delivery';
  const idx = list.indexOf(status);
  return idx === -1 ? list.length - 1 : idx;
});

const isStepCompleted = (step, index) => {
  if (isInStore.value) return props.status === 'delivered';
  if (isQr.value) return isQrStepDone(props.order, step.id);
  return index < currentStep.value || (index === currentStep.value && isAwaitingConfirmation.value);
};

const isStepActive = (step, index) => {
  if (isInStore.value) return props.status === 'delivered';
  if (isQr.value) return isQrStepCurrent(props.order, step.id);
  return index === currentStep.value;
};

const isStepReachedIdx = (step, index) => {
  if (isInStore.value) return true;
  if (isQr.value) return isQrStepReached(props.order, step.id);
  return index <= currentStep.value;
};
</script>
