package com.example.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

        URI uri;
        try {
            uri = URI.create(imageUrl.trim());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "URL khong hop le"));
        }

        // Chi cho phep http/https (chan file://, jar://, ...) va host thuoc danh sach cho phep
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Chi chap nhan URL http/https"));
        }
        if (!hostDuocPhep(uri.getHost())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Chi chap nhan tu imgur.com, flickr.com, bb.com.vn"));
        }

        String duoi = layDuoiFile(uri.getPath());
        if (!DUOI_ANH_HOP_LE.contains(duoi)) {
            return ResponseEntity.badRequest().body(Map.of("error", "URL khong tro toi anh hop le"));
        }

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setInstanceFollowRedirects(false); // redirect co the tro toi host noi bo
            conn.setConnectTimeout(5_000);
            conn.setReadTimeout(10_000);
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return ResponseEntity.badRequest().body(Map.of("error", "Khong tai duoc anh (HTTP " + conn.getResponseCode() + ")"));
            }
            String contentType = conn.getContentType();
            if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "URL khong tro toi anh hop le"));
            }
            long len = conn.getContentLengthLong();
            if (len > MAX_URL_IMAGE_BYTES) {
                return ResponseEntity.badRequest().body(Map.of("error", "Anh vuot qua 10MB"));
            }

            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            String filename = UUID.randomUUID() + "." + duoi;
            Path dest = uploadPath.resolve(filename);

            try (InputStream is = conn.getInputStream()) {
                byte[] data = is.readNBytes((int) MAX_URL_IMAGE_BYTES + 1);
                if (data.length > MAX_URL_IMAGE_BYTES) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Anh vuot qua 10MB"));
                }
                Files.write(dest, data);
            }

            return ResponseEntity.ok(Map.of("url", "/images/" + filename, "filename", filename));
        } catch (IllegalArgumentException | ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "URL khong hop le"));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Loi tai anh: " + e.getMessage()));
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static final long MAX_URL_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> HOST_DUOC_PHEP = Set.of("imgur.com", "flic.kr", "flickr.com", "staticflickr.com", "bb.com.vn");

    // Host phai trung khop hoac la subdomain cua domain cho phep (vd: i.imgur.com)
    private static boolean hostDuocPhep(String host) {
        if (host == null || host.isBlank()) return false;
        String h = host.toLowerCase(Locale.ROOT);
        for (String d : HOST_DUOC_PHEP) {
            if (h.equals(d) || h.endsWith("." + d)) return true;
        }
        return false;
    }
}
