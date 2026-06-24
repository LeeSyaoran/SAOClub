import { get, post, put, del } from './api.js';

export const getAll = () => get('/api/nhan-vien');

export const save = (id, body) =>
  id ? put(`/api/nhan-vien/update/${id}`, body) : post('/api/nhan-vien', body);

export const remove = (id) => del(`/api/nhan-vien/delete/${id}`);
