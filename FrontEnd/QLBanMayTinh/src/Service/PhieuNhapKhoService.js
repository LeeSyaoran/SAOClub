import { get, post, put, del } from './api.js';

export const getAll = () => get('/api/phieu-nhap-kho');

export const save = (id, body) =>
  id ? put(`/api/phieu-nhap-kho/update/${id}`, body) : post('/api/phieu-nhap-kho', body);

export const remove = (id) => del(`/api/phieu-nhap-kho/delete/${id}`);
