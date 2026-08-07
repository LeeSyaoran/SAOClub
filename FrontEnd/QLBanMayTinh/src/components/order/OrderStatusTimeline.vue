<template>
  <!-- Trục thời gian theo dõi trạng thái đơn hàng -->
  <div class="card border-secondary" style="background:var(--bg-hover);">
    <div class="card-body">

      <!-- Desktop: mỗi icon có 2 "nửa đường nối" bên trái/phải (flex-grow:1, nằm ngay
           trong hàng chứa icon đó) thay vì 1 đường full-width tính theo % bề rộng cột (kỹ
           thuật cũ luôn thiếu hẳn đoạn nối vào icon CUỐI cùng dù cột đã đều nhau/hết gap —
           vì đường nối "của cột K" luôn nằm lọt trong vùng cột K-1, không bao giờ vươn tới
           chính icon K). Nửa phải của icon K ghép với nửa trái của icon K+1 tạo thành đúng
           1 đoạn nối liền mạch — 2 nửa luôn bằng nhau vì 2 cột kề nhau bằng nhau tuyệt đối
           ("flex:1 1 0"), không phụ thuộc số liệu % nào cả nên không thể lệch. -->
      <div class="d-none d-md-flex justify-content-between">
        <div v-for="(step, index) in steps" :key="index"
             class="d-flex flex-column align-items-center" style="flex:1 1 0; min-width:0;">
          <div class="d-flex align-items-center" style="width:100%;">
            <div class="flex-grow-1" style="height:1.5px;"
                 :style="index === 0 ? 'background:transparent;'
                   : index <= currentStep ? 'background:var(--accent);' : 'background:var(--border-color-strong); opacity:0.4;'"></div>
            <div class="rounded-circle d-flex align-items-center justify-content-center flex-shrink-0 position-relative"
                 style="width:36px; height:36px; z-index:1;"
                 :style="index <= currentStep
                   ? 'background:var(--bg-hover); border:2px solid var(--accent);'
                   : 'background:var(--bg-card-alt); border:2px solid var(--border-color-strong);'">
              <component :is="step.icon" :size="18" :style="{ opacity: index <= currentStep ? 1 : 0.35 }" />
              <span v-if="isStepDone(index)"
                    class="rounded-circle d-flex align-items-center justify-content-center position-absolute"
                    style="width:15px; height:15px; bottom:-2px; right:-2px; background:var(--accent); color:var(--accent-text); opacity:0.55; border:1px solid var(--bg-hover); display:flex; align-items:center; justify-content:center;"><Check :size="9" /></span>
            </div>
            <div class="flex-grow-1" style="height:1.5px;"
                 :style="index === steps.length - 1 ? 'background:transparent;'
                   : index < currentStep ? 'background:var(--accent);' : 'background:var(--border-color-strong); opacity:0.4;'"></div>
          </div>
          <div class="text-center mt-2">
            <div class="fw-bold" style="font-size:0.82rem;"
                 :style="index === currentStep ? 'color:var(--accent-fg);' : index < currentStep ? 'color:var(--text-primary);' : 'color:var(--text-secondary);'">
              {{ step.title }}
            </div>
            <div style="font-size:0.72rem; color:var(--text-secondary);">{{ step.desc }}</div>
          </div>
        </div>
      </div>

      <!-- Mobile: dọc, icon + chữ từng dòng, không cần đường nối -->
      <div class="d-flex d-md-none flex-column gap-3">
        <div v-for="(step, index) in steps" :key="index" class="d-flex align-items-center gap-3">
          <div class="rounded-circle d-flex align-items-center justify-content-center flex-shrink-0 position-relative"
               style="width:36px; height:36px;"
               :style="index <= currentStep
                 ? 'background:var(--bg-hover); border:2px solid var(--accent);'
                 : 'background:var(--bg-card-alt); border:2px solid var(--border-color-strong);'">
            <component :is="step.icon" :size="18" :style="{ opacity: index <= currentStep ? 1 : 0.35 }" />
            <span v-if="isStepDone(index)"
                  class="rounded-circle d-flex align-items-center justify-content-center position-absolute"
                  style="width:15px; height:15px; bottom:-2px; right:-2px; background:var(--accent); color:var(--accent-text); opacity:0.55; border:1px solid var(--bg-hover); display:flex; align-items:center; justify-content:center;"><Check :size="9" /></span>
          </div>
          <div>
            <div class="fw-bold" style="font-size:0.82rem;"
                 :style="index === currentStep ? 'color:var(--accent-fg);' : index < currentStep ? 'color:var(--text-primary);' : 'color:var(--text-secondary);'">
              {{ step.title }}
            </div>
            <div style="font-size:0.72rem; color:var(--text-secondary);">{{ step.desc }}</div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { t } from '../../i18n/index.js';
