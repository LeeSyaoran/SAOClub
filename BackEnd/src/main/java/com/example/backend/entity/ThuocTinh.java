package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "thuoc_tinh")
public class ThuocTinh {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "thuoc_tinh_id")
    private Integer thuocTinhId;

    @Column(name = "ten_truong", length = 50, nullable = false, unique = true)
    private String tenTruong;

    @Column(name = "ten_hien_thi", length = 100, nullable = false)
    private String tenHienThi;

    @Column(name = "loai_du_lieu", length = 20, nullable = false)
    private String loaiDuLieu = "text"; // text | select

    @Column(name = "bat_buoc", nullable = false)
    private Boolean batBuoc = false;

    @Column(name = "thu_tu_hien_thi", nullable = false)
    private Integer thuTuHienThi = 0;

    @Column(name = "trang_thai", length = 20, nullable = false)
    private String trangThai = "active";

    @Column(name = "pham_vi", length = 20, nullable = false)
    private String phamVi = "san_pham"; // "san_pham" | "bien_the"

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    @OneToMany(mappedBy = "thuocTinh", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("thuTu ASC")
    private List<ThuocTinhGiaTri> giaTriList = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (ngayTao == null) {
            ngayTao = LocalDateTime.now();
        }
    }
}
