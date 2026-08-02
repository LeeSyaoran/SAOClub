package com.example.backend.service;

import com.example.backend.entity.ChiTietDonHang;
import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.entity.ChiTietTraHang;
import com.example.backend.entity.ChucVu;
import com.example.backend.entity.DonHang;
import com.example.backend.entity.KhachHang;
import com.example.backend.entity.PhieuTraHang;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.ChiTietDonHangRepository;
import com.example.backend.repository.ChiTietSanPhamRepository;
import com.example.backend.repository.ChiTietTraHangRepository;
import com.example.backend.repository.DonHangRepository;
import com.example.backend.repository.KhachHangRepository;
import com.example.backend.repository.NhanVienRepository;
import com.example.backend.repository.PhieuTraHangRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.request.DongTraRequest;
import com.example.backend.request.PhieuTraHangRequest;
import com.example.backend.request.YeuCauTraHangRequest;
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
class PhieuTraHangServiceTest {

    @Mock private PhieuTraHangRepository phieuTraHangRepository;
    @Mock private DonHangRepository donHangRepository;
    @Mock private NhanVienRepository nhanVienRepository;
    @Mock private KhachHangRepository khachHangRepository;
    @Mock private ChiTietDonHangRepository chiTietDonHangRepository;
    @Mock private ChiTietTraHangRepository chiTietTraHangRepository;
    @Mock private TaiKhoanRepository taiKhoanRepository;
    @Mock private ChiTietSanPhamRepository chiTietSanPhamRepository;

    @InjectMocks
    private PhieuTraHangService service;

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

    private PhieuTraHangRequest requestDaXuLyQuaVi(Integer donHangId, BigDecimal soTien) {
        PhieuTraHangRequest r = new PhieuTraHangRequest();
        r.setDonHangId(donHangId);
        r.setNhanVienId(null);
        r.setLyDo("Hàng lỗi");
        r.setNgayTra(LocalDateTime.now());
        r.setTrangThai("da_xu_ly");
        r.setSoTienHoan(soTien);
        r.setHinhThucHoan("vi");
        r.setGhiChu("—");
        return r;
    }

    @Test
    void update_chuyenDaXuLy_hinhThucVi_congTienVaoVi() {
        KhachHang kh = new KhachHang();
        kh.setKhachHangId(1);
        kh.setSoDuVi(BigDecimal.valueOf(100_000));
        kh.setDiemTichLuy(20);

        DonHang donHang = new DonHang();
        donHang.setId(9);
        donHang.setKhachHang(kh);

        PhieuTraHang phieu = new PhieuTraHang();
        phieu.setPhieuTraId(5);
        phieu.setDonHang(donHang);
        phieu.setTrangThai("cho_xu_ly");

        when(phieuTraHangRepository.findById(5)).thenReturn(Optional.of(phieu));
        when(donHangRepository.findById(9)).thenReturn(Optional.of(donHang));
        when(phieuTraHangRepository.findByDonHang_Id(9)).thenReturn(List.of());
        when(phieuTraHangRepository.save(phieu)).thenReturn(phieu);

        service.update(5, requestDaXuLyQuaVi(9, BigDecimal.valueOf(50_000)));

        assertThat(kh.getSoDuVi()).isEqualByComparingTo(BigDecimal.valueOf(150_000));
        // Hoàn 50.000 -> trừ lại đúng floor(50000/10000) = 5 điểm đã cộng khi giao hàng trước đó.
        assertThat(kh.getDiemTichLuy()).isEqualTo(15);
        verify(khachHangRepository, times(2)).save(kh);
    }

