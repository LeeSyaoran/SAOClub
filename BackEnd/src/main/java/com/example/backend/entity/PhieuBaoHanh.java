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
@Table(name = "phieu_bao_hanh")
public class PhieuBaoHanh {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bao_hanh_id")
    private Integer baoHanhId;

    @ManyToOne
    @JoinColumn(name = "don_hang_id", nullable = false)
    private DonHang donHang;

    @ManyToOne
    @JoinColumn(name = "san_pham_id", nullable = false)
    private SanPham sanPham;

    @ManyToOne
    @JoinColumn(name = "khach_hang_id", nullable = false)
    private KhachHang khachHang;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "ngay_mua", nullable = false)
    private LocalDateTime ngayMua;

    @Column(name = "ngay_het_bh", nullable = false)
    private LocalDateTime ngayHetBh;

    @Column(name = "ngay_tiep_nhan")
    private LocalDateTime ngayTiepNhan;

    @Column(name = "ngay_tra_khach")
    private LocalDateTime ngayTraKhach;

    @Column(name = "mo_ta_loi", length = 500)
    private String moTaLoi;

    @Column(name = "ket_qua_xu_ly", length = 500)
    private String ketQuaXuLy;

    @Column(name = "trang_thai", length = 30)
    private String trangThai;

    @Column(name = "chi_phi_phat_sinh", precision = 18, scale = 2)
    private BigDecimal chiPhiPhatSinh;

    @Column(name = "ghi_chu", length = 500)
    private String ghiChu;
}
