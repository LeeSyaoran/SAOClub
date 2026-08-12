package com.example.backend.repository;

import com.example.backend.entity.ThanhToan;
import com.example.backend.response.ThanhToanResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ThanhToanRepository extends JpaRepository<ThanhToan, Integer> {
    @Query("SELECT new com.example.backend.response.ThanhToanResponse(t.thanhToanId, t.donHang.id, t.ngayThanhToan, t.phuongThucThanhToan, t.soTien, t.maGiaoDich, t.trangThai, t.ghiChu) FROM ThanhToan t")
    List<ThanhToanResponse> hienThiThanhToan();

    @Query("SELECT new com.example.backend.response.ThanhToanResponse(t.thanhToanId, t.donHang.id, t.ngayThanhToan, t.phuongThucThanhToan, t.soTien, t.maGiaoDich, t.trangThai, t.ghiChu) FROM ThanhToan t WHERE t.donHang.id = :donHangId")
    List<ThanhToanResponse> hienThiThanhToanTheoDonHang(@Param("donHangId") Integer donHangId);

    List<ThanhToan> findByDonHang_Id(Integer donHangId);
}
