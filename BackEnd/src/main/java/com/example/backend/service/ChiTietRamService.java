package com.example.backend.service;

import com.example.backend.entity.ChiTietRam;
import com.example.backend.repository.ChiTietRamRepository;
import com.example.backend.repository.DmRamRepository;
import com.example.backend.request.ChiTietRamRequest;
import com.example.backend.response.ChiTietRamResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChiTietRamService {

    @Autowired
    private ChiTietRamRepository chiTietRamRepository;
    @Autowired
    private DmRamRepository dmRamRepository;

    public List<ChiTietRamResponse> hienThiChiTietRam() {
        return chiTietRamRepository.hienThiChiTietRam();
    }

    public ChiTietRam getById(Integer id) {
        return chiTietRamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Serial RAM không tồn tại với id: " + id));
    }

    public ChiTietRam create(ChiTietRamRequest request) {
        ChiTietRam entity = new ChiTietRam();
        BeanUtils.copyProperties(request, entity, "ramId");
        entity.setRam(dmRamRepository.getReferenceById(request.getRamId()));
        return chiTietRamRepository.save(entity);
    }

    public ChiTietRam update(Integer id, ChiTietRamRequest request) {
        ChiTietRam entity = getById(id);
        BeanUtils.copyProperties(request, entity, "chiTietRamId", "ramId");
        entity.setRam(dmRamRepository.getReferenceById(request.getRamId()));
        return chiTietRamRepository.save(entity);
    }

    public void delete(Integer id) {
        ChiTietRam entity = getById(id);
        if (!"trong_kho".equals(entity.getTrangThai())) {
            throw new IllegalArgumentException("Chỉ được xóa serial đang ở trạng thái \"Trong kho\" (chưa dùng)");
        }
        chiTietRamRepository.deleteById(id);
    }
}
