package com.example.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.entity.BienTheSanPham;
import com.example.backend.entity.NhanVien;
import com.example.backend.entity.TonKho;
import com.example.backend.repository.BienTheSanPhamRepository;
import com.example.backend.repository.ChiTietPhieuNhapRepository;
import com.example.backend.repository.DmCpuRepository;
import com.example.backend.repository.DmGpuRepository;
import com.example.backend.repository.DmOcungRepository;
import com.example.backend.repository.DmRamRepository;
import com.example.backend.repository.LichSuTonKhoRepository;
import com.example.backend.repository.SanPhamRepository;
import com.example.backend.repository.TonKhoRepository;
import com.example.backend.request.BienTheSanPhamRequest;
import com.example.backend.response.BienTheSanPhamPublicResponse;
import com.example.backend.response.BienTheSanPhamResponse;

@Service
public class BienTheSanPhamService {

    @Autowired
    private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Autowired
    private SanPhamRepository sanPhamRepository;
    @Autowired
    private DmCpuRepository dmCpuRepository;
    @Autowired
    private DmRamRepository dmRamRepository;
    @Autowired
    private DmOcungRepository dmOcungRepository;
    @Autowired
    private DmGpuRepository dmGpuRepository;
    @Autowired
    private ChiTietPhieuNhapRepository chiTietPhieuNhapRepository;
    @Autowired
    private TonKhoRepository tonKhoRepository;
    @Autowired
    private LichSuTonKhoRepository lichSuTonKhoRepository;
    @Autowired
    private LichSuThayDoiSanPhamService lichSuThayDoiSanPhamService;

    public List<BienTheSanPhamResponse> hienThiBienTheSanPham() {
        return bienTheSanPhamRepository.hienThiBienTheSanPham();
    }

    public Page<BienTheSanPhamResponse> hienThiBienTheSanPham(Pageable pageable) {
        return bienTheSanPhamRepository.hienThiBienTheSanPham(pageable);
    }

    public List<BienTheSanPhamPublicResponse> hienThiBienTheSanPhamPublic() {
        return bienTheSanPhamRepository.hienThiBienTheSanPhamPublic();
    }

    public BienTheSanPhamPublicResponse getPublicById(Integer id) {
        return bienTheSanPhamRepository.findPublicById(id)
                .orElseThrow(() -> new IllegalArgumentException("Biến thể sản phẩm không tồn tại với id: " + id));
    }

    public BienTheSanPhamResponse getResponseById(Integer id) {
        return bienTheSanPhamRepository.findResponseById(id)
                .orElseThrow(() -> new IllegalArgumentException("Biến thể sản phẩm không tồn tại với id: " + id));
    }

    public BienTheSanPham getById(Integer id) {
        return bienTheSanPhamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Biến thể sản phẩm không tồn tại với id: " + id));
    }

    @Transactional
    public BienTheSanPham create(BienTheSanPhamRequest request) {
        kiemTraDuLieuTaoMoi(request);
        String barcode = chuanHoa(request.getBarcode());
        kiemTraTrungBarcode(barcode, null);
        kiemTraTrungMaSku(request.getMaSku(), null);

        BienTheSanPham entity = new BienTheSanPham();
        // Sao chép các thuộc tính từ request
        BeanUtils.copyProperties(request, entity,
                "bienTheId", "sanPhamId", "cpuId", "ramId", "oCungId", "gpuId", "barcode", "ngayTao");
        if ("cho_nhap_hang".equalsIgnoreCase(request.getTrangThai())) {
            entity.setTrangThai("active");
        }
        entity.setBarcode(barcode);
        // Khởi tạo ngày tạo nếu chưa có
        if (entity.getNgayTao() == null) entity.setNgayTao(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));

        entity.setSanPham(sanPhamRepository.getReferenceById(request.getSanPhamId()));
        entity.setCpu(request.getCpuId() != null ? dmCpuRepository.getReferenceById(request.getCpuId()) : null);
        entity.setRam(request.getRamId() != null ? dmRamRepository.getReferenceById(request.getRamId()) : null);
        entity.setOCung(request.getOCungId() != null ? dmOcungRepository.getReferenceById(request.getOCungId()) : null);
        entity.setGpu(request.getGpuId() != null ? dmGpuRepository.getReferenceById(request.getGpuId()) : null);

