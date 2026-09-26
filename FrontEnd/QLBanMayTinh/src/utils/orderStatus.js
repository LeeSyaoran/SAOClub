import { t } from "../i18n/index.js";
import {
  Clock, CheckCircle2, Package, Truck, Bike, Inbox, PartyPopper, XCircle, Undo2,
  Wallet, Banknote, Smartphone, Landmark, CreditCard, Circle, FileCheck, FileText, QrCode
} from "@lucide/vue";

// Nhãn và màu sắc trạng thái đơn hàng
export const orderStatusLabel = (s) => t(`orderStatus.${s}`);

export const orderStatusColor = (s) => {
  if (s === 'pending')    return { bg: 'rgba(148,163,184,0.15)', text: '#94a3b8' };
  if (s === 'confirmed')  return { bg: 'rgba(59,130,246,0.15)',  text: '#60a5fa' };
  if (s === 'processing') return { bg: 'rgba(250,204,21,0.15)',  text: '#facc15' };
  if (s === 'shipping')   return { bg: 'rgba(139,92,246,0.15)',  text: '#a78bfa' };
  if (s === 'out_for_delivery') return { bg: 'rgba(56,189,248,0.15)', text: '#38bdf8' };
  if (s === 'awaiting_confirmation') return { bg: 'rgba(45,212,191,0.15)', text: '#2dd4bf' };
  if (s === 'delivered')  return { bg: 'rgba(34,197,94,0.15)',   text: '#22c55e' };
  if (s === 'cancelled')  return { bg: 'rgba(239,68,68,0.15)',   text: '#f87171' };
  if (s === 'returned')   return { bg: 'rgba(251,146,60,0.15)',  text: '#fb923c' };
  return { bg: 'rgba(107,114,128,0.15)', text: '#9ca3af' };
};

// Biểu tượng theo trạng thái đơn hàng
export const orderStatusIcon = (s) => {
  if (s === 'pending')    return Clock;
  if (s === 'confirmed')  return CheckCircle2;
  if (s === 'processing') return Package;
  if (s === 'shipping')   return Truck;
  if (s === 'out_for_delivery') return Bike;
  if (s === 'awaiting_confirmation') return Inbox;
  if (s === 'delivered')  return PartyPopper;
  if (s === 'cancelled')  return XCircle;
  if (s === 'returned')   return Undo2;
  return Circle;
};

// Nhãn và màu sắc trạng thái thanh toán
export const paymentStatusLabel = (s) => t(`admin.paymentStatus.${s}`);

export const paymentStatusColor = (s) => {
  if (s === 'unpaid')   return { bg: 'rgba(148,163,184,0.15)', text: '#94a3b8' };
  if (s === 'partial')  return { bg: 'rgba(250,204,21,0.15)',  text: '#facc15' };
  if (s === 'paid')     return { bg: 'rgba(34,197,94,0.15)',   text: '#22c55e' };
  if (s === 'refunded') return { bg: 'rgba(139,92,246,0.15)',  text: '#a78bfa' };
  return { bg: 'rgba(107,114,128,0.15)', text: '#9ca3af' };
};

export const paymentStatusIcon = (s) => {
  if (s === 'unpaid')   return Clock;
  if (s === 'partial')  return Wallet;
  if (s === 'paid')     return CheckCircle2;
  if (s === 'refunded') return Undo2;
  return Circle;
};

// Danh sách phương thức thanh toán hỗ trợ tại POS
export const POS_PAYMENT_METHODS = ['tien_mat', 'vnpay', 'chuyen_khoan', 'the_tin_dung'];

// Nhãn và màu sắc kênh bán hàng
export const channelLabel = (k) => t(`orderChannel.${k}`);

export const channelColor = (k) => {
  if (k === 'in_store')    return { bg: 'rgba(34,197,94,0.15)',  text: '#22c55e' };
  if (k === 'online')      return { bg: 'rgba(59,130,246,0.15)', text: '#60a5fa' };
  if (k === 'phone')       return { bg: 'rgba(250,204,21,0.15)', text: '#facc15' };
  if (k === 'social_media') return { bg: 'rgba(168,85,247,0.15)', text: '#a855f7' };
  return { bg: 'rgba(107,114,128,0.15)', text: '#9ca3af' };
};

