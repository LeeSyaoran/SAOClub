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
public class ChiTietDonHangResponse {
    private Integer id;
    private Integer donHangId;
    private Integer bienTheId;
    private String maSku;
    private Integer chiTietId;
    private Integer soLuong;
    private BigDecimal donGia;
    private BigDecimal giamGiaDong;
    private BigDecimal thanhTien;
    private String ghiChu;
}
