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
public class LichSuQuayResponse {
    private Integer id;
    private LocalDateTime ngayQuay;
    private String ketQua;
    private String tenKhuyenMai; 
    private Integer diemDaTru;
}
