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
public class LichSuTonKhoResponse {
    private Integer lichSuId;
    private Integer bienTheId;
    private String maSku;
    private Integer chiTietId;
    private String loaiBienDong;
    private Integer soLuongThayDoi;
    private Integer donHangId;
    private Integer phieuNhapId;
    private Integer nhanVienId;
    private String ghiChu;
    private LocalDateTime ngayTao;
}
