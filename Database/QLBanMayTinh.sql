      
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

-- ============================================================
--  14. DU LIEU MAU MO RONG
-- ============================================================

-- Thuong hieu bo sung
-- (Dell=1, Apple=2 da co)
INSERT INTO thuong_hieu (ten_thuong_hieu, quoc_gia, mo_ta, trang_thai) VALUES
(N'Asus',    N'Dai Loan',    N'Hang may tinh va thiet bi dien tu Dai Loan', N'active'),
(N'Lenovo',  N'Trung Quoc',  N'Hang may tinh hang dau the gioi',            N'active'),
(N'HP',      N'My',          N'Hewlett-Packard - May tinh va thiet bi in',   N'active'),
(N'Samsung', N'Han Quoc',    N'Tap doan dien tu da quoc gia Han Quoc',       N'active'),
(N'MSI',     N'Dai Loan',    N'Micro-Star International - Chuyen laptop gaming', N'active'),
(N'Xiaomi',  N'Trung Quoc',  N'Hang dien thoai va thiet bi thong minh',     N'active');
-- thuong_hieu: Asus=3, Lenovo=4, HP=5, Samsung=6, MSI=7, Xiaomi=8
GO

-- Danh muc bo sung
-- (Laptop=1, Dien_thoai=2 da co)
INSERT INTO danh_muc (ten_danh_muc, mo_ta, trang_thai) VALUES
(N'Phu kien', N'Tai nghe, chuot, ban phim, sac, cap...', N'active');
-- danh_muc: Phu_kien=3
GO

-- Nha cung cap bo sung
-- (Digiworld=1 da co)
INSERT INTO nha_cung_cap (ten_nha_cung_cap, so_dien_thoai, email, dia_chi, nguoi_lien_he, ma_so_thue, trang_thai) VALUES
(N'FPT Trading',      N'02437820800', N'trading@fpt.com.vn',         N'Toa nha FPT, Ha Noi',              N'Nguyen Hung Cuong', N'0101248141',   N'active'),
(N'The Gioi Di Dong', N'1800 1060',   N'supplier@thegioididong.com', N'128 Tran Quang Khai, Quan 1, HCM', N'Le Thi Binh',       N'0303686244',   N'active');
-- nha_cung_cap: FPT=2, TGDD=3
GO

-- Chuc vu bo sung
-- (Quan ly=1, NVBH=2, Thu kho=3 da co)
INSERT INTO chuc_vu (ten_chuc_vu, mo_ta) VALUES
(N'Ky thuat vien', N'Bao hanh va sua chua thiet bi'),
(N'Ke toan',       N'Quan ly tai chinh, ke toan noi bo');
-- chuc_vu: KTV=4, KeToan=5
GO

-- Nhan vien bo sung
-- (Nguyen Van A chuc_vu_id=2 da co, id=1)
INSERT INTO nhan_vien (ho_ten, so_dien_thoai, email, chuc_vu_id, username, mat_khau_hash, luong_co_ban, trang_thai) VALUES
(N'Tran Thi Bao',   N'0978112233', N'nhanvienb@sao.vn', 2, N'nhanvienb', N'hash_nv_bao',   7500000, N'active'),
(N'Le Van Cuong',   N'0967223344', N'nhanvienc@sao.vn', 3, N'nhanvienc', N'hash_nv_cuong', 6500000, N'active'),
(N'Pham Quoc Dung', N'0956334455', N'nhanviend@sao.vn', 4, N'nhanviend', N'hash_nv_dung',  9000000, N'active');
-- nhan_vien: Bao=2, Cuong=3, Dung=4
GO

-- Khach hang bo sung
-- (Nghiem Viet Anh=1 da co)
INSERT INTO khach_hang (ho_ten, so_dien_thoai, email, dia_chi, loai_khach, diem_tich_luy, trang_thai) VALUES
(N'Tran Thi Binh',       N'0901234567',   N'binh.tran@gmail.com',  N'456 Nguyen Trai, Quan 5, TP.HCM',    N'ca_nhan',      100, N'active'),
(N'Le Hoang Cuong',      N'0912345000',   N'cuong.le@gmail.com',   N'78 Dinh Tien Hoang, Quan 1, TP.HCM', N'ca_nhan',      250, N'active'),
(N'Pham Thi Duyen',      N'0934567890',   N'duyen.pham@gmail.com', N'12 Tran Phu, Hai Chau, Da Nang',     N'ca_nhan',       50, N'active'),
(N'Nguyen Minh Duc',     N'0956789012',   N'duc.nguyen@gmail.com', N'34 Hoang Dieu, Hai Chau, Da Nang',   N'ca_nhan',        0, N'active'),
(N'Cty Minh Anh Tech',   N'02838901234',  N'purchase@minhanh.vn',  N'50 Le Loi, Quan 1, TP.HCM',          N'doanh_nghiep', 800, N'active');
-- khach_hang: Binh=2, Cuong=3, Duyen=4, Duc=5, MinhAnh=6
GO

