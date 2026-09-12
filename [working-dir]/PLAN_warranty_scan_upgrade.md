# Nâng cấp khung tra cứu bảo hành — Layout, 3 trạng thái, phân biệt barcode không tồn tại

## Context

Trang quét barcode trong `WarrantyPanel.vue` hiện thiếu nhiều thông tin quan trọng (ảnh nhỏ 72px, không có banner trạng thái nổi bật, không phân biệt được serial "chưa từng tồn tại" vs "đã bị xóa khỏi hệ thống"). Nhân viên kho/bảo hành quét nhanh nhiều máy liên tục nên cần:

- Bố cục dễ đọc, ảnh sản phẩm to, thông tin phân nhóm rõ ràng.
- Banner trạng thái BH ở đầu card để nhận biết 1 giây: **Trong kho chưa bán** / **Còn bảo hành** / **Hết bảo hành**.
- Phân biệt thông báo "Mã `{serial}` không tồn tại trong hệ thống" (chưa từng có) vs "Mã `{serial}` đã bị xóa khỏi hệ thống" (đã từng tồn tại).
- Với máy trong kho chưa bán: nút "Xem chi tiết biến thể" nhảy sang tab sản phẩm với biến thể đã chọn.

## Tập tin cần thay đổi

### Backend (3 tệp)
- `BackEnd/src/main/java/com/example/backend/repository/ChiTietSanPhamRepository.java` — thêm query tra cứu serial đã bị xóa mềm theo `barcode` hoặc `so_serial`, bao gồm cả những row có cờ xóa.
- `BackEnd/src/main/java/com/example/backend/response/WarrantyLookupResponse.java` — thêm field `daXoa: Boolean` để frontend phân biệt.
- `BackEnd/src/main/java/com/example/backend/service/PhieuBaoHanhService.java` — sửa `traCuuSerial()`:
  - Query trước với `daXoa=false` (chưa xóa) để lấy thông tin bình thường.
  - Nếu rỗng, query lại bao gồm cả row đã xóa mềm để phát hiện "đã từng tồn tại".
  - Throw `EntityNotFoundException` với message khác nhau: `"NOT_FOUND"` (chưa từng có) hoặc `"DELETED"` (đã xóa).
- `BackEnd/src/main/java/com/example/backend/exception/GlobalExceptionHandler.java` — thêm handler riêng cho 2 message trên trả về HTTP 404 với body phân biệt (`{code, message}`).

### Frontend (4 tệp)
- `FrontEnd/QLBanMayTinh/src/services/PhieuBaoHanhService.js` — giữ nguyên signature, parse body lỗi 404 để lấy `code`.
- `FrontEnd/QLBanMayTinh/src/components/admin/WarrantyPanel.vue`:
  - Tái cấu trúc template khung tra cứu: banner trạng thái đầu card (3 màu), ảnh sản phẩm to ~180px bên trái, thông tin chia 2 cột (Thông tin máy / Cấu hình chi tiết).
  - Thêm `lookupErrorCode` (`'NOT_FOUND'` | `'DELETED'` | `''`) để render đúng thông báo.
  - Thêm `variantLink` và router-link "Xem chi tiết biến thể" cho trường hợp `trangThaiSerial !== 'da_ban'`.
  - Nút "Tạo phiếu bảo hành" giữ disable khi chưa bán, đổi title tooltip mới.
- `FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js` — thêm `warrantyScan.notFoundNew` ("Mã `{serial}` không tồn tại trong hệ thống"), `warrantyScan.deleted` ("Mã `{serial}` đã bị xóa khỏi hệ thống"), `warrantyScan.viewVariant` ("Xem chi tiết biến thể"), `warrantyScan.banner.inStock` ("Máy đang trong kho — chưa bán"), `warrantyScan.banner.active` ("Còn bảo hành — {count} ngày"), `warrantyScan.banner.expired` ("Hết bảo hành {count} ngày trước").
- `FrontEnd/QLBanMayTinh/src/i18n/locales/en.js` — bản tiếng Anh tương ứng.

## Phương án chi tiết

### 1. Backend — phân biệt barcode không tồn tại

