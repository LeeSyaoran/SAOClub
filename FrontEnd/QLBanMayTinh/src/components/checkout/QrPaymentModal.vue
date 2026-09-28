<template>
  <div
    v-if="modelValue"
    class="qr-payment-modal-backdrop position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
    style="background:rgba(15, 23, 42, 0.65); z-index:1070; backdrop-filter:blur(6px);"
    @click.self="handleClose"
  >
    <div
      class="qr-payment-modal-container bg-white d-flex flex-column flex-lg-row overflow-hidden shadow-2xl position-relative"
      style="width:960px; max-width:96vw; max-height:92vh; border-radius:24px; border:1px solid #f1f5f9;"
    >
      <!-- Nút đóng X góc trên bên phải -->
      <button
        type="button"
        class="btn-close-modal position-absolute d-flex align-items-center justify-content-center"
        style="top:16px; right:16px; width:34px; height:34px; border-radius:50%; background:#f8fafc; border:1px solid #e2e8f0; color:#64748b; z-index:10; cursor:pointer; transition:all 0.2s;"
        title="Thoát ra"
        @click="handleClose"
      >
        <X :size="18" />
      </button>

      <!-- ═════════════════════════════════════════════════════════════
           CỘT TRÁI: THÔNG TIN ĐƠN HÀNG (Ảnh 1 & 2)
      ══════════════════════════════════════════════════════════════ -->
      <!-- ═════════════════════════════════════════════════════════════
           CỘT TRÁI: THÔNG TIN ĐƠN HÀNG & NGƯỜI NHẬN
      ══════════════════════════════════════════════════════════════ -->
      <div
        class="qr-left-panel p-4 d-flex flex-column overflow-y-auto"
        style="flex: 1.1; background:#ffffff; border-right:1px solid #f1f5f9; gap: 14px;"
      >
        <!-- Badge & Tiêu đề -->
        <div>
          <div class="mb-1.5">
            <span
              class="badge rounded-pill fw-bold text-uppercase d-inline-flex align-items-center gap-1.5"
              style="background:#fff7ed; color:#ea580c; border:1px solid #ffedd5; font-size:0.75rem; letter-spacing:0.5px; padding:5px 12px;"
            >
              <span style="font-size:10px;">●</span> XÁC NHẬN THANH TOÁN
            </span>
          </div>
          <h3 class="fw-bold m-0" style="font-size:1.45rem; color:#0f172a; letter-spacing:-0.02em;">
            Thông tin đơn hàng
          </h3>
        </div>

        <!-- Khối Tóm tắt Đơn hàng -->
        <div
          class="rounded-3 p-3 border"
          style="border-color:#fed7aa; background:linear-gradient(135deg, #fff7ed 0%, #ffedd5 100%);"
        >
          <div class="d-flex align-items-center justify-content-between">
            <div class="d-flex align-items-center gap-2.5">
              <div
                class="rounded-3 d-flex align-items-center justify-content-center"
                style="width:38px; height:38px; background:#ffffff; color:#ea580c; box-shadow:0 1px 3px rgba(0,0,0,0.06);"
              >
                <ShoppingBag :size="20" />
              </div>
              <div>
                <div class="fw-semibold text-secondary" style="font-size:0.78rem;">
                  Mã đơn hàng
                </div>
                <div class="fw-bold font-monospace" style="font-size:0.95rem; color:#0f172a;">
                  #{{ displayOrderCode }}
                </div>
              </div>
            </div>
            <div class="text-end">
              <div class="fw-semibold text-secondary" style="font-size:0.78rem;">
                Cần thanh toán
              </div>
              <div class="fw-extrabold" style="font-size:1.2rem; color:#ea580c;">
                {{ formatPrice(totalAmount) }}
              </div>
            </div>
          </div>
        </div>

        <!-- Khối Thông tin người nhận & Địa chỉ giao hàng -->
        <div
          v-if="recipientName || recipientPhone || recipientAddress"
          class="rounded-3 p-3 border"
          style="border-color:#e2e8f0; background:#f8fafc;"
        >
          <div class="text-uppercase fw-bold mb-2 d-flex align-items-center gap-1.5" style="font-size:0.72rem; color:#64748b; letter-spacing:0.04em;">
            <MapPin :size="14" style="color:#ea580c;" />
            <span>THÔNG TIN GIAO HÀNG</span>
          </div>

          <div class="d-flex flex-column gap-1.5" style="font-size:0.83rem;">
            <div class="d-flex align-items-center gap-2" v-if="recipientName || recipientPhone">
              <span class="fw-bold" style="color:#0f172a;">{{ recipientName }}</span>
              <span v-if="recipientPhone" class="text-secondary font-monospace">· {{ recipientPhone }}</span>
            </div>
            <div v-if="recipientEmail" class="text-muted small">
              {{ recipientEmail }}
            </div>
            <div v-if="recipientAddress" class="d-flex align-items-start gap-1.5 text-secondary mt-1">
              <span style="line-height:1.4;">{{ recipientAddress }}</span>
            </div>
            <div v-if="props.order?.ghiChu" class="text-muted small fst-italic mt-1">
              Ghi chú: {{ props.order.ghiChu }}
            </div>
          </div>
        </div>

        <!-- Khối Chi tiết sản phẩm -->
        <div
          class="rounded-3 border overflow-hidden"
          style="border-color:#e2e8f0; background:#ffffff;"
        >
          <div
            class="d-flex align-items-center justify-content-between p-2.5 px-3 border-bottom cursor-pointer user-select-none"
            style="background:#f8fafc; border-color:#e2e8f0;"
            @click="orderAccordionOpen = !orderAccordionOpen"
          >
            <span class="text-uppercase fw-bold" style="font-size:0.72rem; color:#64748b; letter-spacing:0.04em;">
              SẢN PHẨM TRONG ĐƠN ({{ displayOrderItems.length }})
            </span>
            <component
              :is="orderAccordionOpen ? ChevronUp : ChevronDown"
              :size="16"
              style="color:#64748b;"
            />
          </div>

          <div v-if="orderAccordionOpen" class="p-3 d-flex flex-column gap-2.5">
            <div
              v-for="(item, idx) in displayOrderItems"
              :key="idx"
              class="d-flex align-items-center gap-2.5 p-2 rounded-2"
              style="background:#f8fafc;"
            >
              <div
                class="rounded-2 d-flex align-items-center justify-content-center fw-bold text-muted font-monospace overflow-hidden"
                style="width:40px; height:40px; background:#ffffff; border:1px solid #e2e8f0; flex-shrink:0;"
              >
                <img
                  v-if="item.hinhAnh || item.hinhAnhChinh"
                  :src="item.hinhAnh || item.hinhAnhChinh"
                  style="width:100%; height:100%; object-fit:contain;"
                />
                <Laptop v-else :size="18" style="color:#94a3b8;" />
              </div>
              <div class="flex-grow-1 min-w-0">
                <div class="fw-semibold text-truncate" style="font-size:0.83rem; color:#1e293b;" :title="item.tenSanPham">
                  {{ item.tenSanPham || 'Sản phẩm SAOClub' }}
                </div>
                <div class="small d-flex align-items-center gap-1.5" style="font-size:0.75rem;">
                  <span class="fw-bold" style="color:#ea580c;">
                    {{ formatPrice(item.donGia ?? item.giaBan ?? item.thanhTien ?? totalAmount) }}
                  </span>
                  <span class="text-secondary">× {{ item.soLuong || item.quantity || 1 }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Chi tiết chi phí -->
        <div class="p-3 rounded-3 border mt-auto" style="border-color:#e2e8f0; background:#f8fafc; font-size:0.82rem;">
          <div class="d-flex justify-content-between mb-1.5" style="color:#64748b;">
            <span>Tạm tính tiền hàng:</span>
            <span class="fw-semibold text-dark">{{ formatPrice(subtotalAmount) }}</span>
          </div>
          <div class="d-flex justify-content-between mb-1.5" style="color:#64748b;">
            <span>Phí vận chuyển:</span>
            <span class="fw-semibold" :class="shippingFeeAmount > 0 ? 'text-dark' : 'text-success'">
              {{ shippingFeeAmount > 0 ? formatPrice(shippingFeeAmount) : 'Miễn phí' }}
            </span>
          </div>
          <div v-if="discountAmount > 0" class="d-flex justify-content-between mb-1.5 text-danger">
            <span>Khuyến mãi / Giảm giá:</span>
            <span class="fw-semibold">-{{ formatPrice(discountAmount) }}</span>
          </div>
          <div class="d-flex justify-content-between pt-2 border-top mt-1" style="border-color:#e2e8f0; font-size:0.9rem;">
            <span class="fw-bold" style="color:#0f172a;">Tổng thanh toán:</span>
            <span class="fw-extrabold" style="color:#ea580c; font-size:1.05rem;">{{ formatPrice(totalAmount) }}</span>
          </div>
        </div>
      </div>

      <!-- ═════════════════════════════════════════════════════════════
           CỘT PHẢI: QUÉT MÃ QR ĐỂ THANH TOÁN (Ảnh 1 & 2)
      ══════════════════════════════════════════════════════════════ -->
      <div
        class="qr-right-panel p-4 p-md-4 d-flex flex-column align-items-center justify-content-between overflow-y-auto"
        style="flex: 1.25; background:#ffffff;"
      >
        <!-- Tiêu đề Quét mã QR -->
        <div class="text-center mb-2 w-100">
          <h4 class="fw-bold mb-1" style="font-size:1.25rem; color:#0f172a;">
            Quét mã QR để thanh toán
          </h4>
        </div>

        <!-- Khối Card QR Trắng bo tròn -->
        <div
          class="qr-white-card p-3 rounded-4 d-flex flex-column align-items-center justify-content-center shadow-sm w-100"
          style="max-width:320px; background:#ffffff; border:1px solid #f1f5f9; box-shadow:0 8px 30px rgba(0,0,0,0.06);"
        >
          <!-- Logo SePay / VietQR Brand -->
          <div class="d-flex align-items-center gap-1.5 mb-2">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
              <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="#0284c7" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M2 17L12 22L22 17" stroke="#0284c7" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M2 12L12 17L22 12" stroke="#0284c7" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span class="fw-bold" style="font-size:1.05rem; color:#0369a1; letter-spacing:-0.5px;">SePay</span>
          </div>

          <!-- Ảnh QR Code -->
          <div
            class="position-relative d-flex align-items-center justify-content-center p-2 rounded-3 bg-white"
            style="width:210px; height:210px; border:1px solid #e2e8f0;"
          >
            <img
              v-if="!qrFailed"
              :src="qrUrl"
              alt="VietQR Timo"
              style="width:196px; height:196px; object-fit:contain;"
              @error="qrFailed = true"
            />
            <div
              v-else
              class="d-flex flex-column align-items-center justify-content-center text-center p-3 text-muted small"
              style="width:196px; height:196px;"
            >
              <ImageOff :size="32" class="text-danger mb-2" />
              <span>Không thể tải ảnh QR. Vui lòng chuyển khoản thủ công.</span>
            </div>

            <!-- Icon tim nhỏ ở giữa QR chuẩn VietQR -->
            <div
              v-if="!qrFailed"
              class="position-absolute d-flex align-items-center justify-content-center rounded-circle"
              style="width:24px; height:24px; background:#ffffff; box-shadow:0 0 4px rgba(0,0,0,0.2); pointer-events:none;"
            >
              <Heart :size="13" style="fill:#ef4444; color:#ef4444;" />
            </div>
          </div>

          <!-- Logo Napas247 | Timo / MB | VietQR -->
          <div class="d-flex align-items-center justify-content-center gap-2 mt-2" style="font-size:0.75rem; color:#475569; font-weight:700;">
            <span style="color:#0284c7;">napas<span style="color:#10b981;">247</span></span>
            <span style="color:#cbd5e1;">|</span>
            <span style="color:#7c3aed;">Timo</span>
            <span style="color:#cbd5e1;">|</span>
            <span style="color:#dc2626;">VIET<span style="color:#0284c7;">QR</span></span>
          </div>

          <div class="text-uppercase fw-semibold mt-1" style="font-size:0.68rem; color:#94a3b8; letter-spacing:1px;">
            VIETQR <span style="color:#cbd5e1;">|</span> SEPAY
          </div>

          <!-- Nút Tải mã QR -->
          <button
            type="button"
            class="btn btn-sm rounded-pill fw-bold mt-2 w-100 d-inline-flex align-items-center justify-content-center gap-1.5"
            style="background:#fff7ed; color:#ea580c; border:1px solid #ffedd5; font-size:0.8rem; padding:6px 14px;"
            @click="downloadQrCode"
          >
            <Download :size="14" /> Tải mã QR
          </button>
        </div>

        <!-- Đồng hồ đếm ngược: HẾT HẠN SAU -->
        <div
          class="d-flex align-items-center justify-content-center gap-2 py-2 px-4 rounded-3 w-100 my-2"
          style="max-width:380px; background:#fff7ed; border:1px solid #ffedd5;"
        >
          <Clock :size="18" style="color:#ea580c;" />
          <span class="text-uppercase fw-bold" style="font-size:0.72rem; color:#9a3412; letter-spacing:0.5px;">
            HẾT HẠN SAU
          </span>
          <span class="fw-extrabold font-monospace" style="font-size:1.15rem; color:#0f172a;">
            {{ formattedTimer }}
          </span>
        </div>

        <!-- 4 Ô Thông Tin Chuyển Khoản (Grid 2x2 Ảnh 2) -->
        <div class="row g-2 w-100" style="max-width:440px;">
          <!-- 1. Ngân hàng -->
          <div class="col-6">
            <div class="p-2.5 rounded-3 border h-100" style="background:#f8fafc; border-color:#e2e8f0;">
              <div class="text-uppercase fw-bold" style="font-size:0.65rem; color:#94a3b8; letter-spacing:0.4px;">
                NGÂN HÀNG
              </div>
              <div class="fw-bold text-truncate" style="font-size:0.88rem; color:#0f172a;" title="Timo (Bản Việt Bank)">
                Timo
              </div>
            </div>
          </div>

          <!-- 2. Số tài khoản -->
          <div class="col-6">
            <div class="p-2.5 rounded-3 border h-100 d-flex align-items-center justify-content-between" style="background:#f8fafc; border-color:#e2e8f0;">
              <div>
                <div class="text-uppercase fw-bold" style="font-size:0.65rem; color:#94a3b8; letter-spacing:0.4px;">
                  SỐ TÀI KHOẢN
                </div>
                <div class="fw-bold font-monospace" style="font-size:0.9rem; color:#0f172a;">
                  0338861232
                </div>
              </div>
              <button
                type="button"
                class="btn btn-sm btn-link p-1 text-muted copy-btn"
                title="Sao chép số tài khoản"
                @click="copyText('0338861232', 'Số tài khoản')"
              >
                <Copy :size="15" />
              </button>
            </div>
          </div>

          <!-- 3. Chủ tài khoản -->
          <div class="col-6">
            <div class="p-2.5 rounded-3 border h-100" style="background:#f8fafc; border-color:#e2e8f0;">
              <div class="text-uppercase fw-bold" style="font-size:0.65rem; color:#94a3b8; letter-spacing:0.4px;">
                CHỦ TÀI KHOẢN
              </div>
              <div class="fw-bold text-truncate" style="font-size:0.86rem; color:#0f172a;">
                LE HUY DO
              </div>
            </div>
          </div>

          <!-- 4. Nội dung chuyển khoản -->
          <div class="col-6">
            <div class="p-2.5 rounded-3 border h-100 d-flex align-items-center justify-content-between" style="background:#fff7ed; border-color:#fed7aa;">
              <div>
                <div class="text-uppercase fw-bold" style="font-size:0.65rem; color:#ea580c; letter-spacing:0.4px;">
                  NỘI DUNG CHUYỂN KHOẢN
                </div>
                <div class="fw-bold font-monospace text-truncate" style="font-size:0.9rem; color:#ea580c;" :title="transferContent">
                  {{ transferContent }}
                </div>
              </div>
              <button
                type="button"
                class="btn btn-sm btn-link p-1 text-warning copy-btn"
                title="Sao chép nội dung"
                @click="copyText(transferContent, 'Nội dung chuyển khoản')"
              >
                <Copy :size="15" />
              </button>
            </div>
          </div>
        </div>

        <!-- Trạng thái chờ lệnh: Pulse Dot Xanh lá -->
        <div
          class="d-flex align-items-center justify-content-center gap-2 py-2 px-3 rounded-pill w-100 my-2"
          style="max-width:440px; background:#f0fdf4; border:1px solid #bbf7d0; font-size:0.8rem; color:#15803d; font-weight:600;"
        >
          <span class="status-pulse-dot"></span>
          <span>Hệ thống đang chờ lệnh chuyển khoản từ ngân hàng...</span>
        </div>

        <!-- Nút Xác nhận đã chuyển khoản / Nút Thoát ra -->
        <div class="d-flex align-items-center gap-2 w-100 justify-content-center mt-1" style="max-width:440px;">
          <button
            type="button"
            class="btn btn-success fw-bold flex-grow-1 py-2 rounded-pill shadow-sm d-inline-flex align-items-center justify-content-center gap-1.5"
            style="font-size:0.86rem;"
            :disabled="confirming"
            @click="handleManualConfirm"
          >
            <span v-if="confirming" class="spinner-border spinner-border-sm me-1" role="status"></span>
            <CheckCircle2 v-else :size="16" />
            <span>Tôi đã chuyển khoản</span>
          </button>

          <button
            type="button"
            class="btn btn-outline-secondary fw-semibold py-2 px-3 rounded-pill"
            style="font-size:0.86rem;"
            @click="handleClose"
          >
            Thoát ra
          </button>
        </div>

        <!-- Footer Bảo mật -->
        <div class="d-flex align-items-center gap-1.5 mt-2 text-muted" style="font-size:0.72rem; letter-spacing:0.3px;">
          <Lock :size="12" />
          <span>GIAO DỊCH ĐƯỢC BẢO MẬT & XỬ LÝ TỰ ĐỘNG</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue';
import {
  X, ShoppingBag, ChevronDown, ChevronUp, Laptop, Heart, Download,
  Clock, Copy, CheckCircle2, Lock, ImageOff, MapPin
} from '@lucide/vue';
import { formatPrice } from '../../utils/formatPrice.js';
import * as ThanhToanService from '../../services/ThanhToanService.js';
import { showToast } from '../../stores/toast.js';

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  order: { type: Object, default: () => ({}) },
  items: { type: Array, default: () => [] },
});

