import { get, post, put, del } from './api.js';

export const getAll = () => get('/api/khach-hang');

export const save = (id, body) =>
  id ? put(`/api/khach-hang/update/${id}`, body) : post('/api/khach-hang', body);

export const remove = (id) => del(`/api/khach-hang/delete/${id}`);

export const login = (username, password) =>
  post('/api/khach-hang/login', { username, password });

export const register = (body) =>
  post('/api/khach-hang/register', body);
