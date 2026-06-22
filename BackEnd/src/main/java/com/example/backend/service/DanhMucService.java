package com.example.backend.service;

import com.example.backend.entity.DanhMuc;
import com.example.backend.repository.DanhMucRepository;
import com.example.backend.response.DanhMucResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public DanhMuc create(DanhMuc item) {
        return danhMucRepository.save(item);
    }

    // DanhMuc dùng field "id" (không phải danhMucId) — xem entity DanhMuc
    public DanhMuc update(Integer id, DanhMuc item) {
        if (!danhMucRepository.existsById(id))
            throw new IllegalArgumentException("Danh mục không tồn tại với id: " + id);
        item.setId(id);
        return danhMucRepository.save(item);
    }

    public void delete(Integer id) {
        if (!danhMucRepository.existsById(id))
            throw new IllegalArgumentException("Danh mục không tồn tại với id: " + id);
        danhMucRepository.deleteById(id);
    }
}