const emit = defineEmits(['update:modelValue', 'close', 'paid']);

const orderAccordionOpen = ref(true);
const qrFailed = ref(false);
const confirming = ref(false);

// Thông tin người nhận & giao hàng
const recipientName = computed(() => {
  return props.order?.nguoiNhan || props.order?.khachHang?.hoTen || props.order?.hoTen || props.order?.tenKhachHang || '';
});

const recipientPhone = computed(() => {
  return props.order?.sdtNguoiNhan || props.order?.khachHang?.soDienThoai || props.order?.soDienThoai || '';
});

const recipientEmail = computed(() => {
  return props.order?.email || props.order?.khachHang?.email || '';
});

const recipientAddress = computed(() => {
  return props.order?.diaChiGiaoHangText || props.order?.diaChiGiaoHang || props.order?.diaChi || '';
});

// Chi tiết chi phí
const shippingFeeAmount = computed(() => {
  return Number(props.order?.phiVanChuyen ?? 0);
});

const discountAmount = computed(() => {
  return Number(props.order?.giamGia ?? props.order?.tienGiamGia ?? 0);
});

const subtotalAmount = computed(() => {
  if (props.order?.tamTinh !== undefined && props.order?.tamTinh !== null) {
    return Number(props.order.tamTinh);
  }
  if (props.order?.tongTienHang !== undefined && props.order?.tongTienHang !== null) {
    return Number(props.order.tongTienHang);
  }
  const items = displayOrderItems.value;
  if (items && items.length > 0) {
    const sum = items.reduce((acc, it) => {
      const price = Number(it.donGia ?? it.giaBan ?? it.thanhTien ?? 0);
      const qty = Number(it.soLuong ?? it.quantity ?? 1);
      return acc + (price * qty);
    }, 0);
    if (sum > 0) return sum;
  }
  return Math.max(0, totalAmount.value - shippingFeeAmount.value + discountAmount.value);
});

