import { t } from "../i18n/index.js";

// Bảng màu chốt — không tự ý đổi giá trị nếu không được yêu cầu.
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

// Icon theo trạng thái đơn hàng — dùng thay cho chấm tròn chung chung ở badge trạng thái
export const orderStatusIcon = (s) => {
  if (s === 'pending')    return '⏳';
  if (s === 'confirmed')  return '✅';
  if (s === 'processing') return '📦';
  if (s === 'shipping')   return '🚚';
  if (s === 'out_for_delivery') return '🛵';
  if (s === 'awaiting_confirmation') return '📬';
  if (s === 'delivered')  return '🎉';
  if (s === 'cancelled')  return '❌';
  if (s === 'returned')   return '↩️';
  return '●';
};

// Trạng thái THANH TOÁN (khác trạng thái đơn hàng ở trên) — dùng chung cho bảng đơn hàng
// admin, modal chi tiết đơn, và thẻ đơn hàng bên trang khách (trước đây mỗi chỗ hiển thị
// khác nhau: có chỗ hiện đúng nhãn tiếng Việt, có chỗ hiện thẳng chuỗi "unpaid"/"paid" thô).
export const paymentStatusLabel = (s) => t(`admin.paymentStatus.${s}`);

export const paymentStatusColor = (s) => {
  if (s === 'unpaid')   return { bg: 'rgba(148,163,184,0.15)', text: '#94a3b8' };
  if (s === 'partial')  return { bg: 'rgba(250,204,21,0.15)',  text: '#facc15' };
  if (s === 'paid')     return { bg: 'rgba(34,197,94,0.15)',   text: '#22c55e' };
  if (s === 'refunded') return { bg: 'rgba(139,92,246,0.15)',  text: '#a78bfa' };
  return { bg: 'rgba(107,114,128,0.15)', text: '#9ca3af' };
};

export const paymentStatusIcon = (s) => {
  if (s === 'unpaid')   return '⏳';
  if (s === 'partial')  return '💰';
  if (s === 'paid')     return '✅';
  if (s === 'refunded') return '↩️';
  return '●';
};

// Phuong thuc thanh toan — dung o POS (chon luc tao don) va modal "Chi tiet don hang"
// (hien lai). 1 nguon duy nhat cho danh sach gia tri + icon, tranh 2 noi tu dinh nghia
// roi lech nhau (dung bai hoc tu vu colorDot o productGrouping.js).
export const POS_PAYMENT_METHODS = ['tien_mat', 'vnpay', 'chuyen_khoan', 'the_tin_dung'];

export const paymentMethodLabel = (m) => t(`admin.paymentMethod.${m}`);

export const paymentMethodIcon = (m) => {
  if (m === 'tien_mat')     return '💵';
  if (m === 'vnpay')        return '📱';
  if (m === 'chuyen_khoan') return '🏦';
  if (m === 'the_tin_dung') return '💳';
  return '💰';
};
