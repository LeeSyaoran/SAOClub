package com.example.backend.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class CaiDatHeThongRequest {
    @NotBlank(message = "Tên cửa hàng không được để trống")
    private String tenCuaHang;
    private String diaChi;
    private String soDienThoai;
    private String email;
    private String maSoThue;
    private String logoUrl;
    private String ngonNguMacDinh;
    private String dinhDangSo;
}
