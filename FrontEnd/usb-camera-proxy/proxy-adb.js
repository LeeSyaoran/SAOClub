#!/usr/bin/env node
/**
 * USB Barcode Scanner Proxy
 *
 * Nhan text barcode tu dien thoai qua USB (adb reverse).
 * Khong can WiFi, khong can MJPEG stream.
 *
 * Cach hoat dong:
 *   1. Ket noi USB, bat USB Debugging
 *   2. node proxy-adb.js
 *   3. Tren dien thoai: mo Chrome, truy cap http://localhost:8080
 *   4. Diên thoai quét barcode -> text gui len proxy
 *   5. Web app nhan text qua SSE -> fill vao input
 */

const http = require('http');
const { spawn, execSync } = require('child_process');
const path = require('path');
const os = require('os');

// ── Config ─────────────────────────────────────────────────────────────────
const HTTP_PORT = 8484;
const WS_PATH = '/events';
const SCAN_PATH = '/scan';

let adbPath = 'adb';
let deviceId = null;
let sseClients = [];      // SSE response objects
let lastScan = null;      // last scanned text
let scanHistory = [];      // keep last N scans

// ── ADB helpers ─────────────────────────────────────────────────────────────

function findAdb() {
  const candidates = [
    'adb',
    'D:\\adb\\platform-tools\\adb.exe',
    'C:\\adb\\platform-tools\\adb.exe',
    'D:\\Android\\platform-tools\\adb.exe',
    'C:\\Android\\platform-tools\\adb.exe',
    path.join(__dirname, 'adb', 'platform-tools', 'adb.exe'),
  ];
  for (const p of candidates) {
    try {
      execSync(`"${p}" version`, { stdio: 'ignore', timeout: 3000 });
      return p;
    } catch { /* next */ }
  }
  return null;
}

function getDevices() {
  try {
    const out = execSync(`"${adbPath}" devices`, { encoding: 'utf8', timeout: 5000 });
    return out.trim().split('\n')
      .filter(l => l.trim() && !l.includes('List'))
      .map(l => {
        const parts = l.trim().split(/\s+/);
        return { id: parts[0], status: parts[1] };
      });
  } catch { return []; }
}

function setupReverse() {
  try {
    execSync(`"${adbPath}" reverse --remove tcp:${HTTP_PORT} tcp:${HTTP_PORT}`, { stdio: 'ignore' });
  } catch { /* ignore */ }
  try {
    execSync(`"${adbPath}" reverse tcp:${HTTP_PORT} tcp:${HTTP_PORT}`, { stdio: 'ignore' });
    console.log(`[ADB] Da reverse: localhost:${HTTP_PORT} <-> dien thoai`);
  } catch (e) {
    console.warn('[ADB] reverse that bai, thu port 8081...');
    try {
      execSync(`"${adbPath}" reverse tcp:8081 tcp:${HTTP_PORT}`, { stdio: 'ignore' });
      console.log('[ADB] Reverse: localhost:8081 <-> dien thoai');
    } catch (e2) {
      console.error('[ADB] reverse that bai:', e2.message);
    }
  }
}

function checkDevice() {
  const devices = getDevices();
  const connected = devices.filter(d => d.status === 'device');
  if (connected.length === 0) {
    console.warn('[ADB] Khong co thiet bi. Ket noi USB va chap nhan USB Debugging.');
    return null;
  }
  deviceId = connected[0].id;
  return deviceId;
}

// ── SSE broadcast ───────────────────────────────────────────────────────────

function broadcastScan(text) {
  lastScan = text;
  scanHistory.unshift({ text, time: Date.now() });
  if (scanHistory.length > 20) scanHistory.pop();

  const data = `data: ${JSON.stringify({ text, time: Date.now() })}\n\n`;
  sseClients = sseClients.filter((res) => {
    try {
      if (!res.writableEnded) {
        res.write(data);
        return true;
      }
    } catch { /* ignore */ }
    return false;
  });
}

// ── HTTP Server ─────────────────────────────────────────────────────────────

