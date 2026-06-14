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
public class ChiTietDonHangRequest {
    @NotNull(message = "Đơn hàng không được để trống")
    private Integer donHangId;

    @NotNull(message = "Phiên bản sản phẩm không được để trống")
    private Integer bienTheId;

    private Integer chiTietId;

    @NotNull(message = "Số lượng không được để trống")
    private Integer soLuong;

    private BigDecimal donGia;
    private BigDecimal giamGiaDong;
    private String ghiChu;
}
