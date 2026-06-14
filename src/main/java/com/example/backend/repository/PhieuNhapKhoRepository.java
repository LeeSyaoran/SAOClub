package com.example.backend.repository;

import com.example.backend.entity.PhieuNhapKho;
import com.example.backend.response.PhieuNhapKhoResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhieuNhapKhoRepository extends JpaRepository<PhieuNhapKho, Integer> {
    @Query("SELECT new com.example.backend.response.PhieuNhapKhoResponse(p.phieuNhapId, p.maPhieuNhap, p.nhaCungCap.nhaCungCapId, p.nhanVien.nhanVienId, p.ngayNhap, p.tongTien, p.trangThai, p.ghiChu) FROM PhieuNhapKho p")
    List<PhieuNhapKhoResponse> hienThiPhieuNhapKho();
}
