package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "thuoc_tinh_gia_tri")
public class ThuocTinhGiaTri {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gia_tri_id")
    private Integer giaTriId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thuoc_tinh_id", nullable = false)
    private ThuocTinh thuocTinh;

    @Column(name = "gia_tri", length = 100, nullable = false)
    private String giaTri;

    @Column(name = "thu_tu", nullable = false)
    private Integer thuTu = 0;
}
