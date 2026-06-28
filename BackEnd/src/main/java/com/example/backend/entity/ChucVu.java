package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "chuc_vu")
public class ChucVu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chuc_vu_id")
    private Integer id;

    @Column(name = "ma_chuc_vu", length = 30, unique = true, nullable = false)
    private String maChucVu;

    @Column(name = "ten_chuc_vu", length = 100)
    private String tenChucVu;

    @Column(name = "cap_do")
    private Integer capDo;

    @Column(name = "mo_ta", length = 255)
    private String moTa;
}
