package com.example.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        ensureSchema();
    }

    /**
     * Đảm bảo các bảng cần thiết luôn tồn tại kể cả khi reset DB theo file SQL
     */
    public void ensureSchema() {
        try {
            String sql = """
                IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'khuyen_mai_khach_hang')
                BEGIN
                    CREATE TABLE khuyen_mai_khach_hang (
                        khuyen_mai_khach_hang_id INT IDENTITY(1,1) PRIMARY KEY,
                        khuyen_mai_id            INT NOT NULL,
                        khach_hang_id            INT NOT NULL,
                        CONSTRAINT FK_kmkh_khuyen_mai FOREIGN KEY (khuyen_mai_id) REFERENCES khuyen_mai(khuyen_mai_id) ON DELETE CASCADE,
                        CONSTRAINT FK_kmkh_khach_hang FOREIGN KEY (khach_hang_id) REFERENCES khach_hang(khach_hang_id) ON DELETE CASCADE,
                        CONSTRAINT UQ_kmkh_km_kh      UNIQUE (khuyen_mai_id, khach_hang_id)
                    );
                END
                IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_kmkh_km_kh')
                BEGIN
                    CREATE INDEX IX_kmkh_km_kh ON khuyen_mai_khach_hang(khuyen_mai_id, khach_hang_id);
                END
            """;
            jdbcTemplate.execute(sql);
            log.info("DatabaseInitializer: Đã kiểm tra và đảm bảo schema bảng khuyen_mai_khach_hang.");

            // Đảm bảo các cột yêu cầu hủy đơn trong bảng don_hang
            String sqlDonHang = """
                SET QUOTED_IDENTIFIER ON;
                SET ANSI_NULLS ON;
                IF COL_LENGTH('don_hang', 'yeu_cau_huy') IS NULL
                BEGIN
                    ALTER TABLE don_hang ADD yeu_cau_huy BIT NOT NULL DEFAULT 0;
                END
                IF COL_LENGTH('don_hang', 'ly_do_huy') IS NULL
                BEGIN
                    ALTER TABLE don_hang ADD ly_do_huy NVARCHAR(500) NULL;
                END
                IF COL_LENGTH('don_hang', 'ngay_yeu_cau_huy') IS NULL
                BEGIN
                    ALTER TABLE don_hang ADD ngay_yeu_cau_huy DATETIME NULL;
                END
            """;
            jdbcTemplate.execute(sqlDonHang);
            log.info("DatabaseInitializer: Đã kiểm tra và đảm bảo schema bảng don_hang (yeu_cau_huy, ly_do_huy, ngay_yeu_cau_huy).");

            // Đảm bảo constraint trạng thái của phieu_bao_hanh hỗ trợ cho_xu_ly và huy
            String sqlPbh = """
                SET QUOTED_IDENTIFIER ON;
                SET ANSI_NULLS ON;
                IF EXISTS (
                    SELECT 1 FROM sys.check_constraints
                    WHERE name = 'CK_pbh_trangthai'
                      AND definition NOT LIKE '%cho_xu_ly%'
                )
                BEGIN
                    ALTER TABLE phieu_bao_hanh DROP CONSTRAINT CK_pbh_trangthai;
                    ALTER TABLE phieu_bao_hanh ADD CONSTRAINT CK_pbh_trangthai CHECK (trang_thai IN (N'cho_xu_ly', N'con_bao_hanh', N'dang_xu_ly', N'da_xu_ly', N'het_bao_hanh', N'tu_choi', N'huy'));
                END
            """;
            jdbcTemplate.execute(sqlPbh);
            log.info("DatabaseInitializer: Đã kiểm tra và đảm bảo constraint CK_pbh_trangthai.");
        } catch (Exception e) {
            log.warn("DatabaseInitializer: Lỗi khi kiểm tra schema (có thể DB chưa sẵn sàng): {}", e.getMessage());
        }
    }
}
