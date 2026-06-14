package com.example.backend.repository;

import com.example.backend.entity.ChiTietDonHang;
import com.example.backend.response.ChiTietDonHangResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietDonHangRepository extends JpaRepository<ChiTietDonHang, Integer> {
    @Query("SELECT new com.example.backend.response.ChiTietDonHangResponse(c.id, c.donHang.id, c.bienThe.bienTheId, c.bienThe.maSku, c.chiTietSanPham.chiTietId, c.soLuong, c.donGia, c.giamGiaDong, c.thanhTien, c.ghiChu) FROM ChiTietDonHang c")
    List<ChiTietDonHangResponse> hienThiChiTietDonHang();
}
