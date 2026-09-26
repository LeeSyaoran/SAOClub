package com.example.backend.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SanPhamChiTietResponse {
    // Thông tin sản phẩm
    private Integer sanPhamId;
    private String maSanPham;
    private String tenSanPham;
    private Integer thuongHieuId;
    private String tenThuongHieu;
    private Integer danhMucId;
    private String tenDanhMuc;
    private Integer nhaCungCapId;
    private String tenNhaCungCap;
    private String loaiSanPham;
    private String moTa;
    private String hinhAnhChinh;
    private List<String> hinhAnhList;
    private String trangThai;
    private LocalDateTime ngayTao;
    private LocalDateTime ngayCapNhat;
    private Long khachDat; // Số đơn đã đặt

    // Thông số chung
    private String kichThuocManHinh;
    private String heDieuHanh;
    private String pin;
    private BigDecimal trongLuongKg;
    private Integer baoHanhThang;
    private String phanLoaiTags;
    private String phanLoaiTen;
    private List<String> phanLoai; // Mã phân loại

    // Khoảng giá
    private String khoangGia;      // "10.000.000 – 15.000.000"

    // Biến thể
    private List<BienTheChiTietResponse> variants;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BienTheChiTietResponse {
        private Integer bienTheId;
        private String maSku;
        private String barcode;
        private String mauSac;
        private String cpu;
        private String ram;
        private String oCung;
        private String gpu;
        private BigDecimal giaNhap;
        private BigDecimal giaBan;
        private String trangThai;
        private Long soLuongTon;
    }
}
