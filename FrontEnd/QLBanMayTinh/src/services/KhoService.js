import { get, post, put } from './api.js';

// Quản lý nghiệp vụ kho hàng và kiểm kê

export const getTonKho = () => get('/api/kho/ton-kho');

export const getSerial = (bienTheId) => get(`/api/kho/serial?bienTheId=${bienTheId}`);

export const getLichSu = (bienTheId) => get(`/api/kho/lich-su?bienTheId=${bienTheId}`);

export const getNhanVien = () => get('/api/kho/nhan-vien');

export const getPhieuNhap = () => get('/api/kho/phieu-nhap');

// Tạo phiếu nhập kho hàng
export const nhapHang = (body) => post('/api/kho/nhap-hang', body);

export const capNhatBienThe = (bienTheId, body) => put(`/api/kho/bien-the/${bienTheId}`, body);

export const doiTrangThaiSerial = (chiTietId, body) => put(`/api/kho/serial/${chiTietId}`, body);

export const capNhatTonToiThieu = (bienTheId, tonToiThieu) =>
  put(`/api/kho/ton-kho/${bienTheId}`, { tonToiThieu });