use master;
GO

-- Luôn DROP + tạo lại database mỗi lần chạy file — SINGLE_USER trước để đá hết
-- session khác đang giữ DB (vd tab query khác lỡ mở sẵn), tránh lỗi "database in use".
-- Lưu ý: nếu vẫn treo ở bước này, khả năng cao là IntelliSense của SSMS đang tự giữ 1
-- session nền trong chính DB này (Tools → Options → Text Editor → Transact-SQL →
-- IntelliSense → tắt "Enable IntelliSense" rồi mở lại tab query để giải phóng session cũ).
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
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'thuong_hieu')
BEGIN
    CREATE TABLE thuong_hieu (
        thuong_hieu_id   INT            IDENTITY(1,1) PRIMARY KEY,
        ten_thuong_hieu  NVARCHAR(100)  NOT NULL UNIQUE,
        quoc_gia         NVARCHAR(100)  NULL,
        mo_ta            NVARCHAR(500)  NULL,
        trang_thai       NVARCHAR(20)   NOT NULL DEFAULT N'active'
            CONSTRAINT CK_thuong_hieu_trangthai CHECK (trang_thai IN (N'active', N'inactive')),
        ngay_tao         DATETIME       NOT NULL DEFAULT GETDATE()
    );
END

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'danh_muc')
BEGIN
    CREATE TABLE danh_muc (
        danh_muc_id   INT            IDENTITY(1,1) PRIMARY KEY,
        ten_danh_muc  NVARCHAR(100)  NOT NULL UNIQUE,
        mo_ta         NVARCHAR(500)  NULL,
        trang_thai    NVARCHAR(20)   NOT NULL DEFAULT N'active'
            CONSTRAINT CK_danh_muc_trangthai CHECK (trang_thai IN (N'active', N'inactive')),
        ngay_tao      DATETIME       NOT NULL DEFAULT GETDATE()
    );
END

-- Bảng phân loại theo mục đích sử dụng (văn phòng, gaming, đồ họa,...)
-- Tách riêng khỏi danh_muc để 1 sản phẩm có thể thuộc nhiều nhóm
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'phan_loai')
BEGIN
    CREATE TABLE phan_loai (
        phan_loai_id    INT            IDENTITY(1,1) PRIMARY KEY,
        ma_phan_loai    VARCHAR(30)    NOT NULL UNIQUE,   -- key dùng cho query/filter: 'gaming', 'van_phong'...
        ten_phan_loai   NVARCHAR(100)  NOT NULL,           -- tên hiển thị trên UI
        mo_ta           NVARCHAR(255)  NULL,
        thu_tu          INT            NOT NULL DEFAULT 0, -- thứ tự hiện trên filter bar
        trang_thai      NVARCHAR(20)   NOT NULL DEFAULT N'active'
            CONSTRAINT CK_pl_trangthai CHECK (trang_thai IN (N'active', N'inactive'))
    );
END

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'nha_cung_cap')
BEGIN
    CREATE TABLE nha_cung_cap (
        nha_cung_cap_id   INT            IDENTITY(1,1) PRIMARY KEY,
        ten_nha_cung_cap  NVARCHAR(150)  NOT NULL,
        so_dien_thoai     VARCHAR(20)    NULL,
        email             VARCHAR(100)   NULL,
        dia_chi           NVARCHAR(255)  NULL,
        ma_so_thue        VARCHAR(20)    NULL UNIQUE,
        nguoi_lien_he     NVARCHAR(150)  NULL,
        trang_thai        NVARCHAR(20)   NOT NULL DEFAULT N'active'
            CONSTRAINT CK_nha_cung_cap_trangthai CHECK (trang_thai IN (N'active', N'inactive')),
        ngay_tao          DATETIME       NOT NULL DEFAULT GETDATE()
    );
END
GO

-- ============================================================
--  2. KHÁCH HÀNG & NHÂN VIÊN
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'khach_hang')
BEGIN
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
        diem_tich_luy  INT            NOT NULL DEFAULT 0 CONSTRAINT CK_kh_diem CHECK (diem_tich_luy >= 0),
        trang_thai     NVARCHAR(20)   NOT NULL DEFAULT N'active'
            CONSTRAINT CK_khach_hang_trangthai CHECK (trang_thai IN (N'active', N'inactive', N'blocked')),
        da_xoa         BIT            NOT NULL DEFAULT 0,
        ngay_tao       DATETIME       NOT NULL DEFAULT GETDATE(),
        ngay_cap_nhat  DATETIME       NOT NULL DEFAULT GETDATE()
    );
END

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'chuc_vu')
BEGIN
    CREATE TABLE chuc_vu (
        chuc_vu_id   INT            IDENTITY(1,1) PRIMARY KEY,
        ma_chuc_vu   VARCHAR(30)    NOT NULL UNIQUE,  -- role code dùng cho Spring Security
        ten_chuc_vu  NVARCHAR(100)  NOT NULL UNIQUE,
        cap_do       INT            NOT NULL DEFAULT 1 CONSTRAINT CK_cv_capdo CHECK (cap_do BETWEEN 0 AND 9),
        mo_ta        NVARCHAR(255)  NULL
    );
END

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'nhan_vien')
BEGIN
    CREATE TABLE nhan_vien (
        nhan_vien_id   INT            IDENTITY(1,1) PRIMARY KEY,
        ho_ten         NVARCHAR(150)  NOT NULL,
        so_dien_thoai  VARCHAR(20)    NULL UNIQUE,
        email          VARCHAR(100)   NULL UNIQUE,
        chuc_vu_id     INT            NULL,
        luong_co_ban   DECIMAL(18,0)  NOT NULL DEFAULT 0 CONSTRAINT CK_nv_luong CHECK (luong_co_ban >= 0),
        trang_thai     NVARCHAR(20)   NOT NULL DEFAULT N'active'
            CONSTRAINT CK_nhan_vien_trangthai CHECK (trang_thai IN (N'active', N'inactive', N'nghi_viec')),
        da_xoa         BIT            NOT NULL DEFAULT 0,
        ngay_tao       DATETIME       NOT NULL DEFAULT GETDATE(),
        ngay_cap_nhat  DATETIME       NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_nhan_vien_chuc_vu FOREIGN KEY (chuc_vu_id) REFERENCES chuc_vu(chuc_vu_id)
    );
END

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'tai_khoan')
BEGIN
    CREATE TABLE tai_khoan (
        tai_khoan_id  INT            IDENTITY(1,1) PRIMARY KEY,
        username      VARCHAR(50)    NOT NULL UNIQUE,
        mat_khau_hash VARCHAR(255)   NOT NULL,
        chuc_vu_id    INT            NOT NULL,
        nhan_vien_id  INT            NULL,
        khach_hang_id INT            NULL,
        trang_thai    NVARCHAR(20)   NOT NULL DEFAULT N'active'
            CONSTRAINT CK_tk_trangthai CHECK (trang_thai IN (N'active', N'inactive', N'blocked')),
        ngay_tao      DATETIME       NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_tk_chuc_vu    FOREIGN KEY (chuc_vu_id)    REFERENCES chuc_vu(chuc_vu_id),
        CONSTRAINT FK_tk_nhan_vien  FOREIGN KEY (nhan_vien_id)  REFERENCES nhan_vien(nhan_vien_id),
        CONSTRAINT FK_tk_khach_hang FOREIGN KEY (khach_hang_id) REFERENCES khach_hang(khach_hang_id)
    );
END
GO

-- ============================================================
--  3. SẢN PHẨM GỐC
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'san_pham')
BEGIN
    CREATE TABLE san_pham (
        san_pham_id     INT             IDENTITY(1,1) PRIMARY KEY,
        -- Mã sản phẩm nội bộ hiển thị/tra cứu trên UI (vd 'SP0001') và mã vạch EAN-13 in
        -- trên vỏ hộp (dùng cho máy quét ở quầy). Để NULL được: sản phẩm mới tạo từ form
        -- admin có thể chưa gán mã / chưa dán tem, và các INSERT cũ không truyền 2 cột này
        -- vẫn chạy bình thường. Tính duy nhất xử lý bằng filtered unique index bên dưới.
        ma_san_pham     VARCHAR(50)     NULL,
        barcode         VARCHAR(50)     NULL,
        ten_san_pham    NVARCHAR(200)   NOT NULL,
        thuong_hieu_id  INT             NOT NULL,
        danh_muc_id     INT             NOT NULL,
        nha_cung_cap_id INT             NULL,
        mo_ta           NVARCHAR(MAX)   NULL,
        hinh_anh_chinh  NVARCHAR(500)   NULL,
        loai_san_pham   VARCHAR(20)     NOT NULL
            CONSTRAINT CK_sp_loaisanpham CHECK (loai_san_pham IN ('LAPTOP', 'PHU_KIEN', 'DIEN_THOAI')),
        trang_thai      NVARCHAR(20)    NOT NULL DEFAULT N'active'
            CONSTRAINT CK_san_pham_trangthai CHECK (trang_thai IN (N'active', N'inactive', N'ngung_kinh_doanh')),
        da_xoa          BIT             NOT NULL DEFAULT 0,
        ngay_tao        DATETIME        NOT NULL DEFAULT GETDATE(),
        ngay_cap_nhat   DATETIME        NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_san_pham_thuong_hieu  FOREIGN KEY (thuong_hieu_id)  REFERENCES thuong_hieu(thuong_hieu_id),
        CONSTRAINT FK_san_pham_danh_muc     FOREIGN KEY (danh_muc_id)     REFERENCES danh_muc(danh_muc_id),
        CONSTRAINT FK_san_pham_nha_cung_cap FOREIGN KEY (nha_cung_cap_id) REFERENCES nha_cung_cap(nha_cung_cap_id)
    );
END
GO

-- Mã sản phẩm & barcode phải duy nhất, nhưng phải cho phép NHIỀU dòng NULL (sản phẩm chưa
-- gán mã / chưa dán tem) → dùng filtered unique index; UNIQUE constraint của SQL Server chỉ
-- chấp nhận đúng 1 giá trị NULL nên không dùng được ở đây. Index này cũng giúp truy vấn
-- quét barcode (WHERE barcode = ?) seek thẳng thay vì quét cả bảng.
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_san_pham_ma')
    CREATE UNIQUE INDEX UX_san_pham_ma ON san_pham(ma_san_pham) WHERE ma_san_pham IS NOT NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_san_pham_barcode')
    CREATE UNIQUE INDEX UX_san_pham_barcode ON san_pham(barcode) WHERE barcode IS NOT NULL;
GO

-- Junction table: 1 sản phẩm có thể thuộc nhiều phân loại (nhiều-nhiều)
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'san_pham_phan_loai')
BEGIN
    CREATE TABLE san_pham_phan_loai (
        san_pham_id  INT NOT NULL,
        phan_loai_id INT NOT NULL,
        PRIMARY KEY (san_pham_id, phan_loai_id),
        CONSTRAINT FK_sppl_san_pham  FOREIGN KEY (san_pham_id)  REFERENCES san_pham(san_pham_id) ON DELETE CASCADE,
        CONSTRAINT FK_sppl_phan_loai FOREIGN KEY (phan_loai_id) REFERENCES phan_loai(phan_loai_id)
    );
END
GO

-- Danh mục CPU/RAM/ổ cứng/GPU — lưu LOẠI linh kiện, không phải đơn vị vật lý
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'dm_cpu')
    CREATE TABLE dm_cpu    ( cpu_id    INT IDENTITY(1,1) PRIMARY KEY, ten_cpu     NVARCHAR(100) NOT NULL UNIQUE );
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'dm_ram')
    CREATE TABLE dm_ram    ( ram_id    INT IDENTITY(1,1) PRIMARY KEY, dung_luong  NVARCHAR(50)  NOT NULL UNIQUE );
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'dm_o_cung')
    CREATE TABLE dm_o_cung ( o_cung_id INT IDENTITY(1,1) PRIMARY KEY, loai_o_cung NVARCHAR(100) NOT NULL UNIQUE );
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'dm_gpu')
    CREATE TABLE dm_gpu    ( gpu_id    INT IDENTITY(1,1) PRIMARY KEY, ten_gpu     NVARCHAR(100) NOT NULL UNIQUE );
GO

-- ============================================================
--  4. BIẾN THỂ SẢN PHẨM (ĐỊNH GIÁ & THÔNG SỐ KỸ THUẬT)
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'bien_the_san_pham')
BEGIN
    CREATE TABLE bien_the_san_pham (
        bien_the_id         INT             IDENTITY(1,1) PRIMARY KEY,
        san_pham_id         INT             NOT NULL,
        ma_sku              VARCHAR(50)     NOT NULL UNIQUE,
        gia_nhap            DECIMAL(18,0)   NOT NULL CONSTRAINT CK_bt_gianhap CHECK (gia_nhap >= 0),
        gia_ban             DECIMAL(18,0)   NOT NULL CONSTRAINT CK_bt_giaban  CHECK (gia_ban  >= 0),
        CONSTRAINT CK_bt_giaban_hop_ly CHECK (gia_ban >= gia_nhap * 0.5),
        bao_hanh_thang      INT             NOT NULL DEFAULT 24 CONSTRAINT CK_bt_baohanh CHECK (bao_hanh_thang >= 0),
        hinh_anh_bien_the   NVARCHAR(500)   NULL,
        trang_thai          NVARCHAR(20)    NOT NULL DEFAULT N'active'
            CONSTRAINT CK_bt_trangthai CHECK (trang_thai IN (N'active', N'inactive')),
        da_xoa              BIT             NOT NULL DEFAULT 0,
        mau_sac             NVARCHAR(50)    NULL,

        -- Thông số kỹ thuật Laptop
        cpu_id              INT             NULL,
        ram_id              INT             NULL,
        o_cung_id           INT             NULL,
        gpu_id              INT             NULL,
        kich_thuoc_man_hinh NVARCHAR(50)    NULL,
        he_dieu_hanh        NVARCHAR(100)   NULL,
        pin                 NVARCHAR(50)    NULL,
        trong_luong_kg      DECIMAL(5,2)    NULL,

        -- Phân loại theo mục đích sử dụng — cache từ san_pham_phan_loai để filter nhanh
        -- VD: 'gaming,do_hoa' | 'van_phong,sinh_vien'
        phan_loai_tags      NVARCHAR(200)   NULL,
        phan_loai_ten       NVARCHAR(200)   NULL,
        ngay_tao            DATETIME        NOT NULL DEFAULT GETDATE(),
        ngay_cap_nhat       DATETIME        NOT NULL DEFAULT GETDATE(),

        CONSTRAINT FK_bien_the_san_pham FOREIGN KEY (san_pham_id) REFERENCES san_pham(san_pham_id) ON DELETE CASCADE,
        CONSTRAINT FK_bien_the_cpu      FOREIGN KEY (cpu_id)      REFERENCES dm_cpu(cpu_id),
        CONSTRAINT FK_bien_the_ram      FOREIGN KEY (ram_id)      REFERENCES dm_ram(ram_id),
        CONSTRAINT FK_bien_the_ocung    FOREIGN KEY (o_cung_id)   REFERENCES dm_o_cung(o_cung_id),
        CONSTRAINT FK_bien_the_gpu      FOREIGN KEY (gpu_id)      REFERENCES dm_gpu(gpu_id)
    );
END
GO

-- ============================================================
--  5. KHO HÀNG: TỒN KHO & TỪNG ĐƠN VỊ VẬT LÝ (CÓ SERIAL)
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'ton_kho')
BEGIN
    CREATE TABLE ton_kho (
        cau_hinh_id          INT      IDENTITY(1,1) PRIMARY KEY,
        bien_the_id          INT      NOT NULL UNIQUE,
        so_luong_ton_thuc_te INT      NOT NULL DEFAULT 0 CONSTRAINT CK_chtk_tonthucte CHECK (so_luong_ton_thuc_te >= 0),
        so_luong_giu         INT      NOT NULL DEFAULT 0 CONSTRAINT CK_chtk_giu        CHECK (so_luong_giu >= 0),
        ton_kho_toi_thieu    INT      NOT NULL DEFAULT 5  CONSTRAINT CK_chtk_toithieu  CHECK (ton_kho_toi_thieu >= 0),
        CONSTRAINT CK_chtk_giu_le_ton CHECK (so_luong_giu <= so_luong_ton_thuc_te),
        ngay_tao             DATETIME NOT NULL DEFAULT GETDATE(),
        ngay_cap_nhat        DATETIME NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_ton_kho_bt FOREIGN KEY (bien_the_id) REFERENCES bien_the_san_pham(bien_the_id) ON DELETE CASCADE
    );
END
GO

-- Mỗi hàng = 1 đơn vị laptop/phụ kiện vật lý, nhận dạng qua số serial
-- so_serial: bắt buộc (NOT NULL), in trên máy hoặc hộp đóng gói
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'chi_tiet_san_pham')
BEGIN
    CREATE TABLE chi_tiet_san_pham (
        chi_tiet_id   INT           IDENTITY(1,1) PRIMARY KEY,
        bien_the_id   INT           NOT NULL,
        so_serial     VARCHAR(100)  NOT NULL,
        trang_thai    NVARCHAR(30)  NOT NULL DEFAULT N'trong_kho'
            CONSTRAINT CK_ctsp_trangthai CHECK (trang_thai IN (N'trong_kho', N'giu_hang', N'da_ban', N'loi_bao_hanh', N'da_tra_hang')),
        ngay_nhap_kho DATETIME      NOT NULL DEFAULT GETDATE(),
        ghi_chu       NVARCHAR(255) NULL,
        CONSTRAINT FK_ctsp_bien_the FOREIGN KEY (bien_the_id) REFERENCES bien_the_san_pham(bien_the_id) ON DELETE CASCADE
    );
END
GO

-- Serial phải duy nhất toàn hệ thống
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_ctsp_serial')
    CREATE UNIQUE INDEX UX_ctsp_serial ON chi_tiet_san_pham(so_serial);
GO

-- Serial linh kiện rời (CPU/RAM/GPU/Ổ cứng) — CHỈ để truy vết bảo hành/nhập kho nội bộ,
-- KHÔNG bán rời (không có giá bán, không gắn đơn hàng) nên trạng thái khác chi_tiet_san_pham:
-- trong_kho (còn hàng) / da_su_dung (đã lắp vào máy, không theo dõi lắp vào máy nào cụ thể)
-- / loi_bao_hanh (lỗi, cần đổi trả nhà cung cấp).
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'chi_tiet_cpu')
BEGIN
    CREATE TABLE chi_tiet_cpu (
        chi_tiet_cpu_id INT           IDENTITY(1,1) PRIMARY KEY,
        cpu_id          INT           NOT NULL,
        so_serial       VARCHAR(100)  NOT NULL,
        trang_thai      NVARCHAR(30)  NOT NULL DEFAULT N'trong_kho'
            CONSTRAINT CK_ctcpu_trangthai CHECK (trang_thai IN (N'trong_kho', N'da_su_dung', N'loi_bao_hanh')),
        ngay_nhap_kho   DATETIME      NOT NULL DEFAULT GETDATE(),
        ghi_chu         NVARCHAR(255) NULL,
        CONSTRAINT FK_ctcpu_cpu FOREIGN KEY (cpu_id) REFERENCES dm_cpu(cpu_id) ON DELETE CASCADE
    );
END
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_ctcpu_serial')
    CREATE UNIQUE INDEX UX_ctcpu_serial ON chi_tiet_cpu(so_serial);
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'chi_tiet_ram')
BEGIN
    CREATE TABLE chi_tiet_ram (
        chi_tiet_ram_id INT           IDENTITY(1,1) PRIMARY KEY,
        ram_id          INT           NOT NULL,
        so_serial       VARCHAR(100)  NOT NULL,
        trang_thai      NVARCHAR(30)  NOT NULL DEFAULT N'trong_kho'
            CONSTRAINT CK_ctram_trangthai CHECK (trang_thai IN (N'trong_kho', N'da_su_dung', N'loi_bao_hanh')),
        ngay_nhap_kho   DATETIME      NOT NULL DEFAULT GETDATE(),
        ghi_chu         NVARCHAR(255) NULL,
        CONSTRAINT FK_ctram_ram FOREIGN KEY (ram_id) REFERENCES dm_ram(ram_id) ON DELETE CASCADE
    );
END
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_ctram_serial')
    CREATE UNIQUE INDEX UX_ctram_serial ON chi_tiet_ram(so_serial);
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'chi_tiet_gpu')
BEGIN
    CREATE TABLE chi_tiet_gpu (
        chi_tiet_gpu_id INT           IDENTITY(1,1) PRIMARY KEY,
        gpu_id          INT           NOT NULL,
        so_serial       VARCHAR(100)  NOT NULL,
        trang_thai      NVARCHAR(30)  NOT NULL DEFAULT N'trong_kho'
            CONSTRAINT CK_ctgpu_trangthai CHECK (trang_thai IN (N'trong_kho', N'da_su_dung', N'loi_bao_hanh')),
        ngay_nhap_kho   DATETIME      NOT NULL DEFAULT GETDATE(),
        ghi_chu         NVARCHAR(255) NULL,
        CONSTRAINT FK_ctgpu_gpu FOREIGN KEY (gpu_id) REFERENCES dm_gpu(gpu_id) ON DELETE CASCADE
    );
END
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_ctgpu_serial')
    CREATE UNIQUE INDEX UX_ctgpu_serial ON chi_tiet_gpu(so_serial);
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'chi_tiet_o_cung')
BEGIN
    CREATE TABLE chi_tiet_o_cung (
        chi_tiet_o_cung_id INT           IDENTITY(1,1) PRIMARY KEY,
        o_cung_id          INT           NOT NULL,
        so_serial          VARCHAR(100)  NOT NULL,
        trang_thai         NVARCHAR(30)  NOT NULL DEFAULT N'trong_kho'
            CONSTRAINT CK_ctocung_trangthai CHECK (trang_thai IN (N'trong_kho', N'da_su_dung', N'loi_bao_hanh')),
        ngay_nhap_kho      DATETIME      NOT NULL DEFAULT GETDATE(),
        ghi_chu            NVARCHAR(255) NULL,
        CONSTRAINT FK_ctocung_ocung FOREIGN KEY (o_cung_id) REFERENCES dm_o_cung(o_cung_id) ON DELETE CASCADE
    );
END
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_ctocung_serial')
    CREATE UNIQUE INDEX UX_ctocung_serial ON chi_tiet_o_cung(so_serial);
GO

