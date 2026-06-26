package com.example.backend.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "Giá nhập không được để trống")
    private BigDecimal giaNhap;

    @NotNull(message = "Giá bán không được để trống")
    private BigDecimal giaBan;

    @NotNull(message = "Bảo hành không được để trống")
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
    private BigDecimal trongLuongKg;
}
