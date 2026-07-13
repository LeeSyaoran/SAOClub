package com.example.backend.service;

import com.example.backend.repository.DonHangRepository;
import com.example.backend.repository.KhachHangRepository;
import com.example.backend.repository.SanPhamRepository;
import com.example.backend.repository.TonKhoRepository;
import com.example.backend.response.DashboardKpiResponse;
import com.example.backend.response.ProductSalesResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {

    @Autowired
    private SanPhamRepository sanPhamRepository;
    @Autowired
    private DonHangRepository donHangRepository;
    @Autowired
    private KhachHangRepository khachHangRepository;
    @Autowired
    private TonKhoRepository tonKhoRepository;

    public DashboardKpiResponse getKpi() {
        return new DashboardKpiResponse(
                sanPhamRepository.count(),
                donHangRepository.count(),
                khachHangRepository.count(),
                donHangRepository.sumDoanhThu(),
                tonKhoRepository.countLowStock());
    }

    public List<ProductSalesResponse> getTopSelling(int limit) {
        return sanPhamRepository.topSelling(PageRequest.of(0, limit));
    }

    public List<ProductSalesResponse> getSlowSelling(int limit) {
        return sanPhamRepository.slowSelling(PageRequest.of(0, limit));
    }
}
