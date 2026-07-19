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
@Table(name = "phieu_tra_hang")
public class PhieuTraHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "phieu_tra_id")
    private Integer phieuTraId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "don_hang_id", nullable = false)
    private DonHang donHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nhan_vien_id")
    private NhanVien nhanVien;

    @Column(name = "ly_do", length = 255)
    private String lyDo;

    @Column(name = "ngay_tra", nullable = false)
    private LocalDateTime ngayTra;

    @Column(name = "trang_thai", length = 20)
    private String trangThai;

    @Column(name = "so_tien_hoan", precision = 18, scale = 2)
    private BigDecimal soTienHoan;

    @Column(name = "hinh_thuc_hoan", length = 20)
    private String hinhThucHoan;

    @Column(name = "ghi_chu", length = 500)
    private String ghiChu;
}
