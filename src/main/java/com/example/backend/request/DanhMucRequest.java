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
public class DanhMucRequest {
    @NotBlank(message = "Tên danh mục không được để trống")
    private String tenDanhMuc;

    private String moTa;
    private String trangThai;
    private LocalDateTime ngayTao;
}
