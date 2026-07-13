package com.example.backend.repository;

import com.example.backend.entity.ChiTietTraHang;
import com.example.backend.response.ChiTietTraHangResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietTraHangRepository extends JpaRepository<ChiTietTraHang, Integer> {
    @Query("SELECT new com.example.backend.response.ChiTietTraHangResponse(c.id, c.phieuTraHang.phieuTraId, c.bienThe.bienTheId, c.bienThe.maSku, c.chiTietSanPham.chiTietId, c.soLuong, c.donGiaHoan, c.tinhTrang) FROM ChiTietTraHang c")
    List<ChiTietTraHangResponse> hienThiChiTietTraHang();

    boolean existsByBienThe_BienTheId(Integer bienTheId);
}
