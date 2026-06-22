package com.example.backend.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class KhachHangRequest {
    @NotBlank(message = "Họ tên không được để trống")
    private String hoTen;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(max = 20, message = "Số điện thoại không vượt quá 20 ký tự")
    private String soDienThoai;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Địa chỉ không được để trống")
    private String diaChi;

    @NotBlank(message = "Loại khách không được để trống")
    private String loaiKhach;

    private String tenCongTy;

    private String maSoThue;

    @NotNull(message = "Điểm tích lũy không được để trống")
    @PositiveOrZero(message = "Điểm tích lũy phải lớn hơn hoặc bằng 0")
    private Integer diemTichLuy;

    @NotBlank(message = "Trạng thái không được để trống")
    private String trangThai;
}
