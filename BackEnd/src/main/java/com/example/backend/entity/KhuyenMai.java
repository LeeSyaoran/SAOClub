package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "khuyen_mai")
public class KhuyenMai {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "khuyen_mai_id")
    private Integer khuyenMaiId;

    @Column(name = "ma_khuyen_mai", length = 50, unique = true)
    private String maKhuyenMai;

    @Column(name = "ten_khuyen_mai", length = 150)
    private String tenKhuyenMai;

    @Column(name = "loai", length = 20)
    private String loai;

    @Column(name = "gia_tri", precision = 18, scale = 2)
    private BigDecimal giaTri;

    @Column(name = "gia_tri_toi_da", precision = 18, scale = 2)
    private BigDecimal giaTriToiDa;

    @Column(name = "don_hang_toi_thieu", precision = 18, scale = 2)
    private BigDecimal donHangToiThieu;

    @Column(name = "ngay_bat_dau", nullable = false)
    private LocalDateTime ngayBatDau;

    @Column(name = "ngay_ket_thuc", nullable = false)
    private LocalDateTime ngayKetThuc;

    @Column(name = "so_luong_toi_da")
    private Integer soLuongToiDa;

    @Column(name = "so_lan_da_dung", nullable = false)
    private Integer soLanDaDung;

    @Column(name = "trang_thai", length = 20)
    private String trangThai;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao;
}
