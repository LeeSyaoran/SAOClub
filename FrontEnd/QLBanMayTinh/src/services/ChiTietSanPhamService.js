import { get, post, put, del } from './api.js';

export const getAll = () => get(`/api/chi-tiet-san-pham`);

// Lấy serial của 1 biến thể — dùng thay getAll() khi chỉ cần 1 biến thể
export const getByBienThe = (bienTheId) => get(`/api/chi-tiet-san-pham/bien-the/${bienTheId}`);

export const create = (body) => post('/api/chi-tiet-san-pham', body);

export const update = (id, body) => put(`/api/chi-tiet-san-pham/update/${id}`, body);

// Chỉ xóa được serial đang "trong_kho" (thêm nhầm) — server chặn nếu đã bán/đã dùng.
export const remove = (id) => del(`/api/chi-tiet-san-pham/delete/${id}`);

// Serial đã bán còn trong hạn bảo hành (server tự lọc theo ngày, hết hạn tự rớt khỏi danh sách).
export const getUnderWarranty = () => get('/api/chi-tiet-san-pham/con-bao-hanh');
