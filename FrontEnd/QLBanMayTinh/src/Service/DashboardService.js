import { get } from './api.js';

// KPI + xếp hạng bán chạy/bán chậm — tính bằng SQL (SUM/COUNT/GROUP BY) ở backend,
// thay vì tải toàn bộ san-pham/don-hang/chi-tiet-don-hang về rồi cộng dồn bằng JS.
export const getKpi = () => get('/api/dashboard/kpi');
export const getTopSelling = (limit = 5) => get(`/api/dashboard/top-selling?limit=${limit}`);
export const getSlowSelling = (limit = 5) => get(`/api/dashboard/slow-selling?limit=${limit}`);
