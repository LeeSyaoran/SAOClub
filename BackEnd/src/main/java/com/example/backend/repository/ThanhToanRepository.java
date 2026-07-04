package com.example.backend.repository;

import com.example.backend.entity.ThanhToan;
import com.example.backend.response.ThanhToanResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ThanhToanRepository extends JpaRepository<ThanhToan, Integer> {
    @Query("SELECT new com.example.backend.response.ThanhToanResponse(t.thanhToanId, t.donHang.id, t.ngayThanhToan, t.phuongThucThanhToan, t.soTien, t.maGiaoDich, t.trangThai, t.ghiChu) FROM ThanhToan t")
    List<ThanhToanResponse> hienThiThanhToan();

    List<ThanhToan> findByDonHang_Id(Integer donHangId);
}
