package com.example.backend.service;

import com.example.backend.entity.DmDoiThuong;
import com.example.backend.repository.DmDoiThuongRepository;
import com.example.backend.request.DmDoiThuongRequest;
import com.example.backend.response.DmDoiThuongResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DmDoiThuongService {

    @Autowired
    private DmDoiThuongRepository dmDoiThuongRepository;

    public List<DmDoiThuongResponse> hienThiDmDoiThuong() {
        return dmDoiThuongRepository.hienThiDmDoiThuong();
    }

    public DmDoiThuong getById(Integer id) {
        return dmDoiThuongRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phần thưởng không tồn tại với id: " + id));
    }

    public DmDoiThuong create(DmDoiThuongRequest request) {
        DmDoiThuong entity = new DmDoiThuong();
        BeanUtils.copyProperties(request, entity);
        entity.setNgayTao(LocalDateTime.now());
        return dmDoiThuongRepository.save(entity);
    }

    public DmDoiThuong update(Integer id, DmDoiThuongRequest request) {
        DmDoiThuong entity = getById(id);
        BeanUtils.copyProperties(request, entity, "doiThuongId", "ngayTao");
        return dmDoiThuongRepository.save(entity);
    }

    public void delete(Integer id) {
        if (!dmDoiThuongRepository.existsById(id))
            throw new IllegalArgumentException("Phần thưởng không tồn tại với id: " + id);
        dmDoiThuongRepository.deleteById(id);
    }
}
