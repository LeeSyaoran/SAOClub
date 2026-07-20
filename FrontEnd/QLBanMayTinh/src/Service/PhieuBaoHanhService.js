import { get, post, put, del } from './api.js';

export const getAll = () => get('/api/phieu-bao-hanh');

export const getById = (id) => get(`/api/phieu-bao-hanh/${id}`);

export const save = (id, body) =>
  id ? put(`/api/phieu-bao-hanh/update/${id}`, body) : post('/api/phieu-bao-hanh', body);

export const remove = (id) => del(`/api/phieu-bao-hanh/delete/${id}`);
