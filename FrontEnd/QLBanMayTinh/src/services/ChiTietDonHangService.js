import { get } from './api.js';

// Lấy tất cả sản phẩm thuộc 1 đơn hàng
export const getByDonHang = (donHangId) => get(`/api/chi-tiet-don-hang/don-hang/${donHangId}`);

// Lấy toàn bộ chi tiết đơn hàng trong 1 lần gọi (dùng cho thống kê tổng hợp,
// tránh gọi getByDonHang() lặp lại cho từng đơn — rất chậm khi có nhiều đơn hàng)
export const getAll = () => get(`/api/chi-tiet-don-hang`);
