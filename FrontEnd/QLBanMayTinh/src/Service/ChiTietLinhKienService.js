import { get, post, put, del } from './api.js';

const chiTietLinhKien = (path) => ({
  getAll: () => get(`/api/${path}`),
  create: (body) => post(`/api/${path}`, body),
  update: (id, body) => put(`/api/${path}/update/${id}`, body),
  remove: (id) => del(`/api/${path}/delete/${id}`),
});

export const ChiTietCpuService = chiTietLinhKien('chi-tiet-cpu');
export const ChiTietRamService = chiTietLinhKien('chi-tiet-ram');
export const ChiTietGpuService = chiTietLinhKien('chi-tiet-gpu');
export const ChiTietOCungService = chiTietLinhKien('chi-tiet-o-cung');