-- ============================================================
--  6. KHUYẾN MÃI & ĐỊA CHỈ GIAO HÀNG
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'khuyen_mai')
BEGIN
    CREATE TABLE khuyen_mai (
        khuyen_mai_id      INT            IDENTITY(1,1) PRIMARY KEY,
        ma_khuyen_mai      VARCHAR(50)    NOT NULL UNIQUE,
        ten_khuyen_mai     NVARCHAR(150)  NOT NULL,
        loai               NVARCHAR(20)   NOT NULL CONSTRAINT CK_km_loai CHECK (loai IN (N'percent', N'fixed')),
        gia_tri            DECIMAL(18,0)  NOT NULL CONSTRAINT CK_km_giatri CHECK (gia_tri > 0),
        CONSTRAINT CK_km_percent_max100 CHECK (loai <> N'percent' OR gia_tri <= 100),
        gia_tri_toi_da     DECIMAL(18,0)  NULL,
        don_hang_toi_thieu DECIMAL(18,0)  NULL,
        ngay_bat_dau       DATETIME       NOT NULL,
        ngay_ket_thuc      DATETIME       NOT NULL,
        so_luong_toi_da    INT            NULL,
        so_lan_da_dung     INT            NOT NULL DEFAULT 0 CONSTRAINT CK_km_solan CHECK (so_lan_da_dung >= 0),
        trang_thai         NVARCHAR(20)   NOT NULL DEFAULT N'active'
            CONSTRAINT CK_km_trangthai CHECK (trang_thai IN (N'active', N'inactive', N'het_han')),
        ngay_tao           DATETIME       NOT NULL DEFAULT GETDATE(),
        CONSTRAINT CK_km_ngay CHECK (ngay_ket_thuc > ngay_bat_dau)
    );
END

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'dia_chi_giao_hang')
BEGIN
    CREATE TABLE dia_chi_giao_hang (
        dia_chi_id        INT            IDENTITY(1,1) PRIMARY KEY,
        khach_hang_id     INT            NOT NULL,
        ho_ten_nguoi_nhan NVARCHAR(150)  NOT NULL,
        so_dien_thoai     VARCHAR(20)    NOT NULL,
        dia_chi           NVARCHAR(255)  NOT NULL,
        thanh_pho         NVARCHAR(100)  NULL,
        tinh              NVARCHAR(100)  NULL,
        la_mac_dinh       BIT            NOT NULL DEFAULT 0,
        ngay_tao          DATETIME       NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_dcgh_khach_hang FOREIGN KEY (khach_hang_id) REFERENCES khach_hang(khach_hang_id)
    );
END
GO

-- ============================================================
--  7. PHIẾU NHẬP KHO & ĐƠN HÀNG
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'phieu_nhap_kho')
BEGIN
    CREATE TABLE phieu_nhap_kho (
        phieu_nhap_id   INT            IDENTITY(1,1) PRIMARY KEY,
        ma_phieu_nhap   VARCHAR(50)    NOT NULL UNIQUE DEFAULT UPPER(LEFT(REPLACE(CAST(NEWID() AS VARCHAR(36)), '-', ''), 12)),
        nha_cung_cap_id INT            NOT NULL,
        nhan_vien_id    INT            NULL,
        ngay_nhap       DATETIME       NOT NULL DEFAULT GETDATE(),
        tong_tien       DECIMAL(18,0)  NOT NULL DEFAULT 0 CONSTRAINT CK_pnk_tongtien CHECK (tong_tien >= 0),
        trang_thai      NVARCHAR(20)   NOT NULL DEFAULT N'hoan_thanh'
            CONSTRAINT CK_phieu_nhap_trangthai CHECK (trang_thai IN (N'cho_duyet', N'hoan_thanh', N'huy')),
        ghi_chu         NVARCHAR(500)  NULL,
        CONSTRAINT FK_phieu_nhap_ncc       FOREIGN KEY (nha_cung_cap_id) REFERENCES nha_cung_cap(nha_cung_cap_id),
        CONSTRAINT FK_phieu_nhap_nhan_vien FOREIGN KEY (nhan_vien_id)    REFERENCES nhan_vien(nhan_vien_id)
    );
END

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'chi_tiet_phieu_nhap')
BEGIN
    CREATE TABLE chi_tiet_phieu_nhap (
        chi_tiet_nhap_id INT            IDENTITY(1,1) PRIMARY KEY,
        phieu_nhap_id    INT            NOT NULL,
        bien_the_id      INT            NOT NULL,
        so_luong         INT            NOT NULL CONSTRAINT CK_ctpn_soluong CHECK (so_luong > 0),
        don_gia_nhap     DECIMAL(18,0)  NOT NULL CONSTRAINT CK_ctpn_dongia  CHECK (don_gia_nhap >= 0),
        thanh_tien       AS (so_luong * don_gia_nhap) PERSISTED,
        CONSTRAINT FK_ctpn_phieu_nhap FOREIGN KEY (phieu_nhap_id) REFERENCES phieu_nhap_kho(phieu_nhap_id),
        CONSTRAINT FK_ctpn_bien_the   FOREIGN KEY (bien_the_id)   REFERENCES bien_the_san_pham(bien_the_id)
    );
END

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'don_hang')
BEGIN
    CREATE TABLE don_hang (
        don_hang_id            INT            IDENTITY(1,1) PRIMARY KEY,
        ma_don_hang            VARCHAR(50)    NOT NULL UNIQUE DEFAULT UPPER(LEFT(REPLACE(CAST(NEWID() AS VARCHAR(36)), '-', ''), 12)),
        khach_hang_id          INT            NOT NULL,
        nhan_vien_id           INT            NULL,
        khuyen_mai_id          INT            NULL,
        dia_chi_giao_hang_id   INT            NULL,
        -- Snapshot địa chỉ tại thời điểm đặt — không bị ảnh hưởng khi KH đổi địa chỉ sau đó
        dia_chi_giao_hang_text NVARCHAR(255)  NULL,
        nguoi_nhan             NVARCHAR(150)  NULL,
        sdt_nguoi_nhan         VARCHAR(20)    NULL,

        tong_tien      DECIMAL(18,0)  NOT NULL DEFAULT 0 CONSTRAINT CK_dh_tongtien CHECK (tong_tien >= 0),
        giam_gia       DECIMAL(18,0)  NOT NULL DEFAULT 0 CONSTRAINT CK_dh_giamgia  CHECK (giam_gia  >= 0),
        phi_van_chuyen DECIMAL(18,0)  NOT NULL DEFAULT 0 CONSTRAINT CK_dh_phi      CHECK (phi_van_chuyen >= 0),
        thanh_tien     AS (tong_tien - giam_gia + phi_van_chuyen) PERSISTED,

        ngay_dat             DATETIME       NOT NULL DEFAULT GETDATE(),
        ngay_giao_du_kien    DATETIME       NULL,
        ngay_giao_thuc_te    DATETIME       NULL,
        ngay_tao             DATETIME       NOT NULL DEFAULT GETDATE(),
        ngay_cap_nhat        DATETIME       NOT NULL DEFAULT GETDATE(),

        trang_thai_don_hang  NVARCHAR(30)   NOT NULL DEFAULT N'pending'
            CONSTRAINT CK_dh_trangthai CHECK (trang_thai_don_hang IN (N'pending', N'confirmed', N'processing', N'shipping', N'delivered', N'cancelled', N'returned')),
        trang_thai_thanh_toan NVARCHAR(30)  NOT NULL DEFAULT N'unpaid'
            CONSTRAINT CK_dh_ttthanhtoan CHECK (trang_thai_thanh_toan IN (N'unpaid', N'partial', N'paid', N'refunded')),

        kenh_ban  NVARCHAR(50)  NULL
            CONSTRAINT CK_dh_kenhban CHECK (kenh_ban IN (N'online', N'in_store', N'phone', N'social_media') OR kenh_ban IS NULL),
        ghi_chu   NVARCHAR(500) NULL,

        CONSTRAINT FK_dh_khach_hang        FOREIGN KEY (khach_hang_id)        REFERENCES khach_hang(khach_hang_id),
        CONSTRAINT FK_dh_nhan_vien         FOREIGN KEY (nhan_vien_id)         REFERENCES nhan_vien(nhan_vien_id),
        CONSTRAINT FK_dh_khuyen_mai        FOREIGN KEY (khuyen_mai_id)        REFERENCES khuyen_mai(khuyen_mai_id),
        CONSTRAINT FK_dh_dia_chi_giao_hang FOREIGN KEY (dia_chi_giao_hang_id) REFERENCES dia_chi_giao_hang(dia_chi_id)
    );
END
GO

-- ============================================================
--  8. CHI TIẾT ĐƠN HÀNG & LỊCH SỬ KHO
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'chi_tiet_don_hang')
BEGIN
    CREATE TABLE chi_tiet_don_hang (
        chi_tiet_don_hang_id INT            IDENTITY(1,1) PRIMARY KEY,
        don_hang_id          INT            NOT NULL,
        bien_the_id          INT            NOT NULL,
        chi_tiet_id          INT            NULL,  -- FK đến máy vật lý cụ thể (serial)
        so_luong             INT            NOT NULL CONSTRAINT CK_ctdh_soluong CHECK (so_luong > 0),
        don_gia              DECIMAL(18,0)  NOT NULL CONSTRAINT CK_ctdh_dongia  CHECK (don_gia >= 0),
        giam_gia_dong        DECIMAL(18,0)  NOT NULL DEFAULT 0,
        CONSTRAINT CK_ctdh_giamgia_hop_ly CHECK (giam_gia_dong >= 0 AND giam_gia_dong <= so_luong * don_gia),
        thanh_tien           AS (so_luong * don_gia - giam_gia_dong) PERSISTED,
        ghi_chu              NVARCHAR(255)  NULL,
        CONSTRAINT FK_ctdh_don_hang FOREIGN KEY (don_hang_id) REFERENCES don_hang(don_hang_id),
        CONSTRAINT FK_ctdh_bien_the FOREIGN KEY (bien_the_id) REFERENCES bien_the_san_pham(bien_the_id),
        CONSTRAINT FK_ctdh_ctsp     FOREIGN KEY (chi_tiet_id) REFERENCES chi_tiet_san_pham(chi_tiet_id)
    );
END

-- Gắn nhiều serial cho 1 dòng đơn hàng — chi_tiet_don_hang.chi_tiet_id (FK đơn) chỉ giữ
-- được 1 serial đại diện, bảng này là nguồn đầy đủ khi so_luong > 1. Dùng cho cả 2 kênh
-- bán, nhưng chỉ đơn online thực sự cần luồng giữ chỗ ("giu_hang") -> chọn lại -> đóng gói.
-- IF NOT EXISTS: file này giờ chạy được thẳng (nhấn Execute) vào DB đã có sẵn dữ liệu từ
-- bản dump cũ, không chỉ vào DB trắng — bỏ qua nếu bảng đã tồn tại thay vì báo lỗi.
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'chi_tiet_don_hang_serial')
BEGIN
    CREATE TABLE chi_tiet_don_hang_serial (
        chi_tiet_don_hang_serial_id INT IDENTITY(1,1) PRIMARY KEY,
        chi_tiet_don_hang_id        INT NOT NULL,
        chi_tiet_id                 INT NOT NULL,
        CONSTRAINT FK_ctdhs_ctdh FOREIGN KEY (chi_tiet_don_hang_id) REFERENCES chi_tiet_don_hang(chi_tiet_don_hang_id) ON DELETE CASCADE,
        CONSTRAINT FK_ctdhs_ctsp FOREIGN KEY (chi_tiet_id)          REFERENCES chi_tiet_san_pham(chi_tiet_id),
        CONSTRAINT UX_ctdhs_pair UNIQUE (chi_tiet_don_hang_id, chi_tiet_id)
    );
END
GO

-- DB đã có sẵn bảng lich_su_ton_kho (CREATE TABLE bên dưới sẽ báo lỗi "already an object"
-- và không chạy) vẫn cần constraint cho phép "giu_hang" — áp dụng luôn ở đây, độc lập với
-- CREATE TABLE bên dưới, drop-rồi-add nên chạy lại bao nhiêu lần cũng an toàn.
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_lsdk_loai')
    ALTER TABLE lich_su_ton_kho DROP CONSTRAINT CK_lsdk_loai;
IF EXISTS (SELECT 1 FROM sys.tables WHERE name = 'lich_su_ton_kho')
    ALTER TABLE lich_su_ton_kho ADD CONSTRAINT CK_lsdk_loai
        CHECK (loai_bien_dong IN (N'nhap', N'xuat_ban', N'tra_hang', N'dieu_chinh', N'huy', N'giu_hang'));
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'lich_su_ton_kho')
BEGIN
    CREATE TABLE lich_su_ton_kho (
        lich_su_id        INT            IDENTITY(1,1) PRIMARY KEY,
        bien_the_id       INT            NOT NULL,
        chi_tiet_id       INT            NULL,
        loai_bien_dong    NVARCHAR(30)   NOT NULL
            -- "giu_hang": giữ chỗ serial cho đơn online lúc đặt hàng, trước khi đóng gói chốt "da_ban".
            CONSTRAINT CK_lsdk_loai CHECK (loai_bien_dong IN (N'nhap', N'xuat_ban', N'tra_hang', N'dieu_chinh', N'huy', N'giu_hang')),
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
END
GO

-- ============================================================
--  9. THANH TOÁN & TRẢ HÀNG & BẢO HÀNH
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'thanh_toan')
BEGIN
    CREATE TABLE thanh_toan (
        thanh_toan_id          INT            IDENTITY(1,1) PRIMARY KEY,
        don_hang_id            INT            NOT NULL,
        ngay_thanh_toan        DATETIME       NOT NULL DEFAULT GETDATE(),
        phuong_thuc_thanh_toan NVARCHAR(50)   NOT NULL
            CONSTRAINT CK_tt_phuongthuc CHECK (phuong_thuc_thanh_toan IN (N'tien_mat', N'chuyen_khoan', N'the_tin_dung', N'momo', N'vnpay', N'zalopay', N'tra_gop', N'khac')),
        so_tien    DECIMAL(18,0)  NOT NULL CONSTRAINT CK_tt_sotien CHECK (so_tien > 0),
        ma_giao_dich VARCHAR(100) NULL,
        trang_thai NVARCHAR(30)   NOT NULL DEFAULT N'success'
            CONSTRAINT CK_tt_trangthai CHECK (trang_thai IN (N'success', N'failed', N'pending', N'refunded')),
        ghi_chu    NVARCHAR(255)  NULL,
        CONSTRAINT FK_tt_don_hang FOREIGN KEY (don_hang_id) REFERENCES don_hang(don_hang_id)
    );
END

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'phieu_tra_hang')
BEGIN
    CREATE TABLE phieu_tra_hang (
        phieu_tra_id  INT            IDENTITY(1,1) PRIMARY KEY,
        don_hang_id   INT            NOT NULL,
        nhan_vien_id  INT            NULL,
        ly_do         NVARCHAR(255)  NOT NULL,
        ngay_tra      DATETIME       NOT NULL DEFAULT GETDATE(),
        trang_thai    NVARCHAR(20)   NOT NULL DEFAULT N'cho_xu_ly'
            CONSTRAINT CK_pth_trangthai CHECK (trang_thai IN (N'cho_xu_ly', N'da_xu_ly', N'tu_choi')),
        so_tien_hoan  DECIMAL(18,0)  NOT NULL DEFAULT 0 CONSTRAINT CK_pth_tienhoan CHECK (so_tien_hoan >= 0),
        ghi_chu       NVARCHAR(500)  NULL,

        -- ma_phieu: generated, zero-padded 6 digits (e.g. TR-000123)
        ma_phieu AS ('TR-' + RIGHT('000000' + CONVERT(NVARCHAR(6), phieu_tra_id), 6)) PERSISTED,
        CONSTRAINT UQ_pth_ma_phieu UNIQUE (ma_phieu),

        CONSTRAINT FK_pth_don_hang  FOREIGN KEY (don_hang_id)  REFERENCES don_hang(don_hang_id),
        CONSTRAINT FK_pth_nhan_vien FOREIGN KEY (nhan_vien_id) REFERENCES nhan_vien(nhan_vien_id)
    );
END

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('khach_hang') AND name = 'so_du_vi')
BEGIN
    ALTER TABLE khach_hang ADD so_du_vi DECIMAL(18,0) NOT NULL DEFAULT 0
        CONSTRAINT CK_kh_sodu_vi CHECK (so_du_vi >= 0);
END

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('phieu_tra_hang') AND name = 'hinh_thuc_hoan')
BEGIN
    ALTER TABLE phieu_tra_hang ADD hinh_thuc_hoan NVARCHAR(20) NOT NULL DEFAULT N'vi'
        CONSTRAINT CK_pth_hinhthuchoan CHECK (hinh_thuc_hoan IN (N'tien_mat', N'vi'));
END
-- `ma_phieu` đã được định nghĩa là cột computed trong CREATE TABLE, vì vậy không cần ALTER/UPDATE ở đây.

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'chi_tiet_tra_hang')
BEGIN
    CREATE TABLE chi_tiet_tra_hang (
        chi_tiet_tra_id INT            IDENTITY(1,1) PRIMARY KEY,
        phieu_tra_id    INT            NOT NULL,
        bien_the_id     INT            NOT NULL,
        chi_tiet_id     INT            NULL,
        so_luong        INT            NOT NULL CONSTRAINT CK_ctth_soluong CHECK (so_luong > 0),
        don_gia_hoan    DECIMAL(18,0)  NOT NULL CONSTRAINT CK_ctth_dongia  CHECK (don_gia_hoan >= 0),
        tinh_trang      NVARCHAR(50)   NULL,
        CONSTRAINT FK_ctth_phieu_tra FOREIGN KEY (phieu_tra_id) REFERENCES phieu_tra_hang(phieu_tra_id),
        CONSTRAINT FK_ctth_bien_the  FOREIGN KEY (bien_the_id)  REFERENCES bien_the_san_pham(bien_the_id),
        CONSTRAINT FK_ctth_ctsp      FOREIGN KEY (chi_tiet_id)  REFERENCES chi_tiet_san_pham(chi_tiet_id)
    );
END

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'phieu_bao_hanh')
BEGIN
    CREATE TABLE phieu_bao_hanh (
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
        trang_thai        NVARCHAR(30)   NOT NULL DEFAULT N'con_bao_hanh'
            CONSTRAINT CK_pbh_trangthai CHECK (trang_thai IN (N'con_bao_hanh', N'dang_xu_ly', N'da_xu_ly', N'het_bao_hanh', N'tu_choi')),
        da_xoa            BIT            NOT NULL DEFAULT 0,
        chi_phi_phat_sinh DECIMAL(18,0)  NOT NULL DEFAULT 0 CONSTRAINT CK_pbh_chiphi CHECK (chi_phi_phat_sinh >= 0),
        ghi_chu           NVARCHAR(500)  NULL,
        CONSTRAINT FK_pbh_don_hang   FOREIGN KEY (don_hang_id)   REFERENCES don_hang(don_hang_id),
        CONSTRAINT FK_pbh_bien_the   FOREIGN KEY (bien_the_id)   REFERENCES bien_the_san_pham(bien_the_id),
        CONSTRAINT FK_pbh_khach_hang FOREIGN KEY (khach_hang_id) REFERENCES khach_hang(khach_hang_id),
        CONSTRAINT FK_pbh_ctsp       FOREIGN KEY (chi_tiet_id)   REFERENCES chi_tiet_san_pham(chi_tiet_id)
    );
END

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'san_pham_yeu_thich')
BEGIN
    CREATE TABLE san_pham_yeu_thich (
        yeu_thich_id  INT       IDENTITY(1,1) PRIMARY KEY,
        khach_hang_id INT       NOT NULL,
        bien_the_id   INT       NOT NULL,
        ngay_them     DATETIME  NOT NULL DEFAULT GETDATE(),

        -- 1 khach chi luu 1 bien the vao yeu thich 1 lan — trung thi bo (unlike), khong tao dong moi.
        CONSTRAINT UQ_spyt_kh_bt UNIQUE (khach_hang_id, bien_the_id),
        CONSTRAINT FK_spyt_khach_hang FOREIGN KEY (khach_hang_id) REFERENCES khach_hang(khach_hang_id),
        CONSTRAINT FK_spyt_bien_the   FOREIGN KEY (bien_the_id)   REFERENCES bien_the_san_pham(bien_the_id)
    );
END

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'danh_gia')
BEGIN
    CREATE TABLE danh_gia (
        danh_gia_id   INT            IDENTITY(1,1) PRIMARY KEY,
        khach_hang_id INT            NOT NULL,
        san_pham_id   INT            NOT NULL,
        don_hang_id   INT            NOT NULL,
        so_sao        INT            NOT NULL CONSTRAINT CK_dg_sosao CHECK (so_sao BETWEEN 1 AND 5),
        noi_dung      NVARCHAR(1000) NULL,
        ngay_danh_gia DATETIME       NOT NULL DEFAULT GETDATE(),

        -- 1 khach chi danh gia 1 san pham 1 lan (theo san_pham_id, khong phai tung bien_the/
        -- don_hang) — mua nhieu lan hoac nhieu cau hinh khac nhau cua cung 1 san pham khong
        -- tao them danh gia moi, tranh spam. don_hang_id luu lai don nao dung de xac minh
        -- "da mua" luc tao (xem DanhGiaService.themDanhGia).
        CONSTRAINT UQ_dg_kh_sp UNIQUE (khach_hang_id, san_pham_id),
        CONSTRAINT FK_dg_khach_hang FOREIGN KEY (khach_hang_id) REFERENCES khach_hang(khach_hang_id),
        CONSTRAINT FK_dg_san_pham   FOREIGN KEY (san_pham_id)   REFERENCES san_pham(san_pham_id),
        CONSTRAINT FK_dg_don_hang   FOREIGN KEY (don_hang_id)   REFERENCES don_hang(don_hang_id)
    );
END
GO

-- ============================================================
--  10. TRIGGERS
-- ============================================================

-- Trigger 1: Tự động cập nhật tồn kho khi thêm/đổi trạng thái/xóa đơn vị vật lý
IF OBJECT_ID('trg_CapNhatTonKhoThucTe', 'TR') IS NOT NULL
    DROP TRIGGER trg_CapNhatTonKhoThucTe;
GO

CREATE TRIGGER trg_CapNhatTonKhoThucTe
ON chi_tiet_san_pham
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @TmpTable TABLE (bien_the_id INT, bien_dong INT);

    -- INSERT mới → chỉ cộng nếu trạng thái là "trong_kho"
    IF EXISTS (SELECT 1 FROM inserted) AND NOT EXISTS (SELECT 1 FROM deleted)
    BEGIN
        INSERT INTO @TmpTable
        SELECT bien_the_id, COUNT(*)
        FROM inserted WHERE trang_thai = N'trong_kho'
        GROUP BY bien_the_id;
    END

    -- UPDATE trạng thái
    IF EXISTS (SELECT 1 FROM deleted) AND EXISTS (SELECT 1 FROM inserted)
    BEGIN
        -- Rời "trong_kho" → trừ tồn
        INSERT INTO @TmpTable
        SELECT d.bien_the_id, -COUNT(*)
        FROM deleted d JOIN inserted i ON d.chi_tiet_id = i.chi_tiet_id
        WHERE d.trang_thai = N'trong_kho' AND i.trang_thai <> N'trong_kho'
        GROUP BY d.bien_the_id;

        -- Quay về "trong_kho" (trả hàng / hoàn bảo hành) → cộng tồn
        INSERT INTO @TmpTable
        SELECT i.bien_the_id, COUNT(*)
        FROM deleted d JOIN inserted i ON d.chi_tiet_id = i.chi_tiet_id
        WHERE d.trang_thai <> N'trong_kho' AND i.trang_thai = N'trong_kho'
        GROUP BY i.bien_the_id;
    END

    -- DELETE đơn vị đang ở trong kho → trừ tồn
    IF EXISTS (SELECT 1 FROM deleted) AND NOT EXISTS (SELECT 1 FROM inserted)
    BEGIN
        INSERT INTO @TmpTable
        SELECT bien_the_id, -COUNT(*)
        FROM deleted WHERE trang_thai = N'trong_kho'
        GROUP BY bien_the_id;
    END

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

