import "bootstrap/dist/css/bootstrap.min.css";
import "leaflet/dist/leaflet.css";
import "./assets/main.css";
import "./assets/theme.css";
import "./stores/theme.js"; // áp dụng theme đã lưu trước khi app mount, tránh nháy màu

import { createApp } from "vue";
import { createHead } from "@unhead/vue";
import App from "./App.vue";
import router from "./router/index.js";
import { pinia } from "./stores/pinia.js";

const app = createApp(App);
const head = createHead();
app.use(head);

app.config.errorHandler = (err, instance, info) => {
  console.error("[Global Error]", err, info);
};

window.addEventListener("unhandledrejection", (event) => {
  console.error("[Unhandled Rejection]", event.reason);
});

app.use(pinia).use(router).mount("#app");
