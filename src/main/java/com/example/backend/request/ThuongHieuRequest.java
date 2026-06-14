package com.example.backend.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ThuongHieuRequest {
    @NotBlank(message = "Tên thương hiệu không được để trống")
    private String tenThuongHieu;

    private String quocGia;
    private String moTa;
    private String trangThai;
    private LocalDateTime ngayTao;
}
