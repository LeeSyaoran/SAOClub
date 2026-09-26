<template>
  <div
    class="customer-order-modal-backdrop position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
    style="background:rgba(15, 23, 42, 0.65); z-index:1060; backdrop-filter:blur(6px);"
    @click.self="$emit('close')"
  >
    <div
      class="customer-order-modal d-flex flex-column shadow-2xl position-relative overflow-hidden"
      style="width:880px; max-width:96vw; max-height:92vh; border-radius:22px; background:var(--bg-card, #ffffff); border:1px solid var(--border, #fed7aa); box-shadow:0 25px 50px -12px rgba(15, 23, 42, 0.25);"
    >
      <!-- Header -->
      <div
        class="d-flex justify-content-between align-items-center px-4 py-3"
        style="border-bottom:1px solid var(--border, #f1f5f9); background:linear-gradient(135deg, #fff7ed 0%, #ffffff 100%);"
      >
        <div class="d-flex align-items-center gap-2.5">
          <div
            class="rounded-3 d-flex align-items-center justify-content-center flex-shrink-0"
            style="width:38px; height:38px; background:linear-gradient(135deg, #ea580c 0%, #f97316 100%); color:#fff;"
          >
            <QrCode v-if="isQrPayment(order)" :size="20" />
            <Package v-else :size="20" />
          </div>
          <div>
            <div class="d-flex align-items-center gap-2">
              <span class="fw-bold" style="font-size:1.1rem; color:#0f172a;">
                Đơn hàng #{{ displayOrderCode }}
              </span>
              <span
                v-if="order?.kenhBan"
                class="badge"
                :style="{ background: channelColor(order.kenhBan).bg, color: channelColor(order.kenhBan).text, fontSize: '0.72rem' }"
              >
                {{ channelLabel(order.kenhBan) }}
              </span>
            </div>
            <div class="text-secondary small mt-0.5" style="font-size:0.78rem;">
              {{ formatPendingTime(order?.ngayDat) }} · {{ displayItems.length }} sản phẩm
            </div>
          </div>
        </div>

        <div class="d-flex align-items-center gap-2">
          <!-- Status pill (ẩn khi là Chờ thanh toán QR hoặc đơn QR) -->
          <span
            v-if="effectiveStatusLabel !== 'Chờ thanh toán QR' && !isQrPayment(order)"
            class="badge d-inline-flex align-items-center gap-1.5 px-3 py-1.5 rounded-pill fw-bold"
            :style="{ background: effectiveStatusStyle.bg, color: effectiveStatusStyle.text, border: order.trangThaiThanhToan === 'unpaid' ? '1.5px solid #fed7aa' : 'none', fontSize: '0.8rem' }"
          >
            <component :is="effectiveStatusIcon" :size="13" />
            {{ effectiveStatusLabel }}
          </span>

          <!-- Close X button -->
          <button
            type="button"
            class="btn-close-modal d-flex align-items-center justify-content-center"
            style="width:32px; height:32px; border-radius:50%; background:#f8fafc; border:1px solid #e2e8f0; color:#64748b; cursor:pointer;"
            title="Đóng"
            @click="$emit('close')"
          >
            <X :size="16" />
          </button>
        </div>
      </div>

      <!-- Body: 2 Columns -->
      <div class="d-flex flex-grow-1 overflow-hidden customer-order-modal-body">
        
        <!-- Cột trái: Chi tiết sản phẩm, Banner thanh toán & Tổng kết tài chính -->
        <div class="customer-order-left overflow-y-auto flex-grow-1 p-3 p-md-4" style="border-right:1px solid #f1f5f9;">
          
          <!-- BANNER THANH TOÁN QR NỔI BẬT (Khi đơn chưa thanh toán) -->
          <div
            v-if="order.trangThaiThanhToan !== 'paid'"
            class="rounded-4 p-3 mb-4 shadow-sm"
            style="border:1.5px dashed #fbd38d; background:#fffdfa;"
          >
            <div class="d-flex align-items-start justify-content-between flex-wrap gap-2 mb-3">
              <div class="d-flex align-items-center gap-2.5">
                <div
                  class="rounded-3 d-flex align-items-center justify-content-center flex-shrink-0"
                  style="width:44px; height:44px; background:#fff7ed; border:1.5px solid #ffedd5; color:#ea580c;"
                >
                  <DollarSign :size="24" stroke-width="2.5" />
                </div>
                <div>
                  <div class="fw-bold" style="font-size:0.95rem; color:#0f172a;">
                    Đang chờ thanh toán qua mã VietQR
                  </div>
                  <div class="text-secondary small" style="font-size:0.78rem;">
                    Ngân hàng <strong>Timo</strong> · STK: <strong>0338861232</strong> · <strong>LE HUY DO</strong>
                  </div>
                </div>
              </div>
              <div class="text-end">
                <div class="fw-extrabold" style="font-size:1.35rem; color:#ea580c;">
                  {{ formatPrice(orderTotalAmount) }}
                </div>
              </div>
            </div>

            <div class="d-flex align-items-center justify-content-between flex-wrap gap-2 pt-2 border-top" style="border-color:#ffedd5 !important;">
              <div class="text-muted small" style="font-size:0.75rem; max-width:320px; line-height:1.35;">
                Quý khách quét mã và thanh toán, cửa hàng sẽ kiểm tra & duyệt đơn sang bước <strong>Chờ xử lý</strong>.
              </div>
              <!-- NÚT THANH TOÁN NGAY MÀU CAM NỔI BẬT (Ảnh 5) -->
              <button
                type="button"
                class="btn btn-warning fw-extrabold text-white px-3.5 py-2 rounded-pill shadow-sm d-inline-flex align-items-center gap-2"
                style="background:linear-gradient(135deg, #ea580c 0%, #f97316 100%); border:none; font-size:0.88rem; letter-spacing:0.3px; box-shadow:0 6px 16px rgba(234, 88, 12, 0.3);"
                @click="openQrPaymentModal"
              >
                <CreditCard :size="16" />
                <span>THANH TOÁN NGAY</span>
              </button>
            </div>
          </div>

          <!-- BANNER THÔNG BÁO ĐÃ THANH TOÁN QR & ĐANG CHỜ XỬ LÝ -->
          <div
            v-else-if="isQrPayment(order) && order.trangThaiThanhToan === 'paid' && order.trangThaiDonHang === 'pending'"
            class="rounded-3 p-3 mb-4 d-flex align-items-center gap-3"
            style="background:#f0fdf4; border:1px solid #bbf7d0;"
          >
            <div class="rounded-circle d-flex align-items-center justify-content-center text-white flex-shrink-0" style="width:34px; height:34px; background:#16a34a;">
              <Check :size="18" stroke-width="3" />
            </div>
            <div>
              <div class="fw-bold" style="font-size:0.88rem; color:#15803d;">
                Đã thanh toán VietQR thành công · Đang chờ xử lý
              </div>
              <div class="text-secondary small mt-0.5" style="font-size:0.76rem;">
                Cửa hàng đã xác nhận thanh toán. Nhân viên đang kiểm tra đơn hàng và chuẩn bị xuất kho.
              </div>
            </div>
          </div>

          <!-- Danh sách sản phẩm -->
          <div class="mb-4">
            <div class="text-secondary fw-bold mb-2.5 text-uppercase" style="font-size:0.72rem; letter-spacing:0.05em;">
              SẢN PHẨM TRONG ĐƠN ({{ displayItems.length }})
            </div>

            <div v-if="loading" class="text-center py-4 text-secondary">
              <div class="spinner-border spinner-border-sm me-2 text-warning" role="status"></div>
              Đang tải danh sách sản phẩm...
            </div>

            <div v-else class="d-flex flex-column gap-2">
              <div
                v-for="item in displayItems" :key="item.id || item.chiTietId || item.bienTheId"
                class="d-flex align-items-start gap-3 p-2.5 rounded-3"
                style="background:#ffffff; border:1px solid #e2e8f0;"
              >
                <!-- Ảnh sản phẩm -->
                <div style="width:54px; height:50px; flex-shrink:0; background:#f8fafc; border-radius:8px; display:flex; align-items:center; justify-content:center; overflow:hidden; border:1px solid #e2e8f0;">
                  <img
                    v-if="getProduct(item.bienTheId)?.hinhAnhChinh || item.hinhAnh || item.hinhAnhChinh"
                    :src="getProduct(item.bienTheId)?.hinhAnhChinh || item.hinhAnh || item.hinhAnhChinh"
                    style="max-width:48px; max-height:44px; object-fit:contain;"
                  />
                  <Laptop v-else :size="24" style="color:#94a3b8;" />
                </div>

                <!-- Thông tin SP -->
                <div class="flex-grow-1 min-w-0">
                  <div class="fw-bold text-truncate" style="font-size:0.88rem; color:#0f172a;">
                    {{ getProduct(item.bienTheId)?.tenSanPham || item.tenSanPham || 'Sản phẩm SAOClub' }}
                  </div>
                  <div
                    v-if="getItemSpecs(item)"
                    class="mt-0.5" style="font-size:0.74rem; color:#64748b;"
                  >
                    {{ getItemSpecs(item) }}
                  </div>
                  <div class="d-flex align-items-center gap-2 mt-1" style="font-size:0.72rem; color:#94a3b8;">
                    <span v-if="item.maSku">SKU: <code style="color:#475569;">{{ item.maSku }}</code></span>
                    <span v-if="item.soSerial">· Serial: <strong style="color:#ea580c; font-family:monospace;">{{ item.soSerial }}</strong></span>
                  </div>
                </div>

                <!-- Giá + Số lượng -->
                <div class="text-end flex-shrink-0" style="min-width:90px;">
                  <div class="fw-bold font-monospace" style="font-size:0.92rem; color:#ea580c;">
                    {{ formatPrice(item.thanhTien ?? ((item.donGia || 0) * (item.soLuong || 1))) }}
                  </div>
                  <div class="text-secondary small" style="font-size:0.72rem;">
                    {{ formatPrice(item.donGia) }} × {{ item.soLuong || 1 }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Tổng kết tài chính -->
          <div class="p-3 rounded-3 mb-4" style="background:#f8fafc; border:1px solid #e2e8f0;">
            <div class="d-flex justify-content-between mb-1" style="font-size:0.82rem; color:#64748b;">
              <span>Tiền hàng:</span>
              <span class="text-dark">{{ formatPrice(order.tongTien) }}</span>
            </div>
            <div v-if="order.giamGia > 0" class="d-flex justify-content-between mb-1" style="font-size:0.82rem; color:#16a34a;">
              <span>Giảm giá voucher:</span>
              <span>− {{ formatPrice(order.giamGia) }}</span>
            </div>
            <div v-if="order.kenhBan !== 'in_store'" class="d-flex justify-content-between mb-1" style="font-size:0.82rem; color:#64748b;">
              <span>Phí vận chuyển:</span>
              <span :class="order.phiVanChuyen === 0 ? 'text-success fw-semibold' : 'text-dark'">
                {{ order.phiVanChuyen === 0 ? 'Miễn phí' : formatPrice(order.phiVanChuyen) }}
              </span>
            </div>
            <div class="d-flex justify-content-between fw-bold pt-2 mt-2" style="border-top:1px dashed #cbd5e1; font-size:1rem;">
              <span style="color:#0f172a;">Tổng thanh toán:</span>
              <span style="color:#ea580c; font-size:1.2rem;">
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
              class="btn btn-sm btn-success fw-bold d-inline-flex align-items-center gap-1.5 px-3 py-2"
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
        </div>

        <!-- Cột phải: 8 BƯỚC TIẾN TRÌNH THANH TOÁN QR (Hoặc quy trình chuẩn nếu không phải QR) -->
        <div class="customer-order-right p-3 p-md-4 overflow-y-auto" style="width:320px; min-width:320px; flex-shrink:0; background:#f8fafc; border-left:1px solid #e2e8f0;">
          
          <!-- Trạng thái đơn -->
          <div class="mb-4">
            <div class="text-secondary fw-bold mb-2 text-uppercase" style="font-size:0.7rem; letter-spacing:0.06em;">
              {{ t('admin.orderDetailModal.orderStatus') }}
            </div>
            <span
              class="badge d-inline-flex align-items-center gap-1.5 px-2.5 py-1.5 rounded-pill"
              style="font-size:0.82rem;"
              :style="{ background: effectiveStatusStyle.bg, color: effectiveStatusStyle.text, border: isQrPayment(order) && order.trangThaiThanhToan === 'unpaid' ? '1px solid #fed7aa' : 'none' }"
            >
              <component :is="effectiveStatusIcon" :size="14" />
              {{ effectiveStatusLabel }}
            </span>
          </div>

          <!-- Tiến trình đơn hàng (8 Bước dành cho QR) -->
          <div class="mb-4">
            <div class="d-flex align-items-center justify-content-between mb-3">
              <div class="text-secondary fw-bold text-uppercase" style="font-size:0.7rem; letter-spacing:0.06em;">
                {{ isQrPayment(order) ? 'TIẾN TRÌNH THANH TOÁN QR' : t('orderStatus.timeline.title') }}
              </div>
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
                        ? 'background:#16a34a; border:2px solid #16a34a; color:white;'
                        : isStepCurrent(step.id)
                          ? 'background:#ffffff; border:2.5px solid #ea580c; color:#ea580c; box-shadow:0 0 0 3px rgba(234, 88, 12, 0.2);'
                          : 'background:#ffffff; border:2px solid #cbd5e1; color:#64748b;'
                      : 'background:#ffffff; border:2px solid #e2e8f0; color:#94a3b8;'"
                  >
                    <Check v-if="isStepDone(step.id)" :size="14" stroke-width="3" color="white" />
                    <component v-else :is="step.icon" :size="13" :style="{ opacity: isStepReached(step.id) ? 1 : 0.35 }" />
                  </div>
                  <div
                    v-if="index < orderTimelineSteps.length - 1"
                    style="width:2px; flex-grow:1; min-height:22px; margin-top:4px;"
                    :style="isStepReached(orderTimelineSteps[index+1].id)
                      ? 'background:#16a34a;'
                      : 'background:#e2e8f0;'"
                  ></div>
                </div>

                <!-- Label & mô tả -->
                <div class="flex-grow-1 pb-3" style="padding-top:3px;">
                  <div
                    class="fw-semibold" style="font-size:0.83rem; line-height:1.3;"
                    :style="isStepCurrent(step.id)
                      ? 'color:#ea580c; font-weight:700;'
                      : isStepDone(step.id) ? 'color:#0f172a;' : 'color:#94a3b8;'"
                  >
                    {{ step.title }}
                  </div>
                  <div style="font-size:0.71rem; color:#64748b; line-height:1.35; margin-top:2px;">
                    {{ step.desc }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Thanh toán -->
          <div class="pt-3 mb-3" style="border-top:1px solid #e2e8f0;">
            <div class="text-secondary fw-bold mb-2 text-uppercase" style="font-size:0.7rem; letter-spacing:0.06em;">
              {{ t('admin.orderDetailModal.paymentStatus') }}
            </div>
            <div
              class="badge d-inline-flex align-items-center gap-1 px-2.5 py-1.5 rounded-pill"
              style="font-size:0.8rem;"
              :style="{ background: paymentStatusColor(order.trangThaiThanhToan).bg, color: paymentStatusColor(order.trangThaiThanhToan).text }"
            >
              <component :is="paymentStatusIcon(order.trangThaiThanhToan)" :size="13" />
              {{ order.trangThaiThanhToan ? paymentStatusLabel(order.trangThaiThanhToan) : '—' }}
            </div>

            <!-- Phương thức thanh toán -->
            <div class="mt-2 small text-secondary">
              <span class="d-block mb-1" style="font-size:0.72rem;">Phương thức:</span>
              <span style="color:#0f172a; font-size:0.8rem; font-weight:600;">
                <template v-if="isQrPayment(order)">
                  <QrCode :size="13" style="vertical-align:-2px; color:#ea580c;" /> Quét mã VietQR (Timo)
                </template>
                <template v-else-if="paymentMethodSummary.length">
                  <template v-for="(g, idx) in paymentMethodSummary" :key="g.method">
                    <component :is="paymentMethodIcon(g.method)" :size="13" style="vertical-align:-2px; color:#ea580c;" />
                    {{ paymentMethodLabel(g.method) }}
                    <span v-if="idx < paymentMethodSummary.length - 1">, </span>
                  </template>
                </template>
                <template v-else>—</template>
              </span>
            </div>
          </div>

          <!-- Thông tin khách hàng & Giao hàng -->
          <div class="pt-3" style="border-top:1px solid #e2e8f0;">
            <div class="text-secondary fw-bold mb-2 text-uppercase" style="font-size:0.7rem; letter-spacing:0.06em;">
              Thông tin nhận hàng
            </div>
            <div class="d-flex flex-column gap-2" style="font-size:0.78rem;">
              <div class="p-2 rounded-2" style="background:#ffffff; border:1px solid #e2e8f0;">
                <div class="d-flex align-items-center gap-1.5 fw-semibold" style="color:#0f172a; font-size:0.82rem;">
                  <User :size="13" style="color:#ea580c;" class="flex-shrink-0" />
                  <span>{{ customerName }}</span>
                </div>
                <div v-if="customerPhone" class="d-flex align-items-center gap-1.5 text-secondary mt-1" style="font-size:0.76rem;">
                  <Phone :size="11" class="flex-shrink-0" />
                  <span>{{ customerPhone }}</span>
                </div>
              </div>

              <div class="p-2 rounded-2" style="background:#ffffff; border:1px solid #e2e8f0;">
                <div class="text-secondary fw-semibold mb-1 d-flex align-items-center gap-1" style="font-size:0.68rem; text-transform:uppercase;">
                  <MapPin :size="11" style="color:#ea580c;" /> Địa chỉ nhận:
                </div>
                <div style="line-height:1.4; color:#0f172a; font-size:0.78rem;">
                  {{ deliveryAddress }}
                </div>
              </div>
            </div>
          </div>

        </div>

      </div>

    </div>

    <!-- Modal Thanh Toán QR SePay (khi bấm THANH TOÁN NGAY từ đơn Pending) -->
    <QrPaymentModal
      v-model="showQrModal"
      :order="order"
      :items="displayItems"
      @close="showQrModal = false"
      @paid="onPaymentSuccess"
    />
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
  isQrPayment, QR_TIMELINE_STEPS, isQrStepReached, isQrStepDone, isQrStepCurrent, getQrEffectiveStatus
} from '../../utils/orderStatus.js';
import { formatPrice as formatPriceRaw } from '../../utils/formatPrice.js';
import * as ChiTietDonHangService from '../../services/ChiTietDonHangService.js';
import * as ThanhToanService from '../../services/ThanhToanService.js';
import OrderTrackingLog from './OrderTrackingLog.vue';
import QrPaymentModal from '../checkout/QrPaymentModal.vue';
import {
  Package, Laptop, CheckCircle2, RefreshCw, Undo2, Check,
  Clock, Truck, Bike, Inbox, User, Phone, MapPin, Mail,
  DollarSign, CreditCard, X, ChevronDown, ChevronUp, FileText, QrCode
} from '@lucide/vue';

const props = defineProps({
  order: { type: Object, required: true },
  items: { type: Array, default: () => [] },
  products: { type: Array, default: () => [] },
  history: { type: Array, default: () => [] },
});

const emit = defineEmits(['close', 'confirm-received', 'buy-again', 'request-return', 'order-updated']);

const showQrModal = ref(false);
const localItems = ref([]);
const payments = ref([]);
const loading = ref(false);

const displayOrderCode = computed(() => {
  return props.order?.maDonHang || props.order?.maDon || (props.order?.id ? `as_${props.order.id}` : '11622');
});

const orderTotalAmount = computed(() => {
  return Number(props.order?.thanhTien ?? props.order?.tongTien ?? 0);
});

// Định dạng thời gian hiển thị: "LÚC 15:26 24 THÁNG 9, 2026"
const formatPendingTime = (dateStr) => {
  if (!dateStr) return 'LÚC 15:26 24 THÁNG 9, 2026';
  try {
    const d = new Date(dateStr);
    const hours = String(d.getHours()).padStart(2, '0');
    const minutes = String(d.getMinutes()).padStart(2, '0');
    const day = d.getDate();
    const month = d.getMonth() + 1;
    const year = d.getFullYear();
    return `LÚC ${hours}:${minutes} ${day} THÁNG ${month}, ${year}`;
  } catch {
    return String(dateStr);
  }
};

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

// 6 bước trạng thái tuyến tính cho đơn online thông thường
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
  // Nếu là đơn thanh toán qua mã QR: hiển thị 8 bước đặc thù
  if (isQrPayment(props.order)) {
    return QR_TIMELINE_STEPS;
  }
  // Đơn online thông thường: 6 bước
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
  if (isQrPayment(props.order)) {
    return isQrStepReached(props.order, stepId);
  }
  const cur = getLinearStatusIndex(props.order?.trangThaiDonHang);
  const idx = LINEAR_STATUS_ORDER.indexOf(stepId);
  return cur !== -1 && idx !== -1 && idx <= cur;
};

