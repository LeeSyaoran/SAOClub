package com.example.backend.service;

import com.example.backend.entity.ChiTietTraHang;
import com.example.backend.repository.BienTheSanPhamRepository;
import com.example.backend.repository.ChiTietSanPhamRepository;
import com.example.backend.repository.ChiTietTraHangRepository;
import com.example.backend.repository.PhieuTraHangRepository;
import com.example.backend.request.ChiTietTraHangRequest;
import com.example.backend.response.ChiTietTraHangResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChiTietTraHangService {

    @Autowired
    private ChiTietTraHangRepository chiTietTraHangRepository;
    @Autowired
    private PhieuTraHangRepository phieuTraHangRepository;
    @Autowired
    private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    public List<ChiTietTraHangResponse> hienThiChiTietTraHang() {
        return chiTietTraHangRepository.hienThiChiTietTraHang();
    }

    public ChiTietTraHang getById(Integer id) {
        return chiTietTraHangRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chi tiết trả hàng không tồn tại với id: " + id));
    }

    public ChiTietTraHang create(ChiTietTraHangRequest request) {
        ChiTietTraHang entity = new ChiTietTraHang();
        // BeanUtils copies: soLuong, donGiaHoan, tinhTrang
        BeanUtils.copyProperties(request, entity, "phieuTraId", "bienTheId", "chiTietId");
        entity.setPhieuTraHang(phieuTraHangRepository.getReferenceById(request.getPhieuTraId()));
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getBienTheId()));
        // chiTietId (serial cụ thể) — optional
        if (request.getChiTietId() != null)
            entity.setChiTietSanPham(chiTietSanPhamRepository.getReferenceById(request.getChiTietId()));
        return chiTietTraHangRepository.save(entity);
    }

    public ChiTietTraHang update(Integer id, ChiTietTraHangRequest request) {
        ChiTietTraHang entity = getById(id);
        BeanUtils.copyProperties(request, entity, "id", "phieuTraId", "bienTheId", "chiTietId");
        entity.setPhieuTraHang(phieuTraHangRepository.getReferenceById(request.getPhieuTraId()));
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getBienTheId()));
        entity.setChiTietSanPham(request.getChiTietId() != null
                ? chiTietSanPhamRepository.getReferenceById(request.getChiTietId()) : null);
        return chiTietTraHangRepository.save(entity);
    }

    public void delete(Integer id) {
        if (!chiTietTraHangRepository.existsById(id))
            throw new IllegalArgumentException("Chi tiết trả hàng không tồn tại với id: " + id);
        chiTietTraHangRepository.deleteById(id);
    }
}
