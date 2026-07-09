import { reactive } from "vue";

// ── Theme Store — sáng/tối, lưu vào localStorage ────────────────────────────
const STORAGE_KEY = "saophone_theme";

const saved = localStorage.getItem(STORAGE_KEY);
const initialMode = saved === "light" ? "light" : "dark";

export const ThemeStore = reactive({ mode: initialMode });

const applyToDom = (mode) => {
  document.documentElement.setAttribute("data-theme", mode);
  // Đồng bộ luôn Bootstrap color mode — nếu không, các component Bootstrap
  // dùng biến CSS riêng (alert, form control...) sẽ đứng yên ở dark bất kể
  // theme của app, vì index.html khai báo data-bs-theme="dark" cố định.
  document.documentElement.setAttribute("data-bs-theme", mode);
};

export const setTheme = (mode) => {
  ThemeStore.mode = mode === "light" ? "light" : "dark";
  localStorage.setItem(STORAGE_KEY, ThemeStore.mode);
  applyToDom(ThemeStore.mode);
};

export const toggleTheme = () => setTheme(ThemeStore.mode === "dark" ? "light" : "dark");

// Áp dụng ngay khi module được nạp (trước khi app mount) để tránh nháy màu
applyToDom(ThemeStore.mode);
