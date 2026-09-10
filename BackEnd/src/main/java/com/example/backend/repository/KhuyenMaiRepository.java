package com.example.backend.repository;

import com.example.backend.entity.KhuyenMai;
import com.example.backend.response.KhuyenMaiResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KhuyenMaiRepository extends JpaRepository<KhuyenMai, Integer> {
    Optional<KhuyenMai> findByMaKhuyenMaiIgnoreCase(String maKhuyenMai);
    @Query("SELECT new com.example.backend.response.KhuyenMaiResponse(k.khuyenMaiId, k.maKhuyenMai, k.tenKhuyenMai, k.loai, k.giaTri, k.giaTriToiDa, k.donHangToiThieu, k.ngayBatDau, k.ngayKetThuc, k.soLuongToiDa, k.soLanDaDung, CASE WHEN k.soLuongToiDa IS NULL THEN NULL ELSE k.soLuongToiDa - k.soLanDaDung END, k.trangThai, k.ngayTao) FROM KhuyenMai k")
    List<KhuyenMaiResponse> hienThiKhuyenMai();

    @Query("SELECT k FROM KhuyenMai k WHERE k.trangThai = 'active' " +
           "AND k.ngayBatDau <= CURRENT_TIMESTAMP AND k.ngayKetThuc >= CURRENT_TIMESTAMP")
    List<KhuyenMai> findActiveKhaDung();
}
