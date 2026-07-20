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
public class PhieuBaoHanhRequest {
    @NotNull(message = "Đơn hàng không được để trống")
    private Integer donHangId;

    @NotNull(message = "Biến thể sản phẩm không được để trống")
    private Integer bienTheId;

    @NotNull(message = "Khách hàng không được để trống")
    private Integer khachHangId;

    private Integer chiTietId;

    @NotNull(message = "Ngày mua không được để trống")
    private LocalDateTime ngayMua;

    @NotNull(message = "Ngày hết bảo hành không được để trống")
    private LocalDateTime ngayHetBh;

    private LocalDateTime ngayTiepNhan;

    private LocalDateTime ngayTraKhach;

    @NotBlank(message = "Mô tả lỗi không được để trống")
    private String moTaLoi;

    private String ketQuaXuLy;

    @NotBlank(message = "Trạng thái không được để trống")
    private String trangThai;

    @NotNull(message = "Chi phí phát sinh không được để trống")
    @PositiveOrZero(message = "Chi phí phát sinh phải lớn hơn hoặc bằng 0")
    private BigDecimal chiPhiPhatSinh;

    @NotBlank(message = "Ghi chú không được để trống")
    private String ghiChu;
}
