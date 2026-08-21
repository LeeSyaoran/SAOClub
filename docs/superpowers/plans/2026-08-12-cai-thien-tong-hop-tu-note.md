# Cải thiện tổng hợp từ "Note những thứ cần cải thiện" — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix toàn bộ 22 mục được note trong `Note những thứ cần cải thiện.docx`, chia theo 6 khu vực admin (Bán hàng tại quầy, Đơn hàng, Sản phẩm, Biến thể, Khách hàng, Kho hàng).

**Architecture:** Đây là 6 giai đoạn **độc lập** — mỗi giai đoạn chỉ đụng file của khu vực đó, không phụ thuộc giai đoạn khác (ngoại lệ: Task 1.8 và Task 6.1 cùng sửa một trigger SQL, xem ghi chú tại 6.1). Có thể làm và deploy từng giai đoạn riêng lẻ, theo thứ tự bất kỳ. Không có spec/design doc riêng — nội dung note của người dùng đủ cụ thể để làm spec trực tiếp; các điểm mơ hồ được ghi rõ là "GIẢ ĐỊNH" ngay tại task liên quan, cần xác nhận lại trước khi build phần đó.

**Tech Stack:** Backend Spring Boot (Java) tại `BackEnd/`; Frontend Vue 3 `<script setup>` (không TS) tại `FrontEnd/QLBanMayTinh/`, Bootstrap 5 + `@lucide/vue` icon, `vitest` cho test frontend, JUnit cho test backend; SQL Server, script gốc `Database/QLBanMayTinh.sql`.

## Global Constraints

- **File SQL luôn chạy lại toàn bộ** — mọi thay đổi schema/trigger trong `Database/QLBanMayTinh.sql` phải idempotent (dùng `IF OBJECT_ID(...) IS NOT NULL DROP ...` / `CREATE OR ALTER` như style đã có trong file), không viết snippet SQL rời.
- Frontend không dùng Pinia thật cho domain data — các "store" (`stores/products.js`, `stores/orders.js`, ...) là `reactive({ items, loading, loaded })` tự chế với `ensureX()`/`refreshX()`. Task nào cần load data mới phải theo đúng pattern này, không import `defineStore`.
- Gọi API qua `src/services/api.js` (`get/post/put/patch/del`, tự gắn JWT). Mỗi entity có 1 file `src/services/<Entity>Service.js`. Không dùng axios.
- Màu trạng thái serial (`chi_tiet_san_pham.trang_thai`) và đơn hàng (`don_hang.trang_thai_don_hang`) đã chốt cứng theo bảng màu đã lưu — không đổi màu khi sửa các badge/dot ở khu vực Kho hàng/Đơn hàng.
- Backend không xóa cứng Khách hàng/Nhân viên/Khuyến mãi/Biến thể/Trả hàng/Bảo hành/Nhà cung cấp/CPU/GPU/RAM/Ổ cứng (đã bỏ ở commit `ba0e963`) — không hồi sinh nút Xóa cho các entity này khi sửa UI liên quan.
- Không có test component nào cho các file `.vue` bị đụng trong plan này (`PosPanel`, `OrdersTable`, `ProductsTable`, `BienTheTable`, `CustomersTable`, `InventoryPanel`, `WarrantyPanel`, `DanhGiaPanel`). Các task thuần UI dưới đây dùng bước **"Kiểm tra thủ công"** thay vì viết test giả — không bịa ra test tự động cho phần chưa từng có hạ tầng test.

---

## GIAI ĐOẠN 1 — Bán hàng tại quầy (`PosPanel.vue`)

### Task 1.1: Bỏ hiển thị mã SKU và danh mục sản phẩm

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/PosPanel.vue:587-588` (card sản phẩm), `:660`, `:690` (dòng giỏ hàng)

- [ ] Xóa dòng hiển thị `{{ p.maSku }}` (`:587`) và `{{ p.tenThuongHieu }} · {{ p.tenDanhMuc }}` (`:588`) khỏi card sản phẩm; xóa các chỗ lặp lại SKU ở dòng giỏ hàng (`:660`, `:690`).
- [ ] **Kiểm tra thủ công:** mở tab Bán hàng tại quầy, xác nhận card sản phẩm và dòng giỏ hàng không còn SKU/danh mục, tên sản phẩm + giá vẫn hiển thị đúng.
- [ ] Commit.

### Task 1.2: Tìm kiếm không phân biệt dấu (bỏ dấu tìm)

**GIẢ ĐỊNH:** Note ghi "bỏ dấu tìm, để nó tự load" — đã grep toàn bộ code, không có nút/toggle tìm kiếm nào để bỏ, và kết quả đã tự lọc real-time (`posSearch` là computed, không cần bấm nút). Diễn giải: "bỏ dấu" = tìm không cần gõ dấu tiếng Việt (ví dụ gõ "may tinh" vẫn ra "máy tính"), vì hiện tại `posSearch` so khớp chuỗi có dấu nguyên văn. Nếu ý người viết note khác, cần làm rõ lại trước khi build.

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/PosPanel.vue:28,40-49`
- Create: `FrontEnd/QLBanMayTinh/src/utils/removeAccents.js`

- [ ] Tạo `removeAccents.js`:
```javascript
export function removeAccents(str) {
  return (str ?? '')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D');
}
```
- [ ] Sửa `posSearch` filter (`PosPanel.vue:40-49`) để so khớp qua `removeAccents(...).toLowerCase()` ở cả query lẫn `tenSanPham`/`maSku`, thay vì `.toLowerCase()` trực tiếp.
- [ ] Test: `FrontEnd/QLBanMayTinh/src/__tests__/utils/removeAccents.test.ts`
```typescript
import { describe, it, expect } from 'vitest';
import { removeAccents } from '../../utils/removeAccents';

describe('removeAccents', () => {
  it('strips Vietnamese diacritics', () => {
    expect(removeAccents('Máy tính')).toBe('May tinh');
  });
  it('handles đ/Đ specially', () => {
    expect(removeAccents('Đĩa cứng')).toBe('Dia cung');
  });
  it('passes through plain ascii', () => {
    expect(removeAccents('SSD 512GB')).toBe('SSD 512GB');
  });
});
```
- [ ] Run: `cd FrontEnd/QLBanMayTinh && npx vitest run src/__tests__/utils/removeAccents.test.ts` — expect PASS.
- [ ] Commit.

