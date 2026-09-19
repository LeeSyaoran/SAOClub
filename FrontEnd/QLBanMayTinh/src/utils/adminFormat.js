import { t } from "../i18n/index.js";
import { formatPrice as formatPriceRaw } from "./formatPrice.js";

// ── Helper định dạng dùng chung cho các trang staff (Admin/Staff/WarehouseManagement) ──
// Trước đây là hàm cục bộ trong AdminPage.vue — promote lên đây để các trang khác
// (StaffPage, WarehouseManagementPage) dùng lại được, không phải copy-paste.

export const statusLabel = (s) => {
  if (s == null || s === "") return "—";
  const str = String(s);
  const normalized = str.toLowerCase();
  // 1. Check lowercase in admin.statusLabel
  const direct = t(`admin.statusLabel.${normalized}`);
  if (direct && direct !== `admin.statusLabel.${normalized}`) return direct;
  // 2. Check direct key in admin.statusLabel
  const raw = t(`admin.statusLabel.${str}`);
  if (raw && raw !== `admin.statusLabel.${str}`) return raw;
  // 3. Fallback to orderStatus
  const os = t(`orderStatus.${normalized}`);
  if (os && os !== `orderStatus.${normalized}`) return os;
  // 4. Fallback to admin.returnStatus
  const rs = t(`admin.returnStatus.${normalized}`);
  if (rs && rs !== `admin.returnStatus.${normalized}`) return rs;
  // 5. If nothing matches, return original value rather than raw key code
  return str;
};

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

// Bỏ dấu tiếng Việt để so khớp tìm kiếm không phân biệt dấu (gõ "laptop dell"
// vẫn khớp "Laptop Dell") — dùng NFD tách dấu khỏi ký tự gốc rồi xóa combining marks,
// xử lý riêng đ/Đ vì Unicode không tách nó qua NFD như các ký tự có dấu khác.
export const boDauTiengViet = (str) =>
  (str ?? "")
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase();
