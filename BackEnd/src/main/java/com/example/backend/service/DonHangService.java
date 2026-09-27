package com.example.backend.service;

import com.example.backend.entity.ChiTietDonHang;
import com.example.backend.entity.ChiTietDonHangSerial;
import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.entity.DonHang;
import com.example.backend.entity.LichSuDonHang;
import com.example.backend.entity.ThanhToan;
import com.example.backend.entity.LichSuTonKho;
import com.example.backend.entity.PhieuTraHang;
import com.example.backend.entity.PhieuBaoHanh;
import com.example.backend.entity.KhuyenMai;
import com.example.backend.entity.PhieuGiamGiaCaNhan;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.*;
import com.example.backend.request.DonHangRequest;
import com.example.backend.request.XacNhanDonHangLineRequest;
import com.example.backend.request.XacNhanDonHangRequest;
import com.example.backend.response.DonHangResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import com.example.backend.request.ChiTietDonHangRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DonHangService {

    private static final Logger log = LoggerFactory.getLogger(DonHangService.class);

    @Autowired
    private DonHangRepository donHangRepository;
    @Autowired
    private SseService sseService;
    @Autowired
    private KhachHangRepository khachHangRepository;
    @Autowired
    private NhanVienRepository nhanVienRepository;
    @Autowired
    private KhuyenMaiRepository khuyenMaiRepository;
    @Autowired
    private DiaChiGiaoHangRepository diaChiGiaoHangRepository;
    @Autowired
    private ChiTietDonHangRepository chiTietDonHangRepository;
    @Autowired
    private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Autowired
    private ThanhToanRepository thanhToanRepository;
    @Autowired
    private LichSuTonKhoRepository lichSuTonKhoRepository;
    @Autowired
    private PhieuTraHangRepository phieuTraHangRepository;
    @Autowired
    private PhieuBaoHanhRepository phieuBaoHanhRepository;
    @Autowired
    private LichSuDonHangRepository lichSuDonHangRepository;
    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;
    @Autowired
    private ChiTietDonHangSerialRepository chiTietDonHangSerialRepository;
    @Autowired
    private PhieuGiamGiaCaNhanRepository phieuGiamGiaCaNhanRepository;
    @Autowired
    private TaiKhoanRepository taiKhoanRepository;
    @Autowired
    private KhuyenMaiService khuyenMaiService;
    @Autowired
    private EntityManager entityManager;

    public Page<DonHangResponse> hienThiDonHang(Integer khachHangId, Pageable pageable) {
        return donHangRepository.hienThiDonHang(resolveKhachHangIdForList(khachHangId), pageable);
    }

    public DonHang getById(Integer id) {
        return donHangRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại với id: " + id));
    }

    public DonHang getByIdChoNguoiXem(Integer id) {
        DonHang donHang = getById(id);
        if (!isStaffOrOwner(donHang.getKhachHang().getKhachHangId()))
            throw new AccessDeniedException("Không có quyền xem đơn hàng này");
        return donHang;
    }

    @Transactional
    public DonHang create(DonHangRequest request) {
        // ── IDEMPOTENCY CHECK: Nếu key đã tồn tại, trả lại đơn cũ thay vì tạo mới ──
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            var existing = donHangRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                log.info("[Idempotency] Đơn hàng đã tồn tại với key={}, trả về đơn #{}",
                        request.getIdempotencyKey(), existing.get().getId());
                return existing.get();
            }
        }

        DonHang entity = new DonHang();
        BeanUtils.copyProperties(request, entity,
                "khachHangId", "nhanVienId", "khuyenMaiId", "diaChiGiaoHangId", "giamGia");

        Integer khachHangId = resolveKhachHangIdForCreate(request.getKhachHangId());
        entity.setKhachHang(khachHangRepository.getReferenceById(khachHangId));

        // Tự động hoàn thiện số điện thoại & địa chỉ cho hồ sơ khách hàng nếu hồ sơ chưa có
        try {
            khachHangRepository.findById(khachHangId).ifPresent(kh -> {
                boolean changed = false;
                if ((kh.getSoDienThoai() == null || kh.getSoDienThoai().isBlank())
                        && request.getSdtNguoiNhan() != null && !request.getSdtNguoiNhan().isBlank()) {
                    kh.setSoDienThoai(request.getSdtNguoiNhan());
                    changed = true;
                }
                if ((kh.getDiaChi() == null || kh.getDiaChi().isBlank())
                        && request.getDiaChiGiaoHangText() != null && !request.getDiaChiGiaoHangText().isBlank()) {
                    kh.setDiaChi(request.getDiaChiGiaoHangText());
                    changed = true;
                }
                if (changed) {
                    khachHangRepository.save(kh);
                }
            });
        } catch (Exception ex) {
            log.warn("[CustomerSync] Không thể tự động đồng bộ SĐT/Địa chỉ vào khách hàng #{}: {}", khachHangId, ex.getMessage());
        }
        if (request.getNhanVienId() != null)
            entity.setNhanVien(nhanVienRepository.getReferenceById(request.getNhanVienId()));
        if (request.getDiaChiGiaoHangId() != null)
            entity.setDiaChiGiaoHang(diaChiGiaoHangRepository.getReferenceById(request.getDiaChiGiaoHangId()));

        // Ghi lại phương thức thanh toán & idempotency key
        if (request.getPhuongThucThanhToan() != null)
            entity.setPhuongThucThanhToan(request.getPhuongThucThanhToan());
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank())
            entity.setIdempotencyKey(request.getIdempotencyKey());

        if (request.getKhuyenMaiId() != null && request.getPhieuGiamGiaCaNhanId() != null)
            throw new IllegalArgumentException("Không thể dùng đồng thời mã khuyến mãi và voucher cá nhân");

        PhieuGiamGiaCaNhan phieuDangDung = null;
        if (request.getKhuyenMaiId() != null) {
            KhuyenMai khuyenMai = khuyenMaiRepository.findById(request.getKhuyenMaiId())
                    .orElseThrow(() -> new IllegalArgumentException("Mã khuyến mãi không tồn tại"));

            // Kiểm tra khuyến mãi có áp dụng cho sản phẩm trong đơn hàng không
            List<Integer> sanPhamIds = request.getSanPhamIds();
            if (sanPhamIds != null && !sanPhamIds.isEmpty()) {
                boolean coSanPhamApDung = false;
                for (Integer spId : sanPhamIds) {
                    if (khuyenMaiService.kiemTraSanPhamApDung(khuyenMai.getKhuyenMaiId(), spId)) {
                        coSanPhamApDung = true;
                        break;
                    }
                }
                if (!coSanPhamApDung) {
                    throw new IllegalArgumentException("Mã khuyến mãi không áp dụng cho sản phẩm trong đơn hàng");
                }
            }

            entity.setKhuyenMai(khuyenMai);
            entity.setGiamGia(tinhGiamGiaKhuyenMai(khuyenMai, request.getTongTien()));
        } else if (request.getPhieuGiamGiaCaNhanId() != null) {
            phieuDangDung = phieuGiamGiaCaNhanRepository.findWithLockByPhieuId(request.getPhieuGiamGiaCaNhanId())
                    .orElseThrow(() -> new IllegalArgumentException("Voucher không tồn tại"));
            if (!phieuDangDung.getKhachHang().getKhachHangId().equals(khachHangId))
                throw new IllegalArgumentException("Voucher không thuộc về khách hàng này");
            if (Boolean.TRUE.equals(phieuDangDung.getDaSuDung()))
                throw new IllegalArgumentException("Voucher đã được sử dụng");
            if (LocalDateTime.now().isAfter(phieuDangDung.getNgayHetHan()))
                throw new IllegalArgumentException("Voucher đã hết hạn");
            entity.setGiamGia(tinhGiamGiaVoucher(phieuDangDung, request.getTongTien()));
        } else {
            entity.setGiamGia(BigDecimal.ZERO);
        }

        DonHang saved = donHangRepository.save(entity);
        entityManager.refresh(saved);

        // Lưu thông tin voucher cá nhân nếu có
        if (phieuDangDung != null) {
            phieuDangDung.setDaSuDung(true);
            phieuDangDung.setDonHang(saved);
            phieuGiamGiaCaNhanRepository.save(phieuDangDung);
        }

        sseService.notifyNewOrder(saved.getId());
        return saved;
    }

    // Checkout trực tuyến hoàn tất đơn hàng
    @Transactional
    public DonHang checkoutComplete(DonHangRequest orderReq, List<ChiTietDonHangRequest> items) {
        if (orderReq.getSdtNguoiNhan() == null || orderReq.getSdtNguoiNhan().isBlank()) {
            throw new IllegalArgumentException("Số điện thoại nhận hàng không được để trống");
        }
        if (orderReq.getDiaChiGiaoHangText() == null || orderReq.getDiaChiGiaoHangText().isBlank()) {
            throw new IllegalArgumentException("Địa chỉ giao hàng không được để trống");
        }
        DonHang order = create(orderReq);
        for (ChiTietDonHangRequest item : items) {
            item.setDonHangId(order.getId());
            
            int soLuong = item.getSoLuong() != null ? item.getSoLuong() : 1;
            List<ChiTietSanPham> available = chiTietSanPhamRepository
                    .findByBienThe_BienTheIdAndTrangThaiOrderByNgayNhapKhoAsc(item.getBienTheId(), "trong_kho");
            if (available.size() < soLuong)
                throw new IllegalArgumentException(
                        "Không đủ hàng trong kho: cần " + soLuong + ", còn " + available.size());
            List<ChiTietSanPham> assignedSerials = available.subList(0, soLuong);

            ChiTietDonHang entity = buildChiTietDonHang(item);
            entity.setChiTietSanPham(assignedSerials.get(0));
            ChiTietDonHang saved = chiTietDonHangRepository.save(entity);

            boolean online = "online".equals(order.getKenhBan());
            String trangThaiMoi = online ? "trong_kho" : "da_ban";

            for (ChiTietSanPham serial : assignedSerials) {
                serial.setTrangThai(trangThaiMoi);
                chiTietSanPhamRepository.save(serial);
                ChiTietDonHangSerial link = new ChiTietDonHangSerial();
                link.setChiTietDonHang(saved);
                link.setChiTietSanPham(serial);
                chiTietDonHangSerialRepository.save(link);
            }

            LichSuTonKho lichSu = new LichSuTonKho();
            lichSu.setBienThe(entity.getBienThe());
            lichSu.setChiTietSanPham(assignedSerials.get(0));
            lichSu.setLoaiBienDong(online ? "giu_hang" : "xuat_ban");
            lichSu.setSoLuongThayDoi(-assignedSerials.size());
            lichSu.setDonHang(order);
            lichSu.setNgayTao(LocalDateTime.now());
            lichSu.setGhiChu(online
                    ? "Giữ chỗ (Online) — đơn #" + order.getId()
                    : "Bán hàng — đơn #" + order.getId());
            lichSuTonKhoRepository.save(lichSu);
        }
        return order;
    }

    private ChiTietDonHang buildChiTietDonHang(ChiTietDonHangRequest req) {
        ChiTietDonHang entity = new ChiTietDonHang();
        entity.setDonHang(donHangRepository.getReferenceById(req.getDonHangId()));
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(req.getBienTheId()));
        entity.setSoLuong(req.getSoLuong() != null ? req.getSoLuong() : 1);
        entity.setDonGia(entity.getBienThe().getGiaBan());
        entity.setGiamGiaDong(BigDecimal.ZERO);
        return entity;
    }

    private static final Map<String, Set<String>> CHUYEN_TRANG_THAI_DON_HANG = Map.of(
            "pending",              Set.of("confirmed", "cancelled"),
            "confirmed",            Set.of("processing", "cancelled"),
            "processing",           Set.of("out_for_delivery", "shipping", "cancelled"),
            "shipping",             Set.of("out_for_delivery", "cancelled"),
            "out_for_delivery",     Set.of("delivered", "awaiting_confirmation", "cancelled"),
            "awaiting_confirmation", Set.of("delivered"),
            "delivered",            Set.of("returned"),
            "cancelled",            Set.of(),
            "returned",             Set.of()
    );

    private void kiemTraChuyenTrangThai(String trangThaiCu, String trangThaiMoi, String kenhBan) {
        if (trangThaiCu == null || trangThaiMoi == null || trangThaiCu.equals(trangThaiMoi)) return;
        if ("in_store".equals(kenhBan) && "delivered".equals(trangThaiMoi)) return;
        if (!CHUYEN_TRANG_THAI_DON_HANG.getOrDefault(trangThaiCu, Set.of()).contains(trangThaiMoi))
            throw new IllegalArgumentException(
                    "Không thể chuyển trạng thái đơn hàng từ \"" + trangThaiCu + "\" sang \"" + trangThaiMoi + "\"");
    }

    @Transactional
    public DonHang update(Integer id, DonHangRequest request) {
        DonHang entity = getById(id);
        String oldStatus = entity.getTrangThaiDonHang();
        kiemTraChuyenTrangThai(oldStatus, request.getTrangThaiDonHang(), entity.getKenhBan());
        BeanUtils.copyProperties(request, entity,
                "id", "khachHangId", "nhanVienId", "khuyenMaiId", "diaChiGiaoHangId", "phuongThucThanhToan", "idempotencyKey");
        if (request.getPhuongThucThanhToan() != null && !request.getPhuongThucThanhToan().isBlank()) {
            entity.setPhuongThucThanhToan(request.getPhuongThucThanhToan());
        }
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            entity.setIdempotencyKey(request.getIdempotencyKey());
        }

        entity.setKhachHang(khachHangRepository.getReferenceById(request.getKhachHangId()));
        entity.setNhanVien(request.getNhanVienId() != null
                ? nhanVienRepository.getReferenceById(request.getNhanVienId()) : null);
        entity.setKhuyenMai(request.getKhuyenMaiId() != null
                ? khuyenMaiRepository.getReferenceById(request.getKhuyenMaiId()) : null);
        entity.setDiaChiGiaoHang(request.getDiaChiGiaoHangId() != null
                ? diaChiGiaoHangRepository.getReferenceById(request.getDiaChiGiaoHangId()) : null);

        DonHang saved = donHangRepository.save(entity);

        if ("cancelled".equals(request.getTrangThaiDonHang()) && !"cancelled".equals(oldStatus)) {
            releaseSerialsToStock(id, true);
            giaiPhongKhuyenMaiVoucher(saved);
        }

        // Kích hoạt bảo hành khi giao hàng (POS hoặc online xác nhận đã nhận)
        if ("delivered".equals(request.getTrangThaiDonHang()) && !"delivered".equals(oldStatus)) {
            kichHoatBaoHanhTuDong(saved);
        }

        sseService.notifyOrderUpdate(id);

        return saved;
    }

    @Transactional
    public void xacNhanDaNhanHang(Integer id) {
        DonHang donHang = getById(id);
        if (!isStaffOrOwner(donHang.getKhachHang().getKhachHangId()))
            throw new AccessDeniedException("Không có quyền xác nhận đơn hàng này");
        kiemTraChuyenTrangThai(donHang.getTrangThaiDonHang(), "delivered", donHang.getKenhBan());
        donHang.setTrangThaiDonHang("delivered");
        if (donHang.getNgayGiaoThucTe() == null)
            donHang.setNgayGiaoThucTe(LocalDateTime.now());
        donHangRepository.save(donHang);
        kichHoatBaoHanhTuDong(donHang);
        sseService.notifyOrderUpdate(id);
    }

    // Giao hàng và kích hoạt bảo hành tại quầy
    @Transactional
    public DonHang giaoHang(Integer id, LocalDateTime ngayGiaoThucTe) {
        DonHang donHang = getById(id);
        String trangThaiCu = donHang.getTrangThaiDonHang();
        kiemTraChuyenTrangThai(trangThaiCu, "delivered", donHang.getKenhBan());
        donHang.setTrangThaiDonHang("delivered");
        donHang.setNgayGiaoThucTe(ngayGiaoThucTe != null ? ngayGiaoThucTe : LocalDateTime.now());
        DonHang saved = donHangRepository.save(donHang);
        kichHoatBaoHanhTuDong(saved);
        sseService.notifyOrderUpdate(id);
        return saved;
    }

    // Kích hoạt bảo hành tự động cho serial trong đơn
    private void kichHoatBaoHanhTuDong(DonHang donHang) {
        List<ChiTietDonHang> items = chiTietDonHangRepository.findEntityByDonHangId(donHang.getId());
        for (ChiTietDonHang item : items) {
            if (item.getChiTietSanPham() != null) {
                ChiTietSanPham serial = item.getChiTietSanPham();
                if (!"da_ban".equals(serial.getTrangThai())) {
                    serial.setTrangThai("da_ban");
                    chiTietSanPhamRepository.save(serial);
                }
            }
            // Duyệt qua ChiTietDonHangSerial nếu có (đơn online chọn serial riêng)
            List<ChiTietDonHangSerial> links =
                    chiTietDonHangSerialRepository.findByChiTietDonHang_Id(item.getId());
            for (ChiTietDonHangSerial link : links) {
                ChiTietSanPham serial = link.getChiTietSanPham();
                if (!"da_ban".equals(serial.getTrangThai())) {
                    serial.setTrangThai("da_ban");
                    chiTietSanPhamRepository.save(serial);
                }
            }
        }
    }

    private void releaseSerialsToStock(Integer donHangId) {
        releaseSerialsToStock(donHangId, false);
    }

    private void releaseSerialsToStock(Integer donHangId, boolean broadcastUnlock) {
        List<ChiTietDonHang> items = chiTietDonHangRepository.findEntityByDonHangId(donHangId);
        for (ChiTietDonHang item : items) {
            if (item.getChiTietSanPham() != null) {
                ChiTietSanPham serial = item.getChiTietSanPham();
                serial.setTrangThai("trong_kho");
                chiTietSanPhamRepository.save(serial);
                if (broadcastUnlock) {
                    sseService.notifySerialUnlocked(serial.getChiTietId(), serial.getSoSerial());
                }
            }
            for (ChiTietDonHangSerial link : chiTietDonHangSerialRepository.findByChiTietDonHang_Id(item.getId())) {
                ChiTietSanPham serial = link.getChiTietSanPham();
                serial.setTrangThai("trong_kho");
                chiTietSanPhamRepository.save(serial);
                if (broadcastUnlock) {
                    sseService.notifySerialUnlocked(serial.getChiTietId(), serial.getSoSerial());
                }
            }
        }
    }

    private void giaiPhongKhuyenMaiVoucher(DonHang donHang) {
        if (donHang.getKhuyenMai() != null) {
            KhuyenMai khuyenMai = donHang.getKhuyenMai();
            int daDung = khuyenMai.getSoLanDaDung() != null ? khuyenMai.getSoLanDaDung() : 0;
            khuyenMai.setSoLanDaDung(Math.max(0, daDung - 1));
            khuyenMaiRepository.save(khuyenMai);
        }
        phieuGiamGiaCaNhanRepository.findByDonHang_Id(donHang.getId()).ifPresent(phieu -> {
            phieu.setDaSuDung(false);
            phieuGiamGiaCaNhanRepository.save(phieu);
        });
    }

    @Transactional
    public void delete(Integer id) {
        DonHang donHang = donHangRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại với id: " + id));
        if (!isStaffOrOwner(donHang.getKhachHang().getKhachHangId()))
            throw new AccessDeniedException("Không có quyền xóa đơn hàng này");

        releaseSerialsToStock(id, true);
        giaiPhongKhuyenMaiVoucher(donHang);
        List<ChiTietDonHang> items = chiTietDonHangRepository.findEntityByDonHangId(id);
        for (ChiTietDonHang item : items) {
            chiTietDonHangSerialRepository.deleteByChiTietDonHang_Id(item.getId());
        }
        chiTietDonHangRepository.deleteAll(items);
        lichSuTonKhoRepository.deleteAll(lichSuTonKhoRepository.findByDonHang_Id(id));
        thanhToanRepository.deleteAll(thanhToanRepository.findByDonHang_Id(id));
        donHangRepository.deleteById(id);
    }

    private TaiKhoan currentAccount() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return taiKhoanRepository.findByUsername(username).orElse(null);
    }

    private boolean isStaffOrOwner(Integer khachHangId) {
        TaiKhoan tk = currentAccount();
        if (tk == null) return false;
        if (!"khach_hang".equals(tk.getChucVu().getMaChucVu())) return true;
        return tk.getKhachHang() != null && khachHangId.equals(tk.getKhachHang().getKhachHangId());
    }

    private Integer resolveKhachHangIdForCreate(Integer requestedKhachHangId) {
        TaiKhoan tk = currentAccount();
        if (tk == null)
            return requestedKhachHangId; // Anonymous checkout — dùng khachHangId từ request
        if (!"khach_hang".equals(tk.getChucVu().getMaChucVu()))
            return requestedKhachHangId; // Staff/admin — dùng khách hàng được chọn
        if (tk.getKhachHang() == null)
            throw new AccessDeniedException("Tài khoản chưa liên kết khách hàng");
        return tk.getKhachHang().getKhachHangId(); // Khách đăng nhập — dùng chính mình
    }

    private BigDecimal tinhGiamGiaKhuyenMai(KhuyenMai khuyenMai, BigDecimal tongTien) {
        if (!"active".equals(khuyenMai.getTrangThai()))
            throw new IllegalArgumentException("Mã khuyến mãi không còn hiệu lực (đã ngừng hoạt động)");
        LocalDateTime now = LocalDateTime.now();
        if (khuyenMai.getNgayBatDau() != null && now.isBefore(khuyenMai.getNgayBatDau()))
            throw new IllegalArgumentException("Mã khuyến mãi chưa đến thời gian áp dụng");
        if (khuyenMai.getNgayKetThuc() != null && now.isAfter(khuyenMai.getNgayKetThuc()))
            throw new IllegalArgumentException("Mã khuyến mãi đã hết hạn");
        if (khuyenMai.getSoLuongToiDa() != null
                && (khuyenMai.getSoLanDaDung() == null ? 0 : khuyenMai.getSoLanDaDung()) >= khuyenMai.getSoLuongToiDa())
            throw new IllegalArgumentException("Mã khuyến mãi đã hết lượt sử dụng");
        if (khuyenMai.getDonHangToiThieu() != null && tongTien.compareTo(khuyenMai.getDonHangToiThieu()) < 0)
            throw new IllegalArgumentException("Đơn hàng chưa đạt giá trị tối thiểu để áp dụng mã này");
        return tinhGiamGia(khuyenMai.getLoai(), khuyenMai.getGiaTri(), khuyenMai.getGiaTriToiDa(), tongTien);
    }

    private BigDecimal tinhGiamGiaVoucher(PhieuGiamGiaCaNhan phieu, BigDecimal tongTien) {
        if (phieu.getDonHangToiThieu() != null && tongTien.compareTo(phieu.getDonHangToiThieu()) < 0)
            throw new IllegalArgumentException("Đơn hàng chưa đạt giá trị tối thiểu để dùng voucher này");
        return tinhGiamGia(phieu.getLoai(), phieu.getGiaTri(), phieu.getGiaTriToiDa(), tongTien);
    }

    private BigDecimal tinhGiamGia(String loai, BigDecimal giaTri, BigDecimal giaTriToiDa, BigDecimal tongTien) {
        if ("percent".equals(loai)) {
            BigDecimal giam = tongTien.multiply(giaTri).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            return giaTriToiDa != null ? giam.min(giaTriToiDa) : giam;
        }
        return giaTri != null ? giaTri : BigDecimal.ZERO;
    }

    private Integer resolveKhachHangIdForList(Integer requestedKhachHangId) {
        TaiKhoan tk = currentAccount();
        if (tk == null)
            throw new AccessDeniedException("Không xác định được người dùng");
        if (!"khach_hang".equals(tk.getChucVu().getMaChucVu()))
            return requestedKhachHangId;
        if (tk.getKhachHang() == null)
            throw new AccessDeniedException("Tài khoản chưa liên kết khách hàng");
        return tk.getKhachHang().getKhachHangId();
    }

    @Transactional
    public void xacNhanDonHang(Integer donHangId, XacNhanDonHangRequest request) {
        DonHang donHang = getById(donHangId);
        if (!"online".equals(donHang.getKenhBan()))
            throw new IllegalArgumentException("Chỉ đơn hàng online mới cần chọn serial trước khi xác nhận");
        if (!"pending".equals(donHang.getTrangThaiDonHang()) && !"confirmed".equals(donHang.getTrangThaiDonHang()))
            throw new IllegalArgumentException("Đơn hàng phải ở trạng thái 'Chờ xác nhận' hoặc 'Đã lên đơn' mới cập nhật được serial");

        for (XacNhanDonHangLineRequest line : request.getLines()) {
            ChiTietDonHang item = chiTietDonHangRepository.findById(line.getChiTietDonHangId())
                    .orElseThrow(() -> new IllegalArgumentException("Dòng đơn hàng không tồn tại với id: " + line.getChiTietDonHangId()));
            if (!item.getDonHang().getId().equals(donHangId))
                throw new IllegalArgumentException("Dòng #" + item.getId() + " không thuộc đơn hàng này");

            List<Integer> serialIds = line.getSerialIds();
            if (new HashSet<>(serialIds).size() != serialIds.size())
                throw new IllegalArgumentException("Dòng #" + item.getId() + " chọn trùng serial");
            if (serialIds.size() != item.getSoLuong())
                throw new IllegalArgumentException(
                        "Dòng #" + item.getId() + " cần đúng " + item.getSoLuong() + " serial, đã chọn " + serialIds.size());

            List<ChiTietDonHangSerial> existingLinks = chiTietDonHangSerialRepository.findByChiTietDonHang_Id(item.getId());
            Set<Integer> reservedForThisLine = existingLinks.stream()
                    .map(l -> l.getChiTietSanPham().getChiTietId())
                    .collect(Collectors.toSet());

            List<ChiTietSanPham> finalSerials = new ArrayList<>();
            for (Integer serialId : serialIds) {
                ChiTietSanPham serial = chiTietSanPhamRepository.findByIdForUpdate(serialId)
                        .orElseThrow(() -> new IllegalArgumentException("Serial không tồn tại với id: " + serialId));
                if (!serial.getBienThe().getBienTheId().equals(item.getBienThe().getBienTheId()))
                    throw new IllegalArgumentException("Serial " + serial.getSoSerial() + " không thuộc đúng sản phẩm của dòng #" + item.getId());
                boolean daGiuChoDongNay = reservedForThisLine.contains(serialId);
                if (!"trong_kho".equals(serial.getTrangThai()) && !daGiuChoDongNay)
                    throw new IllegalArgumentException("Serial " + serial.getSoSerial() + " không còn khả dụng, vui lòng chọn lại");
                finalSerials.add(serial);
            }

            for (ChiTietDonHangSerial link : existingLinks) {
                if (!serialIds.contains(link.getChiTietSanPham().getChiTietId())) {
                    link.getChiTietSanPham().setTrangThai("trong_kho");
                    chiTietSanPhamRepository.save(link.getChiTietSanPham());
                }
            }
            chiTietDonHangSerialRepository.deleteByChiTietDonHang_Id(item.getId());
            chiTietDonHangSerialRepository.flush();

            for (ChiTietSanPham serial : finalSerials) {
                serial.setTrangThai("da_ban");
                chiTietSanPhamRepository.save(serial);
                ChiTietDonHangSerial link = new ChiTietDonHangSerial();
                link.setChiTietDonHang(item);
                link.setChiTietSanPham(serial);
                chiTietDonHangSerialRepository.save(link);
            }

            item.setChiTietSanPham(finalSerials.get(0));
            chiTietDonHangRepository.save(item);
        }

        if (request.getNhanVienId() != null) {
            nhanVienRepository.findById(request.getNhanVienId()).ifPresent(donHang::setNhanVien);
        }
        donHang.setTrangThaiDonHang("confirmed");
        donHangRepository.save(donHang);
        sseService.notifyOrderUpdate(donHangId);
    }

    @Transactional
    public void recalculateTongTien(Integer orderId) {
        DonHang order = getById(orderId);
        List<ChiTietDonHang> items = chiTietDonHangRepository.findEntityByDonHangId(orderId);
        BigDecimal total = items.stream()
            .map(item -> {
                BigDecimal line = item.getDonGia().multiply(BigDecimal.valueOf(item.getSoLuong()));
                return item.getGiamGiaDong() != null ? line.subtract(item.getGiamGiaDong()) : line;
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTongTien(total);
        donHangRepository.save(order);
    }

    @Transactional
    public void mergeOrders(Integer targetId, List<Integer> sourceIds) {
        DonHang target = getById(targetId);
        for (Integer sourceId : sourceIds) {
            if (sourceId.equals(targetId)) continue;
            DonHang source = getById(sourceId);
            if ("pending".equals(source.getTrangThaiDonHang()))
                throw new IllegalArgumentException(
                        "Đơn #" + sourceId + " chưa được xác nhận, không thể gộp");
            List<ChiTietDonHang> items = chiTietDonHangRepository.findEntityByDonHangId(sourceId);
            for (ChiTietDonHang item : items) {
                item.setDonHang(target);
                chiTietDonHangRepository.save(item);
            }
            for (ThanhToan tt : thanhToanRepository.findByDonHang_Id(sourceId)) {
                tt.setDonHang(target);
                thanhToanRepository.save(tt);
            }
            for (LichSuTonKho lstk : lichSuTonKhoRepository.findByDonHang_Id(sourceId)) {
                lstk.setDonHang(target);
                lichSuTonKhoRepository.save(lstk);
            }
            for (PhieuTraHang ptr : phieuTraHangRepository.findByDonHang_Id(sourceId)) {
                ptr.setDonHang(target);
                phieuTraHangRepository.save(ptr);
            }
            for (PhieuBaoHanh pbh : phieuBaoHanhRepository.findByDonHang_Id(sourceId)) {
                pbh.setDonHang(target);
                phieuBaoHanhRepository.save(pbh);
            }
            for (LichSuDonHang lsdh : lichSuDonHangRepository.findByDonHangId(sourceId)) {
                lsdh.setDonHangId(target.getId());
                lichSuDonHangRepository.save(lsdh);
            }
            donHangRepository.deleteById(sourceId);
        }
        recalculateTongTien(targetId);
    }

    // Tự động hủy đơn hàng chờ thanh toán quá hạn
    @Scheduled(fixedDelay = 300000)
    public void autoCancelPendingOrders() {
        try {
            List<DonHang> expired = donHangRepository.findPendingOrdersOlderThan(
                LocalDateTime.now().minusMinutes(30)
            );
            for (DonHang order : expired) {
                try {
                    String oldStatus = order.getTrangThaiDonHang();
                    order.setTrangThaiDonHang("cancelled");
                    order.setGhiChu((order.getGhiChu() != null ? order.getGhiChu() + "; " : "") + "[Auto] Hủy tự động: quá 30 phút không thanh toán");
                    donHangRepository.save(order);

                    // Ghi lich su
                    LichSuDonHang lichSu = new LichSuDonHang();
                    lichSu.setDonHangId(order.getId());
                    lichSu.setTrangThaiCu(oldStatus);
                    lichSu.setTrangThaiMoi("cancelled");
                    lichSu.setThoiGian(LocalDateTime.now());
                    lichSuDonHangRepository.save(lichSu);

                    // Tra serial ve kho + broadcast unlock
                    releaseSerialsToStock(order.getId(), true);

                    log.info("[AutoCancel] Đã hủy đơn #{} (pending) — quá 30 phút không thanh toán", order.getId());
                    sseService.notifyOrderUpdate(order.getId());
                } catch (Exception ex) {
                    log.warn("[AutoCancel] Lỗi khi hủy đơn #{}: {}", order.getId(), ex.getMessage());
                }
            }
        } catch (Exception ex) {
            log.warn("[AutoCancel] Lỗi khi quet don hang pending: {}", ex.getMessage());
        }
    }

    // ── POS helpers ──────────────────────────────────────────────────────────────

    /** Don hang gan nhat cua 1 khach hang (cho quick-select) */
    public List<DonHangResponse> getRecentByKhachHang(Integer khachHangId) {
        return donHangRepository.findRecentByKhachHang(khachHangId);
    }

    /** Tat ca don trong 30 ngay qua, moi nhat truoc (cho POS recent orders) */
    public List<DonHangResponse> getRecentForPos() {
        return donHangRepository.findRecentForPos(LocalDateTime.now().minusDays(30));
    }

    /** Top khach hang theo chi tieu (cho POS quick-select) */
    public List<?> getTopCustomers(int limit) {
        return khachHangRepository.chiTieuTheoKhachHang(
                LocalDateTime.now().minusMonths(6),
                LocalDateTime.now(),
                PageRequest.of(0, limit));
    }

    // ── RECONCILIATION: Đối soát đơn hàng đã giao nhưng chưa được đánh dấu thanh toán ──

    /**
     * Chạy mỗi ngày lúc 23:00 — phát hiện đơn đã delivered nhưng trangThaiThanhToan vẫn 'unpaid'.
     * Với COD: tự động đánh dấu là 'paid' (giao hàng xong = thu tiền xong).
     * Với QR/Visa: ghi cảnh báo vào log để kế toán kiểm tra thủ công.
     */
    @Scheduled(cron = "0 0 23 * * *")
    @Transactional
    public void dailyReconciliation() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusHours(2);
            List<DonHang> deliveredUnpaid = donHangRepository
                    .findDeliveredUnpaid(cutoff);

            int autoPaid = 0;
            int warnings = 0;

            for (DonHang order : deliveredUnpaid) {
                String method = order.getPhuongThucThanhToan();
                boolean isCod = method == null || "tien_mat".equals(method);

                if (isCod) {
                    // COD — giao hàng xong → mặc định đã thu tiền
                    order.setTrangThaiThanhToan("paid");
                    donHangRepository.save(order);

                    // Tạo bản ghi ThanhToan
                    ThanhToan tt = new ThanhToan();
                    tt.setDonHang(order);
                    tt.setNgayThanhToan(LocalDateTime.now());
                    tt.setPhuongThucThanhToan("tien_mat");
                    tt.setSoTien(order.getThanhTien());
                    tt.setTrangThai("paid");
                    tt.setGhiChu("[Reconcile] Tự động ghi nhận COD khi giao hàng thành công");
                    thanhToanRepository.save(tt);
                    autoPaid++;
                } else {
                    // QR / Visa — cần kiểm tra thủ công
                    log.warn("[Reconcile] Đơn #{} ({}) đã delivered nhưng chưa paid — phương thức: {}",
                            order.getId(), order.getMaDonHang(), method);
                    warnings++;
                }
            }

            log.info("[Reconcile] Hoàn tất: tự xử lý {} đơn COD, cảnh báo {} đơn QR/Visa cần kiểm tra",
                    autoPaid, warnings);
        } catch (Exception ex) {
            log.warn("[Reconcile] Lỗi khi chạy đối soát: {}", ex.getMessage());
        }
    }

    // ── AUTO-CANCEL EXPIRED HELD ORDERS ──────────────────────────────────────────
    // Chạy mỗi 15 phút: tìm đơn online có serial đang bị lock quá 90 phút → hủy + trả serial về kho

    /**
     * Tự động hủy đơn online quá 90 phút chưa thanh toán.
     * Điều kiện:
     *   - kenh_ban = 'online'
     *   - trang_thai NOT IN (delivered, cancelled, returned)
     *   - thanh_toan = unpaid
     *   - serial đang bị lock đã hết 90 phút (locked_at < cutoff)
     * Serial trong đơn được trả về kho (trang_thai → trong_kho).
     */
    @Scheduled(fixedRate = 900_000) // 15 phút
    @Transactional
    public void autoCancelExpiredHeldOrders() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(90);
            List<DonHang> expired = donHangRepository.findOnlineOrdersWithExpiredHeldSerials(cutoff);
            if (expired.isEmpty()) return;

            int count = 0;
            for (DonHang order : expired) {
                try {
                    // 1. Trả serial về kho (re-use existing logic)
                    releaseSerialsToStock(order.getId(), true);

                    // 2. Giải phóng voucher (re-use existing logic)
                    giaiPhongKhuyenMaiVoucher(order);

                    // 3. Hủy đơn
                    String oldNote = order.getGhiChu() != null ? order.getGhiChu() : "";
                    order.setTrangThaiDonHang("cancelled");
                    order.setGhiChu(oldNote + " [Auto-hủy: serial hết hạn giữ 90 phút]");
                    donHangRepository.save(order);

                    // 4. Ghi lịch sử
                    LichSuDonHang lichSu = new LichSuDonHang();
                    lichSu.setDonHangId(order.getId());
                    lichSu.setTrangThaiCu(order.getTrangThaiDonHang());
                    lichSu.setTrangThaiMoi("cancelled");
                    lichSu.setThoiGian(LocalDateTime.now());
                    lichSuDonHangRepository.save(lichSu);

                    count++;
                    log.info("[AutoCancel] Đơn #{} ({}) bị hủy tự động — serial đã trả về kho",
                            order.getId(), order.getMaDonHang());
                } catch (Exception ex) {
                    log.warn("[AutoCancel] Lỗi khi hủy đơn #{}: {}", order.getId(), ex.getMessage());
                }
            }
            log.info("[AutoCancel] Hoàn tất: đã hủy {} đơn hết hạn giữ serial", count);
        } catch (Exception ex) {
            log.warn("[AutoCancel] Lỗi khi chạy auto-cancel: {}", ex.getMessage());
        }
    }
}