### Task 1.3: Chọn nhiều serial cùng lúc

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/PosPanel.vue:310` (`posOpenSerialPicker`), `:343` (`posSelectSerial`)

- [ ] Đổi modal chọn serial: thay click-để-thêm-1 bằng checkbox mỗi dòng serial + state `posSelectedSerials` (array) + nút "Thêm N serial đã chọn" thêm tất cả cùng lúc vào giỏ hàng (gọi lại logic hiện có của `posSelectSerial` theo vòng lặp, giữ nguyên validate `trong_kho`).
- [ ] **Kiểm tra thủ công:** mở picker serial của 1 biến thể còn ≥ 3 serial, tick 2-3 serial, bấm thêm, xác nhận cả 2-3 dòng lên giỏ hàng đúng, không trùng serial đã `giu_hang`.
- [ ] Commit.

### Task 1.4: Tính phí vận chuyển thật

**GIẢ ĐỊNH cần xác nhận:** `posFee` hiện tại (`PosPanel.vue:90`) là copy nguyên công thức miễn phí ship của checkout online (`>= 300k thì free, else 30k`), áp cho cả đơn tại quầy — vô lý vì khách đứng tại quầy không cần ship. Đề xuất: mặc định phí ship tại quầy = 0; chỉ tính phí khi nhân viên chủ động tick "Giao hàng cho khách" (trường hợp khách mua tại quầy nhưng muốn giao đến địa chỉ khác) — lúc đó tái dùng công thức `phiVanChuyen` đã có ở `CheckoutModal.vue`.

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/PosPanel.vue:90` (`posFee`), thêm state `posIsDelivery` (checkbox mới trong khu vực thanh toán, cạnh phương thức thanh toán)

- [ ] Thêm checkbox "Giao hàng" (`posIsDelivery`, mặc định `false`). Khi `false`: `posFee = 0` luôn. Khi `true`: `posFee` dùng lại công thức ngưỡng miễn phí hiện có, và hiện ô nhập địa chỉ giao (nếu chưa có).
- [ ] **Kiểm tra thủ công:** đơn không tick "Giao hàng" → phí ship luôn 0 bất kể tổng tiền; tick "Giao hàng" với đơn < 300k → phí 30k, ≥ 300k → miễn phí.
- [ ] Commit.

### Task 1.5: Nút chọn voucher mở tab mới

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/PosPanel.vue:114-129` (khu vực `posPromoSuggestions`, render `:714-724`)
- Create: `FrontEnd/QLBanMayTinh/src/pages/VoucherPickerPage.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/router/index.js` — thêm route `/voucher-picker` (role admin, không nằm trong `AdminPage.vue` tab-router vì cần mở ở tab trình duyệt riêng)

- [ ] Tạo `VoucherPickerPage.vue`: danh sách voucher khả dụng (tái dùng data từ `stores/promotions.js`), mỗi dòng có nút "Chọn" gọi `window.opener?.postMessage({ type: 'voucher-selected', code: v.maPhieu }, window.location.origin)` rồi `window.close()`.
- [ ] Ở `PosPanel.vue`, thêm nút "Chọn voucher (tab mới)" cạnh ô nhập mã, `@click` gọi `window.open('/#/voucher-picker', '_blank')`; đăng ký `window.addEventListener('message', ...)` trong `onMounted` (gỡ ở `onUnmounted`) để nhận `voucher-selected` và tự điền vào `posPromoCode` + áp dụng.
- [ ] **Kiểm tra thủ công:** bấm nút, tab mới mở đúng danh sách voucher, chọn 1 mã, tab đóng, tab gốc tự điền + áp mã đúng số tiền giảm.
- [ ] Commit.

### Task 1.6 + 1.7: QR chuyển khoản (giả lập quét) + bỏ mã QR thừa

**GIẢ ĐỊNH quan trọng:** Hai mục note gộp lại vì cùng 1 vấn đề nhưng nằm ở 2 nơi khác nhau trong code:
- `PosPanel.vue` (admin, tại quầy) hiện **không có QR nào** cho "chuyển khoản" — cần thêm mới.
- `CheckoutModal.vue` (khách tự đặt online) đang có **2 lựa chọn thanh toán trùng nhau**: "QR Code" (hiện ảnh VietQR) và "Chuyển khoản" (hiện y hệt thông tin tài khoản nhưng không có ảnh QR) — đây chính là "mã QR thừa".

Đề xuất xử lý thống nhất: gộp "QR Code" vào trong "Chuyển khoản" ở cả 2 nơi (chuyển khoản luôn kèm hiện QR), bỏ lựa chọn "QR Code" đứng riêng.

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/checkout/CheckoutModal.vue:263-285` (xóa hẳn khối `<label>` "QR Code"), `:314-335` (gộp khối hiện ảnh QR vào trong khối `v-if="selectedPayment === 'chuyen_khoan'"` ở `:338-349` thay vì điều kiện riêng `=== 'qr'`), `:35-40` (bỏ nhánh `v-else-if="selectedPayment === 'qr'"` ở màn hình thành công, vì `qr` không còn tồn tại), `:433` (bỏ `'qr'` khỏi comment kiểu dữ liệu `selectedPayment`)
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/PosPanel.vue:738-751` (khu vực nút phương thức thanh toán) — thêm hiện QR khi chọn `chuyen_khoan`, dùng lại đúng công thức tạo link ảnh VietQR đã có ở `CheckoutModal.vue:437-442`
- Modify: `FrontEnd/QLBanMayTinh/src/utils/orderStatus.js` — không đổi danh sách `POS_PAYMENT_METHODS`, giữ nguyên `chuyen_khoan`

- [ ] `CheckoutModal.vue`: xóa khối `<label>` "QR Code" (`:263-285`); đổi điều kiện hiện ảnh QR (`:315`) từ `selectedPayment === 'qr'` thành `selectedPayment === 'chuyen_khoan'`, đặt khối đó lên trước khối "Thông tin chuyển khoản thủ công" hiện tại (giữ cả ảnh QR + thông tin số tài khoản trong cùng 1 khối `chuyen_khoan`); xóa nhánh `v-else-if="selectedPayment === 'qr'"` ở màn hình thành công (`:35-40`), gộp instruction vào nhánh `else` (bank instruction) hiện có.
- [ ] `PosPanel.vue`: khi `posPaymentMethod === 'chuyen_khoan'`, hiện ảnh QR (tái dùng computed giống `qrImageUrl` của `CheckoutModal.vue`, đổi `amount` = `posCartTotal`) + nút "Xác nhận đã quét/chuyển khoản" (`posConfirmBankTransfer`, chỉ set 1 flag local `posBankConfirmed = true` để cho phép bấm "Hoàn tất đơn" — đây là "quét giả lập" vì không có webhook ngân hàng thật).
- [ ] **Kiểm tra thủ công:** Checkout khách hàng chỉ còn 2 lựa chọn (Tiền mặt, Chuyển khoản-có-QR); POS admin chọn "Chuyển khoản" hiện QR + nút xác nhận, không xác nhận thì không hoàn tất được đơn.
- [ ] Commit.

### Task 1.8: Logic ưu tiên bán hàng tại quầy (tồn "giữ" ảo)

**Root cause đã xác định:** Khóa serial khi giữ hàng (`ChiTietSanPhamService.update()`, khóa `findByIdForUpdate`) đã đúng — không có race condition ở mức serial. Vấn đề thật là `ton_kho.so_luong_giu` là **cột nhập tay** (`InventoryPanel.vue:1115`, lưu qua `TonKhoService.update()`), không tự đếm lại từ số serial đang `trang_thai='giu_hang'`, nên khi POS giữ 1 serial, số "giữ" nhân viên thấy ở tab Kho không nhích theo thực tế → khách online có thể vẫn thấy hàng "còn" và đặt vượt số thực bán được.

**Files:**
- Modify: `Database/QLBanMayTinh.sql:637-686` (mở rộng trigger `trg_CapNhatTonKhoThucTe` có sẵn, không tạo trigger mới)
- Modify: `BackEnd/src/main/java/com/example/backend/service/TonKhoService.java:39-45` (`update()`)
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/InventoryPanel.vue:1115` (input `soLuongGiu`)

