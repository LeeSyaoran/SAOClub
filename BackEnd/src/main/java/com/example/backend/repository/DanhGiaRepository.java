package com.example.backend.repository;

import com.example.backend.entity.DanhGia;
import com.example.backend.response.DanhGiaAdminResponse;
import com.example.backend.response.DanhGiaResponse;
import com.example.backend.response.DanhGiaSummaryResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DanhGiaRepository extends JpaRepository<DanhGia, Integer> {

    @Query("""
    SELECT new com.example.backend.response.DanhGiaResponse(
        d.danhGiaId, kh.khachHangId, kh.hoTen, d.soSao, d.noiDung, d.ngayDanhGia
    )
    FROM DanhGia d JOIN d.khachHang kh
    WHERE d.sanPham.sanPhamId = :sanPhamId
    ORDER BY d.ngayDanhGia DESC
    """)
    List<DanhGiaResponse> hienThiTheoSanPham(@Param("sanPhamId") Integer sanPhamId);

    @Query("""
    SELECT new com.example.backend.response.DanhGiaSummaryResponse(
        d.sanPham.sanPhamId, AVG(CAST(d.soSao AS double)), COUNT(d)
    )
    FROM DanhGia d
    GROUP BY d.sanPham.sanPhamId
    """)
    List<DanhGiaSummaryResponse> tongHopTatCa();

    Optional<DanhGia> findByKhachHang_KhachHangIdAndSanPham_SanPhamId(Integer khachHangId, Integer sanPhamId);

    @Query("""
    SELECT new com.example.backend.response.DanhGiaAdminResponse(
        d.danhGiaId, kh.khachHangId, kh.hoTen, sp.sanPhamId, sp.tenSanPham, d.soSao, d.noiDung, d.ngayDanhGia
    )
    FROM DanhGia d JOIN d.khachHang kh JOIN d.sanPham sp
    ORDER BY d.ngayDanhGia DESC
    """)
    List<DanhGiaAdminResponse> hienThiTatCa();

    @Query("""
    SELECT c.donHang.id
    FROM ChiTietDonHang c
    WHERE c.donHang.khachHang.khachHangId = :khachHangId
      AND c.bienThe.sanPham.sanPhamId = :sanPhamId
      AND c.donHang.trangThaiDonHang = 'delivered'
    ORDER BY c.donHang.ngayDat DESC
    """)
    List<Integer> timDonHangDaGiao(@Param("khachHangId") Integer khachHangId, @Param("sanPhamId") Integer sanPhamId);
}
