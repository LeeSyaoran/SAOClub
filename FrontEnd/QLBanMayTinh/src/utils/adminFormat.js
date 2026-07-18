import { t } from "../i18n/index.js";
import { formatPrice as formatPriceRaw } from "./formatPrice.js";

// ── Helper định dạng dùng chung cho các trang staff (Admin/Staff/WarehouseManagement) ──
// Trước đây là hàm cục bộ trong AdminPage.vue — promote lên đây để các trang khác
// (StaffPage, WarehouseManagementPage) dùng lại được, không phải copy-paste.

export const statusLabel = (s) => t(`admin.statusLabel.${s}`);

export const formatPrice = (v) => (v == null ? "—" : formatPriceRaw(v));

export const formatDate = (d) => {
  if (!d) return "—";
  try {
    return new Date(d).toLocaleDateString("vi-VN");
  } catch {
    return d;
  }
};

// Ngày + giờ (khác formatDate — chỉ có ngày) — dùng cho ngày giao dự kiến/thực tế,
// vì admin cần biết cả mốc giờ, không chỉ ngày.
export const formatDateTime = (d) => {
  if (!d) return "—";
  try {
    return new Date(d).toLocaleString("vi-VN");
  } catch {
    return d;
  }
};

export const toLocalDT = (s) =>
  s ? (s.length === 16 ? s + ":00" : s.slice(0, 19)) : null;
