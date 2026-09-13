import { get } from './api.js';

// Lay danh sach danh muc active — cho POS category tabs
export const getActive = () => get('/api/danh-muc/active');

// Lay danh sach tat ca danh muc
export const getAll = () => get('/api/danh-muc');

export default { getActive, getAll };
