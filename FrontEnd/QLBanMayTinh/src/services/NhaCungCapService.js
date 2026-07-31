import { get, post, put, del } from './api.js';

export const getAll = () => get(`/api/nha-cung-cap`);

export const getById = (id) => get(`/api/nha-cung-cap/${id}`);

export const save = (id, body) =>
  id ? put(`/api/nha-cung-cap/update/${id}`, body) : post('/api/nha-cung-cap', body);

export const remove = (id) => del(`/api/nha-cung-cap/delete/${id}`);