        BienTheSanPham saved = bienTheSanPhamRepository.save(entity);
        if (tonKhoRepository != null && tonKhoRepository.findByBienTheBienTheId(saved.getBienTheId()).isEmpty()) {
            TonKho tk = new TonKho();
            tk.setBienThe(saved);
            tk.setSoLuongTon(0);
            tk.setSoLuongGiu(0);
            tk.setTonKhoToiThieu(5);
            tk.setNgayTao(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
            tk.setNgayCapNhat(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
            tonKhoRepository.save(tk);
        }
        NhanVien nguoiSua = lichSuThayDoiSanPhamService.nguoiSuaHienTai();
        Integer spId = entity.getSanPham() != null ? entity.getSanPham().getSanPhamId() : request.getSanPhamId();
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(spId, saved.getBienTheId(), "bien_the", "maSku", null, saved.getMaSku(), nguoiSua);
        return saved;
    }

    private void kiemTraDuLieuTaoMoi(BienTheSanPhamRequest request) {
        if (request == null || request.getSanPhamId() == null) {
            throw new IllegalArgumentException("Phiên bản phải thuộc về một sản phẩm");
        }
        if (chuanHoa(request.getMauSac()) == null) {
            throw new IllegalArgumentException("Phiên bản phải có màu sắc");
        }
        if (request.getCpuId() == null) {
            throw new IllegalArgumentException("Phiên bản phải chọn CPU");
        }
        if (request.getRamId() == null) {
            throw new IllegalArgumentException("Phiên bản phải chọn RAM");
        }
        if (request.getOCungId() == null) {
            throw new IllegalArgumentException("Phiên bản phải chọn ổ cứng");
        }
        if (request.getGpuId() == null) {
            throw new IllegalArgumentException("Phiên bản phải chọn GPU");
        }
        if (request.getGiaNhap() == null || request.getGiaNhap().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá vốn của phiên bản phải lớn hơn 0");
        }
        if (request.getGiaBan() == null || request.getGiaBan().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá bán của phiên bản phải lớn hơn 0");
        }
        if (request.getGiaBan().compareTo(request.getGiaNhap()) < 0) {
            throw new IllegalArgumentException("Giá bán của phiên bản không được nhỏ hơn giá vốn");
        }
        if (request.getTrongLuongKg() != null) {
            if (request.getTrongLuongKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Trọng lượng (kg) phải lớn hơn 0");
            }
            if (request.getTrongLuongKg().compareTo(new BigDecimal("5")) > 0) {
                throw new IllegalArgumentException("Trọng lượng (kg) tối đa của máy tính là 5 kg (vui lòng nhập theo đơn vị kg, VD: 1.7)");
            }
        }
    }

    @Transactional
    public BienTheSanPham update(Integer id, BienTheSanPhamRequest request) {
        if ("cho_nhap_hang".equalsIgnoreCase(request.getTrangThai())) {
            request.setTrangThai("active");
        }
        BienTheSanPham entity = getById(id);

        Integer sanPhamId = entity.getSanPham() != null ? entity.getSanPham().getSanPhamId() : request.getSanPhamId();
        String oldMaSku = entity.getMaSku();
        String oldBarcode = entity.getBarcode();
        BigDecimal oldGiaNhap = entity.getGiaNhap();
        BigDecimal oldGiaBan = entity.getGiaBan();
        Integer oldBaoHanhThang = entity.getBaoHanhThang();
        String oldHinhAnhBienThe = entity.getHinhAnhBienThe();
        String oldTrangThai = entity.getTrangThai();
        String oldMauSac = entity.getMauSac();
        Integer oldCpuId = entity.getCpu() != null ? entity.getCpu().getCpuId() : null;
        Integer oldRamId = entity.getRam() != null ? entity.getRam().getRamId() : null;
        Integer oldOCungId = entity.getOCung() != null ? entity.getOCung().getOCungId() : null;
        Integer oldGpuId = entity.getGpu() != null ? entity.getGpu().getGpuId() : null;
        String oldKichThuocManHinh = entity.getKichThuocManHinh();
        String oldHeDieuHanh = entity.getHeDieuHanh();
        String oldPin = entity.getPin();
        BigDecimal oldTrongLuongKg = entity.getTrongLuongKg();
        String oldMoTa = entity.getMoTa();

        String barcode = chuanHoa(request.getBarcode());
        if (barcode != null) {
            kiemTraTrungBarcode(barcode, id);
            entity.setBarcode(barcode);
        }
        if (request.getMaSku() != null && !request.getMaSku().isBlank()) {
            kiemTraTrungMaSku(request.getMaSku(), id);
            entity.setMaSku(request.getMaSku().trim());
        }

        if (request.getSanPhamId() != null) {
            entity.setSanPham(sanPhamRepository.getReferenceById(request.getSanPhamId()));
        }
        if (request.getCpuId() != null) {
            entity.setCpu(dmCpuRepository.getReferenceById(request.getCpuId()));
        }
        if (request.getRamId() != null) {
            entity.setRam(dmRamRepository.getReferenceById(request.getRamId()));
        }
        if (request.getOCungId() != null) {
            entity.setOCung(dmOcungRepository.getReferenceById(request.getOCungId()));
        }
        if (request.getGpuId() != null) {
            entity.setGpu(dmGpuRepository.getReferenceById(request.getGpuId()));
        }

        if (request.getGiaNhap() != null) entity.setGiaNhap(request.getGiaNhap());
        if (request.getGiaBan() != null) entity.setGiaBan(request.getGiaBan());
        if (request.getBaoHanhThang() != null) entity.setBaoHanhThang(request.getBaoHanhThang());
        if (request.getMauSac() != null && !request.getMauSac().isBlank()) entity.setMauSac(request.getMauSac());
        if (request.getKichThuocManHinh() != null && !request.getKichThuocManHinh().isBlank()) entity.setKichThuocManHinh(request.getKichThuocManHinh());
        if (request.getHeDieuHanh() != null && !request.getHeDieuHanh().isBlank()) entity.setHeDieuHanh(request.getHeDieuHanh());
        if (request.getPin() != null && !request.getPin().isBlank()) entity.setPin(request.getPin());
        if (request.getTrongLuongKg() != null) {
            if (request.getTrongLuongKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Trọng lượng (kg) phải lớn hơn 0");
            }
            if (request.getTrongLuongKg().compareTo(new BigDecimal("5")) > 0) {
                throw new IllegalArgumentException("Trọng lượng (kg) tối đa của máy tính là 5 kg (vui lòng nhập theo đơn vị kg, VD: 1.7)");
            }
            entity.setTrongLuongKg(request.getTrongLuongKg());
        }
        if (request.getHinhAnhBienThe() != null && !request.getHinhAnhBienThe().isBlank()) entity.setHinhAnhBienThe(request.getHinhAnhBienThe());
        if (request.getTrangThai() != null && !request.getTrangThai().isBlank()) entity.setTrangThai(request.getTrangThai());
        if (request.getMoTa() != null && !request.getMoTa().isBlank()) entity.setMoTa(request.getMoTa());

        BienTheSanPham saved = bienTheSanPhamRepository.save(entity);

        NhanVien nguoiSua = lichSuThayDoiSanPhamService.nguoiSuaHienTai();
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "maSku", oldMaSku, saved.getMaSku(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "barcode", oldBarcode, saved.getBarcode(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "giaNhap", oldGiaNhap, saved.getGiaNhap(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "giaBan", oldGiaBan, saved.getGiaBan(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "baoHanhThang", oldBaoHanhThang, saved.getBaoHanhThang(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "hinhAnhBienThe", oldHinhAnhBienThe, saved.getHinhAnhBienThe(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "trangThai", oldTrangThai, saved.getTrangThai(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "mauSac", oldMauSac, saved.getMauSac(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "cpuId", oldCpuId, saved.getCpu() != null ? saved.getCpu().getCpuId() : null, nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "ramId", oldRamId, saved.getRam() != null ? saved.getRam().getRamId() : null, nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "oCungId", oldOCungId, saved.getOCung() != null ? saved.getOCung().getOCungId() : null, nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "gpuId", oldGpuId, saved.getGpu() != null ? saved.getGpu().getGpuId() : null, nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "kichThuocManHinh", oldKichThuocManHinh, saved.getKichThuocManHinh(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "heDieuHanh", oldHeDieuHanh, saved.getHeDieuHanh(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "pin", oldPin, saved.getPin(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "trongLuongKg", oldTrongLuongKg, saved.getTrongLuongKg(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "moTa", oldMoTa, saved.getMoTa(), nguoiSua);

        return saved;
    }

    @Transactional
    public void updateGiaNhap(Integer bienTheId, BigDecimal giaNhap) {
        if (giaNhap == null || giaNhap.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá nhập không hợp lệ");
        }
        BienTheSanPham entity = bienTheSanPhamRepository.findById(bienTheId)
                .orElseThrow(() -> new IllegalArgumentException("Biến thể không tồn tại với id: " + bienTheId));
        BigDecimal oldGiaNhap = entity.getGiaNhap();
        entity.setGiaNhap(giaNhap);
        BienTheSanPham saved = bienTheSanPhamRepository.save(entity);
        Integer sanPhamId = entity.getSanPham() != null ? entity.getSanPham().getSanPhamId() : null;
        NhanVien nguoiSua = lichSuThayDoiSanPhamService.nguoiSuaHienTai();
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, bienTheId, "bien_the", "giaNhap", oldGiaNhap, saved.getGiaNhap(), nguoiSua);
    }

    @Transactional
    public void updateGiaBan(Integer bienTheId, BigDecimal giaBan) {
        if (giaBan == null || giaBan.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá bán không hợp lệ");
        }
        BienTheSanPham entity = bienTheSanPhamRepository.findById(bienTheId)
                .orElseThrow(() -> new IllegalArgumentException("Biến thể không tồn tại với id: " + bienTheId));
        BigDecimal oldGiaBan = entity.getGiaBan();
        entity.setGiaBan(giaBan);
        BienTheSanPham saved = bienTheSanPhamRepository.save(entity);
        Integer sanPhamId = entity.getSanPham() != null ? entity.getSanPham().getSanPhamId() : null;
        NhanVien nguoiSua = lichSuThayDoiSanPhamService.nguoiSuaHienTai();
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, bienTheId, "bien_the", "giaBan", oldGiaBan, saved.getGiaBan(), nguoiSua);
    }

    /** Chuỗi rỗng phải về null: hai biến thể cùng để barcode "" sẽ đụng unique index. */
    private String chuanHoa(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    /** Báo lỗi rõ ràng trước khi để SQL Server bắn unique violation khó đọc. */
    private void kiemTraTrungBarcode(String barcode, Integer boQuaId) {
        if (barcode == null) return;
        boolean trung = boQuaId == null
                ? bienTheSanPhamRepository.existsByBarcode(barcode)
                : bienTheSanPhamRepository.existsByBarcodeAndBienTheIdNot(barcode, boQuaId);
        if (trung) throw new IllegalArgumentException("Barcode '" + barcode + "' đã được dùng");
    }

    /** Kiểm tra trùng SKU rõ ràng */
    private void kiemTraTrungMaSku(String maSku, Integer boQuaId) {
        if (maSku == null || maSku.isBlank()) return;
        boolean trung = boQuaId == null
                ? bienTheSanPhamRepository.existsByMaSku(maSku.trim())
                : bienTheSanPhamRepository.existsByMaSkuAndBienTheIdNot(maSku.trim(), boQuaId);
        if (trung) throw new IllegalArgumentException("Mã SKU '" + maSku.trim() + "' đã được dùng");
    }

}