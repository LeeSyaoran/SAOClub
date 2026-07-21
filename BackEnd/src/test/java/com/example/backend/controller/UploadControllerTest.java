package com.example.backend.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Xác nhận lỗ hổng path traversal (CWE-22) trong uploadImage() đã được chặn tận gốc —
// trước đây dùng thẳng file.getOriginalFilename() làm tên đích, cho phép client ghi đè
// file bất kỳ trong upload.dir (trỏ vào mã nguồn frontend).
class UploadControllerTest {

    @TempDir
    Path tempDir;

    private UploadController newController() {
        UploadController controller = new UploadController();
        ReflectionTestUtils.setField(controller, "uploadDir", tempDir.toString());
        return controller;
    }

    @Test
    void tenFileCoPathTraversal_khongThoatDuocKhoiThuMucDich() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "../../../evil.jpg", "image/jpeg", "noi dung anh".getBytes());

        ResponseEntity<?> res = newController().uploadImage(file);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) res.getBody();
        String filename = body.get("filename");
        assertThat(filename).doesNotContain("..").doesNotContain("/").doesNotContain("\\");
        assertThat(filename).endsWith(".jpg");

        try (var stream = Files.list(tempDir)) {
            assertThat(stream.count()).isEqualTo(1);
        }
        assertThat(Files.exists(tempDir.resolve(filename))).isTrue();
    }

    @Test
    void duoiFileKhongPhaiAnh_biTuChoiVaKhongGhiFile() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "shell.jsp", "application/octet-stream", "<% malicious %>".getBytes());

        ResponseEntity<?> res = newController().uploadImage(file);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        try (var stream = Files.list(tempDir)) {
            assertThat(stream.count()).isZero();
        }
    }

    @Test
    void tenFileHopLe_luuVoiTenTuSinhKhacTenGoc() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "product.png", "image/png", "du lieu anh".getBytes());

        ResponseEntity<?> res = newController().uploadImage(file);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) res.getBody();
        assertThat(body.get("filename")).isNotEqualTo("product.png").endsWith(".png");
    }
}
