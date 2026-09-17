-- Migration script: Thêm các cột còn thiếu trong database QLBanMayTinh
-- Khớp với các entity trong Spring Boot Backend

USE QLBanMayTinh;
GO

-- 1. Table: chi_tiet_san_pham
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('chi_tiet_san_pham') AND name = 'locked_by')
BEGIN
    ALTER TABLE chi_tiet_san_pham ADD locked_by INT NULL;
    PRINT 'Added locked_by to chi_tiet_san_pham';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('chi_tiet_san_pham') AND name = 'locked_at')
BEGIN
    ALTER TABLE chi_tiet_san_pham ADD locked_at DATETIME NULL;
    PRINT 'Added locked_at to chi_tiet_san_pham';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('chi_tiet_san_pham') AND name = 'lock_session')
BEGIN
    ALTER TABLE chi_tiet_san_pham ADD lock_session VARCHAR(64) NULL;
    PRINT 'Added lock_session to chi_tiet_san_pham';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_serial_lock' AND object_id = OBJECT_ID('chi_tiet_san_pham'))
BEGIN
    CREATE INDEX idx_serial_lock ON chi_tiet_san_pham(locked_at, lock_session);
    PRINT 'Created index idx_serial_lock';
END
GO

-- 2. Table: nha_cung_cap
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('nha_cung_cap') AND name = 'hinh_anh')
BEGIN
    ALTER TABLE nha_cung_cap ADD hinh_anh NVARCHAR(500) NULL;
    PRINT 'Added hinh_anh to nha_cung_cap';
END
GO

-- 3. Table: khach_hang
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('khach_hang') AND name = 'hinh_anh')
BEGIN
    ALTER TABLE khach_hang ADD hinh_anh NVARCHAR(500) NULL;
    PRINT 'Added hinh_anh to khach_hang';
END
GO

-- 4. Table: phieu_bao_hanh
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('phieu_bao_hanh') AND name = 'ngay_bat_dau_xu_ly')
BEGIN
    ALTER TABLE phieu_bao_hanh ADD ngay_bat_dau_xu_ly DATETIME NULL;
    PRINT 'Added ngay_bat_dau_xu_ly to phieu_bao_hanh';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('phieu_bao_hanh') AND name = 'phuong_thuc')
BEGIN
    ALTER TABLE phieu_bao_hanh ADD phuong_thuc NVARCHAR(30) NULL;
    PRINT 'Added phuong_thuc to phieu_bao_hanh';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('phieu_bao_hanh') AND name = 'dia_chi_lay_hang')
BEGIN
    ALTER TABLE phieu_bao_hanh ADD dia_chi_lay_hang NVARCHAR(500) NULL;
    PRINT 'Added dia_chi_lay_hang to phieu_bao_hanh';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('phieu_bao_hanh') AND name = 'ly_do_tu_choi')
BEGIN
    ALTER TABLE phieu_bao_hanh ADD ly_do_tu_choi NVARCHAR(500) NULL;
    PRINT 'Added ly_do_tu_choi to phieu_bao_hanh';
END
GO
