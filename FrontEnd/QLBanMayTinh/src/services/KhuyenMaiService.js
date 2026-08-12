import { get, post, put } from './api.js';

export const getAll = () => get(`/api/khuyen-mai`);

export const save = (id, body) =>
  id ? put(`/api/khuyen-mai/update/${id}`, body) : post('/api/khuyen-mai', body);