export const paymentMethodLabel = (m) => t(`admin.paymentMethod.${m}`);

export const paymentMethodIcon = (m) => {
  if (m === 'tien_mat')     return Banknote;
  if (m === 'vnpay')        return Smartphone;
  if (m === 'chuyen_khoan') return Landmark;
  if (m === 'the_tin_dung') return CreditCard;
  return Wallet;
};

// ══════════════════════════════════════════════════════════════════════════
// TIẾN TRÌNH ĐẶC THÙ CHO ĐƠN HÀNG THANH TOÁN QUA MÃ QR (8 BƯỚC)
// Quy trình: Tạo đơn -> Thanh toán -> Chờ xử lý -> Đã lên đơn ->
//            Đang đóng gói -> Đang giao hàng -> Đã giao - chờ xác nhận -> Đã giao
// ══════════════════════════════════════════════════════════════════════════

export const isQrPayment = (order) => {
  if (!order) return false;
  const p = (order.phuongThucThanhToan || order.phuongThuc || '').toLowerCase().trim();
  if (p === 'qr' || p === 'bank_transfer' || p === 'chuyen_khoan' || p === 'vietqr') return true;
  // Đơn hàng online mà chưa thanh toán hoặc không phải là COD/tiền mặt thì đều áp dụng tiến trình thanh toán QR
  if (order.kenhBan === 'online' && !['tien_mat', 'cod'].includes(p)) {
    return true;
  }
  return false;
};

export const QR_TIMELINE_STEPS = [
  { id: 'tao_don',               title: 'Tạo đơn',                desc: 'Đơn hàng đã được tạo thành công',           icon: FileCheck },
  { id: 'thanh_toan',            title: 'Thanh toán',             desc: 'Chờ khách quét QR / Admin duyệt thanh toán', icon: CreditCard },
  { id: 'cho_xu_ly',             title: 'Chờ xử lý',              desc: 'Đã nhận tiền, chờ nhân viên kiểm tra',     icon: Clock },
  { id: 'da_len_don',            title: 'Đã lên đơn',             desc: 'Đơn hàng đã được duyệt và lên đơn',        icon: FileText },
  { id: 'dang_dong_goi',         title: 'Đang đóng gói',          desc: 'Kho đang chuẩn bị và đóng gói sản phẩm',    icon: Package },
  { id: 'dang_giao_hang',        title: 'Đang giao hàng',         desc: 'Shipper đang trên đường giao hàng',         icon: Bike },
  { id: 'da_giao_cho_xac_nhan',  title: 'Đã giao - chờ xác nhận', desc: 'Đã giao tới nơi, chờ khách nhận hàng',    icon: Inbox },
  { id: 'da_giao',               title: 'Đã giao',                desc: 'Đơn hàng đã hoàn tất thành công',          icon: CheckCircle2 },
];

export const isQrStepReached = (order, stepId) => {
  if (!order) return false;
  if (['cancelled', 'returned'].includes(order.trangThaiDonHang)) return false;
  if (stepId === 'tao_don') return true;
  if (stepId === 'thanh_toan') return true;

  const isPaid = order.trangThaiThanhToan === 'paid';
  if (!isPaid) return false;

  if (stepId === 'cho_xu_ly') return isPaid;

  const orderStatuses = ['confirmed', 'processing', 'out_for_delivery', 'awaiting_confirmation', 'delivered'];
  const curIdx = orderStatuses.indexOf(order.trangThaiDonHang === 'shipping' ? 'processing' : order.trangThaiDonHang);

  if (stepId === 'da_len_don') return curIdx >= 0;
  if (stepId === 'dang_dong_goi') return curIdx >= 1;
  if (stepId === 'dang_giao_hang') return curIdx >= 2;
  if (stepId === 'da_giao_cho_xac_nhan') return curIdx >= 3;
  if (stepId === 'da_giao') return curIdx >= 4;

  return false;
};

