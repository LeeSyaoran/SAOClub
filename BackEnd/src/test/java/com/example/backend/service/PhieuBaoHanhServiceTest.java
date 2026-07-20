package com.example.backend.service;

import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.entity.PhieuBaoHanh;
import com.example.backend.repository.*;
import com.example.backend.request.PhieuBaoHanhRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhieuBaoHanhServiceTest {

    @Mock private PhieuBaoHanhRepository phieuBaoHanhRepository;
    @Mock private DonHangRepository donHangRepository;
    @Mock private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Mock private KhachHangRepository khachHangRepository;
    @Mock private ChiTietSanPhamRepository chiTietSanPhamRepository;

    @InjectMocks
    private PhieuBaoHanhService service;

    private PhieuBaoHanhRequest requestCoBan() {
        PhieuBaoHanhRequest r = new PhieuBaoHanhRequest();
        r.setDonHangId(1);
        r.setBienTheId(2);
        r.setKhachHangId(3);
        r.setNgayMua(LocalDateTime.now());
        r.setNgayHetBh(LocalDateTime.now().plusMonths(12));
        r.setMoTaLoi("Máy không lên nguồn");
        r.setTrangThai("con_bao_hanh");
        r.setChiPhiPhatSinh(BigDecimal.ZERO);
        r.setGhiChu("—");
        return r;
    }

    @Test
    void create_coChiTietId_ganChiTietSanPham() {
        PhieuBaoHanhRequest req = requestCoBan();
        req.setChiTietId(100);
        ChiTietSanPham serialMock = new ChiTietSanPham();
        serialMock.setChiTietId(100);
        when(chiTietSanPhamRepository.getReferenceById(100)).thenReturn(serialMock);
        when(phieuBaoHanhRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PhieuBaoHanh saved = service.create(req);

        assertThat(saved.getChiTietSanPham()).isSameAs(serialMock);
    }

    @Test
    void create_khongCoChiTietId_khongGanChiTietSanPham() {
        PhieuBaoHanhRequest req = requestCoBan();
        req.setChiTietId(null);
        when(phieuBaoHanhRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PhieuBaoHanh saved = service.create(req);

        verify(chiTietSanPhamRepository, never()).getReferenceById(any());
        assertThat(saved.getChiTietSanPham()).isNull();
    }

    @Test
    void create_ganDungBienTheTuBienTheId_khongPhaiSanPhamId() {
        PhieuBaoHanhRequest req = requestCoBan();
        req.setBienTheId(42);
        when(phieuBaoHanhRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(req);

        verify(bienTheSanPhamRepository).getReferenceById(42);
    }
}
