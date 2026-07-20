package com.example.backend.repository;

import com.example.backend.entity.ChiTietOcung;
import com.example.backend.response.ChiTietOcungResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietOcungRepository extends JpaRepository<ChiTietOcung, Integer> {
    @Query("SELECT new com.example.backend.response.ChiTietOcungResponse(c.chiTietOCungId, c.oCung.oCungId, c.oCung.loaiOcung, c.soSerial, c.trangThai, c.ngayNhapKho, c.ghiChu) FROM ChiTietOcung c")
    List<ChiTietOcungResponse> hienThiChiTietOcung();
}
