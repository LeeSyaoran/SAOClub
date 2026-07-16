package com.example.backend.service;

import com.example.backend.repository.DonHangRepository;
import com.example.backend.repository.KhachHangRepository;
import com.example.backend.repository.SanPhamRepository;
import com.example.backend.repository.TonKhoRepository;
import com.example.backend.response.CustomerReportResponse;
import com.example.backend.response.CustomerSpendingResponse;
import com.example.backend.response.DashboardKpiResponse;
import com.example.backend.response.ProductSalesResponse;
import com.example.backend.response.RevenueByDayResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    public List<ProductSalesResponse> getTopSelling(int limit, LocalDateTime tuNgay, LocalDateTime denNgay) {
        return sanPhamRepository.topSelling(tuNgay, denNgay, PageRequest.of(0, limit));
    }

    public List<ProductSalesResponse> getSlowSelling(int limit, LocalDateTime tuNgay, LocalDateTime denNgay) {
        return sanPhamRepository.slowSelling(tuNgay, denNgay, PageRequest.of(0, limit));
    }

    public List<RevenueByDayResponse> getRevenueByDay(LocalDateTime tuNgay, LocalDateTime denNgay) {
        return donHangRepository.doanhThuTheoNgay(tuNgay, denNgay);
    }

    // Top khách chi tiêu nhiều nhất + tỷ lệ khách mua từ 2 đơn trở lên trong khoảng ngày —
    // lấy hết (Pageable.unpaged()) để đếm đúng tỷ lệ mua lại trên toàn bộ khách có đơn
    // trong khoảng, rồi mới cắt ra top N để hiển thị bảng.
    public CustomerReportResponse getCustomerReport(LocalDateTime tuNgay, LocalDateTime denNgay, int limit) {
        List<CustomerSpendingResponse> all = khachHangRepository.chiTieuTheoKhachHang(tuNgay, denNgay, Pageable.unpaged());
        long soKhachMuaLai = all.stream().filter(c -> c.getSoDonHang() >= 2).count();
        double tyLeMuaLai = all.isEmpty() ? 0 : (double) soKhachMuaLai / all.size();
        List<CustomerSpendingResponse> top = all.stream().limit(limit).toList();
        return new CustomerReportResponse(top, tyLeMuaLai, all.size());
    }
}
