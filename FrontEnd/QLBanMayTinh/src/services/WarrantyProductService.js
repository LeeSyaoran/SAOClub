// WarrantyProductService.js - API cho danh sách sản phẩm bảo hành admin
import { get } from './api.js';

export const getAllWarrantyProducts = async ({ page = 0, size = 50 } = {}) => {
  try {
    const data = await get(`/api/chi-tiet-don-hang/warranty/all?page=${page}&size=${size}`);
    return data || { content: [], totalElements: 0 };
  } catch (e) {
    console.error('Lỗi khi lấy danh sách sản phẩm BH:', e);
    return { content: [], totalElements: 0 };
  }
};

export const getWarrantyProductsByKhachHang = async (khachHangId) => {
  try {
    return await get(`/api/chi-tiet-don-hang/warranty/khach-hang/${khachHangId}`);
  } catch (e) {
    console.error('Lỗi khi lấy SP BH theo KH:', e);
    return [];
  }
};

export const getWarrantyProductsByDonHang = async (donHangId) => {
  try {
    return await get(`/api/chi-tiet-don-hang/warranty/don-hang/${donHangId}`);
  } catch (e) {
    console.error('Lỗi khi lấy SP BH theo đơn:', e);
    return [];
  }
};
