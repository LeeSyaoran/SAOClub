import { get, post, put, del } from './api.js';

export const getAll = () => get(`/api/phieu-tra-hang`);

export const getById = (id) => get(`/api/phieu-tra-hang/${id}`);

export const save = (id, body) =>
  id ? put(`/api/phieu-tra-hang/update/${id}`, body) : post('/api/phieu-tra-hang', body);

export const remove = (id) => del(`/api/phieu-tra-hang/delete/${id}`);

// Khách hàng tự gửi yêu cầu trả hàng — body: { donHangId, lyDo, dongTra: [{ chiTietDonHangId, soLuong }] }
export const taoYeuCau = (body) => post('/api/phieu-tra-hang/tu-yeu-cau', body);

// Lấy các yêu cầu trả hàng của 1 đơn — dùng cho AccountPage hiện trạng thái xử lý.
export const getByDonHang = (donHangId) => get(`/api/phieu-tra-hang/don-hang/${donHangId}`);
