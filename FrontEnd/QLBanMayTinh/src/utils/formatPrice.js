import { SettingsStore } from "../stores/settings.js";

// Định dạng tiền tệ VNĐ theo cài đặt hiển thị
export const formatPrice = (v) =>
  new Intl.NumberFormat(SettingsStore.dinhDangSo === "en" ? "en-US" : "vi-VN",
    { style: "currency", currency: "VND" }).format(v ?? 0);
