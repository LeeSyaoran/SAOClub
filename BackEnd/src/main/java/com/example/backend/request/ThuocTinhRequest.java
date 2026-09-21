package com.example.backend.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ThuocTinhRequest {

    private Integer thuocTinhId;

    @NotBlank(message = "Tên trường không được trống")
    private String tenTruong;

    @NotBlank(message = "Tên hiển thị không được trống")
    private String tenHienThi;

    private String loaiDuLieu = "text"; // text | select

    private Boolean batBuoc = false;

    private Integer thuTuHienThi = 0;

    private String trangThai = "active";
}
