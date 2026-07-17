import { get, put, post } from './api.js';

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
