package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "chi_tiet_don_hang")
public class ChiTietDonHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chi_tiet_don_hang_id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "don_hang_id")
    private DonHang donHang;

    @ManyToOne
    @JoinColumn(name = "san_pham_id")
    private SanPham sanPham;

    @Column(name = "so_luong")
    private Integer soLuong;

    @Column(name = "don_gia", precision = 18, scale = 2)
    private BigDecimal donGia;

    @Column(name = "giam_gia_dong", precision = 18, scale = 2)
    private BigDecimal giamGiaDong;

    @Column(name = "thanh_tien",precision = 18, scale = 2)
    private BigDecimal thanhTien;

    @Column(name = "ghi_chu", length = 255)
    private String ghiChu;
}