-- Thuoc tinh phu tro bo sung
-- dm_cpu: i5-1235U=1, i7-13620H=2 da co
INSERT INTO dm_cpu (ten_cpu) VALUES
(N'Intel Core i5-13420H'),
(N'Intel Core i9-13900H'),
(N'AMD Ryzen 5 7530U'),
(N'AMD Ryzen 7 7745H'),
(N'Intel Core i7-13700H');
-- dm_cpu: i5-13420H=3, i9-13900H=4, Ryzen5=5, Ryzen7=6, i7-13700H=7

-- dm_ram: 8GB_DDR4=1, 16GB_DDR5=2 da co
INSERT INTO dm_ram (dung_luong) VALUES
(N'32GB DDR5'),
(N'8GB LPDDR5'),
(N'16GB LPDDR5');
-- dm_ram: 32GB=3, 8GB_LPDDR5=4, 16GB_LPDDR5=5

-- dm_o_cung: 512GB_SSD=1 da co
INSERT INTO dm_o_cung (loai_o_cung) VALUES
(N'256GB SSD'),
(N'1TB SSD'),
(N'2TB SSD');
-- dm_o_cung: 256GB=2, 1TB=3, 2TB=4

-- dm_gpu: Iris_Xe=1, RTX4060=2 da co
INSERT INTO dm_gpu (ten_gpu) VALUES
(N'NVIDIA RTX 4050'),
(N'NVIDIA RTX 4070'),
(N'AMD Radeon 780M');
-- dm_gpu: RTX4050=3, RTX4070=4, Radeon780M=5
GO

-- San pham bo sung
-- (Dell_Inspiron=1, iPhone15PM=2 da co)
-- thuong_hieu: Dell=1,Apple=2,Asus=3,Lenovo=4,HP=5,Samsung=6,MSI=7,Xiaomi=8
-- danh_muc: Laptop=1, Dien_thoai=2, Phu_kien=3
-- nha_cung_cap: Digiworld=1, FPT=2, TGDD=3
INSERT INTO san_pham (ten_san_pham, thuong_hieu_id, danh_muc_id, nha_cung_cap_id, loai_san_pham, mo_ta, trang_thai) VALUES
(N'Asus Vivobook 15 X1504VA', 3, 1, 2, 'LAPTOP',     N'Laptop van phong mong nhe, man hinh 15.6" FullHD 60Hz, pin 50Wh ben bi ca ngay', N'active'),
(N'Lenovo IdeaPad 5 Pro 16',  4, 1, 2, 'LAPTOP',     N'Man hinh 2.5K 16" 120Hz, hieu nang AMD Ryzen manh me, vo nhom cao cap',          N'active'),
(N'HP Envy x360 16 2024',     5, 1, 1, 'LAPTOP',     N'Laptop 2-in-1 cao cap, man hinh OLED cam ung 2.8K, chip Intel the he 13',         N'active'),
(N'MSI Stealth 15M B12U',     7, 1, 1, 'LAPTOP',     N'Laptop gaming mong nhe, RTX 4050, man hinh 144Hz, than may 1.7kg',                N'active'),
(N'Samsung Galaxy S24 Ultra', 6, 2, 3, 'DIEN_THOAI', N'Flagship AI voi but S-Pen tich hop, camera 200MP, pin 5000mAh, Snapdragon 8 Gen 3', N'active'),
(N'Xiaomi 14 Ultra',          8, 2, 3, 'DIEN_THOAI', N'Camera Leica dinh cao 4 ong kinh 50MP, chip Snapdragon 8 Gen 3, sac nhanh 90W',   N'active');
-- san_pham: Asus=3, Lenovo=4, HP=5, MSI=6, S24Ultra=7, Xiaomi14Ultra=8
GO

