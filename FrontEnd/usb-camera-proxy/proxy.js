#!/usr/bin/env node
/**
 * USB Camera Proxy v2
 *
 * Chu thich day du: xem README.md
 *
 * Cach hoat dong:
 *   1. Dien thoai mo file index.html (trong thu muc phone-server)
 *      => Dien thoai bat camera, ket noi WebSocket den may tinh
 *   2. Proxy chay tren may tinh, lang nghe WebSocket
 *   3. Proxy chuyen frame thanh MJPEG stream
 *   4. Web app (Chrome tren may tinh) truy cap http://localhost:4747/video
 *
 * Khong can ADB port forwarding — dien thoai ket noi thang qua IP mang LAN
 * hoac qua USB tethering (dien thoai lam IP diem phat WiFi).
 */

const http = require('http');
const WebSocket = require('ws');
const { exec } = require('child_process');
const { promisify } = require('util');
const execAsync = promisify(exec);
const fs = require('fs');
const path = require('path');
const os = require('os');

const PORT = 4747;
const WS_PORT = 4748;

// ── Network helpers ──────────────────────────────────────────────────────────────

function getLocalIPs() {
  const ifaces = os.networkInterfaces();
  const ips = [];
  for (const name of Object.keys(ifaces)) {
    for (const iface of ifaces[name]) {
      if (iface.family === 'IPv4' && !iface.internal) {
        ips.push({ name, ip: iface.address });
      }
    }
  }
  return ips;
}

function getPhoneInstructions(localIP) {
  return [
    `1. Tren dien thoai, mo file: FrontEnd/usb-camera-proxy/phone-server/index.html`,
    `2. (Hoac) Copy file index.html vao bo nho dien thoai, mo bang Chrome`,
    `3. Cho phep truy cap camera khi Chrome hoi`,
    `4. Nhan "Khoi dong Camera"`,
    `5. Nhap dia chi hien thi vao trinh duyet may tinh: http://${localIP}:${PORT}/video`,
    ``,
    `   Sau khi mo camera tren dien thoai:`,
    `   http://localhost:${PORT}/video    (mo tren Chrome may tinh)`,
  ].join('\n');
}

// ── JPEG frame storage (shared between WS and HTTP) ────────────────────────────

let latestJpeg = null;
let frameWidth = 640;
let frameHeight = 480;
let clientCount = 0;

// ── HTTP Server (MJPEG stream) ─────────────────────────────────────────────────

function createHttpServer() {
  return http.createServer((req, res) => {
    const url = req.url.split('?')[0];

    // Landing / status
    if (url === '/' || url === '/index.html') {
      const localIPs = getLocalIPs();
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8', 'Cache-Control': 'no-cache' });
      res.end(getLandingPage(localIPs));
      return;
    }

    // Health check
    if (url === '/status') {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ ok: true, clients: clientCount, hasFrame: !!latestJpeg }));
      return;
    }

    // MJPEG stream
    if (url === '/video' || url === '/mjpeg') {
      if (!latestJpeg) {
        res.writeHead(200, {
          'Content-Type': 'text/html; charset=utf-8',
          'Cache-Control': 'no-cache',
        });
        res.end(getWaitingPage());
        return;
      }

      clientCount++;
      res.writeHead(200, {
        'Content-Type': 'multipart/x-mixed-replace; boundary=--jpgboundary',
        'Cache-Control': 'no-cache, no-store, must-revalidate',
        'Pragma': 'no-cache',
        'Expires': '0',
        'Connection': 'keep-alive',
        'Access-Control-Allow-Origin': '*',
      });

      // Send current frame immediately
      res.write(`--jpgboundary\r\nContent-Type: image/jpeg\r\nContent-Length: ${latestJpeg.length}\r\n\r\n`);
      res.write(latestJpeg);

      // Notify on disconnect
      req.on('close', () => {
        clientCount = Math.max(0, clientCount - 1);
      });

      // Send new frames as they arrive
      const sendFrame = (jpeg) => {
        if (res.writableEnded) return;
        try {
          res.write(`--jpgboundary\r\nContent-Type: image/jpeg\r\nContent-Length: ${jpeg.length}\r\n\r\n`);
          res.write(jpeg);
        } catch { /* client disconnected */ }
      };

      // Store sendFrame somewhere the WS handler can find it
      // We'll use a global registry
      global.__jpegSender = sendFrame;

      return;
    }

    // Serve phone HTML file (for convenience)
    if (url === '/phone') {
      const phoneHtml = fs.readFileSync(path.join(__dirname, 'phone-server', 'index.html'), 'utf8');
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      res.end(phoneHtml);
      return;
    }

    // Fallback: redirect to /
    res.writeHead(302, { 'Location': '/' });
    res.end();
  });
}

