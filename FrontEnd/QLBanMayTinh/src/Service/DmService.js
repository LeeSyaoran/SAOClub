import { get, post, put, del } from './api.js';
export const getThuongHieu = () => get('/api/thuong-hieu');
export const getNhaCungCap = () => get('/api/nha-cung-cap');
export const getChucVu     = () => get('/api/chuc-vu');
export const getCpu        = () => get('/api/dm-cpu');
export const getRam        = () => get('/api/dm-ram');
export const getOCung      = () => get('/api/dm-o-cung');
export const getGpu        = () => get('/api/dm-gpu');

const crud = (path) => ({
  getAll: () => get(`/api/${path}`),
  save: (id, body) => id ? put(`/api/${path}/update/${id}`, body) : post(`/api/${path}`, body),
  remove: (id) => del(`/api/${path}/delete/${id}`),
});

export const DmCpuService = crud('dm-cpu');
export const DmRamService = crud('dm-ram');
export const DmGpuService = crud('dm-gpu');
export const DmOCungService = crud('dm-o-cung');
