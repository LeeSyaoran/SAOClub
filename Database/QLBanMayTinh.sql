      
USE master;
GO

IF EXISTS (SELECT name FROM sys.databases WHERE name = N'QLBanMayTinh')
BEGIN
    ALTER DATABASE QLBanMayTinh SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE QLBanMayTinh;
END
GO

CREATE DATABASE QLBanMayTinh
    COLLATE Vietnamese_CI_AS;
GO

USE QLBanMayTinh;
GO

-- ============================================================
--  1. THƯƠNG HIỆU & DANH MỤC & NHÀ CUNG CẤP
-- ============================================================
CREATE TABLE thuong_hieu (
    thuong_hieu_id   INT            IDENTITY(1,1) PRIMARY KEY,
    ten_thuong_hieu  NVARCHAR(100)  NOT NULL UNIQUE,
    quoc_gia          NVARCHAR(100)  NULL,
    mo_ta            NVARCHAR(500)  NULL,
    trang_thai       NVARCHAR(20)   NOT NULL DEFAULT N'active' CONSTRAINT CK_thuong_hieu_trangthai CHECK (trang_thai IN (N'active', N'inactive')),
    ngay_tao         DATETIME       NOT NULL DEFAULT GETDATE()
);

CREATE TABLE danh_muc (
    danh_muc_id   INT            IDENTITY(1,1) PRIMARY KEY,
    ten_danh_muc  NVARCHAR(100)  NOT NULL UNIQUE,
    mo_ta         NVARCHAR(500)  NULL,
    trang_thai    NVARCHAR(20)   NOT NULL DEFAULT N'active' CONSTRAINT CK_danh_muc_trangthai CHECK (trang_thai IN (N'active', N'inactive')),
    ngay_tao      DATETIME       NOT NULL DEFAULT GETDATE()
);

CREATE TABLE nha_cung_cap (
    nha_cung_cap_id   INT            IDENTITY(1,1) PRIMARY KEY,
    ten_nha_cung_cap  NVARCHAR(150)  NOT NULL,
    so_dien_thoai     VARCHAR(20)    NULL,
    email             VARCHAR(100)   NULL,
    dia_chi           NVARCHAR(255)  NULL,
    ma_so_thue        VARCHAR(20)    NULL UNIQUE,
    nguoi_lien_he     NVARCHAR(150)  NULL,
    trang_thai        NVARCHAR(20)   NOT NULL DEFAULT N'active' CONSTRAINT CK_nha_cung_cap_trangthai CHECK (trang_thai IN (N'active', N'inactive')),
    ngay_tao          DATETIME       NOT NULL DEFAULT GETDATE()
);
GO

-- ============================================================
--  2. KHÁCH HÀNG & NHÂN VIÊN
-- ============================================================
CREATE TABLE khach_hang (
    khach_hang_id  INT            IDENTITY(1,1) PRIMARY KEY,
    ho_ten         NVARCHAR(150)  NOT NULL,
    so_dien_thoai  VARCHAR(20)    NOT NULL UNIQUE,
    email          VARCHAR(100)   NULL,
    dia_chi        NVARCHAR(255)  NULL,
    loai_khach     NVARCHAR(20)   NOT NULL DEFAULT N'ca_nhan' CONSTRAINT CK_khach_hang_loai CHECK (loai_khach IN (N'ca_nhan', N'doanh_nghiep')),
    ten_cong_ty    NVARCHAR(200)  NULL,
    ma_so_thue     VARCHAR(20)    NULL,
    diem_tich_luy  INT            NOT NULL DEFAULT 0,
    trang_thai     NVARCHAR(20)   NOT NULL DEFAULT N'active' CONSTRAINT CK_khach_hang_trangthai CHECK (trang_thai IN (N'active', N'inactive', N'blocked')),
    ngay_tao       DATETIME       NOT NULL DEFAULT GETDATE()
);

CREATE TABLE chuc_vu (
    chuc_vu_id   INT            IDENTITY(1,1) PRIMARY KEY,
    ten_chuc_vu  NVARCHAR(100)  NOT NULL UNIQUE,
    mo_ta        NVARCHAR(255)  NULL
);

CREATE TABLE nhan_vien (
    nhan_vien_id   INT            IDENTITY(1,1) PRIMARY KEY,
    ho_ten         NVARCHAR(150)  NOT NULL,
    so_dien_thoai  VARCHAR(20)    NULL UNIQUE,
    email          VARCHAR(100)   NULL UNIQUE,
    chuc_vu_id     INT            NULL,
    username       VARCHAR(50)    NULL UNIQUE,
    mat_khau_hash  VARCHAR(255)   NULL,
    luong_co_ban   DECIMAL(18,0)  NULL,
    trang_thai     NVARCHAR(20)   NOT NULL DEFAULT N'active' CONSTRAINT CK_nhan_vien_trangthai CHECK (trang_thai IN (N'active', N'inactive', N'nghi_viec')),
    ngay_tao       DATETIME       NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_nhan_vien_chuc_vu FOREIGN KEY (chuc_vu_id) REFERENCES chuc_vu(chuc_vu_id)
);
GO

