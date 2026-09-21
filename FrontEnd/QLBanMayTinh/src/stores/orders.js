import { reactive } from "vue";
import * as DonHangService from "../services/DonHangService.js";

// Store quản lý dữ liệu đơn hàng dùng chung
export const OrdersStore = reactive({ items: [], loading: false, loaded: false });

let ordersPromise = null;

// Reset dữ liệu đơn hàng
export const resetOrders = () => {
  ordersPromise = null;
  OrdersStore.items = [];
  OrdersStore.loaded = false;
};

// Đảm bảo đơn hàng đã được tải
export const ensureOrders = () => {
  if (ordersPromise) return ordersPromise;
  ordersPromise = refreshOrders();
  return ordersPromise;
};

// Tải lại danh sách đơn hàng
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

// Kết nối SSE nhận sự kiện đơn hàng realtime
export const connectOrderEvents = (token, { onNewOrder, onOrderUpdated } = {}) => {
  subscriberCount += 1;
  if (eventSource) return;
  document.cookie = `sse_token=${encodeURIComponent(token ?? '')}; path=/api/don-hang; SameSite=Strict`;
  eventSource = new EventSource('/api/don-hang/events');
  eventSource.onerror = (e) => console.error('Kết nối SSE (đơn hàng real-time) lỗi:', e);
  eventSource.addEventListener('new-order', () => { refreshOrders(); onNewOrder?.(); });
  eventSource.addEventListener('order-updated', () => { refreshOrders(); onOrderUpdated?.(); });
};

// Ngắt kết nối SSE khi rời trang
export const disconnectOrderEvents = () => {
  subscriberCount = Math.max(0, subscriberCount - 1);
  if (subscriberCount === 0 && eventSource) {
    eventSource.close();
    eventSource = null;
    document.cookie = 'sse_token=; path=/api/don-hang; expires=Thu, 01 Jan 1970 00:00:00 GMT; SameSite=Strict';
  }
};
