import { get } from './api.js';

// Lấy tất cả sản phẩm thuộc 1 đơn hàng
export const getByDonHang = (donHangId) => get(`/api/chi-tiet-don-hang/don-hang/${donHangId}`);
