import { post } from './api.js';

export const login = (username, password) =>
  post('/api/auth/login', { username, password });

export const firebaseLogin = (idToken, provider) =>
  post('/api/auth/firebase', { idToken, provider });
