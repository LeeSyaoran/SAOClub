package com.example.backend.service;

import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.entity.LichSuTonKho;
import com.example.backend.entity.NhanVien;
import com.example.backend.exception.DuplicateSerialException;
import com.example.backend.repository.BienTheSanPhamRepository;
import com.example.backend.repository.ChiTietSanPhamRepository;
import com.example.backend.repository.LichSuTonKhoRepository;
import com.example.backend.repository.NhanVienRepository;
import com.example.backend.repository.PhieuNhapKhoRepository;
import com.example.backend.request.ChiTietSanPhamRequest;
import com.example.backend.request.SerialLockRequest;
import com.example.backend.request.SerialUnlockRequest;
import com.example.backend.response.ChiTietSanPhamResponse;
import com.example.backend.response.SerialLockResponse;
import com.example.backend.response.WarrantyStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.service.SseService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChiTietSanPhamService {

    private static final Logger log = LoggerFactory.getLogger(ChiTietSanPhamService.class);

    private final ChiTietSanPhamRepository chiTietSanPhamRepository;
    private final BienTheSanPhamRepository bienTheSanPhamRepository;
    private final PhieuNhapKhoRepository phieuNhapKhoRepository;
    private final LichSuTonKhoRepository lichSuTonKhoRepository;
    private final SseService sseService;
    private final NhanVienRepository nhanVienRepository;

    @Autowired
    public ChiTietSanPhamService(
            ChiTietSanPhamRepository chiTietSanPhamRepository,
            BienTheSanPhamRepository bienTheSanPhamRepository,
            PhieuNhapKhoRepository phieuNhapKhoRepository,
            LichSuTonKhoRepository lichSuTonKhoRepository,
            SseService sseService,
            NhanVienRepository nhanVienRepository) {
        this.chiTietSanPhamRepository = chiTietSanPhamRepository;
        this.bienTheSanPhamRepository = bienTheSanPhamRepository;
        this.phieuNhapKhoRepository = phieuNhapKhoRepository;
        this.lichSuTonKhoRepository = lichSuTonKhoRepository;
        this.sseService = sseService;
        this.nhanVienRepository = nhanVienRepository;
    }

    public List<ChiTietSanPhamResponse> hienThiChiTietSanPham() {
        // KHÔNG gọi releaseOrphanSerials() ở đây — trước đây đã gây bug:
        // mỗi khi POS chọn serial (giu_hang) rồi bumpSerialEvent() → SerialManager reload
        // → GET /api/chi-tiet-san-pham → cleanup chạy → serial chưa có chi_tiet_don_hang
        // (vì đơn chưa được tạo) bị reset về trong_kho ngay lập tức.
        // Cleanup đã được chuyển sang @Scheduled 30 phút/lần bên dưới — chỉ dọn serial
        // thực sự bị kẹt lâu (đóng tab, đơn đã hủy), không đụng đến POS cart đang active.
        return chiTietSanPhamRepository.hienThiChiTietSanPham();
    }

    // Dọn rác serial 'giu_hang' mồ côi — chạy định kỳ 30 phút thay vì mỗi lần load bảng
    // để tránh reset nhầm serial đang trong giỏ POS chưa tạo đơn.
    // fixedDelay = 1800000ms = 30 phút — đủ lớn để một phiên POS bình thường kết thúc.
    @Scheduled(fixedDelay = 1800000)
    public void scheduledReleaseOrphanSerials() {
        try {
            int released = releaseOrphanSerials();
            if (released > 0) log.info("[Scheduled] Đã giải phóng {} serial 'giu_hang' mồ côi", released);
        } catch (Exception ex) {
            log.warn("[Scheduled] Lỗi khi dọn rác serial orphan", ex);
        }
    }

    @Transactional(readOnly = true)
    public List<ChiTietSanPhamResponse> getByBienTheId(Integer bienTheId) {
        return chiTietSanPhamRepository.findByBienTheId(bienTheId);
    }

    @Transactional(readOnly = true)
    public List<ChiTietSanPhamResponse> getByPhieuNhapId(Integer phieuNhapId) {
        try {
            return chiTietSanPhamRepository.findByPhieuNhapId(phieuNhapId);
        } catch (Exception e) {
            log.error("Lỗi khi tải serial theo phiếu nhập {}: {}", phieuNhapId, e.getMessage(), e);
            throw e;
        }
    }

    public ChiTietSanPham getById(Integer id) {
        return chiTietSanPhamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chi tiết sản phẩm không tồn tại với id: " + id));
    }

    public ChiTietSanPham create(ChiTietSanPhamRequest request) {
        if (chiTietSanPhamRepository.existsBySoSerialAndDaXoaFalse(request.getSoSerial())) {
            throw new DuplicateSerialException(request.getSoSerial());
        }
        ChiTietSanPham entity = new ChiTietSanPham();
        BeanUtils.copyProperties(request, entity, "bienTheId", "phieuNhapId");
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getBienTheId()));
        if (request.getPhieuNhapId() != null) {
            entity.setPhieuNhap(phieuNhapKhoRepository.getReferenceById(request.getPhieuNhapId()));
        }
        return chiTietSanPhamRepository.save(entity);
    }

    @Transactional
    public ChiTietSanPham update(Integer id, ChiTietSanPhamRequest request) {
        ChiTietSanPham entity;
        if ("giu_hang".equals(request.getTrangThai())) {
            entity = chiTietSanPhamRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> new IllegalArgumentException("Chi tiết sản phẩm không tồn tại với id: " + id));
            if (!"trong_kho".equals(entity.getTrangThai()))
                throw new IllegalArgumentException("Serial này vừa được giữ/bán bởi giao dịch khác, vui lòng chọn serial khác");
        } else {
            entity = getById(id);
        }
        if (chiTietSanPhamRepository.existsBySoSerialAndChiTietIdNotAndDaXoaFalse(request.getSoSerial(), id)) {
            throw new DuplicateSerialException(request.getSoSerial());
        }
        BeanUtils.copyProperties(request, entity, "chiTietId", "bienTheId", "phieuNhapId");
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getBienTheId()));
        if (request.getPhieuNhapId() != null) {
            entity.setPhieuNhap(phieuNhapKhoRepository.getReferenceById(request.getPhieuNhapId()));
        }
        return chiTietSanPhamRepository.save(entity);
    }

    @Transactional
    public void delete(Integer id) {
        ChiTietSanPham entity = getById(id);
        if (!"trong_kho".equals(entity.getTrangThai())) {
            throw new IllegalArgumentException("Chỉ được xóa serial đang ở trạng thái \"Trong kho\" (chưa bán/chưa sử dụng)");
        }
        // Ghi lịch sử trước khi xóa — để audit
        LichSuTonKho lichSu = new LichSuTonKho();
        lichSu.setBienThe(entity.getBienThe());
        lichSu.setChiTietSanPham(entity);
        lichSu.setLoaiBienDong("xoa_serial");
        lichSu.setSoLuongThayDoi(-1);
        lichSu.setDonHang(null);
        lichSu.setNgayTao(LocalDateTime.now());
        lichSu.setGhiChu("Xóa serial #" + entity.getChiTietId() + " (" + entity.getSoSerial() + ") khỏi kho");
        lichSuTonKhoRepository.save(lichSu);
        chiTietSanPhamRepository.deleteById(id);
    }

    public List<WarrantyStatusResponse> getStillUnderWarranty() {
        LocalDateTime now = LocalDateTime.now();
        List<WarrantyStatusResponse> list = chiTietSanPhamRepository.timSerialDaBanCoGiaoHang();
        list.forEach(w -> w.setNgayHetBaoHanh(w.getNgayGiaoThucTe().plusMonths(w.getBaoHanhThang() == null ? 0 : w.getBaoHanhThang())));
        list.removeIf(w -> !w.getNgayHetBaoHanh().isAfter(now));
        list.sort(Comparator.comparing(WarrantyStatusResponse::getNgayHetBaoHanh));
        return list;
    }

    // Dọn rác: tìm tất cả serial 'giu_hang' mà KHÔNG liên kết chi_tiet_don_hang nào
    // (orphan - đơn đã bị xóa/hủy hoặc user đóng tab POS trước khi đơn được tạo), đưa về
    // 'trong_kho' và ghi lich_su_ton_kho để audit. Tái dùng loai_bien_dong='giu_hang' (vì
    // schema check constraint đã chấp nhận) thay vì thêm enum mới — phân biệt bằng ghi_chu.
    // Trả về số serial đã release để ghi log (hoặc trả về client trong tương lai).
    // @Transactional để đảm bảo nếu 1 dòng lich_su_ton_kho lỗi thì rollback cả lô thay đổi
    // trạng thái serial — tránh trạng thái nửa vời. Public để có thể gọi riêng nếu cần;
    // hiện tại được gọi tự động từ hienThiChiTietSanPham() mỗi lần load bảng serial.
    @Transactional
    public int releaseOrphanSerials() {
        List<ChiTietSanPham> orphans = chiTietSanPhamRepository.findOrphanGiuHangSerials();
        if (orphans.isEmpty()) return 0;
        LocalDateTime now = LocalDateTime.now();
        for (ChiTietSanPham serial : orphans) {
            serial.setTrangThai("trong_kho");
            chiTietSanPhamRepository.save(serial);
            LichSuTonKho lichSu = new LichSuTonKho();
            lichSu.setBienThe(serial.getBienThe());
            lichSu.setChiTietSanPham(serial);
            lichSu.setLoaiBienDong("giu_hang");
            lichSu.setSoLuongThayDoi(+1); // trả về kho = +1 đơn vị khả dụng
            lichSu.setDonHang(null);       // orphan, không thuộc đơn nào
            lichSu.setNgayTao(now);
            lichSu.setGhiChu("Giải phóng serial 'giu_hang' mồ côi (đơn đã hủy/xóa) — serial #" + serial.getChiTietId() + " (" + serial.getSoSerial() + ")");
            lichSuTonKhoRepository.save(lichSu);
        }
        return orphans.size();
    }

    // ========== SERIAL LOCKING ==========

    // Lock timeout: 5 phut
    private static final int LOCK_TIMEOUT_SECONDS = 300;

    @Transactional
    public SerialLockResponse lockSerials(SerialLockRequest request) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredBefore = now.minusSeconds(LOCK_TIMEOUT_SECONDS);

        // Thu lock — chi serial dang trong_kho va chua bi lock (hoac lock da het han)
        int locked = chiTietSanPhamRepository.lockSerials(
            request.getChiTietIds(),
            request.getNhanVienId(),
            now,
            request.getSessionId(),
            expiredBefore
        );

        List<Integer> failedIds = new ArrayList<>();
        List<ChiTietSanPham> lockedSerials = new ArrayList<>();
        if (locked < request.getChiTietIds().size()) {
            // Tim serial bi loi
            for (Integer id : request.getChiTietIds()) {
                ChiTietSanPham serial = chiTietSanPhamRepository.findById(id).orElse(null);
                if (serial == null) {
                    failedIds.add(id);
                } else if (!"trong_kho".equals(serial.getTrangThai())) {
                    failedIds.add(id); // serial khong o trang thai trong_kho
                } else if (serial.getLockedAt() != null
                           && serial.getLockedAt().isAfter(expiredBefore)
                           && !request.getSessionId().equals(serial.getLockSession())) {
                    failedIds.add(id); // serial dang bi lock boi nguoi khac
                }
            }
        }

        // Broadcast SSE cho serial vua lock thanh cong
        if (locked > 0) {
            // Lay thong tin nhan vien lock
            NhanVien nv = nhanVienRepository.findById(request.getNhanVienId()).orElse(null);
            String lockedByTen = nv != null ? nv.getHoTen() : "";
            for (Integer id : request.getChiTietIds()) {
                if (!failedIds.contains(id)) {
                    ChiTietSanPham serial = chiTietSanPhamRepository.findById(id).orElse(null);
                    if (serial != null) {
                        sseService.notifySerialLocked(id, serial.getSoSerial(), request.getNhanVienId(), lockedByTen);
                    }
                }
            }
        }

        return new SerialLockResponse(
            failedIds.isEmpty(),
            locked,
            failedIds,
            failedIds.isEmpty() ? "Lock thanh cong" : "Mot so serial bi loi"
        );
    }

    @Transactional
    public int unlockSerials(SerialUnlockRequest request) {
        int unlocked = chiTietSanPhamRepository.unlockSerials(
            request.getChiTietIds(),
            request.getSessionId()
        );

        // Broadcast SSE cho serial vua unlock
        if (unlocked > 0) {
            for (Integer id : request.getChiTietIds()) {
                ChiTietSanPham serial = chiTietSanPhamRepository.findById(id).orElse(null);
                if (serial != null) {
                    sseService.notifySerialUnlocked(id, serial.getSoSerial());
                }
            }
        }
        return unlocked;
    }

    // Scheduled: giai phong lock da het han (chay moi 1 phut)
    @Scheduled(fixedDelay = 60000)
    public void releaseExpiredLocks() {
        LocalDateTime expiredBefore = LocalDateTime.now().minusSeconds(LOCK_TIMEOUT_SECONDS);
        List<ChiTietSanPham> expired = chiTietSanPhamRepository.findExpiredLocks(expiredBefore);
        for (ChiTietSanPham serial : expired) {
            serial.setLockedBy(null);
            serial.setLockedAt(null);
            serial.setLockSession(null);
            chiTietSanPhamRepository.save(serial);
            // Broadcast unlock khi auto-release
            sseService.notifySerialUnlocked(serial.getChiTietId(), serial.getSoSerial());
        }
        if (!expired.isEmpty()) {
            log.info("[Scheduled] Released {} expired serial locks", expired.size());
        }
    }

    /**
     * POS barcode scan — tim san pham theo barcode (bien_the) hoac so_serial (chi_tiet_san_pham).
     * Tra du lieu day du de POS hien thi: thong tin san pham + bien the + serial.
     */
    public ResponseEntity<?> scanBarcode(String code) {
        List<ChiTietSanPham> results = chiTietSanPhamRepository
                .findActiveByBarcodeOrSoSerial(code, code);

        if (results.isEmpty()) {
            boolean deleted = chiTietSanPhamRepository
                    .existsDeletedByBarcodeOrSoSerial(code, code);
            if (deleted) {
                return ResponseEntity.status(410).body(Map.of("error", "Mã " + code + " đã bị xóa khỏi hệ thống"));
            }
            return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy mã " + code));
        }

        //Uu tien serial da_ban, neu khong co lay dau tien
        ChiTietSanPham serial = results.stream()
                .filter(c -> "da_ban".equals(c.getTrangThai()))
                .findFirst()
                .orElse(results.get(0));

        // Build response
        var bt = serial.getBienThe();
        var sp = bt != null ? bt.getSanPham() : null;

        var result = new java.util.LinkedHashMap<String, Object>();
        result.put("chiTietId", serial.getChiTietId());
        result.put("soSerial", serial.getSoSerial());
        result.put("trangThai", serial.getTrangThai());
        result.put("ngayNhapKho", serial.getNgayNhapKho());
        result.put("trangThaiLabel", switch (serial.getTrangThai()) {
            case "trong_kho" -> "Còn trong kho";
            case "da_ban" -> "Đã bán";
            case "dang_xu_ly" -> "Đang xử lý bảo hành";
            case "loi_bao_hanh" -> "Lỗi bảo hành";
            case "khong_duoc_ban" -> "Không được bán";
            case "tra_lai_ncc" -> "Trả lại NCC";
            default -> serial.getTrangThai();
        });

        if (bt != null) {
            result.put("bienTheId", bt.getBienTheId());
            result.put("maSku", bt.getMaSku());
            result.put("barcode", bt.getBarcode());
            result.put("giaBan", bt.getGiaBan());
            result.put("mauSac", bt.getMauSac());
            result.put("baoHanhThang", bt.getBaoHanhThang());
            result.put("hinhAnh", bt.getHinhAnhBienThe());
            if (sp != null) {
                result.put("sanPhamId", sp.getSanPhamId());
                result.put("tenSanPham", sp.getTenSanPham());
                result.put("cpu", bt.getCpu() != null ? bt.getCpu().getTenCpu() : null);
                result.put("ram", bt.getRam() != null ? bt.getRam().getDungLuong() : null);
                result.put("oCung", bt.getOCung() != null ? bt.getOCung().getLoaiOcung() : null);
                result.put("gpu", bt.getGpu() != null ? bt.getGpu().getTenGpu() : null);
                result.put("kichThuocManHinh", bt.getKichThuocManHinh());
                result.put("heDieuHanh", bt.getHeDieuHanh());
            }
        }
        return ResponseEntity.ok(result);
    }
}
