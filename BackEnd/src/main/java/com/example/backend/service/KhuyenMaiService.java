package com.example.backend.service;

import com.example.backend.entity.KhuyenMai;
import com.example.backend.repository.KhuyenMaiRepository;
import com.example.backend.request.KhuyenMaiRequest;
import com.example.backend.response.KhuyenMaiResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KhuyenMaiService {

    @Autowired
    private KhuyenMaiRepository khuyenMaiRepository;

    public List<KhuyenMaiResponse> hienThiKhuyenMai() {
        return khuyenMaiRepository.hienThiKhuyenMai();
    }

    public KhuyenMai getById(Integer id) {
        return khuyenMaiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khuyến mãi không tồn tại với id: " + id));
    }

    public KhuyenMai create(KhuyenMaiRequest request) {
        KhuyenMai entity = new KhuyenMai();
        BeanUtils.copyProperties(request, entity);
        entity.setNgayTao(LocalDateTime.now());
        entity.setSoLanDaDung(0); 
        return khuyenMaiRepository.save(entity);
    }

    public KhuyenMai update(Integer id, KhuyenMaiRequest request) {
        KhuyenMai entity = getById(id);
        BeanUtils.copyProperties(request, entity, "khuyenMaiId", "ngayTao", "soLanDaDung");
        return khuyenMaiRepository.save(entity);
    }

}
