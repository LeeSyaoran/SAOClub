import { get, post, del } from './api.js';

// Đánh giá sản phẩm — GET công khai (không cần đăng nhập), POST/DELETE cần đăng nhập
// và server tự xác minh đã mua+nhận hàng (xem DanhGiaService.java).
export const getBySanPham = (sanPhamId) => get(`/api/danh-gia/san-pham/${sanPhamId}`);

export const getTongHop = () => get('/api/danh-gia/tong-hop');

export const add = (sanPhamId, soSao, noiDung) => post('/api/danh-gia', { sanPhamId, soSao, noiDung });

export const remove = (danhGiaId) => del(`/api/danh-gia/${danhGiaId}`);

// Kiểm duyệt (admin/nhân viên) — xem tất cả + xóa bất kỳ, không cần là chủ sở hữu.
export const getAllAdmin = () => get('/api/danh-gia/admin');

export const removeAdmin = (danhGiaId) => del(`/api/danh-gia/admin/${danhGiaId}`);
