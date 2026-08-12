package com.example.backend.service;

import com.example.backend.entity.ChucVu;
import com.example.backend.entity.KhachHang;
import com.example.backend.entity.KhuyenMai;
import com.example.backend.entity.PhieuGiamGiaCaNhan;
import com.example.backend.entity.CauHinhVongQuay;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.CauHinhVongQuayRepository;
import com.example.backend.repository.KhachHangRepository;
import com.example.backend.repository.KhuyenMaiRepository;
import com.example.backend.repository.LichSuQuayRepository;
import com.example.backend.repository.PhieuGiamGiaCaNhanRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.response.KetQuaQuayResponse;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
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
class VongQuayServiceTest {

    @Mock private CauHinhVongQuayRepository cauHinhRepository;
    @Mock private LichSuQuayRepository lichSuQuayRepository;
    @Mock private KhuyenMaiRepository khuyenMaiRepository;
    @Mock private KhachHangRepository khachHangRepository;
    @Mock private PhieuGiamGiaCaNhanRepository phieuGiamGiaCaNhanRepository;
    @Mock private TaiKhoanRepository taiKhoanRepository;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private VongQuayService service;

    @BeforeEach
    void setUp() {
        SecurityContext context = mock(SecurityContext.class);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAsKhachHang(String username, Integer khachHangId, int diemTichLuy) {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(username);
        when(SecurityContextHolder.getContext().getAuthentication()).thenReturn(auth);
        ChucVu chucVu = new ChucVu();
        chucVu.setMaChucVu("khach_hang");
        KhachHang kh = new KhachHang();
        kh.setKhachHangId(khachHangId);
        kh.setDiemTichLuy(diemTichLuy);
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername(username);
        tk.setChucVu(chucVu);
        tk.setKhachHang(kh);
        when(taiKhoanRepository.findByUsername(username)).thenReturn(Optional.of(tk));
        lenient().when(khachHangRepository.findWithLockByKhachHangId(khachHangId)).thenReturn(Optional.of(kh));
    }

    private void loginAsNhanVien(String username) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(username);
        when(SecurityContextHolder.getContext().getAuthentication()).thenReturn(auth);
        ChucVu chucVu = new ChucVu();
        chucVu.setMaChucVu("nhan_vien");
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername(username);
        tk.setChucVu(chucVu);
        tk.setKhachHang(null);
        when(taiKhoanRepository.findByUsername(username)).thenReturn(Optional.of(tk));
    }

    private CauHinhVongQuay cauHinh(int diemMoiLuot, int tyLeTruot) {
        return new CauHinhVongQuay(1, diemMoiLuot, tyLeTruot, LocalDateTime.now());
    }

    private KhuyenMai khuyenMaiActive(Integer id, String loai, long giaTri) {
        KhuyenMai k = new KhuyenMai();
        k.setKhuyenMaiId(id);
        k.setMaKhuyenMai("KM" + id);
        k.setTenKhuyenMai("Khuyến mãi " + id);
        k.setLoai(loai);
        k.setGiaTri(BigDecimal.valueOf(giaTri));
        k.setTrangThai("active");
        k.setNgayTao(LocalDateTime.now());
        return k;
    }

    @Test
    void quay_duDiemVaCoKhuyenMaiActive_tyLeTruot0_trungThuong() {
        loginAsKhachHang("khach1", 42, 1000);
        when(cauHinhRepository.findById(1)).thenReturn(Optional.of(cauHinh(100, 0)));
        when(khuyenMaiRepository.findActiveKhaDung()).thenReturn(List.of(khuyenMaiActive(7, "percent", 20)));
        when(khachHangRepository.save(any(KhachHang.class))).thenAnswer(inv -> inv.getArgument(0));
        when(phieuGiamGiaCaNhanRepository.save(any(PhieuGiamGiaCaNhan.class))).thenAnswer(inv -> inv.getArgument(0));

        KetQuaQuayResponse res = service.quay();

        assertThat(res.getKetQua()).isEqualTo("trung");
        assertThat(res.getKhuyenMai().getKhuyenMaiId()).isEqualTo(7);
        assertThat(res.getDiemConLai()).isEqualTo(900); 
        verify(khachHangRepository).save(argThat(kh -> kh.getDiemTichLuy() == 900));
        verify(phieuGiamGiaCaNhanRepository).save(argThat(p ->
                p.getDoiThuong() == null && "percent".equals(p.getLoai())
                        && p.getGiaTri().compareTo(BigDecimal.valueOf(20)) == 0));
        verify(lichSuQuayRepository).save(argThat(l -> "trung".equals(l.getKetQua()) && l.getDiemDaTru() == 100));
        verify(entityManager).refresh(any(PhieuGiamGiaCaNhan.class));
    }

