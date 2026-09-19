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
@Table(name = "cuoc_tro_chuyen")
public class CuocTroChuyen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "loai_khach", length = 20, nullable = false)
    private String loaiKhach;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khach_hang_id")
    private KhachHang khachHang;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "ho_ten_khach", length = 150)
    private String hoTenKhach;

    @Column(name = "trang_thai", length = 30)
    private String trangThai = "HOI_DAP_AI";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nhan_vien_phu_trach")
    private NhanVien nhanVienPhuTrach;

    @Column(name = "so_lan_escalate")
    private Integer soLanEscalate = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.soLanEscalate == null) this.soLanEscalate = 0;
        if (this.trangThai == null) this.trangThai = "HOI_DAP_AI";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public static final String TRANG_THAI_HOI_DAP_AI = "HOI_DAP_AI";
    public static final String TRANG_THAI_CHAT_NV = "CHAT_NHAN_VIEN";
    public static final String TRANG_THAI_DA_DONG = "DA_DONG";

    public static final String LOAI_HE_THONG = "HE_THONG";
    public static final String LOAI_ANONYMOUS = "ANONYMOUS";
}
