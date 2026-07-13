package com.example.backend.repository;

import com.example.backend.entity.PhieuBaoHanh;
import com.example.backend.response.PhieuBaoHanhResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhieuBaoHanhRepository extends JpaRepository<PhieuBaoHanh, Integer> {
    @Query("SELECT new com.example.backend.response.PhieuBaoHanhResponse(p.baoHanhId, p.donHang.id, p.bienThe.sanPham.sanPhamId, p.khachHang.khachHangId, ctsp.soSerial, p.ngayMua, p.ngayHetBh, p.ngayTiepNhan, p.ngayTraKhach, p.moTaLoi, p.ketQuaXuLy, p.trangThai, p.chiPhiPhatSinh, p.ghiChu) FROM PhieuBaoHanh p LEFT JOIN p.chiTietSanPham ctsp")
    List<PhieuBaoHanhResponse> hienThiPhieuBaoHanh();

    List<PhieuBaoHanh> findByDonHang_Id(Integer donHangId);

    boolean existsByBienThe_BienTheId(Integer bienTheId);
}
