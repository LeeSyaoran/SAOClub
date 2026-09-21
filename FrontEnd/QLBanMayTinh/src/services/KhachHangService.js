import { get, post, put } from './api.js';

export const getPage = ({ page = 0, size = 50 } = {}) => get(`/api/khach-hang?page=${page}&size=${size}`);

export const getAll = () => getPage({ size: 200 }).then((p) => p.content);

export const getById = (id) => get(`/api/khach-hang/${id}`);

export const save = (id, body) =>
  id ? put(`/api/khach-hang/update/${id}`, body) : post('/api/khach-hang', body);

export const login = (username, password) =>
  post('/api/khach-hang/login', { username, password });

export const register = (body) =>
  post('/api/khach-hang/register', body);

// Tìm kiếm khách hàng theo số điện thoại
export const findByPhone = (soDienThoai) =>
  get(`/api/khach-hang/tim-theo-sdt?soDienThoai=${encodeURIComponent(soDienThoai)}`);

// Tạo khách hàng vãng lai khi thanh toán
export const createGuest = (body) => post('/api/khach-hang/khach-vang-lai', body);

// Admin tặng điểm cho 1 khách hàng — body: { soDiem, lyDo }
export const tangDiem = (id, body) => post(`/api/khach-hang/${id}/tang-diem`, body);

// Admin xem lịch sử tặng điểm của 1 khách hàng
export const getLichSuDiem = (id) => get(`/api/khach-hang/${id}/lich-su-diem`);
