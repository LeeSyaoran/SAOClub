import { get, put } from './api.js';

export const getAll = () => get(`/api/ton-kho`);

export const getByBienThe = (bienTheId) => get(`/api/ton-kho/bien-the/${bienTheId}`);

export const update = (id, body) => put(`/api/ton-kho/update/${id}`, body);
