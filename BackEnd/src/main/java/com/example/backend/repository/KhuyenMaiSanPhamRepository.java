package com.example.backend.repository;

import com.example.backend.entity.KhuyenMaiSanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KhuyenMaiSanPhamRepository extends JpaRepository<KhuyenMaiSanPham, Integer> {

    List<KhuyenMaiSanPham> findByKhuyenMai_KhuyenMaiId(Integer khuyenMaiId);

    @Modifying
    @Query("DELETE FROM KhuyenMaiSanPham kmsp WHERE kmsp.khuyenMai.khuyenMaiId = :khuyenMaiId")
    void deleteByKhuyenMaiId(@Param("khuyenMaiId") Integer khuyenMaiId);

    @Query("SELECT CASE WHEN COUNT(kmsp) > 0 THEN true ELSE false END FROM KhuyenMaiSanPham kmsp WHERE kmsp.khuyenMai.khuyenMaiId = :khuyenMaiId AND kmsp.sanPham.sanPhamId = :sanPhamId")
    boolean existsByKhuyenMaiIdAndSanPhamId(@Param("khuyenMaiId") Integer khuyenMaiId, @Param("sanPhamId") Integer sanPhamId);

    boolean existsByKhuyenMai_KhuyenMaiId(Integer khuyenMaiId);
}
