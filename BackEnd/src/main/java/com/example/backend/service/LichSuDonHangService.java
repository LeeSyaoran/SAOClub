package com.example.backend.service;

import com.example.backend.repository.LichSuDonHangRepository;
import com.example.backend.response.LichSuDonHangResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LichSuDonHangService {

    @Autowired
    private LichSuDonHangRepository lichSuDonHangRepository;

    public List<LichSuDonHangResponse> getByDonHang(Integer donHangId) {
        return lichSuDonHangRepository.getByDonHangId(donHangId);
    }
}
