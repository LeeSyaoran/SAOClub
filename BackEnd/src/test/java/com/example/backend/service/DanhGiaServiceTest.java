package com.example.backend.service;

import com.example.backend.entity.ChucVu;
import com.example.backend.entity.DanhGia;
import com.example.backend.entity.DonHang;
import com.example.backend.entity.KhachHang;
import com.example.backend.entity.SanPham;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.DanhGiaRepository;
import com.example.backend.repository.DonHangRepository;
import com.example.backend.repository.SanPhamRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.request.DanhGiaRequest;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DanhGiaServiceTest {

    @Mock private DanhGiaRepository danhGiaRepository;
    @Mock private SanPhamRepository sanPhamRepository;
    @Mock private DonHangRepository donHangRepository;
    @Mock private TaiKhoanRepository taiKhoanRepository;

    @InjectMocks
    private DanhGiaService service;

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

    @Test
    void themDanhGia_daMuaVaDaGiao_thanhCong() {
        loginAs("khach1");
        when(taiKhoanRepository.findByUsername("khach1")).thenReturn(Optional.of(taiKhoanKhachHang("khach1", 1)));
        when(danhGiaRepository.findByKhachHang_KhachHangIdAndSanPham_SanPhamId(1, 10)).thenReturn(Optional.empty());
        when(danhGiaRepository.timDonHangDaGiao(1, 10)).thenReturn(List.of(99));
        when(sanPhamRepository.getReferenceById(10)).thenReturn(new SanPham());
        DonHang donHang = new DonHang();
        donHang.setId(99);
        when(donHangRepository.getReferenceById(99)).thenReturn(donHang);
        when(danhGiaRepository.save(any(DanhGia.class))).thenAnswer(inv -> inv.getArgument(0));

        DanhGiaRequest request = new DanhGiaRequest(10, 5, "Rất tốt");
        DanhGia result = service.themDanhGia(request);

        assertThat(result.getSoSao()).isEqualTo(5);
        assertThat(result.getDonHang().getId()).isEqualTo(99);
        verify(danhGiaRepository).save(any(DanhGia.class));
    }

    @Test
    void themDanhGia_chuaMuaHangHoacChuaDuocGiao_biTuChoi() {
        loginAs("khach1");
        when(taiKhoanRepository.findByUsername("khach1")).thenReturn(Optional.of(taiKhoanKhachHang("khach1", 1)));
        when(danhGiaRepository.findByKhachHang_KhachHangIdAndSanPham_SanPhamId(1, 10)).thenReturn(Optional.empty());
        when(danhGiaRepository.timDonHangDaGiao(1, 10)).thenReturn(List.of());

        DanhGiaRequest request = new DanhGiaRequest(10, 5, "Rất tốt");

        assertThatThrownBy(() -> service.themDanhGia(request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(danhGiaRepository, never()).save(any());
    }

    @Test
    void themDanhGia_daDanhGiaRoi_biTuChoi() {
        loginAs("khach1");
        when(taiKhoanRepository.findByUsername("khach1")).thenReturn(Optional.of(taiKhoanKhachHang("khach1", 1)));
        when(danhGiaRepository.findByKhachHang_KhachHangIdAndSanPham_SanPhamId(1, 10)).thenReturn(Optional.of(new DanhGia()));

        DanhGiaRequest request = new DanhGiaRequest(10, 5, "Rất tốt");

        assertThatThrownBy(() -> service.themDanhGia(request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(danhGiaRepository, never()).save(any());
    }

    @Test
    void xoa_chuSoHuu_thanhCong() {
        loginAs("khach1");
        when(taiKhoanRepository.findByUsername("khach1")).thenReturn(Optional.of(taiKhoanKhachHang("khach1", 1)));
        KhachHang kh = new KhachHang();
        kh.setKhachHangId(1);
        DanhGia dg = new DanhGia();
        dg.setDanhGiaId(7);
        dg.setKhachHang(kh);
        when(danhGiaRepository.findById(7)).thenReturn(Optional.of(dg));

        service.xoa(7);

        verify(danhGiaRepository).delete(dg);
    }

    @Test
    void xoa_khongPhaiChuSoHuu_biTuChoi() {
        loginAs("khach2");
        when(taiKhoanRepository.findByUsername("khach2")).thenReturn(Optional.of(taiKhoanKhachHang("khach2", 2)));
        KhachHang chuSoHuu = new KhachHang();
        chuSoHuu.setKhachHangId(1);
        DanhGia dg = new DanhGia();
        dg.setDanhGiaId(7);
        dg.setKhachHang(chuSoHuu);
        when(danhGiaRepository.findById(7)).thenReturn(Optional.of(dg));

        assertThatThrownBy(() -> service.xoa(7))
                .isInstanceOf(AccessDeniedException.class);
        verify(danhGiaRepository, never()).delete(any());
    }

    @Test
    void xoaBoiAdmin_khongPhaiChuSoHuu_vanXoaDuoc() {
        when(danhGiaRepository.existsById(7)).thenReturn(true);

        service.xoaBoiAdmin(7);

        verify(danhGiaRepository).deleteById(7);
    }

    @Test
    void xoaBoiAdmin_khongTonTai_biTuChoi() {
        when(danhGiaRepository.existsById(99)).thenReturn(false);

        assertThatThrownBy(() -> service.xoaBoiAdmin(99))
                .isInstanceOf(IllegalArgumentException.class);
        verify(danhGiaRepository, never()).deleteById(any());
    }
}
