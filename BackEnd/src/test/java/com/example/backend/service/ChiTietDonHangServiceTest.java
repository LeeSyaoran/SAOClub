package com.example.backend.service;

import com.example.backend.entity.BienTheSanPham;
import com.example.backend.entity.ChiTietDonHang;
import com.example.backend.entity.ChiTietDonHangSerial;
import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.entity.DonHang;
import com.example.backend.repository.*;
import com.example.backend.request.ChiTietDonHangRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChiTietDonHangServiceTest {

    @Mock private ChiTietDonHangRepository chiTietDonHangRepository;
    @Mock private DonHangRepository donHangRepository;
    @Mock private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Mock private ChiTietSanPhamRepository chiTietSanPhamRepository;
    @Mock private LichSuTonKhoRepository lichSuTonKhoRepository;
    @Mock private ChiTietDonHangSerialRepository chiTietDonHangSerialRepository;

    @InjectMocks
    private ChiTietDonHangService service;

    private ChiTietSanPham serialTrongKho(Integer id, BienTheSanPham bienThe) {
        ChiTietSanPham s = new ChiTietSanPham();
        s.setChiTietId(id);
        s.setBienThe(bienThe);
        s.setSoSerial("SN-" + id);
        s.setTrangThai("trong_kho");
        return s;
    }

    @Test
    void create_donOnline_giuChoKhongDanhDauDaBan() {
        DonHang donHang = new DonHang();
        donHang.setId(1);
        donHang.setKenhBan("online");
        when(donHangRepository.getReferenceById(1)).thenReturn(donHang);

        BienTheSanPham bienThe = new BienTheSanPham();
        bienThe.setBienTheId(10);
        when(bienTheSanPhamRepository.getReferenceById(10)).thenReturn(bienThe);

        ChiTietSanPham s1 = serialTrongKho(100, bienThe);
        ChiTietSanPham s2 = serialTrongKho(101, bienThe);
        when(chiTietSanPhamRepository.findByBienThe_BienTheIdAndTrangThaiOrderByNgayNhapKhoAsc(10, "trong_kho"))
                .thenReturn(List.of(s1, s2));
        when(chiTietDonHangRepository.save(any(ChiTietDonHang.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ChiTietDonHangRequest request = new ChiTietDonHangRequest(1, 10, null, 2, BigDecimal.TEN, BigDecimal.ZERO, null);

        service.create(request);

        assertThat(s1.getTrangThai()).isEqualTo("giu_hang");
        assertThat(s2.getTrangThai()).isEqualTo("giu_hang");
        verify(chiTietDonHangSerialRepository, times(2)).save(any(ChiTietDonHangSerial.class));
    }

    @Test
    void create_donTaiQuay_danhDauDaBanNgay() {
        DonHang donHang = new DonHang();
        donHang.setId(2);
        donHang.setKenhBan("in_store");
        when(donHangRepository.getReferenceById(2)).thenReturn(donHang);

        BienTheSanPham bienThe = new BienTheSanPham();
        bienThe.setBienTheId(20);
        when(bienTheSanPhamRepository.getReferenceById(20)).thenReturn(bienThe);

        ChiTietSanPham s1 = serialTrongKho(200, bienThe);
        when(chiTietSanPhamRepository.findByBienThe_BienTheIdAndTrangThaiOrderByNgayNhapKhoAsc(20, "trong_kho"))
                .thenReturn(List.of(s1));
        when(chiTietDonHangRepository.save(any(ChiTietDonHang.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ChiTietDonHangRequest request = new ChiTietDonHangRequest(2, 20, null, 1, BigDecimal.TEN, BigDecimal.ZERO, null);

        service.create(request);

        assertThat(s1.getTrangThai()).isEqualTo("da_ban");
        verify(chiTietDonHangSerialRepository, times(1)).save(any(ChiTietDonHangSerial.class));
    }
}
