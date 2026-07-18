package com.example.backend.controller;

import com.example.backend.entity.PhieuNhapKho;
import com.example.backend.request.PhieuNhapKhoRequest;
import com.example.backend.response.PhieuNhapKhoResponse;
import com.example.backend.service.PhieuNhapKhoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Phiếu nhập kho — chỉ staff, không có nơi nào trong code khách hàng gọi tới.
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
@RestController
@RequestMapping("/api/phieu-nhap-kho")
public class PhieuNhapKhoController {

    @Autowired
    private PhieuNhapKhoService phieuNhapKhoService;

    @GetMapping
    public List<PhieuNhapKhoResponse> getAll() {
        return phieuNhapKhoService.hienThiPhieuNhapKho();
    }

    @GetMapping("/{id}")
    public PhieuNhapKho getById(@PathVariable Integer id) {
        return phieuNhapKhoService.getById(id);
    }

    // POST — service xử lý FK nhaCungCap, nhanVien
    @PostMapping
    public ResponseEntity<PhieuNhapKho> create(@Valid @RequestBody PhieuNhapKhoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(phieuNhapKhoService.create(request));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody PhieuNhapKhoRequest request) {
        phieuNhapKhoService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        phieuNhapKhoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
