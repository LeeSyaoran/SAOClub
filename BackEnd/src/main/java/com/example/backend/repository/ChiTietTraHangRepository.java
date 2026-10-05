package com.example.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.ChiTietTraHang;
import com.example.backend.response.ChiTietTraHangResponse;

@Repository
public interface ChiTietTraHangRepository extends JpaRepository<ChiTietTraHang, Integer> {
    @Query("SELECT new com.example.backend.response.ChiTietTraHangResponse(c.id, c.phieuTraHang.phieuTraId, c.bienThe.bienTheId, c.bienThe.maSku, ctsp.chiTietId, ctsp.soSerial, c.soLuong, c.donGiaHoan, c.tinhTrang) FROM ChiTietTraHang c LEFT JOIN c.chiTietSanPham ctsp")
    List<ChiTietTraHangResponse> hienThiChiTietTraHang();

    boolean existsByBienThe_BienTheId(Integer bienTheId);

    List<ChiTietTraHang> findByPhieuTraHang_PhieuTraId(Integer phieuTraId);
}
