const H = { 'Content-Type': 'application/json' };

// QUAN TRỌNG — return type khác nhau:
//   get()           → Promise<parsed JSON>   (throw nếu HTTP error)
//   post/put/del()  → Promise<Response>      (caller tự kiểm tra res.ok)
// Không dùng .then(r => r.ok ? r.json() : []) sau get() — nó đã parse sẵn rồi.

export const get = async (url) => {
  const r = await fetch(url);
  if (!r.ok) {
    const msg = await r.text().catch(() => '');
    throw new Error(`HTTP ${r.status}${msg ? ': ' + msg : ''}`);
  }
  return r.json();
};

export const post = (url, body) =>
  fetch(url, { method: 'POST', headers: H, body: JSON.stringify(body) });

export const put = (url, body) =>
  fetch(url, { method: 'PUT', headers: H, body: JSON.stringify(body) });

export const del = (url) =>
  fetch(url, { method: 'DELETE' });
