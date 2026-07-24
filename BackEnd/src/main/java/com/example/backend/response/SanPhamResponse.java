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
public class SanPhamResponse {
    private Integer sanPhamId;
    private Integer bienTheId;
    private String tenSanPham;
    private Integer danhMucId;
    private String tenDanhMuc;
    private Integer thuongHieuId;
    private String tenThuongHieu;
    private String tenNhaCungCap;
    private Integer nhaCungCapId;
    private String loaiSanPham;
    private String maSku;
    private String cpu;
    private String ram;
    private String oCung;
    private String gpu;
    private String kichThuocManHinh;
    private String heDieuHanh;
    private String pin;
    private BigDecimal trongLuongKg;
    private String mauSac;
    private BigDecimal giaBan;
    private BigDecimal giaNhap;
    private Integer baoHanhThang;
    private String moTa;
    private String hinhAnhChinh;
    private String trangThai;
    private LocalDateTime ngayTao;
    private String phanLoaiTags;
    private String phanLoaiTen;
    private Long soLuongTon;
}
