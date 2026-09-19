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
      // Tắt PWA hoàn toàn trong dev để tránh service worker can thiệp asset loading.
      disable: true,
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
    // Bind-mount qua Docker Desktop trên Windows không phát event fs native đáng tin cậy —
    // Vite/chokidar không tự nhận file đổi (sửa code không thấy cập nhật dù đã lưu). Polling
    // là fallback chuẩn cho trường hợp này, không tốn gì đáng kể khi chạy native.
    watch: {
      usePolling: true,
    },
    proxy: {
      "/api": {
        // Docker: backend chạy ở container riêng ("localhost" bên trong container frontend
        // là chính nó, không phải container backend) — set VITE_API_PROXY_TARGET=http://backend:8080
        // qua docker-compose. Dev native không set biến này thì giữ nguyên localhost:8080.
        target: process.env.VITE_API_PROXY_TARGET || "http://localhost:8080",
        changeOrigin: true,
        secure: false,
      },
      // Lưu ý: KHÔNG proxy /images qua backend. Vite serve trực tiếp public/images và
      // browser sẽ gọi backend /api/images/upload/... cho ảnh động. Proxy ở đây khiến
      // Vite cố "import" SVG trong /public/images và gây lỗi MIME type trong trình duyệt.
    },
    headers: {
      "Content-Security-Policy": [
        "default-src 'self'",
        "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://www.gstatic.com https://accounts.google.com https://apis.google.com https://*.firebaseapp.com blob:",
        "worker-src 'self' blob:",
        "child-src 'self' blob:",
        "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
        "img-src 'self' data: https: blob: https://*.googleusercontent.com",
        "font-src 'self' data: https://fonts.gstatic.com",
        "connect-src 'self' https://www.gstatic.com https://accounts.google.com https://apis.google.com https://oauth2.googleapis.com https://securetoken.googleapis.com https://identitytoolkit.googleapis.com https://www.googleapis.com https://*.firebaseapp.com http://localhost:* ws://localhost:*",
        "frame-src 'self' https://accounts.google.com https://*.firebaseapp.com https://*.googleapis.com https://*.googleusercontent.com",
      ].join("; "),
    },
  },
  test: {
    globals: true,
    environment: "jsdom",
    include: ["src/**/*.{test,spec}.{js,ts}"],
  },
});
