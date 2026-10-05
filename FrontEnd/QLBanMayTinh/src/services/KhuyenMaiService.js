import { get, post, put } from './api.js';

export const getAll = () => get(`/api/khuyen-mai`);

export const save = (id, body) =>
  id ? put(`/api/khuyen-mai/update/${id}`, body) : post('/api/khuyen-mai', body);

// Kiểm tra tính hợp lệ của mã khuyến mãi
export const kiemTra = (maKhuyenMai) =>
  post(`/api/khuyen-mai/kiem-tra`, { maKhuyenMai });

// Lấy danh sách sản phẩm áp dụng cho một khuyến mãi
export const getSanPhamApDung = (khuyenMaiId) =>
  get(`/api/khuyen-mai/${khuyenMaiId}/san-pham`);

// Lấy danh sách khách hàng được nhận voucher
export const getKhachHangNhanVoucher = (khuyenMaiId) =>
  get(`/api/khuyen-mai/${khuyenMaiId}/khach-hang`);