-- Trigger 2: Kiểm tra khuyến mãi khi tạo đơn hàng
-- Chặn: voucher hết hạn | hết lượt | đơn chưa đủ giá trị tối thiểu
-- LƯU Ý: không dùng ROLLBACK TRANSACTION ở đây (anti-pattern trong AFTER INSERT trigger) —
-- nó làm lệch trạng thái transaction mà connection pool (Hibernate/JDBC) đang theo dõi,
-- khiến một số row bị mất ngầm sau khi client đã nhận response thành công, đặc biệt khi
-- insert hàng loạt. Chỉ RAISERROR rồi RETURN, để lỗi lan ra ngoài và Spring's @Transactional
-- tự rollback đúng cách theo exception.
IF OBJECT_ID('trg_KiemTra_KhuyenMai', 'TR') IS NOT NULL
    DROP TRIGGER trg_KiemTra_KhuyenMai;
GO

CREATE TRIGGER trg_KiemTra_KhuyenMai
ON don_hang
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1 FROM inserted i
        JOIN khuyen_mai km ON i.khuyen_mai_id = km.khuyen_mai_id
        WHERE km.trang_thai <> N'active'
           OR km.ngay_ket_thuc < GETDATE()
           OR km.ngay_bat_dau  > GETDATE()
    )
    BEGIN
        RAISERROR(N'Mã khuyến mãi không hợp lệ hoặc đã hết hạn sử dụng', 16, 1);
        RETURN;
    END

    IF EXISTS (
        SELECT 1 FROM inserted i
        JOIN khuyen_mai km ON i.khuyen_mai_id = km.khuyen_mai_id
        WHERE km.so_luong_toi_da IS NOT NULL AND km.so_lan_da_dung >= km.so_luong_toi_da
    )
    BEGIN
        RAISERROR(N'Mã khuyến mãi đã hết lượt sử dụng', 16, 1);
        RETURN;
    END

    IF EXISTS (
        SELECT 1 FROM inserted i
        JOIN khuyen_mai km ON i.khuyen_mai_id = km.khuyen_mai_id
        WHERE km.don_hang_toi_thieu IS NOT NULL AND i.tong_tien < km.don_hang_toi_thieu
    )
    BEGIN
        RAISERROR(N'Đơn hàng chưa đạt giá trị tối thiểu để áp dụng mã khuyến mãi', 16, 1);
        RETURN;
    END

    -- Tự động tăng số lần đã dùng
    UPDATE km
    SET so_lan_da_dung = so_lan_da_dung + 1
    FROM khuyen_mai km
    JOIN inserted i ON km.khuyen_mai_id = i.khuyen_mai_id
    WHERE i.khuyen_mai_id IS NOT NULL;
END;
GO

-- Trigger 3: Tự cập nhật ngay_cap_nhat khi đơn thay đổi
IF OBJECT_ID('trg_CapNhat_TrangThai_DonHang', 'TR') IS NOT NULL
    DROP TRIGGER trg_CapNhat_TrangThai_DonHang;
GO

CREATE TRIGGER trg_CapNhat_TrangThai_DonHang
ON don_hang
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE don_hang
    SET ngay_cap_nhat = GETDATE()
    FROM don_hang dh JOIN inserted i ON dh.don_hang_id = i.don_hang_id;
END;
GO

-- Trigger 4: Tự động sync phan_loai_tags vào bien_the_san_pham khi junction thay đổi
IF OBJECT_ID('trg_SyncPhanLoaiTags', 'TR') IS NOT NULL
    DROP TRIGGER trg_SyncPhanLoaiTags;
GO

CREATE TRIGGER trg_SyncPhanLoaiTags
ON san_pham_phan_loai
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    -- Lấy tất cả san_pham_id bị ảnh hưởng
    DECLARE @AffectedIds TABLE (san_pham_id INT);
    INSERT INTO @AffectedIds SELECT DISTINCT san_pham_id FROM inserted;
    INSERT INTO @AffectedIds SELECT DISTINCT san_pham_id FROM deleted;

    UPDATE bt
    SET
        bt.phan_loai_tags = (
            SELECT STRING_AGG(pl.ma_phan_loai, ',') WITHIN GROUP (ORDER BY pl.thu_tu)
            FROM san_pham_phan_loai sppl
            JOIN phan_loai pl ON sppl.phan_loai_id = pl.phan_loai_id
            WHERE sppl.san_pham_id = bt.san_pham_id AND pl.trang_thai = N'active'
        ),
        bt.phan_loai_ten = (
            SELECT STRING_AGG(pl.ten_phan_loai, ', ') WITHIN GROUP (ORDER BY pl.thu_tu)
            FROM san_pham_phan_loai sppl
            JOIN phan_loai pl ON sppl.phan_loai_id = pl.phan_loai_id
            WHERE sppl.san_pham_id = bt.san_pham_id AND pl.trang_thai = N'active'
        )
    FROM bien_the_san_pham bt
    WHERE bt.san_pham_id IN (SELECT san_pham_id FROM @AffectedIds);
END;
GO

