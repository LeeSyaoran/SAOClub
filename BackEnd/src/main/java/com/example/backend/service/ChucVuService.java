package com.example.backend.service;

import com.example.backend.entity.ChucVu;
import com.example.backend.repository.ChucVuRepository;
import com.example.backend.response.ChucVuResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChucVuService {

    @Autowired
    private ChucVuRepository chucVuRepository;

    public List<ChucVuResponse> hienThiChucVu() {
        return chucVuRepository.hienThiChucVu();
    }

    public ChucVu getById(Integer id) {
        return chucVuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chức vụ không tồn tại với id: " + id));
    }

    public ChucVu create(ChucVu item) {
        return chucVuRepository.save(item);
    }

    // ChucVu dùng field "id" (không phải chucVuId) — xem entity ChucVu
    public ChucVu update(Integer id, ChucVu item) {
        if (!chucVuRepository.existsById(id))
            throw new IllegalArgumentException("Chức vụ không tồn tại với id: " + id);
        item.setId(id);
        return chucVuRepository.save(item);
    }

    public void delete(Integer id) {
        if (!chucVuRepository.existsById(id))
            throw new IllegalArgumentException("Chức vụ không tồn tại với id: " + id);
        chucVuRepository.deleteById(id);
    }
}