-- Bien the san pham bo sung
-- (Dell8GB=1, Dell16GB=2, iPhone256GB=3 da co)
-- cpu: i5-1235U=1,i7-13620H=2,i5-13420H=3,i9-13900H=4,Ryzen5=5,Ryzen7=6,i7-13700H=7
-- ram: 8GB_DDR4=1,16GB_DDR5=2,32GB_DDR5=3,8GB_LPDDR5=4,16GB_LPDDR5=5
-- ocung: 512GB=1,256GB=2,1TB=3,2TB=4
-- gpu: IrisXe=1,RTX4060=2,RTX4050=3,RTX4070=4,Radeon780M=5

-- Asus Vivobook 15 (san_pham_id=3)
INSERT INTO bien_the_san_pham (san_pham_id, ma_sku, gia_nhap, gia_ban, bao_hanh_thang, cpu_id, ram_id, o_cung_id, gpu_id, kich_thuoc_man_hinh, he_dieu_hanh, pin, trong_luong_kg, mau_sac) VALUES
(3, 'ASUS-X1504-I5-8G',  12500000, 14990000, 24, 3, 1, 1, 5, N'15.6" FHD 60Hz', N'Windows 11 Home', N'50Wh', 1.70, N'Bac'),
(3, 'ASUS-X1504-I7-16G', 16000000, 19490000, 24, 7, 2, 1, 5, N'15.6" FHD 60Hz', N'Windows 11 Home', N'50Wh', 1.70, N'Bac');
-- bien_the: Asus_i5=4, Asus_i7=5

-- Lenovo IdeaPad 5 Pro 16 (san_pham_id=4)
INSERT INTO bien_the_san_pham (san_pham_id, ma_sku, gia_nhap, gia_ban, bao_hanh_thang, cpu_id, ram_id, o_cung_id, gpu_id, kich_thuoc_man_hinh, he_dieu_hanh, pin, trong_luong_kg, mau_sac) VALUES
(4, 'LENO-IP5P-R5-8G',  14500000, 17490000, 24, 5, 1, 1, 5, N'16" 2.5K 120Hz', N'Windows 11 Home', N'75Wh', 1.85, N'Xam Bao'),
(4, 'LENO-IP5P-R7-16G', 18000000, 22490000, 24, 6, 2, 3, 5, N'16" 2.5K 120Hz', N'Windows 11 Home', N'75Wh', 1.85, N'Xam Bao');
-- bien_the: Lenovo_R5=6, Lenovo_R7=7

-- HP Envy x360 16 (san_pham_id=5)
INSERT INTO bien_the_san_pham (san_pham_id, ma_sku, gia_nhap, gia_ban, bao_hanh_thang, cpu_id, ram_id, o_cung_id, gpu_id, kich_thuoc_man_hinh, he_dieu_hanh, pin, trong_luong_kg, mau_sac) VALUES
(5, 'HP-ENVY-I7-16G', 22000000, 27490000, 24, 7, 2, 1, 3, N'16" 2.8K OLED 120Hz', N'Windows 11 Home', N'86Wh', 2.10, N'Bac Tu Nhien'),
(5, 'HP-ENVY-I9-32G', 28000000, 34990000, 24, 4, 3, 3, 4, N'16" 2.8K OLED 120Hz', N'Windows 11 Home', N'86Wh', 2.10, N'Bac Tu Nhien');
-- bien_the: HP_i7=8, HP_i9=9

-- MSI Stealth 15M (san_pham_id=6)
INSERT INTO bien_the_san_pham (san_pham_id, ma_sku, gia_nhap, gia_ban, bao_hanh_thang, cpu_id, ram_id, o_cung_id, gpu_id, kich_thuoc_man_hinh, he_dieu_hanh, pin, trong_luong_kg, mau_sac) VALUES
(6, 'MSI-STL15-RTX4050', 22500000, 27990000, 24, 7, 2, 1, 3, N'15.6" FHD 144Hz', N'Windows 11 Home', N'52Wh', 1.70, N'Den'),
(6, 'MSI-STL15-RTX4070', 30000000, 37490000, 24, 7, 2, 3, 4, N'15.6" QHD 240Hz', N'Windows 11 Home', N'52Wh', 1.70, N'Den');
-- bien_the: MSI_RTX4050=10, MSI_RTX4070=11

