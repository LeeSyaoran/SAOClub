package com.example.backend.repository;

import com.example.backend.entity.KhachHang;
import com.example.backend.response.KhachHangResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, Integer> {
	@Query("SELECT new com.example.backend.response.KhachHangResponse(k.khachHangId, k.hoTen, k.soDienThoai, k.email, k.diaChi, k.loaiKhach, k.tenCongTy, k.maSoThue, k.diemTichLuy, k.trangThai, k.ngayTao) FROM KhachHang k")
	java.util.List<KhachHangResponse> hienThiKhachHang();
}
