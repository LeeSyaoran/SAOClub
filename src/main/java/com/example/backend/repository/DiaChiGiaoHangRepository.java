package com.example.backend.repository;

import com.example.backend.entity.DiaChiGiaoHang;
import com.example.backend.response.DiaChiGiaoHangResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiaChiGiaoHangRepository extends JpaRepository<DiaChiGiaoHang, Integer> {
    @Query("SELECT new com.example.backend.response.DiaChiGiaoHangResponse(d.id, d.khachHang.khachHangId, d.hoTenNguoiNhan, d.soDienThoai, d.diaChi, d.thanhPho, d.tinh, d.laMacDinh, d.ngayTao) FROM DiaChiGiaoHang d")
    List<DiaChiGiaoHangResponse> hienThiDiaChiGiaoHang();
}
