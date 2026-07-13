package com.example.backend.controller;

import com.example.backend.response.DashboardKpiResponse;
import com.example.backend.response.ProductSalesResponse;
import com.example.backend.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Endpoint tổng hợp cho Dashboard admin — SUM/COUNT/GROUP BY chạy ở SQL, thay vì tải
// toàn bộ san_pham/don_hang/chi_tiet_don_hang về trình duyệt rồi cộng dồn bằng JS.
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("kpi")
    public DashboardKpiResponse getKpi() {
        return dashboardService.getKpi();
    }

    @GetMapping("top-selling")
    public List<ProductSalesResponse> getTopSelling(@RequestParam(defaultValue = "5") int limit) {
        return dashboardService.getTopSelling(limit);
    }

    @GetMapping("slow-selling")
    public List<ProductSalesResponse> getSlowSelling(@RequestParam(defaultValue = "5") int limit) {
        return dashboardService.getSlowSelling(limit);
    }
}
