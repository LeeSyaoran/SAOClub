package com.example.backend.security.service;

import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.TaiKhoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailService implements UserDetailsService {

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        TaiKhoan tk = taiKhoanRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tồn tại tài khoản: " + username));

        if (!"active".equals(tk.getTrangThai()))
            throw new UsernameNotFoundException("Tài khoản đã bị khóa hoặc vô hiệu");

        String role = tk.getChucVu().getMaChucVu().toUpperCase(); // "ADMIN", "NHAN_VIEN", ...
        return User.withUsername(tk.getUsername())
                .password(tk.getMatKhauHash())
                .roles(role)
                .build();
    }
}