-- ============================================================
--  3. SẢN PHẨM GỐC (THÔNG TIN CHUNG)
-- ============================================================
CREATE TABLE san_pham (
    san_pham_id          INT             IDENTITY(1,1) PRIMARY KEY,
    ten_san_pham         NVARCHAR(200)   NOT NULL,
    thuong_hieu_id       INT             NOT NULL,
    danh_muc_id          INT             NOT NULL,
    nha_cung_cap_id      INT             NULL,
    mo_ta                NVARCHAR(MAX)   NULL,
    hinh_anh_chinh       NVARCHAR(500)   NULL,
    loai_san_pham        VARCHAR(30)     NOT NULL CONSTRAINT CK_sp_loaisanpham CHECK (loai_san_pham IN ('LAPTOP', 'DIEN_THOAI', 'PHU_KIEN')),
    trang_thai           NVARCHAR(20)    NOT NULL DEFAULT N'active' CONSTRAINT CK_san_pham_trangthai CHECK (trang_thai IN (N'active', N'inactive', N'ngung_kin_doanh')),
    ngay_tao             DATETIME        NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_san_pham_thuong_hieu  FOREIGN KEY (thuong_hieu_id)  REFERENCES thuong_hieu(thuong_hieu_id),
    CONSTRAINT FK_san_pham_danh_muc     FOREIGN KEY (danh_muc_id)     REFERENCES danh_muc(danh_muc_id),
    CONSTRAINT FK_san_pham_nha_cung_cap FOREIGN KEY (nha_cung_cap_id) REFERENCES nha_cung_cap(nha_cung_cap_id)
);
GO

-- Các danh mục thuộc tính bổ trợ
CREATE TABLE dm_cpu ( cpu_id INT IDENTITY(1,1) PRIMARY KEY, ten_cpu NVARCHAR(100) NOT NULL UNIQUE );
CREATE TABLE dm_ram ( ram_id INT IDENTITY(1,1) PRIMARY KEY, dung_luong NVARCHAR(50) NOT NULL UNIQUE );
CREATE TABLE dm_o_cung ( o_cung_id INT IDENTITY(1,1) PRIMARY KEY, loai_o_cung NVARCHAR(100) NOT NULL UNIQUE );
CREATE TABLE dm_gpu ( gpu_id INT IDENTITY(1,1) PRIMARY KEY, ten_gpu NVARCHAR(100) NOT NULL UNIQUE );
GO

-- ============================================================
--  4. BIẾN THỂ SẢN PHẨM (NƠI ĐỊNH GIÁ & CẤU HÌNH CHI TIẾT)
-- ============================================================
CREATE TABLE bien_the_san_pham (
    bien_the_id          INT             IDENTITY(1,1) PRIMARY KEY,
    san_pham_id          INT             NOT NULL,
    ma_sku               VARCHAR(50)     NOT NULL UNIQUE,
    gia_nhap             DECIMAL(18,0)   NOT NULL CONSTRAINT CK_bt_gianhap CHECK (gia_nhap >= 0),
    gia_ban              DECIMAL(18,0)   NOT NULL CONSTRAINT CK_bt_giaban CHECK (gia_ban >= 0),
    bao_hanh_thang       INT             NOT NULL DEFAULT 12,
    hinh_anh_bien_the    NVARCHAR(500)   NULL,
    trang_thai           NVARCHAR(20)    NOT NULL DEFAULT N'active' CONSTRAINT CK_bt_trangthai CHECK (trang_thai IN (N'active', N'inactive')),
    
    -- Cấu hình dùng chung/linh hoạt hoặc Nullable tùy thuộc vào loại sản phẩm
    mau_sac              NVARCHAR(50)    NULL,
    -- Thuộc tính riêng cho Laptop
    cpu_id               INT             NULL,
    ram_id               INT             NULL,
    o_cung_id            INT             NULL,
    gpu_id               INT             NULL,
    kich_thuoc_man_hinh  NVARCHAR(50)    NULL,
    he_dieu_hanh         NVARCHAR(100)   NULL,
    pin                  NVARCHAR(50)    NULL,
    trong_luong_kg       DECIMAL(5,2)    NULL,
    -- Thuộc tính riêng cho Điện thoại
    man_hinh_dt          NVARCHAR(100)   NULL,
    camera_sau           NVARCHAR(100)   NULL,
    camera_truoc         NVARCHAR(100)   NULL,
    dung_luong_pin_dt    NVARCHAR(50)    NULL,
    bo_nho_trong_dt      NVARCHAR(50)    NULL,
    chip_xu_ly_dt        NVARCHAR(100)   NULL,
    so_sim               NVARCHAR(30)    NULL,

    CONSTRAINT FK_bien_the_san_pham FOREIGN KEY (san_pham_id) REFERENCES san_pham(san_pham_id) ON DELETE CASCADE,
    CONSTRAINT FK_bien_the_cpu      FOREIGN KEY (cpu_id)      REFERENCES dm_cpu(cpu_id),
    CONSTRAINT FK_bien_the_ram      FOREIGN KEY (ram_id)      REFERENCES dm_ram(ram_id),
    CONSTRAINT FK_bien_the_ocung    FOREIGN KEY (o_cung_id)   REFERENCES dm_o_cung(o_cung_id),
    CONSTRAINT FK_bien_the_gpu      FOREIGN KEY (gpu_id)      REFERENCES dm_gpu(gpu_id)
);
GO

