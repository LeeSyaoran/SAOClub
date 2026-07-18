import { reactive } from "vue";
import * as DonHangService from "../Service/DonHangService.js";

// ── Orders Store — dữ liệu đơn hàng dùng chung (OrdersTable, PosPanel). Có SSE realtime:
// connectOrderEvents() mở 1 kết nối EventSource, tự patch OrdersStore.items khi có đơn mới/
// đơn cập nhật — mọi trang gọi connectOrderEvents() đều thấy cùng dữ liệu, không lệch nhau
// như khi mỗi trang tự fetch + tự mở SSE riêng. ──
export const OrdersStore = reactive({ items: [], loading: false, loaded: false });

let ordersPromise = null;
export const ensureOrders = () => {
  if (ordersPromise) return ordersPromise;
  ordersPromise = refreshOrders();
  return ordersPromise;
};

export const refreshOrders = async () => {
  OrdersStore.loading = true;
  try {
    OrdersStore.items = await DonHangService.getAll().catch(() => []);
    OrdersStore.loaded = true;
  } finally {
    OrdersStore.loading = false;
  }
  return OrdersStore.items;
};

let eventSource = null;
let subscriberCount = 0;

// Gọi trong onMounted của mỗi trang dùng đơn hàng realtime (Admin/Staff). Đếm số trang đang
// mở (subscriberCount) — chỉ mở/đóng EventSource thật khi trang cuối cùng unmount, để 2 trang
// mở cùng lúc (hiếm nhưng có thể, vd 2 tab) không tranh nhau mở 2 kết nối SSE trùng lặp.
export const connectOrderEvents = (token) => {
  subscriberCount += 1;
  if (eventSource) return;
  eventSource = new EventSource(`/api/don-hang/events?token=${encodeURIComponent(token ?? '')}`);
  eventSource.onerror = (e) => console.error('Kết nối SSE (đơn hàng real-time) lỗi:', e);
  eventSource.addEventListener('new-order', () => { refreshOrders(); });
  eventSource.addEventListener('order-updated', () => { refreshOrders(); });
};

export const disconnectOrderEvents = () => {
  subscriberCount = Math.max(0, subscriberCount - 1);
  if (subscriberCount === 0 && eventSource) {
    eventSource.close();
    eventSource = null;
  }
};
