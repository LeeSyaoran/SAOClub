package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "phieu_giam_gia_ca_nhan")
public class PhieuGiamGiaCaNhan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "phieu_id")
    private Integer phieuId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khach_hang_id", nullable = false)
    private KhachHang khachHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doi_thuong_id")
    private DmDoiThuong doiThuong;

    // Sinh tự động bởi DB (UPPER(LEFT(NEWID()...))) → chỉ đọc, không ghi qua JPA
    @Column(name = "ma_phieu", length = 50, insertable = false, updatable = false)
    private String maPhieu;

    @Column(name = "loai", length = 20)
    private String loai;

    @Column(name = "gia_tri", precision = 18, scale = 0)
    private BigDecimal giaTri;

    @Column(name = "gia_tri_toi_da", precision = 18, scale = 0)
    private BigDecimal giaTriToiDa;

    @Column(name = "da_su_dung", nullable = false)
    private Boolean daSuDung;

    @Column(name = "ngay_doi", nullable = false)
    private LocalDateTime ngayDoi;

    @Column(name = "ngay_het_han", nullable = false)
    private LocalDateTime ngayHetHan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "don_hang_id")
    private DonHang donHang;

    // NULL = không yêu cầu đơn tối thiểu. Dùng cho voucher admin tặng trực tiếp lẫn voucher
    // trúng vòng quay (nếu sau này copy nguyên giá trị từ khuyến mãi gốc).
    @Column(name = "don_hang_toi_thieu", precision = 18, scale = 0)
    private BigDecimal donHangToiThieu;
}
