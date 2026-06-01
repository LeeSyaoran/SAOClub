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
@Table(name = "san_pham")
public class SanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "san_pham_id")
    private Integer sanPhamId;

    @Column(name = "ten_san_pham", length = 200)
    private String tenSanPham;

    @ManyToOne
    @JoinColumn(name = "thuong_hieu_id", nullable = false)
    private ThuongHieu thuongHieu;

    @ManyToOne
    @JoinColumn(name = "danh_muc_id", nullable = false)
    private DanhMuc danhMuc;

    @ManyToOne
    @JoinColumn(name = "nha_cung_cap_id")
    private NhaCungCap nhaCungCap;

    @Column(name = "ma_sku", length = 50, unique = true)
    private String maSku;

    @Column(name = "cpu", length = 100)
    private String cpu;

    @Column(name = "ram", length = 50)
    private String ram;

    @Column(name = "o_cung", length = 100)
    private String oCung;

    @Column(name = "gpu", length = 100)
    private String gpu;

    @Column(name = "kich_thuoc_man_hinh", length = 50)
    private String kichThuocManHinh;

    @Column(name = "he_dieu_hanh", length = 100)
    private String heDieuHanh;

    @Column(name = "pin", length = 50)
    private String pin;

    @Column(name = "trong_luong_kg", precision = 5, scale = 2)
    private BigDecimal trongLuongKg;

    @Column(name = "mau_sac", length = 50)
    private String mauSac;

    @Column(name = "gia_ban", precision = 18, scale = 2)
    private BigDecimal giaBan;

    @Column(name = "gia_nhap", precision = 18, scale = 2)
    private BigDecimal giaNhap;

    @Column(name = "bao_hanh_thang", nullable = false)
    private Integer baoHanhThang;

    @Column(name = "mo_ta", columnDefinition = "nvarchar(max)")
    private String moTa;

    @Column(name = "hinh_anh_chinh", length = 500)
    private String hinhAnhChinh;

    @Column(name = "trang_thai", length = 20)
    private String trangThai;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao;
}
