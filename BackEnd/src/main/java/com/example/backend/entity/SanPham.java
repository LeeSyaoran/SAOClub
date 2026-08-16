package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "san_pham")
public class SanPham extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "san_pham_id")
    private Integer sanPhamId;

    @Column(name = "ma_san_pham", length = 50, unique = true)
    private String maSanPham; // Thêm cột Mã sản phẩm

    @Column(name = "barcode", length = 100, unique = true)
    private String barcode; // Thêm cột Barcode

    @Column(name = "ten_san_pham", length = 200)
    private String tenSanPham;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thuong_hieu_id", nullable = false)
    private ThuongHieu thuongHieu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "danh_muc_id", nullable = false)
    private DanhMuc danhMuc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nha_cung_cap_id")
    private NhaCungCap nhaCungCap;

    @Column(name = "loai_san_pham", length = 30)
    private String loaiSanPham;

    @Column(name = "mo_ta", columnDefinition = "nvarchar(max)")
    private String moTa;

    @Column(name = "hinh_anh_chinh", length = 500)
    private String hinhAnhChinh;

    @Column(name = "trang_thai", length = 20)
    private String trangThai;

}