- [ ] Sửa trigger `trg_CapNhatTonKhoThucTe`: thêm tính `bien_dong_giu` song song với `bien_dong` hiện có, dựa trên `trang_thai = N'giu_hang'` (thay vì `N'trong_kho'`), rồi `UPDATE ton_kho SET so_luong_giu = so_luong_giu + t.bien_dong_giu` cùng lúc với `so_luong_ton_thuc_te`:
```sql
IF OBJECT_ID('trg_CapNhatTonKhoThucTe', 'TR') IS NOT NULL
    DROP TRIGGER trg_CapNhatTonKhoThucTe;
GO

CREATE TRIGGER trg_CapNhatTonKhoThucTe
ON chi_tiet_san_pham
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @TmpTable TABLE (bien_the_id INT, bien_dong INT, bien_dong_giu INT);

    IF EXISTS (SELECT 1 FROM inserted) AND NOT EXISTS (SELECT 1 FROM deleted)
    BEGIN
        INSERT INTO @TmpTable
        SELECT bien_the_id,
               SUM(CASE WHEN trang_thai = N'trong_kho' THEN 1 ELSE 0 END),
               SUM(CASE WHEN trang_thai = N'giu_hang' THEN 1 ELSE 0 END)
        FROM inserted GROUP BY bien_the_id;
    END

    IF EXISTS (SELECT 1 FROM deleted) AND EXISTS (SELECT 1 FROM inserted)
    BEGIN
        INSERT INTO @TmpTable
        SELECT d.bien_the_id,
               -SUM(CASE WHEN d.trang_thai = N'trong_kho' AND i.trang_thai <> N'trong_kho' THEN 1 ELSE 0 END)
                + SUM(CASE WHEN d.trang_thai <> N'trong_kho' AND i.trang_thai = N'trong_kho' THEN 1 ELSE 0 END),
               -SUM(CASE WHEN d.trang_thai = N'giu_hang' AND i.trang_thai <> N'giu_hang' THEN 1 ELSE 0 END)
                + SUM(CASE WHEN d.trang_thai <> N'giu_hang' AND i.trang_thai = N'giu_hang' THEN 1 ELSE 0 END)
        FROM deleted d JOIN inserted i ON d.chi_tiet_id = i.chi_tiet_id
        GROUP BY d.bien_the_id;
    END

    IF EXISTS (SELECT 1 FROM deleted) AND NOT EXISTS (SELECT 1 FROM inserted)
    BEGIN
        INSERT INTO @TmpTable
        SELECT bien_the_id,
               -SUM(CASE WHEN trang_thai = N'trong_kho' THEN 1 ELSE 0 END),
               -SUM(CASE WHEN trang_thai = N'giu_hang' THEN 1 ELSE 0 END)
        FROM deleted GROUP BY bien_the_id;
    END

    IF EXISTS (SELECT 1 FROM @TmpTable)
    BEGIN
        UPDATE tk
        SET tk.so_luong_ton_thuc_te = tk.so_luong_ton_thuc_te + t.bien_dong,
            tk.so_luong_giu         = tk.so_luong_giu + t.bien_dong_giu,
            tk.ngay_cap_nhat        = GETDATE()
        FROM ton_kho tk
        JOIN (SELECT bien_the_id, SUM(bien_dong) bien_dong, SUM(bien_dong_giu) bien_dong_giu
              FROM @TmpTable GROUP BY bien_the_id) t
            ON tk.bien_the_id = t.bien_the_id;
    END
END;
GO
```
- [ ] `TonKhoService.update()`: bỏ nhận `soLuongGiu` từ request body (chỉ còn cho sửa `tonKhoToiThieu`); `so_luong_giu` từ nay chỉ do trigger ghi.
- [ ] `InventoryPanel.vue:1115`: đổi input `soLuongGiu` thành hiển thị read-only (giống cách `soLuongTon` đã hiển thị read-only cạnh đó), bỏ `v-model` 2 chiều.
- [ ] Test: `BackEnd/src/test/java/com/example/backend/service/TonKhoServiceTest.java` (file mới — chưa có test cho service này)
```java
package com.example.backend.service;

import com.example.backend.entity.TonKho;
import com.example.backend.repository.TonKhoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TonKhoServiceTest {

    @Mock private TonKhoRepository tonKhoRepository;
    @InjectMocks private TonKhoService tonKhoService;

    @Test
    void update_ignoresClientSentSoLuongGiu_onlyUpdatesTonKhoToiThieu() {
        TonKho existing = new TonKho();
        existing.setBienTheId(1);
        existing.setSoLuongGiu(3);
        existing.setTonKhoToiThieu(5);
        when(tonKhoRepository.findById(1)).thenReturn(Optional.of(existing));
        when(tonKhoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TonKho request = new TonKho();
        request.setSoLuongGiu(999); // client cố gửi giá trị giữ khác — phải bị bỏ qua
        request.setTonKhoToiThieu(10);

        TonKho result = tonKhoService.update(1, request);

        assertThat(result.getSoLuongGiu()).isEqualTo(3); // không đổi, vẫn do trigger quản lý
        assertThat(result.getTonKhoToiThieu()).isEqualTo(10);
    }
}
```
- [ ] Run: `cd BackEnd && ./mvnw test -Dtest=TonKhoServiceTest` — expect PASS (sau khi sửa `update()` bỏ set `soLuongGiu` từ request).
- [ ] Chạy lại toàn bộ `Database/QLBanMayTinh.sql` trên DB dev, xác nhận trigger tạo lại không lỗi và `so_luong_giu` các dòng có sẵn không bị âm (`CK_chtk_giu_le_ton` constraint đã có sẵn để chặn việc này).
- [ ] Commit.
- [ ] `ponytail:` đơn giữ qua "Held orders" của POS (`posHoldOrder`, `PosPanel.vue:164-191`) chỉ lưu ở `localStorage`, không có hạn — nếu mất tab/máy thì serial `giu_hang` bị khóa vĩnh viễn cho tới khi nhân viên tự tay hủy giữ. Không xử lý TTL/cleanup job trong plan này; thêm khi có báo cáo tồn kho bị "giữ" tồn đọng thật.

