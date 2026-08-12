package com.example.backend.service;

import com.example.backend.entity.ThanhToan;
import com.example.backend.repository.DonHangRepository;
import com.example.backend.repository.ThanhToanRepository;
import com.example.backend.request.ThanhToanRequest;
import com.example.backend.response.ThanhToanResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ThanhToanService {

    @Autowired
    private ThanhToanRepository thanhToanRepository;
    @Autowired
    private DonHangRepository donHangRepository;

    public List<ThanhToanResponse> hienThiThanhToan() {
        return thanhToanRepository.hienThiThanhToan();
    }

    public List<ThanhToanResponse> hienThiThanhToanTheoDonHang(Integer donHangId) {
        return thanhToanRepository.hienThiThanhToanTheoDonHang(donHangId);
    }

    public ThanhToan getById(Integer id) {
        return thanhToanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Thanh toán không tồn tại với id: " + id));
    }

    public ThanhToan create(ThanhToanRequest request) {
        ThanhToan entity = new ThanhToan();
        BeanUtils.copyProperties(request, entity, "donHangId");
        entity.setDonHang(donHangRepository.getReferenceById(request.getDonHangId()));
        return thanhToanRepository.save(entity);
    }

    public ThanhToan update(Integer id, ThanhToanRequest request) {
        ThanhToan entity = getById(id);
        BeanUtils.copyProperties(request, entity, "thanhToanId", "donHangId");
        entity.setDonHang(donHangRepository.getReferenceById(request.getDonHangId()));
        return thanhToanRepository.save(entity);
    }

    public void delete(Integer id) {
        if (!thanhToanRepository.existsById(id))
            throw new IllegalArgumentException("Thanh toán không tồn tại với id: " + id);
        thanhToanRepository.deleteById(id);
    }
}
