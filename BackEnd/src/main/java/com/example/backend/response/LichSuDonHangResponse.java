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
public class LichSuDonHangResponse {
    private Integer lichSuId;
    private Integer donHangId;
    private String trangThaiCu;
    private String trangThaiMoi;
    private LocalDateTime thoiGian;
}
