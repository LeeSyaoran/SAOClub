import { get } from './api.js';

// Lấy lịch sử biến động tồn kho
export const getAll = () => get(`/api/lich-su-ton-kho`);
