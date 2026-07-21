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
public class DonHangResponse {
    private Integer donHangId;
    private String maDonHang;
    private Integer khachHangId;
    private Integer nhanVienId;
    private Integer khuyenMaiId;
    private Integer diaChiGiaoHangId;
    private String diaChiGiaoHangText;
    private String nguoiNhan;
    private String sdtNguoiNhan;
    private BigDecimal tongTien;
    private BigDecimal giamGia;
    private BigDecimal phiVanChuyen;
    private BigDecimal thanhTien;
    private LocalDateTime ngayDat;
    private LocalDateTime ngayGiaoDuKien;
    private LocalDateTime ngayGiaoThucTe;
    private String trangThaiDonHang;
    private String trangThaiThanhToan;
    private String kenhBan;
    private String ghiChu;
    private String maVanDon;
}
