package com.example.backend.repository;

import com.example.backend.entity.DonHang;
import com.example.backend.response.DonHangResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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
        d.trangThaiDonHang, d.trangThaiThanhToan, d.kenhBan, d.ghiChu
    )
    FROM DonHang d
    JOIN d.khachHang kh
    LEFT JOIN d.nhanVien nv
    LEFT JOIN d.khuyenMai km
    LEFT JOIN d.diaChiGiaoHang dcgh
    ORDER BY d.ngayDat DESC
    """)
    List<DonHangResponse> hienThiDonHang();
}
