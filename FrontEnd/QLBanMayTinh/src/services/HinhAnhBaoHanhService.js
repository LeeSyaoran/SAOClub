// Quản lý hình ảnh và video minh chứng bảo hành
import { get, post, del } from './api.js';

// Lấy danh sách tệp đính kèm của phiếu bảo hành
export const getByBaoHanh = (baoHanhId) =>
  get(`/api/hinh-anh-bao-hanh/bao-hanh/${baoHanhId}`).catch(() => []);

// Tải lên tệp đính kèm cho phiếu bảo hành
export const upload = (baoHanhId, file) => {
  const form = new FormData();
  form.append('file', file);
  return fetch(`/api/hinh-anh-bao-hanh/bao-hanh/${baoHanhId}`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${JSON.parse(sessionStorage.getItem('saoclub_session') || '{}').token ?? ''}` },
    body: form,
  }).then((r) => {
    if (!r.ok) throw new Error(`Upload failed: ${r.status}`);
    return r.json();
  });
};

// Tải lên ảnh dạng base64
export const uploadBase64 = (baoHanhId, dataUrl, tenFile = 'image.png') => {
  return fetch(`/api/hinh-anh-bao-hanh/bao-hanh/${baoHanhId}/base64`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${JSON.parse(sessionStorage.getItem('saoclub_session') || '{}').token ?? ''}`,
    },
    body: JSON.stringify({ dataUrl, tenFile }),
  }).then((r) => {
    if (!r.ok) throw new Error(`Upload failed: ${r.status}`);
    return r.json();
  });
};

export const remove = (hinhAnhId) =>
  del(`/api/hinh-anh-bao-hanh/${hinhAnhId}`);
