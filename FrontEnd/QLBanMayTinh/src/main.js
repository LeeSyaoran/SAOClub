import "bootstrap/dist/css/bootstrap.min.css";
import "./assets/main.css";
import "./assets/theme.css";
import "./stores/theme.js"; // áp dụng theme đã lưu trước khi app mount, tránh nháy màu

import { createApp } from "vue";
import App from "./App.vue";

createApp(App).mount("#app");
