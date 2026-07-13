import { get } from './api.js';

// Toàn bộ serial đang giữ chỗ/đã gán cho từng dòng của 1 đơn — dùng để load lại lựa chọn
// đã có sẵn khi mở modal "Chọn serial trước khi đóng gói".
export const getByDonHang = (donHangId) => get(`/api/chi-tiet-don-hang/don-hang/${donHangId}/serials`);
