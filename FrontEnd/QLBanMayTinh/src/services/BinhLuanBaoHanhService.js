// Trao đổi bình luận trên phiếu bảo hành
import { get, post } from './api.js';

// Lấy danh sách bình luận của phiếu bảo hành
export const getByBaoHanh = (baoHanhId) =>
  get(`/api/binh-luan-bao-hanh/bao-hanh/${baoHanhId}`).catch(() => []);

// Gửi bình luận mới vào phiếu bảo hành
export const send = (baoHanhId, noiDung) =>
  post(`/api/binh-luan-bao-hanh/bao-hanh/${baoHanhId}`, { noiDung });
