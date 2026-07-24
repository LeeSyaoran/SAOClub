package com.example.backend.service;

import com.example.backend.entity.DanhMuc;
import com.example.backend.repository.DanhMucRepository;
import com.example.backend.request.DanhMucRequest;
import com.example.backend.response.DanhMucResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DanhMucService {

    @Autowired
    private DanhMucRepository danhMucRepository;

    public List<DanhMucResponse> hienThiDanhMuc() {
        return danhMucRepository.hienThiDanhMuc();
    }

    public DanhMuc getById(Integer id) {
        return danhMucRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại với id: " + id));
    }

    // Trước đây controller nhận thẳng entity JPA làm request body — không @Valid, không
    // validate tenDanhMuc bắt buộc, client tự gửi ngayTao (hoặc bỏ trống -> NULL đè lên cột
    // NOT NULL DEFAULT GETDATE(), lỗi DB). Dùng DTO riêng, ngayTao luôn do server tự đặt.
    public DanhMuc create(DanhMucRequest request) {
        DanhMuc entity = new DanhMuc();
        BeanUtils.copyProperties(request, entity);
        if (entity.getTrangThai() == null || entity.getTrangThai().isBlank())
            entity.setTrangThai("active");
        entity.setNgayTao(LocalDateTime.now());
        return danhMucRepository.save(entity);
    }

    // DanhMuc dùng field "id" (không phải danhMucId) — xem entity DanhMuc
    public DanhMuc update(Integer id, DanhMucRequest request) {
        DanhMuc entity = getById(id);
        LocalDateTime ngayTaoGoc = entity.getNgayTao();
        BeanUtils.copyProperties(request, entity);
        entity.setId(id);
        entity.setNgayTao(ngayTaoGoc);
        return danhMucRepository.save(entity);
    }

    public void delete(Integer id) {
        if (!danhMucRepository.existsById(id))
            throw new IllegalArgumentException("Danh mục không tồn tại với id: " + id);
        danhMucRepository.deleteById(id);
    }
}
