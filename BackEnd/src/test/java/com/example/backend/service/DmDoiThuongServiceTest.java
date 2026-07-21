package com.example.backend.service;

import com.example.backend.entity.DmDoiThuong;
import com.example.backend.repository.DmDoiThuongRepository;
import com.example.backend.request.DmDoiThuongRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DmDoiThuongServiceTest {

    @Mock private DmDoiThuongRepository dmDoiThuongRepository;

    @InjectMocks
    private DmDoiThuongService service;

    @Test
    void create_khoiTaoNgayTao() {
        when(dmDoiThuongRepository.save(org.mockito.ArgumentMatchers.any(DmDoiThuong.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DmDoiThuongRequest req = new DmDoiThuongRequest("Giảm 50k", "Mô tả", 500, "fixed", BigDecimal.valueOf(50_000), null, "active");
        DmDoiThuong saved = service.create(req);

        assertThat(saved.getNgayTao()).isNotNull();
        assertThat(saved.getDiemCan()).isEqualTo(500);
    }

    @Test
    void update_khongDoiNgayTao() {
        DmDoiThuong existing = new DmDoiThuong();
        existing.setDoiThuongId(1);
        existing.setNgayTao(java.time.LocalDateTime.of(2026, 1, 1, 0, 0));
        when(dmDoiThuongRepository.findById(1)).thenReturn(Optional.of(existing));
        when(dmDoiThuongRepository.save(existing)).thenReturn(existing);

        DmDoiThuongRequest req = new DmDoiThuongRequest("Giảm 70k", null, 700, "fixed", BigDecimal.valueOf(70_000), null, "active");
        service.update(1, req);

        assertThat(existing.getNgayTao()).isEqualTo(java.time.LocalDateTime.of(2026, 1, 1, 0, 0));
        assertThat(existing.getDiemCan()).isEqualTo(700);
    }
}
