package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PhieuTraHangResponse {
    private Integer phieuTraId;
    private Integer donHangId;
    private Integer nhanVienId;
    private String lyDo;
    private LocalDateTime ngayTra;
    private String trangThai;
    private BigDecimal soTienHoan;
    private String ghiChu;
}
