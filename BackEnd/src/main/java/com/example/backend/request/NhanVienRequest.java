package com.example.backend.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class NhanVienRequest {
    @NotBlank(message = "Họ tên không được để trống")
    private String hoTen;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(max = 20, message = "Số điện thoại không vượt quá 20 ký tự")
    private String soDienThoai;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotNull(message = "Chức vụ không được để trống")
    private Integer chucVuId;

    @NotBlank(message = "Username không được để trống")
    private String username;

    @NotBlank(message = "Mật khẩu hash không được để trống")
    private String matKhauHash;

    @NotNull(message = "Lương cơ bản không được để trống")
    @PositiveOrZero(message = "Lương cơ bản phải lớn hơn hoặc bằng 0")
    private BigDecimal luongCoBan;

    @NotBlank(message = "Trạng thái không được để trống")
    private String trangThai;
}
