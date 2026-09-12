package com.example.backend.service;

import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.entity.PhieuBaoHanh;
import com.example.backend.repository.*;
import com.example.backend.request.PhieuBaoHanhRequest;
import com.example.backend.response.PhieuBaoHanhResponse;
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

    /**
     * Từ chối gia hạn BH.
     */
    public void rejectExtension(Integer baoHanhId, String lyDoTuChoi, String rejectedAt) {
        // TODO: implement when extension logic is finalized
    }

}