---

## GIAI ĐOẠN 2 — Đơn hàng (`OrdersTable.vue`)

### Task 2.1: Hiển thị hóa đơn

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/OrdersTable.vue` — thêm nút "In hóa đơn" trong modal chi tiết đơn (cạnh nơi có nút xóa dòng, `:877` khu vực)
- Reference pattern có sẵn: `FrontEnd/QLBanMayTinh/src/components/admin/InventoryPanel.vue:502-560` (`printPhieuNhapList`/`printPhieuNhapDetail` — mở cửa sổ in riêng, không `window.print()` cả khung admin)

- [ ] Viết `printOrderInvoice(order)` trong `OrdersTable.vue`, theo đúng khuôn `printPhieuNhapDetail`: mở `window.open('', '_blank')`, ghi HTML hóa đơn (mã đơn, ngày, khách hàng, danh sách sản phẩm + số lượng + đơn giá, tổng tiền, phí ship, giảm giá, phương thức thanh toán), gọi `.print()`.
- [ ] Thêm nút "In hóa đơn" gọi `printOrderInvoice(orderDetailData)` trong modal chi tiết đơn.
- [ ] **Kiểm tra thủ công (đọc code, KHÔNG bấm In thật — theo quy tắc đã lưu, in/print dialog treo trình duyệt khi test tự động):** đọc lại HTML sinh ra, xác nhận đủ trường mã đơn/khách hàng/sản phẩm/tổng tiền trước khi coi task xong.
- [ ] Commit.

### Task 2.2: Đổi nút "Hủy/Sửa" thành "Hủy đơn"

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/OrdersTable.vue:659` (nút hành động theo dòng), `:1022-1050` (modal trạng thái, giữ lại nhưng chỉ mở từ trong màn chi tiết đơn, không phải nút chính theo dòng)

- [ ] Đổi nút chính mỗi dòng đơn từ `t('admin.orders.update')` (mở modal sửa mọi trường) thành nút "Hủy đơn" — click gọi hàm mới `cancelOrder(o)`: hiện confirm dialog, nếu đồng ý thì gọi `DonHangService.update(o.donHangId, { trangThaiDonHang: 'cancelled' })` rồi refresh danh sách.
- [ ] Modal trạng thái đầy đủ (đổi mã vận đơn, trạng thái thanh toán, ngày giao...) chuyển vào bên trong modal **chi tiết đơn** (mở qua nút "Xem chi tiết" đã có), không còn là hành động hàng-đầu-tiên ngoài bảng — nhân viên vẫn cập nhật được trạng thái giao hàng bình thường, chỉ không còn lẫn với thao tác hủy nhanh.
- [ ] Thêm i18n key mới `admin.orders.cancel` (vi: "Hủy đơn", en: "Cancel order") ở `src/i18n/locales/{vi,en}.js`, dùng thay cho `admin.orders.update` ở nút hàng.
- [ ] **Kiểm tra thủ công:** bấm "Hủy đơn" trên 1 đơn test → confirm → trạng thái chuyển `cancelled`, danh sách cập nhật; mở "Xem chi tiết" vẫn còn sửa được các trường khác như cũ.
- [ ] Commit.

