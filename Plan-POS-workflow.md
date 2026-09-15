# Plan: Cải tiến quy trình đơn hàng POS & Online (Full Stack)

## Context

Hiện tại codebase đã có sẵn **9 trạng thái đơn hàng** định nghĩa trong frontend (`FrontEnd/src/utils/orderStatus.js`) và graph chuyển trạng thái trong backend (`BackEnd/service/DonHangService.java:158-168`):

- Timeline 7 node chính: `pending → confirmed → processing → shipping → out_for_delivery → awaiting_confirmation → delivered`
- 2 node phụ: `cancelled`, `returned`
- POS tại quầy có 1 đường tắt `confirmed → delivered` (pickup ngay, xem `DonHangService.java:172`)

Hạn chế hiện tại được user xác nhận qua screenshot và câu hỏi:
1. **POS chưa hỗ trợ giao sau** — chỉ có pickup ngay (`delivered`) hoặc `delivered` từ `confirmed`. Khách muốn giao hàng tận nơi sau khi mua tại quầy phải tạo đơn online riêng.
2. **Chưa có trạng thái "Đã giao - chờ xác nhận"** trên trang khách — admin chuyển `out_for_delivery` → trạng thái "đã giao" rồi khách bấm "xác nhận" phải qua API ri (`xacNhanDaNhanHang`) — cần đồng bộ timeline.
3. **Chưa có workflow hoàn tiền nhiều bước** — `cancelled` được dùng chung cho cả "đã hủy trước khi thanh toán" và "đã hoàn tiền".
4. **POS đồng bộ với online** — người dùng muốn POS đi qua cùng 1 hệ thống trạng thái với online (chọn flow: pickup ngay hoặc delivery).

Mục tiêu: cải tiến trang khách + sửa timeline admin + mở rộng POS + thêm trạng thái refund workflow.

## Scope: Full Stack (Backend + Admin + Customer)

**Không cần migration DB** — `DonHang.trangThaiDonHang` là `String(30)`, thêm giá trị mới không ảnh hưởng data cũ.

---

## 1. Backend Changes

### 1.1. DonHangService — mở rộng graph trạng thái (1 chỗ duy nhất)

File: `BackEnd/src/main/java/com/example/backend/service/DonHangService.java:158-176`

```java
private static final Map<String, Set<String>> CHUYEN_TRANG_THAI_DON_HANG = Map.of(
    "pending",                Set.of("confirmed", "cancelled"),
    "confirmed",              Set.of("processing", "shipping", "delivered", "cancelled"),
    "processing",             Set.of("shipping", "cancelled"),
    "shipping",               Set.of("out_for_delivery", "cancelled"),
    "out_for_delivery",       Set.of("awaiting_confirmation", "cancelled"),
    "awaiting_confirmation",  Set.of("delivered", "cancelled"),  // mới: cho phép hủy khi chưa khách xác nhận
    "delivered",              Set.of("returned", "refund_pending"),  // refund_pending mới
    "refund_pending",         Set.of("refunded"),                     // mới
    "cancelled",              Set.of("refund_pending"),               // hủy → chờ hoàn tiền
    "returned",               Set.of("refund_pending", "refunded"),
    "refunded",               Set.of()
);
```

**POS + admin update**: Bỏ đường tắt `confirmed → delivered` đặc biệt ở line 172, thay bằng cách:
- POS chọn `confirmed → delivered` (pickup ngay) → tuân theo graph mới (`confirmed → delivered` giờ đã hợp lệ)
- POS chọn `confirmed → processing` (giao sau) → chạy đúng pipeline 6 node như online

### 1.2. DonHangService — method mới `yeuCauHoanTien(donHangId, lyDo)`

