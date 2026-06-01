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
@Table(name = "chi_tiet_tra_hang")
public class ChiTietTraHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chi_tiet_tra_id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "phieu_tra_id")
    private PhieuTraHang phieuTraHang;

    @ManyToOne
    @JoinColumn(name = "san_pham_id")
    private SanPham sanPham;

    @Column(name = "so_luong")
    private Integer soLuong;

    @Column(name = "don_gia_hoan", precision = 18, scale = 2)
    private BigDecimal donGiaHoan;

    @Column(name = "tinh_trang", length = 50)
    private String tinhTrang;
}
