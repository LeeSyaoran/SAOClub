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
import java.util.Map;

@RestController
@RequestMapping("/api/khuyen-mai")
public class KhuyenMaiController {

    @Autowired
    private KhuyenMaiService khuyenMaiService;

    @GetMapping
    public List<KhuyenMaiResponse> getAll() {
        return khuyenMaiService.hienThiKhuyenMai();
    }

    @GetMapping("/{id}")
    public KhuyenMai getById(@PathVariable Integer id) {
        return khuyenMaiService.getById(id);
    }

    // POST /api/khuyen-mai/kiem-tra — validate promo code for customer checkout
    @PostMapping("kiem-tra")
    public ResponseEntity<?> kiemTraMa(@RequestBody Map<String, Object> body) {
        String maKhuyenMai = (String) body.get("maKhuyenMai");
        try {
            KhuyenMai km = khuyenMaiService.kiemTraMaKhuyenMai(maKhuyenMai);
            return ResponseEntity.ok(Map.of(
                "valid", true,
                "khuyenMaiId", km.getKhuyenMaiId(),
                "tenKhuyenMai", km.getTenKhuyenMai() != null ? km.getTenKhuyenMai() : km.getMaKhuyenMai(),
                "loai", km.getLoai() != null ? km.getLoai() : "fixed",
                "giaTri", km.getGiaTri() != null ? km.getGiaTri().doubleValue() : 0,
                "giaTriToiDa", km.getGiaTriToiDa() != null ? km.getGiaTriToiDa().doubleValue() : null,
                "donHangToiThieu", km.getDonHangToiThieu() != null ? km.getDonHangToiThieu().doubleValue() : null
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "message", e.getMessage()));
        }
    }

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
