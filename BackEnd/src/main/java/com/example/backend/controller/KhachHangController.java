package com.example.backend.controller;

import com.example.backend.entity.KhachHang;
import com.example.backend.request.KhachHangRegisterRequest;
import com.example.backend.request.KhachHangRequest;
import com.example.backend.response.KhachHangResponse;
import com.example.backend.service.KhachHangService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/khach-hang")
public class KhachHangController {

    @Autowired
    private KhachHangService khachHangService;

    // Danh sách toàn bộ khách hàng — chỉ nhân viên/admin/quản kho được xem
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @GetMapping
    public List<KhachHangResponse> getAll() {
        return khachHangService.hienThiKhachHang();
    }

    // Xem 1 khách hàng — nhân viên xem ai cũng được, khách chỉ xem chính mình (check trong service)
    @GetMapping("/{id}")
    public KhachHang getById(@PathVariable Integer id) {
        return khachHangService.getById(id);
    }

    // Tạo khách hàng (walk-in) — chỉ nhân viên/admin, khách tự đăng ký qua /register
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PostMapping
    public ResponseEntity<KhachHang> create(@Valid @RequestBody KhachHangRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(khachHangService.create(request));
    }

    // Sửa khách hàng — nhân viên sửa ai cũng được, khách chỉ sửa chính mình (check trong service)
    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody KhachHangRequest request) {
        khachHangService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        khachHangService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody KhachHangRegisterRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(khachHangService.register(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
