-- ============================================================
--  Thêm cột Firebase Auth cho bảng tai_khoan
--  Chạy file này một lần duy nhất trong SSMS hoặc Azure Data Studio
-- ============================================================

USE QLBanMayTinh;
GO

-- Thêm cột provider (google, facebook, null = login thường)
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('tai_khoan') AND name = 'provider')
    ALTER TABLE tai_khoan ADD provider VARCHAR(20) NULL;
GO

-- Thêm cột provider_uid (ID từ Google/Facebook)
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('tai_khoan') AND name = 'provider_uid')
    ALTER TABLE tai_khoan ADD provider_uid VARCHAR(255) NULL;
GO

-- Thêm cột avatar_url (link ảnh profile từ Google/Facebook)
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('tai_khoan') AND name = 'avatar_url')
    ALTER TABLE tai_khoan ADD avatar_url VARCHAR(500) NULL;
GO

-- Xác nhận đã thêm thành công
SELECT name, type_name(user_type_id) as type, max_length
FROM sys.columns
WHERE object_id = OBJECT_ID('tai_khoan')
  AND name IN ('provider', 'provider_uid', 'avatar_url');
GO

PRINT '✅ Đã thêm cột Firebase Auth thành công!';
