package com.example.backend.controller;

import com.example.backend.entity.KhachHang;
import com.example.backend.entity.SanPhamYeuThich;
import com.example.backend.response.SanPhamYeuThichResponse;
import com.example.backend.service.SanPhamYeuThichService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/yeu-thich")
@PreAuthorize("isAuthenticated()")
public class SanPhamYeuThichController {

    @Autowired
    private SanPhamYeuThichService sanPhamYeuThichService;

    // Staff xem wishlist theo khachHangId, khách hàng xem wishlist của chính mình
    @GetMapping
    public List<SanPhamYeuThichResponse> danhSach(
            @RequestParam(required = false) Integer khachHangId) {
        return sanPhamYeuThichService.danhSach(khachHangId);
    }

    // Khách hàng thêm vào wishlist của chính mình
    @PostMapping("{bienTheId}")
    public ResponseEntity<SanPhamYeuThich> themVao(@PathVariable Integer bienTheId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sanPhamYeuThichService.themVao(bienTheId));
    }

    // Khách hàng xóa khỏi wishlist của chính mình
    @DeleteMapping("{bienTheId}")
    public ResponseEntity<Void> xoa(@PathVariable Integer bienTheId) {
        sanPhamYeuThichService.xoa(bienTheId);
        return ResponseEntity.noContent().build();
    }
}
