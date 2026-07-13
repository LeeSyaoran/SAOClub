package com.example.backend.service;

import com.example.backend.entity.BienTheSanPham;
import com.example.backend.entity.ChiTietDonHang;
import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.entity.DonHang;
import com.example.backend.repository.*;
import com.example.backend.request.DongGoiLineRequest;
import com.example.backend.request.DongGoiRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DonHangServiceTest {

    @Mock private DonHangRepository donHangRepository;
    @Mock private SseService sseService;
    @Mock private KhachHangRepository khachHangRepository;
    @Mock private NhanVienRepository nhanVienRepository;
    @Mock private KhuyenMaiRepository khuyenMaiRepository;
    @Mock private DiaChiGiaoHangRepository diaChiGiaoHangRepository;
    @Mock private ChiTietDonHangRepository chiTietDonHangRepository;
    @Mock private ThanhToanRepository thanhToanRepository;
    @Mock private LichSuTonKhoRepository lichSuTonKhoRepository;
    @Mock private PhieuTraHangRepository phieuTraHangRepository;
    @Mock private PhieuBaoHanhRepository phieuBaoHanhRepository;
    @Mock private ChiTietSanPhamRepository chiTietSanPhamRepository;
    @Mock private ChiTietDonHangSerialRepository chiTietDonHangSerialRepository;

    @InjectMocks
    private DonHangService service;

    private DonHang donHangOnlineConfirmed() {
        DonHang d = new DonHang();
        d.setId(1);
        d.setKenhBan("online");
        d.setTrangThaiDonHang("confirmed");
        return d;
    }

    @Test
    void dongGoi_khongPhaiDonOnline_biChan() {
        DonHang d = donHangOnlineConfirmed();
        d.setKenhBan("in_store");
        when(donHangRepository.findById(1)).thenReturn(Optional.of(d));

        DongGoiRequest req = new DongGoiRequest(List.of());

        assertThatThrownBy(() -> service.dongGoi(1, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("online");
    }

    @Test
    void dongGoi_saiSoLuongSerial_biChan() {
        DonHang d = donHangOnlineConfirmed();
        when(donHangRepository.findById(1)).thenReturn(Optional.of(d));

        BienTheSanPham bienThe = new BienTheSanPham();
        bienThe.setBienTheId(10);
        ChiTietDonHang item = new ChiTietDonHang();
        item.setId(5);
        item.setDonHang(d);
        item.setBienThe(bienThe);
        item.setSoLuong(2);
        when(chiTietDonHangRepository.findById(5)).thenReturn(Optional.of(item));

        DongGoiRequest req = new DongGoiRequest(List.of(new DongGoiLineRequest(5, List.of(100))));

        assertThatThrownBy(() -> service.dongGoi(1, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 serial");
    }

    @Test
    void dongGoi_hopLe_chotDaBanVaChuyenProcessing() {
        DonHang d = donHangOnlineConfirmed();
        when(donHangRepository.findById(1)).thenReturn(Optional.of(d));

        BienTheSanPham bienThe = new BienTheSanPham();
        bienThe.setBienTheId(10);
        ChiTietDonHang item = new ChiTietDonHang();
        item.setId(5);
        item.setDonHang(d);
        item.setBienThe(bienThe);
        item.setSoLuong(1);
        when(chiTietDonHangRepository.findById(5)).thenReturn(Optional.of(item));
        when(chiTietDonHangSerialRepository.findByChiTietDonHang_Id(5)).thenReturn(List.of());

        ChiTietSanPham serial = new ChiTietSanPham();
        serial.setChiTietId(100);
        serial.setBienThe(bienThe);
        serial.setSoSerial("SN-100");
        serial.setTrangThai("trong_kho");
        when(chiTietSanPhamRepository.findById(100)).thenReturn(Optional.of(serial));

        DongGoiRequest req = new DongGoiRequest(List.of(new DongGoiLineRequest(5, List.of(100))));

        service.dongGoi(1, req);

        assertThat(serial.getTrangThai()).isEqualTo("da_ban");
        assertThat(d.getTrangThaiDonHang()).isEqualTo("processing");
    }

    @Test
    void update_chuyenCancelled_giaiPhongCaSerialTrongBangJoin() {
        DonHang d = new DonHang();
        d.setId(1);
        d.setTrangThaiDonHang("processing");
        when(donHangRepository.findById(1)).thenReturn(Optional.of(d));
        when(khachHangRepository.getReferenceById(1)).thenReturn(new com.example.backend.entity.KhachHang());
        when(donHangRepository.save(d)).thenReturn(d);

        ChiTietSanPham repSerial = new ChiTietSanPham();
        repSerial.setChiTietId(100);
        repSerial.setTrangThai("da_ban");
        ChiTietDonHang item = new ChiTietDonHang();
        item.setId(5);
        item.setChiTietSanPham(repSerial);
        when(chiTietDonHangRepository.findEntityByDonHangId(1)).thenReturn(List.of(item));

        ChiTietSanPham extraSerial = new ChiTietSanPham();
        extraSerial.setChiTietId(101);
        extraSerial.setTrangThai("da_ban");
        com.example.backend.entity.ChiTietDonHangSerial link = new com.example.backend.entity.ChiTietDonHangSerial();
        link.setChiTietSanPham(extraSerial);
        when(chiTietDonHangSerialRepository.findByChiTietDonHang_Id(5)).thenReturn(List.of(link));

        com.example.backend.request.DonHangRequest request = new com.example.backend.request.DonHangRequest();
        request.setKhachHangId(1);
        request.setTrangThaiDonHang("cancelled");

        service.update(1, request);

        assertThat(repSerial.getTrangThai()).isEqualTo("trong_kho");
        assertThat(extraSerial.getTrangThai()).isEqualTo("trong_kho");
    }
}
