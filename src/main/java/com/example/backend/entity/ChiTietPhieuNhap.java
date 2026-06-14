package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "chi_tiet_phieu_nhap")
public class ChiTietPhieuNhap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chi_tiet_nhap_id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "phieu_nhap_id")
    private PhieuNhapKho phieuNhapKho;

    @ManyToOne
    @JoinColumn(name = "bien_the_id")
    private BienTheSanPham bienThe;

    @Column(name = "so_luong")
    private Integer soLuong;

    @Column(name = "don_gia_nhap", precision = 18, scale = 2)
    private BigDecimal donGiaNhap;

    @Column(name = "thanh_tien", precision = 18, scale = 2, insertable = false, updatable = false)
    private BigDecimal thanhTien;
}
