package com.example.backend.controller;

import com.example.backend.entity.ChiTietPhieuNhap;
import com.example.backend.request.ChiTietPhieuNhapRequest;
import com.example.backend.response.ChiTietPhieuNhapResponse;
import com.example.backend.service.ChiTietPhieuNhapService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chi-tiet-phieu-nhap")
public class ChiTietPhieuNhapController {

    @Autowired
    private ChiTietPhieuNhapService chiTietPhieuNhapService;

    @GetMapping
    public List<ChiTietPhieuNhapResponse> getAll() {
        return chiTietPhieuNhapService.hienThiChiTietPhieuNhap();
    }

    @GetMapping("/{id}")
    public ChiTietPhieuNhap getById(@PathVariable Integer id) {
        return chiTietPhieuNhapService.getById(id);
    }

    @PostMapping
    public ResponseEntity<ChiTietPhieuNhap> create(@Valid @RequestBody ChiTietPhieuNhapRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chiTietPhieuNhapService.create(request));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody ChiTietPhieuNhapRequest request) {
        chiTietPhieuNhapService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        chiTietPhieuNhapService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
