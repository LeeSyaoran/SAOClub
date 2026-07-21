package com.example.backend.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DmDoiThuongRequest {
    @NotBlank(message = "Tên phần thưởng không được để trống")
    private String ten;

    private String moTa;

    @NotNull(message = "Điểm cần đổi không được để trống")
    @Positive(message = "Điểm cần đổi phải lớn hơn 0")
    private Integer diemCan;

    private String loai;
    private BigDecimal giaTri;
    private BigDecimal giaTriToiDa;
    private String trangThai;
}
