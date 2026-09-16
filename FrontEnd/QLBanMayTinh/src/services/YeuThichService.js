import { get, post, del } from './api.js';

// Danh sách yêu thích của chính mình (khách hàng đăng nhập)
export const getAll = () => get('/api/yeu-thich');

// Danh sách yêu thích của khách hàng (staff xem theo khachHangId)
export const getByCustomer = (khachHangId) => get(`/api/yeu-thich?khachHangId=${khachHangId}`);

export const add = (bienTheId) => post(`/api/yeu-thich/${bienTheId}`);

export const remove = (bienTheId) => del(`/api/yeu-thich/${bienTheId}`);
