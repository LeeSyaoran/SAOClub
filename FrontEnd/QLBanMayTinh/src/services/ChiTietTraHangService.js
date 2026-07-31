import { get, post, put, del } from './api.js';

export const getAll = () => get(`/api/chi-tiet-tra-hang`);

export const create = (body) => post('/api/chi-tiet-tra-hang', body);

export const update = (id, body) => put(`/api/chi-tiet-tra-hang/update/${id}`, body);

export const remove = (id) => del(`/api/chi-tiet-tra-hang/delete/${id}`);
