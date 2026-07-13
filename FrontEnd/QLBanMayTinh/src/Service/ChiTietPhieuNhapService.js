import { get, post, put, del } from './api.js';

export const getAll = () => get('/api/chi-tiet-phieu-nhap');

export const create = (body) => post('/api/chi-tiet-phieu-nhap', body);

export const update = (id, body) => put(`/api/chi-tiet-phieu-nhap/update/${id}`, body);

export const remove = (id) => del(`/api/chi-tiet-phieu-nhap/delete/${id}`);
