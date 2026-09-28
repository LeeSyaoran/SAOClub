package com.example.backend.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private List<Integer> phanLoaiIds; // ID phân loại

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
        private Integer cpuId;
        private String ram;
        private Integer ramId;

        @JsonProperty("oCung")
        private String oCung;

        @JsonProperty("oCungId")
        private Integer oCungId;

        private String gpu;
        private Integer gpuId;
        private BigDecimal giaNhap;
        private BigDecimal giaBan;
        private String trangThai;
        private Long soLuongTon;
        private String hinhAnhBienThe;
        private Integer baoHanhThang;
        private String kichThuocManHinh;
        private String heDieuHanh;
        private String pin;
        private BigDecimal trongLuongKg;
        private String moTa;

        @JsonProperty("ocung")
        public String getOcungAlias() {
            return oCung;
        }

        @JsonProperty("ocungId")
        public Integer getOcungIdAlias() {
            return oCungId;
        }

        // Constructor tương thích ngược cho SanPhamService
        public BienTheChiTietResponse(Integer bienTheId, String maSku, String barcode, String mauSac,
                                     String cpu, String ram, String oCung, String gpu,
                                     BigDecimal giaNhap, BigDecimal giaBan, String trangThai, Long soLuongTon) {
            this.bienTheId = bienTheId;
            this.maSku = maSku;
            this.barcode = barcode;
            this.mauSac = mauSac;
            this.cpu = cpu;
            this.ram = ram;
            this.oCung = oCung;
            this.gpu = gpu;
            this.giaNhap = giaNhap;
            this.giaBan = giaBan;
            this.trangThai = trangThai;
            this.soLuongTon = soLuongTon;
        }
    }
}