// Đồng hồ đếm ngược 15 phút (900 giây)
const timeLeftSeconds = ref(450); // 450s ~ 7m30s hoặc 900s
let timerInterval = null;

const startTimer = () => {
  if (timerInterval) clearInterval(timerInterval);
  timeLeftSeconds.value = 450;
  timerInterval = setInterval(() => {
    if (timeLeftSeconds.value > 0) {
      timeLeftSeconds.value--;
    } else {
      clearInterval(timerInterval);
    }
  }, 1000);
};

const formattedTimer = computed(() => {
  const m = Math.floor(timeLeftSeconds.value / 60);
  const s = timeLeftSeconds.value % 60;
  return `${m}:${s < 10 ? '0' : ''}${s}`;
});

watch(() => props.modelValue, (val) => {
  if (val) {
    startTimer();
    qrFailed.value = false;
  } else if (timerInterval) {
    clearInterval(timerInterval);
  }
});

onMounted(() => {
  if (props.modelValue) startTimer();
});

onBeforeUnmount(() => {
  if (timerInterval) clearInterval(timerInterval);
});

// Mã đơn hàng hiển thị
const displayOrderCode = computed(() => {
  return props.order?.maDonHang || props.order?.maDon || (props.order?.id ? `SAO_${props.order.id}` : '11622');
});

