package com.example.backend.service;

import com.example.backend.entity.*;
import com.example.backend.repository.*;
import com.example.backend.request.PhieuNhapKhoRequest;
import com.example.backend.request.PhieuNhapSerialDraft;
import com.example.backend.response.PhieuNhapKhoResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PhieuNhapKhoService {

    private static final Logger log = LoggerFactory.getLogger(PhieuNhapKhoService.class);

    @Autowired
    private PhieuNhapKhoRepository phieuNhapKhoRepository;
    @Autowired
    private NhaCungCapRepository nhaCungCapRepository;
    @Autowired
    private NhanVienRepository nhanVienRepository;
    @Autowired
    private ChiTietPhieuNhapRepository chiTietPhieuNhapRepository;
    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;
    @Autowired
    private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Autowired
    private LichSuTonKhoRepository lichSuTonKhoRepository;
    @Autowired
    private NhanVienRepository nhanVienRepo;
    @Autowired
    private ChiTietDonHangSerialRepository chiTietDonHangSerialRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<PhieuNhapKhoResponse> hienThiPhieuNhapKho() {
        return phieuNhapKhoRepository.hienThiPhieuNhapKho();
    }

    public PhieuNhapKho getById(Integer id) {
        return phieuNhapKhoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phiếu nhập kho không tồn tại với id: " + id));
    }

    public PhieuNhapKho create(PhieuNhapKhoRequest request) {
        PhieuNhapKho entity = new PhieuNhapKho();
        BeanUtils.copyProperties(request, entity, "nhaCungCapId", "nhanVienId", "serials");
        entity.setNhaCungCap(nhaCungCapRepository.getReferenceById(request.getNhaCungCapId()));
        if (request.getNhanVienId() != null)
            entity.setNhanVien(nhanVienRepository.getReferenceById(request.getNhanVienId()));
        // Lưu serial draft vào JSON column
        if (request.getSerials() != null && !request.getSerials().isEmpty()) {
            try {
                entity.setSerialDraftJson(objectMapper.writeValueAsString(request.getSerials()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Lỗi khi serialize serial draft", e);
            }
        }
        return phieuNhapKhoRepository.save(entity);
    }

    public PhieuNhapKho update(Integer id, PhieuNhapKhoRequest request) {
        PhieuNhapKho entity = getById(id);
        BeanUtils.copyProperties(request, entity, "phieuNhapId", "nhaCungCapId", "nhanVienId");
        entity.setNhaCungCap(nhaCungCapRepository.getReferenceById(request.getNhaCungCapId()));
        entity.setNhanVien(request.getNhanVienId() != null
                ? nhanVienRepository.getReferenceById(request.getNhanVienId()) : null);
        return phieuNhapKhoRepository.save(entity);
    }

    @Transactional
    public void delete(Integer id) {
        if (!phieuNhapKhoRepository.existsById(id))
            throw new IllegalArgumentException("Phiếu nhập kho không tồn tại với id: " + id);
        PhieuNhapKho phieu = getById(id);

        // Dọn serial thuộc phiếu: chỉ soft-delete các serial 'trong_kho' CHƯA được link với
        // đơn hàng nào. Serial đã bán / đang giữ phải giữ nguyên để bảo toàn tham chiếu từ
        // chi_tiet_don_hang_serial (lịch sử bảo hành + đối soát đơn). Soft-delete (không xóa
        // cứng) vì so_serial UNIQUE — nếu trước đó user từng bán-trả-hàng thì xóa cứng sẽ phá
        // vỡ khả năng nhập lại cùng serial (giải thích triệu chứng user báo: phiếu cũ đã xóa
        // nhưng serial vẫn nằm trong DB với da_xoa=false, cản trở nhập lại).
        List<ChiTietSanPham> serials = chiTietSanPhamRepository.findByPhieuNhap_PhieuNhapIdIncludingDeleted(id);
        int deleted = 0;
        for (ChiTietSanPham s : serials) {
            if (Boolean.TRUE.equals(s.getDaXoa())) continue;
            if (!"trong_kho".equals(s.getTrangThai())) continue;
            if (chiTietDonHangSerialRepository.existsByChiTietSanPham_ChiTietId(s.getChiTietId())) continue;
            s.setDaXoa(true);
            s.setGhiChu("Đã xóa cùng phiếu nhập #" + id + " (" + phieu.getMaPhieuNhap() + ")");
            chiTietSanPhamRepository.save(s);
            LichSuTonKho audit = new LichSuTonKho();
            audit.setBienThe(s.getBienThe());
            audit.setChiTietSanPham(s);
            audit.setLoaiBienDong("xoa_serial");
            audit.setSoLuongThayDoi(-1);
            audit.setPhieuNhapKho(null); // phiếu sắp xóa, không set FK
            audit.setNhanVien(phieu.getNhanVien());
            audit.setGhiChu("Xóa serial " + s.getSoSerial() + " do xóa phiếu nhập #" + id);
            audit.setNgayTao(LocalDateTime.now());
            lichSuTonKhoRepository.save(audit);
            deleted++;
        }
        if (deleted > 0)
            log.info("Đã soft-delete {} serial khi xóa phiếu nhập #{}", deleted, id);

        chiTietPhieuNhapRepository.deleteByPhieuNhapKho_PhieuNhapId(id);
        phieuNhapKhoRepository.deleteById(id);
    }

    // ── Duyệt phiếu nhập kho: tạo serial vào kho ───────────────────────────

    /**
     * Check serial trùng với DB — dùng khi tạo phiếu mới (chưa có phieuNhapId).
     * Trả về list serial trùng.
     */
    public List<String> kiemTraSerialVoiDb(List<String> serials) {
        if (serials == null || serials.isEmpty()) return Collections.emptyList();
        return chiTietSanPhamRepository.findBySoSerialInAndDaXoaFalse(serials)
                .stream()
                .map(ChiTietSanPham::getSoSerial)
                .collect(Collectors.toList());
    }

    /**
     * Trả về map: bienTheId → danh sách serial trùng trong DB.
     * Nếu map rỗng → không có trùng, có thể duyệt.
     */
    public Map<Integer, List<String>> kiemTraSerialTrung(Integer phieuNhapId) {
        PhieuNhapKho phieu = getById(phieuNhapId);
        if (phieu.getSerialDraftJson() == null || phieu.getSerialDraftJson().isBlank())
            return Collections.emptyMap();

        List<PhieuNhapSerialDraft> drafts;
        try {
            drafts = objectMapper.readValue(phieu.getSerialDraftJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, PhieuNhapSerialDraft.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Lỗi khi parse serial draft JSON", e);
        }

        // Gom tất cả serial, nhóm theo bienTheId
        Map<Integer, List<String>> trungTheoBienThe = new LinkedHashMap<>();
        for (PhieuNhapSerialDraft draft : drafts) {
            if (draft.getSerials() == null || draft.getSerials().isEmpty()) continue;
            // Check với DB hiện tại
            List<ChiTietSanPham> existing = chiTietSanPhamRepository.findBySoSerialInAndDaXoaFalse(draft.getSerials());
            for (ChiTietSanPham ctsp : existing) {
                if (ctsp.getBienThe().getBienTheId().equals(draft.getBienTheId())) {
                    trungTheoBienThe
                            .computeIfAbsent(draft.getBienTheId(), k -> new ArrayList<>())
                            .add(ctsp.getSoSerial());
                }
            }
        }
        return trungTheoBienThe;
    }

    /**
     * Duyệt phiếu: tạo chi_tiet_san_pham + lich_su_ton_kho cho mỗi serial.
     * Nếu serial trùng trong DB hoặc trùng cross-row trong cùng phiếu → throw.
     */
    @Transactional
    public void approve(Integer phieuNhapId) {
        PhieuNhapKho phieu = getById(phieuNhapId);
        if (!"cho_duyet".equals(phieu.getTrangThai()))
            throw new IllegalArgumentException("Chỉ có thể duyệt phiếu ở trạng thái 'Chờ duyệt'");

        if (phieu.getSerialDraftJson() == null || phieu.getSerialDraftJson().isBlank()) {
            // Không có serial → chỉ đổi trạng thái
            phieu.setTrangThai("hoan_thanh");
            phieuNhapKhoRepository.save(phieu);
            return;
        }

        List<PhieuNhapSerialDraft> drafts;
        try {
            drafts = objectMapper.readValue(phieu.getSerialDraftJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, PhieuNhapSerialDraft.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Lỗi khi parse serial draft JSON", e);
        }

        // Check trùng cross-row trong cùng phiếu
        Set<String> seen = new HashSet<>();
        for (PhieuNhapSerialDraft draft : drafts) {
            if (draft.getSerials() == null) continue;
            for (String serial : draft.getSerials()) {
                if (serial == null || serial.isBlank()) continue;
                if (!seen.add(serial.trim())) {
                    throw new IllegalArgumentException("Serial '" + serial.trim() + "' xuất hiện nhiều lần trong cùng phiếu nhập. Vui lòng sửa lại trước khi duyệt.");
                }
            }
        }

        // Check trùng với DB
        Map<Integer, List<String>> trungDb = kiemTraSerialTrung(phieuNhapId);
        if (!trungDb.isEmpty()) {
            StringBuilder sb = new StringBuilder("Serial đã tồn tại trong kho: ");
            for (Map.Entry<Integer, List<String>> e : trungDb.entrySet()) {
                BienTheSanPham bt = bienTheSanPhamRepository.findById(e.getKey()).orElse(null);
                String ten = bt != null ? bt.getMaSku() : "ID " + e.getKey();
                sb.append(ten).append(": ").append(String.join(", ", e.getValue())).append("; ");
            }
            throw new IllegalArgumentException(sb.toString());
        }

        LocalDateTime now = LocalDateTime.now();
        // Insert serial + chi_tiet_phieu_nhap cùng lúc
        for (PhieuNhapSerialDraft draft : drafts) {
            if (draft.getSerials() == null || draft.getSerials().isEmpty()) continue;
            BienTheSanPham bienThe = bienTheSanPhamRepository.findById(draft.getBienTheId())
                    .orElseThrow(() -> new IllegalArgumentException("Biến thể không tồn tại: " + draft.getBienTheId()));
            int serialCount = 0;
            for (String soSerial : draft.getSerials()) {
                if (soSerial == null || soSerial.isBlank()) continue;
                ChiTietSanPham ctsp = new ChiTietSanPham();
                ctsp.setBienThe(bienThe);
                ctsp.setPhieuNhap(phieu);
                ctsp.setSoSerial(soSerial.trim());
                ctsp.setTrangThai("trong_kho");
                ctsp.setNgayNhapKho(now);
                ctsp.setGhiChu(phieu.getMaPhieuNhap() != null ? phieu.getMaPhieuNhap() : "PN" + phieuNhapId);
                chiTietSanPhamRepository.save(ctsp);

                // Log lịch sử tồn kho
                LichSuTonKho log = new LichSuTonKho();
                log.setBienThe(bienThe);
                log.setChiTietSanPham(ctsp);
                log.setLoaiBienDong("nhap");
                log.setSoLuongThayDoi(1);
                log.setPhieuNhapKho(phieu);
                log.setNhanVien(phieu.getNhanVien());
                log.setGhiChu("Nhập kho từ phiếu " + phieu.getMaPhieuNhap());
                log.setNgayTao(now);
                lichSuTonKhoRepository.save(log);
                serialCount++;
            }
            // Insert dòng chi tiết phiếu nhập (để frontend hiển thị đúng bảng SKU + đơn giá)
            if (serialCount > 0) {
                ChiTietPhieuNhap ctpn = new ChiTietPhieuNhap();
                ctpn.setPhieuNhapKho(phieu);
                ctpn.setBienThe(bienThe);
                ctpn.setSoLuong(serialCount);
                ctpn.setDonGiaNhap(draft.getDonGia() != null ? draft.getDonGia() : BigDecimal.ZERO);
                chiTietPhieuNhapRepository.save(ctpn);
            }
        }

        phieu.setTrangThai("hoan_thanh");
        phieu.setSerialDraftJson(null);
        phieuNhapKhoRepository.save(phieu);
    }
}
