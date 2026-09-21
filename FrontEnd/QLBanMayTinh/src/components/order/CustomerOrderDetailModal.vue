<template>
  <div
    class="customer-order-modal-backdrop position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
    style="background:rgba(0,0,0,0.55); z-index:1060; backdrop-filter:blur(4px);"
    @click.self="$emit('close')"
  >
    <div class="customer-order-modal d-flex flex-column" style="width:860px; max-width:96vw; max-height:92vh; border-radius:16px; background:var(--bg-card, #ffffff); border:1px solid var(--border, #fbcfe8); box-shadow:0 20px 25px -5px rgba(0,0,0,0.1), 0 8px 10px -6px rgba(0,0,0,0.1); overflow:hidden;">
      
      <!-- Header -->
      <div class="d-flex justify-content-between align-items-center px-4 py-3" style="border-bottom:1px solid var(--border, #fbcfe8); background:linear-gradient(135deg, var(--pink-50, #fff5f9) 0%, #ffffff 100%);">
        <div>
          <div class="fw-bold d-flex align-items-center gap-2" style="font-size:1.05rem; color:var(--text-primary, #111827);">
            <Package :size="18" style="color:var(--pink-500, #db2777);" />
            <span>Đơn hàng #{{ order?.maDon || order?.donHangId }}</span>
            <span v-if="order?.maDonHang" class="badge font-monospace fw-normal" style="background:rgba(219,39,119,0.08); color:var(--pink-600, #db2777); font-size:0.75rem;">
              {{ order.maDonHang }}
            </span>
          </div>
          <div class="d-flex align-items-center gap-2 mt-1" style="font-size:0.78rem; color:var(--text-secondary, #6b7280);">
            <span v-if="order?.kenhBan" class="badge" :style="{ background: channelColor(order.kenhBan).bg, color: channelColor(order.kenhBan).text }">
              {{ channelLabel(order.kenhBan) }}
            </span>
            <span>·</span>
            <span>Ngày đặt: {{ formatDate(order?.ngayDat) }}</span>
          </div>
        </div>
        <button
          type="button"
          class="btn-close btn-sm p-2"
          :aria-label="t('common.close')"
          @click="$emit('close')"
        ></button>
      </div>

      <!-- Body: 2 cột (Cột trái: sản phẩm + tài chính; Cột phải: tab trạng thái & tiến trình) -->
      <div class="d-flex flex-grow-1 overflow-hidden customer-order-modal-body">
        
        <!-- Cột trái: Chi tiết sản phẩm & thanh toán -->
        <div class="customer-order-left overflow-y-auto flex-grow-1 p-3 p-md-4" style="border-right:1px solid var(--border, #fbcfe8);">
          <div v-if="loading" class="text-center py-5 text-secondary">
            <div class="spinner-border spinner-border-sm me-2" style="color:var(--pink-500, #db2777);" role="status"></div>
            Đang tải thông tin đơn hàng...
          </div>

          <template v-else>
            <!-- Danh sách sản phẩm -->
            <div class="mb-4">
              <div class="text-secondary fw-bold mb-2 text-uppercase" style="font-size:0.72rem; letter-spacing:0.05em;">
                Sản phẩm trong đơn ({{ displayItems.length }})
              </div>
              <div class="d-flex flex-column gap-2">
                <div
                  v-for="item in displayItems" :key="item.id || item.chiTietId || item.bienTheId"
                  class="d-flex align-items-start gap-3 p-2 rounded-3"
                  style="background:var(--pink-50, #fff5f9); border:1px solid rgba(244,114,182,0.2);"
                >
                  <!-- Ảnh sản phẩm -->
                  <div style="width:56px; height:50px; flex-shrink:0; background:#ffffff; border-radius:8px; display:flex; align-items:center; justify-content:center; overflow:hidden; border:1px solid #fce7f3;">
                    <img
                      v-if="getProduct(item.bienTheId)?.hinhAnhChinh"
                      :src="getProduct(item.bienTheId).hinhAnhChinh"
                      style="max-width:50px; max-height:44px; object-fit:contain;"
                    />
                    <Laptop v-else :size="24" style="color:var(--text-muted, #9ca3af);" />
                  </div>

                  <!-- Thông tin SP -->
                  <div class="flex-grow-1 min-w-0">
                    <div class="fw-semibold text-truncate" style="font-size:0.86rem; color:var(--text-primary, #111827);">
                      {{ getProduct(item.bienTheId)?.tenSanPham || item.tenSanPham || 'Sản phẩm' }}
                    </div>
                    <!-- Phân loại cấu hình -->
                    <div
                      v-if="getItemSpecs(item)"
                      class="mt-1" style="font-size:0.73rem; color:var(--text-secondary, #6b7280);"
                    >
                      {{ getItemSpecs(item) }}
                    </div>
                    <div class="d-flex align-items-center gap-2 mt-1" style="font-size:0.72rem; color:var(--text-muted, #9ca3af);">
                      <span v-if="item.maSku">SKU: <code style="color:var(--text-secondary, #4b5563);">{{ item.maSku }}</code></span>
                      <span v-if="item.soSerial">· Serial: <strong style="color:var(--pink-600, #db2777); font-family:monospace;">{{ item.soSerial }}</strong></span>
                    </div>
                  </div>

                  <!-- Giá + Số lượng -->
                  <div class="text-end flex-shrink-0" style="min-width:90px;">
                    <div class="fw-bold" style="font-size:0.88rem; color:var(--pink-600, #db2777);">
                      {{ formatPrice(item.thanhTien ?? (item.donGia * (item.soLuong || 1))) }}
                    </div>
                    <div class="text-secondary" style="font-size:0.72rem;">
                      {{ formatPrice(item.donGia) }} × {{ item.soLuong || 1 }}
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Tổng kết tài chính -->
            <div class="p-3 rounded-3 mb-4" style="background:#ffffff; border:1px solid var(--border, #fbcfe8);">
              <div class="d-flex justify-content-between mb-1" style="font-size:0.82rem; color:var(--text-secondary, #6b7280);">
                <span>Tiền hàng:</span>
                <span>{{ formatPrice(order.tongTien) }}</span>
              </div>
              <div v-if="order.giamGia > 0" class="d-flex justify-content-between mb-1" style="font-size:0.82rem; color:#16a34a;">
                <span>Giảm giá voucher:</span>
                <span>− {{ formatPrice(order.giamGia) }}</span>
              </div>
              <div v-if="order.kenhBan !== 'in_store'" class="d-flex justify-content-between mb-1" style="font-size:0.82rem; color:var(--text-secondary, #6b7280);">
                <span>Phí vận chuyển:</span>
                <span :class="order.phiVanChuyen === 0 ? 'text-success fw-semibold' : ''">
                  {{ order.phiVanChuyen === 0 ? 'Miễn phí' : formatPrice(order.phiVanChuyen) }}
                </span>
              </div>
              <div class="d-flex justify-content-between fw-bold pt-2 mt-2" style="border-top:1px dashed var(--border, #fbcfe8); font-size:1rem;">
                <span style="color:var(--text-primary, #111827);">Tổng thanh toán:</span>
                <span style="color:var(--pink-600, #db2777); font-size:1.15rem;">
                  {{ formatPrice(order.thanhTien ?? order.tongTien) }}
                </span>
              </div>
            </div>

            <!-- Lịch sử giao hàng (tracking) -->
            <div v-if="order.maVanDon || history.length" class="mb-3">
              <OrderTrackingLog :ma-van-don="order.maVanDon || ''" :history="history" />
            </div>

            <!-- Nút thao tác của khách hàng -->
            <div class="d-flex gap-2 flex-wrap justify-content-end pt-2">
              <button
                v-if="order.trangThaiDonHang === 'awaiting_confirmation'"
                class="btn btn-sm btn-success fw-bold d-inline-flex align-items-center gap-1 px-3 py-2"
                style="font-size:0.85rem;"
                @click="$emit('confirm-received', order)"
              >
                <CheckCircle2 :size="15" /> Xác nhận đã nhận hàng
              </button>

              <button
                v-if="order.trangThaiDonHang === 'delivered'"
                class="btn btn-sm btn-outline-secondary d-inline-flex align-items-center gap-1 px-3 py-2"
                style="font-size:0.82rem;"
                @click="$emit('buy-again', order)"
              >
                <RefreshCw :size="13" /> Mua lại đơn này
              </button>

              <button
                v-if="canReturn"
                class="btn btn-sm btn-outline-danger d-inline-flex align-items-center gap-1 px-3 py-2"
                style="font-size:0.82rem;"
                @click="$emit('request-return', order)"
              >
                <Undo2 :size="13" /> Yêu cầu đổi trả
              </button>
            </div>
          </template>
        </div>

        <!-- Cột phải: Tab trạng thái & tiến trình đơn hàng (giống Admin) -->
        <div class="customer-order-right p-3 p-md-4 overflow-y-auto" style="width:310px; min-width:310px; flex-shrink:0; background:var(--pink-50, #fff5f9);">
          
          <!-- Nhóm: Trạng thái đơn -->
          <div class="mb-4">
            <div class="text-secondary fw-bold mb-2 text-uppercase" style="font-size:0.7rem; letter-spacing:0.06em;">
              {{ t('admin.orderDetailModal.orderStatus') }}
            </div>
            <span
              class="badge d-inline-flex align-items-center gap-1 px-2 py-1"
              style="font-size:0.82rem;"
              :style="{ background: effectiveStatusStyle.bg, color: effectiveStatusStyle.text }"
            >
              <component :is="effectiveStatusIcon" :size="14" />
              {{ effectiveStatusLabel }}
            </span>
          </div>

          <!-- Nhóm: Tiến trình đơn hàng -->
          <div class="mb-4">
            <div class="text-secondary fw-bold mb-3 text-uppercase" style="font-size:0.7rem; letter-spacing:0.06em;">
              {{ t('orderStatus.timeline.title') }}
            </div>

            <div class="d-flex flex-column gap-0" style="position:relative;">
              <div
                v-for="(step, index) in orderTimelineSteps" :key="step.id"
                class="d-flex align-items-start gap-3" style="position:relative;"
              >
                <!-- Cột icon + đường nối dọc -->
                <div class="d-flex flex-column align-items-center" style="width:32px; flex-shrink:0; position:relative;">
                  <div
                    class="rounded-circle d-flex align-items-center justify-content-center position-relative p-0"
                    style="width:30px; height:30px;"
                    :style="isStepReached(step.id)
                      ? isStepDone(step.id)
                        ? 'background:var(--pink-500, #db2777); border:2px solid var(--pink-500, #db2777); color:white;'
                        : 'background:#ffffff; border:2px solid var(--pink-500, #db2777); box-shadow:0 0 0 3px rgba(219,39,119,0.18);'
                      : 'background:#ffffff; border:2px solid #e5e7eb; color:#9ca3af;'"
                  >
                    <Check v-if="isStepDone(step.id)" :size="14" color="white" />
                    <component v-else :is="step.icon" :size="13" :style="{ opacity: isStepReached(step.id) ? 1 : 0.4 }" />
                  </div>
                  <div
                    v-if="index < orderTimelineSteps.length - 1"
                    style="width:2px; flex-grow:1; min-height:20px; margin-top:4px;"
                    :style="isStepReached(orderTimelineSteps[index+1].id)
                      ? 'background:var(--pink-500, #db2777);'
                      : 'background:#e5e7eb;'"
                  ></div>
                </div>

                <!-- Label & mô tả -->
                <div class="flex-grow-1 pb-3" style="padding-top:3px;">
                  <div
                    class="fw-semibold" style="font-size:0.83rem; line-height:1.3;"
                    :style="isStepCurrent(step.id)
                      ? 'color:var(--pink-600, #db2777); font-weight:700;'
                      : isStepReached(step.id) ? 'color:var(--text-primary, #111827);' : 'color:var(--text-muted, #9ca3af);'"
                  >
                    {{ step.title }}
                  </div>
                  <div style="font-size:0.71rem; color:var(--text-muted, #6b7280); line-height:1.35; margin-top:2px;">
                    {{ step.desc }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Nhóm: Thanh toán -->
          <div class="pt-3 mb-3" style="border-top:1px solid var(--border, #fbcfe8);">
            <div class="text-secondary fw-bold mb-2 text-uppercase" style="font-size:0.7rem; letter-spacing:0.06em;">
              {{ t('admin.orderDetailModal.paymentStatus') }}
            </div>
            <div
              class="badge d-inline-flex align-items-center gap-1 px-2 py-1"
              style="font-size:0.8rem;"
              :style="{ background: paymentStatusColor(order.trangThaiThanhToan).bg, color: paymentStatusColor(order.trangThaiThanhToan).text }"
            >
              <component :is="paymentStatusIcon(order.trangThaiThanhToan)" :size="13" />
              {{ order.trangThaiThanhToan ? paymentStatusLabel(order.trangThaiThanhToan) : '—' }}
            </div>

            <!-- Phương thức thanh toán -->
            <div v-if="paymentMethodSummary.length" class="mt-2 small text-secondary">
              <span class="d-block mb-1" style="font-size:0.72rem;">Phương thức:</span>
              <span style="color:var(--text-primary, #111827); font-size:0.8rem;">
                <template v-for="(g, idx) in paymentMethodSummary" :key="g.method">
                  <component :is="paymentMethodIcon(g.method)" :size="13" style="vertical-align:-2px; color:var(--pink-500, #db2777);" />
                  {{ paymentMethodLabel(g.method) }}
                  <span v-if="idx < paymentMethodSummary.length - 1">, </span>
                </template>
              </span>
            </div>
          </div>

          <!-- Nhóm: Thông tin khách hàng & Giao hàng -->
          <div class="pt-3" style="border-top:1px solid var(--border, #fbcfe8);">
            <div class="text-secondary fw-bold mb-2 text-uppercase" style="font-size:0.7rem; letter-spacing:0.06em;">
              Thông tin khách hàng & Giao hàng
            </div>
            <div class="d-flex flex-column gap-2" style="font-size:0.78rem;">
              <!-- Khách hàng / Người nhận -->
              <div class="p-2 rounded-2" style="background:#ffffff; border:1px solid var(--border, #fbcfe8);">
                <div class="d-flex align-items-center gap-1.5 fw-semibold" style="color:var(--text-primary, #111827); font-size:0.82rem;">
                  <User :size="13" style="color:var(--pink-500, #db2777);" class="flex-shrink-0" />
                  <span>{{ customerName }}</span>
                </div>
                <div v-if="customerPhone" class="d-flex align-items-center gap-1.5 text-secondary mt-1" style="font-size:0.76rem;">
                  <Phone :size="11" class="flex-shrink-0" />
                  <span>{{ customerPhone }}</span>
                </div>
                <div v-if="customerEmail" class="d-flex align-items-center gap-1.5 text-secondary mt-0.5" style="font-size:0.76rem;">
                  <Mail :size="11" class="flex-shrink-0" />
                  <span class="text-truncate">{{ customerEmail }}</span>
                </div>
              </div>

              <!-- Địa chỉ giao hàng -->
              <div class="p-2 rounded-2" style="background:#ffffff; border:1px solid var(--border, #fbcfe8);">
                <div class="text-secondary fw-semibold mb-1 d-flex align-items-center gap-1" style="font-size:0.68rem; text-transform:uppercase;">
                  <MapPin :size="11" style="color:var(--pink-500, #db2777);" /> Địa chỉ nhận hàng:
                </div>
                <div v-if="order.nguoiNhan && order.nguoiNhan !== customerName" class="small mb-1" style="font-size:0.76rem; color:var(--text-secondary);">
                  Người nhận: <strong style="color:var(--text-primary, #111827);">{{ order.nguoiNhan }}</strong>
                </div>
                <div v-if="order.sdtNguoiNhan && order.sdtNguoiNhan !== customerPhone" class="small text-secondary mb-1" style="font-size:0.76rem;">
                  SĐT nhận: <strong>{{ order.sdtNguoiNhan }}</strong>
                </div>
                <div style="line-height:1.4; color:var(--text-primary, #111827); font-size:0.78rem;">
                  {{ deliveryAddress }}
                </div>
                <div v-if="order.ngayGiaoThucTe" class="d-flex align-items-center gap-1 text-success mt-1.5 fw-semibold" style="font-size:0.74rem;">
                  <CheckCircle2 :size="12" class="flex-shrink-0" />
                  <span>Giao lúc: {{ formatDate(order.ngayGiaoThucTe) }}</span>
                </div>
                <div v-if="order.ghiChu" class="mt-1 pt-1 border-top small text-secondary" style="font-size:0.72rem;">
                  <em>Ghi chú: {{ order.ghiChu }}</em>
                </div>
              </div>
            </div>
          </div>

        </div>

      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { AuthStore } from '../../stores/index.js';
import { t, I18nStore } from '../../i18n/index.js';
import {
  orderStatusLabel, orderStatusColor, orderStatusIcon,
  channelLabel, channelColor,
  paymentStatusLabel, paymentStatusColor, paymentStatusIcon,
  paymentMethodLabel, paymentMethodIcon,
} from '../../utils/orderStatus.js';
import { formatPrice as formatPriceRaw } from '../../utils/formatPrice.js';
import * as ChiTietDonHangService from '../../services/ChiTietDonHangService.js';
import * as ThanhToanService from '../../services/ThanhToanService.js';
import OrderTrackingLog from './OrderTrackingLog.vue';
import {
  Package, Laptop, CheckCircle2, RefreshCw, Undo2, Check,
  Clock, Truck, Bike, Inbox, User, Phone, MapPin, Mail,
} from '@lucide/vue';

const props = defineProps({
  order: { type: Object, required: true },
  items: { type: Array, default: () => [] },
  products: { type: Array, default: () => [] },
  history: { type: Array, default: () => [] },
});

defineEmits(['close', 'confirm-received', 'buy-again', 'request-return']);

const customerName = computed(() => {
  return props.order.nguoiNhan || props.order.khachHangHoTen || AuthStore.user?.hoTen || 'Khách hàng';
});

const customerPhone = computed(() => {
  return props.order.sdtNguoiNhan || props.order.khachHangSdt || AuthStore.user?.soDienThoai || '';
});

const customerEmail = computed(() => {
  return props.order.khachHangEmail || AuthStore.user?.email || '';
});

const deliveryAddress = computed(() => {
  if (props.order.diaChiGiaoHangText) return props.order.diaChiGiaoHangText;
  if (props.order.khachHangDiaChi) return props.order.khachHangDiaChi;
  if (AuthStore.user?.diaChi) return AuthStore.user.diaChi;
  if (props.order.kenhBan === 'in_store') return 'Khách nhận trực tiếp tại quầy';
  return 'Chưa có địa chỉ giao hàng';
});

const localItems = ref([]);
const payments = ref([]);
const loading = ref(false);

const displayItems = computed(() => {
  return props.items.length ? props.items : localItems.value;
});

const getProduct = (bienTheId) => {
  return props.products.find(p => p.bienTheId === bienTheId);
};

const getItemSpecs = (item) => {
  const p = getProduct(item.bienTheId);
  const parts = [
    p?.cpu || item.cpu,
    p?.ram || item.ram,
    p?.oCung || item.oCung,
    p?.mauSac || item.mauSac,
  ].filter(Boolean);
  return parts.length ? parts.join(' · ') : '';
};

// 6 bước trạng thái tuyến tính cho đơn online (giống hệt bên Admin OrdersTable)
const LINEAR_STATUS_ORDER = [
  'pending', 'confirmed', 'processing',
  'out_for_delivery', 'awaiting_confirmation', 'delivered',
];

const getLinearStatusIndex = (status) => {
  if (status === 'shipping') return LINEAR_STATUS_ORDER.indexOf('processing');
  return LINEAR_STATUS_ORDER.indexOf(status);
};

const orderTimelineSteps = computed(() => {
  if (props.order?.kenhBan === 'in_store') {
    return [
      {
        id: 'delivered',
        title: t('orderStatus.timeline.deliveredTitle'),
        desc: t('orderStatus.timeline.inStoreDeliveredDesc') || t('orderStatus.timeline.deliveredDesc'),
        icon: CheckCircle2,
      },
    ];
  }
  return [
    { id: 'pending',                title: orderStatusLabel('pending'),                desc: t('orderStatus.timeline.placedDesc'),    icon: CheckCircle2 },
    { id: 'confirmed',              title: orderStatusLabel('confirmed'),              desc: t('orderStatus.timeline.confirmedDesc'), icon: CheckCircle2 },
    { id: 'processing',             title: orderStatusLabel('processing'),             desc: t('orderStatus.timeline.packingDesc'),   icon: Package },
    { id: 'out_for_delivery',       title: orderStatusLabel('out_for_delivery'),       desc: t('orderStatus.timeline.outForDeliveryDesc'), icon: Bike },
    { id: 'awaiting_confirmation',  title: orderStatusLabel('awaiting_confirmation'),  desc: t('orderStatus.timeline.deliveredDesc'),  icon: Inbox },
    { id: 'delivered',              title: orderStatusLabel('delivered'),              desc: t('orderStatus.timeline.deliveredDesc'),  icon: CheckCircle2 },
  ];
});

const isStepReached = (stepId) => {
  if (props.order?.kenhBan === 'in_store') {
    return !['cancelled', 'returned'].includes(props.order.trangThaiDonHang);
  }
  const cur = getLinearStatusIndex(props.order?.trangThaiDonHang);
  const idx = LINEAR_STATUS_ORDER.indexOf(stepId);
  return cur !== -1 && idx !== -1 && idx <= cur;
};

const isStepDone = (stepId) => {
  if (props.order?.kenhBan === 'in_store') {
    return !['cancelled', 'returned'].includes(props.order.trangThaiDonHang);
  }
  const cur = getLinearStatusIndex(props.order?.trangThaiDonHang);
  const idx = LINEAR_STATUS_ORDER.indexOf(stepId);
  return cur !== -1 && idx !== -1 && idx <= cur;
};

const isStepCurrent = (stepId) => {
  if (props.order?.kenhBan === 'in_store') return stepId === 'delivered';
  const curStatus = props.order?.trangThaiDonHang === 'shipping' ? 'processing' : props.order?.trangThaiDonHang;
  return curStatus === stepId;
};

// Trạng thái đơn hiệu dụng (đơn tại quầy luôn hiển thị Đã giao trừ khi cancelled/returned)
const effectiveStatus = computed(() => {
  if (props.order?.kenhBan === 'in_store' && !['cancelled', 'returned'].includes(props.order?.trangThaiDonHang)) {
    return 'delivered';
  }
  return props.order?.trangThaiDonHang || props.order?.trangThai;
});

const effectiveStatusLabel = computed(() => orderStatusLabel(effectiveStatus.value));
const effectiveStatusColor = computed(() => orderStatusColor(effectiveStatus.value));
const effectiveStatusIcon  = computed(() => orderStatusIcon(effectiveStatus.value));
const effectiveStatusStyle = computed(() => effectiveStatusColor.value);

const paymentMethodSummary = computed(() => {
  if (!payments.value.length) {
    if (props.order?.phuongThucThanhToan) {
      return [{ method: props.order.phuongThucThanhToan, count: 1 }];
    }
    return [];
  }
  const map = new Map();
  for (const p of payments.value) {
    if (!p.phuongThucThanhToan) continue;
    const cur = map.get(p.phuongThucThanhToan) ?? { method: p.phuongThucThanhToan, count: 0 };
    cur.count += 1;
    map.set(p.phuongThucThanhToan, cur);
  }
  return [...map.values()];
});

const canReturn = computed(() => {
  if (props.order?.trangThaiDonHang !== 'delivered' || !props.order?.ngayGiaoThucTe) return false;
  return Date.now() <= new Date(props.order.ngayGiaoThucTe).getTime() + 7 * 24 * 60 * 60 * 1000;
});

const formatPrice = (v) => (v == null ? '—' : formatPriceRaw(v));
const formatDate = (d) => {
  if (!d) return '—';
  try { return new Date(d).toLocaleString(I18nStore.locale); } catch { return d; }
};

onMounted(async () => {
  if (!props.items.length && props.order?.donHangId) {
    loading.value = true;
    try {
      localItems.value = await ChiTietDonHangService.getByDonHang(props.order.donHangId).catch(() => []);
    } finally {
      loading.value = false;
    }
  }
  if (props.order?.donHangId) {
    payments.value = await ThanhToanService.getByDonHang(props.order.donHangId).catch(() => []);
  }
});
</script>

<style scoped>
@media (max-width: 768px) {
  .customer-order-modal-body {
    flex-direction: column !important;
  }
  .customer-order-right {
    width: 100% !important;
    min-width: 100% !important;
    border-top: 1px solid var(--border, #fbcfe8);
  }
}
</style>
