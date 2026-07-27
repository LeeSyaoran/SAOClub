package com.example.backend.service;

import com.example.backend.entity.ChucVu;
import com.example.backend.entity.KhachHang;
import com.example.backend.entity.LichSuTangDiem;
import com.example.backend.entity.NhanVien;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.ChucVuRepository;
import com.example.backend.repository.KhachHangRepository;
import com.example.backend.repository.LichSuTangDiemRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.request.TangDiemRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KhachHangServiceTest {

    @Mock private KhachHangRepository khachHangRepository;
    @Mock private TaiKhoanRepository taiKhoanRepository;
    @Mock private ChucVuRepository chucVuRepository;
    @Mock private LichSuTangDiemRepository lichSuTangDiemRepository;

    @InjectMocks
    private KhachHangService service;

    @BeforeEach
    void setUpSecurity() {
        SecurityContext context = mock(SecurityContext.class);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDownSecurity() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String username) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(username);
        when(SecurityContextHolder.getContext().getAuthentication()).thenReturn(auth);
    }

    @Test
    void tangDiem_congDiemVaLuuLichSu() {
        loginAs("admin1");
        ChucVu chucVuAdmin = new ChucVu();
        chucVuAdmin.setMaChucVu("admin");
        NhanVien admin = new NhanVien();
        admin.setNhanVienId(9);
        admin.setHoTen("Admin Test");
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername("admin1");
        tk.setChucVu(chucVuAdmin);
        tk.setNhanVien(admin);
        when(taiKhoanRepository.findByUsername("admin1")).thenReturn(Optional.of(tk));

        KhachHang khachHang = new KhachHang();
        khachHang.setKhachHangId(5);
        khachHang.setDiemTichLuy(100);
        when(khachHangRepository.findWithLockByKhachHangId(5)).thenReturn(Optional.of(khachHang));

        TangDiemRequest request = new TangDiemRequest(50, "Khách VIP");
        service.tangDiem(5, request);

        assertThat(khachHang.getDiemTichLuy()).isEqualTo(150);
        verify(khachHangRepository).save(khachHang);

        ArgumentCaptor<LichSuTangDiem> captor = ArgumentCaptor.forClass(LichSuTangDiem.class);
        verify(lichSuTangDiemRepository).save(captor.capture());
        LichSuTangDiem saved = captor.getValue();
        assertThat(saved.getSoDiem()).isEqualTo(50);
        assertThat(saved.getLyDo()).isEqualTo("Khách VIP");
        assertThat(saved.getKhachHang()).isEqualTo(khachHang);
        assertThat(saved.getNhanVien()).isEqualTo(admin);
    }

    @Test
    void tangDiem_khachHangKhongTonTai_nemLoi() {
        when(khachHangRepository.findWithLockByKhachHangId(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.tangDiem(99, new TangDiemRequest(10, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Khách hàng không tồn tại");
    }
}
