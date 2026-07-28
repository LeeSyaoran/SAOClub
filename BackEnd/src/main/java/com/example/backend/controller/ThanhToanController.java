package com.example.backend.controller;

import com.example.backend.entity.ThanhToan;
import com.example.backend.request.ThanhToanRequest;
import com.example.backend.response.ThanhToanResponse;
import com.example.backend.service.ThanhToanService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Thanh toán — chỉ staff. Dùng ở POS (tạo record khi chốt đơn tại quầy) và modal
// "Chi tiết đơn hàng" (hiển thị lại phương thức đã dùng).
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
@RestController
@RequestMapping("/api/thanh-toan")
public class ThanhToanController {

    @Autowired
    private ThanhToanService thanhToanService;

    @GetMapping
    public List<ThanhToanResponse> getAll() {
        return thanhToanService.hienThiThanhToan();
    }

    @GetMapping("/don-hang/{donHangId}")
    public List<ThanhToanResponse> getByDonHang(@PathVariable Integer donHangId) {
        return thanhToanService.hienThiThanhToanTheoDonHang(donHangId);
    }

    @GetMapping("/{id}")
    public ThanhToan getById(@PathVariable Integer id) {
        return thanhToanService.getById(id);
    }

    // POST — service xử lý FK donHang
    @PostMapping
    public ResponseEntity<ThanhToan> create(@Valid @RequestBody ThanhToanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(thanhToanService.create(request));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody ThanhToanRequest request) {
        thanhToanService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        thanhToanService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
