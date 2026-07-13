import { get, post, put, patch, del, authHeaders } from './api.js';

// Backend giờ trả Page<DonHangResponse> — xem BackEnd/.../DonHangController.getAll().
// getPage() trả nguyên object Page cho bảng Đơn hàng có nút Trước/Sau.
// khachHangId optional: lọc sẵn ở server, dùng cho trang "Đơn hàng của tôi" (AccountPage)
// để khỏi tải hết đơn toàn hệ thống rồi lọc ở trình duyệt.
export const getPage = ({ page = 0, size = 20, khachHangId } = {}) => {
  const params = new URLSearchParams({ page, size });
  if (khachHangId) params.set('khachHangId', khachHangId);
  return get(`/api/don-hang?${params}`);
};

// Tương thích các chỗ đang cần "toàn bộ" đơn hàng trong bộ nhớ (dashboard, auto-merge,
// tra cứu...) — lấy 1 trang lớn rồi trả thẳng mảng .content.
export const getAll = () => getPage({ size: 5000 }).then((p) => p.content);

// Toàn bộ đơn hàng của 1 khách hàng cụ thể — dùng cho AccountPage thay vì getAll().
export const getByKhachHang = (khachHangId) =>
  getPage({ size: 5000, khachHangId }).then((p) => p.content);

export const create = (body) => post('/api/don-hang', body);

export const update = (id, body) => put(`/api/don-hang/update/${id}`, body);

export const remove = (id) => del(`/api/don-hang/delete/${id}`);

// Tính lại tong_tien sau khi thêm/xóa sản phẩm trong đơn
export const recalculate = (id) => fetch(`/api/don-hang/${id}/recalculate`, { method: 'PATCH', headers: authHeaders() });

// Gộp nhiều đơn vào 1 đơn đích
export const merge = (targetId, sourceIds) =>
  post('/api/don-hang/merge', { targetId, sourceIds });

export const addChiTiet = (body) => post('/api/chi-tiet-don-hang', body);

// Chọn serial cho từng dòng + chốt bán + chuyển đơn sang "processing" (đóng gói) — chỉ
// dùng cho đơn online. body: { lines: [{ chiTietDonHangId, serialIds: [...] }] }
export const dongGoi = (donHangId, body) => patch(`/api/don-hang/${donHangId}/dong-goi`, body);
