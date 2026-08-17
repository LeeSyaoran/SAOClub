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
@Table(name = "bien_the_san_pham")
public class BienTheSanPham extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bien_the_id")
    private Integer bienTheId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "san_pham_id", nullable = false)
    private SanPham sanPham;

    @Column(name = "ma_sku", length = 50, unique = true)
    private String maSku;

    @Column(name = "barcode", length = 50)
    private String barcode;

    @Column(name = "gia_nhap", precision = 18, scale = 0)
    private BigDecimal giaNhap;

    @Column(name = "gia_ban", precision = 18, scale = 0)
    private BigDecimal giaBan;

    @Column(name = "bao_hanh_thang")
    private Integer baoHanhThang;

    @Column(name = "hinh_anh_bien_the", length = 500)
    private String hinhAnhBienThe;

    @Column(name = "trang_thai", length = 20)
    private String trangThai;

    @Column(name = "mau_sac", length = 50)
    private String mauSac;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cpu_id")
    private DmCpu cpu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ram_id")
    private DmRam ram;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "o_cung_id")
    private DmOcung oCung;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gpu_id")
    private DmGpu gpu;

    @Column(name = "kich_thuoc_man_hinh", length = 50)
    private String kichThuocManHinh;

    @Column(name = "he_dieu_hanh", length = 100)
    private String heDieuHanh;

    @Column(name = "pin", length = 50)
    private String pin;

    @Column(name = "trong_luong_kg", precision = 5, scale = 2)
    private BigDecimal trongLuongKg;

    @Column(name = "phan_loai_tags", length = 200)
    private String phanLoaiTags;

    @Column(name = "phan_loai_ten", length = 200)
    private String phanLoaiTen;
}
