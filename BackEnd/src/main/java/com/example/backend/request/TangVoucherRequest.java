package com.example.backend.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
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
public class TangVoucherRequest {
    @NotBlank(message = "Loại voucher không được để trống")
    @Pattern(regexp = "percent|fixed", message = "Loại voucher phải là 'percent' hoặc 'fixed'")
    private String loai;

    @NotNull(message = "Giá trị không được để trống")
    @Positive(message = "Giá trị phải lớn hơn 0")
    private BigDecimal giaTri;

    private BigDecimal giaTriToiDa;

    @NotNull(message = "Hạn sử dụng không được để trống")
    private LocalDateTime ngayHetHan;

    private BigDecimal donHangToiThieu;
}
