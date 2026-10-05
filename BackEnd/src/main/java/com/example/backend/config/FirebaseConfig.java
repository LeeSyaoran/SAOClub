package com.example.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

/**
 * Khởi tạo Firebase Admin SDK.
 *
 * Đường dẫn service account lấy từ biến môi trường FIREBASE_SERVICE_ACCOUNT, hỗ trợ:
 *   - file:/duong/dan/ngoai/firebase-service-account.json   (khuyến nghị — để private key NGOÀI source code)
 *   - /duong/dan/tuyet/doi.json  hoặc  C:/secrets/firebase.json
 *   - classpath:firebase-service-account.json               (mặc định, tương thích cũ)
 */
@Configuration
public class FirebaseConfig {

    @Value("${firebase.service-account:${FIREBASE_SERVICE_ACCOUNT:classpath:firebase-service-account.json}}")
    private String serviceAccountPath;

    private final ResourceLoader resourceLoader;

    public FirebaseConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void initFirebase() {
        if (!FirebaseApp.getApps().isEmpty()) return;

        String location = serviceAccountPath.trim();
        if (!location.startsWith("classpath:") && !location.startsWith("file:")) {
            location = "file:" + location; // đường dẫn hệ thống tệp thuần
        }

        Resource resource = resourceLoader.getResource(location);
        try (InputStream serviceAccount = resource.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            FirebaseApp.initializeApp(options);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize Firebase Admin SDK from '" + location + "'. "
                    + "Set FIREBASE_SERVICE_ACCOUNT to the service account JSON path.", e);
        }
    }
}

