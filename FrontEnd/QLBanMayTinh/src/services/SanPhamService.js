import { get, post, put, del } from './api.js';

// Backend giờ trả Page<SanPhamResponse> (đã phân trang) thay vì List đầy đủ như trước —
// xem BackEnd/.../SanPhamController.getAll(). getPage() gọi thẳng, trả nguyên object Page
// ({content, totalElements, totalPages, number,...}) cho bảng có nút Trước/Sau + filter.
export const getPage = ({ page = 0, size = 20, keyword, danhMucId, thuongHieuId, trangThai } = {}) => {
  const params = new URLSearchParams({ page, size });
  if (keyword) params.set('keyword', keyword);
  if (danhMucId) params.set('danhMucId', danhMucId);
  if (thuongHieuId) params.set('thuongHieuId', thuongHieuId);
  if (trangThai) params.set('trangThai', trangThai);
  return get(`/api/san-pham/hien-thi?${params}`);
};

// Tương thích các chỗ đang cần "toàn bộ" sản phẩm trong bộ nhớ (dashboard, POS, dropdown
// biến thể...) — lấy 1 trang lớn rồi trả thẳng mảng .content, không đổi chữ ký hàm cũ.
export const getAll = () => getPage({ size: 200 }).then((p) => p.content);

export const save = (id, body) =>
  id ? put(`/api/san-pham/update/${id}`, body) : post('/api/san-pham', body);

export const remove = (id) => del(`/api/san-pham/delete/${id}`);

// Đã có biến thể nào qua giao dịch chưa — gọi trước khi hiện hộp thoại xóa.
export const hasTransactionHistory = (id) => get(`/api/san-pham/${id}/co-giao-dich`);
