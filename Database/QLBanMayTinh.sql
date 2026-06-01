

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
--  1. THƯƠNG HIỆU
-- ============================================================
CREATE TABLE thuong_hieu (
    thuong_hieu_id   INT            IDENTITY(1,1) PRIMARY KEY,
    ten_thuong_hieu  NVARCHAR(100)  NOT NULL UNIQUE,
    quoc_gia         NVARCHAR(100)  NULL,
    mo_ta            NVARCHAR(500)  NULL,
    trang_thai       NVARCHAR(20)   NOT NULL DEFAULT N'active'
        CONSTRAINT CK_thuong_hieu_trangthai CHECK (trang_thai IN (N'active', N'inactive')),
    ngay_tao         DATETIME       NOT NULL DEFAULT GETDATE()
);
GO

-- ============================================================
--  2. DANH MỤC SẢN PHẨM
-- ============================================================
CREATE TABLE danh_muc (
    danh_muc_id   INT            IDENTITY(1,1) PRIMARY KEY,
    ten_danh_muc  NVARCHAR(100)  NOT NULL UNIQUE,
    mo_ta         NVARCHAR(500)  NULL,
    trang_thai    NVARCHAR(20)   NOT NULL DEFAULT N'active'
        CONSTRAINT CK_danh_muc_trangthai CHECK (trang_thai IN (N'active', N'inactive')),
    ngay_tao      DATETIME       NOT NULL DEFAULT GETDATE()
);
GO

-- ============================================================
--  3. NHÀ CUNG CẤP
-- ============================================================
CREATE TABLE nha_cung_cap (
    nha_cung_cap_id   INT            IDENTITY(1,1) PRIMARY KEY,
    ten_nha_cung_cap  NVARCHAR(150)  NOT NULL,
    so_dien_thoai     VARCHAR(20)    NULL,
    email             VARCHAR(100)   NULL,
    dia_chi           NVARCHAR(255)  NULL,
    ma_so_thue        VARCHAR(20)    NULL,          -- UNIQUE qua filtered index bên dưới
    nguoi_lien_he     NVARCHAR(150)  NULL,
    trang_thai        NVARCHAR(20)   NOT NULL DEFAULT N'active'
        CONSTRAINT CK_nha_cung_cap_trangthai CHECK (trang_thai IN (N'active', N'inactive')),
    ngay_tao          DATETIME       NOT NULL DEFAULT GETDATE()
);
GO

-- Unique mã số thuế chỉ khi có giá trị (tránh lỗi duplicate NULL)
CREATE UNIQUE INDEX UX_nha_cung_cap_ma_so_thue
    ON nha_cung_cap(ma_so_thue)
    WHERE ma_so_thue IS NOT NULL;
GO

-- ============================================================
--  4. KHÁCH HÀNG
-- ============================================================
CREATE TABLE khach_hang (
    khach_hang_id  INT            IDENTITY(1,1) PRIMARY KEY,
    ho_ten         NVARCHAR(150)  NOT NULL,
    so_dien_thoai  VARCHAR(20)    NOT NULL UNIQUE,
    email          VARCHAR(100)   NULL,
    dia_chi        NVARCHAR(255)  NULL,
    loai_khach     NVARCHAR(20)   NOT NULL DEFAULT N'ca_nhan'
        CONSTRAINT CK_khach_hang_loai CHECK (loai_khach IN (N'ca_nhan', N'doanh_nghiep')),
    ten_cong_ty    NVARCHAR(200)  NULL,
    ma_so_thue     VARCHAR(20)    NULL,
    diem_tich_luy  INT            NOT NULL DEFAULT 0,
    trang_thai     NVARCHAR(20)   NOT NULL DEFAULT N'active'
        CONSTRAINT CK_khach_hang_trangthai CHECK (trang_thai IN (N'active', N'inactive', N'blocked')),
    ngay_tao       DATETIME       NOT NULL DEFAULT GETDATE()
);
GO

