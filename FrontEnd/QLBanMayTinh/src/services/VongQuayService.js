import { get, post, put } from './api.js';

export const getCauHinh = () => get('/api/vong-quay/cau-hinh');
export const capNhatCauHinh = (body) => put('/api/vong-quay/cau-hinh', body);
export const quay = () => post('/api/vong-quay/quay');
export const getLichSuCuaToi = () => get('/api/vong-quay/lich-su/cua-toi');