function createServer() {
  return http.createServer((req, res) => {
    const url = req.url.split('?')[0];

    // ── POST /scan — dien thoai gui barcode text ────────────────────────
    if (req.method === 'POST' && url === SCAN_PATH) {
      let body = '';
      req.on('data', chunk => { body += chunk; });
      req.on('end', () => {
        try {
          const { text } = JSON.parse(body);
          if (text && typeof text === 'string') {
            const trimmed = text.trim();
            console.log(`[SCAN] "${trimmed}" (tu dien thoai)`);
            broadcastScan(trimmed);
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ ok: true, received: trimmed }));
          } else {
            res.writeHead(400, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ ok: false, error: 'missing text' }));
          }
        } catch {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ ok: false, error: 'invalid json' }));
        }
      });
      return;
    }

    // ── GET /events — SSE cho web app lang nghe scan ────────────────────
    if (req.method === 'GET' && url === WS_PATH) {
      res.writeHead(200, {
        'Content-Type': 'text/event-stream',
        'Cache-Control': 'no-cache',
        'Connection': 'keep-alive',
        'Access-Control-Allow-Origin': '*',
        'X-Accel-Buffering': 'no',
      });

      // Gui trang thai hien tai
      const init = JSON.stringify({ type: 'connected', lastScan, clients: sseClients.length + 1 });
      res.write(`data: ${init}\n\n`);

      sseClients.push(res);
      console.log(`[SSE] Client connected (${sseClients.length} total)`);

      // Heartbeat
      const heartbeat = setInterval(() => {
        try { res.write(': heartbeat\n\n'); } catch { clearInterval(heartbeat); }
      }, 15000);

      req.on('close', () => {
        clearInterval(heartbeat);
        sseClients = sseClients.filter(c => c !== res);
        console.log(`[SSE] Client disconnected (${sseClients.length} total)`);
      });
      return;
    }

    // ── GET /status ─────────────────────────────────────────────────────
    if (url === '/status') {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        ok: true,
        device: deviceId,
        sseClients: sseClients.length,
        lastScan,
        history: scanHistory.slice(0, 10),
      }));
      return;
    }

    // ── GET /phone — tra ve trang HTML cho dien thoai ──────────────────
    if (url === '/phone' || url === '/') {
      try {
        const html = require('fs').readFileSync(path.join(__dirname, 'phone-server', 'index.html'), 'utf8');
        res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
        res.end(html);
      } catch {
        res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
        res.end(getPhoneFallbackPage());
      }
      return;
    }

    // Fallback
    res.writeHead(302, { 'Location': '/phone' });
    res.end();
  });
}

// ── Phone fallback page (neu khong co index.html) ──────────────────────────

function getPhoneFallbackPage() {
  return `<!DOCTYPE html>
<html><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>USB Scanner</title>
<style>
  body { font-family: sans-serif; background: #111; color: #fff; display: flex; align-items: center; justify-content: center; min-height: 100vh; text-align: center; padding: 20px; }
  .card { background: #1a1a2e; border-radius: 16px; padding: 2rem; max-width: 400px; }
  h1 { font-size: 1.2rem; color: #00d4ff; }
  p { color: #888; font-size: 0.9rem; margin: 1rem 0; }
  .note { font-size: 0.75rem; color: #555; margin-top: 1rem; }
</style>
</head><body>
<div class="card">
  <h1>📷 USB Barcode Scanner</h1>
  <p>Copy file <code>phone-server/index.html</code> vao dien thoai va mo bang Chrome.</p>
  <p>Hoac truy cap: <strong>http://localhost:${HTTP_PORT}/phone</strong></p>
  <p class="note">Dam bao da chay <code>node proxy-adb.js</code> va ket noi USB.</p>
</div>
</body></html>`;
}

// ── Landing page ───────────────────────────────────────────────────────────