-- ============================================================
--  11. INDEX TỐI ƯU HÓA TRUY VẤN
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_bien_the_san_pham')
    CREATE INDEX IX_bien_the_san_pham ON bien_the_san_pham(san_pham_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_bien_the_gia')
    CREATE INDEX IX_bien_the_gia ON bien_the_san_pham(gia_ban, gia_nhap);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_ctsp_bien_the')
    CREATE INDEX IX_ctsp_bien_the ON chi_tiet_san_pham(bien_the_id, trang_thai);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_don_hang_khach_hang')
    CREATE INDEX IX_don_hang_khach_hang ON don_hang(khach_hang_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_don_hang_ngay_dat')
    CREATE INDEX IX_don_hang_ngay_dat ON don_hang(ngay_dat DESC);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_don_hang_trang_thai')
    CREATE INDEX IX_don_hang_trang_thai ON don_hang(trang_thai_don_hang, ngay_dat DESC);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_ctdh_don_hang')
    CREATE INDEX IX_ctdh_don_hang ON chi_tiet_don_hang(don_hang_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_lstk_bien_the')
    CREATE INDEX IX_lstk_bien_the ON lich_su_ton_kho(bien_the_id, ngay_tao DESC);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_khuyen_mai_ma')
    CREATE INDEX IX_khuyen_mai_ma ON khuyen_mai(ma_khuyen_mai, trang_thai);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_sppl_phan_loai')
    CREATE INDEX IX_sppl_phan_loai ON san_pham_phan_loai(phan_loai_id);  -- filter nhanh theo phân loại
GO

-- ============================================================
--  12. VIEWS TỔNG HỢP
-- ============================================================

-- Tồn kho tổng quan — dùng cho màn hình quản lý kho
IF OBJECT_ID('vw_ton_kho_tong_quan', 'V') IS NOT NULL
    DROP VIEW vw_ton_kho_tong_quan;
GO

CREATE VIEW vw_ton_kho_tong_quan AS
SELECT
    sp.san_pham_id,
    bt.bien_the_id,
    bt.ma_sku,
    CONCAT(sp.ten_san_pham, CASE WHEN bt.mau_sac IS NOT NULL THEN N' (' + bt.mau_sac + N')' ELSE N'' END) AS ten_phien_ban,
    th.ten_thuong_hieu,
    dm.ten_danh_muc,
    tk.so_luong_ton_thuc_te,
    tk.so_luong_giu,
    (tk.so_luong_ton_thuc_te - tk.so_luong_giu) AS co_the_ban,
    tk.ton_kho_toi_thieu,
    CASE
        WHEN tk.so_luong_ton_thuc_te = 0                             THEN N'Hết hàng'
        WHEN tk.so_luong_ton_thuc_te <= tk.ton_kho_toi_thieu         THEN N'Sắp hết hàng'
        ELSE N'Sẵn hàng'
    END AS tinh_trang_kho,
    bt.gia_ban,
    bt.gia_nhap
FROM bien_the_san_pham bt
JOIN san_pham    sp ON bt.san_pham_id    = sp.san_pham_id
JOIN ton_kho     tk ON bt.bien_the_id    = tk.bien_the_id
JOIN thuong_hieu th ON sp.thuong_hieu_id = th.thuong_hieu_id
JOIN danh_muc    dm ON sp.danh_muc_id    = dm.danh_muc_id
WHERE bt.trang_thai = N'active';
GO

-- Danh sách sản phẩm cho trang khách hàng
IF OBJECT_ID('vw_san_pham_hien_thi', 'V') IS NOT NULL
    DROP VIEW vw_san_pham_hien_thi;
GO

CREATE VIEW vw_san_pham_hien_thi AS
SELECT
    sp.san_pham_id,
    bt.bien_the_id,
    bt.ma_sku,
    sp.ten_san_pham,
    sp.ma_san_pham,
    sp.barcode,
    ISNULL(bt.hinh_anh_bien_the, sp.hinh_anh_chinh) AS hinh_anh_chinh,
    th.ten_thuong_hieu,
    dm.ten_danh_muc,
    sp.loai_san_pham,
    bt.gia_ban,
    bt.gia_nhap,
    bt.bao_hanh_thang,
    bt.mau_sac,
    bt.trang_thai,
    ISNULL(tk.so_luong_ton_thuc_te, 0) - ISNULL(tk.so_luong_giu, 0) AS so_luong_co_the_ban,
    cpu.ten_cpu,
    ram.dung_luong,
    oc.loai_o_cung,
    gpu.ten_gpu,
    bt.kich_thuoc_man_hinh,
    bt.he_dieu_hanh,
    bt.pin,
    bt.trong_luong_kg,
    -- Danh sách phân loại dạng chuỗi: 'van_phong,sinh_vien' — dùng cho filter trên frontend
    (
        SELECT STRING_AGG(pl.ma_phan_loai, ',')
        FROM san_pham_phan_loai sppl
        JOIN phan_loai pl ON sppl.phan_loai_id = pl.phan_loai_id
        WHERE sppl.san_pham_id = sp.san_pham_id AND pl.trang_thai = N'active'
    ) AS phan_loai_tags,
    -- Tên hiển thị dạng chuỗi: 'Văn phòng,Sinh viên'
    (
        SELECT STRING_AGG(pl.ten_phan_loai, ', ')
        FROM san_pham_phan_loai sppl
        JOIN phan_loai pl ON sppl.phan_loai_id = pl.phan_loai_id
        WHERE sppl.san_pham_id = sp.san_pham_id AND pl.trang_thai = N'active'
    ) AS phan_loai_ten
FROM bien_the_san_pham bt
JOIN san_pham    sp  ON bt.san_pham_id    = sp.san_pham_id
JOIN thuong_hieu th  ON sp.thuong_hieu_id = th.thuong_hieu_id
JOIN danh_muc    dm  ON sp.danh_muc_id    = dm.danh_muc_id
LEFT JOIN ton_kho    tk  ON bt.bien_the_id = tk.bien_the_id
LEFT JOIN dm_cpu     cpu ON bt.cpu_id      = cpu.cpu_id
LEFT JOIN dm_ram     ram ON bt.ram_id      = ram.ram_id
LEFT JOIN dm_o_cung  oc  ON bt.o_cung_id  = oc.o_cung_id
LEFT JOIN dm_gpu     gpu ON bt.gpu_id      = gpu.gpu_id
WHERE sp.trang_thai = N'active' AND bt.trang_thai = N'active';
GO

-- ============================================================
--  13. DỮ LIỆU MẪU
-- ============================================================
-- KHÔNG còn guard "IF NOT EXISTS" hay sentinel tạm nào ở đây nữa — vì phần đầu file đã
-- luôn DROP + CREATE DATABASE mới toanh trước khi chạy tới đây, database CHẮC CHẮN rỗng
-- mỗi lần, nên không cần kiểm tra "đã seed chưa" nữa, chạy thẳng.
-- (Từng thử dùng 1 bảng tạm ##seed_run làm "cờ nhớ" xuyên suốt toàn bộ khối, nhưng cách
-- đó có rủi ro: nếu công cụ chạy file (SSMS...) bị ngắt/kết nối lại kết nối giữa chừng
-- — dễ xảy ra với script dài hàng nghìn dòng, nhiều batch GO — bảng tạm đó biến mất theo
-- phiên cũ, khiến toàn bộ phần seed còn lại bị bỏ qua âm thầm không báo lỗi. Bỏ hẳn guard
-- là cách chắc chắn nhất: không phụ thuộc gì vào việc kết nối có ổn định xuyên suốt hay
-- không.)
    INSERT INTO chuc_vu (ma_chuc_vu, ten_chuc_vu, cap_do, mo_ta) VALUES
    ('admin',      N'Admin',       9, N'Toàn quyền hệ thống'),
    ('nhan_vien',  N'Nhân viên',   1, N'Bán hàng, tư vấn sản phẩm'),
    ('quan_kho',   N'Quản kho',    2, N'Nhập kho, xuất kho, kiểm kê'),
    ('khach_hang', N'Khách hàng',  0, N'Khách hàng mua sắm');
    -- chuc_vu: admin=1, nhan_vien=2, quan_kho=3, khach_hang=4

    -- nhan_vien: chuc_vu admin=1, nhan_vien=2, quan_kho=3, khach_hang=4
    INSERT INTO nhan_vien (ho_ten, so_dien_thoai, email, chuc_vu_id, luong_co_ban) VALUES
    (N'Quản trị viên',   '0900000001', 'admin@saoclub.vn',       1,       0),
    (N'Nguyễn Văn An',   '0987654321', 'nhanvienan@sao.vn',      2, 8000000),
    (N'Trần Thị Bảo',    '0978112233', 'nhanvienbao@sao.vn',     2, 7500000),
    (N'Lê Văn Cường',    '0967223344', 'nhanviencuong@sao.vn',   3, 6500000),
    (N'Phạm Quốc Dũng',  '0956334455', 'nhanviendung@sao.vn',    2, 9000000);
    -- nhan_vien: Admin=1, An=2, Bao=3, Cuong=4, Dung=5

    INSERT INTO thuong_hieu (ten_thuong_hieu, quoc_gia, mo_ta) VALUES
    (N'Dell',    N'Mỹ',         N'Laptop cá nhân và doanh nghiệp, nổi tiếng với dòng XPS, Inspiron, Latitude'),
    (N'Apple',   N'Mỹ',         N'MacBook Pro/Air — laptop cao cấp chip Apple Silicon'),
    (N'Asus',    N'Đài Loan',   N'Laptop đa dạng: Vivobook, ZenBook, ROG, TUF Gaming'),
    (N'Lenovo',  N'Trung Quốc', N'ThinkPad, IdeaPad, Legion — hãng laptop lớn nhất thế giới'),
    (N'HP',      N'Mỹ',         N'Pavilion, Envy, Spectre, EliteBook — laptop cho mọi phân khúc'),
    (N'MSI',     N'Đài Loan',   N'Chuyên laptop gaming: Stealth, Raider, Katana, Titan'),
    (N'Acer',    N'Đài Loan',   N'Aspire, Swift, Nitro, Predator — laptop phổ thông và gaming');
    -- thuong_hieu: Dell=1,Apple=2,Asus=3,Lenovo=4,HP=5,MSI=6,Acer=7

    INSERT INTO danh_muc (ten_danh_muc, mo_ta) VALUES
    (N'Laptop',   N'Máy tính xách tay các loại'),
    (N'Phụ kiện', N'Chuột, bàn phím, túi laptop, hub, sạc dự phòng...');
    -- danh_muc: Laptop=1, Phu_kien=2

    INSERT INTO nha_cung_cap (ten_nha_cung_cap, so_dien_thoai, email, dia_chi, nguoi_lien_he, ma_so_thue) VALUES
    (N'Digiworld',   N'02862920999', N'trade@digiworld.com.vn', N'69 Võ Văn Tần, Q3, TP.HCM',    N'Nguyễn Hùng Việt',  N'0302598493'),
    (N'FPT Trading', N'02437820800', N'trading@fpt.com.vn',    N'Tòa nhà FPT, Cầu Giấy, Hà Nội', N'Nguyễn Hưng Cường', N'0101248141'),
    (N'Synnex FPT',  N'1800 6077',   N'laptop@synnex.vn',      N'391A Nam Kỳ Khởi Nghĩa, Q3, TP.HCM', N'Lê Minh Khoa', N'0302103976');
    -- nha_cung_cap: Digiworld=1, FPT=2, Synnex=3

    INSERT INTO khach_hang (ho_ten, so_dien_thoai, email, dia_chi, loai_khach, diem_tich_luy) VALUES
    (N'Nghiêm Việt Anh',   '0912345678',  'anh.nghiem@gmail.com',  N'123 Phố Huế, Hoàn Kiếm, Hà Nội',      N'ca_nhan',      50),
    (N'Trần Thị Bình',     '0901234567',  'binh.tran@gmail.com',   N'456 Nguyễn Trãi, Quận 5, TP.HCM',     N'ca_nhan',     100),
    (N'Lê Hoàng Cường',    '0912345000',  'cuong.le@gmail.com',    N'78 Đinh Tiên Hoàng, Quận 1, TP.HCM',  N'ca_nhan',     250),
    (N'Phạm Thị Duyên',    '0934567890',  'duyen.pham@gmail.com',  N'12 Trần Phú, Hải Châu, Đà Nẵng',      N'ca_nhan',      50),
    (N'Nguyễn Minh Đức',   '0956789012',  'duc.nguyen@gmail.com',  N'34 Hoàng Diệu, Hải Châu, Đà Nẵng',    N'ca_nhan',       0),
    (N'Cty Minh Anh Tech', '02838901234', 'purchase@minhanh.vn',   N'50 Lê Lợi, Quận 1, TP.HCM',           N'doanh_nghiep', 800),
    (N'Khách Hàng Demo',   '0900000002',  'demo@saoclub.vn',        N'123 Đường Demo, TP.HCM',              N'ca_nhan',       0);
    -- khach_hang: VietAnh=1, Binh=2, Cuong=3, Duyen=4, Duc=5, MinhAnh=6, Demo=7

    -- Sinh thêm 123 khách hàng nữa (tổng 130, gồm 7 khách "có tên" ở trên) — set-based,
    -- ghép Họ x Tên (15x15=225 cặp) lấy 123 cặp đầu, số điện thoại & email suy ra từ rn
    -- nên luôn duy nhất, không cần liệt kê tay từng dòng.
    ;WITH Ho(ho, ho_ascii) AS (
        SELECT * FROM (VALUES
            (N'Nguyễn','nguyen'),(N'Trần','tran'),(N'Lê','le'),(N'Phạm','pham'),(N'Hoàng','hoang'),
            (N'Huỳnh','huynh'),(N'Phan','phan'),(N'Vũ','vu'),(N'Đặng','dang'),(N'Bùi','bui'),
            (N'Đỗ','do'),(N'Hồ','ho'),(N'Ngô','ngo'),(N'Dương','duong'),(N'Lý','ly')
        ) v(ho, ho_ascii)
    ),
    Ten(ten, ten_ascii) AS (
        SELECT * FROM (VALUES
            (N'Minh','minh'),(N'Anh','anh'),(N'Trung','trung'),(N'Linh','linh'),(N'Cường','cuong'),
            (N'Tuấn','tuan'),(N'Thắng','thang'),(N'Hiếu','hieu'),(N'Nam','nam'),(N'Huy','huy'),
            (N'Sơn','son'),(N'Mai','mai'),(N'Yến','yen'),(N'Việt','viet'),(N'Ngọc','ngoc')
        ) v(ten, ten_ascii)
    ),
    Ganh AS (
        SELECT ROW_NUMBER() OVER (ORDER BY h.ho, t.ten) AS rn, h.ho, h.ho_ascii, t.ten, t.ten_ascii
        FROM Ho h CROSS JOIN Ten t
    )
    INSERT INTO khach_hang (ho_ten, so_dien_thoai, email, loai_khach, diem_tich_luy)
    SELECT TOP (123)
        g.ho + N' ' + g.ten,
        -- RIGHT('0000000' + số, 7): cách pad số 0 phía trước cho đủ 7 ký tự (T-SQL không
        -- có hàm LPAD như MySQL) — vd rn=5 → '0000005', ghép với '097' ra SĐT 10 số.
        '097' + RIGHT('0000000' + CAST(g.rn AS VARCHAR(7)), 7),
        LOWER(g.ten_ascii) + '.' + LOWER(g.ho_ascii) + CAST(g.rn AS VARCHAR(10)) + '@gmail.com',
        N'ca_nhan',
        ABS(CAST(CHECKSUM(NEWID()) AS BIGINT)) % 300
    FROM Ganh g
    ORDER BY g.rn;
    -- khach_hang: 8..130 sinh tự động

    -- ── Tài khoản đăng nhập ───────────────────────────────────────────────────────
    -- Mật khẩu tất cả: 123456  (BCrypt $2a$10$)
    -- chuc_vu : admin=1, nhan_vien=2, quan_kho=3, khach_hang=4
    -- nhan_vien: Admin=1, An=2, Bao=3, Cuong=4, Dung=5
    -- khach_hang: ..., Demo=7
    INSERT INTO tai_khoan (username, mat_khau_hash, chuc_vu_id, nhan_vien_id, khach_hang_id) VALUES
    ('admin',        '$2a$10$V3q/GGHrWTQ/9cju6ohqEe4HR8TlXWwHXI7R2/V47CTCpHIHwu4Ie', 1, 1, NULL),
    ('nhanvienan',   '$2a$10$V3q/GGHrWTQ/9cju6ohqEe4HR8TlXWwHXI7R2/V47CTCpHIHwu4Ie', 2, 2, NULL),
    ('nhanvienbao',  '$2a$10$V3q/GGHrWTQ/9cju6ohqEe4HR8TlXWwHXI7R2/V47CTCpHIHwu4Ie', 2, 3, NULL),
    ('nhanviencuong','$2a$10$V3q/GGHrWTQ/9cju6ohqEe4HR8TlXWwHXI7R2/V47CTCpHIHwu4Ie', 3, 4, NULL),
    ('khachhang',    '$2a$10$iLnae2KuCuZ.BOPeLXRzde8wEWsgkze93MIooTzcqYkN/hZJkojFu', 4, NULL, 7);
    -- tai_khoan: admin=1, nhanvienan=2, nhanvienbao=3, nhanviencuong=4, khachhang=5

    INSERT INTO dm_cpu (ten_cpu) VALUES
    (N'Intel Core i5-1235U'),
    (N'Intel Core i7-13620H'),
    (N'Intel Core i5-13420H'),
    (N'Intel Core i9-13900H'),
    (N'AMD Ryzen 5 7530U'),
    (N'AMD Ryzen 7 7745H'),
    (N'Intel Core i7-13700H');
    -- dm_cpu: 1-7

    INSERT INTO dm_ram (dung_luong) VALUES
    (N'8GB DDR4'),(N'16GB DDR5'),(N'32GB DDR5'),(N'8GB LPDDR5'),(N'16GB LPDDR5');
    -- dm_ram: 1-5

    INSERT INTO dm_o_cung (loai_o_cung) VALUES
    (N'256GB SSD'),(N'512GB SSD'),(N'1TB SSD'),(N'2TB SSD');
    -- dm_o_cung: 1-4

    INSERT INTO dm_gpu (ten_gpu) VALUES
    (N'Intel Iris Xe'),(N'NVIDIA RTX 4050'),(N'NVIDIA RTX 4060'),(N'NVIDIA RTX 4070'),(N'AMD Radeon 780M');
    -- dm_gpu: 1-5

    -- Phân loại theo mục đích sử dụng
    -- thu_tu: thứ tự xuất hiện trên filter bar của frontend
    INSERT INTO phan_loai (ma_phan_loai, ten_phan_loai, mo_ta, thu_tu) VALUES
    ('van_phong',  N'Văn phòng',       N'Mỏng nhẹ, pin bền, phù hợp công việc văn phòng hàng ngày',           1),
    ('sinh_vien',  N'Sinh viên',       N'Giá tốt, hiệu năng đủ dùng học tập, bền',                             2),
    ('gaming',     N'Gaming',          N'Card đồ họa rời, tản nhiệt mạnh, màn 144Hz+',                          3),
    ('do_hoa',     N'Đồ họa',          N'Màn chuẩn màu sRGB/DCI-P3, RAM lớn, phù hợp Photoshop/Premiere',      4),
    ('ky_thuat',   N'Kỹ thuật - AI',   N'Hiệu năng CPU/GPU cao cho lập trình, AI, kỹ thuật',                   5),
    ('macbook',    N'MacBook',         N'Apple Silicon, hệ sinh thái Apple, hiệu năng/watt tốt nhất thị trường',6),
    ('laptop_cu',  N'Laptop cũ',       N'Hàng refurbished còn bảo hành, đã kiểm tra kỹ',                        7);
    -- phan_loai: van_phong=1, sinh_vien=2, gaming=3, do_hoa=4, ky_thuat=5, macbook=6, laptop_cu=7

    -- Sản phẩm (chỉ LAPTOP)
    -- ma_san_pham: mã nội bộ hiển thị trên UI  |  barcode: EAN-13 (số minh hoạ, tiền tố 893 của VN)
    INSERT INTO san_pham (ma_san_pham, barcode, ten_san_pham, thuong_hieu_id, danh_muc_id, nha_cung_cap_id, loai_san_pham, mo_ta, hinh_anh_chinh) VALUES
    ('SP0001', '8934567000015', N'Dell Inspiron 15 3520',    1, 1, 1, 'LAPTOP', N'Laptop văn phòng phổ thông 15.6" FHD, pin 54Wh, trọng lượng 1.7kg',   N'/images/Dell Inspiron 15 3520.webp'),
    ('SP0002', '8934567000022', N'Asus Vivobook 15 X1504VA', 3, 1, 2, 'LAPTOP', N'Mỏng nhẹ văn phòng, màn 15.6" FHD 60Hz, pin 50Wh cả ngày',          N'/images/Asus Vivobook 15 X1504VA.webp'),
    ('SP0003', '8934567000039', N'Lenovo IdeaPad 5 Pro 16',  4, 1, 2, 'LAPTOP', N'Màn 2.5K 16" 120Hz, AMD Ryzen mạnh, vỏ nhôm bền',                   N'/images/Lenovo IdeaPad 5 Pro 16.webp'),
    ('SP0004', '8934567000046', N'HP Envy x360 16 2024',     5, 1, 1, 'LAPTOP', N'2-in-1 cao cấp, màn OLED 2.8K cảm ứng, chip Intel Gen 13',          N'/images/HP Envy x360 16 2024.webp'),
    ('SP0005', '8934567000053', N'MSI Stealth 15M B12U',     6, 1, 3, 'LAPTOP', N'Gaming mỏng nhẹ RTX 4050, màn 144Hz, trọng lượng chỉ 1.7kg',        N'/images/MSI Stealth 15M B12U.webp');
    -- san_pham: Dell=1, Asus=2, Lenovo=3, HP=4, MSI=5

    -- Biến thể sản phẩm
    INSERT INTO bien_the_san_pham
        (san_pham_id, ma_sku, gia_nhap, gia_ban, bao_hanh_thang,
         cpu_id, ram_id, o_cung_id, gpu_id,
         kich_thuoc_man_hinh, he_dieu_hanh, pin, trong_luong_kg, mau_sac)
    VALUES
    -- Dell Inspiron 15 (sp=1)
    (1,'DELL-3520-I5-8G',  13000000, 15490000, 24, 1,1,2,1, N'15.6" FHD 60Hz', N'Windows 11 Home', N'54Wh', 1.70, N'Xám Bạc'),
    (1,'DELL-3520-I7-16G', 15500000, 17990000, 24, 2,2,2,1, N'15.6" FHD 60Hz', N'Windows 11 Home', N'54Wh', 1.70, N'Đen'),
    -- Asus Vivobook 15 (sp=2)
    (2,'ASUS-X1504-I5-8G', 12500000, 14990000, 24, 3,1,2,5, N'15.6" FHD 60Hz', N'Windows 11 Home', N'50Wh', 1.70, N'Bạc'),
    (2,'ASUS-X1504-I7-16G',16000000, 19490000, 24, 7,2,2,5, N'15.6" FHD 60Hz', N'Windows 11 Home', N'50Wh', 1.70, N'Bạc'),
    -- Lenovo IdeaPad 5 Pro (sp=3)
    (3,'LENO-IP5P-R5-8G',  14500000, 17490000, 24, 5,1,2,5, N'16" 2.5K 120Hz', N'Windows 11 Home', N'75Wh', 1.85, N'Xám Bão'),
    (3,'LENO-IP5P-R7-16G', 18000000, 22490000, 24, 6,2,3,5, N'16" 2.5K 120Hz', N'Windows 11 Home', N'75Wh', 1.85, N'Xám Bão'),
    -- HP Envy x360 (sp=4)
    (4,'HP-ENVY-I7-16G',   22000000, 27490000, 24, 7,2,2,2, N'16" 2.8K OLED 120Hz', N'Windows 11 Home', N'86Wh', 2.10, N'Bạc Tự Nhiên'),
    (4,'HP-ENVY-I9-32G',   28000000, 34990000, 24, 4,3,3,3, N'16" 2.8K OLED 120Hz', N'Windows 11 Home', N'86Wh', 2.10, N'Bạc Tự Nhiên'),
    -- MSI Stealth 15M (sp=5)
    (5,'MSI-STL15-RTX4050',22500000, 27990000, 24, 7,2,2,2, N'15.6" FHD 144Hz', N'Windows 11 Home', N'52Wh', 1.70, N'Đen'),
    (5,'MSI-STL15-RTX4070',30000000, 37490000, 24, 7,2,3,4, N'15.6" QHD 240Hz', N'Windows 11 Home', N'52Wh', 1.70, N'Đen');
    -- bien_the: Dell_i5=1,Dell_i7=2, Asus_i5=3,Asus_i7=4, Leno_R5=5,Leno_R7=6, HP_i7=7,HP_i9=8, MSI_4050=9,MSI_4070=10

    -- Tồn kho ban đầu
    -- Dell (bien_the 1,2): khởi tạo 0, trigger tự cộng khi nhập serial bên dưới
    -- Các model khác: số liệu nhập thủ công (chưa đăng ký serial chi tiết)
    INSERT INTO ton_kho (bien_the_id, so_luong_ton_thuc_te, so_luong_giu, ton_kho_toi_thieu) VALUES
    ( 1,  0, 0, 5),  -- Dell i5 (trigger cộng)
    ( 2,  0, 0, 3),  -- Dell i7 (trigger cộng)
    ( 3, 10, 1, 5),  -- Asus i5
    ( 4,  7, 0, 3),  -- Asus i7
    ( 5, 12, 2, 5),  -- Lenovo R5
    ( 6,  5, 0, 3),  -- Lenovo R7
    ( 7,  8, 1, 4),  -- HP i7
    ( 8,  3, 0, 2),  -- HP i9
    ( 9,  6, 1, 3),  -- MSI RTX4050
    (10,  4, 0, 2);  -- MSI RTX4070
GO

    -- Đơn vị vật lý (serial) — trigger tự cộng ton_kho cho Dell
    INSERT INTO chi_tiet_san_pham (bien_the_id, so_serial, trang_thai) VALUES
    (1, 'SN-DELL-I5-001', N'trong_kho'),
    (1, 'SN-DELL-I5-002', N'trong_kho'),
    (1, 'SN-DELL-I5-003', N'trong_kho'),
    (2, 'SN-DELL-I7-001', N'trong_kho'),
    (2, 'SN-DELL-I7-002', N'trong_kho');
    -- Sau INSERT: ton_kho bien_the 1 = 3, bien_the 2 = 2

    -- Đánh dấu đã bán (trigger tự trừ tồn kho)
    UPDATE chi_tiet_san_pham SET trang_thai = N'da_ban' WHERE so_serial = 'SN-DELL-I5-001';
    -- ton_kho bien_the 1: 3 → 2
GO

    -- Địa chỉ giao hàng mặc định
    INSERT INTO dia_chi_giao_hang (khach_hang_id, ho_ten_nguoi_nhan, so_dien_thoai, dia_chi, thanh_pho, la_mac_dinh) VALUES
    (1, N'Nghiêm Việt Anh',   '0912345678',  N'123 Phố Huế, Hoàn Kiếm',    N'Hà Nội',          1),
    (2, N'Trần Thị Bình',     '0901234567',  N'456 Nguyễn Trãi, Quận 5',   N'TP. Hồ Chí Minh', 1),
    (3, N'Lê Hoàng Cường',    '0912345000',  N'78 Đinh Tiên Hoàng, Quận 1',N'TP. Hồ Chí Minh', 1),
    (4, N'Phạm Thị Duyên',    '0934567890',  N'12 Trần Phú, Hải Châu',     N'Đà Nẵng',         1),
    (5, N'Nguyễn Minh Đức',   '0956789012',  N'34 Hoàng Diệu, Hải Châu',   N'Đà Nẵng',         1),
    (6, N'Cty Minh Anh Tech', '02838901234', N'50 Lê Lợi, Quận 1',         N'TP. Hồ Chí Minh', 1);
GO

    -- Khuyến mãi
    -- so_lan_da_dung: số lần dùng trước khi insert sample data dưới đây
    -- VIP500 sẽ được trigger tăng thêm 1 khi DH3 (Lenovo) được chèn
    INSERT INTO khuyen_mai (ma_khuyen_mai, ten_khuyen_mai, loai, gia_tri, gia_tri_toi_da, don_hang_toi_thieu, ngay_bat_dau, ngay_ket_thuc, so_luong_toi_da, so_lan_da_dung, trang_thai) VALUES
    (N'SUMMER24',  N'Mùa hè 2024 - Giảm 10%',              N'percent', 10, 500000,  2000000, N'2024-06-01', N'2026-12-31', 200, 44, N'active'),
    (N'NEWCUST',   N'Khách hàng mới - Giảm 200.000đ',       N'fixed',  200000, NULL,  500000, N'2024-01-01', N'2026-12-31',1000,  7, N'active'),
    (N'LAPTOP20',  N'Laptop Festival - Giảm 20% tối đa 2tr',N'percent', 20,2000000,10000000, N'2024-08-01', N'2026-12-31',  50,  3, N'active'),
    (N'VIP500',    N'Khách VIP - Giảm 500.000đ',             N'fixed',  500000,NULL, 15000000, N'2024-01-01', N'2026-12-31', 100,  1, N'active'),
    (N'TECHFEST15',N'Tech Fest - Giảm 15%',                  N'percent', 15,1500000, 5000000, N'2024-07-01', N'2024-07-31',  50, 12, N'inactive');
    -- Sau trigger DH3 insert: VIP500.so_lan_da_dung = 2
GO

    -- Phiếu nhập kho
    -- P1: 5×13M + 3×15.5M = 65M + 46.5M = 111.500.000
    -- P2: 10×12.5M+7×16M+12×14.5M+5×18M+8×22M+3×28M+6×22.5M+4×30M = 1.016.000.000
    INSERT INTO phieu_nhap_kho (nha_cung_cap_id, nhan_vien_id, ngay_nhap, tong_tien, trang_thai, ghi_chu) VALUES
    (1, 4, N'2024-05-01',  111500000, N'hoan_thanh', N'Nhập hàng đợt 1 - Dell Inspiron 15 từ Digiworld'),
    (2, 4, N'2024-06-15', 1016000000, N'hoan_thanh', N'Nhập hàng đợt 2 - Asus, Lenovo, HP, MSI từ FPT Trading');
    -- phieu_nhap: P1=1, P2=2

    INSERT INTO chi_tiet_phieu_nhap (phieu_nhap_id, bien_the_id, so_luong, don_gia_nhap) VALUES
    -- P1: Dell
    (1,  1,  5, 13000000),
    (1,  2,  3, 15500000),
    -- P2: Asus, Lenovo, HP, MSI
    (2,  3, 10, 12500000), (2,  4,  7, 16000000),
    (2,  5, 12, 14500000), (2,  6,  5, 18000000),
    (2,  7,  8, 22000000), (2,  8,  3, 28000000),
    (2,  9,  6, 22500000), (2, 10,  4, 30000000);
GO

    -- Đơn hàng mẫu (chỉ laptop)
    -- thanh_tien = tong_tien - giam_gia + phi_van_chuyen (computed, tự tính)
    INSERT INTO don_hang
        (khach_hang_id, nhan_vien_id, khuyen_mai_id, nguoi_nhan, sdt_nguoi_nhan,
         dia_chi_giao_hang_text, tong_tien, giam_gia, phi_van_chuyen,
         ngay_dat, ngay_giao_thuc_te, trang_thai_don_hang, trang_thai_thanh_toan, kenh_ban)
    VALUES
    -- DH1: KH1 mua Dell i5 — đã giao, đã thanh toán, bán online
    (1, 2, NULL,
     N'Nghiêm Việt Anh', '0912345678', N'123 Phố Huế, Hoàn Kiếm, HN',
     15490000, 0, 30000,
     N'2024-06-10 10:30:00', N'2024-06-12 15:00:00', N'delivered', N'paid', N'online'),

    -- DH2: KH3 mua HP Envy i9 — đang xử lý, đặt cọc
    (3, NULL, NULL,
     N'Lê Hoàng Cường', '0912345000', N'78 Đinh Tiên Hoàng, Q1, TP.HCM',
     34990000, 0, 0,
     N'2024-07-05 14:20:00', NULL, N'processing', N'partial', N'online'),

    -- DH3: KH6 (doanh nghiệp) mua Lenovo R7 — km VIP500, đang giao, đã thanh toán
    (6, 3, 4,
     N'Cty Minh Anh Tech', '02838901234', N'50 Lê Lợi, Q1, TP.HCM',
     22490000, 500000, 0,
     N'2024-07-10 08:30:00', NULL, N'shipping', N'paid', N'online'),

    -- DH4: KH5 mua MSI RTX4050 — đã hủy (khách đổi ý)
    (5, NULL, NULL,
     N'Nguyễn Minh Đức', '0956789012', N'34 Hoàng Diệu, Hải Châu, ĐN',
     27990000, 0, 30000,
     N'2024-07-12 10:00:00', NULL, N'cancelled', N'unpaid', N'online');
    -- don_hang: DH1=1, DH2=2, DH3=3, DH4=4
    -- Trigger VIP500: DH3 insert → so_lan_da_dung tăng 1→2
GO

    -- Chi tiết đơn hàng
    INSERT INTO chi_tiet_don_hang (don_hang_id, bien_the_id, so_luong, don_gia, giam_gia_dong) VALUES
    (1,  1, 1, 15490000,      0),  -- DH1: Dell i5
    (2,  8, 1, 34990000,      0),  -- DH2: HP Envy i9
    (3,  6, 1, 22490000,      0),  -- DH3: Lenovo R7 (giảm giá ghi trong don_hang.giam_gia)
    (4,  9, 1, 27990000,      0);  -- DH4: MSI RTX4050 (hủy)
GO

    -- Thanh toán
    INSERT INTO thanh_toan (don_hang_id, ngay_thanh_toan, phuong_thuc_thanh_toan, so_tien, trang_thai, ghi_chu) VALUES
    (1, N'2024-06-10 10:35:00', N'chuyen_khoan', 15520000, N'success', N'CK Vietcombank — DH1 (bao gồm 30K phí ship)'),
    (2, N'2024-07-05 14:25:00', N'tien_mat',     10000000, N'success', N'Đặt cọc 10 triệu tiền mặt — DH2'),
    (3, N'2024-07-10 08:40:00', N'chuyen_khoan', 21990000, N'success', N'CK doanh nghiệp Minh Anh — DH3');
GO

    -- Gán phân loại cho từng sản phẩm (nhiều-nhiều)
    -- san_pham: Dell=1, Asus=2, Lenovo=3, HP=4, MSI=5
    -- phan_loai: van_phong=1, sinh_vien=2, gaming=3, do_hoa=4, ky_thuat=5, macbook=6
    INSERT INTO san_pham_phan_loai (san_pham_id, phan_loai_id) VALUES
    -- Dell Inspiron 15: văn phòng + sinh viên (giá phải chăng, dùng hàng ngày)
    (1, 1), (1, 2),
    -- Asus Vivobook 15: văn phòng + sinh viên (tương tự Dell, thị trường phổ thông)
    (2, 1), (2, 2),
    -- Lenovo IdeaPad 5 Pro: văn phòng + kỹ thuật (màn 2.5K, Ryzen mạnh, dân lập trình hay dùng)
    (3, 1), (3, 5),
    -- HP Envy x360: đồ họa + kỹ thuật (màn OLED chuẩn màu, 2-in-1 cao cấp)
    (4, 4), (4, 5),
    -- MSI Stealth 15M: gaming + đồ họa (RTX 4050/4070, render được video & 3D)
    (5, 3), (5, 4);
GO

    -- Sync cột phan_loai_tags / phan_loai_ten trong bien_the_san_pham từ junction table
    -- Chạy sau mỗi lần thay đổi san_pham_phan_loai (hoặc dùng trigger bên dưới)
    UPDATE bt
    SET
        bt.phan_loai_tags = (
            SELECT STRING_AGG(pl.ma_phan_loai, ',') WITHIN GROUP (ORDER BY pl.thu_tu)
            FROM san_pham_phan_loai sppl
            JOIN phan_loai pl ON sppl.phan_loai_id = pl.phan_loai_id
            WHERE sppl.san_pham_id = bt.san_pham_id
              AND pl.trang_thai = N'active'
        ),
        bt.phan_loai_ten = (
            SELECT STRING_AGG(pl.ten_phan_loai, ', ') WITHIN GROUP (ORDER BY pl.thu_tu)
            FROM san_pham_phan_loai sppl
            JOIN phan_loai pl ON sppl.phan_loai_id = pl.phan_loai_id
            WHERE sppl.san_pham_id = bt.san_pham_id
              AND pl.trang_thai = N'active'
        )
    FROM bien_the_san_pham bt;
GO

    -- Lịch sử xuất kho
    INSERT INTO lich_su_ton_kho (bien_the_id, loai_bien_dong, so_luong_thay_doi, don_hang_id, nhan_vien_id, ghi_chu) VALUES
    (1, N'xuat_ban', -1, 1, 2, N'Xuất bán DH1 - Dell Inspiron i5'),
    (8, N'xuat_ban', -1, 2, NULL, N'Xuất bán DH2 - HP Envy i9/32GB'),
    (6, N'xuat_ban', -1, 3, 3, N'Xuất bán DH3 - Lenovo R7/16GB');
GO

    -- ============================================================
    -- 13.B. DỮ LIỆU BỔ SUNG — Thêm sản phẩm mới + màu sắc
    -- ============================================================

    -- ── Sản phẩm mới (sp 6-11) ───────────────────────────────────────────────────
    -- thuong_hieu: Dell=1,Apple=2,Asus=3,Lenovo=4,HP=5,MSI=6,Acer=7
    -- nha_cung_cap: Digiworld=1, FPT=2, Synnex=3
    -- ma_san_pham / barcode: đánh tiếp từ SP0005 & dải barcode ở mục 13
    INSERT INTO san_pham (ma_san_pham, barcode, ten_san_pham, thuong_hieu_id, danh_muc_id, nha_cung_cap_id, loai_san_pham, mo_ta, hinh_anh_chinh) VALUES
    ('SP0006', '8934567000060', N'Acer Aspire 5 A515-58',   7, 1, 3, N'LAPTOP', N'Laptop học tập văn phòng phổ thông 15.6" FHD, pin 48Wh cả ngày, giá hợp lý',                 N'/images/Acer Aspire 5 A515-58.webp'),
    ('SP0007', '8934567000077', N'Asus ROG Strix G16 G614', 3, 1, 2, N'LAPTOP', N'Gaming cao cấp RTX 40 series, màn 16" 165Hz, tản nhiệt triple fan, RGB Aura Sync',            N'/images/Asus ROG Strix G16 G614.webp'),
    ('SP0008', '8934567000084', N'Lenovo Legion 5 Pro 16',  4, 1, 2, N'LAPTOP', N'Gaming-đồ họa chuyên nghiệp, màn WQXGA 165Hz, AMD Ryzen + NVIDIA RTX, vỏ nhôm',              N'/images/Lenovo Legion 5 Pro 16.webp'),
    ('SP0009', '8934567000091', N'HP Pavilion 15',          5, 1, 1, N'LAPTOP', N'Laptop gia đình phổ thông 15.6" FHD 144Hz, màu sắc đa dạng, giá cạnh tranh',                  N'/images/HP Pavilion 15.webp'),
    ('SP0010', '8934567000107', N'Dell XPS 15 9530',        1, 1, 1, N'LAPTOP', N'Màn OLED 3.5K siêu nét, thiết kế siêu mỏng, lý tưởng cho sáng tạo nội dung chuyên nghiệp',  N'/images/Dell XPS 15 9530.webp'),
    ('SP0011', '8934567000114', N'Acer Nitro V 15',         7, 1, 3, N'LAPTOP', N'Gaming tầm trung RTX 40 series, màn 144Hz, tản nhiệt mạnh, giá tốt nhất phân khúc',           N'/images/Acer Nitro V 15.webp');
    -- san_pham: Acer_Aspire5=6, ROG_Strix=7, Legion5Pro=8, Pavilion15=9, XPS15=10, NitroV=11
GO

    -- ── Biến thể bổ sung: thêm màu cho sp hiện có (1-5) ─────────────────────────
    -- dm_cpu: i5-1235U=1,i7-13620H=2,i5-13420H=3,i9-13900H=4,R5-7530U=5,R7-7745H=6,i7-13700H=7
    -- dm_ram: 8GB DDR4=1,16GB DDR5=2,32GB DDR5=3,8GB LPDDR5=4,16GB LPDDR5=5
    -- dm_o_cung: 256GB=1,512GB=2,1TB=3,2TB=4  |  dm_gpu: IrisXe=1,RTX4050=2,RTX4060=3,RTX4070=4,Radeon780M=5
    INSERT INTO bien_the_san_pham
        (san_pham_id, ma_sku, gia_nhap, gia_ban, bao_hanh_thang,
         cpu_id, ram_id, o_cung_id, gpu_id,
         kich_thuoc_man_hinh, he_dieu_hanh, pin, trong_luong_kg, mau_sac)
    VALUES
    -- Dell Inspiron 15 3520 (sp=1): Đen cho cấu hình i5
    (1,'DELL-3520-I5-8G-BLK',  13000000, 15490000, 24, 1,1,2,1, N'15.6" FHD 60Hz',      N'Windows 11 Home', N'54Wh', 1.70, N'Đen'),
    -- Asus Vivobook 15 (sp=2): Đen cho cả i5 & i7
    (2,'ASUS-X1504-I5-8G-BLK', 12500000, 14990000, 24, 3,1,2,5, N'15.6" FHD 60Hz',      N'Windows 11 Home', N'50Wh', 1.70, N'Đen'),
    (2,'ASUS-X1504-I7-16G-BLK',16000000, 19490000, 24, 7,2,2,5, N'15.6" FHD 60Hz',      N'Windows 11 Home', N'50Wh', 1.70, N'Đen'),
    -- Lenovo IdeaPad 5 Pro (sp=3): Xanh Dương cho R5 & R7
    (3,'LENO-IP5P-R5-8G-BLU',  14500000, 17490000, 24, 5,1,2,5, N'16" 2.5K 120Hz',      N'Windows 11 Home', N'75Wh', 1.85, N'Xanh Dương'),
    (3,'LENO-IP5P-R7-16G-BLU', 18000000, 22490000, 24, 6,2,3,5, N'16" 2.5K 120Hz',      N'Windows 11 Home', N'75Wh', 1.85, N'Xanh Dương'),
    -- HP Envy x360 (sp=4): Vàng Citrus cho i7, Đen Midnight cho i9
    (4,'HP-ENVY-I7-16G-GLD',   22000000, 27490000, 24, 7,2,2,2, N'16" 2.8K OLED 120Hz', N'Windows 11 Home', N'86Wh', 2.10, N'Vàng Citrus'),
    (4,'HP-ENVY-I9-32G-BLK',   28000000, 34990000, 24, 4,3,3,3, N'16" 2.8K OLED 120Hz', N'Windows 11 Home', N'86Wh', 2.10, N'Đen Midnight'),
    -- MSI Stealth 15M (sp=5): Bạc cho RTX4050
    (5,'MSI-STL15-RTX4050-SLV',22500000, 27990000, 24, 7,2,2,2, N'15.6" FHD 144Hz',     N'Windows 11 Home', N'52Wh', 1.70, N'Bạc');
    -- bien_the 11-18
GO

    -- ── Acer Aspire 5 A515-58 (sp=6) — 2 cấu hình × 2 màu ───────────────────────
    INSERT INTO bien_the_san_pham
        (san_pham_id, ma_sku, gia_nhap, gia_ban, bao_hanh_thang,
         cpu_id, ram_id, o_cung_id, gpu_id,
         kich_thuoc_man_hinh, he_dieu_hanh, pin, trong_luong_kg, mau_sac)
    VALUES
    (6,'ACER-A515-I5-8G-SLV', 11000000, 13490000, 12, 1,1,2,1, N'15.6" FHD 60Hz', N'Windows 11 Home', N'48Wh', 1.76, N'Bạc'),
    (6,'ACER-A515-I5-8G-BLU', 11000000, 13490000, 12, 1,1,2,1, N'15.6" FHD 60Hz', N'Windows 11 Home', N'48Wh', 1.76, N'Xanh Dương'),
    (6,'ACER-A515-I7-16G-SLV',14000000, 16990000, 12, 2,2,2,1, N'15.6" FHD 60Hz', N'Windows 11 Home', N'48Wh', 1.76, N'Bạc'),
    (6,'ACER-A515-I7-16G-BLK',14000000, 16990000, 12, 2,2,2,1, N'15.6" FHD 60Hz', N'Windows 11 Home', N'48Wh', 1.76, N'Đen');
    -- bien_the 19-22
GO

    -- ── Asus ROG Strix G16 G614 (sp=7) — 2 cấu hình, 3 biến thể ─────────────────
    INSERT INTO bien_the_san_pham
        (san_pham_id, ma_sku, gia_nhap, gia_ban, bao_hanh_thang,
         cpu_id, ram_id, o_cung_id, gpu_id,
         kich_thuoc_man_hinh, he_dieu_hanh, pin, trong_luong_kg, mau_sac)
    VALUES
    (7,'ROG-G614-I7-16G-BLK', 26000000, 32490000, 24, 7,2,2,3, N'16" FHD 165Hz',  N'Windows 11 Home', N'90Wh', 2.50, N'Đen'),
    (7,'ROG-G614-I7-16G-GRY', 26000000, 32490000, 24, 7,2,2,3, N'16" FHD 165Hz',  N'Windows 11 Home', N'90Wh', 2.50, N'Xám Eclipse'),
    (7,'ROG-G614-I9-32G-BLK', 35000000, 42990000, 24, 4,3,3,4, N'16" QHD 240Hz',  N'Windows 11 Home', N'90Wh', 2.50, N'Đen');
    -- bien_the 23-25
GO

    -- ── Lenovo Legion 5 Pro 16 (sp=8) — 2 cấu hình × 2 màu ──────────────────────
    INSERT INTO bien_the_san_pham
        (san_pham_id, ma_sku, gia_nhap, gia_ban, bao_hanh_thang,
         cpu_id, ram_id, o_cung_id, gpu_id,
         kich_thuoc_man_hinh, he_dieu_hanh, pin, trong_luong_kg, mau_sac)
    VALUES
    (8,'LEGI-5P-R7-16G-GRY', 23000000, 28490000, 24, 6,2,2,3, N'16" WQXGA 165Hz', N'Windows 11 Home', N'80Wh', 2.49, N'Xám'),
    (8,'LEGI-5P-R7-16G-WHT', 23000000, 28490000, 24, 6,2,2,3, N'16" WQXGA 165Hz', N'Windows 11 Home', N'80Wh', 2.49, N'Trắng'),
    (8,'LEGI-5P-R7-32G-GRY', 30000000, 36990000, 24, 6,3,3,4, N'16" WQXGA 165Hz', N'Windows 11 Home', N'80Wh', 2.49, N'Xám'),
    (8,'LEGI-5P-R7-32G-BLK', 30000000, 36990000, 24, 6,3,3,4, N'16" WQXGA 165Hz', N'Windows 11 Home', N'80Wh', 2.49, N'Đen');
    -- bien_the 26-29
GO

    -- ── HP Pavilion 15 (sp=9) — i5 có 3 màu, i7 có 2 màu ────────────────────────
    INSERT INTO bien_the_san_pham
        (san_pham_id, ma_sku, gia_nhap, gia_ban, bao_hanh_thang,
         cpu_id, ram_id, o_cung_id, gpu_id,
         kich_thuoc_man_hinh, he_dieu_hanh, pin, trong_luong_kg, mau_sac)
    VALUES
    (9,'HP-PAV15-I5-8G-SLV', 10000000, 12490000, 12, 1,1,1,1, N'15.6" FHD 144Hz', N'Windows 11 Home', N'41Wh', 1.75, N'Bạc'),
    (9,'HP-PAV15-I5-8G-PNK', 10000000, 12490000, 12, 1,1,1,1, N'15.6" FHD 144Hz', N'Windows 11 Home', N'41Wh', 1.75, N'Hồng'),
    (9,'HP-PAV15-I5-8G-BLU', 10000000, 12490000, 12, 1,1,1,1, N'15.6" FHD 144Hz', N'Windows 11 Home', N'41Wh', 1.75, N'Xanh Dương'),
    (9,'HP-PAV15-I7-16G-SLV',13000000, 15990000, 12, 2,2,2,1, N'15.6" FHD 144Hz', N'Windows 11 Home', N'41Wh', 1.75, N'Bạc'),
    (9,'HP-PAV15-I7-16G-BLK',13000000, 15990000, 12, 2,2,2,1, N'15.6" FHD 144Hz', N'Windows 11 Home', N'41Wh', 1.75, N'Đen');
    -- bien_the 30-34
GO

    -- ── Dell XPS 15 9530 (sp=10) — i7 2 màu, i9 1 màu ───────────────────────────
    INSERT INTO bien_the_san_pham
        (san_pham_id, ma_sku, gia_nhap, gia_ban, bao_hanh_thang,
         cpu_id, ram_id, o_cung_id, gpu_id,
         kich_thuoc_man_hinh, he_dieu_hanh, pin, trong_luong_kg, mau_sac)
    VALUES
    (10,'DELL-XPS15-I7-16G-PLT',35000000, 42990000, 24, 7,5,2,3, N'15.6" OLED 3.5K 60Hz', N'Windows 11 Pro', N'86Wh', 1.86, N'Bạc Bạch Kim'),
    (10,'DELL-XPS15-I7-16G-BLK',35000000, 42990000, 24, 7,5,2,3, N'15.6" OLED 3.5K 60Hz', N'Windows 11 Pro', N'86Wh', 1.86, N'Đen Graphite'),
    (10,'DELL-XPS15-I9-32G-PLT',48000000, 57990000, 24, 4,3,3,4, N'15.6" OLED 3.5K 60Hz', N'Windows 11 Pro', N'86Wh', 1.86, N'Bạc Bạch Kim');
    -- bien_the 35-37
GO

    -- ── Acer Nitro V 15 (sp=11) — 2 cấu hình × 2 màu ────────────────────────────
    INSERT INTO bien_the_san_pham
        (san_pham_id, ma_sku, gia_nhap, gia_ban, bao_hanh_thang,
         cpu_id, ram_id, o_cung_id, gpu_id,
         kich_thuoc_man_hinh, he_dieu_hanh, pin, trong_luong_kg, mau_sac)
    VALUES
    (11,'ACER-NV15-I5-8G-BLK', 18000000, 22490000, 12, 3,1,2,2, N'15.6" FHD 144Hz', N'Windows 11 Home', N'57Wh', 2.10, N'Đen'),
    (11,'ACER-NV15-I5-8G-RED', 18000000, 22490000, 12, 3,1,2,2, N'15.6" FHD 144Hz', N'Windows 11 Home', N'57Wh', 2.10, N'Đỏ Đen'),
    (11,'ACER-NV15-I7-16G-BLK',24000000, 29990000, 12, 2,2,2,3, N'15.6" FHD 144Hz', N'Windows 11 Home', N'57Wh', 2.10, N'Đen'),
    (11,'ACER-NV15-I7-16G-WHT',24000000, 29990000, 12, 2,2,2,3, N'15.6" FHD 144Hz', N'Windows 11 Home', N'57Wh', 2.10, N'Trắng');
    -- bien_the 38-41
GO

    -- ── Tồn kho cho tất cả biến thể mới (bien_the 11-41) ─────────────────────────
    INSERT INTO ton_kho (bien_the_id, so_luong_ton_thuc_te, so_luong_giu, ton_kho_toi_thieu) VALUES
    -- Extra màu sp 1-5 (bien_the 11-18)
    (11,  8, 0, 3),  -- Dell i5 Đen
    (12,  6, 0, 3),  -- Asus i5 Đen
    (13,  4, 0, 2),  -- Asus i7 Đen
    (14,  5, 0, 3),  -- Lenovo R5 Xanh Dương
    (15,  3, 0, 2),  -- Lenovo R7 Xanh Dương
    (16,  4, 0, 2),  -- HP Envy i7 Vàng Citrus
    (17,  2, 0, 1),  -- HP Envy i9 Đen Midnight
    (18,  3, 0, 2),  -- MSI RTX4050 Bạc
    -- Acer Aspire 5 (bien_the 19-22)
    (19,  8, 0, 3),  -- i5 Bạc
    (20,  6, 0, 3),  -- i5 Xanh Dương
    (21,  5, 0, 2),  -- i7 Bạc
    (22,  4, 0, 2),  -- i7 Đen
    -- ROG Strix G16 (bien_the 23-25)
    (23,  5, 0, 2),  -- i7 RTX4060 Đen
    (24,  3, 0, 2),  -- i7 RTX4060 Xám Eclipse
    (25,  2, 0, 1),  -- i9 RTX4070 Đen
    -- Lenovo Legion 5 Pro (bien_the 26-29)
    (26,  6, 0, 3),  -- R7 RTX4060 Xám
    (27,  4, 0, 2),  -- R7 RTX4060 Trắng
    (28,  3, 0, 2),  -- R7 RTX4070 Xám
    (29,  2, 0, 1),  -- R7 RTX4070 Đen
    -- HP Pavilion 15 (bien_the 30-34)
    (30, 10, 0, 5),  -- i5 Bạc
    (31,  8, 0, 4),  -- i5 Hồng
    (32,  7, 0, 4),  -- i5 Xanh Dương
    (33,  6, 0, 3),  -- i7 Bạc
    (34,  5, 0, 3),  -- i7 Đen
    -- Dell XPS 15 9530 (bien_the 35-37)
    (35,  4, 0, 2),  -- i7 Bạc Bạch Kim
    (36,  3, 0, 2),  -- i7 Đen Graphite
    (37,  2, 0, 1),  -- i9 Bạc Bạch Kim
    -- Acer Nitro V 15 (bien_the 38-41)
    (38,  7, 0, 3),  -- i5 RTX4050 Đen
    (39,  5, 0, 3),  -- i5 RTX4050 Đỏ Đen
    (40,  4, 0, 2),  -- i7 RTX4060 Đen
    (41,  3, 0, 2);  -- i7 RTX4060 Trắng
GO
select*from bien_the_san_pham
    -- ── Phân loại cho sản phẩm mới (sp 6-11) ────────────────────────────────────
    -- phan_loai: van_phong=1,sinh_vien=2,gaming=3,do_hoa=4,ky_thuat=5,macbook=6,laptop_cu=7
    INSERT INTO san_pham_phan_loai (san_pham_id, phan_loai_id) VALUES
    -- Acer Aspire 5: văn phòng + sinh viên (phổ thông giá rẻ)
    (6, 1), (6, 2),
    -- ROG Strix G16: gaming + đồ họa (RTX 40, màn cao tần)
    (7, 3), (7, 4),
    -- Legion 5 Pro: gaming (AMD Ryzen + RTX, màn WQXGA)
    (8, 3),
    -- HP Pavilion 15: văn phòng + sinh viên (đa màu, giá phải chăng)
    (9, 1), (9, 2),
    -- Dell XPS 15: đồ họa + kỹ thuật (OLED 3.5K, thiết kế cao cấp)
    (10, 4), (10, 5),
    -- Acer Nitro V: gaming (RTX 40 tầm trung, tản nhiệt tốt)
    (11, 3);
GO

    -- Sync phan_loai_tags / phan_loai_ten cho sp mới (trigger đã xử lý khi INSERT,
    -- block này sync thủ công phòng trường hợp trigger chưa active)
    UPDATE bt
    SET
        bt.phan_loai_tags = (
            SELECT STRING_AGG(pl.ma_phan_loai, ',') WITHIN GROUP (ORDER BY pl.thu_tu)
            FROM san_pham_phan_loai sppl
            JOIN phan_loai pl ON sppl.phan_loai_id = pl.phan_loai_id
            WHERE sppl.san_pham_id = bt.san_pham_id AND pl.trang_thai = N'active'
        ),
        bt.phan_loai_ten = (
            SELECT STRING_AGG(pl.ten_phan_loai, ', ') WITHIN GROUP (ORDER BY pl.thu_tu)
            FROM san_pham_phan_loai sppl
            JOIN phan_loai pl ON sppl.phan_loai_id = pl.phan_loai_id
            WHERE sppl.san_pham_id = bt.san_pham_id AND pl.trang_thai = N'active'
        )
    FROM bien_the_san_pham bt
    WHERE bt.san_pham_id IN (6, 7, 8, 9, 10, 11);
GO

    -- ── Serial numbers cho tất cả biến thể (bt 3-41) ─────────────────────────────
    -- Format: N{năm}{brand}{bt_id:02d}{seq:04d}  (10 ký tự, giống serial laptop thật)
    -- Trước khi INSERT: reset ton_kho về 0 để trigger tính lại đúng
    UPDATE ton_kho SET so_luong_ton_thuc_te = 0, so_luong_giu = 0 WHERE bien_the_id BETWEEN 3 AND 41;
GO

    -- ── Asus Vivobook 15 X1504VA ──────────────────────────────────────────────────
    INSERT INTO chi_tiet_san_pham (bien_the_id, so_serial, trang_thai, ngay_nhap_kho) VALUES
    -- bt=3  ASUS-X1504-I5-8G Bạc: 11 trong_kho + 3 đã bán + 1 bảo hành
    (3,'N24A030001',N'trong_kho','2024-06-16'),(3,'N24A030002',N'trong_kho','2024-06-16'),
    (3,'N24A030003',N'trong_kho','2024-06-16'),(3,'N24A030004',N'trong_kho','2024-06-16'),
    (3,'N24A030005',N'trong_kho','2024-06-16'),(3,'N24A030006',N'trong_kho','2024-06-16'),
    (3,'N24A030007',N'trong_kho','2024-06-16'),(3,'N24A030008',N'trong_kho','2024-06-16'),
    (3,'N24A030009',N'trong_kho','2024-06-16'),(3,'N24A030010',N'trong_kho','2024-06-16'),
    (3,'N24A030011',N'trong_kho','2024-06-16'),
    (3,'N24A030012',N'da_ban',   '2024-06-16'),(3,'N24A030013',N'da_ban',   '2024-06-16'),
    (3,'N24A030014',N'da_ban',   '2024-06-16'),(3,'N24A030015',N'loi_bao_hanh', '2024-06-16'),
    -- bt=4  ASUS-X1504-I7-16G Bạc: 8 trong_kho + 2 đã bán + 1 bảo hành
    (4,'N24A040001',N'trong_kho','2024-06-16'),(4,'N24A040002',N'trong_kho','2024-06-16'),
    (4,'N24A040003',N'trong_kho','2024-06-16'),(4,'N24A040004',N'trong_kho','2024-06-16'),
    (4,'N24A040005',N'trong_kho','2024-06-16'),(4,'N24A040006',N'trong_kho','2024-06-16'),
    (4,'N24A040007',N'trong_kho','2024-06-16'),(4,'N24A040008',N'trong_kho','2024-06-16'),
    (4,'N24A040009',N'da_ban',   '2024-06-16'),(4,'N24A040010',N'da_ban',   '2024-06-16'),
    (4,'N24A040011',N'loi_bao_hanh', '2024-06-16');
GO

    INSERT INTO chi_tiet_san_pham (bien_the_id, so_serial, trang_thai, ngay_nhap_kho) VALUES
    -- bt=5  LENO-IP5P-R5-8G Xám Bão: 13 trong_kho + 4 đã bán + 1 bảo hành
    (5,'N24L050001',N'trong_kho','2024-06-16'),(5,'N24L050002',N'trong_kho','2024-06-16'),
    (5,'N24L050003',N'trong_kho','2024-06-16'),(5,'N24L050004',N'trong_kho','2024-06-16'),
    (5,'N24L050005',N'trong_kho','2024-06-16'),(5,'N24L050006',N'trong_kho','2024-06-16'),
    (5,'N24L050007',N'trong_kho','2024-06-16'),(5,'N24L050008',N'trong_kho','2024-06-16'),
    (5,'N24L050009',N'trong_kho','2024-06-16'),(5,'N24L050010',N'trong_kho','2024-06-16'),
    (5,'N24L050011',N'trong_kho','2024-06-16'),(5,'N24L050012',N'trong_kho','2024-06-16'),
    (5,'N24L050013',N'trong_kho','2024-06-16'),
    (5,'N24L050014',N'da_ban',   '2024-06-16'),(5,'N24L050015',N'da_ban',   '2024-06-16'),
    (5,'N24L050016',N'da_ban',   '2024-06-16'),(5,'N24L050017',N'da_ban',   '2024-06-16'),
    (5,'N24L050018',N'loi_bao_hanh', '2024-06-16'),
    -- bt=6  LENO-IP5P-R7-16G Xám Bão: 5 trong_kho + 2 đã bán + 1 bảo hành
    (6,'N24L060001',N'trong_kho','2024-06-16'),(6,'N24L060002',N'trong_kho','2024-06-16'),
    (6,'N24L060003',N'trong_kho','2024-06-16'),(6,'N24L060004',N'trong_kho','2024-06-16'),
    (6,'N24L060005',N'trong_kho','2024-06-16'),
    (6,'N24L060006',N'da_ban',   '2024-06-16'),(6,'N24L060007',N'da_ban',   '2024-06-16'),
    (6,'N24L060008',N'loi_bao_hanh', '2024-06-16');
GO

    INSERT INTO chi_tiet_san_pham (bien_the_id, so_serial, trang_thai, ngay_nhap_kho) VALUES
    -- bt=7  HP-ENVY-I7-16G Bạc Tự Nhiên: 9 trong_kho + 3 đã bán
    (7,'N24H070001',N'trong_kho','2024-06-16'),(7,'N24H070002',N'trong_kho','2024-06-16'),
    (7,'N24H070003',N'trong_kho','2024-06-16'),(7,'N24H070004',N'trong_kho','2024-06-16'),
    (7,'N24H070005',N'trong_kho','2024-06-16'),(7,'N24H070006',N'trong_kho','2024-06-16'),
    (7,'N24H070007',N'trong_kho','2024-06-16'),(7,'N24H070008',N'trong_kho','2024-06-16'),
    (7,'N24H070009',N'trong_kho','2024-06-16'),
    (7,'N24H070010',N'da_ban',   '2024-06-16'),(7,'N24H070011',N'da_ban',   '2024-06-16'),
    (7,'N24H070012',N'da_ban',   '2024-06-16'),
    -- bt=8  HP-ENVY-I9-32G Bạc Tự Nhiên: 3 trong_kho + 2 đã bán + 1 bảo hành
    (8,'N24H080001',N'trong_kho','2024-06-16'),(8,'N24H080002',N'trong_kho','2024-06-16'),
    (8,'N24H080003',N'trong_kho','2024-06-16'),
    (8,'N24H080004',N'da_ban',   '2024-06-16'),(8,'N24H080005',N'da_ban',   '2024-06-16'),
    (8,'N24H080006',N'loi_bao_hanh', '2024-06-16');
GO

    INSERT INTO chi_tiet_san_pham (bien_the_id, so_serial, trang_thai, ngay_nhap_kho) VALUES
    -- bt=9  MSI-STL15-RTX4050 Đen: 7 trong_kho + 3 đã bán + 1 bảo hành
    (9,'N24M090001',N'trong_kho','2024-06-16'),(9,'N24M090002',N'trong_kho','2024-06-16'),
    (9,'N24M090003',N'trong_kho','2024-06-16'),(9,'N24M090004',N'trong_kho','2024-06-16'),
    (9,'N24M090005',N'trong_kho','2024-06-16'),(9,'N24M090006',N'trong_kho','2024-06-16'),
    (9,'N24M090007',N'trong_kho','2024-06-16'),
    (9,'N24M090008',N'da_ban',   '2024-06-16'),(9,'N24M090009',N'da_ban',   '2024-06-16'),
    (9,'N24M090010',N'da_ban',   '2024-06-16'),(9,'N24M090011',N'loi_bao_hanh', '2024-06-16'),
    -- bt=10 MSI-STL15-RTX4070 Đen: 5 trong_kho + 2 đã bán
    (10,'N24M100001',N'trong_kho','2024-06-16'),(10,'N24M100002',N'trong_kho','2024-06-16'),
    (10,'N24M100003',N'trong_kho','2024-06-16'),(10,'N24M100004',N'trong_kho','2024-06-16'),
    (10,'N24M100005',N'trong_kho','2024-06-16'),
    (10,'N24M100006',N'da_ban',   '2024-06-16'),(10,'N24M100007',N'da_ban',   '2024-06-16');
GO

    -- ── Extra màu sp 1-5 (bien_the 11-18) ────────────────────────────────────────
    INSERT INTO chi_tiet_san_pham (bien_the_id, so_serial, trang_thai, ngay_nhap_kho) VALUES
    -- bt=11 DELL-3520-I5-8G-BLK Đen: 8 trong_kho + 3 đã bán
    (11,'N24D110001',N'trong_kho','2024-10-01'),(11,'N24D110002',N'trong_kho','2024-10-01'),
    (11,'N24D110003',N'trong_kho','2024-10-01'),(11,'N24D110004',N'trong_kho','2024-10-01'),
    (11,'N24D110005',N'trong_kho','2024-10-01'),(11,'N24D110006',N'trong_kho','2024-10-01'),
    (11,'N24D110007',N'trong_kho','2024-10-01'),(11,'N24D110008',N'trong_kho','2024-10-01'),
    (11,'N24D110009',N'da_ban',   '2024-10-01'),(11,'N24D110010',N'da_ban',   '2024-10-01'),
    (11,'N24D110011',N'da_ban',   '2024-10-01'),
    -- bt=12 ASUS-X1504-I5-8G-BLK Đen: 6 trong_kho + 2 đã bán
    (12,'N24A120001',N'trong_kho','2024-10-01'),(12,'N24A120002',N'trong_kho','2024-10-01'),
    (12,'N24A120003',N'trong_kho','2024-10-01'),(12,'N24A120004',N'trong_kho','2024-10-01'),
    (12,'N24A120005',N'trong_kho','2024-10-01'),(12,'N24A120006',N'trong_kho','2024-10-01'),
    (12,'N24A120007',N'da_ban',   '2024-10-01'),(12,'N24A120008',N'da_ban',   '2024-10-01'),
    -- bt=13 ASUS-X1504-I7-16G-BLK Đen: 4 trong_kho + 1 đã bán + 1 bảo hành
    (13,'N24A130001',N'trong_kho','2024-10-01'),(13,'N24A130002',N'trong_kho','2024-10-01'),
    (13,'N24A130003',N'trong_kho','2024-10-01'),(13,'N24A130004',N'trong_kho','2024-10-01'),
    (13,'N24A130005',N'da_ban',   '2024-10-01'),(13,'N24A130006',N'loi_bao_hanh', '2024-10-01');
GO

    INSERT INTO chi_tiet_san_pham (bien_the_id, so_serial, trang_thai, ngay_nhap_kho) VALUES
    -- bt=14 LENO-IP5P-R5-8G-BLU Xanh Dương: 5 trong_kho + 2 đã bán
    (14,'N24L140001',N'trong_kho','2024-10-01'),(14,'N24L140002',N'trong_kho','2024-10-01'),
    (14,'N24L140003',N'trong_kho','2024-10-01'),(14,'N24L140004',N'trong_kho','2024-10-01'),
    (14,'N24L140005',N'trong_kho','2024-10-01'),
    (14,'N24L140006',N'da_ban',   '2024-10-01'),(14,'N24L140007',N'da_ban',   '2024-10-01'),
    -- bt=15 LENO-IP5P-R7-16G-BLU Xanh Dương: 3 trong_kho + 1 đã bán
    (15,'N24L150001',N'trong_kho','2024-10-01'),(15,'N24L150002',N'trong_kho','2024-10-01'),
    (15,'N24L150003',N'trong_kho','2024-10-01'),(15,'N24L150004',N'da_ban',   '2024-10-01'),
    -- bt=16 HP-ENVY-I7-16G-GLD Vàng Citrus: 4 trong_kho + 1 đã bán
    (16,'N24H160001',N'trong_kho','2024-10-01'),(16,'N24H160002',N'trong_kho','2024-10-01'),
    (16,'N24H160003',N'trong_kho','2024-10-01'),(16,'N24H160004',N'trong_kho','2024-10-01'),
    (16,'N24H160005',N'da_ban',   '2024-10-01'),
    -- bt=17 HP-ENVY-I9-32G-BLK Đen Midnight: 2 trong_kho + 1 đã bán
    (17,'N24H170001',N'trong_kho','2024-10-01'),(17,'N24H170002',N'trong_kho','2024-10-01'),
    (17,'N24H170003',N'da_ban',   '2024-10-01'),
    -- bt=18 MSI-STL15-RTX4050-SLV Bạc: 3 trong_kho + 1 đã bán
    (18,'N24M180001',N'trong_kho','2024-10-01'),(18,'N24M180002',N'trong_kho','2024-10-01'),
    (18,'N24M180003',N'trong_kho','2024-10-01'),(18,'N24M180004',N'da_ban',   '2024-10-01');
GO

    -- ── Acer Aspire 5 A515-58 (bien_the 19-22) ───────────────────────────────────
    INSERT INTO chi_tiet_san_pham (bien_the_id, so_serial, trang_thai, ngay_nhap_kho) VALUES
    -- bt=19 ACER-A515-I5-8G-SLV Bạc: 8 trong_kho + 2 đã bán
    (19,'N24C190001',N'trong_kho','2024-11-01'),(19,'N24C190002',N'trong_kho','2024-11-01'),
    (19,'N24C190003',N'trong_kho','2024-11-01'),(19,'N24C190004',N'trong_kho','2024-11-01'),
    (19,'N24C190005',N'trong_kho','2024-11-01'),(19,'N24C190006',N'trong_kho','2024-11-01'),
    (19,'N24C190007',N'trong_kho','2024-11-01'),(19,'N24C190008',N'trong_kho','2024-11-01'),
    (19,'N24C190009',N'da_ban',   '2024-11-01'),(19,'N24C190010',N'da_ban',   '2024-11-01'),
    -- bt=20 ACER-A515-I5-8G-BLU Xanh Dương: 6 trong_kho + 2 đã bán
    (20,'N24C200001',N'trong_kho','2024-11-01'),(20,'N24C200002',N'trong_kho','2024-11-01'),
    (20,'N24C200003',N'trong_kho','2024-11-01'),(20,'N24C200004',N'trong_kho','2024-11-01'),
    (20,'N24C200005',N'trong_kho','2024-11-01'),(20,'N24C200006',N'trong_kho','2024-11-01'),
    (20,'N24C200007',N'da_ban',   '2024-11-01'),(20,'N24C200008',N'da_ban',   '2024-11-01'),
    -- bt=21 ACER-A515-I7-16G-SLV Bạc: 5 trong_kho + 2 đã bán + 1 bảo hành
    (21,'N24C210001',N'trong_kho','2024-11-01'),(21,'N24C210002',N'trong_kho','2024-11-01'),
    (21,'N24C210003',N'trong_kho','2024-11-01'),(21,'N24C210004',N'trong_kho','2024-11-01'),
    (21,'N24C210005',N'trong_kho','2024-11-01'),
    (21,'N24C210006',N'da_ban',   '2024-11-01'),(21,'N24C210007',N'da_ban',   '2024-11-01'),
    (21,'N24C210008',N'loi_bao_hanh', '2024-11-01'),
    -- bt=22 ACER-A515-I7-16G-BLK Đen: 4 trong_kho + 1 đã bán
    (22,'N24C220001',N'trong_kho','2024-11-01'),(22,'N24C220002',N'trong_kho','2024-11-01'),
    (22,'N24C220003',N'trong_kho','2024-11-01'),(22,'N24C220004',N'trong_kho','2024-11-01'),
    (22,'N24C220005',N'da_ban',   '2024-11-01');
GO

    -- ── Asus ROG Strix G16 G614 (bien_the 23-25) ─────────────────────────────────
    INSERT INTO chi_tiet_san_pham (bien_the_id, so_serial, trang_thai, ngay_nhap_kho) VALUES
    -- bt=23 ROG-G614-I7-16G-BLK Đen: 5 trong_kho + 2 đã bán
    (23,'N24R230001',N'trong_kho','2024-11-15'),(23,'N24R230002',N'trong_kho','2024-11-15'),
    (23,'N24R230003',N'trong_kho','2024-11-15'),(23,'N24R230004',N'trong_kho','2024-11-15'),
    (23,'N24R230005',N'trong_kho','2024-11-15'),
    (23,'N24R230006',N'da_ban',   '2024-11-15'),(23,'N24R230007',N'da_ban',   '2024-11-15'),
    -- bt=24 ROG-G614-I7-16G-GRY Xám Eclipse: 3 trong_kho + 1 đã bán + 1 bảo hành
    (24,'N24R240001',N'trong_kho','2024-11-15'),(24,'N24R240002',N'trong_kho','2024-11-15'),
    (24,'N24R240003',N'trong_kho','2024-11-15'),
    (24,'N24R240004',N'da_ban',   '2024-11-15'),(24,'N24R240005',N'loi_bao_hanh', '2024-11-15'),
    -- bt=25 ROG-G614-I9-32G-BLK Đen: 2 trong_kho + 1 đã bán
    (25,'N24R250001',N'trong_kho','2024-11-15'),(25,'N24R250002',N'trong_kho','2024-11-15'),
    (25,'N24R250003',N'da_ban',   '2024-11-15');
GO

    -- ── Lenovo Legion 5 Pro 16 (bien_the 26-29) ──────────────────────────────────
    INSERT INTO chi_tiet_san_pham (bien_the_id, so_serial, trang_thai, ngay_nhap_kho) VALUES
    -- bt=26 LEGI-5P-R7-16G-GRY Xám: 6 trong_kho + 2 đã bán
    (26,'N24G260001',N'trong_kho','2024-11-15'),(26,'N24G260002',N'trong_kho','2024-11-15'),
    (26,'N24G260003',N'trong_kho','2024-11-15'),(26,'N24G260004',N'trong_kho','2024-11-15'),
    (26,'N24G260005',N'trong_kho','2024-11-15'),(26,'N24G260006',N'trong_kho','2024-11-15'),
    (26,'N24G260007',N'da_ban',   '2024-11-15'),(26,'N24G260008',N'da_ban',   '2024-11-15'),
    -- bt=27 LEGI-5P-R7-16G-WHT Trắng: 4 trong_kho + 2 đã bán + 1 bảo hành
    (27,'N24G270001',N'trong_kho','2024-11-15'),(27,'N24G270002',N'trong_kho','2024-11-15'),
    (27,'N24G270003',N'trong_kho','2024-11-15'),(27,'N24G270004',N'trong_kho','2024-11-15'),
    (27,'N24G270005',N'da_ban',   '2024-11-15'),(27,'N24G270006',N'da_ban',   '2024-11-15'),
    (27,'N24G270007',N'loi_bao_hanh', '2024-11-15'),
    -- bt=28 LEGI-5P-R7-32G-GRY Xám: 3 trong_kho + 1 đã bán
    (28,'N24G280001',N'trong_kho','2024-11-15'),(28,'N24G280002',N'trong_kho','2024-11-15'),
    (28,'N24G280003',N'trong_kho','2024-11-15'),(28,'N24G280004',N'da_ban',   '2024-11-15'),
    -- bt=29 LEGI-5P-R7-32G-BLK Đen: 2 trong_kho + 1 đã bán
    (29,'N24G290001',N'trong_kho','2024-11-15'),(29,'N24G290002',N'trong_kho','2024-11-15'),
    (29,'N24G290003',N'da_ban',   '2024-11-15');
GO

    -- ── HP Pavilion 15 (bien_the 30-34) ──────────────────────────────────────────
    INSERT INTO chi_tiet_san_pham (bien_the_id, so_serial, trang_thai, ngay_nhap_kho) VALUES
    -- bt=30 HP-PAV15-I5-8G-SLV Bạc: 10 trong_kho + 3 đã bán + 1 bảo hành
    (30,'N24H300001',N'trong_kho','2024-12-01'),(30,'N24H300002',N'trong_kho','2024-12-01'),
    (30,'N24H300003',N'trong_kho','2024-12-01'),(30,'N24H300004',N'trong_kho','2024-12-01'),
    (30,'N24H300005',N'trong_kho','2024-12-01'),(30,'N24H300006',N'trong_kho','2024-12-01'),
    (30,'N24H300007',N'trong_kho','2024-12-01'),(30,'N24H300008',N'trong_kho','2024-12-01'),
    (30,'N24H300009',N'trong_kho','2024-12-01'),(30,'N24H300010',N'trong_kho','2024-12-01'),
    (30,'N24H300011',N'da_ban',   '2024-12-01'),(30,'N24H300012',N'da_ban',   '2024-12-01'),
    (30,'N24H300013',N'da_ban',   '2024-12-01'),(30,'N24H300014',N'loi_bao_hanh', '2024-12-01'),
    -- bt=31 HP-PAV15-I5-8G-PNK Hồng: 8 trong_kho + 3 đã bán
    (31,'N24H310001',N'trong_kho','2024-12-01'),(31,'N24H310002',N'trong_kho','2024-12-01'),
    (31,'N24H310003',N'trong_kho','2024-12-01'),(31,'N24H310004',N'trong_kho','2024-12-01'),
    (31,'N24H310005',N'trong_kho','2024-12-01'),(31,'N24H310006',N'trong_kho','2024-12-01'),
    (31,'N24H310007',N'trong_kho','2024-12-01'),(31,'N24H310008',N'trong_kho','2024-12-01'),
    (31,'N24H310009',N'da_ban',   '2024-12-01'),(31,'N24H310010',N'da_ban',   '2024-12-01'),
    (31,'N24H310011',N'da_ban',   '2024-12-01');
GO

    INSERT INTO chi_tiet_san_pham (bien_the_id, so_serial, trang_thai, ngay_nhap_kho) VALUES
    -- bt=32 HP-PAV15-I5-8G-BLU Xanh Dương: 7 trong_kho + 2 đã bán
    (32,'N24H320001',N'trong_kho','2024-12-01'),(32,'N24H320002',N'trong_kho','2024-12-01'),
    (32,'N24H320003',N'trong_kho','2024-12-01'),(32,'N24H320004',N'trong_kho','2024-12-01'),
    (32,'N24H320005',N'trong_kho','2024-12-01'),(32,'N24H320006',N'trong_kho','2024-12-01'),
    (32,'N24H320007',N'trong_kho','2024-12-01'),
    (32,'N24H320008',N'da_ban',   '2024-12-01'),(32,'N24H320009',N'da_ban',   '2024-12-01'),
    -- bt=33 HP-PAV15-I7-16G-SLV Bạc: 6 trong_kho + 2 đã bán + 1 bảo hành
    (33,'N24H330001',N'trong_kho','2024-12-01'),(33,'N24H330002',N'trong_kho','2024-12-01'),
    (33,'N24H330003',N'trong_kho','2024-12-01'),(33,'N24H330004',N'trong_kho','2024-12-01'),
    (33,'N24H330005',N'trong_kho','2024-12-01'),(33,'N24H330006',N'trong_kho','2024-12-01'),
    (33,'N24H330007',N'da_ban',   '2024-12-01'),(33,'N24H330008',N'da_ban',   '2024-12-01'),
    (33,'N24H330009',N'loi_bao_hanh', '2024-12-01'),
    -- bt=34 HP-PAV15-I7-16G-BLK Đen: 5 trong_kho + 2 đã bán
    (34,'N24H340001',N'trong_kho','2024-12-01'),(34,'N24H340002',N'trong_kho','2024-12-01'),
    (34,'N24H340003',N'trong_kho','2024-12-01'),(34,'N24H340004',N'trong_kho','2024-12-01'),
    (34,'N24H340005',N'trong_kho','2024-12-01'),
    (34,'N24H340006',N'da_ban',   '2024-12-01'),(34,'N24H340007',N'da_ban',   '2024-12-01');
GO

    -- ── Dell XPS 15 9530 (bien_the 35-37) ────────────────────────────────────────
    INSERT INTO chi_tiet_san_pham (bien_the_id, so_serial, trang_thai, ngay_nhap_kho) VALUES
    -- bt=35 XPS-9530-I7-16G Bạc Bạch Kim: 4 trong_kho + 2 đã bán
    (35,'N24D350001',N'trong_kho','2024-12-15'),(35,'N24D350002',N'trong_kho','2024-12-15'),
    (35,'N24D350003',N'trong_kho','2024-12-15'),(35,'N24D350004',N'trong_kho','2024-12-15'),
    (35,'N24D350005',N'da_ban',   '2024-12-15'),(35,'N24D350006',N'da_ban',   '2024-12-15'),
    -- bt=36 XPS-9530-I7-16G Đen Graphite: 3 trong_kho + 1 đã bán + 1 bảo hành
    (36,'N24D360001',N'trong_kho','2024-12-15'),(36,'N24D360002',N'trong_kho','2024-12-15'),
    (36,'N24D360003',N'trong_kho','2024-12-15'),
    (36,'N24D360004',N'da_ban',   '2024-12-15'),(36,'N24D360005',N'loi_bao_hanh', '2024-12-15'),
    -- bt=37 XPS-9530-I9-32G Bạc Bạch Kim: 2 trong_kho + 1 đã bán
    (37,'N24D370001',N'trong_kho','2024-12-15'),(37,'N24D370002',N'trong_kho','2024-12-15'),
    (37,'N24D370003',N'da_ban',   '2024-12-15');
GO

    -- ── Acer Nitro V 15 (bien_the 38-41) ─────────────────────────────────────────
    INSERT INTO chi_tiet_san_pham (bien_the_id, so_serial, trang_thai, ngay_nhap_kho) VALUES
    -- bt=38 NTV15-I5-RTX4050 Đen: 7 trong_kho + 3 đã bán + 1 bảo hành
    (38,'N25C380001',N'trong_kho','2025-01-10'),(38,'N25C380002',N'trong_kho','2025-01-10'),
    (38,'N25C380003',N'trong_kho','2025-01-10'),(38,'N25C380004',N'trong_kho','2025-01-10'),
    (38,'N25C380005',N'trong_kho','2025-01-10'),(38,'N25C380006',N'trong_kho','2025-01-10'),
    (38,'N25C380007',N'trong_kho','2025-01-10'),
    (38,'N25C380008',N'da_ban',   '2025-01-10'),(38,'N25C380009',N'da_ban',   '2025-01-10'),
    (38,'N25C380010',N'da_ban',   '2025-01-10'),(38,'N25C380011',N'loi_bao_hanh', '2025-01-10'),
    -- bt=39 NTV15-I5-RTX4050 Đỏ Đen: 5 trong_kho + 2 đã bán
    (39,'N25C390001',N'trong_kho','2025-01-10'),(39,'N25C390002',N'trong_kho','2025-01-10'),
    (39,'N25C390003',N'trong_kho','2025-01-10'),(39,'N25C390004',N'trong_kho','2025-01-10'),
    (39,'N25C390005',N'trong_kho','2025-01-10'),
    (39,'N25C390006',N'da_ban',   '2025-01-10'),(39,'N25C390007',N'da_ban',   '2025-01-10'),
    -- bt=40 NTV15-I7-RTX4060 Đen: 4 trong_kho + 2 đã bán + 1 bảo hành
    (40,'N25C400001',N'trong_kho','2025-01-10'),(40,'N25C400002',N'trong_kho','2025-01-10'),
    (40,'N25C400003',N'trong_kho','2025-01-10'),(40,'N25C400004',N'trong_kho','2025-01-10'),
    (40,'N25C400005',N'da_ban',   '2025-01-10'),(40,'N25C400006',N'da_ban',   '2025-01-10'),
    (40,'N25C400007',N'loi_bao_hanh', '2025-01-10'),
    -- bt=41 NTV15-I7-RTX4060 Trắng: 3 trong_kho + 1 đã bán
    (41,'N25C410001',N'trong_kho','2025-01-10'),(41,'N25C410002',N'trong_kho','2025-01-10'),
    (41,'N25C410003',N'trong_kho','2025-01-10'),(41,'N25C410004',N'da_ban',   '2025-01-10');
GO
-- ============================================================
--  13.C. DỮ LIỆU ĐƠN HÀNG MỞ RỘNG — 20-30 đơn/ngày, đa số đã giao + đã thanh toán
-- Từ 01/01/2026 đến 03/07/2026 (khớp khoảng ngày gốc file này từng dùng).
-- Set-based (Tally + random rn rồi JOIN), không liệt kê tay từng dòng như trước.
-- Không dùng UPDATE để vá lại tong_tien sau khi insert — các bước làm ĐÚNG THỨ TỰ:
-- chọn sản phẩm & tính tổng tiền trước (bảng tạm #Staging) → insert don_hang với
-- tong_tien đã đúng ngay từ đầu → MERGE...OUTPUT lấy don_hang_id IDENTITY mới sinh,
-- khớp lại đúng dòng nguồn (INSERT...OUTPUT thường không cho output cột nguồn ngoài
-- inserted.*, MERGE thì được) → insert chi_tiet_don_hang/thanh_toan dựa trên mapping đó.
-- ============================================================
    IF OBJECT_ID('tempdb..#Days') IS NOT NULL DROP TABLE #Days;
    IF OBJECT_ID('tempdb..#Slots') IS NOT NULL DROP TABLE #Slots;
    IF OBJECT_ID('tempdb..#KhachSo') IS NOT NULL DROP TABLE #KhachSo;
    IF OBJECT_ID('tempdb..#BienTheSo') IS NOT NULL DROP TABLE #BienTheSo;
    IF OBJECT_ID('tempdb..#Staging') IS NOT NULL DROP TABLE #Staging;
    IF OBJECT_ID('tempdb..#Mapping') IS NOT NULL DROP TABLE #Mapping;

    CREATE TABLE #Staging (
        staging_key          INT IDENTITY(1,1) PRIMARY KEY,
        khach_hang_id        INT, ho_ten NVARCHAR(150), so_dien_thoai VARCHAR(20),
        ngay_dat              DATETIME,
        trang_thai_don_hang   NVARCHAR(30), trang_thai_thanh_toan NVARCHAR(30),
        phi_van_chuyen        DECIMAL(18,0),
        item1_bien_the_id     INT, item1_gia DECIMAL(18,0), item1_soluong INT,
        item2_bien_the_id     INT NULL, item2_gia DECIMAL(18,0) NULL, item2_soluong INT NULL,
        tong_tien             DECIMAL(18,0)
    );
    CREATE TABLE #Mapping (staging_key INT PRIMARY KEY, don_hang_id INT);

    DECLARE @TuNgay DATE = '2026-01-01';
    DECLARE @DenNgay DATE = '2026-07-03';
    DECLARE @SoKhach INT = (SELECT COUNT(*) FROM khach_hang);
    DECLARE @SoBienThe INT = (SELECT COUNT(*) FROM bien_the_san_pham WHERE trang_thai = N'active');

    -- Bước 1: chọn khách hàng + sản phẩm + tính tổng tiền cho từng đơn, TRƯỚC khi insert
    --
    -- Giải thích các "hàm lạ" dùng bên dưới, để lần sau đọc lại không phải tra cứu:
    --
    -- 1) Tally (E1→E2→E4): T-SQL không có hàm "sinh dãy số 1..N" dựng sẵn (khác
    --    GENERATE_SERIES của Postgres). Đây là idiom kinh điển để tự tạo: E1 có 10 dòng
    --    (từ VALUES), CROSS JOIN E1 với chính nó ra E2 = 10×10 = 100 dòng, CROSS JOIN E2
    --    với chính nó ra E4 = 100×100 = 10.000 dòng — nhân đôi số mũ mỗi bước nên rất ít
    --    dòng CTE mà ra được tập lớn. ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) chỉ để
    --    đánh số thứ tự 1..10000 cho các dòng đó — ORDER BY (SELECT NULL) nghĩa là "thứ
    --    tự nào cũng được, tôi chỉ cần con số tăng dần", không có cột nào thật để sort.
    --
    -- 2) ABS(CAST(CHECKSUM(NEWID()) AS BIGINT)) % N: cách chuẩn để sinh số nguyên ngẫu
    --    nhiên 0..N-1 cho MỖI DÒNG trong T-SQL. Không dùng RAND() vì RAND() chỉ tính 1
    --    lần cho cả câu lệnh (mọi dòng ra cùng 1 số) — NEWID() thì luôn duy nhất mỗi
    --    dòng. CAST sang BIGINT trước ABS() để tránh tràn số (CHECKSUM trả về INT, có 1
    --    giá trị INT âm nhỏ nhất mà ABS() không biểu diễn nổi dưới dạng INT dương).
    --
    -- 3) VẬT CHẤT HOÁ TỪNG GIAI ĐOẠN VÀO BẢNG TẠM (#Days, #Slots...) thay vì lồng CTE
    --    nhiều tầng dùng chung: từng thử dùng toàn CTE lồng nhau, kết quả là SQL Server
    --    có thể tính lại cả nhánh chứa NEWID() nhiều lần trong 1 câu lệnh — vừa chạy rất
    --    chậm (đã gặp: hơn 1 phút cho ~4600 đơn, đáng lẽ chỉ vài giây), vừa tràn số nguyên
    --    (Msg 8115) do CHECKSUM(NEWID()) bị gọi nhiều hơn hẳn dự tính. Bảng tạm ép SQL
    --    Server tính xong 1 giai đoạn, lưu lại, rồi mới sang giai đoạn tiếp theo — không
    --    còn mập mờ "tính bao nhiêu lần" nữa, tốc độ ổn định và dự đoán được.
    ;WITH E1(n) AS (SELECT n FROM (VALUES(1),(1),(1),(1),(1),(1),(1),(1),(1),(1)) v(n)),
    E2(n) AS (SELECT 1 FROM E1 a CROSS JOIN E1 b),
    E4(n) AS (SELECT 1 FROM E2 a CROSS JOIN E2 b),
    Tally AS (SELECT ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS n FROM E4)
    SELECT DATEADD(DAY, n - 1, @TuNgay) AS ngay,
           20 + ABS(CAST(CHECKSUM(NEWID()) AS BIGINT)) % 11 AS so_don   -- 20..30 đơn/ngày
    INTO #Days
    FROM Tally
    WHERE n <= DATEDIFF(DAY, @TuNgay, @DenNgay) + 1;

    -- LƯU Ý: sinh số ngẫu nhiên (kh_rn/bt1_rn/bt2_rn...) NGAY trong SELECT trên #Days×Tally
    -- (bảng tạm × tally, nhiều dòng thật) rồi mới JOIN sang #KhachSo/#BienTheSo theo rn ở
    -- bước sau — KHÔNG dùng CROSS APPLY (SELECT TOP 1 ... ORDER BY NEWID()) để chọn ngẫu
    -- nhiên, vì APPLY không tương quan (không tham chiếu cột nào của bảng ngoài) có thể bị
    -- SQL Server tối ưu gộp thành CROSS JOIN và chỉ tính NEWID() MỘT LẦN cho cả tập kết
    -- quả — đúng lỗi thực tế gặp phải khi test: mọi đơn trong ngày ra cùng 1 khách/1 sp.
    ;WITH E1(n) AS (SELECT n FROM (VALUES(1),(1),(1),(1),(1),(1),(1),(1),(1),(1)) v(n)),
    E2(n) AS (SELECT 1 FROM E1 a CROSS JOIN E1 b),
    Tally30 AS (SELECT TOP (30) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS n FROM E2)
    SELECT d.ngay,
           1 + ABS(CAST(CHECKSUM(NEWID()) AS BIGINT)) % @SoKhach AS kh_rn,
           1 + ABS(CAST(CHECKSUM(NEWID()) AS BIGINT)) % @SoBienThe AS bt1_rn,
           1 + ABS(CAST(CHECKSUM(NEWID()) AS BIGINT)) % @SoBienThe AS bt2_rn,
           ABS(CAST(CHECKSUM(NEWID()) AS BIGINT)) % 100 AS roll,
           DATEADD(MINUTE, ABS(CAST(CHECKSUM(NEWID()) AS BIGINT)) % 780, DATEADD(HOUR, 8, CAST(d.ngay AS DATETIME))) AS ngay_dat,
           1 + ABS(CAST(CHECKSUM(NEWID()) AS BIGINT)) % 2 AS item1_soluong,
           CASE WHEN ABS(CAST(CHECKSUM(NEWID()) AS BIGINT)) % 100 < 35 THEN 1 ELSE 0 END AS co_item2
    INTO #Slots
    FROM #Days d
    CROSS JOIN Tally30 s
    WHERE s.n <= d.so_don;

    -- Đánh số khách hàng/biến thể để JOIN theo rn (thay vì chọn ngẫu nhiên qua APPLY)
    SELECT khach_hang_id, ho_ten, so_dien_thoai,
           ROW_NUMBER() OVER (ORDER BY khach_hang_id) AS rn
    INTO #KhachSo
    FROM khach_hang;

    SELECT bien_the_id, gia_ban,
           ROW_NUMBER() OVER (ORDER BY bien_the_id) AS rn
    INTO #BienTheSo
    FROM bien_the_san_pham WHERE trang_thai = N'active';

    INSERT INTO #Staging (khach_hang_id, ho_ten, so_dien_thoai, ngay_dat, trang_thai_don_hang, trang_thai_thanh_toan,
                          phi_van_chuyen, item1_bien_the_id, item1_gia, item1_soluong,
                          item2_bien_the_id, item2_gia, item2_soluong, tong_tien)
    SELECT
        kh.khach_hang_id, kh.ho_ten, kh.so_dien_thoai, sl.ngay_dat,
        -- đa số delivered/paid, phần nhỏ còn lại rải các trạng thái khác như dữ liệu gốc
        CASE
            WHEN sl.roll < 70 THEN N'delivered'
            WHEN sl.roll < 80 THEN N'shipping'
            WHEN sl.roll < 88 THEN N'processing'
            WHEN sl.roll < 94 THEN N'confirmed'
            WHEN sl.roll < 97 THEN N'pending'
            WHEN sl.roll < 99 THEN N'cancelled'
            ELSE N'returned'
        END,
        CASE
            WHEN sl.roll < 80 THEN N'paid'
            WHEN sl.roll < 88 THEN N'partial'
            WHEN sl.roll < 99 THEN N'unpaid'
            ELSE N'paid'
        END,
        CASE WHEN sl.roll % 5 = 0 THEN 30000 ELSE 0 END,
        bt1.bien_the_id, bt1.gia_ban, sl.item1_soluong,
        CASE WHEN sl.co_item2 = 1 THEN bt2.bien_the_id ELSE NULL END,
        CASE WHEN sl.co_item2 = 1 THEN bt2.gia_ban ELSE NULL END,
        CASE WHEN sl.co_item2 = 1 THEN 1 ELSE NULL END,
        bt1.gia_ban * sl.item1_soluong + CASE WHEN sl.co_item2 = 1 THEN bt2.gia_ban ELSE 0 END
    FROM #Slots sl
    JOIN #KhachSo kh ON kh.rn = sl.kh_rn
    JOIN #BienTheSo bt1 ON bt1.rn = sl.bt1_rn
    JOIN #BienTheSo bt2 ON bt2.rn = sl.bt2_rn;

    -- Bước 2: insert don_hang với tong_tien ĐÃ ĐÚNG ngay từ đầu — dùng MERGE thay vì
    -- INSERT thường vì lý do sau: sau khi insert cần biết "dòng #Staging nào ứng với
    -- don_hang_id (IDENTITY) nào vừa sinh ra" để bước 3/4 insert đúng chi_tiet_don_hang.
    -- INSERT ... OUTPUT chỉ cho lấy cột của bảng đích (inserted.*), KHÔNG cho lấy cột từ
    -- bảng nguồn (#Staging) — nên không thể output staging_key kèm theo. MERGE thì OUTPUT
    -- lấy được cả 2 phía. "ON 1 = 0" là điều kiện luôn sai, có nghĩa ép mọi dòng #Staging
    -- rơi vào nhánh WHEN NOT MATCHED (không tìm được khớp), tức là always insert — MERGE
    -- ở đây chỉ dùng như một cách "INSERT có OUTPUT nguồn", không thật sự merge/update gì.
    MERGE don_hang AS tgt
    USING #Staging AS src
    ON 1 = 0
    WHEN NOT MATCHED THEN
        INSERT (khach_hang_id, nguoi_nhan, sdt_nguoi_nhan, tong_tien, giam_gia, phi_van_chuyen,
                ngay_dat, trang_thai_don_hang, trang_thai_thanh_toan, kenh_ban)
        VALUES (src.khach_hang_id, src.ho_ten, src.so_dien_thoai, src.tong_tien, 0, src.phi_van_chuyen,
                src.ngay_dat, src.trang_thai_don_hang, src.trang_thai_thanh_toan, N'online')
    OUTPUT src.staging_key, inserted.don_hang_id INTO #Mapping(staging_key, don_hang_id);

    -- Bước 3: sinh chi_tiet_don_hang từ đúng sản phẩm đã chọn ở bước 1
    INSERT INTO chi_tiet_don_hang (don_hang_id, bien_the_id, so_luong, don_gia)
    SELECT m.don_hang_id, s.item1_bien_the_id, s.item1_soluong, s.item1_gia
    FROM #Mapping m JOIN #Staging s ON m.staging_key = s.staging_key;

    INSERT INTO chi_tiet_don_hang (don_hang_id, bien_the_id, so_luong, don_gia)
    SELECT m.don_hang_id, s.item2_bien_the_id, s.item2_soluong, s.item2_gia
    FROM #Mapping m JOIN #Staging s ON m.staging_key = s.staging_key
    WHERE s.item2_bien_the_id IS NOT NULL;

    -- Bước 4: sinh thanh_toan cho các đơn đã 'paid' — so_tien tính thẳng từ staging
    -- (tong_tien - giam_gia(=0) + phi_van_chuyen), không cần đọc lại don_hang.
    INSERT INTO thanh_toan (don_hang_id, ngay_thanh_toan, phuong_thuc_thanh_toan, so_tien, trang_thai)
    SELECT m.don_hang_id, s.ngay_dat,
           CASE ABS(CAST(CHECKSUM(NEWID()) AS BIGINT)) % 5
                WHEN 0 THEN N'tien_mat' WHEN 1 THEN N'chuyen_khoan' WHEN 2 THEN N'the_tin_dung'
                WHEN 3 THEN N'momo' ELSE N'vnpay' END,
           s.tong_tien + s.phi_van_chuyen,
           N'success'
    FROM #Mapping m JOIN #Staging s ON m.staging_key = s.staging_key
    WHERE s.trang_thai_thanh_toan = N'paid';

    DROP TABLE #Days;
    DROP TABLE #Slots;
    DROP TABLE #KhachSo;
    DROP TABLE #BienTheSo;
    DROP TABLE #Staging;
    DROP TABLE #Mapping;
GO

-- ============================================================
--  13b. PHIẾU TRẢ HÀNG + VÍ KHÁCH HÀNG DEMO
-- ============================================================
-- Random ~5% đơn "delivered" (vừa sinh ở mục 13) thành có phiếu trả hàng — đa dạng
-- trạng thái (da_xu_ly/cho_xu_ly/tu_choi) và hình thức hoàn (vi/tien_mat) để có sẵn dữ
-- liệu demo cho tính năng Trả hàng + Ví khách hàng mỗi lần chạy lại file. Sản phẩm trả =
-- dòng chi_tiet_don_hang đầu tiên (id nhỏ nhất) của đơn đó — không gán chi_tiet_id
-- (serial cụ thể) vì đơn demo không theo dõi serial theo từng đơn.
--
-- Random dùng NEWID() trực tiếp trong SELECT list, VẬT CHẤT HOÁ ngay vào bảng tạm thật
-- (#DonDaGiao) trước khi JOIN/lọc tiếp — đúng bài học đã rút ra ở mục 13: nếu chỉ dùng
-- CTE (không vật chất hoá) rồi JOIN/CROSS APPLY/WHERE lên các cột NEWID() của nó, SQL
-- Server có thể tính lại các cột NEWID() nhiều lần cho cùng 1 dòng logic (từng thực tế
-- gặp lỗi trùng khoá ở #MapTraHang khi thử theo cách CTE thuần). CROSS APPLY
-- chi_tiet_don_hang bên dưới có tương quan (WHERE don_hang_id = dg.don_hang_id) nên an
-- toàn dù không vật chất hoá riêng.
IF OBJECT_ID('tempdb..#NhanVienSo') IS NOT NULL DROP TABLE #NhanVienSo;
IF OBJECT_ID('tempdb..#DonDaGiao') IS NOT NULL DROP TABLE #DonDaGiao;
IF OBJECT_ID('tempdb..#DonTra') IS NOT NULL DROP TABLE #DonTra;
IF OBJECT_ID('tempdb..#MapTraHang') IS NOT NULL DROP TABLE #MapTraHang;

DECLARE @SoNhanVien2 INT = (SELECT COUNT(*) FROM nhan_vien);

SELECT nhan_vien_id, ROW_NUMBER() OVER (ORDER BY nhan_vien_id) AS rn
INTO #NhanVienSo
FROM nhan_vien;

SELECT don_hang_id,
       ABS(CAST(CHECKSUM(NEWID()) AS BIGINT)) % 100 AS roll_chon,
       1 + ABS(CAST(CHECKSUM(NEWID()) AS BIGINT)) % @SoNhanVien2 AS nhan_vien_rn,
       ABS(CAST(CHECKSUM(NEWID()) AS BIGINT)) % 5 AS roll_lydo,
       ABS(CAST(CHECKSUM(NEWID()) AS BIGINT)) % 100 AS roll_trangthai,
       ABS(CAST(CHECKSUM(NEWID()) AS BIGINT)) % 100 AS roll_hinhthuc,
       ABS(CAST(CHECKSUM(NEWID()) AS BIGINT)) % 14 AS roll_ngay
INTO #DonDaGiao
FROM don_hang
WHERE trang_thai_don_hang = N'delivered';

SELECT
    dg.don_hang_id, nv.nhan_vien_id,
    ct.bien_the_id, ct.don_gia, ct.so_luong,
    CASE dg.roll_lydo
         WHEN 0 THEN N'Sản phẩm không đúng mô tả đặt hàng'
         WHEN 1 THEN N'Máy bị lỗi, không lên nguồn'
         WHEN 2 THEN N'Khách đổi ý, không có lỗi sản phẩm'
         WHEN 3 THEN N'Màn hình có điểm chết'
         ELSE N'Giao nhầm phiên bản/màu sắc'
    END AS ly_do,
    CASE
        WHEN dg.roll_trangthai < 55 THEN N'da_xu_ly'
        WHEN dg.roll_trangthai < 80 THEN N'cho_xu_ly'
        ELSE N'tu_choi'
    END AS trang_thai,
    CASE WHEN dg.roll_hinhthuc < 60 THEN N'vi' ELSE N'tien_mat' END AS hinh_thuc_hoan,
    DATEADD(DAY, -dg.roll_ngay, CAST(GETDATE() AS DATETIME)) AS ngay_tra
INTO #DonTra
FROM #DonDaGiao dg
JOIN #NhanVienSo nv ON nv.rn = dg.nhan_vien_rn
CROSS APPLY (
    SELECT TOP 1 bien_the_id, don_gia, so_luong
    FROM chi_tiet_don_hang
    WHERE don_hang_id = dg.don_hang_id
    ORDER BY chi_tiet_don_hang_id
) ct
WHERE dg.roll_chon < 5;

CREATE TABLE #MapTraHang (don_hang_id INT PRIMARY KEY, phieu_tra_id INT);

MERGE phieu_tra_hang AS tgt
USING #DonTra AS src
ON 1 = 0
WHEN NOT MATCHED THEN
    INSERT (don_hang_id, nhan_vien_id, ly_do, ngay_tra, trang_thai, so_tien_hoan, hinh_thuc_hoan, ghi_chu)
    VALUES (src.don_hang_id, src.nhan_vien_id, src.ly_do, src.ngay_tra, src.trang_thai,
            CASE WHEN src.trang_thai = N'tu_choi' THEN 0 ELSE src.don_gia * src.so_luong END,
            src.hinh_thuc_hoan, N'Dữ liệu demo')
OUTPUT src.don_hang_id, inserted.phieu_tra_id INTO #MapTraHang(don_hang_id, phieu_tra_id);

INSERT INTO chi_tiet_tra_hang (phieu_tra_id, bien_the_id, so_luong, don_gia_hoan, tinh_trang)
SELECT m.phieu_tra_id, s.bien_the_id, s.so_luong, s.don_gia,
       CASE WHEN ABS(CAST(CHECKSUM(NEWID()) AS BIGINT)) % 100 < 40 THEN N'loi' ELSE N'tot' END
FROM #MapTraHang m
JOIN #DonTra s ON s.don_hang_id = m.don_hang_id;

-- Đồng bộ ví: logic cộng ví (PhieuTraHangService.congViNeuVuaHoanTat) chỉ chạy khi đi
-- qua tầng ứng dụng Java lúc tạo/sửa phiếu qua API — INSERT thẳng bằng SQL ở đây không
-- tự kích hoạt, nên phải tự đồng bộ so_du_vi = tổng so_tien_hoan các phiếu da_xu_ly+vi
-- của khách đó. Khách không có phiếu nào qua ví thì giữ nguyên so_du_vi = 0 mặc định.
UPDATE kh
SET so_du_vi = tong.so_tien
FROM khach_hang kh
JOIN (
    SELECT d.khach_hang_id, SUM(p.so_tien_hoan) AS so_tien
    FROM phieu_tra_hang p
    JOIN don_hang d ON d.don_hang_id = p.don_hang_id
    WHERE p.trang_thai = N'da_xu_ly' AND p.hinh_thuc_hoan = N'vi'
    GROUP BY d.khach_hang_id
) tong ON tong.khach_hang_id = kh.khach_hang_id;

DROP TABLE #NhanVienSo;
DROP TABLE #DonDaGiao;
DROP TABLE #DonTra;
DROP TABLE #MapTraHang;
GO

-- ============================================================
--  13c. PHIẾU BẢO HÀNH DEMO
-- ============================================================
-- Random ~5% đơn "delivered" thành có phiếu bảo hành — đa dạng trạng thái xử lý
-- (con_bao_hanh/dang_xu_ly/da_xu_ly/het_bao_hanh/tu_choi) để có sẵn dữ liệu demo. Sản
-- phẩm bảo hành = dòng chi_tiet_don_hang đầu tiên (id nhỏ nhất) của đơn đó — không gán
-- chi_tiet_id (serial cụ thể) vì đơn demo không theo dõi serial theo từng đơn. ngay_mua
-- lấy từ ngay_dat của đơn (đơn demo không có ngay_giao_thuc_te), ngay_het_bh = ngay_mua +
-- 12-24 tháng ngẫu nhiên. phieu_bao_hanh không có cột nhân viên xử lý nên không cần bảng
-- tạm kiểu #NhanVienSo như mục 13b.
--
-- Vật chất hoá NEWID() vào bảng tạm thật (#DonBaoHanh) trước khi CROSS APPLY/lọc — cùng
-- lý do đã ghi ở mục 13b (tránh SQL Server tính lại NEWID() nhiều lần cho cùng 1 dòng).
IF OBJECT_ID('tempdb..#DonBaoHanh') IS NOT NULL DROP TABLE #DonBaoHanh;
IF OBJECT_ID('tempdb..#PhieuBaoHanh') IS NOT NULL DROP TABLE #PhieuBaoHanh;

SELECT don_hang_id, khach_hang_id, ngay_dat,
       ABS(CAST(CHECKSUM(NEWID()) AS BIGINT)) % 100 AS roll_chon,
       ABS(CAST(CHECKSUM(NEWID()) AS BIGINT)) % 6 AS roll_loi,
       ABS(CAST(CHECKSUM(NEWID()) AS BIGINT)) % 100 AS roll_trangthai,
       12 + ABS(CAST(CHECKSUM(NEWID()) AS BIGINT)) % 13 AS so_thang_bh,
       ABS(CAST(CHECKSUM(NEWID()) AS BIGINT)) % 14 AS roll_ngay_tiepnhan,
       ABS(CAST(CHECKSUM(NEWID()) AS BIGINT)) % 5 AS roll_ngay_traklach,
       300000 + ABS(CAST(CHECKSUM(NEWID()) AS BIGINT)) % 1700000 AS roll_chiphi
INTO #DonBaoHanh
FROM don_hang
WHERE trang_thai_don_hang = N'delivered';

SELECT
    d.don_hang_id, d.khach_hang_id,
    ct.bien_the_id,
    d.ngay_dat AS ngay_mua,
    DATEADD(MONTH, d.so_thang_bh, d.ngay_dat) AS ngay_het_bh,
    CASE d.roll_loi
         WHEN 0 THEN N'Máy không lên nguồn'
         WHEN 1 THEN N'Pin sạc không vào, đèn báo pin không sáng'
         WHEN 2 THEN N'Màn hình bị sọc, ám màu'
         WHEN 3 THEN N'Bàn phím một số phím không nhận tín hiệu'
         WHEN 4 THEN N'Ổ cứng phát ra tiếng kêu lạ'
         ELSE N'Wifi chập chờn, hay bị rớt mạng'
    END AS mo_ta_loi,
    CASE
        WHEN d.roll_trangthai < 30 THEN N'con_bao_hanh'
        WHEN d.roll_trangthai < 55 THEN N'dang_xu_ly'
        WHEN d.roll_trangthai < 80 THEN N'da_xu_ly'
        WHEN d.roll_trangthai < 90 THEN N'het_bao_hanh'
        ELSE N'tu_choi'
    END AS trang_thai,
    d.roll_ngay_tiepnhan, d.roll_ngay_traklach, d.roll_chiphi
INTO #PhieuBaoHanh
FROM #DonBaoHanh d
CROSS APPLY (
    SELECT TOP 1 bien_the_id
    FROM chi_tiet_don_hang
    WHERE don_hang_id = d.don_hang_id
    ORDER BY chi_tiet_don_hang_id
) ct
WHERE d.roll_chon < 5;

INSERT INTO phieu_bao_hanh (don_hang_id, bien_the_id, khach_hang_id, ngay_mua, ngay_het_bh,
                             ngay_tiep_nhan, ngay_tra_khach, mo_ta_loi, ket_qua_xu_ly, trang_thai,
                             chi_phi_phat_sinh, ghi_chu)
SELECT
    p.don_hang_id, p.bien_the_id, p.khach_hang_id, p.ngay_mua, p.ngay_het_bh,
    CASE WHEN p.trang_thai IN (N'dang_xu_ly', N'da_xu_ly', N'het_bao_hanh', N'tu_choi')
         THEN DATEADD(DAY, p.roll_ngay_tiepnhan, p.ngay_mua) ELSE NULL END,
    CASE WHEN p.trang_thai IN (N'da_xu_ly', N'het_bao_hanh', N'tu_choi')
         THEN DATEADD(DAY, p.roll_ngay_tiepnhan + p.roll_ngay_traklach, p.ngay_mua) ELSE NULL END,
    p.mo_ta_loi,
    CASE p.trang_thai
         WHEN N'da_xu_ly' THEN N'Đã sửa chữa/thay thế linh kiện, bàn giao lại cho khách'
         WHEN N'het_bao_hanh' THEN N'Từ chối — máy đã hết hạn bảo hành theo hóa đơn'
         WHEN N'tu_choi' THEN N'Từ chối — lỗi không thuộc diện bảo hành'
         ELSE NULL
    END,
    p.trang_thai,
    CASE WHEN p.trang_thai = N'da_xu_ly' THEN p.roll_chiphi ELSE 0 END,
    N'Dữ liệu demo'
FROM #PhieuBaoHanh p;

DROP TABLE #DonBaoHanh;
DROP TABLE #PhieuBaoHanh;
GO

-- ============================================================
--  14. NÂNG TỒN KHO DEMO (mỗi biến thể ~20-30 máy, trừ 1 biến thể sắp hết hàng)
-- ============================================================
-- Đồng bộ ton_kho TRƯỚC — nhiều biến thể ngoài Dell đang bị lệch (so_luong_ton_thuc_te
-- sai lệch so với số serial "trong_kho" thật) do 1 dòng UPDATE reset cũ trong file này.
-- Trigger trg_CapNhatTonKhoThucTe chỉ CỘNG/TRỪ phần thay đổi (delta) mỗi khi serial đổi
-- trạng thái — nếu baseline sai từ trước, delta cộng/trừ vẫn cho ra kết quả sai và có
-- thể vi phạm CHECK (>= 0). Phải sửa đúng baseline ở đây trước khi các bước bên dưới
-- thêm/đổi trạng thái serial.
UPDATE tk
SET so_luong_ton_thuc_te = ISNULL(tinh_lai.trong_kho, 0),
    so_luong_giu         = ISNULL(tinh_lai.giu_hang, 0)
FROM ton_kho tk
LEFT JOIN (
    SELECT bien_the_id,
           SUM(CASE WHEN trang_thai = N'trong_kho' THEN 1 ELSE 0 END) AS trong_kho,
           SUM(CASE WHEN trang_thai = N'giu_hang'  THEN 1 ELSE 0 END) AS giu_hang
    FROM chi_tiet_san_pham
    GROUP BY bien_the_id
) tinh_lai ON tk.bien_the_id = tinh_lai.bien_the_id;
GO

-- Chỉ chạy 1 lần (đánh dấu bằng tiền tố serial 'RESTOCK-') dù file được Execute lại
-- bao nhiêu lần — không cộng dồn thêm máy mỗi lần chạy.
IF NOT EXISTS (SELECT 1 FROM chi_tiet_san_pham WHERE so_serial LIKE N'RESTOCK-%')
BEGIN
    ;WITH Numbers AS (
        SELECT 1 AS n
        UNION ALL SELECT n + 1 FROM Numbers WHERE n < 30
    ),
    Targets AS (
        -- Mỗi biến thể mục tiêu 20-30 máy (rải theo bien_the_id cho đa dạng), riêng
        -- bien_the_id=37 (Dell XPS 15 i9 32GB, ton_kho_toi_thieu=1) giữ mục tiêu chỉ 1
        -- máy để luôn hiện "sắp hết hàng" làm demo.
        SELECT bien_the_id,
               CASE WHEN bien_the_id = 37 THEN 1 ELSE 20 + (bien_the_id % 11) END AS muc_tieu
        FROM bien_the_san_pham
    ),
    TonThucTe AS (
        SELECT bien_the_id, COUNT(*) AS so_luong
        FROM chi_tiet_san_pham
        WHERE trang_thai = N'trong_kho'
        GROUP BY bien_the_id
    )
    INSERT INTO chi_tiet_san_pham (bien_the_id, so_serial, trang_thai, ngay_nhap_kho)
    SELECT t.bien_the_id,
           N'RESTOCK-' + CAST(t.bien_the_id AS NVARCHAR(10)) + '-' + RIGHT('00' + CAST(n.n AS VARCHAR(2)), 2),
           N'trong_kho',
           GETDATE()
    FROM Targets t
    JOIN Numbers n ON n.n <= (t.muc_tieu - ISNULL((SELECT tt.so_luong FROM TonThucTe tt WHERE tt.bien_the_id = t.bien_the_id), 0))
    OPTION (MAXRECURSION 100);
END
GO

-- bien_the_id=37 đã có sẵn vài máy trong_kho — đánh dấu bớt còn đúng 1 máy để mô phỏng
-- sản phẩm sắp hết hàng. Tách guard riêng theo số lượng thật (không dùng chung guard
-- 'RESTOCK-%' ở trên) — tự chạy lại đến khi đúng còn 1 máy, kể cả khi lần trước lỡ dở.
IF (SELECT COUNT(*) FROM chi_tiet_san_pham WHERE bien_the_id = 37 AND trang_thai = N'trong_kho') > 1
BEGIN
    UPDATE TOP (1) chi_tiet_san_pham
    SET trang_thai = N'da_ban'
    WHERE bien_the_id = 37 AND trang_thai = N'trong_kho';
END
GO

-- Đồng bộ lại lần nữa sau khi thêm máy demo + đánh dấu bán ở trên — an toàn chạy lại
-- nhiều lần, luôn tính lại từ dữ liệu thật, không cộng dồn sai.
UPDATE tk
SET so_luong_ton_thuc_te = ISNULL(tinh_lai.trong_kho, 0),
    so_luong_giu         = ISNULL(tinh_lai.giu_hang, 0)
FROM ton_kho tk
LEFT JOIN (
    SELECT bien_the_id,
           SUM(CASE WHEN trang_thai = N'trong_kho' THEN 1 ELSE 0 END) AS trong_kho,
           SUM(CASE WHEN trang_thai = N'giu_hang'  THEN 1 ELSE 0 END) AS giu_hang
    FROM chi_tiet_san_pham
    GROUP BY bien_the_id
) tinh_lai ON tk.bien_the_id = tinh_lai.bien_the_id;
GO

-- Serial mẫu cho linh kiện rời (CPU/RAM/GPU/Ổ cứng) — 10 serial/loại, rải đều qua các
-- mục danh mục đã seed ở trên (dm_cpu 1-7, dm_ram 1-5, dm_gpu 1-5, dm_o_cung 1-4).
-- Trước đây các bảng chi_tiet_cpu/ram/gpu/o_cung không có dữ liệu mẫu nên tab "Serial"
-- (Kho hàng) chỉ thấy serial sản phẩm, không thấy serial linh kiện.
;WITH Seq(n) AS (SELECT n FROM (VALUES(1),(2),(3),(4),(5),(6),(7),(8),(9),(10)) v(n))
INSERT INTO chi_tiet_cpu (cpu_id, so_serial, trang_thai)
SELECT ((n - 1) % 7) + 1, N'CPU-' + RIGHT('0' + CAST(n AS VARCHAR(2)), 2), N'trong_kho'
FROM Seq;
GO

;WITH Seq(n) AS (SELECT n FROM (VALUES(1),(2),(3),(4),(5),(6),(7),(8),(9),(10)) v(n))
INSERT INTO chi_tiet_ram (ram_id, so_serial, trang_thai)
SELECT ((n - 1) % 5) + 1, N'RAM-' + RIGHT('0' + CAST(n AS VARCHAR(2)), 2), N'trong_kho'
FROM Seq;
GO

;WITH Seq(n) AS (SELECT n FROM (VALUES(1),(2),(3),(4),(5),(6),(7),(8),(9),(10)) v(n))
INSERT INTO chi_tiet_gpu (gpu_id, so_serial, trang_thai)
SELECT ((n - 1) % 5) + 1, N'GPU-' + RIGHT('0' + CAST(n AS VARCHAR(2)), 2), N'trong_kho'
FROM Seq;
GO

;WITH Seq(n) AS (SELECT n FROM (VALUES(1),(2),(3),(4),(5),(6),(7),(8),(9),(10)) v(n))
INSERT INTO chi_tiet_o_cung (o_cung_id, so_serial, trang_thai)
SELECT ((n - 1) % 4) + 1, N'OCUNG-' + RIGHT('0' + CAST(n AS VARCHAR(2)), 2), N'trong_kho'
FROM Seq;
GO

-- ============================================================
--  CÀI ĐẶT HỆ THỐNG (singleton — luôn đúng 1 dòng, cai_dat_id = 1)
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'cai_dat_he_thong')
BEGIN
    CREATE TABLE cai_dat_he_thong (
        cai_dat_id                INT            PRIMARY KEY,
        ten_cua_hang              NVARCHAR(200)  NOT NULL DEFAULT N'SAOPhone',
        dia_chi                   NVARCHAR(300)  NOT NULL DEFAULT N'',
        so_dien_thoai             NVARCHAR(20)   NOT NULL DEFAULT N'',
        email                     NVARCHAR(100)  NOT NULL DEFAULT N'',
        ma_so_thue                NVARCHAR(20)   NOT NULL DEFAULT N'',
        logo_url                  NVARCHAR(300)  NULL,
        nguong_ton_kho_mac_dinh   INT            NOT NULL DEFAULT 5,
        ngon_ngu_mac_dinh         VARCHAR(5)     NOT NULL DEFAULT 'vi'
            CONSTRAINT CK_cai_dat_ngonngu CHECK (ngon_ngu_mac_dinh IN ('vi','en','zh','ja','ko')),
        dinh_dang_so              VARCHAR(5)     NOT NULL DEFAULT 'vi'
            CONSTRAINT CK_cai_dat_dinhdangso CHECK (dinh_dang_so IN ('vi','en'))
    );
END

IF NOT EXISTS (SELECT 1 FROM cai_dat_he_thong WHERE cai_dat_id = 1)
BEGIN
    INSERT INTO cai_dat_he_thong (cai_dat_id) VALUES (1);
END
GO

-- ============================================================
--  Mã vận đơn & Lịch sử trạng thái đơn hàng
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('don_hang') AND name = 'ma_van_don')
BEGIN
    ALTER TABLE don_hang ADD ma_van_don VARCHAR(50) NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'lich_su_don_hang')
BEGIN
    CREATE TABLE lich_su_don_hang (
        lich_su_id     INT           IDENTITY(1,1) PRIMARY KEY,
        don_hang_id    INT           NOT NULL,
        trang_thai_cu  NVARCHAR(30)  NULL,
        trang_thai_moi NVARCHAR(30)  NOT NULL,
        thoi_gian      DATETIME      NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_lsdh_don_hang FOREIGN KEY (don_hang_id) REFERENCES don_hang(don_hang_id) ON DELETE CASCADE
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_lsdh_don_hang')
    CREATE INDEX IX_lsdh_don_hang ON lich_su_don_hang(don_hang_id, thoi_gian);
GO

-- Tự ghi log mỗi khi trạng thái đơn đổi — chỗ duy nhất phát sinh log, không cần backend
-- Java can thiệp, không sợ thiếu dòng nếu sau này có thêm đường cập nhật trạng thái khác.
CREATE OR ALTER TRIGGER trg_don_hang_log_trangthai
ON don_hang
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF UPDATE(trang_thai_don_hang)
    BEGIN
        INSERT INTO lich_su_don_hang (don_hang_id, trang_thai_cu, trang_thai_moi, thoi_gian)
        SELECT i.don_hang_id, d.trang_thai_don_hang, i.trang_thai_don_hang, GETDATE()
        FROM inserted i
        JOIN deleted d ON d.don_hang_id = i.don_hang_id
        WHERE d.trang_thai_don_hang <> i.trang_thai_don_hang;
    END
END
GO

-- ============================================================
--  Tích điểm mua hàng & Đổi điểm lấy voucher
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('don_hang') AND name = 'da_cong_diem')
BEGIN
    ALTER TABLE don_hang ADD da_cong_diem BIT NOT NULL DEFAULT 0;
END
GO

-- Cộng điểm khi đơn chuyển "delivered" — tức lúc khách bấm "Xác nhận đã nhận hàng" (đơn
-- online, xem xacNhanDaNhanHang() ở DonHangService) hoặc lúc bán tại quầy (đơn in_store vào
-- thẳng "delivered"). Không cộng sớm hơn (lúc đặt/thanh toán) để tránh khách "cày" điểm bằng
-- cách đặt rồi hủy liên tục — đơn đã "delivered" không còn hủy được nữa (xem
-- CHUYEN_TRANG_THAI_DON_HANG), nên không cần trigger trừ điểm riêng cho trường hợp hủy.
IF EXISTS (SELECT 1 FROM sys.triggers WHERE name = 'trg_don_hang_tru_diem_huy')
    DROP TRIGGER trg_don_hang_tru_diem_huy;
GO

CREATE OR ALTER TRIGGER trg_don_hang_cong_diem
ON don_hang
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF UPDATE(trang_thai_don_hang)
    BEGIN
        -- Cộng dồn theo GROUP BY khach_hang_id trước khi UPDATE — 1 câu UPDATE...FROM...JOIN
        -- trực tiếp trên "many" side chỉ lấy được giá trị từ 1 dòng khớp bất kỳ khi 1 khách có
        -- nhiều đơn cùng chuyển "delivered" trong cùng 1 batch, làm mất điểm âm thầm.
        UPDATE kh
        SET kh.diem_tich_luy = kh.diem_tich_luy + x.diem_cong
        FROM khach_hang kh
        JOIN (
            SELECT i.khach_hang_id, SUM(FLOOR(i.thanh_tien / 10000)) AS diem_cong
            FROM inserted i
            JOIN deleted d ON d.don_hang_id = i.don_hang_id
            WHERE i.trang_thai_don_hang = N'delivered'
              AND d.trang_thai_don_hang <> N'delivered'
              AND i.da_cong_diem = 0
            GROUP BY i.khach_hang_id
        ) x ON x.khach_hang_id = kh.khach_hang_id;

        UPDATE don_hang SET da_cong_diem = 1
        WHERE don_hang_id IN (
            SELECT i.don_hang_id FROM inserted i JOIN deleted d ON d.don_hang_id = i.don_hang_id
            WHERE i.trang_thai_don_hang = N'delivered' AND d.trang_thai_don_hang <> N'delivered' AND i.da_cong_diem = 0
        );
    END
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'dm_doi_thuong')
BEGIN
    CREATE TABLE dm_doi_thuong (
        doi_thuong_id   INT            IDENTITY(1,1) PRIMARY KEY,
        ten             NVARCHAR(150)  NOT NULL,
        mo_ta           NVARCHAR(500)  NULL,
        diem_can        INT            NOT NULL CONSTRAINT CK_ddt_diemcan CHECK (diem_can > 0),
        loai            NVARCHAR(20)   NOT NULL CONSTRAINT CK_ddt_loai CHECK (loai IN (N'percent', N'fixed')),
        gia_tri         DECIMAL(18,0)  NOT NULL CONSTRAINT CK_ddt_giatri CHECK (gia_tri > 0),
        CONSTRAINT CK_ddt_percent_max100 CHECK (loai <> N'percent' OR gia_tri <= 100),
        gia_tri_toi_da  DECIMAL(18,0)  NULL,
        trang_thai      NVARCHAR(20)   NOT NULL DEFAULT N'active'
            CONSTRAINT CK_ddt_trangthai CHECK (trang_thai IN (N'active', N'inactive')),
        ngay_tao        DATETIME       NOT NULL DEFAULT GETDATE()
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'phieu_giam_gia_ca_nhan')
BEGIN
    CREATE TABLE phieu_giam_gia_ca_nhan (
        phieu_id       INT            IDENTITY(1,1) PRIMARY KEY,
        khach_hang_id  INT            NOT NULL,
        doi_thuong_id  INT            NULL,
        ma_phieu       VARCHAR(50)    NOT NULL UNIQUE DEFAULT UPPER(LEFT(REPLACE(CAST(NEWID() AS VARCHAR(36)), '-', ''), 12)),
        loai           NVARCHAR(20)   NOT NULL CONSTRAINT CK_pggcn_loai CHECK (loai IN (N'percent', N'fixed')),
        gia_tri        DECIMAL(18,0)  NOT NULL,
        gia_tri_toi_da DECIMAL(18,0)  NULL,
        da_su_dung     BIT            NOT NULL DEFAULT 0,
        ngay_doi       DATETIME       NOT NULL DEFAULT GETDATE(),
        ngay_het_han   DATETIME       NOT NULL,
        don_hang_id    INT            NULL,
        don_hang_toi_thieu DECIMAL(18,0) NULL,
        CONSTRAINT FK_pggcn_khach_hang FOREIGN KEY (khach_hang_id) REFERENCES khach_hang(khach_hang_id),
        CONSTRAINT FK_pggcn_doi_thuong FOREIGN KEY (doi_thuong_id) REFERENCES dm_doi_thuong(doi_thuong_id),
        CONSTRAINT FK_pggcn_don_hang   FOREIGN KEY (don_hang_id)   REFERENCES don_hang(don_hang_id)
    );