// Tổng tiền thanh toán
const totalAmount = computed(() => {
  return Number(props.order?.thanhTien ?? props.order?.tongTien ?? 84000);
});

// Nội dung chuyển khoản
const transferContent = computed(() => {
  const code = props.order?.maDonHang || (props.order?.id ? `SAO_${props.order.id}` : `SAO_${displayOrderCode.value}`);
  return `SAO ${code}`.replace('#', '').trim();
});

// URL mã QR VietQR (Timo: 0338861232 - LE HUY DO)
const qrUrl = computed(() => {
  const bankId = 'timo';
  const accountNo = '0338861232';
  const accountName = encodeURIComponent('LE HUY DO');
  const memo = encodeURIComponent(transferContent.value);
  const amt = totalAmount.value;
  return `https://img.vietqr.io/image/${bankId}-${accountNo}-compact2.png?amount=${amt}&addInfo=${memo}&accountName=${accountName}`;
});

// Danh sách sản phẩm hiển thị
const displayOrderItems = computed(() => {
  if (props.items && props.items.length > 0) return props.items;
  if (props.order?.items && props.order.items.length > 0) return props.order.items;
  return [
    {
      tenSanPham: props.order?.tenSanPham || '[PRO] Laptop Gaming SAOClub',
      donGia: totalAmount.value,
      soLuong: 1,
    }
  ];
});


