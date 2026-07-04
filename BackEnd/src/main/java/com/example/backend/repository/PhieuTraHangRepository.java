package com.example.backend.repository;

import com.example.backend.entity.PhieuTraHang;
import com.example.backend.response.PhieuTraHangResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhieuTraHangRepository extends JpaRepository<PhieuTraHang, Integer> {
    @Query("SELECT new com.example.backend.response.PhieuTraHangResponse(p.phieuTraId, p.donHang.id, p.nhanVien.nhanVienId, p.lyDo, p.ngayTra, p.trangThai, p.soTienHoan, p.ghiChu) FROM PhieuTraHang p")
    List<PhieuTraHangResponse> hienThiPhieuTraHang();

    List<PhieuTraHang> findByDonHang_Id(Integer donHangId);
}