    @Test
    void quay_khongDuDiem_biChanKhongTruDiem() {
        loginAsKhachHang("khach1", 42, 50);
        when(cauHinhRepository.findById(1)).thenReturn(Optional.of(cauHinh(100, 0)));

        assertThatThrownBy(() -> service.quay())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đủ điểm");
        verify(khachHangRepository, never()).save(any());
        verify(phieuGiamGiaCaNhanRepository, never()).save(any());
        verify(lichSuQuayRepository, never()).save(any());
    }

    @Test
    void quay_tyLeTruot100_luonTruotNhungVanTruDiem() {
        loginAsKhachHang("khach1", 42, 1000);
        when(cauHinhRepository.findById(1)).thenReturn(Optional.of(cauHinh(100, 100)));
        when(khuyenMaiRepository.findActiveKhaDung()).thenReturn(List.of(khuyenMaiActive(7, "percent", 20)));
        when(khachHangRepository.save(any(KhachHang.class))).thenAnswer(inv -> inv.getArgument(0));

        KetQuaQuayResponse res = service.quay();

        assertThat(res.getKetQua()).isEqualTo("truot");
        assertThat(res.getKhuyenMai()).isNull();
        assertThat(res.getDiemConLai()).isEqualTo(900);
        verify(phieuGiamGiaCaNhanRepository, never()).save(any());
        verify(lichSuQuayRepository).save(argThat(l -> "truot".equals(l.getKetQua())));
    }

    @Test
    void quay_danhSachKhuyenMaiRong_luonTruotBatKeTyLeTruot() {
        loginAsKhachHang("khach1", 42, 1000);
        when(cauHinhRepository.findById(1)).thenReturn(Optional.of(cauHinh(100, 0))); 
        when(khuyenMaiRepository.findActiveKhaDung()).thenReturn(List.of()); 
        when(khachHangRepository.save(any(KhachHang.class))).thenAnswer(inv -> inv.getArgument(0));

        KetQuaQuayResponse res = service.quay();

        assertThat(res.getKetQua()).isEqualTo("truot");
        verify(phieuGiamGiaCaNhanRepository, never()).save(any());
    }

    @Test
    void quay_taiKhoanKhongPhaiKhachHang_nemAccessDenied() {
        loginAsNhanVien("nhanvien1");

        assertThatThrownBy(() -> service.quay())
                .isInstanceOf(AccessDeniedException.class);
        verify(khachHangRepository, never()).findWithLockByKhachHangId(any());
    }

    @Test
    void quay_trung_entityManagerRefreshDuocGoiDeReloadMaPhieuTuDB() {
        loginAsKhachHang("khach1", 42, 1000);
        when(cauHinhRepository.findById(1)).thenReturn(Optional.of(cauHinh(100, 0)));
        when(khuyenMaiRepository.findActiveKhaDung()).thenReturn(List.of(khuyenMaiActive(7, "percent", 20)));
        when(khachHangRepository.save(any(KhachHang.class))).thenAnswer(inv -> inv.getArgument(0));
        when(phieuGiamGiaCaNhanRepository.save(any(PhieuGiamGiaCaNhan.class))).thenAnswer(inv -> inv.getArgument(0));

        service.quay();

        verify(entityManager).refresh(any(PhieuGiamGiaCaNhan.class));
    }
}
