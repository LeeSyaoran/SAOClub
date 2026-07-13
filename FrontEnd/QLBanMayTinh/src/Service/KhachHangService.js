import { get, post, put, del } from './api.js';

export const getAll = () => get('/api/khach-hang');

export const getById = (id) => get(`/api/khach-hang/${id}`);

export const save = (id, body) =>
  id ? put(`/api/khach-hang/update/${id}`, body) : post('/api/khach-hang', body);

export const remove = (id) => del(`/api/khach-hang/delete/${id}`);

export const login = (username, password) =>
  post('/api/khach-hang/login', { username, password });

export const register = (body) =>
  post('/api/khach-hang/register', body);

// Tra cứu theo SĐT cho checkout (khách vãng lai lẫn đã đăng nhập) — khác getAll() (chỉ
// nhân viên/admin gọi được), endpoint này công khai, chỉ trả đúng 1 khách khớp SĐT.
export const findByPhone = (soDienThoai) =>
  get(`/api/khach-hang/tim-theo-sdt?soDienThoai=${encodeURIComponent(soDienThoai)}`);

// Tạo khách vãng lai lúc checkout (không mật khẩu) — khác save() (POST /api/khach-hang,
// chỉ nhân viên/admin gọi được) và register() (bắt buộc username/password).
export const createGuest = (body) => post('/api/khach-hang/khach-vang-lai', body);
