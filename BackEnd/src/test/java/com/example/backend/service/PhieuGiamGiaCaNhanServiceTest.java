package com.example.backend.service;

import com.example.backend.entity.ChucVu;
import com.example.backend.entity.DmDoiThuong;
import com.example.backend.entity.KhachHang;
import com.example.backend.entity.PhieuGiamGiaCaNhan;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.DmDoiThuongRepository;
import com.example.backend.repository.KhachHangRepository;
import com.example.backend.repository.LichSuQuayRepository;
import com.example.backend.repository.PhieuGiamGiaCaNhanRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.request.TangVoucherRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhieuGiamGiaCaNhanServiceTest {

    @Mock private PhieuGiamGiaCaNhanRepository phieuGiamGiaCaNhanRepository;
    @Mock private DmDoiThuongRepository dmDoiThuongRepository;
    @Mock private KhachHangRepository khachHangRepository;
    @Mock private TaiKhoanRepository taiKhoanRepository;
    @Mock private LichSuQuayRepository lichSuQuayRepository;

    @InjectMocks
    private PhieuGiamGiaCaNhanService service;

    @BeforeEach
    void setUp() {
        SecurityContext context = mock(SecurityContext.class);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String username, Integer khachHangId) {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(username);
        when(SecurityContextHolder.getContext().getAuthentication()).thenReturn(auth);
        ChucVu chucVu = new ChucVu();
        chucVu.setMaChucVu("khach_hang");
        KhachHang kh = new KhachHang();
        kh.setKhachHangId(khachHangId);
        kh.setDiemTichLuy(1000);
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername(username);
        tk.setChucVu(chucVu);
        tk.setKhachHang(kh);
        when(taiKhoanRepository.findByUsername(username)).thenReturn(Optional.of(tk));
        lenient().when(khachHangRepository.findWithLockByKhachHangId(khachHangId)).thenReturn(Optional.of(kh));
    }

    private DmDoiThuong doiThuongActive(Integer id, Integer diemCan) {
        DmDoiThuong dt = new DmDoiThuong();
        dt.setDoiThuongId(id);
        dt.setDiemCan(diemCan);
        dt.setLoai("fixed");
        dt.setGiaTri(java.math.BigDecimal.valueOf(50_000));
        dt.setTrangThai("active");
        return dt;
    }

    @Test
    void doiThuong_duDiem_truDiemVaTaoPhieu() {
        loginAs("khach1", 42);
        when(dmDoiThuongRepository.findById(5)).thenReturn(Optional.of(doiThuongActive(5, 500)));
        when(khachHangRepository.save(any(KhachHang.class))).thenAnswer(inv -> inv.getArgument(0));
        when(phieuGiamGiaCaNhanRepository.save(any(PhieuGiamGiaCaNhan.class))).thenAnswer(inv -> inv.getArgument(0));

        PhieuGiamGiaCaNhan saved = service.doiThuong(5);

        assertThat(saved.getKhachHang().getDiemTichLuy()).isEqualTo(500); // 1000 - 500
        assertThat(saved.getDaSuDung()).isFalse();
        assertThat(saved.getLoai()).isEqualTo("fixed");
        assertThat(saved.getGiaTri()).isEqualByComparingTo(java.math.BigDecimal.valueOf(50_000));
        verify(khachHangRepository).findWithLockByKhachHangId(42);
        verify(khachHangRepository).save(any(KhachHang.class));
    }

    @Test
    void doiThuong_khongDuDiem_biChan() {
        loginAs("khach1", 42);
        when(dmDoiThuongRepository.findById(5)).thenReturn(Optional.of(doiThuongActive(5, 5000)));

        assertThatThrownBy(() -> service.doiThuong(5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đủ điểm");
        verify(phieuGiamGiaCaNhanRepository, never()).save(any());
    }

    @Test
    void taoVoucherAdmin_tao_thanhCong() {
        KhachHang khachHang = new KhachHang();
        khachHang.setKhachHangId(5);
        when(khachHangRepository.findById(5)).thenReturn(Optional.of(khachHang));
        when(phieuGiamGiaCaNhanRepository.save(any(PhieuGiamGiaCaNhan.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TangVoucherRequest request = new TangVoucherRequest(
                "percent", BigDecimal.valueOf(10), BigDecimal.valueOf(500000),
                LocalDateTime.now().plusDays(30), null);

        PhieuGiamGiaCaNhan result = service.taoVoucherAdmin(5, request);

        assertThat(result.getKhachHang()).isEqualTo(khachHang);
        assertThat(result.getDoiThuong()).isNull();
        assertThat(result.getDaSuDung()).isFalse();
        assertThat(result.getLoai()).isEqualTo("percent");
    }

    @Test
    void taoVoucherAdmin_phanTramVuot100_nemLoi() {
        TangVoucherRequest request = new TangVoucherRequest(
                "percent", BigDecimal.valueOf(150), null, LocalDateTime.now().plusDays(30), null);

        assertThatThrownBy(() -> service.taoVoucherAdmin(5, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không được vượt quá 100%");
    }

    @Test
    void taoVoucherAdmin_hanSuDungDaQua_nemLoi() {
        TangVoucherRequest request = new TangVoucherRequest(
                "fixed", BigDecimal.valueOf(50000), null, LocalDateTime.now().minusDays(1), null);

        assertThatThrownBy(() -> service.taoVoucherAdmin(5, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tương lai");
    }

    @Test
    void getByKhachHangIdForAdmin_ganNhanNguonDung() {
        PhieuGiamGiaCaNhan tuDoiThuong = new PhieuGiamGiaCaNhan();
        tuDoiThuong.setPhieuId(1);
        tuDoiThuong.setDoiThuong(new DmDoiThuong());
        tuDoiThuong.setLoai("fixed");
        tuDoiThuong.setGiaTri(BigDecimal.TEN);
        tuDoiThuong.setDaSuDung(false);
        tuDoiThuong.setNgayDoi(LocalDateTime.now());
        tuDoiThuong.setNgayHetHan(LocalDateTime.now().plusDays(10));

        PhieuGiamGiaCaNhan adminTang = new PhieuGiamGiaCaNhan();
        adminTang.setPhieuId(2);
        adminTang.setLoai("percent");
        adminTang.setGiaTri(BigDecimal.TEN);
        adminTang.setDaSuDung(false);
        adminTang.setNgayDoi(LocalDateTime.now());
        adminTang.setNgayHetHan(LocalDateTime.now().plusDays(10));

        when(phieuGiamGiaCaNhanRepository.findByKhachHang_KhachHangId(5))
                .thenReturn(List.of(tuDoiThuong, adminTang));
        when(lichSuQuayRepository.findPhieuIdsByKhachHangId(5)).thenReturn(List.of());

        var result = service.getByKhachHangIdForAdmin(5);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getNguon()).isEqualTo("Khách tự đổi / trúng thưởng");
        assertThat(result.get(1).getNguon()).isEqualTo("Admin tặng");
    }
}
