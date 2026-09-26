import { get, post } from './api.js';

// Tạo bản ghi thanh toán cho đơn hàng
export const create = (body) => post('/api/thanh-toan', body);

// Lấy danh sách bản ghi thanh toán theo mã đơn hàng
export const getByDonHang = (donHangId) => get(`/api/thanh-toan/don-hang/${donHangId}`);

/**
 * Nhân viên xác nhận đã nhận tiền thủ công (QR / Visa).
 * @param {number} donHangId
 * @param {object} info - { soTien?, maGiaoDich?, phuongThuc? }
 */
export const confirmPayment = (donHangId, info = {}) =>
  post('/api/payment/confirm', { donHangId, ...info });
