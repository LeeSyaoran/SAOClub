package com.example.backend.repository;

import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.response.ChiTietSanPhamResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietSanPhamRepository extends JpaRepository<ChiTietSanPham, Integer> {
    @Query("SELECT new com.example.backend.response.ChiTietSanPhamResponse(c.chiTietId, c.bienThe.bienTheId, c.bienThe.maSku, c.soSerial, c.trangThai, c.ngayNhapKho) FROM ChiTietSanPham c")
    List<ChiTietSanPhamResponse> hienThiChiTietSanPham();

    @Query("SELECT new com.example.backend.response.ChiTietSanPhamResponse(c.chiTietId, c.bienThe.bienTheId, c.bienThe.maSku, c.soSerial, c.trangThai, c.ngayNhapKho) FROM ChiTietSanPham c WHERE c.bienThe.bienTheId = :bienTheId")
    List<ChiTietSanPhamResponse> findByBienTheId(@Param("bienTheId") Integer bienTheId);
}
