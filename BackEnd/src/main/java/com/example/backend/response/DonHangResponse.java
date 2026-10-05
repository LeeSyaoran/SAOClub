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
    private String phuongThucThanhToan;
    // POS: customer quick info
    private String khachHangHoTen;
    private String khachHangSdt;
    private String khachHangDiaChi;

    // Cancellation request info
    private Boolean yeuCauHuy;
    private String lyDoHuy;
    private LocalDateTime ngayYeuCauHuy;

    // Constructor 25 tham số tương thích ngược
    public DonHangResponse(
            Integer donHangId, String maDonHang, Integer khachHangId, Integer nhanVienId,
            Integer khuyenMaiId, Integer diaChiGiaoHangId, String diaChiGiaoHangText,
            String nguoiNhan, String sdtNguoiNhan, BigDecimal tongTien, BigDecimal giamGia,
            BigDecimal phiVanChuyen, BigDecimal thanhTien, LocalDateTime ngayDat,
            LocalDateTime ngayGiaoDuKien, LocalDateTime ngayGiaoThucTe, String trangThaiDonHang,
            String trangThaiThanhToan, String kenhBan, String ghiChu, String maVanDon,
            String phuongThucThanhToan, String khachHangHoTen, String khachHangSdt, String khachHangDiaChi
    ) {
        this(donHangId, maDonHang, khachHangId, nhanVienId, khuyenMaiId, diaChiGiaoHangId,
                diaChiGiaoHangText, nguoiNhan, sdtNguoiNhan, tongTien, giamGia, phiVanChuyen,
                thanhTien, ngayDat, ngayGiaoDuKien, ngayGiaoThucTe, trangThaiDonHang,
                trangThaiThanhToan, kenhBan, ghiChu, maVanDon, phuongThucThanhToan,
                khachHangHoTen, khachHangSdt, khachHangDiaChi, false, null, null);
    }
}
