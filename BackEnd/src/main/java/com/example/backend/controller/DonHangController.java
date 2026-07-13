package com.example.backend.controller;

import com.example.backend.entity.DonHang;
import com.example.backend.request.DonHangRequest;
import com.example.backend.request.XacNhanDonHangRequest;
import com.example.backend.request.MergeOrderRequest;
import com.example.backend.response.DonHangResponse;
import com.example.backend.service.DonHangService;
import com.example.backend.service.SseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/don-hang")
public class DonHangController {

    @Autowired
    private DonHangService donHangService;
    @Autowired
    private SseService sseService;

    // GET /api/don-hang?page=0&size=20&khachHangId=... — DTO query với LEFT JOIN nullable
    // FKs (nhanVien, khuyenMai, diaChiGiaoHang có thể null), có phân trang.
    // khachHangId optional: trang "Đơn hàng của tôi" truyền vào để chỉ lấy đơn của khách
    // đó thay vì tải hết đơn toàn hệ thống rồi lọc ở trình duyệt (rất chậm khi số đơn lớn).
    @GetMapping
    public Page<DonHangResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer khachHangId) {
        return donHangService.hienThiDonHang(khachHangId, PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public DonHang getById(@PathVariable Integer id) {
        return donHangService.getById(id);
    }

    // POST — service xử lý các FK: khachHang, nhanVien, khuyenMai, diaChiGiaoHang
    @PostMapping
    public ResponseEntity<DonHang> create(@Valid @RequestBody DonHangRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(donHangService.create(request));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody DonHangRequest request) {
        donHangService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        donHangService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Gộp nhiều đơn hàng của cùng khách vào 1 đơn đích
    @PostMapping("merge")
    public ResponseEntity<Void> merge(@RequestBody MergeOrderRequest request) {
        donHangService.mergeOrders(request.getTargetId(), request.getSourceIds());
        return ResponseEntity.ok().build();
    }

    // Tính lại tong_tien sau khi thêm/xóa sản phẩm trong đơn
    @PatchMapping("{id}/recalculate")
    public ResponseEntity<Void> recalculate(@PathVariable Integer id) {
        donHangService.recalculateTongTien(id);
        return ResponseEntity.ok().build();
    }

    // Chọn serial cho từng dòng + chốt bán + chuyển sang "confirmed" (xác nhận) — chỉ
    // đơn online (đơn tại quầy đã chốt serial ngay lúc tạo, không qua bước này).
    @PatchMapping("{id}/xac-nhan")
    public ResponseEntity<Void> xacNhan(@PathVariable Integer id, @Valid @RequestBody XacNhanDonHangRequest request) {
        donHangService.xacNhanDonHang(id, request);
        return ResponseEntity.ok().build();
    }

    // SSE — admin subscribe để nhận thông báo đơn hàng mới theo thời gian thực
    @GetMapping(value = "events", produces = "text/event-stream")
    public SseEmitter subscribe() {
        return sseService.subscribe();
    }
}
