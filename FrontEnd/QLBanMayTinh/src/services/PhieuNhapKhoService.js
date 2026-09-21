import { get, post, put, del } from './api.js';

export const getAll = () => get(`/api/phieu-nhap-kho`);

export const getById = (id) => get(`/api/phieu-nhap-kho/${id}`);

export const save = (id, body) =>
  id ? put(`/api/phieu-nhap-kho/update/${id}`, body) : post('/api/phieu-nhap-kho', body);

export const remove = (id) => del(`/api/phieu-nhap-kho/delete/${id}`);

// Kiểm tra serial trùng với DB — gửi mảng serial.
export const kiemTraSerial = (serials) => post('/api/phieu-nhap-kho/kiem-tra-serial', serials);

// Kiểm tra serial trùng với DB (body: List<String>).
export const kiemTraSerialDb = (serials) => post('/api/phieu-nhap-kho/kiem-tra-serial', serials);

// Duyệt phiếu nhập.
export const duyet = (id) => post(`/api/phieu-nhap-kho/${id}/duyet`);

// Từ chối phiếu nhập.
export const tuChoi = (id, lyDo) => post(`/api/phieu-nhap-kho/${id}/tu-choi`, lyDo ? { lyDo } : {});
