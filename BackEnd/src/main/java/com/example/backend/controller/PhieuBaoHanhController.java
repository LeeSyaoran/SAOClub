package com.example.backend.controller;

import com.example.backend.entity.PhieuBaoHanh;
import com.example.backend.request.PhieuBaoHanhRequest;
import com.example.backend.response.PhieuBaoHanhResponse;
import com.example.backend.service.PhieuBaoHanhService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/phieu-bao-hanh")
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
public class PhieuBaoHanhController {

    @Autowired
    private PhieuBaoHanhService phieuBaoHanhService;

    @GetMapping
    public Page<PhieuBaoHanhResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return phieuBaoHanhService.hienThiPhieuBaoHanh(PageRequest.of(page, size));
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

}