**Vấn đề cốt lõi:** Hiện bảng `chi_tiet_san_pham` không có cột `da_xoa` (xóa mềm) — tôi sẽ kiểm tra trước khi sửa:
- Nếu chưa có cột xóa mềm → thêm vào entity `ChiTietSanPham.java` (`@Column(name="da_xoa") Boolean daXoa = false`) + file SQL tương ứng trong `Database/`. Tất cả query hiện tại phải thêm `WHERE da_xoa = false` để giữ nguyên hành vi.
- Nếu đã có → dùng trực tiếp.

**Logic truy vấn mới trong `traCuuSerial`:**

```java
public WarrantyLookupResponse traCuuSerial(String soSerial) {
    // Bước 1: tìm serial chưa xóa
    List<ChiTietSanPham> active = chiTietSanPhamRepository
        .findActiveByBarcodeOrSoSerial(soSerial, soSerial);
    
    if (!active.isEmpty()) {
        return buildLookupResponse(active.get(0));  // giữ logic cũ
    }
    
    // Bước 2: tìm serial đã xóa mềm để phân biệt
    boolean existedDeleted = chiTietSanPhamRepository
        .existsDeletedByBarcodeOrSoSerial(soSerial, soSerial);
    
    if (existedDeleted) {
        throw new SerialDeletedException("Mã " + soSerial + " đã bị xóa khỏi hệ thống");
    }
    
    throw new EntityNotFoundException("Mã " + soSerial + " không tồn tại trong hệ thống");
}
```

**`GlobalExceptionHandler` thêm 2 handler mới:**

```java
@ExceptionHandler(SerialDeletedException.class)
public ResponseEntity<?> handlerSerialDeleted(SerialDeletedException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(Map.of("code", "DELETED", "message", e.getMessage()));
}

@ExceptionHandler(EntityNotFoundException.class)  // đã có, sửa lại
public ResponseEntity<?> handlerEntityNotFound(EntityNotFoundException e) {
    // Detect message prefix để set code=NOT_FOUND
    String msg = e.getMessage();
    if (msg != null && msg.startsWith("Mã ") && msg.contains("không tồn tại")) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("code", "NOT_FOUND", "message", msg));
    }
    // fallback cũ
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(Map.of("code", "NOT_FOUND", "message", "Dữ liệu không hợp lệ hoặc liên kết không tồn tại"));
}
```

### 2. Frontend — bắt lỗi 404 với code

**`PhieuBaoHanhService.js`** sửa `lookupBySerial` để throw Error có chứa `code`:

```js
export const lookupBySerial = async (soSerial) => {
  const r = await fetch(`/api/phieu-bao-hanh/tra-cuu-serial?soSerial=${encodeURIComponent(soSerial)}`, { headers: authHeaders() });
  if (r.status === 404) {
    const body = await r.json().catch(() => ({}));
    const err = new Error(body.message || 'Not found');
    err.code = body.code || 'NOT_FOUND';
    err.status = 404;
    throw err;
  }
  if (!r.ok) throw new Error(`HTTP ${r.status}`);
  return r.json();
};
```

**`WarrantyPanel.vue`** sửa `lookupSerial()`:

```js
catch (e) {
  if (e.status === 404) {
    lookupErrorCode.value = e.code === 'DELETED' ? 'deleted' : 'notFound';
    lookupError.value = t(`admin.warrantyScan.${lookupErrorCode.value}`, { serial });
  } else {
    lookupErrorCode.value = 'error';
    lookupError.value = t('admin.warrantyScan.error');
  }
  lookupResult.value = null;
}
```

### 3. Frontend — Layout mới

**Cấu trúc card kết quả tra cứu:**

