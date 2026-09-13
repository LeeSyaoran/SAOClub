// BinhLuanBaoHanhService.js — Trao đổi giữa khách hàng và nhân viên trên 1 phiếu BH
import { get, post } from './api.js';

/**
 * Lấy tất cả bình luận/trao đổi của 1 phiếu BH, sắp xếp theo thời gian tăng dần.
 * @returns {Promise<Array<{binhLuanId, baoHanhId, nguoiGuiId, tenNguoiGui, vaiTro, noiDung, ngayGui}>>}
 */
export const getByBaoHanh = (baoHanhId) =>
  get(`/api/binh-luan-bao-hanh/bao-hanh/${baoHanhId}`).catch(() => []);

/**
 * Khách hàng hoặc nhân viên gửi bình luận mới.
 */
export const send = (baoHanhId, noiDung) =>
  post(`/api/binh-luan-bao-hanh/bao-hanh/${baoHanhId}`, { noiDung });
