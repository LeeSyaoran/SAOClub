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
    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    public List<PhieuBaoHanhResponse> hienThiPhieuBaoHanh() {
        return phieuBaoHanhRepository.hienThiPhieuBaoHanh();
    }

    public PhieuBaoHanh getById(Integer id) {
        return phieuBaoHanhRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phiếu bảo hành không tồn tại với id: " + id));
    }

    // Trước đây không kiểm tra thứ tự các mốc ngày — có thể lưu ngày hết bảo hành sớm hơn
    // ngày mua, hoặc ngày trả khách sớm hơn ngày tiếp nhận sửa, do nhập tay sai hoặc bug UI.
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
        // BeanUtils copies: ngayMua, ngayHetBh, ngayTiepNhan, ngayTraKhach,
        //                   moTaLoi, ketQuaXuLy, trangThai, chiPhiPhatSinh, ghiChu
        BeanUtils.copyProperties(request, entity, "donHangId", "bienTheId", "khachHangId", "chiTietId");
        entity.setDonHang(donHangRepository.getReferenceById(request.getDonHangId()));
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getBienTheId()));
        entity.setKhachHang(khachHangRepository.getReferenceById(request.getKhachHangId()));
        entity.setChiTietSanPham(request.getChiTietId() != null
                ? chiTietSanPhamRepository.getReferenceById(request.getChiTietId()) : null);
        return phieuBaoHanhRepository.save(entity);
    }

    public PhieuBaoHanh update(Integer id, PhieuBaoHanhRequest request) {
        kiemTraKhoangNgayHopLe(request);
        PhieuBaoHanh entity = getById(id);
        BeanUtils.copyProperties(request, entity, "baoHanhId", "donHangId", "bienTheId", "khachHangId", "chiTietId");
        entity.setDonHang(donHangRepository.getReferenceById(request.getDonHangId()));
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getBienTheId()));
        entity.setKhachHang(khachHangRepository.getReferenceById(request.getKhachHangId()));
        entity.setChiTietSanPham(request.getChiTietId() != null
                ? chiTietSanPhamRepository.getReferenceById(request.getChiTietId()) : null);
        return phieuBaoHanhRepository.save(entity);
    }

    public void delete(Integer id) {
        if (!phieuBaoHanhRepository.existsById(id))
            throw new IllegalArgumentException("Phiếu bảo hành không tồn tại với id: " + id);
        phieuBaoHanhRepository.deleteById(id);
    }
}