```java
@Transactional
public DonHang yeuCauHoanTien(Integer id, String lyDo) {
    DonHang entity = getById(id);
    String current = entity.getTrangThaiDonHang();
    if (!"cancelled".equals(current) && !"returned".equals(current)) {
        throw new IllegalStateException("Chỉ hoàn tiền được đơn đã hủy hoặc trả hàng");
    }
    entity.setTrangThaiDonHang("refund_pending");
    if (lyDo != null) entity.setGhiChu(lyDo);
    DonHang saved = donHangRepository.save(entity);
    ghiLichSu(saved, current, "refund_pending");
    sseService.notifyOrderUpdate(saved.getId());
    return saved;
}

@Transactional
public DonHang xacNhanHoanTien(Integer id) {
    DonHang entity = getById(id);
    if (!"refund_pending".equals(entity.getTrangThaiDonHang())) {
        throw new IllegalStateException("Đơn không ở trạng thái chờ hoàn tiền");
    }
    String current = entity.getTrangThaiDonHang();
    entity.setTrangThaiDonHang("refunded");
    DonHang saved = donHangRepository.save(entity);
    ghiLichSu(saved, current, "refunded");
    sseService.notifyOrderUpdate(saved.getId());
    return saved;
}
```

### 1.3. DonHangController — 2 endpoint mới

```java
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
@PostMapping("{id}/yeu-cau-hoan-tien")
public ResponseEntity<DonHang> yeuCauHoanTien(@PathVariable Integer id, @RequestBody Map<String,String> body) {
    return ResponseEntity.ok(donHangService.yeuCauHoanTien(id, body.get("lyDo")));
}

@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
@PostMapping("{id}/xac-nhan-hoan-tien")
public ResponseEntity<DonHang> xacNhanHoanTien(@PathVariable Integer id) {
    return ResponseEntity.ok(donHangService.xacNhanHoanTien(id));
}
```

### 1.4. POS delivery flow — method helper

```java
@Transactional
public DonHang posDeliveryAfter(Integer id, String trackingNote) {
    DonHang entity = getById(id);
    if (!"in_store".equals(entity.getKenhBan())) {
        throw new IllegalStateException("Chỉ áp dụng cho đơn tại quầy");
    }
    entity.setTrangThaiDonHang("processing");  // POS giao sau → vào pipeline đóng gói
    DonHang saved = donHangRepository.save(entity);
    ghiLichSu(saved, "delivered", "processing");  // ghi log điều chỉnh
    sseService.notifyOrderUpdate(saved.getId());
    return saved;
}
```

---

## 2. Frontend — OrderStatus utility

File: `FrontEnd/src/utils/orderStatus.js` (đã có sẵn cho 9 trạng thái, cập nhật 4 trạng thái mới):

```js
// Thêm vào orderStatusColor, orderStatusIcon:
//   'awaiting_confirmation' → Bike (đã có)
//   'refund_pending'  → Circle (đang chờ) — màu xám-vàng
//   'refunded'        → CheckCircle2 — màu xanh tím (đã hoàn)
//   'cancelled' có thêm màu cam nhạt để phân biệt với refund_pending
```

**Không thay đổi Timeline component shape** — vẫn dùng `LINEAR_STATUS_ORDER` 7 node (đã có sẵn ở `OrdersTable.vue:471-487`).

---

## 3. Frontend — Admin OrdersTable.vue

File: `FrontEnd/src/components/admin/OrdersTable.vue`

### 3.1. Mở rộng sidebar timeline (line 471-487)

Bước `awaiting_confirmation` và `delivered` đã có. **Thêm nút "Yêu cầu hoàn tiền" / "Xác nhận hoàn tiền"** khi đơn ở trạng thái cho phép.

Sau `LINEAR_STATUS_ORDER` thêm:
```js
// 2 trạng thái refund flow — hiển thị dưới timeline
const REFUND_NODES = [
  { id: 'refund_pending', title: t('orderStatus.refund_pending'), desc: '...' },
  { id: 'refunded',       title: t('orderStatus.refunded'),       desc: '...' },
];
```

Khi `order.trangThaiDonHang === 'cancelled' || 'returned'`: hiện panel refund riêng dưới timeline với 2 nút:
- `Yêu cầu hoàn tiền` → POST `/api/don-hang/{id}/yeu-cau-hoan-tien`
- `Xác nhận đã hoàn tiền` → POST `/api/don-hang/{id}/xac-nhan-hoan-tien` (chỉ hiện khi đang `refund_pending`)

