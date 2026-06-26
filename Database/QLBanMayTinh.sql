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
                             quoc_gia         NVARCHAR(100)  NULL,
                             mo_ta            NVARCHAR(500)  NULL,
                             trang_thai       NVARCHAR(20)   NOT NULL DEFAULT N'active'
        CONSTRAINT CK_thuong_hieu_trangthai CHECK (trang_thai IN (N'active', N'inactive')),
                             ngay_tao         DATETIME       NOT NULL DEFAULT GETDATE()
);

CREATE TABLE danh_muc (
                          danh_muc_id   INT            IDENTITY(1,1) PRIMARY KEY,
                          ten_danh_muc  NVARCHAR(100)  NOT NULL UNIQUE,
                          mo_ta         NVARCHAR(500)  NULL,
                          trang_thai    NVARCHAR(20)   NOT NULL DEFAULT N'active'
        CONSTRAINT CK_danh_muc_trangthai CHECK (trang_thai IN (N'active', N'inactive')),
                          ngay_tao      DATETIME       NOT NULL DEFAULT GETDATE()
);

-- Bảng phân loại theo mục đích sử dụng (văn phòng, gaming, đồ họa,...)
-- Tách riêng khỏi danh_muc để 1 sản phẩm có thể thuộc nhiều nhóm
CREATE TABLE phan_loai (
                           phan_loai_id    INT            IDENTITY(1,1) PRIMARY KEY,
                           ma_phan_loai    VARCHAR(30)    NOT NULL UNIQUE,   -- key dùng cho query/filter: 'gaming', 'van_phong'...
                           ten_phan_loai   NVARCHAR(100)  NOT NULL,           -- tên hiển thị trên UI
                           mo_ta           NVARCHAR(255)  NULL,
                           thu_tu          INT            NOT NULL DEFAULT 0, -- thứ tự hiện trên filter bar
                           trang_thai      NVARCHAR(20)   NOT NULL DEFAULT N'active'
        CONSTRAINT CK_pl_trangthai CHECK (trang_thai IN (N'active', N'inactive'))
);

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
GO

-- ============================================================
--  2. KHÁCH HÀNG & NHÂN VIÊN
-- ============================================================
CREATE TABLE khach_hang (
                            khach_hang_id  INT            IDENTITY(1,1) PRIMARY KEY,
                            ho_ten         NVARCHAR(150)  NOT NULL,
                            so_dien_thoai  VARCHAR(20)    NOT NULL UNIQUE,
                            email          VARCHAR(100)   NOT NULL UNIQUE,
                            username       VARCHAR(50)    NOT NULL UNIQUE,
                            mat_khau       VARCHAR(255)   NOT NULL,
                            dia_chi        NVARCHAR(255)  NULL,
                            loai_khach     NVARCHAR(20)   NOT NULL DEFAULT N'ca_nhan'
        CONSTRAINT CK_khach_hang_loai
        CHECK (loai_khach IN (N'ca_nhan', N'doanh_nghiep')),
                            ten_cong_ty    NVARCHAR(200)  NULL,
                            ma_so_thue     VARCHAR(20)    NULL,
                            diem_tich_luy  INT            NOT NULL DEFAULT 0
                                CONSTRAINT CK_kh_diem CHECK (diem_tich_luy >= 0),
                            trang_thai     NVARCHAR(20)   NOT NULL DEFAULT N'active'
        CONSTRAINT CK_khach_hang_trangthai
        CHECK (trang_thai IN (N'active', N'inactive', N'blocked')),
                            ngay_tao       DATETIME       NOT NULL DEFAULT GETDATE(),
                            ngay_cap_nhat  DATETIME       NOT NULL DEFAULT GETDATE()
);

CREATE TABLE chuc_vu (
                         chuc_vu_id   INT            IDENTITY(1,1) PRIMARY KEY,
                         ten_chuc_vu  NVARCHAR(100)  NOT NULL UNIQUE,
                         cap_do       INT            NOT NULL DEFAULT 1 CONSTRAINT CK_cv_capdo CHECK (cap_do BETWEEN 1 AND 9),
                         mo_ta        NVARCHAR(255)  NULL
    -- cap_do: 1=Nhân viên bán hàng, 2=Thủ kho/KTV, 3=Quản lý, 9=Admin
);

