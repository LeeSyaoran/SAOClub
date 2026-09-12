# Plan — USB Barcode Scanner (điện thoại quét → gửi text qua USB → máy tính nhận)

## Context

Nhân viên cầm điện thoại quét barcode sản phẩm. Điện thoại decode barcode được text,
gửi text qua USB cable đến máy tính. Máy tính chỉ nhận text, fill vào ô input,
**không mở camera**.

## Sơ đồ

```
  Điện thoại                              Máy tính
  ┌─────────────────────────────────┐   ┌─────────────────────────────┐
  │ Chrome: index.html               │   │ Node proxy: proxy-adb.js    │
  │   - Camera (getUserMedia)        │   │   - HTTP server :8080       │
  │   - zxing decode → text          │   │   - POST /scan → broadcast  │
  │   - fetch('localhost:8080/scan') │   │   - GET  /events → SSE     │
  └──────────────┬──────────────────┘   └───────────────▲──────────────┘
                 │  adb reverse                              │
                 │  localhost:8080 ◄───────────────────────┘
                 │  (USB cable)
                 │
  ──────────────────────────────────────────────────────────────
  1. `adb reverse tcp:8080 tcp:8080`
  2. Phone browser → localhost:8080 (USB tunnel)
  3. Proxy nhận POST /scan → broadcast SSE
  4. Web app nhận SSE → fill input
```

## 3 thành phần

### 1. `proxy-adb.js` (máy tính)

- HTTP server port 8080
- `POST /scan` — nhận `{ text: '...' }` từ điện thoại
- `GET /events` — Server-Sent Events, broadcast scan text đến web app
- Tự động chạy `adb reverse tcp:8080 tcp:8080`
- Đợi thiet bi Android kết nối

### 2. `phone-server/index.html` (điện thoại)

- Mở camera (getUserMedia), điện thoại đứng yên
- Dùng @zxing/browser decode barcode liên tục
- Khi quét được: POST lên `http://localhost:8080/scan`
- Không cần stream gì cả, chỉ gửi text
- Hiển thị: camera preview + "Đang quét..." + khi quét được → hiện text + gửi

### 3. `WarrantyPanel.vue` (web app)

- Khi bấm nút camera → mở SSE kết nối `http://localhost:8080/events`
- Khi nhận được scan text → fill `serialInput` + gọi `lookupSerial()`
- Hiện modal: "Đang kết nối USB..." / "Đã kết nối — quét trên điện thoại"

## Files

| File | Action |
|------|--------|
| `proxy-adb.js` | Rewrite hoàn toàn — chi can HTTP POST/SSE, bo MJPEG |
| `phone-server/index.html` | Rewrite — zxing decode + fetch POST |
| `WarrantyPanel.vue` | Them SSE listener, hien modal trang thai |
| `QrScanner.vue` | Khong su dung trong flow nay |

## Không can

- sharp (PNG→JPEG)
- ws (WebSocket)
- MJPEG stream
- ADB exec-out screencap

## Verification

1. Ket noi USB, bat USB Debugging
2. `node proxy-adb.js` tren may tinh
3. Mo Chrome tren dien thoai, truy cap `http://localhost:8080`
4. Cho phep camera, nhin vao barcode
5. Web app (cung may tinh) tu dong fill + tra serial
