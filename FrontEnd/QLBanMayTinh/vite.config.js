import { fileURLToPath, URL } from "node:url";

import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  // ID đổi mỗi lần chạy lại `npm run dev` (giữ nguyên khi chỉ reload trang trong
  // cùng 1 lần dev server đang chạy) — dùng để tự đăng xuất đúng lúc dev server
  // restart, không đăng xuất oan khi F5.
  define: {
    __DEV_BOOT_ID__: JSON.stringify(Date.now().toString()),
  },
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  server: {
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
        secure: false,
      },
    },
  },
});
