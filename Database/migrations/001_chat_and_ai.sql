-- ============================================================
--  Migration: Chat & AI Chatbot
--  Chạy: sqlcmd -S localhost -d QLBanMayTinh -i Database/migrations/001_chat_and_ai.sql
-- ============================================================
USE QLBanMayTinh;
GO

SET QUOTED_IDENTIFIER ON;
GO

-- Bảng 1: Cuộc trò chuyện
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'cuoc_tro_chuyen')
BEGIN
    CREATE TABLE cuoc_tro_chuyen (
        id                     BIGINT IDENTITY(1,1) PRIMARY KEY,
        loai_khach             NVARCHAR(20) NOT NULL,
        khach_hang_id          INT NULL,
        session_id             NVARCHAR(100) NULL,
        ho_ten_khach          NVARCHAR(150) NULL,
        trang_thai             NVARCHAR(30) DEFAULT 'HOI_DAP_AI',
        nhan_vien_phu_trach    INT NULL,
        so_lan_escalate        INT DEFAULT 0,
        created_at             DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at             DATETIME DEFAULT CURRENT_TIMESTAMP,
        CONSTRAINT fk_ctc_kh FOREIGN KEY (khach_hang_id) REFERENCES khach_hang(khach_hang_id),
        CONSTRAINT fk_ctc_nv FOREIGN KEY (nhan_vien_phu_trach) REFERENCES nhan_vien(nhan_vien_id)
    );
    CREATE INDEX idx_ctc_loai ON cuoc_tro_chuyen(loai_khach);
    CREATE INDEX idx_ctc_kh ON cuoc_tro_chuyen(khach_hang_id);
    CREATE INDEX idx_ctc_trang_thai ON cuoc_tro_chuyen(trang_thai);
    PRINT 'Created: cuoc_tro_chuyen';
END
ELSE
    PRINT 'Already exists: cuoc_tro_chuyen';
GO

-- Bảng 2: Tin nhắn
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'tin_nhan')
BEGIN
    CREATE TABLE tin_nhan (
        id                     BIGINT IDENTITY(1,1) PRIMARY KEY,
        cuoc_tro_chuyen_id    BIGINT NOT NULL,
        nguoi_gui             NVARCHAR(20) NOT NULL,
        nhan_vien_id          INT NULL,
        noi_dung              NVARCHAR(MAX) NOT NULL,
        loai_nguoi_gui       NVARCHAR(20) DEFAULT 'ANONYMOUS',
        da_doc                BIT DEFAULT 0,
        la_cau_hoi_cua_ai     BIT DEFAULT 0,
        created_at            DATETIME DEFAULT CURRENT_TIMESTAMP,
        CONSTRAINT fk_tn_ctc FOREIGN KEY (cuoc_tro_chuyen_id) REFERENCES cuoc_tro_chuyen(id),
        CONSTRAINT fk_tn_nv FOREIGN KEY (nhan_vien_id) REFERENCES nhan_vien(nhan_vien_id)
    );
    CREATE INDEX idx_tn_ctc ON tin_nhan(cuoc_tro_chuyen_id);
    PRINT 'Created: tin_nhan';
END
ELSE
    PRINT 'Already exists: tin_nhan';
GO

-- Bảng 3: Cơ sở kiến thức AI
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'ai_kien_thuc')
BEGIN
    CREATE TABLE ai_kien_thuc (
        id              BIGINT IDENTITY(1,1) PRIMARY KEY,
        loai            NVARCHAR(30) NOT NULL,
        tieu_de         NVARCHAR(255) NOT NULL,
        noi_dung        NVARCHAR(MAX) NOT NULL,
        san_pham_id     INT NULL,
        active          BIT DEFAULT 1,
        created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
        CONSTRAINT fk_ai_kt_sp FOREIGN KEY (san_pham_id) REFERENCES san_pham(san_pham_id)
    );
    CREATE INDEX idx_ai_kien_thuc_loai ON ai_kien_thuc(loai);
    CREATE INDEX idx_ai_kien_thuc_active ON ai_kien_thuc(active);
    PRINT 'Created: ai_kien_thuc';
