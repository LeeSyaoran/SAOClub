package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@AllArgsConstructor
@Getter
@Setter
public class DashboardKpiResponse {
    private long tongSanPham;
    private long tongDonHang;
    private long tongKhachHang;
    private BigDecimal doanhThu;
    private long sapHetHang;
}
