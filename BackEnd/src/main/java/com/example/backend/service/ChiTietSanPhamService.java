package com.example.backend.service;

import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.entity.LichSuTonKho;
import com.example.backend.exception.DuplicateSerialException;
import com.example.backend.repository.BienTheSanPhamRepository;
import com.example.backend.repository.ChiTietSanPhamRepository;
import com.example.backend.repository.LichSuTonKhoRepository;
import com.example.backend.repository.PhieuNhapKhoRepository;
import com.example.backend.request.ChiTietSanPhamRequest;
import com.example.backend.response.ChiTietSanPhamResponse;
import com.example.backend.response.WarrantyStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class ChiTietSanPhamService {

    private static final Logger log = LoggerFactory.getLogger(ChiTietSanPhamService.class);

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;
    @Autowired
    private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Autowired
    private PhieuNhapKhoRepository phieuNhapKhoRepository;
    @Autowired
    private LichSuTonKhoRepository lichSuTonKhoRepository;

    public List<ChiTietSanPhamResponse> hienThiChiTietSanPham() {
        // Tự động dọn rác serial 'giu_hang' bị kẹt mỗi khi frontend load bảng serial.
        // An toàn vì query NOT EXISTS đảm bảo không đụng serial đang liên kết đơn thật.
        // Xảy ra khi đơn đã bị xóa/hủy nhưng serial đã được set 'giu_hang' từ POS trước đó,
        // hoặc user đóng tab giữa chừng trước khi đơn được tạo chính thức.
        // Try-catch để nếu lỗi DB (vd constraint, deadlock) thì VẪN trả danh sách serial
        // cho user xem — không nên để bảng trống vì cleanup phụ trợi lỗi.
        try {
            int released = releaseOrphanSerials();
            if (released > 0) log.info("Đã tự động giải phóng {} serial 'giu_hang' mồ côi", released);
        } catch (Exception ex) {
            log.warn("Lỗi khi dọn rác serial orphan (không ảnh hưởng đến danh sách hiển thị)", ex);
        }
        return chiTietSanPhamRepository.hienThiChiTietSanPham();
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
}
