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
@Table(name = "khach_hang")
public class KhachHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "khach_hang_id")
    private Integer khachHangId;

    @Column(name = "ho_ten", length = 150)
    private String hoTen;

    @Column(name = "so_dien_thoai", length = 20, unique = true)
    private String soDienThoai;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "username", length = 50, unique = true)
    private String username;

    @Column(name = "mat_khau", length = 255)
    private String matKhau;

    @Column(name = "dia_chi", length = 255)
    private String diaChi;

    @Column(name = "loai_khach", length = 20)
    private String loaiKhach;

    @Column(name = "ten_cong_ty", length = 200)
    private String tenCongTy;

    @Column(name = "ma_so_thue", length = 20)
    private String maSoThue;

    @Column(name = "diem_tich_luy", nullable = false)
    private Integer diemTichLuy;

    @Column(name = "trang_thai", length = 20)
    private String trangThai;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao;
}
