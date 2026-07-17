package com.example.backend.service;

import com.example.backend.entity.CaiDatHeThong;
import com.example.backend.repository.CaiDatHeThongRepository;
import com.example.backend.repository.TonKhoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaiDatHeThongServiceTest {

    @Mock private CaiDatHeThongRepository caiDatHeThongRepository;
    @Mock private TonKhoRepository tonKhoRepository;

    @InjectMocks
    private CaiDatHeThongService service;

    @Test
    void apDungNguongTonKhoChoTatCa_luuNguongMoiVaGoiBulkUpdate() {
        CaiDatHeThong c = new CaiDatHeThong();
        c.setCaiDatId(1);
        when(caiDatHeThongRepository.findById(1)).thenReturn(Optional.of(c));
        when(tonKhoRepository.capNhatNguongChoTatCa(10)).thenReturn(7);

        int result = service.apDungNguongTonKhoChoTatCa(10);

        assertThat(result).isEqualTo(7);
        assertThat(c.getNguongTonKhoMacDinh()).isEqualTo(10);
        verify(caiDatHeThongRepository).save(c);
        verify(tonKhoRepository).capNhatNguongChoTatCa(10);
    }
}
