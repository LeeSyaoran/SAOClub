-- Thêm bảng liên kết khuyến mãi - sản phẩm

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'khuyen_mai_san_pham')
BEGIN
    CREATE TABLE khuyen_mai_san_pham (
        khuyen_mai_san_pham_id  INT  IDENTITY(1,1) PRIMARY KEY,
        khuyen_mai_id           INT  NOT NULL,
        san_pham_id             INT  NOT NULL,
        CONSTRAINT FK_kmsp_khuyen_mai FOREIGN KEY (khuyen_mai_id) REFERENCES khuyen_mai(khuyen_mai_id) ON DELETE CASCADE,
        CONSTRAINT FK_kmsp_san_pham    FOREIGN KEY (san_pham_id)    REFERENCES san_pham(san_pham_id)    ON DELETE CASCADE,
        CONSTRAINT UQ_kmsp_km_sp       UNIQUE (khuyen_mai_id, san_pham_id)
    );
END
GO
