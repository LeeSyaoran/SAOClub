import { t } from "../i18n/index.js";
import {
  Clock,
  CheckCircle2,
  Package,
  Truck,
  Bike,
  Inbox,
  PartyPopper,
  XCircle,
  Undo2,
  Wallet,
  Banknote,
  Smartphone,
  Landmark,
  CreditCard,
  Circle,
  FileCheck,
  FileText,
  QrCode,
} from "@lucide/vue";

// Nhãn và màu sắc trạng thái đơn hàng
export const orderStatusLabel = (s) => t(`orderStatus.${s}`);

export const orderStatusColor = (s) => {
  if (s === "pending") return { bg: "rgba(148,163,184,0.15)", text: "#94a3b8" };
  if (s === "confirmed") return { bg: "rgba(59,130,246,0.15)", text: "#60a5fa" };
  if (s === "processing") return { bg: "rgba(250,204,21,0.15)", text: "#facc15" };
  if (s === "shipping") return { bg: "rgba(139,92,246,0.15)", text: "#a78bfa" };
  if (s === "out_for_delivery") return { bg: "rgba(56,189,248,0.15)", text: "#38bdf8" };
  if (s === "awaiting_confirmation") return { bg: "rgba(45,212,191,0.15)", text: "#2dd4bf" };
  if (s === "delivered") return { bg: "rgba(34,197,94,0.15)", text: "#22c55e" };
  if (s === "cancelled") return { bg: "rgba(239,68,68,0.15)", text: "#f87171" };
  if (s === "returned") return { bg: "rgba(251,146,60,0.15)", text: "#fb923c" };
  return { bg: "rgba(107,114,128,0.15)", text: "#9ca3af" };
};

// Biểu tượng theo trạng thái đơn hàng
export const orderStatusIcon = (s) => {
  if (s === "pending") return Clock;
  if (s === "confirmed") return CheckCircle2;
  if (s === "processing") return Package;
  if (s === "shipping") return Truck;
  if (s === "out_for_delivery") return Bike;
  if (s === "awaiting_confirmation") return Inbox;
  if (s === "delivered") return PartyPopper;
  if (s === "cancelled") return XCircle;
  if (s === "returned") return Undo2;
  return Circle;
};

// Nhãn và màu sắc trạng thái thanh toán
export const paymentStatusLabel = (s) => t(`admin.paymentStatus.${s}`);

export const paymentStatusColor = (s) => {
  if (s === "unpaid") return { bg: "rgba(148,163,184,0.15)", text: "#94a3b8" };
  if (s === "partial") return { bg: "rgba(250,204,21,0.15)", text: "#facc15" };
  if (s === "paid") return { bg: "rgba(34,197,94,0.15)", text: "#22c55e" };
  if (s === "refunded") return { bg: "rgba(139,92,246,0.15)", text: "#a78bfa" };
  return { bg: "rgba(107,114,128,0.15)", text: "#9ca3af" };
};

export const paymentStatusIcon = (s) => {
  if (s === "unpaid") return Clock;
  if (s === "partial") return Wallet;
  if (s === "paid") return CheckCircle2;
  if (s === "refunded") return Undo2;
  return Circle;
};

// Danh sách phương thức thanh toán hỗ trợ tại POS
export const POS_PAYMENT_METHODS = ["tien_mat", "vnpay", "chuyen_khoan", "the_tin_dung"];

// Nhãn và màu sắc kênh bán hàng
export const channelLabel = (k) => t(`orderChannel.${k}`);

export const channelColor = (k) => {
  if (k === "in_store") return { bg: "rgba(34,197,94,0.15)", text: "#22c55e" };
  if (k === "online") return { bg: "rgba(59,130,246,0.15)", text: "#60a5fa" };
  if (k === "phone") return { bg: "rgba(250,204,21,0.15)", text: "#facc15" };
  if (k === "social_media") return { bg: "rgba(168,85,247,0.15)", text: "#a855f7" };
  return { bg: "rgba(107,114,128,0.15)", text: "#9ca3af" };
};

export const paymentMethodLabel = (m) => t(`admin.paymentMethod.${m}`);

export const paymentMethodIcon = (m) => {
  if (m === "tien_mat") return Banknote;
  if (m === "vnpay") return Smartphone;
  if (m === "chuyen_khoan") return Landmark;
  if (m === "the_tin_dung") return CreditCard;
  return Wallet;
};