-- ============================================================
--  5. QUẢN LÝ THỰC THỂ KHO VÀ ĐỊNH MỨC TỒN
-- ============================================================
CREATE TABLE ton_kho (
    cau_hinh_id           INT        IDENTITY(1,1) PRIMARY KEY,
    bien_the_id           INT        NOT NULL UNIQUE,
    so_luong_ton_thuc_te  INT        NOT NULL DEFAULT 0 CONSTRAINT CK_chtk_tonthucte CHECK (so_luong_ton_thuc_te >= 0),
    so_luong_giu          INT        NOT NULL DEFAULT 0 CONSTRAINT CK_chtk_giu CHECK (so_luong_giu >= 0),
    ton_kho_toi_thieu     INT        NOT NULL DEFAULT 5 CONSTRAINT CK_chtk_toithieu CHECK (ton_kho_toi_thieu >= 0),
    ngay_cap_nhat         DATETIME   NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_ton_kho_bt FOREIGN KEY (bien_the_id) REFERENCES bien_the_san_pham(bien_the_id) ON DELETE CASCADE
);
GO

CREATE TABLE chi_tiet_san_pham (
    chi_tiet_id       INT           IDENTITY(1,1) PRIMARY KEY,
    bien_the_id       INT           NOT NULL,
    so_serial         VARCHAR(50)   NOT NULL UNIQUE,
    so_imei           VARCHAR(20)   NULL,
    trang_thai        NVARCHAR(30)  NOT NULL DEFAULT N'trong_kho'
        CONSTRAINT CK_ctsp_trangthai CHECK (trang_thai IN (N'trong_kho', N'giu_hang', N'da_ban', N'loi_bao_hanh', N'da_tra_hang')),
    ngay_nhap_kho     DATETIME      NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_ctsp_bien_the FOREIGN KEY (bien_the_id) REFERENCES bien_the_san_pham(bien_the_id) ON DELETE CASCADE
);
GO

CREATE UNIQUE INDEX UX_ctsp_imei ON chi_tiet_san_pham(so_imei) WHERE so_imei IS NOT NULL;
GO

-- ============================================================
--  6. KHUYẾN MÃI & ĐỊA CHỈ GIAO HÀNG
-- ============================================================
CREATE TABLE khuyen_mai (
    khuyen_mai_id      INT            IDENTITY(1,1) PRIMARY KEY,
    ma_khuyen_mai      VARCHAR(50)    NOT NULL UNIQUE,
    ten_khuyen_mai     NVARCHAR(150)  NOT NULL,
    loai               NVARCHAR(20)   NOT NULL CONSTRAINT CK_km_loai CHECK (loai IN (N'percent', N'fixed')),
    gia_tri            DECIMAL(18,0)  NOT NULL CONSTRAINT CK_km_giatri CHECK (gia_tri > 0),
    gia_tri_toi_da     DECIMAL(18,0)  NULL,
    don_hang_toi_thieu DECIMAL(18,0)  NULL,
    ngay_bat_dau       DATETIME       NOT NULL,
    ngay_ket_thuc      DATETIME       NOT NULL,
    so_luong_toi_da    INT            NULL,
    so_lan_da_dung     INT            NOT NULL DEFAULT 0,
    trang_thai         NVARCHAR(20)   NOT NULL DEFAULT N'active' CONSTRAINT CK_km_trangthai CHECK (trang_thai IN (N'active', N'inactive', N'het_han')),
    ngay_tao           DATETIME       NOT NULL DEFAULT GETDATE(),
    CONSTRAINT CK_km_ngay CHECK (ngay_ket_thuc > ngay_bat_dau)
);

