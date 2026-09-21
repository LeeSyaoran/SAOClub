-- ============================================================
-- MIGRATION: Thuộc tính sản phẩm (màn hình, pin, trọng lượng, HĐH, màu sắc)
-- Chạy file này SAU khi chạy QLBanMayTinh.sql gốc
-- ============================================================

USE QLBanMayTinh;
GO

-- Bảng định nghĩa các trường thuộc tính
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'thuoc_tinh')
BEGIN
    CREATE TABLE thuoc_tinh (
        thuoc_tinh_id    INT            IDENTITY(1,1) PRIMARY KEY,
        ten_truong       VARCHAR(50)    NOT NULL UNIQUE,   -- key: mau_sac, man_hinh, pin, trong_luong, he_dieu_hanh
        ten_hien_thi    NVARCHAR(100)  NOT NULL,          -- Tên hiển thị: "Màu sắc", "Màn hình", "Pin"
        loai_du_lieu    VARCHAR(20)    NOT NULL DEFAULT 'text', -- text | select
        bat_buoc        BIT            NOT NULL DEFAULT 0,  -- Thuộc tính bắt buộc
        thu_tu_hien_thi INT            NOT NULL DEFAULT 0, -- Thứ tự hiển thị trong form
        trang_thai      NVARCHAR(20)   NOT NULL DEFAULT N'active'
            CONSTRAINT CK_tt_trangthai CHECK (trang_thai IN (N'active', N'inactive')),
        ngay_tao        DATETIME       NOT NULL DEFAULT GETDATE()
    );
END
GO

-- Bảng giá trị cho thuộc tính dạng select
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'thuoc_tinh_gia_tri')
BEGIN
    CREATE TABLE thuoc_tinh_gia_tri (
        gia_tri_id      INT            IDENTITY(1,1) PRIMARY KEY,
        thuoc_tinh_id   INT            NOT NULL,
        gia_tri         NVARCHAR(100)  NOT NULL,
        thu_tu          INT            NOT NULL DEFAULT 0,
        CONSTRAINT UQ_ttgt_thuoc_tinh_gia_tri UNIQUE (thuoc_tinh_id, gia_tri),
        CONSTRAINT FK_ttgt_thuoc_tinh FOREIGN KEY (thuoc_tinh_id) REFERENCES thuoc_tinh(thuoc_tinh_id) ON DELETE CASCADE
    );
END
GO

-- Index cho tìm kiếm nhanh
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_ttgt_thuoc_tinh_id')
    CREATE INDEX IX_ttgt_thuoc_tinh_id ON thuoc_tinh_gia_tri(thuoc_tinh_id);
GO

-- ============================================================
-- SEED DATA: Backfill các thuộc tính hiện tại từ hard-code
-- ============================================================

-- Insert các thuộc tính hiện tại (nếu chưa có)
INSERT INTO thuoc_tinh (ten_truong, ten_hien_thi, loai_du_lieu, bat_buoc, thu_tu_hien_thi)
SELECT 'mau_sac', N'Màu sắc', 'select', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM thuoc_tinh WHERE ten_truong = 'mau_sac');

INSERT INTO thuoc_tinh (ten_truong, ten_hien_thi, loai_du_lieu, bat_buoc, thu_tu_hien_thi)
SELECT 'man_hinh', N'Màn hình', 'select', 0, 2
WHERE NOT EXISTS (SELECT 1 FROM thuoc_tinh WHERE ten_truong = 'man_hinh');

INSERT INTO thuoc_tinh (ten_truong, ten_hien_thi, loai_du_lieu, bat_buoc, thu_tu_hien_thi)
SELECT 'pin', N'Pin', 'select', 0, 3
WHERE NOT EXISTS (SELECT 1 FROM thuoc_tinh WHERE ten_truong = 'pin');

INSERT INTO thuoc_tinh (ten_truong, ten_hien_thi, loai_du_lieu, bat_buoc, thu_tu_hien_thi)
SELECT 'trong_luong', N'Trọng lượng (kg)', 'text', 0, 4
WHERE NOT EXISTS (SELECT 1 FROM thuoc_tinh WHERE ten_truong = 'trong_luong');

