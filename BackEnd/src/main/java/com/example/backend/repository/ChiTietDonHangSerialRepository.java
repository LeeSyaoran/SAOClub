package com.example.backend.repository;

import com.example.backend.entity.ChiTietDonHangSerial;
import com.example.backend.response.ChiTietDonHangSerialResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietDonHangSerialRepository extends JpaRepository<ChiTietDonHangSerial, Integer> {

    // Toàn bộ serial đang gắn với 1 dòng đơn hàng — dùng để hiện lại lựa chọn đã giữ chỗ
    // khi mở modal chọn serial, và để giải phóng khi admin đổi lựa chọn hoặc hủy đơn.
    List<ChiTietDonHangSerial> findByChiTietDonHang_Id(Integer chiTietDonHangId);

    void deleteByChiTietDonHang_Id(Integer chiTietDonHangId);

    // Toàn bộ serial đã gắn cho mọi dòng của 1 đơn hàng, gộp theo chiTietDonHangId ở phía
    // gọi (FE) — dùng để load lại modal "Chọn serial trước khi đóng gói".
    @Query("SELECT new com.example.backend.response.ChiTietDonHangSerialResponse(s.chiTietDonHang.id, s.chiTietSanPham.chiTietId, s.chiTietSanPham.soSerial) " +
           "FROM ChiTietDonHangSerial s WHERE s.chiTietDonHang.donHang.id = :donHangId")
    List<ChiTietDonHangSerialResponse> findByDonHangId(@Param("donHangId") Integer donHangId);
}
