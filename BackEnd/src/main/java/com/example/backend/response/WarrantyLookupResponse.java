package com.example.backend.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WarrantyLookupResponse {

    // Serial
    private Integer chiTietId;
    private String soSerial;
    private String trangThaiSerial;
    private LocalDateTime ngayNhapKho;

    // BienThe
    private Integer bienTheId;
    private String maSku;
    private String barcode;
    private BigDecimal giaBan;
    private Integer baoHanhThang;
    private String hinhAnhBienThe;
    private String mauSac;
    private String kichThuocManHinh;
    private String heDieuHanh;
    private String pin;
    private BigDecimal trongLuongKg;

    // CPU/RAM/GPU/OCung
    private String cpuTen;
    private String ramTen;
    private String gpuTen;
    private String oCungTen;

    // SanPham
    private Integer sanPhamId;
    private String tenSanPham;
    private String maSanPham;

    // DonHang (neu serial da ban)
    private Integer donHangId;
    private String maDonHang;
    private LocalDateTime ngayGiaoThucTe;
    private LocalDateTime ngayHetBaoHanh;

    // KhachHang (neu serial da ban)
    private Integer khachHangId;
    private String tenKhachHang;
    private String soDienThoai;

    // Lich su phieu bao hanh cu
    private List<PhieuBaoHanhResponse> lichSuPhieuBaoHanh;
}
