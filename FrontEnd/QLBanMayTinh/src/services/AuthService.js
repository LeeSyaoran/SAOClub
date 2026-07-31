import { post } from './api.js';

export const login = (username, password) =>
  post('/api/auth/login', { username, password });
