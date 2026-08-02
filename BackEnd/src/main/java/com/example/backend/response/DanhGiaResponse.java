package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DanhGiaResponse {
    private Integer danhGiaId;
    private Integer khachHangId;
    private String tenKhachHang;
    private Integer soSao;
    private String noiDung;
    private LocalDateTime ngayDanhGia;
}
