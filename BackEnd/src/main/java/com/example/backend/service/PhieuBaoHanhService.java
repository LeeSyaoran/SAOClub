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

import java.util.List;

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
     * Tra cuu serial theo soSerial — tra duoc bat ky trang thai nao.
     * Lay day du thong tin bien the, san pham, don hang, khach hang + lich su phieu bao hanh cu.
     */
    public WarrantyLookupResponse traCuuSerial(String soSerial) {
        // Bước 1: Tìm serial chưa xóa theo barcode (bien_the) hoac so_serial (chi_tiet_san_pham)
        List<ChiTietSanPham> results = chiTietSanPhamRepository
                .findActiveByBarcodeOrSoSerial(soSerial, soSerial);

        if (!results.isEmpty()) {
            // Tim theo barcode -> lay san pham da_ban neu co, neu khong lay dau tien
            ChiTietSanPham serial = results.stream()
                    .filter(c -> "da_ban".equals(c.getTrangThai()))
                    .findFirst()
                    .orElse(results.get(0));
            return buildLookupResponse(serial);
        }

        // Bước 2: Không tìm thấy -> kiem tra co phai da bi xoa mem
        boolean existedDeleted = chiTietSanPhamRepository
                .existsDeletedByBarcodeOrSoSerial(soSerial, soSerial);

        if (existedDeleted) {
            throw new SerialDeletedException("Mã " + soSerial + " đã bị xóa khỏi hệ thống");
        }

        throw new jakarta.persistence.EntityNotFoundException("Mã " + soSerial + " không tồn tại trong hệ thống");
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
