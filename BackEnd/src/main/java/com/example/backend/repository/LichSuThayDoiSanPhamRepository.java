package com.example.backend.repository;

import com.example.backend.entity.LichSuThayDoiSanPham;
import com.example.backend.response.LichSuThayDoiSanPhamResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LichSuThayDoiSanPhamRepository extends JpaRepository<LichSuThayDoiSanPham, Integer> {
    @Query("SELECT new com.example.backend.response.LichSuThayDoiSanPhamResponse(" +
           "l.lichSuId, l.doiTuong, bt.bienTheId, bt.maSku, l.tenTruong, l.giaTriCu, l.giaTriMoi, nv.hoTen, l.thoiGian) " +
           "FROM LichSuThayDoiSanPham l " +
           "LEFT JOIN l.bienThe bt " +
           "LEFT JOIN l.nhanVien nv " +
           "WHERE l.sanPham.sanPhamId = :sanPhamId " +
           "ORDER BY l.thoiGian DESC")
    List<LichSuThayDoiSanPhamResponse> hienThiLichSu(@Param("sanPhamId") Integer sanPhamId);
}
