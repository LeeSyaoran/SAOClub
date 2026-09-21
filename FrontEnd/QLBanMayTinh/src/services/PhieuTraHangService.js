import { get, post, put } from './api.js';

export const getAll = () => get(`/api/phieu-tra-hang`);

export const getById = (id) => get(`/api/phieu-tra-hang/${id}`);

export const save = (id, body) =>
  id ? put(`/api/phieu-tra-hang/update/${id}`, body) : post('/api/phieu-tra-hang', body);

// Khách hàng gửi yêu cầu đổi trả hàng
export const taoYeuCau = (body) => post('/api/phieu-tra-hang/tu-yeu-cau', body);

// Lấy danh sách yêu cầu đổi trả theo mã đơn hàng
export const getByDonHang = (donHangId) => get(`/api/phieu-tra-hang/don-hang/${donHangId}`);
