package com.example.backend.controller;

import com.example.backend.entity.SanPham;
import com.example.backend.request.SanPhamRequest;
import com.example.backend.response.SanPhamResponse;
import com.example.backend.service.SanPhamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/san-pham")
@Validated
public class SanPhamController {

    @Autowired
    private SanPhamService sanPhamService;

    @GetMapping("hien-thi")
    public List<SanPhamResponse> getAll() {
        return sanPhamService.hienThiSanPham();
    }

    @GetMapping("/{id}")
    public SanPham getById(@PathVariable Integer id) {
        return sanPhamService.getSanPhamById(id);
    }

    @PostMapping
    public void create(@Valid @RequestBody SanPhamRequest request) {
        sanPhamService.createSanPham(request);
    }

    @PutMapping("update/{id}")
    public void update(@PathVariable Integer id,
                       @Valid @RequestBody SanPhamRequest request) {
        sanPhamService.updateSanPham(id, request);
    }

    @DeleteMapping("delete/{id}")
    public void delete(@PathVariable Integer id) {
        sanPhamService.deleteSanPham(id);
    }
}
