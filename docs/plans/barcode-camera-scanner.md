# Plan — Camera barcode scanner cho ứng dụng

## Context

Ứng dụng đang có `QrScanner.vue` (dùng `jsQR` từ CDN, chỉ quét được QR code).
Serial trong kho dùng barcode 1D (CODE128/EAN-13), không phải QR. Feature
tra cứu serial vừa làm ở tab Bảo hành cần hỗ trợ camera điện thoại để quét
(khi không có máy quét USB).

**Mục tiêu:** Nâng cấp `QrScanner.vue` thành module quét **chung (QR + barcode 1D)**,
dùng lại được ở nhiều chỗ (Bảo hành, Kho, Bán hàng).

## Thiết kế tổng thể

### 1. Thư viện quét

Dùng **`@AzurSdk/barcode-scanner`** (npm) — hỗ trợ cả QR lẫn barcode 1D
(CODE128, CODE39, EAN13, EAN8, UPC, ITF, DataMatrix, PDF417, Aztec...).
Nếu không ổn định, fallback là `@zxing/browser`.

> **Không dùng** `jsQR` nữa vì chỉ quét QR, không hỗ trợ 1D.

### 2. Cấu trúc module

```
src/
  composables/
    useBarcodeScanner.js       ← logic quét chung (start/stop/scan, su dung BrowserMultiFormatReader)
  components/
    common/
      QrScanner.vue           ← component UI quay camera (tái cấu trúc, dùng useBarcodeScanner)
```

**`useBarcodeScanner.js`** — composable trả về:
```js
{
  isOpen, isScanning, error,
  start(),          // mo camera
  stop(),           // dong camera
  onScan(callback)  // lang nghe su kien quet → callback(data)
}
```

**`QrScanner.vue`** — component UI, dùng `useBarcodeScanner` bên trong:
- Camera preview + scanning frame animation (giữ nguyên từ bản cũ)
- Nút "Mở camera" / "Đóng camera"
- Hiện kết quả quét
- `emit('scanned', data)` khi quét được

### 3. Tích hợp vào WarrantyPanel

Thêm nút icon **camera** bên cạnh ô input serial trong khung tra cứu:

```
┌─ Khung tra cuu serial ─────────────────────────────────────┐
│ [🔍 Quet hoac go so serial...    ] [🔍] [📷] [Tra cuu]   │
└─────────────────────────────────────────────────────────────┘
```

- Bấm icon camera → mở overlay `QrScanner.vue` (popover/modal nhỏ)
- Quét được barcode → `emit('scanned')` → tự động điền vào `serialInput` → gọi `lookupSerial()`
- Tự động đóng camera sau khi quét thành công

### 4. Mở rộng tương lai (YAGNI, không làm bây giờ)

- POS Panel: quét serial vào đơn hàng
- Inventory Panel: quét serial khi nhập kho
- Những chỗ này có thể dùng lại `useBarcodeScanner` / `QrScanner.vue` y hệt.

## Phạm vi thay đổi

### Backend
**Không có** — toàn bộ xử lý ở frontend (quét + decode trên trình duyệt).

### Frontend

1. **`package.json`** — thêm `@AzurSdk/barcode-scanner` (hoặc `@zxing/browser`).

2. **Mới:** [`composables/useBarcodeScanner.js`](FrontEnd/QLBanMayTinh/src/composables/useBarcodeScanner.js)
   — composable dùng `BrowserMultiFormatReader` (zxing-js/browser), gọi
   `decodeFromVideoDevice()`, emit `scan(data)`.

3. **Mở rộng:** [`QrScanner.vue`](FrontEnd/QLBanMayTinh/src/components/common/QrScanner.vue)
   — thay jsQR bằng `useBarcodeScanner`, giữ nguyên UI + animation + error states.
   Props: `autoClose` (default: true), `hint` (text hướng dẫn).
   Emit: `scanned(data)`, `close`.

4. **Mở rộng:** [`WarrantyPanel.vue`](FrontEnd/QLBanMayTinh/src/components/admin/WarrantyPanel.vue)
   — thêm nút camera icon bên cạnh input serial, mở `QrScanner.vue` khi bấm.
   Khi `scanned` event fire → set `serialInput` → gọi `lookupSerial()`.
   Cần thêm state: `showCameraScanner` (ref bool) + method `handleScanned(data)`.

5. **Mở rộng:** i18n `vi.js` + `en.js` — thêm key
   `admin.warrantyScan.camera*` (mở camera, quét, lỗi camera).

### Files tham chiếu (chỉ đọc)

- [`QrScanner.vue`](FrontEnd/QLBanMayTinh/src/components/common/QrScanner.vue) — UI hiện tại để tái cấu trúc
- [`WarrantyPanel.vue`](FrontEnd/QLBanMayTinh/src/components/admin/WarrantyPanel.vue) — nơi gắn nút camera

## Verification

1. **`npm install`** — cài `@AzurSdk/barcode-scanner`, không lỗi.
2. **`vite build`** — compile thành công.
3. **Test trên điện thoại (Chrome/Safari):**
   - Vào tab Bảo hành, bấm nút camera → hiện preview.
   - Quét barcode 1D (điện thoại để trước tem serial trên máy) → kết quả điền vào ô input + tự tra.
   - Quét QR code → cũng hoạt động.
   - Camera tự đóng sau khi quét (nếu `autoClose: true`).
4. **Desktop:** bấm nút camera → nếu có webcam, preview + quét thử với QR/barcode trên màn hình.
5. **Lỗi:** không cho phép camera → hiện thông báo lỗi rõ ràng.

## Những gì BỊ BỎ (YAGNI)

- **Không làm:** quét từ ảnh đã chụp sẵn (upload file).
- **Không làm:** hỗ trợ nhiều camera (chuyển trước/sau).
- **Không làm:** lưu lịch sử quét trong session.
- **Không làm:** WebUSB / kết nối máy quét chuyên dụng (chỉ camera trình duyệt).
