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
    private Integer soLuong;
    private BigDecimal donGiaHoan;
    private String tinhTrang;
}
