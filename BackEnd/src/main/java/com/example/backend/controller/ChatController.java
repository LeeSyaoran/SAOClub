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

    /**
     * Tạo phiên chat mới
     * POST /api/chat/tao-phien
     * Body: { sessionId?: string, khachHangId?: number, hoTen?: string }
     */
    @PostMapping("/tao-phien")
    public ResponseEntity<ChatCuocTroChuyenResponse> taoPhienChat(
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) Integer khachHangId,
            @RequestParam(required = false) String hoTen) {
        return ResponseEntity.ok(chatService.taoPhienChat(sessionId, khachHangId, hoTen));
    }

    /**
     * Lấy phiên chat theo sessionId
     * GET /api/chat/phien/{sessionId}
     */
    @GetMapping("/phien/{sessionId}")
    public ResponseEntity<ChatCuocTroChuyenResponse> layPhienChat(@PathVariable String sessionId) {
        return ResponseEntity.ok(chatService.layPhienChat(sessionId));
    }

    /**
     * Lấy tin nhắn trong cuộc trò chuyện
     * GET /api/chat/{id}/tin-nhan?page=0&size=50
     */
    @GetMapping("/{id}/tin-nhan")
    public ResponseEntity<Page<ChatTinNhanResponse>> layTinNhan(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(chatService.layTinNhan(id, PageRequest.of(page, size)));
    }

    /**
     * Gửi tin nhắn từ khách hàng
     * POST /api/chat/{id}/tin-nhan
     * Body: { noiDung: string }
     */
    @PostMapping("/{id}/tin-nhan")
    public ResponseEntity<ChatTinNhanResponse> guiTinNhan(
            @PathVariable Long id,
            @Valid @RequestBody ChatMessageRequest request) {
        return ResponseEntity.ok(chatService.guiTinNhan(id, request));
    }

    // ─── Staff endpoints ────────────────────────────────────────────────────────

    /**
     * Danh sách cuộc trò chuyện
     * GET /api/chat/danh-sach?loai=HE_THONG&page=0&size=20
     */
    @GetMapping("/danh-sach")
    @PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN', 'QUAN_KHO')")
    public ResponseEntity<Page<ChatCuocTroChuyenResponse>> layDanhSachChat(
            @RequestParam String loai,
            @RequestParam(required = false) String trangThai,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(chatService.layDanhSachChat(loai, trangThai, PageRequest.of(page, size)));
    }

    /**
     * Nhân viên nhận tiếp cuộc trò chuyện
     * POST /api/chat/{id}/nhan-tiep
     */
    @PostMapping("/{id}/nhan-tiep")
    @PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN', 'QUAN_KHO')")
    public ResponseEntity<ChatCuocTroChuyenResponse> nhanTiepChat(@PathVariable Long id) {
        return ResponseEntity.ok(chatService.nhanTiepChat(id));
    }

    /**
     * Gửi tin nhắn từ nhân viên
     * POST /api/chat/{id}/nv-tin-nhan
     * Body: { noiDung: string }
     */
    @PostMapping("/{id}/nv-tin-nhan")
    @PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN', 'QUAN_KHO')")
    public ResponseEntity<ChatTinNhanResponse> guiTinNhanTuNhanVien(
            @PathVariable Long id,
            @Valid @RequestBody ChatMessageRequest request) {
        return ResponseEntity.ok(chatService.guiTinNhanTuNhanVien(id, request));
    }

    /**
     * Quay lại AI
     * POST /api/chat/{id}/quay-lai-ai
     */
    @PostMapping("/{id}/quay-lai-ai")
    @PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN', 'QUAN_KHO')")
    public ResponseEntity<ChatCuocTroChuyenResponse> quayLaiAI(@PathVariable Long id) {
        return ResponseEntity.ok(chatService.quayLaiAI(id));
    }

    /**
     * Đóng cuộc trò chuyện
     * POST /api/chat/{id}/dong
     */
    @PostMapping("/{id}/dong")
    @PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN', 'QUAN_KHO')")
    public ResponseEntity<ChatCuocTroChuyenResponse> dongChat(@PathVariable Long id) {
        return ResponseEntity.ok(chatService.dongChat(id));
    }

    /**
     * Lịch sử chat của khách hàng
     * GET /api/chat/khach/{khId}?page=0&size=10
     */
    @GetMapping("/khach/{khId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN', 'QUAN_KHO')")
    public ResponseEntity<Page<ChatCuocTroChuyenResponse>> layLichSuChatKhach(
            @PathVariable Integer khId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(chatService.layLichSuChatKhach(khId, PageRequest.of(page, size)));
    }

    /**
     * Thông báo cho dashboard
     * GET /api/chat/thong-bao
     */
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
