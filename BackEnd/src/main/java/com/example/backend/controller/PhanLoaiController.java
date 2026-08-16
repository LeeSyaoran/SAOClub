package com.example.backend.controller;

import com.example.backend.service.PhanLoaiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Trước đây chưa có controller nào map /api/phan-loai nên Spring rơi vào nhánh tìm static
 * resource và ném NoResourceFoundException — log hiện 500 mỗi lần mở màn hàng hóa.
 */
@RestController
@RequestMapping("/api/phan-loai")
public class PhanLoaiController {

    @Autowired
    private PhanLoaiService phanLoaiService;

    /** Danh mục phân loại — dùng cho bộ lọc và form tạo sản phẩm. */
    @GetMapping
    public List<Map<String, Object>> danhSach() {
        return phanLoaiService.danhSach();
    }

    /** Các phân loại đang gán cho một sản phẩm, để form sửa tích sẵn. */
    @GetMapping("/san-pham/{sanPhamId}")
    public List<Integer> cuaSanPham(@PathVariable Integer sanPhamId) {
        return phanLoaiService.cuaSanPham(sanPhamId);
    }

    /** Ghi đè danh sách phân loại của sản phẩm; trigger tự đồng bộ cột cache trên biến thể. */
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PutMapping("/san-pham/{sanPhamId}")
    public ResponseEntity<Void> luuChoSanPham(@PathVariable Integer sanPhamId,
                                              @RequestBody List<Integer> phanLoaiIds) {
        phanLoaiService.luuChoSanPham(sanPhamId, phanLoaiIds);
        return ResponseEntity.ok().build();
    }
}