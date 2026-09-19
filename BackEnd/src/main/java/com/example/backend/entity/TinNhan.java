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
@Table(name = "tin_nhan")
public class TinNhan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuoc_tro_chuyen_id", nullable = false)
    private CuocTroChuyen cuocTroChuyen;

    @Column(name = "nguoi_gui", length = 20, nullable = false)
    private String nguoiGui;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nhan_vien_id")
    private NhanVien nhanVien;

    @Column(name = "noi_dung", columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String noiDung;

    @Column(name = "loai_nguoi_gui", length = 20)
    private String loaiNguoiGui = "ANONYMOUS";

    @Column(name = "da_doc")
    private Boolean daDoc = false;

    @Column(name = "la_cau_hoi_cua_ai")
    private Boolean laCauHoiCuaAi = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.daDoc == null) this.daDoc = false;
        if (this.laCauHoiCuaAi == null) this.laCauHoiCuaAi = false;
    }

    public static final String NGUOI_GUI_KHACH = "KHACH";
    public static final String NGUOI_GUI_NHAN_VIEN = "NHAN_VIEN";
    public static final String NGUOI_GUI_ADMIN = "ADMIN";
    public static final String NGUOI_GUI_AI = "AI";

    public static final String LOAI_ANONYMOUS = "ANONYMOUS";
    public static final String LOAI_KHACH_HANG = "KHACH_HANG";
    public static final String LOAI_NHAN_VIEN = "NHAN_VIEN";
    public static final String LOAI_ADMIN = "ADMIN";
    public static final String LOAI_AI = "AI";
}
