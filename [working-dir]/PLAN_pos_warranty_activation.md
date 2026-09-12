# Kế hoạch: Kích hoạt & Tra cứu Bảo hành từ POS

## Tổng quan

Quy trình có 2 giai đoạn:

```
Giai đoạn 1: Mua hàng tại POS
───────────────────────────────────────────────
Nhân viên quét/barcode sản phẩm
  → Chọn serial → Tạo đơn hàng
  → Khi giao hàng → Cập nhật ngày giao thực tế
  → Bảo hành tự động kích hoạt (tính từ ngày giao)

Giai đoạn 2: Khách đến bảo hành
───────────────────────────────────────────────
Nhân viên quét barcode sản phẩm
  → Hiển thị: Khách hàng, SĐT, Ngày mua, Hạn BH, Lịch sử BH
  → Tạo phiếu bảo hành ngay tại chỗ
```

---

## Giai đoạn 1 — Tự động kích hoạt bảo hành khi giao hàng

### Trigger
Khi `DonHang.trangThai` chuyển → `hoan_thanh` VÀ `ngayGiaoThucTe` được gán → Bảo hành kích hoạt.

### Thay đổi

**Backend (2 file)**

1. `DonHangService.java` — sửa `updateTrangThai()`:
   - Khi chuyển sang `hoan_thanh`: gọi `kichHoatBaoHanhTheoDonHang(donHangId)`.
   - Hàm `kichHoatBaoHanhTheoDonHang()`: với mỗi serial trong đơn → set `trangThai = 'da_ban'` (đã bán) + log.

2. `DonHangController.java` — thêm endpoint kích hoạt thủ công:
   ```
   POST /api/don-hang/{id}/giao-hang
   Body: { ngayGiaoThucTe: "2026-09-08T10:30:00" }
   → Giao hàng + kích hoạt bảo hành
   ```

3. *(Tùy chọn)* `DonHangService` — gửi notification/Zalo khi kích hoạt BH (thông báo cho khách: "Sản phẩm của bạn đã được kích hoạt bảo hành đến ngày X").

---

## Giai đoạn 2 — Tra cứu bảo hành nhanh tại POS

### Mục tiêu
Nhân viên quét barcode → hiển thị ngay thông tin khách + hạn BH → quyết định tạo phiếu BH hoặc từ chối.

### Thay đổi

**Frontend — POS (1 file lớn)**

`PosPage.vue` — thêm tab **"Bảo hành"** trong POS:

```
┌──────────────────────────────────────────────────────┐
│  [Bán hàng]  [Bảo hành]                             │
├──────────────────────────────────────────────────────┤
│  🔍 Quét barcode hoặc gõ serial...         [📷]    │
├──────────────────────────────────────────────────────┤
│                                                      │
│  ✅ Còn bảo hành — còn 247 ngày                   │
│                                                      │
│  ┌──────────┐  MacBook Pro 14 M3 Pro               │
│  │  Ảnh    │  SKU: MB-M3P-18-512-BK              │
│  │  160x160 │  Serial: #C02X1234Y                  │
│  └──────────┘                                       │
│                                                      │
│  ┌─ Thông tin khách ──┐  ┌─ Bảo hành ────────────┐│
│  │ Nguyễn Văn Minh    │  │ Ngày mua: 12/03/2026 ││
│  │ 📞 0901234567      │  │ Hết hạn:  12/03/2027  ││
│  │ 📧 minh@email.com  │  │ Còn lại:  247 ngày   ││
│  └────────────────────┘  └─────────────────────────┘│
│                                                      │
│  Lịch sử bảo hành (1 lần)                          │
│  ┌────────────────────────────────────────────────┐ │
│  │ #45 · 12/06/2026 · Đang xử lý                  │ │
│  │ Lỗi: Loa không ra tiếng                        │ │
│  └────────────────────────────────────────────────┘ │
│                                                      │
│  [Tạo phiếu bảo hành mới]     [Hết hạn BH - Từ chối]│
└──────────────────────────────────────────────────────┘
```

**Chi tiết thay đổi `PosPage.vue`:**

1. Thêm tab `activeTab = ref('sell' | 'warranty')` + UI tab.
2. Di chuyển toàn bộ lookup logic từ `WarrantyPanel.vue` vào `useWarrantyLookup.js` (composable) để tái sử dụng cả 2 chỗ:
   - `WarrantyPanel.vue` (tab Bảo hành trong Admin)
   - `PosPage.vue` (tab Bảo hành trong POS)
3. Trong tab Bảo hành POS:
   - **Serial chưa bán** → "Máy này chưa được bán. Không thể tiếp nhận bảo hành."
   - **Còn BH** → Hiển thị đầy đủ thông tin + nút "Tạo phiếu BH".
   - **Hết BH** → Hiển thị thông tin + nút "Từ chối BH" (ghi nhận từ chối, không tạo phiếu).
   - **Không tìm thấy** → "Mã không tồn tại trong hệ thống."
4. Nút "Tạo phiếu bảo hành" trong POS → mở `WarrantyClaimModal` với form đã pre-fill:
   - `chiTietId`, `sanPhamId`, `khachHangId`, `ngayMua`, `ngayHetBh` được điền sẵn từ lookup.
   - Nhân viên chỉ cần nhập: `moTaLoi`, `ngayTiepNhan`.