-- Samsung Galaxy S24 Ultra (san_pham_id=7)
INSERT INTO bien_the_san_pham (san_pham_id, ma_sku, gia_nhap, gia_ban, bao_hanh_thang, mau_sac, bo_nho_trong_dt, chip_xu_ly_dt, camera_sau, camera_truoc, dung_luong_pin_dt, so_sim) VALUES
(7, 'S24U-256-BLACK',  25000000, 29990000, 12, N'Titan Den', N'256GB', N'Snapdragon 8 Gen 3', N'200MP+50MP+10MP+12MP', N'12MP', N'5000mAh', N'2 Nano SIM'),
(7, 'S24U-512-VIOLET', 27500000, 32990000, 12, N'Titan Tim', N'512GB', N'Snapdragon 8 Gen 3', N'200MP+50MP+10MP+12MP', N'12MP', N'5000mAh', N'2 Nano SIM'),
(7, 'S24U-1TB-GRAY',   32000000, 38990000, 12, N'Titan Xam', N'1TB',  N'Snapdragon 8 Gen 3', N'200MP+50MP+10MP+12MP', N'12MP', N'5000mAh', N'2 Nano SIM');
-- bien_the: S24U_256=12, S24U_512=13, S24U_1TB=14

-- Xiaomi 14 Ultra (san_pham_id=8)
INSERT INTO bien_the_san_pham (san_pham_id, ma_sku, gia_nhap, gia_ban, bao_hanh_thang, mau_sac, bo_nho_trong_dt, chip_xu_ly_dt, camera_sau, camera_truoc, dung_luong_pin_dt, so_sim) VALUES
(8, 'XIAO14U-256-BLK', 22000000, 27990000, 12, N'Den',  N'256GB', N'Snapdragon 8 Gen 3', N'50MP Leica x4', N'32MP', N'5000mAh', N'2 Nano SIM'),
(8, 'XIAO14U-512-WHT', 25000000, 31990000, 12, N'Trang', N'512GB', N'Snapdragon 8 Gen 3', N'50MP Leica x4', N'32MP', N'5000mAh', N'2 Nano SIM');
-- bien_the: Xiaomi256=15, Xiaomi512=16
GO

-- Ton kho cho cac bien the moi (khoi tao truc tiep)
INSERT INTO ton_kho (bien_the_id, so_luong_ton_thuc_te, so_luong_giu, ton_kho_toi_thieu) VALUES
( 4, 10, 1, 5),
( 5,  7, 0, 3),
( 6, 12, 2, 5),
( 7,  5, 0, 3),
( 8,  8, 1, 4),
( 9,  3, 0, 2),
(10,  6, 1, 3),
(11,  4, 0, 2),
(12, 15, 2, 5),
(13,  8, 1, 5),
(14,  3, 0, 2),
(15, 10, 1, 5),
(16,  6, 0, 3);
GO

-- Dia chi giao hang mac dinh cua khach hang
INSERT INTO dia_chi_giao_hang (khach_hang_id, ho_ten_nguoi_nhan, so_dien_thoai, dia_chi, thanh_pho, la_mac_dinh) VALUES
(1, N'Nghiem Viet Anh',     N'0912345678',  N'123 Pho Hue, Hoan Kiem',           N'Ha Noi',          1),
(2, N'Tran Thi Binh',       N'0901234567',  N'456 Nguyen Trai, Quan 5',           N'TP. Ho Chi Minh', 1),
(3, N'Le Hoang Cuong',      N'0912345000',  N'78 Dinh Tien Hoang, Quan 1',        N'TP. Ho Chi Minh', 1),
(4, N'Pham Thi Duyen',      N'0934567890',  N'12 Tran Phu, Hai Chau',             N'Da Nang',         1),
(5, N'Nguyen Minh Duc',     N'0956789012',  N'34 Hoang Dieu, Hai Chau',           N'Da Nang',         1),
(6, N'Cty Minh Anh Tech',   N'02838901234', N'50 Le Loi, Quan 1',                 N'TP. Ho Chi Minh', 1);
GO

