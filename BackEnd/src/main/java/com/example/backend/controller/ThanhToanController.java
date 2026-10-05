package com.example.backend.controller;

import com.example.backend.entity.ThanhToan;
import com.example.backend.request.ThanhToanRequest;
import com.example.backend.response.ThanhToanResponse;
import com.example.backend.service.ThanhToanService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/thanh-toan")
public class ThanhToanController {

    @Autowired
    private ThanhToanService thanhToanService;

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @GetMapping
    public List<ThanhToanResponse> getAll() {
        return thanhToanService.hienThiThanhToan();
    }

    @PreAuthorize("@orderAccessGuard.canView(#donHangId)")
    @GetMapping("/don-hang/{donHangId}")
    public List<ThanhToanResponse> getByDonHang(@PathVariable Integer donHangId) {
        return thanhToanService.hienThiThanhToanTheoDonHang(donHangId);
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @GetMapping("/{id}")
    public ThanhToan getById(@PathVariable Integer id) {
        return thanhToanService.getById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PostMapping
    public ResponseEntity<ThanhToan> create(@Valid @RequestBody ThanhToanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(thanhToanService.create(request));
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody ThanhToanRequest request) {
        thanhToanService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        thanhToanService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
