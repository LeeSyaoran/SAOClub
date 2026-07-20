package com.example.backend.service;

import com.example.backend.entity.ChiTietGpu;
import com.example.backend.repository.ChiTietGpuRepository;
import com.example.backend.repository.DmGpuRepository;
import com.example.backend.request.ChiTietGpuRequest;
import com.example.backend.response.ChiTietGpuResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChiTietGpuService {

    @Autowired
    private ChiTietGpuRepository chiTietGpuRepository;
    @Autowired
    private DmGpuRepository dmGpuRepository;

    public List<ChiTietGpuResponse> hienThiChiTietGpu() {
        return chiTietGpuRepository.hienThiChiTietGpu();
    }

    public ChiTietGpu getById(Integer id) {
        return chiTietGpuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Serial GPU không tồn tại với id: " + id));
    }

    public ChiTietGpu create(ChiTietGpuRequest request) {
        ChiTietGpu entity = new ChiTietGpu();
        BeanUtils.copyProperties(request, entity, "gpuId");
        entity.setGpu(dmGpuRepository.getReferenceById(request.getGpuId()));
        return chiTietGpuRepository.save(entity);
    }

    public ChiTietGpu update(Integer id, ChiTietGpuRequest request) {
        ChiTietGpu entity = getById(id);
        BeanUtils.copyProperties(request, entity, "chiTietGpuId", "gpuId");
        entity.setGpu(dmGpuRepository.getReferenceById(request.getGpuId()));
        return chiTietGpuRepository.save(entity);
    }

    public void delete(Integer id) {
        ChiTietGpu entity = getById(id);
        if (!"trong_kho".equals(entity.getTrangThai())) {
            throw new IllegalArgumentException("Chỉ được xóa serial đang ở trạng thái \"Trong kho\" (chưa dùng)");
        }
        chiTietGpuRepository.deleteById(id);
    }
}
