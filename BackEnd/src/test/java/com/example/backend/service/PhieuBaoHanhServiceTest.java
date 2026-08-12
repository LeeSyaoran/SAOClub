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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    void create_ngayHetBhSomHonNgayMua_biChan() {
        PhieuBaoHanhRequest req = requestCoBan();
        req.setNgayMua(LocalDateTime.now());
        req.setNgayHetBh(LocalDateTime.now().minusDays(1));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sau ngày mua");
        verify(phieuBaoHanhRepository, never()).save(any());
    }

    @Test
    void create_ngayTraKhachSomHonNgayTiepNhan_biChan() {
        PhieuBaoHanhRequest req = requestCoBan();
        LocalDateTime now = LocalDateTime.now();
        req.setNgayTiepNhan(now);
        req.setNgayTraKhach(now.minusDays(1));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trước ngày tiếp nhận");
        verify(phieuBaoHanhRepository, never()).save(any());
    }


    @Test
    void update_chuyenDangXuLy_ganSerialLoiBaoHanh() {
        PhieuBaoHanh entity = new PhieuBaoHanh();
        entity.setBaoHanhId(1);
        entity.setTrangThai("con_bao_hanh");
        ChiTietSanPham serial = new ChiTietSanPham();
        serial.setChiTietId(100);
        serial.setTrangThai("da_ban");
        entity.setChiTietSanPham(serial);
        when(phieuBaoHanhRepository.findById(1)).thenReturn(java.util.Optional.of(entity));
        when(phieuBaoHanhRepository.save(entity)).thenReturn(entity);
        when(chiTietSanPhamRepository.getReferenceById(100)).thenReturn(serial);

        PhieuBaoHanhRequest req = requestCoBan();
        req.setChiTietId(100);
        req.setTrangThai("dang_xu_ly");

        service.update(1, req);

        assertThat(serial.getTrangThai()).isEqualTo("loi_bao_hanh");
        verify(chiTietSanPhamRepository).save(serial);
    }

    @Test
    void update_chuyenDaXuLy_traSerialVeDaBan() {
        PhieuBaoHanh entity = new PhieuBaoHanh();
        entity.setBaoHanhId(1);
        entity.setTrangThai("dang_xu_ly");
        ChiTietSanPham serial = new ChiTietSanPham();
        serial.setChiTietId(100);
        serial.setTrangThai("loi_bao_hanh");
        entity.setChiTietSanPham(serial);
        when(phieuBaoHanhRepository.findById(1)).thenReturn(java.util.Optional.of(entity));
        when(phieuBaoHanhRepository.save(entity)).thenReturn(entity);
        when(chiTietSanPhamRepository.getReferenceById(100)).thenReturn(serial);

        PhieuBaoHanhRequest req = requestCoBan();
        req.setChiTietId(100);
        req.setTrangThai("da_xu_ly");
        req.setKetQuaXuLy("Đã sửa xong, thay bàn phím");

        service.update(1, req);

        assertThat(serial.getTrangThai()).isEqualTo("da_ban");
        verify(chiTietSanPhamRepository).save(serial);
    }

    @Test
    void update_vanConBaoHanh_khongDungToiSerial() {
        PhieuBaoHanh entity = new PhieuBaoHanh();
        entity.setBaoHanhId(1);
        entity.setTrangThai("con_bao_hanh");
        ChiTietSanPham serial = new ChiTietSanPham();
        serial.setChiTietId(100);
        serial.setTrangThai("da_ban");
        entity.setChiTietSanPham(serial);
        when(phieuBaoHanhRepository.findById(1)).thenReturn(java.util.Optional.of(entity));
        when(phieuBaoHanhRepository.save(entity)).thenReturn(entity);
        when(chiTietSanPhamRepository.getReferenceById(100)).thenReturn(serial);

        PhieuBaoHanhRequest req = requestCoBan();
        req.setChiTietId(100);
        req.setGhiChu("Cập nhật ghi chú thôi");
        req.setTrangThai("con_bao_hanh");

        service.update(1, req);

        assertThat(serial.getTrangThai()).isEqualTo("da_ban");
        verify(chiTietSanPhamRepository, never()).save(any());
    }
}
