package com.example.backend.service;

import com.example.backend.repository.LichSuDonHangRepository;
import com.example.backend.response.LichSuDonHangResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LichSuDonHangServiceTest {

    @Mock private LichSuDonHangRepository lichSuDonHangRepository;

    @InjectMocks
    private LichSuDonHangService service;

    @Test
    void getByDonHang_traVeDungThuTuTheoRepository() {
        LichSuDonHangResponse r1 = new LichSuDonHangResponse(1, 5, null, "pending", LocalDateTime.now());
        LichSuDonHangResponse r2 = new LichSuDonHangResponse(2, 5, "pending", "confirmed", LocalDateTime.now());
        when(lichSuDonHangRepository.getByDonHangId(5)).thenReturn(List.of(r1, r2));

        List<LichSuDonHangResponse> result = service.getByDonHang(5);

        assertThat(result).containsExactly(r1, r2);
    }
}
