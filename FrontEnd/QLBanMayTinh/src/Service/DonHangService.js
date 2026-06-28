import { get, post, put, del } from './api.js';

export const getAll = () => get('/api/don-hang');

export const create = (body) => post('/api/don-hang', body);

export const update = (id, body) => put(`/api/don-hang/update/${id}`, body);

export const remove = (id) => del(`/api/don-hang/delete/${id}`);

// Tính lại tong_tien sau khi thêm/xóa sản phẩm trong đơn
export const recalculate = (id) => fetch(`/api/don-hang/${id}/recalculate`, { method: 'PATCH' });

// Gộp nhiều đơn vào 1 đơn đích
export const merge = (targetId, sourceIds) =>
  post('/api/don-hang/merge', { targetId, sourceIds });

export const addChiTiet = (body) => post('/api/chi-tiet-don-hang', body);