CREATE TABLE dia_chi_giao_hang (
    dia_chi_id           INT            IDENTITY(1,1) PRIMARY KEY,
    khach_hang_id        INT            NOT NULL,
    ho_ten_nguoi_nhan    NVARCHAR(150)  NOT NULL,
    so_dien_thoai        VARCHAR(20)    NOT NULL,
    dia_chi              NVARCHAR(255)  NOT NULL,
    thanh_pho            NVARCHAR(100)  NULL,
    tinh                 NVARCHAR(100)  NULL,
    la_mac_dinh          BIT            NOT NULL DEFAULT 0,
    ngay_tao             DATETIME       NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_dcgh_khach_hang FOREIGN KEY (khach_hang_id) REFERENCES khach_hang(khach_hang_id)
);
GO

-- ============================================================
--  7. PHIẾU NHẬP KHO & ĐƠN HÀNG
-- ============================================================
CREATE TABLE phieu_nhap_kho (
    phieu_nhap_id    INT            IDENTITY(1,1) PRIMARY KEY,
    ma_phieu_nhap    VARCHAR(50)    NOT NULL UNIQUE DEFAULT UPPER(LEFT(REPLACE(CAST(NEWID() AS VARCHAR(36)), '-', ''), 12)),
    nha_cung_cap_id  INT            NOT NULL,
    nhan_vien_id     INT            NULL,
    ngay_nhap        DATETIME       NOT NULL DEFAULT GETDATE(),
    tong_tien        DECIMAL(18,0)  NOT NULL DEFAULT 0,
    trang_thai       NVARCHAR(20)   NOT NULL DEFAULT N'hoan_thanh' CONSTRAINT CK_phieu_nhap_trangthai CHECK (trang_thai IN (N'cho_duyet', N'hoan_thanh', N'huy')),
    ghi_chu          NVARCHAR(500)  NULL,
    CONSTRAINT FK_phieu_nhap_ncc       FOREIGN KEY (nha_cung_cap_id) REFERENCES nha_cung_cap(nha_cung_cap_id),
    CONSTRAINT FK_phieu_nhap_nhan_vien FOREIGN KEY (nhan_vien_id)     REFERENCES nhan_vien(nhan_vien_id)
);

CREATE TABLE chi_tiet_phieu_nhap (
    chi_tiet_nhap_id  INT            IDENTITY(1,1) PRIMARY KEY,
    phieu_nhap_id     INT            NOT NULL,
    bien_the_id       INT            NOT NULL,
    so_luong          INT            NOT NULL CONSTRAINT CK_ctpn_soluong CHECK (so_luong > 0),
    don_gia_nhap      DECIMAL(18,0)  NOT NULL CONSTRAINT CK_ctpn_dongia  CHECK (don_gia_nhap >= 0),
    thanh_tien        AS (so_luong * don_gia_nhap) PERSISTED,
    CONSTRAINT FK_ctpn_phieu_nhap FOREIGN KEY (phieu_nhap_id) REFERENCES phieu_nhap_kho(phieu_nhap_id),
    CONSTRAINT FK_ctpn_bien_the   FOREIGN KEY (bien_the_id)   REFERENCES bien_the_san_pham(bien_the_id)
);

CREATE TABLE don_hang (
    don_hang_id             INT            IDENTITY(1,1) PRIMARY KEY,
    ma_don_hang             VARCHAR(50)    NOT NULL UNIQUE DEFAULT UPPER(LEFT(REPLACE(CAST(NEWID() AS VARCHAR(36)), '-', ''), 12)),
    khach_hang_id           INT            NOT NULL,
    nhan_vien_id            INT            NULL,
    khuyen_mai_id           INT            NULL,
    dia_chi_giao_hang_id    INT            NULL,
    dia_chi_giao_hang_text  NVARCHAR(255)  NULL,
    nguoi_nhan              NVARCHAR(150)  NULL,
    sdt_nguoi_nhan          VARCHAR(20)    NULL,
    tong_tien               DECIMAL(18,0)  NOT NULL DEFAULT 0,
    giam_gia                DECIMAL(18,0)  NOT NULL DEFAULT 0,
    phi_van_chuyen          DECIMAL(18,0)  NOT NULL DEFAULT 0,
    thanh_tien              DECIMAL(18,0)  NOT NULL DEFAULT 0,
    ngay_dat                DATETIME       NOT NULL DEFAULT GETDATE(),
    ngay_giao_du_kien       DATETIME       NULL,
    ngay_giao_thuc_te       DATETIME       NULL,
    trang_thai_don_hang     NVARCHAR(30)   NOT NULL DEFAULT N'pending'
        CONSTRAINT CK_dh_trangthai CHECK (trang_thai_don_hang IN (N'pending', N'confirmed', N'processing', N'shipping', N'delivered', N'cancelled', N'returned')),
    trang_thai_thanh_toan   NVARCHAR(30)   NOT NULL DEFAULT N'unpaid'
        CONSTRAINT CK_dh_ttthanhtoan CHECK (trang_thai_thanh_toan IN (N'unpaid', N'partial', N'paid', N'refunded')),
    kenh_ban                NVARCHAR(50)   NULL,
    ghi_chu                 NVARCHAR(500)  NULL,
    CONSTRAINT FK_dh_khach_hang        FOREIGN KEY (khach_hang_id)        REFERENCES khach_hang(khach_hang_id),
    CONSTRAINT FK_dh_nhan_vien         FOREIGN KEY (nhan_vien_id)         REFERENCES nhan_vien(nhan_vien_id),
    CONSTRAINT FK_dh_khuyen_mai        FOREIGN KEY (khuyen_mai_id)        REFERENCES khuyen_mai(khuyen_mai_id),
    CONSTRAINT FK_dh_dia_chi_giao_hang FOREIGN KEY (dia_chi_giao_hang_id) REFERENCES dia_chi_giao_hang(dia_chi_id)
);
GO

