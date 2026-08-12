import { get, post, put } from './api.js';

export const getPage = ({ page = 0, size = 50 } = {}) => get(`/api/bien-the-san-pham?page=${page}&size=${size}`);

export const getAll = () => getPage({ size: 200 }).then((p) => p.content);

export const create = (body) => post('/api/bien-the-san-pham', body);

export const update = (id, body) => put(`/api/bien-the-san-pham/update/${id}`, body);