CREATE TABLE nhan_vien (
                           nhan_vien_id   INT            IDENTITY(1,1) PRIMARY KEY,
                           ho_ten         NVARCHAR(150)  NOT NULL,
                           so_dien_thoai  VARCHAR(20)    NULL UNIQUE,
                           email          VARCHAR(100)   NULL UNIQUE,
                           chuc_vu_id     INT            NULL,
                           username       VARCHAR(50)    NOT NULL UNIQUE,
                           mat_khau_hash  VARCHAR(255)   NOT NULL,
                           luong_co_ban   DECIMAL(18,0)  NOT NULL DEFAULT 0 CONSTRAINT CK_nv_luong CHECK (luong_co_ban >= 0),
                           trang_thai     NVARCHAR(20)   NOT NULL DEFAULT N'active'
        CONSTRAINT CK_nhan_vien_trangthai CHECK (trang_thai IN (N'active', N'inactive', N'nghi_viec')),
                           ngay_tao       DATETIME       NOT NULL DEFAULT GETDATE(),
                           ngay_cap_nhat  DATETIME       NOT NULL DEFAULT GETDATE(),
                           CONSTRAINT FK_nhan_vien_chuc_vu FOREIGN KEY (chuc_vu_id) REFERENCES chuc_vu(chuc_vu_id)
);
GO

-- ============================================================
--  3. SẢN PHẨM GỐC
-- ============================================================
CREATE TABLE san_pham (
                          san_pham_id     INT             IDENTITY(1,1) PRIMARY KEY,
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
                          ngay_tao        DATETIME        NOT NULL DEFAULT GETDATE(),
                          CONSTRAINT FK_san_pham_thuong_hieu  FOREIGN KEY (thuong_hieu_id)  REFERENCES thuong_hieu(thuong_hieu_id),
                          CONSTRAINT FK_san_pham_danh_muc     FOREIGN KEY (danh_muc_id)     REFERENCES danh_muc(danh_muc_id),
                          CONSTRAINT FK_san_pham_nha_cung_cap FOREIGN KEY (nha_cung_cap_id) REFERENCES nha_cung_cap(nha_cung_cap_id)
);
GO

-- Junction table: 1 sản phẩm có thể thuộc nhiều phân loại (nhiều-nhiều)
CREATE TABLE san_pham_phan_loai (
                                    san_pham_id  INT NOT NULL,
                                    phan_loai_id INT NOT NULL,
                                    PRIMARY KEY (san_pham_id, phan_loai_id),
                                    CONSTRAINT FK_sppl_san_pham  FOREIGN KEY (san_pham_id)  REFERENCES san_pham(san_pham_id) ON DELETE CASCADE,
                                    CONSTRAINT FK_sppl_phan_loai FOREIGN KEY (phan_loai_id) REFERENCES phan_loai(phan_loai_id)
);
GO

-- Danh mục CPU/RAM/ổ cứng/GPU — lưu LOẠI linh kiện, không phải đơn vị vật lý
CREATE TABLE dm_cpu    ( cpu_id    INT IDENTITY(1,1) PRIMARY KEY, ten_cpu     NVARCHAR(100) NOT NULL UNIQUE );
CREATE TABLE dm_ram    ( ram_id    INT IDENTITY(1,1) PRIMARY KEY, dung_luong  NVARCHAR(50)  NOT NULL UNIQUE );
CREATE TABLE dm_o_cung ( o_cung_id INT IDENTITY(1,1) PRIMARY KEY, loai_o_cung NVARCHAR(100) NOT NULL UNIQUE );
CREATE TABLE dm_gpu    ( gpu_id    INT IDENTITY(1,1) PRIMARY KEY, ten_gpu     NVARCHAR(100) NOT NULL UNIQUE );
GO