### 3.2. Sửa `canJumpToStep` (line 507-512)

Bỏ qua nếu status là `refund_pending` / `refunded` (không cho bấm timeline chính khi đang refund).

```js
const canJumpToStep = (order, stepId) => {
  if (['cancelled', 'returned', 'refund_pending', 'refunded'].includes(order.trangThaiDonHang)) return false;
  // ...giữ nguyên logic cũ
};
```

### 3.3. Nâng cấp `OrderStatusTimeline.vue` (nếu chưa tồn tại — tạo mới)

Tham khảo component đã reference ở backup: `backup_20260904_000600/AccountPage-v2.vue.bak`. Component customer cần:
- 7 node tuyến tính (giống admin)
- Hiển thị node `current` sáng cam
- Hiển thị node `reached` có dấu check xanh
- Có 1 nhánh phụ cho `cancelled` / `returned` (hiển thị riêng thay vì chen vào timeline)

---

## 4. Frontend — Customer AccountPage.vue

File: `FrontEnd/src/pages/AccountPage.vue`

### 4.1. Mở rộng `TAB_STATUS_GROUPS` (line 89-91)

```js
const TAB_STATUS_GROUPS = {
  pending:    [...],           // thêm 'awaiting_confirmation'
  shipping:   ['shipping', 'out_for_delivery'],
  completed:  ['delivered', 'refunded'],
  cancelled:  ['cancelled', 'returned', 'refund_pending'],
};
```

Thêm badge đếm trạng thái cho tab `cancelled` (refund_pending đếm riêng, hiển thị số "đang chờ hoàn tiền" trong từng thẻ đơn).

### 4.2. Refactor component timeline cho khách

Tách `<OrderStatusTimeline>` ra component riêng ở `FrontEnd/src/components/OrderStatusTimeline.vue` (đã có reference từ backup). Props:
- `orderStatus` (String)
- `order` (full Object - để biết `ngayGiaoThucTe`)
- Loại `compact: 'full' | 'compact'` (compact cho danh sách, full cho chi tiết)

Node `awaiting_confirmation` hiển thị nhãn "Đã giao - chờ xác nhận" + nút `Tôi đã nhận được hàng` (gọi `DonHangService.xacNhanDaNhanHang` đã có sẵn).

Nếu khách bấm "Hủy đơn" trên đơn chưa giao → gọi endpoint hủy hiện có. Nếu đơn đã trả tiền → cảnh báo "Sẽ yêu cầu hoàn tiền" trước khi confirm.

---

## 5. Frontend — POS PosPanel.vue

File: `FrontEnd/src/components/admin/PosPanel.vue`

### 5.1. Đổi radio `posDeliveryMode` từ 2 thành 2-3 lựa chọn (line 96-97)

Thay `posDeliveryMode.value = 'pickup' | 'delivery'` thành:
- `'pickup'` — Khách lấy ngay tại quầy (giữ nguyên flow delivered ngay)
- `'delivery'` — Giao tận nơi (POS chuyển đơn sang trạng thái `processing`, giao hàng bình thường)
- `'installment'` (optional, có thể skip) — Trả góp

Sau khi đặt đơn POS xong:
```js
if (posDeliveryMode.value === 'pickup') {
  // ... giữ nguyên: update trạng thái delivered ngay
} else if (posDeliveryMode.value === 'delivery') {
  // Giữ trạng thái 'processing' (đã có sẵn từ initial create)
  // Hoặc: chuyển 'confirmed' → 'processing' thay vì 'delivered'
}
```

### 5.2. CSS — không thay đổi

---

## 6. Files cần sửa

