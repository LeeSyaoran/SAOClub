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
public class ChiTietTraHangResponse {
    private Integer id;
    private Integer phieuTraId;
    private Integer bienTheId;
    private String maSku;
    private Integer chiTietId;
    private String soSerial;
    private Integer soLuong;
    private BigDecimal donGiaHoan;
    private String tinhTrang;

    public ChiTietTraHangResponse(Integer id, Integer phieuTraId, Integer bienTheId, String maSku,
                                  Integer chiTietId, Integer soLuong, BigDecimal donGiaHoan, String tinhTrang) {
        this.id = id;
        this.phieuTraId = phieuTraId;
        this.bienTheId = bienTheId;
        this.maSku = maSku;
        this.chiTietId = chiTietId;
        this.soLuong = soLuong;
        this.donGiaHoan = donGiaHoan;
        this.tinhTrang = tinhTrang;
    }
}
