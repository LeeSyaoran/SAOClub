package com.example.backend.service;

import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.repository.ChiTietSanPhamRepository;
import com.example.backend.request.SerialLockRequest;
import com.example.backend.request.SerialUnlockRequest;
import com.example.backend.response.SerialLockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChiTietSanPhamServiceLockTest {

    @Mock
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    @InjectMocks
    private ChiTietSanPhamService service;

    private ChiTietSanPham serialInKho;

    @BeforeEach
    void setUp() {
        serialInKho = new ChiTietSanPham();
        serialInKho.setChiTietId(1);
        serialInKho.setTrangThai("trong_kho");
        serialInKho.setSoSerial("SN001");
        serialInKho.setLockedBy(null);
        serialInKho.setLockedAt(null);
        serialInKho.setLockSession(null);
    }

    // ========== lockSerials tests ==========

    @Test
    void lockSerials_shouldLockAvailableSerial_whenSerialIsInKho() {
        SerialLockRequest request = new SerialLockRequest();
        request.setChiTietIds(List.of(1));
        request.setSessionId("session-1");
        request.setNhanVienId(5);

        when(chiTietSanPhamRepository.lockSerials(
            eq(List.of(1)), eq(5), any(LocalDateTime.class), eq("session-1"), any(LocalDateTime.class)
        )).thenReturn(1);

        SerialLockResponse response = service.lockSerials(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getLockedCount()).isEqualTo(1);
        assertThat(response.getFailedIds()).isEmpty();
    }

    @Test
    void lockSerials_shouldFail_whenSerialAlreadyLockedByOther() {
        SerialLockRequest request = new SerialLockRequest();
        request.setChiTietIds(List.of(1));
        request.setSessionId("session-2");
        request.setNhanVienId(6);

        // lockSerials return 0 vì serial đang bị lock bởi session-1
        when(chiTietSanPhamRepository.lockSerials(
            anyList(), anyInt(), any(LocalDateTime.class), anyString(), any(LocalDateTime.class)
        )).thenReturn(0);

        // findById trả serial đang bị lock
        ChiTietSanPham lockedSerial = new ChiTietSanPham();
        lockedSerial.setChiTietId(1);
        lockedSerial.setTrangThai("trong_kho");
        lockedSerial.setLockedBy(5);
        lockedSerial.setLockedAt(LocalDateTime.now());
        lockedSerial.setLockSession("session-1");

        when(chiTietSanPhamRepository.findById(1)).thenReturn(Optional.of(lockedSerial));

        SerialLockResponse response = service.lockSerials(request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getFailedIds()).contains(1);
    }

    @Test
    void lockSerials_shouldFail_whenSerialNotInKho() {
        SerialLockRequest request = new SerialLockRequest();
        request.setChiTietIds(List.of(1));
        request.setSessionId("session-1");
        request.setNhanVienId(5);

        when(chiTietSanPhamRepository.lockSerials(
            anyList(), anyInt(), any(LocalDateTime.class), anyString(), any(LocalDateTime.class)
        )).thenReturn(0);

        ChiTietSanPham soldSerial = new ChiTietSanPham();
        soldSerial.setChiTietId(1);
        soldSerial.setTrangThai("da_ban"); // đã bán, không lock được
        when(chiTietSanPhamRepository.findById(1)).thenReturn(Optional.of(soldSerial));

        SerialLockResponse response = service.lockSerials(request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getFailedIds()).contains(1);
    }

    @Test
    void lockSerials_shouldSucceedPartially_whenSomeLocked() {
        SerialLockRequest request = new SerialLockRequest();
        request.setChiTietIds(List.of(1, 2));
        request.setSessionId("session-1");
        request.setNhanVienId(5);

        // 1 serial lock thành công
        when(chiTietSanPhamRepository.lockSerials(
            anyList(), anyInt(), any(LocalDateTime.class), anyString(), any(LocalDateTime.class)
        )).thenReturn(1);

        // Serial 2 đang bị lock bởi người khác
        ChiTietSanPham lockedSerial = new ChiTietSanPham();
        lockedSerial.setChiTietId(2);
        lockedSerial.setTrangThai("trong_kho");
        lockedSerial.setLockedBy(99);
        lockedSerial.setLockedAt(LocalDateTime.now());
        lockedSerial.setLockSession("other-session");

        when(chiTietSanPhamRepository.findById(1)).thenReturn(Optional.of(serialInKho));
        when(chiTietSanPhamRepository.findById(2)).thenReturn(Optional.of(lockedSerial));

        SerialLockResponse response = service.lockSerials(request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getLockedCount()).isEqualTo(1);
        assertThat(response.getFailedIds()).contains(2);
    }

    // ========== unlockSerials tests ==========

    @Test
    void unlockSerials_shouldUnlockOwnSession() {
        SerialUnlockRequest request = new SerialUnlockRequest();
        request.setChiTietIds(List.of(1));
        request.setSessionId("session-1");

        when(chiTietSanPhamRepository.unlockSerials(List.of(1), "session-1")).thenReturn(1);

        int unlocked = service.unlockSerials(request);

        assertThat(unlocked).isEqualTo(1);
        verify(chiTietSanPhamRepository).unlockSerials(List.of(1), "session-1");
    }

    @Test
    void unlockSerials_shouldReturnZero_whenNoMatchingSession() {
        SerialUnlockRequest request = new SerialUnlockRequest();
        request.setChiTietIds(List.of(1));
        request.setSessionId("wrong-session");

        when(chiTietSanPhamRepository.unlockSerials(List.of(1), "wrong-session")).thenReturn(0);

        int unlocked = service.unlockSerials(request);

        assertThat(unlocked).isZero();
    }

    @Test
    void unlockSerials_shouldOnlyUnlockSpecifiedIds() {
        SerialUnlockRequest request = new SerialUnlockRequest();
        request.setChiTietIds(List.of(1, 2, 3));
        request.setSessionId("session-1");

        when(chiTietSanPhamRepository.unlockSerials(List.of(1, 2, 3), "session-1")).thenReturn(2);

        int unlocked = service.unlockSerials(request);

        assertThat(unlocked).isEqualTo(2);
        verify(chiTietSanPhamRepository).unlockSerials(List.of(1, 2, 3), "session-1");
    }
}
