package com.example.backend.controller;

import com.example.backend.entity.NhanVien;
import com.example.backend.request.NhanVienRequest;
import com.example.backend.response.NhanVienResponse;
import com.example.backend.service.NhanVienService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/nhan-vien")
public class NhanVienController {

    @Autowired
    private NhanVienService nhanVienService;

    // GET /api/nhan-vien — DTO query với LEFT JOIN chuc_vu (tránh implicit INNER JOIN)
    @GetMapping
    public List<NhanVienResponse> getAll() {
        return nhanVienService.hienThiNhanVien();
    }

    @GetMapping("/{id}")
    public NhanVien getById(@PathVariable Integer id) {
        return nhanVienService.getById(id);
    }

    // POST — service xử lý FK chucVuId → ChucVu entity
    @PostMapping
    public ResponseEntity<NhanVien> create(@Valid @RequestBody NhanVienRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(nhanVienService.create(request));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody NhanVienRequest request) {
        nhanVienService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        nhanVienService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
