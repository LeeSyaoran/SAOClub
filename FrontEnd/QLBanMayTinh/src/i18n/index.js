import { reactive } from "vue";
import vi from "./locales/vi.js";
import en from "./locales/en.js";

const MESSAGES = { vi, en };

export const LOCALES = [
  { code: "vi", label: "Tiếng Việt", flag: "🇻🇳" },
  { code: "en", label: "English",    flag: "🇬🇧" },
];

const STORAGE_KEY = "saophone_locale";
const saved = localStorage.getItem(STORAGE_KEY);

export const I18nStore = reactive({
  locale: MESSAGES[saved] ? saved : "vi",
});

export const setLocale = (code) => {
  if (!MESSAGES[code]) return;
  I18nStore.locale = code;
  localStorage.setItem(STORAGE_KEY, code);
};

// Tìm giá trị theo đường dẫn "a.b.c" trong object lồng nhau
const resolve = (obj, path) => path.split(".").reduce((o, k) => (o == null ? undefined : o[k]), obj);

// t('nav.login') → chuỗi đã dịch theo locale hiện tại (fallback: vi, rồi chính key đó)
export const t = (key, vars) => {
  let msg = resolve(MESSAGES[I18nStore.locale], key);
  if (msg == null) msg = resolve(MESSAGES.vi, key);
  if (msg == null) return key;
  if (!vars) return msg;
  return Object.keys(vars).reduce((s, k) => s.replaceAll(`{${k}}`, vars[k]), msg);
};

// Áp dụng ngôn ngữ mặc định hệ thống (Cài đặt) CHỈ khi người dùng chưa từng tự chọn ngôn
// ngữ ở trình duyệt này (chưa có key trong localStorage) — không ghi đè lựa chọn đã có.
export const applySystemDefaultLocale = (code) => {
  if (!localStorage.getItem(STORAGE_KEY) && MESSAGES[code]) {
    setLocale(code);
  }
};
