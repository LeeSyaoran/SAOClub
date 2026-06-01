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
public class ThanhToanResponse {
    private Integer thanhToanId;
    private Integer donHangId;
    private LocalDateTime ngayThanhToan;
    private String phuongThucThanhToan;
    private BigDecimal soTien;
    private String maGiaoDich;
    private String trangThai;
    private String ghiChu;
}
