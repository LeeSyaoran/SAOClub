import { get } from './api.js';

// Lấy các chỉ số KPI tổng quan
export const getKpi = () => get('/api/dashboard/kpi');

// Tạo tham số truy vấn ngày
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

// Lấy dữ liệu doanh thu theo ngày
export const getRevenueByDay = (tuNgay, denNgay) =>
  get(`/api/dashboard/doanh-thu-theo-ngay?tuNgay=${tuNgay}&denNgay=${denNgay}`);

// Lấy báo cáo khách hàng nổi bật
export const getCustomerReport = (tuNgay, denNgay, limit = 5) =>
  get(`/api/dashboard/khach-hang-noi-bat?tuNgay=${tuNgay}&denNgay=${denNgay}&limit=${limit}`);
