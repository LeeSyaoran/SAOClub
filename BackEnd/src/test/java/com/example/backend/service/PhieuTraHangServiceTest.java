package com.example.backend.service;

import com.example.backend.entity.DonHang;
import com.example.backend.entity.KhachHang;
import com.example.backend.entity.PhieuTraHang;
import com.example.backend.repository.DonHangRepository;
import com.example.backend.repository.KhachHangRepository;
import com.example.backend.repository.NhanVienRepository;
import com.example.backend.repository.PhieuTraHangRepository;
import com.example.backend.request.PhieuTraHangRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhieuTraHangServiceTest {

    @Mock private PhieuTraHangRepository phieuTraHangRepository;
    @Mock private DonHangRepository donHangRepository;
    @Mock private NhanVienRepository nhanVienRepository;
    @Mock private KhachHangRepository khachHangRepository;

    @InjectMocks
    private PhieuTraHangService service;

    private PhieuTraHangRequest requestDaXuLyQuaVi(Integer donHangId, BigDecimal soTien) {
        PhieuTraHangRequest r = new PhieuTraHangRequest();
        r.setDonHangId(donHangId);
        r.setNhanVienId(null);
        r.setLyDo("Hàng lỗi");
        r.setNgayTra(LocalDateTime.now());
        r.setTrangThai("da_xu_ly");
        r.setSoTienHoan(soTien);
        r.setHinhThucHoan("vi");
        r.setGhiChu("—");
        return r;
    }

    @Test
    void update_chuyenDaXuLy_hinhThucVi_congTienVaoVi() {
        KhachHang kh = new KhachHang();
        kh.setKhachHangId(1);
        kh.setSoDuVi(BigDecimal.valueOf(100_000));

        DonHang donHang = new DonHang();
        donHang.setId(9);
        donHang.setKhachHang(kh);

        PhieuTraHang phieu = new PhieuTraHang();
        phieu.setPhieuTraId(5);
        phieu.setDonHang(donHang);
        phieu.setTrangThai("cho_xu_ly");

        when(phieuTraHangRepository.findById(5)).thenReturn(Optional.of(phieu));
        when(donHangRepository.getReferenceById(9)).thenReturn(donHang);
        when(phieuTraHangRepository.save(phieu)).thenReturn(phieu);

        service.update(5, requestDaXuLyQuaVi(9, BigDecimal.valueOf(50_000)));

        assertThat(kh.getSoDuVi()).isEqualByComparingTo(BigDecimal.valueOf(150_000));
        verify(khachHangRepository).save(kh);
    }

    @Test
    void update_chuyenDaXuLy_hinhThucTienMat_khongDungToiVi() {
        KhachHang kh = new KhachHang();
        kh.setKhachHangId(1);
        kh.setSoDuVi(BigDecimal.valueOf(100_000));

        DonHang donHang = new DonHang();
        donHang.setId(9);
        donHang.setKhachHang(kh);

        PhieuTraHang phieu = new PhieuTraHang();
        phieu.setPhieuTraId(5);
        phieu.setDonHang(donHang);
        phieu.setTrangThai("cho_xu_ly");

        when(phieuTraHangRepository.findById(5)).thenReturn(Optional.of(phieu));
        when(donHangRepository.getReferenceById(9)).thenReturn(donHang);
        when(phieuTraHangRepository.save(phieu)).thenReturn(phieu);

        PhieuTraHangRequest req = requestDaXuLyQuaVi(9, BigDecimal.valueOf(50_000));
        req.setHinhThucHoan("tien_mat");

        service.update(5, req);

        assertThat(kh.getSoDuVi()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
        verify(khachHangRepository, never()).save(any());
    }

    @Test
    void update_daXuLyRoiSuaLaiVanDaXuLy_khongCongViLanNua() {
        KhachHang kh = new KhachHang();
        kh.setKhachHangId(1);
        kh.setSoDuVi(BigDecimal.valueOf(100_000));

        DonHang donHang = new DonHang();
        donHang.setId(9);
        donHang.setKhachHang(kh);

        PhieuTraHang phieu = new PhieuTraHang();
        phieu.setPhieuTraId(5);
        phieu.setDonHang(donHang);
        phieu.setTrangThai("da_xu_ly"); // đã xử lý từ trước

        when(phieuTraHangRepository.findById(5)).thenReturn(Optional.of(phieu));
        when(donHangRepository.getReferenceById(9)).thenReturn(donHang);
        when(phieuTraHangRepository.save(phieu)).thenReturn(phieu);

        service.update(5, requestDaXuLyQuaVi(9, BigDecimal.valueOf(50_000)));

        assertThat(kh.getSoDuVi()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
        verify(khachHangRepository, never()).save(any());
    }

    // ── Guard: chặn sửa các trường tiền-liên-quan sau khi đã hoàn tiền qua ví ──────

    private PhieuTraHang phieuDaCongViQua(Integer donHangId) {
        KhachHang kh = new KhachHang();
        kh.setKhachHangId(1);
        kh.setSoDuVi(BigDecimal.valueOf(100_000));

        DonHang donHang = new DonHang();
        donHang.setId(donHangId);
        donHang.setKhachHang(kh);

        PhieuTraHang phieu = new PhieuTraHang();
        phieu.setPhieuTraId(5);
        phieu.setDonHang(donHang);
        phieu.setTrangThai("da_xu_ly");
        phieu.setHinhThucHoan("vi");
        phieu.setSoTienHoan(BigDecimal.valueOf(50_000));
        phieu.setLyDo("Hàng lỗi");
        phieu.setGhiChu("—");

        when(phieuTraHangRepository.findById(5)).thenReturn(Optional.of(phieu));
        return phieu;
    }

    @Test
    void update_daCongViQua_doiTrangThai_nemLoi() {
        phieuDaCongViQua(9);

        PhieuTraHangRequest req = requestDaXuLyQuaVi(9, BigDecimal.valueOf(50_000));
        req.setTrangThai("tu_choi");

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.update(5, req));

        verify(phieuTraHangRepository, never()).save(any());
        verify(khachHangRepository, never()).save(any());
    }

    @Test
    void update_daCongViQua_doiSoTienHoan_nemLoi() {
        phieuDaCongViQua(9);

        PhieuTraHangRequest req = requestDaXuLyQuaVi(9, BigDecimal.valueOf(70_000));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.update(5, req));

        verify(phieuTraHangRepository, never()).save(any());
        verify(khachHangRepository, never()).save(any());
    }

    @Test
    void update_daCongViQua_suaGhiChu_thanhCong_khongCongViLanNua() {
        PhieuTraHang phieu = phieuDaCongViQua(9);
        KhachHang kh = phieu.getDonHang().getKhachHang();

        when(donHangRepository.getReferenceById(9)).thenReturn(phieu.getDonHang());
        when(phieuTraHangRepository.save(phieu)).thenReturn(phieu);

        PhieuTraHangRequest req = requestDaXuLyQuaVi(9, BigDecimal.valueOf(50_000));
        req.setGhiChu("Ghi chú mới");

        service.update(5, req);

        assertThat(phieu.getGhiChu()).isEqualTo("Ghi chú mới");
        assertThat(kh.getSoDuVi()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
        verify(khachHangRepository, never()).save(any());
    }
}
