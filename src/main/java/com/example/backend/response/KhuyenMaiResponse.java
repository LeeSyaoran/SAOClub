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
public class KhuyenMaiResponse {
    private Integer khuyenMaiId;
    private String maKhuyenMai;
    private String tenKhuyenMai;
    private String loai;
    private BigDecimal giaTri;
    private BigDecimal giaTriToiDa;
    private BigDecimal donHangToiThieu;
    private LocalDateTime ngayBatDau;
    private LocalDateTime ngayKetThuc;
    private Integer soLuongToiDa;
    private Integer soLanDaDung;
    private String trangThai;
    private LocalDateTime ngayTao;
}
