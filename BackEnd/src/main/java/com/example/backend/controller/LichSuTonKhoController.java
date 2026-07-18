package com.example.backend.controller;

import com.example.backend.entity.LichSuTonKho;
import com.example.backend.request.LichSuTonKhoRequest;
import com.example.backend.response.LichSuTonKhoResponse;
import com.example.backend.service.LichSuTonKhoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Lịch sử tồn kho — chỉ staff, không có nơi nào trong code khách hàng gọi tới.
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
@RestController
@RequestMapping("/api/lich-su-ton-kho")
public class LichSuTonKhoController {

    @Autowired
    private LichSuTonKhoService lichSuTonKhoService;

    @GetMapping
    public List<LichSuTonKhoResponse> getAll() {
        return lichSuTonKhoService.hienThiLichSuTonKho();
    }

    @GetMapping("/{id}")
    public LichSuTonKho getById(@PathVariable Integer id) {
        return lichSuTonKhoService.getById(id);
    }

    // POST — service xử lý nhiều FK optional (bienThe, chiTiet, donHang, phieuNhap, nhanVien)
    @PostMapping
    public ResponseEntity<LichSuTonKho> create(@Valid @RequestBody LichSuTonKhoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lichSuTonKhoService.create(request));
    }

    // Lịch sử tồn kho không cho phép cập nhật — chỉ ghi thêm và xóa (audit trail)
    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        lichSuTonKhoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