```
┌────────────────────────────────────────────────────────────┐
│  [Banner trạng thái - full width]                          │
│  • Còn BH (xanh) | Hết BH (đỏ) | Trong kho (xám)         │
├────────────────────────────────────────────────────────────┤
│  ┌──────────┐  Acer Aspire 5 A515-58                        │
│  │          │  SKU: ACER-A515-I7-16G-SLV                   │
│  │  Ảnh     │  Serial: #N24C210006                          │
│  │  180x180 │  Giá: 16.990.000 đ                           │
│  └──────────┘                                              │
├────────────────────────────────────────────────────────────┤
│  ┌─ Thông tin máy ──────┐  ┌─ Cấu hình chi tiết ────────┐ │
│  │ Ngày mua: 12/03/2024 │  │ CPU: Intel Core i7-13620H │ │
│  │ Hết hạn BH: 12/03/25 │  │ RAM: 16GB DDR5           │ │
│  │ Khách: Nguyễn Văn A  │  │ Ổ cứng: 512GB SSD        │ │
│  │ SĐT: 0901234567      │  │ GPU: Intel Iris Xe        │ │
│  │ Mã ĐH: #DH0123       │  │ Màn: 15.6" FHD 60Hz      │ │
│  └──────────────────────┘  │ HĐH: Windows 11 Home     │ │
│                            │ Màu: Bạc                 │ │
│                            └───────────────────────────┘ │
├────────────────────────────────────────────────────────────┤
│  Lịch sử bảo hành (cards ngang) — giữ nguyên               │
├────────────────────────────────────────────────────────────┤
│  [Tạo phiếu BH (chỉ khi đã bán)]  [Xem chi tiết BT - khi chưa bán]  [Xóa tra cứu] │
└────────────────────────────────────────────────────────────┘
```

**Computed `lookupBanner` thay thế `warrantyBadge`:**

```js
const lookupBanner = computed(() => {
  if (!lookupResult.value) return null;
  const r = lookupResult.value;
  if (r.trangThaiSerial !== 'da_ban') {
    return { kind: 'inStock', icon: '📦', cls: 'banner-gray',
             labelKey: 'warrantyScan.banner.inStock' };
  }
  if (!r.ngayHetBaoHanh) {
    return { kind: 'inStock', icon: '⚠️', cls: 'banner-yellow',
             labelKey: 'warrantyScan.noWarranty' };
  }
  const daysLeft = Math.ceil((new Date(r.ngayHetBaoHanh) - new Date()) / 86400000);
  if (daysLeft < 0) {
    return { kind: 'expired', icon: '❌', cls: 'banner-red',
             labelKey: 'warrantyScan.banner.expired', days: Math.abs(daysLeft) };
  }
  return { kind: 'active', icon: '✅', cls: 'banner-green',
           labelKey: 'warrantyScan.banner.active', days: daysLeft };
});
```

**Computed `canCreateClaim` giữ nguyên** — chỉ enable khi `trangThaiSerial === 'da_ban'`.

**Router-link cho "Xem chi tiết biến thể":**

```vue
<router-link v-if="isSerialNotSold" 
  :to="{ name: 'admin-san-pham-detail', params: { id: lookupResult.sanPhamId } }"
  class="alt-btn">
  <Package :size="13" /> {{ t('admin.warrantyScan.viewVariant') }}
</router-link>
```

## Xác minh

1. **Backend test (terminal):**
   ```bash
   # Barcode chưa từng tồn tại
   curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080/api/phieu-bao-hanh/tra-cuu-serial?soSerial=ZZZ_INVALID"
   # → 404, body: {"code":"NOT_FOUND","message":"Mã ZZZ_INVALID không tồn tại..."}
   
   # Barcode đã xóa mềm (sau khi chạy script xóa mềm 1 row test)
   curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080/api/phieu-bao-hanh/tra-cuu-serial?soSerial=8930000000041_DELETED"
   # → 404, body: {"code":"DELETED","message":"Mã ... đã bị xóa..."}
   
   # Barcode hợp lệ
   curl -s "http://localhost:8080/api/phieu-bao-hanh/tra-cuu-serial?soSerial=8930000000041"
   # → 200, response đầy đủ
   ```

2. **Frontend manual test:**
   - Vào tab Bảo hành → quét barcode hợp lệ → banner xanh "Còn BH — X ngày", ảnh 180px, 2 cột thông tin, nút "Tạo phiếu BH" enable.
   - Quét barcode chưa bán → banner xám "Máy đang trong kho — chưa bán", nút "Tạo phiếu BH" disabled, hiện nút "Xem chi tiết biến thể".
   - Quét barcode cũ đã xóa → thông báo "Mã ... đã bị xóa khỏi hệ thống" (màu cam).
   - Quét barcode không có trong DB → thông báo "Mã ... không tồn tại trong hệ thống" (màu đỏ).

3. **Migration script SQL** (nếu chưa có cột `da_xoa`):
   ```sql
   ALTER TABLE chi_tiet_san_pham ADD da_xoa BIT DEFAULT 0;
   ```
   Cập nhật tất cả JPQL `@Query` hiện tại để thêm `AND c.daXoa = false` (trừ query mới tìm deleted).
