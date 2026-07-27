package com.example.backend.repository;

import com.example.backend.entity.LichSuTangDiem;
import com.example.backend.response.LichSuTangDiemResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LichSuTangDiemRepository extends JpaRepository<LichSuTangDiem, Integer> {

    @Query("SELECT new com.example.backend.response.LichSuTangDiemResponse(" +
           "l.id, l.soDiem, l.lyDo, nv.hoTen, l.ngayTao) " +
           "FROM LichSuTangDiem l JOIN l.nhanVien nv " +
           "WHERE l.khachHang.khachHangId = :khachHangId ORDER BY l.ngayTao DESC")
    List<LichSuTangDiemResponse> findResponsesByKhachHangId(@Param("khachHangId") Integer khachHangId);
}
