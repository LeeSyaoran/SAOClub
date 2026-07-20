package com.example.backend.service;

import com.example.backend.entity.ChiTietOcung;
import com.example.backend.repository.ChiTietOcungRepository;
import com.example.backend.repository.DmOcungRepository;
import com.example.backend.request.ChiTietOcungRequest;
import com.example.backend.response.ChiTietOcungResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChiTietOcungService {

    @Autowired
    private ChiTietOcungRepository chiTietOcungRepository;
    @Autowired
    private DmOcungRepository dmOcungRepository;

    public List<ChiTietOcungResponse> hienThiChiTietOcung() {
        return chiTietOcungRepository.hienThiChiTietOcung();
    }

    public ChiTietOcung getById(Integer id) {
        return chiTietOcungRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Serial ổ cứng không tồn tại với id: " + id));
    }

    public ChiTietOcung create(ChiTietOcungRequest request) {
        ChiTietOcung entity = new ChiTietOcung();
        BeanUtils.copyProperties(request, entity, "oCungId");
        entity.setOCung(dmOcungRepository.getReferenceById(request.getOCungId()));
        return chiTietOcungRepository.save(entity);
    }

    public ChiTietOcung update(Integer id, ChiTietOcungRequest request) {
        ChiTietOcung entity = getById(id);
        BeanUtils.copyProperties(request, entity, "chiTietOCungId", "oCungId");
        entity.setOCung(dmOcungRepository.getReferenceById(request.getOCungId()));
        return chiTietOcungRepository.save(entity);
    }

    public void delete(Integer id) {
        ChiTietOcung entity = getById(id);
        if (!"trong_kho".equals(entity.getTrangThai())) {
            throw new IllegalArgumentException("Chỉ được xóa serial đang ở trạng thái \"Trong kho\" (chưa dùng)");
        }
        chiTietOcungRepository.deleteById(id);
    }
}
