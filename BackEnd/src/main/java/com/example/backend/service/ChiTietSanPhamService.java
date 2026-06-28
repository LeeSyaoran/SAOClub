package com.example.backend.service;

import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.repository.BienTheSanPhamRepository;
import com.example.backend.repository.ChiTietSanPhamRepository;
import com.example.backend.request.ChiTietSanPhamRequest;
import com.example.backend.response.ChiTietSanPhamResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChiTietSanPhamService {

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;
    @Autowired
    private BienTheSanPhamRepository bienTheSanPhamRepository;

    public List<ChiTietSanPhamResponse> hienThiChiTietSanPham() {
        return chiTietSanPhamRepository.hienThiChiTietSanPham();
    }

    public List<ChiTietSanPhamResponse> getByBienTheId(Integer bienTheId) {
        return chiTietSanPhamRepository.findByBienTheId(bienTheId);
    }

    public ChiTietSanPham getById(Integer id) {
        return chiTietSanPhamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chi tiết sản phẩm không tồn tại với id: " + id));
    }

    public ChiTietSanPham create(ChiTietSanPhamRequest request) {
        ChiTietSanPham entity = new ChiTietSanPham();
        // BeanUtils copies: soSerial, soImei, trangThai, ngayNhapKho
        BeanUtils.copyProperties(request, entity, "bienTheId");
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getBienTheId()));
        return chiTietSanPhamRepository.save(entity);
    }

    public ChiTietSanPham update(Integer id, ChiTietSanPhamRequest request) {
        ChiTietSanPham entity = getById(id);
        BeanUtils.copyProperties(request, entity, "chiTietId", "bienTheId");
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getBienTheId()));
        return chiTietSanPhamRepository.save(entity);
    }

    public void delete(Integer id) {
        if (!chiTietSanPhamRepository.existsById(id))
            throw new IllegalArgumentException("Chi tiết sản phẩm không tồn tại với id: " + id);
        chiTietSanPhamRepository.deleteById(id);
    }
}