    @Test
    void update_chuyenDaXuLy_hinhThucTienMat_khongDungToiVi() {
        KhachHang kh = new KhachHang();
        kh.setKhachHangId(1);
        kh.setSoDuVi(BigDecimal.valueOf(100_000));
        kh.setDiemTichLuy(20);

        DonHang donHang = new DonHang();
        donHang.setId(9);
        donHang.setKhachHang(kh);

        PhieuTraHang phieu = new PhieuTraHang();
        phieu.setPhieuTraId(5);
        phieu.setDonHang(donHang);
        phieu.setTrangThai("cho_xu_ly");

        when(phieuTraHangRepository.findById(5)).thenReturn(Optional.of(phieu));
        when(donHangRepository.findById(9)).thenReturn(Optional.of(donHang));
        when(phieuTraHangRepository.findByDonHang_Id(9)).thenReturn(List.of());
        when(phieuTraHangRepository.save(phieu)).thenReturn(phieu);

        PhieuTraHangRequest req = requestDaXuLyQuaVi(9, BigDecimal.valueOf(50_000));
        req.setHinhThucHoan("tien_mat");

        service.update(5, req);

        assertThat(kh.getSoDuVi()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
        // Hoàn tiền mặt không đụng ví, nhưng vẫn phải trừ lại điểm tích lũy đã cộng khi giao
        // hàng — điểm gắn với đơn đã trả, không phụ thuộc hình thức hoàn tiền lần này.
        assertThat(kh.getDiemTichLuy()).isEqualTo(15);
        verify(khachHangRepository, times(1)).save(kh);
    }

    // ── Cộng lại kho khi phiếu chuyển "da_xu_ly" ──────

    @Test
    void update_chuyenDaXuLy_dongConSerialKhongLoi_congLaiKho() {
        KhachHang kh = new KhachHang();
        kh.setKhachHangId(1);
        kh.setSoDuVi(BigDecimal.valueOf(100_000));
        kh.setDiemTichLuy(20);

        DonHang donHang = new DonHang();
        donHang.setId(9);
        donHang.setKhachHang(kh);

        PhieuTraHang phieu = new PhieuTraHang();
        phieu.setPhieuTraId(5);
        phieu.setDonHang(donHang);
        phieu.setTrangThai("cho_xu_ly");

        ChiTietSanPham serial = new ChiTietSanPham();
        serial.setChiTietId(200);
        serial.setTrangThai("da_ban");
        ChiTietTraHang dongTra = new ChiTietTraHang();
        dongTra.setChiTietSanPham(serial);
        dongTra.setTinhTrang("Đổi ý, máy còn nguyên"); // không chứa "lỗi"/"hỏng"

        when(phieuTraHangRepository.findById(5)).thenReturn(Optional.of(phieu));
        when(donHangRepository.findById(9)).thenReturn(Optional.of(donHang));
        when(phieuTraHangRepository.findByDonHang_Id(9)).thenReturn(List.of());
        when(phieuTraHangRepository.save(phieu)).thenReturn(phieu);
        when(chiTietTraHangRepository.findByPhieuTraHang_PhieuTraId(5)).thenReturn(List.of(dongTra));

        service.update(5, requestDaXuLyQuaVi(9, BigDecimal.valueOf(50_000)));

        assertThat(serial.getTrangThai()).isEqualTo("trong_kho");
        verify(chiTietSanPhamRepository).save(serial);
    }

    @Test
    void update_chuyenDaXuLy_dongHangLoi_khongCongLaiKho() {
        KhachHang kh = new KhachHang();
        kh.setKhachHangId(1);
        kh.setSoDuVi(BigDecimal.valueOf(100_000));
        kh.setDiemTichLuy(20);

        DonHang donHang = new DonHang();
        donHang.setId(9);
        donHang.setKhachHang(kh);

        PhieuTraHang phieu = new PhieuTraHang();
        phieu.setPhieuTraId(5);
        phieu.setDonHang(donHang);
        phieu.setTrangThai("cho_xu_ly");

        ChiTietSanPham serial = new ChiTietSanPham();
        serial.setChiTietId(200);
        serial.setTrangThai("da_ban");
        ChiTietTraHang dongTra = new ChiTietTraHang();
        dongTra.setChiTietSanPham(serial);
        dongTra.setTinhTrang("Máy bị lỗi màn hình");

        when(phieuTraHangRepository.findById(5)).thenReturn(Optional.of(phieu));
        when(donHangRepository.findById(9)).thenReturn(Optional.of(donHang));
        when(phieuTraHangRepository.findByDonHang_Id(9)).thenReturn(List.of());
        when(phieuTraHangRepository.save(phieu)).thenReturn(phieu);
        when(chiTietTraHangRepository.findByPhieuTraHang_PhieuTraId(5)).thenReturn(List.of(dongTra));

        service.update(5, requestDaXuLyQuaVi(9, BigDecimal.valueOf(50_000)));

        assertThat(serial.getTrangThai()).isEqualTo("da_ban"); // giữ nguyên, không cộng lại kho
        verify(chiTietSanPhamRepository, never()).save(any());
    }

    // ── Cập nhật đơn hàng (thanh toán refunded + trạng thái returned) khi phiếu chuyển "da_xu_ly" ──────

    @Test
    void update_chuyenDaXuLy_donDangDelivered_chuyenReturnedVaRefunded() {
        KhachHang kh = new KhachHang();
        kh.setKhachHangId(1);
        kh.setSoDuVi(BigDecimal.valueOf(100_000));
        kh.setDiemTichLuy(20);

        DonHang donHang = new DonHang();
        donHang.setId(9);
        donHang.setKhachHang(kh);
        donHang.setTrangThaiDonHang("delivered");
        donHang.setTrangThaiThanhToan("paid");

        PhieuTraHang phieu = new PhieuTraHang();
        phieu.setPhieuTraId(5);
        phieu.setDonHang(donHang);
        phieu.setTrangThai("cho_xu_ly");

        when(phieuTraHangRepository.findById(5)).thenReturn(Optional.of(phieu));
        when(donHangRepository.findById(9)).thenReturn(Optional.of(donHang));
        when(phieuTraHangRepository.findByDonHang_Id(9)).thenReturn(List.of());
        when(phieuTraHangRepository.save(phieu)).thenReturn(phieu);

        service.update(5, requestDaXuLyQuaVi(9, BigDecimal.valueOf(50_000)));

        assertThat(donHang.getTrangThaiThanhToan()).isEqualTo("refunded");
        assertThat(donHang.getTrangThaiDonHang()).isEqualTo("returned");
        verify(donHangRepository).save(donHang);
    }

    @Test
    void update_chuyenDaXuLy_donChuaDelivered_khongEpTrangThaiDon() {
        KhachHang kh = new KhachHang();
        kh.setKhachHangId(1);
        kh.setSoDuVi(BigDecimal.valueOf(100_000));
        kh.setDiemTichLuy(20);

        DonHang donHang = new DonHang();
        donHang.setId(9);
        donHang.setKhachHang(kh);
        donHang.setTrangThaiDonHang("shipping"); // nhan vien tao phieu tra som, don chua giao xong
        donHang.setTrangThaiThanhToan("paid");

        PhieuTraHang phieu = new PhieuTraHang();
        phieu.setPhieuTraId(5);
        phieu.setDonHang(donHang);
        phieu.setTrangThai("cho_xu_ly");

        when(phieuTraHangRepository.findById(5)).thenReturn(Optional.of(phieu));
        when(donHangRepository.findById(9)).thenReturn(Optional.of(donHang));
        when(phieuTraHangRepository.findByDonHang_Id(9)).thenReturn(List.of());
        when(phieuTraHangRepository.save(phieu)).thenReturn(phieu);

        service.update(5, requestDaXuLyQuaVi(9, BigDecimal.valueOf(50_000)));

        // Van danh dau da hoan tien, nhung KHONG ep trang thai don nhay coc tu "shipping".
        assertThat(donHang.getTrangThaiThanhToan()).isEqualTo("refunded");
        assertThat(donHang.getTrangThaiDonHang()).isEqualTo("shipping");
    }

    @Test
    void update_daXuLyRoiSuaLaiVanDaXuLy_khongCongViLanNua() {
        KhachHang kh = new KhachHang();
        kh.setKhachHangId(1);
        kh.setSoDuVi(BigDecimal.valueOf(100_000));

        DonHang donHang = new DonHang();
        donHang.setId(9);
        donHang.setKhachHang(kh);

        PhieuTraHang phieu = new PhieuTraHang();
        phieu.setPhieuTraId(5);
        phieu.setDonHang(donHang);
        phieu.setTrangThai("da_xu_ly"); // đã xử lý từ trước
        // Khớp đúng giá trị requestDaXuLyQuaVi() gửi lên (hinhThucHoan="vi", soTienHoan=50_000)
        // — không đổi trường tiền-liên-quan nào nên qua được guard chanSuaSauKhiDaCongVi().
        phieu.setHinhThucHoan("vi");
        phieu.setSoTienHoan(BigDecimal.valueOf(50_000));

        when(phieuTraHangRepository.findById(5)).thenReturn(Optional.of(phieu));
        when(donHangRepository.findById(9)).thenReturn(Optional.of(donHang));
        when(phieuTraHangRepository.findByDonHang_Id(9)).thenReturn(List.of());
        when(phieuTraHangRepository.save(phieu)).thenReturn(phieu);

        service.update(5, requestDaXuLyQuaVi(9, BigDecimal.valueOf(50_000)));

        assertThat(kh.getSoDuVi()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
        verify(khachHangRepository, never()).save(any());
    }

    // ── Guard: chặn sửa các trường tiền-liên-quan sau khi đã hoàn tiền qua ví ──────

    private PhieuTraHang phieuDaCongViQua(Integer donHangId) {
        KhachHang kh = new KhachHang();
        kh.setKhachHangId(1);
        kh.setSoDuVi(BigDecimal.valueOf(100_000));

        DonHang donHang = new DonHang();
        donHang.setId(donHangId);
        donHang.setKhachHang(kh);

        PhieuTraHang phieu = new PhieuTraHang();
        phieu.setPhieuTraId(5);
        phieu.setDonHang(donHang);
        phieu.setTrangThai("da_xu_ly");
        phieu.setHinhThucHoan("vi");
        phieu.setSoTienHoan(BigDecimal.valueOf(50_000));
        phieu.setLyDo("Hàng lỗi");
        phieu.setGhiChu("—");

        when(phieuTraHangRepository.findById(5)).thenReturn(Optional.of(phieu));
        return phieu;
    }

    @Test
    void update_daCongViQua_doiTrangThai_nemLoi() {
        phieuDaCongViQua(9);

        PhieuTraHangRequest req = requestDaXuLyQuaVi(9, BigDecimal.valueOf(50_000));
        req.setTrangThai("tu_choi");

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.update(5, req));

        verify(phieuTraHangRepository, never()).save(any());
        verify(khachHangRepository, never()).save(any());
    }

    @Test
    void update_daCongViQua_doiSoTienHoan_nemLoi() {
        phieuDaCongViQua(9);

        PhieuTraHangRequest req = requestDaXuLyQuaVi(9, BigDecimal.valueOf(70_000));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.update(5, req));

        verify(phieuTraHangRepository, never()).save(any());
        verify(khachHangRepository, never()).save(any());
    }

