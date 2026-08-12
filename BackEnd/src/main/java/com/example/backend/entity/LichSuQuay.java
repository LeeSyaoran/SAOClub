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
@Table(name = "lich_su_quay")
public class LichSuQuay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khach_hang_id", nullable = false)
    private KhachHang khachHang;

    @Column(name = "ngay_quay", nullable = false)
    private LocalDateTime ngayQuay;

    @Column(name = "ket_qua", length = 10, nullable = false)
    private String ketQua; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khuyen_mai_id")
    private KhuyenMai khuyenMai;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phieu_giam_gia_ca_nhan_id")
    private PhieuGiamGiaCaNhan phieuGiamGiaCaNhan;

    @Column(name = "diem_da_tru", nullable = false)
    private Integer diemDaTru;
}
