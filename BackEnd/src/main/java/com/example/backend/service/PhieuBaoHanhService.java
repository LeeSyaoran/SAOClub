package com.example.backend.service;

import com.example.backend.entity.BienTheSanPham;
import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.entity.ChiTietDonHang;
import com.example.backend.entity.DonHang;
import com.example.backend.entity.PhieuBaoHanh;
import com.example.backend.entity.SanPham;
import com.example.backend.exception.SerialDeletedException;
import com.example.backend.repository.*;
import com.example.backend.request.PhieuBaoHanhRequest;
import com.example.backend.response.PhieuBaoHanhResponse;
import com.example.backend.response.WarrantyLookupResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class PhieuBaoHanhService {

    @Autowired
    private PhieuBaoHanhRepository phieuBaoHanhRepository;
    @Autowired
    private DonHangRepository donHangRepository;
    @Autowired
    private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Autowired
    private KhachHangRepository khachHangRepository;
    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    public List<PhieuBaoHanhResponse> hienThiPhieuBaoHanh() {
        return phieuBaoHanhRepository.hienThiPhieuBaoHanh();
    }

    public Page<PhieuBaoHanhResponse> hienThiPhieuBaoHanh(Pageable pageable) {
        return phieuBaoHanhRepository.hienThiPhieuBaoHanh(pageable);
    }

    public PhieuBaoHanh getById(Integer id) {
        return phieuBaoHanhRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phiếu bảo hành không tồn tại với id: " + id));
    }

    private void kiemTraKhoangNgayHopLe(PhieuBaoHanhRequest request) {
        if (!request.getNgayHetBh().isAfter(request.getNgayMua()))
            throw new IllegalArgumentException("Ngày hết bảo hành phải sau ngày mua");
        if (request.getNgayTiepNhan() != null && request.getNgayTiepNhan().isBefore(request.getNgayMua()))
            throw new IllegalArgumentException("Ngày tiếp nhận không thể trước ngày mua");
        if (request.getNgayTraKhach() != null && request.getNgayTiepNhan() != null
                && request.getNgayTraKhach().isBefore(request.getNgayTiepNhan()))
            throw new IllegalArgumentException("Ngày trả khách không thể trước ngày tiếp nhận");
    }

    public PhieuBaoHanh create(PhieuBaoHanhRequest request) {
        kiemTraKhoangNgayHopLe(request);
        PhieuBaoHanh entity = new PhieuBaoHanh();
        BeanUtils.copyProperties(request, entity, "donHangId", "bienTheId", "khachHangId", "chiTietId");
        entity.setDonHang(donHangRepository.getReferenceById(request.getDonHangId()));
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getBienTheId()));
        entity.setKhachHang(khachHangRepository.getReferenceById(request.getKhachHangId()));
        entity.setChiTietSanPham(request.getChiTietId() != null
                ? chiTietSanPhamRepository.getReferenceById(request.getChiTietId()) : null);
        PhieuBaoHanh saved = phieuBaoHanhRepository.save(entity);
        capNhatSerialTheoTrangThai(null, saved);
        return saved;
    }

    public PhieuBaoHanh update(Integer id, PhieuBaoHanhRequest request) {
        kiemTraKhoangNgayHopLe(request);
        PhieuBaoHanh entity = getById(id);
        String trangThaiCu = entity.getTrangThai();
        BeanUtils.copyProperties(request, entity, "baoHanhId", "donHangId", "bienTheId", "khachHangId", "chiTietId");
        entity.setDonHang(donHangRepository.getReferenceById(request.getDonHangId()));
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getBienTheId()));
        entity.setKhachHang(khachHangRepository.getReferenceById(request.getKhachHangId()));
        entity.setChiTietSanPham(request.getChiTietId() != null
                ? chiTietSanPhamRepository.getReferenceById(request.getChiTietId()) : null);
        PhieuBaoHanh saved = phieuBaoHanhRepository.save(entity);
        capNhatSerialTheoTrangThai(trangThaiCu, saved);
        return saved;
    }

    private static final java.util.Set<String> TRANG_THAI_DA_DONG =
            java.util.Set.of("da_xu_ly", "het_bao_hanh", "tu_choi");

    private void capNhatSerialTheoTrangThai(String trangThaiCu, PhieuBaoHanh phieu) {
        ChiTietSanPham serial = phieu.getChiTietSanPham();
        if (serial == null) return;

        boolean vuaVaoXuLy = "dang_xu_ly".equals(phieu.getTrangThai()) && !"dang_xu_ly".equals(trangThaiCu);
        boolean vuaDong = TRANG_THAI_DA_DONG.contains(phieu.getTrangThai())
                && !TRANG_THAI_DA_DONG.contains(trangThaiCu);

        if (vuaVaoXuLy) {
            serial.setTrangThai("loi_bao_hanh");
            chiTietSanPhamRepository.save(serial);
        } else if (vuaDong) {
            serial.setTrangThai("da_ban");
            chiTietSanPhamRepository.save(serial);
        }
    }

    /**
     * Lấy tất cả phiếu BH của 1 khách hàng.
     */
    public List<PhieuBaoHanhResponse> getByKhachHang(Integer khachHangId) {
        return phieuBaoHanhRepository.findByKhachHangId(khachHangId);
    }


    /**
     * Khách hàng tự hủy phiếu BH — chỉ cho phép khi trạng thái là 'cho_xu_ly'.
     */
    public void huyPhieu(Integer baoHanhId, String lyDo) {
        PhieuBaoHanh entity = getById(baoHanhId);
        if (!"cho_xu_ly".equals(entity.getTrangThai())) {
            throw new IllegalStateException("Chỉ có thể hủy phiếu đang ở trạng thái chờ xử lý");
        }
        entity.setTrangThai("da_huy");
        if (lyDo != null) entity.setGhiChu(lyDo);
        phieuBaoHanhRepository.save(entity);
    }


    /**
     * Lấy các yêu cầu gia hạn BH (phiếu có loaiYeuCau = 'gia_han').
     */
    public List<PhieuBaoHanhResponse> getExtensionRequests() {
        // TODO: implement when extension-request entity is added
        return List.of();
    }

    /**
     * Duyệt gia hạn BH — cập nhật ngày hết BH của serial.
     */
    public void approveExtension(Integer baoHanhId, String approvedAt) {
        // TODO: implement when extension logic is finalized
    }

    // Từ chối gia hạn bảo hành
    public void rejectExtension(Integer baoHanhId, String lyDoTuChoi, String rejectedAt) {
        // TODO: implement when extension logic is finalized
    }

    // Tra cứu thông tin bảo hành theo số serial hoặc mã vạch
    public WarrantyLookupResponse traCuuSerial(String soSerial) {
        String cleanCode = soSerial != null ? soSerial.trim() : "";
        if (cleanCode.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập mã serial hoặc mã vạch");
        }

        // Bước 1: Tìm serial chưa xóa theo barcode (bien_the) hoặc so_serial (chi_tiet_san_pham)
        List<ChiTietSanPham> results = chiTietSanPhamRepository
                .findActiveByBarcodeOrSoSerial(cleanCode, cleanCode);

        if (!results.isEmpty()) {
            // Tìm theo barcode/serial -> ưu tiên lấy sản phẩm da_ban nếu có, nếu không lấy đầu tiên
            ChiTietSanPham serial = results.stream()
                    .filter(c -> "da_ban".equals(c.getTrangThai()))
                    .findFirst()
                    .orElse(results.get(0));
            return buildLookupResponse(serial);
        }

        // Bước 2: Kiểm tra xem mã vạch hoặc SKU có tồn tại trong danh mục Biến Thể Sản Phẩm không
        Optional<BienTheSanPham> btOpt = bienTheSanPhamRepository.findByBarcodeWithDetails(cleanCode);
        if (btOpt.isEmpty()) {
            btOpt = bienTheSanPhamRepository.findByMaSkuWithDetails(cleanCode);
        }
        if (btOpt.isPresent()) {
            BienTheSanPham bt = btOpt.get();
            // Nếu biến thể có serial trong kho, kiểm tra xem có serial nào liên kết không
            if (bt.getBarcode() != null && !bt.getBarcode().isBlank()) {
                List<ChiTietSanPham> btSerials = chiTietSanPhamRepository.findActiveByBarcodeOrSoSerial(bt.getBarcode(), "");
                if (!btSerials.isEmpty()) {
                    ChiTietSanPham serial = btSerials.stream()
                            .filter(c -> "da_ban".equals(c.getTrangThai()))
                            .findFirst()
                            .orElse(btSerials.get(0));
                    return buildLookupResponse(serial);
                }
            }
            return buildLookupResponseFromBienThe(bt);
        }

        // Bước 3: Không tìm thấy -> kiểm tra xem có phải đã bị xóa mềm
        boolean existedDeleted = chiTietSanPhamRepository
                .existsDeletedByBarcodeOrSoSerial(cleanCode, cleanCode);

        if (existedDeleted) {
            throw new SerialDeletedException("Mã " + cleanCode + " đã bị xóa khỏi hệ thống");
        }

        throw new jakarta.persistence.EntityNotFoundException("Mã " + cleanCode + " không tồn tại trong hệ thống");
    }

    private WarrantyLookupResponse buildLookupResponseFromBienThe(BienTheSanPham bt) {
        SanPham sp = bt.getSanPham();

        WarrantyLookupResponse r = new WarrantyLookupResponse();
        r.setChiTietId(null);
        r.setSoSerial(null);
        r.setTrangThaiSerial("trong_kho");
        r.setNgayNhapKho(null);

        // BienThe
        r.setBienTheId(bt.getBienTheId());
        r.setMaSku(bt.getMaSku());
        r.setBarcode(bt.getBarcode());
        r.setGiaBan(bt.getGiaBan());
        r.setBaoHanhThang(bt.getBaoHanhThang());
        r.setHinhAnhBienThe(bt.getHinhAnhBienThe() != null ? bt.getHinhAnhBienThe() : (sp != null ? sp.getHinhAnhChinh() : null));
        r.setMauSac(bt.getMauSac());
        r.setKichThuocManHinh(bt.getKichThuocManHinh());
        r.setHeDieuHanh(bt.getHeDieuHanh());
        r.setPin(bt.getPin());
        r.setTrongLuongKg(bt.getTrongLuongKg());

        // CPU/RAM/GPU/OCung
        r.setCpuTen(bt.getCpu() != null ? bt.getCpu().getTenCpu() : null);
        r.setRamTen(bt.getRam() != null ? bt.getRam().getDungLuong() : null);
        r.setGpuTen(bt.getGpu() != null ? bt.getGpu().getTenGpu() : null);
        r.setOCungTen(bt.getOCung() != null ? bt.getOCung().getLoaiOcung() : null);

        // SanPham
        if (sp != null) {
            r.setSanPhamId(sp.getSanPhamId());
            r.setTenSanPham(sp.getTenSanPham());
            r.setMaSanPham(sp.getMaSanPham());
        }

        r.setLichSuPhieuBaoHanh(Collections.emptyList());
        return r;
    }

    private WarrantyLookupResponse buildLookupResponse(ChiTietSanPham serial) {

        BienTheSanPham bt = serial.getBienThe();
        SanPham sp = bt.getSanPham();

        WarrantyLookupResponse r = new WarrantyLookupResponse();
        // Serial
        r.setChiTietId(serial.getChiTietId());
        r.setSoSerial(serial.getSoSerial());
        r.setTrangThaiSerial(serial.getTrangThai());
        r.setNgayNhapKho(serial.getNgayNhapKho());

        // BienThe
        r.setBienTheId(bt.getBienTheId());
        r.setMaSku(bt.getMaSku());
        r.setBarcode(bt.getBarcode());
        r.setGiaBan(bt.getGiaBan());
        r.setBaoHanhThang(bt.getBaoHanhThang());
        r.setHinhAnhBienThe(bt.getHinhAnhBienThe());
        r.setMauSac(bt.getMauSac());
        r.setKichThuocManHinh(bt.getKichThuocManHinh());
        r.setHeDieuHanh(bt.getHeDieuHanh());
        r.setPin(bt.getPin());
        r.setTrongLuongKg(bt.getTrongLuongKg());

        // CPU/RAM/GPU/OCung
        r.setCpuTen(bt.getCpu() != null ? bt.getCpu().getTenCpu() : null);
        r.setRamTen(bt.getRam() != null ? bt.getRam().getDungLuong() : null);
        r.setGpuTen(bt.getGpu() != null ? bt.getGpu().getTenGpu() : null);
        r.setOCungTen(bt.getOCung() != null ? bt.getOCung().getLoaiOcung() : null);

        // SanPham
        r.setSanPhamId(sp.getSanPhamId());
        r.setTenSanPham(sp.getTenSanPham());
        r.setMaSanPham(sp.getMaSanPham());

        // DonHang + KhachHang (neu co)
        chiTietSanPhamRepository.findLatestOrderBySerialChiTietId(serial.getChiTietId())
                .ifPresent(ctdh -> {
                    DonHang donHang = ctdh.getDonHang();
                    r.setDonHangId(donHang.getId());
                    r.setMaDonHang(donHang.getMaDonHang());
                    r.setNgayGiaoThucTe(donHang.getNgayGiaoThucTe());
                    if (donHang.getKhachHang() != null) {
                        r.setKhachHangId(donHang.getKhachHang().getKhachHangId());
                        r.setTenKhachHang(donHang.getKhachHang().getHoTen());
                        r.setSoDienThoai(donHang.getKhachHang().getSoDienThoai());
                    }
                    // Tinh ngay het bao hanh
                    if (donHang.getNgayGiaoThucTe() != null && bt.getBaoHanhThang() != null) {
                        r.setNgayHetBaoHanh(donHang.getNgayGiaoThucTe().plusMonths(bt.getBaoHanhThang()));
                    }
                });

        // Lich su phieu bao hanh cu
        r.setLichSuPhieuBaoHanh(
                phieuBaoHanhRepository.findByChiTietId(serial.getChiTietId()));

        return r;
    }

}
