# Tích điểm mua hàng & Đổi điểm lấy voucher — Design Spec

**Ngày:** 2026-07-21

## Bối cảnh

`khach_hang.diem_tich_luy` đã tồn tại trong schema từ trước nhưng chưa từng được hệ thống tự cộng ở đâu — chỉ nhân viên sửa tay được qua form khách hàng. Hệ thống khuyến mãi (`khuyen_mai`) hiện tại là mã công khai dùng chung (bất kỳ ai biết mã đều dùng được, giới hạn theo tổng số lần dùng toàn hệ thống `so_luong_toi_da`/`so_lan_da_dung`), không có khái niệm "voucher riêng của 1 khách" — nên phần đổi điểm lấy voucher cần cơ chế mới, không tái dùng thẳng bảng `khuyen_mai`.

## Mục tiêu

1. Tự động cộng điểm cho khách hàng khi đơn hàng chuyển sang trạng thái "đã giao" (delivered), tỷ lệ 10.000đ = 1 điểm.
2. Admin quản lý danh mục phần thưởng đổi điểm (tên, điểm cần, mức giảm).
3. Khách hàng tự đổi điểm lấy voucher cá nhân (hạn dùng 30 ngày), áp dụng được ở checkout.

## Phần 1 — Tích điểm tự động (DB trigger)

### Thay đổi DB (thêm vào cuối `Database/QLBanMayTinh.sql`, idempotent)

```sql
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('don_hang') AND name = 'da_cong_diem')
BEGIN
    ALTER TABLE don_hang ADD da_cong_diem BIT NOT NULL DEFAULT 0;
END
GO

CREATE OR ALTER TRIGGER trg_don_hang_cong_diem
ON don_hang
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF UPDATE(trang_thai_don_hang)
    BEGIN
        UPDATE kh
        SET kh.diem_tich_luy = kh.diem_tich_luy + FLOOR(i.thanh_tien / 10000)
        FROM khach_hang kh
        JOIN inserted i ON i.khach_hang_id = kh.khach_hang_id
        JOIN deleted d ON d.don_hang_id = i.don_hang_id
        WHERE i.trang_thai_don_hang = N'delivered'
          AND d.trang_thai_don_hang <> N'delivered'
          AND i.da_cong_diem = 0;

        UPDATE don_hang SET da_cong_diem = 1
        WHERE don_hang_id IN (
            SELECT i.don_hang_id FROM inserted i JOIN deleted d ON d.don_hang_id = i.don_hang_id
            WHERE i.trang_thai_don_hang = N'delivered' AND d.trang_thai_don_hang <> N'delivered' AND i.da_cong_diem = 0
        );
    END
END
GO
```

Không đụng code Java — đúng nguyên tắc đã dùng cho `trg_don_hang_log_trangthai` (mã vận đơn/lịch sử): mọi đường cập nhật trạng thái đơn (hiện tại chỉ có 1 chỗ, `PUT /don-hang/update`) đều tự động kích hoạt, không cần nhớ gọi hàm cộng điểm ở từng nơi. Cờ `da_cong_diem` chặn cộng điểm 2 lần nếu đơn lỡ đổi qua lại trạng thái.

**Không hoàn/trừ điểm khi đơn bị trả hàng sau đó** — ngoài phạm vi, xử lý thủ công nếu cần (nhân viên tự trừ tay qua form khách hàng đã có sẵn).

## Phần 2 — Danh mục đổi thưởng (admin CRUD)

### Bảng mới `dm_doi_thuong`

```sql
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'dm_doi_thuong')
BEGIN
    CREATE TABLE dm_doi_thuong (
        doi_thuong_id   INT            IDENTITY(1,1) PRIMARY KEY,
        ten             NVARCHAR(150)  NOT NULL,
        mo_ta           NVARCHAR(500)  NULL,
        diem_can        INT            NOT NULL CONSTRAINT CK_ddt_diemcan CHECK (diem_can > 0),
        loai            NVARCHAR(20)   NOT NULL CONSTRAINT CK_ddt_loai CHECK (loai IN (N'percent', N'fixed')),
        gia_tri         DECIMAL(18,0)  NOT NULL CONSTRAINT CK_ddt_giatri CHECK (gia_tri > 0),
        CONSTRAINT CK_ddt_percent_max100 CHECK (loai <> N'percent' OR gia_tri <= 100),
        gia_tri_toi_da  DECIMAL(18,0)  NULL,
        trang_thai      NVARCHAR(20)   NOT NULL DEFAULT N'active'
            CONSTRAINT CK_ddt_trangthai CHECK (trang_thai IN (N'active', N'inactive')),
        ngay_tao        DATETIME       NOT NULL DEFAULT GETDATE()
    );
END
GO
```

Backend: `DmDoiThuong` entity + `DmDoiThuongRequest`/`Response` + `DmDoiThuongRepository` + `DmDoiThuongService` + `DmDoiThuongController` — CRUD đầy đủ, khóa `@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")` class-level (đúng khuôn `PhieuTraHangController`, không theo khuôn `KhuyenMaiController` — controller đó đang mở CRUD hoàn toàn không khóa gì, có vẻ là lỗ hổng có sẵn, ngoài phạm vi task này, không đụng vào). Riêng `GET` (xem danh mục) mở cho khách hàng xem để chọn đổi — thêm `@PreAuthorize("isAuthenticated()")` override method-level cho `getAll()`.

Frontend: thêm section "Đổi thưởng" vào `AdminPage.vue`, đúng khuôn CRUD "Khuyến mãi" đã có sẵn trong cùng file (form + bảng, không tách component riêng vì cũng chỉ admin dùng).

