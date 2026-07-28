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
public class ThanhToanRequest {
    @NotNull(message = "Đơn hàng không được để trống")
    private Integer donHangId;

    @NotNull(message = "Ngày thanh toán không được để trống")
    private LocalDateTime ngayThanhToan;

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String phuongThucThanhToan;

    @NotNull(message = "Số tiền không được để trống")
    @Positive(message = "Số tiền phải lớn hơn 0")
    private BigDecimal soTien;

    private String maGiaoDich;

    @NotBlank(message = "Trạng thái không được để trống")
    private String trangThai;

    private String ghiChu;
}
