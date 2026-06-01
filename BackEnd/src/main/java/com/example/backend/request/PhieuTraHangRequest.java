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
public class PhieuTraHangRequest {
    @NotNull(message = "Đơn hàng không được để trống")
    private Integer donHangId;

    @NotNull(message = "Nhân viên không được để trống")
    private Integer nhanVienId;

    @NotBlank(message = "Lý do không được để trống")
    private String lyDo;

    @NotNull(message = "Ngày trả không được để trống")
    private LocalDateTime ngayTra;

    @NotBlank(message = "Trạng thái không được để trống")
    private String trangThai;

    @NotNull(message = "Số tiền hoàn không được để trống")
    @PositiveOrZero(message = "Số tiền hoàn phải lớn hơn hoặc bằng 0")
    private BigDecimal soTienHoan;

    @NotBlank(message = "Ghi chú không được để trống")
    private String ghiChu;
}
