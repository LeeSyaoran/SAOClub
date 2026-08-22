package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Không dùng @Data ở entity có quan hệ LAZY:
 *  - toString() sinh ra sẽ gọi sanPham.toString(); nếu SanPham có List<BienTheSanPham>
 *    ngược lại thì hai bên gọi nhau vô hạn → StackOverflowError.
 *  - equals()/hashCode() đụng vào proxy LAZY ngoài session → LazyInitializationException,
 *    hoặc bắn thêm query thừa khi entity nằm trong Set/Map.
 *  - @Data trên class extends BaseEntity còn lặng lẽ bỏ qua toàn bộ field của lớp cha.
 * Thay bằng @Getter/@Setter, toString loại quan hệ, equals/hashCode chỉ theo khoá chính.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"sanPham", "cpu", "ram", "oCung", "gpu"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@Table(name = "bien_the_san_pham")
public class BienTheSanPham extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "bien_the_id")
    private Integer bienTheId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "san_pham_id", nullable = false)
    private SanPham sanPham;

    @Column(name = "ma_sku", length = 50, unique = true)
    private String maSku;

    // unique = true ở đây chỉ có tác dụng khi Hibernate tự sinh schema. Dự án dùng CSDL
    // có sẵn từ QLBanMayTinh.sql nên ràng buộc thật nằm ở unique index trong file SQL —
    // giữ lại cho đúng ý đồ thiết kế, nhưng đừng trông chờ nó tự tạo index.
    @Column(name = "barcode", length = 50, unique = true)
    private String barcode;

    @Column(name = "gia_nhap", precision = 18, scale = 0)
    private BigDecimal giaNhap;

    @Column(name = "gia_ban", precision = 18, scale = 0)
    private BigDecimal giaBan;

    @Column(name = "bao_hanh_thang")
    private Integer baoHanhThang;

    @Column(name = "hinh_anh_bien_the", length = 500)
    private String hinhAnhBienThe;

    @Column(name = "trang_thai", length = 20)
    private String trangThai;

    @Column(name = "mau_sac", length = 50)
    private String mauSac;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cpu_id")
    private DmCpu cpu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ram_id")
    private DmRam ram;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "o_cung_id")
    private DmOcung oCung;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gpu_id")
    private DmGpu gpu;

    @Column(name = "kich_thuoc_man_hinh", length = 50)
    private String kichThuocManHinh;

    @Column(name = "he_dieu_hanh", length = 100)
    private String heDieuHanh;

    @Column(name = "pin", length = 50)
    private String pin;

    @Column(name = "trong_luong_kg", precision = 5, scale = 2)
    private BigDecimal trongLuongKg;

    @Column(name = "phan_loai_tags", length = 200)
    private String phanLoaiTags;

    @Column(name = "phan_loai_ten", length = 200)
    private String phanLoaiTen;
}