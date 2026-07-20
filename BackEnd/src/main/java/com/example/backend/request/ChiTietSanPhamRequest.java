package com.example.backend.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ChiTietSanPhamRequest {
    @NotNull(message = "Phiên bản sản phẩm không được để trống")
    private Integer bienTheId;

    @NotBlank(message = "Số serial không được để trống")
    private String soSerial;

    private String trangThai;
    private LocalDateTime ngayNhapKho;
    private String ghiChu;
}
