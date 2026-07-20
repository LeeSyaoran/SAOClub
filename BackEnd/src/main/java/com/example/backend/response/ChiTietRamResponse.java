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
public class ChiTietRamResponse {
    private Integer chiTietRamId;
    private Integer ramId;
    private String dungLuong;
    private String soSerial;
    private String trangThai;
    private LocalDateTime ngayNhapKho;
    private String ghiChu;
}
