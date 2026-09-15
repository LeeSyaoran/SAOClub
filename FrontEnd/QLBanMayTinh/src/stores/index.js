import { defineStore } from "pinia";
import { pinia } from "./pinia.js";

const STORAGE_KEY = "saophone_session";

// Lưu bootId trong session object — so sánh với __DEV_BOOT_ID__ để phát hiện
// dev server restart (bundle mới) mà không bị ảnh hưởng bởi F5 trong cùng bundle.
// __DEV_BOOT_ID__ = Date.now() tại thời điểm bundle, thay đổi khi restart dev
// nhưng giữ nguyên khi chỉ reload trang (cùng bundle đang chạy).
const saved = (() => {
  try {
    const s = JSON.parse(sessionStorage.getItem(STORAGE_KEY));
    if (import.meta.env.DEV && s?.bootId && s.bootId !== __DEV_BOOT_ID__) {
      // Dev server restarted between sessions — invalidate old session.
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
