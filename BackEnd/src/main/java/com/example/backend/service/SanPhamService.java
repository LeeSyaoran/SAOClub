package com.example.backend.service;

import com.example.backend.entity.BienTheSanPham;
import com.example.backend.entity.SanPham;
import com.example.backend.repository.*;
import com.example.backend.request.SanPhamRequest;
import com.example.backend.response.SanPhamCreatedResponse;
import com.example.backend.response.SanPhamResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SanPhamService {

    @Autowired
    private SanPhamRepository sanPhamRepository;
    @Autowired
    private ThuongHieuRepository thuongHieuRepository;
    @Autowired
    private DanhMucRepository danhMucRepository;
    @Autowired
    private NhaCungCapRepository nhaCungCapRepository;
    @Autowired
    private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Autowired
    private DmCpuRepository dmCpuRepository;
    @Autowired
    private DmRamRepository dmRamRepository;
    @Autowired
    private DmOcungRepository dmOcungRepository;
    @Autowired
    private DmGpuRepository dmGpuRepository;

    // Chuỗi rỗng ("") không khớp ":keyword IS NULL" trong JPQL — chuẩn hóa về null ở đây
    // để ô tìm kiếm trống trên frontend không vô tình lọc mất tất cả kết quả.
    public Page<SanPhamResponse> hienThiSanPham(String keyword, Integer danhMucId,
                                                Integer thuongHieuId, String trangThai,
                                                Pageable pageable) {
        return sanPhamRepository.hienThiSanPham(chuanHoa(keyword), danhMucId, thuongHieuId,
                chuanHoa(trangThai), pageable);
    }

    public SanPham getSanPhamById(Integer sanPhamId) {
        return sanPhamRepository.findById(sanPhamId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại với id: " + sanPhamId));
    }

    /**
     * Tạo SanPham + BienTheSanPham đầu tiên trong CÙNG một transaction.
     * Dòng ton_kho tương ứng do trigger trg_BienThe_TaoTonKho của CSDL tự tạo — đặt ở tầng
     * CSDL thay vì tầng service để mọi đường ghi (import SQL, màn nhập hàng, code sau này)
     * đều có dòng tồn kho, không phụ thuộc lập trình viên có nhớ gọi hay không.
     */
    @Transactional
    public SanPhamCreatedResponse createSanPham(SanPhamRequest request) {
        String maSanPham = chuanHoa(request.getMaSanPham());
        String barcode = chuanHoa(request.getBarcode());
        kiemTraTrungMa(maSanPham, barcode, null);

        SanPham sanPham = new SanPham();
        BeanUtils.copyProperties(request, sanPham, "sanPhamId", "bienTheId", "ngayTao", "maSanPham", "barcode");
        sanPham.setMaSanPham(maSanPham);
        sanPham.setBarcode(barcode);
        sanPham.setNgayTao(request.getNgayTao() != null ? request.getNgayTao() : LocalDateTime.now());

        sanPham.setThuongHieu(thuongHieuRepository.getReferenceById(request.getThuongHieuId()));
        sanPham.setDanhMuc(danhMucRepository.getReferenceById(request.getDanhMucId()));
        if (request.getNhaCungCapId() != null)
            sanPham.setNhaCungCap(nhaCungCapRepository.getReferenceById(request.getNhaCungCapId()));

        SanPham saved = sanPhamRepository.save(sanPham);

        // Người dùng bỏ trống mã thì sinh theo id vừa có, để cột ma_san_pham không bao giờ rỗng
        if (saved.getMaSanPham() == null) {
            saved.setMaSanPham(String.format("SP%04d", saved.getSanPhamId()));
            saved = sanPhamRepository.save(saved);
        }

        BienTheSanPham bt = new BienTheSanPham();
        // Loại trừ ngayTao khỏi BeanUtils: request.ngayTao null sẽ ghi đè null lên cột
        // ngay_tao NOT NULL của bien_the_san_pham và làm cả giao dịch đổ.
        BeanUtils.copyProperties(request, bt, "bienTheId", "ngayTao");
        bt.setNgayTao(request.getNgayTao() != null ? request.getNgayTao() : LocalDateTime.now());
        bt.setSanPham(saved);
        bt.setTrangThai(trangThaiBienThe(request.getTrangThai()));
        ganLinhKien(bt, request);

        BienTheSanPham savedBt = bienTheSanPhamRepository.save(bt);

        return new SanPhamCreatedResponse(saved.getSanPhamId(), saved.getMaSanPham(),
                saved.getBarcode(), savedBt.getBienTheId(), savedBt.getMaSku());
    }

    @Transactional
    public void updateSanPham(Integer sanPhamId, SanPhamRequest request) {
        SanPham sanPham = sanPhamRepository.findById(sanPhamId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại với id: " + sanPhamId));

        String maSanPham = chuanHoa(request.getMaSanPham());
        String barcode = chuanHoa(request.getBarcode());
        kiemTraTrungMa(maSanPham, barcode, sanPhamId);

        BeanUtils.copyProperties(request, sanPham, "sanPhamId", "bienTheId", "ngayTao", "maSanPham", "barcode");
        sanPham.setMaSanPham(maSanPham);
        sanPham.setBarcode(barcode);
        if (request.getNgayTao() != null) sanPham.setNgayTao(request.getNgayTao());

        sanPham.setThuongHieu(thuongHieuRepository.getReferenceById(request.getThuongHieuId()));
        sanPham.setDanhMuc(danhMucRepository.getReferenceById(request.getDanhMucId()));
        sanPham.setNhaCungCap(request.getNhaCungCapId() != null
                ? nhaCungCapRepository.getReferenceById(request.getNhaCungCapId()) : null);

        sanPhamRepository.save(sanPham);

        if (request.getBienTheId() != null) {
            BienTheSanPham bt = bienTheSanPhamRepository.findById(request.getBienTheId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Biến thể không tồn tại với id: " + request.getBienTheId()));

            // Giữ nguyên ngày tạo gốc của biến thể, chỉ cập nhật phần dữ liệu nghiệp vụ
            BeanUtils.copyProperties(request, bt, "bienTheId", "ngayTao");
            bt.setSanPham(sanPham);
            bt.setTrangThai(trangThaiBienThe(request.getTrangThai()));
            ganLinhKien(bt, request);

            bienTheSanPhamRepository.save(bt);
        }
    }

    // Sản phẩm đã có biến thể nào qua giao dịch chưa — dùng để FE hỏi trước khi hiện hộp
    // thoại xóa.
    public boolean hasTransactionHistory(Integer sanPhamId) {
        return bienTheSanPhamRepository.hasTransactionHistoryBySanPhamId(sanPhamId);
    }

    @Transactional
    public void deleteSanPham(Integer sanPhamId) {
        if (!sanPhamRepository.existsById(sanPhamId))
            throw new IllegalArgumentException("Sản phẩm không tồn tại với id: " + sanPhamId);
        for (BienTheSanPham bt : bienTheSanPhamRepository.findBySanPham_SanPhamId(sanPhamId)) {
            bienTheSanPhamRepository.deleteById(bt.getBienTheId());
        }
        sanPhamRepository.deleteById(sanPhamId);
    }

    /* ───────────────────────── Helper ───────────────────────── */

    private void ganLinhKien(BienTheSanPham bt, SanPhamRequest request) {
        bt.setCpu(request.getCpuId() != null ? dmCpuRepository.getReferenceById(request.getCpuId()) : null);
        bt.setRam(request.getRamId() != null ? dmRamRepository.getReferenceById(request.getRamId()) : null);
        bt.setOCung(request.getOCungId() != null ? dmOcungRepository.getReferenceById(request.getOCungId()) : null);
        bt.setGpu(request.getGpuId() != null ? dmGpuRepository.getReferenceById(request.getGpuId()) : null);
    }

    /** Chuỗi rỗng phải về null: hai sản phẩm cùng để barcode "" sẽ đụng unique index. */
    private String chuanHoa(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    /**
     * bien_the_san_pham chỉ nhận active/inactive (CK_bt_trangthai), trong khi san_pham còn
     * nhận thêm ngung_kinh_doanh. Request dùng chung một trường trangThai nên quy đổi tại đây.
     */
    private String trangThaiBienThe(String trangThai) {
        return "active".equalsIgnoreCase(trangThai) ? "active" : "inactive";
    }

    /** Báo lỗi rõ ràng trước khi để SQL Server bắn unique violation khó đọc. */
    private void kiemTraTrungMa(String maSanPham, String barcode, Integer boQuaId) {
        if (maSanPham != null) {
            boolean trung = boQuaId == null
                    ? sanPhamRepository.existsByMaSanPham(maSanPham)
                    : sanPhamRepository.existsByMaSanPhamAndSanPhamIdNot(maSanPham, boQuaId);
            if (trung) throw new IllegalArgumentException("Mã sản phẩm '" + maSanPham + "' đã được dùng");
        }
        if (barcode != null) {
            boolean trung = boQuaId == null
                    ? sanPhamRepository.existsByBarcode(barcode)
                    : sanPhamRepository.existsByBarcodeAndSanPhamIdNot(barcode, boQuaId);
            if (trung) throw new IllegalArgumentException("Barcode '" + barcode + "' đã được dùng");
        }
    }
}