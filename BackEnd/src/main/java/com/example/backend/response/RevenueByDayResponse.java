package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
public class RevenueByDayResponse {
    private LocalDate ngay;
    private BigDecimal doanhThu;
}
