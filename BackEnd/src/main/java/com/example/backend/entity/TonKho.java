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
@Table(name = "ton_kho")
public class TonKho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cau_hinh_id")
    private Integer tonKhoId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bien_the_id")
    private BienTheSanPham bienThe;

    @Column(name = "so_luong_ton_thuc_te")
    private Integer soLuongTon;

    @Column(name = "so_luong_giu")
    private Integer soLuongGiu;

    @Column(name = "ton_kho_toi_thieu")
    private Integer tonKhoToiThieu;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;
}
