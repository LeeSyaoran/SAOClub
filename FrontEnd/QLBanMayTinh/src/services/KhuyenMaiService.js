import { get, post, put, del } from './api.js';

export const getAll = () => get(`/api/khuyen-mai`);

export const save = (id, body) =>
  id ? put(`/api/khuyen-mai/update/${id}`, body) : post('/api/khuyen-mai', body);

export const remove = (id) => del(`/api/khuyen-mai/delete/${id}`);
