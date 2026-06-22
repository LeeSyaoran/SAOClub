package com.example.backend.service;

import com.example.backend.entity.NhaCungCap;
import com.example.backend.repository.NhaCungCapRepository;
import com.example.backend.response.NhaCungCapResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NhaCungCapService {

    @Autowired
    private NhaCungCapRepository nhaCungCapRepository;

    public List<NhaCungCapResponse> hienThiNhaCungCap() {
        return nhaCungCapRepository.hienThiNhaCungCap();
    }

    public NhaCungCap getById(Integer id) {
        return nhaCungCapRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nhà cung cấp không tồn tại với id: " + id));
    }

    public NhaCungCap create(NhaCungCap item) {
        return nhaCungCapRepository.save(item);
    }

    public NhaCungCap update(Integer id, NhaCungCap item) {
        if (!nhaCungCapRepository.existsById(id))
            throw new IllegalArgumentException("Nhà cung cấp không tồn tại với id: " + id);
        item.setNhaCungCapId(id);
        return nhaCungCapRepository.save(item);
    }

    public void delete(Integer id) {
        if (!nhaCungCapRepository.existsById(id))
            throw new IllegalArgumentException("Nhà cung cấp không tồn tại với id: " + id);
        nhaCungCapRepository.deleteById(id);
    }
}
