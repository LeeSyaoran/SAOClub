package com.example.backend.repository;

import com.example.backend.entity.PhieuTraHang;
import com.example.backend.response.PhieuTraHangResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhieuTraHangRepository extends JpaRepository<PhieuTraHang, Integer> {
    @Query("SELECT new com.example.backend.response.PhieuTraHangResponse(p.phieuTraId, p.donHang.id, nv.nhanVienId, p.lyDo, p.ngayTra, p.trangThai, p.soTienHoan, p.hinhThucHoan, p.ghiChu, p.maPhieu) " +
           "FROM PhieuTraHang p LEFT JOIN p.nhanVien nv")
    List<PhieuTraHangResponse> hienThiPhieuTraHang();

    List<PhieuTraHang> findByDonHang_Id(Integer donHangId);
}