export const isQrStepDone = (order, stepId) => {
  if (!order) return false;
  if (['cancelled', 'returned'].includes(order.trangThaiDonHang)) return false;
  if (stepId === 'tao_don') return true;
  if (stepId === 'thanh_toan') return order.trangThaiThanhToan === 'paid';

  const isPaid = order.trangThaiThanhToan === 'paid';
  if (!isPaid) return false;

  const orderStatuses = ['confirmed', 'processing', 'out_for_delivery', 'awaiting_confirmation', 'delivered'];
  const curIdx = orderStatuses.indexOf(order.trangThaiDonHang === 'shipping' ? 'processing' : order.trangThaiDonHang);

  if (stepId === 'cho_xu_ly') return curIdx >= 0;
  if (stepId === 'da_len_don') return curIdx >= 1;
  if (stepId === 'dang_dong_goi') return curIdx >= 2;
  if (stepId === 'dang_giao_hang') return curIdx >= 3;
  if (stepId === 'da_giao_cho_xac_nhan') return curIdx >= 4;
  if (stepId === 'da_giao') return curIdx >= 4;

  return false;
};

export const isQrStepCurrent = (order, stepId) => {
  if (!order) return false;
  if (['cancelled', 'returned'].includes(order.trangThaiDonHang)) return false;

  if (stepId === 'thanh_toan') return order.trangThaiThanhToan !== 'paid';

  if (order.trangThaiThanhToan !== 'paid') return false;

  if (stepId === 'cho_xu_ly') return order.trangThaiDonHang === 'pending';
  if (stepId === 'da_len_don') return order.trangThaiDonHang === 'confirmed';
  if (stepId === 'dang_dong_goi') return order.trangThaiDonHang === 'processing';
  if (stepId === 'dang_giao_hang') return order.trangThaiDonHang === 'out_for_delivery' || order.trangThaiDonHang === 'shipping';
  if (stepId === 'da_giao_cho_xac_nhan') return order.trangThaiDonHang === 'awaiting_confirmation';
  if (stepId === 'da_giao') return order.trangThaiDonHang === 'delivered';

  return false;
};

export const getQrEffectiveStatus = (order) => {
  if (!order) return { label: 'Chờ xác nhận', color: { bg: 'rgba(148,163,184,0.15)', text: '#94a3b8' } };
  if (['cancelled', 'returned'].includes(order.trangThaiDonHang)) {
    return {
      label: orderStatusLabel(order.trangThaiDonHang),
      color: orderStatusColor(order.trangThaiDonHang)
    };
  }
  if (order.trangThaiThanhToan !== 'paid') {
    return { label: 'Chờ thanh toán QR', color: { bg: '#fff7ed', text: '#ea580c' } };
  }
  if (order.trangThaiDonHang === 'pending') {
    return { label: 'Chờ xử lý', color: { bg: 'rgba(250,204,21,0.15)', text: '#d97706' } };
  }
  if (order.trangThaiDonHang === 'confirmed') {
    return { label: 'Đã lên đơn', color: { bg: 'rgba(59,130,246,0.15)', text: '#2563eb' } };
  }
  if (order.trangThaiDonHang === 'processing' || order.trangThaiDonHang === 'shipping') {
    return { label: 'Đang đóng gói', color: { bg: 'rgba(168,85,247,0.15)', text: '#9333ea' } };
  }
  if (order.trangThaiDonHang === 'out_for_delivery') {
    return { label: 'Đang giao hàng', color: { bg: 'rgba(56,189,248,0.15)', text: '#0284c7' } };
  }
  if (order.trangThaiDonHang === 'awaiting_confirmation') {
    return { label: 'Đã giao - chờ xác nhận', color: { bg: 'rgba(45,212,191,0.15)', text: '#0d9488' } };
  }
  if (order.trangThaiDonHang === 'delivered') {
    return { label: 'Đã giao', color: { bg: 'rgba(34,197,94,0.15)', text: '#16a34a' } };
  }
  return {
    label: orderStatusLabel(order.trangThaiDonHang),
    color: orderStatusColor(order.trangThaiDonHang)
  };
};