-- ============================================================
--  8. LỊCH SỬ KHO & CHI TIẾT ĐƠN HÀNG
-- ============================================================
CREATE TABLE chi_tiet_don_hang (
    chi_tiet_don_hang_id  INT            IDENTITY(1,1) PRIMARY KEY,
    don_hang_id           INT            NOT NULL,
    bien_the_id           INT            NOT NULL,
    chi_tiet_id           INT            NULL,     
    so_luong              INT            NOT NULL CONSTRAINT CK_ctdh_soluong CHECK (so_luong > 0),
    don_gia               DECIMAL(18,0)  NOT NULL CONSTRAINT CK_ctdh_dongia  CHECK (don_gia >= 0),
    giam_gia_dong         DECIMAL(18,0)  NOT NULL DEFAULT 0,
    thanh_tien            AS (so_luong * don_gia - giam_gia_dong) PERSISTED,
    ghi_chu               NVARCHAR(255)  NULL,
    CONSTRAINT FK_ctdh_don_hang  FOREIGN KEY (don_hang_id) REFERENCES don_hang(don_hang_id),
    CONSTRAINT FK_ctdh_bien_the  FOREIGN KEY (bien_the_id) REFERENCES bien_the_san_pham(bien_the_id),
    CONSTRAINT FK_ctdh_ctsp      FOREIGN KEY (chi_tiet_id) REFERENCES chi_tiet_san_pham(chi_tiet_id)
);

CREATE TABLE lich_su_ton_kho (
    lich_su_id        INT            IDENTITY(1,1) PRIMARY KEY,
    bien_the_id       INT            NOT NULL,
    chi_tiet_id       INT            NULL,     
    loai_bien_dong    NVARCHAR(30)   NOT NULL CONSTRAINT CK_lsdk_loai CHECK (loai_bien_dong IN (N'nhap', N'xuat_ban', N'tra_hang', N'dieu_chinh', N'huy')),
    so_luong_thay_doi INT            NOT NULL,
    don_hang_id       INT            NULL,
    phieu_nhap_id     INT            NULL,
    nhan_vien_id      INT            NULL,
    ghi_chu           NVARCHAR(255)  NULL,
    ngay_tao          DATETIME       NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_lstk_bien_the   FOREIGN KEY (bien_the_id)   REFERENCES bien_the_san_pham(bien_the_id),
    CONSTRAINT FK_lstk_ctsp       FOREIGN KEY (chi_tiet_id)   REFERENCES chi_tiet_san_pham(chi_tiet_id),
    CONSTRAINT FK_lstk_nhan_vien  FOREIGN KEY (nhan_vien_id)  REFERENCES nhan_vien(nhan_vien_id),
    CONSTRAINT FK_lstk_don_hang   FOREIGN KEY (don_hang_id)   REFERENCES don_hang(don_hang_id),
    CONSTRAINT FK_lstk_phieu_nhap FOREIGN KEY (phieu_nhap_id) REFERENCES phieu_nhap_kho(phieu_nhap_id)
);
GO

