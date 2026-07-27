package com.example.backend.service;

import com.example.backend.entity.BienTheSanPham;
import com.example.backend.entity.ChiTietDonHang;
import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.entity.ChucVu;
import com.example.backend.entity.DonHang;
import com.example.backend.entity.KhachHang;
import com.example.backend.entity.LichSuDonHang;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.*;
import com.example.backend.request.XacNhanDonHangLineRequest;
import com.example.backend.request.XacNhanDonHangRequest;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DonHangServiceTest {

    @Mock private DonHangRepository donHangRepository;
    @Mock private SseService sseService;
    @Mock private KhachHangRepository khachHangRepository;
    @Mock private NhanVienRepository nhanVienRepository;
    @Mock private KhuyenMaiRepository khuyenMaiRepository;
    @Mock private DiaChiGiaoHangRepository diaChiGiaoHangRepository;
    @Mock private ChiTietDonHangRepository chiTietDonHangRepository;
    @Mock private ThanhToanRepository thanhToanRepository;
    @Mock private LichSuTonKhoRepository lichSuTonKhoRepository;
    @Mock private PhieuTraHangRepository phieuTraHangRepository;
    @Mock private PhieuBaoHanhRepository phieuBaoHanhRepository;
    @Mock private LichSuDonHangRepository lichSuDonHangRepository;
    @Mock private ChiTietSanPhamRepository chiTietSanPhamRepository;
    @Mock private ChiTietDonHangSerialRepository chiTietDonHangSerialRepository;
    @Mock private PhieuGiamGiaCaNhanRepository phieuGiamGiaCaNhanRepository;
    @Mock private TaiKhoanRepository taiKhoanRepository;

    @InjectMocks
    private DonHangService service;

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

    private DonHang donHangOnlinePending() {
        DonHang d = new DonHang();
        d.setId(1);
        d.setKenhBan("online");
        d.setTrangThaiDonHang("pending");
        return d;
    }

    @Test
    void xacNhan_khongPhaiDonOnline_biChan() {
        DonHang d = donHangOnlinePending();
        d.setKenhBan("in_store");
        when(donHangRepository.findById(1)).thenReturn(Optional.of(d));

        XacNhanDonHangRequest req = new XacNhanDonHangRequest(List.of());

        assertThatThrownBy(() -> service.xacNhanDonHang(1, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("online");
    }

    @Test
    void xacNhan_saiSoLuongSerial_biChan() {
        DonHang d = donHangOnlinePending();
        when(donHangRepository.findById(1)).thenReturn(Optional.of(d));

        BienTheSanPham bienThe = new BienTheSanPham();
        bienThe.setBienTheId(10);
        ChiTietDonHang item = new ChiTietDonHang();
        item.setId(5);
        item.setDonHang(d);
        item.setBienThe(bienThe);
        item.setSoLuong(2);
        when(chiTietDonHangRepository.findById(5)).thenReturn(Optional.of(item));

        XacNhanDonHangRequest req = new XacNhanDonHangRequest(List.of(new XacNhanDonHangLineRequest(5, List.of(100))));

        assertThatThrownBy(() -> service.xacNhanDonHang(1, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 serial");
    }

    @Test
    void xacNhan_hopLe_chotDaBanVaChuyenConfirmed() {
        DonHang d = donHangOnlinePending();
        when(donHangRepository.findById(1)).thenReturn(Optional.of(d));

        BienTheSanPham bienThe = new BienTheSanPham();
        bienThe.setBienTheId(10);
        ChiTietDonHang item = new ChiTietDonHang();
        item.setId(5);
        item.setDonHang(d);
        item.setBienThe(bienThe);
        item.setSoLuong(1);
        when(chiTietDonHangRepository.findById(5)).thenReturn(Optional.of(item));
        when(chiTietDonHangSerialRepository.findByChiTietDonHang_Id(5)).thenReturn(List.of());

        ChiTietSanPham serial = new ChiTietSanPham();
        serial.setChiTietId(100);
        serial.setBienThe(bienThe);
        serial.setSoSerial("SN-100");
        serial.setTrangThai("trong_kho");
        when(chiTietSanPhamRepository.findByIdForUpdate(100)).thenReturn(Optional.of(serial));

        XacNhanDonHangRequest req = new XacNhanDonHangRequest(List.of(new XacNhanDonHangLineRequest(5, List.of(100))));

        service.xacNhanDonHang(1, req);

        assertThat(serial.getTrangThai()).isEqualTo("da_ban");
        assertThat(d.getTrangThaiDonHang()).isEqualTo("confirmed");
    }

    @Test
    void update_chuyenCancelled_giaiPhongCaSerialTrongBangJoin() {
        DonHang d = new DonHang();
        d.setId(1);
        d.setTrangThaiDonHang("processing");
        when(donHangRepository.findById(1)).thenReturn(Optional.of(d));
        when(khachHangRepository.getReferenceById(1)).thenReturn(new com.example.backend.entity.KhachHang());
        when(donHangRepository.save(d)).thenReturn(d);

        ChiTietSanPham repSerial = new ChiTietSanPham();
        repSerial.setChiTietId(100);
        repSerial.setTrangThai("da_ban");
        ChiTietDonHang item = new ChiTietDonHang();
        item.setId(5);
        item.setChiTietSanPham(repSerial);
        when(chiTietDonHangRepository.findEntityByDonHangId(1)).thenReturn(List.of(item));

        ChiTietSanPham extraSerial = new ChiTietSanPham();
        extraSerial.setChiTietId(101);
        extraSerial.setTrangThai("da_ban");
        com.example.backend.entity.ChiTietDonHangSerial link = new com.example.backend.entity.ChiTietDonHangSerial();
        link.setChiTietSanPham(extraSerial);
        when(chiTietDonHangSerialRepository.findByChiTietDonHang_Id(5)).thenReturn(List.of(link));

        com.example.backend.request.DonHangRequest request = new com.example.backend.request.DonHangRequest();
        request.setKhachHangId(1);
        request.setTrangThaiDonHang("cancelled");

        service.update(1, request);

        assertThat(repSerial.getTrangThai()).isEqualTo("trong_kho");
        assertThat(extraSerial.getTrangThai()).isEqualTo("trong_kho");
    }

    @Test
    void mergeOrders_donNguonChuaXacNhan_biChan() {
        DonHang target = new DonHang();
        target.setId(1);
        target.setTrangThaiDonHang("confirmed");
        DonHang source = new DonHang();
        source.setId(2);
        source.setTrangThaiDonHang("pending");
        when(donHangRepository.findById(1)).thenReturn(Optional.of(target));
        when(donHangRepository.findById(2)).thenReturn(Optional.of(source));

        assertThatThrownBy(() -> service.mergeOrders(1, List.of(2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chưa được xác nhận");
    }

    @Test
    void mergeOrders_hopLe_chuyenLichSuDonHangSangDonDich() {
        DonHang target = new DonHang();
        target.setId(1);
        target.setTrangThaiDonHang("confirmed");
        DonHang source = new DonHang();
        source.setId(2);
        source.setTrangThaiDonHang("confirmed");
        when(donHangRepository.findById(1)).thenReturn(Optional.of(target));
        when(donHangRepository.findById(2)).thenReturn(Optional.of(source));
        when(chiTietDonHangRepository.findEntityByDonHangId(2)).thenReturn(List.of());
        when(chiTietDonHangRepository.findEntityByDonHangId(1)).thenReturn(List.of());
        when(thanhToanRepository.findByDonHang_Id(2)).thenReturn(List.of());
        when(lichSuTonKhoRepository.findByDonHang_Id(2)).thenReturn(List.of());
        when(phieuTraHangRepository.findByDonHang_Id(2)).thenReturn(List.of());
        when(phieuBaoHanhRepository.findByDonHang_Id(2)).thenReturn(List.of());

        LichSuDonHang log = new LichSuDonHang();
        log.setLichSuId(9);
        log.setDonHangId(2);
        log.setTrangThaiMoi("confirmed");
        when(lichSuDonHangRepository.findByDonHangId(2)).thenReturn(List.of(log));

        service.mergeOrders(1, List.of(2));

        assertThat(log.getDonHangId()).isEqualTo(1);
        verify(lichSuDonHangRepository).save(log);
    }

    @Test
    void create_coPhieuGiamGiaCaNhan_danhDauDaSuDung() {
        loginAs("khach1");
        when(taiKhoanRepository.findByUsername("khach1")).thenReturn(Optional.of(taiKhoanKhachHang("khach1", 1)));

        com.example.backend.entity.KhachHang kh = new com.example.backend.entity.KhachHang();
        kh.setKhachHangId(1);
        when(khachHangRepository.getReferenceById(1)).thenReturn(kh);

        DonHang saved = new DonHang();
        saved.setId(10);
        saved.setKhachHang(kh);
        when(donHangRepository.save(any(DonHang.class))).thenReturn(saved);

        com.example.backend.entity.PhieuGiamGiaCaNhan phieu = new com.example.backend.entity.PhieuGiamGiaCaNhan();
        phieu.setPhieuId(7);
        phieu.setKhachHang(kh);
        phieu.setDaSuDung(false);
        phieu.setNgayHetHan(java.time.LocalDateTime.now().plusDays(10));
        when(phieuGiamGiaCaNhanRepository.findWithLockByPhieuId(7)).thenReturn(Optional.of(phieu));

        com.example.backend.request.DonHangRequest req = new com.example.backend.request.DonHangRequest();
        req.setKhachHangId(1);
        req.setTrangThaiDonHang("pending");
        req.setTrangThaiThanhToan("unpaid");
        req.setNguoiNhan("A");
        req.setSdtNguoiNhan("0900000000");
        req.setTongTien(java.math.BigDecimal.ZERO);
        req.setGiamGia(java.math.BigDecimal.ZERO);
        req.setPhiVanChuyen(java.math.BigDecimal.ZERO);
        req.setNgayDat(java.time.LocalDateTime.now());
        req.setPhieuGiamGiaCaNhanId(7);

        service.create(req);

        assertThat(phieu.getDaSuDung()).isTrue();
        assertThat(phieu.getDonHang()).isEqualTo(saved);
        verify(phieuGiamGiaCaNhanRepository).findWithLockByPhieuId(7);
        verify(phieuGiamGiaCaNhanRepository).save(phieu);
    }

    // Không tin giamGia client gửi lên khi KHÔNG có mã/voucher nào — nếu không, ai đó gọi
    // thẳng API (bỏ qua UI) có thể tự đặt giảm giá tuỳ ý mà chẳng cần mã gì cả.
    @Test
    void create_khongCoMaHayVoucher_giamGiaLuonBiEpVeKhong() {
        loginAs("khach1");
        when(taiKhoanRepository.findByUsername("khach1")).thenReturn(Optional.of(taiKhoanKhachHang("khach1", 1)));
        when(khachHangRepository.getReferenceById(1)).thenReturn(new com.example.backend.entity.KhachHang());
        when(donHangRepository.save(any(DonHang.class))).thenAnswer(inv -> inv.getArgument(0));

        com.example.backend.request.DonHangRequest req = new com.example.backend.request.DonHangRequest();
        req.setKhachHangId(1);
        req.setTrangThaiDonHang("pending");
        req.setTrangThaiThanhToan("unpaid");
        req.setNguoiNhan("A");
        req.setSdtNguoiNhan("0900000000");
        req.setTongTien(java.math.BigDecimal.valueOf(1_000_000));
        req.setGiamGia(java.math.BigDecimal.valueOf(999_999)); // bịa, không gắn mã/voucher nào
        req.setPhiVanChuyen(java.math.BigDecimal.ZERO);
        req.setNgayDat(java.time.LocalDateTime.now());

        DonHang saved = service.create(req);

        assertThat(saved.getGiamGia()).isEqualByComparingTo(java.math.BigDecimal.ZERO);
    }

    // Mã khuyến mãi hết hạn vẫn có thể còn tồn tại trong DB (chỉ ẩn khỏi danh sách gợi ý ở
    // UI) — request thẳng API với đúng id của nó phải bị chặn ở server, không chỉ ở UI.
    @Test
    void create_khuyenMaiHetHan_biTuChoi() {
        loginAs("khach1");
        when(taiKhoanRepository.findByUsername("khach1")).thenReturn(Optional.of(taiKhoanKhachHang("khach1", 1)));
        when(khachHangRepository.getReferenceById(1)).thenReturn(new com.example.backend.entity.KhachHang());

        com.example.backend.entity.KhuyenMai km = new com.example.backend.entity.KhuyenMai();
        km.setKhuyenMaiId(3);
        km.setTrangThai("active");
        km.setLoai("percent");
        km.setGiaTri(java.math.BigDecimal.TEN);
        km.setNgayBatDau(java.time.LocalDateTime.now().minusDays(30));
        km.setNgayKetThuc(java.time.LocalDateTime.now().minusDays(1)); // đã hết hạn hôm qua
        when(khuyenMaiRepository.findById(3)).thenReturn(Optional.of(km));

        com.example.backend.request.DonHangRequest req = new com.example.backend.request.DonHangRequest();
        req.setKhachHangId(1);
        req.setKhuyenMaiId(3);
        req.setTrangThaiDonHang("pending");
        req.setTrangThaiThanhToan("unpaid");
        req.setNguoiNhan("A");
        req.setSdtNguoiNhan("0900000000");
        req.setTongTien(java.math.BigDecimal.valueOf(1_000_000));
        req.setGiamGia(java.math.BigDecimal.ZERO);
        req.setPhiVanChuyen(java.math.BigDecimal.ZERO);
        req.setNgayDat(java.time.LocalDateTime.now());

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hết hạn");

        verify(donHangRepository, never()).save(any());
    }

    // Voucher cá nhân trúng từ vòng quay giữ nguyên đơn tối thiểu của khuyến mãi gốc — phải
    // bị chặn ở server nếu đơn chưa đạt, không chỉ ẩn khỏi danh sách chọn ở checkout.
    @Test
    void create_voucherChuaDatDonToiThieu_biTuChoi() {
        loginAs("khach1");
        when(taiKhoanRepository.findByUsername("khach1")).thenReturn(Optional.of(taiKhoanKhachHang("khach1", 1)));

        com.example.backend.entity.KhachHang kh = new com.example.backend.entity.KhachHang();
        kh.setKhachHangId(1);
        when(khachHangRepository.getReferenceById(1)).thenReturn(kh);

        com.example.backend.entity.PhieuGiamGiaCaNhan phieu = new com.example.backend.entity.PhieuGiamGiaCaNhan();
        phieu.setPhieuId(7);
        phieu.setKhachHang(kh);
        phieu.setDaSuDung(false);
        phieu.setNgayHetHan(java.time.LocalDateTime.now().plusDays(10));
        phieu.setLoai("fixed");
        phieu.setGiaTri(java.math.BigDecimal.valueOf(2_000_000));
        phieu.setDonHangToiThieu(java.math.BigDecimal.valueOf(10_000_000));
        when(phieuGiamGiaCaNhanRepository.findWithLockByPhieuId(7)).thenReturn(Optional.of(phieu));

        com.example.backend.request.DonHangRequest req = new com.example.backend.request.DonHangRequest();
        req.setKhachHangId(1);
        req.setPhieuGiamGiaCaNhanId(7);
        req.setTrangThaiDonHang("pending");
        req.setTrangThaiThanhToan("unpaid");
        req.setNguoiNhan("A");
        req.setSdtNguoiNhan("0900000000");
        req.setTongTien(java.math.BigDecimal.valueOf(100_000)); // dưới xa đơn tối thiểu 10tr
        req.setGiamGia(java.math.BigDecimal.ZERO);
        req.setPhiVanChuyen(java.math.BigDecimal.ZERO);
        req.setNgayDat(java.time.LocalDateTime.now());

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tối thiểu");

        verify(donHangRepository, never()).save(any());
    }

    // Client có thể gửi giamGia sai (cố tình hoặc do bug UI) — server phải tự tính lại từ
    // chính mã khuyến mãi + tongTien thật, bỏ qua hoàn toàn số client gửi.
    @Test
    void create_khuyenMaiHopLe_tuTinhLaiGiamGiaKhongTinSoClientGui() {
        loginAs("khach1");
        when(taiKhoanRepository.findByUsername("khach1")).thenReturn(Optional.of(taiKhoanKhachHang("khach1", 1)));
        when(khachHangRepository.getReferenceById(1)).thenReturn(new com.example.backend.entity.KhachHang());
        when(donHangRepository.save(any(DonHang.class))).thenAnswer(inv -> inv.getArgument(0));

        com.example.backend.entity.KhuyenMai km = new com.example.backend.entity.KhuyenMai();
        km.setKhuyenMaiId(3);
        km.setTrangThai("active");
        km.setLoai("percent");
        km.setGiaTri(java.math.BigDecimal.TEN); // giảm 10%
        km.setNgayBatDau(java.time.LocalDateTime.now().minusDays(1));
        km.setNgayKetThuc(java.time.LocalDateTime.now().plusDays(1));
        when(khuyenMaiRepository.findById(3)).thenReturn(Optional.of(km));

        com.example.backend.request.DonHangRequest req = new com.example.backend.request.DonHangRequest();
        req.setKhachHangId(1);
        req.setKhuyenMaiId(3);
        req.setTrangThaiDonHang("pending");
        req.setTrangThaiThanhToan("unpaid");
        req.setNguoiNhan("A");
        req.setSdtNguoiNhan("0900000000");
        req.setTongTien(java.math.BigDecimal.valueOf(1_000_000)); // 10% = 100.000
        req.setGiamGia(java.math.BigDecimal.valueOf(999_999));    // client gửi bừa, phải bị bỏ qua
        req.setPhiVanChuyen(java.math.BigDecimal.ZERO);
        req.setNgayDat(java.time.LocalDateTime.now());

        DonHang saved = service.create(req);

        assertThat(saved.getGiamGia()).isEqualByComparingTo(java.math.BigDecimal.valueOf(100_000));
    }

    @Test
    void create_coCaKhuyenMaiVaPhieuGiamGiaCaNhan_biChan() {
        loginAs("khach1");
        when(taiKhoanRepository.findByUsername("khach1")).thenReturn(Optional.of(taiKhoanKhachHang("khach1", 1)));

        com.example.backend.entity.KhachHang kh = new com.example.backend.entity.KhachHang();
        kh.setKhachHangId(1);
        when(khachHangRepository.getReferenceById(1)).thenReturn(kh);

        com.example.backend.request.DonHangRequest req = new com.example.backend.request.DonHangRequest();
        req.setKhachHangId(1);
        req.setKhuyenMaiId(3);
        req.setTrangThaiDonHang("pending");
        req.setTrangThaiThanhToan("unpaid");
        req.setNguoiNhan("A");
        req.setSdtNguoiNhan("0900000000");
        req.setTongTien(java.math.BigDecimal.ZERO);
        req.setGiamGia(java.math.BigDecimal.ZERO);
        req.setPhiVanChuyen(java.math.BigDecimal.ZERO);
        req.setNgayDat(java.time.LocalDateTime.now());
        req.setPhieuGiamGiaCaNhanId(7);

        // Kiểm tra "chỉ 1 trong 2" giờ chạy TRƯỚC khi đụng tới khuyenMaiRepository/
        // donHangRepository.save() (xem create()) — không cần stub 2 cái đó nữa, request có
        // cả 2 id là bị chặn ngay, chưa kịp lookup hay insert gì.
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đồng thời");

        verify(donHangRepository, never()).save(any());
        verify(phieuGiamGiaCaNhanRepository, never()).findWithLockByPhieuId(any());
        verify(phieuGiamGiaCaNhanRepository, never()).save(any());
    }

    private DonHang donHangCuaKhach(Integer donHangId, Integer khachHangId) {
        KhachHang kh = new KhachHang();
        kh.setKhachHangId(khachHangId);
        DonHang d = new DonHang();
        d.setId(donHangId);
        d.setKhachHang(kh);
        return d;
    }

    @Test
    void delete_khongPhaiChuDon_biTuChoi() {
        loginAs("khach2");
        when(taiKhoanRepository.findByUsername("khach2")).thenReturn(Optional.of(taiKhoanKhachHang("khach2", 99)));

        DonHang d = donHangCuaKhach(1, 1);
        when(donHangRepository.findById(1)).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> service.delete(1))
                .isInstanceOf(AccessDeniedException.class);
        verify(donHangRepository, never()).deleteById(any());
    }

    @Test
    void delete_laChuDon_xoaThanhCong() {
        loginAs("khach1");
        when(taiKhoanRepository.findByUsername("khach1")).thenReturn(Optional.of(taiKhoanKhachHang("khach1", 1)));

        DonHang d = donHangCuaKhach(1, 1);
        when(donHangRepository.findById(1)).thenReturn(Optional.of(d));
        when(chiTietDonHangRepository.findEntityByDonHangId(1)).thenReturn(List.of());
        when(lichSuTonKhoRepository.findByDonHang_Id(1)).thenReturn(List.of());

        service.delete(1);

        verify(donHangRepository).deleteById(1);
    }

    @Test
    void delete_laStaff_xoaDuocDonKhachKhac() {
        loginAs("nv1");
        when(taiKhoanRepository.findByUsername("nv1")).thenReturn(Optional.of(taiKhoanStaff("nv1")));

        DonHang d = donHangCuaKhach(1, 55);
        when(donHangRepository.findById(1)).thenReturn(Optional.of(d));
        when(chiTietDonHangRepository.findEntityByDonHangId(1)).thenReturn(List.of());
        when(lichSuTonKhoRepository.findByDonHang_Id(1)).thenReturn(List.of());

        service.delete(1);

        verify(donHangRepository).deleteById(1);
    }

    @Test
    void create_khachSpoofKhachHangIdNguoiKhac_biGhiDeVeChinhMinh() {
        loginAs("khach1");
        when(taiKhoanRepository.findByUsername("khach1")).thenReturn(Optional.of(taiKhoanKhachHang("khach1", 1)));

        KhachHang minh = new KhachHang();
        minh.setKhachHangId(1);
        when(khachHangRepository.getReferenceById(1)).thenReturn(minh);
        when(donHangRepository.save(any(DonHang.class))).thenAnswer(inv -> inv.getArgument(0));

        com.example.backend.request.DonHangRequest req = new com.example.backend.request.DonHangRequest();
        req.setKhachHangId(999); // giả mạo id của khách khác
        req.setTrangThaiDonHang("pending");
        req.setTrangThaiThanhToan("unpaid");
        req.setNguoiNhan("A");
        req.setSdtNguoiNhan("0900000000");
        req.setTongTien(java.math.BigDecimal.ZERO);
        req.setGiamGia(java.math.BigDecimal.ZERO);
        req.setPhiVanChuyen(java.math.BigDecimal.ZERO);
        req.setNgayDat(java.time.LocalDateTime.now());

        service.create(req);

        verify(khachHangRepository).getReferenceById(1);
        verify(khachHangRepository, never()).getReferenceById(999);
    }

    @Test
    void create_staffTaoDonHoKhach_dungKhachHangIdTuRequest() {
        loginAs("nv1");
        when(taiKhoanRepository.findByUsername("nv1")).thenReturn(Optional.of(taiKhoanStaff("nv1")));

        KhachHang khach = new KhachHang();
        khach.setKhachHangId(77);
        when(khachHangRepository.getReferenceById(77)).thenReturn(khach);
        when(donHangRepository.save(any(DonHang.class))).thenAnswer(inv -> inv.getArgument(0));

        com.example.backend.request.DonHangRequest req = new com.example.backend.request.DonHangRequest();
        req.setKhachHangId(77);
        req.setTrangThaiDonHang("pending");
        req.setTrangThaiThanhToan("unpaid");
        req.setNguoiNhan("A");
        req.setSdtNguoiNhan("0900000000");
        req.setTongTien(java.math.BigDecimal.ZERO);
        req.setGiamGia(java.math.BigDecimal.ZERO);
        req.setPhiVanChuyen(java.math.BigDecimal.ZERO);
        req.setNgayDat(java.time.LocalDateTime.now());

        service.create(req);

        verify(khachHangRepository).getReferenceById(77);
    }
}
