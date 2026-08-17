package com.example.backend.repository;

import com.example.backend.entity.SanPhamHinhAnh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SanPhamHinhAnhRepository extends JpaRepository<SanPhamHinhAnh, Integer> {

    @Query("SELECT h.duongDan FROM SanPhamHinhAnh h WHERE h.sanPham.sanPhamId = :sanPhamId ORDER BY h.thuTu ASC")
    List<String> layDuongDanTheoSanPham(@Param("sanPhamId") Integer sanPhamId);

    @Modifying
    @Query("DELETE FROM SanPhamHinhAnh h WHERE h.sanPham.sanPhamId = :sanPhamId")
    void deleteBySanPhamId(@Param("sanPhamId") Integer sanPhamId);
}
