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

    // === BUG CŨ ===
    // d.nhanVien.nhanVienId, d.khuyenMai.khuyenMaiId, d.diaChiGiaoHang.id
    // → JPQL path navigation trên nullable @ManyToOne tạo ra IMPLICIT INNER JOIN
    // → Đơn hàng không có nhân viên / khuyến mãi / địa chỉ bị BỎ SÓT khỏi kết quả
    //
    // === FIX ===
    // Khai báo LEFT JOIN rõ ràng với alias (nv, km, dcgh)
    // Dùng alias trong SELECT thay vì path navigation (nv.nhanVienId thay vì d.nhanVien.nhanVienId)
    // khachHang NOT NULL trong DB nên dùng JOIN thường (không cần LEFT)
    //
    // Phân trang qua Pageable — countQuery riêng vì JPQL DTO projection không tự suy count được.
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
        d.trangThaiDonHang, d.trangThaiThanhToan, d.kenhBan, d.ghiChu, d.maVanDon
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

    // Tổng doanh thu cho Dashboard KPI — SUM ở SQL thay vì kéo hết don_hang về JS cộng dồn.
    // Loại đơn "cancelled" — đơn đã hủy không phải doanh thu thật, trước đây tính gộp làm
    // sai lệch KPI.
    @Query("SELECT COALESCE(SUM(d.thanhTien), 0) FROM DonHang d WHERE d.trangThaiDonHang <> 'cancelled'")
    BigDecimal sumDoanhThu();

    // Doanh thu gộp theo ngày trong khoảng — dùng cho biểu đồ cột "Doanh thu theo thời
    // gian" ở tab Báo cáo. CAST sang LocalDate để gộp đúng theo ngày (ngayDat là
    // LocalDateTime, có giờ phút giây khác nhau).
    @Query("""
    SELECT new com.example.backend.response.RevenueByDayResponse(CAST(d.ngayDat AS java.time.LocalDate), SUM(d.thanhTien))
    FROM DonHang d
    WHERE d.ngayDat >= :tuNgay AND d.ngayDat <= :denNgay AND d.trangThaiDonHang <> 'cancelled'
    GROUP BY CAST(d.ngayDat AS java.time.LocalDate)
    ORDER BY CAST(d.ngayDat AS java.time.LocalDate)
    """)
    List<RevenueByDayResponse> doanhThuTheoNgay(@Param("tuNgay") LocalDateTime tuNgay, @Param("denNgay") LocalDateTime denNgay);
}
