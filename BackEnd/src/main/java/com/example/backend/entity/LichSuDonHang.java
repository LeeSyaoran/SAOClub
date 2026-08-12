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
@Table(name = "lich_su_don_hang")
public class LichSuDonHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lich_su_id")
    private Integer lichSuId;

    @Column(name = "don_hang_id", nullable = false)
    private Integer donHangId;

    @Column(name = "trang_thai_cu", length = 30)
    private String trangThaiCu;

    @Column(name = "trang_thai_moi", length = 30, nullable = false)
    private String trangThaiMoi;

    @Column(name = "thoi_gian", nullable = false)
    private LocalDateTime thoiGian;
}
