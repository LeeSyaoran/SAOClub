import { get, post, put, authHeaders } from './api.js';

export const getPage = ({ page = 0, size = 50 } = {}) => get(`/api/phieu-bao-hanh?page=${page}&size=${size}`);

export const getAll = () => getPage({ size: 200 }).then((p) => p.content);

export const getById = (id) => get(`/api/phieu-bao-hanh/${id}`);

// Lookup tra cuu serial. Throw Error voi .code='NOT_FOUND' hoac .code='DELETED'
// de frontend phan biet 2 truong hop "chua ton tai" vs "da bi xoa".
export const lookupBySerial = async (soSerial) => {
  const r = await fetch(
    `/api/phieu-bao-hanh/tra-cuu-serial?soSerial=${encodeURIComponent(soSerial)}`,
    { headers: authHeaders() }
  );
  if (r.status === 404) {
    const body = await r.json().catch(() => ({}));
    const err = new Error(body.message || 'Not found');
    err.code = body.code || 'NOT_FOUND';
    err.status = 404;
    throw err;
  }
  if (!r.ok) throw new Error(`HTTP ${r.status}`);
  return r.json();
};

export const save = (id, body) =>
  id ? put(`/api/phieu-bao-hanh/update/${id}`, body) : post('/api/phieu-bao-hanh', body);
