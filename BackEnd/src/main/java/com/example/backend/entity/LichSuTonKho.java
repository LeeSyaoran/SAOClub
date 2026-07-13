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
@Table(name = "lich_su_ton_kho")
public class LichSuTonKho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lich_su_id")
    private Integer lichSuId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bien_the_id", nullable = false)
    private BienTheSanPham bienThe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chi_tiet_id")
    private ChiTietSanPham chiTietSanPham;

    @Column(name = "loai_bien_dong", length = 30)
    private String loaiBienDong;

    @Column(name = "so_luong_thay_doi", nullable = false)
    private Integer soLuongThayDoi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "don_hang_id")
    private DonHang donHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phieu_nhap_id")
    private PhieuNhapKho phieuNhapKho;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nhan_vien_id")
    private NhanVien nhanVien;

    @Column(name = "ghi_chu", length = 255)
    private String ghiChu;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao;
}
