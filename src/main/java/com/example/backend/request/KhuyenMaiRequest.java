package com.example.backend.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class KhuyenMaiRequest {
    @NotBlank(message = "Mã khuyến mãi không được để trống")
    private String maKhuyenMai;

    @NotBlank(message = "Tên khuyến mãi không được để trống")
    private String tenKhuyenMai;

    private String loai;
    private BigDecimal giaTri;
    private BigDecimal giaTriToiDa;
    private BigDecimal donHangToiThieu;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDateTime ngayBatDau;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDateTime ngayKetThuc;

    private Integer soLuongToiDa;
    private Integer soLanDaDung;
    private String trangThai;
    private LocalDateTime ngayTao;
}