function getLandingPage(ips) {
  const ipList = ips.map(i => `<div class="ip-row"><span>${i.name}</span><code>${i.ip}</code></div>`).join('');
  return `<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>USB Barcode Scanner</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { font-family: -apple-system, sans-serif; background: #0f1117; color: #e5e7eb; min-height: 100vh; display: flex; align-items: center; justify-content: center; }
  .card { background: #1a1d27; border: 1px solid #2d3142; border-radius: 16px; padding: 2rem; max-width: 520px; width: 95%; }
  .icon { font-size: 3rem; text-align: center; }
  h1 { text-align: center; font-size: 1.3rem; margin: 0.5rem 0; }
  p { text-align: center; color: #6b7280; font-size: 0.85rem; margin-bottom: 1.5rem; }
  .status { display: flex; align-items: center; gap: 0.5rem; background: #1f2937; border-radius: 20px; padding: 0.3rem 1rem; font-size: 0.8rem; color: #10b981; width: fit-content; margin: 0 auto 1rem; }
  .dot { width: 8px; height: 8px; background: #10b981; border-radius: 50%; animation: p 1.5s infinite; }
  @keyframes p { 0%,100%{opacity:1}50%{opacity:.3} }
  .box { background: #0f1117; border-radius: 10px; padding: 1rem; margin-bottom: 1rem; }
  h3 { font-size: 0.7rem; color: #6b7280; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 0.5rem; }
  .ip-row { display: flex; justify-content: space-between; font-size: 0.85rem; padding: 0.2rem 0; }
  code { color: #00d4ff; font-family: monospace; }
  ol { padding-left: 1.3rem; color: #9ca3af; font-size: 0.82rem; line-height: 2; }
  strong { color: #e5e7eb; }
  .btn { display: block; background: #10b981; color: #fff; padding: 0.7rem; border-radius: 8px; text-align: center; text-decoration: none; font-size: 0.95rem; font-weight: 700; margin-bottom: 1rem; transition: background 0.2s; }
  .btn:hover { background: #059669; }
  .btn2 { display: block; background: #374151; color: #9ca3af; padding: 0.7rem; border-radius: 8px; text-align: center; text-decoration: none; font-size: 0.9rem; margin-bottom: 1rem; }
  .btn2:hover { background: #4b5563; }
  .note { font-size: 0.72rem; color: #4b5563; text-align: center; line-height: 1.7; }
  .scan-log { background: #0f1117; border-radius: 8px; padding: 0.8rem; margin-top: 0.5rem; max-height: 120px; overflow-y: auto; }
  .scan-item { font-family: monospace; font-size: 0.8rem; color: #10b981; padding: 0.2rem 0; border-bottom: 1px solid #1f2937; }
  .scan-item:last-child { border-bottom: none; }
  .scan-time { color: #6b7280; font-size: 0.7rem; }
  .empty { color: #4b5563; font-size: 0.8rem; text-align: center; }
</style>
</head>
<body>
<div class="card">
  <div class="icon">📱</div>
  <h1>USB Barcode Scanner</h1>
  <p>Quet ma vach tu dien thoai, gui qua USB</p>
  <div class="status"><div class="dot"></div>USB Debugging: <code>${deviceId || '—'}</code></div>

  <div class="box">
    <h3>Huong dan (2 buoc)</h3>
    <ol>
      <li>Tren dien thoai, mo Chrome truy cap: <a href="http://localhost:${HTTP_PORT}/phone" style="color:#10b981;"><strong>http://localhost:${HTTP_PORT}/phone</strong></a></li>
      <li>Chap nhan camera, quet ma vach → text gui ve may tinh</li>
    </ol>
  </div>

  <a href="http://localhost:${HTTP_PORT}/phone" class="btn">📷 Mo Scanner Tren Dien Thoai</a>

  <div class="box">
    <h3>Lich su quet gan nhat</h3>
    <div class="scan-log" id="scanLog"><div class="empty">Chua co quet nao</div></div>
  </div>

  <p class="note">Dien thoai va may tinh ket noi qua USB cable (adb reverse). Khong can WiFi.</p>
</div>
<script>
(function() {
  const log = document.getElementById('scanLog');
  let eventSource;

  function formatTime(ts) {
    return new Date(ts).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  }

  function addScan(text, time) {
    if (log.querySelector('.empty')) log.innerHTML = '';
    const item = document.createElement('div');
    item.className = 'scan-item';
    item.innerHTML = '<span class="scan-time">' + formatTime(time) + '</span> ' + text;
    log.insertBefore(item, log.firstChild);
    if (log.children.length > 10) log.removeChild(log.lastChild);
  }

  function connect() {
    eventSource = new EventSource('http://localhost:${HTTP_PORT}${WS_PATH}');
    eventSource.onmessage = function(e) {
      try {
        var d = JSON.parse(e.data);
        if (d.text) {
          addScan(d.text, d.time || Date.now());
        }
      } catch {}
    };
    eventSource.onerror = function() {
      eventSource.close();
      setTimeout(connect, 3000);
    };
  }

  // Hien thi history
  fetch('/status').then(r => r.json()).then(d => {
    if (d.history && d.history.length) {
      log.innerHTML = '';
      d.history.slice().reverse().forEach(function(s) {
        addScan(s.text, s.time);
      });
    }
  });

  connect();
})();
</script>
</body>
</html>`;
}

