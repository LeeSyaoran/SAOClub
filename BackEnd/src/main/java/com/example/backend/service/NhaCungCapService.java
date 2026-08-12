package com.example.backend.service;

import com.example.backend.entity.NhaCungCap;
import com.example.backend.repository.NhaCungCapRepository;
import com.example.backend.request.NhaCungCapRequest;
import com.example.backend.response.NhaCungCapResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    // Trước đây controller nhận thẳng entity JPA làm request body — xem DanhMucService lý do.
    public NhaCungCap create(NhaCungCapRequest request) {
        NhaCungCap entity = new NhaCungCap();
        BeanUtils.copyProperties(request, entity);
        if (entity.getTrangThai() == null || entity.getTrangThai().isBlank())
            entity.setTrangThai("active");
        entity.setNgayTao(LocalDateTime.now());
        return nhaCungCapRepository.save(entity);
    }

    public NhaCungCap update(Integer id, NhaCungCapRequest request) {
        NhaCungCap entity = getById(id);
        LocalDateTime ngayTaoGoc = entity.getNgayTao();
        BeanUtils.copyProperties(request, entity);
        entity.setNhaCungCapId(id);
        entity.setNgayTao(ngayTaoGoc);
        return nhaCungCapRepository.save(entity);
    }

}
