-- ============================================================
--  MIGRATION 003: Cập nhật Trigger tự động tính Tồn thực tế và Giữ hàng
-- ============================================================
-- Trigger cập nhật số lượng tồn thực tế và giữ hàng khi serial thay đổi

IF OBJECT_ID('trg_CapNhatTonKhoThucTe', 'TR') IS NOT NULL
    DROP TRIGGER trg_CapNhatTonKhoThucTe;
GO

CREATE TRIGGER trg_CapNhatTonKhoThucTe
ON chi_tiet_san_pham
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    -- Bảng tạm gom biến động theo bien_the_id
    DECLARE @TmpTable TABLE (
        bien_the_id   INT NOT NULL,
        bien_dong_ton INT NOT NULL,
        bien_dong_giu INT NOT NULL
    );

    -- 1) Dòng bị xóa hoặc giá trị cũ trước khi update (dấu âm -)
    INSERT INTO @TmpTable (bien_the_id, bien_dong_ton, bien_dong_giu)
    SELECT
        d.bien_the_id,
        -SUM(CASE WHEN d.trang_thai IN (N'trong_kho', N'giu_hang') THEN 1 ELSE 0 END),
        -SUM(CASE WHEN d.trang_thai = N'giu_hang' THEN 1 ELSE 0 END)
    FROM deleted d
    GROUP BY d.bien_the_id;

    -- 2) Dòng mới được thêm hoặc giá trị mới sau khi update (dấu dương +)
    INSERT INTO @TmpTable (bien_the_id, bien_dong_ton, bien_dong_giu)
    SELECT
        i.bien_the_id,
        SUM(CASE WHEN i.trang_thai IN (N'trong_kho', N'giu_hang') THEN 1 ELSE 0 END),
        SUM(CASE WHEN i.trang_thai = N'giu_hang' THEN 1 ELSE 0 END)
    FROM inserted i
    GROUP BY i.bien_the_id;

    -- 3) Cập nhật ton_kho nếu có biến động thực sự
    IF EXISTS (SELECT 1 FROM @TmpTable)
    BEGIN
        UPDATE tk
        SET tk.so_luong_ton_thuc_te = CASE
                WHEN tk.so_luong_ton_thuc_te + t.tong_bien_dong_ton < 0 THEN 0
                ELSE tk.so_luong_ton_thuc_te + t.tong_bien_dong_ton
            END,
            tk.so_luong_giu = CASE
                WHEN tk.so_luong_giu + t.tong_bien_dong_giu < 0 THEN 0
                ELSE tk.so_luong_giu + t.tong_bien_dong_giu
            END,
            tk.ngay_cap_nhat = GETDATE()
        FROM ton_kho tk
        JOIN (
            SELECT
                bien_the_id,
                SUM(bien_dong_ton) AS tong_bien_dong_ton,
                SUM(bien_dong_giu) AS tong_bien_dong_giu
            FROM @TmpTable
            GROUP BY bien_the_id
            HAVING SUM(bien_dong_ton) <> 0 OR SUM(bien_dong_giu) <> 0
        ) t ON tk.bien_the_id = t.bien_the_id;
    END
END;
GO

-- Đồng bộ lại toàn bộ dữ liệu baseline trong ton_kho khớp với chi_tiet_san_pham
UPDATE tk
SET tk.so_luong_ton_thuc_te = ISNULL(tinh_lai.ton_thuc_te, 0),
    tk.so_luong_giu         = ISNULL(tinh_lai.giu_hang, 0),
    tk.ngay_cap_nhat        = GETDATE()
FROM ton_kho tk
LEFT JOIN (
    SELECT
        bien_the_id,
        SUM(CASE WHEN trang_thai IN (N'trong_kho', N'giu_hang') THEN 1 ELSE 0 END) AS ton_thuc_te,
        SUM(CASE WHEN trang_thai = N'giu_hang' THEN 1 ELSE 0 END) AS giu_hang
    FROM chi_tiet_san_pham
    GROUP BY bien_the_id
) tinh_lai ON tk.bien_the_id = tinh_lai.bien_the_id;
GO
