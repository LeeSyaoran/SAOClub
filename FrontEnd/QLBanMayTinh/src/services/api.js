import { clearSession } from '../stores/index.js';
import { showToast } from '../stores/toast.js';
import { t } from '../i18n/index.js';
import { resetAllStores } from '../stores/resetAll.js';

// Gắn JWT (nếu đã đăng nhập) vào mọi request — token được lưu trong session
// bởi stores/index.js sau khi login (xem setSession).
export const authHeaders = () => {
  try {
    const session = JSON.parse(sessionStorage.getItem('saophone_session'));
    return session?.token ? { Authorization: `Bearer ${session.token}` } : {};
  } catch {
    return {};
  }
};
const headers = () => ({ 'Content-Type': 'application/json', ...authHeaders() });

// 401 = JwtAuthFilter không xác thực được (thiếu/hết hạn token, xem SecurityConfig.java) —
// khác 403 (đã đăng nhập nhưng sai vai trò, không tự đăng xuất). Trước đây không xử lý gì,
// request cứ lỗi âm thầm (bảng trống/lỗi thô) đến khi khách tự F5/đăng nhập lại. Chỉ đăng
// xuất 1 lần dù nhiều request 401 cùng lúc (vd trang có vài fetch song song).
let dangDangXuatDoHetPhien = false;
const kiemTraHetPhien = (r) => {
  if (r.status === 401 && !dangDangXuatDoHetPhien) {
    dangDangXuatDoHetPhien = true;
    clearSession();
    resetAllStores();
    showToast(t('toast.sessionExpired'), 'error');
    window.location.hash = '#/';
    // Cờ này chỉ để gộp nhiều request 401 xảy ra gần như đồng thời (cùng 1 lần hết phiên,
    // vd trang có vài fetch song song) thành 1 lần xử lý — KHÔNG phải khóa vĩnh viễn cho cả
    // tab. Không tự reset thì lần hết phiên thứ 2 (sau khi đăng nhập lại trong cùng tab, vd
    // ca làm dài, JWT hết hạn lần nữa) sẽ bị bỏ qua hoàn toàn, không tự đăng xuất nữa.
    setTimeout(() => { dangDangXuatDoHetPhien = false; }, 2000);
  }
  return r;
};

// QUAN TRỌNG — return type khác nhau:
//   get()           → Promise<parsed JSON>   (throw nếu HTTP error)
//   post/put/del()  → Promise<Response>      (caller tự kiểm tra res.ok)
// Không dùng .then(r => r.ok ? r.json() : []) sau get() — nó đã parse sẵn rồi.

export const get = async (url) => {
  const r = kiemTraHetPhien(await fetch(url, { headers: authHeaders() }));
  if (!r.ok) {
    const msg = await r.text().catch(() => '');
    throw new Error(`HTTP ${r.status}${msg ? ': ' + msg : ''}`);
  }
  return r.json();
};

export const post = (url, body) =>
  fetch(url, { method: 'POST', headers: headers(), body: JSON.stringify(body) }).then(kiemTraHetPhien);

export const put = (url, body) =>
  fetch(url, { method: 'PUT', headers: headers(), body: JSON.stringify(body) }).then(kiemTraHetPhien);

export const patch = (url, body) =>
  fetch(url, { method: 'PATCH', headers: headers(), body: JSON.stringify(body) }).then(kiemTraHetPhien);

export const del = (url) =>
  fetch(url, { method: 'DELETE', headers: authHeaders() }).then(kiemTraHetPhien);