-- ============================================================
--  4. BIẾN THỂ SẢN PHẨM (ĐỊNH GIÁ & THÔNG SỐ KỸ THUẬT)
-- ============================================================
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

                                   CONSTRAINT FK_bien_the_san_pham FOREIGN KEY (san_pham_id) REFERENCES san_pham(san_pham_id) ON DELETE CASCADE,
                                   CONSTRAINT FK_bien_the_cpu      FOREIGN KEY (cpu_id)      REFERENCES dm_cpu(cpu_id),
                                   CONSTRAINT FK_bien_the_ram      FOREIGN KEY (ram_id)      REFERENCES dm_ram(ram_id),
                                   CONSTRAINT FK_bien_the_ocung    FOREIGN KEY (o_cung_id)   REFERENCES dm_o_cung(o_cung_id),
                                   CONSTRAINT FK_bien_the_gpu      FOREIGN KEY (gpu_id)      REFERENCES dm_gpu(gpu_id)
);
GO

-- ============================================================
--  5. KHO HÀNG: TỒN KHO & TỪNG ĐƠN VỊ VẬT LÝ (CÓ SERIAL)
-- ============================================================
CREATE TABLE ton_kho (
                         cau_hinh_id          INT      IDENTITY(1,1) PRIMARY KEY,
                         bien_the_id          INT      NOT NULL UNIQUE,
                         so_luong_ton_thuc_te INT      NOT NULL DEFAULT 0 CONSTRAINT CK_chtk_tonthucte CHECK (so_luong_ton_thuc_te >= 0),
                         so_luong_giu         INT      NOT NULL DEFAULT 0 CONSTRAINT CK_chtk_giu        CHECK (so_luong_giu >= 0),
                         ton_kho_toi_thieu    INT      NOT NULL DEFAULT 5  CONSTRAINT CK_chtk_toithieu  CHECK (ton_kho_toi_thieu >= 0),
                         CONSTRAINT CK_chtk_giu_le_ton CHECK (so_luong_giu <= so_luong_ton_thuc_te),
                         ngay_cap_nhat        DATETIME NOT NULL DEFAULT GETDATE(),
                         CONSTRAINT FK_ton_kho_bt FOREIGN KEY (bien_the_id) REFERENCES bien_the_san_pham(bien_the_id) ON DELETE CASCADE
);
GO

-- Mỗi hàng = 1 đơn vị laptop/phụ kiện vật lý, nhận dạng qua số serial
-- so_serial: bắt buộc (NOT NULL), in trên máy hoặc hộp đóng gói
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
GO

-- Serial phải duy nhất toàn hệ thống
CREATE UNIQUE INDEX UX_ctsp_serial ON chi_tiet_san_pham(so_serial);
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
GO

-- ============================================================
--  7. PHIẾU NHẬP KHO & ĐƠN HÀNG
-- ============================================================
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
GO

-- ============================================================
--  8. CHI TIẾT ĐƠN HÀNG & LỊCH SỬ KHO
-- ============================================================
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

CREATE TABLE lich_su_ton_kho (
                                 lich_su_id        INT            IDENTITY(1,1) PRIMARY KEY,
                                 bien_the_id       INT            NOT NULL,
                                 chi_tiet_id       INT            NULL,
                                 loai_bien_dong    NVARCHAR(30)   NOT NULL
        CONSTRAINT CK_lsdk_loai CHECK (loai_bien_dong IN (N'nhap', N'xuat_ban', N'tra_hang', N'dieu_chinh', N'huy')),
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
                                CONSTRAINT FK_pth_don_hang  FOREIGN KEY (don_hang_id)  REFERENCES don_hang(don_hang_id),
                                CONSTRAINT FK_pth_nhan_vien FOREIGN KEY (nhan_vien_id) REFERENCES nhan_vien(nhan_vien_id)
);

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
                                chi_phi_phat_sinh DECIMAL(18,0)  NOT NULL DEFAULT 0 CONSTRAINT CK_pbh_chiphi CHECK (chi_phi_phat_sinh >= 0),
                                ghi_chu           NVARCHAR(500)  NULL,
                                CONSTRAINT FK_pbh_don_hang   FOREIGN KEY (don_hang_id)   REFERENCES don_hang(don_hang_id),
                                CONSTRAINT FK_pbh_bien_the   FOREIGN KEY (bien_the_id)   REFERENCES bien_the_san_pham(bien_the_id),
                                CONSTRAINT FK_pbh_khach_hang FOREIGN KEY (khach_hang_id) REFERENCES khach_hang(khach_hang_id),
                                CONSTRAINT FK_pbh_ctsp       FOREIGN KEY (chi_tiet_id)   REFERENCES chi_tiet_san_pham(chi_tiet_id)
);
GO