### Task 2.3: Hiển thị thông tin khách hàng đầy đủ trong chi tiết đơn

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/OrdersTable.vue:821` (header modal chi tiết)

- [ ] Cạnh dòng `customerName(orderDetailData?.khachHangId)` hiện có, thêm hiển thị `sdtNguoiNhan`/`diaChiGiaoHangText` đã có sẵn trên chính `DonHang` (không cần gọi API thêm), và `email` lấy từ `KhachHang` đã load sẵn trong `stores/customers.js` (join qua `khachHangId`, cùng cách `customerName()` đang làm ở `:26-27`).
- [ ] **Kiểm tra thủ công:** mở chi tiết 1 đơn, xác nhận thấy đủ họ tên, SĐT, email, địa chỉ giao.
- [ ] Commit.

### Task 2.4: Bỏ nút thêm/xóa sản phẩm trong đơn đã tạo

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/OrdersTable.vue:279` (`removeItemFromOrder`), `:658` (`showAddItemDetailModal`), `:877-884` (nút xóa dòng), `:946-996` (khối thêm sản phẩm trong modal chi tiết)

- [ ] Xóa nút xóa dòng sản phẩm (`:877-884`) và toàn bộ khối UI "MODAL THÊM SẢN PHẨM CHI TIẾT" (`:946-996`) cùng hàm `removeItemFromOrder`, biến `showAddItemDetailModal` và các hàm chỉ phục vụ khối này.
- [ ] **Không xóa** endpoint backend `ChiTietDonHangController` add/delete — kiểm tra trước khi xóa hoàn toàn phía frontend xem `ChiTietDonHangService.js` còn được gọi ở nơi khác (VD: tạo đơn từ `PosPanel.vue`/`CheckoutModal.vue`) hay không; nếu còn, chỉ xóa lời gọi trong `OrdersTable.vue`, giữ nguyên service file + backend (theo đúng pattern đã áp dụng ở commit `ba0e963`/`60dab21`).
- [ ] **Kiểm tra thủ công:** mở chi tiết 1 đơn đã tạo, xác nhận không còn nút thêm/xóa sản phẩm trong danh sách dòng hàng.
- [ ] Commit.

---

## GIAI ĐOẠN 3 — Sản phẩm

### Task 3.1: Chi tiết sản phẩm → trang riêng (tab mới) với 3 tab con

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/ProductsTable.vue:459` (`openDetail`)
- Create: `FrontEnd/QLBanMayTinh/src/pages/ProductDetailPage.vue` (thay thế cách dùng `ProductDetailModal.vue` cho luồng admin; giữ `ProductDetailModal.vue` nếu còn chỗ khác dùng modal)
- Modify: `FrontEnd/QLBanMayTinh/src/router/index.js` — thêm route `/product/:id` (role admin)
- Backend — Create: bảng `lich_su_san_pham` trong `Database/QLBanMayTinh.sql` (theo đúng pattern `lich_su_ton_kho`/`lich_su_don_hang` đã có), entity `LichSuSanPham.java`, `LichSuSanPhamRepository.java`, `LichSuSanPhamService.java`, `LichSuSanPhamController.java` (`GET /api/lich-su-san-pham/san-pham/{sanPhamId}`)
- Modify: `BackEnd/src/main/java/com/example/backend/service/ChiTietSanPhamService.java` và service sửa `BienTheSanPham` — ghi 1 dòng vào `lich_su_san_pham` mỗi lần `update()` sản phẩm/biến thể thành công

- [ ] SQL — thêm bảng (idempotent, theo style file hiện có):
```sql
IF OBJECT_ID('lich_su_san_pham', 'U') IS NULL
BEGIN
    CREATE TABLE lich_su_san_pham (
        lich_su_id     INT           IDENTITY(1,1) PRIMARY KEY,
        san_pham_id    INT           NOT NULL,
        bien_the_id    INT           NULL,
        loai_thay_doi  NVARCHAR(30)  NOT NULL
            CONSTRAINT CK_lssp_loai CHECK (loai_thay_doi IN (N'san_pham', N'bien_the')),
        noi_dung       NVARCHAR(500) NOT NULL,
        nguoi_thuc_hien_id INT       NULL,
        thoi_gian      DATETIME      NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_lssp_san_pham FOREIGN KEY (san_pham_id) REFERENCES san_pham(san_pham_id) ON DELETE CASCADE
    );
