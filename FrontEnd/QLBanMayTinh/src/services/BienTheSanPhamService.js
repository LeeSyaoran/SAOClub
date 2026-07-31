import { get, post, put, del } from './api.js';

export const getPage = ({ page = 0, size = 50 } = {}) => get(`/api/bien-the-san-pham?page=${page}&size=${size}`);

export const getAll = () => getPage({ size: 200 }).then((p) => p.content);

export const create = (body) => post('/api/bien-the-san-pham', body);

export const update = (id, body) => put(`/api/bien-the-san-pham/update/${id}`, body);

export const remove = (id) => del(`/api/bien-the-san-pham/delete/${id}`);

// Biến thể đã qua giao dịch chưa — gọi trước khi hiện hộp thoại xóa.
export const hasTransactionHistory = (id) => get(`/api/bien-the-san-pham/${id}/co-giao-dich`);
