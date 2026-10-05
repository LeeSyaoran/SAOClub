package com.example.backend.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    // Thêm tên sản phẩm và tên nhân viên để hiển thị trên bảng
    private String tenSanPham;
    private String tenNhanVien;
}
