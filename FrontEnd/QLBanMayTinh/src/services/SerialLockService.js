import { authHeaders } from './api.js';

const BASE = '/api/chi-tiet-san-pham';

export const SerialLockService = {
  // Khóa giữ chỗ danh sách serial theo phiên làm việc
  async lock(chiTietIds, sessionId, nhanVienId) {
    const res = await fetch(`${BASE}/lock`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ chiTietIds, sessionId, nhanVienId }),
    });
    return res.json();
  },

  // Mở khóa danh sách serial theo phiên làm việc
  async unlock(chiTietIds, sessionId) {
    const res = await fetch(`${BASE}/unlock`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ chiTietIds, sessionId }),
    });
    return res.json();
  },
};
