package com.example.backend.service;

import com.example.backend.entity.DonHang;
import com.example.backend.entity.ThanhToan;
import com.example.backend.repository.DonHangRepository;
import com.example.backend.repository.ThanhToanRepository;
import com.example.backend.request.ThanhToanRequest;
import com.example.backend.response.ThanhToanResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThanhToanServiceTest {

    @Mock private ThanhToanRepository thanhToanRepository;
    @Mock private DonHangRepository donHangRepository;

    @InjectMocks
    private ThanhToanService service;

    // Thanh toan tien mat tai quay khong co "ma giao dich" — dung use-case chinh khien
    // validate cu (bat buoc maGiaoDich/ghiChu) sai, xem Task 1 Step 1.
    @Test
    void create_khongCoMaGiaoDichVaGhiChu_vanThanhCong() {
        DonHang donHang = new DonHang();
        when(donHangRepository.getReferenceById(1)).thenReturn(donHang);
        when(thanhToanRepository.save(any(ThanhToan.class))).thenAnswer(inv -> inv.getArgument(0));

        ThanhToanRequest req = new ThanhToanRequest();
        req.setDonHangId(1);
        req.setNgayThanhToan(LocalDateTime.now());
        req.setPhuongThucThanhToan("tien_mat");
        req.setSoTien(BigDecimal.valueOf(500000));
        req.setMaGiaoDich(null);
        req.setTrangThai("success");
        req.setGhiChu(null);

        ThanhToan saved = service.create(req);

        assertThat(saved.getMaGiaoDich()).isNull();
        assertThat(saved.getGhiChu()).isNull();
        assertThat(saved.getPhuongThucThanhToan()).isEqualTo("tien_mat");
        assertThat(saved.getDonHang()).isSameAs(donHang);
    }

    @Test
    void hienThiThanhToanTheoDonHang_goiDungRepository() {
        List<ThanhToanResponse> expected = List.of(
                new ThanhToanResponse(1, 5, LocalDateTime.now(), "tien_mat", BigDecimal.TEN, null, "success", null));
        when(thanhToanRepository.hienThiThanhToanTheoDonHang(5)).thenReturn(expected);

        List<ThanhToanResponse> result = service.hienThiThanhToanTheoDonHang(5);

        assertThat(result).isEqualTo(expected);
    }
}
