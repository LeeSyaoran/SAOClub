package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BienTheSanPhamResponse {
    private Integer bienTheId;
    private Integer sanPhamId;
    private String maSku;
    private BigDecimal giaNhap;
    private BigDecimal giaBan;
    private Integer baoHanhThang;
    private String hinhAnhBienThe;
    private String trangThai;
    private String mauSac;
    private Integer cpuId;
    private String cpuName;
    private Integer ramId;
    private String ramValue;
    private Integer oCungId;
    private String oCungValue;
    private Integer gpuId;
    private String gpuName;
    private String kichThuocManHinh;
    private String heDieuHanh;
    private String pin;
    private BigDecimal trongLuongKg;
}