// ══════════════════════════════════════════════════════════════════════════
// TIẾN TRÌNH CHO ĐƠN HÀNG THANH TOÁN SAU (COD) - 5 BƯỚC:
// Chờ xác nhận -> Đã lên đơn -> Đang đóng gói -> Đang giao hàng -> Đã giao
// ══════════════════════════════════════════════════════════════════════════

export const COD_TIMELINE_STEPS = [
  { id: "pending", title: "Chờ xác nhận", desc: "Đơn hàng đang chờ shop xác nhận", icon: Clock },
  {
    id: "confirmed",
    title: "Đã lên đơn",
    desc: "Đơn hàng đã được duyệt và lên đơn",
    icon: FileText,
  },
  {
    id: "processing",
    title: "Đang đóng gói",
    desc: "Kho đang chuẩn bị và đóng gói sản phẩm",
    icon: Package,
  },
  {
    id: "out_for_delivery",
    title: "Đang giao hàng",
    desc: "Shipper đang trên đường giao hàng",
    icon: Bike,
  },
  {
    id: "delivered",
    title: "Đã giao",
    desc: "Đơn hàng đã hoàn tất thành công",
    icon: CheckCircle2,
  },
];

export const COD_LINEAR_STATUS_ORDER = [
  "pending",
  "confirmed",
  "processing",
  "out_for_delivery",
  "delivered",
];

// Chỉ số bước đang active (sáng đèn) trên thanh tiến trình COD (0..4), 5 = đã hoàn tất toàn bộ
export const getCodActiveStepIndex = (status) => {
  if (status === "pending") return 0;
  if (status === "confirmed") return 1;
  if (status === "processing" || status === "shipping") return 2;
  if (status === "out_for_delivery") return 3;
  if (status === "awaiting_confirmation") return 4;
  if (status === "delivered") return 5;
  return -1;
};

export const getCodLinearStatusIndex = (status) => {
  const idx = getCodActiveStepIndex(status);
  return idx === 5 ? 4 : idx;
};

export const isCodStepReached = (order, stepId) => {
  if (!order) return false;
  if (["cancelled", "returned"].includes(order.trangThaiDonHang)) return false;
  const activeIdx = getCodActiveStepIndex(order.trangThaiDonHang);
  const idx = COD_LINEAR_STATUS_ORDER.indexOf(stepId);
  return activeIdx !== -1 && idx !== -1 && idx <= activeIdx;
};

export const isCodStepDone = (order, stepId) => {
  if (!order) return false;
  if (["cancelled", "returned"].includes(order.trangThaiDonHang)) return false;
  const activeIdx = getCodActiveStepIndex(order.trangThaiDonHang);
  const idx = COD_LINEAR_STATUS_ORDER.indexOf(stepId);
  if (activeIdx === -1 || idx === -1) return false;
  return idx < activeIdx;
};

export const isCodStepNext = (order, stepId) => {
  if (!order) return false;
  if (["cancelled", "returned"].includes(order.trangThaiDonHang)) return false;
  const activeIdx = getCodActiveStepIndex(order.trangThaiDonHang);
  const idx = COD_LINEAR_STATUS_ORDER.indexOf(stepId);
  if (activeIdx === -1 || idx === -1) return false;
  return idx === activeIdx;
};

export const isCodStepCurrent = (order, stepId) => {
  return isCodStepNext(order, stepId);
};

