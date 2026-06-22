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
@Table(name = "don_hang")
public class DonHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "don_hang_id")
    private Integer id;

    @Column(name = "ma_don_hang", length = 30, insertable = false, updatable = false)
    private String maDonHang;

    @ManyToOne
    @JoinColumn(name = "khach_hang_id")
    private KhachHang khachHang;

    @ManyToOne
    @JoinColumn(name = "nhan_vien_id")
    private NhanVien nhanVien;

    @ManyToOne
    @JoinColumn(name = "khuyen_mai_id")
    private KhuyenMai khuyenMai;

    @ManyToOne
    @JoinColumn(name = "dia_chi_giao_hang_id")
    private DiaChiGiaoHang diaChiGiaoHang;

    @Column(name = "dia_chi_giao_hang_text", length = 255)
    private String diaChiGiaoHangText;

    @Column(name = "nguoi_nhan", length = 150)
    private String nguoiNhan;

    @Column(name = "sdt_nguoi_nhan", length = 20)
    private String sdtNguoiNhan;

    @Column(name = "tong_tien", precision = 18, scale = 2)
    private BigDecimal tongTien;

    @Column(name = "giam_gia", precision = 18, scale = 2)
    private BigDecimal giamGia;

    @Column(name = "phi_van_chuyen", precision = 18, scale = 2)
    private BigDecimal phiVanChuyen;

    @Column(name = "thanh_tien", precision = 18, scale = 2)
    private BigDecimal thanhTien;

    @Column(name = "ngay_dat")
    private LocalDateTime ngayDat;

    @Column(name = "ngay_giao_du_kien")
    private LocalDateTime ngayGiaoDuKien;

    @Column(name = "ngay_giao_thuc_te")
    private LocalDateTime ngayGiaoThucTe;

    @Column(name = "trang_thai_don_hang", length = 30)
    private String trangThaiDonHang;

    @Column(name = "trang_thai_thanh_toan", length = 30)
    private String trangThaiThanhToan;

    @Column(name = "kenh_ban", length = 50)
    private String kenhBan;

    @Column(name = "ghi_chu", length = 500)
    private String ghiChu;
}
