-- ============================================================
--  MIGRATION 002: Tự động tạo bản ghi tồn kho khi thêm biến thể
-- ============================================================
-- Trigger tạo bản ghi tồn kho mặc định cho biến thể mới

IF OBJECT_ID('trg_BienThe_TaoTonKho', 'TR') IS NOT NULL
    DROP TRIGGER trg_BienThe_TaoTonKho;
GO

CREATE TRIGGER trg_BienThe_TaoTonKho
ON bien_the_san_pham
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO ton_kho (bien_the_id, so_luong_ton_thuc_te, so_luong_giu, ton_kho_toi_thieu, ngay_tao, ngay_cap_nhat)
    SELECT i.bien_the_id, 0, 0, 5, GETDATE(), GETDATE()
    FROM inserted i
    WHERE NOT EXISTS (
        SELECT 1 FROM ton_kho tk WHERE tk.bien_the_id = i.bien_the_id
    );
END;
GO

-- Backfill: Tạo bản ghi tồn kho cho các biến thể hiện có nhưng chưa có trong ton_kho
INSERT INTO ton_kho (bien_the_id, so_luong_ton_thuc_te, so_luong_giu, ton_kho_toi_thieu, ngay_tao, ngay_cap_nhat)
SELECT bt.bien_the_id, 0, 0, 5, GETDATE(), GETDATE()
FROM bien_the_san_pham bt
WHERE NOT EXISTS (
    SELECT 1 FROM ton_kho tk WHERE tk.bien_the_id = bt.bien_the_id
);
GO
