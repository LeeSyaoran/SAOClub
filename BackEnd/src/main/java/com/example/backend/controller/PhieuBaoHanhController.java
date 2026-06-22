package com.example.backend.controller;

import com.example.backend.entity.PhieuBaoHanh;
import com.example.backend.request.PhieuBaoHanhRequest;
import com.example.backend.response.PhieuBaoHanhResponse;
import com.example.backend.service.PhieuBaoHanhService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/phieu-bao-hanh")
public class PhieuBaoHanhController {

    @Autowired
    private PhieuBaoHanhService phieuBaoHanhService;

    @GetMapping
    public List<PhieuBaoHanhResponse> getAll() {
        return phieuBaoHanhService.hienThiPhieuBaoHanh();
    }

    @GetMapping("/{id}")
    public PhieuBaoHanh getById(@PathVariable Integer id) {
        return phieuBaoHanhService.getById(id);
    }

    // POST — service xử lý FK: donHang, bienThe (qua sanPhamId), khachHang
    @PostMapping
    public ResponseEntity<PhieuBaoHanh> create(@Valid @RequestBody PhieuBaoHanhRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(phieuBaoHanhService.create(request));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody PhieuBaoHanhRequest request) {
        phieuBaoHanhService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        phieuBaoHanhService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
