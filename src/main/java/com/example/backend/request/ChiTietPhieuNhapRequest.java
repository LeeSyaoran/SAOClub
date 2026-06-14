package com.example.backend.request;

import jakarta.validation.constraints.NotNull;
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
    private Integer soLuong;

    private BigDecimal donGiaNhap;
}
