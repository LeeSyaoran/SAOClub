package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@AllArgsConstructor
@Getter
@Setter
public class CustomerSpendingResponse {
    private Integer khachHangId;
    private String hoTen;
    private Long soDonHang;
    private BigDecimal tongChiTieu;
}
