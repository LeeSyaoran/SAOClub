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
@Table(name = "ai_kien_thuc")
public class AiKienThuc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "loai", length = 30, nullable = false)
    private String loai;

    @Column(name = "tieu_de", length = 255, nullable = false)
    private String tieuDe;

    @Column(name = "noi_dung", columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String noiDung;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "san_pham_id")
    private SanPham sanPham;

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.active == null) this.active = true;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public static final String LOAI_SAN_PHAM = "SAN_PHAM";
    public static final String LOAI_FAQ = "FAQ";
    public static final String LOAI_CHINH_SACH = "CHINH_SACH";
    public static final String LOAI_KHAC = "KHAC";
}
