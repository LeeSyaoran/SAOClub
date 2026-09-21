import { get, post, put, del } from './api.js';

const BASE = '/api/thuoc-tinh';

export const ThuocTinhService = {
  // Lấy tất cả thuộc tính
  getAll: () => get(`${BASE}`),

  // Lấy thuộc tính theo ID
  getById: (id) => get(`${BASE}/${id}`),

  // Lấy thuộc tính theo tên trường
  getByTenTruong: (tenTruong) => get(`${BASE}/ten-truong/${tenTruong}`),

  // Tạo thuộc tính mới
  create: (data) => post(BASE, data),

  // Cập nhật thuộc tính
  update: (id, data) => put(`${BASE}/${id}`, data),

  // Xóa thuộc tính
  delete: (id) => del(`${BASE}/${id}`),

  // === GIÁ TRỊ THUỘC TÍNH ===

  // Lấy giá trị của 1 thuộc tính
  getGiaTri: (thuocTinhId) => get(`${BASE}/${thuocTinhId}/gia-tri`),

  // Thêm giá trị mới
  addGiaTri: (thuocTinhId, data) => post(`${BASE}/${thuocTinhId}/gia-tri`, data),

  // Xóa giá trị
  deleteGiaTri: (thuocTinhId, giaTriId) => del(`${BASE}/${thuocTinhId}/gia-tri/${giaTriId}`),
};
