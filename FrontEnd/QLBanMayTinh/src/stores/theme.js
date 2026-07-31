import { defineStore } from "pinia";
import { pinia } from "./pinia.js";

const STORAGE_KEY = "saophone_theme";

const saved = localStorage.getItem(STORAGE_KEY);
const initialMode = saved === "light" ? "light" : "dark";

const applyToDom = (mode) => {
  document.documentElement.setAttribute("data-theme", mode);
  document.documentElement.setAttribute("data-bs-theme", mode);
};

export const useThemeStore = defineStore("theme", {
  state: () => ({ mode: initialMode }),
  actions: {
    setTheme(mode) {
      const m = mode === "light" ? "light" : "dark";
      this.mode = m;
      localStorage.setItem(STORAGE_KEY, m);
      applyToDom(m);
    },
    toggleTheme() {
      this.setTheme(this.mode === "dark" ? "light" : "dark");
    },
  },
});

export const ThemeStore = useThemeStore(pinia);
export const setTheme = (mode) => ThemeStore.setTheme(mode);
export const toggleTheme = () => ThemeStore.toggleTheme();

applyToDom(ThemeStore.mode);
