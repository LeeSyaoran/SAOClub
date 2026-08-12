package com.example.backend.service;

import com.example.backend.entity.ChiTietPhieuNhap;
import com.example.backend.repository.BienTheSanPhamRepository;
import com.example.backend.repository.ChiTietPhieuNhapRepository;
import com.example.backend.repository.PhieuNhapKhoRepository;
import com.example.backend.request.ChiTietPhieuNhapRequest;
import com.example.backend.response.ChiTietPhieuNhapResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChiTietPhieuNhapService {

    @Autowired
    private ChiTietPhieuNhapRepository chiTietPhieuNhapRepository;
    @Autowired
    private PhieuNhapKhoRepository phieuNhapKhoRepository;
    @Autowired
    private BienTheSanPhamRepository bienTheSanPhamRepository;

    public List<ChiTietPhieuNhapResponse> hienThiChiTietPhieuNhap() {
        return chiTietPhieuNhapRepository.hienThiChiTietPhieuNhap();
    }

    public ChiTietPhieuNhap getById(Integer id) {
        return chiTietPhieuNhapRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chi tiết phiếu nhập không tồn tại với id: " + id));
    }

    public ChiTietPhieuNhap create(ChiTietPhieuNhapRequest request) {
        ChiTietPhieuNhap entity = new ChiTietPhieuNhap();
        BeanUtils.copyProperties(request, entity, "phieuNhapId", "bienTheId");
        entity.setPhieuNhapKho(phieuNhapKhoRepository.getReferenceById(request.getPhieuNhapId()));
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getBienTheId()));
        return chiTietPhieuNhapRepository.save(entity);
    }

    public ChiTietPhieuNhap update(Integer id, ChiTietPhieuNhapRequest request) {
        ChiTietPhieuNhap entity = getById(id);
        BeanUtils.copyProperties(request, entity, "id", "phieuNhapId", "bienTheId");
        entity.setPhieuNhapKho(phieuNhapKhoRepository.getReferenceById(request.getPhieuNhapId()));
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getBienTheId()));
        return chiTietPhieuNhapRepository.save(entity);
    }

    public void delete(Integer id) {
        if (!chiTietPhieuNhapRepository.existsById(id))
            throw new IllegalArgumentException("Chi tiết phiếu nhập không tồn tại với id: " + id);
        chiTietPhieuNhapRepository.deleteById(id);
    }
}
