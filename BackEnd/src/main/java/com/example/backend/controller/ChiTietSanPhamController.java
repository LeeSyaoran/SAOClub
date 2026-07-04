package com.example.backend.controller;

import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.request.ChiTietSanPhamRequest;
import com.example.backend.response.ChiTietSanPhamResponse;
import com.example.backend.service.ChiTietSanPhamService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chi-tiet-san-pham")
public class ChiTietSanPhamController {

    @Autowired
    private ChiTietSanPhamService chiTietSanPhamService;

    @GetMapping
    public List<ChiTietSanPhamResponse> getAll() {
        return chiTietSanPhamService.hienThiChiTietSanPham();
    }

    @GetMapping("/{id}")
    public ChiTietSanPham getById(@PathVariable Integer id) {
        return chiTietSanPhamService.getById(id);
    }

    @GetMapping("/bien-the/{bienTheId}")
    public List<ChiTietSanPhamResponse> getByBienThe(@PathVariable Integer bienTheId) {
        return chiTietSanPhamService.getByBienTheId(bienTheId);
    }

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody ChiTietSanPhamRequest request) {
        chiTietSanPhamService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody ChiTietSanPhamRequest request) {
        chiTietSanPhamService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        chiTietSanPhamService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