END
GO
```
- [ ] Backend: entity/repo/service/controller mới cho `lich_su_san_pham` (CRUD chỉ cần `create` nội bộ + `GET list by sanPhamId`, không cần update/delete).
- [ ] Trong `ChiTietSanPhamService.update()` (sửa biến thể) và service sửa `SanPham`, sau khi save thành công, gọi `lichSuSanPhamService.log(sanPhamId, bienTheId, loai, "Cập nhật ...")` mô tả ngắn field nào đổi.
- [ ] Frontend: `ProductDetailPage.vue` — 3 tab (`thông tin`/`biến thể`/`lịch sử`) dùng state `activeTab` local, tab 1 tái dùng nội dung hiện có của `ProductDetailModal.vue`, tab 2 là bảng biến thể lọc theo `sanPhamId` (tái dùng cách `BienTheTable.vue` render hàng), tab 3 gọi API list mới, hiện timeline `thoi_gian` + `noi_dung`.
- [ ] `ProductsTable.vue:459` đổi `openDetail` từ mở modal sang `window.open('/#/product/' + p.sanPhamId, '_blank')`.
- [ ] Test backend: `BackEnd/src/test/java/com/example/backend/service/LichSuSanPhamServiceTest.java` — test `log()` tạo đúng 1 bản ghi với đúng `sanPhamId`/`loaiThayDoi`.
- [ ] Run: `cd BackEnd && ./mvnw test -Dtest=LichSuSanPhamServiceTest` — expect PASS.
- [ ] **Kiểm tra thủ công:** mở chi tiết 1 sản phẩm → tab mới mở đúng trang, 3 tab chuyển được; sửa 1 biến thể → quay lại tab lịch sử thấy dòng mới.
- [ ] Commit.

### Task 3.2: Bỏ khả năng xóa danh mục sản phẩm

**Files:**
- Modify: `BackEnd/src/main/java/com/example/backend/controller/DanhMucController.java:46-51`
- Modify: `BackEnd/src/main/java/com/example/backend/service/DanhMucService.java` (method `delete`)

- [ ] Xóa endpoint `DELETE /api/danh-muc/delete/{id}` khỏi `DanhMucController.java` và method `delete()` khỏi `DanhMucService.java` (frontend không có nút xóa danh mục nào để gỡ — đã xác nhận `DanhMuc` chỉ xuất hiện dưới dạng `<select>` trong form sản phẩm, không phải bảng CRUD riêng).
- [ ] Test: chạy lại `BackEnd/src/test/java/com/example/backend/` các test liên quan `DanhMuc` (nếu có) để xác nhận không có test nào còn gọi `.delete()`.
- [ ] Commit.

---

## GIAI ĐOẠN 4 — Biến thể (`BienTheTable.vue`)

### Task 4.1: Tinh chỉnh phần ảnh — hỗ trợ nhiều ảnh (gallery)

**Files:**
- Modify: `Database/QLBanMayTinh.sql` — thêm bảng `bien_the_hinh_anh`
- Create: `BienTheHinhAnh.java`, `BienTheHinhAnhRepository.java`, `BienTheHinhAnhService.java`, `BienTheHinhAnhController.java` (`GET /api/bien-the-hinh-anh/bien-the/{id}`, `POST`, `DELETE /{id}`)
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/BienTheTable.vue:226-230,567` (form ảnh)

- [ ] SQL:
```sql
IF OBJECT_ID('bien_the_hinh_anh', 'U') IS NULL
BEGIN
    CREATE TABLE bien_the_hinh_anh (
        hinh_anh_id  INT           IDENTITY(1,1) PRIMARY KEY,
        bien_the_id  INT           NOT NULL,
        url          NVARCHAR(500) NOT NULL,
        thu_tu       INT           NOT NULL DEFAULT 0,
        la_anh_chinh BIT           NOT NULL DEFAULT 0,
        CONSTRAINT FK_bthh_bien_the FOREIGN KEY (bien_the_id) REFERENCES bien_the_san_pham(bien_the_id) ON DELETE CASCADE
    );
END
GO
```
- [ ] Backend CRUD mới cho `BienTheHinhAnh` (list theo `bienTheId`, create dùng URL trả về từ `UploadController` có sẵn, delete theo id). Giữ nguyên cột `hinh_anh_bien_the` hiện có trên `bien_the_san_pham` làm ảnh đại diện (đồng bộ = ảnh có `la_anh_chinh = 1`).
- [ ] Frontend: đổi input ảnh đơn (`:567`) thành multi-file input + lưới thumbnail (dùng lại `handleImageFile`/`UploadController` cho từng ảnh), mỗi thumbnail có nút xóa + nút "Đặt làm ảnh chính". Không làm kéo-thả sắp xếp thứ tự (YAGNI — thêm khi có yêu cầu cụ thể).
- [ ] Test backend: `BienTheHinhAnhServiceTest.java` — test tạo ảnh mới tự set `thuTu` tăng dần, và đặt `laAnhChinh=true` cho 1 ảnh thì các ảnh khác cùng `bienTheId` tự về `false`.
- [ ] Run: `cd BackEnd && ./mvnw test -Dtest=BienTheHinhAnhServiceTest` — expect PASS.
- [ ] **Kiểm tra thủ công:** thêm 3 ảnh cho 1 biến thể, xóa 1 ảnh, đặt ảnh khác làm ảnh chính, xác nhận ảnh chính hiện đúng ở `ProductsTable.vue`/trang sản phẩm khách hàng.
- [ ] Commit.

---

## GIAI ĐOẠN 5 — Khách hàng

### Task 5.1: Thêm mã khách hàng + ngày sinh

**Lưu ý:** Note ghi thiếu cả "địa chỉ" nhưng `diaChi` đã tồn tại và bắt buộc nhập (`CustomerFormModal.vue:68-71`) — chỉ thật sự thiếu **mã khách hàng** và **ngày sinh**.

**Files:**
- Modify: `Database/QLBanMayTinh.sql` (bảng `khach_hang`)
- Modify: `BackEnd/src/main/java/com/example/backend/entity/KhachHang.java`, DTO request/response liên quan, `KhachHangService.java` (sinh mã khách hàng tự động khi tạo mới, format ví dụ `KH000123`)
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/CustomerFormModal.vue:19-29` (`emptyCustomerForm`), `:68-71` khu vực input
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/CustomerDetailPage.vue:88-90`

- [ ] SQL: `ALTER TABLE khach_hang ADD ma_khach_hang NVARCHAR(20) NULL, ngay_sinh DATE NULL;` bọc trong kiểm tra `IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('khach_hang') AND name = 'ma_khach_hang')` để idempotent.
- [ ] `KhachHangService.create()`: tự sinh `maKhachHang` dạng `KH` + số thứ tự 6 chữ số (dựa `khach_hang_id` sau khi save, hoặc `SELECT MAX` — chọn cách nào không đụng entity khác đang có sẵn cho việc sinh mã tương tự, nếu có thì tái dùng).
- [ ] `CustomerFormModal.vue`: thêm input `ngày sinh` (`type="date"`, không bắt buộc); `mã khách hàng` chỉ hiển thị read-only (do backend sinh), không có ở form tạo mới.
- [ ] `CustomerDetailPage.vue`: hiện `maKhachHang` cạnh tên, `ngaySinh` (định dạng `dd/MM/yyyy`) trong khối thông tin.
- [ ] Test backend: `KhachHangServiceTest.java` (đã có file) — thêm case `create_generatesSequentialMaKhachHang()`.
- [ ] Run: `cd BackEnd && ./mvnw test -Dtest=KhachHangServiceTest` — expect PASS.
- [ ] **Kiểm tra thủ công:** tạo khách hàng mới, xác nhận có mã tự sinh; sửa ngày sinh, lưu, xem lại chi tiết hiện đúng.
- [ ] Commit.