END
ELSE
    PRINT 'Already exists: ai_kien_thuc';
GO

-- Seed cơ sở kiến thức mặc định
IF NOT EXISTS (SELECT * FROM ai_kien_thuc)
BEGIN
    PRINT 'Seeding ai_kien_thuc...';

    INSERT INTO ai_kien_thuc (loai, tieu_de, noi_dung) VALUES
    ('FAQ', 'Chính sách đổi trả', 'Quý khách được đổi trả sản phẩm trong vòng 7 ngày kể từ ngày mua nếu sản phẩm còn nguyên vẹn, chưa qua sử dụng và còn đầy đủ phụ kiện đi kèm. Sản phẩm được bảo hành theo chính sách bảo hành của nhà sản xuất.'),
    ('FAQ', 'Phương thức thanh toán', 'Chúng tôi hỗ trợ thanh toán bằng tiền mặt, chuyển khoản ngân hàng, thẻ ATM nội địa và thẻ tín dụng quốc tế (Visa, Mastercard, JCB).'),
    ('FAQ', 'Chính sách bảo hành', 'Tất cả sản phẩm laptop được bảo hành 12-24 tháng tùy theo nhà sản xuất. Bảo hành bao gồm lỗi phần cứng từ nhà sản xuất. Không bảo hành các lỗi do va đập, vào nước hoặc tự ý sửa chữa.'),
    ('FAQ', 'Vận chuyển và giao hàng', 'Chúng tôi giao hàng toàn quốc qua các đơn vị vận chuyển uy tín. Nội thành TP.HCM và Hà Nội: 1-2 ngày. Các tỉnh khác: 2-5 ngày. Miễn phí vận chuyển cho đơn hàng từ 500.000đ.'),
    ('FAQ', 'Laptop gaming', 'Chúng tôi cung cấp đa dạng laptop gaming từ các thương hiệu ASUS ROG, Acer Predator, MSI, Lenovo Legion, Dell G Series. Máy được trang bị card đồ họa rời NVIDIA GeForce RTX 30xx/40xx series.'),
    ('FAQ', 'Laptop văn phòng', 'Dòng laptop văn phòng phù hợp cho công việc hàng ngày, học tập. Giá từ 10-20 triệu. Cấu hình đề xuất: CPU Intel Core i5/i7 thế hệ 13, RAM 8-16GB, SSD 512GB.'),
    ('FAQ', 'Cách đặt hàng', 'Quý khách có thể đặt hàng trực tiếp trên website, gọi điện hotline hoặc đến cửa hàng. Sau khi đặt hàng, nhân viên sẽ liên hệ xác nhận trong vòng 30 phút.'),
    ('FAQ', 'Khuyến mãi', 'Chúng tôi thường xuyên có các chương trình khuyến mãi hấp dẫn. Đăng ký nhận tin để cập nhật các ưu đãi mới nhất. Giảm 5-15% cho học sinh, sinh viên khi xuất trình thẻ.');

    INSERT INTO ai_kien_thuc (loai, tieu_de, noi_dung) VALUES
    ('CHINH_SACH', 'Giới thiệu cửa hàng', 'SAOClub là cửa hàng chuyên cung cấp laptop và thiết bị công nghệ chính hãng. Chúng tôi cam kết 100% sản phẩm chính hãng, giá tốt nhất thị trường và dịch vụ hậu mãi chu đáo.'),
    ('CHINH_SACH', 'Tích điểm thưởng', 'Khách hàng tích lũy 1% giá trị đơn hàng vào tài khoản. Điểm thưởng có thể đổi thành phiếu giảm giá hoặc sử dụng để thanh toán đơn hàng tiếp theo.'),
    ('CHINH_SACH', 'Liên hệ', 'Hotline: 1900.xxxx. Giờ làm việc: 8h-21h các ngày trong tuần. Email: contact@saoclub.com');

    PRINT 'Seeded: ai_kien_thuc';
END
ELSE
    PRINT 'Already seeded: ai_kien_thuc';
GO

PRINT '=== Migration 001_chat_and_ai completed ===';
