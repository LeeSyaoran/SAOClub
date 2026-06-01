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

    @ManyToOne
    @JoinColumn(name = "san_pham_id", nullable = false)
    private SanPham sanPham;

    @Column(name = "loai_bien_dong", length = 30)
    private String loaiBienDong;

    @Column(name = "so_luong_thay_doi", nullable = false)
    private Integer soLuongThayDoi;

    @Column(name = "so_luong_truoc", nullable = false)
    private Integer soLuongTruoc;

    @Column(name = "so_luong_sau", nullable = false)
    private Integer soLuongSau;

    @ManyToOne
    @JoinColumn(name = "don_hang_id")
    private DonHang donHang;

    @ManyToOne
    @JoinColumn(name = "phieu_nhap_id")
    private PhieuNhapKho phieuNhapKho;

    @ManyToOne
    @JoinColumn(name = "nhan_vien_id")
    private NhanVien nhanVien;

    @Column(name = "ghi_chu", length = 255)
    private String ghiChu;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao;
}