-- ============================================================
--  5. CHỨC VỤ
-- ============================================================
CREATE TABLE chuc_vu (
    chuc_vu_id   INT            IDENTITY(1,1) PRIMARY KEY,
    ten_chuc_vu  NVARCHAR(100)  NOT NULL UNIQUE,
    mo_ta        NVARCHAR(255)  NULL
);
GO

-- ============================================================
--  6. NHÂN VIÊN
-- ============================================================
CREATE TABLE nhan_vien (
    nhan_vien_id   INT            IDENTITY(1,1) PRIMARY KEY,
    ho_ten         NVARCHAR(150)  NOT NULL,
    so_dien_thoai  VARCHAR(20)    NULL UNIQUE,
    email          VARCHAR(100)   NULL UNIQUE,
    chuc_vu_id     INT            NULL,
    username       VARCHAR(50)    NULL UNIQUE,
    mat_khau_hash  VARCHAR(255)   NULL,
    luong_co_ban   DECIMAL(18,2)  NULL,
    trang_thai     NVARCHAR(20)   NOT NULL DEFAULT N'active'
        CONSTRAINT CK_nhan_vien_trangthai CHECK (trang_thai IN (N'active', N'inactive', N'nghi_viec')),
    ngay_tao       DATETIME       NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_nhan_vien_chuc_vu FOREIGN KEY (chuc_vu_id)
        REFERENCES chuc_vu(chuc_vu_id)
);
GO

-- ============================================================
--  7. SẢN PHẨM
-- ============================================================
CREATE TABLE san_pham (
    san_pham_id          INT             IDENTITY(1,1) PRIMARY KEY,
    ten_san_pham         NVARCHAR(200)   NOT NULL,
    thuong_hieu_id       INT             NOT NULL,
    danh_muc_id          INT             NOT NULL,
    nha_cung_cap_id      INT             NULL,
    ma_sku               VARCHAR(50)     NOT NULL UNIQUE,
    -- Thông số kỹ thuật
    cpu                  NVARCHAR(100)   NULL,
    ram                  NVARCHAR(50)    NULL,
    o_cung               NVARCHAR(100)   NULL,
    gpu                  NVARCHAR(100)   NULL,
    kich_thuoc_man_hinh  NVARCHAR(50)    NULL,
    he_dieu_hanh         NVARCHAR(100)   NULL,
    pin                  NVARCHAR(50)    NULL,
    trong_luong_kg       DECIMAL(5,2)    NULL,
    mau_sac              NVARCHAR(50)    NULL,
    -- Giá & bảo hành
    gia_ban              DECIMAL(18,2)   NOT NULL CONSTRAINT CK_san_pham_giaban CHECK (gia_ban >= 0),
    gia_nhap             DECIMAL(18,2)   NULL     CONSTRAINT CK_san_pham_gianhap CHECK (gia_nhap >= 0),
    bao_hanh_thang       INT             NOT NULL DEFAULT 12,
    -- Hình ảnh & mô tả
    mo_ta                NVARCHAR(MAX)   NULL,
    hinh_anh_chinh       NVARCHAR(500)   NULL,
    -- Trạng thái
    trang_thai           NVARCHAR(20)    NOT NULL DEFAULT N'active'
        CONSTRAINT CK_san_pham_trangthai CHECK (trang_thai IN (N'active', N'inactive', N'ngung_kinh_doanh')),
    ngay_tao             DATETIME        NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_san_pham_thuong_hieu  FOREIGN KEY (thuong_hieu_id)  REFERENCES thuong_hieu(thuong_hieu_id),
    CONSTRAINT FK_san_pham_danh_muc     FOREIGN KEY (danh_muc_id)     REFERENCES danh_muc(danh_muc_id),
    CONSTRAINT FK_san_pham_nha_cung_cap FOREIGN KEY (nha_cung_cap_id) REFERENCES nha_cung_cap(nha_cung_cap_id)
);
GO

