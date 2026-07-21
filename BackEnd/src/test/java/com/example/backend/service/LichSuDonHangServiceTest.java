package com.example.backend.service;

import com.example.backend.entity.ChucVu;
import com.example.backend.entity.DonHang;
import com.example.backend.entity.KhachHang;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.DonHangRepository;
import com.example.backend.repository.LichSuDonHangRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.response.LichSuDonHangResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LichSuDonHangServiceTest {

    @Mock private LichSuDonHangRepository lichSuDonHangRepository;
    @Mock private DonHangRepository donHangRepository;
    @Mock private TaiKhoanRepository taiKhoanRepository;

    @InjectMocks
    private LichSuDonHangService service;

    @BeforeEach
    void setUp() {
        SecurityContext context = mock(SecurityContext.class);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String username) {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(username);
        when(SecurityContextHolder.getContext().getAuthentication()).thenReturn(auth);
    }

    private TaiKhoan taiKhoanKhachHang(String username, Integer khachHangId) {
        ChucVu chucVu = new ChucVu();
        chucVu.setMaChucVu("khach_hang");
        KhachHang kh = new KhachHang();
        kh.setKhachHangId(khachHangId);
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername(username);
        tk.setChucVu(chucVu);
        tk.setKhachHang(kh);
        return tk;
    }

    private TaiKhoan taiKhoanStaff(String username) {
        ChucVu chucVu = new ChucVu();
        chucVu.setMaChucVu("admin");
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername(username);
        tk.setChucVu(chucVu);
        return tk;
    }

    private DonHang donHangCuaKhach(Integer donHangId, Integer khachHangId) {
        DonHang donHang = new DonHang();
        donHang.setId(donHangId);
        KhachHang kh = new KhachHang();
        kh.setKhachHangId(khachHangId);
        donHang.setKhachHang(kh);
        return donHang;
    }

    @Test
    void getByDonHang_traVeDungThuTuTheoRepository() {
        loginAs("admin1");
        when(taiKhoanRepository.findByUsername("admin1")).thenReturn(Optional.of(taiKhoanStaff("admin1")));
        when(donHangRepository.findById(5)).thenReturn(Optional.of(donHangCuaKhach(5, 99)));

        LichSuDonHangResponse r1 = new LichSuDonHangResponse(1, 5, null, "pending", LocalDateTime.now());
        LichSuDonHangResponse r2 = new LichSuDonHangResponse(2, 5, "pending", "confirmed", LocalDateTime.now());
        when(lichSuDonHangRepository.getByDonHangId(5)).thenReturn(List.of(r1, r2));

        List<LichSuDonHangResponse> result = service.getByDonHang(5);

        assertThat(result).containsExactly(r1, r2);
    }

    @Test
    void getByDonHang_nhanVienXemDuocDonBatKy() {
        loginAs("staff1");
        when(taiKhoanRepository.findByUsername("staff1")).thenReturn(Optional.of(taiKhoanStaff("staff1")));
        when(donHangRepository.findById(7)).thenReturn(Optional.of(donHangCuaKhach(7, 42)));
        when(lichSuDonHangRepository.getByDonHangId(7)).thenReturn(List.of());

        assertThat(service.getByDonHang(7)).isEmpty();
    }

    @Test
    void getByDonHang_chuDonXemDuocDonCuaChinhMinh() {
        loginAs("khach1");
        when(taiKhoanRepository.findByUsername("khach1")).thenReturn(Optional.of(taiKhoanKhachHang("khach1", 42)));
        when(donHangRepository.findById(7)).thenReturn(Optional.of(donHangCuaKhach(7, 42)));
        when(lichSuDonHangRepository.getByDonHangId(7)).thenReturn(List.of());

        assertThat(service.getByDonHang(7)).isEmpty();
    }

    @Test
    void getByDonHang_khachKhacBiTuChoi() {
        loginAs("khach2");
        when(taiKhoanRepository.findByUsername("khach2")).thenReturn(Optional.of(taiKhoanKhachHang("khach2", 43)));
        when(donHangRepository.findById(7)).thenReturn(Optional.of(donHangCuaKhach(7, 42)));

        assertThatThrownBy(() -> service.getByDonHang(7))
                .isInstanceOf(AccessDeniedException.class);
    }
}
