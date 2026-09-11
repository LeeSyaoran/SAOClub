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

    // Constructor mới với locking fields
    public ChiTietSanPhamResponse(Integer chiTietId, Integer bienTheId, Integer phieuNhapId,
        String maSku, String soSerial, String trangThai, LocalDateTime ngayNhapKho,
        String ghiChu, Integer lockedBy, LocalDateTime lockedAt, String lockSession, String lockedByTen) {
        this.chiTietId = chiTietId;
        this.bienTheId = bienTheId;
        this.phieuNhapId = phieuNhapId;
        this.maSku = maSku;
        this.soSerial = soSerial;
        this.trangThai = trangThai;
        this.ngayNhapKho = ngayNhapKho;
        this.ghiChu = ghiChu;
        this.lockedBy = lockedBy;
        this.lockedAt = lockedAt;
        this.lockSession = lockSession;
        this.lockedByTen = lockedByTen;
    }
}