### Task 5.2: Bỏ tặng voucher (giữ tặng điểm)

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/CustomerDetailPage.vue:95` (nút `giftVoucher`)
- Delete: `FrontEnd/QLBanMayTinh/src/components/admin/TangVoucherModal.vue`

- [ ] Xóa nút "Tặng voucher" và `showGiftVoucherModal` khỏi `CustomerDetailPage.vue`; xóa import + `<TangVoucherModal>` khỏi template. **Không đụng** `TangDiemModal.vue`/nút "Tặng điểm" (`:94`) — đây là chức năng khác, note chỉ yêu cầu bỏ voucher vì sẽ làm riêng ở khuyến mại.
- [ ] Xóa file `TangVoucherModal.vue` nếu không còn chỗ nào khác import nó (grep trước khi xóa).
- [ ] **Kiểm tra thủ công:** vào chi tiết 1 khách hàng, xác nhận chỉ còn nút "Tặng điểm", không còn "Tặng voucher".
- [ ] Commit.

### Task 5.3: Hoàn thiện chức năng đánh giá — thêm phản hồi của shop

**Files:**
- Modify: `Database/QLBanMayTinh.sql` (bảng `danh_gia`)
- Modify: `BackEnd/src/main/java/com/example/backend/entity/DanhGia.java`, `DanhGiaController.java`, `DanhGiaService.java` — thêm endpoint `PUT /api/danh-gia/{id}/phan-hoi` (role admin/nhân viên)
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/DanhGiaPanel.vue:26-31` (thêm lọc theo số sao), thêm UI nhập phản hồi mỗi dòng đánh giá

- [ ] SQL: `ALTER TABLE danh_gia ADD phan_hoi NVARCHAR(1000) NULL, ngay_phan_hoi DATETIME NULL;` (idempotent như trên).
- [ ] Backend: DTO `PhanHoiDanhGiaRequest { String noiDung }`; `DanhGiaService.phanHoi(id, noiDung)` set `phanHoi`/`ngayPhanHoi = now()`; controller endpoint mới.
- [ ] `DanhGiaPanel.vue`: thêm dropdown lọc theo số sao (1-5) cạnh ô tìm kiếm hiện có; mỗi dòng đánh giá thêm ô nhập/hiện phản hồi shop (nếu đã có `phanHoi` thì hiện read-only + nút Sửa, chưa có thì hiện ô nhập + nút Gửi).
- [ ] Test backend: `DanhGiaServiceTest.java` (đã có file) — thêm case `phanHoi_setsContentAndTimestamp()`.
- [ ] Run: `cd BackEnd && ./mvnw test -Dtest=DanhGiaServiceTest` — expect PASS.
- [ ] **Kiểm tra thủ công:** lọc đánh giá theo 5 sao, phản hồi 1 đánh giá, xác nhận hiện lại đúng nội dung sau khi tải lại trang.
- [ ] Commit.

---

## GIAI ĐOẠN 6 — Kho hàng

### Task 6.1: Thiết kế lại hiển thị tồn / giữ / tối thiểu

**Phụ thuộc:** dùng chung trigger đã sửa ở **Task 1.8** — làm Task 1.8 trước.

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/InventoryPanel.vue:775-778` (bảng tồn kho), `:1112-1116` (modal sửa)
- Backend: expose thêm trường `coTheBaN` (= `so_luong_ton_thuc_te - so_luong_giu`, view đã tính sẵn ở `Database/QLBanMayTinh.sql:826`) qua `TonKhoController`/`TonKhoService` thay vì để frontend tự trừ

- [ ] Backend: thêm field `soLuongCoTheBan` vào response DTO của `TonKhoController` list/detail, lấy từ view đã có sẵn công thức (`so_luong_ton_thuc_te - so_luong_giu`) thay vì tính lại ở Java (tránh lệch logic 2 nơi).
- [ ] Frontend: bảng tồn kho thêm cột "Có thể bán" (`soLuongCoTheBan`), giữ "Tồn" + "Giữ" (đều read-only sau Task 1.8) + "Tối thiểu" (vẫn sửa được tay); dòng nào `soLuongCoTheBan <= 0` tô đỏ nhẹ.
- [ ] **Kiểm tra thủ công:** so khớp cột "Có thể bán" = "Tồn" − "Giữ" cho vài dòng bất kỳ.
- [ ] Commit.

### Task 6.2: Chức năng lọc serial

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/InventoryPanel.vue:57` (`inventorySearch`)
- Reference pattern có sẵn: `WarrantyPanel.vue:24-40` (`warrantySearch` đã tìm theo nhiều field gồm serial)

- [ ] Mở rộng `inventorySearch` (hiện chỉ lọc theo tên sản phẩm, `:77`) để thêm 1 ô tìm kiếm serial riêng trong modal "Chi tiết tồn kho theo biến thể" (nơi liệt kê từng serial) — lọc client-side theo `chiTietId`/số serial, theo đúng cách `warrantySearch` đang làm.
- [ ] **Kiểm tra thủ công:** mở chi tiết tồn kho 1 biến thể có nhiều serial, gõ 1 phần mã serial, danh sách lọc đúng.
- [ ] Commit.

### Task 6.3: Thiết kế lại phiếu nhập kho (gộp 4 mục note)

