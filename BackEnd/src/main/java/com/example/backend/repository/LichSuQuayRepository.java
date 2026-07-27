package com.example.backend.repository;

import com.example.backend.entity.LichSuQuay;
import com.example.backend.response.LichSuQuayResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LichSuQuayRepository extends JpaRepository<LichSuQuay, Integer> {

    // Projection thẳng ra Response (đúng pattern ChiTietDonHangSerialRepository.findByDonHangId)
    // — tránh N+1 lazy-load lichSu.khuyenMai.tenKhuyenMai riêng cho mỗi dòng.
    @Query("SELECT new com.example.backend.response.LichSuQuayResponse(" +
           "l.id, l.ngayQuay, l.ketQua, k.tenKhuyenMai, l.diemDaTru) " +
           "FROM LichSuQuay l LEFT JOIN l.khuyenMai k " +
           "WHERE l.khachHang.khachHangId = :khachHangId ORDER BY l.ngayQuay DESC")
    List<LichSuQuayResponse> findResponsesByKhachHangId(@Param("khachHangId") Integer khachHangId);

    // Danh sách phieuId đã trúng qua vòng quay của 1 khách — dùng để suy ra cột "Nguồn"
    // trong danh sách voucher admin xem (không thêm cột DB mới).
    @Query("SELECT l.phieuGiamGiaCaNhan.phieuId FROM LichSuQuay l " +
           "WHERE l.khachHang.khachHangId = :khachHangId AND l.phieuGiamGiaCaNhan IS NOT NULL")
    List<Integer> findPhieuIdsByKhachHangId(@Param("khachHangId") Integer khachHangId);
}
