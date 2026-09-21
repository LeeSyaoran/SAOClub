package com.example.backend.controller;

import com.example.backend.service.PhanLoaiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Controller quản lý phân loại sản phẩm
@RestController
@RequestMapping("/api/phan-loai")
public class PhanLoaiController {

    @Autowired
    private PhanLoaiService phanLoaiService;

    // Lấy danh sách phân loại
    @GetMapping
    public List<Map<String, Object>> danhSach() {
        return phanLoaiService.danhSach();
    }

    // Lấy danh sách phân loại theo sản phẩm
    @GetMapping("/san-pham/{sanPhamId}")
    public List<Integer> cuaSanPham(@PathVariable Integer sanPhamId) {
        return phanLoaiService.cuaSanPham(sanPhamId);
    }

    // Cập nhật phân loại cho sản phẩm
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PutMapping("/san-pham/{sanPhamId}")
    public ResponseEntity<Void> luuChoSanPham(@PathVariable Integer sanPhamId,
                                              @RequestBody List<Integer> phanLoaiIds) {
        phanLoaiService.luuChoSanPham(sanPhamId, phanLoaiIds);
        return ResponseEntity.ok().build();
    }
}