import { get, post, del } from './api.js';

// Lấy danh sách đánh giá theo sản phẩm
export const getBySanPham = (sanPhamId) => get(`/api/danh-gia/san-pham/${sanPhamId}`);

export const getTongHop = () => get('/api/danh-gia/tong-hop');

export const add = (sanPhamId, soSao, noiDung) => post('/api/danh-gia', { sanPhamId, soSao, noiDung });

export const remove = (danhGiaId) => del(`/api/danh-gia/${danhGiaId}`);

// Quản lý kiểm duyệt đánh giá dành cho admin
export const getAllAdmin = () => get('/api/danh-gia/admin');

export const removeAdmin = (danhGiaId) => del(`/api/danh-gia/admin/${danhGiaId}`);
