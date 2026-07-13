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
@Table(name = "chi_tiet_san_pham")
public class ChiTietSanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chi_tiet_id")
    private Integer chiTietId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bien_the_id", nullable = false)
    private BienTheSanPham bienThe;

    @Column(name = "so_serial", length = 100, unique = true)
    private String soSerial;

    @Column(name = "trang_thai", length = 30)
    private String trangThai;

    @Column(name = "ngay_nhap_kho")
    private LocalDateTime ngayNhapKho;
}
