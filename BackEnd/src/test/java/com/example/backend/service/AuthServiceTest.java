package com.example.backend.service;

import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.security.jwt.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private TaiKhoanRepository taiKhoanRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void doiMatKhau_saiMatKhauCu_nemLoiKhongLuu() {
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername("admin");
        tk.setMatKhauHash("hash-cu");
        when(taiKhoanRepository.findByUsername("admin")).thenReturn(Optional.of(tk));
        when(passwordEncoder.matches("sai", "hash-cu")).thenReturn(false);

        assertThatThrownBy(() -> authService.doiMatKhau("admin", "sai", "moimoi123"))
                .isInstanceOf(BadCredentialsException.class);

        verify(taiKhoanRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doiMatKhau_dungMatKhauCu_luuHashMoi() {
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername("admin");
        tk.setMatKhauHash("hash-cu");
        when(taiKhoanRepository.findByUsername("admin")).thenReturn(Optional.of(tk));
        when(passwordEncoder.matches("dung", "hash-cu")).thenReturn(true);
        when(passwordEncoder.encode("moimoi123")).thenReturn("hash-moi");

        authService.doiMatKhau("admin", "dung", "moimoi123");

        assertThat(tk.getMatKhauHash()).isEqualTo("hash-moi");
        verify(taiKhoanRepository).save(tk);
    }
}
