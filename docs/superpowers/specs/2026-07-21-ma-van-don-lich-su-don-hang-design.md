# Mã vận đơn & Lịch sử trạng thái đơn hàng — Design Spec

**Ngày:** 2026-07-21

## Bối cảnh

Khách hàng tham khảo màn "My Purchases" của Shopee (tab trạng thái + timeline giao hàng + log
lịch sử theo mốc thời gian + mã vận đơn + nút "Buy Again"). So sánh với `AccountPage.vue` hiện
tại: timeline 5 bước (`OrderStatusTimeline.vue`) đã phủ đủ vòng đời đơn và đẹp hơn bản Shopee,
nhưng thiếu 2 thứ Shopee có: **mã vận đơn** và **log lịch sử chi tiết theo mốc thời gian**. Nút
"Mua lại" đã làm xong riêng (không cần spec, chỉ là nối `add-to-cart` có sẵn).

Mọi đường cập nhật `trang_thai_don_hang` của đơn hàng hiện tại đi qua đúng 1 chỗ:
`OrdersTable.vue` → `DonHangService.update()` → `PUT /api/don-hang/update/{id}` →
`DonHangService.update()` (backend). Đây là điểm chốt để tự động ghi log mà không cần sửa
nhiều nơi.

## Mục tiêu

1. Thêm cột `ma_van_don` cho `don_hang`, nhân viên/admin nhập tay khi chuyển đơn sang "Đang giao".
2. Tự động ghi 1 dòng lịch sử mỗi khi `trang_thai_don_hang` đổi — qua DB trigger, không đụng code Java.
3. Khách hàng xem được mã vận đơn + danh sách lịch sử theo mốc thời gian trong `AccountPage.vue`.

## Phần 1 — Dữ liệu (thêm vào cuối `Database/QLBanMayTinh.sql`, idempotent)

```sql
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('don_hang') AND name = 'ma_van_don')
BEGIN
    ALTER TABLE don_hang ADD ma_van_don VARCHAR(50) NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'lich_su_don_hang')
BEGIN
    CREATE TABLE lich_su_don_hang (
        lich_su_id     INT           IDENTITY(1,1) PRIMARY KEY,
        don_hang_id    INT           NOT NULL,
        trang_thai_cu  NVARCHAR(30)  NULL,
        trang_thai_moi NVARCHAR(30)  NOT NULL,
        thoi_gian      DATETIME      NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_lsdh_don_hang FOREIGN KEY (don_hang_id) REFERENCES don_hang(don_hang_id) ON DELETE CASCADE
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_lsdh_don_hang')
    CREATE INDEX IX_lsdh_don_hang ON lich_su_don_hang(don_hang_id, thoi_gian);
GO

-- Tự ghi log mỗi khi trạng thái đơn đổi — chỗ duy nhất phát sinh log, không cần backend
-- Java can thiệp, không sợ thiếu dòng nếu sau này có thêm đường cập nhật trạng thái khác.
CREATE OR ALTER TRIGGER trg_don_hang_log_trangthai
ON don_hang
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF UPDATE(trang_thai_don_hang)
    BEGIN
        INSERT INTO lich_su_don_hang (don_hang_id, trang_thai_cu, trang_thai_moi, thoi_gian)
        SELECT i.don_hang_id, d.trang_thai_don_hang, i.trang_thai_don_hang, GETDATE()
        FROM inserted i
        JOIN deleted d ON d.don_hang_id = i.don_hang_id
        WHERE d.trang_thai_don_hang <> i.trang_thai_don_hang;
    END
END
GO
```

Không thêm cột `ghi_chu` cho `lich_su_don_hang` — quyết định đã chốt là log tự động thuần
tuý (không có ghi chú tay), thêm cột không dùng tới là YAGNI.

## Phần 2 — Nhập mã vận đơn (Admin/Staff, dùng chung `OrdersTable.vue`)

- `orderStatusForm` (đang có `trangThaiDonHang`, `trangThaiThanhToan`, `ngayGiaoDuKien`,
  `ngayGiaoThucTe`) thêm field `maVanDon`.
- `openOrderStatus(o)` prefill `orderStatusForm.maVanDon = o.maVanDon ?? ""`.
- `buildOrderUpdateBody()` thêm tham số + field `maVanDon` vào body PUT.
- Modal "Cập nhật trạng thái đơn hàng" (dòng ~934 trong `OrdersTable.vue`) thêm 1 input text
  "Mã vận đơn" ngay dưới dropdown trạng thái. Không validate bắt buộc — chỉ để trống nếu chưa có.
