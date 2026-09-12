import { authHeaders } from './api.js';

const BASE = '/api/chi-tiet-san-pham';

export const SerialLockService = {
  /**
   * Lock nhiều serial cùng lúc
   * @param {number[]} chiTietIds
   * @param {string} sessionId - UUID của POS session
   * @param {number} nhanVienId
   * @returns {Promise<{success: boolean, lockedCount: number, failedIds: number[], message: string}>}
   */
  async lock(chiTietIds, sessionId, nhanVienId) {
    const res = await fetch(`${BASE}/lock`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ chiTietIds, sessionId, nhanVienId }),
    });
    return res.json();
  },

  /**
   * Unlock nhiều serial
   * @param {number[]} chiTietIds
   * @param {string} sessionId
   * @returns {Promise<{unlocked: number}>}
   */
  async unlock(chiTietIds, sessionId) {
    const res = await fetch(`${BASE}/unlock`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ chiTietIds, sessionId }),
    });
    return res.json();
  },
};
