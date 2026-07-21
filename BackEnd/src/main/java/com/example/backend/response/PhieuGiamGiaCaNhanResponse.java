package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PhieuGiamGiaCaNhanResponse {
    private Integer phieuId;
    private String maPhieu;
    private String loai;
    private BigDecimal giaTri;
    private BigDecimal giaTriToiDa;
    private Boolean daSuDung;
    private LocalDateTime ngayDoi;
    private LocalDateTime ngayHetHan;
}