INSERT INTO thuoc_tinh (ten_truong, ten_hien_thi, loai_du_lieu, bat_buoc, thu_tu_hien_thi)
SELECT 'he_dieu_hanh', N'Hệ điều hành', 'select', 0, 5
WHERE NOT EXISTS (SELECT 1 FROM thuoc_tinh WHERE ten_truong = 'he_dieu_hanh');
GO

-- Seed giá trị mặc định cho Màu sắc
INSERT INTO thuoc_tinh_gia_tri (thuoc_tinh_id, gia_tri, thu_tu)
SELECT tt.thuoc_tinh_id, gt.gia_tri, gt.thu_tu
FROM thuoc_tinh tt
CROSS JOIN (
    SELECT N'Đen' AS gia_tri, 1 AS thu_tu UNION ALL
    SELECT N'Trắng', 2 UNION ALL
    SELECT N'Bạc', 3 UNION ALL
    SELECT N'Xám', 4 UNION ALL
    SELECT N'Xanh Dương', 5 UNION ALL
    SELECT N'Xanh Lá', 6 UNION ALL
    SELECT N'Đỏ', 7 UNION ALL
    SELECT N'Vàng', 8 UNION ALL
    SELECT N'Hồng', 9 UNION ALL
    SELECT N'Tím', 10 UNION ALL
    SELECT N'Cam', 11 UNION ALL
    SELECT N'Nâu', 12
) gt
WHERE tt.ten_truong = 'mau_sac'
AND NOT EXISTS (
    SELECT 1 FROM thuoc_tinh_gia_tri ttgt
    WHERE ttgt.thuoc_tinh_id = tt.thuoc_tinh_id AND ttgt.gia_tri = gt.gia_tri
);
GO

-- Seed giá trị mặc định cho Màn hình
INSERT INTO thuoc_tinh_gia_tri (thuoc_tinh_id, gia_tri, thu_tu)
SELECT tt.thuoc_tinh_id, gt.gia_tri, gt.thu_tu
FROM thuoc_tinh tt
CROSS JOIN (
    SELECT N'15.6" FHD 60Hz' AS gia_tri, 1 AS thu_tu UNION ALL
    SELECT N'15.6" FHD 144Hz', 2 UNION ALL
    SELECT N'15.6" QHD 240Hz', 3 UNION ALL
    SELECT N'16" 2.5K 120Hz', 4 UNION ALL
    SELECT N'16" FHD 165Hz', 5 UNION ALL
    SELECT N'16" WQXGA 165Hz', 6 UNION ALL
    SELECT N'16" 2.8K OLED 120Hz', 7 UNION ALL
    SELECT N'14" FHD 60Hz', 8 UNION ALL
    SELECT N'14" 2.8K OLED', 9
) gt
WHERE tt.ten_truong = 'man_hinh'
AND NOT EXISTS (
    SELECT 1 FROM thuoc_tinh_gia_tri ttgt
    WHERE ttgt.thuoc_tinh_id = tt.thuoc_tinh_id AND ttgt.gia_tri = gt.gia_tri
);
GO

-- Seed giá trị mặc định cho Pin
INSERT INTO thuoc_tinh_gia_tri (thuoc_tinh_id, gia_tri, thu_tu)
SELECT tt.thuoc_tinh_id, gt.gia_tri, gt.thu_tu
FROM thuoc_tinh tt
CROSS JOIN (
    SELECT N'41Wh' AS gia_tri, 1 AS thu_tu UNION ALL
    SELECT N'48Wh', 2 UNION ALL
    SELECT N'50Wh', 3 UNION ALL
    SELECT N'52Wh', 4 UNION ALL
    SELECT N'54Wh', 5 UNION ALL
    SELECT N'57Wh', 6 UNION ALL
    SELECT N'75Wh', 7 UNION ALL
    SELECT N'80Wh', 8 UNION ALL
    SELECT N'86Wh', 9 UNION ALL
    SELECT N'90Wh', 10
) gt
WHERE tt.ten_truong = 'pin'
AND NOT EXISTS (
    SELECT 1 FROM thuoc_tinh_gia_tri ttgt
    WHERE ttgt.thuoc_tinh_id = tt.thuoc_tinh_id AND ttgt.gia_tri = gt.gia_tri
);
GO