// ── WebSocket Server ───────────────────────────────────────────────────────────

function createWsServer(server) {
  const wss = new WebSocket.Server({ server });

  wss.on('connection', (ws, req) => {
    console.log('[WS] Client connected from:', req.socket.remoteAddress);

    ws.on('message', (data) => {
      // Client sends: { type: 'frame', jpeg: base64, w: number, h: number }
      //            or { type: 'ping' }
      try {
        const msg = JSON.parse(data.toString());

        if (msg.type === 'ping') {
          ws.send(JSON.stringify({ type: 'pong', clients: clientCount }));
          return;
        }

        if (msg.type === 'frame') {
          const jpeg = Buffer.from(msg.jpeg, 'base64');
          latestJpeg = jpeg;
          if (msg.w) frameWidth = msg.w;
          if (msg.h) frameHeight = msg.h;

          // Broadcast to all MJPEG clients
          if (global.__jpegSender) {
            global.__jpegSender(jpeg);
          }
        }
      } catch { /* ignore */ }
    });

    ws.on('close', () => {
      console.log('[WS] Client disconnected');
    });

    ws.on('error', () => { /* ignore */ });
  });

  return wss;
}

// ── Landing page ───────────────────────────────────────────────────────────────

function getLandingPage(localIPs) {
  const ipList = localIPs.map(i => `<div class="ip-row"><span class="ip-name">${i.name}</span><span class="ip-val">${i.ip}</span></div>`).join('');
  const videoUrl = `http://localhost:${PORT}/video`;
  const phoneUrl = `http://localhost:${PORT}/phone`;

  return `<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>USB Camera Proxy</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #0f1117; color: #e5e7eb; min-height: 100vh; display: flex; align-items: center; justify-content: center; }
  .card { background: #1a1d27; border: 1px solid #2d3142; border-radius: 16px; padding: 2rem; max-width: 520px; width: 95%; }
  .header { text-align: center; margin-bottom: 1.5rem; }
  .icon { font-size: 2.5rem; }
  h1 { font-size: 1.3rem; margin: 0.5rem 0; color: #fff; }
  .subtitle { color: #9ca3af; font-size: 0.85rem; }
  .ip-section { background: #0f1117; border-radius: 10px; padding: 1rem; margin-bottom: 1rem; }
  .ip-section h3 { font-size: 0.75rem; color: #6b7280; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 0.5rem; }
  .ip-row { display: flex; justify-content: space-between; align-items: center; padding: 0.3rem 0; }
  .ip-name { color: #9ca3af; font-size: 0.8rem; }
  .ip-val { font-family: monospace; color: #00d4ff; font-size: 0.95rem; }
  .steps { background: #0f1117; border-radius: 10px; padding: 1rem; margin-bottom: 1rem; }
  .steps h3 { font-size: 0.75rem; color: #6b7280; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 0.5rem; }
  .steps ol { padding-left: 1.2rem; color: #9ca3af; font-size: 0.82rem; line-height: 1.8; }
  .steps li strong { color: #e5e7eb; }
  .btn-row { display: flex; gap: 0.5rem; }
  .btn { flex: 1; display: block; padding: 0.65rem; border: none; border-radius: 8px; text-align: center; text-decoration: none; font-size: 0.85rem; font-weight: 600; cursor: pointer; transition: all 0.2s; }
  .btn-primary { background: #10b981; color: #fff; }
  .btn-primary:hover { background: #059669; }
  .btn-secondary { background: #374151; color: #9ca3af; }
  .btn-secondary:hover { background: #4b5563; }
  .note { font-size: 0.75rem; color: #6b7280; text-align: center; margin-top: 1rem; }
  .status { display: inline-flex; align-items: center; gap: 0.5rem; background: #1f2937; border-radius: 20px; padding: 0.3rem 1rem; font-size: 0.8rem; color: #10b981; margin-bottom: 1rem; }
  .dot { width: 8px; height: 8px; background: #10b981; border-radius: 50%; animation: pulse 1.5s infinite; }
  @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }
  .qr-hint { font-size: 0.75rem; color: #6b7280; margin-top: 0.5rem; }
</style>
</head>
<body>
<div class="card">
  <div class="header">
    <div class="icon">📷</div>
    <h1>USB Camera Proxy</h1>
    <div class="subtitle">Chia se camera tu dien thoai Android sang may tinh qua USB</div>
  </div>

  <div class="status"><div class="dot"></div>USB Camera Proxy dang chay</div>

  <div class="ip-section">
    <h3>Dia chi may tinh</h3>
    ${ipList}
  </div>

  <div class="steps">
    <h3>Huong dan ket noi</h3>
    <ol>
      <li>Ket noi USB giua may tinh va dien thoai Android</li>
      <li>Tren dien thoai, <strong>mo Chrome</strong> va truy cap: <a href="${phoneUrl}" style="color:#10b981;">http://localhost:${PORT}/phone</a></li>
      <li>Hoac mo file <strong>phone-server/index.html</strong> trong thu muc du an</li>
      <li>Cho phep truy cap camera tren dien thoai</li>
      <li>Nhan <strong>"Khoi dong Camera"</strong></li>
      <li>Tren may tinh, mo: <a href="${videoUrl}" style="color:#10b981;">${videoUrl}</a></li>
    </ol>
  </div>

  <div class="btn-row">
    <a href="${videoUrl}" class="btn btn-primary">📹 Xem Camera</a>
    <a href="${phoneUrl}" class="btn btn-secondary" target="_blank">📱 Huong dan tren dien thoai</a>
  </div>

  <div class="qr-hint">
    Dien thoai va may tinh can cung mot mang LAN hoac USB tethering dang bat.
    Khi bat USB tethering, dien thoai se tao IP rieng (VD: 192.168.42.129).
  </div>
</div>
</body>
</html>`;
}

