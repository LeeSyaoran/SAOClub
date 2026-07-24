package com.example.backend.service;

import com.example.backend.entity.ThuongHieu;
import com.example.backend.repository.ThuongHieuRepository;
import com.example.backend.request.ThuongHieuRequest;
import com.example.backend.response.ThuongHieuResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ThuongHieuService {

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    public List<ThuongHieuResponse> hienThiThuongHieu() {
        return thuongHieuRepository.hienThiThuongHieu();
    }

    public ThuongHieu getById(Integer id) {
        return thuongHieuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Thương hiệu không tồn tại với id: " + id));
    }

    // Trước đây controller nhận thẳng entity JPA làm request body — xem DanhMucService lý do.
    public ThuongHieu create(ThuongHieuRequest request) {
        ThuongHieu entity = new ThuongHieu();
        BeanUtils.copyProperties(request, entity);
        if (entity.getTrangThai() == null || entity.getTrangThai().isBlank())
            entity.setTrangThai("active");
        entity.setNgayTao(LocalDateTime.now());
        return thuongHieuRepository.save(entity);
    }

    public ThuongHieu update(Integer id, ThuongHieuRequest request) {
        ThuongHieu entity = getById(id);
        LocalDateTime ngayTaoGoc = entity.getNgayTao();
        BeanUtils.copyProperties(request, entity);
        entity.setThuongHieuId(id);
        entity.setNgayTao(ngayTaoGoc);
        return thuongHieuRepository.save(entity);
    }

    public void delete(Integer id) {
        if (!thuongHieuRepository.existsById(id))
            throw new IllegalArgumentException("Thương hiệu không tồn tại với id: " + id);
        thuongHieuRepository.deleteById(id);
    }
}
