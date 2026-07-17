import { SettingsStore } from "../stores/settings.js";

// Định dạng tiền VNĐ dùng chung toàn app — đọc SettingsStore.dinhDangSo ('vi'|'en') để đổi
// kiểu nhóm chữ số (1.234.567 đ kiểu Việt / 1,234,567 đ kiểu Anh), đơn vị tiền tệ luôn là
// VNĐ (không đổi currency thật, chỉ đổi cách hiển thị số — xem spec "Ngoài phạm vi").
export const formatPrice = (v) =>
  new Intl.NumberFormat(SettingsStore.dinhDangSo === "en" ? "en-US" : "vi-VN",
    { style: "currency", currency: "VND" }).format(v ?? 0);
