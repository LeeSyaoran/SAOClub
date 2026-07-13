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

// QUAN TRỌNG — return type khác nhau:
//   get()           → Promise<parsed JSON>   (throw nếu HTTP error)
//   post/put/del()  → Promise<Response>      (caller tự kiểm tra res.ok)
// Không dùng .then(r => r.ok ? r.json() : []) sau get() — nó đã parse sẵn rồi.

export const get = async (url) => {
  const r = await fetch(url, { headers: authHeaders() });
  if (!r.ok) {
    const msg = await r.text().catch(() => '');
    throw new Error(`HTTP ${r.status}${msg ? ': ' + msg : ''}`);
  }
  return r.json();
};

export const post = (url, body) =>
  fetch(url, { method: 'POST', headers: headers(), body: JSON.stringify(body) });

export const put = (url, body) =>
  fetch(url, { method: 'PUT', headers: headers(), body: JSON.stringify(body) });

export const patch = (url, body) =>
  fetch(url, { method: 'PATCH', headers: headers(), body: JSON.stringify(body) });

export const del = (url) =>
  fetch(url, { method: 'DELETE', headers: authHeaders() });
