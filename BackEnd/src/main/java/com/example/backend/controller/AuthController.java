package com.example.backend.controller;

import com.example.backend.entity.KhachHang;
import com.example.backend.repository.KhachHangRepository;
import com.example.backend.request.LoginRequest;
import com.example.backend.request.RegisterRequest;
import com.example.backend.response.AuthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private KhachHangRepository khachHangRepository;

    // =======================
    // ĐĂNG KÝ
    // =======================
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody RegisterRequest request
    ){

        if(khachHangRepository.existsByUsername(request.getUsername())){
            return ResponseEntity.badRequest()
                    .body(new AuthResponse(
                            false,
                            "Tên đăng nhập đã tồn tại",
                            null
                    ));
        }

        if(khachHangRepository.existsByEmail(request.getEmail())){
            return ResponseEntity.badRequest()
                    .body(new AuthResponse(
                            false,
                            "Email đã tồn tại",
                            null
                    ));
        }

        if(khachHangRepository.existsBySoDienThoai(request.getSoDienThoai())){
            return ResponseEntity.badRequest()
                    .body(new AuthResponse(
                            false,
                            "Số điện thoại đã tồn tại",
                            null
                    ));
        }

        KhachHang kh = new KhachHang();

        kh.setHoTen(request.getHoTen());
        kh.setSoDienThoai(request.getSoDienThoai());
        kh.setEmail(request.getEmail());
        kh.setUsername(request.getUsername());
        kh.setMatKhau(request.getPassword());
        kh.setLoaiKhach("ca_nhan");
        kh.setDiemTichLuy(0);
        kh.setTrangThai("active");
        kh.setNgayTao(LocalDateTime.now());

        khachHangRepository.save(kh);

        return ResponseEntity.ok(
                new AuthResponse(
                        true,
                        "Đăng ký thành công",
                        kh
                )
        );

    }

    // =======================
    // ĐĂNG NHẬP
    // =======================
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request
    ){

        Optional<KhachHang> user =
                khachHangRepository.findByUsername(request.getUsername());

        if(user.isEmpty()){

            return ResponseEntity.badRequest()
                    .body(
                            new AuthResponse(
                                    false,
                                    "Sai tài khoản",
                                    null
                            )
                    );

        }

        if(!user.get().getMatKhau().equals(request.getPassword())){

            return ResponseEntity.badRequest()
                    .body(
                            new AuthResponse(
                                    false,
                                    "Sai mật khẩu",
                                    null
                            )
                    );

        }

        return ResponseEntity.ok(
                new AuthResponse(
                        true,
                        "Đăng nhập thành công",
                        user.get()
                )
        );

    }

}