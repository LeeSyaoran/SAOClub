package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class CaiDatHeThongResponse {
    private String tenCuaHang;
    private String diaChi;
    private String soDienThoai;
    private String email;
    private String maSoThue;
    private String logoUrl;
    private Integer nguongTonKhoMacDinh;
    private String ngonNguMacDinh;
    private String dinhDangSo;
}
