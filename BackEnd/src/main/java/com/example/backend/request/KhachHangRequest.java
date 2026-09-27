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

    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Địa chỉ không được để trống")
    private String diaChi;

    private String loaiKhach;

    private String tenCongTy;

    private String maSoThue;

    @PositiveOrZero(message = "Điểm tích lũy phải lớn hơn hoặc bằng 0")
    private Integer diemTichLuy;

    private String trangThai;

    private String hinhAnh;

    public void setEmail(String email) {
        this.email = (email != null && email.trim().isEmpty()) ? null : email;
    }
}