import { get, post, put, del } from './api.js';

export const getAll = () => get('/api/phieu-tra-hang');

export const getById = (id) => get(`/api/phieu-tra-hang/${id}`);

export const save = (id, body) =>
  id ? put(`/api/phieu-tra-hang/update/${id}`, body) : post('/api/phieu-tra-hang', body);

export const remove = (id) => del(`/api/phieu-tra-hang/delete/${id}`);