function getLocalIPs() {
  const ifaces = os.networkInterfaces();
  const ips = [];
  for (const name of Object.keys(ifaces)) {
    for (const iface of ifaces[name]) {
      if (iface.family === 'IPv4' && !iface.internal) ips.push({ name, ip: iface.address });
    }
  }
  return ips;
}

// ── Device watcher ─────────────────────────────────────────────────────────

let reverseInterval = null;

function startDeviceWatcher() {
  // Kiem tra thiet bi moi 5s
  reverseInterval = setInterval(() => {
    const devices = getDevices().filter(d => d.status === 'device');
    if (devices.length > 0 && !deviceId) {
      deviceId = devices[0].id;
      console.log('[ADB] Thiet bi ket noi:', deviceId);
      setupReverse();
    } else if (devices.length === 0 && deviceId) {
      console.log('[ADB] Thiet bi ngat ket noi');
      deviceId = null;
    }
  }, 5000);
}

// ── Main ──────────────────────────────────────────────────────────────────

function main() {
  console.log('\n╔══════════════════════════════════════════╗');
  console.log('║  USB Barcode Scanner Proxy v3           ║');
  console.log('╚══════════════════════════════════════════╝\n');

  // 1) Tim adb
  adbPath = findAdb();
  if (!adbPath) {
    console.error('❌ Khong tim thay "adb" trong PATH.\n');
    console.error('Cai Android Platform Tools:');
    console.error('  1. Tai: https://developer.android.com/studio/releases/platform-tools');
    console.error('  2. Giai nen vao D:\\adb\\platform-tools');
    console.error('  3. Them D:\\adb\\platform-tools vao PATH');
    console.error('  4. Mo CMD moi, kiem tra: adb version\n');
    process.exit(1);
  }
  console.log('✅ ADB:', adbPath);
  console.log(execSync(`"${adbPath}" version`, { encoding: 'utf8' }).trim());

  // 2) Kiem tra thiet bi
  console.log('\n[1] Kiem tra thiet bi...');
  const dev = checkDevice();
  if (dev) {
    console.log('✅ Thiet bi:', dev);
    setupReverse();
  } else {
    console.log('⚠️  Chua co thiet bi. Cho ket noi...\n');
  }

  // 3) Khoi dong HTTP server
  console.log('\n[2] Khoi dong server...');
  const server = createServer();
  server.listen(HTTP_PORT, () => {
    const ips = getLocalIPs();
    console.log(`\n✅ Server chay tren: http://localhost:${HTTP_PORT}`);
    console.log(`   Trang chu:          http://localhost:${HTTP_PORT}/`);
    console.log(`   Scanner (dien thoai): http://localhost:${HTTP_PORT}/phone`);
    console.log(`   Status:             http://localhost:${HTTP_PORT}/status`);
    console.log('\n   Huong dan:');
    console.log(`   1. Tren dien thoai, mo Chrome truy cap:`);
    console.log(`      http://localhost:${HTTP_PORT}/phone`);
    console.log('   2. Chap nhan camera, quet ma vach');
    console.log('   3. Web app nhan text qua SSE.\n');
    console.log('Ctrl+C de dung.\n');
  });

  startDeviceWatcher();

  const cleanup = () => {
    console.log('\nDang don dep...');
    if (reverseInterval) clearInterval(reverseInterval);
    try {
      execSync(`"${adbPath}" reverse --remove tcp:${HTTP_PORT}`, { stdio: 'ignore' });
    } catch {}
    sseClients.forEach(c => { try { c.end(); } catch {} });
    server.close();
    process.exit(0);
  };
  process.on('SIGINT', cleanup);
  process.on('SIGTERM', cleanup);
}

main();
