package com.example.backend.controller;

import com.example.backend.entity.KhuyenMai;
import com.example.backend.request.KhuyenMaiRequest;
import com.example.backend.response.KhuyenMaiResponse;
import com.example.backend.service.KhuyenMaiService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
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

    // POST — service tự set ngayTao và soLanDaDung=0
    @PostMapping
    public ResponseEntity<KhuyenMai> create(@Valid @RequestBody KhuyenMaiRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(khuyenMaiService.create(request));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody KhuyenMaiRequest request) {
        khuyenMaiService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        khuyenMaiService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
