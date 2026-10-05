import { clearSession } from '../stores/index.js';
import { showToast } from '../stores/toast.js';
import { t } from '../i18n/index.js';
import { resetAllStores } from '../stores/resetAll.js';

// Gắn token xác thực JWT vào header request
export const authHeaders = () => {
  try {
    const session = JSON.parse(sessionStorage.getItem('saoclub_session'));
    return session?.token ? { Authorization: `Bearer ${session.token}` } : {};
  } catch {
    return {};
  }
};
const headers = () => ({ 'Content-Type': 'application/json', ...authHeaders() });

// Cờ chống lặp thao tác đăng xuất khi nhận nhiều lỗi 401
let dangDangXuatDoHetPhien = false;
const hasTokenSession = () => {
  try {
    const session = JSON.parse(sessionStorage.getItem('saoclub_session'));
    return Boolean(session?.token);
  } catch {
    return false;
  }
};

const kiemTraHetPhien = (r) => {
  if (r.status === 401 && hasTokenSession() && !dangDangXuatDoHetPhien) {
    dangDangXuatDoHetPhien = true;
    clearSession();
    resetAllStores();
    showToast(t('toast.sessionExpired'), 'error');
    window.location.hash = '#/';
    // Mở lại cờ sau 2 giây
    setTimeout(() => { dangDangXuatDoHetPhien = false; }, 2000);
  }
  return r;
};

// Gửi GET request và parse JSON kết quả
export const get = async (url) => {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 15000);
  try {
    const r = kiemTraHetPhien(await fetch(url, { headers: authHeaders(), signal: controller.signal }));
    // Trả về null nếu không có quyền truy cập
    if (r.status === 403) {
      return null;
    }
    if (!r.ok) {
      const msg = await r.text().catch(() => '');
      throw new Error(`HTTP ${r.status}${msg ? ': ' + msg : ''}`);
    }
    return await r.json();
  } finally {
    clearTimeout(timer);
  }
};

export const post = (url, body) =>
  fetch(url, { method: 'POST', headers: headers(), body: JSON.stringify(body) }).then(kiemTraHetPhien);

export const put = (url, body) =>
  fetch(url, { method: 'PUT', headers: headers(), body: JSON.stringify(body) }).then(kiemTraHetPhien);

export const patch = (url, body) =>
  fetch(url, { method: 'PATCH', headers: headers(), body: JSON.stringify(body) }).then(kiemTraHetPhien);

export const del = (url) =>
  fetch(url, { method: 'DELETE', headers: authHeaders() }).then(kiemTraHetPhien);
