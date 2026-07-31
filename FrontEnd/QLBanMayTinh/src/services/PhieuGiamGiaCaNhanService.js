import { get, post } from './api.js';

export const doiThuong = (doiThuongId) => post(`/api/phieu-giam-gia-ca-nhan/doi-thuong/${doiThuongId}`, {});

export const getCuaToi = () => get('/api/phieu-giam-gia-ca-nhan/cua-toi');

// Admin tặng voucher trực tiếp cho 1 khách hàng — body: { loai, giaTri, giaTriToiDa, ngayHetHan, donHangToiThieu }
export const taoVoucherAdmin = (khachHangId, body) => post(`/api/phieu-giam-gia-ca-nhan/tang/${khachHangId}`, body);

// Admin xem toàn bộ voucher/điểm thưởng của 1 khách hàng
export const getByKhachHang = (khachHangId) => get(`/api/phieu-giam-gia-ca-nhan/khach-hang/${khachHangId}`);
