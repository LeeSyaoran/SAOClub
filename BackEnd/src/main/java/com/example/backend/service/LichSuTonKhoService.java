package com.example.backend.service;

import com.example.backend.entity.LichSuTonKho;
import com.example.backend.repository.*;
import com.example.backend.request.LichSuTonKhoRequest;
import com.example.backend.response.LichSuTonKhoResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LichSuTonKhoService {

    @Autowired
    private LichSuTonKhoRepository lichSuTonKhoRepository;
    @Autowired
    private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;
    @Autowired
    private DonHangRepository donHangRepository;
    @Autowired
    private PhieuNhapKhoRepository phieuNhapKhoRepository;
    @Autowired
    private NhanVienRepository nhanVienRepository;

    public List<LichSuTonKhoResponse> hienThiLichSuTonKho() {
        return lichSuTonKhoRepository.hienThiLichSuTonKho();
    }

    public LichSuTonKho getById(Integer id) {
        return lichSuTonKhoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lịch sử tồn kho không tồn tại với id: " + id));
    }

    public LichSuTonKho create(LichSuTonKhoRequest request) {
        LichSuTonKho entity = new LichSuTonKho();
        BeanUtils.copyProperties(request, entity, "bienTheId", "chiTietId", "donHangId", "phieuNhapId", "nhanVienId", "ngayTao");
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getBienTheId()));
        if (request.getChiTietId() != null)
            entity.setChiTietSanPham(chiTietSanPhamRepository.getReferenceById(request.getChiTietId()));
        if (request.getDonHangId() != null)
            entity.setDonHang(donHangRepository.getReferenceById(request.getDonHangId()));
        if (request.getPhieuNhapId() != null)
            entity.setPhieuNhapKho(phieuNhapKhoRepository.getReferenceById(request.getPhieuNhapId()));
        if (request.getNhanVienId() != null)
            entity.setNhanVien(nhanVienRepository.getReferenceById(request.getNhanVienId()));
        entity.setNgayTao(request.getNgayTao() != null ? request.getNgayTao() : LocalDateTime.now());
        return lichSuTonKhoRepository.save(entity);
    }

    public void delete(Integer id) {
        if (!lichSuTonKhoRepository.existsById(id))
            throw new IllegalArgumentException("Lịch sử tồn kho không tồn tại với id: " + id);
        lichSuTonKhoRepository.deleteById(id);
    }
}
