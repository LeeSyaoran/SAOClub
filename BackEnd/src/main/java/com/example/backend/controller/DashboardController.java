package com.example.backend.controller;

import com.example.backend.response.CustomerReportResponse;
import com.example.backend.response.DashboardKpiResponse;
import com.example.backend.response.ProductSalesResponse;
import com.example.backend.response.RevenueByDayResponse;
import com.example.backend.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
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
    public List<ProductSalesResponse> getTopSelling(
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        return dashboardService.getTopSelling(limit,
                tuNgay == null ? null : tuNgay.atStartOfDay(),
                denNgay == null ? null : LocalDateTime.of(denNgay, LocalTime.MAX));
    }

    @GetMapping("slow-selling")
    public List<ProductSalesResponse> getSlowSelling(
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        return dashboardService.getSlowSelling(limit,
                tuNgay == null ? null : tuNgay.atStartOfDay(),
                denNgay == null ? null : LocalDateTime.of(denNgay, LocalTime.MAX));
    }

    @GetMapping("doanh-thu-theo-ngay")
    public List<RevenueByDayResponse> getRevenueByDay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        return dashboardService.getRevenueByDay(tuNgay.atStartOfDay(), LocalDateTime.of(denNgay, LocalTime.MAX));
    }

    @GetMapping("khach-hang-noi-bat")
    public CustomerReportResponse getCustomerReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
            @RequestParam(defaultValue = "5") int limit) {
        return dashboardService.getCustomerReport(tuNgay.atStartOfDay(), LocalDateTime.of(denNgay, LocalTime.MAX), limit);
    }
}
