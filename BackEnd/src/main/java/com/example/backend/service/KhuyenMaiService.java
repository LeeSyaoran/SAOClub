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

    /**
     * Validate a promo code and return details if valid.
     * Returns null if not found or invalid.
     * Throws descriptive messages for each failure case.
     */
    public KhuyenMai kiemTraMaKhuyenMai(String maKhuyenMai) {
        if (maKhuyenMai == null || maKhuyenMai.isBlank())
            throw new IllegalArgumentException("Vui lòng nhập mã khuyến mãi");
        KhuyenMai km = khuyenMaiRepository.findByMaKhuyenMaiIgnoreCase(maKhuyenMai.trim())
                .orElseThrow(() -> new IllegalArgumentException("Mã khuyến mãi không tồn tại"));
        if (!"active".equals(km.getTrangThai()))
            throw new IllegalArgumentException("Mã khuyến mãi không còn hiệu lực (đã ngừng hoạt động)");
        LocalDateTime now = LocalDateTime.now();
        if (km.getNgayBatDau() != null && now.isBefore(km.getNgayBatDau()))
            throw new IllegalArgumentException("Mã khuyến mãi chưa đến thời gian áp dụng");
        if (km.getNgayKetThuc() != null && now.isAfter(km.getNgayKetThuc()))
            throw new IllegalArgumentException("Mã khuyến mãi đã hết hạn");
        int daDung = km.getSoLanDaDung() != null ? km.getSoLanDaDung() : 0;
        if (km.getSoLuongToiDa() != null && daDung >= km.getSoLuongToiDa())
            throw new IllegalArgumentException("Mã khuyến mãi đã hết lượt sử dụng");
        return km;
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
