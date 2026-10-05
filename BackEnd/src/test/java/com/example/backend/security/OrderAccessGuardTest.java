package com.example.backend.security;

import com.example.backend.entity.ChucVu;
import com.example.backend.entity.DonHang;
import com.example.backend.entity.KhachHang;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.DonHangRepository;
import com.example.backend.repository.TaiKhoanRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderAccessGuardTest {

    @Mock
    private DonHangRepository donHangRepository;

    @Mock
    private TaiKhoanRepository taiKhoanRepository;

    @InjectMocks
    private OrderAccessGuard guard;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void canView_chuaDangNhap_traVeFalse() {
        assertThat(guard.canView(1)).isFalse();
    }

    @Test
    void canView_donHangIdNull_traVeFalse() {
        assertThat(guard.canView(null)).isFalse();
    }

    @Test
    void canView_nhanVien_duocXemMoiDon() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin_user", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        TaiKhoan tk = new TaiKhoan();
        ChucVu cv = new ChucVu();
        cv.setMaChucVu("admin");
        tk.setChucVu(cv);

        when(taiKhoanRepository.findByUsername("admin_user")).thenReturn(Optional.of(tk));

        assertThat(guard.canView(99)).isTrue();
    }

    @Test
    void canView_khachHangDungDon_traVeTrue() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("khach1", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );

        TaiKhoan tk = new TaiKhoan();
        ChucVu cv = new ChucVu();
        cv.setMaChucVu("khach_hang");
        tk.setChucVu(cv);

        KhachHang kh = new KhachHang();
        kh.setKhachHangId(10);
        tk.setKhachHang(kh);

        DonHang dh = new DonHang();
        dh.setId(100);
        dh.setKhachHang(kh);

        when(taiKhoanRepository.findByUsername("khach1")).thenReturn(Optional.of(tk));
        when(donHangRepository.findById(100)).thenReturn(Optional.of(dh));

        assertThat(guard.canView(100)).isTrue();
    }

    @Test
    void canView_khachHangXemDonCuaNguoiKhac_traVeFalse() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("khach1", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );

        TaiKhoan tk = new TaiKhoan();
        ChucVu cv = new ChucVu();
        cv.setMaChucVu("khach_hang");
        tk.setChucVu(cv);

        KhachHang kh1 = new KhachHang();
        kh1.setKhachHangId(10);
        tk.setKhachHang(kh1);

        KhachHang kh2 = new KhachHang();
        kh2.setKhachHangId(20);

        DonHang dh = new DonHang();
        dh.setId(101);
        dh.setKhachHang(kh2);

        when(taiKhoanRepository.findByUsername("khach1")).thenReturn(Optional.of(tk));
        when(donHangRepository.findById(101)).thenReturn(Optional.of(dh));

        assertThat(guard.canView(101)).isFalse();
    }
}