**Root cause đã xác định:** `InventoryPanel.vue` có comment thừa nhận sẵn — phiếu nhập hiện tại chỉ là "chứng từ đối soát nhà cung cấp", **không** tạo ra dòng `chi_tiet_san_pham` (serial) thật nào, và `soLuong`/`donGia` đều nhập tay không ràng buộc gì.

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/InventoryPanel.vue:342-449` (`phieuNhapForm`, `addPhieuNhapItemRow`, `savePhieuNhap`), `:994-995` (input `soLuong`/`donGia`)
- Modify: `BackEnd/src/main/java/com/example/backend/controller/ChiTietPhieuNhapController.java`, `ChiTietPhieuNhapService.java` — khi tạo chi tiết phiếu nhập, đồng thời tạo N dòng `ChiTietSanPham` (serial) tương ứng

- [ ] Frontend — mỗi dòng `phieuNhapForm.items` thêm ô nhập danh sách serial, tái dùng đúng `importSerialsFromFile` (đã có sẵn ở modal "Cập nhật tồn kho", đọc `.xlsx/.xls` qua package `xlsx` đã cài, hoặc `.csv/.txt`) — không cho gõ tay serial (theo đúng yêu cầu note), chỉ cho import file.
- [ ] Frontend — khi chọn biến thể cho 1 dòng: tự điền `donGia = bienThe.giaNhap`; `soLuong` tự bằng `serials.length` của dòng đó. Đổi input `soLuong`/`donGia` (`:994-995`) từ `<input type="number">` sang hiển thị tĩnh (không cho sửa tay).
- [ ] Backend — `ChiTietPhieuNhapService`: khi lưu phiếu nhập, với mỗi dòng nhận thêm field `serials: string[]` từ request, tạo tương ứng N bản ghi `ChiTietSanPham` (`bienTheId`, `serial`, `trangThai = 'trong_kho'`) — đây là điểm nối còn thiếu hiện tại (`savePhieuNhap` không hề gọi sang `ChiTietSanPhamService`).
- [ ] Backend — validate: nếu `serials.length != soLuong` gửi lên thì trả lỗi 400 (phòng trường hợp client cũ/lỗi đồng bộ số).
- [ ] Test backend: `ChiTietPhieuNhapServiceTest.java` (file mới) — test lưu 1 dòng phiếu nhập với 2 serial tạo đúng 2 bản ghi `ChiTietSanPham` trạng thái `trong_kho`, và test lưu với `serials.length` lệch `soLuong` bị từ chối.
- [ ] Run: `cd BackEnd && ./mvnw test -Dtest=ChiTietPhieuNhapServiceTest` — expect PASS.
- [ ] **Kiểm tra thủ công:** tạo 1 phiếu nhập, import file chứa 3 serial cho 1 dòng, xác nhận `soLuong`/`donGia` tự điền đúng không sửa được tay, sau khi lưu vào tab "Serial"/"Tồn kho" thấy đúng 3 serial mới ở trạng thái `trong_kho`.
- [ ] Commit.

### Task 6.4: Thời gian còn lại bảo hành (bảng Phiếu bảo hành)

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/WarrantyPanel.vue:246-268` (bảng "Phiếu bảo hành")

- [ ] Thêm cột "Còn lại" vào bảng Phiếu bảo hành, dùng lại đúng công thức đã có ở bảng "Còn hạn bảo hành" cùng file (`daysUntilExpiry`, `:41`) áp cho `p.ngayHetBh` của từng phiếu; hiện "Hết hạn" (đỏ) nếu số ngày ≤ 0, không phụ thuộc vào `trangThai` nhập tay.
- [ ] **Kiểm tra thủ công:** đối chiếu vài phiếu bảo hành, cột "Còn lại" đúng bằng `ngayHetBh − hôm nay` theo ngày.
- [ ] Commit.

---

## Self-review — đối chiếu 22 mục note

| # | Mục note | Task |
|---|---|---|
| 1 | Bỏ SKU + danh mục ở POS | 1.1 |
| 2 | Bỏ dấu tìm, tự load | 1.2 |
| 3 | Chọn nhiều serial | 1.3 |
| 4 | Tính phí vận chuyển | 1.4 |
| 5 | Voucher mở tab mới | 1.5 |
| 6 | QR chuyển khoản giả lập | 1.6/1.7 |
| 7 | Bỏ mã QR thừa | 1.6/1.7 |
| 8 | Ưu tiên bán tại quầy | 1.8 |
| 9 | Hiển thị hóa đơn | 2.1 |
| 10 | Hủy/sửa → hủy đơn | 2.2 |
| 11 | Thông tin khách hàng trong đơn | 2.3 |
| 12 | Bỏ thêm/xóa sản phẩm trong đơn | 2.4 |
| 13 | Chi tiết SP mở tab, 3 tab con | 3.1 |
| 14 | Xóa danh mục sản phẩm | 3.2 |
| 15 | Tinh chỉnh ảnh biến thể | 4.1 |
| 16 | Mã KH, ngày sinh, địa chỉ | 5.1 (địa chỉ đã có sẵn) |
| 17 | Bỏ tặng voucher | 5.2 |
| 18 | Hoàn thiện đánh giá | 5.3 |
| 19 | Thiết kế lại tồn/giữ/tối thiểu | 6.1 |
| 20 | Lọc serial | 6.2 |
| 21 | Thiết kế lại phiếu nhập (4 ý) | 6.3 |
| 22 | Thời gian còn lại bảo hành | 6.4 |

Tất cả 22 mục đều có task tương ứng. 3 điểm cần người viết note xác nhận lại trước khi build (đánh dấu GIẢ ĐỊNH ở trên): Task 1.2 (ý nghĩa "bỏ dấu tìm"), Task 1.4 (phí ship có áp dụng cho đơn tại quầy hay không), Task 1.6/1.7 (gộp QR vào chuyển khoản có đúng ý hay không).
