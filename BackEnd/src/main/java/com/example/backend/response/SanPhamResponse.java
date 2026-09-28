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
    private String maSanPham;
    private Integer bienTheId;
    private String tenSanPham;
    private Integer danhMucId;
    private String tenDanhMuc;
    private Integer maDanhMuc;  // cùng dm.id với danhMucId
    private Integer thuongHieuId;
    private String tenThuongHieu;
    private String tenNhaCungCap;
    private Integer nhaCungCapId;
    private String loaiSanPham;
    private String maSku;
    private String barcode;        // Barcode chính thức của biến thể (thay thế cho barcode sản phẩm cha cũ)
    private String cpu;
    private String ram;

    @com.fasterxml.jackson.annotation.JsonProperty("oCung")
    private String oCung;

    private String gpu;

    private Integer cpuId;
    private Integer ramId;

    @com.fasterxml.jackson.annotation.JsonProperty("oCungId")
    private Integer oCungId;

    private Integer gpuId;

    @com.fasterxml.jackson.annotation.JsonProperty("ocung")
    public String getOcungAlias() {
        return oCung;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("ocung")
    public void setOcungAlias(String val) {
        if (this.oCung == null) this.oCung = val;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("ocungId")
    public Integer getOcungIdAlias() {
        return oCungId;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("ocungId")
    public void setOcungIdAlias(Integer val) {
        if (this.oCungId == null) this.oCungId = val;
    }
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
    private LocalDateTime ngayCapNhat;
    private String phanLoaiTags;
    private String phanLoaiTen;
    private Long soLuongTon;
}