import { t } from "../i18n/index.js";
import { formatPrice as formatPriceRaw } from "./formatPrice.js";

// Các hàm định dạng dùng chung cho giao diện quản trị

export const statusLabel = (s) => {
  if (s == null || s === "") return "—";
  const str = String(s);
  const normalized = str.toLowerCase();
  if (normalized === "cho_nhap_hang" || normalized === "pending") return "Chờ nhập hàng";
  if (normalized === "active" || normalized === "dang_ban") return "Đang bán";
  if (normalized === "het_hang" || normalized === "out") return "Hết hàng";
  if (normalized === "inactive" || normalized === "tam_ngung") return "Tạm ngừng";
  if (normalized === "ngung_kinh_doanh") return "Ngừng kinh doanh";
  // Tra cứu nhãn trạng thái theo i18n
  const direct = t(`admin.statusLabel.${normalized}`);
  if (direct && direct !== `admin.statusLabel.${normalized}`) return direct;
  const raw = t(`admin.statusLabel.${str}`);
  if (raw && raw !== `admin.statusLabel.${str}`) return raw;
  const os = t(`orderStatus.${normalized}`);
  if (os && os !== `orderStatus.${normalized}`) return os;
  const rs = t(`admin.returnStatus.${normalized}`);
  if (rs && rs !== `admin.returnStatus.${normalized}`) return rs;
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

// Định dạng ngày và giờ theo chuẩn Việt Nam
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

// Loại bỏ dấu tiếng Việt để phục vụ tìm kiếm
export const boDauTiengViet = (str) =>
  (str ?? "")
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase();
