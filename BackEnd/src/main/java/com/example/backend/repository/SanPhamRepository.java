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

import java.util.List;

@Repository
public interface SanPhamRepository extends JpaRepository<SanPham, Integer> {

    // Trả về 1 dòng/biến thể, gộp đủ thông tin sản phẩm + biến thể vào SanPhamResponse.
    // Dùng LEFT JOIN toàn bộ để sản phẩm thiếu danh mục / thương hiệu vẫn hiển thị.
    // Kết quả mới nhất lên đầu (ORDER BY ngayTao DESC).
    // Phân trang qua Pageable + lọc động (keyword/danhMucId/thuongHieuId/trangThai) —
    // truyền null để bỏ qua điều kiện đó (":x IS NULL OR ..." pattern).
    // countQuery riêng vì Spring Data không tự suy count đúng cho JPQL DTO projection nhiều JOIN.
    @Query(value = """
    SELECT new com.example.backend.response.SanPhamResponse(
        sp.sanPhamId,
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
        bt.phanLoaiTags,
        bt.phanLoaiTen
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
    WHERE (:keyword IS NULL OR LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :keyword, '%')))
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
    WHERE (:keyword IS NULL OR LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :keyword, '%')))
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

    // Xếp hạng sản phẩm bán chạy/bán chậm cho Dashboard — SUM ở SQL thay vì kéo hết
    // chi_tiet_don_hang (hàng nghìn dòng) về JS để cộng dồn. LEFT JOIN để sản phẩm
    // chưa từng bán vẫn xuất hiện với soLuongDaBan = 0 (cần cho biểu đồ "bán chậm").
    @Query("""
    SELECT new com.example.backend.response.ProductSalesResponse(sp.tenSanPham, COALESCE(SUM(ct.soLuong), 0))
    FROM SanPham sp
    LEFT JOIN BienTheSanPham bt ON bt.sanPham = sp
    LEFT JOIN ChiTietDonHang ct ON ct.bienThe = bt
    GROUP BY sp.sanPhamId, sp.tenSanPham
    ORDER BY COALESCE(SUM(ct.soLuong), 0) DESC
    """)
    List<ProductSalesResponse> topSelling(Pageable pageable);

    @Query("""
    SELECT new com.example.backend.response.ProductSalesResponse(sp.tenSanPham, COALESCE(SUM(ct.soLuong), 0))
    FROM SanPham sp
    LEFT JOIN BienTheSanPham bt ON bt.sanPham = sp
    LEFT JOIN ChiTietDonHang ct ON ct.bienThe = bt
    GROUP BY sp.sanPhamId, sp.tenSanPham
    ORDER BY COALESCE(SUM(ct.soLuong), 0) ASC
    """)
    List<ProductSalesResponse> slowSelling(Pageable pageable);
}