const isStepDone = (stepId) => {
  if (props.order?.kenhBan === 'in_store') {
    return !['cancelled', 'returned'].includes(props.order.trangThaiDonHang);
  }
  if (isQrPayment(props.order)) {
    return isQrStepDone(props.order, stepId);
  }
  const cur = getLinearStatusIndex(props.order?.trangThaiDonHang);
  const idx = LINEAR_STATUS_ORDER.indexOf(stepId);
  return cur !== -1 && idx !== -1 && idx <= cur;
};

const isStepCurrent = (stepId) => {
  if (props.order?.kenhBan === 'in_store') return stepId === 'delivered';
  if (isQrPayment(props.order)) {
    return isQrStepCurrent(props.order, stepId);
  }
  const curStatus = props.order?.trangThaiDonHang === 'shipping' ? 'processing' : props.order?.trangThaiDonHang;
  return curStatus === stepId;
};

const effectiveStatus = computed(() => {
  if (props.order?.kenhBan === 'in_store' && !['cancelled', 'returned'].includes(props.order?.trangThaiDonHang)) {
    return 'delivered';
  }
  return props.order?.trangThaiDonHang || props.order?.trangThai;
});

const effectiveStatusLabel = computed(() => {
  if (isQrPayment(props.order)) {
    return getQrEffectiveStatus(props.order).label;
  }
  return orderStatusLabel(effectiveStatus.value);
});

