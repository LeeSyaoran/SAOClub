package com.example.backend.repository;

import com.example.backend.entity.SanPhamYeuThich;
import com.example.backend.response.SanPhamYeuThichResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SanPhamYeuThichRepository extends JpaRepository<SanPhamYeuThich, Integer> {

    @Query("""
    SELECT new com.example.backend.response.SanPhamYeuThichResponse(
        y.yeuThichId, bt.bienTheId, sp.sanPhamId, sp.tenSanPham, th.tenThuongHieu,
        bt.maSku, bt.giaBan, sp.hinhAnhChinh, bt.trangThai,
        (SELECT COUNT(c) FROM ChiTietSanPham c WHERE c.bienThe = bt AND c.trangThai = 'trong_kho'),
        y.ngayThem
    )
    FROM SanPhamYeuThich y
    JOIN y.bienThe bt
    JOIN bt.sanPham sp
    LEFT JOIN sp.thuongHieu th
    WHERE y.khachHang.khachHangId = :khachHangId
    ORDER BY y.ngayThem DESC
    """)
    List<SanPhamYeuThichResponse> hienThiTheoKhachHang(@Param("khachHangId") Integer khachHangId);

    Optional<SanPhamYeuThich> findByKhachHang_KhachHangIdAndBienThe_BienTheId(Integer khachHangId, Integer bienTheId);

    long countByKhachHang_KhachHangId(Integer khachHangId);
}
