package com.example.backend.service;

import com.example.backend.entity.BienTheSanPham;
import com.example.backend.entity.NhanVien;
import com.example.backend.entity.SanPham;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.BienTheSanPhamRepository;
import com.example.backend.repository.LichSuThayDoiSanPhamRepository;
import com.example.backend.repository.SanPhamRepository;
import com.example.backend.repository.TaiKhoanRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LichSuThayDoiSanPhamServiceTest {

    @Mock private LichSuThayDoiSanPhamRepository lichSuThayDoiSanPhamRepository;
    @Mock private SanPhamRepository sanPhamRepository;
    @Mock private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Mock private TaiKhoanRepository taiKhoanRepository;

    @InjectMocks
    private LichSuThayDoiSanPhamService service;

    @BeforeEach
    void setUpSecurity() {
        SecurityContext context = mock(SecurityContext.class);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDownSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ghiNeuThayDoi_giaTriKhacNhau_luuLog() {
        when(sanPhamRepository.getReferenceById(10)).thenReturn(new SanPham());
        NhanVien nv = new NhanVien();
        nv.setNhanVienId(3);

        service.ghiNeuThayDoi(10, null, "san_pham", "tenSanPham", "Ten cu", "Ten moi", nv);

        verify(lichSuThayDoiSanPhamRepository).save(argThat(log ->
                "san_pham".equals(log.getDoiTuong())
                        && "tenSanPham".equals(log.getTenTruong())
                        && "Ten cu".equals(log.getGiaTriCu())
                        && "Ten moi".equals(log.getGiaTriMoi())
                        && log.getNhanVien() == nv
                        && log.getBienThe() == null));
    }

    @Test
    void ghiNeuThayDoi_giaTriGiongNhau_khongLuu() {
        service.ghiNeuThayDoi(10, null, "san_pham", "trangThai", "active", "active", null);

        verify(lichSuThayDoiSanPhamRepository, never()).save(any());
        verify(sanPhamRepository, never()).getReferenceById(any());
    }

    @Test
    void ghiNeuThayDoi_caHaiGiaTriNull_khongLuu() {
        service.ghiNeuThayDoi(10, null, "san_pham", "nhaCungCapId", null, null, null);

        verify(lichSuThayDoiSanPhamRepository, never()).save(any());
    }

    @Test
    void ghiNeuThayDoi_coBienThe_ganDungBienThe() {
        when(sanPhamRepository.getReferenceById(10)).thenReturn(new SanPham());
        BienTheSanPham bt = new BienTheSanPham();
        when(bienTheSanPhamRepository.getReferenceById(20)).thenReturn(bt);

        service.ghiNeuThayDoi(10, 20, "bien_the", "giaBan", "1000", "2000", null);

        verify(lichSuThayDoiSanPhamRepository).save(argThat(log -> log.getBienThe() == bt));
    }

    @Test
    void nguoiSuaHienTai_taiKhoanCoNhanVien_traVeNhanVien() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("nv1");
        when(SecurityContextHolder.getContext().getAuthentication()).thenReturn(auth);
        NhanVien nv = new NhanVien();
        nv.setNhanVienId(7);
        TaiKhoan tk = new TaiKhoan();
        tk.setNhanVien(nv);
        when(taiKhoanRepository.findByUsername("nv1")).thenReturn(Optional.of(tk));

        assertThat(service.nguoiSuaHienTai()).isEqualTo(nv);
    }

    @Test
    void nguoiSuaHienTai_taiKhoanKhongTonTai_traVeNull() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("ghost");
        when(SecurityContextHolder.getContext().getAuthentication()).thenReturn(auth);
        when(taiKhoanRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThat(service.nguoiSuaHienTai()).isNull();
    }
}
