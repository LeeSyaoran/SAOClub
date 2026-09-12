package com.example.backend.repository;

import com.example.backend.entity.PhieuBaoHanh;
import com.example.backend.response.PhieuBaoHanhResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhieuBaoHanhRepository extends JpaRepository<PhieuBaoHanh, Integer> {
    @Query("SELECT new com.example.backend.response.PhieuBaoHanhResponse(p.baoHanhId, p.donHang.id, p.bienThe.bienTheId, p.bienThe.maSku, p.bienThe.sanPham.tenSanPham, p.khachHang.khachHangId, p.khachHang.hoTen, ctsp.chiTietId, ctsp.soSerial, p.ngayMua, p.ngayHetBh, p.ngayTiepNhan, p.ngayBatDauXuLy, p.ngayTraKhach, p.moTaLoi, p.ketQuaXuLy, p.trangThai, p.chiPhiPhatSinh, p.ghiChu, p.phuongThuc, p.diaChiLayHang, p.lyDoTuChoi) FROM PhieuBaoHanh p LEFT JOIN p.chiTietSanPham ctsp")
    List<PhieuBaoHanhResponse> hienThiPhieuBaoHanh();

    @Query(value = "SELECT new com.example.backend.response.PhieuBaoHanhResponse(p.baoHanhId, p.donHang.id, p.bienThe.bienTheId, p.bienThe.maSku, p.bienThe.sanPham.tenSanPham, p.khachHang.khachHangId, p.khachHang.hoTen, ctsp.chiTietId, ctsp.soSerial, p.ngayMua, p.ngayHetBh, p.ngayTiepNhan, p.ngayBatDauXuLy, p.ngayTraKhach, p.moTaLoi, p.ketQuaXuLy, p.trangThai, p.chiPhiPhatSinh, p.ghiChu, p.phuongThuc, p.diaChiLayHang, p.lyDoTuChoi) FROM PhieuBaoHanh p LEFT JOIN p.chiTietSanPham ctsp",
           countQuery = "SELECT COUNT(p) FROM PhieuBaoHanh p")
    Page<PhieuBaoHanhResponse> hienThiPhieuBaoHanh(Pageable pageable);

    List<PhieuBaoHanh> findByDonHang_Id(Integer donHangId);

    boolean existsByBienThe_BienTheId(Integer bienTheId);

<<<<<<< HEAD
    @Query("SELECT new com.example.backend.response.PhieuBaoHanhResponse(p.baoHanhId, p.donHang.id, p.bienThe.bienTheId, p.bienThe.maSku, p.bienThe.sanPham.tenSanPham, p.khachHang.khachHangId, p.khachHang.hoTen, ctsp.chiTietId, ctsp.soSerial, p.ngayMua, p.ngayHetBh, p.ngayTiepNhan, p.ngayBatDauXuLy, p.ngayTraKhach, p.moTaLoi, p.ketQuaXuLy, p.trangThai, p.chiPhiPhatSinh, p.ghiChu, p.phuongThuc, p.diaChiLayHang, p.lyDoTuChoi) FROM PhieuBaoHanh p LEFT JOIN p.chiTietSanPham ctsp WHERE p.khachHang.khachHangId = ?1")
    List<PhieuBaoHanhResponse> findByKhachHangId(Integer khachHangId);

}
=======
    // Lich su phieu bao hanh cua 1 serial, moi nhat truoc
    @Query("SELECT new com.example.backend.response.PhieuBaoHanhResponse(p.baoHanhId, p.donHang.id, p.bienThe.bienTheId, p.bienThe.maSku, p.khachHang.khachHangId, ctsp.chiTietId, ctsp.soSerial, p.ngayMua, p.ngayHetBh, p.ngayTiepNhan, p.ngayTraKhach, p.moTaLoi, p.ketQuaXuLy, p.trangThai, p.chiPhiPhatSinh, p.ghiChu) FROM PhieuBaoHanh p LEFT JOIN p.chiTietSanPham ctsp WHERE ctsp.chiTietId = :chiTietId ORDER BY p.ngayTiepNhan DESC NULLS LAST")
    List<PhieuBaoHanhResponse> findByChiTietId(@Param("chiTietId") Integer chiTietId);
}
>>>>>>> 263fbf4733d7677b5a1c903b79b60a2fc9633142
