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
public class ChiTietSanPhamResponse {
    private Integer chiTietId;
    private Integer bienTheId;
    private Integer phieuNhapId;
    private String maSku;
    private String soSerial;
    private String trangThai;
    private LocalDateTime ngayNhapKho;
    private String ghiChu;
    private Integer lockedBy;
    private LocalDateTime lockedAt;
    private String lockSession;
    private String lockedByTen; // Ho ten nhan vien lock
    private String tenSanPham; // Ten san pham (computed)
}