-- ============================================================
--  10. TRIGGERS
-- ============================================================

-- Trigger 1: Tự động cập nhật tồn kho khi thêm/đổi trạng thái/xóa đơn vị vật lý
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
ROLLBACK TRANSACTION; RETURN;
END

    IF EXISTS (
        SELECT 1 FROM inserted i
        JOIN khuyen_mai km ON i.khuyen_mai_id = km.khuyen_mai_id
        WHERE km.so_luong_toi_da IS NOT NULL AND km.so_lan_da_dung >= km.so_luong_toi_da
    )
BEGIN
        RAISERROR(N'Mã khuyến mãi đã hết lượt sử dụng', 16, 1);
ROLLBACK TRANSACTION; RETURN;
END

    IF EXISTS (
        SELECT 1 FROM inserted i
        JOIN khuyen_mai km ON i.khuyen_mai_id = km.khuyen_mai_id
        WHERE km.don_hang_toi_thieu IS NOT NULL AND i.tong_tien < km.don_hang_toi_thieu
    )
BEGIN
        RAISERROR(N'Đơn hàng chưa đạt giá trị tối thiểu để áp dụng mã khuyến mãi', 16, 1);
ROLLBACK TRANSACTION; RETURN;
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
CREATE INDEX IX_bien_the_san_pham   ON bien_the_san_pham(san_pham_id);
CREATE INDEX IX_bien_the_gia        ON bien_the_san_pham(gia_ban, gia_nhap);
CREATE INDEX IX_ctsp_bien_the       ON chi_tiet_san_pham(bien_the_id, trang_thai);
CREATE INDEX IX_don_hang_khach_hang ON don_hang(khach_hang_id);
CREATE INDEX IX_don_hang_ngay_dat   ON don_hang(ngay_dat DESC);
CREATE INDEX IX_don_hang_trang_thai ON don_hang(trang_thai_don_hang, ngay_dat DESC);
CREATE INDEX IX_ctdh_don_hang       ON chi_tiet_don_hang(don_hang_id);
CREATE INDEX IX_lstk_bien_the       ON lich_su_ton_kho(bien_the_id, ngay_tao DESC);
CREATE INDEX IX_khuyen_mai_ma       ON khuyen_mai(ma_khuyen_mai, trang_thai);
CREATE INDEX IX_sppl_phan_loai      ON san_pham_phan_loai(phan_loai_id);  -- filter nhanh theo phân loại
GO

-- ============================================================
--  12. VIEWS TỔNG HỢP
-- ============================================================

-- Tồn kho tổng quan — dùng cho màn hình quản lý kho
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
CREATE VIEW vw_san_pham_hien_thi AS
SELECT
    sp.san_pham_id,
    bt.bien_the_id,
    bt.ma_sku,
    sp.ten_san_pham,
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

INSERT INTO chuc_vu (ten_chuc_vu, cap_do, mo_ta) VALUES
(N'Quản lý',            3, N'Quản lý cửa hàng, duyệt đơn, xem báo cáo'),
(N'Nhân viên bán hàng', 1, N'Tạo đơn hàng, tư vấn sản phẩm'),
(N'Thủ kho',            2, N'Nhập kho, xuất kho, kiểm kê tồn kho'),
(N'Kỹ thuật viên',      2, N'Bảo hành và sửa chữa thiết bị'),
(N'Kế toán',            2, N'Quản lý tài chính, kế toán nội bộ');
-- chuc_vu: Quan_ly=1, NVBH=2, Thu_kho=3, KTV=4, Ke_toan=5

