import { get, post, put, del } from './api.js';

// Lấy danh sách sản phẩm có phân trang và bộ lọc
export const getPage = ({ page = 0, size = 20, keyword, danhMucId, thuongHieuId, trangThai } = {}) => {
  const params = new URLSearchParams({ page, size });
  if (keyword) params.set('keyword', keyword);
  if (danhMucId) params.set('danhMucId', danhMucId);
  if (thuongHieuId) params.set('thuongHieuId', thuongHieuId);
  if (trangThai) params.set('trangThai', trangThai);
  return get(`/api/san-pham/hien-thi?${params}`);
};

// Lấy danh sách tất cả sản phẩm
export const getAll = () => getPage({ size: 200 }).then((p) => p.content);

export const save = (id, body) =>
  id ? put(`/api/san-pham/update/${id}`, body) : post('/api/san-pham', body);

export const remove = (id) => del(`/api/san-pham/delete/${id}`);

// Kiểm tra sản phẩm đã có lịch sử giao dịch chưa
export const hasTransactionHistory = (id) => get(`/api/san-pham/${id}/co-giao-dich`);

export const getLichSu = (id) => get(`/api/san-pham/${id}/lich-su`);

// Lấy danh sách hình ảnh chi tiết của sản phẩm
export const getHinhAnh = (id) => get(`/api/san-pham/${id}/hinh-anh`);

// Lấy tất cả sản phẩm cho dropdown chọn (khuyến mãi, etc.)
export const getDanhSachChon = () => get(`/api/san-pham/danh-sach-chon`);
