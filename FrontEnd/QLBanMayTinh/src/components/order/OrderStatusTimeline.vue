<template>
  <!-- Trục thời gian theo dõi trạng thái đơn hàng -->
  <div class="card border-secondary" style="background:var(--bg-hover);">
    <div class="card-body">
      <div class="d-flex flex-column flex-md-row justify-content-between gap-3">
        <div v-for="(step, index) in steps" :key="index"
             class="d-flex align-items-center flex-md-column gap-3 gap-md-2 flex-md-grow-1 position-relative">

          <!-- Đường kẻ nối giữa các bước (chỉ hiện từ bước 2 trở đi trên màn rộng) -->
          <div v-if="index > 0"
               class="d-none d-md-block position-absolute"
               style="top:18px; right:50%; width:100%; height:2px; transform:translateX(-50%);"
               :style="index <= currentStep ? 'background:var(--accent);' : 'background:var(--border-color-strong);'"></div>

          <!-- Icon vòng tròn — icon riêng theo từng bước. Nền phải ĐẶC (không rgba mờ),
               nếu không đường kẻ nối phía sau sẽ lộ xuyên qua vòng tròn. -->
          <div class="rounded-circle d-flex align-items-center justify-content-center flex-shrink-0 position-relative"
               style="width:36px; height:36px; z-index:1;"
               :style="index <= currentStep
                 ? 'background:var(--bg-hover); border:2px solid var(--accent);'
                 : 'background:var(--bg-card-alt); border:2px solid var(--border-color-strong);'">
            <span :style="index <= currentStep ? 'font-size:1rem; opacity:1;' : 'font-size:1rem; opacity:0.35;'">{{ step.icon }}</span>
            <!-- Dấu tích nhạt màu cho bước đã hoàn tất (không phải bước đang tới) -->
            <span v-if="index < currentStep"
                  class="rounded-circle d-flex align-items-center justify-content-center position-absolute"
                  style="width:15px; height:15px; bottom:-2px; right:-2px; background:var(--accent); color:var(--accent-text); font-size:9px; opacity:0.55; border:1px solid var(--bg-hover);">✓</span>
          </div>

          <!-- Chữ mô tả -->
          <div class="text-start text-md-center">
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

// Nhận thẳng trạng thái đơn (status) thay vì số bước — timeline tự chọn hiển thị bộ 3
// bước "Đặt/Xác nhận/Đóng gói" (đơn còn ở tab "Chờ xác nhận") hay bộ 3 bước "Gửi hàng/
// Đang giao/Đã giao" (đơn đã sang tab "Đang giao"), xem TAB_STATUS_GROUPS ở
// AccountPage.vue. Đơn "delivered" không render component này nữa (chuyển sang dạng
// dòng gọn trong tab "Hoàn tất"), nên PRE_SHIP/POST_SHIP chỉ cần phủ tới lúc đó.
const props = defineProps({ status: { type: String, default: 'pending' } });

const PRE_SHIP  = ['pending', 'confirmed', 'processing'];
const POST_SHIP = ['shipping', 'out_for_delivery', 'delivered'];

const isPostShip = computed(() => POST_SHIP.includes(props.status));

const steps = computed(() => isPostShip.value ? [
  { title: t('orderStatus.timeline.shippingTitle'),        desc: t('orderStatus.timeline.shippingDesc'),        icon: '📤' },
  { title: t('orderStatus.timeline.outForDeliveryTitle'),  desc: t('orderStatus.timeline.outForDeliveryDesc'),  icon: '🛵' },
  { title: t('orderStatus.timeline.deliveredTitle'),       desc: t('orderStatus.timeline.deliveredDesc'),       icon: '🎉' },
] : [
  { title: t('orderStatus.timeline.placedTitle'),    desc: t('orderStatus.timeline.placedDesc'),    icon: '📝' },
  { title: t('orderStatus.timeline.confirmedTitle'), desc: t('orderStatus.timeline.confirmedDesc'), icon: '✅' },
  { title: t('orderStatus.timeline.packingTitle'),   desc: t('orderStatus.timeline.packingDesc'),   icon: '📦' },
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
</script>
