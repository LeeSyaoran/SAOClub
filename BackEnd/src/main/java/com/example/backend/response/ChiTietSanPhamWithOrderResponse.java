package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Serial đang bị giữ hoặc đang lên đơn kèm thông tin đơn hàng và người thực hiện.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ChiTietSanPhamWithOrderResponse {
    // Serial
    private Integer chiTietId;
    private Integer bienTheId;
    private String maSku;
    private String soSerial;
    private String trangThai;
    private LocalDateTime ngayNhapKho;
    private String tenSanPham;
    // Lock info (POS)
    private Integer lockedBy;
    private LocalDateTime lockedAt;
    private String lockSession;
    private String lockedByTen; // tên nhân viên lock POS
    // Order info (online / in_store)
    private Integer donHangId;
    private String maDonHang;
    private String trangThaiDonHang;
    private String khachHangTen;
    private String khachHangSdt;
    private LocalDateTime ngayDat;
    private String loai; // "pos_lock" | "online_held"
    // Performer info
    private Integer nhanVienId;
    private String nhanVienTen;
    private String nhanVienRole; // "admin" | "nhan_vien" | etc.

    public ChiTietSanPhamWithOrderResponse(
            Integer chiTietId, Integer bienTheId, String maSku, String soSerial, String trangThai,
            LocalDateTime ngayNhapKho, String tenSanPham,
            Integer lockedBy, LocalDateTime lockedAt, String lockSession, String lockedByTen,
            Integer donHangId, String maDonHang, String trangThaiDonHang,
            String khachHangTen, String khachHangSdt, LocalDateTime ngayDat,
            String loai
    ) {
        this(chiTietId, bienTheId, maSku, soSerial, trangThai, ngayNhapKho, tenSanPham,
             lockedBy, lockedAt, lockSession, lockedByTen,
             donHangId, maDonHang, trangThaiDonHang, khachHangTen, khachHangSdt, ngayDat, loai,
             null, null, null);
    }
}
