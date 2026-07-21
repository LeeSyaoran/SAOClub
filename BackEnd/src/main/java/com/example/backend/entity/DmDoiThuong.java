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
@Table(name = "dm_doi_thuong")
public class DmDoiThuong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doi_thuong_id")
    private Integer doiThuongId;

    @Column(name = "ten", length = 150)
    private String ten;

    @Column(name = "mo_ta", length = 500)
    private String moTa;

    @Column(name = "diem_can", nullable = false)
    private Integer diemCan;

    @Column(name = "loai", length = 20)
    private String loai;

    @Column(name = "gia_tri", precision = 18, scale = 0)
    private BigDecimal giaTri;

    @Column(name = "gia_tri_toi_da", precision = 18, scale = 0)
    private BigDecimal giaTriToiDa;

    @Column(name = "trang_thai", length = 20)
    private String trangThai;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao;
}
