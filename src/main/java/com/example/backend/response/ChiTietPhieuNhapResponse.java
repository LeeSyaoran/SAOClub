package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ChiTietPhieuNhapResponse {
    private Integer id;
    private Integer phieuNhapId;
    private Integer bienTheId;
    private String maSku;
    private Integer soLuong;
    private BigDecimal donGiaNhap;
    private BigDecimal thanhTien;
}