-- Khuyen mai (loai = 'percent' hoac 'fixed' theo CHECK constraint)
INSERT INTO khuyen_mai (ma_khuyen_mai, ten_khuyen_mai, loai, gia_tri, gia_tri_toi_da, don_hang_toi_thieu, ngay_bat_dau, ngay_ket_thuc, so_luong_toi_da, so_lan_da_dung, trang_thai) VALUES
(N'SUMMER24',   N'Mua he 2024 - Giam 10%',              N'percent', 10,      500000,   2000000, N'2024-06-01', N'2026-12-31', 200,  45, N'active'),
(N'NEWCUST',    N'Khach hang moi - Giam 200.000d',       N'fixed',   200000,  NULL,     500000,  N'2024-01-01', N'2026-12-31', 1000,  8, N'active'),
(N'LAPTOP20',   N'Laptop Festival - Giam 20% toi da 2tr', N'percent', 20,    2000000, 10000000, N'2024-08-01', N'2026-12-31',  50,   3, N'active'),
(N'VIP500',     N'Khach VIP - Giam 500.000d',            N'fixed',   500000,  NULL,   15000000, N'2024-01-01', N'2026-12-31', 100,   2, N'active'),
(N'TECHFEST15', N'Tech Fest - Giam 15%',                 N'percent', 15,    1500000,   5000000, N'2024-07-01', N'2024-07-31',  50,  12, N'inactive');
-- khuyen_mai: SUMMER24=1, NEWCUST=2, LAPTOP20=3, VIP500=4, TECHFEST15=5
GO

-- Phieu nhap kho
INSERT INTO phieu_nhap_kho (nha_cung_cap_id, nhan_vien_id, ngay_nhap, tong_tien, trang_thai, ghi_chu) VALUES
(1, 3, N'2024-05-01', 243500000, N'hoan_thanh', N'Nhap hang dot 1 - Dell va iPhone tu Digiworld'),
(2, 3, N'2024-06-15', 572000000, N'hoan_thanh', N'Nhap hang dot 2 - Asus, Lenovo, HP, MSI tu FPT'),
(3, 3, N'2024-07-01', 492000000, N'hoan_thanh', N'Nhap hang dot 3 - Samsung S24 Ultra, Xiaomi 14 Ultra tu TGDD');
-- phieu_nhap: P1=1, P2=2, P3=3
GO

-- Chi tiet phieu nhap kho
INSERT INTO chi_tiet_phieu_nhap (phieu_nhap_id, bien_the_id, so_luong, don_gia_nhap) VALUES
(1,  1,  5, 13000000), (1,  2,  3, 15000000), (1,  3,  3, 28000000),
(2,  4, 10, 12500000), (2,  5,  7, 16000000),
(2,  6, 12, 14500000), (2,  7,  5, 18000000),
(2,  8,  8, 22000000), (2,  9,  3, 28000000),
(2, 10,  6, 22500000), (2, 11,  4, 30000000),
(3, 12, 15, 25000000), (3, 13,  8, 27500000), (3, 14,  3, 32000000),
(3, 15, 10, 22000000), (3, 16,  6, 25000000);
GO

-- Don hang mau
-- trang_thai_don_hang: pending|confirmed|processing|shipping|delivered|cancelled|returned
-- trang_thai_thanh_toan: unpaid|partial|paid|refunded
INSERT INTO don_hang (khach_hang_id, nhan_vien_id, khuyen_mai_id, nguoi_nhan, sdt_nguoi_nhan, dia_chi_giao_hang_text, tong_tien, giam_gia, phi_van_chuyen, thanh_tien, ngay_dat, ngay_giao_thuc_te, trang_thai_don_hang, trang_thai_thanh_toan, kenh_ban) VALUES
-- DH1: KH1 mua Dell 8GB - da giao, da thanh toan
(1, 1, NULL, N'Nghiem Viet Anh',      N'0912345678',  N'123 Pho Hue, Hoan Kiem, Ha Noi',      15500000,       0,  30000, 15530000, N'2024-06-10 10:30:00', N'2024-06-12 15:00:00', N'delivered',  N'paid',    N'online'),
-- DH2: KH2 mua Xiaomi 14U 256 (km SUMMER24 -500K) - da xac nhan, chua thanh toan
(2, 2, 1,    N'Tran Thi Binh',        N'0901234567',  N'456 Nguyen Trai, Quan 5, TP.HCM',     27990000,  500000,      0, 27490000, N'2024-07-01 09:15:00', NULL,                   N'confirmed',  N'unpaid',  N'online'),
-- DH3: KH3 mua HP Envy i9/32GB - dang xu ly, thanh toan 1 phan
(3, NULL, NULL, N'Le Hoang Cuong',    N'0912345000',  N'78 Dinh Tien Hoang, Quan 1, TP.HCM',  34990000,       0,      0, 34990000, N'2024-07-05 14:20:00', NULL,                   N'processing', N'partial', N'online'),
-- DH4: KH4 mua Samsung S24U 256 (km NEWCUST -200K) - cho xac nhan, chua thanh toan
(4, NULL, 2,  N'Pham Thi Duyen',      N'0934567890',  N'12 Tran Phu, Hai Chau, Da Nang',      29990000,  200000,      0, 29790000, N'2024-07-08 16:45:00', NULL,                   N'pending',    N'unpaid',  N'online'),
-- DH5: KH1 mua iPhone 15PM - da giao, da thanh toan (tai cua hang)
(1, 1, NULL, N'Nghiem Viet Anh',      N'0912345678',  N'123 Pho Hue, Ha Noi',                 32500000,       0,      0, 32500000, N'2024-06-20 11:00:00', N'2024-06-20 11:30:00', N'delivered',  N'paid',    N'in_store'),
-- DH6: KH6 (doanh nghiep) mua Lenovo R7 (km VIP500) - dang giao, da thanh toan
(6, 2, 4,    N'Cty Minh Anh Tech',    N'02838901234', N'50 Le Loi, Quan 1, TP.HCM',           22490000,  500000,      0, 21990000, N'2024-07-10 08:30:00', NULL,                   N'shipping',   N'paid',    N'online'),
-- DH7: KH5 mua MSI RTX4050 - da huy
(5, NULL, NULL, N'Nguyen Minh Duc',   N'0956789012',  N'34 Hoang Dieu, Da Nang',              27990000,       0,  30000, 28020000, N'2024-07-12 10:00:00', NULL,                   N'cancelled',  N'unpaid',  N'online');
-- don_hang: DH1=1, DH2=2, DH3=3, DH4=4, DH5=5, DH6=6, DH7=7
GO

