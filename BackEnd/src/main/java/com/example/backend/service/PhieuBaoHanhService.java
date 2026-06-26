package com.example.backend.service;

import com.example.backend.entity.PhieuBaoHanh;
import com.example.backend.repository.*;
import com.example.backend.request.PhieuBaoHanhRequest;
import com.example.backend.response.PhieuBaoHanhResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

    public List<PhieuBaoHanhResponse> hienThiPhieuBaoHanh() {
        return phieuBaoHanhRepository.hienThiPhieuBaoHanh();
    }

    public PhieuBaoHanh getById(Integer id) {
        return phieuBaoHanhRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phiếu bảo hành không tồn tại với id: " + id));
    }

    public PhieuBaoHanh create(PhieuBaoHanhRequest request) {
        PhieuBaoHanh entity = new PhieuBaoHanh();
        // BeanUtils copies: ngayMua, ngayHetBh, ngayTiepNhan, ngayTraKhach,
        //                   moTaLoi, ketQuaXuLy, trangThai, chiPhiPhatSinh, ghiChu
        // Bỏ qua: donHangId, sanPhamId (dùng làm bienTheId), khachHangId, serialNumber (không có trong entity)
        BeanUtils.copyProperties(request, entity, "donHangId", "sanPhamId", "khachHangId");
        entity.setDonHang(donHangRepository.getReferenceById(request.getDonHangId()));
        // Request dùng sanPhamId nhưng entity cần bienThe (bienTheId)
        // — sanPhamId trong request thực tế là bienTheId
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getSanPhamId()));
        entity.setKhachHang(khachHangRepository.getReferenceById(request.getKhachHangId()));
        return phieuBaoHanhRepository.save(entity);
    }

    public PhieuBaoHanh update(Integer id, PhieuBaoHanhRequest request) {
        PhieuBaoHanh entity = getById(id);
        BeanUtils.copyProperties(request, entity, "baoHanhId", "donHangId", "sanPhamId", "khachHangId");
        entity.setDonHang(donHangRepository.getReferenceById(request.getDonHangId()));
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getSanPhamId()));
        entity.setKhachHang(khachHangRepository.getReferenceById(request.getKhachHangId()));
        return phieuBaoHanhRepository.save(entity);
    }

    public void delete(Integer id) {
        if (!phieuBaoHanhRepository.existsById(id))
            throw new IllegalArgumentException("Phiếu bảo hành không tồn tại với id: " + id);
        phieuBaoHanhRepository.deleteById(id);
    }
}