export const getCodEffectiveStatus = (order) => {
  if (!order)
    return { label: "Chờ xác nhận", color: { bg: "rgba(148,163,184,0.15)", text: "#94a3b8" } };
  if (["cancelled", "returned"].includes(order.trangThaiDonHang)) {
    return {
      label: orderStatusLabel(order.trangThaiDonHang),
      color: orderStatusColor(order.trangThaiDonHang),
    };
  }
  if (order.kenhBan === "in_store") {
    return { label: "Đã giao", color: { bg: "rgba(34,197,94,0.15)", text: "#16a34a" } };
  }
  if (order.trangThaiDonHang === "pending") {
    return { label: "Chờ xác nhận", color: { bg: "rgba(148,163,184,0.15)", text: "#94a3b8" } };
  }
  if (order.trangThaiDonHang === "confirmed") {
    return { label: "Đã lên đơn", color: { bg: "rgba(59,130,246,0.15)", text: "#2563eb" } };
  }
  if (order.trangThaiDonHang === "processing" || order.trangThaiDonHang === "shipping") {
    return { label: "Đang đóng gói", color: { bg: "rgba(168,85,247,0.15)", text: "#9333ea" } };
  }
  if (
    order.trangThaiDonHang === "out_for_delivery" ||
    order.trangThaiDonHang === "awaiting_confirmation"
  ) {
    return { label: "Đang giao hàng", color: { bg: "rgba(56,189,248,0.15)", text: "#0284c7" } };
  }
  if (order.trangThaiDonHang === "delivered") {
    return { label: "Đã giao", color: { bg: "rgba(34,197,94,0.15)", text: "#16a34a" } };
  }
  return {
    label: orderStatusLabel(order.trangThaiDonHang),
    color: orderStatusColor(order.trangThaiDonHang),
  };
};

// ══════════════════════════════════════════════════════════════════════════
// TIẾN TRÌNH CHO ĐƠN HÀNG THANH TOÁN TRƯỚC BẰNG QR (7 BƯỚC):
// Tạo đơn -> Chờ thanh toán -> Chờ xử lý -> Đã lên đơn ->
// Đang đóng gói -> Đang giao hàng -> Đã giao
// ══════════════════════════════════════════════════════════════════════════

export const isQrPayment = (order) => {
  if (!order) return false;
  const p = (order.phuongThucThanhToan || order.phuongThuc || "").toLowerCase().trim();
  // Nếu rõ ràng là QR hoặc chuyển khoản ngân hàng
  if (["qr", "bank_transfer", "chuyen_khoan", "vietqr", "vnpay"].includes(p)) return true;
  // Nếu là tiền mặt / COD thì chắc chắn là phương thức thanh toán sau (COD)
  if (["tien_mat", "cod", "tiền mặt", "tien mat"].includes(p)) return false;
  // Kiểm tra nếu có danh sách thanh toán chứa chuyển khoản / QR
  if (
    Array.isArray(order.thanhToans) &&
    order.thanhToans.some((t) => {
      const tp = (t.phuongThucThanhToan || t.phuongThuc || "").toLowerCase().trim();
      return ["qr", "bank_transfer", "chuyen_khoan", "vietqr", "vnpay"].includes(tp);
    })
  ) {
    return true;
  }
  return false;
};

export const QR_TIMELINE_STEPS = [
  { id: "tao_don", title: "Tạo đơn", desc: "Đơn hàng đã được tạo thành công", icon: FileCheck },
  {
    id: "cho_thanh_toan",
    title: "Chờ thanh toán",
    desc: "Đang chờ khách hàng quét mã QR thanh toán",
    icon: CreditCard,
  },
  {
    id: "cho_xu_ly",
    title: "Chờ xử lý",
    desc: "Đã nhận tiền, chờ nhân viên kiểm tra",
    icon: Clock,
  },
  {
    id: "da_len_don",
    title: "Đã lên đơn",
    desc: "Đơn hàng đã được duyệt và lên đơn",
    icon: FileText,
  },
  {
    id: "dang_dong_goi",
    title: "Đang đóng gói",
    desc: "Kho đang chuẩn bị và đóng gói sản phẩm",
    icon: Package,
  },
  {
    id: "dang_giao_hang",
    title: "Đang giao hàng",
    desc: "Shipper đang trên đường giao hàng",
    icon: Bike,
  },
  { id: "da_giao", title: "Đã giao", desc: "Đơn hàng đã hoàn tất thành công", icon: CheckCircle2 },
];

export const QR_LINEAR_STEP_ORDER = [
  "tao_don",
  "cho_thanh_toan",
  "cho_xu_ly",
  "da_len_don",
  "dang_dong_goi",
  "dang_giao_hang",
  "da_giao",
];

