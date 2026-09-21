import { get, post, put, patch, del, authHeaders } from './api.js';

// Tính phí vận chuyển
export const tinhPhiVanChuyen = (body) => post('/api/don-hang/tinh-phi-van-chuyen', body);

// Lấy danh sách đơn hàng phân trang
export const getPage = ({ page = 0, size = 20, khachHangId } = {}) => {
  const params = new URLSearchParams({ page, size });
  if (khachHangId) params.set('khachHangId', khachHangId);
  return get(`/api/don-hang?${params}`);
};

// Lấy toàn bộ đơn hàng
export const getAll = () => getPage({ size: 200 }).then((p) => p.content);

// Lấy danh sách đơn hàng theo khách hàng
export const getByKhachHang = (khachHangId) =>
  getPage({ size: 200, khachHangId }).then((p) => p.content);

// Tạo đơn hàng
export const create = (body) => post('/api/don-hang', body);

// Tạo đơn hàng hoàn chỉnh (checkout)
export const checkoutComplete = (body) => post('/api/don-hang/checkout-complete', body);

// Cập nhật đơn hàng
export const update = (id, body) => put(`/api/don-hang/update/${id}`, body);

// Xóa đơn hàng
export const remove = (id) => del(`/api/don-hang/delete/${id}`);

// Tính lại tổng tiền đơn hàng
export const recalculate = (id) => fetch(`/api/don-hang/${id}/recalculate`, { method: 'PATCH', headers: authHeaders() });

// Gộp đơn hàng
export const merge = (targetId, sourceIds) =>
  post('/api/don-hang/merge', { targetId, sourceIds });

// Thêm sản phẩm vào đơn hàng
export const addChiTiet = (body) => post('/api/chi-tiet-don-hang', body);

// Xác nhận đơn online và gán serial
export const xacNhan = (donHangId, body) => patch(`/api/don-hang/${donHangId}/xac-nhan`, body);

// Khách hàng xác nhận đã nhận hàng
export const xacNhanDaNhanHang = (donHangId) => patch(`/api/don-hang/${donHangId}/xac-nhan-nhan-hang`, {});

// ── POS helpers ────────────────────────────────────────────────────────────────

// Đơn hàng gần đây cho POS
export const getRecentForPos = () => get('/api/don-hang/pos/recent');

// Top khách hàng chi tiêu nhiều nhất
export const getTopCustomers = (limit = 5) => get(`/api/don-hang/pos/top-customers?limit=${limit}`);

// Đơn hàng gần nhất của một khách hàng
export const getRecentByKhachHang = (khachHangId) => get(`/api/don-hang/pos/customer/${khachHangId}/orders`);
