package com.example.backend.service;

import com.example.backend.repository.*;
import com.example.backend.response.CustomerSpendingResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private SanPhamRepository sanPhamRepository;
    @Mock private DonHangRepository donHangRepository;
    @Mock private KhachHangRepository khachHangRepository;
    @Mock private TonKhoRepository tonKhoRepository;

    @InjectMocks
    private DashboardService service;

    @Test
    void getTopSelling_khongTruyenNgay_goiRepoVoiNullGiuHanhViCu() {
        service.getTopSelling(5, null, null);
        verify(sanPhamRepository).topSelling(isNull(), isNull(), any());
    }

    @Test
    void getTopSelling_coTruyenNgay_goiRepoDungThamSo() {
        LocalDateTime tu = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime den = LocalDateTime.of(2026, 1, 31, 23, 59, 59);
        service.getTopSelling(5, tu, den);
        verify(sanPhamRepository).topSelling(eq(tu), eq(den), any());
    }

    @Test
    void getCustomerReport_danhSachRong_tyLeMuaLaiBang0KhongChiaCho0() {
        when(khachHangRepository.chiTieuTheoKhachHang(any(), any(), any())).thenReturn(List.of());

        var result = service.getCustomerReport(LocalDateTime.now().minusDays(7), LocalDateTime.now(), 5);

        assertThat(result.getTyLeMuaLai()).isEqualTo(0.0);
        assertThat(result.getTongSoKhach()).isEqualTo(0);
        assertThat(result.getTopKhach()).isEmpty();
    }

    @Test
    void getCustomerReport_tinhDungTyLeKhachMuaTuHaiDonTroLen() {
        List<CustomerSpendingResponse> all = List.of(
                new CustomerSpendingResponse(1, "A", 3L, BigDecimal.valueOf(1000)),
                new CustomerSpendingResponse(2, "B", 1L, BigDecimal.valueOf(500)),
                new CustomerSpendingResponse(3, "C", 2L, BigDecimal.valueOf(800)),
                new CustomerSpendingResponse(4, "D", 1L, BigDecimal.valueOf(200))
        );
        when(khachHangRepository.chiTieuTheoKhachHang(any(), any(), any())).thenReturn(all);

        var result = service.getCustomerReport(LocalDateTime.now().minusDays(7), LocalDateTime.now(), 2);

        assertThat(result.getTyLeMuaLai()).isEqualTo(0.5);
        assertThat(result.getTongSoKhach()).isEqualTo(4);
        assertThat(result.getTopKhach()).hasSize(2);
        assertThat(result.getTopKhach().get(0).getHoTen()).isEqualTo("A");
    }
}
