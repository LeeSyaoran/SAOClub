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
@Table(name = "cau_hinh_vong_quay")
public class CauHinhVongQuay {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "diem_moi_luot", nullable = false)
    private Integer diemMoiLuot;

    @Column(name = "ty_le_truot", nullable = false)
    private Integer tyLeTruot;

    @Column(name = "ngay_cap_nhat", nullable = false)
    private LocalDateTime ngayCapNhat;
}
