package com.example.backend.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BienTheSanPhamRequest {
    @NotNull(message = "Sản phẩm không được để trống")
    private Integer sanPhamId;

    @NotBlank(message = "Mã SKU không được để trống")
    private String maSku;

    @Pattern(regexp = "^$|^\\d{8,13}$", message = "Barcode phải gồm 8–13 chữ số")
    private String barcode;

    @com.fasterxml.jackson.annotation.JsonProperty("barcodeBienThe")
    public void setBarcodeBienTheAlias(String barcodeBienThe) {
        if (this.barcode == null || this.barcode.isBlank()) {
            this.barcode = barcodeBienThe;
        }
    }

    public String getBarcodeBienThe() {
        return this.barcode;
    }

    @NotNull(message = "Giá nhập không được để trống")
    @PositiveOrZero(message = "Giá nhập phải lớn hơn hoặc bằng 0")
    private BigDecimal giaNhap;

    @NotNull(message = "Giá bán không được để trống")
    @PositiveOrZero(message = "Giá bán phải lớn hơn hoặc bằng 0")
    private BigDecimal giaBan;

    @NotNull(message = "Bảo hành không được để trống")
    @PositiveOrZero(message = "Bảo hành tháng phải lớn hơn hoặc bằng 0")
    private Integer baoHanhThang;

    private String hinhAnhBienThe;
    private String trangThai;
    private String mauSac;
    private Integer cpuId;
    private Integer ramId;

    @com.fasterxml.jackson.annotation.JsonProperty("oCungId")
    private Integer oCungId;

    private Integer gpuId;

    @com.fasterxml.jackson.annotation.JsonProperty("ocungId")
    public void setOcungIdAlias(Integer id) {
        if (this.oCungId == null) this.oCungId = id;
    }
    private String kichThuocManHinh;
    private String heDieuHanh;
    private String pin;

    @PositiveOrZero(message = "Trọng lượng (kg) phải lớn hơn hoặc bằng 0")
    @DecimalMax(value = "5.00", message = "Trọng lượng (kg) tối đa của máy tính là 5 kg (vui lòng nhập theo đơn vị kg, VD: 1.7)")
    private BigDecimal trongLuongKg;

    private String moTa;
}