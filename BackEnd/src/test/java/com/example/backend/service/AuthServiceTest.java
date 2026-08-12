package com.example.backend.service;

import com.example.backend.entity.ChucVu;
import com.example.backend.entity.NhanVien;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.NhanVienRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.request.HoSoRequest;
import com.example.backend.response.HoSoResponse;
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
    @Mock private NhanVienRepository nhanVienRepository;
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

    @Test
    void capNhatHoSo_coNhanVien_chiSuaDungBaTruong() {
        NhanVien nv = new NhanVien();
        nv.setNhanVienId(7);
        nv.setHoTen("Tên cũ");
        nv.setSoDienThoai("0900000000");
        nv.setEmail("cu@example.com");
        nv.setChucVu(new ChucVu()); 
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername("admin");
        tk.setNhanVien(nv);
        when(taiKhoanRepository.findByUsername("admin")).thenReturn(Optional.of(tk));

        HoSoRequest req = new HoSoRequest();
        req.setHoTen("Tên mới");
        req.setSoDienThoai("0911111111");
        req.setEmail("moi@example.com");

        HoSoResponse res = authService.capNhatHoSo("admin", req);

        assertThat(res.getHoTen()).isEqualTo("Tên mới");
        assertThat(res.getSoDienThoai()).isEqualTo("0911111111");
        assertThat(res.getEmail()).isEqualTo("moi@example.com");
        assertThat(nv.getChucVu()).isNotNull(); 
        verify(nhanVienRepository).save(nv);
    }

    @Test
    void capNhatHoSo_taiKhoanKhongCoNhanVien_nemLoi() {
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername("khachle");
        tk.setNhanVien(null);
        when(taiKhoanRepository.findByUsername("khachle")).thenReturn(Optional.of(tk));

        assertThatThrownBy(() -> authService.capNhatHoSo("khachle", new HoSoRequest()))
                .isInstanceOf(IllegalStateException.class);

        verify(nhanVienRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