-- ============================================================
--  9. THANH TOÁN & TRẢ HÀNG & BẢO HÀNH
-- ============================================================
CREATE TABLE thanh_toan (
    thanh_toan_id           INT            IDENTITY(1,1) PRIMARY KEY,
    don_hang_id             INT            NOT NULL,
    ngay_thanh_toan         DATETIME       NOT NULL DEFAULT GETDATE(),
    phuong_thuc_thanh_toan  NVARCHAR(50)   NOT NULL CONSTRAINT CK_tt_phuongthuc CHECK (phuong_thuc_thanh_toan IN (N'tien_mat', N'chuyen_khoan', N'the_tin_dung', N'momo', N'vnpay', N'zalopay', N'tra_gop', N'khac')),
    so_tien                 DECIMAL(18,0)  NOT NULL CONSTRAINT CK_tt_sotien CHECK (so_tien > 0),
    ma_giao_dich            VARCHAR(100)   NULL,
    trang_thai              NVARCHAR(30)   NOT NULL DEFAULT N'success' CONSTRAINT CK_tt_trangthai CHECK (trang_thai IN (N'success', N'failed', N'pending', N'refunded')),
    ghi_chu                 NVARCHAR(255)  NULL,
    CONSTRAINT FK_tt_don_hang FOREIGN KEY (don_hang_id) REFERENCES don_hang(don_hang_id)
);

CREATE TABLE phieu_tra_hang (
    phieu_tra_id  INT            IDENTITY(1,1) PRIMARY KEY,
    don_hang_id   INT            NOT NULL,
    nhan_vien_id  INT            NULL,
    ly_do         NVARCHAR(255)  NOT NULL,
    ngay_tra      DATETIME       NOT NULL DEFAULT GETDATE(),
    trang_thai    NVARCHAR(20)   NOT NULL DEFAULT N'cho_xu_ly' CONSTRAINT CK_pth_trangthai CHECK (trang_thai IN (N'cho_xu_ly', N'da_xu_ly', N'tu_choi')),
    so_tien_hoan  DECIMAL(18,0)  NOT NULL DEFAULT 0,
    ghi_chu       NVARCHAR(500)  NULL,
    CONSTRAINT FK_pth_don_hang  FOREIGN KEY (don_hang_id)  REFERENCES don_hang(don_hang_id),
    CONSTRAINT FK_pth_nhan_vien FOREIGN KEY (nhan_vien_id) REFERENCES nhan_vien(nhan_vien_id)
);

CREATE TABLE chi_tiet_tra_hang (
    chi_tiet_tra_id  INT            IDENTITY(1,1) PRIMARY KEY,
    phieu_tra_id     INT            NOT NULL,
    bien_the_id      INT            NOT NULL,
    chi_tiet_id      INT            NULL,     
    so_luong         INT            NOT NULL CONSTRAINT CK_ctth_soluong CHECK (so_luong > 0),
    don_gia_hoan     DECIMAL(18,0)  NOT NULL,
    tinh_trang       NVARCHAR(50)   NULL,
    CONSTRAINT FK_ctth_phieu_tra FOREIGN KEY (phieu_tra_id) REFERENCES phieu_tra_hang(phieu_tra_id),
    CONSTRAINT FK_ctth_bien_the  FOREIGN KEY (bien_the_id)  REFERENCES bien_the_san_pham(bien_the_id),
    CONSTRAINT FK_ctth_ctsp      FOREIGN KEY (chi_tiet_id)  REFERENCES chi_tiet_san_pham(chi_tiet_id)
);

CREATE TABLE phieu_bao_hang (
    bao_hanh_id       INT            IDENTITY(1,1) PRIMARY KEY,
    don_hang_id       INT            NOT NULL,
    bien_the_id       INT            NOT NULL,
    khach_hang_id     INT            NOT NULL,
    chi_tiet_id       INT            NULL,     
    ngay_mua          DATETIME       NOT NULL,
    ngay_het_bh       DATETIME       NOT NULL,
    ngay_tiep_nhan    DATETIME       NULL,
    ngay_tra_khach    DATETIME       NULL,
    mo_ta_loi         NVARCHAR(500)  NULL,
    ket_qua_xu_ly     NVARCHAR(500)  NULL,
    trang_thai        NVARCHAR(30)   NOT NULL DEFAULT N'con_bao_hanh' CONSTRAINT CK_pbh_trangthai CHECK (trang_thai IN (N'con_bao_hanh', N'dang_xu_ly', N'da_xu_ly', N'het_bao_hanh', N'tu_choi')),
    chi_phi_phat_sinh DECIMAL(18,0)  NOT NULL DEFAULT 0,
    ghi_chu           NVARCHAR(500)  NULL,
    CONSTRAINT FK_pbh_don_hang   FOREIGN KEY (don_hang_id)   REFERENCES don_hang(don_hang_id),
    CONSTRAINT FK_pbh_bien_the   FOREIGN KEY (bien_the_id)   REFERENCES bien_the_san_pham(bien_the_id),
    CONSTRAINT FK_pbh_khach_hang FOREIGN KEY (khach_hang_id) REFERENCES khach_hang(khach_hang_id),
    CONSTRAINT FK_pbh_ctsp       FOREIGN KEY (chi_tiet_id)   REFERENCES chi_tiet_san_pham(chi_tiet_id)
);
GO

