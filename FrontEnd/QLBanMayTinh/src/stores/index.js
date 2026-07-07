import { reactive } from "vue";

// sessionStorage (không phải localStorage) — mỗi tab có phiên đăng nhập riêng.
// Dùng localStorage sẽ bị 2 tab đăng nhập 2 vai trò khác nhau (vd 1 tab khách
// hàng, 1 tab admin) ghi đè phiên của nhau, tab kia reload sẽ ra sai/trắng trang.
const STORAGE_KEY = "saophone_session";

// Dev: chỉ bắt đăng nhập lại khi `npm run dev` được chạy lại (dev server restart —
// __DEV_BOOT_ID__ đổi giá trị), KHÔNG đăng xuất khi chỉ tải lại trang (F5) trong
// lúc dev server vẫn đang chạy. Bản build thật (production) không bị ảnh hưởng.
// Mã boot vẫn dùng localStorage (dùng chung cho mọi tab là đúng ý — restart dev
// server thì tab nào cũng cần đăng nhập lại).
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

// AuthStore — dùng cho cả customer và staff/admin
export const AuthStore = reactive({
  user:    saved ?? null,
  isAdmin: STAFF_ROLES.includes(saved?.role) || false,
});

// Gọi sau khi login thành công
export const setSession = (user) => {
  AuthStore.user    = user;
  AuthStore.isAdmin = STAFF_ROLES.includes(user.role);
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(user));
};

// Gọi khi logout
export const clearSession = () => {
  AuthStore.user    = null;
  AuthStore.isAdmin = false;
  sessionStorage.removeItem(STORAGE_KEY);
};
