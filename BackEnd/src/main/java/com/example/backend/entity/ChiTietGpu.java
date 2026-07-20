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
@Table(name = "chi_tiet_gpu")
public class ChiTietGpu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chi_tiet_gpu_id")
    private Integer chiTietGpuId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gpu_id", nullable = false)
    private DmGpu gpu;

    @Column(name = "so_serial", length = 100, unique = true)
    private String soSerial;

    @Column(name = "trang_thai", length = 30)
    private String trangThai;

    @Column(name = "ngay_nhap_kho")
    private LocalDateTime ngayNhapKho;

    @Column(name = "ghi_chu", length = 255)
    private String ghiChu;
}
