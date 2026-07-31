package com.example.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Chỉ admin/nhân viên/quản kho dùng (ProductsTable.vue) — chỉ khách hàng chưa từng gọi tới.
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final Set<String> DUOI_ANH_HOP_LE = Set.of("jpg", "jpeg", "png", "gif", "webp");

    @Value("${upload.dir}")
    private String uploadDir;

    @PostMapping("/image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File rong"));
        }

        // Chỉ nhận đúng đuôi file ảnh — không tin content-type client tự khai.
        String tenGoc = file.getOriginalFilename();
        String duoi = layDuoiFile(tenGoc);
        if (!DUOI_ANH_HOP_LE.contains(duoi)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Chỉ chấp nhận file ảnh (jpg, jpeg, png, gif, webp)"));
        }

        // Xác thực nội dung file bằng magic bytes — chặn file giả đuôi (.exe đổi thành .png)
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[8];
            int read = is.read(header);
            if (read < 4) {
                return ResponseEntity.badRequest().body(Map.of("error", "File không hợp lệ"));
            }
            boolean validMagic = switch (duoi) {
                case "jpg", "jpeg" -> header[0] == (byte) 0xFF && header[1] == (byte) 0xD8;
                case "png" -> header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47;
                case "gif" -> header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46;
                case "webp" -> header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46;
                default -> false;
            };
            if (!validMagic) {
                return ResponseEntity.badRequest().body(Map.of("error", "File không phải là ảnh hợp lệ"));
            }
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi đọc file"));
        }

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            // Luôn tự sinh tên file bằng UUID — KHÔNG bao giờ dùng tên file gốc từ client cho
            // đường dẫn ghi, kể cả sau khi "làm sạch". Trước đây dùng thẳng
            // file.getOriginalFilename() làm tên đích — client gửi tên dạng "../../..." sẽ ghi
            // đè được file bất kỳ trong cây thư mục (upload.dir trỏ thẳng vào
            // FrontEnd/QLBanMayTinh/public/images, tức là mã nguồn frontend).
            String filename = UUID.randomUUID() + "." + duoi;
            Path dest = uploadPath.resolve(filename);
            Files.write(dest, file.getBytes());

            return ResponseEntity.ok(Map.of("url", "/images/" + filename, "filename", filename));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private static String layDuoiFile(String tenFile) {
        if (tenFile == null) return "";
        int cham = tenFile.lastIndexOf('.');
        if (cham < 0 || cham == tenFile.length() - 1) return "";
        return tenFile.substring(cham + 1).toLowerCase(Locale.ROOT);
    }
}
