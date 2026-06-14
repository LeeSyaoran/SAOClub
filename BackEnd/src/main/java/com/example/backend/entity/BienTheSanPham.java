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
public class BienTheSanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bien_the_id")
    private Integer bienTheId;

    @ManyToOne
    @JoinColumn(name = "san_pham_id", nullable = false)
    private SanPham sanPham;

    @Column(name = "ma_sku", length = 50, unique = true)
    private String maSku;

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

    @ManyToOne
    @JoinColumn(name = "cpu_id")
    private DmCpu cpu;

    @ManyToOne
    @JoinColumn(name = "ram_id")
    private DmRam ram;

    @ManyToOne
    @JoinColumn(name = "o_cung_id")
    private DmOcung oCung;

    @ManyToOne
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

    @Column(name = "man_hinh_dt", length = 100)
    private String manHinhDt;

    @Column(name = "camera_sau", length = 100)
    private String cameraSau;

    @Column(name = "camera_truoc", length = 100)
    private String cameraTruoc;

    @Column(name = "dung_luong_pin_dt", length = 50)
    private String dungLuongPinDt;

    @Column(name = "bo_nho_trong_dt", length = 50)
    private String boNhoTrongDt;

    @Column(name = "chip_xu_ly_dt", length = 100)
    private String chipXuLyDt;

    @Column(name = "so_sim", length = 30)
    private String soSim;
}
