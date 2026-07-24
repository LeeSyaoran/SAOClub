package com.example.backend.service;

import com.example.backend.entity.BienTheSanPham;
import com.example.backend.entity.ChiTietDonHang;
import com.example.backend.entity.ChiTietTraHang;
import com.example.backend.entity.DonHang;
import com.example.backend.entity.PhieuTraHang;
import com.example.backend.repository.BienTheSanPhamRepository;
import com.example.backend.repository.ChiTietDonHangRepository;
import com.example.backend.repository.ChiTietSanPhamRepository;
import com.example.backend.repository.ChiTietTraHangRepository;
import com.example.backend.repository.PhieuTraHangRepository;
import com.example.backend.request.ChiTietTraHangRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChiTietTraHangServiceTest {

    @Mock private ChiTietTraHangRepository chiTietTraHangRepository;
    @Mock private PhieuTraHangRepository phieuTraHangRepository;
    @Mock private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Mock private ChiTietSanPhamRepository chiTietSanPhamRepository;
    @Mock private ChiTietDonHangRepository chiTietDonHangRepository;

    @InjectMocks
    private ChiTietTraHangService service;

    private DonHang donHangDaMua2(Integer donHangId, Integer bienTheId) {
        BienTheSanPham bienThe = new BienTheSanPham();
        bienThe.setBienTheId(bienTheId);

        DonHang donHang = new DonHang();
        donHang.setId(donHangId);

        ChiTietDonHang dong = new ChiTietDonHang();
        dong.setBienThe(bienThe);
        dong.setSoLuong(2);
        when(chiTietDonHangRepository.findEntityByDonHangId(donHangId)).thenReturn(List.of(dong));
        return donHang;
    }

    private PhieuTraHang phieuChoXuLy(Integer phieuTraId, DonHang donHang) {
        PhieuTraHang phieu = new PhieuTraHang();
        phieu.setPhieuTraId(phieuTraId);
        phieu.setDonHang(donHang);
        phieu.setTrangThai("cho_xu_ly");
        when(phieuTraHangRepository.findById(phieuTraId)).thenReturn(Optional.of(phieu));
        return phieu;
    }

    @Test
    void create_soLuongTraVuotSoDaMua_congDonCacDongKhac_biChan() {
        DonHang donHang = donHangDaMua2(1, 10);
        PhieuTraHang phieu = phieuChoXuLy(5, donHang);
        when(phieuTraHangRepository.findByDonHang_Id(1)).thenReturn(List.of(phieu));

        // Đã trả 1 (dòng khác, thuộc cùng phiếu) trước đó rồi.
        ChiTietTraHang dongDaTra = new ChiTietTraHang();
        dongDaTra.setId(100);
        BienTheSanPham bienThe = new BienTheSanPham();
        bienThe.setBienTheId(10);
        dongDaTra.setBienThe(bienThe);
        dongDaTra.setSoLuong(1);
        when(chiTietTraHangRepository.findByPhieuTraHang_PhieuTraId(5)).thenReturn(List.of(dongDaTra));

        // Đã mua 2, đã trả 1, giờ trả thêm 2 -> tổng 3 > 2 đã mua -> phải chặn.
        ChiTietTraHangRequest request = new ChiTietTraHangRequest(5, 10, null, 2, BigDecimal.TEN, "tot");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vượt quá số đã mua");
    }

    @Test
    void create_soLuongTraTrongHan_thanhCong() {
        DonHang donHang = donHangDaMua2(1, 10);
        phieuChoXuLy(5, donHang);
        when(phieuTraHangRepository.findByDonHang_Id(1)).thenReturn(List.of());
        when(bienTheSanPhamRepository.getReferenceById(10)).thenReturn(new BienTheSanPham());
        when(chiTietTraHangRepository.save(org.mockito.ArgumentMatchers.any(ChiTietTraHang.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ChiTietTraHangRequest request = new ChiTietTraHangRequest(5, 10, null, 2, BigDecimal.TEN, "tot");

        ChiTietTraHang saved = service.create(request);

        assertThat(saved.getSoLuong()).isEqualTo(2);
    }
}
