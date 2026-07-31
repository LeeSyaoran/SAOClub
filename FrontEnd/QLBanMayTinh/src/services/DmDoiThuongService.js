import { get, post, put, del } from './api.js';

export const getAll = () => get(`/api/dm-doi-thuong`);

export const save = (id, body) =>
  id ? put(`/api/dm-doi-thuong/update/${id}`, body) : post('/api/dm-doi-thuong', body);

export const remove = (id) => del(`/api/dm-doi-thuong/delete/${id}`);