INSERT INTO nhan_vien (ho_ten, so_dien_thoai, email, chuc_vu_id, username, mat_khau_hash, luong_co_ban) VALUES
                                                                                                            (N'Nguyễn Văn An',   '0987654321', 'nhanvienan@sao.vn',   2, 'nhanvienan', '$2b$10$placeholder_hash_nv_a', 8000000),
                                                                                                            (N'Trần Thị Bảo',   '0978112233', 'nhanvienbao@sao.vn',  2, 'nhanvienbao','$2b$10$placeholder_hash_nv_b', 7500000),
                                                                                                            (N'Lê Văn Cường',   '0967223344', 'nhanviencuong@sao.vn',3, 'nhanviencuong','$2b$10$placeholder_hash_nv_c',6500000),
                                                                                                            (N'Phạm Quốc Dũng', '0956334455', 'nhanviendung@sao.vn', 4, 'nhanviendung','$2b$10$placeholder_hash_nv_d',9000000);
-- nhan_vien: An=1, Bao=2, Cuong=3, Dung=4

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

INSERT INTO khach_hang
(
    ho_ten,
    so_dien_thoai,
    email,
    username,
    mat_khau,
    dia_chi,
    loai_khach,
    diem_tich_luy
)
VALUES
    (
        N'Nghiêm Việt Anh',
        '0912345678',
        'anh.nghiem@gmail.com',
        'vietanh',
        '123456',
        N'123 Phố Huế, Hoàn Kiếm, Hà Nội',
        N'ca_nhan',
        50
    ),
    (
        N'Trần Thị Bình',
        '0901234567',
        'binh.tran@gmail.com',
        'binh',
        '123456',
        N'456 Nguyễn Trãi, Quận 5, TP.HCM',
        N'ca_nhan',
        100
    ),
    (
        N'Lê Hoàng Cường',
        '0912345000',
        'cuong.le@gmail.com',
        'cuong',
        '123456',
        N'78 Đinh Tiên Hoàng, Quận 1, TP.HCM',
        N'ca_nhan',
        250
    ),
    (
        N'Phạm Thị Duyên',
        '0934567890',
        'duyen.pham@gmail.com',
        'duyen',
        '123456',
        N'12 Trần Phú, Hải Châu, Đà Nẵng',
        N'ca_nhan',
        50
    ),
    (
        N'Nguyễn Minh Đức',
        '0956789012',
        'duc.nguyen@gmail.com',
        'duc',
        '123456',
        N'34 Hoàng Diệu, Hải Châu, Đà Nẵng',
        N'ca_nhan',
        0
    ),
    (
        N'Cty Minh Anh Tech',
        '02838901234',
        'purchase@minhanh.vn',
        'minhanh',
        '123456',
        N'50 Lê Lợi, Quận 1, TP.HCM',
        N'doanh_nghiep',
        800
    );
-- khach_hang: VietAnh=1, Binh=2, Cuong=3, Duyen=4, Duc=5, MinhAnh=6

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
INSERT INTO san_pham (ten_san_pham, thuong_hieu_id, danh_muc_id, nha_cung_cap_id, loai_san_pham, mo_ta, hinh_anh_chinh) VALUES
                                                                                                                            (N'Dell Inspiron 15 3520',    1, 1, 1, 'LAPTOP', N'Laptop văn phòng phổ thông 15.6" FHD, pin 54Wh, trọng lượng 1.7kg',   N'/images/Dell Inspiron 15 3520.webp'),
                                                                                                                            (N'Asus Vivobook 15 X1504VA', 3, 1, 2, 'LAPTOP', N'Mỏng nhẹ văn phòng, màn 15.6" FHD 60Hz, pin 50Wh cả ngày',          N'/images/Asus Vivobook 15 X1504VA.webp'),
                                                                                                                            (N'Lenovo IdeaPad 5 Pro 16',  4, 1, 2, 'LAPTOP', N'Màn 2.5K 16" 120Hz, AMD Ryzen mạnh, vỏ nhôm bền',                   N'/images/Lenovo IdeaPad 5 Pro 16.webp'),
                                                                                                                            (N'HP Envy x360 16 2024',     5, 1, 1, 'LAPTOP', N'2-in-1 cao cấp, màn OLED 2.8K cảm ứng, chip Intel Gen 13',          N'/images/HP Envy x360 16 2024.webp'),
                                                                                                                            (N'MSI Stealth 15M B12U',     6, 1, 3, 'LAPTOP', N'Gaming mỏng nhẹ RTX 4050, màn 144Hz, trọng lượng chỉ 1.7kg',        N'/images/MSI Stealth 15M B12U.webp');
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
(1, 3, N'2024-05-01',  111500000, N'hoan_thanh', N'Nhập hàng đợt 1 - Dell Inspiron 15 từ Digiworld'),
(2, 3, N'2024-06-15', 1016000000, N'hoan_thanh', N'Nhập hàng đợt 2 - Asus, Lenovo, HP, MSI từ FPT Trading');
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
(1, 1, NULL,
 N'Nghiêm Việt Anh', '0912345678', N'123 Phố Huế, Hoàn Kiếm, HN',
 15490000, 0, 30000,
 N'2024-06-10 10:30:00', N'2024-06-12 15:00:00', N'delivered', N'paid', N'online'),

