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

    @ManyToOne
    @JoinColumn(name = "don_hang_id", nullable = false)
    private DonHang donHang;

    @ManyToOne
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

    @Column(name = "ghi_chu", length = 500)
    private String ghiChu;
}
