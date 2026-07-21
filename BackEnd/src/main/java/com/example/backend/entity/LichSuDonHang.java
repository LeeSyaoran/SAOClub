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
@Table(name = "lich_su_don_hang")
public class LichSuDonHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lich_su_id")
    private Integer lichSuId;

    // Plain FK column, không dùng @ManyToOne — bảng này chỉ được TRIGGER trong DB ghi (xem
    // trg_don_hang_log_trangthai trong QLBanMayTinh.sql). Java chỉ đọc, trừ lúc gộp đơn cần
    // đổi lại donHangId (xem DonHangService.mergeOrders() ở Task 3) — không cần điều hướng
    // quan hệ JPA cho việc đó, set thẳng Integer là đủ.
    @Column(name = "don_hang_id", nullable = false)
    private Integer donHangId;

    @Column(name = "trang_thai_cu", length = 30)
    private String trangThaiCu;

    @Column(name = "trang_thai_moi", length = 30, nullable = false)
    private String trangThaiMoi;

    @Column(name = "thoi_gian", nullable = false)
    private LocalDateTime thoiGian;
}
