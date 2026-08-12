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

    List<ChiTietDonHangSerial> findByChiTietDonHang_Id(Integer chiTietDonHangId);

    void deleteByChiTietDonHang_Id(Integer chiTietDonHangId);

    @Query("SELECT new com.example.backend.response.ChiTietDonHangSerialResponse(s.chiTietDonHang.id, s.chiTietSanPham.chiTietId, s.chiTietSanPham.soSerial) " +
           "FROM ChiTietDonHangSerial s WHERE s.chiTietDonHang.donHang.id = :donHangId")
    List<ChiTietDonHangSerialResponse> findByDonHangId(@Param("donHangId") Integer donHangId);
}
