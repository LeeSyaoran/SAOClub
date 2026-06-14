package com.example.backend.repository;

import com.example.backend.entity.NhanVien;
import com.example.backend.response.NhanVienResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NhanVienRepository extends JpaRepository<NhanVien, Integer> {
	@Query("SELECT new com.example.backend.response.NhanVienResponse(n.nhanVienId, n.hoTen, n.soDienThoai, n.email, n.chucVu.id, n.username, n.luongCoBan, n.trangThai, n.ngayTao) FROM NhanVien n LEFT JOIN n.chucVu")
	java.util.List<NhanVienResponse> hienThiNhanVien();
}
