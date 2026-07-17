package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "cai_dat_he_thong")
public class CaiDatHeThong {

    @Id
    @Column(name = "cai_dat_id")
    private Integer caiDatId;

    @Column(name = "ten_cua_hang", length = 200)
    private String tenCuaHang;

    @Column(name = "dia_chi", length = 300)
    private String diaChi;

    @Column(name = "so_dien_thoai", length = 20)
    private String soDienThoai;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "ma_so_thue", length = 20)
    private String maSoThue;

    @Column(name = "logo_url", length = 300)
    private String logoUrl;

    @Column(name = "nguong_ton_kho_mac_dinh")
    private Integer nguongTonKhoMacDinh;

    @Column(name = "ngon_ngu_mac_dinh", length = 5)
    private String ngonNguMacDinh;

    @Column(name = "dinh_dang_so", length = 5)
    private String dinhDangSo;
}
