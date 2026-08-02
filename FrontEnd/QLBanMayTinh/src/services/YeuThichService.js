import { get, post, del } from './api.js';

// Danh sách yêu thích — chỉ dành cho khách hàng đã đăng nhập (server tự suy khách hàng qua
// JWT, không cần/không nên gửi khachHangId từ client — xem SanPhamYeuThichService.java).
export const getAll = () => get('/api/yeu-thich');

export const add = (bienTheId) => post(`/api/yeu-thich/${bienTheId}`);

export const remove = (bienTheId) => del(`/api/yeu-thich/${bienTheId}`);
