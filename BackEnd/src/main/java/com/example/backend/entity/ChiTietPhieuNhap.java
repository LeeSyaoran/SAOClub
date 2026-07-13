package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Formula;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phieu_nhap_id")
    private PhieuNhapKho phieuNhapKho;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bien_the_id")
    private BienTheSanPham bienThe;

    @Column(name = "so_luong")
    private Integer soLuong;

    @Column(name = "don_gia_nhap", precision = 18, scale = 0)
    private BigDecimal donGiaNhap;

    @Formula("(so_luong * don_gia_nhap)")
    private BigDecimal thanhTien;
}