- `advanceOrderStatus()` (nút "bước tiếp theo" 1-click trên bảng): khi `next === 'shipping'`,
  đổi từ PUT thẳng sang mở modal `openOrderStatus(o)` (prefill sẵn
  `orderStatusForm.trangThaiDonHang = 'shipping'`) — bắt nhân viên dừng lại nhập mã vận đơn,
  đúng theo mẫu đã có sẵn cho case "confirmed + online" (mở modal chọn serial thay vì 1-click).
  Các bước khác (`confirmed`→`processing`, `shipping`→`delivered`...) giữ nguyên 1-click.

## Phần 3 — Backend

- `DonHang` entity: thêm `@Column(name = "ma_van_don", length = 50) private String maVanDon;`.
- `DonHangRequest` / `DonHangResponse`: thêm field `maVanDon` (không validation bắt buộc).
- `DonHangRepository.hienThiDonHang()`: thêm `d.maVanDon` vào constructor JPQL DTO projection.
- Entity mới `LichSuDonHang` (map bảng `lich_su_don_hang`, mọi cột `insertable = false` /
  read-only phía JPA vì trigger DB tự ghi — Java không bao giờ INSERT/UPDATE bảng này).
- `LichSuDonHangResponse` (`lichSuId, donHangId, trangThaiCu, trangThaiMoi, thoiGian`).
- `LichSuDonHangRepository` — 1 method `findByDonHangIdOrderByThoiGianAsc(Integer donHangId)`.
- `LichSuDonHangController` — đúng khuôn `ChiTietDonHangController.getByDonHang()`:
  ```java
  @RestController
  @RequestMapping("/api/lich-su-don-hang")
  public class LichSuDonHangController {
      @GetMapping("/don-hang/{donHangId}")
      public List<LichSuDonHangResponse> getByDonHang(@PathVariable Integer donHangId) { ... }
  }
  ```
  Giữ mở (không `@PreAuthorize`) — khách hàng xem lịch sử đơn của chính mình, giống
  `ChiTietDonHangController.getByDonHang()` đang mở.

## Phần 4 — Frontend khách hàng (`AccountPage.vue`)

- `Service/LichSuDonHangService.js` — 1 hàm `getByDonHang(donHangId)`, đúng khuôn
  `ChiTietDonHangService.js`.
- Tải song song với `itemsByOrder` trong `fetchData()`, lưu vào `historyByOrder.value[donHangId]`.
- Dưới `<OrderStatusTimeline>` hiện có (chỉ hiện với đơn ở tab "Đang giao"/"Hoàn tất", tức
  `trangThaiDonHang` là `shipping` hoặc `delivered`):
  - Nếu có `o.maVanDon`: hiện dòng "📦 Mã vận đơn: `{{ o.maVanDon }}`" kèm nút copy (dùng
    `navigator.clipboard.writeText`, đổi icon tạm 1.5s báo đã copy — không cần thư viện).
  - Danh sách log dọc: mỗi dòng `historyByOrder[donHangId]` hiện `thoiGian` (format qua
    `formatDate` có sẵn) + nhãn trạng thái (tái dùng `orderStatusLabel(trangThaiMoi)` đã có
    trong `utils/orderStatus.js` — không cần bảng message riêng).

## Ngoài phạm vi (Non-goals)

- Không cho nhân viên/admin tự viết ghi chú tự do vào log (đã chốt: log tự động thuần tuý).
- Không tích hợp API tra cứu vận đơn thật của đơn vị vận chuyển (GHN/GHTK...) — mã vận đơn chỉ
  là text tự nhập, hiển thị tham khảo.
- Không đổi trigger/log cho các trạng thái không phải qua `PUT /don-hang/update` (không có
  đường nào khác hiện tại — nếu phát sinh sau này, trigger vẫn tự bắt được vì gắn ở tầng DB).

## Tự rà soát (self-review)

**1. Phủ đủ yêu cầu đã chốt:** mã vận đơn nhập tay bởi cả nhân viên lẫn admin (dùng chung
`OrdersTable.vue`) ✅; log tự động khi đổi trạng thái ✅; giữ timeline cũ + thêm log bên dưới ✅.

**2. Không còn placeholder** — mọi bảng/cột/file/hàm đã xác định chính xác qua đọc code +
schema thực tế (`DonHangController`, `DonHangRepository`, `DonHang` entity, `OrdersTable.vue`,
`ChiTietDonHangController` làm khuôn mẫu).

**3. Nhất quán:** trigger đặt tên `trg_don_hang_log_trangthai` không trùng trigger nào đã có
trong file (`trg_ctsp_*` là trigger tồn kho, tên khác namespace). Cột `ma_van_don` không có
CHECK constraint vì là text tự do, không phải enum.

**4. Idempotency:** `ALTER TABLE`/`CREATE TABLE` đều bọc `IF NOT EXISTS`; `CREATE OR ALTER
TRIGGER` tự idempotent theo cú pháp T-SQL — khớp đúng convention file `.sql` hiện tại (user luôn
chạy lại toàn bộ file, không chạy snippet lẻ, nên mọi thay đổi schema phải an toàn khi re-run).
