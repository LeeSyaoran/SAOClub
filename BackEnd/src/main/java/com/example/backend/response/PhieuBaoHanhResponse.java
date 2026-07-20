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
public class PhieuBaoHanhResponse {
    private Integer baoHanhId;
    private Integer donHangId;
    private Integer bienTheId;
    private String maSku;
    private Integer khachHangId;
    private Integer chiTietId;
    private String soSerial;
    private LocalDateTime ngayMua;
    private LocalDateTime ngayHetBh;
    private LocalDateTime ngayTiepNhan;
    private LocalDateTime ngayTraKhach;
    private String moTaLoi;
    private String ketQuaXuLy;
    private String trangThai;
    private BigDecimal chiPhiPhatSinh;
    private String ghiChu;
}
