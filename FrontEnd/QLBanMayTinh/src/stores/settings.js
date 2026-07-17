import { reactive } from "vue";
import { getCaiDat } from "../Service/CaiDatService.js";

// ── Settings Store — cấu hình hệ thống tải từ backend lúc khởi động ─────────
// Giá trị mặc định dưới đây dùng ngay trước khi tải xong (không chặn render);
// nếu tải lỗi (mất mạng...) thì giữ nguyên mặc định, không chặn app.
export const SettingsStore = reactive({
  tenCuaHang: "SAOPhone",
  diaChi: "",
  soDienThoai: "",
  email: "",
  maSoThue: "",
  logoUrl: "",
  nguongTonKhoMacDinh: 5,
  ngonNguMacDinh: "vi",
  dinhDangSo: "vi",
  loaded: false,
});

export const loadSettings = async () => {
  try {
    const data = await getCaiDat();
    Object.assign(SettingsStore, data);
  } catch {
    // giữ mặc định — không chặn app khi tải lỗi
  }
  SettingsStore.loaded = true;
};
