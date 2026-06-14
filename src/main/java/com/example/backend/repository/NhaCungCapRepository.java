package com.example.backend.repository;

import com.example.backend.entity.NhaCungCap;
import com.example.backend.response.NhaCungCapResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NhaCungCapRepository extends JpaRepository<NhaCungCap, Integer> {
	@Query("SELECT new com.example.backend.response.NhaCungCapResponse(n.nhaCungCapId, n.tenNhaCungCap, n.soDienThoai, n.email, n.diaChi, n.maSoThue, n.nguoiLienHe, n.trangThai, n.ngayTao) FROM NhaCungCap n")
	java.util.List<NhaCungCapResponse> hienThiNhaCungCap();
}
