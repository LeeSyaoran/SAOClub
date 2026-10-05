package com.example.backend.controller;

import com.example.backend.service.AdminAiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin-ai")
@PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN', 'QUAN_KHO')")
public class AdminAiChatController {

    @Autowired
    private AdminAiChatService adminAiChatService;

    // Lấy tin nhắn chào mừng
    @GetMapping("/welcome")
    public ResponseEntity<Map<String, String>> getWelcome() {
        return ResponseEntity.ok(Map.of("message", adminAiChatService.getWelcomeMessage()));
    }

    // Gửi câu hỏi và nhận câu trả lời từ AI (kèm lịch sử chat để giữ ngữ cảnh)
    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody AdminAiChatRequest request) {
        if (request.getCauHoi() == null || request.getCauHoi().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Câu hỏi không được để trống"));
        }
        String reply = adminAiChatService.traLoi(request.getCauHoi(), request.getLichSuChat());
        return ResponseEntity.ok(Map.of("reply", reply));
    }

    // ─── Request DTO ────────────────────────────────────────────────────────────
    public static class AdminAiChatRequest {
        private String cauHoi;
        private List<Map<String, String>> lichSuChat;

        public String getCauHoi() { return cauHoi; }
        public void setCauHoi(String cauHoi) { this.cauHoi = cauHoi; }

        public List<Map<String, String>> getLichSuChat() { return lichSuChat; }
        public void setLichSuChat(List<Map<String, String>> lichSuChat) { this.lichSuChat = lichSuChat; }
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDenied(Exception e) {
        return ResponseEntity.status(403).body("Chỉ Admin mới có quyền truy cập chức năng này");
    }
}
