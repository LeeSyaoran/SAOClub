package com.example.backend.controller;

import com.example.backend.entity.PhieuGiamGiaCaNhan;
import com.example.backend.response.PhieuGiamGiaCaNhanResponse;
import com.example.backend.service.PhieuGiamGiaCaNhanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Chỉ đăng nhập là đủ — khách hàng tự đổi điểm/xem voucher của chính mình, service tự suy
// khách hàng qua SecurityContextHolder, không nhận khachHangId từ client.
@RestController
@RequestMapping("/api/phieu-giam-gia-ca-nhan")
@PreAuthorize("isAuthenticated()")
public class PhieuGiamGiaCaNhanController {

    @Autowired
    private PhieuGiamGiaCaNhanService phieuGiamGiaCaNhanService;

    @PostMapping("doi-thuong/{doiThuongId}")
    public ResponseEntity<PhieuGiamGiaCaNhan> doiThuong(@PathVariable Integer doiThuongId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(phieuGiamGiaCaNhanService.doiThuong(doiThuongId));
    }

    @GetMapping("cua-toi")
    public List<PhieuGiamGiaCaNhanResponse> getCuaToi() {
        return phieuGiamGiaCaNhanService.getCuaToi();
    }
}
