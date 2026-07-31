import { get } from './api.js';

// Lấy lịch sử thay đổi trạng thái của 1 đơn hàng, theo mốc thời gian tăng dần.
export const getByDonHang = (donHangId) => get(`/api/lich-su-don-hang/don-hang/${donHangId}`);
