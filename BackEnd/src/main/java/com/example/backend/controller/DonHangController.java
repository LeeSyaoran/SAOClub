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
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
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

    @GetMapping(value = "events", produces = "text/event-stream")
    public SseEmitter subscribe() {
        return sseService.subscribe();
    }
}