    @Test
    void update_daCongViQua_doiHinhThucHoan_nemLoi() {
        phieuDaCongViQua(9);

        PhieuTraHangRequest req = requestDaXuLyQuaVi(9, BigDecimal.valueOf(50_000));
        req.setHinhThucHoan("tien_mat");

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.update(5, req));

        verify(phieuTraHangRepository, never()).save(any());
        verify(khachHangRepository, never()).save(any());
    }

    // Phieu tien_mat da xu ly cung phai bi khoa nhu phieu vi — truoc day guard chi ap dung
    // khi hinhThucHoan="vi", phieu tien_mat co the doi lui trang_thai roi luu lai "da_xu_ly"
    // de tru diem tich luy 2 lan cho cung 1 lan hoan thuc te (xem truHoiDiemNeuVuaHoanTat).
    @Test
    void update_daXuLyRoiTienMat_doiTrangThai_nemLoi() {
        KhachHang kh = new KhachHang();
        kh.setKhachHangId(1);
        kh.setDiemTichLuy(20);

        DonHang donHang = new DonHang();
        donHang.setId(9);
        donHang.setKhachHang(kh);

        PhieuTraHang phieu = new PhieuTraHang();
        phieu.setPhieuTraId(5);
        phieu.setDonHang(donHang);
        phieu.setTrangThai("da_xu_ly");
        phieu.setHinhThucHoan("tien_mat");
        phieu.setSoTienHoan(BigDecimal.valueOf(50_000));
        when(phieuTraHangRepository.findById(5)).thenReturn(Optional.of(phieu));

        PhieuTraHangRequest req = requestDaXuLyQuaVi(9, BigDecimal.valueOf(50_000));
        req.setHinhThucHoan("tien_mat");
        req.setTrangThai("cho_xu_ly"); // thu doi lui de sau do luu lai "da_xu_ly"

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.update(5, req));

        verify(phieuTraHangRepository, never()).save(any());
        verify(khachHangRepository, never()).save(any());
    }

    @Test
    void update_daCongViQua_suaGhiChu_thanhCong_khongCongViLanNua() {
        PhieuTraHang phieu = phieuDaCongViQua(9);
        KhachHang kh = phieu.getDonHang().getKhachHang();

        when(donHangRepository.findById(9)).thenReturn(Optional.of(phieu.getDonHang()));
        when(phieuTraHangRepository.findByDonHang_Id(9)).thenReturn(List.of(phieu));
        when(phieuTraHangRepository.save(phieu)).thenReturn(phieu);

        PhieuTraHangRequest req = requestDaXuLyQuaVi(9, BigDecimal.valueOf(50_000));
        req.setGhiChu("Ghi chú mới");

        service.update(5, req);

        assertThat(phieu.getGhiChu()).isEqualTo("Ghi chú mới");
        assertThat(kh.getSoDuVi()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
        verify(khachHangRepository, never()).save(any());
    }

    @Test
    void create_ganMaPhieuSauKhiLuuMoi() {
        // requestDaXuLyQuaVi() tạo phiếu "da_xu_ly" + hoàn "vi" ngay từ lúc tạo mới, nên
        // create() cũng chạm nhánh cộng ví/trừ điểm (congViNeuVuaHoanTat) — phải gắn KhachHang
        // đầy đủ vào donHang giống các test khác dùng chung helper này, nếu không NPE ở
        // KhachHang.getSoDuVi() dù test này chỉ nhắm tới việc gán mã phiếu.
        KhachHang kh = new KhachHang();
        kh.setKhachHangId(1);
        kh.setSoDuVi(BigDecimal.valueOf(100_000));
        kh.setDiemTichLuy(20);

        DonHang donHang = new DonHang();
        donHang.setId(9);
        donHang.setKhachHang(kh);
        donHang.setThanhTien(BigDecimal.valueOf(100_000));
        donHang.setTongTien(BigDecimal.valueOf(100_000));

        when(donHangRepository.findById(9)).thenReturn(Optional.of(donHang));
        when(phieuTraHangRepository.findByDonHang_Id(9)).thenReturn(List.of());
        when(phieuTraHangRepository.save(any(PhieuTraHang.class))).thenAnswer(inv -> {
            PhieuTraHang p = inv.getArgument(0);
            if (p.getPhieuTraId() == null) p.setPhieuTraId(101);
            return p;
        });

        PhieuTraHangRequest req = requestDaXuLyQuaVi(9, BigDecimal.valueOf(50_000));
        PhieuTraHang saved = service.create(req);

        assertThat(saved.getMaPhieu()).isEqualTo("TR-101");
    }

    // ── Khách hàng tự gửi yêu cầu trả hàng ──────

    @Test
    void taoYeuCau_hopLe_taoPhieuChoXuLyHoanVi() {
        loginAs("khach1");
        when(taiKhoanRepository.findByUsername("khach1")).thenReturn(Optional.of(taiKhoanKhachHang("khach1", 42)));

        KhachHang kh = new KhachHang();
        kh.setKhachHangId(42);
        DonHang donHang = new DonHang();
        donHang.setId(9);
        donHang.setKhachHang(kh);
        donHang.setTrangThaiDonHang("delivered");
        donHang.setNgayGiaoThucTe(LocalDateTime.now().minusDays(2));
        when(donHangRepository.findById(9)).thenReturn(Optional.of(donHang));
        when(phieuTraHangRepository.findByDonHang_Id(9)).thenReturn(List.of());

        ChiTietDonHang dong = new ChiTietDonHang();
        dong.setId(100);
        dong.setDonHang(donHang);
        dong.setSoLuong(2);
        dong.setDonGia(BigDecimal.valueOf(500_000));
        when(chiTietDonHangRepository.findById(100)).thenReturn(Optional.of(dong));

        when(phieuTraHangRepository.save(any(PhieuTraHang.class))).thenAnswer(inv -> inv.getArgument(0));

        YeuCauTraHangRequest req = new YeuCauTraHangRequest(9, "Không vừa ý", List.of(new DongTraRequest(100, 1)));

        PhieuTraHang saved = service.taoYeuCauTuKhachHang(req);

        assertThat(saved.getTrangThai()).isEqualTo("cho_xu_ly");
        assertThat(saved.getHinhThucHoan()).isEqualTo("vi");
        assertThat(saved.getNhanVien()).isNull();
        assertThat(saved.getSoTienHoan()).isEqualByComparingTo(BigDecimal.valueOf(500_000));
        verify(chiTietTraHangRepository).save(any(ChiTietTraHang.class));
    }

    @Test
    void taoYeuCau_donKhongPhaiCuaMinh_biTuChoi() {
        loginAs("khach2");
        when(taiKhoanRepository.findByUsername("khach2")).thenReturn(Optional.of(taiKhoanKhachHang("khach2", 43)));

        KhachHang chuDon = new KhachHang();
        chuDon.setKhachHangId(42);
        DonHang donHang = new DonHang();
        donHang.setId(9);
        donHang.setKhachHang(chuDon);
        donHang.setTrangThaiDonHang("delivered");
        donHang.setNgayGiaoThucTe(LocalDateTime.now().minusDays(2));
        when(donHangRepository.findById(9)).thenReturn(Optional.of(donHang));

        YeuCauTraHangRequest req = new YeuCauTraHangRequest(9, "Không vừa ý", List.of(new DongTraRequest(100, 1)));

        assertThatThrownBy(() -> service.taoYeuCauTuKhachHang(req))
                .isInstanceOf(AccessDeniedException.class);
        verify(phieuTraHangRepository, never()).save(any());
    }

    @Test
    void taoYeuCau_quaHan7Ngay_biChan() {
        loginAs("khach1");
        when(taiKhoanRepository.findByUsername("khach1")).thenReturn(Optional.of(taiKhoanKhachHang("khach1", 42)));

        KhachHang kh = new KhachHang();
        kh.setKhachHangId(42);
        DonHang donHang = new DonHang();
        donHang.setId(9);
        donHang.setKhachHang(kh);
        donHang.setTrangThaiDonHang("delivered");
        donHang.setNgayGiaoThucTe(LocalDateTime.now().minusDays(8));
        when(donHangRepository.findById(9)).thenReturn(Optional.of(donHang));

        YeuCauTraHangRequest req = new YeuCauTraHangRequest(9, "Không vừa ý", List.of(new DongTraRequest(100, 1)));

        assertThatThrownBy(() -> service.taoYeuCauTuKhachHang(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("7 ngày");
        verify(phieuTraHangRepository, never()).save(any());
    }

    @Test
    void taoYeuCau_donCoPhieuChoXuLyRoi_biChanTaoTrung() {
        loginAs("khach1");
        when(taiKhoanRepository.findByUsername("khach1")).thenReturn(Optional.of(taiKhoanKhachHang("khach1", 42)));

        KhachHang kh = new KhachHang();
        kh.setKhachHangId(42);
        DonHang donHang = new DonHang();
        donHang.setId(9);
        donHang.setKhachHang(kh);
        donHang.setTrangThaiDonHang("delivered");
        donHang.setNgayGiaoThucTe(LocalDateTime.now().minusDays(1));
        when(donHangRepository.findById(9)).thenReturn(Optional.of(donHang));

        PhieuTraHang phieuCu = new PhieuTraHang();
        phieuCu.setTrangThai("cho_xu_ly");
        when(phieuTraHangRepository.findByDonHang_Id(9)).thenReturn(List.of(phieuCu));

        YeuCauTraHangRequest req = new YeuCauTraHangRequest(9, "Không vừa ý", List.of(new DongTraRequest(100, 1)));

        assertThatThrownBy(() -> service.taoYeuCauTuKhachHang(req))
                .isInstanceOf(IllegalArgumentException.class);
        verify(phieuTraHangRepository, never()).save(any());
    }

    @Test
    void taoYeuCau_soLuongVuotSoDaMua_biChan() {
        loginAs("khach1");
        when(taiKhoanRepository.findByUsername("khach1")).thenReturn(Optional.of(taiKhoanKhachHang("khach1", 42)));

        KhachHang kh = new KhachHang();
        kh.setKhachHangId(42);
        DonHang donHang = new DonHang();
        donHang.setId(9);
        donHang.setKhachHang(kh);
        donHang.setTrangThaiDonHang("delivered");
        donHang.setNgayGiaoThucTe(LocalDateTime.now().minusDays(1));
        when(donHangRepository.findById(9)).thenReturn(Optional.of(donHang));
        when(phieuTraHangRepository.findByDonHang_Id(9)).thenReturn(List.of());

        ChiTietDonHang dong = new ChiTietDonHang();
        dong.setId(100);
        dong.setDonHang(donHang);
        dong.setSoLuong(1);
        dong.setDonGia(BigDecimal.valueOf(500_000));
        when(chiTietDonHangRepository.findById(100)).thenReturn(Optional.of(dong));

        YeuCauTraHangRequest req = new YeuCauTraHangRequest(9, "Không vừa ý", List.of(new DongTraRequest(100, 2)));

        assertThatThrownBy(() -> service.taoYeuCauTuKhachHang(req))
                .isInstanceOf(IllegalArgumentException.class);
        verify(phieuTraHangRepository, never()).save(any());
    }

    @Test
    void taoYeuCau_trungChiTietDonHang_congDonVuotSoDaMua_biChan() {
        loginAs("khach1");
        when(taiKhoanRepository.findByUsername("khach1")).thenReturn(Optional.of(taiKhoanKhachHang("khach1", 42)));

        KhachHang kh = new KhachHang();
        kh.setKhachHangId(42);
        DonHang donHang = new DonHang();
        donHang.setId(9);
        donHang.setKhachHang(kh);
        donHang.setTrangThaiDonHang("delivered");
        donHang.setNgayGiaoThucTe(LocalDateTime.now().minusDays(1));
        when(donHangRepository.findById(9)).thenReturn(Optional.of(donHang));
        when(phieuTraHangRepository.findByDonHang_Id(9)).thenReturn(List.of());

        ChiTietDonHang dong = new ChiTietDonHang();
        dong.setId(100);
        dong.setDonHang(donHang);
        dong.setSoLuong(3);
        dong.setDonGia(BigDecimal.valueOf(500_000));
        when(chiTietDonHangRepository.findById(100)).thenReturn(Optional.of(dong));

        // Mỗi dòng riêng lẻ (2) vẫn <= số đã mua (3), nhưng cộng dồn (2+2=4) thì vượt.
        YeuCauTraHangRequest req = new YeuCauTraHangRequest(9, "Không vừa ý",
                List.of(new DongTraRequest(100, 2), new DongTraRequest(100, 2)));

        assertThatThrownBy(() -> service.taoYeuCauTuKhachHang(req))
                .isInstanceOf(IllegalArgumentException.class);
        verify(phieuTraHangRepository, never()).save(any());
    }
}