const effectiveStatusStyle = computed(() => {
  if (isQrPayment(props.order)) {
    return getQrEffectiveStatus(props.order).color;
  }
  return orderStatusColor(effectiveStatus.value);
});

const effectiveStatusIcon = computed(() => {
  return orderStatusIcon(props.order?.trangThaiDonHang);
});

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

const openQrPaymentModal = () => {
  showQrModal.value = true;
};

const onPaymentSuccess = () => {
  props.order.trangThaiThanhToan = 'paid';
  // Với đơn QR: sau khi thanh toán thành công chuyển vào bước 3 "Chờ xử lý" (pending)
  if (props.order.trangThaiDonHang === 'pending') {
    // giữ pending tương ứng với bước Chờ xử lý để nhân viên cửa hàng lên đơn
  }
  emit('order-updated', props.order);
};

onMounted(async () => {
  const orderId = props.order?.id || props.order?.donHangId;
  if (!props.items.length && orderId) {
    loading.value = true;
    try {
      localItems.value = await ChiTietDonHangService.getByDonHang(orderId).catch(() => []);
    } finally {
      loading.value = false;
    }
  }
  if (orderId) {
    payments.value = await ThanhToanService.getByDonHang(orderId).catch(() => []);
  }
});
</script>

<style scoped>
.btn-close-modal:hover {
  background: #fee2e2 !important;
  color: #ef4444 !important;
  transform: rotate(90deg);
}
@media (max-width: 768px) {
  .customer-order-modal-body {
    flex-direction: column !important;
  }
  .customer-order-right {
    width: 100% !important;
    min-width: 100% !important;
    border-left: none !important;
    border-top: 1px solid #e2e8f0;
  }
}
</style>
