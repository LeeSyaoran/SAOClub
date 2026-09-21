import { get, post } from './api.js';

// Tạo bản ghi thanh toán cho đơn hàng
export const create = (body) => post('/api/thanh-toan', body);

// Lấy danh sách bản ghi thanh toán theo mã đơn hàng
export const getByDonHang = (donHangId) => get(`/api/thanh-toan/don-hang/${donHangId}`);
