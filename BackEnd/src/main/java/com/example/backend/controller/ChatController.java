package com.example.backend.controller;

import com.example.backend.entity.CuocTroChuyen;
import com.example.backend.request.ChatMessageRequest;
import com.example.backend.response.ChatCuocTroChuyenResponse;
import com.example.backend.response.ChatTinNhanResponse;
import com.example.backend.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    // ─── Public endpoints (khách hàng) ─────────────────────────────────────────

    // Tạo phiên chat mới
    @PostMapping("/tao-phien")
    public ResponseEntity<ChatCuocTroChuyenResponse> taoPhienChat(
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) Integer khachHangId,
            @RequestParam(required = false) String hoTen) {
        return ResponseEntity.ok(chatService.taoPhienChat(sessionId, khachHangId, hoTen));
    }

    // Lấy phiên chat theo session ID
    @GetMapping("/phien/{sessionId}")
    public ResponseEntity<ChatCuocTroChuyenResponse> layPhienChat(@PathVariable String sessionId) {
        return ResponseEntity.ok(chatService.layPhienChat(sessionId));
    }

    // Lấy danh sách tin nhắn của cuộc trò chuyện
    @GetMapping("/{id}/tin-nhan")
    public ResponseEntity<Page<ChatTinNhanResponse>> layTinNhan(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(chatService.layTinNhan(id, PageRequest.of(page, size)));
    }

    // Gửi tin nhắn từ khách hàng
    @PostMapping("/{id}/tin-nhan")
    public ResponseEntity<ChatTinNhanResponse> guiTinNhan(
            @PathVariable Long id,
            @Valid @RequestBody ChatMessageRequest request) {
        return ResponseEntity.ok(chatService.guiTinNhan(id, request));
    }

    // Lấy danh sách cuộc trò chuyện cho nhân viên
    @GetMapping("/danh-sach")
    @PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN', 'QUAN_KHO')")
    public ResponseEntity<Page<ChatCuocTroChuyenResponse>> layDanhSachChat(
            @RequestParam String loai,
            @RequestParam(required = false) String trangThai,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(chatService.layDanhSachChat(loai, trangThai, PageRequest.of(page, size)));
    }

    // Nhân viên tiếp nhận cuộc trò chuyện
    @PostMapping("/{id}/nhan-tiep")
    @PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN', 'QUAN_KHO')")
    public ResponseEntity<ChatCuocTroChuyenResponse> nhanTiepChat(@PathVariable Long id) {
        return ResponseEntity.ok(chatService.nhanTiepChat(id));
    }

    // Gửi tin nhắn từ nhân viên
    @PostMapping("/{id}/nv-tin-nhan")
    @PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN', 'QUAN_KHO')")
    public ResponseEntity<ChatTinNhanResponse> guiTinNhanTuNhanVien(
            @PathVariable Long id,
            @Valid @RequestBody ChatMessageRequest request) {
        return ResponseEntity.ok(chatService.guiTinNhanTuNhanVien(id, request));
    }

    // Chuyển cuộc trò chuyện lại cho AI
    @PostMapping("/{id}/quay-lai-ai")
    @PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN', 'QUAN_KHO')")
    public ResponseEntity<ChatCuocTroChuyenResponse> quayLaiAI(@PathVariable Long id) {
        return ResponseEntity.ok(chatService.quayLaiAI(id));
    }

    // Đóng cuộc trò chuyện
    @PostMapping("/{id}/dong")
    @PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN', 'QUAN_KHO')")
    public ResponseEntity<ChatCuocTroChuyenResponse> dongChat(@PathVariable Long id) {
        return ResponseEntity.ok(chatService.dongChat(id));
    }

    // Lấy lịch sử chat của khách hàng
    @GetMapping("/khach/{khId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN', 'QUAN_KHO')")
    public ResponseEntity<Page<ChatCuocTroChuyenResponse>> layLichSuChatKhach(
            @PathVariable Integer khId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(chatService.layLichSuChatKhach(khId, PageRequest.of(page, size)));
    }

    // Đếm số thông báo tin nhắn mới
    @GetMapping("/thong-bao")
    @PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN', 'QUAN_KHO')")
    public ResponseEntity<Map<String, Long>> demThongBao() {
        return ResponseEntity.ok(chatService.demThongBao());
    }

    // ─── Error handling ────────────────────────────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDenied(Exception e) {
        return ResponseEntity.status(403).body("Không có quyền thực hiện thao tác này");
    }
}