END
GO

-- Voucher cá nhân trúng từ vòng quay giữ nguyên đơn tối thiểu của khuyến mãi gốc (khách vẫn
-- phải đạt đơn tối thiểu mới áp được, y hệt mã khuyến mãi công khai) — cột thêm sau, ALTER
-- idempotent cho DB đã có sẵn bảng từ trước.
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('phieu_giam_gia_ca_nhan') AND name = 'don_hang_toi_thieu')
BEGIN
    ALTER TABLE phieu_giam_gia_ca_nhan ADD don_hang_toi_thieu DECIMAL(18,0) NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'lich_su_tang_diem')
BEGIN
    CREATE TABLE lich_su_tang_diem (
        id             INT           IDENTITY(1,1) PRIMARY KEY,
        khach_hang_id  INT           NOT NULL,
        nhan_vien_id   INT           NOT NULL,
        so_diem        INT           NOT NULL CONSTRAINT CK_lstd_sodiem CHECK (so_diem > 0),
        ly_do          NVARCHAR(255) NULL,
        ngay_tao       DATETIME      NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_lstd_khach_hang FOREIGN KEY (khach_hang_id) REFERENCES khach_hang(khach_hang_id),
        CONSTRAINT FK_lstd_nhan_vien  FOREIGN KEY (nhan_vien_id)  REFERENCES nhan_vien(nhan_vien_id)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'cau_hinh_vong_quay')
