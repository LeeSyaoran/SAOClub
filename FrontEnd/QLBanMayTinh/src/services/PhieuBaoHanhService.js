import { get, post, put, del } from './api.js';

export const getPage = ({ page = 0, size = 50 } = {}) => get(`/api/phieu-bao-hanh?page=${page}&size=${size}`);

export const getAll = () => getPage({ size: 200 }).then((p) => p.content);

export const getById = (id) => get(`/api/phieu-bao-hanh/${id}`);

export const save = (id, body) =>
  id ? put(`/api/phieu-bao-hanh/update/${id}`, body) : post('/api/phieu-bao-hanh', body);

export const remove = (id) => del(`/api/phieu-bao-hanh/delete/${id}`);