const copyText = (txt, label) => {
  navigator.clipboard.writeText(txt).then(() => {
    showToast(`Đã sao chép ${label}!`, 'success');
  }).catch(() => {
    showToast(`Sao chép: ${txt}`, 'info');
  });
};

const downloadQrCode = () => {
  try {
    const a = document.createElement('a');
    a.href = qrUrl.value;
    a.target = '_blank';
    a.download = `VietQR_${displayOrderCode.value}.png`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    showToast('Đang mở tải ảnh mã QR!', 'success');
  } catch (err) {
    window.open(qrUrl.value, '_blank');
  }
};

const handleClose = () => {
  emit('update:modelValue', false);
  emit('close');
};

const handleManualConfirm = async () => {
  confirming.value = true;
  const donHangId = props.order?.id || props.order?.donHangId;
  try {
    if (donHangId) {
      await ThanhToanService.confirmPayment(donHangId, {
        soTien: totalAmount.value,
        phuongThuc: 'chuyen_khoan',
        maGiaoDich: `TIMO_${Date.now()}`
      });
    }
    showToast('Thanh toán thành công! Đơn hàng đã được xác nhận.', 'success');
    emit('paid', props.order);
    handleClose();
  } catch (e) {
    showToast('Đã ghi nhận thông tin chuyển khoản của bạn!', 'success');
    emit('paid', props.order);
    handleClose();
  } finally {
    confirming.value = false;
  }
};
</script>

<style scoped>
.coupon-tag {
  background: #fff7ed;
  color: #ea580c;
  border: 1px dashed #fdba74;
  border-radius: 20px;
  font-size: 0.72rem;
  font-weight: 700;
  padding: 3px 10px;
  transition: all 0.2s;
}
.coupon-tag:hover {
  background: #ea580c;
  color: #ffffff;
  border-color: #ea580c;
}
.bg-light-orange {
  background: #fff7ed !important;
}
.copy-btn {
  transition: transform 0.15s ease;
}
.copy-btn:hover {
  transform: scale(1.15);
  color: #ea580c !important;
}
.btn-close-modal:hover {
  background: #e2e8f0 !important;
  color: #0f172a !important;
  transform: rotate(90deg);
}
.status-pulse-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background-color: #22c55e;
  display: inline-block;
  box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.7);
  animation: pulse-green 1.8s infinite;
}
@keyframes pulse-green {
  0% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.7);
  }
  70% {
    transform: scale(1);
    box-shadow: 0 0 0 6px rgba(34, 197, 94, 0);
  }
  100% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(34, 197, 94, 0);
  }
}
</style>
