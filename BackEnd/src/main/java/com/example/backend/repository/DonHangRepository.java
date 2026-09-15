package com.example.backend.repository;

import com.example.backend.entity.DonHang;
import com.example.backend.response.DonHangResponse;
import com.example.backend.response.RevenueByDayResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DonHangRepository extends JpaRepository<DonHang, Integer> {

    @Query(value = """
    SELECT new com.example.backend.response.DonHangResponse(
        d.id, d.maDonHang,
        kh.khachHangId,
        nv.nhanVienId,
        km.khuyenMaiId,
        dcgh.id, dcgh.diaChi,
        d.nguoiNhan, d.sdtNguoiNhan,
        d.tongTien, d.giamGia, d.phiVanChuyen, d.thanhTien,
        d.ngayDat, d.ngayGiaoDuKien, d.ngayGiaoThucTe,
        d.trangThaiDonHang, d.trangThaiThanhToan, d.kenhBan, d.ghiChu, d.maVanDon,
        kh.hoTen, kh.soDienThoai, kh.diaChi
    )
    FROM DonHang d
    JOIN d.khachHang kh
    LEFT JOIN d.nhanVien nv
    LEFT JOIN d.khuyenMai km
    LEFT JOIN d.diaChiGiaoHang dcgh
    WHERE (:khachHangId IS NULL OR kh.khachHangId = :khachHangId)
    ORDER BY d.ngayDat DESC
    """,
    countQuery = """
    SELECT COUNT(d) FROM DonHang d JOIN d.khachHang kh
    WHERE (:khachHangId IS NULL OR kh.khachHangId = :khachHangId)
    """)
    Page<DonHangResponse> hienThiDonHang(@Param("khachHangId") Integer khachHangId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(d.thanhTien), 0) FROM DonHang d WHERE d.trangThaiDonHang <> 'cancelled'")
    BigDecimal sumDoanhThu();

    @Query("""
    SELECT new com.example.backend.response.RevenueByDayResponse(CAST(d.ngayDat AS java.time.LocalDate), SUM(d.thanhTien))
    FROM DonHang d
    WHERE d.ngayDat >= :tuNgay AND d.ngayDat <= :denNgay AND d.trangThaiDonHang <> 'cancelled'
    GROUP BY CAST(d.ngayDat AS java.time.LocalDate)
    ORDER BY CAST(d.ngayDat AS java.time.LocalDate)
    """)
    List<RevenueByDayResponse> doanhThuTheoNgay(@Param("tuNgay") LocalDateTime tuNgay, @Param("denNgay") LocalDateTime denNgay);

    // Tim don pending qua han 30 phut (dung cho auto-cancel)
    @Query("SELECT d FROM DonHang d WHERE d.trangThaiDonHang = 'pending' AND d.ngayDat < :cutoff")
    List<DonHang> findPendingOrdersOlderThan(@Param("cutoff") LocalDateTime cutoff);

    // POS: don hang gan nhat cua 1 khach hang (cho quick-select)
    @Query("""
    SELECT new com.example.backend.response.DonHangResponse(
        d.id, d.maDonHang,
        kh.khachHangId,
        nv.nhanVienId,
        km.khuyenMaiId,
        dcgh.id, dcgh.diaChi,
        d.nguoiNhan, d.sdtNguoiNhan,
        d.tongTien, d.giamGia, d.phiVanChuyen, d.thanhTien,
        d.ngayDat, d.ngayGiaoDuKien, d.ngayGiaoThucTe,
        d.trangThaiDonHang, d.trangThaiThanhToan, d.kenhBan, d.ghiChu, d.maVanDon,
        kh.hoTen, kh.soDienThoai, kh.diaChi
    )
    FROM DonHang d
    JOIN d.khachHang kh
    LEFT JOIN d.nhanVien nv
    LEFT JOIN d.khuyenMai km
    LEFT JOIN d.diaChiGiaoHang dcgh
    WHERE kh.khachHangId = :khachHangId AND d.trangThaiDonHang <> 'cancelled'
    ORDER BY d.ngayDat DESC
    """)
    java.util.List<DonHangResponse> findRecentByKhachHang(@Param("khachHangId") Integer khachHangId);

    // POS: tong hop don gan nhat trong 30 ngay (cho POS recent orders)
    @Query(value = """
    SELECT new com.example.backend.response.DonHangResponse(
        d.id, d.maDonHang,
        kh.khachHangId,
        nv.nhanVienId,
        km.khuyenMaiId,
        dcgh.id, dcgh.diaChi,
        d.nguoiNhan, d.sdtNguoiNhan,
        d.tongTien, d.giamGia, d.phiVanChuyen, d.thanhTien,
        d.ngayDat, d.ngayGiaoDuKien, d.ngayGiaoThucTe,
        d.trangThaiDonHang, d.trangThaiThanhToan, d.kenhBan, d.ghiChu, d.maVanDon,
        kh.hoTen, kh.soDienThoai, kh.diaChi
    )
    FROM DonHang d
    JOIN d.khachHang kh
    LEFT JOIN d.nhanVien nv
    LEFT JOIN d.khuyenMai km
    LEFT JOIN d.diaChiGiaoHang dcgh
    WHERE d.ngayDat >= :since AND d.trangThaiDonHang <> 'cancelled'
    ORDER BY d.ngayDat DESC
    """)
    java.util.List<DonHangResponse> findRecentForPos(@Param("since") LocalDateTime since);
}