## Phần 3 — Đổi điểm lấy voucher cá nhân

### Bảng mới `phieu_giam_gia_ca_nhan`

```sql
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'phieu_giam_gia_ca_nhan')
BEGIN
    CREATE TABLE phieu_giam_gia_ca_nhan (
        phieu_id       INT            IDENTITY(1,1) PRIMARY KEY,
        khach_hang_id  INT            NOT NULL,
        doi_thuong_id  INT            NULL,
        ma_phieu       VARCHAR(50)    NOT NULL UNIQUE DEFAULT UPPER(LEFT(REPLACE(CAST(NEWID() AS VARCHAR(36)), '-', ''), 12)),
        loai           NVARCHAR(20)   NOT NULL CONSTRAINT CK_pggcn_loai CHECK (loai IN (N'percent', N'fixed')),
        gia_tri        DECIMAL(18,0)  NOT NULL,
        gia_tri_toi_da DECIMAL(18,0)  NULL,
        da_su_dung     BIT            NOT NULL DEFAULT 0,
        ngay_doi       DATETIME       NOT NULL DEFAULT GETDATE(),
        ngay_het_han   DATETIME       NOT NULL,
        don_hang_id    INT            NULL,
        CONSTRAINT FK_pggcn_khach_hang FOREIGN KEY (khach_hang_id) REFERENCES khach_hang(khach_hang_id),
        CONSTRAINT FK_pggcn_doi_thuong FOREIGN KEY (doi_thuong_id) REFERENCES dm_doi_thuong(doi_thuong_id),
        CONSTRAINT FK_pggcn_don_hang   FOREIGN KEY (don_hang_id)   REFERENCES don_hang(don_hang_id)
    );
END
GO
```

`loai`/`gia_tri`/`gia_tri_toi_da` copy từ `dm_doi_thuong` tại thời điểm đổi (không tham chiếu sống — admin sửa danh mục sau này không ảnh hưởng voucher khách đã đổi trước đó, đúng nguyên tắc snapshot đã dùng cho địa chỉ giao hàng trong `don_hang`).

### Backend

- `PhieuGiamGiaCaNhanService.doiThuong(Integer doiThuongId)`: tự suy khách hàng qua `SecurityContextHolder` (đúng pattern đã dùng cho trả hàng/lịch sử đơn). Kiểm tra: `dm_doi_thuong.trang_thai='active'`; `khach_hang.diem_tich_luy >= diem_can`. Transaction: trừ điểm (`diem_tich_luy -= diem_can`), tạo `phieu_giam_gia_ca_nhan` với `ngay_het_han = now() + 30 ngày`.
- `PhieuGiamGiaCaNhanService.getByKhachHang()`: trả voucher của khách đang đăng nhập (tự suy, không nhận `khachHangId` từ client).
- Endpoint: `POST /api/phieu-giam-gia-ca-nhan/doi-thuong/{doiThuongId}`, `GET /api/phieu-giam-gia-ca-nhan/cua-toi` — cả 2 chỉ cần đăng nhập (`@PreAuthorize("isAuthenticated()")`), không cần vai trò staff.
- Áp dụng lúc thanh toán: `DonHangService` khi tạo đơn nhận thêm `phieuGiamGiaCaNhanId` (optional) trong `DonHangRequest` — nếu có, kiểm tra thuộc đúng khách, chưa dùng, chưa hết hạn, rồi trừ vào `giam_gia`, đánh dấu `da_su_dung=1` + gắn `don_hang_id`.

### Frontend

- `AccountPage.vue`: thêm khu vực mới (tab hoặc section trong "Cài đặt tài khoản") hiện số điểm hiện có, danh sách phần thưởng đổi được (nút "Đổi" nếu đủ điểm), danh sách voucher đã đổi (mã, giá trị, hạn dùng, đã dùng hay chưa).
- `CheckoutModal.vue`: thêm khối "Voucher của bạn" — tách riêng khỏi danh sách `eligiblePromos` (khuyến mãi công khai) hiện có vì khác bảng/khác cơ chế, khách chọn 1 trong 2 loại giảm giá (không cộng dồn, giữ đơn giản như checkout hiện tại chỉ cho áp 1 mã).

## Ngoài phạm vi (Non-goals)

- Không hoàn/trừ điểm khi đơn bị trả hàng.
- Không cho dùng đồng thời mã khuyến mãi công khai + voucher cá nhân trong cùng 1 đơn.
- Không sửa `KhuyenMaiController` (lỗ hổng quyền có sẵn, không thuộc phạm vi task này).
- Không cho khách chuyển nhượng/tặng voucher cho khách khác.

## Tự rà soát (self-review)

**1. Phủ đủ yêu cầu đã chốt:** tích điểm tự động lúc delivered, 10.000đ=1 điểm ✅; admin tự CRUD danh mục ✅; voucher hạn 30 ngày ✅.

**2. Không còn placeholder** — mọi bảng/cột/endpoint đã xác định cụ thể qua đọc code + schema thực tế (`KhuyenMaiController` làm khuôn tham khảo, `PhieuTraHangController` làm khuôn khóa quyền).

**3. Nhất quán:** trigger đặt tên `trg_don_hang_cong_diem` không trùng `trg_don_hang_log_trangthai` đã có; `phieu_giam_gia_ca_nhan` snapshot dữ liệu tại thời điểm đổi, không tham chiếu sống tới `dm_doi_thuong`.

**4. Idempotency:** mọi `ALTER TABLE`/`CREATE TABLE` bọc `IF NOT EXISTS`, trigger dùng `CREATE OR ALTER` — khớp convention file `.sql` hiện tại.
