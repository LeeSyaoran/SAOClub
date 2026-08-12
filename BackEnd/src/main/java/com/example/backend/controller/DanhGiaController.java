package com.example.backend.controller;

import com.example.backend.entity.DanhGia;
import com.example.backend.request.DanhGiaRequest;
import com.example.backend.response.DanhGiaAdminResponse;
import com.example.backend.response.DanhGiaResponse;
import com.example.backend.response.DanhGiaSummaryResponse;
import com.example.backend.service.DanhGiaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/danh-gia")
public class DanhGiaController {

    @Autowired
    private DanhGiaService danhGiaService;

    @GetMapping("san-pham/{sanPhamId}")
    public List<DanhGiaResponse> hienThiTheoSanPham(@PathVariable Integer sanPhamId) {
        return danhGiaService.hienThiTheoSanPham(sanPhamId);
    }

    @GetMapping("tong-hop")
    public List<DanhGiaSummaryResponse> tongHopTatCa() {
        return danhGiaService.tongHopTatCa();
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<DanhGia> themDanhGia(@Valid @RequestBody DanhGiaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(danhGiaService.themDanhGia(request));
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("{id}")
    public ResponseEntity<Void> xoa(@PathVariable Integer id) {
        danhGiaService.xoa(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN')")
    @GetMapping("admin")
    public List<DanhGiaAdminResponse> hienThiTatCaAdmin() {
        return danhGiaService.hienThiTatCaAdmin();
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN')")
    @DeleteMapping("admin/{id}")
    public ResponseEntity<Void> xoaBoiAdmin(@PathVariable Integer id) {
        danhGiaService.xoaBoiAdmin(id);
        return ResponseEntity.noContent().build();
    }
}
