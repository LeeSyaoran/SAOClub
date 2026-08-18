package com.example.backend.repository;

import com.example.backend.entity.SanPham;
import com.example.backend.response.ProductSalesResponse;
import com.example.backend.response.SanPhamResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SanPhamRepository extends JpaRepository<SanPham, Integer> {

    // Kiểm tra trùng mã TRƯỚC khi insert để báo lỗi tiếng Việt rõ ràng
    boolean existsByMaSanPham(String maSanPham);

    boolean existsByMaSanPhamAndSanPhamIdNot(String maSanPham, Integer sanPhamId);

    @Query(value = """
    SELECT new com.example.backend.response.SanPhamResponse(
        sp.sanPhamId,
        sp.maSanPham,
        bt.bienTheId,
        sp.tenSanPham,
        dm.id,
        dm.tenDanhMuc,
        th.thuongHieuId,
        th.tenThuongHieu,
        ncc.tenNhaCungCap,
        ncc.nhaCungCapId,
        sp.loaiSanPham,
        bt.maSku,
        bt.barcode,
        cpu.tenCpu,
        ram.dungLuong,
        oCung.loaiOcung,
        gpu.tenGpu,
        bt.kichThuocManHinh,
        bt.heDieuHanh,
        bt.pin,
        bt.trongLuongKg,
        bt.mauSac,
        bt.giaBan,
        bt.giaNhap,
        bt.baoHanhThang,
        sp.moTa,
        sp.hinhAnhChinh,
        bt.trangThai,
        sp.ngayTao,
        sp.ngayCapNhat,
        bt.phanLoaiTags,
        bt.phanLoaiTen,
        (SELECT COUNT(c) FROM ChiTietSanPham c WHERE c.bienThe = bt AND c.trangThai = 'trong_kho')
    )
    FROM BienTheSanPham bt
    JOIN bt.sanPham sp
    LEFT JOIN sp.danhMuc dm
    LEFT JOIN sp.thuongHieu th
    LEFT JOIN sp.nhaCungCap ncc
    LEFT JOIN bt.cpu cpu
    LEFT JOIN bt.ram ram
    LEFT JOIN bt.oCung oCung
    LEFT JOIN bt.gpu gpu
    WHERE (:keyword IS NULL OR LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :keyword, '%')) 
           OR LOWER(sp.maSanPham) LIKE LOWER(CONCAT('%', :keyword, '%')) 
           OR LOWER(bt.maSku) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(bt.barcode) LIKE LOWER(CONCAT('%', :keyword, '%')))
      AND (:danhMucId IS NULL OR dm.id = :danhMucId)
      AND (:thuongHieuId IS NULL OR th.thuongHieuId = :thuongHieuId)
      AND (:trangThai IS NULL OR bt.trangThai = :trangThai)
    ORDER BY sp.ngayTao DESC
    """,
            countQuery = """
    SELECT COUNT(bt)
    FROM BienTheSanPham bt
    JOIN bt.sanPham sp
    LEFT JOIN sp.danhMuc dm
    LEFT JOIN sp.thuongHieu th
    WHERE (:keyword IS NULL OR LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(sp.maSanPham) LIKE LOWER(CONCAT('%', :keyword, '%')) 
           OR LOWER(bt.maSku) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(bt.barcode) LIKE LOWER(CONCAT('%', :keyword, '%')))
      AND (:danhMucId IS NULL OR dm.id = :danhMucId)
      AND (:thuongHieuId IS NULL OR th.thuongHieuId = :thuongHieuId)
      AND (:trangThai IS NULL OR bt.trangThai = :trangThai)
    """)
    Page<SanPhamResponse> hienThiSanPham(
            @Param("keyword") String keyword,
            @Param("danhMucId") Integer danhMucId,
            @Param("thuongHieuId") Integer thuongHieuId,
            @Param("trangThai") String trangThai,
            Pageable pageable);

    @Query("""
    SELECT new com.example.backend.response.ProductSalesResponse(sp.tenSanPham, COALESCE(SUM(ct.soLuong), 0))
    FROM SanPham sp
    LEFT JOIN BienTheSanPham bt ON bt.sanPham = sp
    LEFT JOIN ChiTietDonHang ct ON ct.bienThe = bt
    LEFT JOIN ct.donHang d
    WHERE (:tuNgay IS NULL OR d.ngayDat >= :tuNgay) AND (:denNgay IS NULL OR d.ngayDat <= :denNgay)
      AND (d.trangThaiDonHang IS NULL OR d.trangThaiDonHang <> 'cancelled')
    GROUP BY sp.sanPhamId, sp.tenSanPham
    ORDER BY COALESCE(SUM(ct.soLuong), 0) DESC
    """)
    List<ProductSalesResponse> topSelling(@Param("tuNgay") LocalDateTime tuNgay, @Param("denNgay") LocalDateTime denNgay, Pageable pageable);

    @Query("""
    SELECT new com.example.backend.response.ProductSalesResponse(sp.tenSanPham, COALESCE(SUM(ct.soLuong), 0))
    FROM SanPham sp
    LEFT JOIN BienTheSanPham bt ON bt.sanPham = sp
    LEFT JOIN ChiTietDonHang ct ON ct.bienThe = bt
    LEFT JOIN ct.donHang d
    WHERE (:tuNgay IS NULL OR d.ngayDat >= :tuNgay) AND (:denNgay IS NULL OR d.ngayDat <= :denNgay)
      AND (d.trangThaiDonHang IS NULL OR d.trangThaiDonHang <> 'cancelled')
    GROUP BY sp.sanPhamId, sp.tenSanPham
    ORDER BY COALESCE(SUM(ct.soLuong), 0) ASC
    """)
    List<ProductSalesResponse> slowSelling(@Param("tuNgay") LocalDateTime tuNgay, @Param("denNgay") LocalDateTime denNgay, Pageable pageable);
}