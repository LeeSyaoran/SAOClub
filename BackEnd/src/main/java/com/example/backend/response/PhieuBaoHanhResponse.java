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
    private String tenSanPham;
    private Integer khachHangId;
    private String tenKhachHang;  // Thêm tên khách hàng
    private Integer chiTietId;
    private String soSerial;
    private LocalDateTime ngayMua;
    private LocalDateTime ngayHetBh;
    private LocalDateTime ngayTiepNhan;
    private LocalDateTime ngayBatDauXuLy;
    private LocalDateTime ngayTraKhach;
    private String moTaLoi;
    private String ketQuaXuLy;
    private String trangThai;
    private BigDecimal chiPhiPhatSinh;
    private String ghiChu;
    private String phuongThuc;
    private String diaChiLayHang;
    private String lyDoTuChoi;
}
