package com.example.backend.controller;

import com.example.backend.entity.DonHang;
import com.example.backend.entity.ThanhToan;
import com.example.backend.repository.DonHangRepository;
import com.example.backend.repository.ThanhToanRepository;
import com.example.backend.service.SseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * PaymentWebhookController — Xử lý xác nhận thanh toán.
 *
 * Endpoint nội bộ: Nhân viên xác nhận thủ công khi thấy tiền về tài khoản.
 * Cấu trúc để sau này thay bằng Webhook thật từ VNPay / Timo Open Banking.
 *
 * POST /api/payment/confirm      — Nhân viên xác nhận đã nhận tiền (ADMIN/NHAN_VIEN)
 * POST /api/payment/vnpay/ipn    — Placeholder cho VNPay IPN server-to-server (public)
 */
@RestController
@RequestMapping("/api/payment")
public class PaymentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

    @Autowired private DonHangRepository donHangRepository;
    @Autowired private ThanhToanRepository thanhToanRepository;
    @Autowired private SseService sseService;

    // ──────────────────────────────────────────────────────────────────────────
    // Endpoint 1: Nhân viên xác nhận đã nhận tiền thủ công
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Xác nhận thanh toán thủ công — dành cho nhân viên khi thấy tiền về tài khoản Timo.
     * Body: { donHangId, soTien, maGiaoDich, phuongThuc }
     */
    @PostMapping("/confirm")
    @Transactional
    public ResponseEntity<?> confirmPayment(@RequestBody Map<String, Object> body) {
        try {
            Integer donHangId = ((Number) body.get("donHangId")).intValue();
            DonHang donHang = donHangRepository.findById(donHangId)
                    .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại: " + donHangId));

            if ("paid".equals(donHang.getTrangThaiThanhToan())) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Đơn hàng đã được đánh dấu thanh toán trước đó",
                        "idempotent", true
                ));
            }

            // Cập nhật trạng thái thanh toán & trạng thái đơn hàng
            donHang.setTrangThaiThanhToan("paid");
            String phuongThuc = donHang.getPhuongThucThanhToan();
            boolean isQr = phuongThuc != null && (
                    phuongThuc.equalsIgnoreCase("qr") ||
                    phuongThuc.equalsIgnoreCase("bank_transfer") ||
                    phuongThuc.equalsIgnoreCase("chuyen_khoan") ||
                    phuongThuc.equalsIgnoreCase("vietqr")
            );
            if (body.containsKey("trangThaiDonHang") && body.get("trangThaiDonHang") != null) {
                donHang.setTrangThaiDonHang((String) body.get("trangThaiDonHang"));
            } else if (!isQr && "pending".equalsIgnoreCase(donHang.getTrangThaiDonHang())) {
                donHang.setTrangThaiDonHang("confirmed");
            }
            // Đối với đơn hàng QR, sau khi thanh toán sẽ chuyển vào bước 3 "Chờ xử lý" (trangThaiDonHang = pending, trangThaiThanhToan = paid)
            donHangRepository.save(donHang);

            // Tạo bản ghi thanh toán
            ThanhToan tt = new ThanhToan();
            tt.setDonHang(donHang);
            tt.setNgayThanhToan(LocalDateTime.now());
            tt.setPhuongThucThanhToan(getStr(body, "phuongThuc", donHang.getPhuongThucThanhToan()));
            tt.setSoTien(getDecimal(body.get("soTien"), donHang.getThanhTien()));
            tt.setMaGiaoDich(getStr(body, "maGiaoDich", null));
            tt.setTrangThai("paid");
            tt.setGhiChu("[Manual] Nhân viên xác nhận đã nhận tiền");
            thanhToanRepository.save(tt);

            // Push SSE để admin panel refresh
            sseService.notifyOrderUpdate(donHangId);

            log.info("[Payment] Đơn #{} ({}) đã được xác nhận thanh toán thủ công — phương thức: {}, mã GD: {}",
                    donHang.getId(), donHang.getMaDonHang(),
                    tt.getPhuongThucThanhToan(), tt.getMaGiaoDich());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "donHangId", donHangId,
                    "maDonHang", donHang.getMaDonHang() != null ? donHang.getMaDonHang() : "",
                    "message", "Đã xác nhận thanh toán thành công"
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            log.error("[Payment] Lỗi khi xác nhận thanh toán: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Endpoint 2: Placeholder VNPay IPN (server-to-server từ cổng thanh toán)
    // Sau này thay bằng logic verify HMAC-SHA512 thật
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * VNPay IPN endpoint — public (không cần auth, VNPay gọi trực tiếp).
     * Hiện tại chỉ log, chưa xử lý (cần API key VNPay để verify chữ ký).
     */
    @PostMapping("/vnpay/ipn")
    public ResponseEntity<?> vnpayIpn(@RequestParam Map<String, String> params) {
        log.info("[VNPay IPN] Nhận callback: txnRef={}, responseCode={}, amount={}",
                params.get("vnp_TxnRef"),
                params.get("vnp_ResponseCode"),
                params.get("vnp_Amount"));

        // TODO: Khi có API key VNPay, implement:
        // 1. Verify HMAC-SHA512 checksum
        // 2. Tìm đơn hàng theo vnp_TxnRef (= maDonHang)
        // 3. Cập nhật trangThaiThanhToan = 'paid' nếu vnp_ResponseCode = '00'
        // 4. Trả về {"RspCode":"00","Message":"Confirm Success"}

        return ResponseEntity.ok(Map.of("RspCode", "99", "Message", "Not yet implemented"));
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private String getStr(Map<String, Object> body, String key, String def) {
        Object v = body.get(key);
        return v instanceof String s && !s.isBlank() ? s : def;
    }

    private BigDecimal getDecimal(Object val, BigDecimal def) {
        if (val == null) return def;
        try { return new BigDecimal(val.toString()); } catch (Exception e) { return def; }
    }
}
