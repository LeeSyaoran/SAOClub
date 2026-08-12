import { get, post, put } from './api.js';

export const getAll = () => get(`/api/nha-cung-cap`);

export const getById = (id) => get(`/api/nha-cung-cap/${id}`);

export const save = (id, body) =>
  id ? put(`/api/nha-cung-cap/update/${id}`, body) : post('/api/nha-cung-cap', body);
