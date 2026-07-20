package com.example.backend.repository;

import com.example.backend.entity.ChiTietRam;
import com.example.backend.response.ChiTietRamResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietRamRepository extends JpaRepository<ChiTietRam, Integer> {
    @Query("SELECT new com.example.backend.response.ChiTietRamResponse(c.chiTietRamId, c.ram.ramId, c.ram.dungLuong, c.soSerial, c.trangThai, c.ngayNhapKho, c.ghiChu) FROM ChiTietRam c")
    List<ChiTietRamResponse> hienThiChiTietRam();
}
