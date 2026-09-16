package com.example.backend.controller;

import com.example.backend.entity.DonHang;
import com.example.backend.request.ChiTietDonHangRequest;
import com.example.backend.request.DonHangRequest;
import com.example.backend.request.MergeOrderRequest;
import com.example.backend.request.XacNhanDonHangRequest;
import com.example.backend.response.DonHangResponse;
import com.example.backend.service.DonHangService;
import com.example.backend.service.SseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/don-hang")
public class DonHangController {

    @Autowired
    private DonHangService donHangService;
    @Autowired
    private SseService sseService;

    @GetMapping
    public Page<DonHangResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer khachHangId) {
        return donHangService.hienThiDonHang(khachHangId, PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public DonHang getById(@PathVariable Integer id) {
        return donHangService.getByIdChoNguoiXem(id);
    }

    @PostMapping
    public ResponseEntity<DonHang> create(@Valid @RequestBody DonHangRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(donHangService.create(request));
    }

    /**
     * Checkout online: tạo đơn + thêm tất cả sản phẩm trong 1 transaction.
     * Dùng cho khách vãng lai không đăng nhập — không cần quyền staff.
     */
    @PostMapping("/checkout-complete")
    public ResponseEntity<?> checkoutComplete(@RequestBody Map<String, Object> body) {
        try {
            DonHangRequest orderReq = convertToDonHangRequest(body);
            List<ChiTietDonHangRequest> items = parseChiTietItems(body.get("items"));
            DonHang order = donHangService.checkoutComplete(orderReq, items);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (IllegalArgumentException | org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi khi tạo đơn hàng: " + e.getMessage()));
        }
    }

    private DonHangRequest convertToDonHangRequest(Map<String, Object> body) {
        DonHangRequest req = new DonHangRequest();
        req.setKhachHangId(getInt(body.get("khachHangId")));
        req.setNguoiNhan((String) body.get("nguoiNhan"));
        req.setSdtNguoiNhan((String) body.get("sdtNguoiNhan"));
        req.setDiaChiGiaoHangText((String) body.get("diaChiGiaoHangText"));
        req.setKhuyenMaiId(getInt(body.get("khuyenMaiId")));
        req.setPhieuGiamGiaCaNhanId(getInt(body.get("phieuGiamGiaCaNhanId")));
        req.setTongTien(getDecimal(body.get("tongTien")));
        req.setGiamGia(getDecimal(body.get("giamGia")));
        req.setPhiVanChuyen(getDecimal(body.get("phiVanChuyen")));
        req.setNgayDat(body.get("ngayDat") != null
                ? LocalDateTime.parse((String) body.get("ngayDat")) : LocalDateTime.now());
        req.setTrangThaiDonHang((String) body.getOrDefault("trangThaiDonHang", "pending"));
        req.setTrangThaiThanhToan((String) body.getOrDefault("trangThaiThanhToan", "unpaid"));
        req.setKenhBan("online");
        req.setGhiChu((String) body.get("ghiChu"));
        return req;
    }

    @SuppressWarnings("unchecked")
    private List<ChiTietDonHangRequest> parseChiTietItems(Object itemsObj) {
        if (itemsObj == null) return List.of();
        List<Map<String, Object>> items = (List<Map<String, Object>>) itemsObj;
        return items.stream().map(item -> {
            ChiTietDonHangRequest req = new ChiTietDonHangRequest();
            req.setBienTheId(getInt(item.get("bienTheId")));
            req.setSoLuong(getInt(item.get("soLuong"), 1));
            return req;
        }).toList();
    }

    private Integer getInt(Object val) { return getInt(val, null); }
    private Integer getInt(Object val, Integer def) {
        if (val == null) return def;
        return ((Number) val).intValue();
    }

    private BigDecimal getDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof Number) return BigDecimal.valueOf(((Number) val).doubleValue());
        return new BigDecimal(val.toString());
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody DonHangRequest request) {
        donHangService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        donHangService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PostMapping("merge")
    public ResponseEntity<?> merge(@RequestBody MergeOrderRequest request) {
        try {
            donHangService.mergeOrders(request.getTargetId(), request.getSourceIds());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PatchMapping("{id}/recalculate")
    public ResponseEntity<Void> recalculate(@PathVariable Integer id) {
        donHangService.recalculateTongTien(id);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PatchMapping("{id}/xac-nhan")
    public ResponseEntity<Void> xacNhan(@PathVariable Integer id, @Valid @RequestBody XacNhanDonHangRequest request) {
        donHangService.xacNhanDonHang(id, request);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("{id}/xac-nhan-nhan-hang")
    public ResponseEntity<Void> xacNhanDaNhanHang(@PathVariable Integer id) {
        donHangService.xacNhanDaNhanHang(id);
        return ResponseEntity.ok().build();
    }

    // POST /api/don-hang/{id}/giao-hang — giao hàng tại quầy (POS), kích hoạt bảo hành
    @PostMapping("{id}/giao-hang")
    public ResponseEntity<DonHang> giaoHang(
            @PathVariable Integer id,
            @RequestBody(required = false) Map<String, String> body) {
        String ngayStr = body != null ? body.get("ngayGiaoThucTe") : null;
        LocalDateTime ngayGiao = ngayStr != null ? LocalDateTime.parse(ngayStr) : null;
        DonHang saved = donHangService.giaoHang(id, ngayGiao);
        return ResponseEntity.ok(saved);
    }

    @GetMapping(value = "events", produces = "text/event-stream")
    public SseEmitter subscribe() {
        return sseService.subscribe();
    }

    // POST /api/don-hang/tinh-phi-van-chuyen — tính phí vận chuyển theo địa chỉ
    @PostMapping("tinh-phi-van-chuyen")
    public ResponseEntity<?> tinhPhiVanChuyen(@RequestBody Map<String, Object> body) {
        try {
            double tongTienHang = 0;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
            if (items != null) {
                for (Map<String, Object> item : items) {
                    Object giaBan = item.get("giaBan");
                    Object soLuong = item.get("soLuong");
                    double g = giaBan instanceof Number ? ((Number) giaBan).doubleValue() : 0;
                    double qty = soLuong instanceof Number ? ((Number) soLuong).doubleValue() : 1;
                    tongTienHang += g * qty;
                }
            }
            double phiVanChuyen = tongTienHang >= 300_000 ? 0 : 30_000;
            return ResponseEntity.ok(Map.of("phiVanChuyen", phiVanChuyen, "mienPhiTu", 300_000));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("phiVanChuyen", 30_000, "mienPhiTu", 300_000));
        }
    }

    // ── POS endpoints ─────────────────────────────────────────────────────────

    /** Don hang gan nhat trong 30 ngay — cho POS recent orders panel */
    @GetMapping("/pos/recent")
    public List<DonHangResponse> getRecentForPos() {
        return donHangService.getRecentForPos();
    }

    /** Top khach hang theo chi tieu 6 thang — cho POS quick-select */
    @GetMapping("/pos/top-customers")
    public List<?> getTopCustomers(@RequestParam(defaultValue = "5") int limit) {
        return donHangService.getTopCustomers(limit);
    }

    /** Don hang gan nhat cua 1 khach hang — khi POS chon khach */
    @GetMapping("/pos/customer/{khachHangId}/orders")
    public List<DonHangResponse> getRecentByKhachHang(@PathVariable Integer khachHangId) {
        return donHangService.getRecentByKhachHang(khachHangId);
    }
}
