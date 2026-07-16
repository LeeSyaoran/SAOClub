import { get } from './api.js';

// KPI + xếp hạng bán chạy/bán chậm — tính bằng SQL (SUM/COUNT/GROUP BY) ở backend,
// thay vì tải toàn bộ san-pham/don-hang/chi-tiet-don-hang về rồi cộng dồn bằng JS.
export const getKpi = () => get('/api/dashboard/kpi');

// tuNgay/denNgay (chuỗi 'YYYY-MM-DD') optional — không truyền = không lọc theo ngày,
// dùng cho tab Dashboard (không đổi hành vi cũ). Tab Báo cáo truyền kèm ngày để lọc.
const dateParams = (tuNgay, denNgay) => {
  const p = new URLSearchParams();
  if (tuNgay) p.set('tuNgay', tuNgay);
  if (denNgay) p.set('denNgay', denNgay);
  return p.toString();
};

export const getTopSelling = (limit = 5, tuNgay, denNgay) =>
  get(`/api/dashboard/top-selling?limit=${limit}&${dateParams(tuNgay, denNgay)}`);

export const getSlowSelling = (limit = 5, tuNgay, denNgay) =>
  get(`/api/dashboard/slow-selling?limit=${limit}&${dateParams(tuNgay, denNgay)}`);

// Doanh thu theo ngày trong khoảng — cho biểu đồ cột ở tab Báo cáo. tuNgay/denNgay bắt buộc.
export const getRevenueByDay = (tuNgay, denNgay) =>
  get(`/api/dashboard/doanh-thu-theo-ngay?tuNgay=${tuNgay}&denNgay=${denNgay}`);

// Top khách chi tiêu nhiều nhất + tỷ lệ mua lại trong khoảng. tuNgay/denNgay bắt buộc.
export const getCustomerReport = (tuNgay, denNgay, limit = 5) =>
  get(`/api/dashboard/khach-hang-noi-bat?tuNgay=${tuNgay}&denNgay=${denNgay}&limit=${limit}`);
