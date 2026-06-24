const H = { 'Content-Type': 'application/json' };

// GET — throw nếu HTTP error, caller dùng .catch(() => []) nếu muốn im lặng
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