// Chỉ số bước đang active (sáng đèn) trên thanh tiến trình QR (1..6), 7 = đã hoàn tất toàn bộ
export const getQrActiveStepIndex = (order) => {
  if (!order) return -1;
  if (["cancelled", "returned"].includes(order.trangThaiDonHang)) return -1;
  if (order.trangThaiThanhToan !== "paid") return 1; // 'cho_thanh_toan' đang sáng, 'tao_don' đã tích

  const s = order.trangThaiDonHang;
  if (s === "pending") return 2; // 'cho_xu_ly' đang sáng
  if (s === "confirmed") return 4; // 'da_len_don' đã xong, 'dang_dong_goi' đang sáng
  if (s === "processing" || s === "shipping") return 5; // 'dang_dong_goi' đã xong, 'dang_giao_hang' đang sáng
  if (s === "out_for_delivery") return 6; // 'dang_giao_hang' đã xong, 'da_giao' đang sáng
  if (s === "awaiting_confirmation") return 6; // 'da_giao' đang sáng
  if (s === "delivered") return 7; // Tất cả 7 bước hoàn tất
  return -1;
};

export const isQrStepReached = (order, stepId) => {
  if (!order) return false;
  if (["cancelled", "returned"].includes(order.trangThaiDonHang)) return false;
  const normalizedId = stepId === "thanh_toan" ? "cho_thanh_toan" : stepId;
  const activeIdx = getQrActiveStepIndex(order);
  const idx = QR_LINEAR_STEP_ORDER.indexOf(normalizedId);
  return activeIdx !== -1 && idx !== -1 && idx <= activeIdx;
};

export const isQrStepDone = (order, stepId) => {
  if (!order) return false;
  if (["cancelled", "returned"].includes(order.trangThaiDonHang)) return false;
  const normalizedId = stepId === "thanh_toan" ? "cho_thanh_toan" : stepId;
  const activeIdx = getQrActiveStepIndex(order);
  const idx = QR_LINEAR_STEP_ORDER.indexOf(normalizedId);
  if (activeIdx === -1 || idx === -1) return false;
  return idx < activeIdx;
};

export const isQrStepNext = (order, stepId) => {
  if (!order) return false;
  if (["cancelled", "returned"].includes(order.trangThaiDonHang)) return false;
  const normalizedId = stepId === "thanh_toan" ? "cho_thanh_toan" : stepId;
  const activeIdx = getQrActiveStepIndex(order);
  const idx = QR_LINEAR_STEP_ORDER.indexOf(normalizedId);
  if (activeIdx === -1 || idx === -1) return false;
  return idx === activeIdx;
};

export const isQrStepCurrent = (order, stepId) => {
  return isQrStepNext(order, stepId);
};

export const getQrEffectiveStatus = (order) => {
  if (!order)
    return { label: "Chờ xác nhận", color: { bg: "rgba(148,163,184,0.15)", text: "#94a3b8" } };
  if (["cancelled", "returned"].includes(order.trangThaiDonHang)) {
    return {
      label: orderStatusLabel(order.trangThaiDonHang),
      color: orderStatusColor(order.trangThaiDonHang),
    };
  }
  if (order.trangThaiThanhToan !== "paid") {
    return { label: "Chờ thanh toán QR", color: { bg: "#fff7ed", text: "#ea580c" } };
  }
  if (order.trangThaiDonHang === "pending") {
    return { label: "Chờ xử lý", color: { bg: "rgba(250,204,21,0.15)", text: "#d97706" } };
  }
  if (order.trangThaiDonHang === "confirmed") {
    return { label: "Đã lên đơn", color: { bg: "rgba(59,130,246,0.15)", text: "#2563eb" } };
  }
  if (order.trangThaiDonHang === "processing" || order.trangThaiDonHang === "shipping") {
    return { label: "Đang đóng gói", color: { bg: "rgba(168,85,247,0.15)", text: "#9333ea" } };
  }
  if (
    order.trangThaiDonHang === "out_for_delivery" ||
    order.trangThaiDonHang === "awaiting_confirmation"
  ) {
    return { label: "Đang giao hàng", color: { bg: "rgba(56,189,248,0.15)", text: "#0284c7" } };
  }
  if (order.trangThaiDonHang === "delivered") {
    return { label: "Đã giao", color: { bg: "rgba(34,197,94,0.15)", text: "#16a34a" } };
  }
  return {
    label: orderStatusLabel(order.trangThaiDonHang),
    color: orderStatusColor(order.trangThaiDonHang),
  };
};
