package com.example.backend.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DiaChiGiaoHangRequest {
    @NotNull(message = "Khách hàng không được để trống")
    private Integer khachHangId;

    @NotBlank(message = "Tên người nhận không được để trống")
    private String hoTenNguoiNhan;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String soDienThoai;

    @NotBlank(message = "Địa chỉ không được để trống")
    private String diaChi;

    private String thanhPho;
    private String tinh;
    private Boolean laMacDinh;
    private LocalDateTime ngayTao;
}