-- DH2: KH3 mua HP Envy i9 — đang xử lý, đặt cọc
(3, NULL, NULL,
 N'Lê Hoàng Cường', '0912345000', N'78 Đinh Tiên Hoàng, Q1, TP.HCM',
 34990000, 0, 0,
 N'2024-07-05 14:20:00', NULL, N'processing', N'partial', N'online'),

-- DH3: KH6 (doanh nghiệp) mua Lenovo R7 — km VIP500, đang giao, đã thanh toán
(6, 2, 4,
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
(1, N'xuat_ban', -1, 1, 1, N'Xuất bán DH1 - Dell Inspiron i5'),
(8, N'xuat_ban', -1, 2, NULL, N'Xuất bán DH2 - HP Envy i9/32GB'),
(6, N'xuat_ban', -1, 3, 2, N'Xuất bán DH3 - Lenovo R7/16GB');
GO

-- ============================================================
-- 13.B. DỮ LIỆU BỔ SUNG — Thêm sản phẩm mới + màu sắc
-- ============================================================

-- ── Sản phẩm mới (sp 6-11) ───────────────────────────────────────────────────
-- thuong_hieu: Dell=1,Apple=2,Asus=3,Lenovo=4,HP=5,MSI=6,Acer=7
-- nha_cung_cap: Digiworld=1, FPT=2, Synnex=3
INSERT INTO san_pham (ten_san_pham, thuong_hieu_id, danh_muc_id, nha_cung_cap_id, loai_san_pham, mo_ta, hinh_anh_chinh) VALUES
(N'Acer Aspire 5 A515-58',   7, 1, 3, N'LAPTOP', N'Laptop học tập văn phòng phổ thông 15.6" FHD, pin 48Wh cả ngày, giá hợp lý',                 NULL),
(N'Asus ROG Strix G16 G614', 3, 1, 2, N'LAPTOP', N'Gaming cao cấp RTX 40 series, màn 16" 165Hz, tản nhiệt triple fan, RGB Aura Sync',            NULL),
(N'Lenovo Legion 5 Pro 16',  4, 1, 2, N'LAPTOP', N'Gaming-đồ họa chuyên nghiệp, màn WQXGA 165Hz, AMD Ryzen + NVIDIA RTX, vỏ nhôm',              NULL),
(N'HP Pavilion 15',          5, 1, 1, N'LAPTOP', N'Laptop gia đình phổ thông 15.6" FHD 144Hz, màu sắc đa dạng, giá cạnh tranh',                  NULL),
(N'Dell XPS 15 9530',        1, 1, 1, N'LAPTOP', N'Màn OLED 3.5K siêu nét, thiết kế siêu mỏng, lý tưởng cho sáng tạo nội dung chuyên nghiệp',  NULL),
(N'Acer Nitro V 15',         7, 1, 3, N'LAPTOP', N'Gaming tầm trung RTX 40 series, màn 144Hz, tản nhiệt mạnh, giá tốt nhất phân khúc',           NULL);
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
