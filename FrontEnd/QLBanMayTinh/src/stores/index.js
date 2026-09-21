import { defineStore } from "pinia";
import { pinia } from "./pinia.js";

const STORAGE_KEY = "saoclub_session";

// Khôi phục phiên đăng nhập từ sessionStorage
const saved = (() => {
  try {
    const s = JSON.parse(sessionStorage.getItem(STORAGE_KEY));
    if (import.meta.env.DEV && s?.bootId && s.bootId !== __DEV_BOOT_ID__) {
      // Hủy phiên cũ khi dev server khởi động lại
      sessionStorage.removeItem(STORAGE_KEY);
      return null;
    }
    return s;
  } catch {
    return null;
  }
})();

const STAFF_ROLES = ["admin", "nhan_vien", "quan_kho"];

export const useAuthStore = defineStore("auth", {
  state: () => ({
    user: saved ?? null,
    isAdmin: STAFF_ROLES.includes(saved?.role) || false,
  }),
  actions: {
    setSession(user) {
      this.user = user;
      this.isAdmin = STAFF_ROLES.includes(user.role);
      const session = { ...user };
      if (import.meta.env.DEV) session.bootId = __DEV_BOOT_ID__;
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    },
    clearSession() {
      this.user = null;
      this.isAdmin = false;
      sessionStorage.removeItem(STORAGE_KEY);
    },
  },
});

export const AuthStore = useAuthStore(pinia);
export const setSession = (user) => AuthStore.setSession(user);
export const clearSession = () => AuthStore.clearSession();
