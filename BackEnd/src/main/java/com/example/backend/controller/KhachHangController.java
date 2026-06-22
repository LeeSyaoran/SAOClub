package com.example.backend.controller;

import com.example.backend.entity.KhachHang;
import com.example.backend.request.KhachHangRequest;
import com.example.backend.response.KhachHangResponse;
import com.example.backend.service.KhachHangService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/khach-hang")
public class KhachHangController {

    @Autowired
    private KhachHangService khachHangService;

    // GET /api/khach-hang — DTO query, tránh serialize toàn bộ entity
    @GetMapping
    public List<KhachHangResponse> getAll() {
        return khachHangService.hienThiKhachHang();
    }

    @GetMapping("/{id}")
    public KhachHang getById(@PathVariable Integer id) {
        return khachHangService.getById(id);
    }

    // POST /api/khach-hang — dùng Request DTO có validation
    @PostMapping
    public ResponseEntity<KhachHang> create(@Valid @RequestBody KhachHangRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(khachHangService.create(request));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody KhachHangRequest request) {
        khachHangService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        khachHangService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
