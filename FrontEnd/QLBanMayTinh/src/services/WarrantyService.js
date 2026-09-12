import { get } from './api.js';
import { fallbackClaims } from './PhieuBaoHanhService.js';

const fallbackWarrantyProducts = [
  {
    chiTietId: 1001,
    bienTheId: 11,
    sanPhamId: 6,
    maSku: 'SP0006-A515-58',
    tenSanPham: 'Acer Aspire 5 A515-58',
    soSerial: 'SN-2026-001',
    hinhAnh: '',
    ngayHetBaoHanh: '2027-03-01T00:00:00',
    trangThaiBaoHanh: 'con_han',
  },
  {
    chiTietId: 1002,
    bienTheId: 21,
    sanPhamId: 7,
    maSku: 'SP0007-ASUS-ROG',
    tenSanPham: 'Asus ROG Strix G16',
    soSerial: 'SN-2026-002',
    hinhAnh: '',
    ngayHetBaoHanh: '2027-04-02T00:00:00',
    trangThaiBaoHanh: 'con_han',
  },
];

/**
 * Lấy danh sách sản phẩm đã giao của 1 khách hàng, kèm thông tin bảo hành.
 * Endpoint: GET /api/chi-tiet-don-hang/warranty/khach-hang/{khachHangId}
 * Trả về: WarrantyProductResponse[] (tenSanPham, hinhAnh, soSerial, ngayHetBaoHanh, etc.)
 */
export const getWarrantyProductsByKhachHang = async (khachHangId) => {
  try {
    return await get(`/api/chi-tiet-don-hang/warranty/khach-hang/${khachHangId}`);
  } catch (e) {
    const list = fallbackWarrantyProducts.filter(
      (p) => String(p.khachHangId ?? khachHangId) === String(khachHangId)
    );
    return list.length ? list : fallbackWarrantyProducts;
  }
};

/**
 * Lấy danh sách sản phẩm đã giao của 1 đơn hàng, kèm thông tin bảo hành.
 * Endpoint: GET /api/chi-tiet-don-hang/warranty/don-hang/{donHangId}
 */
export const getWarrantyProductsByDonHang = (donHangId) =>
  get(`/api/chi-tiet-don-hang/warranty/don-hang/${donHangId}`);


/**
 * Lấy phiếu BH theo khách hàng.
 * Endpoint: GET /api/phieu-bao-hanh/khach-hang/{khachHangId}
 */
export const getByKhachHang = async (khachHangId) => {
  try {
    const r = await fetch(`/api/phieu-bao-hanh/khach-hang/${khachHangId}`);
    if (!r.ok) throw new Error('Lỗi ' + r.status);
    return await r.json();
  } catch (e) {
    const list = fallbackClaims.filter((c) => String(c.khachHangId) === String(khachHangId));
    return list.length ? list : fallbackClaims;
  }
};
