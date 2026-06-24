import { get, post, put, del } from './api.js';

export const getAll = () => get('/api/san-pham/hien-thi');

export const save = (id, body) =>
  id ? put(`/api/san-pham/update/${id}`, body) : post('/api/san-pham', body);

export const remove = (id) => del(`/api/san-pham/delete/${id}`);
