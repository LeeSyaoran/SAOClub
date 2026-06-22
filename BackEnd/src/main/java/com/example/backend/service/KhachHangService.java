package com.example.backend.service;

import com.example.backend.entity.KhachHang;
import com.example.backend.repository.KhachHangRepository;
import com.example.backend.request.KhachHangRequest;
import com.example.backend.response.KhachHangResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KhachHangService {

    @Autowired
    private KhachHangRepository khachHangRepository;

    public List<KhachHangResponse> hienThiKhachHang() {
        return khachHangRepository.hienThiKhachHang();
    }

    public KhachHang getById(Integer id) {
        return khachHangRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại với id: " + id));
    }

    public KhachHang create(KhachHangRequest request) {
        KhachHang entity = new KhachHang();
        // BeanUtils copies: hoTen, soDienThoai, email, diaChi, loaiKhach, tenCongTy, maSoThue, diemTichLuy, trangThai
        BeanUtils.copyProperties(request, entity);
        entity.setNgayTao(LocalDateTime.now());
        return khachHangRepository.save(entity);
    }

    public KhachHang update(Integer id, KhachHangRequest request) {
        KhachHang entity = getById(id);
        BeanUtils.copyProperties(request, entity, "khachHangId", "ngayTao");
        return khachHangRepository.save(entity);
    }

    public void delete(Integer id) {
        if (!khachHangRepository.existsById(id))
            throw new IllegalArgumentException("Khách hàng không tồn tại với id: " + id);
        khachHangRepository.deleteById(id);
    }
}
