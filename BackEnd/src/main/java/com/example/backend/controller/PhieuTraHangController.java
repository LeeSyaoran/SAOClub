package com.example.backend.controller;

import com.example.backend.entity.PhieuTraHang;
import com.example.backend.request.PhieuTraHangRequest;
import com.example.backend.request.YeuCauTraHangRequest;
import com.example.backend.response.PhieuTraHangResponse;
import com.example.backend.service.PhieuTraHangService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/phieu-tra-hang")
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
public class PhieuTraHangController {

    @Autowired
    private PhieuTraHangService phieuTraHangService;

    @GetMapping
    public List<PhieuTraHangResponse> getAll() {
        return phieuTraHangService.hienThiPhieuTraHang();
    }

    @GetMapping("/{id}")
    public PhieuTraHang getById(@PathVariable Integer id) {
        return phieuTraHangService.getById(id);
    }

    @PostMapping
    public ResponseEntity<PhieuTraHang> create(@Valid @RequestBody PhieuTraHangRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(phieuTraHangService.create(request));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody PhieuTraHangRequest request) {
        phieuTraHangService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        phieuTraHangService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Giữ mở cho MỌI người dùng đã đăng nhập (override @PreAuthorize class-level ở trên) —
    // khách hàng tự gửi yêu cầu trả hàng cho đơn của chính mình. Service tự suy khách hàng
    // qua SecurityContextHolder, không tin donHangId/khách hàng từ client.
    @PreAuthorize("isAuthenticated()")
    @PostMapping("tu-yeu-cau")
    public ResponseEntity<PhieuTraHang> taoYeuCauTuKhachHang(@Valid @RequestBody YeuCauTraHangRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(phieuTraHangService.taoYeuCauTuKhachHang(request));
    }

    // Giữ mở cho MỌI người dùng đã đăng nhập — khách xem yêu cầu trả hàng của đơn mình,
    // nhân viên xem được của bất kỳ đơn nào (service tự kiểm tra quyền theo vai trò).
    @PreAuthorize("isAuthenticated()")
    @GetMapping("don-hang/{donHangId}")
    public List<PhieuTraHangResponse> getByDonHang(@PathVariable Integer donHangId) {
        return phieuTraHangService.getByDonHang(donHangId);
    }
}
