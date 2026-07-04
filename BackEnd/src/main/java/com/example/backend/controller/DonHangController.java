package com.example.backend.controller;

import com.example.backend.entity.DonHang;
import com.example.backend.request.DonHangRequest;
import com.example.backend.request.MergeOrderRequest;
import com.example.backend.response.DonHangResponse;
import com.example.backend.service.DonHangService;
import com.example.backend.service.SseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/don-hang")
public class DonHangController {

    @Autowired
    private DonHangService donHangService;
    @Autowired
    private SseService sseService;

    // GET /api/don-hang — DTO query với LEFT JOIN nullable FKs
    // (nhanVien, khuyenMai, diaChiGiaoHang có thể null)
    @GetMapping
    public List<DonHangResponse> getAll() {
        return donHangService.hienThiDonHang();
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

    // SSE — admin subscribe để nhận thông báo đơn hàng mới theo thời gian thực
    @GetMapping(value = "events", produces = "text/event-stream")
    public SseEmitter subscribe() {
        return sseService.subscribe();
    }
}
