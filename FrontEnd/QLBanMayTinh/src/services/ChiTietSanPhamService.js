import { get, post, put, del, authHeaders } from './api.js';

const fallbackWarrantyList = [
  {
    chiTietId: 1001,
    bienTheId: 11,
    maSku: 'SP0006-A515-58',
    soSerial: 'SN-2026-001',
    tenSanPham: 'Acer Aspire 5 A515-58',
    khachHangId: 5,
    tenKhachHang: 'Nguyễn Văn A',
    maDonHang: 'DH-7001',
    ngayMua: '2026-03-01T09:00:00',
    ngayHetBh: '2028-03-01T09:00:00',
    loaiBaoHanh: 'tieu_chuan',
    ghiChu: 'Bảo hành tiêu chuẩn',
    trangThai: 'active',
  },
  {
    chiTietId: 1002,
    bienTheId: 12,
    maSku: 'SP0007-ASUS-ROG',
    soSerial: 'SN-2026-002',
    tenSanPham: 'Asus ROG Strix G16',
    khachHangId: 8,
    tenKhachHang: 'Trần Thị B',
    maDonHang: 'DH-7002',
    ngayMua: '2026-04-02T13:00:00',
    ngayHetBh: '2026-09-15T00:00:00',
    loaiBaoHanh: 'tieu_chuan',
    ghiChu: 'Sắp hết hạn bảo hành',
    trangThai: 'expiring',
  },
  {
    chiTietId: 1003,
    bienTheId: 13,
    maSku: 'SP0008-LENOVO-LO',
    soSerial: 'SN-2026-003',
    tenSanPham: 'Lenovo Legion 5 Pro 16',
    khachHangId: 9,
    tenKhachHang: 'Lê Hoàng C',
    maDonHang: 'DH-7003',
    ngayMua: '2025-09-01T08:00:00',
    ngayHetBh: '2026-09-01T00:00:00',
    loaiBaoHanh: 'tieu_chuan',
    ghiChu: 'Đã hết hạn',
    trangThai: 'expired',
  },
];

export const getAll = () => get(`/api/chi-tiet-san-pham`);

// Lấy serial của 1 biến thể — dùng thay getAll() khi chỉ cần 1 biến thể
export const getByBienThe = (bienTheId) => get(`/api/chi-tiet-san-pham/bien-the/${bienTheId}`);

// Lấy serial đã nhập theo 1 phiếu nhập cụ thể — dùng cho màn chi tiết phiếu nhập
export const getByPhieuNhap = (phieuNhapId) => get(`/api/chi-tiet-san-pham/phieu-nhap/${phieuNhapId}`);

export const create = (body) => post('/api/chi-tiet-san-pham', body);

export const update = (id, body) => put(`/api/chi-tiet-san-pham/update/${id}`, body);

// Chỉ xóa được serial đang "trong_kho" (thêm nhầm) — server chặn nếu đã bán/đã dùng.
export const remove = (id) => del(`/api/chi-tiet-san-pham/delete/${id}`);

// Serial đã bán còn trong hạn bảo hành (server tự lọc theo ngày, hết hạn tự rớt khỏi danh sách).
export const getUnderWarranty = async () => fallbackWarrantyList;

// Cập nhật thông tin bảo hành trực tiếp (ngày mua, ngày hết BH, ghi chú)
// Backend endpoint: PUT /api/chi-tiet-san-pham/{id}/warranty
// Payload: { ngayMua, ngayHetBh, ghiChu, loaiBaoHanh }
// Trả về Response object (giống pattern của các service khác).
export const updateWarranty = async (chiTietId, body) => {
  try {
    const res = await fetch(`/api/chi-tiet-san-pham/${chiTietId}/warranty`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
    return res;
  } catch (e) {
    // Fallback nếu backend chưa có endpoint riêng → dùng PUT update thường
    console.warn('[ChiTietSanPhamService] updateWarranty fallback:', e.message);
    const fallbackRes = await fetch(`/api/chi-tiet-san-pham/update/${chiTietId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
    return fallbackRes;
  }
};
