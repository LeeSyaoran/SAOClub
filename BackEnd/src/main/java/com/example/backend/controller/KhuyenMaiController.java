package com.example.backend.controller;

import com.example.backend.entity.KhuyenMai;
import com.example.backend.request.KhuyenMaiRequest;
import com.example.backend.response.KhuyenMaiResponse;
import com.example.backend.service.KhuyenMaiService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/khuyen-mai")
public class KhuyenMaiController {

    @Autowired
    private KhuyenMaiService khuyenMaiService;

    // Giữ mở — CheckoutModal.vue tải danh sách mã khuyến mãi cho cả khách vãng lai chưa
    // đăng nhập lẫn khách đã đăng nhập.
    @GetMapping
    public List<KhuyenMaiResponse> getAll() {
        return khuyenMaiService.hienThiKhuyenMai();
    }

    @GetMapping("/{id}")
    public KhuyenMai getById(@PathVariable Integer id) {
        return khuyenMaiService.getById(id);
    }

    // Tạo/sửa/xoá mã khuyến mãi ảnh hưởng trực tiếp doanh thu — chỉ staff. Trước đây
    // không khoá gì, bất kỳ khách hàng nào đăng nhập cũng tạo/sửa/xoá được mã khuyến mãi.
    // POST — service tự set ngayTao và soLanDaDung=0
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PostMapping
    public ResponseEntity<KhuyenMai> create(@Valid @RequestBody KhuyenMaiRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(khuyenMaiService.create(request));
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody KhuyenMaiRequest request) {
        khuyenMaiService.update(id, request);
        return ResponseEntity.ok().build();
    }

}
