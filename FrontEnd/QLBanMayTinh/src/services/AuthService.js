import { post, put, get } from './api.js';

export const login = (username, password) =>
  post('/api/auth/login', { username, password });

export const firebaseLogin = (idToken, provider) =>
  post('/api/auth/firebase', { idToken, provider });

export const updateProfile = (body) =>
  put('/api/auth/profile', body);

export const getCurrentUser = () =>
  get('/api/auth/me');
