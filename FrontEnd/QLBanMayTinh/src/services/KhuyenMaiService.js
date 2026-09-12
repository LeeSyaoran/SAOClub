import { get, post, put } from './api.js';

export const getAll = () => get(`/api/khuyen-mai`);

export const save = (id, body) =>
  id ? put(`/api/khuyen-mai/update/${id}`, body) : post('/api/khuyen-mai', body);

// Validate a promo code — returns { valid, ...data } or throws with error message
export const kiemTra = (maKhuyenMai) =>
  post(`/api/khuyen-mai/kiem-tra`, { maKhuyenMai });
