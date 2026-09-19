-- ============================================================
-- MIGRATION: Cập nhật từ git (396f357, 7d43379, 8e40ba0)
-- Chạy sau QLBanMayTinh.sql gốc
-- ============================================================

-- Thêm cột hinh_anh vào bảng khach_hang (idempotent)
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('khach_hang') AND name = 'hinh_anh')
BEGIN
    ALTER TABLE khach_hang ADD hinh_anh NVARCHAR(500) NULL;
END
GO

-- Cập nhật hình ảnh cho 3 khách hàng đầu tiên
UPDATE khach_hang SET hinh_anh = '/images/kh1.jpg' WHERE khach_hang_id = 1;
UPDATE khach_hang SET hinh_anh = '/images/kh2.webp' WHERE khach_hang_id = 2;
UPDATE khach_hang SET hinh_anh = '/images/kh3.webp' WHERE khach_hang_id = 3;
GO
