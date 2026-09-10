# Plan — WiFi barcode scanner (điện thoại quét → máy tính nhận text)

## Context

Hiện tại `QrScanner.vue` mở camera **trực tiếp trên trình duyệt máy tính**.
Nhân viên ngồi máy tính phải đưa laptop/webcam vào mã vạch — bất tiện.

**Yêu cầu mới:** Nhân viên cầm **điện thoại** quét barcode → điện thoại gửi
text (chuỗi serial) qua WiFi → **máy tính tự fill vào ô input** và tra tự động.

Máy tính **KHÔNG mở camera** — chỉ nhận text.

## Sơ đồ

```
  Điện thoại (Chrome)                  Máy tính
  ┌──────────────────────┐             ┌──────────────────────┐
  │ Camera quét mã vạch │             │ Web app mở            │
  │ (getUserMedia)       │             │ WarrantyPanel          │
  │ decode thành text    │             │                       │
  └──────────┬───────────┘             └──────────▲────────────┘
             │                                        │
             │ WebSocket: { type: "scan", text: "..." } │
             └────────────────────────────────────────┘
                          ws://<IP>:4748
```

Cùng mạng LAN (WiFi nội bộ). Điện thoại + máy tính kết nối WiFi cùng 1 router.

## Thay đổi

### 1. Điện thoại — `FrontEnd/usb-camera-proxy/phone-server/index.html`

Trang HTML dùng:
- `getUserMedia({ video: { facingMode: 'environment' } })` — mở camera sau
- `@zxing/browser` — decode QR + barcode 1D (CODE128, EAN13, CODE39...)
- WebSocket gửi `{ type: 'scan', text: '...' }` khi quét được

Hiển thị:
- Camera preview (full screen, điện thoại cầm ngang)
- Khung quét + animation
- Khi quét được: rung nhẹ (navigator.vibrate) + gửi ngay

### 2. Proxy — `FrontEnd/usb-camera-proxy/proxy.js`

Chuyển từ "proxy stream MJPEG" → **proxy forward WebSocket**:
- WebSocket server (port 4748) nhận `{ type: 'scan', text }` từ điện thoại
- HTTP endpoint `/scan` để web app (cùng máy tính) kết nối
- Khi nhận được scan → broadcast đến tất cả clients đang kết nối qua HTTP

**Cấu trúc đơn giản hơn nhiều** — không cần MJPEG, không cần sharp, không cần ADB.

### 3. Web app — `FrontEnd/QLBanMayTinh/src/components/admin/WarrantyPanel.vue`

Bỏ `QrScanner.vue` — thay bằng **modal nhỏ** hiện thị:
- QR code trỏ đến URL trên điện thoại
- Hoặc text hướng dẫn + link để nhân viên mở

Khi proxy nhận scan từ điện thoại:
- Web app nhận được qua SSE (Server-Sent Events) hoặc polling `/scan/latest`
- Fill `serialInput.value = text`
- Auto gọi `lookupSerial()`

### 4. Công nghệ kết nối

| Công nghệ | Vai trò |
|-----------|---------|
| WebSocket | Điện thoại → Proxy |
| Server-Sent Events (SSE) | Proxy → Web app (đơn giản hơn WS cho 1 chiều) |

Dùng SSE cho web app → đơn giản hơn WS vì web app chỉ nhận, không gửi.

### 5. Files

| File | Hành động |
|------|-----------|
| `phone-server/index.html` | Rewrite — dùng zxing để quét + gửi WebSocket |
| `proxy.js` | Rewrite — chỉ forward WS→SSE |
| `package.json` | Bỏ `sharp` (không cần nữa) — chỉ cần `ws` |
| `WarrantyPanel.vue` | Thêm modal QR code + SSE listener |
| `QrScanner.vue` | Xóa khỏi WarrantyPanel (chỉ giữ ở các chỗ cần camera trực tiếp) |

### 6. Yêu cầu mạng

- **Cùng WiFi LAN** (192.168.x.x) — không cần USB, không cần ADB
- HTTP & WS có thể chạy không cần HTTPS trong LAN
- Nếu WiFi nội bộ chặn port, có thể đổi port (mặc định 4748)

### 7. Cách dùng (cuối cùng)

1. Trên **máy tính**, chạy `node proxy.js` → terminal in ra URL (VD: `192.168.1.93:4748`)
2. Mở web app → tab **Bảo hành** → bấm nút **📷 camera** → modal hiện QR code
3. Trên **điện thoại**, quét QR code → mở Chrome → cho phép camera → bắt đầu quét
4. Đưa điện thoại vào mã vạch → quét xong → **tự động fill vào ô input + tra** trên máy tính

## Verification

1. Cùng WiFi, chạy proxy trên máy tính, mở web app
2. Mở trang phone-server trên điện thoại (Chrome)
3. Cầm điện thoại quét một mã QR / barcode
4. **Mong đợi:** text barcode xuất hiện trong ô input trên máy tính → tự tra
5. Test nhiều lần liên tiếp (5-10 lần) để chắc kết nối ổn định

## Bỏ qua (YAGNI)

- ❌ MJPEG stream (không cần thiết)
- ❌ ADB / USB
- ❌ Camera preview trên máy tính
- ❌ Chuyển đổi nguồn camera (direct/USB/custom)
- ❌ Quét từ ảnh upload
