package com.example.backend.request;

import jakarta.validation.constraints.NotNull;
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
public class ChiTietTraHangRequest {
    @NotNull(message = "Phiếu trả hàng không được để trống")
    private Integer phieuTraId;

    @NotNull(message = "Phiên bản sản phẩm không được để trống")
    private Integer bienTheId;

    private Integer chiTietId;

    @NotNull(message = "Số lượng không được để trống")
    @PositiveOrZero(message = "Số lượng phải lớn hơn hoặc bằng 0")
    private Integer soLuong;

    private BigDecimal donGiaHoan;
    private String tinhTrang;
}