-- ============================================================
--  8. TỒN KHO
-- ============================================================
CREATE TABLE ton_kho (
    ton_kho_id        INT       IDENTITY(1,1) PRIMARY KEY,
    san_pham_id       INT       NOT NULL UNIQUE,
    so_luong_ton      INT       NOT NULL DEFAULT 0 CONSTRAINT CK_ton_kho_ton CHECK (so_luong_ton >= 0),
    so_luong_giu      INT       NOT NULL DEFAULT 0 CONSTRAINT CK_ton_kho_giu CHECK (so_luong_giu >= 0),
    ton_kho_toi_thieu INT       NOT NULL DEFAULT 5,
    ngay_cap_nhat     DATETIME  NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_ton_kho_san_pham FOREIGN KEY (san_pham_id) REFERENCES san_pham(san_pham_id)
);
GO

-- ============================================================
--  9. KHUYẾN MÃI
-- ============================================================
CREATE TABLE khuyen_mai (
    khuyen_mai_id      INT            IDENTITY(1,1) PRIMARY KEY,
    ma_khuyen_mai      VARCHAR(50)    NOT NULL UNIQUE,
    ten_khuyen_mai     NVARCHAR(150)  NOT NULL,
    loai               NVARCHAR(20)   NOT NULL
        CONSTRAINT CK_km_loai CHECK (loai IN (N'percent', N'fixed')),
    gia_tri            DECIMAL(18,2)  NOT NULL CONSTRAINT CK_km_giatri CHECK (gia_tri > 0),
    gia_tri_toi_da     DECIMAL(18,2)  NULL,
    don_hang_toi_thieu DECIMAL(18,2)  NULL,
    ngay_bat_dau       DATETIME       NOT NULL,
    ngay_ket_thuc      DATETIME       NOT NULL,
    so_luong_toi_da    INT            NULL,
    so_lan_da_dung     INT            NOT NULL DEFAULT 0,
    trang_thai         NVARCHAR(20)   NOT NULL DEFAULT N'active'
        CONSTRAINT CK_km_trangthai CHECK (trang_thai IN (N'active', N'inactive', N'het_han')),
    ngay_tao           DATETIME       NOT NULL DEFAULT GETDATE(),
    CONSTRAINT CK_km_ngay CHECK (ngay_ket_thuc > ngay_bat_dau)
);
GO

-- ============================================================
--  10. ĐỊA CHỈ GIAO HÀNG KHÁCH HÀNG
-- ============================================================
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
--  11. PHIẾU NHẬP KHO
-- ============================================================
CREATE TABLE phieu_nhap_kho (
    phieu_nhap_id    INT            IDENTITY(1,1) PRIMARY KEY,
    ma_phieu_nhap    VARCHAR(30)    NOT NULL UNIQUE DEFAULT CONCAT('PN', FORMAT(GETDATE(),'yyyyMMdd')),
    nha_cung_cap_id  INT            NOT NULL,
    nhan_vien_id     INT            NULL,
    ngay_nhap        DATETIME       NOT NULL DEFAULT GETDATE(),
    tong_tien        DECIMAL(18,2)  NOT NULL DEFAULT 0,
    trang_thai       NVARCHAR(20)   NOT NULL DEFAULT N'hoan_thanh'
        CONSTRAINT CK_phieu_nhap_trangthai CHECK (trang_thai IN (N'cho_duyet', N'hoan_thanh', N'huy')),
    ghi_chu          NVARCHAR(500)  NULL,
    CONSTRAINT FK_phieu_nhap_ncc       FOREIGN KEY (nha_cung_cap_id) REFERENCES nha_cung_cap(nha_cung_cap_id),
    CONSTRAINT FK_phieu_nhap_nhan_vien FOREIGN KEY (nhan_vien_id)    REFERENCES nhan_vien(nhan_vien_id)
);
GO

