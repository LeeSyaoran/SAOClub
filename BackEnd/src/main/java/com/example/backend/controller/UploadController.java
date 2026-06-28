package com.example.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Value("${upload.dir}")
    private String uploadDir;

    @PostMapping("/image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File rong"));
        }
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String filename = file.getOriginalFilename();
            if (filename == null || filename.isBlank()) {
                filename = "upload_" + System.currentTimeMillis();
            }

            Path dest = uploadPath.resolve(filename);
            Files.write(dest, file.getBytes());

            return ResponseEntity.ok(Map.of("url", "/images/" + filename, "filename", filename));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
