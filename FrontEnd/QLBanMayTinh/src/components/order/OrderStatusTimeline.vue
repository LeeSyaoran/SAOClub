<template>
  <!-- Bố cục đứng (vertical), gọn để đặt sidebar trong modal chi tiết đơn — step đang active
       phát sáng nhẹ để tách bạch với step done (đã tick xanh) và step pending (mờ). -->
  <div class="d-flex flex-column gap-0" style="position:relative;">
    <div
      v-for="(step, index) in steps" :key="index"
      class="d-flex align-items-start gap-3" style="position:relative;"
    >
      <!-- Cột icon + đường nối dọc giữa các step -->
      <div class="d-flex flex-column align-items-center" style="width:32px; flex-shrink:0; position:relative;">
        <div
          class="rounded-circle d-flex align-items-center justify-content-center position-relative"
          style="width:32px; height:32px;"
          :style="isStepDone(index)
            ? 'background:var(--accent); border:2px solid var(--accent);'
            : index === currentStep
              ? 'background:var(--bg-hover); border:2px solid var(--accent);'
              : 'background:var(--bg-card-alt); border:2px solid var(--border-color-strong);'"
        >
          <Check v-if="isStepDone(index)" :size="14" color="white" />
          <component v-else :is="step.icon" :size="14" :style="{ opacity: index <= currentStep ? 1 : 0.4 }" />
        </div>
        <div
          v-if="index < steps.length - 1" style="width:2px; flex-grow:1; min-height:18px; margin-top:4px;"
          :style="index < currentStep ? 'background:var(--accent);' : 'background:var(--border-color-strong); opacity:0.4;'"
        ></div>
      </div>

      <!-- Label + mô tả -->
      <div class="flex-grow-1 pb-3" style="padding-top:4px;">
        <div
          class="fw-semibold" style="font-size:0.85rem; line-height:1.3;"
          :style="index === currentStep
            ? 'color:var(--accent-fg);'
            : isStepDone(index) ? 'color:var(--text-primary);' : 'color:var(--text-secondary);'"
        >
          {{ step.title }}
        </div>
        <div style="font-size:0.72rem; color:var(--text-muted); line-height:1.35; margin-top:2px;">
          {{ step.desc }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { t } from '../../i18n/index.js';
import { Check, Send, Bike, PartyPopper, FileText, CheckCircle2, Package, Clock } from '@lucide/vue';

// Nhận thẳng trạng thái đơn (status) — timeline tự chọn hiển thị 3 bước pre-ship
// ("Chờ xác nhận/Đã xác nhận/Đang đóng gói") hay 3 bước post-ship ("Đã gửi hàng/
// Đang giao/Chờ khách xác nhận"). Bước cuối pre-ship "Đang đóng gói" thực chất ứng với
// trạng thái "processing" — đơn đang chuẩn bị hàng trong kho trước khi giao cho shipper.
const props = defineProps({
  status:    { type: String, default: 'pending' },
  kenhBan:   { type: String, default: 'online' },
});

const PRE_SHIP  = ['pending', 'confirmed', 'processing'];
const POST_SHIP = ['shipping', 'out_for_delivery', 'awaiting_confirmation'];

const isInStore = computed(() => props.kenhBan === 'in_store');
const isPostShip = computed(() => POST_SHIP.includes(props.status));
const isAwaitingConfirmation = computed(() => props.status === 'awaiting_confirmation');

const steps = computed(() => {
  // Đơn tại quầy: mua + thanh toán + nhận hàng tại quầy = xong, không đi qua pipeline ship.
  if (isInStore.value) {
    return [
      { title: t('orderStatus.timeline.deliveredTitle'), desc: t('orderStatus.timeline.deliveredDesc'), icon: PartyPopper },
    ];
  }
  return isPostShip.value ? [
    { title: t('orderStatus.timeline.shippingTitle'),        desc: t('orderStatus.timeline.shippingDesc'),        icon: Send },
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
  const list = isPostShip.value ? POST_SHIP : PRE_SHIP;
  const idx = list.indexOf(props.status);
  return idx === -1 ? list.length - 1 : idx;
});

// Dấu tích cho bước đã hoàn tất. Bước cuối "awaiting_confirmation" (admin đã giao) cũng
// tính là hoàn tất dù đang là bước active — hành động "giao hàng" xong rồi, phần còn thiếu
// (khách xác nhận) là 1 hành động khác, có nút riêng bên dưới.
const isStepDone = (index) => {
  if (isInStore.value) return false; // đơn tại quầy: chỉ 1 step, không tick thêm
  return index < currentStep.value || (index === currentStep.value && isAwaitingConfirmation.value);
};
</script>
