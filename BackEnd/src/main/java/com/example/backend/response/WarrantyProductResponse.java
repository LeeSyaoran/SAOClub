package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarrantyProductResponse {
    private Integer chiTietId;
    private Integer bienTheId;
    private String tenSanPham;
    private String tenBienThe;
    private String maSku;
    private String soSerial;
    private String hinhAnh;
    private Integer donHangId;
    private String maDonHang;
    private LocalDateTime ngayDat;
    private LocalDateTime ngayGiaoThucTe;
    private LocalDateTime ngayHetBaoHanh;
    private Integer baoHanhThang;
    private String trangThaiDonHang;
}
