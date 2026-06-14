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
public class PhieuNhapKhoResponse {
    private Integer phieuNhapId;
    private String maPhieuNhap;
    private Integer nhaCungCapId;
    private Integer nhanVienId;
    private LocalDateTime ngayNhap;
    private BigDecimal tongTien;
    private String trangThai;
    private String ghiChu;
}
