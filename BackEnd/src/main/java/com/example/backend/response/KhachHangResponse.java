package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class KhachHangResponse {
    private Integer khachHangId;
    private String hoTen;
    private String soDienThoai;
    private String email;
    private String diaChi;
    private String loaiKhach;
    private String tenCongTy;
    private String maSoThue;
    private Integer diemTichLuy;
    private java.math.BigDecimal soDuVi;
    private String trangThai;
    private LocalDateTime ngayTao;
}
