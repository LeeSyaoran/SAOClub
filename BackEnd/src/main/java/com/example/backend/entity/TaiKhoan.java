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
@Table(name = "tai_khoan")
public class TaiKhoan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tai_khoan_id")
    private Integer taiKhoanId;

    @Column(name = "username", length = 50, unique = true, nullable = false)
    private String username;

    @Column(name = "mat_khau_hash", length = 255, nullable = false)
    private String matKhauHash;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "chuc_vu_id", nullable = false)
    private ChucVu chucVu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nhan_vien_id")
    private NhanVien nhanVien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khach_hang_id")
    private KhachHang khachHang;

    @Column(name = "trang_thai", length = 20)
    private String trangThai;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;
}
