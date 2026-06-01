package com.example.backend.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PhieuNhapKhoRequest {
    @NotNull(message = "Nhà cung cấp không được để trống")
    private Integer nhaCungCapId;

    @NotNull(message = "Nhân viên không được để trống")
    private Integer nhanVienId;

    @NotNull(message = "Ngày nhập không được để trống")
    private LocalDateTime ngayNhap;

    @NotNull(message = "Tổng tiền không được để trống")
    @PositiveOrZero(message = "Tổng tiền phải lớn hơn hoặc bằng 0")
    private BigDecimal tongTien;

    @NotBlank(message = "Trạng thái không được để trống")
    private String trangThai;

    @NotBlank(message = "Ghi chú không được để trống")
    private String ghiChu;
}
