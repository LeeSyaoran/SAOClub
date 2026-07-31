import { get, post, put, del } from './api.js';

export const getPage = ({ page = 0, size = 50 } = {}) => get(`/api/nhan-vien?page=${page}&size=${size}`);

export const getAll = () => getPage({ size: 200 }).then((p) => p.content);

export const save = (id, body) =>
  id ? put(`/api/nhan-vien/update/${id}`, body) : post('/api/nhan-vien', body);

export const remove = (id) => del(`/api/nhan-vien/delete/${id}`);
