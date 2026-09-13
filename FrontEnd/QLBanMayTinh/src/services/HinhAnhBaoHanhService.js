// HinhAnhBaoHanhService.js — Upload/xem ảnh/video minh chứng lỗi cho phiếu BH
import { get, post, del } from './api.js';

/**
 * Lấy danh sách ảnh/video của 1 phiếu bảo hành.
 * @returns {Promise<Array<{hinhAnhId, baoHanhId, loai:'image'|'video', url, tenFile, ngayUpload}>>}
 */
export const getByBaoHanh = (baoHanhId) =>
  get(`/api/hinh-anh-bao-hanh/bao-hanh/${baoHanhId}`).catch(() => []);

/**
 * Upload 1 file (ảnh hoặc video) cho phiếu BH.
 * @param {number} baoHanhId
 * @param {File} file
 */
export const upload = (baoHanhId, file) => {
  const form = new FormData();
  form.append('file', file);
  return fetch(`/api/hinh-anh-bao-hanh/bao-hanh/${baoHanhId}`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${JSON.parse(sessionStorage.getItem('saophone_session') || '{}').token ?? ''}` },
    body: form,
  }).then((r) => {
    if (!r.ok) throw new Error(`Upload failed: ${r.status}`);
    return r.json();
  });
};

/**
 * Upload base64 (dùng khi muốn gửi preview ngay trước khi tạo phiếu).
 */
export const uploadBase64 = (baoHanhId, dataUrl, tenFile = 'image.png') => {
  return fetch(`/api/hinh-anh-bao-hanh/bao-hanh/${baoHanhId}/base64`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${JSON.parse(sessionStorage.getItem('saophone_session') || '{}').token ?? ''}`,
    },
    body: JSON.stringify({ dataUrl, tenFile }),
  }).then((r) => {
    if (!r.ok) throw new Error(`Upload failed: ${r.status}`);
    return r.json();
  });
};

export const remove = (hinhAnhId) =>
  del(`/api/hinh-anh-bao-hanh/${hinhAnhId}`);
