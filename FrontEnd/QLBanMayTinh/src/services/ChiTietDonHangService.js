import { get } from './api.js';

// Lấy tất cả sản phẩm thuộc 1 đơn hàng
export const getByDonHang = (donHangId) => get(`/api/chi-tiet-don-hang/don-hang/${donHangId}`);

// Lấy tất cả chi tiết đơn hàng phục vụ thống kê tổng hợp
export const getAll = () => get(`/api/chi-tiet-don-hang`);
