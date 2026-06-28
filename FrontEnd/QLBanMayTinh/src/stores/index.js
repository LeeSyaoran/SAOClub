import { reactive } from "vue";

const STORAGE_KEY = "saophone_session";

const saved = (() => {
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY)); }
  catch { return null; }
})();

const STAFF_ROLES = ["admin", "nhan_vien", "quan_kho"];

// AuthStore — dùng cho cả customer và staff/admin
export const AuthStore = reactive({
  user:    saved ?? null,
  isAdmin: STAFF_ROLES.includes(saved?.role) || false,
});

// Gọi sau khi login thành công
export const setSession = (user) => {
  AuthStore.user    = user;
  AuthStore.isAdmin = STAFF_ROLES.includes(user.role);
  localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
};

// Gọi khi logout
export const clearSession = () => {
  AuthStore.user    = null;
  AuthStore.isAdmin = false;
  localStorage.removeItem(STORAGE_KEY);
};
