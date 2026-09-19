package com.example.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

        String tenGoc = file.getOriginalFilename();
        String duoi = layDuoiFile(tenGoc);
        if (!DUOI_ANH_HOP_LE.contains(duoi)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Chỉ chấp nhận file ảnh (jpg, jpeg, png, gif, webp)"));
        }

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

    @PostMapping("/image-by-url")
    public ResponseEntity<?> uploadImageByUrl(@RequestBody Map<String, String> body) {
        String imageUrl = body.get("url");
        if (imageUrl == null || imageUrl.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "URL rong"));
        }

        // Chi cho phep tu cac domain hinh anh pho bien
        String[] duocPhep = { "imgur.com", "i.imgur.com", "flic.kr", "flickr.com", "bb.com.vn" };
        boolean choPhep = false;
        for (String d : duocPhep) {
            if (imageUrl.contains(d)) { choPhep = true; break; }
        }
        if (!choPhep) {
            return ResponseEntity.badRequest().body(Map.of("error", "Chi chap nhan tu imgur.com, flickr.com, bb.com.vn"));
        }

        try {
            URI uri = URI.create(imageUrl);
            URL url = uri.toURL();
            String duoi = layDuoiTuUrl(imageUrl);
            if (!DUOI_ANH_HOP_LE.contains(duoi)) {
                return ResponseEntity.badRequest().body(Map.of("error", "URL khong tro toi anh hop le"));
            }

            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            String filename = UUID.randomUUID() + "." + duoi;
            Path dest = uploadPath.resolve(filename);

            try (InputStream is = url.openStream()) {
                Files.copy(is, dest, StandardCopyOption.REPLACE_EXISTING);
            }

            return ResponseEntity.ok(Map.of("url", "/images/" + filename, "filename", filename));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "URL khong hop le"));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Loi tai anh: " + e.getMessage()));
        }
    }

    private static String layDuoiTuUrl(String url) {
        try {
            String path = URI.create(url).getPath();
            return layDuoiFile(path);
        } catch (Exception e) {
            return "";
        }
    }
}
