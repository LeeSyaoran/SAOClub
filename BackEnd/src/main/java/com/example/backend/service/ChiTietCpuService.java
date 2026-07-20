package com.example.backend.service;

import com.example.backend.entity.ChiTietCpu;
import com.example.backend.repository.ChiTietCpuRepository;
import com.example.backend.repository.DmCpuRepository;
import com.example.backend.request.ChiTietCpuRequest;
import com.example.backend.response.ChiTietCpuResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChiTietCpuService {

    @Autowired
    private ChiTietCpuRepository chiTietCpuRepository;
    @Autowired
    private DmCpuRepository dmCpuRepository;

    public List<ChiTietCpuResponse> hienThiChiTietCpu() {
        return chiTietCpuRepository.hienThiChiTietCpu();
    }

    public ChiTietCpu getById(Integer id) {
        return chiTietCpuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Serial CPU không tồn tại với id: " + id));
    }

    public ChiTietCpu create(ChiTietCpuRequest request) {
        ChiTietCpu entity = new ChiTietCpu();
        BeanUtils.copyProperties(request, entity, "cpuId");
        entity.setCpu(dmCpuRepository.getReferenceById(request.getCpuId()));
        return chiTietCpuRepository.save(entity);
    }

    public ChiTietCpu update(Integer id, ChiTietCpuRequest request) {
        ChiTietCpu entity = getById(id);
        BeanUtils.copyProperties(request, entity, "chiTietCpuId", "cpuId");
        entity.setCpu(dmCpuRepository.getReferenceById(request.getCpuId()));
        return chiTietCpuRepository.save(entity);
    }

    // Chỉ cho xóa serial đang "trong_kho" (thêm nhầm) — đã dùng/lỗi bảo hành mà xóa sẽ
    // làm sai lịch sử nhập kho đã ghi nhận.
    public void delete(Integer id) {
        ChiTietCpu entity = getById(id);
        if (!"trong_kho".equals(entity.getTrangThai())) {
            throw new IllegalArgumentException("Chỉ được xóa serial đang ở trạng thái \"Trong kho\" (chưa dùng)");
        }
        chiTietCpuRepository.deleteById(id);
    }
}