CREATE TABLE chi_tiet_phieu_nhap (
    chi_tiet_nhap_id  INT            IDENTITY(1,1) PRIMARY KEY,
    phieu_nhap_id     INT            NOT NULL,
    san_pham_id       INT            NOT NULL,
    so_luong          INT            NOT NULL CONSTRAINT CK_ctpn_soluong CHECK (so_luong > 0),
    don_gia_nhap      DECIMAL(18,2)  NOT NULL CONSTRAINT CK_ctpn_dongia  CHECK (don_gia_nhap >= 0),
    thanh_tien        AS (so_luong * don_gia_nhap) PERSISTED,
    CONSTRAINT FK_ctpn_phieu_nhap FOREIGN KEY (phieu_nhap_id) REFERENCES phieu_nhap_kho(phieu_nhap_id),
    CONSTRAINT FK_ctpn_san_pham   FOREIGN KEY (san_pham_id)   REFERENCES san_pham(san_pham_id)
);
GO

-- ============================================================
--  12. ĐƠN HÀNG
--  (Khai báo sau phieu_nhap_kho, khuyen_mai, dia_chi_giao_hang)
-- ============================================================
CREATE TABLE don_hang (
    don_hang_id             INT            IDENTITY(1,1) PRIMARY KEY,
    ma_don_hang             VARCHAR(30)    NOT NULL UNIQUE DEFAULT CONCAT('DH', FORMAT(GETDATE(),'yyyyMMdd')),
    khach_hang_id           INT            NOT NULL,
    nhan_vien_id            INT            NULL,
    khuyen_mai_id           INT            NULL,
    dia_chi_giao_hang_id    INT            NULL,
    -- Snapshot thông tin giao hàng tại thời điểm đặt
    dia_chi_giao_hang_text  NVARCHAR(255)  NULL,
    nguoi_nhan              NVARCHAR(150)  NULL,
    sdt_nguoi_nhan          VARCHAR(20)    NULL,
    -- Tiền
    tong_tien               DECIMAL(18,2)  NOT NULL DEFAULT 0,
    giam_gia                DECIMAL(18,2)  NOT NULL DEFAULT 0,
    phi_van_chuyen          DECIMAL(18,2)  NOT NULL DEFAULT 0,
    thanh_tien              DECIMAL(18,2)  NOT NULL DEFAULT 0,
    -- Thời gian
    ngay_dat                DATETIME       NOT NULL DEFAULT GETDATE(),
    ngay_giao_du_kien       DATETIME       NULL,
    ngay_giao_thuc_te       DATETIME       NULL,
    -- Trạng thái
    trang_thai_don_hang     NVARCHAR(30)   NOT NULL DEFAULT N'pending'
        CONSTRAINT CK_dh_trangthai CHECK (trang_thai_don_hang IN
            (N'pending', N'confirmed', N'processing', N'shipping', N'delivered', N'cancelled', N'returned')),
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
--  13. LỊCH SỬ TỒN KHO
--  (Khai báo sau don_hang và phieu_nhap_kho để FK đầy đủ ngay)
-- ============================================================
CREATE TABLE lich_su_ton_kho (
    lich_su_id        INT            IDENTITY(1,1) PRIMARY KEY,
    san_pham_id       INT            NOT NULL,
    loai_bien_dong    NVARCHAR(30)   NOT NULL
        CONSTRAINT CK_lsdk_loai CHECK (loai_bien_dong IN (N'nhap', N'xuat_ban', N'tra_hang', N'dieu_chinh', N'huy')),
    so_luong_thay_doi INT            NOT NULL,
    so_luong_truoc    INT            NOT NULL,
    so_luong_sau      INT            NOT NULL,
    don_hang_id       INT            NULL,
    phieu_nhap_id     INT            NULL,
    nhan_vien_id      INT            NULL,
    ghi_chu           NVARCHAR(255)  NULL,
    ngay_tao          DATETIME       NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_lstk_san_pham   FOREIGN KEY (san_pham_id)  REFERENCES san_pham(san_pham_id),
    CONSTRAINT FK_lstk_nhan_vien  FOREIGN KEY (nhan_vien_id) REFERENCES nhan_vien(nhan_vien_id),
    CONSTRAINT FK_lstk_don_hang   FOREIGN KEY (don_hang_id)  REFERENCES don_hang(don_hang_id),
    CONSTRAINT FK_lstk_phieu_nhap FOREIGN KEY (phieu_nhap_id) REFERENCES phieu_nhap_kho(phieu_nhap_id)
);
GO

-- ============================================================
--  14. CHI TIẾT ĐƠN HÀNG
-- ============================================================
CREATE TABLE chi_tiet_don_hang (
    chi_tiet_don_hang_id  INT            IDENTITY(1,1) PRIMARY KEY,
    don_hang_id           INT            NOT NULL,
    san_pham_id           INT            NOT NULL,
    so_luong              INT            NOT NULL CONSTRAINT CK_ctdh_soluong CHECK (so_luong > 0),
    don_gia               DECIMAL(18,2)  NOT NULL CONSTRAINT CK_ctdh_dongia  CHECK (don_gia >= 0),
    giam_gia_dong         DECIMAL(18,2)  NOT NULL DEFAULT 0,
    thanh_tien            AS (so_luong * don_gia - giam_gia_dong) PERSISTED,
    ghi_chu               NVARCHAR(255)  NULL,
    CONSTRAINT FK_ctdh_don_hang FOREIGN KEY (don_hang_id) REFERENCES don_hang(don_hang_id),
    CONSTRAINT FK_ctdh_san_pham FOREIGN KEY (san_pham_id) REFERENCES san_pham(san_pham_id)
);
GO

-- ============================================================
--  15. THANH TOÁN
-- ============================================================
CREATE TABLE thanh_toan (
    thanh_toan_id           INT            IDENTITY(1,1) PRIMARY KEY,
    don_hang_id             INT            NOT NULL,
    ngay_thanh_toan         DATETIME       NOT NULL DEFAULT GETDATE(),
    phuong_thuc_thanh_toan  NVARCHAR(50)   NOT NULL
        CONSTRAINT CK_tt_phuongthuc CHECK (phuong_thuc_thanh_toan IN
            (N'tien_mat', N'chuyen_khoan', N'the_tin_dung', N'momo', N'vnpay', N'zalopay', N'tra_gop', N'khac')),
    so_tien                 DECIMAL(18,2)  NOT NULL CONSTRAINT CK_tt_sotien CHECK (so_tien > 0),
    ma_giao_dich            VARCHAR(100)   NULL,
    trang_thai              NVARCHAR(30)   NOT NULL DEFAULT N'success'
        CONSTRAINT CK_tt_trangthai CHECK (trang_thai IN (N'success', N'failed', N'pending', N'refunded')),
    ghi_chu                 NVARCHAR(255)  NULL,
    CONSTRAINT FK_tt_don_hang FOREIGN KEY (don_hang_id) REFERENCES don_hang(don_hang_id)
);
GO

-- ============================================================
--  16. TRẢ HÀNG / HOÀN TRẢ
-- ============================================================
CREATE TABLE phieu_tra_hang (
    phieu_tra_id  INT            IDENTITY(1,1) PRIMARY KEY,
    don_hang_id   INT            NOT NULL,
    nhan_vien_id  INT            NULL,
    ly_do         NVARCHAR(255)  NOT NULL,
    ngay_tra      DATETIME       NOT NULL DEFAULT GETDATE(),
    trang_thai    NVARCHAR(20)   NOT NULL DEFAULT N'cho_xu_ly'
        CONSTRAINT CK_pth_trangthai CHECK (trang_thai IN (N'cho_xu_ly', N'da_xu_ly', N'tu_choi')),
    so_tien_hoan  DECIMAL(18,2)  NOT NULL DEFAULT 0,
    ghi_chu       NVARCHAR(500)  NULL,
    CONSTRAINT FK_pth_don_hang  FOREIGN KEY (don_hang_id)  REFERENCES don_hang(don_hang_id),
    CONSTRAINT FK_pth_nhan_vien FOREIGN KEY (nhan_vien_id) REFERENCES nhan_vien(nhan_vien_id)
);
GO

CREATE TABLE chi_tiet_tra_hang (
    chi_tiet_tra_id  INT            IDENTITY(1,1) PRIMARY KEY,
    phieu_tra_id     INT            NOT NULL,
    san_pham_id      INT            NOT NULL,
    so_luong         INT            NOT NULL CONSTRAINT CK_ctth_soluong CHECK (so_luong > 0),
    don_gia_hoan     DECIMAL(18,2)  NOT NULL,
    tinh_trang       NVARCHAR(50)   NULL,
    CONSTRAINT FK_ctth_phieu_tra FOREIGN KEY (phieu_tra_id) REFERENCES phieu_tra_hang(phieu_tra_id),
    CONSTRAINT FK_ctth_san_pham  FOREIGN KEY (san_pham_id)  REFERENCES san_pham(san_pham_id)
);
GO

-- ============================================================
--  17. BẢO HÀNH
-- ============================================================
CREATE TABLE phieu_bao_hanh (
    bao_hanh_id       INT            IDENTITY(1,1) PRIMARY KEY,
    don_hang_id       INT            NOT NULL,
    san_pham_id       INT            NOT NULL,
    khach_hang_id     INT            NOT NULL,
    serial_number     VARCHAR(100)   NULL,
    ngay_mua          DATETIME       NOT NULL,
    ngay_het_bh       DATETIME       NOT NULL,
    ngay_tiep_nhan    DATETIME       NULL,
    ngay_tra_khach    DATETIME       NULL,
    mo_ta_loi         NVARCHAR(500)  NULL,
    ket_qua_xu_ly     NVARCHAR(500)  NULL,
    trang_thai        NVARCHAR(30)   NOT NULL DEFAULT N'con_bao_hanh'
        CONSTRAINT CK_pbh_trangthai CHECK (trang_thai IN
            (N'con_bao_hanh', N'dang_xu_ly', N'da_xu_ly', N'het_bao_hanh', N'tu_choi')),
    chi_phi_phat_sinh DECIMAL(18,2)  NOT NULL DEFAULT 0,
    ghi_chu           NVARCHAR(500)  NULL,
    CONSTRAINT FK_pbh_don_hang   FOREIGN KEY (don_hang_id)   REFERENCES don_hang(don_hang_id),
    CONSTRAINT FK_pbh_san_pham   FOREIGN KEY (san_pham_id)   REFERENCES san_pham(san_pham_id),
    CONSTRAINT FK_pbh_khach_hang FOREIGN KEY (khach_hang_id) REFERENCES khach_hang(khach_hang_id)
);
GO

-- ============================================================
--  18. INDEX TỐI ƯU HIỆU SUẤT
-- ============================================================
-- Sản phẩm
CREATE INDEX IX_san_pham_danh_muc    ON san_pham(danh_muc_id);
CREATE INDEX IX_san_pham_thuong_hieu ON san_pham(thuong_hieu_id);
CREATE INDEX IX_san_pham_trang_thai  ON san_pham(trang_thai);

-- Đơn hàng
CREATE INDEX IX_don_hang_khach_hang  ON don_hang(khach_hang_id);
CREATE INDEX IX_don_hang_nhan_vien   ON don_hang(nhan_vien_id);
CREATE INDEX IX_don_hang_ngay_dat    ON don_hang(ngay_dat DESC);
CREATE INDEX IX_don_hang_trangthai   ON don_hang(trang_thai_don_hang, trang_thai_thanh_toan);

-- Chi tiết đơn hàng
CREATE INDEX IX_ctdh_don_hang        ON chi_tiet_don_hang(don_hang_id);
CREATE INDEX IX_ctdh_san_pham        ON chi_tiet_don_hang(san_pham_id);

-- Thanh toán
CREATE INDEX IX_thanh_toan_don_hang  ON thanh_toan(don_hang_id);

-- Tồn kho
CREATE INDEX IX_ton_kho_san_pham     ON ton_kho(san_pham_id);

-- Lịch sử tồn kho
CREATE INDEX IX_lstk_san_pham        ON lich_su_ton_kho(san_pham_id, ngay_tao DESC);

-- Khách hàng
CREATE INDEX IX_khach_hang_loai      ON khach_hang(loai_khach);
GO

-- ============================================================
--  19. VIEWS HỮU ÍCH
-- ============================================================

-- Tổng quan tồn kho + cảnh báo
CREATE VIEW vw_ton_kho_tong_quan AS
SELECT
    sp.san_pham_id,
    sp.ma_sku,
    sp.ten_san_pham,
    th.ten_thuong_hieu,
    dm.ten_danh_muc,
    tk.so_luong_ton,
    tk.so_luong_giu,
    tk.so_luong_ton - tk.so_luong_giu AS co_the_ban,
    tk.ton_kho_toi_thieu,
    CASE
        WHEN tk.so_luong_ton = 0                     THEN N'Het hang'
        WHEN tk.so_luong_ton <= tk.ton_kho_toi_thieu THEN N'Sap het'
        ELSE N'Du hang'
    END AS tinh_trang_kho,
    sp.gia_ban,
    sp.gia_nhap,
    tk.ngay_cap_nhat
FROM san_pham sp
JOIN ton_kho     tk ON sp.san_pham_id    = tk.san_pham_id
JOIN thuong_hieu th ON sp.thuong_hieu_id = th.thuong_hieu_id
JOIN danh_muc    dm ON sp.danh_muc_id    = dm.danh_muc_id
WHERE sp.trang_thai = N'active';
GO

-- Doanh thu theo đơn hàng
CREATE VIEW vw_don_hang_tong_hop AS
SELECT
    dh.don_hang_id,
    dh.ma_don_hang,
    kh.ho_ten               AS ten_khach_hang,
    kh.so_dien_thoai,
    nv.ho_ten               AS ten_nhan_vien,
    dh.ngay_dat,
    dh.tong_tien,
    dh.giam_gia,
    dh.phi_van_chuyen,
    dh.thanh_tien,
    dh.trang_thai_don_hang,
    dh.trang_thai_thanh_toan,
    COUNT(ctdh.chi_tiet_don_hang_id) AS so_san_pham
FROM don_hang dh
JOIN khach_hang kh              ON dh.khach_hang_id = kh.khach_hang_id
LEFT JOIN nhan_vien nv          ON dh.nhan_vien_id  = nv.nhan_vien_id
LEFT JOIN chi_tiet_don_hang ctdh ON dh.don_hang_id  = ctdh.don_hang_id
GROUP BY
    dh.don_hang_id, dh.ma_don_hang,
    kh.ho_ten, kh.so_dien_thoai,
    nv.ho_ten,
    dh.ngay_dat, dh.tong_tien, dh.giam_gia,
    dh.phi_van_chuyen, dh.thanh_tien,
    dh.trang_thai_don_hang, dh.trang_thai_thanh_toan;
GO

-- ============================================================
--  20. DỮ LIỆU MẪU
-- ============================================================
INSERT INTO chuc_vu (ten_chuc_vu, mo_ta) VALUES
    (N'Quản lý',             N'Quản lý cửa hàng'),
    (N'Nhân viên bán hàng',  N'Tư vấn và bán hàng'),
    (N'Kỹ thuật viên',       N'Bảo hành và sửa chữa'),
    (N'Thủ kho',             N'Quản lý kho hàng');

INSERT INTO thuong_hieu (ten_thuong_hieu, quoc_gia) VALUES
    (N'Dell',   N'Mỹ'),
    (N'HP',     N'Mỹ'),
    (N'Lenovo', N'Trung Quốc'),
    (N'Asus',   N'Đài Loan'),
    (N'Apple',  N'Mỹ'),
    (N'Acer',   N'Đài Loan'),
    (N'MSI',    N'Đài Loan');

INSERT INTO danh_muc (ten_danh_muc) VALUES
    (N'Laptop văn phòng'),
    (N'Laptop gaming'),
    (N'Laptop đồ họa'),
    (N'Máy tính để bàn'),
    (N'Phụ kiện');

INSERT INTO nha_cung_cap (ten_nha_cung_cap, so_dien_thoai, email, dia_chi) VALUES
    (N'Công ty TNHH Phân Phối Tin Học Minh Khoa', '02838123456', 'minhkhoa@example.com', N'123 Nguyễn Văn Cừ, Q5, TP.HCM'),
    (N'Công ty CP Công Nghệ Sao Việt',            '02439876543', 'saoviet@example.com',  N'45 Lê Văn Lương, Hà Nội');

INSERT INTO nhan_vien (ho_ten, so_dien_thoai, email, chuc_vu_id, username, trang_thai) VALUES
    (N'Nguyễn Văn An',  '0901234567', 'an.nguyen@shop.com',   1, 'an.nguyen',  N'active'),
    (N'Trần Thị Bình',  '0912345678', 'binh.tran@shop.com',   2, 'binh.tran',  N'active'),
    (N'Lê Minh Châu',   '0923456789', 'chau.le@shop.com',     3, 'chau.le',    N'active'),
    (N'Phạm Thị Dung',  '0934567890', 'dung.pham@shop.com',   4, 'dung.pham',  N'active');

INSERT INTO khach_hang (ho_ten, so_dien_thoai, email, dia_chi) VALUES
    (N'Hoàng Văn Hùng', '0941111111', 'hung@gmail.com', N'10 Lê Lợi, Hà Nội'),
    (N'Nguyễn Thị Mai', '0952222222', 'mai@gmail.com',  N'22 Trần Phú, Đà Nẵng'),
    (N'Trịnh Quốc Bảo', '0963333333', 'bao@gmail.com',  N'5 Hai Bà Trưng, TP.HCM');

INSERT INTO san_pham (ten_san_pham, thuong_hieu_id, danh_muc_id, nha_cung_cap_id, ma_sku,
    cpu, ram, o_cung, gpu, kich_thuoc_man_hinh, gia_ban, gia_nhap, bao_hanh_thang) VALUES
    (N'Dell Inspiron 15 3520',          1, 1, 1, 'DELL-INS-3520',
     N'Intel Core i5-1235U',  N'8GB DDR4',   N'512GB SSD', N'Intel Iris Xe',      N'15.6 inch FHD',
     15990000, 13500000, 12),
    (N'ASUS TUF Gaming F15 2024',       4, 2, 1, 'ASUS-TUF-F15-2024',
     N'Intel Core i7-13620H', N'16GB DDR5',  N'512GB SSD', N'NVIDIA RTX 4060',    N'15.6 inch FHD 144Hz',
     29990000, 26000000, 24),
    (N'Lenovo ThinkPad X1 Carbon Gen 11', 3, 1, 2, 'LENO-X1C-G11',
     N'Intel Core i7-1365U',  N'16GB LPDDR5',N'512GB SSD', N'Intel Iris Xe',      N'14 inch WUXGA IPS',
     39990000, 35000000, 12);

INSERT INTO ton_kho (san_pham_id, so_luong_ton, so_luong_giu, ton_kho_toi_thieu) VALUES
    (1, 20, 0, 5),
    (2, 15, 0, 3),
    (3,  8, 0, 2);

INSERT INTO khuyen_mai (ma_khuyen_mai, ten_khuyen_mai, loai, gia_tri,
    ngay_bat_dau, ngay_ket_thuc, so_luong_toi_da, don_hang_toi_thieu) VALUES
    ('SALE10',   N'Giảm 10% toàn bộ sản phẩm',    N'percent', 10,
     '2025-01-01', '2025-12-31', 500,  5000000),
    ('GIAM500K', N'Giảm 500,000đ đơn từ 20 triệu', N'fixed',   500000,
     '2025-01-01', '2025-12-31', NULL, 20000000);
GO

