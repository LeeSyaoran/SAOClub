/// <reference types="vitest" />
import { fileURLToPath, URL } from "node:url";

import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { VitePWA } from "vite-plugin-pwa";

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    VitePWA({
      registerType: "autoUpdate",
      includeAssets: ["images/**/*"],
      manifest: {
        name: "SAOPhone",
        short_name: "SAOPhone",
        description: "Cửa hàng laptop & thiết bị công nghệ",
        theme_color: "#0f0d1a",
        background_color: "#0f0d1a",
        display: "standalone",
        icons: [
          {
            src: "/images/icon-192.png",
            sizes: "192x192",
            type: "image/png",
          },
          {
            src: "/images/icon-512.png",
            sizes: "512x512",
            type: "image/png",
          },
        ],
      },
      workbox: {
        globPatterns: ["**/*.{js,css,html,ico,png,svg,webp}"],
        maximumFileSizeToCacheInBytes: 10 * 1024 * 1024,
      },
    }),
  ],
  build: {
    minify: "esbuild",
    cssMinify: true,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes("node_modules/vue") || id.includes("node_modules/pinia") || id.includes("node_modules/vue-router")) return "vendor";
          if (id.includes("node_modules/bootstrap")) return "bootstrap";
        },
      },
    },
  },
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
  test: {
    globals: true,
    environment: "jsdom",
    include: ["src/**/*.{test,spec}.{js,ts}"],
  },
});