-- ============================================================
--  10. TRIGGERS TỰ ĐỘNG ĐỒNG BỘ SỐ LƯỢNG KHO THỰC TẾ (REAL-TIME)
-- ============================================================
CREATE TRIGGER trg_CapNhatTonKhoThucTe
ON chi_tiet_san_pham
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @TmpTable TABLE (bien_the_id INT, bien_dong INT);

    -- Thêm máy mới vào kho
    INSERT INTO @TmpTable
    SELECT bien_the_id, COUNT(*) 
    FROM inserted 
    WHERE trang_thai = N'trong_kho'
    GROUP BY bien_the_id;

    -- Cập nhật trạng thái máy
    IF EXISTS (SELECT 1 FROM deleted) AND EXISTS (SELECT 1 FROM inserted)
    BEGIN
        -- Giảm tồn kho thực tế
        INSERT INTO @TmpTable
        SELECT d.bien_the_id, -COUNT(*)
        FROM deleted d
        JOIN inserted i ON d.chi_tiet_id = i.chi_tiet_id
        WHERE d.trang_thai = N'trong_kho' AND i.trang_thai <> N'trong_kho'
        GROUP BY d.bien_the_id;

        -- Tăng tồn kho thực tế
        INSERT INTO @TmpTable
        SELECT i.bien_the_id, COUNT(*)
        FROM deleted d
        JOIN inserted i ON d.chi_tiet_id = i.chi_tiet_id
        WHERE d.trang_thai <> N'trong_kho' AND i.trang_thai = N'trong_kho'
        GROUP BY i.bien_the_id;
    END

    -- Xóa bản ghi máy vật lý
    IF EXISTS (SELECT 1 FROM deleted) AND NOT EXISTS (SELECT 1 FROM inserted)
    BEGIN
        INSERT INTO @TmpTable
        SELECT bien_the_id, -COUNT(*)
        FROM deleted
        WHERE trang_thai = N'trong_kho'
        GROUP BY bien_the_id;
    END

    -- Cập nhật bảng cấu hình định mức tồn kho
    IF EXISTS (SELECT 1 FROM @TmpTable)
    BEGIN
        UPDATE tk
        SET tk.so_luong_ton_thuc_te = tk.so_luong_ton_thuc_te + t.bien_dong,
            tk.ngay_cap_nhat = GETDATE()
        FROM ton_kho tk
        JOIN (SELECT bien_the_id, SUM(bien_dong) AS bien_dong FROM @TmpTable GROUP BY bien_the_id) t 
        ON tk.bien_the_id = t.bien_the_id;
    END
END;
GO

-- ============================================================
--  11. INDEX TỐI ƯU HÓA TRUY VẤN TỐC ĐỘ CAO
-- ============================================================
CREATE INDEX IX_bien_the_san_pham   ON bien_the_san_pham(san_pham_id);
CREATE INDEX IX_bien_the_gia        ON bien_the_san_pham(gia_ban, gia_nhap);
CREATE INDEX IX_ctsp_bien_the       ON chi_tiet_san_pham(bien_the_id, trang_thai);
CREATE INDEX IX_don_hang_khach_hang ON don_hang(khach_hang_id);
CREATE INDEX IX_don_hang_ngay_dat   ON don_hang(ngay_dat DESC);
CREATE INDEX IX_ctdh_don_hang       ON chi_tiet_don_hang(don_hang_id);
CREATE INDEX IX_lstk_bien_the       ON lich_su_ton_kho(bien_the_id, ngay_tao DESC);
GO

-- ============================================================
--  12. VIEWS TỔNG HỢP SIÊU NHANH
-- ============================================================
CREATE VIEW vw_ton_kho_tong_quan AS
SELECT
    sp.san_pham_id,
    bt.bien_the_id,
    bt.ma_sku,
    CONCAT(sp.ten_san_pham, ' (', bt.mau_sac, ')') AS ten_phien_ban,
    sp.loai_san_pham,
    th.ten_thuong_hieu,
    dm.ten_danh_muc,
    tk.so_luong_ton_thuc_te, 
    tk.so_luong_giu,
    (tk.so_luong_ton_thuc_te - tk.so_luong_giu) AS co_the_ban,
    tk.ton_kho_toi_thieu,
    CASE
        WHEN tk.so_luong_ton_thuc_te = 0 THEN N'Hết hàng'
        WHEN tk.so_luong_ton_thuc_te <= tk.ton_kho_toi_thieu THEN N'Sắp hết hàng'
        ELSE N'Sẵn hàng'
    END AS tinh_trang_kho,
    bt.gia_ban,
    bt.gia_nhap
