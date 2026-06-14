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

    @NotNull(message = "Sản phẩm không được để trống")
    private Integer sanPhamId;

    @NotNull(message = "Khách hàng không được để trống")
    private Integer khachHangId;

    @NotBlank(message = "Serial number không được để trống")
    private String serialNumber;

    @NotNull(message = "Ngày mua không được để trống")
    private LocalDateTime ngayMua;

    @NotNull(message = "Ngày hết bảo hành không được để trống")
    private LocalDateTime ngayHetBh;

    @NotNull(message = "Ngày tiếp nhận không được để trống")
    private LocalDateTime ngayTiepNhan;

    @NotNull(message = "Ngày trả khách không được để trống")
    private LocalDateTime ngayTraKhach;

    @NotBlank(message = "Mô tả lỗi không được để trống")
    private String moTaLoi;

    @NotBlank(message = "Kết quả xử lý không được để trống")
    private String ketQuaXuLy;

    @NotBlank(message = "Trạng thái không được để trống")
    private String trangThai;

    @NotNull(message = "Chi phí phát sinh không được để trống")
    @PositiveOrZero(message = "Chi phí phát sinh phải lớn hơn hoặc bằng 0")
    private BigDecimal chiPhiPhatSinh;

    @NotBlank(message = "Ghi chú không được để trống")
    private String ghiChu;
}
