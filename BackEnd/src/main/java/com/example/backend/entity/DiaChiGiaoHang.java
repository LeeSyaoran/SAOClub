package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "dia_chi_giao_hang")
public class DiaChiGiaoHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dia_chi_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khach_hang_id")
    private KhachHang khachHang;

    @Column(name = "ho_ten_nguoi_nhan", length = 150)
    private String hoTenNguoiNhan;

    @Column(name = "so_dien_thoai", length = 20)
    private String soDienThoai;

    @Column(name = "dia_chi", length = 255)
    private String diaChi;

    @Column(name = "thanh_pho", length = 100)
    private String thanhPho;

    @Column(name = "tinh", length = 100)
    private String tinh;

    @Column(name = "la_mac_dinh")
    private Boolean laMacDinh;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;
}
