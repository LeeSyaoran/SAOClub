package com.example.backend.repository;

import com.example.backend.entity.LichSuDonHang;
import com.example.backend.response.LichSuDonHangResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LichSuDonHangRepository extends JpaRepository<LichSuDonHang, Integer> {

    @Query("SELECT new com.example.backend.response.LichSuDonHangResponse(l.lichSuId, l.donHangId, l.trangThaiCu, l.trangThaiMoi, l.thoiGian) " +
           "FROM LichSuDonHang l WHERE l.donHangId = :donHangId ORDER BY l.thoiGian ASC")
    List<LichSuDonHangResponse> getByDonHangId(@Param("donHangId") Integer donHangId);

    List<LichSuDonHang> findByDonHangId(Integer donHangId);
}
