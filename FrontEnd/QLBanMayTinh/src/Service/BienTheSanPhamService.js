import { get, post, put, del } from './api.js';

export const getAll = () => get('/api/bien-the-san-pham');

export const create = (body) => post('/api/bien-the-san-pham', body);

export const update = (id, body) => put(`/api/bien-the-san-pham/update/${id}`, body);

export const remove = (id) => del(`/api/bien-the-san-pham/delete/${id}`);
