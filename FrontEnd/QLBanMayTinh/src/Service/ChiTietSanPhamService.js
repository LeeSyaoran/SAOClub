import { get, post, put } from './api.js';

export const getAll = () => get('/api/chi-tiet-san-pham');

// Lấy serial của 1 biến thể — dùng thay getAll() khi chỉ cần 1 biến thể
export const getByBienThe = (bienTheId) => get(`/api/chi-tiet-san-pham/bien-the/${bienTheId}`);

export const create = (body) => post('/api/chi-tiet-san-pham', body);

export const update = (id, body) => put(`/api/chi-tiet-san-pham/update/${id}`, body);
