import { get } from './api.js';

export const getAll = () => get('/api/danh-muc?page=0&size=200');
