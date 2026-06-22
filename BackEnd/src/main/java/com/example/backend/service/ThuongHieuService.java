package com.example.backend.service;

import com.example.backend.entity.ThuongHieu;
import com.example.backend.repository.ThuongHieuRepository;
import com.example.backend.response.ThuongHieuResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public ThuongHieu create(ThuongHieu item) {
        return thuongHieuRepository.save(item);
    }

    public ThuongHieu update(Integer id, ThuongHieu item) {
        if (!thuongHieuRepository.existsById(id))
            throw new IllegalArgumentException("Thương hiệu không tồn tại với id: " + id);
        item.setThuongHieuId(id);
        return thuongHieuRepository.save(item);
    }

    public void delete(Integer id) {
        if (!thuongHieuRepository.existsById(id))
            throw new IllegalArgumentException("Thương hiệu không tồn tại với id: " + id);
        thuongHieuRepository.deleteById(id);
    }
}