function getWaitingPage() {
  return `<!DOCTYPE html>
<html><head><meta charset="UTF-8"><title>Camera — cho</title>
<style>
  body { background:#111; color:#555; display:flex; align-items:center; justify-content:center; min-height:100vh; font-family:sans-serif; text-align:center; }
  .msg { font-size:1.1rem; }
  .hint { font-size:0.8rem; margin-top:0.5rem; }
</style>
</head>
<body>
<div>
  <div class="msg">Dang cho camera tu dien thoai...</div>
  <div class="hint">Dam bao dien thoai da mo <strong>index.html</strong> va nhan "Khoi dong Camera"</div>
</div>
</body></html>`;
}

// ── Auto-detect USB tethering IP ───────────────────────────────────────────────

async function detectUsbIp() {
  if (process.platform !== 'win32') return null;
  try {
    // Windows: USB tethering thuong tao adapter "USB Ethernet" hoac "Mobile Hotspot"
    const { stdout } = await execAsync('netsh interface show interface | findstr "Connected"');
    // Lay IPv4 cua moi adapter
    const ifs = os.networkInterfaces();
    for (const name of Object.keys(ifs)) {
      for (const i of ifs[name]) {
        if (i.family === 'IPv4' && !i.internal && i.address.startsWith('192.168.42')) {
          return { name, ip: i.address };
        }
      }
    }
  } catch { /* ignore */ }
  return null;
}

// ── Main ──────────────────────────────────────────────────────────────────────

async function main() {
  console.log('\n╔══════════════════════════════════════════╗');
  console.log('║   USB Camera Proxy v2 — Camera qua USB  ║');
  console.log('╚══════════════════════════════════════════╝\n');

  // Kiem tra ADB (chi can thong bao, khong bat buoc)
  if (process.platform === 'win32') {
    try {
      await execAsync('adb version');
      console.log('[ADB] OK — ADB co san (khong bat buoc neu dung cach 2)');
    } catch {
      console.log('[ADB] Khong tim thay ADB. Ban co the su dung cach 2 (USB Tethering).');
    }
  }

  // Lay IP cuc bo
  const localIPs = getLocalIPs();
  const usbIp = await detectUsbIp();
  const primaryIP = usbIp?.ip || localIPs[0]?.ip || 'localhost';

  console.log('[IP] Dia chi mang:', localIPs.map(i => `${i.ip} (${i.name})`).join(', '));
  if (usbIp) console.log('[IP] USB Tethering detected:', usbIp.ip);

  // Khoi tao server
  const server = createHttpServer();
  createWsServer(server);

  server.listen(PORT, () => {
    console.log(`\n✅ Proxy running on: http://localhost:${PORT}`);
    console.log(`   Camera stream:         http://localhost:${PORT}/video`);
    console.log(`   Phone setup page:      http://localhost:${PORT}/phone`);
    console.log(`   Status check:         http://localhost:${PORT}/status\n`);

    if (localIPs.length > 0) {
      console.log('   Tu dien thoai, truy cap:');
      localIPs.forEach(i => {
        console.log(`   → http://${i.ip}:${PORT}/phone`);
      });
    }

    console.log('\nChi dan:');
    console.log(getPhoneInstructions(primaryIP).replace(/^/gm, '   '));
    console.log('\nDong nay (Ctrl+C) khi khong su dung.\n');
  });

  const cleanup = () => {
    console.log('\nDang don dep...');
    server.close();
    process.exit(0);
  };
  process.on('SIGINT', cleanup);
  process.on('SIGTERM', cleanup);
}

main().catch(err => {
  console.error('\n❌ Loi:', err.message);
  process.exit(1);
});
