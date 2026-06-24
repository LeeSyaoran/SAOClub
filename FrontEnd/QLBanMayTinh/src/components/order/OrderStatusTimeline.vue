<template>
  <!-- Trục thời gian theo dõi trạng thái đơn hàng -->
  <div class="card border-secondary" style="background:rgba(255,255,255,0.03);">
    <div class="card-body">
      <div class="d-flex flex-column flex-md-row justify-content-between gap-3">
        <div v-for="(step, index) in steps" :key="index"
             class="d-flex align-items-center flex-md-column gap-3 gap-md-2 flex-md-grow-1 position-relative">

          <!-- Đường kẻ nối giữa các bước (chỉ hiện từ bước 2 trở đi trên màn rộng) -->
          <div v-if="index > 0"
               class="d-none d-md-block position-absolute"
               style="top:18px; right:50%; width:100%; height:2px; transform:translateX(-50%);"
               :style="index <= currentStep ? 'background:#f4c200;' : 'background:rgba(255,255,255,0.1);'"></div>

          <!-- Icon vòng tròn -->
          <div class="rounded-circle d-flex align-items-center justify-content-center flex-shrink-0 position-relative"
               style="width:36px; height:36px; z-index:1;"
               :style="index <= currentStep
                 ? 'background:rgba(244,194,0,0.15); border:2px solid #f4c200;'
                 : 'background:rgba(255,255,255,0.05); border:2px solid rgba(255,255,255,0.15);'">
            <span v-if="index < currentStep" style="color:#f4c200; font-size:1rem;">✓</span>
            <span v-else-if="index === currentStep" style="color:#f4c200; font-size:0.75rem;">●</span>
            <span v-else style="color:#444; font-size:0.75rem;">○</span>
          </div>

          <!-- Chữ mô tả -->
          <div class="text-start text-md-center">
            <div class="fw-bold" style="font-size:0.82rem;"
                 :class="index === currentStep ? 'text-warning' : index < currentStep ? 'text-light' : 'text-secondary'">
              {{ step.title }}
            </div>
            <div class="text-secondary" style="font-size:0.72rem;">{{ step.desc }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// currentStep: 0 = đặt hàng, 1 = xác nhận, 2 = đang giao, 3 = hoàn tất
defineProps({ currentStep: { type: Number, default: 0 } });

const steps = [
  { title: 'Đơn hàng đã đặt',   desc: 'Hệ thống đã tiếp nhận' },
  { title: 'Đã xác nhận',       desc: 'Kho đang đóng gói' },
  { title: 'Đang vận chuyển',   desc: 'Shipper đã lấy hàng' },
  { title: 'Giao thành công',   desc: 'Đơn hàng hoàn tất' },
];
</script>
