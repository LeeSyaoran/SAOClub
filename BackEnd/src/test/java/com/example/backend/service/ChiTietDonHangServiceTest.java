package com.example.backend.service;

import com.example.backend.entity.BienTheSanPham;
import com.example.backend.entity.ChiTietDonHang;
import com.example.backend.entity.ChiTietDonHangSerial;
import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.entity.ChucVu;
import com.example.backend.entity.DonHang;
import com.example.backend.entity.KhachHang;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.*;
import com.example.backend.request.ChiTietDonHangRequest;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChiTietDonHangServiceTest {

    @Mock private ChiTietDonHangRepository chiTietDonHangRepository;
    @Mock private DonHangRepository donHangRepository;
    @Mock private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Mock private ChiTietSanPhamRepository chiTietSanPhamRepository;
    @Mock private LichSuTonKhoRepository lichSuTonKhoRepository;
    @Mock private ChiTietDonHangSerialRepository chiTietDonHangSerialRepository;
    @Mock private TaiKhoanRepository taiKhoanRepository;

    @InjectMocks
    private ChiTietDonHangService service;

    @BeforeEach
    void setUpSecurity() {
        SecurityContext context = mock(SecurityContext.class);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDownSecurity() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String username) {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(username);
        when(SecurityContextHolder.getContext().getAuthentication()).thenReturn(auth);
    }

    private TaiKhoan taiKhoanKhachHang(String username, Integer khachHangId) {
        ChucVu chucVu = new ChucVu();
        chucVu.setMaChucVu("khach_hang");
        KhachHang kh = new KhachHang();
        kh.setKhachHangId(khachHangId);
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername(username);
        tk.setChucVu(chucVu);
        tk.setKhachHang(kh);
        return tk;
    }

    private TaiKhoan taiKhoanStaff(String username) {
        ChucVu chucVu = new ChucVu();
        chucVu.setMaChucVu("nhan_vien");
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername(username);
        tk.setChucVu(chucVu);
        return tk;
    }

    private ChiTietSanPham serialTrongKho(Integer id, BienTheSanPham bienThe) {
        ChiTietSanPham s = new ChiTietSanPham();
        s.setChiTietId(id);
        s.setBienThe(bienThe);
        s.setSoSerial("SN-" + id);
        s.setTrangThai("trong_kho");
        return s;
    }

    private DonHang donHangCuaKhach(Integer id, String kenhBan, Integer khachHangId) {
        KhachHang kh = new KhachHang();
        kh.setKhachHangId(khachHangId);
        DonHang d = new DonHang();
        d.setId(id);
        d.setKenhBan(kenhBan);
        d.setKhachHang(kh);
        return d;
    }

    @Test
    void create_donOnline_giuChoKhongDanhDauDaBan() {
        loginAs("khach1");
        when(taiKhoanRepository.findByUsername("khach1")).thenReturn(Optional.of(taiKhoanKhachHang("khach1", 1)));

        DonHang donHang = donHangCuaKhach(1, "online", 1);
        when(donHangRepository.findById(1)).thenReturn(Optional.of(donHang));
        when(donHangRepository.getReferenceById(1)).thenReturn(donHang);

        BienTheSanPham bienThe = new BienTheSanPham();
        bienThe.setBienTheId(10);
        when(bienTheSanPhamRepository.findById(10)).thenReturn(Optional.of(bienThe));

        ChiTietSanPham s1 = serialTrongKho(100, bienThe);
        ChiTietSanPham s2 = serialTrongKho(101, bienThe);
        when(chiTietSanPhamRepository.findByBienThe_BienTheIdAndTrangThaiOrderByNgayNhapKhoAsc(10, "trong_kho"))
                .thenReturn(List.of(s1, s2));
        when(chiTietDonHangRepository.save(any(ChiTietDonHang.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ChiTietDonHangRequest request = new ChiTietDonHangRequest(1, 10, null, 2, BigDecimal.TEN, BigDecimal.ZERO, null);

        service.create(request);

        assertThat(s1.getTrangThai()).isEqualTo("giu_hang");
        assertThat(s2.getTrangThai()).isEqualTo("giu_hang");
        verify(chiTietDonHangSerialRepository, times(2)).save(any(ChiTietDonHangSerial.class));
    }

    @Test
    void create_donTaiQuay_danhDauDaBanNgay() {
        loginAs("nv1");
        when(taiKhoanRepository.findByUsername("nv1")).thenReturn(Optional.of(taiKhoanStaff("nv1")));

        DonHang donHang = donHangCuaKhach(2, "in_store", 99);
        when(donHangRepository.findById(2)).thenReturn(Optional.of(donHang));
        when(donHangRepository.getReferenceById(2)).thenReturn(donHang);

        BienTheSanPham bienThe = new BienTheSanPham();
        bienThe.setBienTheId(20);
        when(bienTheSanPhamRepository.findById(20)).thenReturn(Optional.of(bienThe));

        ChiTietSanPham s1 = serialTrongKho(200, bienThe);
        when(chiTietSanPhamRepository.findByBienThe_BienTheIdAndTrangThaiOrderByNgayNhapKhoAsc(20, "trong_kho"))
                .thenReturn(List.of(s1));
        when(chiTietDonHangRepository.save(any(ChiTietDonHang.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ChiTietDonHangRequest request = new ChiTietDonHangRequest(2, 20, null, 1, BigDecimal.TEN, BigDecimal.ZERO, null);

        service.create(request);

        assertThat(s1.getTrangThai()).isEqualTo("da_ban");
        verify(chiTietDonHangSerialRepository, times(1)).save(any(ChiTietDonHangSerial.class));
    }

    @Test
    void create_serialChonTayDaBiGanChoDonKhac_biTuChoi() {
        loginAs("nv1");
        when(taiKhoanRepository.findByUsername("nv1")).thenReturn(Optional.of(taiKhoanStaff("nv1")));

        DonHang donHang = donHangCuaKhach(2, "in_store", 99);
        when(donHangRepository.findById(2)).thenReturn(Optional.of(donHang));
        when(donHangRepository.getReferenceById(2)).thenReturn(donHang);

        BienTheSanPham bienThe = new BienTheSanPham();
        bienThe.setBienTheId(20);
        when(bienTheSanPhamRepository.findById(20)).thenReturn(Optional.of(bienThe));

        ChiTietSanPham s1 = serialTrongKho(300, bienThe);
        s1.setTrangThai("giu_hang");
        when(chiTietSanPhamRepository.findByIdForUpdate(300)).thenReturn(Optional.of(s1));

        ChiTietDonHangRequest request = new ChiTietDonHangRequest(2, 20, 300, 1, BigDecimal.TEN, BigDecimal.ZERO, null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(chiTietDonHangRepository, never()).save(any());
    }

    @Test
    void create_khongPhaiChuDon_biTuChoi() {
        loginAs("khach2");
        when(taiKhoanRepository.findByUsername("khach2")).thenReturn(Optional.of(taiKhoanKhachHang("khach2", 2)));

        DonHang donHang = donHangCuaKhach(1, "online", 1);
        when(donHangRepository.findById(1)).thenReturn(Optional.of(donHang));

        ChiTietDonHangRequest request = new ChiTietDonHangRequest(1, 10, null, 1, BigDecimal.TEN, BigDecimal.ZERO, null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AccessDeniedException.class);
        verify(chiTietDonHangRepository, never()).save(any());
    }

    @Test
    void delete_traLaiSerialDaiDienVaCacSerialTrongBangJoinVeTrongKho() {
        BienTheSanPham bienThe = new BienTheSanPham();
        bienThe.setBienTheId(10);

        ChiTietSanPham repSerial = serialTrongKho(100, bienThe);
        repSerial.setTrangThai("da_ban");
        ChiTietDonHang entity = new ChiTietDonHang();
        entity.setId(5);
        entity.setChiTietSanPham(repSerial);
        when(chiTietDonHangRepository.findById(5)).thenReturn(Optional.of(entity));

        ChiTietSanPham extraSerial = serialTrongKho(101, bienThe);
        extraSerial.setTrangThai("da_ban");
        ChiTietDonHangSerial link = new ChiTietDonHangSerial();
        link.setChiTietSanPham(extraSerial);
        when(chiTietDonHangSerialRepository.findByChiTietDonHang_Id(5)).thenReturn(List.of(link));

        service.delete(5);

        assertThat(repSerial.getTrangThai()).isEqualTo("trong_kho");
        assertThat(extraSerial.getTrangThai()).isEqualTo("trong_kho");
        verify(chiTietDonHangRepository).deleteById(5);
    }
}
