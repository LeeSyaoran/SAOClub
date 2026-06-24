import { get, post, put } from './api.js';

export const getAll = () => get('/api/don-hang');

export const create = (body) => post('/api/don-hang', body);

export const update = (id, body) => put(`/api/don-hang/update/${id}`, body);

export const addChiTiet = (body) => post('/api/chi-tiet-don-hang', body);
