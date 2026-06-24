import { get } from './api.js';

export const getThuongHieu = () => get('/api/thuong-hieu');
export const getNhaCungCap = () => get('/api/nha-cung-cap');
export const getChucVu     = () => get('/api/chuc-vu');
export const getCpu        = () => get('/api/dm-cpu');
export const getRam        = () => get('/api/dm-ram');
export const getOCung      = () => get('/api/dm-o-cung');
export const getGpu        = () => get('/api/dm-gpu');