-- Chi tiet don hang
INSERT INTO chi_tiet_don_hang (don_hang_id, bien_the_id, so_luong, don_gia, giam_gia_dong) VALUES
(1,  1, 1, 15500000,      0),   -- DH1: Dell Inspiron 8GB
(2, 15, 1, 27990000, 500000),   -- DH2: Xiaomi 14U 256 (km)
(3,  9, 1, 34990000,      0),   -- DH3: HP Envy i9/32GB
(4, 12, 1, 29990000, 200000),   -- DH4: Samsung S24U 256 (km)
(5,  3, 1, 32500000,      0),   -- DH5: iPhone 15 Pro Max
(6,  7, 1, 22490000,      0),   -- DH6: Lenovo IdeaPad R7/16GB
(7, 10, 1, 27990000,      0);   -- DH7: MSI Stealth RTX4050 (huy)
GO

-- Thanh toan cho don hang da thanh toan / dat coc
INSERT INTO thanh_toan (don_hang_id, ngay_thanh_toan, phuong_thuc_thanh_toan, so_tien, trang_thai, ghi_chu) VALUES
(1, N'2024-06-10 10:35:00', N'chuyen_khoan', 15530000, N'success', N'Chuyen khoan Vietcombank'),
(3, N'2024-07-05 14:25:00', N'tien_mat',     10000000, N'success', N'Dat coc 10 trieu tien mat'),
(5, N'2024-06-20 11:05:00', N'tien_mat',     32500000, N'success', N'Thanh toan tien mat tai quay'),
(6, N'2024-07-10 08:40:00', N'chuyen_khoan', 21990000, N'success', N'Chuyen khoan doanh nghiep');
GO

-- Lich su ton kho (xuat hang cho don da giao / dang giao)
INSERT INTO lich_su_ton_kho (bien_the_id, loai_bien_dong, so_luong_thay_doi, don_hang_id, nhan_vien_id, ghi_chu) VALUES
( 1, N'xuat_ban', -1, 1, 1, N'Xuat ban DH1 - Dell Inspiron 8GB'),
(15, N'xuat_ban', -1, 2, 2, N'Xuat ban DH2 - Xiaomi 14U 256GB'),
( 9, N'xuat_ban', -1, 3, NULL, N'Xuat ban DH3 - HP Envy i9/32GB'),
(12, N'xuat_ban', -1, 4, NULL, N'Xuat ban DH4 - Samsung S24U 256'),
( 3, N'xuat_ban', -1, 5, 1, N'Xuat ban DH5 - iPhone 15 Pro Max'),
( 7, N'xuat_ban', -1, 6, 2, N'Xuat ban DH6 - Lenovo R7/16GB');
GO
