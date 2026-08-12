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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
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
        DonHang entity = new DonHang();
        BeanUtils.copyProperties(request, entity,
                "khachHangId", "nhanVienId", "khuyenMaiId", "diaChiGiaoHangId", "giamGia");

        Integer khachHangId = resolveKhachHangIdForCreate(request.getKhachHangId());
        entity.setKhachHang(khachHangRepository.getReferenceById(khachHangId));
        if (request.getNhanVienId() != null)
            entity.setNhanVien(nhanVienRepository.getReferenceById(request.getNhanVienId()));
        if (request.getDiaChiGiaoHangId() != null)
            entity.setDiaChiGiaoHang(diaChiGiaoHangRepository.getReferenceById(request.getDiaChiGiaoHangId()));

        if (request.getKhuyenMaiId() != null && request.getPhieuGiamGiaCaNhanId() != null)
            throw new IllegalArgumentException("Không thể dùng đồng thời mã khuyến mãi và voucher cá nhân");

        PhieuGiamGiaCaNhan phieuDangDung = null;
        if (request.getKhuyenMaiId() != null) {
            KhuyenMai khuyenMai = khuyenMaiRepository.findById(request.getKhuyenMaiId())
                    .orElseThrow(() -> new IllegalArgumentException("Mã khuyến mãi không tồn tại"));
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

        if (phieuDangDung != null) {
            phieuDangDung.setDaSuDung(true);
            phieuDangDung.setDonHang(saved);
            phieuGiamGiaCaNhanRepository.save(phieuDangDung);
        }

        sseService.notifyNewOrder(saved.getId());
        return saved;
    }

    private static final Map<String, Set<String>> CHUYEN_TRANG_THAI_DON_HANG = Map.of(
            "pending",              Set.of("confirmed", "cancelled"),
            "confirmed",            Set.of("processing", "cancelled"),
            "processing",           Set.of("shipping", "cancelled"),
            "shipping",             Set.of("out_for_delivery", "cancelled"),
            "out_for_delivery",     Set.of("awaiting_confirmation", "cancelled"),
            "awaiting_confirmation", Set.of("delivered"),
            "delivered",            Set.of("returned"),
            "cancelled",            Set.of(),
            "returned",             Set.of()
    );

    private void kiemTraChuyenTrangThai(String trangThaiCu, String trangThaiMoi, String kenhBan) {
        if (trangThaiCu == null || trangThaiMoi == null || trangThaiCu.equals(trangThaiMoi)) return;
        if ("in_store".equals(kenhBan) && "confirmed".equals(trangThaiCu) && "delivered".equals(trangThaiMoi)) return;
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
                "id", "khachHangId", "nhanVienId", "khuyenMaiId", "diaChiGiaoHangId");

        entity.setKhachHang(khachHangRepository.getReferenceById(request.getKhachHangId()));
        entity.setNhanVien(request.getNhanVienId() != null
                ? nhanVienRepository.getReferenceById(request.getNhanVienId()) : null);
        entity.setKhuyenMai(request.getKhuyenMaiId() != null
                ? khuyenMaiRepository.getReferenceById(request.getKhuyenMaiId()) : null);
        entity.setDiaChiGiaoHang(request.getDiaChiGiaoHangId() != null
                ? diaChiGiaoHangRepository.getReferenceById(request.getDiaChiGiaoHangId()) : null);

        DonHang saved = donHangRepository.save(entity);

        if ("cancelled".equals(request.getTrangThaiDonHang()) && !"cancelled".equals(oldStatus)) {
            releaseSerialsToStock(id);
            giaiPhongKhuyenMaiVoucher(saved);
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
        donHangRepository.save(donHang);
        sseService.notifyOrderUpdate(id);
    }

    private void releaseSerialsToStock(Integer donHangId) {
        List<ChiTietDonHang> items = chiTietDonHangRepository.findEntityByDonHangId(donHangId);
        for (ChiTietDonHang item : items) {
            if (item.getChiTietSanPham() != null) {
                item.getChiTietSanPham().setTrangThai("trong_kho");
                chiTietSanPhamRepository.save(item.getChiTietSanPham());
            }
            for (ChiTietDonHangSerial link : chiTietDonHangSerialRepository.findByChiTietDonHang_Id(item.getId())) {
                link.getChiTietSanPham().setTrangThai("trong_kho");
                chiTietSanPhamRepository.save(link.getChiTietSanPham());
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

        releaseSerialsToStock(id);
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
            throw new AccessDeniedException("Không xác định được người dùng");
        if (!"khach_hang".equals(tk.getChucVu().getMaChucVu()))
            return requestedKhachHangId;
        if (tk.getKhachHang() == null)
            throw new AccessDeniedException("Tài khoản chưa liên kết khách hàng");
        return tk.getKhachHang().getKhachHangId();
    }

    private BigDecimal tinhGiamGiaKhuyenMai(KhuyenMai khuyenMai, BigDecimal tongTien) {
        if (!"active".equals(khuyenMai.getTrangThai()))
            throw new IllegalArgumentException("Mã khuyến mãi không còn hiệu lực");
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
        if (!"pending".equals(donHang.getTrangThaiDonHang()))
            throw new IllegalArgumentException("Đơn hàng phải ở trạng thái 'Chờ xác nhận' mới xác nhận được");

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
}
