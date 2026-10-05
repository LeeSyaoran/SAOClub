package com.example.backend.repository;

import com.example.backend.entity.KhuyenMaiKhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KhuyenMaiKhachHangRepository extends JpaRepository<KhuyenMaiKhachHang, Integer> {

    List<KhuyenMaiKhachHang> findByKhuyenMai_KhuyenMaiId(Integer khuyenMaiId);

    @Modifying
    @Query("DELETE FROM KhuyenMaiKhachHang kmkh WHERE kmkh.khuyenMai.khuyenMaiId = :khuyenMaiId")
    void deleteByKhuyenMaiId(@Param("khuyenMaiId") Integer khuyenMaiId);

    @Query("SELECT CASE WHEN COUNT(kmkh) > 0 THEN true ELSE false END FROM KhuyenMaiKhachHang kmkh WHERE kmkh.khuyenMai.khuyenMaiId = :khuyenMaiId AND kmkh.khachHang.khachHangId = :khachHangId")
    boolean existsByKhuyenMaiIdAndKhachHangId(@Param("khuyenMaiId") Integer khuyenMaiId, @Param("khachHangId") Integer khachHangId);

    boolean existsByKhuyenMai_KhuyenMaiId(Integer khuyenMaiId);
}