| Layer | File | Thay đổi |
|-------|------|---------|
| Backend | `DonHangService.java:158-168` | Mở rộng graph trạng thái (4 trạng thái mới: `refund_pending`, `refunded` cho `cancelled`/`returned`) |
| Backend | `DonHangService.java` (thêm method) | `yeuCauHoanTien`, `xacNhanHoanTien`, `posDeliveryAfter` |
| Backend | `DonHangController.java` | 2 endpoint refund mới |
| Backend | `DonHangRepository.java` | (không đổi) |
| Frontend | `FrontEnd/src/utils/orderStatus.js` | Thêm icon/color cho 2 trạng thái mới |
| Frontend | `FrontEnd/src/components/admin/OrdersTable.vue` | Sidebar refund panel + sửa `canJumpToStep` |
| Frontend | `FrontEnd/src/components/admin/OrderStatusTimeline.vue` (tạo mới) | Tách timeline từ OrdersTable.vue ra component riêng |
| Frontend | `FrontEnd/src/components/PosPanel.vue` | Đổi radio delivery mode thành 3 lựa chọn |
| Frontend | `FrontEnd/src/pages/AccountPage.vue:89-91` | Mở rộng `TAB_STATUS_GROUPS` |
| Frontend | `FrontEnd/src/pages/AccountPage.vue` (line ~674 panel 'history') | Render `<OrderStatusTimeline>` cho từng card đơn + nút "Tôi đã nhận" |
| i18n | `FrontEnd/src/i18n/index.js` | Thêm key: `orderStatus.refund_pending`, `orderStatus.refunded`, `orderStatus.timeline.refundPendingDesc`, `orderStatus.timeline.refundedDesc`, `account.tabRefundPending` |

---

## 7. Verification

### 7.1. Backend build + test

```bash
cd BackEnd
mvn compile                    # phải pass
mvn test -q -DskipTests=false  # chạy hết 159 test, không vỡ test mới
```

### 7.2. Thêm test mới cho state machine (1 test)

`DonHangServiceTest.java` — thêm method `testChuyenTrangThaiRefund`:
- `delivered → refund_pending` hợp lệ
- `cancelled → refund_pending` hợp lệ
- `returned → refund_pending` hợp lệ
- `refund_pending → refunded` hợp lệ
- `refund_pending → cancelled` KHÔNG hợp lệ

### 7.3. Smoke test thủ công

**Admin flow**:
1. Mở admin → Đơn hàng → Sửa 1 đơn đang `out_for_delivery`
2. Click `awaiting_confirmation` → đơn chuyển trạng thái, badge xanh
3. Click `delivered` → đơn chuyển trạng thái
4. Click `Yêu cầu hoàn tiền` (panel mới dưới timeline) → refund_pending
5. Click `Xác nhận đã hoàn tiền` → refunded

**Customer flow**:
1. Khách mở `/account/history` → 4 tab
2. Tab "Đang xử lý" chứa cả đơn `awaiting_confirmation`
3. Mỗi card có button "Tôi đã nhận được hàng" khi đơn ở `awaiting_confirmation`

**POS flow**:
1. Mở POS → tạo đơn cho khách mới
2. Chọn `Giao tận nơi` (delivery)
3. Đặt đơn → đơn ở trạng thái `processing`, không phải `delivered`
4. Admin vào bảng Đơn hàng → đơn POS có thể next/prev status như đơn online

### 7.4. Database verify

```sql
SELECT trang_thai_don_hang, COUNT(*) FROM don_hang GROUP BY trang_thai_don_hang;
-- Đảm bảo không có giá trị NULL/mới không mong muốn sau migration.
```

---

## 8. Out of scope (làm sau nếu cần)

- **Payment gateway integration** — `refund_pending` và `refunded` hiện chỉ là trạng thái hiển thị, chưa tích hợp VNPay/QR hoàn tiền tự động. Sau khi có API mới thì hook vào method `xacNhanHoanTien`.
- **Push notification** — webhook hoặc email khi trạng thái đổi sang `refund_pending` / `refunded`.
- **Trả góp (installment)** — POS radio "Trả góp" có thể làm ở sprint riêng, cần tích hợp bên thứ 3 (FE Credit, etc.).
- **Auto-cancel refund** — nếu `refund_pending` quá 7 ngày → tự động `cancelled` hoặc escalation.