**Tái sử dụng composable `useWarrantyLookup.js`:**

```js
// src/composables/useWarrantyLookup.js
export const useWarrantyLookup = () => {
  const serialInput = ref('')
  const lookupResult = ref(null)
  const lookupLoading = ref(false)
  const lookupError = ref('')
  const lookupErrorCode = ref('')
  // ... USB scanner, barcode camera
  const lookupSerial = async () => { /* existing logic */ }
  const clearLookup = () => { /* existing logic */ }
  return { serialInput, lookupResult, lookupLoading, lookupError, lookupErrorCode,
           lookupSerial, clearLookup, openUsbScanner, closeUsbScanner }
}
```

---

## Thứ tự thực hiện

```
Bước 1: Tạo useWarrantyLookup.js (composable)
         → Tách logic lookup từ WarrantyPanel.vue

Bước 2: Cập nhật WarrantyPanel.vue dùng composable
         → Không thay đổi UI, chỉ refactor code

Bước 3: DonHangService — kích hoạt BH khi giao hàng
         → Thêm khi trangThai = hoan_thanh
         → Thêm endpoint POST /api/don-hang/{id}/giao-hang

Bước 4: PosPage — thêm tab Bảo hành + dùng composable
         → Nút quét barcode/camera
         → Hiển thị thông tin khách + BH
         → Tạo phiếu BH pre-fill

Bước 5: Test end-to-end
         → Mua hàng → giao hàng → kiểm tra BH kích hoạt
         → Khách đến → quét → xem thông tin → tạo phiếu BH
```

---

## File cần tạo mới

- `FrontEnd/QLBanMayTinh/src/composables/useWarrantyLookup.js`

## File cần sửa

### Backend
- `BackEnd/src/main/java/com/example/backend/service/DonHangService.java` — kích hoạt BH khi giao
- `BackEnd/src/main/java/com/example/backend/controller/DonHangController.java` — endpoint giao hàng

### Frontend
- `FrontEnd/QLBanMayTinh/src/components/admin/WarrantyPanel.vue` — dùng composable
- `FrontEnd/QLBanMayTinh/src/pages/admin/PosPage.vue` — thêm tab Bảo hành

## Tra cứu nhanh — Logic hiện tại đã hỗ trợ gì?

| Thông tin | Đã có trong `WarrantyLookupResponse` | Ghi chú |
|-----------|--------------------------------------|---------|
| Tên khách hàng | ✅ `tenKhachHang` | |
| SĐT khách | ✅ `soDienThoai` | |
| Email khách | ❌ chưa có | Cần thêm |
| Ngày mua (giao) | ✅ `ngayGiaoThucTe` | |
| Hạn bảo hành | ✅ `ngayHetBaoHanh` | |
| Số ngày còn lại | ✅ computed frontend | |
| Lịch sử phiếu BH | ✅ `lichSuPhieuBaoHanh` | |
| Ảnh sản phẩm | ✅ `hinhAnhBienThe` | |
| Tên sản phẩm | ✅ `tenSanPham` | |
| SKU | ✅ `maSku` | |
| Serial | ✅ `soSerial` | |
| Mã đơn hàng | ✅ `maDonHang` | |

### Cần thêm vào `WarrantyLookupResponse`

```java
// KhachHang (neu serial da ban)
private String emailKhachHang;  // ← THÊM
```

Frontend: gọi API thêm field `email` từ khách hàng.

---

## Chi tiết Bước 3 — Kích hoạt bảo hành tự động

### Trigger trong `DonHangService`

```java
public DonHang updateTrangThai(Integer id, String trangThai) {
    DonHang dh = donHangRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Đơn không tồn tại"));

    String trangThaiCu = dh.getTrangThai();
    dh.setTrangThai(trangThai);
    DonHang saved = donHangRepository.save(dh);

    // Kích hoạt bảo hành khi giao hàng thành công
    if ("hoan_thanh".equals(trangThai) && !trangThaiCu.equals(trangThai)) {
        kichHoatBaoHanhTuDong(saved);
    }

    return saved;
}

private void kichHoatBaoHanhTuDong(DonHang dh) {
    // Lấy tất cả serial trong đơn
    List<ChiTietDonHangSerial> serials = chiTietDonHangSerialRepository
            .findByDonHangId(dh.getId());
    for (ChiTietDonHangSerial s : serials) {
        ChiTietSanPham c = s.getChiTietSanPham();
        if (c != null && !"da_ban".equals(c.getTrangThai())) {
            c.setTrangThai("da_ban");
            chiTietSanPhamRepository.save(c);
        }
    }
}
```

### Endpoint giao hàng thủ công

```
POST /api/don-hang/{id}/giao-hang
Body: { ngayGiaoThucTe: "2026-09-08T10:30:00" }

→ Set ngayGiaoThucTe + trangThai = hoan_thanh
→ Gọi kichHoatBaoHanhTuDong()
→ Trả về đơn hàng đã cập nhật
```

### Frontend POS — nút "Giao hàng"

Trong `PosPage.vue`, khi tạo đơn hàng thành công → gọi:

```js
// Tự động giao hàng nếu khách lấy ngay (pickup at store)
if (deliveryMode === 'pickup') {
  await DonHangService.giaoHang(orderId, { ngayGiaoThucTe: new Date().toISOString() });
}
```
