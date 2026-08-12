package com.example.backend.service;

import com.example.backend.entity.DiaChiGiaoHang;
import com.example.backend.repository.DiaChiGiaoHangRepository;
import com.example.backend.repository.KhachHangRepository;
import com.example.backend.request.DiaChiGiaoHangRequest;
import com.example.backend.response.DiaChiGiaoHangResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DiaChiGiaoHangService {

    @Autowired
    private DiaChiGiaoHangRepository diaChiGiaoHangRepository;
    @Autowired
    private KhachHangRepository khachHangRepository;

    public List<DiaChiGiaoHangResponse> hienThiDiaChiGiaoHang() {
        return diaChiGiaoHangRepository.hienThiDiaChiGiaoHang();
    }

    public DiaChiGiaoHang getById(Integer id) {
        return diaChiGiaoHangRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Địa chỉ giao hàng không tồn tại với id: " + id));
    }

    public DiaChiGiaoHang create(DiaChiGiaoHangRequest request) {
        DiaChiGiaoHang entity = new DiaChiGiaoHang();
        BeanUtils.copyProperties(request, entity, "khachHangId", "ngayTao");
        entity.setKhachHang(khachHangRepository.getReferenceById(request.getKhachHangId()));
        entity.setNgayTao(request.getNgayTao() != null ? request.getNgayTao() : LocalDateTime.now());
        return diaChiGiaoHangRepository.save(entity);
    }

    public DiaChiGiaoHang update(Integer id, DiaChiGiaoHangRequest request) {
        DiaChiGiaoHang entity = getById(id);
        BeanUtils.copyProperties(request, entity, "id", "khachHangId", "ngayTao");
        entity.setKhachHang(khachHangRepository.getReferenceById(request.getKhachHangId()));
        return diaChiGiaoHangRepository.save(entity);
    }

    public void delete(Integer id) {
        if (!diaChiGiaoHangRepository.existsById(id))
            throw new IllegalArgumentException("Địa chỉ giao hàng không tồn tại với id: " + id);
        diaChiGiaoHangRepository.deleteById(id);
    }
}
