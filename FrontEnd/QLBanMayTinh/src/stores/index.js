import { defineStore } from "pinia";
import { pinia } from "./pinia.js";

const STORAGE_KEY = "saophone_session";

if (import.meta.env.DEV) {
  const BOOT_ID_KEY = "saophone_dev_boot_id";
  if (localStorage.getItem(BOOT_ID_KEY) !== __DEV_BOOT_ID__) {
    sessionStorage.removeItem(STORAGE_KEY);
    localStorage.setItem(BOOT_ID_KEY, __DEV_BOOT_ID__);
  }
}

const saved = (() => {
  try { return JSON.parse(sessionStorage.getItem(STORAGE_KEY)); }
  catch { return null; }
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
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(user));
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
