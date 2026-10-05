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

    private Integer bienTheId;

    @NotNull(message = "Khách hàng không được để trống")
    private Integer khachHangId;

    private Integer chiTietId;

    private LocalDateTime ngayMua;

    private LocalDateTime ngayHetBh;

    private LocalDateTime ngayTiepNhan;

    private LocalDateTime ngayTraKhach;

    @NotBlank(message = "Mô tả lỗi không được để trống")
    private String moTaLoi;

    private String ketQuaXuLy;

    @NotBlank(message = "Trạng thái không được để trống")
    private String trangThai;

    @PositiveOrZero(message = "Chi phí phát sinh phải lớn hơn hoặc bằng 0")
    private BigDecimal chiPhiPhatSinh;

    private String ghiChu;

    private String phuongThuc;

    private String diaChiLayHang;

    private String lyDoTuChoi;
}
