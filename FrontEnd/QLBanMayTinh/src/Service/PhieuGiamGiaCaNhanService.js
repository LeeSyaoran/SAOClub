import { get, post } from './api.js';

export const doiThuong = (doiThuongId) => post(`/api/phieu-giam-gia-ca-nhan/doi-thuong/${doiThuongId}`, {});

export const getCuaToi = () => get('/api/phieu-giam-gia-ca-nhan/cua-toi');
