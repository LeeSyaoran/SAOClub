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
@Table(name = "thanh_toan")
public class ThanhToan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "thanh_toan_id")
    private Integer thanhToanId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "don_hang_id", nullable = false)
    private DonHang donHang;

    @Column(name = "ngay_thanh_toan", nullable = false)
    private LocalDateTime ngayThanhToan;

    @Column(name = "phuong_thuc_thanh_toan", length = 50)
    private String phuongThucThanhToan;

    @Column(name = "so_tien", precision = 18, scale = 2)
    private BigDecimal soTien;

    @Column(name = "ma_giao_dich", length = 100)
    private String maGiaoDich;

    @Column(name = "trang_thai", length = 30)
    private String trangThai;

    @Column(name = "ghi_chu", length = 255)
    private String ghiChu;
}
