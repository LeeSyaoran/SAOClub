import { get } from './api.js';

// Lấy danh sách serial đã gán hoặc giữ chỗ theo mã đơn hàng
export const getByDonHang = (donHangId) => get(`/api/chi-tiet-don-hang/don-hang/${donHangId}/serials`);
