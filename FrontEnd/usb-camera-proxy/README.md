# USB Barcode Scanner

## Cách hoạt động

```
  Điện thoại (Chrome)                Máy tính
  ┌──────────────────┐             ┌─────────────────┐
  │ Camera quet      │             │ proxy-adb.js    │
  │ barcode          │             │ HTTP :8484      │
  │ decode → text    │ fetch POST  │ POST /scan      │
  │                  │ ──────────► │ GET  /events → SSE │
  └──────────────────┘             └────────┬────────┘
        adb reverse                        │
        localhost:8484 ◄──────────────────┘
        (USB cable)

  Web app ◄─── SSE ───────────────────────┘
  WarrantyPanel ◄── auto fill serialInput
```

## Yêu cầu

- Máy tính: **adb** trong PATH
- Điện thoại: bật **USB Debugging**
- Cáp USB kết nối 2 thiết bị

## Bước 1: Cài ADB (nếu chưa có)

1. Tải [Android Platform Tools](https://developer.android.com/studio/releases/platform-tools)
2. Giải nén vào `D:\adb\platform-tools`
3. Thêm vào PATH:
   ```powershell
   setx PATH "$env:PATH;D:\adb\platform-tools"
   ```
4. Mở CMD mới, kiểm tra: `adb version`

## Bước 2: Chạy proxy

```bash
cd FrontEnd/usb-camera-proxy
npm install   # chỉ cần chạy 1 lần
node proxy-adb.js
```

Terminal sẽ hiển thị:
```
╔══════════════════════════════════════════╗
║  USB Barcode Scanner Proxy v3           ║
╚══════════════════════════════════════════╝

✅ ADB: D:\adb\platform-tools\adb.exe
Android Debug Bridge version 1.0.41

[1] Kiem tra thiet bi...
✅ Thiet bi: R5CR80X0123
[ADB] Da reverse: localhost:8484 <-> dien thoai

[2] Khoi dong server...

✅ Server chay tren: http://localhost:8484
   Trang chu:          http://localhost:8484/
   Scanner (dien thoai): http://localhost:8484/phone
```

## Bước 3: Mở scanner trên điện thoại

1. Mở **Chrome** trên điện thoại
2. Truy cập: `http://localhost:8080/phone`
3. Cho phép truy cập camera
4. Màn hình hiện camera + khung quét → đưa camera vào mã vạch

## Bước 4: Quét barcode

1. Cầm điện thoại, đưa camera vào mã vạch sản phẩm
2. Điện thoại **decode** barcode → gửi text qua USB
3. **Máy tính tự động:**
   - Điền serial vào ô tra cứu
   - Tra thông tin bảo hành

## Web app (WarrantyPanel)

Mở ứng dụng → Tab **Bảo hành** → bấm nút **📷 camera** → modal hướng dẫn.

Mỗi lần điện thoại quét được → serial tự fill vào input + auto lookup.

## Khắc phục lỗi

| Lỗi | Cách xử lý |
|------|-----------|
| `adb: command not found` | Cài Android Platform Tools, thêm vào PATH |
| `Thiết bị chưa được phép` | Bấm "Cho phép USB Debugging" trên điện thoại |
| Điện thoại trắng màn hình | Tải file `phone-server/index.html`, mở trực tiếp bằng Chrome |
| Máy tính không nhận scan | Kiểm tra `adb reverse` đã chạy chưa |

Kiểm tra trạng thái:
```bash
curl http://localhost:8080/status
```

## Thu hồi quyền khi không dùng

Tắt **USB Debugging** trong Cài đặt → Tùy chọn nhà phát triển khi không cần dùng.
