package com.example.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Công cụ migration thủ công — CHỈ dùng khi phát triển.
 * Bean chỉ được tạo khi chạy profile "dev" và yêu cầu quyền ADMIN.
 * Các cột Firebase đã có trong Database/QLBanMayTinh.sql nên bình thường không cần gọi.
 */
@Profile("dev")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/dev")
public class DevController {

    @Autowired
    private DataSource dataSource;

    // Migration cột Firebase Auth cho bảng tài khoản
    @PostMapping("/migrate-firebase-columns")
    public ResponseEntity<?> migrateFirebaseColumns() {
        String[] sqls = {
            "IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('tai_khoan') AND name = 'provider') " +
            "ALTER TABLE tai_khoan ADD provider VARCHAR(20) NULL",

            "IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('tai_khoan') AND name = 'provider_uid') " +
            "ALTER TABLE tai_khoan ADD provider_uid VARCHAR(255) NULL",

            "IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('tai_khoan') AND name = 'avatar_url') " +
            "ALTER TABLE tai_khoan ADD avatar_url VARCHAR(500) NULL"
        };

        StringBuilder results = new StringBuilder();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            for (String sql : sqls) {
                try {
                    stmt.execute(sql);
                    results.append("✅ ").append(sql.substring(sql.indexOf("ADD") + 4)).append("\n");
                } catch (Exception e) {
                    results.append("⚠️ ").append(e.getMessage()).append("\n");
                }
            }
            return ResponseEntity.ok(results.toString());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi: " + e.getMessage());
        }
    }
}