-- Seed giá trị mặc định cho Hệ điều hành
INSERT INTO thuoc_tinh_gia_tri (thuoc_tinh_id, gia_tri, thu_tu)
SELECT tt.thuoc_tinh_id, gt.gia_tri, gt.thu_tu
FROM thuoc_tinh tt
CROSS JOIN (
    SELECT N'Windows 11 Home' AS gia_tri, 1 AS thu_tu UNION ALL
    SELECT N'Windows 11 Pro', 2 UNION ALL
    SELECT N'Windows 10 Home', 3 UNION ALL
    SELECT N'Windows 10 Pro', 4 UNION ALL
    SELECT N'macOS', 5 UNION ALL
    SELECT N'Không kèm HĐH', 6 UNION ALL
    SELECT N'Linux', 7
) gt
WHERE tt.ten_truong = 'he_dieu_hanh'
AND NOT EXISTS (
    SELECT 1 FROM thuoc_tinh_gia_tri ttgt
    WHERE ttgt.thuoc_tinh_id = tt.thuoc_tinh_id AND ttgt.gia_tri = gt.gia_tri
);
GO

-- ============================================================
-- MIGRATE: Backfill giá trị từ dữ liệu hiện có (bien_the_san_pham)
-- ============================================================

-- Màu sắc từ bien_the_san_pham.mau_sac
INSERT INTO thuoc_tinh_gia_tri (thuoc_tinh_id, gia_tri, thu_tu)
SELECT tt.thuoc_tinh_id, bt.mau_sac, 100
FROM thuoc_tinh tt
JOIN (
    SELECT DISTINCT mau_sac FROM bien_the_san_pham
    WHERE mau_sac IS NOT NULL AND mau_sac <> N''
) bt ON tt.ten_truong = 'mau_sac'
WHERE NOT EXISTS (
    SELECT 1 FROM thuoc_tinh_gia_tri ttgt
    WHERE ttgt.thuoc_tinh_id = tt.thuoc_tinh_id AND ttgt.gia_tri = bt.mau_sac
);
GO

-- Màn hình từ bien_the_san_pham.kich_thuoc_man_hinh
INSERT INTO thuoc_tinh_gia_tri (thuoc_tinh_id, gia_tri, thu_tu)
SELECT tt.thuoc_tinh_id, bt.kich_thuoc_man_hinh, 100
FROM thuoc_tinh tt
JOIN (
    SELECT DISTINCT kich_thuoc_man_hinh FROM bien_the_san_pham
    WHERE kich_thuoc_man_hinh IS NOT NULL AND kich_thuoc_man_hinh <> N''
) bt ON tt.ten_truong = 'man_hinh'
WHERE NOT EXISTS (
    SELECT 1 FROM thuoc_tinh_gia_tri ttgt
    WHERE ttgt.thuoc_tinh_id = tt.thuoc_tinh_id AND ttgt.gia_tri = bt.kich_thuoc_man_hinh
);
GO

-- Pin từ bien_the_san_pham.pin
INSERT INTO thuoc_tinh_gia_tri (thuoc_tinh_id, gia_tri, thu_tu)
SELECT tt.thuoc_tinh_id, bt.pin, 100
FROM thuoc_tinh tt
JOIN (
    SELECT DISTINCT pin FROM bien_the_san_pham
    WHERE pin IS NOT NULL AND pin <> N''
) bt ON tt.ten_truong = 'pin'
WHERE NOT EXISTS (
    SELECT 1 FROM thuoc_tinh_gia_tri ttgt
    WHERE ttgt.thuoc_tinh_id = tt.thuoc_tinh_id AND ttgt.gia_tri = bt.pin
);
GO

-- Hệ điều hành từ bien_the_san_pham.he_dieu_hanh
INSERT INTO thuoc_tinh_gia_tri (thuoc_tinh_id, gia_tri, thu_tu)
SELECT tt.thuoc_tinh_id, bt.he_dieu_hanh, 100
FROM thuoc_tinh tt
JOIN (
    SELECT DISTINCT he_dieu_hanh FROM bien_the_san_pham
    WHERE he_dieu_hanh IS NOT NULL AND he_dieu_hanh <> N''
) bt ON tt.ten_truong = 'he_dieu_hanh'
WHERE NOT EXISTS (
    SELECT 1 FROM thuoc_tinh_gia_tri ttgt
    WHERE ttgt.thuoc_tinh_id = tt.thuoc_tinh_id AND ttgt.gia_tri = bt.he_dieu_hanh
);
GO

PRINT N'Migration thuoc_tinh hoàn tất.';
GO
