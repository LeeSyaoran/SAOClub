package com.example.backend.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class LichSuTonKhoRequest {
    @NotNull(message = "Biến thể sản phẩm không được để trống")
    private Integer bienTheId;

    private Integer chiTietId;
    private String loaiBienDong;

    @NotNull(message = "Số lượng thay đổi không được để trống")
    @PositiveOrZero(message = "Số lượng thay đổi phải lớn hơn hoặc bằng 0")
    private Integer soLuongThayDoi;

    private Integer donHangId;
    private Integer phieuNhapId;
    private Integer nhanVienId;
    private String ghiChu;
    private LocalDateTime ngayTao;
}
