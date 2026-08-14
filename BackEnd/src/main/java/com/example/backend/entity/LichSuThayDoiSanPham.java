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
@Table(name = "lich_su_thay_doi_san_pham")
public class LichSuThayDoiSanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lich_su_id")
    private Integer lichSuId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "san_pham_id", nullable = false)
    private SanPham sanPham;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bien_the_id")
    private BienTheSanPham bienThe;

    @Column(name = "doi_tuong", length = 20, nullable = false)
    private String doiTuong;

    @Column(name = "ten_truong", length = 50, nullable = false)
    private String tenTruong;

    @Column(name = "gia_tri_cu", length = 500)
    private String giaTriCu;

    @Column(name = "gia_tri_moi", length = 500)
    private String giaTriMoi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nhan_vien_id")
    private NhanVien nhanVien;

    @Column(name = "thoi_gian", nullable = false)
    private LocalDateTime thoiGian;

    @PrePersist
    protected void onCreate() {
        this.thoiGian = LocalDateTime.now();
    }
}
