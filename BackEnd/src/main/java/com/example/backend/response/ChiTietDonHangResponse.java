package com.example.backend.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private String soSerial;
    private Integer soLuong;
    private BigDecimal donGia;
    private BigDecimal giamGiaDong;
    private BigDecimal thanhTien;
    private String ghiChu;
    private List<ChiTietDonHangSerialResponse> serials;

    public ChiTietDonHangResponse(Integer id, Integer donHangId, Integer bienTheId, String maSku,
                                  Integer chiTietId, String soSerial, Integer soLuong,
                                  BigDecimal donGia, BigDecimal giamGiaDong, BigDecimal thanhTien, String ghiChu) {
        this.id = id;
        this.donHangId = donHangId;
        this.bienTheId = bienTheId;
        this.maSku = maSku;
        this.chiTietId = chiTietId;
        this.soSerial = soSerial;
        this.soLuong = soLuong;
        this.donGia = donGia;
        this.giamGiaDong = giamGiaDong;
        this.thanhTien = thanhTien;
        this.ghiChu = ghiChu;
    }
}
