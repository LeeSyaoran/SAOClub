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
@Table(name = "thuong_hieu")
public class ThuongHieu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "thuong_hieu_id")
    private Integer thuongHieuId;

    @Column(name = "ten_thuong_hieu", length = 100)
    private String tenThuongHieu;

    @Column(name = "quoc_gia", length = 100)
    private String quocGia;

    @Column(name = "mo_ta", length = 500)
    private String moTa;

    @Column(name = "trang_thai", length = 20)
    private String trangThai;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao;
}
