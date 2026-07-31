import { defineStore } from "pinia";
import { getCaiDat } from "../services/CaiDatService.js";
import { pinia } from "./pinia.js";

export const useSettingsStore = defineStore("settings", {
  state: () => ({
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
  }),
  actions: {
    async loadSettings() {
      try {
        const data = await getCaiDat();
        Object.assign(this.$state, data);
      } catch {
        // giữ mặc định — không chặn app khi tải lỗi
      }
      this.loaded = true;
    },
  },
});

export const SettingsStore = useSettingsStore(pinia);
export const loadSettings = () => SettingsStore.loadSettings();
