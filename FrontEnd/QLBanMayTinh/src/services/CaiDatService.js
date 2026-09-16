import { get, put, post, authHeaders } from './api.js';

// post()/put() ở api.js trả Promise<Response> CHƯA parse (khác get()) — tự parse ở đây.
const parseOrThrow = async (res) => {
  if (!res.ok) throw new Error((await res.text().catch(() => '')) || `HTTP ${res.status}`);
  return res.json();
};

export const getCaiDat = () => get('/api/cai-dat');
export const updateCaiDat = (data) => put('/api/cai-dat', data).then(parseOrThrow);
export const apDungNguongTonKho = (nguong) =>
  post('/api/cai-dat/ap-dung-nguong-ton-kho', { nguong }).then(parseOrThrow);
export const doiMatKhau = (matKhauCu, matKhauMoi) =>
  post('/api/cai-dat/doi-mat-khau', { matKhauCu, matKhauMoi }).then(parseOrThrow);
export const capNhatHoSo = (data) => put('/api/cai-dat/ho-so', data).then(parseOrThrow);

export const uploadImage = async (file) => {
  const formData = new FormData();
  formData.append('file', file);
  const res = await fetch('/api/upload/image', {
    method: 'POST',
    headers: authHeaders(),
    body: formData
  });
  if (!res.ok) {
    const err = await res.text().catch(() => '');
    try { throw new Error(JSON.parse(err).error || `HTTP ${res.status}`); } 
    catch { throw new Error(err || `HTTP ${res.status}`); }
  }
  return res.json();
};
