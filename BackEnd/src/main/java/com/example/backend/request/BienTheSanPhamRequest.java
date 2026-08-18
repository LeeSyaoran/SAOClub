package com.example.backend.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

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
    private Integer oCungId;
    private Integer gpuId;
    private String kichThuocManHinh;
    private String heDieuHanh;
    private String pin;

    @PositiveOrZero(message = "Trọng lượng phải lớn hơn hoặc bằng 0")
    private BigDecimal trongLuongKg;
}