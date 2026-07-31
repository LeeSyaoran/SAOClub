package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Formula;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "don_hang")
public class DonHang extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "don_hang_id")
    private Integer id;

    // Sinh tự động bởi trigger DB (format: DH-YYYYMMDD-XXXX) → chỉ đọc, không ghi qua JPA
    @Column(name = "ma_don_hang", length = 30, insertable = false, updatable = false)
    private String maDonHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khach_hang_id")
    private KhachHang khachHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nhan_vien_id")
    private NhanVien nhanVien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khuyen_mai_id")
    private KhuyenMai khuyenMai;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dia_chi_giao_hang_id")
    private DiaChiGiaoHang diaChiGiaoHang;

    @Column(name = "dia_chi_giao_hang_text", length = 255)
    private String diaChiGiaoHangText;

    @Column(name = "nguoi_nhan", length = 150)
    private String nguoiNhan;

    @Column(name = "sdt_nguoi_nhan", length = 20)
    private String sdtNguoiNhan;

    @Column(name = "tong_tien", precision = 18, scale = 0)
    private BigDecimal tongTien;

    @Column(name = "giam_gia", precision = 18, scale = 0)
    private BigDecimal giamGia;

    @Column(name = "phi_van_chuyen", precision = 18, scale = 0)
    private BigDecimal phiVanChuyen;

    // Computed column trong DB: tong_tien - giam_gia + phi_van_chuyen → chỉ đọc
    @Formula("(tong_tien - giam_gia + phi_van_chuyen)")
    private BigDecimal thanhTien;

    @Column(name = "ngay_dat")
    private LocalDateTime ngayDat;

    @Column(name = "ngay_giao_du_kien")
    private LocalDateTime ngayGiaoDuKien;

    @Column(name = "ngay_giao_thuc_te")
    private LocalDateTime ngayGiaoThucTe;

    // Giá trị: "pending" | "confirmed" | "processing" | "shipping" | "out_for_delivery" |
    //          "delivered" | "cancelled" | "returned"
    @Column(name = "trang_thai_don_hang", length = 30)
    private String trangThaiDonHang;

    // Giá trị: "unpaid" | "paid" | "partial" | "refunded"
    @Column(name = "trang_thai_thanh_toan", length = 30)
    private String trangThaiThanhToan;

    // Giá trị: "online" | "offline" (POS tại quầy)
    @Column(name = "kenh_ban", length = 50)
    private String kenhBan;

    @Column(name = "ghi_chu", length = 500)
    private String ghiChu;

    // Nhập tay bởi nhân viên/admin khi chuyển đơn sang "shipping" — text tự do, không phải
    // mã tra cứu thật của đơn vị vận chuyển.
    @Column(name = "ma_van_don", length = 50)
    private String maVanDon;
}
