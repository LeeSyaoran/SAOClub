package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class KhuyenMaiResponse {
    private Integer khuyenMaiId;
    private String maKhuyenMai;
    private String tenKhuyenMai;
    private String loai;
    private BigDecimal giaTri;
    private BigDecimal giaTriToiDa;
    private BigDecimal donHangToiThieu;
    private LocalDateTime ngayBatDau;
    private LocalDateTime ngayKetThuc;
    private Integer soLuongToiDa;
    private Integer soLanDaDung;
    private Integer soLuotConLai;
    private String trangThai;
    private LocalDateTime ngayTao;

    // Danh sách sản phẩm áp dụng - null hoặc rỗng = áp dụng cho tất cả
    private List<SanPhamSimpleResponse> sanPhams;

    public KhuyenMaiResponse(Integer khuyenMaiId, String maKhuyenMai, String tenKhuyenMai,
                             String loai, BigDecimal giaTri, BigDecimal giaTriToiDa, BigDecimal donHangToiThieu,
                             LocalDateTime ngayBatDau, LocalDateTime ngayKetThuc, Integer soLuongToiDa,
                             Integer soLanDaDung, Integer soLuotConLai, String trangThai, LocalDateTime ngayTao) {
        this(khuyenMaiId, maKhuyenMai, tenKhuyenMai, loai, giaTri, giaTriToiDa, donHangToiThieu,
                ngayBatDau, ngayKetThuc, soLuongToiDa, soLanDaDung, soLuotConLai, trangThai, ngayTao, null);
    }

    @lombok.Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SanPhamSimpleResponse {
        private Integer sanPhamId;
        private String tenSanPham;
        private String hinhAnhChinh;
    }
}