FROM bien_the_san_pham bt
JOIN san_pham sp         ON bt.san_pham_id = sp.san_pham_id
JOIN ton_kho tk ON bt.bien_the_id = tk.bien_the_id
JOIN thuong_hieu th      ON sp.thuong_hieu_id = th.thuong_hieu_id
JOIN danh_muc dm         ON sp.danh_muc_id = dm.danh_muc_id
WHERE bt.trang_thai = N'active';
GO

-- ============================================================
--  13. BỘ DỮ LIỆU MẪU CHUẨN ĐỂ TEST HỆ THỐNG
-- ============================================================
-- Chèn chức vụ & Nhân viên mẫu
INSERT INTO chuc_vu (ten_chuc_vu) VALUES (N'Quản lý'), (N'Nhân viên bán hàng'), (N'Thủ kho');
INSERT INTO nhan_vien (ho_ten, so_dien_thoai, email, chuc_vu_id, username, mat_khau_hash, luong_co_ban) 
VALUES (N'Nguyễn Văn A', '0987654321', 'nhanviena@gmail.com', 2, 'nhanviena', 'hash_password_here', 8000000);

INSERT INTO thuong_hieu (ten_thuong_hieu, quoc_gia) VALUES (N'Dell', N'Mỹ'), (N'Apple', N'Mỹ');
INSERT INTO danh_muc (ten_danh_muc) VALUES (N'Laptop'), (N'Điện thoại');
INSERT INTO nha_cung_cap (ten_nha_cung_cap) VALUES (N'Nhà phân phối Digiworld');
INSERT INTO khach_hang (ho_ten, so_dien_thoai) VALUES (N'Nghiêm Việt Anh', '0912345678');

-- Tạo thuộc tính phụ trợ Laptop
INSERT INTO dm_cpu (ten_cpu) VALUES (N'Intel Core i5-1235U'), (N'Intel Core i7-13620H');
INSERT INTO dm_ram (dung_luong) VALUES (N'8GB DDR4'), (N'16GB DDR5');
INSERT INTO dm_o_cung (loai_o_cung) VALUES (N'512GB SSD');
INSERT INTO dm_gpu (ten_gpu) VALUES (N'Intel Iris Xe'), (N'NVIDIA RTX 4060');

-- Chèn Sản Phẩm Gốc
INSERT INTO san_pham (ten_san_pham, thuong_hieu_id, danh_muc_id, loai_san_pham) VALUES
    (N'Dell Inspiron 15 3520', 1, 1, 'LAPTOP'),
    (N'iPhone 15 Pro Max',     2, 2, 'DIEN_THOAI');

-- Chèn các cấu hình phiên bản (Biến thể)
INSERT INTO bien_the_san_pham (san_pham_id, ma_sku, gia_nhap, gia_ban, cpu_id, ram_id, o_cung_id, gpu_id, mau_sac)
VALUES (1, 'DELL-3520-8GB', 13000000, 15500000, 1, 1, 1, 1, N'Xám');

INSERT INTO bien_the_san_pham (san_pham_id, ma_sku, gia_nhap, gia_ban, cpu_id, ram_id, o_cung_id, gpu_id, mau_sac)
VALUES (1, 'DELL-3520-16GB', 15000000, 17900000, 2, 2, 1, 2, N'Đen');

INSERT INTO bien_the_san_pham (san_pham_id, ma_sku, gia_nhap, gia_ban, bo_nho_trong_dt, chip_xu_ly_dt, mau_sac)
VALUES (2, 'IP15PM-256GB', 28000000, 32500000, '256GB', 'Apple A17 Pro', N'Titan Tự Nhiên');

-- Khởi tạo cấu hình định mức tồn kho (bắt buộc trước khi chèn serial)
INSERT INTO ton_kho (bien_the_id, so_luong_ton_thuc_te, so_luong_giu, ton_kho_toi_thieu) VALUES
    (1, 0, 0, 5), (2, 0, 0, 3), (3, 0, 0, 4);

-- Chèn máy vật lý có số Serial cụ thể (Kích hoạt Trigger tự cộng dồn)
INSERT INTO chi_tiet_san_pham (bien_the_id, so_serial, so_imei, trang_thai) VALUES
    (1, 'SER-DELL8-001', NULL, N'trong_kho'),
    (1, 'SER-DELL8-002', NULL, N'trong_kho'),
    (2, 'SER-DELL16-001', NULL, N'trong_kho'),
    (3, 'SER-IP15PM-001', 'IMEI-999888777666', N'trong_kho');
GO

    