BEGIN
    CREATE TABLE cau_hinh_vong_quay (
        id             INT            NOT NULL PRIMARY KEY CHECK (id = 1),
        diem_moi_luot  INT            NOT NULL CHECK (diem_moi_luot > 0),
        ty_le_truot    INT            NOT NULL CHECK (ty_le_truot BETWEEN 0 AND 100),
        ngay_cap_nhat  DATETIME       NOT NULL DEFAULT GETDATE()
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'lich_su_quay')
BEGIN
    CREATE TABLE lich_su_quay (
        id                         INT      IDENTITY(1,1) PRIMARY KEY,
        khach_hang_id              INT      NOT NULL,
        ngay_quay                  DATETIME NOT NULL DEFAULT GETDATE(),
        ket_qua                    NVARCHAR(10) NOT NULL CONSTRAINT CK_lsq_ket_qua CHECK (ket_qua IN (N'trung', N'truot')),
        khuyen_mai_id              INT      NULL,
        phieu_giam_gia_ca_nhan_id  INT      NULL,
        diem_da_tru                INT      NOT NULL,
        CONSTRAINT FK_lsq_khach_hang FOREIGN KEY (khach_hang_id) REFERENCES khach_hang(khach_hang_id),
        CONSTRAINT FK_lsq_khuyen_mai FOREIGN KEY (khuyen_mai_id) REFERENCES khuyen_mai(khuyen_mai_id),
        CONSTRAINT FK_lsq_phieu FOREIGN KEY (phieu_giam_gia_ca_nhan_id) REFERENCES phieu_giam_gia_ca_nhan(phieu_id)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_pggcn_khach_hang')
    CREATE INDEX IX_pggcn_khach_hang ON phieu_giam_gia_ca_nhan(khach_hang_id, da_su_dung);
GO

-- ============================================================
--  Mở rộng danh sách trạng thái đơn hàng theo thời gian (out_for_delivery, rồi
--  awaiting_confirmation) — Drop-rồi-add (không gói trong CREATE TABLE) nên chạy lại file
--  bao nhiêu lần trên DB đã có sẵn cũng an toàn. "awaiting_confirmation": admin đã bấm "Đã
--  giao" nhưng khách chưa bấm "Xác nhận đã nhận hàng" — chỉ khách (hoặc staff) xác nhận mới
--  chuyển tiếp "delivered", đơn mới thật sự rơi vào tab "Hoàn tất" phía khách hàng. Xem
--  DonHangService.xacNhanDaNhanHang() (BackEnd) và CHUYEN_TRANG_THAI_DON_HANG.
-- ============================================================
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_dh_trangthai')
    ALTER TABLE don_hang DROP CONSTRAINT CK_dh_trangthai;
ALTER TABLE don_hang ADD CONSTRAINT CK_dh_trangthai
    CHECK (trang_thai_don_hang IN (N'pending', N'confirmed', N'processing', N'shipping', N'out_for_delivery', N'awaiting_confirmation', N'delivered', N'cancelled', N'returned'));
GO
GO
select*from ton_kho
select*from bien_the_san_pham
select*from chi_tiet_san_pham
