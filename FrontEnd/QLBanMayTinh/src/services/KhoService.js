import { get, post, put } from './api.js';

// Toàn bộ màn Kho hàng chỉ dùng nhóm endpoint /api/kho — mọi truy vấn tồn kho, serial,
// lịch sử và nhập hàng đều gom về một chỗ để dễ kiểm soát quyền và dễ sửa sau này.

export const getTonKho = () => get('/api/kho/ton-kho');

export const getSerial = (bienTheId) => get(`/api/kho/serial?bienTheId=${bienTheId}`);

export const getLichSu = (bienTheId) => get(`/api/kho/lich-su?bienTheId=${bienTheId}`);

export const getNhanVien = () => get('/api/kho/nhan-vien');

export const getPhieuNhap = () => get('/api/kho/phieu-nhap');

// body: { nhaCungCapId, nhanVienId, ngayNhap, ghiChu, capNhatGiaNhap, dongNhap: [...] }
export const nhapHang = (body) => post('/api/kho/nhap-hang', body);

export const capNhatBienThe = (bienTheId, body) => put(`/api/kho/bien-the/${bienTheId}`, body);

export const doiTrangThaiSerial = (chiTietId, body) => put(`/api/kho/serial/${chiTietId}`, body);

export const capNhatTonToiThieu = (bienTheId, tonToiThieu) =>
  put(`/api/kho/ton-kho/${bienTheId}`, { tonToiThieu });