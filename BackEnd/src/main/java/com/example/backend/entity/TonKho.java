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
    @Column(name = "ton_kho_id")
    private Integer tonKhoId;

    @OneToOne
    @JoinColumn(name = "san_pham_id")
    private SanPham sanPham;

    @Column(name = "so_luong_ton")
    private Integer soLuongTon;

    @Column(name = "so_luong_giu")
    private Integer soLuongGiu;

    @Column(name = "ton_kho_toi_thieu")
    private Integer tonKhoToiThieu;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;
}