import { Check, Send, Bike, PartyPopper, FileText, CheckCircle2, Package } from '@lucide/vue';

// Nhận thẳng trạng thái đơn (status) thay vì số bước — timeline tự chọn hiển thị bộ 3
// bước "Đặt/Xác nhận/Đóng gói" (đơn còn ở tab "Chờ xác nhận") hay bộ 3 bước "Gửi hàng/
// Đang giao/Đã giao" (đơn đã sang tab "Đang giao"), xem TAB_STATUS_GROUPS ở
// AccountPage.vue. Bước cuối "deliveredTitle/deliveredDesc" thực chất hiển thị lúc đơn ở
// "awaiting_confirmation" (admin đã giao, chờ khách xác nhận) — đơn "delivered" thật sự
// không render component này nữa (chuyển sang dạng dòng gọn trong tab "Hoàn tất"), nên
// POST_SHIP dùng "awaiting_confirmation" làm bước cuối thay vì "delivered".
const props = defineProps({ status: { type: String, default: 'pending' } });

const PRE_SHIP  = ['pending', 'confirmed', 'processing'];
const POST_SHIP = ['shipping', 'out_for_delivery', 'awaiting_confirmation'];

const isPostShip = computed(() => POST_SHIP.includes(props.status));
const isAwaitingConfirmation = computed(() => props.status === 'awaiting_confirmation');

const steps = computed(() => isPostShip.value ? [
  { title: t('orderStatus.timeline.shippingTitle'),        desc: t('orderStatus.timeline.shippingDesc'),        icon: Send },
  { title: t('orderStatus.timeline.outForDeliveryTitle'),  desc: t('orderStatus.timeline.outForDeliveryDesc'),  icon: Bike },
  { title: t('orderStatus.timeline.deliveredTitle'),       desc: t('orderStatus.timeline.deliveredDesc'),       icon: PartyPopper },
] : [
  { title: t('orderStatus.timeline.placedTitle'),    desc: t('orderStatus.timeline.placedDesc'),    icon: FileText },
  { title: t('orderStatus.timeline.confirmedTitle'), desc: t('orderStatus.timeline.confirmedDesc'), icon: CheckCircle2 },
  { title: t('orderStatus.timeline.packingTitle'),   desc: t('orderStatus.timeline.packingDesc'),   icon: Package },
]);

const currentStep = computed(() => {
  const list = isPostShip.value ? POST_SHIP : PRE_SHIP;
  const idx = list.indexOf(props.status);
  // Trạng thái không thuộc phase nào (vd "processing" vẫn nằm pre-ship) → idx tìm thấy
  // bình thường; nếu không tìm thấy (không nên xảy ra trong 2 tab dùng component này)
  // mặc định về bước cuối của phase hiện tại thay vì bước đầu, an toàn hơn khi có trạng
  // thái mới phát sinh sau này mà quên cập nhật danh sách trên.
  return idx === -1 ? list.length - 1 : idx;
});

// Dấu tích cho bước đã hoàn tất. Bước cuối "awaiting_confirmation" (admin đã giao) cũng
// tính là hoàn tất dù đang là bước active — hành động "giao hàng" xong rồi, phần còn thiếu
// (khách xác nhận) là 1 hành động khác, có nút riêng bên dưới, không phải lý do để icon
// này chỉ sáng viền mà thiếu tích như các bước đang-thực-hiện khác (đóng gói/đang giao).
const isStepDone = (index) => index < currentStep.value || (index === currentStep.value && isAwaitingConfirmation.value);
</script>
