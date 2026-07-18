package com.example.backend.controller;

import com.example.backend.request.CaiDatHeThongRequest;
import com.example.backend.request.DoiMatKhauRequest;
import com.example.backend.response.CaiDatHeThongResponse;
import com.example.backend.service.AuthService;
import com.example.backend.service.CaiDatHeThongService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// /api/cai-dat/** — KHÔNG nằm trong permitAll() của SecurityConfig, nên mọi endpoint ở đây
// tự động yêu cầu JWT hợp lệ qua .anyRequest().authenticated() (xem SecurityConfig.java).
// get/update/ap-dung-nguong-ton-kho chỉ admin (xem cấu hình cửa hàng, đổi ngưỡng tồn kho hàng loạt).
// doi-mat-khau đặt ở đây (không phải AuthController) vì /api/auth/** đang permitAll() toàn bộ,
// và CỐ Ý không giới hạn role — mọi tài khoản (admin, nhân viên, quản kho, khách hàng) đều cần đổi được mật khẩu của chính mình.
@RestController
@RequestMapping("/api/cai-dat")
public class CaiDatController {

    @Autowired
    private CaiDatHeThongService caiDatHeThongService;

    @Autowired
    private AuthService authService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public CaiDatHeThongResponse get() {
        return caiDatHeThongService.get();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public CaiDatHeThongResponse update(@Valid @RequestBody CaiDatHeThongRequest req) {
        return caiDatHeThongService.update(req);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/ap-dung-nguong-ton-kho")
    public Map<String, Integer> apDungNguongTonKho(@RequestBody Map<String, Integer> body) {
        int nguong = body.getOrDefault("nguong", 0);
        int soBienTheDaCapNhat = caiDatHeThongService.apDungNguongTonKhoChoTatCa(nguong);
        return Map.of("soBienTheDaCapNhat", soBienTheDaCapNhat);
    }

    @PostMapping("/doi-mat-khau")
    public ResponseEntity<?> doiMatKhau(@Valid @RequestBody DoiMatKhauRequest req) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            authService.doiMatKhau(username, req.getMatKhauCu(), req.getMatKhauMoi());
            return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
