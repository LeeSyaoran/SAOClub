package com.example.backend.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class WarrantyStatusResponse {
    private Integer chiTietId;
    private String soSerial;
    private String maSku;
    private String tenSanPham;
    private Integer baoHanhThang;
    private LocalDateTime ngayGiaoThucTe;
    private String maDonHang;
    private String tenKhachHang;
    private String soDienThoaiKhachHang;
    private Integer donHangId;
    private Integer bienTheId;
    private Integer khachHangId;
    private LocalDateTime ngayHetBaoHanh;

    public WarrantyStatusResponse(Integer chiTietId, String soSerial, String maSku, String tenSanPham,
                                   Integer baoHanhThang, LocalDateTime ngayGiaoThucTe, String maDonHang,
                                   String tenKhachHang, String soDienThoaiKhachHang,
                                   Integer donHangId, Integer bienTheId, Integer khachHangId) {
        this.chiTietId = chiTietId;
        this.soSerial = soSerial;
        this.maSku = maSku;
        this.tenSanPham = tenSanPham;
        this.baoHanhThang = baoHanhThang;
        this.ngayGiaoThucTe = ngayGiaoThucTe;
        this.maDonHang = maDonHang;
        this.tenKhachHang = tenKhachHang;
        this.soDienThoaiKhachHang = soDienThoaiKhachHang;
        this.donHangId = donHangId;
        this.bienTheId = bienTheId;
        this.khachHangId = khachHangId;
    }
}
