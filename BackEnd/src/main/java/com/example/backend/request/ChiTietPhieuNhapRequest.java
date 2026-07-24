package com.example.backend.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ChiTietPhieuNhapRequest {
    @NotNull(message = "Phiếu nhập không được để trống")
    private Integer phieuNhapId;

    @NotNull(message = "Phiên bản sản phẩm không được để trống")
    private Integer bienTheId;

    @NotNull(message = "Số lượng không được để trống")
    @Positive(message = "Số lượng phải lớn hơn 0")
    private Integer soLuong;

    @PositiveOrZero(message = "Đơn giá nhập phải lớn hơn hoặc bằng 0")
    private BigDecimal donGiaNhap;
}
