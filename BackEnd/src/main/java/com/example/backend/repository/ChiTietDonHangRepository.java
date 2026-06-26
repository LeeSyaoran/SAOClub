package com.example.backend.repository;

import com.example.backend.entity.ChiTietDonHang;
import com.example.backend.response.ChiTietDonHangResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietDonHangRepository extends JpaRepository<ChiTietDonHang, Integer> {
    @Query("SELECT new com.example.backend.response.ChiTietDonHangResponse(c.id, c.donHang.id, c.bienThe.bienTheId, c.bienThe.maSku, ctsp.chiTietId, c.soLuong, c.donGia, c.giamGiaDong, c.thanhTien, c.ghiChu) FROM ChiTietDonHang c LEFT JOIN c.chiTietSanPham ctsp")
    List<ChiTietDonHangResponse> hienThiChiTietDonHang();

    // Lấy tất cả sản phẩm thuộc 1 đơn hàng cụ thể — dùng ?1 để tránh cần @Param
    @Query("SELECT new com.example.backend.response.ChiTietDonHangResponse(c.id, c.donHang.id, c.bienThe.bienTheId, c.bienThe.maSku, ctsp.chiTietId, c.soLuong, c.donGia, c.giamGiaDong, c.thanhTien, c.ghiChu) FROM ChiTietDonHang c LEFT JOIN c.chiTietSanPham ctsp WHERE c.donHang.id = ?1")
    List<ChiTietDonHangResponse> findByDonHangId(Integer donHangId);

    // Trả về entity (không phải DTO) — dùng cho merge và recalculate
    @Query("SELECT c FROM ChiTietDonHang c WHERE c.donHang.id = ?1")
    List<ChiTietDonHang> findEntityByDonHangId(Integer donHangId);
}
